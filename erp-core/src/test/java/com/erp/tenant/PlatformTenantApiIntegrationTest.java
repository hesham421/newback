package com.erp.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import com.erp.testsupport.AbstractIntegrationTest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * erp-core step 05 — the platform tenant API ({@code /api/v1/platform/tenants}) over real HTTP:
 * provisioning (happy and failure paths), reads, and the status lifecycle with its effect on login
 * and on already issued tokens. Not transactional: every request commits, so fixtures use unique
 * tenant codes (a provisioned tenant's rows are invisible to every other tenant, PLATFORM included).
 */
class PlatformTenantApiIntegrationTest extends AbstractIntegrationTest {

    private static final String TENANTS = "/api/v1/platform/tenants";

    @Value("${local.server.port}")
    private int port;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private TenantHttp http;
    private String platformToken;

    @BeforeEach
    void platformOperatorLogsIn() {
        http = new TenantHttp(port);
        String operator = TenantHttp.platformOperator(jdbcTemplate, passwordEncoder);
        platformToken = http.token(TenantConstants.PLATFORM_TENANT_CODE, operator);
        assertThat(TenantHttp.tenantIdOf(platformToken)).isEqualTo(TenantConstants.PLATFORM_TENANT_ID);
    }

    @Test
    void create_provisionsTheTenant_whoseAdministratorLogsInWithXTenantCode() {
        String code = TenantHttp.unique("ACME");

        HttpResponse<String> created = http.createTenant(platformToken, code);

        assertThat(created.statusCode()).isEqualTo(201);
        long tenantId = ((Number) JsonPath.read(created.body(), "$.data.id")).longValue();
        assertThat(tenantId).isGreaterThan(TenantConstants.PLATFORM_TENANT_ID);
        assertThat((String) JsonPath.read(created.body(), "$.data.code")).isEqualTo(code);
        assertThat((String) JsonPath.read(created.body(), "$.data.statusCode")).isEqualTo("ACTIVE");
        assertThat(created.body()).doesNotContain(TenantHttp.PASSWORD);

        String tenantToken = http.token(code, "admin");
        assertThat(TenantHttp.tenantIdOf(tenantToken)).isEqualTo(tenantId);

        // the new tenant got the role catalog and its admin holds SYS_ADMIN (it may list users) ...
        HttpResponse<String> users = http.post(tenantToken, "/api/v1/sec/users/search", "{\"size\":100}");
        assertThat(users.statusCode()).isEqualTo(200);
        List<String> usernames = JsonPath.read(users.body(), "$.data.content[*].username");
        assertThat(usernames).containsExactly("admin");
        assertThat(jdbcTemplate.queryForList("select code from sec_role where tenant_id = ? order by code",
            String.class, tenantId)).containsExactly("CU_ADMIN", "FILE_ADMIN", "NOTIF_ADMIN", "SYS_ADMIN");
        assertThat(jdbcTemplate.queryForObject("select count(*) from mdl_lookup_type where tenant_id = ?",
            Integer.class, tenantId)).isEqualTo(4);
        // PASSWORD_RESET, ACCOUNT_ACTIVATION (V9) + CUSTOMER_VERIFY_EMAIL, CUSTOMER_PASSWORD_RESET (V11, step 06)
        assertThat(jdbcTemplate.queryForObject("select count(*) from notif_template where tenant_id = ?",
            Integer.class, tenantId)).isEqualTo(4);

        // ... but never PLATFORM_TENANT_MANAGE: the platform API stays closed to it
        assertThat(jdbcTemplate.queryForObject("select count(*) from sec_role_action_grant g"
                + " join sec_action_reg a on a.action_reg_pk = g.action_id"
                + " where g.tenant_id = ? and a.permission_code = 'PLATFORM_TENANT_MANAGE'",
            Integer.class, tenantId)).isZero();
        assertThat(http.get(tenantToken, TENANTS).statusCode()).isEqualTo(403);
        assertThat(http.createTenant(tenantToken, TenantHttp.unique("EVIL")).statusCode()).isEqualTo(403);
    }

    @Test
    void create_withAnInvalidCode_is400TenantCodeInvalid_andCreatesNothing() {
        for (String code : new String[] {"acme", "AB", "A-B", "X".repeat(33)}) {
            HttpResponse<String> response = http.createTenant(platformToken, code);
            assertThat(response.statusCode()).as(code).isEqualTo(400);
            assertThat(TenantHttp.errorCode(response)).as(code).isEqualTo("TENANT_CODE_INVALID");
        }
        assertThat(jdbcTemplate.queryForObject("select count(*) from core_tenant where code in ('ACME','AB')",
            Integer.class)).isZero();
    }

    @Test
    void create_withATakenCode_is409TenantCodeDuplicate() {
        String code = TenantHttp.unique("DUP");
        http.provisionTenant(platformToken, code);

        HttpResponse<String> again = http.createTenant(platformToken, code);

        assertThat(again.statusCode()).isEqualTo(409);
        assertThat(TenantHttp.errorCode(again)).isEqualTo("TENANT_CODE_DUPLICATE");
        assertThat(TenantHttp.errorCode(http.createTenant(platformToken, TenantConstants.PLATFORM_TENANT_CODE)))
            .isEqualTo("TENANT_CODE_DUPLICATE");
    }

    @Test
    void create_withMissingFields_is400ValidationError() {
        HttpResponse<String> response = http.post(platformToken, TENANTS, "{\"code\":\"" + TenantHttp.unique("V") + "\"}");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(TenantHttp.errorCode(response)).isEqualTo("VALIDATION_ERROR");
    }

    @Test
    void platformEndpoints_withoutAToken_are401() {
        assertThat(http.get(null, TENANTS).statusCode()).isEqualTo(401);
        assertThat(http.createTenant(null, TenantHttp.unique("ANON")).statusCode()).isEqualTo(401);
    }

    @Test
    void getById_list_andSearch_returnTenants_andAnUnknownIdIs404() {
        String code = TenantHttp.unique("READ");
        long id = http.provisionTenant(platformToken, code);

        HttpResponse<String> one = http.get(platformToken, TENANTS + "/" + id);
        assertThat(one.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(one.body(), "$.data.code")).isEqualTo(code);

        HttpResponse<String> list = http.get(platformToken, TENANTS + "?size=1000");
        assertThat(list.statusCode()).isEqualTo(200);
        List<String> codes = JsonPath.read(list.body(), "$.data.content[*].code");
        assertThat(codes).contains(TenantConstants.PLATFORM_TENANT_CODE, code);

        HttpResponse<String> search = http.post(platformToken, TENANTS + "/search",
            "{\"filters\":[{\"field\":\"code\",\"operator\":\"EQUALS\",\"value\":\"" + code + "\"}]}");
        assertThat(search.statusCode()).isEqualTo(200);
        List<String> found = JsonPath.read(search.body(), "$.data.content[*].code");
        assertThat(found).containsExactly(code);

        HttpResponse<String> missing = http.get(platformToken, TENANTS + "/987654321");
        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(TenantHttp.errorCode(missing)).isEqualTo("TENANT_NOT_FOUND");
    }

    @Test
    void suspend_blocksLoginWith403_andRevokesIssuedTokens_andActivateRestoresThem() {
        String code = TenantHttp.unique("SUSP");
        long id = http.provisionTenant(platformToken, code);
        String issuedBeforeSuspension = http.token(code, "admin");

        HttpResponse<String> suspended = http.patch(platformToken, TENANTS + "/" + id + "/status",
            "{\"statusCode\":\"SUSPENDED\"}");
        assertThat(suspended.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(suspended.body(), "$.data.statusCode")).isEqualTo("SUSPENDED");

        HttpResponse<String> login = http.login(code, "admin", TenantHttp.PASSWORD);
        assertThat(login.statusCode()).isEqualTo(403);
        assertThat(TenantHttp.errorCode(login)).isEqualTo("TENANT_SUSPENDED");

        HttpResponse<String> withOldToken = http.post(issuedBeforeSuspension, "/api/v1/sec/users/search", "{}");
        assertThat(withOldToken.statusCode()).isEqualTo(403);
        assertThat(TenantHttp.errorCode(withOldToken)).isEqualTo("TENANT_SUSPENDED");

        HttpResponse<String> activated = http.patch(platformToken, TENANTS + "/" + id + "/status",
            "{\"statusCode\":\"ACTIVE\"}");
        assertThat(activated.statusCode()).isEqualTo(200);
        assertThat(http.login(code, "admin", TenantHttp.PASSWORD).statusCode()).isEqualTo(200);
        assertThat(http.post(issuedBeforeSuspension, "/api/v1/sec/users/search", "{}").statusCode()).isEqualTo(200);
    }

    @Test
    void updateStatus_cannotSuspendThePlatformTenant_rejectsAnUnknownStatus_andAnUnknownTenant() {
        HttpResponse<String> platform = http.patch(platformToken,
            TENANTS + "/" + TenantConstants.PLATFORM_TENANT_ID + "/status", "{\"statusCode\":\"SUSPENDED\"}");
        assertThat(platform.statusCode()).isEqualTo(422);
        assertThat(TenantHttp.errorCode(platform)).isEqualTo("TENANT_PLATFORM_PROTECTED");

        HttpResponse<String> bogus = http.patch(platformToken,
            TENANTS + "/" + TenantConstants.PLATFORM_TENANT_ID + "/status", "{\"statusCode\":\"DELETED\"}");
        assertThat(bogus.statusCode()).isEqualTo(400);
        assertThat(TenantHttp.errorCode(bogus)).isEqualTo("VALIDATION_ERROR");

        HttpResponse<String> unknown = http.patch(platformToken, TENANTS + "/987654321/status",
            "{\"statusCode\":\"ACTIVE\"}");
        assertThat(unknown.statusCode()).isEqualTo(404);
        assertThat(TenantHttp.errorCode(unknown)).isEqualTo("TENANT_NOT_FOUND");

        assertThat(jdbcTemplate.queryForObject("select status_code from core_tenant where id = 1", String.class))
            .isEqualTo("ACTIVE");
    }
}
