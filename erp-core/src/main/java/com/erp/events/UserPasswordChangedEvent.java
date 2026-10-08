package com.erp.events;

import lombok.Getter;

/**
 * tenant-maturity D (REQ-SEC-089) — a STAFF password was set by an administrator ({@code byAdmin}) or
 * changed by its owner (SEC). Ids only, never a password; the actor is the administrator or the user.
 */
@Getter
public final class UserPasswordChangedEvent extends DomainEvent {

    private final Long userId;
    private final boolean byAdmin;

    public UserPasswordChangedEvent(Long userId, boolean byAdmin) {
        this.userId = userId;
        this.byAdmin = byAdmin;
    }
}
