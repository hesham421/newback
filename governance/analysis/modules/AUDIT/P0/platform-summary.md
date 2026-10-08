# PLATFORM SUMMARY — سجل التدقيق العام / Audit (AUDIT)
══════════════════════════════════════════════════════════════════
Profile : erp   Source version : erp-core 1.2.0 (as built)   Analysis : v1 (as-built baseline)
Written : 2026-10-07, from the code at main @ 19b19a4 (1.3.0-SNAPSHOT; the audit code is unchanged
          since tag v1.2.0 apart from the helper move of 6b01816, no behaviour change)
══════════════════════════════════════════════════════════════════

Paths cited below are relative to the repository root, except: Java sources are relative to
`erp-core/src/main/java/com/erp/`, and core migrations (`V<N>__*.sql`) to
`erp-core/src/main/resources/db/migration/core/`. `file:line` points at main @ 19b19a4.

This analysis was NOT produced before the code: the audit module was built by erp-core plan step 10
(`erp-core-plan/10-STEP-generic-audit-log.md`, report `docs/steps/10-report.md`), rebased onto step 09 (the
`NumberSeries` annotation) and given its reference report by step 11. It records what exists, so that
every later audit change (the tenant-maturity plan's new actions, `docs/plans/tenant-maturity-plan.md`)
is written as an "Implementation Addendum" on top of it, like the other modules' 1.2.0 addenda. The full
description of the platform as a whole stays in
[`../../SEC/P0/platform-summary.md`](../../SEC/P0/platform-summary.md) → "Implementation Addendum —
erp-core 1.2.0" (L85–146: the module table, the per-module summary blocks and the platform conventions;
the single-copy decision recorded there); this file describes only the audit module's place in it.

## OVERVIEW
وحدة التدقيق العام هي سجل واحد لكل المنصة يجيب عن "من غيّر ماذا ومتى" (`CORE_AUDIT_EVENT`)، خاص بكل
مستأجر. تكتب فيه الوحدات والتطبيقات حقائق صريحة عبر `AuditApi.record` أو تُدرج كياناتها فيه بالتعليق
`@Audited` فتُسجَّل تغييرات حقولها تلقائيًا، متزامنةً داخل معاملة الكاتب وعلى اتصاله نفسه، فلا يبقى
سجل لتغيير تراجع. الحقول الحسّاسة لا تُكتب أبدًا. يقرأ الموظفون السجل عبر `GET /api/v1/audit/events`
بصلاحية `AUDIT:EVENT:READ`، والاحتفاظ به قرار التطبيق. [`erp-core-plan/10-STEP-generic-audit-log.md`; docs/steps/10-report.md Summary]

The audit module is the platform's single, tenant-scoped "who changed what, when" store
(`CORE_AUDIT_EVENT`). Modules and applications record explicit facts through `AuditApi.record`, or opt
their entities in with `@Audited` so that field-level changes are recorded automatically — synchronously,
inside the writer's transaction and on its connection, so a rolled-back change leaves no row. Sensitive
fields are never written. Staff read the log at `GET /api/v1/audit/events` with `AUDIT:EVENT:READ`;
retention is the application's decision.

