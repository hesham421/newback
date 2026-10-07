package com.erp.audit.service;

import com.erp.audit.crossmodule.AuditChange;
import com.erp.audit.crossmodule.AuditEntry;
import com.erp.audit.domain.AuditEventDomain;
import com.erp.audit.web.RequestInfoHolder;
import com.erp.common.util.SecurityContextHelper;
import com.erp.common.util.Strings;
import com.erp.tenant.TenantContext;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.temporal.TemporalAccessor;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Records audit rows (erp-core step 10): resolves the defaults of an {@link AuditEntry}, validates it
 * ({@link AuditEventDomain}), drops sensitive changes and hands it to {@link AuditEventStore}.
 *
 * <p>No {@code @PreAuthorize} and no {@code @Transactional}: it is reached in-process only — through
 * {@code AuditApi} by every module (also on anonymous paths such as a login) and by the
 * {@code @Audited} listener inside a Hibernate flush — and it must join the caller's transaction on
 * the caller's connection, never open one of its own (named deviation from build-create-service
 * A.5.2/A.5.3; same precedent as the step-05 provisioning contributors and step-08 delivery workers).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuditRecordingService {

    /** Longest value kept for one side of a change; longer text is cut and marked. */
    static final int MAX_VALUE_LENGTH = 2000;

    private static final int MAX_ACTOR = 100;
    private static final int MAX_ENTITY_TYPE = 128;
    private static final int MAX_ENTITY_ID = 64;
    private static final int MAX_SUMMARY = 1000;
    private static final int MAX_REFERENCE = 100;

    private final AuditEventStore store;

    /** {@code AuditApi.record}: inserts on the connection of the current Spring transaction. */
    public void record(AuditEntry entry) {
        AuditEntry resolved = resolve(entry);
        store.insert(resolved);
        log.debug("Recorded audit {} {} {}", resolved.getAction(), resolved.getEntityType(), resolved.getEntityId());
    }

    /** As {@link #record(AuditEntry)}, on the given connection (the Hibernate session's, for the listener). */
    public void record(AuditEntry entry, Connection connection) throws SQLException {
        AuditEntry resolved = resolve(entry);
        store.insert(connection, resolved);
        log.debug("Recorded audit {} {} {}", resolved.getAction(), resolved.getEntityType(), resolved.getEntityId());
    }

    /** Validates {@code entry} and fills every default (see {@link AuditEntry}). */
    AuditEntry resolve(AuditEntry entry) {
        AuditEventDomain domain = AuditEventDomain.create(entry.getAction());
        Optional<RequestInfoHolder.RequestInfo> request = RequestInfoHolder.current();
        return entry.toBuilder()
            .tenantId(entry.getTenantId() != null ? entry.getTenantId() : TenantContext.require())
            .occurredAt(entry.getOccurredAt() != null ? entry.getOccurredAt() : Instant.now())
            .actor(Strings.truncate(entry.getActor() != null && !entry.getActor().isBlank()
                ? entry.getActor() : SecurityContextHelper.currentActorOrSystem(), MAX_ACTOR))
            .actorRealm(entry.getActorRealm() != null ? entry.getActorRealm() : SecurityContextHelper.currentRealm())
            .entityType(Strings.truncate(entry.getEntityType(), MAX_ENTITY_TYPE))
            .entityId(Strings.truncate(entry.getEntityId(), MAX_ENTITY_ID))
            .summaryAr(Strings.truncate(entry.getSummaryAr(), MAX_SUMMARY))
            .summaryEn(Strings.truncate(entry.getSummaryEn(), MAX_SUMMARY))
            .clearChanges()
            .changes(normalize(domain.recordableChanges(entry.getChanges())))
            .ip(entry.getIp() != null ? entry.getIp() : request.map(RequestInfoHolder.RequestInfo::ip).orElse(null))
            .userAgent(entry.getUserAgent() != null
                ? entry.getUserAgent() : request.map(RequestInfoHolder.RequestInfo::userAgent).orElse(null))
            .reference(Strings.truncate(entry.getReference(), MAX_REFERENCE))
            .build();
    }

    /** Values become JSON-safe scalars: strings (cut at {@value #MAX_VALUE_LENGTH}), numbers, booleans. */
    private static List<AuditChange> normalize(List<AuditChange> changes) {
        return changes.stream()
            .map(change -> new AuditChange(change.field(), scalar(change.oldValue()), scalar(change.newValue())))
            .toList();
    }

    static Object scalar(Object value) {
        if (value == null || value instanceof Boolean || value instanceof Number) {
            return value;
        }
        if (value instanceof Enum<?> constant) {
            return constant.name();
        }
        String text = value instanceof TemporalAccessor ? value.toString() : String.valueOf(value);
        return text.length() <= MAX_VALUE_LENGTH ? text : text.substring(0, MAX_VALUE_LENGTH) + "…";
    }
}
