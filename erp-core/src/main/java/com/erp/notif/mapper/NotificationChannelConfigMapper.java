package com.erp.notif.mapper;

import com.erp.notif.dto.ChannelCreateRequest;
import com.erp.notif.dto.ChannelResponse;
import com.erp.notif.dto.ChannelUpdateRequest;
import com.erp.notif.entity.NotificationChannelConfig;
import org.springframework.stereotype.Component;

/** Manual entity/DTO mapper for ENTITY-NOTIF-003 (NotificationChannelConfig). */
@Component
public class NotificationChannelConfigMapper {

    public NotificationChannelConfig toEntity(ChannelCreateRequest request) {
        if (request == null) {
            return null;
        }
        return NotificationChannelConfig.builder()
            .channelTypeId(request.getChannelTypeId())   // NOT .toUpperCase() — @PrePersist owns it
            .isEnabled(request.getIsEnabledFl() != null ? request.getIsEnabledFl() : Boolean.TRUE)
            .configJson(request.getConfigJson())
            .build();
    }

    /**
     * Mutates in place. Skips channelTypeId — immutable (RULE-NOTIF-006, structurally absent from the
     * request). Bridges the DTO's isEnabledFl onto the entity's isEnabled when supplied.
     */
    public void updateEntityFromRequest(NotificationChannelConfig entity, ChannelUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }
        if (request.getIsEnabledFl() != null) {
            entity.setIsEnabled(request.getIsEnabledFl());
        }
        entity.setConfigJson(request.getConfigJson());
    }

    public ChannelResponse toResponse(NotificationChannelConfig entity) {
        if (entity == null) {
            return null;
        }
        return ChannelResponse.builder()
            .id(entity.getId())
            .channelTypeId(entity.getChannelTypeId())
            .isEnabledFl(Boolean.TRUE.equals(entity.getIsEnabled()))
            .configJson(entity.getConfigJson())
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}
