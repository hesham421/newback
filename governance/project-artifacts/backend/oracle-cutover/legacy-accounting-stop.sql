-- ============================================================================
-- legacy-accounting-stop.sql — stop the legacy in-trigger accounting (Oracle, schema LOAN_SYS)
-- ============================================================================
-- Prepared 2026-09-23 against LOAN_SYS@localhost:1521/FREEPDB1 (Oracle AI Database 26ai Free
-- 23.26.2.0.0). Companion: seed-and-cutover-findings.md §5 (per-trigger evidence) and
-- fin-seed-production.sql (the PostgreSQL side that must be in place BEFORE this runs).
--
-- WHAT THIS DOES
--   ALTER TRIGGER ... DISABLE on the triggers whose ONLY job is writing the legacy journal
--   (DAILY_RESTRICTIONS_MASTER / DAILY_RESTRICTIONS_DT), plus two scheduler jobs: one from the
--   abandoned May-2025 accounting framework, one nightly batch that writes the same journal. Nothing is dropped, no table is altered, no row
--   is changed. Every statement has its exact inverse in the ROLLBACK COMPANION at the bottom.
--
-- WHAT THIS DOES NOT DO
--   It does not touch any trigger that carries business logic (ARREARS, ITEMS_DT, EXPENSE_TYPE_DT
--   feeding, org/branch stamping, SAFE row creation, period state) — see the KEEP list. It does
--   not touch the three journal-internal triggers (they only fire when a journal row is written,
--   which stops). It does not touch the two SAFE/BANK_ACCOUNT balance triggers, which are
--   already DISABLED and must stay that way.
--
-- MIXED TRIGGERS
--   None. Every accounting trigger in this schema is a dedicated *_Daliy_TR (sic) trigger; the
--   business logic lives in separately named sibling triggers on the same tables. No trigger
--   needed splitting. (Findings §5.1.)
--
-- PRECONDITIONS — confirm before running
--   1. fin-seed-production.sql committed on PostgreSQL and the FIN event rules that replace each
--      branch below are ACTIVE — otherwise the business keeps running and nothing records it.
--   2. Run as LOAN_SYS (owner of every object below). LOAN_SYS holds DBA.
--   3. Note the exact timestamp of the run: the verification query at the end uses it.
--
-- HOW TO RUN
--   sqlplus LOAN_SYS/...@//host:1521/FREEPDB1 @legacy-accounting-stop.sql
--   Each ALTER TRIGGER is DDL and auto-commits; there is no single transaction to roll back —
--   the ROLLBACK COMPANION is the undo. Run it top to bottom; the order matters (see notes).
-- ============================================================================

SET ECHO ON
SET SERVEROUTPUT ON
WHENEVER SQLERROR EXIT FAILURE

-- Record the stop instant for the verification query (read-only; copy the printed value).
SELECT TO_CHAR(SYSTIMESTAMP, 'YYYY-MM-DD HH24:MI:SS.FF3 TZR') AS stop_marker FROM dual;

-- ----------------------------------------------------------------------------
-- STOP LIST — disable order: least-used branch first, LOAN_PAYMENT last, journal totals after.
-- Each line: trigger | table | what it writes | 2026 volume | replacing FIN event type(s)
-- ----------------------------------------------------------------------------

-- S01  TRIGGER_EXPENSE_TYPE_DT_OLD | EXPENSE_TYPE_DT | journal for expense/revenue/custody rows
--      using EXPENSE_TYPE.ACCOUNT_CHART_FK + screens 30/35 | 2026: 300 entries
--      → EXPENSE_PAID, REVENUE_RECEIVED, CUSTODY_PAID_OUT, CUSTODY_RECEIVED (all need mapping store)
--      Dependency: TRIGGER_COMPLAINT_DT_EXP_DT (KEEP) inserts EXPENSE_TYPE_DT rows from
--      COMPLAINT_DT; after S01 those rows simply get no journal — expected.
ALTER TRIGGER "TRIGGER_EXPENSE_TYPE_DT_OLD" DISABLE;
--      Its predecessor is already disabled and stays so (no statement):
--      TRIGGER_EXPENSE_TYPE_DT — DISABLED since before this work.

