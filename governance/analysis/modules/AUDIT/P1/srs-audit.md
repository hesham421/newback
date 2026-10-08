# SRS — سجل التدقيق العام / Audit (AUDIT)
══════════════════════════════════════════════════════════════════
Module : AUDIT   Version : v1 (as-built baseline)   Profile : erp
Inputs : prd-audit, module-registry-audit, business-policies-audit; the code at main @ 19b19a4 (erp-core 1.2.0 behaviour)
Counts : ENT 1 · REQ 21 · AC 21 · RULE 13 · SCR-REQ 2 · API 1 (+ 2 report endpoints owned by REPORT, + 1 in-process) · XM 4 · ADR 3
══════════════════════════════════════════════════════════════════

Written from the code, not before it. Every REQ / AC / RULE names the code location it was read from;
the id → location index is `registry-srs-audit.md`. Java paths are relative to
`erp-core/src/main/java/com/erp/`, migrations to `erp-core/src/main/resources/db/migration/core/`,
`file:line` at main @ 19b19a4. Test-case ids `TC-CORE-AUDIT-NNN` / `TC-CORE-REPORT-NNN` are those of
`docs/test-api/core-test-plan.md`; JUnit names are those of `erp-core/src/test/java/com/erp/audit/` and
`erp-core/src/test/java/com/erp/sec/SecAuditIntegrationTest.java`.

# PART A — MODULE FOUNDATION

## A1 — Document information
| Item | Value |
|---|---|
| Module | AUDIT — سجل التدقيق العام / Audit (package `com.erp.audit`, registry module `AUDIT`) |
| Feature code | AUDIT |
| Version | v1 (as built in erp-core 1.2.0) |
| Date | 2026-10-07 |
| Status | AS-BUILT baseline |
| Prepared by | analysis lane, from the code |
| Decisions applied count | 3 (ADR-AUDIT-001, -002, -003) |

## A2 — Functional context

**In scope:** سجل تدقيق واحد خاص بكل مستأجر (`CORE_AUDIT_EVENT`)، كتابة الحقائق الصريحة عبر
`AuditApi.record`، التسجيل التلقائي لتغييرات حقول الكيانات المعلَّمة بـ `@Audited`، حجب الحقول الحسّاسة،
القيم الافتراضية للمنفذ والنطاق والوقت وعنوان العميل، الاستعلام الورقي للموظفين، تقرير قائمة الأحداث،
وظيفة الاحتفاظ، تسجيل أحداث حساب SEC في السجل نفسه.

**Out of scope:** الكتابة غير المتزامنة أو عبر الأحداث، نقاط نهاية الكتابة أو التعديل أو الحذف، تحديد
معرّف المستخدم تلقائيًا من المبدأ، جدولة الاحتفاظ من النواة، الثقة في `X-Forwarded-For`، تعديل
`SEC_AUDIT_LOG` — [business-policies-audit.md → SCOPE EXCEPTIONS; ADR-AUDIT-001, ADR-AUDIT-003].

**Module function (one paragraph):** وحدة AUDIT هي الإجابة الواحدة في المنصة عن "من غيّر ماذا ومتى":
تجمع الحقائق الصريحة وتغييرات الحقول من كل الوحدات والتطبيقات في خط زمني واحد لكل مستأجر، لا يحمل
أسرارًا ولا يصف إلا ما التُزم به فعلًا، ولا يقرأه إلا الموظفون المخوّلون.

**Detailed description (workflow narrative, roles):** تعلّم الوحدة المالكة كيانها بـ `@Audited` فتُسجَّل
تغييرات حقوله تلقائيًا عند كل إدراج أو تعديل أو حذف، داخل معاملة التغيير وعلى اتصاله؛ وتسجّل الحقائق
الأخرى (دخول، اعتماد، تصدير) بـ `AuditApi.record` داخل معاملتها. تملأ الوحدة المستأجر والمنفذ ونطاقه
والوقت وعنوان IP ووكيل المستخدم، وتحذف أي حقل حسّاس، وتقصّ القيم الطويلة. يستعرض موظف يحمل
`AUDIT:EVENT:READ` سجل مستأجره الأحدث أولًا مع مرشحات الكيان والمنفذ والإجراء والفترة، ويصدّره حامل
`AUDIT:REPORT:AUDIT_EVENT_LIST` كتقرير. يطبّق التطبيق سياسة احتفاظ إن شاء.

**Current situation:** built (erp-core plan step 10; `NumberSeries` annotated on the rebase onto step 09;
report provider added by step 11).

**General notes:** two `erp.core.audit.*` properties exist (`retention-days` 0, `retention-cron` `-`);
the api-doc generator prints the permission constant's name (`AUDIT_EVENT_READ`) where the code checks the
authority string `AUDIT:EVENT:READ` (`docs/api-docs/audit/endpoints/audit-log.md:16`; audit/service/AuditEventService.java:56).

## A3 — Entities and fields

### ENT-AUDIT-001 — حدث تدقيق / AuditEvent
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| log (tenant-scoped, append-only; JPA read model, JDBC writes) | PRIVATE — no table references it | No — `id` only | create (in-process only: `AuditApi.record`, the `@Audited` listener), read, search (query API, report); delete only by retention; no update | written by SEC (`AuditApi`, XM-AUDIT-002) and by the ten `@Audited` entities of seven modules (XM-AUDIT-003); FK `TENANT_ID` → `CORE_TENANT` (XM-AUDIT-001) | audit/entity/AuditEvent.java:29-85; V15__audit_schema.sql:14-46 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| id | number | yes (system) | `nextval('SEQ_CORE_AUDIT_EVENT')` in the insert | primary key | المعرف الفريد | Unique identifier |
| tenantId | number | yes (system) | entity's own `TENANT_ID`, else `TenantContext.require()` (RULE-AUDIT-007) | never a client field | — | — |
| occurredAt | date-time | yes (system) | entry value or now | stored as UTC wall-clock `TIMESTAMP` (ADR-AUDIT-003); sort key, newest first | وقت الحدوث | When it happened |
| actor | text (≤ 100) | yes (system) | entry value, else the principal's name, else `system` | cut at 100 | اسم المستخدم المنفذ | Username of the actor |
| actorRealm | code (≤ 16) | yes (system) | `STAFF` \| `CUSTOMER` \| `SYSTEM` (A6) | the `DomainEvent` rule | نطاق المنفذ | Actor realm |
| actorUserId | number | no | set by the caller only (SEC passes it) | no FK, no default | معرف المستخدم المنفذ | Actor user id |
| action | code (3–64) | yes | `^[A-Z_]{3,64}$` (RULE-AUDIT-001); standard `CREATE`, `UPDATE`, `DELETE`, `STATUS_CHANGE`, `LOGIN`, `LOGOUT`, `PASSWORD_RESET` | exact-match filter | الإجراء | Action |
| entityType | text (≤ 128) | no | `@Audited.entityType` (by convention the table name) or the caller's value | cut at 128; exact-match filter | نوع الكيان | Entity type |
| entityId | text (≤ 64) | no | the entity's id as text | cut at 64; exact-match filter | معرف الكيان | Entity id |
| summaryAr | text (≤ 1000) | no | listener: "إنشاء / تعديل / حذف `<type>` رقم `<id>`"; caller's value otherwise | cut at 1000 | الملخص بالعربية | Summary (Arabic) |
| summaryEn | text (≤ 1000) | no | listener: "Created / Updated / Deleted `<type>` #`<id>`"; caller's value otherwise | cut at 1000 | الملخص بالإنجليزية | Summary (English) |
| changes | json | no | array of `{field, old, new}` after redaction (RULE-AUDIT-004); null when empty | `JSONB`; returned raw | تغييرات الحقول | Field changes |
| ip | text (≤ 64) | no | entry value, else `getRemoteAddr()` of the current request | null outside a request | عنوان العميل | Client IP |
| userAgent | text (≤ 256) | no | entry value, else the `User-Agent` header | null outside a request | متصفح العميل | Client user agent |
| reference | text (≤ 100) | no | caller's correlation id | cut at 100 | مرجع الربط | Correlation reference |
| createdBy, createdAt, updatedBy, updatedAt | system | no | bound to the actor / now on insert; never updated | `GlobalAuditableEntity` columns | تاريخ الإنشاء / أنشئ بواسطة / تاريخ التحديث / حُدّث بواسطة | Created timestamp / Created by / Updated timestamp / Updated by |
| version | system | yes | always 0 | rows are never updated | — | — |
Source: audit/entity/AuditEvent.java:40-84; audit/crossmodule/AuditEntry.java:41-59;
audit/service/AuditRecordingService.java:61-80; audit/service/AuditEventStore.java:62-90;
audit/dto/AuditEventResponse.java:15-75 (labels: the `@Schema` descriptions);
audit/listener/AuditedEntityListener.java:133-134 (listener summaries).

