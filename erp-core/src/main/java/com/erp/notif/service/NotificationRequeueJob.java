package com.erp.notif.service;

import com.erp.autoconfigure.ErpCoreProperties;
import com.erp.events.DomainEventPublisher;
import com.erp.events.NotificationRequestedEvent;
import com.erp.notif.domain.NotificationLogDomain;
import com.erp.notif.entity.NotificationLog;
import com.erp.notif.repository.NotificationLogRepository;
import com.erp.tenant.TenantContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * Crash recovery for the asynchronous delivery (erp-core step 08): re-dispatches {@code QUEUED} rows
 * whose next attempt — or, before any attempt, whose creation — is older than
 * {@code erp.core.notif.requeue.stale-after-minutes} (default 10), e.g. because the JVM stopped while
 * they waited in the executor queue or between retries.
 *
 * <p>Registered by {@code ErpCoreNotifAutoConfiguration} only when
 * {@code erp.core.notif.requeue.enabled=true} (default {@code false}). Core never turns on scheduling:
 * the {@link Scheduled} trigger fires only in an application that declares {@code @EnableScheduling};
 * an application may instead call {@link #requeueStale()} from its own scheduler.
 *
 * <p>Runs without a request, so it walks the tenants itself: one cross-tenant read of the distinct
 * {@code TENANT_ID}s that have {@code QUEUED} rows (plain JDBC — a tenant-scoped repository cannot
 * see other tenants), then, per tenant inside {@code TenantContext.callAs}, the tenant-filtered
 * repository query and one {@link NotificationRequestedEvent} per stale row. The events are published
 * outside a transaction, so the delivery listener receives them at once (on the event executor).
 */
@Slf4j
public class NotificationRequeueJob {

    private static final String TENANTS_WITH_QUEUED_ROWS =
        "SELECT DISTINCT TENANT_ID FROM NOTIF_LOG WHERE NOTIFICATION_STATUS_ID = ? ORDER BY TENANT_ID";

    private final NotificationLogRepository logRepository;
    private final DomainEventPublisher eventPublisher;
    private final JdbcTemplate jdbcTemplate;
    private final ErpCoreProperties properties;

    public NotificationRequeueJob(NotificationLogRepository logRepository, DomainEventPublisher eventPublisher,
                                  JdbcTemplate jdbcTemplate, ErpCoreProperties properties) {
        this.logRepository = logRepository;
        this.eventPublisher = eventPublisher;
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
    }

    /** Re-dispatches every stale {@code QUEUED} row of every tenant; returns how many. */
    @Scheduled(fixedDelayString = "${erp.core.notif.requeue.interval-ms:60000}",
        initialDelayString = "${erp.core.notif.requeue.interval-ms:60000}")
    public int requeueStale() {
        Instant cutoff = Instant.now().minus(properties.getNotif().getRequeue().getStaleAfterMinutes(), ChronoUnit.MINUTES);
        List<Long> tenants = jdbcTemplate.queryForList(TENANTS_WITH_QUEUED_ROWS, Long.class,
            NotificationLogDomain.STATUS_QUEUED);
        int requeued = 0;
        for (Long tenantId : tenants) {
            requeued += TenantContext.callAs(tenantId, () -> requeueTenant(cutoff));
        }
        if (requeued > 0) {
            log.info("Requeued {} stale QUEUED notification(s) older than {}", requeued, cutoff);
        }
        return requeued;
    }

    private int requeueTenant(Instant cutoff) {
        List<NotificationLog> stale = logRepository.findStale(NotificationLogDomain.STATUS_QUEUED, cutoff);
        for (NotificationLog row : stale) {
            eventPublisher.publish(new NotificationRequestedEvent(row.getId(), row.getChannelTypeId(),
                row.getRecipientId(), row.getTemplateFk().getTemplateCode()));
        }
        return stale.size();
    }
}
