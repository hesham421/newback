# PLATFORM SUMMARY — منصة تخطيط موارد المؤسسات (ERP Platform)
══════════════════════════════════════════════════════════════════
Profile : erp   Domain profile : v1   Registry : v1.0.0
══════════════════════════════════════════════════════════════════

## OVERVIEW
منصة ERP متعددة الوحدات، قائمة على مبدأ "كل ما يمكن أن يتغير = بيانات، لا شيفرة"،
تُبنى بـ Spring (خلفية) وReact (واجهة) على PostgreSQL كهدف بناء وحيد؛ النظام القديم
Oracle/ADF يبقى مصدر أحداث فقط. هذه الدفعة تُنشئ ثلاث وحدات تأسيسية بالترتيب الصارم
SEC ← MDL ← FIN؛ باقي وحدات المنصة (ORG, PRC, HR, INV, SLS, CTR) معروفة الرمز
والسياق من `profiles/erp.yaml` لكنها خارج نطاق هذه الدفعة. [domain-profile §1-§2]

## MODULES
| #   | Code | Module (ar/en) | Bounded context | Layer | Type | Depends on | Status |
|-----|------|--------|-----------------|-------|------|------------|--------|
| 1.1 | ORG | الهيكل التنظيمي / Organization | organization | L1 | master data | ROOT | NEW (not this batch) |
| 1.2 | SEC | الأمان / Security | organization | L1 | security engine | ROOT | NEW — this batch, first |
| 1.3 | MDL | البيانات المرجعية / Master Data Lookup | organization | L1 | reference | ROOT | NEW (not this batch) |
| 2.1 | PRC | المشتريات / Procurement | supply | L3 | transactional | SEC, MDL (SOFT/HARD, not yet detailed) | NEW (not this batch) |
| 2.2 | FIN | الحسابات العامة / Finance (GL) | finance | L3 | transactional/reporting | SEC (HARD), MDL (HARD) | NEW — this batch, third |
| 2.3 | INV | المخزون / Inventory | supply | L3 | transactional | SEC, MDL (not yet detailed) | NEW (not this batch) |
| 3.1 | SLS | المبيعات / Sales | commercial | L4 | transactional | SEC, MDL (not yet detailed) | NEW (not this batch) |
| 3.2 | CTR | العقود / Contracts | commercial | L4 | transactional | SEC, MDL (not yet detailed) | NEW (not this batch) |
| 3.3 | HR | الموارد البشرية / Human Resources | people | L4 | transactional | SEC, MDL (not yet detailed) | NEW (not this batch) |
Status: NEW (Phase 2 produces) · EXISTING (Phase 2 extends) · EXCEPTION (read as-is)
Numbering: [tier].[sequence within tier] — the user requests Phase 2 by this number.
This run's Phase 2 request: **1.2 SEC** (module convergence below).

## DEPENDENCY MAP
Build order: Tier 1 [ORG, SEC, MDL] → Tier 2 [PRC, FIN, INV] → Tier 3 [SLS, CTR, HR] → Tier 4 (reporting, none yet)
Key dependencies (one line each):
  FIN → HARD → SEC : identity + module/screen/action grants + SoD (entry-creator ≠ period-close approver)
  FIN → HARD → MDL : payment methods, accounting event types, account types, period states, journal types
  SEC → SOFT → NOTIF (external, ready) : password-reset message, optional only
  FIN → SOFT → NOTIF (external, ready) : period-close-awaiting notice, optional only
  FIN → SOFT → FILESVC (external, ready) : statement export, optional only
  (host business system) → EVENT → FIN : canonical accounting event only, no table read/write

## DEFERRED (not in scope for this version)
| Item | Reason / activation trigger |
| ORG, PRC, HR, INV, SLS, CTR detailed analysis | user-scoped this batch to SEC → MDL → FIN only (GENERATION-INSTRUCTIONS.md §3) |
| Workflow engine | profile: `forbidden` |
| Notifications / File Service redesign | ready external modules; consumed only on real need, never re-specified [security-module-plan-en.md §8; lookup-module-plan-en.md §5; general-accounting-system-plan-en.md §2.3] |
| Multi-currency, multi-ledger/entity, statistical accounts, multi-pattern calendar, attachments (FIN) | excluded by explicit decision [general-accounting-system-plan-en.md §15] |

