# PLATFORM SUMMARY — التقارير / Report (REPORT)
══════════════════════════════════════════════════════════════════
Profile : erp   Source version : erp-core 1.2.0 (as built)   Analysis : v1 (as-built baseline)
Written : 2026-10-07, from the code at main @ 19b19a4 (1.3.0-SNAPSHOT; the report code is unchanged
          since tag v1.2.0, no behaviour change)
══════════════════════════════════════════════════════════════════

Paths cited below are relative to the repository root, except: Java sources are relative to
`erp-core/src/main/java/com/erp/`, and core migrations (`V<N>__*.sql`) to
`erp-core/src/main/resources/db/migration/core/`. `file:line` points at main @ 19b19a4.

This analysis was NOT produced before the code: the report module was built by erp-core plan step 11
(`erp-core-plan/11-STEP-reporting-minimal.md`, report `docs/steps/11-report.md`). It records what
exists, so that every later reporting change is written as an "Implementation Addendum" on top of it,
like the other modules' 1.2.0 addenda. The full description of the platform as a whole stays in
[`../../SEC/P0/platform-summary.md`](../../SEC/P0/platform-summary.md) → "Implementation Addendum —
erp-core 1.2.0" (the single-copy decision recorded there; the report block is its lines 128–133); this
file describes only the report module's place in it.

## OVERVIEW
التقارير في المنصة "كود لا بيانات": كل وحدة (أو تطبيق) تسجّل تقريرًا بمجرد كشف bean ينفّذ
`ReportProvider`؛ يجمع السجل كل التقارير عند الإقلاع ويرفض الكتالوج غير الصالح فورًا، ويُولِّد لكل
تقرير صلاحيته `<MODULE>:REPORT:<CODE>` على الشاشة `<MODULE>_REPORTS` في كتالوج الصلاحيات تلقائيًا
دون ترحيل. أربع نقاط نهاية مشتركة تعرض التعريفات وتتحقق من المعاملات وتشغّل صفحة واحدة وتصدّر CSV أو
JSON داخل مستأجر المستدعي وضمن سقف صفوف قابل للضبط. [`docs/steps/11-report.md` Summary; ADR-REPORT-001]

Reports are code, not data: a module or an application registers a report by exposing one bean that
implements `ReportProvider`; `ReportRegistry` collects every such bean at start-up and refuses an
invalid catalog by failing the start-up; `ReportPermissions` turns every report into the authority
`<MODULE>:REPORT:<CODE>` on the screen `<MODULE>_REPORTS` of the RBAC catalog, with no migration. Four
shared endpoints list the definitions, validate the parameters, run one page and export CSV or JSON,
inside the caller's tenant and under a configurable row cap.

