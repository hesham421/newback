package com.erp.file.mapper;

import com.erp.file.dto.CategoryCreateRequest;
import com.erp.file.dto.CategoryResponse;
import com.erp.file.dto.CategoryUpdateRequest;
import com.erp.file.entity.FileCategory;
import org.springframework.stereotype.Component;

/** Manual entity/DTO mapper for ENTITY-FILE-002 (FileCategory). */
@Component
public class FileCategoryMapper {

    public FileCategory toEntity(CategoryCreateRequest request) {
        if (request == null) {
            return null;
        }
        return FileCategory.builder()
            .categoryCode(request.getCategoryCode())   // NOT .toUpperCase() — @PrePersist owns it
            .nameAr(request.getNameAr())
            .nameEn(request.getNameEn())
            .maxSizeBytes(request.getMaxSizeBytes())
            .allowedContentTypes(request.getAllowedContentTypes())
            .isActive(request.getIsActiveFl() != null ? request.getIsActiveFl() : Boolean.TRUE)
            .build();
    }

    /**
     * Mutates in place. Skips categoryCode — immutable (RULE-FILE-007, structurally absent from the
     * request). Does NOT touch isActive: active-state changes go exclusively through the
     * DELETE-gated deactivate endpoint, so update cannot be used to bypass that authorization.
     */
    public void updateEntityFromRequest(FileCategory entity, CategoryUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setNameAr(request.getNameAr());
        entity.setNameEn(request.getNameEn());
        entity.setMaxSizeBytes(request.getMaxSizeBytes());
        entity.setAllowedContentTypes(request.getAllowedContentTypes());
    }

    public CategoryResponse toResponse(FileCategory entity) {
        if (entity == null) {
            return null;
        }
        return CategoryResponse.builder()
            .id(entity.getId())
            .categoryCode(entity.getCategoryCode())
            .nameAr(entity.getNameAr())
            .nameEn(entity.getNameEn())
            .maxSizeBytes(entity.getMaxSizeBytes())
            .allowedContentTypes(entity.getAllowedContentTypes())
            .isActiveFl(Boolean.TRUE.equals(entity.getIsActive()))
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}
