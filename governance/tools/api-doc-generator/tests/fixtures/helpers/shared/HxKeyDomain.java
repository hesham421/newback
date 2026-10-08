package com.erp.common.idempotency;

public final class HxKeyDomain {

    private final String key;

    private HxKeyDomain(String key) {
        this.key = key;
    }

    public static HxKeyDomain from(String key) {
        return new HxKeyDomain(key);
    }

    public static void assertKeyValid(String key) {
        if (key.length() > 64) {
            throw new LocalizedException(Status.VALIDATION_ERROR, HxKeyErrorCodes.HX_KEY_INVALID);
        }
    }

    public void assertReplayableFor(Object request) {
        if (request == null) {
            throw new LocalizedException(Status.CONFLICT, HxKeyErrorCodes.HX_KEY_CONFLICT);
        }
    }
}
