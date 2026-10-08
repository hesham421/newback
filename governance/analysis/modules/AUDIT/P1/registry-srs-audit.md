## REGISTRY — P1 — AUDIT v1 (as-built baseline, erp-core 1.2.0)
══════════════════════════════════════════════════════════════════

Every id below is traced to the code it was read from (main @ 19b19a4). Java paths are relative to
`erp-core/src/main/java/com/erp/`, migrations to `erp-core/src/main/resources/db/migration/core/`.

Entities
| ENT id | Name (ar/en) | Kind | PRIVATE/SHARED | Status | Code location |
|---|---|---|---|---|---|
| ENT-AUDIT-001 | حدث تدقيق / AuditEvent | log (tenant-scoped, append-only; JPA read model, JDBC writes) | PRIVATE | REGISTERED (built) | audit/entity/AuditEvent.java:29-85; V15__audit_schema.sql:14-46 |

Consumed
| Owner | What | Kind | XM | Code location |
|---|---|---|---|---|
| TENANT | `CORE_TENANT(ID)` (FK of `TENANT_ID`); `TenantContext.require()` | HARD-FK + context | XM-AUDIT-001 | V15__audit_schema.sql:40; audit/service/AuditRecordingService.java:65 |
| SEC | `PermissionContributor` | SPI implemented | — | audit/permission/AuditPermissions.java:19-42 |
| report | `ReportProvider` (+ `ReportParam`, `ReportColumn`, `ReportResult`, `ReportAuthorities`) | SPI implemented (`AuditEventListReport`) | XM-AUDIT-004 | audit/report/AuditEventListReport.java:48-132 |

Cross-module surfaces (exposed direction)
| XM id | Surface | Kind | Consumers / implementers | Status | Code location |
|---|---|---|---|---|---|
| XM-AUDIT-002 | `com.erp.audit.crossmodule.AuditApi.record(AuditEntry)` (+ `AuditEntry`, `AuditChange`) | crossmodule call (ungated, synchronous) | SEC (`SecAuditEntries` via `AuthService`, `CustomerAccountService`, `PasswordResetService`); applications | ACTIVE (built) | audit/crossmodule/AuditApi.java:18-47; audit/crossmodule/AuditApiImpl.java:13-23 |
| XM-AUDIT-003 | `com.erp.audit.crossmodule.Audited` | annotation + Hibernate listener | SEC, TENANT, FILE, NOTIF, MDL, CU, SEQUENCE (10 entities); applications | ACTIVE (built) | audit/crossmodule/Audited.java:32-45; audit/listener/AuditedEntityListener.java:52-139 |
| XM-AUDIT-004 | report `AUDIT_EVENT_LIST` | `ReportProvider` registered in the report registry | report module, frontend | ACTIVE (built) | audit/report/AuditEventListReport.java:48-132 |

Lookups owned
| Key | ENT | Values count | Code location |
|---|---|---|---|
| ACTOR_REALM value set (`CHK_CORE_AUDIT_EVENT_REALM`) | ENT-AUDIT-001 | 3 (STAFF, CUSTOMER, SYSTEM) | V15__audit_schema.sql:42; audit/crossmodule/AuditApi.java:28-30 |
| ACTION (free code `^[A-Z_]{3,64}$`, `CHK_CORE_AUDIT_EVENT_ACTION`) | ENT-AUDIT-001 | 7 standard constants (open set) | V15__audit_schema.sql:41; audit/crossmodule/AuditApi.java:20-26 |

Lookups consumed
none.

Screens
| SCR-REQ id | Name (ar/en) | Page code | Code location |
|---|---|---|---|
| SCR-REQ-AUDIT-001 | سجل أحداث التدقيق / Audit events | AUDIT_EVENTS | audit/permission/AuditPermissions.java:26-27 |
| SCR-REQ-AUDIT-002 | تقارير AUDIT / AUDIT Reports (registry-only; REPORT module page) | AUDIT_REPORTS | report/permission/ReportPermissions.java:62-68; audit/report/AuditEventListReport.java:50-53 |

Requirements
REQ count: 21 · AC count: 21 · RULE count: 13 · ENT count: 1 · SCR-REQ count: 2 · API count: 1 (+ 2 REPORT endpoints, + 1 in-process) · XM count: 4
Last sequence per atom: REQ: 021 · AC: 021 · ENT: 001 · RULE: 013 · SCR-REQ: 002 · API: 003 · XM: 004 · US: 006 · POL: 011

