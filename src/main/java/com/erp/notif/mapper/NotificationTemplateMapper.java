package com.erp.notif.mapper;

import com.erp.notif.dto.TemplateCreateRequest;
import com.erp.notif.dto.TemplateResponse;
import com.erp.notif.dto.TemplateUpdateRequest;
import com.erp.notif.entity.NotificationTemplate;
import org.springframework.stereotype.Component;

/** Manual entity/DTO mapper for ENTITY-NOTIF-002 (NotificationTemplate). */
@Component
public class NotificationTemplateMapper {

    public NotificationTemplate toEntity(TemplateCreateRequest request) {
        if (request == null) {
            return null;
        }
        return NotificationTemplate.builder()
            .templateCode(request.getTemplateCode())   // NOT .toUpperCase() — @PrePersist owns it
            .nameAr(request.getNameAr())
            .nameEn(request.getNameEn())
            .subjectAr(request.getSubjectAr())
            .subjectEn(request.getSubjectEn())
            .bodyAr(request.getBodyAr())
            .bodyEn(request.getBodyEn())
            .attachmentFileId(request.getAttachmentFileId())
            .isActive(request.getIsActiveFl() != null ? request.getIsActiveFl() : Boolean.TRUE)
            .build();
    }

    /**
     * Mutates in place. Skips templateCode — immutable (RULE-NOTIF-006, structurally absent from the
     * request). Bridges the DTO's isActiveFl onto the entity's isActive when supplied.
     */
    public void updateEntityFromRequest(NotificationTemplate entity, TemplateUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setNameAr(request.getNameAr());
        entity.setNameEn(request.getNameEn());
        entity.setSubjectAr(request.getSubjectAr());
        entity.setSubjectEn(request.getSubjectEn());
        entity.setBodyAr(request.getBodyAr());
        entity.setBodyEn(request.getBodyEn());
        entity.setAttachmentFileId(request.getAttachmentFileId());
        if (request.getIsActiveFl() != null) {
            entity.setIsActive(request.getIsActiveFl());
        }
    }

    public TemplateResponse toResponse(NotificationTemplate entity) {
        if (entity == null) {
            return null;
        }
        return TemplateResponse.builder()
            .id(entity.getId())
            .templateCode(entity.getTemplateCode())
            .nameAr(entity.getNameAr())
            .nameEn(entity.getNameEn())
            .subjectAr(entity.getSubjectAr())
            .subjectEn(entity.getSubjectEn())
            .bodyAr(entity.getBodyAr())
            .bodyEn(entity.getBodyEn())
            .attachmentFileId(entity.getAttachmentFileId())
            .isActiveFl(Boolean.TRUE.equals(entity.getIsActive()))
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}
