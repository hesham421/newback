package com.erp.events;

import lombok.Getter;

/**
 * tenant-maturity C12 (REQ-TENANT-033) — a suspended tenant was activated again (tenant module). {@link #getTenantId()}
 * is the ACTIVATED tenant, {@link #getActor()} the PLATFORM operator. Published only on a real SUSPENDED → ACTIVE
 * transition, delivered after commit (NOTIF re-dispatches the notifications it held, RULE-NOTIF-024).
 */
@Getter
public final class TenantActivatedEvent extends DomainEvent {

    private final String tenantCode;

    public TenantActivatedEvent(Long activatedTenantId, String tenantCode, String actor) {
        super(activatedTenantId, actor, REALM_STAFF);
        this.tenantCode = tenantCode;
    }
}
