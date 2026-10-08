# SRS — الأساس المشترك / Common foundation (COMMON)
══════════════════════════════════════════════════════════════════
Module : COMMON   Version : v1 (as-built baseline)   Profile : erp
Inputs : prd-common, module-registry-common, business-policies-common; the code at main @ 19b19a4 (erp-core 1.2.0 behaviour)
Counts : ENT 2 · REQ 18 · AC 18 · RULE 10 · SCR-REQ 0 · XM 1 · ADR 3
══════════════════════════════════════════════════════════════════

Written from the code, not before it. Every REQ / AC / RULE names the code location it was read from;
the id → location index is `registry-srs-common.md`. Java paths are relative to
`erp-core/src/main/java/com/erp/`, migrations to `erp-core/src/main/resources/db/migration/core/`,
`file:line` at main @ 19b19a4. There is no endpoint of its own: verification is by every module's
endpoints (`TC-CORE-CORE-001…008` of `docs/test-api/core-test-plan.md` for the cross-cutting cases) and
the JUnit classes under `erp-core/src/test/java/com/erp/common/` and `…/architecture/`.

# PART A — MODULE FOUNDATION

## A1 — Document information
| Item | Value |
|---|---|
| Module | COMMON — الأساس المشترك / Common foundation (package `com.erp.common`; no registry module, no screen, no endpoint) |
| Feature code | COMMON |
| Version | v1 (as built in erp-core 1.2.0) |
| Date | 2026-10-07 |
| Status | AS-BUILT baseline |
| Prepared by | analysis lane, from the code |
| Decisions applied count | 3 (ADR-COMMON-001, -002, -003) |

## A2 — Functional context

**In scope:** غلاف الاستجابة `ApiResponse` وتحويل `Status` إلى HTTP، الاستثناء المرمَّز
`LocalizedException` ورموز `CommonErrorCodes` التسعة والمعالج العام `GlobalExceptionHandler` وكاتب
الغلاف في المرشّحات، عقد البحث (`SearchRequest`، `SpecBuilder`، `PageableBuilder`، المحوّلات)، الصنفان
الأساسيان `GlobalAuditableEntity` / `AuditableEntity` ومستمع التدقيق، حرّاس المجال `DomainRules` و
`StatusTransitions`، مساعد القوائم المملوكة `OwnedLookups`، محوّلات القيم المنطقية، و`SecurityContextHelper`
والمساعدات الصغيرة.

**Out of scope:** كتالوج أخطاء محكوم داخل الأساس، معاملا `IS NULL` / `OR` في `SpecBuilder`، حالة 503،
ترجمة رسالة الجسم المشوَّه، آلية التكرار (`idempotency`، 1.3.0)، سياسة كلمة المرور (SEC) —
[business-policies-common.md → SCOPE EXCEPTIONS].

**Module function (one paragraph):** حزمة `com.erp.common` تعطي كل وحدة العقد نفسه مع العميل
(غلاف ورموز ورسائل ثنائية اللغة)، والطريقة نفسها في رفض الخطأ والبحث والترقيم، والأعمدة نفسها في كل
جدول (تدقيق، قفل تفاؤلي، مستأجر)، دون أن تعتمد هي على أي وحدة.

**Detailed description (workflow narrative, roles):** مطوّر الوحدة يرمي `LocalizedException` برمز مسجَّل
ويعيد `ServiceResult`؛ يبني البحث بـ `SpecBuilder` و`PageableBuilder`؛ يرث كيانه `AuditableEntity`؛
يستدعي `DomainRules` و`StatusTransitions` من كائن المجال. العميل يقرأ الغلاف الواحد. مشغّل المنصة يرى
رسائل عربية/إنجليزية بحسب `Accept-Language`.

**Current situation:** built (pre-erp-core foundation; reshaped by steps 01, 05, 15; helper move on main, CHANGELOG [Unreleased]).

**General notes:** the package imports no other `com.erp` package; the tenant binding of `AuditableEntity`
is Hibernate's (`@TenantId`), resolved by the tenant module at runtime.

## A3 — Entities and fields

### ENT-COMMON-001 — الكيان العام المدقَّق / Global auditable entity (`GlobalAuditableEntity`)
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| `@MappedSuperclass` (no table of its own); `@EntityListeners(AuditEntityListener)`; `@SuperBuilder`, protected no-arg constructor | SHARED (owner) — extended directly by the global allow-list only: `ModuleRegistry`, `ScreenRegistry`, `ActionRegistry` (SEC), `Tenant` (TENANT), `AppConfiguration` (CU) | No | fields filled by the listener on persist / update; `version` managed by Hibernate | base of every global entity; parent of ENT-COMMON-002 | common/domain/GlobalAuditableEntity.java:15-53 |

| Field | Logical type | Required | Values / source | Column (physical) | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|---|
| createdBy | text (≤ 100) | yes (system) | `SecurityContextHelper.getCurrentUsername()` (`system` without caller) | `CREATED_BY`, `updatable = false`, `length = 100` | the JPA length is the narrowest live width (SEC/MDL/CORE 100; CU/NOTIF/FILE 255) | أنشئ بواسطة | Created by |
| createdAt | instant | yes (system) | `Instant.now()` on persist | `CREATED_AT`, `updatable = false` | — | تاريخ الإنشاء | Created at |
| updatedBy | text (≤ 100) | yes (system) | current user on persist and on every update | `UPDATED_BY`, `length = 100` | — | عُدِّل بواسطة | Updated by |
| updatedAt | instant | yes (system) | `Instant.now()` on persist and on every update | `UPDATED_AT` | — | تاريخ التعديل | Updated at |
| version | number | yes (system) | 0 on insert, +1 per update (Hibernate `@Version`) | `VERSION NOT NULL` (`BIGINT DEFAULT 0`, V10) | stale update → 409 `CONCURRENT_MODIFICATION`; never set by hand | الإصدار | Version |
Source: common/domain/GlobalAuditableEntity.java:30-53; common/audit/AuditEntityListener.java:12-26; V10__tenant_schema.sql:7-8.

