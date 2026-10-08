# DATABASE — التقارير / Report (REPORT)
══════════════════════════════════════════════════════════════════
Module : REPORT   Version : v1 (as-built baseline, erp-core 1.2.0)   Dialect : postgresql16   Schema prefix : none
Identifier transformation : not applicable — the module creates no physical object. The only physical
  names it produces are the registry codes written into SEC's tables (§2), UPPER_SNAKE_CASE as the
  providers declare them.
Date : 2026-10-07
Counts : 0 tables · 0 sequences · 0 DBF · 3 value sets (Java enums) · 2 XM (1 exposed SPI, 1 registry-row write into SEC)
══════════════════════════════════════════════════════════════════

**No table; in-memory registry.** The report module owns no migration, no table, no sequence and no JPA
entity: `docs/steps/11-report.md` "Untouched: every migration (no new one; none is reserved for step 11)"
and "no entity was created". `ReportRegistry` holds the catalog in memory
(`erp-core/src/main/java/com/erp/report/registry/ReportRegistry.java:42-66`), rebuilt from the
`ReportProvider` beans at every start-up. The core Flyway chain `V2..V15` contains no `REPORT` object
(`erp-core/src/main/resources/db/migration/core/README.md` "The chain"). Java paths below are relative
to `erp-core/src/main/java/com/erp/`, migrations to `erp-core/src/main/resources/db/migration/core/`,
`file:line` at main @ 19b19a4.

## 1. DB FIELD TRACEABILITY MATRIX — REPORT v1

No DBF id: no column is owned by the module. ENT-REPORT-001 (report definition) and ENT-REPORT-002
(report result) are value objects (`../P1/srs-report.md` A3) that never reach the database.

### 1a. Value sets (Java enums, no CHECK constraint, no lookup row)
| Value set | Values | Where it appears on the wire | Source |
|---|---|---|---|
| `ParamType` | `STRING`, `INTEGER`, `DECIMAL`, `DATE`, `DATETIME`, `BOOLEAN`, `LOOKUP` | `ReportParamResponse.type` (definition) | report/ParamType.java:18-26; report/dto/ReportParamResponse.java:16-17 |
| `ColumnType` | `STRING`, `INTEGER`, `DECIMAL`, `DATE`, `DATETIME`, `BOOLEAN` | `ReportColumnResponse.type` (run / JSON export); drives the CSV cell format | report/ColumnType.java:10-17; report/dto/ReportColumnResponse.java:16-17; report/export/CsvReportWriter.java:90-106 |
| `ReportRunDomain.ExportFormat` | `CSV` (`text/csv;charset=UTF-8`, `.csv`), `JSON` (`application/json`, `.json`) | query parameter `format` (case-insensitive; blank = `csv`) | report/domain/ReportRunDomain.java:19-38, :63-75 |

## 2. XM REGISTER — REPORT v1

The module holds no FK and no SOFT-READ of another module's table in the data sense. Its two
cross-module facts are an exposed Java SPI and the registry rows it causes SEC's synchronizer to write;
a third (the MDL lookup read) is a crossmodule interface call and is listed in `../P1/srs-report.md` A8
(XM-REPORT-003).

