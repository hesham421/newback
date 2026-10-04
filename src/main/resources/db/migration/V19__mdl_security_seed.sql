-- V19 — Master Data Lookup (MDL) security-registry seed
-- Source: governance/modules/MDL/packages/backend-execution/SEC-BE/SEC-BE.md (2-screen
--   permission matrix + "Seed data"), V16__sec_schema.sql (the SEC_MODULE_REG / SEC_SCREEN_REG /
--   SEC_ACTION_REG DDL), V17__sec_security_seed.sql (the only existing precedent for registering a
--   module + its screens + its actions into SEC's own tables).
-- Purpose: register MDL itself into SEC_MODULE_REG (SEC_SCREEN_REG.MODULE_ID is a FK to it, so MDL
--   needs a real module row to point at — SEC-BE.md point 4), then its two screens (MDL_LOOKUPS,
--   MDL_TYPE_REGISTRY) and their action rows, so PermissionConstants' PERM_MDL_* codes resolve to
--   real, grantable SEC_ACTION_REG rows.
--
-- Registration mechanism — resolved reading, not a guess: SEC-BE.md's "registered into SEC via
--   SEC's own screen-registration endpoint" / "via SEC's own action-registration endpoint" wording
--   describes the DATA being registered and the mechanism SEC itself *exposes* (RegistryController's
--   REST endpoints, API-SEC-018/019/020) for a LIVE runtime registration by some other future caller
--   (e.g. an admin UI flow). It does NOT describe how THIS codebase seeds a module's OWN screens at
--   deploy time. V17 proves that: it seeds SEC's own 9 screens and their actions directly via plain
--   `INSERT INTO SEC_MODULE_REG / SEC_SCREEN_REG / SEC_ACTION_REG` statements in a Flyway migration —
--   not by SEC calling its own RegistryController over HTTP at startup — because Flyway seed
--   migrations are the established, deploy-time mechanism for reference/registry data in this repo,
--   and no runtime HTTP server exists yet at migration time to call anyway. This migration mirrors
--   that exact precedent for MDL. No Java source is touched — matching V17, which needed none either.
--
-- Style matches V17: plain INSERTs (not idempotent — runs once on a fresh schema), surrogate PKs
--   from the V16 SEQ_SEC_* sequences, every FK resolved by natural key (CODE / PAGE_CODE), never a
--   hardcoded id. Booleans are native (V16 uses BOOLEAN).

-- 1. SEC_MODULE_REG — module MDL (Tier-1 grantable unit). MDL is not yet registered anywhere else
--    in this schema (grep confirms only SEC has ever completed this step for itself).
INSERT INTO SEC_MODULE_REG (MODULE_REG_PK, CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_MODULE_REG'), 'MDL', 'البيانات المرجعية', 'Master Data Lookup', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- 2. SEC_SCREEN_REG — the 2 page codes of the SEC-BE.md matrix.
INSERT INTO SEC_SCREEN_REG (SCREEN_REG_PK, PAGE_CODE, MODULE_ID, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_SCREEN_REG'), 'MDL_LOOKUPS',
        (SELECT MODULE_REG_PK FROM SEC_MODULE_REG WHERE CODE = 'MDL'),
        'قوائم البيانات المرجعية', 'Master data lookups', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_SCREEN_REG (SCREEN_REG_PK, PAGE_CODE, MODULE_ID, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_SCREEN_REG'), 'MDL_TYPE_REGISTRY',
        (SELECT MODULE_REG_PK FROM SEC_MODULE_REG WHERE CODE = 'MDL'),
        'سجل أنواع البيانات المرجعية', 'Master data type registry', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- 3. SEC_ACTION_REG — exactly the cells the SEC-BE.md matrix names a permission code for.
--    PERMISSION_CODE values are copied verbatim from com.erp.sec.permission.PermissionConstants
--    (PERM_MDL_LOOKUPS_VIEW / _CREATE / _UPDATE / PERM_MDL_TYPE_REGISTRY_VIEW). No DELETE row for
--    MDL_LOOKUPS — deactivate is modeled as UPDATE (SEC-BE.md, no hard-delete endpoint exists) —
--    and no CREATE/UPDATE/DELETE rows for MDL_TYPE_REGISTRY (VIEW only per the matrix).

-- MDL_LOOKUPS — VIEW (API-MDL-001, 005, 011), CREATE (API-MDL-002, 006), UPDATE (API-MDL-003, 004, 007, 008, 009)
INSERT INTO SEC_ACTION_REG (ACTION_REG_PK, PERMISSION_CODE, SCREEN_ID, ACTION_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_ACTION_REG'), 'PERM_MDL_LOOKUPS_VIEW',
        (SELECT SCREEN_REG_PK FROM SEC_SCREEN_REG WHERE PAGE_CODE = 'MDL_LOOKUPS'), 'VIEW',
        'قوائم البيانات المرجعية - عرض', 'Master data lookups - VIEW', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_ACTION_REG (ACTION_REG_PK, PERMISSION_CODE, SCREEN_ID, ACTION_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_ACTION_REG'), 'PERM_MDL_LOOKUPS_CREATE',
        (SELECT SCREEN_REG_PK FROM SEC_SCREEN_REG WHERE PAGE_CODE = 'MDL_LOOKUPS'), 'CREATE',
        'قوائم البيانات المرجعية - إنشاء', 'Master data lookups - CREATE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_ACTION_REG (ACTION_REG_PK, PERMISSION_CODE, SCREEN_ID, ACTION_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_ACTION_REG'), 'PERM_MDL_LOOKUPS_UPDATE',
        (SELECT SCREEN_REG_PK FROM SEC_SCREEN_REG WHERE PAGE_CODE = 'MDL_LOOKUPS'), 'UPDATE',
        'قوائم البيانات المرجعية - تعديل', 'Master data lookups - UPDATE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- MDL_TYPE_REGISTRY — VIEW only (API-MDL-010)
INSERT INTO SEC_ACTION_REG (ACTION_REG_PK, PERMISSION_CODE, SCREEN_ID, ACTION_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_ACTION_REG'), 'PERM_MDL_TYPE_REGISTRY_VIEW',
        (SELECT SCREEN_REG_PK FROM SEC_SCREEN_REG WHERE PAGE_CODE = 'MDL_TYPE_REGISTRY'), 'VIEW',
        'سجل أنواع البيانات المرجعية - عرض', 'Master data type registry - VIEW', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- No SEC_ROLE / SEC_ROLE_MODULE_GRANT / SEC_ROLE_SCREEN_GRANT / SEC_ROLE_ACTION_GRANT rows here:
-- V17 already created the SYS_ADMIN bootstrap role, and its SEC_ROLE_ACTION_GRANT seed ran once,
-- at V17's own apply time, over whatever SEC_ACTION_REG rows existed then — it does NOT
-- retroactively cover the new MDL_* rows this migration inserts later. Granting SYS_ADMIN (or any
-- role) the new MDL module/screens/actions is deliberately out of scope for this seed: SEC-BE.md's
-- matrix specifies registration only, not a grant decision, and this sub's own instructions are
-- pure SQL seed data for the registry tables — a future grant/administration step is left to SEC's
-- own role-management flow (or a later, explicitly-scoped migration), not silently bundled here.
