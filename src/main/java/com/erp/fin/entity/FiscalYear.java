package com.erp.fin.entity;

import com.erp.common.domain.AuditableEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
 * ENT-FIN-007 — FiscalYear (FIN_FISCAL_YEAR). Source: db-script-fin.md §1 DBF-FIN-065..074 /
 * §3 BLOCK 2, DATA-DOM-MASTER.md ENT-FIN-007.
 *
 * <p>No Domain companion: DATA-DOM-MASTER.md records "DOMAIN RULES: none scoped alone" for this
 * entity (year-end close, REQ-FIN-036, is orchestrated over both FiscalYear and FiscalPeriod at
 * API-FIN-027), and A.0.7 forbids manufacturing one Domain object per entity. Its
 * {@code statusCode} lifecycle is binary (OPEN → CLOSED, set once by REQ-FIN-036, SRS A7).
 *
 * <p>{@code periods} is a genuine composition: REQ-FIN-031 generates the year's periods with the
 * year itself and QR-FIN-038 saves both in one operation. {@code periodCount} is a
 * {@code @Formula} (A.1.16) — never {@code periods.size()}, and no helper iterates the lazy
 * collection (A.1.19).
 *
 * <p>{@code statusCode} is a plain {@code String} code column (lookup FISCAL_YEAR_STATUS,
 * XM-FIN-001), never a Java enum; membership is validated at the service layer against MDL.
 *
 * <p>Caching is prohibited across FIN.
 */
@Entity
@Table(name = "FIN_FISCAL_YEAR",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_FIN_FISCAL_YEAR_CODE", columnNames = {"CODE"})
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class FiscalYear extends AuditableEntity {

    /** FISCAL_YEAR_STATUS (SRS A6/A7) — the year is open for posting. */
    public static final String STATUS_OPEN = "OPEN";

    /** FISCAL_YEAR_STATUS (SRS A6/A7) — terminal; set once by the year-end close, REQ-FIN-036. */
    public static final String STATUS_CLOSED = "CLOSED";

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fin_fiscal_year_seq")
    @SequenceGenerator(name = "fin_fiscal_year_seq",
        sequenceName = "SEQ_FIN_FISCAL_YEAR", allocationSize = 1)
    @Column(name = "FISCAL_YEAR_PK")
    private Long fiscalYearPk;

    /**
     * DBF-FIN-066 — UQ_FIN_FISCAL_YEAR_CODE; the natural key, and the middle segment of
     * {@code JournalEntry.docNo} (CORE.md numbering). Not case-normalized: no normalization rule
     * is stated in srs-fin.md or db-script-fin.md.
     */
    @NotBlank(message = "{validation.required}")
    @Size(max = 10, message = "{validation.size}")
    @Column(name = "CODE", length = 10, nullable = false)
    private String code;

    /** DBF-FIN-067 — DATE → LocalDate per CORE.md's type-mapping table. */
    @NotNull(message = "{validation.required}")
    @Column(name = "START_DATE", nullable = false)
    private LocalDate startDate;

    /** DBF-FIN-068 — DATE → LocalDate per CORE.md's type-mapping table. */
    @NotNull(message = "{validation.required}")
    @Column(name = "END_DATE", nullable = false)
    private LocalDate endDate;

    /** DBF-FIN-069 — lookup FISCAL_YEAR_STATUS (XM-FIN-001); DB default {@code 'OPEN'}. */
    @NotBlank(message = "{validation.required}")
    @Size(max = 10, message = "{validation.size}")
    @Column(name = "STATUS_CODE", length = 10, nullable = false)
    @Builder.Default
    private String statusCode = STATUS_OPEN;

    /** DBF-FIN-070 — native postgres BOOLEAN NOT NULL DEFAULT TRUE; no converter (A.1.6). */
    @Column(name = "IS_ACTIVE_FL", nullable = false)
    @Builder.Default
    private Boolean isActiveFl = Boolean.TRUE;

    /** REQ-FIN-031 / QR-FIN-038 — the year's periods, generated and saved with the year. */
    @OneToMany(mappedBy = "fiscalYear", cascade = CascadeType.ALL,
        orphanRemoval = false, fetch = FetchType.LAZY)
    @Builder.Default
    private List<FiscalPeriod> periods = new ArrayList<>();

    @Formula("(SELECT COUNT(*) FROM FIN_FISCAL_PERIOD p WHERE p.FISCAL_YEAR_ID = FISCAL_YEAR_PK)")
    private Integer periodCount;

    @PrePersist
    protected void onCreate() {
        if (statusCode == null) {
            statusCode = STATUS_OPEN;
        }
        if (isActiveFl == null) {
            isActiveFl = Boolean.TRUE;
        }
    }

    /** Plain state mutation — REQ-FIN-036's year-end close orchestration executes this. */
    public void close() {
        this.statusCode = STATUS_CLOSED;
    }

    public void activate() {
        this.isActiveFl = Boolean.TRUE;
    }

    public void deactivate() {
        this.isActiveFl = Boolean.FALSE;
    }
}
