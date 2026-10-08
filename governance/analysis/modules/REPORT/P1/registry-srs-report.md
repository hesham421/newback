## REGISTRY — P1 — REPORT v1 (as-built baseline, erp-core 1.2.0)
══════════════════════════════════════════════════════════════════

Every id below is traced to the code it was read from (main @ 19b19a4). Java paths are relative to
`erp-core/src/main/java/com/erp/`.

Entities
| ENT id | Name (ar/en) | Kind | PRIVATE/SHARED | Status | Code location |
|---|---|---|---|---|---|
| ENT-REPORT-001 | تعريف التقرير / Report definition (`ReportProvider` + `ReportParam`) | SPI value object, in memory (no table) | SHARED (owner) | REGISTERED (built) | report/ReportProvider.java:25-51; report/ReportParam.java:13-25 |
| ENT-REPORT-002 | نتيجة التقرير / Report result (`ReportResult` + `ReportColumn`) | SPI value object, per run (no table) | SHARED (owner) | REGISTERED (built) | report/ReportResult.java:20-32; report/ReportColumn.java:12 |

Consumed
| Owner | What | Kind | XM | Code location |
|---|---|---|---|---|
| MDL | active values of the lookup type named by a `LOOKUP` parameter (`MdlLookupApi.readActiveValuesByKey` → `LookupOptionView.code`) | SOFT-READ (crossmodule) | XM-REPORT-003 | report/service/ReportService.java:143-154 |
| SEC | `PermissionContributor` SPI (implemented) | SPI | XM-REPORT-002 | report/permission/ReportPermissions.java:32 |

Cross-module surfaces (exposed direction)
| XM id | Surface | Kind | Consumers / implementers | Status | Code location |
|---|---|---|---|---|---|
| XM-REPORT-001 | `com.erp.report.ReportProvider` + `ReportParam`, `ReportColumn`, `ReportResult`, `ParamType`, `ColumnType`, `ReportAuthorities` (root package) | SPI | SEC `SecUserListReport`, NOTIF `NotifLogSummaryReport`, AUDIT `AuditEventListReport`, reference app `AppSmokeReport`; any application | ACTIVE (built) | report/ReportProvider.java:25-51; `erp-core/src/test/java/com/erp/architecture/CrossModuleBoundaryArchTest.java:66-68` |
| XM-REPORT-002 | catalog rows `<MODULE>_REPORTS`, `PERM_<MODULE>_REPORTS_VIEW`, `<MODULE>:REPORT:<CODE>` contributed to `SEC_SCREEN_REG` / `SEC_ACTION_REG` | registry rows (widths: `../P2/db-script-report.md`) | SEC synchronizer, role editor, frontend menu | ACTIVE (built) | report/permission/ReportPermissions.java:44-70 |
| XM-REPORT-003 | `MdlLookupApi.readActiveValuesByKey` (consume direction) | crossmodule read | REPORT → MDL | ACTIVE (built) | report/service/ReportService.java:143-154 |
Note: `com.erp.report.crossmodule` is declared in ArchUnit as the module's crossmodule package but holds
no class; the SPI is the root package.

Lookups owned
| Key | ENT | Values count | Code location |
|---|---|---|---|
| `ParamType` (Java enum) | ENT-REPORT-001 | 7 (STRING, INTEGER, DECIMAL, DATE, DATETIME, BOOLEAN, LOOKUP) | report/ParamType.java:18-26 |
| `ColumnType` (Java enum) | ENT-REPORT-002 | 6 (the same without LOOKUP) | report/ColumnType.java:10-17 |
| `ReportRunDomain.ExportFormat` (Java enum) | — | 2 (CSV, JSON) | report/domain/ReportRunDomain.java:19-38 |

Lookups consumed
| Key | Owner | Code location |
|---|---|---|
| any MDL type named by a provider's `lookupKey` (today `NOTIF_CHANNEL`, `NOTIF_STATUS`) | MDL (type owned by the provider's module) | notif/report/NotifLogSummaryReport.java:218-219; report/service/ReportService.java:147 |

