package com.erp.notif.report;

import com.erp.common.util.UtcDates;
import com.erp.notif.repository.NotificationLogSummaryRepository;
import com.erp.report.ColumnType;
import com.erp.report.ParamType;
import com.erp.report.ReportAuthorities;
import com.erp.report.ReportColumn;
import com.erp.report.ReportParam;
import com.erp.report.ReportProvider;
import com.erp.report.ReportResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core reference report {@code NOTIF_LOG_SUMMARY} (erp-core step 11): the tenant's notification log
 * grouped by channel, status and day, with counts and a grand total. Channel and status are
 * {@code LOOKUP} parameters (MDL types {@code NOTIF_CHANNEL} / {@code NOTIF_STATUS}); the day window
 * defaults to everything. Queried with JPQL through {@link NotificationLogSummaryRepository}
 * (tenant-filtered by Hibernate). The aggregate is small (channels × statuses × days), so it is read
 * whole and paged in memory, which also gives the exact {@code totalRows}.
 */
@Component
@RequiredArgsConstructor
public class NotifLogSummaryReport implements ReportProvider {

    public static final String CODE = "NOTIF_LOG_SUMMARY";
    public static final String MODULE = "NOTIF";
    /** {@code NOTIF:REPORT:NOTIF_LOG_SUMMARY}. */
    public static final String AUTHORITY = ReportAuthorities.of(MODULE, CODE);

    private static final Instant BEGINNING = UtcDates.startOfDay(LocalDate.of(1970, 1, 1));
    private static final Instant END = UtcDates.startOfDay(LocalDate.of(9999, 1, 1));

    private static final List<ReportColumn> COLUMNS = List.of(
        new ReportColumn("day", ColumnType.DATE, "اليوم", "Day"),
        new ReportColumn("channel", ColumnType.STRING, "القناة", "Channel"),
        new ReportColumn("status", ColumnType.STRING, "الحالة", "Status"),
        new ReportColumn("count", ColumnType.INTEGER, "العدد", "Count"));

    private final NotificationLogSummaryRepository summaryRepository;

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
        return "ملخص سجل الإشعارات";
    }

    @Override
    public String titleEn() {
        return "Notification log summary";
    }

    @Override
    public List<ReportParam> params() {
        return List.of(
            ReportParam.lookup("channel", "NOTIF_CHANNEL", false, "القناة", "Channel"),
            ReportParam.lookup("status", "NOTIF_STATUS", false, "الحالة", "Status"),
            ReportParam.of("dateFrom", ParamType.DATE, false, "من تاريخ", "Date from"),
            ReportParam.of("dateTo", ParamType.DATE, false, "حتى تاريخ", "Date to"));
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority(T(com.erp.notif.report.NotifLogSummaryReport).AUTHORITY)")
    public ReportResult run(Map<String, Object> params, Pageable page) {
        Instant from = params.get("dateFrom") instanceof LocalDate day ? UtcDates.startOfDay(day) : BEGINNING;
        Instant to = params.get("dateTo") instanceof LocalDate day ? UtcDates.startOfDay(day.plusDays(1)) : END;
        List<Map<String, Object>> groups = summaryRepository.summarize(text(params.get("channel")),
                text(params.get("status")), from, to).stream()
            .map(NotifLogSummaryReport::toRow)
            .toList();

        long total = groups.stream().mapToLong(row -> (Long) row.get("count")).sum();
        int start = (int) Math.min((long) page.getPageNumber() * page.getPageSize(), groups.size());
        int end = (int) Math.min((long) start + page.getPageSize(), groups.size());
        return new ReportResult(COLUMNS, groups.subList(start, end), Map.of("count", total), (long) groups.size());
    }

    private static Map<String, Object> toRow(Object[] group) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("day", group[2]);
        row.put("channel", group[0]);
        row.put("status", group[1]);
        row.put("count", ((Number) group[3]).longValue());
        return row;
    }

    private static String text(Object value) {
        return value == null ? "" : value.toString();
    }

}
