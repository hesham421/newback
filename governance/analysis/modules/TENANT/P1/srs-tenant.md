# SRS — المستأجرون / Tenant (TENANT)
══════════════════════════════════════════════════════════════════
Module : TENANT   Version : v1 (as-built baseline)   Profile : erp
Inputs : prd-tenant, module-registry-tenant, business-policies-tenant; the code at main @ 2274f86 (erp-core 1.2.0 behaviour)
Counts : ENT 1 · REQ 23 · AC 23 · RULE 9 · SCR-REQ 1 · XM 2 · ADR 1
══════════════════════════════════════════════════════════════════

Written from the code, not before it. Every REQ / AC / RULE names the code location it was read from;
the id → location index is `registry-srs-tenant.md`. Java paths are relative to
`erp-core/src/main/java/com/erp/`, migrations to `erp-core/src/main/resources/db/migration/core/`,
`file:line` at main @ 2274f86. Test-case ids `TC-CORE-TENANT-NNN` are those of
`docs/test-api/core-test-plan.md`.

# PART A — MODULE FOUNDATION

## A1 — Document information
| Item | Value |
|---|---|
| Module | TENANT — المستأجرون / Tenant (package `com.erp.tenant`, registry module `PLATFORM`) |
| Feature code | TENANT |
| Version | v1 (as built in erp-core 1.2.0) |
| Date | 2026-10-07 |
| Status | AS-BUILT baseline (tenant-maturity plan package A) |
| Prepared by | analysis lane, from the code |
| Decisions applied count | 1 (ADR-TENANT-001) |

## A2 — Functional context

**In scope:** سجل المستأجرين (`CORE_TENANT`)، تجهيز مستأجر جديد مع مديره الأول عبر واجهة المنصة،
عرض المستأجرين والبحث فيهم، تعليق المستأجر وإعادة تفعيله، تحديد مستأجر كل طلب (المسار، الرمز،
الترويسة)، عزل البيانات على مستوى الصف عبر Hibernate `@TenantId`، سياق المستأجر للعمل خارج الطلب،
واجهة التجهيز (SPI) وواجهة قراءة رمز المستأجر للوحدات الأخرى.

**Out of scope:** تعديل اسم المستأجر أو حذفه، إحصاءات الاستخدام، بيانات الملف التعريفي، حقائق التعليق
(السبب/الوقت)، قطع الرموز عند إعادة التفعيل، أحداث دورة الحياة عدا الإنشاء، العلامة التجارية، المخطط لكل
مستأجر و RLS — [business-policies-tenant.md → SCOPE EXCEPTIONS; ADR-TENANT-001].

**Module function (one paragraph):** وحدة TENANT تجعل منصة واحدة بقاعدة بيانات واحدة تخدم مؤسسات كثيرة
معزولة تمامًا: تملك سجل المستأجرين، وتحدد مستأجر كل طلب قبل التفويض، وتسلّم Hibernate هذا المستأجر
ليقيّد به كل استعلام وكل إدراج، وتتيح لمشغّل المنصة وحده إنشاء المستأجرين وتعليقهم.

**Detailed description (workflow narrative, roles):** مشغّل المنصة (مستأجر `PLATFORM`، صلاحية
`PLATFORM_TENANT_MANAGE`) ينشئ مستأجرًا بمديره الأول؛ تجهّز كل وحدة بياناتها للمستأجر الجديد في المعاملة
نفسها؛ يسجّل مدير المستأجر الدخول بالترويسة `X-Tenant-Code` ويحمل رمزه المطالبة `tid`؛ كل طلب لاحق يعمل
داخل ذلك المستأجر وحده. يعلّق المشغّل المستأجر فيُرفض دخوله وكل طلباته فورًا، ويعيد تفعيله فتعود.

**Current situation:** built (erp-core plan step 05, extended by steps 07, 09, 15).

**General notes:** the tenant resolution order is fixed (table "Tenant resolution order" in STANDALONE);
the catalog permissions of module `PLATFORM` are effective only inside the PLATFORM tenant.

## A3 — Entities and fields

### ENT-TENANT-001 — المستأجر / Tenant
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| platform registry (global — the one core table that owns tenant data without a `TENANT_ID`) | SHARED (owner) — 22 tenant-scoped tables FK to it | No — `code` is the natural key | create, read, search, list, change status (activate / suspend); no update, no delete | `TENANT_ID` FK target of every module; `TenantLookupApi` (XM-TENANT-001) | tenant/entity/Tenant.java:32-95; V10__tenant_schema.sql:31-51 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| id | number | yes (system) | `SEQ_CORE_TENANT`; 1 = PLATFORM | primary key | معرّف المستأجر | Tenant id |
| code | text (3–32) | yes | `^[A-Z0-9_]{3,32}$`, unique (RULE-TENANT-001/002) | immutable (RULE-TENANT-003); sent as `X-Tenant-Code` | رمز المستأجر | Tenant code |
| nameAr | text (≤ 200) | yes | — | not editable through the API (no update endpoint) | اسم المستأجر (عربي) | Tenant name (Arabic) |
| nameEn | text (≤ 200) | yes | — | same | اسم المستأجر (إنجليزي) | Tenant name (English) |
| statusCode | code (≤ 20) | yes | `ACTIVE` \| `SUSPENDED` (RULE-TENANT-004) | default `ACTIVE`; drives A7 | الحالة | Status |
| createdBy, createdAt, updatedBy, updatedAt | system | createdBy/At yes | — | `GlobalAuditableEntity` | — | — |
| version | system | yes | optimistic lock | `GlobalAuditableEntity` `@Version` | — | — |
Source: tenant/entity/Tenant.java:42-66; common/domain/GlobalAuditableEntity.java:34-53; tenant/dto/TenantResponse.java:19-46.

## A4 — Functional requirements (EARS) and acceptance criteria

### REQ-TENANT-001 — تجهيز مستأجر / Provision a tenant
Pattern    : event
Statement  : When a platform operator submits a valid tenant-create request, the system shall create the tenant `ACTIVE`, run every provisioning contributor for it in the same transaction, and return the tenant.
Traces     : US-TENANT-001, US-TENANT-006
Entities   : ENT-TENANT-001
Rationale  : POL-TENANT-004, POL-TENANT-010
Source     : tenant/controller/PlatformTenantController.java:41-46; tenant/service/TenantService.java:68-103
Priority   : HIGH
#### AC-TENANT-001 — [REQ-TENANT-001]
Given an authenticated PLATFORM operator and a request with a free, well-formed code and a complete first administrator
When the operator posts it to `POST /api/v1/platform/tenants`
Then the system answers 201 with `statusCode = ACTIVE`, the new tenant holds copies of PLATFORM's four catalog roles `SYS_ADMIN`, `CU_ADMIN`, `NOTIF_ADMIN`, `FILE_ADMIN` with their grants (minus the `PLATFORM` module; other PLATFORM roles are not copied — sec/tenant/SecTenantProvisioningContributor.java:40, :80) and of its lookups, templates, channels and series, and its administrator can log in with `X-Tenant-Code` (TC-CORE-TENANT-005, -006, -012)

