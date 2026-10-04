package com.erp.events;

/**
 * Publishes {@link DomainEvent}s on the erp-core event bus (erp-core step 08). The default
 * implementation wraps Spring's {@code ApplicationEventPublisher}; an application may replace the
 * bean.
 *
 * <p><b>Delivery semantics</b> (Spring's own, unchanged):
 * <ul>
 *   <li>Publishing is synchronous and cheap: it hands the event to the listeners registered for it.</li>
 *   <li>A listener declared {@code @TransactionalEventListener(phase = AFTER_COMMIT, fallbackExecution =
 *       true)} receives an event published inside a transaction only after that transaction commits
 *       (never on rollback), and an event published outside any transaction immediately.</li>
 *   <li>Adding {@code @Async(ErpCoreEvents.EXECUTOR)} runs the listener on the core event executor,
 *       which propagates the publisher's tenant and security context to the worker thread and clears
 *       them afterwards.</li>
 * </ul>
 * Publish from the service that made the change, inside its transaction, after the change is
 * written.
 */
public interface DomainEventPublisher {

    /** Publishes {@code event} to every listener of its type (or a supertype, e.g. {@link DomainEvent}). */
    void publish(DomainEvent event);
}
