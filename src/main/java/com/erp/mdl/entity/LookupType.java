package com.erp.mdl.entity;

import com.erp.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
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
 * ENT-MDL-001 — LookupType (MDL_LOOKUP_TYPE). Source: db-script-mdl.md §1 DBF-MDL-001..010 /
 * §3 BLOCK 2, DATA-DOM.md ENT-MDL-001. {@code key} (DBF-MDL-002) and {@code ownerModuleCode}
 * (DBF-MDL-003) are both create-only — read-only after creation per RULE-MDL-003 (enforced by
 * DTO shape at the SVC-API layer, not here). Neither field is upper-cased in the lifecycle
 * hooks: DATA-DOM.md gives no explicit case-normalization rule for {@code key}, and the
 * build-create-entity template's natural-key uppercasing is illustrative, not mandatory —
 * inventing one here would not trace to any SRS/db-script source.
 */
@Entity
@Table(name = "MDL_LOOKUP_TYPE",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_MDL_LOOKUP_TYPE_KEY", columnNames = {"KEY"})
    },
    indexes = {
        @Index(name = "IDX_MDL_LOOKUP_TYPE_OWNER", columnList = "OWNER_MODULE_CODE"),
        @Index(name = "IDX_MDL_LOOKUP_TYPE_ACTIVE", columnList = "IS_ACTIVE_FL")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class LookupType extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mdl_lookup_type_seq")
    @SequenceGenerator(name = "mdl_lookup_type_seq", sequenceName = "SEQ_MDL_LOOKUP_TYPE", allocationSize = 1)
    @Column(name = "LOOKUP_TYPE_PK")
    private Long lookupTypePk;

    /** DBF-MDL-002 — UQ_MDL_LOOKUP_TYPE_KEY; immutable after create (RULE-MDL-003). */
    @NotBlank(message = "{validation.required}")
    @Size(max = 80, message = "{validation.size}")
    @Column(name = "KEY", length = 80, nullable = false)
    private String key;

    /** DBF-MDL-003 — XM-MDL-001 SOFT-READ validates against SEC_MODULE_REG.code (RULE-MDL-001); immutable after create. */
    @NotBlank(message = "{validation.required}")
    @Size(max = 10, message = "{validation.size}")
    @Column(name = "OWNER_MODULE_CODE", length = 10, nullable = false)
    private String ownerModuleCode;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Column(name = "NAME_AR", length = 150, nullable = false)
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Column(name = "NAME_EN", length = 150, nullable = false)
    private String nameEn;

    /** DBF-MDL-006 — native postgres BOOLEAN NOT NULL DEFAULT TRUE; no converter (A.1.6). */
    @Column(name = "IS_ACTIVE_FL", nullable = false)
    @Builder.Default
    private Boolean isActiveFl = Boolean.TRUE;

    @PrePersist
    protected void onCreate() {
        if (isActiveFl == null) {
            isActiveFl = Boolean.TRUE;
        }
    }

    public void activate() {
        this.isActiveFl = Boolean.TRUE;
    }

    public void deactivate() {
        this.isActiveFl = Boolean.FALSE;
    }
}
