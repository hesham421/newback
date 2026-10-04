package com.erp.sec.repository;

import com.erp.sec.entity.PasswordResetToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-SEC-012 (PasswordResetToken). Module-internal (A.2.3). QR-SEC-038 is
 * catalogued as an EXISTS but is realised here as a fetch, because its predicate IS
 * RULE-SEC-006's verdict — see governance/project-artifacts/sec-implementation-notes.md.
 */
@Repository
public interface PasswordResetTokenRepository
    extends JpaRepository<PasswordResetToken, Long>,
            JpaSpecificationExecutor<PasswordResetToken> {

    /**
     * QR-SEC-038 / QR-SEC-004 (API-SEC-004). {@code JOIN FETCH} loads the owning user, whose
     * password hash the completion step updates. An empty result is reported with the same
     * {@code SEC-409-RESET-TOKEN-INVALID} code as an expired or used token, so a caller can never
     * distinguish "unknown token" from "expired token".
     */
    @Query("SELECT t FROM PasswordResetToken t JOIN FETCH t.user WHERE t.tokenHash = :tokenHash")
    Optional<PasswordResetToken> findByTokenHash(@Param("tokenHash") String tokenHash);
}
