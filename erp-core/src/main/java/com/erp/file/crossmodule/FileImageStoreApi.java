package com.erp.file.crossmodule;

/**
 * XM-FILE-002 (tenant-maturity D.4) — FILE's shared image store for small public images (SEC profile
 * photos, TENANT logos). An image is validated from its bytes (RULE-FILE-008/009) and, when accepted,
 * stored uncategorised in the current tenant and published at once under a random slug on the public
 * file path (RULE-FILE-010, ADR-FILE-008). A rejection is a result value, never an exception, so the
 * caller raises its own error code. The consuming service carries the permission gate.
 */
public interface FileImageStoreApi {

    String TYPE_PNG = "image/png";
    String TYPE_JPEG = "image/jpeg";
    String TYPE_WEBP = "image/webp";
    String TYPE_SVG = "image/svg+xml";

    /** Validates and, when accepted, stores and publishes the image in the current tenant. */
    ImageStoreResult storePublicImage(ImageStoreRequest request);

    /** Withdraws an image: DELETED and PRIVATE (its URL answers 404 at once); unknown or deleted ids are ignored. */
    void discard(Long documentId);
}
