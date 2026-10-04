# HANDOVER — Oracle `LOAN_SYS` accounting cutover to FIN

**As of:** 2026-09-23 evening · **Owner of record:** heshamezzat60@gmail.com · **Prepared by:** Claude (session of 2026-09-23)
**Purpose of this file:** anyone can pick this up on any later day and continue without the original session. Everything below is verifiable from the files in this folder and the databases named.

> **الخلاصة بالعربي:** المحاسبة القديمة داخل تريجرات Oracle **موقوفة فعلاً** منذ 2026-09-23 14:17 UTC على نسخة الاختبار (وهي نسخة الإنتاج). قاعدة FIN على PostgreSQL مهيأة (v1). ملفات إرسال الأحداث من Oracle **مكتوبة ولم تُنفَّذ**. الباقي: بناء FIN v2، تفعيل القواعد، تشغيل ملفات Oracle 1→3، بناء الـ consumer، ثم replay 2026. من لحظة الإيقاف كل عملية تجارية تُحفظ هي دين على الـ replay.

---

## 1. Environments

| | Where | State now |
|---|---|---|
| **Oracle (legacy)** | `LOAN_SYS@localhost:1521/FREEPDB1` — Oracle AI Database 26ai Free 23.26.2.0.0, `compatible=23.6.0`. **Production copy used for test** (user statement 2026-09-23). Credentials `LOAN_SYS`/`LOAN_SYS`, user holds `DBA` | legacy accounting **stopped** (§3); no queue, no emit package, no emit triggers yet |
| **PostgreSQL (FIN)** | `erp_db@localhost:5432` (Docker `erp-backend-postgres`, PG 17); app jar on `:7272`; Flyway at **v34** (FIN v1) | reset + seeded 2026-09-23 (§3); v2 schema **not** built |
| **Governance** | `governance/shared` submodule at `cbc6a0e` — FIN v2 analysis + packages delivered; `backend/modules/FIN/v2/execution-state.json` = every phase **PENDING**, current `CORE` | |
| **MCP** | `.mcp.json` → `oracle`, `postgres` | for read checks; writes in this work were done with node-oracledb / container `psql`. `ORACLE_ALLOW_WRITE` was flipped to `true` on 2026-09-23 by user instruction — a `/mcp` reconnect is required for a running server to pick it up, and a server started before the literal password landed in `.mcp.json` answers every query with `ORA-01017` until reconnected. |

---

## 2. Files in this folder — and the PostgreSQL ones next door

| File | Role | Executed? |
|---|---|---|
| `oracle-aq-setup.sql` | **1** — queue `ACCOUNTING_EVENT_Q` (TxEventQ, JSON, single consumer FIFO, `max_retries 5`, `retry_delay 60`), `ACCOUNTING_EVENT_SEQ`, `ACCOUNTING_EVENT_LOG` (audit + replay source). Idempotent. Classic-AQ fallback commented. Rollback at end | **no** |
| `oracle-event-emit-package.sql` | **2** — `PKG_ACCOUNTING_EVENT.emit(...)`: builds the FIN API-FIN-020 JSON, logs, enqueues `ON_COMMIT` inside the business transaction, never raises | **no** |
| `oracle-event-emit-triggers.sql` | **3** — 7 `TRG_AE_*` triggers (LOAN_PAYMENT, CONTRACT, COMPLAINTS, COMPLAINT_DT, INVOICE_IMPORT, EXPENSE_TYPE_DT, TRANSFER_SAFE). INSERT → event; amount UPDATE → reversal + new; DELETE → reversal | **no** |
| `legacy-accounting-stop.sql` | **4** — disables 10 accounting triggers + 2 scheduler jobs; keep-list untouched; rollback companion inside | **YES — 2026-09-23 14:17:31.635 UTC** |
| `../seed-scripts/fin-seed-production.sql` | PostgreSQL, FIN v1 shape: 325 accounts, ORG/BRANCH, FY 2026 (12 OPEN months), 21 rules (6 active), MDL values. Idempotent | **YES** (twice, second run 0 rows) |
| `../seed-scripts/fin-seed-v2-delta.sql` | PostgreSQL, after v2 migrations: mapping rows (45), rule-line ORG/BRANCH tags, activates the 15 waiting rules | **no** — v2 schema absent |
| `../../seed-and-cutover-findings.md` | the full evidence: findings, decisions D-01..D-15, open questions | — |
| `../../oracle-aq-fin-integration-plan.md` | the architecture (queue → consumer → FIN), event contract, exactly-once, failure handling | — |

