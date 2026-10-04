package com.erp.report;

/**
 * Type of a {@link ReportParam} (erp-core step 11). The report service validates every raw request value
 * against its declared type and hands the provider the converted Java value:
 * <ul>
 *   <li>{@link #STRING} → {@link String} (trimmed, never blank)</li>
 *   <li>{@link #INTEGER} → {@link Long}</li>
 *   <li>{@link #DECIMAL} → {@link java.math.BigDecimal}</li>
 *   <li>{@link #DATE} → {@link java.time.LocalDate} (ISO-8601 {@code yyyy-MM-dd})</li>
 *   <li>{@link #DATETIME} → {@link java.time.Instant} (ISO-8601 with {@code Z} or an offset; a value
 *       without zone is read as UTC)</li>
 *   <li>{@link #BOOLEAN} → {@link Boolean} ({@code true}/{@code false})</li>
 *   <li>{@link #LOOKUP} → {@link String}: the code of an active value of the MDL lookup type named by
 *       {@link ReportParam#lookupKey()}</li>
 * </ul>
 */
public enum ParamType {
    STRING,
    INTEGER,
    DECIMAL,
    DATE,
    DATETIME,
    BOOLEAN,
    LOOKUP
}
