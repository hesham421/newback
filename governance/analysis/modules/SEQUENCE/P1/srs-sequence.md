# SRS — سلاسل الترقيم / Sequence (SEQUENCE)
══════════════════════════════════════════════════════════════════
Module : SEQUENCE   Version : v1 (as-built baseline)   Profile : erp
Inputs : prd-sequence, module-registry-sequence, business-policies-sequence; the code at main @ 19b19a4 (erp-core 1.2.0 behaviour)
Counts : ENT 1 · REQ 20 · AC 20 · RULE 14 · SCR-REQ 1 · API 6 (+ 2 in-process) · XM 3 · ADR 3
══════════════════════════════════════════════════════════════════

Written from the code, not before it. Every REQ / AC / RULE names the code location it was read from;
the id → location index is `registry-srs-sequence.md`. Java paths are relative to
`erp-core/src/main/java/com/erp/`, migrations to `erp-core/src/main/resources/db/migration/core/`,
`file:line` at main @ 19b19a4. Test-case ids `TC-CORE-SEQ-NNN` are those of
`docs/test-api/core-test-plan.md`; JUnit names are those of `erp-core/src/test/java/com/erp/sequence/`.

# PART A — MODULE FOUNDATION

## A1 — Document information
| Item | Value |
|---|---|
| Module | SEQUENCE — سلاسل الترقيم / Sequence (package `com.erp.sequence`, registry module `SEQUENCE`) |
| Feature code | SEQUENCE |
| Version | v1 (as built in erp-core 1.2.0) |
| Date | 2026-10-07 |
| Status | AS-BUILT baseline |
| Prepared by | analysis lane, from the code |
| Decisions applied count | 3 (ADR-SEQUENCE-001, -002, -003) |

## A2 — Functional context

**In scope:** سلاسل ترقيم المستندات لكل مستأجر (`CORE_NUMBER_SERIES`)، نمط الترقيم ورموزه، سياسة
إعادة الترقيم (أبدًا / سنويًا / شهريًا) بصف لكل فترة، سحب الرقم التالي ذرّيًا ومعاينته، واجهة إدارة
السلاسل للموظفين (إنشاء، بحث، قراءة، تعديل، تفعيل، إلغاء تفعيل)، نسخ السلاسل للمستأجر الجديد، تدقيق
تغييرات التهيئة.

**Out of scope:** الترقيم بلا فجوات عبر المعاملات المتراجعة، حذف السلسلة أو تعديل عدّادها أو رمزها أو
سياستها عبر الواجهة، أبعاد الفرع أو نوع المستند داخل السلسلة، التخزين المؤقت —
[business-policies-sequence.md → SCOPE EXCEPTIONS; ADR-SEQUENCE-001, ADR-SEQUENCE-003].

**Module function (one paragraph):** وحدة SEQUENCE تمنح كل مستأجر سلاسل ترقيم مهيأة مسبقًا، وتسلّم
الوحدات والتطبيقات الرقم التالي منسّقًا ومتتابعًا بلا تكرار تحت التزامن، وتجعل تهيئة السلاسل قابلة
للإدارة من موظفي المستأجر المخوّلين فقط.

**Detailed description (workflow narrative, roles):** يهيّئ مدير المستأجر (أو سكربت `V1000+` للمنصة)
سلسلة برمز ونمط وسياسة إعادة؛ تنسخها الوحدة إلى كل مستأجر يُنشأ لاحقًا بعدّاد 1. عند إنشاء مستند،
تستدعي الوحدة المالكة `NumberSeriesApi.next(code)` فتُقفل مرساة السلسلة، ويُقفل صف الفترة الحالية أو
يُنشأ، ويُسحب العدّاد ويُلتزم به مستقلًا عن معاملة المستدعي. تغييرات البادئة والنمط والتفعيل تُطبَّق
على كل فترات الرمز وتُسجَّل في سجل التدقيق؛ حركة العدّاد لا تُسجَّل.

