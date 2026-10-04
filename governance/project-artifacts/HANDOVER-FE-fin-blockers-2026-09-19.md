# Handover to frontend — FIN: answers to the pre-F1 blocker prompt

**Date:** 2026-09-19 · **Module:** FIN · **Repo:** `backend` · **Branch:** `main`
**Answers:** `BACKEND PROMPT — FIN: gaps that block the frontend implementation`
**Contracts touched:** no existing endpoint, request shape, response field, status code or error
code was changed. One new endpoint and one widened response, both additive. Two new filter fields
on existing searches, both optional.
**Companion:** `HANDOVER-FE-fin-plan-edits-2026-09-19.md` — the same changes expressed as an edit
list against `srs-fin.md`, `ADR-FIN-006`, the ADR-FIN-002 binding annex and your execution plan.
Read this document for what the backend does; read that one for what to change in the plan.

---

## TL;DR

| Your ask | Status | What you get |
|---|---|---|
| §1 — a rule's lines cannot be read back | **Done, your option 1** | `EventTypeRuleResponse.lines[]` + `lineCount`, on search and on every write response |
| §2 — a fiscal year cannot be listed | **Done** | `POST /api/v1/fin/fiscal-years/search` → paginated `FiscalYearResponse` |
| §3(a) — publish the filterable/sortable fields | **Done** | Named per endpoint in the OpenAPI description, so api-docs carries it |
| §3 — `parentAccountId` / `dimensionId` / `fiscalYearId` | **All three yes** | `dimensionId` already worked; `parentAccountId` and (on journal entries) `fiscalYearId` added |
| §3(b) — unknown field must 400, never pass silently | **Done, platform-wide** | `400 VALIDATION_ERROR` with one `fieldError` per offending field |
| §4 — five endpoints carry no contract id | **Cannot fix here** | The registry is the factory's file; recorded for them, now seven endpoints |
| §5 — confirm the v1 exclusions | **Confirmed against the delivered surface** | None of them is in the registry; whether the plan should demand them is the factory's call |
| §6(a) — the twelve page codes, two custom actions | **Confirmed** | All twelve seeded by V24, both actions registered |
| §6(b) — the 13 lookup keys | **Confirmed** | V26 seeds all 13 type rows; the eleven non-host keys carry their values |
| §7 — the smaller items | **Answered** | Two of them changed our mind about the account tree; the rest recorded, not acted on |

Neither §1 nor §2 needed a new screen, and nothing below asks you to change a shape you already
parse.

---

## 1. An event-type rule now carries its lines

We took your **option 1**. `EventTypeRuleResponse` gains `lines[]` and `lineCount`, exactly the way
`RecurringTemplateResponse.lines` and `AllocationRuleResponse.targets` already behave — which is
also why we did not add a by-id read instead: making this aggregate behave like the other two keeps
ADR-FIN-006's reasoning intact rather than carving out an exception to it.

**No new contract id.** This is a widened response on endpoints you already bind:

```
POST /api/v1/fin/event-rules/search      (API-FIN-009)  → each row now carries its lines
POST /api/v1/fin/event-rules             (API-FIN-010)  → lines: [] (a new rule has none yet)
PUT  /api/v1/fin/event-rules/{id}/deactivate            → the rule with its lines
```

```jsonc
// POST /api/v1/fin/event-rules/search — one row
{
  "eventTypeRulePk": 12,
  "eventTypeCode": "SALES_INVOICE",
  "nameAr": "فاتورة مبيعات",
  "nameEn": "Sales invoice",
  "isActiveFl": true,
  "lineCount": 3,
  "lines": [
    {
      "ruleLinePk": 41, "lineNo": 1,
      "accountDerivationTypeCode": "CONSTANT", "accountDerivationValue": "1101",
      "amountSourceTypeCode": "FIELD",       "amountSourceValue": "totalAmount",
      "directionCode": "DEBIT",
      "distributionTypeCode": "FIXED",       "isRemainderFl": false
    }
    // … ordered by lineNo
  ],
  "createdAt": "…", "createdBy": "…", "updatedAt": "…", "updatedBy": "…"
}
```

