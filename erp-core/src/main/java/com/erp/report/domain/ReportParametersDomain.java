package com.erp.report.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.ErrorDetail;
import com.erp.common.exception.LocalizedException;
import com.erp.report.ParamType;
import com.erp.report.ReportParam;
import com.erp.report.exception.ReportErrorCodes;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Validates a report request's raw parameters against the report's declared {@link ReportParam}s and
 * converts them to the types {@link ParamType} documents (erp-core step 11). Plain class: no Spring,
 * no persistence; the lookup values a {@code LOOKUP} parameter is checked against are resolved by the
 * service (cross-module, MDL) and passed in.
 *
 * <p>Rules, each failure reported as {@code REPORT_PARAM_INVALID} for that parameter (all failures at
 * once, 400): a required parameter must be present (null and blank strings count as absent); a value
 * must convert to its type (dates ISO-8601); a {@code LOOKUP} value must be an active code of its type;
 * a parameter the report does not declare is rejected rather than silently ignored.
 */
public final class ReportParametersDomain {

    private final List<ReportParam> definitions;

    private ReportParametersDomain(List<ReportParam> definitions) {
        this.definitions = definitions == null ? List.of() : List.copyOf(definitions);
    }

    /** The parameter rules of one report. */
    public static ReportParametersDomain from(List<ReportParam> definitions) {
        return new ReportParametersDomain(definitions);
    }

    /**
     * Validates and converts {@code raw}.
     *
     * @param raw         request parameters (may be {@code null})
     * @param lookupCodes active codes of a lookup type key, or {@code null} when the type is unknown or
     *                    inactive
     * @return the converted parameters; absent optional parameters are not in the map
     * @throws LocalizedException {@code VALIDATION_ERROR} / {@code REPORT_PARAM_INVALID} listing every
     *                            failing parameter
     */
    public Map<String, Object> validate(Map<String, Object> raw, Function<String, Set<String>> lookupCodes) {
        Map<String, Object> input = raw == null ? Map.of() : raw;
        List<ErrorDetail> errors = new ArrayList<>();
        Map<String, Object> converted = new LinkedHashMap<>();

        Set<String> declared = new java.util.HashSet<>();
        for (ReportParam param : definitions) {
            declared.add(param.name());
            Object value = input.get(param.name());
            if (isAbsent(value)) {
                if (param.required()) {
                    errors.add(invalid(param.name()));
                }
                continue;
            }
            Object typed = convert(param, value, lookupCodes);
            if (typed == null) {
                errors.add(invalid(param.name()));
            } else {
                converted.put(param.name(), typed);
            }
        }
        input.keySet().stream().filter(name -> !declared.contains(name)).sorted()
            .forEach(name -> errors.add(invalid(name)));

        if (!errors.isEmpty()) {
            throw new LocalizedException(Status.VALIDATION_ERROR, errors);
        }
        return converted;
    }

    /** {@code REPORT_PARAM_INVALID} for one parameter (or the export {@code format}). */
    public static ErrorDetail invalid(String name) {
        return ErrorDetail.ofField(name, ReportErrorCodes.REPORT_PARAM_INVALID, name);
    }

    private static boolean isAbsent(Object value) {
        return value == null || (value instanceof String text && text.isBlank());
    }

    /** The converted value, or {@code null} when {@code value} is not valid for the parameter. */
    private static Object convert(ReportParam param, Object value, Function<String, Set<String>> lookupCodes) {
        return switch (param.type()) {
            case STRING -> scalarText(value);
            case INTEGER -> toLong(value);
            case DECIMAL -> toDecimal(value);
            case DATE -> toDate(value);
            case DATETIME -> toInstant(value);
            case BOOLEAN -> toBoolean(value);
            case LOOKUP -> toLookupCode(param, value, lookupCodes);
        };
    }

    private static String scalarText(Object value) {
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            String text = value.toString().trim();
            return text.isEmpty() ? null : text;
        }
        return null;
    }

    private static Long toLong(Object value) {
        try {
            if (value instanceof Integer || value instanceof Long || value instanceof Short || value instanceof Byte) {
                return ((Number) value).longValue();
            }
            if (value instanceof BigInteger big) {
                return big.longValueExact();
            }
            if (value instanceof Number number) {
                return new BigDecimal(number.toString()).longValueExact();
            }
            if (value instanceof String text) {
                return Long.parseLong(text.trim());
            }
        } catch (ArithmeticException | NumberFormatException e) {
            return null;
        }
        return null;
    }

    private static BigDecimal toDecimal(Object value) {
        try {
            if (value instanceof BigDecimal decimal) {
                return decimal;
            }
            if (value instanceof Number || value instanceof String) {
                return new BigDecimal(value.toString().trim());
            }
        } catch (NumberFormatException e) {
            return null;
        }
        return null;
    }

    private static LocalDate toDate(Object value) {
        if (!(value instanceof String text)) {
            return null;
        }
        try {
            return LocalDate.parse(text.trim());
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static Instant toInstant(Object value) {
        if (!(value instanceof String text)) {
            return null;
        }
        String trimmed = text.trim();
        try {
            return OffsetDateTime.parse(trimmed).toInstant();
        } catch (DateTimeParseException withoutOffset) {
            try {
                return LocalDateTime.parse(trimmed).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException e) {
                return null;
            }
        }
    }

    private static Boolean toBoolean(Object value) {
        if (value instanceof Boolean flag) {
            return flag;
        }
        if (value instanceof String text) {
            String normalized = text.trim().toLowerCase(java.util.Locale.ROOT);
            if (normalized.equals("true")) {
                return Boolean.TRUE;
            }
            if (normalized.equals("false")) {
                return Boolean.FALSE;
            }
        }
        return null;
    }

    private static String toLookupCode(ReportParam param, Object value, Function<String, Set<String>> lookupCodes) {
        String code = scalarText(value);
        if (code == null) {
            return null;
        }
        Set<String> active = lookupCodes == null ? null : lookupCodes.apply(param.lookupKey());
        return active != null && active.contains(code) ? code : null;
    }
}
