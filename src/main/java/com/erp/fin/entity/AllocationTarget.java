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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ENT-FIN-014 — AllocationTarget (FIN_ALLOCATION_TARGET). Source: db-script-fin.md §1
 * DBF-FIN-139..146 / §3 BLOCK 3, DATA-DOM-LOOKUP.md ENT-FIN-014.
 *
 * <p><b>A.1.1 exemption (declared):</b> this entity deliberately does NOT extend
 * {@code AuditableEntity}. {@code FIN_ALLOCATION_TARGET} carries NO audit column at all — the
 * db-script's DBF range DBF-FIN-139..146 ends at {@code is_remainder_fl}; there is no
 * {@code created_by}/{@code created_at}/{@code updated_by}/{@code updated_at} and not even the
 * lone {@code created_at} the other two line tables have. Targets are written and replaced as
 * part of their parent rule's definition, which is itself audited. Declared in the same style
 * as SEC's exempt entities.
 *
 * <p>{@code targetAccount} (DBF-FIN-142, FK_ALLOCATION_TARGET_ACCOUNT → FIN_ACCOUNT) is a LAZY
 * {@code @ManyToOne} to {@code Account} (ENT-FIN-001), promoted from the plain {@code Long} this
 * sub deferred, by DATA-DOM-MASTER once the {@code Account} class existed.
 *
 * <p>RULE-FIN-003 (exactly one remainder target when any sibling uses percentage distribution)
 * is decided by {@link com.erp.fin.domain.AllocationRuleDomain} — it spans the rule's whole
 * target set, so it sits with the aggregate root.
 *
 * <p>No {@code activate()}/{@code deactivate()} helpers (A.1.18): the table has no active flag.
 */
@Entity
@Table(name = "FIN_ALLOCATION_TARGET",
    indexes = {
        @Index(name = "IDX_FIN_ALLOCATION_TARGET_RULE", columnList = "ALLOCATION_RULE_ID"),
        @Index(name = "IDX_FIN_ALLOCATION_TARGET_ACCOUNT", columnList = "TARGET_ACCOUNT_ID")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class AllocationTarget {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fin_allocation_target_seq")
    @SequenceGenerator(name = "fin_allocation_target_seq",
        sequenceName = "SEQ_FIN_ALLOCATION_TARGET", allocationSize = 1)
    @Column(name = "ALLOCATION_TARGET_PK")
    private Long allocationTargetPk;

    /** DBF-FIN-140 — FK_ALLOCATION_TARGET_RULE. */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ALLOCATION_RULE_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_ALLOCATION_TARGET_RULE"))
    private AllocationRule allocationRule;

    /** DBF-FIN-141 — bare NUMERIC line number; Integer per CORE.md's type-mapping table. */
    @NotNull(message = "{validation.required}")
    @Column(name = "LINE_NO", nullable = false)
    private Integer lineNo;

    /** DBF-FIN-142 — FK_ALLOCATION_TARGET_ACCOUNT → FIN_ACCOUNT; see the class javadoc. */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "TARGET_ACCOUNT_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_ALLOCATION_TARGET_ACCOUNT"))
    private Account targetAccount;

    /** DBF-FIN-143 — FK_ALLOCATION_TARGET_DIMVAL; nullable per the db-script. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DIMENSION_VALUE_ID",
        foreignKey = @ForeignKey(name = "FK_ALLOCATION_TARGET_DIMVAL"))
    private DimensionValue dimensionValue;

    /** DBF-FIN-144 — lookup DISTRIBUTION_TYPE (XM-FIN-001); RULE-FIN-003's other half. */
    @NotBlank(message = "{validation.required}")
    @Size(max = 15, message = "{validation.size}")
    @Column(name = "DISTRIBUTION_TYPE_CODE", length = 15, nullable = false)
    private String distributionTypeCode;

    /** DBF-FIN-145 — NUMERIC(18,4); nullable (a remainder target carries no explicit value). */
    @Column(name = "DISTRIBUTION_VALUE", precision = 18, scale = 4)
    private BigDecimal distributionValue;

    /** DBF-FIN-146 — native postgres BOOLEAN NOT NULL DEFAULT FALSE; no converter (A.1.6). */
    @Column(name = "IS_REMAINDER_FL", nullable = false)
    @Builder.Default
    private Boolean isRemainderFl = Boolean.FALSE;

    @PrePersist
    protected void onCreate() {
        if (isRemainderFl == null) {
            isRemainderFl = Boolean.FALSE;
        }
    }
}
