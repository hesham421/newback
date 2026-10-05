package com.erp.notif.service;

import com.erp.autoconfigure.ErpCoreProperties;
import com.erp.events.DomainEventPublisher;
import com.erp.events.NotificationDispatchedEvent;
import com.erp.events.NotificationFailedEvent;
import com.erp.notif.channel.DeliveryResult;
import com.erp.notif.channel.OutboundMessage;
import com.erp.notif.domain.NotificationLogDomain;
import com.erp.notif.entity.NotificationLog;
import com.erp.notif.entity.NotificationTemplate;
import com.erp.notif.repository.NotificationLogRepository;
import com.erp.tenant.TenantContext;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The transactional halves of one delivery attempt of a queued {@code NOTIF_LOG} row (erp-core step
 * 08). {@link NotificationDeliveryWorker} calls them inside {@code TenantContext.callAs(row's tenant)}
 * — around each transaction, so every Hibernate session opens under that tenant — and calls the
 * channel provider <em>between</em> them, outside any transaction, so no database connection is held
 * while a provider talks to SMTP or another remote system:
 * <ol>
 *   <li>{@link #prepare} — the row must still be {@code QUEUED} and unclaimed; increments {@code ATTEMPTS}
 *       (committed, so an attempt cut short by a crash still counts), claims the row with a lease in
 *       {@code NEXT_ATTEMPT_AT} (erp-core 1.2.0) and builds the {@link OutboundMessage};</li>
 *   <li>the provider sends;</li>
 *   <li>{@link #recordOutcome} — {@code SENT} (+ {@link NotificationDispatchedEvent}),
 *       {@code SKIPPED_NO_PROVIDER} (final, never retried), a failed attempt with attempts left: the row
 *       stays {@code QUEUED} with {@code LAST_ERROR} and {@code NEXT_ATTEMPT_AT} = when the retry is due
 *       and the worker retries; or the last allowed attempt failed: {@code FAILED} at once;</li>
 *   <li>{@code FAILED} + {@link NotificationFailedEvent} when the attempts are exhausted
 *       ({@link #markFailed} if the worker's retries run out first), or at once when the provider
 *       rejects the message permanently (erp-core step 14, e.g. an EMAIL with no recipient address).</li>
 * </ol>
 * Every final status clears {@code VARIABLES_JSON}. Delivery is at-least-once: a crash after a send
 * but before its outcome is recorded leaves the row {@code QUEUED} for the requeue job.
 *
 * <p>Infrastructure reached only from the in-process worker (no caller principal on a pooled thread),
 * so it carries no {@code @PreAuthorize}: the same precedent as the tenant provisioning contributors.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationDeliveryProcessor {

    /** What one attempt ended with. */
    public enum Outcome {
        /** Delivered ({@code SENT}). */
        DELIVERED,
        /** No provider for the channel ({@code SKIPPED_NO_PROVIDER}). */
        SKIPPED,
        /** The attempt failed and was recorded; another attempt is due. */
        RETRY,
        /** The provider rejected the message permanently: {@code FAILED} after this attempt (step 14). */
        REJECTED,
        /** The last allowed attempt failed: {@code FAILED} after this attempt (erp-core 1.2.0). */
        EXHAUSTED,
        /** The row is unknown, no longer {@code QUEUED}, or claimed by another attempt: nothing was done. */
        NOT_QUEUED
    }

    /** {@code LAST_ERROR} of a row failed for exhausted attempts when no attempt recorded an error. */
    static final String ATTEMPTS_EXHAUSTED = "delivery attempts exhausted";

    private final NotificationLogRepository logRepository;
    private final DomainEventPublisher eventPublisher;
    private final ErpCoreProperties properties;

    /**
     * Starts an attempt: empty when the row is unknown or no longer {@code QUEUED} (e.g. already
     * delivered after a requeue), or when it is claimed — its {@code NEXT_ATTEMPT_AT} lies in the
     * future because another attempt holds its lease or waits out its retry delay; otherwise counts
     * the attempt, claims the row and returns the message to send.
     *
     * <p>erp-core 1.2.0 — the claim: {@code NEXT_ATTEMPT_AT} becomes now + the lease
     * ({@code erp.core.notif.requeue.stale-after-minutes}) while the provider sends, so the requeue
     * job (which picks rows whose {@code NEXT_ATTEMPT_AT} is older than now − the same period) never
     * re-dispatches a row in flight, and a duplicate delivery of the row skips it. Two concurrent
     * claims are serialized by the row's {@code VERSION}: the loser's commit fails with an
     * optimistic-locking error, which the worker treats as "not queued". A row that already used
     * every attempt (e.g. a requeued row with a crash in between) is failed instead of attempted
     * again, so requeues never push {@code ATTEMPTS} past {@code erp.core.notif.retry.max-attempts}.
     *
     * <p>Ownership: the claim is identified by the row {@code VERSION} the claiming write produced
     * (returned in {@link Claim#version()}, and again by {@link #recordOutcome} for a retry). The worker
     * passes the version of its own last write as {@code ownedVersion}; a claimed row whose
     * {@code VERSION} still equals it was claimed by this very delivery run and is attempted again —
     * e.g. after recording the previous outcome failed (database error), or when the retry fires
     * before {@code NEXT_ATTEMPT_AT} because the clock moved. Any other write to the row (another
     * run's claim) changes {@code VERSION}, so a duplicate never mistakes someone else's claim for
     * its own; this needs no clock comparison.
     *
     * @param ownedVersion the {@code VERSION} written by this delivery run's last claim or retry
     *                     record, or {@code null} when the run holds no claim yet
     */
    @Transactional
    public Optional<Claim> prepare(Long logId, Long ownedVersion) {
        NotificationLog row = logRepository.findWithTemplateById(logId).orElse(null);
        if (row == null || !NotificationLogDomain.from(row).isAwaitingDelivery()) {
            log.debug("Notification {} is not queued (unknown or already final) — attempt skipped", logId);
            return Optional.empty();
        }
        Instant now = Instant.now();
        boolean owned = ownedVersion != null && ownedVersion.equals(row.getVersion());
        if (!owned && row.getNextAttemptAt() != null && row.getNextAttemptAt().isAfter(now)) {
            log.debug("Notification {} is claimed until {} — attempt skipped", logId, row.getNextAttemptAt());
            return Optional.empty();
        }
        if (!NotificationLogDomain.hasAttemptsLeft(row.getAttempts(), maxAttempts())) {
            log.warn("Notification {} already used its {} attempts — failed instead of attempted again", logId,
                row.getAttempts());
            fail(row, row.getLastError() != null ? row.getLastError() : ATTEMPTS_EXHAUSTED);
            return Optional.empty();
        }
        int attempt = row.getAttempts() + 1;
        row.setAttempts(attempt);
        row.setRetryCount((short) (attempt - 1));
        row.setNextAttemptAt(now.plus(lease()));
        NotificationLog claimed = logRepository.saveAndFlush(row);
        return Optional.of(new Claim(toMessage(claimed), claimed.getVersion()));
    }

    /** A claimed row: the message to send and the {@code VERSION} that identifies the claim. */
    public record Claim(OutboundMessage message, Long version) {
    }

    /**
     * What {@link #recordOutcome} recorded; {@code version} is the row {@code VERSION} written for a
     * {@link Outcome#RETRY} (the claim the run's next attempt owns), otherwise {@code null}.
     */
    public record Recorded(Outcome outcome, Long version) {
    }

    /**
     * Records what the provider reported for the attempt {@link #prepare} started.
     *
     * @param round the 1-based attempt number of the worker's current delivery run (Spring Retry's
     *              counter): a failed attempt sets {@code NEXT_ATTEMPT_AT} to exactly when the worker's
     *              next retry is due, so that retry finds the row unclaimed. It can differ from
     *              {@code ATTEMPTS} once a requeue restarted the run.
     */
    @Transactional
    public Recorded recordOutcome(Long logId, DeliveryResult result, int round) {
        NotificationLog row = logRepository.findWithTemplateById(logId).orElse(null);
        if (row == null || !NotificationLogDomain.from(row).isAwaitingDelivery()) {
            return new Recorded(Outcome.NOT_QUEUED, null);
        }
        int attempt = row.getAttempts();
        switch (result.status()) {
            case SENT -> {
                transitionTo(row, NotificationLogDomain.STATUS_SENT);
                row.setSentAt(LocalDateTime.now());
                row.setNextAttemptAt(null);
                row.setVariablesJson(null);
                logRepository.save(row);
                log.info("Notification {} sent via {} (attempt {})", logId, row.getChannelTypeId(), attempt);
                eventPublisher.publish(new NotificationDispatchedEvent(row.getId(), row.getChannelTypeId(),
                    row.getRecipientId(), row.getTemplateFk().getTemplateCode(), attempt));
                return new Recorded(Outcome.DELIVERED, null);
            }
            case SKIPPED_NO_PROVIDER -> {
                transitionTo(row, NotificationLogDomain.STATUS_SKIPPED_NO_PROVIDER);
                row.setErrorMessage(result.detail());
                row.setLastError(result.detail());
                row.setNextAttemptAt(null);
                row.setVariablesJson(null);
                logRepository.save(row);
                log.info("Notification {} skipped: no provider for channel {}", logId, row.getChannelTypeId());
                return new Recorded(Outcome.SKIPPED, null);
            }
            case REJECTED -> {
                log.warn("Notification {} rejected by the {} provider (attempt {}), not retried: {}", logId,
                    row.getChannelTypeId(), attempt, result.detail());
                fail(row, result.detail());
                return new Recorded(Outcome.REJECTED, null);
            }
            default -> {
                if (!NotificationLogDomain.hasAttemptsLeft(attempt, maxAttempts())) {
                    // the last attempt failed: final at once, so the row is never left QUEUED unclaimed
                    log.warn("Notification {} attempt {} via {} failed: {}", logId, attempt, row.getChannelTypeId(),
                        result.detail());
                    fail(row, result.detail());
                    return new Recorded(Outcome.EXHAUSTED, null);
                }
                row.setLastError(result.detail());
                row.setNextAttemptAt(Instant.now().plusMillis(backoffAfter(Math.max(1, round))));
                NotificationLog recorded = logRepository.saveAndFlush(row);
                log.warn("Notification {} attempt {} via {} failed: {}", logId, attempt, row.getChannelTypeId(),
                    result.detail());
                return new Recorded(Outcome.RETRY, recorded.getVersion());
            }
        }
    }

    /**
     * The attempts are exhausted: {@code FAILED} and {@link NotificationFailedEvent} (RULE-NOTIF-002).
     * A row that is no longer {@code QUEUED} is left alone.
     */
    @Transactional
    public void markFailed(Long logId, String error) {
        NotificationLog row = logRepository.findWithTemplateById(logId).orElse(null);
        if (row == null || !NotificationLogDomain.from(row).isAwaitingDelivery()) {
            return;
        }
        String reason = row.getLastError() != null ? row.getLastError() : error;
        fail(row, reason);
    }

    /** The row becomes {@code FAILED} (final) and {@link NotificationFailedEvent} is published. */
    private void fail(NotificationLog row, String reason) {
        transitionTo(row, NotificationLogDomain.STATUS_FAILED);
        row.setErrorMessage(reason);
        row.setLastError(reason);
        row.setNextAttemptAt(null);
        row.setVariablesJson(null);
        logRepository.save(row);
        log.warn("Notification {} FAILED after {} attempts: {}", row.getId(), row.getAttempts(), reason);
        eventPublisher.publish(new NotificationFailedEvent(row.getId(), row.getChannelTypeId(), row.getRecipientId(),
            row.getTemplateFk().getTemplateCode(), row.getAttempts(), reason));
    }

    private int maxAttempts() {
        return properties.getNotif().getRetry().getMaxAttempts();
    }

    /** How long a claimed row is protected from a second attempt: the requeue staleness period (at least 1 min). */
    Duration lease() {
        return Duration.ofMinutes(Math.max(1L, properties.getNotif().getRequeue().getStaleAfterMinutes()));
    }

    /** Delay before the attempt after {@code attemptsMade}: initial × multiplier^(n-1), capped. */
    long backoffAfter(int attemptsMade) {
        ErpCoreProperties.Notif.Retry retry = properties.getNotif().getRetry();
        double delay = retry.getInitialDelayMs() * Math.pow(retry.getMultiplier(), Math.max(0, attemptsMade - 1));
        return (long) Math.min(delay, retry.getMaxDelayMs());
    }

    private OutboundMessage toMessage(NotificationLog row) {
        NotificationTemplate template = row.getTemplateFk();
        return new OutboundMessage(row.getId(), TenantContext.current(), row.getChannelTypeId(), row.getRecipientId(),
            template.getTemplateCode(), template.getNameAr(), template.getNameEn(), template.getSubjectAr(),
            template.getSubjectEn(), template.getBodyAr(), template.getBodyEn(), row.getModuleCode(),
            row.getReferenceType(), row.getReferenceId(), DispatchVariables.read(row.getVariablesJson()));
    }

    /** LOV-NOTIF-002 (A6) — guard the transition via the log Domain, then mutate the status. */
    private static void transitionTo(NotificationLog row, String targetStatus) {
        NotificationLogDomain.from(row).assertCanTransitionTo(targetStatus);
        row.setNotificationStatusId(targetStatus);
    }
}
