package com.erp.events;

import lombok.Getter;

/**
 * A customer verified its e-mail address. Defined by the event bus (erp-core step 08); published by
 * the customer-realm service of erp-core step 06. {@code userId} is the customer's {@code SEC_USER} id.
 */
@Getter
public final class CustomerVerifiedEvent extends DomainEvent {

    private final Long userId;

    public CustomerVerifiedEvent(Long userId) {
        this.userId = userId;
    }

    /** For a publisher whose thread carries no customer context (e.g. a public endpoint). */
    public CustomerVerifiedEvent(Long tenantId, String actor, Long userId) {
        super(tenantId, actor, REALM_CUSTOMER);
        this.userId = userId;
    }
}
