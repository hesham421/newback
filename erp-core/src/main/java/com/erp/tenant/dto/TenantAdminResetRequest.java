package com.erp.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * tenant-maturity B (REQ-TENANT-027) — {@code POST /api/v1/platform/tenants/{id}/admin-reset}. {@code newPassword}
 * is the raw secret (SEC RULE-SEC-056), hashed server-side and never logged; a null
 * {@code requireChangeAtNextLogin} means TRUE (SEC RULE-SEC-058, ADR-SEC-063).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Reset a tenant administrator's password - إعادة تعيين كلمة مرور مدير المستأجر")
public class TenantAdminResetRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    @Schema(description = "Username of a staff user of the tenant holding a super role - اسم دخول مدير المستأجر",
        example = "admin")
    private String username;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @ToString.Exclude
    @Schema(description = "New raw password, hashed server-side - كلمة المرور الجديدة", example = "N3wP@ssw0rd1")
    private String newPassword;

    @Schema(description = "Whether the user must change the password at the next sign-in (default true)"
        + " - إلزام المستخدم بتغيير كلمة المرور عند الدخول التالي (افتراضيًا نعم)", example = "true")
    private Boolean requireChangeAtNextLogin;
}
