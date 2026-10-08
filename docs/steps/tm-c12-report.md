# TM-C12 — tenant lifecycle events and per-tenant token cut-off

| | |
|---|---|
| Plan | `docs/plans/tenant-maturity-plan.md` §5 C.1 (item 15), C.2 (item 14), §9 ADR-TENANT-002, §10 |
| Branch | `tm/c12-tenant-events-token-cutoff` (from `main` @ 60f251c, which includes A, G, C3, D, B, E) |
| Migration | none (`CORE_TENANT.TOKENS_INVALID_BEFORE` is `V19__tenant_lifecycle.sql`, package B) |
| Date | 2026-10-08 |

## Summary

**C.1.** `TenantService.updateStatus` publishes `TenantSuspendedEvent(tenantId, tenantCode, reason, actor)` /
`TenantActivatedEvent(tenantId, tenantCode, actor)` (`com.erp.events`, catalogue 11 → 13) on real transitions only,
delivered after commit. SEC's `TenantSuspendedSessionListener` (synchronous, after commit, inside `callAs(tenantId)` with
a `REQUIRES_NEW` transaction) terminates every open session of the tenant, both realms, `TERMINATED_BY` = the operator,
one `SESSION_TERMINATED` row each. NOTIF never claims a `QUEUED` row of a tenant that is not ACTIVE
(`NotificationDeliveryProcessor.prepare`, via the new uncached `TenantLookupApi.isActive`), the requeue job skips such
tenants, and `NotificationTenantActivationListener` re-dispatches the held rows on `TenantActivatedEvent`.

**C.2.** `JwtAuthenticationFilter` puts `TenantTokenFacts(tid, iat)` on the request for every signature-valid token;
`TenantResolutionFilter` answers 401 `TENANT_TOKEN_REVOKED` when `iat` (whole seconds) is before the tenant's cut-off
truncated to the second — for an authenticated token and for one SEC dropped (session ended), on every non-public path
of both chains (`/api/v1/tenant/me` included); a dropped token of a suspended tenant still answers 403
`TENANT_SUSPENDED`; public paths ignore a stale token (login works). `POST /api/v1/platform/tenants/{id}/revoke-tokens`
writes the cut-off in a PLATFORM transaction, then ends every session inside the tenant and audits `TOKENS_REVOKED`
in the tenant and in PLATFORM; answers `{ id, code, sessionsTerminated }`; PLATFORM refused (422
`TENANT_REVOKE_TOKENS_PLATFORM`). **Behaviour change:** a token from before a re-activation is now refused.

## Analysis entries written (commit b6a263c, before any code)

