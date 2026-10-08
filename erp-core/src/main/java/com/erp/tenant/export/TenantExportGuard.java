package com.erp.tenant.export;

import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * RULE-TENANT-028 — the tenants whose export is running on this node: one export per tenant, at most {@code maxConcurrent}
 * together (in memory, so per node — ADR-TENANT-006). The caller releases a taken slot in a {@code finally}.
 */
@Component
public class TenantExportGuard {

    /** The outcome of {@link #tryStart}; only {@link #STARTED} takes a slot. */
    public enum Start { STARTED, ALREADY_RUNNING, BUSY }

    private final Set<Long> running = new HashSet<>();

    /** Takes the tenant's slot unless its export already runs or {@code maxConcurrent} exports run. */
    public synchronized Start tryStart(Long tenantId, int maxConcurrent) {
        if (running.contains(tenantId)) {
            return Start.ALREADY_RUNNING;
        }
        if (running.size() >= maxConcurrent) {
            return Start.BUSY;
        }
        running.add(tenantId);
        return Start.STARTED;
    }

    /** Releases the tenant's slot (no-op when it is free). */
    public synchronized void finish(Long tenantId) {
        running.remove(tenantId);
    }

    /** Whether an export of the tenant is running on this node. */
    public synchronized boolean isRunning(Long tenantId) {
        return running.contains(tenantId);
    }
}
