package com.erp.cu.domain;

/**
 * Which rows a configuration CRUD call addresses (erp-core step 09, query parameter {@code scope}):
 * the platform defaults ({@code TENANT_ID IS NULL}) or the caller's tenant overrides.
 */
public enum SettingScope {

    /** Platform defaults — visible to every tenant unless it overrides the key. PLATFORM tenant only,
     *  {@code PLATFORM_SETTINGS_MANAGE}. */
    PLATFORM,
    /** The caller's own tenant overrides (the default, and the only scope before step 09). */
    TENANT
}
