package com.erp.fin.entity;

import com.erp.common.domain.AuditableEntity;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Formula;

/**
 * ENT-FIN-004 — JournalEntry (FIN_JOURNAL_ENTRY). Source: db-script-fin.md §1 DBF-FIN-034..050 /
 * §3 BLOCK 3, DATA-DOM-TRANSACTIONAL.md ENT-FIN-004. The posting core: every entry, whatever its
 * source (manual, event, recurring template, allocation), is this row plus its lines.
 *
 * <p>The seven rules CORE.md assigns to this aggregate — RULE-FIN-004, 006, 008, 011, 012, 013,
 * 016 — are decided by {@link com.erp.fin.domain.JournalEntryDomain}; the helpers below are plain
 * state mutation, exactly as CORE.md prescribes ({@code JournalEntryDomain.assertCanPost(...)}
 * then {@code JournalEntry.post(...)}). Placement note: DATA-DOM-TRANSACTIONAL.md annotates every
 * one of them "— service", but CORE.md's domain-class mandate and gov-enforce-backend-contract
 * A.5.18 supersede that wording.
 *
 * <p><b>{@code docNo}</b> ({@code JV-{fiscalYearCode}-{NNNNNN}}, CORE.md "Numbering") is assigned
 * once on create by {@code com.erp.fin.numbering.JournalDocNoGenerator}, under the fiscal-year
 * row lock ({@code FiscalYearRepository.lockForDocNoAllocation}); {@code updatable = false} keeps
 * it immutable thereafter.
 *
 * <p><b>{@code postedAt} (DBF-FIN-046) is a plain system-timestamp business column</b>, not an
 * audit field: {@code AuditableEntity} supplies {@code createdBy/createdAt/updatedBy/updatedAt}
 * and its listener fills only those. {@code postedAt} is set explicitly by the service at post
 * time, through {@link #post(Instant)}.
 *
 * <p><b>RULE-FIN-016 (lock after posting) is enforced by omission</b> — no repository in this
 * module declares an UPDATE or DELETE mapping against a POSTED row, and FIN's API registry
 * declares no PUT and no DELETE on {@code /journal-entries}, so there is no code path that could
 * edit one. There is deliberately no guard method and no error code for it. Under classic
 * reversal no carve-out is needed either: the reversal flow (API-FIN-021) leaves the original's
 * accounting state entirely alone — {@link #linkReversal(JournalEntry)} writes only the
 * {@code REVERSAL_ENTRY_ID} back-reference (DBF-FIN-043), never {@code statusCode}, amount, line
 * or period. See {@code JournalEntryDomain}'s class javadoc.
 *
 * <p>{@code journalTypeCode} and {@code statusCode} are lookup-backed (XM-FIN-001): plain VARCHAR
 * codes validated against MDL in the service layer, never numeric FKs and never Java enums.
 *
 * <p>No {@code activate()}/{@code deactivate()} helpers (A.1.18): FIN_JOURNAL_ENTRY has no active
 * flag — DBF-FIN-034..050 carries none. Its lifecycle is {@code statusCode} (JOURNAL_STATUS,
 * SRS A7: DRAFT → POSTED, terminal), not a binary active flag.
 *
 * <p>Caching is prohibited across FIN — and an accounting posting is on
 * gov-enforce-caching-rules' "never cacheable" list regardless.
 */
