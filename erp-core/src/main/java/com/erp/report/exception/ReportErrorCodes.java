package com.erp.report.exception;

/**
 * Module-specific error codes of the reporting module (erp-core step 11). Descriptive
 * {@code <ENTITY>_<SCENARIO>} format; every code has an EN ({@code messages.properties}) and an AR
 * ({@code messages_ar.properties}) message. A caller lacking a report's permission gets the shared
 * {@code CommonErrorCodes.ACCESS_DENIED} (403).
 */
public final class ReportErrorCodes {

    private ReportErrorCodes() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** 404 — no report with this code is registered. Argument: the code. */
    public static final String REPORT_NOT_FOUND = "REPORT_NOT_FOUND";

    /**
     * 400 — a parameter is missing, of the wrong type, not an active lookup value or not declared; also
     * an unsupported export format. Argument: the parameter name. Every failing parameter is listed in
     * {@code fieldErrors}.
     */
    public static final String REPORT_PARAM_INVALID = "REPORT_PARAM_INVALID";

    /** 422 — the export would exceed {@code erp.core.report.max-export-rows}. Argument: the cap. */
    public static final String REPORT_EXPORT_TOO_LARGE = "REPORT_EXPORT_TOO_LARGE";
}
