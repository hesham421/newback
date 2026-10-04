package com.erp.file.entity;

import com.erp.common.domain.AuditableEntity;
import com.erp.file.domain.FileDocumentDomain;
import com.erp.file.repository.FileMetadataView;
import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

/**
 * ENTITY-FILE-001 — FileDocument (stored file bytes + metadata). Source: db-script-FILE.md
 * DBS-FILE-001, DATA-DOM.md. Ownership (ownerId/ownerType/moduleCode) is a polymorphic
 * application reference — no governed FK (SRS A3/A7). contentType is server auto-detected
 * (RULE-FILE-002). fileContent is BYTEA, lazy-fetched and never eagerly serialized (download-only).
 * fileCategoryFk is the optional intra-module FK. Lifecycle (fileStatusId, LOV-FILE-002, A6) is
 * decided in FileDocumentDomain. Persistence-only.
 *
 * <p>erp-core step 07: the content lives where {@code storageProvider} says ({@code DB} = the
 * {@code fileContent} column, which is therefore nullable; {@code LOCAL}/{@code S3} = an external object
 * named by {@code storageRef}). {@code visibility} PRIVATE/PUBLIC and the per-tenant unique
 * {@code publicSlug} drive the public URL; {@code contentHash} (SHA-256 hex) is its ETag. Whether a
 * document may become PUBLIC is decided in FileDocumentDomain.
 */
@Entity
@Table(name = "FILE_DOCUMENT",
    indexes = {
        @Index(name = "IDX_FILE_DOCUMENT_OWNER", columnList = "OWNER_ID, OWNER_TYPE, MODULE_CODE"),
        @Index(name = "IDX_FILE_DOCUMENT_STATUS", columnList = "FILE_STATUS_ID"),
        @Index(name = "IDX_FILE_DOCUMENT_CATEGORY_FK", columnList = "FILE_CATEGORY_FK")
    }
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @SuperBuilder
public class FileDocument extends AuditableEntity implements FileMetadataView {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "file_document_seq")
    @SequenceGenerator(name = "file_document_seq", sequenceName = "SEQ_FILE_DOCUMENT", allocationSize = 1)
    @Column(name = "ID")
    private Long id;

    @NotNull(message = "{validation.required}")
    @Column(name = "OWNER_ID", nullable = false)
    private Long ownerId;

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    @Column(name = "OWNER_TYPE", length = 100, nullable = false)
    private String ownerType;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "MODULE_CODE", length = 50, nullable = false)
    private String moduleCode;

    @NotBlank(message = "{validation.required}")
    @Size(max = 255, message = "{validation.size}")
    @Column(name = "FILE_NAME", length = 255, nullable = false)
    private String fileName;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Column(name = "CONTENT_TYPE", length = 150, nullable = false)
    private String contentType;

    @Column(name = "FILE_SIZE")
    private Long fileSize;

    // No @Lob: FILE_CONTENT is a plain BYTEA column (V8), not a Postgres large object (OID).
    // @Lob on a byte[] makes Hibernate bind it as a LOB (OID/bigint), which PostgreSQL rejects
    // against a BYTEA column ("column is of type bytea but expression is of type bigint").
    // Nullable since erp-core step 07: only the DB storage provider fills it (DbStorageProvider).
    @Basic(fetch = FetchType.LAZY)
    @Column(name = "FILE_CONTENT")
    private byte[] fileContent;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "FILE_TYPE_ID", length = 50, nullable = false)
    private String fileTypeId;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Column(name = "FILE_STATUS_ID", length = 50, nullable = false)
    private String fileStatusId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "FILE_CATEGORY_FK",
        foreignKey = @ForeignKey(name = "FK_FILE_DOCUMENT_CATEGORY"))
    private FileCategory fileCategoryFk;

    /** StorageProvider key holding the content: DB | LOCAL | S3 (erp-core step 07). */
    @NotBlank(message = "{validation.required}")
    @Size(max = 8, message = "{validation.size}")
    @Column(name = "STORAGE_PROVIDER", length = 8, nullable = false)
    private String storageProvider;

    /** Provider-specific reference: DB = row id, LOCAL = path relative to the root, S3 = object key. */
    @Size(max = 512, message = "{validation.size}")
    @Column(name = "STORAGE_REF", length = 512)
    private String storageRef;

    /** PRIVATE (token download) or PUBLIC (public URL); see FileDocumentDomain. */
    @NotBlank(message = "{validation.required}")
    @Size(max = 8, message = "{validation.size}")
    @Column(name = "VISIBILITY", length = 8, nullable = false)
    @Builder.Default
    private String visibility = FileDocumentDomain.VISIBILITY_PRIVATE;

    /** Random slug of a PUBLIC document, unique per tenant (UQ_FILE_DOCUMENT_PUBLIC_SLUG); null when PRIVATE. */
    @Size(max = 64, message = "{validation.size}")
    @Column(name = "PUBLIC_SLUG", length = 64)
    private String publicSlug;

    /** SHA-256 (hex) of the content — the public ETag. */
    @Size(max = 64, message = "{validation.size}")
    @Column(name = "CONTENT_HASH", length = 64)
    private String contentHash;

    /** Makes the document PUBLIC under {@code slug} — pure mutation; FileDocumentDomain decides first. */
    public void publish(String slug) {
        this.visibility = FileDocumentDomain.VISIBILITY_PUBLIC;
        this.publicSlug = slug;
    }

    /** Makes the document PRIVATE and drops its slug — pure mutation. */
    public void unpublish() {
        this.visibility = FileDocumentDomain.VISIBILITY_PRIVATE;
        this.publicSlug = null;
    }

    /** {@link FileMetadataView#getCategoryAllowPublic()} — the category's public policy; null without a category. */
    @Override
    public Boolean getCategoryAllowPublic() {
        return fileCategoryFk != null ? fileCategoryFk.getAllowPublic() : null;
    }

    /** {@link FileMetadataView#getFileCategoryId()} — flattens the to-one FK to its id for the shared mapper. */
    @Override
    public Long getFileCategoryId() {
        return fileCategoryFk != null ? fileCategoryFk.getId() : null;
    }
}
