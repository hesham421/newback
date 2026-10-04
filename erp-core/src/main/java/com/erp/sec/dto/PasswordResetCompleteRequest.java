package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-SEC-004 completion body. {@code token} is the raw opaque value delivered out-of-band; only
 * its SHA-256 hash is ever persisted or compared (DBF-SEC-093).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Complete a password reset - إتمام إعادة تعيين كلمة المرور")
public class PasswordResetCompleteRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Raw reset token - رمز إعادة التعيين", example = "0f6c2c2e-6f0e-4f6b-9a6f-2c8b9a1d7e30")
    private String token;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "New raw password - كلمة المرور الجديدة", example = "N3wP@ssw0rd!")
    private String newPassword;
}