## RESOLVED DECISIONS (this phase)
| # | Point | Recommended | Confirmed by user | Sources |
|---|---|---|---|---|
| 1 | Numbering/tiering of the 9 platform modules | Foundation=1.x, Core business=2.x, Extended business=3.x, per KB §1 tiers | Yes — no conflicting statement in the plans | [KB:erp-domain-standards §1] |
| 2 | Phase 2 request for this P0 run | Module 1.2 SEC (first of the batch order SEC→MDL→FIN) | Yes — GENERATION-INSTRUCTIONS.md §3 | GENERATION-INSTRUCTIONS.md §3 |

## OPEN ITEMS
None — platform scope fully determined for this batch (SEC, MDL, FIN); the other six
modules are out of scope, not ambiguous — no registry ↔ vision conflict exists.

## NEXT STEP
Module 1.2 SEC converges below (module-registry-sec.md, business-policies-sec.md).
Reply with a plain instruction to adjust, or request module 1.3 (MDL) next per the
mandated order.

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 01–12 (plan), 14 (Phase D fixes, shipped in 1.1.0), 15 (hardening, shipped in 1.2.0)
Revised        : 2026-10-08 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Paths cited below are relative to the erp-core repository at that tag. Each row is labelled NEW /
CHANGED / REMOVED against the analysis above.

### Decision — where the platform description lives
`platform-summary.md` exists once per module folder. To avoid five diverging copies, the FULL
description of the implemented platform (shape, the new core modules that have no analysis folder,
conventions, release policy) is recorded ONLY in this file
(`analysis/modules/SEC/P0/platform-summary.md`). The `platform-summary.md` of NOTIF, FILE, MDL and CU
each carry a short addendum that references this file by relative path and lists only that module's own
platform-relevant changes.

### Platform shape (as implemented)
| Kind | Aspect | Implemented | Source |
|---|---|---|---|
| CHANGED | Delivery form | `com.erp:erp-core` — a versioned, auto-configured Spring Boot library (Java 21 at 1.2.0, Spring Boot 4.0.1, PostgreSQL 16, Flyway, `ddl-auto=none`), consumed as a Maven dependency from GitHub Packages. Everything arrives through `AutoConfiguration.imports`; an application never scans `com.erp.*` and never copies or patches a core class. | 00-README-EXECUTION-PLAN.md §2, §4; docs/CONSUMING.md; docs/steps/03-report.md |
| CHANGED | Toolchain | JDK 25 is required since `1.3.0-SNAPSHOT` (`maven.compiler.release=25`, enforced at `validate`; `.sdkmanrc` = `java=25-tem`, CI = Temurin 25). The "Java 21" of the Delivery form row describes the 1.2.0 tag. | pom.xml; CLAUDE.md (Versions) |
| NEW | Reference consumer | `erp-app-reference` (runnable app, `com.erp.app`) proves consumption; CI builds it against the published artifact on every release tag. | docs/RELEASE.md; DEVIATIONS [12] (CI) |
| REMOVED | Business modules | None in core. The `fin` module was deleted completely (code, 18 migrations, seeds, 30 `PERM_FIN_*` permissions, OpenAPI group, i18n) and is never restored. Business modules (Phase 4) come later through the Governance Factory. The batch order "SEC → MDL → FIN" above no longer applies to erp-core. | 01-STEP; docs/steps/01-report.md; plan README §6 |
| CHANGED | Core packages | `com.erp.{common, cu, mdl, sec, file, notif, tenant, events, sequence, audit, report}` + `autoconfigure` (wiring only) | DEVIATIONS [10] (rebase, `CORE_PACKAGES`), [11] |
| CHANGED | Configuration | All core keys are `erp.core.*`, bound by `ErpCoreProperties` (old `app.*` / `file.*` keys renamed). Required: DB, `erp.core.security.jwt.secret`, `erp.core.files.access-token-secret`. Redis, SMTP, the S3 SDK and springdoc are optional. | docs/steps/03-report.md; DEVIATIONS [03] |

