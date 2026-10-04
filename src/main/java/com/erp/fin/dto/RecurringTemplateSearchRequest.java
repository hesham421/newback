package com.erp.fin.dto;

import com.erp.common.dto.BaseSearchContractRequest;
import com.erp.common.search.SearchFilter;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * API-FIN-012 — search body for ENT-FIN-011 (RecurringTemplate), QR-FIN-017. Filters named by the
 * plan: {@code nameAr}/{@code nameEn} (LIKE), {@code scheduleTypeCode} (EXACT) and
 * {@code isActiveFl} (EXACT).
 *
 * <p><b>A.3.1 deviation, forced by the language.</b> The skill's DTO annotation set is
 * {@code @Data @Builder @NoArgsConstructor @AllArgsConstructor}; this class declares no instance
 * field of its own (every filter travels in the inherited {@code filters} list), so
 * {@code @AllArgsConstructor} would generate a second no-argument constructor and the class would
 * not compile. {@code @NoArgsConstructor} is the one Jackson needs to deserialize the body, so
 * {@code @AllArgsConstructor} is the one omitted; {@code @SuperBuilder} (not {@code @Builder})
 * is required in its place because the superclass uses it.
 */
@Data
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Search request for recurring templates - طلب بحث في القوالب المتكررة")
public class RecurringTemplateSearchRequest extends BaseSearchContractRequest {

    @Override
    @Schema(description = "Filter criteria. Supported fields: nameAr / nameEn (LIKE), "
        + "scheduleTypeCode (EQUALS - RECURRING_SCHEDULE_TYPE), frequencyCode (EQUALS - "
        + "RECURRING_FREQUENCY), startDate / nextRunDate / endDate (EQUALS, GREATER_THAN_OR_EQUAL, "
        + "LESS_THAN_OR_EQUAL - ISO yyyy-MM-dd), isActiveFl (EQUALS), recurringTemplatePk (EQUALS, "
        + "IN), createdAt (comparison operators). Any other field, or an operator a field does not "
        + "list, is rejected as 400 VALIDATION_ERROR naming it - معايير التصفية")
    public List<SearchFilter> getFilters() {
        return super.getFilters();
    }

    @Override
    @Schema(description = "Sort field. Supported: recurringTemplatePk, nameAr, nameEn, "
        + "scheduleTypeCode, frequencyCode, startDate, nextRunDate, endDate, isActiveFl, "
        + "createdAt. Any other value is rejected as 400 FIN-400-INVALID-SORT - حقل الترتيب")
    public String getSortField() {
        return super.getSortField();
    }
}
