package com.erp.notif.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.erp.events.NotificationRequestedEvent;
import com.erp.tenant.TenantContext;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;
import org.springframework.core.task.TaskRejectedException;

/**
 * erp-core 1.1.1 — the delivery listener tracks a row while its delivery waits in, or runs on, the
 * event executor, ignores a duplicate for a tracked row, and survives an executor that rejects the
 * task (queue full): the row is released (so the requeue job can pick it up later) and the worker
 * never runs.
 */
class NotificationDeliveryListenerTest {

    private static final long TENANT = 7L;

    private final NotificationDeliveryWorker worker = mock(NotificationDeliveryWorker.class);
    private final NotificationDeliveryTracker tracker = new NotificationDeliveryTracker();

    @Test
    void anAcceptedDelivery_isTrackedUntilTheWorkerReturns() {
        List<Runnable> queued = new ArrayList<>();
        NotificationDeliveryListener listener = new NotificationDeliveryListener(worker, queued::add, tracker);

        listener.onNotificationRequested(event(11L));

        assertThat(queued).hasSize(1);
        assertThat(tracker.isPending(11L)).as("tracked while waiting in the executor queue").isTrue();
        verify(worker, never()).deliver(anyLong(), anyLong());

        queued.get(0).run();

        verify(worker).deliver(TENANT, 11L);
        assertThat(tracker.isPending(11L)).as("released once the delivery returned").isFalse();
    }

    @Test
    void aDuplicateEvent_forARowStillPendingOnThisNode_isNotSubmittedTwice() {
        List<Runnable> queued = new ArrayList<>();
        NotificationDeliveryListener listener = new NotificationDeliveryListener(worker, queued::add, tracker);

        listener.onNotificationRequested(event(12L));
        listener.onNotificationRequested(event(12L));

        assertThat(queued).hasSize(1);
        queued.get(0).run();
        verify(worker, times(1)).deliver(TENANT, 12L);
    }

    @Test
    void anExecutorRejection_isSwallowed_andTheRowIsReleasedForTheRequeueJob() {
        Executor full = task -> {
            throw new TaskRejectedException("queue full");
        };
        NotificationDeliveryListener listener = new NotificationDeliveryListener(worker, full, tracker);

        assertThatCode(() -> listener.onNotificationRequested(event(13L))).doesNotThrowAnyException();

        assertThat(tracker.isPending(13L)).isFalse();
        verify(worker, never()).deliver(anyLong(), anyLong());
    }

    @Test
    void aFailingDelivery_isStillReleased() {
        List<Runnable> queued = new ArrayList<>();
        NotificationDeliveryListener listener = new NotificationDeliveryListener(worker, queued::add, tracker);
        doThrow(new IllegalStateException("boom")).when(worker).deliver(TENANT, 14L);

        listener.onNotificationRequested(event(14L));
        try {
            queued.get(0).run();
        } catch (IllegalStateException expected) {
            // the executor's own handling of a failing task is not the listener's concern
        }

        assertThat(tracker.isPending(14L)).isFalse();
    }

    @Test
    void anEventWithoutATenant_isDroppedWithoutSubmitting() {
        List<Runnable> queued = new ArrayList<>();
        NotificationDeliveryListener listener = new NotificationDeliveryListener(worker, queued::add, tracker);

        TenantContext.clear();
        listener.onNotificationRequested(new NotificationRequestedEvent(15L, "EMAIL", 1L, "T"));

        assertThat(queued).isEmpty();
        assertThat(tracker.isPending(15L)).isFalse();
    }

    private static NotificationRequestedEvent event(long logId) {
        return TenantContext.callAs(TENANT, () -> new NotificationRequestedEvent(logId, "EMAIL", 1L, "T"));
    }
}
