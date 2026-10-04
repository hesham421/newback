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
 * API-FIN-005 — search body for ENT-FIN-002 (Dimension), QR-FIN-007. The plan's block names one
 * filter, {@code code} (LIKE); the rest of the contract is the inherited paging and sort.
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
@Schema(description = "Search request for analysis dimensions - طلب بحث في الأبعاد التحليلية")
public class DimensionSearchRequest extends BaseSearchContractRequest {

    @Override
    @Schema(description = "Filter criteria. Supported fields: code (LIKE), nameAr / nameEn (LIKE), "
        + "isActiveFl (EQUALS), dimensionPk (EQUALS, IN), createdAt (comparison operators). Any "
        + "other field, or an operator a field does not list, is rejected as 400 VALIDATION_ERROR "
        + "naming it - معايير التصفية")
    public List<SearchFilter> getFilters() {
        return super.getFilters();
    }

    @Override
    @Schema(description = "Sort field. Supported: dimensionPk, code, nameAr, nameEn, isActiveFl, "
        + "createdAt. Any other value is rejected as 400 FIN-400-INVALID-SORT - حقل الترتيب")
    public String getSortField() {
        return super.getSortField();
    }
}
