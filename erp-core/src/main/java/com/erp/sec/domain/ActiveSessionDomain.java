package com.erp.sec.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.sec.entity.ActiveSession;
import com.erp.sec.exception.SecErrorCodes;
import java.time.Instant;

/**
 * Domain companion for ENT-SEC-010 (ActiveSession): API-SEC-026's guard that the session is still
 * active ({@code terminatedAt IS NULL}) → {@code SEC-409-ALREADY-TERMINATED}. Exposes only
 * {@code from(..)} and no {@code create(..)} — a session row is minted by the login flow itself
 * (API-SEC-001) with no permit/deny decision attached.
 */
public final class ActiveSessionDomain {

    private final Instant terminatedAt;

    private ActiveSessionDomain(Instant terminatedAt) {
        this.terminatedAt = terminatedAt;
    }

    /** Reconstructs a Domain view over a persisted row — no validation. */
    public static ActiveSessionDomain from(ActiveSession entity) {
        return new ActiveSessionDomain(entity.getTerminatedAt());
    }

    /**
     * API-SEC-026, and the per-session guard of API-SEC-009's bulk termination.
     *
     * @throws LocalizedException {@code SEC-409-ALREADY-TERMINATED} (Status.CONFLICT → 409)
     */
    public void assertCanTerminate() {
        if (terminatedAt != null) {
            throw new LocalizedException(Status.CONFLICT,
                SecErrorCodes.SEC_409_ALREADY_TERMINATED);
        }
    }

    /** State-machine read: {@code terminatedAt IS NULL} = active (DATA-DOM-TRANSACTIONAL.md). */
    public boolean isActive() {
        return terminatedAt == null;
    }
}
