package com.erp.notif.exception;

/**
 * Module-specific error codes for Notification Service (NOTIF).
 * Descriptive &lt;ENTITY&gt;_&lt;SCENARIO&gt; format (never the numbered ERR-xxxx tracking id).
 * Only the Domain-layer codes (NotificationTemplateDomain / NotificationChannelConfigDomain /
 * NotificationLogDomain) are declared in this DATA-DOM sub; later phases (SVC-API) append the
 * 404 not-found codes (ERR-0004 PLATFORM-STD).
 */
public final class NotifErrorCodes {

    private NotifErrorCodes() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** RULE-NOTIF-004 (ERR-0001) — bodyAr/bodyEn missing on template create/update. */
    public static final String NOTIF_TEMPLATE_BILINGUAL_REQUIRED = "NOTIF_TEMPLATE_BILINGUAL_REQUIRED";

    /** RULE-NOTIF-006 (ERR-0002) — duplicate templateCode on create. */
    public static final String NOTIF_TEMPLATE_CODE_DUPLICATE = "NOTIF_TEMPLATE_CODE_DUPLICATE";

    /** RULE-NOTIF-006 (ERR-0003) — duplicate channelTypeId on channel-config create. */
    public static final String NOTIF_CHANNEL_CONFIG_DUPLICATE = "NOTIF_CHANNEL_CONFIG_DUPLICATE";

    /** LOV-NOTIF-002 (A6) — illegal notificationStatusId transition (internal invariant). */
    public static final String NOTIF_LOG_INVALID_TRANSITION = "NOTIF_LOG_INVALID_TRANSITION";

    /** RULE-NOTIF-006 — required channelTypeId missing on channel-config create. */
    public static final String NOTIF_CHANNEL_TYPE_REQUIRED = "NOTIF_CHANNEL_TYPE_REQUIRED";

    /** RULE-NOTIF-007 (A6) — dispatch requested against a deactivated template. */
    public static final String NOTIF_TEMPLATE_INACTIVE = "NOTIF_TEMPLATE_INACTIVE";

    /** ERR-0004 (PLATFORM-STD) — template not found by id / unknown templateCode on dispatch. */
    public static final String NOTIF_TEMPLATE_NOT_FOUND = "NOTIF_TEMPLATE_NOT_FOUND";

    /** ERR-0004 (PLATFORM-STD) — channel config not found by id. */
    public static final String NOTIF_CHANNEL_CONFIG_NOT_FOUND = "NOTIF_CHANNEL_CONFIG_NOT_FOUND";

    /** ERR-0004 (PLATFORM-STD) — notification log not found by id. */
    public static final String NOTIF_LOG_NOT_FOUND = "NOTIF_LOG_NOT_FOUND";

    /** XM-NOTIF-002 — template attachmentFileId names no available FILE document. */
    public static final String NOTIF_TEMPLATE_ATTACHMENT_NOT_FOUND = "NOTIF_TEMPLATE_ATTACHMENT_NOT_FOUND";

    /** ERR-0004 (PLATFORM-STD) — unknown lookupKey (API-NOTIF-006). */
    public static final String NOTIF_LOOKUP_KEY_UNKNOWN = "NOTIF_LOOKUP_KEY_UNKNOWN";

    /**
     * erp-core step 08 — the channel cannot serve this caller/message: thrown (403) when the in-app
     * inbox is used by a principal that is no user account, and recorded as the reason of a
     * {@code SKIPPED_NO_PROVIDER} log row (no {@code ChannelProvider} handles the channel).
     */
    public static final String NOTIF_CHANNEL_UNAVAILABLE = "NOTIF_CHANNEL_UNAVAILABLE";

    /** erp-core step 08 — inbox item not found, or not the caller's own (never revealed which). */
    public static final String INBOX_ITEM_NOT_FOUND = "INBOX_ITEM_NOT_FOUND";
}
