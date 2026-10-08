package com.erp.file.repository;

import jakarta.persistence.Tuple;
import java.time.Instant;

/**
 * Read-only projection for FileDocument metadata (DRV-003) — every column EXCEPT the BYTEA
 * {@code fileContent}. Backs the single metadata read (QR-FILE-0004) and the paged owner list
 * (QR-FILE-0005) so those reads never load file bytes. {@code fileCategoryId} reads the
 * FILE_CATEGORY_FK column via the to-one identifier (no join — Hibernate resolves it from the FK).
 */
public interface FileMetadataView {

    Long getId();

    Long getOwnerId();

    String getOwnerType();

    String getModuleCode();

    String getFileName();

    String getContentType();

    Long getFileSize();

    String getFileTypeId();

    String getFileStatusId();

    Long getFileCategoryId();

    Instant getCreatedAt();

    String getCreatedBy();

    Instant getUpdatedAt();

    String getUpdatedBy();

    /** erp-core step 07 — StorageProvider key (DB | LOCAL | S3). */
    String getStorageProvider();

    /** erp-core step 07 — provider-specific reference of the content. */
    String getStorageRef();

    /** erp-core step 07 — PRIVATE | PUBLIC. */
    String getVisibility();

    /** erp-core step 07 — slug of a PUBLIC document, else null. */
    String getPublicSlug();

    /** erp-core step 07 — SHA-256 (hex) of the content. */
    String getContentHash();

    /** erp-core step 07 — FILE_CATEGORY.ALLOW_PUBLIC of the document's category; null without a category. */
    Boolean getCategoryAllowPublic();

    /** tenant-maturity C5 (RULE-FILE-012) — the authority a caller must hold to see the document; null = none. */
    String getRequiredAuthority();

    /**
     * Builds a view from a {@link FileDocumentRepository#METADATA_SELECT} {@link Tuple} row. The
     * record's component names match this interface's getters exactly, so its auto-generated
     * accessors satisfy {@link FileMetadataView} with no manual delegation needed.
     */
    static FileMetadataView from(Tuple tuple) {
        record TupleFileMetadataView(
            Long getId, Long getOwnerId, String getOwnerType, String getModuleCode,
            String getFileName, String getContentType, Long getFileSize, String getFileTypeId,
            String getFileStatusId, Long getFileCategoryId, Instant getCreatedAt,
            String getCreatedBy, Instant getUpdatedAt, String getUpdatedBy, String getStorageProvider,
            String getStorageRef, String getVisibility, String getPublicSlug,
            String getContentHash, Boolean getCategoryAllowPublic, String getRequiredAuthority)
            implements FileMetadataView {
        }
        return new TupleFileMetadataView(
            tuple.get("id", Long.class), tuple.get("ownerId", Long.class),
            tuple.get("ownerType", String.class), tuple.get("moduleCode", String.class),
            tuple.get("fileName", String.class), tuple.get("contentType", String.class),
            tuple.get("fileSize", Long.class), tuple.get("fileTypeId", String.class),
            tuple.get("fileStatusId", String.class), tuple.get("fileCategoryId", Long.class),
            tuple.get("createdAt", Instant.class), tuple.get("createdBy", String.class),
            tuple.get("updatedAt", Instant.class), tuple.get("updatedBy", String.class),
            tuple.get("storageProvider", String.class), tuple.get("storageRef", String.class),
            tuple.get("visibility", String.class), tuple.get("publicSlug", String.class),
            tuple.get("contentHash", String.class), tuple.get("categoryAllowPublic", Boolean.class),
            tuple.get("requiredAuthority", String.class));
    }
}
