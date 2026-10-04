package com.erp.file.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.file.exception.FileErrorCodes;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * RULE-FILE-003 — issues and validates the short-lived AES/GCM access token that gates a single
 * download (separate from JWT). The token encodes fileId + expiry + a random nonce; GCM's auth tag
 * provides integrity. This class does the crypto only — generate, and validate (not-expired +
 * integrity). Single-use enforcement (a consumed-nonce store) is an SVC-API concern, not here.
 * The AES key is derived (SHA-256) from a secret passed in as a plain argument via the static
 * factory — no Spring/JPA annotations, no persistence access. The consuming @Service supplies the
 * secret from configuration ({@code file.access-token.secret}).
 */
public final class FileAccessTokenDomainService {

    public static final Duration TOKEN_TTL = Duration.ofMinutes(10);

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String ALGORITHM = "AES";
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final int NONCE_BYTES = 16;
    private static final int PAYLOAD_BYTES = Long.BYTES + Long.BYTES + NONCE_BYTES;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final SecretKeySpec key;

    private FileAccessTokenDomainService(SecretKeySpec key) {
        this.key = key;
    }

    /** Derives a 256-bit AES key from the configured secret (SHA-256). */
    public static FileAccessTokenDomainService create(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("file.access-token.secret must be configured");
        }
        try {
            byte[] keyBytes = MessageDigest.getInstance("SHA-256")
                .digest(secret.getBytes(StandardCharsets.UTF_8));
            return new FileAccessTokenDomainService(new SecretKeySpec(keyBytes, ALGORITHM));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to derive file access-token key", e);
        }
    }

    /** Issues a fresh token bound to {@code fileId}, valid for {@link #TOKEN_TTL}. */
    public String issueToken(long fileId) {
        byte[] nonce = new byte[NONCE_BYTES];
        SECURE_RANDOM.nextBytes(nonce);
        byte[] plaintext = ByteBuffer.allocate(PAYLOAD_BYTES)
            .putLong(fileId)
            .putLong(Instant.now().plus(TOKEN_TTL).toEpochMilli())
            .put(nonce)
            .array();

        byte[] iv = new byte[GCM_IV_BYTES];
        SECURE_RANDOM.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plaintext);
            byte[] token = ByteBuffer.allocate(iv.length + ciphertext.length)
                .put(iv).put(ciphertext).array();
            return Base64.getUrlEncoder().withoutPadding().encodeToString(token);
        } catch (Exception e) {
            throw new LocalizedException(Status.UNAUTHORIZED, FileErrorCodes.FILE_ACCESS_TOKEN_INVALID);
        }
    }

    /**
     * RULE-FILE-003 — validates integrity and expiry, returning the encoded fileId. Throws
     * {@code FILE_ACCESS_TOKEN_INVALID} on any tamper, malformed input, or expiry.
     */
    public long validateAndExtractFileId(String token) {
        try {
            byte[] raw = Base64.getUrlDecoder().decode(token);
            ByteBuffer buffer = ByteBuffer.wrap(raw);
            byte[] iv = new byte[GCM_IV_BYTES];
            buffer.get(iv);
            byte[] ciphertext = new byte[buffer.remaining()];
            buffer.get(ciphertext);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
            ByteBuffer payload = ByteBuffer.wrap(cipher.doFinal(ciphertext));

            long fileId = payload.getLong();
            long expiryEpochMillis = payload.getLong();
            if (Instant.now().toEpochMilli() > expiryEpochMillis) {
                throw new LocalizedException(Status.UNAUTHORIZED,
                    FileErrorCodes.FILE_ACCESS_TOKEN_INVALID);
            }
            return fileId;
        } catch (LocalizedException e) {
            throw e;
        } catch (Exception e) {
            throw new LocalizedException(Status.UNAUTHORIZED, FileErrorCodes.FILE_ACCESS_TOKEN_INVALID);
        }
    }
}
