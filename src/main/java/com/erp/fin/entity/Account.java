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
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ENT-FIN-001 — Account (FIN_ACCOUNT). Source: db-script-fin.md §1 DBF-FIN-001..013 / §3 BLOCK 2,
 * DATA-DOM-MASTER.md ENT-FIN-001.
 *
 * <p>RULE-FIN-001 (an account with sub-accounts cannot accept direct posting) and RULE-FIN-007
 * (a posting line's account must be leaf + active) are decided by
 * {@link com.erp.fin.domain.AccountDomain}. DATA-DOM-MASTER.md annotates RULE-FIN-001 with
 * "owner layer: service", but CORE.md mandates a domain class for every rule answering "is this
 * operation allowed?" and gov-enforce-backend-contract A.5.18 makes a business-rule {@code if}
 * inlined in a service an automatic rejection — CORE.md wins.
 *
 * <p>{@code parentAccount} (DBF-FIN-007, FK_ACCOUNT_PARENT) is a self-reference. No
 * {@code @OneToMany} children collection is mapped: sub-accounts are not owned components (a
 * {@code cascade = ALL} hierarchy would delete children with their parent), and A.1.19 forbids
 * deciding "has children" by iterating a lazy collection — RULE-FIN-001's fact comes from
 * {@code AccountRepository.existsByParentAccount_AccountPk} (QR-FIN-006).
 *
 * <p>{@code accountTypeCode} (ACCOUNT_TYPE) and {@code natureCode} (DEBIT_CREDIT) are plain
 * {@code String} code columns, never a numeric FK and never a Java enum; membership is validated
 * at the service layer against MDL (XM-FIN-001).
 *
 * <p>Caching is prohibited across FIN (approved register empty; gov-enforce-caching-rules bars
 * financial/accounting data) — accounts are posted onto journal lines.
 */
@Entity
@Table(name = "FIN_ACCOUNT",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_FIN_ACCOUNT_CODE", columnNames = {"CODE"})
    },
    indexes = {
        @Index(name = "IDX_FIN_ACCOUNT_PARENT", columnList = "PARENT_ACCOUNT_ID"),
        @Index(name = "IDX_FIN_ACCOUNT_TYPE", columnList = "ACCOUNT_TYPE_CODE")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class Account extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fin_account_seq")
    @SequenceGenerator(name = "fin_account_seq",
        sequenceName = "SEQ_FIN_ACCOUNT", allocationSize = 1)
    @Column(name = "ACCOUNT_PK")
    private Long accountPk;

    /**
     * DBF-FIN-002 — UQ_FIN_ACCOUNT_CODE; the natural key, client-chosen (no numbering engine)
     * and immutable on update. Not case-normalized: neither srs-fin.md nor db-script-fin.md
     * states a normalization rule for it — same decision already taken for
     * {@code Dimension.code}.
     */
    @NotBlank(message = "{validation.required}")
    @Size(max = 30, message = "{validation.size}")
    @Column(name = "CODE", length = 30, nullable = false)
    private String code;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Column(name = "NAME_AR", length = 200, nullable = false)
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Column(name = "NAME_EN", length = 200, nullable = false)
    private String nameEn;

    /** DBF-FIN-005 — lookup ACCOUNT_TYPE (XM-FIN-001); stored as the code, never a FK. */
    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Column(name = "ACCOUNT_TYPE_CODE", length = 20, nullable = false)
    private String accountTypeCode;

    /** DBF-FIN-006 — lookup DEBIT_CREDIT (XM-FIN-001); stored as the code, never a FK. */
    @NotBlank(message = "{validation.required}")
    @Size(max = 10, message = "{validation.size}")
    @Column(name = "NATURE_CODE", length = 10, nullable = false)
    private String natureCode;

    /** DBF-FIN-007 — FK_ACCOUNT_PARENT, self-referencing; nullable (a root account has none). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "PARENT_ACCOUNT_ID",
        foreignKey = @ForeignKey(name = "FK_ACCOUNT_PARENT"))
    private Account parentAccount;

    /**
     * DBF-FIN-008 — native postgres BOOLEAN NOT NULL DEFAULT TRUE; no converter (A.1.6).
     * RULE-FIN-001 governs when this may be {@code true}; the decision is
     * {@code AccountDomain}'s, this field is the plain state it mutates.
     */
    @Column(name = "IS_LEAF_FL", nullable = false)
    @Builder.Default
    private Boolean isLeafFl = Boolean.TRUE;

    /** DBF-FIN-009 — native postgres BOOLEAN NOT NULL DEFAULT TRUE; no converter (A.1.6). */
    @Column(name = "IS_ACTIVE_FL", nullable = false)
    @Builder.Default
    private Boolean isActiveFl = Boolean.TRUE;

    /**
     * DBF-FIN-147 — native postgres BOOLEAN NOT NULL DEFAULT FALSE; no converter (A.1.6). Marks
     * the single Retained Earnings account API-FIN-027 closes the year's result into
     * (REQ-FIN-036 / POL-FIN-010); the system derives the account from this column and never
     * takes it from a caller.
     *
     * <p><b>At most one row may carry {@code TRUE}</b>, guaranteed by
     * {@code UQ_FIN_ACCOUNT_RETAINED_EARNINGS} — a <i>partial</i> unique index created by
     * {@code V23__fin_account_retained_earnings_flag.sql}. It is deliberately NOT declared in
     * {@code @Table(uniqueConstraints = ...)} (A.1.12/A.1.13): JPA can only express a total
     * {@code UNIQUE (IS_RETAINED_EARNINGS_FL)}, which would also cap the number of ordinary
     * accounts at one. The physical object is therefore the migration's partial index, and the
     * entity documents it rather than mis-declaring it.
     *
     * <p><b>Read-only through the APIs.</b> Neither {@code AccountCreateRequest} nor
     * {@code AccountUpdateRequest} carries it: SVC-API-CRUD.md fixes API-FIN-002's and
     * API-FIN-003's request contracts field by field and neither lists it, and letting a caller
     * set it would hand back exactly the unaudited ledger decision this column was added to
     * remove. It is set as data (seed/migration); a governed endpoint for it would need a new
     * API id in the API Registry, which no decision authorises.
     */
    @Column(name = "IS_RETAINED_EARNINGS_FL", nullable = false)
    @Builder.Default
    private Boolean isRetainedEarningsFl = Boolean.FALSE;

    @PrePersist
    protected void onCreate() {
        if (isLeafFl == null) {
            isLeafFl = Boolean.TRUE;
        }
        if (isActiveFl == null) {
            isActiveFl = Boolean.TRUE;
        }
        if (isRetainedEarningsFl == null) {
            isRetainedEarningsFl = Boolean.FALSE;
        }
    }

    /** Plain state mutation — {@code AccountDomain} decides, this executes (RULE-FIN-001). */
    public void markAsLeaf() {
        this.isLeafFl = Boolean.TRUE;
    }

    /** Plain state mutation — {@code AccountDomain} decides, this executes (RULE-FIN-001). */
    public void markAsParent() {
        this.isLeafFl = Boolean.FALSE;
    }

    public void activate() {
        this.isActiveFl = Boolean.TRUE;
    }

    public void deactivate() {
        this.isActiveFl = Boolean.FALSE;
    }
}
