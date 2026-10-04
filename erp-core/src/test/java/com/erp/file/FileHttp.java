package com.erp.file;

import com.erp.tenant.TenantConstants;
import com.jayway.jsonpath.JsonPath;
import java.io.ByteArrayOutputStream;
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
 * Test-only HTTP client for the FILE storage tests (erp-core step 07): talks to the running test
 * server like a real client, so the whole chain — JWT filter, tenant filter (header and path),
 * authorization, controllers — is exercised. Fixtures are created over HTTP; only the PLATFORM
 * operator account is written with JDBC (FILE tests must not import SEC or tenant internals).
 */
final class FileHttp {

    static final String PASSWORD = "File-Test-Passw0rd!";
    static final String PLATFORM = TenantConstants.PLATFORM_TENANT_CODE;

    /** A tiny PNG (signature + IHDR start + random tail) so content sniffing detects image/png. */
    static byte[] png(String marker) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13, 'I', 'H', 'D', 'R'});
        out.writeBytes(marker.getBytes(StandardCharsets.UTF_8));
        return out.toByteArray();
    }

    private static final AtomicReference<String> PLATFORM_OPERATOR = new AtomicReference<>();

    private final HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
    private final String baseUrl;

    FileHttp(int port) {
        this.baseUrl = "http://localhost:" + port;
    }

    static String unique(String prefix) {
        return prefix + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    /** A PLATFORM account holding PLATFORM's SYS_ADMIN (every FILE permission, FILE:DOCUMENT:PUBLISH, tenant management). */
    static String platformOperator(JdbcTemplate jdbc, PasswordEncoder encoder) {
        return PLATFORM_OPERATOR.updateAndGet(existing -> existing != null ? existing : createOperator(jdbc, encoder));
    }

    private static String createOperator(JdbcTemplate jdbc, PasswordEncoder encoder) {
        String username = "file-op-" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("INSERT INTO SEC_USER (USER_PK, TENANT_ID, USERNAME, EMAIL, PASSWORD_HASH, FULL_NAME_AR,"
                + " FULL_NAME_EN, STATUS_CODE, REALM, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)"
                + " VALUES (nextval('SEQ_SEC_USER'), 1, ?, ?, ?, 'مشغل الملفات', 'File operator', 'ACTIVE', 'STAFF', TRUE,"
                + " 'test', now())",
            username, username + "@file.test", encoder.encode(PASSWORD));
        jdbc.update("INSERT INTO SEC_USER_ROLE (USER_ROLE_PK, TENANT_ID, USER_ID, ROLE_ID, ASSIGNED_BY, ASSIGNED_AT)"
                + " SELECT nextval('SEQ_SEC_USER_ROLE'), 1, u.USER_PK, r.ROLE_PK, 'test', now()"
                + " FROM SEC_USER u JOIN SEC_ROLE r ON r.TENANT_ID = 1 AND r.CODE = 'SYS_ADMIN'"
                + " WHERE u.TENANT_ID = 1 AND u.USERNAME = ?",
            username);
        return username;
    }

    String token(String tenantCode, String username) {
        HttpResponse<String> response = send(json("/api/v1/sec/auth/login")
            .header(TenantConstants.TENANT_CODE_HEADER, tenantCode)
            .POST(body("{\"username\":\"" + username + "\",\"password\":\"" + PASSWORD + "\"}")));
        if (response.statusCode() != 200) {
            throw new AssertionError("login " + tenantCode + "/" + username + " -> " + response.statusCode()
                + " " + response.body());
        }
        return JsonPath.read(response.body(), "$.data.accessToken");
    }

    /**
     * Administrator username of the tenants these tests provision. Deliberately not {@code admin}: some
     * pre-existing SEC tests count rows of the user {@code admin} across all tenants.
     */
    static final String TENANT_ADMIN = "file-admin";

    /** Provisions a tenant whose administrator is {@link #TENANT_ADMIN} / {@link #PASSWORD}. */
    void provisionTenant(String platformToken, String code) {
        HttpResponse<String> response = post(platformToken, "/api/v1/platform/tenants", "{\"code\":\"" + code
            + "\",\"nameAr\":\"مستأجر\",\"nameEn\":\"Tenant " + code + "\",\"adminUsername\":\"" + TENANT_ADMIN + "\",\"adminEmail\":\"admin@"
            + code.toLowerCase() + ".test\",\"adminPassword\":\"" + PASSWORD
            + "\",\"adminFullNameAr\":\"مدير\",\"adminFullNameEn\":\"Administrator\"}");
        if (response.statusCode() != 201) {
            throw new AssertionError("create tenant " + code + " -> " + response.statusCode() + " " + response.body());
        }
    }

    /**
     * A CUSTOMER-realm access token of {@code tenantCode}: registers over the public API, activates the
     * account with JDBC (skipping the e-mail verification, which is step 06's own test), then logs in.
     */
    String customerToken(JdbcTemplate jdbc, String tenantCode) {
        String email = "buyer." + UUID.randomUUID().toString().substring(0, 8) + "@shop.test";
        HttpResponse<String> registered = send(json("/api/v1/public/customers/register")
            .header(TenantConstants.TENANT_CODE_HEADER, tenantCode)
            .POST(body("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\",\"fullName\":\"Buyer\"}")));
        if (registered.statusCode() != 201) {
            throw new AssertionError("register -> " + registered.statusCode() + " " + registered.body());
        }
        long id = ((Number) JsonPath.read(registered.body(), "$.data.id")).longValue();
        jdbc.update("UPDATE SEC_USER SET STATUS_CODE = 'ACTIVE' WHERE USER_PK = ? AND REALM = 'CUSTOMER'", id);
        HttpResponse<String> login = send(json("/api/v1/public/customers/login")
            .header(TenantConstants.TENANT_CODE_HEADER, tenantCode)
            .POST(body("{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}")));
        if (login.statusCode() != 200) {
            throw new AssertionError("customer login -> " + login.statusCode() + " " + login.body());
        }
        return JsonPath.read(login.body(), "$.data.accessToken");
    }

    /** Creates an active file category and returns its id. */
    long createCategory(String token, String code, boolean allowPublic) {
        HttpResponse<String> response = post(token, "/api/v1/files/categories", "{\"categoryCode\":\"" + code
            + "\",\"nameAr\":\"فئة\",\"nameEn\":\"Category " + code + "\",\"allowPublic\":" + allowPublic + "}");
        if (response.statusCode() != 201) {
            throw new AssertionError("create category " + code + " -> " + response.statusCode() + " " + response.body());
        }
        return ((Number) JsonPath.read(response.body(), "$.data.id")).longValue();
    }

    /** Multipart upload (POST /api/v1/files); {@code categoryId} may be null. */
    HttpResponse<String> upload(String token, Long categoryId, String fileName, byte[] content) {
        String boundary = "----erp" + UUID.randomUUID().toString().replace("-", "");
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        field(body, boundary, "ownerId", "4711");
        field(body, boundary, "ownerType", "PRODUCT");
        field(body, boundary, "moduleCode", "SHOP");
        if (categoryId != null) {
            field(body, boundary, "fileCategoryFk", String.valueOf(categoryId));
        }
        body.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"file\"; filename=\"" + fileName
            + "\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        body.writeBytes(content);
        body.writeBytes(("\r\n--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return send(HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/files"))
            .header("Authorization", "Bearer " + token)
            .header("Accept-Language", "en")
            .header("Content-Type", "multipart/form-data; boundary=" + boundary)
            .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())));
    }

    long uploadOk(String token, Long categoryId, String fileName, byte[] content) {
        HttpResponse<String> response = upload(token, categoryId, fileName, content);
        if (response.statusCode() != 201) {
            throw new AssertionError("upload -> " + response.statusCode() + " " + response.body());
        }
        return ((Number) JsonPath.read(response.body(), "$.data.id")).longValue();
    }

    HttpResponse<String> setVisibility(String token, long documentId, String visibility) {
        return send(authorized(json("/api/v1/files/" + documentId + "/visibility"), token)
            .method("PATCH", body("{\"visibility\":\"" + visibility + "\"}")));
    }

    /** Private download: mint a token, then GET /api/v1/files/download. */
    HttpResponse<byte[]> privateDownload(String token, long documentId) {
        HttpResponse<String> minted = post(token, "/api/v1/files/" + documentId + "/access-token", "");
        if (minted.statusCode() != 200) {
            throw new AssertionError("access-token -> " + minted.statusCode() + " " + minted.body());
        }
        String accessToken = JsonPath.read(minted.body(), "$.data.accessToken");
        return sendBytes(HttpRequest.newBuilder(URI.create(baseUrl + "/api/v1/files/download?token="
                + java.net.URLEncoder.encode(accessToken, StandardCharsets.UTF_8)))
            .header("Authorization", "Bearer " + token).GET());
    }

    /** A plain GET of a public path: no token, no X-Tenant-Code (what a browser or curl does). */
    HttpResponse<byte[]> anonymousGet(String path) {
        return sendBytes(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET());
    }

    /** A GET of a public path carrying a bearer token. */
    HttpResponse<byte[]> getWithToken(String token, String path) {
        return sendBytes(HttpRequest.newBuilder(URI.create(baseUrl + path)).header("Authorization", "Bearer " + token).GET());
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

    /** A request with an arbitrary method and no token or tenant header. */
    HttpResponse<String> anonymous(String method, String path) {
        return send(HttpRequest.newBuilder(URI.create(baseUrl + path)).method(method, HttpRequest.BodyPublishers.noBody()));
    }

    static String errorCode(HttpResponse<String> response) {
        return JsonPath.read(response.body(), "$.error.code");
    }

    static String errorCodeOf(HttpResponse<byte[]> response) {
        return JsonPath.read(new String(response.body(), StandardCharsets.UTF_8), "$.error.code");
    }

    private static void field(ByteArrayOutputStream body, String boundary, String name, String value) {
        body.writeBytes(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + name + "\"\r\n\r\n"
            + value + "\r\n").getBytes(StandardCharsets.UTF_8));
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

    private HttpResponse<byte[]> sendBytes(HttpRequest.Builder request) {
        try {
            return http.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }
}
