package com.erp.audit.crossmodule;

import java.time.Instant;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import lombok.ToString;

/**
 * One audit row to record through {@link AuditApi#record(AuditEntry)} (erp-core step 10). Only
 * {@link #getAction() action} is required; every field left {@code null} gets a sensible default when
 * the entry is recorded:
 * <ul>
 *   <li>{@code tenantId} — {@code TenantContext.require()} (the current tenant; none is an error);</li>
 *   <li>{@code occurredAt} — now;</li>
 *   <li>{@code actor} — the authenticated principal's name, else {@code system};</li>
 *   <li>{@code actorRealm} — {@code CUSTOMER} for a caller holding {@code ROLE_CUSTOMER}, {@code STAFF}
 *       for any other authenticated caller, {@code SYSTEM} without a caller (the rule of
 *       {@code DomainEvent});</li>
 *   <li>{@code ip} / {@code userAgent} — from the current HTTP request (captured by the audit module's
 *       request filter), {@code null} outside a request.</li>
 * </ul>
 * {@code actorUserId}, {@code entityType}, {@code entityId}, the summaries, {@code changes} and
 * {@code reference} have no default. Changes naming a sensitive field are dropped on record.
 *
 * <pre>{@code
 * auditApi.record(AuditEntry.builder()
 *     .action(AuditApi.ACTION_STATUS_CHANGE)
 *     .entityType("SALES_INVOICE").entityId(String.valueOf(invoice.getId()))
 *     .summaryAr("اعتماد الفاتورة").summaryEn("Invoice approved")
 *     .change(new AuditChange("status", "DRAFT", "APPROVED"))
 *     .build());
 * }</pre>
 */
@Getter
@Builder(toBuilder = true)
@ToString
public final class AuditEntry {

    /** {@code CREATE|UPDATE|DELETE|STATUS_CHANGE|LOGIN|...}; must match {@code ^[A-Z_]{3,64}$}. */
    private final String action;

    private final Long tenantId;
    private final Instant occurredAt;
    private final String actor;
    /** {@code STAFF}, {@code CUSTOMER} or {@code SYSTEM}. */
    private final String actorRealm;
    private final Long actorUserId;
    private final String entityType;
    private final String entityId;
    private final String summaryAr;
    private final String summaryEn;
    @Singular
    private final List<AuditChange> changes;
    private final String ip;
    private final String userAgent;
    /** Optional correlation id chosen by the caller. */
    private final String reference;
}
