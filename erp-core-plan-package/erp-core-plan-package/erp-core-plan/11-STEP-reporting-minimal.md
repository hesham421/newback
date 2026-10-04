# Step 11 — Minimal reporting engine

**Branch:** `step/11-reporting`
**Goal:** modules and apps register reports as code (`ReportProvider`); the core exposes one endpoint that lists reports, validates parameters, runs the provider under tenant + permission checks, and returns JSON or CSV. No report designer, no PDF.
**Why:** the audit found no reporting outside `fin`. Business modules (Phase 4) must "close with their reports"; they need a uniform place to plug them in, with export and permissions handled once.

## Preconditions
- Step 05 merged.

## Design (fixed decisions)
- Package `com.erp.report`.
  ```java
  public record ReportParam(String name, ParamType type /*STRING,INTEGER,DECIMAL,DATE,DATETIME,BOOLEAN,LOOKUP*/, boolean required, String lookupKey, String labelAr, String labelEn) {}
  public record ReportColumn(String key, ColumnType type, String labelAr, String labelEn) {}
  public record ReportResult(List<ReportColumn> columns, List<Map<String,Object>> rows, Map<String,Object> totals) {}
  public interface ReportProvider {
      String code();                 // e.g. "SEC_USER_LIST"
      String moduleCode();           // owning module → permission "<MODULE>:REPORT:<CODE>" contributed automatically
      String titleAr(); String titleEn();
      List<ReportParam> params();
      ReportResult run(Map<String,Object> params, Pageable page);   // page may be unpaged for export
  }
  ```
- Registry: all `ReportProvider` beans collected at startup into `ReportRegistry`; duplicate codes fail startup.
- Permissions: for each provider the registry contributes `PermissionDef(moduleCode, "REPORT", code)` through a `PermissionContributor` so that reports appear in the RBAC catalog automatically.
- Endpoints (STAFF): `GET /api/v1/report/definitions` (filtered by the caller's authorities), `GET /api/v1/report/definitions/{code}`, `POST /api/v1/report/{code}/run` (JSON, paged), `POST /api/v1/report/{code}/export?format=csv` (streams `text/csv` with UTF-8 BOM; AR/EN headers by `Accept-Language`). Export cap `erp.core.report.max-export-rows` (default 100 000) → `REPORT_EXPORT_TOO_LARGE`.
- Parameter validation: types, required, `LOOKUP` values validated against `MdlLookupApi`; dates parsed ISO-8601.
- Reference providers shipped by core (and useful as examples): `SEC_USER_LIST` (sec), `AUDIT_EVENT_LIST` (audit), `NOTIF_LOG_SUMMARY` (notif, grouped by channel/status/day).

## Tasks
1. Implement registry, SPI, controller, CSV writer (Jackson CSV module or hand-written RFC 4180 with proper quoting), validation, `ReportPermissions` contributor bridging.
2. Implement the three reference providers using JPA/`SpecBuilder` queries (no native SQL).
3. **i18n**: `REPORT_NOT_FOUND`, `REPORT_PARAM_INVALID`, `REPORT_EXPORT_TOO_LARGE`.
4. **Tests**: registry rejects duplicate code; definitions list filtered by authority; run with missing required param → 400; CSV export has BOM, quoted commas, AR headers with `Accept-Language: ar`; tenant isolation (user list report in tenant A excludes B); export cap enforced.
5. `erp-app-reference`: add `APP_SMOKE_REPORT` provider to prove app-side registration.

## Acceptance
- Tests green; `GET /api/v1/report/definitions` returns the 3 core reports (+1 in the reference app).
- Opening an exported CSV with Arabic headers in Excel shows correct characters (manual check noted in the report).

## Commit
`step(11): minimal reporting engine — ReportProvider SPI, registry with auto permissions, run/export endpoints, three reference reports`

## Out of scope
PDF/XLSX export, scheduling, report designer, charts.
