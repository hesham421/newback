package com.erp.tenant;

import com.jayway.jsonpath.JsonPath;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Test-only HTTP client for the tenant tests: talks to the running test server exactly like a
 * client would (bearer token, {@code X-Tenant-Code}), so the whole core security chain — the JWT
 * filter, the tenant resolution filter, authorization — is exercised.
 *
 * <p>Fixtures are written over HTTP (the platform API, the SEC user API) or, for the one PLATFORM
 * operator account, with plain JDBC: the tenant package must not import SEC internals
 * ({@code CrossModuleBoundaryArchTest} analyses test classes too).
 */
final class TenantHttp {

    /** Password of every account these tests create. */
    static final String PASSWORD = "Tenant-Test-Passw0rd!";

    private static final AtomicReference<String> PLATFORM_OPERATOR = new AtomicReference<>();

    private final HttpClient http = HttpClient.newHttpClient();
    private final String baseUrl;

    TenantHttp(int port) {
        this.baseUrl = "http://localhost:" + port;
    }

    static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    /**
     * A PLATFORM-tenant account holding the PLATFORM tenant's SYS_ADMIN (and so
     * {@code PLATFORM_TENANT_MANAGE}). Created once per JVM and committed; the seeded {@code admin}
     * stays untouched (other tests assert it is still PENDING).
     */
    static String platformOperator(JdbcTemplate jdbc, PasswordEncoder encoder) {
        return PLATFORM_OPERATOR.updateAndGet(existing -> existing != null ? existing : createOperator(jdbc, encoder));
    }

    /** tenant-maturity C4 — one more PLATFORM operator (SYS_ADMIN), distinct from {@link #platformOperator}. */
    static String anotherPlatformOperator(JdbcTemplate jdbc, PasswordEncoder encoder) {
        return createOperator(jdbc, encoder);
    }

