# CU — api-verify problems report

Generated: 2026-10-05 · RUN_ID `199514` · tier **Full** (api-docs + P3_5_BE/test-execution-manifest.md both present)
Target: `http://localhost:7305` — erp-core 1.2.0 code @ newback `main` (reference app jar, profile `dev`, `spring.cache.type=simple`, dedicated PostgreSQL 16 database)
Run account: bootstrap `admin` in tenant `PLATFORM` (password from `ERP_BOOTSTRAP_ADMIN_PASSWORD`)

**Final result: 22 passed · 0 failed** across 2 suites · 13 observation(s), which never affect the totals.

## Legacy-suite re-run against erp-core 1.2.0

### Baseline (original script)

- Original script (git history, pre-2026-10-05), unmodified except the base URL passed as its own `[base_url]` argument: **0 passed · 0 failed · 0 executed — exit 2** (`FATAL: could not authenticate — ENVIRONMENT_FAILURE`).
- Cause: `POST /api/v1/sec/auth/login` with `admin`/`admin` and no tenant header answers **400 `TENANT_REQUIRED`**; with `X-Tenant-Code: PLATFORM` but `admin`/`admin` it answers **401 `SEC-401-INVALID-CREDENTIALS`**. Every test case is therefore blocked by the login contract alone.
- Diagnostic re-run with ONLY the login patched (tenant header + bootstrap password, no other change): **22 passed · 0 failed**, 13 observations — i.e. no behavioural difference in CU's CRUD surface broke any legacy assertion.

### Classification of baseline failures and adapted spots

Classes: **(a)** intended change (documented) · **(b)** test defect · **(c)** suspected app defect.

| Test / spot | Class | Reference & adaptation |
|---|---|---|
| Login (all 22 tests blocked) — tenant header | (a) INTENDED CHANGE | DEVIATIONS [05] resolution rule 3 (`400 TENANT_REQUIRED` on public non-exempt paths); CHANGELOG 1.0.0 'tenant resolution from the token or `X-Tenant-Code`'. Adapted: login sends `X-Tenant-Code` (env `ERP_TENANT_CODE`, default `PLATFORM`). |
| Login (all 22 tests blocked) — `admin`/`admin` | (a) INTENDED CHANGE | CHANGELOG 1.0.0 'the bootstrap admin password comes from `erp.core.security.bootstrap-admin-password` (no more `admin/admin`)'; DEVIATIONS [04]. Adapted: password from env `ERP_BOOTSTRAP_ADMIN_PASSWORD`; hard-coded credential removed. |
| Base URL | (b) TEST-DEFECT (run convention) | Hard-coded `http://localhost:7272`; now env `ERP_BASE_URL` (default unchanged), the `[base_url]` argument still wins. |
| TC-BE-CU-010 — documented response fields | (a) INTENDED CHANGE (assertion extended, not weakened) | DEVIATIONS [09] 'responses gain `scope`'; srs-cu.md addendum §1/§5; api-docs ConfigurationResponse.scope. `scope` added to the documented-field list and asserted `TENANT` for a row created without `?scope`. |
| Stage I `grants()` / Privileges section | (a) INTENDED CHANGE (text only — no assertion) | CU is now gated by `CuPermissions` (DEVIATIONS [06], [09]; module-registry-cu.md addendum PERMISSIONS). The PLATFORM bootstrap admin's super role holds them, so still no grant is needed; the obsolete 'permission gating absent' finding is marked RESOLVED. |
| Cleanup residue SQL | (a) INTENDED CHANGE (text only) | RULE-CU-001 is unique per owner since V14 (`UQ_CU_APP_CONFIG_CONFIG_KEY` on `(COALESCE(TENANT_ID,0), CONFIG_KEY)`; db-script.md addendum), so the cleanup SQL names the run tenant. |
| Session residue disclosure (`PROBE_AUDIT_12644`, `PROBE_KEY_61874`) | (b) TEST-DEFECT (stale report data) | Described a manual probe of an earlier run in a shared database, not this run; removed (git history keeps it). |

