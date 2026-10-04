# Handover to frontend — FIN: the plan edits today's backend changes force

**Date:** 2026-09-19 · **Module:** FIN · **Repo:** `backend` · **Branch:** `main`
**Companion to:** `HANDOVER-FE-fin-blockers-2026-09-19.md` — that document says what the backend
now does; **this one is the edit list**: which lines of which governance artifacts are now false,
and what each should say instead.

> **What I could and could not read.** `srs-fin.md`, `registry-srs-fin.md`, `db-script-fin.md`,
> `manifest.json` and `_inputs/api-docs-binding-fin.md` are in this checkout, so every quotation
> below is copied from the file and carries its line number. **`frontend-execution-plan-fin.md`
> (P3.2), `packages/frontend-execution/` and the `ADR-FIN-*` records are NOT** — they live in your
> repo. For those I describe the change by screen and by decision, not by line. Treat §4 as
> "these screens are affected"; you hold the anchors.
>
> **None of these files is mine to edit.** `analysis/` is read-only to this repo and the shared
> `CODEOWNERS` enforces it; the frontend partition is yours. Everything below is a request, made in
> the one channel that reaches the factory — see §7.

---

## 0. The edit list at a glance

| # | File | What is now false | Severity |
|---|---|---|---|
| 1 | `_inputs/api-docs-binding-fin.md` | 37 endpoints, 5 unbound ids | Blocks nothing; goes stale on next fetch |
| 2 | `srs-fin.md` SCR-REQ-FIN-007 §B1 + §B5 | "There is NO fiscal-YEAR search"; "KNOWN GAP" | **Contradicts the running backend** |
| 3 | `srs-fin.md` SCR-REQ-FIN-003 §B1 + §B5 | Rule lines have no read path | **Contradicts the running backend** |
| 4 | `srs-fin.md` SCR-REQ-FIN-001 §B2, SCR-REQ-FIN-006 §B2 | Filter lists are short by one field each | Understated, not wrong |
| 5 | `ADR-FIN-006` | Accepts two workarounds that are no longer needed | Supersede, don't delete |
| 6 | `frontend-execution-plan-fin.md` + `packages/frontend-execution/` | Four screens planned around absent endpoints | **Plan-level rework, §4** |
| 7 | Everywhere | A new client-error path (400 on an unknown filter field) | New, must be handled |

---

## 1. `_inputs/api-docs-binding-fin.md` — one new row, and a changed count

The annex is ADR-FIN-002's, written by the factory and explicitly *not* published by this backend,
so I did not touch it. It now undercounts:

- Header, lines 13-14: *"Five of the 37 — API-FIN-033..037 — are not yet in `registry-exec-be-fin.md`'s
  `API-FIN-001..032` range"*.
- Footer, line 58: *"Coverage: 37 planned ids ↔ 37 published endpoints"*.

As of today the backend serves **38**, and **six** carry no registry id. Add the row:

```
| API-FIN-038 | search fiscal years | POST `/api/v1/fin/fiscal-years/search` | ✓ (not yet in registry-exec-be range — ADR-FIN-002) |
```

`API-FIN-038` is a **suggestion, not a fact** — the number is the factory's to assign, exactly as
033..037 still are. Bind it to whatever id they issue. Nothing in the backend depends on the
number; it depends only on there being one.

---

## 2. `srs-fin.md` SCR-REQ-FIN-007 — two passages are now factually wrong

This is the most important edit in the document, because both passages assert an absence that no
longer holds, and the second one reasons at length from it.

**(a) §B1 Operations, `srs-fin.md:1484-1486`:**

> `Operations   : create (year); search — PERIODS only (API-FIN-033); open, soft-close, hard-close`
> `               (period); run year-end close. There is NO fiscal-YEAR search and no by-id read of`
> `               either a year or a period — see the note under B5`

There is now a fiscal-year search. Suggested replacement:

> `Operations   : create (year); search (years, and periods via API-FIN-033); open, soft-close,`
> `               hard-close (period); run year-end close. There is still no by-id read of either a`
> `               year or a period — see the note under B5`

