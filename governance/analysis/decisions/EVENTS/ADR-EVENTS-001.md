# ADR-EVENTS-001 — In-process Spring event bus: after-commit, at-most-once, no broker or outbox

Module  : EVENTS     Version : v1 (as built, erp-core 1.2.0)     Stage raised : P0 (Module registry) — recorded after the fact
Status  : ACCEPTED (as built)

## Context
CU's original analysis placed "Events" inside the Common Utils module as a synchronous in-process
publisher (Spring `ApplicationEvent`), explicitly "no broker, no outbox table", with the recorded
escape hatch "if durable/async cross-module delivery is required later, add an event-outbox entity +
async dispatch" (`../../modules/CU/P0/module-registry-cu.md` AUTO-DECISIONS). erp-core plan step 08
then required NOTIF dispatch to return in under 50 ms with no synchronous send, listeners that never
see a rolled-back change, and an "app-side extension door" that receives core events
(`erp-core-plan/08-STEP-events-and-async-notifications.md`; `docs/steps/08-report.md` Acceptance).
Three designs were available:
- **Synchronous `ApplicationEvent` inside the publisher's transaction** — simplest, but a listener
  runs before the commit (it may act on data that is rolled back) and blocks the publisher.
- **A durable bus** (broker, or a transactional outbox polled by a relay) — exactly-once or
  at-least-once delivery across restarts, at the price of a table, a relay, a new dependency and
  ordering / idempotency concerns in every listener.
- **Spring's transactional event listeners** — `@TransactionalEventListener(AFTER_COMMIT,
  fallbackExecution = true)` delivers after the commit, never on rollback, immediately outside a
  transaction; combined with `@Async` the listener does not block the publisher; nothing is persisted.

The one core consumer with durable needs, NOTIF delivery, already has its own durable state
(`NOTIF_LOG` rows `QUEUED`, a requeue job, a claim lease — `docs/DEVIATIONS.md` [08], [15]).

## Decision
**An in-process Spring event bus, after-commit, at-most-once, no broker and no outbox**, in a dedicated
module `com.erp.events` rather than inside CU (as built):
- `DomainEventPublisher.publish(DomainEvent)` hands the event to `ApplicationEventPublisher`
  (`erp-core/src/main/java/com/erp/events/support/SpringDomainEventPublisher.java:24-29`); the bean is
  `@ConditionalOnMissingBean`, so an application may replace it
  (`autoconfigure/ErpCoreEventsAutoConfiguration.java:38-42`);
- the listener contract is `@TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution =
  true)` (+ `@Async(ErpCoreEvents.EXECUTOR)` for slow work): delivered after the commit only, never on
  rollback, at once when published outside a transaction (`events/DomainEventPublisher.java:8-19`;
  `notif/service/NotificationDeliveryListener.java:46`);
- publishers publish from the service that made the change, inside its transaction, after the write
  (SEC, TENANT, FILE, NOTIF — `docs/steps/08-report.md` "Publishers");
- nothing is persisted and nothing is replayed: delivery is at-most-once if the process dies after the
  commit; the executor does not wait for running tasks on shutdown
  (`autoconfigure/ErpCoreEventsAutoConfiguration.java:29-31`); durable work recovers from its own table
  (NOTIF's requeue job, `docs/CONSUMING.md` §7), not from the bus;
- the module is `com.erp.events`, appended to `CORE_PACKAGE_LIST`; CU's analysis records the move as a
  CHANGED delta (`../../modules/CU/P0/business-policies-cu.md` addendum row 6;
  `../../modules/CU/P1/srs-cu.md` addendum "Events are no longer a synchronous CU bus").

Reasons, from the step file, its report and the deviations:
1. **After-commit is the correctness requirement.** A NOTIF listener must find the `NOTIF_LOG` row it
   was told about; a listener that ran before the commit could not (`NotificationDeliveryListener`
   Javadoc: "never on rollback — the row would not exist").
2. **Medium complexity.** No broker, no relay, no new table; Spring's own semantics unchanged
   ("Delivery semantics (Spring's own, unchanged)", `DomainEventPublisher.java:8`).
3. **Durability where it is needed.** The only core consumer that must not lose work keeps its own
   state and a requeue job; a generic outbox would duplicate that.
4. **An extension door that costs an application one annotation**
   (`erp-app-reference/src/test/java/com/erp/app/UserCreatedEventProbe.java:27-29`; `docs/CONSUMING.md` §5).

## Consequences
- An event is lost when the JVM dies between the commit and the listener's work, and when the
  executor's queue is full (NOTIF catches the rejection and leaves the row `QUEUED` for the requeue
  job, `docs/DEVIATIONS.md` [15]); listeners that need guarantees own their recovery.
- There is no ordering guarantee across events and no replay; `DomainEvent.getId()` is the listener's
  idempotency key when it retries on its own.
- A synchronous `@EventListener` still works (same transaction, before commit) — it is the listener's
  choice, documented in `docs/steps/08-report.md` "Notes for later steps".
- A listener on `DomainEvent` itself receives every event; the audit log chose not to use it
  (`docs/DEVIATIONS.md` [10] "events for async write").
- Revisiting this choice (an outbox, a broker) is additive — a new module or table — and is not
  planned; the `ScopedValue` spike of the tenant-maturity plan (C.6) touches the executor's context
  propagation (ADR-EVENTS-002), not this decision.

## Traces
ENT-EVENTS-001 · REQ-EVENTS-001, REQ-EVENTS-002, REQ-EVENTS-003, REQ-EVENTS-011 · RULE-EVENTS-001,
RULE-EVENTS-002, RULE-EVENTS-003 · POL-EVENTS-001, POL-EVENTS-002 · XM-EVENTS-001 ·
docs/DEVIATIONS.md [08] (executor wiring, where the core events live), [10], [15]; docs/steps/08-report.md;
../../modules/CU/P0/module-registry-cu.md AUTO-DECISIONS (superseded default)