### REQ-TENANT-002 — رفض رمز غير صالح / Reject an invalid tenant code
Pattern    : unwanted
Statement  : If a create request carries a code that does not match `^[A-Z0-9_]{3,32}$` as entered, then the system shall reject it and create nothing.
Traces     : US-TENANT-001
Entities   : ENT-TENANT-001
Rationale  : POL-TENANT-001
Source     : tenant/domain/TenantDomain.java:34-36
Priority   : HIGH
#### AC-TENANT-002 — [REQ-TENANT-002]
Given a create request whose code is `AB`, `BAD-CODE` or lower-case `acme`
When the operator posts it
Then the system answers 400 `TENANT_CODE_INVALID` (ar: "رمز المستأجر ''{0}'' غير صالح…" · en: "Invalid tenant code ''{0}''…") and no tenant exists with that code (TC-CORE-TENANT-009)

### REQ-TENANT-003 — رفض رمز مكرر / Reject a duplicate tenant code
Pattern    : unwanted
Statement  : If a create request carries a code that an existing tenant already holds, then the system shall reject it, also when a concurrent request takes the code between the check and the insert.
Traces     : US-TENANT-001
Entities   : ENT-TENANT-001
Rationale  : POL-TENANT-001
Source     : tenant/service/TenantService.java:73-83; tenant/domain/TenantDomain.java:38
Priority   : HIGH
#### AC-TENANT-003 — [REQ-TENANT-003]
Given a tenant with code `ACME`
When the operator posts another create request with code `ACME`
Then the system answers 409 `TENANT_CODE_DUPLICATE` and nothing new is created (TC-CORE-TENANT-008)

### REQ-TENANT-004 — رفض طلب إنشاء ناقص / Reject an incomplete create request
Pattern    : unwanted
Statement  : If a create request lacks a required field or exceeds a field width (names ≤ 200, admin username ≤ 100, admin e-mail a valid address ≤ 255, admin password 8–200, admin full names ≤ 200), then the system shall reject it with the offending fields.
Traces     : US-TENANT-001
Entities   : ENT-TENANT-001
Rationale  : input validation before any rule runs
Source     : tenant/dto/TenantCreateRequest.java:25-63; tenant/controller/PlatformTenantController.java:44
Priority   : MEDIUM
#### AC-TENANT-004 — [REQ-TENANT-004]
Given an empty create body `{}`
When the operator posts it
Then the system answers 400 `VALIDATION_ERROR` whose `fieldErrors` name `code`, `nameAr`, `nameEn`, `adminUsername`, `adminEmail`, `adminPassword`, `adminFullNameAr`, `adminFullNameEn` (TC-CORE-TENANT-010)

### REQ-TENANT-005 — عرض قائمة المستأجرين / List tenants
Pattern    : event
Statement  : When a platform operator asks for the tenant list with a page and a size, the system shall return that page of all tenants in id order.
Traces     : US-TENANT-002
Entities   : ENT-TENANT-001
Rationale  : POL-TENANT-006
Source     : tenant/controller/PlatformTenantController.java:48-54; tenant/service/TenantService.java:133-139
Priority   : MEDIUM
#### AC-TENANT-005 — [REQ-TENANT-005]
Given the PLATFORM tenant and three provisioned tenants
When the operator calls `GET /api/v1/platform/tenants?page=0&size=200`
Then the system answers 200 with a page whose codes include `PLATFORM` and the three tenants (TC-CORE-TENANT-011); defaults are page 0, size 20, maximum size 200

### REQ-TENANT-006 — قراءة مستأجر / Read a tenant
Pattern    : event
Statement  : When a platform operator asks for a tenant by id, the system shall return it; if no tenant has that id, the system shall answer not found.
Traces     : US-TENANT-002
Entities   : ENT-TENANT-001
Rationale  : POL-TENANT-006
Source     : tenant/controller/PlatformTenantController.java:56-60; tenant/service/TenantService.java:105-114
Priority   : MEDIUM
#### AC-TENANT-006 — [REQ-TENANT-006]
Given an existing tenant id and the id 999999999 that does not exist
When the operator calls `GET /api/v1/platform/tenants/{id}` with each
Then the system answers 200 with the tenant, then 404 `TENANT_NOT_FOUND` (TC-CORE-TENANT-011)

### REQ-TENANT-007 — البحث في المستأجرين / Search tenants
Pattern    : event
Statement  : When a platform operator submits a search, the system shall return the matching page, filtering and sorting only on `id`, `code`, `nameAr`, `nameEn`, `statusCode`, `createdAt`.
Traces     : US-TENANT-002
Entities   : ENT-TENANT-001
Rationale  : shared search contract (`BaseSearchContractRequest`)
Source     : tenant/controller/PlatformTenantController.java:62-67; tenant/service/TenantService.java:59-61, :116-131; tenant/dto/TenantSearchRequest.java:19
Priority   : MEDIUM
#### AC-TENANT-007 — [REQ-TENANT-007]
Given provisioned tenants
When the operator posts `{"filters":[{"field":"code","operator":"EQUALS","value":"<code>"}]}` to `POST /api/v1/platform/tenants/search`
Then the system answers 200 with exactly that tenant (TC-CORE-TENANT-011)

### REQ-TENANT-008 — تغيير حالة المستأجر / Change a tenant's status
Pattern    : event
Statement  : When a platform operator sets a tenant's status to `ACTIVE` or `SUSPENDED`, the system shall apply it (re-applying the current status is allowed) and return the tenant; any other value is rejected, and an unknown id answers not found.
Traces     : US-TENANT-003
Entities   : ENT-TENANT-001
Rationale  : POL-TENANT-002
Source     : tenant/controller/PlatformTenantController.java:69-75; tenant/service/TenantService.java:141-160; tenant/dto/TenantStatusUpdateRequest.java:19-22
Priority   : HIGH
#### AC-TENANT-008 — [REQ-TENANT-008]
Given an ACTIVE tenant C
When the operator patches `{"statusCode":"SUSPENDED"}` to `/api/v1/platform/tenants/{C}/status`, later `{"statusCode":"ACTIVE"}`; and patches `{"statusCode":"BOGUS"}`; and patches an unknown id
Then the system answers 200 `SUSPENDED`, then 200 `ACTIVE` and C's administrator can log in again; 400 `VALIDATION_ERROR`; 404 `TENANT_NOT_FOUND` (TC-CORE-TENANT-019, -020, -024)

### REQ-TENANT-009 — حماية مستأجر المنصة / Protect the PLATFORM tenant
Pattern    : unwanted
Statement  : If a status change asks to suspend the PLATFORM tenant, then the system shall refuse it and leave PLATFORM `ACTIVE`.
Traces     : US-TENANT-003
Entities   : ENT-TENANT-001
Rationale  : POL-TENANT-003
Source     : tenant/domain/TenantDomain.java:53-59; tenant/service/TenantService.java:149
Priority   : HIGH
#### AC-TENANT-009 — [REQ-TENANT-009]
Given the PLATFORM tenant (id 1)
When the operator patches `{"statusCode":"SUSPENDED"}` to `/api/v1/platform/tenants/1/status`
Then the system answers 422 `TENANT_PLATFORM_PROTECTED` and PLATFORM stays `ACTIVE` (TC-CORE-TENANT-019)

### REQ-TENANT-010 — رفض طلبات المستأجر المعلّق / Refuse a suspended tenant's requests
Pattern    : state
Statement  : While a tenant is `SUSPENDED`, the system shall refuse every request resolved to it — by token, by header or by path — before authorization runs.
Traces     : US-TENANT-003, US-TENANT-004
Entities   : ENT-TENANT-001
Rationale  : POL-TENANT-002
Source     : tenant/security/TenantResolutionFilter.java:89-99, :109-112, :139-142
Priority   : HIGH
#### AC-TENANT-010 — [REQ-TENANT-010]
Given tenant C suspended, a staff token and a customer token of C issued before the suspension
When C's administrator logs in, the staff token calls `GET /api/v1/sec/menu`, the customer token calls `GET /api/v1/customers/me`, and an anonymous caller requests `/api/v1/public/files/C/anything`
Then each answer is 403 `TENANT_SUSPENDED` (TC-CORE-TENANT-021, -022, -023, -026)

