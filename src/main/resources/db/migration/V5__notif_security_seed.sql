-- V7 — Notification Service (NOTIF) security-registry seed
-- Source: governance/modules/NOTIF/packages/backend-execution/SEC-BE/SEC-BE.md (SECURITY SEED DATA REQUIREMENTS)
--         governance/modules/NOTIF/P1/srs.md STANDALONE "Permissions Summary & Registry Update"
-- Seeds NOTIF into the SEC RBAC registry: NOTIF module, NOTIF_ADMIN role, 3 pages
--   (NOTIF_TEMPLATES, NOTIF_CHANNELS, NOTIF_LOG), 4 CORE-9 permissions per page (12 total),
--   Tier-1 (SEC_ROLE_MODULE) + Tier-2 (SEC_ROLE_PERMISSION) grants.
-- All FKs resolved by natural key (never a hardcoded id). Surrogate PKs from the V2 sequences.
--
-- PERMISSION_CODE convention: PERM_<PAGE_CODE>_<TYPE> (e.g. PERM_NOTIF_TEMPLATES_VIEW), so the seed
--   equals runtime PermissionGenerationDomainService output (RULE-SEC-011 / CORE-9). These values
--   EXACTLY equal the string VALUES of the NOTIF constants in
--   com.erp.security.permission.PermissionConstants so runtime hasAuthority(...) in each service's
--   @PreAuthorize resolves. (SEC-BE reconciled an earlier SVC-API singular/plural mismatch: the
--   constants were PERM_NOTIF_TEMPLATE_*/PERM_NOTIF_CHANNEL_* [SINGULAR] while the page_codes are
--   NOTIF_TEMPLATES/NOTIF_CHANNELS [PLURAL]; the constants were renamed to the PLURAL, page-derived
--   form used here — see execution-state.json api_doc_gaps (SEC-BE).)
--
-- Role-code derivation note: the SRS names the role as "مدير الإشعارات / Notification Administrator"
--   and uses the role short-code NOTIF_ADMIN throughout the B4/Permissions-Summary tables. It is
--   created here with that code (mirrors the short-uppercase SYS_ADMIN pattern).
-- Pre-existing dependency: SYS_ADMIN ('مدير النظام' / 'System Administrator') was already seeded by
--   V3 (SEC seed). It is NOT re-inserted here — it is referenced by natural key (ROLE_CODE = 'SYS_ADMIN').
--
-- NOTIF_LOG is a VIEW-only system record (SCR-NOTIF-003 exposes VIEW only). The full 4-per-page
--   permission set is still SEEDED for NOTIF_LOG to match runtime generation (RULE-SEC-011 / CORE-9
--   always generates 4), but only PERM_NOTIF_LOG_VIEW is GRANTED to any role. The CREATE/UPDATE/DELETE
--   rows exist unused and harmless.
--
-- Nav-parent note: SRS lists "parent: الإشعارات (Notifications)" for all 3 pages. That is a
--   frontend-navigation concern; PARENT_PAGE_FK is left NULL here — a parent nav page would have to
--   be invented (no such SEC_PAGE row exists), and nothing invented is seeded.
--
-- Dispatch note: dispatch (API-NOTIF-001) is a service/event endpoint behind the Security filter,
--   NOT tied to a management screen (SEC-BE.md / RULE-NOTIF-005). SEC_PERMISSION.PAGE_FK is NOT NULL,
--   so a dispatch permission cannot be seeded as a page permission and must not be forced onto an
--   unrelated page. No dispatch permission is seeded; the dispatch gate is now @PreAuthorize
--   isAuthenticated() (any authenticated principal) — see execution-state.json api_doc_gaps (SEC-BE).

