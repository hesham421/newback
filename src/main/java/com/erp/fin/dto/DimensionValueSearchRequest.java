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
 * API-FIN-008 — search body for ENT-FIN-003 (DimensionValue), QR-FIN-011. This is the CHILD
 * variant of build-create-dto's SearchRequest template (A.3.10 / A.3.11): the parent id travels
 * inside the body's {@code filters} list under {@value #PARENT_ID_FILTER} and is read back through
 * {@link #getDimensionId()}, never as a path variable — the plan's own Request line says so
 * ("carried in the body filters and read by the child parent-id extractor (never a path
 * variable)").
 *
 * <p>{@link #toCommonSearchRequest()} excludes that filter from the generic specification because
 * {@code SpecBuilder} resolves a field as a flat {@code root.get(field)} and the parent scope is a
 * nested {@code dimension.dimensionPk} path; the service ANDs it in as an explicit
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
@Schema(description = "Search request for dimension values - طلب بحث في قيم البُعد التحليلي")
public class DimensionValueSearchRequest extends BaseSearchContractRequest {

    /** The filter field carrying the parent dimension id (DBF-FIN-024). */
    public static final String PARENT_ID_FILTER = "dimensionId";

    @Override
    public SearchRequest toCommonSearchRequest() {
        return toCommonSearchRequest(Set.of(PARENT_ID_FILTER));
    }

    /**
     * A.3.11 — the parent-id extractor. Returns {@code null} when the caller sent no
     * {@value #PARENT_ID_FILTER} filter; A.5.16's non-null requirement is enforced by the service,
     * which owns the catalog code ({@code FIN-404-DIMENSION}).
     */
    @Schema(hidden = true)
    public Long getDimensionId() {
        return extractLongFilter(PARENT_ID_FILTER);
    }

    @Override
    @Schema(description = "Filter criteria. Supported fields: dimensionId (EQUALS - the parent "
        + "scope), code (LIKE), nameAr / nameEn (LIKE), sortOrder (EQUALS, comparison operators), "
        + "isActiveFl (EQUALS), dimensionValuePk (EQUALS, IN), createdAt (comparison operators). "
        + "Any other field, or an operator a field does not list, is rejected as 400 "
        + "VALIDATION_ERROR naming it - معايير التصفية")
    public List<SearchFilter> getFilters() {
        return super.getFilters();
    }

    @Override
    @Schema(description = "Sort field. Supported: dimensionValuePk, code, nameAr, nameEn, "
        + "sortOrder, isActiveFl, createdAt. Any other value is rejected as 400 "
        + "FIN-400-INVALID-SORT - حقل الترتيب")
    public String getSortField() {
        return super.getSortField();
    }
}
