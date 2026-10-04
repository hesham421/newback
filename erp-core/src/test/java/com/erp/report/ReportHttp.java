package com.erp.report;

import com.erp.tenant.TenantConstants;
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
 * Test-only HTTP client for the reporting tests (erp-core step 11): talks to the running test server
 * like a client (bearer token, {@code X-Tenant-Code}, {@code Accept-Language}), through the real security
 * chains. Accounts are created with JDBC (tests in this package must not import SEC internals); tenants
 * are provisioned through the platform API.
 */
final class ReportHttp {

    static final String PASSWORD = "Report-Test-Passw0rd!";

    /** Username of a provisioned tenant's first administrator. */
    static final String TENANT_ADMIN = "report-admin";

    private final HttpClient http = HttpClient.newHttpClient();
    private final String baseUrl;

    ReportHttp(int port) {
        this.baseUrl = "http://localhost:" + port;
    }

    static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    /** A committed ACTIVE account of {@code tenantId} in {@code realm}; returns its username (= e-mail for customers). */
    static String createUser(JdbcTemplate jdbc, PasswordEncoder encoder, long tenantId, String realm, String username,
                             String email) {
        jdbc.update("INSERT INTO SEC_USER (USER_PK, TENANT_ID, USERNAME, EMAIL, PASSWORD_HASH, FULL_NAME_AR,"
                + " FULL_NAME_EN, STATUS_CODE, REALM, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)"
                + " VALUES (nextval('SEQ_SEC_USER'), ?, ?, ?, ?, 'مستخدم، تقرير', 'Report, \"User\"', 'ACTIVE', ?, TRUE,"
                + " 'test', now())",
            tenantId, username, email, encoder.encode(PASSWORD), realm);
        return username;
    }

    /** Gives the STAFF user {@code username} of {@code tenantId} the tenant's role {@code roleCode}. */
    static void assignRole(JdbcTemplate jdbc, long tenantId, String username, String roleCode) {
        jdbc.update("INSERT INTO SEC_USER_ROLE (USER_ROLE_PK, TENANT_ID, USER_ID, ROLE_ID, ASSIGNED_BY, ASSIGNED_AT)"
                + " SELECT nextval('SEQ_SEC_USER_ROLE'), ?, u.USER_PK, r.ROLE_PK, 'test', now()"
                + " FROM SEC_USER u JOIN SEC_ROLE r ON r.TENANT_ID = u.TENANT_ID AND r.CODE = ?"
                + " WHERE u.TENANT_ID = ? AND u.REALM = 'STAFF' AND u.USERNAME = ?",
            tenantId, roleCode, tenantId, username);
    }

    /** A committed PLATFORM-tenant STAFF account holding PLATFORM's SYS_ADMIN (super role); returns its username. */
    static String platformOperator(JdbcTemplate jdbc, PasswordEncoder encoder) {
        String username = "report-op-" + UUID.randomUUID().toString().substring(0, 8);
        createUser(jdbc, encoder, TenantConstants.PLATFORM_TENANT_ID, "STAFF", username, username + "@platform.test");
        assignRole(jdbc, TenantConstants.PLATFORM_TENANT_ID, username, "SYS_ADMIN");
        return username;
    }

    /**
     * Provisions a tenant whose STAFF administrator is {@link #TENANT_ADMIN} / {@link #PASSWORD}; returns its id.
     * Not {@code admin}: {@code BootstrapAdminPasswordIntegrationTest} counts {@code admin} rows across all tenants.
     */
    long provisionTenant(String platformToken, String code) {
        HttpResponse<String> response = post(platformToken, null, "/api/v1/platform/tenants", "{\"code\":\"" + code
            + "\",\"nameAr\":\"مستأجر\",\"nameEn\":\"Tenant " + code + "\",\"adminUsername\":\"" + TENANT_ADMIN + "\",\"adminEmail\":\"admin@"
            + code.toLowerCase() + ".test\",\"adminPassword\":\"" + PASSWORD
            + "\",\"adminFullNameAr\":\"مدير\",\"adminFullNameEn\":\"Administrator\"}", "en");
        if (response.statusCode() != 201) {
            throw new AssertionError("create tenant " + code + " -> " + response.statusCode() + " " + response.body());
        }
        return ((Number) JsonPath.read(response.body(), "$.data.id")).longValue();
    }

    /** Staff login; returns the access token. */
    String staffToken(String tenantCode, String username) {
        HttpResponse<String> response = post(null, tenantCode, "/api/v1/sec/auth/login",
            "{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}", "en");
        if (response.statusCode() != 200) {
            throw new AssertionError("staff login " + tenantCode + "/" + username + " -> " + response.statusCode()
                + " " + response.body());
        }
        return JsonPath.read(response.body(), "$.data.accessToken");
    }

    /** Customer login; returns the access token. */
    String customerToken(String tenantCode, String email) {
        HttpResponse<String> response = post(null, tenantCode, "/api/v1/public/customers/login",
            "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}", "en");
        if (response.statusCode() != 200) {
            throw new AssertionError("customer login " + tenantCode + "/" + email + " -> " + response.statusCode()
                + " " + response.body());
        }
        return JsonPath.read(response.body(), "$.data.accessToken");
    }

    HttpResponse<String> get(String token, String path) {
        return send(request(path, token, null, "en").GET(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    HttpResponse<String> post(String token, String tenantCode, String path, String jsonBody, String language) {
        return send(request(path, token, tenantCode, language).POST(body(jsonBody)),
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
    }

    HttpResponse<String> post(String token, String path, String jsonBody) {
        return post(token, null, path, jsonBody, "en");
    }

    /** POST answering the raw bytes (exports). */
    HttpResponse<byte[]> postBytes(String token, String path, String jsonBody, String language) {
        return send(request(path, token, null, language).POST(body(jsonBody)), HttpResponse.BodyHandlers.ofByteArray());
    }

    static String errorCode(HttpResponse<String> response) {
        return JsonPath.read(response.body(), "$.error.code");
    }

    private HttpRequest.Builder request(String path, String token, String tenantCode, String language) {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .header("Accept-Language", language);
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (tenantCode != null) {
            request.header(TenantConstants.TENANT_CODE_HEADER, tenantCode);
        }
        return request;
    }

    private static HttpRequest.BodyPublisher body(String json) {
        return HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8);
    }

    private <T> HttpResponse<T> send(HttpRequest.Builder request, HttpResponse.BodyHandler<T> handler) {
        try {
            return http.send(request.build(), handler);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
