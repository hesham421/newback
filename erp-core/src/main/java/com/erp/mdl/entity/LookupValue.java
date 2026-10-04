package com.erp.mdl.entity;

import com.erp.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ENT-MDL-002 — LookupValue (MDL_LOOKUP_VALUE). Source: db-script-mdl.md §1 DBF-MDL-011..021 /
 * §3 BLOCK 3, DATA-DOM.md ENT-MDL-002. DBF-MDL-012 ({@code lookupTypeId}) is the FK
 * {@code FK_LOOKUP_VALUE_TYPE}, modelled as the {@code @ManyToOne} {@code lookupType} (same
 * project convention as {@code RoleModuleGrant.module} / {@code ScreenRegistry.module}).
 * {@code lookupType} and {@code code} are both create-only per DATA-DOM.md's DTO membership
 * hint; neither is upper-cased in the lifecycle hooks (no normalization rule is given).
 */
@Entity
@Table(name = "MDL_LOOKUP_VALUE",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_MDL_LOOKUP_VALUE_TYPE_CODE", columnNames = {"LOOKUP_TYPE_ID", "CODE"})
    },
    indexes = {
        @Index(name = "IDX_MDL_LOOKUP_VALUE_TYPE", columnList = "LOOKUP_TYPE_ID"),
        @Index(name = "IDX_MDL_LOOKUP_VALUE_SORT", columnList = "LOOKUP_TYPE_ID, SORT_ORDER")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class LookupValue extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "mdl_lookup_value_seq")
    @SequenceGenerator(name = "mdl_lookup_value_seq", sequenceName = "SEQ_MDL_LOOKUP_VALUE", allocationSize = 1)
    @Column(name = "LOOKUP_VALUE_PK")
    private Long lookupValuePk;

    /** DBF-MDL-012 — FK_LOOKUP_VALUE_TYPE; half of the composite UQ; immutable after create. */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "LOOKUP_TYPE_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_LOOKUP_VALUE_TYPE"))
    private LookupType lookupType;

    /** DBF-MDL-013 — half of UQ_MDL_LOOKUP_VALUE_TYPE_CODE (RULE-MDL-002); immutable after create. */
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

    /**
     * DBF-MDL-016 — bare NUMERIC in the db-script; mapped to {@code Integer} per CORE.md's
     * stated deviation (a whole-number ordering field, not a monetary/precision decimal).
     */
    @NotNull(message = "{validation.required}")
    @Column(name = "SORT_ORDER", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /** DBF-MDL-017 — native postgres BOOLEAN NOT NULL DEFAULT TRUE; no converter (A.1.6). */
    @Column(name = "IS_ACTIVE_FL", nullable = false)
    @Builder.Default
    private Boolean isActiveFl = Boolean.TRUE;

    @PrePersist
    protected void onCreate() {
        if (isActiveFl == null) {
            isActiveFl = Boolean.TRUE;
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }

    public void activate() {
        this.isActiveFl = Boolean.TRUE;
    }

    public void deactivate() {
        this.isActiveFl = Boolean.FALSE;
    }
}
