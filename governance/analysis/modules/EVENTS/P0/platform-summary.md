# PLATFORM SUMMARY — أحداث المجال / Events (EVENTS)
══════════════════════════════════════════════════════════════════
Profile : erp   Source version : erp-core 1.2.0 (as built)   Analysis : v1 (as-built baseline)
Written : 2026-10-07, from the code at main @ 19b19a4 (1.3.0-SNAPSHOT; the events code is unchanged
          since tag v1.2.0 apart from `DomainEvent` aliasing the realm constants of `SecurityContextHelper`
          moved into `com.erp.common` — docs/CHANGELOG.md [Unreleased] — no behaviour change)
══════════════════════════════════════════════════════════════════

Paths cited below are relative to the repository root, except: Java sources are relative to
`erp-core/src/main/java/com/erp/`. `file:line` points at main @ 19b19a4.

This analysis was NOT produced before the code: the event bus was built by erp-core plan step 08
(`erp-core-plan/08-STEP-events-and-async-notifications.md`, report `docs/steps/08-report.md`) and
hardened in 1.2.0 (NOTIF's listener, `docs/steps/15-report.md`). It records what exists, so that every
later event (the tenant-maturity plan C.1 and D.3) is written as an "Implementation Addendum" on top of
it. The full description of the platform as a whole stays in
[`../../SEC/P0/platform-summary.md`](../../SEC/P0/platform-summary.md) → "Implementation Addendum —
erp-core 1.2.0" (the events block is its lines 117–120); this file describes only the event bus's place
in it.

## OVERVIEW
ناقل أحداث داخل العملية بلا وسيط رسائل ولا جدول صندوق صادر: الوحدة التي تُحدث تغييرًا تنشر "حقيقة"
(`DomainEvent`) عبر `DomainEventPublisher` داخل معاملتها، ولا يستلمها المستمعون إلا بعد الالتزام
(`AFTER_COMMIT`)، وأبدًا عند التراجع. يحمل كل حدث معرّفًا فريدًا ووقتًا ومستأجرًا وفاعلًا ونطاقًا، ولا يحمل
إلا قيمًا بسيطة — لا كيانات ولا أسرارًا. المستمع غير المتزامن يعمل على المنفّذ `erpCoreEventExecutor`
الذي ينقل سياق المستأجر والأمان إلى خيط العمل ويعيده بعده. [`docs/steps/08-report.md` Summary; ADR-EVENTS-001]

An in-process event bus with no broker and no outbox: the module that changes state publishes a fact
(`DomainEvent`) through `DomainEventPublisher` inside its transaction; listeners receive it only after
the commit (`@TransactionalEventListener(AFTER_COMMIT, fallbackExecution = true)`), never on rollback,
and at once when it was published outside a transaction. Every event carries an id (idempotency key),
`occurredAt`, `tenantId`, `actor` and `realm`, and plain values only. An asynchronous listener runs on
`erpCoreEventExecutor`, whose decorator carries the tenant and the security context to the worker thread
and restores the thread afterwards.

## THE EVENT BUS IN THE PLATFORM (as built)
| Aspect | As built | Code location |
|---|---|---|
| Envelope | `DomainEvent` (abstract): `id` (random UUID), `occurredAt`, `tenantId`, `actor` (`system` when none), `realm` (`STAFF` / `CUSTOMER` / `SYSTEM`), `getType()` = simple class name; the no-arg constructor captures tenant, actor and realm from the publishing thread, the explicit `(tenantId, actor, realm)` constructor publishes on behalf of someone else | events/DomainEvent.java:34-96 |
| Publisher | `DomainEventPublisher.publish(DomainEvent)`; default implementation `SpringDomainEventPublisher` → `ApplicationEventPublisher.publishEvent`; `@ConditionalOnMissingBean`, so an application may replace it | events/DomainEventPublisher.java:21-25; events/support/SpringDomainEventPublisher.java:16-30; autoconfigure/ErpCoreEventsAutoConfiguration.java:38-42 |
| Listener contract | `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution = true)` (+ `@Async(ErpCoreEvents.EXECUTOR)` for asynchronous work); a plain `@EventListener` still works synchronously | events/DomainEventPublisher.java:8-19; docs/CONSUMING.md §5 |
| Executor | `erpCoreEventExecutor`: `ThreadPoolTaskExecutor` (core 4, max 16 clamped ≥ core, queue 500, prefix `erp-event-`), `defaultCandidate = false`, `@ConditionalOnMissingBean(name)`, decorated by `TenantAndSecurityContextTaskDecorator`; `@EnableAsync` is on the auto-configuration; ordered after Boot's `TaskExecutionAutoConfiguration`; no wait on shutdown | autoconfigure/ErpCoreEventsAutoConfiguration.java:33-36, :44-55; autoconfigure/ErpCoreProperties.java:282-306 |
| Context propagation | the decorator captures `TenantContext.current()` and a copy of the `SecurityContext` when the task is submitted, applies them on the worker, and restores the worker's previous state (normally nothing) in a `finally` | events/support/TenantAndSecurityContextTaskDecorator.java:19-58 |
| Catalogue | 10 events in the root package `com.erp.events`: `UserCreatedEvent`, `UserStatusChangedEvent`, `CustomerRegisteredEvent`, `CustomerVerifiedEvent`, `PasswordResetRequestedEvent`, `TenantCreatedEvent`, `FileDocumentPublishedEvent`, `NotificationRequestedEvent`, `NotificationDispatchedEvent`, `NotificationFailedEvent` (full table: `../P1/srs-events.md` A3) | events/*.java; docs/CONSUMING.md §5 |
| Publishers | SEC (`UserService`, `SignupRequestService`, `CustomerAccountService`, `PasswordResetService`), TENANT (`TenantService`), FILE (`FileService`), NOTIF (`DispatchService`, `NotificationDeliveryProcessor`, `NotificationRequeueJob`) — every publish inside the writing transaction, after the write | sec/service/UserService.java:115, :168, :186; sec/service/SignupRequestService.java:147; sec/service/CustomerAccountService.java:142, :166; sec/service/PasswordResetService.java:204; tenant/service/TenantService.java:99-100; file/service/FileService.java:158, :339; notif/service/DispatchService.java:156; notif/service/NotificationDeliveryProcessor.java:164, :225; notif/service/NotificationRequeueJob.java:97 |
| Core consumer | one: NOTIF's `NotificationDeliveryListener` on `NotificationRequestedEvent` (after commit; since 1.2.0 it submits the delivery to the executor itself, tracks the row and swallows a rejected execution). No other core listener; the example application listener is `UserCreatedEventProbe` (test scope of the reference app) | notif/service/NotificationDeliveryListener.java:30-72; `erp-app-reference/src/test/java/com/erp/app/UserCreatedEventProbe.java:27-29` |
| Public surface | the root package `com.erp.events` (envelope, publisher interface, `ErpCoreEvents`, the event classes) is public in ArchUnit; `com.erp.events.support` is internal; `com.erp.events.crossmodule` is declared but holds no class | `erp-core/src/test/java/com/erp/architecture/CrossModuleBoundaryArchTest.java:57-61` |
| Persistence | none: no table, no migration, no entity, no outbox; delivery is at-most-once if the process dies after the commit | `docs/steps/08-report.md`; `../P2/db-script-events.md` |
| Configuration | `erp.core.events.executor.core-pool-size` 4, `.max-pool-size` 16, `.queue-capacity` 500, `.thread-name-prefix` `erp-event-` | autoconfigure/ErpCoreProperties.java:291-304 |
| HTTP surface | none (no endpoint, no permission, no error code) | governance/analysis/platform/project-registry.md "Packages without an HTTP surface" |

## REALMS INTERPLAY
| Realm | Event handling | Code location |
|---|---|---|
| STAFF | the no-arg constructor records `realm = STAFF` for any authenticated caller without `ROLE_CUSTOMER`; `TenantCreatedEvent` is always `STAFF` (the platform operator) | events/DomainEvent.java:57-61; common/util/SecurityContextHelper.java:74-82; events/TenantCreatedEvent.java:15-18 |
| CUSTOMER | a caller holding `ROLE_CUSTOMER` records `realm = CUSTOMER`; `CustomerRegisteredEvent` / `CustomerVerifiedEvent` have an explicit constructor that forces `REALM_CUSTOMER` because the publishing thread of a public endpoint carries no customer principal | events/CustomerRegisteredEvent.java:21-26; events/CustomerVerifiedEvent.java:18-22; sec/service/CustomerAccountService.java:142, :166 |
| SYSTEM | no caller (start-up runner, job, async worker) → `actor = system`, `realm = SYSTEM`; the NOTIF requeue job republishes `NotificationRequestedEvent` outside a transaction, delivered at once | events/DomainEvent.java:48-49, :68-69; notif/service/NotificationRequeueJob.java:97 |

## DEPENDENCY MAP
```
SEC, TENANT, FILE, NOTIF ──publish (DomainEventPublisher, XM-EVENTS-001)──▶ EVENTS
NOTIF (NotificationDeliveryListener), applications ──listen (@TransactionalEventListener, XM-EVENTS-001)──▶ EVENTS
EVENTS ──reads (TenantContext.current/set/clear, XM-EVENTS-002)──▶ TENANT (root package)
EVENTS ──reads (SecurityContextHelper.currentActorOrSystem/currentRealm, REALM_* constants)──▶ common
autoconfigure (ErpCoreEventsAutoConfiguration) ──wires (publisher bean, executor, @EnableAsync)──▶ EVENTS
```
Build order: `com.erp.events` follows the seven first core packages in `CORE_PACKAGE_LIST`
(autoconfigure/ErpCoreAutoConfiguration.java:88-93); it was built in step 08 after TENANT (05) and the
realms (06), before SEQUENCE, AUDIT and REPORT. Tier: cross-cutting infrastructure (L1), no table.

## DEFERRED (not in scope of the as-built module)
| Item | Reason / activation trigger |
|---|---|
| Message broker, outbox table, durable or exactly-once delivery, replay | not built: in-process Spring events, at-most-once after commit (ADR-EVENTS-001); CU's module registry already recorded "no broker, no outbox" as the default (`../../CU/P0/module-registry-cu.md` AUTO-DECISIONS) |
| An audit listener on `DomainEvent` | not built: the audit log writes synchronously in the caller's transaction (docs/DEVIATIONS.md [10] "events for async write") |
| Core listeners for the 9 non-NOTIF events | none exist; applications listen (`UserCreatedEventProbe` is the example) |
| Lifecycle events `TenantSuspendedEvent`, `TenantActivatedEvent`; `UserPasswordChangedEvent` | tenant-maturity plan C.1 and D.3 (documented by the implementing run as each package lands; reference rows in `docs/plans/tenant-maturity-analysis-reference.md`) |
| `ScopedValue` instead of the `ThreadLocal` behind `TenantContext` (touches the decorator) | tenant-maturity plan C.6 spike, go/no-go ADR-TENANT-004 (decision owned by the implementing package; draft in `docs/plans/tenant-maturity-analysis-reference.md`) |

## OPEN ITEMS
None — this file describes what exists. Behaviour the code does not have is listed under DEFERRED,
never described as present.

## NEXT STEP
Module registry and policies: `module-registry-events.md`, `business-policies-events.md`.

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 08
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : none recorded here yet — the three events of the tenant-maturity plan (C.1, D.3) are documented by the implementing run as each package lands on main
Statement      : This addendum records only what is already on main for 1.3.0; the plan packages' rows are written by the implementing run.

Plan packages B, C, D, E, G: documented by each package as it lands on main (analysis-first, written by the implementing run); the verified reference rows are in docs/plans/tenant-maturity-analysis-reference.md.