## A4 — Functional requirements (EARS) and acceptance criteria

### REQ-AUDIT-001 — تسجيل إدخال صريح / Record an explicit entry
Pattern    : event
Statement  : When a module calls `AuditApi.record(entry)` with a valid action, the system shall fill every default the entry leaves null, drop its sensitive changes, cut its values, and insert one row on the connection of the current Spring transaction (committing on its own when there is none).
Traces     : US-AUDIT-002
Entities   : ENT-AUDIT-001
Rationale  : POL-AUDIT-002, POL-AUDIT-006, POL-AUDIT-009
Source     : audit/crossmodule/AuditApi.java:40-47; audit/crossmodule/AuditApiImpl.java:19-22; audit/service/AuditRecordingService.java:46-51, :60-81; audit/service/AuditEventStore.java:53-59
Priority   : HIGH
#### AC-AUDIT-001 — [REQ-AUDIT-001]
Given a staff caller `admin` of tenant A inside a transaction, and an entry with only `action = "APPROVE"`, `entityType = "SALES_INVOICE"`, `entityId = "42"`
When `record` is called and the transaction commits
Then one row exists in tenant A with `actor = admin`, `actorRealm = STAFF`, `occurredAt` ≈ now, `action = APPROVE`, `entityType = SALES_INVOICE`, `entityId = 42` (`AuditApiIntegrationTest.record_fillsDefaults_fromTheTenantAndSecurityContext`)

### REQ-AUDIT-002 — رفض إجراء غير صالح / Reject an invalid action
Pattern    : unwanted
Statement  : If an entry's action is null or does not match `^[A-Z_]{3,64}$`, then the system shall reject it with `AUDIT_ACTION_INVALID` and write nothing.
Traces     : US-AUDIT-002
Entities   : ENT-AUDIT-001
Rationale  : POL-AUDIT-005
Source     : audit/domain/AuditEventDomain.java:21-22, :37-44; audit/service/AuditRecordingService.java:62
Priority   : HIGH
#### AC-AUDIT-002 — [REQ-AUDIT-002]
Given entries with action `ab`, `has space` and null
When each is recorded
Then each fails with `AUDIT_ACTION_INVALID` (400 `VALIDATION_ERROR` over HTTP; message "Audit action ''{0}'' is invalid: use 3 to 64 upper-case letters or underscores") and no row is written (`AuditApiIntegrationTest.record_withAnInvalidAction_isRejected`; `AuditEventDomainTest`)

### REQ-AUDIT-003 — الفشل السريع بلا مستأجر / Fail fast without a tenant
Pattern    : unwanted
Statement  : If an entry carries no `tenantId` and no tenant is current, then the system shall fail with `TENANT_CONTEXT_MISSING` and write nothing; an explicit `tenantId` wins over the current tenant.
Traces     : US-AUDIT-002
Entities   : ENT-AUDIT-001
Rationale  : POL-AUDIT-001
Source     : audit/service/AuditRecordingService.java:65; tenant/TenantContext.java:47-53
Priority   : HIGH
#### AC-AUDIT-003 — [REQ-AUDIT-003]
Given a thread with no tenant, then the same thread running as tenant A with an entry whose `tenantId` is B
When `record` is called in each case
Then the first fails with `TENANT_CONTEXT_MISSING` and the second writes the row in tenant B (`AuditApiIntegrationTest.record_withoutAnyTenant_failsFast_andAnExplicitTenantWins`)

### REQ-AUDIT-004 — لا صف لتغيير متراجع / No row for a rolled-back change
Pattern    : unwanted
Statement  : If the transaction in which an entry was recorded, or in which an `@Audited` entity was flushed, rolls back, then the system shall leave no audit row; if it commits, the row stays.
Traces     : US-AUDIT-002, US-AUDIT-003
Entities   : ENT-AUDIT-001
Rationale  : POL-AUDIT-002
Source     : audit/crossmodule/AuditApi.java:10-13; audit/service/AuditEventStore.java:23-32; audit/listener/AuditedEntityListener.java:39-42, :120-123, :138
Priority   : HIGH
#### AC-AUDIT-004 — [REQ-AUDIT-004]
Given a transaction that records an entry (or updates an audited user) and then rolls back, and another that commits
When the log is queried
Then no row exists for the first and one for the second (`AuditApiIntegrationTest.record_inARolledBackTransaction_leavesNoRow_andInACommittedOne_staysWritten`; `SecAuditIntegrationTest.auditedChange_inARolledBackTransaction_…`)

### REQ-AUDIT-005 — تسجيل إنشاء كيان مدقَّق / Record the creation of an audited entity
Pattern    : event
Statement  : When Hibernate inserts an entity annotated with `@Audited`, the system shall write one `CREATE` row with `entityType` = the annotation's value (the JPA entity name when blank), `entityId` = the id, summaries "إنشاء `<type>` رقم `<id>`" / "Created `<type>` #`<id>`", and `changes` listing every non-null recordable property with `old` null.
Traces     : US-AUDIT-003
Entities   : ENT-AUDIT-001
Rationale  : POL-AUDIT-009
Source     : audit/listener/AuditedEntityListener.java:63-76, :125-150
Priority   : HIGH
#### AC-AUDIT-005 — [REQ-AUDIT-005]
Given tenant A's administrator creates user `alice`
When `GET /api/v1/audit/events?entityType=SEC_USER&entityId=<alice id>&action=CREATE` is called
Then exactly one row answers, with `actor = ta-admin`, `actorRealm = STAFF`, `ip` set, and `changes[*].field` ⊇ {`username`, `email`, `fullNameAr`, `fullNameEn`, `statusCode`, `realm`, `isActiveFl`} and none of `passwordHash`, `lastLoginAt`, `createdBy`, `createdAt`, `updatedBy`, `updatedAt`, `version`, `tenantId` (TC-CORE-AUDIT-001; `AuditLogApiIntegrationTest.creatingAUser_writesOneCreateRow_withoutThePasswordHash`)

