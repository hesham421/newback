-- ============================================================
-- V31 — CU / NOTIF / FILE security-registry seed and grants (re-applied under the V16 schema)
-- ============================================================
-- Purpose: make real permission enforcement possible again on the three legacy-path modules.
--   Every @PreAuthorize in com.erp.cu.service, com.erp.notif.service and com.erp.file.service was
--   commented out behind "TODO: SEC-PENDING — re-add ... once the new SEC module ships
--   PermissionConstants", so any authenticated caller could create, update and delete platform
--   configuration, notification templates and channels, and files. The constants now exist in
--   com.erp.sec.permission.PermissionConstants and the gates are live; this migration registers
--   and grants the rows those gates resolve against. Without it the three modules would answer
--   403 ACCESS_DENIED to every caller, SYS_ADMIN included — exactly the state V19 left MDL in and
--   V21 had to repair. Registration and grants therefore land together in one file, as V24+V25 do.
--
-- Sources — nothing below is invented:
--   * governance/modules/CU/packages/backend-execution/SEC-BE/SEC-BE.md
--   * governance/modules/NOTIF/packages/backend-execution/SEC-BE/SEC-BE.md (+ P1/srs.md screen headings)
--   * governance/modules/FILE/packages/backend-execution/SEC-BE/SEC-BE.md (+ P1/srs.md screen headings)
--   * V13__cu_security_seed.sql — the RECORDED PRIOR DECISION for CU (see below)
--   * V7__notif_security_seed.sql / V9__file_security_seed.sql — the same modules' old-schema seeds,
--     the source of every bilingual screen and role name reused here (never retyped from memory)
--   * V16__sec_schema.sql — every column, constraint and sequence name below is verbatim from it
--   * V24/V25/V27 — the current-schema shape this file follows (natural-key FK resolution,
--     SEQ_SEC_* surrogate PKs, Tier-1 → Tier-2 → Tier-3 ordering)
--
-- ------------------------------------------------------------
-- CU — re-applying an existing decision, not making a new one
-- ------------------------------------------------------------
-- CU/SEC-BE.md classifies CU as backend-only with NO screens and states "SECURITY SEED DATA
-- REQUIREMENTS: none", while delegating the concrete authority mechanism to SEC ("CU declares the
-- enforcement requirement, SEC provides the mechanism"; SEC-IMPL-RULE-3 "DEFERRED TO SEC MODULE").
-- V13 is that deferred mechanism, approved by explicit human decision on 2026-09-06: because
-- SEC_PERMISSION.PAGE_FK was NOT NULL, CU's four authorities were anchored to one backend-only
-- holder page, CU_CONFIGURATIONS, that exists solely to hold them and is not a rendered screen.
-- V14 dropped that schema, so V13 is dead — this file RE-APPLIES its decision under V16, where
-- SEC_ACTION_REG.SCREEN_ID is likewise NOT NULL and the same holder screen is still required.
--
-- V13's PERMISSION_CODE deviation is re-applied unchanged: CU's four codes are CONFIG_VIEW /
-- CONFIG_CREATE / CONFIG_UPDATE / CONFIG_DEACTIVATE, NOT the PERM_<PAGE_CODE>_<ACTION> shape
-- NOTIF/FILE/FIN use. They equal the string values of the CU constants in PermissionConstants
-- verbatim, which is what runtime hasAuthority(...) compares against. Do NOT rename them.
-- SEC_ACTION_REG.PERMISSION_CODE is free text (VARCHAR(100), UNIQUE, no CHECK), so the deviation
-- costs nothing structurally. ACTION_CODE still uses the platform vocabulary, and in particular
-- CU's gateway row carries ACTION_CODE 'VIEW': MenuService.effectiveAuthorityCodes keeps a
-- non-gateway grant only while the same screen's ACTION_CODE='VIEW' row is granted too
-- (RoleActionGrantDomain.isGatewayAction, RULE-SEC-007). CU's DEACTIVATE row keeps ACTION_CODE
-- 'DEACTIVATE' (V13 typed it DELETE-class) so its permission code stays exactly CONFIG_DEACTIVATE.
--
-- ------------------------------------------------------------
-- What is seeded per screen, and the two judgement calls
-- ------------------------------------------------------------
-- NOTIF_TEMPLATES and NOTIF_CHANNELS: the full VIEW/CREATE/UPDATE/DELETE set their matrix names.
-- FILE_CATEGORIES: same full set (API-FILE-007 CRUD).
-- FILE_BROWSER: all four. VIEW (API-FILE-004/005) and UPDATE/DELETE (API-FILE-006) are named
--   outright; CREATE is named as "contextual in owner module", the SEED DATA line registers
--   "PERM_FILE_BROWSER_{...}" 4-per-page, FileService.store() gates on PERM_FILE_BROWSER_CREATE,
--   and V9 seeded and granted PERM_FILE_BROWSER_CREATE under the old schema. Omitting it would
--   leave a live gate unresolvable.
-- NOTIF_LOG: VIEW only. Its matrix says "CREATE/UPDATE/DELETE not exposed" outright. V7 seeded all
--   four anyway (granting only VIEW) on the grounds that SEC's runtime generator always makes 4;
--   under the V16 schema nothing auto-generates, rows are hand-seeded, and V24's precedent is to
--   register only the cells the matrix names rather than create permanently-unreferenced registry
--   data. The VIEW-only reading is taken, deliberately diverging from V7.
--
-- PERM_FILE_BROWSER_UPDATE and PERM_FILE_BROWSER_DELETE are registered and granted but have no
-- constant in PermissionConstants and no @PreAuthorize consuming them: FileService.softDelete(id,
-- action) selects between archive and soft-delete by request argument and still carries its
-- SEC-PENDING TODO, because one annotation cannot express two permissions chosen by argument.
-- The rows follow the matrix; the missing gate is reported, not invented. (V24 set the precedent
-- for a registered row with no constant — PERM_FIN_PERIODS_VIEW.)
--
-- ------------------------------------------------------------
-- Grants — SYS_ADMIN gets everything here, on purpose
-- ------------------------------------------------------------
-- Each module gets its own admin role, exactly as its matrix names (NOTIF_ADMIN, FILE_ADMIN, and
-- CU's equivalent CU_ADMIN from V13), holding only its own module's permissions. Every permission
-- is ALSO granted to SYS_ADMIN, the role the bootstrap 'admin' user holds (V17 §8/§9) — the same
-- both-roles pattern V7/V9/V13 documented. This is not a convenience: without it, the moment the
-- gates go live every existing caller and all three api-verify suites break with 403. No
-- separation-of-duties rule applies to these three modules (nothing here resembles RULE-FIN-015),
-- so there is no permission to withhold from SYS_ADMIN.
--
-- Ordering follows V25/V27: Tier-1 SEC_ROLE_MODULE_GRANT, then Tier-2 SEC_ROLE_SCREEN_GRANT
-- (RULE-SEC-001/002), then Tier-3 SEC_ROLE_ACTION_GRANT. Tier-3 is what @PreAuthorize actually
-- reads (JwtAuthenticationFilter ← MenuService.effectivePermissionCodes); Tiers 1-2 keep the
-- navigation view and the registry self-consistent.
--
-- ------------------------------------------------------------
-- Idempotency — a deliberate deviation from V24/V25/V27
-- ------------------------------------------------------------
-- Those files use plain INSERTs ("runs once on a fresh schema"). This one guards every insert with
-- WHERE NOT EXISTS on the row's natural key, because unlike FIN these modules are PARTIALLY seeded
-- on live dev databases already: V20 registered NOTIF and FILE in SEC_MODULE_REG (verified: both
-- present, CU absent, and neither has any screen or action row). The CU module row is therefore
-- the only SEC_MODULE_REG insert here, and the guards make a re-run against any intermediate state
-- a no-op instead of a unique-constraint failure. Every FK is still resolved by natural key, never
-- a hardcoded id; surrogate PKs still come from the V16 SEQ_SEC_* sequences.
--
-- Seeds: 1 module (CU) | 6 screens (CU 1, NOTIF 3, FILE 2) | 21 actions (CU 4, NOTIF 9, FILE 8)
--        3 roles (CU_ADMIN, NOTIF_ADMIN, FILE_ADMIN)
--        6 Tier-1 + 12 Tier-2 + 42 Tier-3 grants
-- ============================================================

-- ------------------------------------------------------------
-- 1. SEC_MODULE_REG — CU only. NOTIF and FILE were registered by V20 §1 and are NOT re-inserted.
--    Bilingual name from V13 §1 ('الأدوات المشتركة' / 'Common Utilities').
-- ------------------------------------------------------------
INSERT INTO SEC_MODULE_REG (MODULE_REG_PK, CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
SELECT nextval('SEQ_SEC_MODULE_REG'), 'CU', 'الأدوات المشتركة', 'Common Utilities',
       TRUE, 'SYSTEM', CURRENT_TIMESTAMP
WHERE NOT EXISTS (SELECT 1 FROM SEC_MODULE_REG WHERE CODE = 'CU');

-- ------------------------------------------------------------
-- 2. SEC_ROLE — the three module-admin roles their matrices name. SYS_ADMIN already exists
--    (V17 §4) and is referenced by natural key, never re-inserted. Bilingual names are the ones
--    V13 §2 / V7 §2 / V9 §2 already used for these same three roles.
-- ------------------------------------------------------------
INSERT INTO SEC_ROLE (ROLE_PK, CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
SELECT nextval('SEQ_SEC_ROLE'), v.code, v.name_ar, v.name_en, TRUE, 'SYSTEM', CURRENT_TIMESTAMP
FROM (VALUES
    ('CU_ADMIN',    'مدير الإعدادات', 'Configuration Administrator'),
    ('NOTIF_ADMIN', 'مدير الإشعارات', 'Notification Administrator'),
    ('FILE_ADMIN',  'مدير الملفات',   'File Administrator')
) AS v(code, name_ar, name_en)
WHERE NOT EXISTS (SELECT 1 FROM SEC_ROLE r WHERE r.CODE = v.code);

-- ------------------------------------------------------------
-- 3. SEC_SCREEN_REG — the 6 page codes the three SEC-BE files name. CU_CONFIGURATIONS is the
--    backend-only holder screen V13 introduced (not a rendered CU screen). Bilingual names come
--    from V13 §3 and from the SCR-NOTIF-00x / SCR-FILE-00x headings in each module's P1/srs.md,
--    which V7 §3 / V9 §3 already used. No nav parent exists in this schema (SEC_SCREEN_REG has no
--    parent column at all), so V7's nav-parent note is moot here.
-- ------------------------------------------------------------
INSERT INTO SEC_SCREEN_REG (SCREEN_REG_PK, PAGE_CODE, MODULE_ID, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
SELECT nextval('SEQ_SEC_SCREEN_REG'), v.page_code,
       (SELECT MODULE_REG_PK FROM SEC_MODULE_REG WHERE CODE = v.module_code),
       v.name_ar, v.name_en, TRUE, 'SYSTEM', CURRENT_TIMESTAMP
FROM (VALUES
    ('CU',    'CU_CONFIGURATIONS', 'إدارة إعدادات المنصة',  'Platform Configuration'),
    ('NOTIF', 'NOTIF_TEMPLATES',   'إدارة قوالب الإشعارات', 'Notification Templates'),
    ('NOTIF', 'NOTIF_CHANNELS',    'تهيئة القنوات',         'Channel Configuration'),
    ('NOTIF', 'NOTIF_LOG',         'سجل الإشعارات',         'Notification Log'),
    ('FILE',  'FILE_CATEGORIES',   'إدارة فئات الملفات',    'File Categories'),
    ('FILE',  'FILE_BROWSER',      'مستعرض الملفات',        'File Browser')
) AS v(module_code, page_code, name_ar, name_en)
WHERE NOT EXISTS (SELECT 1 FROM SEC_SCREEN_REG s WHERE s.PAGE_CODE = v.page_code);

-- ------------------------------------------------------------
-- 4. SEC_ACTION_REG — 21 rows, exactly the cells the three matrices name (see the header for the
--    NOTIF_LOG and FILE_BROWSER judgement calls). PERMISSION_CODE is carried explicitly rather
--    than synthesized, because CU's four codes deliberately do not follow PERM_<PAGE>_<ACTION>.
--    SCREEN_ID is resolved from PAGE_CODE. NAME_AR/NAME_EN mirror V19/V24's "<screen> - <action>".
-- ------------------------------------------------------------
INSERT INTO SEC_ACTION_REG (ACTION_REG_PK, PERMISSION_CODE, SCREEN_ID, ACTION_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
SELECT nextval('SEQ_SEC_ACTION_REG'),
       v.permission_code,
       s.SCREEN_REG_PK,
       v.action_code,
       s.NAME_AR || ' - ' || v.action_ar,
       s.NAME_EN || ' - ' || v.action_code,
       TRUE, 'SYSTEM', CURRENT_TIMESTAMP
FROM (VALUES
    -- CU_CONFIGURATIONS — V13's four codes, unrenamed. ConfigurationService gates all four.
    ('CU_CONFIGURATIONS', 'VIEW',       'عرض',   'CONFIG_VIEW'),
    ('CU_CONFIGURATIONS', 'CREATE',     'إنشاء', 'CONFIG_CREATE'),
    ('CU_CONFIGURATIONS', 'UPDATE',     'تعديل', 'CONFIG_UPDATE'),
    ('CU_CONFIGURATIONS', 'DEACTIVATE', 'إلغاء تفعيل', 'CONFIG_DEACTIVATE'),
    -- NOTIF_TEMPLATES — API-NOTIF-004 CRUD, all four cells ✓ for NOTIF_ADMIN.
    ('NOTIF_TEMPLATES',   'VIEW',   'عرض',   'PERM_NOTIF_TEMPLATES_VIEW'),
    ('NOTIF_TEMPLATES',   'CREATE', 'إنشاء', 'PERM_NOTIF_TEMPLATES_CREATE'),
    ('NOTIF_TEMPLATES',   'UPDATE', 'تعديل', 'PERM_NOTIF_TEMPLATES_UPDATE'),
    ('NOTIF_TEMPLATES',   'DELETE', 'حذف',   'PERM_NOTIF_TEMPLATES_DELETE'),
    -- NOTIF_CHANNELS — API-NOTIF-005 CRUD, all four cells ✓ for NOTIF_ADMIN.
    ('NOTIF_CHANNELS',    'VIEW',   'عرض',   'PERM_NOTIF_CHANNELS_VIEW'),
    ('NOTIF_CHANNELS',    'CREATE', 'إنشاء', 'PERM_NOTIF_CHANNELS_CREATE'),
    ('NOTIF_CHANNELS',    'UPDATE', 'تعديل', 'PERM_NOTIF_CHANNELS_UPDATE'),
    ('NOTIF_CHANNELS',    'DELETE', 'حذف',   'PERM_NOTIF_CHANNELS_DELETE'),
    -- NOTIF_LOG — read-only system record: "CREATE/UPDATE/DELETE not exposed" (API-NOTIF-002/003).
    ('NOTIF_LOG',         'VIEW',   'عرض',   'PERM_NOTIF_LOG_VIEW'),
    -- FILE_CATEGORIES — API-FILE-007 CRUD, all four cells ✓ for FILE_ADMIN.
    ('FILE_CATEGORIES',   'VIEW',   'عرض',   'PERM_FILE_CATEGORIES_VIEW'),
    ('FILE_CATEGORIES',   'CREATE', 'إنشاء', 'PERM_FILE_CATEGORIES_CREATE'),
    ('FILE_CATEGORIES',   'UPDATE', 'تعديل', 'PERM_FILE_CATEGORIES_UPDATE'),
    ('FILE_CATEGORIES',   'DELETE', 'حذف',   'PERM_FILE_CATEGORIES_DELETE'),
    -- FILE_BROWSER — VIEW (API-FILE-004/005 + 002), CREATE (API-FILE-001 upload, contextual),
    --   UPDATE (API-FILE-006 archive) and DELETE (API-FILE-006 soft-delete). The last two are
    --   registered per the matrix but currently gate nothing — see the header.
    ('FILE_BROWSER',      'VIEW',   'عرض',   'PERM_FILE_BROWSER_VIEW'),
    ('FILE_BROWSER',      'CREATE', 'إنشاء', 'PERM_FILE_BROWSER_CREATE'),
    ('FILE_BROWSER',      'UPDATE', 'تعديل', 'PERM_FILE_BROWSER_UPDATE'),
    ('FILE_BROWSER',      'DELETE', 'حذف',   'PERM_FILE_BROWSER_DELETE')
) AS v(page_code, action_code, action_ar, permission_code)
JOIN SEC_SCREEN_REG s ON s.PAGE_CODE = v.page_code
WHERE NOT EXISTS (SELECT 1 FROM SEC_ACTION_REG a WHERE a.PERMISSION_CODE = v.permission_code);

-- ------------------------------------------------------------
-- 5. Tier-1 — SEC_ROLE_MODULE_GRANT (RULE-SEC-001: must precede any screen grant).
--    Each module-admin role gets its own module; SYS_ADMIN gets all three.
-- ------------------------------------------------------------
INSERT INTO SEC_ROLE_MODULE_GRANT (ROLE_MODULE_GRANT_PK, ROLE_ID, MODULE_ID, GRANTED_BY, GRANTED_AT)
SELECT nextval('SEQ_SEC_ROLE_MODULE_GRANT'), r.ROLE_PK, m.MODULE_REG_PK, 'SYSTEM', CURRENT_TIMESTAMP
FROM (VALUES
    ('CU_ADMIN',    'CU'),
    ('NOTIF_ADMIN', 'NOTIF'),
    ('FILE_ADMIN',  'FILE'),
    ('SYS_ADMIN',   'CU'),
    ('SYS_ADMIN',   'NOTIF'),
    ('SYS_ADMIN',   'FILE')
) AS v(role_code, module_code)
JOIN SEC_ROLE r       ON r.CODE = v.role_code
JOIN SEC_MODULE_REG m ON m.CODE = v.module_code
WHERE NOT EXISTS (
    SELECT 1 FROM SEC_ROLE_MODULE_GRANT g
    WHERE g.ROLE_ID = r.ROLE_PK AND g.MODULE_ID = m.MODULE_REG_PK);

-- ------------------------------------------------------------
-- 6. Tier-2 — SEC_ROLE_SCREEN_GRANT (RULE-SEC-002). Every screen of the granted module, selected
--    by module rather than by listing page codes, so the set stays correct if a screen is added.
-- ------------------------------------------------------------
INSERT INTO SEC_ROLE_SCREEN_GRANT (ROLE_SCREEN_GRANT_PK, ROLE_ID, SCREEN_ID, GRANTED_BY, GRANTED_AT)
SELECT nextval('SEQ_SEC_ROLE_SCREEN_GRANT'), r.ROLE_PK, s.SCREEN_REG_PK, 'SYSTEM', CURRENT_TIMESTAMP
FROM (VALUES
    ('CU_ADMIN',    'CU'),
    ('NOTIF_ADMIN', 'NOTIF'),
    ('FILE_ADMIN',  'FILE'),
    ('SYS_ADMIN',   'CU'),
    ('SYS_ADMIN',   'NOTIF'),
    ('SYS_ADMIN',   'FILE')
) AS v(role_code, module_code)
JOIN SEC_ROLE r        ON r.CODE = v.role_code
JOIN SEC_MODULE_REG m  ON m.CODE = v.module_code
JOIN SEC_SCREEN_REG s  ON s.MODULE_ID = m.MODULE_REG_PK
WHERE NOT EXISTS (
    SELECT 1 FROM SEC_ROLE_SCREEN_GRANT g
    WHERE g.ROLE_ID = r.ROLE_PK AND g.SCREEN_ID = s.SCREEN_REG_PK);

-- ------------------------------------------------------------
-- 7. Tier-3 — SEC_ROLE_ACTION_GRANT. This is the tier @PreAuthorize actually reads. Every action
--    row seeded in §4 goes to its own module-admin role and to SYS_ADMIN; nothing is withheld
--    (no SoD rule applies to CU/NOTIF/FILE). The grant is scoped to this file's 21 permission
--    codes explicitly, so a role never picks up an unrelated module's actions.
--    RULE-SEC-007 holds on every screen: each screen's ACTION_CODE='VIEW' row is granted by the
--    same statement, so no non-gateway grant is filtered out by MenuService.
-- ------------------------------------------------------------
INSERT INTO SEC_ROLE_ACTION_GRANT (ROLE_ACTION_GRANT_PK, ROLE_ID, ACTION_ID, GRANTED_BY, GRANTED_AT)
SELECT nextval('SEQ_SEC_ROLE_ACTION_GRANT'), r.ROLE_PK, a.ACTION_REG_PK, 'SYSTEM', CURRENT_TIMESTAMP
FROM (VALUES
    ('CU_ADMIN',    'CU'),
    ('NOTIF_ADMIN', 'NOTIF'),
    ('FILE_ADMIN',  'FILE'),
    ('SYS_ADMIN',   'CU'),
    ('SYS_ADMIN',   'NOTIF'),
    ('SYS_ADMIN',   'FILE')
) AS v(role_code, module_code)
JOIN SEC_ROLE r        ON r.CODE = v.role_code
JOIN SEC_MODULE_REG m  ON m.CODE = v.module_code
JOIN SEC_SCREEN_REG s  ON s.MODULE_ID = m.MODULE_REG_PK
JOIN SEC_ACTION_REG a  ON a.SCREEN_ID = s.SCREEN_REG_PK
WHERE a.PERMISSION_CODE IN (
    'CONFIG_VIEW', 'CONFIG_CREATE', 'CONFIG_UPDATE', 'CONFIG_DEACTIVATE',
    'PERM_NOTIF_TEMPLATES_VIEW', 'PERM_NOTIF_TEMPLATES_CREATE',
    'PERM_NOTIF_TEMPLATES_UPDATE', 'PERM_NOTIF_TEMPLATES_DELETE',
    'PERM_NOTIF_CHANNELS_VIEW', 'PERM_NOTIF_CHANNELS_CREATE',
    'PERM_NOTIF_CHANNELS_UPDATE', 'PERM_NOTIF_CHANNELS_DELETE',
    'PERM_NOTIF_LOG_VIEW',
    'PERM_FILE_CATEGORIES_VIEW', 'PERM_FILE_CATEGORIES_CREATE',
    'PERM_FILE_CATEGORIES_UPDATE', 'PERM_FILE_CATEGORIES_DELETE',
    'PERM_FILE_BROWSER_VIEW', 'PERM_FILE_BROWSER_CREATE',
    'PERM_FILE_BROWSER_UPDATE', 'PERM_FILE_BROWSER_DELETE')
  AND NOT EXISTS (
    SELECT 1 FROM SEC_ROLE_ACTION_GRANT g
    WHERE g.ROLE_ID = r.ROLE_PK AND g.ACTION_ID = a.ACTION_REG_PK);
