package com.erp.common.search;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.CommonErrorCodes;
import com.erp.common.exception.LocalizedException;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Set;

/**
 * Converts the filter values of the listed fields from an ISO-8601 string to an {@link Instant}.
 * A JSON body carries a timestamp as a string; without this the criteria build would compare a
 * String to an {@code Instant} column. A malformed value is client input, so it is a 400
 * ({@code VALIDATION_ERROR}). Other fields, {@code null} and values that are already an
 * {@code Instant} pass through unchanged.
 */
public final class InstantFieldValueConverter implements FieldValueConverter {

    private final Set<String> instantFields;

    public InstantFieldValueConverter(Set<String> instantFields) {
        this.instantFields = Set.copyOf(instantFields);
    }

    @Override
    public Object convert(String field, Object rawValue) {
        if (rawValue == null || rawValue instanceof Instant || !instantFields.contains(field)) {
            return rawValue;
        }
        try {
            return Instant.parse(String.valueOf(rawValue).trim());
        } catch (DateTimeParseException e) {
            throw new LocalizedException(Status.VALIDATION_ERROR, CommonErrorCodes.VALIDATION_ERROR);
        }
    }
}
