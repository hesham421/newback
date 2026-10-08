# ADR-TENANT-004 — `ScopedValue` for `TenantContext`: a spike with go / no-go

Module  : TENANT     Version : erp-core 1.3.0 (tenant-maturity plan, package C.6)     Stage raised : P1 (SRS) — before the code
Status  : REJECTED — no-go (erp-core 1.3.0, package C6, 2026-10-08; written PROPOSED-before-code in the analysis
          commit a14a9c5, decided after the spike: spike code ae5fe50, reverted by 10a77b5). The `ThreadLocal` stays.

## Context
`TenantContext` holds the current tenant in a `ThreadLocal<Long>`
(`erp-core/src/main/java/com/erp/tenant/TenantContext.java:27`). Its public surface is `current`, `find`,
`require`, `isPlatform`, `set`, `clear`, `runAs`, `callAs` (`:34-92`), and `docs/RELEASE.md` lists
`TenantContext` as public API: removing a member or changing what it does is a MAJOR change.

Who binds the tenant today (every `set` / `clear` caller in `src/main`, grep 2026-10-08):
- `sec/security/JwtAuthenticationFilter.java:92-115` — first tenant-aware filter of both chains: clears a tenant
  leaked on a pooled thread with a WARN (REQ-TENANT-023, erp-core 1.2.0), sets the token's `tid` inside
  `authenticate` (`:143`) **and leaves it set for the rest of the chain**, clears it when the token does not
  authenticate (`:109`), clears it in a `finally` (`:114`).
- `tenant/security/TenantResolutionFilter.java` — reads the token tenant (`:94`); C12 clears it **in the middle of the
  request** when a revoked token reaches a public path (`:108`), then falls through to the header branch, which sets
  the header tenant around the rest of the chain (`:131-136`); the path-tenant branch sets and restores (`:187-200`).
- `events/support/TenantAndSecurityContextTaskDecorator.java:22-48` — copies the submitter's tenant into a pooled
  worker of `erpCoreEventExecutor` and restores the worker's previous value afterwards.
- Everything else uses `runAs` / `callAs` (bootstrap runner, SEC `TenantSuspendedSessionListener` — synchronous,
  request thread, `REQUIRES_NEW` after commit, nested inside the PLATFORM request; NOTIF worker, requeue job,
  `NotificationTenantActivationListener` — `@Async`, nested `callAs` while the decorator bound PLATFORM;
  `TenantLookupApiImpl.isActive` — switches to PLATFORM via `callAs` only when no tenant is current).
- Hibernate reads the value when a session opens (`tenant/config/TenantIdentifierResolver.java:36-42`: the `-1`
  sentinel while bootstrapping, `require()` once strict).
- Tests: the Spring `TenantContextTestExecutionListener` sets PLATFORM in `beforeTestMethod` and clears it in
  `afterTestMethod`, spanning `@BeforeEach`, the test-managed transaction and `@AfterEach`; several unit tests call
  `set` / `clear` directly.

`java.lang.ScopedValue` binds a value to a bounded dynamic scope instead of a thread: a binding cannot outlive its
scope, nested bindings restore by construction, and reading it is cheap on virtual threads. It is **final in JDK 25**
(JEP 506; the JDK 25.0.4 class carries no `@PreviewFeature`), the project's `maven.compiler.release`; only
`StructuredTaskScope`, which would inherit bindings into subtasks, is still a preview API in JDK 25. Item 11 of the
plan asks whether the tenant should move to it — answered by measurement, not by a rewrite (plan §5 C.6).

## Question
Can `TenantContext` move from `ThreadLocal` to `ScopedValue` behind the **same public API**, with every existing
semantic kept (the 1.2.0 leak guard, C12's mid-request clear, nested `callAs` in after-commit listeners, the task
decorator, the Hibernate resolver), and does it pay for itself?

