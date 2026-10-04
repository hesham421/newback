package com.erp.audit;

import com.erp.tenant.TenantConstants;
import com.jayway.jsonpath.JsonPath;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Test-only HTTP client for the audit tests (erp-core step 10): talks to the running test server like
 * a client (bearer token, {@code X-Tenant-Code}), so the whole chain — JWT filter, tenant filter, realm
 * enforcement, authorization, the audited services — is exercised. The audit test package may not
 * import other modules' internals (ArchUnit analyses test classes), so fixtures are written over HTTP;
 * only the PLATFORM operator account and the customer activation use JDBC.
 */
final class AuditHttp {

    static final String PASSWORD = "Audit-Test-Passw0rd!";
    static final String PLATFORM = TenantConstants.PLATFORM_TENANT_CODE;
    static final String USER_AGENT = "erp-audit-test/1.0";

    /**
     * Administrator of the tenants these tests provision — deliberately not {@code admin}: some SEC
     * tests count rows of the user {@code admin} across all tenants.
     */
    static final String TENANT_ADMIN = "audit-admin";

    /**
     * Field-name words that must never appear in {@code CHANGES} — the step-10 denylist (password,
     * secret, token, hash) extended with the hashed/secret field names that exist in the core model.
     */
    static final List<String> SENSITIVE_WORDS = List.of("password", "passwordhash", "token", "tokenhash", "secret",
        "hash", "contenthash", "credential", "apikey", "privatekey", "salt", "configjson");

    private static final AtomicReference<String> PLATFORM_OPERATOR = new AtomicReference<>();

    private final HttpClient http = HttpClient.newHttpClient();
    private final String baseUrl;

    AuditHttp(int port) {
        this.baseUrl = "http://localhost:" + port;
    }

