# Project registry — live modules of erp-core 1.2.0

The modules the running platform serves, with where each one's analysis, screens, API contract
and test suites live. Derived 2026-10-07 from the permission catalog
(`erp-core/src/main/java/com/erp/<module>/permission/*Permissions.java`), `docs/api-docs/README.md`
and the vendored analysis. It replaces the plan-generator registry of the same name; the
historical version (entity candidates, XM atoms, pipeline status) is in `governance-shared` @
`1087165`.

Conventions: analysis folders are upper-case module codes; api-docs folders are the lower-case
package names. Page codes are the `SEC_SCREEN_REG.PAGE_CODE` values; the permission of an action
on a screen is `PERM_<PAGE_CODE>_<ACTION>`, `VIEW` being the gateway action.

## Modules

| Module | Package | Analysis (`governance/analysis/…`) | Permission module → screens (page codes) | API contract (`docs/api-docs/…`, operations) | Test suites |
|---|---|---|---|---|---|
| SEC — identity, roles, grants, staff and customer auth | `com.erp.sec` | `modules/SEC/` P0–P2 (+ addenda), `implementation-notes.md` · `decisions/SEC/` 13 ADRs | `SEC` → `SEC_LOGIN`, `SEC_SIGNUP`, `SEC_PWD_RESET`, `SEC_USERS`, `SEC_ROLES`, `SEC_MODULE_REGISTRY`, `SEC_DASHBOARD`, `SEC_AUDIT_LOG`, `SEC_SESSIONS` | `sec/` — 40 | `docs/test-api/` (TC-CORE) · `governance/backend/modules/SEC/test-api/` · `governance/frontend/modules/SEC/tests/` (auth, cross-cutting, customer portal, SEC screens) |
| TENANT — platform tenant provisioning | `com.erp.tenant` | — (erp-core plan step 05, `docs/steps/05-report.md`) | `PLATFORM` → `PLATFORM_TENANTS` | `tenant/` — 5 | `docs/test-api/` · `governance/frontend/modules/PLATFORM/tests/` |
| FILE — files, categories, public files | `com.erp.file` | `modules/FILE/` P0–P2_5 (+ addenda) · `decisions/FILE/` none | `FILE` → `FILE_CATEGORIES`, `FILE_BROWSER` | `file/` — 14 | `docs/test-api/` · `governance/backend/modules/FILE/test-api/` · `governance/frontend/modules/FILE/tests/` |
| NOTIF — templates, channels, dispatch, logs, inbox | `com.erp.notif` | `modules/NOTIF/` P0–P2_5 (+ addenda) · `decisions/NOTIF/` none | `NOTIF` → `NOTIF_TEMPLATES`, `NOTIF_CHANNELS`, `NOTIF_LOG` | `notif/` — 18 | `docs/test-api/` · `governance/backend/modules/NOTIF/test-api/` · `governance/frontend/modules/NOTIF/tests/` |
| MDL — master-data lookups | `com.erp.mdl` | `modules/MDL/` P0–P2_5 (+ addenda) · `decisions/MDL/` 13 ADRs | `MDL` → `MDL_LOOKUPS`, `MDL_TYPE_REGISTRY` | `mdl/` — 11 | `governance/backend/modules/MDL/test-api/` · `governance/frontend/modules/MDL/tests/` |
| CU — configuration and settings store | `com.erp.cu` | `modules/CU/` P0–P2 (+ addenda) · `decisions/CU/` none | `CU` → `CU_CONFIGURATIONS`; `PLATFORM` → `PLATFORM_SETTINGS` | `cu/` — 5 | `docs/test-api/` · `governance/backend/modules/CU/test-api/` · `governance/frontend/modules/CU/tests/` |
| SEQUENCE — number series | `com.erp.sequence` | — (erp-core plan step 09) | `SEQUENCE` → `SEQUENCE_SERIES` | `sequence/` — 6 | `docs/test-api/` · `governance/frontend/modules/SEQUENCE/tests/` |
| AUDIT — generic audit events | `com.erp.audit` | — (erp-core plan step 10) | `AUDIT` → `AUDIT_EVENTS` | `audit/` — 1 | `docs/test-api/` · `governance/frontend/modules/AUDIT/tests/` |
| REPORT — report definitions, run and export | `com.erp.report` | — (erp-core plan step 11) | one screen per owning module, `<MODULE>_REPORTS`, plus `<MODULE>:REPORT:<CODE>` authorities, registered automatically from the `ReportProvider`s | `report/` — 4 | `docs/test-api/` · `governance/frontend/modules/REPORT/tests/` |
| APP — the reference application's own endpoints (dev profile) | `com.erp.app` | — | — | `app/` — 1 | `erp-app-reference/src/test/` |

Packages without an HTTP surface: `com.erp.common` (foundation), `com.erp.events` (event bus),
`com.erp.autoconfigure` (wiring). Total documented operations: 105 (`docs/api-docs/README.md`).

## Analysis status per analysed module

| Module | Analysis version | What the addenda record |
|---|---|---|
| SEC | v1 (current). The service-account change set (CS-SEC-001, analysed as SEC v2) was never implemented; its five G5 endpoint declarations (`GET /sec/users/{id}`, `GET /sec/roles/{id}`, `PUT /sec/roles/{id}`, `GET /sec/roles/{id}/grants`, `POST /sec/signup-requests/search`) are the as-built endpoints recorded in ADR-SEC-038 and `docs/api-docs/sec/`; the rest of v2 is not vendored (reference: `governance-shared` @ `1087165`, `analysis/modules/SEC/v2/`). | customer realm (self-registration, verification, customer login and reset), tenancy, the permission catalog replacing the static constants, realm separation of staff APIs, login rate limiting |
| MDL | v1 | tenancy, lookup types owned per module, `OwnedLookups` consumers |
| CU | v1 (legacy path) | the `SettingsApi`, platform defaults and tenant overrides, `PLATFORM_SETTINGS` |
| FILE | v1 (legacy path) | the `StorageProvider` SPI, public files, download tokens, `PUBLISH` action |
| NOTIF | v1 (legacy path) | event-driven delivery, claim lease and retry bounds, `ChannelProvider` SPI, the customer inbox |

The ADRs kept under `governance/analysis/decisions/` are those whose decision still describes
the current code (search endpoints are `POST …/search`, no by-id read in MDL, the SEC lookups
stay CHECK-constrained, every primary key comes from a named sequence, …). Customer login is
throttled in SEC (`CUSTOMER_LOGIN_RATE_LIMITED` 429); staff login is not. ADRs about the
unimplemented SEC v2 change set, about the former plan generator's id binding and review rounds,
and those contradicted by the code (the SEC throttle claims, MDL's index strategy, the
inactive-type guard, the fixed sort field) were dropped — `docs/governance-vendoring-report.md`
lists them.

## Cross-module reads

| From | To | What | Mechanism |
|---|---|---|---|
| MDL | SEC | a lookup type's owner module must exist in the module registry | `crossmodule` call |
| SEC | NOTIF | password-reset, sign-up and customer-verification mails | domain events → NOTIF delivery |
| every module | SEC | screens and actions registered through `PermissionContributor`; reports through `ReportProvider` | catalog synchronisation at start-up |
| every module | TENANT | tenant-scoped rows and provisioning hooks | `TenantContext`, `TenantProvisioningContributor` |
| every module | AUDIT | `@Audited` entities and `AuditApi` | entity listener / in-process API |

No physical foreign key crosses a module boundary; `CrossModuleBoundaryArchTest` enforces the
package rule.
