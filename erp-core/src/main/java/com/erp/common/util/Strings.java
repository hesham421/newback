package com.erp.common.util;

/** Small null-safe string helpers. */
public final class Strings {

    private Strings() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** {@code value} cut to at most {@code max} characters; {@code null} stays {@code null}. */
    public static String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }
}
