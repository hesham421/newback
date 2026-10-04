package com.erp.sec.entity;

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
import jakarta.validation.constraints.Size;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * ENT-SEC-011 — AuditLogEntry (SEC_AUDIT_LOG). Source: db-script-sec.md §1 DBF-SEC-083..090 /
 * §3 BLOCK 3, DATA-DOM-TRANSACTIONAL.md ENT-SEC-011. Append-only (POL-SEC-009): no setter, no
 * mutator, no Domain companion, and no AuditableEntity — see
 * governance/project-artifacts/sec-implementation-notes.md.
 */
@Entity
@Table(name = "SEC_AUDIT_LOG",
    indexes = {
        @Index(name = "IDX_SEC_AUDIT_LOG_EVENT_TYPE", columnList = "EVENT_TYPE_CODE"),
        @Index(name = "IDX_SEC_AUDIT_LOG_ACTOR", columnList = "ACTOR_USER_ID"),
        @Index(name = "IDX_SEC_AUDIT_LOG_OCCURRED_AT", columnList = "OCCURRED_AT")
    }
)
@Getter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class AuditLogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sec_audit_log_seq")
    @SequenceGenerator(name = "sec_audit_log_seq", sequenceName = "SEQ_SEC_AUDIT_LOG",
        allocationSize = 1)
    @Column(name = "AUDIT_LOG_PK")
    private Long auditLogPk;

    /** DBF-SEC-084 — AUDIT_EVENT_TYPE code (closed set, CHK_SEC_AUDIT_LOG_EVENT_TYPE). */
    @NotBlank(message = "{validation.required}")
    @Size(max = 40, message = "{validation.size}")
    @Column(name = "EVENT_TYPE_CODE", length = 40, nullable = false)
    private String eventTypeCode;

    /** DBF-SEC-085 — FK_AUDIT_LOG_USER; nullable (unknown/unauthenticated actor, REQ-SEC-002). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ACTOR_USER_ID",
        foreignKey = @ForeignKey(name = "FK_AUDIT_LOG_USER"))
    private User actor;

    /** DBF-SEC-086 — system-set, DB DEFAULT now(); immutable once written (POL-SEC-009). */
    @Column(name = "OCCURRED_AT", nullable = false)
    private Instant occurredAt;

    /** DBF-SEC-087 — free text (e.g. the affected user/role id); nullable. */
    @Size(max = 200, message = "{validation.size}")
    @Column(name = "TARGET_REF", length = 200)
    private String targetRef;

    /** DBF-SEC-088 — TEXT; nullable. Both languages are written together (CORE.md "Languages"). */
    @Column(name = "DETAILS_AR", columnDefinition = "TEXT")
    private String detailsAr;

    /** DBF-SEC-089 — TEXT; nullable. */
    @Column(name = "DETAILS_EN", columnDefinition = "TEXT")
    private String detailsEn;

    /** DBF-SEC-090 — nullable. */
    @Size(max = 64, message = "{validation.size}")
    @Column(name = "IP_ADDRESS", length = 64)
    private String ipAddress;

    /**
     * Mirrors the db-script DEFAULT now() for DBF-SEC-086 and upper-cases the AUDIT_EVENT_TYPE
     * code (A.1.17). No {@code @PreUpdate} counterpart — this table is never updated.
     */
    @PrePersist
    protected void onCreate() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
        if (eventTypeCode != null) {
            eventTypeCode = eventTypeCode.trim().toUpperCase();
        }
    }
}
