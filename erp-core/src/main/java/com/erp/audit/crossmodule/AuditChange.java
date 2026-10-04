package com.erp.audit.crossmodule;

/**
 * One field-level change of an audit row — an element of the {@code CHANGES} JSON array, written as
 * {@code {"field": ..., "old": ..., "new": ...}} (erp-core step 10).
 *
 * @param field    the property name
 * @param oldValue the value before ({@code null} for a creation); a string, number or boolean
 * @param newValue the value after ({@code null} for a deletion); a string, number or boolean
 */
public record AuditChange(String field, Object oldValue, Object newValue) {
}
