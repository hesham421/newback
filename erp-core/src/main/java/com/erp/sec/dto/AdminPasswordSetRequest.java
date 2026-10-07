package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * tenant-maturity D (REQ-SEC-083) — {@code PUT /api/v1/sec/users/{id}/password}. {@code newPassword} is
 * the raw secret (RULE-SEC-056), hashed server-side and never logged; a null
 * {@code requireChangeAtNextLogin} means TRUE (RULE-SEC-058).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Set a staff user's password - تعيين كلمة مرور مستخدم")
public class AdminPasswordSetRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @ToString.Exclude
    @Schema(description = "New raw password, hashed server-side - كلمة المرور الجديدة", example = "N3wP@ssw0rd1")
    private String newPassword;

    @Schema(description = "Whether the user must change the password at the next sign-in (default true)"
        + " - إلزام المستخدم بتغيير كلمة المرور عند الدخول التالي (افتراضيًا نعم)", example = "true")
    private Boolean requireChangeAtNextLogin;
}
