-- ============================================================
-- V26 — Finance / General Ledger (FIN) — MDL lookup-registry seed
-- ============================================================
-- Source (every key and every value below is read from an artifact, none invented):
--   * governance/modules/FIN/P1/srs-fin.md §A6 "Lookups" — the authoritative 13-key table with
--     its value lists, and §A7 "Status lifecycle" — the JOURNAL_STATUS (3), PERIOD_STATE (4) and
--     FISCAL_YEAR_STATUS (2) state machines.
--   * governance/modules/FIN/P0/module-registry-fin.md "LOOKUPS OWNED" — the same 13 keys with
--     their bilingual descriptions (used verbatim as the MDL_LOOKUP_TYPE names below) and the
--     same value lists with a per-key source citation.
--   * governance/modules/FIN/P1/registry-srs-fin.md — the per-key value COUNTS
--     (5,2,4,2,5,3,0,0,3,3,3,2,4 = 36), which match A6 and P0 exactly.
--   * governance/modules/FIN/packages/backend-execution/CORE/CORE.md and
--     governance/modules/FIN/P2/db-script-fin.md §COMMENTs — the 16 lookup-backed columns marked
--     XM-FIN-001 and the key each one reads.
--   * src/main/java/com/erp/fin/service/FinLookupValidationService.java — the 13 KEY_* constants,
--     copied verbatim here; and every FIN entity/domain/service status literal (grepped: 32
--     distinct literals, all present below as active values).
--   * src/main/resources/db/migration/V18__mdl_sequences.sql — the MDL_LOOKUP_TYPE /
--     MDL_LOOKUP_VALUE DDL. Every table, column and sequence name below is verbatim from it.
--   * V19__mdl_security_seed.sql, V20__notif_file_lookup_data_migration.sql and
--     V24__fin_security_seed.sql — the house style followed here.
--
-- Purpose: on a fresh database nothing seeds FIN's lookup types. MdlLookupApiImpl
--   .readActiveValuesByKey throws MDL-404-TYPE-KEY for an unknown key, which
--   FinLookupValidationService.assertValidCode relabels as FIN-400-INVALID-LOOKUP (HTTP 400), so
--   the very first POST /api/v1/fin/accounts fails on accountTypeCode and FIN is unusable out of
--   the box. This migration seeds the 13 types and their 36 values so every lookup-backed FIN
--   column has real reference data to validate against.
--
-- Mechanism — a migration, not a runtime REST call, by explicit human decision recorded in
--   governance/modules/FIN/execution-state.json (api_doc_gaps, phase/sub ALIGN-BE). It mirrors the
--   precedent this repo has already set twice: V20 seeds NOTIF's and FILE's own lookup types
--   straight into MDL_LOOKUP_TYPE / MDL_LOOKUP_VALUE, and V24 seeds FIN's own screens straight
--   into SEC's registry tables. REQ-FIN-045 ("when FIN is deployed, the system shall register its
--   13 lookup types as data into the Lookup module, naming FIN as owner") describes the DATA and
--   the owner, not an HTTP mechanism — and no HTTP server exists at migration time to call.
--
-- Style matches V19/V20/V24: plain INSERTs (run once on a fresh schema, not idempotent),
--   surrogate PKs from the V18 SEQ_MDL_* sequences, every FK resolved by natural key (KEY),
--   never a hardcoded id, native BOOLEAN, bilingual NAME_AR / NAME_EN on every row.
--
-- OWNER_MODULE_CODE = 'FIN': MDL's RULE-MDL-001 / XM-MDL-001 requires the owner module to exist
--   in SEC_MODULE_REG. V24 already inserted that row (CODE = 'FIN'); the guard below fails the
--   migration loudly rather than seeding orphaned types if it is ever missing. It is a SOFT-READ,
--   so there is deliberately no FK to resolve — the check is an assertion, not a join.
--
-- ------------------------------------------------------------
-- Two keys are seeded with ZERO values — deliberate, not an omission
-- ------------------------------------------------------------
--   ACCOUNTING_EVENT_TYPE — srs-fin.md §A6: "none seeded — host-specific, added as data
--     [§3, §6.4]"; module-registry-fin.md: "None named — host-specific, added as data per host".
--   PAYMENT_METHOD — §A6: "none seeded — host-specific"; registry-srs-fin.md:42 adds
--     "(registered, no dedicated column this v1)" — it has no lookup-backed column in
--     db-script-fin.md at all, only a KEY_ constant.
-- Both TYPES are created (REQ-FIN-045 registers 13, and a registered-but-empty type makes MDL
--   return an empty active-value list instead of MDL-404-TYPE-KEY, which is the honest answer),
--   but no VALUE rows are invented for them. Consequence to be aware of: FIN_EVENT_TYPE_RULE
--   .event_type_code is validated against ACCOUNTING_EVENT_TYPE, so creating an event-type rule
--   still requires the host to add its own event-type values through MDL's lookup administration
--   first. That is the documented host-specific step, not a defect of this seed.
--
-- Value NAME_AR / NAME_EN labels: the codes and their per-key membership are taken from the
--   artifacts above; the bilingual display labels for individual VALUES are not given by any
--   artifact (A6 lists codes only) and are written here as the standard Arabic/English accounting
--   terms for each code, exactly as V20 did for NOTIF/FILE. TYPE-level labels are verbatim from
--   module-registry-fin.md's LOOKUPS OWNED descriptions.
--
-- SORT_ORDER follows the order each value is listed in srs-fin.md §A6 (1-based), which for the
--   three status keys is also their §A7 lifecycle order.
-- ============================================================