### ENT-COMMON-002 — الكيان الخاص بمستأجر / Tenant-scoped auditable entity (`AuditableEntity extends GlobalAuditableEntity`)
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| `@MappedSuperclass` adding the Hibernate discriminator | SHARED (owner) — the base of every other `@Entity` (21 tenant-aware entities) | No | `tenantId` set by Hibernate from the session's tenant on insert; added as a predicate to every query, join and load by id; never updatable | `TENANT_ID` is a HARD FK to `CORE_TENANT(ID)` on every tenant-scoped table (TENANT's DBF-TENANT-011…032) | common/domain/AuditableEntity.java:12-37 |

| Field | Logical type | Required | Values / source | Column (physical) | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|---|
| tenantId | number | yes (system) | `@TenantId` — the current tenant resolved by `TenantIdentifierResolver` when the session opens | `TENANT_ID NOT NULL`, `updatable = false` (`BIGINT`, FK `FK_<TABLE>_TENANT`, index `IDX_<TABLE>_TENANT`) | never set by application code; `isRoot = false` → no tenant sees another's rows | المستأجر | Tenant |
| (inherited) | ENT-COMMON-001 fields | — | — | — | — | — | — |
Source: common/domain/AuditableEntity.java:34-37; tenant/config/TenantIdentifierResolver.java; `erp-core/src/main/resources/db/migration/core/README.md` "Tenant columns (since V10)".

## A4 — Functional requirements (EARS) and acceptance criteria

### REQ-COMMON-001 — غلاف استجابة موحّد / Uniform response envelope
Pattern    : ubiquitous
Statement  : The system shall answer every JSON endpoint with `ApiResponse<T>`: `success`, `data` (on success), `error{code, message, fieldErrors[]{field, message}}` (on failure), `timestamp`; null members are omitted; a controller builds a success through `OperationCode.craftResponse(ServiceResult)`, whose `Status` gives the HTTP status (200 / 201).
Traces     : US-COMMON-001
Entities   : —
Rationale  : POL-COMMON-001
Source     : common/web/ApiResponse.java:11-33; common/web/ApiError.java:9-14; common/web/FieldErrorItem.java:8-12; common/web/OperationCode.java:10-14; common/domain/status/ServiceResult.java:6-23
Priority   : HIGH
#### AC-COMMON-001 — [REQ-COMMON-001]
Given any module endpoint
When it succeeds and when it fails
Then the body is `{success: true, data: …, timestamp}` or `{success: false, error: {code, message[, fieldErrors]}, timestamp}` (every `docs/api-docs/<module>/index.md` "Common Response Envelope")

### REQ-COMMON-002 — استثناء مرمَّز واحد / One coded exception
Pattern    : ubiquitous
Statement  : A business or validation failure shall be thrown as `LocalizedException(Status, errorCode, args…)`; several failures at once as `LocalizedException(Status, List<ErrorDetail>)` (the first detail gives the top-level code) or `LocalizedException.withDetails(Status, errorCode, details)` (explicit top-level code); an `ErrorDetail` carries an optional `field`, a code and its arguments.
Traces     : US-COMMON-002, US-COMMON-005
Entities   : —
Rationale  : POL-COMMON-002
Source     : common/exception/LocalizedException.java:27-83; common/exception/ErrorDetail.java:25-39
Priority   : HIGH
#### AC-COMMON-002 — [REQ-COMMON-002]
Given `TenantDomain.create` with a duplicate code, and `ReportParametersDomain.validate` with three bad parameters
When each throws
Then the first is a single-code `LocalizedException(ALREADY_EXISTS, TENANT_CODE_DUPLICATE, code)` and the second a multi-error `LocalizedException(VALIDATION_ERROR, [3 details])` whose top-level code is `REPORT_PARAM_INVALID` (tenant/domain/TenantDomain.java:38; report/domain/ReportParametersDomain.java:83-85)

### REQ-COMMON-003 — تحويل الحالة إلى HTTP والرسالة إلى لغة الطلب / Status → HTTP and message → locale
Pattern    : event
Statement  : When a `LocalizedException` reaches the dispatcher, the system shall answer `Status.getHttpStatus()` with `error.code` = the error code and `error.message` = the bundle text of that code in `LocaleContextHolder.getLocale()` with the exception's arguments (the code itself, and a WARN, when no bundle entry exists), plus one `fieldErrors[]` entry per `ErrorDetail` whose `field` is `detail.field()` or, when null, `detail.errorCode()`.
Traces     : US-COMMON-001, US-COMMON-002
Entities   : —
Rationale  : POL-COMMON-002
Source     : common/web/GlobalExceptionHandler.java:38-60, :225-233; common/domain/status/Status.java:7-31
Priority   : HIGH
#### AC-COMMON-003 — [REQ-COMMON-003]
Given a request with `Accept-Language: ar` that triggers `REPORT_NOT_FOUND`
When the handler answers
Then the status is 404, `error.code` is `REPORT_NOT_FOUND` and `error.message` is "التقرير ''NOPE'' غير موجود" (`GlobalExceptionHandlerTest`; TC-CORE-REPORT-002 with `AL:ar` cases of the plan)

### REQ-COMMON-004 — أخطاء الإطار كأخطاء عميل / Framework failures as client errors
Pattern    : unwanted
Statement  : If a request fails bean validation, carries a malformed body, lacks a required parameter or sends one of the wrong type, uses an unsupported HTTP method, violates a database constraint, or is refused by method security, then the system shall answer, respectively, 400 `VALIDATION_ERROR` (one `fieldErrors` entry per field), 400 `VALIDATION_ERROR` (fixed English message), 400 `VALIDATION_ERROR` (`fieldErrors[0].field` = the parameter name), 405 `METHOD_NOT_ALLOWED` with an `Allow` header, 409 `DATA_INTEGRITY_VIOLATION`, 403 `ACCESS_DENIED`.
Traces     : US-COMMON-002
Entities   : —
Rationale  : POL-COMMON-003
Source     : common/web/GlobalExceptionHandler.java:62-76, :87-95, :105-121, :127-140, :142-150, :163-170
Priority   : HIGH
#### AC-COMMON-004 — [REQ-COMMON-004]
Given an empty create body `{}` on `POST /api/v1/platform/tenants`, a `PUT` on a GET-only path, and a caller without the required authority
When each is sent
Then 400 `VALIDATION_ERROR` naming every missing field (TC-CORE-TENANT-010), 405 `METHOD_NOT_ALLOWED`, and 403 `ACCESS_DENIED` (every "Other Possible Responses" table of the api-docs)

