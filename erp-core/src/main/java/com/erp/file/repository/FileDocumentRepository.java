package com.erp.file.repository;

import com.erp.file.entity.FileDocument;
import jakarta.persistence.Tuple;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ENTITY-FILE-001 (FileDocument). Metadata reads (QR-FILE-0004 single,
 * QR-FILE-0005 owner list) return the bytes-excluded {@link FileMetadataView} shape (DRV-003) —
 * built from a {@link Tuple} rather than Spring Data's automatic interface-projection conversion,
 * which fails ("Failed to convert from type [Object[]] to type [FileMetadataView]") against this
 * project's Spring Data JPA / Hibernate 7 versions for this alias-select JPQL shape. Only the
 * download path (QR-FILE-0003) loads the BYTEA content. Ownership is polymorphic with no FK — the
 * owner list matches ownerId/ownerType/moduleCode EXACT with optional fileTypeId/fileStatusId
 * EXACT. Module-internal; consumed only by the FILE services and the FILE crossmodule adapter.
 */
@Repository
public interface FileDocumentRepository
    extends JpaRepository<FileDocument, Long>,
            JpaSpecificationExecutor<FileDocument> {

    String METADATA_SELECT = "SELECT f.id AS id, f.ownerId AS ownerId, f.ownerType AS ownerType, "
        + "f.moduleCode AS moduleCode, f.fileName AS fileName, f.contentType AS contentType, "
        + "f.fileSize AS fileSize, f.fileTypeId AS fileTypeId, f.fileStatusId AS fileStatusId, "
        + "f.fileCategoryFk.id AS fileCategoryId, f.createdAt AS createdAt, f.createdBy AS createdBy, "
        + "f.updatedAt AS updatedAt, f.updatedBy AS updatedBy, f.storageProvider AS storageProvider, "
        + "f.storageRef AS storageRef, f.visibility AS visibility, f.publicSlug AS publicSlug, "
        + "f.contentHash AS contentHash, c.allowPublic AS categoryAllowPublic "
        + "FROM FileDocument f LEFT JOIN f.fileCategoryFk c";

    /** Alias of the content column in {@link #findContentTupleById}. */
    String CONTENT_ALIAS = "content";

    /** QR-FILE-0004 / QR-FILE-0002 — single metadata read by id (bytes excluded). */
    @Query(METADATA_SELECT + " WHERE f.id = :id")
    Optional<Tuple> findMetadataTupleById(@Param("id") Long id);

    /**
     * QR-FILE-0005 — owner list (bytes excluded, DRV-003). ownerId/ownerType/moduleCode EXACT;
     * fileTypeId/fileStatusId optional EXACT (null param = no filter). Paged, no join.
     */
    @Query(value = METADATA_SELECT + " WHERE f.ownerId = :ownerId AND f.ownerType = :ownerType "
        + "AND f.moduleCode = :moduleCode "
        + "AND (:fileTypeId IS NULL OR f.fileTypeId = :fileTypeId) "
        + "AND (:fileStatusId IS NULL OR f.fileStatusId = :fileStatusId)",
        countQuery = "SELECT COUNT(f) FROM FileDocument f WHERE f.ownerId = :ownerId "
        + "AND f.ownerType = :ownerType AND f.moduleCode = :moduleCode "
        + "AND (:fileTypeId IS NULL OR f.fileTypeId = :fileTypeId) "
        + "AND (:fileStatusId IS NULL OR f.fileStatusId = :fileStatusId)")
    Page<Tuple> findMetadataTupleByOwner(@Param("ownerId") Long ownerId,
                                         @Param("ownerType") String ownerType,
                                         @Param("moduleCode") String moduleCode,
                                         @Param("fileTypeId") String fileTypeId,
                                         @Param("fileStatusId") String fileStatusId,
                                         Pageable pageable);

    /** tenant-maturity D.4 — metadata of several documents at once ({@code FileDocumentLookupApi.publicUrls}). */
    @Query(METADATA_SELECT + " WHERE f.id IN :ids")
    List<Tuple> findMetadataTuplesByIdIn(@Param("ids") Collection<Long> ids);

    /** Existence check for {@code FileDocumentLookupApi} — no content or metadata loaded. */
    boolean existsByIdAndFileStatusIdNot(Long id, String fileStatusId);

    /**
     * erp-core step 07 — the bytes of a DB-stored document (DbStorageProvider only). A {@link Tuple}
     * rather than {@code Optional<byte[]>}, which Spring Data would treat as a collection result.
     */
    @Query("SELECT f.fileContent AS " + CONTENT_ALIAS + " FROM FileDocument f WHERE f.id = :id")
    Optional<Tuple> findContentTupleById(@Param("id") Long id);

    /**
     * erp-core step 07 — a servable public document of the current tenant by slug: PUBLIC, in the given
     * lifecycle status, and in a category that (still) allows public files — or, since tenant-maturity
     * D.4, uncategorised (an image-store document, RULE-FILE-010). Bytes excluded.
     */
    @Query(METADATA_SELECT + " WHERE f.publicSlug = :slug AND f.visibility = :visibility "
        + "AND f.fileStatusId = :status AND (c.id IS NULL OR c.allowPublic = true)")
    Optional<Tuple> findPublicMetadataTupleBySlug(@Param("slug") String slug,
                                                  @Param("visibility") String visibility,
                                                  @Param("status") String status);
}
