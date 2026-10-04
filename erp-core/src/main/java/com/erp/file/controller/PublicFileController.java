package com.erp.file.controller;

import com.erp.file.service.FileService;
import com.erp.file.service.PublicFileUrls;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public (unauthenticated) file URLs — erp-core step 07: {@code GET /api/v1/public/files/{tenantCode}/{publicSlug}}.
 * Shareable as is (product images): no token, no header. The tenant comes from the path — the tenant
 * filter resolves {@code {tenantCode}} ({@code erp.core.tenant.path-tenant-paths}) before this runs,
 * and the security chain permits the path ({@code ErpCoreSecurityAutoConfiguration.PUBLIC_FILE_PATHS}).
 *
 * <p>Answers 302 to the provider's own URL when it can serve the content (S3 with a public base URL),
 * otherwise streams the bytes; both carry {@code Cache-Control: public, max-age=86400} and the
 * content-hash ETag. Streamed content is {@code inline} only for raster images and PDF
 * ({@code FileDocumentDomain.INLINE_SAFE_CONTENT_TYPES}); anything else (HTML, SVG, ...) is an
 * {@code attachment}. Every answer also carries {@code X-Content-Type-Options: nosniff} and
 * {@code Content-Security-Policy: sandbox; default-src 'none'}, so even a file a browser does render
 * cannot run script or load anything on the platform origin. Unknown tenant → 404 {@code TENANT_NOT_FOUND}; unknown, PRIVATE, archived/deleted
 * slug or a category no longer allowing public files → 404 {@code FILE_DOCUMENT_NOT_FOUND}. The only
 * logic here is mapping the service's result onto HTTP (redirect vs. body, headers).
 */
@RestController
@RequestMapping(PublicFileUrls.PUBLIC_PATH)
@RequiredArgsConstructor
@Tag(name = "Public Files", description = "Public, unauthenticated file URLs - روابط الملفات العامة دون مصادقة")
public class PublicFileController {

    /** {@code Cache-Control} of every public file answer. */
    static final CacheControl CACHE_CONTROL = CacheControl.maxAge(Duration.ofDays(1)).cachePublic();

    /** {@code Content-Security-Policy} of every public file answer: no script, no subresources, sandboxed. */
    static final String CONTENT_SECURITY_POLICY = "sandbox; default-src 'none'";

    private final FileService service;

    @GetMapping("/{tenantCode}/{publicSlug}")
    @Operation(summary = "Get a public file (no authentication)", description = "جلب ملف عام دون مصادقة")
    public ResponseEntity<InputStreamResource> get(
            @Parameter(description = "Tenant code - رمز المستأجر", example = "ACME") @PathVariable String tenantCode,
            @Parameter(description = "Public slug - المعرّف العام", example = "3q2-7wEjK9mZ0aBcDeFgHiJkLmNoPqRs")
            @PathVariable String publicSlug) {
        return toResponse(service.openPublic(publicSlug));
    }

    private static ResponseEntity<InputStreamResource> toResponse(FileService.PublicFile file) {
        HttpHeaders headers = new HttpHeaders();
        headers.setCacheControl(CACHE_CONTROL);
        headers.set("X-Content-Type-Options", "nosniff");
        headers.set("Content-Security-Policy", CONTENT_SECURITY_POLICY);
        if (file.etag() != null) {
            headers.setETag(file.etag());
        }
        if (file.redirectUrl() != null) {
            headers.setLocation(URI.create(file.redirectUrl()));
            return new ResponseEntity<>(headers, HttpStatus.FOUND);
        }
        headers.setContentType(MediaType.parseMediaType(file.contentType()));
        headers.setContentDisposition((file.inline() ? ContentDisposition.inline() : ContentDisposition.attachment())
            .filename(file.fileName(), StandardCharsets.UTF_8).build());
        if (file.size() != null) {
            headers.setContentLength(file.size());
        }
        return new ResponseEntity<>(new InputStreamResource(file.content()), headers, HttpStatus.OK);
    }
}