All seven `ENT-FIN-010` fields are there, so SCR-FIN-003's Detail pane binds to `lines`, and you can
check `RULE-FIN-003` (exactly one `isRemainderFl: true` per rule) client-side before you submit —
the server still decides, with `FIN-409-REMAINDER-COUNT`.

`API-FIN-011` (add a line) is untouched and still returns the single created line.

**Cost note, since it is yours to pace:** the lines for a whole page are fetched in one batch query
keyed by the page's rule ids, not one query per row. A large `size` on this search is therefore one
extra query, not N.

---

## 2. Fiscal years can now be listed

```
POST /api/v1/fin/fiscal-years/search
Authorization: Bearer <caller holding PERM_FIN_PERIODS_VIEW>

{ "filters": [{"field": "statusCode", "operator": "EQUALS", "value": "OPEN"}],
  "sortField": "startDate", "sortDirection": "DESC", "page": 0, "size": 20 }
```

Same envelope, same pagination shape, same `FIN-400-INVALID-SORT` as every other FIN search. Rows
are `FiscalYearResponse`:

```jsonc
{
  "fiscalYearPk": 3,
  "code": "2026",
  "startDate": "2026-01-01",
  "endDate": "2026-12-31",
  "statusCode": "OPEN",        // FISCAL_YEAR_STATUS
  "isActiveFl": true,
  "periodCount": 12,
  "periods": [],               // deliberately empty here — see below
  "createdAt": "…", "createdBy": "…", "updatedAt": "…", "updatedBy": "…"
}
```

That is all five attributes SCR-FIN-007's Master column needs, and it populates the required
`fiscalYearId` selector on SCR-FIN-010 (API-FIN-030) and SCR-FIN-011 (API-FIN-031). You can stop
discovering year ids from `FiscalPeriodResponse.fiscalYearId`.

### Three things worth knowing

- **`periods` is empty on a search row, by design.** `periodCount` is real (a DB-side count), but
  the period set of every year on the page would be a second unbounded read. SCR-FIN-007's Detail
  pane already has the right endpoint for it — the fiscal-period search scoped by `fiscalYearId`.
  It is the same division `POST /journal-entries/search` makes against `API-FIN-022`. On the 201 of
  `API-FIN-023` (create) `periods` is still fully populated, as it always was.
- **`PERM_FIN_PERIODS_VIEW` gates it**, the same permission the fiscal-period search carries; this
  endpoint exposes no field those rows did not already reach. Incidentally this settles an open
  note in `V24__fin_security_seed.sql`, which recorded `FIN_PERIODS / VIEW` as the one ✓ in the
  matrix with no `API-FIN-` beside it and asked whether a read endpoint was missing. It was.
- **No `GET /fiscal-years/{id}` was added.** You said the list, not the by-id read, is what
  unblocks you, and every attribute the by-id read would return is on the search row.

**It has no contract id yet** — same situation as logout had in SEC last round. See §4.

---

## 3. Which fields each search actually filters and sorts on

### (a) It is now published

Every FIN `SearchRequest` names, in its OpenAPI description of `filters[].field` and `sortField`,
exactly the fields that endpoint admits, the operators each accepts, and the lookup key behind each
coded field. Once api-docs are regenerated it lands in the Description column, so you can cite it
instead of guessing. Nothing about the envelope changed.

### (b) An unknown field is now a 400, not a silent drop

This was fixed platform-wide the same day, for the identical SEC defect you reported
(`backend-prompt-sec-search-filters-2026-09-19.md`). `SpecBuilder` now rejects the whole request
rather than skipping the filter:

```jsonc
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "…",
    "fieldErrors": [{ "field": "nonsense", "message": "Filter field 'nonsense' is not supported…" }]
  }
}
```

FIN's eight searches — now nine — are covered by that, as is SEC. "Filter matched everything" and
"filter was discarded" are no longer indistinguishable on the wire.

