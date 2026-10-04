-- ============================================================
-- V38 — Finance / General Ledger (FIN) — legacy LOAN_SYS production configuration seed
-- ============================================================
-- Source: governance/project-artifacts/backend/seed-scripts/fin-seed-production.sql (generation 2,
--   2026-09-23, accountant decisions D-01..D-18 applied; that file's own header lists them and
--   governance/project-artifacts/seed-and-cutover-findings.md §6 records why). Moved into Flyway on
--   2026-09-24 by user instruction so every environment — dev, test, live — carries the same
--   configuration from the same applied record, instead of a script run by hand.
-- Target: FIN v1 tables (V22..V34) + MDL lookup values (V18/V26) + a SEC registration guard (V24..V28).
-- Content: chart of accounts (325 rows read from LOAN_SYS.ACCOUNTS_CHART), ORG/BRANCH dimensions,
--   fiscal year 2026 (12 OPEN periods), ACCOUNTING_EVENT_TYPE / PAYMENT_METHOD lookup values,
--   21 event-type rules with 43 lines. Configuration only — NO opening balance, NO journal entry.
-- Idempotent: every INSERT is ON CONFLICT DO NOTHING on the table's natural key, the fiscal year is
--   created only when absent, so it converges a database that already received the script by hand
--   (the dev database did, 2026-09-23 13:10) and is a no-op there except for verification.
-- Verification (section 8) RAISEs on any integrity failure and Flyway rolls the migration back;
--   decision gate 8.14 has no psql variable here, so it is always strict.
-- Flyway wraps this migration in its own transaction — the script's \set / BEGIN / COMMIT are the
--   only lines removed. The 15 rules that need a MAPPING derivation are seeded is_active_fl = FALSE
--   and in v1 shape; V39 (the v2 configuration delta) moves them to v2 shape and activates them.
-- Never edit this file once applied: a later regeneration of the seed is a NEW migration.
-- ============================================================

-- ============================================================================
-- fin-seed-production.sql — FIN production configuration seed (PostgreSQL)
-- ============================================================================
-- Generated 2026-09-23 (generation 2 — accountant decisions applied) from the live legacy
-- database LOAN_SYS@localhost:1521/FREEPDB1 (Oracle AI Database 26ai Free 23.26.2.0.0, the
-- production copy used for test) and the live FIN schema (Flyway V22..V34).
-- Companion documents: seed-and-cutover-findings.md (§6 lists every decision applied here) and
-- legacy-accounting-stop.sql (the Oracle side).
--
-- WHAT THIS IS
--   Configuration only: chart of accounts, dimensions, fiscal year 2026, MDL lookup values,
--   event-type rules and rule lines, and a guard that SEC registration is present.
--   NO opening balances. NO journal entries. Nothing here posts.
--
-- WHAT IT MATCHES
--   FIN v1 as built (Flyway V22..V34) — the schema live at seed time. The FIN v2 delta
--   (governance/shared/analysis/modules/FIN/v2/, delivered 2026-09-23 ~18:00) adds
--   FIN_ACCOUNT_MAPPING, FIN_RULE_LINE_DIM and FIN_RULE_LINE.account_business_field_code, but its
--   backend execution is still PENDING (/FIN/v2/execute-backend, phase CORE). Therefore:
--     * nothing is seeded into tables that do not exist yet;
--     * rules that need a MAPPING derivation are seeded is_active_fl = FALSE, because on v1
--       EventTypeRuleDomain.resolveAccountCode throws FIN-422-MAPPING-UNSUPPORTED for them, and
--       their MAPPING lines carry the FIN_EVENT_BUSINESS_FIELD code ('PAYMENT_METHOD' /
--       'EXPENSE_TYPE_CODE') in account_derivation_value — v1's only column for it;
--     * fin-seed-v2-delta.sql (run AFTER the v2 migrations) moves those lines to v2 shape, inserts
--       the FIN_ACCOUNT_MAPPING rows (section 6 below is their reviewed preview), tags every line
--       with ORG/BRANCH and activates the 15 rules. See seed-and-cutover-findings.md §10 / OQ-13.
--
-- ACCOUNTANT DECISIONS APPLIED (2026-09-23, on the user's instruction "do recommended, act as
-- accountant" — full record in the findings file §6):
--   D-01  rules post to the leaf child, never the parent: 32 العملاء → 54 عملاء محليين,
--         85 الموردين → 86 موردين محليين  (55/87 have never been posted; 54/86 are the live leaves)
--   D-02  CONTRACT_CREATED credits 342 '00040005 المبيعات' (revenue leaf, empty) instead of
--         310 'مبيعات' (a parent filed under expenses)
--   D-04  ACCOUNT_TYPE 31/NULL → nature from the root; expense parents 175/263/234 corrected to DEBIT
--   D-05  FL=49 kept as its own event type INVESTOR_FEE with FL=59's accounts (legacy behaviour)
--   D-06  the 8 'موظف تجريبي' test accounts are seeded is_active_fl = FALSE; 3 over-long codes excluded
--   D-07  FL=8 / FL=130 retired (not seeded); SAFE_TRANSFER_TO_BANK kept (symmetric, cheap)
--   D-08  Retained Earnings stays V34's '3200'; Oracle 147 'أرباح وخسائر مرحلة' carried as history
--   D-10  one bank GL account (30) for every cheque, as legacy does — split later if wanted
--   D-11  expense-type mapping rows corrected to postable leaves (section 6b, each row says why)
--   D-12  chart topology (sales under expenses, equity under liabilities) carried verbatim: the
--         code scheme is prefix-based and reparenting would break it; rules no longer post there
--
-- HOW TO RUN
--   psql -v ON_ERROR_STOP=1 -f fin-seed-production.sql
--   ONE transaction. Section 8 verifies and RAISEs on any integrity failure (everything rolls
--   back). Re-runnable: every INSERT is ON CONFLICT DO NOTHING on the table's natural key and the
--   fiscal year is created only when absent. Decision gate 8.14 refuses to commit while an ACTIVE
--   rule line targets a non-postable account; with D-01/D-02 applied it passes.
--
-- CONVENTIONS (verbatim from V24/V26/V34)
--   surrogate PKs from seq_fin_* / seq_mdl_*, FKs resolved by natural key never by id,
--   created_by = 'SYSTEM', native BOOLEAN, bilingual name_ar / name_en on every row.
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 0. PREFLIGHT — the schema and the modules this seed depends on must be there
-- ----------------------------------------------------------------------------
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM flyway_schema_history WHERE version = '34' AND success) THEN
    RAISE EXCEPTION 'fin-seed: Flyway V34 (Retained Earnings seed) is not applied — run the application migrations first';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM sec_module_reg WHERE code = 'FIN') THEN
    RAISE EXCEPTION 'fin-seed: module FIN is not registered in SEC_MODULE_REG (V24 missing)';
  END IF;
  IF (SELECT count(*) FROM mdl_lookup_type WHERE owner_module_code = 'FIN') < 13 THEN
    RAISE EXCEPTION 'fin-seed: FIN''s 13 MDL lookup types are not registered (V26 missing)';
  END IF;
  IF to_regclass('public.fin_account_mapping') IS NOT NULL THEN
    RAISE NOTICE 'fin-seed: a FIN_ACCOUNT_MAPPING table exists — section 6 of this script is still a comment; activate it and the MAPPING rules deliberately';
  END IF;
END $$;

-- ----------------------------------------------------------------------------
-- 1. MDL LOOKUP VALUES — the two FIN-owned keys V26 deliberately left empty
--    ("host-specific, added as data", srs-fin.md §A6). Owner = FIN (RULE-MDL-001).
-- ----------------------------------------------------------------------------
-- 1a. PAYMENT_METHOD — source: SYSTEM_TANSACTION_DT 15 'نقدى', 16 'شيك' (group 'نوع الدفع');
--     the value every legacy trigger branches on as PAYMENT_TYPE_FK 0 (cash) / 1 (cheque).
--     2026 volumes: LOAN_PAYMENT 0→9,680 rows, 1→19,387; COMPLAINT_DT 0→449, 1→5,750.
INSERT INTO mdl_lookup_value (lookup_value_pk, lookup_type_id, code, name_ar, name_en, sort_order, is_active_fl, created_by, created_at)
SELECT nextval('seq_mdl_lookup_value'), t.lookup_type_pk, v.code, v.name_ar, v.name_en, v.sort_order, TRUE, 'SYSTEM', now()
FROM (VALUES
  ('CASH',   'نقدي', 'Cash',   1),   -- PAYMENT_TYPE_FK = 0
  ('CHEQUE', 'شيك',  'Cheque', 2)    -- PAYMENT_TYPE_FK = 1
) AS v(code, name_ar, name_en, sort_order)
JOIN mdl_lookup_type t ON t.key = 'PAYMENT_METHOD' AND t.owner_module_code = 'FIN'
ON CONFLICT (lookup_type_id, code) DO NOTHING;

-- 1b. ACCOUNTING_EVENT_TYPE — one value per live legacy posting branch (findings §4).
--     FIN_EVENT_TYPE_RULE.event_type_code is validated against this key (EventTypeRuleService).
INSERT INTO mdl_lookup_value (lookup_value_pk, lookup_type_id, code, name_ar, name_en, sort_order, is_active_fl, created_by, created_at)
SELECT nextval('seq_mdl_lookup_value'), t.lookup_type_pk, v.code, v.name_ar, v.name_en, v.sort_order, TRUE, 'SYSTEM', now()
FROM (VALUES
  ('INSTALLMENT_PAYMENT_RECEIVED', 'سداد قسط', 'Installment payment received', 1),   -- LOAN_PAYMENT.FL=7, screen 14
  ('INVESTOR_SUPPORT_OR_DRAWING', 'دعم / مسحوبات مستثمر', 'Investor support or drawing', 2),   -- LOAN_PAYMENT.FL=32, screen 25/32
  ('INVESTOR_SALES_INVOICE', 'فاتورة مبيعات للمستثمر', 'Investor sales invoice', 3),   -- LOAN_PAYMENT.FL=66, screen 25/66
  ('LAWYER_ADVANCE', 'مقدم المحامي', 'Lawyer advance', 4),   -- LOAN_PAYMENT.FL=58, screen 25/58
  ('INVESTOR_FEE_2_5', 'رسوم المستثمر 2.5', 'Investor fee 2.5%', 5),   -- LOAN_PAYMENT.FL=59, screen 25/59
  ('INVESTOR_FEE', 'رسوم المستثمر', 'Investor fee', 6),   -- LOAN_PAYMENT.FL=49, screen 25/59 (D-05)
  ('EXECUTION_ACTION_FEE', 'إجراء تنفيذي', 'Execution action fee', 7),   -- LOAN_PAYMENT.FL=55, screen 25/32
  ('BAEETHA_FEE', 'بعيثة', 'Ba''eetha fee', 8),   -- LOAN_PAYMENT.FL=56, screen 25/32
  ('CIVIL_FEE', 'رسوم مدنية', 'Civil fee', 9),   -- LOAN_PAYMENT.FL=57, screen 25/32
  ('CONTRACT_CREATED', 'عقد', 'Contract created', 10),   -- CONTRACT.FL=12, screen 11
  ('CONTRACT_SETTLEMENT', 'مخالصة عقد', 'Contract settlement', 11),   -- CONTRACT.FL=13, screen 15/121
  ('COMPLAINT_SETTLEMENT', 'مخالصة شكوى', 'Complaint settlement', 12),   -- COMPLAINTS.FL=1, screen 23 + ACCOUNT_PRIMARY_FL=1
  ('COURT_INSTALLMENT_PAYMENT', 'سداد قسط بالمحكمة', 'Court installment payment', 13),   -- COMPLAINT_DT.FL=43, screen 24/43
  ('LAWYER_FEE_PAYMENT', 'سداد دفعات محامي', 'Lawyer fee payment', 14),   -- COMPLAINT_DT.FL=44, screen 24/44
  ('PURCHASE_INVOICE', 'فاتورة مشتريات', 'Purchase invoice', 15),   -- INVOICE_IMPORT, screen 12
  ('EXPENSE_PAID', 'مصروف', 'Expense paid', 16),   -- EXPENSE_TYPE_DT, category 65 'مصروف'
  ('REVENUE_RECEIVED', 'إيراد', 'Revenue received', 17),   -- EXPENSE_TYPE_DT, category 64 'ايراد'
  ('CUSTODY_PAID_OUT', 'معاملات صادرة', 'Custody paid out', 18),   -- EXPENSE_TYPE_DT, category 132 'معاملات صادرة'
  ('CUSTODY_RECEIVED', 'معاملات واردة', 'Custody received', 19),   -- EXPENSE_TYPE_DT, category 131 'معاملات واردة'
  ('SAFE_TRANSFER_TO_SAFE', 'تحويل من حساب إلى خزنة', 'Transfer bank to safe', 20),   -- TRANSFER_SAFE.TYPE=25 (hardcoded 24/30)
  ('SAFE_TRANSFER_TO_BANK', 'تحويل من خزنة إلى حساب', 'Transfer safe to bank', 21)   -- TRANSFER_SAFE.TYPE=24 (hardcoded 30/24)
) AS v(code, name_ar, name_en, sort_order)
JOIN mdl_lookup_type t ON t.key = 'ACCOUNTING_EVENT_TYPE' AND t.owner_module_code = 'FIN'
ON CONFLICT (lookup_type_id, code) DO NOTHING;
-- Not seeded (D-07): LOAN_PAYMENT.FL=8 'سداد موردين' and FL=130 'دعم/مسحوبات صاحب الشركة' — coded, 0 rows ever.
-- Not seeded (D-03): ASSETS / DEPRECIABLE — 0 rows in both tables; their accounts are undecided.

