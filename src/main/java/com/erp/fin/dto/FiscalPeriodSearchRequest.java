package com.erp.fin.dto;

import com.erp.common.dto.BaseSearchContractRequest;
import com.erp.common.search.SearchFilter;
import com.erp.common.search.SearchRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * API-FIN-033 — search body for ENT-FIN-008 (FiscalPeriod). This is the CHILD variant of
 * build-create-dto's SearchRequest template (A.3.10 / A.3.11): the parent id travels inside the
 * body's {@code filters} list under {@value #PARENT_ID_FILTER} and is read back through
 * {@link #getFiscalYearId()}, never as a path variable — the same mechanism
 * {@link DimensionValueSearchRequest} and {@link JournalEntrySearchRequest} already use.
 *
 * <p>srs-fin.md SCR-REQ-FIN-007 §B2 names both filters of this screen:
 * {@code fiscalYearId(EXACT)} and {@code statusCode(EXACT)}. Neither is declared mandatory, so
 * {@link #getFiscalYearId()} returning {@code null} is a legitimate "all periods" request, not an
 * error — see {@code FiscalPeriodService.search} for why that matters here.
 *
 * <p>{@link #toCommonSearchRequest()} excludes the parent filter from the generic specification
 * because {@code SpecBuilder} resolves a field as a flat {@code root.get(field)} and the parent
 * scope is a nested {@code fiscalYear.fiscalYearPk} path; the service ANDs it in as an explicit
 * {@code Specification} join instead (A.5.17).
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
@Schema(description = "Search request for fiscal periods - طلب بحث في الفترات المحاسبية")
public class FiscalPeriodSearchRequest extends BaseSearchContractRequest {

    /** The filter field carrying the parent fiscal-year id (DBF-FIN-076). */
    public static final String PARENT_ID_FILTER = "fiscalYearId";

    @Override
    public SearchRequest toCommonSearchRequest() {
        return toCommonSearchRequest(Set.of(PARENT_ID_FILTER));
    }

    /**
     * A.3.11 — the parent-id extractor. Returns {@code null} when the caller sent no
     * {@value #PARENT_ID_FILTER} filter, which is a supported request here: §B2 makes the filter
     * EXACT but not mandatory, so the service applies the parent predicate only when it is
     * present.
     */
    @Schema(hidden = true)
    public Long getFiscalYearId() {
        return extractLongFilter(PARENT_ID_FILTER);
    }

    @Override
    @Schema(description = "Filter criteria. Supported fields: fiscalYearId (EQUALS - the parent "
        + "scope), statusCode (EQUALS, IN - PERIOD_STATE), periodNo (EQUALS, comparison "
        + "operators), nameAr / nameEn (LIKE), startDate / endDate (EQUALS, GREATER_THAN_OR_EQUAL, "
        + "LESS_THAN_OR_EQUAL - ISO yyyy-MM-dd), fiscalPeriodPk (EQUALS, IN), createdAt "
        + "(comparison operators). Any other field, or an operator a field does not list, is "
        + "rejected as 400 VALIDATION_ERROR naming it - معايير التصفية")
    public List<SearchFilter> getFilters() {
        return super.getFilters();
    }

    @Override
    @Schema(description = "Sort field. Supported: fiscalPeriodPk, periodNo, nameAr, nameEn, "
        + "startDate, endDate, statusCode, createdAt. Any other value is rejected as 400 "
        + "FIN-400-INVALID-SORT - حقل الترتيب")
    public String getSortField() {
        return super.getSortField();
    }
}
