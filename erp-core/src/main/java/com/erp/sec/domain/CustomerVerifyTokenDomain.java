package com.erp.sec.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.sec.entity.CustomerVerifyToken;
import com.erp.sec.exception.SecErrorCodes;
import java.time.Instant;

/**
 * Domain companion for {@link CustomerVerifyToken} (erp-core step 06): a verification token is usable
 * only while unconsumed and before {@code expiresAt}, otherwise {@code VERIFY_TOKEN_INVALID}. The
 * comparison clock is passed in, so the decision is deterministic.
 */
public final class CustomerVerifyTokenDomain {

    private final Instant expiresAt;
    private final Instant usedAt;

    private CustomerVerifyTokenDomain(Instant expiresAt, Instant usedAt) {
        this.expiresAt = expiresAt;
        this.usedAt = usedAt;
    }

    /** Reconstructs a Domain view over a persisted row — no validation. */
    public static CustomerVerifyTokenDomain from(CustomerVerifyToken entity) {
        return new CustomerVerifyTokenDomain(entity.getExpiresAt(), entity.getUsedAt());
    }

    /**
     * @param now the service's comparison instant
     * @throws LocalizedException {@code VERIFY_TOKEN_INVALID} (Status.CONFLICT → 409)
     */
    public void assertUsable(Instant now) {
        if (usedAt != null || expiresAt == null || !expiresAt.isAfter(now)) {
            throw new LocalizedException(Status.CONFLICT, SecErrorCodes.VERIFY_TOKEN_INVALID);
        }
    }

    /** The "unknown token" verdict, the same code as an expired or used one (no token oracle). */
    public static LocalizedException invalid() {
        return new LocalizedException(Status.CONFLICT, SecErrorCodes.VERIFY_TOKEN_INVALID);
    }
}
