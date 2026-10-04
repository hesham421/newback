package com.erp.events;

import lombok.Getter;

/**
 * A customer (storefront account) registered itself and awaits e-mail verification. Defined by the
 * event bus (erp-core step 08); published by the customer-realm service of erp-core step 06.
 * {@code userId} is the customer's {@code SEC_USER} id.
 */
@Getter
public final class CustomerRegisteredEvent extends DomainEvent {

    private final Long userId;
    private final String email;

    public CustomerRegisteredEvent(Long userId, String email) {
        this.userId = userId;
        this.email = email;
    }

    /** For a publisher whose thread carries no customer context (e.g. a public endpoint). */
    public CustomerRegisteredEvent(Long tenantId, String actor, Long userId, String email) {
        super(tenantId, actor, REALM_CUSTOMER);
        this.userId = userId;
        this.email = email;
    }
}