-- S02  TRIGGER_TRANSFER_SAFE_Daliy_TR | TRANSFER_SAFE | journal 24↔30 hardcoded | 2026: 26
--      → SAFE_TRANSFER_TO_SAFE / SAFE_TRANSFER_TO_BANK (CONSTANT, active)
ALTER TRIGGER "TRIGGER_TRANSFER_SAFE_Daliy_TR" DISABLE;

-- S03  TRIGGER_ASSETS_Daliy_TR | ASSETS | journal via screen 23 (complaint-settlement accounts —
--      suspected defect D-03) | ASSETS has 0 rows | → no FIN rule (D-03: decide before any asset is entered)
ALTER TRIGGER "TRIGGER_ASSETS_Daliy_TR" DISABLE;

-- S04  TRIGGER_DEPRECIABLE_Daliy_TR | DEPRECIABLE | as S03 | 0 rows | → no FIN rule (D-03)
ALTER TRIGGER "TRIGGER_DEPRECIABLE_Daliy_TR" DISABLE;

-- S05  TRIGGER_INVOICE_IMPORT_Daliy_TR | INVOICE_IMPORT | 3-line journal (323 / 85 / 24|30) | 2026: 50
--      → PURCHASE_INVOICE (needs mapping store)
--      KEEP untouched on the same screen: TRIGGER_INVOICE_IMPORT (INVOICE_IMPORT_DT → ITEMS_DT stock).
ALTER TRIGGER "TRIGGER_INVOICE_IMPORT_Daliy_TR" DISABLE;

-- S06  TRIGGER_COMPLAINT_DT_Daliy_TR | COMPLAINT_DT | journal for FL 43/44 | 2026: 528 + 138
--      → COURT_INSTALLMENT_PAYMENT, LAWYER_FEE_PAYMENT (need mapping store)
--      KEEP untouched on the same table: TRIGGER_COMPLAINT_DT_EXP_DT (business: mirrors the row
--      into EXPENSE_TYPE_DT); TRIGGER_COMPLAINT_DT (SAFE/BANK balances) is already DISABLED.
ALTER TRIGGER "TRIGGER_COMPLAINT_DT_Daliy_TR" DISABLE;

-- S07  TRIGGER_COMPLAINT_Daliy_TR | COMPLAINTS | journal for complaint settlement (FL=1) | 2026: 93
--      → COMPLAINT_SETTLEMENT (CONSTANT, active)
--      KEEP untouched on the same table: TRIGGER_COMPLAINT (business — contract/complaint state).
ALTER TRIGGER "TRIGGER_COMPLAINT_Daliy_TR" DISABLE;

-- S08  TRIGGER_CONTRACT_Daliy_TR | CONTRACT | journal for FL 12/13, and DELETES the settlement
--      journal on a 13→12 revert | 2026: 225 + 7 + 21
--      → CONTRACT_CREATED, CONTRACT_SETTLEMENT (CONSTANT, active); the 13→12 delete becomes a
--      FIN reversal (RULE-FIN-011) emitted by the replay/consumer, not a delete.
--      KEEP untouched on the same table: TRIGGER_CONTRACT_ARREAR and TRIGGER_ARREARS_CONTRACT_FL
--      (ARREARS), TRIGGER_CONTRACT_INSERTDT (already DISABLED). TRIGGER_CONTRACT lives on
--      CONTRACT_DT and maintains ITEMS_DT — untouched.
ALTER TRIGGER "TRIGGER_CONTRACT_Daliy_TR" DISABLE;

