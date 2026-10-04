-- ============================================================
-- V7 — Security (SEC) seed: the module/screen/action registry of every core module
--      (SEC, MDL, NOTIF, FILE, CU), the four roles, their three-tier grants, and the
--      bootstrap 'admin' account.
-- Target: POSTGRESQL_16 | schema: V4__sec_schema.sql
-- Squashed in erp-core step 04 from the FINAL state the old chain left behind. Old-chain file
--   names in the pre-step-01 numbering (the post-step-01 name in brackets):
--     V17__sec_security_seed.sql                [V14]  SEC registry, SYS_ADMIN, its grants, admin user
--     V19__mdl_security_seed.sql                [V16]  MDL registry
--     V20__notif_file_lookup_data_migration.sql [V17]  NOTIF / FILE module rows (its lookups → V8)
--     V21__mdl_role_grants.sql                  [V18]  SYS_ADMIN grants on MDL
--     V31__cu_notif_file_security_seed.sql      [V19]  CU module, CU/NOTIF/FILE registry, module-admin roles, grants
--     V10__sec_bootstrap_admin_user.sql         [V8]   the admin account (superseded by V17's copy)
--   Old V3/V7/V9/V13 [V3/V5/V7/V11] seeded the LEGACY SEC tables (SEC_MODULE/SEC_PAGE/
--   SEC_PERMISSION...) that old V14 [V12] dropped again: nothing of them survives except the
--   bilingual role and screen names that V31 had already re-used, so they contribute no rows here.
--
-- Rules every row below follows:
--   * surrogate PKs come from the V4 SEQ_SEC_* sequences; every FK is resolved by natural key
--     (CODE / PAGE_CODE / PERMISSION_CODE / USERNAME), never by a hard-coded id;
--   * PERMISSION_CODE = PERM_<PAGE_CODE>_<ACTION_CODE> (RegistryService's derivation,
--     DBF-SEC-050), except CU's four codes (CONFIG_*), a recorded decision kept verbatim because
--     they equal the CU constants in PermissionConstants that hasAuthority(...) compares against;
--   * action NAME_AR / NAME_EN = '<screen name> - <action>' (the shape every old seed used);
--   * only the cells each module's SEC-BE permission matrix names are registered (no action row
--     for SEC_LOGIN / SEC_SIGNUP / SEC_PWD_RESET, which are public, pre-authentication screens).
-- Booleans are native (V4 uses BOOLEAN).
-- ============================================================

