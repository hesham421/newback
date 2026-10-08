## MODULE REGISTRY — سجل التدقيق العام / Audit (AUDIT)
══════════════════════════════════════════════════════════════════
Module Code    : AUDIT   (package `com.erp.audit`; permission-registry module `AUDIT`; api-docs folder `audit`)
Bounded context: platform
Layer / Type   : L1 / core service (cross-module audit trail)     Execution tier : core (V15, erp-core plan step 10)
Source         : AS-BUILT (erp-core 1.2.0; code at main @ 19b19a4)
Knowledge      : erp-core-plan/10-STEP-generic-audit-log.md; docs/steps/10-report.md; docs/DEVIATIONS.md [10], [11]; docs/CONSUMING.md §9
Readiness      : READY (built)
══════════════════════════════════════════════════════════════════

Java paths are relative to `erp-core/src/main/java/com/erp/`, migrations to
`erp-core/src/main/resources/db/migration/core/`.

ENTITIES OWNED   (entity ids are assigned in P1: ENT-AUDIT-001)
| Entity (ar/en) | Kind | PRIVATE / SHARED | Source |
|---|---|---|---|
| حدث تدقيق / AuditEvent (`CORE_AUDIT_EVENT`) | log (tenant-scoped, append-only; JPA read model, JDBC writes) | PRIVATE — no other module's table references it; other modules write only through `AuditApi` / `@Audited` and read only through the query API and the report | audit/entity/AuditEvent.java:19-38; V15__audit_schema.sql:14-46 |

Not entities, but owned runtime surface (no table): the write API `AuditApi` with `AuditEntry` and
`AuditChange`, the annotation `@Audited`, the domain `AuditEventDomain`, the Hibernate listener
`AuditedEntityListener` and its registration `AuditHibernateConfiguration`, the request filter
`RequestInfoHolder`, the retention job `AuditRetentionJob` and the report provider
`AuditEventListReport` (audit/crossmodule/AuditApi.java:18; audit/crossmodule/AuditEntry.java:39;
audit/crossmodule/AuditChange.java:11; audit/crossmodule/Audited.java:35; audit/domain/AuditEventDomain.java:19;
audit/listener/AuditedEntityListener.java:52; audit/config/AuditHibernateConfiguration.java:28;
audit/web/RequestInfoHolder.java:29; audit/service/AuditRetentionJob.java:24; audit/report/AuditEventListReport.java:48).

LOOKUPS OWNED
| Lookup key | Description | Initial values | Source |
|---|---|---|---|
| (value set of `CORE_AUDIT_EVENT.ACTOR_REALM`) | نطاق المنفذ / actor realm | `STAFF`, `CUSTOMER`, `SYSTEM` — a CHECK constraint (`CHK_CORE_AUDIT_EVENT_REALM`), the `SecurityContextHelper.REALM_*` constants, not an MDL lookup (same pattern as ADR-SEC-001) | V15__audit_schema.sql:42; audit/crossmodule/AuditApi.java:28-30; common/util/SecurityContextHelper.java:12-18 |
| (format of `CORE_AUDIT_EVENT.ACTION`) | الإجراء / action | free code `^[A-Z_]{3,64}$` (`CHK_CORE_AUDIT_EVENT_ACTION`); standard constants `CREATE`, `UPDATE`, `DELETE`, `STATUS_CHANGE`, `LOGIN`, `LOGOUT`, `PASSWORD_RESET` | V15__audit_schema.sql:41, :49; audit/crossmodule/AuditApi.java:20-26; audit/domain/AuditEventDomain.java:22 |

LOOKUPS CONSUMED
None.

SHARED ENTITIES CONSUMED
| Entity | Owner code | HARD-FK / SOFT-READ | Why |
|---|---|---|---|
| `CORE_TENANT` | TENANT | HARD-FK (`FK_CORE_AUDIT_EVENT_TENANT` on `TENANT_ID`) | every audit row belongs to one tenant |
Source: V15__audit_schema.sql:40. The audited entities of other modules are not consumed: the listener
reads the flushing entity's state from Hibernate's event, never their tables.

