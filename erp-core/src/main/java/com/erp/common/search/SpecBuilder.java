package com.erp.common.search;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.CommonErrorCodes;
import com.erp.common.exception.ErrorDetail;
import com.erp.common.exception.LocalizedException;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class SpecBuilder {

    private SpecBuilder() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    public static <T> Specification<T> build(SearchRequest searchRequest, SetAllowedFields allowedFields,
                                              FieldValueConverter valueConverter) {
        return (root, query, criteriaBuilder) -> {
            if (searchRequest == null || searchRequest.getFilters() == null || searchRequest.getFilters().isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            List<Predicate> predicates = new ArrayList<>();
            assertFieldsAllowed(searchRequest, allowedFields);
            for (SearchFilter filter : searchRequest.getFilters()) {
                if (!allowedFields.isAllowed(filter.getField())) {
                    continue;
                }
                Object value = valueConverter.convert(filter.getField(), filter.getValue());
                if (value == null) {
                    continue;
                }
                predicates.add(toPredicate(root, criteriaBuilder, filter.getField(), filter.getOperator(), value));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * A filter naming a field this search does not support is a client error, reported as a 400
     * naming every offending field.
     *
     * <p>Until 2026-09-19 such a filter was silently skipped, which made "the filter matched
     * everything" and "the filter was discarded" indistinguishable on the wire: the SEC frontend
     * E2E run found two searches (API-SEC-025 {@code username}, API-SEC-012 {@code name}) rendering
     * an unfiltered list that looked filtered, with no way for any client to detect it. Fields a
     * request handles outside the generic set must be lifted out before they reach here — that is
     * what {@code BaseSearchContractRequest.toCommonSearchRequest(Set)}'s exclude set is for.
     */
    private static void assertFieldsAllowed(SearchRequest searchRequest, SetAllowedFields allowedFields) {
        List<ErrorDetail> unsupported = searchRequest.getFilters().stream()
            .filter(f -> f != null && !allowedFields.isAllowed(f.getField()))
            .map(f -> ErrorDetail.ofField(f.getField(),
                CommonErrorCodes.UNSUPPORTED_FILTER_FIELD, f.getField()))
            .toList();
        if (!unsupported.isEmpty()) {
            throw LocalizedException.withDetails(
                Status.VALIDATION_ERROR, CommonErrorCodes.VALIDATION_ERROR, unsupported);
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Predicate toPredicate(Root<?> root, CriteriaBuilder cb, String field,
                                          SearchOperator operator, Object value) {
        return switch (operator) {
            case EQUALS -> cb.equal(root.get(field), value);
            case NOT_EQUALS -> cb.notEqual(root.get(field), value);
            case LIKE -> cb.like(cb.lower(root.get(field).as(String.class)),
                "%" + String.valueOf(value).toLowerCase() + "%");
            case GREATER_THAN -> cb.greaterThan(asComparablePath(root, field), (Comparable) value);
            case GREATER_THAN_OR_EQUAL -> cb.greaterThanOrEqualTo(asComparablePath(root, field), (Comparable) value);
            case LESS_THAN -> cb.lessThan(asComparablePath(root, field), (Comparable) value);
            case LESS_THAN_OR_EQUAL -> cb.lessThanOrEqualTo(asComparablePath(root, field), (Comparable) value);
            case IN -> root.get(field).in((List<?>) value);
        };
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Path<Comparable> asComparablePath(Root<?> root, String field) {
        return (Path<Comparable>) (Path) root.get(field);
    }
}
