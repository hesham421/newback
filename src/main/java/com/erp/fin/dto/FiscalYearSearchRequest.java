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
 * Search body for ENT-FIN-007 (FiscalYear).
 *
 * <p>Added 2026-09-19, and it carries no API-FIN id yet — the factory assigns those. Until then
 * {@code FiscalYearResponse} was reachable only as the 201 of API-FIN-023, the moment of creation:
 * a year could be created and then never listed or read. SCR-FIN-007's Master pane, SCR-FIN-010's
 * and SCR-FIN-011's required {@code fiscalYearId} selectors all had no source, and the year ids
 * discoverable from {@code FiscalPeriodResponse.fiscalYearId} are bare integers with not one
 * displayable attribute attached. srs-fin.md §B5 recorded the gap; it turned out to be
 * load-bearing.
 *
 * <p>Nothing about the surrounding search contract changes: same
 * {@code filters[] / sortField / sortDirection / page / size} envelope, same
 * {@code FIN-400-INVALID-SORT}, same {@code ALLOWED_SORT_FIELDS} whitelist mechanism, and every
 * filter reaches {@code SpecBuilder} untouched — ENT-FIN-007's filterable columns are all flat, so
 * this class lifts nothing out the way {@link FiscalPeriodSearchRequest} lifts its parent id.
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
@Schema(description = "Search request for fiscal years - طلب بحث في السنوات المالية")
public class FiscalYearSearchRequest extends BaseSearchContractRequest {

    @Override
    @Schema(description = "Filter criteria. Supported fields: code (LIKE), statusCode (EQUALS, IN "
        + "- FISCAL_YEAR_STATUS), startDate / endDate (EQUALS, GREATER_THAN_OR_EQUAL, "
        + "LESS_THAN_OR_EQUAL - ISO yyyy-MM-dd), isActiveFl (EQUALS), fiscalYearPk (EQUALS, IN), "
        + "createdAt (EQUALS, GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL). Any other field, or an "
        + "operator a field does not list, is rejected as 400 VALIDATION_ERROR naming it - معايير "
        + "التصفية")
    public List<SearchFilter> getFilters() {
        return super.getFilters();
    }

    @Override
    @Schema(description = "Sort field. Supported: fiscalYearPk, code, startDate, endDate, "
        + "statusCode, isActiveFl, createdAt. Any other value is rejected as 400 "
        + "FIN-400-INVALID-SORT - حقل الترتيب")
    public String getSortField() {
        return super.getSortField();
    }
}
