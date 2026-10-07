package com.erp.file.service;

import com.erp.autoconfigure.ErpCoreProperties;
import com.erp.file.domain.FileDocumentDomain;
import com.erp.file.repository.FileMetadataView;
import com.erp.file.storage.StorageProviderRegistry;
import com.erp.tenant.TenantContext;
import com.erp.tenant.crossmodule.TenantLookupApi;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Public file URLs (erp-core step 07). A PUBLIC, non-deleted document is reachable at
 * {@code [erp.core.files.public-base-url]/api/v1/public/files/{tenantCode}/{publicSlug}} — the tenant
 * travels in the path, so the URL works in a plain browser or {@code curl} without any header — or,
 * when its storage provider can serve it directly (S3 with {@code public-base-url}), at that direct URL.
 * Also issues the random slugs: 24 bytes from {@link SecureRandom}, base64url without padding
 * (32 characters, 192 bits — not guessable, unique per tenant by {@code UQ_FILE_DOCUMENT_PUBLIC_SLUG}).
 */
@Component
@RequiredArgsConstructor
public class PublicFileUrls {

    /** Path prefix of {@code PublicFileController}. */
    public static final String PUBLIC_PATH = "/api/v1/public/files";

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SLUG_BYTES = 24;

    private final TenantLookupApi tenantLookupApi;
    private final StorageProviderRegistry storageProviders;
    private final ErpCoreProperties properties;

    /** A fresh random public slug. */
    public static String newSlug() {
        byte[] bytes = new byte[SLUG_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * The public URL of {@code view} in the current tenant, or empty unless the public endpoint would
     * actually serve it ({@link FileDocumentDomain#isPubliclyServable}: PUBLIC, ACTIVE, category allows
     * public files or, for an image-store document, no category).
     */
    public Optional<String> of(FileMetadataView view) {
        if (view == null || !FileDocumentDomain.isPubliclyServable(view.getVisibility(), view.getPublicSlug(),
                view.getFileStatusId(), view.getFileCategoryId(), view.getCategoryAllowPublic())) {
            return Optional.empty();
        }
        Optional<String> direct = storageProviders.forKey(view.getStorageProvider()).publicUrl(view.getStorageRef());
        if (direct.isPresent()) {
            return direct;
        }
        return tenantLookupApi.codeOf(TenantContext.require())
            .map(code -> baseUrl() + PUBLIC_PATH + "/" + code + "/" + view.getPublicSlug());
    }

    private String baseUrl() {
        String base = properties.getFiles().getPublicBaseUrl();
        if (!StringUtils.hasText(base)) {
            return "";
        }
        String trimmed = base.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }
}
