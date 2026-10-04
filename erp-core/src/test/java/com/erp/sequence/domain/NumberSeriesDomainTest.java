package com.erp.sequence.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.sequence.entity.NumberSeries;
import com.erp.sequence.exception.SequenceErrorCodes;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** erp-core step 09 — pattern rendering (table test), pattern validation, reset periods and series rules. */
class NumberSeriesDomainTest {

    private static final LocalDate DATE = LocalDate.of(2026, 3, 7);

    @ParameterizedTest(name = "[{index}] {0} -> {4}")
    @CsvSource(delimiter = '|', value = {
        "{PREFIX}-{YYYY}-{SEQ:6}       | INV   | 123       | ACME | INV-2026-000123",
        "{PREFIX}/{YY}/{MM}/{SEQ:4}    | SO    | 7         | ACME | SO/26/03/0007",
        "{TENANT}-{PREFIX}{SEQ:3}      | PO    | 42        | ACME | ACME-PO042",
        "{SEQ:1}                       |       | 5         |      | 5",
        "{SEQ:2}                       |       | 12345     |      | 12345",
        "R{YYYY}{MM}-{SEQ:5}           | X     | 99999     | T1   | R202603-99999",
        "{PREFIX}-{YYYY}-{SEQ:6}       |       | 1         |      | -2026-000001",
        "Doc #{SEQ:18}                 |       | 1         |      | Doc #000000000000000001",
    })
    void render_table(String pattern, String prefix, long sequence, String tenant, String expected) {
        assertThat(NumberPattern.parse(pattern.trim()).render(blankToNull(prefix), DATE, sequence, blankToNull(tenant)))
            .isEqualTo(expected.trim());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @ValueSource(strings = {
        "{PREFIX}-{YYYY}",              // no {SEQ:n}
        "{SEQ:3}-{SEQ:3}",              // two {SEQ:n}
        "{SEQ}",                        // SEQ without width
        "{SEQ:0}",                      // width 0
        "{SEQ:19}",                     // width > 18
        "{SEQ:123}",                    // three digits
        "{DAY}-{SEQ:4}",                // unknown token
        "{prefix}-{SEQ:4}",             // tokens are upper case
        "{PREFIX-{SEQ:4}",              // unmatched '{'
        "PREFIX}-{SEQ:4}",              // unmatched '}'
        "{SEQ:4",                       // unclosed
        "   ",                          // blank
    })
    void invalidPatterns_areRejected_withSequencePatternInvalid(String pattern) {
        assertThatThrownBy(() -> NumberPattern.parse(pattern))
            .isInstanceOfSatisfying(LocalizedException.class, e -> {
                assertThat(e.getErrorCode()).isEqualTo(SequenceErrorCodes.SEQUENCE_PATTERN_INVALID);
                assertThat(e.getStatus()).isEqualTo(Status.VALIDATION_ERROR);
            });
    }

    @Test
    void resetPolicy_periodKeys() {
        assertThat(ResetPolicy.NEVER.periodKey(DATE)).isEmpty();
        assertThat(ResetPolicy.YEARLY.periodKey(DATE)).isEqualTo("2026");
        assertThat(ResetPolicy.MONTHLY.periodKey(DATE)).isEqualTo("2026-03");
        assertThat(ResetPolicy.MONTHLY.periodKey(LocalDate.of(999, 12, 31))).isEqualTo("0999-12");
    }

    @Test
    void patternMustCarryThePeriod_underAResetPolicy() {
        NumberPattern noYear = NumberPattern.parse("{PREFIX}-{SEQ:6}");
        noYear.assertDistinctUnder(ResetPolicy.NEVER);
        assertThatThrownBy(() -> noYear.assertDistinctUnder(ResetPolicy.YEARLY)).isInstanceOf(LocalizedException.class);

        NumberPattern yearOnly = NumberPattern.parse("{PREFIX}-{YY}-{SEQ:6}");
        yearOnly.assertDistinctUnder(ResetPolicy.YEARLY);
        assertThatThrownBy(() -> yearOnly.assertDistinctUnder(ResetPolicy.MONTHLY)).isInstanceOf(LocalizedException.class);

        NumberPattern.parse("{YYYY}{MM}-{SEQ:6}").assertDistinctUnder(ResetPolicy.MONTHLY);
    }

    @Test
    void create_rejectsATakenCode_andAnInvalidPattern() {
        assertThatThrownBy(() -> NumberSeriesDomain.create("SALES_INVOICE", "INV", NumberSeries.DEFAULT_PATTERN,
                ResetPolicy.YEARLY, true))
            .isInstanceOfSatisfying(LocalizedException.class, e -> {
                assertThat(e.getErrorCode()).isEqualTo(SequenceErrorCodes.NUMBER_SERIES_CODE_DUPLICATE);
                assertThat(e.getStatus()).isEqualTo(Status.ALREADY_EXISTS);
            });
        assertThatThrownBy(() -> NumberSeriesDomain.create("X", null, "{PREFIX}-{SEQ:6}", ResetPolicy.MONTHLY, false))
            .isInstanceOfSatisfying(LocalizedException.class,
                e -> assertThat(e.getErrorCode()).isEqualTo(SequenceErrorCodes.SEQUENCE_PATTERN_INVALID));

        NumberSeriesDomain ok = NumberSeriesDomain.create("X", "INV", NumberSeries.DEFAULT_PATTERN, ResetPolicy.YEARLY, false);
        assertThat(ok.isActive()).isTrue();
    }

    @Test
    void anInactiveSeries_cannotAllocate_andCountsAsNotConfigured() {
        NumberSeries row = NumberSeries.builder().code("X").prefix("INV").pattern(NumberSeries.DEFAULT_PATTERN)
            .resetPolicy(ResetPolicy.YEARLY).isActive(false).build();

        assertThatThrownBy(() -> NumberSeriesDomain.from(row).assertCanAllocate())
            .isInstanceOfSatisfying(LocalizedException.class, e -> {
                assertThat(e.getErrorCode()).isEqualTo(SequenceErrorCodes.SEQUENCE_NOT_CONFIGURED);
                assertThat(e.getStatus()).isEqualTo(Status.BUSINESS_RULE_VIOLATION);
            });

        row.activate();
        NumberSeriesDomain active = NumberSeriesDomain.from(row);
        active.assertCanAllocate();
        assertThat(active.format(DATE, 9, null)).isEqualTo("INV-2026-000009");
        assertThat(active.needsTenantCode()).isFalse();
        assertThatThrownBy(() -> active.assertCanChangePattern("{PREFIX}-{SEQ:6}")).isInstanceOf(LocalizedException.class);
    }

    @Test
    void takeNextValue_handsOutTheCounterAndAdvancesIt() {
        NumberSeries row = NumberSeries.builder().code("X").nextValue(41L).build();
        assertThat(row.takeNextValue()).isEqualTo(41L);
        assertThat(row.takeNextValue()).isEqualTo(42L);
        assertThat(row.getNextValue()).isEqualTo(43L);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
