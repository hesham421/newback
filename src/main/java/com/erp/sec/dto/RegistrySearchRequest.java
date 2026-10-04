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
 * API-SEC-021 search body. The paged root is the module, so a {@code pageCode} filter — a column of
 * the child screen — is lifted out of the generic set and applied by the service as an EXISTS
 * subquery. No {@code @AllArgsConstructor} — {@code pageCode} is the only field the class adds, and
 * a one-arg constructor would be a confusing companion to the no-arg one.
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
@Schema(description = "Search request for the module/screen/action registry - طلب بحث سجل الوحدات")
public class RegistrySearchRequest extends BaseSearchContractRequest {

    private static final String PAGE_CODE = "pageCode";

    @Schema(description = "Partial match on a screen page code - مطابقة جزئية لرمز الصفحة",
        example = "TST_SCREEN")
    private String pageCode;

    @Override
    public SearchRequest toCommonSearchRequest() {
        return toCommonSearchRequest(Set.of(PAGE_CODE));
    }

    /** The top-level field when set, else the {@code filters[]} spelling of the same thing. */
    public String getPageCode() {
        return pageCode != null && !pageCode.isBlank() ? pageCode : extractStringFilter(PAGE_CODE);
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