### REQ-COMMON-005 — تعديل متزامن 409 / Concurrent modification is 409
Pattern    : unwanted
Statement  : If an update fails Hibernate's optimistic-lock check (`OptimisticLockingFailureException`), then the system shall answer 409 `CONCURRENT_MODIFICATION`.
Traces     : US-COMMON-004
Entities   : ENT-COMMON-001
Rationale  : POL-COMMON-005 (step 05)
Source     : common/web/GlobalExceptionHandler.java:152-161; common/domain/GlobalAuditableEntity.java:46-53; common/exception/CommonErrorCodes.java:29-31
Priority   : HIGH
#### AC-COMMON-005 — [REQ-COMMON-005]
Given two clients that loaded the same row
When both save a change
Then the second answers 409 `CONCURRENT_MODIFICATION` "This record was changed by someone else in the meantime. Reload it and try again." (docs/DEVIATIONS.md [05]; `docs/api-docs/*/index.md`)

### REQ-COMMON-006 — مسار مجهول 404 / Unknown path is 404
Pattern    : unwanted
Statement  : If a request reaches the dispatcher for a path no controller or static resource serves (`NoResourceFoundException`), then the system shall answer 404 `NOT_FOUND` (an anonymous request on a protected prefix still gets 401 from the security chain first).
Traces     : US-COMMON-002
Entities   : —
Rationale  : POL-COMMON-003 (1.2.0; was 500)
Source     : common/web/GlobalExceptionHandler.java:172-186; common/exception/CommonErrorCodes.java:33-35
Priority   : HIGH
#### AC-COMMON-006 — [REQ-COMMON-006]
Given an authenticated caller, an anonymous caller under `/swagger-ui/**`, and an anonymous caller under `/api/v1/**`
When each requests an unknown path
Then 404 `NOT_FOUND`, 404 `NOT_FOUND`, and 401 (`ErrorResponseHardeningIntegrationTest.anUnknownPath_forAnAuthenticatedCaller_is404NotFound`, `…_underAPermittedPrefix_forAnAnonymousCaller_is404NotFound`, `…_underAProtectedPrefix_forAnAnonymousCaller_staysUnauthorized`; TC-CORE-CORE-007)

### REQ-COMMON-007 — الاستثناء الملفوف يُجاب برمزه / A wrapped exception answers with its own code
Pattern    : unwanted
Statement  : If an exception reaching the catch-all wraps a `LocalizedException` anywhere in its cause chain (depth ≤ 16, cycle-safe), then the system shall answer that `LocalizedException`'s status and code; otherwise 500 `INTERNAL_ERROR`.
Traces     : US-COMMON-002
Entities   : —
Rationale  : POL-COMMON-003 (1.2.0; `TENANT_CONTEXT_MISSING` inside `CannotCreateTransactionException` used to be a bare 500)
Source     : common/web/GlobalExceptionHandler.java:188-223
Priority   : HIGH
#### AC-COMMON-007 — [REQ-COMMON-007]
Given a transaction opened without a tenant after start-up
When the transaction manager wraps `TENANT_CONTEXT_MISSING`
Then the response is 500 with `error.code = TENANT_CONTEXT_MISSING` (`ErrorResponseHardeningIntegrationTest.tenantContextMissing_wrappedByTheTransactionManager_isAnsweredWithItsOwnCode`)

### REQ-COMMON-008 — الغلاف من داخل المرشّح / The envelope from inside a filter
Pattern    : optional
Statement  : Where a servlet filter refuses a request before the dispatcher, the system shall write the same envelope by hand (`FilterErrorResponseWriter.write`): status, `application/json` UTF-8, `{success: false, error: {code, message}, timestamp}`, the message resolved in `request.getLocale()` and the code itself when unknown.
Traces     : US-COMMON-001
Entities   : —
Rationale  : POL-COMMON-001 — neither the handler nor Jackson runs before the dispatcher
Source     : common/web/FilterErrorResponseWriter.java:12-46; sec/security/SecSecurityErrorHandler.java; tenant/security/TenantResolutionFilter.java
Priority   : HIGH
#### AC-COMMON-008 — [REQ-COMMON-008]
Given a public staff login without `X-Tenant-Code`
When the tenant filter refuses it
Then the body is `{"success":false,"error":{"code":"TENANT_REQUIRED","message":"…"},"timestamp":"…"}` with status 400 (TC-CORE-TENANT-001)

### REQ-COMMON-009 — عقد البحث / The search contract
Pattern    : ubiquitous
Statement  : A search request shall carry `filters[]{field, operator, value}`, `sortField`, `sortDirection` (ASC default), `page` (0), `size` (20); `SpecBuilder.build(request, allowedFields, converter)` shall AND one predicate per filter with a non-null converted value, using `EQUALS`, `NOT_EQUALS`, `LIKE` (lower-cased contains), `GREATER_THAN`, `GREATER_THAN_OR_EQUAL`, `LESS_THAN`, `LESS_THAN_OR_EQUAL`, `IN` (list).
Traces     : US-COMMON-003
Entities   : —
Rationale  : POL-COMMON-004; `tenantId` is never a client filter field (Hibernate adds it)
Source     : common/search/SearchRequest.java:14-29; common/search/SearchFilter.java:12-17; common/search/SearchOperator.java:3-12; common/search/SpecBuilder.java:21-41, :67-80; common/dto/BaseSearchContractRequest.java:25-61
Priority   : HIGH
#### AC-COMMON-009 — [REQ-COMMON-009]
Given `POST /api/v1/platform/tenants/search` with `{"filters":[{"field":"code","operator":"EQUALS","value":"ACME"}]}`
When it runs
Then exactly that tenant is returned (TC-CORE-TENANT-011); `SpecBuilder` adds no predicate for a null value

### REQ-COMMON-010 — رفض حقل أو معامل غير مدعوم / Reject an unsupported field or operator
Pattern    : unwanted
Statement  : If a search names a filter field outside the endpoint's allowed set, then the system shall answer 400 `VALIDATION_ERROR` with one `fieldErrors` entry per offending field (`field` = the field, detail code `UNSUPPORTED_FILTER_FIELD`); if a lifted id filter uses an operator other than scalar `EQUALS`, then 400 with detail code `UNSUPPORTED_FILTER_OPERATOR`; a non-numeric lifted id value is 400 `VALIDATION_ERROR`.
Traces     : US-COMMON-003
Entities   : —
Rationale  : POL-COMMON-004
Source     : common/search/SpecBuilder.java:43-64; common/dto/BaseSearchContractRequest.java:94-144; common/exception/CommonErrorCodes.java:20-27
Priority   : HIGH
#### AC-COMMON-010 — [REQ-COMMON-010]
Given a tenant search filtering on `bogus`
When it runs
Then 400 `VALIDATION_ERROR` with `fieldErrors[0].field = bogus` and message "This search does not support filtering on "bogus"" (TC-CORE-TENANT-011 unsupported-filter case; `SecSearchFilterIntegrationTest`)

