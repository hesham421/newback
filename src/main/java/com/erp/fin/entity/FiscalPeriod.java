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
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ENT-FIN-008 — FiscalPeriod (FIN_FISCAL_PERIOD). Source: db-script-fin.md §1 DBF-FIN-075..088 /
 * §3 BLOCK 3, DATA-DOM-MASTER.md ENT-FIN-008.
 *
 * <p>RULE-FIN-014 (a Hard Closed period is never reopened) is decided by
 * {@link com.erp.fin.domain.FiscalPeriodDomain}; the transition helpers below are plain state
 * mutation, exactly as CORE.md prescribes ({@code FiscalPeriodDomain.assertCanReopen()} then
 * {@code FiscalPeriod.hardClose()}). RULE-FIN-015 (the close-approval permission is distinct
 * from the entry-creation permission) is not decided here or in the Domain class at all — it is
 * the {@code @PreAuthorize(PERM_FIN_PERIODS_CLOSE_APPROVE)} gate on the two close endpoints, per
 * the SRS. RULE-FIN-008 (period must be Open at post time) is
 * NOT here: CORE.md assigns it to {@code JournalEntryDomain}, owned by the
 * DATA-DOM-TRANSACTIONAL sub — duplicating it would breach A.0.7.
 *
 * <p><b>{@code closedBy}/{@code closedAt} are ordinary business columns, not audit columns.</b>
 * {@code AuditableEntity} supplies only {@code createdBy/createdAt/updatedBy/updatedAt} and its
 * listener fills only those from the current principal; there is no hook for a second principal.
 * DBF-FIN-083/084 record the period-close approver (CORE.md "Audit fields"), set explicitly by
 * the service at hard-close time.
 *
 * <p><b>No {@code activate()}/{@code deactivate()} helpers (A.1.18):</b> FIN_FISCAL_PERIOD has no
 * active flag — DBF-FIN-075..088 carries none. Its lifecycle is {@code statusCode} (PERIOD_STATE,
 * SRS A7), not a binary active flag.
 *
 * <p>Caching is prohibited across FIN.
 */
@Entity
@Table(name = "FIN_FISCAL_PERIOD",
    indexes = {
        @Index(name = "IDX_FIN_FISCAL_PERIOD_YEAR", columnList = "FISCAL_YEAR_ID"),
        @Index(name = "IDX_FIN_FISCAL_PERIOD_STATUS", columnList = "STATUS_CODE")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class FiscalPeriod extends AuditableEntity {

    /** PERIOD_STATE (SRS A6/A7) — accepts postings. */
    public static final String STATUS_OPEN = "OPEN";

    /** PERIOD_STATE (SRS A6/A7) — reversible back to OPEN (REQ-FIN-032). */
    public static final String STATUS_SOFT_CLOSE = "SOFT_CLOSE";

    /** PERIOD_STATE (SRS A6/A7) — terminal; never reopened (RULE-FIN-014). */
    public static final String STATUS_HARD_CLOSE = "HARD_CLOSE";

    /** PERIOD_STATE (SRS A6/A7) — terminal; set by the year-end close (REQ-FIN-036). */
    public static final String STATUS_YEAR_END_CLOSE = "YEAR_END_CLOSE";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fin_fiscal_period_seq")
    @SequenceGenerator(name = "fin_fiscal_period_seq",
        sequenceName = "SEQ_FIN_FISCAL_PERIOD", allocationSize = 1)
    @Column(name = "FISCAL_PERIOD_PK")
    private Long fiscalPeriodPk;

    /** DBF-FIN-076 — FK_FISCAL_PERIOD_YEAR; the owning year (REQ-FIN-031). */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FISCAL_YEAR_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_FISCAL_PERIOD_YEAR"))
    private FiscalYear fiscalYear;

    /** DBF-FIN-077 — bare NUMERIC period number; Integer per CORE.md's type-mapping table. */
    @NotNull(message = "{validation.required}")
    @Column(name = "PERIOD_NO", nullable = false)
    private Integer periodNo;

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    @Column(name = "NAME_AR", length = 100, nullable = false)
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    @Column(name = "NAME_EN", length = 100, nullable = false)
    private String nameEn;

    /** DBF-FIN-080 — DATE → LocalDate per CORE.md's type-mapping table. */
    @NotNull(message = "{validation.required}")
    @Column(name = "START_DATE", nullable = false)
    private LocalDate startDate;

    /** DBF-FIN-081 — DATE → LocalDate per CORE.md's type-mapping table. */
    @NotNull(message = "{validation.required}")
    @Column(name = "END_DATE", nullable = false)
    private LocalDate endDate;

    /** DBF-FIN-082 — lookup PERIOD_STATE (XM-FIN-001); DB default {@code 'OPEN'}. */
    @NotBlank(message = "{validation.required}")
    @Size(max = 15, message = "{validation.size}")
    @Column(name = "STATUS_CODE", length = 15, nullable = false)
    @Builder.Default
    private String statusCode = STATUS_OPEN;

    /**
     * DBF-FIN-083 — the close-approver principal (RULE-FIN-015). A business column, not an audit
     * column: set explicitly by the service at hard-close, never by {@code AuditEntityListener}.
     */
    @Size(max = 100, message = "{validation.size}")
    @Column(name = "CLOSED_BY", length = 100)
    private String closedBy;

    /** DBF-FIN-084 — TIMESTAMPTZ → Instant; a business column, see {@code closedBy}. */
    @Column(name = "CLOSED_AT")
    private Instant closedAt;

    @PrePersist
    protected void onCreate() {
        if (statusCode == null) {
            statusCode = STATUS_OPEN;
        }
    }

    /**
     * REQ-FIN-032 (reopen a soft-closed period). Plain state mutation —
     * {@code FiscalPeriodDomain.assertCanReopen(...)} decides first (RULE-FIN-014).
     */
    public void open() {
        this.statusCode = STATUS_OPEN;
    }

    /** REQ-FIN-033 (soft-close). Plain state mutation — the Domain object decides first. */
    public void softClose() {
        this.statusCode = STATUS_SOFT_CLOSE;
    }

    /**
     * REQ-FIN-034 (hard-close, approval-gated). Plain state mutation —
     * {@code FiscalPeriodDomain.assertCanReopen()} decides first (RULE-FIN-014) and the
     * {@code @PreAuthorize(PERM_FIN_PERIODS_CLOSE_APPROVE)} gate on the calling service method
     * is RULE-FIN-015's whole enforcement; the approver principal and timestamp are supplied by
     * the service.
     */
    public void hardClose(String approverPrincipal, Instant closedAtInstant) {
        this.statusCode = STATUS_HARD_CLOSE;
        this.closedBy = approverPrincipal;
        this.closedAt = closedAtInstant;
    }

    /** REQ-FIN-036 (year-end close over every period of the year). Plain state mutation. */
    public void yearEndClose() {
        this.statusCode = STATUS_YEAR_END_CLOSE;
    }
}
