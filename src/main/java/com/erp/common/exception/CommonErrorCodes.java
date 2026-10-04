package com.erp.common.exception;

public final class CommonErrorCodes {

    private CommonErrorCodes() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static final String ACCESS_DENIED = "ACCESS_DENIED";
    public static final String DATA_INTEGRITY_VIOLATION = "DATA_INTEGRITY_VIOLATION";

    // Added 2026-09-12 by an explicit recorded human decision, not by any pre-existing
    // requirement: no artifact registered a platform code for HTTP 405. Introduced so that an
    // unsupported HTTP method reports 405 instead of the 500 the catch-all handler previously
    // produced (found by the FIN api-verify run, TC-FIN-016).
    public static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";

    // Added 2026-09-19: the per-field detail code behind a VALIDATION_ERROR raised by
    // SpecBuilder when a search body names a filter field the endpoint does not support. It is a
    // fieldErrors[] entry only — the envelope's top-level code stays VALIDATION_ERROR, so no new
    // wire code is introduced. See SpecBuilder.assertFieldsAllowed.
    public static final String UNSUPPORTED_FILTER_FIELD = "UNSUPPORTED_FILTER_FIELD";

    /** Same slot and same rules, for a supported field combined with an operator it cannot honour. */
    public static final String UNSUPPORTED_FILTER_OPERATOR = "UNSUPPORTED_FILTER_OPERATOR";
}
