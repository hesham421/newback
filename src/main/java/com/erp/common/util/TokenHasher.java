package com.erp.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Canonical at-rest hashing for opaque tokens (refresh / password-reset / account-activation),
 * RULE-SEC-004 / DRV-005: only the SHA-256 hex digest is ever persisted, never the raw value. This
 * is the single shared implementation — services hash a presented raw token here before lookup and
 * before insert, so the stored form and the lookup form can never diverge.
 */
public final class TokenHasher {

    private TokenHasher() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** SHA-256 hash (lowercase hex) of an opaque token — the only form persisted. */
    public static String sha256Hex(String rawToken) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is mandated by the Java platform, so this is an unreachable JVM-integrity
            // failure rather than a business error — never surfaced to a client.
            throw new IllegalStateException("SHA-256 algorithm unavailable in this JVM", e);
        }
    }
}