## THE REPORT MODULE IN THE PLATFORM (as built)
| Aspect | As built | Code location |
|---|---|---|
| SPI (public surface) | the root package `com.erp.report`: `ReportProvider` (`code`, `moduleCode`, `titleAr`, `titleEn`, `params`, `run(params, Pageable)`), the records `ReportParam`, `ReportColumn`, `ReportResult(columns, rows, totals, totalRows)`, the enums `ParamType`, `ColumnType`, and `ReportAuthorities.of(module, code)` | report/ReportProvider.java:25-51; report/ReportParam.java:13-25; report/ReportColumn.java:12; report/ReportResult.java:20-32; report/ParamType.java:18-26; report/ColumnType.java:10-17; report/ReportAuthorities.java:9-27; `erp-core/src/test/java/com/erp/architecture/CrossModuleBoundaryArchTest.java:66-68` |
| Registry | `ReportRegistry` collects every `ReportProvider` bean (`ObjectProvider.orderedStream`), validates it and keeps the catalog ordered by module code then code; in memory only, no table | report/registry/ReportRegistry.java:44-66, :69-76 |
| Fail-fast catalog | an `IllegalStateException` naming the provider class at start-up for: a duplicate code; a code not `^[A-Z][A-Z0-9_]{1,39}$`; a module code not `^[A-Z][A-Z0-9_]{0,9}$`; a blank title; a parameter without name or type; a duplicate parameter; a `LOOKUP` parameter without key | report/registry/ReportRegistry.java:39-40, :54-58, :78-101 |
| Permissions | `ReportPermissions` (`PermissionContributor` fed by the registry): one screen `<MODULE>_REPORTS` per owning module (names "تقارير <MODULE>" / "<MODULE> Reports"), its `VIEW` gateway `PERM_<MODULE>_REPORTS_VIEW`, one action per report (action code = report code, authority `<MODULE>:REPORT:<CODE>`, names = the report titles); no module rows declared | report/permission/ReportPermissions.java:30-70 |
| Endpoints (STAFF chain) | `GET /api/v1/report/definitions`, `GET /api/v1/report/definitions/{code}`, `POST /api/v1/report/{code}/run`, `POST /api/v1/report/{code}/export?format=csv\|json` | report/controller/ReportController.java:33-76; `docs/api-docs/report/index.md:114-121` |
| Authorization | every service method `@PreAuthorize("isAuthenticated()")`; the per-report authority is checked in code (`SecurityContextHelper.hasAuthority`) and decided by `ReportRunDomain.assertCanRun` → 403 `ACCESS_DENIED`; the definitions list is filtered to the reports the caller holds; super roles hold every catalog authority | report/service/ReportService.java:72-80, :126-132; report/domain/ReportRunDomain.java:44-49 |
| Parameters | `ReportParametersDomain` validates and converts every raw value by its `ParamType`, rejects undeclared names, lists every failure under `REPORT_PARAM_INVALID` (400) in `fieldErrors`; `LOOKUP` values must be active codes read through `MdlLookupApi.readActiveValuesByKey` | report/domain/ReportParametersDomain.java:58-87; report/service/ReportService.java:143-154 |
| Run | one page through `PageableBuilder` (size 1..200, default 20), inside a read-only transaction on the request thread, whose tenant the security chain has set; the provider's JPA queries are tenant-filtered by Hibernate | report/service/ReportService.java:89-104; report/dto/ReportRunRequest.java:22-31 |
| Export | the provider is asked for one page of `max-export-rows + 1` rows; over the cap (rows or `totalRows`) → 422 `REPORT_EXPORT_TOO_LARGE`; CSV (`CsvReportWriter`: UTF-8 BOM, CRLF, RFC 4180 quoting, header language by `Accept-Language`, formula-injection guard) or JSON (`ReportRunResponse` without envelope); a `byte[]` attachment `<code>.csv` / `<code>.json` | report/service/ReportService.java:106-124; report/domain/ReportRunDomain.java:55-75; report/export/CsvReportWriter.java:35-107; report/controller/ReportController.java:62-75 |
| Errors | `REPORT_NOT_FOUND` 404, `REPORT_PARAM_INVALID` 400, `REPORT_EXPORT_TOO_LARGE` 422; common `ACCESS_DENIED` 403; `REALM_MISMATCH` 403 for a customer token (staff chain) | report/exception/ReportErrorCodes.java:15-26; `erp-core/src/main/resources/i18n/messages.properties:178-180`, `messages_ar.properties:175-177` |
| Configuration | `erp.core.report.max-export-rows` (`@Positive`, default 100 000) | autoconfigure/ErpCoreProperties.java:379-384 |
| Core providers | `SEC_USER_LIST` (SEC), `NOTIF_LOG_SUMMARY` (NOTIF), `AUDIT_EVENT_LIST` (AUDIT); the reference application adds `APP_SMOKE_REPORT` (APP) | sec/report/SecUserListReport.java:43-48; notif/report/NotifLogSummaryReport.java:178-183; audit/report/AuditEventListReport.java:303-308; `erp-app-reference/src/main/java/com/erp/app/report/AppSmokeReport.java:32-37` |
| Persistence | none: no table, no migration, no entity; the registry is in memory and the permission rows are written into SEC's global catalog by `PermissionCatalogSynchronizer` at start-up | `docs/steps/11-report.md` "Untouched: every migration"; `../P2/db-script-report.md` |