| File | Section | Ids |
|---|---|---|
| `TENANT/P1/srs-tenant.md` | 1.3.0 addendum, package-C12 block C1–C11 (after E's) | REQ/AC-TENANT-033 … 035, RULE-TENANT-023, -024; CHANGED RULE-TENANT-006, -012, -015, -016; 2 error codes; events; XM-TENANT-001 CHANGED |
| `TENANT/P1/registry-srs-tenant.md` | package-C12 block | last REQ 035 · RULE 024 · POL 015 · US 015 · ADR 005 (002 used) |
| `TENANT/P0/*`, `P0_5/prd-tenant.md` | package-C12 blocks | POL-TENANT-015; CHANGED POL-002, -006; US-TENANT-015; CHANGED US-003, -004; resolved decision 2 |
| `TENANT/P2/db-script-tenant.md`, `registry-db-tenant.md` | package-C12 blocks | no migration; DBF-TENANT-042 enforced; XM-TENANT-001 CHANGED (`isActive` → DBF-TENANT-005) |
| `decisions/TENANT/ADR-TENANT-002.md` | NEW | per-tenant cut-off instead of a `jti` denylist (ACCEPTED) |
| `SEC/P1/srs-sec.md` §12, `registry-srs-sec.md`, `P0/business-policies-sec.md` (#10), `module-registry-sec.md` | package-C12 | REQ/AC-SEC-092, -093 / 098, -099; XM-SEC-007 |
| `NOTIF/P1/srs.md` §5, `registry-srs-notif.md`, `P0/*`, `P0_5/prd-notif.md`, `P2/db-script.md` | package-C12 | RULE-NOTIF-024; XM-NOTIF-004, -005 |
| `platform/PROJECT-OVERVIEW.md` | events row | 11 → 13 core events |

Ids re-verified against the tree, this repository's full history and `governance-shared` (all history).

## Files changed

Code (erp-core main): `events/TenantSuspendedEvent`, `TenantActivatedEvent` (new); `tenant/TenantTokenFacts` (new),
`dto/TenantTokenRevocationResponse` (new), `TenantResolutionFilter`, `TenantService` (`updateStatus` publishes,
`revokeTokens`, `recordInTenantAndPlatform`), `PlatformTenantController.revokeTenantTokens`, `TenantDomain`
(`isTokenRevoked`, `assertTokenRevocationAllowed`), `Tenant.revokeTokens`, `TenantMapper`, `TenantErrorCodes`,
`TenantLookupApi(Impl).isActive`; SEC `JwtAuthenticationFilter`, `TenantSuspendedSessionListener` (new),
`UserSessionTerminator.terminateAllOpenSessions`, `SessionService.terminateAllSessionsForPlatform`,
`ActiveSessionRepository.findAllNonTerminated`, `SecAdminRecoveryApi(Impl).terminateAllSessions`; NOTIF
`NotificationLogDomain.isDeliverable / deliversFor`, `NotificationDeliveryProcessor`, `NotificationRequeueJob`,
`NotificationTenantActivationListener` (new); `ErpCoreNotifAutoConfiguration`; i18n (one C12 block each).
Tests: `TenantTokenCutOffIntegrationTest`, `TenantLifecycleEventsIntegrationTest`,
`NotificationSuspendedTenantIntegrationTest` (new); `TenantDomainTest` (+3), `PlatformTenantApiIntegrationTest`
(behaviour change), `TenantHttp` (iat helpers), `NotifTestFixtures` (patch with body).
Docs: CHANGELOG, CONSUMING, DEVIATIONS, core-test-plan, core_api_verify.py, run archive, api-docs (tenant),
governance/README (ADR count).

## Decisions & deviations (all in `docs/DEVIATIONS.md` `[TM-C12]`)

- `iat` exposed as a request attribute (`TenantTokenFacts`), not on `AuthRealm`: module boundary, and the token of a
  suspended / revoked tenant no longer authenticates after C.1.
- The tenant filter also judges a **dropped** token on non-public paths (keeps the 403 of TC-CORE-TENANT-022/-031 and
  makes `TENANT_TOKEN_REVOKED` observable after a re-activation); public paths ignore a revoked token.
- Cut-off compared in **whole seconds, strict**: a token of the cut-off's own second is served (logins right after an
  activation must work — TENANT-024/-032/-043 do so). The reference draft said "same second refused" — rejected.
- PLATFORM revoke refused (new code `TENANT_REVOKE_TOKENS_PLATFORM`); a suspended tenant may be revoked.
- Response `TenantTokenRevocationResponse` instead of the draft's `TenantResponse`.
- SEC listener synchronous; NOTIF re-dispatch on `TenantActivatedEvent` (the requeue job is off by default).
- `NotificationRequeueJob` new constructor with `TenantLookupApi` (old ones kept).
- Commit hygiene: the "public path ignores a revoked authenticated token" refinement of `TenantResolutionFilter` landed
  in the test commit 5008ff6.
- Reference snapshot (C1, C2, ADR draft) adopted except the points above, the event count (11 → 13, not 10 → 12) and
  "NOTIF registers no listener"; its facts 3 and 6 are confirmed and closed.

## Acceptance checklist

| # | Item | Evidence |
|---|---|---|
| 1 | Events published after commit by `updateStatus`, registered (catalogue/docs) | `TenantLifecycleEventsIntegrationTest.realTransitions_…`, `aStatusChangeRolledBack…`; PROJECT-OVERVIEW, CONSUMING §5 |
| 2 | Not on no-op / refused / rolled-back changes | same tests |
| 3 | `TenantLookupApi.isActive` (uncached) | `TenantLookupApiImpl`; lifecycle test asserts it |
| 4 | NOTIF claim/requeue skip non-ACTIVE, status unchanged, delivered after activation | `NotificationSuspendedTenantIntegrationTest` |
| 5 | SEC ends sessions of both realms on suspension, audited | `TenantLifecycleEventsIntegrationTest.suspension_…`; TC-CORE-TENANT-047 |
| 6 | Cut-off enforced (`TENANT_TOKEN_REVOKED`, both realms, `/tenant/me`, boundary) | `TenantTokenCutOffIntegrationTest`, `TenantDomainTest`; TENANT-047/048 |
| 7 | `revoke-tokens` + PLATFORM refusal + audit rows, cut-off never exposed | `TenantTokenCutOffIntegrationTest`; TENANT-048 … 050 |
| 8 | `PlatformTenantApiIntegrationTest` changed after the analysis entry; other flows re-login | renamed test; TENANT-024/032/043 unchanged and PASS |
| 9 | ADR-TENANT-002 | `decisions/TENANT/ADR-TENANT-002.md` |
| 10 | §10 DoD: analysis first (b6a263c precedes ab2afc5) | git log |
| 11 | §10 DoD: code = addendum | table below |
| 12 | §10 DoD: `mvn -q verify` green | below |
| 13 | §10 DoD: api-docs regenerated, completeness clean | below |
| 14 | §10 DoD: test plan + run archived | below |
| 15 | §10 DoD: CHANGELOG | `[TM-C12]` lines (Added, Changed — behaviour change) |
| 16 | §10 DoD frontend | n/a (backend package; FE impact rows in C11) |

16/16 (item 16 not applicable).

## Code ↔ addendum check

| Addendum item | Code | Match |
|---|---|---|
| `POST /{id}/revoke-tokens`, `PLATFORM_TENANT_MANAGE`, 200 `TenantTokenRevocationResponse {id, code, sessionsTerminated}`, 404 / 422 | `PlatformTenantController.revokeTenantTokens`, `TenantService.revokeTokens`; api-docs | yes |
| RULE-TENANT-023 (seconds, strict, null iat revoked; status first; dropped token; public ignore) | `TenantDomain.isTokenRevoked`, `TenantResolutionFilter` | yes |
| RULE-TENANT-024 | `TenantDomain.assertTokenRevocationAllowed` | yes |
| Error codes `TENANT_TOKEN_REVOKED` 401, `TENANT_REVOKE_TOKENS_PLATFORM` 422 + i18n | `TenantErrorCodes`, both bundles | yes |
| Events + payload names, realm STAFF, publisher | `TenantSuspendedEvent`, `TenantActivatedEvent`, `updateStatus` | yes |
| XM-TENANT-001 `isActive` | `TenantLookupApi(Impl)` | yes |
| `TenantTokenFacts` + attribute | `JwtAuthenticationFilter.exposeTokenFacts` | yes |
| SEC REQ-SEC-092/093, XM-SEC-007, `findAllNonTerminated`, `terminateAllOpenSessions` | as listed | yes |
| NOTIF RULE-NOTIF-024, XM-NOTIF-004/005, `isDeliverable`, listener, job constructor | as listed | yes |
| Audit `TOKENS_REVOKED` ×2, same tx, no instant | `TenantService.endSessions` | yes |
| No migration | — | yes |
| Registry "verified by" for REQ-TENANT-033 | NOTIF part moved to `NotificationSuspendedTenantIntegrationTest` (ArchUnit boundary) | fixed in 6bfe756 |

## Verification output

- `mvn -q verify` (offline, clean `target/`, code of f4b83f7): BUILD SUCCESS, JaCoCo met (erp-core lines 82.11 %).
  - erp-core: tests **618**, failures 0, errors 0, skipped 0 (91 suites).
  - erp-app-reference: tests **10**, failures 0, errors 0, skipped 0.
- HTTP suite: run **`261008065354`**, profile P-LIVE, port 18107, fresh `erp_tm_c12` (dropped afterwards):
  **196 PASS, 0 FAIL, 0 BLOCKED** (22 profile cases not run) — `docs/test-api/results/20261008T065357-P-LIVE.json` /
  `-report.md`. New cases TENANT-047 … 050 run before TENANT-046 (no public-branding call).
- api-docs: `review` → `update` (`--base http://localhost:18107 --server-url http://localhost:7272`): only `tenant/`
  changed (1 added, 2 updated — the logo endpoints' walk lists name the extracted `recordInTenantAndPlatform`).
  `check_completeness.py --base http://localhost:18107`: `per module: app=1, audit=1, cu=5, file=14, mdl=11, notif=18,
  report=4, sec=50, sequence=6, tenant=14 (sum 124) missing=0 duplicated=0 stale=0 RESULT: PASS`. `check`: TENANT PASS;
  FILE, NOTIF, CU, AUDIT, APP — the five known limitations, unchanged. Generator unit tests: OK.

## Skills checked

`gov-enforce-backend-contract` (Domain rules on `TenantDomain` / `NotificationLogDomain`, facts passed in; A.5.3
deviation as B/E for `revokeTokens`), `build-create-entity` (`Tenant.revokeTokens` field mutation only),
`build-create-repository` (`findAllNonTerminated`, JPQL, tenant-filtered), `build-create-dto` (bilingual `@Schema`),
`build-create-mapper` (null-safe `toTokenRevocationResponse`), `build-create-service` (cross-module via
`SecAdminRecoveryApi` / `TenantLookupApi`, events on the public bus), `build-create-controller` (thin, unique method
name, `@Operation`), `gov-enforce-caching-rules` (no cache: tenant status has a lifecycle), `gov-enforce-error-handling`
(LocalizedException + registered codes; filter-written 401), `gov-validate-backend-feature`, `api-verify`.

## Notes for later steps

- Next free ids: TENANT REQ/AC-036, RULE-025 (012 … 015 reserved), POL-016, US-016, ENT-002, SCR-REQ-002, XM-004,
  DBF-045, ADR-TENANT-003 (C.4) / 004 (C.6); SEC REQ-094, AC-100, RULE-063, ENT-015, DBF-124, XM-008, ADR-069;
  NOTIF RULE-025, XM-006; FILE RULE-011, XM-003, ADR-009. HTTP: TC-CORE-TENANT-051, TC-CORE-PLATFORM-006,
  TC-CORE-SEC-056, TC-CORE-NOTIF-017. Counts TENANT 50, total 218, P-LIVE 196.
- For C4/C6/C5: `TenantResolutionFilter` now reads a request attribute (`TenantTokenFacts`) set by
  `JwtAuthenticationFilter`, may `TenantContext.clear()` mid-request on a public path with a revoked token, and does
  one extra `CORE_TENANT` lookup (as PLATFORM, `callAs`) for a dropped token on non-public paths. Two after-commit
  listeners use `TenantContext.callAs`: SEC's (synchronous, in the request thread, `REQUIRES_NEW`) and NOTIF's
  (`@Async` executor). `TenantLookupApi.isActive` switches to PLATFORM via `callAs` when no tenant is current.
  A `ScopedValue` spike must keep these semantics (nested `callAs` inside an after-commit callback while the request's
  PLATFORM tenant is bound).
- HTTP suite: a test that suspends or re-activates a tenant must sign its users in again; to cut a token off it must
  wait for the second after the token's `iat` (`await_second_after`). Tenant D ends ACTIVE with a cut-off and one
  session (`T_D`).