-- ------------------------------------------------------------
-- 0. Guard — FIN must be registered in SEC_MODULE_REG (RULE-MDL-001, XM-MDL-001 SOFT-READ).
--    V24 inserts it; this aborts the migration if that ever stops being true.
-- ------------------------------------------------------------
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM SEC_MODULE_REG WHERE CODE = 'FIN') THEN
    RAISE EXCEPTION 'V26: module FIN is not registered in SEC_MODULE_REG; '
                    'MDL lookup types cannot name an unregistered owner module (RULE-MDL-001). '
                    'V24__fin_security_seed.sql must have been applied first.';
  END IF;
END $$;

-- ------------------------------------------------------------
-- 1. MDL_LOOKUP_TYPE — the 13 FIN-owned keys (srs-fin.md §A6, in its listed order).
--    KEY values are verbatim from FinLookupValidationService's KEY_* constants.
--    NAME_AR / NAME_EN are verbatim from module-registry-fin.md's LOOKUPS OWNED descriptions.
-- ------------------------------------------------------------
INSERT INTO MDL_LOOKUP_TYPE (LOOKUP_TYPE_PK, KEY, OWNER_MODULE_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
SELECT nextval('SEQ_MDL_LOOKUP_TYPE'), t.key, 'FIN', t.name_ar, t.name_en, TRUE, 'SYSTEM', CURRENT_TIMESTAMP
FROM (VALUES
    ('ACCOUNT_TYPE',            'نوع الحساب',             'Account type'),
    ('DEBIT_CREDIT',            'الطبيعة والاتجاه',        'Nature & posting direction'),
    ('PERIOD_STATE',            'حالة الفترة',            'Period state'),
    ('ACCOUNTING_EVENT_TYPE',   'نوع الحدث المحاسبي',      'Accounting event type'),
    ('PAYMENT_METHOD',          'طريقة الدفع',            'Payment method'),
    ('JOURNAL_TYPE',            'نوع اليومية',            'Journal type'),
    ('FISCAL_YEAR_STATUS',      'حالة السنة المالية',      'Fiscal year status'),
    ('JOURNAL_STATUS',          'حالة القيد',             'Journal entry status'),
    ('ACCOUNT_DERIVATION_TYPE', 'نوع اشتقاق الحساب',       'Account derivation type'),
    ('AMOUNT_SOURCE_TYPE',      'نوع مصدر المبلغ',         'Amount source type'),
    ('DISTRIBUTION_TYPE',       'نوع التوزيع',            'Distribution type'),
    ('RECURRING_SCHEDULE_TYPE', 'نوع الجدولة',            'Recurring schedule type'),
    ('RECURRING_FREQUENCY',     'تكرار الجدولة',          'Recurring frequency')
) AS t(key, name_ar, name_en);

-- ------------------------------------------------------------
-- 2. MDL_LOOKUP_VALUE — 36 values across 11 of the 13 types (the other two are host-specific;
--    see the header). LOOKUP_TYPE_ID is resolved from KEY, never hardcoded. The composite
--    UQ_MDL_LOOKUP_VALUE_TYPE_CODE (RULE-MDL-002) holds: no code repeats within a key.
-- ------------------------------------------------------------
INSERT INTO MDL_LOOKUP_VALUE (LOOKUP_VALUE_PK, LOOKUP_TYPE_ID, CODE, NAME_AR, NAME_EN, SORT_ORDER, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
SELECT nextval('SEQ_MDL_LOOKUP_VALUE'),
       (SELECT LOOKUP_TYPE_PK FROM MDL_LOOKUP_TYPE WHERE KEY = v.type_key AND OWNER_MODULE_CODE = 'FIN'),
       v.code, v.name_ar, v.name_en, v.sort_order, TRUE, 'SYSTEM', CURRENT_TIMESTAMP
FROM (VALUES
    -- ACCOUNT_TYPE (5) — A6; general-accounting-system-plan-en.md §4.2. FIN_ACCOUNT.account_type_code.
    ('ACCOUNT_TYPE',            'ASSET',           'أصول',              'Asset',            1),
    ('ACCOUNT_TYPE',            'LIABILITY',       'خصوم',              'Liability',        2),
    ('ACCOUNT_TYPE',            'EQUITY',          'حقوق الملكية',       'Equity',           3),
    ('ACCOUNT_TYPE',            'REVENUE',         'إيرادات',           'Revenue',          4),
    ('ACCOUNT_TYPE',            'EXPENSE',         'مصروفات',           'Expense',          5),

    -- DEBIT_CREDIT (2) — A6; §4.2, §6.2(c). FIN_ACCOUNT.nature_code, FIN_JOURNAL_LINE
    -- .direction_code, FIN_RULE_LINE.direction_code, FIN_RECURRING_TEMPLATE_LINE.direction_code.
    ('DEBIT_CREDIT',            'DEBIT',           'مدين',              'Debit',            1),
    ('DEBIT_CREDIT',            'CREDIT',          'دائن',              'Credit',           2),

    -- PERIOD_STATE (4) — A6 + A7 lifecycle order (OPEN -> SOFT_CLOSE -> HARD_CLOSE ->
    -- YEAR_END_CLOSE); §10.2. FIN_FISCAL_PERIOD.status_code.
    ('PERIOD_STATE',            'OPEN',            'مفتوحة',            'Open',             1),
    ('PERIOD_STATE',            'SOFT_CLOSE',      'إغلاق مبدئي',        'Soft close',       2),
    ('PERIOD_STATE',            'HARD_CLOSE',      'إغلاق نهائي',        'Hard close',       3),
    ('PERIOD_STATE',            'YEAR_END_CLOSE',  'إغلاق نهاية السنة',   'Year-end close',   4),

    -- JOURNAL_TYPE (5) — A6; §5.2, §7 (journal sources) + §9 (reversal).
    -- FIN_JOURNAL_ENTRY.journal_type_code.
    ('JOURNAL_TYPE',            'EVENT_GENERATED', 'مولّد من حدث',        'Event generated',  1),
    ('JOURNAL_TYPE',            'MANUAL',          'يدوي',              'Manual',           2),
    ('JOURNAL_TYPE',            'RECURRING',       'متكرر',             'Recurring',        3),
    ('JOURNAL_TYPE',            'ALLOCATION',      'توزيع',             'Allocation',       4),
    ('JOURNAL_TYPE',            'REVERSAL',        'عكسي',              'Reversal',         5),

    -- FISCAL_YEAR_STATUS (2) — A6 + A7 (binary, set by REQ-FIN-036). FIN_FISCAL_YEAR.status_code.
    ('FISCAL_YEAR_STATUS',      'OPEN',            'مفتوحة',            'Open',             1),
    ('FISCAL_YEAR_STATUS',      'CLOSED',          'مغلقة',             'Closed',           2),

    -- JOURNAL_STATUS (3) — A6 + A7 lifecycle (DRAFT -> POSTED -> VOID); §8.1.
    -- FIN_JOURNAL_ENTRY.status_code.
    ('JOURNAL_STATUS',          'DRAFT',           'مسودة',             'Draft',            1),
    ('JOURNAL_STATUS',          'POSTED',          'مُرحَّل',             'Posted',           2),
    ('JOURNAL_STATUS',          'VOID',            'ملغي',              'Void',             3),

    -- ACCOUNT_DERIVATION_TYPE (3) — A6; §6.2(a). FIN_RULE_LINE.account_derivation_type_code.
    ('ACCOUNT_DERIVATION_TYPE', 'CONSTANT',        'ثابت',              'Constant',         1),
    ('ACCOUNT_DERIVATION_TYPE', 'DIRECT',          'مباشر',             'Direct',           2),
    ('ACCOUNT_DERIVATION_TYPE', 'MAPPING',         'تخطيط',             'Mapping',          3),

    -- AMOUNT_SOURCE_TYPE (3) — A6; §6.2(b). FIN_RULE_LINE.amount_source_type_code.
    ('AMOUNT_SOURCE_TYPE',      'FIELD',           'حقل',               'Field',            1),
    ('AMOUNT_SOURCE_TYPE',      'PERCENTAGE',      'نسبة مئوية',         'Percentage',       2),
    ('AMOUNT_SOURCE_TYPE',      'REMAINDER',       'المتبقي',            'Remainder',        3),

    -- DISTRIBUTION_TYPE (3) — A6; §6.3, reused by ENT-FIN-014 (§7.4).
    -- FIN_RULE_LINE.distribution_type_code, FIN_ALLOCATION_TARGET.distribution_type_code.
    ('DISTRIBUTION_TYPE',       'FIXED',           'مبلغ ثابت',          'Fixed',            1),
    ('DISTRIBUTION_TYPE',       'PERCENTAGE',      'نسبة مئوية',         'Percentage',       2),
    ('DISTRIBUTION_TYPE',       'REMAINDER',       'المتبقي',            'Remainder',        3),

    -- RECURRING_SCHEDULE_TYPE (2) — A6; §7.3. FIN_RECURRING_TEMPLATE.schedule_type_code.
    ('RECURRING_SCHEDULE_TYPE', 'RECURRING',       'متكرر',             'Recurring',        1),
    ('RECURRING_SCHEDULE_TYPE', 'REVERSING',       'عكسي',              'Reversing',        2),

    -- RECURRING_FREQUENCY (4) — A6; §7.3. FIN_RECURRING_TEMPLATE.frequency_code (required only
    -- when schedule_type_code = RECURRING).
    ('RECURRING_FREQUENCY',     'MONTHLY',         'شهري',              'Monthly',          1),
    ('RECURRING_FREQUENCY',     'QUARTERLY',       'ربع سنوي',           'Quarterly',        2),
    ('RECURRING_FREQUENCY',     'ANNUALLY',        'سنوي',              'Annually',         3),
    ('RECURRING_FREQUENCY',     'WEEKLY',          'أسبوعي',            'Weekly',           4)
) AS v(type_key, code, name_ar, name_en, sort_order);

-- ------------------------------------------------------------
-- 3. No grants and no SEC rows here. V24 already registered FIN's module/screens/actions and V25
--    its role grants; MDL's lookup types are reference DATA, not a grantable unit, and
--    MDL_LOOKUP_TYPE carries no permission surface of its own. Reading these values at runtime
--    goes through MdlLookupApi in-process (XM-FIN-001), which is not permission-gated per lookup.
-- ------------------------------------------------------------
