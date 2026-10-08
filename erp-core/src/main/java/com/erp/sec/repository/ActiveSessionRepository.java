package com.erp.sec.repository;

import com.erp.sec.entity.ActiveSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-SEC-010 (ActiveSession). Module-internal (A.2.3). "Non-terminated" is
 * expressed as {@code terminatedAt IS NULL} throughout — the binary state machine for this
 * table; there is no {@code is_active_fl} column.
 */
@Repository
public interface ActiveSessionRepository
    extends JpaRepository<ActiveSession, Long>,
            JpaSpecificationExecutor<ActiveSession> {

    /**
     * QR-SEC-026 (bulk) — every still-active session of one user, the set API-SEC-009 terminates
     * when a user is deactivated (REQ-SEC-011).
     */
    @Query("SELECT s FROM ActiveSession s JOIN FETCH s.user "
        + "WHERE s.user.userPk = :userPk AND s.terminatedAt IS NULL")
    List<ActiveSession> findNonTerminatedByUser(@Param("userPk") Long userPk);

    /**
     * REQ-SEC-092 / -093 (tenant-maturity C12) — every still-open session of the current tenant, both realms (the
     * {@code @TenantId} discriminator restricts it), the set a tenant suspension or a token revocation ends.
     */
    @Query("SELECT s FROM ActiveSession s WHERE s.terminatedAt IS NULL")
    List<ActiveSession> findAllNonTerminated();

    /**
     * REQ-SEC-028's request-time half: the access token's {@code jti} IS this {@code tokenRef}
     * (DBF-SEC-077), so {@code JwtAuthenticationFilter} resolves the row to see whether the session
     * behind a signature-valid token is still live. Returns the FACT; the filter takes the verdict.
     */
    Optional<ActiveSession> findByTokenRef(String tokenRef);

    /**
     * QR-SEC-022 — the active-sessions widget count; same {@code terminatedAt IS NULL} state test.
     * STAFF-realm sessions only since erp-core step 14 (customer logins open sessions too).
     */
    @Query("SELECT COUNT(s) FROM ActiveSession s WHERE s.terminatedAt IS NULL AND s.user.realm = 'STAFF'")
    long countNonTerminated();

    /** REQ-SEC-090 (tenant-maturity B) — the current tenant's open sessions of either realm (tenant usage figures). */
    @Query("SELECT COUNT(s) FROM ActiveSession s WHERE s.terminatedAt IS NULL")
    long countOpenSessions();

    /**
     * erp-core step 14 — API-SEC-026's lookup: a session of a STAFF account only, so the staff
     * session API can neither see nor terminate a customer's session (404 {@code SEC-404-SESSION},
     * indistinguishable from an unknown id).
     */
    @Query("SELECT s FROM ActiveSession s JOIN FETCH s.user u "
        + "WHERE s.activeSessionPk = :id AND u.realm = 'STAFF'")
    Optional<ActiveSession> findStaffSessionById(@Param("id") Long id);
}
