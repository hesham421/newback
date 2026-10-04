package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-FIN-007 request body (SVC-API-CRUD.md): {@code {code, nameAr, nameEn, sortOrder}}. The
 * owning {@code dimensionId} travels in the path, never in this body. Excludes
 * {dimensionValuePk, isActiveFl, audit}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create a dimension value - إنشاء قيمة بُعد")
public class DimensionValueCreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 30, message = "{validation.size}")
    @Schema(description = "Unique code within the parent dimension - الرمز الفريد ضمن البُعد",
        example = "CC-100")
    private String code;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Name (Arabic) - الاسم بالعربية", example = "الإدارة المالية")
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Name (English) - الاسم بالإنجليزية", example = "Finance department")
    private String nameEn;

    @NotNull(message = "{validation.required}")
    @Schema(description = "Display sort order - ترتيب العرض", example = "10")
    private Integer sortOrder;
}
