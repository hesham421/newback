package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-SEC-019 register-screen body (ENT-SEC-005). The owning module is named by its
 * {@code moduleCode}, not its id — RULE-SEC-004 resolves it server-side.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Register a screen - تسجيل شاشة")
public class ScreenRegistryCreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 10, message = "{validation.size}")
    @Schema(description = "Owning module code - رمز الوحدة المالكة", example = "TST")
    private String moduleCode;

    @NotBlank(message = "{validation.required}")
    @Size(max = 50, message = "{validation.size}")
    @Schema(description = "Page code, upper-cased server-side - رمز الصفحة", example = "TST_SCREEN")
    private String pageCode;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Screen name (Arabic) - اسم الشاشة بالعربية", example = "شاشة الاختبار")
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Screen name (English) - اسم الشاشة بالإنجليزية", example = "Test screen")
    private String nameEn;
}
