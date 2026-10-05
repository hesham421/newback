package com.erp.notif.service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * The {@code NOTIF_LOG} rows this JVM has handed to the core event executor and not finished yet —
 * waiting in the executor's queue, being sent, or between two retries (erp-core 1.2.0 hardening).
 *
 * <p>{@link NotificationDeliveryListener} tracks a row before it submits its delivery and releases it
 * when the delivery returns (or when the executor rejects it); {@link NotificationRequeueJob} never
 * re-dispatches a tracked row. A row waiting in a long executor queue therefore cannot be queued a
 * second time by the requeue job of the same node. Across nodes the lease that
 * {@link NotificationDeliveryProcessor#prepare} writes to {@code NEXT_ATTEMPT_AT} (and the row's
 * optimistic lock) keeps a duplicate from sending twice.
 *
 * <p>Internal infrastructure, not public API. Log ids come from one global sequence, so the id alone
 * identifies a row across tenants.
 */
@Component
public class NotificationDeliveryTracker {

    private final Set<Long> pending = ConcurrentHashMap.newKeySet();

    /** Marks a row as handed to the executor; {@code false} when it already is (a duplicate). */
    public boolean track(Long logId) {
        return pending.add(logId);
    }

    /** The row's delivery returned, or was never accepted by the executor. */
    public void release(Long logId) {
        pending.remove(logId);
    }

    /** Whether the row is waiting in, or running on, this JVM's event executor. */
    public boolean isPending(Long logId) {
        return pending.contains(logId);
    }
}
