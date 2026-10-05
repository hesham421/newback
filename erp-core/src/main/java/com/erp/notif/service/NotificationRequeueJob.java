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
 *
 * <p>erp-core 1.1.1 — what is never re-dispatched: a row in flight (its attempt claimed it with a
 * lease in {@code NEXT_ATTEMPT_AT}, see {@code NotificationDeliveryProcessor.prepare}), a row between
 * two retries ({@code NEXT_ATTEMPT_AT} = when the retry is due), and a row waiting in, or running on,
 * this node's event executor ({@link NotificationDeliveryTracker}). A row the executor rejected is
 * untracked and unclaimed, so it is delivered once it is stale.
 */
@Slf4j
public class NotificationRequeueJob {

    private static final String TENANTS_WITH_QUEUED_ROWS =
        "SELECT DISTINCT TENANT_ID FROM NOTIF_LOG WHERE NOTIFICATION_STATUS_ID = ? ORDER BY TENANT_ID";

    private final NotificationLogRepository logRepository;
    private final DomainEventPublisher eventPublisher;
    private final JdbcTemplate jdbcTemplate;
    private final ErpCoreProperties properties;
    private final NotificationDeliveryTracker tracker;

    /** Without a tracker: no row is known to be pending on this node (kept for existing callers). */
    public NotificationRequeueJob(NotificationLogRepository logRepository, DomainEventPublisher eventPublisher,
                                  JdbcTemplate jdbcTemplate, ErpCoreProperties properties) {
        this(logRepository, eventPublisher, jdbcTemplate, properties, new NotificationDeliveryTracker());
    }

    /**
     * @param tracker the rows waiting in, or running on, this node's event executor: never requeued
     *                (erp-core 1.1.1)
     */
    public NotificationRequeueJob(NotificationLogRepository logRepository, DomainEventPublisher eventPublisher,
                                  JdbcTemplate jdbcTemplate, ErpCoreProperties properties,
                                  NotificationDeliveryTracker tracker) {
        this.logRepository = logRepository;
        this.eventPublisher = eventPublisher;
        this.jdbcTemplate = jdbcTemplate;
        this.properties = properties;
        this.tracker = tracker;
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
        int requeued = 0;
        for (NotificationLog row : stale) {
            if (tracker.isPending(row.getId())) {
                log.debug("Notification {} is still queued or in flight on this node — not requeued", row.getId());
                continue;
            }
            eventPublisher.publish(new NotificationRequestedEvent(row.getId(), row.getChannelTypeId(),
                row.getRecipientId(), row.getTemplateFk().getTemplateCode()));
            requeued++;
        }
        return requeued;
    }
}
