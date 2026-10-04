package com.erp.notif.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.events.DomainEventPublisher;
import com.erp.events.NotificationRequestedEvent;
import com.erp.notif.channel.NotifChannels;
import com.erp.notif.crossmodule.RecipientDirectory;
import com.erp.notif.domain.NotificationChannelConfigDomain;
import com.erp.notif.domain.NotificationLogDomain;
import com.erp.notif.domain.NotificationTemplateDomain;
import com.erp.notif.dto.DispatchRequest;
import com.erp.notif.dto.DispatchResponse;
import com.erp.notif.entity.NotificationChannelConfig;
import com.erp.notif.entity.NotificationLog;
import com.erp.notif.entity.NotificationTemplate;
import com.erp.notif.exception.NotifErrorCodes;
import com.erp.notif.repository.NotificationChannelConfigRepository;
import com.erp.notif.repository.NotificationLogRepository;
import com.erp.notif.repository.NotificationTemplateRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * API-NOTIF-001 dispatch orchestration (ENTITY-NOTIF-001) — event-driven since erp-core step 08.
 * Business-neutral fan-out: resolve the template (QR-NOTIF-0007), skip an inactive recipient
 * (RULE-NOTIF-007), then create one NOTIF_LOG per requested channel (RULE-NOTIF-001) — a
 * disabled/unconfigured channel becomes CHANNEL_DISABLED and is never delivered (RULE-NOTIF-003), an
 * enabled channel becomes QUEUED (its variables kept in VARIABLES_JSON) and a
 * {@link NotificationRequestedEvent} is published. An EMAIL row whose request carries no {@code email}
 * variable gets the recipient's account e-mail from the recipient directory (erp-core step 14); an
 * explicit {@code variables.email} always wins.
 *
 * <p>Nothing is sent here: {@link NotificationDeliveryListener} delivers each queued row asynchronously
 * after this transaction commits, through the channel's {@code ChannelProvider}, with Spring Retry
 * (RULE-NOTIF-002, {@link NotificationDeliveryWorker}). Dispatch therefore returns in the time of a few
 * inserts, whatever the channel. Pure "is this allowed?" decisions are delegated to the entity Domain
 * companions; this service is orchestration only.
 *
 * <p>No caching annotations — NOTIF is absent from the caching approved-register.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DispatchService {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationChannelConfigRepository channelRepository;
    private final NotificationLogRepository logRepository;
    private final RecipientDirectory recipientDirectory;
    private final DomainEventPublisher eventPublisher;

    /**
     * API-NOTIF-001 — fan-out dispatch. Returns the created NOTIF_LOG ids (empty when the recipient is
     * inactive, RULE-NOTIF-007). Note: the plan specified HTTP 202; the shared ServiceResult/Status
     * taxonomy expresses only 200/201 (common is out of scope to modify), so this returns 200
     * (recorded as an api_doc_gap).
     */
    @Transactional
    // RULE-NOTIF-005 — dispatch sits behind the Security filter, not tied to a management screen, so
    // it is gated to any authenticated principal rather than a page permission (SEC_PERMISSION.PAGE_FK
    // is NOT NULL, so no screenless dispatch permission can be seeded).
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<DispatchResponse> dispatch(DispatchRequest request) {
        return doDispatch(request, recipientDirectory.isActive(request.getRecipientId()));
    }

    /**
     * Independent-transaction entry point, reached from within this JVM only — never from a
     * controller. Its declared caller is {@code NotificationDispatchApi.dispatchIndependently}, the
     * cross-module surface a consuming module uses when its own writes must survive a failed
     * dispatch (SEC's password-reset token issuance, API-SEC-003).
     *
     * <p>Gated by the same {@code isAuthenticated()} check as {@link #dispatch} (RULE-NOTIF-005): a
     * principal-less in-process caller satisfies it by wrapping the call in its own internal-caller
     * utility, which installs a synthetic authentication for the duration of the call.
     *
     * <p>REQUIRES_NEW is deliberate: a failure inside dispatch must not mark a consuming module's
     * transaction rollback-only — that flag survives a consuming-side catch and would fail the
     * caller's commit. The queued rows commit with this inner transaction, which is also when their
     * {@link NotificationRequestedEvent}s reach the asynchronous delivery listener.
     *
     * <p>The recipient's status is resolved by the caller, <em>before</em> this transaction opens: a
     * REQUIRES_NEW transaction cannot see a user account the consuming module created in its own,
     * still-open transaction, so resolving it here would wrongly skip that recipient as unknown.
     *
     * @param recipientActive the RULE-NOTIF-007 answer, resolved in the caller's transaction
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<DispatchResponse> dispatchSystem(DispatchRequest request, boolean recipientActive) {
        return doDispatch(request, recipientActive);
    }

    private ServiceResult<DispatchResponse> doDispatch(DispatchRequest request, boolean recipientActive) {
        log.info("Dispatching notification: template={}, recipient={}, channels={}",
            request.getTemplateCode(), request.getRecipientId(), request.getChannelHint());

        // QR-NOTIF-0007 — resolve template by natural key; unknown code → ERR-0004 404.
        NotificationTemplate template = templateRepository.findByTemplateCode(normalize(request.getTemplateCode()))
            .orElseThrow(() -> new LocalizedException(
                Status.NOT_FOUND, NotifErrorCodes.NOTIF_TEMPLATE_NOT_FOUND, request.getTemplateCode()));

        // RULE-NOTIF-007 (A6) — a deactivated template is retired from dispatch; reject up-front. The
        // bilingual body (RULE-NOTIF-004) is a NOT-NULL/@NotBlank column, guaranteed on any persisted
        // row, so it needs no re-check here.
        NotificationTemplateDomain.from(template).assertDispatchable();

        // RULE-NOTIF-007 — do not dispatch to an inactive or unknown recipient (XM-NOTIF-001, resolved
        // through SEC's user directory); create no logs, keep history.
        if (!recipientActive) {
            log.info("Recipient {} is inactive or unknown — dispatch skipped (RULE-NOTIF-007); no logs created",
                request.getRecipientId());
            return ServiceResult.success(DispatchResponse.builder().logIds(List.of()).build());
        }

        String variablesJson = DispatchVariables.write(request.getVariables());
        String emailVariablesJson = null;     // resolved once, only if an EMAIL row is queued
        List<Long> logIds = new ArrayList<>();
        for (String channelHint : request.getChannelHint()) {
            String channelTypeId = normalize(channelHint);

            // QR-NOTIF-0011 — resolve channel config; a missing config is treated as not-enabled.
            NotificationChannelConfig channel = channelRepository.findByChannelTypeId(channelTypeId).orElse(null);
            boolean enabled = channel != null
                && NotificationChannelConfigDomain.from(channel).isEnabledForDispatch();

            NotificationLog logRow = newPendingLog(request, template, channelTypeId);
            if (!enabled) {
                // RULE-NOTIF-003 — disabled/unconfigured channel → CHANNEL_DISABLED, never delivered.
                transitionTo(logRow, NotificationLogDomain.STATUS_CHANNEL_DISABLED);
            } else {
                // erp-core step 08 — queue it; the asynchronous worker delivers it after commit.
                transitionTo(logRow, NotificationLogDomain.STATUS_QUEUED);
                if (NotifChannels.EMAIL.equals(channelTypeId)) {
                    if (emailVariablesJson == null) {
                        emailVariablesJson = DispatchVariables.write(withRecipientEmail(request));
                    }
                    logRow.setVariablesJson(emailVariablesJson);
                } else {
                    logRow.setVariablesJson(variablesJson);
                }
            }
            NotificationLog saved = logRepository.save(logRow);
            logIds.add(saved.getId());
            if (enabled) {
                eventPublisher.publish(new NotificationRequestedEvent(saved.getId(), channelTypeId,
                    saved.getRecipientId(), template.getTemplateCode()));
            }
        }

        return ServiceResult.success(DispatchResponse.builder().logIds(logIds).build());
    }

    /**
     * erp-core step 14 — the variables of an EMAIL row: the request's own, plus {@code email} from the
     * recipient's account when the request supplied none (blank counts as none). An explicit
     * {@code variables.email} is an override and is kept as is. When the directory knows no address
     * either, the variables stay without it and the EMAIL provider rejects the row (FAILED, one attempt).
     */
    private Map<String, String> withRecipientEmail(DispatchRequest request) {
        Map<String, String> variables = request.getVariables() == null
            ? new LinkedHashMap<>()
            : new LinkedHashMap<>(request.getVariables());
        String explicit = variables.get("email");
        if (explicit == null || explicit.isBlank()) {
            recipientDirectory.emailOf(request.getRecipientId())
                .ifPresent(email -> variables.put("email", email));
        }
        return variables;
    }

    /** Builds a transient PENDING log for one channel (RULE-NOTIF-001 fan-out row). */
    private NotificationLog newPendingLog(DispatchRequest request, NotificationTemplate template,
                                          String channelTypeId) {
        return NotificationLog.builder()
            .recipientId(request.getRecipientId())
            .channelTypeId(channelTypeId)
            .notificationStatusId(NotificationLogDomain.STATUS_PENDING)
            .moduleCode(request.getModuleCode())
            .referenceId(request.getReferenceId())
            .referenceType(request.getReferenceType())
            .retryCount((short) 0)
            .attempts(0)
            .templateFk(template)
            .build();
    }

    /** LOV-NOTIF-002 (A6) — guard the transition via the log Domain, then mutate the status. */
    private void transitionTo(NotificationLog logRow, String targetStatus) {
        NotificationLogDomain.from(logRow).assertCanTransitionTo(targetStatus);
        logRow.setNotificationStatusId(targetStatus);
    }

    private static String normalize(String code) {
        return code == null ? null : code.trim().toUpperCase();
    }
}