---

## 3. What has been done (with evidence)

| When (2026-09-23) | Action | Evidence |
|---|---|---|
| morning | Oracle investigation read-only: 29 triggers, 16 posting templates, 21 live branches, chart 328, org/branch, expense types; the May-2025 abandoned in-DB framework found still running | findings §2 |
| ~13:00 | v1 seed regenerated with accountant decisions (D-01 leaves 54/86/342 instead of parents 32/85/310, D-04, D-06, D-11 …) | findings §6 |
| ~13:05 | PostgreSQL reset: `DROP SCHEMA public CASCADE`, jar restarted, Flyway 31 migrations → v34 | findings §0 |
| ~13:10 | `fin-seed-production.sql` run twice — verification passed both times | findings §0 |
| **14:17:31.635 UTC** | **`legacy-accounting-stop.sql` executed** — 10 triggers DISABLED, `JOB_PROCESS_ACCOUNTING_QUEUE` + `LOAN_PAYMENT_DAILY_TR` DISABLED, keep-list 11/11 ENABLED, 0 automatic journal rows after marker | findings §0.2 |
| ~18:05 | FIN v2 delta discovered (delivered 18:02) → `fin-seed-v2-delta.sql` written; seed keys aligned (`EXPENSE_TYPE_CODE`) | findings §10 |
| ~19:00 | Oracle files 1–3 written; sign orientation corrected (D-14: negative is normal for FL 32/55/56/57/58/59/66 — 4 rules re-oriented in seed + live DB); OQ-14 closed (complaint mirrors are the only accounting for FL 50 → not skipped) | findings §11 |

### 3.1 First real execution of files 1–3 (2026-09-23 evening)

Until this point files 1–3 had only been written and read; nothing had ever been compiled or run.
Running them found four defects that review had not:

| # | What happened | Fix applied to the file |
|---|---|---|
| 1 | `DBMS_AQADM.ALTER_QUEUE` → **ORA-24218**, "feature ALTER_QUEUE not supported for transactional event queues". Because `CREATE_TRANSACTIONAL_EVENT_QUEUE` commits on its own, this left the queue **existing but never started** (enqueue NO / dequeue NO) — and the file's "skip if the queue exists" idempotence would have skipped straight past that forever. | `max_retries => 5` moved into the CREATE call; ALTER_QUEUE removed; the ELSE branch now *reasserts* shard count and start state instead of skipping. |
| 2 | `retry_delay` is **not settable at all** on a TxEventQ. `GET_QUEUE_PARAMETER` accepts only `SHARD_NUM`, `KEY_BASED_ENQUEUE`, `STICKY_DEQUEUE`; `RETRY_DELAY`/`RETRY_INTERVAL`/`MAX_RETRIES` each raise ORA-00904 "Unsupported param". | Documented in the file. The plan's 60 s spacing is now explicitly the **consumer's** responsibility. `retry_delay = 0` in `user_queues` is expected, not a defect. |
| 3 | The queue came up with **`SHARD_NUM = 5`** (the default). A TxEventQ orders messages only *within* a shard, so global FIFO — which plan §3.5 relies on, since an update is a reversal plus a new entry — was **not** actually in force. | `SET_QUEUE_PARAMETER(..., 'SHARD_NUM', 1)` on both the create and the repair path, plus a verification block that prints it. |
| 4 | `PKG_ACCOUNTING_EVENT` body → **PLS-00201: identifier 'DBMS_AQ' must be declared**. LOAN_SYS holds DBA but only as a *role* and has no direct object grants (`user_tab_privs`: no rows). Roles are live in an anonymous block and disabled inside a definer-rights stored unit — which is why every `DBMS_AQADM` call in file 1 worked while the package body would not compile. | New **section 0** in `oracle-aq-setup.sql`: `GRANT EXECUTE ON SYS.DBMS_AQ TO LOAN_SYS`. **NOT YET APPLIED — needs approval** (see below). |

