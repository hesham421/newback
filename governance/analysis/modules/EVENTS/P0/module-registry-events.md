## MODULE REGISTRY — أحداث المجال / Events (EVENTS)
══════════════════════════════════════════════════════════════════
Module Code    : EVENTS   (package `com.erp.events`; no permission-registry module, no api-docs folder)
Bounded context: platform (cross-cutting infrastructure)
Layer / Type   : L1 / in-process event bus (envelope, publisher, executor, event catalogue)     Execution tier : erp-core plan step 08
Source         : AS-BUILT (erp-core 1.2.0; code at main @ 19b19a4)
Knowledge      : erp-core-plan/08-STEP-events-and-async-notifications.md; docs/steps/08-report.md; docs/DEVIATIONS.md [08], [15]; docs/CONSUMING.md §5
Readiness      : READY (built)
══════════════════════════════════════════════════════════════════

Java paths are relative to `erp-core/src/main/java/com/erp/`.

ENTITIES OWNED   (no JPA entity, no table — the envelope is a value object; ids assigned in P1)
| Entity (ar/en) | Kind | PRIVATE / SHARED | Source |
|---|---|---|---|
| غلاف الحدث / Domain event envelope (`DomainEvent`) | value object, immutable, in memory only | SHARED (owner) — subclassed by every core event and by applications | events/DomainEvent.java:34-96 |

Not entities, but owned runtime surface (no table): `DomainEventPublisher` (interface) and its default
`SpringDomainEventPublisher`, `ErpCoreEvents.EXECUTOR` (the executor bean name), the executor
`erpCoreEventExecutor` and its `TenantAndSecurityContextTaskDecorator`, and the 10 event classes
(events/DomainEventPublisher.java:21; events/support/SpringDomainEventPublisher.java:16; events/ErpCoreEvents.java:16;
autoconfigure/ErpCoreEventsAutoConfiguration.java:44-55; events/support/TenantAndSecurityContextTaskDecorator.java:19).

EVENT CATALOGUE OWNED (10; every class `final`, `@Getter`, in the root package)
| Event | Payload (besides the envelope) | Publisher(s) | Core consumer | Source |
|---|---|---|---|---|
| `UserCreatedEvent` | `userId`, `username` | SEC `UserService.create`, `SignupRequestService.decide` (approve) | none (app example `UserCreatedEventProbe`) | events/UserCreatedEvent.java:7-16; sec/service/UserService.java:115; sec/service/SignupRequestService.java:147 |
| `UserStatusChangedEvent` | `userId`, `username`, `statusCode`, `active` | SEC `UserService.deactivate` / `reactivate` | none | events/UserStatusChangedEvent.java:7-20; sec/service/UserService.java:168, :186 |
| `CustomerRegisteredEvent` | `userId`, `email`; explicit ctor forces realm `CUSTOMER` | SEC `CustomerAccountService.register` | none | events/CustomerRegisteredEvent.java:11-27; sec/service/CustomerAccountService.java:142 |
| `CustomerVerifiedEvent` | `userId`; explicit ctor forces realm `CUSTOMER` | SEC `CustomerAccountService.verify` | none | events/CustomerVerifiedEvent.java:10-23; sec/service/CustomerAccountService.java:166 |
| `PasswordResetRequestedEvent` | `userId`, `resetTokenId`, `expiresAt` (never the raw token) | SEC `PasswordResetService` | none | events/PasswordResetRequestedEvent.java:11-22; sec/service/PasswordResetService.java:204 |
| `TenantCreatedEvent` | `tenantCode`; envelope `tenantId` = the NEW tenant, `actor` = the platform operator, realm `STAFF` | TENANT `TenantService.create` | none | events/TenantCreatedEvent.java:11-19; tenant/service/TenantService.java:99-100 |
| `FileDocumentPublishedEvent` | `documentId`, `moduleCode`, `ownerType`, `ownerId`, `fileName`, `contentType`, `visibility` (`PRIVATE` on store, `PUBLIC` when made public) | FILE `FileService.store`, `FileService.updateVisibility` | none | events/FileDocumentPublishedEvent.java:11-37; file/service/FileService.java:158, :339 |
| `NotificationRequestedEvent` | `notificationLogId`, `channel`, `recipientId`, `templateCode` | NOTIF `DispatchService` (inside the dispatch transaction), `NotificationRequeueJob` (outside any transaction) | NOTIF `NotificationDeliveryListener` | events/NotificationRequestedEvent.java:11-24; notif/service/DispatchService.java:156; notif/service/NotificationRequeueJob.java:97; notif/service/NotificationDeliveryListener.java:46-71 |
| `NotificationDispatchedEvent` | `notificationLogId`, `channel`, `recipientId`, `templateCode`, `attempts` | NOTIF `NotificationDeliveryProcessor` (`SENT`) | none | events/NotificationDispatchedEvent.java:7-23; notif/service/NotificationDeliveryProcessor.java:164 |
| `NotificationFailedEvent` | `notificationLogId`, `channel`, `recipientId`, `templateCode`, `attempts`, `lastError` | NOTIF `NotificationDeliveryProcessor` (`FAILED`) | none | events/NotificationFailedEvent.java:10-28; notif/service/NotificationDeliveryProcessor.java:225 |

