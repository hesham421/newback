package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** erp-core step 06 — customer login body (tenant from {@code X-Tenant-Code}). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer login credentials - بيانات دخول العميل")
public class CustomerLoginRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    @Schema(description = "E-mail address (the customer login) - البريد الإلكتروني", example = "jane@example.com")
    private String email;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Raw password - كلمة المرور", example = "N3wP@ssw0rd!")
    private String password;

    /** Never log the password. */
    @Override
    public String toString() {
        return "CustomerLoginRequest(email=" + email + ")";
    }
}
