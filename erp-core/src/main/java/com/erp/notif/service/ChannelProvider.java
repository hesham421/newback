package com.erp.notif.service;

import com.erp.notif.entity.NotificationTemplate;
import java.util.Map;

/**
 * DRV-004 (OQ-NOTIF-001) — provider-agnostic send abstraction. EMAIL is resolved to a real SMTP send
 * (see {@link DefaultChannelProvider}); SMS/WhatsApp/Push/internal remain a later implementation
 * decision. Dispatch resolves an implementation generically and delegates the actual send, applying
 * {@link com.erp.notif.domain.RetryPolicy}.
 */
public interface ChannelProvider {

    /**
     * Attempts to deliver the rendered template to the recipient over the given channel. Never
     * throws for a delivery failure — it returns a failed {@link ChannelSendResult} so the caller
     * can drive the RULE-NOTIF-002 retry/FAILED path.
     */
    ChannelSendResult send(String channelTypeId, Long recipientId, NotificationTemplate template,
                           String configJson, Map<String, String> variables);
}
