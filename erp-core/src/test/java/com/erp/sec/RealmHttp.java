package com.erp.sec;

import com.erp.tenant.TenantConstants;
import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Test-only HTTP client for the realm tests (erp-core step 06): talks to the running test server
 * exactly like a client (bearer token, {@code X-Tenant-Code}), so both security chains — JWT filter,
 * tenant resolution, realm enforcement, authorization — are exercised. Fresh tenants are provisioned
 * through the platform API by a PLATFORM operator account created with JDBC.
 */
final class RealmHttp {

    static final String PASSWORD = "Realm-Test-Passw0rd!";

    private final HttpClient http = HttpClient.newHttpClient();
    private final String baseUrl;

    RealmHttp(int port) {
        this.baseUrl = "http://localhost:" + port;
    }

    static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    /** A committed PLATFORM-tenant STAFF account holding PLATFORM's SYS_ADMIN; returns its username. */
    static String platformOperator(JdbcTemplate jdbc, PasswordEncoder encoder) {
        String username = "realm-op-" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO SEC_USER (USER_PK, TENANT_ID, USERNAME, EMAIL, PASSWORD_HASH, FULL_NAME_AR,"
                + " FULL_NAME_EN, STATUS_CODE, REALM, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)"
                + " VALUES (nextval('SEQ_SEC_USER'), 1, ?, ?, ?, 'مشغل', 'Operator', 'ACTIVE', 'STAFF', TRUE, 'test', now())",
            username, username + "@platform.test", encoder.encode(PASSWORD));
        jdbc.update("INSERT INTO SEC_USER_ROLE (USER_ROLE_PK, TENANT_ID, USER_ID, ROLE_ID, ASSIGNED_BY, ASSIGNED_AT)"
                + " SELECT nextval('SEQ_SEC_USER_ROLE'), 1, u.USER_PK, r.ROLE_PK, 'test', now()"
                + " FROM SEC_USER u JOIN SEC_ROLE r ON r.TENANT_ID = 1 AND r.CODE = 'SYS_ADMIN'"
                + " WHERE u.TENANT_ID = 1 AND u.REALM = 'STAFF' AND u.USERNAME = ?",
            username);
        return username;
    }

    /** Provisions a tenant whose STAFF administrator is {@code admin} / {@link #PASSWORD}; returns its id. */
    long provisionTenant(String platformToken, String code) {
        HttpResponse<String> response = post(platformToken, null, "/api/v1/platform/tenants", "{\"code\":\"" + code
            + "\",\"nameAr\":\"مستأجر\",\"nameEn\":\"Tenant " + code + "\",\"adminUsername\":\"admin\",\"adminEmail\":\"admin@"
            + code.toLowerCase() + ".test\",\"adminPassword\":\"" + PASSWORD
            + "\",\"adminFullNameAr\":\"مدير\",\"adminFullNameEn\":\"Administrator\"}");
        if (response.statusCode() != 201) {
            throw new AssertionError("create tenant " + code + " -> " + response.statusCode() + " " + response.body());
        }
        return ((Number) JsonPath.read(response.body(), "$.data.id")).longValue();
    }

    /** Staff login (POST /api/v1/sec/auth/login); returns the access token. */
    String staffToken(String tenantCode, String username, String password) {
        HttpResponse<String> response = post(null, tenantCode, "/api/v1/sec/auth/login",
            "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
        if (response.statusCode() != 200) {
            throw new AssertionError("staff login " + tenantCode + "/" + username + " -> " + response.statusCode()
                + " " + response.body());
        }
        return JsonPath.read(response.body(), "$.data.accessToken");
    }

    HttpResponse<String> register(String tenantCode, String email, String password, String fullName) {
        return post(null, tenantCode, "/api/v1/public/customers/register",
            "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"fullName\":\"" + fullName + "\"}");
    }

    HttpResponse<String> verify(String tenantCode, String token) {
        return post(null, tenantCode, "/api/v1/public/customers/verify", "{\"token\":\"" + token + "\"}");
    }

    HttpResponse<String> customerLogin(String tenantCode, String email, String password) {
        return post(null, tenantCode, "/api/v1/public/customers/login",
            "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
    }

    HttpResponse<String> get(String token, String tenantCode, String path) {
        return send(headers(json(path), token, tenantCode).GET());
    }

    HttpResponse<String> post(String token, String tenantCode, String path, String jsonBody) {
        return send(headers(json(path), token, tenantCode).POST(body(jsonBody)));
    }

    HttpResponse<String> patch(String token, String tenantCode, String path, String jsonBody) {
        return send(headers(json(path), token, tenantCode).method("PATCH", body(jsonBody)));
    }

    HttpResponse<String> put(String token, String tenantCode, String path, String jsonBody) {
        return send(headers(json(path), token, tenantCode).PUT(body(jsonBody)));
    }

    HttpResponse<String> delete(String token, String tenantCode, String path) {
        return send(headers(json(path), token, tenantCode).DELETE());
    }

    /** A claim of an access token (payload decoded without verification — test only). */
    static Object claim(String token, String name) {
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
        return JsonPath.read(payload, "$." + name);
    }

    static String errorCode(HttpResponse<String> response) {
        return JsonPath.read(response.body(), "$.error.code");
    }

    private HttpRequest.Builder json(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .header("Accept-Language", "en");
    }

    private static HttpRequest.Builder headers(HttpRequest.Builder request, String token, String tenantCode) {
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
