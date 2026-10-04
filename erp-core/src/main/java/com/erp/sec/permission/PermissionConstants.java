package com.erp.sec.permission;

/**
 * Platform permission codes, shaped {@code PERM_<PAGE_CODE>_<ACTION>}
 * (profile.conventions.security_model.permission_pattern, srs-sec.md §1 DBF-SEC-050). Only the
 * codes an implemented API's {@code Security :} line names are declared; the permission matrix and
 * its seed data belong to the SEC-BE phase.
 */
public final class PermissionConstants {

    private PermissionConstants() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** API-SEC-006 — screen SEC_USERS. */
    public static final String PERM_SEC_USERS_CREATE = "PERM_SEC_USERS_CREATE";

    /** API-SEC-007, 008, 009, 010, 011 — screen SEC_USERS. */
    public static final String PERM_SEC_USERS_UPDATE = "PERM_SEC_USERS_UPDATE";

    /** API-SEC-013 — screen SEC_ROLES. */
    public static final String PERM_SEC_ROLES_CREATE = "PERM_SEC_ROLES_CREATE";

    /** API-SEC-014, 015, 016, 017 — screen SEC_ROLES. */
    public static final String PERM_SEC_ROLES_UPDATE = "PERM_SEC_ROLES_UPDATE";

    /** API-SEC-026 — screen SEC_SESSIONS. */
    public static final String PERM_SEC_SESSIONS_DELETE = "PERM_SEC_SESSIONS_DELETE";

    /** API-SEC-018, 019, 020 — screen SEC_MODULE_REGISTRY. */
    public static final String PERM_SEC_MODULE_REGISTRY_UPDATE = "PERM_SEC_MODULE_REGISTRY_UPDATE";

    /** API-SEC-024 — screen SEC_AUDIT_LOG; search and export share this VIEW permission. */
    public static final String PERM_SEC_AUDIT_LOG_VIEW = "PERM_SEC_AUDIT_LOG_VIEW";

    /** API-SEC-005 — screen SEC_USERS. */
    public static final String PERM_SEC_USERS_VIEW = "PERM_SEC_USERS_VIEW";

    /** API-SEC-012 — screen SEC_ROLES. */
    public static final String PERM_SEC_ROLES_VIEW = "PERM_SEC_ROLES_VIEW";

    /** API-SEC-021 — screen SEC_MODULE_REGISTRY. */
    public static final String PERM_SEC_MODULE_REGISTRY_VIEW = "PERM_SEC_MODULE_REGISTRY_VIEW";

    /** API-SEC-022 — screen SEC_DASHBOARD; the gateway permission, per-widget VIEWs apply on top. */
    public static final String PERM_SEC_DASHBOARD_VIEW = "PERM_SEC_DASHBOARD_VIEW";

    /** API-SEC-025 — screen SEC_SESSIONS. */
    public static final String PERM_SEC_SESSIONS_VIEW = "PERM_SEC_SESSIONS_VIEW";

    /** API-MDL-006 — screen MDL_LOOKUPS (also gates API-MDL-002, create lookup type). */
    public static final String PERM_MDL_LOOKUPS_CREATE = "PERM_MDL_LOOKUPS_CREATE";

    /**
     * API-MDL-003, 004, 007, 008, 009 — screen MDL_LOOKUPS. Deactivate (API-MDL-004/008) is
     * modelled as UPDATE per SEC-BE.md — MDL_LOOKUPS has no DELETE permission.
     */
    public static final String PERM_MDL_LOOKUPS_UPDATE = "PERM_MDL_LOOKUPS_UPDATE";

    /** API-MDL-001, 005, 011 — screen MDL_LOOKUPS. */
    public static final String PERM_MDL_LOOKUPS_VIEW = "PERM_MDL_LOOKUPS_VIEW";