-- ------------------------------------------------------------
-- 1. SEC_MODULE_REG — the five core modules (Tier-1 grantable units).
-- ------------------------------------------------------------
INSERT INTO SEC_MODULE_REG (MODULE_REG_PK, CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
SELECT nextval('SEQ_SEC_MODULE_REG'), v.code, v.name_ar, v.name_en, TRUE, 'SYSTEM', CURRENT_TIMESTAMP
FROM (VALUES
    (1, 'SEC',   'الأمان',             'Security'),
    (2, 'MDL',   'البيانات المرجعية',  'Master Data Lookup'),
    (3, 'NOTIF', 'خدمة الإشعارات',     'Notification Service'),
    (4, 'FILE',  'خدمة الملفات',       'File Service'),
    (5, 'CU',    'الأدوات المشتركة',   'Common Utilities')
) AS v(ord, code, name_ar, name_en)
ORDER BY v.ord;

-- ------------------------------------------------------------
-- 2. SEC_SCREEN_REG — 17 screens.
--    SEC: the 9 page codes of SEC's matrix (SEC_LOGIN / SEC_SIGNUP / SEC_PWD_RESET are public and
--    carry no action and no grant; they are registered so the registry is complete).
--    CU_CONFIGURATIONS is a backend-only holder screen (CU has no rendered screen) that exists
--    only to anchor CU's four authorities, since SEC_ACTION_REG.SCREEN_ID is NOT NULL.
-- ------------------------------------------------------------
INSERT INTO SEC_SCREEN_REG (SCREEN_REG_PK, PAGE_CODE, MODULE_ID, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
SELECT nextval('SEQ_SEC_SCREEN_REG'), v.page_code, m.MODULE_REG_PK, v.name_ar, v.name_en,
       TRUE, 'SYSTEM', CURRENT_TIMESTAMP
FROM (VALUES
    ( 1, 'SEC',   'SEC_LOGIN',           'تسجيل الدخول',                    'Login'),
    ( 2, 'SEC',   'SEC_SIGNUP',          'إنشاء حساب',                      'Sign-up'),
    ( 3, 'SEC',   'SEC_PWD_RESET',       'إعادة تعيين كلمة المرور',         'Forgot/reset password'),
    ( 4, 'SEC',   'SEC_USERS',           'المستخدمون',                      'Users'),
    ( 5, 'SEC',   'SEC_ROLES',           'الأدوار والصلاحيات',              'Roles & permissions'),
    ( 6, 'SEC',   'SEC_MODULE_REGISTRY', 'سجل الوحدات والشاشات والإجراءات', 'Module/screen/action registry'),
    ( 7, 'SEC',   'SEC_DASHBOARD',       'لوحة تحكم المشرف',                'Admin dashboard'),
    ( 8, 'SEC',   'SEC_AUDIT_LOG',       'سجل التدقيق',                     'Audit log'),
    ( 9, 'SEC',   'SEC_SESSIONS',        'الجلسات النشطة',                  'Active sessions'),
    (10, 'MDL',   'MDL_LOOKUPS',         'قوائم البيانات المرجعية',         'Master data lookups'),
    (11, 'MDL',   'MDL_TYPE_REGISTRY',   'سجل أنواع البيانات المرجعية',     'Master data type registry'),
    (12, 'CU',    'CU_CONFIGURATIONS',   'إدارة إعدادات المنصة',            'Platform Configuration'),
    (13, 'NOTIF', 'NOTIF_TEMPLATES',     'إدارة قوالب الإشعارات',           'Notification Templates'),
    (14, 'NOTIF', 'NOTIF_CHANNELS',      'تهيئة القنوات',                   'Channel Configuration'),
    (15, 'NOTIF', 'NOTIF_LOG',           'سجل الإشعارات',                   'Notification Log'),
    (16, 'FILE',  'FILE_CATEGORIES',     'إدارة فئات الملفات',              'File Categories'),
    (17, 'FILE',  'FILE_BROWSER',        'مستعرض الملفات',                  'File Browser')
) AS v(ord, module_code, page_code, name_ar, name_en)
JOIN SEC_MODULE_REG m ON m.CODE = v.module_code
ORDER BY v.ord;

-- ------------------------------------------------------------
-- 3. SEC_ACTION_REG — 38 actions: exactly the cells the modules' SEC-BE matrices name.
--    Deliberately absent: SEC_USERS DELETE, SEC_MODULE_REGISTRY CREATE, SEC_DASHBOARD /
--    SEC_AUDIT_LOG CREATE/UPDATE/DELETE, SEC_SESSIONS CREATE/UPDATE, MDL_LOOKUPS DELETE
--    (deactivate is UPDATE), MDL_TYPE_REGISTRY CREATE/UPDATE/DELETE, NOTIF_LOG CREATE/UPDATE/DELETE.
--    Registered although no @PreAuthorize consumes them today: PERM_SEC_ROLES_DELETE (reserved),
--    PERM_FILE_BROWSER_UPDATE / _DELETE (FileService.softDelete picks archive vs delete by argument).
--    PERM_SEC_MODULE_REGISTRY_UPDATE gates the registry write endpoints (API-SEC-018/019/020).
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
    -- SEC
    ( 1, 'SEC_USERS',           'VIEW',       'عرض',         'PERM_SEC_USERS_VIEW'),
    ( 2, 'SEC_USERS',           'CREATE',     'إنشاء',       'PERM_SEC_USERS_CREATE'),
    ( 3, 'SEC_USERS',           'UPDATE',     'تعديل',       'PERM_SEC_USERS_UPDATE'),
    ( 4, 'SEC_ROLES',           'VIEW',       'عرض',         'PERM_SEC_ROLES_VIEW'),
    ( 5, 'SEC_ROLES',           'CREATE',     'إنشاء',       'PERM_SEC_ROLES_CREATE'),
    ( 6, 'SEC_ROLES',           'UPDATE',     'تعديل',       'PERM_SEC_ROLES_UPDATE'),
    ( 7, 'SEC_ROLES',           'DELETE',     'حذف',         'PERM_SEC_ROLES_DELETE'),
    ( 8, 'SEC_MODULE_REGISTRY', 'VIEW',       'عرض',         'PERM_SEC_MODULE_REGISTRY_VIEW'),
    ( 9, 'SEC_MODULE_REGISTRY', 'UPDATE',     'تعديل',       'PERM_SEC_MODULE_REGISTRY_UPDATE'),
    (10, 'SEC_DASHBOARD',       'VIEW',       'عرض',         'PERM_SEC_DASHBOARD_VIEW'),
    (11, 'SEC_AUDIT_LOG',       'VIEW',       'عرض',         'PERM_SEC_AUDIT_LOG_VIEW'),
    (12, 'SEC_SESSIONS',        'VIEW',       'عرض',         'PERM_SEC_SESSIONS_VIEW'),
    (13, 'SEC_SESSIONS',        'DELETE',     'إنهاء',       'PERM_SEC_SESSIONS_DELETE'),
    -- MDL
    (14, 'MDL_LOOKUPS',         'VIEW',       'عرض',         'PERM_MDL_LOOKUPS_VIEW'),
    (15, 'MDL_LOOKUPS',         'CREATE',     'إنشاء',       'PERM_MDL_LOOKUPS_CREATE'),
    (16, 'MDL_LOOKUPS',         'UPDATE',     'تعديل',       'PERM_MDL_LOOKUPS_UPDATE'),
    (17, 'MDL_TYPE_REGISTRY',   'VIEW',       'عرض',         'PERM_MDL_TYPE_REGISTRY_VIEW'),
    -- CU (codes deliberately not PERM_<PAGE>_<ACTION>; see the header)
    (18, 'CU_CONFIGURATIONS',   'VIEW',       'عرض',         'CONFIG_VIEW'),
    (19, 'CU_CONFIGURATIONS',   'CREATE',     'إنشاء',       'CONFIG_CREATE'),
    (20, 'CU_CONFIGURATIONS',   'UPDATE',     'تعديل',       'CONFIG_UPDATE'),
    (21, 'CU_CONFIGURATIONS',   'DEACTIVATE', 'إلغاء تفعيل', 'CONFIG_DEACTIVATE'),
    -- NOTIF
    (22, 'NOTIF_TEMPLATES',     'VIEW',       'عرض',         'PERM_NOTIF_TEMPLATES_VIEW'),
    (23, 'NOTIF_TEMPLATES',     'CREATE',     'إنشاء',       'PERM_NOTIF_TEMPLATES_CREATE'),
    (24, 'NOTIF_TEMPLATES',     'UPDATE',     'تعديل',       'PERM_NOTIF_TEMPLATES_UPDATE'),
    (25, 'NOTIF_TEMPLATES',     'DELETE',     'حذف',         'PERM_NOTIF_TEMPLATES_DELETE'),
    (26, 'NOTIF_CHANNELS',      'VIEW',       'عرض',         'PERM_NOTIF_CHANNELS_VIEW'),
    (27, 'NOTIF_CHANNELS',      'CREATE',     'إنشاء',       'PERM_NOTIF_CHANNELS_CREATE'),
    (28, 'NOTIF_CHANNELS',      'UPDATE',     'تعديل',       'PERM_NOTIF_CHANNELS_UPDATE'),
    (29, 'NOTIF_CHANNELS',      'DELETE',     'حذف',         'PERM_NOTIF_CHANNELS_DELETE'),
    (30, 'NOTIF_LOG',           'VIEW',       'عرض',         'PERM_NOTIF_LOG_VIEW'),
    -- FILE
    (31, 'FILE_CATEGORIES',     'VIEW',       'عرض',         'PERM_FILE_CATEGORIES_VIEW'),
    (32, 'FILE_CATEGORIES',     'CREATE',     'إنشاء',       'PERM_FILE_CATEGORIES_CREATE'),
    (33, 'FILE_CATEGORIES',     'UPDATE',     'تعديل',       'PERM_FILE_CATEGORIES_UPDATE'),
    (34, 'FILE_CATEGORIES',     'DELETE',     'حذف',         'PERM_FILE_CATEGORIES_DELETE'),
    (35, 'FILE_BROWSER',        'VIEW',       'عرض',         'PERM_FILE_BROWSER_VIEW'),
    (36, 'FILE_BROWSER',        'CREATE',     'إنشاء',       'PERM_FILE_BROWSER_CREATE'),
    (37, 'FILE_BROWSER',        'UPDATE',     'تعديل',       'PERM_FILE_BROWSER_UPDATE'),
    (38, 'FILE_BROWSER',        'DELETE',     'حذف',         'PERM_FILE_BROWSER_DELETE')
) AS v(ord, page_code, action_code, action_ar, permission_code)
JOIN SEC_SCREEN_REG s ON s.PAGE_CODE = v.page_code
ORDER BY v.ord;