### REQ-TENANT-011 — المستأجر من المسار / Tenant from the path
Pattern    : optional
Statement  : Where a request path matches `erp.core.tenant.path-tenant-paths` (default `/api/v1/public/files/{tenantCode}/**`, customer chain only), the system shall take the tenant from the `{tenantCode}` variable (trimmed, upper-cased), ahead of token and header, and treat a caller authenticated in another tenant as anonymous.
Traces     : US-TENANT-005
Entities   : ENT-TENANT-001
Rationale  : POL-TENANT-008, POL-TENANT-009 — public URLs must work without header or token
Source     : tenant/security/TenantResolutionFilter.java:83-87, :131-171; autoconfigure/ErpCoreProperties.java:252-261; autoconfigure/ErpCoreSecurityAutoConfiguration.java:123-125, :169-171
Priority   : MEDIUM
#### AC-TENANT-011 — [REQ-TENANT-011]
Given a public file of tenant A and a token of tenant B
When the file is requested at `/api/v1/public/files/A/<slug>` anonymously, with B's token, and at `/api/v1/public/files/NOPE/<slug>`
Then A's file is served in the first two cases (B's authentication dropped) and the third answers 404 `TENANT_NOT_FOUND`

### REQ-TENANT-012 — المستأجر من رمز الدخول / Tenant from the access token
Pattern    : event
Statement  : When a request carries a valid access token, the system shall make the token's `tid` claim the request tenant before the user is looked up, ignore any `X-Tenant-Code` header, and refuse the request when that tenant is missing or not `ACTIVE`.
Traces     : US-TENANT-004
Entities   : ENT-TENANT-001
Rationale  : POL-TENANT-009
Source     : sec/security/JwtAuthenticationFilter.java:129-136; tenant/security/TenantResolutionFilter.java:89-99
Priority   : HIGH
#### AC-TENANT-012 — [REQ-TENANT-012]
Given a token of tenant A
When it searches users with the header `X-Tenant-Code: <B>`
Then the system answers 200 with A's users only (TC-CORE-TENANT-016); a token without `tid` does not authenticate

### REQ-TENANT-013 — المستأجر من الترويسة / Tenant from the `X-Tenant-Code` header
Pattern    : event
Statement  : When a request carries no token tenant and an `X-Tenant-Code` header, the system shall resolve the trimmed, upper-cased code; an unknown code is refused not found, a suspended tenant is refused forbidden, otherwise the tenant is set for the request and cleared afterwards.
Traces     : US-TENANT-004
Entities   : ENT-TENANT-001
Rationale  : POL-TENANT-008 — login, sign-up and reset run before any token exists
Source     : tenant/security/TenantResolutionFilter.java:101-120; tenant/TenantConstants.java:17
Priority   : HIGH
#### AC-TENANT-013 — [REQ-TENANT-013]
Given the PLATFORM tenant
When the admin logs in with `X-Tenant-Code: " platform "`, and again with `X-Tenant-Code: NOPE_x`
Then the first login answers 200 with a token, the second 404 `TENANT_NOT_FOUND` (TC-CORE-TENANT-002, -003); a user of A cannot log in with B's header (401, TC-CORE-TENANT-018)

### REQ-TENANT-014 — طلب بلا مستأجر / Request without a tenant
Pattern    : unwanted
Statement  : If a request has no path, token or header tenant, then the system shall let it proceed on an exempt path (`erp.core.tenant.exempt-paths`), refuse it on any other public path, and let the authorization layer answer any protected path.
Traces     : US-TENANT-004
Entities   : —
Rationale  : POL-TENANT-008
Source     : tenant/security/TenantResolutionFilter.java:122-127; autoconfigure/ErpCoreProperties.java:233-246
Priority   : HIGH
#### AC-TENANT-014 — [REQ-TENANT-014]
Given no token and no header
When the caller posts staff login, sign-up or password-reset request; calls `/actuator/health`; calls `GET /api/v1/sec/menu`
Then the first three answer 400 `TENANT_REQUIRED` (TC-CORE-TENANT-001, -004), the exempt path proceeds, and the protected path answers 401 `SEC-401-INVALID-CREDENTIALS`

### REQ-TENANT-015 — واجهة المنصة لمشغّليها فقط / Platform API for platform operators only
Pattern    : ubiquitous
Statement  : The system shall serve `/api/v1/platform/**` only to an authenticated caller whose request tenant is PLATFORM and who holds `PLATFORM_TENANT_MANAGE`, and the tenant service shall re-check that authority on every operation.
Traces     : US-TENANT-001, US-TENANT-002, US-TENANT-003
Entities   : ENT-TENANT-001
Rationale  : POL-TENANT-006
Source     : autoconfigure/ErpCoreSecurityAutoConfiguration.java:85-88, :130-131, :199-205; tenant/service/TenantService.java:69, :106, :117, :135, :142
Priority   : HIGH
#### AC-TENANT-015 — [REQ-TENANT-015]
Given tenant A's `SYS_ADMIN` token (a super role) and no token at all
When each calls `GET /api/v1/platform/tenants`
Then A's token answers 403 `SEC-403-FORBIDDEN` (the `PLATFORM` module authority is never effective outside PLATFORM) and the anonymous call answers 401 `SEC-401-INVALID-CREDENTIALS`; a PLATFORM user without the authority answers 403 `SEC-403-FORBIDDEN` and a customer token 403 `REALM_MISMATCH` (TC-CORE-PLATFORM-001…004)

### REQ-TENANT-016 — عزل على مستوى الصف / Row-level isolation
Pattern    : ubiquitous
Statement  : The system shall restrict every entity query, join and load by id to the request tenant, set `TENANT_ID` from the request tenant on every insert, and never let a write change it.
Traces     : US-TENANT-004
Entities   : ENT-TENANT-001 (FK target); every tenant-scoped entity
Rationale  : POL-TENANT-007
Source     : common/domain/AuditableEntity.java:35-37; tenant/config/TenantIdentifierResolver.java:36-47
Priority   : HIGH
#### AC-TENANT-016 — [REQ-TENANT-016]
Given users, roles, files, templates and lookups in tenants A and B
When A searches them and reads B's user by id
Then A's searches contain none of B's rows and B's id answers 404 (TC-CORE-TENANT-014, -015, -025; `TenantScopedQueryIntegrationTest.specBuilderSearch_findById_andCount_neverCrossTheTenant`, `.tenantIdIsAssignedFromTheSession_andIsNotUpdatable`)

### REQ-TENANT-017 — الفشل السريع بلا سياق مستأجر / Fail fast without a tenant context
Pattern    : unwanted
Statement  : If code opens a database session without a tenant after the application has started, then the system shall fail with `TENANT_CONTEXT_MISSING`; before the web server accepts requests, sessions opened during bootstrap without a tenant shall match no row.
Traces     : US-TENANT-008
Entities   : —
Rationale  : POL-TENANT-007
Source     : tenant/TenantContext.java:47-53; tenant/config/TenantIdentifierResolver.java:31, :36-42; tenant/config/TenantHibernateConfiguration.java:34, :50-52; common/web/GlobalExceptionHandler.java:195-201
Priority   : HIGH
#### AC-TENANT-017 — [REQ-TENANT-017]
Given a started application
When code without `runAs` accesses a tenant-scoped repository, directly or wrapped in a transaction-creation exception
Then it fails with `TENANT_CONTEXT_MISSING`, answered 500 with that code over HTTP (`TenantContextIntegrationTest.runAs_onAThreadWithoutARequest_seesExactlyThatTenant_andWithoutItFailsFast`; `TenantBootstrapWindowIntegrationTest.theResolverIsStrictBeforeTheWebServerAcceptsRequests`)

