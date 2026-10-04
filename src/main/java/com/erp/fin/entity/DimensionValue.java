package com.erp.fin.entity;

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
 * ENT-FIN-003 — DimensionValue (FIN_DIMENSION_VALUE). Source: db-script-fin.md §1
 * DBF-FIN-023..033 / §3 BLOCK 3, DATA-DOM-LOOKUP.md ENT-FIN-003.
 *
 * <p>RULE-FIN-002 (duplicate code within a dimension) is decided by {@link
 * com.erp.fin.domain.DimensionValueDomain}, backed at the database by
 * {@code UQ_FIN_DIMENSION_VALUE_DIM_CODE}. {@code dimension} and {@code code} are both
 * create-only.
 *
 * <p>Caching is prohibited across FIN — dimension values are posted onto journal lines.
 */
@Entity
@Table(name = "FIN_DIMENSION_VALUE",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_FIN_DIMENSION_VALUE_DIM_CODE",
            columnNames = {"DIMENSION_ID", "CODE"})
    },
    indexes = {
        @Index(name = "IDX_FIN_DIMENSION_VALUE_DIMENSION", columnList = "DIMENSION_ID")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class DimensionValue extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fin_dimension_value_seq")
    @SequenceGenerator(name = "fin_dimension_value_seq",
        sequenceName = "SEQ_FIN_DIMENSION_VALUE", allocationSize = 1)
    @Column(name = "DIMENSION_VALUE_PK")
    private Long dimensionValuePk;

    /** DBF-FIN-024 — FK_DIMENSION_VALUE_DIMENSION; half of the composite UQ; create-only. */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DIMENSION_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_DIMENSION_VALUE_DIMENSION"))
    private Dimension dimension;

    /** DBF-FIN-025 — half of UQ_FIN_DIMENSION_VALUE_DIM_CODE (RULE-FIN-002); create-only. */
    @NotBlank(message = "{validation.required}")
    @Size(max = 30, message = "{validation.size}")
    @Column(name = "CODE", length = 30, nullable = false)
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
     * DBF-FIN-028 — bare NUMERIC in the db-script; mapped to {@code Integer} per CORE.md's
     * type-mapping table (a whole-number ordering field, not a monetary decimal).
     */
    @NotNull(message = "{validation.required}")
    @Column(name = "SORT_ORDER", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    /** DBF-FIN-029 — native postgres BOOLEAN NOT NULL DEFAULT TRUE; no converter (A.1.6). */
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