### REQ-COMMON-011 — الترقيم / Paging
Pattern    : ubiquitous
Statement  : `PageableBuilder.from(request, allowedSortFields)` shall clamp `page` to ≥ 0, use size 20 when `size ≤ 0` and at most 200 otherwise, sort only on an allowed field (ASC default), and reject with 400 `VALIDATION_ERROR` on field `page` a page whose first row would pass `Integer.MAX_VALUE` (`page × size + size`).
Traces     : US-COMMON-003
Entities   : —
Rationale  : JPA's first-result is an `int` (1.2.0: rejected, not clamped)
Source     : common/search/PageableBuilder.java:15-16, :28-45
Priority   : HIGH
#### AC-COMMON-011 — [REQ-COMMON-011]
Given `page = 2147483647, size = 20` and `page = 10737418, size = 200` (the largest representable)
When each is built
Then the first answers 400 `VALIDATION_ERROR` with `fieldErrors[0].field = page`, the second an empty page (`PageableBuilderTest`; `ErrorResponseHardeningIntegrationTest.aPageWhoseOffsetOverflows_is400ValidationErrorOnPage`, `.aLargeButRepresentablePage_isAnEmptyPage`)

### REQ-COMMON-012 — تحويل قيم المرشّحات / Filter value conversion
Pattern    : optional
Statement  : Where a search declares a `FieldValueConverter`, the system shall convert listed boolean fields from text (`BooleanFieldValueConverter`) and listed instant fields from ISO-8601 text to `Instant` (`InstantFieldValueConverter`; a malformed value is 400 `VALIDATION_ERROR`); otherwise values pass through (`DefaultFieldValueConverter.INSTANCE`).
Traces     : US-COMMON-003
Entities   : —
Rationale  : a JSON body carries timestamps as strings; the criteria build must compare like with like
Source     : common/search/FieldValueConverter.java:3-6; common/search/BooleanFieldValueConverter.java:5-23; common/search/InstantFieldValueConverter.java:17-36; common/search/DefaultFieldValueConverter.java:3-14
Priority   : MEDIUM
#### AC-COMMON-012 — [REQ-COMMON-012]
Given `GET /api/v1/audit/events?from=not-a-date`
When it runs
Then 400 `VALIDATION_ERROR` (audit/service/AuditEventService.java uses `InstantFieldValueConverter`; `docs/api-docs/audit/`)

### REQ-COMMON-013 — أعمدة التدقيق تُملأ تلقائيًا / Audit columns are filled automatically
Pattern    : event
Statement  : When an entity extending ENT-COMMON-001 is persisted, the system shall set `createdBy`, `updatedBy` to the current username (`system` without an authenticated caller) and `createdAt`, `updatedAt` to now; when it is updated, `updatedBy` and `updatedAt` only.
Traces     : US-COMMON-004
Entities   : ENT-COMMON-001, ENT-COMMON-002
Rationale  : POL-COMMON-005
Source     : common/audit/AuditEntityListener.java:12-26; common/util/SecurityContextHelper.java:27-33
Priority   : HIGH
#### AC-COMMON-013 — [REQ-COMMON-013]
Given operator `admin` creates and later suspends a tenant
When the row is read
Then `createdBy = admin`, `updatedBy = admin`, `updatedAt > createdAt`, and the bootstrap runner's rows carry `system`

### REQ-COMMON-014 — المميّز المستأجري على كل كيان خاص / The tenant discriminator on every tenant-scoped entity
Pattern    : ubiquitous
Statement  : Every entity extending ENT-COMMON-002 shall carry `TENANT_ID` as Hibernate's `@TenantId` (NOT NULL, not updatable), filled from the session's tenant on insert and added to every query, join and load by id, so that a row of another tenant is never found.
Traces     : US-COMMON-004
Entities   : ENT-COMMON-002
Rationale  : POL-COMMON-006; ADR-TENANT-001
Source     : common/domain/AuditableEntity.java:12-37
Priority   : HIGH
#### AC-COMMON-014 — [REQ-COMMON-014]
Given users in tenants A and B
When A searches users and reads B's user by id
Then B's rows never appear and the id answers 404 (TC-CORE-TENANT-014…016; `TenantScopedQueryIntegrationTest.tenantIdIsAssignedFromTheSession_andIsNotUpdatable`)

### REQ-COMMON-015 — قائمة السماح للكيانات العامة / The global-entity allow-list
Pattern    : ubiquitous
Statement  : Every `@Entity` shall extend ENT-COMMON-001 or ENT-COMMON-002; only `com.erp.sec.entity.ModuleRegistry`, `ScreenRegistry`, `ActionRegistry`, `com.erp.tenant.entity.Tenant` and `com.erp.cu.entity.AppConfiguration` may extend `GlobalAuditableEntity` directly, and none of them may extend `AuditableEntity`; the build fails otherwise.
Traces     : US-COMMON-004
Entities   : ENT-COMMON-001, ENT-COMMON-002
Rationale  : POL-COMMON-006 (a new global entity needs a step that names it global)
Source     : `erp-core/src/test/java/com/erp/architecture/CoreLibraryRulesArchTest.java:67-75`, `:110-133`; common/domain/GlobalAuditableEntity.java:16-20
Priority   : HIGH
#### AC-COMMON-015 — [REQ-COMMON-015]
Given a new `@Entity` extending `GlobalAuditableEntity` that is not on the list
When `mvn verify` runs
Then `rule2_only_the_listed_entities_are_global` fails naming the class (docs/steps/12-report.md, each rule verified against a deliberate violation)