    /** API-MDL-010 — screen MDL_TYPE_REGISTRY, a separate screen-gate from MDL_LOOKUPS. */
    public static final String PERM_MDL_TYPE_REGISTRY_VIEW = "PERM_MDL_TYPE_REGISTRY_VIEW";

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // CU — Common Utilities. Backend-only module: CU/SEC-BE.md declares "no screens", so these
    // four codes deliberately DEVIATE from PERM_<PAGE_CODE>_<ACTION> and are the literal strings
    // ConfigurationService's @PreAuthorize gates resolve. That deviation is a recorded, already-
    // made decision ("Do NOT rename them"), carried into the squashed core seed
    // V7__sec_seed.sql (erp-core step 04), which anchors them
    // to the backend-only holder screen CU_CONFIGURATIONS (SEC_ACTION_REG.SCREEN_ID is NOT NULL).
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /** API-CU-002 (search configurations), API-CU-003 (read by key) — screen CU_CONFIGURATIONS. */
    public static final String CONFIG_VIEW = "CONFIG_VIEW";

    /** API-CU-001 (create configuration) — screen CU_CONFIGURATIONS. */
    public static final String CONFIG_CREATE = "CONFIG_CREATE";

    /** API-CU-004 (update configuration value) — screen CU_CONFIGURATIONS. */
    public static final String CONFIG_UPDATE = "CONFIG_UPDATE";

    /**
     * API-CU-005 (deactivate configuration) — screen CU_CONFIGURATIONS. CU names its soft-delete
     * authority DEACTIVATE rather than DELETE; it is DELETE-class, and the V7__sec_seed.sql row keeps the
     * action code DEACTIVATE so the permission code stays exactly this string.
     */
    public static final String CONFIG_DEACTIVATE = "CONFIG_DEACTIVATE";

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // NOTIF — Notification Service. NOTIF/SEC-BE.md: SCR-NOTIF-001 NOTIF_TEMPLATES and
    // SCR-NOTIF-002 NOTIF_CHANNELS each expose the full VIEW/CREATE/UPDATE/DELETE set (CORE-9,
    // "4 permissions per page"); SCR-NOTIF-003 NOTIF_LOG is read-only — "CREATE/UPDATE/DELETE not
    // exposed" — so it declares VIEW only. All granted to NOTIF_ADMIN (and SYS_ADMIN) by V31.
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /** API-NOTIF-004 (search templates, read one) — screen NOTIF_TEMPLATES. */
    public static final String PERM_NOTIF_TEMPLATES_VIEW = "PERM_NOTIF_TEMPLATES_VIEW";

    /** API-NOTIF-004 (create template) — screen NOTIF_TEMPLATES. */
    public static final String PERM_NOTIF_TEMPLATES_CREATE = "PERM_NOTIF_TEMPLATES_CREATE";

    /** API-NOTIF-004 (update template) — screen NOTIF_TEMPLATES. */
    public static final String PERM_NOTIF_TEMPLATES_UPDATE = "PERM_NOTIF_TEMPLATES_UPDATE";

    /**
     * API-NOTIF-004 (deactivate template) — screen NOTIF_TEMPLATES. NOTIF's soft-delete is the
     * matrix's DELETE cell; NotificationTemplateService.deactivate() is the method it gates.
     */
    public static final String PERM_NOTIF_TEMPLATES_DELETE = "PERM_NOTIF_TEMPLATES_DELETE";

    /** API-NOTIF-005 (search channel configs, read one) — screen NOTIF_CHANNELS. */
    public static final String PERM_NOTIF_CHANNELS_VIEW = "PERM_NOTIF_CHANNELS_VIEW";

    /** API-NOTIF-005 (create channel config) — screen NOTIF_CHANNELS. */
    public static final String PERM_NOTIF_CHANNELS_CREATE = "PERM_NOTIF_CHANNELS_CREATE";

    /** API-NOTIF-005 (update channel config) — screen NOTIF_CHANNELS. */
    public static final String PERM_NOTIF_CHANNELS_UPDATE = "PERM_NOTIF_CHANNELS_UPDATE";

