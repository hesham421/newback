-- ============================================================
-- V36 — Finance / General Ledger (FIN) v2 — MDL lookup-registry delta seed
-- Source: governance/shared/analysis/modules/FIN/v2/P2/db-script-fin.md §3 BLOCK 8, verbatim
--         in substance; keys and values from srs-fin.md v2 §A6.
-- Target tables: MDL_LOOKUP_TYPE / MDL_LOOKUP_VALUE (owned by MDL, DDL in V18; keys owned by FIN,
--   so FIN seeds them — same mechanism and precedent as V26, which seeded the 13 v1 keys).
-- Column and sequence names verified against V18__mdl_sequences.sql: lookup_type_pk, key,
--   owner_module_code, name_ar, name_en, is_active_fl, created_by, created_at, updated_by,
--   updated_at / lookup_value_pk, lookup_type_id, code, sort_order; SEQ_MDL_LOOKUP_TYPE,
--   SEQ_MDL_LOOKUP_VALUE.
-- Only the v2 changes are seeded: two ADDED types (FIN_EVENT_BUSINESS_FIELD, 4 values;
--   FIN_DIMENSION_VALUE_SOURCE, 2 values), the CHEQUE value under PAYMENT_METHOD, and the soft
--   retirement of ACCOUNT_DERIVATION_TYPE / DIRECT (ADR-FIN-010). The 11 unchanged v1 keys stay
--   as V26 left them.
-- Idempotent: every statement is guarded against MDL's unique keys (UQ_MDL_LOOKUP_TYPE_KEY,
--   UQ_MDL_LOOKUP_VALUE_TYPE_CODE), so it coexists with FIN's API onboarding (REQ-FIN-045).
-- OWNER_MODULE_CODE = 'FIN' is registered in SEC_MODULE_REG by V24, which precedes this file.
-- Flyway wraps this migration in its own transaction — the db-script's trailing COMMIT is
--   intentionally omitted, as in V22 / V26.
-- ============================================================

-- FIN_EVENT_BUSINESS_FIELD — ADDED (ADR-FIN-013; SRS A6)
INSERT INTO MDL_LOOKUP_TYPE (lookup_type_pk, key, owner_module_code, name_ar, name_en, is_active_fl, created_by, created_at)
SELECT nextval('SEQ_MDL_LOOKUP_TYPE'), 'FIN_EVENT_BUSINESS_FIELD', 'FIN', 'الحقل التجاري للحدث', 'Event business field', TRUE, 'SYSTEM', now()
WHERE NOT EXISTS (SELECT 1 FROM MDL_LOOKUP_TYPE WHERE key = 'FIN_EVENT_BUSINESS_FIELD');

INSERT INTO MDL_LOOKUP_VALUE (lookup_value_pk, lookup_type_id, code, name_ar, name_en, sort_order, is_active_fl, created_by, created_at)
SELECT nextval('SEQ_MDL_LOOKUP_VALUE'), t.lookup_type_pk, s.code, s.name_ar, s.name_en, s.sort_order, TRUE, 'SYSTEM', now()
FROM MDL_LOOKUP_TYPE t
CROSS JOIN (VALUES
  ('PAYMENT_METHOD',    'طريقة الدفع',     'Payment method',    1),
  ('EXPENSE_TYPE_CODE', 'رمز نوع المصروف', 'Expense type code', 2),
  ('ORGANISATION_CODE', 'رمز المؤسسة',     'Organisation code', 3),
  ('BRANCH_CODE',       'رمز الفرع',       'Branch code',       4)
) AS s (code, name_ar, name_en, sort_order)
WHERE t.key = 'FIN_EVENT_BUSINESS_FIELD'
  AND NOT EXISTS (SELECT 1 FROM MDL_LOOKUP_VALUE v WHERE v.lookup_type_id = t.lookup_type_pk AND v.code = s.code);

-- FIN_DIMENSION_VALUE_SOURCE — ADDED (ADR-FIN-010; SRS A6)
INSERT INTO MDL_LOOKUP_TYPE (lookup_type_pk, key, owner_module_code, name_ar, name_en, is_active_fl, created_by, created_at)
SELECT nextval('SEQ_MDL_LOOKUP_TYPE'), 'FIN_DIMENSION_VALUE_SOURCE', 'FIN', 'مصدر قيمة البُعد', 'Dimension value source', TRUE, 'SYSTEM', now()
WHERE NOT EXISTS (SELECT 1 FROM MDL_LOOKUP_TYPE WHERE key = 'FIN_DIMENSION_VALUE_SOURCE');

INSERT INTO MDL_LOOKUP_VALUE (lookup_value_pk, lookup_type_id, code, name_ar, name_en, sort_order, is_active_fl, created_by, created_at)
SELECT nextval('SEQ_MDL_LOOKUP_VALUE'), t.lookup_type_pk, s.code, s.name_ar, s.name_en, s.sort_order, TRUE, 'SYSTEM', now()
FROM MDL_LOOKUP_TYPE t
CROSS JOIN (VALUES
  ('CONSTANT',       'ثابت',       'Constant',       1),
  ('BUSINESS_FIELD', 'حقل تجاري', 'Business field', 2)
) AS s (code, name_ar, name_en, sort_order)
WHERE t.key = 'FIN_DIMENSION_VALUE_SOURCE'
  AND NOT EXISTS (SELECT 1 FROM MDL_LOOKUP_VALUE v WHERE v.lookup_type_id = t.lookup_type_pk AND v.code = s.code);

-- PAYMENT_METHOD — MODIFIED: CHEQUE seeded (named by the user; SRS A6). Other methods are host data.
INSERT INTO MDL_LOOKUP_VALUE (lookup_value_pk, lookup_type_id, code, name_ar, name_en, sort_order, is_active_fl, created_by, created_at)
SELECT nextval('SEQ_MDL_LOOKUP_VALUE'), t.lookup_type_pk, 'CHEQUE', 'شيك', 'Cheque', 1, TRUE, 'SYSTEM', now()
FROM MDL_LOOKUP_TYPE t
WHERE t.key = 'PAYMENT_METHOD'
  AND NOT EXISTS (SELECT 1 FROM MDL_LOOKUP_VALUE v WHERE v.lookup_type_id = t.lookup_type_pk AND v.code = 'CHEQUE');

-- ACCOUNT_DERIVATION_TYPE — MODIFIED: DIRECT retired (ADR-FIN-010). Soft retire, per the
-- soft-delete default, so exactly CONSTANT and MAPPING stay active (AC-FIN-045).
-- No FIN_RULE_LINE row cites DIRECT (ADR-FIN-010: v1 never delivered it), so nothing is converted.
UPDATE MDL_LOOKUP_VALUE v
   SET is_active_fl = FALSE, updated_by = 'SYSTEM', updated_at = now()
  FROM MDL_LOOKUP_TYPE t
 WHERE v.lookup_type_id = t.lookup_type_pk
   AND t.key = 'ACCOUNT_DERIVATION_TYPE'
   AND v.code = 'DIRECT'
   AND v.is_active_fl;