### REQ-COMMON-016 — حرّاس المجال المشتركة / Shared domain guards
Pattern    : optional
Statement  : Where a Domain object checks uniqueness, required text or a status transition, the system shall provide `DomainRules.assertUnique(alreadyTaken, code, args…)` (409 `ALREADY_EXISTS`), `DomainRules.assertNotBlank(code, values…)` (400 `VALIDATION_ERROR`) and `StatusTransitions(allowed, code).assertAllowed(from, to)` (422 `BUSINESS_RULE_VIOLATION` with `(from, to)`; a status absent from the table, or mapped to an empty set, is terminal).
Traces     : US-COMMON-005
Entities   : —
Rationale  : each Domain keeps its rules and order; only the throw is shared
Source     : common/domain/DomainRules.java:17-30; common/domain/StatusTransitions.java:9-33
Priority   : MEDIUM
#### AC-COMMON-016 — [REQ-COMMON-016]
Given `TenantDomain.create` with a taken code, and `NotificationLogDomain` moving `SENT` → `QUEUED`
When each guard runs
Then 409 `TENANT_CODE_DUPLICATE` (tenant/domain/TenantDomain.java:38; `TenantDomainTest`) and 422 with NOTIF's transition code (notif/domain/NotificationLogDomain.java; `NotificationDomainsTest`)

### REQ-COMMON-017 — القوائم المملوكة / Module-owned lookups
Pattern    : optional
Statement  : Where a module serves the LOVs it owns but MDL stores, `OwnedLookups.read(rawKey, ownedKeys, unknownKeyErrorCode, reader)` shall trim and upper-case the key, refuse a key outside `ownedKeys` with the module's own 404 code, call the reader for an owned key, and re-map a `NOT_FOUND` from the reader to the module's code (any other exception propagates); options are `LookupOptionResponse(code, labelAr, labelEn)`.
Traces     : US-COMMON-005
Entities   : —
Rationale  : MDL's codes never leak out of another module's API (cross-module rule)
Source     : common/lookup/OwnedLookups.java:9-46; common/lookup/LookupOptionResponse.java:18-28; file/service/FileLookupService.java; notif/service/NotificationLookupService.java
Priority   : MEDIUM
#### AC-COMMON-017 — [REQ-COMMON-017]
Given FILE's lookup endpoint asked for `NOTIF_CHANNEL`
When it runs
Then 404 with FILE's own unknown-key code, never `MDL_404_TYPE_KEY` (`docs/api-docs/file/`)

### REQ-COMMON-018 — مساعد سياق الأمان / The security-context helper
Pattern    : ubiquitous
Statement  : `SecurityContextHelper` shall answer `getCurrentUsername()` (`system` when none or unauthenticated), `hasAuthority(authority)` (false without an authenticated caller), `currentCaller()` (null for none, unauthenticated or anonymous), `currentActorOrSystem()` and `currentRealm()` (`SYSTEM` / `CUSTOMER` for `ROLE_CUSTOMER` / `STAFF`), with the constants `REALM_STAFF`, `REALM_CUSTOMER`, `REALM_SYSTEM`, `CUSTOMER_AUTHORITY = "ROLE_CUSTOMER"`.
Traces     : US-COMMON-004, US-COMMON-005
Entities   : —
Rationale  : one realm mapping shared by the audit columns, the events (`DomainEvent`) and the audit log (`AuditApi`)
Source     : common/util/SecurityContextHelper.java:7-83; events/DomainEvent.java:37-46; audit/crossmodule/AuditApi.java:28-30
Priority   : HIGH
#### AC-COMMON-018 — [REQ-COMMON-018]
Given a staff token, a customer token and no token
When `currentRealm()` runs
Then `STAFF`, `CUSTOMER`, `SYSTEM` (`DomainEventTest`; docs/DEVIATIONS.md [08] "DomainEvent realm")

## A5 — Business rules

### RULE-COMMON-001 — كل خطأ `LocalizedException` برمز مسجَّل ثنائي اللغة / Every error is a `LocalizedException` with a registered bilingual code
Scope      : every request path of every module
Trigger    : on any failure
Statement  : A request path shall throw only `LocalizedException` with a code registered in a `<Module>ErrorCodes` class (or `CommonErrorCodes`) whose value is its i18n key, present in `messages.properties` (English base) and `messages_ar.properties`; since 1.2.0 an exception wrapping a `LocalizedException` is answered with the wrapped code. (Start-up failures are the exception: `IllegalStateException`, e.g. `ReportRegistry`, `FileStorageAutoConfiguration`.)
Data source: the bundles; `GlobalExceptionHandler.resolveMessage`
Message    : the code's own text; the code itself when the bundle has no entry (WARN logged)
Traces     : REQ-COMMON-002, REQ-COMMON-003, REQ-COMMON-007
Source     : common/exception/LocalizedException.java:27-33; common/web/GlobalExceptionHandler.java:195-233; docs/DEVIATIONS.md [01] (no `messages_en`), [15]; .claude/skills/gov-enforce-error-handling

### RULE-COMMON-002 — `SpecBuilder` يرفض الحقل والمعامل غير المدعومين / `SpecBuilder` rejects unknown fields and operators
Scope      : every search
Trigger    : on building the specification
Statement  : A filter naming a field outside `SetAllowedFields` is refused with 400 naming every offending field (`UNSUPPORTED_FILTER_FIELD` detail); a lifted id filter with an operator other than scalar `EQUALS` is refused (`UNSUPPORTED_FILTER_OPERATOR` detail); before 2026-09-19 such filters were silently skipped. Supported operators: `EQUALS`, `NOT_EQUALS`, `LIKE`, `GREATER_THAN`, `GREATER_THAN_OR_EQUAL`, `LESS_THAN`, `LESS_THAN_OR_EQUAL`, `IN`.
Data source: the endpoint's allowed-field set
Message    : ar: "هذا البحث لا يدعم التصفية على الحقل "{0}"" · en: "This search does not support filtering on "{0}""; ar: "هذا البحث لا يدعم المعامل {0} على الحقل "{1}"" · en: "This search does not support the {0} operator on "{1}""
Traces     : REQ-COMMON-009, REQ-COMMON-010
Source     : common/search/SpecBuilder.java:43-64; common/dto/BaseSearchContractRequest.java:94-128; `erp-core/src/main/resources/i18n/messages.properties:21-22`