Screens
| SCR-REQ id | Name (ar/en) | Page code | Code location |
|---|---|---|---|
| SCR-REQ-REPORT-001 | تقارير الوحدة / Module reports | `<MODULE>_REPORTS` — one dynamic row per owning module: `SEC_REPORTS`, `NOTIF_REPORTS`, `AUDIT_REPORTS` (reference app: `APP_REPORTS`) | report/permission/ReportPermissions.java:35, :40-42, :62-69 |

Requirements
REQ count: 16 · AC count: 16 · RULE count: 12 · ENT count: 2 · SCR-REQ count: 1 · XM count: 3
Last sequence per atom: REQ: 016 · AC: 016 · ENT: 002 · RULE: 012 · SCR-REQ: 001 · XM: 003 · US: 007 · POL: 008

## Id → code location
| Id | Title | Code location (primary) | Verified by |
|---|---|---|---|
| REQ-REPORT-001 / AC-REPORT-001 | Register a report by exposing a bean | report/registry/ReportRegistry.java:44-66 | TC-CORE-REPORT-001; `ReferenceApplicationSmokeTest.reportDefinitionsListTheThreeCoreReportsAndTheApplicationsOwn` |
| REQ-REPORT-002 / AC-REPORT-002 | Refuse an invalid catalog at start-up | report/registry/ReportRegistry.java:54-58, :78-101 | `ReportRegistryTest` (4) |
| REQ-REPORT-003 / AC-REPORT-003 | Register every report's permission automatically | report/permission/ReportPermissions.java:44-70 | `ReportApiIntegrationTest.definitions_listTheThreeCoreReports_forASuperRole` |
| REQ-REPORT-004 / AC-REPORT-004 | List the definitions the caller holds | report/service/ReportService.java:72-80 | TC-CORE-REPORT-011, -012; `ReportApiIntegrationTest.definitions_areFilteredByTheCallersAuthorities_andRunNeedsTheReportsPermission` |
| REQ-REPORT-005 / AC-REPORT-005 | Read one definition | report/service/ReportService.java:82-87, :126-132 | TC-CORE-REPORT-002; `ReportApiIntegrationTest.unknownReport_is404ReportNotFound` |
| REQ-REPORT-006 / AC-REPORT-006 | Run one page | report/service/ReportService.java:89-104; report/dto/ReportRunRequest.java:22-31 | TC-CORE-REPORT-003 |
| REQ-REPORT-007 / AC-REPORT-007 | Validate the parameters all at once | report/domain/ReportParametersDomain.java:58-92 | TC-CORE-REPORT-004, -017; `ReportParametersDomainTest` (4) |
| REQ-REPORT-008 / AC-REPORT-008 | Export as CSV | report/service/ReportService.java:106-124; report/export/CsvReportWriter.java:47-67 | TC-CORE-REPORT-005…008; `CsvReportWriterTest` (3); `ReportApiIntegrationTest.csvExport_hasTheUtf8Bom_arabicHeadersForAcceptLanguageAr_andRfc4180Quoting` |
| REQ-REPORT-009 / AC-REPORT-009 | Export as JSON | report/domain/ReportRunDomain.java:63-75; report/service/ReportService.java:119-123 | TC-CORE-REPORT-009; `ReportApiIntegrationTest.jsonExport_…` |
| REQ-REPORT-010 / AC-REPORT-010 | Reject an unsupported export format | report/domain/ReportRunDomain.java:74 | TC-CORE-REPORT-010 |
| REQ-REPORT-011 / AC-REPORT-011 | Enforce the export cap | report/service/ReportService.java:114-117; report/domain/ReportRunDomain.java:55-61 | TC-CORE-REPORT-014; `ReportApiIntegrationTest.exportCap_isEnforced` |
| REQ-REPORT-012 / AC-REPORT-012 | Refuse a caller without the report's authority | report/domain/ReportRunDomain.java:44-49 | TC-CORE-REPORT-013 |
| REQ-REPORT-013 / AC-REPORT-013 | Run inside the caller's tenant | report/service/ReportService.java:54-55, :72-73 | TC-CORE-REPORT-003, -015, -016; `ReportApiIntegrationTest.coreReports_seeOnlyTheCallersTenant` |
| REQ-REPORT-014 / AC-REPORT-014 | LOOKUP parameters from MDL lookups | report/service/ReportService.java:143-154; report/domain/ReportParametersDomain.java:196-203 | TC-CORE-REPORT-017 |
| REQ-REPORT-015 / AC-REPORT-015 | Report paths for the STAFF realm only | report/service/ReportService.java:73, :83, :90, :107 | `ReportApiIntegrationTest.customerToken_isRejectedWith403` |
| REQ-REPORT-016 / AC-REPORT-016 | Configure the export cap | autoconfigure/ErpCoreProperties.java:379-384 | `ReportApiIntegrationTest.exportCap_isEnforced` (cap 5 via property) |
| RULE-REPORT-001 | Catalog validity at start-up | report/registry/ReportRegistry.java:39-40, :78-101 | `ReportRegistryTest` |
| RULE-REPORT-002 | Catalog order | report/registry/ReportRegistry.java:60-64 | TC-CORE-REPORT-001 |
| RULE-REPORT-003 | Only held reports are visible and runnable | report/service/ReportService.java:76-77, :130; report/domain/ReportRunDomain.java:44-49 | TC-CORE-REPORT-011…013 |
| RULE-REPORT-004 | Parameter rules | report/domain/ReportParametersDomain.java:58-203 | `ReportParametersDomainTest`; TC-CORE-REPORT-004 |
| RULE-REPORT-005 | One page per run | report/dto/ReportRunRequest.java:22-31; report/service/ReportService.java:97-101 | TC-CORE-REPORT-003 |
| RULE-REPORT-006 | Export cap by a cap + 1 probe | report/service/ReportService.java:114-117; report/domain/ReportRunDomain.java:51-61 | TC-CORE-REPORT-014 |
| RULE-REPORT-007 | Export format | report/domain/ReportRunDomain.java:63-75 | TC-CORE-REPORT-009, -010 |
| RULE-REPORT-008 | CSV shape | report/export/CsvReportWriter.java:47-101 | `CsvReportWriterTest`; TC-CORE-REPORT-005…008 |
| RULE-REPORT-009 | Formula-injection guard | report/export/CsvReportWriter.java:102-104 | `CsvReportWriterTest`; TC-CORE-REPORT-007 |
| RULE-REPORT-010 | Bounded attachment body | report/controller/ReportController.java:65-75 | TC-CORE-REPORT-005 |
| RULE-REPORT-011 | Provider runs in the tenant and queries through JPA | report/ReportProvider.java:18-20; `CoreLibraryRulesArchTest` rule 7 | TC-CORE-REPORT-015, -016 |
| RULE-REPORT-012 | Provider honours the page size | report/ReportProvider.java:42-50 | TC-CORE-REPORT-014 |
| ENT-REPORT-001 | Report definition | report/ReportProvider.java:25-51 | TC-CORE-REPORT-001 |
| ENT-REPORT-002 | Report result | report/ReportResult.java:20-32 | TC-CORE-REPORT-003 |
| XM-REPORT-001 | ReportProvider SPI | report/ReportProvider.java:25-51 | TC-CORE-REPORT-001 (4 implementers listed) |
| XM-REPORT-002 | Catalog rows in SEC's registry | report/permission/ReportPermissions.java:44-70 | `ReportApiIntegrationTest.definitions_listTheThreeCoreReports_forASuperRole` |
| XM-REPORT-003 | MdlLookupApi.readActiveValuesByKey | report/service/ReportService.java:143-154 | TC-CORE-REPORT-017 |
| SCR-REQ-REPORT-001 | `<MODULE>_REPORTS` | report/permission/ReportPermissions.java:35, :62-69 | `governance/frontend/modules/REPORT/tests/` |
| US-REPORT-001…007 | stories | `../P0_5/prd-report.md` | — |
| POL-REPORT-001…008 | policies | `../P0/business-policies-report.md` | — |

