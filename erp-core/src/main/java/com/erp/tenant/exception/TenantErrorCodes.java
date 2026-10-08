package com.erp.tenant.exception;

/**
 * Error codes of the tenant module (erp-core step 05). Descriptive {@code TENANT_<SCENARIO>} form;
 * every code has an entry in {@code i18n/messages.properties} and {@code messages_ar.properties}.
 */
public final class TenantErrorCodes {

    private TenantErrorCodes() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** 400 — a request that needs a tenant carried neither a token ({@code tid}) nor {@code X-Tenant-Code}. */
    public static final String TENANT_REQUIRED = "TENANT_REQUIRED";

    /** 404 — no tenant with that id / code. */
    public static final String TENANT_NOT_FOUND = "TENANT_NOT_FOUND";

    /** 403 — the tenant is suspended: no login, no API call. */
    public static final String TENANT_SUSPENDED = "TENANT_SUSPENDED";

    /** 500 — code ran against tenant-scoped data without a tenant (a missing {@code TenantContext.runAs}). */
    public static final String TENANT_CONTEXT_MISSING = "TENANT_CONTEXT_MISSING";

    /** 400 — a tenant code that does not match {@code ^[A-Z0-9_]{3,32}$}. */
    public static final String TENANT_CODE_INVALID = "TENANT_CODE_INVALID";

    /** 409 — a tenant with that code already exists. */
    public static final String TENANT_CODE_DUPLICATE = "TENANT_CODE_DUPLICATE";

    /** 422 — the PLATFORM tenant cannot be suspended (it hosts the platform operators). */
    public static final String TENANT_PLATFORM_PROTECTED = "TENANT_PLATFORM_PROTECTED";

    /** 400 — tenant-maturity B (RULE-TENANT-016): a suspension without a reason of 3 to 500 characters. */
    public static final String TENANT_SUSPENSION_REASON_REQUIRED = "TENANT_SUSPENSION_REASON_REQUIRED";

    /**
     * 422 — tenant-maturity B review round 1 (RULE-TENANT-017): admin-reset never targets the PLATFORM tenant;
     * platform operators set each other's passwords through SEC, where RULE-SEC-057 refuses one's own account.
     */
    public static final String TENANT_ADMIN_RESET_PLATFORM = "TENANT_ADMIN_RESET_PLATFORM";

    /** 404 — tenant-maturity B (RULE-TENANT-017): admin-reset names no STAFF user of that tenant. */
    public static final String TENANT_ADMIN_NOT_FOUND = "TENANT_ADMIN_NOT_FOUND";

    /** 422 — tenant-maturity B (RULE-TENANT-017): the admin-reset target holds no active super role. */
    public static final String TENANT_ADMIN_NOT_SUPER = "TENANT_ADMIN_NOT_SUPER";

    /** 400 — tenant-maturity E (RULE-TENANT-018): the logo is not a PNG, JPEG, WebP or plain SVG of at most 1 MB. */
    public static final String TENANT_LOGO_INVALID = "TENANT_LOGO_INVALID";

    /** 400 — tenant-maturity E (RULE-TENANT-021): a brand colour that is not {@code #RRGGBB}. */
    public static final String TENANT_BRAND_COLOR_INVALID = "TENANT_BRAND_COLOR_INVALID";

    /** 429 — tenant-maturity E (RULE-TENANT-022): too many public branding requests from one client address. */
    public static final String TENANT_BRANDING_RATE_LIMITED = "TENANT_BRANDING_RATE_LIMITED";

    /** 401 — tenant-maturity C12 (RULE-TENANT-023): the token was issued before its tenant's token cut-off. */
    public static final String TENANT_TOKEN_REVOKED = "TENANT_TOKEN_REVOKED";

    /** 422 — tenant-maturity C12 (RULE-TENANT-024): revoke-tokens never targets the PLATFORM tenant. */
    public static final String TENANT_REVOKE_TOKENS_PLATFORM = "TENANT_REVOKE_TOKENS_PLATFORM";
}
