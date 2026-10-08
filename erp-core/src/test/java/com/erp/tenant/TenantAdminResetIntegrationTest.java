package com.erp.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.erp.testsupport.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * tenant-maturity B — {@code POST /api/v1/platform/tenants/{id}/admin-reset} (REQ-TENANT-027, RULE-TENANT-017;
 * SEC REQ-SEC-091): the platform operator recovers a tenant's super administrator, inside that tenant, in one
 * transaction; refusals change nothing. Fixtures over HTTP; the audit and session rows are read with JDBC.
 */
class TenantAdminResetIntegrationTest extends AbstractIntegrationTest {

    private static final String TENANTS = "/api/v1/platform/tenants";
    private static final String NEW_PASSWORD = "Rec0vered-Passw0rd";

    @Value("${local.server.port}")
    private int port;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private TenantHttp http;
    private String operator;
    private String platformToken;
    private String code;
    private long tenantId;

    @BeforeEach
    void aTenantWithItsAdministrator() {
        http = new TenantHttp(port);
        operator = TenantHttp.platformOperator(jdbcTemplate, passwordEncoder);
        platformToken = http.token(TenantConstants.PLATFORM_TENANT_CODE, operator);
        code = TenantHttp.unique("RST");
        tenantId = http.provisionTenant(platformToken, code);
    }

    @Test
    void unknownOrNonSuperTargets_weakPasswordsAndUnknownTenants_areRefused_andNothingChanges() {
        String adminToken = http.token(code, "admin");
        String clerk = "clerk-" + code.toLowerCase();
        http.createUser(adminToken, clerk);

        assertRefused(reset(tenantId, "ghost", NEW_PASSWORD), 404, "TENANT_ADMIN_NOT_FOUND");
        assertRefused(reset(tenantId, clerk, NEW_PASSWORD), 422, "TENANT_ADMIN_NOT_SUPER");
        HttpResponse<String> weak = reset(tenantId, "admin", "abcdefgh");
        assertRefused(weak, 400, "SEC-400-PASSWORD-POLICY");
        assertThat((String) JsonPath.read(weak.body(), "$.error.fieldErrors[0].field")).isEqualTo("newPassword");
        assertRefused(reset(987654321L, "admin", NEW_PASSWORD), 404, "TENANT_NOT_FOUND");
        assertRefused(http.post(platformToken, TENANTS + "/" + tenantId + "/admin-reset", "{\"newPassword\":\"x\"}"),
            400, "VALIDATION_ERROR");

        // review round 1: never on PLATFORM itself — not even the operator's own account (RULE-SEC-057 stays whole)
        assertRefused(reset(TenantConstants.PLATFORM_TENANT_ID, operator, NEW_PASSWORD), 422, "TENANT_ADMIN_RESET_PLATFORM");
        assertRefused(reset(TenantConstants.PLATFORM_TENANT_ID, clerk, NEW_PASSWORD), 422, "TENANT_ADMIN_RESET_PLATFORM");
        assertThat(http.login(TenantConstants.PLATFORM_TENANT_CODE, operator, TenantHttp.PASSWORD).statusCode()).isEqualTo(200);

        assertThat(http.get(adminToken, "/api/v1/sec/me").statusCode()).isEqualTo(200);
        assertThat(http.login(code, "admin", TenantHttp.PASSWORD).statusCode()).isEqualTo(200);
        assertThat(auditRows()).isEmpty();
        assertThat(platformAuditRows()).isEmpty();
    }

