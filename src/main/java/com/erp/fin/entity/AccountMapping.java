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
 * ENT-FIN-015 — AccountMapping (FIN_ACCOUNT_MAPPING), v2. Source: db-script-fin.md v2 §1
 * DBF-FIN-149..158 / §3 BLOCK 3, DATA-DOM-LOOKUP.md ENT-FIN-015. The natural key
 * (eventTypeCode, businessFieldCode, businessValue) is unique among ACTIVE rows only, by the
 * partial unique index {@code UQ_FIN_ACCOUNT_MAPPING_ACTIVE_KEY} (V35): JPA cannot express a
 * WHERE-clause uniqueness, so it is deliberately NOT declared here (RULE-FIN-022, ADR-FIN-028).
 */
@Entity
@Table(name = "FIN_ACCOUNT_MAPPING",
    indexes = {
        @Index(name = "IDX_FIN_ACCOUNT_MAPPING_EVT_FLD_VAL",
            columnList = "EVENT_TYPE_CODE, BUSINESS_FIELD_CODE, BUSINESS_VALUE"),
        @Index(name = "IDX_FIN_ACCOUNT_MAPPING_FIELD", columnList = "BUSINESS_FIELD_CODE"),
        @Index(name = "IDX_FIN_ACCOUNT_MAPPING_ACCOUNT", columnList = "ACCOUNT_ID")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class AccountMapping extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fin_account_mapping_seq")
    @SequenceGenerator(name = "fin_account_mapping_seq",
        sequenceName = "SEQ_FIN_ACCOUNT_MAPPING", allocationSize = 1)
    @Column(name = "ACCOUNT_MAPPING_PK")
    private Long accountMappingPk;

    /** DBF-FIN-150 — lookup ACCOUNTING_EVENT_TYPE; fixed after create (ADR-FIN-024). */
    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "EVENT_TYPE_CODE", length = 50, nullable = false, updatable = false)
    private String eventTypeCode;

    /** DBF-FIN-151 — lookup FIN_EVENT_BUSINESS_FIELD; fixed after create. */
    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "BUSINESS_FIELD_CODE", length = 50, nullable = false, updatable = false)
    private String businessFieldCode;

    /**
     * DBF-FIN-152 — stored as a code, never a display text; an active PAYMENT_METHOD code when
     * {@code businessFieldCode = PAYMENT_METHOD} (RULE-FIN-024). Compared by exact code, blanks
     * trimmed on save (ADR-FIN-039); fixed after create.
     */
    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    @Column(name = "BUSINESS_VALUE", length = 100, nullable = false, updatable = false)
    private String businessValue;

    /** DBF-FIN-153 — FK_ACCOUNT_MAPPING_ACCOUNT; the only field an update changes (ADR-FIN-024). */
    @NotNull(message = "{validation.required}")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ACCOUNT_ID", nullable = false,
        foreignKey = @ForeignKey(name = "FK_ACCOUNT_MAPPING_ACCOUNT"))
    private Account account;

    /** DBF-FIN-154 — native postgres BOOLEAN NOT NULL DEFAULT TRUE; no converter (A.1.6). */
    @Column(name = "IS_ACTIVE_FL", nullable = false)
    @Builder.Default
    private Boolean isActiveFl = Boolean.TRUE;

    @PrePersist
    protected void onCreate() {
        if (isActiveFl == null) {
            isActiveFl = Boolean.TRUE;
        }
        if (businessValue != null) {
            businessValue = businessValue.trim();
        }
    }

    public void activate() {
        this.isActiveFl = Boolean.TRUE;
    }

    public void deactivate() {
        this.isActiveFl = Boolean.FALSE;
    }
}