LOOKUPS OWNED
| Lookup key | Description | Initial values | Source |
|---|---|---|---|
| (envelope `realm`) | نطاق الفاعل / actor realm | `STAFF`, `CUSTOMER`, `SYSTEM` — String constants `DomainEvent.REALM_*`, aliases of `SecurityContextHelper.REALM_*`; not an MDL lookup | events/DomainEvent.java:36-43; common/util/SecurityContextHelper.java:11-18 |

LOOKUPS CONSUMED
None.

SHARED ENTITIES CONSUMED
None — the module reads no table at all (no repository, no JDBC).

DEPENDENCIES
| Module code | HARD / SOFT / SPI | What is consumed | Source |
|---|---|---|---|
| tenant | root-package API | `TenantContext.current()` (captured by the no-arg constructor and by the decorator), `TenantContext.set` / `clear` (applied on the worker) | events/DomainEvent.java:4, :59; events/support/TenantAndSecurityContextTaskDecorator.java:3, :23, :43-49 |
| common | foundation | `SecurityContextHelper.currentActorOrSystem()`, `currentRealm()`, the `REALM_*` and `CUSTOMER_AUTHORITY` constants | events/DomainEvent.java:3, :37-46, :59-60 |
| autoconfigure | wiring | `ErpCoreEventsAutoConfiguration` (publisher bean, executor, `@EnableAsync`); `ErpCoreProperties.Events.Executor`; `com.erp.events` in `CORE_PACKAGE_LIST` | autoconfigure/ErpCoreEventsAutoConfiguration.java:33-55; autoconfigure/ErpCoreProperties.java:282-306; autoconfigure/ErpCoreAutoConfiguration.java:90 |
| Spring (framework) | runtime | `ApplicationEventPublisher`, `@TransactionalEventListener`, `@Async`, `ThreadPoolTaskExecutor`, `TaskDecorator`, `SecurityContextHolder` | events/support/SpringDomainEventPublisher.java:7; events/support/TenantAndSecurityContextTaskDecorator.java:4-6 |
ROOT: NO for code (depends on tenant and common); YES for data (owns and reads no table).

EXPOSED SURFACE (consumed by other modules)
| Surface | Kind | Consumers | XM id (P1) | Source |
|---|---|---|---|---|
| `com.erp.events` root package: `DomainEvent`, `DomainEventPublisher`, `ErpCoreEvents`, the 10 event classes (public in ArchUnit; `support` internal; `crossmodule` declared, empty) | public API (docs/RELEASE.md) | publishers SEC, TENANT, FILE, NOTIF; listener NOTIF; applications (publish and listen) | XM-EVENTS-001 | `erp-core/src/test/java/com/erp/architecture/CrossModuleBoundaryArchTest.java:57-61`; docs/CONSUMING.md §5 |
| `erpCoreEventExecutor` (`ErpCoreEvents.EXECUTOR`) | named bean | `@Async(ErpCoreEvents.EXECUTOR)` listeners; NOTIF injects it `@Qualifier(ErpCoreEvents.EXECUTOR)`; applications | — | autoconfigure/ErpCoreEventsAutoConfiguration.java:44; notif/service/NotificationDeliveryListener.java:38-40 |
| `DomainEventPublisher` bean (replaceable: `@ConditionalOnMissingBean`) | extension point | an application may provide its own publisher | — | autoconfigure/ErpCoreEventsAutoConfiguration.java:38-42 |

PERMISSION MODULE → SCREEN → ACTIONS
None — no screen, no endpoint, no permission, no error code.

AUTO-DECISIONS
AUTO: module code EVENTS for the analysis folder (package `com.erp.events`; the platform registry lists it as "Packages without an HTTP surface")
  FROM: governance/analysis/platform/project-registry.md; PROJECT-OVERVIEW.md packages table
  IF WRONG: rename the folder; no id changes.
AUTO: `DomainEvent` classified SHARED (owner)
  FROM: 10 core subclasses in four modules plus application subclasses
  IF WRONG: none — the subclasses exist.

RESOLVED DECISIONS
| # | Point | Decision | Sources |
|---|---|---|---|
| 1 | Bus model | in-process Spring events, after-commit, at-most-once, no broker or outbox | ADR-EVENTS-001 |
| 2 | Executor | tenant- and security-propagating `ThreadPoolTaskExecutor`, `defaultCandidate = false` | ADR-EVENTS-002 |
| 3 | Payload and placement | plain values, no secrets, tenant captured at publish; all events in the root package | ADR-EVENTS-003 |

POLICIES OWNED (full text in business-policies-events.md)
POL-EVENTS-001, POL-EVENTS-002, POL-EVENTS-003, POL-EVENTS-004, POL-EVENTS-005, POL-EVENTS-006
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 08
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : none recorded here yet — the three events of the tenant-maturity plan (C.1, D.3) are documented by the implementing run as each package lands on main
Statement      : This addendum records only what is already on main for 1.3.0; the plan packages' rows are written by the implementing run.

Plan packages B, C, D, E, G: documented by each package as it lands on main (analysis-first, written by the implementing run); the verified reference rows are in docs/plans/tenant-maturity-analysis-reference.md.
