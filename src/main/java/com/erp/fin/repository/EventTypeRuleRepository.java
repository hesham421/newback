package com.erp.fin.repository;

import com.erp.fin.entity.EventTypeRule;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-FIN-009 (EventTypeRule). Module-internal (A.2.3).
 *
 * <p>QR-FIN-012 (search event-type rules, API-FIN-009) is satisfied by the inherited
 * {@code findAll(Specification, Pageable)} plus the shared search layer at the service;
 * QR-FIN-013 (create, API-FIN-010) by the inherited {@code save}.
 */
@Repository
public interface EventTypeRuleRepository
    extends JpaRepository<EventTypeRule, Long>,
            JpaSpecificationExecutor<EventTypeRule> {

    /**
     * QR-FIN-014 — one rule per event type, active or not (§6.4, {@code UQ_FIN_EVENT_TYPE_RULE_CODE}
     * is unconditional and this query takes no active flag, so deactivating a rule does not free
     * its event type).
     * The service passes the result into {@code EventTypeRuleDomain.create(...)}, which owns the
     * decision ({@code FIN-409-RULE-DUP}). No {@code AndIdNot} variant: {@code eventTypeCode} is
     * create-only (A.2.5).
     */
    boolean existsByEventTypeCode(String eventTypeCode);

    /**
     * QR-FIN-027 — RULE-FIN-005's fact at post time (API-FIN-020): the active rule for a given
     * event type, if any. Returns the row rather than a boolean because the same call feeds
     * QR-FIN-025's entry build; absence is turned into {@code FIN-404-NO-ACTIVE-RULE} by the
     * service. {@code eventTypeCode} is unique, so at most one row can match.
     */
    Optional<EventTypeRule> findByEventTypeCodeAndIsActiveFl(String eventTypeCode,
                                                             Boolean isActiveFl);

    /**
     * API-FIN-011 Concurrency — the parent rule row under PESSIMISTIC_WRITE before lineNo is read
     * and RULE-FIN-003 is decided over the sibling set; FIN_RULE_LINE has no unique constraint on
     * (rule, lineNo), so this lock is the only guard. Same shape as
     * {@code JournalEntryRepository.lockForReversal}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM EventTypeRule r WHERE r.eventTypeRulePk = :id")
    Optional<EventTypeRule> lockForLineAppend(@Param("id") Long id);
}
