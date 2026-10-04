package com.erp.report;

/**
 * One output column of a report result (erp-core step 11). {@link #key()} is the key of the column's
 * value in every row map; the labels are the CSV header (chosen by {@code Accept-Language}).
 *
 * @param key     row-map key
 * @param type    value type
 * @param labelAr Arabic header
 * @param labelEn English header
 */
public record ReportColumn(String key, ColumnType type, String labelAr, String labelEn) {
}
