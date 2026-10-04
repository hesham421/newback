package com.erp.sec.mapper;

import com.erp.sec.dto.ActionRegistryResponse;
import com.erp.sec.dto.ScreenMenuResponse;
import com.erp.sec.dto.ScreenRegistryCreateRequest;
import com.erp.sec.dto.ScreenRegistryResponse;
import com.erp.sec.entity.ModuleRegistry;
import com.erp.sec.entity.ScreenRegistry;
import java.util.List;
import org.springframework.stereotype.Component;

/** Manual entity/DTO mapper for ENT-SEC-005 (ScreenRegistry). */
@Component
public class ScreenRegistryMapper {

    /** The owning module is a parameter (A.4.2) — the service resolves it from {@code moduleCode}. */
    public ScreenRegistry toEntity(ScreenRegistryCreateRequest request, ModuleRegistry module) {
        if (request == null) {
            return null;
        }
        return ScreenRegistry.builder()
            .pageCode(request.getPageCode())   // NOT .toUpperCase() — @PrePersist owns it
            .module(module)
            .nameAr(request.getNameAr())
            .nameEn(request.getNameEn())
            .isActiveFl(Boolean.TRUE)
            .build();
    }

    /** API-SEC-021 — same row plus its active actions, which the service resolves and hands in. */
    public ScreenRegistryResponse toResponse(ScreenRegistry entity,
                                             List<ActionRegistryResponse> actions) {
        ScreenRegistryResponse response = toResponse(entity);
        if (response != null) {
            response.setActions(actions);
        }
        return response;
    }

    /** API-SEC-027 leaf — navigation only, so no audit fields and no active flag. */
    public ScreenMenuResponse toMenuResponse(ScreenRegistry entity) {
        if (entity == null) {
            return null;
        }
        return ScreenMenuResponse.builder()
            .screenRegPk(entity.getScreenRegPk())
            .pageCode(entity.getPageCode())
            .nameAr(entity.getNameAr())
            .nameEn(entity.getNameEn())
            .build();
    }

    public ScreenRegistryResponse toResponse(ScreenRegistry entity) {
        if (entity == null) {
            return null;
        }
        ModuleRegistry module = entity.getModule();
        return ScreenRegistryResponse.builder()
            .screenRegPk(entity.getScreenRegPk())
            .pageCode(entity.getPageCode())
            .moduleId(module != null ? module.getModuleRegPk() : null)
            .moduleCode(module != null ? module.getCode() : null)
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
