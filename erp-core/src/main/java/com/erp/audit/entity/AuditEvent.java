package com.erp.audit.entity;

import com.erp.common.domain.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Immutable;

/**
 * One row of the generic audit log, {@code CORE_AUDIT_EVENT} (erp-core step 10, V15). Tenant-scoped
 * (Hibernate's {@code @TenantId} filter restricts every read to the current tenant).
 *
 * <p><b>Read model only.</b> Rows are written by {@code AuditEventStore} with JDBC, in the writer's
 * transaction and on its connection — also from inside a Hibernate flush (the {@code @Audited}
 * listener), where persisting another entity through the same session is not safe. Hence
 * {@link Immutable} and no setters: JPA never inserts or updates this table. Append-only; no
 * activate/deactivate.
 */
@Entity
@Immutable
@Table(name = "CORE_AUDIT_EVENT",
    indexes = {
        @Index(name = "IDX_CORE_AUDIT_EVENT_ENTITY", columnList = "TENANT_ID,ENTITY_TYPE,ENTITY_ID"),
        @Index(name = "IDX_CORE_AUDIT_EVENT_OCCURRED", columnList = "TENANT_ID,OCCURRED_AT")
    }
)
@Getter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class AuditEvent extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "core_audit_event_seq")
    @SequenceGenerator(name = "core_audit_event_seq", sequenceName = "SEQ_CORE_AUDIT_EVENT", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @Column(name = "OCCURRED_AT", nullable = false)
    private Instant occurredAt;

    @Column(name = "ACTOR", length = 100, nullable = false)
    private String actor;

    @Column(name = "ACTOR_REALM", length = 16, nullable = false)
    private String actorRealm;

    @Column(name = "ACTOR_USER_ID")
    private Long actorUserId;

    @Column(name = "ACTION", length = 64, nullable = false)
    private String action;

    @Column(name = "ENTITY_TYPE", length = 128)
    private String entityType;

    @Column(name = "ENTITY_ID", length = 64)
    private String entityId;

    @Column(name = "SUMMARY_AR", length = 1000)
    private String summaryAr;

    @Column(name = "SUMMARY_EN", length = 1000)
    private String summaryEn;

    /** The {@code JSONB} array of {@code {field, old, new}}, read as its JSON text. */
    @Column(name = "CHANGES", columnDefinition = "jsonb")
    private String changes;

    @Column(name = "IP", length = 64)
    private String ip;

    @Column(name = "USER_AGENT", length = 256)
    private String userAgent;

    @Column(name = "REFERENCE", length = 100)
    private String reference;
}
