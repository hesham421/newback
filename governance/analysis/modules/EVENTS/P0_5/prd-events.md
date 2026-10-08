# PRD — أحداث المجال / Events (EVENTS)
══════════════════════════════════════════════════════════════════
Module          : EVENTS     Version : v1 (as-built baseline, erp-core 1.2.0)
Source artifacts: platform-summary, module-registry-events, business-policies-events
Stories         : 5   Policies covered : 6/6   Deferred : 0
Status          : AS-BUILT — every story is implemented; the code location is in `Source`
══════════════════════════════════════════════════════════════════

Java paths are relative to `erp-core/src/main/java/com/erp/`. There is no HTTP surface, so no
`TC-CORE-*` case targets the bus directly; verification is by the JUnit classes named in each story
(`erp-core/src/test/java/com/erp/events/`, `erp-app-reference/src/test/java/com/erp/app/`).

## USER STORIES

US-EVENTS-001
  Title          : نشر حقيقة دون معرفة المستمعين / Publish a fact without knowing the listeners
  Story          : As a core module that changed state (a user created, a tenant provisioned, a file stored, a notification queued), I need to publish a fact inside my transaction and forget it, so that other modules and the application can react without being called by me and without my transaction waiting for them.
  Priority       : HIGH — the module's purpose (08-STEP Goal)
  Success metric : `NotificationDispatchApi.dispatch` returns in < 50 ms with the send blocked (`NotificationAsyncDeliveryIntegrationTest.dispatch_returnsInUnder50ms_withoutWaitingForTheSend`)
  Traces         : POL-EVENTS-001, POL-EVENTS-002, POL-EVENTS-004
  Source         : events/DomainEventPublisher.java:21-25; events/support/SpringDomainEventPublisher.java:24-29; sec/service/UserService.java:115
  Status         : AS-BUILT

US-EVENTS-002
  Title          : التفاعل مع حقيقة أساسية بعد الالتزام / React to a core fact after the commit
  Story          : As an application (or core module) developer, I need to listen to a core event and be sure the change it describes is committed, so that my listener never works on data that was rolled back.
  Priority       : HIGH (08-STEP acceptance b3 "the app-side extension door")
  Success metric : the application's `UserCreatedEventProbe` receives `UserCreatedEvent` with userId, username, tenant 1 and actor `admin` on an `erp-event-*` thread (`ReferenceApplicationSmokeTest.anApplicationListener_receivesTheCoreUserCreatedEvent`); nothing on rollback (`DomainEventBusIntegrationTest.eventPublishedInATransaction_isDeliveredOnlyAfterCommit_andNeverOnRollback`)
  Traces         : POL-EVENTS-001, POL-EVENTS-003
  Source         : events/DomainEventPublisher.java:8-19; `erp-app-reference/src/test/java/com/erp/app/UserCreatedEventProbe.java:27-29`; docs/CONSUMING.md §5
  Status         : AS-BUILT

US-EVENTS-003
  Title          : معرفة المستأجر والفاعل من الحدث نفسه / Know the tenant and the actor from the event itself
  Story          : As a listener running on a pooled thread, I need every event to tell me its tenant, actor, realm, time and a unique id, so that I can run my work as that tenant, attribute it, and ignore a duplicate.
  Priority       : HIGH
  Success metric : the worker sees the publisher's tenant and principal (`DomainEventBusIntegrationTest.eventPublishedOutsideATransaction_isDeliveredImmediately_withTheTenantAndPrincipalOfThePublisher`; `NotificationAsyncDeliveryIntegrationTest.theWorkerRunsAsTheDispatchingTenant`); no leak across pooled threads (`TenantAndSecurityContextTaskDecoratorTest`)
  Traces         : POL-EVENTS-003, POL-EVENTS-005
  Source         : events/DomainEvent.java:12-33, :57-70; events/support/TenantAndSecurityContextTaskDecorator.java:22-41
  Status         : AS-BUILT

US-EVENTS-004
  Title          : تشغيل المستمع البطيء بلا تعطيل الناشر / Run a slow listener without blocking the publisher
  Story          : As a listener that does slow work (sending e-mail), I need a named executor that already carries the tenant and the security context, so that I run off the request thread safely with one annotation.
  Priority       : HIGH
  Success metric : `@Async(ErpCoreEvents.EXECUTOR)` listeners run on `erp-event-*` threads with the tenant set; Boot's `applicationTaskExecutor` still serves unqualified `@Async` (`ErpCoreEventsAndNotifAutoConfigurationTest`)
  Traces         : POL-EVENTS-005, POL-EVENTS-006
  Source         : autoconfigure/ErpCoreEventsAutoConfiguration.java:44-55; events/ErpCoreEvents.java:10-16
  Status         : AS-BUILT

