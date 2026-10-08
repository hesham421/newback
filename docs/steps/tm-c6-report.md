# Tenant-maturity package C6 — `ScopedValue` spike for `TenantContext` (go / no-go) — report

## Summary

Plan §0 D6 / §5 C.6 (item 11): a spike, never a silent rewrite. The question — can `TenantContext` move from its
`ThreadLocal` to `java.lang.ScopedValue` behind the same public API, and does it pay for itself — was written down
first as ADR-TENANT-004 (PROPOSED) with a measurement plan and go / no-go criteria fixed before any code (hard gates
H1–H4, benefit B1 structural or B2 performance). The spike (branch commit ae5fe50) then replaced the binding behind the
same API, passed every gate it could (full `mvn verify`, the tenant / NOTIF / decorator tests under virtual threads,
full P-LIVE 196 / 196), and was measured.

**Decision: NO-GO — ADR-TENANT-004 REJECTED.** `ScopedValue` is final in JDK 25 (H1) and the spike was green (H3) with
no p95 regression (H4), but keeping `set` / `clear` working outside a scope (H2 — `TenantContext` is public API under
`docs/RELEASE.md`) needs a `ThreadLocal` fallback, so the leak class REQ-TENANT-023 guards against remains (B1 not
met); refusing an unscoped `set` instead breaks 282 tests in 56 classes (the Spring test listener cannot hold a bounded
scope) and any application that calls `set` — a MAJOR change. No latency gain was measurable (B2 not met): p95 equal
within noise, `callAs` about twice as slow in-process at nanosecond scale. The spike code is reverted (10a77b5); the
branch carries the analysis, the decided ADR with its evidence, one leak test that passes on the `ThreadLocal`, the
P-LIVE archive of the spike build, CHANGELOG / DEVIATIONS lines and this report. No production file differs from main
(abbae25).

## Analysis entries written

