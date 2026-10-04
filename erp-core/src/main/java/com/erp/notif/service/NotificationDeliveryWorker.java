package com.erp.notif.service;

import com.erp.notif.channel.DeliveryResult;
import com.erp.notif.channel.OutboundMessage;
import com.erp.tenant.TenantContext;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;

/**
 * Delivers one queued notification with retries (erp-core step 08, RULE-NOTIF-002) — Spring Retry
 * replaces the old hand-written {@code RetryPolicy} loop. Runs on the core event executor (called by
 * {@link NotificationDeliveryListener}), never inside a transaction.
 *
 * <p>Each attempt runs inside {@code TenantContext.callAs(tenantId, ...)}: {@code prepare} (its own
 * transaction), the channel provider's {@code send} (no transaction, so no database connection is held
 * while it talks to a remote system), then {@code recordOutcome} (its own transaction). A failed
 * attempt is committed first, then signalled with {@link DeliveryAttemptFailedException}, on which
 * {@code @Retryable} waits with exponential backoff ({@code erp.core.notif.retry.*}: by default 5
 * attempts, 2 s doubling, at most 32 s between two) and tries again. After the last failed attempt
 * {@link #exhausted} marks the row {@code FAILED}. The waiting happens inside Spring Retry.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryWorker {

    private final NotificationDeliveryProcessor processor;
    private final ChannelProviderRegistry providerRegistry;

    @Retryable(
        retryFor = DeliveryAttemptFailedException.class,
        maxAttemptsExpression = "${erp.core.notif.retry.max-attempts:5}",
        backoff = @Backoff(
            delayExpression = "${erp.core.notif.retry.initial-delay-ms:2000}",
            multiplierExpression = "${erp.core.notif.retry.multiplier:2.0}",
            maxDelayExpression = "${erp.core.notif.retry.max-delay-ms:32000}"))
    public void deliver(Long tenantId, Long logId) {
        NotificationDeliveryProcessor.Outcome outcome;
        String error;
        try {
            Attempt attempt = TenantContext.callAs(tenantId, () -> attempt(logId));
            outcome = attempt.outcome();
            error = attempt.error();
        } catch (RuntimeException e) {
            // e.g. a database error while recording: not recorded, but still worth another attempt
            throw new DeliveryAttemptFailedException(describe(e), e);
        }
        if (outcome == NotificationDeliveryProcessor.Outcome.RETRY) {
            throw new DeliveryAttemptFailedException(error);
        }
    }

    /** Spring Retry's recovery once every attempt failed: the row becomes {@code FAILED}. */
    @Recover
    public void exhausted(DeliveryAttemptFailedException e, Long tenantId, Long logId) {
        log.warn("Notification {} (tenant {}) exhausted its delivery attempts", logId, tenantId);
        TenantContext.runAs(tenantId, () -> processor.markFailed(logId, e.getMessage()));
    }

    private Attempt attempt(Long logId) {
        Optional<OutboundMessage> prepared = processor.prepare(logId);
        if (prepared.isEmpty()) {
            return new Attempt(NotificationDeliveryProcessor.Outcome.NOT_QUEUED, null);
        }
        OutboundMessage message = prepared.get();
        DeliveryResult result;
        try {
            result = providerRegistry.resolve(message.channel()).send(message);
        } catch (RuntimeException e) {
            result = DeliveryResult.failed(describe(e));
        }
        return new Attempt(processor.recordOutcome(logId, result), result.detail());
    }

    private static String describe(RuntimeException e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getName();
    }

    private record Attempt(NotificationDeliveryProcessor.Outcome outcome, String error) {
    }
}
