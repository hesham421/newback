## REGISTRY — P1 — EVENTS v1 (as-built baseline, erp-core 1.2.0)
══════════════════════════════════════════════════════════════════

Every id below is traced to the code it was read from (main @ 19b19a4). Java paths are relative to
`erp-core/src/main/java/com/erp/`.

Entities
| ENT id | Name (ar/en) | Kind | PRIVATE/SHARED | Status | Code location |
|---|---|---|---|---|---|
| ENT-EVENTS-001 | غلاف الحدث / Domain event envelope (`DomainEvent`) | immutable value object (no table) | SHARED (owner) | REGISTERED (built) | events/DomainEvent.java:34-96 |

Event catalogue (10 subclasses of ENT-EVENTS-001; full table in srs-events.md → A3)
| Event | Payload | Publisher(s) | Core consumer | Code location |
|---|---|---|---|---|
| `UserCreatedEvent` | userId, username | SEC `UserService:115`, `SignupRequestService:147` | — | events/UserCreatedEvent.java:7-16 |
| `UserStatusChangedEvent` | userId, username, statusCode, active | SEC `UserService:168`, `:186` | — | events/UserStatusChangedEvent.java:7-20 |
| `CustomerRegisteredEvent` | userId, email (realm CUSTOMER) | SEC `CustomerAccountService:142` | — | events/CustomerRegisteredEvent.java:11-27 |
| `CustomerVerifiedEvent` | userId (realm CUSTOMER) | SEC `CustomerAccountService:166` | — | events/CustomerVerifiedEvent.java:10-23 |
| `PasswordResetRequestedEvent` | userId, resetTokenId, expiresAt | SEC `PasswordResetService:204` | — | events/PasswordResetRequestedEvent.java:11-22 |
| `TenantCreatedEvent` | tenantCode (tenant = the new tenant) | TENANT `TenantService:99-100` | — | events/TenantCreatedEvent.java:11-19 |
| `FileDocumentPublishedEvent` | documentId, moduleCode, ownerType, ownerId, fileName, contentType, visibility | FILE `FileService:158`, `:339` | — | events/FileDocumentPublishedEvent.java:11-37 |
| `NotificationRequestedEvent` | notificationLogId, channel, recipientId, templateCode | NOTIF `DispatchService:156`, `NotificationRequeueJob:97` | NOTIF `NotificationDeliveryListener:46-71` | events/NotificationRequestedEvent.java:11-24 |
| `NotificationDispatchedEvent` | + attempts | NOTIF `NotificationDeliveryProcessor:164` | — | events/NotificationDispatchedEvent.java:7-23 |
| `NotificationFailedEvent` | + attempts, lastError | NOTIF `NotificationDeliveryProcessor:225` | — | events/NotificationFailedEvent.java:10-28 |

Consumed
| Owner | What | Kind | XM | Code location |
|---|---|---|---|---|
| tenant | `TenantContext.current()`, `.set()`, `.clear()` | runtime API (root package) | XM-EVENTS-002 | events/DomainEvent.java:59; events/support/TenantAndSecurityContextTaskDecorator.java:23, :43-49 |
| common | `SecurityContextHelper.currentActorOrSystem()`, `.currentRealm()`, `REALM_*`, `CUSTOMER_AUTHORITY` | foundation | — | events/DomainEvent.java:37-46, :59-60 |
No table is read (no repository, no JDBC).

Cross-module surfaces (exposed direction)
| XM id | Surface | Kind | Consumers / implementers | Status | Code location |
|---|---|---|---|---|---|
| XM-EVENTS-001 | the root package `com.erp.events` (`DomainEvent`, `DomainEventPublisher`, `ErpCoreEvents`, 10 events) | public API: publish and listen | SEC, TENANT, FILE, NOTIF (publish); NOTIF (listen); applications | ACTIVE (built) | `erp-core/src/test/java/com/erp/architecture/CrossModuleBoundaryArchTest.java:57-61` |
| XM-EVENTS-002 | (consume direction) `TenantContext` | runtime API | EVENTS → TENANT | ACTIVE (built) | events/DomainEvent.java:4 |
Note: `com.erp.events.support` is internal; `com.erp.events.crossmodule` is declared in ArchUnit but holds no class.