## REALMS INTERPLAY
| Realm | Report handling | Code location |
|---|---|---|
| STAFF (core chain) | `/api/v1/report/**` lives on the staff chain; a staff caller sees and runs the reports whose `<MODULE>:REPORT:<CODE>` its token carries (a super role holds all) | report/service/ReportService.java:47-52; `docs/DEVIATIONS.md` [11] "Authorization" |
| CUSTOMER (customer chain) | no report endpoint; a customer token on `/api/v1/report/**` answers 403 `REALM_MISMATCH` from the staff chain's realm filter | `docs/steps/11-report.md` Endpoints; `erp-core/src/test/java/com/erp/report/ReportApiIntegrationTest.java` (`customerToken_isRejectedWith403`) |
| Both | a provider runs under the caller's tenant only; `SEC_USER_LIST` lists both realms of that tenant (parameter `realm`), never a password hash | sec/report/SecUserListReport.java:35-40, :88-93 |

## DEPENDENCY MAP
```
SEC, NOTIF, AUDIT, APP (reference app) ──implement (ReportProvider, XM-REPORT-001)──▶ REPORT
REPORT ──SPI (PermissionContributor: ReportPermissions, XM-REPORT-002)──▶ SEC (SEC_SCREEN_REG / SEC_ACTION_REG rows)
REPORT ──crossmodule (MdlLookupApi.readActiveValuesByKey, XM-REPORT-003)──▶ MDL
REPORT ──configuration (ErpCoreProperties.report)──▶ autoconfigure
REPORT ──foundation (ServiceResult, LocalizedException, PageableBuilder, SecurityContextHelper, ApiResponse)──▶ common
```
Build order: REPORT is the last core package of `CORE_PACKAGE_LIST` (autoconfigure/ErpCoreAutoConfiguration.java:88-93);
it was built after SEC (catalog), MDL (lookups), AUDIT (the audit report) and NOTIF (the log summary).
Tier: cross-cutting service (L1), no table.

## DEFERRED (not in scope of the as-built module)
| Item | Reason / activation trigger |
|---|---|
| Persisted report definitions, scheduling, saved parameters, charts, PDF/XLSX export | not built: the step's goal is a minimal tabular engine (`docs/steps/11-report.md` Summary); ADR-REPORT-001 |
| Streaming export | not built: the body is a bounded `byte[]` (ADR-REPORT-003; `docs/DEVIATIONS.md` [11] "streams `text/csv`") |
| Totals in CSV | JSON only (`docs/DEVIATIONS.md` [11] "Export formats and CSV rules") |
| An OpenAPI group `report` | not added; the endpoints appear in the aggregate `/v3/api-docs` (`docs/DEVIATIONS.md` [11] "Property and OpenAPI") |
| A `LOOKUP` parameter whose lookup type is missing in the caller's tenant can surface as a 500 on commit | known limitation (`docs/steps/11-report.md` "Known limitation"); all core lookup keys are seeded per tenant |
| The TENANT data export of the tenant-maturity plan (C.5) | does not use the report engine (`docs/plans/tenant-maturity-plan.md` C.5: own SPI `TenantExportContributor`, ZIP of CSV streams stored as a `FILE_DOCUMENT`) |

## OPEN ITEMS
None — this file describes what exists. Behaviour the code does not have is listed under DEFERRED,
never described as present.

## NEXT STEP
Module registry and policies: `module-registry-report.md`, `business-policies-report.md`.

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 11
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : none — no report delta is planned for 1.3.0; the TENANT data export of `docs/plans/tenant-maturity-plan.md` C.5 does NOT use the report engine (own `TenantExportContributor` SPI, ZIP stored as a `FILE_DOCUMENT`).
Statement      : This addendum records the deltas being implemented for 1.3.0; every row is verified against the code before the tag.

| # | Kind | Delta | Source |
|---|---|---|---|
| — | — | none | docs/plans/tenant-maturity-plan.md C.5; docs/CHANGELOG.md [Unreleased] (no report entry) |
