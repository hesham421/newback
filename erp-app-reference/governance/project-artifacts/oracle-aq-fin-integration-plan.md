# Oracle AQ → Event Consumer → FIN — investigation and plan

**Date:** 2026-09-23 · **Status:** investigation + plan, nothing changed in either system
**Scope:** retiring the legacy in-trigger accounting in `LOAN_SYS` (Oracle) in favour of a
canonical event on a queue, consumed and posted by FIN (PostgreSQL).

> **Read this first.** Everything in §1 was read out of the live database and the working tree.
> Everything in §3–§6 is a proposal. The two are deliberately kept apart. Where the database did
> not show something, it says *not found* rather than guessing.

---

## 0. What was investigated, and the one caveat on it

| | |
|---|---|
| Oracle instance | `Oracle AI Database 26ai Free Release 23.26.2.0.0` (`v$version`) |
| Connection | `localhost:1521/FREEPDB1`, schema `LOAN_SYS` |
| Access used | read-only SELECT via node-oracledb thin mode; no DDL, no DML, no trigger touched |
| Backend | this repo, `src/main/java/com/erp/fin/**`, `src/main/resources/db/migration/V22__fin_schema.sql` |

**Caveat that affects how much of this you can trust.** The instance reachable here is **Oracle
Free edition on localhost**, holding 24,337 journal masters and 29,067 payment rows. It behaves
like a recent restore of production, but it is not the production host. Two things must be
re-confirmed against the real production instance before any of §5 is scheduled: the **edition**
(Free vs Enterprise — this decides whether TxEventQ, §2.1, is available) and whether the trigger
bodies there are byte-identical to the ones read here. Everything else below holds regardless.

A second thing worth stating plainly: **`LOAN_SYS` holds the `DBA` role** (`user_role_privs`:
`CONNECT`, `RESOURCE`, `DBA`). That is how the queue can be created at all without a DBA request
— and it is also a standing security problem that this project should not deepen.

---

## 1. Findings

### 1.1 The accounting layer is a family of triggers with a common idiom

29 triggers exist. The accounting ones are recognisable by name — the `*_Daliy_TR` family (sic,
the misspelling is in the database) — and by their dependency on the journal tables.

**Triggers that write `DAILY_RESTRICTIONS_MASTER` / `_DT`** (`user_dependencies`):

| Trigger | Table | Status | Accounting part | Business part in the same trigger |
|---|---|---|---|---|
| `TRIGGER_LOAN_PAYMENT_Daliy_TR` | `LOAN_PAYMENT` | ENABLED | all of it (9 branches) | none — pure accounting |
| `TRIGGER_LOAN_PAYMENT_Daliy_TR_AFTER` | `LOAN_PAYMENT` | ENABLED | updates `SUM_DEBIT`/`SUM_CREDIT` | none |
| `TRIGGER_CONTRACT_Daliy_TR` | `CONTRACT` | ENABLED | all of it (FL 12, 13) | none |
| `TRIGGER_COMPLAINT_Daliy_TR` | `COMPLAINTS` | ENABLED | all of it (FL 0, 1) | none |
| `TRIGGER_COMPLAINT_DT_Daliy_TR` | `COMPLAINT_DT` | ENABLED | all of it (FL 43, 44) | none |
| `TRIGGER_INVOICE_IMPORT_Daliy_TR` | `INVOICE_IMPORT` | ENABLED | all of it | none |
| `TRIGGER_TRANSFER_SAFE_Daliy_TR` | `TRANSFER_SAFE` | ENABLED | all of it | none |
| `TRIGGER_ASSETS_Daliy_TR` | `ASSETS` | ENABLED | all of it | none |
| `TRIGGER_DEPRECIABLE_Daliy_TR` | `DEPRECIABLE` | ENABLED | all of it | none |
| `TRIGGER_EXPENSE_TYPE_DT_OLD` | `EXPENSE_TYPE_DT` | ENABLED | all of it | none |
| `TRIGGER_EXPENSE_TYPE_DT` | `EXPENSE_TYPE_DT` | **DISABLED** | all of it | none — superseded by `_OLD` |
| `TRIGGER_DAILY_RESTRICTIONS_DT` | `DAILY_RESTRICTIONS_DT` | ENABLED | journal-internal totals | none |
| `TRIGGER_DAILY_RESTRICTIONS_DT_BEFOR` | `DAILY_RESTRICTIONS_DT` | ENABLED | journal-internal | none |
| `TRIGGER_DAILY_RESTRICTIONS_MASTER` | `DAILY_RESTRICTIONS_MASTER` | ENABLED | journal-internal | none |

**This is the single most useful structural fact in the whole investigation:** the accounting
logic is *already physically separated* into its own triggers. The brief anticipated triggers
that mix accounting with business logic and warned that `ARREARS` and `ITEMS_DT` maintenance must
survive. In this database they are **not** mixed — the business logic lives in differently named
triggers on the same tables:

| Business trigger | Table | What it maintains — must survive |
|---|---|---|
| `TRIGGER_CONTRACT_ARREAR` | `CONTRACT` | `ARREARS` |
| `TRIGGER_ARREARS_CONTRACT_FL` | `CONTRACT` | `ARREARS` (statement-level) |
| `TRIGGER_CONTRACT` | `CONTRACT_DT` | `ITEMS_DT` |
| `TRIGGER_INVOICE_IMPORT` | `INVOICE_IMPORT_DT` | `ITEMS_DT` |
| `TRIGGER_ITEMS_DT` | `ITEMS_DT` | stock |
| `TRIGGER_COMPLAINT_DT_EXP_DT` | `COMPLAINT_DT` | expense detail |
| `TRIGGER_LOAN_PAYMENT_ORG_SUB`, `COMPOUND_TRIGGER_LOAN_PAYMENT_ORG_SUB` | `LOAN_PAYMENT` | org/branch derivation |
| `TRIGGER_ORGANIZATION_SUB`, `TRIGGER_YEAR_PERIOD` | — | `SAFE` row creation, period state |

Retirement is therefore *per trigger*, not per statement inside a trigger. That removes most of
the risk the brief was braced for.

### 1.2 How an account is chosen today

Every accounting trigger uses the same idiom (`TRIGGER_LOAN_PAYMENT_Daliy_TR`, lines 20–30):

```sql
select ACCOUNT_CHART_FK into P_ACCOUNT_CHART_FK_CREDIT
  from DAILY_RESTRICTIONS_DT, DAILY_RESTRICTIONS_MASTER
 where RESTRICTIONS_MASTER_PK = RESTRICTIONS_MASTER_FK
   and SCREEN_FK = 14 and RESTRICTIONS_TYPE_FK = 23;
```

**The journal tables double as the configuration store.** A `DAILY_RESTRICTIONS_MASTER` row with
`SCREEN_FK IS NOT NULL` is not a journal entry — it is an account-mapping template. Real entries
have `SCREEN_FK IS NULL`. The template key is up to four columns:

- `SCREEN_FK` — which business screen (`PRV_SCREENS`)
- `DAILY_SCREEN_STATUS_FK` — which event variant (`SYSTEM_TANSACTION_DT`)
- `RESTRICTIONS_TYPE_FK` — `22` = debit side, `23` = credit side
- `ACCOUNT_PRIMARY_FL` — a fourth disambiguator, used only by `TRIGGER_COMPLAINT_Daliy_TR`

