package com.erp.audit.mapper;

import com.erp.audit.dto.AuditEventResponse;
import com.erp.audit.entity.AuditEvent;
import org.springframework.stereotype.Component;

/** Entity → response mapping of the audit read model (erp-core step 10); read-only, so no toEntity/update. */
@Component
public class AuditEventMapper {

    public AuditEventResponse toResponse(AuditEvent entity) {
        if (entity == null) {
            return null;
        }
        return AuditEventResponse.builder()
            .id(entity.getId())
            .occurredAt(entity.getOccurredAt())
            .actor(entity.getActor())
            .actorRealm(entity.getActorRealm())
            .actorUserId(entity.getActorUserId())
            .action(entity.getAction())
            .entityType(entity.getEntityType())
            .entityId(entity.getEntityId())
            .summaryAr(entity.getSummaryAr())
            .summaryEn(entity.getSummaryEn())
            .changes(entity.getChanges())
            .ip(entity.getIp())
            .userAgent(entity.getUserAgent())
            .reference(entity.getReference())
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}
