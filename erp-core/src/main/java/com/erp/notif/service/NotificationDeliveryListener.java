package com.erp.notif.service;

import com.erp.events.ErpCoreEvents;
import com.erp.events.NotificationRequestedEvent;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Starts the asynchronous delivery of a queued notification (erp-core step 08): reacts to
 * {@link NotificationRequestedEvent} after the dispatching transaction commits (never on rollback —
 * the row would not exist), or at once when the event is published outside a transaction (the requeue
 * job). The delivery itself runs on the core event executor ({@value ErpCoreEvents#EXECUTOR}), so
 * dispatch returns without waiting for any send.
 *
 * <p>erp-core 1.1.1: the listener submits the delivery to the executor itself (instead of
 * {@code @Async}) so that
 * <ul>
 *   <li>the row is recorded in {@link NotificationDeliveryTracker} while it waits in the executor's
 *       queue or runs, and the requeue job leaves it alone;</li>
 *   <li>an executor that rejects the task (queue full) is not an error of the dispatching call: the
 *       row simply stays {@code QUEUED}, untouched, and the requeue job delivers it once it is stale.
 *       Before, the rejection escaped from the after-commit callback into the dispatcher.</li>
 * </ul>
 */
@Component
@Slf4j
public class NotificationDeliveryListener {

    private final NotificationDeliveryWorker worker;
    private final Executor executor;
    private final NotificationDeliveryTracker tracker;

    public NotificationDeliveryListener(NotificationDeliveryWorker worker,
                                        @Qualifier(ErpCoreEvents.EXECUTOR) Executor executor,
                                        NotificationDeliveryTracker tracker) {
        this.worker = worker;
        this.executor = executor;
        this.tracker = tracker;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotificationRequested(NotificationRequestedEvent event) {
        Long tenantId = event.getTenantId();
        Long logId = event.getNotificationLogId();
        if (tenantId == null) {
            log.error("{} carries no tenant — notification {} cannot be delivered", event, logId);
            return;
        }
        if (!tracker.track(logId)) {
            log.debug("Notification {} is already queued or being delivered on this node — duplicate ignored", logId);
            return;
        }
        try {
            executor.execute(() -> {
                try {
                    worker.deliver(tenantId, logId);
                } finally {
                    tracker.release(logId);
                }
            });
        } catch (RejectedExecutionException e) {
            tracker.release(logId);
            log.warn("The event executor rejected the delivery of notification {} (tenant {}): it stays QUEUED"
                + " and the requeue job delivers it once it is stale ({})", logId, tenantId, e.getMessage());
        }
    }
}