### RULE-COMMON-003 — ترقيم افتراضي 20 وحد أقصى 200 ورفض الفيض / Default 20, maximum 200, overflow rejected
Scope      : every paged read
Trigger    : on building the pageable
Statement  : `page` < 0 becomes 0; `size` ≤ 0 becomes 20; `size` > 200 becomes 200; `page × size + size > Integer.MAX_VALUE` is refused with 400 `VALIDATION_ERROR` on `page` (not clamped); an unknown sort field means no sort.
Data source: `SearchRequest.page`, `.size`, `.sortField`
Message    : `VALIDATION_ERROR` with `fieldErrors[0].field = page`
Traces     : REQ-COMMON-011
Source     : common/search/PageableBuilder.java:15-16, :28-45; docs/DEVIATIONS.md [15] "Huge `page`"

### RULE-COMMON-004 — `StatusTransitions` جدول انتقالات / `StatusTransitions` is a transition table
Scope      : Domain objects with a status
Trigger    : on a status change
Statement  : A Domain holds a static `StatusTransitions(Map<from, Set<to>>, errorCode)`; `assertAllowed(from, to)` throws `BUSINESS_RULE_VIOLATION` with arguments `(from, to)` unless `to` is non-null and reachable from `from`; a missing or empty entry is terminal. Used by FILE (`FileDocumentDomain`) and NOTIF (`NotificationLogDomain`).
Data source: the Domain's table
Message    : the Domain's own code (422)
Traces     : REQ-COMMON-016
Source     : common/domain/StatusTransitions.java:9-33; file/domain/FileDocumentDomain.java; notif/domain/NotificationLogDomain.java

### RULE-COMMON-005 — `DomainRules` حارسان / `DomainRules` is two guards
Scope      : Domain objects
Trigger    : on create / update
Statement  : `assertUnique(alreadyTaken, code, args…)` throws `ALREADY_EXISTS` (409) when taken; `assertNotBlank(code, values…)` throws `VALIDATION_ERROR` (400) when any value is null or blank; used by 17 Domain objects across cu, file, mdl, notif, sec, sequence, tenant.
Data source: the Domain's facts
Message    : the Domain's own code
Traces     : REQ-COMMON-016
Source     : common/domain/DomainRules.java:17-30; consumers listed in `../P0/module-registry-common.md`

### RULE-COMMON-006 — `OwnedLookups` يخدم مفاتيحه فقط / `OwnedLookups` serves only the module's own keys
Scope      : module lookup endpoints (FILE, NOTIF)
Trigger    : on a lookup read
Statement  : The key is trimmed and upper-cased; a key outside `ownedKeys` → the module's 404 code with the raw key; a reader `NOT_FOUND` (an unseeded or deactivated MDL type) → the module's 404 code; any other exception propagates unchanged.
Data source: the module's owned keys; `MdlLookupApi.readActiveValuesByKey`
Message    : the module's own 404 code
Traces     : REQ-COMMON-017
Source     : common/lookup/OwnedLookups.java:32-46

### RULE-COMMON-007 — `FilterErrorResponseWriter` غلاف مكتوب يدويًا / `FilterErrorResponseWriter` is the hand-written envelope
Scope      : servlet filters (SEC `SecSecurityErrorHandler`, TENANT `TenantResolutionFilter`)
Trigger    : on a refusal before the dispatcher
Statement  : The writer sets the status, `application/json` and UTF-8, writes `{"success":false,"error":{"code","message"},"timestamp"}` with the message resolved from `request.getLocale()` (not `LocaleContextHolder`, not yet populated) and the code itself when unknown; quotes and backslashes in the message are escaped.
Data source: `MessageSource`, the request
Message    : the code's text
Traces     : REQ-COMMON-008
Source     : common/web/FilterErrorResponseWriter.java:12-46

### RULE-COMMON-008 — `InstantFieldValueConverter` ISO-8601 → `Instant` / ISO-8601 to `Instant`
Scope      : searches with instant fields (AUDIT `AuditEventService`, SEC `SecSearchSupport`)
Trigger    : on filter conversion
Statement  : For a listed field, a text value is parsed with `Instant.parse` after trimming; a malformed value is 400 `VALIDATION_ERROR`; null, an existing `Instant` and other fields pass through.
Data source: the filter value
Message    : `VALIDATION_ERROR`
Traces     : REQ-COMMON-012
Source     : common/search/InstantFieldValueConverter.java:17-36

### RULE-COMMON-009 — المساعدات الصغيرة / The small helpers
Scope      : every module
Trigger    : —
Statement  : `Strings.truncate(value, max)` cuts to `max` characters (null stays null); `UtcDates.startOfDay(date)` is midnight UTC; `PlainJson.MAPPER` is a default `JsonMapper` independent of the application's Jackson customisation (for JSON erp-core stores itself); `TokenHasher.sha256Hex(String | byte[])` is the only persisted form of an opaque token or a content checksum (lower-case hex). They entered `com.erp.common` in 1.3.0-SNAPSHOT (CHANGELOG [Unreleased]).
Data source: —
Message    : —
Traces     : REQ-COMMON-018 (shared helpers)
Source     : common/util/Strings.java:4-14; common/util/UtcDates.java:8-18; common/util/PlainJson.java:10-17; common/util/TokenHasher.java:14-35

### RULE-COMMON-010 — `ActiveFlagQueryHelper` بلا مستهلك / `ActiveFlagQueryHelper` has no consumer
Scope      : search helpers
Trigger    : —
Statement  : `ActiveFlagQueryHelper.isActive(root, cb, field, active)` (conjunction when `active` is null) exists in the package and is used by no class at main @ 19b19a4; it stays, because removing a public member of `com.erp.common` is a MAJOR change (POL-COMMON-007).
Data source: —
Message    : —
Traces     : REQ-COMMON-009
Source     : common/search/ActiveFlagQueryHelper.java:7-19; docs/RELEASE.md

## A6 — Lookups

