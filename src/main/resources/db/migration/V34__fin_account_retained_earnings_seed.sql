-- ============================================================
-- V34 — FIN_ACCOUNT gets its one Retained Earnings row (DBF-FIN-147)
-- Source: governance/shared/analysis/modules/FIN/P2/db-script-fin.md §1 row DBF-FIN-147 and its
-- closing note — "seeded as data (migration/seed), never through an API".
-- Forward-only on top of V22/V23 (both applied and checksummed — never edited).
--
-- WHY: V23 added the marker column but seeded no row, so on every existing database
-- `select count(*) from fin_account where is_retained_earnings_fl` is 0 and API-FIN-027
-- (year-end close, REQ-FIN-036 / POL-FIN-010) answers FIN-404-ACCOUNT unconditionally —
-- AccountRepository.findFirstByIsRetainedEarningsFlTrueOrderByAccountPkAsc() has nothing to find.
-- There is no API writer for the flag by decision, not by gap (AccountCreateRequest and
-- AccountUpdateRequest exclude it deliberately; a write endpoint would need an unauthorised new
-- API id), so a seed is the only governed remedy.
--
-- ⚠ The guard is "is any row MARKED", never "does this code exist": UQ_FIN_ACCOUNT_RETAINED_EARNINGS
-- is a PARTIAL unique index (V23), so a host that already marked its own account must be left
-- alone while its code is irrelevant, and a taken code must not abort the migration —
-- UQ_FIN_ACCOUNT_CODE would reject the insert. Hence the marked-row early exit plus the
-- free-code search below; nothing already in the chart of accounts is read, updated or re-parented.
-- ============================================================

DO $$
DECLARE
  seed_code TEXT    := '3200';
  suffix    INTEGER := 0;
BEGIN
  IF EXISTS (SELECT 1 FROM FIN_ACCOUNT WHERE is_retained_earnings_fl) THEN
    RAISE NOTICE 'V34: a Retained Earnings account is already marked — nothing seeded.';
    RETURN;
  END IF;

  WHILE EXISTS (SELECT 1 FROM FIN_ACCOUNT WHERE code = seed_code) LOOP
    suffix := suffix + 1;
    seed_code := '3200-' || suffix;
  END LOOP;

  INSERT INTO FIN_ACCOUNT (account_pk, code, name_ar, name_en, account_type_code, nature_code,
                           parent_account_id, is_leaf_fl, is_active_fl, is_retained_earnings_fl,
                           created_by, created_at)
  VALUES (nextval('SEQ_FIN_ACCOUNT'), seed_code, 'الأرباح المحتجزة', 'Retained Earnings',
          'EQUITY', 'CREDIT', NULL, TRUE, TRUE, TRUE, 'SYSTEM', CURRENT_TIMESTAMP);

  RAISE NOTICE 'V34: seeded Retained Earnings account with code %.', seed_code;
END $$;
