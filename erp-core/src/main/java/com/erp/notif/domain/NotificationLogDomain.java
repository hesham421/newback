package com.erp.notif.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.notif.entity.NotificationLog;
import com.erp.notif.exception.NotifErrorCodes;
import java.util.Map;
import java.util.Set;

/**
 * Domain companion for ENTITY-NOTIF-001 (NotificationLog) — lifecycle state-machine guardian for
 * LOV-NOTIF-002 (A6), event-driven since erp-core step 08:
 * <pre>
 *   PENDING ──► QUEUED ──► SENT | FAILED | SKIPPED_NO_PROVIDER
 *      └──────► CHANNEL_DISABLED
 * </pre>
 * {@code PENDING} is the transient state of a row being built by dispatch: an enabled channel's row is
 * persisted {@code QUEUED} (the asynchronous worker delivers it), a disabled or unconfigured channel's
 * row {@code CHANNEL_DISABLED} (RULE-NOTIF-003, never delivered). Every other state is final. No
 * Spring/JPA annotations, no repository access; constructed only via the static factory.
 */
public final class NotificationLogDomain {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_QUEUED = "QUEUED";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CHANNEL_DISABLED = "CHANNEL_DISABLED";
    public static final String STATUS_SKIPPED_NO_PROVIDER = "SKIPPED_NO_PROVIDER";

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
        STATUS_PENDING, Set.of(STATUS_QUEUED, STATUS_CHANNEL_DISABLED),
        STATUS_QUEUED, Set.of(STATUS_SENT, STATUS_FAILED, STATUS_SKIPPED_NO_PROVIDER),
        STATUS_SENT, Set.of(),
        STATUS_FAILED, Set.of(),
        STATUS_CHANNEL_DISABLED, Set.of(),
        STATUS_SKIPPED_NO_PROVIDER, Set.of()
    );

    private final String currentStatus;

    private NotificationLogDomain(String currentStatus) {
        this.currentStatus = currentStatus;
    }

    /** Reconstructs a Domain view over a persisted entity — no validation. */
    public static NotificationLogDomain from(NotificationLog entity) {
        return new NotificationLogDomain(entity.getNotificationStatusId());
    }

    /**
     * LOV-NOTIF-002 (A6) — decision only: throws on an illegal transition from the current status.
     * The service calls this before mutating notificationStatusId.
     */
    public void assertCanTransitionTo(String targetStatus) {
        Set<String> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (targetStatus == null || !allowed.contains(targetStatus)) {
            throw new LocalizedException(Status.BUSINESS_RULE_VIOLATION,
                NotifErrorCodes.NOTIF_LOG_INVALID_TRANSITION, currentStatus, targetStatus);
        }
    }

    /**
     * Whether a delivery attempt may run: only a {@code QUEUED} row is delivered. A row already in a
     * final state (delivered by another worker, e.g. after a requeue) is left alone.
     */
    public boolean isAwaitingDelivery() {
        return STATUS_QUEUED.equals(currentStatus);
    }

    /**
     * Whether another attempt is allowed after {@code attemptsMade} failed ones, given the configured
     * ceiling (RULE-NOTIF-002, default 5).
     */
    public static boolean hasAttemptsLeft(int attemptsMade, int maxAttempts) {
        return attemptsMade < maxAttempts;
    }

    public String getCurrentStatus() {
        return currentStatus;
    }
}