**Status (ENT-less; `Status` enum)** — owned by COMMON — control type: Java enum
| Code | HTTP | Label (ar) | Label (en) |
|---|---|---|---|
| SUCCESS | 200 | نجاح | Success |
| CREATED | 201 | أُنشئ | Created |
| UPDATED | 200 | عُدِّل | Updated |
| NOT_FOUND | 404 | غير موجود | Not found |
| ALREADY_EXISTS | 409 | موجود مسبقًا | Already exists |
| CONFLICT | 409 | تعارض | Conflict |
| BUSINESS_RULE_VIOLATION | 422 | مخالفة قاعدة عمل | Business rule violation |
| VALIDATION_ERROR | 400 | خطأ تحقق | Validation error |
| PAYLOAD_TOO_LARGE | 413 | الحمولة كبيرة جدًا | Payload too large |
| UNSUPPORTED_MEDIA_TYPE | 415 | نوع وسائط غير مدعوم | Unsupported media type |
| UNAUTHORIZED | 401 | غير مصادَق | Unauthorized |
| FORBIDDEN | 403 | ممنوع | Forbidden |
| TOO_MANY_REQUESTS | 429 | طلبات كثيرة جدًا | Too many requests |
| INTERNAL_ERROR | 500 | خطأ داخلي | Internal error |
Source: common/domain/status/Status.java:7-20 (the same table every `docs/api-docs/<module>/index.md` prints as "Status -> HTTP Status Reference").

**SearchOperator** — common/search/SearchOperator.java:3-12: `EQUALS`, `NOT_EQUALS`, `LIKE`, `GREATER_THAN`,
`GREATER_THAN_OR_EQUAL`, `LESS_THAN`, `LESS_THAN_OR_EQUAL`, `IN`.

**realm** — common/util/SecurityContextHelper.java:11-18: `STAFF`, `CUSTOMER`, `SYSTEM` (see `../../EVENTS/P1/srs-events.md` A6).

Consumed lookups: none.

## A7 — Status lifecycle
Not applicable — the foundation owns no entity with a status; `StatusTransitions` is the mechanism the
owning Domains (FILE document, NOTIF log) parameterise with their own tables.

## A8 — Module dependencies

| Consumed entity | Owner ENT id | Owner module | HARD-FK / SOFT-READ | XM |
|---|---|---|---|---|
None — COMMON reads no table and imports no other `com.erp` package.

| Consumed surface | Owner | Kind | Source |
|---|---|---|---|
| `messageSource` bean (application bundles first, then `i18n/messages`, UTF-8, no system-locale fallback) | autoconfigure | wiring | docs/DEVIATIONS.md [03]; common/web/GlobalExceptionHandler.java:36 |
| `TenantIdentifierResolver` (resolves the `@TenantId` of ENT-COMMON-002 at session open) | tenant | Hibernate runtime binding, no Java import | tenant/config/TenantIdentifierResolver.java |

**Exposed direction** (consumers of the foundation)
| XM id | Exposed surface | Kind | Consumers | Source |
|---|---|---|---|---|
| XM-COMMON-001 | `com.erp.common.*` — envelope (`ApiResponse`, `ApiError`, `FieldErrorItem`, `OperationCode`, `GlobalExceptionHandler`, `FilterErrorResponseWriter`), exception (`LocalizedException`, `ErrorDetail`, `CommonErrorCodes`), status (`Status`, `ServiceResult`), search (`SearchRequest`, `SearchFilter`, `SearchOperator`, `SetAllowedFields`, converters, `SpecBuilder`, `PageableBuilder`, `BaseSearchContractRequest`), base entities (ENT-COMMON-001/002, `AuditEntityListener`), guards (`DomainRules`, `StatusTransitions`), lookups (`OwnedLookups`, `LookupOptionResponse`), converters, `SecurityContextHelper`, `Strings`, `UtcDates`, `PlainJson`, `TokenHasher` | public API of the library (additive only) | every core module and application; per-helper consumer lists in `../P0/module-registry-common.md` EXPOSED SURFACE | docs/RELEASE.md; governance/analysis/platform/PROJECT-OVERVIEW.md |

# PART B — SCREEN REQUIREMENTS
**No screen, no endpoint.** The foundation has no HTTP surface, no page code, no permission
(governance/analysis/platform/project-registry.md "Packages without an HTTP surface"). No SCR-REQ id
is minted. The nine common codes and the `Status` table appear on every module's api-docs page.

# STANDALONE

## Common error codes (HTTP status as the handler answers it)
| Code | HTTP | Raised by | Code location | Message (en / ar) |
|---|---|---|---|---|
| `VALIDATION_ERROR` | 400 | bean validation (`handleValidation`), malformed body (`handleMalformedRequestBody`, fixed English text), missing / mistyped parameter (`handleRequestParameter`), `SpecBuilder` / `BaseSearchContractRequest` / `PageableBuilder` / `InstantFieldValueConverter` / `DomainRules.assertNotBlank` (`Status.VALIDATION_ERROR`) | common/web/GlobalExceptionHandler.java:62-121; common/exception/CommonErrorCodes.java:9 | "Validation failed" / "فشل التحقق من البيانات" |
| `INTERNAL_ERROR` | 500 | the catch-all when no `LocalizedException` is wrapped | common/web/GlobalExceptionHandler.java:195-208; common/exception/CommonErrorCodes.java:10 | "An unexpected error occurred. Please try again later." / "حدث خطأ غير متوقع. يرجى المحاولة لاحقاً." |
| `ACCESS_DENIED` | 403 | `AccessDeniedException` (method security); `ReportRunDomain.assertCanRun` (`Status.FORBIDDEN`) | common/web/GlobalExceptionHandler.java:163-170; common/exception/CommonErrorCodes.java:11 | "You do not have permission to perform this operation" / "ليس لديك صلاحية لتنفيذ هذه العملية" |
| `DATA_INTEGRITY_VIOLATION` | 409 | `DataIntegrityViolationException` | common/web/GlobalExceptionHandler.java:142-150; common/exception/CommonErrorCodes.java:12 | "The request could not be completed because it violates a data constraint" / "تعذّر إتمام الطلب لأنه يخالف قيدًا على البيانات" |
| `METHOD_NOT_ALLOWED` | 405 (+ `Allow`) | `HttpRequestMethodNotSupportedException` (added 2026-09-12) | common/web/GlobalExceptionHandler.java:127-140; common/exception/CommonErrorCodes.java:14-18 | "The HTTP method is not supported for this resource" / "طريقة الطلب غير مدعومة لهذا المورد" |
| `UNSUPPORTED_FILTER_FIELD` | (detail of 400 `VALIDATION_ERROR`) | `SpecBuilder.assertFieldsAllowed` (added 2026-09-19) | common/search/SpecBuilder.java:54-64; common/exception/CommonErrorCodes.java:20-24 | "This search does not support filtering on "{0}"" / "هذا البحث لا يدعم التصفية على الحقل "{0}"" |
| `UNSUPPORTED_FILTER_OPERATOR` | (detail of 400 `VALIDATION_ERROR`) | `BaseSearchContractRequest.assertScalarEqualsFilter` | common/dto/BaseSearchContractRequest.java:114-128; common/exception/CommonErrorCodes.java:26-27 | "This search does not support the {0} operator on "{1}"" / "هذا البحث لا يدعم المعامل {0} على الحقل "{1}"" |
| `CONCURRENT_MODIFICATION` | 409 | `OptimisticLockingFailureException` (step 05) | common/web/GlobalExceptionHandler.java:152-161; common/exception/CommonErrorCodes.java:29-31 | "This record was changed by someone else in the meantime. Reload it and try again." / "عدّل مستخدم آخر هذا السجل في الأثناء. أعد تحميله ثم حاول مجددًا." |
| `NOT_FOUND` | 404 | `NoResourceFoundException` (1.2.0) | common/web/GlobalExceptionHandler.java:178-186; common/exception/CommonErrorCodes.java:33-35 | "The requested resource was not found" / "المورد المطلوب غير موجود" |
Messages: `erp-core/src/main/resources/i18n/messages.properties` lines 15–22 and 140, `messages_ar.properties`
lines 12–19 and 137. `Status` → HTTP: common/domain/status/Status.java:7-20 (A6).

