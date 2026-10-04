package com.erp.sequence.mapper;

import com.erp.sequence.domain.ResetPolicy;
import com.erp.sequence.dto.NumberSeriesCreateRequest;
import com.erp.sequence.dto.NumberSeriesResponse;
import com.erp.sequence.dto.NumberSeriesUpdateRequest;
import com.erp.sequence.entity.NumberSeries;
import org.springframework.stereotype.Component;

/** Manual mapper of {@link NumberSeries} (erp-core step 09). No normalization (the entity owns it). */
@Component
public class NumberSeriesMapper {

    /** A new series: its first row, for {@code periodKey} (computed by the service from the policy). */
    public NumberSeries toEntity(NumberSeriesCreateRequest request, String periodKey) {
        if (request == null) {
            return null;
        }
        return NumberSeries.builder()
            .code(request.getCode())
            .prefix(request.getPrefix())
            .pattern(request.getPattern() != null ? request.getPattern() : NumberSeries.DEFAULT_PATTERN)
            .resetPolicy(request.getResetPolicy() != null ? request.getResetPolicy() : ResetPolicy.YEARLY)
            .periodKey(periodKey)
            .nextValue(request.getNextValue() != null ? request.getNextValue() : 1L)
            .build();
    }

    /** The row of a new period of {@code anchor}'s series: same configuration, counter at 1. */
    public NumberSeries toNewPeriod(NumberSeries anchor, String periodKey) {
        if (anchor == null) {
            return null;
        }
        return NumberSeries.builder()
            .code(anchor.getCode())
            .prefix(anchor.getPrefix())
            .pattern(anchor.getPattern())
            .resetPolicy(anchor.getResetPolicy())
            .periodKey(periodKey)
            .nextValue(1L)
            .isActive(Boolean.TRUE.equals(anchor.getIsActive()))
            .build();
    }

    /** Mutates in place; code, reset policy, period and counter are never touched. */
    public void updateEntityFromRequest(NumberSeries entity, NumberSeriesUpdateRequest request) {
        if (entity == null || request == null) {
            return;
        }
        entity.setPrefix(request.getPrefix());
        entity.setPattern(request.getPattern());
    }

    public NumberSeriesResponse toResponse(NumberSeries entity) {
        if (entity == null) {
            return null;
        }
        return NumberSeriesResponse.builder()
            .id(entity.getId())
            .code(entity.getCode())
            .prefix(entity.getPrefix())
            .pattern(entity.getPattern())
            .resetPolicy(entity.getResetPolicy())
            .periodKey(entity.getPeriodKey())
            .nextValue(entity.getNextValue())
            .isActive(Boolean.TRUE.equals(entity.getIsActive()))
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}
