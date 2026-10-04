package com.erp.fin.repository;

import com.erp.fin.entity.JournalLine;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-FIN-005 (JournalLine). Module-internal (A.2.3).
 *
 * <p>QR-FIN-024 / QR-FIN-025 / QR-FIN-034 write lines through the cascade on
 * {@code JournalEntry.lines}, in the single transaction CORE.md mandates — no save mapping is
 * declared here. Search is the inherited {@code findAll(Specification, Pageable)} plus the shared
 * {@code SpecBuilder}/{@code PageableBuilder} layer, which is also how the report reads
 * QR-FIN-042 (account ledger) and QR-FIN-043 (trial balance / statements) will be served in
 * their own phase.
 *
 * <p><b>RULE-FIN-016.</b> No UPDATE or DELETE mapping is declared here either: a line is locked
 * with its header the moment the entry posts.
 */
@Repository
public interface JournalLineRepository
    extends JpaRepository<JournalLine, Long>,
            JpaSpecificationExecutor<JournalLine> {

    /**
     * QR-FIN-029 / QR-FIN-034 / QR-FIN-037 — the entry's line set: the rows
     * {@code JournalEntryDomain.assertBalanced(...)} is decided over (RULE-FIN-006), the rows a
     * reversal mirrors (RULE-FIN-011), and the lines API-FIN-022 returns. {@code JOIN FETCH} on
     * the target account avoids N+1 and also supplies RULE-FIN-007's facts for
     * {@code AccountDomain.assertPostable()} (A.2.6). The repository only fetches — it never
     * decides.
     */
    @Query("SELECT l FROM JournalLine l JOIN FETCH l.account "
        + "WHERE l.journalEntry.journalEntryPk = :journalEntryPk ORDER BY l.lineNo ASC")
    List<JournalLine> findByJournalEntryPk(@Param("journalEntryPk") Long journalEntryPk);

    /**
     * QR-FIN-042 — API-FIN-028's account ledger: every POSTED line on one account, oldest first,
     * optionally narrowed to a docDate range and to lines carrying a given dimension or dimension
     * value.
     *
     * <p><b>POSTED only, live.</b> {@code e.statusCode = :postedStatus} is the ONE gate that makes
     * this report correct: a DRAFT line is not yet part of the ledger. A reversed entry stays
     * POSTED and is deliberately still seen here — classic reversal (RULE-FIN-011) cancels it
     * through its mirror entry's opposite-direction lines, which this query also sees, so the pair
     * nets to zero while both remain visible in the ledger. There is no stored balance column in
     * db-script-fin.md to read instead (POL-FIN-009), and none may be introduced.
     *
     * <p>Returns a projection, not entities (A.2.8) — the read spans FIN_JOURNAL_LINE and
     * FIN_JOURNAL_ENTRY and needs no managed state.
     */
    @Query("SELECT e.journalEntryPk AS journalEntryId, e.docNo AS docNo, e.docDate AS docDate, "
        + "e.journalTypeCode AS journalTypeCode, e.eventReference AS eventReference, "
        + "l.journalLinePk AS journalLineId, l.lineNo AS lineNo, l.amount AS amount, "
        + "l.directionCode AS directionCode, l.descriptionAr AS descriptionAr, "
        + "l.descriptionEn AS descriptionEn "
        + "FROM JournalLine l JOIN l.journalEntry e "
        + "WHERE l.account.accountPk = :accountId AND e.statusCode = :postedStatus "
        + "AND (CAST(:fromDate AS LocalDate) IS NULL OR e.docDate >= :fromDate) "
        + "AND (CAST(:toDate AS LocalDate) IS NULL OR e.docDate <= :toDate) "
        + "AND (CAST(:dimensionId AS Long) IS NULL OR EXISTS (SELECT jd.journalLineDimensionPk "
        + "FROM JournalLineDimension jd WHERE jd.journalLine = l "
        + "AND jd.dimension.dimensionPk = :dimensionId)) "
        + "AND (CAST(:dimensionValueId AS Long) IS NULL OR EXISTS (SELECT jv.journalLineDimensionPk "
        + "FROM JournalLineDimension jv WHERE jv.journalLine = l "
        + "AND jv.dimensionValue.dimensionValuePk = :dimensionValueId)) "
        + "ORDER BY e.docDate ASC, e.docNo ASC, l.lineNo ASC")
    List<AccountLedgerLineView> findAccountLedgerLines(
        @Param("accountId") Long accountId,
        @Param("postedStatus") String postedStatus,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate,
        @Param("dimensionId") Long dimensionId,
        @Param("dimensionValueId") Long dimensionValueId);

    /**
     * QR-FIN-043 — the ONE aggregation behind API-FIN-029 (trial balance), API-FIN-030 (balance
     * sheet) and API-FIN-031 (income statement). The catalog states the three reports are the same
     * aggregation filtered per report by {@code accountTypeCode}, so exactly one query exists and
     * the type filter is applied to its result by {@code ReportService} — never three near
     * identical queries.
     *
     * <p><b>POSTED only, live</b> — same gate and same reason as
     * {@link #findAccountLedgerLines}. Debits and credits are summed into separate, unsigned
     * columns; because every contributing entry balanced before it could post (RULE-FIN-006,
     * QR-FIN-029), the two column totals match across the whole result by construction
     * (POL-FIN-008).
     *
     * <p>Every scoping parameter is optional: a null one drops its predicate, which is how one
     * query serves a period-scoped trial balance, a year-and-cut-off balance sheet and a
     * year-and-range income statement.
     */
    @Query("SELECT a.accountPk AS accountId, a.code AS accountCode, a.nameAr AS accountNameAr, "
        + "a.nameEn AS accountNameEn, a.accountTypeCode AS accountTypeCode, "
        + "a.natureCode AS natureCode, "
        + "SUM(CASE WHEN l.directionCode = :debitCode THEN l.amount ELSE 0 END) AS debitTotal, "
        + "SUM(CASE WHEN l.directionCode = :debitCode THEN 0 ELSE l.amount END) AS creditTotal "
        + "FROM JournalLine l JOIN l.journalEntry e JOIN l.account a "
        + "WHERE e.statusCode = :postedStatus "
        + "AND (CAST(:periodId AS Long) IS NULL OR e.period.fiscalPeriodPk = :periodId) "
        + "AND (CAST(:fiscalYearId AS Long) IS NULL OR e.fiscalYear.fiscalYearPk = :fiscalYearId) "
        + "AND (CAST(:fromDate AS LocalDate) IS NULL OR e.docDate >= :fromDate) "
        + "AND (CAST(:toDate AS LocalDate) IS NULL OR e.docDate <= :toDate) "
        + "GROUP BY a.accountPk, a.code, a.nameAr, a.nameEn, a.accountTypeCode, a.natureCode "
        + "ORDER BY a.code ASC")
    List<AccountBalanceView> aggregateAccountBalances(
        @Param("postedStatus") String postedStatus,
        @Param("debitCode") String debitCode,
        @Param("periodId") Long periodId,
        @Param("fiscalYearId") Long fiscalYearId,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate);
}
