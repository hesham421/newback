-- ============================================================
-- V39 — Finance / General Ledger (FIN) v2 — configuration delta on the V38 seed
-- ============================================================
-- Source: governance/project-artifacts/backend/seed-scripts/fin-seed-v2-delta.sql (2026-09-23,
--   D-16 twins added 2026-09-24). Moved into Flyway on 2026-09-24 by user instruction, right after
--   V38, so the v1 seed and its v2 completion are one applied sequence.
-- Requires: V35 (FIN_ACCOUNT_MAPPING, FIN_RULE_LINE_DIM, FIN_RULE_LINE.account_business_field_code),
--   V36 (FIN_EVENT_BUSINESS_FIELD / FIN_DIMENSION_VALUE_SOURCE / CHEQUE) and V38 — section 0
--   refuses to run otherwise.
-- Content:
--   1. OQ-13 — the 19 v1-shape MAPPING lines moved to v2 shape (V35 added the CHECK without this
--      data step; the rows pass the CHECK but EventEntryService finds a NULL business field)
--   2. FIN_ACCOUNT_MAPPING — PAYMENT_METHOD (CASH → 24, CHEQUE → 30) per event type and
--      EXPENSE_TYPE_CODE per real LOAN_SYS.EXPENSE_TYPE id (D-10, D-11)
--   3. FIN_RULE_LINE_DIM — ORG ← ORGANISATION_CODE, BRANCH ← BRANCH_CODE on every seeded line
--   4. activation of the 15 rules V38 left inactive
--   4b. D-16 — the 21 <TYPE>_REVERSED twins (lines mirrored, mappings and tags copied) so every
--      UPDATE and DELETE on the legacy tables can post
--   5. verification 5.1–5.10, scoped to created_by = 'SYSTEM'; RAISE → Flyway rolls back
-- Idempotent (NOT EXISTS / ON CONFLICT on every insert; converging UPDATEs).
-- Flyway wraps this migration in its own transaction — the script's \set / BEGIN / COMMIT are the
--   only lines removed. Never edit this file once applied.
-- ============================================================

-- ============================================================================
-- fin-seed-v2-delta.sql — FIN v2 configuration delta (PostgreSQL)
-- ============================================================================
-- Prepared 2026-09-23 against the FIN v2 analysis delivered the same day:
--   governance/shared/analysis/modules/FIN/v2/P2/db-script-fin.md   (BLOCK 1–7 DDL, DBF-FIN-148..165)
--   governance/shared/analysis/modules/FIN/v2/P1/srs-fin.md         (RULE-FIN-018..028, A6 lookups)
--   governance/shared/backend/modules/FIN/packages/v2/backend-execution/SVC-API/SVC-API-INT.md
-- Companion: fin-seed-production.sql (v1 shape, already applied) and seed-and-cutover-findings.md §10.
--
-- WHEN TO RUN
--   AFTER the v2 backend execution (/FIN/v2/execute-backend, phases CORE → SEC-BE) has landed its
--   Flyway migrations: FIN_ACCOUNT_MAPPING, FIN_RULE_LINE_DIM, FIN_RULE_LINE.account_business_field_code,
--   the MDL keys FIN_EVENT_BUSINESS_FIELD / FIN_DIMENSION_VALUE_SOURCE and the CHEQUE value.
--   Section 0 refuses to run until every one of those exists. Never run it against a v1 schema.
--
-- ┌───────────────────────────────────────────────────────────────────────────────────────────┐
-- │ WARNING FOR THE v2 DATA-DOM EXECUTOR — read before writing the v2 schema migration          │
-- │                                                                                             │
-- │ The v1 seed left 19 MAPPING rule lines whose account_derivation_value holds the business-   │
-- │ field key ('PAYMENT_METHOD' / 'EXPENSE_TYPE_CODE') because v1 had no other column for it   │
-- │ and the column was NOT NULL. v2's CHK_FIN_RULE_LINE_DERIVATION_SPEC                         │
-- │   CHECK ((account_derivation_value IS NULL) <> (account_business_field_code IS NULL))       │
-- │ lets those rows THROUGH (value set, field null → exactly one null → TRUE) — so V35 applied   │
-- │ cleanly on 2026-09-23 and the rows are still in v1 shape. They are wrong all the same:      │
-- │ EventEntryService reads account_business_field_code for a MAPPING line and finds NULL, so   │
-- │ every one of the 15 rules fails at post with FIN-422-MISSING-BUSINESS-FIELD. (Corrected     │
-- │ 2026-09-24: the earlier text here claimed the constraint would reject them — it does not.)  │
-- │ The migration should have moved the value between ADD COLUMN and ADD CONSTRAINT            │
-- │ (db-script BLOCK 2 → BLOCK 5c); V35 did not, so section 1 below does it:                   │
-- │                                                                                             │
-- │   UPDATE fin_rule_line                                                                      │
-- │      SET account_business_field_code = account_derivation_value,                            │
-- │          account_derivation_value    = NULL                                                 │
-- │    WHERE account_derivation_type_code = 'MAPPING'                                           │
-- │      AND account_business_field_code IS NULL                                                │
-- │      AND account_derivation_value IN ('PAYMENT_METHOD','EXPENSE_TYPE_CODE');                │
-- │                                                                                             │
-- │ Section 1 below repeats it as a no-op safety net. Recorded as seed-and-cutover-findings     │
-- │ OQ-13; the executor should file it in the v2 execution-state api_doc_gaps[] as well.        │
-- └───────────────────────────────────────────────────────────────────────────────────────────┘
--
-- WHAT THIS ADDS (configuration only — no journal entry, no opening balance)
--   1. the v1→v2 shape move of the MAPPING rule lines (safety net, see above)
--   2. FIN_ACCOUNT_MAPPING — one row per (event type, business field, business value):
--        PAYMENT_METHOD: CASH → 24 الصندوق, CHEQUE → 30 البنك التجارى, for each of the 15 event
--        types that carry a MAPPING(PAYMENT_METHOD) line   (RULE-FIN-020 keys the lookup by event type)
--        EXPENSE_TYPE_CODE: one row per real LOAN_SYS.EXPENSE_TYPE id, per the event type of its
--        category (decisions D-11 already applied to the targets)
--   3. FIN_RULE_LINE_DIM — every seeded rule line tagged ORG ← ORGANISATION_CODE and
--        BRANCH ← BRANCH_CODE (value source BUSINESS_FIELD; the event carries the Oracle PK as the
--        code, which is exactly FIN_DIMENSION_VALUE.code — RULE-FIN-027, ADR-FIN-026)
--   4. activation of the 15 rules the v1 seed left inactive
--   5. verification, RAISE on failure → everything rolls back
--
-- HOW TO RUN
--   psql -v ON_ERROR_STOP=1 -f fin-seed-v2-delta.sql      (one transaction, re-runnable)
-- ============================================================================


