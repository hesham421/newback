package com.erp.file.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-FILE-007 response body (ENTITY-FILE-002 FileCategory) — all business fields plus audit. The
 * entity property is {@code isActive}; the DTO exposes {@code isActiveFl} (the mapper bridges).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "File category - فئة ملف")
public class CategoryResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long id;

    @Schema(description = "Unique category code - رمز الفئة الفريد", example = "CONTRACTS")
    private String categoryCode;

    @Schema(description = "Category name (Arabic) - اسم الفئة بالعربية")
    private String nameAr;

    @Schema(description = "Category name (English) - اسم الفئة بالإنجليزية")
    private String nameEn;

    @Schema(description = "Maximum file size in bytes (per-category override) - الحد الأقصى لحجم الملف بالبايت")
    private Long maxSizeBytes;

    @Schema(description = "Allowed content types, comma-separated (per-category override) - أنواع المحتوى المسموحة")
    private String allowedContentTypes;

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
