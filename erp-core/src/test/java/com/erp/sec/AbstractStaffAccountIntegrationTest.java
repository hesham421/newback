package com.erp.sec;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.testsupport.AbstractIntegrationTest;
import com.erp.testsupport.StaffApiClient;
import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * erp-core 1.3.0 (TM-D) — shared fixture of the staff password / profile / photo tests: one fresh tenant
 * per test class (provisioned over the platform API, so the real chain runs), its first administrator
 * ({@link StaffApiClient#TENANT_ADMIN}, a super role) and helpers to create and sign in staff users over HTTP.
 */
abstract class AbstractStaffAccountIntegrationTest extends AbstractIntegrationTest {

    protected static final String PASSWORD = StaffApiClient.PASSWORD;

    private static final Map<Class<?>, String> TENANTS = new HashMap<>();

    @LocalServerPort
    private int port;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    protected PasswordEncoder passwordEncoder;

    protected StaffApiClient api;
    protected String tenant;
    protected String adminToken;

    @BeforeEach
    void provisionTenantOnce() {
        api = new StaffApiClient(port);
        tenant = TENANTS.computeIfAbsent(getClass(), type -> {
            String code = StaffApiClient.unique("TMD");
            String platformToken = api.token("PLATFORM", StaffApiClient.platformOperator(jdbcTemplate, passwordEncoder));
            api.provisionTenant(platformToken, code);
            return code;
        });
        adminToken = api.token(tenant, StaffApiClient.TENANT_ADMIN);
    }

    /** Creates a staff user in the class's tenant; {@code requireChange} null leaves the field out (default TRUE). */
    protected long createUser(String username, Boolean requireChange) {
        HttpResponse<String> response = api.post(adminToken, "/api/v1/sec/users", userBody(username, PASSWORD, requireChange));
        assertThat(response.statusCode()).as(response.body()).isEqualTo(201);
        return ((Number) JsonPath.read(response.body(), "$.data.userPk")).longValue();
    }

    protected static String userBody(String username, String password, Boolean requireChange) {
        return "{\"username\":\"" + username + "\",\"email\":\"" + username + "@tmd.test\",\"fullNameAr\":\"مستخدم\","
            + "\"fullNameEn\":\"User " + username + "\",\"password\":\"" + password + "\""
            + (requireChange == null ? "" : ",\"requireChangeAtNextLogin\":" + requireChange) + "}";
    }

    /** Signs in with {@code password} and answers the access token (fails the test otherwise). */
    protected String login(String username, String password) {
        HttpResponse<String> response = api.login(tenant, username, password);
        assertThat(response.statusCode()).as(response.body()).isEqualTo(200);
        return JsonPath.read(response.body(), "$.data.accessToken");
    }

    protected static String unique(String prefix) {
        return StaffApiClient.unique(prefix).toLowerCase();
    }

    protected static String errorCode(HttpResponse<String> response) {
        return StaffApiClient.errorCode(response);
    }

    protected static Object data(HttpResponse<String> response, String path) {
        return JsonPath.read(response.body(), "$.data." + path);
    }

    /** {@code CORE_AUDIT_EVENT} rows of {@code action} about SEC user {@code userId} in the class's tenant. */
    protected int auditRows(String action, long userId) {
        Integer rows = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM CORE_AUDIT_EVENT e JOIN CORE_TENANT t"
                + " ON t.ID = e.TENANT_ID WHERE t.CODE = ? AND e.ACTION = ? AND e.ENTITY_TYPE = 'SEC_USER' AND e.ENTITY_ID = ?",
            Integer.class, tenant, action, String.valueOf(userId));
        return rows == null ? 0 : rows;
    }
}
