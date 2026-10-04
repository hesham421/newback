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
 * API-FIN-018 — search body for ENT-FIN-004 (JournalEntry), QR-FIN-023. Filters named by the plan:
 * {@code docNo} (LIKE), {@code docDate} (DATE_RANGE — two filters using the shared
 * {@code GREATER_THAN_OR_EQUAL} / {@code LESS_THAN_OR_EQUAL} operators), {@code periodId} (EXACT),
 * {@code statusCode} (EXACT) and {@code journalTypeCode} (EXACT), plus {@code eventReference}
 * (EXACT, REQ-FIN-046's drill-down) and {@code fiscalYearId} (EXACT).
 *
 * <p>{@code periodId} (DBF-FIN-038) is a foreign key mapped as the association {@code period}, a
 * nested {@code period.fiscalPeriodPk} path, so it is excluded here and ANDed in by the service as
 * an explicit {@code Specification} join — same mechanism as
 * {@link AllocationRuleSearchRequest#getSourceAccountId()}.
 *
 * <p>{@code fiscalYearId} (DBF-FIN-037) was added 2026-09-19 and is the same kind of field: a
 * foreign key mapped as the association {@code fiscalYear}, lifted out and joined the same way.
 * It is the entry's own column, not a walk through {@code period.fiscalYear}, so filtering by year
 * and by period are independent — sending both narrows to their intersection.
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
@Schema(description = "Search request for journal entries - طلب بحث في قيود اليومية")
public class JournalEntrySearchRequest extends BaseSearchContractRequest {

    /** The filter field carrying the target fiscal period id (DBF-FIN-038). */
    public static final String PERIOD_ID_FILTER = "periodId";

    /** The filter field carrying the owning fiscal year id (DBF-FIN-037). */
    public static final String FISCAL_YEAR_ID_FILTER = "fiscalYearId";

    @Override
    public SearchRequest toCommonSearchRequest() {
        return toCommonSearchRequest(Set.of(PERIOD_ID_FILTER, FISCAL_YEAR_ID_FILTER));
    }

    /** The nested-path filter, or {@code null} when the caller sent none (it is optional here). */
    @Schema(hidden = true)
    public Long getPeriodId() {
        return extractLongFilter(PERIOD_ID_FILTER);
    }

    /** The nested-path filter, or {@code null} when the caller sent none (it is optional here). */
    @Schema(hidden = true)
    public Long getFiscalYearId() {
        return extractLongFilter(FISCAL_YEAR_ID_FILTER);
    }

    @Override
    @Schema(description = "Filter criteria. Supported fields: docNo (LIKE), docDate (EQUALS, "
        + "GREATER_THAN_OR_EQUAL, LESS_THAN_OR_EQUAL - ISO yyyy-MM-dd, send both bounds for a "
        + "range), periodId (EQUALS), fiscalYearId (EQUALS), statusCode (EQUALS, IN - "
        + "JOURNAL_STATUS), journalTypeCode (EQUALS, IN - JOURNAL_TYPE), eventReference (EQUALS), "
        + "journalEntryPk (EQUALS, IN), postedAt / createdAt (comparison operators). Any other "
        + "field, or an operator a field does not list, is rejected as 400 VALIDATION_ERROR naming "
        + "it - معايير التصفية")
    public List<SearchFilter> getFilters() {
        return super.getFilters();
    }

    @Override
    @Schema(description = "Sort field. Supported: journalEntryPk, docNo, docDate, journalTypeCode, "
        + "statusCode, eventReference, postedAt, createdAt. Any other value is rejected as 400 "
        + "FIN-400-INVALID-SORT - حقل الترتيب")
    public String getSortField() {
        return super.getSortField();
    }
}