| XM-ID | Type | Surface | Target / implementers | Physical object | Status |
|---|---|---|---|---|---|
| XM-REPORT-001 | SPI (exposed) | `com.erp.report.ReportProvider` (root package) | implementers SEC `SecUserListReport`, NOTIF `NotifLogSummaryReport`, AUDIT `AuditEventListReport`, reference app `AppSmokeReport` | none (Java interface; each implementer queries its own module's tables) | ACTIVE |
| XM-REPORT-002 | registry-row write (consumed SEC SPI `PermissionContributor`) | `ReportPermissions.screens()` / `.permissions()` → rows of `SEC_SCREEN_REG` and `SEC_ACTION_REG` (global tables owned by SEC, ENT-SEC-005 / ENT-SEC-006), upserted at start-up by `PermissionCatalogSynchronizer`; no module row declared | SEC | `SEC_SCREEN_REG`, `SEC_ACTION_REG` (and, when no other contributor names the module, a `SEC_MODULE_REG` row created with its code as name) | ACTIVE |

### 2a. Width limits of XM-REPORT-002 (the physical constraint on what a provider may declare)
| Provider value | Registry column (V4) | Physical width | Guard in the module | Source |
|---|---|---|---|---|
| `moduleCode()` | `SEC_MODULE_REG.CODE` | `VARCHAR(10)` | `ReportRegistry` `MODULE_CODE = ^[A-Z][A-Z0-9_]{0,9}$` (1–10) | V4__sec_schema.sql:71; report/registry/ReportRegistry.java:40, :82-83 |
| `<moduleCode>_REPORTS` (screen) | `SEC_SCREEN_REG.PAGE_CODE` | `VARCHAR(50)`, globally unique (`UQ_SEC_SCREEN_REG_PAGE`) | at most 10 + 8 = 18 characters by construction | V4__sec_schema.sql:105; report/permission/ReportPermissions.java:35, :40-42 |
| screen names "تقارير <MODULE>" / "<MODULE> Reports" | `SEC_SCREEN_REG.NAME_AR` / `NAME_EN` | `VARCHAR(150)` | by construction (≤ 10 + 8) | V4__sec_schema.sql:107-108; report/permission/ReportPermissions.java:66-67 |
| `code()` (action code) | `SEC_ACTION_REG.ACTION_CODE` | `VARCHAR(40)` | `ReportRegistry` `CODE = ^[A-Z][A-Z0-9_]{1,39}$` (2–40) | V4__sec_schema.sql:120; report/registry/ReportRegistry.java:39, :80-81 |
| `<MODULE>:REPORT:<CODE>` (authority) and `PERM_<MODULE>_REPORTS_VIEW` | `SEC_ACTION_REG.PERMISSION_CODE` | `VARCHAR(100)`, globally unique (`UQ_SEC_ACTION_REG_PERM`) | at most 10 + 8 + 40 = 58 / 5 + 10 + 13 = 28 characters by construction | V4__sec_schema.sql:118; report/ReportAuthorities.java:18-21; sec/permission/PermissionDef.java:92-94 |
| `titleAr()` / `titleEn()` (action names) | `SEC_ACTION_REG.NAME_AR` / `NAME_EN` | `VARCHAR(150)` | not checked by `ReportRegistry` (only non-blank, RULE-REPORT-001); a title longer than 150 characters is a physical limit the provider must respect | V4__sec_schema.sql:121-122; report/registry/ReportRegistry.java:84-85; report/permission/ReportPermissions.java:56-57 |
| `VIEW` (gateway action) | `SEC_ACTION_REG.ACTION_CODE` | `VARCHAR(40)` | constant | report/permission/ReportPermissions.java:53; sec/permission/PermissionDef.java:81-83 |

Rows written today (SEC's tables, PLATFORM-global catalog): screens `SEC_REPORTS`, `NOTIF_REPORTS`,
`AUDIT_REPORTS`; actions `PERM_SEC_REPORTS_VIEW`, `SEC:REPORT:SEC_USER_LIST`, `PERM_NOTIF_REPORTS_VIEW`,
`NOTIF:REPORT:NOTIF_LOG_SUMMARY`, `PERM_AUDIT_REPORTS_VIEW`, `AUDIT:REPORT:AUDIT_EVENT_LIST`; in the
reference application additionally the module row `APP` (created by the synchronizer, name = code),
screen `APP_REPORTS`, actions `PERM_APP_REPORTS_VIEW`, `APP:REPORT:APP_SMOKE_REPORT`. None of them is
seeded by a migration; `PermissionCatalogIntegrationTest` leaves the report contributor and the
`*_REPORTS` screens out of its seed comparison (docs/DEVIATIONS.md [11] "Existing tests adapted").

## 3. FULL_DATABASE_SCRIPT (as built)

```sql
-- ============================================================
-- REPORT — no DDL. The module owns no table, sequence, constraint,
-- index, trigger, view, function or seed. Its catalog is in memory
-- (ReportRegistry) and its permission rows are written by SEC's
-- PermissionCatalogSynchronizer at start-up (XM-REPORT-002).
-- ============================================================
```

## 4. Deviations from this analysis
None — written from the code.
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 11
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : none — no report table or registry-row change is planned for 1.3.0; the TENANT data export of `docs/plans/tenant-maturity-plan.md` C.5 does NOT use the report engine.
Statement      : This addendum records the deltas being implemented for 1.3.0; every row is verified against the code before the tag.

| # | Kind | Schema delta | Migration | Source |
|---|---|---|---|---|
| — | — | none | — | docs/plans/tenant-maturity-plan.md C.5, §11 (no report migration expected) |
