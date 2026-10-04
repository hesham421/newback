-- ============================================================
-- V23 — FIN_ACCOUNT gains the Retained Earnings marker (DBF-FIN-147)
-- Source: governance/modules/FIN/P2/db-script-fin.md §1 Table FIN_ACCOUNT / §3 BLOCK 2 + BLOCK 6
-- Forward-only fix on top of V22 (V22 is applied and checksummed — never edited).
--
-- WHY: API-FIN-027 (year-end close, REQ-FIN-036 / POL-FIN-010) must close every result
-- account into Retained Earnings. Until now FIN_ACCOUNT carried no marker for that account
-- and ACCOUNT_TYPE only reaches EQUITY, which many accounts share, so the account could not
-- be derived and was being supplied by the caller — an unaudited ledger decision. The system
-- now derives it from this column instead.
--
-- SINGLE-ACCOUNT GUARANTEE: a PARTIAL unique index, not a plain UNIQUE constraint. A plain
-- UNIQUE (is_retained_earnings_fl) would also allow only ONE account with FALSE, i.e. exactly
-- one non-retained-earnings account — absurd for a chart of accounts. The partial index below
-- indexes only the rows where the flag is TRUE, so the uniqueness of that constant value means
-- "at most one marked account", while any number of rows may carry FALSE. Postgres has no
-- table-level equivalent (a UNIQUE *constraint* cannot carry a WHERE clause), which is why
-- this is CREATE UNIQUE INDEX rather than ALTER TABLE ... ADD CONSTRAINT.
-- ============================================================

ALTER TABLE FIN_ACCOUNT
  ADD COLUMN is_retained_earnings_fl BOOLEAN NOT NULL DEFAULT FALSE;

COMMENT ON COLUMN FIN_ACCOUNT.is_retained_earnings_fl IS
  'DBF-FIN-147 — REQ-FIN-036 / POL-FIN-010: marks the single Retained Earnings account the '
  'year-end closing entry posts the year''s result to; at most one row may be TRUE '
  '(UQ_FIN_ACCOUNT_RETAINED_EARNINGS, partial unique index)';

CREATE UNIQUE INDEX UQ_FIN_ACCOUNT_RETAINED_EARNINGS
  ON FIN_ACCOUNT (is_retained_earnings_fl)
  WHERE is_retained_earnings_fl;
