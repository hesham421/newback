package com.erp.cu.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.cu.entity.AppConfiguration;
import com.erp.cu.exception.CuErrorCodes;

/**
 * Domain companion for ENTITY-CU-001 (AppConfiguration). Owns every "is this operation
 * allowed?" decision for the entity — RULE-CU-001 (key uniqueness) and RULE-CU-002
 * (required fields on create/update). No Spring/JPA annotations, no repository access;
 * constructed only via the static factories below.
 *
 * RULE-CU-003 (config key immutability) is NOT enforced here — per DATA-DOM.md its
 * enforcement mechanism is structural: configKey is excluded from UpdateRequest at the
 * DTO layer (SVC-API sub), so there is no code path in this or any future phase that could
 * attempt to change configKey post-creation. A runtime guard method here would be
 * unreachable dead code.
 */
public final class AppConfigurationDomain {

    private final String configKey;
    private final boolean active;

    private AppConfigurationDomain(String configKey, boolean active) {
        this.configKey = configKey;
        this.active = active;
    }

    /**
     * Construction-time validation for create: RULE-CU-002 (required fields) then
     * RULE-CU-001 (key uniqueness, pre-checked by the service via QR-CU-0006).
     */
    public static AppConfigurationDomain create(String configKey, String configValue, boolean keyAlreadyTaken) {
        if (configKey == null || configKey.isBlank() || configValue == null || configValue.isBlank()) {
            throw new LocalizedException(Status.VALIDATION_ERROR, CuErrorCodes.APP_CONFIGURATION_FIELDS_REQUIRED);
        }
        if (keyAlreadyTaken) {
            throw new LocalizedException(Status.ALREADY_EXISTS, CuErrorCodes.APP_CONFIGURATION_KEY_DUPLICATE, configKey);
        }
        return new AppConfigurationDomain(configKey, true);
    }

    /** Reconstructs a Domain view over a persisted entity — no validation. */
    public static AppConfigurationDomain from(AppConfiguration entity) {
        return new AppConfigurationDomain(entity.getConfigKey(), Boolean.TRUE.equals(entity.getIsActive()));
    }

    /** RULE-CU-002 (required fields, UPDATE scope) — called before the service mutates the entity. */
    public void assertCanUpdate(String configValue) {
        if (configValue == null || configValue.isBlank()) {
            throw new LocalizedException(Status.VALIDATION_ERROR, CuErrorCodes.APP_CONFIGURATION_FIELDS_REQUIRED);
        }
    }

    public String getConfigKey() {
        return configKey;
    }

    public boolean isActive() {
        return active;
    }
}
