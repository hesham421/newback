package com.erp.events;

import lombok.Getter;

/**
 * tenant-maturity C12 (REQ-TENANT-033) — a tenant was suspended (tenant module). {@link #getTenantId()} is the
 * SUSPENDED tenant, {@link #getActor()} the PLATFORM operator who suspended it, {@link #getReason()} the stored reason.
 * Published only on a real ACTIVE → SUSPENDED transition, delivered after commit (SEC ends the tenant's sessions).
 */
@Getter
public final class TenantSuspendedEvent extends DomainEvent {

    private final String tenantCode;
    private final String reason;

    public TenantSuspendedEvent(Long suspendedTenantId, String tenantCode, String reason, String actor) {
        super(suspendedTenantId, actor, REALM_STAFF);
        this.tenantCode = tenantCode;
        this.reason = reason;
    }
}
