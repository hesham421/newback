-- ============================================================
-- V37 — Finance / General Ledger (FIN) v2 — FIN_ACCOUNT_MAPPINGS security seed
-- ============================================================
-- Source: governance/shared/backend/modules/FIN/packages/v2/backend-execution/SEC-BE/SEC-BE.md
--   ("v2 — FIN_ACCOUNT_MAPPINGS": one SEC_PAGES row, three actions VIEW/CREATE/UPDATE, granted to
--   SYS_ADMIN explicitly, following V28) and srs-fin.md v2 SCR-REQ-FIN-013 §B4.
-- Shapes: SEC_SCREEN_REG / SEC_ACTION_REG INSERTs copied from V24__fin_security_seed.sql (§2, §3)
--   and V28__fin_dimensions_update_action.sql; the single-screen Tier-2 grant from V27 §3; the
--   Tier-3 grant from V28 §2. Column, constraint and sequence names verbatim from V16.
-- Why the grants are here: V25's Tier-2/Tier-3 SELECTs over the FIN registry have already run
--   everywhere and Flyway never re-evaluates them (V28's reasoning). Tier-1 (module FIN) exists
--   from V25 and UQ_SEC_ROLE_MODULE_GRANT_ROLE_MODULE forbids repeating it. No DELETE row (deactivate
--   is UPDATE, as FIN_ACCOUNTS); FIN_CLOSE_APPROVER receives nothing. Plain INSERTs, as V24/V28.
-- ============================================================

-- ------------------------------------------------------------
-- 1. SEC_SCREEN_REG — the FIN_ACCOUNT_MAPPINGS page under module FIN (MODULE_ID resolved by CODE).
-- ------------------------------------------------------------
INSERT INTO SEC_SCREEN_REG (SCREEN_REG_PK, PAGE_CODE, MODULE_ID, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
SELECT nextval('SEQ_SEC_SCREEN_REG'), v.page_code,
       (SELECT MODULE_REG_PK FROM SEC_MODULE_REG WHERE CODE = 'FIN'),
       v.name_ar, v.name_en, TRUE, 'SYSTEM', CURRENT_TIMESTAMP
FROM (VALUES
    ('FIN_ACCOUNT_MAPPINGS',     'روابط الحسابات',                'Account mappings')
) AS v(page_code, name_ar, name_en);

-- ------------------------------------------------------------
-- 2. SEC_ACTION_REG — VIEW (the gateway, API-FIN-038), CREATE (API-FIN-039), UPDATE (API-FIN-040
--    change the account; API-FIN-041 deactivate). PERMISSION_CODE is synthesised as V24/V28 do it,
--    'PERM_' || page_code || '_' || action_code, yielding the three PermissionConstants verbatim.
-- ------------------------------------------------------------
INSERT INTO SEC_ACTION_REG (ACTION_REG_PK, PERMISSION_CODE, SCREEN_ID, ACTION_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
SELECT nextval('SEQ_SEC_ACTION_REG'),
       'PERM_' || v.page_code || '_' || v.action_code,
       s.SCREEN_REG_PK,
       v.action_code,
       s.NAME_AR || ' - ' || v.action_ar,
       s.NAME_EN || ' - ' || v.action_code,
       TRUE, 'SYSTEM', CURRENT_TIMESTAMP
FROM (VALUES
    ('FIN_ACCOUNT_MAPPINGS',    'VIEW',          'عرض'),
    ('FIN_ACCOUNT_MAPPINGS',    'CREATE',        'إنشاء'),
    ('FIN_ACCOUNT_MAPPINGS',    'UPDATE',        'تعديل')
) AS v(page_code, action_code, action_ar)
JOIN SEC_SCREEN_REG s ON s.PAGE_CODE = v.page_code;

-- ------------------------------------------------------------
-- 3. Tier-2 — SEC_ROLE_SCREEN_GRANT: the new screen to SYS_ADMIN (RULE-SEC-002).
-- ------------------------------------------------------------
INSERT INTO SEC_ROLE_SCREEN_GRANT (ROLE_SCREEN_GRANT_PK, ROLE_ID, SCREEN_ID, GRANTED_BY, GRANTED_AT)
VALUES (nextval('SEQ_SEC_ROLE_SCREEN_GRANT'),
        (SELECT ROLE_PK FROM SEC_ROLE WHERE CODE = 'SYS_ADMIN'),
        (SELECT SCREEN_REG_PK FROM SEC_SCREEN_REG WHERE PAGE_CODE = 'FIN_ACCOUNT_MAPPINGS'),
        'SYSTEM', CURRENT_TIMESTAMP);

-- ------------------------------------------------------------
-- 4. Tier-3 — SEC_ROLE_ACTION_GRANT: the three new rows to SYS_ADMIN. Scoped by PERMISSION_CODE so
--    nothing V25/V27/V28/V30/V33 granted is touched; VIEW is granted in the same statement, so
--    RULE-SEC-007 (the gateway) holds for CREATE and UPDATE.
-- ------------------------------------------------------------
INSERT INTO SEC_ROLE_ACTION_GRANT (ROLE_ACTION_GRANT_PK, ROLE_ID, ACTION_ID, GRANTED_BY, GRANTED_AT)
SELECT nextval('SEQ_SEC_ROLE_ACTION_GRANT'),
       (SELECT ROLE_PK FROM SEC_ROLE WHERE CODE = 'SYS_ADMIN'),
       a.ACTION_REG_PK, 'SYSTEM', CURRENT_TIMESTAMP
FROM SEC_ACTION_REG a
WHERE a.PERMISSION_CODE IN ('PERM_FIN_ACCOUNT_MAPPINGS_VIEW',
                            'PERM_FIN_ACCOUNT_MAPPINGS_CREATE',
                            'PERM_FIN_ACCOUNT_MAPPINGS_UPDATE');
