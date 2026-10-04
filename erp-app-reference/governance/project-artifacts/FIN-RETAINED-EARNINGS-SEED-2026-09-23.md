# FIN — the Retained Earnings account is SEEDED, not settable (V34)

**Date:** 2026-09-23 · **Module:** FIN · **Repo:** `backend` · **Contracts touched:** none — no
endpoint, controller method, DTO field or service write path; regenerated api-docs shows no
endpoint diff.

## The defect

`POST /api/v1/fin/fiscal-years/{id}/year-end-close` (API-FIN-027) could never succeed on any
deployed database. `FiscalYearService.retainedEarningsAccount()` resolves its target through
`AccountRepository.findFirstByIsRetainedEarningsFlTrueOrderByAccountPkAsc()`, and no row was marked
— `select count(*) from fin_account where is_retained_earnings_fl` returned **0**.

`V23` added the column (`DBF-FIN-147`) and its partial unique index, but seeded no row. Every call
answered `FIN-404-ACCOUNT`, blocking the frontend's TC-FIN-084 / TC-FIN-089 / TC-FIN-090.

## Why the fix is a seed and not an endpoint

`analysis/modules/FIN/P2/db-script-fin.md` §1 row `DBF-FIN-147` and its §1 closing note settle it:
the column is *"seeded as data (migration/seed), never through an API"*, `AccountCreateRequest` /
`AccountUpdateRequest` exclude it deliberately, and *"a governed write endpoint would need a new
API id in the API Registry, which no decision authorises this version."* The frontend had filed
the blocker as an **ABSENT contract**; it is not one.

## What V34 does

`src/main/resources/db/migration/V34__fin_account_retained_earnings_seed.sql` — a `DO` block that

1. returns immediately if **any** row is already marked (a host that marked its own is left alone),
2. otherwise inserts one account and marks it: `code = 3200`, `الأرباح المحتجزة` /
   `Retained Earnings`, `EQUITY` / `CREDIT`, leaf, active, no parent, `created_by = 'SYSTEM'`.

⚠ The guard is *"is any row marked"*, never *"does this code exist"*: `UQ_FIN_ACCOUNT_RETAINED_EARNINGS`
is a **partial** unique index, so the code is irrelevant to the precondition, while
`UQ_FIN_ACCOUNT_CODE` would abort the insert on a taken code — hence the separate free-code search
(`3200`, then `3200-1`, …). No existing account is updated, deactivated or re-parented.

## Evidence

| Check | Result |
|---|---|
| Flyway | `Successfully applied 1 migration … now at version v34`; `V34: seeded Retained Earnings account with code 3200.` |
| Marked rows | exactly **1** — `account_pk 650`, `3200`, `EQUITY`/`CREDIT`, leaf, active, `SYSTEM` |
| API-FIN-027 live | **201 CREATED** with `JV-2150-000001` (CLOSING) and `JV-2151-000001` (OPENING) on a purpose-built FY 2150 → 2151 |
| Backend test | `FinYearEndCoverageIntegrationTest#yearEndClose_resolvesTheSeededRetainedEarningsAccountWithNoFixtureMarker` — marks and clears nothing, so it fails without the seed |
| Suite | `mvn test` → **62/62** (was 61) |
| api-docs | `--function review`: 0 added / 0 removed / 0 updated / 38 unchanged |

## What the factory should amend in the analysis

`analysis/**` is the factory's partition and was not touched; recorded in
`backend/modules/FIN/execution-state.json` → `api_doc_gaps` (`ABSENT` / `IMPLEMENTED`) instead.

- `P2/db-script-fin.md` is right that DBF-FIN-147 is seeded data but never says **which row** to
  seed — which is why every database shipped without one. A seeded-account row naming `3200` /
  `EQUITY` / `CREDIT` (db-script seed section, or FIN's deployment notes) would stop a future host
  rediscovering this as a 404.
- The frontend's matching ABSENT record can be closed against V34.
