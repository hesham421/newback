package com.erp.sec.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.sec.entity.PasswordResetToken;
import com.erp.sec.exception.SecErrorCodes;
import java.time.Instant;

/**
 * Domain companion for ENT-SEC-012 (PasswordResetToken): RULE-SEC-006 — a submission whose token
 * is expired or already used is rejected with {@code SEC-409-RESET-TOKEN-INVALID}. The comparison
 * clock is passed in, never read from {@code Instant.now()} here, so the decision stays
 * deterministic and testable.
 */
public final class PasswordResetTokenDomain {

    private final Instant expiresAt;
    private final Instant usedAt;

    private PasswordResetTokenDomain(Instant expiresAt, Instant usedAt) {
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
    }

    /** Reconstructs a Domain view over a persisted row — no validation. */
    public static PasswordResetTokenDomain from(PasswordResetToken entity) {
        return new PasswordResetTokenDomain(entity.getExpiresAt(), entity.getUsedAt());
    }

    /**
     * API-SEC-004 — RULE-SEC-006: usable only while unconsumed and before {@code expiresAt}.
     *
     * @param now the service's comparison instant
     * @throws LocalizedException {@code SEC-409-RESET-TOKEN-INVALID} (Status.CONFLICT → 409)
     */
    public void assertUsable(Instant now) {
        if (usedAt != null || expiresAt == null || !expiresAt.isAfter(now)) {
            throw new LocalizedException(Status.CONFLICT,
                SecErrorCodes.SEC_409_RESET_TOKEN_INVALID);
        }
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }
}
