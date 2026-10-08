package com.erp.common.idempotency;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import javax.crypto.spec.SecretKeySpec;

/**
 * {@code erp.core.idempotency.*} as the mechanism needs it, built by {@code ErpCoreAutoConfiguration}, so this package
 * imports nothing outside {@code com.erp.common}. The request-hash key is derived from the JWT secret with a label of
 * its own, never the signing key itself.
 */
public record IdempotencySettings(boolean enabled, Duration retention, SecretKeySpec hashKey) {

    static final String HASH_ALGORITHM = "HmacSHA256";

    private static final String KEY_LABEL = "erp.core.idempotency.request-hash|";

    public IdempotencySettings {
        if (retention == null || retention.isNegative() || retention.isZero()) {
            throw new IllegalStateException("erp.core.idempotency.retention must be a positive duration, was " + retention);
        }
    }

    public static IdempotencySettings of(boolean enabled, Duration retention, String serverSecret) {
        return new IdempotencySettings(enabled, retention, new SecretKeySpec(sha256(KEY_LABEL + serverSecret), HASH_ALGORITHM));
    }

    private static byte[] sha256(String text) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm unavailable in this JVM", e);
        }
    }

    @Override
    public String toString() {
        return "IdempotencySettings[enabled=" + enabled + ", retention=" + retention + "]";
    }
}
