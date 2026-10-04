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
 * erp-core step 05 — cross-tenant access is impossible by construction. Two tenants A and B are
 * provisioned through the platform API, each gets an extra user, and everything below is plain HTTP
 * through the real security chain. (SEC has no {@code GET /api/v1/sec/users} list endpoint; the list
 * is {@code POST /api/v1/sec/users/search}.)
 */
class TenantIsolationIntegrationTest extends AbstractIntegrationTest {

    @Value("${local.server.port}")
    private int port;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private TenantHttp http;
    private String codeA;
    private String codeB;
    private long tenantA;
    private long tenantB;
    private String tokenA;
    private String tokenB;
    private long userOfA;
    private long userOfB;

    @BeforeEach
    void twoTenantsWithOneExtraUserEach() {
        http = new TenantHttp(port);
        String platformToken = http.token(TenantConstants.PLATFORM_TENANT_CODE,
            TenantHttp.platformOperator(jdbcTemplate, passwordEncoder));
        codeA = TenantHttp.unique("TA");
        codeB = TenantHttp.unique("TB");
        tenantA = http.provisionTenant(platformToken, codeA);
        tenantB = http.provisionTenant(platformToken, codeB);
        tokenA = http.token(codeA, "admin");
        tokenB = http.token(codeB, "admin");
        userOfA = http.createUser(tokenA, "alice");
        userOfB = http.createUser(tokenB, "bob");
    }

    @Test
    void userSearchAsA_neverReturnsBsRows() {
        HttpResponse<String> response = http.post(tokenA, "/api/v1/sec/users/search", "{\"size\":1000}");

        assertThat(response.statusCode()).isEqualTo(200);
        List<String> usernames = JsonPath.read(response.body(), "$.data.content[*].username");
        List<Number> ids = JsonPath.read(response.body(), "$.data.content[*].userPk");
        assertThat(usernames).containsExactlyInAnyOrder("admin", "alice");
        assertThat(ids.stream().map(Number::longValue)).contains(userOfA).doesNotContain(userOfB);
        assertThat(ids.stream().map(Number::longValue).toList())
            .allSatisfy(id -> assertThat(tenantOfUser(id)).isEqualTo(tenantA));

        // a filter that would match B's user finds nothing
        HttpResponse<String> filtered = http.post(tokenA, "/api/v1/sec/users/search",
            "{\"filters\":[{\"field\":\"username\",\"operator\":\"EQUALS\",\"value\":\"bob\"}]}");
        assertThat(filtered.statusCode()).isEqualTo(200);
        assertThat((List<?>) JsonPath.read(filtered.body(), "$.data.content")).isEmpty();
    }

    @Test
    void getUserByIdOfB_asA_is404_whileBSeesIt() {
        HttpResponse<String> asA = http.get(tokenA, "/api/v1/sec/users/" + userOfB);
        assertThat(asA.statusCode()).isEqualTo(404);

        HttpResponse<String> asB = http.get(tokenB, "/api/v1/sec/users/" + userOfB);
        assertThat(asB.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(asB.body(), "$.data.username")).isEqualTo("bob");
    }

    @Test
    void usernamesAreUniquePerTenant_notGlobally() {
        // both provisioned administrators are called "admin", like the PLATFORM bootstrap admin
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from sec_user where username = 'admin' and tenant_id in (1, ?, ?)",
            Integer.class, tenantA, tenantB)).isEqualTo(3);

        http.createUser(tokenB, "alice");   // same username as A's user: allowed in another tenant

        HttpResponse<String> duplicateInA = http.post(tokenA, "/api/v1/sec/users", "{\"username\":\"alice\","
            + "\"email\":\"alice2@users.test\",\"fullNameAr\":\"م\",\"fullNameEn\":\"U\",\"password\":\""
            + TenantHttp.PASSWORD + "\"}");
        assertThat(duplicateInA.statusCode()).isEqualTo(409);
    }

    @Test
    void login_withoutXTenantCode_is400TenantRequired_andAnUnknownCodeIs404() {
        HttpResponse<String> noHeader = http.login(null, "admin", TenantHttp.PASSWORD);
        assertThat(noHeader.statusCode()).isEqualTo(400);
        assertThat(TenantHttp.errorCode(noHeader)).isEqualTo("TENANT_REQUIRED");

        HttpResponse<String> unknown = http.login("NO_SUCH_TENANT", "admin", TenantHttp.PASSWORD);
        assertThat(unknown.statusCode()).isEqualTo(404);
        assertThat(TenantHttp.errorCode(unknown)).isEqualTo("TENANT_NOT_FOUND");
    }

    @Test
    void login_isScopedToTheHeaderTenant() {
        // alice exists in A only
        assertThat(http.login(codeA, "alice", TenantHttp.PASSWORD).statusCode()).isEqualTo(200);
        assertThat(http.login(codeB, "alice", TenantHttp.PASSWORD).statusCode()).isEqualTo(401);
        // the header is case-insensitive and trimmed
        assertThat(http.login(" " + codeA.toLowerCase() + " ", "alice", TenantHttp.PASSWORD).statusCode()).isEqualTo(200);
    }

    @Test
    void aTokenOfA_ignoresAnXTenantCodeHeaderOfB() {
        // the token's tid wins over the header: A's admin still sees A's users only
        HttpResponse<String> response = http.post(tokenA, codeB, "/api/v1/sec/users/search", "{\"size\":1000}");
        assertThat(response.statusCode()).isEqualTo(200);
        List<String> usernames = JsonPath.read(response.body(), "$.data.content[*].username");
        assertThat(usernames).containsExactlyInAnyOrder("admin", "alice");
        assertThat(TenantHttp.tenantIdOf(tokenA)).isEqualTo(tenantA);
        assertThat(TenantHttp.tenantIdOf(tokenB)).isEqualTo(tenantB);
    }

    private long tenantOfUser(long userPk) {
        return jdbcTemplate.queryForObject("select tenant_id from sec_user where user_pk = ?", Long.class, userPk);
    }
}
