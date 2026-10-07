package com.erp.audit.service;

import com.erp.audit.crossmodule.AuditChange;
import com.erp.audit.crossmodule.AuditEntry;
import com.erp.common.util.PlainJson;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * The write side of {@code CORE_AUDIT_EVENT} (erp-core step 10): plain JDBC.
 *
 * <p>Why JDBC and not the JPA repository: the {@code @Audited} listener records from inside a
 * Hibernate flush ({@code PostInsert/PostUpdate/PostDelete}), where persisting another entity through
 * the same session is unsafe (the action queue is being executed, and an insert queued then may never
 * be flushed). Every row is therefore inserted with one statement on the writer's own connection —
 * the Hibernate session's connection for the listener, the Spring-transaction-bound connection
 * ({@link JdbcTemplate}) for explicit {@code AuditApi.record} calls — so it commits or rolls back with
 * the change it describes.
 *
 * <p>Tenant rule for raw SQL (erp-core step 05): every statement names {@code TENANT_ID} explicitly.
 * Timestamps are bound as UTC wall-clock {@code LocalDateTime}s, exactly what Hibernate writes for an
 * {@code Instant} into a {@code TIMESTAMP} column, so the JPA read model reads them back unchanged.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditEventStore {

    static final String INSERT = "INSERT INTO CORE_AUDIT_EVENT (ID, TENANT_ID, OCCURRED_AT, ACTOR, ACTOR_REALM,"
        + " ACTOR_USER_ID, ACTION, ENTITY_TYPE, ENTITY_ID, SUMMARY_AR, SUMMARY_EN, CHANGES, IP, USER_AGENT, REFERENCE,"
        + " CREATED_BY, CREATED_AT, UPDATED_BY, UPDATED_AT, VERSION)"
        + " VALUES (nextval('SEQ_CORE_AUDIT_EVENT'), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSONB), ?, ?, ?, ?, ?, ?, ?, 0)";

    /** A private mapper: the CHANGES shape is fixed, independent of the application's JSON settings. */
    private static final JsonMapper MAPPER = PlainJson.MAPPER;

    private final JdbcTemplate jdbcTemplate;

    /** Inserts a fully resolved entry on the connection bound to the current Spring transaction. */
    public void insert(AuditEntry entry) {
        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            insert(connection, entry);
            return null;
        });
    }

    /** Inserts a fully resolved entry on {@code connection} (the caller owns the connection). */
    public void insert(Connection connection, AuditEntry entry) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(INSERT)) {
            LocalDateTime now = utc(Instant.now());
            int i = 1;
            statement.setLong(i++, entry.getTenantId());
            statement.setObject(i++, utc(entry.getOccurredAt()));
            statement.setString(i++, entry.getActor());
            statement.setString(i++, entry.getActorRealm());
            if (entry.getActorUserId() == null) {
                statement.setNull(i++, Types.BIGINT);
            } else {
                statement.setLong(i++, entry.getActorUserId());
            }
            statement.setString(i++, entry.getAction());
            statement.setString(i++, entry.getEntityType());
            statement.setString(i++, entry.getEntityId());
            statement.setString(i++, entry.getSummaryAr());
            statement.setString(i++, entry.getSummaryEn());
            statement.setString(i++, toJson(entry.getChanges()));
            statement.setString(i++, entry.getIp());
            statement.setString(i++, entry.getUserAgent());
            statement.setString(i++, entry.getReference());
            statement.setString(i++, entry.getActor());
            statement.setObject(i++, now);
            statement.setString(i++, entry.getActor());
            statement.setObject(i, now);
            statement.executeUpdate();
        }
    }

    /**
     * Deletes every row that occurred before {@code cutoff}, tenant by tenant (the retention job runs
     * outside any tenant). Returns the number of rows deleted.
     */
    public int deleteOccurredBefore(Instant cutoff) {
        LocalDateTime limit = utc(cutoff);
        List<Long> tenants = jdbcTemplate.queryForList(
            "SELECT DISTINCT TENANT_ID FROM CORE_AUDIT_EVENT WHERE OCCURRED_AT < ?", Long.class, limit);
        int deleted = 0;
        for (Long tenantId : tenants) {
            int rows = jdbcTemplate.update(
                "DELETE FROM CORE_AUDIT_EVENT WHERE TENANT_ID = ? AND OCCURRED_AT < ?", tenantId, limit);
            log.info("Audit retention deleted {} rows of tenant {} older than {}", rows, tenantId, cutoff);
            deleted += rows;
        }
        return deleted;
    }

    /** The JSON array of {@code {field, old, new}}, or {@code null} when there is no change. */
    static String toJson(List<AuditChange> changes) {
        if (changes == null || changes.isEmpty()) {
            return null;
        }
        List<Map<String, Object>> array = changes.stream().map(change -> {
            Map<String, Object> element = new LinkedHashMap<>();
            element.put("field", change.field());
            element.put("old", change.oldValue());
            element.put("new", change.newValue());
            return element;
        }).toList();
        return MAPPER.writeValueAsString(array);
    }

    private static LocalDateTime utc(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }
}
