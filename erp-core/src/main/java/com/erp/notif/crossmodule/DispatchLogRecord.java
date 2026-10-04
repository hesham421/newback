package com.erp.notif.crossmodule;

/**
 * Read-model for {@link NotificationLogQueryApi#findByRecipientModuleAndReference} — the
 * ENTITY-NOTIF-001 fields a caller needs to confirm what was actually dispatched: the log id,
 * the resolved template code (never the raw {@code templateFk} association), and the current
 * dispatch status.
 */
public record DispatchLogRecord(Long id, String templateCode, String notificationStatusId) {
}