    @Test
    void aReset_setsThePassword_endsEverySession_forcesAChange_auditsInTheTargetTenant_andMailsTheUser() {
        String adminToken = http.token(code, "admin");
        // a user of the target tenant that carries the operator's name must never be taken for the operator
        http.createUser(adminToken, operator);
        long adminId = ((Number) JsonPath.read(http.get(adminToken, "/api/v1/sec/me").body(), "$.data.userPk")).longValue();

        HttpResponse<String> done = reset(tenantId, "admin", NEW_PASSWORD);

        assertThat(done.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(done.body(), "$.data.username")).isEqualTo("admin");
        assertThat((Integer) JsonPath.read(done.body(), "$.data.sessionsTerminated")).isEqualTo(1);
        assertThat(done.body()).doesNotContain(NEW_PASSWORD);

        assertThat(http.get(adminToken, "/api/v1/sec/me").statusCode()).isEqualTo(401);
        assertThat(http.login(code, "admin", TenantHttp.PASSWORD).statusCode()).isEqualTo(401);
        HttpResponse<String> login = http.login(code, "admin", NEW_PASSWORD);
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat((Boolean) JsonPath.read(login.body(), "$.data.passwordChangeRequired")).isTrue();

        List<Map<String, Object>> audit = auditRows();
        assertThat(audit).hasSize(1);
        assertThat(audit.get(0)).containsEntry("actor", operator).containsEntry("actor_user_id", null)
            .containsEntry("actor_realm", "STAFF").containsEntry("entity_type", "SEC_USER")
            .containsEntry("entity_id", String.valueOf(adminId));
        assertThat(audit.get(0).toString()).doesNotContain(NEW_PASSWORD).doesNotContain("$2a$");
        assertThat(jdbcTemplate.queryForList("select actor_user_id from sec_audit_log where tenant_id = ?"
            + " and event_type_code = 'SESSION_TERMINATED'", Long.class, tenantId)).containsExactly((Long) null);
        assertThat(jdbcTemplate.queryForObject("select count(*) from core_audit_event where tenant_id = ?"
            + " and action = 'ADMIN_PASSWORD_RESET'", Integer.class, TenantConstants.PLATFORM_TENANT_ID)).isZero();

        // review round 1: the PLATFORM trace of the recovery; the target tenant still has exactly its own row
        List<Map<String, Object>> platform = platformAuditRows();
        assertThat(platform).hasSize(1);
        assertThat(platform.get(0)).containsEntry("actor", operator).containsEntry("actor_realm", "STAFF")
            .containsEntry("entity_type", "CORE_TENANT").containsEntry("entity_id", String.valueOf(tenantId));
        assertThat((String) platform.get(0).get("summary_en")).contains("admin").contains(code)
            .contains("sessions terminated: 1");
        assertThat(platform.get(0).toString()).doesNotContain(NEW_PASSWORD).doesNotContain("$2a$");
        assertThat(auditRows()).hasSize(1);

        await().atMost(Duration.ofSeconds(20)).until(() -> jdbcTemplate.queryForObject("select count(*) from notif_log l"
            + " join notif_template t on t.id = l.template_fk where l.tenant_id = ? and l.recipient_id = ?"
            + " and t.template_code = 'STAFF_PASSWORD_CHANGED'", Integer.class, tenantId, adminId) == 1);
    }

    @Test
    void aReset_withRequireChangeFalse_leavesNoForcedChange() {
        HttpResponse<String> done = http.post(platformToken, TENANTS + "/" + tenantId + "/admin-reset",
            "{\"username\":\"admin\",\"newPassword\":\"" + NEW_PASSWORD + "\",\"requireChangeAtNextLogin\":false}");

        assertThat(done.statusCode()).isEqualTo(200);
        assertThat((Integer) JsonPath.read(done.body(), "$.data.sessionsTerminated")).isZero();
        HttpResponse<String> login = http.login(code, "admin", NEW_PASSWORD);
        assertThat(login.statusCode()).isEqualTo(200);
        assertThat((Boolean) JsonPath.read(login.body(), "$.data.passwordChangeRequired")).isFalse();
    }

    private HttpResponse<String> reset(long id, String username, String password) {
        return http.post(platformToken, TENANTS + "/" + id + "/admin-reset",
            "{\"username\":\"" + username + "\",\"newPassword\":\"" + password + "\"}");
    }

    private List<Map<String, Object>> auditRows() {
        return jdbcTemplate.queryForList("select actor, actor_user_id, actor_realm, entity_type, entity_id,"
            + " summary_ar, summary_en, changes::text as changes from core_audit_event"
            + " where tenant_id = ? and action = 'ADMIN_PASSWORD_RESET'", tenantId);
    }

    private List<Map<String, Object>> platformAuditRows() {
        return jdbcTemplate.queryForList("select actor, actor_realm, entity_type, entity_id, summary_ar, summary_en,"
            + " changes::text as changes from core_audit_event where tenant_id = ? and action = 'TENANT_ADMIN_RESET'"
            + " and entity_id = ?", TenantConstants.PLATFORM_TENANT_ID, String.valueOf(tenantId));
    }

    private static void assertRefused(HttpResponse<String> response, int status, String code) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(status);
        assertThat(TenantHttp.errorCode(response)).isEqualTo(code);
    }
}
