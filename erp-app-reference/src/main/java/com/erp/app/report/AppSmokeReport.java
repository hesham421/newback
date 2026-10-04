package com.erp.app.report;

import com.erp.report.ColumnType;
import com.erp.report.ParamType;
import com.erp.report.ReportAuthorities;
import com.erp.report.ReportColumn;
import com.erp.report.ReportParam;
import com.erp.report.ReportProvider;
import com.erp.report.ReportResult;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;

/**
 * {@code APP_SMOKE_REPORT} — proves that an application registers its own report with erp-core's
 * reporting SPI just by exposing a {@link ReportProvider} bean (erp-core step 11): it appears in
 * {@code GET /api/v1/report/definitions}, its permission {@code APP:REPORT:APP_SMOKE_REPORT} reaches the
 * RBAC catalog, and it runs and exports through the core endpoints. It returns a small fixed table (no
 * database access), optionally filtered by {@code label}.
 */
@Component
public class AppSmokeReport implements ReportProvider {

    public static final String CODE = "APP_SMOKE_REPORT";
    public static final String MODULE = "APP";
    /** {@code APP:REPORT:APP_SMOKE_REPORT}. */
    public static final String AUTHORITY = ReportAuthorities.of(MODULE, CODE);

    private static final List<ReportColumn> COLUMNS = List.of(
        new ReportColumn("id", ColumnType.INTEGER, "المعرف", "Id"),
        new ReportColumn("label", ColumnType.STRING, "التسمية", "Label"));

    @Override
    public String code() {
        return CODE;
    }

    @Override
    public String moduleCode() {
        return MODULE;
    }

    @Override
    public String titleAr() {
        return "تقرير اختبار التطبيق";
    }

    @Override
    public String titleEn() {
        return "Application smoke report";
    }

    @Override
    public List<ReportParam> params() {
        return List.of(ReportParam.of("label", ParamType.STRING, false, "التسمية", "Label"));
    }

    @Override
    @PreAuthorize("hasAuthority(T(com.erp.app.report.AppSmokeReport).AUTHORITY)")
    public ReportResult run(Map<String, Object> params, Pageable page) {
        Object label = params.get("label");
        List<Map<String, Object>> rows = List.of(row(1, "alpha"), row(2, "بيتا"), row(3, "gamma, delta")).stream()
            .filter(row -> label == null || row.get("label").equals(label))
            .toList();
        int start = (int) Math.min((long) page.getPageNumber() * page.getPageSize(), rows.size());
        int end = (int) Math.min((long) start + page.getPageSize(), rows.size());
        return new ReportResult(COLUMNS, rows.subList(start, end), Map.of(), (long) rows.size());
    }

    private static Map<String, Object> row(long id, String label) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", id);
        row.put("label", label);
        return row;
    }
}
