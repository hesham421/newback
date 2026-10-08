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

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package B — tenant level 1: editable names and profile, suspension with a recorded reason, platform-side recovery of a tenant administrator, usage figures (plan §4 B.1–B.5)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Ids continue from the highest number ever issued for TENANT (tree and history): REQ / AC 024 (C3),
RULE 011 (C3); RULE-TENANT-012 … 015 are reserved for the as-built rules of the analysis-coverage work, so
this block mints **REQ/AC-TENANT-025 … 028, RULE-TENANT-016 … 017** (POL-TENANT-012/013, US-TENANT-009 … 011
in P0 / P0_5, DBF-TENANT-033 … 042 in P2). No ENT, XM, SCR-REQ, permission or page code is added (plan §0
D5: every endpoint stays behind `PLATFORM_TENANT_MANAGE`). Migrations `V18__tenant_profile.sql` and
`V19__tenant_lifecycle.sql` (the plan expected V16 / V17; numbers re-derived at creation time, plan §1.3 /
§11, `docs/DEVIATIONS.md` `[TM-B]`). Paths are relative to `/api/v1/platform/tenants`.

### B1. Endpoints (SCR-REQ-TENANT-001 B5; `PlatformTenantController`)
Every row keeps the 1.2.0 gate: authority `PLATFORM_TENANT_MANAGE` on the service plus the chain gate
"caller's tenant = PLATFORM" (REQ-TENANT-015); 401 `SEC-401-INVALID-CREDENTIALS` without a token, 403
`SEC-403-FORBIDDEN` for any other caller.

| Kind | Method | Path | Request | Response (`ApiResponse<T>`) | Errors (HTTP · code) | Traces |
|---|---|---|---|---|---|---|
| NEW | PUT | `/{id}` | `TenantUpdateRequest { nameAr*, nameEn*, contactEmail, contactPhone, countryCode, defaultLocale, timezone, notes }` (formats in B5) — no `code`, no `statusCode`, no `version`; a JSON field of another name is ignored (`fail-on-unknown-properties: false`) | 200 `TenantResponse` | 404 · `TENANT_NOT_FOUND`; 400 · `VALIDATION_ERROR` (field named); 409 · `CONCURRENT_MODIFICATION` (a concurrent write between read and flush, the `VERSION` lock) | REQ-TENANT-025; RULE-TENANT-003 |
| CHANGED | PATCH | `/{id}/status` | `TenantStatusUpdateRequest { statusCode*, reason }` — `reason` required for `SUSPENDED` (3..500 characters after trimming), ignored for `ACTIVE` | 200 `TenantResponse` | + 400 · `TENANT_SUSPENSION_REASON_REQUIRED`; as before 404 · `TENANT_NOT_FOUND`, 422 · `TENANT_PLATFORM_PROTECTED` (checked first), 400 · `VALIDATION_ERROR`, 409 · `CONCURRENT_MODIFICATION` | REQ-TENANT-026; RULE-TENANT-004, -005, -016 |
| NEW | POST | `/{id}/admin-reset` | `TenantAdminResetRequest { username* (≤ 100), newPassword* (≤ 200, raw, never logged), requireChangeAtNextLogin (Boolean, null = true) }` | 200 `TenantAdminResetResponse { username, sessionsTerminated }` | 404 · `TENANT_NOT_FOUND`; 422 · `TENANT_ADMIN_RESET_PLATFORM` (`{id}` = PLATFORM; review round 1); 404 · `TENANT_ADMIN_NOT_FOUND`; 422 · `TENANT_ADMIN_NOT_SUPER`; 400 · `SEC-400-PASSWORD-POLICY` (`fieldErrors[0].field = newPassword`, SEC RULE-SEC-056); 400 · `VALIDATION_ERROR` | REQ-TENANT-027; RULE-TENANT-017 |
| NEW | GET | `/{id}/usage` | — | 200 `TenantUsageResponse { id, staffUsers, customerUsers, activeSessions, fileDocuments, fileBytes, notificationsLast30Days, collectedAt }` | 404 · `TENANT_NOT_FOUND` | REQ-TENANT-028 |
| CHANGED | GET / POST / GET / PATCH / PUT | `/{id}`, `/search`, list, `/{id}/status`, `/{id}` | — | `TenantResponse` + `contactEmail`, `contactPhone`, `countryCode`, `defaultLocale`, `timezone`, `notes`, `suspendedAt`, `suspendedBy`, `suspensionReason` (`tokensInvalidBefore` is not exposed) | — | REQ-TENANT-025, -026 |
| CHANGED | POST | `/search` | filters / sorts | + `contactEmail`, `countryCode`, `suspendedAt` (ISO-8601 instant; a malformed value 400 `VALIDATION_ERROR`) on the allow-list `id, code, nameAr, nameEn, statusCode, createdAt` | as before | REQ-TENANT-007 |

Order of checks — admin-reset: tenant (`TENANT_NOT_FOUND`) → not PLATFORM (`TENANT_ADMIN_RESET_PLATFORM`) → target exists (`TENANT_ADMIN_NOT_FOUND`) →
target super (`TENANT_ADMIN_NOT_SUPER`) → password policy → write. Status change: request validation →
tenant → PLATFORM protection (RULE-TENANT-005) → reason (RULE-TENANT-016) → write.

### B2. Requirements (§A4) — NEW

### REQ-TENANT-025 — تعديل أسماء المستأجر وملفه / Update a tenant's names and profile
Pattern    : event
Statement  : When a platform operator puts a tenant's names and profile (`nameAr`, `nameEn`, `contactEmail`, `contactPhone`, `countryCode`, `defaultLocale`, `timezone`, `notes`) to `PUT /api/v1/platform/tenants/{id}`, the system shall replace them — an absent or empty optional field is cleared — and return the tenant; it shall never change the tenant's `code` or `statusCode` through this operation (body fields of those names are ignored).
Traces     : US-TENANT-009
Entities   : ENT-TENANT-001
Rationale  : POL-TENANT-001 (CHANGED: names and profile editable, code still immutable), POL-TENANT-006
Source     : docs/plans/tenant-maturity-plan.md §4 B.2
Priority   : MEDIUM
#### AC-TENANT-025 — [REQ-TENANT-025]
Given a provisioned tenant D
When the operator puts new `nameAr` / `nameEn`, `contactEmail`, `contactPhone`, `countryCode = SA`, `defaultLocale = ar`, `timezone = Asia/Riyadh`, `notes`, plus `code = OTHER` and `statusCode = SUSPENDED`
Then the system answers 200 with the new values, `code` and `statusCode` unchanged; `GET /{id}` shows the same; `POST /search` with `countryCode EQUALS SA` and with `contactEmail` finds D;
and a second PUT without the optional fields clears them; an unknown id answers 404 `TENANT_NOT_FOUND`; `defaultLocale = fr`, `countryCode = sau`, a blank `nameEn` answer 400 `VALIDATION_ERROR` naming the field and change nothing

