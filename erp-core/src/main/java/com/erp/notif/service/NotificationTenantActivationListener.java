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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * RULE-NOTIF-024 (tenant-maturity C12, XM-NOTIF-005) — when a suspended tenant is activated again, re-dispatches its
 * {@code QUEUED} rows that no attempt holds (no lease, no retry due later) and that are not pending on this node: the
 * rows the claim left alone during the suspension. After commit, on the core event executor, like the requeue job.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationTenantActivationListener {

    private final NotificationLogRepository logRepository;
    private final DomainEventPublisher eventPublisher;
    private final NotificationDeliveryTracker tracker;

    @Async(ErpCoreEvents.EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onTenantActivated(TenantActivatedEvent event) {
        Long tenantId = event.getTenantId();
        if (tenantId == null) {
            log.error("{} carries no tenant — no held notification is re-dispatched", event);
            return;
        }
        Instant now = Instant.now();
        int redispatched = TenantContext.callAs(tenantId, () -> redispatchHeld(now));
        log.info("Tenant ID: {} activated — {} held QUEUED notification(s) re-dispatched", tenantId, redispatched);
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
