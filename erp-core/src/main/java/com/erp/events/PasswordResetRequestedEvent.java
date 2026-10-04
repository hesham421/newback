package com.erp.events;

import java.time.Instant;
import lombok.Getter;

/**
 * A password-reset token was issued for a user (SEC). Never carries the raw token — only the token
 * row id and its expiry.
 */
@Getter
public final class PasswordResetRequestedEvent extends DomainEvent {

    private final Long userId;
    private final Long resetTokenId;
    private final Instant expiresAt;

    public PasswordResetRequestedEvent(Long userId, Long resetTokenId, Instant expiresAt) {
        this.userId = userId;
        this.resetTokenId = resetTokenId;
        this.expiresAt = expiresAt;
    }
}
