package com.erp.fin.mapper;

import com.erp.fin.dto.DimensionCreateRequest;
import com.erp.fin.dto.DimensionResponse;
import com.erp.fin.entity.Dimension;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for ENT-FIN-002 (Dimension) — build-create-mapper, no MapStruct. API-FIN-006 is
 * the only write this sub delivers for the entity, so no {@code updateEntityFromRequest} exists
 * yet: the plan's ENTITY REGISTRY gives ENT-FIN-002 "create, read, search, deactivate" and no
 * update at all.
 */
@Component
public class DimensionMapper {

    public Dimension toEntity(DimensionCreateRequest request) {
        if (request == null) {
            return null;
        }
        return Dimension.builder()
            .code(request.getCode())
            .nameAr(request.getNameAr())
            .nameEn(request.getNameEn())
            .isActiveFl(Boolean.TRUE)
            .build();
    }

    public DimensionResponse toResponse(Dimension entity) {
        if (entity == null) {
            return null;
        }
        return DimensionResponse.builder()
            .dimensionPk(entity.getDimensionPk())
            .code(entity.getCode())
            .nameAr(entity.getNameAr())
            .nameEn(entity.getNameEn())
            .isActiveFl(Boolean.TRUE.equals(entity.getIsActiveFl()))
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}
