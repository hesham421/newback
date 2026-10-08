package com.erp.file.crossmodule;

/**
 * XM-FILE-003 (tenant-maturity C5) — FILE's private store for a server-generated file (the tenant export archive): stored
 * as a PRIVATE, uncategorised document of the current tenant (RULE-FILE-011) and handed out through FILE's single-use
 * download token, bound to the calling user (RULE-FILE-003). The consuming service carries the permission gate.
 */
public interface FilePrivateStoreApi {

    /** Stores the request's local file in the current tenant, joining the caller's transaction. */
    StoredPrivateFile storePrivateFile(PrivateFileStoreRequest request);

    /** A single-use download token (10 minutes) for a document of the current tenant, bound to the calling username. */
    DownloadGrant issueDownloadToken(Long documentId);
}
