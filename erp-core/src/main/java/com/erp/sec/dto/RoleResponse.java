package com.erp.sec.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** ENT-SEC-002 response — all fields plus audit (DATA-DOM-MASTER.md ENT-SEC-002). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Role - دور")
public class RoleResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long rolePk;

    @Schema(description = "Unique role code - رمز الدور الفريد", example = "SEC_ADMIN")
    private String code;

    @Schema(description = "Role name (Arabic) - اسم الدور بالعربية", example = "مدير الأمان")
    private String nameAr;

    @Schema(description = "Role name (English) - اسم الدور بالإنجليزية", example = "Security administrator")
    private String nameEn;

    @Schema(description = "Description (Arabic) - الوصف بالعربية", example = "إدارة المستخدمين والأدوار")
    private String descriptionAr;

    @Schema(description = "Description (English) - الوصف بالإنجليزية", example = "Manages users and roles")
    private String descriptionEn;

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
