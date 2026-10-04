package com.erp.fin.service;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.CommonErrorCodes;
import com.erp.common.exception.LocalizedException;
import com.erp.common.search.FieldValueConverter;
import com.erp.fin.exception.FinErrorCodes;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;

/**
 * The two things FIN's searches (SVC-API-SEARCH) need that the shared search layer does not
 * give them. Both are deliberately thin helpers over the shared plumbing, never a replacement for
 * it: {@code SpecBuilder} and {@code PageableBuilder} still build every specification and every
 * pageable.
 *
 * <p>Same shape, and the same reasoning, as SEC's delivered {@code SecSearchSupport} — copied as a
 * FIN-local class rather than shared, because promoting it to {@code com.erp.common} would mean
 * modifying shared infrastructure and the error code it raises is module-specific by design
 * ({@code FIN-400-INVALID-SORT}, not SEC's).
 */
final class FinSearchSupport {

    private FinSearchSupport() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /**
     * {@code PageableBuilder.from(...)} silently drops a sort field that is not on the whitelist
     * and returns an unsorted page, so the caller would receive a differently ordered result than
     * it asked for with nothing saying so. The Error Catalog registers a code for exactly this
     * ({@code FIN-400-INVALID-SORT}, "every search API"), so the whitelist check is made before
     * the builder is reached.
     *
     * <p>A null or blank sort field is not an error — it means "no sort", the contract's default.
     */
    static void assertSortAllowed(String sortField, Set<String> allowedSortFields) {
        if (sortField == null || sortField.isBlank()) {
            return;
        }
        if (!allowedSortFields.contains(sortField)) {
            throw new LocalizedException(
                Status.VALIDATION_ERROR, FinErrorCodes.FIN_400_INVALID_SORT, sortField);
        }
    }


    /**
     * The audit timestamps every FIN entity inherits from {@code AuditableEntity}. They are
     * {@code Instant}, not {@code LocalDate}, and they are on every FIN search's
     * {@code ALLOWED_SORT_FIELDS} — so every FIN search admits them as filters too.
     */
    static final Set<String> AUDIT_INSTANT_FIELDS = Set.of("createdAt", "updatedAt");

    /**
     * One converter for both temporal shapes a FIN search can carry.
     *
     * <p>Added 2026-09-19, replacing a {@code localDateFieldConverter} that coerced the
     * {@code DATE} columns and nothing else. API-FIN-018's {@code docDate} (DBF-FIN-036) is the
     * filter that first needed one: a JSON body carries the bound as a string, and without the
     * coercion the criteria build compared a String to a {@code LocalDate} path. The same was
     * true of every {@code Instant} column and went unnoticed, so those reached the build as a
     * String and the comparison threw — a 500, not a 400, on a perfectly ordinary
     * {@code createdAt >= "..."} filter. The fault predates this method: {@code createdAt} has
     * been on every FIN whitelist since SVC-API-SEARCH, so the 500 was always reachable; it became
     * worth fixing rather than merely latent when each search started publishing the field as
     * filterable in its own api-docs. An api-doc that advertises a filter must not name one that
     * answers 500.
     *
     * @param dateFields    columns mapped as {@code LocalDate} (ISO {@code yyyy-MM-dd})
     * @param instantFields columns mapped as {@code Instant} (ISO-8601 instant, e.g.
     *                      {@code 2026-01-01T00:00:00Z})
     */
    static FieldValueConverter temporalFieldConverter(Set<String> dateFields,
                                                      Set<String> instantFields) {
        return (field, rawValue) -> {
            if (rawValue == null) {
                return rawValue;
            }
            if (dateFields.contains(field)) {
                return rawValue instanceof LocalDate ? rawValue : parseLocalDate(rawValue);
            }
            if (instantFields.contains(field)) {
                return rawValue instanceof Instant ? rawValue : parseInstant(rawValue);
            }
            return rawValue;
        };
    }

    /** The common case: a search whose only {@code Instant} columns are the audit pair. */
    static FieldValueConverter temporalFieldConverter(Set<String> dateFields) {
        return temporalFieldConverter(dateFields, AUDIT_INSTANT_FIELDS);
    }

    private static LocalDate parseLocalDate(Object rawValue) {
        try {
            return LocalDate.parse(String.valueOf(rawValue).trim());
        } catch (DateTimeParseException e) {
            throw new LocalizedException(Status.VALIDATION_ERROR, CommonErrorCodes.VALIDATION_ERROR);
        }
    }

    private static Instant parseInstant(Object rawValue) {
        try {
            return Instant.parse(String.valueOf(rawValue).trim());
        } catch (DateTimeParseException e) {
            throw new LocalizedException(Status.VALIDATION_ERROR, CommonErrorCodes.VALIDATION_ERROR);
        }
    }
}
