package com.erp.sec.mapper;

import com.erp.sec.dto.AuditLogEntryResponse;
import com.erp.sec.entity.AuditLogEntry;
import com.erp.sec.entity.User;
import org.springframework.stereotype.Component;

/**
 * Manual entity/DTO mapper for ENT-SEC-011 (AuditLogEntry). Read-only by design — the table is
 * append-only, so there is no {@code toEntity} / {@code updateEntityFromRequest} half.
 */
@Component
public class AuditLogEntryMapper {

    /**
     * The actor is flattened to its id only, which a LAZY proxy answers without initializing, so a
     * page of entries costs no extra select.
     */
    public AuditLogEntryResponse toResponse(AuditLogEntry entity) {
        if (entity == null) {
            return null;
        }
        User actor = entity.getActor();
        return AuditLogEntryResponse.builder()
            .auditLogPk(entity.getAuditLogPk())
            .eventTypeCode(entity.getEventTypeCode())
            .actorUserId(actor != null ? actor.getUserPk() : null)
            .occurredAt(entity.getOccurredAt())
            .targetRef(entity.getTargetRef())
            .detailsAr(entity.getDetailsAr())
            .detailsEn(entity.getDetailsEn())
            .ipAddress(entity.getIpAddress())
            .build();
    }
}
