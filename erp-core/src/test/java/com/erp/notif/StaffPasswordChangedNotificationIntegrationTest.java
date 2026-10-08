package com.erp.notif;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.erp.events.UserPasswordChangedEvent;
import com.erp.testsupport.AbstractAsyncIntegrationTest;
import com.erp.testsupport.StaffApiClient;
import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * erp-core 1.3.0 (TM-D D.3): a staff password set by an administrator or changed by its owner publishes
 * {@link UserPasswordChangedEvent} after commit (REQ-SEC-089), and NOTIF answers with a
 * {@code STAFF_PASSWORD_CHANGED} e-mail to that user (RULE-NOTIF-023). V17 seeded the template in every
 * tenant; a tenant provisioned later copies it from PLATFORM.
 */
class StaffPasswordChangedNotificationIntegrationTest extends AbstractAsyncIntegrationTest {

    private static String tenant;

    @LocalServerPort
    private int port;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private StaffApiClient api;
    private String adminToken;

    @BeforeEach
    void tenantAndAdministrator() {
        api = new StaffApiClient(port);
        if (tenant == null) {
            tenant = StaffApiClient.unique("NTFD");
            api.provisionTenant(api.token("PLATFORM", StaffApiClient.platformOperator(jdbcTemplate, passwordEncoder)), tenant);
        }
        adminToken = api.token(tenant, StaffApiClient.TENANT_ADMIN);
    }

    @Test
    void theTemplateExistsInPlatform_andIsCopiedToANewTenant() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("SELECT t.CODE, n.SUBJECT_EN, n.BODY_EN, n.IS_ACTIVE_FL"
            + " FROM NOTIF_TEMPLATE n JOIN CORE_TENANT t ON t.ID = n.TENANT_ID WHERE n.TEMPLATE_CODE = 'STAFF_PASSWORD_CHANGED'"
            + " AND t.CODE IN ('PLATFORM', ?)", tenant);
        assertThat(rows).extracting(row -> row.get("code")).containsExactlyInAnyOrder("PLATFORM", tenant);
        assertThat(rows).allSatisfy(row -> {
            assertThat((String) row.get("body_en")).contains("{changedAt}").contains("{changedBy}");
            assertThat(((Number) row.get("is_active_fl")).intValue()).isEqualTo(1);
        });
    }

    @Test
    void adminSetAndSelfChange_eachPublishTheEvent_andQueueTheEmail() {
        String username = StaffApiClient.unique("ntf-").toLowerCase();
        HttpResponse<String> created = api.post(adminToken, "/api/v1/sec/users", "{\"username\":\"" + username
            + "\",\"email\":\"" + username + "@ntf.test\",\"fullNameAr\":\"م\",\"fullNameEn\":\"N\",\"password\":\""
            + StaffApiClient.PASSWORD + "\",\"requireChangeAtNextLogin\":false}");
        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        long id = ((Number) JsonPath.read(created.body(), "$.data.userPk")).longValue();

        assertThat(api.put(adminToken, "/api/v1/sec/users/" + id + "/password", "{\"newPassword\":\"Admin-Set-Passw0rd1\"}")
            .statusCode()).isEqualTo(200);
        UserPasswordChangedEvent byAdmin = awaitEvent(id, true);
        assertThat(byAdmin.getActor()).isEqualTo(StaffApiClient.TENANT_ADMIN);
        assertThat(byAdmin.getTenantId()).isEqualTo(tenantId());
        awaitLogs(id, 1);

        String token = JsonPath.read(api.login(tenant, username, "Admin-Set-Passw0rd1").body(), "$.data.accessToken");
        assertThat(api.put(token, "/api/v1/sec/me/password",
            "{\"currentPassword\":\"Admin-Set-Passw0rd1\",\"newPassword\":\"Own-Passw0rd2\"}").statusCode()).isEqualTo(200);
        assertThat(awaitEvent(id, false).getActor()).isEqualTo(username);
        awaitLogs(id, 2);
        assertThat(jdbcTemplate.queryForList("SELECT CHANNEL_TYPE_ID FROM NOTIF_LOG WHERE TENANT_ID = ? AND RECIPIENT_ID = ?",
            String.class, tenantId(), id)).containsOnly("EMAIL");
    }

    @Test
    void aRefusedChange_publishesNothing() {
        String username = StaffApiClient.unique("ntf-no-").toLowerCase();
        HttpResponse<String> created = api.post(adminToken, "/api/v1/sec/users", "{\"username\":\"" + username
            + "\",\"email\":\"" + username + "@ntf.test\",\"fullNameAr\":\"م\",\"fullNameEn\":\"N\",\"password\":\""
            + StaffApiClient.PASSWORD + "\",\"requireChangeAtNextLogin\":false}");
        long id = ((Number) JsonPath.read(created.body(), "$.data.userPk")).longValue();

        assertThat(api.put(adminToken, "/api/v1/sec/users/" + id + "/password", "{\"newPassword\":\"weak\"}").statusCode())
            .isEqualTo(400);
        awaitExecutorIdle();
        assertThat(probe.find(UserPasswordChangedEvent.class, e -> e.getUserId() == id)).isEmpty();
        assertThat(logCount(id)).isZero();
    }

    private UserPasswordChangedEvent awaitEvent(long userId, boolean byAdmin) {
        return (UserPasswordChangedEvent) await().atMost(ASYNC_TIMEOUT).until(
                () -> probe.find(UserPasswordChangedEvent.class, e -> e.getUserId() == userId && e.isByAdmin() == byAdmin),
                java.util.Optional::isPresent)
            .orElseThrow().event();
    }

    private void awaitLogs(long userId, int expected) {
        await().atMost(ASYNC_TIMEOUT).until(() -> logCount(userId) == expected);
    }

    private int logCount(long userId) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM NOTIF_LOG l JOIN NOTIF_TEMPLATE t ON t.ID = l.TEMPLATE_FK"
            + " WHERE l.TENANT_ID = ? AND l.RECIPIENT_ID = ? AND t.TEMPLATE_CODE = 'STAFF_PASSWORD_CHANGED'"
            + " AND l.MODULE_CODE = 'SEC' AND l.REFERENCE_TYPE = 'SEC_USER'", Integer.class, tenantId(), userId);
        return count == null ? 0 : count;
    }

    private long tenantId() {
        return jdbcTemplate.queryForObject("SELECT ID FROM CORE_TENANT WHERE CODE = ?", Long.class, tenant);
    }
}
