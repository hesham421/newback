package com.erp.sec.dto;

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
 * API-SEC-012 search body. {@code name} is a field of its own rather than a generic filter because
 * it matches nameAr OR nameEn and {@code SearchOperator} has no OR; every other filter reaches
 * SpecBuilder untouched. No {@code @AllArgsConstructor} — {@code name} is the only field the class
 * adds, and a one-arg constructor would be a confusing companion to the no-arg one.
 *
 * <p>Until 2026-09-19 {@code name} existed only as a getter reading {@code filters[]}. Jackson
 * publishes a getter, so the API docs advertised a top-level {@code name} the deserializer then
 * silently dropped, and {@code {"name":"..."} } returned an unfiltered list that looked filtered.
 * It is now a real settable field; the {@code filters[]} spelling still works, and the top-level
 * value wins when both are present.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Search request for Role - طلب بحث الأدوار")
public class RoleSearchRequest extends BaseSearchContractRequest {

    private static final String NAME = "name";

    @Schema(description = "Partial match on role name, Arabic or English - مطابقة جزئية لاسم الدور",
        example = "Security administrator")
    private String name;

    @Override
    public SearchRequest toCommonSearchRequest() {
        return toCommonSearchRequest(Set.of(NAME));
    }

    /** The top-level field when set, else the {@code filters[]} spelling of the same thing. */
    public String getName() {
        return name != null && !name.isBlank() ? name : extractStringFilter(NAME);
    }

    private String extractStringFilter(String field) {
        List<SearchFilter> current = getFilters();
        if (current == null) {
            return null;
        }
        return current.stream()
            .filter(f -> f != null && field.equals(f.getField()) && f.getValue() != null)
            .map(f -> String.valueOf(f.getValue()))
            .findFirst()
            .orElse(null);
    }
}
