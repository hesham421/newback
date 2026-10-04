package com.erp.fin.entity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ENT-FIN-005 — JournalLine (FIN_JOURNAL_LINE). Source: db-script-fin.md §1 DBF-FIN-051..060 /
 * §3 BLOCK 3, DATA-DOM-TRANSACTIONAL.md ENT-FIN-005.
 *
 * <p><b>A.1.1 exemption (declared):</b> this entity deliberately does NOT extend
 * {@code AuditableEntity}. {@code FIN_JOURNAL_LINE} carries a single audit column,
 * {@code created_at} (DBF-FIN-060) — there is no {@code created_by}, {@code updated_by} or
 * {@code updated_at} column on the table, so inheriting the four-column base would map columns the
 * schema does not have. Verified against db-script-fin.md §3 BLOCK 3 and V22__fin_schema.sql.
 * Declared in the same style as {@code RuleLine} and SEC's exempt entities. A line is written with
 * its header and never independently updated — it is locked with it (RULE-FIN-016).
 *
 * <p><b>No Domain companion of its own (A.0.7).</b> The two rules DATA-DOM-TRANSACTIONAL.md lists
 * under this entity already belong to existing owners: RULE-FIN-007 (leaf + active account) is
 * {@code AccountDomain.assertPostable()}, and RULE-FIN-010 (the remainder line absorbs the
 * rounding difference) spans a whole sibling line set, so it sits on the aggregate roots —
 * {@code EventTypeRuleDomain.assertRemainderLineSetValid(...)} and
 * {@code AllocationRuleDomain.assertRemainderTargetSetValid(...)}. RULE-FIN-006 (debits = credits)
 * likewise spans the entry's whole line set and lives on {@code JournalEntryDomain}. A second
 * Domain object here would duplicate ownership, which A.0.7 exists to prevent.
 *
 * <p>{@code amount} is always positive; the sign is carried by {@code directionCode}
 * (POL-FIN-005, {@code CHK_FIN_JOURNAL_LINE_AMOUNT_POSITIVE}). {@code directionCode} is
 * lookup-backed (DEBIT_CREDIT, XM-FIN-001): a plain VARCHAR code, validated against MDL in the
 * service layer, never a numeric FK and never a Java enum.
 *
 * <p>No {@code activate()}/{@code deactivate()} helpers (A.1.18): the table has no active flag.
 * Caching is prohibited across FIN.
 */
@Entity
@Table(name = "FIN_JOURNAL_LINE",
    indexes = {
        @Index(name = "IDX_FIN_JOURNAL_LINE_ENTRY", columnList = "JOURNAL_ENTRY_ID"),
        @Index(name = "IDX_FIN_JOURNAL_LINE_ACCOUNT", columnList = "ACCOUNT_ID")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class JournalLine {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fin_journal_line_seq")
    @SequenceGenerator(name = "fin_journal_line_seq",
        sequenceName = "SEQ_FIN_JOURNAL_LINE", allocationSize = 1)
    @Column(name = "JOURNAL_LINE_PK")
    private Long journalLinePk;

    /** DBF-FIN-052 — FK_JOURNAL_LINE_ENTRY; the owning header. */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "JOURNAL_ENTRY_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_JOURNAL_LINE_ENTRY"))
    private JournalEntry journalEntry;

    /** DBF-FIN-053 — bare NUMERIC line number; Integer per CORE.md's type-mapping table. */
    @NotNull(message = "{validation.required}")
    @Column(name = "LINE_NO", nullable = false)
    private Integer lineNo;

    /** DBF-FIN-054 — FK_JOURNAL_LINE_ACCOUNT; leaf + active at post time (RULE-FIN-007). */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ACCOUNT_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_JOURNAL_LINE_ACCOUNT"))
    private Account account;

    /**
     * DBF-FIN-055 — NUMERIC(18,4) → BigDecimal; always positive
     * ({@code CHK_FIN_JOURNAL_LINE_AMOUNT_POSITIVE}, POL-FIN-005).
     */
    @NotNull(message = "{validation.required}")
    @Column(name = "AMOUNT", precision = 18, scale = 4, nullable = false)
    private BigDecimal amount;

    /** DBF-FIN-056 — lookup DEBIT_CREDIT (XM-FIN-001); carries the sign. */
    @NotBlank(message = "{validation.required}")
    @Size(max = 10, message = "{validation.size}")
    @Column(name = "DIRECTION_CODE", length = 10, nullable = false)
    private String directionCode;

    /** DBF-FIN-057 — native postgres BOOLEAN NOT NULL DEFAULT FALSE; no converter (A.1.6). */
    @Column(name = "IS_REMAINDER_FL", nullable = false)
    @Builder.Default
    private Boolean isRemainderFl = Boolean.FALSE;

    /** DBF-FIN-058 — TEXT, nullable. */
    @Column(name = "DESCRIPTION_AR", columnDefinition = "TEXT")
    private String descriptionAr;

    /** DBF-FIN-059 — TEXT, nullable. */
    @Column(name = "DESCRIPTION_EN", columnDefinition = "TEXT")
    private String descriptionEn;

    /** DBF-FIN-060 — the table's only audit column (see the A.1.1 exemption above). */
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * QR-FIN-024/025/034 — the line's 0..N dimension rows, saved with it in the same transaction.
     * A.1.19: no helper here iterates or filters this collection.
     */
    @OneToMany(mappedBy = "journalLine", cascade = CascadeType.ALL,
        orphanRemoval = false, fetch = FetchType.LAZY)
    @Builder.Default
    private List<JournalLineDimension> dimensions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (isRemainderFl == null) {
            isRemainderFl = Boolean.FALSE;
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}
