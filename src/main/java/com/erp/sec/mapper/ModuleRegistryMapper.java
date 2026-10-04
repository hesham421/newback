package com.erp.sec.mapper;

import com.erp.sec.dto.ModuleMenuResponse;
import com.erp.sec.dto.ModuleRegistryCreateRequest;
import com.erp.sec.dto.ModuleRegistryResponse;
import com.erp.sec.dto.RegistryRowResponse;
import com.erp.sec.dto.ScreenMenuResponse;
import com.erp.sec.dto.ScreenRegistryResponse;
import com.erp.sec.entity.ModuleRegistry;
import java.util.List;
import org.springframework.stereotype.Component;

/** Manual entity/DTO mapper for ENT-SEC-004 (ModuleRegistry). */
@Component
public class ModuleRegistryMapper {

    public ModuleRegistry toEntity(ModuleRegistryCreateRequest request) {
        if (request == null) {
            return null;
        }
        return ModuleRegistry.builder()
            .code(request.getCode())   // NOT .toUpperCase() — @PrePersist owns it
            .nameAr(request.getNameAr())
            .nameEn(request.getNameEn())
            .isActiveFl(Boolean.TRUE)
            .build();
    }

    /** API-SEC-021 row — the screens are assembled by the service and handed in (A.4.2 shape). */
    public RegistryRowResponse toRegistryRowResponse(ModuleRegistry entity,
                                                     List<ScreenRegistryResponse> screens) {
        if (entity == null) {
            return null;
        }
        return RegistryRowResponse.builder()
            .moduleRegPk(entity.getModuleRegPk())
            .code(entity.getCode())
            .nameAr(entity.getNameAr())
            .nameEn(entity.getNameEn())
            .isActiveFl(Boolean.TRUE.equals(entity.getIsActiveFl()))
            .screens(screens)
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }

    /** API-SEC-027 node — navigation only, so no audit fields and no active flag. */
    public ModuleMenuResponse toMenuResponse(ModuleRegistry entity, List<ScreenMenuResponse> screens) {
        if (entity == null) {
            return null;
        }
        return ModuleMenuResponse.builder()
            .moduleRegPk(entity.getModuleRegPk())
            .code(entity.getCode())
            .nameAr(entity.getNameAr())
            .nameEn(entity.getNameEn())
            .screens(screens)
            .build();
    }

    public ModuleRegistryResponse toResponse(ModuleRegistry entity) {
        if (entity == null) {
            return null;
        }
        return ModuleRegistryResponse.builder()
            .moduleRegPk(entity.getModuleRegPk())
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
