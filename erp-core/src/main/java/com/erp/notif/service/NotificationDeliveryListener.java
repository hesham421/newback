package com.erp.notif.service;

import com.erp.events.ErpCoreEvents;
import com.erp.events.NotificationRequestedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Starts the asynchronous delivery of a queued notification (erp-core step 08): reacts to
 * {@link NotificationRequestedEvent} after the dispatching transaction commits (never on rollback —
 * the row would not exist), or at once when the event is published outside a transaction (the requeue
 * job). Runs on the core event executor, so dispatch returns without waiting for any send.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryListener {

    private final NotificationDeliveryWorker worker;

    @Async(ErpCoreEvents.EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onNotificationRequested(NotificationRequestedEvent event) {
        if (event.getTenantId() == null) {
            log.error("{} carries no tenant — notification {} cannot be delivered", event, event.getNotificationLogId());
            return;
        }
        worker.deliver(event.getTenantId(), event.getNotificationLogId());
    }
}
