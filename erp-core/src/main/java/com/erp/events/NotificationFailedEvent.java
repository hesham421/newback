package com.erp.events;

import lombok.Getter;

/**
 * A queued notification could not be delivered: every retry failed ({@code NOTIF_LOG} status
 * {@code FAILED}). {@code lastError} is the provider's last error message.
 */
@Getter
public final class NotificationFailedEvent extends DomainEvent {

    private final Long notificationLogId;
    private final String channel;
    private final Long recipientId;
    private final String templateCode;
    private final int attempts;
    private final String lastError;

    public NotificationFailedEvent(Long notificationLogId, String channel, Long recipientId, String templateCode,
                                   int attempts, String lastError) {
        this.notificationLogId = notificationLogId;
        this.channel = channel;
        this.recipientId = recipientId;
        this.templateCode = templateCode;
        this.attempts = attempts;
        this.lastError = lastError;
    }
}