-- ----------------------------------------------------------------------------
-- 0. PREFLIGHT — v2 schema and v2 lookups must be present; v1 seed must be present
-- ----------------------------------------------------------------------------
DO $$
DECLARE missing TEXT := '';
BEGIN
  IF to_regclass('public.fin_account_mapping') IS NULL THEN missing := missing || ' FIN_ACCOUNT_MAPPING'; END IF;
  IF to_regclass('public.fin_rule_line_dim')   IS NULL THEN missing := missing || ' FIN_RULE_LINE_DIM'; END IF;
  IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'fin_rule_line' AND column_name = 'account_business_field_code') THEN
    missing := missing || ' FIN_RULE_LINE.account_business_field_code';
  END IF;
  IF to_regclass('public.seq_fin_account_mapping') IS NULL THEN missing := missing || ' SEQ_FIN_ACCOUNT_MAPPING'; END IF;
  IF to_regclass('public.seq_fin_rule_line_dim')   IS NULL THEN missing := missing || ' SEQ_FIN_RULE_LINE_DIM'; END IF;
  IF missing <> '' THEN
    RAISE EXCEPTION 'fin-seed-v2: v2 schema not present — missing:% . Run /FIN/v2/execute-backend (DATA-DOM) first.', missing;
  END IF;
  -- MDL v2 keys and values (seeded by the v2 DATA-DOM-LOOKUP migration)
  SELECT string_agg(k, ', ') INTO missing FROM unnest(ARRAY['FIN_EVENT_BUSINESS_FIELD','FIN_DIMENSION_VALUE_SOURCE']) AS k
   WHERE NOT EXISTS (SELECT 1 FROM mdl_lookup_type t WHERE t.key = k AND t.owner_module_code = 'FIN');
  IF missing IS NOT NULL THEN RAISE EXCEPTION 'fin-seed-v2: MDL lookup types missing: %', missing; END IF;
  SELECT string_agg(c, ', ') INTO missing FROM unnest(ARRAY['PAYMENT_METHOD','EXPENSE_TYPE_CODE','ORGANISATION_CODE','BRANCH_CODE']) AS c
   WHERE NOT EXISTS (SELECT 1 FROM mdl_lookup_value v JOIN mdl_lookup_type t ON t.lookup_type_pk = v.lookup_type_id
                     WHERE t.key = 'FIN_EVENT_BUSINESS_FIELD' AND v.code = c AND v.is_active_fl);
  IF missing IS NOT NULL THEN RAISE EXCEPTION 'fin-seed-v2: FIN_EVENT_BUSINESS_FIELD values missing: %', missing; END IF;
  SELECT string_agg(c, ', ') INTO missing FROM unnest(ARRAY['CONSTANT','BUSINESS_FIELD']) AS c
   WHERE NOT EXISTS (SELECT 1 FROM mdl_lookup_value v JOIN mdl_lookup_type t ON t.lookup_type_pk = v.lookup_type_id
                     WHERE t.key = 'FIN_DIMENSION_VALUE_SOURCE' AND v.code = c AND v.is_active_fl);
  IF missing IS NOT NULL THEN RAISE EXCEPTION 'fin-seed-v2: FIN_DIMENSION_VALUE_SOURCE values missing: %', missing; END IF;
  SELECT string_agg(c, ', ') INTO missing FROM unnest(ARRAY['CASH','CHEQUE']) AS c
   WHERE NOT EXISTS (SELECT 1 FROM mdl_lookup_value v JOIN mdl_lookup_type t ON t.lookup_type_pk = v.lookup_type_id
                     WHERE t.key = 'PAYMENT_METHOD' AND v.code = c AND v.is_active_fl);
  IF missing IS NOT NULL THEN RAISE EXCEPTION 'fin-seed-v2: PAYMENT_METHOD values missing: % (CASH comes from fin-seed-production.sql, CHEQUE from v2 MDL seed)', missing; END IF;
  -- v1 seed present
  IF (SELECT count(*) FROM fin_event_type_rule WHERE event_type_code IN ('INSTALLMENT_PAYMENT_RECEIVED','CONTRACT_CREATED','EXPENSE_PAID')) < 3 THEN
    RAISE EXCEPTION 'fin-seed-v2: the v1 seed (fin-seed-production.sql) is not applied — run it first';
  END IF;
  IF NOT EXISTS (SELECT 1 FROM fin_dimension WHERE code = 'ORG') OR NOT EXISTS (SELECT 1 FROM fin_dimension WHERE code = 'BRANCH') THEN
    RAISE EXCEPTION 'fin-seed-v2: ORG / BRANCH dimensions missing';
  END IF;
