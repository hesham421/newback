# SRS — التقارير / Report (REPORT)
══════════════════════════════════════════════════════════════════
Module : REPORT   Version : v1 (as-built baseline)   Profile : erp
Inputs : prd-report, module-registry-report, business-policies-report; the code at main @ 19b19a4 (erp-core 1.2.0 behaviour)
Counts : ENT 2 · REQ 16 · AC 16 · RULE 12 · SCR-REQ 1 · XM 3 · ADR 3
══════════════════════════════════════════════════════════════════

Written from the code, not before it. Every REQ / AC / RULE names the code location it was read from;
the id → location index is `registry-srs-report.md`. Java paths are relative to
`erp-core/src/main/java/com/erp/`, `file:line` at main @ 19b19a4. Test-case ids `TC-CORE-REPORT-NNN`
are those of `docs/test-api/core-test-plan.md`; endpoint paths are those of `docs/api-docs/report/`.

# PART A — MODULE FOUNDATION

## A1 — Document information
| Item | Value |
|---|---|
| Module | REPORT — التقارير / Report (package `com.erp.report`; screens `<MODULE>_REPORTS` under each owning registry module) |
| Feature code | REPORT |
| Version | v1 (as built in erp-core 1.2.0) |
| Date | 2026-10-07 |
| Status | AS-BUILT baseline |
| Prepared by | analysis lane, from the code |
| Decisions applied count | 3 (ADR-REPORT-001, -002, -003) |

## A2 — Functional context

**In scope:** واجهة `ReportProvider` التي تنفّذها الوحدات والتطبيقات، سجل التقارير في الذاكرة مع
التحقق الفوري عند الإقلاع، التسجيل التلقائي لصلاحية كل تقرير في كتالوج الصلاحيات، نقاط النهاية الأربع
(قائمة التعريفات، تعريف واحد، تشغيل صفحة، تصدير CSV/JSON)، التحقق من المعاملات وتحويلها، سقف التصدير،
كتابة CSV الآمنة، والتقارير المرجعية الثلاثة في SEC وNOTIF وAUDIT.

**Out of scope:** تخزين تعريفات التقارير أو تحريرها وقت التشغيل، الجدولة، الرسوم البيانية، PDF/XLSX،
التصدير المتدفق، الإجماليات داخل CSV، مجموعة OpenAPI خاصة، تصدير بيانات المستأجر (خطة النضج C.5) —
[business-policies-report.md → SCOPE EXCEPTIONS; ADR-REPORT-001; ADR-REPORT-003].

**Module function (one paragraph):** وحدة REPORT تجعل أي وحدة أو تطبيق قادرًا على تقديم تقرير جدولي
بمجرد كشف bean واحد: تجمع الكتالوج وتتحقق منه عند الإقلاع، وتولّد صلاحية كل تقرير تلقائيًا، وتقدّم
للعميل نقاط نهاية موحّدة تعرض التعريفات وتتحقق من المعاملات وتشغّل صفحة واحدة وتصدّر CSV أو JSON داخل
مستأجر المستدعي وتحت سقف صفوف مضبوط.

**Detailed description (workflow narrative, roles):** مطوّر الوحدة يكتب `ReportProvider` (رمز، وحدة،
عنوانان، معاملات، `run`). عند الإقلاع يجمعه `ReportRegistry` ويرفض الكتالوج غير الصالح، ويكتب
`ReportPermissions` شاشة `<MODULE>_REPORTS` وبوابتها وإجراء التقرير في كتالوج SEC. مدير الأمان يمنح
الإجراء لدور (الدور الأعلى يحمله تلقائيًا). المستخدم يستعرض التعريفات التي يحملها، يشغّل التقرير صفحةً
صفحة، أو يصدّره؛ كل ذلك داخل مستأجره.

**Current situation:** built (erp-core plan step 11; rebased onto steps 08–10).

**General notes:** the module has no table and no entity; the only persisted side effect is the catalog
rows SEC's synchronizer writes from `ReportPermissions` (`../P2/db-script-report.md`).

## A3 — Entities and fields

### ENT-REPORT-001 — تعريف التقرير / Report definition (`ReportProvider` + `ReportParam`)
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| SPI value object, in memory (one Spring bean per report; no table) | SHARED (owner) — implemented by SEC, NOTIF, AUDIT and applications (XM-REPORT-001) | `code` is the natural key (unique in the registry) | collected at start-up; list, read; never created, updated or deleted at runtime | each definition becomes one `SEC_ACTION_REG` row and contributes to one `SEC_SCREEN_REG` row (XM-REPORT-002) | report/ReportProvider.java:25-51; report/ReportParam.java:13-25; report/registry/ReportRegistry.java:44-66 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| code | text (2–40) | yes | `^[A-Z][A-Z0-9_]{1,39}$` (RULE-REPORT-001) | unique (RULE-REPORT-001); becomes `SEC_ACTION_REG.ACTION_CODE`; `ReportDefinitionResponse.code` | رمز التقرير | Report code |
| moduleCode | text (1–10) | yes | `^[A-Z][A-Z0-9_]{0,9}$` (RULE-REPORT-001) | the owning registry module; `SEC_MODULE_REG.CODE` | الوحدة المالكة | Owning module |
| titleAr, titleEn | text | yes (not blank) | — | names of the action row | العنوان العربي / الإنجليزي | Arabic / English title |
| params | list of `ReportParam` | no (may be empty) | `name` (unique per report), `type` (`ParamType`), `required`, `lookupKey` (`LOOKUP` only), `labelAr`, `labelEn` | display order kept | المعاملات | Parameters |
| authority (derived) | text | — | `<MODULE>:REPORT:<CODE>` (`ReportAuthorities.of`) | in `ReportDefinitionResponse.authority` | الصلاحية المطلوبة | Permission |
Source: report/ReportProvider.java:27-40; report/ReportParam.java:13-24; report/mapper/ReportMapper.java:19-46; report/dto/ReportDefinitionResponse.java:14-30.

### ENT-REPORT-002 — نتيجة التقرير / Report result (`ReportResult` + `ReportColumn`)
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| SPI value object, per run (never stored) | SHARED (owner) — produced by every provider | No | produced by `run`, mapped to `ReportRunResponse` or written as CSV | — | report/ReportResult.java:20-32; report/ReportColumn.java:12 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| columns | list of `ReportColumn(key, type, labelAr, labelEn)` | yes (copied, never null) | `type` ∈ `ColumnType` | `key` = the row-map key; labels = the CSV header | الأعمدة | Columns |
| rows | list of `Map<String, Object>` | yes (never null) | values by `ColumnType`: String, Number, LocalDate, Instant / OffsetDateTime / LocalDateTime, Boolean; `null` = empty cell | one page (run) or the whole capped result (export) | الصفوف | Rows |
| totals | `Map<String, Object>` | no (empty map) | keyed by column key | JSON only, never in CSV | الإجماليات | Totals |
| totalRows | number | no (`null` = unknown) | whole-result count | drives the export cap and `ReportRunResponse.totalRows`; the three-argument constructor leaves it null | إجمالي الصفوف | Total rows |
Source: report/ReportResult.java:6-32; report/ColumnType.java:3-9; report/dto/ReportRunResponse.java:15-35.