**Current situation:** built (erp-core plan step 09; audited since step 10's rebase onto 09).

**General notes:** no `erp.core.*` property belongs to the module; an application-provided
`java.time.Clock` bean, when present, decides the period and the date tokens (RULE-SEQUENCE-007).

## A3 — Entities and fields

### ENT-SEQUENCE-001 — سلسلة الترقيم (فترة) / NumberSeries
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| config (tenant-scoped; one row per code and period) | PRIVATE — no table references it | No — `code` + `periodKey` is the natural key per tenant | create, read, search, update (prefix, pattern), activate, deactivate; allocation moves `nextValue`; no delete | reached only through `NumberSeriesApi` (XM-SEQUENCE-003); FK `TENANT_ID` → `CORE_TENANT` (XM-SEQUENCE-001) | sequence/entity/NumberSeries.java:40-125; V14__sequence_and_settings.sql:31-61 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| id | number | yes (system) | `SEQ_CORE_NUMBER_SERIES`, `allocationSize = 1` | primary key; the lowest id of a code is the series' anchor row (RULE-SEQUENCE-004) | المعرف الفريد | Unique identifier |
| tenantId | number | yes (system) | `TenantContext` through `AuditableEntity` (`@TenantId`) | never a client field; not updatable | — | — |
| code | text (≤ 50) | yes | request `^[A-Za-z0-9_]+$`, stored trimmed upper case; unique per (tenant, period) (RULE-SEQUENCE-014) | immutable (`updatable = false`) | رمز السلسلة | Series code |
| prefix | text (≤ 20) | no | — | value of the `{PREFIX}` token; null renders empty | البادئة | Prefix |
| pattern | text (≤ 100) | yes | tokens `{PREFIX}` `{YYYY}` `{YY}` `{MM}` `{SEQ:n}` `{TENANT}` (RULE-SEQUENCE-001, -002) | default `{PREFIX}-{YYYY}-{SEQ:6}` | نمط الترقيم | Pattern |
| resetPolicy | code (≤ 10) | yes | `NEVER` \| `YEARLY` \| `MONTHLY` (A6) | default `YEARLY`; immutable | سياسة إعادة الترقيم | Reset policy |
| periodKey | text (≤ 7) | yes (system) | `''` (NEVER), `YYYY` (YEARLY), `YYYY-MM` (MONTHLY) of the creation date / the allocation date | immutable; computed, never sent by the client | الفترة | Period |
| nextValue | number (≥ 1) | yes | create: optional starting value, default 1; afterwards moved only by allocation | never editable over HTTP (RULE-SEQUENCE-009) | القيمة التالية | Next value |
| isActive | flag | yes | true / false | default true; `activate` / `deactivate` apply to every row of the code | حالة التفعيل | Active status |
| createdBy, createdAt, updatedBy, updatedAt | system | createdAt yes | — | `GlobalAuditableEntity` | تاريخ الإنشاء / أنشئ بواسطة / تاريخ التحديث / حُدّث بواسطة | Created timestamp / Created by / Updated timestamp / Updated by |
| version | system | yes | optimistic lock | `GlobalAuditableEntity` `@Version` | — | — |
Source: sequence/entity/NumberSeries.java:51-92; sequence/dto/NumberSeriesCreateRequest.java:22-42;
sequence/dto/NumberSeriesResponse.java:18-57; common/domain/AuditableEntity.java:35-36;
common/domain/GlobalAuditableEntity.java:34-52. Labels: the `@Schema` descriptions of the DTOs.

## A4 — Functional requirements (EARS) and acceptance criteria

### REQ-SEQUENCE-001 — إنشاء سلسلة / Create a series
Pattern    : event
Statement  : When an authorised administrator submits a valid create request, the system shall store the series' first period row in the caller's tenant with the code upper-cased, the defaults applied (reset policy `YEARLY`, pattern `{PREFIX}-{YYYY}-{SEQ:6}`, `nextValue` 1), the period key of today under the policy, active, and return it.
Traces     : US-SEQUENCE-001
Entities   : ENT-SEQUENCE-001
Rationale  : POL-SEQUENCE-003, POL-SEQUENCE-007
Source     : sequence/service/NumberSeriesService.java:63-79; sequence/mapper/NumberSeriesMapper.java:15-27; sequence/entity/NumberSeries.java:94-102
Priority   : HIGH
#### AC-SEQUENCE-001 — [REQ-SEQUENCE-001]
Given a PLATFORM administrator holding `PERM_SEQUENCE_SERIES_MANAGE`
When they post `{"code":"tc_inv","prefix":"INV"}` to `POST /api/v1/sequence/series`
Then the system answers 201 with `code = "TC_INV"`, `pattern = "{PREFIX}-{YYYY}-{SEQ:6}"`, `resetPolicy = "YEARLY"`, `periodKey` = the current year, `nextValue = 1`, `isActive = true` (TC-CORE-SEQ-001; `NumberSeriesApiIntegrationTest.create_201_withDefaults_then_duplicate_409_and_invalidPattern_400`)

### REQ-SEQUENCE-002 — رفض نمط غير صالح / Reject an invalid pattern
Pattern    : unwanted
Statement  : If a create or update request carries a pattern with an unknown token, an unmatched brace, no `{SEQ:n}`, more than one `{SEQ:n}`, or `n` outside 1..18, then the system shall reject it and store nothing.
Traces     : US-SEQUENCE-001, US-SEQUENCE-002
Entities   : ENT-SEQUENCE-001
Rationale  : POL-SEQUENCE-001
Source     : sequence/domain/NumberPattern.java:43-108, :159-162; sequence/domain/NumberSeriesDomain.java:40, :53
Priority   : HIGH
#### AC-SEQUENCE-002 — [REQ-SEQUENCE-002]
Given create requests with `pattern` `{DAY}-{SEQ:3}`, `{PREFIX}`, `{SEQ:3}-{SEQ:3}`, `{SEQ:0}`, `{SEQ:19}` and `{PREFIX-{SEQ:3}` (reset policy `NEVER`)
When each is posted
Then each answers 400 `SEQUENCE_PATTERN_INVALID` and no series is created (TC-CORE-SEQ-004, -006; `NumberSeriesDomainTest`)

### REQ-SEQUENCE-003 — رفض نمط يكرّر الأرقام تحت سياسة الإعادة / Reject a pattern that repeats under the reset policy
Pattern    : unwanted
Statement  : If the series resets `YEARLY` and the pattern has neither `{YYYY}` nor `{YY}`, or resets `MONTHLY` and the pattern lacks a year token or `{MM}`, then the system shall reject the pattern on create and on update.
Traces     : US-SEQUENCE-001, US-SEQUENCE-002
Entities   : ENT-SEQUENCE-001
Rationale  : POL-SEQUENCE-002
Source     : sequence/domain/NumberPattern.java:115-125; sequence/domain/NumberSeriesDomain.java:41, :52-54
Priority   : HIGH
#### AC-SEQUENCE-003 — [REQ-SEQUENCE-003]
Given `{"code":"TC_BAD2","pattern":"{PREFIX}-{SEQ:3}","resetPolicy":"YEARLY"}` and `{"code":"TC_BAD3","pattern":"{PREFIX}-{YYYY}-{SEQ:3}","resetPolicy":"MONTHLY"}`, and later a `PUT` of `{"pattern":"{PREFIX}-{SEQ:5}"}` on a YEARLY series
When each is sent
Then each answers 400 `SEQUENCE_PATTERN_INVALID` (TC-CORE-SEQ-005, -010)

### REQ-SEQUENCE-004 — رفض رمز مكرر / Reject a duplicate code
Pattern    : unwanted
Statement  : If a create request carries a code that a series of the caller's tenant already holds (compared upper-cased), then the system shall reject it.
Traces     : US-SEQUENCE-001
Entities   : ENT-SEQUENCE-001
Rationale  : POL-SEQUENCE-003 (one series per code)
Source     : sequence/domain/NumberSeriesDomain.java:39; sequence/repository/NumberSeriesRepository.java:25; sequence/service/NumberSeriesService.java:68, :72
Priority   : HIGH
#### AC-SEQUENCE-004 — [REQ-SEQUENCE-004]
Given a series `TC_INV` in the tenant
When `{"code":"TC_INV"}` is posted again
Then the system answers 409 `NUMBER_SERIES_CODE_DUPLICATE` (TC-CORE-SEQ-003)

### REQ-SEQUENCE-005 — رفض طلب إنشاء غير صالح شكلًا / Reject a malformed create request
Pattern    : unwanted
Statement  : If a create request lacks `code`, or `code` does not match `^[A-Za-z0-9_]+$` or exceeds 50 characters, or `prefix` exceeds 20, or `pattern` exceeds 100, or `nextValue` is below 1, then the system shall reject it naming the offending fields.
Traces     : US-SEQUENCE-001
Entities   : ENT-SEQUENCE-001
Rationale  : input validation before any rule runs
Source     : sequence/dto/NumberSeriesCreateRequest.java:22-42; sequence/controller/NumberSeriesController.java:40
Priority   : MEDIUM
#### AC-SEQUENCE-005 — [REQ-SEQUENCE-005]
Given `{"code":"has space"}`
When it is posted
Then the system answers 400 `VALIDATION_ERROR` with `fieldErrors[0].field = code` (TC-CORE-SEQ-007)

### REQ-SEQUENCE-006 — قراءة صف فترة / Read a period row
Pattern    : event
Statement  : When an authorised reader asks for a period row by id, the system shall return it; if no row has that id in the caller's tenant, the system shall answer not found.
Traces     : US-SEQUENCE-003
Entities   : ENT-SEQUENCE-001
Rationale  : POL-SEQUENCE-007, POL-SEQUENCE-009
Source     : sequence/service/NumberSeriesService.java:81-86, :143-146
Priority   : MEDIUM
#### AC-SEQUENCE-006 — [REQ-SEQUENCE-006]
Given a NEVER series created with `{"code":"TC_NEV","prefix":"P","pattern":"{PREFIX}-{SEQ:4}","resetPolicy":"NEVER"}` and the id 987654321
When `GET /api/v1/sequence/series/{id}` is called with each
Then the first answers 200 with `resetPolicy = "NEVER"` and `periodKey = ""`, the second 404 `NUMBER_SERIES_NOT_FOUND` (TC-CORE-SEQ-008; `NumberSeriesApiIntegrationTest.getById_200_and_404`)

### REQ-SEQUENCE-007 — البحث في صفوف الفترات / Search period rows
Pattern    : event
Statement  : When an authorised reader submits a search, the system shall return the matching page of the caller's tenant, filtering only on `id`, `code`, `periodKey`, `isActive`, `createdAt`, `updatedAt` and sorting only on `id`, `code`, `periodKey`, `createdAt`, `updatedAt`; an unsupported filter field is rejected naming the field.
Traces     : US-SEQUENCE-003
Entities   : ENT-SEQUENCE-001
Rationale  : shared search contract (`BaseSearchContractRequest`, `SpecBuilder`, `PageableBuilder` — page 0, size 20, max 200)
Source     : sequence/service/NumberSeriesService.java:51-57, :88-99; sequence/dto/NumberSeriesSearchRequest.java:19; common/search/SpecBuilder.java:58-62
Priority   : MEDIUM
#### AC-SEQUENCE-007 — [REQ-SEQUENCE-007]
Given series in the tenant
When `{"filters":[{"field":"code","operator":"EQUALS","value":"TC_NEV"}]}` and then `{"filters":[{"field":"tenantId","operator":"EQUALS","value":"2"}]}` are posted to `POST /api/v1/sequence/series/search`
Then the first answers 200 with exactly `TC_NEV`, the second 400 `VALIDATION_ERROR` with `fieldErrors[0].field = tenantId` (TC-CORE-SEQ-009; `NumberSeriesApiIntegrationTest.search_200_byCode_and_400_onAnUnsupportedFilter`)

### REQ-SEQUENCE-008 — تعديل البادئة والنمط لكل فترات الرمز / Update prefix and pattern across the code's periods
Pattern    : event
Statement  : When an authorised administrator updates any period row of a code with a prefix and a valid pattern, the system shall apply both to every period row of that code and return the addressed row; the code, the reset policy, the period key and the counter are not changed, whatever the body carries.
Traces     : US-SEQUENCE-002
Entities   : ENT-SEQUENCE-001
Rationale  : POL-SEQUENCE-003, POL-SEQUENCE-006
Source     : sequence/service/NumberSeriesService.java:101-115; sequence/mapper/NumberSeriesMapper.java:45-52; sequence/dto/NumberSeriesUpdateRequest.java:20-30; sequence/repository/NumberSeriesRepository.java:45
Priority   : HIGH
#### AC-SEQUENCE-008 — [REQ-SEQUENCE-008]
Given series `TC_INV` (YEARLY) and `TC_NEV` (NEVER, `nextValue` 1)
When `{"prefix":"NEW","pattern":"{PREFIX}/{YY}/{SEQ:5}"}` is put on `TC_INV`'s row, and `{"prefix":"P2","pattern":"{PREFIX}-{SEQ:4}","code":"HIJACK","resetPolicy":"YEARLY","nextValue":999}` on `TC_NEV`'s row, and `{"pattern":"{SEQ:3}"}` on id 987654321
Then the first answers 200 with `prefix = "NEW"` and the new pattern; the second 200 with `prefix = "P2"` while `code`, `resetPolicy` and `nextValue` are unchanged; the third 404 `NUMBER_SERIES_NOT_FOUND` (TC-CORE-SEQ-010, -011; `NumberSeriesApiIntegrationTest.update_200_appliesToTheCode_and_400_onAnInvalidPattern`)

### REQ-SEQUENCE-009 — إلغاء تفعيل سلسلة وإعادة تفعيلها / Deactivate and re-activate a series
Pattern    : event
Statement  : When an authorised administrator deactivates or activates any period row of a code, the system shall set `isActive` on every period row of that code and return the addressed row; an unknown id answers not found.
Traces     : US-SEQUENCE-002
Entities   : ENT-SEQUENCE-001
Rationale  : POL-SEQUENCE-003, POL-SEQUENCE-006 — the only way to stop a series
Source     : sequence/service/NumberSeriesService.java:117-141; sequence/entity/NumberSeries.java:118-124
Priority   : HIGH
#### AC-SEQUENCE-009 — [REQ-SEQUENCE-009]
Given series `TC_NEV`
When `PUT …/{id}/deactivate`, then `PUT …/{id}/activate`, then `PUT /api/v1/sequence/series/987654321/deactivate` are called
Then the answers are 200 `isActive = false`, 200 `isActive = true`, 404 `NUMBER_SERIES_NOT_FOUND` (TC-CORE-SEQ-012; `NumberSeriesApiIntegrationTest.deactivate_then_activate_200_and_404`)

### REQ-SEQUENCE-010 — سحب الرقم التالي ذرّيًا / Allocate the next number atomically
Pattern    : event
Statement  : When a module calls `NumberSeriesApi.next(code)`, the system shall, in a transaction of its own (`REQUIRES_NEW`) under the caller's tenant, lock the code's anchor row, lock the current period's row, take its `nextValue`, move the counter on, commit, and return the number rendered from the pattern; two concurrent allocations shall never return the same value and the values of one period shall be consecutive.
Traces     : US-SEQUENCE-004
Entities   : ENT-SEQUENCE-001
Rationale  : POL-SEQUENCE-004
Source     : sequence/service/NumberAllocationService.java:55-78; sequence/repository/NumberSeriesRepository.java:31-36; sequence/entity/NumberSeries.java:112-116
Priority   : HIGH
#### AC-SEQUENCE-010 — [REQ-SEQUENCE-010]
Given a fresh series `T` with pattern `T-{SEQ:4}` and policy `NEVER`
When 50 threads released together each call `next("T")`
Then the results are exactly `T-0001` … `T-0050`, `NEXT_VALUE` is 51 and one row exists (`NumberSeriesConcurrencyIntegrationTest.fiftyConcurrentCalls_produceFiftyUniqueConsecutiveNumbers`); a caller whose own transaction rolls back after the call does not get its number back (docs/steps/09-report.md Summary)

### REQ-SEQUENCE-011 — بدء فترة جديدة من 1 / Start a new period at 1
Pattern    : event
Statement  : When an allocation or a preview falls in a period that has no row yet (a `YEARLY` or `MONTHLY` series after a reset), the system shall create that period's row from the anchor's configuration with `nextValue` 1 (allocation) or treat it as 1 (preview), and leave the previous period's row untouched.
Traces     : US-SEQUENCE-004, US-SEQUENCE-005
Entities   : ENT-SEQUENCE-001
Rationale  : POL-SEQUENCE-003
Source     : sequence/service/NumberAllocationService.java:67-73, :91-95; sequence/mapper/NumberSeriesMapper.java:30-43; sequence/domain/ResetPolicy.java:19-25
Priority   : HIGH
#### AC-SEQUENCE-011 — [REQ-SEQUENCE-011]
Given a YEARLY series whose row of last year holds `nextValue` 124
When `next` is called this year
Then a new row with this year's period key exists with the same code, prefix, pattern, policy and activation, the number ends in `000001`, and last year's row still holds 124 (`NumberSeriesIntegrationTest.yearlyReset_createsANewPeriodRow_startingAtOne_andLeavesThePreviousYearUntouched`)

### REQ-SEQUENCE-012 — رفض رمز غير مهيأ أو غير مفعّل / Refuse an unconfigured or inactive code
Pattern    : unwanted
Statement  : If `next` or `preview` names a code that has no series in the caller's tenant, or whose series is inactive, then the system shall fail with `SEQUENCE_NOT_CONFIGURED` and create nothing.
Traces     : US-SEQUENCE-004, US-SEQUENCE-005
Entities   : ENT-SEQUENCE-001
Rationale  : POL-SEQUENCE-005
Source     : sequence/service/NumberAllocationService.java:62-65, :86-89, :107-109; sequence/domain/NumberSeriesDomain.java:57-61
Priority   : HIGH
#### AC-SEQUENCE-012 — [REQ-SEQUENCE-012]
Given no series `NOPE` and a deactivated series `OFF`
When `next("NOPE")`, `preview("NOPE")`, `next("OFF")` are called
Then each fails with `SEQUENCE_NOT_CONFIGURED` (422 `BUSINESS_RULE_VIOLATION` over HTTP) and no row is created (`NumberSeriesIntegrationTest.unknownOrInactiveCode_isSequenceNotConfigured_andNothingIsCreated`)

### REQ-SEQUENCE-013 — معاينة بلا استهلاك / Preview without consuming
Pattern    : event
Statement  : When a module calls `NumberSeriesApi.preview(code)`, the system shall return the number `next` would return now, in a read-only transaction, without locking or writing; codes are matched case-insensitively (trimmed, upper-cased).
Traces     : US-SEQUENCE-005
Entities   : ENT-SEQUENCE-001
Rationale  : POL-SEQUENCE-005
Source     : sequence/service/NumberAllocationService.java:80-97, :111-113; sequence/repository/NumberSeriesRepository.java:39-42
Priority   : LOW
#### AC-SEQUENCE-013 — [REQ-SEQUENCE-013]
Given a series `INV` with `nextValue` 7
When `preview("inv")` is called twice and then `next("INV")`
Then both previews and the allocation return the same number and `nextValue` becomes 8 (`NumberSeriesIntegrationTest.preview_showsTheNextNumber_withoutConsumingIt_andCodesAreCaseInsensitive`)

### REQ-SEQUENCE-014 — تنسيق الرقم من النمط / Render the number from the pattern
Pattern    : ubiquitous
Statement  : The system shall render `{PREFIX}` as the series prefix (empty when null), `{YYYY}` as the four-digit year, `{YY}` as its last two digits, `{MM}` as the two-digit month, `{SEQ:n}` as the value zero-padded to n digits and never cut when longer, `{TENANT}` as the current tenant's code read through `TenantLookupApi` (empty when unknown), and every other character literally.
Traces     : US-SEQUENCE-004, US-SEQUENCE-005
Entities   : ENT-SEQUENCE-001
Rationale  : POL-SEQUENCE-001
Source     : sequence/domain/NumberPattern.java:133-153; sequence/service/NumberAllocationService.java:99-101; sequence/domain/NumberSeriesDomain.java:68-76
Priority   : HIGH
#### AC-SEQUENCE-014 — [REQ-SEQUENCE-014]
Given tenant `ACME` and a series with pattern `{TENANT}/{PREFIX}-{YY}{MM}-{SEQ:3}`, prefix `INV`, `nextValue` 1234, on 2026-10-07
When `next` is called
Then the number is `ACME/INV-2610-1234` (the sequence is not cut to 3 digits) (`NumberSeriesIntegrationTest.tenantToken_rendersTheTenantCode`; `NumberSeriesDomainTest.render_table`)

### REQ-SEQUENCE-015 — تاريخ الفترة من ساعة التطبيق / Period date from the application clock
Pattern    : optional
Statement  : Where the application defines a `java.time.Clock` bean, the system shall take the period and the date tokens from it; otherwise it shall use the JVM's default time zone.
Traces     : US-SEQUENCE-001, US-SEQUENCE-004
Entities   : ENT-SEQUENCE-001
Rationale  : tests and multi-zone deployments need a controllable date; no property was introduced (docs/DEVIATIONS.md [09])
Source     : sequence/service/NumberAllocationService.java:53, :103-105; sequence/service/NumberSeriesService.java:61, :148-150
Priority   : LOW
#### AC-SEQUENCE-015 — [REQ-SEQUENCE-015]
Given a `Clock` bean fixed at 2027-01-01
When a YEARLY series is created and a number drawn
Then the period key and `{YYYY}` are `2027`

### REQ-SEQUENCE-016 — عزل السلاسل بين المستأجرين / Tenant isolation of series
Pattern    : ubiquitous
Statement  : The system shall confine every read, write, lock and allocation to the caller's tenant: a row of another tenant answers not found, and the same code in two tenants counts independently from 1.
Traces     : US-SEQUENCE-003, US-SEQUENCE-004
Entities   : ENT-SEQUENCE-001
Rationale  : POL-SEQUENCE-007
Source     : sequence/entity/NumberSeries.java:35-36, :48; sequence/repository/NumberSeriesRepository.java:13-15; common/domain/AuditableEntity.java:35-36
Priority   : HIGH
#### AC-SEQUENCE-016 — [REQ-SEQUENCE-016]
Given PLATFORM's series row and tenant A's administrator
When A reads PLATFORM's row by id, creates `TC_OWN` in A, and PLATFORM reads A's row by id
Then the answers are 404, 201, 404 (TC-CORE-SEQ-013; `NumberSeriesApiIntegrationTest.anotherTenantsAdmin_managesItsOwnSeries_andNeverSeesPlatformRows`); tenants A and B each start the same code at 1 (`NumberSeriesIntegrationTest.tenantsAandB_eachStartAtOne_forTheSameCode`)

### REQ-SEQUENCE-017 — نسخ السلاسل إلى المستأجر الجديد / Copy series into a new tenant
Pattern    : event
Statement  : When a tenant is provisioned, the system shall insert, for each code of the source tenant (PLATFORM), a copy of that code's anchor row (code, prefix, pattern, reset policy, period key, activation) with `NEXT_VALUE` 1, inside the provisioning transaction, after SEC, MDL and NOTIF (order 40).
Traces     : US-SEQUENCE-006
Entities   : ENT-SEQUENCE-001
Rationale  : POL-SEQUENCE-008
Source     : sequence/tenant/SequenceTenantProvisioningContributor.java:27-44
Priority   : HIGH
Note       : XM-SEQUENCE-002 (`TenantProvisioningContributor`). Explicit JDBC naming `TENANT_ID` in the insert and both predicates (RULE-TENANT-008).
#### AC-SEQUENCE-017 — [REQ-SEQUENCE-017]
Given PLATFORM holds `TC_INV` with `nextValue` 5
When tenant A is provisioned and searches `code = TC_INV`
Then exactly one row exists in A with a new id, `nextValue = 1` and PLATFORM's pattern (TC-CORE-SEQ-002; `NumberSeriesIntegrationTest.tenantProvisioning_copiesTheSeriesDefinitions_withTheCounterBackAtOne`)

### REQ-SEQUENCE-018 — التفويض على واجهة الإدارة / Authorisation of the admin API
Pattern    : ubiquitous
Statement  : The system shall require `PERM_SEQUENCE_SERIES_VIEW` for `GET /{id}` and `POST /search`, and `PERM_SEQUENCE_SERIES_MANAGE` for create, update, activate and deactivate; a request without a token is refused as unauthenticated and a holder of neither authority as forbidden.
Traces     : US-SEQUENCE-001, US-SEQUENCE-002, US-SEQUENCE-003
Entities   : ENT-SEQUENCE-001
Rationale  : POL-SEQUENCE-009; RULE-SEC-007 (VIEW is the screen's gateway)
Source     : sequence/service/NumberSeriesService.java:64, :82, :89, :102, :118, :131; sequence/permission/SequencePermissions.java:20-46
Priority   : HIGH
#### AC-SEQUENCE-018 — [REQ-SEQUENCE-018]
Given no token, a token without sequence grants, and a token holding only `PERM_SEQUENCE_SERIES_VIEW`
When each posts `{}` to `/search` and the last also posts a create
Then the answers are 401 `SEC-401-INVALID-CREDENTIALS`, 403 `ACCESS_DENIED`, 200, and 403 `ACCESS_DENIED` for the create (TC-CORE-SEQ-014; `NumberSeriesApiIntegrationTest.withoutAToken_401`)

### REQ-SEQUENCE-019 — تدقيق التهيئة دون العدّاد / Audit the configuration, not the counter
Pattern    : event
Statement  : When a series row is created, updated or (de)activated, the system shall record the change field by field in the platform audit log under entity type `CORE_NUMBER_SERIES`; a change of `nextValue` alone (an allocation) shall write no audit row.
Traces     : US-SEQUENCE-002, US-SEQUENCE-004
Entities   : ENT-SEQUENCE-001
Rationale  : POL-SEQUENCE-010
Source     : sequence/entity/NumberSeries.java:41; audit/listener/AuditedEntityListener.java:78-100
Priority   : MEDIUM
#### AC-SEQUENCE-019 — [REQ-SEQUENCE-019]
Given a series created with prefix `INV`, then updated to prefix `NEW`, then numbered twice
When the audit log is queried for `entityType = CORE_NUMBER_SERIES` and the row's id
Then it holds one `CREATE` row and one `UPDATE` row with `prefix` INV → NEW (and `pattern`), and no row names `nextValue` (TC-CORE-AUDIT-013; `AuditedEntitiesCoverageIntegrationTest.sequenceNumberSeries_configIsAudited_allocationIsNot`)

### REQ-SEQUENCE-020 — عقد واجهة الترقيم عبر الوحدات / Cross-module number API contract
Pattern    : optional
Statement  : Where another module or an application holds a series code, the system shall offer `NumberSeriesApi` with `String next(String code)` and `String preview(String code)` as its only public surface, returning the formatted number and nothing else, without an authority check of its own.
Traces     : US-SEQUENCE-004, US-SEQUENCE-005
Entities   : ENT-SEQUENCE-001
Rationale  : POL-SEQUENCE-009; ArchUnit bounds the module to `com.erp.sequence.crossmodule`
Source     : sequence/crossmodule/NumberSeriesApi.java:12-23; sequence/crossmodule/NumberSeriesApiImpl.java:13-28; erp-core/src/test/java/com/erp/architecture/CrossModuleBoundaryArchTest.java:61-62
Priority   : HIGH
Note       : XM-SEQUENCE-003. No core consumer; applications inject it (docs/CONSUMING.md §3).
#### AC-SEQUENCE-020 — [REQ-SEQUENCE-020]
Given a bean injecting `com.erp.sequence.crossmodule.NumberSeriesApi`
When it calls `next` inside its own transaction
Then it receives the formatted number, the allocation is committed on its own connection (a second pooled connection for the call), and no other class of `com.erp.sequence` is reachable from outside the module (ArchUnit `CrossModuleBoundaryArchTest`)

### A4 — Error codes (HTTP status as the code has it)
| Code | HTTP | Raised when (trigger) | Code location | Message (en / ar) |
|---|---|---|---|---|
| `SEQUENCE_NOT_CONFIGURED` | 422 (`Status.BUSINESS_RULE_VIOLATION`) | `next` / `preview` on a code with no series in the tenant, or an inactive series — never from the admin API | sequence/service/NumberAllocationService.java:107-109; sequence/domain/NumberSeriesDomain.java:57-61; sequence/exception/SequenceErrorCodes.java:14 | "No active number series is configured for code ''{0}''" / "لا توجد سلسلة ترقيم مفعّلة للرمز ''{0}''" |
| `SEQUENCE_PATTERN_INVALID` | 400 (`Status.VALIDATION_ERROR`) | create or update with an unknown token, unmatched brace, not exactly one `{SEQ:n}`, `n` outside 1..18, or a pattern that repeats under the reset policy | sequence/domain/NumberPattern.java:159-162 (via `parse`, `assertDistinctUnder`); sequence/exception/SequenceErrorCodes.java:18 | "Invalid number pattern ''{0}'': use the tokens '{PREFIX}' '{YYYY}' '{YY}' '{MM}' '{SEQ:n}' '{TENANT}' with exactly one '{SEQ:n}', and include the year (and the month) when the series resets yearly (monthly)" / "نمط الترقيم ''{0}'' غير صالح: استخدم الرموز '{PREFIX}' '{YYYY}' '{YY}' '{MM}' '{SEQ:n}' '{TENANT}' مع '{SEQ:n}' واحد فقط، وأدرج السنة (والشهر) إذا كانت السلسلة تُعاد سنويًا (شهريًا)" |
| `NUMBER_SERIES_NOT_FOUND` | 404 (`Status.NOT_FOUND`) | get, update, activate, deactivate with an id that is not a row of the caller's tenant | sequence/service/NumberSeriesService.java:143-146; sequence/exception/SequenceErrorCodes.java:21 | "Number series with ID {0} was not found" / "لم يتم العثور على سلسلة الترقيم ذات المعرف {0}" |
| `NUMBER_SERIES_CODE_DUPLICATE` | 409 (`Status.ALREADY_EXISTS`) | create with a code already held in the tenant | sequence/domain/NumberSeriesDomain.java:39 via common/domain/DomainRules.java:17; sequence/exception/SequenceErrorCodes.java:24 | "A number series with code ''{0}'' already exists" / "توجد سلسلة ترقيم بالرمز ''{0}'' مسبقًا" |
`Status` → HTTP: common/domain/status/Status.java:8-21. Messages: `erp-core/src/main/resources/i18n/messages.properties`
lines 166–169, `messages_ar.properties` lines 163–166. Shared codes the endpoints also answer:
`VALIDATION_ERROR` 400 (bean validation, unsupported filter field via `UNSUPPORTED_FILTER_FIELD` detail,
page overflow), `ACCESS_DENIED` 403, `SEC-401-INVALID-CREDENTIALS` 401, `CONCURRENT_MODIFICATION` 409,
`DATA_INTEGRITY_VIOLATION` 409, `INTERNAL_ERROR` 500; `TENANT_CONTEXT_MISSING` 500 from `next`/`preview`
called without a tenant (docs/api-docs/sequence/index.md:86-97).

## A5 — Business rules

### RULE-SEQUENCE-001 — قواعد نمط الترقيم / Pattern grammar
Scope      : ENT-SEQUENCE-001.pattern
Trigger    : on create; on update (pattern)
Statement  : The system shall accept a pattern only if every `{…}` is one of `{PREFIX}`, `{YYYY}`, `{YY}`, `{MM}`, `{SEQ:n}`, `{TENANT}` (case-sensitive), braces are balanced and not nested, and exactly one `{SEQ:n}` with 1 ≤ n ≤ 18 is present; everything outside braces is literal text; a null or blank pattern is invalid.
Data source: the request's `pattern`
Message    : `SEQUENCE_PATTERN_INVALID` (A4)
Traces     : REQ-SEQUENCE-002
Source     : sequence/domain/NumberPattern.java:27-30, :43-108

### RULE-SEQUENCE-002 — تميّز الأرقام تحت سياسة الإعادة / Distinct numbers under the reset policy
Scope      : ENT-SEQUENCE-001.pattern × resetPolicy
Trigger    : on create; on update (pattern, checked against the immutable policy)
Statement  : The system shall accept a pattern under `NEVER` always, under `YEARLY` only if it uses `{YYYY}` or `{YY}`, and under `MONTHLY` only if it uses a year token and `{MM}`.
Data source: the parsed token set
Message    : `SEQUENCE_PATTERN_INVALID` (A4)
Traces     : REQ-SEQUENCE-003
Source     : sequence/domain/NumberPattern.java:110-125; sequence/domain/NumberSeriesDomain.java:41, :52-54

### RULE-SEQUENCE-003 — قواعد التنسيق / Rendering rules
Scope      : ENT-SEQUENCE-001
Trigger    : on allocation; on preview
Statement  : The system shall zero-pad `{SEQ:n}` to n digits and never truncate a longer value; shall render `{PREFIX}` and `{TENANT}` as empty strings when the prefix is null or the tenant code is unknown; shall format `{YYYY}` as `%04d`, `{YY}` as `%02d` of year mod 100 and `{MM}` as `%02d`.
Data source: ENT-SEQUENCE-001.prefix; the allocation date; `TenantLookupApi.codeOf(TenantContext.require())` when `{TENANT}` is used
Message    : —
Traces     : REQ-SEQUENCE-014
Source     : sequence/domain/NumberPattern.java:132-153; sequence/service/NumberAllocationService.java:99-101

### RULE-SEQUENCE-004 — صف لكل فترة ومرساة لكل رمز / One row per period, one anchor per code
Scope      : ENT-SEQUENCE-001
Trigger    : on every read, write and allocation
Statement  : The system shall keep one row per (tenant, code, period key) (`UQ_CORE_NUMBER_SERIES_CODE_PERIOD`); the row with the lowest id of a code is the series' anchor; the configuration fields (`prefix`, `pattern`, `resetPolicy`, `isActive`) shall be identical on every row of a code; update, activate and deactivate address any row of a code and apply to all of them.
Data source: ENT-SEQUENCE-001 rows of the code (`findAllByCodeOrderByIdAsc`)
Message    : —
Traces     : REQ-SEQUENCE-008, REQ-SEQUENCE-009, REQ-SEQUENCE-011
Source     : V14__sequence_and_settings.sql:57; sequence/entity/NumberSeries.java:28-38, :42-46; sequence/service/NumberSeriesService.java:109-111, :123-125, :136-138; sequence/repository/NumberSeriesRepository.java:45

### RULE-SEQUENCE-005 — بروتوكول السحب / Allocation protocol
Scope      : ENT-SEQUENCE-001.nextValue
Trigger    : `NumberSeriesApi.next`
Statement  : The system shall run `next` in a `REQUIRES_NEW` transaction; lock the anchor row with `PESSIMISTIC_WRITE` (`findFirstByCodeOrderByIdAsc`), then the current period's row with `PESSIMISTIC_WRITE` (`findByCodeAndPeriodKey`), creating it at 1 when absent; take `nextValue` and increment it; commit before returning. Values of one period are consecutive; a caller whose own transaction rolls back leaves a gap; a caller inside a transaction holds two connections for the duration of the call.
Data source: ENT-SEQUENCE-001 (anchor and period rows)
Message    : —
Traces     : REQ-SEQUENCE-010, REQ-SEQUENCE-011
Source     : sequence/service/NumberAllocationService.java:23-32, :55-78; sequence/repository/NumberSeriesRepository.java:27-36; sequence/entity/NumberSeries.java:112-116; ADR-SEQUENCE-001

### RULE-SEQUENCE-006 — المعاينة بلا قفل ولا كتابة / Preview locks and writes nothing
Scope      : ENT-SEQUENCE-001
Trigger    : `NumberSeriesApi.preview`
Statement  : The system shall read the anchor and the current period's row without a lock in a read-only transaction, treat a missing period row as `nextValue` 1, and write nothing.
Data source: ENT-SEQUENCE-001
Message    : `SEQUENCE_NOT_CONFIGURED` when the code is unknown or inactive
Traces     : REQ-SEQUENCE-013
Source     : sequence/service/NumberAllocationService.java:80-97; sequence/repository/NumberSeriesRepository.java:38-42

### RULE-SEQUENCE-007 — مصدر التاريخ / Date source
Scope      : ENT-SEQUENCE-001.periodKey; the date tokens
Trigger    : on create; on allocation; on preview
Statement  : The system shall take "today" from the application's `java.time.Clock` bean when one exists, otherwise from `Clock.systemDefaultZone()`.
Data source: `ObjectProvider<Clock>`
Message    : —
Traces     : REQ-SEQUENCE-015
Source     : sequence/service/NumberAllocationService.java:53, :103-105; sequence/service/NumberSeriesService.java:61, :148-150

### RULE-SEQUENCE-008 — السلسلة غير المفعّلة غير مهيأة / An inactive series counts as not configured
Scope      : ENT-SEQUENCE-001.isActive
Trigger    : `next`; `preview`
Statement  : The system shall refuse to draw or preview a number from a series whose anchor row is inactive, with the same code as for an unknown series.
Data source: ENT-SEQUENCE-001.isActive (anchor row)
Message    : `SEQUENCE_NOT_CONFIGURED` (A4)
Traces     : REQ-SEQUENCE-012
Source     : sequence/domain/NumberSeriesDomain.java:56-61; sequence/service/NumberAllocationService.java:64-65, :88-89

### RULE-SEQUENCE-009 — العدّاد لا يُحرَّر ولا حذف / No counter edits, no delete
Scope      : ENT-SEQUENCE-001
Trigger    : admin API
Statement  : The system shall expose no operation that edits `nextValue`, `code`, `resetPolicy` or `periodKey` after creation (the update body carries only `prefix` and `pattern`; unknown fields are ignored), and no delete; a series is stopped by `deactivate`.
Data source: —
Message    : —
Traces     : REQ-SEQUENCE-008, REQ-SEQUENCE-009
Source     : sequence/dto/NumberSeriesUpdateRequest.java:11-13, :20-30; sequence/mapper/NumberSeriesMapper.java:45-52; sequence/entity/NumberSeries.java:61, :76, :81 (`updatable = false`); sequence/controller/NumberSeriesController.java:24-28; ADR-SEQUENCE-003

### RULE-SEQUENCE-010 — القيم الافتراضية والتطبيع / Defaults and normalisation
Scope      : ENT-SEQUENCE-001
Trigger    : on create
Statement  : The system shall default `resetPolicy` to `YEARLY`, `pattern` to `{PREFIX}-{YYYY}-{SEQ:6}`, `nextValue` to 1 and `isActive` to true; shall set `periodKey` to today's period under the policy; and shall store `code` trimmed and upper-cased (`@PrePersist` / `@PreUpdate`).
Data source: the request; `ResetPolicy.periodKey(today)`
Message    : —
Traces     : REQ-SEQUENCE-001
Source     : sequence/service/NumberSeriesService.java:68-74; sequence/mapper/NumberSeriesMapper.java:15-27; sequence/entity/NumberSeries.java:51, :71-72, :77-78, :82-83, :87-88, :91-92, :94-109

### RULE-SEQUENCE-011 — المستأجر الجديد يبدأ من 1 / A new tenant starts at 1
Scope      : ENT-SEQUENCE-001 (provisioning)
Trigger    : tenant provisioning, contributor order 40
Statement  : The system shall insert into the new tenant one row per code of the source tenant — the anchor row's `CODE`, `PREFIX`, `PATTERN`, `RESET_POLICY`, `PERIOD_KEY`, `IS_ACTIVE` — with `NEXT_VALUE` 1, `CREATED_BY` = the operator, `CREATED_AT` = now, naming `TENANT_ID` explicitly in the insert and both predicates.
Data source: `CORE_NUMBER_SERIES` rows of the source tenant whose `ID = MIN(ID)` per code
Message    : —
Traces     : REQ-SEQUENCE-017
Source     : sequence/tenant/SequenceTenantProvisioningContributor.java:27-44

### RULE-SEQUENCE-012 — لا تخزين مؤقت / No caching
Scope      : ENT-SEQUENCE-001
Trigger    : every read
Statement  : The system shall not cache series rows: the counter moves on every allocation and the table is not on the caching register.
Data source: —
Message    : —
Traces     : REQ-SEQUENCE-010
Source     : sequence/service/NumberSeriesService.java:43-44; docs/steps/09-report.md "Skills checked" (gov-enforce-caching-rules)

### RULE-SEQUENCE-013 — حقول البحث والترتيب المسموحة / Allowed search and sort fields
Scope      : ENT-SEQUENCE-001 (search)
Trigger    : `POST /search`
Statement  : The system shall accept filters only on `id`, `code`, `periodKey`, `isActive`, `createdAt`, `updatedAt` and sorts only on `id`, `code`, `periodKey`, `createdAt`, `updatedAt`; `tenantId` is never a client field; any other field is refused naming it.
Data source: `ALLOWED_FILTER_FIELDS`, `ALLOWED_SORT_FIELDS`
Message    : `VALIDATION_ERROR` 400 with `fieldErrors[].field` = the offending field (`UNSUPPORTED_FILTER_FIELD` detail)
Traces     : REQ-SEQUENCE-007
Source     : sequence/service/NumberSeriesService.java:51-57, :93-96; common/search/SpecBuilder.java:58-62

### RULE-SEQUENCE-014 — تفرّد الرمز وثباته / Code uniqueness and immutability
Scope      : ENT-SEQUENCE-001.code
Trigger    : on create; on update
Statement  : The system shall prevent two series with the same code (upper-cased) in one tenant, and shall never change a code after creation; the database guards (tenant, code, period) with `UQ_CORE_NUMBER_SERIES_CODE_PERIOD`.
Data source: ENT-SEQUENCE-001 (`existsByCode`)
Message    : `NUMBER_SERIES_CODE_DUPLICATE` (A4)
Traces     : REQ-SEQUENCE-004, REQ-SEQUENCE-008
Source     : sequence/domain/NumberSeriesDomain.java:37-43; sequence/repository/NumberSeriesRepository.java:17, :25; sequence/entity/NumberSeries.java:59-62; V14__sequence_and_settings.sql:57

## A6 — Lookups

**RESET_POLICY of ENT-SEQUENCE-001** — owned by SEQUENCE — control type: fixed value set (CHECK constraint `CHK_CORE_NUMBER_SERIES_RESET` + Java enum `ResetPolicy`, not an MDL lookup)
| Code | Period key | Label (ar) | Label (en) |
|---|---|---|---|
| NEVER | `''` | — | One period for ever |
| YEARLY | `YYYY` | — | One period per calendar year |
| MONTHLY | `YYYY-MM` | — | One period per calendar month |
Source: V14__sequence_and_settings.sql:37, :52, :59; sequence/domain/ResetPolicy.java:9-25 (the English
labels are the enum's Javadoc; the code carries no Arabic value labels — the API returns the code, whose
field label is "سياسة إعادة الترقيم").

Consumed lookups: none.

## A7 — Status lifecycle

**ENT-SEQUENCE-001 NumberSeries.isActive (2 states, flag)**
```
(create, REQ-SEQUENCE-001) ─────────────────────────▶ ACTIVE (isActive = true)
ACTIVE   --(REQ-SEQUENCE-009, deactivate, all rows of the code)--> INACTIVE
INACTIVE --(REQ-SEQUENCE-009, activate, all rows of the code)----> ACTIVE
```
An inactive series refuses allocation and preview (RULE-SEQUENCE-008). There is no terminal state and no
delete (RULE-SEQUENCE-009). A new period row inherits the anchor's activation (sequence/mapper/NumberSeriesMapper.java:41).
Source: sequence/entity/NumberSeries.java:90-92, :118-124; sequence/service/NumberSeriesService.java:117-141.

## A8 — Module dependencies

| Consumed entity | Owner ENT id | Owner module | HARD-FK / SOFT-READ | XM |
|---|---|---|---|---|
| `CORE_TENANT` | ENT-TENANT-001 | TENANT | HARD-FK (`FK_CORE_NUMBER_SERIES_TENANT`) + SOFT-READ (`TenantLookupApi.codeOf`, only for `{TENANT}`) | XM-SEQUENCE-001 (the tenant side: XM-TENANT-001) |

| Consumed surface | Owner | Kind | Source |
|---|---|---|---|
| `TenantContext.require()`; Hibernate `@TenantId` via `AuditableEntity` | TENANT (root package) / common | context | sequence/service/NumberAllocationService.java:100; sequence/entity/NumberSeries.java:48 |
| `TenantProvisioningContributor` | TENANT | SPI implemented by `SequenceTenantProvisioningContributor` (order 40) — XM-SEQUENCE-002 (tenant side: XM-TENANT-002) | sequence/tenant/SequenceTenantProvisioningContributor.java:23-30 |
| `PermissionContributor` | SEC | SPI implemented by `SequencePermissions` | sequence/permission/SequencePermissions.java:18 |
| `@Audited` | audit | entity listener on `CORE_NUMBER_SERIES` (`nextValue` ignored) | sequence/entity/NumberSeries.java:41 |
| `DomainRules`, `LocalizedException`, `Status`, `ServiceResult`, `SpecBuilder`, `PageableBuilder`, `BaseSearchContractRequest`, `OperationCode` | common | foundation | sequence/domain/NumberSeriesDomain.java:3-5; sequence/service/NumberSeriesService.java:3-10 |

**Exposed direction** (consumers of the sequence module)
| XM id | Exposed surface | Kind | Consumers | Source |
|---|---|---|---|---|
| XM-SEQUENCE-003 | `com.erp.sequence.crossmodule.NumberSeriesApi` — `String next(String code)`, `String preview(String code)` | crossmodule call, in-process, ungated | none in core; applications and future business modules | sequence/crossmodule/NumberSeriesApi.java:12-23; sequence/crossmodule/NumberSeriesApiImpl.java:13-28; docs/CONSUMING.md §3 |
| — | `CORE_NUMBER_SERIES` rows of `TENANT_ID = 1` | seed target for applications' `V1000+` scripts | applications | docs/CONSUMING.md §3 (seed example) |

# PART B — SCREEN REQUIREMENTS

## SCR-REQ-SEQUENCE-001 — سلاسل الترقيم / Number Series
### B1 — Definition
Purpose      : إدارة سلاسل ترقيم المستأجر: إنشاء سلسلة، استعراض فتراتها، تعديل البادئة والنمط، التفعيل وإلغاء التفعيل.
Entities     : ENT-SEQUENCE-001
Operations   : search, read, create, update (prefix, pattern), activate, deactivate
Users        : مدير المستأجر (أي موظف يحمل صلاحيات الشاشة)
Navigation   : SEQUENCE (الترقيم التسلسلي / Sequences) → Number Series (registry module `SEQUENCE`)
Content shape: flat record (search list of period rows + create / edit form + activation action)
Traces       : REQ-SEQUENCE-001…009, REQ-SEQUENCE-016, REQ-SEQUENCE-018
Composite    : Search + Entry = ONE screen requirement
### B2 — Search / list
Filters: `id`, `code`, `periodKey`, `isActive`, `createdAt`, `updatedAt`; sort: `id`, `code`, `periodKey`,
`createdAt`, `updatedAt` (sequence/service/NumberSeriesService.java:51-57). Page 0, size 20, max 200.
### B3 — Input
Create: `code` (required, `^[A-Za-z0-9_]+$`, ≤ 50), `prefix` (≤ 20), `pattern` (≤ 100, default
`{PREFIX}-{YYYY}-{SEQ:6}`), `resetPolicy` (NEVER | YEARLY | MONTHLY, default YEARLY), `nextValue` (≥ 1,
default 1) — sequence/dto/NumberSeriesCreateRequest.java:22-42. Update: `prefix` (≤ 20, null clears it),
`pattern` (required, ≤ 100) — sequence/dto/NumberSeriesUpdateRequest.java:22-29. No delete; the counter,
code, policy and period are read-only.
### B4 — Access
Page code: `SEQUENCE_SERIES`. Actions: `VIEW` (`PERM_SEQUENCE_SERIES_VIEW`, gateway; gates read and
search) and `MANAGE` (`PERM_SEQUENCE_SERIES_MANAGE`, every write). Staff chain only (customer tokens
answer 403 `REALM_MISMATCH`). Every tenant's super role holds both.
### B5 — API expectations
| API-ID | Operation | Verb | Path | Inputs | Outputs | Permission | RULEs | Errors | Traces (REQ) |
|---|---|---|---|---|---|---|---|---|---|
| API-SEQUENCE-001 | create series | POST | /api/v1/sequence/series | `NumberSeriesCreateRequest` | 201 `NumberSeriesResponse` | `PERM_SEQUENCE_SERIES_MANAGE` | RULE-SEQUENCE-001, -002, -010, -014 | 400 `VALIDATION_ERROR`, 400 `SEQUENCE_PATTERN_INVALID`, 409 `NUMBER_SERIES_CODE_DUPLICATE` | REQ-SEQUENCE-001…005 |
| API-SEQUENCE-002 | search period rows | POST | /api/v1/sequence/series/search | `NumberSeriesSearchRequest` (filters, sortField, sortDirection, page, size) | 200 `Page<NumberSeriesResponse>` | `PERM_SEQUENCE_SERIES_VIEW` | RULE-SEQUENCE-013 | 400 `VALIDATION_ERROR` | REQ-SEQUENCE-007 |
| API-SEQUENCE-003 | read period row | GET | /api/v1/sequence/series/{id} | id | 200 `NumberSeriesResponse` | `PERM_SEQUENCE_SERIES_VIEW` | — | 404 `NUMBER_SERIES_NOT_FOUND` | REQ-SEQUENCE-006 |
| API-SEQUENCE-004 | update series (all periods of its code) | PUT | /api/v1/sequence/series/{id} | `NumberSeriesUpdateRequest` (`prefix`, `pattern`) | 200 `NumberSeriesResponse` | `PERM_SEQUENCE_SERIES_MANAGE` | RULE-SEQUENCE-001, -002, -004, -009 | 400 `VALIDATION_ERROR`, 400 `SEQUENCE_PATTERN_INVALID`, 404 `NUMBER_SERIES_NOT_FOUND`, 409 `CONCURRENT_MODIFICATION` | REQ-SEQUENCE-008 |
| API-SEQUENCE-005 | activate series | PUT | /api/v1/sequence/series/{id}/activate | id | 200 `NumberSeriesResponse` | `PERM_SEQUENCE_SERIES_MANAGE` | RULE-SEQUENCE-004 | 404 `NUMBER_SERIES_NOT_FOUND` | REQ-SEQUENCE-009 |
| API-SEQUENCE-006 | deactivate series | PUT | /api/v1/sequence/series/{id}/deactivate | id | 200 `NumberSeriesResponse` | `PERM_SEQUENCE_SERIES_MANAGE` | RULE-SEQUENCE-004, -008 | 404 `NUMBER_SERIES_NOT_FOUND` | REQ-SEQUENCE-009 |
Every path additionally answers 401 `SEC-401-INVALID-CREDENTIALS` without a token and 403 `ACCESS_DENIED`
without the authority (REQ-SEQUENCE-018). Source: sequence/controller/NumberSeriesController.java:29-75;
`docs/api-docs/sequence/index.md:122-131`; `docs/api-docs/sequence/endpoints/number-series-management.md`.

> **In-process (not HTTP):** `NumberSeriesApi.next(code)` (RULE-SEQUENCE-005, -008; `SEQUENCE_NOT_CONFIGURED`,
> `SEQUENCE_PATTERN_INVALID` never — the stored pattern was validated at save time; `TENANT_CONTEXT_MISSING`
> without a tenant) and `NumberSeriesApi.preview(code)` (RULE-SEQUENCE-006, -008) — REQ-SEQUENCE-010…014, -020;
> XM-SEQUENCE-003.

# STANDALONE

## Traceability matrix
| P0.5 | REQ | AC | RULE | ENT | SCR-REQ |
|---|---|---|---|---|---|
| US-SEQUENCE-001 | REQ-SEQUENCE-001…005, -015, -018 | AC-SEQUENCE-001…005, -015, -018 | RULE-SEQUENCE-001, -002, -007, -010, -014 | ENT-SEQUENCE-001 | SCR-REQ-SEQUENCE-001 |
| US-SEQUENCE-002 | REQ-SEQUENCE-002, -003, -008, -009, -018, -019 | AC-SEQUENCE-002, -003, -008, -009, -018, -019 | RULE-SEQUENCE-001, -002, -004, -009, -014 | ENT-SEQUENCE-001 | SCR-REQ-SEQUENCE-001 |
| US-SEQUENCE-003 | REQ-SEQUENCE-006, -007, -016, -018 | AC-SEQUENCE-006, -007, -016, -018 | RULE-SEQUENCE-012, -013 | ENT-SEQUENCE-001 | SCR-REQ-SEQUENCE-001 |
| US-SEQUENCE-004 | REQ-SEQUENCE-010, -011, -012, -014, -015, -016, -019, -020 | AC-SEQUENCE-010, -011, -012, -014, -015, -016, -019, -020 | RULE-SEQUENCE-003, -004, -005, -007, -008, -012 | ENT-SEQUENCE-001 | — |
| US-SEQUENCE-005 | REQ-SEQUENCE-011, -012, -013, -014, -020 | AC-SEQUENCE-011, -012, -013, -014, -020 | RULE-SEQUENCE-003, -006, -008 | ENT-SEQUENCE-001 | — |
| US-SEQUENCE-006 | REQ-SEQUENCE-017 | AC-SEQUENCE-017 | RULE-SEQUENCE-011 | ENT-SEQUENCE-001 | — |

Every story traces to ≥ 1 REQ; every REQ has one AC; every RULE traces to a REQ; the screen traces to
its REQs. No orphan, no dangling id.

## Decisions applied
| DEFAULT / ADR | What | Source | Status |
|---|---|---|---|
| ADR-SEQUENCE-001 | anchor-row pessimistic lock in `REQUIRES_NEW`; gaps accepted, not gap-free | governance/analysis/decisions/SEQUENCE/ADR-SEQUENCE-001.md | ACCEPTED (as built) |
| ADR-SEQUENCE-002 | one row per (code, period) with an immutable reset policy; the pattern must carry the period | governance/analysis/decisions/SEQUENCE/ADR-SEQUENCE-002.md | ACCEPTED (as built) |
| ADR-SEQUENCE-003 | no delete — deactivate only; counters reset to 1 on tenant provisioning; `NumberSeriesApi` ungated | governance/analysis/decisions/SEQUENCE/ADR-SEQUENCE-003.md | ACCEPTED (as built) |
| DEFAULT | PK column `ID` with `SEQ_CORE_NUMBER_SERIES` (new-table convention) | docs/steps/09-report.md "Skills checked" | as built |
| DEFAULT | `RESET_POLICY` CHECK + enum, not an MDL lookup | V14__sequence_and_settings.sql:59; pattern of ADR-SEC-001 | as built |

## Access summary
| Page code | Screen | VIEW | CREATE | UPDATE | DELETE | Custom |
|---|---|---|---|---|---|---|
| SEQUENCE_SERIES | Number Series | `PERM_SEQUENCE_SERIES_VIEW` (gateway; read, search) | — | — | — | `PERM_SEQUENCE_SERIES_MANAGE`: create, update, activate, deactivate |
Every action beyond VIEW additionally requires VIEW on the same screen (RULE-SEC-007). The frontend's
screen archive is `governance/frontend/modules/SEQUENCE/tests/specs/sequences/number-series.spec.ts`.
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 09
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.
