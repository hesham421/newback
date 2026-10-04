package com.erp.audit.service;

import com.erp.autoconfigure.ErpCoreProperties;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Audit retention (erp-core step 10): deletes {@code CORE_AUDIT_EVENT} rows older than
 * {@code erp.core.audit.retention-days} (default {@code 0} = keep forever, the job does nothing).
 *
 * <p>Core never schedules anything. The bean is always present; an application either calls
 * {@link #run()} from its own scheduler, or enables scheduling ({@code @EnableScheduling}) and sets
 * {@code erp.core.audit.retention-cron} (default {@code -}, disabled). Runs outside any tenant and
 * request, across all tenants (the store deletes tenant by tenant). No {@code @PreAuthorize}: system
 * job, never reachable over HTTP.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditRetentionJob {

    private final AuditEventStore store;
    private final ErpCoreProperties properties;

    /** The scheduled trigger (fires only with {@code @EnableScheduling} and a retention cron). */
    @Scheduled(cron = "${erp.core.audit.retention-cron:-}")
    public void scheduledRun() {
        run();
    }

    /** Applies the configured retention; returns the number of rows deleted ({@code 0} when disabled). */
    public int run() {
        int retentionDays = properties.getAudit().getRetentionDays();
        if (retentionDays <= 0) {
            log.debug("Audit retention disabled (erp.core.audit.retention-days = {})", retentionDays);
            return 0;
        }
        Instant cutoff = Instant.now().minus(Duration.ofDays(retentionDays));
        int deleted = store.deleteOccurredBefore(cutoff);
        log.info("Audit retention: deleted {} rows older than {} days", deleted, retentionDays);
        return deleted;
    }
}