## A4 — Functional requirements (EARS) and acceptance criteria

### REQ-REPORT-001 — تسجيل تقرير بكشف bean / Register a report by exposing a bean
Pattern    : optional
Statement  : Where a module or application exposes a Spring bean implementing `ReportProvider`, the system shall collect it at start-up into the report catalog, ordered by module code then code, with no endpoint, migration or seed of the contributor's own.
Traces     : US-REPORT-005
Entities   : ENT-REPORT-001
Rationale  : POL-REPORT-001
Source     : report/registry/ReportRegistry.java:44-66; report/ReportProvider.java:8-12; docs/CONSUMING.md §6
Priority   : HIGH
#### AC-REPORT-001 — [REQ-REPORT-001]
Given the three core providers and the reference application's `AppSmokeReport` bean
When the application starts and a super role calls `GET /api/v1/report/definitions`
Then the answer lists exactly `SEC_USER_LIST`, `AUDIT_EVENT_LIST`, `NOTIF_LOG_SUMMARY` and `APP_SMOKE_REPORT`, each with `moduleCode`, `titleAr`, `titleEn`, `authority` and `params[]` (TC-CORE-REPORT-001; `ReferenceApplicationSmokeTest.reportDefinitionsListTheThreeCoreReportsAndTheApplicationsOwn`)

### REQ-REPORT-002 — رفض كتالوج غير صالح عند الإقلاع / Refuse an invalid catalog at start-up
Pattern    : unwanted
Statement  : If two providers share a code, or a provider's code is not upper snake case of 2–40 characters, or its module code is not upper case of 1–10 characters, or a title is blank, or a parameter lacks a name or a type, or a parameter name repeats, or a `LOOKUP` parameter has no lookup key, then the system shall fail the start-up with an `IllegalStateException` naming the provider class.
Traces     : US-REPORT-005
Entities   : ENT-REPORT-001
Rationale  : POL-REPORT-002 — codes become registry rows of fixed width (`SEC_ACTION_REG.ACTION_CODE VARCHAR(40)`, `SEC_MODULE_REG.CODE VARCHAR(10)`)
Source     : report/registry/ReportRegistry.java:39-40, :54-58, :78-101
Priority   : HIGH
#### AC-REPORT-002 — [REQ-REPORT-002]
Given two providers both answering code `DUP_REPORT`
When the registry is built
Then it throws `IllegalStateException` "Duplicate report code 'DUP_REPORT': contributed by <classA> and <classB>" (`ReportRegistryTest.duplicateCodes_failTheStartup_namingBothProviders`); a code `bad code`, a 41-character code, an 11-character module code, a blank title or a `LOOKUP` parameter without key each fail the same way

