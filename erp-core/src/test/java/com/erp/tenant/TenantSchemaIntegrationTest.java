package com.erp.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.common.domain.AuditableEntity;
import com.erp.common.domain.GlobalAuditableEntity;
import com.erp.testsupport.AbstractIntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.EntityType;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * erp-core step 05 acceptance, checked against the migrated database and the JPA metamodel:
 * every tenant-scoped table has {@code TENANT_ID NOT NULL} without a default (plus FK, index and
 * composite uniqueness), exactly the four global tables lack it, and every entity extends the
 * tenant-aware {@link AuditableEntity} except the four global ones.
 */
class TenantSchemaIntegrationTest extends AbstractIntegrationTest {

    /** Base tables that legitimately have no TENANT_ID (the step file's global list + CORE_TENANT). */
    private static final Set<String> GLOBAL_TABLES =
        Set.of("core_tenant", "sec_module_reg", "sec_screen_reg", "sec_action_reg", "flyway_schema_history");

    /** Entities that extend GlobalAuditableEntity instead (acceptance: the three Sec*Reg and Tenant). */
    private static final Set<String> GLOBAL_ENTITIES = Set.of("ModuleRegistry", "ScreenRegistry", "ActionRegistry", "Tenant");

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void everyScopedTable_hasTenantIdNotNull_withoutDefault() {
        List<Map<String, Object>> columns = jdbcTemplate.queryForList(
            "select table_name, is_nullable, column_default, data_type from information_schema.columns"
                + " where table_schema = 'public' and column_name = 'tenant_id' order by table_name");

        assertThat(columns).hasSize(20); // 18 (step 05) + SEC_CUSTOMER_VERIFY_TOKEN (step 06) + NOTIF_INBOX (step 08)
        assertThat(columns).allSatisfy(column -> {
            assertThat(column.get("is_nullable")).as("%s nullable", column.get("table_name")).isEqualTo("NO");
            assertThat(column.get("column_default")).as("%s default", column.get("table_name")).isNull();
            assertThat(column.get("data_type")).as("%s type", column.get("table_name")).isEqualTo("bigint");
        });

        List<String> tablesWithoutTenantId = jdbcTemplate.queryForList(
            "select t.table_name from information_schema.tables t where t.table_schema = 'public'"
                + " and t.table_type = 'BASE TABLE' and not exists (select 1 from information_schema.columns c"
                + " where c.table_schema = t.table_schema and c.table_name = t.table_name and c.column_name = 'tenant_id')",
            String.class);
        assertThat(tablesWithoutTenantId).containsExactlyInAnyOrderElementsOf(GLOBAL_TABLES);
    }

    @Test
    void everyScopedTable_hasAForeignKeyToCoreTenant_anIndex_andCompositeUniqueConstraints() {
        List<String> scoped = jdbcTemplate.queryForList(
            "select table_name from information_schema.columns where table_schema = 'public'"
                + " and column_name = 'tenant_id'", String.class);

        List<String> withFk = jdbcTemplate.queryForList(
            "select distinct cl.relname from pg_constraint co join pg_class cl on cl.oid = co.conrelid"
                + " join pg_class ref on ref.oid = co.confrelid where co.contype = 'f' and ref.relname = 'core_tenant'",
            String.class);
        assertThat(withFk).containsExactlyInAnyOrderElementsOf(scoped);

        List<String> withIndex = jdbcTemplate.queryForList(
            "select distinct tablename from pg_indexes where schemaname = 'public'"
                + " and indexdef like '%(tenant_id)'", String.class);
        assertThat(withIndex).containsAll(scoped);

        // every UNIQUE constraint of a scoped table includes TENANT_ID
        Map<String, String> uniqueColumns = new TreeMap<>();
        jdbcTemplate.query(
            "select co.conname, cl.relname, string_agg(a.attname, ',' order by a.attnum) cols"
                + " from pg_constraint co join pg_class cl on cl.oid = co.conrelid"
                + " join pg_attribute a on a.attrelid = co.conrelid and a.attnum = any(co.conkey)"
                + " where co.contype = 'u' group by co.conname, cl.relname",
            rs -> {
                if (scoped.contains(rs.getString("relname"))) {
                    uniqueColumns.put(rs.getString("conname"), rs.getString("cols"));
                }
            });
        assertThat(uniqueColumns).hasSize(14); // 13 (step 05) + UQ_SEC_CUSTOMER_VERIFY_TOKEN_HASH (step 06)
        assertThat(uniqueColumns.values()).allSatisfy(cols -> assertThat(cols.split(",")).contains("tenant_id"));
    }

    @Test
    void everyEntity_extendsAuditableEntity_exceptTheFourGlobalOnes() {
        Map<Boolean, Set<String>> byTenantAware = entityManagerFactory.getMetamodel().getEntities().stream()
            .map(EntityType::getJavaType)
            .peek(type -> assertThat(GlobalAuditableEntity.class).isAssignableFrom(type))
            .collect(Collectors.partitioningBy(AuditableEntity.class::isAssignableFrom,
                Collectors.mapping(Class::getSimpleName, Collectors.toSet())));

        assertThat(byTenantAware.get(false)).containsExactlyInAnyOrderElementsOf(GLOBAL_ENTITIES);
        assertThat(byTenantAware.get(true)).hasSize(20); // + CustomerVerifyToken (step 06) + NotificationInboxItem (step 08)
    }

    @Test
    void thePlatformTenantIsSeeded_andOnlyItsSysAdminHoldsPlatformTenantManage() {
        assertThat(jdbcTemplate.queryForMap("select id, code, status_code from core_tenant where id = 1"))
            .containsEntry("code", "PLATFORM").containsEntry("status_code", "ACTIVE");

        List<String> holders = jdbcTemplate.queryForList(
            "select r.tenant_id || ':' || r.code from sec_role_action_grant g"
                + " join sec_action_reg a on a.action_reg_pk = g.action_id"
                + " join sec_role r on r.role_pk = g.role_id where a.permission_code = 'PLATFORM_TENANT_MANAGE'",
            String.class);
        assertThat(holders).containsExactly("1:SYS_ADMIN");
        assertThat(jdbcTemplate.queryForList(
            "select r.code from sec_user_role ur join sec_role r on r.role_pk = ur.role_id"
                + " join sec_user u on u.user_pk = ur.user_id where u.username = 'admin' and u.tenant_id = 1",
            String.class)).containsExactly("SYS_ADMIN");
    }
}
