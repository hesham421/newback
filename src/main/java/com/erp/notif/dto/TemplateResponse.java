package com.erp.notif.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-NOTIF-004 response body (ENTITY-NOTIF-002 NotificationTemplate) — all business fields plus
 * audit. The entity property is {@code isActive}; the DTO exposes {@code isActiveFl} (the mapper
 * bridges).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Notification template - قالب إشعار")
public class TemplateResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long id;

    @Schema(description = "Unique template code - رمز القالب الفريد", example = "PASSWORD_RESET")
    private String templateCode;

    @Schema(description = "Template name (Arabic) - اسم القالب بالعربية")
    private String nameAr;

    @Schema(description = "Template name (English) - اسم القالب بالإنجليزية")
    private String nameEn;

    @Schema(description = "Email subject (Arabic) - عنوان البريد بالعربية")
    private String subjectAr;

    @Schema(description = "Email subject (English) - عنوان البريد بالإنجليزية")
    private String subjectEn;

    @Schema(description = "Template body (Arabic) - متن القالب بالعربية")
    private String bodyAr;

    @Schema(description = "Template body (English) - متن القالب بالإنجليزية")
    private String bodyEn;

    @Schema(description = "Optional attachment file id (FILE service) - معرّف مرفق اختياري")
    private Long attachmentFileId;

    @Schema(description = "Active status - حالة التفعيل", example = "true")
    private Boolean isActiveFl;

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
