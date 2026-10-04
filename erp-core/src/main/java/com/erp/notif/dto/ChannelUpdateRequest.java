package com.erp.notif.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-NOTIF-005 update request body. Excludes channelTypeId — immutable after creation
 * (RULE-NOTIF-006, structurally enforced by omission). isEnabledFl drives dispatch behavior
 * (RULE-NOTIF-003) and is mutable through update.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update a notification channel config - تحديث تهيئة قناة إشعار")
public class ChannelUpdateRequest {

    @Schema(description = "Enabled status (RULE-NOTIF-003) - حالة التفعيل", example = "true")
    private Boolean isEnabledFl;

    @Schema(description = "Provider config as JSON text - تهيئة المزوّد (JSON)")
    private String configJson;
}
