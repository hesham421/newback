package com.erp.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/** Calendar dates as UTC instants (date filters are whole UTC days). */
public final class UtcDates {

    private UtcDates() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** Midnight UTC at the start of {@code date}. */
    public static Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
