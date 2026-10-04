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
 * API-MDL-007 request body. Excludes {lookupValuePk, lookupTypeId, code, isActiveFl, audit} —
 * {@code lookupTypeId} and {@code code} are create-only (DATA-DOM.md ENT-MDL-002).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update a lookup value - تعديل قيمة لوكب")
public class LookupValueUpdateRequest {

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
