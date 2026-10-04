package com.erp.sec.mapper;

import com.erp.sec.dto.ActionRegistryCreateRequest;
import com.erp.sec.dto.ActionRegistryResponse;
import com.erp.sec.entity.ActionRegistry;
import com.erp.sec.entity.ScreenRegistry;
import org.springframework.stereotype.Component;

/** Manual entity/DTO mapper for ENT-SEC-006 (ActionRegistry). */
@Component
public class ActionRegistryMapper {

    /**
     * The owning screen and the already-derived {@code permissionCode} are parameters: the
     * derivation is the service's (API-SEC-020 Orchestration line), never the mapper's.
     */
    public ActionRegistry toEntity(ActionRegistryCreateRequest request,
                                   ScreenRegistry screen,
                                   String permissionCode) {
        if (request == null) {
            return null;
        }
        return ActionRegistry.builder()
            .permissionCode(permissionCode)
            .screen(screen)
            .actionCode(request.getActionCode())   // NOT .toUpperCase() — @PrePersist owns it
            .nameAr(request.getNameAr())
            .nameEn(request.getNameEn())
            .isActiveFl(Boolean.TRUE)
            .build();
    }

    public ActionRegistryResponse toResponse(ActionRegistry entity) {
        if (entity == null) {
            return null;
        }
        ScreenRegistry screen = entity.getScreen();
        return ActionRegistryResponse.builder()
            .actionRegPk(entity.getActionRegPk())
            .permissionCode(entity.getPermissionCode())
            .screenId(screen != null ? screen.getScreenRegPk() : null)
            .pageCode(screen != null ? screen.getPageCode() : null)
            .actionCode(entity.getActionCode())
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