## Alternatives
1. **Keep the `ThreadLocal`** — known behaviour, the 1.2.0 leak guard, nothing changes in `events` / `notif`.
2. **`ScopedValue` behind the same API** — `runAs` / `callAs` become `ScopedValue.where(...).call(...)`; the request
   filter opens a bounded `ScopedValue.where(...).run(chain)` so that `set` / `clear` keep working inside it (a scope
   frame they update); the task decorator binds the captured tenant around the task. What `set` does **outside** any
   scope (tests, an application's own code) is the open point the spike measures.
3. **`ScopedValue` with a new API** (no `set` / `clear`; every binding is `where(...).run`) — a MAJOR change for every
   consumer under `docs/RELEASE.md`; excluded for 1.3.0.

## Measurement plan (the spike, branch `spike/tenant-scoped-value`)
- **M1 leak tests** — a unit test class that binds a tenant on a reused pooled platform thread and on virtual
  threads (`callAs` that throws, nested `callAs`, a decorated task, a raw `set` without `clear`) and asserts what the
  next task on the same thread sees; run against both implementations.
- **M2 filter latency** — the same machine and database, the ThreadLocal build (main) and the spike build run in turn
  on port 18109; per build and round: 500 warm-up requests, then N = 3 000 timed requests of (a) `GET
  /api/v1/tenant/me` with a staff token (JWT filter → token branch → handler) and (b) the same path with only
  `X-Tenant-Code` (header branch, `set` / `clear`, then 401); order A B A B so the A-vs-A spread gives the noise band;
  p50 / p95 / p99 reported. Plus an in-process timing of `current()` and `callAs` on platform and virtual threads.
- **M3 complexity** — production lines changed, number of binding mechanisms left, call sites that had to change.
- **M4 virtual threads** — `TenantIsolationIntegrationTest`, the decorator tests and the NOTIF claim / async /
  activation tests run with `spring.threads.virtual.enabled=true` (one-off command, environment variable; no new
  Spring context combination in the committed suite).
- **M5 consumer impact** — the suite run once with `set` outside a scope refused, counting what breaks.
- Plus the gates below: full `mvn -q verify` from a clean tree and the full P-LIVE HTTP suite on the spike build.

## Go / no-go criteria (fixed before the measurements)
Hard gates — any one failing is **no-go**:
- **H1** `ScopedValue` is final at release 25 (no `--enable-preview` for consumers).
- **H2** Same public API **and** behaviour: every public member keeps its signature and its documented effect —
  including `set` / `clear` called outside a request (a public method that starts throwing is a MAJOR change).
- **H3** Green: `mvn -q verify` (clean), `TenantIsolationIntegrationTest`, the decorator tests, the NOTIF tests under
  virtual threads (M4), the full P-LIVE suite; no assertion weakened.
- **H4** No latency regression: the spike's p95 (M2) is not above the ThreadLocal p95 by more than 5 % beyond the
  A-vs-A noise band.

Benefit — **go** needs at least one:
- **B1** Structural: no production path is left that can bind a tenant past its scope, so the REQ-TENANT-023 leak
  guard becomes unnecessary (a `ThreadLocal` kept only for API compatibility keeps the class of bug: B1 not met).
- **B2** Performance: p95 or the in-process cost measurably better than ThreadLocal beyond the noise band.

Cost: a second binding mechanism kept only for compatibility, or call sites restructured beyond the two filters and
the decorator, count against a go that rests on B1 alone.

**Decision rule:** go ⇔ H1–H4 hold and (B1 or B2) holds. Otherwise no-go: this ADR becomes REJECTED with the
evidence, the `ThreadLocal` stays, item 11 closes, and the spike's code is reverted (only docs and a leak test that
passes on the `ThreadLocal` remain).

## Decision
**No-go — REJECTED.** `TenantContext` keeps its `ThreadLocal`; item 11 closes for 1.3.0. The spike's code is reverted;
what stays is this ADR, the C6 analysis rows and the leak test `TenantContextLeakTest` (M1), which passes on the
`ThreadLocal`.