### REQ-REPORT-003 — تسجيل صلاحية كل تقرير تلقائيًا / Register every report's permission automatically
Pattern    : event
Statement  : When the permission catalog is synchronised at start-up, the system shall contribute, for each owning module of the catalog, the screen `<MODULE>_REPORTS` with its `VIEW` gateway `PERM_<MODULE>_REPORTS_VIEW`, and, for each report, one action whose code is the report code and whose authority is `<MODULE>:REPORT:<CODE>`, named by the report titles; it shall declare no module row.
Traces     : US-REPORT-005, US-REPORT-006
Entities   : ENT-REPORT-001
Rationale  : POL-REPORT-003; RULE-SEC-007 (a screen's non-VIEW grants count only with its VIEW)
Source     : report/permission/ReportPermissions.java:44-70; report/ReportAuthorities.java:18-26
Priority   : HIGH
#### AC-REPORT-003 — [REQ-REPORT-003]
Given the three core providers
When the application starts
Then `SEC_SCREEN_REG` holds `SEC_REPORTS`, `NOTIF_REPORTS` and `AUDIT_REPORTS`, `SEC_ACTION_REG` holds `PERM_SEC_REPORTS_VIEW`, `SEC:REPORT:SEC_USER_LIST`, `PERM_NOTIF_REPORTS_VIEW`, `NOTIF:REPORT:NOTIF_LOG_SUMMARY`, `PERM_AUDIT_REPORTS_VIEW`, `AUDIT:REPORT:AUDIT_EVENT_LIST`, and `SEC_MODULE_REG` keeps exactly one `AUDIT` row named by `AuditPermissions` (`ReportApiIntegrationTest.definitions_listTheThreeCoreReports_forASuperRole`; docs/steps/11-report.md "Module consistency")

### REQ-REPORT-004 — قائمة التعريفات المحمولة / List the definitions the caller holds
Pattern    : event
Statement  : When an authenticated staff caller asks for the report definitions, the system shall return only the reports whose `<MODULE>:REPORT:<CODE>` authority the caller holds, in catalog order, each with its parameters and authority.
Traces     : US-REPORT-001
Entities   : ENT-REPORT-001
Rationale  : POL-REPORT-004
Source     : report/controller/ReportController.java:42-46; report/service/ReportService.java:72-80
Priority   : HIGH
#### AC-REPORT-004 — [REQ-REPORT-004]
Given a role holding only `PERM_SEC_REPORTS_VIEW` and `SEC:REPORT:SEC_USER_LIST`, and a role with no report grant
When each calls `GET /api/v1/report/definitions`
Then the first answers exactly `[SEC_USER_LIST]` and the second `[]` (`ReportApiIntegrationTest.definitions_areFilteredByTheCallersAuthorities_andRunNeedsTheReportsPermission`; TC-CORE-REPORT-011, -012)

### REQ-REPORT-005 — قراءة تعريف واحد / Read one definition
Pattern    : event
Statement  : When an authenticated staff caller asks for the definition of a code, the system shall return it when the caller holds its authority; an unknown code shall answer not found before the authority is checked, and a held-less code forbidden.
Traces     : US-REPORT-001
Entities   : ENT-REPORT-001
Rationale  : POL-REPORT-004
Source     : report/controller/ReportController.java:48-52; report/service/ReportService.java:82-87, :126-132
Priority   : HIGH
#### AC-REPORT-005 — [REQ-REPORT-005]
Given a super role
When it calls `GET /api/v1/report/definitions/AUDIT_EVENT_LIST`, then `GET /api/v1/report/definitions/NOPE`
Then the first answers 200 with `moduleCode = AUDIT`, `authority = AUDIT:REPORT:AUDIT_EVENT_LIST`, `params[*].name = [action, entityType, entityId, actor, occurredFrom, occurredTo]`; the second 404 `REPORT_NOT_FOUND` (TC-CORE-REPORT-002; `ReportApiIntegrationTest.unknownReport_is404ReportNotFound`)

### REQ-REPORT-006 — تشغيل صفحة واحدة / Run one page
Pattern    : event
Statement  : When an authenticated staff caller posts `{params, page, size}` (body optional; `page ≥ 0`, `size` 1..200) to run a held report, the system shall validate the parameters, build the page through `PageableBuilder` (default size 20, maximum 200), call the provider inside the request's read-only transaction and answer `code, columns, rows, totals, page, size, totalRows` in the shared envelope.
Traces     : US-REPORT-002
Entities   : ENT-REPORT-001, ENT-REPORT-002
Rationale  : POL-REPORT-005, POL-REPORT-006
Source     : report/controller/ReportController.java:54-60; report/service/ReportService.java:89-104; report/dto/ReportRunRequest.java:22-31; report/mapper/ReportMapper.java:61-74
Priority   : HIGH
#### AC-REPORT-006 — [REQ-REPORT-006]
Given tenant A's administrator
When it posts `{"params":{"realm":"STAFF"},"page":0,"size":50}` to `POST /api/v1/report/SEC_USER_LIST/run`
Then the answer is 200 with `code = SEC_USER_LIST`, `columns[*].key = [username, email, fullNameAr, fullNameEn, realm, status, active, lastLoginAt, createdAt]`, only A's staff users in `rows`, `page = 0`, `size = 50`, `totalRows` = the number of rows (TC-CORE-REPORT-003); `size = 0` or `size = 201` answers 400 `VALIDATION_ERROR`

### REQ-REPORT-007 — التحقق من المعاملات دفعة واحدة / Validate the parameters all at once
Pattern    : unwanted
Statement  : If a run or export request carries a required parameter that is absent (null or blank), a value that does not convert to its declared type, a `LOOKUP` value that is not an active code of its lookup type, or a parameter the report does not declare, then the system shall reject it with 400 `REPORT_PARAM_INVALID` listing every failing parameter name in `fieldErrors`.
Traces     : US-REPORT-002, US-REPORT-003
Entities   : ENT-REPORT-001
Rationale  : POL-REPORT-006
Source     : report/domain/ReportParametersDomain.java:58-92; report/service/ReportService.java:134-154
Priority   : HIGH
#### AC-REPORT-007 — [REQ-REPORT-007]
Given `SEC_USER_LIST`
When a caller posts `{"params":{"activeOnly":"maybe","createdFrom":"2026-13-45","bogus":"1"}}` with `Accept-Language: ar`
Then the answer is 400 `REPORT_PARAM_INVALID` whose `fieldErrors[*].field` contains `activeOnly`, `createdFrom` and `bogus`, with Arabic messages (TC-CORE-REPORT-004; `ReportApiIntegrationTest.run_withAMissingRequiredOrInvalidParameter_is400ReportParamInvalid`); a `NOTIF_LOG_SUMMARY` run with `channel = FAX` is rejected on `channel` (TC-CORE-REPORT-017)

### REQ-REPORT-008 — تصدير CSV / Export as CSV
Pattern    : event
Statement  : When an authenticated staff caller posts `{params}` (body optional) to export a held report with `format=csv` (the default), the system shall ask the provider for one page of `max-export-rows + 1` rows and answer the CSV as an attachment `<code>.csv` of type `text/csv;charset=UTF-8`.
Traces     : US-REPORT-003
Entities   : ENT-REPORT-002
Rationale  : POL-REPORT-007, POL-REPORT-008
Source     : report/controller/ReportController.java:62-75; report/service/ReportService.java:106-124; report/domain/ReportRunDomain.java:19-21
Priority   : HIGH
#### AC-REPORT-008 — [REQ-REPORT-008]
Given the reference application's `APP_SMOKE_REPORT`
When a super role posts to `POST /api/v1/report/APP_SMOKE_REPORT/export`
Then the answer is 200 with `Content-Disposition: attachment; filename="APP_SMOKE_REPORT.csv"` and the body `EF BB BF` + `المعرف,التسمية\r\n1,alpha\r\n2,بيتا\r\n3,"gamma, delta"\r\n` for `Accept-Language: ar` (docs/steps/11-report.md Acceptance; TC-CORE-REPORT-005…008)

### REQ-REPORT-009 — تصدير JSON / Export as JSON
Pattern    : event
Statement  : When the export is requested with `format=json` (case-insensitive), the system shall answer the whole capped result as a `ReportRunResponse` document (no envelope, `page` and `size` null) as an attachment `<code>.json` of type `application/json`.
Traces     : US-REPORT-004
Entities   : ENT-REPORT-002
Rationale  : the Goal's "returns JSON or CSV" (docs/DEVIATIONS.md [11] "Export formats")
Source     : report/domain/ReportRunDomain.java:19-21, :63-75; report/service/ReportService.java:119-123; report/mapper/ReportMapper.java:60-74
Priority   : MEDIUM
#### AC-REPORT-009 — [REQ-REPORT-009]
Given `SEC_USER_LIST` in tenant A
When a caller posts to `POST /api/v1/report/SEC_USER_LIST/export?format=JSON`
Then the answer is 200 `application/json` with `code`, `columns`, `rows`, `totals`, `totalRows` and null `page`/`size`, and `rows` hold only A's users (TC-CORE-REPORT-009; `ReportApiIntegrationTest.jsonExport_...`)

### REQ-REPORT-010 — رفض صيغة تصدير غير مدعومة / Reject an unsupported export format
Pattern    : unwanted
Statement  : If the export `format` is neither `csv` nor `json` (blank = `csv`), then the system shall reject the request with 400 `REPORT_PARAM_INVALID` on field `format`.
Traces     : US-REPORT-004
Entities   : —
Rationale  : POL-REPORT-006 (the format is validated like a parameter)
Source     : report/domain/ReportRunDomain.java:63-75; report/domain/ReportParametersDomain.java:89-92
Priority   : MEDIUM
#### AC-REPORT-010 — [REQ-REPORT-010]
Given any held report
When a caller posts to `…/export?format=xml`
Then the answer is 400 `REPORT_PARAM_INVALID` with `fieldErrors[0].field = format` (TC-CORE-REPORT-010)

### REQ-REPORT-011 — تطبيق سقف التصدير / Enforce the export cap
Pattern    : unwanted
Statement  : If the provider returns more than `erp.core.report.max-export-rows` rows, or reports a `totalRows` above it, then the system shall reject the export with 422 `REPORT_EXPORT_TOO_LARGE` carrying the cap, without having loaded more than cap + 1 rows.
Traces     : US-REPORT-007
Entities   : ENT-REPORT-002
Rationale  : POL-REPORT-007
Source     : report/service/ReportService.java:114-117; report/domain/ReportRunDomain.java:51-61; autoconfigure/ErpCoreProperties.java:379-384
Priority   : HIGH
#### AC-REPORT-011 — [REQ-REPORT-011]
Given `erp.core.report.max-export-rows = 5`
When a report of 5 rows and then one of 6 rows are exported
Then the first answers 200 and the second 422 `REPORT_EXPORT_TOO_LARGE` with "…limit of 5 rows…" (`ReportApiIntegrationTest.exportCap_isEnforced`; TC-CORE-REPORT-014)

### REQ-REPORT-012 — رفض المستدعي بلا صلاحية التقرير / Refuse a caller without the report's authority
Pattern    : unwanted
Statement  : If an authenticated staff caller reads, runs or exports a registered report whose `<MODULE>:REPORT:<CODE>` authority its token does not carry, then the system shall answer 403 `ACCESS_DENIED`; a super role holds every report's authority.
Traces     : US-REPORT-006
Entities   : ENT-REPORT-001
Rationale  : POL-REPORT-004; ADR-REPORT-002
Source     : report/service/ReportService.java:126-132; report/domain/ReportRunDomain.java:44-49; common/util/SecurityContextHelper.java:40-47
Priority   : HIGH
#### AC-REPORT-012 — [REQ-REPORT-012]
Given a role holding only `PERM_SEC_REPORTS_VIEW` + `SEC:REPORT:SEC_USER_LIST`
When it runs `NOTIF_LOG_SUMMARY`
Then the answer is 403 `ACCESS_DENIED` (TC-CORE-REPORT-013); the same caller runs `SEC_USER_LIST` with 200

### REQ-REPORT-013 — التقرير داخل مستأجر المستدعي / Run inside the caller's tenant
Pattern    : ubiquitous
Statement  : The system shall run a provider on the request thread inside a read-only transaction whose Hibernate session carries the caller's tenant, so that a provider querying through JPA / `SpecBuilder` sees that tenant's rows only.
Traces     : US-REPORT-002, US-REPORT-003
Entities   : ENT-REPORT-002
Rationale  : POL-REPORT-005; POL-TENANT-007
Source     : report/service/ReportService.java:54-55, :72-73, :89-90, :106-107; report/ReportProvider.java:18-20
Priority   : HIGH
#### AC-REPORT-013 — [REQ-REPORT-013]
Given tenants A and B with their own users, notification logs and audit events
When A's administrator runs and exports `SEC_USER_LIST`, `NOTIF_LOG_SUMMARY` and `AUDIT_EVENT_LIST`
Then no row or total of B appears, and B's CSV holds exactly B's rows (`ReportApiIntegrationTest.coreReports_seeOnlyTheCallersTenant`; TC-CORE-REPORT-003, -015, -016)

### REQ-REPORT-014 — معاملات LOOKUP من قوائم MDL / LOOKUP parameters from MDL lookups
Pattern    : optional
Statement  : Where a report declares a `LOOKUP` parameter, the system shall accept only a code that is an active value of the MDL lookup type named by `lookupKey`, read through `MdlLookupApi.readActiveValuesByKey` inside the same read-only transaction; an unknown or inactive type makes every value of that parameter invalid.
Traces     : US-REPORT-002
Entities   : ENT-REPORT-001
Rationale  : POL-REPORT-006; the module reaches MDL only through its crossmodule package
Source     : report/service/ReportService.java:138-154; report/domain/ReportParametersDomain.java:196-203; mdl/crossmodule/MdlLookupApi.java:33
Priority   : MEDIUM
Note       : XM-REPORT-003. Known limitation: a lookup type missing in the caller's tenant may surface as a 500 on commit because MDL's `@Transactional` has marked the shared transaction rollback-only (docs/steps/11-report.md "Known limitation").
#### AC-REPORT-014 — [REQ-REPORT-014]
Given `NOTIF_LOG_SUMMARY` (`channel` → `NOTIF_CHANNEL`, `status` → `NOTIF_STATUS`)
When a caller runs it with `channel = EMAIL`, then with `channel = FAX`
Then the first answers 200 and the second 400 `REPORT_PARAM_INVALID` on `channel` (TC-CORE-REPORT-017; `ReportParametersDomainTest`)

### REQ-REPORT-015 — مسارات التقارير لطاقم العمل فقط / Report paths for the STAFF realm only
Pattern    : ubiquitous
Statement  : The system shall serve `/api/v1/report/**` on the staff security chain only: every service method requires an authenticated caller, and a customer-realm token is refused by the chain's realm filter.
Traces     : US-REPORT-001, US-REPORT-002
Entities   : —
Rationale  : reports are a back-office capability; the customer chain has no report path
Source     : report/service/ReportService.java:47-49, :73, :83, :90, :107; docs/steps/11-report.md Endpoints
Priority   : HIGH
#### AC-REPORT-015 — [REQ-REPORT-015]
Given a customer token and no token
When each calls `GET /api/v1/report/definitions`, `POST …/SEC_USER_LIST/run` and `POST …/SEC_USER_LIST/export`
Then the customer token answers 403 `REALM_MISMATCH` on all three and the anonymous call 401 (`ReportApiIntegrationTest.customerToken_isRejectedWith403`)

### REQ-REPORT-016 — ضبط سقف التصدير / Configure the export cap
Pattern    : optional
Statement  : Where an application sets `erp.core.report.max-export-rows`, the system shall bind it as a positive integer (default 100 000) and use it as the export cap of every report.
Traces     : US-REPORT-007
Entities   : —
Rationale  : POL-REPORT-007; core configuration is bound through `ErpCoreProperties` (ArchUnit rule 4)
Source     : autoconfigure/ErpCoreProperties.java:376-384; report/service/ReportService.java:114
Priority   : MEDIUM
#### AC-REPORT-016 — [REQ-REPORT-016]
Given no property set
When the application starts
Then the cap is 100 000; `erp.core.report.max-export-rows = 0` fails the binding (`@Positive`)

## A5 — Business rules

### RULE-REPORT-001 — صحة الكتالوج عند الإقلاع / Catalog validity at start-up
Scope      : ENT-REPORT-001
Trigger    : on start-up (registry construction)
Statement  : The system shall accept a provider only if its code matches `^[A-Z][A-Z0-9_]{1,39}$` and is unique, its module code matches `^[A-Z][A-Z0-9_]{0,9}$`, both titles are non-blank, every parameter has a name and a type, no parameter name repeats and every `LOOKUP` parameter has a lookup key; otherwise the start-up fails naming the class.
Data source: every `ReportProvider` bean
Message    : `IllegalStateException` "Invalid report provider <class>: <reason>" / "Duplicate report code '<code>': contributed by <a> and <b>" (not a `LocalizedException`: start-up, not a request path)
Traces     : REQ-REPORT-002
Source     : report/registry/ReportRegistry.java:39-40, :54-58, :78-101

### RULE-REPORT-002 — ترتيب الكتالوج / Catalog order
Scope      : ENT-REPORT-001
Trigger    : on start-up; on every list
Statement  : The system shall order the catalog by module code, then by code, and list definitions in that order.
Data source: the registry
Message    : —
Traces     : REQ-REPORT-001, REQ-REPORT-004
Source     : report/registry/ReportRegistry.java:60-64, :69-71

### RULE-REPORT-003 — المستدعي يرى ويشغّل ما يحمله فقط / Only held reports are visible and runnable
Scope      : ENT-REPORT-001
Trigger    : on list, read, run, export
Statement  : The system shall show a report in the definitions list only if the caller's token carries `<MODULE>:REPORT:<CODE>`, and shall refuse read, run and export of a registered report without it; a super role holds every active catalog authority.
Data source: the token's authorities (`SecurityContextHelper.hasAuthority`)
Message    : ar: "ليس لديك صلاحية لتنفيذ هذه العملية" · en: "You do not have permission to perform this operation" (`ACCESS_DENIED`, 403)
Traces     : REQ-REPORT-004, REQ-REPORT-005, REQ-REPORT-012
Source     : report/service/ReportService.java:76-77, :130; report/domain/ReportRunDomain.java:44-49

### RULE-REPORT-004 — قواعد المعاملات / Parameter rules
Scope      : ENT-REPORT-001 (params)
Trigger    : on run, on export
Statement  : The system shall treat null and blank as absent; require every `required` parameter; convert `STRING` to a trimmed non-blank text, `INTEGER` to `Long`, `DECIMAL` to `BigDecimal`, `DATE` to an ISO-8601 `LocalDate`, `DATETIME` to an `Instant` (offset or `Z`; a value without zone is read as UTC), `BOOLEAN` to `true`/`false` only, `LOOKUP` to an active code of its type; reject every undeclared name; and report all failures at once.
Data source: the request's `params`; MDL active codes for `LOOKUP`
Message    : ar: "معامل التقرير ''{0}'' مفقود أو غير صالح" · en: "Report parameter ''{0}'' is missing or invalid" (`REPORT_PARAM_INVALID`, one `fieldErrors` entry per parameter, 400)
Traces     : REQ-REPORT-007, REQ-REPORT-014
Source     : report/domain/ReportParametersDomain.java:58-203; report/ParamType.java:3-17

### RULE-REPORT-005 — صفحة واحدة في التشغيل / One page per run
Scope      : ENT-REPORT-002
Trigger    : on run
Statement  : The system shall run exactly one page: `page ≥ 0`, `size` 1..200 (request validation), built by `PageableBuilder` (default 20, maximum 200, no sort), and hand the provider that `Pageable`.
Data source: `ReportRunRequest.page`, `.size`
Message    : `VALIDATION_ERROR` 400 with `fieldErrors` on `page` / `size` (`{validation.min}` / `{validation.max}`)
Traces     : REQ-REPORT-006
Source     : report/dto/ReportRunRequest.java:22-31; report/service/ReportService.java:97-101; common/search/PageableBuilder.java:28-45

### RULE-REPORT-006 — سقف التصدير بسبر cap + 1 / Export cap by a cap + 1 probe
Scope      : ENT-REPORT-002
Trigger    : on export
Statement  : The system shall ask the provider for page 0 of `max-export-rows + 1` rows (the cap itself when the cap is `Integer.MAX_VALUE`) and refuse the export when the rows returned, or the provider's `totalRows`, exceed the cap.
Data source: `ErpCoreProperties.report.maxExportRows`; `ReportResult.rows().size()`, `.totalRows()`
Message    : ar: "يتجاوز التصدير الحد الأقصى البالغ {0} صف…" · en: "The export exceeds the limit of {0} rows. Narrow the report parameters and try again." (`REPORT_EXPORT_TOO_LARGE`, 422 `BUSINESS_RULE_VIOLATION`)
Traces     : REQ-REPORT-011
Source     : report/service/ReportService.java:114-117; report/domain/ReportRunDomain.java:51-61

### RULE-REPORT-007 — صيغة التصدير / Export format
Scope      : export request
Trigger    : on export
Statement  : The system shall accept `csv` (default when blank) and `json`, case-insensitively, and refuse any other value.
Data source: query parameter `format`
Message    : `REPORT_PARAM_INVALID` on field `format` (400)
Traces     : REQ-REPORT-009, REQ-REPORT-010
Source     : report/domain/ReportRunDomain.java:19-38, :63-75

### RULE-REPORT-008 — شكل CSV / CSV shape
Scope      : ENT-REPORT-002 (CSV)
Trigger    : on CSV export
Statement  : The system shall write the UTF-8 BOM `EF BB BF`, a header row of the columns' Arabic labels when the request locale's language is `ar` and English labels otherwise, CRLF record separators, RFC 4180 quoting (a field containing a comma, a double quote, CR, LF or a leading/trailing blank is enclosed in double quotes with embedded quotes doubled), numbers plain (`BigDecimal.toPlainString`), dates and times ISO-8601, booleans `true`/`false`, `null` empty; totals are never written.
Data source: `ReportResult.columns()`, `.rows()`; `LocaleContextHolder`
Message    : —
Traces     : REQ-REPORT-008
Source     : report/export/CsvReportWriter.java:21-34, :47-67, :80-101; report/service/ReportService.java:157-159

### RULE-REPORT-009 — حارس حقن الصيغ / Formula-injection guard
Scope      : ENT-REPORT-002 (CSV, STRING cells)
Trigger    : on CSV export
Statement  : The system shall prefix a `STRING` cell whose text starts with `=`, `+`, `-`, `@`, TAB or CR with an apostrophe, so that a spreadsheet shows it as text instead of evaluating it.
Data source: `ColumnType.STRING` values
Message    : —
Traces     : REQ-REPORT-008
Source     : report/export/CsvReportWriter.java:30-32, :102-104

### RULE-REPORT-010 — جسم التصدير محدود ومرفق / Bounded attachment body
Scope      : export response
Trigger    : on export
Statement  : The system shall answer the export as a `byte[]` bounded by the cap (never a streaming body), with `Content-Disposition: attachment; filename="<code>.csv"` or `"<code>.json"` (UTF-8 filename) and the format's content type.
Data source: `ReportService.ReportExport(fileName, contentType, content)`
Message    : —
Traces     : REQ-REPORT-008, REQ-REPORT-009
Source     : report/controller/ReportController.java:65-75; report/service/ReportService.java:68-70, :119-123; docs/DEVIATIONS.md [11] "streams `text/csv`"

### RULE-REPORT-011 — المزوّد يعمل داخل المستأجر ويستعلم عبر JPA / Provider runs in the tenant and queries through JPA
Scope      : every `ReportProvider.run`
Trigger    : on run, on export
Statement  : The system shall call the provider inside the request's read-only transaction under the caller's tenant; a provider shall query through JPA repositories / `SpecBuilder`, or include `TENANT_ID` in any native SQL (ArchUnit rule 7 forbids native SQL outside tenant, sequence and audit anyway).
Data source: the request's `TenantContext`
Message    : —
Traces     : REQ-REPORT-013
Source     : report/ReportProvider.java:18-20; report/service/ReportService.java:54-55; `erp-core/src/test/java/com/erp/architecture/CoreLibraryRulesArchTest.java:78`, `:266-291`; docs/CONSUMING.md §6

### RULE-REPORT-012 — المزوّد يحترم حجم الصفحة / Provider honours the page size
Scope      : every `ReportProvider.run`
Trigger    : on run, on export
Statement  : A provider shall return at most `page.getPageSize()` rows of page `page.getPageNumber()` and, when it knows it, the whole-result count in `totalRows`; the export relies on this to detect an over-cap result from one page of cap + 1 rows.
Data source: the `Pageable` passed to `run`
Message    : —
Traces     : REQ-REPORT-006, REQ-REPORT-011
Source     : report/ReportProvider.java:42-50; report/ReportResult.java:10-13; sec/report/SecUserListReport.java:114-118; notif/report/NotifLogSummaryReport.java:236-238; audit/report/AuditEventListReport.java:382-386

## A6 — Lookups

Value sets owned by the module are Java enums, not MDL lookup types (CHECK-less: no table):

**ParamType** — report/ParamType.java:18-26
| Code | Java value handed to the provider | Label (ar) | Label (en) |
|---|---|---|---|
| STRING | `String` (trimmed, never blank) | نص | Text |
| INTEGER | `Long` | عدد صحيح | Integer |
| DECIMAL | `BigDecimal` | عدد عشري | Decimal |
| DATE | `LocalDate` (ISO `yyyy-MM-dd`) | تاريخ | Date |
| DATETIME | `Instant` (offset / `Z`; no zone = UTC) | تاريخ ووقت | Date-time |
| BOOLEAN | `Boolean` (`true` / `false`) | منطقي | Boolean |
| LOOKUP | `String` — an active code of the MDL type `lookupKey` | قيمة قائمة | Lookup value |

**ColumnType** — report/ColumnType.java:10-17: `STRING`, `INTEGER`, `DECIMAL`, `DATE`, `DATETIME`, `BOOLEAN`
(the same labels as above without LOOKUP); row values `String`, `Number`, `LocalDate`,
`Instant` / `OffsetDateTime` / `LocalDateTime`, `Boolean`; `null` = empty cell.

**ReportRunDomain.ExportFormat** — report/domain/ReportRunDomain.java:19-38
| Code | Content type | Extension | Label (ar) | Label (en) |
|---|---|---|---|---|
| CSV | `text/csv;charset=UTF-8` | `csv` | ملف CSV | CSV file |
| JSON | `application/json` | `json` | ملف JSON | JSON file |

Consumed lookups: the MDL lookup type named by each `LOOKUP` parameter (today `NOTIF_CHANNEL`,
`NOTIF_STATUS` — notif/report/NotifLogSummaryReport.java:218-219), active values only, through
`MdlLookupApi.readActiveValuesByKey` (report/service/ReportService.java:147-148).

## A7 — Status lifecycle
Not applicable — the module has no entity with a status. A report definition is static for the life of
the process (collected once, never changed); a result is a value returned once.

## A8 — Module dependencies

| Consumed entity | Owner ENT id | Owner module | HARD-FK / SOFT-READ | XM |
|---|---|---|---|---|
| MDL lookup values (active codes of a type) | ENT-MDL-002 (`MDL_LOOKUP_VALUE`) | MDL | SOFT-READ through `MdlLookupApi.readActiveValuesByKey` (read model `LookupOptionView`, never the entity) | XM-REPORT-003 |

| Consumed surface | Owner | Kind | Source |
|---|---|---|---|
| `PermissionContributor` (`PermissionDef`, `PermissionScreen`) | SEC | SPI implemented by `ReportPermissions`; its rows are upserted into `SEC_SCREEN_REG` / `SEC_ACTION_REG` by `PermissionCatalogSynchronizer` | report/permission/ReportPermissions.java:6-8, :32; XM-REPORT-002 |
| `ErpCoreProperties.Report` | autoconfigure | configuration (`max-export-rows`) | report/service/ReportService.java:65, :114 |
| `SecurityContextHelper.hasAuthority`, `PageableBuilder`, `ServiceResult`, `LocalizedException`, `CommonErrorCodes.ACCESS_DENIED` | common | foundation | report/service/ReportService.java:4-9; report/domain/ReportRunDomain.java:3-5 |

**Exposed direction** (consumers of the report module)
| XM id | Exposed surface | Kind | Consumers / implementers | Source |
|---|---|---|---|---|
| XM-REPORT-001 | `com.erp.report.ReportProvider` with `ReportParam`, `ReportColumn`, `ReportResult`, `ParamType`, `ColumnType`, `ReportAuthorities` (root package, public in ArchUnit) | SPI | SEC `SecUserListReport` (`SEC_USER_LIST`), NOTIF `NotifLogSummaryReport` (`NOTIF_LOG_SUMMARY`), AUDIT `AuditEventListReport` (`AUDIT_EVENT_LIST`), reference app `AppSmokeReport` (`APP_SMOKE_REPORT`); any application | report/ReportProvider.java:25-51; `erp-core/src/test/java/com/erp/architecture/CrossModuleBoundaryArchTest.java:66-68`; sec/report/SecUserListReport.java:43; notif/report/NotifLogSummaryReport.java:178; audit/report/AuditEventListReport.java:303; `erp-app-reference/src/main/java/com/erp/app/report/AppSmokeReport.java:32` |
| XM-REPORT-002 | catalog rows `<MODULE>_REPORTS` (screen), `PERM_<MODULE>_REPORTS_VIEW` and `<MODULE>:REPORT:<CODE>` (actions) contributed to SEC's global registry | registry rows (written by SEC's synchronizer, widths in `../P2/db-script-report.md`) | SEC (owner of `SEC_SCREEN_REG` / `SEC_ACTION_REG`), every role editor, the frontend menu | report/permission/ReportPermissions.java:44-70 |
| XM-REPORT-003 | (consume direction, recorded here for the index) `MdlLookupApi.readActiveValuesByKey` | crossmodule read | REPORT → MDL | report/service/ReportService.java:143-154 |