## What `error.fieldErrors[].field` carries (per handler)
Read from `GlobalExceptionHandler.java`: the expression each handler passes to `FieldErrorItem.field(...)`.
The same slot is filled differently per handler, so a client must not assume it is always a request
field path — a row whose fallback branch is not a field path can put something else there.

| Handler | Exceptions | HTTP Status | Code | `field` holds | Source expression |
|---|---|---|---|---|---|
| handleLocalizedException | LocalizedException | the thrown Status's HTTP status | the thrown error code | `detail.field()` when `detail.field()` is non-null, otherwise `detail.errorCode()` | `detail.field() != null ? detail.field() : detail.errorCode()` |
| handleValidation | MethodArgumentNotValidException | 400 BAD_REQUEST | VALIDATION_ERROR | the value of `fe.getField()` | `fe.getField()` |
| handleRequestParameter | MissingServletRequestParameterException, MethodArgumentTypeMismatchException | 400 BAD_REQUEST | VALIDATION_ERROR | the value of `parameterName` | `parameterName` |
Source: `docs/api-docs/sec/index.md` "What `error.fieldErrors[].field` carries" (generated from the
handler); common/web/GlobalExceptionHandler.java:50-55, :64-69, :115-118. The other handlers set no
`fieldErrors`.

## Traceability matrix
| P0.5 | REQ | AC | RULE | ENT | SCR-REQ |
|---|---|---|---|---|---|
| US-COMMON-001 | REQ-COMMON-001, -003, -008 | AC-COMMON-001, -003, -008 | RULE-COMMON-001, -007 | — | — |
| US-COMMON-002 | REQ-COMMON-002, -003, -004, -006, -007 | AC-COMMON-002, -003, -004, -006, -007 | RULE-COMMON-001 | — | — |
| US-COMMON-003 | REQ-COMMON-009, -010, -011, -012 | AC-COMMON-009, -010, -011, -012 | RULE-COMMON-002, -003, -008, -010 | — | — |
| US-COMMON-004 | REQ-COMMON-005, -013, -014, -015, -018 | AC-COMMON-005, -013, -014, -015, -018 | — | ENT-COMMON-001, -002 | — |
| US-COMMON-005 | REQ-COMMON-002, -016, -017, -018 | AC-COMMON-002, -016, -017, -018 | RULE-COMMON-004, -005, -006, -009 | — | — |
| US-COMMON-006 | (policy only: POL-COMMON-007) | — | RULE-COMMON-010 | — | — |

Every story traces to ≥ 1 REQ or policy; every REQ has one AC; every RULE traces to a REQ. No orphan,
no dangling id.

## Decisions applied
| DEFAULT / ADR | What | Source | Status |
|---|---|---|---|
| ADR-COMMON-001 | `LocalizedException` with registered bilingual codes and the `Status` → HTTP envelope, including the filter-side writer | governance/analysis/decisions/COMMON/ADR-COMMON-001.md | ACCEPTED (as built) |
| ADR-COMMON-002 | `SpecBuilder` rejects unknown filter fields / operators with 400; page size capped at 200 | governance/analysis/decisions/COMMON/ADR-COMMON-002.md | ACCEPTED (as built) |
| ADR-COMMON-003 | base-entity hierarchy `GlobalAuditableEntity` / `AuditableEntity` with `@Version` and the global-entity allow-list | governance/analysis/decisions/COMMON/ADR-COMMON-003.md | ACCEPTED (as built) |

## Access summary
No page code, no permission (no screen, no endpoint).
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 01, 05, 15
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : shared helpers moved into `com.erp.common` (CHANGELOG [Unreleased], already on main); the idempotency mechanism of the tenant-maturity plan (C.4) and SEC's `PasswordPolicy` (D.1) are documented by the implementing run as they land on main
Statement      : This addendum records only what is already on main for 1.3.0; the plan packages' rows are written by the implementing run.

### 1. Endpoints
none on main (common has no HTTP surface).

### 2. Business rules
| Kind | Rule | Source |
|---|---|---|
| CHANGED (moved) | RULE-COMMON-004 (`StatusTransitions`), -005 (`DomainRules`), -006 (`OwnedLookups` + `LookupOptionResponse`), -007 (`FilterErrorResponseWriter`), -008 (`InstantFieldValueConverter`), -009 (`Strings`, `UtcDates`, `PlainJson`, `TokenHasher`) and the `SecurityContextHelper` members `currentCaller()` / `currentActorOrSystem()` / `currentRealm()` + `REALM_*` / `CUSTOMER_AUTHORITY` (REQ-COMMON-018): one copy in `com.erp.common` replaces copies that lived in two or more modules; `DomainEvent` and `AuditApi` alias the constants; no behaviour change | docs/CHANGELOG.md [Unreleased] Added |

### 3. Error codes / 4. Permissions / 5. Entities
none on main.

Plan packages B, C, D, E, G: documented by each package as it lands on main (analysis-first, written by the implementing run); the verified reference rows are in docs/plans/tenant-maturity-analysis-reference.md.
