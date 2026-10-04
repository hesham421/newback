package com.erp.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.tenant.TenantConstants;
import com.erp.testsupport.TestPostgres;
import com.jayway.jsonpath.JsonPath;
import com.erp.testsupport.TestcontainersPostgresConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The reference application boots from its own unmodified {@code application.yml} with only the
 * environment it documents ({@code DB_URL}, {@code DB_USER}, {@code DB_PASSWORD}, {@code JWT_SECRET},
 * {@code FILE_TOKEN_SECRET}) — no Redis, no SMTP — against a PostgreSQL 16 from erp-core's test-jar
 * ({@link TestPostgres}: Testcontainers when Docker is available, otherwise embedded). It also sets
 * the optional {@code ERP_BOOTSTRAP_ADMIN_PASSWORD}, which erp-core's bootstrap runner gives to the
 * seeded {@code admin} account (shipped without a usable password since erp-core step 04).
 *
 * <p>Since erp-core step 05 every login names its tenant with {@code X-Tenant-Code}: the bootstrap
 * admin belongs to the {@code PLATFORM} tenant. The admin provisions a second tenant through the
 * platform endpoint, and that tenant's first administrator logs in with its own code.
 */
@SpringBootTest(classes = ReferenceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@Import(TestcontainersPostgresConfiguration.class)
class ReferenceApplicationSmokeTest {

    @DynamicPropertySource
    static void environment(DynamicPropertyRegistry registry) {
        registry.add("DB_URL", TestPostgres::jdbcUrl);
        registry.add("DB_USER", TestPostgres::username);
        registry.add("DB_PASSWORD", TestPostgres::password);
        registry.add("JWT_SECRET", () -> "reference-smoke-test-jwt-secret-0123456789abcdef0123456789");
        registry.add("FILE_TOKEN_SECRET", () -> "reference-smoke-test-file-token-secret-0123456789abcdef");
        registry.add("ERP_BOOTSTRAP_ADMIN_PASSWORD", () -> BOOTSTRAP_ADMIN_PASSWORD);
    }

    private static final String BOOTSTRAP_ADMIN_PASSWORD = "Smoke-Admin-Passw0rd!";

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void flywayAppliedTheCoreChainAndThenTheApplicationMigration() {
        List<String> versions = jdbcTemplate.queryForList(
            "select version from flyway_schema_history where success order by installed_rank", String.class);
        // core V2..V11 (V1 is reserved and not shipped; V10 = tenant schema, V11 = auth realms), then the
        // application's own V1000
        assertThat(versions).containsExactly("2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "1000");
        assertThat(jdbcTemplate.queryForObject(
            "select count(*) from flyway_schema_history where not success", Integer.class)).isZero();
        assertThat(jdbcTemplate.queryForObject("select to_regclass('public.app_smoke')::text", String.class))
            .isEqualTo("app_smoke");
    }

    @Test
    void healthIsUp() throws Exception {
        HttpResponse<String> response = http.send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/actuator/health")).GET().build(),
            HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    @Test
    void bootstrapAdminLogsInWithTheConfiguredPassword_andReceivesAJwt() throws Exception {
        HttpResponse<String> response = login(BOOTSTRAP_ADMIN_PASSWORD);
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).containsPattern("\"accessToken\":\"[\\w-]+\\.[\\w-]+\\.[\\w-]+\"");
    }

    @Test
    void theOldWellKnownAdminAdminCredentialIsRejected() throws Exception {
        assertThat(login("admin").statusCode()).isEqualTo(401);
    }

    private HttpResponse<String> login(String password) throws Exception {
        return login(TenantConstants.PLATFORM_TENANT_CODE, "admin", password);
    }

    private HttpResponse<String> login(String tenantCode, String username, String password) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/sec/auth/login"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(
                "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
        if (tenantCode != null) {
            request.header(TenantConstants.TENANT_CODE_HEADER, tenantCode);
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void loginWithoutXTenantCode_isRejectedWith400TenantRequired() throws Exception {
        HttpResponse<String> response = login(null, "admin", BOOTSTRAP_ADMIN_PASSWORD);
        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("\"code\":\"TENANT_REQUIRED\"");
    }

    @Test
    void bootstrapAdminProvisionsASecondTenant_whoseAdministratorLogsInWithXTenantCode() throws Exception {
        String platformToken = JsonPath.read(login(BOOTSTRAP_ADMIN_PASSWORD).body(), "$.data.accessToken");

        HttpResponse<String> created = http.send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/platform/tenants"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + platformToken)
                .POST(HttpRequest.BodyPublishers.ofString("{\"code\":\"SMOKE_TENANT\",\"nameAr\":\"مستأجر\","
                    + "\"nameEn\":\"Smoke tenant\",\"adminUsername\":\"admin\",\"adminEmail\":\"admin@smoke.test\","
                    + "\"adminPassword\":\"Smoke-Tenant-Passw0rd!\",\"adminFullNameAr\":\"مدير\","
                    + "\"adminFullNameEn\":\"Smoke admin\"}", java.nio.charset.StandardCharsets.UTF_8))
                .build(),
            HttpResponse.BodyHandlers.ofString());
        assertThat(created.statusCode()).as(created.body()).isEqualTo(201);
        long tenantId = ((Number) JsonPath.read(created.body(), "$.data.id")).longValue();
        assertThat(tenantId).isGreaterThan(TenantConstants.PLATFORM_TENANT_ID);

        HttpResponse<String> tenantLogin = login("SMOKE_TENANT", "admin", "Smoke-Tenant-Passw0rd!");
        assertThat(tenantLogin.statusCode()).isEqualTo(200);
        assertThat(tenantLogin.body()).containsPattern("\"accessToken\":\"[\\w-]+\\.[\\w-]+\\.[\\w-]+\"");
        // the same "admin" username in the PLATFORM tenant keeps its own password
        assertThat(login(TenantConstants.PLATFORM_TENANT_CODE, "admin", "Smoke-Tenant-Passw0rd!").statusCode())
            .isEqualTo(401);
    }

    @Test
    void protectedEndpointsNeedAToken() throws Exception {
        HttpResponse<String> response = http.send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/sec/users")).GET().build(),
            HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(401);
    }
}
