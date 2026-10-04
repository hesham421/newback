package com.erp.sequence.domain;

import java.time.LocalDate;

/**
 * When a number series starts again at 1 ({@code CORE_NUMBER_SERIES.RESET_POLICY}, erp-core step 09).
 * Each policy maps a date to the period key of the row that numbers are drawn from.
 */
public enum ResetPolicy {

    /** One period for ever: period key {@code ''}. */
    NEVER,
    /** One period per calendar year: period key {@code YYYY}. */
    YEARLY,
    /** One period per calendar month: period key {@code YYYY-MM}. */
    MONTHLY;

    /** The period key of {@code date} under this policy. */
    public String periodKey(LocalDate date) {
        return switch (this) {
            case NEVER -> "";
            case YEARLY -> String.format("%04d", date.getYear());
            case MONTHLY -> String.format("%04d-%02d", date.getYear(), date.getMonthValue());
        };
    }
}