## Id → code location
| Id | Title | Code location (primary) | Verified by |
|---|---|---|---|
| REQ-AUDIT-001 / AC-AUDIT-001 | Record an explicit entry | audit/service/AuditRecordingService.java:46-51, :60-81 | `AuditApiIntegrationTest.record_fillsDefaults_fromTheTenantAndSecurityContext` |
| REQ-AUDIT-002 / AC-AUDIT-002 | Reject an invalid action | audit/domain/AuditEventDomain.java:37-44 | `AuditApiIntegrationTest.record_withAnInvalidAction_isRejected`; `AuditEventDomainTest` |
| REQ-AUDIT-003 / AC-AUDIT-003 | Fail fast without a tenant | audit/service/AuditRecordingService.java:65 | `AuditApiIntegrationTest.record_withoutAnyTenant_failsFast_andAnExplicitTenantWins` |
| REQ-AUDIT-004 / AC-AUDIT-004 | No row for a rolled-back change | audit/service/AuditEventStore.java:23-32; audit/listener/AuditedEntityListener.java:138 | `AuditApiIntegrationTest.record_inARolledBackTransaction_…`; `SecAuditIntegrationTest.auditedChange_inARolledBackTransaction_…` |
| REQ-AUDIT-005 / AC-AUDIT-005 | Record a creation | audit/listener/AuditedEntityListener.java:63-76 | TC-CORE-AUDIT-001; `AuditLogApiIntegrationTest.creatingAUser_writesOneCreateRow_withoutThePasswordHash` |
| REQ-AUDIT-006 / AC-AUDIT-006 | Record an update | audit/listener/AuditedEntityListener.java:78-100 | TC-CORE-AUDIT-002; `AuditLogApiIntegrationTest.updatingTwoFields_writesOneUpdateRow_withExactlyTwoChanges`; `SecAuditIntegrationTest.passwordOnlyUpdate_writesNoRow_andAnotherFieldDoes` |
| REQ-AUDIT-007 / AC-AUDIT-007 | Record a deletion | audit/listener/AuditedEntityListener.java:102-118 | — (no core endpoint deletes an audited entity) |
| REQ-AUDIT-008 / AC-AUDIT-008 | Redaction of changes | audit/domain/AuditEventDomain.java:50-73; audit/listener/AuditedEntityListener.java:157-183 | TC-CORE-AUDIT-016; `AuditApiIntegrationTest.record_dropsSensitiveChanges_whoeverSuppliesThem`; `AuditRows.assertNoSensitiveFieldAnywhere` |
| REQ-AUDIT-009 / AC-AUDIT-009 | Default actor, time and client | audit/service/AuditRecordingService.java:63-78; audit/web/RequestInfoHolder.java:46-61 | TC-CORE-AUDIT-006; `AuditApiIntegrationTest.record_withoutCaller_isSystem_andCustomerAuthority_isCustomerRealm` |
| REQ-AUDIT-010 / AC-AUDIT-010 | Tenant of a row | audit/listener/AuditedEntityListener.java:129; audit/service/AuditRecordingService.java:65 | TC-CORE-AUDIT-011, -014; `AuditedEntitiesCoverageIntegrationTest.coreTenant`, `.cuAppConfiguration_platformDefault_isRecordedUnderTheActingPlatformTenant` |
| REQ-AUDIT-011 / AC-AUDIT-011 | Truncation of values | audit/service/AuditRecordingService.java:35-42, :90-99 | — |
| REQ-AUDIT-012 / AC-AUDIT-012 | Query the log | audit/service/AuditEventService.java:55-82 | TC-CORE-AUDIT-007, -010; `AuditLogApiIntegrationTest.query_returnsTheEntityHistory_newestFirst_forAHolderOfAuditEventRead`, `.query_isTenantIsolated` |
| REQ-AUDIT-013 / AC-AUDIT-013 | Reject malformed time bounds | common/search/InstantFieldValueConverter.java:30-34; audit/service/AuditEventService.java:45 | TC-CORE-AUDIT-010; `AuditLogApiIntegrationTest.query_withAMalformedDate_is400` |
| REQ-AUDIT-014 / AC-AUDIT-014 | Authorisation of the query | audit/service/AuditEventService.java:56; audit/controller/AuditEventController.java:18-21 | TC-CORE-AUDIT-008, -009; `AuditLogApiIntegrationTest.query_withoutAuditEventRead_is403_andWithoutToken_is401`, `.query_customerToken_is403` |
| REQ-AUDIT-015 / AC-AUDIT-015 | Append-only log | audit/controller/AuditEventController.java:17-32; audit/entity/AuditEvent.java:23-30 | `docs/api-docs/audit/index.md:110-116` |
| REQ-AUDIT-016 / AC-AUDIT-016 | SEC account events in the log | sec/service/SecAuditEntries.java:22-34 | TC-CORE-AUDIT-003…006; `AuditLogApiIntegrationTest.loginAndLogout_areRecordedForTheAccount`; `SecAuditIntegrationTest.completedPasswordReset_isRecordedAsPasswordReset_forTheAccount` |
| REQ-AUDIT-017 / AC-AUDIT-017 | Audited core entities | the ten `@Audited(` sites (srs-audit.md A4) | TC-CORE-AUDIT-012, -013, -015; `AuditedEntitiesCoverageIntegrationTest` (9 tests) |
| REQ-AUDIT-018 / AC-AUDIT-018 | Apply retention | audit/service/AuditRetentionJob.java:30-46; audit/service/AuditEventStore.java:92-108 | `AuditRetentionJobIntegrationTest.retentionDeletesRowsOlderThanNDays_inEveryTenant_andKeepsTheRest` |
| REQ-AUDIT-019 / AC-AUDIT-019 | Code-contributed permission catalog | audit/permission/AuditPermissions.java:10-42 | TC-CORE-SEC-003; `AuditApiIntegrationTest.theReadPermission_isSynchronizedIntoTheCatalog_fromTheContributor` |
| REQ-AUDIT-020 / AC-AUDIT-020 | The audit event list report | audit/report/AuditEventListReport.java:92-132 | TC-CORE-REPORT-002, -014, -016 |
| REQ-AUDIT-021 / AC-AUDIT-021 | Registering the listener with Hibernate | audit/config/AuditHibernateConfiguration.java:36-68 | every listener test above |
| RULE-AUDIT-001 | Action format | audit/domain/AuditEventDomain.java:22, :37-44; V15__audit_schema.sql:41 | `AuditEventDomainTest` |
| RULE-AUDIT-002 | Synchronous write in the caller's transaction | audit/service/AuditEventStore.java:53-59; audit/service/AuditRecordingService.java:20-29 | `AuditApiIntegrationTest.record_inARolledBackTransaction_…` |
| RULE-AUDIT-003 | Listener writes from inside the flush with JDBC | audit/listener/AuditedEntityListener.java:120-139; audit/config/AuditHibernateConfiguration.java:36-68 | `SecAuditIntegrationTest.auditedChange_inARolledBackTransaction_…` |
| RULE-AUDIT-004 | Redaction rules | audit/domain/AuditEventDomain.java:28-29, :50-73; audit/listener/AuditedEntityListener.java:157-183 | TC-CORE-AUDIT-016 |
| RULE-AUDIT-005 | Defaults | audit/service/AuditRecordingService.java:61-80 | `AuditApiIntegrationTest.record_fillsDefaults_…` |
| RULE-AUDIT-006 | Width limits | audit/service/AuditRecordingService.java:35-42, :84-99; audit/web/RequestInfoHolder.java:31-33 | — |
| RULE-AUDIT-007 | Tenant of a row | audit/listener/AuditedEntityListener.java:129; audit/service/AuditRecordingService.java:65 | TC-CORE-AUDIT-011, -014 |
| RULE-AUDIT-008 | Audited entities and their ignores | the ten `@Audited(` sites | `AuditedEntitiesCoverageIntegrationTest` |
| RULE-AUDIT-009 | SEC dual write | sec/service/SecAuditEntries.java:12-35 | TC-CORE-AUDIT-003…006 |
| RULE-AUDIT-010 | Retention semantics | audit/service/AuditRetentionJob.java:30-46 | `AuditRetentionJobIntegrationTest` |
| RULE-AUDIT-011 | No asynchronous writer | docs/DEVIATIONS.md [10] "Step 08 events for async write" | — (absence) |
| RULE-AUDIT-012 | Query filters and order | audit/service/AuditEventService.java:39-45, :63-80 | TC-CORE-AUDIT-010 |
| RULE-AUDIT-013 | Append-only, no write endpoint | audit/controller/AuditEventController.java:17-32; audit/entity/AuditEvent.java:23-30 | `docs/api-docs/audit/index.md:110-116` |
| ENT-AUDIT-001 | AuditEvent | audit/entity/AuditEvent.java:29-85 | `TenantSchemaIntegrationTest` (tenant column) |
| XM-AUDIT-001 | CORE_TENANT FK + TenantContext | V15__audit_schema.sql:40; audit/service/AuditRecordingService.java:65 | `AuditApiIntegrationTest.record_withoutAnyTenant_…` |
| XM-AUDIT-002 | AuditApi | audit/crossmodule/AuditApi.java:18-47 | ArchUnit `CrossModuleBoundaryArchTest` |
| XM-AUDIT-003 | @Audited | audit/crossmodule/Audited.java:32-45 | `AuditedEntitiesCoverageIntegrationTest` |
| XM-AUDIT-004 | AuditEventListReport | audit/report/AuditEventListReport.java:48-132 | TC-CORE-REPORT-002, -014, -016 |
| SCR-REQ-AUDIT-001 | AUDIT_EVENTS | audit/permission/AuditPermissions.java:26-27, :41 | `governance/frontend/modules/AUDIT/tests/specs/audit/audit-events.spec.ts` |
| SCR-REQ-AUDIT-002 | AUDIT_REPORTS | report/permission/ReportPermissions.java:50-68 | `governance/frontend/modules/REPORT/tests/` |
| API-AUDIT-001 | GET /api/v1/audit/events | audit/controller/AuditEventController.java:32-45 | `docs/api-docs/audit/index.md:110-116` |
| API-AUDIT-002 / 003 | REPORT run / export of `AUDIT_EVENT_LIST` | audit/report/AuditEventListReport.java:102-132 | `docs/api-docs/report/index.md:114-121` |
| US-AUDIT-001…006 | stories | `../P0_5/prd-audit.md` | — |
| POL-AUDIT-001…011 | policies | `../P0/business-policies-audit.md` | — |

