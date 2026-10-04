package com.erp.notif.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-NOTIF-005 response body (ENTITY-NOTIF-003 NotificationChannelConfig) — all business fields
 * plus audit. The entity property is {@code isEnabled}; the DTO exposes {@code isEnabledFl} (the
 * mapper bridges).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification channel config - تهيئة قناة إشعار")
public class ChannelResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long id;

    @Schema(description = "Channel type code (LOV-NOTIF-001) - رمز القناة", example = "EMAIL")
    private String channelTypeId;

    @Schema(description = "Enabled status (RULE-NOTIF-003) - حالة التفعيل", example = "true")
    private Boolean isEnabledFl;

    @Schema(description = "Provider config as JSON text - تهيئة المزوّد (JSON)")
    private String configJson;

    @Schema(description = "Created timestamp - تاريخ الإنشاء")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    @Schema(description = "Created by - أنشئ بواسطة")
    private String createdBy;

    @Schema(description = "Updated timestamp - تاريخ التحديث")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;

    @Schema(description = "Updated by - حُدّث بواسطة")
    private String updatedBy;
}