    static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
    }

    /** A PLATFORM account holding PLATFORM's SYS_ADMIN (a super role: every permission, AUDIT:EVENT:READ included). */
    static String platformOperator(JdbcTemplate jdbc, PasswordEncoder encoder) {
        return PLATFORM_OPERATOR.updateAndGet(existing -> existing != null ? existing : createOperator(jdbc, encoder));
    }

    private static String createOperator(JdbcTemplate jdbc, PasswordEncoder encoder) {
        String username = "audit-op-" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO SEC_USER (USER_PK, TENANT_ID, USERNAME, EMAIL, PASSWORD_HASH, FULL_NAME_AR,"
                + " FULL_NAME_EN, STATUS_CODE, REALM, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)"
                + " VALUES (nextval('SEQ_SEC_USER'), 1, ?, ?, ?, 'مشغل التدقيق', 'Audit operator', 'ACTIVE', 'STAFF', TRUE,"
                + " 'test', now())",
            username, username + "@audit.test", encoder.encode(PASSWORD));
        jdbc.update("INSERT INTO SEC_USER_ROLE (USER_ROLE_PK, TENANT_ID, USER_ID, ROLE_ID, ASSIGNED_BY, ASSIGNED_AT)"
                + " SELECT nextval('SEQ_SEC_USER_ROLE'), 1, u.USER_PK, r.ROLE_PK, 'test', now()"
                + " FROM SEC_USER u JOIN SEC_ROLE r ON r.TENANT_ID = 1 AND r.CODE = 'SYS_ADMIN'"
                + " WHERE u.TENANT_ID = 1 AND u.USERNAME = ?",
            username);
        return username;
    }

    HttpResponse<String> login(String tenantCode, String username, String password) {
        return send(json("/api/v1/sec/auth/login")
            .header(TenantConstants.TENANT_CODE_HEADER, tenantCode)
            .POST(body("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}")));
    }

    String token(String tenantCode, String username) {
        HttpResponse<String> response = login(tenantCode, username, PASSWORD);
        if (response.statusCode() != 200) {
            throw new AssertionError("login " + tenantCode + "/" + username + " -> " + response.statusCode()
                + " " + response.body());
        }
        return JsonPath.read(response.body(), "$.data.accessToken");
    }

    /** Provisions a tenant whose administrator is {@link #TENANT_ADMIN} / {@link #PASSWORD}; returns its id. */
    long provisionTenant(String platformToken, String code) {
        HttpResponse<String> response = post(platformToken, "/api/v1/platform/tenants", "{\"code\":\"" + code
            + "\",\"nameAr\":\"مستأجر\",\"nameEn\":\"Tenant " + code + "\",\"adminUsername\":\"" + TENANT_ADMIN
            + "\",\"adminEmail\":\"admin@" + code.toLowerCase(Locale.ROOT) + ".test\",\"adminPassword\":\"" + PASSWORD
            + "\",\"adminFullNameAr\":\"مدير\",\"adminFullNameEn\":\"Administrator\"}");
        expect(response, 201, "create tenant " + code);
        return id(response, "$.data.id");
    }

    /** Creates a staff user (POST /api/v1/sec/users) and returns its id. */
    long createUser(String token, String username) {
        HttpResponse<String> response = post(token, "/api/v1/sec/users", "{\"username\":\"" + username
            + "\",\"email\":\"" + username + "@users.test\",\"fullNameAr\":\"مستخدم\",\"fullNameEn\":\"User\","
            + "\"password\":\"" + PASSWORD + "\"}");
        expect(response, 201, "create user " + username);
        return id(response, "$.data.userPk");
    }

    /**
     * A CUSTOMER-realm access token of {@code tenantCode}: registers over the public API, activates the
     * account with JDBC (the e-mail verification is step 06's own test), then logs in.
     */
    String customerToken(JdbcTemplate jdbc, String tenantCode) {
        String email = "buyer." + UUID.randomUUID().toString().substring(0, 8) + "@shop.test";
        HttpResponse<String> registered = send(json("/api/v1/public/customers/register")
            .header(TenantConstants.TENANT_CODE_HEADER, tenantCode)
            .POST(body("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\",\"fullName\":\"Buyer\"}")));
        expect(registered, 201, "register customer");
        long id = id(registered, "$.data.id");
        jdbc.update("UPDATE SEC_USER SET STATUS_CODE = 'ACTIVE' WHERE USER_PK = ? AND REALM = 'CUSTOMER'", id);
        HttpResponse<String> login = send(json("/api/v1/public/customers/login")
            .header(TenantConstants.TENANT_CODE_HEADER, tenantCode)
            .POST(body("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}")));
        expect(login, 200, "customer login");
        return JsonPath.read(login.body(), "$.data.accessToken");
    }

    /** {@code GET /api/v1/audit/events} with the given query parameters. */
    HttpResponse<String> events(String token, Map<String, String> query) {
        StringBuilder path = new StringBuilder("/api/v1/audit/events?size=200");
        query.forEach((name, value) -> path.append('&').append(name).append('=')
            .append(URLEncoder.encode(value, StandardCharsets.UTF_8)));
        return get(token, path.toString());
    }

    /** Multipart upload (POST /api/v1/files) into {@code categoryId}; returns the document id. */
    long upload(String token, long categoryId, String fileName, byte[] content) {
        String boundary = "----erp" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream form = new ByteArrayOutputStream();
        field(form, boundary, "ownerId", "4711");
        field(form, boundary, "ownerType", "PRODUCT");
        field(form, boundary, "moduleCode", "SHOP");
        field(form, boundary, "fileCategoryFk", String.valueOf(categoryId));
        form.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + fileName
            + "\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        form.writeBytes(content);
        form.writeBytes(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        HttpResponse<String> response = send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/files"))
            .header("Authorization", "Bearer " + token)
            .header("Accept-Language", "en")
            .header("User-Agent", USER_AGENT)
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(form.toByteArray())));
        expect(response, 201, "upload");
        return id(response, "$.data.id");
    }

    HttpResponse<String> get(String token, String path) {
        return send(authorized(json(path), token).GET());
    }

    HttpResponse<String> post(String token, String path, String jsonBody) {
        return send(authorized(json(path), token).POST(body(jsonBody)));
    }

    HttpResponse<String> put(String token, String path, String jsonBody) {
        return send(authorized(json(path), token).PUT(body(jsonBody)));
    }

    HttpResponse<String> patch(String token, String path, String jsonBody) {
        return send(authorized(json(path), token).method("PATCH", body(jsonBody)));
    }

    static void expect(HttpResponse<String> response, int status, String what) {
        if (response.statusCode() != status) {
            throw new AssertionError(what + " -> " + response.statusCode() + " " + response.body());
        }
    }

    static long id(HttpResponse<String> response, String path) {
        return ((Number) JsonPath.read(response.body(), path)).longValue();
    }

    static String errorCode(HttpResponse<String> response) {
        return JsonPath.read(response.body(), "$.error.code");
    }

    private static void field(ByteArrayOutputStream body, String boundary, String name, String value) {
        body.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
            + value + "\r\n").getBytes(StandardCharsets.UTF_8));
    }

    private HttpRequest.Builder json(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
            .header("Content-Type", "application/json")
            .header("Accept-Language", "en")
            .header("User-Agent", USER_AGENT);
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
