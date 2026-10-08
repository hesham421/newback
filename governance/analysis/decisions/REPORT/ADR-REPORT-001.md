# ADR-REPORT-001 — Reports as code: `ReportProvider` SPI + fail-fast start-up registry, no persistence

Module  : REPORT     Version : v1 (as built, erp-core 1.2.0)     Stage raised : P1 (Requirements) — recorded after the fact
Status  : ACCEPTED (as built)

## Context
erp-core plan step 11 asked for a "minimal reporting engine": modules and applications contribute
tabular reports, and the core lists, validates, runs and exports them with one set of endpoints
(`erp-core-plan/11-STEP-reporting-minimal.md`; `docs/steps/11-report.md` Summary). Three ways to know
which reports exist were available:
- **A table of report definitions** (`REPORT_DEFINITION` with parameters and a query), editable at
  runtime — needs a migration, a CRUD API, a query language and a way to keep a stored query in step
  with the entities it reads.
- **A static list in configuration** (properties / YAML) — no migration, but the query still has to
  live somewhere in code, and the list and the code can drift.
- **Code only**: a report is a Spring bean implementing an SPI; a registry collects the beans at
  start-up and refuses an invalid catalog before any request is served.

The governing complexity rule (`../../modules/CU/P0/business-policies-cu.md` POLICY-CLI-01) and the
"extension door for applications" goal of the step (acceptance "+1 in the reference app") pushed
towards the third option; the permission catalog had already been made code-defined in step 06
(`PermissionContributor`, `docs/steps/06-report.md`), so a code-defined report catalog follows the same
pattern.

## Decision
**Reports are code, collected by a fail-fast in-memory registry; nothing is persisted** (as built):
- the SPI is the module's root package: `ReportProvider` (`code`, `moduleCode`, `titleAr`, `titleEn`,
  `params`, `run(params, Pageable)`), `ReportParam`, `ReportColumn`, `ReportResult(columns, rows,
  totals, totalRows)`, `ParamType`, `ColumnType`, `ReportAuthorities`
  (`erp-core/src/main/java/com/erp/report/ReportProvider.java:25-51`; ArchUnit
  `CrossModuleBoundaryArchTest.java:66-68` declares the root package public);
- `ReportRegistry` collects every `ReportProvider` bean (`ObjectProvider.orderedStream`), validates it
  and fails the start-up with an `IllegalStateException` naming the class on a duplicate code, a code
  not `^[A-Z][A-Z0-9_]{1,39}$`, a module code not `^[A-Z][A-Z0-9_]{0,9}$`, a blank title, a parameter
  without name or type, a duplicate parameter or a `LOOKUP` parameter without key
  (`report/registry/ReportRegistry.java:39-40`, `:44-66`, `:78-101`); the catalog is ordered by module
  code then code and held in memory;
- there is no table, no migration and no entity (`docs/steps/11-report.md` "Untouched: every
  migration"; "no entity was created"); the only persisted side effect is the permission rows
  `ReportPermissions` contributes to SEC's catalog (ADR-REPORT-002);
- `ReportResult` gained a fourth component `totalRows` beyond the step's three, with the three-argument
  constructor kept (`docs/DEVIATIONS.md` [11] first entry), so paging and the export cap work without
  loading everything.

Reasons, from the step file, its report and the code:
1. **One owner per report.** The query lives next to the entity and repository it reads, in the module
   that owns the data; it is reviewed, tested and versioned with them (the three core providers use
   `SpecBuilder` / JPQL, never native SQL — `docs/steps/11-report.md` "Reference providers").
2. **Fail fast, not at request time.** The codes become registry rows of fixed width
   (`SEC_ACTION_REG.ACTION_CODE VARCHAR(40)`, `SEC_MODULE_REG.CODE VARCHAR(10)`); checking them at
   start-up means an invalid catalog never reaches the database or a client
   (`ReportRegistry.java:19-34` Javadoc; the `FileStorageAutoConfiguration` precedent).
3. **No seed, no migration, no CRUD.** An application adds a report by exposing one bean
   (`erp-app-reference/src/main/java/com/erp/app/report/AppSmokeReport.java`; `docs/CONSUMING.md` §6);
   nothing else is needed, which is the step's acceptance.
4. **Medium complexity.** A persisted, editable definition would need a query language and a runtime
   editing surface that no requirement asked for.

## Consequences
- A report cannot be added, changed or removed without a deployment; every change to a report is a
  code change of its owning module (and, when its code or module changes, a new catalog row — old rows
  are never deleted by the synchronizer, `docs/DEVIATIONS.md` [06]).
- A broken provider in any module or application stops the whole application from starting, by
  design; the message names the class.
- The definitions endpoint reflects the running process; two nodes with different builds would show
  different catalogs — the catalog is part of the build.
- Everything a provider needs from the core (tenant filtering, typed parameters, the export cap) is
  decided once in `ReportService` / `ReportParametersDomain` / `ReportRunDomain`; providers only query.
- Revisiting this choice (persisted definitions, a report designer) would be a new table and a new
  CRUD API — additive, a MINOR — but none is planned (`docs/plans/tenant-maturity-plan.md` C.5 builds
  the tenant export outside the report engine).

## Traces
ENT-REPORT-001, ENT-REPORT-002 · REQ-REPORT-001, REQ-REPORT-002, REQ-REPORT-004, REQ-REPORT-005 ·
RULE-REPORT-001, RULE-REPORT-002 · POL-REPORT-001, POL-REPORT-002 · XM-REPORT-001 ·
docs/DEVIATIONS.md [11] (entries 1, 2, 11); docs/steps/11-report.md