-- S09  TRIGGER_LOAN_PAYMENT_Daliy_TR | LOAN_PAYMENT | journal for FL 7/8/32/49/55/56/57/58/59/66/130
--      | 2026: 3,439 entries — the highest-volume trigger, disabled last
--      → INSTALLMENT_PAYMENT_RECEIVED, INVESTOR_*, LAWYER_ADVANCE, *_FEE, INVESTOR_SALES_INVOICE
--      KEEP untouched on the same table: TRIGGER_LOAN_PAYMENT_ORG_SUB and
--      COMPOUND_TRIGGER_LOAN_PAYMENT_ORG_SUB (org/branch derivation); TRIGGER_LOAN_PAYMENT
--      (SAFE/BANK balances) and TRIGGER_LOAN_PAYMENT_Daliy_PER_TR are already DISABLED.
ALTER TRIGGER "TRIGGER_LOAN_PAYMENT_Daliy_TR" DISABLE;

-- S10  TRIGGER_LOAN_PAYMENT_Daliy_TR_AFTER | LOAN_PAYMENT (AFTER INSERT) | updates
--      DAILY_RESTRICTIONS_MASTER.SUM_DEBIT/SUM_CREDIT for the entry S09 just created.
--      Must follow S09: with S09 off it would look up a RESTRICTIONS_MASTER_FK that is NULL.
ALTER TRIGGER "TRIGGER_LOAN_PAYMENT_Daliy_TR_AFTER" DISABLE;

-- S11  JOB_PROCESS_ACCOUNTING_QUEUE | scheduler job, FREQ=MINUTELY;INTERVAL=5, ENABLED, last run
--      2026-09-23 | runs PROCESS_ACCOUNTING_QUEUE over ACCOUNTING_QUEUE — the abandoned May-2025
--      in-database accounting framework (1 rule configured, 4 queue rows). Accounting, so in scope.
--      Disabled, not dropped; its procedures and tables stay.
BEGIN
  DBMS_SCHEDULER.DISABLE(name => 'LOAN_SYS.JOB_PROCESS_ACCOUNTING_QUEUE', force => TRUE);
END;
/

-- S12  LOAN_PAYMENT_DAILY_TR | scheduler job, FREQ=DAILY;BYTIME=210000, ENABLED, 0 runs ever, next run
--      TONIGHT 21:00 | procedure LOAN_SYS.LOAN_PAYMENT_DALIY_TR — a 1,851-line batch copy of
--      TRIGGER_LOAN_PAYMENT_Daliy_TR that reads LOAN_PAYMENT_PRE_TR (619 rows, 35 pending, last
--      status_date 2024-12-28) and INSERTs into DAILY_RESTRICTIONS_MASTER (9 sites) / _DT (18 sites).
--      Accounting, so in scope (closes OQ-S1). Disabled, not dropped.
BEGIN
  DBMS_SCHEDULER.DISABLE(name => 'LOAN_SYS.LOAN_PAYMENT_DAILY_TR', force => TRUE);
END;
/

-- ----------------------------------------------------------------------------
-- NOT IN THE STOP LIST — and why (no statements below, this is the record)
-- ----------------------------------------------------------------------------
-- KEEP (business logic, must survive):
--   TRIGGER_CONTRACT_ARREAR            CONTRACT      → ARREARS
--   TRIGGER_ARREARS_CONTRACT_FL        CONTRACT      → ARREARS (statement level)
--   TRIGGER_CONTRACT                   CONTRACT_DT   → ITEMS_DT
--   TRIGGER_INVOICE_IMPORT             INVOICE_IMPORT_DT → ITEMS_DT
--   TRIGGER_ITEMS_DT                   ITEMS_DT      → stock
--   TRIGGER_COMPLAINT                  COMPLAINTS    → contract / complaint state
--   TRIGGER_COMPLAINT_DT_EXP_DT        COMPLAINT_DT  → EXPENSE_TYPE_DT rows
--   TRIGGER_LOAN_PAYMENT_ORG_SUB, COMPOUND_TRIGGER_LOAN_PAYMENT_ORG_SUB  LOAN_PAYMENT → org/branch
--   TRIGGER_ORGANIZATION_SUB           ORGANIZATION_SUB → SAFE row per branch
--   TRIGGER_YEAR_PERIOD                YEAR_PERIOD   → period state
-- LEAVE AS IS (journal-internal; fire only when a journal row is written, which now stops):
--   TRIGGER_DAILY_RESTRICTIONS_DT, TRIGGER_DAILY_RESTRICTIONS_DT_BEFOR, TRIGGER_DAILY_RESTRICTIONS_MASTER
--   — manual journal entry through the legacy screen still works during the parallel window.
-- ALREADY DISABLED (do not enable):
--   TRIGGER_LOAN_PAYMENT, TRIGGER_COMPLAINT_DT (SAFE.SAFE_BALANCE / BANK_ACCOUNT.DEBIT,CREDIT writers —
--   stored balances are stale and being retired), TRIGGER_EXPENSE_TYPE_DT, TRIGGER_CONTRACT_INSERTDT,
--   TRIGGER_LOAN_PAYMENT_Daliy_PER_TR.

