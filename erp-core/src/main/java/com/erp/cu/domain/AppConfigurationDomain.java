package com.erp.cu.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.cu.entity.AppConfiguration;
import com.erp.cu.exception.CuErrorCodes;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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

    /**
     * erp-core step 09 — platform defaults ({@link SettingScope#PLATFORM}) may be read and written only
     * from the PLATFORM tenant (the caller's authority is checked by the service's {@code @PreAuthorize};
     * this is the tenant half of the rule).
     */
    public static void assertScopeAllowed(SettingScope scope, boolean callerIsPlatformTenant) {
        if (scope == SettingScope.PLATFORM && !callerIsPlatformTenant) {
            throw new LocalizedException(Status.FORBIDDEN, CuErrorCodes.SETTING_PLATFORM_SCOPE_FORBIDDEN);
        }
    }

    /**
     * erp-core step 09 — settings resolution for {@code tenantId}: its active override wins, then the
     * active platform default; a deactivated row counts as absent (soft delete). {@code candidates} are
     * the key's override and default rows ({@code findOverrideAndDefault}).
     */
    public static Optional<String> resolve(List<AppConfiguration> candidates, Long tenantId) {
        Optional<AppConfiguration> override = candidates.stream()
            .filter(c -> c.getTenantId() != null && Objects.equals(c.getTenantId(), tenantId))
            .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
            .findFirst();
        if (override.isPresent()) {
            return override.map(AppConfiguration::getConfigValue);
        }
        return candidates.stream()
            .filter(c -> c.getTenantId() == null)
            .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
            .findFirst()
            .map(AppConfiguration::getConfigValue);
    }

    public String getConfigKey() {
        return configKey;
    }

    public boolean isActive() {
        return active;
    }
}
