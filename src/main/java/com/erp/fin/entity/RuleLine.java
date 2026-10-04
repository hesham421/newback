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
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ENT-FIN-010 — RuleLine (FIN_RULE_LINE). Source: db-script-fin.md §1 DBF-FIN-098..108 /
 * §3 BLOCK 3, DATA-DOM-LOOKUP.md ENT-FIN-010; v2 adds DBF-FIN-148 (V35).
 *
 * <p><b>A.1.1 exemption (declared):</b> this entity deliberately does NOT extend
 * {@code AuditableEntity}. {@code FIN_RULE_LINE} carries a single audit column,
 * {@code created_at} (DBF-FIN-108) — there is no {@code created_by}, {@code updated_by} or
 * {@code updated_at} column on the table, so inheriting the four-column base would map columns
 * the schema does not have. Declared here in the same style as SEC's exempt entities
 * ({@code RoleActionGrant}, {@code UserRoleAssignment}). A rule line is created or deleted as
 * part of its parent rule's definition; it is never independently updated.
 *
 * <p>{@code accountDerivationTypeCode}, {@code amountSourceTypeCode}, {@code directionCode} and
 * {@code distributionTypeCode} are all lookup-backed (XM-FIN-001): plain VARCHAR codes,
 * validated against MDL in the service layer, never numeric FKs and never Java enums.
 *
 * <p>RULE-FIN-003 (exactly one remainder line when any sibling uses percentage distribution) is
 * decided by {@link com.erp.fin.domain.EventTypeRuleDomain} — it spans the rule's whole line
 * set, so it sits with the aggregate root, not on the individual line.
 *
 * <p>No {@code activate()}/{@code deactivate()} helpers (A.1.18): the table has no active flag.
 */
@Entity
@Table(name = "FIN_RULE_LINE",
    indexes = {
        @Index(name = "IDX_FIN_RULE_LINE_RULE", columnList = "EVENT_TYPE_RULE_ID")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class RuleLine {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fin_rule_line_seq")
    @SequenceGenerator(name = "fin_rule_line_seq",
        sequenceName = "SEQ_FIN_RULE_LINE", allocationSize = 1)
    @Column(name = "RULE_LINE_PK")
    private Long ruleLinePk;

    /** DBF-FIN-099 — FK_RULE_LINE_RULE. */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "EVENT_TYPE_RULE_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_RULE_LINE_RULE"))
    private EventTypeRule eventTypeRule;

    /** DBF-FIN-100 — bare NUMERIC line number; Integer per CORE.md's type-mapping table. */
    @NotNull(message = "{validation.required}")
    @Column(name = "LINE_NO", nullable = false)
    private Integer lineNo;

    /** DBF-FIN-101 — lookup ACCOUNT_DERIVATION_TYPE (XM-FIN-001). */
    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Column(name = "ACCOUNT_DERIVATION_TYPE_CODE", length = 20, nullable = false)
    private String accountDerivationTypeCode;

    /**
     * DBF-FIN-102 — TEXT, nullable since v2 (V35): the constant account code when CONSTANT, NULL
     * when MAPPING (RULE-FIN-019, CHK_FIN_RULE_LINE_DERIVATION_SPEC).
     */
    @Column(name = "ACCOUNT_DERIVATION_VALUE", columnDefinition = "TEXT")
    private String accountDerivationValue;

    /**
     * DBF-FIN-148 (v2) — lookup FIN_EVENT_BUSINESS_FIELD: the business field whose
     * event value selects the account through FIN_ACCOUNT_MAPPING when MAPPING; NULL when
     * CONSTANT (RULE-FIN-019).
     */
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "ACCOUNT_BUSINESS_FIELD_CODE", length = 50)
    private String accountBusinessFieldCode;

    /** DBF-FIN-103 — lookup AMOUNT_SOURCE_TYPE (XM-FIN-001). */
    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Column(name = "AMOUNT_SOURCE_TYPE_CODE", length = 20, nullable = false)
    private String amountSourceTypeCode;

    /** DBF-FIN-104 — TEXT, nullable. */
    @Column(name = "AMOUNT_SOURCE_VALUE", columnDefinition = "TEXT")
    private String amountSourceValue;

    /** DBF-FIN-105 — lookup DEBIT_CREDIT (XM-FIN-001). */
    @NotBlank(message = "{validation.required}")
    @Size(max = 10, message = "{validation.size}")
    @Column(name = "DIRECTION_CODE", length = 10, nullable = false)
    private String directionCode;

    /** DBF-FIN-106 — lookup DISTRIBUTION_TYPE (XM-FIN-001); RULE-FIN-003's other half. */
    @NotBlank(message = "{validation.required}")
    @Size(max = 15, message = "{validation.size}")
    @Column(name = "DISTRIBUTION_TYPE_CODE", length = 15, nullable = false)
    private String distributionTypeCode;

    /** DBF-FIN-107 — native postgres BOOLEAN NOT NULL DEFAULT FALSE; no converter (A.1.6). */
    @Column(name = "IS_REMAINDER_FL", nullable = false)
    @Builder.Default
    private Boolean isRemainderFl = Boolean.FALSE;

    /** DBF-FIN-108 — the table's only audit column (see the A.1.1 exemption above). */
    @Column(name = "CREATED_AT", nullable = false, updatable = false)
    private Instant createdAt;

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
