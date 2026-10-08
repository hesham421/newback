# ERP Platform — project overview (erp-core 1.2.0)

What the platform is today, as implemented. Sources: `docs/CHANGELOG.md`, `docs/CONSUMING.md`,
`docs/api-docs/README.md` and the "Implementation Addendum — erp-core 1.2.0" sections of the
module analysis under `governance/analysis/modules/<MOD>/`. Written 2026-10-07; replaces the
plan-generator rendering of the same name.

## Shape

The platform is a **library plus a reference application**, in one Maven reactor
(`com.erp:erp-platform`):

| Artifact | What it is |
|---|---|
| `com.erp:erp-core` | an auto-configured Spring Boot 4 library (Java 25, PostgreSQL 16). An application adds it as a dependency; everything arrives through `META-INF/spring/…AutoConfiguration.imports`. The application never scans `com.erp.*` and never copies a core class. |
| `erp-app-reference` | the consuming application (`com.erp.app`): the working example, built in CI against the published jar on every release tag, and the instance the API docs and verification suite are generated from (profile `dev`, port 7272). |

Three rules govern consumption (`docs/CONSUMING.md`): the core is never copied or modified by an
application; application logic lives in the application's own package; a change needed by two
applications goes into core, additively, as a new MINOR version.

## The eleven core packages

| Package | Role | HTTP surface | Analysis (`governance/analysis/…`) |
|---|---|---|---|
| `com.erp.common` | the foundation every module consumes: `AuditableEntity`, `Status`/`ServiceResult`, `LocalizedException` + error codes, `ApiResponse`/`GlobalExceptionHandler`, the search builders (`SpecBuilder`, `PageableBuilder`, `SearchRequest`), boolean converters, `SecurityContextHelper`, the shared helpers added in 1.3.0-SNAPSHOT (`FilterErrorResponseWriter`, `StatusTransitions`, `DomainRules`, `OwnedLookups`, …); since 1.3.0 the `Idempotency-Key` mechanism (`idempotency.IdempotentResponses`, table `CORE_IDEMPOTENCY_KEY`, 24 h retention job; first consumer `POST /api/v1/platform/tenants`) | none | `modules/COMMON/` (as-built, 2026-10-08) · `decisions/COMMON/` |
| `com.erp.cu` | configuration and settings store: `SettingsApi` (typed, cached, platform defaults + tenant overrides) and configuration management | `cu/` | `modules/CU/` · `decisions/CU/` |
| `com.erp.mdl` | master-data lookups: lookup types registered by their owning module, values read by key | `mdl/` | `modules/MDL/` · `decisions/MDL/` |
| `com.erp.sec` | identity and access: staff users (since 1.3.0 with phone, job title, preferred language, a public photo, an administrator-set password that must be changed at the next sign-in, the own password change and the staff `/me` profile), roles, the three-level Module → Screen → Action grants, module/screen/action registry, sessions, audit log, dashboard, customer accounts, the permission catalog (`PermissionContributor`) | `sec/` | `modules/SEC/` · `decisions/SEC/` |
| `com.erp.file` | files and categories, the `StorageProvider` SPI (DB, LOCAL, S3), content hashes, public files at stable URLs, single-use download tokens (since 1.3.0 also the image store and the private store of server-generated files, e.g. tenant export archives, which can be restricted to an authority: hidden and 404 without it, bytes removed on delete) | `file/` | `modules/FILE/` · `decisions/FILE/` |
| `com.erp.notif` | templates, channels (`ChannelProvider` SPI), event-driven retried delivery with a claim lease, in-app inbox for staff and customers, dispatch and logs | `notif/` | `modules/NOTIF/` · `decisions/NOTIF/` |
| `com.erp.tenant` | multi-tenancy: `TenantContext`, `TENANT_ID` on every tenant-scoped core table filtered by Hibernate `@TenantId`, tenant provisioning (`POST /api/v1/platform/tenants`) and the `TenantProvisioningContributor` SPI; since 1.3.0 the tenant data export and its `TenantExportContributor` SPI | `tenant/` | `modules/TENANT/` (as-built, 2026-10-07) · `decisions/TENANT/` |
| `com.erp.audit` | the generic, tenant-scoped audit log: `CORE_AUDIT_EVENT`, `AuditApi`, the `@Audited` entity listener (sensitive fields redacted), a query API and a retention job | `audit/` | `modules/AUDIT/` (as-built, 2026-10-08) · `decisions/AUDIT/` |
| `com.erp.events` | the domain event bus (13 core events; `UserPasswordChangedEvent`, `TenantSuspendedEvent` and `TenantActivatedEvent` since 1.3.0) with a tenant-propagating async executor; consumed with `@TransactionalEventListener` | none | `modules/EVENTS/` (as-built, 2026-10-08) · `decisions/EVENTS/` |
| `com.erp.sequence` | tenant-scoped number series: `NumberSeriesApi`, patterns, reset policies, admin API | `sequence/` | `modules/SEQUENCE/` (as-built, 2026-10-08) · `decisions/SEQUENCE/` |
| `com.erp.report` | the `ReportProvider` SPI and registry with automatic report permissions, run and CSV/JSON export, three core reference reports | `report/` | `modules/REPORT/` (as-built, 2026-10-08) · `decisions/REPORT/` |