-- ----------------------------------------------------------------------------
-- VERIFICATION — run immediately, then again after a business day
-- ----------------------------------------------------------------------------
-- V1. The stop list is disabled, the keep list is not.
SELECT trigger_name, table_name, status
  FROM user_triggers
 WHERE trigger_name IN ('TRIGGER_EXPENSE_TYPE_DT_OLD','TRIGGER_TRANSFER_SAFE_Daliy_TR','TRIGGER_ASSETS_Daliy_TR',
                        'TRIGGER_DEPRECIABLE_Daliy_TR','TRIGGER_INVOICE_IMPORT_Daliy_TR','TRIGGER_COMPLAINT_DT_Daliy_TR',
                        'TRIGGER_COMPLAINT_Daliy_TR','TRIGGER_CONTRACT_Daliy_TR','TRIGGER_LOAN_PAYMENT_Daliy_TR',
                        'TRIGGER_LOAN_PAYMENT_Daliy_TR_AFTER')
 ORDER BY trigger_name;
-- expected: 10 rows, every STATUS = DISABLED
SELECT COUNT(*) AS keep_list_still_enabled
  FROM user_triggers
 WHERE trigger_name IN ('TRIGGER_CONTRACT_ARREAR','TRIGGER_ARREARS_CONTRACT_FL','TRIGGER_CONTRACT','TRIGGER_INVOICE_IMPORT',
                        'TRIGGER_ITEMS_DT','TRIGGER_COMPLAINT','TRIGGER_COMPLAINT_DT_EXP_DT','TRIGGER_LOAN_PAYMENT_ORG_SUB',
                        'COMPOUND_TRIGGER_LOAN_PAYMENT_ORG_SUB','TRIGGER_ORGANIZATION_SUB','TRIGGER_YEAR_PERIOD')
   AND status = 'ENABLED';
-- expected: 11
SELECT job_name, enabled FROM user_scheduler_jobs WHERE job_name IN ('JOB_PROCESS_ACCOUNTING_QUEUE','LOAN_PAYMENT_DAILY_TR');
-- expected: 2 rows, both FALSE

-- V2. No automatic journal row is written after the stop. Replace :stop_marker with the value
--     printed at the top of the run. Real entries have SCREEN_FK IS NULL; a manual entry keyed
--     through the legacy journal screen would appear here too — check DAILY_RESTRICTIONS_MASTER_NOTE
--     (automatic entries all begin with 'قيد الى' / 'قيد آلي').
SELECT COUNT(*) AS auto_journal_rows_after_stop
  FROM DAILY_RESTRICTIONS_MASTER
 WHERE SCREEN_FK IS NULL
   AND CREATED_DATE > TO_TIMESTAMP_TZ(:stop_marker, 'YYYY-MM-DD HH24:MI:SS.FF3 TZR')
   AND (DAILY_RESTRICTIONS_MASTER_NOTE LIKE 'قيد الى%' OR DAILY_RESTRICTIONS_MASTER_NOTE LIKE 'قيد آل%');
