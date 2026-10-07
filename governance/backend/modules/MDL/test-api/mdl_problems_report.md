# MDL legacy API suite — erp-core 1.2.0 re-run report

| | |
|---|---|
| Run date | 2026-10-05 |
| Target | erp-core 1.2.0 code @ newback `main` (app jar built from main), profile `dev`, fresh PostgreSQL 16 DB, PLATFORM tenant |
| Script | `backend/modules/MDL/test-api/test_mdl_apis.py` (adapted in place; original in git history) |
| Run | `ERP_BASE_URL=http://localhost:<port> ERP_BOOTSTRAP_ADMIN_PASSWORD=<pw> python test_mdl_apis.py` (optional `ERP_TENANT_CODE`, `ERP_ADMIN_USERNAME`, `MDL_RUN_REPORT=<path>` for the auto-generated per-run report) |

## 1. Baseline (original script, unmodified except BASE_URL)

**0 PASS / 0 FAIL: the script aborted before the first test** (exit 2):

```
MDL api-verify run — RUN_ID=199207, tier=FULL
FATAL: could not log in as admin/admin — aborting.
```

The original login request returned 400 `TENANT_REQUIRED`, because it sent no `X-Tenant-Code` header. With
the header added, `admin`/`admin` returns 401 `SEC-401-INVALID-CREDENTIALS`. Then only the login was adapted
(tenant header plus the bootstrap password from env). The next run was **30 PASS / 0 FAIL / 6 observations**.
None of the MDL behaviour assertions needed a change.

## 2. Classification

| Test / spot | Baseline outcome | Class | Reference |
|---|---|---|---|
| `login()` / `main()`: admin login | FATAL abort (400 `TENANT_REQUIRED`; 401 for admin/admin) | (a) INTENDED CHANGE | DEVIATIONS [05] Task 1 (`TenantResolutionFilter`, staff tenant from `X-Tenant-Code`); DEVIATIONS [05] Task 7 (`BootstrapAdminPasswordRunner`); core-test-plan TC-CORE-SEC-001 / TC-CORE-SEC-002 (admin/admin rejected) |
| Suite 0: grant MDL module/screens/actions to SYS_ADMIN | not reached; afterwards PASS via `SEC-409-GRANT-DUP` (already granted) | (a) INTENDED CHANGE (comments only; assertions unchanged) | docs/steps/04-report.md "Old → new mapping" (`V7__sec_seed.sql` contains ex-V21 `mdl_role_grants`); docs/steps/06-report.md (SYS_ADMIN super role, code-defined `MdlPermissions`); CHANGELOG |
| `main()`: report written to the relative path `governance/modules/MDL/test-api/...` | not reached | (b) TEST-DEFECT | The path depends on the working directory, does not exist in this repo layout and overwrote the curated report. Writing is now opt-in via `MDL_RUN_REPORT` |
| Suites 1–2 (all 21 MDL assertions, TC-MDL-001..013) | not reached; PASS after login fix | none (no change) | MDL P1 addendum §1: "No endpoint added, changed or removed". Tenant scoping (V10, P1 addendum §2) does not affect fixtures that stay in one tenant |

Counts: **(a) 2 · (b) 1 · (c) 0**.

## 3. Adaptations made (each marked `# erp-core 1.2.0:` in the script)

1. `BASE_URL` comes from env `ERP_BASE_URL` (default `http://localhost:7272`).
2. The hard-coded `admin`/`admin` is removed. The password comes from env `ERP_BOOTSTRAP_ADMIN_PASSWORD`, and the script aborts cleanly if it is unset. Username and tenant can be overridden (`ERP_ADMIN_USERNAME`, `ERP_TENANT_CODE`, default `PLATFORM`).
3. `X-Tenant-Code` is sent on every request.
4. Module docstring and Suite 0 docstring: the "SYS_ADMIN has no MDL grants" precondition gap is obsolete. Suite 0 is kept as an idempotent check. It never creates a grant now, so it never revokes one.
5. The run report is written only when `MDL_RUN_REPORT` is set.

No assertion was weakened. Every TC id and test intent is unchanged.

## 4. Final results (2 consecutive runs, fresh RUN suffix each)

| Run | RUN_ID | Result |
|---|---|---|
| 1 | 199282 | **30 PASS / 0 FAIL / 6 observations** (exit 0) |
| 2 | 199286 | **30 PASS / 0 FAIL / 6 observations** (exit 0) |

Suites: 0. Setup 9/0 · 1. LookupType 11/0 · 2. LookupValue 10/0.

Observations (Stage E, never pass/fail), all consistent with `docs/api-docs/mdl/index.md`:

- duplicate type key → 409 `MDL-409-TYPE-DUP`
- type key omitted → 400; key of 81 chars (max 80) → 400
- value code omitted → 400
- value under non-existent type id → 404 `MDL-404-TYPE`
- reorder with a foreign value id → 400 `MDL-400-REORDER-MISMATCH`

Seed catalog cross-check (PLATFORM, informational): the active lookup types are `NOTIF_CHANNEL`, `NOTIF_STATUS`,
`FILE_FILE_STATUS` and `FILE_FILE_TYPE`, with no FIN types (FIN was removed in step 01; a read of
`FIN_PAYMENT_METHOD` returns 404 `MDL-404-TYPE-KEY`). `NOTIF_STATUS` now includes `QUEUED` and
`SKIPPED_NO_PROVIDER`, and `NOTIF_CHANNEL` includes `IN_APP`. This matches the MDL P2 addendum / V13 §3a.

## 5. Suspected application defects (class c)

**None.** Every behaviour from the original analysis that this suite exercises still holds on erp-core 1.2.0.

## 6. Carried-over notes (unchanged from the original suite)

- TC-MDL-010: the plan says "sortOrder persisted as 3,1,2"; the implementation assigns the 0-based list position. The suite asserts the behavioural read order instead (passes).
- TC-MDL-004 and TC-MDL-009 step 2: when a type is deactivated, a consumer read answers 404 `MDL-404-TYPE-KEY`, not an empty 200 list. This is documented as RULE-MDL-004 in the api-docs consumer note.
- TC-MDL-014 (INT-XM, "SEC unreachable") is not exercised. XM-MDL-001 is an in-process bean call, so the failure cannot be induced over HTTP. It stays a GAP and is not reported as a PASS.
- Not covered by this legacy suite: the new 1.2.0 behaviours (tenant confinement across tenants, provisioning copy, optimistic lock, audit events). These are covered by newback `docs/test-api/core-test-plan.md`.
