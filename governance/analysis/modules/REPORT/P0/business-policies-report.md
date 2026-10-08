## BUSINESS POLICIES — التقارير / Report (REPORT)
══════════════════════════════════════════════════════════════════
Module   : REPORT  Source of truth : the code at main @ 19b19a4 (erp-core 1.2.0 behaviour);
           erp-core-plan/11-STEP-reporting-minimal.md; docs/steps/11-report.md; docs/DEVIATIONS.md [11]; docs/CONSUMING.md §6
Read by  : P0.5 (every user story cites the policies it serves)
══════════════════════════════════════════════════════════════════

As-built baseline: every policy below is CONFIRMED by the code location in its `Source` line, not by a
dialogue. Java paths are relative to `erp-core/src/main/java/com/erp/`.

CLIENT-SPECIFIC POLICIES

POL-REPORT-001 — التقارير كود مسجّل، لا بيانات / Reports are registered code, not stored data
  Statement (ar) : يجب على النظام أن يتعرّف على التقرير فقط من bean ينفّذ `ReportProvider`، وأن يجمع الكتالوج عند الإقلاع، وألا يخزّن أي تعريف تقرير في قاعدة البيانات.
  Statement (en) : The system shall know a report only from a Spring bean implementing `ReportProvider`, shall collect the catalog at start-up, and shall store no report definition in the database.
  Pattern   : ubiquitous
  Trigger   : Application start-up
  Rationale : a report is versioned and reviewed with the code that owns its query; no runtime editing surface, no table, no migration
  Source    : report/ReportProvider.java:8-12; report/registry/ReportRegistry.java:44-47; docs/steps/11-report.md "Untouched: every migration"
  Status    : CONFIRMED (as built)

POL-REPORT-002 — الكتالوج غير الصالح يمنع الإقلاع / An invalid catalog stops the start-up
  Statement (ar) : إذا احتوى الكتالوج على رمز مكرر أو رمز/وحدة بصيغة غير صالحة أو عنوان فارغ أو معامل ناقص أو مكرر أو معامل LOOKUP بلا مفتاح، فيجب على النظام إيقاف الإقلاع مسمّيًا الصنف المخالف.
  Statement (en) : If the catalog holds a duplicate code, a malformed code or module code, a blank title, an incomplete or duplicate parameter, or a `LOOKUP` parameter without a key, then the system shall fail the start-up naming the offending provider class.
  Pattern   : unwanted
  Trigger   : Application start-up
  Rationale : a broken catalog is a programming error of the contributing module; the registry's codes become registry rows with fixed widths, so they are checked before anything is written
  Source    : report/registry/ReportRegistry.java:19-34, :54-58, :78-101
  Status    : CONFIRMED (as built)

POL-REPORT-003 — لكل تقرير صلاحيته المولَّدة تلقائيًا / Every report has its own automatically registered permission
  Statement (ar) : يجب على النظام أن يمنح كل تقرير مسجّل الصلاحية `<MODULE>:REPORT:<CODE>` كإجراء على الشاشة `<MODULE>_REPORTS` ذات البوابة `PERM_<MODULE>_REPORTS_VIEW`، وأن يكتبها في كتالوج الصلاحيات عند الإقلاع دون أي ترحيل.
  Statement (en) : The system shall give every registered report the permission `<MODULE>:REPORT:<CODE>` as an action of the screen `<MODULE>_REPORTS` whose gateway is `PERM_<MODULE>_REPORTS_VIEW`, written into the permission catalog at start-up with no migration.
  Pattern   : ubiquitous
  Trigger   : Application start-up (catalog synchronisation)
  Rationale : administrators grant reports like any other action; super roles hold them at once; one screen per module because `SEC_SCREEN_REG.PAGE_CODE` is globally unique
  Source    : report/permission/ReportPermissions.java:16-29, :49-60; report/ReportAuthorities.java:18-21; docs/DEVIATIONS.md [11] "Permissions"
  Status    : CONFIRMED (as built)