# PART B — SCREEN REQUIREMENTS

## SCR-REQ-REPORT-001 — تقارير الوحدة / Module reports (`<MODULE>_REPORTS`, one dynamic screen per owning module)
### B1 — Definition
Purpose      : عرض تقارير وحدة واحدة المسموح للمستخدم بتشغيلها، وتشغيل كل منها بمعاملاته صفحةً صفحة، وتصديره CSV أو JSON.
Entities     : ENT-REPORT-001, ENT-REPORT-002
Operations   : list (definitions filtered to the caller), read one definition, run (one page), export
Users        : مستخدم طاقم العمل الذي يحمل `PERM_<MODULE>_REPORTS_VIEW` وإجراء تقرير واحد على الأقل
Navigation   : `<MODULE>` → Reports; one screen row per owning module is registered dynamically from the catalog (today `SEC_REPORTS`, `NOTIF_REPORTS`, `AUDIT_REPORTS`; in the reference app also `APP_REPORTS`)
Content shape: a report picker (the held definitions of the module) + a parameter form built from `params[]` + a paged result grid (`columns[]`, `rows[]`, `totals`) + export buttons
Traces       : REQ-REPORT-004…013, REQ-REPORT-015
Composite    : Picker + Parameter form + Result grid = ONE screen requirement, instantiated once per module
### B2 — Search / list
The definitions list is the module's held reports (`moduleCode = <MODULE>`); results are paged by `page`/`size`
(1..200) with the provider's own fixed order (no client sort). The run response carries `totalRows` when
the provider knows it (report/service/ReportService.java:76-80, :97-103).
### B3 — Input
Parameters: `params[]` of the definition — `name`, `type` (`ParamType`), `required`, `lookupKey`
(`LOOKUP` → MDL options of that type), `labelAr`, `labelEn` (report/dto/ReportParamResponse.java:13-29).
Run: `{params, page, size}`; export: `{params}` + query `format` (report/dto/ReportRunRequest.java:15-32;
report/dto/ReportExportRequest.java:13-19). Dates are ISO-8601 strings.
### B4 — Access
Page code: `<MODULE>_REPORTS`. Actions: `VIEW` (`PERM_<MODULE>_REPORTS_VIEW`, gateway / menu entry only, grants
no report) and one action per report with code `<CODE>` and authority `<MODULE>:REPORT:<CODE>` (required by
read, run and export of that report; the list shows only held reports). All endpoints additionally require
an authenticated STAFF caller (`isAuthenticated()`; the authority is checked in code, not by `@PreAuthorize`).
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | Permission | RULEs | Errors | Traces (REQ) |
|---|---|---|---|---|---|---|---|---|
| list definitions | GET | /api/v1/report/definitions | — | 200 `List<ReportDefinitionResponse>` (held reports only) | `isAuthenticated()`; filtered by `<MODULE>:REPORT:<CODE>` | RULE-REPORT-002, -003 | 401; 403 `REALM_MISMATCH` (customer token) | REQ-REPORT-004, -015 |
| get definition | GET | /api/v1/report/definitions/{code} | code | 200 `ReportDefinitionResponse` | `isAuthenticated()` + `<MODULE>:REPORT:<CODE>` | RULE-REPORT-003 | 404 `REPORT_NOT_FOUND`, 403 `ACCESS_DENIED` | REQ-REPORT-005, -012 |
| run | POST | /api/v1/report/{code}/run | `ReportRunRequest` `{params, page, size}` (optional) | 200 `ReportRunResponse` (envelope) | `isAuthenticated()` + `<MODULE>:REPORT:<CODE>` | RULE-REPORT-003, -004, -005, -011, -012 | 404 `REPORT_NOT_FOUND`, 403 `ACCESS_DENIED`, 400 `REPORT_PARAM_INVALID` (every failing parameter in `fieldErrors`), 400 `VALIDATION_ERROR` (page/size, malformed body) | REQ-REPORT-006, -007, -012, -013, -014 |
| export | POST | /api/v1/report/{code}/export?format=csv\|json | `ReportExportRequest` `{params}` (optional); query `format` (default `csv`) | 200 attachment `<code>.csv` (`text/csv;charset=UTF-8`) or `<code>.json` (`application/json`), raw body (no envelope) | `isAuthenticated()` + `<MODULE>:REPORT:<CODE>` | RULE-REPORT-003, -004, -006…012 | 404 `REPORT_NOT_FOUND`, 403 `ACCESS_DENIED`, 400 `REPORT_PARAM_INVALID` (parameters; field `format` for an unsupported format), 422 `REPORT_EXPORT_TOO_LARGE` | REQ-REPORT-007…011, -013 |
Source: report/controller/ReportController.java:33-76; `docs/api-docs/report/endpoints/reports.md`.

