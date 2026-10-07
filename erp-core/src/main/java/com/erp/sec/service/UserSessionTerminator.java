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
class UserSessionTerminator {

    /** AUDIT_EVENT_TYPE code (CHK_SEC_AUDIT_LOG_EVENT_TYPE). */
    private static final String EVENT_SESSION_TERMINATED = "SESSION_TERMINATED";

    private final ActiveSessionRepository activeSessionRepository;
    private final AuditLogEntryRepository auditLogEntryRepository;
    private final UserRepository userRepository;

    /**
     * Terminates every open session of {@code user} except the one whose {@code tokenRef} is
     * {@code keepTokenRef} (null keeps none) and answers how many it ended.
     */
    int terminateOpenSessions(User user, String keepTokenRef, String detailsAr, String detailsEn) {
        String principal = SecurityContextHelper.getCurrentUsername();
        User actor = userRepository.findByUsername(principal).orElse(null);
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
}