    /**
     * tenant-maturity C5 review round 1 — a PLATFORM account whose only authorities are the FILE browser's view and delete
     * (a role of its own with those two action grants), so it does not hold {@code PLATFORM_TENANT_MANAGE}.
     */
    static String fileViewerOperator(JdbcTemplate jdbc, PasswordEncoder encoder) {
        String username = "file-viewer-" + UUID.randomUUID().toString().substring(0, 8);
        String role = "FV_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        jdbc.update("INSERT INTO SEC_ROLE (ROLE_PK, TENANT_ID, CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)"
            + " VALUES (nextval('SEQ_SEC_ROLE'), 1, ?, 'عارض الملفات', 'File viewer', TRUE, 'test', now())", role);
        jdbc.update("INSERT INTO SEC_ROLE_ACTION_GRANT (ROLE_ACTION_GRANT_PK, TENANT_ID, ROLE_ID, ACTION_ID, GRANTED_BY, GRANTED_AT)"
            + " SELECT nextval('SEQ_SEC_ROLE_ACTION_GRANT'), 1, r.ROLE_PK, a.ACTION_REG_PK, 'test', now()"
            + " FROM SEC_ROLE r JOIN SEC_ACTION_REG a ON a.PERMISSION_CODE IN ('PERM_FILE_BROWSER_VIEW', 'PERM_FILE_BROWSER_DELETE')"
            + " WHERE r.TENANT_ID = 1 AND r.CODE = ?", role);
        jdbc.update("INSERT INTO SEC_USER (USER_PK, TENANT_ID, USERNAME, EMAIL, PASSWORD_HASH, FULL_NAME_AR,"
                + " FULL_NAME_EN, STATUS_CODE, REALM, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)"
                + " VALUES (nextval('SEQ_SEC_USER'), 1, ?, ?, ?, 'عارض', 'File viewer', 'ACTIVE', 'STAFF', TRUE, 'test', now())",
            username, username + "@platform.test", encoder.encode(PASSWORD));
        jdbc.update("INSERT INTO SEC_USER_ROLE (USER_ROLE_PK, TENANT_ID, USER_ID, ROLE_ID, ASSIGNED_BY, ASSIGNED_AT)"
                + " SELECT nextval('SEQ_SEC_USER_ROLE'), 1, u.USER_PK, r.ROLE_PK, 'test', now()"
                + " FROM SEC_USER u JOIN SEC_ROLE r ON r.TENANT_ID = 1 AND r.CODE = ?"
                + " WHERE u.TENANT_ID = 1 AND u.USERNAME = ?",
            role, username);
        return username;
    }

    private static String createOperator(JdbcTemplate jdbc, PasswordEncoder encoder) {
        String username = "platform-op-" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO SEC_USER (USER_PK, TENANT_ID, USERNAME, EMAIL, PASSWORD_HASH, FULL_NAME_AR,"
                + " FULL_NAME_EN, STATUS_CODE, REALM, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)"
                + " VALUES (nextval('SEQ_SEC_USER'), 1, ?, ?, ?, 'مشغل المنصة', 'Platform operator', 'ACTIVE', 'STAFF', TRUE,"
                + " 'test', now())",
            username, username + "@platform.test", encoder.encode(PASSWORD));
        jdbc.update("INSERT INTO SEC_USER_ROLE (USER_ROLE_PK, TENANT_ID, USER_ID, ROLE_ID, ASSIGNED_BY, ASSIGNED_AT)"
                + " SELECT nextval('SEQ_SEC_USER_ROLE'), 1, u.USER_PK, r.ROLE_PK, 'test', now()"
                + " FROM SEC_USER u JOIN SEC_ROLE r ON r.TENANT_ID = 1 AND r.CODE = 'SYS_ADMIN'"
                + " WHERE u.TENANT_ID = 1 AND u.USERNAME = ?",
            username);
        return username;
    }

    HttpResponse<String> login(String tenantCode, String username, String password) {
        HttpRequest.Builder request = json("/api/v1/sec/auth/login")
            .POST(body("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}"));
        if (tenantCode != null) {
            request.header(TenantConstants.TENANT_CODE_HEADER, tenantCode);
        }
        return send(request);
    }

    String token(String tenantCode, String username) {
        HttpResponse<String> response = login(tenantCode, username, PASSWORD);
        if (response.statusCode() != 200) {
            throw new AssertionError("login " + tenantCode + "/" + username + " -> " + response.statusCode()
                + " " + response.body());
        }
        return JsonPath.read(response.body(), "$.data.accessToken");
    }

    /** Provisions a tenant whose administrator is {@code admin} / {@link #PASSWORD}. */
    HttpResponse<String> createTenant(String token, String code) {
        return post(token, "/api/v1/platform/tenants", createTenantBody(code));
    }

    /** Creates a tenant and returns its id (fails the test on any other answer than 201). */
    long provisionTenant(String platformToken, String code) {
        HttpResponse<String> response = createTenant(platformToken, code);
        if (response.statusCode() != 201) {
            throw new AssertionError("create tenant " + code + " -> " + response.statusCode() + " " + response.body());
        }
        return ((Number) JsonPath.read(response.body(), "$.data.id")).longValue();
    }

    /** Creates a SEC user in the caller's tenant (POST /api/v1/sec/users) and returns its id. */
    long createUser(String token, String username) {
        HttpResponse<String> response = post(token, "/api/v1/sec/users", "{\"username\":\"" + username
            + "\",\"email\":\"" + username + "@users.test\",\"fullNameAr\":\"مستخدم\",\"fullNameEn\":\"User\","
            + "\"password\":\"" + PASSWORD + "\"}");
        if (response.statusCode() != 201) {
            throw new AssertionError("create user " + username + " -> " + response.statusCode() + " " + response.body());
        }
        return ((Number) JsonPath.read(response.body(), "$.data.userPk")).longValue();
    }

    HttpResponse<String> get(String token, String path) {
        return send(authorized(json(path), token).GET());
    }

    HttpResponse<String> post(String token, String path, String jsonBody) {
        return send(authorized(json(path), token).POST(body(jsonBody)));
    }

    /** POST with both a bearer token and an {@code X-Tenant-Code} header. */
    HttpResponse<String> post(String token, String tenantCode, String path, String jsonBody) {
        return send(authorized(json(path), token).header(TenantConstants.TENANT_CODE_HEADER, tenantCode)
            .POST(body(jsonBody)));
    }

    /** tenant-maturity C4 — POST with one extra request header ({@code Idempotency-Key}); {@code null} sends none. */
    HttpResponse<String> post(String token, String path, String jsonBody, String headerName, String headerValue) {
        HttpRequest.Builder request = authorized(json(path), token);
        if (headerValue != null) {
            request.header(headerName, headerValue);
        }
        return send(request.POST(body(jsonBody)));
    }

    /** The JSON body of {@link #createTenant}, for a request sent with headers of the caller's choice. */
    static String createTenantBody(String code) {
        return "{\"code\":\"" + code + "\",\"nameAr\":\"مستأجر " + code
            + "\",\"nameEn\":\"Tenant " + code + "\",\"adminUsername\":\"admin\",\"adminEmail\":\"admin@"
            + code.toLowerCase() + ".test\",\"adminPassword\":\"" + PASSWORD
            + "\",\"adminFullNameAr\":\"مدير\",\"adminFullNameEn\":\"Administrator\"}";
    }

    HttpResponse<String> patch(String token, String path, String jsonBody) {
        return send(authorized(json(path), token).method("PATCH", body(jsonBody)));
    }

    HttpResponse<String> put(String token, String path, String jsonBody) {
        return send(authorized(json(path), token).PUT(body(jsonBody)));
    }

    /** Multipart upload ({@code POST /api/v1/files}) of a tiny PNG owned by {@code SHOP/PRODUCT/ownerId}. */
    HttpResponse<String> uploadPng(String token, long ownerId, String fileName) {
        String boundary = "----erp" + UUID.randomUUID().toString().replace("-", "");
        StringBuilder fields = new StringBuilder();
        for (String[] field : new String[][] {{"ownerId", String.valueOf(ownerId)}, {"ownerType", "PRODUCT"},
            {"moduleCode", "SHOP"}}) {
            fields.append("--").append(boundary).append("\r\nContent-Disposition: form-data; name=\"").append(field[0])
                .append("\"\r\n\r\n").append(field[1]).append("\r\n");
        }
        fields.append("--").append(boundary).append("\r\nContent-Disposition: form-data; name=\"file\"; filename=\"")
            .append(fileName).append("\"\r\nContent-Type: application/octet-stream\r\n\r\n");
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        content.writeBytes(fields.toString().getBytes(StandardCharsets.UTF_8));
        content.writeBytes(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13, 'I', 'H', 'D', 'R'});
        content.writeBytes(fileName.getBytes(StandardCharsets.UTF_8));
        content.writeBytes(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return send(authorized(HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/files")), token)
            .header("Accept-Language", "en")
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(content.toByteArray())));
    }

    /** tenant-maturity E — a multipart {@code PUT} with one part {@code file} (the logo endpoint). */
    HttpResponse<String> putFile(String token, String path, String fileName, byte[] bytes) {
        String boundary = "----erp" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream content = new ByteArrayOutputStream();
        content.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + fileName
            + "\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        content.writeBytes(bytes);
        content.writeBytes(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return send(authorized(HttpRequest.newBuilder(URI.create(baseUrl + path)), token)
            .header("Accept-Language", "en")
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .PUT(HttpRequest.BodyPublishers.ofByteArray(content.toByteArray())));
    }

    HttpResponse<String> delete(String token, String path) {
        return send(authorized(json(path), token).DELETE());
    }

    /** An anonymous GET answered as bytes (a public file), without any header but the URL. */
    HttpResponse<byte[]> getBytes(String path) {
        try {
            return http.send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /** tenant-maturity C5 — a GET answered as bytes (a private download), with the caller's bearer token. */
    HttpResponse<byte[]> getBytes(String token, String path) {
        try {
            return http.send(authorized(HttpRequest.newBuilder(URI.create(baseUrl + path)), token)
                .header("Accept-Language", "en").GET().build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    /** The {@code tid} claim of an access token (payload decoded without verification — test only). */
    static long tenantIdOf(String token) {
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(payload, "$." + TenantConstants.TENANT_ID_CLAIM)).longValue();
    }

    /** tenant-maturity C12 — the {@code iat} claim (whole seconds) of an access token, decoded without verification. */
    static long issuedAtOf(String token) {
        String payload = new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(payload, "$.iat")).longValue();
    }

    /**
     * tenant-maturity C12 — waits until the clock is past the second {@code token} was issued in, so a cut-off written
     * now lies in a later second than the token's {@code iat} (RULE-TENANT-023 compares whole seconds).
     */
    static void awaitSecondAfterIssueOf(String token) {
        long issuedAt = issuedAtOf(token);
        while (Instant.now().getEpochSecond() <= issuedAt) {
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
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
