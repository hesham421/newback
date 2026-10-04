package com.erp.fin.entity;

import com.erp.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * ENT-FIN-002 — Dimension (FIN_DIMENSION). Source: db-script-fin.md §1 DBF-FIN-014..022 /
 * §3 BLOCK 2, DATA-DOM-LOOKUP.md ENT-FIN-002.
 *
 * <p>No Domain companion: DATA-DOM-LOOKUP.md records "DOMAIN RULES: none scoped alone" for this
 * entity, and A.0.7 forbids manufacturing one Domain object per entity. Code uniqueness is a
 * plain platform-standard pre-check ({@code UQ_FIN_DIMENSION_CODE}) owned by the service, not
 * an SRS RULE.
 *
 * <p>Caching is prohibited across FIN (approved register empty; gov-enforce-caching-rules bars
 * financial/accounting data) — dimensions feed postings, so no {@code @Cacheable} here or in
 * any future FIN service reading this entity.
 */
@Entity
@Table(name = "FIN_DIMENSION",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_FIN_DIMENSION_CODE", columnNames = {"CODE"})
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class Dimension extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fin_dimension_seq")
    @SequenceGenerator(name = "fin_dimension_seq", sequenceName = "SEQ_FIN_DIMENSION", allocationSize = 1)
    @Column(name = "DIMENSION_PK")
    private Long dimensionPk;

    /**
     * DBF-FIN-015 — UQ_FIN_DIMENSION_CODE; the natural key. Not case-normalized: neither
     * srs-fin.md nor db-script-fin.md states a normalization rule for it, and inventing one
     * would change stored data (same decision already taken for {@code LookupValue.code}).
     */
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

    /** DBF-FIN-018 — native postgres BOOLEAN NOT NULL DEFAULT TRUE; no converter (A.1.6). */
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
