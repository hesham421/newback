package com.erp.notif.mapper;

import com.erp.notif.dto.NotificationLogResponse;
import com.erp.notif.entity.NotificationLog;
import org.springframework.stereotype.Component;

/**
 * Manual entity/DTO mapper for ENTITY-NOTIF-001 (NotificationLog). Read-only surface — the log is a
 * system record created internally by dispatch, never from a client request, so there is no
 * {@code toEntity}/{@code updateEntityFromRequest}. The parent template association is exposed as its
 * id only, never as a nested entity.
 */
@Component
public class NotificationLogMapper {

    public NotificationLogResponse toResponse(NotificationLog entity) {
        if (entity == null) {
            return null;
        }
        return NotificationLogResponse.builder()
            .id(entity.getId())
            .recipientId(entity.getRecipientId())
            .channelTypeId(entity.getChannelTypeId())
            .notificationStatusId(entity.getNotificationStatusId())
            .moduleCode(entity.getModuleCode())
            .referenceId(entity.getReferenceId())
            .referenceType(entity.getReferenceType())
            .retryCount(entity.getRetryCount())
            .errorMessage(entity.getErrorMessage())
            .sentAt(entity.getSentAt())
            .templateId(entity.getTemplateFk() != null ? entity.getTemplateFk().getId() : null)
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}
