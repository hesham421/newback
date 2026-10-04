package com.erp.mdl.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-MDL-006 request body. {@code lookupTypeId} comes from the path (SVC-API-CRUD.md), not this
 * body. Excludes {lookupValuePk, isActiveFl, audit}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create a lookup value - إنشاء قيمة لوكب جديدة")
public class LookupValueCreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Unique code within the parent lookup type - الرمز الفريد ضمن النوع", example = "PENDING")
    private String code;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Name (Arabic) - الاسم بالعربية", example = "قيد الانتظار")
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Name (English) - الاسم بالإنجليزية", example = "Pending")
    private String nameEn;

    @NotNull(message = "{validation.required}")
    @Schema(description = "Display sort order - ترتيب العرض", example = "10")
    private Integer sortOrder;
}
