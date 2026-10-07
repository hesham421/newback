package com.erp.tenant.domain;

import com.erp.common.domain.DomainRules;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.tenant.TenantConstants;
import com.erp.tenant.entity.Tenant;
import com.erp.tenant.exception.TenantErrorCodes;
import java.util.regex.Pattern;

/**
 * The tenant's business rules: code format and uniqueness on create, and which status transitions
 * are allowed. No Spring/JPA annotations, no repository; the service passes every fact in.
 */
public final class TenantDomain {

    /** Tenant code format (step 05): upper-case letters, digits, underscore; 3 to 32 characters. */
    public static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9_]{3,32}$");

    private final Long id;
    private final String code;
    private final String statusCode;

    private TenantDomain(Long id, String code, String statusCode) {
        this.id = id;
        this.code = code;
        this.statusCode = statusCode;
    }

    /**
     * Construction-time rules: the code must match {@link #CODE_PATTERN} as given (it is not
     * upper-cased for the client) and must not be taken.
     */
    public static TenantDomain create(String code, boolean codeAlreadyTaken) {
        if (code == null || !CODE_PATTERN.matcher(code).matches()) {
            throw new LocalizedException(Status.VALIDATION_ERROR, TenantErrorCodes.TENANT_CODE_INVALID, code);
        }
        DomainRules.assertUnique(codeAlreadyTaken, TenantErrorCodes.TENANT_CODE_DUPLICATE, code);
        return new TenantDomain(null, code, TenantConstants.STATUS_ACTIVE);
    }

    /** A Domain view over a persisted tenant. */
    public static TenantDomain from(Tenant tenant) {
        return new TenantDomain(tenant.getId(), tenant.getCode(), tenant.getStatusCode());
    }

    /**
     * Whether the tenant may move to {@code targetStatus} ({@code ACTIVE} or {@code SUSPENDED}; the
     * request DTO already restricts the value). Re-applying the current status is allowed (idempotent).
     * The PLATFORM tenant can never be suspended: it hosts the platform operators, and suspending it
     * would lock everyone out of tenant management.
     */
    public void assertCanChangeStatusTo(String targetStatus) {
        if (TenantConstants.STATUS_SUSPENDED.equals(targetStatus)
            && Long.valueOf(TenantConstants.PLATFORM_TENANT_ID).equals(id)) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                TenantErrorCodes.TENANT_PLATFORM_PROTECTED, code);
        }
    }

    /** Whether requests of this tenant are served (login and every API call). */
    public boolean isActive() {
        return TenantConstants.STATUS_ACTIVE.equals(statusCode);
    }

    public String getCode() {
        return code;
    }
}
