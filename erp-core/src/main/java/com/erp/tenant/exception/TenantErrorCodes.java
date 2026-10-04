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
}
