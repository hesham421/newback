package com.erp.mdl.mapper;

import com.erp.mdl.dto.LookupTypeCreateRequest;
import com.erp.mdl.dto.LookupTypeResponse;
import com.erp.mdl.dto.LookupTypeUpdateRequest;
import com.erp.mdl.entity.LookupType;
import org.springframework.stereotype.Component;

/** Manual mapper for ENT-MDL-001 (LookupType) — build-create-mapper, no MapStruct. */
@Component
public class LookupTypeMapper {

    public LookupType toEntity(LookupTypeCreateRequest request) {
        if (request == null) {
            return null;
        }
        return LookupType.builder()
            .key(request.getKey())
            .ownerModuleCode(request.getOwnerModuleCode())
            .nameAr(request.getNameAr())
            .nameEn(request.getNameEn())
            .build();
    }

    /** Name-only, per RULE-MDL-003 — key/ownerModuleCode are absent from the request entirely. */
    public void updateEntityFromRequest(LookupType entity, LookupTypeUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setNameAr(request.getNameAr());
        entity.setNameEn(request.getNameEn());
    }

    public LookupTypeResponse toResponse(LookupType entity) {
        if (entity == null) {
            return null;
        }
        return LookupTypeResponse.builder()
            .lookupTypePk(entity.getLookupTypePk())
            .key(entity.getKey())
            .ownerModuleCode(entity.getOwnerModuleCode())
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
