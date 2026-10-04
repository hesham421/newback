package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-SEC-011 body (ENT-SEC-013). The closed decision set is enforced structurally, so an
 * unknown value is a 400 validation failure rather than a catalog error.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Approve or reject a sign-up request - الموافقة على طلب تسجيل أو رفضه")
public class SignupDecisionRequest {

    /** The two values API-SEC-011 accepts. */
    public static final String DECISION_APPROVE = "APPROVE";
    public static final String DECISION_REJECT = "REJECT";

    @NotBlank(message = "{validation.required}")
    @Pattern(regexp = "APPROVE|REJECT", message = "{validation.invalid}")
    @Schema(description = "Decision: APPROVE or REJECT - القرار: موافقة أو رفض", example = "APPROVE")
    private String decision;
}
