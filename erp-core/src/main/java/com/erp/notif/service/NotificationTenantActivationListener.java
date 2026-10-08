package com.erp.notif.service;

import com.erp.events.DomainEventPublisher;
import com.erp.events.ErpCoreEvents;
import com.erp.events.NotificationRequestedEvent;
import com.erp.events.TenantActivatedEvent;
import com.erp.notif.domain.NotificationLogDomain;
import com.erp.notif.entity.NotificationLog;
import com.erp.notif.repository.NotificationLogRepository;
import com.erp.tenant.TenantContext;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * RULE-NOTIF-024 (tenant-maturity C12, XM-NOTIF-005) — when a suspended tenant is activated again, re-dispatches its
 * {@code QUEUED} rows that no attempt holds (no lease, no retry due later) and that are not pending on this node: the
 * rows the claim left alone during the suspension. After commit, on the core event executor, submitted here (the
 * {@link NotificationDeliveryListener} pattern) so a rejected task is logged at WARN instead of failing silently.
 */
@Component
@Slf4j
public class NotificationTenantActivationListener {

    private final NotificationLogRepository logRepository;
    private final DomainEventPublisher eventPublisher;
    private final NotificationDeliveryTracker tracker;
    private final Executor executor;

    public NotificationTenantActivationListener(NotificationLogRepository logRepository,
                                                DomainEventPublisher eventPublisher,
                                                NotificationDeliveryTracker tracker,
                                                @Qualifier(ErpCoreEvents.EXECUTOR) Executor executor) {
        this.logRepository = logRepository;
        this.eventPublisher = eventPublisher;
        this.tracker = tracker;
        this.executor = executor;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTenantActivated(TenantActivatedEvent event) {
        Long tenantId = event.getTenantId();
        if (tenantId == null) {
            log.error("{} carries no tenant — no held notification is re-dispatched", event);
            return;
        }
        try {
            executor.execute(() -> redispatch(tenantId));
        } catch (RejectedExecutionException e) {
            log.warn("The event executor rejected the re-dispatch of tenant {}'s held notifications: they stay QUEUED"
                + " for the requeue job or a later activation ({})", tenantId, e.getMessage());
        }
    }

    private void redispatch(Long tenantId) {
        try {
            Instant now = Instant.now();
            int redispatched = TenantContext.callAs(tenantId, () -> redispatchHeld(now));
            log.info("Tenant ID: {} activated — {} held QUEUED notification(s) re-dispatched", tenantId, redispatched);
        } catch (RuntimeException e) {
            log.warn("The held notifications of tenant {} could not be re-dispatched: they stay QUEUED for the requeue job",
                tenantId, e);
        }
    }

    private int redispatchHeld(Instant now) {
        List<NotificationLog> held = logRepository.findStale(NotificationLogDomain.STATUS_QUEUED, now);
        int redispatched = 0;
        for (NotificationLog row : held) {
            if (tracker.isPending(row.getId())) {
                continue;
            }
            eventPublisher.publish(new NotificationRequestedEvent(row.getId(), row.getChannelTypeId(),
                row.getRecipientId(), row.getTemplateFk().getTemplateCode()));
            redispatched++;
        }
        return redispatched;
    }
}
