# PRD — سجل التدقيق العام / Audit (AUDIT)
══════════════════════════════════════════════════════════════════
Module          : AUDIT     Version : v1 (as-built baseline, erp-core 1.2.0)
Source artifacts: platform-summary, module-registry-audit, business-policies-audit
Stories         : 6   Policies covered : 11/11   Deferred : 0
Status          : AS-BUILT — every story is implemented; the code location is in `Source`
══════════════════════════════════════════════════════════════════

Java paths are relative to `erp-core/src/main/java/com/erp/`. HTTP verification: the `TC-CORE-AUDIT-*`
cases of `docs/test-api/core-test-plan.md`; the frontend's E2E archive is
`governance/frontend/modules/AUDIT/tests/specs/audit/audit-events.spec.ts`.

## USER STORIES

US-AUDIT-001
  Title          : استعراض سجل التدقيق / Browse the audit log
  Story          : As a staff member holding `AUDIT:EVENT:READ`, I need to list my tenant's audit events newest first and narrow them by entity, actor, action and time window, so that I can see who changed what and when.
  Priority       : HIGH — the module's only HTTP surface (10-STEP task 5)
  Success metric : `GET /api/v1/audit/events` answers the entity's history newest first, tenant-isolated (TC-CORE-AUDIT-007, -010)
  Traces         : POL-AUDIT-001, POL-AUDIT-004, POL-AUDIT-006, POL-AUDIT-007
  Source         : audit/controller/AuditEventController.java:32-45; audit/service/AuditEventService.java:55-82
  Status         : AS-BUILT

US-AUDIT-002
  Title          : تسجيل حقيقة صريحة / Record an explicit business fact
  Story          : As a module or application, I need to record a fact of my own (an approval, a status change, a login, an export) with an action, an entity, bilingual summaries and optional field changes, inside my transaction, so that it appears in the tenant's timeline only if my change commits.
  Priority       : HIGH
  Success metric : a rolled-back transaction leaves no row; defaults are filled from the tenant, the security context and the request (`AuditApiIntegrationTest.record_inARolledBackTransaction_leavesNoRow_andInACommittedOne_staysWritten`, `record_fillsDefaults_fromTheTenantAndSecurityContext`)
  Traces         : POL-AUDIT-001, POL-AUDIT-002, POL-AUDIT-003, POL-AUDIT-005, POL-AUDIT-006, POL-AUDIT-009, POL-AUDIT-011
  Source         : audit/crossmodule/AuditApi.java:40-47; audit/crossmodule/AuditEntry.java:36-60; audit/service/AuditRecordingService.java:46-81
  Status         : AS-BUILT

US-AUDIT-003
  Title          : إدراج كيان في التدقيق على مستوى الحقول / Opt an entity into field-level history
  Story          : As a module or application owning a JPA entity, I need to annotate it once so that every insert, update and delete is recorded with the fields that changed, and to name the fields that must never be recorded, so that I get a change history without writing audit code.
  Priority       : HIGH
  Success metric : creating a user writes one `CREATE` row without the password hash; updating two fields writes one `UPDATE` row with exactly two changes (TC-CORE-AUDIT-001, -002)
  Traces         : POL-AUDIT-001, POL-AUDIT-002, POL-AUDIT-003, POL-AUDIT-009, POL-AUDIT-011
  Source         : audit/crossmodule/Audited.java:32-45; audit/listener/AuditedEntityListener.java:63-139; audit/config/AuditHibernateConfiguration.java:36-49
  Status         : AS-BUILT

US-AUDIT-004
  Title          : أحداث الحساب في الخط الزمني نفسه / Account events in the same timeline
  Story          : As a security administrator, I need a user's logins, logouts and password resets next to the changes made to that user, so that one query answers "what happened to this account".
  Priority       : MEDIUM
  Success metric : `LOGIN`, `LOGOUT`, `PASSWORD_RESET` rows exist for the account with the account as actor (TC-CORE-AUDIT-003, -004, -005, -006)
  Traces         : POL-AUDIT-010
  Source         : sec/service/SecAuditEntries.java:22-34; sec/service/AuthService.java:117, :166; sec/service/CustomerAccountService.java:206, :266; sec/service/PasswordResetService.java:146
  Status         : AS-BUILT

US-AUDIT-005
  Title          : تصدير قائمة أحداث التدقيق / Export the audit event list
  Story          : As a staff member holding `AUDIT:REPORT:AUDIT_EVENT_LIST`, I need to run and export the audit events of my tenant as a tabular report filtered by action, entity, actor and time window, so that I can hand an auditor a file.
  Priority       : MEDIUM
  Success metric : `POST /api/v1/report/AUDIT_EVENT_LIST/run` and `/export` answer the filtered rows of the caller's tenant only (TC-CORE-REPORT-002, -014, -016)
  Traces         : POL-AUDIT-001, POL-AUDIT-007
  Source         : audit/report/AuditEventListReport.java:48-132; docs/api-docs/report/index.md:114-121
  Status         : AS-BUILT

