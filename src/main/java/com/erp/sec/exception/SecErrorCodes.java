package com.erp.sec.exception;

/**
 * Module-specific error codes for Security (SEC) — one constant per row of the SEC v1 §Error
 * Catalog. The constant value is the catalog code verbatim, and is both the wire {@code code} in
 * the {@code ApiError} envelope and the i18n message key. Constant names replace the catalog's
 * hyphens with underscores — see governance/project-artifacts/sec-implementation-notes.md.
 */
public final class SecErrorCodes {

    private SecErrorCodes() {
        throw new UnsupportedOperationException("Utility class \u2014 cannot be instantiated");
    }

    /**
     * PLATFORM-STD (ADR-SEC-002) — wrong/unknown credentials.
     * API: API-SEC-001. HTTP 401.
     */
    public static final String SEC_401_INVALID_CREDENTIALS = "SEC-401-INVALID-CREDENTIALS";

    /**
     * PLATFORM-STD (uniqueness) — duplicate username/email.
     * API: API-SEC-006, 007. HTTP 409.
     */
    public static final String SEC_409_USER_DUP = "SEC-409-USER-DUP";

    /**
     * PLATFORM-STD (not found) — unknown user id.
     * API: API-SEC-007, 008, 009, 010. HTTP 404.
     */
    public static final String SEC_404_USER = "SEC-404-USER";

    /**
     * RULE-SEC-005 — conflicting action pair on one user.
     * API: API-SEC-008, 017. HTTP 409.
     */
    public static final String SEC_409_SOD_CONFLICT = "SEC-409-SOD-CONFLICT";

    /**
     * PLATFORM-STD (not found) — unknown role id.
     * API: API-SEC-008, 014, 016, 017. HTTP 404.
     */
    public static final String SEC_404_ROLE = "SEC-404-ROLE";

    /**
     * PLATFORM-STD (lifecycle) — transition not allowed from current status.
     * API: API-SEC-010, 011. HTTP 409.
     */
    public static final String SEC_409_INVALID_TRANSITION = "SEC-409-INVALID-TRANSITION";

    /**
     * PLATFORM-STD (not found) — unknown signup request id.
     * API: API-SEC-011. HTTP 404.
     */
    public static final String SEC_404_SIGNUP = "SEC-404-SIGNUP";

    /**
     * PLATFORM-STD (uniqueness) — duplicate role code.
     * API: API-SEC-013. HTTP 409.
     */
    public static final String SEC_409_ROLE_DUP = "SEC-409-ROLE-DUP";

    /**
     * PLATFORM-STD (uniqueness) — grant already exists.
     * API: API-SEC-014, 016, 017. HTTP 409.
     */
    public static final String SEC_409_GRANT_DUP = "SEC-409-GRANT-DUP";

    /**
     * PLATFORM-STD (not found) — unknown module id.
     * API: API-SEC-014, 015. HTTP 404.
     */
    public static final String SEC_404_MODULE = "SEC-404-MODULE";

    /**
     * PLATFORM-STD (not found) — grant does not exist.
     * API: API-SEC-015. HTTP 404.
     */
    public static final String SEC_404_GRANT = "SEC-404-GRANT";

    /**
     * RULE-SEC-001 — screen grant attempted without module grant.
     * API: API-SEC-016. HTTP 409.
     */
    public static final String SEC_409_NO_MODULE_GRANT = "SEC-409-NO-MODULE-GRANT";

    /**
     * PLATFORM-STD (not found) — unknown screen id / page code.
     * API: API-SEC-016, 017, 019. HTTP 404.
     */
    public static final String SEC_404_SCREEN = "SEC-404-SCREEN";

    /**
     * RULE-SEC-002 — action grant attempted without screen grant.
     * API: API-SEC-017. HTTP 409.
     */
    public static final String SEC_409_NO_SCREEN_GRANT = "SEC-409-NO-SCREEN-GRANT";

    /**
     * RULE-SEC-007 — non-VIEW action grant attempted without VIEW.
     * API: API-SEC-017. HTTP 409.
     */
    public static final String SEC_409_NO_VIEW_GRANT = "SEC-409-NO-VIEW-GRANT";

    /**
     * PLATFORM-STD (not found) — unknown action id.
     * API: API-SEC-017. HTTP 404.
     */
    public static final String SEC_404_ACTION = "SEC-404-ACTION";

    /**
     * PLATFORM-STD (uniqueness) — duplicate module code.
     * API: API-SEC-018. HTTP 409.
     */
    public static final String SEC_409_MODULE_DUP = "SEC-409-MODULE-DUP";

    /**
     * RULE-SEC-004 — screen registered under an unregistered module.
     * API: API-SEC-019. HTTP 409.
     */
    public static final String SEC_409_MODULE_NOT_REGISTERED = "SEC-409-MODULE-NOT-REGISTERED";

    /**
     * PLATFORM-STD (uniqueness) — duplicate page code.
     * API: API-SEC-019. HTTP 409.
     */
    public static final String SEC_409_SCREEN_DUP = "SEC-409-SCREEN-DUP";

    /**
     * PLATFORM-STD (referential) — action registered under an unregistered screen.
     * API: API-SEC-020. HTTP 409.
     */
    public static final String SEC_409_SCREEN_NOT_REGISTERED = "SEC-409-SCREEN-NOT-REGISTERED";

    /**
     * PLATFORM-STD (uniqueness) — duplicate permission code.
     * API: API-SEC-020. HTTP 409.
     */
    public static final String SEC_409_ACTION_DUP = "SEC-409-ACTION-DUP";

    /**
     * RULE-SEC-006 — expired or used reset token.
     * API: API-SEC-004. HTTP 409.
     */
    public static final String SEC_409_RESET_TOKEN_INVALID = "SEC-409-RESET-TOKEN-INVALID";

    /**
     * PLATFORM-STD (uniqueness) — duplicate pending/registered email.
     * API: API-SEC-002. HTTP 409.
     */
    public static final String SEC_409_SIGNUP_DUP = "SEC-409-SIGNUP-DUP";

    /**
     * PLATFORM-STD (not found) — unknown session id.
     * API: API-SEC-026. HTTP 404.
     */
    public static final String SEC_404_SESSION = "SEC-404-SESSION";

    /**
     * PLATFORM-STD (lifecycle) — session already terminated.
     * API: API-SEC-026. HTTP 409.
     */
    public static final String SEC_409_ALREADY_TERMINATED = "SEC-409-ALREADY-TERMINATED";

    /**
     * PLATFORM-STD (RULE-SEC-007 + REQ-SEC-033, CORE interceptor) — missing module/screen/action grant.
     * API: every secured API. HTTP 403.
     */
    public static final String SEC_403_FORBIDDEN = "SEC-403-FORBIDDEN";

    /**
     * PLATFORM-STD (search contract) — unrecognized {@code sort} field.
     * API: every search API. HTTP 400.
     */
    public static final String SEC_400_INVALID_SORT = "SEC-400-INVALID-SORT";
}
