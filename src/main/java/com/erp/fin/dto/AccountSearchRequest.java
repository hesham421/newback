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
 * API-FIN-001 — search body for ENT-FIN-001 (Account), QR-FIN-001.
 *
 * <p>Filters the plan's API block names: {@code code} (LIKE), {@code nameAr}/{@code nameEn}
 * (LIKE), {@code accountTypeCode} (EXACT) and {@code isActiveFl} (EXACT). They travel in the
 * inherited {@code filters} list rather than as typed properties — the shared search contract
 * (CORE.md "Search contract": {@code {filters, page, size, sort}}) is the single input shape, and
 * {@code AccountService.ALLOWED_SORT_FIELDS} is what actually admits a field.
 *
 * <p>{@code parentAccountId} was added 2026-09-19. SCR-FIN-001 is a true parent/child hierarchy
 * and had no way to scope a level to its parent, so the tree could only be assembled by pulling
 * every account and grouping client-side. It is a foreign key mapped as the self-reference
 * {@code parentAccount} (DBF-FIN-007), so it is a nested {@code parentAccount.accountPk} path that
 * {@code SpecBuilder}'s flat {@code root.get(field)} cannot express: it is lifted out of the
 * generic specification here and ANDed in by the service as an explicit {@code Specification}
 * join, the same mechanism {@link AllocationRuleSearchRequest#getSourceAccountId()} uses.
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
@Schema(description = "Search request for chart-of-accounts accounts - طلب بحث في دليل الحسابات")
public class AccountSearchRequest extends BaseSearchContractRequest {

    /** The filter field carrying the parent account id (DBF-FIN-007, FK_ACCOUNT_PARENT). */
    public static final String PARENT_ACCOUNT_ID_FILTER = "parentAccountId";

    @Override
    public SearchRequest toCommonSearchRequest() {
        return toCommonSearchRequest(Set.of(PARENT_ACCOUNT_ID_FILTER));
    }

    /**
     * The nested-path filter, or {@code null} when the caller sent none — an unscoped search over
     * every account, which is what API-FIN-001 has always answered and stays the default.
     */
    @Schema(hidden = true)
    public Long getParentAccountId() {
        return extractLongFilter(PARENT_ACCOUNT_ID_FILTER);
    }

    @Override
    @Schema(description = "Filter criteria. Supported fields: code (LIKE), nameAr / nameEn (LIKE), "
        + "accountTypeCode (EQUALS, IN - ACCOUNT_TYPE), natureCode (EQUALS - DEBIT_CREDIT), "
        + "parentAccountId (EQUALS), isLeafFl (EQUALS), isActiveFl (EQUALS), accountPk (EQUALS, "
        + "IN), createdAt (comparison operators). Any other field, or an operator a field does not "
        + "list, is rejected as 400 VALIDATION_ERROR naming it - معايير التصفية")
    public List<SearchFilter> getFilters() {
        return super.getFilters();
    }

    @Override
    @Schema(description = "Sort field. Supported: accountPk, code, nameAr, nameEn, "
        + "accountTypeCode, natureCode, isLeafFl, isActiveFl, createdAt. Any other value is "
        + "rejected as 400 FIN-400-INVALID-SORT - حقل الترتيب")
    public String getSortField() {
        return super.getSortField();
    }
}
