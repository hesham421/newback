package com.erp.fin.repository;

import com.erp.fin.entity.RecurringTemplate;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-FIN-011 (RecurringTemplate). Module-internal (A.2.3).
 *
 * <p>QR-FIN-017 (search templates, API-FIN-012) is satisfied by the inherited
 * {@code findAll(Specification, Pageable)} plus the shared search layer at the service;
 * QR-FIN-018 (create template with lines, API-FIN-013) by the inherited {@code save}, with the
 * lines saved through {@code RecurringTemplateLineRepository} in the same transaction.
 * QR-FIN-019's single-template load (API-FIN-014) uses the inherited {@code findById}.
 */
@Repository
public interface RecurringTemplateRepository
    extends JpaRepository<RecurringTemplate, Long>,
            JpaSpecificationExecutor<RecurringTemplate> {

    /**
     * API-FIN-014's run lock (code review fix): {@code run()} reads {@code nextRunDate}, posts a
     * full entry for it, then advances the schedule, with no other guard against a second,
     * concurrent {@code run()} for the same template (reachable from both a scheduler and a user,
     * per the service's own javadoc) reading the same {@code nextRunDate} and posting again.
     * Mirrors {@code FiscalYearRepository.lockForDocNoAllocation} — a {@code PESSIMISTIC_WRITE}
     * read of the template row, taken before the schedule read and held to commit, so two
     * concurrent runs of the same template are serialized instead of double-posting.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM RecurringTemplate t WHERE t.recurringTemplatePk = :recurringTemplatePk")
    Optional<RecurringTemplate> lockForRun(@Param("recurringTemplatePk") Long recurringTemplatePk);
}