US-AUDIT-006
  Title          : تطبيق سياسة الاحتفاظ / Apply a retention policy
  Story          : As the operator of an application, I need to delete audit rows older than N days across all tenants, on my own schedule, so that the log stays within my legal and storage limits — and to keep everything when I set nothing.
  Priority       : LOW
  Success metric : rows older than N days are deleted in every tenant and younger ones kept; 0 days deletes nothing (`AuditRetentionJobIntegrationTest.retentionDeletesRowsOlderThanNDays_inEveryTenant_andKeepsTheRest`)
  Traces         : POL-AUDIT-004, POL-AUDIT-008
  Source         : audit/service/AuditRetentionJob.java:30-46; audit/service/AuditEventStore.java:96-108
  Status         : AS-BUILT

## THE RECORDING STORY (US-AUDIT-002 + US-AUDIT-003, as built)
1. A module records a fact: `auditApi.record(AuditEntry.builder().action("APPROVE").entityType("SALES_INVOICE").entityId("42")…build())`
   inside its business transaction — audit/crossmodule/AuditEntry.java:27-34 (example); docs/CONSUMING.md §9.
2. Or Hibernate flushes an `@Audited` entity: the listener receives `PostInsertEvent` / `PostUpdateEvent` /
   `PostDeleteEvent`, diffs the recordable persistent properties (CREATE: every non-null value; UPDATE: the
   values that differ from the loaded snapshot; DELETE: every non-null deleted value) and builds an entry
   whose tenant is the entity's own `TENANT_ID` (null for a global entity) — audit/listener/AuditedEntityListener.java:63-136.
3. `AuditRecordingService.resolve` validates the action (`^[A-Z_]{3,64}$` → `AUDIT_ACTION_INVALID`), fills
   the defaults (tenant from `TenantContext.require()`, now, principal or `system`, realm, IP / User-Agent),
   drops sensitive changes, cuts values — audit/service/AuditRecordingService.java:61-81, :84-99.
4. `AuditEventStore` runs one `INSERT INTO CORE_AUDIT_EVENT … VALUES (nextval('SEQ_CORE_AUDIT_EVENT'), …, CAST(? AS JSONB), …)`
   — for an explicit call on the Spring-transaction-bound connection (`JdbcTemplate`), for the listener on the
   flushing session's connection (`Session.doWork`) — audit/service/AuditEventStore.java:43-90;
   audit/listener/AuditedEntityListener.java:138.
5. The row commits or rolls back with the change it describes.

## TRACEABILITY — story → policy
| US | Traces (POL) | Source |
|---|---|---|
| US-AUDIT-001 | POL-AUDIT-001, -004, -006, -007 | audit/service/AuditEventService.java:55-82 |
| US-AUDIT-002 | POL-AUDIT-001, -002, -003, -005, -006, -009, -011 | audit/service/AuditRecordingService.java:46-81 |
| US-AUDIT-003 | POL-AUDIT-001, -002, -003, -009, -011 | audit/listener/AuditedEntityListener.java:63-139 |
| US-AUDIT-004 | POL-AUDIT-010 | sec/service/SecAuditEntries.java:22-34 |
| US-AUDIT-005 | POL-AUDIT-001, -007 | audit/report/AuditEventListReport.java:102-132 |
| US-AUDIT-006 | POL-AUDIT-004, -008 | audit/service/AuditRetentionJob.java:36-46 |
Every policy POL-AUDIT-001 … POL-AUDIT-011 appears in at least one row above (001 → US-001/002/003/005;
002, 003, 009, 011 → US-002/003; 004 → US-001/006; 005 → US-002; 006 → US-001/002; 007 → US-001/005;
008 → US-006; 010 → US-004).

## RESOLVED DECISIONS (dialogue)
| # | Question | Recommended | Confirmed by user | Sources |
|---|---|---|---|---|
| 1 | Write path | synchronous JDBC on the caller's connection, also inside the Hibernate flush | fixed decision of erp-core plan step 10, as built | ADR-AUDIT-001 |
| 2 | Redaction model | name-substring denylist + technical fields + `@Audited(ignore)` | step 10, as built | ADR-AUDIT-002 |
| 3 | `SEC_AUDIT_LOG`, retention, timestamp type | dual write; opt-in app-scheduled retention; TIMESTAMP without zone | step 10, as built | ADR-AUDIT-003 |
No other question: the stories describe built behaviour, read from the code.

## DEFERRED
| US | Reason | Activation trigger |
|---|---|---|
| (asynchronous writer, write endpoints, default `ACTOR_USER_ID`, core-scheduled retention, `X-Forwarded-For`) | not built (business-policies-audit.md SCOPE EXCEPTIONS) | explicit future request; applications may schedule retention and mirror events themselves |
| (new action codes of the tenant-maturity plan) | callers' codes; no audit change | packages B–E (1.3.0) — `docs/plans/tenant-maturity-analysis-reference.md`; each package records its code in `../P1/srs-audit.md` → "Implementation Addendum — erp-core 1.3.0" when it lands |

## APPROVAL
Approved by : n/a — as-built baseline (the stories describe implemented behaviour, erp-core 1.2.0)   Date : 2026-10-07
Later changes are appended as "Implementation Addendum — erp-core 1.3.0" sections, never by rewriting
the stories above.
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 10
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.
