package com.erp.fin.repository;

import com.erp.fin.entity.FiscalYear;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-FIN-007 (FiscalYear). Module-internal (A.2.3).
 *
 * <p>QR-FIN-038 (SAVE create fiscal year + periods, API-FIN-023) uses the inherited {@code save}:
 * {@code FiscalYear.periods} cascades, so the year and its generated periods are written in one
 * operation. Search is the inherited {@code findAll(Specification, Pageable)} plus the shared
 * {@code SpecBuilder}/{@code PageableBuilder} layer. QR-FIN-041 (AGGREGATE, year-end) reads
 * ENT-FIN-004/005 and belongs to the journal-entry repositories, not here.
 */
@Repository
public interface FiscalYearRepository
    extends JpaRepository<FiscalYear, Long>,
            JpaSpecificationExecutor<FiscalYear> {

    /**
     * API-FIN-023's duplicate-code pre-check ({@code UQ_FIN_FISCAL_YEAR_CODE},
     * {@code FIN-409-YEAR-DUP}). No {@code AndIdNot} variant: DATA-DOM-MASTER.md gives
     * ENT-FIN-007 no update endpoint, so {@code code} is never mutated and the update-time
     * exclusion check would be dead code (A.2.5).
     */
    boolean existsByCode(String code);

    /**
     * API-FIN-027 — the successor fiscal year that receives the opening entry, found by adjacency:
     * the year whose {@code startDate} is the day after the closing year's {@code endDate}.
     *
     * <p>A DERIVED resolution, not a specified one: ENT-FIN-007 declares no successor column
     * (DBF-FIN-065..074), so adjacency is the only derivation the schema supports. Recorded as a
     * derived decision on {@code execution-state.json}; an absent successor is
     * {@code FIN-404-YEAR}.
     */
    Optional<FiscalYear> findByStartDate(LocalDate startDate);

    /**
     * ALIGN-BE — the {@code docNo} numbering series' allocation lock (DBF-FIN-035).
     *
     * <p>{@code JournalDocNoGenerator} computes "highest committed + 1", so two concurrent creates
     * in the same fiscal year could observe the same predecessor, compute the same number, and let
     * the loser die on {@code UQ_FIN_JOURNAL_ENTRY_YEAR_DOCNO} as an unlocalized data-integrity
     * 409. The recorded docNo decision chose a per-fiscal-year counter backed by that constraint
     * but named no allocation mechanism; this is it — a {@code PESSIMISTIC_WRITE} read of the
     * owning {@code FIN_FISCAL_YEAR} row (SQL {@code SELECT ... FOR UPDATE}), taken before the
     * highest-docNo read and held to commit, so allocation within one fiscal year is serialized
     * and the second writer observes the first's committed number. No sequence, no new column and
     * no retry loop: the schema already carries the row the series belongs to, and a service may
     * not catch {@code DataIntegrityViolationException} (build-create-service, "Shared Layer
     * Mandate"). The unique constraint stays the authoritative backstop, now unreachable in
     * practice rather than the primary mechanism.
     *
     * <p>Callers: {@code JournalEntryService.createManual} (API-FIN-019) and
     * {@code JournalPostingService.buildValidateAndPost} (every system-generated entry) — the only
     * two places a {@code docNo} is allocated — and {@code FiscalYearService.yearEndClose}, which
     * takes the same row lock at the top of its transaction, before the OPEN check (ADR-FIN-041).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT y FROM FiscalYear y WHERE y.fiscalYearPk = :fiscalYearPk")
    Optional<FiscalYear> lockForDocNoAllocation(@Param("fiscalYearPk") Long fiscalYearPk);
}
