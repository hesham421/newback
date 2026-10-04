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

    /** API-FIN-002 — screen FIN_ACCOUNTS (SEC-BE.md matrix, FIN_ACCOUNTS / CREATE). */
    public static final String PERM_FIN_ACCOUNTS_CREATE = "PERM_FIN_ACCOUNTS_CREATE";

    /**
     * API-FIN-003, 004 — screen FIN_ACCOUNTS (SEC-BE.md matrix, FIN_ACCOUNTS / UPDATE).
     * Deactivate (API-FIN-004) is modelled as UPDATE, per that API's own {@code Security :} line;
     * same posture as MDL_LOOKUPS, which likewise has no DELETE permission.
     */
    public static final String PERM_FIN_ACCOUNTS_UPDATE = "PERM_FIN_ACCOUNTS_UPDATE";

    /** API-FIN-006, 007 — screen FIN_DIMENSIONS (SEC-BE.md matrix, FIN_DIMENSIONS / CREATE). */
    public static final String PERM_FIN_DIMENSIONS_CREATE = "PERM_FIN_DIMENSIONS_CREATE";

    /**
     * API-FIN-035 (deactivate a dimension value) — screen FIN_DIMENSIONS. Deactivate is modelled
     * as UPDATE, not DELETE: srs-fin.md:1139-1141 gates API-FIN-004's
     * {@code PUT /{id}/deactivate} on {@code PERM_FIN_ACCOUNTS_UPDATE} ("there is no DELETE
     * endpoint and no PERM_FIN_ACCOUNTS_DELETE"), and srs-fin.md:1484-1487 states the same
     * module-wide — "deactivation, where it exists, is PUT /{id}/deactivate gated by the screen's
     * UPDATE permission, and V24 seeds no PERM_FIN_*_DELETE row".
     *
     * <p>srs-fin.md:1165-1169 still records this screen as having no UPDATE row and no
     * dimension-value deactivate endpoint. That is an accurate description of the state BEFORE
     * API-FIN-035; the spec-alignment session is bringing it into line.
     *
     * <p>Unlike every other FIN constant here, this one has NO action row in
     * {@code V24__fin_security_seed.sql}: that seed registered FIN_DIMENSIONS with VIEW and CREATE
     * only, because no UPDATE-class endpoint existed on the screen yet. The row — and the
     * SYS_ADMIN grant V25's already-applied {@code SELECT} over the registry cannot retroactively
     * pick up — is added by {@code V28__fin_dimensions_update_action.sql}.
     */
    public static final String PERM_FIN_DIMENSIONS_UPDATE = "PERM_FIN_DIMENSIONS_UPDATE";

    /** API-FIN-010 — screen FIN_RULES (SEC-BE.md matrix, FIN_RULES / CREATE). */
    public static final String PERM_FIN_RULES_CREATE = "PERM_FIN_RULES_CREATE";

    /** API-FIN-011 (add line) — screen FIN_RULES (SEC-BE.md matrix, FIN_RULES / UPDATE). */
    public static final String PERM_FIN_RULES_UPDATE = "PERM_FIN_RULES_UPDATE";

    /**
     * API-FIN-013 — screen FIN_RECURRING_TEMPLATES (SEC-BE.md matrix,
     * FIN_RECURRING_TEMPLATES / CREATE).
     */
    public static final String PERM_FIN_RECURRING_TEMPLATES_CREATE =
        "PERM_FIN_RECURRING_TEMPLATES_CREATE";

    /**
     * API-FIN-016 — screen FIN_ALLOCATION_RULES (SEC-BE.md matrix,
     * FIN_ALLOCATION_RULES / CREATE).
     */
    public static final String PERM_FIN_ALLOCATION_RULES_CREATE =
        "PERM_FIN_ALLOCATION_RULES_CREATE";

    /**
     * API-FIN-019 — screen FIN_JOURNAL_ENTRIES (SEC-BE.md matrix,
     * FIN_JOURNAL_ENTRIES / CREATE). RULE-FIN-015 requires this permission to be held by a role
     * distinct from {@code PERM_FIN_PERIODS_CLOSE_APPROVE}; that check lives in the period-close
     * flow (API-FIN-026/027), not here.
     */
    public static final String PERM_FIN_JOURNAL_ENTRIES_CREATE = "PERM_FIN_JOURNAL_ENTRIES_CREATE";

    /**
     * API-FIN-014 (run template) — screen FIN_RECURRING_TEMPLATES (SEC-BE.md matrix,
     * FIN_RECURRING_TEMPLATES / UPDATE). Running a template is modelled as an update-class custom
     * action, per that API's own {@code Security :} line.
     */
    public static final String PERM_FIN_RECURRING_TEMPLATES_UPDATE =
        "PERM_FIN_RECURRING_TEMPLATES_UPDATE";

    /**
     * API-FIN-017 (run allocation rule) — screen FIN_ALLOCATION_RULES (SEC-BE.md matrix,
     * FIN_ALLOCATION_RULES / UPDATE).
     */
    public static final String PERM_FIN_ALLOCATION_RULES_UPDATE =
        "PERM_FIN_ALLOCATION_RULES_UPDATE";

    /**
     * API-FIN-021 (reverse entry) — screen FIN_JOURNAL_ENTRIES, the custom action SEC-BE.md's
     * matrix names verbatim ({@code Reverse (PERM_FIN_JOURNAL_ENTRIES_REVERSE, API-FIN-021)}).
     */
    public static final String PERM_FIN_JOURNAL_ENTRIES_REVERSE =
        "PERM_FIN_JOURNAL_ENTRIES_REVERSE";

    /** API-FIN-023 (create fiscal year) — screen FIN_PERIODS (SEC-BE.md matrix, FIN_PERIODS / CREATE). */
    public static final String PERM_FIN_PERIODS_CREATE = "PERM_FIN_PERIODS_CREATE";

    /**
     * API-FIN-024, 025 (open / soft-close a period) — screen FIN_PERIODS (SEC-BE.md matrix,
     * FIN_PERIODS / UPDATE).
     */
    public static final String PERM_FIN_PERIODS_UPDATE = "PERM_FIN_PERIODS_UPDATE";

    /**
     * API-FIN-026, 027 (hard-close a period, run year-end close) — screen FIN_PERIODS, the custom
     * SoD-gated action SEC-BE.md's matrix names verbatim
     * ({@code Close-approve (PERM_FIN_PERIODS_CLOSE_APPROVE, API-FIN-026,027 — RULE-FIN-015 SoD)}).
     * RULE-FIN-015 additionally requires that no single user hold both this permission and
     * {@code PERM_FIN_JOURNAL_ENTRIES_CREATE}; that union check runs in FIN's service layer.
     */
    public static final String PERM_FIN_PERIODS_CLOSE_APPROVE = "PERM_FIN_PERIODS_CLOSE_APPROVE";
    // ─────────────────────────────────────────────────────────────────────────────────────────
    // FIN — SVC-API-SEARCH. The VIEW column of SEC-BE.md's permission matrix, one constant per
    // secured screen that exposes a read endpoint. Names are taken verbatim from that matrix and
    // follow PERM_<PAGE_CODE>_<ACTION>. Per SEC-BE.md's "Gateway" note, VIEW is the gateway
    // permission of its screen: every non-VIEW action on the same screen already requires it, so
    // these are the least-privileged FIN permissions, not additions on top of the write ones.
    // ─────────────────────────────────────────────────────────────────────────────────────────

    /** API-FIN-001 (search accounts) — screen FIN_ACCOUNTS (SEC-BE.md matrix, FIN_ACCOUNTS / VIEW). */
    public static final String PERM_FIN_ACCOUNTS_VIEW = "PERM_FIN_ACCOUNTS_VIEW";

    /**
     * API-FIN-005 and API-FIN-008 (search dimensions, search dimension values) — screen
     * FIN_DIMENSIONS (SEC-BE.md matrix, FIN_DIMENSIONS / VIEW, which names both APIs).
     */
    public static final String PERM_FIN_DIMENSIONS_VIEW = "PERM_FIN_DIMENSIONS_VIEW";

    /** API-FIN-009 (search event-type rules) — screen FIN_RULES (SEC-BE.md matrix, FIN_RULES / VIEW). */
    public static final String PERM_FIN_RULES_VIEW = "PERM_FIN_RULES_VIEW";

    /**
     * API-FIN-012 (search recurring templates) — screen FIN_RECURRING_TEMPLATES (SEC-BE.md matrix,
     * FIN_RECURRING_TEMPLATES / VIEW).
     */
    public static final String PERM_FIN_RECURRING_TEMPLATES_VIEW =
        "PERM_FIN_RECURRING_TEMPLATES_VIEW";

    /**
     * API-FIN-015 (search allocation rules) — screen FIN_ALLOCATION_RULES (SEC-BE.md matrix,
     * FIN_ALLOCATION_RULES / VIEW).
     */
    public static final String PERM_FIN_ALLOCATION_RULES_VIEW = "PERM_FIN_ALLOCATION_RULES_VIEW";

    /**
     * API-FIN-018 and API-FIN-022 (search entries, read one entry) — screen FIN_JOURNAL_ENTRIES
     * (SEC-BE.md matrix, FIN_JOURNAL_ENTRIES / VIEW, which names both APIs).
     */
    public static final String PERM_FIN_JOURNAL_ENTRIES_VIEW = "PERM_FIN_JOURNAL_ENTRIES_VIEW";

    /**
     * API-FIN-028 (account ledger) — screen FIN_ACCOUNT_LEDGER (SEC-BE.md matrix,
     * FIN_ACCOUNT_LEDGER / VIEW, its only action).
     */
    public static final String PERM_FIN_ACCOUNT_LEDGER_VIEW = "PERM_FIN_ACCOUNT_LEDGER_VIEW";

    /**
     * API-FIN-029 (trial balance) — screen FIN_TRIAL_BALANCE (SEC-BE.md matrix,
     * FIN_TRIAL_BALANCE / VIEW, its only action).
     */
    public static final String PERM_FIN_TRIAL_BALANCE_VIEW = "PERM_FIN_TRIAL_BALANCE_VIEW";

    /**
     * API-FIN-030 (balance sheet) — screen FIN_BALANCE_SHEET (SEC-BE.md matrix,
     * FIN_BALANCE_SHEET / VIEW, its only action).
     */
    public static final String PERM_FIN_BALANCE_SHEET_VIEW = "PERM_FIN_BALANCE_SHEET_VIEW";

    /**
     * API-FIN-031 (income statement) — screen FIN_INCOME_STATEMENT (SEC-BE.md matrix,
     * FIN_INCOME_STATEMENT / VIEW, its only action).
     */
    public static final String PERM_FIN_INCOME_STATEMENT_VIEW = "PERM_FIN_INCOME_STATEMENT_VIEW";

    /**
     * API-FIN-032 (dimension report) — screen FIN_DIMENSION_REPORTS (SEC-BE.md matrix,
     * FIN_DIMENSION_REPORTS / VIEW, its only action).
     */
    public static final String PERM_FIN_DIMENSION_REPORTS_VIEW = "PERM_FIN_DIMENSION_REPORTS_VIEW";

    /**
     * API-FIN-033 (search fiscal periods) — screen FIN_PERIODS (SEC-BE.md matrix,
     * FIN_PERIODS / VIEW). The action row already exists in the database:
     * {@code V24__fin_security_seed.sql} seeds the tuple {@code ('FIN_PERIODS', 'VIEW', 'عرض')}
     * and synthesizes its code as {@code 'PERM_' || v.page_code || '_' || v.action_code}, giving
     * {@code PERM_FIN_PERIODS_VIEW}; {@code V25__fin_role_grants.sql} grants it to SYS_ADMIN (it
     * excludes only {@code PERM_FIN_PERIODS_CLOSE_APPROVE}) and
     * {@code V27__fin_close_approver_role.sql} grants it to FIN_CLOSE_APPROVER. V24's own header
     * records why it was seeded without a constant — "FIN exposes no read endpoint on FIN_PERIODS"
     * and "either the matrix's FIN_PERIODS/VIEW ✓ is spurious, or a FIN_PERIODS read endpoint is
     * missing". API-FIN-033 is that endpoint, so the constant is declared here now. No migration
     * accompanies this: the row and both grants are already applied.
     */
    public static final String PERM_FIN_PERIODS_VIEW = "PERM_FIN_PERIODS_VIEW";

    /**
     * API-FIN-038 (search account mappings, SVC-API-SEARCH) — screen FIN_ACCOUNT_MAPPINGS
     * (srs-fin.md SCR-REQ-FIN-013 §B4, VIEW is the gateway). Declared with the v2 CRUD sub so the
     * SEARCH sub can gate on it; the SEC_* seed rows are SEC-BE's, not a migration here.
     */
    public static final String PERM_FIN_ACCOUNT_MAPPINGS_VIEW = "PERM_FIN_ACCOUNT_MAPPINGS_VIEW";

    /** API-FIN-039 (create account mapping) — screen FIN_ACCOUNT_MAPPINGS (SCR-REQ-FIN-013 §B4, CREATE). */
    public static final String PERM_FIN_ACCOUNT_MAPPINGS_CREATE = "PERM_FIN_ACCOUNT_MAPPINGS_CREATE";

    /**
     * API-FIN-040, 041 (change the account, deactivate) — screen FIN_ACCOUNT_MAPPINGS
     * (SCR-REQ-FIN-013 §B4: UPDATE covers deactivate, as FIN_ACCOUNTS models it).
     */
    public static final String PERM_FIN_ACCOUNT_MAPPINGS_UPDATE = "PERM_FIN_ACCOUNT_MAPPINGS_UPDATE";

    // ─────────────────────────────────────────────────────────────────────────────────────────
    // CU — Common Utilities. Backend-only module: CU/SEC-BE.md declares "no screens", so these
    // four codes deliberately DEVIATE from PERM_<PAGE_CODE>_<ACTION> and are the literal strings
    // ConfigurationService's @PreAuthorize gates resolve. That deviation is a recorded, already-
    // made decision — see V13__cu_security_seed.sql's header ("Do NOT rename them"), re-applied
    // against the current SEC schema by V31__cu_notif_file_security_seed.sql, which anchors them
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
     * authority DEACTIVATE rather than DELETE; V13 typed it DELETE-class and the row keeps the
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
}