REQ ids (full text in srs-audit.md → A4): REQ-AUDIT-001, REQ-AUDIT-002, REQ-AUDIT-003, REQ-AUDIT-004,
REQ-AUDIT-005, REQ-AUDIT-006, REQ-AUDIT-007, REQ-AUDIT-008, REQ-AUDIT-009, REQ-AUDIT-010, REQ-AUDIT-011,
REQ-AUDIT-012, REQ-AUDIT-013, REQ-AUDIT-014, REQ-AUDIT-015, REQ-AUDIT-016, REQ-AUDIT-017, REQ-AUDIT-018,
REQ-AUDIT-019, REQ-AUDIT-020, REQ-AUDIT-021

AC ids (full text in srs-audit.md → A4, one per REQ above): AC-AUDIT-001 … AC-AUDIT-021

RULE ids (full text in srs-audit.md → A5): RULE-AUDIT-001, RULE-AUDIT-002, RULE-AUDIT-003, RULE-AUDIT-004,
RULE-AUDIT-005, RULE-AUDIT-006, RULE-AUDIT-007, RULE-AUDIT-008, RULE-AUDIT-009, RULE-AUDIT-010,
RULE-AUDIT-011, RULE-AUDIT-012, RULE-AUDIT-013

APIs (full table in srs-audit.md → B5)
| API-ID | Method | Endpoint | Owning SCR-ID | Permission |
|---|---|---|---|---|
| API-AUDIT-001 | GET | /api/v1/audit/events | SCR-REQ-AUDIT-001 | `AUDIT:EVENT:READ` |
| API-AUDIT-002 (REPORT) | POST | /api/v1/report/AUDIT_EVENT_LIST/run | SCR-REQ-AUDIT-002 | `AUDIT:REPORT:AUDIT_EVENT_LIST` |
| API-AUDIT-003 (REPORT) | POST | /api/v1/report/AUDIT_EVENT_LIST/export | SCR-REQ-AUDIT-002 | `AUDIT:REPORT:AUDIT_EVENT_LIST` |
In-process (not HTTP): `AuditApi.record(AuditEntry)` — XM-AUDIT-002; `@Audited` — XM-AUDIT-003; `AuditRetentionJob.run()`.

