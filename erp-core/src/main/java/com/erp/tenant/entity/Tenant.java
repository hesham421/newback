package com.erp.tenant.entity;

import com.erp.audit.crossmodule.Audited;
import com.erp.common.domain.GlobalAuditableEntity;
import com.erp.tenant.TenantConstants;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.Locale;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * A tenant ({@code CORE_TENANT}, V10). The one tenant-scoped-data owner that is itself global: it
 * has no {@code TENANT_ID} and extends {@link GlobalAuditableEntity}. {@code code} is the immutable
 * natural key clients send as {@code X-Tenant-Code}. State: {@code ACTIVE} / {@code SUSPENDED}
 * (no active flag; {@link #activate(Instant)}/{@link #suspend(Instant, String, String)}, decided by {@code TenantDomain}).
 * Profile (V18), lifecycle facts (V19): tenant-maturity B; branding (V20: logo reference, brand colour): E.
 */
@Entity
@Audited(entityType = "CORE_TENANT")
@Table(name = "CORE_TENANT",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_CORE_TENANT_CODE", columnNames = {"CODE"})
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class Tenant extends GlobalAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "core_tenant_seq")
    @SequenceGenerator(name = "core_tenant_seq", sequenceName = "SEQ_CORE_TENANT", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @NotBlank(message = "{validation.required}")
    @Size(min = 3, max = 32, message = "{validation.size}")
    @Column(name = "CODE", length = 32, nullable = false, updatable = false)
    private String code;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Column(name = "NAME_AR", length = 200, nullable = false)
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Column(name = "NAME_EN", length = 200, nullable = false)
    private String nameEn;

    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Column(name = "STATUS_CODE", length = 20, nullable = false)
    private String statusCode;

    @Size(max = 255, message = "{validation.size}")
    @Column(name = "CONTACT_EMAIL", length = 255)
    private String contactEmail;

    @Size(max = 30, message = "{validation.size}")
    @Column(name = "CONTACT_PHONE", length = 30)
    private String contactPhone;

    @Size(max = 2, message = "{validation.size}")
    @Column(name = "COUNTRY_CODE", length = 2)
    private String countryCode;

    @Size(max = 5, message = "{validation.size}")
    @Column(name = "DEFAULT_LOCALE", length = 5)
    private String defaultLocale;

    @Size(max = 64, message = "{validation.size}")
    @Column(name = "TIMEZONE", length = 64)
    private String timezone;

    @Size(max = 1000, message = "{validation.size}")
    @Column(name = "NOTES", length = 1000)
    private String notes;

    @Column(name = "SUSPENDED_AT")
    private Instant suspendedAt;

    @Size(max = 100, message = "{validation.size}")
    @Column(name = "SUSPENDED_BY", length = 100)
    private String suspendedBy;

    @Size(max = 500, message = "{validation.size}")
    @Column(name = "SUSPENSION_REASON", length = 500)
    private String suspensionReason;

    /** Tokens issued before this instant are refused (set on activation; enforced by tenant-maturity C.2). */
    @Column(name = "TOKENS_INVALID_BEFORE")
    private Instant tokensInvalidBefore;

    /** tenant-maturity E (XM-TENANT-003): the logo, a soft reference (no FK) to a PUBLIC document in this tenant's rows. */
    @Column(name = "LOGO_FILE_ID")
    private Long logoFileId;

    /** tenant-maturity E (RULE-TENANT-021): optional accent colour {@code #RRGGBB}, stored upper case. */
    @Size(max = 7, message = "{validation.size}")
    @Column(name = "BRAND_COLOR", length = 7)
    private String brandColor;

    /** Natural-key normalization, profile normalization and the DB default for the status (A.1.17: the sole site). */
    @PrePersist
    protected void onCreate() {
        if (statusCode == null) {
            statusCode = TenantConstants.STATUS_ACTIVE;
        }
        normalize();
    }

    @PreUpdate
    protected void onUpdate() {
        normalize();
    }

    private void normalize() {
        if (code != null) {
            code = code.toUpperCase(Locale.ROOT);
        }
        contactEmail = trimToNull(contactEmail);
        contactPhone = trimToNull(contactPhone);
        countryCode = trimToNull(countryCode);
        if (countryCode != null) {
            countryCode = countryCode.toUpperCase(Locale.ROOT);
        }
        defaultLocale = trimToNull(defaultLocale);
        timezone = trimToNull(timezone);
        notes = trimToNull(notes);
        suspensionReason = trimToNull(suspensionReason);
        brandColor = trimToNull(brandColor);
        if (brandColor != null) {
            brandColor = brandColor.toUpperCase(Locale.ROOT);
        }
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * Field mutation only — whether the transition is allowed is decided by {@code TenantDomain}
     * (RULE-TENANT-016): clears the suspension facts and sets the token cut-off.
     */
    public void activate(Instant at) {
        this.statusCode = TenantConstants.STATUS_ACTIVE;
        this.suspendedAt = null;
        this.suspendedBy = null;
        this.suspensionReason = null;
        this.tokensInvalidBefore = at;
    }

    /** Field mutation only — whether the transition is allowed is decided by {@code TenantDomain} (RULE-TENANT-016). */
    public void suspend(Instant at, String by, String reason) {
        this.statusCode = TenantConstants.STATUS_SUSPENDED;
        this.suspendedAt = at;
        this.suspendedBy = by;
        this.suspensionReason = reason;
    }
}