### REQ-TENANT-018 — العمل كمستأجر خارج الطلب / Run as a tenant outside a request
Pattern    : optional
Statement  : Where system code runs outside a request, the system shall let it run as a given tenant through `TenantContext.runAs` / `callAs` and restore the previous tenant (or none) afterwards, also when nested or failing.
Traces     : US-TENANT-008
Entities   : —
Rationale  : POL-TENANT-007
Source     : tenant/TenantContext.java:71-92
Priority   : HIGH
#### AC-TENANT-018 — [REQ-TENANT-018]
Given a thread with no tenant
When it calls `runAs(A, …)` around a transactional read
Then the read sees exactly A's rows and the thread has no tenant afterwards (`TenantContextTest.runAs_andCallAs_setTheTenant_andRestoreThePreviousOne_evenWhenNestedOrFailing`)

### REQ-TENANT-019 — قراءة رمز المستأجر عبر الوحدات / Cross-module read of a tenant code
Pattern    : optional
Statement  : Where another module holds a tenant id, the system shall return that tenant's code — and nothing else — through `TenantLookupApi.codeOf`, empty for an unknown or null id.
Traces     : US-TENANT-007
Entities   : ENT-TENANT-001
Rationale  : public file URLs and the `{TENANT}` number-series token need the code
Source     : tenant/crossmodule/TenantLookupApi.java:10-14; tenant/crossmodule/TenantLookupApiImpl.java:21-25
Priority   : LOW
Note       : XM-TENANT-001 (registry-srs-tenant.md). No `@PreAuthorize`: a tenant code is not secret.
#### AC-TENANT-019 — [REQ-TENANT-019]
Given tenant A with code `ACME`
When FILE builds a public URL of A's document
Then the URL path carries `/api/v1/public/files/ACME/…`; `codeOf(null)` and `codeOf(<unknown>)` are empty

### REQ-TENANT-020 — عقد واجهة التجهيز / Provisioning SPI contract
Pattern    : optional
Statement  : Where a module implements `TenantProvisioningContributor`, the system shall call it for every new tenant, ordered by `order()` (lower first), inside the transaction that inserted the tenant, with the new tenant id, the source tenant (PLATFORM), the operator and the first administrator.
Traces     : US-TENANT-006
Entities   : ENT-TENANT-001
Rationale  : POL-TENANT-004, POL-TENANT-010
Source     : tenant/TenantProvisioningContributor.java:22-31; tenant/TenantProvisioning.java:14-28; tenant/service/TenantService.java:85-95
Priority   : HIGH
Note       : XM-TENANT-002. Core implementers: SEC 0, MDL 10, NOTIF 20, SEQUENCE 40.
#### AC-TENANT-020 — [REQ-TENANT-020]
Given the four core contributors
When a tenant is provisioned
Then SEC runs before MDL, MDL before NOTIF, NOTIF before SEQUENCE, and a failure in any of them leaves no `CORE_TENANT` row (one transaction)

### REQ-TENANT-021 — حدث إنشاء المستأجر / Tenant-created event
Pattern    : event
Statement  : When a tenant has been provisioned, the system shall publish `TenantCreatedEvent` whose tenant is the new tenant and whose actor is the platform operator.
Traces     : US-TENANT-001
Entities   : ENT-TENANT-001
Rationale  : applications seed their own data for new tenants from an `AFTER_COMMIT` listener
Source     : tenant/service/TenantService.java:98-100; events/TenantCreatedEvent.java:11-18
Priority   : MEDIUM
#### AC-TENANT-021 — [REQ-TENANT-021]
Given a listener on `TenantCreatedEvent`
When a tenant is created and the transaction commits
Then the listener receives one event with `tenantId` = the new tenant id and `tenantCode` = its code

### REQ-TENANT-022 — تدقيق تغييرات المستأجر / Audit tenant changes
Pattern    : event
Statement  : When a tenant row is inserted or updated, the system shall record the change field by field in the platform audit log under entity type `CORE_TENANT`.
Traces     : US-TENANT-001, US-TENANT-003
Entities   : ENT-TENANT-001
Rationale  : "who changed what, when" for platform operations
Source     : tenant/entity/Tenant.java:33
Priority   : MEDIUM
#### AC-TENANT-022 — [REQ-TENANT-022]
Given a tenant suspended by operator `admin`
When the audit log of the PLATFORM tenant is queried for entity type `CORE_TENANT`
Then it holds an `UPDATE` entry with `statusCode` ACTIVE → SUSPENDED and actor `admin`

### REQ-TENANT-023 — تنظيف مستأجر متسرّب / Clear a leaked tenant at request start
Pattern    : unwanted
Statement  : If a tenant is already set on the thread when a request arrives, then the system shall log a warning and clear it before anything else runs, so the request never uses it.
Traces     : US-TENANT-004
Entities   : —
Rationale  : POL-TENANT-007 — pooled worker threads must not carry a tenant from earlier work
Source     : sec/security/JwtAuthenticationFilter.java:85-91
Priority   : MEDIUM
Note       : implemented in SEC's `JwtAuthenticationFilter` (the first tenant-aware filter of both chains), erp-core 1.2.0 (DEVIATIONS [15]).
#### AC-TENANT-023 — [REQ-TENANT-023]
Given a worker thread left with tenant 7
When a request without a token arrives on it
Then the request runs with no tenant (or its own) and a WARN names the leaked tenant

## A5 — Business rules

### RULE-TENANT-001 — صيغة رمز المستأجر / Tenant code format
Scope      : ENT-TENANT-001
Trigger    : on create
Statement  : The system shall accept a tenant code only if it matches `^[A-Z0-9_]{3,32}$` exactly as entered (a lower-case code is refused, never upper-cased for the client).
Data source: the request's `code`
Message    : ar: "رمز المستأجر ''{0}'' غير صالح: استخدم من 3 إلى 32 حرفًا لاتينيًا كبيرًا أو رقمًا أو شرطة سفلية" · en: "Invalid tenant code ''{0}'': use 3 to 32 upper-case letters, digits or underscores"
Traces     : REQ-TENANT-002
Source     : tenant/domain/TenantDomain.java:18, :34-36; V10__tenant_schema.sql:51 (`CHK_CORE_TENANT_CODE`)

### RULE-TENANT-002 — تفرّد رمز المستأجر / Tenant code uniqueness
Scope      : ENT-TENANT-001
Trigger    : on create
Statement  : The system shall prevent two tenants from holding the same code.
Data source: ENT-TENANT-001 (existing codes); `UQ_CORE_TENANT_CODE` for the race
Message    : ar: "يوجد مستأجر بالرمز ''{0}'' مسبقًا" · en: "A tenant with code ''{0}'' already exists"
Traces     : REQ-TENANT-003
Source     : tenant/domain/TenantDomain.java:38; tenant/service/TenantService.java:73, :79-83; V10__tenant_schema.sql:49