# STANDALONE

## Error codes (HTTP status as the code has it)
| Code | HTTP | Raised by | Code location | Message (en / ar) |
|---|---|---|---|---|
| `REPORT_NOT_FOUND` | 404 | `ReportService.permittedProvider` (`Status.NOT_FOUND`); argument: the code | report/service/ReportService.java:128-129; report/exception/ReportErrorCodes.java:16 | "Report ''{0}'' was not found" / "التقرير ''{0}'' غير موجود" |
| `REPORT_PARAM_INVALID` | 400 | `ReportParametersDomain.validate` (one `ErrorDetail.ofField` per failing parameter, `Status.VALIDATION_ERROR`); `ReportRunDomain.exportFormat` (field `format`); argument: the parameter name | report/domain/ReportParametersDomain.java:83-92; report/domain/ReportRunDomain.java:74; report/exception/ReportErrorCodes.java:23 | "Report parameter ''{0}'' is missing or invalid" / "معامل التقرير ''{0}'' مفقود أو غير صالح" |
| `REPORT_EXPORT_TOO_LARGE` | 422 | `ReportRunDomain.assertWithinExportCap` (`Status.BUSINESS_RULE_VIOLATION`); argument: the cap | report/domain/ReportRunDomain.java:57-59; report/exception/ReportErrorCodes.java:26 | "The export exceeds the limit of {0} rows. Narrow the report parameters and try again." / "يتجاوز التصدير الحد الأقصى البالغ {0} صف. يرجى تضييق معاملات التقرير والمحاولة مجددًا" |
| `ACCESS_DENIED` (common) | 403 | `ReportRunDomain.assertCanRun` (`Status.FORBIDDEN`) | report/domain/ReportRunDomain.java:45-48; common/exception/CommonErrorCodes.java:11 | "You do not have permission to perform this operation" / "ليس لديك صلاحية لتنفيذ هذه العملية" |
`Status` → HTTP: common/domain/status/Status.java:10-20. Messages: `erp-core/src/main/resources/i18n/messages.properties`
lines 178–180, `messages_ar.properties` lines 175–177. Shared codes the endpoints can also answer:
`VALIDATION_ERROR` 400 (body / `page` / `size`), `REALM_MISMATCH` 403 (customer token on the staff chain),
`SEC-401-INVALID-CREDENTIALS` 401, `INTERNAL_ERROR` 500.

