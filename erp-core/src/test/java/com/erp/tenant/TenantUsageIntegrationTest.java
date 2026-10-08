package com.erp.tenant;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.testsupport.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * tenant-maturity B — {@code GET /api/v1/platform/tenants/{id}/usage} (REQ-TENANT-028; SEC REQ-SEC-090, FILE
 * XM-FILE-001, NOTIF {@code countDispatchedSince}): every figure is counted inside the tenant asked for, by its
 * owner module, and never includes another tenant's rows (POL-TENANT-007).
 */
class TenantUsageIntegrationTest extends AbstractIntegrationTest {

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
        platformToken = http.token(TenantConstants.PLATFORM_TENANT_CODE,
            TenantHttp.platformOperator(jdbcTemplate, passwordEncoder));
    }

    @Test
    void aFreshTenant_hasOneStaffUser_andNothingElse() {
        long id = http.provisionTenant(platformToken, TenantHttp.unique("USE"));
        Instant before = Instant.now().minusSeconds(1);

        HttpResponse<String> usage = http.get(platformToken, TENANTS + "/" + id + "/usage");

        assertThat(usage.statusCode()).isEqualTo(200);
        assertThat(((Number) JsonPath.read(usage.body(), "$.data.id")).longValue()).isEqualTo(id);
        assertFigures(usage, 1, 0, 0, 0, 0, 0);
        assertThat(Instant.parse(JsonPath.read(usage.body(), "$.data.collectedAt"))).isAfter(before);
    }

    @Test
    void theFigures_countOnlyThatTenantsRows() {
        String codeA = TenantHttp.unique("USA");
        long tenantA = http.provisionTenant(platformToken, codeA);
        long tenantB = http.provisionTenant(platformToken, TenantHttp.unique("USB"));
        String tokenA = http.token(codeA, "admin");
        long adminA = ((Number) JsonPath.read(http.get(tokenA, "/api/v1/sec/me").body(), "$.data.userPk")).longValue();
        http.createUser(tokenA, "clerk");
        assertThat(http.uploadPng(tokenA, 4711, "usage.png").statusCode()).isEqualTo(201);
        assertThat(http.post(null, codeA, "/api/v1/public/customers/register",
            "{\"email\":\"c-" + codeA.toLowerCase() + "@shop.test\",\"password\":\"" + TenantHttp.PASSWORD
                + "\",\"fullName\":\"Customer\"}").statusCode()).isEqualTo(201);
        HttpResponse<String> dispatched = http.post(tokenA, "/api/v1/notifications/dispatch", "{\"recipientId\":" + adminA
            + ",\"templateCode\":\"PASSWORD_RESET\",\"channelHint\":[\"EMAIL\"],\"moduleCode\":\"TEST\","
            + "\"referenceType\":\"TC_REF\",\"referenceId\":1,\"variables\":{\"actionLink\":\"http://x/\",\"expiresAt\":\"soon\"}}");
        assertThat(dispatched.statusCode()).as(dispatched.body()).isEqualTo(200);
        long bytesA = jdbcTemplate.queryForObject("select coalesce(sum(file_size), 0) from file_document"
            + " where tenant_id = ? and file_status_id <> 'DELETED'", Long.class, tenantA);
        assertThat(bytesA).isPositive();

        // the dispatch above and the customer's verification e-mail: NOTIF writes one log row each
        long notificationsA = jdbcTemplate.queryForObject("select count(*) from notif_log where tenant_id = ?",
            Long.class, tenantA);
        assertThat(notificationsA).isGreaterThanOrEqualTo(1);

        HttpResponse<String> usageA = http.get(platformToken, TENANTS + "/" + tenantA + "/usage");
        assertThat(usageA.statusCode()).isEqualTo(200);
        assertFigures(usageA, 2, 1, 1, 1, bytesA, notificationsA);

        HttpResponse<String> usageB = http.get(platformToken, TENANTS + "/" + tenantB + "/usage");
        assertFigures(usageB, 1, 0, 0, 0, 0, 0);
    }

    @Test
    void anUnknownTenant_is404() {
        HttpResponse<String> missing = http.get(platformToken, TENANTS + "/987654321/usage");

        assertThat(missing.statusCode()).isEqualTo(404);
        assertThat(TenantHttp.errorCode(missing)).isEqualTo("TENANT_NOT_FOUND");
    }

    private static void assertFigures(HttpResponse<String> usage, int staff, int customers, int sessions,
                                      long documents, long bytes, long notifications) {
        assertThat((Integer) JsonPath.read(usage.body(), "$.data.staffUsers")).isEqualTo(staff);
        assertThat((Integer) JsonPath.read(usage.body(), "$.data.customerUsers")).isEqualTo(customers);
        assertThat((Integer) JsonPath.read(usage.body(), "$.data.activeSessions")).isEqualTo(sessions);
        assertThat(((Number) JsonPath.read(usage.body(), "$.data.fileDocuments")).longValue()).isEqualTo(documents);
        assertThat(((Number) JsonPath.read(usage.body(), "$.data.fileBytes")).longValue()).isEqualTo(bytes);
        assertThat(((Number) JsonPath.read(usage.body(), "$.data.notificationsLast30Days")).longValue())
            .isEqualTo(notifications);
    }
}
