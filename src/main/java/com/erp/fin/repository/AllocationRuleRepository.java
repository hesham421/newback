package com.erp.fin.repository;

import com.erp.fin.entity.AllocationRule;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-FIN-013 (AllocationRule). Module-internal (A.2.3).
 *
 * <p>QR-FIN-020 (search allocation rules, API-FIN-015) is satisfied by the inherited
 * {@code findAll(Specification, Pageable)} plus the shared search layer at the service;
 * QR-FIN-021 (create rule with targets, API-FIN-016) by the inherited {@code save}, with the
 * targets saved through {@code AllocationTargetRepository} in the same transaction; QR-FIN-022's
 * single-rule load (API-FIN-017) by the inherited {@code findById}.
 */
@Repository
public interface AllocationRuleRepository
    extends JpaRepository<AllocationRule, Long>,
            JpaSpecificationExecutor<AllocationRule> {

    /**
     * API-FIN-017's run lock (code review fix): {@code run()} reads the source account's balance
     * and posts a full offsetting distribution with no other guard against a second, concurrent
     * {@code run()} for the same rule reading the same pre-relief balance and posting again. This
     * mirrors {@code FiscalYearRepository.lockForDocNoAllocation} — a {@code PESSIMISTIC_WRITE}
     * read of the rule row, taken before the balance read and held to commit, so two concurrent
     * runs of the same rule are serialized instead of double-posting.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM AllocationRule r WHERE r.allocationRulePk = :allocationRulePk")
    Optional<AllocationRule> lockForRun(@Param("allocationRulePk") Long allocationRulePk);
}
