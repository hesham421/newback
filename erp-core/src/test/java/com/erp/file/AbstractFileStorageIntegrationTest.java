package com.erp.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.testsupport.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * erp-core step 07 — the FILE storage and public-URL flows over HTTP, run once per storage provider:
 * {@link DbStorageFileIntegrationTest} ({@code erp.core.files.storage=DB}, the default) and
 * {@link LocalStorageFileIntegrationTest} ({@code LOCAL} on a temporary directory). Each subclass adds
 * the provider-specific assertion of where the bytes actually landed ({@link #assertStoredByProvider}).
 *
 * <p>Covers: upload → private download via the single-use token; publish → public GET without any
 * token or header returns the bytes with {@code Cache-Control} and the content-hash ETag; unpublish →
 * public GET 404; a category without {@code ALLOW_PUBLIC} (or no category) → 409; tenant B can neither
 * publish nor read tenant A's document; plus the failure paths of the new endpoints.
 */
abstract class AbstractFileStorageIntegrationTest extends AbstractIntegrationTest {

    @LocalServerPort
    private int port;
    @Autowired
    protected JdbcTemplate jdbcTemplate;
    @Autowired
    private PasswordEncoder passwordEncoder;

    protected FileHttp http;
    protected String platformToken;

    /** Asserts how the provider under test stored the document (DB row vs. file on disk). */
    protected abstract void assertStoredByProvider(long documentId, byte[] content);

    /** The STORAGE_PROVIDER value new uploads must carry. */
    protected abstract String expectedProvider();

    @BeforeEach
    void setUpClient() {
        http = new FileHttp(port);
        platformToken = http.token(FileHttp.PLATFORM, FileHttp.platformOperator(jdbcTemplate, passwordEncoder));
    }

    @Test
    void upload_thenPrivateDownloadViaToken_returnsTheBytes_fromTheSelectedProvider() {
        byte[] content = FileHttp.png("private-" + FileHttp.unique("C"));
        long id = http.uploadOk(platformToken, null, "secret.png", content);

        HttpResponse<String> metadata = http.get(platformToken, "/api/v1/files/" + id);
        assertThat(metadata.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(metadata.body(), "$.data.storageProvider")).isEqualTo(expectedProvider());
        assertThat((String) JsonPath.read(metadata.body(), "$.data.visibility")).isEqualTo("PRIVATE");
        assertThat((Object) JsonPath.read(metadata.body(), "$.data.publicUrl")).isNull();

        HttpResponse<byte[]> download = http.privateDownload(platformToken, id);
        assertThat(download.statusCode()).isEqualTo(200);
        assertThat(download.body()).isEqualTo(content);
        assertThat(download.headers().firstValue("Content-Type")).hasValue("image/png");

        Map<String, Object> row = jdbcTemplate.queryForMap(
            "select storage_provider, storage_ref, visibility, public_slug, content_hash from file_document where id = ?", id);
        assertThat(row).containsEntry("storage_provider", expectedProvider())
            .containsEntry("visibility", "PRIVATE").containsEntry("content_hash", sha256(content));
        assertThat(row.get("public_slug")).isNull();
        assertStoredByProvider(id, content);
    }

    @Test
    void privateDownload_tokenIsSingleUse() {
        long id = http.uploadOk(platformToken, null, "once.png", FileHttp.png("once"));
        HttpResponse<String> minted = http.post(platformToken, "/api/v1/files/" + id + "/access-token", "");
        String accessToken = JsonPath.read(minted.body(), "$.data.accessToken");
        String path = "/api/v1/files/download?token=" + java.net.URLEncoder.encode(accessToken, StandardCharsets.UTF_8);

        assertThat(http.getWithToken(platformToken, path).statusCode()).isEqualTo(200);
        HttpResponse<byte[]> second = http.getWithToken(platformToken, path);
        assertThat(second.statusCode()).isEqualTo(401);
        assertThat(FileHttp.errorCodeOf(second)).isEqualTo("FILE_ACCESS_TOKEN_INVALID");
    }

    @Test
    void publish_thenPublicGetWithoutAuthOrHeader_returnsBytesAndCacheHeaders_unpublish_then404() {
        long category = http.createCategory(platformToken, FileHttp.unique("PUB_"), true);
        byte[] content = FileHttp.png("public-" + FileHttp.unique("C"));
        long id = http.uploadOk(platformToken, category, "product image.png", content);

        // Not public yet: no slug exists, so nothing to fetch; publishing returns the URL.
        HttpResponse<String> published = http.setVisibility(platformToken, id, "PUBLIC");
        assertThat(published.statusCode()).isEqualTo(200);
        assertThat((String) JsonPath.read(published.body(), "$.data.visibility")).isEqualTo("PUBLIC");
        String publicUrl = JsonPath.read(published.body(), "$.data.publicUrl");
        assertThat(publicUrl).matches("/api/v1/public/files/PLATFORM/[A-Za-z0-9_-]{32}");

        HttpResponse<byte[]> anonymous = http.anonymousGet(publicUrl);
        assertThat(anonymous.statusCode()).isEqualTo(200);
        assertThat(anonymous.body()).isEqualTo(content);
        assertThat(anonymous.headers().firstValue("Cache-Control")).hasValue("max-age=86400, public");
        assertThat(anonymous.headers().firstValue("ETag")).hasValue("\"" + sha256(content) + "\"");
        assertThat(anonymous.headers().firstValue("Content-Type")).hasValue("image/png");

        // Publishing again keeps the URL stable.
        HttpResponse<String> again = http.setVisibility(platformToken, id, "PUBLIC");
        assertThat((String) JsonPath.read(again.body(), "$.data.publicUrl")).isEqualTo(publicUrl);
        assertThat((String) JsonPath.read(http.get(platformToken, "/api/v1/files/" + id).body(), "$.data.publicUrl"))
            .isEqualTo(publicUrl);

        HttpResponse<String> unpublished = http.setVisibility(platformToken, id, "PRIVATE");
        assertThat(unpublished.statusCode()).isEqualTo(200);
        assertThat((Object) JsonPath.read(unpublished.body(), "$.data.publicUrl")).isNull();
        assertThat(jdbcTemplate.queryForObject("select public_slug from file_document where id = ?", String.class, id))
            .isNull();

        HttpResponse<byte[]> gone = http.anonymousGet(publicUrl);
        assertThat(gone.statusCode()).isEqualTo(404);
        assertThat(FileHttp.errorCodeOf(gone)).isEqualTo("FILE_DOCUMENT_NOT_FOUND");
    }

    @Test
    void publish_inCategoryWithoutAllowPublic_or_withoutCategory_is409() {
        long closed = http.createCategory(platformToken, FileHttp.unique("PRV_"), false);
        long inClosed = http.uploadOk(platformToken, closed, "a.png", FileHttp.png("closed"));
        long uncategorized = http.uploadOk(platformToken, null, "b.png", FileHttp.png("uncategorized"));

        for (long id : new long[] {inClosed, uncategorized}) {
            HttpResponse<String> response = http.setVisibility(platformToken, id, "PUBLIC");
            assertThat(response.statusCode()).isEqualTo(409);
            assertThat(FileHttp.errorCode(response)).isEqualTo("FILE_PUBLIC_NOT_ALLOWED");
            assertThat(jdbcTemplate.queryForObject("select visibility from file_document where id = ?", String.class, id))
                .isEqualTo("PRIVATE");
        }
    }

    @Test
    void visibility_failurePaths_unknownDocument404_invalidValue400_noToken401_deleted404() {
        assertThat(http.setVisibility(platformToken, 987_654_321L, "PUBLIC").statusCode()).isEqualTo(404);

        long category = http.createCategory(platformToken, FileHttp.unique("PUB_"), true);
        long id = http.uploadOk(platformToken, category, "c.png", FileHttp.png("failures"));
        HttpResponse<String> invalid = http.setVisibility(platformToken, id, "WORLD");
        assertThat(invalid.statusCode()).isEqualTo(400);
        assertThat(http.setVisibility(null, id, "PUBLIC").statusCode()).isEqualTo(401);

        assertThat(http.get(platformToken, "/api/v1/files/" + id).statusCode()).isEqualTo(200);
        HttpResponse<String> deleted = sendDelete(id);
        assertThat(deleted.statusCode()).isEqualTo(200);
        HttpResponse<String> afterDelete = http.setVisibility(platformToken, id, "PUBLIC");
        assertThat(afterDelete.statusCode()).isEqualTo(404);
        assertThat(FileHttp.errorCode(afterDelete)).isEqualTo("FILE_DOCUMENT_NOT_FOUND");
    }

    @Test
    void publicGet_unknownSlug404_unknownTenant404_archivedDocument404() {
        HttpResponse<byte[]> unknownSlug = http.anonymousGet("/api/v1/public/files/PLATFORM/doesNotExist0123456789abcdefABCD");
        assertThat(unknownSlug.statusCode()).isEqualTo(404);
        assertThat(FileHttp.errorCodeOf(unknownSlug)).isEqualTo("FILE_DOCUMENT_NOT_FOUND");

        HttpResponse<byte[]> unknownTenant = http.anonymousGet("/api/v1/public/files/NO_SUCH_TENANT_X/whatever");
        assertThat(unknownTenant.statusCode()).isEqualTo(404);
        assertThat(FileHttp.errorCodeOf(unknownTenant)).isEqualTo("TENANT_NOT_FOUND");

        long category = http.createCategory(platformToken, FileHttp.unique("PUB_"), true);
        long id = http.uploadOk(platformToken, category, "d.png", FileHttp.png("archived"));
        String url = JsonPath.read(http.setVisibility(platformToken, id, "PUBLIC").body(), "$.data.publicUrl");
        assertThat(http.anonymousGet(url).statusCode()).isEqualTo(200);
        jdbcTemplate.update("update file_document set file_status_id = 'ARCHIVED' where id = ?", id);
        assertThat(http.anonymousGet(url).statusCode()).isEqualTo(404);
    }

    @Test
    void tenantB_cannotPublishOrReadTenantAsDocument_andAPublicUrlIgnoresAForeignToken() {
        String codeB = FileHttp.unique("FB");
        http.provisionTenant(platformToken, codeB);
        String tokenB = http.token(codeB, FileHttp.TENANT_ADMIN);

        long category = http.createCategory(platformToken, FileHttp.unique("PUB_"), true);
        byte[] content = FileHttp.png("tenant-a-" + FileHttp.unique("C"));
        long idA = http.uploadOk(platformToken, category, "a.png", content);

        // B cannot publish A's document (tenant-filtered: it does not exist for B)
        HttpResponse<String> publishByB = http.setVisibility(tokenB, idA, "PUBLIC");
        assertThat(publishByB.statusCode()).isEqualTo(404);
        assertThat(FileHttp.errorCode(publishByB)).isEqualTo("FILE_DOCUMENT_NOT_FOUND");
        assertThat(http.get(tokenB, "/api/v1/files/" + idA).statusCode()).isEqualTo(404);

        String urlA = JsonPath.read(http.setVisibility(platformToken, idA, "PUBLIC").body(), "$.data.publicUrl");
        String slugA = urlA.substring(urlA.lastIndexOf('/') + 1);

        // A's slug under B's tenant code is not found
        HttpResponse<byte[]> viaB = http.anonymousGet("/api/v1/public/files/" + codeB + "/" + slugA);
        assertThat(viaB.statusCode()).isEqualTo(404);
        assertThat(FileHttp.errorCodeOf(viaB)).isEqualTo("FILE_DOCUMENT_NOT_FOUND");
        assertThat(http.getWithToken(tokenB, "/api/v1/public/files/" + codeB + "/" + slugA).statusCode()).isEqualTo(404);

        // A's public URL works for anyone, also when the caller carries B's token (path tenant wins)
        HttpResponse<byte[]> withForeignToken = http.getWithToken(tokenB, urlA);
        assertThat(withForeignToken.statusCode()).isEqualTo(200);
        assertThat(withForeignToken.body()).isEqualTo(content);

        // B's own documents: B can publish in its own category and gets a URL with its own code
        long categoryB = http.createCategory(tokenB, FileHttp.unique("PUB_"), true);
        long idB = http.uploadOk(tokenB, categoryB, "b.png", FileHttp.png("tenant-b"));
        String urlB = JsonPath.read(http.setVisibility(tokenB, idB, "PUBLIC").body(), "$.data.publicUrl");
        assertThat(urlB).startsWith("/api/v1/public/files/" + codeB + "/");
        assertThat(http.anonymousGet(urlB).statusCode()).isEqualTo(200);
        // ... and A cannot read B's slug under its own code
        assertThat(http.anonymousGet("/api/v1/public/files/PLATFORM/" + urlB.substring(urlB.lastIndexOf('/') + 1))
            .statusCode()).isEqualTo(404);
    }

    private HttpResponse<String> sendDelete(long id) {
        try {
            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder(
                    java.net.URI.create("http://localhost:" + port + "/api/v1/files/" + id + "?action=DELETE"))
                .header("Authorization", "Bearer " + platformToken).DELETE().build();
            return java.net.http.HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        } catch (java.io.IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    protected static String sha256(byte[] content) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