### RULE-TENANT-003 — ثبات رمز المستأجر / Tenant code immutability
Scope      : ENT-TENANT-001
Trigger    : on update
Statement  : The system shall never change a tenant's code after creation.
Data source: —
Message    : — (no operation accepts a code change)
Traces     : REQ-TENANT-001, REQ-TENANT-008
Source     : tenant/entity/Tenant.java:50 (`updatable = false`); tenant/mapper/TenantMapper.java:9-11 (no update mapping); tenant/controller/PlatformTenantController.java:28-30

### RULE-TENANT-004 — قيم حالة المستأجر / Tenant status values
Scope      : ENT-TENANT-001
Trigger    : on status change
Statement  : The system shall accept only `ACTIVE` or `SUSPENDED` as a tenant status; re-applying the current status is allowed and changes nothing.
Data source: the request's `statusCode`; `CHK_CORE_TENANT_STATUS`
Message    : the shared `VALIDATION_ERROR` (pattern `ACTIVE|SUSPENDED`)
Traces     : REQ-TENANT-008
Source     : tenant/dto/TenantStatusUpdateRequest.java:19-22; tenant/domain/TenantDomain.java:47-52; V10__tenant_schema.sql:50

### RULE-TENANT-005 — مستأجر المنصة لا يُعلَّق / PLATFORM cannot be suspended
Scope      : ENT-TENANT-001
Trigger    : on status change to SUSPENDED
Statement  : The system shall prevent suspending the tenant with id 1 (PLATFORM).
Data source: ENT-TENANT-001.id
Message    : ar: "لا يمكن تعليق مستأجر المنصة ''{0}''" · en: "The platform tenant ''{0}'' cannot be suspended"
Traces     : REQ-TENANT-009
Source     : tenant/domain/TenantDomain.java:53-59

### RULE-TENANT-006 — المستأجر المعلّق لا يُخدَم / A suspended tenant is not served
Scope      : ENT-TENANT-001
Trigger    : on every request resolved to the tenant
Statement  : The system shall refuse a request whose resolved tenant is not `ACTIVE` (or, for a token tenant, no longer exists).
Data source: ENT-TENANT-001.statusCode, read as PLATFORM (`TenantContext.callAs`)
Message    : ar: "هذا المستأجر معلّق" · en: "This tenant is suspended"
Traces     : REQ-TENANT-010, REQ-TENANT-012
Source     : tenant/security/TenantResolutionFilter.java:89-99, :109-112, :139-142, :173-179

### RULE-TENANT-007 — صلاحيات وحدة PLATFORM لا تغادر مستأجر المنصة / PLATFORM-module permissions never leave PLATFORM
Scope      : ENT-TENANT-001 (provisioning); SEC grants of new tenants
Trigger    : on provisioning; on authority resolution of a super role
Statement  : The system shall copy no grant under registry module `PLATFORM` (and never `PLATFORM_TENANT_MANAGE`) into a new tenant, and shall give a super role outside PLATFORM no `PLATFORM`-module authority.
Data source: SEC_MODULE_REG / SEC_ACTION_REG (global catalog)
Message    : — (enforced by omission)
Traces     : REQ-TENANT-001, REQ-TENANT-015
Source     : sec/tenant/SecTenantProvisioningContributor.java:46-53, :84-121; sec/service/MenuService.java:142-143

### RULE-TENANT-008 — كتابات التجهيز تسمّي TENANT_ID صراحة / Provisioning writes name TENANT_ID explicitly
Scope      : every tenant-scoped table written by a contributor
Trigger    : on provisioning
Statement  : The system shall write the new tenant's rows with explicit SQL naming `TENANT_ID` in every statement — the new tenant on inserts, the source tenant on reads — never through tenant-aware JPA entities.
Data source: the tables of the contributing module
Message    : —
Traces     : REQ-TENANT-020
Source     : tenant/TenantProvisioningContributor.java:14-17; sec/tenant/SecTenantProvisioningContributor.java:29-32

### RULE-TENANT-009 — الترويسة والمسار لا يبدّلان مستأجر الرمز / Header and path never switch a token's tenant
Scope      : request tenant
Trigger    : on every authenticated request
Statement  : The system shall ignore `X-Tenant-Code` when the token names a tenant, and shall drop a token's authentication on a path-tenant path that names another tenant.
Data source: token claim `tid`; header; path variable
Message    : —
Traces     : REQ-TENANT-011, REQ-TENANT-012
Source     : tenant/security/TenantResolutionFilter.java:89-99, :143-147

## A6 — Lookups

**STATUS_CODE of ENT-TENANT-001** — owned by TENANT — control type: fixed value set (CHECK constraint, not an MDL lookup)
| Code | Label (ar) | Label (en) |
|---|---|---|
| ACTIVE | نشط | Active |
| SUSPENDED | معلّق | Suspended |
Source: V10__tenant_schema.sql:46, :50; tenant/TenantConstants.java:23, :26.

Consumed lookups: none.

## A7 — Status lifecycle

**ENT-TENANT-001 Tenant.statusCode (2 states)**
```
(create, REQ-TENANT-001) ──────────────────────▶ ACTIVE
ACTIVE    --(REQ-TENANT-008, operator suspends)--> SUSPENDED   (refused for PLATFORM, RULE-TENANT-005)
SUSPENDED --(REQ-TENANT-008, operator activates)-> ACTIVE
```
Re-applying the current status is a no-op transition (RULE-TENANT-004). There is no terminal state and
no delete (POL-TENANT-005). Source: tenant/entity/Tenant.java:87-94; tenant/service/TenantService.java:149-155.

## A8 — Module dependencies

| Consumed entity | Owner ENT id | Owner module | HARD-FK / SOFT-READ | XM |
|---|---|---|---|---|
None — TENANT reads no other module's table.

| Consumed surface | Owner | Kind | Source |
|---|---|---|---|
| `PermissionContributor` | SEC | SPI implemented by `TenantPermissions` | tenant/permission/TenantPermissions.java:18 |
| `DomainEventPublisher` | events | publishes `TenantCreatedEvent` | tenant/service/TenantService.java:66, :99 |
| `@Audited` | audit | entity listener on `CORE_TENANT` | tenant/entity/Tenant.java:33 |

**Exposed direction** (consumers of the tenant module)
| XM id | Exposed surface | Kind | Consumers | Source |
|---|---|---|---|---|
| XM-TENANT-001 | `com.erp.tenant.crossmodule.TenantLookupApi` — `Optional<String> codeOf(Long tenantId)` | crossmodule read (plain value, never the entity) | FILE `PublicFileUrls`; SEQUENCE `NumberAllocationService` | tenant/crossmodule/TenantLookupApi.java:10-14; file/service/PublicFileUrls.java:34; sequence/service/NumberAllocationService.java:52 |
| XM-TENANT-002 | `com.erp.tenant.TenantProvisioningContributor` (`order()`, `provision(TenantProvisioning)`) | SPI | SEC, MDL, NOTIF, SEQUENCE; applications | tenant/TenantProvisioningContributor.java:22-31 |
| — | `TenantContext`, `TenantConstants` | root-package public API | every module, `TenantAndSecurityContextTaskDecorator`, applications | tenant/TenantContext.java:25; events/support/TenantAndSecurityContextTaskDecorator.java:19 |
| — | `CORE_TENANT(ID)` | HARD FK target of 22 `TENANT_ID` columns | every core module | `../P2/db-script-tenant.md` (DBF-TENANT-011…032) |

# PART B — SCREEN REQUIREMENTS

