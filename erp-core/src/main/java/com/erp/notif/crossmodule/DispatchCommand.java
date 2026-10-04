package com.erp.notif.crossmodule;

import java.util.List;
import java.util.Map;

/**
 * Narrow read-model carried across the module boundary by {@link NotificationDispatchApi} — a plain
 * record, never a JPA entity or an internal DTO. Other modules build one of these to request a
 * notification dispatch instead of NOTIF listening to their internal events (skill outranks the
 * plan's "consume CU NotificationEvent" approach).
 */
public record DispatchCommand(
    Long recipientId,
    String templateCode,
    List<String> channelHint,
    String moduleCode,
    Long referenceId,
    String referenceType,
    Map<String, String> variables) {
}
