package com.erp.sec;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.tenant.TenantConstants;
import com.erp.testsupport.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * erp-core step 14 — the staff side of SEC is STAFF-realm only (supersedes step 06 deviation 6 for
 * by-id management, search, sessions and the dashboard). Over HTTP, through the real chains, in a
 * freshly provisioned tenant per test: a CUSTOMER id on every staff by-id user endpoint answers 404
 * {@code SEC-404-USER} exactly like an unknown id and changes nothing; the staff user search never lists
 * a customer; a customer's session is neither listed nor terminable by staff; the dashboard counts staff
 * only; and an unverified customer cannot be turned ACTIVE by staff (the login stays 403
 * {@code CUSTOMER_NOT_VERIFIED}). Customer self-service ({@code /api/v1/customers/me}) is untouched.
 */
class StaffRealmIsolationIntegrationTest extends AbstractIntegrationTest {

    private static final String CUSTOMER_PASSWORD = "Cust0mer-Passw0rd!";

    @Value("${local.server.port}")
    private int port;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private RealmHttp http;
    private String tenantCode;
    private long tenantId;
    private String adminToken;

    @BeforeEach
    void freshTenant() {
        http = new RealmHttp(port);
        String platformToken = http.staffToken(TenantConstants.PLATFORM_TENANT_CODE,
            RealmHttp.platformOperator(jdbcTemplate, passwordEncoder), RealmHttp.PASSWORD);
        tenantCode = RealmHttp.unique("SR");
        tenantId = http.provisionTenant(platformToken, tenantCode);
        adminToken = http.staffToken(tenantCode, "admin", RealmHttp.PASSWORD);
    }

