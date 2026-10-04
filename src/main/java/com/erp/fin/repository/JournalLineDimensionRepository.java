package com.erp.fin.repository;

import com.erp.fin.entity.JournalLineDimension;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENT-FIN-006 (JournalLineDimension). Module-internal (A.2.3).
 *
 * <p>QR-FIN-024 / QR-FIN-025 / QR-FIN-034 write these rows through the cascade on
 * {@code JournalLine.dimensions}, in the same single transaction as the header — no save mapping
 * is declared here. QR-FIN-044 (the dimension report, API-FIN-032) is
 * {@link #aggregateByAccountAndDimensionValue}.
 *
 * <p><b>RULE-FIN-016.</b> No UPDATE or DELETE mapping is declared here: a dimension row is locked
 * with the line and header it belongs to.
 */
@Repository
public interface JournalLineDimensionRepository
    extends JpaRepository<JournalLineDimension, Long>,
            JpaSpecificationExecutor<JournalLineDimension> {

    /**
     * QR-FIN-032 / QR-FIN-037 — every dimension row of one entry: the facts RULE-FIN-009 is
     * decided over by {@code DimensionValueDomain.assertUsableOnLine(...)}, and the analytical
     * tags API-FIN-022 returns alongside each line. {@code JOIN FETCH} on both the stated
     * dimension and the cited value avoids N+1 and supplies the rule's comparison in one read
     * (A.2.6); keeping this query separate from QR-FIN-037's header read is what lets that one
     * fetch the line collection without a second collection join.
     */
    @Query("SELECT d FROM JournalLineDimension d "
        + "JOIN FETCH d.dimension JOIN FETCH d.dimensionValue "
        + "WHERE d.journalLine.journalEntry.journalEntryPk = :journalEntryPk "
        + "ORDER BY d.journalLine.lineNo ASC")
    List<JournalLineDimension> findByJournalEntryPk(
        @Param("journalEntryPk") Long journalEntryPk);

    /**
     * QR-FIN-044 — API-FIN-032's aggregation: POSTED amounts grouped by the FULL account AND
     * dimension-value combination, which POL-FIN-011 defines as the posting identity. The
     * {@code GROUP BY} deliberately carries the dimension value, so one account posted against two
     * project values yields two rows and never a single collapsed one (AC-FIN-043).
     *
     * <p><b>POSTED only, live</b> — the same {@code e.statusCode = :postedStatus} gate as
     * QR-FIN-042/043, for the same POL-FIN-009 reason.
     *
     * <p>Returns a projection (A.2.8): the read spans FIN_JOURNAL_LINE_DIM, FIN_JOURNAL_LINE,
     * FIN_JOURNAL_ENTRY, FIN_ACCOUNT and FIN_DIMENSION_VALUE and needs no managed state.
     */
    @Query("SELECT a.accountPk AS accountId, a.code AS accountCode, "
        + "a.nameAr AS accountNameAr, a.nameEn AS accountNameEn, a.natureCode AS natureCode, "
        + "d.dimensionPk AS dimensionId, dv.dimensionValuePk AS dimensionValueId, "
        + "dv.code AS dimensionValueCode, dv.nameAr AS dimensionValueNameAr, "
        + "dv.nameEn AS dimensionValueNameEn, "
        + "SUM(CASE WHEN l.directionCode = :debitCode THEN l.amount ELSE 0 END) AS debitTotal, "
        + "SUM(CASE WHEN l.directionCode = :debitCode THEN 0 ELSE l.amount END) AS creditTotal "
        + "FROM JournalLineDimension jld JOIN jld.journalLine l JOIN l.journalEntry e "
        + "JOIN l.account a JOIN jld.dimension d JOIN jld.dimensionValue dv "
        + "WHERE e.statusCode = :postedStatus AND d.dimensionPk = :dimensionId "
        + "AND (CAST(:dimensionValueId AS Long) IS NULL OR dv.dimensionValuePk = :dimensionValueId) "
        + "AND (CAST(:periodId AS Long) IS NULL OR e.period.fiscalPeriodPk = :periodId) "
        + "GROUP BY a.accountPk, a.code, a.nameAr, a.nameEn, a.natureCode, d.dimensionPk, "
        + "dv.dimensionValuePk, dv.code, dv.nameAr, dv.nameEn "
        + "ORDER BY a.code ASC, dv.code ASC")
    List<DimensionBalanceView> aggregateByAccountAndDimensionValue(
        @Param("postedStatus") String postedStatus,
        @Param("debitCode") String debitCode,
        @Param("dimensionId") Long dimensionId,
        @Param("dimensionValueId") Long dimensionValueId,
        @Param("periodId") Long periodId);
}