Three more defects surfaced on the second day (2026-09-24), all found only by running:

| # | What happened | Fix applied to the file |
|---|---|---|
| 5 | The §0 self-grant → **ORA-01749** "Cannot GRANT or REVOKE privileges to or from yourself" — DBA role or not, LOAN_SYS cannot grant to LOAN_SYS. | §0 is now a **check** that stops the file with the exact statement to run as SYS (`ALTER SESSION SET CONTAINER = FREEPDB1; GRANT EXECUTE ON SYS.DBMS_AQ TO LOAN_SYS;`). The user issued it via `docker exec -i erp-oracle bash -lc 'sqlplus -S / as sysdba'` (OS authentication in the container). |
| 6 | The repair branch's `START_QUEUE` on an already-started queue → **ORA-24210**. | The branch reads `user_queues.enqueue_enabled / dequeue_enabled` and starts only the side that is stopped. |
| 7 | `emit` returned silently — no log row, no error row. Cause: **ORA-40573** "invalid use of PL/SQL JSON object type": `JSON(v_payload.to_clob())` (and `to_json()`, and `to_clob()`) cannot appear inside a SQL `INSERT`; the same expression is fine as an `ENQUEUE` argument. Both the main insert and `log_only`'s insert failed, and `log_only`'s `WHEN OTHERS THEN ROLLBACK` swallowed the second failure, so nothing was visible anywhere. | The envelope is materialised once into a local `v_json JSON := v_payload.to_json()` and that local is bound in the INSERT and the ENQUEUE (same in `log_only`). `log_only`'s last-resort handler now also writes the error to `DBMS_OUTPUT`, so a double failure is never silent again. |

**State on the test copy right now (2026-09-24 ~16:35 UTC):**

| Object | State |
|---|---|
| `ACCOUNTING_EVENT_SEQ` | created |
| `ACCOUNTING_EVENT_LOG` | created, D-16 columns present, 0 rows |
| `ACCOUNTING_EVENT_Q` | started (enq YES / deq YES), `max_retries 5`, `SHARD_NUM 1` |
| `PKG_ACCOUNTING_EVENT` | spec + body **VALID** |
| `TRG_AE_*` | all 7 **VALID, ENABLED** |

**Proven end-to-end on a real row** (`LOAN_PAYMENT` 39766, FL 7, inside a transaction that was rolled back):
UPDATE → `…_REVERSED` undo of 96 (`UNDO_FL 1`, no `reversesEventReference` because the original predates the emitter → the twin-rule fallback) + new 97; second UPDATE → undo naming `…:5` + new 98; DELETE → undo naming `…:7`. Payload keys exactly `EventEntryBuildRequest`; `fields.PAYMENT_METHOD = CHEQUE` for `PAYMENT_TYPE_FK 1`, `ORGANISATION_CODE / BRANCH_CODE = "1"`. After ROLLBACK: 0 log rows, value back to 96. A committed smoke emit on a synthetic source was then seen in `AQ$ACCOUNTING_EVENT_Q` as READY, dequeued with `DBMS_AQ.DEQUEUE`, and its log row removed.

**Defects 8–11, found while building and testing the consumer (2026-09-24 evening)** — each proven on the test copy, all four applied there:

| # | What happened | What changed |
|---|---|---|
| 8 | **D-19 — an undo could double-book.** `v_flip := p_reverse OR (sign mismatch)`: the undo of an original that was itself mirrored (`X_REVERSED`) came out as `X_REVERSED` again → the same direction posted twice instead of cancelled. Harmless while undo went through `/reverse`; real once undo goes through the twin rules (the user's decision for the consumer). | File 2: XOR. Truth table proven on the package, all 4 cases (normal / contra × original / undo). |
| 9 | **D-20 — past `max_retries` a message vanished into limbo.** No exception queue existed (TxEventQ creates none), and on 23.26.2 one created by `CREATE_EQ_EXCEPTION_QUEUE` receives **only messages whose enqueue named it** — otherwise after 5 deliveries the message sits `RETRYEXPIRED` in the queue table, dequeueable from nowhere (tried: main queue, exception queue, by msgid, by correlation). A rollback counts as a delivery, so "retry later" alone would lose events. | File 1 §3b creates + starts `ACCOUNTING_EVENT_Q_EXCPT` (idempotent). File 2 sets `message_properties.exception_queue` on every message. Proven: 5 rollbacks → the package's message is dequeued from the exception queue. |
| 10 | `CREATE_EQ_EXCEPTION_QUEUE(teq_queue_name => …)` — the published signature — is refused with PLS-00306 on this build. | Parameter names read from `ALL_ARGUMENTS`: `queue_name`, `exception_queue_name`. |
| 12 | **D-21 — no CONTRACT or COMPLAINTS event ever reached the queue.** Found by the general flow test (real triggers on all 7 tables): `emit` looked up the contract / complaint number with a `SELECT` on the very table whose trigger was firing → **ORA-04091 mutating table** → logged `ENQUEUED_FL = 0`, event not sent. Also hit `LOAN_PAYMENT` rows a legacy trigger updates in cascade from `CONTRACT`. Not silent (the log row is the replay hook) and the save was not blocked — but on the live instance `CONTRACT_CREATED`, `CONTRACT_SETTLEMENT` and `COMPLAINT_SETTLEMENT` would never have posted. Earlier tests only exercised `LOAN_PAYMENT` directly. | Files 2 + 3: the two triggers pass their own number (`p_contract_no` / `p_complaint_no`, new optional parameters); the lookups catch ORA-04091 and fall back to the key (the number is informational — never an accounting input). Re-run: 56/56 emissions enqueued. |
| 13 | Applying D-21 once produced **"Package body created with compilation errors"** (a declaration after a subprogram, PLS-00103). sqlplus treats that as a warning, so `WHENEVER SQLERROR` did not stop — and with file 3 installed an INVALID package makes **every save on the 7 tables fail**. Fixed within a minute on the test copy. | File 2 now ends with a guard that raises (stopping the run) if the package is not VALID, naming the emergency action: disable the `TRG_AE_*` triggers (file 3's rollback companion) so users save again at once; the log/replay covers the gap. |
| 11 | Re-running file 2 recompiles the package spec, which leaves the 7 `TRG_AE_*` triggers **INVALID** until their next fire recompiles them — a live table must not depend on that. | File 2 now recompiles any invalid `TRG_AE_*` at its end, and its verification lists them. |

**Files 1–3 are therefore ready for the live instance after approval** — with the SYS grant issued there first. Files 2 and 3 must go together with 1 §3b: a package with D-20 names an exception queue that must exist.

**General flow test (2026-09-24 night, after D-21):** real `UPDATE +1` on one real 2026 row per event type across **all 7 tables** (every mapped FL, every expense category/type used in 2026, cash and cheque), inside a transaction that was **rolled back** — the triggers' payloads were captured, then enqueued under `GEN:` references and consumed. 56 emitted → **54 POSTED**, every entry balanced with its dimensions, every document nets exactly the +1 change (undo = exact mirror); 2 held FAILED `FIN-422-MISSING-BUSINESS-FIELD` = a **2025** transfer row with a NULL branch (type 24 has no 2026 row) — **0 of ~4,800 2026 rows lack a branch**. Trial balance balanced. Details: `fin-consumer/docs/test-report.md` §General.

**The consumer exists and is tested end to end (2026-09-24):** sibling repo `fin-consumer/` (next to `backend/`), README `fin-consumer-README.md`, test report `docs/test-report.md` (11 scenarios: rollback, baseline + legacy reconciliation, replay, duplicate, crash mid-flight, FIN down + order, graceful stop, closed period, missing mapping, token expiry, trial balance — 11 events = 11 entries, balanced), findings `docs/findings.md`. Its own Oracle setup (`FIN_CONSUMER` user, DEQUEUE grants, `LOAN_SYS.FINC_QUEUE_DEPTH`, store table) is in `fin-consumer/oracle/01–03`.

### 3.2 Review after FIN v2 landed (2026-09-24)

| What | Result |
|---|---|
| Backend | Rebuilt (5 FIN sources were newer than the running jar) and restarted: `Started ErpMainApplication in 4.7 s`, Flyway at **V37**, `db: UP`. (`mail: DOWN` is the pre-existing missing SMTP password, unrelated.) |
| v2 schema vs the delta | V35–V37 match `fin-seed-v2-delta.sql`'s preflight name for name. **OQ-13 confirmed:** V35 added `CHK_FIN_RULE_LINE_DERIVATION_SPEC` without the data step; the check *passes* the 19 v1-shape MAPPING lines (one null → TRUE) but `EventEntryService` reads `account_business_field_code` and gets NULL, so the 15 MAPPING rules fail at post until the delta's section 1 runs. The delta's header text claiming the constraint would *reject* them was wrong and is corrected. |
| Dev PostgreSQL state | **Polluted by the v2 test runs** (api-verify + JUnit, `created_by = admin` / `e2e_*`): 149 extra accounts, 96 extra rules, 162 mappings, 27 dimensions, 87 fiscal years, **356 journal entries** (47 of them hit 70 lines on *seeded* accounts, so the production chart already carries test balances), and one test-added line on the seeded `INSTALLMENT_PAYMENT_RECEIVED` rule (line 3, `netAmount` → `0001000100010001`, created 11:42:53 today — it unbalances a production rule). The 326 seeded accounts / 21 rules / 2 dimensions themselves are intact and unmodified. **A reset (`DROP SCHEMA public CASCADE` → boot → `fin-seed-production.sql` → `fin-seed-v2-delta.sql`) is required before any go-live rehearsal**; the classifier refused to let me run the DROP, so it is yours to run. |
| Chart of accounts (seeded rows) | 326 accounts, 6 roots, depth 1–7, 269 leaves, 8 inactive; **0** code-prefix violations, **0** leaf-with-children, **0** duplicate sibling names, **0** active-child-under-inactive-parent; every CONSTANT rule target is an active leaf; 0 test mappings target a non-leaf. Findings → **D-18** (32/54/55 nature fixed; 82, the expenses-filed `مبيعات` subtree, one childless non-leaf and root 3200 flagged for the owner). |
| Oracle ↔ FIN linkage | Payload keys match `EventEntryBuildRequest` (`eventReference`, `eventTypeCode`, `docDate`, `baseAmount`, `amounts{}`, `fields{}`, `descriptionAr/En`); `fields{}` keys match `FIN_EVENT_BUSINESS_FIELD`; mapping lookup is exact-match on (event type, field, value) and dimensions resolve by `FIN_DIMENSION_VALUE.code` — the Oracle PK as text, which is what the seed wrote. `TRANSFER` (retired by a test teardown) is never sent: the package emits only `CASH` / `CHEQUE`. **D-16** (undo → `/reverse`, twins as fallback) and **D-17** (date/org/branch edits re-emit) closed the two gaps the user's I/U/D instruction pointed at. |
| Files changed today | `oracle-event-emit-triggers.sql` (D-17, 7 triggers), `oracle-event-emit-package.sql` (D-16), `oracle-aq-setup.sql` (D-16 columns, converging ALTER), `fin-seed-v2-delta.sql` (§4b twins + 5.10, header correction), `fin-seed-production.sql` + generator (D-18). **None of the Oracle files has been re-run** — still blocked on the `GRANT EXECUTE ON SYS.DBMS_AQ` (§3.1 defect 4). |
| **Flyway (user's instruction, 20:03)** | Both PostgreSQL seeds are now migrations: **`V38__fin_legacy_production_config_seed.sql`** and **`V39__fin_v2_configuration_delta_seed.sql`** (verbatim minus `\set`/`BEGIN`/`COMMIT`). Applied on the dev DB at boot: V38 a no-op (325 accounts already present), V39 moved the 19 v1-shape lines, wrote 45 mappings + 88 tags, activated the 15 rules and seeded the 21 twins — **42 SYSTEM rules, all active, 0 v1-shape lines left.** App restarted on it: `Started in 5.1 s`, Flyway "up to date" at v39. One divergence recorded in the delta's header: V39 §4b.4 copied 3 api-verify mappings into `EXPENSE_PAID_REVERSED` on the polluted dev DB (it copies every active base mapping, not only SYSTEM ones); a clean DB is unaffected and the reset removes them. |

**Consequence in force:** since 14:17 UTC the legacy system records no accounting, and FIN receives nothing (no consumer). Every business row saved after the marker must be replayed (§5 step 6). `SELECT COUNT(*) FROM LOAN_PAYMENT WHERE CREATED_DATE > TIMESTAMP '2026-09-23 14:17:31.635 +00:00'` is the running size of that debt.

---

## 4. How to run what is here

```bash
# Oracle (as LOAN_SYS) — order matters.
# There is no sqlplus on the Mac host, but the erp-oracle container ships one
# (SQL*Plus 23.26.2.0.0 at /opt/oracle/product/26ai/dbhomeFree/bin/sqlplus).
# Use it — these files are written FOR sqlplus (SET ECHO ON, WHENEVER SQLERROR
# EXIT FAILURE, SHOW ERRORS, "/" terminators) and a hand-rolled splitter gets
# them wrong: see the warning below.
for f in oracle-aq-setup.sql oracle-event-emit-package.sql oracle-event-emit-triggers.sql; do
  docker cp "$f" erp-oracle:/tmp/"$f"
  docker exec erp-oracle sqlplus -S LOAN_SYS/LOAN_SYS@//localhost:1521/FREEPDB1 @/tmp/"$f" || break
done

# DO NOT drive these through node-oracledb by splitting the file yourself.
# That was tried 2026-09-23 and the splitter mis-parsed files 2 and 3: a package
# body and a trigger body both contain ";" on interior lines, so any rule that
# ends a statement at ";" cuts a PL/SQL block into fragments that will not
# compile. Only a lone "/" terminates those, which is exactly what sqlplus does
# and what an ad-hoc parser has to be told. node-oracledb is fine for single
# statements (that is how legacy-accounting-stop.sql ran — one ALTER per call),
# not for whole PL/SQL files.

# PostgreSQL — NOTHING TO RUN BY HAND since 2026-09-24: both seeds are Flyway migrations
#   V38__fin_legacy_production_config_seed.sql   (= fin-seed-production.sql)
#   V39__fin_v2_configuration_delta_seed.sql     (= fin-seed-v2-delta.sql, incl. the D-16 twins)
# Booting the application applies them (idempotent; verification RAISEs → Flyway rolls back and
# the app refuses to start — read the log). The scripts in ../seed-scripts/ are the provenance copies.
```

Verification queries are at the end of every file. Every file is re-runnable; every Oracle file has its rollback companion at the end.

**Production sequencing (differs from what happened on the test copy):** 1 → 2 → 3 with the legacy triggers still ENABLED (shadow), run the consumer, compare, **then** 4. On the test copy 4 ran first by explicit instruction; 1–3 can still run there any time — they do not touch the retired triggers.

---

## 5. Remaining — in order, with what each needs

| # | Step | Needs | How you know it is done |
|---|---|---|---|
| 1 | **Build FIN v2 backend** — `/FIN/v2/execute-backend` CORE → DATA-DOM → SVC-API → DOC → INT-C → INT-R → SEC-BE → ALIGN-BE | the executor must put the **OQ-13 data step** in the DATA-DOM migration (between `ADD COLUMN account_business_field_code` and `ADD CONSTRAINT CHK_FIN_RULE_LINE_DERIVATION_SPEC`): `UPDATE fin_rule_line SET account_business_field_code = account_derivation_value, account_derivation_value = NULL WHERE account_derivation_type_code = 'MAPPING' AND account_business_field_code IS NULL` — otherwise the migration fails on any DB carrying the v1 seed | `to_regclass('fin_account_mapping')` not null; `execution-state.json` phases COMPLETE |
| 2 | ~~Add `_REVERSED` mirror rules~~ **DONE 2026-09-24 (D-16)** — `fin-seed-v2-delta.sql` §4b seeds the 21 twins with mappings and tags; dry-run with ROLLBACK passes verification 5.1–5.10 on the current dev DB | — | 42 SYSTEM rules after the delta runs |
| 2a | ~~`GRANT EXECUTE ON SYS.DBMS_AQ TO LOAN_SYS`~~ **DONE 2026-09-24** as SYS (ORA-01749 forbids self-grant); files 1→2→3 applied and proven on a real row (§3.1). **On the live instance the DBA issues the same grant as SYS before file 1.** | — | — |
| 2b | ~~Reset the dev PostgreSQL~~ **DONE 2026-09-24** by the user; Flyway V1→V39 rebuilt it clean (326 / 42 / 90 / 0 entries). Original instruction kept for reference: **Reset the dev PostgreSQL** (`docker exec -i erp-backend-postgres psql -U postgres -d erp_db -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"` then restart the jar — Flyway V1→V39 rebuilds schema **and** seeds; nothing to run by hand) — the v2 test runs left 356 test journal entries and test balances on seeded accounts (§3.2); the classifier refuses the DROP | **you** | `fin_journal_entry` = 0, `fin_account` = 326, `fin_event_type_rule` = 42 all SYSTEM, `fin_account_mapping` = 90 |
| 3 | **Run `fin-seed-v2-delta.sql`** | step 1 | `NOTICE: verification passed — mappings 45+, tags 86+, active rules 21+` |
| 4 | **Run Oracle files 1 → 2 → 3** | nothing (test copy); on real production, do it before step 4 of the old order | `user_queues` shows `ACCOUNTING_EVENT_Q`; 7 `TRG_AE_*` VALID/ENABLED; first business save writes an `ACCOUNTING_EVENT_LOG` row with `ENQUEUED_FL = 1` |
| 5 | ~~Build the Event Consumer~~ **DONE 2026-09-24** — `fin-consumer/` (sibling repo). Differs from the old sketch where testing proved it had to: JDBC `DBMS_AQ.DEQUEUE`, not JMS (the queue's payload is JSON — no JMS path consumes it); the held-event store is in Oracle in the consumer's own schema (atomic with the dequeue), not a PostgreSQL inbox; closed period → PARK, not roll back (rollback spends the 5-delivery budget and blocks the FIFO); the SEC principal holds VIEW + CREATE (SEC refuses CREATE without VIEW). Reasons in `fin-consumer/docs/findings.md` §4 | — | done: test report, 11/11 |
| 6 | **Replay 2026** from 1 Jan to now (and everything after the 14:17 UTC marker): regenerate events from the business tables in date order through `PKG_ACCOUNTING_EVENT.emit(..., p_source_action => 'REPLAY')`; opening balances into January (period OPEN); reconcile against `DAILY_RESTRICTIONS_*` keyed on legacy `CREATED_DATE` (D-15) | steps 3–5; the one unbalanced legacy master and the 6 unlinked complaint settlements are known mismatches | trial balance FIN = legacy at the marker |
| 7 | Close periods; retire stored balances (`SAFE`, `BANK_ACCOUNT` readers `GET_PREVIOUS_SAFE` / `GET_PERIVIOUS_SAFE`) | step 6 | — |

---

## 6. Decisions already taken (do not re-open without a reason)

| # | Decision | Where |
|---|---|---|
| D-01/02 | rules post to leaves `54 عملاء محليين`, `86 موردين محليين`, `342 المبيعات` — never the legacy parents 32/85/310 | seed §5 |
| D-04 | account nature: 22→DEBIT, 23→CREDIT, 31/NULL→root default; 175/263/234 corrected to DEBIT | seed §2 |
| D-06 | 8 `موظف تجريبي` test accounts inactive; 3 over-long codes excluded | seed §2 |
| D-07 | FL 8 / 130 have no rule but **are emitted** — a revival fails loudly at FIN | triggers file |
| D-08 | Retained Earnings = V34's `3200` | — |
| D-10 | one bank GL account (30) for every cheque, as legacy | mapping rows |
| D-11 | expense-type targets corrected: 81→150, 101→306, 422/424→26, 324/282→83 (rename 83, OQ-10) | delta seed §2b |
| D-14 | rule orientation follows the books; negative is the normal sign for FL 32/55/56/57/58/59/66 | seed §5, triggers `normal_sign` |
| D-15 | `docDate` = business date (legacy used `SYSDATE`) | package |
| OQ-14 | complaint mirrors in `EXPENSE_TYPE_DT` (FL 50) are emitted — they were the only accounting for complaint revenue | package `c_skip_complaint_mirrors := FALSE` |

## 7. Open questions still needing a human

| # | Question |
|---|---|
| OQ-07 | English account names (`name_en = name_ar` today) |
| OQ-10 | rename account `83 'حـساب /'` |
| OQ-13 | the DATA-DOM executor must include the data step (see §5 step 1) |
| OQ-15 | ~~go for the `_REVERSED` mirror rules~~ closed by D-16 (2026-09-24) |
| D-18 | `82 جارى/ مساهمين الشركة` is an ASSET with CREDIT nature and the hub of 9 rule lines — keep as legacy, or reclassify as a liability? Reporting sign only; postings unaffected. Same question for the `مبيعات` subtree filed under EXPENSES |
| F-14 | `COMPLAINT_DT_FL = 49 رسوم المستثمر` (119 rows in 2026) is accounted by neither system — intended? |

---

## 8. If something goes wrong

| Symptom | Do |
|---|---|
| Legacy users need the old journal back | run the ROLLBACK COMPANION in `legacy-accounting-stop.sql` (reverse order, ENABLE). Rows saved while it was off get no legacy journal retroactively |
| A business save fails after file 3 | it cannot be the emit (it swallows) — but check `ACCOUNTING_EVENT_LOG WHERE ENQUEUED_FL = 0` for the logged error; `ALTER TRIGGER TRG_AE_<table> DISABLE` isolates |
| Queue backs up / exception queue non-empty | `SELECT * FROM AQ$ACCOUNTING_EVENT_Q` (TxEventQ view name may be `AQ$<queue>`); replay from `ACCOUNTING_EVENT_LOG` by `EVENT_REFERENCE` |
| FIN seed must be re-applied on a fresh DB | migrations to v34 → `fin-seed-production.sql` → (v2) → `fin-seed-v2-delta.sql`; all idempotent |