### Core modules — analysis coverage
| Kind | Module | Package | Analysis folder | Notes |
|---|---|---|---|---|
| CHANGED | SEC | `com.erp.sec` | `analysis/modules/SEC` | + realms, permission SPI, super role, logout, sequences (see the SEC addenda) |
| CHANGED | MDL | `com.erp.mdl` | `analysis/modules/MDL` | tenant-scoped catalog |
| CHANGED | CU | `com.erp.cu` (+ `com.erp.common` foundation) | `analysis/modules/CU` | + settings: platform default / tenant override |
| CHANGED | FILE | `com.erp.file` | `analysis/modules/FILE` | + storage SPI, public files |
| CHANGED | NOTIF | `com.erp.notif` | `analysis/modules/NOTIF` | + async delivery, channel SPI, in-app inbox |
| NEW | Tenant | `com.erp.tenant` | `analysis/modules/TENANT` (added 2026-10-07, as-built) | described below |
| NEW | Audit | `com.erp.audit` | none | described below |
| NEW | Events | `com.erp.events` | none | described below |
| NEW | Sequence | `com.erp.sequence` | none | described below |
| NEW | Report | `com.erp.report` | none | described below |

### New core modules without an analysis folder at 1.2.0

**Tenant (`com.erp.tenant`) — step 05.** Row-level multi-tenancy in one shared schema.
- `CORE_TENANT` (global): `CODE` `^[A-Z0-9_]{3,32}$` (immutable), bilingual name, `STATUS_CODE` ACTIVE | SUSPENDED. PLATFORM = id 1, cannot be suspended.
- Every tenant-scoped row carries `TENANT_ID`, filled and filtered by Hibernate `@TenantId` on every query, join and load by id. `TenantContext` (ThreadLocal; `runAs` / `callAs` for jobs and listeners).
- Resolution per request: token claim `tid`, then header `X-Tenant-Code`, then (public files only) the path segment `{tenantCode}`. A public non-exempt path without a tenant → 400 `TENANT_REQUIRED`; unknown code → 404 `TENANT_NOT_FOUND`; suspended → 403 `TENANT_SUSPENDED` (also for tokens already issued). Since 1.2.0 the resolver is strict before the web server accepts requests, and a tenant leaked on a pooled thread is logged and cleared (step 15).
- API `/api/v1/platform/tenants`: `POST` (provision a tenant and its first administrator), `GET` (paged), `GET /{id}`, `POST /search`, `PATCH /{id}/status`. Requires `PLATFORM_TENANT_MANAGE` (+ gateway `PERM_PLATFORM_TENANTS_VIEW`) and an authenticated caller of the PLATFORM tenant.
- Provisioning SPI `TenantProvisioningContributor`, run in the same transaction as the tenant insert: SEC (roles, grants, first admin), MDL (lookup catalog), NOTIF (channel configs without credentials, templates without attachment), sequence (series, counter back to 1). Cross-module read `TenantLookupApi.codeOf(tenantId)`.
- Errors: `TENANT_REQUIRED` 400, `TENANT_NOT_FOUND` 404, `TENANT_SUSPENDED` 403, `TENANT_CONTEXT_MISSING` 500, `TENANT_CODE_INVALID` 400, `TENANT_CODE_DUPLICATE` 409, `TENANT_PLATFORM_PROTECTED` 422.
- Sources: 05-STEP; docs/steps/05-report.md; DEVIATIONS [05], [07] (path tenant), [09] (sequence contributor), [15]; docs/api-docs/tenant/index.md. The module's own analysis (`analysis/modules/TENANT/`) was written as-built on 2026-10-07 and is the current reference for it.

**Audit (`com.erp.audit`) — step 10.** One cross-module "who changed what, when" store.
- `CORE_AUDIT_EVENT` (tenant-scoped): actor, actor realm (STAFF | CUSTOMER | SYSTEM), action (`^[A-Z_]{3,64}$`), entity type / id, bilingual summary, `CHANGES` JSONB (`[{field, old, new}]`), IP, user agent, reference.
- Writers: explicit `AuditApi.record(...)` and the opt-in `@Audited` Hibernate listener, both synchronous inside the writer's transaction (a rolled-back change leaves no row). Audited entities: `SEC_USER`, `SEC_ROLE`, `CORE_TENANT`, `FILE_DOCUMENT`, `FILE_CATEGORY`, `NOTIF_TEMPLATE`, `MDL_LOOKUP_TYPE`, `MDL_LOOKUP_VALUE`, `CU_APP_CONFIGURATION`, `CORE_NUMBER_SERIES`. Field names containing password, secret, token, hash, credential, apikey, privatekey or salt are never recorded.
- Query: `GET /api/v1/audit/events` (filters `entityType`, `entityId`, `actor`, `action`, `from` / `to`; newest first), authority `AUDIT:EVENT:READ`. Retention: `erp.core.audit.retention-days` (0 = keep forever) + optional cron. Error `AUDIT_ACTION_INVALID` 400.
- `SEC_AUDIT_LOG` stays as the security-specific log; SEC additionally writes LOGIN / LOGOUT / PASSWORD_RESET here.
- Sources: 10-STEP; docs/steps/10-report.md; DEVIATIONS [10]; V15__audit_schema.sql.

