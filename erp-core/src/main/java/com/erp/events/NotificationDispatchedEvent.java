package com.erp.events;

import lombok.Getter;

/** A queued notification was delivered by its channel provider ({@code NOTIF_LOG} status {@code SENT}). */
@Getter
public final class NotificationDispatchedEvent extends DomainEvent {

    private final Long notificationLogId;
    private final String channel;
    private final Long recipientId;
    private final String templateCode;
    private final int attempts;

    public NotificationDispatchedEvent(Long notificationLogId, String channel, Long recipientId,
                                       String templateCode, int attempts) {
        this.notificationLogId = notificationLogId;
        this.channel = channel;
        this.recipientId = recipientId;
        this.templateCode = templateCode;
        this.attempts = attempts;
    }
}
