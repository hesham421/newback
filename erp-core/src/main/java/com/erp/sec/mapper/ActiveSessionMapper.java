package com.erp.sec.mapper;

import com.erp.sec.dto.ActiveSessionResponse;
import com.erp.sec.dto.SessionTerminationResponse;
import com.erp.sec.entity.ActiveSession;
import com.erp.sec.entity.User;
import org.springframework.stereotype.Component;

/**
 * Manual entity/DTO mapper for ENT-SEC-010 (ActiveSession). {@code tokenRef} is never serialized
 * (DATA-DOM-TRANSACTIONAL.md ENT-SEC-010 DTO MEMBERSHIP).
 */
@Component
public class ActiveSessionMapper {

    /** API-SEC-025 row. {@code username} needs the User itself, unlike the id-only audit mapping. */
    public ActiveSessionResponse toResponse(ActiveSession entity) {
        if (entity == null) {
            return null;
        }
        User user = entity.getUser();
        return ActiveSessionResponse.builder()
            .activeSessionPk(entity.getActiveSessionPk())
            .userId(user != null ? user.getUserPk() : null)
            .username(user != null ? user.getUsername() : null)
            .startedAt(entity.getStartedAt())
            .lastActivityAt(entity.getLastActivityAt())
            .ipAddress(entity.getIpAddress())
            .build();
    }

    public SessionTerminationResponse toTerminationResponse(ActiveSession entity) {
        if (entity == null) {
            return null;
        }
        return SessionTerminationResponse.builder()
            .activeSessionPk(entity.getActiveSessionPk())
            .terminatedAt(entity.getTerminatedAt())
            .build();
    }
}