Counts: (a) 5 · (b) 2 · (c) 0

### Earlier findings re-checked

- Previous report (2026-09-12, pre-erp-core): **22 passed / 0 failed**, 13 observations. This run reproduces the same 22 assertions and 13 observations.
- **TC-BE-CU-008 (response body)** - the 'PUT response one update behind on `updatedAt`' bug kept in CLASSIFICATIONS from an older run does not reproduce: `ConfigurationService.update()` calls `saveAndFlush` before mapping the response.
- **TC-BE-CU-004 / TC-BE-CU-006** - the older governance mismatches stay resolved in the script itself (400 `VALIDATION_ERROR` naming `configValue`; the RULE-CU-003 invariant instead of a 422). Both pass unchanged.
- **Observation delta (not asserted) - unknown search FILTER field**: the previous report saw it silently ignored (HTTP 200, every row returned); it is now refused with 400 `VALIDATION_ERROR` and a `fieldErrors` entry naming the field ('This search does not support filtering on ...'). Origin: a deliberate platform change dated 2026-09-19, recorded in the comments of `SpecBuilder.assertFieldsAllowed` (error message key `UNSUPPORTED_FILTER_FIELD`; until then such a filter was silently skipped), shipped before erp-core (present in newback's initial commit a9ed097); DEVIATIONS [11] mentions it as existing behaviour. Fail-closed, contradicts no governed artifact; recorded for comparison only.

### (c) Suspected application defects

_None — after adaptation no failure remains that a documented decision does not explain._

## Suites

| Suite | Passed | Failed |
|---|---|---|
| PREFLIGHT (stage A0) | 3 | 0 |
| AppConfiguration | 19 | 0 |

## Failures

_None._

## Stage G — failure classification

_No failures to classify._

## Documentation findings (no assertion attached)

- **RESOLVED in erp-core 1.2.0 — api-docs POST status** — the old doc showed `Response 200 — OK` for `POST /api/v1/common/configurations`; newback `docs/api-docs/cu/endpoints/configuration-management.md` now documents `Response 201 — Created`, matching the live answer the test asserts.
- **api-verify-config.md §3 vs CU's runtime codes** — the config states the error-code format `{MOD}-{http}[-{SLUG}]` (e.g. `SEC-409-USER-DUP`). CU emits descriptive codes instead (`APP_CONFIGURATION_KEY_DUPLICATE`, ...), deliberately: `CuErrorCodes`' javadoc cites gov-enforce-error-handling's `<ENTITY>_<SCENARIO>` format. Unchanged in 1.2.0 (srs-cu.md addendum §3: `APP_CONFIGURATION_*` codes are unchanged). Needs a human reconciliation of the two governance documents.
- **api-verify-config.md §3 vs the actual error envelope** — the config describes `{code, messageAr, messageEn}`. The live envelope is `{code, message, fieldErrors}` — a single `message`, resolved from `Accept-Language`. The language assertions send two requests instead.
- **backend-test-plan TC-BE-CU-007 / TC-BE-CU-012** — both describe search as `GET /api/v1/common/configurations?configKey=...`. No such GET endpoint exists; the api-docs document `POST /api/v1/common/configurations/search` (also recorded as informational in srs-cu.md's 1.2.0 addendum §1). The script uses POST /search.
- **backend-test-plan TC-BE-CU-001** — expects `appConfigurationPk` and `isActiveFl` in the response. The live `ConfigurationResponse` exposes `id` and `isActive` (and, since erp-core step 09, `scope`). The script asserts the api-docs' field names.
- **RESOLVED in erp-core 1.2.0 — permission gating** — the old run found every `@PreAuthorize` commented out (`TODO: SEC-PENDING`). Now every operation is gated by `CONFIG_VIEW/CREATE/UPDATE/DEACTIVATE` (scope=TENANT) or `PLATFORM_SETTINGS_MANAGE` (scope=PLATFORM), code-defined by `CuPermissions` (DEVIATIONS [06], [09]; srs-cu.md addendum §4). Unauthenticated access is refused with 401 (observed).
- **index.md pagination envelope** — the table still omits `content`, which every search response carries (verified in preflight). Minor generator gap.

## Preconditions (stage A0)

_All preconditions satisfied. CU's payloads reference no externally-owned value (no FK, no lookup key, no owner-module code), so stage A0 verified reachability, key availability and the documented page envelope only._

## Observations (stage E — not pass/fail)

- **stage E — RULE-CU-002 violated with a blank configValue rather than an omitted one** — blank (whitespace) configValue -> HTTP 400 (VALIDATION_ERROR) — same generic code as the omitted-field case
- **stage E — an unknown property (configKey) in the update body** — unknown property `configKey` in the update body -> HTTP 200 (accepted, silently ignored) — the client is told the rename succeeded
- **stage E — configKey exactly at maxLength 150** — configKey at the documented 150-char limit -> HTTP 201 (accepted)
- **stage E — configKey one character over maxLength 150** — configKey one over maxLength 150 -> HTTP 400 (VALIDATION_ERROR)
- **stage E — notes one character over maxLength 2000 (update)** — notes one over maxLength 2000 -> HTTP 400 (VALIDATION_ERROR)
- **stage E — isActive sent with the wrong type** — isActive sent as a string -> HTTP 400 (VALIDATION_ERROR)
- **stage E — update with the required configValue omitted** — update with configValue omitted -> HTTP 400 (VALIDATION_ERROR)
- **stage E — requested page size above the documented maximum of 200** — size=500 (documented max 200) -> HTTP 200; effective size=200
- **stage E — an unknown sort field on search** — unknown sortField -> HTTP 200 (None)
- **stage E — an unknown search FILTER field** — unknown filter field -> HTTP 400, totalElements=None (silently ignored vs rejected)
- **stage E — an unauthenticated call to a CU endpoint** — unauthenticated GET -> HTTP 401 (SEC-401-INVALID-CREDENTIALS)
- **stage E — deactivate an already-deactivated configuration** — second DELETE on an already-inactive row -> HTTP 204 (None)
- **stage E — reactivating through the update endpoint's isActive field** — update with isActive=true on an inactive row -> HTTP 200; isActive=True (CU documents no dedicated activate endpoint)

## Surviving records

- AppConfiguration `MAIL_SMTP_HOST_199514` (id=17) — deactivated (soft, isActive=false); CU documents no hard delete, so the row remains in CU_APP_CONFIGURATION.
- AppConfiguration `MAIL_SMTP_HOST_B_199514` (id=18) — deactivated (soft, isActive=false); CU documents no hard delete, so the row remains in CU_APP_CONFIGURATION.
- AppConfiguration `MAIL_SMTP_HOST_SQLI_199514` (id=19) — deactivated (soft, isActive=false); CU documents no hard delete, so the row remains in CU_APP_CONFIGURATION.
- AppConfiguration `AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA_199514` (id=20) — deactivated (soft, isActive=false); CU documents no hard delete, so the row remains in CU_APP_CONFIGURATION.

### Residue from this session outside the script's own tracking

_None._

### Permanent residue (reference data other modules read)

_None — every row this run created was confirmed deactivated._

## Privileges

No permission grant was created or revoked by this run, and no grant journal was written. erp-core 1.2.0 gates every CU operation (`CONFIG_VIEW` / `CONFIG_CREATE` / `CONFIG_UPDATE` / `CONFIG_DEACTIVATE` for scope=TENANT, `PLATFORM_SETTINGS_MANAGE` for scope=PLATFORM — `CuPermissions`, DEVIATIONS [06]/[09]); the run account is the PLATFORM bootstrap admin, whose super role already holds them. Stage I needed no grant and **no standing privilege was left behind**.

