# SRS — أحداث المجال / Events (EVENTS)
══════════════════════════════════════════════════════════════════
Module : EVENTS   Version : v1 (as-built baseline)   Profile : erp
Inputs : prd-events, module-registry-events, business-policies-events; the code at main @ 19b19a4 (erp-core 1.2.0 behaviour)
Counts : ENT 1 · REQ 13 · AC 13 · RULE 9 · SCR-REQ 0 · XM 2 · ADR 3
══════════════════════════════════════════════════════════════════

Written from the code, not before it. Every REQ / AC / RULE names the code location it was read from;
the id → location index is `registry-srs-events.md`. Java paths are relative to
`erp-core/src/main/java/com/erp/`, `file:line` at main @ 19b19a4. There is no HTTP surface: no
`TC-CORE-*` case, no api-docs folder; verification is by the JUnit classes under
`erp-core/src/test/java/com/erp/events/` and the NOTIF / reference-app tests named below.

# PART A — MODULE FOUNDATION

## A1 — Document information
| Item | Value |
|---|---|
| Module | EVENTS — أحداث المجال / Events (package `com.erp.events`; no registry module, no screen, no endpoint) |
| Feature code | EVENTS |
| Version | v1 (as built in erp-core 1.2.0) |
| Date | 2026-10-07 |
| Status | AS-BUILT baseline |
| Prepared by | analysis lane, from the code |
| Decisions applied count | 3 (ADR-EVENTS-001, -002, -003) |

## A2 — Functional context

**In scope:** غلاف الحدث `DomainEvent` وما يحمله (معرّف، وقت، مستأجر، فاعل، نطاق)، واجهة النشر
`DomainEventPublisher` وتنفيذها الافتراضي، عقد الاستماع بعد الالتزام، المنفّذ `erpCoreEventExecutor`
ومزخرفه الذي ينقل سياق المستأجر والأمان، كتالوج الأحداث العشرة ومن ينشرها، المستهلك الأساسي الوحيد
(تسليم الإشعارات)، وخصائص الضبط `erp.core.events.executor.*`.

**Out of scope:** وسيط رسائل، صندوق صادر، تسليم دائم أو مرة واحدة بالضبط، إعادة التشغيل، مستمع تدقيق على
`DomainEvent`، أحداث دورة حياة المستأجر وتغيير كلمة المرور (1.3.0) — [business-policies-events.md →
SCOPE EXCEPTIONS; ADR-EVENTS-001].

**Module function (one paragraph):** وحدة EVENTS تفصل بين من يُحدث التغيير ومن يتفاعل معه: الوحدة
تنشر حقيقة داخل معاملتها، ولا يستلمها المستمعون إلا بعد الالتزام، ويحمل الحدث مستأجره وفاعله فيعمل
المستمع غير المتزامن على منفّذ مشترك ينقل هذا السياق ويعيده، دون وسيط ولا جدول.

**Detailed description (workflow narrative, roles):** مطوّر الوحدة ينشر عبر `DomainEventPublisher`
داخل الخدمة بعد الكتابة؛ مطوّر التطبيق أو الوحدة المستمعة يعلّق `@TransactionalEventListener(AFTER_COMMIT,
fallbackExecution = true)` وربما `@Async(ErpCoreEvents.EXECUTOR)` ويعمل داخل `TenantContext.runAs(event.getTenantId())`؛
مشغّل المنصة يضبط حجم المنفّذ.

