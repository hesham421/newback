package com.erp.fin.repository;

import com.erp.fin.entity.AccountMapping;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-FIN-015 (AccountMapping, v2). Module-internal (A.2.3).
 *
 * <p>QR-FIN-050 (search, API-FIN-038) is the inherited {@code findAll(Specification, Pageable)}
 * plus the shared {@code SpecBuilder}/{@code PageableBuilder} layer at the service; whether the
 * account's display columns need a {@code JOIN FETCH} variant is SVC-API-SEARCH's decision.
 * QR-FIN-051 (create), QR-FIN-053 (update) and QR-FIN-054 (deactivate) use the inherited
 * {@code save}; the row is loaded by the inherited {@code findById}.
 */
@Repository
public interface AccountMappingRepository
    extends JpaRepository<AccountMapping, Long>,
            JpaSpecificationExecutor<AccountMapping> {

    /**
     * QR-FIN-052 — RULE-FIN-022's fact and API-FIN-039's duplicate pre-check
     * ({@code UQ_FIN_ACCOUNT_MAPPING_ACTIVE_KEY}). The service passes the result into
     * {@code AccountMappingDomain.assertKeyFree(...)}, which owns the decision. No
     * {@code AndIdNot} variant: the key is fixed after create (ADR-FIN-024), so an update-time
     * exclusion check would be dead code (A.2.5).
     */
    boolean existsByEventTypeCodeAndBusinessFieldCodeAndBusinessValueAndIsActiveFlTrue(
        String eventTypeCode, String businessFieldCode, String businessValue);

    /**
     * QR-FIN-055 — RULE-FIN-020's fact for API-FIN-020: the one active mapping for (event type,
     * business field, event value); at most one row by the partial unique index. The account is
     * {@code JOIN FETCH}ed (A.2.6) rather than projected to its id (A.2.8) because the build
     * needs the account row itself right after — its code on the journal line and its leaf/active
     * flags for RULE-FIN-007 — so a projection would only force a second read. The service passes
     * the result into {@code AccountMappingDomain.assertResolved(...)}.
     */
    @Query("SELECT m FROM AccountMapping m JOIN FETCH m.account "
        + "WHERE m.eventTypeCode = :eventTypeCode AND m.businessFieldCode = :businessFieldCode "
        + "AND m.businessValue = :businessValue AND m.isActiveFl = true")
    Optional<AccountMapping> findActiveByKey(@Param("eventTypeCode") String eventTypeCode,
                                             @Param("businessFieldCode") String businessFieldCode,
                                             @Param("businessValue") String businessValue);
}
