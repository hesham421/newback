package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The action-revoke confirmation (REQ-SEC-037) — every action grant the call removed, the
 * requested one included: 1, or 1 + N when revoking VIEW cascaded N others (RULE-SEC-009).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Action grant revocation result - نتيجة سحب منح الإجراء")
public class ActionGrantRevokeResponse {

    @Schema(description = "Action grants removed, the requested one included - عدد منح الإجراءات المسحوبة بما فيها المطلوب",
        example = "1")
    private int revokedActionGrants;
}