**(b) §B5, the closing paragraph at `srs-fin.md:1544-1550`:**

> *"\*Fiscal-year search\* is recorded as a KNOWN GAP rather than a reasoned exclusion — no REQ or
> AC asks for one … and it is not blocking, because `FiscalPeriodResponse` carries `fiscalYearId`
> … so an unfiltered API-FIN-033 call discovers year ids indirectly. It is a gap and not a defect
> for that reason alone: the discovery path is awkward (read a year id off any period row) but it
> exists …"*

That paragraph should be rewritten as **closed**, not edited in place — the whole argument it makes
is now history. Something like:

> **The fiscal-year search was built on 2026-09-19** (`POST /api/v1/fin/fiscal-years/search`,
> `FiscalYearController.search` → `FiscalYearService.search`), gated by `PERM_FIN_PERIODS_VIEW` —
> the same row API-FIN-033 uses, so again no migration. It answers a paginated
> `FiscalYearResponse` carrying `code`, `startDate`, `endDate`, `statusCode`, `isActiveFl` and the
> DB-side `periodCount`; `periods` is empty on a search row, because the period set is read through
> API-FIN-033 scoped by `fiscalYearId`. It was recorded here as a KNOWN GAP on the grounds that
> year ids were discoverable off any period row — true, but that discovery path yields ids and not
> one displayable attribute, which is why SCR-REQ-FIN-007's Master pane and the required
> `fiscalYearId` inputs of API-FIN-030/031 could not be driven by a user. *By-id read* remains a
> deliberate v1 exclusion on both resources, for the reason already stated: no field is reachable
> through it that the search rows do not already return.

Also add the row to the §B5 table:

```
| search fiscal years | POST | /api/v1/fin/fiscal-years/search | filters (code?, statusCode?, dates?), paging (request body) | Page\<FiscalYear\> | — | REQ-FIN-031 |
```

**One thing worth preserving from the old text:** its observation that
`PERM_FIN_PERIODS_VIEW` was "a gateway row with no endpoint behind it" until API-FIN-033. The same
note in `V24__fin_security_seed.sql` asked whether the matrix's `FIN_PERIODS / VIEW` ✓ was spurious
or whether a read endpoint was missing. Two endpoints now sit behind that row. The question is
settled and can be recorded as such.

---

## 3. `srs-fin.md` SCR-REQ-FIN-003 — the rule's lines are now readable

**(a) §B1 Operations, `srs-fin.md:1235-1236`:**

> `Operations   : search, create, deactivate (rule — API-FIN-034); create (line). As built there is`
> `               no update and no by-id read on either, and no line delete — see B4`

Still true as written — but it is now *misleading by omission*, because a reader concludes the
lines cannot be read. Suggested:

> `Operations   : search, create, deactivate (rule — API-FIN-034); create (line). The rule's lines`
> `               are returned with the rule by API-FIN-009/010/034, so no line read endpoint is`
> `               needed. There is still no update and no by-id read on either, and no line delete`
> `               — see B4`

**(b) §B4, the "Not built, and why" paragraph, `srs-fin.md:1263-1268`:** it says *"the rule has no
update and no by-id read endpoint, and the line has none of update, by-id read or delete."*
Accurate, and it should stay — but append the reason a by-id read is no longer wanted:

> A by-id read on the rule was considered and deliberately not built: `EventTypeRuleResponse` now
> carries `lines[]` and `lineCount`, so API-FIN-009 already returns everything a
> `GET /event-rules/{id}` would. That is the same shape `RecurringTemplateResponse.lines` and
> `AllocationRuleResponse.targets` have always had, and the same reason ADR-FIN-006 gives for
> dropping the by-id reads on those two — this aggregate now simply matches them.

**(c) §B5 table rows, `srs-fin.md:1272-1275`:** the Outputs column for *search rules*, *create rule* and
*deactivate rule* should read `EventTypeRule with lines` (or `Page\<EventTypeRule with lines\>`),
not bare `EventTypeRule`.

