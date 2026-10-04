package com.erp.file.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * FileDocument metadata response (API-FILE-001/004/005) — all descriptive fields plus audit, and
 * NEVER the BYTEA content (DRV-003). fileTypeId (LOV-FILE-001) and fileStatusId (LOV-FILE-002) are
 * runtime lifecycle codes; fileCategoryId is the optional category FK.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "File metadata (no bytes) - بيانات الملف الوصفية")
public class FileMetadataResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "5001")
    private Long id;

    @Schema(description = "Polymorphic owner id - معرّف المالك", example = "1001")
    private Long ownerId;

    @Schema(description = "Polymorphic owner type - نوع المالك", example = "PURCHASE_ORDER")
    private String ownerType;

    @Schema(description = "Owning module code - رمز الموديول المالك", example = "PROC")
    private String moduleCode;

    @Schema(description = "Original file name - اسم الملف", example = "contract.pdf")
    private String fileName;

    @Schema(description = "Server-detected MIME type - نوع المحتوى المكتشف", example = "application/pdf")
    private String contentType;

    @Schema(description = "File size in bytes - حجم الملف بالبايت", example = "204800")
    private Long fileSize;

    @Schema(description = "File type bucket (LOV-FILE-001) - نوع الملف", example = "DOCUMENT")
    private String fileTypeId;

    @Schema(description = "Lifecycle status (LOV-FILE-002) - حالة الملف", example = "ACTIVE")
    private String fileStatusId;

    @Schema(description = "Optional file category id - معرّف فئة الملف", example = "3")
    private Long fileCategoryId;

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
