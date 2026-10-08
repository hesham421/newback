## MODULE REGISTRY — التقارير / Report (REPORT)
══════════════════════════════════════════════════════════════════
Module Code    : REPORT   (package `com.erp.report`; permission-registry screens `<MODULE>_REPORTS` under each owning module; api-docs folder `report`)
Bounded context: platform (cross-cutting service)
Layer / Type   : L1 / reporting engine (SPI + registry + shared endpoints)     Execution tier : last core package (erp-core plan step 11)
Source         : AS-BUILT (erp-core 1.2.0; code at main @ 19b19a4)
Knowledge      : erp-core-plan/11-STEP-reporting-minimal.md; docs/steps/11-report.md; docs/DEVIATIONS.md [11]; docs/CONSUMING.md §6; docs/api-docs/report/
Readiness      : READY (built)
══════════════════════════════════════════════════════════════════

Java paths are relative to `erp-core/src/main/java/com/erp/`.

ENTITIES OWNED   (no JPA entity, no table — value objects of the SPI; ids assigned in P1)
| Entity (ar/en) | Kind | PRIVATE / SHARED | Source |
|---|---|---|---|
| تعريف التقرير / Report definition (`ReportProvider` + `ReportParam`) | SPI value object, in-memory (one bean per report) | SHARED (owner) — implemented by SEC, NOTIF, AUDIT and applications | report/ReportProvider.java:25-51; report/ReportParam.java:13-25 |
| نتيجة التقرير / Report result (`ReportResult` + `ReportColumn`) | SPI value object, per run | SHARED (owner) — returned by every provider | report/ReportResult.java:20-32; report/ReportColumn.java:12 |

