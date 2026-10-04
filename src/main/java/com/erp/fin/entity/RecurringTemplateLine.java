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
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ENT-FIN-012 — RecurringTemplateLine (FIN_RECURRING_TEMPLATE_LINE). Source: db-script-fin.md
 * §1 DBF-FIN-122..129 / §3 BLOCK 3, DATA-DOM-LOOKUP.md ENT-FIN-012.
 *
 * <p><b>A.1.1 exemption (declared):</b> this entity deliberately does NOT extend
 * {@code AuditableEntity}. {@code FIN_RECURRING_TEMPLATE_LINE} carries a single audit column,
 * {@code created_at} (DBF-FIN-129); there is no {@code created_by}, {@code updated_by} or
 * {@code updated_at} column to map. Same shape as SEC's exempt entities. A template line is
 * created or deleted with its template, never independently updated.
 *
 * <p>{@code account} (DBF-FIN-125, FK_RECURRING_TPL_LINE_ACCOUNT → FIN_ACCOUNT) is a LAZY
 * {@code @ManyToOne} to {@code Account} (ENT-FIN-001), promoted from the plain {@code Long} this
 * sub deferred, by DATA-DOM-MASTER once the {@code Account} class existed.
 *
 * <p>{@code amount} is DB-CHECK'd positive by
 * {@code CHK_FIN_RECURRING_TPL_LINE_AMOUNT_POS}; {@code directionCode} is lookup-backed
 * (XM-FIN-001, DEBIT_CREDIT).
 *
 * <p>No {@code activate()}/{@code deactivate()} helpers (A.1.18): the table has no active flag.
 */
@Entity
@Table(name = "FIN_RECURRING_TEMPLATE_LINE",
    indexes = {
        @Index(name = "IDX_FIN_RECURRING_TPL_LINE_TPL", columnList = "RECURRING_TEMPLATE_ID"),
        @Index(name = "IDX_FIN_RECURRING_TPL_LINE_ACCOUNT", columnList = "ACCOUNT_ID")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class RecurringTemplateLine {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE,
        generator = "fin_recurring_template_line_seq")
    @SequenceGenerator(name = "fin_recurring_template_line_seq",
        sequenceName = "SEQ_FIN_RECURRING_TEMPLATE_LINE", allocationSize = 1)
    @Column(name = "RECURRING_TEMPLATE_LINE_PK")
    private Long recurringTemplateLinePk;

    /** DBF-FIN-123 — FK_RECURRING_TPL_LINE_TPL. */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "RECURRING_TEMPLATE_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_RECURRING_TPL_LINE_TPL"))
    private RecurringTemplate recurringTemplate;

    /** DBF-FIN-124 — bare NUMERIC line number; Integer per CORE.md's type-mapping table. */
    @NotNull(message = "{validation.required}")
    @Column(name = "LINE_NO", nullable = false)
    private Integer lineNo;

    /** DBF-FIN-125 — FK_RECURRING_TPL_LINE_ACCOUNT → FIN_ACCOUNT; see the class javadoc. */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ACCOUNT_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_RECURRING_TPL_LINE_ACCOUNT"))
    private Account account;

    /** DBF-FIN-126 — NUMERIC(18,4); CHK_FIN_RECURRING_TPL_LINE_AMOUNT_POS enforces &gt; 0. */
    @NotNull(message = "{validation.required}")
    @Column(name = "AMOUNT", precision = 18, scale = 4, nullable = false)
    private BigDecimal amount;

    /** DBF-FIN-127 — lookup DEBIT_CREDIT (XM-FIN-001). */
    @NotBlank(message = "{validation.required}")
    @Size(max = 10, message = "{validation.size}")
    @Column(name = "DIRECTION_CODE", length = 10, nullable = false)
    private String directionCode;

    /** DBF-FIN-128 — FK_RECURRING_TPL_LINE_DIMVAL; nullable per the db-script. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DIMENSION_VALUE_ID",
        foreignKey = @ForeignKey(name = "FK_RECURRING_TPL_LINE_DIMVAL"))
    private DimensionValue dimensionValue;

    /** DBF-FIN-129 — the table's only audit column (see the A.1.1 exemption above). */
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