**Events (`com.erp.events`) — step 08.** In-process domain event bus (no broker).
- `DomainEvent` carries id, `occurredAt`, `tenantId`, actor, realm. 10 core events: `UserCreatedEvent`, `UserStatusChangedEvent`, `CustomerRegisteredEvent`, `CustomerVerifiedEvent`, `PasswordResetRequestedEvent`, `TenantCreatedEvent`, `FileDocumentPublishedEvent`, `NotificationRequestedEvent`, `NotificationDispatchedEvent`, `NotificationFailedEvent`.
- Published inside the writing transaction; listeners use `@TransactionalEventListener(AFTER_COMMIT)` and run on `erpCoreEventExecutor`, which propagates `TenantContext` and the `SecurityContext` to the worker thread.
- Sources: 08-STEP; docs/steps/08-report.md; docs/CONSUMING.md §5.

**Sequence (`com.erp.sequence`) — step 09.** Tenant-scoped document number series.
- `CORE_NUMBER_SERIES`: one row per (code, period); pattern tokens `{PREFIX} {YYYY} {YY} {MM} {SEQ:n} {TENANT}` (exactly one `{SEQ:n}`, 1 ≤ n ≤ 18); reset policy NEVER | YEARLY | MONTHLY.
- `NumberSeriesApi.next(code)` (REQUIRES_NEW, row lock, consecutive; a rolled-back caller leaves a gap) and `preview(code)`. Admin API `/api/v1/sequence/series` (create, get, search, update, activate, deactivate; no delete — issued numbers are never reissued), authorities `PERM_SEQUENCE_SERIES_VIEW` / `PERM_SEQUENCE_SERIES_MANAGE`.
- Errors: `SEQUENCE_NOT_CONFIGURED` 422, `SEQUENCE_PATTERN_INVALID`, `NUMBER_SERIES_NOT_FOUND` 404, `NUMBER_SERIES_CODE_DUPLICATE` 409.
- Sources: 09-STEP; docs/steps/09-report.md; DEVIATIONS [09]; V14__sequence_and_settings.sql.

**Report (`com.erp.report`) — step 11.** Minimal tabular reporting.
- SPI `ReportProvider` (code, owning module, typed parameters STRING / INTEGER / DECIMAL / DATE / DATETIME / BOOLEAN / LOOKUP, columns, `run(params, pageable)`) + `ReportRegistry`. Each report gets the authority `<MODULE>:REPORT:<CODE>` on screen `<MODULE>_REPORTS` (gateway `PERM_<MODULE>_REPORTS_VIEW`), synchronized automatically — no migration.
- API: `GET /api/v1/report/definitions`, `GET /api/v1/report/definitions/{code}`, `POST /api/v1/report/{code}/run` (one page, JSON), `POST /api/v1/report/{code}/export?format=csv|json` (CSV: UTF-8 BOM, CRLF, header language from `Accept-Language`, spreadsheet-formula guard; cap `erp.core.report.max-export-rows`, default 100 000).
- Core reference reports: `AUDIT_EVENT_LIST` (audit), `NOTIF_LOG_SUMMARY` (NOTIF), `SEC_USER_LIST` (SEC).
- Errors: `REPORT_NOT_FOUND` 404, `REPORT_PARAM_INVALID` 400, `REPORT_EXPORT_TOO_LARGE` 422; a caller without the report authority gets 403 `ACCESS_DENIED`.
- Sources: 11-STEP; docs/steps/11-report.md; DEVIATIONS [11]; docs/api-docs/report/index.md.