END $$;

-- ----------------------------------------------------------------------------
-- 1. v1 → v2 shape of the MAPPING rule lines (safety net; a no-op when the migration did it)
-- ----------------------------------------------------------------------------
UPDATE fin_rule_line
   SET account_business_field_code = account_derivation_value,
       account_derivation_value    = NULL
 WHERE account_derivation_type_code = 'MAPPING'
   AND account_business_field_code IS NULL
   AND account_derivation_value IN ('PAYMENT_METHOD', 'EXPENSE_TYPE_CODE');

-- ----------------------------------------------------------------------------
-- 2. FIN_ACCOUNT_MAPPING — explicit rows, one per real business value, per event type
-- ----------------------------------------------------------------------------
-- RULE-FIN-022 makes (event_type_code, business_field_code, business_value) unique among ACTIVE
-- rows via a partial unique index (BLOCK 7, name assigned by the migration), so idempotency is a
-- NOT EXISTS on the active key rather than ON CONFLICT.
CREATE TEMP TABLE seed_mapping (
  event_type_code VARCHAR(50), business_field_code VARCHAR(50), business_value VARCHAR(100),
  account_code VARCHAR(30), legacy TEXT
) ON COMMIT DROP;

-- 2a. PAYMENT_METHOD — the legacy "cash unless cheque" swap, explicit, for every event type whose
--     rule carries a MAPPING(PAYMENT_METHOD) line (fin-seed-production.sql §5). Source: every
--     accounting trigger's CASE on PAYMENT_TYPE_FK (0 → 24 الصندوق, 1 → 30 البنك التجارى); D-10.
INSERT INTO seed_mapping
SELECT r.event_type_code, 'PAYMENT_METHOD', v.business_value, v.account_code, v.legacy
FROM fin_event_type_rule r
JOIN fin_rule_line l ON l.event_type_rule_id = r.event_type_rule_pk
CROSS JOIN (VALUES
  ('CASH',   '0001000200010001', 'PAYMENT_TYPE_FK = 0 → 24 الصندوق'),
  ('CHEQUE', '0001000200010007', 'PAYMENT_TYPE_FK = 1 → 30 البنك التجارى')
) AS v(business_value, account_code, legacy)
WHERE r.created_by = 'SYSTEM'   -- only the seeded rules; never configuration added through the API
  AND l.account_derivation_type_code = 'MAPPING' AND l.account_business_field_code = 'PAYMENT_METHOD'
GROUP BY r.event_type_code, v.business_value, v.account_code, v.legacy;

