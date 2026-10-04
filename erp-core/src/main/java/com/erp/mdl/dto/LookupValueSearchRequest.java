package com.erp.mdl.dto;

import com.erp.common.dto.BaseSearchContractRequest;
import com.erp.common.search.SearchRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Set;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * API-MDL-005 — search body for ENT-MDL-002 (LookupValue). This is the CHILD variant of
 * build-create-dto's SearchRequest template (A.3.10 / A.3.11): the parent id travels inside the
 * body's {@code filters} list under {@value #PARENT_ID_FILTER} and is read back through
 * {@link #getLookupTypeId()}, never as a path variable — the same mechanism
 * {@code DimensionValueSearchRequest} and {@code FiscalPeriodSearchRequest} already use.
 *
 * <p>{@link #toCommonSearchRequest()} excludes the parent filter from the generic specification
 * because {@code SpecBuilder} resolves a field as a flat {@code root.get(field)} and the parent
 * scope is a nested {@code lookupType.lookupTypePk} path; the service ANDs it in as an explicit
 * {@code Specification} join instead. {@code code} (LIKE) is the one remaining filter and flows
 * through the generic set untouched.
 *
 * <p><b>Forced by the language.</b> The class declares no instance field of its own (every filter
 * travels in the inherited {@code filters} list), so {@code @AllArgsConstructor} would generate a
 * second no-argument constructor and the class would not compile. {@code @NoArgsConstructor} is
 * the one Jackson needs to deserialize the body, so {@code @AllArgsConstructor} is the one
 * omitted; {@code @SuperBuilder} (not {@code @Builder}) is required in its place because the
 * superclass uses it.
 */
@Data
@NoArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
@Schema(description = "Search request for lookup values of a type - طلب بحث في قيم نوع لوكب")
public class LookupValueSearchRequest extends BaseSearchContractRequest {

    /** The filter field carrying the parent lookup-type id. */
    public static final String PARENT_ID_FILTER = "lookupTypeId";

    @Override
    public SearchRequest toCommonSearchRequest() {
        return toCommonSearchRequest(Set.of(PARENT_ID_FILTER));
    }

    @Schema(hidden = true)
    public Long getLookupTypeId() {
        return extractLongFilter(PARENT_ID_FILTER);
    }
}
