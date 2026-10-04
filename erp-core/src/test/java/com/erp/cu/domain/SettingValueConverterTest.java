package com.erp.cu.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.cu.exception.CuErrorCodes;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** erp-core step 09 — typed reading of setting values, and SETTING_TYPE_MISMATCH. */
class SettingValueConverterTest {

    @Test
    void convertsEverySupportedType() {
        assertThat(SettingValueConverter.convert("K", " text ", String.class)).isEqualTo(" text ");
        assertThat(SettingValueConverter.convert("K", " 42 ", Integer.class)).isEqualTo(42);
        assertThat(SettingValueConverter.convert("K", "9000000000", Long.class)).isEqualTo(9_000_000_000L);
        assertThat(SettingValueConverter.convert("K", "TRUE", Boolean.class)).isTrue();
        assertThat(SettingValueConverter.convert("K", "false", Boolean.class)).isFalse();
        assertThat(SettingValueConverter.convert("K", "12.50", BigDecimal.class)).isEqualByComparingTo("12.5");
        assertThat(SettingValueConverter.convert("K", "PT30S", Duration.class)).isEqualTo(Duration.ofSeconds(30));
        assertThat(SettingValueConverter.convert("K", "P1D", Duration.class)).isEqualTo(Duration.ofDays(1));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @CsvSource({
        "1500, PT1.5S",
        "1500ms, PT1.5S",
        "30s, PT30S",
        "5m, PT5M",
        "2h, PT2H",
        "3d, PT72H",
        "10us, PT0.00001S",
        "-1s, PT-1S",
    })
    void readsSimpleDurations_likeSpringBootProperties(String raw, String iso) {
        assertThat(SettingValueConverter.convert("K", raw, Duration.class)).isEqualTo(Duration.parse(iso));
    }

    @ParameterizedTest(name = "[{index}] {0} as {1}")
    @CsvSource({
        "abc, Integer",
        "2147483648, Integer",
        "1.5, Long",
        "yes, Boolean",
        "1, Boolean",
        "'1,5', BigDecimal",
        "ten seconds, Duration",
        "5 weeks, Duration",
        "PT, Duration",
    })
    void malformedValues_areSettingTypeMismatch(String raw, String typeName) throws ClassNotFoundException {
        Class<?> type = switch (typeName) {
            case "Integer" -> Integer.class;
            case "Long" -> Long.class;
            case "Boolean" -> Boolean.class;
            case "BigDecimal" -> BigDecimal.class;
            default -> Duration.class;
        };
        assertThatThrownBy(() -> SettingValueConverter.convert("MY_KEY", raw, type))
            .isInstanceOfSatisfying(LocalizedException.class, e -> {
                assertThat(e.getErrorCode()).isEqualTo(CuErrorCodes.SETTING_TYPE_MISMATCH);
                assertThat(e.getStatus()).isEqualTo(Status.BUSINESS_RULE_VIOLATION);
                assertThat(e.getArgs()).containsExactly("MY_KEY", type.getSimpleName());
            });
    }

    @Test
    void unsupportedTypes_areSettingTypeMismatch() {
        assertThatThrownBy(() -> SettingValueConverter.convert("K", "2026-01-01", LocalDate.class))
            .isInstanceOfSatisfying(LocalizedException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(CuErrorCodes.SETTING_TYPE_MISMATCH));
        assertThatThrownBy(() -> SettingValueConverter.assertSupported("K", null))
            .isInstanceOf(LocalizedException.class);
    }
}