| File | Section | Ids |
|---|---|---|
| `governance/analysis/decisions/TENANT/ADR-TENANT-004.md` | new ADR (PROPOSED in commit a14a9c5, REJECTED with measurements in 5d13312) | ADR-TENANT-004 (plan's own number, reserved for C.6) |
| `governance/analysis/modules/TENANT/P1/srs-tenant.md` | 1.3.0 addendum, new package C6 block: C6-1 (spike and criteria), C6-2 (outcome) | — (no REQ / AC / RULE minted) |
| `governance/analysis/modules/TENANT/P1/registry-srs-tenant.md` | 1.3.0 C6 block: Decisions delta (ADR-TENANT-004 REJECTED), Tests delta | — |
| `governance/analysis/modules/TENANT/P0/platform-summary.md` | 1.3.0 C6 block: spike row + outcome row | — |
| `governance/analysis/modules/TENANT/P0/module-registry-tenant.md` | 1.3.0 C6 block: exposed surface unchanged, RESOLVED DECISIONS 4 → no-go | — |

Next free TENANT ids unchanged by this package: REQ / AC-036, RULE-025, POL-016, US-016, XM-004, DBF-045; ADR-TENANT-003
(reserved for C.4) — ADR-TENANT-004 is now used.

## Files changed

Net against main (abbae25) — what merges:
- NEW `governance/analysis/decisions/TENANT/ADR-TENANT-004.md`
- CHANGED (append-only) `governance/analysis/modules/TENANT/P0/platform-summary.md`, `P0/module-registry-tenant.md`,
  `P1/srs-tenant.md`, `P1/registry-srs-tenant.md`
- NEW `erp-core/src/test/java/com/erp/events/support/TenantContextLeakTest.java` (8 tests)
- NEW `docs/test-api/results/20261008T075803-P-LIVE.json`, `…-P-LIVE-report.md` (the spike build's gate run)
- CHANGED `docs/CHANGELOG.md` (one `[TM-C6]` line), `docs/DEVIATIONS.md` (section `[TM-C6]`)
- NEW `docs/steps/tm-c6-report.md`

In branch history only (reverted): `TenantContext.java`, `JwtAuthenticationFilter.java`,
`TenantAndSecurityContextTaskDecorator.java` (ae5fe50, +76 / −41; reverted by 10a77b5).

Commits: a14a9c5 analysis · dd7c273 leak test · ae5fe50 spike · a41e401 test move (module boundary) · 10a77b5 revert ·
5d13312 decision docs · (this report).

## Decisions & deviations

- No-go per the criteria; spike code reverted; docs + one test merge (DEVIATIONS `[TM-C6]`).
- The request scope was opened in `JwtAuthenticationFilter` (first tenant-aware filter of both chains, which binds the
  token tenant before `TenantResolutionFilter`), not in `TenantResolutionFilter`; the latter needed no change because
  its `set` / `clear` — C12's mid-request clear on a public path included — updated the request frame. The immutable
  alternative (nested scopes) is analysed per caller in the ADR.
- Virtual threads: 14 classes run once with `SPRING_THREADS_VIRTUAL_ENABLED=true` (no committed Spring context
  combination — test pool limit).
- "p95 of the filter": measured end to end per request plus an in-process harness; no JMH.
- No TC-CORE cases, no api-docs regeneration: no endpoint, DTO or HTTP behaviour change.
- Leak test placed in `com.erp.events.support` (`CrossModuleBoundaryArchTest` analyses tests too — the first verify run
  of the spike failed exactly there; moved in a41e401).
- Reference snapshot ADR draft cited RULE-TENANT-013 / -014 (not on main): replaced by REQ-TENANT-017 / -018 / -023 and
  RULE-TENANT-023. Its statement "`ScopedValue` final in JDK 25" verified (`javap -v`, no `@PreviewFeature`).

## Measurements

All on one Windows 11 machine, JDK 25.0.4.1 (Temurin), PostgreSQL 16; details and method in ADR-TENANT-004.

| Id | What | Result |
|---|---|---|
| H1 | `ScopedValue` status at release 25 | final (JEP 506; no `@PreviewFeature` in the JDK class); `StructuredTaskScope` still preview |
| M1 | `TenantContextLeakTest`, 8 cases, pooled platform + virtual threads | 8 / 8 on the `ThreadLocal` and 8 / 8 on the spike; no leak via `callAs`, decorator or virtual threads on either; a raw `set` on a pooled thread leaks on both (spike: through its compatibility fallback) |
| M4 | 14 tenant / NOTIF / decorator classes with `spring.threads.virtual.enabled=true` (requests on `tomcat-handler-N` virtual threads), spike build | 65 / 65 |
| M5 | erp-core suite with unscoped `set` refused (`ERP_TENANT_CONTEXT_STRICT=true`) | 282 errors in 56 / 93 classes (269 from `TenantContextTestExecutionListener.beforeTestMethod`); 0 production-path failures |
| M2 | HTTP p95 (ms), `GET /api/v1/tenant/me`, N = 3 000 after 500 warm-up, fresh JVM per round, A B A B A B | staff token: A 11.08 / 11.82 / 10.85 vs B 8.54 / 12.63 / 9.99 (median 11.08 → 9.99, inside B's spread); header-only: A 2.02 / 1.81 / 1.95 vs B 1.52 / 2.15 / 2.13 (median 1.95 → 2.13, inside A's 10.6 % band); means A 4.32 / 0.97 vs B 4.57 / 1.10 |
| M2b | in-process ns/op (platform / virtual) | `current()` in scope ≈ equal (1.7–2.8 vs 2.1–2.3); `callAs` 21–27 vs 45–50; `set/current/clear` in scope 22–24 vs 8; outside a scope 21–24 vs 39–42 |
| M3 | complexity | +76 / −41 production lines in 3 files; two binding mechanisms; one new public method (`callScoped`); checked exceptions wrapped through a `CallableOp` in the JWT filter |

## Decision

**NO-GO.** Go ⇔ H1 ∧ H2 ∧ H3 ∧ H4 ∧ (B1 ∨ B2). H1, H3, H4 met; H2 met only with the `ThreadLocal` fallback, which
forfeits B1; B2 not met. ADR-TENANT-004 is REJECTED; `TenantContext` keeps its `ThreadLocal`; REQ-TENANT-023's guard
stays; item 11 closes for 1.3.0. Revisit only with a MAJOR version that drops `set` / `clear` (plus a JUnit-level
around-hook for the test tenant) or when `StructuredTaskScope` is final and core forks subtasks.

## Acceptance checklist

| # | Item (plan §5 C.6 / brief / §10 DoD) | Evidence |
|---|---|---|
| 1 | Analysis first: ADR-TENANT-004 PROPOSED + TENANT 1.3.0 C6 row with criteria before code | a14a9c5 precedes ae5fe50 |
| 2 | Branch `spike/tenant-scoped-value`, `ThreadLocal` replaced behind the same API | ae5fe50 (public signatures unchanged; `callScoped` added) |
| 3 | `set` / `clear` for the servlet filter via a bounded `ScopedValue.where(...).run(chain)` | `JwtAuthenticationFilter` `callScoped(null, …)` around the chain (ae5fe50) |
| 4 | Every `set` / `clear` caller analysed; C12 mid-request clear | ADR-TENANT-004 table "Every set / clear caller" |
| 5 | Full `mvn -q verify` (clean) on the spike | 631 + 10, 0 failures (after a41e401) |
| 6 | `TenantIsolationIntegrationTest`, decorator tests, NOTIF claim / async under virtual threads | M4: 65 / 65 |
| 7 | Full P-LIVE on port 18109, DB `erp_tm_c6` | run `2610080758F9`: 196 PASS / 0 FAIL / 22 profile cases not run |
| 8 | Context-leak tests with and without virtual threads | M1 (`TenantContextLeakTest`, both implementations) |
| 9 | p95 of the tenant filter, both builds, same machine, warm-up, N ≥ 2 000 | M2 (N = 3 000 × 3 rounds per build) + M2b |
| 10 | Code complexity delta | M3 |
| 11 | Decision in ADR-TENANT-004 with measurements | REJECTED (5d13312) |
| 12 | No-go: code reverted; only docs, the leak test, CHANGELOG / DEVIATIONS, report remain | 10a77b5; `git diff abbae25 -- erp-core/src/main erp-app-reference/src` empty |
| 13 | Branch green | final `mvn -q verify` on the reverted branch: 631 + 10, 0 failures, JaCoCo 82 % |
| 14 | DoD: code matches addendum; deviations recorded | table below; DEVIATIONS `[TM-C6]` |
| 15 | DoD: api-docs / check_completeness | n/a — no endpoint or DTO change |
| 16 | DoD: test plan extended, api-verify archived | no new case (no HTTP change); spike's P-LIVE archived |
| 17 | DoD: CHANGELOG `[Unreleased]` | `[TM-C6]` line under Added |
| 18 | DoD: frontend | n/a — nothing visible to the frontend |

18 / 18 (4 of them n/a by the nature of the package).

## Code ↔ addendum check

| Addendum item | Code (final branch) | Match |
|---|---|---|
| C6-1: public API of `TenantContext` frozen whatever the outcome | `TenantContext.java` identical to main | yes |
| C6-1: "on no-go … the spike code is reverted" | 10a77b5; production diff vs abbae25 empty | yes |
| C6-1: REQ-TENANT-023 changes only on go | `JwtAuthenticationFilter` leak guard identical to main | yes |
| C6-2: `TenantContextLeakTest` (`com.erp.events.support`, 8 cases) | file present, 8 / 8 green | yes |
| registry: ADR-TENANT-004 REJECTED | ADR status line REJECTED | yes |
| module-registry: no exposed-surface change | no `src/main` change | yes |
| No REQ / RULE / ENT / DBF / endpoint / migration | none in the diff | yes |

## Verification output

- `mvn -q verify` (all `target/` deleted first), final branch (docs + test): erp-core **631 tests, 0 failures, 0
  errors, 0 skipped**; erp-app-reference **10, 0, 0, 0**; JaCoCo erp-core 82 % instructions.
- Same on the spike commit (a41e401): erp-core 631 / 0 / 0 / 0, app 10 / 0 / 0 / 0.
- Virtual threads (spike): 65 tests in 14 classes, 0 failures.
- HTTP suite (spike jar, port 18109, fresh `erp_tm_c6`, dropped afterwards): run **`2610080758F9`**, P-LIVE,
  **196 PASS / 0 FAIL / 0 BLOCKED** (22 profile cases not run) — `docs/test-api/results/20261008T075803-P-LIVE.json` /
  `-report.md`.
- `check_completeness.py`: not run (no api-docs regeneration; no endpoint change).

## Skills checked

`GOVERNANCE-RULES.md` routing: no entity / repository / DTO / mapper / service / controller was created or changed, so
the `build-*` lane and `gov-enforce-backend-contract` did not apply; the spike touched two filters and an executor
decorator, outside the contract's seven layers. `gov-enforce-error-handling` noted: the spike's strict probe threw a raw
`IllegalStateException` — acceptable only as a reverted measurement switch, which it was. `api-verify` conventions
followed for the run archive. `CLAUDE.md` "Analysis first" followed (ADR and addendum before code, check after).

## Notes for later steps

- **For C5 (export):** `TenantContext` stays on `ThreadLocal`; use `TenantContext.callAs(id, () -> new
  TransactionTemplate(tm).execute(...))` for per-tenant reads (B precedent). Streaming a ZIP from one request thread is
  fine; if export work is handed to `erpCoreEventExecutor`, the decorator propagates the tenant and nested `callAs`
  inside the task works (pinned by `TenantContextLeakTest`). Do not start raw threads / `CompletableFuture.supplyAsync`
  for per-tenant work: a new thread inherits no tenant (also pinned). Never call `TenantContext.set` in export code.
- Test-pool note: no new Spring context was added. Virtual-thread checks are a one-off command
  (`SPRING_THREADS_VIRTUAL_ENABLED=true mvn -pl erp-core test -Dtest=…`), not a committed property set.
- Counts unchanged: test plan TENANT 50, total 218, P-LIVE 196; erp-core tests 623 → 631 (+8), app 10.
- `ADR-TENANT-004` is used; ADR-TENANT-003 remains reserved for C.4.
