package com.erp.notif;

import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Test-only fixtures for the erp-core step 08 NOTIF tests: SEC accounts written with plain JDBC (the
 * notif test package must not import SEC internals — {@code CrossModuleBoundaryArchTest} analyses test
 * classes too) and a minimal HTTP client that logs in and calls the API like a real client.
 */
final class NotifTestFixtures {

    static final String PASSWORD = "Notif-Test-Passw0rd!";
    static final String PLATFORM = "PLATFORM";

    /**
     * Administrator username of the tenants these tests provision — deliberately not {@code admin}:
     * an existing SEC test counts the {@code admin} accounts across all tenants.
     */
    static final String TENANT_ADMIN = "ntf-admin";

    private final HttpClient http = HttpClient.newHttpClient();
    private final String baseUrl;
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;

    NotifTestFixtures(int port, JdbcTemplate jdbc, PasswordEncoder encoder) {
        this.baseUrl = "http://localhost:" + port;
        this.jdbc = jdbc;
        this.encoder = encoder;
    }

    static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toLowerCase();
    }

    /** An ACTIVE account without roles in {@code tenantId}; returns its {@code SEC_USER} id. */
    long activeUser(long tenantId, String username) {
        jdbc.update("INSERT INTO SEC_USER (USER_PK, TENANT_ID, USERNAME, EMAIL, PASSWORD_HASH, FULL_NAME_AR,"
                + " FULL_NAME_EN, STATUS_CODE, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)"
                + " VALUES (nextval('SEQ_SEC_USER'), ?, ?, ?, ?, 'مستخدم', 'Notif test user', 'ACTIVE', TRUE, 'test', now())",
            tenantId, username, username + "@notif.test", encoder.encode(PASSWORD));
        return jdbc.queryForObject("SELECT USER_PK FROM SEC_USER WHERE TENANT_ID = ? AND USERNAME = ?",
            Long.class, tenantId, username);
    }

    /** An ACTIVE PLATFORM account holding PLATFORM's SYS_ADMIN (and so PLATFORM_TENANT_MANAGE). */
    String platformOperator() {
        String username = unique("notif-op-");
        activeUser(1L, username);
        jdbc.update("INSERT INTO SEC_USER_ROLE (USER_ROLE_PK, TENANT_ID, USER_ID, ROLE_ID, ASSIGNED_BY, ASSIGNED_AT)"
                + " SELECT nextval('SEQ_SEC_USER_ROLE'), 1, u.USER_PK, r.ROLE_PK, 'test', now()"
                + " FROM SEC_USER u JOIN SEC_ROLE r ON r.TENANT_ID = 1 AND r.CODE = 'SYS_ADMIN'"
                + " WHERE u.TENANT_ID = 1 AND u.USERNAME = ?",
            username);
        return username;
    }

    String token(String tenantCode, String username) {
        HttpResponse<String> response = send(json("/api/v1/sec/auth/login")
            .header("X-Tenant-Code", tenantCode)
            .POST(body("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}")));
        if (response.statusCode() != 200) {
            throw new AssertionError("login " + tenantCode + "/" + username + " -> " + response.statusCode()
                + " " + response.body());
        }
        return JsonPath.read(response.body(), "$.data.accessToken");
    }

    /** Provisions a tenant (administrator {@link #TENANT_ADMIN} / {@link #PASSWORD}) and returns its id. */
    long provisionTenant(String platformToken, String code) {
        HttpResponse<String> response = post(platformToken, "/api/v1/platform/tenants", "{\"code\":\"" + code
            + "\",\"nameAr\":\"مستأجر\",\"nameEn\":\"Tenant " + code + "\",\"adminUsername\":\"" + TENANT_ADMIN + "\","
            + "\"adminEmail\":\"admin@" + code.toLowerCase() + ".test\",\"adminPassword\":\"" + PASSWORD
            + "\",\"adminFullNameAr\":\"مدير\",\"adminFullNameEn\":\"Administrator\"}");
        if (response.statusCode() != 201) {
            throw new AssertionError("create tenant " + code + " -> " + response.statusCode() + " " + response.body());
        }
        return ((Number) JsonPath.read(response.body(), "$.data.id")).longValue();
    }

    HttpResponse<String> get(String token, String path) {
        return send(authorized(json(path), token).GET());
    }

    HttpResponse<String> post(String token, String path, String jsonBody) {
        return send(authorized(json(path), token).POST(body(jsonBody)));
    }

    HttpResponse<String> patch(String token, String path) {
        return send(authorized(json(path), token).method("PATCH", HttpRequest.BodyPublishers.noBody()));
    }

    static String errorCode(HttpResponse<String> response) {
        return JsonPath.read(response.body(), "$.error.code");
    }

    private HttpRequest.Builder json(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .header("Accept-Language", "en");
    }

    private static HttpRequest.Builder authorized(HttpRequest.Builder request, String token) {
        return token == null ? request : request.header("Authorization", "Bearer " + token);
    }

    private static HttpRequest.BodyPublisher body(String json) {
        return HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8);
    }

    private HttpResponse<String> send(HttpRequest.Builder request) {
        try {
            return http.send(request.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
