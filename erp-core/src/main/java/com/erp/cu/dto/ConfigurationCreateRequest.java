package com.erp.cu.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-CU-001 request body. Excludes appConfigurationPk (system-generated), isActiveFl (every
 * configuration starts active — see AppConfiguration's own @Builder.Default), and audit fields.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create a platform configuration entry - إنشاء إعداد منصة جديد")
public class ConfigurationCreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Unique configuration key - مفتاح الإعداد الفريد", example = "MAIL_SMTP_HOST")
    private String configKey;

    @NotBlank(message = "{validation.required}")
    @Schema(description = "Configuration value - قيمة الإعداد", example = "smtp.example.com")
    private String configValue;

    @Size(max = 2000, message = "{validation.size}")
    @Schema(description = "Optional notes describing this configuration entry - ملاحظات اختيارية", example = "SMTP host used by the notification module")
    private String notes;
}
