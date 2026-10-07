package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/** tenant-maturity D (REQ-SEC-085) — {@code PUT /api/v1/sec/me/password}. Both raw secrets, never logged. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Change my password - تغيير كلمة المرور الخاصة بي")
public class PasswordChangeRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @ToString.Exclude
    @Schema(description = "Current raw password - كلمة المرور الحالية", example = "Old-Passw0rd")
    private String currentPassword;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @ToString.Exclude
    @Schema(description = "New raw password, hashed server-side - كلمة المرور الجديدة", example = "N3wP@ssw0rd1")
    private String newPassword;
}