-- 2b. EXPENSE_TYPE_CODE — LOAN_SYS.EXPENSE_TYPE.EXPENSE_TYPE_ID (the code the event carries) →
--     account, per the event type of the type's category (64 ايراد → REVENUE_RECEIVED,
--     65 مصروف → EXPENSE_PAID, 131 معاملات واردة → CUSTODY_RECEIVED, 132 معاملات صادرة → CUSTODY_PAID_OUT).
--     Targets are the D-11 corrected leaves (findings §6). 361/362 have no account in Oracle and
--     0 rows — deliberately absent: RULE-FIN-020 rejects them loudly if ever used.
INSERT INTO seed_mapping VALUES
  ('REVENUE_RECEIVED', 'EXPENSE_TYPE_CODE', '1',   '00040003',           'EXPENSE_TYPE 1 ايرادات عقود → 313 إيرادات متنوعة (2026: 112 rows)'),
  ('REVENUE_RECEIVED', 'EXPENSE_TYPE_CODE', '181', '00040001',           'EXPENSE_TYPE 181 ايرادات تسديد عملاء → 309 إيرادات خدمات'),
  ('REVENUE_RECEIVED', 'EXPENSE_TYPE_CODE', '561', '00040006',           'EXPENSE_TYPE 561 ايرادات اخرى → 382 ايرادات أخرى (2026: 7)'),
  ('EXPENSE_PAID',     'EXPENSE_TYPE_CODE', '61',  '0003000100040029',   'EXPENSE_TYPE 61 مصروفات أخرى → 208 تشغيل / أتعاب وإستشارات مهنية (2026: 45)'),
  ('EXPENSE_PAID',     'EXPENSE_TYPE_CODE', '81',  '0003000100010001',   'EXPENSE_TYPE 81 مرتبات → 150 تشغيل / راتب أساسي (D-11: legacy pointed at root 1 الأصول) (2026: 28)'),
  ('EXPENSE_PAID',     'EXPENSE_TYPE_CODE', '101', '0003000300040031',   'EXPENSE_TYPE 101 مصروفات عموميه → 306 أخرى (D-11: legacy pointed at root 3 / parent 275) (2026: 75)'),
  ('EXPENSE_PAID',     'EXPENSE_TYPE_CODE', '161', '0003000100040003',   'EXPENSE_TYPE 161 ايجارات → 191 تشغيل / إيجار (2026: 18)'),
  ('CUSTODY_RECEIVED', 'EXPENSE_TYPE_CODE', '324', '0001000200140001',   'EXPENSE_TYPE 324 سداد قرضات → 83 (D-11: legacy parent 43; OQ-10 rename 83) (2026: 1)'),
  ('CUSTODY_RECEIVED', 'EXPENSE_TYPE_CODE', '381', '0001000200060001',   'EXPENSE_TYPE 381 عهدة مبالغ للمحاماه والمحكمة → 61 عهدة مؤقتة'),
  ('CUSTODY_RECEIVED', 'EXPENSE_TYPE_CODE', '401', '0002000100010001',   'EXPENSE_TYPE 401 سداد مسحوبات علوش → 141 جاري الشريك 1'),
  ('CUSTODY_RECEIVED', 'EXPENSE_TYPE_CODE', '422', '0001000200010003',   'EXPENSE_TYPE 422 تحويلات بنكية → 26 تحويلات نقدية (D-11: legacy parent 6)'),
  ('CUSTODY_PAID_OUT', 'EXPENSE_TYPE_CODE', '281', '0002000100010001',   'EXPENSE_TYPE 281 سحوبات علوش من المكتب → 141 جاري الشريك 1 (2026: 8)'),
  ('CUSTODY_PAID_OUT', 'EXPENSE_TYPE_CODE', '282', '0001000200140001',   'EXPENSE_TYPE 282 قرضات للمستثمريين → 83 (D-11: legacy parent 43) (2026: 2)'),
  ('CUSTODY_PAID_OUT', 'EXPENSE_TYPE_CODE', '382', '0001000200060001',   'EXPENSE_TYPE 382 عهدة مبالغ للمحاماه والمحكمه → 61 عهدة مؤقتة'),
  ('CUSTODY_PAID_OUT', 'EXPENSE_TYPE_CODE', '424', '0001000200010003',   'EXPENSE_TYPE 424 تحويلات بنكيه صادرة → 26 تحويلات نقدية (D-11: legacy parent 6)');

INSERT INTO fin_account_mapping (account_mapping_pk, event_type_code, business_field_code, business_value,
                                 account_id, is_active_fl, created_by, created_at)
SELECT nextval('seq_fin_account_mapping'), s.event_type_code, s.business_field_code, s.business_value,
       a.account_pk, TRUE, 'SYSTEM', now()
FROM seed_mapping s
JOIN fin_account a ON a.code = s.account_code
WHERE NOT EXISTS (SELECT 1 FROM fin_account_mapping m
                   WHERE m.event_type_code = s.event_type_code AND m.business_field_code = s.business_field_code
                     AND m.business_value = s.business_value AND m.is_active_fl)
ORDER BY s.event_type_code, s.business_field_code, s.business_value;

