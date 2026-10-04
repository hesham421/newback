package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-SEC-001 login body. {@code password} is the raw secret, verified against the stored hash and
 * never stored, logged or echoed back (POL-SEC-004).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login credentials - بيانات تسجيل الدخول")
public class LoginRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    @Schema(description = "Login identity - اسم الدخول", example = "u1")
    private String username;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Raw password - كلمة المرور", example = "N3wP@ssw0rd!")
    private String password;
}