### REQ-AUDIT-006 — تسجيل تعديل كيان مدقَّق / Record the update of an audited entity
Pattern    : event
Statement  : When Hibernate updates an `@Audited` entity, the system shall write one `UPDATE` row whose `changes` list only the recordable properties whose value differs from the loaded snapshot (or Hibernate's dirty set when there is no snapshot); when no recordable property differs, the system shall write no row.
Traces     : US-AUDIT-003
Entities   : ENT-AUDIT-001
Rationale  : POL-AUDIT-009
Source     : audit/listener/AuditedEntityListener.java:78-100, :185-188
Priority   : HIGH
#### AC-AUDIT-006 — [REQ-AUDIT-006]
Given `alice` is updated with a new e-mail (unchanged), `fullNameAr` and `fullNameEn`, and separately with a new password only
When the log is queried for `action=UPDATE`
Then exactly one `UPDATE` row exists whose `changes[*].field` = {`fullNameAr`, `fullNameEn`}, the `fullNameEn` entry having `old = "Alice"` and `new = "New Name"`; the password-only change wrote no row (TC-CORE-AUDIT-002; `AuditLogApiIntegrationTest.updatingTwoFields_writesOneUpdateRow_withExactlyTwoChanges`; `SecAuditIntegrationTest.passwordOnlyUpdate_writesNoRow_andAnotherFieldDoes`)

### REQ-AUDIT-007 — تسجيل حذف كيان مدقَّق / Record the deletion of an audited entity
Pattern    : event
Statement  : When Hibernate deletes an `@Audited` entity, the system shall write one `DELETE` row whose `changes` list every non-null recordable property of the deleted state with `new` null, summaries "حذف …" / "Deleted …".
Traces     : US-AUDIT-003
Entities   : ENT-AUDIT-001
Rationale  : POL-AUDIT-009
Source     : audit/listener/AuditedEntityListener.java:102-118
Priority   : MEDIUM
#### AC-AUDIT-007 — [REQ-AUDIT-007]
Given an `@Audited` entity row is removed through JPA
When the log is queried for that entity id
Then one `DELETE` row exists listing its former values as `old` with `new` null (no core endpoint deletes an audited entity; the path is exercised by application entities)

### REQ-AUDIT-008 — حجب الحقول / Redaction of changes
Pattern    : ubiquitous
Statement  : The system shall never write to `changes` a field whose name contains `password`, `secret`, `token`, `hash`, `credential`, `apikey`, `privatekey` or `salt` (case-insensitive), nor `createdBy`, `createdAt`, `updatedBy`, `updatedAt`, `version`, `tenantId`, nor a field named in the entity's `@Audited(ignore)`, nor a collection or binary value; an association shall be written as the referenced id; the rule applies to the listener and to explicit entries alike.
Traces     : US-AUDIT-002, US-AUDIT-003
Entities   : ENT-AUDIT-001
Rationale  : POL-AUDIT-003
Source     : audit/crossmodule/AuditApi.java:32-38; audit/domain/AuditEventDomain.java:28-29, :50-73; audit/listener/AuditedEntityListener.java:157-183; audit/service/AuditRecordingService.java:75
Priority   : HIGH
#### AC-AUDIT-008 — [REQ-AUDIT-008]
Given every row visible to PLATFORM, tenant A and tenant B after the whole verification run, and an explicit entry carrying a change named `apiToken`
When every `changes` array is scanned
Then no `field` contains `password`, `secret`, `token`, `hash`, `credential`, `apikey`, `privatekey`, `salt` or `configjson`, and the explicit `apiToken` change was dropped (TC-CORE-AUDIT-016; `AuditApiIntegrationTest.record_dropsSensitiveChanges_whoeverSuppliesThem`; `AuditRows.assertNoSensitiveFieldAnywhere`)

### REQ-AUDIT-009 — القيم الافتراضية للمنفذ والوقت والعميل / Default actor, time and client
Pattern    : ubiquitous
Statement  : The system shall default `occurredAt` to now; `actor` to the authenticated principal's name, else `system`; `actorRealm` to `CUSTOMER` when the caller holds `ROLE_CUSTOMER`, `STAFF` for any other caller, `SYSTEM` without a caller; `ip` to `getRemoteAddr()` and `userAgent` to the `User-Agent` header of the current request, null outside one; `actorUserId` has no default.
Traces     : US-AUDIT-002, US-AUDIT-003
Entities   : ENT-AUDIT-001
Rationale  : POL-AUDIT-006
Source     : audit/service/AuditRecordingService.java:63-78; audit/web/RequestInfoHolder.java:27-61; common/util/SecurityContextHelper.java:64, :74-81; docs/DEVIATIONS.md [10] (`ACTOR_USER_ID` default)
Priority   : HIGH
#### AC-AUDIT-009 — [REQ-AUDIT-009]
Given no security context, then a context holding only `ROLE_CUSTOMER`
When an entry with only an action is recorded in each
Then the first row has `actor = system`, `actorRealm = SYSTEM`, the second `actorRealm = CUSTOMER`; a customer login row has `actorRealm = CUSTOMER` and `actor` = the e-mail (`AuditApiIntegrationTest.record_withoutCaller_isSystem_andCustomerAuthority_isCustomerRealm`; TC-CORE-AUDIT-006)

### REQ-AUDIT-010 — مستأجر الصف / Tenant of a row
Pattern    : ubiquitous
Statement  : The system shall assign a listener row to the entity's own `TENANT_ID` when the entity is tenant-scoped (`AuditableEntity`), and otherwise — a global entity such as `CORE_TENANT` or `CU_APP_CONFIGURATION`, or an explicit entry without `tenantId` — to the current tenant.
Traces     : US-AUDIT-002, US-AUDIT-003
Entities   : ENT-AUDIT-001
Rationale  : POL-AUDIT-001
Source     : audit/listener/AuditedEntityListener.java:44-46, :129; audit/service/AuditRecordingService.java:65
Priority   : HIGH
#### AC-AUDIT-010 — [REQ-AUDIT-010]
Given PLATFORM's operator provisions tenant A, and writes a platform-default setting with `scope=PLATFORM`
When the log is queried for `entityType=CORE_TENANT` as PLATFORM and as A, and for `entityType=CU_APP_CONFIGURATION` as PLATFORM
Then PLATFORM sees the tenant's `CREATE` row (actor `admin`, `changes` with `code`), A sees none; the setting's rows are in tenant 1 although the setting row has `TENANT_ID` NULL (TC-CORE-AUDIT-011, -014; `AuditedEntitiesCoverageIntegrationTest.coreTenant`, `.cuAppConfiguration_platformDefault_isRecordedUnderTheActingPlatformTenant`)

### REQ-AUDIT-011 — قصّ القيم / Truncation of values
Pattern    : ubiquitous
Statement  : The system shall cut a change value longer than 2000 characters to 2000 plus `…`, the actor at 100, the entity type at 128, the entity id at 64, each summary at 1000, the reference at 100, the IP at 64 and the User-Agent at 256; enums are written by name, temporals as ISO-8601, numbers and booleans as JSON scalars.
Traces     : US-AUDIT-002, US-AUDIT-003
Entities   : ENT-AUDIT-001
Rationale  : POL-AUDIT-011
Source     : audit/service/AuditRecordingService.java:35-42, :67-79, :84-99; audit/web/RequestInfoHolder.java:31-33, :50-51
Priority   : MEDIUM
#### AC-AUDIT-011 — [REQ-AUDIT-011]
Given an entry whose `summaryEn` is 1500 characters and whose change value is 3000 characters
When it is recorded
Then the stored summary is 1000 characters and the change value is 2000 characters followed by `…`; the insert never fails on width

### REQ-AUDIT-012 — الاستعلام عن السجل / Query the log
Pattern    : event
Statement  : When a holder of `AUDIT:EVENT:READ` calls `GET /api/v1/audit/events`, the system shall return the caller's tenant's rows matching every given filter — exact `entityType`, `entityId`, `actor`, `action`, and `occurredAt` within [`from`, `to`] — newest first (`occurredAt` DESC), paged (`page` 0, `size` 20, max 200).
Traces     : US-AUDIT-001
Entities   : ENT-AUDIT-001
Rationale  : POL-AUDIT-001, POL-AUDIT-007
Source     : audit/controller/AuditEventController.java:32-45; audit/service/AuditEventService.java:39-45, :55-82
Priority   : HIGH
#### AC-AUDIT-012 — [REQ-AUDIT-012]
Given `alice` created then updated in tenant A, and tenant B's administrator
When A calls `?entityType=SEC_USER&entityId=<alice>`, `…&action=CREATE&from=2000-01-01T00:00:00Z`, `…&from=2999-01-01T00:00:00Z`, and B calls `?entityType=SEC_USER&entityId=<alice>` and `?actor=ta-admin&size=100`
Then A gets both rows with `occurredAt` non-increasing and the `UPDATE` first, then only the `CREATE`, then an empty page; B gets empty pages (TC-CORE-AUDIT-007, -010; `AuditLogApiIntegrationTest.query_returnsTheEntityHistory_newestFirst_forAHolderOfAuditEventRead`, `.query_isTenantIsolated`)

### REQ-AUDIT-013 — رفض حدود زمنية غير صالحة / Reject malformed time bounds
Pattern    : unwanted
Statement  : If `from` or `to` is not an ISO-8601 instant, then the system shall reject the query as a validation error.
Traces     : US-AUDIT-001
Entities   : ENT-AUDIT-001
Rationale  : client input, not a server fault
Source     : audit/service/AuditEventService.java:44-45, :79; common/search/InstantFieldValueConverter.java:26-34
Priority   : MEDIUM
#### AC-AUDIT-013 — [REQ-AUDIT-013]
Given `GET /api/v1/audit/events?from=yesterday`
When it is called by a holder of `AUDIT:EVENT:READ`
Then the system answers 400 `VALIDATION_ERROR` (TC-CORE-AUDIT-010; `AuditLogApiIntegrationTest.query_withAMalformedDate_is400`)

### REQ-AUDIT-014 — التفويض على الاستعلام / Authorisation of the query
Pattern    : ubiquitous
Statement  : The system shall serve `GET /api/v1/audit/events` only to an authenticated staff caller holding `AUDIT:EVENT:READ`; without a token the request is unauthenticated, without the authority it is forbidden, and a customer token is refused by the staff chain.
Traces     : US-AUDIT-001
Entities   : ENT-AUDIT-001
Rationale  : POL-AUDIT-007
Source     : audit/service/AuditEventService.java:56; audit/controller/AuditEventController.java:18-21; audit/permission/AuditPermissions.java:23-24, :41
Priority   : HIGH
#### AC-AUDIT-014 — [REQ-AUDIT-014]
Given no token, a staff token without the grant, a staff token holding only sequence grants, and a customer token
When each calls `GET /api/v1/audit/events`
Then the answers are 401 `SEC-401-INVALID-CREDENTIALS`, 403 `ACCESS_DENIED`, 403 `ACCESS_DENIED`, 403 `REALM_MISMATCH` (TC-CORE-AUDIT-008, -009; `AuditLogApiIntegrationTest.query_withoutAuditEventRead_is403_andWithoutToken_is401`, `.query_customerToken_is403`)

### REQ-AUDIT-015 — السجل للإلحاق فقط / Append-only log
Pattern    : ubiquitous
Statement  : The system shall expose no HTTP operation that creates, updates, deletes or toggles an audit row, and shall never update a row through JPA; rows are inserted in-process only and removed only by retention.
Traces     : US-AUDIT-001, US-AUDIT-006
Entities   : ENT-AUDIT-001
Rationale  : POL-AUDIT-004
Source     : audit/controller/AuditEventController.java:17-21 (single `GET`); audit/entity/AuditEvent.java:23-30 (`@Immutable`, no setters); audit/repository/AuditEventRepository.java:9-10; audit/mapper/AuditEventMapper.java:7
Priority   : HIGH
#### AC-AUDIT-015 — [REQ-AUDIT-015]
Given the audit API
When `POST`, `PUT`, `PATCH` or `DELETE` is sent to `/api/v1/audit/events` or `/api/v1/audit/events/{id}`
Then the system answers 405 `METHOD_NOT_ALLOWED` or 404 `NOT_FOUND` (no mapping exists; `docs/api-docs/audit/index.md:110-116` lists one operation)

### REQ-AUDIT-016 — أحداث حساب SEC في السجل / SEC account events in the log
Pattern    : event
Statement  : When a staff or customer login succeeds, a staff logout completes, or a staff or customer password reset completes, SEC shall record `LOGIN`, `LOGOUT` or `PASSWORD_RESET` through `AuditApi` with the account as actor (`actor` = username, `actorRealm` = the account's realm, `actorUserId` = the account's id), `entityType = SEC_USER` and `entityId` = the account's id; `SEC_AUDIT_LOG` is written as before.
Traces     : US-AUDIT-004
Entities   : ENT-AUDIT-001 (ENT-SEC-001 as the subject)
Rationale  : POL-AUDIT-010
Source     : sec/service/SecAuditEntries.java:22-34; sec/service/AuthService.java:117, :166; sec/service/CustomerAccountService.java:206, :266; sec/service/PasswordResetService.java:146
Priority   : MEDIUM
#### AC-AUDIT-016 — [REQ-AUDIT-016]
Given `alice` logs in, logs out, and completes a password reset; customer `c2` logs in
When the log is queried per action for each account
Then `LOGIN` (≥ 1, `actorUserId` = alice's id), `LOGOUT` (1) and `PASSWORD_RESET` (1, actor = alice) rows exist for alice with `actorRealm = STAFF`, and a `LOGIN` row for c2 with `actorRealm = CUSTOMER`; no `UPDATE` row carries `lastLoginAt` (TC-CORE-AUDIT-003…006; `AuditLogApiIntegrationTest.loginAndLogout_areRecordedForTheAccount`; `SecAuditIntegrationTest.completedPasswordReset_isRecordedAsPasswordReset_forTheAccount`)

### REQ-AUDIT-017 — الكيانات الأساسية المدقَّقة / Audited core entities
Pattern    : ubiquitous
Statement  : The system shall audit, with the listener, exactly these core entities: `SEC_USER` (ignoring `passwordHash`, `lastLoginAt`), `SEC_ROLE`, `CORE_TENANT`, `FILE_DOCUMENT`, `FILE_CATEGORY`, `NOTIF_TEMPLATE`, `MDL_LOOKUP_TYPE`, `MDL_LOOKUP_VALUE`, `CU_APP_CONFIGURATION`, `CORE_NUMBER_SERIES` (ignoring `nextValue`).
Traces     : US-AUDIT-003
Entities   : ENT-AUDIT-001
Rationale  : the step-10 task-3 list plus the step-09 rebase (docs/DEVIATIONS.md [10])
Source     : sec/entity/User.java:32; sec/entity/Role.java:30; tenant/entity/Tenant.java:33; file/entity/FileDocument.java:45; file/entity/FileCategory.java:33; notif/entity/NotificationTemplate.java:33; mdl/entity/LookupType.java:34; mdl/entity/LookupValue.java:38; cu/entity/AppConfiguration.java:38; sequence/entity/NumberSeries.java:41
Priority   : HIGH
#### AC-AUDIT-017 — [REQ-AUDIT-017]
Given each of the ten entities is created and updated over HTTP
When the log is queried per entity
Then each has exactly one `CREATE` row and one `UPDATE` row whose changes are exactly the edited fields; `FILE_DOCUMENT` has its publish `UPDATE` (`visibility` PRIVATE → PUBLIC) and never `contentHash`; `CORE_NUMBER_SERIES` never `nextValue` (TC-CORE-AUDIT-012, -013, -015; `AuditedEntitiesCoverageIntegrationTest` — 9 tests)

### REQ-AUDIT-018 — تطبيق الاحتفاظ / Apply retention
Pattern    : optional
Statement  : Where `erp.core.audit.retention-days` is greater than 0, `AuditRetentionJob.run()` shall delete, tenant by tenant, every row whose `occurredAt` is older than now minus that many days and return the count; at 0 (the default) it shall delete nothing; its `@Scheduled` trigger fires only in an application that enables scheduling and sets `erp.core.audit.retention-cron` (default `-`).
Traces     : US-AUDIT-006
Entities   : ENT-AUDIT-001
Rationale  : POL-AUDIT-008
Source     : audit/service/AuditRetentionJob.java:30-46; audit/service/AuditEventStore.java:92-108; autoconfigure/ErpCoreProperties.java:358-374
Priority   : LOW
#### AC-AUDIT-018 — [REQ-AUDIT-018]
Given rows older and younger than N days in two tenants and `retention-days = N`
When `run()` is called
Then the older rows of both tenants are deleted and the younger ones kept; with `retention-days = 0` nothing is deleted (`AuditRetentionJobIntegrationTest.retentionDeletesRowsOlderThanNDays_inEveryTenant_andKeepsTheRest`)

### REQ-AUDIT-019 — كتالوج الصلاحيات من الكود / Code-contributed permission catalog
Pattern    : ubiquitous
Statement  : The system shall declare registry module `AUDIT`, screen `AUDIT_EVENTS` and its `VIEW` action with the literal authority `AUDIT:EVENT:READ` through `PermissionContributor`, never by a migration seed, so that the catalog synchronizer upserts them at startup and every super role holds the authority.
Traces     : US-AUDIT-001
Entities   : —
Rationale  : POL-AUDIT-007; RULE-SEC-007 (VIEW is the gateway)
Source     : audit/permission/AuditPermissions.java:10-42; V15__audit_schema.sql:10-11
Priority   : MEDIUM
#### AC-AUDIT-019 — [REQ-AUDIT-019]
Given a started application
When `SEC_MODULE_REG`, `SEC_SCREEN_REG` and `SEC_ACTION_REG` are read
Then rows `AUDIT` (سجل التدقيق العام / Audit Log), `AUDIT_EVENTS` (سجل أحداث التدقيق / Audit events) and the action `VIEW` with permission code `AUDIT:EVENT:READ` exist, and a fresh tenant's `SYS_ADMIN` holds it (`AuditApiIntegrationTest.theReadPermission_isSynchronizedIntoTheCatalog_fromTheContributor`; TC-CORE-SEC-003)

### REQ-AUDIT-020 — تقرير قائمة الأحداث / The audit event list report
Pattern    : optional
Statement  : Where a holder of `AUDIT:REPORT:AUDIT_EVENT_LIST` runs or exports report `AUDIT_EVENT_LIST`, the system shall return the caller's tenant's rows filtered by `action` (upper-cased, exact), `entityType`, `entityId`, `actor` (exact) and `occurredFrom` ≤ `occurredAt` < `occurredTo`, newest first (then id DESC), with the columns `occurredAt`, `action`, `actor`, `actorRealm`, `entityType`, `entityId`, `summaryAr`, `summaryEn`, `ip`, `reference` and the total `events`; `changes` is not a column.
Traces     : US-AUDIT-005
Entities   : ENT-AUDIT-001
Rationale  : POL-AUDIT-007 — the report authority is independent of `AUDIT:EVENT:READ`
Source     : audit/report/AuditEventListReport.java:50-67, :92-132
Priority   : MEDIUM
Note       : XM-AUDIT-004. The run / export endpoints belong to the report module (`docs/api-docs/report/index.md:114-121`).
#### AC-AUDIT-020 — [REQ-AUDIT-020]
Given tenant A's rows and tenant B's rows
When A posts `{"params":{"entityType":"SEC_USER"}}` to `/api/v1/report/AUDIT_EVENT_LIST/run` and `/export?format=csv`
Then only A's rows answer, newest first, with the ten columns; `GET /api/v1/report/definitions/AUDIT_EVENT_LIST` describes six optional params (TC-CORE-REPORT-002, -014, -016)

### REQ-AUDIT-021 — تسجيل المستمع لدى Hibernate / Registering the listener with Hibernate
Pattern    : ubiquitous
Statement  : The system shall append `AuditedEntityListener` to Hibernate's `POST_INSERT`, `POST_UPDATE` and `POST_DELETE` event listeners through an `IntegratorProvider` set by a `HibernatePropertiesCustomizer`, composing with — never replacing — an `IntegratorProvider` the application already set.
Traces     : US-AUDIT-003
Entities   : —
Rationale  : the listener must see every flush of every `@Audited` entity, in core and in applications
Source     : audit/config/AuditHibernateConfiguration.java:27-68
Priority   : MEDIUM
#### AC-AUDIT-021 — [REQ-AUDIT-021]
Given an application that sets its own `hibernate.integrator_provider`
When the `EntityManagerFactory` is built
Then both the application's integrators and the audit integrator are applied, and an `@Audited` application entity writes rows like a core one (docs/steps/10-report.md "Notes for later steps")

### A4 — Error codes (HTTP status as the code has it)
| Code | HTTP | Raised when (trigger) | Code location | Message (en / ar) |
|---|---|---|---|---|
| `AUDIT_ACTION_INVALID` | 400 (`Status.VALIDATION_ERROR`) | `AuditApi.record` with an action that is null or does not match `^[A-Z_]{3,64}$` — never from the query API | audit/domain/AuditEventDomain.java:37-44; audit/exception/AuditErrorCodes.java:11 | "Audit action ''{0}'' is invalid: use 3 to 64 upper-case letters or underscores" / "إجراء التدقيق ''{0}'' غير صالح: استخدم من 3 إلى 64 حرفًا إنجليزيًا كبيرًا أو شرطة سفلية" |
| `TENANT_CONTEXT_MISSING` (tenant module) | 500 (`Status.INTERNAL_ERROR`) | `AuditApi.record` without `tenantId` and without a current tenant | audit/service/AuditRecordingService.java:65; tenant/TenantContext.java:47-53 | "The operation ran without a tenant context" / "نُفّذت العملية دون سياق مستأجر" |
| `VALIDATION_ERROR` (common) | 400 | `from` / `to` not an ISO-8601 instant | common/search/InstantFieldValueConverter.java:30-34 | "Validation failed" / "فشل التحقق من البيانات" |
| `ACCESS_DENIED` (common) | 403 | `GET /api/v1/audit/events` without `AUDIT:EVENT:READ`; report without `AUDIT:REPORT:AUDIT_EVENT_LIST` | audit/service/AuditEventService.java:56; audit/report/AuditEventListReport.java:104 | "You do not have permission to perform this operation" / "ليس لديك صلاحية لتنفيذ هذه العملية" |
| `REALM_MISMATCH` (SEC) | 403 | a customer token on the staff chain | docs/api-docs/sec/index.md; docs/DEVIATIONS.md [10] (query API shape) | — |
`Status` → HTTP: common/domain/status/Status.java:8-21. Messages: `erp-core/src/main/resources/i18n/messages.properties`
line 175, `messages_ar.properties` line 172. Other shared codes the endpoint answers:
`SEC-401-INVALID-CREDENTIALS` 401, `METHOD_NOT_ALLOWED` 405, `INTERNAL_ERROR` 500
(docs/api-docs/audit/index.md:79-87).

## A5 — Business rules

### RULE-AUDIT-001 — صيغة الإجراء / Action format
Scope      : ENT-AUDIT-001.action
Trigger    : `AuditApi.record`; the listener (which passes only `CREATE`, `UPDATE`, `DELETE`)
Statement  : The system shall accept an action only if it matches `^[A-Z_]{3,64}$`; the standard actions are `CREATE`, `UPDATE`, `DELETE`, `STATUS_CHANGE`, `LOGIN`, `LOGOUT`, `PASSWORD_RESET`; the database enforces the same regex (`CHK_CORE_AUDIT_EVENT_ACTION`).
Data source: the entry's `action`
Message    : `AUDIT_ACTION_INVALID` (A4)
Traces     : REQ-AUDIT-002
Source     : audit/domain/AuditEventDomain.java:21-22, :37-44; audit/crossmodule/AuditApi.java:20-26; V15__audit_schema.sql:41

### RULE-AUDIT-002 — الكتابة المتزامنة في معاملة المستدعي / Synchronous write in the caller's transaction
Scope      : ENT-AUDIT-001
Trigger    : `AuditApi.record`
Statement  : The system shall insert the row synchronously on the connection bound to the current Spring transaction (`JdbcTemplate`); a rollback of that transaction removes the row; called outside a transaction the insert commits on its own. `AuditRecordingService` opens no transaction of its own and checks no authority.
Data source: —
Message    : —
Traces     : REQ-AUDIT-001, REQ-AUDIT-004
Source     : audit/crossmodule/AuditApi.java:10-13; audit/service/AuditEventStore.java:53-59; audit/service/AuditRecordingService.java:20-29; ADR-AUDIT-001

### RULE-AUDIT-003 — المستمع يكتب من داخل الـ flush عبر JDBC / The listener writes from inside the flush with JDBC
Scope      : ENT-AUDIT-001
Trigger    : Hibernate `POST_INSERT` / `POST_UPDATE` / `POST_DELETE` of an `@Audited` entity
Statement  : The system shall write the listener's row through `AuditRecordingService.record(entry, connection)` on the flushing session's connection (`Session.doWork`), never by persisting an entity through the session; the listener is registered by an integrator that composes with an existing `hibernate.integrator_provider`; `requiresPostCommitHandling` is false.
Data source: the event's persister, state, old state and dirty properties
Message    : —
Traces     : REQ-AUDIT-004, REQ-AUDIT-005, REQ-AUDIT-006, REQ-AUDIT-007, REQ-AUDIT-021
Source     : audit/listener/AuditedEntityListener.java:120-139; audit/config/AuditHibernateConfiguration.java:36-68; audit/service/AuditEventStore.java:26-32; ADR-AUDIT-001

### RULE-AUDIT-004 — قواعد الحجب / Redaction rules
Scope      : ENT-AUDIT-001.changes
Trigger    : every recorded change
Statement  : The system shall drop a change whose field name contains one of `password`, `secret`, `token`, `hash`, `credential`, `apikey`, `privatekey`, `salt` (case-insensitive substrings), whoever supplied it; the listener shall additionally skip `createdBy`, `createdAt`, `updatedBy`, `updatedAt`, `version`, `tenantId`, the entity's `@Audited(ignore)` names, collection properties and binary values, and shall write an association as the referenced id; an `UPDATE` with no remaining difference writes no row.
Data source: `AuditApi.SENSITIVE_FIELD_WORDS`; `AuditEventDomain.TECHNICAL_FIELDS`; `@Audited.ignore`
Message    : —
Traces     : REQ-AUDIT-006, REQ-AUDIT-008
Source     : audit/crossmodule/AuditApi.java:32-38; audit/domain/AuditEventDomain.java:28-29, :50-73; audit/listener/AuditedEntityListener.java:95-98, :157-183; ADR-AUDIT-002

### RULE-AUDIT-005 — القيم الافتراضية / Defaults
Scope      : ENT-AUDIT-001
Trigger    : every recorded row
Statement  : The system shall fill `tenantId` from `TenantContext.require()`, `occurredAt` with now, `actor` with the principal's name or `system`, `actorRealm` with `CUSTOMER` (caller holds `ROLE_CUSTOMER`) / `STAFF` (other caller) / `SYSTEM` (no caller), `ip` and `userAgent` from `RequestInfoHolder` — each only when the entry leaves it null; `actorUserId`, `entityType`, `entityId`, the summaries, `changes` and `reference` have no default.
Data source: `TenantContext`; `SecurityContextHelper`; `RequestInfoHolder.current()`
Message    : `TENANT_CONTEXT_MISSING` when no tenant can be found
Traces     : REQ-AUDIT-003, REQ-AUDIT-009
Source     : audit/crossmodule/AuditEntry.java:10-25; audit/service/AuditRecordingService.java:61-80; audit/web/RequestInfoHolder.java:41-61

### RULE-AUDIT-006 — حدود الأطوال / Width limits
Scope      : ENT-AUDIT-001
Trigger    : every recorded row
Statement  : The system shall cut change values at 2000 characters (+ `…`), `actor` at 100, `entityType` at 128, `entityId` at 64, `summaryAr` / `summaryEn` at 1000, `reference` at 100, `ip` at 64, `userAgent` at 256, and turn values into JSON scalars (enum name, ISO-8601 temporal, number, boolean, string).
Data source: `AuditRecordingService.MAX_*`; `RequestInfoHolder.MAX_*`
Message    : —
Traces     : REQ-AUDIT-011
Source     : audit/service/AuditRecordingService.java:35-42, :67-79, :84-99; audit/web/RequestInfoHolder.java:31-33, :50-51

### RULE-AUDIT-007 — مستأجر الصف / Tenant of a row
Scope      : ENT-AUDIT-001.tenantId
Trigger    : every recorded row
Statement  : The system shall take the row's tenant from the flushed entity's own `TENANT_ID` when it extends `AuditableEntity`; otherwise (global entity, explicit entry without `tenantId`) from the current tenant; `CORE_AUDIT_EVENT.TENANT_ID` is never null.
Data source: `AuditableEntity.getTenantId()`; `TenantContext.require()`
Message    : `TENANT_CONTEXT_MISSING`
Traces     : REQ-AUDIT-003, REQ-AUDIT-010
Source     : audit/listener/AuditedEntityListener.java:44-46, :129; audit/service/AuditRecordingService.java:65; V15__audit_schema.sql:18

### RULE-AUDIT-008 — الكيانات المدقَّقة واستثناءاتها / Audited entities and their ignores
Scope      : the ten core entities
Trigger    : entity annotation
Statement  : The system shall audit `SEC_USER` (ignore `passwordHash`, `lastLoginAt`), `SEC_ROLE`, `CORE_TENANT`, `FILE_DOCUMENT`, `FILE_CATEGORY`, `NOTIF_TEMPLATE`, `MDL_LOOKUP_TYPE`, `MDL_LOOKUP_VALUE`, `CU_APP_CONFIGURATION` and `CORE_NUMBER_SERIES` (ignore `nextValue`), with `ENTITY_TYPE` = the table name.
Data source: the `@Audited` annotations
Message    : —
Traces     : REQ-AUDIT-017
Source     : the ten `@Audited(` sites listed under REQ-AUDIT-017; docs/DEVIATIONS.md [10] (task-3 entity list; `@Audited` placement; rebase onto 09)

### RULE-AUDIT-009 — الكتابة المزدوجة من SEC / SEC dual write
Scope      : SEC account events
Trigger    : successful login (staff, customer), staff logout, reset completion (staff, customer)
Statement  : SEC shall record `LOGIN`, `LOGOUT`, `PASSWORD_RESET` through `AuditApi` with the account as actor, and shall keep writing `SEC_AUDIT_LOG` unchanged; failed logins and reset requests are written to `SEC_AUDIT_LOG` only.
Data source: `SecAuditEntries.accountEvent`
Message    : —
Traces     : REQ-AUDIT-016
Source     : sec/service/SecAuditEntries.java:12-35; sec/service/AuthService.java:117, :166; sec/service/CustomerAccountService.java:206, :266; sec/service/PasswordResetService.java:146; ADR-AUDIT-003

### RULE-AUDIT-010 — دلالات الاحتفاظ / Retention semantics
Scope      : ENT-AUDIT-001 (deletion)
Trigger    : `AuditRetentionJob.run()`; the optional `@Scheduled` trigger
Statement  : The system shall delete nothing when `erp.core.audit.retention-days` ≤ 0; otherwise delete, for every tenant having such rows, the rows with `OCCURRED_AT` before now minus the days, one `DELETE` per tenant naming `TENANT_ID`; core never schedules the job (`retention-cron` default `-`).
Data source: `ErpCoreProperties.Audit`
Message    : —
Traces     : REQ-AUDIT-018
Source     : audit/service/AuditRetentionJob.java:30-46; audit/service/AuditEventStore.java:92-108; autoconfigure/ErpCoreProperties.java:361-374

### RULE-AUDIT-011 — لا كاتب غير متزامن / No asynchronous writer
Scope      : ENT-AUDIT-001
Trigger    : design
Statement  : The system shall provide no asynchronous audit writer and no listener on `DomainEvent`; every row is written by RULE-AUDIT-002 or RULE-AUDIT-003.
Data source: —
Message    : —
Traces     : REQ-AUDIT-004
Source     : docs/DEVIATIONS.md [10] "Step 08 events for async write"; docs/steps/10-report.md "Decisions & deviations" 4; ADR-AUDIT-001

### RULE-AUDIT-012 — مرشحات الاستعلام وترتيبه / Query filters and order
Scope      : ENT-AUDIT-001 (query)
Trigger    : `GET /api/v1/audit/events`
Statement  : The system shall filter only on `entityType`, `entityId`, `actor`, `action` (EQUALS, trimmed; blank ignored) and `occurredAt` (`from` ≥, `to` ≤, ISO-8601 instants), shall sort by `occurredAt` DESC only (no client sort), shall page with `PageableBuilder` (size ≤ 200), and shall serve staff only.
Data source: `AuditEventService.ALLOWED_SORT_FIELDS`; `InstantFieldValueConverter`
Message    : `VALIDATION_ERROR` for a malformed instant
Traces     : REQ-AUDIT-012, REQ-AUDIT-013, REQ-AUDIT-014
Source     : audit/service/AuditEventService.java:39-45, :63-80, :84-88

### RULE-AUDIT-013 — الإلحاق فقط ولا نقطة كتابة / Append-only, no write endpoint
Scope      : ENT-AUDIT-001
Trigger    : any access
Statement  : The system shall map one HTTP operation only (`GET`), shall keep `AuditEvent` `@Immutable` without setters, and shall never update a row; `VERSION` stays 0.
Data source: —
Message    : —
Traces     : REQ-AUDIT-015
Source     : audit/controller/AuditEventController.java:17-24, :32; audit/entity/AuditEvent.java:23-30; audit/service/AuditEventStore.java:46 (`VERSION` 0)

## A6 — Lookups

**ACTOR_REALM of ENT-AUDIT-001** — owned by AUDIT — control type: fixed value set (CHECK constraint `CHK_CORE_AUDIT_EVENT_REALM`; the `SecurityContextHelper.REALM_*` constants; not an MDL lookup)
| Code | Label (ar) | Label (en) |
|---|---|---|
| STAFF | — | STAFF |
| CUSTOMER | — | CUSTOMER |
| SYSTEM | — | SYSTEM |
Source: V15__audit_schema.sql:21, :42; audit/crossmodule/AuditApi.java:28-30; common/util/SecurityContextHelper.java:12-18
(the code carries no value labels; the API returns the code, whose field label is "نطاق المنفذ / Actor realm").

**ACTION of ENT-AUDIT-001** — owned by AUDIT — control type: free code `^[A-Z_]{3,64}$` (not a closed list)
| Code | Written by |
|---|---|
| CREATE, UPDATE, DELETE | the `@Audited` listener |
| LOGIN, LOGOUT, PASSWORD_RESET | SEC (`SecAuditEntries`) |
| STATUS_CHANGE | constant offered to callers (no core writer) |
| any other `^[A-Z_]{3,64}$` | applications through `AuditApi` |
Source: audit/crossmodule/AuditApi.java:20-26; V15__audit_schema.sql:41, :49.

Consumed lookups: none.

## A7 — Status lifecycle

**ENT-AUDIT-001 — no lifecycle.** A row is inserted once and never changes (RULE-AUDIT-013); the only
removal is retention (RULE-AUDIT-010). There is no status column, no activation flag and no terminal
state. Source: audit/entity/AuditEvent.java:23-30; audit/service/AuditRetentionJob.java:36-46.

## A8 — Module dependencies

| Consumed entity | Owner ENT id | Owner module | HARD-FK / SOFT-READ | XM |
|---|---|---|---|---|
| `CORE_TENANT` | ENT-TENANT-001 | TENANT | HARD-FK (`FK_CORE_AUDIT_EVENT_TENANT`) | XM-AUDIT-001 (tenant side: DBF-TENANT-032) |

| Consumed surface | Owner | Kind | Source |
|---|---|---|---|
| `TenantContext.require()`; Hibernate `@TenantId` via `AuditableEntity` (reads) | TENANT (root package) / common | context | audit/service/AuditRecordingService.java:9, :65; audit/entity/AuditEvent.java:38 |
| `PermissionContributor` | SEC | SPI implemented by `AuditPermissions` | audit/permission/AuditPermissions.java:19 |
| `ReportProvider`, `ReportParam`, `ReportColumn`, `ReportResult`, `ReportAuthorities`, `ParamType`, `ColumnType` | report | SPI implemented by `AuditEventListReport` — XM-AUDIT-004 | audit/report/AuditEventListReport.java:11-17, :48 |
| `SecurityContextHelper` (`currentActorOrSystem`, `currentRealm`, `REALM_*`), `Strings.truncate`, `PlainJson.MAPPER`, `InstantFieldValueConverter`, `SpecBuilder`, `PageableBuilder`, `ServiceResult`, `LocalizedException`, `OperationCode` | common | foundation | audit/service/AuditRecordingService.java:7-8; audit/service/AuditEventStore.java:5; audit/service/AuditEventService.java:7-15 |
| `ErpCoreProperties.Audit` | autoconfigure | properties | audit/service/AuditRetentionJob.java:27, :37 |
| `HibernatePropertiesCustomizer`, `IntegratorProvider`, `EventListenerRegistry` | Spring Boot / Hibernate | integration | audit/config/AuditHibernateConfiguration.java:28-68 |

**Exposed direction** (consumers of the audit module)
| XM id | Exposed surface | Kind | Consumers | Source |
|---|---|---|---|---|
| XM-AUDIT-002 | `com.erp.audit.crossmodule.AuditApi.record(AuditEntry)` (+ `AuditEntry`, `AuditChange`) | crossmodule call, in-process, ungated, synchronous | SEC — `AuthService`, `CustomerAccountService`, `PasswordResetService` via `SecAuditEntries`; applications | audit/crossmodule/AuditApi.java:18-47; sec/service/SecAuditEntries.java:22-34; docs/CONSUMING.md §9 |
| XM-AUDIT-003 | `com.erp.audit.crossmodule.Audited` | annotation read by the Hibernate listener | SEC (2), TENANT (1), FILE (2), NOTIF (1), MDL (2), CU (1), SEQUENCE (1); applications | audit/crossmodule/Audited.java:32-45; REQ-AUDIT-017 |
| XM-AUDIT-004 | report `AUDIT_EVENT_LIST` (`AuditEventListReport`) | `ReportProvider` registered in the report registry; authority `AUDIT:REPORT:AUDIT_EVENT_LIST` | report module (`/api/v1/report/AUDIT_EVENT_LIST/{run,export}`), frontend | audit/report/AuditEventListReport.java:48-132; docs/api-docs/report/index.md:114-121 |
| — | `AuditRetentionJob.run()` | public bean method | applications' schedulers | audit/service/AuditRetentionJob.java:36-46 |

# PART B — SCREEN REQUIREMENTS

## SCR-REQ-AUDIT-001 — سجل أحداث التدقيق / Audit events
### B1 — Definition
Purpose      : استعراض الخط الزمني للتدقيق في المستأجر: من غيّر ماذا ومتى، مع تفاصيل تغييرات الحقول.
Entities     : ENT-AUDIT-001
Operations   : search (read-only)
Users        : موظف يحمل `AUDIT:EVENT:READ` (كل دور خارق يحمله)
Navigation   : AUDIT (سجل التدقيق العام / Audit Log) → Audit events (registry module `AUDIT`)
Content shape: flat record (filtered list, newest first, with an expandable `changes` detail)
Traces       : REQ-AUDIT-012, REQ-AUDIT-013, REQ-AUDIT-014, REQ-AUDIT-015
Composite    : single screen (search list; no entry form — the log is append-only)
### B2 — Search / list
Filters: `entityType`, `entityId`, `actor`, `action` (exact), `from`, `to` (ISO-8601 instants on
`occurredAt`); fixed order `occurredAt` DESC; `page` (0), `size` (20, ≤ 200)
(audit/service/AuditEventService.java:39-45, :63-80).
### B3 — Input
Not applicable — read-only; no create, update, delete or toggle (RULE-AUDIT-013).
### B4 — Access
Page code: `AUDIT_EVENTS`. Action: `VIEW` carrying the literal authority `AUDIT:EVENT:READ` — gateway and
read authority in one grant. Staff chain only (customer tokens answer 403 `REALM_MISMATCH`). Every tenant's
super role holds it.
### B5 — API expectations
| API-ID | Operation | Verb | Path | Inputs | Outputs | Permission | RULEs | Errors | Traces (REQ) |
|---|---|---|---|---|---|---|---|---|---|
| API-AUDIT-001 | search audit events (newest first) | GET | /api/v1/audit/events | query `entityType?`, `entityId?`, `actor?`, `action?`, `from?`, `to?`, `page` (0), `size` (20) | 200 `Page<AuditEventResponse>` | `AUDIT:EVENT:READ` | RULE-AUDIT-012, -013 | 400 `VALIDATION_ERROR`, 401 `SEC-401-INVALID-CREDENTIALS`, 403 `ACCESS_DENIED`, 403 `REALM_MISMATCH` | REQ-AUDIT-012…015 |
Source: audit/controller/AuditEventController.java:24-45; `docs/api-docs/audit/index.md:110-116`;
`docs/api-docs/audit/endpoints/audit-log.md` (which prints the permission as the constant name `AUDIT_EVENT_READ`).

> **In-process (not HTTP):** `AuditApi.record(AuditEntry)` — RULE-AUDIT-001, -002, -004, -005, -006, -007;
> `AUDIT_ACTION_INVALID`, `TENANT_CONTEXT_MISSING` — REQ-AUDIT-001…004, -008…011; XM-AUDIT-002.
> `@Audited` — RULE-AUDIT-003, -004, -007, -008 — REQ-AUDIT-005…008, -010, -017, -021; XM-AUDIT-003.

## SCR-REQ-AUDIT-002 — تقارير التدقيق / Audit reports
### B1 — Definition
Purpose      : تشغيل تقرير قائمة أحداث التدقيق وتصديره (CSV / JSON) بمرشحات الإجراء والكيان والمنفذ والفترة.
Entities     : ENT-AUDIT-001
Operations   : run, export (read-only)
Users        : موظف يحمل `AUDIT:REPORT:AUDIT_EVENT_LIST`
Navigation   : AUDIT → AUDIT Reports (registry-only screen `AUDIT_REPORTS`, created by the report synchronizer from the registered provider; the frontend's generic report screen renders it)
Content shape: other (report run / export — the REPORT module's generic screen)
Traces       : REQ-AUDIT-020
Composite    : single screen requirement (the page is the REPORT module's; the row and the action are AUDIT's)
### B2 — Search / list
Params: `action`, `entityType`, `entityId`, `actor` (STRING, optional), `occurredFrom`, `occurredTo`
(DATETIME, optional); order `occurredAt` DESC, `id` DESC; page size ≤ 200 (REPORT); export cap
`erp.core.report.max-export-rows` (audit/report/AuditEventListReport.java:92-100, :128-129).
### B3 — Input
Not applicable — read-only.
### B4 — Access
Page code: `AUDIT_REPORTS` (تقارير AUDIT / AUDIT Reports). Actions: `VIEW` → `PERM_AUDIT_REPORTS_VIEW`
(gateway) and `AUDIT_EVENT_LIST` (سجل أحداث التدقيق / Audit event list) → `AUDIT:REPORT:AUDIT_EVENT_LIST`;
independent of `AUDIT:EVENT:READ` (report/permission/ReportPermissions.java:50-68; audit/report/AuditEventListReport.java:42-44, :104).
### B5 — API expectations (endpoints owned by the REPORT module — `docs/api-docs/report/`)
| API-ID | Operation | Verb | Path | Inputs | Outputs | Permission | RULEs | Errors | Traces (REQ) |
|---|---|---|---|---|---|---|---|---|---|
| API-AUDIT-002 (REPORT `run`) | run the audit event list | POST | /api/v1/report/AUDIT_EVENT_LIST/run | `{params, page, size}` | 200 one page: columns `occurredAt`, `action`, `actor`, `actorRealm`, `entityType`, `entityId`, `summaryAr`, `summaryEn`, `ip`, `reference`; totals `events` | `AUDIT:REPORT:AUDIT_EVENT_LIST` | RULE-AUDIT-012 (same filters, read-only) | 400 `REPORT_PARAM_INVALID`, 403 `ACCESS_DENIED`, 404 `REPORT_NOT_FOUND` | REQ-AUDIT-020 |
| API-AUDIT-003 (REPORT `export`) | export the audit event list | POST | /api/v1/report/AUDIT_EVENT_LIST/export?format=csv\|json | `{params}` | 200 file `AUDIT_EVENT_LIST.csv` / `.json` | `AUDIT:REPORT:AUDIT_EVENT_LIST` | — | 400 `REPORT_PARAM_INVALID`, 403 `ACCESS_DENIED`, 422 `REPORT_EXPORT_TOO_LARGE` | REQ-AUDIT-020 |
Source: audit/report/AuditEventListReport.java:48-132; `docs/api-docs/report/index.md:114-121`.

# STANDALONE

## Traceability matrix
| P0.5 | REQ | AC | RULE | ENT | SCR-REQ |
|---|---|---|---|---|---|
| US-AUDIT-001 | REQ-AUDIT-012, -013, -014, -015, -019 | AC-AUDIT-012, -013, -014, -015, -019 | RULE-AUDIT-012, -013 | ENT-AUDIT-001 | SCR-REQ-AUDIT-001 |
| US-AUDIT-002 | REQ-AUDIT-001, -002, -003, -004, -008, -009, -010, -011 | AC-AUDIT-001, -002, -003, -004, -008, -009, -010, -011 | RULE-AUDIT-001, -002, -004, -005, -006, -007, -011 | ENT-AUDIT-001 | — |
| US-AUDIT-003 | REQ-AUDIT-004, -005, -006, -007, -008, -009, -010, -011, -017, -021 | AC-AUDIT-004, -005, -006, -007, -008, -009, -010, -011, -017, -021 | RULE-AUDIT-003, -004, -005, -006, -007, -008, -011 | ENT-AUDIT-001 | — |
| US-AUDIT-004 | REQ-AUDIT-016 | AC-AUDIT-016 | RULE-AUDIT-009 | ENT-AUDIT-001 | SCR-REQ-AUDIT-001 |
| US-AUDIT-005 | REQ-AUDIT-020 | AC-AUDIT-020 | RULE-AUDIT-012 | ENT-AUDIT-001 | SCR-REQ-AUDIT-002 |
| US-AUDIT-006 | REQ-AUDIT-015, -018 | AC-AUDIT-015, -018 | RULE-AUDIT-010, -013 | ENT-AUDIT-001 | — |

Every story traces to ≥ 1 REQ; every REQ has one AC; every RULE traces to a REQ; both screens trace to
their REQs. No orphan, no dangling id.

## Decisions applied
| DEFAULT / ADR | What | Source | Status |
|---|---|---|---|
| ADR-AUDIT-001 | synchronous JDBC write on the caller's connection, including from inside the Hibernate flush | governance/analysis/decisions/AUDIT/ADR-AUDIT-001.md | ACCEPTED (as built) |
| ADR-AUDIT-002 | redaction model: name-substring denylist + technical fields + `@Audited(ignore)` | governance/analysis/decisions/AUDIT/ADR-AUDIT-002.md | ACCEPTED (as built) |
| ADR-AUDIT-003 | coexistence with `SEC_AUDIT_LOG` (dual write), opt-in app-scheduled retention, `OCCURRED_AT` as TIMESTAMP without zone | governance/analysis/decisions/AUDIT/ADR-AUDIT-003.md | ACCEPTED (as built) |
| DEFAULT | a single `GET` with query parameters instead of `POST /search`; no write / usage / toggle endpoints; one READ permission instead of four | docs/DEVIATIONS.md [10] (query API shape; permission) | as built |
| DEFAULT | `ACTOR_REALM` CHECK + constants, `ACTION` free regex code — no MDL lookup | V15__audit_schema.sql:41-42; pattern of ADR-SEC-001 | as built |

## Access summary
| Page code | Screen | VIEW | CREATE | UPDATE | DELETE | Custom |
|---|---|---|---|---|---|---|
| AUDIT_EVENTS | Audit events | `AUDIT:EVENT:READ` (gateway and the read authority) | — | — | — | — |
| AUDIT_REPORTS | AUDIT Reports (registry-only, REPORT module page) | `PERM_AUDIT_REPORTS_VIEW` (gateway) | — | — | — | `AUDIT:REPORT:AUDIT_EVENT_LIST`: run, export |
Every action beyond VIEW additionally requires VIEW on the same screen (RULE-SEC-007). The frontend's
screen archive is `governance/frontend/modules/AUDIT/tests/specs/audit/audit-events.spec.ts`; the report
screen is exercised by `governance/frontend/modules/REPORT/tests/`.
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 10
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : none recorded here yet — the audit action codes of the tenant-maturity plan (B–E) are documented by the implementing run as each package lands on main
Statement      : This addendum records only what is already on main for 1.3.0; the plan packages' rows are written by the implementing run.

Plan packages B, C, D, E, G: documented by each package as it lands on main (analysis-first, written by the implementing run); the verified reference rows are in docs/plans/tenant-maturity-analysis-reference.md.