**The complete posting matrix as configured today** (31 rows, 16 template pairs):

| Screen | Status | Screen name (AR) | Event variant (AR) | Debit (22) | Credit (23) |
|---|---|---|---|---|---|
| 11 | — | العقود | — | 82 جارى/ مساهمين الشركة | 310 مبيعات |
| 12 | — | فاتورة مشتريات | — | 323 مشتريات | 85 الموردين **and** 24 الصندوق ⚠ |
| 14 | — | التسديدات | — | 24 الصندوق | 32 العملاء |
| 15 | 121 | مخالصات | مخالصة بقيمة العقد | 236 تسويقى/خصم مسموح به | 32 العملاء |
| 23 | 43 | مخالصات الشكاوى | سداد قسط بالمحكمة | 236 تسويقى/خصم مسموح به | 32 العملاء |
| 24 | 43 | تسديد الشكاوى | سداد قسط بالمحكمة | 24 الصندوق | 32 العملاء |
| 24 | 44 | تسديد الشكاوى | سداد دفعات محامى | 24 الصندوق | 295 أتعاب وإستشارات مهنية |
| 25 | 32 | اشعار اضافة/خصم للمستثمر | دعم/مسحوبات مستثمر | 24 الصندوق | 82 جارى/ مساهمين الشركة |
| 25 | 58 | اشعار اضافة/خصم للمستثمر | مقدم المحامى | 24 الصندوق | 82 جارى/ مساهمين الشركة |
| 25 | 59 | اشعار اضافة/خصم للمستثمر | رسوم المستثمر 2.5 | 24 الصندوق | 82 جارى/ مساهمين الشركة |
| 25 | 66 | اشعار اضافة/خصم للمستثمر | فاتورة مبيعات للمستثمر | 82 جارى/ مساهمين الشركة | 32 العملاء |
| 30 | — | المصروفات | — | 3 المصروفات | 24 الصندوق |
| 34 | — | تسديدات الموردين | — | 85 الموردين | 24 الصندوق |
| 35 | — | الايرادات | — | 24 الصندوق | 4 الإيرادات |
| 38 | — | صاحب الشركة | — | 141 جاري الشريك 1 | 24 الصندوق |

⚠ Screen 12 has **two** credit rows and the trigger disambiguates with `rownum`, so which account
a purchase invoice credits depends on row order. That is a latent defect, not a rule.

**Cash/bank swap.** Every trigger post-processes the resolved account with the same `CASE`:

```sql
case when P_ACCOUNT_CHART_FK_DEBIT = 24 and :new.PAYMENT_TYPE_FK = 1 then 30   -- bank
     when P_ACCOUNT_CHART_FK_DEBIT = 24 and :new.PAYMENT_TYPE_FK = 0 then 24   -- cash
     else P_ACCOUNT_CHART_FK_DEBIT end
```

So account 24 (الصندوق) becomes 30 when the payment is by cheque. The payment method is a
*business* fact; the account swap is an *accounting* decision. This is exactly the seam the new
design cuts along.

### 1.3 The complete event vocabulary actually in use

Decoded by joining `SYSTEM_TANSACTION_DT.SYSTEM_TANSACTION_DT_PK` (the value the `*_FL` columns
carry) to `SYSTEM_TRANSACTION`. Volumes are `COUNT(*)` over `LOAN_PAYMENT`.

