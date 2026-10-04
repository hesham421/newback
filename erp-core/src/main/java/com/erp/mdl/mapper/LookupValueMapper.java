package com.erp.mdl.mapper;

import com.erp.mdl.dto.LookupValueCreateRequest;
import com.erp.mdl.dto.LookupValueResponse;
import com.erp.mdl.dto.LookupValueUpdateRequest;
import com.erp.mdl.entity.LookupType;
import com.erp.mdl.entity.LookupValue;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for ENT-MDL-002 (LookupValue) — build-create-mapper, no MapStruct. Child shape:
 * {@code toEntity} takes the resolved parent {@code LookupType} as a compile-time-safe parameter
 * (A.4.2) — the caller (service) cannot forget to set the FK.
 */
@Component
public class LookupValueMapper {

    public LookupValue toEntity(LookupValueCreateRequest request, LookupType parent) {
        if (request == null) {
            return null;
        }
        return LookupValue.builder()
            .lookupType(parent)
            .code(request.getCode())
            .nameAr(request.getNameAr())
            .nameEn(request.getNameEn())
            .sortOrder(request.getSortOrder())
            .build();
    }

    /** {@code lookupTypeId} and {@code code} are create-only — never touched here. */
    public void updateEntityFromRequest(LookupValue entity, LookupValueUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setNameAr(request.getNameAr());
        entity.setNameEn(request.getNameEn());
        entity.setSortOrder(request.getSortOrder());
    }

    public LookupValueResponse toResponse(LookupValue entity) {
        if (entity == null) {
            return null;
        }
        return LookupValueResponse.builder()
            .lookupValuePk(entity.getLookupValuePk())
            .lookupTypeId(entity.getLookupType() == null ? null : entity.getLookupType().getLookupTypePk())
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
