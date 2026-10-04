package com.erp.fin.mapper;

import com.erp.fin.dto.FiscalPeriodResponse;
import com.erp.fin.entity.FiscalPeriod;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for ENT-FIN-008 (FiscalPeriod) — build-create-mapper, no MapStruct. Serves
 * API-FIN-024/025/026 directly and API-FIN-023 through {@link FiscalYearMapper}.
 *
 * <p>There is no {@code toEntity(FiscalPeriodCreateRequest)}: a period is never created on its
 * own. REQ-FIN-031 generates the whole set with its fiscal year, so the construction lives on
 * {@link FiscalYearMapper#toPeriodEntity} where the parent is in hand (A.4.2). There is no
 * {@code updateEntityFromRequest} either: the only changes a period ever takes are its three
 * guarded status transitions, which are the entity's own {@code open()} / {@code softClose()} /
 * {@code hardClose(...)} / {@code yearEndClose()} mutators, never a request-body merge.
 */
@Component
public class FiscalPeriodMapper {

    public FiscalPeriodResponse toResponse(FiscalPeriod entity) {
        if (entity == null) {
            return null;
        }
        return FiscalPeriodResponse.builder()
            .fiscalPeriodPk(entity.getFiscalPeriodPk())
            .fiscalYearId(entity.getFiscalYear() == null
                ? null : entity.getFiscalYear().getFiscalYearPk())
            .periodNo(entity.getPeriodNo())
            .nameAr(entity.getNameAr())
            .nameEn(entity.getNameEn())
            .startDate(entity.getStartDate())
            .endDate(entity.getEndDate())
            .statusCode(entity.getStatusCode())
            .closedBy(entity.getClosedBy())
            .closedAt(entity.getClosedAt())
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }
}
