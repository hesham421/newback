package com.erp.notif.service;

import com.erp.events.ErpCoreEvents;
import com.erp.events.UserPasswordChangedEvent;
import com.erp.notif.channel.NotifChannels;
import com.erp.notif.dto.DispatchRequest;
import com.erp.tenant.TenantContext;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * RULE-NOTIF-023 (tenant-maturity D, XM-NOTIF-003) — e-mails {@code STAFF_PASSWORD_CHANGED} to the user
 * whose password an administrator set or who changed it (the public core event
 * {@link UserPasswordChangedEvent}). After commit, on the core event executor, inside the event's tenant;
 * a failure (template missing or inactive, recipient inactive) is logged and never touches the change.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StaffPasswordChangedNotifier {

    public static final String TEMPLATE_CODE = "STAFF_PASSWORD_CHANGED";

    private static final String MODULE_CODE = "SEC";
    private static final String REFERENCE_TYPE = "SEC_USER";
    private static final DateTimeFormatter CHANGED_AT_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'", Locale.ENGLISH).withZone(ZoneOffset.UTC);

    private final DispatchService dispatchService;

    @Async(ErpCoreEvents.EXECUTOR)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onUserPasswordChanged(UserPasswordChangedEvent event) {
        if (event.getTenantId() == null) {
            log.error("{} carries no tenant — the password-change e-mail of User ID {} is not sent", event, event.getUserId());
            return;
        }
        try {
            TenantContext.runAs(event.getTenantId(), () -> dispatchService.dispatch(DispatchRequest.builder()
                .recipientId(event.getUserId())
                .templateCode(TEMPLATE_CODE)
                .channelHint(List.of(NotifChannels.EMAIL))
                .moduleCode(MODULE_CODE)
                .referenceId(event.getUserId())
                .referenceType(REFERENCE_TYPE)
                .variables(Map.of(
                    "changedAt", CHANGED_AT_FORMAT.format(event.getOccurredAt()),
                    "changedBy", event.getActor()))
                .build()));
            log.info("Password-change e-mail queued for User ID {} (by administrator: {})", event.getUserId(), event.isByAdmin());
        } catch (RuntimeException e) {
            log.warn("Password-change e-mail for User ID {} could not be dispatched — the change itself stands",
                event.getUserId(), e);
        }
    }
}