## Core reference reports (as built)
| Code | Module | Authority | Parameters (type) | Columns (type) | Query | Source |
|---|---|---|---|---|---|---|
| `SEC_USER_LIST` | SEC | `SEC:REPORT:SEC_USER_LIST` | `realm` (STRING), `status` (STRING), `activeOnly` (BOOLEAN), `createdFrom`, `createdTo` (DATE) — all optional | `username`, `email`, `fullNameAr`, `fullNameEn`, `realm`, `status` (STRING), `active` (BOOLEAN), `lastLoginAt`, `createdAt` (DATETIME); totals `users` | `UserRepository.findAll(SpecBuilder …)` sorted by username, realm; never the password hash | sec/report/SecUserListReport.java:45-118 |
| `NOTIF_LOG_SUMMARY` | NOTIF | `NOTIF:REPORT:NOTIF_LOG_SUMMARY` | `channel` (LOOKUP `NOTIF_CHANNEL`), `status` (LOOKUP `NOTIF_STATUS`), `dateFrom`, `dateTo` (DATE) — all optional | `day` (DATE), `channel`, `status` (STRING), `count` (INTEGER); totals `count` | JPQL group-by through `NotificationLogSummaryRepository.summarize`, paged in memory (exact `totalRows`) | notif/report/NotifLogSummaryReport.java:179-239; notif/repository/NotificationLogSummaryRepository.java:17-31 |
| `AUDIT_EVENT_LIST` | AUDIT | `AUDIT:REPORT:AUDIT_EVENT_LIST` | `action`, `entityType`, `entityId`, `actor` (STRING), `occurredFrom`, `occurredTo` (DATETIME) — all optional | `occurredAt` (DATETIME), `action`, `actor`, `actorRealm`, `entityType`, `entityId`, `summaryAr`, `summaryEn`, `ip`, `reference` (STRING); totals `events` | `AuditEventRepository.findAll(SpecBuilder …)` newest first; `CHANGES` is not a column; independent of `AUDIT:EVENT:READ` | audit/report/AuditEventListReport.java:304-386 |
| `APP_SMOKE_REPORT` (reference app) | APP | `APP:REPORT:APP_SMOKE_REPORT` | `label` (STRING, optional) | `id` (INTEGER), `label` (STRING) | fixed three rows, no database | `erp-app-reference/src/main/java/com/erp/app/report/AppSmokeReport.java:34-78` |
Each provider also carries its own same-module `@PreAuthorize("hasAuthority(T(<class>).AUTHORITY)")`
(defence in depth; the ArchUnit rule-5 exception for a `ReportProvider`'s `AUTHORITY` constant).

## Traceability matrix
| P0.5 | REQ | AC | RULE | ENT | SCR-REQ |
|---|---|---|---|---|---|
| US-REPORT-001 | REQ-REPORT-004, -005, -015 | AC-REPORT-004, -005, -015 | RULE-REPORT-002, -003 | ENT-REPORT-001 | SCR-REQ-REPORT-001 |
| US-REPORT-002 | REQ-REPORT-006, -007, -013, -014, -015 | AC-REPORT-006, -007, -013, -014, -015 | RULE-REPORT-003, -004, -005, -011, -012 | ENT-REPORT-001, -002 | SCR-REQ-REPORT-001 |
| US-REPORT-003 | REQ-REPORT-007, -008, -013 | AC-REPORT-007, -008, -013 | RULE-REPORT-006, -008, -009, -010, -011 | ENT-REPORT-002 | SCR-REQ-REPORT-001 |
| US-REPORT-004 | REQ-REPORT-009, -010 | AC-REPORT-009, -010 | RULE-REPORT-007, -010 | ENT-REPORT-002 | SCR-REQ-REPORT-001 |
| US-REPORT-005 | REQ-REPORT-001, -002, -003 | AC-REPORT-001, -002, -003 | RULE-REPORT-001, -002, -011, -012 | ENT-REPORT-001 | — |
| US-REPORT-006 | REQ-REPORT-003, -012 | AC-REPORT-003, -012 | RULE-REPORT-003 | ENT-REPORT-001 | SCR-REQ-REPORT-001 |
| US-REPORT-007 | REQ-REPORT-011, -016 | AC-REPORT-011, -016 | RULE-REPORT-006 | ENT-REPORT-002 | — |

Every story traces to ≥ 1 REQ; every REQ has one AC; every RULE traces to a REQ; the screen traces to
its REQs. No orphan, no dangling id.

## Decisions applied
| DEFAULT / ADR | What | Source | Status |
|---|---|---|---|
| ADR-REPORT-001 | reports as code: `ReportProvider` SPI + fail-fast start-up registry, no persistence | governance/analysis/decisions/REPORT/ADR-REPORT-001.md | ACCEPTED (as built) |
| ADR-REPORT-002 | automatic permission registration (one screen per module, one action per report) checked in code, not by `@PreAuthorize` | governance/analysis/decisions/REPORT/ADR-REPORT-002.md | ACCEPTED (as built) |
| ADR-REPORT-003 | bounded `byte[]` export with cap + 1 probe, CSV BOM and formula-injection guard | governance/analysis/decisions/REPORT/ADR-REPORT-003.md | ACCEPTED (as built) |

## Access summary
| Page code | Screen | VIEW | CREATE | UPDATE | DELETE | Custom |
|---|---|---|---|---|---|---|
| `<MODULE>_REPORTS` (today `SEC_REPORTS`, `NOTIF_REPORTS`, `AUDIT_REPORTS`; app `APP_REPORTS`) | `<MODULE>` Reports | `PERM_<MODULE>_REPORTS_VIEW` (gateway, menu only) | — | — | — | `<MODULE>:REPORT:<CODE>` per report: read definition, run, export |
The frontend's report screen archive is `governance/frontend/modules/REPORT/tests/`.
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 11
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : none — no report delta is planned for 1.3.0; the TENANT data export of `docs/plans/tenant-maturity-plan.md` C.5 does NOT use the report engine (own `TenantExportContributor` SPI in `com.erp.tenant`, `POST /api/v1/platform/tenants/{id}/export`, ZIP stored as a `FILE_DOCUMENT`).
Statement      : This addendum records the deltas being implemented for 1.3.0; every row is verified against the code before the tag.

### 1. Endpoints
| Kind | Method | Path | Access | Notes | Source |
|---|---|---|---|---|---|
| — | — | none | — | no new or changed report endpoint | docs/plans/tenant-maturity-plan.md C.5 |

### 2. Business rules / 3. Error codes / 4. Permissions / 5. Entities
none.
