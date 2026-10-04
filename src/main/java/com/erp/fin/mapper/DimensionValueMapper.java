package com.erp.fin.mapper;

import com.erp.fin.dto.DimensionValueCreateRequest;
import com.erp.fin.dto.DimensionValueResponse;
import com.erp.fin.entity.Dimension;
import com.erp.fin.entity.DimensionValue;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for ENT-FIN-003 (DimensionValue) — build-create-mapper, no MapStruct. Child shape:
 * {@code toEntity} takes the resolved parent {@code Dimension} as a compile-time-safe parameter
 * (A.4.2), so the service cannot forget the FK.
 */
@Component
public class DimensionValueMapper {

    public DimensionValue toEntity(DimensionValueCreateRequest request, Dimension parent) {
        if (request == null) {
            return null;
        }
        return DimensionValue.builder()
            .dimension(parent)
            .code(request.getCode())
            .nameAr(request.getNameAr())
            .nameEn(request.getNameEn())
            .sortOrder(request.getSortOrder())
            .isActiveFl(Boolean.TRUE)
            .build();
    }

    public DimensionValueResponse toResponse(DimensionValue entity) {
        if (entity == null) {
            return null;
        }
        return DimensionValueResponse.builder()
            .dimensionValuePk(entity.getDimensionValuePk())
            .dimensionId(entity.getDimension() == null
                ? null : entity.getDimension().getDimensionPk())
            .code(entity.getCode())
            .nameAr(entity.getNameAr())
            .nameEn(entity.getNameEn())
            .sortOrder(entity.getSortOrder())
            .isActiveFl(Boolean.TRUE.equals(entity.getIsActiveFl()))
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}