| Source | Flag | Meaning (AR) | Screen/Status used | Rows | Last seen |
|---|---|---|---|---|---|
| `LOAN_PAYMENT` | 7 | سداد الاقساط | 14 | 15,934 | 2026-09-22 |
| `LOAN_PAYMENT` | 66 | فاتورة مبيعات للمستثمر | 25/66 | 4,377 | 2026-09-20 |
| `LOAN_PAYMENT` | 32 | دعم/مسحوبات مستثمر | 25/32 | 2,217 | 2026-09-21 |
| `LOAN_PAYMENT` | 59 | رسوم المستثمر 2.5 | 25/59 | 1,839 | *null* |
| `LOAN_PAYMENT` | 58 | مقدم المحامى | 25/58 | 1,839 | *null* |
| `LOAN_PAYMENT` | 55 | اجراء تنفيذي | 25/**32** | 1,569 | *null* |
| `LOAN_PAYMENT` | 49 | رسوم المستثمر | 25/**59** ⚠ | 666 | *null* |
| `LOAN_PAYMENT` | 56 | بعيثة | 25/**32** | 386 | *null* |
| `LOAN_PAYMENT` | 57 | رسوم مدنية | 25/**32** | 240 | *null* |
| `LOAN_PAYMENT` | 8 | سداد موردين | 34 | **0** | — |
| `LOAN_PAYMENT` | 130 | دعم/مسحوبات صاحب الشركة | 38 | **0** | — |
| `CONTRACT` | 12 | عقد | 11 | — | — |
| `CONTRACT` | 13 | مخالصة | 15/121 | — | — |
| `COMPLAINTS` | 0, 1 | شكوى / مخالصة شكاوي | 23 + `ACCOUNT_PRIMARY_FL=1` | — | — |
| `COMPLAINT_DT` | 43 | سداد قسط بالمحكمة | 24/43 | — | — |
| `COMPLAINT_DT` | 44 | سداد دفعات محامى | 24/44 | — | — |
| `INVOICE_IMPORT` | — | فاتورة مشتريات | 12 | 187 | — |
| `EXPENSE_TYPE_DT` | — | مصروف / إيراد | 30, 35 | — | — |
| `TRANSFER_SAFE` | — | تحويل خزنة | **hardcoded 24/30** | — | — |
| `ASSETS`, `DEPRECIABLE` | — | أصول / إهلاك | 23 ⚠ | — | — |

**Branches 8 and 130 are dead code** — coded, never used. They should be carried as event types
only if the business says they will be revived.

### 1.4 Defects found in the current accounting code

These matter because a migration that reproduces them faithfully reproduces bugs, and one that
silently fixes them changes the books.

1. **`FL = 49` reads the wrong template.** `TRIGGER_LOAN_PAYMENT_Daliy_TR:337` branches on
   `LOAN_PAYMENT_FL = 49` but looks up `DAILY_SCREEN_STATUS_FK = 59`, and its note guard at
   line 357 tests `IF :new.LOAN_PAYMENT_FL = 59` — which can never be true inside a `FL = 49`
   branch. 666 rows have posted with a `NULL` description as a result.
2. **`ASSETS` and `DEPRECIABLE` post to the complaints-settlement accounts.** Both look up
   `SCREEN_FK = 23` with no status and no `ACCOUNT_PRIMARY_FL` filter. Screen 23 holds exactly
   one pair: `236 تسويقى/خصم مسموح به` / `32 العملاء`. Asset and depreciation entries are
   therefore landing on a marketing-discount and a receivables account. **Suspected defect —
   needs business confirmation before it is either reproduced or corrected.**
3. **No exception handler anywhere.** `grep -ci exception` returns 0 across all nine accounting
   triggers. A `SELECT … INTO` that finds no template row raises `NO_DATA_FOUND`, which is
   unhandled, which rolls back **the business DML itself**. A missing configuration row does not
   just skip the accounting — it stops the user saving the payment.
4. **Screen 12 resolved by `rownum`** (see §1.2).
5. **One unbalanced master** exists: `SUM(DEBIT) <> SUM(CREDIT)` for one
   `RESTRICTIONS_MASTER_FK`. One in 24,337 — but FIN's `RULE-FIN-006` would reject it, so the
   parallel run must expect exactly one mismatch of this kind.

### 1.5 The stored balances are already dead

| Column | Current state | Written by | Status |
|---|---|---|---|
| `SAFE.SAFE_BALANCE` | 1900 / 470 / 0 across 3 rows | `TRIGGER_LOAN_PAYMENT`, `TRIGGER_COMPLAINT_DT` | **both DISABLED** |
| `SAFE.CUSTOMER_BALANCE`, `SUPPLIER_BALANCE` | all 0 | same | **both DISABLED** |
| `BANK_ACCOUNT.DEBIT` | all 0 | same | **both DISABLED** |
| `BANK_ACCOUNT.CREDIT` | 2022.5 / 205 / 573 / 0 | same | **both DISABLED** |

Both writers are disabled, so these numbers stopped moving at some unknown past date while the
journal kept going to 2026-09-22. **They are stale, and nothing has depended on them being
correct for some time.** Readers still exist — `GET_PREVIOUS_SAFE` and `GET_PERIVIOUS_SAFE`
(both spellings, two separate functions) — and whatever reports call them are already reporting
frozen numbers. This removes essentially all risk from "balances become derived": there is no
correct stored balance to lose.

### 1.6 Someone already built a version of this, and it is still running

| Object | Kind | State |
|---|---|---|
| `ACCOUNTING_QUEUE` | table | 4 rows, all 2025-05-13 |
| `ACCOUNTING_RULES` | table | **1** row |
| `ACCOUNTING_RULE_ITEMS` | table | 2 rows |
| `ACCOUNTING_ENTRY_HISTORY` | table | 7 rows |
| `CREATE_ACCOUNTING_ENTRY`, `UPDATE_ACCOUNTING_ENTRY`, `PROCESS_ACCOUNTING_QUEUE`, `RETRY_FAILED_ACCOUNTING_QUEUE`, `UNIVERSAL_CREATE_ENTRY`, `UNIVERSAL_UPDATE_ENTRY` | procedures | present |
| `JOB_PROCESS_ACCOUNTING_QUEUE` | scheduler job | **ENABLED, `FREQ=MINUTELY;INTERVAL=5`, last run 2026-09-23** |

A May-2025 attempt at exactly this architecture: a table-based queue, data-defined rules
(`ENTITY_SOURCE`, `AMOUNT_COLUMN`, `ACCOUNT_FK_COLUMN`), a polling job, a retry procedure and an
audit history. It got exactly **one** rule configured (`قاعدة المصروفات`, over `EXPENSE_TYPE_DT`)
and was abandoned — but **the job is still enabled and still firing every five minutes today**.

Two things it teaches, both expensive to learn twice:

- **It kept the accounting inside Oracle.** `ACCOUNTING_RULES.CASH_ACCOUNT_FK = 24`,
  `BANK_ACCOUNT_FK = 30`, `ACCOUNTING_RULE_ITEMS.ACCOUNT_CHART_FK` — the rules table is a
  chart-of-accounts consumer. That is precisely the boundary the new design forbids, and it is
  why this attempt could never have retired the accounting, only relocated it.
- **Idempotency bit it immediately.** Of 4 queue rows, 3 say
  `تمت معالجة هذه العملية سابقاً، لا داعي للتكرار` for the *same* `ENTITY_ID = 4668`, at 18:02,
  18:03, 18:08 and 18:13. The first produced a reversal plus a new entry (16312, 16313); the next
  three were caught by a guard. Duplicate delivery is not hypothetical here — it happened within
  eleven minutes of the only real test.

### 1.7 Oracle AQ is not installed

```
user_queues        → 0 rows
user_queue_tables  → 0 rows
```

`SYS.DBMS_AQ` and `SYS.DBMS_AQADM` exist and `LOAN_SYS` is `DBA`, so creating a queue needs no
new grant. **But nothing about AQ is in place today — this is a greenfield build, not a
configuration change.**

### 1.8 The FIN side of the contract

| What | Where | Fact |
|---|---|---|
| Endpoint | [JournalEntryController.java:59](src/main/java/com/erp/fin/controller/JournalEntryController.java#L59) | `POST /api/v1/fin/journal-entries/from-event` exists and is wired to `EventEntryService.build` |
| Request body | [EventEntryBuildRequest.java](src/main/java/com/erp/fin/dto/EventEntryBuildRequest.java) | `eventReference` (≤100, required), `eventTypeCode` (≤50, required), `docDate` (required), `baseAmount` (required, ≥0), `amounts: Map<String,BigDecimal>`, `fields: Map<String,String>`, `descriptionAr`, `descriptionEn` |
| Idempotency | [EventEntryService.java:92](src/main/java/com/erp/fin/service/EventEntryService.java#L92) + [V22__fin_schema.sql:422](src/main/resources/db/migration/V22__fin_schema.sql#L422) | `existsByEventReference` → `FIN-409-DUPLICATE-EVENT` (HTTP 409), **backed by a real unique constraint** `UQ_FIN_JOURNAL_ENTRY_EVENT_REF` |
| Rule lookup | same, line 97 | `EventTypeRule` by `eventTypeCode` + active → else `FIN-404-NO-ACTIVE-RULE` |
| Rule shape | `EventTypeRule`, `RuleLine` | rule: `EVENT_TYPE_CODE`, `NAME_AR/EN`, `IS_ACTIVE_FL`. line: `LINE_NO`, `ACCOUNT_DERIVATION_TYPE_CODE`, `ACCOUNT_DERIVATION_VALUE`, `AMOUNT_SOURCE_TYPE_CODE`, `AMOUNT_SOURCE_VALUE`, `DIRECTION_CODE`, `DISTRIBUTION_TYPE_CODE`, `IS_REMAINDER_FL` |
| Account derivation | [EventTypeRuleDomain.java:180](src/main/java/com/erp/fin/domain/EventTypeRuleDomain.java#L180) | `CONSTANT` = the value *is* the account code · `DIRECT` = the value names an **event field carrying the account code** · `MAPPING` = **rejected**, `FIN-422-MAPPING-UNSUPPORTED` |
| Amount sourcing | same | `FIELD` (named key in `amounts`), `PERCENTAGE` (of `baseAmount`), `REMAINDER` |
| Auth | `@PreAuthorize` on `EventEntryService.build` | `PERM_FIN_JOURNAL_ENTRIES_CREATE`; JWT via `JwtAuthenticationFilter`, validated against an `ActiveSession` row; `app.jwt.expiration-ms=3600000` |
| Dimensions | `EventEntryService.buildLines` javadoc | **rule-driven lines carry no dimension tags** — `ENT-FIN-010` declares no dimension column |

> **Superseded 2026-09-23 18:05 by FIN v2** (`governance/shared/analysis/modules/FIN/v2/`, backend execution pending): `DIRECT` is removed (ADR-FIN-010), `MAPPING` is implemented through `FIN_ACCOUNT_MAPPING` keyed per event type (RULE-FIN-020/022), rule lines carry `account_business_field_code`, and `FIN_RULE_LINE_DIM` stamps ORG/BRANCH from the event (RULE-FIN-025..027). The event's `fields{}` keys are the `FIN_EVENT_BUSINESS_FIELD` codes shown in §3.2. The paragraph below describes v1 as it was when this plan was written; Phase 0 in §5 is now "execute `/FIN/v2/execute-backend`", not "specify a store".

**The finding that shaped the v1 design.** Of FIN's three derivation modes:

- `CONSTANT` — boundary-safe, but the account is fixed per rule line.
- `DIRECT` — **would breach the hard boundary.** It reads the account code out of
  `request.fields`, i.e. it requires the event to carry an accounting identifier.
- `MAPPING` — the only mode that could turn a *business* key into an account inside FIN, and it
  **throws `FIN-422-MAPPING-UNSUPPORTED`**. FIN has no mapping store; the code says so explicitly
  and refuses rather than guessing.

So today FIN can honour the boundary **only** with `CONSTANT` lines. Read against §1.2, that is
almost enough — 14 of the 16 legacy template pairs are fixed account pairs and map to `CONSTANT`
lines directly. What `CONSTANT` cannot express is the two places where the legacy code varies the
account by a business value: the **cash/bank swap on `PAYMENT_TYPE_FK`** and the **expense
account per `EXPENSE_TYPE_FK`**. Those two need `MAPPING`, and `MAPPING` does not exist.

---

## 2. Research summary

### 2.1 AQ vs TxEventQ, and which Java API

The native `oracle.AQ` Java package was **deprecated in 10g Release 1**; Oracle directs new work
to the JMS API ([Oracle AQ programmatic
interfaces](https://docs.oracle.com/cd/E11882_01/server.112/e11013/aq_envir.htm)). For a database
at 23ai, **Transactional Event Queues (TxEventQ) is the recommended choice for new
applications** — it is an optimised reimplementation of AQ (originally AQ Sharded Queues,
rebranded in 21c), speaks the same JMS surface, and is the only variant with Kafka-compatible
APIs ([Oracle AQ/TxEventQ product
page](https://www.oracle.com/database/advanced-queuing/), [Oracle
blog](https://blogs.oracle.com/database/oracle-transactional-event-queues-txeventq-scalable-messaging-streaming-in-the-database)).

**Where this changed the recommendation.** The brief names "Oracle AQ" and I would otherwise have
planned a classic `DBMS_AQADM.CREATE_QUEUE_TABLE` queue with the native API. Both halves of that
are wrong for a 23ai target: the native Java API is two decades deprecated, and classic AQ is the
legacy variant. The plan below uses **TxEventQ with a JSON payload, consumed over JMS**, and
keeps the PL/SQL enqueue on `DBMS_AQ.ENQUEUE`, which is unchanged between the two.

**Unresolved, and it matters:** whether TxEventQ is available on the *production* edition. If
production is Free or Standard and TxEventQ is unavailable, classic AQ over JMS is the fallback —
same enqueue code, same JMS consumer, only the `CREATE_QUEUE_TABLE` call differs. This is
**OQ-1** in §7.

### 2.2 Exactly-once across two databases

The consensus is unambiguous and it is the opposite of what "exactly-once" suggests: you get
**at-least-once delivery plus an idempotent consumer**, and that combination is what "effectively
once" means in practice ([Outbox, inbox patterns and delivery
guarantees](https://event-driven.io/en/outbox_inbox_patterns_and_delivery_guarantees_explained/),
[AWS Prescriptive
Guidance](https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/transactional-outbox.html)).
The consumer must carry a stable event id and check it before applying any state change
([inbox pattern](https://adhdecode.com/message-queues/outbox-pattern-and-cdc/inbox-pattern/)).

The dequeue-commit ordering is where this bites. The consumer holds a dequeue open in an Oracle
transaction while making an HTTP call to a different database. Both orderings lose something:

- **Commit the dequeue before the HTTP call** → a crash in between loses the event permanently.
- **Commit after** → a crash after FIN posted but before the dequeue commits re-delivers the
  event, and `max_retries` eventually parks it in the exception queue *even though it succeeded*.

The second is the correct trade, because the redelivery is recoverable and the loss is not — but
only if FIN's duplicate rejection is treated as **success**, not as an error.

**Where this changed the recommendation.** I would have accepted FIN's `RULE-FIN-004` as
sufficient on its own. It is not, for one specific reason: a `409 FIN-409-DUPLICATE-EVENT` and a
lost response to a `201` are indistinguishable from the consumer's side, and both must
acknowledge the message. That is fine. What `RULE-FIN-004` cannot do is tell the consumer
*which FIN entry* a duplicate corresponds to, nor survive the case where FIN is reachable but the
consumer dies before committing the dequeue more than `max_retries` times. A consumer-side inbox
table closes both. See §3.3.

### 2.3 Dead-letter and retry in AQ

`max_retries` counts **rollbacks after a `REMOVE`-mode dequeue**; on reaching it the message
moves to the exception queue, `AQ$_<queue_table>_E` by default. `retry_delay` (seconds, default
0) holds the message unavailable before the queue monitor marks it `READY` again. Only
`max_retries`, `retry_delay`, `retention_time` and `comment` are alterable after creation
([DBMS_AQADM](https://docs.oracle.com/en/database/oracle/oracle-database/19/arpls/DBMS_AQADM.html),
[AQ administrative
interface](https://docs.oracle.com/en/database/oracle/oracle-database/21/adque/aq-administrative-interface.html)).

**Critical constraint:** `sort_list` — the dequeue order — **cannot be changed after the queue
table is created**. Ordering must be decided before the first `CREATE_QUEUE_TABLE`, not
discovered later.

### 2.4 Parallel run

Shadow mode is the standard approach: the new path consumes the same events and produces output
that is **compared but not acted on**, for long enough to cover the business cycle, before any
traffic depends on it ([parallel-run
playbook](https://www.easy.bi/blog/parallel-run-legacy-migration-playbook/), [Azure strangler
fig](https://learn.microsoft.com/en-us/azure/architecture/patterns/strangler-fig)).

**Where this changed the recommendation.** A shorter shadow window would have looked defensible.
The volume table in §1.3 rules it out: `FL = 58/59/55/49/56/57` together account for ~6,500 rows
and **none of them carries a `CREATED_DATE`**, so their frequency cannot be measured from the
data. A month-long shadow that happens to miss a quarterly investor-fee run proves nothing about
the branch it did not exercise. The window must be defined by **branch coverage**, not by
elapsed time — §5, Phase 4.

---

## 3. Target design

### 3.1 Where the consumer lives — a separate service

**Recommendation: a separate deployable, not a module inside the existing Spring Boot app.**

The boundary rule decides it. `FIN never reads a legacy table` is a property that has to be
*enforceable*, and the cheapest enforcement is that the process which posts to FIN has no Oracle
DataSource in it at all. Putting the consumer inside this backend would place an Oracle driver,
an Oracle connection pool and Oracle credentials into the same JVM as FIN — and this repo's own
ArchUnit module-boundary suite (`src/test/java/com/erp/architecture`) checks *package*
dependencies, not datasource reachability. It could not catch the violation.

Secondary reasons, in order of weight: the consumer's failure and restart cadence has nothing to
do with the web app's; it needs to scale to zero without taking the API down; and it must keep
working during a backend deploy, or the queue silently backs up mid-release.

Cost of the split, stated honestly: a second deployable to build, monitor and release, and a
service identity to manage (§3.6). Both are real. Neither is comparable to an accounting boundary
that is only enforced by convention.

### 3.2 The canonical event

Payload: **JSON**, on a TxEventQ queue `LOAN_SYS.ACCOUNTING_EVENT_Q`.

```jsonc
{
  "eventReference": "LEGACY:LOAN_PAYMENT:1048577:9137",   // LEGACY:<TABLE>:<PK>:<ACCOUNTING_EVENT_SEQ> — as PKG_ACCOUNTING_EVENT emits it
  "eventTypeCode":  "INSTALLMENT_PAYMENT_RECEIVED",
  "occurredAt":     "2026-09-22T11:04:19Z",
  "docDate":        "2026-09-22",
  "baseAmount":     250.000,
  "amounts":        { "grossAmount": 250.000 },
  "fields": {                          // keys = FIN_EVENT_BUSINESS_FIELD codes (FIN v2, ADR-FIN-013)
    "PAYMENT_METHOD":    "CASH",       // from PAYMENT_TYPE_FK 0|1 — a business fact; MDL PAYMENT_METHOD code
    "EXPENSE_TYPE_CODE": null,         // EXPENSE_TYPE_ID as a string, expense events only
    "ORGANISATION_CODE": "1",          // ORGANIZATION_FK — resolves FIN_DIMENSION_VALUE.code in ORG
    "BRANCH_CODE":       "2",          // ORGANIZATION_SUB_FK — resolves FIN_DIMENSION_VALUE.code in BRANCH
    "sourceTable":       "LOAN_PAYMENT",   // the rest are references FIN never reads (REQ-FIN-047)
    "sourcePk":          "1048577",
    "partyType":         "CUSTOMER",
    "partyCode":         "4412",
    "contractNo":        "C-2026-0118",
    "investorCode":      null,
    "complaintNo":       null
  },
  "descriptionAr": "سداد قسط",
  "descriptionEn": "Installment payment"
}
```

Field by field, and why each one is allowed to exist:

| Field | Source in Oracle | Why it is not an accounting concept |
|---|---|---|
| `eventReference` | composed from table + flag + PK + revision | an identity, not a ledger fact |
| `eventTypeCode` | derived from `*_FL` via a fixed table in the trigger | names a *business* event |
| `occurredAt` | `SYSTIMESTAMP` at enqueue | — |
| `docDate` | `LOAN_PAYMENT_DATE` / `COMPLAINT_DT_DATE` / `INV_IMPORT_DATE` / `CONTRACT` date | — |
| `baseAmount` | `LOAN_PAYMENT_VALUE`, `VALUE`, `CASH_PRICE`, `AMOUNT`, `ASSETS_VALUE` | a business amount |
| `amounts.*` | additional amount columns (`DISCOUNT`, `PAYMENT`, `REMAINING`) | business amounts |
| `fields.orgCode`, `branchCode` | `ORGANIZATION_FK`, `ORGANIZATION_SUB_FK` | organisational, not accounting |
| `fields.paymentMethod` | `PAYMENT_TYPE_FK` → `CASH` \| `CHEQUE` | **the business fact behind the 24→30 swap** |
| `fields.partyType`/`partyCode` | `CUSTOMERS_FK` / `SUPPLIERS_FK` / `INVESTOR_FK` | a party, not a control account |
| `fields.contractNo`, `complaintNo`, `investorCode` | `CONTRACT_FK`, `COMPLAINT_FK`, `INVESTOR_FK` | references |
| `fields.expenseTypeCode` | `EXPENSE_TYPE_FK` (expense events only) | a business classification |

**What is deliberately absent:** `ACCOUNT_CHART_FK`, `RESTRICTIONS_TYPE_FK`, `SCREEN_FK`,
`DAILY_SCREEN_STATUS_FK`, `ACCOUNT_PRIMARY_FL`, `RESTRICTIONS_MASTER_FK`, and any debit/credit
designation. The legacy `SCREEN_FK`/`DAILY_SCREEN_STATUS_FK` pair is **not** carried and then
ignored — it is replaced at the trigger by `eventTypeCode`, so the accounting vocabulary never
enters the payload in the first place.

**`paymentMethod` is the test case for the boundary.** Legacy resolves account 24, then swaps it
to 30 when `PAYMENT_TYPE_FK = 1`. The new design sends `paymentMethod: "CHEQUE"` and lets FIN
decide that cheques land on account 30. The event says *how the customer paid*; FIN says *which
account that means*. If anyone later proposes putting `cashAccountCode` in the payload to "save a
lookup", that is the boundary breaking, and §1.6 is what it looks like a year later.

### 3.3 Exactly-once — FIN's rule is necessary but not sufficient

**Decision: keep `RULE-FIN-004` *and* add a consumer-side inbox table (in PostgreSQL, owned by
the consumer).**

The mechanism, end to end:

1. **Enqueue is atomic with the business write.** The trigger calls `DBMS_AQ.ENQUEUE` in the same
   transaction as the `INSERT`/`UPDATE`. No outbox table is needed: for a queue *inside the same
   database*, the queue **is** the outbox. If the business write rolls back, so does the message.
2. **Consumer dequeues** in `REMOVE` mode inside an Oracle transaction, and does **not** commit.
3. **Consumer writes `inbox(event_reference, status='IN_FLIGHT')`** to its own PostgreSQL schema
   and commits. This is the durable record that the event was seen.
4. **Consumer calls FIN.** `201` → mark `POSTED` with the returned entry id. `409
   FIN-409-DUPLICATE-EVENT` → mark `POSTED` (already done — **this is success, not failure**).
   `4xx` other → mark `REJECTED`. `5xx`/timeout → leave `IN_FLIGHT`.
5. **Consumer commits the Oracle dequeue.** Message gone.

What each layer buys, and what dropping it costs:

| Layer | Catches | Cost of omitting it |
|---|---|---|
| `RULE-FIN-004` + `UQ_FIN_JOURNAL_ENTRY_EVENT_REF` | any duplicate arriving at FIN, from any source, forever | double-posted entries — unacceptable |
| Consumer inbox | crash between step 4 and step 5; distinguishes "never sent" from "sent, response lost"; gives an operator a queryable record of every event ever seen | recoverable, but only by reading FIN — and a `409` alone cannot tell you *when* or *why* |

**Why not rely on `RULE-FIN-004` alone?** It is sufficient for *correctness* — no duplicate entry
can ever be created, because a unique constraint enforces it in PostgreSQL. It is not sufficient
for *operations*. Without the inbox, an event that dies between steps 4 and 5 is retried until
`max_retries`, then lands in the exception queue looking like a failure, when FIN posted it
successfully on the first attempt. Someone then has to reconcile the exception queue against
FIN by hand. The inbox is one table and it turns that investigation into a `SELECT`.

**One race worth naming:** `existsByEventReference` followed by an insert is check-then-act, so
two concurrent deliveries of the same reference can both pass the check. The unique constraint
catches the loser, but it surfaces as a `DataIntegrityViolationException`, not as
`FIN-409-DUPLICATE-EVENT`. With a single-threaded consumer per queue (§3.5) this cannot arise
from our side. It is **OQ-5**.

### 3.4 Failure handling — nothing is dropped

| Failure | Behaviour | Recovery |
|---|---|---|
| **Translation failure** (unknown `eventTypeCode`, missing required column) | consumer does not call FIN; marks inbox `UNTRANSLATABLE`; **rolls back** the dequeue | after `max_retries` → exception queue `AQ$_ACCOUNTING_EVENT_QT_E`; alert; fix the mapping; replay |
| **FIN unreachable / 5xx / timeout** | leave `IN_FLIGHT`, roll back the dequeue | `retry_delay = 60`, `max_retries = 5` → ~5 min of retries, then exception queue |
| **FIN rejects — `FIN-404-NO-ACTIVE-RULE`** | mark `REJECTED`, roll back | a configuration gap, not a data problem: configure the rule, replay from the exception queue |
| **FIN rejects — `FIN-409-PERIOD-NOT-OPEN`** | mark `DEFERRED`, roll back | period-open is an operational state; this is the one case where a long `retry_delay` is right |
| **FIN rejects — `FIN-409-DUPLICATE-EVENT`** | **mark `POSTED`, commit the dequeue** | none needed — this is the success path for a redelivery |
| **FIN rejects — `FIN-422-MAPPING-UNSUPPORTED`** | mark `REJECTED`, alert loudly | means a `MAPPING` rule line was configured before §5 Phase 0 landed |
| **Poison message** (malformed JSON) | exception queue immediately, no retry | manual |
| **Consumer crash** | message never committed → redelivered | inbox tells you whether FIN already has it |

**The exception queue is the single place nothing escapes from.** It needs an owner, an alert on
`depth > 0`, and a documented replay procedure. A dead-letter queue nobody watches is the same as
dropping the message, just slower.

### 3.5 Ordering

**Per-key ordering is required, and the key is the source document.**

The legacy triggers fire on `INSERT OR UPDATE`, and the update path rewrites the existing entry's
lines (`update DAILY_RESTRICTIONS_DT set CREDIT = …`). In the new model an update becomes a
reversal plus a new entry. Applying those two out of order leaves the ledger reversed instead of
restated. Within one source document, order is a correctness property.

Across documents it is not: two unrelated payments may post in any order.

**Recommendation:** create the queue table with `sort_list = 'ENQ_TIME'` and run **one consumer
thread per queue**, at least through Phase 5. Because `sort_list` is immutable after creation
(§2.3), this must be right the first time. Single-threaded throughput is the constraint to check
against volume — 15,934 installment payments in the visible history is not a demanding rate, but
it is the number to size against.

If throughput later demands parallelism, the correct move is TxEventQ **message groups keyed by
`sourceTable:sourcePk`**, which preserves per-document order while allowing concurrency across
documents. Do not parallelise by simply adding threads to a single queue — that silently discards
the ordering guarantee.

### 3.6 Identity and permission

The consumer authenticates as a dedicated SEC user holding **exactly**
`PERM_FIN_JOURNAL_ENTRIES_CREATE` and nothing else.

Two facts from §1.8 constrain this: the access token expires after **1 hour**, and
`JwtAuthenticationFilter` validates every request against a live `ActiveSession` row. A daemon
must therefore re-authenticate or refresh on `401`, and a session cleanup job that deletes its
row will take the integration down. **There is no service-account concept in SEC today** — this
is a human-user credential used by a machine. It works; it is not what it was designed for. That
is **OQ-4**.

---

## 4. Event mapping table

`C` = `CONSTANT` (account code fixed on the rule line). `M` = needs `MAPPING` — **unsupported
today** (§1.8).

| # | Legacy branch | Canonical `eventTypeCode` | Debit line | Credit line | Gap |
|---|---|---|---|---|---|
| 1 | `LOAN_PAYMENT.FL=7` (scr 14) | `INSTALLMENT_PAYMENT_RECEIVED` | C 24 / **M** on `paymentMethod` → 30 | C 32 | cash/bank swap needs `MAPPING` |
| 2 | `LOAN_PAYMENT.FL=8` (scr 34) | `SUPPLIER_PAYMENT_MADE` | C 85 | C 24 / **M** → 30 | **0 rows — dead branch**, confirm before building |
| 3 | `LOAN_PAYMENT.FL=32` (scr 25/32) | `INVESTOR_SUPPORT_OR_DRAWING` | C 24 / **M** → 30 | C 82 | — |
| 4 | `LOAN_PAYMENT.FL=66` (scr 25/66) | `INVESTOR_SALES_INVOICE` | C 82 | C 32 | — |
| 5 | `LOAN_PAYMENT.FL=58` (scr 25/58) | `LAWYER_ADVANCE` | C 24 / **M** → 30 | C 82 | — |
| 6 | `LOAN_PAYMENT.FL=59` (scr 25/59) | `INVESTOR_FEE_2_5` | C 24 / **M** → 30 | C 82 | — |
| 7 | `LOAN_PAYMENT.FL=49` (scr 25/**59**) | `INVESTOR_FEE` | C 24 / **M** → 30 | C 82 | **defect §1.4.1** — same template as #6; is 49 a distinct event or a duplicate of 59? |
| 8 | `LOAN_PAYMENT.FL=55` (scr 25/**32**) | `EXECUTION_ACTION_FEE` | C 24 / **M** → 30 | C 82 | shares #3's template; distinguished only by note text |
| 9 | `LOAN_PAYMENT.FL=56` (scr 25/**32**) | `BAEETHA_FEE` | C 24 / **M** → 30 | C 82 | as #8 |
| 10 | `LOAN_PAYMENT.FL=57` (scr 25/**32**) | `CIVIL_FEE` | C 24 / **M** → 30 | C 82 | as #8 |
| 11 | `LOAN_PAYMENT.FL=130` (scr 38) | `OWNER_DRAWING` | C 141 | C 24 / **M** → 30 | **0 rows — dead branch** |
| 12 | `CONTRACT.FL=12` (scr 11) | `CONTRACT_CREATED` | C 82 | C 310 | — |
| 13 | `CONTRACT.FL=13` (scr 15/121) | `CONTRACT_SETTLEMENT` | C 236 | C 32 | also **deletes** the prior settlement entry on 13→12 — becomes a reversal (§6) |
| 14 | `COMPLAINTS.FL=0\|1` (scr 23, `PRIMARY_FL=1`) | `COMPLAINT_SETTLEMENT` | C 236 | C 32 | what distinguishes FL 0 from FL 1 — **not found** in the trigger's account logic |
| 15 | `COMPLAINT_DT.FL=43` (scr 24/43) | `COURT_INSTALLMENT_PAYMENT` | C 24 / **M** → 30 | C 32 | — |
| 16 | `COMPLAINT_DT.FL=44` (scr 24/44) | `LAWYER_FEE_PAYMENT` | C 24 / **M** → 30 | C 295 | — |
| 17 | `INVOICE_IMPORT` (scr 12) | `PURCHASE_INVOICE` | C 323 | **ambiguous: 85 or 24** | **defect §1.4.4** — `rownum` decides today |
| 18 | `EXPENSE_TYPE_DT` (scr 30) | `EXPENSE_RECORDED` | **M** on `expenseTypeCode` | C 24 / **M** → 30 | legacy uses the generic parent account 3; per-type accounts need `MAPPING` |
| 19 | `EXPENSE_TYPE_DT` (scr 35) | `REVENUE_RECORDED` | C 24 / **M** → 30 | C 4 | — |
| 20 | `TRANSFER_SAFE` | `SAFE_TRANSFER` | C 24 | C 30 | accounts **hardcoded in PL/SQL** (`p_account_chart_fk_debit := 24`), not template-driven |
| 21 | `ASSETS` (scr 23) | `ASSET_REGISTERED` | C 236 ⚠ | C 32 ⚠ | **suspected defect §1.4.2** — posts to complaint-settlement accounts. Do not migrate until confirmed |
| 22 | `DEPRECIABLE` (scr 23) | `DEPRECIATION_RECORDED` | C 236 ⚠ | C 32 ⚠ | as #21 |

**Reading of this table:** 22 branches, of which **2 are dead**, **2 are suspected defects**, **2
are ambiguous**, and **11 of the remaining 16 need `MAPPING` for the cash/bank swap alone**. The
`MAPPING` gap is not an edge case — it blocks the majority of the mapping. That is why it is
Phase 0 in §5 and not an afterthought.

---

## 5. Implementation plan

### Phase 0 — close the `MAPPING` gap in FIN *(blocking)*

Nothing downstream can proceed without it (§4).

| Step | Touches | Verified by |
|---|---|---|
| Specify a FIN-owned mapping store: `(eventTypeCode, mappingKey, sourceValue) → accountCode` | `db-script-fin.md` via the factory feedback channel; a new Flyway `V<N+1>__fin_account_mapping.sql` | migration applies; `V22` untouched |
| Implement `MAPPING` in `EventTypeRuleDomain.resolveAccountCode`, replacing the `FIN-422` throw | `EventTypeRuleDomain.java`, `EventEntryService` | unit tests: `paymentMethod=CASH`→24, `CHEQUE`→30, unmapped→`FIN-404-ACCOUNT` |
| Seed mappings for `paymentMethod` and `expenseTypeCode` | migration | the 16 pairs of §1.2 resolve |

`db-script-fin.md` is the factory's to write — record this as an `api_doc_gaps` entry with
`type: ABSENT`, `resolution: OPEN` in `$GOV/modules/FIN/backend/execution-state.json` rather than
editing the plan (per `CLAUDE.md`).

**Also decide here:** whether FIN's rule lines need dimension tags to carry `orgCode`/`branchCode`
(§1.8). Legacy stamps `ORGANIZATION_FK`/`ORGANIZATION_SUB_FK` on every master. If FIN cannot, the
new entries lose branch attribution and the §4 comparison will diverge on every row.

### Phase 1 — queue infrastructure *(Oracle, additive)*

Create `ACCOUNTING_EVENT_QT` / `ACCOUNTING_EVENT_Q` (JSON payload, `sort_list = 'ENQ_TIME'`,
`max_retries = 5`, `retry_delay = 60`) and a `PKG_ACCOUNTING_EVENT.emit(...)` wrapper. Nothing
calls it yet. Verified by enqueue/dequeue of a synthetic message in a scratch session.

**Decide `sort_list` now** — it is immutable (§2.3).

### Phase 2 — the consumer *(new deployable)*

Spring Boot, Oracle JMS in, HTTP out, PostgreSQL inbox. No FIN code on its classpath. Verified
against a stubbed FIN: duplicate delivery produces one post, `5xx` retries, exception queue
receives a poison message.

### Phase 3 — emit in shadow *(Oracle, additive)*

Add `PKG_ACCOUNTING_EVENT.emit(...)` calls to each accounting trigger **without removing anything**.
Both paths now run: the trigger still writes `DAILY_RESTRICTIONS_*`, and a message is also
enqueued. One trigger at a time, starting with `TRIGGER_LOAN_PAYMENT_Daliy_TR` FL=7 (the highest
volume, the simplest mapping).

Every emit is inside the existing trigger body, so it inherits the transaction. **Risk to state
plainly:** an exception in `emit` would roll back the business DML, exactly as §1.4.3 describes
for the existing lookups. `emit` must therefore be wrapped in its own exception handler that
logs to `ERROR_LOG` and swallows — a missed event is recoverable in shadow mode; a customer who
cannot save a payment is not.

### Phase 4 — parallel run and comparison

Both paths write. FIN's entries are compared against `DAILY_RESTRICTIONS_*`, nothing is acted on.

**Comparison method:** a reconciliation query joining `FIN_JOURNAL_ENTRY.event_reference` to the
legacy master via the `sourceTable:sourcePk` embedded in the reference, asserting per entry:
same `docDate`, same total debit, same total credit, same account codes per side. Run daily; every
mismatch is a ticket.

**Exit criteria — coverage, not elapsed time** (§2.4):

- every branch in §4 that is not marked dead has been exercised **at least 20 times**;
- zero unexplained mismatches for 14 consecutive days;
- the one known unbalanced master (§1.4.5) is the *only* balance mismatch;
- the exception queue has been empty for 7 consecutive days;
- `#7`, `#14`, `#17`, `#21`, `#22` in §4 have documented business answers.

Branches that cannot be exercised naturally must be exercised deliberately in a copy of
production, or they do not count.

### Phase 5 — cutover, one event type at a time

Per event type, in ascending volume order (rarest first, `FL=7` last): disable the accounting
part of its trigger (§6), leave the emit. FIN becomes the book of record for that type.

**Rollback:** re-enable the trigger, stop the consumer, delete the FIN entries created in the
window (they are identifiable by `event_reference`). This is why nothing is dropped in this
phase — rollback must stay a one-line `ALTER TRIGGER … ENABLE`.

### Phase 6 — retire the stored balances

Only after Phase 5 completes for every type: repoint `GET_PREVIOUS_SAFE` / `GET_PERIVIOUS_SAFE`
readers at FIN-derived balances. Low risk — §1.5 establishes the stored values are already stale.

### Phase 7 — remove the 2025 framework

Disable `JOB_PROCESS_ACCOUNTING_QUEUE` (§1.6). This can happen **at any time** — it is enabled,
firing every 5 minutes, and has one configured rule. It should arguably be Phase 0.5, before it
processes something unexpected.

---

## 6. Retirement list

Disable, never delete. In this order, rarest branch first.

| Order | Trigger | Disable | Keep |
|---|---|---|---|
| 1 | `TRIGGER_EXPENSE_TYPE_DT_OLD` | whole trigger | — (`TRIGGER_EXPENSE_TYPE_DT` already disabled) |
| 2 | `TRIGGER_TRANSFER_SAFE_Daliy_TR` | whole trigger | — |
| 3 | `TRIGGER_ASSETS_Daliy_TR` | whole trigger | — **hold until §1.4.2 is answered** |
| 4 | `TRIGGER_DEPRECIABLE_Daliy_TR` | whole trigger | — **hold until §1.4.2 is answered** |
| 5 | `TRIGGER_INVOICE_IMPORT_Daliy_TR` | whole trigger | `TRIGGER_INVOICE_IMPORT` (`ITEMS_DT`) — **untouched** |
| 6 | `TRIGGER_COMPLAINT_DT_Daliy_TR` | whole trigger | `TRIGGER_COMPLAINT_DT_EXP_DT` — **untouched** |
| 7 | `TRIGGER_COMPLAINT_Daliy_TR` | whole trigger | — |
| 8 | `TRIGGER_CONTRACT_Daliy_TR` | whole trigger | `TRIGGER_CONTRACT_ARREAR`, `TRIGGER_ARREARS_CONTRACT_FL` (`ARREARS`) — **untouched** |
| 9 | `TRIGGER_LOAN_PAYMENT_Daliy_TR` | whole trigger | `TRIGGER_LOAN_PAYMENT_ORG_SUB`, `COMPOUND_TRIGGER_LOAN_PAYMENT_ORG_SUB` — **untouched** |
| 10 | `TRIGGER_LOAN_PAYMENT_Daliy_TR_AFTER` | whole trigger | — (journal totals; meaningless once nothing writes the journal) |
| 11 | `TRIGGER_DAILY_RESTRICTIONS_DT`, `_DT_BEFOR`, `_MASTER` | last | these maintain the legacy journal itself — disable only when it is frozen |
| — | `TRIGGER_LOAN_PAYMENT`, `TRIGGER_COMPLAINT_DT` | **already DISABLED** — leave alone | they are the `SAFE`/`BANK_ACCOUNT` writers (§1.5) |

Because the accounting is already in dedicated triggers (§1.1), **every row above is a whole-trigger
`ALTER TRIGGER … DISABLE`** — no trigger needs to be rewritten to split accounting from business
logic. The `Keep` column lists the sibling triggers on the same table that must not be touched.

Nothing is dropped, no column removed, no `DAILY_RESTRICTIONS_*` row deleted. The legacy journal
becomes read-only history.

---

## 7. Open questions

| # | Question | Why it blocks | What would close it |
|---|---|---|---|
| **OQ-1** | Is production Oracle Free/Standard or Enterprise, and is TxEventQ available? | decides TxEventQ vs classic AQ (§2.1) | `v$version` + edition on the production host |
| **OQ-2** | Are the production trigger bodies identical to the ones read here? | the whole of §1 assumes so | diff `user_triggers.trigger_body` against this investigation |
| **OQ-3** | **`MAPPING` is unimplemented in FIN** and 11 of 16 live branches need it | blocks Phase 1 onward | factory specifies the mapping store (§5 Phase 0) |
| **OQ-4** | No service-account concept in SEC; 1-hour tokens validated against `ActiveSession` | the consumer is a daemon using a human credential | SEC decides: long-lived service principal, or documented refresh |
| **OQ-5** | `existsByEventReference` is check-then-act; the unique constraint surfaces as `DataIntegrityViolationException`, not `FIN-409` | only matters with a concurrent consumer | map the constraint violation to `FIN-409-DUPLICATE-EVENT` |
| **OQ-6** | `ASSETS`/`DEPRECIABLE` post to screen 23 = complaint-settlement accounts (§1.4.2) | reproduce the behaviour, or fix it? | business/accounting confirmation |
| **OQ-7** | `FL=49` uses `FL=59`'s template and posts a `NULL` description (§1.4.1); 666 rows affected | is 49 a distinct event? | business confirmation |
| **OQ-8** | Screen 12 (purchase invoice) has two credit templates, resolved by `rownum` (§1.4.4) | the credit account is genuinely undefined | accounting decides: 85 الموردين or 24 الصندوق, by what condition |
| **OQ-9** | What distinguishes `COMPLAINT_FL = 0` from `= 1`? | both map to the same accounts; may be one event or two | read the ADF screen, or ask the business |
| **OQ-10** | `FL=8` and `FL=130` have zero rows | build event types for dead branches? | business: revived or retired |
| **OQ-11** | Can FIN carry `orgCode`/`branchCode` on rule-driven lines? | legacy stamps org on every master; without it the Phase 4 comparison diverges everywhere | FIN dimension support on `ENT-FIN-010` |
| **OQ-12** | What reads `GET_PREVIOUS_SAFE` / `GET_PERIVIOUS_SAFE`? | Phase 6 needs the caller list; ADF forms are not in this database | search the ADF application source |
| **OQ-13** | The single unbalanced master (§1.4.5) | FIN would reject it; the comparison must expect it | accounting reviews that one entry |

---

### Appendix — how the facts above were obtained

Read-only queries through node-oracledb thin mode against `LOAN_SYS@localhost:1521/FREEPDB1`:
`v$version`, `user_triggers` (29 rows, bodies dumped), `user_dependencies`, `user_tables`,
`user_tab_columns`, `user_objects`, `user_scheduler_jobs`, `user_queues`, `user_queue_tables`,
`user_role_privs`, `user_sys_privs`, plus counts and joins over `DAILY_RESTRICTIONS_MASTER`,
`DAILY_RESTRICTIONS_DT`, `ACCOUNTS_CHART`, `PRV_SCREENS`, `SYSTEM_TRANSACTION`,
`SYSTEM_TANSACTION_DT`, `LOAN_PAYMENT`, `SAFE`, `BANK_ACCOUNT`, `ACCOUNTING_*`.

No DDL, no DML, no `ALTER`, no trigger enabled or disabled.

**Sources cited in §2:**
[Oracle AQ programmatic interfaces](https://docs.oracle.com/cd/E11882_01/server.112/e11013/aq_envir.htm) ·
[Oracle AQ/TxEventQ](https://www.oracle.com/database/advanced-queuing/) ·
[TxEventQ announcement](https://blogs.oracle.com/database/oracle-transactional-event-queues-txeventq-scalable-messaging-streaming-in-the-database) ·
[DBMS_AQADM](https://docs.oracle.com/en/database/oracle/oracle-database/19/arpls/DBMS_AQADM.html) ·
[AQ administrative interface](https://docs.oracle.com/en/database/oracle/oracle-database/21/adque/aq-administrative-interface.html) ·
[Outbox/inbox delivery guarantees](https://event-driven.io/en/outbox_inbox_patterns_and_delivery_guarantees_explained/) ·
[AWS transactional outbox](https://docs.aws.amazon.com/prescriptive-guidance/latest/cloud-design-patterns/transactional-outbox.html) ·
[Inbox pattern](https://adhdecode.com/message-queues/outbox-pattern-and-cdc/inbox-pattern/) ·
[Parallel-run playbook](https://www.easy.bi/blog/parallel-run-legacy-migration-playbook/) ·
[Azure strangler fig](https://learn.microsoft.com/en-us/azure/architecture/patterns/strangler-fig)