US-EVENTS-005
  Title          : ضبط حجم المنفّذ / Size the event executor
  Story          : As a platform operator, I need to size the thread pool and queue of the event executor by properties, so that the asynchronous load of my deployment fits its resources.
  Priority       : MEDIUM
  Success metric : `erp.core.events.executor.*` bound with defaults 4 / 16 / 500 / `erp-event-`
  Traces         : POL-EVENTS-006
  Source         : autoconfigure/ErpCoreProperties.java:282-306; autoconfigure/ErpCoreEventsAutoConfiguration.java:46-52
  Status         : AS-BUILT

## THE PUBLISH-AND-LISTEN STORY (US-EVENTS-001 + US-EVENTS-002, as built)
1. A service writes its change and, still inside its `@Transactional` method, calls
   `eventPublisher.publish(new XxxEvent(...))` — sec/service/UserService.java:115; docs/steps/08-report.md
   "Publishers".
2. The event's no-arg constructor captures `TenantContext.current()`, the actor
   (`SecurityContextHelper.currentActorOrSystem()`) and the realm (`currentRealm()`), assigns a random
   UUID and `Instant.now()` — events/DomainEvent.java:57-70.
3. `SpringDomainEventPublisher` hands it to `ApplicationEventPublisher.publishEvent` —
   events/support/SpringDomainEventPublisher.java:24-29.
4. Spring holds the event until the transaction commits; on rollback it is dropped. A listener declared
   `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)` is invoked after the
   commit, or at once when there was no transaction — events/DomainEventPublisher.java:11-13.
5. An asynchronous listener (`@Async(ErpCoreEvents.EXECUTOR)`, or NOTIF's explicit `executor.execute`)
   runs on `erpCoreEventExecutor`; the decorator applies the captured tenant and security context on the
   worker and restores the worker afterwards; the listener does its tenant work inside
   `TenantContext.runAs(event.getTenantId(), ...)` — events/support/TenantAndSecurityContextTaskDecorator.java:22-41;
   notif/service/NotificationDeliveryListener.java:46-71.

## TRACEABILITY — story → policy
| US | Traces (POL) | Source |
|---|---|---|
| US-EVENTS-001 | POL-EVENTS-001, POL-EVENTS-002, POL-EVENTS-004 | events/DomainEventPublisher.java:21-25 |
| US-EVENTS-002 | POL-EVENTS-001, POL-EVENTS-003 | events/DomainEventPublisher.java:8-19 |
| US-EVENTS-003 | POL-EVENTS-003, POL-EVENTS-005 | events/DomainEvent.java:57-70 |
| US-EVENTS-004 | POL-EVENTS-005, POL-EVENTS-006 | autoconfigure/ErpCoreEventsAutoConfiguration.java:44-55 |
| US-EVENTS-005 | POL-EVENTS-006 | autoconfigure/ErpCoreProperties.java:282-306 |
Every policy POL-EVENTS-001 … POL-EVENTS-006 appears in at least one row above (001 → US-001/002;
002 → US-001; 003 → US-002/003; 004 → US-001; 005 → US-003/004; 006 → US-004/005).

## RESOLVED DECISIONS (dialogue)
| # | Question | Recommended | Confirmed by user | Sources |
|---|---|---|---|---|
| 1 | Synchronous bus in CU or an asynchronous after-commit module? | dedicated module, after commit, at-most-once, no broker | fixed design of erp-core plan step 08, as built | ADR-EVENTS-001 |
| 2 | Which executor and how is the context carried? | named executor, `defaultCandidate = false`, decorator copies tenant + security context | step 08, as built | ADR-EVENTS-002 |
| 3 | Payload contract and placement? | plain values, no secrets, tenant captured at publish, root package | step 08, as built | ADR-EVENTS-003 |
No other question: the stories describe built behaviour, read from the code.

## DEFERRED
| US | Reason | Activation trigger |
|---|---|---|
| (durable / exactly-once delivery, replay, broker) | not built (business-policies-events.md SCOPE EXCEPTIONS) | explicit future request |
| (tenant lifecycle and password-changed events) | not built in 1.2.0 | tenant-maturity plan C.1, D.3 — documented by the implementing run as each package lands; reference rows in `docs/plans/tenant-maturity-analysis-reference.md` |

## APPROVAL
Approved by : n/a — as-built baseline (the stories describe implemented behaviour, erp-core 1.2.0)   Date : 2026-10-07
Later changes are appended as "Implementation Addendum — erp-core 1.3.0" sections, never by rewriting
the stories above.
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
