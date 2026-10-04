package com.erp.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-FILE-007 update request body. Excludes categoryCode — immutable after creation
 * (RULE-FILE-007, structurally enforced by omission). nameAr and nameEn stay mandatory. Active
 * state is intentionally absent: (de)activation is a separate, DELETE-permission-gated operation,
 * so it must not be settable through a plain update.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update a file category - تحديث فئة ملف")
public class CategoryUpdateRequest {

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
}
