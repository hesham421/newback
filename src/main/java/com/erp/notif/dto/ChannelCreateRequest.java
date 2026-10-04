package com.erp.notif.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-NOTIF-005 create request body (ENTITY-NOTIF-003 NotificationChannelConfig). channelTypeId is
 * the create-only natural key (LOV-NOTIF-001, unique — RULE-NOTIF-006), so it is structurally absent
 * from the update DTO. Excludes id and audit fields (AuditEntityListener).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create a notification channel config - إنشاء تهيئة قناة إشعار")
public class ChannelCreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 20, message = "{validation.size}")
    @Schema(description = "Channel type code (LOV-NOTIF-001) - رمز القناة", example = "EMAIL")
    private String channelTypeId;

    @Schema(description = "Enabled status (RULE-NOTIF-003) - حالة التفعيل", example = "true")
    @Builder.Default
    private Boolean isEnabledFl = Boolean.TRUE;

    @Schema(description = "Provider config as JSON text - تهيئة المزوّد (JSON)")
    private String configJson;
}
