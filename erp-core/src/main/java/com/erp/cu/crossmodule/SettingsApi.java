package com.erp.cu.crossmodule;

import java.util.Optional;

/**
 * Typed, cached, tenant-aware read access to the CU settings ({@code CU_APP_CONFIGURATION}, erp-core
 * step 09) — CU's public surface for every other module and application.
 *
 * <p><b>Resolution</b> for the current tenant ({@code com.erp.tenant.TenantContext}): the tenant's active
 * override, otherwise the active platform default (a row with no tenant), otherwise
 * {@link NoSuchSettingException} ({@code SETTING_NOT_FOUND}). Keys are case-insensitive (stored upper case).
 *
 * <p><b>Types</b>: {@code String}, {@code Integer}, {@code Long}, {@code Boolean}, {@code BigDecimal},
 * {@code Duration}; a value that does not parse as the requested type, or any other type, fails with
 * {@code SETTING_TYPE_MISMATCH} — also from {@code find}/{@code getOrDefault}: a default covers a
 * <em>missing</em> setting, never a malformed one.
 *
 * <p><b>Cache</b>: Spring cache {@value #CACHE_NAME}, key {@code <tenantId>:<KEY>}, evicted by every write
 * through the configuration CRUD API. Works with the {@code simple} and the Redis cache types (the cached
 * value is the raw text, or {@code null} for "absent").
 */
public interface SettingsApi {

    /** Spring cache holding resolved values, keyed {@code <tenantId>:<KEY>}. */
    String CACHE_NAME = "erpCoreSettings";

    /** The value of {@code key}; {@link NoSuchSettingException} when it is not set. */
    String get(String key);

    /** The value of {@code key} as {@code type}; {@link NoSuchSettingException} when it is not set. */
    <T> T get(String key, Class<T> type);

    /** The value of {@code key}, if it is set. */
    Optional<String> find(String key);

    /** The value of {@code key} as {@code type}, if it is set. */
    <T> Optional<T> find(String key, Class<T> type);

    /** The value of {@code key}, or {@code defaultValue} when it is not set. */
    String getOrDefault(String key, String defaultValue);

    /** The value of {@code key} as {@code type}, or {@code defaultValue} when it is not set. */
    <T> T getOrDefault(String key, Class<T> type, T defaultValue);
}
