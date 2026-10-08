# ADR-TENANT-004 — `ScopedValue` for `TenantContext`: a spike with go / no-go

Module  : TENANT     Version : erp-core 1.3.0 (tenant-maturity plan, package C.6)     Stage raised : P1 (SRS) — before the code
Status  : PROPOSED — decision pending the spike on branch `spike/tenant-scoped-value` (plan §0 D6); may slip to 1.4.0
          without blocking the 1.3.0 tag

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
Pending — written here when the spike ends (ACCEPTED = go, REJECTED = no-go), with the measurements.

## Consequences
- The public API is frozen either way: no consumer, in core or in an application, changes a line.
- A go removes the class of bug REQ-TENANT-023 guards against and re-states that requirement for the new binding.
- A no-go costs nothing at runtime; the evidence stays here for a later MAJOR version.

## Traces
REQ-TENANT-017, REQ-TENANT-018, REQ-TENANT-023 · RULE-TENANT-023 (C12's public-path clear) · US-TENANT-008 ·
POL-TENANT-007 · plan §0 D6, §5 C.6, §9
