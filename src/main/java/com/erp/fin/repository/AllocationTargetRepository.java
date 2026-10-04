package com.erp.fin.repository;

import com.erp.fin.entity.AllocationTarget;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-FIN-014 (AllocationTarget). Module-internal (A.2.3).
 *
 * <p>Written with QR-FIN-021 (create allocation rule with targets, API-FIN-016) through the
 * inherited {@code save}/{@code saveAll}.
 */
@Repository
public interface AllocationTargetRepository
    extends JpaRepository<AllocationTarget, Long>,
            JpaSpecificationExecutor<AllocationTarget> {

    /**
     * QR-FIN-022 — the rule's target set, read at run time (API-FIN-017) to distribute the
     * source balance, and the same sibling set RULE-FIN-003 is decided over by
     * {@code AllocationRuleDomain}. The repository only fetches; the decision stays in the
     * Domain object. {@code JOIN FETCH} on the parent avoids N+1 (A.2.6).
     */
    @Query("SELECT t FROM AllocationTarget t JOIN FETCH t.allocationRule "
        + "WHERE t.allocationRule.allocationRulePk = :allocationRulePk ORDER BY t.lineNo ASC")
    List<AllocationTarget> findByAllocationRulePk(
        @Param("allocationRulePk") Long allocationRulePk);

    /**
     * QR-FIN-020's companion batch read for API-FIN-015: the targets of a whole PAGE of allocation
     * rules in one query, so the search does not issue {@link #findByAllocationRulePk} once per row
     * (A.2.6 — the N+1 this JOIN FETCH exists to prevent). {@code AllocationRuleResponse} reports a
     * {@code targetCount}, so the search cannot simply omit them.
     */
    @Query("SELECT t FROM AllocationTarget t JOIN FETCH t.allocationRule "
        + "WHERE t.allocationRule.allocationRulePk IN :allocationRulePks ORDER BY t.lineNo ASC")
    List<AllocationTarget> findByAllocationRulePkIn(
        @Param("allocationRulePks") Collection<Long> allocationRulePks);
}