-- ----------------------------------------------------------------------------
-- 2. CHART OF ACCOUNTS — LOAN_SYS.ACCOUNTS_CHART, 328 rows read, 325 seeded
-- ----------------------------------------------------------------------------
-- Carry-over rule (findings §2.1, decisions D-04/D-06):
--   * code            = ACCOUNT_CHART_NO verbatim (4-digit segments; the parent is the code prefix);
--   * parent          = ACCOUNT_CHART_FK resolved through the parent's code, never by Oracle PK;
--   * is_leaf_fl      = the account has no children in Oracle (RULE-FIN-001 / RULE-FIN-007);
--   * account_type    = from the ROOT segment: 0001 ASSET, 0002 LIABILITY, 0003 EXPENSE,
--                       0004 REVENUE, 0005 EQUITY (D-12: placement carried verbatim);
--   * nature_code     = ACCOUNT_TYPE 22 'مدين' → DEBIT, 23 'دائن' → CREDIT; the 19 rows typed
--                       31 (undefined) or NULL take the root's default nature ( -- nature DERIVED );
--                       3 expense parents typed credit are corrected to DEBIT ( -- nature FIXED D-04 );
--   * is_active_fl    = TRUE except the 8 test accounts ( -- INACTIVE D-06 );
--   * name_en         = Oracle holds Arabic names only; name_en repeats name_ar (F-07, OQ-07);
--   * EXCLUDED (3): codes longer than FIN_ACCOUNT.code VARCHAR(30) — 319 خصومات أخرى, 320 خصومات موظف تجريبي 1, 321 خصومات موظف تجريبي 2 (32 chars, never posted).
--   * The Retained Earnings account '3200' V34 seeded is left untouched (D-08).
-- Rows are inserted level by level (code length ascending) so a parent always exists before its
-- child; ON CONFLICT (code) DO NOTHING keeps a re-run from touching an existing row.
-- VALUES columns: (code, parent_code, account_type, nature, is_leaf, is_active, name_ar)
--   trailing comment: Oracle PK | Oracle ACCOUNT_TYPE | postings in 2026 (real entries)
CREATE TEMP TABLE seed_chart (
  code VARCHAR(30) PRIMARY KEY, parent_code VARCHAR(30), account_type_code VARCHAR(20),
  nature_code VARCHAR(10), is_leaf_fl BOOLEAN, is_active_fl BOOLEAN, name_ar VARCHAR(200)
) ON COMMIT DROP;
INSERT INTO seed_chart (code, parent_code, account_type_code, nature_code, is_leaf_fl, is_active_fl, name_ar) VALUES
  ('0001', NULL, 'ASSET', 'DEBIT', FALSE, TRUE, 'الأصول'),  -- pk 1 | type 22 | 2026 postings 6
  ('0002', NULL, 'LIABILITY', 'CREDIT', FALSE, TRUE, 'الإلتزامات'),  -- pk 2 | type 23 | 2026 postings 0
  ('0003', NULL, 'EXPENSE', 'DEBIT', FALSE, TRUE, 'المصروفات'),  -- pk 3 | type 22 | 2026 postings 15
  ('0004', NULL, 'REVENUE', 'CREDIT', FALSE, TRUE, 'الإيرادات'),  -- pk 4 | type 23 | 2026 postings 0
  ('0005', NULL, 'EQUITY', 'CREDIT', FALSE, TRUE, 'حقوق الملكية'),  -- pk 402 | type 23 | 2026 postings 0
  ('00010001', '0001', 'ASSET', 'DEBIT', FALSE, TRUE, 'الأصول الثابتة'),  -- pk 5 | type 22 | 2026 postings 0
  ('00010002', '0001', 'ASSET', 'DEBIT', FALSE, TRUE, 'الأصول المتداولة'),  -- pk 6 | type 22 | 2026 postings 0
  ('00010003', '0001', 'ASSET', 'DEBIT', TRUE, TRUE, 'أصول أخرى'),  -- pk 7 | type 22 | 2026 postings 0
  ('00020001', '0002', 'LIABILITY', 'CREDIT', FALSE, TRUE, 'إلتزامات متداولة'),  -- pk 84 | type 23 | 2026 postings 0
  ('00020002', '0002', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'إلتزامات طويلة الأجل'),  -- pk 114 | type 23 | 2026 postings 0
  ('00030001', '0003', 'EXPENSE', 'DEBIT', FALSE, TRUE, 'مصاريف تشغيل'),  -- pk 148 | type 22 | 2026 postings 0
  ('00030002', '0003', 'EXPENSE', 'DEBIT', FALSE, TRUE, 'مصاريف تسويقية'),  -- pk 219 | type 22 | 2026 postings 0
  ('00030003', '0003', 'EXPENSE', 'DEBIT', FALSE, TRUE, 'مصاريف عمومية'),  -- pk 237 | type 22 | 2026 postings 0
  ('00030004', '0003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'المشتريات'),  -- pk 343 | type 22 | 2026 postings 0
  ('00040001', '0004', 'REVENUE', 'CREDIT', TRUE, TRUE, 'إيرادات خدمات'),  -- pk 309 | type 23 | 2026 postings 0
  ('00040002', '0004', 'REVENUE', 'CREDIT', TRUE, TRUE, 'إيرادات مشاريع'),  -- pk 311 | type 23 | 2026 postings 0
  ('00040003', '0004', 'REVENUE', 'CREDIT', TRUE, TRUE, 'إيرادات متنوعة'),  -- pk 313 | type 23 | 2026 postings 112
  ('00040004', '0004', 'REVENUE', 'CREDIT', TRUE, TRUE, 'إيرادات الإيجارات'),  -- pk 322 | type 23 | 2026 postings 0
  ('00040005', '0004', 'REVENUE', 'CREDIT', TRUE, TRUE, 'المبيعات'),  -- pk 342 | type 23 | 2026 postings 0
  ('00040006', '0004', 'REVENUE', 'CREDIT', TRUE, TRUE, 'ايرادات أخرى'),  -- pk 382 | type 23 | 2026 postings 7
  ('00050001', '0005', 'EQUITY', 'CREDIT', TRUE, TRUE, 'رأس المال'),  -- pk 404 | type 23 | 2026 postings 0
  ('000100010001', '00010001', 'ASSET', 'DEBIT', FALSE, TRUE, 'الأصول الثابتة'),  -- pk 8 | type 22 | 2026 postings 0
  ('000100010002', '00010001', 'ASSET', 'DEBIT', FALSE, TRUE, 'مشروعات تحت التنفيذ'),  -- pk 9 | type 22 | 2026 postings 0
  ('000100020001', '00010002', 'ASSET', 'DEBIT', FALSE, TRUE, 'النقد'),  -- pk 23 | type 31 | 2026 postings 0 -- nature DERIVED
  ('000100020002', '00010002', 'ASSET', 'DEBIT', FALSE, TRUE, 'المخزون'),  -- pk 31 | type 22 | 2026 postings 0
  ('000100020003', '00010002', 'ASSET', 'DEBIT', FALSE, TRUE, 'العملاء'),  -- pk 32 | type 23 | 2026 postings 2793 -- nature FIXED D-18 (receivable = debit)
  ('000100020004', '00010002', 'ASSET', 'DEBIT', FALSE, TRUE, 'إيرادات مستحقة'),  -- pk 33 | type 22 | 2026 postings 0
  ('000100020005', '00010002', 'ASSET', 'DEBIT', FALSE, TRUE, 'سلف العاملين'),  -- pk 34 | type 22 | 2026 postings 0
  ('000100020006', '00010002', 'ASSET', 'DEBIT', FALSE, TRUE, 'عهد مالية'),  -- pk 35 | type 22 | 2026 postings 0
  ('000100020007', '00010002', 'ASSET', 'DEBIT', FALSE, TRUE, 'تأمينات لدى الغير'),  -- pk 36 | type 22 | 2026 postings 0
  ('000100020008', '00010002', 'ASSET', 'DEBIT', FALSE, TRUE, 'الإعتمادات المستندية'),  -- pk 37 | type 22 | 2026 postings 0
  ('000100020009', '00010002', 'ASSET', 'DEBIT', FALSE, TRUE, 'أوراق قبض'),  -- pk 38 | type 22 | 2026 postings 0
  ('000100020010', '00010002', 'ASSET', 'DEBIT', FALSE, TRUE, 'مصروفات مدفوعة مقدما'),  -- pk 39 | type 22 | 2026 postings 0
  ('000100020011', '00010002', 'ASSET', 'DEBIT', FALSE, TRUE, 'دفعات مقدمة للغير'),  -- pk 40 | type 22 | 2026 postings 0
  ('000100020012', '00010002', 'ASSET', 'DEBIT', FALSE, TRUE, 'توقيفات جهات مالكة'),  -- pk 41 | type NULL | 2026 postings 0 -- nature DERIVED
  ('000100020013', '00010002', 'ASSET', 'DEBIT', FALSE, TRUE, 'جارى مؤسسات ذات علاقة مدينة'),  -- pk 42 | type 22 | 2026 postings 0
  ('000100020014', '00010002', 'ASSET', 'DEBIT', FALSE, TRUE, 'أرصدة مدينة أخرى'),  -- pk 43 | type 22 | 2026 postings 3
  ('000200010001', '00020001', 'LIABILITY', 'CREDIT', FALSE, TRUE, 'حساب جاري الشركاء'),  -- pk 140 | type 23 | 2026 postings 0
  ('000200010002', '00020001', 'LIABILITY', 'CREDIT', FALSE, TRUE, 'أمانات العاملين'),  -- pk 88 | type 23 | 2026 postings 0
  ('000200010003', '00020001', 'LIABILITY', 'CREDIT', FALSE, TRUE, 'مصروفات مستحقة'),  -- pk 105 | type 23 | 2026 postings 0
  ('000200010004', '00020001', 'LIABILITY', 'CREDIT', FALSE, TRUE, 'رأس المال'),  -- pk 137 | type 23 | 2026 postings 0
  ('000200010005', '00020001', 'LIABILITY', 'CREDIT', FALSE, TRUE, 'الموردين'),  -- pk 85 | type 23 | 2026 postings 50
  ('000200010006', '00020001', 'LIABILITY', 'CREDIT', FALSE, TRUE, 'مستحقات موظفين'),  -- pk 91 | type 23 | 2026 postings 0
  ('000200010007', '00020001', 'LIABILITY', 'CREDIT', FALSE, TRUE, 'دفعات مقدمة مقبوضة من الغير'),  -- pk 96 | type 23 | 2026 postings 0
  ('000200010008', '00020001', 'LIABILITY', 'CREDIT', FALSE, TRUE, 'مستحقات مقاولين باطن'),  -- pk 99 | type NULL | 2026 postings 0 -- nature DERIVED
  ('000200010009', '00020001', 'LIABILITY', 'CREDIT', FALSE, TRUE, 'توقيفات مقاولين باطن'),  -- pk 101 | type NULL | 2026 postings 0 -- nature DERIVED
  ('000200010010', '00020001', 'LIABILITY', 'CREDIT', FALSE, TRUE, 'أوراق دفع'),  -- pk 103 | type 23 | 2026 postings 0
  ('000200010011', '00020001', 'LIABILITY', 'CREDIT', FALSE, TRUE, 'تأمينات للغير'),  -- pk 108 | type 23 | 2026 postings 0
  ('000200010012', '00020001', 'LIABILITY', 'CREDIT', FALSE, TRUE, 'جارى مؤسسات ذات علاقة دائنة'),  -- pk 110 | type 23 | 2026 postings 0
  ('000200010013', '00020001', 'LIABILITY', 'CREDIT', FALSE, TRUE, 'دائنون متنوعون'),  -- pk 112 | type 23 | 2026 postings 0
  ('000200010014', '00020001', 'LIABILITY', 'CREDIT', FALSE, TRUE, 'قروض طويلة الأجل'),  -- pk 115 | type 23 | 2026 postings 0
  ('000200010015', '00020001', 'LIABILITY', 'CREDIT', FALSE, TRUE, 'المخصصات'),  -- pk 117 | type 23 | 2026 postings 0
  ('000300010001', '00030001', 'EXPENSE', 'DEBIT', FALSE, TRUE, 'رواتب وأجور وما في حكمها'),  -- pk 149 | type 22 | 2026 postings 0
  ('000300010002', '00030001', 'EXPENSE', 'DEBIT', FALSE, TRUE, 'صيانة ومحروقات'),  -- pk 164 | type 22 | 2026 postings 0
  ('000300010003', '00030001', 'EXPENSE', 'DEBIT', FALSE, TRUE, 'مصاريف إهلاك'),  -- pk 175 | type 23 | 2026 postings 0 -- nature FIXED D-04
  ('000300010004', '00030001', 'EXPENSE', 'DEBIT', FALSE, TRUE, 'مصاريف تشغيل متنوعة'),  -- pk 187 | type 22 | 2026 postings 0
  ('000300010005', '00030001', 'EXPENSE', 'DEBIT', FALSE, TRUE, 'تكلفة المشاريع'),  -- pk 217 | type 22 | 2026 postings 0
  ('000300020001', '00030002', 'EXPENSE', 'DEBIT', FALSE, TRUE, 'رواتب وأجور وما في حكمها'),  -- pk 220 | type 22 | 2026 postings 0
  ('000300020002', '00030002', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'مصاريف إهلاك'),  -- pk 233 | type 22 | 2026 postings 0
  ('000300020003', '00030002', 'EXPENSE', 'DEBIT', FALSE, TRUE, 'مصاريف تسويقية متنوعة'),  -- pk 234 | type 23 | 2026 postings 0 -- nature FIXED D-04
  ('000300030001', '00030003', 'EXPENSE', 'DEBIT', FALSE, TRUE, 'رواتب وأجور وما في حكمها'),  -- pk 238 | type 22 | 2026 postings 0
  ('000300030002', '00030003', 'EXPENSE', 'DEBIT', FALSE, TRUE, 'مصاريف صيانة ومحروقات'),  -- pk 251 | type 22 | 2026 postings 0
  ('000300030003', '00030003', 'EXPENSE', 'DEBIT', FALSE, TRUE, 'مصاريف إهلاك'),  -- pk 263 | type 23 | 2026 postings 0 -- nature FIXED D-04
  ('000300030004', '00030003', 'EXPENSE', 'DEBIT', FALSE, TRUE, 'مصاريف عمومية متنوعة'),  -- pk 275 | type 22 | 2026 postings 59
  ('0001000100010001', '000100010001', 'ASSET', 'DEBIT', TRUE, TRUE, 'أراضي'),  -- pk 10 | type 22 | 2026 postings 0
  ('0001000100010002', '000100010001', 'ASSET', 'DEBIT', TRUE, TRUE, 'مباني'),  -- pk 11 | type 22 | 2026 postings 0
  ('0001000100010003', '000100010001', 'ASSET', 'DEBIT', TRUE, TRUE, 'آلات ومعدات'),  -- pk 12 | type 22 | 2026 postings 0
  ('0001000100010004', '000100010001', 'ASSET', 'DEBIT', TRUE, TRUE, 'وسائل نقل داخلي'),  -- pk 13 | type 22 | 2026 postings 0
  ('0001000100010005', '000100010001', 'ASSET', 'DEBIT', TRUE, TRUE, 'وسائل نقل خارجي'),  -- pk 14 | type 22 | 2026 postings 0
  ('0001000100010006', '000100010001', 'ASSET', 'DEBIT', TRUE, TRUE, 'عدد وأدوات'),  -- pk 15 | type 22 | 2026 postings 0
  ('0001000100010007', '000100010001', 'ASSET', 'DEBIT', TRUE, TRUE, 'حاويات'),  -- pk 16 | type 22 | 2026 postings 0
  ('0001000100010008', '000100010001', 'ASSET', 'DEBIT', TRUE, TRUE, 'أثاث ومعدات مكتبية'),  -- pk 17 | type 22 | 2026 postings 0
  ('0001000100010009', '000100010001', 'ASSET', 'DEBIT', TRUE, TRUE, 'أثاث سكني'),  -- pk 18 | type 22 | 2026 postings 0
  ('0001000100010010', '000100010001', 'ASSET', 'DEBIT', TRUE, TRUE, 'برامج حاسب آلي'),  -- pk 19 | type 22 | 2026 postings 0
  ('0001000100010011', '000100010001', 'ASSET', 'DEBIT', TRUE, TRUE, 'أجهزة حاسب آلي وطابعات'),  -- pk 20 | type 22 | 2026 postings 0
  ('0001000100010012', '000100010001', 'ASSET', 'DEBIT', TRUE, TRUE, 'مساكن جاهزة'),  -- pk 21 | type 22 | 2026 postings 0
  ('0001000100020001', '000100010002', 'ASSET', 'DEBIT', TRUE, TRUE, 'مشروع إنشاء مقر الشركة'),  -- pk 22 | type 22 | 2026 postings 0
  ('0001000200010001', '000100020001', 'ASSET', 'DEBIT', TRUE, TRUE, 'الصندوق'),  -- pk 24 | type 22 | 2026 postings 179
  ('0001000200010002', '000100020001', 'ASSET', 'DEBIT', TRUE, TRUE, 'صندوق الشركة'),  -- pk 25 | type 31 | 2026 postings 0 -- nature DERIVED
  ('0001000200010003', '000100020001', 'ASSET', 'DEBIT', TRUE, TRUE, 'تحويلات نقدية'),  -- pk 26 | type 31 | 2026 postings 0 -- nature DERIVED
  ('0001000200010004', '000100020001', 'ASSET', 'DEBIT', TRUE, TRUE, 'البنوك'),  -- pk 27 | type 31 | 2026 postings 0 -- nature DERIVED
  ('0001000200010005', '000100020001', 'ASSET', 'DEBIT', TRUE, TRUE, 'مصرف الراجحى'),  -- pk 28 | type 31 | 2026 postings 0 -- nature DERIVED
  ('0001000200010006', '000100020001', 'ASSET', 'DEBIT', TRUE, TRUE, 'بنك بيت التمويل'),  -- pk 29 | type 22 | 2026 postings 0
  ('0001000200010007', '000100020001', 'ASSET', 'DEBIT', TRUE, TRUE, 'البنك التجارى'),  -- pk 30 | type 22 | 2026 postings 4080
  ('0001000200020001', '000100020002', 'ASSET', 'DEBIT', TRUE, TRUE, 'المخزن الرئيسي'),  -- pk 44 | type 22 | 2026 postings 0
  ('0001000200020002', '000100020002', 'ASSET', 'DEBIT', TRUE, TRUE, 'مخزن الأصول الثابتة'),  -- pk 45 | type 22 | 2026 postings 0
  ('0001000200020003', '000100020002', 'ASSET', 'DEBIT', TRUE, TRUE, 'بضاعة اول المدة'),  -- pk 46 | type 22 | 2026 postings 0
  ('0001000200020004', '000100020002', 'ASSET', 'DEBIT', TRUE, TRUE, 'مخزن مواد تحت التشغيل'),  -- pk 47 | type 22 | 2026 postings 0
  ('0001000200020005', '000100020002', 'ASSET', 'DEBIT', TRUE, TRUE, 'مخزن منتج تام'),  -- pk 48 | type 22 | 2026 postings 0
  ('0001000200020006', '000100020002', 'ASSET', 'DEBIT', TRUE, TRUE, 'التحويلات المخزنية'),  -- pk 49 | type 22 | 2026 postings 0
  ('0001000200020007', '000100020002', 'ASSET', 'DEBIT', TRUE, TRUE, 'التسوية المخزنية'),  -- pk 50 | type 22 | 2026 postings 0
  ('0001000200020008', '000100020002', 'ASSET', 'DEBIT', TRUE, TRUE, 'مخزن بضاعة بالطريق'),  -- pk 51 | type 22 | 2026 postings 0
  ('0001000200020009', '000100020002', 'ASSET', 'DEBIT', TRUE, TRUE, 'مخزن بضاعة مستلمة'),  -- pk 52 | type 22 | 2026 postings 0
  ('0001000200020010', '000100020002', 'ASSET', 'DEBIT', TRUE, TRUE, 'مخزن بضاعة مسلمة'),  -- pk 53 | type 22 | 2026 postings 0
  ('0001000200030001', '000100020003', 'ASSET', 'DEBIT', TRUE, TRUE, 'عملاء محليين'),  -- pk 54 | type 23 | 2026 postings 0 -- nature FIXED D-18
  ('0001000200030002', '000100020003', 'ASSET', 'DEBIT', TRUE, TRUE, 'عملاء دوليين'),  -- pk 55 | type 23 | 2026 postings 0 -- nature FIXED D-18
  ('0001000200040001', '000100020004', 'ASSET', 'DEBIT', TRUE, TRUE, 'إيرادات مستحقة'),  -- pk 56 | type 22 | 2026 postings 0
  ('0001000200040002', '000100020004', 'ASSET', 'DEBIT', TRUE, TRUE, 'إيجارات غير محصلة'),  -- pk 57 | type 22 | 2026 postings 0
  ('0001000200050001', '000100020005', 'ASSET', 'DEBIT', TRUE, TRUE, 'سلف شخصية'),  -- pk 58 | type 22 | 2026 postings 0
  ('0001000200050002', '000100020005', 'ASSET', 'DEBIT', TRUE, FALSE, 'سلف موظف تجريبي 1'),  -- pk 59 | type 22 | 2026 postings 0 -- INACTIVE D-06
  ('0001000200050003', '000100020005', 'ASSET', 'DEBIT', TRUE, FALSE, 'سلف موظف تجريبي 2'),  -- pk 60 | type 22 | 2026 postings 0 -- INACTIVE D-06
  ('0001000200060001', '000100020006', 'ASSET', 'DEBIT', TRUE, TRUE, 'عهدة مؤقتة'),  -- pk 61 | type 22 | 2026 postings 0
  ('0001000200060002', '000100020006', 'ASSET', 'DEBIT', TRUE, FALSE, 'عهدة مؤقتة موظف تجريبي 1'),  -- pk 62 | type 22 | 2026 postings 0 -- INACTIVE D-06
  ('0001000200060003', '000100020006', 'ASSET', 'DEBIT', TRUE, FALSE, 'عهدة مؤقتة موظف تجريبي 2'),  -- pk 63 | type 22 | 2026 postings 0 -- INACTIVE D-06
  ('0001000200060004', '000100020006', 'ASSET', 'DEBIT', TRUE, TRUE, 'عهدة مستديمة'),  -- pk 64 | type 22 | 2026 postings 0
  ('0001000200060005', '000100020006', 'ASSET', 'DEBIT', TRUE, FALSE, 'عهدة مستديمة موظف تجريبي 1'),  -- pk 65 | type 22 | 2026 postings 0 -- INACTIVE D-06
  ('0001000200060006', '000100020006', 'ASSET', 'DEBIT', TRUE, FALSE, 'عهدة مستديمة موظف تجريبي 2'),  -- pk 66 | type 22 | 2026 postings 0 -- INACTIVE D-06
  ('0001000200070001', '000100020007', 'ASSET', 'DEBIT', TRUE, TRUE, 'خطابات ضمان إبتدائي'),  -- pk 67 | type 22 | 2026 postings 0
  ('0001000200070002', '000100020007', 'ASSET', 'DEBIT', TRUE, TRUE, 'خطابات ضمان نهائي'),  -- pk 68 | type 22 | 2026 postings 0
  ('0001000200070003', '000100020007', 'ASSET', 'DEBIT', TRUE, TRUE, 'خطابات ضمان دفعة مقدمة'),  -- pk 69 | type 22 | 2026 postings 0
  ('0001000200070004', '000100020007', 'ASSET', 'DEBIT', TRUE, TRUE, 'تأمينات لدى الغير متنوعة'),  -- pk 70 | type 22 | 2026 postings 0
  ('0001000200070005', '000100020007', 'ASSET', 'DEBIT', TRUE, TRUE, 'تأمين إيجار مكتب الشركة'),  -- pk 71 | type 22 | 2026 postings 0
  ('0001000200080001', '000100020008', 'ASSET', 'DEBIT', TRUE, TRUE, 'إعتمادات مستندية محلية'),  -- pk 72 | type 22 | 2026 postings 0
  ('0001000200080002', '000100020008', 'ASSET', 'DEBIT', TRUE, TRUE, 'إعتمادات مستندية خارجية'),  -- pk 73 | type 22 | 2026 postings 0
  ('0001000200090001', '000100020009', 'ASSET', 'DEBIT', TRUE, TRUE, 'شيكات تحت التحصيل'),  -- pk 74 | type 22 | 2026 postings 0
  ('0001000200100001', '000100020010', 'ASSET', 'DEBIT', TRUE, TRUE, 'إيجارات مدفوعة مقدما'),  -- pk 75 | type 22 | 2026 postings 0
  ('0001000200100002', '000100020010', 'ASSET', 'DEBIT', TRUE, TRUE, 'بدل سكن مدفوع مقدما'),  -- pk 76 | type 22 | 2026 postings 0
  ('0001000200100003', '000100020010', 'ASSET', 'DEBIT', TRUE, TRUE, 'مصاريف مدفوعة مقدما متنوعة'),  -- pk 77 | type 22 | 2026 postings 0
  ('0001000200100004', '000100020010', 'ASSET', 'DEBIT', TRUE, TRUE, 'رسوم تأشيرات إستقدام مدفوعة مقدما'),  -- pk 78 | type 22 | 2026 postings 0
  ('0001000200110001', '000100020011', 'ASSET', 'DEBIT', TRUE, TRUE, 'دفعات مقدمة / شركة 1'),  -- pk 79 | type 22 | 2026 postings 0
  ('0001000200110002', '000100020011', 'ASSET', 'DEBIT', TRUE, TRUE, 'دفعات مقدمة مقاولين باطن'),  -- pk 80 | type 22 | 2026 postings 0
  ('0001000200120001', '000100020012', 'ASSET', 'DEBIT', TRUE, TRUE, 'توقيفات جهة مالكة 1'),  -- pk 81 | type NULL | 2026 postings 0 -- nature DERIVED
  ('0001000200130001', '000100020013', 'ASSET', 'CREDIT', TRUE, TRUE, 'جارى/ مساهمين الشركة'),  -- pk 82 | type 23 | 2026 postings 1790
  ('0001000200140001', '000100020014', 'ASSET', 'DEBIT', TRUE, TRUE, 'حـساب /'),  -- pk 83 | type 22 | 2026 postings 0
  ('0002000100010001', '000200010001', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'جاري الشريك 1'),  -- pk 141 | type 23 | 2026 postings 8
  ('0002000100010002', '000200010001', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'جاري الشريك 2'),  -- pk 142 | type 23 | 2026 postings 0
  ('0002000100010003', '000200010001', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'الإحتياطيات'),  -- pk 143 | type 23 | 2026 postings 0
  ('0002000100010004', '000200010001', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'إحتياطي نظامي'),  -- pk 144 | type 23 | 2026 postings 0
  ('0002000100010005', '000200010001', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'أرباح وخسائر'),  -- pk 145 | type 23 | 2026 postings 0
  ('0002000100010006', '000200010001', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'أرباح وخسائر العام'),  -- pk 146 | type 23 | 2026 postings 0
  ('0002000100010007', '000200010001', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'أرباح وخسائر مرحلة'),  -- pk 147 | type 23 | 2026 postings 0
  ('0002000100020001', '000200010002', 'LIABILITY', 'CREDIT', TRUE, FALSE, 'أمانات موظف تجريبي 1'),  -- pk 89 | type 23 | 2026 postings 0 -- INACTIVE D-06
  ('0002000100020002', '000200010002', 'LIABILITY', 'CREDIT', TRUE, FALSE, 'أمانات موظف تجريبي 2'),  -- pk 90 | type 23 | 2026 postings 0 -- INACTIVE D-06
  ('0002000100030001', '000200010003', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'م . مستحقة / أتعاب مكتب المراجعة'),  -- pk 106 | type 23 | 2026 postings 0
  ('0002000100030002', '000200010003', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'م . مستحقة / شركة 1'),  -- pk 107 | type 23 | 2026 postings 0
  ('0002000100040001', '000200010004', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'رأس مال الشريك 2'),  -- pk 139 | type 23 | 2026 postings 0
  ('0002000100040002', '000200010004', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'رأس مال المساهمين'),  -- pk 138 | type 23 | 2026 postings 0
  ('0002000100050001', '000200010005', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'موردين محليين'),  -- pk 86 | type 23 | 2026 postings 0
  ('0002000100050002', '000200010005', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'موردين دوليين'),  -- pk 87 | type 23 | 2026 postings 0
  ('0002000100060001', '000200010006', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'رواتب مستحقة'),  -- pk 92 | type 23 | 2026 postings 0
  ('0002000100060002', '000200010006', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'رواتب أجازة مستحقة'),  -- pk 93 | type 23 | 2026 postings 0
  ('0002000100060003', '000200010006', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'تذاكر سفر مستحقة'),  -- pk 94 | type 23 | 2026 postings 0
  ('0002000100060004', '000200010006', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'نهاية خدمة مستحقة'),  -- pk 95 | type 23 | 2026 postings 0
  ('0002000100070001', '000200010007', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'دفعات مقدمة جهات مالكة'),  -- pk 97 | type 23 | 2026 postings 0
  ('0002000100070002', '000200010007', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'إيراد إيجار مقدم'),  -- pk 98 | type 23 | 2026 postings 0
  ('0002000100080001', '000200010008', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'مقاولين باطن مسحقة'),  -- pk 100 | type NULL | 2026 postings 0 -- nature DERIVED
  ('0002000100090001', '000200010009', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'توقيفات مقاولين باطن'),  -- pk 102 | type NULL | 2026 postings 0 -- nature DERIVED
  ('0002000100100001', '000200010010', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'شيكات تحت الدفع'),  -- pk 104 | type 23 | 2026 postings 0
  ('0002000100110001', '000200010011', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'تأمين مسترد'),  -- pk 109 | type 23 | 2026 postings 0
  ('0002000100120001', '000200010012', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'جاري / شركة 1'),  -- pk 111 | type 23 | 2026 postings 0
  ('0002000100130001', '000200010013', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'شركة 1'),  -- pk 113 | type 23 | 2026 postings 0
  ('0002000100140001', '000200010014', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'قرض رقم 1'),  -- pk 116 | type 23 | 2026 postings 0
  ('0002000100150001', '000200010015', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'مخصص إهلاك عدد وأدوات'),  -- pk 123 | type 23 | 2026 postings 0
  ('0002000100150002', '000200010015', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'مخصص مكافأة نهاية الخدمة'),  -- pk 130 | type 23 | 2026 postings 0
  ('0002000100150003', '000200010015', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'مخصص إهلاك الأصول الثابتة'),  -- pk 118 | type 23 | 2026 postings 0
  ('0002000100150004', '000200010015', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'مخصص إهلاك مباني'),  -- pk 119 | type 23 | 2026 postings 0
  ('0002000100150005', '000200010015', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'مخصص إهلاك آلات ومعدات'),  -- pk 120 | type 23 | 2026 postings 0
  ('0002000100150006', '000200010015', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'مخصص إهلاك وسائل نقل داخلي'),  -- pk 121 | type 23 | 2026 postings 0
  ('0002000100150007', '000200010015', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'مخصص إهلاك وسائل نقل خارجي'),  -- pk 122 | type 23 | 2026 postings 0
  ('0002000100150008', '000200010015', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'مخصص إهلاك حاويات'),  -- pk 124 | type 23 | 2026 postings 0
  ('0002000100150009', '000200010015', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'مخصص إهلاك أثاث ومعدات مكتبية'),  -- pk 125 | type 23 | 2026 postings 0
  ('0002000100150010', '000200010015', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'مخصص إهلاك أثاث سكني'),  -- pk 126 | type 23 | 2026 postings 0
  ('0002000100150011', '000200010015', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'مخصص إهلاك برامج حاسب آلي'),  -- pk 127 | type 23 | 2026 postings 0
  ('0002000100150012', '000200010015', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'مخصص إهلاك أجهزة حاسب آلي وطابعات'),  -- pk 128 | type 23 | 2026 postings 0
  ('0002000100150013', '000200010015', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'مخصص إهلاك مساكن جاهزة'),  -- pk 129 | type 23 | 2026 postings 0
  ('0002000100150014', '000200010015', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'مخصص مكافاة نهاية الخدمة'),  -- pk 131 | type 23 | 2026 postings 0
  ('0002000100150015', '000200010015', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'مخصص ديون مشكوك في تحصيلها'),  -- pk 132 | type 23 | 2026 postings 0
  ('0002000100150016', '000200010015', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'مخصص ديون عام 2016'),  -- pk 133 | type 23 | 2026 postings 0
  ('0002000100150017', '000200010015', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'مخصص الزكاة'),  -- pk 134 | type 23 | 2026 postings 0
  ('0002000100150018', '000200010015', 'LIABILITY', 'CREDIT', TRUE, TRUE, 'مخصص الزكاة عام 2016'),  -- pk 135 | type 23 | 2026 postings 0
  ('0003000100010001', '000300010001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / راتب أساسي'),  -- pk 150 | type 22 | 2026 postings 23
  ('0003000100010002', '000300010001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / إضافي'),  -- pk 151 | type 22 | 2026 postings 0
  ('0003000100010003', '000300010001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / بدل نقل'),  -- pk 152 | type 22 | 2026 postings 0
  ('0003000100010004', '000300010001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / بدل إعاشة'),  -- pk 153 | type 22 | 2026 postings 0
  ('0003000100010005', '000300010001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / بدل سكن'),  -- pk 154 | type 22 | 2026 postings 0
  ('0003000100010006', '000300010001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل/ بدل إتصال'),  -- pk 155 | type 22 | 2026 postings 0
  ('0003000100010007', '000300010001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / بدل أجازة'),  -- pk 156 | type 22 | 2026 postings 0
  ('0003000100010008', '000300010001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / تذاكر سفر'),  -- pk 157 | type 22 | 2026 postings 0
  ('0003000100010009', '000300010001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / نهاية خدمة'),  -- pk 158 | type 22 | 2026 postings 0
  ('0003000100010010', '000300010001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / تجديد إقامات'),  -- pk 159 | type 22 | 2026 postings 0
  ('0003000100010011', '000300010001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / تأشيرات'),  -- pk 160 | type 22 | 2026 postings 0
  ('0003000100010012', '000300010001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / علاج'),  -- pk 161 | type 22 | 2026 postings 0
  ('0003000100010013', '000300010001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / تأمينات إجتماعية'),  -- pk 162 | type 22 | 2026 postings 0
  ('0003000100010014', '000300010001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / مكافآت'),  -- pk 163 | type 22 | 2026 postings 0
  ('0003000100020001', '000300010002', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / صيانة سيارات'),  -- pk 165 | type 22 | 2026 postings 0
  ('0003000100020002', '000300010002', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / بنزين'),  -- pk 166 | type 22 | 2026 postings 0
  ('0003000100020003', '000300010002', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / ديزل'),  -- pk 167 | type 22 | 2026 postings 0
  ('0003000100020004', '000300010002', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / شحومات'),  -- pk 169 | type 22 | 2026 postings 0
  ('0003000100020005', '000300010002', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / صيانة معدات'),  -- pk 170 | type 22 | 2026 postings 0
  ('0003000100020006', '000300010002', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / صيانة مباني'),  -- pk 171 | type 22 | 2026 postings 0
  ('0003000100020007', '000300010002', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / صيانة أثاث مكتبي'),  -- pk 172 | type 22 | 2026 postings 0
  ('0003000100020008', '000300010002', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / صيانة برامج حاسب'),  -- pk 174 | type 22 | 2026 postings 0
  ('0003000100020009', '000300010002', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / زيوت'),  -- pk 168 | type 22 | 2026 postings 0
  ('0003000100020010', '000300010002', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / صيانة أثاث سكني'),  -- pk 173 | type 22 | 2026 postings 0
  ('0003000100030001', '000300010003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / مصروف إهلاك مباني'),  -- pk 176 | type 22 | 2026 postings 0
  ('0003000100030002', '000300010003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / مصروف إهلاك آلات ومعدات'),  -- pk 177 | type 22 | 2026 postings 0
  ('0003000100030003', '000300010003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / مصروف إهلاك وسائل نقل داخلي'),  -- pk 178 | type 22 | 2026 postings 0
  ('0003000100030004', '000300010003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / مصروف إهلاك وسائل نقل خارجي'),  -- pk 179 | type 22 | 2026 postings 0
  ('0003000100030005', '000300010003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / مصروف إهلاك عدد وأدوات'),  -- pk 180 | type 22 | 2026 postings 0
  ('0003000100030006', '000300010003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / مصروف إهلاك حاويات'),  -- pk 181 | type 22 | 2026 postings 0
  ('0003000100030007', '000300010003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / مصروف إهلاك أثاث سكني'),  -- pk 183 | type 22 | 2026 postings 0
  ('0003000100030008', '000300010003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / مصروف إهلاك برامج حاسب آلي'),  -- pk 184 | type 22 | 2026 postings 0
  ('0003000100030009', '000300010003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / مصروف إهلاك أجهزة حاسب آلي وطابعات'),  -- pk 185 | type 22 | 2026 postings 0
  ('0003000100030010', '000300010003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / مصروف إهلاك مساكن جاهزة'),  -- pk 186 | type 22 | 2026 postings 0
  ('0003000100030011', '000300010003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / مصروف إهلاك أثاث ومعدات مكتبية'),  -- pk 182 | type 22 | 2026 postings 0
  ('0003000100040001', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / رسوم'),  -- pk 188 | type 22 | 2026 postings 0
  ('0003000100040002', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / إشتراكات'),  -- pk 190 | type 22 | 2026 postings 0
  ('0003000100040003', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / إيجار'),  -- pk 191 | type 22 | 2026 postings 22
  ('0003000100040004', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / كهرباء'),  -- pk 192 | type 22 | 2026 postings 0
  ('0003000100040005', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / مياه'),  -- pk 193 | type 22 | 2026 postings 0
  ('0003000100040006', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / بريد'),  -- pk 194 | type 22 | 2026 postings 0
  ('0003000100040007', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / قرطاسية'),  -- pk 196 | type 22 | 2026 postings 0
  ('0003000100040008', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / مطبوعات'),  -- pk 197 | type 22 | 2026 postings 0
  ('0003000100040009', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / مواد مستهلكة'),  -- pk 198 | type 22 | 2026 postings 0
  ('0003000100040010', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / مزايا عاملين'),  -- pk 199 | type 22 | 2026 postings 0
  ('0003000100040011', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / عمولات بنكية'),  -- pk 201 | type 22 | 2026 postings 0
  ('0003000100040012', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / خصم مسموح به'),  -- pk 202 | type 22 | 2026 postings 0
  ('0003000100040013', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / ضيافة'),  -- pk 203 | type 22 | 2026 postings 0
  ('0003000100040014', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / نظافة'),  -- pk 204 | type 22 | 2026 postings 0
  ('0003000100040015', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / مأموريات'),  -- pk 205 | type 22 | 2026 postings 0
  ('0003000100040016', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / شحن ونقل'),  -- pk 206 | type 22 | 2026 postings 0
  ('0003000100040017', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / كراسات شروط'),  -- pk 207 | type 22 | 2026 postings 0
  ('0003000100040018', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / دعاية وإعلان'),  -- pk 209 | type 22 | 2026 postings 0
  ('0003000100040019', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / إنتقالات وليموزين'),  -- pk 210 | type 22 | 2026 postings 0
  ('0003000100040020', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / دعم فني'),  -- pk 211 | type 22 | 2026 postings 0
  ('0003000100040021', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / تجهيز مواقع فرق'),  -- pk 212 | type 22 | 2026 postings 0
  ('0003000100040022', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / إيجار عمالة خارجية'),  -- pk 213 | type 22 | 2026 postings 0
  ('0003000100040023', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / إقامة بفنادق وشقق مفروشة'),  -- pk 214 | type 22 | 2026 postings 0
  ('0003000100040024', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / إيجار سيارات'),  -- pk 215 | type 22 | 2026 postings 0
  ('0003000100040025', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / إيجار معدات'),  -- pk 216 | type 22 | 2026 postings 0
  ('0003000100040026', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / تصديقات'),  -- pk 189 | type 22 | 2026 postings 0
  ('0003000100040027', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / هاتف'),  -- pk 195 | type 22 | 2026 postings 0
  ('0003000100040028', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / مستلزمات تشغيل'),  -- pk 200 | type 22 | 2026 postings 0
  ('0003000100040029', '000300010004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تشغيل / أتعاب وإستشارات مهنية'),  -- pk 208 | type 22 | 2026 postings 45
  ('0003000100050001', '000300010005', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تكلفة المشاريع'),  -- pk 218 | type NULL | 2026 postings 0 -- nature DERIVED
  ('0003000200010001', '000300020001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تسويقى / راتب أساسي'),  -- pk 221 | type 22 | 2026 postings 0
  ('0003000200010002', '000300020001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تسويقى / بدل إتصال'),  -- pk 222 | type 22 | 2026 postings 0
  ('0003000200010003', '000300020001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تسويقى / بدل نقل'),  -- pk 223 | type 22 | 2026 postings 0
  ('0003000200010004', '000300020001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تسويقى / بدل سكن'),  -- pk 224 | type 22 | 2026 postings 0
  ('0003000200010005', '000300020001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تسويقى / بدل أجازة'),  -- pk 225 | type 22 | 2026 postings 0
  ('0003000200010006', '000300020001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تسويقى / نهاية خدمة'),  -- pk 226 | type 22 | 2026 postings 0
  ('0003000200010007', '000300020001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تسويقى / تجديد إقامات'),  -- pk 227 | type 22 | 2026 postings 0
  ('0003000200010008', '000300020001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تسويقى / تأشيرات'),  -- pk 228 | type 22 | 2026 postings 0
  ('0003000200010009', '000300020001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تسويقى / علاج'),  -- pk 229 | type 22 | 2026 postings 0
  ('0003000200010010', '000300020001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تسويقى / تأمينات إجتماعية'),  -- pk 230 | type 22 | 2026 postings 0
  ('0003000200010011', '000300020001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تسويقى / مكافآت'),  -- pk 231 | type 22 | 2026 postings 0
  ('0003000200010012', '000300020001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تسويقى / تأشيرات خروج وعودة'),  -- pk 232 | type 22 | 2026 postings 0
  ('0003000200030001', '000300020003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تسويقى / عمولات'),  -- pk 235 | type 22 | 2026 postings 0
  ('0003000200030002', '000300020003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تسويقى / خصم مسموح به'),  -- pk 236 | type 22 | 2026 postings 120
  ('0003000300010001', '000300030001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'بدل نقل'),  -- pk 241 | type 22 | 2026 postings 0
  ('0003000300010002', '000300030001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'بدل سكن'),  -- pk 242 | type 22 | 2026 postings 0
  ('0003000300010003', '000300030001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'بدل أجازة'),  -- pk 243 | type 22 | 2026 postings 0
  ('0003000300010004', '000300030001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'نهاية خدمة'),  -- pk 244 | type 22 | 2026 postings 0
  ('0003000300010005', '000300030001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تجديد إقامات'),  -- pk 245 | type 22 | 2026 postings 0
  ('0003000300010006', '000300030001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تأمينات إجتماعية'),  -- pk 248 | type 22 | 2026 postings 0
  ('0003000300010007', '000300030001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'مكافآت'),  -- pk 249 | type 22 | 2026 postings 0
  ('0003000300010008', '000300030001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تأشيرات خروج وعودة'),  -- pk 250 | type 22 | 2026 postings 0
  ('0003000300010009', '000300030001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'راتب أساسي'),  -- pk 239 | type 22 | 2026 postings 0
  ('0003000300010010', '000300030001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'بدل إتصال'),  -- pk 240 | type 22 | 2026 postings 0
  ('0003000300010011', '000300030001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تأشيرات'),  -- pk 246 | type 22 | 2026 postings 0
  ('0003000300010012', '000300030001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'علاج'),  -- pk 247 | type 22 | 2026 postings 0
  ('0003000300020001', '000300030002', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'صيانة سيارات'),  -- pk 252 | type 22 | 2026 postings 0
  ('0003000300020002', '000300030002', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'بنزين'),  -- pk 253 | type 22 | 2026 postings 0
  ('0003000300020003', '000300030002', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'ديزل'),  -- pk 254 | type 22 | 2026 postings 0
  ('0003000300020004', '000300030002', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'زيوت'),  -- pk 255 | type 22 | 2026 postings 0
  ('0003000300020005', '000300030002', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'شحومات'),  -- pk 256 | type 22 | 2026 postings 0
  ('0003000300020006', '000300030002', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'مواد كهرباء'),  -- pk 257 | type 22 | 2026 postings 0
  ('0003000300020007', '000300030002', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'صيانة مبانى'),  -- pk 258 | type 22 | 2026 postings 0
  ('0003000300020008', '000300030002', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'صيانة أثاث مكتبي'),  -- pk 259 | type 22 | 2026 postings 0
  ('0003000300020009', '000300030002', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'صيانة أثاث سكني'),  -- pk 260 | type 22 | 2026 postings 0
  ('0003000300020010', '000300030002', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'صيانة برامج حاسب آلي'),  -- pk 261 | type 22 | 2026 postings 0
  ('0003000300020011', '000300030002', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تأمين سيارات'),  -- pk 262 | type 22 | 2026 postings 0
  ('0003000300030001', '000300030003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'مصروف إهلاك مباني'),  -- pk 264 | type 22 | 2026 postings 0
  ('0003000300030002', '000300030003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'مصروف إهلاك آلات ومعدات'),  -- pk 265 | type 22 | 2026 postings 0
  ('0003000300030003', '000300030003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'مصروف إهلاك وسائل نقل داخلي'),  -- pk 266 | type 22 | 2026 postings 0
  ('0003000300030004', '000300030003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'مصروف إهلاك وسائل نقل خارجي'),  -- pk 267 | type 22 | 2026 postings 0
  ('0003000300030005', '000300030003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'مصروف إهلاك عدد وأدوات'),  -- pk 268 | type 22 | 2026 postings 0
  ('0003000300030006', '000300030003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'مصروف إهلاك حاويات'),  -- pk 269 | type 22 | 2026 postings 0
  ('0003000300030007', '000300030003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'مصروف إهلاك أثاث ومعدات مكتبية'),  -- pk 270 | type 22 | 2026 postings 0
  ('0003000300030008', '000300030003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'مصروف إهلاك أثاث سكني'),  -- pk 271 | type 22 | 2026 postings 0
  ('0003000300030009', '000300030003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'مصروف إهلاك برامج حاسب آلي'),  -- pk 272 | type 22 | 2026 postings 0
  ('0003000300030010', '000300030003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'مصروف إهلاك أجهزة حاسب آلي وطابعات'),  -- pk 273 | type 22 | 2026 postings 0
  ('0003000300030011', '000300030003', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'مصروف إهلاك مساكن جاهزة'),  -- pk 274 | type 22 | 2026 postings 0
  ('0003000300040001', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'رسوم'),  -- pk 276 | type 22 | 2026 postings 0
  ('0003000300040002', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'تصديقات'),  -- pk 277 | type 22 | 2026 postings 0
  ('0003000300040003', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'إشتراكات'),  -- pk 278 | type 22 | 2026 postings 0
  ('0003000300040004', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'إيجار'),  -- pk 279 | type 22 | 2026 postings 0
  ('0003000300040005', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'كهرباء'),  -- pk 280 | type 22 | 2026 postings 0
  ('0003000300040006', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'مياه'),  -- pk 281 | type 22 | 2026 postings 0
  ('0003000300040007', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'بريد'),  -- pk 282 | type 22 | 2026 postings 0
  ('0003000300040008', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'هاتف'),  -- pk 283 | type 22 | 2026 postings 0
  ('0003000300040009', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'قرطاسية'),  -- pk 284 | type 22 | 2026 postings 0
  ('0003000300040010', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'مطبوعات'),  -- pk 285 | type 22 | 2026 postings 0
  ('0003000300040011', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'مواد مستهلكة'),  -- pk 286 | type 22 | 2026 postings 0
  ('0003000300040012', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'مزايا عاملين'),  -- pk 287 | type 22 | 2026 postings 0
  ('0003000300040013', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'مستلزمات'),  -- pk 288 | type 22 | 2026 postings 0
  ('0003000300040014', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'عمولات بنكية'),  -- pk 289 | type 22 | 2026 postings 0
  ('0003000300040015', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'ضيافة'),  -- pk 290 | type 22 | 2026 postings 0
  ('0003000300040016', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'نظافة'),  -- pk 291 | type 22 | 2026 postings 0
  ('0003000300040017', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'مأموريات'),  -- pk 292 | type 22 | 2026 postings 0
  ('0003000300040018', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'شحن ونقل'),  -- pk 293 | type 22 | 2026 postings 0
  ('0003000300040019', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'كراسات شروط'),  -- pk 294 | type 22 | 2026 postings 0
  ('0003000300040020', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'أتعاب وإستشارات مهنية'),  -- pk 295 | type 22 | 2026 postings 138
  ('0003000300040021', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'دعاية وإعلان'),  -- pk 296 | type 22 | 2026 postings 0
  ('0003000300040022', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'زكاة'),  -- pk 297 | type 22 | 2026 postings 0
  ('0003000300040023', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'صدقات وتبرعات'),  -- pk 298 | type 22 | 2026 postings 0
  ('0003000300040024', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'برامج حاسب آلي'),  -- pk 299 | type 22 | 2026 postings 0
  ('0003000300040025', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'خسائر بيع أصول ثابتة'),  -- pk 300 | type 22 | 2026 postings 0
  ('0003000300040026', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'مصاريف سعي'),  -- pk 301 | type 22 | 2026 postings 0
  ('0003000300040027', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'إعلانات بالصحف الرسمية'),  -- pk 302 | type 22 | 2026 postings 0
  ('0003000300040028', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'دعم فني'),  -- pk 303 | type 22 | 2026 postings 0
  ('0003000300040029', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'ترجمة'),  -- pk 304 | type 22 | 2026 postings 0
  ('0003000300040030', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'ليموزين'),  -- pk 305 | type 22 | 2026 postings 0
  ('0003000300040031', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'أخرى'),  -- pk 306 | type 22 | 2026 postings 0
  ('0003000300040032', '000300030004', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'إيجار سيارات'),  -- pk 307 | type 22 | 2026 postings 0
  ('0003000300040033', '000300030004', 'EXPENSE', 'DEBIT', FALSE, TRUE, 'رسوم خروج وعودة'),  -- pk 308 | type 22 | 2026 postings 0
  ('00030003000400330001', '0003000300040033', 'EXPENSE', 'CREDIT', FALSE, TRUE, 'مبيعات'),  -- pk 310 | type 23 | 2026 postings 248
  ('00030003000400330002', '0003000300040033', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'مشتريات'),  -- pk 323 | type 22 | 2026 postings 50
  ('00030003000400330003', '0003000300040033', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'مصاريف مشتريات'),  -- pk 324 | type NULL | 2026 postings 0 -- nature DERIVED
  ('000300030004003300010001', '00030003000400330001', 'EXPENSE', 'DEBIT', FALSE, TRUE, 'إيرادات مشاريع'),  -- pk 312 | type NULL | 2026 postings 0 -- nature DERIVED
  ('0003000300040033000100010001', '000300030004003300010001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'أرباح بيع أصول ثابتة'),  -- pk 314 | type NULL | 2026 postings 0 -- nature DERIVED
  ('0003000300040033000100010002', '000300030004003300010001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'خصم مكتسب'),  -- pk 315 | type NULL | 2026 postings 0 -- nature DERIVED
  ('0003000300040033000100010003', '000300030004003300010001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'إيرادات سنوات سابقة'),  -- pk 316 | type NULL | 2026 postings 0 -- nature DERIVED
  ('0003000300040033000100010004', '000300030004003300010001', 'EXPENSE', 'DEBIT', FALSE, TRUE, 'إيرادات أخرى'),  -- pk 317 | type NULL | 2026 postings 0 -- nature DERIVED
  ('0003000300040033000100010005', '000300030004003300010001', 'EXPENSE', 'DEBIT', TRUE, TRUE, 'خصومات');  -- pk 318 | type NULL | 2026 postings 0 -- nature DERIVED

DO $$
DECLARE
  len INT;
  inserted INT := 0;
  n INT;
BEGIN
  FOR len IN SELECT DISTINCT length(code) FROM seed_chart ORDER BY 1 LOOP
    INSERT INTO fin_account (account_pk, code, name_ar, name_en, account_type_code, nature_code,
                             parent_account_id, is_leaf_fl, is_active_fl, created_by, created_at)
    SELECT nextval('seq_fin_account'), s.code, s.name_ar, s.name_ar, s.account_type_code, s.nature_code,
           (SELECT p.account_pk FROM fin_account p WHERE p.code = s.parent_code),
           s.is_leaf_fl, s.is_active_fl, 'SYSTEM', now()
    FROM seed_chart s
    WHERE length(s.code) = len
    ON CONFLICT (code) DO NOTHING;
    GET DIAGNOSTICS n = ROW_COUNT;
    inserted := inserted + n;
  END LOOP;
  RAISE NOTICE 'fin-seed: chart of accounts — % new rows inserted (% already present)', inserted, (SELECT count(*) FROM seed_chart) - inserted;
END $$;

-- ----------------------------------------------------------------------------
-- 3. DIMENSIONS — organisation and branch (LOAN_SYS.ORGANIZATION / ORGANIZATION_SUB)
-- ----------------------------------------------------------------------------
-- Oracle has no code column on either table; the primary key is the only stable identifier and
-- is what every business row carries (ORGANIZATION_FK / ORGANIZATION_SUB_FK), so it is the code
-- the event will carry as orgCode / branchCode. Names verbatim. F-09: v1 rule lines cannot stamp
-- a dimension, so these serve manual entries and reports until then.
INSERT INTO fin_dimension (dimension_pk, code, name_ar, name_en, is_active_fl, created_by, created_at)
VALUES
  (nextval('seq_fin_dimension'), 'ORG',    'المؤسسة', 'Organisation', TRUE, 'SYSTEM', now()),
  (nextval('seq_fin_dimension'), 'BRANCH', 'الفرع',   'Branch',       TRUE, 'SYSTEM', now())
ON CONFLICT (code) DO NOTHING;

INSERT INTO fin_dimension_value (dimension_value_pk, dimension_id, code, name_ar, name_en, sort_order, is_active_fl, created_by, created_at)
SELECT nextval('seq_fin_dimension_value'), d.dimension_pk, v.code, v.name_ar, v.name_en, v.sort_order, TRUE, 'SYSTEM', now()
FROM (VALUES
  ('ORG',    '1', 'شركه الوسائط',   'Al Wasaet Company',               1),  -- ORGANIZATION_PK 1
  ('BRANCH', '1', 'افضل وسيط',      'Afdal Waseet (Al Jahra)',         1),  -- ORGANIZATION_SUB_PK 1, الجهراء
  ('BRANCH', '2', 'الوسيط الذهبى',  'Al Waseet Al Dhahabi (Fahaheel)', 2),  -- ORGANIZATION_SUB_PK 2, الفحيحيل
  ('BRANCH', '3', 'الوسائط الفضيه', 'Al Wasaet Al Fiddiya (Mirqab)',   3)   -- ORGANIZATION_SUB_PK 3, المرقاب
) AS v(dim_code, code, name_ar, name_en, sort_order)
JOIN fin_dimension d ON d.code = v.dim_code
ON CONFLICT (dimension_id, code) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 4. FISCAL YEAR 2026 — twelve calendar-month periods, ALL OPEN
-- ----------------------------------------------------------------------------
-- Why all twelve are OPEN (findings §7, research R-02): step 3 replays 1 Jan 2026 → today and
-- posts an opening entry into January; RULE-FIN-008 rejects a post into any non-OPEN period and
-- RULE-FIN-014 forbids reopening. Nothing may be closed before the replay is reconciled.
-- Period boundaries and names reproduce FiscalPeriodDomain.generatedPeriod exactly (calendar
-- months, Arabic/English month names), so a year created here is indistinguishable from one
-- created through API-FIN-023.
-- Idempotent: created only when no fiscal year with code '2026' exists; an existing one is left
-- untouched and section 8 checks its shape.
DO $$
DECLARE
  fy BIGINT;
  m INT;
  ar TEXT[] := ARRAY['يناير','فبراير','مارس','أبريل','مايو','يونيو','يوليو','أغسطس','سبتمبر','أكتوبر','نوفمبر','ديسمبر'];
  en TEXT[] := ARRAY['January','February','March','April','May','June','July','August','September','October','November','December'];
BEGIN
  IF EXISTS (SELECT 1 FROM fin_fiscal_year WHERE code = '2026') THEN
    RAISE NOTICE 'fin-seed: fiscal year 2026 already exists — left untouched (verified in section 8)';
    RETURN;
  END IF;
  fy := nextval('seq_fin_fiscal_year');
  INSERT INTO fin_fiscal_year (fiscal_year_pk, code, start_date, end_date, status_code, is_active_fl, created_by, created_at)
  VALUES (fy, '2026', DATE '2026-01-01', DATE '2026-12-31', 'OPEN', TRUE, 'SYSTEM', now());
  FOR m IN 1..12 LOOP
    INSERT INTO fin_fiscal_period (fiscal_period_pk, fiscal_year_id, period_no, name_ar, name_en,
                                   start_date, end_date, status_code, created_by, created_at)
    VALUES (nextval('seq_fin_fiscal_period'), fy, m, ar[m], en[m],
            make_date(2026, m, 1), (make_date(2026, m, 1) + INTERVAL '1 month' - INTERVAL '1 day')::date,
            'OPEN', 'SYSTEM', now());
  END LOOP;
  RAISE NOTICE 'fin-seed: fiscal year 2026 created with 12 OPEN periods';
END $$;

-- ----------------------------------------------------------------------------
-- 5. EVENT-TYPE RULES AND RULE LINES — one rule per live legacy posting branch
-- ----------------------------------------------------------------------------
-- Source of every account: the posting-template rows of DAILY_RESTRICTIONS_MASTER (SCREEN_FK IS
-- NOT NULL) joined to DAILY_RESTRICTIONS_DT, keyed (SCREEN_FK, DAILY_SCREEN_STATUS_FK,
-- RESTRICTIONS_TYPE_FK 22=debit/23=credit, ACCOUNT_PRIMARY_FL) — findings §2.2 — cross-checked
-- against the accounts the 2026 entries actually hit (§2.3), then D-01/D-02 applied (the comment
-- on each line says the legacy account and the decision).
--
-- Every line: amount_source_type_code = 'FIELD', amount_source_value = the event's named amount
-- ('amount'; PURCHASE_INVOICE names grossAmount / paidAmount / remainingAmount), distribution
-- 'FIXED', is_remainder_fl FALSE — no percentage line, therefore no remainder line (RULE-FIN-003).
--
-- CONSTANT = account_derivation_value IS the FIN account code (works today).
-- MAPPING  = account_derivation_value is the mapping-set KEY ('PAYMENT_METHOD' / 'EXPENSE_TYPE');
--            NO STORE EXISTS in v1 → every rule with a MAPPING line is seeded is_active_fl = FALSE.
-- The legacy "cash unless cheque" swap becomes two explicit mapping rows (section 6a), never a
-- default. Legacy sign flipping (negative amount swaps sides) is not reproduced (F-10, step 3).
CREATE TEMP TABLE seed_rule (
  event_type_code VARCHAR(50) PRIMARY KEY, name_ar VARCHAR(150), name_en VARCHAR(150), is_active_fl BOOLEAN, note TEXT
) ON COMMIT DROP;
CREATE TEMP TABLE seed_rule_line (
  event_type_code VARCHAR(50), line_no NUMERIC, deriv_type VARCHAR(20), deriv_value TEXT,
  amount_field TEXT, direction VARCHAR(10), legacy TEXT
) ON COMMIT DROP;

INSERT INTO seed_rule VALUES
  ('INSTALLMENT_PAYMENT_RECEIVED', 'سداد قسط', 'Installment payment received', FALSE, '[INACTIVE: needs mapping store] LOAN_PAYMENT.FL=7 screen 14; 2026: 1,897'),
  ('INVESTOR_SUPPORT_OR_DRAWING', 'دعم / مسحوبات مستثمر', 'Investor support or drawing', FALSE, '[INACTIVE: needs mapping store] LOAN_PAYMENT.FL=32 screen 25/32; 2026: 506'),
  ('INVESTOR_SALES_INVOICE', 'فاتورة مبيعات للمستثمر', 'Investor sales invoice', TRUE, 'LOAN_PAYMENT.FL=66 screen 25/66; 2026: 248. No cash side — CONSTANT only'),
  ('LAWYER_ADVANCE', 'مقدم المحامي', 'Lawyer advance', FALSE, '[INACTIVE: needs mapping store] LOAN_PAYMENT.FL=58 screen 25/58; 2026: 208'),
  ('INVESTOR_FEE_2_5', 'رسوم المستثمر 2.5', 'Investor fee 2.5%', FALSE, '[INACTIVE: needs mapping store] LOAN_PAYMENT.FL=59 screen 25/59; 2026: 208'),
  ('INVESTOR_FEE', 'رسوم المستثمر', 'Investor fee', FALSE, '[INACTIVE: needs mapping store] LOAN_PAYMENT.FL=49 → template 25/59 (D-05: kept distinct, same accounts); 2026: 100'),
  ('EXECUTION_ACTION_FEE', 'إجراء تنفيذي', 'Execution action fee', FALSE, '[INACTIVE: needs mapping store] LOAN_PAYMENT.FL=55 → template 25/32; 2026: 163'),
  ('BAEETHA_FEE', 'بعيثة', 'Ba''eetha fee', FALSE, '[INACTIVE: needs mapping store] LOAN_PAYMENT.FL=56 → template 25/32; 2026: 35'),
  ('CIVIL_FEE', 'رسوم مدنية', 'Civil fee', FALSE, '[INACTIVE: needs mapping store] LOAN_PAYMENT.FL=57 → template 25/32; 2026: 74'),
  ('CONTRACT_CREATED', 'عقد', 'Contract created', TRUE, 'CONTRACT.FL=12 screen 11; 2026: 225. CONSTANT only'),
  ('CONTRACT_SETTLEMENT', 'مخالصة عقد', 'Contract settlement', TRUE, 'CONTRACT.FL=13 screen 15/121 (also the QUIT entry); 2026: 7 + 21. CONSTANT only'),
  ('COMPLAINT_SETTLEMENT', 'مخالصة شكوى', 'Complaint settlement', TRUE, 'COMPLAINTS.FL=1 screen 23 ACCOUNT_PRIMARY_FL=1; 2026: 93. CONSTANT only'),
  ('COURT_INSTALLMENT_PAYMENT', 'سداد قسط بالمحكمة', 'Court installment payment', FALSE, '[INACTIVE: needs mapping store] COMPLAINT_DT.FL=43 screen 24/43; 2026: 528'),
  ('LAWYER_FEE_PAYMENT', 'سداد دفعات محامي', 'Lawyer fee payment', FALSE, '[INACTIVE: needs mapping store] COMPLAINT_DT.FL=44 screen 24/44; 2026: 138'),
  ('PURCHASE_INVOICE', 'فاتورة مشتريات', 'Purchase invoice', FALSE, '[INACTIVE: needs mapping store] INVOICE_IMPORT screen 12; 2026: 50 entries × 3 lines'),
  ('EXPENSE_PAID', 'مصروف', 'Expense paid', FALSE, '[INACTIVE: needs mapping store] EXPENSE_TYPE_DT category 65; 2026: 231'),
  ('REVENUE_RECEIVED', 'إيراد', 'Revenue received', FALSE, '[INACTIVE: needs mapping store] EXPENSE_TYPE_DT category 64; 2026: 119'),
  ('CUSTODY_PAID_OUT', 'معاملات صادرة', 'Custody paid out', FALSE, '[INACTIVE: needs mapping store] EXPENSE_TYPE_DT category 132; 2026: 10'),
  ('CUSTODY_RECEIVED', 'معاملات واردة', 'Custody received', FALSE, '[INACTIVE: needs mapping store] EXPENSE_TYPE_DT category 131; 2026: 1'),
  ('SAFE_TRANSFER_TO_SAFE', 'تحويل من حساب إلى خزنة', 'Transfer bank to safe', TRUE, 'TRANSFER_SAFE.TRANSFER_SAFE_TYPE_FK=25; accounts hardcoded in the trigger; 2026: 26'),
  ('SAFE_TRANSFER_TO_BANK', 'تحويل من خزنة إلى حساب', 'Transfer safe to bank', TRUE, 'TRANSFER_SAFE.TRANSFER_SAFE_TYPE_FK=24; coded, 0 rows in 2026 (D-07: kept)');

INSERT INTO seed_rule_line VALUES
  ('INSTALLMENT_PAYMENT_RECEIVED', 1, 'MAPPING', 'PAYMENT_METHOD', 'amount', 'DEBIT', 'template 14/22 → 24 الصندوق, 30 when cheque'),
  ('INSTALLMENT_PAYMENT_RECEIVED', 2, 'CONSTANT', '0001000200030001', 'amount', 'CREDIT', 'template 14/23 → 32 العملاء; D-01 → 54 عملاء محليين'),
  ('INVESTOR_SUPPORT_OR_DRAWING', 1, 'CONSTANT', '0001000200130001', 'amount', 'DEBIT', 'books 2026: DR 82 (469/505 rows negative → legacy flipped template 25/32; D-14)'),
  ('INVESTOR_SUPPORT_OR_DRAWING', 2, 'MAPPING', 'PAYMENT_METHOD', 'amount', 'CREDIT', 'books 2026: CR 24/30 by payment method'),
  ('INVESTOR_SALES_INVOICE', 1, 'CONSTANT', '0001000200030001', 'amount', 'DEBIT', 'books 2026: DR 32 العملاء (all 248 rows negative → template flipped; D-14); D-01 → 54'),
  ('INVESTOR_SALES_INVOICE', 2, 'CONSTANT', '0001000200130001', 'amount', 'CREDIT', 'books 2026: CR 82'),
  ('LAWYER_ADVANCE', 1, 'CONSTANT', '0001000200130001', 'amount', 'DEBIT', 'books 2026: DR 82 (all 208 rows negative → template flipped; D-14)'),
  ('LAWYER_ADVANCE', 2, 'MAPPING', 'PAYMENT_METHOD', 'amount', 'CREDIT', 'books 2026: CR 24/30 by payment method'),
  ('INVESTOR_FEE_2_5', 1, 'CONSTANT', '0001000200130001', 'amount', 'DEBIT', 'books 2026: DR 82 (all 208 rows negative → template flipped; D-14)'),
  ('INVESTOR_FEE_2_5', 2, 'MAPPING', 'PAYMENT_METHOD', 'amount', 'CREDIT', 'books 2026: CR 24/30 by payment method'),
  ('INVESTOR_FEE', 1, 'MAPPING', 'PAYMENT_METHOD', 'amount', 'DEBIT', 'template 25/59/22 → 24, 30 when cheque'),
  ('INVESTOR_FEE', 2, 'CONSTANT', '0001000200130001', 'amount', 'CREDIT', 'template 25/59/23 → 82'),
  ('EXECUTION_ACTION_FEE', 1, 'CONSTANT', '0001000200130001', 'amount', 'DEBIT', 'observed 2026: DR 82 (legacy amounts negative → sides flipped vs template) F-10'),
  ('EXECUTION_ACTION_FEE', 2, 'MAPPING', 'PAYMENT_METHOD', 'amount', 'CREDIT', 'observed 2026: CR 24/30 by payment method'),
  ('BAEETHA_FEE', 1, 'CONSTANT', '0001000200130001', 'amount', 'DEBIT', 'observed 2026: DR 82 F-10'),
  ('BAEETHA_FEE', 2, 'MAPPING', 'PAYMENT_METHOD', 'amount', 'CREDIT', 'observed 2026: CR 24/30'),
  ('CIVIL_FEE', 1, 'CONSTANT', '0001000200130001', 'amount', 'DEBIT', 'observed 2026: DR 82 F-10'),
  ('CIVIL_FEE', 2, 'MAPPING', 'PAYMENT_METHOD', 'amount', 'CREDIT', 'observed 2026: CR 24/30'),
  ('CONTRACT_CREATED', 1, 'CONSTANT', '0001000200130001', 'amount', 'DEBIT', 'template 11/22 → 82'),
  ('CONTRACT_CREATED', 2, 'CONSTANT', '00040005', 'amount', 'CREDIT', 'template 11/23 → 310 مبيعات (parent, under expenses); D-02 → 342 ''00040005 المبيعات'''),
  ('CONTRACT_SETTLEMENT', 1, 'CONSTANT', '0003000200030002', 'amount', 'DEBIT', 'template 15/121/22 → 236 تسويقى / خصم مسموح به'),
  ('CONTRACT_SETTLEMENT', 2, 'CONSTANT', '0001000200030001', 'amount', 'CREDIT', 'template 15/121/23 → 32 العملاء; D-01 → 54'),
  ('COMPLAINT_SETTLEMENT', 1, 'CONSTANT', '0003000200030002', 'amount', 'DEBIT', 'template 23/43/22 → 236'),
  ('COMPLAINT_SETTLEMENT', 2, 'CONSTANT', '0001000200030001', 'amount', 'CREDIT', 'template 23/43/23 → 32 العملاء; D-01 → 54'),
  ('COURT_INSTALLMENT_PAYMENT', 1, 'MAPPING', 'PAYMENT_METHOD', 'amount', 'DEBIT', 'template 24/43/22 → 24, 30 when cheque'),
  ('COURT_INSTALLMENT_PAYMENT', 2, 'CONSTANT', '0001000200030001', 'amount', 'CREDIT', 'template 24/43/23 → 32 العملاء; D-01 → 54'),
  ('LAWYER_FEE_PAYMENT', 1, 'MAPPING', 'PAYMENT_METHOD', 'amount', 'DEBIT', 'template 24/44/22 → 24, 30 when cheque'),
  ('LAWYER_FEE_PAYMENT', 2, 'CONSTANT', '0003000300040020', 'amount', 'CREDIT', 'template 24/44/23 → 295 أتعاب وإستشارات مهنية'),
  ('PURCHASE_INVOICE', 1, 'CONSTANT', '00030003000400330002', 'grossAmount', 'DEBIT', 'template 12/22 PRIMARY → 323 مشتريات; amount = CASH_PRICE'),
  ('PURCHASE_INVOICE', 2, 'MAPPING', 'PAYMENT_METHOD', 'paidAmount', 'CREDIT', 'template 12/23 PRIMARY → 24, 30 when cheque; amount = PAYMENT'),
  ('PURCHASE_INVOICE', 3, 'CONSTANT', '0002000100050001', 'remainingAmount', 'CREDIT', 'template 12/23 non-primary → 85 الموردين; D-01 → 86 موردين محليين; amount = REMAINING'),
  ('EXPENSE_PAID', 1, 'MAPPING', 'EXPENSE_TYPE_CODE', 'amount', 'DEBIT', 'EXPENSE_TYPE.ACCOUNT_CHART_FK of the event''s expenseTypeCode (section 6b, D-11)'),
  ('EXPENSE_PAID', 2, 'MAPPING', 'PAYMENT_METHOD', 'amount', 'CREDIT', '24 cash / 30 cheque'),
  ('REVENUE_RECEIVED', 1, 'MAPPING', 'PAYMENT_METHOD', 'amount', 'DEBIT', '24 cash / 30 cheque'),
  ('REVENUE_RECEIVED', 2, 'MAPPING', 'EXPENSE_TYPE_CODE', 'amount', 'CREDIT', 'EXPENSE_TYPE.ACCOUNT_CHART_FK (section 6b)'),
  ('CUSTODY_PAID_OUT', 1, 'MAPPING', 'EXPENSE_TYPE_CODE', 'amount', 'DEBIT', 'section 6b (141 / 83 / 61 / 26 per type)'),
  ('CUSTODY_PAID_OUT', 2, 'MAPPING', 'PAYMENT_METHOD', 'amount', 'CREDIT', '24 cash / 30 cheque'),
  ('CUSTODY_RECEIVED', 1, 'MAPPING', 'PAYMENT_METHOD', 'amount', 'DEBIT', '24 cash / 30 cheque'),
  ('CUSTODY_RECEIVED', 2, 'MAPPING', 'EXPENSE_TYPE_CODE', 'amount', 'CREDIT', 'section 6b'),
  ('SAFE_TRANSFER_TO_SAFE', 1, 'CONSTANT', '0001000200010001', 'amount', 'DEBIT', 'p_account_chart_fk_debit := 24 الصندوق'),
  ('SAFE_TRANSFER_TO_SAFE', 2, 'CONSTANT', '0001000200010007', 'amount', 'CREDIT', 'p_account_chart_fk_credit := 30 البنك التجارى'),
  ('SAFE_TRANSFER_TO_BANK', 1, 'CONSTANT', '0001000200010007', 'amount', 'DEBIT', 'p_account_chart_fk_debit := 30'),
  ('SAFE_TRANSFER_TO_BANK', 2, 'CONSTANT', '0001000200010001', 'amount', 'CREDIT', 'p_account_chart_fk_credit := 24');

INSERT INTO fin_event_type_rule (event_type_rule_pk, event_type_code, name_ar, name_en, is_active_fl, created_by, created_at)
SELECT nextval('seq_fin_event_type_rule'), r.event_type_code, r.name_ar, r.name_en, r.is_active_fl, 'SYSTEM', now()
FROM seed_rule r
ON CONFLICT (event_type_code) DO NOTHING;

-- FIN_RULE_LINE has no natural unique key: lines are added only to a rule that has none yet.
INSERT INTO fin_rule_line (rule_line_pk, event_type_rule_id, line_no, account_derivation_type_code,
                           account_derivation_value, amount_source_type_code, amount_source_value,
                           direction_code, distribution_type_code, is_remainder_fl, created_at)
SELECT nextval('seq_fin_rule_line'), r.event_type_rule_pk, l.line_no, l.deriv_type, l.deriv_value,
       'FIELD', l.amount_field, l.direction, 'FIXED', FALSE, now()
FROM seed_rule_line l
JOIN fin_event_type_rule r ON r.event_type_code = l.event_type_code
WHERE NOT EXISTS (SELECT 1 FROM fin_rule_line x WHERE x.event_type_rule_id = r.event_type_rule_pk)
ORDER BY l.event_type_code, l.line_no;

-- ----------------------------------------------------------------------------
-- 6. ACCOUNT MAPPINGS — explicit rows, one per real business value. NO STORE EXISTS IN v1.
-- ----------------------------------------------------------------------------
-- Intentionally a comment: the complete, reviewed content of the mapping store the approved
-- design calls for. Not inserted because no FIN table can hold it and this seed invents no DDL
-- (F-01, D-09). Each row: (mapping_key, business_value, FIN account code, source / decision).
--
-- 6a. PAYMENT_METHOD — the "cash unless cheque" swap made explicit (no default, no fallback):
--   ('PAYMENT_METHOD', 'CASH',   '0001000200010001')  -- 24 الصندوق        ← PAYMENT_TYPE_FK = 0
--   ('PAYMENT_METHOD', 'CHEQUE', '0001000200010007')  -- 30 البنك التجارى  ← PAYMENT_TYPE_FK = 1 (D-10: one bank account, as legacy)
--
-- 6b. EXPENSE_TYPE_CODE — LOAN_SYS.EXPENSE_TYPE.ACCOUNT_CHART_FK, with D-11 corrections where the
--     legacy target is a root/parent (RULE-FIN-007 would reject it) or NULL:
--   ('EXPENSE_TYPE_CODE', '1', '00040003')  -- 1 ايرادات عقود (cat 64) → legacy 313 إيرادات متنوعة; 2026 rows 112
--   ('EXPENSE_TYPE_CODE', '181', '00040001')  -- 181 ايرادات تسديد عملاء (cat 64) → legacy 309 إيرادات خدمات; 2026 rows 0
--   ('EXPENSE_TYPE_CODE', '561', '00040006')  -- 561 ايرادات اخرى (cat 64) → legacy 382 ايرادات أخرى; 2026 rows 7
--   ('EXPENSE_TYPE_CODE', '61', '0003000100040029')  -- 61 مصروفات أخرى (cat 65) → legacy 208 تشغيل / أتعاب وإستشارات مهنية; 2026 rows 45
--   ('EXPENSE_TYPE_CODE', '81', '0003000100010001')  -- 81 مرتبات (cat 65) → legacy 1 الأصول; 2026 rows 28  ← D-11: was 1 الأصول (root); 2026 postings actually hit 150 تشغيل / راتب أساسي
--   ('EXPENSE_TYPE_CODE', '101', '0003000300040031')  -- 101 مصروفات عموميه (cat 65) → legacy 3 المصروفات; 2026 rows 75  ← D-11: was 3 المصروفات (root) / 275 (parent); → 306 أخرى, the misc-expenses leaf
--   ('EXPENSE_TYPE_CODE', '161', '0003000100040003')  -- 161 ايجارات (cat 65) → legacy 191 تشغيل / إيجار; 2026 rows 18
--   -- ('EXPENSE_TYPE_CODE', '361', NULL)  -- 361 سلف (cat 65): NULL in Oracle, 0 rows — left unmapped; decide when first used
--   -- ('EXPENSE_TYPE_CODE', '362', NULL)  -- 362 قرض (cat 65): NULL in Oracle, 0 rows — left unmapped
--   ('EXPENSE_TYPE_CODE', '324', '0001000200140001')  -- 324 سداد قرضات (cat 131) → legacy 43 أرصدة مدينة أخرى; 2026 rows 1  ← D-11: was 43 أرصدة مدينة أخرى (parent); → its only leaf 83 'حـساب /' — rename it (OQ-10)
--   ('EXPENSE_TYPE_CODE', '381', '0001000200060001')  -- 381 عهدة مبالغ للمحاماه والمحكمة (cat 131) → legacy 61 عهدة مؤقتة; 2026 rows 0
--   ('EXPENSE_TYPE_CODE', '401', '0002000100010001')  -- 401 سداد مسحوبات علوش (cat 131) → legacy 141 جاري الشريك 1; 2026 rows 0
--   ('EXPENSE_TYPE_CODE', '422', '0001000200010003')  -- 422 تحويلات بنكية (cat 131) → legacy 6 الأصول المتداولة; 2026 rows 0  ← D-11: was 6 الأصول المتداولة (parent); → 26 تحويلات نقدية
--   ('EXPENSE_TYPE_CODE', '281', '0002000100010001')  -- 281 سحوبات علوش من المكتب (cat 132) → legacy 141 جاري الشريك 1; 2026 rows 8
--   ('EXPENSE_TYPE_CODE', '282', '0001000200140001')  -- 282 قرضات للمستثمريين (cat 132) → legacy 43 أرصدة مدينة أخرى; 2026 rows 2  ← D-11: was 43 (parent); → 83 as above
--   ('EXPENSE_TYPE_CODE', '382', '0001000200060001')  -- 382 عهدة مبالغ للمحاماه والمحكمه (cat 132) → legacy 61 عهدة مؤقتة; 2026 rows 0
--   ('EXPENSE_TYPE_CODE', '424', '0001000200010003')  -- 424 تحويلات بنكيه صادرة (cat 132) → legacy 6 الأصول المتداولة; 2026 rows 0  ← D-11: was 6 الأصول المتداولة (parent); → 26 تحويلات نقدية
--   EXPENSE_TYPE_CUST / EXPENSE_TYPE_DT_CUST: payroll side-copy, not a posting source (findings §2.4).

-- ----------------------------------------------------------------------------
-- 7. SEC REGISTRATION — already complete (V24, V27, V28); nothing to insert, only to assert
-- ----------------------------------------------------------------------------
DO $$
DECLARE missing TEXT;
BEGIN
  SELECT string_agg(p, ', ') INTO missing FROM unnest(ARRAY[
    'PERM_FIN_ACCOUNTS_VIEW','PERM_FIN_ACCOUNTS_CREATE','PERM_FIN_ACCOUNTS_UPDATE',
    'PERM_FIN_DIMENSIONS_VIEW','PERM_FIN_DIMENSIONS_CREATE','PERM_FIN_DIMENSIONS_UPDATE',
    'PERM_FIN_RULES_VIEW','PERM_FIN_RULES_CREATE','PERM_FIN_RULES_UPDATE',
    'PERM_FIN_JOURNAL_ENTRIES_VIEW','PERM_FIN_JOURNAL_ENTRIES_CREATE','PERM_FIN_JOURNAL_ENTRIES_REVERSE',
    'PERM_FIN_PERIODS_VIEW','PERM_FIN_PERIODS_CREATE','PERM_FIN_PERIODS_UPDATE','PERM_FIN_PERIODS_CLOSE_APPROVE',
    'PERM_FIN_ACCOUNT_LEDGER_VIEW','PERM_FIN_TRIAL_BALANCE_VIEW','PERM_FIN_BALANCE_SHEET_VIEW',
    'PERM_FIN_INCOME_STATEMENT_VIEW','PERM_FIN_DIMENSION_REPORTS_VIEW']) AS p
  WHERE NOT EXISTS (SELECT 1 FROM sec_action_reg a WHERE a.permission_code = p);
  IF missing IS NOT NULL THEN
    RAISE EXCEPTION 'fin-seed: SEC action registration incomplete — missing: %', missing;
  END IF;
  IF (SELECT count(*) FROM sec_screen_reg s JOIN sec_module_reg m ON m.module_reg_pk = s.module_id WHERE m.code = 'FIN') < 12 THEN
    RAISE EXCEPTION 'fin-seed: fewer than the 12 FIN screens V24 registers are present';
  END IF;
  RAISE NOTICE 'fin-seed: SEC registration for FIN verified (module, 12 screens, 27 actions)';
END $$;

-- ----------------------------------------------------------------------------
-- 8. VERIFICATION — proves the seed landed; any failure RAISEs and the transaction rolls back
-- ----------------------------------------------------------------------------
DO $$
DECLARE
  v_count INT; v_list TEXT; allow TEXT;
BEGIN
  SELECT count(*) INTO v_count FROM fin_account a JOIN seed_chart s ON s.code = a.code;
  IF v_count <> (SELECT count(*) FROM seed_chart) THEN
    RAISE EXCEPTION 'fin-seed 8.1: % of % chart rows present', v_count, (SELECT count(*) FROM seed_chart);
  END IF;
  SELECT string_agg(s.code, ', ') INTO v_list FROM seed_chart s JOIN fin_account a ON a.code = s.code
   WHERE s.parent_code IS NOT NULL AND a.parent_account_id IS NULL;
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'fin-seed 8.2: orphaned accounts: %', v_list; END IF;
  SELECT string_agg(a.code, ', ') INTO v_list FROM fin_account a JOIN seed_chart s ON s.code = a.code
   WHERE a.is_leaf_fl AND EXISTS (SELECT 1 FROM fin_account c WHERE c.parent_account_id = a.account_pk);
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'fin-seed 8.3: leaf accounts that have children: %', v_list; END IF;
  SELECT string_agg(c.code, ', ') INTO v_list FROM fin_account c JOIN fin_account p ON p.account_pk = c.parent_account_id
   JOIN seed_chart s ON s.code = c.code WHERE c.account_type_code <> p.account_type_code;
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'fin-seed 8.4: child/parent account type mismatch: %', v_list; END IF;
  SELECT count(*) INTO v_count FROM fin_account WHERE is_retained_earnings_fl;
  IF v_count <> 1 THEN RAISE EXCEPTION 'fin-seed 8.5: % retained-earnings accounts (expected 1)', v_count; END IF;
  IF (SELECT count(*) FROM fin_dimension_value v JOIN fin_dimension d ON d.dimension_pk = v.dimension_id WHERE d.code IN ('ORG','BRANCH')) < 4 THEN
    RAISE EXCEPTION 'fin-seed 8.6: ORG/BRANCH dimension values incomplete';
  END IF;
  SELECT count(*) INTO v_count FROM fin_fiscal_period p JOIN fin_fiscal_year y ON y.fiscal_year_pk = p.fiscal_year_id
   WHERE y.code = '2026' AND y.start_date = DATE '2026-01-01' AND y.end_date = DATE '2026-12-31';
  IF v_count <> 12 THEN RAISE EXCEPTION 'fin-seed 8.7: fiscal year 2026 has % periods / wrong bounds (expected 12, 2026-01-01..2026-12-31)', v_count; END IF;
  SELECT string_agg(p.period_no::text, ',') INTO v_list FROM fin_fiscal_period p JOIN fin_fiscal_year y ON y.fiscal_year_pk = p.fiscal_year_id
   WHERE y.code = '2026' AND p.status_code <> 'OPEN';
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'fin-seed 8.7: 2026 periods not OPEN: % — the replay (step 3) cannot post into them', v_list; END IF;
  SELECT count(*) INTO v_count FROM (
    SELECT p.end_date, lead(p.start_date) OVER (ORDER BY p.period_no) AS next_start
    FROM fin_fiscal_period p JOIN fin_fiscal_year y ON y.fiscal_year_pk = p.fiscal_year_id WHERE y.code = '2026') x
   WHERE x.next_start IS NOT NULL AND x.next_start <> x.end_date + 1;
  IF v_count > 0 THEN RAISE EXCEPTION 'fin-seed 8.7: 2026 periods are not contiguous'; END IF;
  SELECT string_agg(code, ', ') INTO v_list FROM fin_fiscal_year
   WHERE code <> '2026' AND start_date <= DATE '2026-12-31' AND end_date >= DATE '2026-01-01';
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'fin-seed 8.8: fiscal years overlapping 2026 exist: % — resolvePeriodContaining would post into them (F-12)', v_list; END IF;
  SELECT string_agg(r.event_type_code, ', ') INTO v_list FROM fin_event_type_rule r JOIN seed_rule s ON s.event_type_code = r.event_type_code
   WHERE NOT EXISTS (SELECT 1 FROM mdl_lookup_value v JOIN mdl_lookup_type t ON t.lookup_type_pk = v.lookup_type_id
                     WHERE t.key = 'ACCOUNTING_EVENT_TYPE' AND v.code = r.event_type_code AND v.is_active_fl);
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'fin-seed 8.9: rules without a registered event type: %', v_list; END IF;
  SELECT string_agg(s.event_type_code, ', ') INTO v_list FROM seed_rule s JOIN fin_event_type_rule r ON r.event_type_code = s.event_type_code
   WHERE NOT EXISTS (SELECT 1 FROM fin_rule_line l WHERE l.event_type_rule_id = r.event_type_rule_pk);
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'fin-seed 8.10: rules with no lines: %', v_list; END IF;
  SELECT string_agg(r.event_type_code || '#' || l.line_no || '→' || l.account_derivation_value, ', ') INTO v_list
    FROM fin_rule_line l JOIN fin_event_type_rule r ON r.event_type_rule_pk = l.event_type_rule_id JOIN seed_rule s ON s.event_type_code = r.event_type_code
   WHERE l.account_derivation_type_code = 'CONSTANT' AND NOT EXISTS (SELECT 1 FROM fin_account a WHERE a.code = l.account_derivation_value);
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'fin-seed 8.10: CONSTANT lines citing unknown accounts: %', v_list; END IF;
  SELECT string_agg(r.event_type_code, ', ') INTO v_list FROM fin_event_type_rule r JOIN seed_rule s ON s.event_type_code = r.event_type_code
   WHERE NOT EXISTS (SELECT 1 FROM fin_rule_line l WHERE l.event_type_rule_id = r.event_type_rule_pk AND l.direction_code = 'DEBIT')
      OR NOT EXISTS (SELECT 1 FROM fin_rule_line l WHERE l.event_type_rule_id = r.event_type_rule_pk AND l.direction_code = 'CREDIT');
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'fin-seed 8.11: one-sided rules: %', v_list; END IF;
  SELECT count(*) INTO v_count FROM fin_rule_line l JOIN fin_event_type_rule r ON r.event_type_rule_pk = l.event_type_rule_id
   JOIN seed_rule s ON s.event_type_code = r.event_type_code WHERE l.account_derivation_type_code = 'DIRECT';
  IF v_count > 0 THEN RAISE EXCEPTION 'fin-seed 8.12: % DIRECT rule lines — an account code would travel in the event', v_count; END IF;
  IF to_regclass('public.fin_account_mapping') IS NULL THEN
    SELECT string_agg(DISTINCT r.event_type_code, ', ') INTO v_list FROM fin_rule_line l JOIN fin_event_type_rule r ON r.event_type_rule_pk = l.event_type_rule_id
     JOIN seed_rule s ON s.event_type_code = r.event_type_code WHERE r.is_active_fl AND l.account_derivation_type_code = 'MAPPING';
    IF v_list IS NOT NULL THEN RAISE EXCEPTION 'fin-seed 8.13: ACTIVE rules with MAPPING lines but no mapping store — every event would fail FIN-422: %', v_list; END IF;
  END IF;
  SELECT string_agg(r.event_type_code || '#' || l.line_no || '→' || a.code || ' ' || a.name_ar, ' ; ') INTO v_list
    FROM fin_rule_line l JOIN fin_event_type_rule r ON r.event_type_rule_pk = l.event_type_rule_id JOIN seed_rule s ON s.event_type_code = r.event_type_code
    JOIN fin_account a ON a.code = l.account_derivation_value
   WHERE r.is_active_fl AND l.account_derivation_type_code = 'CONSTANT' AND (NOT a.is_leaf_fl OR NOT a.is_active_fl);
  allow := current_setting('fin_seed.allow_nonpostable_targets', TRUE);
  IF v_list IS NOT NULL THEN
    IF coalesce(allow, 'off') = 'on' THEN
      RAISE WARNING 'fin-seed 8.14: committing with ACTIVE rule lines that target NON-POSTABLE accounts: %', v_list;
    ELSE
      RAISE EXCEPTION 'fin-seed 8.14: ACTIVE rule lines target NON-POSTABLE (non-leaf/inactive) accounts: %', v_list;
    END IF;
  END IF;
  -- 8.15 inactive rules also target postable accounts (so activating them later needs no chart change)
  SELECT string_agg(r.event_type_code || '#' || l.line_no || '→' || a.code, ' ; ') INTO v_list
    FROM fin_rule_line l JOIN fin_event_type_rule r ON r.event_type_rule_pk = l.event_type_rule_id JOIN seed_rule s ON s.event_type_code = r.event_type_code
    JOIN fin_account a ON a.code = l.account_derivation_value
   WHERE NOT r.is_active_fl AND l.account_derivation_type_code = 'CONSTANT' AND (NOT a.is_leaf_fl OR NOT a.is_active_fl);
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'fin-seed 8.15: INACTIVE rule lines target non-postable accounts: %', v_list; END IF;
  -- 8.16 this seed created no journal entry
  IF EXISTS (SELECT 1 FROM fin_journal_entry WHERE created_by = 'SYSTEM' AND created_at >= now() - INTERVAL '1 minute') THEN
    RAISE EXCEPTION 'fin-seed 8.16: a journal entry was created by this run — the seed must be configuration only';
  END IF;
  RAISE NOTICE 'fin-seed: verification passed — accounts %, dimension values %, rules % (active %), rule lines %, event types %, 2026 periods %',
    (SELECT count(*) FROM fin_account a JOIN seed_chart s ON s.code = a.code),
    (SELECT count(*) FROM fin_dimension_value v JOIN fin_dimension d ON d.dimension_pk = v.dimension_id WHERE d.code IN ('ORG','BRANCH')),
    (SELECT count(*) FROM fin_event_type_rule r JOIN seed_rule s ON s.event_type_code = r.event_type_code),
    (SELECT count(*) FROM fin_event_type_rule r JOIN seed_rule s ON s.event_type_code = r.event_type_code WHERE r.is_active_fl),
    (SELECT count(*) FROM fin_rule_line l JOIN fin_event_type_rule r ON r.event_type_rule_pk = l.event_type_rule_id JOIN seed_rule s ON s.event_type_code = r.event_type_code),
    (SELECT count(*) FROM mdl_lookup_value v JOIN mdl_lookup_type t ON t.lookup_type_pk = v.lookup_type_id WHERE t.key = 'ACCOUNTING_EVENT_TYPE' AND v.code IN (SELECT event_type_code FROM seed_rule)),
    (SELECT count(*) FROM fin_fiscal_period p JOIN fin_fiscal_year y ON y.fiscal_year_pk = p.fiscal_year_id WHERE y.code = '2026');
END $$;