**Lifted fields are `EQUALS`-only, and say so.** A field the endpoint lifts out of the generic set
(`parentAccountId`, `fiscalYearId`, `periodId`, `dimensionId`, `sourceAccountId`) is read for its
*value* and the service supplies its own equality predicate. Sending one with any other operator,
or with an `IN` list, is a `400` carrying `UNSUPPORTED_FILTER_OPERATOR` on that field.

An earlier draft of this document said such a request was silently treated as `EQUALS`. That was
true when it was written and is worth correcting rather than quietly fixing, because the old
behaviour was worse than "treated as EQUALS" in two cases a review caught the same day:
`NOT_EQUALS parentAccountId=1` returned the rows whose parent *is* 1 — the exact inverse of the
request — and `IN [1,4]` dropped the filter altogether, so a node's children pane would have
rendered the entire chart of accounts. Both now answer 400.

### The table you asked for

Your three bold, structural asks first, because you asked us to answer those first:

| Field | Endpoint | Answer |
|---|---|---|
| **`parentAccountId`** | `POST /fin/accounts/search` | **Added.** Was genuinely absent; it is now an `EQUALS` filter. |
| **`dimensionId`** | `POST /fin/dimensions/values/search` | **Already supported**, no change needed. |
| **`fiscalYearId`** | `POST /fin/fiscal-periods/search` | **Already supported**, no change needed. |

And the rest of your table, field by field:

| Endpoint | Verdict |
|---|---|
| `POST /fin/accounts/search` | `code` LIKE ✓ · `nameAr`/`nameEn` LIKE ✓ · `accountTypeCode` ✓ · `isActiveFl` ✓ · `parentAccountId` ✓ **(new)**. Also available: `natureCode`, `isLeafFl`, `accountPk`, `createdAt`. |
| `POST /fin/dimensions/search` | `code` LIKE ✓ · `nameAr`/`nameEn` LIKE ✓. Also `isActiveFl`, `dimensionPk`, `createdAt`. |
| `POST /fin/dimensions/values/search` | `dimensionId` ✓ · `code` LIKE ✓ · `isActiveFl` ✓. Also `nameAr`/`nameEn`, `sortOrder`, `createdAt`. |
| `POST /fin/event-rules/search` | `eventTypeCode` ✓ · `nameAr`/`nameEn` LIKE ✓ · `isActiveFl` ✓. |
| `POST /fin/recurring-templates/search` | `nameAr`/`nameEn` LIKE ✓ · `scheduleTypeCode` ✓ · `isActiveFl` ✓. Also `frequencyCode` and the three dates. |
| `POST /fin/allocation-rules/search` | `nameAr`/`nameEn` LIKE ✓ · `sourceAccountId` ✓ · `isActiveFl` ✓. |
| `POST /fin/journal-entries/search` | `docNo` LIKE ✓ · `docDate` GTE+LTE range ✓ · `periodId` ✓ · `statusCode` ✓ · `journalTypeCode` ✓ · `eventReference` ✓ · `fiscalYearId` ✓ **(new)**. |
| `POST /fin/fiscal-periods/search` | `fiscalYearId` ✓ · `statusCode` ✓. Also `periodNo`, `nameAr`/`nameEn`, `startDate`/`endDate`. |

Two notes on that table:

- On journal entries, `fiscalYearId` is the entry's **own** column, not a walk through
  `period.fiscalYear`. Filtering by year and by period are independent; send both and you get the
  intersection.
- Date bounds are ISO `yyyy-MM-dd`. On recurring templates the three date fields were on the
  whitelist but would have compared a string to a date column; that is fixed, so what the docs
  advertise is now what the endpoint accepts.

---

## 4. The five (now seven) endpoints without a contract id — we cannot fix this here