Lookups owned
| Key | ENT | Values count | Code location |
|---|---|---|---|
| realm (String constants aliased from `SecurityContextHelper`) | ENT-EVENTS-001 | 3 (STAFF, CUSTOMER, SYSTEM) | events/DomainEvent.java:36-43 |

Lookups consumed
none.

Screens
None — no screen, no endpoint (no SCR-REQ id).

Requirements
REQ count: 13 · AC count: 13 · RULE count: 9 · ENT count: 1 · SCR-REQ count: 0 · XM count: 2
Last sequence per atom: REQ: 013 · AC: 013 · ENT: 001 · RULE: 009 · SCR-REQ: none · XM: 002 · US: 005 · POL: 006

## Id → code location
| Id | Title | Code location (primary) | Verified by |
|---|---|---|---|
| REQ-EVENTS-001 / AC-EVENTS-001 | Publish an event through the publisher | events/support/SpringDomainEventPublisher.java:24-29 | `DomainEventBusIntegrationTest` (4) |
| REQ-EVENTS-002 / AC-EVENTS-002 | Deliver after the commit only | events/DomainEventPublisher.java:11-13 | `DomainEventBusIntegrationTest.eventPublishedInATransaction_isDeliveredOnlyAfterCommit_andNeverOnRollback` |
| REQ-EVENTS-003 / AC-EVENTS-003 | Deliver immediately outside a transaction | events/DomainEventPublisher.java:11-13; notif/service/NotificationRequeueJob.java:97 | `DomainEventBusIntegrationTest.eventPublishedOutsideATransaction_isDeliveredImmediately_withTheTenantAndPrincipalOfThePublisher` |
| REQ-EVENTS-004 / AC-EVENTS-004 | Capture tenant, actor and realm at publish | events/DomainEvent.java:57-70 | `DomainEventTest` (3) |
| REQ-EVENTS-005 / AC-EVENTS-005 | Publish on behalf of another tenant or realm | events/DomainEvent.java:63-70; events/TenantCreatedEvent.java:15-18 | `DomainEventBusIntegrationTest.customerRegistration_publishesCustomerRegisteredEvent` |
| REQ-EVENTS-006 / AC-EVENTS-006 | Asynchronous listener on the core executor | autoconfigure/ErpCoreEventsAutoConfiguration.java:44-55 | `ReferenceApplicationSmokeTest.anApplicationListener_receivesTheCoreUserCreatedEvent` |
| REQ-EVENTS-007 / AC-EVENTS-007 | Propagate the context to the worker and restore it | events/support/TenantAndSecurityContextTaskDecorator.java:22-57 | `TenantAndSecurityContextTaskDecoratorTest` (2) |
| REQ-EVENTS-008 / AC-EVENTS-008 | The event's tenant is authoritative | notif/service/NotificationDeliveryListener.java:48-53, :61 | `NotificationAsyncDeliveryIntegrationTest.theWorkerRunsAsTheDispatchingTenant` |
| REQ-EVENTS-009 / AC-EVENTS-009 | An event carries plain values only | events/DomainEvent.java:25-26; events/*.java | A3 field inspection |
| REQ-EVENTS-010 / AC-EVENTS-010 | The event id is the idempotency key | events/DomainEvent.java:65, :72-74 | `DomainEventTest` |
| REQ-EVENTS-011 / AC-EVENTS-011 | The publisher is replaceable | autoconfigure/ErpCoreEventsAutoConfiguration.java:38-42 | `@ConditionalOnMissingBean` |
| REQ-EVENTS-012 / AC-EVENTS-012 | The core executor never replaces the application's | autoconfigure/ErpCoreEventsAutoConfiguration.java:33, :44-45 | `ErpCoreEventsAndNotifAutoConfigurationTest` |
| REQ-EVENTS-013 / AC-EVENTS-013 | Configure the executor | autoconfigure/ErpCoreProperties.java:291-304; autoconfigure/ErpCoreEventsAutoConfiguration.java:46-52 | — |
| RULE-EVENTS-001 | Publishing is synchronous and cheap | events/support/SpringDomainEventPublisher.java:24-29 | `NotificationAsyncDeliveryIntegrationTest.dispatch_returnsInUnder50ms_withoutWaitingForTheSend` |
| RULE-EVENTS-002 | Publish from the service, inside its transaction, after the write | events/DomainEventPublisher.java:18-19 | docs/steps/08-report.md "Publishers" |
| RULE-EVENTS-003 | Listener contract | events/DomainEventPublisher.java:11-16 | `DomainEventBusIntegrationTest` |
| RULE-EVENTS-004 | The named executor | autoconfigure/ErpCoreEventsAutoConfiguration.java:33-36, :44-55 | `ErpCoreEventsAndNotifAutoConfigurationTest` |
| RULE-EVENTS-005 | The decorator propagates and restores the context | events/support/TenantAndSecurityContextTaskDecorator.java:22-57 | `TenantAndSecurityContextTaskDecoratorTest` |
| RULE-EVENTS-006 | The event's tenant is authoritative | events/DomainEvent.java:17-20 | `NotificationAsyncDeliveryIntegrationTest.theWorkerRunsAsTheDispatchingTenant` |
| RULE-EVENTS-007 | Plain values only | events/DomainEvent.java:25-26 | A3 |
| RULE-EVENTS-008 | No-arg captures, explicit acts on behalf | events/DomainEvent.java:28-33, :57-70 | `DomainEventTest` |
| RULE-EVENTS-009 | `ErpCoreEvents` holds only the executor name | events/ErpCoreEvents.java:4-17 | — |
| ENT-EVENTS-001 | Domain event envelope | events/DomainEvent.java:34-96 | `DomainEventTest` |
| XM-EVENTS-001 | the root package | `CrossModuleBoundaryArchTest.java:57-61` | ArchUnit |
| XM-EVENTS-002 | TenantContext (consumed) | events/DomainEvent.java:4 | — |
| US-EVENTS-001…005 | stories | `../P0_5/prd-events.md` | — |
| POL-EVENTS-001…006 | policies | `../P0/business-policies-events.md` | — |

REQ ids (full text in srs-events.md → A4): REQ-EVENTS-001, REQ-EVENTS-002, REQ-EVENTS-003,
REQ-EVENTS-004, REQ-EVENTS-005, REQ-EVENTS-006, REQ-EVENTS-007, REQ-EVENTS-008, REQ-EVENTS-009,
REQ-EVENTS-010, REQ-EVENTS-011, REQ-EVENTS-012, REQ-EVENTS-013

AC ids (full text in srs-events.md → A4, one per REQ above): AC-EVENTS-001 … AC-EVENTS-013

RULE ids (full text in srs-events.md → A5): RULE-EVENTS-001, RULE-EVENTS-002, RULE-EVENTS-003,
RULE-EVENTS-004, RULE-EVENTS-005, RULE-EVENTS-006, RULE-EVENTS-007, RULE-EVENTS-008, RULE-EVENTS-009

Error codes
none owned.

Permissions
none.

Configuration
`erp.core.events.executor.core-pool-size` 4 · `.max-pool-size` 16 · `.queue-capacity` 500 · `.thread-name-prefix` `erp-event-`
(autoconfigure/ErpCoreProperties.java:291-304).

Decisions
ADR ids: ADR-EVENTS-001, ADR-EVENTS-002, ADR-EVENTS-003 (all ACCEPTED, as built) — `governance/analysis/decisions/EVENTS/`.

Event
"P1 completed: EVENTS v1 (as built) — 1 entity (value object), 10 catalogued events, 13 requirements, 13 acceptance criteria, 9 rules, 0 screen requirements, 2 XM, 3 ADRs"
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
