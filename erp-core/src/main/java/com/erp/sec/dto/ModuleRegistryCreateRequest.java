package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-SEC-018 register-module body (ENT-SEC-004). {@code isActiveFl} is absent — a freshly
 * registered module is always active (AC-SEC-016).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Register a module - تسجيل وحدة")
public class ModuleRegistryCreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 10, message = "{validation.size}")
    @Schema(description = "Module code, upper-cased server-side - رمز الوحدة", example = "TST")
    private String code;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Module name (Arabic) - اسم الوحدة بالعربية", example = "وحدة الاختبار")
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Module name (English) - اسم الوحدة بالإنجليزية", example = "Test module")
    private String nameEn;
}
