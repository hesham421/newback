package com.erp.file;

import static org.assertj.core.api.Assertions.assertThat;

import com.erp.file.crossmodule.FileDocumentLookupApi;
import com.erp.file.crossmodule.FileImageStoreApi;
import com.erp.file.crossmodule.ImageRejection;
import com.erp.file.crossmodule.ImageStoreRequest;
import com.erp.file.crossmodule.ImageStoreResult;
import com.erp.testsupport.AbstractIntegrationTest;
import com.erp.testsupport.StaffApiClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * erp-core 1.3.0 (TM-D D.4) — {@link FileImageStoreApi} (XM-FILE-002) end to end in the PLATFORM tenant:
 * an accepted image is stored uncategorised and served on the public path at once (RULE-FILE-010,
 * ADR-FILE-001); PNG inline, an allowed safe SVG as an attachment; rejections write nothing
 * (RULE-FILE-008/009); discard withdraws the URL and is idempotent; {@code publicUrls} batches.
 */
class FileImageStoreIntegrationTest extends AbstractIntegrationTest {

    private static final Set<String> RASTER = Set.of(FileImageStoreApi.TYPE_PNG, FileImageStoreApi.TYPE_JPEG,
        FileImageStoreApi.TYPE_WEBP);
    private static final Set<String> LOGO = Set.of(FileImageStoreApi.TYPE_PNG, FileImageStoreApi.TYPE_SVG);

    @LocalServerPort
    private int port;
    @Autowired
    private FileImageStoreApi imageStore;
    @Autowired
    private FileDocumentLookupApi lookupApi;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private StaffApiClient api;

    @BeforeEach
    void authenticate() {
        api = new StaffApiClient(port);
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("image-store-test", null, List.of()));
    }

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void anAcceptedPng_isStoredUncategorisedPublic_andServedInlineWithoutAToken() {
        byte[] png = FileHttp.png("store-" + StaffApiClient.unique("P"));
        ImageStoreResult result = imageStore.storePublicImage(request(png, 1_000, RASTER));

        assertThat(result.isStored()).isTrue();
        assertThat(result.rejection()).isNull();
        assertThat(result.image().contentType()).isEqualTo("image/png");
        assertThat(result.image().size()).isEqualTo(png.length);
        assertThat(result.image().publicUrl()).startsWith("/api/v1/public/files/PLATFORM/");
        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT FILE_CATEGORY_FK, VISIBILITY, FILE_STATUS_ID, FILE_TYPE_ID,"
            + " CONTENT_TYPE, OWNER_TYPE, MODULE_CODE FROM FILE_DOCUMENT WHERE ID = ?", result.image().documentId());
        assertThat(row.get("file_category_fk")).isNull();
        assertThat(row).containsEntry("visibility", "PUBLIC").containsEntry("file_status_id", "ACTIVE")
            .containsEntry("file_type_id", "IMAGE").containsEntry("content_type", "image/png")
            .containsEntry("owner_type", "TEST_OWNER").containsEntry("module_code", "TEST");

        HttpResponse<byte[]> served = api.anonymousGet(result.image().publicUrl());
        assertThat(served.statusCode()).isEqualTo(200);
        assertThat(served.body()).isEqualTo(png);
        assertThat(served.headers().firstValue("Content-Disposition")).hasValueSatisfying(v -> assertThat(v).startsWith("inline"));
        assertThat(served.headers().firstValue("X-Content-Type-Options")).hasValue("nosniff");

        assertThat(lookupApi.publicUrl(result.image().documentId())).hasValue(result.image().publicUrl());
        assertThat(lookupApi.publicUrls(List.of(result.image().documentId(), 987_654_321L)))
            .containsExactly(Map.entry(result.image().documentId(), result.image().publicUrl()));

        imageStore.discard(result.image().documentId());
        assertThat(api.anonymousGet(result.image().publicUrl()).statusCode()).isEqualTo(404);
        assertThat(jdbcTemplate.queryForMap("SELECT VISIBILITY, PUBLIC_SLUG, FILE_STATUS_ID FROM FILE_DOCUMENT WHERE ID = ?",
            result.image().documentId())).containsEntry("visibility", "PRIVATE").containsEntry("file_status_id", "DELETED")
            .containsEntry("public_slug", null);
        imageStore.discard(result.image().documentId());
        imageStore.discard(null);
        imageStore.discard(987_654_321L);
        assertThat(lookupApi.publicUrl(result.image().documentId())).isEmpty();
    }

    @Test
    void anAllowedSafeSvg_isStoredAndServedAsAnAttachment_whileUnsafeOrDisallowedOnesWriteNothing() {
        byte[] svg = ("<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 2 2\"><circle r=\"1\" fill=\"url(#g)\"/>"
            + "<!-- " + StaffApiClient.unique("S") + " --></svg>").getBytes(StandardCharsets.UTF_8);
        ImageStoreResult logo = imageStore.storePublicImage(request(svg, 10_000, LOGO));
        assertThat(logo.isStored()).isTrue();
        assertThat(logo.image().contentType()).isEqualTo("image/svg+xml");
        HttpResponse<byte[]> served = api.anonymousGet(logo.image().publicUrl());
        assertThat(served.statusCode()).isEqualTo(200);
        assertThat(served.headers().firstValue("Content-Disposition"))
            .hasValueSatisfying(v -> assertThat(v).startsWith("attachment"));
        assertThat(served.headers().firstValue("Content-Security-Policy")).hasValue("sandbox; default-src 'none'");

        Integer before = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM FILE_DOCUMENT", Integer.class);
        assertThat(imageStore.storePublicImage(request(svg, 10_000, RASTER)).rejection())
            .isEqualTo(ImageRejection.TYPE_NOT_ALLOWED);
        assertThat(imageStore.storePublicImage(request(
            "<svg onload=\"alert(1)\"/>".getBytes(StandardCharsets.UTF_8), 10_000, LOGO)).rejection())
            .isEqualTo(ImageRejection.UNSAFE_SVG);
        assertThat(imageStore.storePublicImage(request(FileHttp.png("big"), 4, RASTER)).rejection())
            .isEqualTo(ImageRejection.TOO_LARGE);
        assertThat(imageStore.storePublicImage(request(new byte[0], 10, RASTER)).rejection()).isEqualTo(ImageRejection.EMPTY);
        assertThat(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM FILE_DOCUMENT", Integer.class)).isEqualTo(before);
    }

    @Test
    void anUncategorisedDocumentUploadedThroughTheApi_stillCannotBePublishedByVisibility() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM FILE_DOCUMENT WHERE FILE_CATEGORY_FK IS NULL"
            + " AND VISIBILITY = 'PUBLIC' AND OWNER_TYPE NOT IN ('TEST_OWNER', 'SEC_USER')", Integer.class);
        assertThat(count).as("only the image store creates uncategorised PUBLIC documents").isZero();
    }

    private static ImageStoreRequest request(byte[] content, long maxBytes, Set<String> allowed) {
        return new ImageStoreRequest("TEST_OWNER", 4711L, "TEST", content, "image.bin", maxBytes, allowed);
    }
}
