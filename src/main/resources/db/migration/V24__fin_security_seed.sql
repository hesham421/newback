-- ============================================================
-- V24 — Finance / General Ledger (FIN) security-registry seed
-- ============================================================
-- Source: governance/modules/FIN/packages/backend-execution/SEC-BE/SEC-BE.md (the 12-screen
--   permission matrix + its "Seed data" note, REQ-FIN-044), V16__sec_schema.sql (the
--   SEC_MODULE_REG / SEC_SCREEN_REG / SEC_ACTION_REG DDL — every column and constraint below is
--   verbatim from it), V17__sec_security_seed.sql and V19__mdl_security_seed.sql (the two
--   existing precedents for registering a module + its screens + its actions).
--
-- Purpose: register FIN into SEC_MODULE_REG (SEC_SCREEN_REG.MODULE_ID is a real FK to it —
--   FK_SCREEN_REG_MODULE, RULE-SEC-004), then its 12 screens and their 26 action rows, so the
--   PERM_FIN_* codes that com.erp.sec.permission.PermissionConstants declares — and that FIN's
--   @PreAuthorize gates reference through a T(...) SpEL reference — resolve to real, grantable
--   SEC_ACTION_REG rows. FIN is not registered anywhere else in this schema: V20 registered only
--   NOTIF and FILE, and V22/V23 (FIN's own schema) touch no SEC table.
--
-- Registration mechanism — the same resolved reading V19 records: SEC-BE.md's "registered via
--   SEC's screen-registration endpoint" wording describes the DATA and the surface SEC *exposes*
--   for a live runtime registration by some future admin flow. It does not describe how this
--   codebase seeds a module's own screens at deploy time. V17 seeds SEC's own 9 screens with
--   plain INSERTs in a Flyway migration; V19 did the same for MDL. No HTTP server exists at
--   migration time to call anyway. No Java source is touched — V17 and V19 needed none either.
--
-- Style matches V17/V19/V21: plain INSERTs (run once on a fresh schema, not idempotent),
--   surrogate PKs from the V16 SEQ_SEC_* sequences, every FK resolved by natural key
--   (CODE / PAGE_CODE), never a hardcoded id. Booleans are native (V16 uses BOOLEAN).
--
-- ------------------------------------------------------------
-- The 26 action rows vs. the 25 PERM_FIN_* constants — one deliberate, reported difference
-- ------------------------------------------------------------
-- 25 of the 26 rows below carry a PERMISSION_CODE that exists verbatim as a constant in
-- PermissionConstants and is referenced by at least one FIN @PreAuthorize. The 26th is
-- PERM_FIN_PERIODS_VIEW:
--   * SEC-BE.md's matrix declares FIN_PERIODS / VIEW as ✓ (the only ✓ cell in the matrix with no
--     API-FIN-xxx beside it), and srs-fin.md §"Access summary" repeats it ("FIN_PERIODS |
--     Fiscal periods & years | role-granted | ...").
--   * FIN exposes no read endpoint on FIN_PERIODS, so no constant was ever declared for it and
--     nothing in src/main/java references the code today.
--   * It is registered anyway because the platform gateway convention SEC-BE.md states —
--     "every non-VIEW permission requires VIEW on the same screen first" — makes the screen's
--     VIEW row the prerequisite of PERM_FIN_PERIODS_CREATE / _UPDATE / _CLOSE_APPROVE. Omitting
--     it would leave FIN_PERIODS the only screen in this seed whose write permissions have no
--     gateway to sit behind, and would contradict a ✓ the matrix states outright.
-- This is recorded as a gap in governance/modules/FIN/execution-state.json rather than silently
-- reconciled: either the matrix's FIN_PERIODS/VIEW ✓ is spurious, or a FIN_PERIODS read endpoint
-- (and its constant) is missing from SVC-API. A human decision is needed; until then the row is
-- harmless registry data and a correct gateway.
--
-- Conversely, SEC-BE.md's FIN_ACCOUNTS / DELETE cell ("✓ deactivate (API-FIN-004)") gets NO
-- DELETE row: API-FIN-004's own Security line and AccountService.deactivate() both gate on
-- PERM_FIN_ACCOUNTS_UPDATE — deactivate is modelled as UPDATE, exactly as MDL_LOOKUPS does. Same
-- for FIN_DIMENSIONS, FIN_RULES, FIN_RECURRING_TEMPLATES and FIN_ALLOCATION_RULES, whose
-- srs-fin.md B4 "Actions:" lines say "DELETE (deactivate ..., modeled as UPDATE)". Inventing
-- DELETE rows there would create permanently-unreferenced registry data.
--
-- GRANTS ARE NOT IN THIS FILE — see V25. Registration is not a grant: V19 registered MDL and
-- granted it to nobody, and the observable result on a fresh database was every MDL endpoint
-- answering 403 to every caller including SYS_ADMIN, which V21 had to repair. V25 lands with
-- this migration so FIN never reaches that state.
-- ============================================================

-- ------------------------------------------------------------
-- 1. SEC_MODULE_REG — module FIN (Tier-1 grantable unit; RULE-SEC-004's FK target)
-- ------------------------------------------------------------
INSERT INTO SEC_MODULE_REG (MODULE_REG_PK, CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
VALUES (nextval('SEQ_SEC_MODULE_REG'), 'FIN', 'المالية والأستاذ العام', 'Finance / General Ledger',
        TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

-- ------------------------------------------------------------
-- 2. SEC_SCREEN_REG — the 12 page codes of SEC-BE.md's matrix. Bilingual names are taken from
--    srs-fin.md's SCR-REQ-FIN-001..012 headings, which are already Arabic / English pairs.
-- ------------------------------------------------------------
INSERT INTO SEC_SCREEN_REG (SCREEN_REG_PK, PAGE_CODE, MODULE_ID, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
SELECT nextval('SEQ_SEC_SCREEN_REG'), v.page_code,
       (SELECT MODULE_REG_PK FROM SEC_MODULE_REG WHERE CODE = 'FIN'),
       v.name_ar, v.name_en, TRUE, 'SYSTEM', CURRENT_TIMESTAMP
FROM (VALUES
    ('FIN_ACCOUNTS',             'شجرة الحسابات',                'Chart of accounts'),
    ('FIN_DIMENSIONS',           'تعريف الأبعاد وقيمها',          'Dimension definition & values'),
    ('FIN_RULES',                'قواعد المحرك',                  'Engine rules'),
    ('FIN_RECURRING_TEMPLATES',  'قوالب متكررة/عكسية',            'Recurring / reversing templates'),
    ('FIN_ALLOCATION_RULES',     'قواعد التوزيع',                 'Allocation rules'),
    ('FIN_JOURNAL_ENTRIES',      'قيود اليومية',                  'Journal entries'),
    ('FIN_PERIODS',              'الفترات والسنوات المالية',       'Fiscal periods & years'),
    ('FIN_ACCOUNT_LEDGER',       'دفتر الحساب',                   'Account ledger'),
    ('FIN_TRIAL_BALANCE',        'ميزان المراجعة',                'Trial balance'),
    ('FIN_BALANCE_SHEET',        'الميزانية العمومية',            'Balance sheet'),
    ('FIN_INCOME_STATEMENT',     'قائمة الدخل',                   'Income statement'),
    ('FIN_DIMENSION_REPORTS',    'تقارير الأبعاد',                'Dimension reports')
) AS v(page_code, name_ar, name_en);

-- ------------------------------------------------------------
-- 3. SEC_ACTION_REG — exactly the cells SEC-BE.md's matrix names, one row per action.
--    PERMISSION_CODE follows PERM_<PAGE_CODE>_<ACTION_CODE> and is copied verbatim from
--    PermissionConstants (except PERM_FIN_PERIODS_VIEW — see the header note). SCREEN_ID is
--    resolved from PAGE_CODE, never hardcoded. NAME_AR/NAME_EN mirror V19's
--    "<screen name> - <action>" shape.
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
    -- FIN_ACCOUNTS — VIEW (API-FIN-001), CREATE (API-FIN-002), UPDATE (API-FIN-003, 004 deactivate)
    ('FIN_ACCOUNTS',            'VIEW',          'عرض'),
    ('FIN_ACCOUNTS',            'CREATE',        'إنشاء'),
    ('FIN_ACCOUNTS',            'UPDATE',        'تعديل'),
    -- FIN_DIMENSIONS — VIEW (API-FIN-005, 008), CREATE (API-FIN-006, 007). No UPDATE cell.
    ('FIN_DIMENSIONS',          'VIEW',          'عرض'),
    ('FIN_DIMENSIONS',          'CREATE',        'إنشاء'),
    -- FIN_RULES — VIEW (API-FIN-009), CREATE (API-FIN-010), UPDATE (API-FIN-011 add line)
    ('FIN_RULES',               'VIEW',          'عرض'),
    ('FIN_RULES',               'CREATE',        'إنشاء'),
    ('FIN_RULES',               'UPDATE',        'تعديل'),
    -- FIN_RECURRING_TEMPLATES — VIEW (012), CREATE (013), UPDATE (014 run)
    ('FIN_RECURRING_TEMPLATES', 'VIEW',          'عرض'),
    ('FIN_RECURRING_TEMPLATES', 'CREATE',        'إنشاء'),
    ('FIN_RECURRING_TEMPLATES', 'UPDATE',        'تعديل'),
    -- FIN_ALLOCATION_RULES — VIEW (015), CREATE (016), UPDATE (017 run)
    ('FIN_ALLOCATION_RULES',    'VIEW',          'عرض'),
    ('FIN_ALLOCATION_RULES',    'CREATE',        'إنشاء'),
    ('FIN_ALLOCATION_RULES',    'UPDATE',        'تعديل'),
    -- FIN_JOURNAL_ENTRIES — VIEW (018, 022), CREATE (019, 020 incl. post),
    --   custom REVERSE (021). RULE-FIN-015 separates CREATE from FIN_PERIODS/CLOSE_APPROVE.
    ('FIN_JOURNAL_ENTRIES',     'VIEW',          'عرض'),
    ('FIN_JOURNAL_ENTRIES',     'CREATE',        'إنشاء'),
    ('FIN_JOURNAL_ENTRIES',     'REVERSE',       'عكس'),
    -- FIN_PERIODS — VIEW (matrix ✓, no API; gateway row, see header), CREATE (023 fiscal year),
    --   UPDATE (024 open / 025 soft-close), custom CLOSE_APPROVE (026 hard-close / 027 year-end).
    ('FIN_PERIODS',             'VIEW',          'عرض'),
    ('FIN_PERIODS',             'CREATE',        'إنشاء'),
    ('FIN_PERIODS',             'UPDATE',        'تعديل'),
    ('FIN_PERIODS',             'CLOSE_APPROVE', 'اعتماد الإغلاق'),
    -- The five read-only report screens — VIEW only (API-FIN-028..032)
    ('FIN_ACCOUNT_LEDGER',      'VIEW',          'عرض'),
    ('FIN_TRIAL_BALANCE',       'VIEW',          'عرض'),
    ('FIN_BALANCE_SHEET',       'VIEW',          'عرض'),
    ('FIN_INCOME_STATEMENT',    'VIEW',          'عرض'),
    ('FIN_DIMENSION_REPORTS',   'VIEW',          'عرض')
) AS v(page_code, action_code, action_ar)
JOIN SEC_SCREEN_REG s ON s.PAGE_CODE = v.page_code;
