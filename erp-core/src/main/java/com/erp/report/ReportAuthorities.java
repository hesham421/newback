package com.erp.report;

/**
 * The permission code of a report (erp-core step 11): {@code <MODULE>:REPORT:<CODE>}, e.g.
 * {@code SEC:REPORT:SEC_USER_LIST}. Contributed to the RBAC catalog automatically for every
 * {@link ReportProvider}; a provider may reference it in its own {@code @PreAuthorize} through a
 * constant of its own class ({@code public static final String AUTHORITY = ReportAuthorities.of(...)}).
 */
public final class ReportAuthorities {

    /** The action segment of every report permission. */
    public static final String REPORT_SEGMENT = "REPORT";

    private ReportAuthorities() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** {@code <MODULE>:REPORT:<CODE>}. */
    public static String of(String moduleCode, String reportCode) {
        return moduleCode + ":" + REPORT_SEGMENT + ":" + reportCode;
    }

    /** The authority of {@code provider}. */
    public static String of(ReportProvider provider) {
        return of(provider.moduleCode(), provider.code());
    }
}
