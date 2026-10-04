package com.erp.cu.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.cu.exception.CuErrorCodes;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a stored setting value ({@code CONFIG_VALUE}, always text) as one of the types
 * {@code SettingsApi} supports (erp-core step 09): {@code String}, {@code Integer}, {@code Long},
 * {@code Boolean}, {@code BigDecimal}, {@code Duration}. A value that does not parse, or another type,
 * is {@code SETTING_TYPE_MISMATCH}. Pure: no Spring, no database.
 *
 * <ul>
 *   <li>Numbers: the trimmed text ({@code Integer.valueOf}, {@code Long.valueOf}, {@code new BigDecimal}).</li>
 *   <li>Boolean: {@code true} / {@code false}, case-insensitive; anything else is a mismatch.</li>
 *   <li>Duration: ISO-8601 ({@code PT30S}, {@code P1D}) or a number with an optional unit
 *       {@code ns|us|ms|s|m|h|d} ({@code 30s}, {@code 5m}; no unit = milliseconds), as Spring Boot reads
 *       durations in properties.</li>
 * </ul>
 */
public final class SettingValueConverter {

    /** The types {@code SettingsApi#get(String, Class)} accepts. */
    public static final Set<Class<?>> SUPPORTED_TYPES =
        Set.of(String.class, Integer.class, Long.class, Boolean.class, BigDecimal.class, Duration.class);

    private static final Pattern SIMPLE_DURATION = Pattern.compile("^([+-]?\\d+)(ns|us|ms|s|m|h|d)?$");

    private SettingValueConverter() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** Fails with {@code SETTING_TYPE_MISMATCH} unless {@code type} is one of {@link #SUPPORTED_TYPES}. */
    public static void assertSupported(String key, Class<?> type) {
        if (type == null || !SUPPORTED_TYPES.contains(type)) {
            throw mismatch(key, type);
        }
    }

    /** {@code raw} (the value of {@code key}) as {@code type}. */
    public static <T> T convert(String key, String raw, Class<T> type) {
        assertSupported(key, type);
        if (type == String.class) {
            return type.cast(raw);
        }
        String text = raw == null ? "" : raw.trim();
        try {
            Object value;
            if (type == Integer.class) {
                value = Integer.valueOf(text);
            } else if (type == Long.class) {
                value = Long.valueOf(text);
            } else if (type == BigDecimal.class) {
                value = new BigDecimal(text);
            } else if (type == Boolean.class) {
                value = parseBoolean(key, text);
            } else {
                value = parseDuration(key, text);
            }
            return type.cast(value);
        } catch (NumberFormatException | DateTimeParseException | ArithmeticException e) {
            throw mismatch(key, type);
        }
    }

    private static Boolean parseBoolean(String key, String text) {
        return switch (text.toLowerCase(Locale.ROOT)) {
            case "true" -> Boolean.TRUE;
            case "false" -> Boolean.FALSE;
            default -> throw mismatch(key, Boolean.class);
        };
    }

    private static Duration parseDuration(String key, String text) {
        if (text.startsWith("P") || text.startsWith("p") || text.startsWith("-P") || text.startsWith("-p")) {
            return Duration.parse(text);
        }
        Matcher matcher = SIMPLE_DURATION.matcher(text.toLowerCase(Locale.ROOT));
        if (!matcher.matches()) {
            throw mismatch(key, Duration.class);
        }
        long amount = Long.parseLong(matcher.group(1));
        String unit = matcher.group(2) == null ? "ms" : matcher.group(2);
        return switch (unit) {
            case "ns" -> Duration.ofNanos(amount);
            case "us" -> Duration.ofNanos(Math.multiplyExact(amount, 1000L));
            case "ms" -> Duration.ofMillis(amount);
            case "s" -> Duration.ofSeconds(amount);
            case "m" -> Duration.ofMinutes(amount);
            case "h" -> Duration.ofHours(amount);
            default -> Duration.ofDays(amount);
        };
    }

    private static LocalizedException mismatch(String key, Class<?> type) {
        return new LocalizedException(Status.BUSINESS_RULE_VIOLATION, CuErrorCodes.SETTING_TYPE_MISMATCH, key,
            type == null ? "null" : type.getSimpleName());
    }
}
