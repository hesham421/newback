package com.erp.fin.entity;

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
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ENT-FIN-016 — RuleLineDimension (FIN_RULE_LINE_DIM), v2. Source: db-script-fin.md v2 §1
 * DBF-FIN-159..165 / §3 BLOCK 3, DATA-DOM-LOOKUP.md ENT-FIN-016.
 *
 * <p><b>A.1.1 exemption (declared):</b> this entity deliberately does NOT extend
 * {@code AuditableEntity}. {@code FIN_RULE_LINE_DIM} carries a single audit column,
 * {@code created_at} (DBF-FIN-165) — no {@code created_by}, {@code updated_by} or
 * {@code updated_at} — so inheriting the four-column base would map columns the schema does not
 * have. Same declaration as its parent {@link RuleLine}: a tag is written with its rule line
 * and lives and retires with it (ADR-FIN-025), never independently updated.
 *
 * <p>No {@code activate()}/{@code deactivate()} helpers (A.1.18): the table has no active flag.
 * RULE-FIN-025/026 are decided by {@link com.erp.fin.domain.EventTypeRuleDomain} over the
 * submitted tag set; the value-source and business-field codes are plain lookup codes
 *, never enums.
 */
@Entity
@Table(name = "FIN_RULE_LINE_DIM",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_FIN_RULE_LINE_DIM_LINE_DIM",
            columnNames = {"RULE_LINE_ID", "DIMENSION_ID"})
    },
    indexes = {
        @Index(name = "IDX_FIN_RULE_LINE_DIM_DIMENSION", columnList = "DIMENSION_ID"),
        @Index(name = "IDX_FIN_RULE_LINE_DIM_VALUE", columnList = "DIMENSION_VALUE_ID")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class RuleLineDimension {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fin_rule_line_dim_seq")
    @SequenceGenerator(name = "fin_rule_line_dim_seq",
        sequenceName = "SEQ_FIN_RULE_LINE_DIM", allocationSize = 1)
    @Column(name = "RULE_LINE_DIMENSION_PK")
    private Long ruleLineDimensionPk;

    /** DBF-FIN-162 — FK_RULE_LINE_DIM_LINE; the owning line, half of the unique key. */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RULE_LINE_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_RULE_LINE_DIM_LINE"))
    private RuleLine ruleLine;

    /** DBF-FIN-163 — FK_RULE_LINE_DIM_DIMENSION; the tagged dimension (RULE-FIN-025). */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DIMENSION_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_RULE_LINE_DIM_DIMENSION"))
    private Dimension dimension;

    /** DBF-FIN-164 — FK_RULE_LINE_DIM_VALUE; set when CONSTANT, NULL when BUSINESS_FIELD (RULE-FIN-026). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DIMENSION_VALUE_ID",
        foreignKey = @ForeignKey(name = "FK_RULE_LINE_DIM_VALUE"))
    private DimensionValue dimensionValue;

    /** DBF-FIN-160 — lookup FIN_DIMENSION_VALUE_SOURCE: CONSTANT or BUSINESS_FIELD. */
    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "VALUE_SOURCE_CODE", length = 50, nullable = false)
    private String valueSourceCode;

    /** DBF-FIN-161 — lookup FIN_EVENT_BUSINESS_FIELD when BUSINESS_FIELD; NULL when CONSTANT. */
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "BUSINESS_FIELD_CODE", length = 50)
    private String businessFieldCode;

    /** DBF-FIN-165 — the table's only audit column (see the A.1.1 exemption above). */
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