-- 1. SEC_MODULE — module NOTIF (Tier-1 grantable unit)
INSERT INTO SEC_MODULE (ID, MODULE_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_MODULE'), 'NOTIF', 'الإشعارات', 'Notifications', 1, 'SYSTEM', CURRENT_TIMESTAMP);

-- 2. SEC_ROLE — NOTIF_ADMIN (new). SYS_ADMIN already exists (V3) — not inserted here.
INSERT INTO SEC_ROLE (ID, ROLE_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_ROLE'), 'NOTIF_ADMIN', 'مدير الإشعارات', 'Notification Administrator', 1, 'SYSTEM', CURRENT_TIMESTAMP);

-- 3. SEC_PAGE — 3 screens, MODULE_FK -> NOTIF, no parent nav (PARENT_PAGE_FK NULL)
INSERT INTO SEC_PAGE (ID, PAGE_CODE, NAME_AR, NAME_EN, MODULE_FK, PARENT_PAGE_FK, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_PAGE'), 'NOTIF_TEMPLATES', 'إدارة قوالب الإشعارات', 'Notification Templates',
        (SELECT ID FROM SEC_MODULE WHERE MODULE_CODE = 'NOTIF'), NULL, 1, 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_PAGE (ID, PAGE_CODE, NAME_AR, NAME_EN, MODULE_FK, PARENT_PAGE_FK, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_PAGE'), 'NOTIF_CHANNELS', 'تهيئة القنوات', 'Channel Configuration',
        (SELECT ID FROM SEC_MODULE WHERE MODULE_CODE = 'NOTIF'), NULL, 1, 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_PAGE (ID, PAGE_CODE, NAME_AR, NAME_EN, MODULE_FK, PARENT_PAGE_FK, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_PAGE'), 'NOTIF_LOG', 'سجل الإشعارات', 'Notification Log',
        (SELECT ID FROM SEC_MODULE WHERE MODULE_CODE = 'NOTIF'), NULL, 1, 'SYSTEM', CURRENT_TIMESTAMP);

-- 4. SEC_PERMISSION — 4 per page (CORE-9, 12 total). code = PERM_<PAGE_CODE>_<TYPE>;
--    name = <page name> - <TYPE> (matches PermissionGenerationDomainService so the seed equals runtime).

-- 4a. NOTIF_TEMPLATES page -> PERM_NOTIF_TEMPLATES_{VIEW,CREATE,UPDATE,DELETE}
INSERT INTO SEC_PERMISSION (ID, PERMISSION_CODE, PERMISSION_TYPE, NAME_AR, NAME_EN, IS_ACTIVE_FL, PAGE_FK, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_PERMISSION'), 'PERM_NOTIF_TEMPLATES_VIEW', 'VIEW', 'إدارة قوالب الإشعارات - VIEW', 'Notification Templates - VIEW', 1,
        (SELECT ID FROM SEC_PAGE WHERE PAGE_CODE = 'NOTIF_TEMPLATES'), 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_PERMISSION (ID, PERMISSION_CODE, PERMISSION_TYPE, NAME_AR, NAME_EN, IS_ACTIVE_FL, PAGE_FK, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_PERMISSION'), 'PERM_NOTIF_TEMPLATES_CREATE', 'CREATE', 'إدارة قوالب الإشعارات - CREATE', 'Notification Templates - CREATE', 1,
        (SELECT ID FROM SEC_PAGE WHERE PAGE_CODE = 'NOTIF_TEMPLATES'), 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_PERMISSION (ID, PERMISSION_CODE, PERMISSION_TYPE, NAME_AR, NAME_EN, IS_ACTIVE_FL, PAGE_FK, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_PERMISSION'), 'PERM_NOTIF_TEMPLATES_UPDATE', 'UPDATE', 'إدارة قوالب الإشعارات - UPDATE', 'Notification Templates - UPDATE', 1,
        (SELECT ID FROM SEC_PAGE WHERE PAGE_CODE = 'NOTIF_TEMPLATES'), 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_PERMISSION (ID, PERMISSION_CODE, PERMISSION_TYPE, NAME_AR, NAME_EN, IS_ACTIVE_FL, PAGE_FK, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_PERMISSION'), 'PERM_NOTIF_TEMPLATES_DELETE', 'DELETE', 'إدارة قوالب الإشعارات - DELETE', 'Notification Templates - DELETE', 1,
        (SELECT ID FROM SEC_PAGE WHERE PAGE_CODE = 'NOTIF_TEMPLATES'), 'SYSTEM', CURRENT_TIMESTAMP);

-- 4b. NOTIF_CHANNELS page -> PERM_NOTIF_CHANNELS_{VIEW,CREATE,UPDATE,DELETE}
INSERT INTO SEC_PERMISSION (ID, PERMISSION_CODE, PERMISSION_TYPE, NAME_AR, NAME_EN, IS_ACTIVE_FL, PAGE_FK, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_PERMISSION'), 'PERM_NOTIF_CHANNELS_VIEW', 'VIEW', 'تهيئة القنوات - VIEW', 'Channel Configuration - VIEW', 1,
        (SELECT ID FROM SEC_PAGE WHERE PAGE_CODE = 'NOTIF_CHANNELS'), 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_PERMISSION (ID, PERMISSION_CODE, PERMISSION_TYPE, NAME_AR, NAME_EN, IS_ACTIVE_FL, PAGE_FK, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_PERMISSION'), 'PERM_NOTIF_CHANNELS_CREATE', 'CREATE', 'تهيئة القنوات - CREATE', 'Channel Configuration - CREATE', 1,
        (SELECT ID FROM SEC_PAGE WHERE PAGE_CODE = 'NOTIF_CHANNELS'), 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_PERMISSION (ID, PERMISSION_CODE, PERMISSION_TYPE, NAME_AR, NAME_EN, IS_ACTIVE_FL, PAGE_FK, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_PERMISSION'), 'PERM_NOTIF_CHANNELS_UPDATE', 'UPDATE', 'تهيئة القنوات - UPDATE', 'Channel Configuration - UPDATE', 1,
        (SELECT ID FROM SEC_PAGE WHERE PAGE_CODE = 'NOTIF_CHANNELS'), 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_PERMISSION (ID, PERMISSION_CODE, PERMISSION_TYPE, NAME_AR, NAME_EN, IS_ACTIVE_FL, PAGE_FK, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_PERMISSION'), 'PERM_NOTIF_CHANNELS_DELETE', 'DELETE', 'تهيئة القنوات - DELETE', 'Channel Configuration - DELETE', 1,
        (SELECT ID FROM SEC_PAGE WHERE PAGE_CODE = 'NOTIF_CHANNELS'), 'SYSTEM', CURRENT_TIMESTAMP);

-- 4c. NOTIF_LOG page -> PERM_NOTIF_LOG_{VIEW,CREATE,UPDATE,DELETE}
--     Only PERM_NOTIF_LOG_VIEW is a Java constant / used by @PreAuthorize and granted below;
--     the other 3 exist only to match runtime 4-per-page generation and are never granted.
INSERT INTO SEC_PERMISSION (ID, PERMISSION_CODE, PERMISSION_TYPE, NAME_AR, NAME_EN, IS_ACTIVE_FL, PAGE_FK, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_PERMISSION'), 'PERM_NOTIF_LOG_VIEW', 'VIEW', 'سجل الإشعارات - VIEW', 'Notification Log - VIEW', 1,
        (SELECT ID FROM SEC_PAGE WHERE PAGE_CODE = 'NOTIF_LOG'), 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_PERMISSION (ID, PERMISSION_CODE, PERMISSION_TYPE, NAME_AR, NAME_EN, IS_ACTIVE_FL, PAGE_FK, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_PERMISSION'), 'PERM_NOTIF_LOG_CREATE', 'CREATE', 'سجل الإشعارات - CREATE', 'Notification Log - CREATE', 1,
        (SELECT ID FROM SEC_PAGE WHERE PAGE_CODE = 'NOTIF_LOG'), 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_PERMISSION (ID, PERMISSION_CODE, PERMISSION_TYPE, NAME_AR, NAME_EN, IS_ACTIVE_FL, PAGE_FK, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_PERMISSION'), 'PERM_NOTIF_LOG_UPDATE', 'UPDATE', 'سجل الإشعارات - UPDATE', 'Notification Log - UPDATE', 1,
        (SELECT ID FROM SEC_PAGE WHERE PAGE_CODE = 'NOTIF_LOG'), 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_PERMISSION (ID, PERMISSION_CODE, PERMISSION_TYPE, NAME_AR, NAME_EN, IS_ACTIVE_FL, PAGE_FK, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_PERMISSION'), 'PERM_NOTIF_LOG_DELETE', 'DELETE', 'سجل الإشعارات - DELETE', 'Notification Log - DELETE', 1,
        (SELECT ID FROM SEC_PAGE WHERE PAGE_CODE = 'NOTIF_LOG'), 'SYSTEM', CURRENT_TIMESTAMP);

-- 5. SEC_ROLE_MODULE — Tier-1 grants (NOTIF_ADMIN -> NOTIF, SYS_ADMIN -> NOTIF).
--    MUST precede Tier-2 grants (RULE-SEC-014 prerequisite: no orphan screen permission).
INSERT INTO SEC_ROLE_MODULE (ROLE_FK, MODULE_FK)
VALUES ((SELECT ID FROM SEC_ROLE WHERE ROLE_CODE = 'NOTIF_ADMIN'),
        (SELECT ID FROM SEC_MODULE WHERE MODULE_CODE = 'NOTIF'));
INSERT INTO SEC_ROLE_MODULE (ROLE_FK, MODULE_FK)
VALUES ((SELECT ID FROM SEC_ROLE WHERE ROLE_CODE = 'SYS_ADMIN'),
        (SELECT ID FROM SEC_MODULE WHERE MODULE_CODE = 'NOTIF'));

-- 6. SEC_ROLE_PERMISSION — Tier-2: grant to BOTH NOTIF_ADMIN and SYS_ADMIN:
--    all 4 TEMPLATES perms, all 4 CHANNELS perms, and PERM_NOTIF_LOG_VIEW ONLY (9 total each role).
--    NOTIF_LOG CREATE/UPDATE/DELETE are deliberately NOT granted (VIEW-only system record).
INSERT INTO SEC_ROLE_PERMISSION (ROLE_FK, PERMISSION_FK)
SELECT (SELECT ID FROM SEC_ROLE WHERE ROLE_CODE = 'NOTIF_ADMIN'), p.ID
FROM SEC_PERMISSION p
WHERE p.PERMISSION_CODE IN (
  'PERM_NOTIF_TEMPLATES_VIEW', 'PERM_NOTIF_TEMPLATES_CREATE', 'PERM_NOTIF_TEMPLATES_UPDATE', 'PERM_NOTIF_TEMPLATES_DELETE',
  'PERM_NOTIF_CHANNELS_VIEW', 'PERM_NOTIF_CHANNELS_CREATE', 'PERM_NOTIF_CHANNELS_UPDATE', 'PERM_NOTIF_CHANNELS_DELETE',
  'PERM_NOTIF_LOG_VIEW'
);
INSERT INTO SEC_ROLE_PERMISSION (ROLE_FK, PERMISSION_FK)
SELECT (SELECT ID FROM SEC_ROLE WHERE ROLE_CODE = 'SYS_ADMIN'), p.ID
FROM SEC_PERMISSION p
WHERE p.PERMISSION_CODE IN (
  'PERM_NOTIF_TEMPLATES_VIEW', 'PERM_NOTIF_TEMPLATES_CREATE', 'PERM_NOTIF_TEMPLATES_UPDATE', 'PERM_NOTIF_TEMPLATES_DELETE',
  'PERM_NOTIF_CHANNELS_VIEW', 'PERM_NOTIF_CHANNELS_CREATE', 'PERM_NOTIF_CHANNELS_UPDATE', 'PERM_NOTIF_CHANNELS_DELETE',
  'PERM_NOTIF_LOG_VIEW'
);