    /**
     * API-NOTIF-005 (disable channel config) — screen NOTIF_CHANNELS, the matrix's DELETE cell;
     * NotificationChannelConfigService.disable() is the method it gates.
     */
    public static final String PERM_NOTIF_CHANNELS_DELETE = "PERM_NOTIF_CHANNELS_DELETE";

    /**
     * API-NOTIF-002, 003 (search the notification log, read one entry) — screen NOTIF_LOG, its
     * only action. Dispatch (API-NOTIF-001) is a service endpoint behind the Security filter
     * (RULE-NOTIF-005) and is tied to no management screen, so it declares no permission.
     */
    public static final String PERM_NOTIF_LOG_VIEW = "PERM_NOTIF_LOG_VIEW";

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // FILE — File Service. FILE/SEC-BE.md: SCR-FILE-001 FILE_CATEGORIES exposes the full CRUD set
    // (API-FILE-007); SCR-FILE-002 FILE_BROWSER is VIEW (API-FILE-004/005) + UPDATE/DELETE
    // (API-FILE-006) with CREATE "contextual in owner module". PERM_FILE_BROWSER_UPDATE / _DELETE
    // are registered by V9/V31 and consumed since 2026-09-23 by FileService.softDelete, whose gate
    // picks the one matching the request's action argument (SpEL on #action).
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /** API-FILE-007 (search categories, read one) — screen FILE_CATEGORIES. */
    public static final String PERM_FILE_CATEGORIES_VIEW = "PERM_FILE_CATEGORIES_VIEW";

    /** API-FILE-007 (create category) — screen FILE_CATEGORIES. */
    public static final String PERM_FILE_CATEGORIES_CREATE = "PERM_FILE_CATEGORIES_CREATE";

    /** API-FILE-007 (update category) — screen FILE_CATEGORIES. */
    public static final String PERM_FILE_CATEGORIES_UPDATE = "PERM_FILE_CATEGORIES_UPDATE";

    /**
     * API-FILE-007 (deactivate category) — screen FILE_CATEGORIES, the matrix's DELETE cell;
     * FileCategoryService.deactivate() is the method it gates.
     */
    public static final String PERM_FILE_CATEGORIES_DELETE = "PERM_FILE_CATEGORIES_DELETE";

    /**
     * API-FILE-004, 005 (file metadata, owner list) and API-FILE-002 (issue a download token) —
     * screen FILE_BROWSER. Also the gateway (VIEW) permission of that screen.
     */
    public static final String PERM_FILE_BROWSER_VIEW = "PERM_FILE_BROWSER_VIEW";

    /**
     * API-FILE-001 (upload) — screen FILE_BROWSER. SEC-BE.md calls upload "contextual in owner
     * module"; RULE-SEC-011's 4-per-page rule still registers the CREATE action on the screen, and
     * FileService.store() is the gate that consumes it.
     */
    public static final String PERM_FILE_BROWSER_CREATE = "PERM_FILE_BROWSER_CREATE";

    /** API-FILE-006 with action=ARCHIVE — screen FILE_BROWSER; FileService.softDelete() gates on it. */
    public static final String PERM_FILE_BROWSER_UPDATE = "PERM_FILE_BROWSER_UPDATE";

    /** API-FILE-006 with action=DELETE — screen FILE_BROWSER; FileService.softDelete() gates on it. */
    public static final String PERM_FILE_BROWSER_DELETE = "PERM_FILE_BROWSER_DELETE";

    /**
     * erp-core step 05 — every {@code /api/v1/platform/tenants} operation (TenantService) — screen
     * PLATFORM_TENANTS, module PLATFORM. The code is the step file's literal (not
     * {@code PERM_<PAGE>_<ACTION>}), like CU's {@code CONFIG_*}. Seeded by {@code V10__tenant_schema.sql}
     * to the PLATFORM tenant's SYS_ADMIN only; tenant provisioning never copies it to a new tenant.
     */
    public static final String PLATFORM_TENANT_MANAGE = "PLATFORM_TENANT_MANAGE";
}