You are right about the mapping, and about the workaround being a workaround. We cannot apply it:
`registry-exec-be-fin.md` is the factory's file and the shared repo's `CODEOWNERS` refuses a change
to it from this repo. Recorded for the factory as `HUMAN` in
`modules/FIN/backend/execution-state.json`, with ADR-FIN-002's mapping quoted verbatim
(033 = fiscal-period search, 034 = event-rule deactivate, 035 = dimension-value deactivate,
036 = recurring-template deactivate, 037 = allocation-rule deactivate).

The list is now **seven**, not five: the fiscal-year search of §2 also needs an id.

Worth knowing while you wait: this backend's own source already *names* `API-FIN-033` and
`API-FIN-034` in its javadoc, on the strength of your annex. The numbering is in use whether or not
the registry records it, which is an argument for ratifying it rather than assigning different ids.

---

## 5. The v1 exclusions — confirmed against what is delivered

Every one of them is absent from `registry-exec-be-fin.md`, and no `REQ-FIN-*` demands any of them.
So their absence is the plan's shape, not something dropped in implementation:

- no update endpoint for a recurring/reversing template (SCR-FIN-004);
- no update endpoint for an allocation rule (SCR-FIN-005);
- no by-id read on account, template, allocation rule or fiscal year;
- no `DELETE` on a rule line, template line or allocation target;
- no deactivate on the parent `Dimension` (only on its values).

Building any of them here would be an unrequested endpoint, not a fix — so we did not. Draw no Edit
affordance on SCR-FIN-004 and SCR-FIN-005; deactivate-and-recreate is the delivered path, and your
plan to say so on screen matches the backend exactly.

Whether the **plan** should have demanded them is the factory's call, and is filed for them.

---

## 6. Runtime prerequisites — both confirmed

**(a) The twelve page codes are exactly right.** `V24__fin_security_seed.sql` registers the FIN
module and all twelve screens under precisely the codes you listed:

```
FIN_ACCOUNTS · FIN_DIMENSIONS · FIN_RULES · FIN_RECURRING_TEMPLATES · FIN_ALLOCATION_RULES
FIN_JOURNAL_ENTRIES · FIN_PERIODS · FIN_ACCOUNT_LEDGER · FIN_TRIAL_BALANCE
FIN_BALANCE_SHEET · FIN_INCOME_STATEMENT · FIN_DIMENSION_REPORTS
```

Both custom actions are registered too — `PERM_FIN_JOURNAL_ENTRIES_REVERSE` and
`PERM_FIN_PERIODS_CLOSE_APPROVE` — and the server stays the authority on both writes, answering
`FIN-403-FORBIDDEN`.

One thing your route guard should expect: `PERM_FIN_PERIODS_CLOSE_APPROVE` is deliberately **not**
granted the way the others are. `V25` granted SYS_ADMIN every FIN action except that one
(RULE-FIN-015 forbids one holder of both close-approve and entry creation); `V27` then mints a
dedicated `FIN_CLOSE_APPROVER` role that holds it, and `V30` additionally grants it to SYS_ADMIN.
So on a fresh deployment the screen is reachable and the action is held by a specific role — do not
assume every user who can see FIN_PERIODS can approve a close.

**(b) All 13 lookup keys are seeded by `V26__fin_mdl_lookup_seed.sql`** — every key has its type
row, and the eleven non-host keys carry their value sets:

| Key | Values seeded |
|---|---|
| `ACCOUNT_TYPE` | 5 — ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE |
| `DEBIT_CREDIT` | 2 — DEBIT, CREDIT |
| `PERIOD_STATE` | 4 — OPEN, SOFT_CLOSE, HARD_CLOSE, YEAR_END_CLOSE |
| `FISCAL_YEAR_STATUS` | 2 — OPEN, CLOSED |
| `JOURNAL_TYPE` | 5 — EVENT_GENERATED, MANUAL, RECURRING, ALLOCATION, REVERSAL |
| `JOURNAL_STATUS` | 3 — DRAFT, POSTED, VOID |
| `ACCOUNT_DERIVATION_TYPE` | 3 — CONSTANT, DIRECT, MAPPING |
| `AMOUNT_SOURCE_TYPE` | 3 — FIELD, PERCENTAGE, REMAINDER |
| `DISTRIBUTION_TYPE` | 3 — FIXED, PERCENTAGE, REMAINDER |
| `RECURRING_SCHEDULE_TYPE` | 2 — RECURRING, REVERSING |
| `RECURRING_FREQUENCY` | 4 — WEEKLY, MONTHLY, QUARTERLY, ANNUALLY |
| `ACCOUNTING_EVENT_TYPE` | type row only — host-defined, legitimately empty |
| `PAYMENT_METHOD` | type row only — unused in v1 |

