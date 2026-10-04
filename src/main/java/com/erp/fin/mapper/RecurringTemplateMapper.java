package com.erp.fin.mapper;

import com.erp.fin.dto.RecurringTemplateCreateRequest;
import com.erp.fin.dto.RecurringTemplateLineResponse;
import com.erp.fin.dto.RecurringTemplateResponse;
import com.erp.fin.entity.RecurringTemplate;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Manual mapper for ENT-FIN-011 (RecurringTemplate) — build-create-mapper, no MapStruct.
 *
 * <p>{@code nextRunDate} is seeded from {@code startDate} here, which is a plain field derivation
 * the spec states outright ("set nextRunDate=startDate", API-FIN-013 Orchestration), not a
 * business decision.
 *
 * <p>{@code toResponse} takes the already-mapped line responses rather than reading the entity's
 * lazy child collection — {@code RecurringTemplate} declares no {@code @OneToMany} at all, and
 * A.1.19 forbids walking one in any case, so the line set is assembled by the service and passed
 * in.
 */
@Component
public class RecurringTemplateMapper {

    /** RECURRING_FREQUENCY (SRS A6). */
    private static final String FREQUENCY_WEEKLY = "WEEKLY";

    /** RECURRING_FREQUENCY (SRS A6). */
    private static final String FREQUENCY_MONTHLY = "MONTHLY";

    /** RECURRING_FREQUENCY (SRS A6). */
    private static final String FREQUENCY_QUARTERLY = "QUARTERLY";

    /** RECURRING_FREQUENCY (SRS A6). */
    private static final String FREQUENCY_ANNUALLY = "ANNUALLY";

    public RecurringTemplate toEntity(RecurringTemplateCreateRequest request) {
        if (request == null) {
            return null;
        }
        return RecurringTemplate.builder()
            .nameAr(request.getNameAr())
            .nameEn(request.getNameEn())
            .scheduleTypeCode(request.getScheduleTypeCode())
            .frequencyCode(request.getFrequencyCode())
            .startDate(request.getStartDate())
            .nextRunDate(request.getStartDate())
            .endDate(request.getEndDate())
            .isActiveFl(Boolean.TRUE)
            .build();
    }

    /**
     * API-FIN-014 — "advance {@code nextRunDate} per {@code frequencyCode}" (REQ-FIN-023,
     * AC-FIN-023). A pure calendar step over the RECURRING_FREQUENCY value list (SRS A6:
     * {@code MONTHLY, QUARTERLY, ANNUALLY, WEEKLY}) — a field transformation, not a decision about
     * whether an operation is allowed, so it belongs on the mapper rather than in the service
     * body or a Domain class. ENT-FIN-011 carries no SRS RULE and has no Domain companion in
     * CORE.md's roster, so there is no guard to delegate to.
     *
     * <p>A reversing template has no frequency at all (ENT-FIN-011: "not applicable to a pure
     * reversing template", and the column is nullable, DBF-FIN-113); its run date is then left
     * where it is rather than advanced by an invented default.
     *
     * @param current       the template's current {@code nextRunDate}
     * @param frequencyCode DBF-FIN-113, or {@code null} for a reversing template
     */
    public LocalDate nextRunDateAfter(LocalDate current, String frequencyCode) {
        if (current == null || frequencyCode == null) {
            return current;
        }
        return switch (frequencyCode.toUpperCase(Locale.ROOT)) {
            case FREQUENCY_WEEKLY -> current.plusWeeks(1);
            case FREQUENCY_QUARTERLY -> current.plusMonths(3);
            case FREQUENCY_ANNUALLY -> current.plusYears(1);
            case FREQUENCY_MONTHLY -> current.plusMonths(1);
            default -> current;
        };
    }

    public RecurringTemplateResponse toResponse(RecurringTemplate entity,
                                                List<RecurringTemplateLineResponse> lines) {
        if (entity == null) {
            return null;
        }
        List<RecurringTemplateLineResponse> safeLines = lines == null ? List.of() : lines;
        return RecurringTemplateResponse.builder()
            .recurringTemplatePk(entity.getRecurringTemplatePk())
            .nameAr(entity.getNameAr())
            .nameEn(entity.getNameEn())
            .scheduleTypeCode(entity.getScheduleTypeCode())
            .frequencyCode(entity.getFrequencyCode())
            .startDate(entity.getStartDate())
            .nextRunDate(entity.getNextRunDate())
            .endDate(entity.getEndDate())
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
