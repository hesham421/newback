package com.erp.sec.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-SEC-021 row — one ENT-SEC-004 module with its active screens and, per screen, its active
 * actions. The paged unit is the module, so nesting is assembled by the service, never by a join.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Registry row - صف سجل الوحدات")
public class RegistryRowResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long moduleRegPk;

    @Schema(description = "Module code - رمز الوحدة", example = "FIN")
    private String code;

    @Schema(description = "Module name (Arabic) - اسم الوحدة بالعربية", example = "المالية")
    private String nameAr;

    @Schema(description = "Module name (English) - اسم الوحدة بالإنجليزية", example = "Finance")
    private String nameEn;

    @Schema(description = "Active status - حالة التفعيل", example = "true")
    private Boolean isActiveFl;

    @Schema(description = "Active screens of this module - الشاشات النشطة للوحدة")
    private List<ScreenRegistryResponse> screens;

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
