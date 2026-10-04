package com.erp.sec.service;

import com.erp.audit.crossmodule.AuditEntry;
import com.erp.sec.entity.User;

/**
 * erp-core step 10 — the generic-audit-log entries SEC writes for its account events (LOGIN, LOGOUT,
 * PASSWORD_RESET), next to the security-specific {@code SEC_AUDIT_LOG} rows it keeps writing, so one
 * timeline exists. The actor is the account itself — also on the anonymous paths (login, reset
 * completion), where the security context carries no caller yet.
 */
final class SecAuditEntries {

    /** {@code ENTITY_TYPE} of SEC user accounts (the {@code @Audited} type of {@code User}). */
    static final String ENTITY_TYPE_USER = "SEC_USER";

    private SecAuditEntries() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** An account event of {@code user}; {@code ip} may be {@code null} (the request's IP is then used). */
    static AuditEntry accountEvent(String action, User user, String summaryAr, String summaryEn, String ip) {
        return AuditEntry.builder()
            .action(action)
            .actor(user.getUsername())
            .actorRealm(user.getRealm())
            .actorUserId(user.getUserPk())
            .entityType(ENTITY_TYPE_USER)
            .entityId(String.valueOf(user.getUserPk()))
            .summaryAr(summaryAr)
            .summaryEn(summaryEn)
            .ip(ip)
            .build();
    }
}
