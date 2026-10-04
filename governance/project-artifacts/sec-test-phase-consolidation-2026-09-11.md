# SEC — Test-Phase Consolidation (2026-09-11)

Human-readable summary of the `TEST-PLAN-BE` bookkeeping consolidation
recorded in `governance/modules/SEC/execution-state.json`'s `test_phases[]`
entry on this date. This doc is informational only — it does not drive
generation or execution; the JSON entry (specifically its `note` field) is
the durable, authoritative record. See `governance/modules/SEC/execution-state.json`.

## Why this exists

Three independent test mechanisms ran against SEC's 35 governed
`TC-SEC-<seq>` today, but before this pass only one of them (TestSprite)
was tracked in `execution-state.json`. This consolidates all three into one
honest, combined picture.

## The three mechanisms

| Mechanism | Location | Result against the 35 governed TCs |
|---|---|---|
| TestSprite | `governance/modules/SEC/testsprite/`, report at `reports/TEST-REPORT-SEC-backend-2026-09-11.md` | 4/35 PASS: TC-SEC-001, 002, 003, 006 |
| MODE-5 hand-written script | `governance/modules/SEC/test-api/test_sec_apis.py`, `sec_api_test_report.html` (Total: 69 · Passed: 67 · Failed: 2), `sec_problems_report.md` | 25/35 PASS: 001–006, 008(\*), 009, 010, 012–019, 021, 025–028, 030–032 |
| New JUnit integration tests | `src/test/java/com/erp/sec/SecCoverageIntegrationTest.java` (6 tests, `@Transactional` rollback, all passing) | 6/35 PASS: 007, 023, 024, 029, 034, 035 |

(\*) TC-SEC-008: MODE-5 substituted a never-issued token for a constructed
expired/used one, but exercises the same RULE-SEC-006 /
`SEC-409-RESET-TOKEN-INVALID` code path — counted as a real pass.

## Combined result: 31/35 real PASS, 4 genuinely open

| TC | Disposition | Reason |
|---|---|---|
| TC-SEC-020 | Confirmed-expected-fail (by design) | RULE-SEC-005's SoD conflict detection is deliberately inert in v1 — `UserRoleService.java:150` unconditionally returns `Set.of()`. Already recorded in this module's own INT-R gap resolution. Not a defect. |
| TC-SEC-011 | Partial | MODE-5 verified login-rejected-after-deactivate; did not directly assert the `ActiveSession` row itself is terminated in the DB. |
| TC-SEC-022 | Partial | MODE-5 exercised a single dashboard smoke `GET`; the TC's full call-twice-and-compare scenario text was not exercised. |
| TC-SEC-033 | Genuinely blocked | `com.erp.fin` has no source anywhere under `src/main/java` (confirmed: only `cu`, `file`, `notif`, `sec`, `common`, `main` exist) and `FIN` is absent from `governance/modules-registry.json`'s registered modules. No FIN code path exists for any mechanism to exercise — not a gap SEC can close alone. |

All other 31 TCs have at least one independently-confirmed PASS across the
three mechanisms (see `execution-state.json`'s `pass_tcs` and each sub's
`pass_tcs` for the exact RULE-SCENARIOS/API-SCENARIOS split).

## A real bug found and closed today (not TC-mapped)

MODE-5's problems report (`sec_problems_report.md`, Run ID 136014) flagged
exactly 2 "likely real bugs". One was the TC-SEC-020 SoD-inert-by-design
item above (not a defect). The other was real and is now closed:
`ActiveSessionSearchRequest` and `AuditLogEntrySearchRequest` exposed
top-level `userId`/`actorUserId` fields in the OpenAPI schema as if they
were independently settable, but the server silently ignored them — the
real, only-working contract was always `filters[]`. Fixed with
`@Schema(hidden = true)` on both fields (confirmed:
`src/main/java/com/erp/sec/dto/ActiveSessionSearchRequest.java:37`,
`AuditLogEntrySearchRequest.java:37`), docs regenerated, no behavioral
change.

## Status

`TEST-PLAN-BE` remains `PARTIAL` — 4 of 35 TCs are not yet a clean,
full-scenario-text PASS by any mechanism. No new status value was invented;
`PARTIAL` is the closest existing value and is defensible given the
remaining count.

## Pointers

- TestSprite report: `reports/TEST-REPORT-SEC-backend-2026-09-11.md`
- MODE-5 problems digest: `governance/modules/SEC/test-api/sec_problems_report.md`
- MODE-5 HTML report: `governance/modules/SEC/test-api/sec_api_test_report.html`
- MODE-5 script: `governance/modules/SEC/test-api/test_sec_apis.py`
- JUnit tests: `src/test/java/com/erp/sec/SecCoverageIntegrationTest.java`
- Authoritative record: `governance/modules/SEC/execution-state.json` → `test_phases[0].note`
