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

    /** Builds a new FileDocument from the upload request plus the service-detected fields. */
    public FileDocument toEntity(UploadRequest request, String fileName, String contentType,
                                 long fileSize, byte[] fileContent, String fileTypeId,
                                 String fileStatusId, FileCategory category) {
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
            .fileContent(fileContent)
            .fileTypeId(fileTypeId)
            .fileStatusId(fileStatusId)
            .fileCategoryFk(category)
            .build();
    }

    /**
     * Single mapping for both the entity (store/softDelete) and the bytes-excluded projection
     * (getMetadata/listByOwner) paths — {@link FileDocument} implements {@link FileMetadataView}, so
     * the two callers share one method and can never drift field-for-field.
     */
    public FileMetadataResponse toMetadataResponse(FileMetadataView view) {
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
            .createdAt(view.getCreatedAt())
            .createdBy(view.getCreatedBy())
            .updatedAt(view.getUpdatedAt())
            .updatedBy(view.getUpdatedBy())
            .build();
    }
}
