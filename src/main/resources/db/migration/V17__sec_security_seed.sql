-- V17 — Security (SEC) security-registry seed
-- Source: governance/modules/SEC/packages/backend-execution/SEC-BE/SEC-BE.md (9-screen permission
--   matrix + "Seed data"), srs-sec.md §7.1 Access summary, V16__sec_schema.sql (the DDL).
-- Purpose: SEC-BE turns authentication/authorization ON. Against the V16 schema, which V16 leaves
--   completely empty, that would lock every caller out: no module/screen/action registry rows, no
--   role, no grants and no user, so nobody could authenticate and nothing could be authorized.
--   This seed is SEC registering itself into itself — the one module for which no external caller
--   is needed (SEC-BE.md "Seed data").
-- V10 seeds an admin into SEC_USER_ACCOUNT, a table V14 dropped, so it is dead for this schema.
--   It is never edited (Flyway checksums applied migrations) — this file is the forward fix.
-- Style matches V3/V11/V13: plain INSERTs (not idempotent — these run once on a fresh schema),
--   surrogate PKs from the V16 SEQ_SEC_* sequences, every FK resolved by natural key, never a
--   hardcoded id. Booleans are native (V16 uses BOOLEAN, unlike V2's numeric flags).

-- 1. SEC_MODULE_REG — module SEC (the Tier-1 grantable unit).
INSERT INTO SEC_MODULE_REG (MODULE_REG_PK, CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_MODULE_REG'), 'SEC', 'الأمان', 'Security', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- 2. SEC_SCREEN_REG — the 9 page codes of the SEC-BE.md matrix. SEC_LOGIN / SEC_SIGNUP /
--    SEC_PWD_RESET are the three public, pre-authentication screens: registered so the registry is
--    complete, but carrying no SEC_ACTION_REG row and no grant (matrix: "public", no permission).
INSERT INTO SEC_SCREEN_REG (SCREEN_REG_PK, PAGE_CODE, MODULE_ID, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_SCREEN_REG'), 'SEC_LOGIN',
        (SELECT MODULE_REG_PK FROM SEC_MODULE_REG WHERE CODE = 'SEC'),
        'تسجيل الدخول', 'Login', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_SCREEN_REG (SCREEN_REG_PK, PAGE_CODE, MODULE_ID, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_SCREEN_REG'), 'SEC_SIGNUP',
        (SELECT MODULE_REG_PK FROM SEC_MODULE_REG WHERE CODE = 'SEC'),
        'إنشاء حساب', 'Sign-up', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_SCREEN_REG (SCREEN_REG_PK, PAGE_CODE, MODULE_ID, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_SCREEN_REG'), 'SEC_PWD_RESET',
        (SELECT MODULE_REG_PK FROM SEC_MODULE_REG WHERE CODE = 'SEC'),
        'إعادة تعيين كلمة المرور', 'Forgot/reset password', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_SCREEN_REG (SCREEN_REG_PK, PAGE_CODE, MODULE_ID, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_SCREEN_REG'), 'SEC_USERS',
        (SELECT MODULE_REG_PK FROM SEC_MODULE_REG WHERE CODE = 'SEC'),
        'المستخدمون', 'Users', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_SCREEN_REG (SCREEN_REG_PK, PAGE_CODE, MODULE_ID, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_SCREEN_REG'), 'SEC_ROLES',
        (SELECT MODULE_REG_PK FROM SEC_MODULE_REG WHERE CODE = 'SEC'),
        'الأدوار والصلاحيات', 'Roles & permissions', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_SCREEN_REG (SCREEN_REG_PK, PAGE_CODE, MODULE_ID, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_SCREEN_REG'), 'SEC_MODULE_REGISTRY',
        (SELECT MODULE_REG_PK FROM SEC_MODULE_REG WHERE CODE = 'SEC'),
        'سجل الوحدات والشاشات والإجراءات', 'Module/screen/action registry', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_SCREEN_REG (SCREEN_REG_PK, PAGE_CODE, MODULE_ID, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_SCREEN_REG'), 'SEC_DASHBOARD',
        (SELECT MODULE_REG_PK FROM SEC_MODULE_REG WHERE CODE = 'SEC'),
        'لوحة تحكم المشرف', 'Admin dashboard', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_SCREEN_REG (SCREEN_REG_PK, PAGE_CODE, MODULE_ID, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_SCREEN_REG'), 'SEC_AUDIT_LOG',
        (SELECT MODULE_REG_PK FROM SEC_MODULE_REG WHERE CODE = 'SEC'),
        'سجل التدقيق', 'Audit log', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_SCREEN_REG (SCREEN_REG_PK, PAGE_CODE, MODULE_ID, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_SCREEN_REG'), 'SEC_SESSIONS',
        (SELECT MODULE_REG_PK FROM SEC_MODULE_REG WHERE CODE = 'SEC'),
        'الجلسات النشطة', 'Active sessions', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- 3. SEC_ACTION_REG — exactly the cells the SEC-BE.md matrix names a permission code for.
--    PERMISSION_CODE = PERM_<PAGE_CODE>_<ACTION_CODE>, the same derivation RegistryService
--    implements (PERMISSION_CODE_FORMAT, DBF-SEC-050). Twelve of the thirteen equal a constant in
--    com.erp.sec.permission.PermissionConstants; PERM_SEC_ROLES_DELETE has no constant because no
--    v1 endpoint gates on it (matrix: "reserved").
--    Not seeded, because the matrix names no permission code for them: SEC_USERS DELETE ("—"),
--    SEC_MODULE_REGISTRY CREATE ("via the registering module's own call" — API-SEC-018/019/020 are
--    gated on PERM_SEC_MODULE_REGISTRY_UPDATE, not on a CREATE code), SEC_DASHBOARD /
--    SEC_AUDIT_LOG CREATE/UPDATE/DELETE, SEC_SESSIONS CREATE/UPDATE.

-- SEC_USERS — VIEW (API-SEC-005), CREATE (API-SEC-006), UPDATE (API-SEC-007..011)
INSERT INTO SEC_ACTION_REG (ACTION_REG_PK, PERMISSION_CODE, SCREEN_ID, ACTION_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_ACTION_REG'), 'PERM_SEC_USERS_VIEW',
        (SELECT SCREEN_REG_PK FROM SEC_SCREEN_REG WHERE PAGE_CODE = 'SEC_USERS'), 'VIEW',
        'المستخدمون - عرض', 'Users - VIEW', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_ACTION_REG (ACTION_REG_PK, PERMISSION_CODE, SCREEN_ID, ACTION_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_ACTION_REG'), 'PERM_SEC_USERS_CREATE',
        (SELECT SCREEN_REG_PK FROM SEC_SCREEN_REG WHERE PAGE_CODE = 'SEC_USERS'), 'CREATE',
        'المستخدمون - إنشاء', 'Users - CREATE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_ACTION_REG (ACTION_REG_PK, PERMISSION_CODE, SCREEN_ID, ACTION_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_ACTION_REG'), 'PERM_SEC_USERS_UPDATE',
        (SELECT SCREEN_REG_PK FROM SEC_SCREEN_REG WHERE PAGE_CODE = 'SEC_USERS'), 'UPDATE',
        'المستخدمون - تعديل', 'Users - UPDATE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- SEC_ROLES — VIEW (API-SEC-012), CREATE (API-SEC-013), UPDATE (API-SEC-014..017),
--   DELETE (reserved: no delete-role endpoint in v1, deactivate only — which is UPDATE)
INSERT INTO SEC_ACTION_REG (ACTION_REG_PK, PERMISSION_CODE, SCREEN_ID, ACTION_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_ACTION_REG'), 'PERM_SEC_ROLES_VIEW',
        (SELECT SCREEN_REG_PK FROM SEC_SCREEN_REG WHERE PAGE_CODE = 'SEC_ROLES'), 'VIEW',
        'الأدوار والصلاحيات - عرض', 'Roles & permissions - VIEW', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_ACTION_REG (ACTION_REG_PK, PERMISSION_CODE, SCREEN_ID, ACTION_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_ACTION_REG'), 'PERM_SEC_ROLES_CREATE',
        (SELECT SCREEN_REG_PK FROM SEC_SCREEN_REG WHERE PAGE_CODE = 'SEC_ROLES'), 'CREATE',
        'الأدوار والصلاحيات - إنشاء', 'Roles & permissions - CREATE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_ACTION_REG (ACTION_REG_PK, PERMISSION_CODE, SCREEN_ID, ACTION_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_ACTION_REG'), 'PERM_SEC_ROLES_UPDATE',
        (SELECT SCREEN_REG_PK FROM SEC_SCREEN_REG WHERE PAGE_CODE = 'SEC_ROLES'), 'UPDATE',
        'الأدوار والصلاحيات - تعديل', 'Roles & permissions - UPDATE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_ACTION_REG (ACTION_REG_PK, PERMISSION_CODE, SCREEN_ID, ACTION_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_ACTION_REG'), 'PERM_SEC_ROLES_DELETE',
        (SELECT SCREEN_REG_PK FROM SEC_SCREEN_REG WHERE PAGE_CODE = 'SEC_ROLES'), 'DELETE',
        'الأدوار والصلاحيات - حذف', 'Roles & permissions - DELETE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- SEC_MODULE_REGISTRY — VIEW (API-SEC-021); UPDATE is the matrix's second "reserved" cell (row
--   deactivation has no v1 endpoint) but is NOT dormant: RegistryService gates API-SEC-018/019/020
--   on PERM_SEC_MODULE_REGISTRY_UPDATE, so without this row those three endpoints are unreachable.
INSERT INTO SEC_ACTION_REG (ACTION_REG_PK, PERMISSION_CODE, SCREEN_ID, ACTION_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_ACTION_REG'), 'PERM_SEC_MODULE_REGISTRY_VIEW',
        (SELECT SCREEN_REG_PK FROM SEC_SCREEN_REG WHERE PAGE_CODE = 'SEC_MODULE_REGISTRY'), 'VIEW',
        'سجل الوحدات والشاشات والإجراءات - عرض', 'Module/screen/action registry - VIEW', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_ACTION_REG (ACTION_REG_PK, PERMISSION_CODE, SCREEN_ID, ACTION_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_ACTION_REG'), 'PERM_SEC_MODULE_REGISTRY_UPDATE',
        (SELECT SCREEN_REG_PK FROM SEC_SCREEN_REG WHERE PAGE_CODE = 'SEC_MODULE_REGISTRY'), 'UPDATE',
        'سجل الوحدات والشاشات والإجراءات - تعديل', 'Module/screen/action registry - UPDATE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- SEC_DASHBOARD — VIEW only (API-SEC-022); per-widget source VIEWs apply on top of it.
INSERT INTO SEC_ACTION_REG (ACTION_REG_PK, PERMISSION_CODE, SCREEN_ID, ACTION_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_ACTION_REG'), 'PERM_SEC_DASHBOARD_VIEW',
        (SELECT SCREEN_REG_PK FROM SEC_SCREEN_REG WHERE PAGE_CODE = 'SEC_DASHBOARD'), 'VIEW',
        'لوحة تحكم المشرف - عرض', 'Admin dashboard - VIEW', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- SEC_AUDIT_LOG — VIEW only (API-SEC-023 search and API-SEC-024 export share it).
INSERT INTO SEC_ACTION_REG (ACTION_REG_PK, PERMISSION_CODE, SCREEN_ID, ACTION_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_ACTION_REG'), 'PERM_SEC_AUDIT_LOG_VIEW',
        (SELECT SCREEN_REG_PK FROM SEC_SCREEN_REG WHERE PAGE_CODE = 'SEC_AUDIT_LOG'), 'VIEW',
        'سجل التدقيق - عرض', 'Audit log - VIEW', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- SEC_SESSIONS — VIEW (API-SEC-025), DELETE (API-SEC-026 force-terminate).
INSERT INTO SEC_ACTION_REG (ACTION_REG_PK, PERMISSION_CODE, SCREEN_ID, ACTION_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_ACTION_REG'), 'PERM_SEC_SESSIONS_VIEW',
        (SELECT SCREEN_REG_PK FROM SEC_SCREEN_REG WHERE PAGE_CODE = 'SEC_SESSIONS'), 'VIEW',
        'الجلسات النشطة - عرض', 'Active sessions - VIEW', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);
INSERT INTO SEC_ACTION_REG (ACTION_REG_PK, PERMISSION_CODE, SCREEN_ID, ACTION_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_ACTION_REG'), 'PERM_SEC_SESSIONS_DELETE',
        (SELECT SCREEN_REG_PK FROM SEC_SCREEN_REG WHERE PAGE_CODE = 'SEC_SESSIONS'), 'DELETE',
        'الجلسات النشطة - إنهاء', 'Active sessions - DELETE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- 4. SEC_ROLE — SYS_ADMIN, the bootstrap role holding the whole SEC surface.
INSERT INTO SEC_ROLE (ROLE_PK, CODE, NAME_AR, NAME_EN, DESCRIPTION_AR, DESCRIPTION_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_ROLE'), 'SYS_ADMIN', 'مدير النظام', 'System Administrator',
        'دور التهيئة الأولية — يملك كامل صلاحيات وحدة الأمان', 'Bootstrap role — holds the full Security module surface',
        TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- 5. SEC_ROLE_MODULE_GRANT — Tier-1. Must precede the screen grants (RULE-SEC-001).
INSERT INTO SEC_ROLE_MODULE_GRANT (ROLE_MODULE_GRANT_PK, ROLE_ID, MODULE_ID, GRANTED_BY, GRANTED_AT)
VALUES (nextval('SEQ_SEC_ROLE_MODULE_GRANT'),
        (SELECT ROLE_PK FROM SEC_ROLE WHERE CODE = 'SYS_ADMIN'),
        (SELECT MODULE_REG_PK FROM SEC_MODULE_REG WHERE CODE = 'SEC'),
        'SYSTEM', CURRENT_TIMESTAMP);

-- 6. SEC_ROLE_SCREEN_GRANT — Tier-2, the six secured screens only. The three public screens carry
--    no permission at all, so granting them would assert a right that does not exist.
INSERT INTO SEC_ROLE_SCREEN_GRANT (ROLE_SCREEN_GRANT_PK, ROLE_ID, SCREEN_ID, GRANTED_BY, GRANTED_AT)
SELECT nextval('SEQ_SEC_ROLE_SCREEN_GRANT'),
       (SELECT ROLE_PK FROM SEC_ROLE WHERE CODE = 'SYS_ADMIN'),
       s.SCREEN_REG_PK, 'SYSTEM', CURRENT_TIMESTAMP
FROM SEC_SCREEN_REG s
WHERE s.PAGE_CODE IN ('SEC_USERS', 'SEC_ROLES', 'SEC_MODULE_REGISTRY',
                      'SEC_DASHBOARD', 'SEC_AUDIT_LOG', 'SEC_SESSIONS');

-- 7. SEC_ROLE_ACTION_GRANT — Tier-3, every seeded action. RULE-SEC-002 holds (each action's screen
--    is granted above) and RULE-SEC-007 holds (every screen carrying a non-VIEW action also
--    carries its VIEW).
INSERT INTO SEC_ROLE_ACTION_GRANT (ROLE_ACTION_GRANT_PK, ROLE_ID, ACTION_ID, GRANTED_BY, GRANTED_AT)
SELECT nextval('SEQ_SEC_ROLE_ACTION_GRANT'),
       (SELECT ROLE_PK FROM SEC_ROLE WHERE CODE = 'SYS_ADMIN'),
       a.ACTION_REG_PK, 'SYSTEM', CURRENT_TIMESTAMP
FROM SEC_ACTION_REG a;

-- 8. SEC_USER — the one bootstrap account. Without it a fresh database has no identity that can
--    log in to create the first user (user creation itself needs an authenticated SYS_ADMIN).
--    PASSWORD_HASH is copied verbatim from V10 — a BCrypt hash of "admin".
--    DEV/TEST BOOTSTRAP CREDENTIAL ONLY (admin/admin). Change or disable it before any real
--    environment; it is a published, well-known secret.
INSERT INTO SEC_USER (USER_PK, USERNAME, EMAIL, PASSWORD_HASH, FULL_NAME_AR, FULL_NAME_EN,
                      STATUS_CODE, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_USER'), 'admin', 'admin@erp.local',
        '$2y$10$.G36apS4.ChTaMI.YU3bxO1nPj9IREwDvDa1MEHGuB5dlRSU7ikLe',
        'مدير النظام', 'System Administrator', 'ACTIVE', TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- 9. SEC_USER_ROLE — admin holds SYS_ADMIN.
INSERT INTO SEC_USER_ROLE (USER_ROLE_PK, USER_ID, ROLE_ID, ASSIGNED_BY, ASSIGNED_AT)
VALUES (nextval('SEQ_SEC_USER_ROLE'),
        (SELECT USER_PK FROM SEC_USER WHERE USERNAME = 'admin'),
        (SELECT ROLE_PK FROM SEC_ROLE WHERE CODE = 'SYS_ADMIN'),
        'SYSTEM', CURRENT_TIMESTAMP);
