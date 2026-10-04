package com.erp.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-FILE-001 upload metadata (bound as multipart form fields via {@code @ModelAttribute}); the
 * file part itself is a separate controller {@code @RequestParam}, not a field here. Ownership
 * (ownerId/ownerType/moduleCode) is mandatory structural validation for RULE-FILE-005 (ERR-0004).
 * fileCategoryFk is the optional per-category limits reference (DRV-004). Server-detected fields
 * (fileName/contentType/fileSize/fileTypeId/fileStatusId) are never client-supplied.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Upload file metadata - بيانات رفع الملف")
public class UploadRequest {

    @NotNull(message = "{validation.required}")
    @Schema(description = "Polymorphic owner id - معرّف المالك", example = "1001")
    private Long ownerId;

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    @Schema(description = "Polymorphic owner type - نوع المالك", example = "PURCHASE_ORDER")
    private String ownerType;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Owning module code - رمز الموديول المالك", example = "PROC")
    private String moduleCode;

    @Schema(description = "Optional file category id (per-category size/type limits) - معرّف فئة الملف", example = "3")
    private Long fileCategoryFk;
}
