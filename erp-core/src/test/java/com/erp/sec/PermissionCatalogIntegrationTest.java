package com.erp.sec;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.sec.permission.PermissionContributor;
import com.erp.sec.permission.PermissionDef;
import com.erp.sec.permission.PermissionModule;
import com.erp.sec.permission.PermissionScreen;
import com.erp.sec.service.MenuService;
import com.erp.sec.service.PermissionCatalogSynchronizer;
import com.erp.tenant.TenantContext;
import com.erp.testsupport.AbstractIntegrationTest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * erp-core step 06 — the pluggable permission catalog: the synchronizer is idempotent, the core
 * contributors reproduce the seeded V7/V10 catalog row for row, and a super role holds a permission
 * that only a test-only contributor declares (no grant row), while non-super roles do not and super
 * roles of other tenants never receive the PLATFORM module's permissions.
 */
@Import(PermissionCatalogIntegrationTest.TestOnlyPermissions.class)
class PermissionCatalogIntegrationTest extends AbstractIntegrationTest {

    static final String TEST_AUTHORITY = "PERM_TSTX_THINGS_VIEW";

    /** A contributor that exists only in this test's context (module TSTX, one screen, one VIEW). */
    @TestConfiguration
    static class TestOnlyPermissions {

        @Bean
        PermissionContributor testOnlyPermissionContributor() {
            return new PermissionContributor() {
                private final PermissionScreen screen = new PermissionScreen("TSTX", "TSTX_THINGS", "أشياء", "Things");

                @Override
                public List<PermissionDef> permissions() {
                    return List.of(PermissionDef.of(screen, "VIEW", "عرض"));
                }

                @Override
                public List<PermissionModule> modules() {
                    return List.of(new PermissionModule("TSTX", "وحدة اختبار", "Test-only module"));
                }

                @Override
                public List<PermissionScreen> screens() {
                    return List.of(screen);
                }
            };
        }
    }

    @Autowired
    private PermissionCatalogSynchronizer synchronizer;
    @Autowired
    private List<PermissionContributor> contributors;
    @Autowired
    private MenuService menuService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void synchronizer_isIdempotent_rowCountsStable() {
        Map<String, Integer> before = registryCounts();

        PermissionCatalogSynchronizer.Result first = synchronizer.synchronize();
        PermissionCatalogSynchronizer.Result second = synchronizer.synchronize();

        assertThat(registryCounts()).isEqualTo(before);
        // the startup run already wrote everything (including the test-only contributor's rows)
        assertThat(first.changedNothing()).as("%s", first).isTrue();
        assertThat(second.changedNothing()).as("%s", second).isTrue();
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from sec_action_reg where permission_code = ?", Integer.class, TEST_AUTHORITY)).isEqualTo(1);
    }

    @Test
    void coreContributors_reproduceTheSeededCatalog_rowForRow() {
        List<PermissionDef> core = contributors.stream()
            .filter(c -> c.getClass().getName().startsWith("com.erp.") && !c.getClass().getName().contains("Test"))
            .flatMap(c -> c.permissions().stream())
            .toList();
        // V7: 38 actions of SEC/MDL/NOTIF/FILE/CU; V10: 2 PLATFORM actions; V12 (step 07): FILE:DOCUMENT:PUBLISH
        assertThat(core).hasSize(41);
        assertThat(core.stream().map(PermissionDef::authority).collect(Collectors.toSet())).hasSize(41);

        Set<String> seeded = Set.copyOf(jdbcTemplate.queryForList(
            "select a.permission_code from sec_action_reg a join sec_screen_reg s on s.screen_reg_pk = a.screen_id"
                + " join sec_module_reg m on m.module_reg_pk = s.module_id"
                + " where m.code in ('SEC','MDL','NOTIF','FILE','CU','PLATFORM')", String.class));
        assertThat(core.stream().map(PermissionDef::authority).collect(Collectors.toSet()))
            .containsExactlyInAnyOrderElementsOf(seeded);

        for (PermissionDef def : core) {
            Map<String, Object> row = jdbcTemplate.queryForMap(
                "select m.code module_code, s.page_code, a.action_code, a.name_ar, a.name_en"
                    + " from sec_action_reg a join sec_screen_reg s on s.screen_reg_pk = a.screen_id"
                    + " join sec_module_reg m on m.module_reg_pk = s.module_id where a.permission_code = ?",
                def.authority());
            assertThat(row).as(def.authority())
                .containsEntry("module_code", def.moduleCode())
                .containsEntry("page_code", def.screenCode())
                .containsEntry("action_code", def.actionCode())
                .containsEntry("name_ar", def.nameAr())
                .containsEntry("name_en", def.nameEn());
        }

        // every seeded screen (18, including SEC's three public screens) is declared with its seeded names
        List<PermissionScreen> screens = contributors.stream()
            .filter(c -> !c.getClass().getName().contains("Test"))
            .flatMap(c -> c.screens().stream()).toList();
        assertThat(screens).hasSize(18);
        for (PermissionScreen screen : screens) {
            assertThat(jdbcTemplate.queryForMap("select name_ar, name_en from sec_screen_reg where page_code = ?",
                screen.screenCode())).as(screen.screenCode())
                .containsEntry("name_ar", screen.nameAr()).containsEntry("name_en", screen.nameEn());
        }
    }

