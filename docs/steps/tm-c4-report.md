# TM-C4 — idempotent provisioning (`Idempotency-Key`)

| | |
|---|---|
| Plan | `docs/plans/tenant-maturity-plan.md` §5 C.4 (item 13), §9 ADR-TENANT-003, §10, §11 |
| Branch | `tm/c4-idempotent-provisioning` (from `main` @ abbae25, which includes A, G, C3, D, B, E, C12) |
| Migration | `V21__core_idempotency_key.sql` (the reserved number; the plan expected V20) |
| Date | 2026-10-08 |

## Summary

`POST /api/v1/platform/tenants` accepts an optional `Idempotency-Key` header (`^[A-Za-z0-9._:-]{1,64}$`, else 400
`IDEMPOTENCY_KEY_INVALID`). The mechanism is common (`com.erp.common.idempotency`, no import from any module):
`IdempotentResponses.craftResponse(key, endpoint, request, dataType, action)` is the controller's response helper. With
a key it looks the key up in the caller's tenant (Hibernate `@TenantId`); a live row with the same keyed request hash and
the same `CREATED_BY` is **replayed** (stored status and envelope, header `Idempotent-Replayed: true`, nothing runs);
any other live row → 409 `IDEMPOTENCY_KEY_CONFLICT`. Without a live row the key's row is **claimed** (inserted and
flushed) and the create runs **in the same transaction** (the `@Transactional` service joins it); a 2xx answer is stored
in the row before the commit, anything else rolls everything back (only successful answers are stored). Two simultaneous
first requests: the second claim waits on `UQ_CORE_IDEMPOTENCY_KEY`, then replays the first (no "in progress" state).
The request hash is an HMAC-SHA256 of the canonical JSON of the bound body (keyed from the JWT secret: the body carries
`adminPassword`). Keys live 24 h (`erp.core.idempotency.retention`): expired rows are ignored at lookup and purged by
`IdempotencyKeyRetentionJob` (tenant by tenant, `AuditRetentionJob` pattern, `retention-cron`, default off).
`CORE_IDEMPOTENCY_KEY` is tenant-scoped (entity extends `AuditableEntity`): the plan's nine columns plus `CREATED_BY`
(NOT NULL, the key's owner), `UPDATED_BY`, `UPDATED_AT`. C12 nit: revoke-tokens' failure path now guards the PLATFORM
audit write.

## Analysis entries written (commit df7e4e7, before any code)

| File | Section | Ids |
|---|---|---|
| `TENANT/P1/srs-tenant.md` | 1.3.0 addendum, package-C4 block I1–I11 (after C12's) | REQ/AC-TENANT-036, RULE-TENANT-025, -026; error codes `IDEMPOTENCY_KEY_INVALID` 400, `IDEMPOTENCY_KEY_CONFLICT` 409; I6 = where the common mechanism's code-level rules live (no COMMON analysis folder) |
| `TENANT/P1/registry-srs-tenant.md` | package-C4 block | last REQ 036 · RULE 026 · POL 016 · DBF 045 · ADR 005 (002, 003 used) |
| `TENANT/P0/business-policies-tenant.md`, `module-registry-tenant.md`, `platform-summary.md` | package-C4 blocks | POL-TENANT-016; CHANGED POL-TENANT-004; resolved decision 3; AUTO-decision (table registered in TENANT's P2) |
| `TENANT/P0_5/prd-tenant.md` | package-C4 block | US-TENANT-001 CHANGED (no new story) |
| `TENANT/P2/db-script-tenant.md`, `registry-db-tenant.md` | package-C4 blocks | `CORE_IDEMPOTENCY_KEY` (12 columns, exact names/widths/constraints/indexes/sequence), V21 script, DBF-TENANT-045, counts 23 / 15 / 22; the AuditableEntity open point decided |
| `decisions/TENANT/ADR-TENANT-003.md` | NEW | PROPOSED in df7e4e7, ACCEPTED in the check commit 968f68d |
| `platform/PROJECT-OVERVIEW.md`, `project-registry.md`, `governance/README.md` | `com.erp.common` row; TENANT ADR count 3 → 4 (registry row: 4 ADRs, 14 operations — the row was stale since C12) | — |

Ids re-verified against the tree, this repository's full history and `governance-shared` (no TENANT ids there).

## Files changed

Code (erp-core main): `common/idempotency/` (new) `IdempotentResponses`, `IdempotencyKeyDomain`, `IdempotencyKey`,
`IdempotencyKeyRepository`, `IdempotencySettings`, `IdempotencyKeyRetentionJob`, `IdempotencyErrorCodes`;
`autoconfigure/ErpCoreProperties` (`Idempotency`), `ErpCoreAutoConfiguration` (`idempotencySettings` bean);
`tenant/controller/PlatformTenantController.create` (header, helper); `tenant/service/TenantService`
(`recordSessionsNotTerminated`, cause chained); `db/migration/core/V21__core_idempotency_key.sql`; i18n (one C4 block
each). Tests: `IdempotencyKeyDomainTest`, `IdempotentResponsesTest`, `TenantIdempotentProvisioningIntegrationTest` (new);
`TenantTokenCutOffIntegrationTest` (+1), `TenantHttp` (header POST, body, another operator), `TenantSchemaIntegrationTest`
(23 / 15 / 22), `CoreLibraryRulesArchTest` (`RAW_JDBC_CLASSES` + the retention job), `ReferenceApplicationSmokeTest` (V21).
Docs: CHANGELOG, CONSUMING (§2 property row, scheduling row, §3 "Idempotent POSTs"), DEVIATIONS `[TM-C4]`,
core-test-plan, core_api_verify.py, run archive, `docs/api-docs/tenant/endpoints/platform-tenants.md`.

## Decisions & deviations (all in `docs/DEVIATIONS.md` `[TM-C4]`)

- V20 → V21 (execution order). Nine → twelve columns (`AuditableEntity`; `CREATED_BY` NOT NULL = owner).
- Mechanism shape: a response helper beside `OperationCode` (not a servlet filter: one transaction with the service, a
  typed replay keeps the api-docs schema). Its codes are named in the create's `@Operation` (the generator does not walk
  into common beans); the header is a documented parameter.
- Concurrency: no `IDEMPOTENCY_KEY_IN_PROGRESS`; the unique index serialises same-key requests; the loser replays.
- Failures: only 2xx stored (one transaction: a failed create left nothing, so a retry must run).
- Hash: keyed HMAC-SHA256 of canonical JSON (not raw bytes; not a plain SHA-256, which would be an offline verifier of
  `adminPassword`). The stored response (`TenantResponse`) contains no password (verified on the stored rows of the run).
- Replay only to the key's owner (another user → 409): a replay happens before the service's `@PreAuthorize`.
- Expiry at lookup as well as by the job; the job is a documented raw-JDBC exception (common must not import
  `TenantContext`).
- No COMMON analysis folder: the mechanism's rules sit in TENANT's C4 block (I6) and in CONSUMING.
- C12 nit fixed (audit failure suppressed, session failure = cause via `initCause`).
- Header `@Schema` pattern without braces (generator quirk, found by the regeneration — see below).
- Reference snapshot C4 + ADR draft adopted except: V20 → V21, the COMMON folder rows (none here), the shared-namespace
  replay (owner-only), "REQUEST_HASH = `TokenHasher.sha256Hex`" (keyed HMAC instead), and the open column point
  (decided: 12 columns). Snapshot fact 7 (FK / index names by convention) confirmed and bound.

## Acceptance checklist

| # | Item | Evidence |
|---|---|---|
| 1 | `V21__core_idempotency_key.sql`: table, `UQ_CORE_IDEMPOTENCY_KEY (TENANT_ID, IDEMPOTENCY_KEY, ENDPOINT)`, `SEQ_CORE_IDEMPOTENCY_KEY`, index on `CREATED_AT`; AuditableEntity decision with the exact column list | P2 entry; migration; `TenantSchemaIntegrationTest` (23 / 15 / 22), `ReferenceApplicationSmokeTest` (V21), `MigrationNamingTest` |
| 2 | Mechanism in `com.erp.common.idempotency`, no common → tenant dependency | `CrossModuleBoundaryArchTest`, `CoreLibraryRulesArchTest` green; `IdempotencySettings` built in autoconfigure |
| 3 | Optional header ≤ 64, charset validated, invalid → 400 | `IdempotencyKeyDomainTest`, integration test, TC-CORE-TENANT-053 |
| 4 | Same key + same body → replay with `Idempotent-Replayed: true`, nothing re-provisioned (tenant count) | integration test (field order too), TC-CORE-TENANT-051 |
| 5 | Same key + different body → 409 `IDEMPOTENCY_KEY_CONFLICT` (AR+EN) | integration test, TC-CORE-TENANT-052; both bundles |
| 6 | Concurrent same-key first requests: exactly one provisions | `twoSimultaneousFirstRequestsWithOneKey_…`, `IdempotentResponsesTest.aClaimLost…` |
| 7 | Failures not stored (decided, recorded) | `invalidKeys_are400_aRefusedCreateStoresNothing_…`, `IdempotentResponsesTest.aNon2xxAnswer_…`, TC-CORE-TENANT-053 |
| 8 | Canonical hash; password only inside a keyed hash; response free of secrets | ADR-TENANT-003 Reason 5; integration test (row and bodies without the password); run DB check |
| 9 | Header absent = 1.2.0 behaviour | integration test, TC-CORE-TENANT-053, whole P-LIVE suite unchanged |
| 10 | Retention 24 h via the scheduled-job pattern; `erp.core.idempotency.*` in `ErpCoreProperties` + CONSUMING | `IdempotencyKeyRetentionJob`; `anExpiredKey_countsAsUnused_andTheRetentionJobDeletes…`; CONSUMING §2/§3 |
| 11 | Tenant isolation of keys | `aKeyStoredInAnotherTenant_isNeverSeenByPlatform`; retention test (other tenant's row purged) |
| 12 | ADR-TENANT-003 | `decisions/TENANT/ADR-TENANT-003.md` (ACCEPTED) |
| 13 | C12 nit: PLATFORM audit call guarded | `TenantService.recordSessionsNotTerminated`; `TenantTokenCutOffIntegrationTest.revokeTokens_whoseSessionStepAndPlatformAuditFail_…` |
| 14 | §10 DoD: analysis first (df7e4e7 precedes 0a5ad05) | git log |
| 15 | §10 DoD: code = addendum | table below |
| 16 | §10 DoD: `mvn -q verify` green | below |
| 17 | §10 DoD: api-docs regenerated, completeness clean | below |
| 18 | §10 DoD: test plan + run archived | below |
| 19 | §10 DoD: CHANGELOG | `[TM-C4]` (Added, Fixed) |
| 20 | §10 DoD frontend | n/a (backend package; FE rows in I11) |

20/20 (item 20 not applicable).

## Code ↔ addendum check

| Addendum item | Code | Match |
|---|---|---|
| `CORE_IDEMPOTENCY_KEY` 12 columns, types and widths, NOT NULLs, defaults | V21; `IdempotencyKey` (+ inherited audit/tenant/version) | yes |
| `PK_` / `FK_` / `UQ_CORE_IDEMPOTENCY_KEY` / `IDX_…_TENANT` / `IDX_…_CREATED_AT` / `SEQ_CORE_IDEMPOTENCY_KEY` | V21; `@Table`, `@SequenceGenerator(allocationSize = 1)` | yes |
| DBF-TENANT-045 discriminator; counts 23 / 15 / 22 | `TenantSchemaIntegrationTest` | yes |
| I1 endpoint, header, response header, order of checks | `PlatformTenantController.create`, `IdempotentResponses` | yes |
| RULE-TENANT-025 pattern | `IdempotencyKeyDomain.KEY_PATTERN` | yes (the OpenAPI schema states it as `+` with min/max length — same set) |
| RULE-TENANT-026 lookup / expiry / replay-or-conflict / claim / 2xx only / owner | `IdempotentResponses`, `IdempotencyKeyDomain` | yes |
| I4 codes, statuses, i18n | `IdempotencyErrorCodes`; `VALIDATION_ERROR` / `CONFLICT`; both bundles | yes |
| I6 classes and repository methods | as listed | yes |
| I7 properties and defaults; retention must be positive | `ErpCoreProperties.Idempotency`; `IdempotencySettings` constructor | yes |
| Retention job SQL names `TENANT_ID` | `IdempotencyKeyRetentionJob` | yes |
| I9 logs without key/body/hash | `IdempotentResponses` log lines | yes |
| I10 C12 follow-up | `TenantService.revokeTokens` | yes |
| ADR-TENANT-003 decision | code | yes |

## Verification output

- `mvn -q verify` (offline, clean `target/`, code of 968f68d — the check commit; later commits are docs only): BUILD
  SUCCESS, JaCoCo met (erp-core lines 82.27 %).
  - erp-core: tests **640**, failures 0, errors 0, skipped 0 (95 suites; C12 ended at 623: +5 domain, +5 helper,
    +6 integration, +1 revoke-tokens).
  - erp-app-reference: tests **10**, failures 0, errors 0, skipped 0.
- HTTP suite: run **`2610080813D9`**, profile P-LIVE, port 18108, fresh DB `erp_tm_c4` (dropped afterwards):
  **199 PASS, 0 FAIL, 0 BLOCKED** (22 profile cases not run) — `docs/test-api/results/20261008T081344-P-LIVE.json` /
  `-report.md`. New cases TENANT-051 … 053 run before TENANT-046 (no public-branding call). An earlier run on the code
  before the check commit (also 199/199) was replaced by this one.
- api-docs: `review` → `update` (`--base http://localhost:18108 --server-url http://localhost:7272`): only `tenant/`
  changed — the create's description and its `Idempotency-Key` header row; revoke-tokens' walk list names
  `recordSessionsNotTerminated`. The first regeneration lost the create's permission and 403 row (braces in the header's
  `@Schema` pattern); fixed in the check commit and regenerated. `check_completeness.py --base http://localhost:18108`:
  `per module: app=1, audit=1, cu=5, file=14, mdl=11, notif=18, report=4, sec=50, sequence=6, tenant=14 (sum 124)
  missing=0 duplicated=0 stale=0 RESULT: PASS`. `check`: TENANT PASS; FILE, NOTIF, CU, AUDIT, APP — the five known
  limitations, unchanged. Generator unit tests: 67, OK.

## Skills checked

`gov-enforce-backend-contract` (Domain plain, `create`/`from`-style factories, LocalizedException; entity A.1.x incl.
`@Table` constraints/indexes, `@SuperBuilder`, sequence; repository A.2.1/2.2; A.5.3 deviation already recorded for
non-`@Transactional` tenant methods — the helper opens the transaction itself), `build-create-entity`,
`build-create-repository`, `build-create-dto` / `build-create-mapper` (n/a: no DTO or mapper — the mechanism stores the
answer's JSON), `build-create-service` (n/a for the mechanism: no service of its own; `TenantService` change follows it),
`build-create-controller` (thin; A.6.3 helper beside `OperationCode`; `@Operation`; unique method names unchanged),
`gov-enforce-error-handling` (two registered codes, both bundles), `gov-enforce-caching-rules` (no cache: a key table is
never cacheable), `gov-validate-backend-feature` (Stage 0 items 3, 4, 7–9 n/a for private infrastructure; raw-SQL rule
RULE-TENANT-011 checked on the retention job), `api-verify`.

## Notes for later steps

- Next free ids: TENANT REQ/AC-037, RULE-027 (012 … 015 reserved), POL-017, US-016, ENT-002, SCR-REQ-002, XM-004,
  DBF-046, ADR-TENANT-004 (C.6); SEC, FILE, NOTIF unchanged (SEC REQ-094, AC-100, RULE-063, ENT-015, DBF-124, XM-008,
  ADR-069; FILE RULE-011, XM-003, ADR-009; NOTIF RULE-025, XM-006). HTTP: TC-CORE-TENANT-054, TC-CORE-PLATFORM-006,
  TC-CORE-SEC-056, TC-CORE-NOTIF-017. Counts TENANT 53, total 221, P-LIVE 199.
- For C5 (export): `POST /{id}/export` could reuse `IdempotentResponses` only if its work joins one transaction — it
  stores a FILE document inside `callAs(PLATFORM)`/REQUIRES_NEW, so do **not** wrap it (ADR-TENANT-003 Consequences).
  `CORE_IDEMPOTENCY_KEY` is tenant data: an export of PLATFORM would include its rows; an export of a tenant has none
  today (only PLATFORM calls the endpoint) — decide whether the export SPI skips the table (it holds answers, no
  business data). The table count for `TenantSchemaIntegrationTest` is now 23 / 15 / 22.
- For C6 (ScopedValue): the mechanism reads the tenant only through Hibernate's resolver (no `TenantContext` call in
  common); the claim transaction is opened in the request thread before the service.
- Api-doc generator: a `{…}` inside a controller annotation string breaks `security_extractor._method_body_span`
  (permission dropped silently); avoid braces in `@Schema(pattern)` on controller parameters, or fix the generator by
  blanking string literals there.
- Test DB pool: the concurrency test holds two request transactions at once (pool 4 per context) — no new context.