-- ------------------------------------------------------------
-- 4. SEC_ROLE — SYS_ADMIN (the bootstrap role: every core permission) and one admin role per
--    legacy-path module, each holding only its own module's permissions.
-- ------------------------------------------------------------
INSERT INTO SEC_ROLE (ROLE_PK, CODE, NAME_AR, NAME_EN, DESCRIPTION_AR, DESCRIPTION_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
SELECT nextval('SEQ_SEC_ROLE'), v.code, v.name_ar, v.name_en, v.description_ar, v.description_en,
       TRUE, 'SYSTEM', CURRENT_TIMESTAMP
FROM (VALUES
    (1, 'SYS_ADMIN',   'مدير النظام',    'System Administrator',
        'دور التهيئة الأولية — يملك كامل صلاحيات وحدة الأمان', 'Bootstrap role — holds the full Security module surface'),
    (2, 'CU_ADMIN',    'مدير الإعدادات', 'Configuration Administrator', NULL, NULL),
    (3, 'NOTIF_ADMIN', 'مدير الإشعارات', 'Notification Administrator',  NULL, NULL),
    (4, 'FILE_ADMIN',  'مدير الملفات',   'File Administrator',          NULL, NULL)
) AS v(ord, code, name_ar, name_en, description_ar, description_en)
ORDER BY v.ord;

-- ------------------------------------------------------------
-- 5. Role → module scope used by the three grant tiers below:
--    SYS_ADMIN holds all five modules; each module-admin role holds its own module.
--    Ordering: Tier-1 SEC_ROLE_MODULE_GRANT, then Tier-2 SEC_ROLE_SCREEN_GRANT (RULE-SEC-001/002),
--    then Tier-3 SEC_ROLE_ACTION_GRANT — the tier @PreAuthorize reads
--    (JwtAuthenticationFilter ← MenuService.effectivePermissionCodes); Tiers 1-2 drive the
--    navigation menu and keep the registry self-consistent.
-- ------------------------------------------------------------

-- Tier-1 — module grants (8)
INSERT INTO SEC_ROLE_MODULE_GRANT (ROLE_MODULE_GRANT_PK, ROLE_ID, MODULE_ID, GRANTED_BY, GRANTED_AT)
SELECT nextval('SEQ_SEC_ROLE_MODULE_GRANT'), r.ROLE_PK, m.MODULE_REG_PK, 'SYSTEM', CURRENT_TIMESTAMP
FROM (VALUES
    (1, 'SYS_ADMIN',   'SEC'),
    (2, 'SYS_ADMIN',   'MDL'),
    (3, 'CU_ADMIN',    'CU'),
    (4, 'NOTIF_ADMIN', 'NOTIF'),
    (5, 'FILE_ADMIN',  'FILE'),
    (6, 'SYS_ADMIN',   'CU'),
    (7, 'SYS_ADMIN',   'NOTIF'),
    (8, 'SYS_ADMIN',   'FILE')
) AS v(ord, role_code, module_code)
JOIN SEC_ROLE r       ON r.CODE = v.role_code
JOIN SEC_MODULE_REG m ON m.CODE = v.module_code
ORDER BY v.ord;

-- Tier-2 — screen grants (20): every screen of a granted module that carries at least one action.
--   This excludes exactly the three public SEC screens, which carry no permission, so granting
--   them would assert a right that does not exist.
INSERT INTO SEC_ROLE_SCREEN_GRANT (ROLE_SCREEN_GRANT_PK, ROLE_ID, SCREEN_ID, GRANTED_BY, GRANTED_AT)
SELECT nextval('SEQ_SEC_ROLE_SCREEN_GRANT'), g.ROLE_ID, s.SCREEN_REG_PK, 'SYSTEM', CURRENT_TIMESTAMP
FROM SEC_ROLE_MODULE_GRANT g
JOIN SEC_SCREEN_REG s ON s.MODULE_ID = g.MODULE_ID
WHERE EXISTS (SELECT 1 FROM SEC_ACTION_REG a WHERE a.SCREEN_ID = s.SCREEN_REG_PK)
ORDER BY g.ROLE_MODULE_GRANT_PK, s.SCREEN_REG_PK;

-- Tier-3 — action grants (59): every action of every granted screen. RULE-SEC-002 holds (each
--   action's screen is granted above) and RULE-SEC-007 holds (every screen carrying a non-VIEW
--   action also carries its VIEW, granted by the same statement).
INSERT INTO SEC_ROLE_ACTION_GRANT (ROLE_ACTION_GRANT_PK, ROLE_ID, ACTION_ID, GRANTED_BY, GRANTED_AT)
SELECT nextval('SEQ_SEC_ROLE_ACTION_GRANT'), g.ROLE_ID, a.ACTION_REG_PK, 'SYSTEM', CURRENT_TIMESTAMP
FROM SEC_ROLE_SCREEN_GRANT g
JOIN SEC_ACTION_REG a ON a.SCREEN_ID = g.SCREEN_ID
ORDER BY g.ROLE_SCREEN_GRANT_PK, a.ACTION_REG_PK;

-- ------------------------------------------------------------
-- 6. SEC_USER — the one bootstrap account, holding SYS_ADMIN. Without it a fresh database has no
--    identity that can create the first user (user creation needs an authenticated SYS_ADMIN).
--    It ships WITHOUT a usable password (erp-core step 04): STATUS_CODE 'PENDING' (login requires
--    'ACTIVE') and a PASSWORD_HASH placeholder that is not a BCrypt hash, so no password matches.
--    On application start, com.erp.sec.security.BootstrapAdminPasswordRunner sets the password
--    from erp.core.security.bootstrap-admin-password and activates the account — once, and only
--    while the row still carries this exact placeholder (UserDomain.BOOTSTRAP_PASSWORD_PLACEHOLDER).
--    The well-known admin/admin hash of the old chain is gone.
-- ------------------------------------------------------------
INSERT INTO SEC_USER (USER_PK, USERNAME, EMAIL, PASSWORD_HASH, FULL_NAME_AR, FULL_NAME_EN,
                      STATUS_CODE, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_USER'), 'admin', 'admin@erp.local', 'BOOTSTRAP-PASSWORD-NOT-SET',
        'مدير النظام', 'System Administrator', 'PENDING', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

INSERT INTO SEC_USER_ROLE (USER_ROLE_PK, USER_ID, ROLE_ID, ASSIGNED_BY, ASSIGNED_AT)
VALUES (nextval('SEQ_SEC_USER_ROLE'),
        (SELECT USER_PK FROM SEC_USER WHERE USERNAME = 'admin'),
        (SELECT ROLE_PK FROM SEC_ROLE WHERE CODE = 'SYS_ADMIN'),
        'SYSTEM', CURRENT_TIMESTAMP);
