package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The screen-revoke confirmation (REQ-SEC-036) — how many action grants RULE-SEC-008's cascade
 * removed alongside the screen grant itself.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Screen grant revocation result - نتيجة سحب منح الشاشة")
public class ScreenGrantRevokeResponse {

    @Schema(description = "Cascaded action grants removed - عدد منح الإجراءات المسحوبة", example = "3")
    private int revokedActionGrants;
}
