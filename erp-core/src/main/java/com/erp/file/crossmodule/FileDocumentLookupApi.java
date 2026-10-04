package com.erp.file.crossmodule;

/**
 * FILE's inbound cross-module surface: lets another module validate a {@code fileId} it stores as
 * a soft reference (no FK) — e.g. NOTIF's template {@code attachmentFileId} (XM-NOTIF-002). Direct
 * Spring interface injection, never loopback HTTP; returns primitives only, never the entity or bytes.
 */
public interface FileDocumentLookupApi {

    /** True when a file with this id exists and is not soft-deleted (ACTIVE or ARCHIVED). */
    boolean isAvailable(Long fileId);
}
