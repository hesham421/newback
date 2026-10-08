package com.erp.sec.service;

import com.erp.common.util.SecurityContextHelper;
import com.erp.sec.entity.ActiveSession;
import com.erp.sec.entity.AuditLogEntry;
import com.erp.sec.entity.User;
import com.erp.sec.repository.ActiveSessionRepository;
import com.erp.sec.repository.AuditLogEntryRepository;
import com.erp.sec.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * tenant-maturity D — ends a user's open sessions after a password change (admin-set: all of them;
 * self-change: all but the caller's own), one {@code SESSION_TERMINATED} SEC audit row per session, in the
 * caller's transaction. The loop of {@code UserService.deactivate} / {@code PasswordResetService.complete}.
 */
@Component
@RequiredArgsConstructor
public class UserSessionTerminator {

    /** AUDIT_EVENT_TYPE code (CHK_SEC_AUDIT_LOG_EVENT_TYPE). */
    private static final String EVENT_SESSION_TERMINATED = "SESSION_TERMINATED";

    private final ActiveSessionRepository activeSessionRepository;
    private final AuditLogEntryRepository auditLogEntryRepository;
    private final UserRepository userRepository;

    /**
     * Terminates every open session of {@code user} except the one whose {@code tokenRef} is
     * {@code keepTokenRef} (null keeps none) and answers how many it ended.
     */
    public int terminateOpenSessions(User user, String keepTokenRef, String detailsAr, String detailsEn) {
        User actor = userRepository.findByUsername(SecurityContextHelper.getCurrentUsername()).orElse(null);
        return terminateOpenSessions(user, keepTokenRef, actor, detailsAr, detailsEn);
    }

    /**
     * As above with the acting user given (null = none). tenant-maturity B: the platform's admin-reset runs inside
     * the target tenant, where the operator's username could name a different user, so it passes none.
     */
    public int terminateOpenSessions(User user, String keepTokenRef, User actor, String detailsAr, String detailsEn) {
        String principal = SecurityContextHelper.getCurrentUsername();
        Instant now = Instant.now();
        List<ActiveSession> ended = activeSessionRepository.findNonTerminatedByUser(user.getUserPk()).stream()
            .filter(session -> keepTokenRef == null || !keepTokenRef.equals(session.getTokenRef()))
            .toList();
        for (ActiveSession session : ended) {
            session.terminate(principal);
            auditLogEntryRepository.save(AuditLogEntry.builder()
                .eventTypeCode(EVENT_SESSION_TERMINATED)
                .actor(actor)
                .occurredAt(now)
                .targetRef(String.valueOf(session.getActiveSessionPk()))
                .detailsAr(detailsAr)
                .detailsEn(detailsEn)
                .build());
        }
        activeSessionRepository.saveAll(ended);
        return ended.size();
    }

    /**
     * REQ-SEC-092 / -093 (tenant-maturity C12) — ends every open session of the current tenant, both realms, as
     * {@code terminatedBy} (the platform operator, who is no user of this tenant: no actor user on the audit rows).
     */
    public int terminateAllOpenSessions(String terminatedBy, String detailsAr, String detailsEn) {
        Instant now = Instant.now();
        List<ActiveSession> ended = activeSessionRepository.findAllNonTerminated();
        for (ActiveSession session : ended) {
            session.terminate(terminatedBy);
            auditLogEntryRepository.save(AuditLogEntry.builder()
                .eventTypeCode(EVENT_SESSION_TERMINATED)
                .occurredAt(now)
                .targetRef(String.valueOf(session.getActiveSessionPk()))
                .detailsAr(detailsAr)
                .detailsEn(detailsEn)
                .build());
        }
        activeSessionRepository.saveAll(ended);
        return ended.size();
    }
}
