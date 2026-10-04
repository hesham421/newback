package com.erp.fin.repository;

import com.erp.fin.entity.RuleLine;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-FIN-010 (RuleLine). Module-internal (A.2.3).
 *
 * <p>QR-FIN-015 (add rule line, API-FIN-011) uses the inherited {@code save}.
 */
@Repository
public interface RuleLineRepository
    extends JpaRepository<RuleLine, Long>,
            JpaSpecificationExecutor<RuleLine> {

    /**
     * QR-FIN-016 — the sibling line set RULE-FIN-003 is decided over, and the same read the
     * event-entry build (QR-FIN-025/028) uses at post time. The service combines these rows with
     * the submitted line and hands the result to
     * {@code EventTypeRuleDomain.assertRemainderLineSetValid(...)} — the repository only
     * fetches, it never decides (A.2.9's companion rule). {@code JOIN FETCH} on the parent
     * avoids N+1 (A.2.6).
     */
    @Query("SELECT l FROM RuleLine l JOIN FETCH l.eventTypeRule "
        + "WHERE l.eventTypeRule.eventTypeRulePk = :eventTypeRulePk ORDER BY l.lineNo ASC")
    List<RuleLine> findByEventTypeRulePk(@Param("eventTypeRulePk") Long eventTypeRulePk);

    /**
     * The same read widened to a whole page of parents, so API-FIN-009 can carry each rule's lines
     * without one child query per row — the batch shape
     * {@code RecurringTemplateLineRepository.findByRecurringTemplatePkIn} already uses for
     * API-FIN-012. {@code JOIN FETCH} on the parent avoids N+1 when the caller groups by it
     * (A.2.6).
     */
    @Query("SELECT l FROM RuleLine l JOIN FETCH l.eventTypeRule "
        + "WHERE l.eventTypeRule.eventTypeRulePk IN :eventTypeRulePks "
        + "ORDER BY l.eventTypeRule.eventTypeRulePk ASC, l.lineNo ASC")
    List<RuleLine> findByEventTypeRulePkIn(
        @Param("eventTypeRulePks") List<Long> eventTypeRulePks);
}