`com.erp.autoconfigure` wires all of the above (`ErpCoreAutoConfiguration`, security, Flyway,
OpenAPI, file storage, NOTIF) from the `erp.core.*` properties bound by `ErpCoreProperties`; it is
described in `modules/COMMON/`.
Module boundaries are package-based and enforced by the ArchUnit suite
(`erp-core/src/test/java/com/erp/architecture/`); cross-module calls go through each module's
`crossmodule` package.

## Tenancy and realms

- **Tenancy.** Every tenant-scoped core table carries `TENANT_ID` (global: `CORE_TENANT`,
  `SEC_MODULE_REG`, `SEC_SCREEN_REG`, `SEC_ACTION_REG`; `CU_APP_CONFIGURATION.TENANT_ID` is
  nullable — a NULL row is a platform default); the tenant of a request comes from the
  token's `tid` claim or the `X-Tenant-Code` header; `PLATFORM` (id 1) is the seeded platform
  tenant that owns the bootstrap `admin` (`erp.core.security.bootstrap-admin-password`, no
  default) and the platform-level screens (`PLATFORM_TENANTS`, `PLATFORM_SETTINGS`).
  Since 1.3.0 (TM-B) a tenant carries a profile (contact e-mail and phone, country, default language,
  time zone, notes) and editable names (`PUT /api/v1/platform/tenants/{id}`; the code never changes); a
  suspension needs a reason and records who suspended it, when and why (cleared on re-activation, which
  also stores a token cut-off enforced from package C.2); the platform operator can reset the password of
  a tenant's super administrator (`/{id}/admin-reset`) and read a tenant's usage figures (`/{id}/usage`),
  both executed inside that tenant through SEC, FILE and NOTIF cross-module APIs.
  Since 1.3.0 (TM-E) a tenant also has a branding the platform operator sets (decision D5, ADR-TENANT-005): a logo
  (`/{id}/logo`, a PUBLIC FILE document in the tenant's own rows) and an optional brand colour (`/{id}/branding`);
  every user reads it through `GET /api/v1/tenant/me` (either realm) and the login page through the anonymous,
  rate-limited `GET /api/v1/public/tenants/{tenantCode}/branding` (tenant from the path).
  Since 1.3.0 (TM-C5) the platform operator can export a tenant's data (`POST /{id}/export`, ADR-TENANT-006): every
  module writes its rows of the tenant as CSV through the `TenantExportContributor` SPI (applications may add theirs)
  into a ZIP with a manifest, never passwords, tokens, credentials or file bytes; the ZIP is a PRIVATE FILE document of
  PLATFORM restricted to `PLATFORM_TENANT_MANAGE` and downloaded once with FILE's single-use token; bounded by
  `erp.core.tenant.export.max-rows` and `max-concurrent`.
  Since 1.3.0 (TM-C12) a suspension publishes `TenantSuspendedEvent` (SEC ends every open session of the tenant, NOTIF
  holds its queued notifications) and a re-activation `TenantActivatedEvent` (NOTIF sends what it held); the token
  cut-off is enforced: a token issued before the re-activation or before `POST /{id}/revoke-tokens` answers 401
  `TENANT_TOKEN_REVOKED` on every non-public path of both realms (ADR-TENANT-002). Since 1.3.0 (TM-C4) the tenant
  create accepts an optional `Idempotency-Key` (ADR-TENANT-003). `TenantContext` stays a `ThreadLocal`: the
  `ScopedValue` spike was a no-go (ADR-TENANT-004, REJECTED).
- **Realms.** STAFF (`/api/v1/sec/auth/**`, every administrative endpoint) and CUSTOMER
  (`/api/v1/public/customers/**` for register / verify / login / password reset,
  `/api/v1/customers/me/**` for the profile and inbox). A token of one realm is rejected on the
  other's endpoints; staff user, session and dashboard APIs cover STAFF accounts only (1.1.0).
- **Authorization.** Permissions are `PERM_<PAGE_CODE>_<ACTION>` from the catalog each module
  contributes; `VIEW` is the gateway action of a screen; `SYS_ADMIN` is a super role; reports get
  `<MODULE>_REPORTS` screens and `<MODULE>:REPORT:<CODE>` authorities automatically.
- **Public login throttling.** Pre-authentication endpoints are rate limited in SEC
  (`LoginRateLimiter`, bucket4j); since 1.3.0 the anonymous public branding is rate limited per client address
  (`PublicBrandingRateLimitFilter`, `erp.core.tenant.public-branding-rate-limit.*`).
- **Staff passwords (1.3.0).** One password policy (`erp.core.security.password-policy.*`: at least 8
  characters, at most 72 UTF-8 bytes — BCrypt's limit — with a letter and a digit by default); a password chosen by an administrator must be changed by its
  owner before any other STAFF call is served (403 `SEC-403-PASSWORD-CHANGE-REQUIRED`, ADR-SEC-063).

## Versions

| Version | Date | Content |
|---|---|---|
| 1.0.0 | 2026-10-05 | first library release: FIN removed; Testcontainers/embedded-PG test infrastructure; library split + auto-configuration; core Flyway chain `V1..V999` (additive only), applications `V1000+`; multi-tenancy; STAFF/CUSTOMER realms + permission catalog; file storage and public URLs; events + async notifications; sequences + settings API; generic audit log; minimal reporting; ArchUnit/CI/release. Tagged but never published (CI failed). |
| 1.1.0 | 2026-10-05 | first published version. Staff APIs limited to the STAFF realm; NOTIF `REJECTED` delivery status and `RecipientDirectory.emailOf`; EMAIL dispatch uses the account e-mail; generated api-docs, the core test plan and the api-verify report. |
| 1.2.0 | 2026-10-05 | NOTIF claim lease and retry bounds; unknown path → 404; page-offset overflow → 400; wrapped `LocalizedException` answered with its own code; strict tenant resolver before start-up; tenant context cleared per request; `CommonErrorCodes.NOT_FOUND`. **The documented version.** |
| 1.3.0-SNAPSHOT | in progress | shared helpers moved into `com.erp.common` (no behaviour change); JDK 25 required; the tenant-maturity plan — every backend package merged, plan report `docs/steps/tm-plan-report.md` (TM-A … TM-C5 and the closure Z; frontend package F is the frontend repository's): TENANT analysis, single screen / action grant revoke, staff passwords / profile / photo / `/me`, tenant profile, suspension facts, admin-reset and usage, tenant branding, lifecycle events and per-tenant token cut-off, isolation tests, idempotent provisioning, tenant data export with restricted FILE documents; migrations `V16` … `V22` (`docs/CHANGELOG.md` `[Unreleased]`). |

Release policy: `docs/RELEASE.md` (SemVer; MINOR = additive only; core migrations never edited).

## Where the current reference material lives

| Question | Document |
|---|---|
| What does the API look like? | `docs/api-docs/<module>/` — generated from the running reference app, 125 operations across `sec`, `tenant`, `file`, `notif`, `mdl`, `cu`, `sequence`, `audit`, `report`, `app` |
| Why does a module behave as it does? | `governance/analysis/modules/<MOD>/` for SEC, MDL, CU, FILE, NOTIF, TENANT, SEQUENCE, AUDIT, REPORT, EVENTS, COMMON (P0 policies, P0.5 PRD, P1 SRS, P2 DB script, P2.5 UI/UX for FILE, NOTIF, MDL) with their "Implementation Addendum — erp-core 1.2.0" and (unreleased) "— erp-core 1.3.0" sections, the ADRs under `governance/analysis/decisions/<MOD>/`, and `docs/DEVIATIONS.md` |
| Does the analysis describe the code? | `docs/plans/analysis-coverage-review.md` (audit of 2026-10-08) |
| What is the 1.3.0 tenant-maturity work, and where is it recorded? | `docs/plans/tenant-maturity-plan.md` (the plan), `docs/steps/tm-plan-report.md` (the plan report — every backend package merged), the modules' 1.3.0 addenda (one block per package); `docs/plans/tenant-maturity-analysis-reference.md` holds the superseded pre-implementation reference rows (not governance) |
| How is the platform consumed and configured? | `docs/CONSUMING.md` |
| How is it released? | `docs/RELEASE.md`, `docs/CHANGELOG.md` |
| How was it built, step by step? | `erp-core-plan/`, `docs/steps/NN-report.md` |
| How is it verified over HTTP? | `docs/test-api/` (core suite), `governance/backend/modules/<MOD>/test-api/` (legacy adapted suites), `governance/frontend/modules/<MOD>/tests/` (the frontend's E2E archive) |
| Which modules and screens exist? | `governance/analysis/platform/project-registry.md` |
| How does multi-tenancy work (tenant resolution, provisioning, the `TENANT_ID` columns)? | `governance/analysis/modules/TENANT/` (as-built from 1.2.0, with its no-delta 1.2.0 addendum and its "Implementation Addendum — erp-core 1.3.0") and `governance/analysis/decisions/TENANT/` (ADR-TENANT-001 … 006) |

## Known documentation and contract defects

Recorded 2026-10-08, see `docs/plans/analysis-coverage-review.md` §4; re-checked against
origin/main `f48b9ab` after the tenant-maturity plan merged. Each is a defect of a generated
document, a test or a schema detail, not of the analysis; it stays listed until fixed.

- `docs/api-docs/audit/` shows the permission as `AUDIT_EVENT_READ` (the constant name) instead of
  `AUDIT:EVENT:READ`; `docs/api-docs/file/` shows `DOCUMENT_PUBLISH` instead of `FILE:DOCUMENT:PUBLISH`.
- `docs/api-docs/file/endpoints/public-files.md` lists only the 200 response.
- The NOTIF inbox endpoints carry no authorization rule in the generated docs ("not extracted").
- No OpenAPI groups exist for `tenant`, `sequence`, `audit`, `report` (`ErpCoreOpenApiAutoConfiguration`
  declares `sec`, `notif`, `file`, `mdl`, `cu`, `customers`).
- `CrossModuleBoundaryArchTest` names `com.erp.events.crossmodule` and `com.erp.report.crossmodule`,
  packages that hold no class.
- `CORE_AUDIT_EVENT` uses `TIMESTAMP` while the other `CORE_*` tables use `TIMESTAMPTZ`.