REQ ids (full text in srs-report.md → A4): REQ-REPORT-001, REQ-REPORT-002, REQ-REPORT-003,
REQ-REPORT-004, REQ-REPORT-005, REQ-REPORT-006, REQ-REPORT-007, REQ-REPORT-008, REQ-REPORT-009,
REQ-REPORT-010, REQ-REPORT-011, REQ-REPORT-012, REQ-REPORT-013, REQ-REPORT-014, REQ-REPORT-015,
REQ-REPORT-016

AC ids (full text in srs-report.md → A4, one per REQ above): AC-REPORT-001 … AC-REPORT-016

RULE ids (full text in srs-report.md → A5): RULE-REPORT-001, RULE-REPORT-002, RULE-REPORT-003,
RULE-REPORT-004, RULE-REPORT-005, RULE-REPORT-006, RULE-REPORT-007, RULE-REPORT-008, RULE-REPORT-009,
RULE-REPORT-010, RULE-REPORT-011, RULE-REPORT-012

Error codes (full table in srs-report.md → STANDALONE)
| Code | HTTP | Code location |
|---|---|---|
| `REPORT_NOT_FOUND` | 404 | report/service/ReportService.java:128-129 |
| `REPORT_PARAM_INVALID` | 400 | report/domain/ReportParametersDomain.java:89-92; report/domain/ReportRunDomain.java:74 |
| `REPORT_EXPORT_TOO_LARGE` | 422 | report/domain/ReportRunDomain.java:57-59 |
| `ACCESS_DENIED` (common) | 403 | report/domain/ReportRunDomain.java:45-48 |
All three own codes: report/exception/ReportErrorCodes.java:15-26; i18n `messages.properties` 178–180, `messages_ar.properties` 175–177.

