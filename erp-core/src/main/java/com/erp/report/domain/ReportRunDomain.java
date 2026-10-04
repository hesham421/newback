package com.erp.report.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.CommonErrorCodes;
import com.erp.common.exception.LocalizedException;
import com.erp.report.exception.ReportErrorCodes;
import java.util.List;
import java.util.Locale;

/**
 * The run/export decisions of the reporting module (erp-core step 11) — "may this caller run this
 * report?", "is this export within the cap?", "is this export format supported?". Plain class: the
 * service resolves the facts (does the caller hold the report's authority, how many rows came back)
 * and passes them in.
 */
public final class ReportRunDomain {

    /** Export formats of {@code POST /api/v1/report/{code}/export?format=...}. */
    public enum ExportFormat {
        CSV("text/csv;charset=UTF-8", "csv"),
        JSON("application/json", "json");

        private final String contentType;
        private final String extension;

        ExportFormat(String contentType, String extension) {
            this.contentType = contentType;
            this.extension = extension;
        }

        public String contentType() {
            return contentType;
        }

        public String extension() {
            return extension;
        }
    }

    private ReportRunDomain() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** A caller may see and run a report only while holding its {@code <MODULE>:REPORT:<CODE>} authority. */
    public static void assertCanRun(boolean callerHoldsReportAuthority) {
        if (!callerHoldsReportAuthority) {
            throw new LocalizedException(Status.FORBIDDEN, CommonErrorCodes.ACCESS_DENIED);
        }
    }

    /**
     * An export may contain at most {@code maxRows} rows. The provider was asked for {@code maxRows + 1}
     * rows; {@code totalRows} is its whole-result count when it knows it.
     */
    public static void assertWithinExportCap(int returnedRows, Long totalRows, int maxRows) {
        long rows = totalRows != null ? Math.max(totalRows, returnedRows) : returnedRows;
        if (rows > maxRows) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION, ReportErrorCodes.REPORT_EXPORT_TOO_LARGE,
                String.valueOf(maxRows));
        }
    }

    /** {@code csv} (default when blank) or {@code json}, case-insensitive; anything else is 400. */
    public static ExportFormat exportFormat(String format) {
        if (format == null || format.isBlank()) {
            return ExportFormat.CSV;
        }
        String normalized = format.trim().toUpperCase(Locale.ROOT);
        for (ExportFormat candidate : ExportFormat.values()) {
            if (candidate.name().equals(normalized)) {
                return candidate;
            }
        }
        throw new LocalizedException(Status.VALIDATION_ERROR, List.of(ReportParametersDomain.invalid("format")));
    }
}
