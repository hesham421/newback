package com.erp.file.crossmodule;

import java.util.Optional;

/**
 * FILE's inbound cross-module surface: lets another module validate a {@code fileId} it stores as
 * a soft reference (no FK) — e.g. NOTIF's template {@code attachmentFileId} (XM-NOTIF-002) — and, since
 * erp-core step 07, obtain the public URL of a published document (e.g. a product image). Direct
 * Spring interface injection, never loopback HTTP; returns primitives only, never the entity or bytes.
 */
public interface FileDocumentLookupApi {

    /** True when a file with this id exists and is not soft-deleted (ACTIVE or ARCHIVED). */
    boolean isAvailable(Long fileId);

    /**
     * The public URL of a PUBLIC, non-deleted document of the current tenant — the
     * {@code /api/v1/public/files/{tenantCode}/{publicSlug}} URL (prefixed with
     * {@code erp.core.files.public-base-url} when set) or the storage provider's direct URL — else empty.
     */
    Optional<String> publicUrl(Long documentId);
}