Not entities, but owned runtime surface (no table): `ReportRegistry` (the in-memory catalog),
`ReportPermissions` (the `PermissionContributor` that writes the catalog rows), `ReportParametersDomain`,
`ReportRunDomain`, `CsvReportWriter`, `ReportService`, `ReportController`, the DTOs
`ReportDefinitionResponse`, `ReportParamResponse`, `ReportColumnResponse`, `ReportRunRequest`,
`ReportExportRequest`, `ReportRunResponse` (report/registry/ReportRegistry.java:37; report/permission/ReportPermissions.java:32;
report/domain/ReportParametersDomain.java:35; report/domain/ReportRunDomain.java:16; report/export/CsvReportWriter.java:35;
report/service/ReportService.java:60; report/controller/ReportController.java:37; report/dto/*.java).

LOOKUPS OWNED
| Lookup key | Description | Initial values | Source |
|---|---|---|---|
| (Java enum `ParamType`) | نوع معامل التقرير / report parameter type | `STRING`, `INTEGER`, `DECIMAL`, `DATE`, `DATETIME`, `BOOLEAN`, `LOOKUP` — a Java enum, not an MDL lookup type | report/ParamType.java:18-26 |
| (Java enum `ColumnType`) | نوع عمود النتيجة / result column type | `STRING`, `INTEGER`, `DECIMAL`, `DATE`, `DATETIME`, `BOOLEAN` | report/ColumnType.java:10-17 |
| (Java enum `ReportRunDomain.ExportFormat`) | صيغة التصدير / export format | `CSV` (`text/csv;charset=UTF-8`, `.csv`), `JSON` (`application/json`, `.json`) | report/domain/ReportRunDomain.java:19-38 |

LOOKUPS CONSUMED
| Lookup key | Owner code | READ-ONLY | Source |
|---|---|---|---|
| any MDL lookup type named by a provider's `ReportParam.lookupKey` (today `NOTIF_CHANNEL`, `NOTIF_STATUS` of `NOTIF_LOG_SUMMARY`) | MDL (type owned by the provider's module) | yes — active codes only, through `MdlLookupApi.readActiveValuesByKey` | report/service/ReportService.java:143-154; notif/report/NotifLogSummaryReport.java:218-219 |

SHARED ENTITIES CONSUMED
None — the module reads no other module's table. (Its permission rows live in SEC's global registry,
written by SEC's catalog synchronizer from `ReportPermissions`; see DEPENDENCIES.)

DEPENDENCIES
| Module code | HARD / SOFT / SPI | What is consumed | Source |
|---|---|---|---|
| SEC | SPI (implements) | `com.erp.sec.permission.PermissionContributor` — `ReportPermissions` declares one screen per owning module, its `VIEW` gateway and one action per report; rows upserted into `SEC_SCREEN_REG` / `SEC_ACTION_REG` at start-up | report/permission/ReportPermissions.java:32, :44-60; sec/permission/PermissionContributor.java:21-35 |
| MDL | SOFT (crossmodule read) | `com.erp.mdl.crossmodule.MdlLookupApi.readActiveValuesByKey(typeKey)` → `LookupOptionView.code()`; MDL's 404 (`MDL_404_TYPE_KEY`) is caught and the parameter reported `REPORT_PARAM_INVALID` | report/service/ReportService.java:143-154; mdl/crossmodule/MdlLookupApi.java:33; mdl/crossmodule/LookupOptionView.java |
| autoconfigure | configuration | `ErpCoreProperties.Report.maxExportRows` (`erp.core.report.max-export-rows`); `com.erp.report` is the last entry of `CORE_PACKAGE_LIST` | autoconfigure/ErpCoreProperties.java:379-384; autoconfigure/ErpCoreAutoConfiguration.java:88-93 |
| common | foundation | `ServiceResult`/`Status`, `LocalizedException`/`ErrorDetail`, `CommonErrorCodes.ACCESS_DENIED`, `PageableBuilder`/`SearchRequest`, `SecurityContextHelper.hasAuthority`, `ApiResponse`/`OperationCode` | report/service/ReportService.java:4-9; report/domain/ReportRunDomain.java:3-5; report/controller/ReportController.java:3-4 |
ROOT: NO. The module owns no data; it is wired by component scan (`CORE_PACKAGE_LIST`) and needs no
auto-configuration class of its own.

EXPOSED SURFACE (consumed by other modules)
| Surface | Kind | Consumers / implementers | XM id (P1) | Source |
|---|---|---|---|---|
| `com.erp.report.ReportProvider` (+ `ReportParam`, `ReportColumn`, `ReportResult`, `ParamType`, `ColumnType`, `ReportAuthorities`) — the module's root package, public in ArchUnit | SPI | SEC (`SecUserListReport`, `SEC_USER_LIST`), NOTIF (`NotifLogSummaryReport`, `NOTIF_LOG_SUMMARY`), AUDIT (`AuditEventListReport`, `AUDIT_EVENT_LIST`), reference app (`AppSmokeReport`, `APP_SMOKE_REPORT`); any application | XM-REPORT-001 | report/ReportProvider.java:25-51; `erp-core/src/test/java/com/erp/architecture/CrossModuleBoundaryArchTest.java:66-68`; sec/report/SecUserListReport.java:43; notif/report/NotifLogSummaryReport.java:178; audit/report/AuditEventListReport.java:303; `erp-app-reference/src/main/java/com/erp/app/report/AppSmokeReport.java:32` |
| `ReportAuthorities.of(module, code)` → `<MODULE>:REPORT:<CODE>` | helper | each provider's own `AUTHORITY` constant, referenced by its same-module `@PreAuthorize` (ArchUnit rule 5 exception) | — | report/ReportAuthorities.java:18-26; `erp-core/src/test/java/com/erp/architecture/CoreLibraryRulesArchTest.java:197-203`, `:243` |
| `com.erp.report.crossmodule` | declared in ArchUnit as the module's crossmodule package | no class exists in it today — the SPI is the root package | — | `erp-core/src/test/java/com/erp/architecture/CrossModuleBoundaryArchTest.java:68` |

PERMISSION MODULE → SCREEN → ACTIONS (registry rows written from code at start-up; nothing seeded by any migration)
| Registry module | Screen (page code) | Action code | Authority | Meaning | Source |
|---|---|---|---|---|---|
| the owning `<MODULE>` of each provider (row not declared here; created by the synchronizer with its code as name when no other contributor names it) | `<MODULE>_REPORTS` — "تقارير <MODULE>" / "<MODULE> Reports" | `VIEW` | `PERM_<MODULE>_REPORTS_VIEW` | gateway action of the screen (RULE-SEC-007): a role's report grants count only together with it | report/permission/ReportPermissions.java:40-47, :52-54, :62-69 |
| `<MODULE>` | `<MODULE>_REPORTS` | `<CODE>` (the report code) | `<MODULE>:REPORT:<CODE>` | see, run and export that one report; names = `titleAr` / `titleEn` | report/permission/ReportPermissions.java:55-58; report/ReportAuthorities.java:18-21 |
Today's rows: `SEC_REPORTS` (`PERM_SEC_REPORTS_VIEW`, `SEC:REPORT:SEC_USER_LIST`), `NOTIF_REPORTS`
(`PERM_NOTIF_REPORTS_VIEW`, `NOTIF:REPORT:NOTIF_LOG_SUMMARY`), `AUDIT_REPORTS` (`PERM_AUDIT_REPORTS_VIEW`,
`AUDIT:REPORT:AUDIT_EVENT_LIST`) and, in the reference application, `APP_REPORTS` (`PERM_APP_REPORTS_VIEW`,
`APP:REPORT:APP_SMOKE_REPORT`). Every tenant's super role (`SYS_ADMIN`) holds them through the catalog;
for a non-super role an administrator grants `PERM_<MODULE>_REPORTS_VIEW` + `<MODULE>:REPORT:<CODE>`
(docs/steps/11-report.md "Notes for later steps").

AUTO-DECISIONS
AUTO: module code REPORT for the analysis folder although the module owns no registry module of its own (its screens hang under each owning module)
  FROM: governance/analysis/platform/project-registry.md (row "REPORT — report definitions, run and export"); docs/api-docs/report/
  IF WRONG: rename the folder; no id changes.
AUTO: `ReportProvider` classified SHARED (owner)
  FROM: four implementers outside the module (SEC, NOTIF, AUDIT, APP)
  IF WRONG: none — the implementations exist.

RESOLVED DECISIONS
| # | Point | Decision | Sources |
|---|---|---|---|
| 1 | Report storage | code-defined catalog, fail-fast registry, no persistence | ADR-REPORT-001 |
| 2 | Report permissions | one screen per owning module, one action per report, checked in code | ADR-REPORT-002 |
| 3 | Export shape | cap + 1 probe, bounded `byte[]`, CSV with BOM and formula guard | ADR-REPORT-003 |

POLICIES OWNED (full text in business-policies-report.md)
POL-REPORT-001, POL-REPORT-002, POL-REPORT-003, POL-REPORT-004, POL-REPORT-005, POL-REPORT-006,
POL-REPORT-007, POL-REPORT-008
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 11
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : none — no report delta is planned for 1.3.0; the TENANT data export of `docs/plans/tenant-maturity-plan.md` C.5 does NOT use the report engine.
Statement      : This addendum records the deltas being implemented for 1.3.0; every row is verified against the code before the tag.

| # | Kind | Delta | Source |
|---|---|---|---|
| — | — | none | docs/plans/tenant-maturity-plan.md C.5 |