-- ----------------------------------------------------------------------------
-- 3. FIN_RULE_LINE_DIM — ORG and BRANCH on every seeded rule line, sourced from the event
-- ----------------------------------------------------------------------------
-- Legacy stamps ORGANIZATION_FK / ORGANIZATION_SUB_FK on every journal master (findings §2.5);
-- the event carries them as fields.ORGANISATION_CODE / fields.BRANCH_CODE = the Oracle PK, which
-- is the FIN_DIMENSION_VALUE.code seeded in v1 — so BUSINESS_FIELD resolves without translation
-- (ADR-FIN-012 deferral does not bite). RULE-FIN-026: BUSINESS_FIELD tag ⇒ dimension_value_id NULL.
INSERT INTO fin_rule_line_dim (rule_line_dimension_pk, value_source_code, business_field_code,
                               rule_line_id, dimension_id, dimension_value_id, created_at)
SELECT nextval('seq_fin_rule_line_dim'), 'BUSINESS_FIELD', t.business_field_code,
       l.rule_line_pk, d.dimension_pk, NULL, now()
FROM fin_rule_line l
JOIN fin_event_type_rule r ON r.event_type_rule_pk = l.event_type_rule_id
CROSS JOIN (VALUES ('ORG', 'ORGANISATION_CODE'), ('BRANCH', 'BRANCH_CODE')) AS t(dim_code, business_field_code)
JOIN fin_dimension d ON d.code = t.dim_code
WHERE r.created_by = 'SYSTEM'
  AND r.event_type_code IN (SELECT DISTINCT event_type_code FROM seed_mapping
                            UNION SELECT event_type_code FROM fin_event_type_rule
                             WHERE event_type_code IN ('INVESTOR_SALES_INVOICE','CONTRACT_CREATED','CONTRACT_SETTLEMENT',
                                                       'COMPLAINT_SETTLEMENT','SAFE_TRANSFER_TO_SAFE','SAFE_TRANSFER_TO_BANK'))
ON CONFLICT (rule_line_id, dimension_id) DO NOTHING;   -- UQ_FIN_RULE_LINE_DIM_LINE_DIM (RULE-FIN-025)

-- ----------------------------------------------------------------------------
-- 4. ACTIVATE the rules the v1 seed left inactive — their MAPPING lines now resolve
-- ----------------------------------------------------------------------------
UPDATE fin_event_type_rule r
   SET is_active_fl = TRUE, updated_by = 'SYSTEM', updated_at = now()
 WHERE r.created_by = 'SYSTEM' AND NOT r.is_active_fl
   AND r.event_type_code IN (SELECT DISTINCT event_type_code FROM seed_mapping);

-- ----------------------------------------------------------------------------
-- 4b. <TYPE>_REVERSED twins — every INSERT has an UPDATE and a DELETE (D-16, closes OQ-15)
-- ----------------------------------------------------------------------------
-- The legacy triggers fired on INSERT OR UPDATE OR DELETE and rewrote the journal in place. The
-- Oracle emitters keep that behaviour by sending, on UPDATE, an undo of the old value plus the new
-- value, and on DELETE an undo — the undo travels as eventTypeCode <TYPE>_REVERSED. A negative
-- amount on a branch whose normal sign is positive (a genuine contra row, F-10 / D-14) travels the
-- same way. FIN rejects any event type without an active rule (FIN-404-NO-ACTIVE-RULE), so without
-- these twins every edit and deletion would die in the exception queue.
--
-- A twin is the base rule with every line's direction swapped and everything else identical:
-- same derivation (CONSTANT code / MAPPING business field), same amount field, same ORG/BRANCH
-- tags — and, because FIN_ACCOUNT_MAPPING is keyed per event type (RULE-FIN-020), a copy of every
-- mapping row under the twin's code. The twin's active flag follows its base, always.
--
-- The consumer uses the twin only as the fallback: when the undo carries
-- fields.reversesEventReference (the package found the emission it cancels) the consumer calls
-- API-FIN-021 POST /journal-entries/{id}/reverse on the entry it posted under that reference —
-- FIN's own linked reversal. The twin is for rows whose original was never emitted (they predate
-- the cutover marker; their accounting is in the legacy journal / opening balances) and for
-- contra-sign rows. Both paths net to zero against the original.

CREATE TEMP TABLE seed_base_rule ON COMMIT DROP AS
SELECT event_type_rule_pk, event_type_code, name_ar, name_en, is_active_fl
  FROM fin_event_type_rule
 WHERE created_by = 'SYSTEM' AND event_type_code NOT LIKE '%\_REVERSED';

-- 4b.1 MDL ACCOUNTING_EVENT_TYPE values (rule creation validates against them, API-FIN-011)
INSERT INTO mdl_lookup_value (lookup_value_pk, lookup_type_id, code, name_ar, name_en, sort_order, is_active_fl, created_by, created_at)
SELECT nextval('seq_mdl_lookup_value'), t.lookup_type_pk, b.event_type_code || '_REVERSED',
       'عكس ' || b.name_ar, 'Reversal of ' || lower(b.name_en), 100 + row_number() OVER (ORDER BY b.event_type_code), TRUE, 'SYSTEM', now()