Permissions (exact authority strings, all code-defined, nothing seeded)
`PERM_<MODULE>_REPORTS_VIEW` (gateway of `<MODULE>_REPORTS`), `<MODULE>:REPORT:<CODE>` per report —
today `PERM_SEC_REPORTS_VIEW`, `SEC:REPORT:SEC_USER_LIST`, `PERM_NOTIF_REPORTS_VIEW`,
`NOTIF:REPORT:NOTIF_LOG_SUMMARY`, `PERM_AUDIT_REPORTS_VIEW`, `AUDIT:REPORT:AUDIT_EVENT_LIST`
(reference app: `PERM_APP_REPORTS_VIEW`, `APP:REPORT:APP_SMOKE_REPORT`).

Configuration
`erp.core.report.max-export-rows` — `@Positive`, default 100 000 (autoconfigure/ErpCoreProperties.java:379-384).

Decisions
ADR ids: ADR-REPORT-001, ADR-REPORT-002, ADR-REPORT-003 (all ACCEPTED, as built) — `governance/analysis/decisions/REPORT/`.

Event
"P1 completed: REPORT v1 (as built) — 2 entities (value objects), 16 requirements, 16 acceptance criteria, 12 rules, 1 screen requirement, 3 XM, 3 ADRs"
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 11
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : none — no report delta is planned for 1.3.0; the TENANT data export of `docs/plans/tenant-maturity-plan.md` C.5 does NOT use the report engine.
Statement      : This addendum records the deltas being implemented for 1.3.0; every row is verified against the code before the tag.

| # | Kind | Registry delta | Source |
|---|---|---|---|
| — | — | none (no new REQ / AC / RULE / ENT / SCR-REQ / XM id) | docs/plans/tenant-maturity-plan.md C.5 |
