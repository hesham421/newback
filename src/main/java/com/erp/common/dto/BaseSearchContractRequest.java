package com.erp.common.dto;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.CommonErrorCodes;
import com.erp.common.exception.LocalizedException;
import com.erp.common.exception.ErrorDetail;
import com.erp.common.search.SearchFilter;
import com.erp.common.search.SearchOperator;
import com.erp.common.search.SearchRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.springframework.data.domain.Sort;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Schema(description = "Base search/filter contract - العقد الأساسي للبحث والتصفية")
public class BaseSearchContractRequest {

    @Builder.Default
    @Schema(description = "Filter criteria - معايير التصفية")
    private List<SearchFilter> filters = List.of();

    @Schema(description = "Sort field - حقل الترتيب")
    private String sortField;

    @Builder.Default
    @Schema(description = "Sort direction - اتجاه الترتيب")
    private Sort.Direction sortDirection = Sort.Direction.ASC;

    @Builder.Default
    @Schema(description = "Page number, zero-based - رقم الصفحة", example = "0")
    private int page = 0;

    @Builder.Default
    @Schema(description = "Page size - حجم الصفحة", example = "20")
    private int size = 20;

    public SearchRequest toCommonSearchRequest() {
        return toCommonSearchRequest(Set.of());
    }

    public SearchRequest toCommonSearchRequest(Set<String> excludeFields) {
        List<SearchFilter> effectiveFilters = filters == null ? List.of() : filters.stream()
            .filter(f -> f != null && f.getField() != null && !excludeFields.contains(f.getField()))
            .toList();
        return SearchRequest.builder()
            .filters(effectiveFilters)
            .sortField(sortField)
            .sortDirection(sortDirection != null ? sortDirection : Sort.Direction.ASC)
            .page(page)
            .size(size)
            .build();
    }

    /**
     * The whole filter a request lifted out of the generic set, so the service can honour its
     * operator — not only its value. Needed by any field the shared {@code SpecBuilder} cannot
     * express (an association's column, an OR across two columns); see
     * {@code ActiveSessionSearchRequest}.
     */
    protected SearchFilter extractFilter(String field) {
        if (filters == null) {
            return null;
        }
        return filters.stream()
            .filter(f -> f != null && field.equals(f.getField()) && f.getValue() != null)
            .findFirst()
            .orElse(null);
    }

    protected Long extractLongFilter(String field) {
        if (filters == null) {
            return null;
        }
        SearchFilter match = filters.stream()
            .filter(f -> f != null && field.equals(f.getField()) && f.getValue() != null)
            .findFirst()
            .orElse(null);
        if (match == null) {
            return null;
        }
        assertScalarEqualsFilter(match);
        return toLong(match.getValue());
    }

    /**
     * A lifted id filter is scalar EQUALS and nothing else, and saying so out loud is the whole
     * point of this method.
     *
     * <p>Every field a request lifts out of the generic set — a parent scope, a foreign key — is
     * read by {@link #extractLongFilter(String)} for its VALUE, and the service then ANDs in its
     * own {@code cb.equal(...)} predicate. The operator the caller sent never reaches that
     * predicate. Until 2026-09-19 it was simply ignored, with two results that a client could not
     * tell apart from success: {@code NOT_EQUALS} produced the EQUALS result set — the exact
     * inverse of what was asked — and an {@code IN} list was skipped by the old scalar filter, so
     * the request came back unscoped, which on {@code parentAccountId} means the entire chart of
     * accounts rendered as one node's children. That is precisely the silent-drop class
     * {@code SpecBuilder.assertFieldsAllowed} was added to eliminate, so it is rejected the same
     * way and with the same registered code.
     *
     * <p>A null operator is EQUALS, matching {@code SpecBuilder}'s own default. Honouring more
     * operators here is possible — {@code SessionService.usernameMatches} does exactly that for
     * its lifted field — but it belongs in the service that owns the predicate, not in a value
     * extractor, and no FIN or SEC screen asks for a non-EXACT id filter today.
     */
    private static void assertScalarEqualsFilter(SearchFilter filter) {
        Object value = filter.getValue();
        boolean listValue = value instanceof Iterable<?> || value.getClass().isArray();
        SearchOperator operator = filter.getOperator() == null
            ? SearchOperator.EQUALS
            : filter.getOperator();
        if (!listValue && operator == SearchOperator.EQUALS) {
            return;
        }
        throw LocalizedException.withDetails(
            Status.VALIDATION_ERROR, CommonErrorCodes.VALIDATION_ERROR,
            List.of(ErrorDetail.ofField(filter.getField(),
                CommonErrorCodes.UNSUPPORTED_FILTER_OPERATOR,
                listValue ? SearchOperator.IN : operator, filter.getField())));
    }

    /**
     * Parses a scalar filter value to a Long. A malformed (non-numeric) value is a client input
     * error, so it surfaces as a 400 VALIDATION_ERROR rather than an unhandled 500 from a raw
     * NumberFormatException escaping through the specification build.
     */
    private static Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value).trim());
        } catch (NumberFormatException e) {
            throw new LocalizedException(Status.VALIDATION_ERROR, CommonErrorCodes.VALIDATION_ERROR);
        }
    }
}