FROM seed_base_rule b
JOIN mdl_lookup_type t ON t.key = 'ACCOUNTING_EVENT_TYPE' AND t.owner_module_code = 'FIN'
ON CONFLICT (lookup_type_id, code) DO NOTHING;

-- 4b.2 the twin rules — active iff the base is active (base activation happened in section 4)
INSERT INTO fin_event_type_rule (event_type_rule_pk, event_type_code, name_ar, name_en, is_active_fl, created_by, created_at)
SELECT nextval('seq_fin_event_type_rule'), b.event_type_code || '_REVERSED',
       'عكس ' || b.name_ar, 'Reversal of ' || lower(b.name_en), b.is_active_fl, 'SYSTEM', now()
FROM seed_base_rule b
ON CONFLICT (event_type_code) DO NOTHING;

UPDATE fin_event_type_rule t
   SET is_active_fl = b.is_active_fl, updated_by = 'SYSTEM', updated_at = now()
  FROM seed_base_rule b
 WHERE t.event_type_code = b.event_type_code || '_REVERSED' AND t.is_active_fl <> b.is_active_fl;

-- 4b.3 the twin lines — direction swapped, everything else copied; only into a twin with no lines
INSERT INTO fin_rule_line (rule_line_pk, event_type_rule_id, line_no, account_derivation_type_code,
                           account_derivation_value, account_business_field_code,
                           amount_source_type_code, amount_source_value,
                           direction_code, distribution_type_code, is_remainder_fl, created_at)
SELECT nextval('seq_fin_rule_line'), tw.event_type_rule_pk, l.line_no, l.account_derivation_type_code,
       l.account_derivation_value, l.account_business_field_code,
       l.amount_source_type_code, l.amount_source_value,
       CASE l.direction_code WHEN 'DEBIT' THEN 'CREDIT' WHEN 'CREDIT' THEN 'DEBIT' END,
       l.distribution_type_code, l.is_remainder_fl, now()
FROM seed_base_rule b
JOIN fin_rule_line l ON l.event_type_rule_id = b.event_type_rule_pk
JOIN fin_event_type_rule tw ON tw.event_type_code = b.event_type_code || '_REVERSED'
WHERE NOT EXISTS (SELECT 1 FROM fin_rule_line x WHERE x.event_type_rule_id = tw.event_type_rule_pk)
ORDER BY b.event_type_code, l.line_no;

-- 4b.4 the twin mappings — a copy of every ACTIVE base mapping under the twin's event type
INSERT INTO fin_account_mapping (account_mapping_pk, event_type_code, business_field_code, business_value,
                                 account_id, is_active_fl, created_by, created_at)
SELECT nextval('seq_fin_account_mapping'), b.event_type_code || '_REVERSED', m.business_field_code, m.business_value,
       m.account_id, TRUE, 'SYSTEM', now()
FROM seed_base_rule b
JOIN fin_account_mapping m ON m.event_type_code = b.event_type_code AND m.is_active_fl
WHERE NOT EXISTS (SELECT 1 FROM fin_account_mapping x
                   WHERE x.event_type_code = b.event_type_code || '_REVERSED' AND x.business_field_code = m.business_field_code
                     AND x.business_value = m.business_value AND x.is_active_fl)
ORDER BY 2, 3, 4;

-- 4b.5 the twin tags — the base line's tags, matched by line_no
INSERT INTO fin_rule_line_dim (rule_line_dimension_pk, value_source_code, business_field_code,
                               rule_line_id, dimension_id, dimension_value_id, created_at)
SELECT nextval('seq_fin_rule_line_dim'), d.value_source_code, d.business_field_code,
       tl.rule_line_pk, d.dimension_id, d.dimension_value_id, now()
FROM seed_base_rule b
JOIN fin_rule_line bl ON bl.event_type_rule_id = b.event_type_rule_pk
JOIN fin_rule_line_dim d ON d.rule_line_id = bl.rule_line_pk
JOIN fin_event_type_rule tw ON tw.event_type_code = b.event_type_code || '_REVERSED'
JOIN fin_rule_line tl ON tl.event_type_rule_id = tw.event_type_rule_pk AND tl.line_no = bl.line_no
ON CONFLICT (rule_line_id, dimension_id) DO NOTHING;

