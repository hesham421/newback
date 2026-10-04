package com.erp.notif.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-NOTIF-004 create request body (ENTITY-NOTIF-002 NotificationTemplate). templateCode is the
 * create-only natural key — immutable afterwards (RULE-NOTIF-006), so it is structurally absent from
 * the update DTO. bodyAr and bodyEn are both mandatory (RULE-NOTIF-004, bilingual). Excludes id and
 * audit fields (AuditEntityListener).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create a notification template - إنشاء قالب إشعار")
public class TemplateCreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 80, message = "{validation.size}")
    @Schema(description = "Unique template code - رمز القالب الفريد", example = "PASSWORD_RESET")
    private String templateCode;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Template name (Arabic) - اسم القالب بالعربية", example = "ترحيب بالمستخدم")
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    @Schema(description = "Template name (English) - اسم القالب بالإنجليزية", example = "User Welcome")
    private String nameEn;

    @Size(max = 300, message = "{validation.size}")
    @Schema(description = "Email subject (Arabic) - عنوان البريد بالعربية", example = "مرحبًا بك")
    private String subjectAr;

    @Size(max = 300, message = "{validation.size}")
    @Schema(description = "Email subject (English) - عنوان البريد بالإنجليزية", example = "Welcome")
    private String subjectEn;

    @NotBlank(message = "{validation.required}")
    @Schema(description = "Template body (Arabic) - متن القالب بالعربية", example = "أهلًا {0}")
    private String bodyAr;

    @NotBlank(message = "{validation.required}")
    @Schema(description = "Template body (English) - متن القالب بالإنجليزية", example = "Hello {0}")
    private String bodyEn;

    @Schema(description = "Optional attachment file id (FILE service) - معرّف مرفق اختياري", example = "1001")
    private Long attachmentFileId;

    @Schema(description = "Active status - حالة التفعيل", example = "true")
    @Builder.Default
    private Boolean isActiveFl = Boolean.TRUE;
}
