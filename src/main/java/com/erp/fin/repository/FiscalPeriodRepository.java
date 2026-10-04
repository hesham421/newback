package com.erp.fin.repository;

import com.erp.fin.entity.FiscalPeriod;
import java.time.LocalDate;
import java.util.List;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-FIN-008 (FiscalPeriod). Module-internal (A.2.3).
 *
 * <p>QR-FIN-038 (SAVE with the year) is served by the cascade on {@code FiscalYear.periods};
 * QR-FIN-039 (UPDATE open / soft-close / hard-close, API-FIN-024/025/026) loads the row through
 * {@link #lockForTransition} and uses the inherited {@code save} after the Domain object has
 * decided. Search is the inherited
 * {@code findAll(Specification, Pageable)} plus the shared {@code SpecBuilder}/
 * {@code PageableBuilder} layer.
 */
@Repository
public interface FiscalPeriodRepository
    extends JpaRepository<FiscalPeriod, Long>,
            JpaSpecificationExecutor<FiscalPeriod> {

    /**
     * QR-FIN-049 — API-FIN-023's response (the year plus its generated periods) and API-FIN-027's year-end
     * close, which transitions every period of the year. {@code JOIN FETCH} on the owning year
     * avoids the N+1 a derived query would cause (A.2.6).
     */
    @Query("SELECT p FROM FiscalPeriod p JOIN FETCH p.fiscalYear "
        + "WHERE p.fiscalYear.fiscalYearPk = :fiscalYearPk ORDER BY p.periodNo")
    List<FiscalPeriod> findByFiscalYearId(@Param("fiscalYearPk") Long fiscalYearPk);

    /**
     * QR-FIN-059 (v2) — API-FIN-020: the period covering the event date, its year fetched (docNo
     * series, RULE-FIN-017; A.2.6). The service passes {@code Limit.of(1)} and hands the result to
     * {@code FiscalPeriodDomain.assertDateCovered}. Overlapping fiscal years are possible
     * (ADR-FIN-040, v1 defect not undertaken by v2): the ordering keeps the chosen row deterministic.
     */
    @Query("SELECT p FROM FiscalPeriod p JOIN FETCH p.fiscalYear "
        + "WHERE p.startDate <= :date AND p.endDate >= :date "
        + "ORDER BY p.fiscalYear.fiscalYearPk, p.periodNo")
    Optional<FiscalPeriod> findCoveringDate(@Param("date") LocalDate date, Limit limit);

    /**
     * API-FIN-024/025/026 — the period row under {@code PESSIMISTIC_WRITE} before its status is
     * judged, so two concurrent transitions of one period serialize instead of racing
     * last-writer-wins (ADR-FIN-041). Same shape as {@code JournalEntryRepository.lockForReversal};
     * read-only paths keep the inherited {@code findById}.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM FiscalPeriod p WHERE p.fiscalPeriodPk = :id")
    Optional<FiscalPeriod> lockForTransition(@Param("id") Long id);
}
