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
 *   <li>{@link #prepare} — the row must still be {@code QUEUED}; increments {@code ATTEMPTS} (committed,
 *       so an attempt cut short by a crash still counts) and builds the {@link OutboundMessage};</li>
 *   <li>the provider sends;</li>
 *   <li>{@link #recordOutcome} — {@code SENT} (+ {@link NotificationDispatchedEvent}),
 *       {@code SKIPPED_NO_PROVIDER} (final, never retried), or a failed attempt: the row stays
 *       {@code QUEUED} with {@code LAST_ERROR} and {@code NEXT_ATTEMPT_AT} and the worker retries;</li>
 *   <li>{@link #markFailed} once the retries are exhausted ({@code FAILED} + {@link NotificationFailedEvent}).</li>
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
        /** The row is unknown or no longer {@code QUEUED}: nothing was done. */
        NOT_QUEUED
    }

    private final NotificationLogRepository logRepository;
    private final DomainEventPublisher eventPublisher;
    private final ErpCoreProperties properties;

    /**
     * Starts an attempt: empty when the row is unknown or no longer {@code QUEUED} (e.g. already
     * delivered after a requeue); otherwise counts the attempt and returns the message to send.
     */
    @Transactional
    public Optional<OutboundMessage> prepare(Long logId) {
        NotificationLog row = logRepository.findWithTemplateById(logId).orElse(null);
        if (row == null || !NotificationLogDomain.from(row).isAwaitingDelivery()) {
            log.debug("Notification {} is not queued (unknown or already final) — attempt skipped", logId);
            return Optional.empty();
        }
        int attempt = row.getAttempts() + 1;
        row.setAttempts(attempt);
        row.setRetryCount((short) (attempt - 1));
        logRepository.save(row);
        return Optional.of(toMessage(row));
    }

    /** Records what the provider reported for the attempt {@link #prepare} started. */
    @Transactional
    public Outcome recordOutcome(Long logId, DeliveryResult result) {
        NotificationLog row = logRepository.findWithTemplateById(logId).orElse(null);
        if (row == null || !NotificationLogDomain.from(row).isAwaitingDelivery()) {
            return Outcome.NOT_QUEUED;
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
                return Outcome.DELIVERED;
            }
            case SKIPPED_NO_PROVIDER -> {
                transitionTo(row, NotificationLogDomain.STATUS_SKIPPED_NO_PROVIDER);
                row.setErrorMessage(result.detail());
                row.setLastError(result.detail());
                row.setNextAttemptAt(null);
                row.setVariablesJson(null);
                logRepository.save(row);
                log.info("Notification {} skipped: no provider for channel {}", logId, row.getChannelTypeId());
                return Outcome.SKIPPED;
            }
            default -> {
                row.setLastError(result.detail());
                boolean more = NotificationLogDomain.hasAttemptsLeft(attempt,
                    properties.getNotif().getRetry().getMaxAttempts());
                row.setNextAttemptAt(more ? Instant.now().plusMillis(backoffAfter(attempt)) : null);
                logRepository.save(row);
                log.warn("Notification {} attempt {} via {} failed: {}", logId, attempt, row.getChannelTypeId(),
                    result.detail());
                return Outcome.RETRY;
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
        transitionTo(row, NotificationLogDomain.STATUS_FAILED);
        row.setErrorMessage(reason);
        row.setLastError(reason);
        row.setNextAttemptAt(null);
        row.setVariablesJson(null);
        logRepository.save(row);
        log.warn("Notification {} FAILED after {} attempts: {}", logId, row.getAttempts(), reason);
        eventPublisher.publish(new NotificationFailedEvent(row.getId(), row.getChannelTypeId(), row.getRecipientId(),
            row.getTemplateFk().getTemplateCode(), row.getAttempts(), reason));
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
