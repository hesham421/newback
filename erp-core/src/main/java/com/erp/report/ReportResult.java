package com.erp.report;

import java.util.List;
import java.util.Map;

/**
 * A tabular report result (erp-core step 11): the columns, the rows of the requested page (each row a
 * map keyed by {@link ReportColumn#key()}) and optional totals over the whole result.
 *
 * <p>{@link #totalRows()} is an addition to the step file's three components: the number of rows of
 * the whole result (all pages), so the run endpoint can report paging and the export endpoint can
 * enforce its row cap without loading everything. {@code null} means "unknown" — use the three-argument
 * constructor; the service then relies on the returned rows alone.
 *
 * @param columns   output columns, in display order
 * @param rows      rows of the requested page
 * @param totals    totals over the whole result keyed by column key (empty when none)
 * @param totalRows number of rows of the whole result, or {@code null} when unknown
 */
public record ReportResult(List<ReportColumn> columns, List<Map<String, Object>> rows,
                           Map<String, Object> totals, Long totalRows) {

    public ReportResult {
        columns = columns == null ? List.of() : List.copyOf(columns);
        rows = rows == null ? List.of() : rows;
        totals = totals == null ? Map.of() : totals;
    }

    /** The step file's shape: total row count unknown. */
    public ReportResult(List<ReportColumn> columns, List<Map<String, Object>> rows, Map<String, Object> totals) {
        this(columns, rows, totals, null);
    }
}
