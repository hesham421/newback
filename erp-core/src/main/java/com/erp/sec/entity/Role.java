package com.erp.sec.entity;

import com.erp.audit.crossmodule.Audited;
import com.erp.common.domain.AuditableEntity;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ENT-SEC-002 — Role (SEC_ROLE). Source: db-script-sec.md §1 DBF-SEC-014..024 / §3 BLOCK 2,
 * DATA-DOM-MASTER.md ENT-SEC-002. {@code code} is the immutable natural key — upper-cased in the
 * lifecycle hooks, excluded from the update request.
 */
@Entity
@Audited(entityType = "SEC_ROLE")
@Table(name = "SEC_ROLE",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_SEC_ROLE_CODE", columnNames = {"TENANT_ID", "CODE"})
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class Role extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sec_role_seq")
    @SequenceGenerator(name = "sec_role_seq", sequenceName = "SEQ_SEC_ROLE", allocationSize = 1)
    @Column(name = "ROLE_PK")
    private Long rolePk;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "CODE", length = 50, nullable = false)
    private String code;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Column(name = "NAME_AR", length = 150, nullable = false)
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Column(name = "NAME_EN", length = 150, nullable = false)
    private String nameEn;

    @Size(max = 500, message = "{validation.size}")
    @Column(name = "DESCRIPTION_AR", length = 500)
    private String descriptionAr;

    @Size(max = 500, message = "{validation.size}")
    @Column(name = "DESCRIPTION_EN", length = 500)
    private String descriptionEn;

    /** DBF-SEC-020 — native postgres BOOLEAN NOT NULL DEFAULT TRUE; no converter (see User). */
    @Column(name = "IS_ACTIVE_FL", nullable = false)
    @Builder.Default
    private Boolean isActiveFl = Boolean.TRUE;

    /**
     * erp-core step 06 — a super role holds every catalog permission without per-permission grants
     * (MenuService). Native BOOLEAN, DB DEFAULT FALSE; seeded TRUE for SYS_ADMIN, never set by the API.
     */
    @Column(name = "IS_SUPER", nullable = false)
    @Builder.Default
    private Boolean isSuper = Boolean.FALSE;

    @PrePersist
    protected void onCreate() {
        if (isActiveFl == null) {
            isActiveFl = Boolean.TRUE;
        }
        normalize();
    }

    @PreUpdate
    protected void onUpdate() {
        normalize();
    }

    private void normalize() {
        if (code != null) {
            code = code.trim().toUpperCase();
        }
    }

    public void activate() {
        this.isActiveFl = Boolean.TRUE;
    }

    public void deactivate() {
        this.isActiveFl = Boolean.FALSE;
    }
}
