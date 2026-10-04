package com.erp.sequence.exception;

/**
 * Error codes of the sequence module (erp-core step 09), descriptive {@code <ENTITY>_<SCENARIO>} form.
 * Every code has an EN ({@code messages.properties}) and an AR ({@code messages_ar.properties}) message.
 */
public final class SequenceErrorCodes {

    private SequenceErrorCodes() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** {@code NumberSeriesApi.next/preview}: no active series with this code in the current tenant. */
    public static final String SEQUENCE_NOT_CONFIGURED = "SEQUENCE_NOT_CONFIGURED";

    /** Save time: unknown token, malformed braces, not exactly one {@code {SEQ:n}}, or a pattern that
     *  would repeat numbers under its reset policy (YEARLY without a year token, MONTHLY without year+month). */
    public static final String SEQUENCE_PATTERN_INVALID = "SEQUENCE_PATTERN_INVALID";

    /** Admin API: no series row with this id in the current tenant. */
    public static final String NUMBER_SERIES_NOT_FOUND = "NUMBER_SERIES_NOT_FOUND";

    /** Admin API: a series with this code already exists in the current tenant. */
    public static final String NUMBER_SERIES_CODE_DUPLICATE = "NUMBER_SERIES_CODE_DUPLICATE";
}