POL-REPORT-004 — المستدعي يرى ويشغّل ما يحمله فقط / A caller sees and runs only the reports it holds
  Statement (ar) : يجب على النظام ألا يعرض في قائمة التعريفات إلا التقارير التي يحمل المستدعي صلاحيتها، وأن يرفض قراءة تعريف تقرير أو تشغيله أو تصديره دون تلك الصلاحية؛ الرمز غير المعروف يُجاب "غير موجود" قبل فحص الصلاحية.
  Statement (en) : The system shall list only the reports whose authority the caller holds, and shall refuse reading, running or exporting a report without that authority; an unknown code is answered not found before the authority is checked.
  Pattern   : ubiquitous
  Trigger   : Any `/api/v1/report/**` request
  Rationale : the required authority depends on the path variable, so it is checked in code (not by a fixed `@PreAuthorize`), with the common `ACCESS_DENIED`
  Source    : report/service/ReportService.java:74-80, :126-132; report/domain/ReportRunDomain.java:44-49
  Status    : CONFIRMED (as built)

POL-REPORT-005 — التقرير يعمل داخل مستأجر المستدعي / A report runs inside the caller's tenant
  Statement (ar) : يجب على النظام تشغيل مزوّد التقرير داخل معاملة القراءة على خيط الطلب الذي ضبطت سلسلة الأمان مستأجره، ويجب على كل مزوّد أن يستعلم عبر JPA/`SpecBuilder` أو أن يسمّي `TENANT_ID` في أي SQL أصلي.
  Statement (en) : The system shall run a report provider inside the request's read-only transaction, whose tenant the security chain has set, and every provider shall query through JPA / `SpecBuilder` or name `TENANT_ID` in any native SQL.
  Pattern   : ubiquitous
  Trigger   : Run or export
  Rationale : POL-TENANT-007 — isolation by construction (Hibernate `@TenantId`), no cross-tenant rows in any report
  Source    : report/ReportProvider.java:18-20; report/service/ReportService.java:54-55, :72-73; docs/CONSUMING.md §6
  Status    : CONFIRMED (as built)

POL-REPORT-006 — المعاملات مُعلَنة ومُتحقَّق منها كلها دفعة واحدة / Parameters are declared and validated all at once
  Statement (ar) : يجب على النظام قبول المعاملات المعلنة فقط، وتحويل كل قيمة إلى نوعها المعلن، واعتبار الفراغ غيابًا، ورفض الطلب بقائمة كل المعاملات الفاشلة معًا.
  Statement (en) : The system shall accept only declared parameters, convert every value to its declared type, treat a blank value as absent, and reject the request listing every failing parameter at once.
  Pattern   : unwanted
  Trigger   : Run or export
  Rationale : a provider receives typed, trusted values only; a client fixes all its mistakes in one round trip
  Source    : report/domain/ReportParametersDomain.java:30-33, :58-87
  Status    : CONFIRMED (as built)

POL-REPORT-007 — التصدير محدود السقف / An export is bounded
  Statement (ar) : يجب على النظام رفض تصدير يتجاوز `erp.core.report.max-export-rows` صفًا دون تحميل النتيجة كاملة، وأن يبقي جسم الاستجابة مصفوفة بايتات محدودة بذلك السقف.
  Statement (en) : The system shall refuse an export exceeding `erp.core.report.max-export-rows` rows without loading the whole result, and shall keep the response body a byte array bounded by that cap.
  Pattern   : unwanted
  Trigger   : Export
  Rationale : memory and time are bounded per request; the provider is asked for cap + 1 rows, which is enough to detect the overflow
  Source    : report/service/ReportService.java:114-117; report/domain/ReportRunDomain.java:55-61; autoconfigure/ErpCoreProperties.java:379-384; ADR-REPORT-003
  Status    : CONFIRMED (as built)