    @Test
    void superRole_holdsAPermissionContributedByATestOnlyContributor_withoutAnyGrant() {
        assertThat(jdbcTemplate.queryForObject("select count(*) from sec_role_action_grant g join sec_action_reg a"
            + " on a.action_reg_pk = g.action_id where a.permission_code = ?", Integer.class, TEST_AUTHORITY)).isZero();

        String superUser = userWithRole(1L, "SYS_ADMIN");
        Set<String> superCodes = authoritiesOf(1L, superUser);
        assertThat(superCodes).contains(TEST_AUTHORITY, "PERM_SEC_USERS_VIEW", "PERM_TSTX_THINGS_VIEW",
            "PLATFORM_TENANT_MANAGE", "PERM_PLATFORM_TENANTS_VIEW");

        String plainUser = userWithRole(1L, "FILE_ADMIN");
        Set<String> plainCodes = authoritiesOf(1L, plainUser);
        assertThat(plainCodes).contains("PERM_FILE_BROWSER_VIEW").doesNotContain(TEST_AUTHORITY, "PERM_SEC_USERS_VIEW");
    }

    @Test
    void superRoleOfAnotherTenant_neverHoldsThePlatformModulesPermissions() {
        long tenantId = jdbcTemplate.queryForObject("select nextval('SEQ_CORE_TENANT')", Long.class);
        String code = "PC" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
        jdbcTemplate.update("insert into core_tenant (id, code, name_ar, name_en, status_code, created_by, created_at)"
            + " values (?, ?, 'مستأجر', 'Tenant', 'ACTIVE', 'test', now())", tenantId, code);
        jdbcTemplate.update("insert into sec_role (role_pk, tenant_id, code, name_ar, name_en, is_active_fl, is_super,"
            + " created_by, created_at) values (nextval('SEQ_SEC_ROLE'), ?, 'SYS_ADMIN', 'مدير', 'Admin', true, true,"
            + " 'test', now())", tenantId);
        String admin = userWithRole(tenantId, "SYS_ADMIN");

        Set<String> codes = authoritiesOf(tenantId, admin);
        assertThat(codes).contains(TEST_AUTHORITY, "PERM_SEC_USERS_VIEW", "CONFIG_VIEW")
            .doesNotContain("PLATFORM_TENANT_MANAGE", "PERM_PLATFORM_TENANTS_VIEW");
    }

    // ------------------------------------------------------------------------------------------

    private Map<String, Integer> registryCounts() {
        return Map.of(
            "modules", jdbcTemplate.queryForObject("select count(*) from sec_module_reg", Integer.class),
            "screens", jdbcTemplate.queryForObject("select count(*) from sec_screen_reg", Integer.class),
            "actions", jdbcTemplate.queryForObject("select count(*) from sec_action_reg", Integer.class));
    }

    /** A committed STAFF account of {@code tenantId} holding that tenant's role {@code roleCode}. */
    private String userWithRole(long tenantId, String roleCode) {
        String username = "cat-" + UUID.randomUUID().toString().substring(0, 8);
        jdbcTemplate.update("insert into sec_user (user_pk, tenant_id, username, email, password_hash, full_name_ar,"
                + " full_name_en, status_code, realm, is_active_fl, created_by, created_at) values (nextval('SEQ_SEC_USER'),"
                + " ?, ?, ?, 'x', 'مستخدم', 'User', 'ACTIVE', 'STAFF', true, 'test', now())",
            tenantId, username, username + "@catalog.test");
        jdbcTemplate.update("insert into sec_user_role (user_role_pk, tenant_id, user_id, role_id, assigned_by, assigned_at)"
                + " select nextval('SEQ_SEC_USER_ROLE'), ?, u.user_pk, r.role_pk, 'test', now() from sec_user u"
                + " join sec_role r on r.tenant_id = u.tenant_id and r.code = ? where u.tenant_id = ? and u.username = ?",
            tenantId, roleCode, tenantId, username);
        return username;
    }

    /** The caller's effective authorities as the JWT filter computes them, in {@code tenantId}. */
    private Set<String> authoritiesOf(long tenantId, String username) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(username, null, List.of()));
        return TenantContext.callAs(tenantId, () -> menuService.effectiveAuthorityCodes().getData());
    }
}
