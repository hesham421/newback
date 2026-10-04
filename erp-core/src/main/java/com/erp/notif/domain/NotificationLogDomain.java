package com.erp.notif.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.notif.entity.NotificationLog;
import com.erp.notif.exception.NotifErrorCodes;
import java.util.Map;
import java.util.Set;

/**
 * Domain companion for ENTITY-NOTIF-001 (NotificationLog) — lifecycle state-machine guardian for
 * LOV-NOTIF-002 (A6). Valid transitions from the initial PENDING state: PENDING→SENT,
 * PENDING→FAILED, PENDING→CHANNEL_DISABLED. Fan-out (RULE-NOTIF-001), retry (RULE-NOTIF-002),
 * provider dispatch (RULE-NOTIF-003) are SVC-API concerns and are NOT implemented here. No
 * Spring/JPA annotations, no repository access; constructed only via the static factory.
 */
public final class NotificationLogDomain {

    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SENT = "SENT";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_CHANNEL_DISABLED = "CHANNEL_DISABLED";

    private static final Map<String, Set<String>> ALLOWED_TRANSITIONS = Map.of(
        STATUS_PENDING, Set.of(STATUS_SENT, STATUS_FAILED, STATUS_CHANNEL_DISABLED),
        STATUS_SENT, Set.of(),
        STATUS_FAILED, Set.of(),
        STATUS_CHANNEL_DISABLED, Set.of()
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

    public String getCurrentStatus() {
        return currentStatus;
    }
}