Error codes (full table in srs-audit.md → A4)
| Code | HTTP | Code location |
|---|---|---|
| `AUDIT_ACTION_INVALID` | 400 | audit/domain/AuditEventDomain.java:40-41 |
| `TENANT_CONTEXT_MISSING` (tenant) | 500 | audit/service/AuditRecordingService.java:65 → tenant/TenantContext.java:50 |
| `VALIDATION_ERROR` (common) | 400 | common/search/InstantFieldValueConverter.java:33 |
`AUDIT_ACTION_INVALID`: audit/exception/AuditErrorCodes.java:11; i18n `messages.properties` 175, `messages_ar.properties` 172.

Permissions (Permissions Summary)
`AUDIT:EVENT:READ` (module `AUDIT`, screen `AUDIT_EVENTS`, action `VIEW` — gateway and read in one;
code-registered by `AuditPermissions`, never seeded); `PERM_AUDIT_REPORTS_VIEW` and
`AUDIT:REPORT:AUDIT_EVENT_LIST` (screen `AUDIT_REPORTS`; registered by `ReportPermissions` from the provider).

Decisions
ADR ids: ADR-AUDIT-001, ADR-AUDIT-002, ADR-AUDIT-003 (all ACCEPTED, as built) — `governance/analysis/decisions/AUDIT/`.

Event
"P1 completed: AUDIT v1 (as built) — 1 entity, 21 requirements, 21 acceptance criteria, 13 rules, 2 screen requirements, 1 API (+2 REPORT), 4 XM, 3 ADRs"
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 10
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.