@Entity
@Table(name = "FIN_JOURNAL_ENTRY",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_FIN_JOURNAL_ENTRY_YEAR_DOCNO",
            columnNames = {"FISCAL_YEAR_ID", "DOC_NO"}),
        @UniqueConstraint(name = "UQ_FIN_JOURNAL_ENTRY_EVENT_REF",
            columnNames = {"EVENT_REFERENCE"})
    },
    indexes = {
        @Index(name = "IDX_FIN_JOURNAL_ENTRY_YEAR", columnList = "FISCAL_YEAR_ID"),
        @Index(name = "IDX_FIN_JOURNAL_ENTRY_PERIOD", columnList = "PERIOD_ID"),
        @Index(name = "IDX_FIN_JOURNAL_ENTRY_STATUS", columnList = "STATUS_CODE"),
        @Index(name = "IDX_FIN_JOURNAL_ENTRY_DOCDATE", columnList = "DOC_DATE")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class JournalEntry extends AuditableEntity {

    /** JOURNAL_STATUS (SRS A6/A7) — transient build state; never browsable (SRS A7 note). */
    public static final String STATUS_DRAFT = "DRAFT";

    /** JOURNAL_STATUS (SRS A6/A7) — posted and locked (RULE-FIN-016). */
    public static final String STATUS_POSTED = "POSTED";

    /**
     * JOURNAL_STATUS (SRS A6/A7) — retained lookup code with <b>no writer in FIN</b>. Under
     * classic reversal (RULE-FIN-011) a reversed original stays POSTED, so nothing sets this
     * value; it is kept because JOURNAL_STATUS is shared MDL lookup data (XM-FIN-001) that this
     * module does not own, and removing the code is a separate, explicit decision.
     */
    public static final String STATUS_VOID = "VOID";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fin_journal_entry_seq")
    @SequenceGenerator(name = "fin_journal_entry_seq",
        sequenceName = "SEQ_FIN_JOURNAL_ENTRY", allocationSize = 1)
    @Column(name = "JOURNAL_ENTRY_PK")
    private Long journalEntryPk;

    /**
     * DBF-FIN-035 — the business code, {@code JV-{fiscalYearCode}-{NNNNNN}}; unique per fiscal
     * year ({@code UQ_FIN_JOURNAL_ENTRY_YEAR_DOCNO}). Assigned by {@code JournalDocNoGenerator}
     * on create; {@code updatable = false} makes it immutable after create.
     */
    @NotBlank(message = "{validation.required}")
    @Size(max = 30, message = "{validation.size}")
    @Column(name = "DOC_NO", length = 30, nullable = false, updatable = false)
    private String docNo;

    /** DBF-FIN-036 — DATE → LocalDate per CORE.md's type-mapping table. */
    @NotNull(message = "{validation.required}")
    @Column(name = "DOC_DATE", nullable = false)
    private LocalDate docDate;

    /** DBF-FIN-037 — FK_JOURNAL_ENTRY_YEAR; also the {@code docNo} counter's scope. */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FISCAL_YEAR_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_JOURNAL_ENTRY_YEAR"))
    private FiscalYear fiscalYear;

    /** DBF-FIN-038 — FK_JOURNAL_ENTRY_PERIOD; must be Open at post time (RULE-FIN-008). */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PERIOD_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_JOURNAL_ENTRY_PERIOD"))
    private FiscalPeriod period;

    /** DBF-FIN-039 — lookup JOURNAL_TYPE (XM-FIN-001). */
    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Column(name = "JOURNAL_TYPE_CODE", length = 20, nullable = false)
    private String journalTypeCode;

    /** DBF-FIN-040 — lookup JOURNAL_STATUS (XM-FIN-001); DB default {@code 'DRAFT'}. */
    @NotBlank(message = "{validation.required}")
    @Size(max = 10, message = "{validation.size}")
    @Column(name = "STATUS_CODE", length = 10, nullable = false)
    @Builder.Default
    private String statusCode = STATUS_DRAFT;

    /**
     * DBF-FIN-041 — the event-sourced idempotency key (RULE-FIN-004, POL-FIN-012), nullable for
     * every non-event source; {@code UQ_FIN_JOURNAL_ENTRY_EVENT_REF} backs uniqueness (NULLs
     * never conflict).
     */
    @Size(max = 100, message = "{validation.size}")
    @Column(name = "EVENT_REFERENCE", length = 100)
    private String eventReference;

    /** DBF-FIN-042 — FK_JOURNAL_ENTRY_ORIGINAL; set on a reversal entry (RULE-FIN-011). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ORIGINAL_ENTRY_ID",
        foreignKey = @ForeignKey(name = "FK_JOURNAL_ENTRY_ORIGINAL"))
    private JournalEntry originalEntry;

    /** DBF-FIN-043 — FK_JOURNAL_ENTRY_REVERSAL; set on the original entry (RULE-FIN-011). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REVERSAL_ENTRY_ID",
        foreignKey = @ForeignKey(name = "FK_JOURNAL_ENTRY_REVERSAL"))
    private JournalEntry reversalEntry;

    /** DBF-FIN-044 — TEXT, nullable. */
    @Column(name = "DESCRIPTION_AR", columnDefinition = "TEXT")
    private String descriptionAr;

    /** DBF-FIN-045 — TEXT, nullable. */
    @Column(name = "DESCRIPTION_EN", columnDefinition = "TEXT")
    private String descriptionEn;

    /** DBF-FIN-046 — TIMESTAMPTZ → Instant; a business column, see the class javadoc. */
    @Column(name = "POSTED_AT")
    private Instant postedAt;

    /**
     * QR-FIN-024/025/034 — the entry's lines, saved with the header in one transaction
     * (CORE.md "Transaction scope"). A.1.19: no helper here iterates or filters this collection.
     */
    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL,
        orphanRemoval = false, fetch = FetchType.LAZY)
    @Builder.Default
    private List<JournalLine> lines = new ArrayList<>();

    /** A.1.16 — computed count, never {@code lines.size()}. */
    @Formula("(SELECT COUNT(*) FROM FIN_JOURNAL_LINE l "
        + "WHERE l.JOURNAL_ENTRY_ID = JOURNAL_ENTRY_PK)")
    private Integer lineCount;

    @PrePersist
    protected void onCreate() {
        if (statusCode == null) {
            statusCode = STATUS_DRAFT;
        }
    }

    /**
     * QR-FIN-033 — DRAFT → POSTED (SRS A7, REQ-FIN-017). Plain state mutation: the service calls
     * {@code JournalEntryDomain}'s balance, account, period and dimension guards first, and
     * RULE-FIN-016's lock takes effect the moment this returns.
     */
    public void post(Instant postedAtInstant) {
        this.statusCode = STATUS_POSTED;
        this.postedAt = postedAtInstant;
    }

    /**
     * QR-FIN-034 — the original's half of RULE-FIN-011's link (SRS A7, REQ-FIN-028). <b>Classic
     * reversal:</b> the original stays POSTED and only {@code reversalEntryId} (DBF-FIN-043) is
     * set; the mirror reversal entry carries the offsetting effect, so the net ledger movement is
     * zero and the full audit trail stays visible. Plain state mutation —
     * {@code JournalEntryDomain.assertCanReverse()} decides first (RULE-FIN-013).
     */
    public void linkReversal(JournalEntry reversal) {
        this.reversalEntry = reversal;
    }

    /**
     * QR-FIN-034 — the reversal's own half of RULE-FIN-011's link. Plain state mutation, applied
     * to the newly built reversal entry.
     */
    public void linkOriginal(JournalEntry original) {
        this.originalEntry = original;
    }
}
