package com.erp.sec.service;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.CommonErrorCodes;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.FieldValueConverter;
import com.erp.common.search.SearchFilter;
import com.erp.common.search.SearchOperator;
import com.erp.sec.exception.SecErrorCodes;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;

/**
 * The two things SEC's searches need that the shared search layer does not give them: a sort-field
 * check that signals instead of silently dropping, and ISO-8601 to {@link Instant} coercion for a
 * filter value that arrives as a JSON string. {@code addFilter} serves the one remaining endpoint
 * that still builds its filters from query parameters (API-SEC-024, the CSV export).
 */
final class SecSearchSupport {

    private SecSearchSupport() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** Skips a null or blank parameter, so an absent query param never narrows the result set. */
    static void addFilter(List<SearchFilter> filters, String field,
                          SearchOperator operator, Object value) {
        if (value == null || (value instanceof String text && text.isBlank())) {
            return;
        }
        filters.add(SearchFilter.builder().field(field).operator(operator).value(value).build());
    }

    /**
     * {@code PageableBuilder} silently drops an unknown sort field instead of signalling, so the
     * whitelist check that raises SEC-400-INVALID-SORT has to happen before it is reached.
     */
    static void assertSortAllowed(String sortField, Set<String> allowedSortFields) {
        if (sortField == null || sortField.isBlank()) {
            return;
        }
        if (!allowedSortFields.contains(sortField)) {
            throw new LocalizedException(
                Status.VALIDATION_ERROR, SecErrorCodes.SEC_400_INVALID_SORT, sortField);
        }
    }

    /**
     * A JSON body carries a timestamp as a string; without this the criteria build would compare a
     * String to an {@code Instant} column. A malformed value is client input, so it is a 400.
     */
    static FieldValueConverter instantFieldConverter(Set<String> instantFields) {
        return (field, rawValue) -> {
            if (rawValue == null || rawValue instanceof Instant || !instantFields.contains(field)) {
                return rawValue;
            }
            try {
                return Instant.parse(String.valueOf(rawValue).trim());
            } catch (DateTimeParseException e) {
                throw new LocalizedException(
                    Status.VALIDATION_ERROR, CommonErrorCodes.VALIDATION_ERROR);
            }
        };
    }
}
