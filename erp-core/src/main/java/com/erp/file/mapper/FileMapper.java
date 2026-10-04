package com.erp.file.mapper;

import com.erp.file.dto.FileMetadataResponse;
import com.erp.file.dto.UploadRequest;
import com.erp.file.entity.FileCategory;
import com.erp.file.entity.FileDocument;
import com.erp.file.repository.FileMetadataView;
import org.springframework.stereotype.Component;

/**
 * Manual entity/DTO mapper for ENTITY-FILE-001 (FileDocument). Documents are immutable once stored
 * (no update-from-request), so this maps only upload → entity and entity/projection → metadata
 * response. Server-detected fields (contentType, fileTypeId, fileSize, fileStatusId) and the
 * resolved FileCategory are passed in by the service — the mapper performs no detection or lookup.
 */
@Component
public class FileMapper {

    /**
     * Builds a new FileDocument from the upload request plus the service-detected fields. The content
     * itself is not mapped: the service hands it to the selected StorageProvider (erp-core step 07),
     * which records where it lives ({@code storageProvider}, then {@code storageRef}).
     */
    public FileDocument toEntity(UploadRequest request, String fileName, String contentType,
                                 long fileSize, String contentHash, String fileTypeId,
                                 String fileStatusId, FileCategory category, String storageProvider) {
        if (request == null) {
            return null;
        }
        return FileDocument.builder()
            .ownerId(request.getOwnerId())
            .ownerType(request.getOwnerType())
            .moduleCode(request.getModuleCode())
            .fileName(fileName)
            .contentType(contentType)
            .fileSize(fileSize)
            .contentHash(contentHash)
            .storageProvider(storageProvider)
            .fileTypeId(fileTypeId)
            .fileStatusId(fileStatusId)
            .fileCategoryFk(category)
            .build();
    }

    /**
     * Single mapping for both the entity (store/softDelete) and the bytes-excluded projection
     * (getMetadata/listByOwner) paths — {@link FileDocument} implements {@link FileMetadataView}, so
     * the two callers share one method and can never drift field-for-field. {@code publicUrl} is
     * resolved by the service (it needs the tenant code), null for a PRIVATE document.
     */
    public FileMetadataResponse toMetadataResponse(FileMetadataView view, String publicUrl) {
        if (view == null) {
            return null;
        }
        return FileMetadataResponse.builder()
            .id(view.getId())
            .ownerId(view.getOwnerId())
            .ownerType(view.getOwnerType())
            .moduleCode(view.getModuleCode())
            .fileName(view.getFileName())
            .contentType(view.getContentType())
            .fileSize(view.getFileSize())
            .fileTypeId(view.getFileTypeId())
            .fileStatusId(view.getFileStatusId())
            .fileCategoryId(view.getFileCategoryId())
            .storageProvider(view.getStorageProvider())
            .visibility(view.getVisibility())
            .publicUrl(publicUrl)
            .createdAt(view.getCreatedAt())
            .createdBy(view.getCreatedBy())
            .updatedAt(view.getUpdatedAt())
            .updatedBy(view.getUpdatedBy())
            .build();
    }
}