## SCR-REQ-TENANT-001 — المستأجرون / Tenants
### B1 — Definition
Purpose      : إدارة المستأجرين من المنصة: إنشاء مستأجر بمديره الأول، عرضه والبحث فيه، تعليقه وإعادة تفعيله.
Entities     : ENT-TENANT-001
Operations   : search, list, read, create (provision), change status (activate / suspend)
Users        : مشغّل المنصة (مستأجر PLATFORM)
Navigation   : PLATFORM → Tenants (registry module `PLATFORM`)
Content shape: flat record (search list + create form + status action)
Traces       : REQ-TENANT-001…009, REQ-TENANT-015
Composite    : Search + Entry = ONE screen requirement
### B2 — Search / list
Filters and sort: `id`, `code`, `nameAr`, `nameEn`, `statusCode`, `createdAt` (tenant/service/TenantService.java:59-61).
### B3 — Input
Create: `code`, `nameAr`, `nameEn`, `adminUsername`, `adminEmail`, `adminPassword`, `adminFullNameAr`,
`adminFullNameEn` (tenant/dto/TenantCreateRequest.java:25-63). Status: `statusCode` ACTIVE | SUSPENDED.
No edit form (code immutable, names not editable), no delete.
### B4 — Access
Page code: `PLATFORM_TENANTS`. Actions: `VIEW` (`PERM_PLATFORM_TENANTS_VIEW`, gateway / menu entry only) and
`MANAGE` (`PLATFORM_TENANT_MANAGE`, required by every endpoint below, reads included). Effective only in
the PLATFORM tenant.
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | Permission | RULEs | Errors | Traces (REQ) |
|---|---|---|---|---|---|---|---|---|
| provision tenant | POST | /api/v1/platform/tenants | `TenantCreateRequest` | 201 `TenantResponse` | `PLATFORM_TENANT_MANAGE` | RULE-TENANT-001, -002, -007, -008 | 400 `VALIDATION_ERROR`, 400 `TENANT_CODE_INVALID`, 409 `TENANT_CODE_DUPLICATE`, 500 `INTERNAL_ERROR` (PLATFORM has no `SYS_ADMIN`) | REQ-TENANT-001…004, -020, -021 |
| list tenants | GET | /api/v1/platform/tenants?page&size | page (0), size (20, ≤ 200) | 200 `Page<TenantResponse>` | `PLATFORM_TENANT_MANAGE` | — | 400 `VALIDATION_ERROR` | REQ-TENANT-005 |
| read tenant | GET | /api/v1/platform/tenants/{id} | id | 200 `TenantResponse` | `PLATFORM_TENANT_MANAGE` | — | 404 `TENANT_NOT_FOUND` | REQ-TENANT-006 |
| search tenants | POST | /api/v1/platform/tenants/search | `TenantSearchRequest` (filters, sort, page, size) | 200 `Page<TenantResponse>` | `PLATFORM_TENANT_MANAGE` | — | 400 `VALIDATION_ERROR` | REQ-TENANT-007 |
| change status | PATCH | /api/v1/platform/tenants/{id}/status | `TenantStatusUpdateRequest` (`statusCode`) | 200 `TenantResponse` | `PLATFORM_TENANT_MANAGE` | RULE-TENANT-004, -005 | 400 `VALIDATION_ERROR`, 404 `TENANT_NOT_FOUND`, 422 `TENANT_PLATFORM_PROTECTED`, 409 `CONCURRENT_MODIFICATION` | REQ-TENANT-008, -009 |
Every path additionally answers 401 `SEC-401-INVALID-CREDENTIALS` without a token and 403 `SEC-403-FORBIDDEN`
for a caller who is not a PLATFORM operator (REQ-TENANT-015). Source: tenant/controller/PlatformTenantController.java:33-75;
`docs/api-docs/tenant/endpoints/platform-tenants.md`.

# STANDALONE

## Tenant resolution order (per request, both security chains)
| # | Source | Applies when | Outcome | Error | Code location |
|---|---|---|---|---|---|
| 1 | path variable `{tenantCode}` | path matches `erp.core.tenant.path-tenant-paths` (customer chain only; default `/api/v1/public/files/{tenantCode}/**`) | trimmed, upper-cased code resolved; wins over token and header; a token of another tenant is dropped | unknown 404 `TENANT_NOT_FOUND`; suspended 403 `TENANT_SUSPENDED` | tenant/security/TenantResolutionFilter.java:83-87, :131-158 |
| 2 | token claim `tid` | a valid token (set by `JwtAuthenticationFilter` before the user lookup) | the token's tenant; any header is ignored | tenant missing or not ACTIVE 403 `TENANT_SUSPENDED` | sec/security/JwtAuthenticationFilter.java:129-136; tenant/security/TenantResolutionFilter.java:89-99 |
| 3 | header `X-Tenant-Code` | no token tenant | trimmed, upper-cased code resolved; set for the request, cleared in `finally` | unknown 404 `TENANT_NOT_FOUND`; suspended 403 `TENANT_SUSPENDED` | tenant/security/TenantResolutionFilter.java:101-120 |
| 4 | none | — | exempt path (`/actuator/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/error`, `/api/v1/platform/**`) proceeds; another public path is refused; a protected path proceeds to authorization (401) | 400 `TENANT_REQUIRED` | tenant/security/TenantResolutionFilter.java:122-127; autoconfigure/ErpCoreProperties.java:233-239 |
`CORE_TENANT` lookups of the filter run as PLATFORM (tenant/security/TenantResolutionFilter.java:173-175).

## Error codes (HTTP status as the code has it)
| Code | HTTP | Raised by | Code location | Message (en / ar) |
|---|---|---|---|---|
| `TENANT_REQUIRED` | 400 | `TenantResolutionFilter` (written by the filter, not thrown) | tenant/security/TenantResolutionFilter.java:124; tenant/exception/TenantErrorCodes.java:14 | "A tenant is required: send the X-Tenant-Code header" / "يلزم تحديد المستأجر: أرسل الترويسة X-Tenant-Code" |
| `TENANT_NOT_FOUND` | 404 | filter (header, path); `TenantService.getById`, `updateStatus` (`Status.NOT_FOUND`) | tenant/security/TenantResolutionFilter.java:106, :136; tenant/service/TenantService.java:111, :147; tenant/exception/TenantErrorCodes.java:17 | "Tenant not found" / "المستأجر غير موجود" |
| `TENANT_SUSPENDED` | 403 | filter (token, header, path) | tenant/security/TenantResolutionFilter.java:94, :110, :140; tenant/exception/TenantErrorCodes.java:20 | "This tenant is suspended" / "هذا المستأجر معلّق" |
| `TENANT_CONTEXT_MISSING` | 500 | `TenantContext.require()` (`Status.INTERNAL_ERROR`), also when wrapped | tenant/TenantContext.java:50; common/web/GlobalExceptionHandler.java:195-201; tenant/exception/TenantErrorCodes.java:23 | "The operation ran without a tenant context" / "نُفّذت العملية دون سياق مستأجر" |
| `TENANT_CODE_INVALID` | 400 | `TenantDomain.create` (`Status.VALIDATION_ERROR`) | tenant/domain/TenantDomain.java:36; tenant/exception/TenantErrorCodes.java:26 | "Invalid tenant code ''{0}''…" / "رمز المستأجر ''{0}'' غير صالح…" |
| `TENANT_CODE_DUPLICATE` | 409 | `TenantDomain.create` via `DomainRules.assertUnique` (`Status.ALREADY_EXISTS`); `TenantService.create` on the insert race | tenant/domain/TenantDomain.java:38; common/domain/DomainRules.java:17-21; tenant/service/TenantService.java:81-82; tenant/exception/TenantErrorCodes.java:29 | "A tenant with code ''{0}'' already exists" / "يوجد مستأجر بالرمز ''{0}'' مسبقًا" |
| `TENANT_PLATFORM_PROTECTED` | 422 | `TenantDomain.assertCanChangeStatusTo` (`Status.BUSINESS_RULE_VIOLATION`) | tenant/domain/TenantDomain.java:56-57; tenant/exception/TenantErrorCodes.java:32 | "The platform tenant ''{0}'' cannot be suspended" / "لا يمكن تعليق مستأجر المنصة ''{0}''" |
`Status` → HTTP: common/domain/status/Status.java:10-20. Messages: `erp-core/src/main/resources/i18n/messages.properties`
lines 143–149, `messages_ar.properties` lines 140–146. Shared codes the endpoints can also answer:
`VALIDATION_ERROR` 400, `CONCURRENT_MODIFICATION` 409, `SEC-401-INVALID-CREDENTIALS` 401,
`SEC-403-FORBIDDEN` 403, `INTERNAL_ERROR` 500.

