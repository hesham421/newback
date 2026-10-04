package com.erp.fin.mapper;

import com.erp.fin.domain.FiscalPeriodDomain;
import com.erp.fin.domain.FiscalPeriodDomain.GeneratedPeriod;
import com.erp.fin.dto.FiscalPeriodResponse;
import com.erp.fin.dto.FiscalYearCreateRequest;
import com.erp.fin.dto.FiscalYearResponse;
import com.erp.fin.entity.FiscalPeriod;
import com.erp.fin.entity.FiscalYear;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for ENT-FIN-007 (FiscalYear) and the period set REQ-FIN-031 generates with it —
 * build-create-mapper, no MapStruct. Serves API-FIN-023.
 *
 * <p>{@code statusCode} and {@code isActiveFl} are not mapped from the request: the entity's own
 * {@code @Builder.Default}/{@code @PrePersist} declare {@code OPEN} and active, matching the
 * db-script's column defaults. There is no {@code updateEntityFromRequest}: FIN's API registry
 * declares no update endpoint for a fiscal year — its only state change is
 * {@code FiscalYear.close()}, driven by API-FIN-027.
 */
@Component
public class FiscalYearMapper {

    public FiscalYear toEntity(FiscalYearCreateRequest request) {
        if (request == null) {
            return null;
        }
        return FiscalYear.builder()
            .code(request.getCode())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .build();
    }

    /**
     * QR-FIN-038 — one generated period of the year, built against its already-resolved parent
     * (A.4.2) and left in the {@code OPEN} state its entity default declares ("each initially
     * Open", AC-FIN-031).
     *
     * <p><b>Spans and names</b> ({@code NAME_AR}/{@code NAME_EN}, both {@code NOT NULL}) come from
     * {@code FiscalPeriodDomain.generatedPeriod(...)}, which owns the convention SVC-API-INT.md's
     * API-FIN-023 block now states: twelve periods over a whole year are the twelve calendar
     * months, named from the JDK's own CLDR month names in Arabic and English; any other period
     * count falls back to the even day split and "الفترة N" / "Period N". The mapper only builds
     * the row from what that derivation returns.
     */
    public FiscalPeriod toPeriodEntity(FiscalYear fiscalYear, int periodNo, int periodCount) {
        if (fiscalYear == null) {
            return null;
        }
        GeneratedPeriod span = FiscalPeriodDomain.generatedPeriod(
            fiscalYear.getStartDate(), fiscalYear.getEndDate(), periodNo, periodCount);

        return FiscalPeriod.builder()
            .fiscalYear(fiscalYear)
            .periodNo(periodNo)
            .nameAr(span.nameAr())
            .nameEn(span.nameEn())
            .startDate(span.startDate())
            .endDate(span.endDate())
            .build();
    }

    /**
     * {@code periods} is passed in rather than read off the entity: the response is crafted
     * straight after the save, where the just-written list is authoritative and the
     * {@code @Formula} {@code periodCount} has not been recomputed by a re-read (A.1.19).
     */
    public FiscalYearResponse toResponse(FiscalYear entity, List<FiscalPeriodResponse> periods) {
        if (entity == null) {
            return null;
        }
        List<FiscalPeriodResponse> safePeriods = periods == null ? List.of() : periods;
        return FiscalYearResponse.builder()
            .fiscalYearPk(entity.getFiscalYearPk())
            .code(entity.getCode())
            .startDate(entity.getStartDate())
            .endDate(entity.getEndDate())
            .statusCode(entity.getStatusCode())
            .isActiveFl(Boolean.TRUE.equals(entity.getIsActiveFl()))
            .periodCount(safePeriods.size())
            .periods(safePeriods)
            .createdAt(entity.getCreatedAt())
            .createdBy(entity.getCreatedBy())
            .updatedAt(entity.getUpdatedAt())
            .updatedBy(entity.getUpdatedBy())
            .build();
    }

    /**
     * The search row: the year's own columns and its {@code @Formula} period count, with no nested
     * period set. {@link #toResponse(FiscalYear, List)}'s count is the size of the list it was
     * handed, which is correct straight after a create and wrong for a row read back later; here
     * the entity's own formula is authoritative and no lazy collection is walked (A.1.19).
     *
     * <p>{@code periods} is left empty rather than populated per row: the period set of every year
     * on the page is a second, unbounded read, and SCR-FIN-007's Detail pane already fetches it
     * through the fiscal-period search scoped by {@code fiscalYearId} — the same division
     * {@code JournalEntryMapper.toSummaryResponse} makes between a list row and API-FIN-022.
     */
    public FiscalYearResponse toSummaryResponse(FiscalYear entity) {
        if (entity == null) {
            return null;
        }
        FiscalYearResponse response = toResponse(entity, List.of());
        response.setPeriodCount(entity.getPeriodCount() != null ? entity.getPeriodCount() : 0);
        return response;
    }
}
