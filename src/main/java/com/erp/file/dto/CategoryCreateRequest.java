package com.erp.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-FILE-007 create request body (ENTITY-FILE-002 FileCategory). categoryCode is the create-only
 * natural key — immutable afterwards (RULE-FILE-007), so it is structurally absent from the update
 * DTO. nameAr and nameEn are both mandatory. maxSizeBytes / allowedContentTypes are optional
 * per-category overrides feeding RULE-FILE-001/002. Excludes id and audit fields
 * (AuditEntityListener).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create a file category - إنشاء فئة ملف")
public class CategoryCreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Unique category code - رمز الفئة الفريد", example = "CONTRACTS")
    private String categoryCode;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Category name (Arabic) - اسم الفئة بالعربية", example = "العقود")
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    @Schema(description = "Category name (English) - اسم الفئة بالإنجليزية", example = "Contracts")
    private String nameEn;

    @Schema(description = "Maximum file size in bytes (per-category override) - الحد الأقصى لحجم الملف بالبايت", example = "10485760")
    private Long maxSizeBytes;

    @Schema(description = "Allowed content types, comma-separated (per-category override) - أنواع المحتوى المسموحة", example = "application/pdf,image/png")
    private String allowedContentTypes;

    @Schema(description = "Active status - حالة التفعيل", example = "true")
    @Builder.Default
    private Boolean isActiveFl = Boolean.TRUE;
}
