package com.erp.events;

import lombok.Getter;

/** A user account was deactivated or reactivated (SEC). {@code statusCode} is the new USER_STATUS. */
@Getter
public final class UserStatusChangedEvent extends DomainEvent {

    private final Long userId;
    private final String username;
    private final String statusCode;
    private final boolean active;

    public UserStatusChangedEvent(Long userId, String username, String statusCode, boolean active) {
        this.userId = userId;
        this.username = username;
        this.statusCode = statusCode;
        this.active = active;
    }
}
