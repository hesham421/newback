package com.erp.common.idempotency;

/**
 * Error codes of the {@code Idempotency-Key} mechanism (tenant-maturity C4, RULE-TENANT-025 / -026); every code has
 * an entry in {@code i18n/messages.properties} and {@code messages_ar.properties}.
 */
public final class IdempotencyErrorCodes {

    private IdempotencyErrorCodes() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** 400 — the header is present but not 1 to 64 characters of {@code A-Z a-z 0-9 . _ : -}. */
    public static final String IDEMPOTENCY_KEY_INVALID = "IDEMPOTENCY_KEY_INVALID";

    /** 409 — the key is stored for this endpoint with another request body, or by another user. */
    public static final String IDEMPOTENCY_KEY_CONFLICT = "IDEMPOTENCY_KEY_CONFLICT";
}