## Traceability matrix
| P0.5 | REQ | AC | RULE | ENT | SCR-REQ |
|---|---|---|---|---|---|
| US-TENANT-001 | REQ-TENANT-001…004, -015, -021, -022 | AC-TENANT-001…004, -015, -021, -022 | RULE-TENANT-001, -002, -003, -007 | ENT-TENANT-001 | SCR-REQ-TENANT-001 |
| US-TENANT-002 | REQ-TENANT-005, -006, -007, -015 | AC-TENANT-005, -006, -007, -015 | — | ENT-TENANT-001 | SCR-REQ-TENANT-001 |
| US-TENANT-003 | REQ-TENANT-008, -009, -010, -015, -022 | AC-TENANT-008, -009, -010, -015, -022 | RULE-TENANT-004, -005, -006 | ENT-TENANT-001 | SCR-REQ-TENANT-001 |
| US-TENANT-004 | REQ-TENANT-010, -012, -013, -014, -016, -023 | AC-TENANT-010, -012, -013, -014, -016, -023 | RULE-TENANT-006, -009 | ENT-TENANT-001 | — |
| US-TENANT-005 | REQ-TENANT-011 | AC-TENANT-011 | RULE-TENANT-009 | ENT-TENANT-001 | — |
| US-TENANT-006 | REQ-TENANT-001, -020 | AC-TENANT-001, -020 | RULE-TENANT-007, -008 | ENT-TENANT-001 | — |
| US-TENANT-007 | REQ-TENANT-019 | AC-TENANT-019 | — | ENT-TENANT-001 | — |
| US-TENANT-008 | REQ-TENANT-017, -018 | AC-TENANT-017, -018 | — | — | — |

Every story traces to ≥ 1 REQ; every REQ has one AC; every RULE traces to a REQ; the screen traces to
its REQs. No orphan, no dangling id.

## Decisions applied
| DEFAULT / ADR | What | Source | Status |
|---|---|---|---|
| ADR-TENANT-001 | row-level (discriminator) multi-tenancy in one shared schema, not schema-per-tenant | governance/analysis/decisions/TENANT/ADR-TENANT-001.md | ACCEPTED (as built) |

## Access summary
| Page code | Screen | VIEW | CREATE | UPDATE | DELETE | Custom |
|---|---|---|---|---|---|---|
| PLATFORM_TENANTS | Tenants | `PERM_PLATFORM_TENANTS_VIEW` (gateway, menu only) | — | — | — | `PLATFORM_TENANT_MANAGE`: every endpoint (provision, list, read, search, change status) |
Both actions are effective only inside the PLATFORM tenant (RULE-TENANT-007). The frontend's PLATFORM
screen archive is `governance/frontend/modules/PLATFORM/tests/`.
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package C3 — automated tenant-isolation tests (plan §5 C.3, item 12)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

REQ-TENANT-016 (row-level isolation) and RULE-TENANT-008 (provisioning SQL names `TENANT_ID`) state the
isolation guarantee, but nothing failed the build when a new entity or a new raw SQL statement escaped it.
This addendum turns the guarantee into one requirement with three checkable parts: the entity set, the HTTP
behaviour of every core module, and the raw-SQL rule. It names the test or review rule that holds each part.
Ids continue the module's sequence from the highest number ever issued (REQ / AC 023, RULE 009; the TENANT
analysis has no pre-vendoring history). No ENT, DBF, XM, endpoint, permission, error code, schema change or
migration is added.

### 1. Requirements (§A4) — NEW

