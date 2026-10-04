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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ENT-FIN-013 — AllocationRule (FIN_ALLOCATION_RULE). Source: db-script-fin.md §1
 * DBF-FIN-130..138 / §3 BLOCK 2, DATA-DOM-LOOKUP.md ENT-FIN-013.
 *
 * <p>{@code sourceAccount} (DBF-FIN-133, FK_ALLOCATION_RULE_SRC_ACCT → FIN_ACCOUNT) is a LAZY
 * {@code @ManyToOne} to {@code Account} (ENT-FIN-001), promoted from the plain {@code Long} this
 * sub deferred, by DATA-DOM-MASTER once the {@code Account} class existed.
 *
 * <p>The entity's own fields carry no SRS rule ("DOMAIN RULES: none scoped alone"), but the
 * aggregate does: RULE-FIN-003 over its target set is decided by
 * {@link com.erp.fin.domain.AllocationRuleDomain}. That is one Domain object for the aggregate
 * root, not one per entity (A.0.7).
 *
 * <p>Caching is prohibited across FIN.
 */
@Entity
@Table(name = "FIN_ALLOCATION_RULE",
    indexes = {
        @Index(name = "IDX_FIN_ALLOCATION_RULE_SRC_ACCT", columnList = "SOURCE_ACCOUNT_ID")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class AllocationRule extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fin_allocation_rule_seq")
    @SequenceGenerator(name = "fin_allocation_rule_seq",
        sequenceName = "SEQ_FIN_ALLOCATION_RULE", allocationSize = 1)
    @Column(name = "ALLOCATION_RULE_PK")
    private Long allocationRulePk;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Column(name = "NAME_AR", length = 150, nullable = false)
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Column(name = "NAME_EN", length = 150, nullable = false)
    private String nameEn;

    /** DBF-FIN-133 — FK_ALLOCATION_RULE_SRC_ACCT → FIN_ACCOUNT; see the class javadoc. */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SOURCE_ACCOUNT_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_ALLOCATION_RULE_SRC_ACCT"))
    private Account sourceAccount;

    /** DBF-FIN-134 — native postgres BOOLEAN NOT NULL DEFAULT TRUE; no converter (A.1.6). */
    @Column(name = "IS_ACTIVE_FL", nullable = false)
    @Builder.Default
    private Boolean isActiveFl = Boolean.TRUE;

    @PrePersist
    protected void onCreate() {
        if (isActiveFl == null) {
            isActiveFl = Boolean.TRUE;
        }
    }

    public void activate() {
        this.isActiveFl = Boolean.TRUE;
    }

    public void deactivate() {
        this.isActiveFl = Boolean.FALSE;
    }
}