**(d) §B3, `srs-fin.md:1245-1248`:** it already names the seven `ENT-FIN-010` line fields and says
`RULE-FIN-003` "blocks saving until exactly one remainder line exists when a percentage line is
present". That is now checkable client-side before submit, because the sibling lines are on screen.
The server remains the authority (`FIN-409-REMAINDER-COUNT`) — say so if you add the precheck, so
nobody later mistakes the client check for the enforcement point.

---

## 4. The two filter lists that are now short

Both are understatements rather than errors — the screens work as written, they just have one more
field available than the document admits.

**SCR-REQ-FIN-001 §B2, `srs-fin.md:1147`:**

> `Filters: code(LIKE), nameAr/nameEn(LIKE), accountTypeCode(EXACT), isActiveFl(EXACT) — correspond to result columns.`

Add `parentAccountId(EXACT)`. Unlike the others it does **not** correspond to a result column — it
is the tree-scoping filter, so note it as such rather than adding a column for it.

**SCR-REQ-FIN-006 §B2, `srs-fin.md:1461`:**

> `Filters: docNo(LIKE), docDate(DATE_RANGE), periodId(EXACT), statusCode(EXACT), journalTypeCode(EXACT) — correspond to result columns.`

Add `fiscalYearId(EXACT)` and `eventReference(EXACT)`. `eventReference` was always filterable; it
is the field REQ-FIN-046's drill-down follows, and the list simply never named it.

---

## 5. `ADR-FIN-006` — supersede two acceptances, keep the rest

I cannot read the ADR, so treat this as "these two clauses changed", not as drafted text.

- **"Year ids discovered from the period rows' `fiscalYearId`"** — this workaround is retired. The
  decision it justified (no by-id read on a fiscal year) is *unchanged and still correct*; only its
  supporting mechanism changed, and for the better: the search returns the attributes the workaround
  could not.
- **"No fiscal-year search" as a v1 exclusion** — it is no longer excluded. If the ADR lists it
  among the exclusions, move it out and record the date.

**What must NOT change in ADR-FIN-006**, because the backend confirmed each one is deliberate and
none is coming in v1 (see `HANDOVER-FE-fin-blockers-2026-09-19.md` §5): no update on a recurring
template; no update on an allocation rule; no by-id read on account, template or allocation rule;
no `DELETE` on a rule line, template line or allocation target; no deactivate on the parent
`Dimension`. All five are absent from `registry-exec-be-fin.md`, so their absence is the plan's
shape. Keep drawing no Edit affordance on SCR-FIN-004 and SCR-FIN-005.

Note also that **ADR-FIN-002 is not superseded** — the binding annex is still needed, because the
registry still declares only `API-FIN-001..032`. It gets *longer* (§1), not retired. It can only be
retired once the factory issues the six missing ids.

---

## 6. `frontend-execution-plan-fin.md` and `packages/frontend-execution/` — the screen work

Four screens were planned around an absent endpoint, and each can now be planned around a present
one. In F-phase terms this is **F1/F2 scope** (the master/detail data binding), not a new screen and
not a new route.

**SCR-FIN-003 — Engine rules.** The Detail pane binds to `lines[]` on the row the Master pane
already holds. No second fetch, no by-id read, no "lines unavailable until you create one"
placeholder. Rendering a rule you did not create in this session now works. If the plan carries a
deferred task, a TODO or a reduced acceptance criterion for this pane, that is the task to close.

**SCR-FIN-007 — Fiscal periods & years.** The Master pane binds to the new fiscal-year search
instead of to ids scraped off period rows. The column that could only render a raw integer becomes
`code` / `startDate`–`endDate` / `statusCode` / `periodCount`. The Detail pane is unchanged — it
was already API-FIN-033 scoped by `fiscalYearId`, and that is still the right call, which is why
`periods` comes back empty on a search row.

