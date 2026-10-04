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
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ENT-FIN-006 — JournalLineDimension (FIN_JOURNAL_LINE_DIM). Source: db-script-fin.md §1
 * DBF-FIN-061..064 / §3 BLOCK 3, DATA-DOM-TRANSACTIONAL.md ENT-FIN-006. The analytical tagging of
 * one posting line: 0..N (dimension, dimension value) pairs per line.
 *
 * <p><b>A.1.1 exemption (declared):</b> this entity deliberately does NOT extend
 * {@code AuditableEntity}. {@code FIN_JOURNAL_LINE_DIM} carries NO audit column at all — the
 * db-script's DBF range DBF-FIN-061..064 ends at {@code dimension_value_id}, and not even the lone
 * {@code created_at} the line table has. Verified against db-script-fin.md §3 BLOCK 3 and
 * V22__fin_schema.sql. The rows are written as part of their line, which is written as part of an
 * audited header. Declared in the same style as {@code AllocationTarget}.
 *
 * <p><b>No Domain companion of its own (A.0.7).</b> RULE-FIN-009 (the cited dimension value must
 * belong to its stated dimension and be active) is a decision about a {@code DimensionValue}, and
 * ENT-FIN-003 already owns a Domain object — the guard is
 * {@code DimensionValueDomain.assertUsableOnLine(...)}, added by this sub. A second Domain object
 * here would duplicate that ownership.
 *
 * <p>No {@code activate()}/{@code deactivate()} helpers (A.1.18): the table has no active flag,
 * and no {@code @PrePersist} hook: it has no defaultable or normalizable column either.
 * Caching is prohibited across FIN.
 */
@Entity
@Table(name = "FIN_JOURNAL_LINE_DIM",
    indexes = {
        @Index(name = "IDX_FIN_JOURNAL_LINE_DIM_LINE", columnList = "JOURNAL_LINE_ID"),
        @Index(name = "IDX_FIN_JOURNAL_LINE_DIM_VALUE", columnList = "DIMENSION_VALUE_ID")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class JournalLineDimension {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fin_journal_line_dim_seq")
    @SequenceGenerator(name = "fin_journal_line_dim_seq",
        sequenceName = "SEQ_FIN_JOURNAL_LINE_DIM", allocationSize = 1)
    @Column(name = "JOURNAL_LINE_DIM_PK")
    private Long journalLineDimensionPk;

    /** DBF-FIN-062 — FK_JOURNAL_LINE_DIM_LINE; the owning line. */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "JOURNAL_LINE_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_JOURNAL_LINE_DIM_LINE"))
    private JournalLine journalLine;

    /** DBF-FIN-063 — FK_JOURNAL_LINE_DIM_DIMENSION; the stated dimension (RULE-FIN-009). */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DIMENSION_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_JOURNAL_LINE_DIM_DIMENSION"))
    private Dimension dimension;

    /** DBF-FIN-064 — FK_JOURNAL_LINE_DIM_VALUE; must belong to {@code dimension} and be active. */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "DIMENSION_VALUE_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_JOURNAL_LINE_DIM_VALUE"))
    private DimensionValue dimensionValue;
}
