# Step 11 — Minimal reporting engine — report

## Summary

Modules and applications now register reports as code. The core lists them, validates their parameters,
runs them under the caller's tenant and permission, and returns JSON or exports CSV/JSON.

- **SPI (`com.erp.report`, the module's public root package).** It contains:
  - `ReportProvider` (`code`, `moduleCode`, `titleAr/En`, `params`, `run(params, Pageable)`);
  - the records `ReportParam`, `ReportColumn` and `ReportResult`;
  - the enums `ParamType` and `ColumnType`;
  - `ReportAuthorities.of(module, code)`, which builds `<MODULE>:REPORT:<CODE>`.

  `ReportResult` has a fourth component, `totalRows`, and keeps the step's three-argument constructor.
- **Registry.** `ReportRegistry` collects every `ReportProvider` bean at startup. The startup fails with
  an `IllegalStateException` naming the classes when:
  - two providers share a code;
  - a code is not upper snake case of at most 40 characters, or a module code is longer than 10;
  - a title is blank;
  - a parameter is duplicated, or a `LOOKUP` parameter has no key.
- **Automatic permissions.** `ReportPermissions` is a `PermissionContributor` fed by the registry. For
  each owning module it contributes:
  - a screen `<MODULE>_REPORTS`;
  - that screen's VIEW gateway, `PERM_<MODULE>_REPORTS_VIEW`;
  - one action per report: action code = report code, permission code `<MODULE>:REPORT:<CODE>`.

  Step 06's synchronizer upserts these rows at startup, and super roles hold them at once. No migration
  is needed.
- **Endpoints (STAFF chain, `ReportController` → `ReportService`).**
  - `GET /api/v1/report/definitions`: only the reports the caller holds.
  - `GET /api/v1/report/definitions/{code}`.
  - `POST /api/v1/report/{code}/run`: body `{params, page, size}`; answers `ReportRunResponse` in the
    usual envelope.
  - `POST /api/v1/report/{code}/export?format=csv|json`: an attachment.

  Errors: an unknown code → 404 `REPORT_NOT_FOUND`; a missing permission → 403 `ACCESS_DENIED`; a customer
  token → 403 `REALM_MISMATCH` (from the staff chain).
- **Validation (`ReportParametersDomain`).** Values are checked for type and required-ness, and dates are
  ISO-8601. `LOOKUP` values are checked against `MdlLookupApi`'s active codes. Undeclared parameters are
  rejected. Every failure is listed under `REPORT_PARAM_INVALID` (400) in `fieldErrors`.
- **Export (`ReportRunDomain`, `CsvReportWriter`).**
  - The provider is asked for one page of `erp.core.report.max-export-rows + 1` rows (default 100 000).
    An export over the cap → 422 `REPORT_EXPORT_TOO_LARGE`.
  - CSV is hand-written RFC 4180: UTF-8 BOM, CRLF, proper quoting, and a guard against formula injection.
  - The header row is Arabic when `Accept-Language` is `ar`, otherwise English.
- **Reference providers.**
  - `SEC_USER_LIST` (`com.erp.sec.report`): `UserRepository` + `SpecBuilder`.
  - `AUDIT_EVENT_LIST` (`com.erp.audit.report`, module `AUDIT`): step 10's `CORE_AUDIT_EVENT` through
    `AuditEventRepository` + `SpecBuilder` (since the rebase onto 08–10; see below).
  - `NOTIF_LOG_SUMMARY` (`com.erp.notif.report`): a JPQL group-by over channel/status/day through the new
    read-only `NotificationLogSummaryRepository`.

  None of them uses native SQL. All are tenant-filtered by Hibernate.
- **Reference app.** `APP_SMOKE_REPORT` (`com.erp.app.report.AppSmokeReport`, module `APP`) is registered
  just by being a bean. The smoke test shows it next to the three core reports and exports it.

`mvn -q verify` is green: erp-core 197 tests (177 before, + 20 new: 9 integration, 11 unit),
erp-app-reference 9 (8 before, + 1).

## Files changed

**Created**
- SPI, in `erp-core/src/main/java/com/erp/report/`: `ReportProvider`, `ReportParam`, `ReportColumn`,
  `ReportResult`, `ParamType`, `ColumnType`, `ReportAuthorities`.
- The module's other classes, in `erp-core/src/main/java/com/erp/report/`:
  - `registry/ReportRegistry`
  - `permission/ReportPermissions`
  - `domain/ReportParametersDomain`, `domain/ReportRunDomain`
  - `export/CsvReportWriter`
  - `exception/ReportErrorCodes`
  - `dto/ReportRunRequest`, `ReportExportRequest`, `ReportDefinitionResponse`, `ReportParamResponse`,
    `ReportColumnResponse`, `ReportRunResponse`
  - `mapper/ReportMapper`
  - `service/ReportService`
  - `controller/ReportController`
- Reference providers:
  - `erp-core/src/main/java/com/erp/sec/report/SecUserListReport.java`
  - `erp-core/src/main/java/com/erp/audit/report/AuditEventListReport.java` (since the rebase; the interim `sec/report` version was deleted)
  - `erp-core/src/main/java/com/erp/notif/report/NotifLogSummaryReport.java`
  - `erp-core/src/main/java/com/erp/notif/repository/NotificationLogSummaryRepository.java`
- `erp-app-reference/src/main/java/com/erp/app/report/AppSmokeReport.java`
- Tests, in `erp-core/src/test/java/com/erp/report/`:
  - `ReportApiIntegrationTest` (9)
  - `ReportHttp` (helper)
  - `registry/ReportRegistryTest` (4)
  - `domain/ReportParametersDomainTest` (4)
  - `export/CsvReportWriterTest` (3)
- `docs/steps/11-report.md`

**Modified** (shared files, each with a minimal, appended edit)
- `erp-core/src/main/java/com/erp/autoconfigure/ErpCoreAutoConfiguration.java`: `,com.erp.report` appended
  to `CORE_PACKAGE_LIST`.
- `erp-core/src/main/java/com/erp/autoconfigure/ErpCoreProperties.java`: a `report` field and a nested
  `Report` class (`maxExportRows`), appended; plus the `Positive` import.
- `erp-core/src/main/resources/i18n/messages.properties` and `messages_ar.properties`: an own step-11 block
  at the end (3 codes).
- `erp-core/src/test/java/com/erp/architecture/CrossModuleBoundaryArchTest.java`: `com.erp.report`
  appended to `MODULES`, with its root package public.
- `erp-core/src/test/java/com/erp/autoconfigure/ErpCoreAutoConfigurationTest.java`: `com.erp.report`
  appended to the expected `CORE_PACKAGES`.
- `erp-core/src/test/java/com/erp/sec/PermissionCatalogIntegrationTest.java`: the seed comparison leaves
  out the registry-derived report contributor and the `*_REPORTS` screens.
- `erp-app-reference/src/test/java/com/erp/app/ReferenceApplicationSmokeTest.java`: one new test, appended.
- `docs/DEVIATIONS.md`: 13 `[11]` entries, appended.

**Deleted**: none.

**Untouched**: `erp-core-plan/execution-state.json`; every migration (no new one; none is reserved for
step 11); `ErpCoreSecurityAutoConfiguration`; `ErpCoreOpenApiAutoConfiguration`;
`TenantSchemaIntegrationTest` (no new table or entity); `NotificationLogRepository`.

## Decisions & deviations

These mirror the 13 `[11]` entries in `docs/DEVIATIONS.md`.

1. **`ReportResult` gained `Long totalRows`**, plus the step's three-argument constructor. The run response
   carries `page/size/totalRows`.
2. **Permission screen.** The screen is `<MODULE>_REPORTS`, not `REPORT`, because `PAGE_CODE` is globally
   unique.
   - A VIEW gateway is added per screen (RULE-SEC-007).
   - Each report's action code is its report code, with the explicit authority `<MODULE>:REPORT:<CODE>`.
   - No module rows are declared.
   - The registry caps the code at 40 characters and the module code at 10.
3. **`AUDIT_EVENT_LIST`** lives in `com.erp.audit.report` over `CORE_AUDIT_EVENT` (read-only). The
   `AUDIT` module row and its names come from step 10's `AuditPermissions`; the report adds only the
   screen `AUDIT_REPORTS`. Before the rebase, an interim version read `SEC_AUDIT_LOG` from
   `com.erp.sec.report`; it was removed.
4. **Export query.** The export asks for one page of cap + 1 rows rather than an unpaged result. Over the
   cap → 422 (`BUSINESS_RULE_VIOLATION`).
5. **Export body.** The CSV or JSON is sent as a `byte[]` bounded by the cap, not as a
   `StreamingResponseBody`, because an async dispatch would re-enter the security and tenant filters.
6. **Export formats and CSV rules.**
   - `csv` (the default) and `json`; any other format → 400 `REPORT_PARAM_INVALID` with field `format`.
   - CSV follows RFC 4180, with a formula-injection guard on STRING cells.
   - Totals go in JSON only.
   - The header language comes from the request locale.
7. **Authorization.** The service uses `isAuthenticated()` plus a check of the dynamic per-report
   authority, which answers 403 with the common `ACCESS_DENIED` code. An unknown code → 404 first. Each
   provider also carries its own same-module `@PreAuthorize`.
8. **Run request shape and parameter rules.**
   - Run body: `{params, page, size}`, with size 1..200 through `PageableBuilder`.
   - Undeclared parameters are rejected.
   - A `DATETIME` without a zone is read as UTC.
   - An unknown lookup type makes the parameter invalid.
   - EN messages are in `messages.properties` (step 01).
9. **`NOTIF_LOG_SUMMARY` query.** A separate read-only JPQL repository, so step 08's NOTIF work does not
   conflict. The groups are paged in memory.
10. **`SEC_USER_LIST` content.** Both realms; parameters realm/status/activeOnly/createdFrom/createdTo;
    never the password hash.
11. **Property and OpenAPI.** The property is `erp.core.report.max-export-rows` (`@Positive`, default
    100 000). No OpenAPI group was added.
12. **Test adaptations.** No assertion was weakened; the adapted tests are listed above.
13. **Excel.** A manual Excel check is not possible on this machine. The BOM and the header bytes were
    verified programmatically.

## Acceptance checklist

2/2 ✅ (the Excel bullet as recorded in deviation 13)

- ✅ **Tests green; `GET /api/v1/report/definitions` returns the 3 core reports (+1 in the reference app).**
  - `mvn -q verify` EXIT=0: erp-core 197/0/0/0, erp-app-reference 9/0/0/0.
  - `ReportApiIntegrationTest.definitions_listTheThreeCoreReports_forASuperRole`: `SEC_USER_LIST`,
    `AUDIT_EVENT_LIST` and `NOTIF_LOG_SUMMARY`, plus the test-only `TST_CSV_SAMPLE` of that context, and
    exactly 4 codes.
  - `ReferenceApplicationSmokeTest.reportDefinitionsListTheThreeCoreReportsAndTheApplicationsOwn`:
    `containsExactlyInAnyOrder("SEC_USER_LIST","AUDIT_EVENT_LIST","NOTIF_LOG_SUMMARY","APP_SMOKE_REPORT")`.
- ✅ (manual part not possible) **An exported CSV with Arabic headers opens correctly in Excel.**
  - **Manual Excel check not possible on this machine; BOM verified.**
  - Over HTTP with `Accept-Language: ar`, the response body starts with `EF BB BF`. It is followed
    byte-for-byte by the UTF-8 encoding of `الرقم,الاسم,الاسم العربي,صيغة\r\n`
    (`d8 a7 d9 84 d8 b1 d9 82 d9 85 2c ...`). See `csvExport_hasTheUtf8Bom_arabicHeadersForAcceptLanguageAr_andRfc4180Quoting`.
  - The reference app's CSV is asserted in full: `﻿المعرف,التسمية\r\n1,alpha\r\n2,بيتا\r\n3,"gamma, delta"\r\n`.
  - A UTF-8 BOM is what Excel needs to detect UTF-8 when it opens a CSV.

Task 4 tests, all green:

| Task 4 test | Where |
|---|---|
| registry rejects a duplicate code | `ReportRegistryTest.duplicateCodes_failTheStartup_namingBothProviders` |
| definitions list filtered by authority | `ReportApiIntegrationTest.definitions_areFilteredByTheCallersAuthorities_andRunNeedsTheReportsPermission`: a role holding only `PERM_SEC_REPORTS_VIEW` + `SEC:REPORT:SEC_USER_LIST` sees exactly `[SEC_USER_LIST]`; no grants → `[]`; other report → 403 |
| run with a missing required param → 400 | `run_withAMissingRequiredOrInvalidParameter_is400ReportParamInvalid`: `REPORT_PARAM_INVALID` with field `rows`; AR message; wrong type plus an unknown param; invalid LOOKUP `FAX` plus a bad date |
| CSV: BOM, quoted commas, AR headers with `Accept-Language: ar` | `csvExport_...`: `"Doe, ""J"" 1"`, `'=1+1`, EN headers with `en` |
| tenant isolation (user list of A excludes B) | `coreReports_seeOnlyTheCallersTenant`: SEC_USER_LIST as JSON (totalRows 2) and as CSV; NOTIF_LOG_SUMMARY totals A = 3, B = 5; AUDIT_EVENT_LIST (`CORE_AUDIT_EVENT`): A's `LOGIN` count and total equal A's own rows (fewer than all tenants'), and B's CSV has exactly B's `LOGIN` rows |
| export cap enforced | `exportCap_isEnforced` (cap 5: 5 rows → 200, 6 → 422 `REPORT_EXPORT_TOO_LARGE`) |

Also covered: JSON export (`jsonExport_...`, plus bad format → 400), unknown code → 404, and a customer
token → 403 on list/run/export (`customerToken_isRejectedWith403`).

## Verification output

```
$ java -version → openjdk version "21.0.7" 2025-04-15
$ rm -rf target erp-core/target erp-app-reference/target; mvn -q -o verify
EXIT=0 secs=180      [TestPostgres] integration-test database backend: EMBEDDED (once per module JVM)

surefire totals (target/surefire-reports/TEST-*.xml)                 tests fail err skip
== erp-core
com.erp.architecture.CrossModuleBoundaryArchTest                         4  0  0  0
com.erp.autoconfigure.ErpCoreAutoConfigurationTest                      10  0  0  0
com.erp.report.ReportApiIntegrationTest                                  9  0  0  0   (new)
com.erp.report.domain.ReportParametersDomainTest                         4  0  0  0   (new)
com.erp.report.export.CsvReportWriterTest                                3  0  0  0   (new)
com.erp.report.registry.ReportRegistryTest                               4  0  0  0   (new)
com.erp.sec.PermissionCatalogIntegrationTest                             4  0  0  0
... (all other 31 classes unchanged, 0 failures)
erp-core tests=197 failures=0 errors=0 skipped=0
== erp-app-reference
com.erp.app.ReferenceApplicationSmokeTest                                9  0  0  0   (+1)
erp-app-reference tests=9 failures=0 errors=0 skipped=0

$ grep -rn "nativeQuery = true\|JdbcTemplate\|createNativeQuery" <step-11 main sources> || echo OK-no-raw-sql-in-step11
OK-no-raw-sql-in-step11
$ grep -rn "@Cacheable\|@CacheEvict" erp-core/src/main/java/com/erp/report || echo OK-no-cache
OK-no-cache
```

Failed attempts on the way (all fixed):
- **`ReportRegistry` had two public constructors** → "No default constructor found". The
  `ObjectProvider` constructor is now `@Autowired`.
- **The CSV test expected the Arabic cell quoted.** The Arabic comma U+060C is not a separator, so the
  writer was right and the expectation was corrected.
- **The first full verify failed in `BootstrapAdminPasswordIntegrationTest`.** It counts `admin` rows
  across tenants, and my provisioned tenants used admin username `admin` (the pitfall step 07 recorded).
  They now use `report-admin`.

## Skills checked

- **`.claude/skills/build-create-service/SKILL.md`:** `ReportService` is compliant on:
  - `@Service @RequiredArgsConstructor @Slf4j`, `@Transactional(readOnly = true)` and `ServiceResult`;
  - `LocalizedException` with registered codes and `log.debug` for reads;
  - `PageableBuilder` for run paging;
  - decisions delegated to `ReportRunDomain` / `ReportParametersDomain`;
  - MDL reached only through `mdl.crossmodule.MdlLookupApi`, whose 404 is caught at the call site.

  Named deviation A.5.2: `@PreAuthorize("isAuthenticated()")` plus a programmatic check, because the
  authority depends on the path. This is `SecurityContextHelper.hasAuthority`'s documented use case. Not
  CRUD, so there is no create/update/delete/usage.
- **`build-create-controller`:** compliant. The controller is thin, uses `OperationCode.craftResponse`, a
  bilingual `@Tag`, `@Operation` on every endpoint and `@Valid @RequestBody`. Named deviations:
  - the export returns a raw file (FILE download precedent);
  - there are no search, activate or usage endpoints (no entity).
- **`build-create-dto`:** compliant. The DTOs use `@Data @Builder @NoArgsConstructor @AllArgsConstructor`,
  bilingual `@Schema` with examples, and i18n validation keys (`{validation.min}/{validation.max}`). There
  is no CRUD set because there is no entity.
- **`gov-enforce-error-handling`:** compliant.
  - Every request-path throw is a `LocalizedException`.
  - Codes are registered in `ReportErrorCodes` (private constructor that throws) and the common
    `ACCESS_DENIED`.
  - All 3 new codes have EN and AR messages with `''{0}''`.
  - Status by case: NOT_FOUND 404, VALIDATION_ERROR 400, BUSINESS_RULE_VIOLATION 422, FORBIDDEN 403.
  - Exception: the registry's startup `IllegalStateException`, which is not a request path (the
    `FileStorageAutoConfiguration` precedent).
- **`gov-enforce-caching-rules`:** compliant. There are no caching annotations, and reports are not on the
  register.
- **`gov-enforce-backend-contract` / `gov-validate-backend-feature`:**
  - The feature has no entity (code-defined catalog), so the entity, repository-CRUD and usage checks do
    not apply.
  - Applicable checks pass: domain objects are plain classes with static factories and no Spring/JPA; the
    service orchestrates only; the controller is thin.
  - CU.3–CU.8 pass.
  - Named deviations: A.2.1 (`NotificationLogSummaryRepository` extends `Repository`, read-only on
    purpose); A.5.2 (as above); A.6.4 (the raw-file export).
  - No automatic-rejection trigger is hit: no raw exceptions on request paths, no cross-module internals
    (ArchUnit green), no native SQL.
- **`build-create-entity` / `build-create-repository`:** no entity was created. The one new repository
  carries `@Repository`, uses JPQL, and its method has a caller.

## Notes for later steps

- **Audit report:** done in the rebase (`com.erp.audit.report.AuditEventListReport`). `AUDIT:EVENT:READ`
  and `AUDIT:REPORT:AUDIT_EVENT_LIST` are independent grants.
- **Writing a report (any module or app).**
  - Expose a `ReportProvider` bean. Query with JPA/`SpecBuilder` only, or with native SQL that includes
    `TENANT_ID`.
  - Honour `page.getPageSize()`: the export asks for cap + 1 rows.
  - Return `totalRows` when known.
  - Optionally add `@PreAuthorize("hasAuthority(T(<own class>).AUTHORITY)")` with
    `AUTHORITY = ReportAuthorities.of(MODULE, CODE)`.
  - The code must be at most 40 characters and the module code at most 10.
  - Nothing to seed: the permission appears in the catalog automatically, and `SYS_ADMIN` holds it.
  - For non-super roles, grant `PERM_<MODULE>_REPORTS_VIEW` + `<MODULE>:REPORT:<CODE>`.
- **Known limitation.** A `LOOKUP` parameter whose lookup type is missing in the caller's tenant is
  reported as `REPORT_PARAM_INVALID`. MDL's own `@Transactional` has already marked the shared read-only
  transaction rollback-only by then, so that misconfiguration can surface as a 500 on commit. All core
  lookup keys are seeded per tenant.
- **Shared files touched (merge planning):** `ErpCoreAutoConfiguration.CORE_PACKAGE_LIST`,
  `ErpCoreProperties` (field and class appended), `messages{,_ar}.properties` (own block at the end),
  `CrossModuleBoundaryArchTest.MODULES`, `ErpCoreAutoConfigurationTest` (package list),
  `PermissionCatalogIntegrationTest`, `ReferenceApplicationSmokeTest`, `docs/DEVIATIONS.md`.
- **Tests that provision tenants must not name the first administrator `admin`**
  (`BootstrapAdminPasswordIntegrationTest`).

## Rebase onto 08/09/10

`git rebase main` (main at `9b2ebfb`, which contains the merges of steps 08, 09 and 10; step 10's merge
is `779c012`). Conflicts and how each was resolved, keeping every step:

| File | Resolution |
|---|---|
| `ErpCoreAutoConfiguration.CORE_PACKAGE_LIST` | `... ,com.erp.events ,com.erp.sequence ,com.erp.audit ,com.erp.report` |
| `ErpCoreProperties` | fields `events`, `notif`, `audit`, then `report`; classes `Events`, `Notif`, `Audit`, then `Report` |
| `messages.properties`, `messages_ar.properties` | blocks in the order 08, 09, 10, then 11 |
| `docs/DEVIATIONS.md` | entries 08, 09, 10, then 11 |
| `CrossModuleBoundaryArchTest.MODULES` | events, sequence, audit, then `com.erp.report` (root package public) |
| `ErpCoreAutoConfigurationTest` | the expected package list ends `events, sequence, audit, report` |
| `PermissionCatalogIntegrationTest` | combines 09's `NOT_SEEDED_AUTHORITIES`/`NOT_SEEDED_SCREENS`, 10's `com.erp.audit.` contributor exclusion and 11's report-contributor and `*_REPORTS` exclusion; the 41/18 assertions are unchanged |
| `ReferenceApplicationSmokeTest` | auto-merged: Flyway `2..15, 1000` (from main) plus this step's report test |

**Audit report move.**
- `com.erp.sec.report.AuditEventListReport` (interim, over `SEC_AUDIT_LOG`) was deleted.
- `com.erp.audit.report.AuditEventListReport` was created. It keeps code `AUDIT_EVENT_LIST`, module
  `AUDIT` and permission `AUDIT:REPORT:AUDIT_EVENT_LIST`.
- It reads `AuditEvent` (`@Immutable`) through `AuditEventRepository` + `SpecBuilder`: parameters action,
  entityType, entityId, actor, occurredFrom/To; newest first.
- Module consistency: `ReportPermissions` declares no module, so `SEC_MODULE_REG` keeps exactly one
  `AUDIT` row named by `AuditPermissions` ("Audit Log"). Its screens are `AUDIT_EVENTS` (step 10) and
  `AUDIT_REPORTS` (step 11), asserted in `ReportApiIntegrationTest.definitions_listTheThreeCoreReports_forASuperRole`.
- The tenant-isolation test now uses generic audit rows:
  - A's `LOGIN` rows and A's whole total equal A's own `CORE_AUDIT_EVENT` count (fewer than all rows);
  - B's CSV has exactly B's `LOGIN` rows.
- The `[11]` DEVIATIONS entry was rewritten to match.

**Verification after the rebase:**

```
$ rm -rf target erp-core/target erp-app-reference/target; mvn -q -o verify
EXIT=0 secs=214
erp-core tests=336 failures=0 errors=0 skipped=0      (report classes 9+4+3+4, CrossModuleBoundaryArchTest 5, PermissionCatalogIntegrationTest 4)
erp-app-reference tests=10 failures=0 errors=0 skipped=0   (ReferenceApplicationSmokeTest 10)
```