    @Test
    void everyStaffByIdUserEndpoint_onACustomerId_is404_andChangesNothing() {
        String email = uniqueEmail("pending");
        long customerId = register(email);
        long sysAdminRole = jdbcTemplate.queryForObject(
            "SELECT ROLE_PK FROM SEC_ROLE WHERE TENANT_ID = ? AND CODE = 'SYS_ADMIN'", Long.class, tenantId);
        String path = "/api/v1/sec/users/" + customerId;

        assertNotFoundUser(http.get(adminToken, null, path));
        assertNotFoundUser(http.put(adminToken, null, path,
            "{\"email\":\"" + email + "\",\"fullNameAr\":\"س\",\"fullNameEn\":\"Hijack\"}"));
        assertNotFoundUser(http.put(adminToken, null, path + "/roles", "{\"roleIds\":[" + sysAdminRole + "]}"));
        assertNotFoundUser(http.delete(adminToken, null, path));
        assertNotFoundUser(http.patch(adminToken, null, path, "{}"));

        // indistinguishable from an id that does not exist at all
        HttpResponse<String> unknown = http.get(adminToken, null, "/api/v1/sec/users/987654321");
        assertNotFoundUser(unknown);

        Map<String, Object> row = jdbcTemplate.queryForMap(
            "SELECT FULL_NAME_EN, STATUS_CODE, IS_ACTIVE_FL FROM SEC_USER WHERE USER_PK = ?", customerId);
        assertThat(row.get("FULL_NAME_EN")).isEqualTo("Customer");
        assertThat(row.get("STATUS_CODE")).isEqualTo("PENDING_VERIFICATION");
        assertThat(row.get("IS_ACTIVE_FL")).isEqualTo(Boolean.TRUE);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SEC_USER_ROLE WHERE USER_ID = ?",
            Integer.class, customerId)).as("the customer holds no role").isZero();

        // the deactivate + reactivate pair can no longer bypass the e-mail verification
        HttpResponse<String> login = http.customerLogin(tenantCode, email, CUSTOMER_PASSWORD);
        assertThat(login.statusCode()).as(login.body()).isEqualTo(403);
        assertThat(RealmHttp.errorCode(login)).isEqualTo("CUSTOMER_NOT_VERIFIED");
    }

    @Test
    void theStaffByIdEndpoints_stillWorkOnStaffAccounts() {
        HttpResponse<String> created = http.post(adminToken, null, "/api/v1/sec/users", "{\"username\":\"clerk-"
            + RealmHttp.unique("u").toLowerCase() + "\",\"email\":\"" + uniqueEmail("clerk")
            + "\",\"fullNameAr\":\"موظف\",\"fullNameEn\":\"Clerk\",\"password\":\"" + RealmHttp.PASSWORD + "\"}");
        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        long staffId = ((Number) JsonPath.read(created.body(), "$.data.userPk")).longValue();
        String path = "/api/v1/sec/users/" + staffId;

        assertThat(http.get(adminToken, null, path).statusCode()).isEqualTo(200);
        HttpResponse<String> disabled = http.delete(adminToken, null, path);
        assertThat(disabled.statusCode()).as(disabled.body()).isEqualTo(200);
        assertThat((String) JsonPath.read(disabled.body(), "$.data.statusCode")).isEqualTo("DISABLED");
        HttpResponse<String> reactivated = http.patch(adminToken, null, path, "{}");
        assertThat(reactivated.statusCode()).as(reactivated.body()).isEqualTo(200);
        assertThat((String) JsonPath.read(reactivated.body(), "$.data.statusCode")).isEqualTo("ACTIVE");
    }

    @Test
    void theStaffUserSearch_neverListsACustomer_evenOneSharingAStaffEmail() {
        String email = uniqueEmail("both");
        long customerId = register(email);
        HttpResponse<String> staff = http.post(adminToken, null, "/api/v1/sec/users", "{\"username\":\"" + email
            + "\",\"email\":\"" + email + "\",\"fullNameAr\":\"موظف\",\"fullNameEn\":\"Staff\",\"password\":\""
            + RealmHttp.PASSWORD + "\"}");
        assertThat(staff.statusCode()).as(staff.body()).isEqualTo(201);
        long staffId = ((Number) JsonPath.read(staff.body(), "$.data.userPk")).longValue();

        HttpResponse<String> all = http.post(adminToken, null, "/api/v1/sec/users/search", "{\"size\":200}");
        assertThat(all.statusCode()).as(all.body()).isEqualTo(200);
        List<String> realms = JsonPath.read(all.body(), "$.data.content[*].realm");
        assertThat(realms).isNotEmpty().containsOnly("STAFF");
        List<Number> ids = JsonPath.read(all.body(), "$.data.content[*].userPk");
        assertThat(ids).extracting(Number::longValue).contains(staffId).doesNotContain(customerId);
        assertThat(((Number) JsonPath.read(all.body(), "$.data.totalElements")).longValue())
            .isEqualTo(staffCount());

        HttpResponse<String> byEmail = http.post(adminToken, null, "/api/v1/sec/users/search",
            "{\"filters\":[{\"field\":\"email\",\"operator\":\"EQUALS\",\"value\":\"" + email + "\"}]}");
        assertThat(byEmail.statusCode()).as(byEmail.body()).isEqualTo(200);
        List<Number> matched = JsonPath.read(byEmail.body(), "$.data.content[*].userPk");
        assertThat(matched).extracting(Number::longValue).containsExactly(staffId);
    }

    @Test
    void aCustomersSession_isNeitherListedNorTerminableByStaff() {
        String email = uniqueEmail("buyer");
        long customerId = register(email);
        jdbcTemplate.update("UPDATE SEC_USER SET STATUS_CODE = 'ACTIVE' WHERE USER_PK = ? AND REALM = 'CUSTOMER'",
            customerId);
        HttpResponse<String> login = http.customerLogin(tenantCode, email, CUSTOMER_PASSWORD);
        assertThat(login.statusCode()).as(login.body()).isEqualTo(200);
        String customerToken = JsonPath.read(login.body(), "$.data.accessToken");
        long customerSession = jdbcTemplate.queryForObject("SELECT ACTIVE_SESSION_PK FROM SEC_ACTIVE_SESSION"
            + " WHERE USER_ID = ? AND TERMINATED_AT IS NULL", Long.class, customerId);

        HttpResponse<String> listed = http.post(adminToken, null, "/api/v1/sec/sessions/search", "{\"size\":200}");
        assertThat(listed.statusCode()).as(listed.body()).isEqualTo(200);
        List<Number> sessionIds = JsonPath.read(listed.body(), "$.data.content[*].activeSessionPk");
        List<Number> owners = JsonPath.read(listed.body(), "$.data.content[*].userId");
        assertThat(sessionIds).isNotEmpty().extracting(Number::longValue).doesNotContain(customerSession);
        assertThat(owners).extracting(Number::longValue).doesNotContain(customerId);

        HttpResponse<String> byOwner = http.post(adminToken, null, "/api/v1/sec/sessions/search",
            "{\"filters\":[{\"field\":\"userId\",\"operator\":\"EQUALS\",\"value\":\"" + customerId + "\"}]}");
        assertThat(byOwner.statusCode()).as(byOwner.body()).isEqualTo(200);
        assertThat((List<?>) JsonPath.read(byOwner.body(), "$.data.content")).isEmpty();

        HttpResponse<String> terminate = http.delete(adminToken, null, "/api/v1/sec/sessions/" + customerSession);
        assertThat(terminate.statusCode()).as(terminate.body()).isEqualTo(404);
        assertThat(RealmHttp.errorCode(terminate)).isEqualTo("SEC-404-SESSION");
        assertThat(jdbcTemplate.queryForObject("SELECT TERMINATED_AT IS NULL FROM SEC_ACTIVE_SESSION"
            + " WHERE ACTIVE_SESSION_PK = ?", Boolean.class, customerSession)).isTrue();
        // the customer's token still works on the customer chain
        assertThat(http.get(customerToken, null, "/api/v1/customers/me").statusCode()).isEqualTo(200);
    }

    @Test
    void theDashboardCounts_staffAccountsAndSessionsOnly() {
        long before = dashboardTotal();
        assertThat(before).isEqualTo(staffCount());

        String pending = uniqueEmail("p");
        register(pending);
        String active = uniqueEmail("a");
        long activeId = register(active);
        jdbcTemplate.update("UPDATE SEC_USER SET STATUS_CODE = 'ACTIVE' WHERE USER_PK = ?", activeId);
        assertThat(http.customerLogin(tenantCode, active, CUSTOMER_PASSWORD).statusCode()).isEqualTo(200);

        HttpResponse<String> dashboard = http.get(adminToken, null, "/api/v1/sec/dashboard");
        assertThat(dashboard.statusCode()).as(dashboard.body()).isEqualTo(200);
        assertThat(((Number) JsonPath.read(dashboard.body(), "$.data.usersOverview.total")).longValue())
            .as("two customers added, the staff total is unchanged").isEqualTo(before).isEqualTo(staffCount());
        assertThat(((Number) JsonPath.read(dashboard.body(), "$.data.usersOverview.active")).longValue())
            .isEqualTo(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SEC_USER WHERE TENANT_ID = ?"
                + " AND REALM = 'STAFF' AND STATUS_CODE = 'ACTIVE'", Long.class, tenantId));
        assertThat(((Number) JsonPath.read(dashboard.body(), "$.data.activeSessions.count")).longValue())
            .isEqualTo(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SEC_ACTIVE_SESSION s JOIN SEC_USER u"
                + " ON u.USER_PK = s.USER_ID WHERE s.TENANT_ID = ? AND s.TERMINATED_AT IS NULL AND u.REALM = 'STAFF'",
                Long.class, tenantId))
            .isLessThan(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SEC_ACTIVE_SESSION WHERE TENANT_ID = ?"
                + " AND TERMINATED_AT IS NULL", Long.class, tenantId));
    }

    // ---------------------------------------------------------------------------------------------

    private long register(String email) {
        HttpResponse<String> registered = http.register(tenantCode, email, CUSTOMER_PASSWORD, "Customer");
        assertThat(registered.statusCode()).as(registered.body()).isEqualTo(201);
        return ((Number) JsonPath.read(registered.body(), "$.data.id")).longValue();
    }

    private long dashboardTotal() {
        HttpResponse<String> dashboard = http.get(adminToken, null, "/api/v1/sec/dashboard");
        assertThat(dashboard.statusCode()).as(dashboard.body()).isEqualTo(200);
        return ((Number) JsonPath.read(dashboard.body(), "$.data.usersOverview.total")).longValue();
    }

    private long staffCount() {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM SEC_USER WHERE TENANT_ID = ? AND REALM = 'STAFF'",
            Long.class, tenantId);
    }

    private static String uniqueEmail(String prefix) {
        return prefix + "." + RealmHttp.unique("x").toLowerCase() + "@shop.test";
    }

    private static void assertNotFoundUser(HttpResponse<String> response) {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(404);
        assertThat(RealmHttp.errorCode(response)).isEqualTo("SEC-404-USER");
    }
}