### Platform conventions (as implemented)
| Kind | Convention | Rule | Source |
|---|---|---|---|
| NEW | Tenant column | Every tenant-scoped table: `TENANT_ID BIGINT NOT NULL` (no default), FK `FK_<TABLE>_TENANT` → `CORE_TENANT(ID)`, index `IDX_<TABLE>_TENANT`; every unique constraint starts with `TENANT_ID`. Global tables only where a step names them: `CORE_TENANT`, `SEC_MODULE_REG`, `SEC_SCREEN_REG`, `SEC_ACTION_REG`, and `CU_APP_CONFIGURATION` (nullable `TENANT_ID`, NULL = platform default). | db/migration/core/README.md; DEVIATIONS [12] (rule 2) |
| NEW | Base entities | `AuditableEntity` (tenant-scoped, `@TenantId`) extends `GlobalAuditableEntity` (audit columns + `@Version`). | DEVIATIONS [05] |
| NEW | Optimistic lock | `VERSION BIGINT NOT NULL DEFAULT 0` on every core table; a lost update answers 409 `CONCURRENT_MODIFICATION`. | V10__tenant_schema.sql; DEVIATIONS [05] |
| CHANGED | Response envelope | `ApiResponse<T>` (`success`, `data`, `error{code, message, fieldErrors}`, `timestamp`); the same shape as analysed. | docs/api-docs/*/index.md |
| CHANGED | Errors | `LocalizedException` with a registered code; message AR (`messages_ar.properties`) + EN (base `messages.properties`). Since 1.2.0: unknown path → 404 `NOT_FOUND`; page-offset overflow → 400 `VALIDATION_ERROR`; a wrapped `LocalizedException` answers with its own code and status. | docs/CHANGELOG.md [1.2.0]; DEVIATIONS [01], [15] |
| CHANGED | Migrations | Core owns `V1..V999` in `classpath:db/migration/core` (chain `V2..V15`; `V1` unused), applications own `V1000+`. The historical chain was squashed into `V2..V9` in step 04 (old → new mapping in docs/steps/04-report.md). After `V9` core scripts are additive only, enforced by `MigrationNamingTest`. | docs/steps/04-report.md; db/migration/core/README.md |
| CHANGED | Naming | Tables `<MODULE>_<NAME>`; new core tables `CORE_*`; every table uses `ID BIGINT` (SEC: `<TABLE>_PK`) + a per-table named sequence, never IDENTITY; new booleans `BOOLEAN`. | plan README §4; V4__sec_schema.sql:17-31 |
| CHANGED | Endpoints | `/api/v1/<module>/...`; unauthenticated endpoints under `/api/v1/public/...`. Two security chains: customer chain (`/api/v1/public/**`, `/api/v1/customers/**`) and staff chain (everything else). | docs/CONSUMING.md §4 |
| NEW | Module boundaries | ArchUnit: `CrossModuleBoundaryArchTest` (cross-module access only through `crossmodule` / root SPI packages) and `CoreLibraryRulesArchTest` (no dependency on `com.erp.app..`, global-entity allow-list, `@PreAuthorize` must name its own module's `*Permissions` class, raw JDBC only in tenant / sequence / audit / provisioning contributors / requeue job). | DEVIATIONS [12] |

### Release and versioning
- Semantic versioning (`docs/RELEASE.md`): MINOR = additive only (modules, tables, nullable or defaulted columns, endpoints, events, SPI methods with a default, permissions, error codes); MAJOR expected never. Public API = `crossmodule` packages, the SPIs, `com.erp.events`, `TenantContext`, `com.erp.common.*`, REST contracts, `erp.core.*` keys, core tables.
- History: `v1.0.0` tagged but never published (CI failed); `1.1.0` first published (Phase D fixes: staff APIs STAFF-only, NOTIF e-mail addressing); `1.2.0` hardening (NOTIF claim/lease, 404/400 instead of 500, tenant bootstrap window). A published version is never re-tagged.
- Verification at 1.2.0: the governed core API test plan (`docs/test-api/core-test-plan.md`) executed by api-verify, 172 / 172 passing (`docs/test-api/core-verify-report.md`); api-docs generated from the running application for 105 operations (`docs/api-docs/`).
- Sources: docs/CHANGELOG.md; docs/RELEASE.md; DEVIATIONS [12], [12-ci], [15].
