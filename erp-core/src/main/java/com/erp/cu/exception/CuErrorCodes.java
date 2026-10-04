package com.erp.cu.exception;

/**
 * Module-specific error codes for Common Utils (CU).
 * Registered per gov-enforce-error-handling's descriptive &lt;ENTITY&gt;_&lt;SCENARIO&gt; format
 * (never mixed with the numbered ERR-xxxx form, which is a governance tracking id only —
 * see backend-execution/_SECTIONS.md SECTION A for the ERR-ID cross-reference).
 */
public final class CuErrorCodes {

    private CuErrorCodes() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** RULE-CU-001 (ERR-0001) — duplicate configKey on create. */
    public static final String APP_CONFIGURATION_KEY_DUPLICATE = "APP_CONFIGURATION_KEY_DUPLICATE";

    /** RULE-CU-002 (ERR-0002) — configKey/configValue missing on create or update. */
    public static final String APP_CONFIGURATION_FIELDS_REQUIRED = "APP_CONFIGURATION_FIELDS_REQUIRED";

    /**
     * RULE-CU-003 (ERR-0003) — configKey change attempt after creation.
     * Registered for catalog completeness; unused by any throw site because enforcement is
     * structural — configKey is excluded from ConfigurationUpdateRequest at the DTO layer
     * (SVC-API sub), so there is no reachable code path to guard here.
     */
    public static final String APP_CONFIGURATION_KEY_IMMUTABLE = "APP_CONFIGURATION_KEY_IMMUTABLE";

    /**
     * PLATFORM-STD (ERR-0004, DRV-001) — configKey not found. Thrown by every find-by-key path:
     * getByKey, update, deactivate, and the internal getValue().
     */
    public static final String APP_CONFIGURATION_NOT_FOUND = "APP_CONFIGURATION_NOT_FOUND";
}