**Current situation:** built (erp-core plan step 08; NOTIF's listener hardened in 1.2.0, DEVIATIONS [15]).

**General notes:** the module has no table, no endpoint, no permission and no error code; it depends on
`TenantContext` (tenant root package) and `SecurityContextHelper` (common) only.

## A3 — Entities and fields

### ENT-EVENTS-001 — غلاف الحدث / Domain event envelope (`DomainEvent`)
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| immutable value object, in memory only (no table, never stored) | SHARED (owner) — subclassed by the 10 core events and by application events (XM-EVENTS-001) | `id` (UUID) is the idempotency key | created by a publisher, delivered to listeners; never updated | reads `TenantContext.current()` (tenant) and `SecurityContextHelper` (common) at construction | events/DomainEvent.java:34-96 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| id | UUID | yes (system) | `UUID.randomUUID()` | unique per event — the listener's idempotency key (RULE-EVENTS-008) | معرّف الحدث | Event id |
| occurredAt | instant | yes (system) | `Instant.now()` at construction | — | وقت الحدث | Occurred at |
| tenantId | number | no (`null` only outside any tenant) | `TenantContext.current()` (no-arg ctor) or the explicit value | authoritative tenant for listeners (RULE-EVENTS-006); `TenantCreatedEvent` carries the NEW tenant | المستأجر | Tenant |
| actor | text | yes (defaults) | `SecurityContextHelper.currentActorOrSystem()` or explicit; null/blank → `system` | the username that caused the fact | الفاعل | Actor |
| realm | code | yes (defaults) | `STAFF` \| `CUSTOMER` \| `SYSTEM` (`SecurityContextHelper.currentRealm()` or explicit; null → `SYSTEM`) | A6 | النطاق | Realm |
| type (derived) | text | — | `getClass().getSimpleName()` | stable event type name | نوع الحدث | Event type |
| (payload) | plain values | per subclass | ids, codes, instants, booleans (RULE-EVENTS-007) | see the catalogue below | — | — |
Source: events/DomainEvent.java:51-55 (fields), :57-70 (constructors), :72-96 (accessors); common/util/SecurityContextHelper.java:63-82.

### The core event catalogue (10 subclasses of ENT-EVENTS-001, all `final`, root package)
| Event | Payload | Constructor(s) | Publisher(s) (inside the writing transaction unless noted) | Core consumer | Source |
|---|---|---|---|---|---|
| `UserCreatedEvent` | `userId`, `username` | no-arg capture | SEC `UserService.create` (:115), `SignupRequestService.decide` approve (:147) | — | events/UserCreatedEvent.java:7-16 |
| `UserStatusChangedEvent` | `userId`, `username`, `statusCode`, `active` | no-arg capture | SEC `UserService.deactivate` (:168), `reactivate` (:186) | — | events/UserStatusChangedEvent.java:7-20 |
| `CustomerRegisteredEvent` | `userId`, `email` | no-arg capture; explicit `(tenantId, actor, userId, email)` → realm `CUSTOMER` | SEC `CustomerAccountService.register` (:142, explicit ctor: public endpoint, no principal) | — | events/CustomerRegisteredEvent.java:11-27 |
| `CustomerVerifiedEvent` | `userId` | no-arg capture; explicit `(tenantId, actor, userId)` → realm `CUSTOMER` | SEC `CustomerAccountService.verify` (:166, explicit ctor) | — | events/CustomerVerifiedEvent.java:10-23 |
| `PasswordResetRequestedEvent` | `userId`, `resetTokenId`, `expiresAt` — never the raw token | no-arg capture | SEC `PasswordResetService` (:204) | — | events/PasswordResetRequestedEvent.java:11-22 |
| `TenantCreatedEvent` | `tenantCode` | explicit only `(createdTenantId, tenantCode, actor)` → tenant = the NEW tenant, realm `STAFF` | TENANT `TenantService.create` (:99-100) | — | events/TenantCreatedEvent.java:11-19 |
| `FileDocumentPublishedEvent` | `documentId`, `moduleCode`, `ownerType`, `ownerId`, `fileName`, `contentType`, `visibility` (`VISIBILITY_PRIVATE` / `VISIBILITY_PUBLIC`) | no-arg capture | FILE `FileService.store` (:158, PRIVATE), `FileService.updateVisibility` (:339, PUBLIC) | — | events/FileDocumentPublishedEvent.java:11-37 |
| `NotificationRequestedEvent` | `notificationLogId`, `channel`, `recipientId`, `templateCode` | no-arg capture | NOTIF `DispatchService` (:156); `NotificationRequeueJob` (:97, outside any transaction → delivered at once) | NOTIF `NotificationDeliveryListener.onNotificationRequested` | events/NotificationRequestedEvent.java:11-24; notif/service/NotificationDeliveryListener.java:46-71 |
| `NotificationDispatchedEvent` | `notificationLogId`, `channel`, `recipientId`, `templateCode`, `attempts` | no-arg capture | NOTIF `NotificationDeliveryProcessor` (:164, row `SENT`) | — | events/NotificationDispatchedEvent.java:7-23 |
| `NotificationFailedEvent` | `notificationLogId`, `channel`, `recipientId`, `templateCode`, `attempts`, `lastError` | no-arg capture | NOTIF `NotificationDeliveryProcessor` (:225, row `FAILED`) | — | events/NotificationFailedEvent.java:10-28 |
Publisher paths: sec/service/UserService.java, sec/service/SignupRequestService.java, sec/service/CustomerAccountService.java,
sec/service/PasswordResetService.java, tenant/service/TenantService.java, file/service/FileService.java,
notif/service/DispatchService.java, notif/service/NotificationDeliveryProcessor.java, notif/service/NotificationRequeueJob.java.
Users created by the tenant provisioning contributor (JDBC) publish no `UserCreatedEvent`; `TenantCreatedEvent` covers them (docs/DEVIATIONS.md [08] "Where the core events live").

## A4 — Functional requirements (EARS) and acceptance criteria

### REQ-EVENTS-001 — نشر حدث عبر الناشر / Publish an event through the publisher
Pattern    : event
Statement  : When a module calls `DomainEventPublisher.publish(event)`, the system shall hand the event synchronously to Spring's `ApplicationEventPublisher`, so that every `@EventListener` / `@TransactionalEventListener` registered for its type (or a supertype, `DomainEvent` included) receives it.
Traces     : US-EVENTS-001
Entities   : ENT-EVENTS-001
Rationale  : POL-EVENTS-002
Source     : events/DomainEventPublisher.java:21-25; events/support/SpringDomainEventPublisher.java:24-29
Priority   : HIGH
#### AC-EVENTS-001 — [REQ-EVENTS-001]
Given a listener on `DomainEvent` (the test `DomainEventProbe`)
When any core event is published
Then the probe receives it (`erp-core/src/test/java/com/erp/testsupport/DomainEventProbe.java`; `DomainEventBusIntegrationTest`)

### REQ-EVENTS-002 — التسليم بعد الالتزام فقط / Deliver after the commit only
Pattern    : state
Statement  : While an event is published inside a transaction, the system shall deliver it to an `AFTER_COMMIT` transactional listener only after that transaction commits, and shall never deliver it when the transaction rolls back.
Traces     : US-EVENTS-001, US-EVENTS-002
Entities   : ENT-EVENTS-001
Rationale  : POL-EVENTS-001
Source     : events/DomainEventPublisher.java:11-13; notif/service/NotificationDeliveryListener.java:46; docs/steps/08-report.md "Listener pattern"
Priority   : HIGH
#### AC-EVENTS-002 — [REQ-EVENTS-002]
Given a transactional method that publishes an event and then commits, and another that publishes and rolls back
When both run
Then the listener receives the first event after the commit and never the second (`DomainEventBusIntegrationTest.eventPublishedInATransaction_isDeliveredOnlyAfterCommit_andNeverOnRollback`)

### REQ-EVENTS-003 — التسليم الفوري خارج المعاملة / Deliver immediately outside a transaction
Pattern    : event
Statement  : When an event is published with no transaction open, the system shall deliver it at once to listeners declared with `fallbackExecution = true`.
Traces     : US-EVENTS-001
Entities   : ENT-EVENTS-001
Rationale  : POL-EVENTS-001 — the NOTIF requeue job republishes outside a transaction
Source     : events/DomainEventPublisher.java:11-13; notif/service/NotificationRequeueJob.java:97; notif/service/NotificationDeliveryListener.java:14-18
Priority   : HIGH
#### AC-EVENTS-003 — [REQ-EVENTS-003]
Given no transaction and tenant 777 set on the thread
When an event is published
Then the listener runs immediately, with tenant 777 and the publisher's principal on the worker (`DomainEventBusIntegrationTest.eventPublishedOutsideATransaction_isDeliveredImmediately_withTheTenantAndPrincipalOfThePublisher`)

### REQ-EVENTS-004 — التقاط المستأجر والفاعل والنطاق عند النشر / Capture tenant, actor and realm at publish
Pattern    : event
Statement  : When an event is constructed with the no-arg constructor, the system shall capture `TenantContext.current()` as its tenant, the current caller's name (or `system`) as its actor and the caller's realm (`CUSTOMER` for `ROLE_CUSTOMER`, `STAFF` for any other authenticated caller, `SYSTEM` for none), plus a random UUID and the current instant.
Traces     : US-EVENTS-003
Entities   : ENT-EVENTS-001
Rationale  : POL-EVENTS-003
Source     : events/DomainEvent.java:57-70; common/util/SecurityContextHelper.java:54-82
Priority   : HIGH
#### AC-EVENTS-004 — [REQ-EVENTS-004]
Given a staff principal `admin` in tenant 1, a customer principal, and no principal
When an event is constructed in each context
Then it carries (`1`, `admin`, `STAFF`), (tenant, customer name, `CUSTOMER`) and (`null` or the thread's tenant, `system`, `SYSTEM`) respectively, with a non-null `id` and `occurredAt` (`DomainEventTest`)

### REQ-EVENTS-005 — النشر نيابةً عن مستأجر أو نطاق آخر / Publish on behalf of another tenant or realm
Pattern    : optional
Statement  : Where the publishing thread does not represent the fact's tenant or realm, the system shall let the publisher pass `tenantId`, `actor` and `realm` explicitly; a null or blank actor becomes `system` and a null realm `SYSTEM`.
Traces     : US-EVENTS-003
Entities   : ENT-EVENTS-001
Rationale  : `TenantCreatedEvent` belongs to the NEW tenant; customer events are published by public endpoints without a principal
Source     : events/DomainEvent.java:63-70; events/TenantCreatedEvent.java:15-18; events/CustomerRegisteredEvent.java:21-26; events/CustomerVerifiedEvent.java:18-22
Priority   : HIGH
#### AC-EVENTS-005 — [REQ-EVENTS-005]
Given a tenant provisioned by the PLATFORM operator `admin`
When `TenantCreatedEvent` is published
Then `getTenantId()` is the new tenant's id, `getActor()` is `admin` and `getRealm()` is `STAFF`; a customer registration publishes `CustomerRegisteredEvent` with realm `CUSTOMER` (`DomainEventBusIntegrationTest.customerRegistration_publishesCustomerRegisteredEvent`)

### REQ-EVENTS-006 — المستمع غير المتزامن على منفّذ النواة / Asynchronous listener on the core executor
Pattern    : optional
Statement  : Where a listener is annotated `@Async(ErpCoreEvents.EXECUTOR)` (or submits to the executor itself), the system shall run it on `erpCoreEventExecutor`, a `ThreadPoolTaskExecutor` whose threads are named `erp-event-*`, with `@EnableAsync` provided by the core.
Traces     : US-EVENTS-004
Entities   : —
Rationale  : POL-EVENTS-005, POL-EVENTS-006
Source     : autoconfigure/ErpCoreEventsAutoConfiguration.java:34, :44-55; events/ErpCoreEvents.java:16; notif/service/NotificationDeliveryListener.java:38-40, :59
Priority   : HIGH
#### AC-EVENTS-006 — [REQ-EVENTS-006]
Given the application's `UserCreatedEventProbe` (`@Async(ErpCoreEvents.EXECUTOR)` + after-commit listener)
When the bootstrap admin creates a user over HTTP
Then the probe receives the event on an `erp-event-*` thread running as tenant 1 (`ReferenceApplicationSmokeTest.anApplicationListener_receivesTheCoreUserCreatedEvent`)

### REQ-EVENTS-007 — نقل السياق إلى خيط العمل وإعادته / Propagate the context to the worker and restore it
Pattern    : ubiquitous
Statement  : The system shall capture, when a task is submitted to the core executor, the submitting thread's `TenantContext` and a copy of its `SecurityContext`, apply both on the worker thread for the task, and restore the worker's previous tenant and security context afterwards (clearing them when there was none).
Traces     : US-EVENTS-003, US-EVENTS-004
Entities   : —
Rationale  : POL-EVENTS-005
Source     : events/support/TenantAndSecurityContextTaskDecorator.java:22-57; autoconfigure/ErpCoreEventsAutoConfiguration.java:53
Priority   : HIGH
#### AC-EVENTS-007 — [REQ-EVENTS-007]
Given a worker thread that previously ran a task of tenant A
When a task submitted from tenant B runs and ends
Then it sees tenant B and B's principal while running, and the thread holds no tenant and no principal afterwards (`TenantAndSecurityContextTaskDecoratorTest`, 2 tests)

### REQ-EVENTS-008 — مستأجر الحدث هو الحجة / The event's tenant is authoritative
Pattern    : ubiquitous
Statement  : An asynchronous listener shall do its tenant work inside `TenantContext.runAs(event.getTenantId(), …)` (around a transactional call, never inside an open transaction), because the event's own tenant — not the propagated thread context — names the tenant the fact belongs to; a listener that receives an event without a tenant shall not act on it.
Traces     : US-EVENTS-003
Entities   : ENT-EVENTS-001
Rationale  : POL-EVENTS-003
Source     : events/DomainEvent.java:17-20; notif/service/NotificationDeliveryListener.java:48-53, :61; docs/CONSUMING.md §5
Priority   : HIGH
#### AC-EVENTS-008 — [REQ-EVENTS-008]
Given a notification dispatched in a provisioned tenant B
When the delivery worker runs
Then the row and the event carry B and the worker thread is B (`NotificationAsyncDeliveryIntegrationTest.theWorkerRunsAsTheDispatchingTenant`); a `NotificationRequestedEvent` without tenant is logged as an error and not delivered

### REQ-EVENTS-009 — الحدث يحمل قيمًا بسيطة فقط / An event carries plain values only
Pattern    : ubiquitous
Statement  : Every event subclass shall add only plain values (ids, codes, instants, booleans, short texts) to the envelope — never a JPA entity, a password, a raw token or another secret.
Traces     : US-EVENTS-001
Entities   : ENT-EVENTS-001
Rationale  : POL-EVENTS-004
Source     : events/DomainEvent.java:25-26; events/PasswordResetRequestedEvent.java:6-15; events/*.java (fields)
Priority   : HIGH
#### AC-EVENTS-009 — [REQ-EVENTS-009]
Given the 10 core events
When their fields are inspected
Then every field is a `Long`, `String`, `Instant`, `int` or `boolean` (A3 table); `PasswordResetRequestedEvent` carries `resetTokenId` and `expiresAt`, not the token

### REQ-EVENTS-010 — معرّف الحدث مفتاح للتكرار / The event id is the idempotency key
Pattern    : ubiquitous
Statement  : The system shall give every event a random UUID unique per event, so that a listener can recognise and ignore a duplicate delivery.
Traces     : US-EVENTS-003
Entities   : ENT-EVENTS-001
Rationale  : at-most-once in-process delivery; application listeners may still be re-entered by their own retries
Source     : events/DomainEvent.java:15, :65, :72-74
Priority   : MEDIUM
#### AC-EVENTS-010 — [REQ-EVENTS-010]
Given two events constructed in a row
When their ids are compared
Then they differ and neither is null (`DomainEventTest`)

### REQ-EVENTS-011 — الناشر قابل للاستبدال / The publisher is replaceable
Pattern    : optional
Statement  : Where an application defines its own `DomainEventPublisher` bean, the system shall not create the default `SpringDomainEventPublisher`.
Traces     : US-EVENTS-002
Entities   : —
Rationale  : library extension point (docs/RELEASE.md public API)
Source     : autoconfigure/ErpCoreEventsAutoConfiguration.java:38-42; events/DomainEventPublisher.java:4-6
Priority   : LOW
#### AC-EVENTS-011 — [REQ-EVENTS-011]
Given an application context with its own `DomainEventPublisher` bean
When the events auto-configuration runs
Then exactly one `DomainEventPublisher` exists, the application's (`@ConditionalOnMissingBean(DomainEventPublisher.class)`)

### REQ-EVENTS-012 — منفّذ النواة لا يحلّ محلّ منفّذ التطبيق / The core executor never replaces the application's
Pattern    : ubiquitous
Statement  : The system shall declare `erpCoreEventExecutor` with `defaultCandidate = false` and create it only when no bean of that name exists, ordered after Boot's `TaskExecutionAutoConfiguration`, so that Boot's `applicationTaskExecutor` is still created and still serves unqualified `@Async` and `TaskExecutor` injection; the core shall never enable scheduling.
Traces     : US-EVENTS-004
Entities   : —
Rationale  : POL-EVENTS-006; ArchUnit rule 6 (no `@EnableScheduling` in erp-core)
Source     : autoconfigure/ErpCoreEventsAutoConfiguration.java:33, :44-45; `erp-core/src/test/java/com/erp/architecture/CoreLibraryRulesArchTest.java` (rule 6)
Priority   : HIGH
#### AC-EVENTS-012 — [REQ-EVENTS-012]
Given the auto-configured context
When the executors are inspected
Then `applicationTaskExecutor` exists and is the unqualified default, and `erpCoreEventExecutor` is reachable by name only (`ErpCoreEventsAndNotifAutoConfigurationTest`; docs/DEVIATIONS.md [08] "Executor wiring")

### REQ-EVENTS-013 — ضبط المنفّذ / Configure the executor
Pattern    : optional
Statement  : Where an application sets `erp.core.events.executor.core-pool-size`, `.max-pool-size`, `.queue-capacity` or `.thread-name-prefix`, the system shall bind them (defaults 4, 16, 500, `erp-event-`), clamp the maximum pool size to at least the core size, and not wait for running tasks on shutdown.
Traces     : US-EVENTS-005
Entities   : —
Rationale  : POL-EVENTS-006; an interrupted NOTIF delivery is recovered by the requeue job, not by the bus
Source     : autoconfigure/ErpCoreProperties.java:291-304; autoconfigure/ErpCoreEventsAutoConfiguration.java:46-52, :29-31
Priority   : MEDIUM
#### AC-EVENTS-013 — [REQ-EVENTS-013]
Given `max-pool-size = 2` and `core-pool-size = 4`
When the executor is built
Then its maximum pool size is 4 (`Math.max`), its thread name prefix `erp-event-` and its queue capacity 500

## A5 — Business rules

### RULE-EVENTS-001 — النشر متزامن ورخيص / Publishing is synchronous and cheap
Scope      : ENT-EVENTS-001
Trigger    : on publish
Statement  : `publish` shall hand the event to `ApplicationEventPublisher` and return; it shall never wait for a listener's work (asynchronous listeners run on the executor).
Data source: —
Message    : —
Traces     : REQ-EVENTS-001
Source     : events/support/SpringDomainEventPublisher.java:24-29; events/DomainEventPublisher.java:10

### RULE-EVENTS-002 — النشر من الخدمة داخل معاملتها بعد الكتابة / Publish from the service, inside its transaction, after the write
Scope      : every publisher
Trigger    : on a state change
Statement  : The service that made the change shall publish the event inside its own transaction, after the change is written, so that after-commit delivery describes a persisted fact.
Data source: —
Message    : —
Traces     : REQ-EVENTS-002
Source     : events/DomainEventPublisher.java:18-19; sec/service/UserService.java:115; tenant/service/TenantService.java:98-100; docs/steps/08-report.md "Publishers"

### RULE-EVENTS-003 — عقد المستمع / Listener contract
Scope      : every listener
Trigger    : on delivery
Statement  : A listener shall be declared `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)`: delivered after the commit, never on rollback, immediately outside a transaction; in-process only, no persistence — at-most-once if the process dies after the commit. A plain `@EventListener` still works, synchronously.
Data source: —
Message    : —
Traces     : REQ-EVENTS-002, REQ-EVENTS-003
Source     : events/DomainEventPublisher.java:11-16; notif/service/NotificationDeliveryListener.java:46; docs/steps/08-report.md "Notes for later steps"

### RULE-EVENTS-004 — المنفّذ المسمّى / The named executor
Scope      : asynchronous listeners
Trigger    : on start-up; on `@Async(ErpCoreEvents.EXECUTOR)`
Statement  : `erpCoreEventExecutor` shall be a `ThreadPoolTaskExecutor` declared `defaultCandidate = false`, created only when no bean of that name exists, with `@EnableAsync` on the core auto-configuration and no wait for running tasks on shutdown.
Data source: `erp.core.events.executor.*`
Message    : —
Traces     : REQ-EVENTS-006, REQ-EVENTS-012, REQ-EVENTS-013
Source     : autoconfigure/ErpCoreEventsAutoConfiguration.java:33-36, :44-55

### RULE-EVENTS-005 — المزخرف ينقل السياق ويعيده / The decorator propagates and restores the context
Scope      : every task on the core executor
Trigger    : on submit; on completion
Statement  : The decorator shall capture `TenantContext.current()` and a fresh copy of the `SecurityContext` at submission (on the submitting thread), apply them before the task and restore the worker's previous tenant and security context in a `finally` (clearing them when there were none).
Data source: `TenantContext`, `SecurityContextHolder`
Message    : —
Traces     : REQ-EVENTS-007
Source     : events/support/TenantAndSecurityContextTaskDecorator.java:22-57

### RULE-EVENTS-006 — مستأجر الحدث هو الحجة / The event's tenant is authoritative
Scope      : asynchronous listeners
Trigger    : on handling
Statement  : A listener shall run its tenant work inside `TenantContext.runAs(event.getTenantId(), …)` around a transactional call, never relying on the propagated thread context alone.
Data source: `DomainEvent.tenantId`
Message    : —
Traces     : REQ-EVENTS-008
Source     : events/DomainEvent.java:17-20; notif/service/NotificationDeliveryListener.java:48-53, :61; docs/CONSUMING.md §5

### RULE-EVENTS-007 — قيم بسيطة فقط / Plain values only
Scope      : ENT-EVENTS-001 subclasses
Trigger    : on event design
Statement  : An event shall carry plain values only; never a JPA entity, a password, a raw token or a secret.
Data source: —
Message    : —
Traces     : REQ-EVENTS-009
Source     : events/DomainEvent.java:25-26; events/PasswordResetRequestedEvent.java:6-9

### RULE-EVENTS-008 — المنشئ الفارغ يلتقط، والصريح ينوب / The no-arg constructor captures, the explicit one acts on behalf
Scope      : ENT-EVENTS-001
Trigger    : on construction
Statement  : The protected no-arg constructor shall capture tenant, actor and realm from the publishing thread; the explicit `(tenantId, actor, realm)` constructor shall be used only when the fact belongs to another tenant or realm (`TenantCreatedEvent` → the new tenant; customer events → `CUSTOMER`); a blank actor becomes `system`, a null realm `SYSTEM`.
Data source: `TenantContext`, `SecurityContextHelper`
Message    : —
Traces     : REQ-EVENTS-004, REQ-EVENTS-005
Source     : events/DomainEvent.java:28-33, :57-70; events/TenantCreatedEvent.java:15-18; events/CustomerRegisteredEvent.java:21-26

### RULE-EVENTS-009 — `ErpCoreEvents` يحمل اسم المنفّذ فقط / `ErpCoreEvents` holds only the executor name
Scope      : module constants
Trigger    : —
Statement  : `ErpCoreEvents` shall hold exactly one constant, `EXECUTOR = "erpCoreEventExecutor"`, and shall not be instantiated.
Data source: —
Message    : —
Traces     : REQ-EVENTS-006
Source     : events/ErpCoreEvents.java:4-17

## A6 — Lookups

**realm of ENT-EVENTS-001** — owned by common (`SecurityContextHelper`), aliased by `DomainEvent` — control type: fixed String constants (no CHECK, no MDL lookup)
| Code | Label (ar) | Label (en) | When |
|---|---|---|---|
| STAFF | طاقم العمل | Staff | any authenticated caller without `ROLE_CUSTOMER`; `TenantCreatedEvent` always |
| CUSTOMER | عميل | Customer | a caller holding `ROLE_CUSTOMER`; the explicit customer-event constructors |
| SYSTEM | النظام | System | no caller (start-up runner, job, worker); null realm |
Source: events/DomainEvent.java:36-46, :69; common/util/SecurityContextHelper.java:11-21, :74-82.

Consumed lookups: none.

## A7 — Status lifecycle
Not applicable — an event is immutable and has no status. (The delivery states of a notification —
`QUEUED` → `SENT` / `FAILED` / `SKIPPED_NO_PROVIDER` — belong to NOTIF, `../../NOTIF/`.)

## A8 — Module dependencies

| Consumed entity | Owner ENT id | Owner module | HARD-FK / SOFT-READ | XM |
|---|---|---|---|---|
None — EVENTS reads no table.

| Consumed surface | Owner | Kind | Source |
|---|---|---|---|
| `TenantContext.current()`, `.set()`, `.clear()` | tenant (root package, public) | runtime read/write of the thread's tenant | events/DomainEvent.java:59; events/support/TenantAndSecurityContextTaskDecorator.java:23, :43-49 (XM-EVENTS-002) |
| `SecurityContextHelper.currentActorOrSystem()`, `.currentRealm()`, `REALM_*`, `CUSTOMER_AUTHORITY` | common | foundation | events/DomainEvent.java:37-46, :59-60 |
| `ErpCoreProperties.Events.Executor` | autoconfigure | configuration | autoconfigure/ErpCoreEventsAutoConfiguration.java:47-52 |

**Exposed direction** (consumers of the events module)
| XM id | Exposed surface | Kind | Consumers | Source |
|---|---|---|---|---|
| XM-EVENTS-001 | the root package `com.erp.events`: `DomainEvent`, `DomainEventPublisher`, `ErpCoreEvents`, the 10 event classes | public API (publish and listen) | publishers SEC, TENANT, FILE, NOTIF; listener NOTIF `NotificationDeliveryListener`; applications (`UserCreatedEventProbe`) | `erp-core/src/test/java/com/erp/architecture/CrossModuleBoundaryArchTest.java:57-61`; docs/CONSUMING.md §5 |
| XM-EVENTS-002 | (consume direction, recorded here for the index) `TenantContext` of the tenant root package | runtime API | EVENTS → TENANT | events/DomainEvent.java:4; events/support/TenantAndSecurityContextTaskDecorator.java:3 |
| — | `erpCoreEventExecutor` (named bean) | executor | `@Async(ErpCoreEvents.EXECUTOR)` listeners; NOTIF `@Qualifier` injection; applications | autoconfigure/ErpCoreEventsAutoConfiguration.java:44; notif/service/NotificationDeliveryListener.java:38-40 |
| — | `DomainEventPublisher` bean (`@ConditionalOnMissingBean`) | replaceable extension point | applications | autoconfigure/ErpCoreEventsAutoConfiguration.java:38-42 |

# PART B — SCREEN REQUIREMENTS
**No screen, no endpoint.** The module has no HTTP surface, no page code, no permission and no error
code (governance/analysis/platform/project-registry.md "Packages without an HTTP surface"). No
SCR-REQ id is minted.

# STANDALONE

## Error codes
None owned. A listener that needs a tenant and has none fails with the tenant module's
`TENANT_CONTEXT_MISSING` (tenant/TenantContext.java:47-53) when it opens a session without `runAs`;
NOTIF's listener instead logs an error and skips an event without tenant
(notif/service/NotificationDeliveryListener.java:50-53).

## Configuration
| Property | Default | Bound to | Source |
|---|---|---|---|
| `erp.core.events.executor.core-pool-size` | 4 | `ThreadPoolTaskExecutor.corePoolSize` | autoconfigure/ErpCoreProperties.java:294; autoconfigure/ErpCoreEventsAutoConfiguration.java:49 |
| `erp.core.events.executor.max-pool-size` | 16 | `maxPoolSize` = `max(maxPoolSize, corePoolSize)` | autoconfigure/ErpCoreProperties.java:297; autoconfigure/ErpCoreEventsAutoConfiguration.java:50 |
| `erp.core.events.executor.queue-capacity` | 500 | `queueCapacity` | autoconfigure/ErpCoreProperties.java:300; autoconfigure/ErpCoreEventsAutoConfiguration.java:51 |
| `erp.core.events.executor.thread-name-prefix` | `erp-event-` | `threadNamePrefix` | autoconfigure/ErpCoreProperties.java:303; autoconfigure/ErpCoreEventsAutoConfiguration.java:52 |
No validation annotation on these four fields (unlike `erp.core.report.max-export-rows`).

## Traceability matrix
| P0.5 | REQ | AC | RULE | ENT | SCR-REQ |
|---|---|---|---|---|---|
| US-EVENTS-001 | REQ-EVENTS-001, -002, -003, -009 | AC-EVENTS-001, -002, -003, -009 | RULE-EVENTS-001, -002, -003, -007 | ENT-EVENTS-001 | — |
| US-EVENTS-002 | REQ-EVENTS-002, -011 | AC-EVENTS-002, -011 | RULE-EVENTS-003 | ENT-EVENTS-001 | — |
| US-EVENTS-003 | REQ-EVENTS-004, -005, -007, -008, -010 | AC-EVENTS-004, -005, -007, -008, -010 | RULE-EVENTS-005, -006, -008 | ENT-EVENTS-001 | — |
| US-EVENTS-004 | REQ-EVENTS-006, -007, -012 | AC-EVENTS-006, -007, -012 | RULE-EVENTS-004, -005, -009 | — | — |
| US-EVENTS-005 | REQ-EVENTS-013 | AC-EVENTS-013 | RULE-EVENTS-004 | — | — |

Every story traces to ≥ 1 REQ; every REQ has one AC; every RULE traces to a REQ. No orphan, no
dangling id.

## Decisions applied
| DEFAULT / ADR | What | Source | Status |
|---|---|---|---|
| ADR-EVENTS-001 | in-process Spring event bus: after-commit, at-most-once, no broker or outbox | governance/analysis/decisions/EVENTS/ADR-EVENTS-001.md | ACCEPTED (as built) |
| ADR-EVENTS-002 | tenant- and security-propagating executor declared `defaultCandidate = false` | governance/analysis/decisions/EVENTS/ADR-EVENTS-002.md | ACCEPTED (as built) |
| ADR-EVENTS-003 | event payload contract: plain values, no secrets, tenant captured at publish; all events in the root package | governance/analysis/decisions/EVENTS/ADR-EVENTS-003.md | ACCEPTED (as built) |

## Access summary
No page code, no permission (no screen, no endpoint).
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
