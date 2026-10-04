package com.erp.notif.channel;

import com.erp.notif.entity.NotificationInboxItem;
import com.erp.notif.repository.NotificationInboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * The core IN_APP {@link ChannelProvider} (erp-core step 08): writes the rendered bilingual title and
 * body into the recipient's {@code NOTIF_INBOX} (one repository transaction, opened under the message's
 * tenant, so the row belongs to that tenant). An application may replace it with its own
 * IN_APP provider bean (e.g. one that also pushes over a WebSocket).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InAppChannelProvider implements ChannelProvider {

    private static final int TITLE_MAX = 300;

    private final NotificationInboxRepository inboxRepository;

    @Override
    public String channel() {
        return NotifChannels.IN_APP;
    }

    @Override
    public DeliveryResult send(OutboundMessage message) {
        NotificationInboxItem saved = inboxRepository.save(NotificationInboxItem.builder()
            .recipientUserId(message.recipientId())
            .titleAr(title(TemplateText.render(message.titleAr(), message.variables())))
            .titleEn(title(TemplateText.render(message.titleEn(), message.variables())))
            .bodyAr(TemplateText.render(message.bodyAr(), message.variables()))
            .bodyEn(TemplateText.render(message.bodyEn(), message.variables()))
            .referenceType(message.referenceType())
            .referenceId(message.referenceId())
            .build());
        log.info("IN_APP notification {} stored as inbox item {} for recipient {}",
            message.notificationLogId(), saved.getId(), message.recipientId());
        return DeliveryResult.sent();
    }

    /** NOTIF_INBOX.TITLE_* is VARCHAR(300); a substituted subject may be longer. */
    private static String title(String text) {
        return text != null && text.length() > TITLE_MAX ? text.substring(0, TITLE_MAX) : text;
    }
}
