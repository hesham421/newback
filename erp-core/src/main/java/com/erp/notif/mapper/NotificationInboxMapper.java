package com.erp.notif.mapper;

import com.erp.notif.dto.InboxItemResponse;
import com.erp.notif.entity.NotificationInboxItem;
import org.springframework.stereotype.Component;

/**
 * Manual entity/DTO mapper for {@code NOTIF_INBOX} (erp-core step 08). Read-only surface: inbox rows
 * are written by the IN_APP channel provider, never from a client request, so there is no
 * {@code toEntity}/{@code updateEntityFromRequest}.
 */
@Component
public class NotificationInboxMapper {

    public InboxItemResponse toResponse(NotificationInboxItem entity) {
        if (entity == null) {
            return null;
        }
        return InboxItemResponse.builder()
            .id(entity.getId())
            .recipientUserId(entity.getRecipientUserId())
            .titleAr(entity.getTitleAr())
            .titleEn(entity.getTitleEn())
            .bodyAr(entity.getBodyAr())
            .bodyEn(entity.getBodyEn())
            .read(entity.getReadAt() != null)
            .readAt(entity.getReadAt())
            .referenceType(entity.getReferenceType())
            .referenceId(entity.getReferenceId())
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}
