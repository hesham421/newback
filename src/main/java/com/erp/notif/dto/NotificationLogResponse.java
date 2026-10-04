package com.erp.notif.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-NOTIF-002/003 response body (ENTITY-NOTIF-001 NotificationLog) — all fields including
 * errorMessage and retryCount (SCR-NOTIF-003 read-only detail). templateFk is exposed as the parent
 * template id ({@code templateId}) — the association is never serialized as a nested entity.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification log entry - سجل إشعار")
public class NotificationLogResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long id;

    @Schema(description = "Recipient UserAccount id (SEC) - معرّف المستلِم", example = "42")
    private Long recipientId;

    @Schema(description = "Channel type code (LOV-NOTIF-001) - رمز القناة", example = "EMAIL")
    private String channelTypeId;

    @Schema(description = "Notification status code (LOV-NOTIF-002) - رمز الحالة", example = "SENT")
    private String notificationStatusId;

    @Schema(description = "Sending module code - رمز الموديول المُرسِل", example = "SEC")
    private String moduleCode;

    @Schema(description = "Source entity reference id - معرّف المرجع", example = "1001")
    private Long referenceId;

    @Schema(description = "Source entity reference type - نوع المرجع", example = "USER_ACCOUNT")
    private String referenceType;

    @Schema(description = "Retry counter (<=5 then FAILED) - عدد المحاولات", example = "0")
    private Short retryCount;

    @Schema(description = "Failure reason, if any - رسالة الخطأ")
    private String errorMessage;

    @Schema(description = "Sent timestamp - تاريخ الإرسال")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS")
    private LocalDateTime sentAt;

    @Schema(description = "Parent template id - معرّف القالب", example = "5")
    private Long templateId;

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
