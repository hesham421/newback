package com.erp.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.testsupport.TestPostgres;
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
 * ({@link TestPostgres}: Testcontainers when Docker is available, otherwise embedded).
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
    }

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final HttpClient http = HttpClient.newHttpClient();

    @Test
    void flywayAppliedTheCoreChainAndTheApplicationMigration() {
        List<String> versions = jdbcTemplate.queryForList(
            "select version from flyway_schema_history where success order by installed_rank", String.class);
        assertThat(versions).contains("1", "20", "1000");
        assertThat(versions.get(versions.size() - 1)).isEqualTo("1000");
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
    void bootstrapAdminLogsIn_andReceivesAJwt() throws Exception {
        HttpResponse<String> response = http.send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/sec/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"admin\",\"password\":\"admin\"}"))
                .build(),
            HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).containsPattern("\"accessToken\":\"[\\w-]+\\.[\\w-]+\\.[\\w-]+\"");
    }

    @Test
    void protectedEndpointsNeedAToken() throws Exception {
        HttpResponse<String> response = http.send(
            HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/v1/sec/users")).GET().build(),
            HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(401);
    }
}
