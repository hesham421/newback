package com.erp.cu.crossmodule;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.cu.exception.CuErrorCodes;

/**
 * Thrown by {@link SettingsApi#get} when a key has neither an active override of the current tenant nor
 * an active platform default: a {@link LocalizedException} with {@code SETTING_NOT_FOUND} (404) and the
 * key as argument, so it reaches a client localized if a caller lets it through.
 */
public class NoSuchSettingException extends LocalizedException {

    private final String key;

    public NoSuchSettingException(String key) {
        super(Status.NOT_FOUND, CuErrorCodes.SETTING_NOT_FOUND, key);
        this.key = key;
    }

    /** The (normalized, upper-case) key that is not set. */
    public String getKey() {
        return key;
    }
}
