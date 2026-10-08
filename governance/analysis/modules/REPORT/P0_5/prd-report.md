# PRD — التقارير / Report (REPORT)
══════════════════════════════════════════════════════════════════
Module          : REPORT     Version : v1 (as-built baseline, erp-core 1.2.0)
Source artifacts: platform-summary, module-registry-report, business-policies-report
Stories         : 7   Policies covered : 8/8   Deferred : 0
Status          : AS-BUILT — every story is implemented; the code location is in `Source`
══════════════════════════════════════════════════════════════════

Java paths are relative to `erp-core/src/main/java/com/erp/`. HTTP verification: the `TC-CORE-REPORT-*`
cases (001…017) of `docs/test-api/core-test-plan.md`; frontend E2E archive `governance/frontend/modules/REPORT/tests/`.

## USER STORIES

US-REPORT-001
  Title          : عرض التقارير المتاحة لي / See the reports I may run
  Story          : As a staff user, I need the list of report definitions my role holds, with their parameters, so that the client can offer me exactly the reports I am allowed to run and build their parameter forms.
  Priority       : HIGH — the module's entry point (11-STEP acceptance b1)
  Success metric : `GET /api/v1/report/definitions` lists the three core reports (+ the application's) for a super role, exactly the held ones otherwise (TC-CORE-REPORT-001, -002)
  Traces         : POL-REPORT-003, POL-REPORT-004
  Source         : report/controller/ReportController.java:42-52; report/service/ReportService.java:72-87
  Status         : AS-BUILT

US-REPORT-002
  Title          : تشغيل تقرير صفحةً صفحة / Run a report one page at a time
  Story          : As a staff user, I need to run a report with typed parameters and receive one page of rows with columns, totals and the total row count, so that the client can page through a large result.
  Priority       : HIGH
  Success metric : `POST /api/v1/report/{code}/run` answers columns, rows, totals, page, size, totalRows within the caller's tenant (TC-CORE-REPORT-003)
  Traces         : POL-REPORT-004, POL-REPORT-005, POL-REPORT-006
  Source         : report/controller/ReportController.java:54-60; report/service/ReportService.java:89-104
  Status         : AS-BUILT

US-REPORT-003
  Title          : تصدير CSV يفتحه Excel / Export a CSV that Excel opens correctly
  Story          : As a staff user, I need to export a report as a CSV file with headers in my language, so that I can open it in Excel without broken Arabic text and without formula surprises.
  Priority       : HIGH (11-STEP acceptance b2)
  Success metric : the body starts with `EF BB BF`, headers are Arabic for `Accept-Language: ar`, commas and quotes are RFC 4180 quoted, `=1+1` arrives as `'=1+1` (TC-CORE-REPORT-005…008)
  Traces         : POL-REPORT-007, POL-REPORT-008
  Source         : report/controller/ReportController.java:62-75; report/service/ReportService.java:106-124; report/export/CsvReportWriter.java:47-67
  Status         : AS-BUILT

US-REPORT-004
  Title          : تصدير JSON كاملًا / Export the whole result as JSON
  Story          : As an integrating client, I need the whole (capped) result as one JSON document, so that I can feed it to another system without paging.
  Priority       : MEDIUM
  Success metric : `?format=json` answers the `ReportRunResponse` of the whole result without the envelope; an unknown format is 400 (TC-CORE-REPORT-009, -010)
  Traces         : POL-REPORT-007
  Source         : report/domain/ReportRunDomain.java:63-75; report/service/ReportService.java:119-123
  Status         : AS-BUILT

US-REPORT-005
  Title          : تسجيل تقرير بوحدتي / Register a report of my module
  Story          : As a developer of a core module or an application, I need to add a report by exposing one bean, so that it is listed, secured, validated, run and exported by the core without any endpoint, migration or seed of my own.
  Priority       : HIGH — the extension door (11-STEP acceptance b1 "+1 in the reference app")
  Success metric : `APP_SMOKE_REPORT` appears next to the three core reports and exports (`ReferenceApplicationSmokeTest.reportDefinitionsListTheThreeCoreReportsAndTheApplicationsOwn`)
  Traces         : POL-REPORT-001, POL-REPORT-002, POL-REPORT-003, POL-REPORT-005
  Source         : report/ReportProvider.java:25-51; report/registry/ReportRegistry.java:44-66; docs/CONSUMING.md §6
  Status         : AS-BUILT

US-REPORT-006
  Title          : منح تقرير لدور / Grant a report to a role
  Story          : As a security administrator, I need every report to be an action in the permission catalog, so that I grant it (with its screen's VIEW) to a role like any other action, and a super role holds it at once.
  Priority       : HIGH
  Success metric : a role holding only `PERM_SEC_REPORTS_VIEW` + `SEC:REPORT:SEC_USER_LIST` sees exactly `[SEC_USER_LIST]`; another report answers 403 (TC-CORE-REPORT-011…013)
  Traces         : POL-REPORT-003, POL-REPORT-004
  Source         : report/permission/ReportPermissions.java:44-60; report/domain/ReportRunDomain.java:44-49
  Status         : AS-BUILT

US-REPORT-007
  Title          : ضبط سقف التصدير / Configure the export cap
  Story          : As a platform operator, I need to set the maximum number of rows an export may contain, so that a report can never exhaust the server's memory.
  Priority       : MEDIUM
  Success metric : with cap 5, an export of 5 rows is 200 and of 6 rows is 422 `REPORT_EXPORT_TOO_LARGE` (`ReportApiIntegrationTest.exportCap_isEnforced`; TC-CORE-REPORT-014)
  Traces         : POL-REPORT-007
  Source         : autoconfigure/ErpCoreProperties.java:379-384; report/domain/ReportRunDomain.java:55-61
  Status         : AS-BUILT

## THE RUN STORY (US-REPORT-002, as built)
1. The caller posts `{params, page, size}` (body optional) to `POST /api/v1/report/{code}/run` —
   report/controller/ReportController.java:54-60; report/dto/ReportRunRequest.java:15-32.
2. `ReportService.permittedProvider` finds the code in the registry (404 `REPORT_NOT_FOUND` otherwise) and
   `ReportRunDomain.assertCanRun` checks `<MODULE>:REPORT:<CODE>` on the token (403 `ACCESS_DENIED`) —
   report/service/ReportService.java:126-132.
3. `ReportParametersDomain.validate` converts every declared parameter, rejects undeclared names and
   lists every failure under 400 `REPORT_PARAM_INVALID`; `LOOKUP` values are checked against the active
   MDL codes — report/domain/ReportParametersDomain.java:58-87; report/service/ReportService.java:143-154.
4. `PageableBuilder.from` builds the page (default 20, max 200) and the provider's `run` executes inside
   the read-only transaction under the caller's tenant — report/service/ReportService.java:97-101.
5. `ReportMapper.toRunResponse` answers `code, columns, rows, totals, page, size, totalRows` in the
   shared envelope — report/mapper/ReportMapper.java:61-74.

## TRACEABILITY — story → policy
| US | Traces (POL) | Source |
|---|---|---|
| US-REPORT-001 | POL-REPORT-003, POL-REPORT-004 | report/service/ReportService.java:72-87 |
| US-REPORT-002 | POL-REPORT-004, POL-REPORT-005, POL-REPORT-006 | report/service/ReportService.java:89-104 |
| US-REPORT-003 | POL-REPORT-007, POL-REPORT-008 | report/export/CsvReportWriter.java:47-67 |
| US-REPORT-004 | POL-REPORT-007 | report/domain/ReportRunDomain.java:63-75 |
| US-REPORT-005 | POL-REPORT-001, POL-REPORT-002, POL-REPORT-003, POL-REPORT-005 | report/registry/ReportRegistry.java:44-66 |
| US-REPORT-006 | POL-REPORT-003, POL-REPORT-004 | report/permission/ReportPermissions.java:44-60 |
| US-REPORT-007 | POL-REPORT-007 | autoconfigure/ErpCoreProperties.java:379-384 |
Every policy POL-REPORT-001 … POL-REPORT-008 appears in at least one row above (001, 002 → US-005;
003 → US-001/005/006; 004 → US-001/002/006; 005 → US-002/005; 006 → US-002; 007 → US-003/004/007;
008 → US-003).

## RESOLVED DECISIONS (dialogue)
| # | Question | Recommended | Confirmed by user | Sources |
|---|---|---|---|---|
| 1 | Report definitions in code or in a table? | code, fail-fast registry | fixed design of erp-core plan step 11, as built | ADR-REPORT-001 |
| 2 | One shared screen `REPORT` or one per module? | one per module (`PAGE_CODE` is globally unique) | step 11, as built (docs/DEVIATIONS.md [11]) | ADR-REPORT-002 |
| 3 | Streamed or buffered export? | buffered `byte[]` bounded by the cap | step 11, as built (docs/DEVIATIONS.md [11]) | ADR-REPORT-003 |
No other question: the stories describe built behaviour, read from the code.

## DEFERRED
| US | Reason | Activation trigger |
|---|---|---|
| (saved/scheduled reports, charts, PDF/XLSX, streaming, totals in CSV) | not built (business-policies-report.md SCOPE EXCEPTIONS) | explicit future request |

## APPROVAL
Approved by : n/a — as-built baseline (the stories describe implemented behaviour, erp-core 1.2.0)   Date : 2026-10-07
Later changes are appended as "Implementation Addendum — erp-core 1.3.0" sections, never by rewriting
the stories above.
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 11
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : none — no report delta is planned for 1.3.0; the TENANT data export of `docs/plans/tenant-maturity-plan.md` C.5 does NOT use the report engine.
Statement      : This addendum records the deltas being implemented for 1.3.0; every row is verified against the code before the tag.

| # | Kind | Story delta | Source |
|---|---|---|---|
| — | — | none | docs/plans/tenant-maturity-plan.md C.5 |
