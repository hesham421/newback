package com.erp.fin.repository;

import com.erp.fin.entity.Account;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-FIN-001 (Account). Module-internal (A.2.3).
 *
 * <p>QR-FIN-001 (search accounts, API-FIN-001) is satisfied by the inherited
 * {@code findAll(Specification, Pageable)} plus the shared {@code SpecBuilder}/
 * {@code PageableBuilder} layer at the service. QR-FIN-002 (create) and QR-FIN-003/004
 * (update, deactivate) use the inherited {@code save}. v2 QR-FIN-060 (RULE-FIN-023's leaf and
 * active flags by account id, API-FIN-039/040) is the inherited {@code findById} — one row of one
 * table, so no projection (A.2.8).
 */
@Repository
public interface AccountRepository
    extends JpaRepository<Account, Long>,
            JpaSpecificationExecutor<Account> {

    /**
     * QR-FIN-005 — API-FIN-002's duplicate-code pre-check ({@code UQ_FIN_ACCOUNT_CODE},
     * {@code FIN-409-ACCOUNT-DUP}). No {@code AndIdNot} variant: DATA-DOM-MASTER.md's DTO
     * membership makes {@code code} immutable on update, so the update-time exclusion check
     * would be dead code (A.2.5).
     */
    boolean existsByCode(String code);

    /**
     * QR-FIN-006 — RULE-FIN-001's fact: does this account have any sub-account? The service
     * passes the result into {@code AccountDomain.assertCanBeMarkedLeaf(...)}, which owns the
     * decision (A.2.9's counterpart to the repository's "fetch and count, never decide" rule).
     */
    boolean existsByParentAccount_AccountPk(Long parentAccountPk);

    /**
     * API-FIN-027 (REQ-FIN-036 / POL-FIN-010) — the account marked as Retained Earnings
     * (DBF-FIN-147), which the year-end closing entry posts the year's result to. The system
     * derives it here instead of accepting it from the caller.
     *
     * <p>{@code UQ_FIN_ACCOUNT_RETAINED_EARNINGS} (partial unique index, V23) makes a second
     * marked row impossible, so "first ordered by pk" can only ever be the one marked account;
     * the explicit ordering keeps the read deterministic even against a database predating the
     * migration. An empty result is the service's {@code FIN-404-ACCOUNT}.
     */
    Optional<Account> findFirstByIsRetainedEarningsFlTrueOrderByAccountPkAsc();
}