Note `JOURNAL_TYPE` does **not** include `CLOSING` or `OPENING`. They exist only as internal
constants used by year-end close, so a caller can never name them — which is also why a manual entry
sent with `journalTypeCode: "CLOSING"` answers `400 FIN-400-INVALID-LOOKUP` rather than the 409 you
might expect. That mismatch is already recorded and escalated on our side.

**(c) `GET /api/v1/mdl/lookups` carrying no contract id** is MDL's registry, not FIN's. Recorded for
the factory alongside §4; we changed nothing.

---

## 7. The smaller items

- **The chart of accounts.** `parentAccountId` (§3) is now a supported filter, so the lazy
  per-level expand is the shape we would rather support than an unpaginated tree read: one search
  per expanded node, scoped to its children. `PageableBuilder` still caps `size` at 200 and defaults
  to 20. We are not adding an unpaginated endpoint unless a real tenant's account count forces it —
  tell us if it does.
- **`FIN-503`** — agreed, dead row. It was already recorded as struck on our side (the platform has
  no 503-capable `Status`, and cross-module calls here are in-process, so "MDL unreachable" has no
  producer). Removing it from the catalog is the factory's edit; it will never be emitted.
- **`FIN-403-SOD-VIOLATION`** — confirmed unreachable. No objection to dropping it from your message
  catalog. We did not remove the registered code here: deleting a published code from this side
  would be a silent contract change.
- **`JOURNAL_STATUS.VOID`** — confirmed seeded and confirmed unreachable through classic reversal;
  rendering it defensively is the right call.
- **The red P3.1 gate** — both findings are the factory's files (`manifest.json → plans.backend/test`
  pointing at a path that resolves to nothing, and the 30-vs-31 finding count). Recorded, not
  touched.

---

## 8. What changed in this repo

Java, all under `src/main/java/com/erp/`:

| Area | Files |
|---|---|
| §1 rule lines | `fin/dto/EventTypeRuleResponse`, `fin/mapper/EventTypeRuleMapper`, `fin/service/EventTypeRuleService`, `fin/repository/RuleLineRepository` |
| §2 fiscal-year search | `fin/dto/FiscalYearSearchRequest` (new), `fin/mapper/FiscalYearMapper`, `fin/service/FiscalYearService`, `fin/controller/FiscalYearController` |
| §3 filters | `fin/dto/AccountSearchRequest`, `fin/dto/JournalEntrySearchRequest`, `fin/service/AccountService`, `fin/service/JournalEntryService`, `fin/service/RecurringTemplateService` |
| §3(a) published field sets | all nine `fin/dto/*SearchRequest` |
| §3(b) 400 on unknown field | `common/search/SpecBuilder` — platform-wide, shared with SEC |

`§1`, `§2`, `§3` and `§4`–`§7` are all recorded in
`modules/FIN/backend/execution-state.json → api_doc_gaps`, which is the channel the factory reads:
four as `RESOLVED`, three as `HUMAN`.

No migration was added. No existing endpoint changed shape.

**api-docs are not yet regenerated.** They are produced from the running app, and the instance on
this machine belongs to another session; regenerating means restarting it. Until that happens the
published `modules/FIN/api-docs/` still describes the pre-2026-09-19 surface — this document is
ahead of it. Everything above is read off the delivered source, which compiles and passes the
ArchUnit module-boundary suite.
