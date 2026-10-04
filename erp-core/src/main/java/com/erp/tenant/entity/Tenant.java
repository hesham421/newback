package com.erp.tenant.entity;

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
 * (there is no active flag; {@link #activate()}/{@link #suspend()} are the two transitions, decided
 * by {@code TenantDomain}).
 */
@Entity
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

    /** Natural-key normalization and the DB default for the status (A.1.17: the sole site). */
    @PrePersist
    protected void onCreate() {
        if (statusCode == null) {
            statusCode = TenantConstants.STATUS_ACTIVE;
        }
        if (code != null) {
            code = code.toUpperCase(Locale.ROOT);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (code != null) {
            code = code.toUpperCase(Locale.ROOT);
        }
    }

    /** Field mutation only — whether the transition is allowed is decided by {@code TenantDomain}. */
    public void activate() {
        this.statusCode = TenantConstants.STATUS_ACTIVE;
    }

    /** Field mutation only — whether the transition is allowed is decided by {@code TenantDomain}. */
    public void suspend() {
        this.statusCode = TenantConstants.STATUS_SUSPENDED;
    }
}