DEPENDENCIES
| Module code | HARD / SOFT / SPI | What is consumed | Source |
|---|---|---|---|
| TENANT | HARD (FK + context) | `CORE_TENANT(ID)`; `TenantContext.require()` (default tenant of an explicit entry and of a global entity's row); Hibernate `@TenantId` through `AuditableEntity` for reads | V15__audit_schema.sql:40; audit/service/AuditRecordingService.java:9, :65; audit/entity/AuditEvent.java:38 |
| SEC | SPI (implements) | `com.erp.sec.permission.PermissionContributor` — `AuditPermissions` declares module `AUDIT`, screen `AUDIT_EVENTS`, action `VIEW` with the literal authority `AUDIT:EVENT:READ` | audit/permission/AuditPermissions.java:19-42 |
| report | SPI (implements) | `com.erp.report.ReportProvider` (+ `ReportParam`, `ReportColumn`, `ReportResult`, `ReportAuthorities`) — `AuditEventListReport` | audit/report/AuditEventListReport.java:11-17, :48 |
| common | foundation | `AuditableEntity`, `SecurityContextHelper` (`currentActorOrSystem`, `currentRealm`, `REALM_*`), `Strings.truncate`, `PlainJson.MAPPER`, `InstantFieldValueConverter`, `SpecBuilder`, `PageableBuilder`, `ServiceResult`, `LocalizedException`/`Status`, `OperationCode`/`ApiResponse` | audit/service/AuditRecordingService.java:7-8; audit/service/AuditEventStore.java:5; audit/service/AuditEventService.java:7-15 |
| autoconfigure | properties | `ErpCoreProperties.Audit` — `erp.core.audit.retention-days`, `erp.core.audit.retention-cron` | audit/service/AuditRetentionJob.java:3, :27, :30, :37; autoconfigure/ErpCoreProperties.java:48-49, :358-374 |
| Hibernate | integrator | `HibernatePropertiesCustomizer` → `hibernate.integrator_provider` → `EventListenerRegistry.appendListeners(POST_INSERT / POST_UPDATE / POST_DELETE)`, composed with an application's existing provider | audit/config/AuditHibernateConfiguration.java:28-68 |
ROOT: NO — depends on TENANT (hard), SEC and report (SPI), common (foundation). Seven core modules depend
on AUDIT (EXPOSED SURFACE below); SEC and AUDIT depend on each other only through `crossmodule` packages
(docs/DEVIATIONS.md [10] "`ACTOR_USER_ID` default" avoids the cycle on the audit side).

EXPOSED SURFACE (consumed by other modules)
| Surface | Kind | Consumers | XM id (P1) | Source |
|---|---|---|---|---|
| `com.erp.audit.crossmodule.AuditApi.record(AuditEntry)` with `AuditEntry` (builder) and `AuditChange` | crossmodule call (in-process, no `@PreAuthorize`; synchronous in the caller's transaction) | SEC (`AuthService`, `CustomerAccountService`, `PasswordResetService` via `SecAuditEntries`); applications | XM-AUDIT-002 | audit/crossmodule/AuditApi.java:18-47; audit/crossmodule/AuditApiImpl.java:13-23; sec/service/SecAuditEntries.java:22-34 |
| `com.erp.audit.crossmodule.Audited` (`entityType`, `ignore`) | annotation read by the Hibernate listener | SEC (`User`, `Role`), TENANT (`Tenant`), FILE (`FileDocument`, `FileCategory`), NOTIF (`NotificationTemplate`), MDL (`LookupType`, `LookupValue`), CU (`AppConfiguration`), SEQUENCE (`NumberSeries`); applications | XM-AUDIT-003 | audit/crossmodule/Audited.java:32-45; the ten `@Audited(` sites listed in `platform-summary.md` |
| `AuditRetentionJob.run()` | public bean method | applications' own schedulers | — | audit/service/AuditRetentionJob.java:36-46; docs/CONSUMING.md §2 "Scheduling" |
| Report `AUDIT_EVENT_LIST` | `ReportProvider` registered in the report registry; run / export through `/api/v1/report/AUDIT_EVENT_LIST/{run,export}` | report module (REST), frontend | XM-AUDIT-004 | audit/report/AuditEventListReport.java:48-132; docs/api-docs/report/index.md:114-121 |

PERMISSION MODULE → SCREEN → ACTIONS (registry rows; code-registered, upserted by SEC's catalog synchronizer at startup — never seeded by a migration, V15__audit_schema.sql:10-11)
| Registry module | Screen (page code) | Action code | Authority | Meaning | Registered by | Source |
|---|---|---|---|---|---|---|
| `AUDIT` — سجل التدقيق العام / Audit Log | `AUDIT_EVENTS` — سجل أحداث التدقيق / Audit events | `VIEW` — عرض | `AUDIT:EVENT:READ` (literal, not `PERM_AUDIT_EVENTS_VIEW`) | gateway of the screen (RULE-SEC-007) and the read authority of `GET /api/v1/audit/events` — one grant suffices | `AuditPermissions` (code) | audit/permission/AuditPermissions.java:21-42 |
| `AUDIT` | `AUDIT_REPORTS` — تقارير AUDIT / AUDIT Reports | `VIEW` — عرض | `PERM_AUDIT_REPORTS_VIEW` | gateway of the per-module report screen | `ReportPermissions` (code, from the registered providers) | report/permission/ReportPermissions.java:50-53, :62-68 |
| `AUDIT` | `AUDIT_REPORTS` | `AUDIT_EVENT_LIST` — سجل أحداث التدقيق / Audit event list | `AUDIT:REPORT:AUDIT_EVENT_LIST` | run and export the audit list report; independent of `AUDIT:EVENT:READ` | `ReportPermissions` (code) | report/permission/ReportPermissions.java:54-57; audit/report/AuditEventListReport.java:50-53, :82-89 |
Every tenant's super role holds all three through the catalog (audit/permission/AuditPermissions.java:11-12).
`PermissionCatalogIntegrationTest` leaves the `com.erp.audit` contributor out of its seeded-catalog
comparison (docs/DEVIATIONS.md [10] permission entry). The api-doc generator prints the constant name
`AUDIT_EVENT_READ` for the endpoint's permission (`docs/api-docs/audit/endpoints/audit-log.md:16`); the
authority string the code checks is `AUDIT:EVENT:READ` (audit/service/AuditEventService.java:56).

AUTO-DECISIONS
AUTO: `ACTOR_REALM` is a CHECK-constrained column, not an MDL lookup; `ACTION` is a regex-checked free code
  FROM: V15__audit_schema.sql:41-42; audit/domain/AuditEventDomain.java:22 (callers name their own actions)
  IF WRONG: none — a closed action list would need a core change for every application fact.
AUTO: AuditEvent classified PRIVATE
  FROM: no FK from any table to `CORE_AUDIT_EVENT`; cross-module access only through `AuditApi`, `@Audited`, the query API and the report
  IF WRONG: none.
AUTO: PK column `ID` with `SEQ_CORE_AUDIT_EVENT`; `OCCURRED_AT` and the audit columns `TIMESTAMP` without zone
  FROM: V15__audit_schema.sql:14, :17, :19, :33-35; audit/service/AuditEventStore.java:34-36, :125-127 (UTC wall-clock binding)
  IF WRONG: a type change is forbidden by the additive-only rule; ADR-AUDIT-003 records the choice.

RESOLVED DECISIONS
| # | Point | Decision | Sources |
|---|---|---|---|
| 1 | Write path | synchronous JDBC on the caller's connection, including from inside the Hibernate flush | ADR-AUDIT-001 |
| 2 | Redaction | name-substring denylist + technical fields + `@Audited(ignore)` | ADR-AUDIT-002 |
| 3 | Coexistence, retention, timestamps | `SEC_AUDIT_LOG` dual write; opt-in app-scheduled retention; `OCCURRED_AT` TIMESTAMP without zone | ADR-AUDIT-003 |

POLICIES OWNED (full text in business-policies-audit.md)
POL-AUDIT-001, POL-AUDIT-002, POL-AUDIT-003, POL-AUDIT-004, POL-AUDIT-005, POL-AUDIT-006,
POL-AUDIT-007, POL-AUDIT-008, POL-AUDIT-009, POL-AUDIT-010, POL-AUDIT-011
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
