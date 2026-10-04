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
 * API-FIN-015 — search body for ENT-FIN-013 (AllocationRule), QR-FIN-020. Filters named by the
 * plan: {@code nameAr}/{@code nameEn} (LIKE), {@code sourceAccountId} (EXACT) and
 * {@code isActiveFl} (EXACT).
 *
 * <p>{@code sourceAccountId} (DBF-FIN-133) is a foreign key mapped as the association
 * {@code sourceAccount}, so it is a nested {@code sourceAccount.accountPk} path that
 * {@code SpecBuilder}'s flat {@code root.get(field)} cannot express. It is therefore excluded from
 * the generic specification here and ANDed in by the service as an explicit {@code Specification}
 * join — the same mechanism A.5.17 mandates for a child search, applied to the one nested filter
 * this search declares.
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
@Schema(description = "Search request for allocation rules - طلب بحث في قواعد التوزيع")
public class AllocationRuleSearchRequest extends BaseSearchContractRequest {

    /** The filter field carrying the source account id (DBF-FIN-133). */
    public static final String SOURCE_ACCOUNT_ID_FILTER = "sourceAccountId";

    @Override
    public SearchRequest toCommonSearchRequest() {
        return toCommonSearchRequest(Set.of(SOURCE_ACCOUNT_ID_FILTER));
    }

    /** The nested-path filter, or {@code null} when the caller sent none (it is optional here). */
    @Schema(hidden = true)
    public Long getSourceAccountId() {
        return extractLongFilter(SOURCE_ACCOUNT_ID_FILTER);
    }

    @Override
    @Schema(description = "Filter criteria. Supported fields: nameAr / nameEn (LIKE), "
        + "sourceAccountId (EQUALS), isActiveFl (EQUALS), allocationRulePk (EQUALS, IN), createdAt "
        + "(comparison operators). Any other field, or an operator a field does not list, is "
        + "rejected as 400 VALIDATION_ERROR naming it - معايير التصفية")
    public List<SearchFilter> getFilters() {
        return super.getFilters();
    }

    @Override
    @Schema(description = "Sort field. Supported: allocationRulePk, nameAr, nameEn, isActiveFl, "
        + "createdAt. Any other value is rejected as 400 FIN-400-INVALID-SORT - حقل الترتيب")
    public String getSortField() {
        return super.getSortField();
    }
}
