package com.erp.events;

import lombok.Getter;

/**
 * A notification was queued for one channel (NOTIF): its {@code NOTIF_LOG} row is {@code QUEUED} and
 * NOTIF's asynchronous delivery worker picks it up after the dispatching transaction commits (or at
 * once when published outside a transaction, e.g. by the requeue job).
 */
@Getter
public final class NotificationRequestedEvent extends DomainEvent {

    private final Long notificationLogId;
    private final String channel;
    private final Long recipientId;
    private final String templateCode;

    public NotificationRequestedEvent(Long notificationLogId, String channel, Long recipientId, String templateCode) {
        this.notificationLogId = notificationLogId;
        this.channel = channel;
        this.recipientId = recipientId;
        this.templateCode = templateCode;
    }
}