-- ----------------------------------------------------------------------------
-- 5. VERIFICATION — RAISE on any failure; the transaction rolls back
-- ----------------------------------------------------------------------------
DO $$
DECLARE v_list TEXT; v_count INT;
BEGIN
  -- 5.1 RULE-FIN-019: every MAPPING line has a business field and no value; every CONSTANT the reverse
  -- Every check below is scoped to what this seed and its v1 companion wrote (created_by = SYSTEM):
  -- configuration added through the API by tests or users is not this file's to judge.
  SELECT string_agg(r.event_type_code || '#' || l.line_no, ', ') INTO v_list
    FROM fin_rule_line l JOIN fin_event_type_rule r ON r.event_type_rule_pk = l.event_type_rule_id
   WHERE r.created_by = 'SYSTEM'
     AND (l.account_derivation_type_code = 'MAPPING'  AND (l.account_business_field_code IS NULL OR l.account_derivation_value IS NOT NULL))
      OR (l.account_derivation_type_code = 'CONSTANT' AND (l.account_business_field_code IS NOT NULL OR l.account_derivation_value IS NULL));
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'fin-seed-v2 5.1: rule lines violating RULE-FIN-019: %', v_list; END IF;
  -- 5.2 no DIRECT anywhere (RULE-FIN-018)
  SELECT count(*) INTO v_count FROM fin_rule_line WHERE account_derivation_type_code = 'DIRECT';
  IF v_count > 0 THEN RAISE EXCEPTION 'fin-seed-v2 5.2: % DIRECT rule lines', v_count; END IF;
  -- 5.3 RULE-FIN-020 coverage: every ACTIVE MAPPING(PAYMENT_METHOD) line has CASH and CHEQUE mappings for its event type
  SELECT string_agg(r.event_type_code || '/' || v, ', ') INTO v_list
    FROM fin_rule_line l JOIN fin_event_type_rule r ON r.event_type_rule_pk = l.event_type_rule_id
    CROSS JOIN unnest(ARRAY['CASH','CHEQUE']) AS v
   WHERE r.created_by = 'SYSTEM' AND r.is_active_fl AND l.account_derivation_type_code = 'MAPPING' AND l.account_business_field_code = 'PAYMENT_METHOD'
     AND NOT EXISTS (SELECT 1 FROM fin_account_mapping m WHERE m.event_type_code = r.event_type_code
                       AND m.business_field_code = 'PAYMENT_METHOD' AND m.business_value = v AND m.is_active_fl);
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'fin-seed-v2 5.3: PAYMENT_METHOD mappings missing: %', v_list; END IF;
  -- 5.4 every ACTIVE MAPPING(EXPENSE_TYPE_CODE) line has at least one mapping for its event type
  SELECT string_agg(DISTINCT r.event_type_code, ', ') INTO v_list
    FROM fin_rule_line l JOIN fin_event_type_rule r ON r.event_type_rule_pk = l.event_type_rule_id
   WHERE r.created_by = 'SYSTEM' AND r.is_active_fl AND l.account_derivation_type_code = 'MAPPING' AND l.account_business_field_code = 'EXPENSE_TYPE_CODE'
     AND NOT EXISTS (SELECT 1 FROM fin_account_mapping m WHERE m.event_type_code = r.event_type_code
                       AND m.business_field_code = 'EXPENSE_TYPE_CODE' AND m.is_active_fl);
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'fin-seed-v2 5.4: EXPENSE_TYPE_CODE mappings missing for: %', v_list; END IF;
  -- 5.5 RULE-FIN-023: every active mapping targets an active leaf
  SELECT string_agg(m.event_type_code || '/' || m.business_field_code || '/' || m.business_value || '→' || a.code, ', ') INTO v_list
    FROM fin_account_mapping m JOIN fin_account a ON a.account_pk = m.account_id
   WHERE m.created_by = 'SYSTEM' AND m.is_active_fl AND (NOT a.is_leaf_fl OR NOT a.is_active_fl);
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'fin-seed-v2 5.5: mappings to non-postable accounts: %', v_list; END IF;
  -- 5.6 RULE-FIN-024: PAYMENT_METHOD business values are active PAYMENT_METHOD codes
  SELECT string_agg(DISTINCT m.business_value, ', ') INTO v_list FROM fin_account_mapping m
   WHERE m.created_by = 'SYSTEM' AND m.business_field_code = 'PAYMENT_METHOD' AND NOT EXISTS (SELECT 1 FROM mdl_lookup_value v JOIN mdl_lookup_type t ON t.lookup_type_pk = v.lookup_type_id
                                                                  WHERE t.key = 'PAYMENT_METHOD' AND v.code = m.business_value AND v.is_active_fl);
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'fin-seed-v2 5.6: undefined PAYMENT_METHOD values: %', v_list; END IF;
  -- 5.7 RULE-FIN-022: no duplicate active key
  SELECT string_agg(k, ', ') INTO v_list FROM (SELECT event_type_code || '/' || business_field_code || '/' || business_value AS k
    FROM fin_account_mapping WHERE created_by = 'SYSTEM' AND is_active_fl GROUP BY 1 HAVING count(*) > 1) d;
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'fin-seed-v2 5.7: duplicate active mappings: %', v_list; END IF;
  -- 5.8 every seeded rule line carries exactly two tags (ORG, BRANCH), RULE-FIN-025/026 shape
  SELECT string_agg(r.event_type_code || '#' || l.line_no, ', ') INTO v_list
    FROM fin_rule_line l JOIN fin_event_type_rule r ON r.event_type_rule_pk = l.event_type_rule_id
   WHERE r.created_by = 'SYSTEM'
     AND r.event_type_code IN (SELECT DISTINCT event_type_code FROM seed_mapping
                               UNION SELECT unnest(ARRAY['INVESTOR_SALES_INVOICE','CONTRACT_CREATED','CONTRACT_SETTLEMENT','COMPLAINT_SETTLEMENT','SAFE_TRANSFER_TO_SAFE','SAFE_TRANSFER_TO_BANK']))
     AND (SELECT count(*) FROM fin_rule_line_dim t WHERE t.rule_line_id = l.rule_line_pk) <> 2;
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'fin-seed-v2 5.8: rule lines without both ORG/BRANCH tags: %', v_list; END IF;
  SELECT count(*) INTO v_count FROM fin_rule_line_dim
   WHERE (value_source_code = 'BUSINESS_FIELD' AND (business_field_code IS NULL OR dimension_value_id IS NOT NULL))
      OR (value_source_code = 'CONSTANT' AND (business_field_code IS NOT NULL OR dimension_value_id IS NULL));
  IF v_count > 0 THEN RAISE EXCEPTION 'fin-seed-v2 5.8: % tags violate RULE-FIN-026', v_count; END IF;
  -- 5.9 all 21 seeded rules active; no journal entry created
  SELECT string_agg(event_type_code, ', ') INTO v_list FROM fin_event_type_rule WHERE NOT is_active_fl
     AND event_type_code IN (SELECT DISTINCT event_type_code FROM seed_mapping);
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'fin-seed-v2 5.9: rules still inactive: %', v_list; END IF;
  IF EXISTS (SELECT 1 FROM fin_journal_entry WHERE created_by = 'SYSTEM' AND created_at >= now() - INTERVAL '1 minute') THEN
    RAISE EXCEPTION 'fin-seed-v2 5.9: a journal entry was created by this run';
  END IF;
  -- 5.10 D-16: every base rule has a twin; the twin mirrors it line for line (count, derivation,
  --      amount field, opposite direction), carries the same tags and the same mapping keys, and
  --      shares its active flag
  SELECT string_agg(b.event_type_code, ', ') INTO v_list FROM seed_base_rule b
   WHERE NOT EXISTS (SELECT 1 FROM fin_event_type_rule t WHERE t.event_type_code = b.event_type_code || '_REVERSED' AND t.is_active_fl = b.is_active_fl);
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'fin-seed-v2 5.10: twin missing or active flag differs: %', v_list; END IF;
  SELECT string_agg(DISTINCT b.event_type_code, ', ') INTO v_list
    FROM seed_base_rule b
    JOIN fin_rule_line bl ON bl.event_type_rule_id = b.event_type_rule_pk
    JOIN fin_event_type_rule tw ON tw.event_type_code = b.event_type_code || '_REVERSED'
    LEFT JOIN fin_rule_line tl ON tl.event_type_rule_id = tw.event_type_rule_pk AND tl.line_no = bl.line_no
   WHERE tl.rule_line_pk IS NULL
      OR tl.account_derivation_type_code <> bl.account_derivation_type_code
      OR tl.account_derivation_value IS DISTINCT FROM bl.account_derivation_value
      OR tl.account_business_field_code IS DISTINCT FROM bl.account_business_field_code
      OR tl.amount_source_value IS DISTINCT FROM bl.amount_source_value
      OR tl.direction_code = bl.direction_code
      OR (SELECT count(*) FROM fin_rule_line_dim x WHERE x.rule_line_id = tl.rule_line_pk)
         <> (SELECT count(*) FROM fin_rule_line_dim x WHERE x.rule_line_id = bl.rule_line_pk);
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'fin-seed-v2 5.10: twin lines do not mirror the base: %', v_list; END IF;
  SELECT string_agg(DISTINCT b.event_type_code, ', ') INTO v_list
    FROM seed_base_rule b
    JOIN fin_account_mapping m ON m.event_type_code = b.event_type_code AND m.is_active_fl
   WHERE NOT EXISTS (SELECT 1 FROM fin_account_mapping x WHERE x.event_type_code = b.event_type_code || '_REVERSED'
                       AND x.business_field_code = m.business_field_code AND x.business_value = m.business_value
                       AND x.account_id = m.account_id AND x.is_active_fl);
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'fin-seed-v2 5.10: twin mappings missing for: %', v_list; END IF;
  RAISE NOTICE 'fin-seed-v2: verification passed — mappings %, rule-line tags %, active rules %',
    (SELECT count(*) FROM fin_account_mapping WHERE is_active_fl),
    (SELECT count(*) FROM fin_rule_line_dim),
    (SELECT count(*) FROM fin_event_type_rule WHERE is_active_fl);
END $$;

