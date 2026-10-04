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
 * API-SEC-005 search body. A {@code fullName} filter is lifted out of the generic set because it
 * matches fullNameAr OR fullNameEn and {@code SearchOperator} has no OR; every other filter reaches
 * SpecBuilder untouched. No {@code @AllArgsConstructor} — {@code fullName} is the only field the
 * class adds, and a one-arg constructor would be a confusing companion to the no-arg one.
 *
 * <p>Until 2026-09-19 it existed only as a getter reading {@code filters[]}. Jackson publishes a
 * getter, so the API docs advertised a top-level field the deserializer then silently dropped. It
 * is now a real settable field; the {@code filters[]} spelling still works, and the top-level
 * value wins when both are present.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Search request for User - طلب بحث المستخدمين")
public class UserSearchRequest extends BaseSearchContractRequest {

    private static final String FULL_NAME = "fullName";

    @Schema(description = "Partial match on the user full name, Arabic or English - مطابقة جزئية للاسم الكامل",
        example = "Ahmed")
    private String fullName;

    @Override
    public SearchRequest toCommonSearchRequest() {
        return toCommonSearchRequest(Set.of(FULL_NAME));
    }

    /** The top-level field when set, else the {@code filters[]} spelling of the same thing. */
    public String getFullName() {
        return fullName != null && !fullName.isBlank() ? fullName : extractStringFilter(FULL_NAME);
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
