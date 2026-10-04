package com.erp.notif.domain;

import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.notif.entity.NotificationChannelConfig;
import com.erp.notif.exception.NotifErrorCodes;

/**
 * Domain companion for ENTITY-NOTIF-003 (NotificationChannelConfig). Owns the "is this operation
 * allowed?" decisions — RULE-NOTIF-006 (unique channelTypeId). Also exposes the pure enable/disable
 * decision (RULE-NOTIF-003) consumed later by dispatch (SVC-API). No Spring/JPA annotations, no
 * repository access; constructed only via the static factories.
 */
public final class NotificationChannelConfigDomain {

    private final String channelTypeId;
    private final boolean enabled;

    private NotificationChannelConfigDomain(String channelTypeId, boolean enabled) {
        this.channelTypeId = channelTypeId;
        this.enabled = enabled;
    }

    /**
     * Construction-time validation for create: required channelTypeId then RULE-NOTIF-006 (unique
     * channelTypeId, pre-checked by the service).
     */
    public static NotificationChannelConfigDomain create(String channelTypeId, boolean channelAlreadyTaken) {
        if (isBlank(channelTypeId)) {
            throw new LocalizedException(Status.VALIDATION_ERROR,
                NotifErrorCodes.NOTIF_CHANNEL_TYPE_REQUIRED);
        }
        if (channelAlreadyTaken) {
            throw new LocalizedException(Status.ALREADY_EXISTS,
                NotifErrorCodes.NOTIF_CHANNEL_CONFIG_DUPLICATE, channelTypeId);
        }
        return new NotificationChannelConfigDomain(channelTypeId, true);
    }

    /** Reconstructs a Domain view over a persisted entity — no validation. */
    public static NotificationChannelConfigDomain from(NotificationChannelConfig entity) {
        return new NotificationChannelConfigDomain(entity.getChannelTypeId(),
            Boolean.TRUE.equals(entity.getIsEnabled()));
    }

    /**
     * RULE-NOTIF-003 decision helper (no side effects) — whether dispatch may proceed on this
     * channel. A disabled channel yields a CHANNEL_DISABLED log at dispatch time (SVC-API).
     */
    public boolean isEnabledForDispatch() {
        return enabled;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public String getChannelTypeId() {
        return channelTypeId;
    }
}
