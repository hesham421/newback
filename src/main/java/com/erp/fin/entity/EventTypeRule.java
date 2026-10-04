package com.erp.fin.entity;

import com.erp.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
 * ENT-FIN-009 — EventTypeRule (FIN_EVENT_TYPE_RULE). Source: db-script-fin.md §1
 * DBF-FIN-089..097 / §3 BLOCK 2, DATA-DOM-LOOKUP.md ENT-FIN-009.
 *
 * <p>{@code eventTypeCode} is lookup-backed (XM-FIN-001, key {@code ACCOUNTING_EVENT_TYPE}): a
 * plain VARCHAR code, never a numeric FK and never a Java enum; membership is validated against
 * MDL in the service layer, not here. "One active rule per event type" is decided by
 * {@link com.erp.fin.domain.EventTypeRuleDomain} and backed by
 * {@code UQ_FIN_EVENT_TYPE_RULE_CODE}.
 *
 * <p>Caching is prohibited across FIN — this rule set drives postings.
 */
@Entity
@Table(name = "FIN_EVENT_TYPE_RULE",
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_FIN_EVENT_TYPE_RULE_CODE", columnNames = {"EVENT_TYPE_CODE"})
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class EventTypeRule extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "fin_event_type_rule_seq")
    @SequenceGenerator(name = "fin_event_type_rule_seq",
        sequenceName = "SEQ_FIN_EVENT_TYPE_RULE", allocationSize = 1)
    @Column(name = "EVENT_TYPE_RULE_PK")
    private Long eventTypeRulePk;

    /** DBF-FIN-090 — UQ_FIN_EVENT_TYPE_RULE_CODE; lookup ACCOUNTING_EVENT_TYPE (XM-FIN-001). */
    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "EVENT_TYPE_CODE", length = 50, nullable = false)
    private String eventTypeCode;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Column(name = "NAME_AR", length = 150, nullable = false)
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Column(name = "NAME_EN", length = 150, nullable = false)
    private String nameEn;

    /** DBF-FIN-093 — native postgres BOOLEAN NOT NULL DEFAULT TRUE; no converter (A.1.6). */
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
