-- ============================================================
-- V41 — FIN — contra accounts take their group's nature (fix to V40)
-- ============================================================
-- V40 gave the contra accounts the OPPOSITE nature of their class (ASSET/CREDIT, REVENUE/DEBIT).
-- In FIN an account's nature is not "which side its balance usually sits on" for display only: it is
-- the SIGN convention the statements use (POL-FIN-002, ReportMapper.signedAgainstNature) — a group
-- total is the sum of its accounts' signed balances. With the opposite nature a contra balance is
-- signed POSITIVE and is ADDED to its group: an ECL allowance of 100 raised total assets by 100
-- instead of lowering them, and 42 of settlement discounts raised revenue by 42 (found 2026-09-26 on
-- the income statement after V40). Giving every contra account its group's nature signs its balance
-- NEGATIVE, so it reduces the group, which is exactly the IFRS presentation (net carrying amount,
-- net revenue). The "(-)" in the names says what it is; the trial balance is unaffected (its columns
-- follow each balance's own side, never the nature).
-- ============================================================

UPDATE fin_account
   SET nature_code = 'DEBIT', updated_by = 'SYSTEM', updated_at = now()
 WHERE account_type_code = 'ASSET' AND nature_code = 'CREDIT'
   AND code IN ('110203', '110204', '1203', '120302', '120303', '120304', '120305', '120402');

UPDATE fin_account
   SET nature_code = 'CREDIT', updated_by = 'SYSTEM', updated_at = now()
 WHERE account_type_code = 'REVENUE' AND nature_code = 'DEBIT'
   AND code IN ('49', '4901');

DO $$
DECLARE v_list TEXT;
BEGIN
  -- every account now carries its class's normal nature
  SELECT string_agg(code, ', ') INTO v_list FROM fin_account
   WHERE (account_type_code IN ('ASSET', 'EXPENSE') AND nature_code <> 'DEBIT')
      OR (account_type_code IN ('LIABILITY', 'EQUITY', 'REVENUE') AND nature_code <> 'CREDIT');
  IF v_list IS NOT NULL THEN RAISE EXCEPTION 'V41: accounts whose nature differs from their class: %', v_list; END IF;
END $$;
