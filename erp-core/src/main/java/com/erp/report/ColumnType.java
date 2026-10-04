package com.erp.report;

/**
 * Type of a {@link ReportColumn} (erp-core step 11): how a client should render the column and how the
 * CSV writer formats its values. Row values are expected as {@link String} ({@link #STRING}), a
 * {@link Number} ({@link #INTEGER}, {@link #DECIMAL}), {@link java.time.LocalDate} ({@link #DATE}),
 * {@link java.time.Instant} / {@link java.time.OffsetDateTime} / {@link java.time.LocalDateTime}
 * ({@link #DATETIME}) and {@link Boolean} ({@link #BOOLEAN}); {@code null} is an empty cell.
 */
public enum ColumnType {
    STRING,
    INTEGER,
    DECIMAL,
    DATE,
    DATETIME,
    BOOLEAN
}
