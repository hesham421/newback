package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** erp-core step 06 — customer e-mail verification body: the raw token from the verification e-mail. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Verify a customer e-mail address - تأكيد البريد الإلكتروني للعميل")
public class CustomerVerifyRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Raw verification token - رمز التأكيد", example = "0f6c2c2e-6f0e-4f6b-9a6f-2c8b9a1d7e30")
    private String token;
}