### What the spike built (commit ae5fe50, +76 / −41 lines in 3 production files)
- `TenantContext`: a `ScopedValue<Frame>` (a small mutable frame per binding). `callAs` / `runAs` bind a new frame
  (`ScopedValue.where(...).call`); a new `callScoped(tenantOrNull, op)` opens a scope; `set` / `clear` update the
  innermost frame. **Outside any scope** `set` / `clear` fell back to a per-thread value (a `ThreadLocal`, for H2), or
  — with `ERP_TENANT_CONTEXT_STRICT=true`, the M5 probe — `set` threw.
- `JwtAuthenticationFilter`: after the 1.2.0 leak guard, the rest of the filter (and so the chain) runs inside
  `callScoped(null, …)`; `authenticate`'s `set` and the `clear` on a rejected token change that request frame.
  `TenantResolutionFilter` needed **no change**: its `set` / `clear` (header branch, path-tenant set-and-restore, C12's
  mid-request clear on a public path) all update the request frame. The pure immutable form would need nested scopes
  instead: header and path branches → `callScoped(tenant, chain)`, C12's clear → `callScoped(null, rest of the
  filter)`; all three are expressible.
- `TenantAndSecurityContextTaskDecorator`: the task runs inside `callScoped(capturedTenant, task)`; the worker's own
  value is untouched (the old "restore the previous value" comes for free).

### Every `set` / `clear` caller — expressible as a bounded scope?
| Caller | Today | Bounded scope? |
|---|---|---|
| `JwtAuthenticationFilter` (leak guard, `authenticate`, `finally`) | `set` before the chain, `clear` after | yes — `callScoped` around the chain; the guard stays for unscoped leaks |
| `TenantResolutionFilter` (token check, C12 public-path clear, header, path tenant) | `set` / `clear` / restore around the chain | yes — update of the request frame, or nested scopes |
| `TenantAndSecurityContextTaskDecorator` | `set` / `clear` around the task | yes — `callScoped(captured, task)` |
| SEC `TenantSuspendedSessionListener` (sync, request thread, `REQUIRES_NEW`), NOTIF `NotificationTenantActivationListener` (`@Async`), `TenantLookupApiImpl.isActive`, NOTIF worker and requeue job, bootstrap runner | `callAs` / `runAs` only | already bounded — nested `callAs` works unchanged (the integration tests below) |
| test `TenantContextTestExecutionListener` (PLATFORM for every integration test) | `set` in `beforeTestMethod`, `clear` in `afterTestMethod` | **no** — a Spring `TestExecutionListener` has no around-hook, and the test-managed transaction begins in `beforeTestMethod`, so the tenant must outlive that call |
| direct `set` / `clear` in tests (`TenantContextTest`, `SettingsCacheTest`, `DomainEventTest`, `AuditApiIntegrationTest`, `SecLogoutIntegrationTest`, the decorator and leak tests, `JwtAuthenticationFilterTenantLeakTest`, `WebServerStartTenantProbe`) | direct `set` / `clear` | only by rewriting each test |
| applications (`TenantContext` is public API, `docs/RELEASE.md`) | may call `set` / `clear` anywhere | unknown — a public method that starts throwing is a MAJOR change |

### Measurements (2026-10-08, one Windows 11 machine, JDK 25.0.4.1 Temurin, PostgreSQL 16)
- **H1** — `java.lang.ScopedValue` in JDK 25.0.4 carries no `@PreviewFeature` (`javap -v`; JEP 506, final);
  `StructuredTaskScope` is still preview. **Met.**
- **H3** — spike build: `mvn -q verify` from a clean tree green (erp-core 631 tests, 0 failures / errors; app 10).
  With `SPRING_THREADS_VIRTUAL_ENABLED=true` (requests on virtual `tomcat-handler-N` threads; one-off command, no new
  Spring context in the suite): `TenantIsolationIntegrationTest`, `TenantAndSecurityContextTaskDecoratorTest`,
  `TenantContextLeakTest`, `NotificationClaimIntegrationTest`, `NotificationAsyncDeliveryIntegrationTest`,
  `NotificationSuspendedTenantIntegrationTest`, `NotificationTenantActivationListenerTest`,
  `NotificationDeliveryListenerTest`, `StaffPasswordChangedNotificationIntegrationTest`,
  `TenantLifecycleEventsIntegrationTest`, `TenantTokenCutOffIntegrationTest`, `TenantContextIntegrationTest`,
  `TenantBootstrapWindowIntegrationTest`, `JwtAuthenticationFilterTenantLeakTest` — 65 / 65 green. Full P-LIVE HTTP
  suite on the spike jar (port 18109, fresh `erp_tm_c6`), run `2610080758F9`: **196 PASS / 0 FAIL**
  (`docs/test-api/results/20261008T075803-P-LIVE.json`). **Met** (compatibility mode).
- **M1 leak tests** — `TenantContextLeakTest` (8 cases: failing and nested `callAs`, a decorated task with a nested
  PLATFORM `callAs`, no inheritance into a new thread, 2 000 concurrent virtual threads, a raw `set` on a pooled
  thread) passes on **both** implementations. On a reused pooled platform thread neither leaks through `callAs` or the
  decorator into the next task; on virtual threads the test pins the in-task semantics only (restore, nested `callAs`,
  per-thread isolation) — a virtual thread is never reused, so it cannot show a next-task leak (review round 1). On
  both, a raw `set` without `clear` on a pooled platform thread is seen by the next task (compatibility mode keeps it)
  — the leak REQ-TENANT-023's guard exists for.
- **M5 consumer impact** — the erp-core suite with unscoped `set` refused: **282 errors in 56 of 93 test classes**
  (269 from `TenantContextTestExecutionListener.beforeTestMethod`, the rest direct `set` in tests); **no production
  path failed** — production code binds only inside scopes once the JWT filter and the decorator open them.
- **M2 filter latency** — sequential HTTP timing, one keep-alive connection, measured client side; per round a fresh
  JVM of the jar (same DB), 500 warm-up then N = 3 000 timed `GET /api/v1/tenant/me`; rounds A B A B A B
  (A = ThreadLocal, main abbae25; B = spike), milliseconds:

  | Case | A p50 (3 rounds) | B p50 | A p95 | B p95 | A mean | B mean |
  |---|---|---|---|---|---|---|
  | staff token (JWT filter → token branch → handler) | 3.31 / 3.30 / 3.19 | 4.19 / 3.65 / 3.37 | 11.08 / 11.82 / 10.85 | 8.54 / 12.63 / 9.99 | 4.32 | 4.57 |
  | `X-Tenant-Code` only (header branch `set` / `clear`, then 401) | 0.74 / 0.69 / 0.71 | 0.83 / 0.75 / 0.74 | 2.02 / 1.81 / 1.95 | 1.52 / 2.15 / 2.13 | 0.97 | 1.10 |

  Noise band and H4 test (review round 1: derived from the p95 values in the table):
  `band = (max − min of the three A p95) / median of the three A p95`; H4 fails iff
  `median B p95 > median A p95 × (1 + band + 0.05)`.
  Staff token: band = (11.82 − 10.85) / 11.08 = 8.8 %; limit 11.08 × 1.138 = 12.61; B median 9.99 (−9.8 % vs A).
  Header only: band = (2.02 − 1.81) / 1.95 = 10.8 %; limit 1.95 × 1.158 = 2.26; B median 2.13 (+9.2 % vs A).
  **H4 met** (both B medians under their limits); **no gain** — the token median is lower on B, but B's own rounds
  spread over 4.09 ms (8.54 … 12.63), and p50 and the means are slightly higher on B in every round (0.04–0.25 ms).
- **M2b in-process** (a Java harness compiled against each erp-core jar; median of 15 rounds × 5 M operations; ns per
  operation, platform / virtual thread): `current()` inside a scope 1.7–1.9 / 2.5–2.8 (ThreadLocal) vs 2.1 / 2.1–2.3
  (ScopedValue); `callAs(id, current)` 21–27 vs **45–50** (each binding allocates a carrier and a frame);
  `set` / `current` / `clear` inside a scope 22–24 vs 7.7–8.1; outside a scope 21–24 vs 39–42 (the `isBound` miss plus
  the fallback). Nanoseconds against milliseconds per request: no request-level effect either way.
- **Reproduction** — the HTTP timing script (`http_bench.py`), the in-process harness (`Bench.java`) and their raw
  outputs (`http-bench-results.jsonl`, `micro-bench-results.txt`) are archived in `docs/steps/tm-c6/`, together with
  the commands in `docs/steps/tm-c6-report.md` → Measurements (review round 1).
- **M3 complexity** — +76 / −41 production lines in 3 files; two binding mechanisms instead of one (scoped frame +
  `ThreadLocal` fallback); one new public method (`callScoped`); the JWT filter wraps its checked exceptions
  (`IOException` / `ServletException`) through a `CallableOp`.

### Verdict against the criteria
H1 met · H2 met **only** with the `ThreadLocal` fallback (strict mode breaks 56 test classes and any application that
calls `set` outside a request — a MAJOR change) · H3 met · H4 met · **B1 not met** — with the fallback a raw `set`
still binds a tenant past any scope, so REQ-TENANT-023's guard must stay and the class of bug remains · **B2 not met**
— p95 equal within noise, `callAs` twice as slow in-process (irrelevant at request scale). Go ⇔ H1–H4 ∧ (B1 ∨ B2):
**no-go**. The cost (a second binding mechanism, a new public method, a filter rewritten around a `CallableOp`) buys
nothing measurable.

### B1 could not be met once H2 held (review round 1)
H2 requires `set` / `clear` to keep working outside any scope, which needs a per-thread fallback; a per-thread fallback
lets a raw `set` outlive every scope, which is exactly what B1 excludes. So under these criteria a go could only ever
have come from B2 (a measurable latency gain), and that was knowable before the spike: the measurements confirmed the
compatibility cost (M5) and found no B2. A future revisit should not re-run this ADR as written: it should **drop H2**
and accept a MAJOR change (`set` / `clear` removed or failing outside a scope), so that B1 becomes reachable, and judge
the go on B1 against the migration cost for applications and tests.

### When to revisit
A MAJOR release (2.0) that removes `set` / `clear` from the public API (every binding a bounded scope), together with a
test-support extension that binds PLATFORM around each test method (a JUnit `InvocationInterceptor`, which also needs
the test-managed transaction to begin inside it); or a JDK in which `StructuredTaskScope` is final and core forks
subtasks that must inherit the tenant. Nothing in 1.3.0 needs either.

## Consequences
- The public API and its behaviour are unchanged: no consumer, in core or in an application, changes a line; no
  production file differs from main (abbae25).
- REQ-TENANT-023 (the JWT filter's leak guard) stays as written; `TenantContextLeakTest` now pins the `ThreadLocal`'s
  behaviour on pooled platform and virtual threads, including the nested `callAs` the C12 listeners rely on.
- Item 11 of the plan is closed for 1.3.0; the evidence above is the starting point of a later MAJOR version.

## Traces
Decided after the spike (report `docs/steps/tm-c6-report.md`).
REQ-TENANT-017, REQ-TENANT-018, REQ-TENANT-023 · RULE-TENANT-023 (C12's public-path clear) · US-TENANT-008 ·
POL-TENANT-007 · plan §0 D6, §5 C.6, §9
