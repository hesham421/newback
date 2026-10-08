package com.erp.tenant.domain;

import com.erp.common.domain.DomainRules;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.ErrorDetail;
import com.erp.common.exception.LocalizedException;
import com.erp.tenant.TenantConstants;
import com.erp.tenant.entity.Tenant;
import com.erp.tenant.exception.TenantErrorCodes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The tenant's business rules: code format and uniqueness on create, which status transitions are
 * allowed and with which reason (RULE-TENANT-016), who may be recovered by the platform's admin-reset
 * (RULE-TENANT-017), the branding rules (RULE-TENANT-018, -021; tenant-maturity E) and the token cut-off and its
 * revocation (RULE-TENANT-023, -024; C12), the data export's limit and single run (RULE-TENANT-027, -028; C5). No
 * Spring/JPA annotations, no repository; the service passes every fact in.
 */
public final class TenantDomain {

    /** Tenant code format (step 05): upper-case letters, digits, underscore; 3 to 32 characters. */
    public static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9_]{3,32}$");

    /** RULE-TENANT-016: bounds of a suspension reason, after trimming. */
    public static final int REASON_MIN_LENGTH = 3;
    public static final int REASON_MAX_LENGTH = 500;

    /** RULE-TENANT-018: the logo is a FILE image-store document of this owner type, owner id = the tenant id. */
    public static final String LOGO_OWNER_TYPE = "CORE_TENANT";
    public static final String LOGO_MODULE_CODE = "TENANT";
    public static final String LOGO_BASE_NAME = "logo";
    public static final long LOGO_MAX_BYTES = 1_048_576L;

    /** RULE-TENANT-018: PNG, JPEG, WebP and (sanitised by FILE, RULE-FILE-009) SVG. */
    public static final Set<String> LOGO_TYPES = Set.of("image/png", "image/jpeg", "image/webp", "image/svg+xml");

    /** RULE-TENANT-021: an accent colour {@code #RRGGBB} (the database repeats it as {@code CHK_CORE_TENANT_BRAND_COLOR}). */
    public static final Pattern BRAND_COLOR_PATTERN = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    /** RULE-TENANT-027: the export archive is a PRIVATE FILE document of PLATFORM with this owner type, owner id = the tenant id. */
    public static final String EXPORT_OWNER_TYPE = "CORE_TENANT";
    public static final String EXPORT_MODULE_CODE = "TENANT";

    private static final String FIELD_LOGO_FILE = "file";
    private static final String FIELD_BRAND_COLOR = "brandColor";

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

    /**
     * RULE-TENANT-016 — a suspension needs a reason of {@value #REASON_MIN_LENGTH}..{@value #REASON_MAX_LENGTH}
     * characters after trimming; the reason of an activation is ignored. Called after
     * {@link #assertCanChangeStatusTo(String)}, so PLATFORM's protection answers first.
     */
    public void assertSuspensionReasonGiven(String targetStatus, String reason) {
        if (!TenantConstants.STATUS_SUSPENDED.equals(targetStatus)) {
            return;
        }
        int length = reason == null ? 0 : reason.strip().length();
        if (length < REASON_MIN_LENGTH || length > REASON_MAX_LENGTH) {
            throw new LocalizedException(Status.VALIDATION_ERROR, TenantErrorCodes.TENANT_SUSPENSION_REASON_REQUIRED);
        }
    }

    /**
     * Whether moving to {@code targetStatus} is a transition. Re-applying the current status changes
     * nothing — neither the status nor the suspension facts nor the token cut-off (RULE-TENANT-004, -016).
     */
    public boolean changesStatusTo(String targetStatus) {
        return !targetStatus.equals(statusCode);
    }

    /**
     * RULE-TENANT-017 (review round 1) — the platform's admin-reset never runs on the PLATFORM tenant itself: there it
     * would let an operator reset their own password without the current one (SEC RULE-SEC-057).
     */
    public void assertAdminResetAllowed() {
        if (Long.valueOf(TenantConstants.PLATFORM_TENANT_ID).equals(id)) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                TenantErrorCodes.TENANT_ADMIN_RESET_PLATFORM, code);
        }
    }

    /**
     * RULE-TENANT-024 (tenant-maturity C12) — revoke-tokens never runs on the PLATFORM tenant: it would sign every
     * platform operator out, the caller included. Any other tenant, ACTIVE or SUSPENDED, may be revoked.
     */
    public void assertTokenRevocationAllowed() {
        if (Long.valueOf(TenantConstants.PLATFORM_TENANT_ID).equals(id)) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                TenantErrorCodes.TENANT_REVOKE_TOKENS_PLATFORM, code);
        }
    }

    /**
     * RULE-TENANT-023 (review round 1) — revoke-tokens' cut-off: the start of the next whole second after {@code now}, so
     * the cut-off alone refuses every token issued up to and including the revoke's own second.
     */
    public static Instant revocationCutOff(Instant now) {
        return now.truncatedTo(ChronoUnit.SECONDS).plusSeconds(1);
    }

    /**
     * RULE-TENANT-023 (ADR-TENANT-002) — a token is revoked when the tenant has a cut-off and the token's {@code iat}
     * (whole seconds) lies before the cut-off truncated to the second: a token of the cut-off's own second is served,
     * one without {@code iat} is not.
     */
    public static boolean isTokenRevoked(Instant issuedAt, Instant tokensInvalidBefore) {
        if (tokensInvalidBefore == null) {
            return false;
        }
        return issuedAt == null || issuedAt.getEpochSecond() < tokensInvalidBefore.getEpochSecond();
    }

    /**
     * RULE-TENANT-017 — the platform's admin-reset only targets a STAFF user of this tenant holding an
     * active super role. Both facts are computed by SEC inside this tenant and passed in by the service.
     */
    public void assertCanResetAdministrator(String username, boolean staffUserExists, boolean holdsSuperRole) {
        if (!staffUserExists) {
            throw new LocalizedException(Status.NOT_FOUND, TenantErrorCodes.TENANT_ADMIN_NOT_FOUND, username, code);
        }
        if (!holdsSuperRole) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                TenantErrorCodes.TENANT_ADMIN_NOT_SUPER, username, code);
        }
    }

    /**
     * RULE-TENANT-027 — a tenant export holds at most {@code maxRows} rows ({@code erp.core.tenant.export.max-rows}): more is
     * 422 {@code TENANT_EXPORT_TOO_LARGE}, before anything is written (and again if a contributor writes past the limit).
     */
    public static void assertExportWithinLimit(long rows, long maxRows) {
        if (rows > maxRows) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION, TenantErrorCodes.TENANT_EXPORT_TOO_LARGE,
                rows, maxRows);
        }
    }

    /** RULE-TENANT-028 — one export of a tenant at a time on this node: the slot was taken, so 409 {@code TENANT_EXPORT_IN_PROGRESS}. */
    public static void assertExportStartable(boolean started, String tenantCode) {
        if (!started) {
            throw new LocalizedException(Status.CONFLICT, TenantErrorCodes.TENANT_EXPORT_IN_PROGRESS, tenantCode);
        }
    }

    /**
     * RULE-TENANT-018 — FILE refused the image (empty, over {@link #LOGO_MAX_BYTES}, not one of {@link #LOGO_TYPES}
     * by its bytes, or an unsafe SVG): 400 {@code TENANT_LOGO_INVALID} on field {@code file}; nothing was stored.
     */
    public static void assertLogoAccepted(boolean accepted) {
        if (!accepted) {
            throw new LocalizedException(Status.VALIDATION_ERROR,
                List.of(ErrorDetail.ofField(FIELD_LOGO_FILE, TenantErrorCodes.TENANT_LOGO_INVALID)));
        }
    }

    /**
     * RULE-TENANT-021 — a brand colour is absent (null or blank: it clears the colour) or, trimmed, matches
     * {@link #BRAND_COLOR_PATTERN}; anything else is 400 {@code TENANT_BRAND_COLOR_INVALID} on field {@code brandColor}.
     */
    public static void assertBrandColorValid(String brandColor) {
        if (brandColor != null && !brandColor.isBlank() && !BRAND_COLOR_PATTERN.matcher(brandColor.strip()).matches()) {
            throw new LocalizedException(Status.VALIDATION_ERROR,
                List.of(ErrorDetail.ofField(FIELD_BRAND_COLOR, TenantErrorCodes.TENANT_BRAND_COLOR_INVALID, brandColor)));
        }
    }

    /**
     * RULE-TENANT-006 — the branding reads ({@code /tenant/me}, the public branding) serve an ACTIVE tenant only;
     * the tenant filter refuses a suspended one first, this repeats it where the read happens (403 {@code TENANT_SUSPENDED}).
     */
    public void assertServed() {
        if (!isActive()) {
            throw new LocalizedException(Status.FORBIDDEN, TenantErrorCodes.TENANT_SUSPENDED);
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
