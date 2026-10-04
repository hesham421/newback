package com.erp.testsupport;

import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Test-only HTTP client for staff-realm API tests of any module (erp-core step 09): it talks to the
 * running test server like a real client (bearer token, {@code X-Tenant-Code}), so the whole core
 * security chain and method security are exercised. Lives in {@code com.erp.testsupport} (not bounded by
 * {@code CrossModuleBoundaryArchTest}), so module tests can use it without importing SEC or tenant
 * internals: the one PLATFORM operator account is written with plain JDBC, tenants are provisioned
 * through the platform API.
 */
public final class StaffApiClient {

    /** Password of every account this client creates. */
    public static final String PASSWORD = "Staff-Api-Test-Passw0rd!";

    /**
     * Username of the first administrator of tenants this client provisions. Deliberately not
     * {@code admin}: some SEC tests look up the seeded PLATFORM {@code admin} by username across tenants.
     */
    public static final String TENANT_ADMIN = "tenant-admin";

    private static final AtomicReference<String> PLATFORM_OPERATOR = new AtomicReference<>();

    private final HttpClient http = HttpClient.newHttpClient();
    private final String baseUrl;

    public StaffApiClient(int port) {
        this.baseUrl = "http://localhost:" + port;
    }

    /** A random upper-case suffix for codes that must be unique across test runs on one database. */
    public static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    /**
     * A committed STAFF account of the PLATFORM tenant holding its {@code SYS_ADMIN} (a super role: every
     * catalog authority, PLATFORM-module ones included). Created once per JVM.
     */
    public static String platformOperator(JdbcTemplate jdbc, PasswordEncoder encoder) {
        return PLATFORM_OPERATOR.updateAndGet(existing -> existing != null ? existing : createOperator(jdbc, encoder));
    }

    private static String createOperator(JdbcTemplate jdbc, PasswordEncoder encoder) {
        String username = "staff-op-" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO SEC_USER (USER_PK, TENANT_ID, USERNAME, EMAIL, PASSWORD_HASH, FULL_NAME_AR,"
                + " FULL_NAME_EN, STATUS_CODE, REALM, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)"
                + " VALUES (nextval('SEQ_SEC_USER'), 1, ?, ?, ?, 'مشغل', 'Operator', 'ACTIVE', 'STAFF', TRUE, 'test', now())",
            username, username + "@platform.test", encoder.encode(PASSWORD));
        jdbc.update("INSERT INTO SEC_USER_ROLE (USER_ROLE_PK, TENANT_ID, USER_ID, ROLE_ID, ASSIGNED_BY, ASSIGNED_AT)"
                + " SELECT nextval('SEQ_SEC_USER_ROLE'), 1, u.USER_PK, r.ROLE_PK, 'test', now()"
                + " FROM SEC_USER u JOIN SEC_ROLE r ON r.TENANT_ID = 1 AND r.CODE = 'SYS_ADMIN'"
                + " WHERE u.TENANT_ID = 1 AND u.USERNAME = ?",
            username);
        return username;
    }

    /** Logs in ({@code POST /api/v1/sec/auth/login}) and returns the access token; fails the test otherwise. */
    public String token(String tenantCode, String username) {
        HttpResponse<String> response = send(json("/api/v1/sec/auth/login")
            .header("X-Tenant-Code", tenantCode)
            .POST(body("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}")));
        if (response.statusCode() != 200) {
            throw new AssertionError("login " + tenantCode + "/" + username + " -> " + response.statusCode()
                + " " + response.body());
        }
        return JsonPath.read(response.body(), "$.data.accessToken");
    }

    /**
     * Provisions tenant {@code code} through {@code POST /api/v1/platform/tenants} (its administrator is
     * {@link #TENANT_ADMIN} / {@link #PASSWORD}, a super role of that tenant) and returns the tenant id.
     */
    public long provisionTenant(String platformToken, String code) {
        HttpResponse<String> response = post(platformToken, "/api/v1/platform/tenants", "{\"code\":\"" + code
            + "\",\"nameAr\":\"مستأجر\",\"nameEn\":\"Tenant " + code + "\",\"adminUsername\":\"" + TENANT_ADMIN + "\",\"adminEmail\":\"admin@"
            + code.toLowerCase() + ".test\",\"adminPassword\":\"" + PASSWORD
            + "\",\"adminFullNameAr\":\"مدير\",\"adminFullNameEn\":\"Administrator\"}");
        if (response.statusCode() != 201) {
            throw new AssertionError("create tenant " + code + " -> " + response.statusCode() + " " + response.body());
        }
        return ((Number) JsonPath.read(response.body(), "$.data.id")).longValue();
    }

    public HttpResponse<String> get(String token, String path) {
        return send(authorized(json(path), token).GET());
    }

    public HttpResponse<String> post(String token, String path, String jsonBody) {
        return send(authorized(json(path), token).POST(body(jsonBody)));
    }

    public HttpResponse<String> put(String token, String path, String jsonBody) {
        return send(authorized(json(path), token).PUT(body(jsonBody)));
    }

    public HttpResponse<String> delete(String token, String path) {
        return send(authorized(json(path), token).DELETE());
    }

    /** {@code $.error.code} of an error envelope. */
    public static String errorCode(HttpResponse<String> response) {
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
