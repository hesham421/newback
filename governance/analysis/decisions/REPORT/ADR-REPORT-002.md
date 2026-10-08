# ADR-REPORT-002 — Automatic permission registration (one screen per module, one action per report) checked in code, not by `@PreAuthorize`

Module  : REPORT     Version : v1 (as built, erp-core 1.2.0)     Stage raised : P1 (Requirements) — recorded after the fact
Status  : ACCEPTED (as built)

## Context
Every report must be grantable to a role like any other action (RULE-SEC-007: a screen's non-VIEW
actions count only with its VIEW), and the step file sketched the permission as
`PermissionDef(moduleCode, "REPORT", code)` — one screen `REPORT` shared by every module
(`erp-core-plan/11-STEP-reporting-minimal.md`; `docs/DEVIATIONS.md` [11] "Permissions"). Two facts of
the platform contradicted that sketch:
- `SEC_SCREEN_REG.PAGE_CODE` is globally unique (`UQ_SEC_SCREEN_REG_PAGE`, V4), so one screen `REPORT`
  cannot belong to several modules;
- the required authority of a report endpoint depends on the path variable (`<MODULE>:REPORT:<CODE>`),
  which a fixed `@PreAuthorize("hasAuthority(T(...).CONSTANT)")` — the convention of every other
  service (`.claude/skills/build-create-service`, ArchUnit rule 5) — cannot express.

The alternatives were: one global screen `REPORT` (rejected by the unique page code); a seeded
migration per report (contradicts ADR-REPORT-001 and the automatic catalog of step 06); a
`@PreAuthorize` SpEL that computes the authority from the method argument (possible, but it would be
the only SpEL in the code base reading a path variable, outside the ArchUnit rule's reviewed shape);
or an in-code check with the existing `SecurityContextHelper.hasAuthority` helper, whose documented
purpose is exactly "the rare gate a single `@PreAuthorize` cannot express"
(`erp-core/src/main/java/com/erp/common/util/SecurityContextHelper.java:35-39`).

## Decision
**One screen per owning module, one action per report, registered automatically; the per-report
authority is checked in code** (as built):
- `ReportPermissions` (`com.erp.report.permission`, a `PermissionContributor` fed by `ReportRegistry`)
  contributes, per owning module, the screen `<MODULE>_REPORTS` (names "تقارير <MODULE>" /
  "<MODULE> Reports") with its `VIEW` gateway `PERM_<MODULE>_REPORTS_VIEW`, and, per report, one action
  whose code is the report code and whose authority is the explicit `<MODULE>:REPORT:<CODE>`
  (`ReportAuthorities.of`), named by the report titles; it declares no module row (that would rename
  the owning module's row; a module known only from a report is created by the synchronizer with its
  code as name) — `erp-core/src/main/java/com/erp/report/permission/ReportPermissions.java:16-70`;
- step 06's `PermissionCatalogSynchronizer` upserts the rows at start-up; nothing is seeded by a
  migration; every tenant's super role holds them at once
  (`docs/steps/11-report.md` "Automatic permissions");
- every `ReportService` method is `@PreAuthorize("isAuthenticated()")` and checks
  `SecurityContextHelper.hasAuthority(ReportAuthorities.of(provider))`, decided by
  `ReportRunDomain.assertCanRun` → 403 with the common `ACCESS_DENIED` (no new i18n key); an unknown
  code answers 404 `REPORT_NOT_FOUND` first; the definitions list is filtered to the held reports
  (`report/service/ReportService.java:72-80`, `:126-132`; `report/domain/ReportRunDomain.java:44-49`);
- each core provider additionally carries `@PreAuthorize("hasAuthority(T(<its own class>).AUTHORITY)")`
  (defence in depth, same-module SpEL); ArchUnit rule 5 documents a `ReportProvider`'s `AUTHORITY`
  constant as an accepted exception to "the referenced type is a `*Permissions` class"
  (`erp-core/src/test/java/com/erp/architecture/CoreLibraryRulesArchTest.java:197-203`, `:243`;
  `docs/DEVIATIONS.md` [12] rule 5).

## Consequences
- A report appears in the catalog the first time the application starts with its provider; an
  administrator grants `PERM_<MODULE>_REPORTS_VIEW` + `<MODULE>:REPORT:<CODE>` to a non-super role; the
  frontend menu shows one "Reports" screen per module (`SEC_REPORTS`, `NOTIF_REPORTS`, `AUDIT_REPORTS`;
  `APP_REPORTS` in the reference app).
- A screen code is at most 18 characters and an authority at most 58 by construction (module ≤ 10,
  code ≤ 40), inside `PAGE_CODE VARCHAR(50)` and `PERMISSION_CODE VARCHAR(100)`; titles become
  `NAME_AR` / `NAME_EN VARCHAR(150)` and are not length-checked by the registry
  (`../../modules/REPORT/P2/db-script-report.md` §2a).
- `ReportService` is the named deviation A.5.2 of `build-create-service` (`isAuthenticated()` plus a
  programmatic check); the api-docs generator reports the authorization rule as `isAuthenticated()`
  and cannot see the per-report authority (`docs/api-docs/report/endpoints/reports.md`).
- `AUDIT:REPORT:AUDIT_EVENT_LIST` and `AUDIT:EVENT:READ` are independent grants: a role may export the
  audit report without browsing the event API and vice versa (`docs/steps/11-report.md` "Notes").
- Removing a report leaves its catalog rows in place (the synchronizer never deletes); they become
  inert.

## Traces
REQ-REPORT-003, REQ-REPORT-004, REQ-REPORT-005, REQ-REPORT-012 · RULE-REPORT-003 · POL-REPORT-003,
POL-REPORT-004 · SCR-REQ-REPORT-001 · XM-REPORT-002 · docs/DEVIATIONS.md [11] (entries 2, 7), [12] (rule 5);
docs/steps/11-report.md; RULE-SEC-007
