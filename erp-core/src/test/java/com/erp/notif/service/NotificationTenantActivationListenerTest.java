package com.erp.notif.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.erp.events.DomainEventPublisher;
import com.erp.events.TenantActivatedEvent;
import com.erp.notif.repository.NotificationLogRepository;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import org.junit.jupiter.api.Test;

/**
 * tenant-maturity C12 review round 1 (RULE-NOTIF-024) — a re-dispatch task the core event executor rejects is logged and
 * leaves the held rows QUEUED; it never fails the activation that published the event.
 */
class NotificationTenantActivationListenerTest {

    @Test
    void aRejectedReDispatchTask_isLogged_andNeverEscapesIntoTheActivation() {
        NotificationLogRepository logRepository = mock(NotificationLogRepository.class);
        DomainEventPublisher publisher = mock(DomainEventPublisher.class);
        Executor rejecting = task -> {
            throw new RejectedExecutionException("queue full");
        };
        NotificationTenantActivationListener listener = new NotificationTenantActivationListener(logRepository, publisher,
            new NotificationDeliveryTracker(), rejecting);

        assertThatCode(() -> listener.onTenantActivated(new TenantActivatedEvent(7L, "ACME", "operator")))
            .doesNotThrowAnyException();
        verifyNoInteractions(logRepository);
        verify(publisher, never()).publish(any());
    }
}
