package com.erp.cu.mapper;

import com.erp.cu.dto.ConfigurationCreateRequest;
import com.erp.cu.dto.ConfigurationResponse;
import com.erp.cu.dto.ConfigurationUpdateRequest;
import com.erp.cu.entity.AppConfiguration;
import org.springframework.stereotype.Component;

/**
 * Manual entity/DTO mapper for ENTITY-CU-001 (AppConfiguration), named after the DTO/service
 * family (Configuration) per SVC-API.md's deliberate naming, not the entity name.
 */
@Component
public class ConfigurationMapper {

    public AppConfiguration toEntity(ConfigurationCreateRequest request) {
        if (request == null) {
            return null;
        }
        return AppConfiguration.builder()
            .configKey(request.getConfigKey())
            .configValue(request.getConfigValue())
            .notes(request.getNotes())
            .build();
        // isActive intentionally left unset here — AppConfiguration.isActive carries its own
        // @Builder.Default (Boolean.TRUE) and ConfigurationCreateRequest carries no isActive
        // field at all (excluded per API-CU-001 REQUEST — every configuration starts active).
    }

    /**
     * Mutates in place. Skips configKey — immutable (RULE-CU-003, structurally excluded from the
     * request type). When request.isActive is present, applies it via activate()/deactivate() —
     * never a raw setter (gov-enforce-backend-contract: "the active flag set directly ... is a
     * rejection trigger"). A null isActive means "no change" (partial update semantics).
     */
    public void updateEntityFromRequest(AppConfiguration entity, ConfigurationUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setConfigValue(request.getConfigValue());
        // notes is optional — a null (omitted) notes means "no change", consistent with isActive,
        // so a partial update does not wipe existing notes (send an empty string to clear it).
        if (request.getNotes() != null) {
            entity.setNotes(request.getNotes());
        }
        if (request.getIsActive() != null) {
            if (Boolean.TRUE.equals(request.getIsActive())) {
                entity.activate();
            } else {
                entity.deactivate();
            }
        }
    }

    public ConfigurationResponse toResponse(AppConfiguration entity) {
        if (entity == null) {
            return null;
        }
        return ConfigurationResponse.builder()
            .id(entity.getId())
            .configKey(entity.getConfigKey())
            .configValue(entity.getConfigValue())
            .notes(entity.getNotes())
            .isActive(Boolean.TRUE.equals(entity.getIsActive()))
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}