### REQ-TENANT-024 — ضمان عزل المستأجرين / Tenant isolation guarantee
Pattern    : ubiquitous
Statement  : The system shall keep every tenant-scoped entity under Hibernate's tenant discriminator — every JPA entity of erp-core (every `@Entity` under `com.erp` in erp-core; the reference application declares none) extends `AuditableEntity` and so carries `@TenantId` on `TENANT_ID`, except the documented global set (RULE-TENANT-010) — so that a caller's search, list and read by id in every core module return only its own tenant's rows and an id of another tenant answers 404; and every raw SQL statement on a tenant-scoped table shall name `TENANT_ID` (RULE-TENANT-011).
Traces     : US-TENANT-004
Entities   : ENT-TENANT-001 (FK target); every tenant-scoped entity (21 classes, listed in §3)
Rationale  : POL-TENANT-007; REQ-TENANT-016 made checkable by the build (plan §5 C.3)
Source     : common/domain/AuditableEntity.java:35-37; common/domain/GlobalAuditableEntity.java (the global base); tenant/config/TenantIdentifierResolver.java:36-47
Priority   : HIGH
#### AC-TENANT-024 — [REQ-TENANT-024]
Given two tenants A and B provisioned through `POST /api/v1/platform/tenants`, and in each of them one row of SEC (a role), MDL (a lookup type), FILE (a document), NOTIF (a template), CU (a configuration override), SEQUENCE (a number series) and AUDIT (the `CORE_AUDIT_EVENT` row written when the role was created)
When A's administrator searches or lists each module and asks for B's row by its id
Then every row A receives belongs to tenant A and includes A's new row, never B's; B's id answers 404 with the module's not-found code — `SEC-404-ROLE` (`GET /api/v1/sec/roles/{id}`), `MDL-404-TYPE` (`PUT /api/v1/mdl/lookup-types/{id}`: MDL has no read-by-id endpoint; B's row stays unchanged), `FILE_DOCUMENT_NOT_FOUND` (`GET /api/v1/files/{id}`), `NOTIF_TEMPLATE_NOT_FOUND` (`GET /api/v1/notifications/templates/{id}`), `APP_CONFIGURATION_NOT_FOUND` (`GET /api/v1/common/configurations/{key}` with B's key), `NUMBER_SERIES_NOT_FOUND` (`GET /api/v1/sequence/series/{id}`); AUDIT has no read-by-id endpoint, so `GET /api/v1/audit/events?entityId=<B's role id>` answers an empty page to A while B finds its row; B sees its own rows the same way (`TenantIsolationIntegrationTest`)
And when a JPA entity of erp-core (a production `@Entity` under `com.erp` on erp-core's classpath) outside the global set does not extend `AuditableEntity`, or a class of the global set carries `@TenantId` or is not an entity, the build fails naming the class and telling the developer to make it tenant-scoped or to add it to the global list explicitly (`TenantScopedEntityTest`)

### 2. Business rules (§A5) — NEW

### RULE-TENANT-010 — المجموعة العامة من الكيانات / The global entity set
Scope      : every JPA entity of erp-core
Trigger    : at build time (ArchUnit), whenever an entity is added
Statement  : The system shall treat exactly these entities as global (no Hibernate `@TenantId`; they extend `GlobalAuditableEntity`, not `AuditableEntity`): `com.erp.tenant.entity.Tenant` (`CORE_TENANT`, the tenant registry); `com.erp.sec.entity.ModuleRegistry` (`SEC_MODULE_REG`), `com.erp.sec.entity.ScreenRegistry` (`SEC_SCREEN_REG`) and `com.erp.sec.entity.ActionRegistry` (`SEC_ACTION_REG`), the code-defined permission catalog; `com.erp.cu.entity.AppConfiguration` (`CU_APP_CONFIGURATION`: nullable `TENANT_ID`, `NULL` = platform default, a tenant id = that tenant's override, every query names the owner explicitly in `ConfigurationService`). Every other entity extends `AuditableEntity`. A new global entity is added to the list explicitly, with an analysis entry that says why.
Data source: the entity classes
Message    : — (build failure: "<class> is not tenant-scoped: extend AuditableEntity — or, only if it is truly global, add it explicitly to TenantScopedEntityTest.GLOBAL_ENTITIES with an analysis entry that says why")
Traces     : REQ-TENANT-024
Source     : tenant/entity/Tenant.java:32-40; sec/entity/ModuleRegistry.java:29-36; sec/entity/ScreenRegistry.java:35-45; sec/entity/ActionRegistry.java:35-45; cu/entity/AppConfiguration.java:37-41, :50

### RULE-TENANT-011 — كل جملة SQL صريحة تسمّي TENANT_ID / Every raw SQL statement names TENANT_ID
Scope      : every `JdbcTemplate` / native SQL statement on a tenant-scoped table, in every module (RULE-TENANT-008 is its provisioning case)
Trigger    : on code review (`gov-validate-backend-feature` checklist), wherever raw SQL is added
Statement  : The system shall name `TENANT_ID` explicitly in every raw SQL statement that touches a tenant-scoped table: as the inserted value; as a predicate on every tenant-scoped table and join of a read, update or delete; or as the selected column of a deliberate cross-tenant discovery scan whose follow-up work then runs tenant by tenant. Raw SQL bypasses Hibernate's discriminator.
Data source: the statement text
Message    : — (review finding)
Traces     : REQ-TENANT-024
Source     : `governance/rules/GOVERNANCE-RULES.md` → Governance Rules; `.claude/skills/gov-validate-backend-feature/SKILL.md`; tenant/TenantProvisioningContributor.java:14-17. Where raw SQL may live at all: `CoreLibraryRulesArchTest.rule7_raw_jdbc_only_in_documented_places`, `rule7_native_queries_only_in_tenant_sequence_audit`

### 3. Verified as-built facts
| Kind | Fact | Source |
|---|---|---|
| NEW (note) | Tenant-scoped entities (21, each `extends AuditableEntity`): AUDIT `AuditEvent`; FILE `FileCategory`, `FileDocument`; MDL `LookupType`, `LookupValue`; NOTIF `NotificationChannelConfig`, `NotificationInboxItem`, `NotificationLog`, `NotificationTemplate`; SEC `ActiveSession`, `AuditLogEntry`, `CustomerVerifyToken`, `PasswordResetToken`, `Role`, `RoleActionGrant`, `RoleModuleGrant`, `RoleScreenGrant`, `SignupRequest`, `User`, `UserRoleAssignment`; SEQUENCE `NumberSeries`. With `CU_APP_CONFIGURATION` they are the 22 `TENANT_ID` tables of `../P2/db-script-tenant.md`. | the `@Entity` classes under erp-core/src/main/java/com/erp; `TenantSchemaIntegrationTest.everyEntity_extendsAuditableEntity_exceptTheFourGlobalOnes` (asserts 21) |
| NEW (note) | Raw SQL on tenant-scoped tables: 15 statements, each naming `TENANT_ID` (RULE-TENANT-011). SEC provisioning 6 (`SEC_ROLE`, `SEC_ROLE_MODULE_GRANT`, `SEC_ROLE_SCREEN_GRANT`, `SEC_ROLE_ACTION_GRANT`, `SEC_USER`, `SEC_USER_ROLE`), MDL provisioning 2, NOTIF provisioning 2, SEQUENCE provisioning 1, `AuditEventStore` 3 (the insert; retention's tenant scan and per-tenant delete), `NotificationRequeueJob` 1 (tenant scan, then JPA per tenant through `TenantContext.callAs`). There is no `@Query(nativeQuery = true)` and no `createNativeQuery`. | sec, mdl, notif, sequence `tenant/*TenantProvisioningContributor.java`; audit/service/AuditEventStore.java:43-46, :98-103; notif/service/NotificationRequeueJob.java:43-44, :77-81 |

### 4. Tests and rules that hold it
| Kind | Item | Covers |
|---|---|---|
| NEW | ArchUnit `erp-core/src/test/java/com/erp/architecture/TenantScopedEntityTest.java` | RULE-TENANT-010; the entity part of REQ-TENANT-024 |
| CHANGED | `erp-core/src/test/java/com/erp/tenant/TenantIsolationIntegrationTest.java` (step 05: SEC users only) gains one test per module: SEC role, MDL lookup type, FILE document, NOTIF template, CU configuration, SEQUENCE number series, AUDIT event | AC-TENANT-024, the HTTP part |
| NEW | `governance/rules/GOVERNANCE-RULES.md` → Governance Rules: the raw-SQL rule; one checklist item of `gov-validate-backend-feature` | RULE-TENANT-011 |

### 5. Frontend impact
None: no endpoint, field, permission, page code or error code changes.

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package D — what SEC's user profile and password policy change on the tenant side
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

No TENANT id is minted by package D (other packages of the plan append their own rows to this 1.3.0
section: C3 isolation tests, B profile / lifecycle, C events / token cut-off, E branding).

| Kind | Item | Delta | Source |
|---|---|---|---|
| CHANGED | XM-TENANT-001 `TenantLookupApi` | + `Optional<TenantSummary> summaryOf(Long tenantId)` — `TenantSummary(Long id, String code, String nameAr, String nameEn)` in `com.erp.tenant.crossmodule`, plain values, never the entity; empty for an unknown id; no `@PreAuthorize` (code and names are what the login page and the shell show anyway). `codeOf` unchanged. Consumer: SEC `GET /api/v1/sec/me` (`tenant { code, nameAr, nameEn }`). Package E's public branding can reuse it. | SEC srs-sec.md 1.3.0 §9.5, §9.7 |
| CHANGED | REQ-TENANT-001 (provision a tenant), SCR-REQ-TENANT-001 B5 create | `adminPassword` must meet SEC's password policy (SEC RULE-SEC-056: 8..72 characters, at most 72 bytes, a letter and a digit by default): otherwise 400 `SEC-400-PASSWORD-POLICY` with `fieldErrors[0].field = adminPassword`, raised by SEC's provisioning contributor inside the provisioning transaction, so nothing is created. The first administrator is not flagged `passwordChangeRequired` (the platform operator hands the password over; package B's admin-reset is the recovery path). | SEC srs-sec.md 1.3.0 §9.9; RULE-SEC-058 |