## THE MODULE IN THE PLATFORM (as built)
| Aspect | As built | Code location |
|---|---|---|
| Package / surface | `com.erp.audit`; public surface = `com.erp.audit.crossmodule` only (`AuditApi`, `AuditEntry`, `AuditChange`, `@Audited`), enforced by ArchUnit | `erp-core/src/test/java/com/erp/architecture/CrossModuleBoundaryArchTest.java:63-65`; `autoconfigure/ErpCoreAutoConfiguration.java:92` |
| Table | `CORE_AUDIT_EVENT`, tenant-scoped, append-only; `CHANGES JSONB` = `[{field, old, new}]`; `OCCURRED_AT TIMESTAMP` (without zone, UTC wall-clock — the one departure from the chain's `TIMESTAMPTZ`) | `V15__audit_schema.sql:14-46`; `audit/entity/AuditEvent.java:29-85`; ADR-AUDIT-003 |
| Writers | explicit `AuditApi.record(AuditEntry)` and the opt-in `@Audited` Hibernate listener (`POST_INSERT` / `POST_UPDATE` / `POST_DELETE`); both go through `AuditRecordingService` → `AuditEventStore` (one JDBC `INSERT` on the writer's own connection) | `audit/crossmodule/AuditApi.java:40-47`; `audit/listener/AuditedEntityListener.java:63-139`; `audit/service/AuditEventStore.java:43-90`; ADR-AUDIT-001 |
| Redaction | field names containing `password`, `secret`, `token`, `hash`, `credential`, `apikey`, `privatekey`, `salt` (case-insensitive substrings), the technical fields `createdBy`, `createdAt`, `updatedBy`, `updatedAt`, `version`, `tenantId`, the entity's `@Audited(ignore)` list, collections and binary values are never written | `audit/crossmodule/AuditApi.java:32-38`; `audit/domain/AuditEventDomain.java:28-29`, `:50-73`; `audit/listener/AuditedEntityListener.java:157-183`; ADR-AUDIT-002 |
| Audited core entities (10) | `SEC_USER` (ignores `passwordHash`, `lastLoginAt`), `SEC_ROLE`, `CORE_TENANT`, `FILE_DOCUMENT`, `FILE_CATEGORY`, `NOTIF_TEMPLATE`, `MDL_LOOKUP_TYPE`, `MDL_LOOKUP_VALUE`, `CU_APP_CONFIGURATION`, `CORE_NUMBER_SERIES` (ignores `nextValue`) | `sec/entity/User.java:32`; `sec/entity/Role.java:30`; `tenant/entity/Tenant.java:33`; `file/entity/FileDocument.java:45`; `file/entity/FileCategory.java:33`; `notif/entity/NotificationTemplate.java:33`; `mdl/entity/LookupType.java:34`; `mdl/entity/LookupValue.java:38`; `cu/entity/AppConfiguration.java:38`; `sequence/entity/NumberSeries.java:41` |
| SEC dual write | `SEC_AUDIT_LOG` unchanged; SEC additionally records `LOGIN` (staff and customer), `LOGOUT` (staff) and `PASSWORD_RESET` (staff and customer completion) here, actor = the account | `sec/service/SecAuditEntries.java:12-35`; `sec/service/AuthService.java:117`, `:166`; `sec/service/CustomerAccountService.java:206`, `:266`; `sec/service/PasswordResetService.java:146`; ADR-AUDIT-003 |
| Query API | `GET /api/v1/audit/events` — `entityType`, `entityId`, `actor`, `action` (exact), `from` / `to` (ISO-8601 instants), `page`, `size`; newest first; tenant-isolated; authority `AUDIT:EVENT:READ`; staff chain (a customer token gets 403 `REALM_MISMATCH`) | `audit/controller/AuditEventController.java:24-45`; `audit/service/AuditEventService.java:55-82`; `docs/api-docs/audit/index.md:110-116` |
| Permissions | registry module `AUDIT` (سجل التدقيق العام / Audit Log), screen `AUDIT_EVENTS` (سجل أحداث التدقيق / Audit events), action `VIEW` carrying the literal authority `AUDIT:EVENT:READ` (gateway and read in one grant); code-contributed, never seeded; every super role holds it. Report bridge: screen `AUDIT_REPORTS` (تقارير AUDIT / AUDIT Reports), gateway `PERM_AUDIT_REPORTS_VIEW`, action `AUDIT_EVENT_LIST` → `AUDIT:REPORT:AUDIT_EVENT_LIST` | `audit/permission/AuditPermissions.java:21-42`; `V15__audit_schema.sql:10-11`; `report/permission/ReportPermissions.java:50-68` |
| Report | `AuditEventListReport` (`AUDIT_EVENT_LIST`, module `AUDIT`): params `action`, `entityType`, `entityId`, `actor` (STRING), `occurredFrom`, `occurredTo` (DATETIME); 10 columns, newest first; `CHANGES` is not a column | `audit/report/AuditEventListReport.java:50-132` |
| Defaults of a row | tenant = `TenantContext.require()` (or the entity's own `TENANT_ID`), `occurredAt` = now, actor = principal name or `system`, realm = `CUSTOMER` / `STAFF` / `SYSTEM` (the `DomainEvent` rule), IP / User-Agent from the request filter `RequestInfoHolder` | `audit/service/AuditRecordingService.java:61-80`; `audit/web/RequestInfoHolder.java:27-61` |
| Retention | `AuditRetentionJob.run()`: `erp.core.audit.retention-days` (default 0 = keep forever); `@Scheduled(cron = "${erp.core.audit.retention-cron:-}")` fires only in an application with `@EnableScheduling` and a cron; deletes tenant by tenant with JDBC | `audit/service/AuditRetentionJob.java:24-46`; `audit/service/AuditEventStore.java:96-108`; `autoconfigure/ErpCoreProperties.java:358-374` |
| Errors | `AUDIT_ACTION_INVALID` 400 (from `AuditApi` only); `TENANT_CONTEXT_MISSING` 500 when recording without a tenant; `VALIDATION_ERROR` 400 for a malformed `from` / `to` | `audit/exception/AuditErrorCodes.java:11`; `audit/service/AuditRecordingService.java:65`; `common/search/InstantFieldValueConverter.java:30-34` |
| Configuration | `erp.core.audit.retention-days` (int, 0), `erp.core.audit.retention-cron` (String, `-`) | `autoconfigure/ErpCoreProperties.java:48-49`, `:358-374` |
| OpenAPI | no dedicated springdoc group (the aggregate document lists the endpoint); api-docs folder `docs/api-docs/audit/` | `autoconfigure/ErpCoreOpenApiAutoConfiguration.java` (no audit group); docs/DEVIATIONS.md [10] (query API shape) |

## DEPENDENCY MAP
```
AUDIT ──HARD (FK TENANT_ID → CORE_TENANT; TenantContext.require, XM-AUDIT-001)──▶ TENANT
AUDIT ──SPI (PermissionContributor: AuditPermissions)──▶ SEC
AUDIT ──SPI (ReportProvider: AuditEventListReport, XM-AUDIT-004)──▶ report
AUDIT ──foundation──▶ common (AuditableEntity, SecurityContextHelper, Strings, PlainJson, InstantFieldValueConverter, SpecBuilder, PageableBuilder)
SEC (AuthService, CustomerAccountService, PasswordResetService) ──crossmodule (AuditApi.record, XM-AUDIT-002)──▶ AUDIT
SEC, MDL, FILE, NOTIF, CU, TENANT, SEQUENCE ──crossmodule (@Audited, XM-AUDIT-003)──▶ AUDIT
Hibernate (POST_INSERT / POST_UPDATE / POST_DELETE) ──integrator (AuditHibernateConfiguration)──▶ AuditedEntityListener
```
Build order: created by V15 after every module whose entities it annotates (V2..V14); the listener is
registered when the `EntityManagerFactory` is built. Tier: core service (L1), beside the other `CORE_*`
modules.

## DEFERRED (not in scope of the as-built module)
| Item | Reason / activation trigger |
|---|---|
| Asynchronous or event-driven audit writer; a `DomainEvent` listener mirroring events into the log | not built by design: the step fixes a synchronous insert in the caller's transaction (docs/DEVIATIONS.md [10] "Step 08 events for async write"); an application may mirror events itself from an `AFTER_COMMIT` listener inside `TenantContext.runAs` |
| Write, edit or delete endpoints; per-row activation | not built: append-only read model (`audit/entity/AuditEvent.java:23-27`; docs/DEVIATIONS.md [10] query API shape) |
| `ACTOR_USER_ID` resolved by default from the principal | not built: would make audit depend on SEC while SEC depends on audit; SEC passes it itself (docs/DEVIATIONS.md [10] `ACTOR_USER_ID` default) |
| `X-Forwarded-For` trust for the IP | not built: `getRemoteAddr()` only; configure `server.forward-headers-strategy` behind a proxy (`audit/web/RequestInfoHolder.java:22-25`) |
| Core-scheduled retention | not built: core never enables scheduling (`audit/service/AuditRetentionJob.java:15-18`; docs/CONSUMING.md §2 "Scheduling") |
| New actions `ADMIN_PASSWORD_RESET`, `TOKENS_REVOKED`, `TENANT_EXPORTED`, `TENANT_LOGO_CHANGED`, `PASSWORD_SET_BY_ADMIN`, `PASSWORD_CHANGED`, `PROFILE_PHOTO_CHANGED` | tenant-maturity plan packages B–E (1.3.0) — callers' action codes, nothing in the audit module changes; each package records its code in `../P1/srs-audit.md` → "Implementation Addendum — erp-core 1.3.0" when it lands on main; the verified reference rows are in `docs/plans/tenant-maturity-analysis-reference.md` |

## OPEN ITEMS
None — this file describes what exists. Behaviour the code does not have is listed under DEFERRED,
never described as present.

## NEXT STEP
Module registry and policies: `module-registry-audit.md`, `business-policies-audit.md`.

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 10
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.