POL-REPORT-008 — CSV يفتحه Excel بأمان وبلغة المستدعي / CSV opens safely in Excel, in the caller's language
  Statement (ar) : يجب على النظام كتابة CSV بعلامة ترتيب البايتات UTF-8 وبفواصل أسطر CRLF واقتباس RFC 4180، برؤوس عربية عند `Accept-Language: ar` وإنجليزية خلاف ذلك، وأن يسبق كل خلية نصية تبدأ بـ `=` أو `+` أو `-` أو `@` أو TAB أو CR بعلامة اقتباس مفردة.
  Statement (en) : The system shall write CSV with the UTF-8 byte-order mark, CRLF records and RFC 4180 quoting, with Arabic headers for `Accept-Language: ar` and English otherwise, and shall prefix every STRING cell starting with `=`, `+`, `-`, `@`, TAB or CR with an apostrophe.
  Pattern   : ubiquitous
  Trigger   : CSV export
  Rationale : Excel needs the BOM to detect UTF-8 Arabic; a leading formula character would otherwise be evaluated by a spreadsheet (formula injection)
  Source    : report/export/CsvReportWriter.java:21-34, :57-59, :80-88, :102-104; report/service/ReportService.java:157-159
  Status    : CONFIRMED (as built)

CUSTOM LOOKUP VALUES
| Lookup key | Added values | Source |
|---|---|---|
None — the module owns no MDL lookup. Its three value sets are Java enums: `ParamType` (STRING, INTEGER,
DECIMAL, DATE, DATETIME, BOOLEAN, LOOKUP), `ColumnType` (the same without LOOKUP) and
`ReportRunDomain.ExportFormat` (CSV, JSON) — report/ParamType.java:18-26; report/ColumnType.java:10-17;
report/domain/ReportRunDomain.java:19-38. A `LOOKUP` parameter consumes an MDL lookup type named by the
provider (`ReportParam.lookupKey`).

SCOPE EXCEPTIONS
| Excluded / Deferred | Statement | Activation trigger | Source |
|---|---|---|---|
| Persisted / editable report definitions, scheduling, charts, PDF/XLSX | not built; the engine is minimal and tabular | explicit future request | docs/steps/11-report.md Summary; ADR-REPORT-001 |
| Streaming export body | not built; bounded `byte[]` | explicit future request | docs/DEVIATIONS.md [11] "streams `text/csv`"; ADR-REPORT-003 |
| Totals in CSV | JSON only | — | docs/DEVIATIONS.md [11] "Export formats and CSV rules" |
| OpenAPI group `report` | not added (aggregate document only) | — | docs/DEVIATIONS.md [11] "Property and OpenAPI" |
| Tenant data export (tenant-maturity plan C.5) | not a report; own SPI and ZIP output | plan package C | docs/plans/tenant-maturity-plan.md C.5 |

RESOLVED DECISIONS
| # | Question | Answer | Confirmed by | Sources |
|---|---|---|---|---|
| 1 | Where do report definitions live? | in code (`ReportProvider` beans), collected by a fail-fast in-memory registry; no table | erp-core plan step 11 (fixed design), as built | ADR-REPORT-001 |
| 2 | How is a report secured? | one automatically registered action per report, checked in code against the path variable | step 11, as built | ADR-REPORT-002 |
| 3 | How is an export bounded and written? | cap + 1 probe, bounded `byte[]`, hand-written RFC 4180 CSV with BOM and formula guard | step 11, as built | ADR-REPORT-003 |
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 11
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : none — no report delta is planned for 1.3.0; the TENANT data export of `docs/plans/tenant-maturity-plan.md` C.5 does NOT use the report engine.
Statement      : This addendum records the deltas being implemented for 1.3.0; every row is verified against the code before the tag.

| # | Kind | Policy-level delta | Source |
|---|---|---|---|
| — | — | none | docs/plans/tenant-maturity-plan.md C.5 |
