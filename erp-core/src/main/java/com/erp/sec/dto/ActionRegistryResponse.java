package com.erp.sec.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENT-SEC-006 response — all fields plus audit, including the server-derived
 * {@code permissionCode} (API-SEC-020 Response line).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Registered action - إجراء مسجَّل")
public class ActionRegistryResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long actionRegPk;

    @Schema(description = "Server-derived permission code - رمز الصلاحية المشتق", example = "PERM_TST_SCREEN_VIEW")
    private String permissionCode;

    @Schema(description = "Owning screen id - معرف الشاشة المالكة", example = "1")
    private Long screenId;

    @Schema(description = "Owning screen page code - رمز صفحة الشاشة المالكة", example = "TST_SCREEN")
    private String pageCode;

    @Schema(description = "Action code - رمز الإجراء", example = "VIEW")
    private String actionCode;

    @Schema(description = "Action name (Arabic) - اسم الإجراء بالعربية", example = "عرض")
    private String nameAr;

    @Schema(description = "Action name (English) - اسم الإجراء بالإنجليزية", example = "View")
    private String nameEn;

    @Schema(description = "Active status - حالة التفعيل", example = "true")
    private Boolean isActiveFl;

    @Schema(description = "Created timestamp - تاريخ الإنشاء")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    @Schema(description = "Created by - أنشئ بواسطة", example = "admin")
    private String createdBy;

    @Schema(description = "Updated timestamp - تاريخ التحديث")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;

    @Schema(description = "Updated by - حُدّث بواسطة", example = "admin")
    private String updatedBy;
}
