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
 * API-FIN-038 — search body for ENT-FIN-015 (AccountMapping, v2), QR-FIN-050. The SRS §B2
 * filters travel in the inherited {@code filters} list; {@code accountId} is the nested path
 * {@code account.accountPk} the flat {@code SpecBuilder} cannot express, so it is lifted out here
 * and ANDed in by the service, exactly as {@link AccountSearchRequest#getParentAccountId()}.
 * A.3.1 deviation as on that class: no own field, so {@code @AllArgsConstructor} would duplicate
 * the no-arg constructor; {@code @SuperBuilder} because the base uses it.
 */
@Data
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Search request for account mappings - طلب بحث في روابط الحسابات")
public class AccountMappingSearchRequest extends BaseSearchContractRequest {

    /** The filter field carrying the mapped account id (DBF-FIN-153, FK_ACCOUNT_MAPPING_ACCOUNT). */
    public static final String ACCOUNT_ID_FILTER = "accountId";

    @Override
    public SearchRequest toCommonSearchRequest() {
        return toCommonSearchRequest(Set.of(ACCOUNT_ID_FILTER));
    }

    /** The lifted account filter, or {@code null} when the caller sent none (every filter optional). */
    @Schema(hidden = true)
    public Long getAccountId() {
        return extractLongFilter(ACCOUNT_ID_FILTER);
    }

    @Override
    @Schema(description = "Filter criteria. Supported fields: eventTypeCode (EQUALS, IN - "
        + "ACCOUNTING_EVENT_TYPE), businessFieldCode (EQUALS, IN - FIN_EVENT_BUSINESS_FIELD), "
        + "businessValue (LIKE), accountId (EQUALS), isActiveFl (EQUALS), accountMappingPk "
        + "(EQUALS, IN), createdAt (comparison operators). Any other field, or an operator a field "
        + "does not list, is rejected as 400 VALIDATION_ERROR naming it - معايير التصفية")
    public List<SearchFilter> getFilters() {
        return super.getFilters();
    }

    @Override
    @Schema(description = "Sort field. Supported: accountMappingPk, eventTypeCode, "
        + "businessFieldCode, businessValue, isActiveFl, createdAt. Any other value is rejected "
        + "as 400 FIN-400-INVALID-SORT - حقل الترتيب")
    public String getSortField() {
        return super.getSortField();
    }
}
