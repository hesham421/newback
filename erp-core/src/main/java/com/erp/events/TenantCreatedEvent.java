package com.erp.events;

import lombok.Getter;

/**
 * A tenant was created and provisioned (tenant module). {@link #getTenantId()} is the NEW tenant —
 * a listener that seeds application data for it runs {@code TenantContext.runAs(event.getTenantId(), ...)}
 * — while {@link #getActor()} is the PLATFORM operator who created it.
 */
@Getter
public final class TenantCreatedEvent extends DomainEvent {

    private final String tenantCode;

    public TenantCreatedEvent(Long createdTenantId, String tenantCode, String actor) {
        super(createdTenantId, actor, REALM_STAFF);
        this.tenantCode = tenantCode;
    }
}
