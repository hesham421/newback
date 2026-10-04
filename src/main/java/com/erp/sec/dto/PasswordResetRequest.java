package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** API-SEC-003 request body. Whether the address resolves to a user is never revealed. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request a password reset - طلب إعادة تعيين كلمة المرور")
public class PasswordResetRequest {

    @NotBlank(message = "{validation.required}")
    @Email(message = "{validation.invalid}")
    @Size(max = 255, message = "{validation.size}")
    @Schema(description = "Email address - البريد الإلكتروني", example = "u1@example.com")
    private String email;
}
