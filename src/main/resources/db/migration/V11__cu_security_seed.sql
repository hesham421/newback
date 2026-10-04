-- V13 — Common Utils (CU) security-registry seed
-- Purpose: make the CU configuration-management endpoints reachable. Every method on
--   ConfigurationService carries @PreAuthorize hasAuthority(CONFIG_VIEW/CREATE/UPDATE/DEACTIVATE)
--   (see PermissionConstants + SEC-BE.md verification 2026-09-04), but NO SEC_PERMISSION rows for
--   those authorities were ever seeded, and no role was granted them. Result: the endpoints were
--   unreachable by every user, including SYS_ADMIN — a 403 for everyone. This seed closes that gap.
--
-- Governance note (deliberate, human-approved deviation): CU/SEC-BE.md classifies CU as backend-only
--   with no screens and states "SECURITY SEED DATA REQUIREMENTS: none", while ALSO delegating the
--   concrete authority mechanism to the SEC module ("CU declares the enforcement requirement, SEC
--   provides the mechanism"; SEC-IMPL-RULE-3 "DEFERRED TO SEC MODULE"). This migration is that
--   deferred SEC-side mechanism. Because SEC_PERMISSION.PAGE_FK is NOT NULL, the four CONFIG_*
--   permissions are anchored to one backend-only page (CU_CONFIGURATIONS, no nav parent) that exists
--   solely to hold them — it is not a rendered CU screen. Approved via explicit human decision on
--   2026-09-06 in response to the TestSprite TC009 finding.
--
-- PERMISSION_CODE convention: CU intentionally deviates from the CORE-9 PERM_<PAGE_CODE>_<TYPE> naming
--   used by FILE/NOTIF. The four codes below EXACTLY equal the string VALUES of the CU constants in
--   com.erp.security.permission.PermissionConstants (CONFIG_VIEW/CREATE/UPDATE/DEACTIVATE, value==name)
--   so runtime hasAuthority(...) in ConfigurationService's @PreAuthorize resolves. Do NOT rename them.
--
-- Pattern: mirrors V9 (FILE) / V7 (NOTIF) — new module + new <MOD>_ADMIN role, permissions granted to
--   both the module-admin role and the pre-existing SYS_ADMIN (V3), all FKs resolved by natural key,
--   surrogate PKs from the V2 SEQ_SEC_* sequences. Tier-1 (SEC_ROLE_MODULE) grants precede Tier-2
--   (SEC_ROLE_PERMISSION) grants (RULE-SEC-014: no orphan screen permission).

-- 1. SEC_MODULE — module CU (Tier-1 grantable unit)
INSERT INTO SEC_MODULE (ID, MODULE_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_MODULE'), 'CU', 'الأدوات المشتركة', 'Common Utilities', 1, 'SYSTEM', CURRENT_TIMESTAMP);

-- 2. SEC_ROLE — CU_ADMIN (new). SYS_ADMIN already exists (V3) — not inserted here.
INSERT INTO SEC_ROLE (ID, ROLE_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_ROLE'), 'CU_ADMIN', 'مدير الإعدادات', 'Configuration Administrator', 1, 'SYSTEM', CURRENT_TIMESTAMP);

-- 3. SEC_PAGE — one backend-only holder page (no rendered screen, no nav parent).
INSERT INTO SEC_PAGE (ID, PAGE_CODE, NAME_AR, NAME_EN, MODULE_FK, PARENT_PAGE_FK, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_PAGE'), 'CU_CONFIGURATIONS', 'إدارة إعدادات المنصة', 'Platform Configuration',
        (SELECT ID FROM SEC_MODULE WHERE MODULE_CODE = 'CU'), NULL, 1, 'SYSTEM', CURRENT_TIMESTAMP);

-- 4. SEC_PERMISSION — the four CU authorities. Codes == PermissionConstants CONFIG_* values.
--    PERMISSION_TYPE uses the standard CRUD vocabulary already in use (VIEW/CREATE/UPDATE/DELETE);
--    the soft-delete authority (CONFIG_DEACTIVATE) is typed DELETE.
INSERT INTO SEC_PERMISSION (ID, PERMISSION_CODE, PERMISSION_TYPE, NAME_AR, NAME_EN, IS_ACTIVE_FL, PAGE_FK, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_PERMISSION'), 'CONFIG_VIEW', 'VIEW', 'إدارة إعدادات المنصة - VIEW', 'Platform Configuration - VIEW', 1,
        (SELECT ID FROM SEC_PAGE WHERE PAGE_CODE = 'CU_CONFIGURATIONS'), 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_PERMISSION (ID, PERMISSION_CODE, PERMISSION_TYPE, NAME_AR, NAME_EN, IS_ACTIVE_FL, PAGE_FK, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_PERMISSION'), 'CONFIG_CREATE', 'CREATE', 'إدارة إعدادات المنصة - CREATE', 'Platform Configuration - CREATE', 1,
        (SELECT ID FROM SEC_PAGE WHERE PAGE_CODE = 'CU_CONFIGURATIONS'), 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_PERMISSION (ID, PERMISSION_CODE, PERMISSION_TYPE, NAME_AR, NAME_EN, IS_ACTIVE_FL, PAGE_FK, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_PERMISSION'), 'CONFIG_UPDATE', 'UPDATE', 'إدارة إعدادات المنصة - UPDATE', 'Platform Configuration - UPDATE', 1,
        (SELECT ID FROM SEC_PAGE WHERE PAGE_CODE = 'CU_CONFIGURATIONS'), 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_PERMISSION (ID, PERMISSION_CODE, PERMISSION_TYPE, NAME_AR, NAME_EN, IS_ACTIVE_FL, PAGE_FK, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_PERMISSION'), 'CONFIG_DEACTIVATE', 'DELETE', 'إدارة إعدادات المنصة - DEACTIVATE', 'Platform Configuration - DEACTIVATE', 1,
        (SELECT ID FROM SEC_PAGE WHERE PAGE_CODE = 'CU_CONFIGURATIONS'), 'SYSTEM', CURRENT_TIMESTAMP);

-- 5. SEC_ROLE_MODULE — Tier-1 grants (CU_ADMIN -> CU, SYS_ADMIN -> CU). Must precede Tier-2.
INSERT INTO SEC_ROLE_MODULE (ROLE_FK, MODULE_FK)
VALUES ((SELECT ID FROM SEC_ROLE WHERE ROLE_CODE = 'CU_ADMIN'),
        (SELECT ID FROM SEC_MODULE WHERE MODULE_CODE = 'CU'));
INSERT INTO SEC_ROLE_MODULE (ROLE_FK, MODULE_FK)
VALUES ((SELECT ID FROM SEC_ROLE WHERE ROLE_CODE = 'SYS_ADMIN'),
        (SELECT ID FROM SEC_MODULE WHERE MODULE_CODE = 'CU'));

-- 6. SEC_ROLE_PERMISSION — Tier-2: grant all four CONFIG_* to BOTH CU_ADMIN and SYS_ADMIN.
INSERT INTO SEC_ROLE_PERMISSION (ROLE_FK, PERMISSION_FK)
SELECT (SELECT ID FROM SEC_ROLE WHERE ROLE_CODE = 'CU_ADMIN'), p.ID
FROM SEC_PERMISSION p
WHERE p.PERMISSION_CODE IN ('CONFIG_VIEW', 'CONFIG_CREATE', 'CONFIG_UPDATE', 'CONFIG_DEACTIVATE');
INSERT INTO SEC_ROLE_PERMISSION (ROLE_FK, PERMISSION_FK)
SELECT (SELECT ID FROM SEC_ROLE WHERE ROLE_CODE = 'SYS_ADMIN'), p.ID
FROM SEC_PERMISSION p
WHERE p.PERMISSION_CODE IN ('CONFIG_VIEW', 'CONFIG_CREATE', 'CONFIG_UPDATE', 'CONFIG_DEACTIVATE');
