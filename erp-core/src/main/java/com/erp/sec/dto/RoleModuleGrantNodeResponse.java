package com.erp.sec.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Module level of the role grant tree. {@code granted} is the ENT-SEC-007 grant itself; the
 * {@code screens} beneath it are exactly what RULE-SEC-003's cascade removes when this module
 * grant is revoked, which is what a revoke confirmation has to be able to name.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Module grant held by a role - وحدة ممنوحة للدور")
public class RoleModuleGrantNodeResponse {

    @Schema(description = "Module registry identifier - معرف الوحدة", example = "1")
    private Long moduleRegPk;

    @Schema(description = "Module code - رمز الوحدة", example = "SEC")
    private String code;

    @Schema(description = "Module name (Arabic) - اسم الوحدة بالعربية", example = "الأمان")
    private String nameAr;

    @Schema(description = "Module name (English) - اسم الوحدة بالإنجليزية", example = "Security")
    private String nameEn;

    @Schema(description = "Module grant held - هل الوحدة ممنوحة", example = "true")
    private Boolean granted;

    @Schema(description = "Granted timestamp, null when not granted - تاريخ المنح")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant grantedAt;

    @Schema(description = "Screen grants held beneath this module - الشاشات الممنوحة ضمن الوحدة")
    private List<RoleScreenGrantNodeResponse> screens;
}