**SCR-FIN-010 and SCR-FIN-011 — Balance sheet, Income statement.** Their required `fiscalYearId`
query parameter now has a selector source. These two screens go from *not drivable by a user* to
ordinary. Whatever the plan says about deferring them or hard-coding a year should come out.

**SCR-FIN-001 — Chart of accounts.** `parentAccountId` makes a lazy per-level expand possible: one
search per expanded node, scoped to its children, instead of paging the whole account set and
grouping client-side. This is the shape the backend would rather support, and it removes the
"a tree assembled from one page has missing branches" risk at 200 rows. **Your call on timing** —
client-side assembly still works and is not broken. If you keep it for v1, record *why*, so the
choice is not later mistaken for the old constraint.

**SCR-FIN-006 — Journal entries.** An optional year filter is now available alongside the period
filter. They are independent (`fiscalYearId` is the entry's own column, not a walk through the
period), so sending both narrows to the intersection. Purely additive; ignore it if the screen
does not want it.

**Cross-cutting: a new client-error path.** An unknown or unsupported `filters[].field` is now
`400 VALIDATION_ERROR` carrying one `fieldErrors` entry per offending field, instead of being
silently dropped:

```jsonc
{"success": false,
 "error": {"code": "VALIDATION_ERROR", "message": "Validation failed",
           "fieldErrors": [{"field": "nonsense",
                            "message": "This search does not support filtering on \"nonsense\""}]}}
```

Two consequences for the plan: (i) your error handling must not treat this as a server fault or a
retryable condition — it is a programming error in the request, and surfacing the `fieldErrors`
entry is the useful behaviour; (ii) any screen that builds a filter set dynamically must send only
fields the endpoint publishes. Every FIN search now names its own set in the api-docs Description
column for `filters[].field` and `sortField`, so the list is citable rather than guessable.

**One caveat, because it will bite otherwise:** a field the endpoint lifts out of the generic set —
`parentAccountId`, `fiscalYearId`, `periodId`, `dimensionId`, `sourceAccountId` — is `EQUALS`-only.
Any other operator, or an `IN` list, is a `400` carrying `UNSUPPORTED_FILTER_OPERATOR` on that
field. Build those filters with `EQUALS` and treat the 400 as a bug in your request, not a server
fault.

**What does not change anywhere:** no route, no page code, no permission, no existing request shape,
no existing response field, no status code, no error code. Your route guard, your twelve page codes
and your `PERM_*` handling are all untouched.

---

## 7. What I did on the backend side, and what I could not

**Edited here (this repo's own partition and tools):**

- `modules/FIN/backend/execution-state.json` — the seven gaps from your prompt, plus an eighth
  recorded today: the stale `srs-fin.md` passages of §2 and §3 above, filed so the factory sees them
  rather than discovering them at the next gate.
- `modules/FIN/api-docs/` — regenerated from the running app. 1 endpoint added, 10 updated, 27
  unchanged. The published field sets of §6 are in it.
- `governance/governance-tools/api-doc-generator/discovery.py` — a genuine bug: `default_output_dir`
  resolved against `paths.modules` (the analysis tree) instead of `tracks.backend.partition`, so the
  first regeneration reported all 38 endpoints as "added" and was about to write a second,
  empty-history copy of the docs beside the real ones. Fixed.

**Not edited, deliberately:** `srs-fin.md`, `registry-srs-fin.md`, `ADR-FIN-*`,
`_inputs/api-docs-binding-fin.md`, `registry-exec-be-fin.md`, `frontend-execution-plan-fin.md`,
`packages/frontend-execution/`. The first five are the factory's under `analysis/`, which this repo
reads and never writes; the last two are yours. A plan is not patched from the implementing side —
what implementation discovers goes into this repo's own `execution-state.json`, which
`gov.py feedback` reads, and the next version's gate stays open until each item is answered or
waived. That is the channel; §2 and §3 are in it now.

**Still blocked, and it is not ours:** the six endpoints with no contract id. Until
`registry-exec-be-fin.md` grows them, every api-docs regeneration will keep dropping the ids and
your binding annex stays load-bearing.
