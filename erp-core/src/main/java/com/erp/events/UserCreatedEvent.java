package com.erp.events;

import lombok.Getter;

/** A user account was created (SEC: staff user create, sign-up approval). Published after the insert. */
@Getter
public final class UserCreatedEvent extends DomainEvent {

    private final Long userId;
    private final String username;

    public UserCreatedEvent(Long userId, String username) {
        this.userId = userId;
        this.username = username;
    }
}
