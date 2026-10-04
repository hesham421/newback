package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-SEC-020 register-action body (ENT-SEC-006). {@code permissionCode} is absent by design — it
 * is derived server-side as {@code PERM_<pageCode>_<actionCode>} and never caller-supplied.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Register an action - تسجيل إجراء")
public class ActionRegistryCreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Owning screen page code - رمز صفحة الشاشة المالكة", example = "TST_SCREEN")
    private String pageCode;

    @NotBlank(message = "{validation.required}")
    @Size(max = 40, message = "{validation.size}")
    @Schema(description = "Action code, upper-cased server-side - رمز الإجراء", example = "VIEW")
    private String actionCode;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Action name (Arabic) - اسم الإجراء بالعربية", example = "عرض")
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Action name (English) - اسم الإجراء بالإنجليزية", example = "View")
    private String nameEn;
}