-- expected: 0
-- V3. Business rows keep flowing but no longer link to a journal (proves business path is alive).
SELECT COUNT(*) AS business_rows_after_stop,
       SUM(CASE WHEN RESTRICTIONS_MASTER_FK IS NULL THEN 1 ELSE 0 END) AS without_journal
  FROM LOAN_PAYMENT
 WHERE CREATED_DATE > TO_TIMESTAMP_TZ(:stop_marker, 'YYYY-MM-DD HH24:MI:SS.FF3 TZR');
-- expected after a business day: business_rows_after_stop > 0 AND without_journal = business_rows_after_stop

-- ----------------------------------------------------------------------------
-- WHAT LEGACY USERS WILL SEE CHANGE (findings §5.3)
-- ----------------------------------------------------------------------------
--   * Saving a payment, contract, complaint settlement, purchase invoice, expense or safe transfer
--     no longer creates a row in the daily-journal screen; RESTRICTIONS_MASTER_FK stays NULL on the
--     new business row, so any screen/report that follows that link shows nothing for new rows.
--   * Reverting a contract settlement (13→12) no longer deletes the earlier settlement journal.
--   * SUM_DEBIT / SUM_CREDIT on DAILY_RESTRICTIONS_MASTER stop being maintained for new rows (none
--     are created).
--   * Legacy reports built on DAILY_RESTRICTIONS_* (trial balance, ledger) freeze at the stop
--     instant; FIN is the ledger from then on.
--   * Nothing changes for instalments (ARREARS), stock (ITEMS_DT), complaint→expense mirroring,
--     org/branch stamping or user-facing screens other than the journal.

-- ============================================================================
-- ROLLBACK COMPANION — exact inverse, reverse order. Run all of it or none of it.
-- ============================================================================
-- BEGIN
--   DBMS_SCHEDULER.ENABLE(name => 'LOAN_SYS.LOAN_PAYMENT_DAILY_TR');
--   DBMS_SCHEDULER.ENABLE(name => 'LOAN_SYS.JOB_PROCESS_ACCOUNTING_QUEUE');
-- END;
-- /
-- ALTER TRIGGER "TRIGGER_LOAN_PAYMENT_Daliy_TR"        ENABLE;   -- S09 before S10, so the AFTER
-- ALTER TRIGGER "TRIGGER_LOAN_PAYMENT_Daliy_TR_AFTER"  ENABLE;   -- trigger never sees a NULL link
-- ALTER TRIGGER "TRIGGER_CONTRACT_Daliy_TR"            ENABLE;
-- ALTER TRIGGER "TRIGGER_COMPLAINT_Daliy_TR"           ENABLE;
-- ALTER TRIGGER "TRIGGER_COMPLAINT_DT_Daliy_TR"        ENABLE;
-- ALTER TRIGGER "TRIGGER_INVOICE_IMPORT_Daliy_TR"      ENABLE;
-- ALTER TRIGGER "TRIGGER_DEPRECIABLE_Daliy_TR"         ENABLE;
-- ALTER TRIGGER "TRIGGER_ASSETS_Daliy_TR"              ENABLE;
-- ALTER TRIGGER "TRIGGER_TRANSFER_SAFE_Daliy_TR"       ENABLE;
-- ALTER TRIGGER "TRIGGER_EXPENSE_TYPE_DT_OLD"          ENABLE;
-- Rollback caveat: business rows saved while the triggers were off have RESTRICTIONS_MASTER_FK = NULL
-- and will NOT get a legacy journal retroactively — re-enabling only affects rows saved afterwards.
-- Those rows are exactly the ones FIN recorded; if FIN is also being rolled back they need a
-- manual legacy journal or a replay of the trigger logic. That is why the stop should not run
-- before the FIN rules are active and verified.
