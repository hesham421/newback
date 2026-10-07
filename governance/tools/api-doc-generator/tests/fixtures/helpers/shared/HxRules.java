package com.erp.common.domain;

public final class HxRules {

    private HxRules() {
        throw new UnsupportedOperationException("utility");
    }

    public static void assertUnique(boolean alreadyTaken, String errorCode, Object... args) {
        if (alreadyTaken) {
            throw new LocalizedException(Status.ALREADY_EXISTS, errorCode, args);
        }
    }

    public static void assertNotBlank(String errorCode, String... values) {
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new LocalizedException(Status.VALIDATION_ERROR, errorCode);
            }
        }
    }
}
