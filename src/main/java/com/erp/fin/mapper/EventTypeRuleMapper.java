package com.erp.fin.mapper;

import com.erp.fin.dto.EventTypeRuleCreateRequest;
import com.erp.fin.dto.EventTypeRuleResponse;
import com.erp.fin.dto.RuleLineResponse;
import com.erp.fin.entity.EventTypeRule;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for ENT-FIN-009 (EventTypeRule) — build-create-mapper, no MapStruct. API-FIN-010
 * is the only write this sub delivers for the entity, so no {@code updateEntityFromRequest}
 * exists yet.
 *
 * <p>{@code toResponse} takes the already-mapped line responses rather than reading a child
 * collection off the entity: ENT-FIN-009 maps no {@code @OneToMany} at all (RuleLine owns the FK),
 * and A.1.19 forbids walking one in any case, so the line set is assembled by the service and
 * passed in — the same arrangement as {@code RecurringTemplateMapper}.
 */
@Component
public class EventTypeRuleMapper {

    public EventTypeRule toEntity(EventTypeRuleCreateRequest request) {
        if (request == null) {
            return null;
        }
        return EventTypeRule.builder()
            .eventTypeCode(request.getEventTypeCode())
            .nameAr(request.getNameAr())
            .nameEn(request.getNameEn())
            .isActiveFl(Boolean.TRUE)
            .build();
    }

    public EventTypeRuleResponse toResponse(EventTypeRule entity,
                                            List<RuleLineResponse> lines) {
        if (entity == null) {
            return null;
        }
        List<RuleLineResponse> safeLines = lines == null ? List.of() : lines;
        return EventTypeRuleResponse.builder()
            .eventTypeRulePk(entity.getEventTypeRulePk())
            .eventTypeCode(entity.getEventTypeCode())
            .nameAr(entity.getNameAr())
            .nameEn(entity.getNameEn())
            .isActiveFl(Boolean.TRUE.equals(entity.getIsActiveFl()))
            .lineCount(safeLines.size())
            .lines(safeLines)
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}
