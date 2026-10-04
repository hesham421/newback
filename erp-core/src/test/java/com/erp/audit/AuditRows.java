package com.erp.audit;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

/** JDBC reads of {@code CORE_AUDIT_EVENT} for the audit tests (erp-core step 10), across tenants. */
final class AuditRows {

    private AuditRows() {
    }

    /** Every row of one entity, oldest first: id, tenant_id, action, actor, actor_realm, ip, user_agent, changes (text). */
    static List<Map<String, Object>> of(JdbcTemplate jdbc, String entityType, Object entityId) {
        return jdbc.queryForList("SELECT id, tenant_id, action, actor, actor_realm, actor_user_id, ip, user_agent,"
                + " summary_en, changes::text AS changes FROM core_audit_event"
                + " WHERE entity_type = ? AND entity_id = ? ORDER BY id",
            entityType, String.valueOf(entityId));
    }

    /** The rows of one entity with the given action. */
    static List<Map<String, Object>> of(JdbcTemplate jdbc, String entityType, Object entityId, String action) {
        return of(jdbc, entityType, entityId).stream().filter(row -> action.equals(row.get("action"))).toList();
    }

    /** The field names of a {@code CHANGES} array. */
    static List<String> changedFields(Map<String, Object> row) {
        Object changes = row.get("changes");
        return changes == null ? List.of() : JsonPath.read((String) changes, "$[*].field");
    }

    /**
     * The acceptance denylist scan: no field name of any {@code CHANGES} array in the whole table (all
     * tenants, every writer) contains a sensitive word, and no such word appears as a JSON key either.
     */
    static void assertNoSensitiveFieldAnywhere(JdbcTemplate jdbc) {
        List<String> allChanges = jdbc.queryForList(
            "SELECT changes::text FROM core_audit_event WHERE changes IS NOT NULL", String.class);
        assertThat(allChanges).as("audit rows with CHANGES").isNotEmpty();
        for (String json : allChanges) {
            List<String> fields = JsonPath.read(json, "$[*].field");
            for (String field : fields) {
                String lower = field.toLowerCase(Locale.ROOT);
                assertThat(AuditHttp.SENSITIVE_WORDS).as("sensitive field %s in %s", field, json)
                    .noneMatch(lower::contains);
            }
            List<Map<String, Object>> elements = JsonPath.read(json, "$[*]");
            elements.forEach(element -> assertThat(element.keySet()).containsExactlyInAnyOrder("field", "old", "new"));
        }
    }
}
