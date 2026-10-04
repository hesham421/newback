package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The API-SEC-015 confirmation — how many dependent grants RULE-SEC-003's mandatory cascade
 * removed alongside the module grant itself.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Module grant revocation result - نتيجة سحب منح الوحدة")
public class ModuleGrantRevokeResponse {

    @Schema(description = "Cascaded screen grants removed - عدد منح الشاشات المسحوبة", example = "2")
    private int revokedScreenGrants;

    @Schema(description = "Cascaded action grants removed - عدد منح الإجراءات المسحوبة", example = "3")
    private int revokedActionGrants;
}
