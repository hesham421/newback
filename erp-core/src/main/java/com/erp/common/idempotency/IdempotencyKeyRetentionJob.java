package com.erp.common.idempotency;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Deletes {@code CORE_IDEMPOTENCY_KEY} rows older than {@code erp.core.idempotency.retention}, across all tenants, tenant
 * by tenant (every statement names {@code TENANT_ID}, RULE-TENANT-011). The {@code AuditRetentionJob} pattern: the bean
 * is always present; its trigger fires only when the application enables scheduling and sets
 * {@code erp.core.idempotency.retention-cron}. A system job, never reachable over HTTP.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class IdempotencyKeyRetentionJob {

    private final JdbcTemplate jdbcTemplate;
    private final IdempotencySettings settings;

    /** The scheduled trigger (fires only with {@code @EnableScheduling} and a retention cron). */
    @Scheduled(cron = "${erp.core.idempotency.retention-cron:-}")
    public void scheduledRun() {
        run();
    }

    /** Applies the retention; returns the number of rows deleted. */
    public int run() {
        OffsetDateTime cutoff = Instant.now().minus(settings.retention()).atOffset(ZoneOffset.UTC);
        List<Long> tenants = jdbcTemplate.queryForList(
            "SELECT DISTINCT TENANT_ID FROM CORE_IDEMPOTENCY_KEY WHERE CREATED_AT < ?", Long.class, cutoff);
        int deleted = 0;
        for (Long tenantId : tenants) {
            deleted += jdbcTemplate.update(
                "DELETE FROM CORE_IDEMPOTENCY_KEY WHERE TENANT_ID = ? AND CREATED_AT < ?", tenantId, cutoff);
        }
        log.info("Idempotency retention: deleted {} rows older than {} in {} tenants", deleted, settings.retention(),
            tenants.size());
        return deleted;
    }
}
