package com.erp.notif.service;

import com.erp.notif.channel.DeliveryResult;
import com.erp.notif.channel.OutboundMessage;
import com.erp.tenant.TenantContext;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.RetryContext;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.retry.support.RetrySynchronizationManager;
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
 *
 * <p>erp-core 1.2.0: the last allowed attempt ({@code ATTEMPTS} reaching the maximum) is failed by the
 * processor directly, an attempt that finds the row claimed by another attempt (a duplicate
 * delivery after a requeue) ends quietly, and each failed attempt stores in {@code NEXT_ATTEMPT_AT}
 * the moment this run's next retry is due (from Spring Retry's own counter). The run's own retries
 * recognise their own claim by the row {@code VERSION} the run last wrote, so a retry after a failed
 * outcome record (or a clock step) proceeds instead of skipping its own claim.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryWorker {

    /** Retry-context attribute holding the run's owned claim (a row {@code VERSION}). */
    private static final String CLAIM_ATTRIBUTE = NotificationDeliveryWorker.class.getName() + ".claim";

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
            int round = currentRound();
            Attempt attempt = TenantContext.callAs(tenantId, () -> attempt(logId, round));
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

    private Attempt attempt(Long logId, int round) {
        Optional<NotificationDeliveryProcessor.Claim> prepared;
        try {
            prepared = processor.prepare(logId, ownedClaim());
        } catch (OptimisticLockingFailureException e) {
            // erp-core 1.2.0: a concurrent attempt (a duplicate delivery) claimed the row first
            log.debug("Notification {} was claimed concurrently by another attempt — attempt skipped", logId);
            return new Attempt(NotificationDeliveryProcessor.Outcome.NOT_QUEUED, null);
        }
        if (prepared.isEmpty()) {
            return new Attempt(NotificationDeliveryProcessor.Outcome.NOT_QUEUED, null);
        }
        rememberClaim(prepared.get().version());
        OutboundMessage message = prepared.get().message();
        DeliveryResult result;
        try {
            result = providerRegistry.resolve(message.channel()).send(message);
        } catch (RuntimeException e) {
            result = DeliveryResult.failed(describe(e));
        }
        NotificationDeliveryProcessor.Recorded recorded = processor.recordOutcome(logId, result, round);
        if (recorded.version() != null) {
            rememberClaim(recorded.version());
        }
        return new Attempt(recorded.outcome(), result.detail());
    }

    /**
     * The row {@code VERSION} of this delivery run's last claim or retry record, kept in the Spring
     * Retry context so that it survives from one attempt of the run to the next (erp-core 1.2.0).
     * When recording an outcome fails (a database error), the claim written by {@code prepare} is
     * still the row's current version, so the run's next attempt owns it and retries at once instead
     * of stalling behind its own lease.
     */
    private static Long ownedClaim() {
        RetryContext context = RetrySynchronizationManager.getContext();
        return context != null ? (Long) context.getAttribute(CLAIM_ATTRIBUTE) : null;
    }

    private static void rememberClaim(Long version) {
        RetryContext context = RetrySynchronizationManager.getContext();
        if (context != null) {
            context.setAttribute(CLAIM_ATTRIBUTE, version);
        }
    }

    /** The 1-based number of this attempt within the current {@code @Retryable} run. */
    private static int currentRound() {
        RetryContext context = RetrySynchronizationManager.getContext();
        return context != null ? context.getRetryCount() + 1 : 1;
    }

    private static String describe(RuntimeException e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getName();
    }

    private record Attempt(NotificationDeliveryProcessor.Outcome outcome, String error) {
    }
}
