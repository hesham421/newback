package com.erp.common.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;

/**
 * The guard lines Domain objects share. Each Domain keeps its own rules and their order; only the
 * throw is shared.
 */
public final class DomainRules {

    private DomainRules() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** {@code ALREADY_EXISTS} (409) with {@code errorCode} and {@code args} when {@code alreadyTaken}. */
    public static void assertUnique(boolean alreadyTaken, String errorCode, Object... args) {
        if (alreadyTaken) {
            throw new LocalizedException(Status.ALREADY_EXISTS, errorCode, args);
        }
    }

    /** {@code VALIDATION_ERROR} (400) with {@code errorCode} and no arguments when any value is null or blank. */
    public static void assertNotBlank(String errorCode, String... values) {
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new LocalizedException(Status.VALIDATION_ERROR, errorCode);
            }
        }
    }
}
