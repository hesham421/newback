package com.erp.fin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-FIN-006 request body (SVC-API-CRUD.md): {@code {code, nameAr, nameEn}}. Excludes
 * {dimensionPk, isActiveFl, audit}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create an analysis dimension - إنشاء بُعد تحليلي")
public class DimensionCreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 30, message = "{validation.size}")
    @Schema(description = "Unique dimension code - رمز البُعد الفريد", example = "COST_CENTER")
    private String code;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Name (Arabic) - الاسم بالعربية", example = "مركز التكلفة")
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Name (English) - الاسم بالإنجليزية", example = "Cost centre")
    private String nameEn;
}