### REQ-TENANT-026 — تعليق بسبب وحقائق التعليق / Suspend with a reason; suspension facts
Pattern    : event
Statement  : When a platform operator suspends a tenant, the system shall require a reason of 3 to 500 characters and record when (`suspendedAt`), by whom (`suspendedBy`, the operator's username) and why (`suspensionReason`); if the reason is missing or outside that length, the system shall refuse the suspension with 400 `TENANT_SUSPENSION_REASON_REQUIRED` and change nothing; when a suspended tenant is activated, the system shall clear the three facts and set the tenant's token cut-off (`tokensInvalidBefore`) to the activation time.
Traces     : US-TENANT-003 (CHANGED)
Entities   : ENT-TENANT-001
Rationale  : POL-TENANT-012; RULE-TENANT-016
Source     : docs/plans/tenant-maturity-plan.md §4 B.1, B.2
Priority   : HIGH
Note       : the cut-off is stored here; package C.2 (plan §5) refuses tokens issued before it. Until then an activation does not invalidate earlier tokens.
#### AC-TENANT-026 — [REQ-TENANT-026]
Given an ACTIVE tenant D
When the operator suspends it without `reason`, then with `reason = "ab"`
Then each answers 400 `TENANT_SUSPENSION_REASON_REQUIRED` and D stays ACTIVE with no facts;
when the operator suspends it with `reason = "Unpaid invoice"`, the system answers 200 `statusCode = SUSPENDED`, `suspendedAt` set, `suspendedBy` = the operator, `suspensionReason = "Unpaid invoice"`, and D's administrator's login answers 403 `TENANT_SUSPENDED`;
when the operator activates D, the system answers 200 `statusCode = ACTIVE` with `suspendedAt`, `suspendedBy`, `suspensionReason` null, `CORE_TENANT.TOKENS_INVALID_BEFORE` holds the activation time, and the administrator logs in again;
and suspending PLATFORM without a reason still answers 422 `TENANT_PLATFORM_PROTECTED`

### REQ-TENANT-027 — إعادة تعيين كلمة مرور مدير المستأجر / Reset a tenant administrator's password
Pattern    : event
Statement  : When a platform operator posts a username and a new password to `POST /api/v1/platform/tenants/{id}/admin-reset`, the system shall, in one transaction of tenant {id}, verify that the username is a STAFF user of that tenant holding an active super role (RULE-TENANT-017), apply the STAFF password policy, store the new password's hash, require a change at the next sign-in unless `requireChangeAtNextLogin` is false, terminate every open session of that user, record `ADMIN_PASSWORD_RESET` in that tenant's generic audit log (actor = the platform operator), then record `TENANT_ADMIN_RESET` in the PLATFORM tenant's audit log (entity `CORE_TENANT` / {id}), and answer the username and the number of terminated sessions; it shall refuse the reset when {id} is the PLATFORM tenant itself; a refusal changes nothing.
Traces     : US-TENANT-010
Entities   : ENT-TENANT-001; SEC ENT-SEC-001 (through `SecAdminRecoveryApi`)
Rationale  : POL-TENANT-013, POL-TENANT-006, POL-TENANT-011; SEC ADR-SEC-063 (an administrator-chosen password forces a change by default)
Source     : docs/plans/tenant-maturity-plan.md §4 B.2, B.4
Priority   : HIGH
#### AC-TENANT-027 — [REQ-TENANT-027]
Given tenant D whose administrator `td-admin` (role `SYS_ADMIN`, `IS_SUPER`) has one open session, and a STAFF user of D without a role
When the operator posts an unknown username → 404 `TENANT_ADMIN_NOT_FOUND`; any username to the PLATFORM tenant (id 1), the operator's own included → 422 `TENANT_ADMIN_RESET_PLATFORM`; the role-less user → 422 `TENANT_ADMIN_NOT_SUPER`; `td-admin` with `abcdefgh` → 400 `SEC-400-PASSWORD-POLICY` (`newPassword`); an unknown tenant id → 404 `TENANT_NOT_FOUND`
Then nothing changed (the old password still signs in);
when the operator posts `td-admin` with a valid password, the system answers 200 `{ username: td-admin, sessionsTerminated: 1 }`, the old token answers 401, the old password 401, the new password signs in with `passwordChangeRequired = true` (false when the request said `requireChangeAtNextLogin: false`), and D's audit log has one `ADMIN_PASSWORD_RESET` row for that user whose actor is the operator and which contains no password, and PLATFORM's audit log has one `TENANT_ADMIN_RESET` row for `CORE_TENANT` / D's id naming `td-admin` and the terminated-session count, without a secret

### REQ-TENANT-028 — أرقام استخدام المستأجر / Tenant usage figures
Pattern    : event
Statement  : When a platform operator asks for `GET /api/v1/platform/tenants/{id}/usage`, the system shall return the tenant's `staffUsers`, `customerUsers`, `activeSessions`, `fileDocuments`, `fileBytes` and `notificationsLast30Days` with `collectedAt`, each counted inside tenant {id} (one read-only transaction under `TenantContext.callAs(id)`) through the cross-module APIs of SEC, FILE and NOTIF, never by reading their tables.
Traces     : US-TENANT-011
Entities   : ENT-TENANT-001
Rationale  : POL-TENANT-006, POL-TENANT-007 (the counts of one tenant never include another's rows)
Source     : docs/plans/tenant-maturity-plan.md §4 B.2, B.4
Priority   : MEDIUM
#### AC-TENANT-028 — [REQ-TENANT-028]
Given a tenant D just provisioned
When the operator asks for its usage
Then the system answers 200 `staffUsers = 1`, `customerUsers = 0`, `activeSessions = 0`, `fileDocuments = 0`, `fileBytes = 0`, `notificationsLast30Days = 0`, `collectedAt` set;
after D's administrator signs in and creates a user, `staffUsers = 2` and `activeSessions ≥ 1`, while another tenant's figures are unchanged by D's rows; an unknown id answers 404 `TENANT_NOT_FOUND`

### B3. Business rules (§A5) — NEW / CHANGED

### RULE-TENANT-016 — التعليق يتطلب سببًا والتفعيل يمحو حقائقه / Suspension requires a reason; activation clears it
Scope      : ENT-TENANT-001
Trigger    : on status change
Statement  : The system shall suspend a tenant only with a `reason` whose trimmed length is 3..500 and shall then record `SUSPENDED_AT` (now), `SUSPENDED_BY` (the operator's username) and `SUSPENSION_REASON` (trimmed); a transition SUSPENDED → ACTIVE clears the three and sets `TOKENS_INVALID_BEFORE` to now. Re-applying the current status (RULE-TENANT-004) changes neither the status nor the facts nor the cut-off; a reason sent with `ACTIVE` is ignored. Evaluated after RULE-TENANT-005.
Data source: the request's `statusCode` and `reason`; ENT-TENANT-001.statusCode
Message    : ar: "يتطلب تعليق المستأجر سببًا من 3 إلى 500 حرف" · en: "A suspension needs a reason of 3 to 500 characters"
Traces     : REQ-TENANT-026
Source     : docs/plans/tenant-maturity-plan.md §4 B.2, B.3
Decided by : `TenantDomain` (`assertSuspensionReasonGiven`, `changesStatusTo`); the entity's `suspend(...)` / `activate(...)` only set the fields

### RULE-TENANT-017 — هدف إعادة تعيين كلمة مرور المدير / Admin-reset target
Scope      : ENT-TENANT-001; SEC ENT-SEC-001
Trigger    : on `/{id}/admin-reset`
Statement  : The system shall reset a password through `/{id}/admin-reset` only for a STAFF user of tenant {id} (any account status) holding at least one ACTIVE role with `IS_SUPER = TRUE`, and never when {id} is the PLATFORM tenant (id 1): platform operators set each other's passwords through SEC's `PUT /api/v1/sec/users/{id}/password`, where RULE-SEC-057 refuses one's own account (review round 1). The facts "exists" and "holds an active super role" are computed by SEC (`SecAdminRecoveryApi.findRecoveryTarget`, inside tenant {id}) and passed into `TenantDomain` by the service; a CUSTOMER account of that name is not a target.
Data source: SEC — `SEC_USER` (realm STAFF), `SEC_USER_ROLE`, `SEC_ROLE.IS_SUPER` / `IS_ACTIVE_FL` of tenant {id}
Message    : `TENANT_ADMIN_RESET_PLATFORM` ar: "لا يُستعاد مستأجر المنصة ''{0}'' من هنا: تُعيَّن كلمة مرور مشغّل المنصة من شاشة المستخدمين" · en: "The platform tenant ''{0}'' is not recovered here: a platform operator's password is set on the users screen"; `TENANT_ADMIN_NOT_FOUND` ar: "لا يوجد مستخدم موظف باسم ''{0}'' في المستأجر ''{1}''" · en: "No staff user ''{0}'' exists in tenant ''{1}''"; `TENANT_ADMIN_NOT_SUPER` ar: "المستخدم ''{0}'' في المستأجر ''{1}'' لا يحمل دورًا فائقًا؛ لا يُستعاد هنا إلا مدير المستأجر" · en: "User ''{0}'' of tenant ''{1}'' holds no super role; only a tenant administrator can be recovered here"
Traces     : REQ-TENANT-027
Source     : docs/plans/tenant-maturity-plan.md §4 B.2, B.3
Decided by : `TenantDomain.assertAdminResetAllowed` (PLATFORM), `TenantDomain.assertCanResetAdministrator` (target)

| Kind | Rule | Delta |
|---|---|---|
| CHANGED | RULE-TENANT-003 (code immutability) | the names and the profile become editable (`PUT /{id}`); the code still never changes (`updatable = false`, not in `TenantUpdateRequest`) |
| CHANGED | RULE-TENANT-004 (status values) | re-applying the current status also leaves the suspension facts and the cut-off untouched (RULE-TENANT-016) |
| CHANGED (review round 1) | RULE-TENANT-017 (admin-reset target) | + never on the PLATFORM tenant (422 `TENANT_ADMIN_RESET_PLATFORM`): the first version let a platform operator reset their own password there without the current one, bypassing SEC RULE-SEC-057 |

### B4. Error codes — NEW
| Code | HTTP | `Status` | Raised by | Message args |
|---|---|---|---|---|
| `TENANT_SUSPENSION_REASON_REQUIRED` | 400 | `VALIDATION_ERROR` | `TenantDomain.assertSuspensionReasonGiven` | — |
| `TENANT_ADMIN_RESET_PLATFORM` | 422 | `BUSINESS_RULE_VIOLATION` | `TenantDomain.assertAdminResetAllowed` (review round 1) | tenant code |
| `TENANT_ADMIN_NOT_FOUND` | 404 | `NOT_FOUND` | `TenantDomain.assertCanResetAdministrator` | username, tenant code |
| `TENANT_ADMIN_NOT_SUPER` | 422 | `BUSINESS_RULE_VIOLATION` | `TenantDomain.assertCanResetAdministrator` | username, tenant code |
`TENANT_PLATFORM_PROTECTED` is not reused for the PLATFORM refusal: its message says the platform tenant "cannot be suspended". Referenced (SEC): `SEC-400-PASSWORD-POLICY` (400). Every new code has an entry in `messages.properties` and
`messages_ar.properties` (one `tenant-maturity B` block each); the seven 1.2.0 codes are unchanged.

### B5. ENT-TENANT-001 Tenant — CHANGED (fields)
| Kind | Field | Logical type | Required | Rule / format (request validation → 400 `VALIDATION_ERROR`) | Written by | Label-ar | Label-en |
|---|---|---|---|---|---|---|---|
| CHANGED | nameAr / nameEn | text (≤ 200) | yes | not blank — now editable (`PUT /{id}`) | operator | الاسم بالعربية / بالإنجليزية | Name (Arabic / English) |
| NEW | contactEmail | text (≤ 255) | no | e-mail format | operator | بريد التواصل | Contact e-mail |
| NEW | contactPhone | text (≤ 30) | no | `^\+?[0-9][0-9 -]{5,28}[0-9]$` (the format of SEC's user `phone`) | operator | هاتف التواصل | Contact phone |
| NEW | countryCode | text (2) | no | ISO 3166-1 alpha-2, upper case `^[A-Z]{2}$` | operator | رمز الدولة | Country code |
| NEW | defaultLocale | text (≤ 5) | no | `ar` \| `en` (`CHK_CORE_TENANT_LOCALE`) | operator | اللغة الافتراضية | Default language |
| NEW | timezone | text (≤ 64) | no | IANA zone id format `^[A-Za-z][A-Za-z0-9_+-]*(/[A-Za-z0-9_+-]+)*$` (e.g. `Asia/Riyadh`); the format is checked, not membership in the zone database | operator | المنطقة الزمنية | Time zone |
| NEW | notes | text (≤ 1000) | no | — | operator | ملاحظات | Notes |
| NEW | suspendedAt | date-time | no | read-only | RULE-TENANT-016 | تاريخ التعليق | Suspended at |
| NEW | suspendedBy | text (≤ 100) | no | read-only (the operator's username) | RULE-TENANT-016 | علّقه | Suspended by |
| NEW | suspensionReason | text (≤ 500) | no | read-only | RULE-TENANT-016 | سبب التعليق | Suspension reason |
| NEW | tokensInvalidBefore | date-time | no | system only, not exposed | RULE-TENANT-016 (activation); package C.2 | حد صلاحية الرموز | Tokens invalid before |
An empty string sent for an optional profile field is stored as NULL; strings are trimmed and `countryCode`
upper-cased by the entity's `@PrePersist` / `@PreUpdate` (A.1.17). Physical names, widths and
constraints: `../P2/db-script-tenant.md` 1.3.0 addendum (DBF-TENANT-033 … 042). New DTOs:
`TenantUpdateRequest`, `TenantAdminResetRequest`, `TenantAdminResetResponse`, `TenantUsageResponse`;
`TenantResponse` and `TenantStatusUpdateRequest` extended.

### B6. Status lifecycle (§A7) — CHANGED
```
ACTIVE    --(operator suspends with a reason, RULE-TENANT-016)--> SUSPENDED   records suspendedAt / suspendedBy / suspensionReason
SUSPENDED --(operator activates)--------------------------------> ACTIVE      clears the three, sets tokensInvalidBefore = now
same status re-applied → nothing changes (RULE-TENANT-004)
```
Still two states, no `ARCHIVED` (level 2 is out of scope, plan §0 D2).

### B7. Dependencies (§A8) — NEW consumed surfaces
TENANT now consumes three modules, always inside `TenantContext.callAs(id)` and through their `crossmodule`
packages only (ArchUnit `CrossModuleBoundaryArchTest`):

| Consumed surface | Owner | Methods | Used by |
|---|---|---|---|
| `com.erp.sec.crossmodule.SecUserDirectoryApi` | SEC (REQ-SEC-090) | `int countStaff()`, `int countCustomers()`, `int countActiveSessions()` — current tenant | usage |
| `com.erp.sec.crossmodule.SecAdminRecoveryApi` (NEW) | SEC (REQ-SEC-091) | `Optional<RecoveryTarget> findRecoveryTarget(String username)` → `RecoveryTarget(Long userId, String username, boolean superRole)`; `int resetSuperUserPassword(String username, String rawPassword, Boolean requireChangeAtNextLogin)` (null = TRUE, applied by SEC: RULE-SEC-058) → terminated-session count; throws `SEC-400-PASSWORD-POLICY` | admin-reset |
| `com.erp.file.crossmodule.FileDocumentLookupApi` | FILE (XM-FILE-001, CHANGED) | `long countDocuments()`, `long sumBytes()` — current tenant, documents not `DELETED` | usage |
| `com.erp.notif.crossmodule.NotificationLogQueryApi` | NOTIF (1.3.0 addendum) | `long countDispatchedSince(Instant since)` — current tenant's `NOTIF_LOG` rows created at or after `since` | usage |
| `com.erp.audit.crossmodule.AuditApi` | audit | `ADMIN_PASSWORD_RESET` is written by SEC's recovery inside tenant {id}; `TENANT_ADMIN_RESET` by `TenantService` in PLATFORM (B8) | admin-reset |
The admin-reset and usage service methods are deliberately not `@Transactional`: a transaction opened in
the PLATFORM request would bind the PLATFORM Hibernate session (REQ-TENANT-018, `TenantContext` Javadoc),
so they open one transaction inside `callAs(id)` with a `TransactionTemplate` (the
`PermissionCatalogSynchronizer` precedent): admin-reset read-write (check + write atomic), usage read-only.

### B8. Audit, sessions, events
| Operation | `CORE_AUDIT_EVENT` | Sessions | Event |
|---|---|---|---|
| `PUT /{id}` | `UPDATE` row of `CORE_TENANT` (`@Audited`, in PLATFORM) with the changed fields | — | — |
| suspend / activate | `UPDATE` row of `CORE_TENANT` (`statusCode` + the suspension facts; `tokensInvalidBefore` is dropped by the audit denylist word `token`) | — (package C.1 terminates them on suspension) | — (package C.1 adds the lifecycle events) |
| admin-reset | (1) `ADMIN_PASSWORD_RESET` in tenant {id}: actor = the operator's username, realm `STAFF`, `actorUserId` null (the operator is not a user of {id}), entity `SEC_USER` / the user's id, summaries name the user and say "by the platform operator", no secret; (2) review round 1: after the reset committed, `TENANT_ADMIN_RESET` in the PLATFORM tenant (outside `callAs`, its own commit): actor = the operator, entity `CORE_TENANT` / {id}, summaries name the tenant code, the target username and `sessionsTerminated`, no secret — so a PLATFORM auditor sees every recovery | every open session of the user terminated, one SEC `SESSION_TERMINATED` row each (no actor user: the operator is not a user of {id}) | `UserPasswordChangedEvent(userId, byAdmin = true)` in tenant {id} → NOTIF e-mails `STAFF_PASSWORD_CHANGED` to the administrator (RULE-NOTIF-023) |
| usage | — | — | — |

### B9. SCR-REQ-TENANT-001 PLATFORM_TENANTS — CHANGED
| Kind | Section | Delta |
|---|---|---|
| CHANGED | B1 Operations | + edit names and profile, recover the administrator's password, show usage |
| CHANGED | B2 Search / list | filters and sorts + `contactEmail`, `countryCode`, `suspendedAt` |
| CHANGED | B3 Input | edit form (names + profile, `code` read-only); suspend dialog with a mandatory reason (3..500); admin-reset form (`username`, `newPassword`, `requireChangeAtNextLogin` default on); usage panel (read-only) |
| unchanged | B4 Access | same two actions; every new endpoint needs `PLATFORM_TENANT_MANAGE` (D5) |
| CHANGED | B5 API expectations | + the rows of B1 above |

### B10. Decisions and deliberate differences from the plan
| Kind | Note |
|---|---|
| CHANGED (plan) | Migrations `V18__tenant_profile.sql` / `V19__tenant_lifecycle.sql` (plan: V16 / V17; package D took V16 / V17). |
| NEW (decision) | `PUT /{id}` carries no `version`: no erp-core PUT does; the `VERSION` lock answers 409 `CONCURRENT_MODIFICATION` for a write that races between read and flush. It is a full replacement of the editable fields (a new endpoint, no older client to protect). |
| CHANGED (plan) | Admin-reset body + optional `requireChangeAtNextLogin` (null = true): an operator-chosen password is an administrator-chosen password, so SEC ADR-SEC-063 / RULE-SEC-058 apply (forced change by default, opt-out per request). |
| CHANGED (plan) | `SecAdminRecoveryApi`: + `findRecoveryTarget(username)` (the facts RULE-TENANT-017 needs, decided in `TenantDomain` per plan B.3) and `resetSuperUserPassword(username, rawPassword, requireChangeAtNextLogin)` (the request's flag passed through; SEC applies its null = TRUE default, so RULE-SEC-058 stays in SEC). Both run in the one transaction of tenant {id}, so the check and the write are atomic. |
| CHANGED (plan) | NOTIF "existing dispatch API + `countDispatchedSince`" → `NotificationLogQueryApi.countDispatchedSince`: `NotificationDispatchApi` is NOTIF's write surface; the dispatch-history read surface is `NotificationLogQueryApi`. |
| NEW (decision) | What each figure counts: `staffUsers` / `customerUsers` = `SEC_USER` rows of realm STAFF / CUSTOMER in any status; `activeSessions` = `SEC_ACTIVE_SESSION` rows with `TERMINATED_AT` NULL, either realm; `fileDocuments` / `fileBytes` = `FILE_DOCUMENT` rows not `DELETED` (ACTIVE, ARCHIVED) and the sum of their `FILE_SIZE`; `notificationsLast30Days` = `NOTIF_LOG` rows (one per channel, any status) created in the 30 days before `collectedAt`. |
| NEW (decision) | `TenantUsageResponse` is a figures DTO, not build-create-dto's eligibility `UsageResponse` (`canDelete` / `canDeactivate`): a tenant is never deleted (POL-TENANT-005) and its suspension is never blocked by data. |
| NEW (review round 1) | Admin-reset refuses the PLATFORM tenant (RULE-TENANT-017 CHANGED, 422 `TENANT_ADMIN_RESET_PLATFORM`, a dedicated code because `TENANT_PLATFORM_PROTECTED`'s message is about suspension); every successful reset is also audited in PLATFORM (`TENANT_ADMIN_RESET`, B8). Logs of the admin-reset path name the tenant id and the user id, never the username (plan §1.7). |
| NEW (note) | `TOKENS_INVALID_BEFORE` is written on activation but enforced only by package C.2; until then an activation does not invalidate tokens issued before it. |

### B11. Frontend impact (read by the frontend repository — plan §8 F3)
| Kind | Item |
|---|---|
| NEW | `PUT /{id}`, `POST /{id}/admin-reset`, `GET /{id}/usage` on the `PLATFORM_TENANTS` screen (no new page code, permission or menu entry). |
| CHANGED | The suspend action must send a `reason` (3..500); without it 400 `TENANT_SUSPENSION_REASON_REQUIRED`. `TenantResponse` carries the profile and the suspension facts. |
| NEW | Error codes `TENANT_SUSPENSION_REASON_REQUIRED`, `TENANT_ADMIN_RESET_PLATFORM`, `TENANT_ADMIN_NOT_FOUND`, `TENANT_ADMIN_NOT_SUPER` (both languages); the admin-reset form is not offered for the PLATFORM row; admin-reset may also answer `SEC-400-PASSWORD-POLICY`. |

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package E — tenant branding: a logo and an optional brand colour set by the platform operator from `PLATFORM_TENANTS`, `GET /api/v1/tenant/me`, public branding by tenant code (plan §0 D5, §7 E.1–E.4)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Ids continue from the highest number ever issued for TENANT (tree and history of both repositories): REQ / AC
028, RULE 017 (RULE-TENANT-012 … 015 reserved for the analysis-coverage work's as-built rules), XM 002, POL 013,
US 011, DBF 042. This block mints **REQ/AC-TENANT-029 … 032, RULE-TENANT-018 … 022, XM-TENANT-003**
(POL-TENANT-014, US-TENANT-012 … 014 in P0 / P0_5, DBF-TENANT-043 … 044 in P2) and decision
**ADR-TENANT-005** (the plan's own number, reserved for E). No ENT, SCR-REQ, module, screen, permission, grant
seed or page code is added (plan §0 D5). Migration `V20__tenant_branding.sql` (the plan expected V18; numbers
re-derived at creation time, plan §1.3 / §11, `docs/DEVIATIONS.md` `[TM-E]`). Platform paths are relative to
`/api/v1/platform/tenants`. Rows marked **FE** are read by the frontend (plan §8 F2, F3).

### E1. Endpoints
The three platform rows keep the 1.2.0 gate: authority `PLATFORM_TENANT_MANAGE` on the service plus the chain
gate "caller's tenant = PLATFORM" (REQ-TENANT-015); 401 `SEC-401-INVALID-CREDENTIALS` without a token, 403
`SEC-403-FORBIDDEN` for any other caller — a tenant administrator included (RULE-TENANT-020).

| Kind | Method | Path | Access | Request | Response (`ApiResponse<T>`) | Errors (HTTP · code) | Traces |
|---|---|---|---|---|---|---|---|
| NEW — **FE** | PUT | `/{id}/logo` | `PLATFORM_TENANT_MANAGE` | `multipart/form-data`, part `file` (the image; at most 1 MB, PNG / JPEG / WebP / plain SVG, detected from the bytes) | 200 `TenantResponse` with the new `logoUrl` | 404 · `TENANT_NOT_FOUND`; 400 · `TENANT_LOGO_INVALID` (`fieldErrors[0].field = file`; empty, too large, another type, unsafe SVG); 400 · `VALIDATION_ERROR` (no `file` part, not a multipart request) | REQ-TENANT-029; RULE-TENANT-018, -019, -020 |
| NEW — **FE** | DELETE | `/{id}/logo` | `PLATFORM_TENANT_MANAGE` | — | 204 (no body); a tenant without a logo also answers 204 (idempotent, D's photo precedent) | 404 · `TENANT_NOT_FOUND` | REQ-TENANT-029; RULE-TENANT-018, -020 |
| NEW — **FE** | PATCH | `/{id}/branding` | `PLATFORM_TENANT_MANAGE` | `TenantBrandingUpdateRequest { brandColor }` — `#RRGGBB`; null, absent or blank clears it | 200 `TenantResponse` | 404 · `TENANT_NOT_FOUND`; 400 · `TENANT_BRAND_COLOR_INVALID` (`fieldErrors[0].field = brandColor`) | REQ-TENANT-030; RULE-TENANT-020, -021 |
| NEW — **FE** | GET | `/api/v1/tenant/me` | `isAuthenticated()`, **any realm** (STAFF or CUSTOMER token) | — | 200 `TenantBrandingResponse { code, nameAr, nameEn, logoUrl, brandColor, defaultLocale }` of the token's tenant | 401 · `SEC-401-INVALID-CREDENTIALS`; 403 · `TENANT_SUSPENDED` (the token's tenant is suspended, RULE-TENANT-006); 404 · `TENANT_NOT_FOUND` (defensive: the token's tenant vanished) | REQ-TENANT-031 |
| NEW — **FE** | GET | `/api/v1/public/tenants/{tenantCode}/branding` | public, no token, no header | — | 200 `TenantBrandingResponse` | 404 · `TENANT_NOT_FOUND` (unknown code); 403 · `TENANT_SUSPENDED`; 429 · `TENANT_BRANDING_RATE_LIMITED` (RULE-TENANT-022) | REQ-TENANT-032; RULE-TENANT-006, -012 |
| CHANGED — **FE** | POST / GET / GET / POST / PUT / PATCH | create, `/{id}`, list, `/search`, `/{id}`, `/{id}/status` | as before | as before | `TenantResponse` + `logoUrl` (nullable: the public URL of the logo, resolved inside the tenant), `brandColor` (nullable `#RRGGBB`); `LOGO_FILE_ID` itself is not exposed | as before | REQ-TENANT-029, -030 |

Order of checks — logo PUT: request binding (`file` part) → tenant (`TENANT_NOT_FOUND`) → image (`TENANT_LOGO_INVALID`)
→ write. Branding PATCH: tenant → colour → write. Public branding: rate limit (per client address, before anything
else) → tenant from the path (`TENANT_NOT_FOUND` / `TENANT_SUSPENDED`) → read.

Wiring (`ErpCoreSecurityAutoConfiguration`, `ErpCoreProperties`):
- `/api/v1/tenant/me` is outside the customer chain's matcher, so the core (staff) chain serves it; it is named
  realm-neutral there **for `GET` only** (constant `TENANT_ME_PATH`; review round 1): that chain's
  `RealmEnforcementFilter` does not refuse a CUSTOMER token on `GET /api/v1/tenant/me` (any other method still answers
  403 `REALM_MISMATCH` to a CUSTOMER token), and the path still needs an authenticated caller
  (`anyRequest().authenticated()`). The forced-change gate lets the `GET` through too (SEC RULE-SEC-059 CHANGED): it
  reveals nothing the public branding does not.
- `/api/v1/public/tenants/{tenantCode}/branding` lies under `/api/v1/public/**`, so the customer chain serves it
  (constant `PUBLIC_TENANT_BRANDING_PATHS` = `/api/v1/public/tenants/*/branding`): `GET` permitted, public for the
  realm and tenant filters, its tenant taken from the path — the `erp.core.tenant.path-tenant-paths` default gains
  `/api/v1/public/tenants/{tenantCode}/branding` (RULE-TENANT-012 source 1, the step-07 mechanism of the public
  files). An application that replaces the list and leaves the path out gets 400 `TENANT_REQUIRED` there. A
  `PublicBrandingRateLimitFilter` runs first on that chain, for that path only (RULE-TENANT-022).

### E2. Requirements (§A4) — NEW

### REQ-TENANT-029 — شعار المستأجر يضبطه مدير المنصة / Tenant logo set by the platform administrator
Pattern    : event
Statement  : When a platform operator puts an image to `PUT /api/v1/platform/tenants/{id}/logo`, the system shall validate it through FILE's image store (RULE-TENANT-018), store it as a PUBLIC document **in tenant {id}'s own rows** (`TenantContext.callAs(id)`; owner `CORE_TENANT` / {id}, module `TENANT`, base name `logo`), reference it from `CORE_TENANT.LOGO_FILE_ID`, discard the previous logo document, record `TENANT_LOGO_CHANGED` and answer the tenant with its `logoUrl` (`/api/v1/public/files/{thatTenantCode}/{slug}`) — all in one transaction of tenant {id}; when the operator calls `DELETE …/{id}/logo`, the system shall clear the reference, discard the document and record `TENANT_LOGO_CHANGED`; a refused image changes nothing.
Traces     : US-TENANT-012
Entities   : ENT-TENANT-001; FILE ENTITY-FILE-001 (through `FileImageStoreApi`, XM-TENANT-003)
Rationale  : POL-TENANT-014; ADR-TENANT-005; FILE ADR-FILE-008 (PUBLIC, non-guessable slug)
Source     : docs/plans/tenant-maturity-plan.md §7 E.1, E.2, E.4
Priority   : MEDIUM
#### AC-TENANT-029 — [REQ-TENANT-029]
Given a tenant T and a platform operator
When the operator puts a small PNG to `/{T}/logo`
Then the system answers 200 with `logoUrl` starting `/api/v1/public/files/{T's code}/`; `GET /{T}` carries the same `logoUrl`; T's administrator's `GET /api/v1/tenant/me` carries the same `logoUrl`; an anonymous `GET {logoUrl}` answers 200 with the bytes (`image/png`, inline); the `FILE_DOCUMENT` row has `TENANT_ID = T`, owner `CORE_TENANT` / T, module `TENANT`, and is not visible to another tenant;
when the operator puts another image, the old URL answers 404 `FILE_DOCUMENT_NOT_FOUND` and the new one 200; an SVG carrying `<script>`, an executable and an image over 1 MB each answer 400 `TENANT_LOGO_INVALID` and keep the current logo; a plain SVG is accepted and served as `image/svg+xml` with `Content-Disposition: attachment`, `nosniff` and the sandbox CSP;
when the operator deletes the logo, the system answers 204, `logoUrl` becomes null everywhere and the old URL answers 404; an unknown tenant id answers 404 `TENANT_NOT_FOUND`; T's administrator calling any of the three platform endpoints gets 403 `SEC-403-FORBIDDEN`; PLATFORM itself (id 1) can carry a logo

### REQ-TENANT-030 — لون العلامة / Brand colour
Pattern    : event
Statement  : When a platform operator patches `PATCH /api/v1/platform/tenants/{id}/branding` with `brandColor`, the system shall store it upper-cased if it is `#` followed by six hexadecimal digits, clear it if it is null, absent or blank, and refuse anything else with 400 `TENANT_BRAND_COLOR_INVALID` without a change.
Traces     : US-TENANT-012
Entities   : ENT-TENANT-001
Rationale  : POL-TENANT-014; plan §0 D5 ("optional part"): the frontend uses it only when present (plan §8 F2)
Source     : docs/plans/tenant-maturity-plan.md §7 E.1, E.2
Priority   : LOW
#### AC-TENANT-030 — [REQ-TENANT-030]
Given a tenant T
When the operator patches `{"brandColor":"#1a2b3c"}`
Then the system answers 200 `brandColor = "#1A2B3C"`, and `GET /{T}`, T's `/tenant/me` and the public branding show it;
`{"brandColor":"red"}`, `"#12345"`, `"#1234567"`, `"1A2B3C"` each answer 400 `TENANT_BRAND_COLOR_INVALID` and keep `#1A2B3C`;
`{"brandColor":null}` (or `{}`) answers 200 `brandColor = null`; the database refuses a malformed value as well (`CHK_CORE_TENANT_BRAND_COLOR`)

### REQ-TENANT-031 — علامة المستأجر الحالي / Branding of the token's tenant
Pattern    : event
Statement  : When an authenticated caller of either realm asks for `GET /api/v1/tenant/me`, the system shall return the branding of the tenant its token names — `code`, `nameAr`, `nameEn`, `logoUrl`, `brandColor`, `defaultLocale` — and nothing else (no contact, profile, status or audit field); it is read-only and needs no permission; a staff caller with a pending forced password change may call it.
Traces     : US-TENANT-013
Entities   : ENT-TENANT-001
Rationale  : POL-TENANT-007 (only the caller's own tenant), POL-TENANT-014 (the read side is open to every user of the tenant, ADR-TENANT-005)
Source     : docs/plans/tenant-maturity-plan.md §7 E.2, §8 F2
Priority   : MEDIUM
#### AC-TENANT-031 — [REQ-TENANT-031]
Given tenant T with a logo and a brand colour, its staff administrator and one of its customers
When each calls `GET /api/v1/tenant/me` with their token
Then each answers 200 with exactly the keys `code, nameAr, nameEn, logoUrl, brandColor, defaultLocale` (T's values); a token of another tenant answers that tenant's branding; no token → 401; a staff user with a pending forced change → 200; T suspended → 403 `TENANT_SUSPENDED`

### REQ-TENANT-032 — علامة عامة برمز المستأجر / Public branding by tenant code
Pattern    : event
Statement  : When an anonymous caller asks for `GET /api/v1/public/tenants/{tenantCode}/branding`, the system shall resolve the tenant from the path (trimmed, upper-cased; RULE-TENANT-012 source 1) and return its `TenantBrandingResponse`; if the code is unknown the system shall answer 404 `TENANT_NOT_FOUND`, if the tenant is suspended 403 `TENANT_SUSPENDED`, and if the caller's address exceeded its budget 429 `TENANT_BRANDING_RATE_LIMITED` (RULE-TENANT-022).
Traces     : US-TENANT-014; US-TENANT-005 (CHANGED: a second path-tenant path)
Entities   : ENT-TENANT-001
Rationale  : POL-TENANT-008; the login page needs the logo before a token exists (plan §8 F2); the rate limit bounds tenant-code enumeration
Source     : docs/plans/tenant-maturity-plan.md §7 E.2, E.4
Priority   : MEDIUM
#### AC-TENANT-032 — [REQ-TENANT-032]
Given tenant T (ACTIVE, with a logo) and a suspended tenant S
When an anonymous client asks for T's, an unknown code's and S's branding (T's code in lower case included)
Then T → 200 with T's branding (the same `logoUrl` as `/tenant/me`), no contact or profile field; unknown → 404 `TENANT_NOT_FOUND`; S → 403 `TENANT_SUSPENDED`;
and after `capacity` calls in one `period` from one address the next call — whatever the code, unknown codes included — answers 429 `TENANT_BRANDING_RATE_LIMITED`, while another address is still served

### E3. Business rules (§A5) — NEW / CHANGED

### RULE-TENANT-018 — شعار المستأجر / Tenant logo
Scope      : ENT-TENANT-001; FILE ENTITY-FILE-001
Trigger    : on `PUT` / `DELETE /{id}/logo`
Statement  : A tenant has at most one logo: a PUBLIC `FILE_DOCUMENT` of at most 1 048 576 bytes whose content is PNG, JPEG, WebP or SVG (type detected from the bytes, RULE-FILE-008; SVG only if it passes FILE's allow-list, RULE-FILE-009 — plain / optimised SVG: no script, event attribute, external reference, editor metadata, DOCTYPE or duplicate `id`), stored in the tenant's own rows (`TENANT_ID = {id}`, owner `CORE_TENANT` / {id}, module `TENANT`), so its URL carries that tenant's code. Replacing or removing the logo discards the previous document in the same transaction (DELETED + PRIVATE: its URL answers 404 at once, RULE-FILE-010); a refused image changes nothing. The rejection comes from FILE as a value, never an exception, and the tenant raises `TENANT_LOGO_INVALID`.
Data source: FILE's `ImageStoreResult`; ENT-TENANT-001.logoFileId
Message    : ar: "يجب أن يكون الشعار صورة PNG أو JPEG أو WebP أو SVG بسيطة بحجم لا يتجاوز 1 ميغابايت (SVG دون نصوص برمجية أو مراجع خارجية أو بيانات محرّر: صدّره بصيغة SVG بسيطة أو محسّنة)" · en: "The logo must be a PNG, JPEG, WebP or plain SVG image of at most 1 MB (SVG without scripts, external references or editor metadata: export it as plain or optimised SVG)"
Traces     : REQ-TENANT-029
Source     : docs/plans/tenant-maturity-plan.md §7 E.1, E.3; §6 D.4
Decided by : FILE `ImageValidationDomainService` (the verdict) and `TenantDomain.assertLogoAccepted` (the tenant's error); the constants `LOGO_OWNER_TYPE`, `LOGO_MODULE_CODE`, `LOGO_BASE_NAME`, `LOGO_MAX_BYTES`, `LOGO_TYPES` live on `TenantDomain`

### RULE-TENANT-019 — مستأجر المنصة يحمل شعارًا أيضًا / PLATFORM may carry a logo
Scope      : ENT-TENANT-001
Trigger    : on `PUT /1/logo`
Statement  : The PLATFORM tenant may carry a logo like any tenant (its URL is `/api/v1/public/files/PLATFORM/{slug}`); the platform **mark** the frontend shows is a static asset, never a tenant logo, so a tenant without a logo always has a fallback.
Data source: —
Message    : —
Traces     : REQ-TENANT-029
Source     : docs/plans/tenant-maturity-plan.md §7 E.3
Decided by : no refusal exists (`TenantDomain` has no PLATFORM check on branding, unlike RULE-TENANT-005 / -017)

### RULE-TENANT-020 — العلامة التجارية من المنصة فقط / Branding is written by the platform only
Scope      : ENT-TENANT-001
Trigger    : on the logo and branding endpoints
Statement  : The system shall serve `PUT` / `DELETE /{id}/logo` and `PATCH /{id}/branding` only to a PLATFORM operator holding `PLATFORM_TENANT_MANAGE` (decision D5): a tenant administrator, whatever its roles, has no write path to branding in 1.3.0; every user of a tenant reads it through `GET /api/v1/tenant/me`.
Data source: the caller's authorities and tenant
Message    : `SEC-403-FORBIDDEN`
Traces     : REQ-TENANT-029, REQ-TENANT-030
Source     : docs/plans/tenant-maturity-plan.md §0 D5, §7 E.3; ADR-TENANT-005
Decided by : the chain gate on `/api/v1/platform/**` (REQ-TENANT-015) and `@PreAuthorize(PLATFORM_TENANT_MANAGE)` on `TenantService.setLogo`, `removeLogo`, `updateBranding`

### RULE-TENANT-021 — صيغة لون العلامة / Brand colour format
Scope      : ENT-TENANT-001.brandColor
Trigger    : on `PATCH /{id}/branding`
Statement  : `brandColor` shall be null or, after trimming, `^#[0-9A-Fa-f]{6}$`; it is stored upper-case (`@PreUpdate`). A blank value clears it. Anything else → 400 `TENANT_BRAND_COLOR_INVALID`; the database repeats the check (`CHK_CORE_TENANT_BRAND_COLOR`).
Data source: the request's `brandColor`
Message    : ar: "لون العلامة ''{0}'' غير صالح: استخدم الصيغة #RRGGBB (ستة أرقام ست عشرية)" · en: "Brand colour ''{0}'' is invalid: use #RRGGBB (six hexadecimal digits)"
Traces     : REQ-TENANT-030
Source     : docs/plans/tenant-maturity-plan.md §7 E.1, E.2
Decided by : `TenantDomain.assertBrandColorValid`

### RULE-TENANT-022 — حدّ معدّل العلامة العامة / Public branding rate limit
Scope      : `GET /api/v1/public/tenants/{tenantCode}/branding`
Trigger    : on every request to that path
Statement  : The system shall allow each client address (`HttpServletRequest.getRemoteAddr()`, i.e. the proxy-resolved address when the application sets `server.forward-headers-strategy=native` with `server.tomcat.remoteip.internal-proxies`) at most `erp.core.tenant.public-branding-rate-limit.capacity` requests per `period` (defaults 60 per 1 minute, bucket4j, refilled greedily), counted **before** the tenant is resolved — so unknown and suspended codes consume the budget and the endpoint cannot enumerate tenant codes faster than the limit; over the limit → 429 `TENANT_BRANDING_RATE_LIMITED` with a `Retry-After` header (whole seconds until one request is available again). An IPv4 address is its own key; an IPv6 address is keyed by its **/64 prefix** (one subscriber's network, so rotating the interface bits does not reset the budget). Per JVM (a cluster gets one budget per node). The buckets are **bounded** (review round 1): a bucket unused for `period` expires (it would be full again anyway), and at most 10 000 keys are held, the least recently used one evicted first.
Data source: the client address
Message    : ar: "طلبات كثيرة لعلامة المستأجر. يرجى الانتظار قليلًا ثم المحاولة مجددًا" · en: "Too many tenant branding requests. Please wait a moment and try again"
Traces     : REQ-TENANT-032
Source     : docs/plans/tenant-maturity-plan.md §7 E.2 ("rate-limited like customer login, bucket per IP")
Decided by : `PublicBrandingRateLimitFilter` (`com.erp.tenant.security`, first filter of the customer chain, acting on that path only; writes the envelope like `TenantResolutionFilter`)

| Kind | Rule | Delta |
|---|---|---|
| CHANGED | RULE-TENANT-012 (request-tenant resolution order, source 1: path) | the `path-tenant-paths` default gains `/api/v1/public/tenants/{tenantCode}/branding` (still the customer chain only) |
| CHANGED | RULE-TENANT-006 (a suspended tenant is not served) | also refuses the public branding (403 `TENANT_SUSPENDED` from the filter, and from `TenantDomain.assertServed` in the service) and `/tenant/me`; a suspended tenant's logo URL answers 403 like its other public files |

### E4. Error codes — NEW
| Code | HTTP | `Status` | Raised by | Message args |
|---|---|---|---|---|
| `TENANT_LOGO_INVALID` | 400 | `VALIDATION_ERROR` (field error `file`) | `TenantDomain.assertLogoAccepted` | — |
| `TENANT_BRAND_COLOR_INVALID` | 400 | `VALIDATION_ERROR` (field error `brandColor`) | `TenantDomain.assertBrandColorValid` | the value sent |
| `TENANT_BRANDING_RATE_LIMITED` | 429 | (written by the filter) | `PublicBrandingRateLimitFilter` | — |
Referenced: `TENANT_NOT_FOUND` 404 (also thrown by `TenantBrandingService`), `TENANT_SUSPENDED` 403 (also thrown by
`TenantDomain.assertServed` for the two branding reads, so the generated api-docs list it), `VALIDATION_ERROR` 400.
Every new code has an entry in `messages.properties` and `messages_ar.properties` (one `tenant-maturity E` block each).

### E5. ENT-TENANT-001 Tenant — CHANGED (fields); DTOs
| Kind | Field | Logical type | Required | Rule / format | Written by | Label-ar | Label-en |
|---|---|---|---|---|---|---|---|
| NEW | logoFileId | id (soft reference to `FILE_DOCUMENT.ID`, no FK — XM-TENANT-003) | no | not exposed; the API shows `logoUrl` | `PUT` / `DELETE /{id}/logo` (RULE-TENANT-018) | الشعار | Logo |
| NEW | brandColor | text (7) | no | `#RRGGBB`, upper-cased (RULE-TENANT-021) | `PATCH /{id}/branding` | لون العلامة | Brand colour |
New DTOs: `TenantBrandingUpdateRequest { brandColor }` and `TenantBrandingResponse { code, nameAr, nameEn, logoUrl,
brandColor, defaultLocale }` (no id, status, contact, profile or audit field). `TenantResponse` CHANGED: + `logoUrl`,
`brandColor`. `TenantUpdateRequest` unchanged (profile only; the logo has its own endpoints). `logoUrl` is resolved
inside the tenant (`TenantContext.callAs(id)`, a new read-only transaction when the caller's tenant differs), because
FILE's lookup reads the current tenant's documents only; a logo document that is no longer servable yields null.
Physical names, widths, constraint: `../P2/db-script-tenant.md` 1.3.0 addendum (DBF-TENANT-043, -044).

### E6. Dependencies (§A8) — NEW / CHANGED
| Kind | Id | Surface | Owner | Used by |
|---|---|---|---|---|
| NEW | XM-TENANT-003 | SOFT-REF (consumed) `CORE_TENANT.LOGO_FILE_ID` → `FILE_DOCUMENT.ID`, no FK (the `NOTIF_TEMPLATE.ATTACHMENT_FILE_ID` / `SEC_USER.PHOTO_FILE_ID` convention) — written through `FileImageStoreApi.storePublicImage` / `discard` (FILE XM-FILE-002), read through `FileDocumentLookupApi.publicUrl` (XM-FILE-001), always inside `TenantContext.callAs(id)` | FILE | logo endpoints; every `TenantResponse` / `TenantBrandingResponse` |
| CHANGED | — | `com.erp.audit.crossmodule.AuditApi` — + action `TENANT_LOGO_CHANGED` (E8) | audit | logo endpoints |
| CONFIG | — | NEW `erp.core.tenant.public-branding-rate-limit.capacity` (60) / `period` (1m); CHANGED `erp.core.tenant.path-tenant-paths` default + `/api/v1/public/tenants/{tenantCode}/branding` | — | RULE-TENANT-012, -022 |
| EXPOSED | — | `GET /api/v1/tenant/me`, `GET /api/v1/public/tenants/{tenantCode}/branding` (`TenantBrandingResponse`) — HTTP, for the frontend shell and login page (plan §8 F2) | — | frontend |
The write runs like package B's admin-reset: `TenantService.setLogo` / `removeLogo` are not `@Transactional` (a
transaction of the PLATFORM request would bind the PLATFORM Hibernate session, and the image would land in PLATFORM's
rows); each opens one transaction inside `callAs(id)` (`TransactionTemplate`, `REQUIRES_NEW`), so the stored image,
the `CORE_TENANT` update, the discard of the previous document and the audit rows commit or roll back together.

### E7. Serving an SVG logo — facts and decision (for the frontend)
| Kind | Item |
|---|---|
| fact | PNG, JPEG and WebP logos are served `inline` (step 07's inline list). An SVG logo is served `Content-Type: image/svg+xml`, `Content-Disposition: attachment`, `X-Content-Type-Options: nosniff`, `Content-Security-Policy: sandbox; default-src 'none'` (SVG is deliberately not inline-safe, FILE srs 1.3.0 §3): an `<img src="{logoUrl}">` renders it (the disposition only affects navigation, and an SVG in `<img>` runs no script and loads nothing), opening the URL in a tab downloads it. |
| decision | The inline list is **not** changed (no FILE ADR): SVG stays accepted for logos (plan §7 E.3) because `<img>` is the only way the frontend shows a logo (plan §8 F2 `<TenantLogo>`, F3 preview). The frontend must render `logoUrl` only through `<img>` (never `<object>`, `<embed>`, `<iframe>` or inline markup). Raster is preferred where the file is opened directly or reused outside the page (e-mail, favicon): the PLATFORM_TENANTS upload hint should say "PNG or WebP recommended; SVG must be plain or optimised". |
| decision | No server-side resize (plan §6 D.4); the frontend constrains the height (28 px in the shell). |

### E8. Audit
| Operation | `CORE_AUDIT_EVENT` |
|---|---|
| `PUT /{id}/logo`, `DELETE /{id}/logo` | `TENANT_LOGO_CHANGED` (actor = the operator's username, realm `STAFF`, `actorUserId` null, entity `CORE_TENANT` / {id}, summaries "logo set" / "logo removed" with the tenant code and the document id) recorded **twice in the same transaction**: once in tenant {id} (its administrators see who changed their branding) and once in PLATFORM (`tenantId = 1`, the operator's trail — B's `TENANT_ADMIN_RESET` precedent); one row only when {id} is PLATFORM. The `@Audited` `UPDATE` row of `CORE_TENANT` (`logoFileId`) lands in tenant {id}, the tenant the change was made in (the audit module's rule for global entities). A removal of a missing logo records nothing. |
| `PATCH /{id}/branding` | the `@Audited` `UPDATE` row of `CORE_TENANT` (`brandColor`), in PLATFORM; no explicit action |
| `/tenant/me`, public branding | none (reads) |

### E9. SCR-REQ-TENANT-001 PLATFORM_TENANTS — CHANGED
| Kind | Section | Delta |
|---|---|---|
| CHANGED | B1 Operations | + set / replace / remove a tenant's logo, set / clear its brand colour |
| CHANGED | B3 Input | branding row in the tenant detail: logo file input (PNG / JPEG / WebP / plain SVG, ≤ 1 MB) with preview, remove (sensitive, confirmation), optional brand colour `#RRGGBB` |
| unchanged | B4 Access | same two actions; the new endpoints need `PLATFORM_TENANT_MANAGE` (D5, ADR-TENANT-005) |
| CHANGED | B5 API expectations | + the three platform rows of E1 |

### E10. Decisions and deliberate differences from the plan
| Kind | Note |
|---|---|
| NEW (ADR) | ADR-TENANT-005 — the logo is set by the platform administrator from `PLATFORM_TENANTS`; no tenant self-service screen in 1.3.0 (decision D5). |
| CHANGED (plan) | Migration `V20__tenant_branding.sql` (plan: V18; packages D and B took V16 … V19). |
| NEW (decision) | DELETE answers 204 without a body (plan §7 E.2; SEC's photo removal; build-create-controller A.6.5); removing a missing logo is not an error. G's "200 + count" precedent applies to revokes that report a cascade count, which a logo removal has not. |
| NEW (decision) | `brandColor` is upper-cased on save (one spelling per colour); blank clears like null. |
| NEW (decision) | `TENANT_LOGO_CHANGED` in both the target tenant and PLATFORM (E8); the brand colour relies on the entity audit. |
| NEW (decision) | The rate limit is a filter keyed by client address only (not by tenant code), counted before the tenant lookup (RULE-TENANT-022): a limiter inside the controller would never see the 404 / 403 answers the tenant filter gives, and keying by code would not bound enumeration. A new error code `TENANT_BRANDING_RATE_LIMITED` (429; the plan named none; `CUSTOMER_LOGIN_RATE_LIMITED` is SEC's and speaks of sign-in). Default 60 per minute: a login page asks once per tenant code it settles on. |
| NEW (decision) | `/api/v1/tenant/me` is realm-neutral on the core chain rather than moved to the customer chain: a STAFF token would be refused there, as a CUSTOMER token is on the core chain by default (SEC realm rule CHANGED, srs-sec.md 1.3.0 §11). Review round 1: for `GET` only, like the forced-change exemption. |
| NEW (review round 1) | Rate-limit buckets: IPv6 keyed by /64, entries expire after `period` unused, at most 10 000 keys (LRU), `Retry-After` on 429 — no new dependency (Caffeine is not on erp-core's classpath; an access-ordered map does it). `LoginRateLimiter` keeps its clear-all-above-10 000 behaviour (SEC, a follow-up, not changed by E). |
| NEW (decision) | The two read endpoints live in a new `TenantBrandingController` (`/api/v1`) with service `TenantBrandingService` (`getMyTenantBranding` `isAuthenticated()`, `getPublicTenantBranding` `permitAll()` — the FILE public-download precedent); the three writes extend `PlatformTenantController` / `TenantService`. Controller method names are unique across the application (`setTenantLogo`, `removeTenantLogo`, `updateTenantBranding`, `getMyTenantBranding`, `getPublicTenantBranding`) so springdoc's operation ids of other modules do not shift. |

### E11. Frontend impact (read by the frontend repository — plan §8 F2, F3)
| Kind | Item |
|---|---|
| NEW | `GET /api/v1/tenant/me` (after login, any realm) and `GET /api/v1/public/tenants/{code}/branding` (login page, no token; 404 → platform mark only; 429 → platform mark only, no toast) → `TenantBrandingResponse`. |
| NEW | `PUT` / `DELETE /{id}/logo`, `PATCH /{id}/branding` on `PLATFORM_TENANTS` (no new page code, permission or menu entry); `TenantResponse` + `logoUrl`, `brandColor`. |
| NEW | Error codes `TENANT_LOGO_INVALID`, `TENANT_BRAND_COLOR_INVALID`, `TENANT_BRANDING_RATE_LIMITED` (both languages). |
| NOTE | Render `logoUrl` through `<img>` only (E7); a replaced logo gets a new URL (every upload is a new document with a new random slug), so a cache never serves the old file under the new URL; but public files carry `Cache-Control: max-age=86400, public` (FILE step 07), so a removed or replaced logo's **old** URL may still be served for up to 24 h by a browser or CDN cache that already holds it — the shell must always take `logoUrl` from `/tenant/me` / the public branding (not from a remembered URL). While a tenant is suspended its logo URL and its public branding answer 403. |
| NOTE | The public branding answers 429 `TENANT_BRANDING_RATE_LIMITED` with `Retry-After` when an address exceeds its budget: the login page shows the platform mark alone (no toast) and does not retry before `Retry-After`. |

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package C12 — tenant lifecycle events (plan §5 C.1, item 15) and the per-tenant token cut-off with `POST /{id}/revoke-tokens` (plan §5 C.2, item 14)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Ids continue from the highest number ever issued for TENANT (tree and history of both repositories): REQ / AC
032, RULE 022 (RULE-TENANT-012 … 015 reserved for the analysis-coverage work's as-built rules), POL 014, US 014,
XM 003, DBF 044. This block mints **REQ/AC-TENANT-033 … 035, RULE-TENANT-023 … 024** (POL-TENANT-015 in P0,
US-TENANT-015 in P0_5) and decision **ADR-TENANT-002** (the plan's own number, reserved for C.2). No ENT, XM,
SCR-REQ, DBF, permission, page code or migration is added: `CORE_TENANT.TOKENS_INVALID_BEFORE` exists since
`V19__tenant_lifecycle.sql` (DBF-TENANT-042, package B). Platform paths are relative to
`/api/v1/platform/tenants`. Rows marked **FE** are read by the frontend (plan §8 F3). SEC's and NOTIF's sides are
in `../../SEC/P1/srs-sec.md` 1.3.0 §12 and `../../NOTIF/P1/srs.md` 1.3.0 §5.

### C1. Endpoints
The platform row keeps the 1.2.0 gate: authority `PLATFORM_TENANT_MANAGE` on the service plus the chain gate
"caller's tenant = PLATFORM" (REQ-TENANT-015); 401 `SEC-401-INVALID-CREDENTIALS` without a token, 403
`SEC-403-FORBIDDEN` for any other caller.

| Kind | Method | Path | Access | Request | Response (`ApiResponse<T>`) | Errors (HTTP · code) | Traces |
|---|---|---|---|---|---|---|---|
| NEW — **FE** | POST | `/{id}/revoke-tokens` | `PLATFORM_TENANT_MANAGE` | — (no body) | 200 `TenantTokenRevocationResponse { id, code, sessionsTerminated }` — the cut-off instant is not returned | 404 · `TENANT_NOT_FOUND`; 422 · `TENANT_REVOKE_TOKENS_PLATFORM` (`{id}` = PLATFORM) | REQ-TENANT-035; RULE-TENANT-023, -024 |
| CHANGED — **FE** | PATCH | `/{id}/status` | as before | as before | as before | as before; a real transition publishes `TenantSuspendedEvent` / `TenantActivatedEvent` after commit; a suspension ends the tenant's sessions (SEC); an activation cuts off every earlier token | REQ-TENANT-033, -034 |
| CHANGED — **FE** | (filter) | every authenticated request, both chains, `GET /api/v1/tenant/me` included | — | — | — | + 401 · `TENANT_TOKEN_REVOKED` — the token's `iat` lies before its tenant's `TOKENS_INVALID_BEFORE` (written by `TenantResolutionFilter`, like `TENANT_SUSPENDED`) | REQ-TENANT-034; RULE-TENANT-023 |

Order of checks — revoke-tokens: tenant (`TENANT_NOT_FOUND`) → not PLATFORM (`TENANT_REVOKE_TOKENS_PLATFORM`) →
cut-off written (PLATFORM request, own commit) → inside tenant {id}, one transaction: sessions terminated, audit.
Tenant filter, token branch: tenant gone or not ACTIVE (403 `TENANT_SUSPENDED`) → token before the cut-off (401
`TENANT_TOKEN_REVOKED`) → served.

Controller method `revokeTenantTokens` (unique name, so springdoc's operation ids of other modules do not shift);
service `TenantService.revokeTokens`.

### C2. Requirements (§A4) — NEW

### REQ-TENANT-033 — أحداث دورة حياة المستأجر / Tenant lifecycle events
Pattern    : event
Statement  : When a platform operator suspends an ACTIVE tenant or activates a SUSPENDED one and the transaction commits, the system shall publish `TenantSuspendedEvent(tenantId, tenantCode, reason, actor)` or `TenantActivatedEvent(tenantId, tenantCode, actor)` on the core event bus (`com.erp.events`), delivered to `@TransactionalEventListener(AFTER_COMMIT)` listeners only after the commit; re-applying the current status, a refused change (PLATFORM protection, missing reason, unknown tenant) and a rolled-back transaction publish nothing. On `TenantSuspendedEvent` SEC terminates every open session of that tenant, both realms (SEC REQ-SEC-092); while the tenant is not ACTIVE NOTIF neither claims nor re-dispatches its `QUEUED` notifications and, on `TenantActivatedEvent`, re-dispatches them (NOTIF RULE-NOTIF-024).
Traces     : US-TENANT-003 (CHANGED)
Entities   : ENT-TENANT-001
Rationale  : POL-TENANT-002 (CHANGED); the 1.2.0 suspension paused tokens but left `SEC_ACTIVE_SESSION` rows open and let queued mail leave a suspended tenant
Source     : docs/plans/tenant-maturity-plan.md §5 C.1
Priority   : HIGH
#### AC-TENANT-033 — [REQ-TENANT-033]
Given a tenant T whose administrator holds two open staff sessions and whose customer holds one, another tenant U with an open session, and a probe listening after commit
When the operator suspends T with `reason = "  Unpaid invoice  "`
Then one `TenantSuspendedEvent` arrives after the commit with `tenantId = T`, `tenantCode = T's code`, `reason = "Unpaid invoice"`, `actor` = the operator, realm `STAFF`; T's three sessions are terminated (`TERMINATED_BY` = the operator, one `SESSION_TERMINATED` row each in T's `SEC_AUDIT_LOG`), U's session is still open, and every old token of T answers 403 `TENANT_SUSPENDED`;
suspending T again, suspending PLATFORM, suspending without a reason and a status change rolled back by its caller publish nothing;
when the operator activates T, one `TenantActivatedEvent(T, code, operator)` arrives; activating it again publishes nothing;
a notification queued in T while it is suspended stays `QUEUED` with `ATTEMPTS = 0` (no mail), is not re-dispatched by the requeue job, and is `SENT` after the activation

### REQ-TENANT-034 — حدّ إبطال الرموز لكل مستأجر / Per-tenant token cut-off
Pattern    : unwanted behaviour
Statement  : If an access token of either realm reaches a non-public path and its `iat` lies before its tenant's `TOKENS_INVALID_BEFORE` (RULE-TENANT-023), then the system shall refuse the request with 401 `TENANT_TOKEN_REVOKED`, whether the token's session is still open or was already terminated; the cut-off is set to the time of every SUSPENDED → ACTIVE transition (RULE-TENANT-016) and of every `revoke-tokens` call (REQ-TENANT-035); a token issued at or after it — in particular a fresh login right after an activation — is served.
Traces     : US-TENANT-003 (CHANGED), US-TENANT-004 (CHANGED), US-TENANT-015
Entities   : ENT-TENANT-001
Rationale  : POL-TENANT-015; ADR-TENANT-002
Source     : docs/plans/tenant-maturity-plan.md §5 C.2
Priority   : HIGH
Note       : behaviour change — before 1.3.0 a token issued before a suspension worked again after the re-activation.
#### AC-TENANT-034 — [REQ-TENANT-034]
Given a tenant T, a staff token and a customer token of T issued in second S with their sessions open
When `TOKENS_INVALID_BEFORE` is set within second S (after the issue)
Then both tokens are still served (`/api/v1/sec/menu`, `/api/v1/customers/me`, `/api/v1/tenant/me` → 200);
when it is set to second S + 1, each of those calls answers 401 `TENANT_TOKEN_REVOKED` (`/api/v1/tenant/me` for both realms included), a fresh login answers 200 and its token is served, and a login sent with the stale token in `Authorization` answers 200;
and after a suspension and a re-activation of T in a later second, a token issued before the suspension answers 401 `TENANT_TOKEN_REVOKED` while a login right after the activation is served

### REQ-TENANT-035 — إبطال رموز مستأجر / Revoke a tenant's tokens
Pattern    : event
Statement  : When a platform operator posts `POST /api/v1/platform/tenants/{id}/revoke-tokens`, the system shall set the tenant's `TOKENS_INVALID_BEFORE` to now (in the PLATFORM request, its own commit), then, in one transaction of tenant {id}, terminate every open session of the tenant (both realms, SEC) and record `TOKENS_REVOKED` in the tenant's and in PLATFORM's audit logs, and answer the tenant's id, code and the number of terminated sessions; it shall refuse the PLATFORM tenant (422 `TENANT_REVOKE_TOKENS_PLATFORM`, RULE-TENANT-024); a suspended tenant may be revoked (its sessions are already ended, the cut-off is written).
Traces     : US-TENANT-015
Entities   : ENT-TENANT-001; SEC ENT-SEC-010 (through `SecAdminRecoveryApi.terminateAllSessions`)
Rationale  : POL-TENANT-015, POL-TENANT-006; ADR-TENANT-002
Source     : docs/plans/tenant-maturity-plan.md §5 C.2
Priority   : HIGH
#### AC-TENANT-035 — [REQ-TENANT-035]
Given tenant T with a staff and a customer session opened in an earlier second, and tenant U with a session
When the operator posts `/{T}/revoke-tokens`
Then the system answers 200 `{ id: T, code, sessionsTerminated: 2 }` without any cut-off field; both old tokens answer 401 `TENANT_TOKEN_REVOKED`; fresh logins are served; U's token is served; `TOKENS_REVOKED` is recorded once in T and once in PLATFORM (actor = the operator, entity `CORE_TENANT` / T, summaries naming T's code and the session count, no instant); each ended session has one `SESSION_TERMINATED` row in T's `SEC_AUDIT_LOG`;
`/{1}/revoke-tokens` answers 422 `TENANT_REVOKE_TOKENS_PLATFORM` and changes nothing, an unknown id 404 `TENANT_NOT_FOUND`, T's administrator 403 `SEC-403-FORBIDDEN`, no token 401; a suspended tenant answers 200 with `sessionsTerminated = 0`

### C3. Business rules (§A5) — NEW / CHANGED

### RULE-TENANT-023 — حدّ إبطال الرموز لكل مستأجر / Per-tenant token cut-off
Scope      : ENT-TENANT-001.tokensInvalidBefore; every authenticated request
Trigger    : on every request carrying a signature-valid bearer token, on a non-public path of either chain
Statement  : A token is revoked when its tenant's `TOKENS_INVALID_BEFORE` is set and the token's `iat` (whole seconds) is **less than** the cut-off truncated to the second (a token issued in the cut-off's own second is served; one without `iat` is revoked). The tenant filter refuses a revoked token with 401 `TENANT_TOKEN_REVOKED` (security context cleared) — for a token that authenticated and for one SEC dropped (terminated session, inactive user), because SEC exposes the facts of every signature-valid token (`com.erp.tenant.TenantTokenFacts(tenantId, issuedAt)`, request attribute). The tenant's status is checked first: a token whose tenant is gone or suspended answers 403 `TENANT_SUSPENDED`, authenticated or not. A public path (`erp.core.security.public-paths`, the customer chain's unauthenticated paths) ignores a stale token, so a client can sign in again with an old `Authorization` header. The cut-off is written by RULE-TENANT-016 (activation) and REQ-TENANT-035 (revoke-tokens), never exposed (responses, audit).
Data source: ENT-TENANT-001.statusCode, .tokensInvalidBefore, read as PLATFORM; the token's `tid` and `iat`
Message    : ar: "لم يعد رمز الدخول صالحًا لهذا المستأجر: يرجى تسجيل الدخول مجددًا" · en: "This sign-in is no longer valid for this tenant: please sign in again"
Traces     : REQ-TENANT-034, REQ-TENANT-035
Source     : docs/plans/tenant-maturity-plan.md §5 C.2; ADR-TENANT-002
Decided by : `TenantDomain.isTokenRevoked(issuedAt, tokensInvalidBefore)`; applied by `TenantResolutionFilter`

### RULE-TENANT-024 — مستأجر المنصة لا تُبطَل رموزه / PLATFORM's tokens are not revoked
Scope      : ENT-TENANT-001
Trigger    : on `/{id}/revoke-tokens`
Statement  : The system shall refuse `revoke-tokens` for the PLATFORM tenant (id 1) with 422 `TENANT_REVOKE_TOKENS_PLATFORM` and change nothing: it would sign every platform operator out, the caller included (PLATFORM is never suspended either, RULE-TENANT-005); a platform operator's session is ended through SEC's session API. Every other tenant may be revoked, ACTIVE or SUSPENDED.
Data source: ENT-TENANT-001.id
Message    : ar: "لا يمكن إبطال رموز مستأجر المنصة ''{0}'': سيُخرج ذلك جميع مشغّلي المنصة" · en: "The tokens of the platform tenant ''{0}'' cannot be revoked: it would sign every platform operator out"
Traces     : REQ-TENANT-035
Source     : docs/plans/tenant-maturity-plan.md §5 C.2 (the plan leaves PLATFORM open; decided here, ADR-TENANT-002)
Decided by : `TenantDomain.assertTokenRevocationAllowed`

| Kind | Rule | Delta |
|---|---|---|
| CHANGED | RULE-TENANT-006 (a suspended tenant is not served) | + on `TenantSuspendedEvent` SEC terminates every open session of the tenant (both realms; the rows stayed open in 1.2.0), and NOTIF does not claim or re-dispatch the tenant's `QUEUED` notifications while it is not ACTIVE (status set unchanged). Because the sessions are now terminated, SEC drops such a token; the tenant filter still answers 403 `TENANT_SUSPENDED` for it on a non-public path (RULE-TENANT-023's "SEC dropped" branch), so the 1.2.0 answer is kept. |
| CHANGED | RULE-TENANT-016 (suspension reason; activation) | the `TOKENS_INVALID_BEFORE` an activation writes is now enforced (RULE-TENANT-023); the transition also publishes `TenantActivatedEvent` / a suspension `TenantSuspendedEvent` (REQ-TENANT-033) |
| CHANGED | RULE-TENANT-012 (request-tenant resolution order) | source 2 (the token) also checks the cut-off; a signature-valid token that SEC did not authenticate is checked for its tenant's status and cut-off on a non-public path before the header source is tried |
| CHANGED | RULE-TENANT-015 (no caching) | holds for `TenantLookupApi.isActive` too: a status change is effective at once for NOTIF's claim |

### C4. Error codes — NEW
| Code | HTTP | `Status` | Raised by | Message args |
|---|---|---|---|---|
| `TENANT_TOKEN_REVOKED` | 401 | (written by the filter) | `TenantResolutionFilter` (RULE-TENANT-023) | — |
| `TENANT_REVOKE_TOKENS_PLATFORM` | 422 | `BUSINESS_RULE_VIOLATION` | `TenantDomain.assertTokenRevocationAllowed` | tenant code |
`TENANT_PLATFORM_PROTECTED` is not reused (its message is about suspension), the `TENANT_ADMIN_RESET_PLATFORM`
precedent. Every new code has an entry in `messages.properties` and `messages_ar.properties` (one `tenant-maturity
C12` block each). `TENANT_TOKEN_REVOKED` is answered by a filter, so the generated api-docs do not list it per
endpoint (like `TENANT_SUSPENDED` for tokens); the revoke-tokens `@Operation` names it.

### C5. ENT-TENANT-001 Tenant — CHANGED (field use); DTOs
| Kind | Field | Delta |
|---|---|---|
| CHANGED | tokensInvalidBefore | now **enforced** (RULE-TENANT-023) and written by `revoke-tokens` too (`Tenant.revokeTokens(Instant)`); still system-only and never exposed |
New DTO: `TenantTokenRevocationResponse { id, code, sessionsTerminated }`. No request DTO (the endpoint has no
body). `TenantResponse` unchanged (no cut-off field).

### C6. Events — NEW (`com.erp.events`, core catalogue 11 → 13)
| Event | Payload (besides the `DomainEvent` envelope) | Constructor | Publisher | Consumers |
|---|---|---|---|---|
| `TenantSuspendedEvent` | `tenantCode`, `reason` (the stored, trimmed reason); `getTenantId()` = the suspended tenant, `getActor()` = the operator, realm `STAFF` | explicit (the fact belongs to the suspended tenant, the publisher is the PLATFORM operator — the `TenantCreatedEvent` shape) | `TenantService.updateStatus`, ACTIVE → SUSPENDED only, inside the transaction after the row is flushed | SEC `TenantSuspendedSessionListener` (REQ-SEC-092) |
| `TenantActivatedEvent` | `tenantCode`; `getTenantId()` = the activated tenant, `getActor()` = the operator, realm `STAFF` | explicit (as above) | `TenantService.updateStatus`, SUSPENDED → ACTIVE only | NOTIF `NotificationTenantActivationListener` (RULE-NOTIF-024) |
Plain values only (no entity, no secret). The plan's payload name `code` is `tenantCode` here, as on
`TenantCreatedEvent`. `revoke-tokens` publishes no event (SEC is called directly, so the response can carry the
count).

### C7. Dependencies (§A8) — NEW / CHANGED
| Kind | Id | Surface | Owner | Used by |
|---|---|---|---|---|
| CHANGED | XM-TENANT-001 | `TenantLookupApi` + `boolean isActive(Long tenantId)` — false for null, an unknown id or a tenant not ACTIVE; reads `CORE_TENANT.STATUS_CODE` (DBF-TENANT-005) by id; uncached (gov-enforce-caching-rules: a state lifecycle is never cacheable, and the claim must see a status change at once); usable without a current tenant (reads as PLATFORM, the `TenantResolutionFilter` precedent) | TENANT (exposed) | NOTIF (claim and requeue, RULE-NOTIF-024) |
| NEW | — | `com.erp.tenant.TenantTokenFacts(Long tenantId, Instant issuedAt)` + `REQUEST_ATTRIBUTE` — root-package public type, set by SEC's `JwtAuthenticationFilter` for every signature-valid token, read by `TenantResolutionFilter` | TENANT (exposed, root package) | SEC (writes it) |
| NEW (consumed) | — | `com.erp.sec.crossmodule.SecAdminRecoveryApi` + `int terminateAllSessions()` (SEC REQ-SEC-093) — inside `TenantContext.callAs(id)`, joins that transaction | SEC | revoke-tokens |
| NEW (published) | — | `TenantSuspendedEvent`, `TenantActivatedEvent` (C6) | events | SEC, NOTIF, applications |
| CHANGED | — | `com.erp.audit.crossmodule.AuditApi` — + action `TOKENS_REVOKED` (C8) | audit | revoke-tokens |
`TenantService.revokeTokens` is not `@Transactional` (the B / E precedent): the cut-off is written by a
`TransactionTemplate` of the PLATFORM request (the `CORE_TENANT` row is global; `updateStatus` writes it there
too), then one `REQUIRES_NEW` transaction inside `callAs(id)` ends the sessions and records both audit rows.
The cut-off commits first on purpose: if the session step fails, the tokens are already refused.

### C8. Audit, sessions
| Operation | `CORE_AUDIT_EVENT` | Sessions (SEC) |
|---|---|---|
| suspend (C.1) | as before (`UPDATE` row of `CORE_TENANT` in PLATFORM) | every open session of the tenant terminated after commit, `TERMINATED_BY` = the operator; one `SESSION_TERMINATED` row each in the tenant's `SEC_AUDIT_LOG` (no actor user: the operator is not a user of that tenant; the details name the operator) |
| activate | as before; the cut-off is not recorded (entity-audit denylist word `token`) | — (none are open) |
| revoke-tokens | `TOKENS_REVOKED` recorded **twice in the same transaction of tenant {id}**: once in tenant {id} and once in PLATFORM (`tenantId = 1`): actor = the operator's username, realm `STAFF`, `actorUserId` null, entity `CORE_TENANT` / {id}, summaries naming the tenant code and the number of sessions terminated — never the cut-off instant. The cut-off's own `UPDATE` writes no entity-audit row (its only change is a denylisted field). | every open session of the tenant, both realms, terminated in that transaction, `SESSION_TERMINATED` rows as for a suspension |

### C9. SCR-REQ-TENANT-001 PLATFORM_TENANTS — CHANGED
| Kind | Section | Delta |
|---|---|---|
| CHANGED | B1 Operations | + revoke a tenant's tokens (sign every user of the tenant out) |
| CHANGED | B3 Input | a "revoke tokens" action on a tenant row other than PLATFORM (sensitive: confirmation naming the tenant; the result shows `sessionsTerminated`) |
| unchanged | B4 Access | same two actions; the endpoint needs `PLATFORM_TENANT_MANAGE` (D5) |
| CHANGED | B5 API expectations | + `POST /{id}/revoke-tokens` (C1) |

### C10. Decisions and deliberate differences from the plan
| Kind | Note |
|---|---|
| NEW (ADR) | ADR-TENANT-002 — per-tenant cut-off instead of a `jti` denylist; second precision, same-second tokens served; the token facts as a request attribute; PLATFORM refused. |
| CHANGED (plan) | Plan C.2 "the JWT filter exposes `iat` on the authentication details" → a request attribute `TenantTokenFacts(tenantId, issuedAt)` set for every signature-valid token (ADR-TENANT-002): the tenant module may not read SEC's `AuthRealm`, and after C.1 the token of a suspended or revoked tenant no longer authenticates (its session is terminated), so the facts are needed for a token SEC dropped. |
| NEW (decision) | The tenant filter checks a dropped token's tenant on non-public paths (403 `TENANT_SUSPENDED` / 401 `TENANT_TOKEN_REVOKED`): without it, C.1's session termination would turn the 1.2.0 answer for an issued token of a suspended tenant (403 `TENANT_SUSPENDED`, TC-CORE-TENANT-022) into a bare 401, and the cut-off code would never be seen after a re-activation. Public paths are exempt so a login with a stale header still works. |
| NEW (decision) | Revoke-tokens refuses PLATFORM (RULE-TENANT-024, new code `TENANT_REVOKE_TOKENS_PLATFORM`); a suspended tenant may be revoked. |
| CHANGED (plan) | Response of revoke-tokens: `TenantTokenRevocationResponse { id, code, sessionsTerminated }` (the reference analysis proposed `TenantResponse`): the count is the operation's result (the admin-reset precedent), and the tenant record itself does not change visibly. 200 with a body, `Status.UPDATED`. |
| NEW (decision) | `TenantSuspendedEvent` / `TenantActivatedEvent` carry `tenantCode` (the plan's `code`), like `TenantCreatedEvent`. SEC's listener runs **synchronously** after commit (in the operator's request, inside `callAs(tenantId)` with a `REQUIRES_NEW` transaction), so the sessions are closed when the PATCH answers; a failure there is logged and never undoes the suspension (the token is refused by the tenant filter anyway, and the cut-off of a later activation covers a session that stayed open). |
| NEW (decision) | NOTIF re-dispatches a re-activated tenant's queued notifications on `TenantActivatedEvent` (asynchronous listener) rather than waiting for the requeue job, which is off by default (`erp.core.notif.requeue.enabled=false`) — the plan said "the job simply does not claim them while suspended" and left their delivery to the job (NOTIF srs.md 1.3.0 §5). |
| NEW (decision) | No new XM-TENANT id: XM-TENANT-001 is CHANGED (`isActive`), the consumed SEC method is recorded here (B7 precedent), the token facts type is root-package public API like `TenantContext`. |

### C11. Frontend impact (read by the frontend repository — plan §8 F3)
| Kind | Item |
|---|---|
| NEW | `POST /{id}/revoke-tokens` on `PLATFORM_TENANTS` (no new page code, permission or menu entry); not offered for PLATFORM (422 `TENANT_REVOKE_TOKENS_PLATFORM`); show `sessionsTerminated` after success. |
| NEW | Error codes `TENANT_TOKEN_REVOKED` (401, any request of a signed-in user) and `TENANT_REVOKE_TOKENS_PLATFORM` (both languages). On 401 `TENANT_TOKEN_REVOKED` the shell clears the session and returns to the login page (as for any 401), optionally saying "your organisation's sessions were ended". |
| CHANGED | After a tenant is re-activated, its users sign in again (their earlier tokens answer 401 `TENANT_TOKEN_REVOKED`); a suspension ends their sessions at once. |
