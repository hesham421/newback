package com.erp.tenant.export;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * RULE-TENANT-028 — the tenants whose export is running on this node (in memory, so per node: two nodes may export one
 * tenant at once, which is harmless — ADR-TENANT-006). The caller releases its slot in a {@code finally}.
 */
@Component
public class TenantExportGuard {

    private final Set<Long> running = ConcurrentHashMap.newKeySet();

    /** Takes the tenant's slot; {@code false} when an export of that tenant already holds it. */
    public boolean tryStart(Long tenantId) {
        return running.add(tenantId);
    }

    /** Releases the tenant's slot (no-op when it is free). */
    public void finish(Long tenantId) {
        running.remove(tenantId);
    }

    /** Whether an export of the tenant is running on this node. */
    public boolean isRunning(Long tenantId) {
        return running.contains(tenantId);
    }
}
