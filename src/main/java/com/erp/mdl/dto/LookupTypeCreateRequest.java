package com.erp.mdl.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-MDL-002 request body. Excludes {lookupTypePk, isActiveFl, audit} — SVC-API-CRUD.md.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create a lookup type - إنشاء نوع لوكب جديد")
public class LookupTypeCreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 80, message = "{validation.size}")
    @Schema(description = "Unique lookup type key - مفتاح نوع اللوكب الفريد", example = "ORDER_STATUS")
    private String key;

    @NotBlank(message = "{validation.required}")
    @Size(max = 10, message = "{validation.size}")
    @Schema(description = "Owner module code, validated against SEC's module registry - رمز الوحدة المالكة", example = "FIN")
    private String ownerModuleCode;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Name (Arabic) - الاسم بالعربية", example = "حالة الطلب")
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Name (English) - الاسم بالإنجليزية", example = "Order Status")
    private String nameEn;
}
