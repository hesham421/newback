package com.erp.mdl.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-MDL-003 request body — name-only. {@code key} and {@code ownerModuleCode} are absent by
 * design: RULE-MDL-003 (key immutability, DATA-DOM.md ENT-MDL-001) is enforced by this DTO shape,
 * not by a service-layer guard.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update a lookup type's names - تعديل أسماء نوع اللوكب")
public class LookupTypeUpdateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Name (Arabic) - الاسم بالعربية", example = "حالة الطلب")
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Name (English) - الاسم بالإنجليزية", example = "Order Status")
    private String nameEn;
}
