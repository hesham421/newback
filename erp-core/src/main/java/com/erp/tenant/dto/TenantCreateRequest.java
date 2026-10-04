package com.erp.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * {@code POST /api/v1/platform/tenants} body: the tenant plus its first administrator. The code's
 * format ({@code ^[A-Z0-9_]{3,32}$}) is a business rule checked by {@code TenantDomain}
 * ({@code TENANT_CODE_INVALID}), not a bean-validation constraint. {@code adminPassword} is the raw
 * secret, hashed server-side and never stored or returned.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create a tenant with its first administrator - إنشاء مستأجر مع أول مدير له")
public class TenantCreateRequest {

    @NotBlank(message = "{validation.required}")
    @Schema(description = "Tenant code, ^[A-Z0-9_]{3,32}$, immutable - رمز المستأجر، غير قابل للتعديل", example = "ACME")
    private String code;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Tenant name (Arabic) - اسم المستأجر بالعربية", example = "شركة أكمي")
    private String nameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Tenant name (English) - اسم المستأجر بالإنجليزية", example = "Acme Ltd")
    private String nameEn;

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    @Schema(description = "First administrator's username (unique within the tenant) - اسم دخول أول مدير", example = "admin")
    private String adminUsername;

    @NotBlank(message = "{validation.required}")
    @Email(message = "{validation.invalid}")
    @Size(max = 255, message = "{validation.size}")
    @Schema(description = "First administrator's email - البريد الإلكتروني لأول مدير", example = "admin@acme.example")
    private String adminEmail;

    @NotBlank(message = "{validation.required}")
    @Size(min = 8, max = 200, message = "{validation.size}")
    @Schema(description = "First administrator's raw password, hashed server-side - كلمة مرور أول مدير", example = "N3wP@ssw0rd!")
    private String adminPassword;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "First administrator's full name (Arabic) - الاسم الكامل لأول مدير بالعربية", example = "مدير أكمي")
    private String adminFullNameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "First administrator's full name (English) - الاسم الكامل لأول مدير بالإنجليزية", example = "Acme Administrator")
    private String adminFullNameEn;

    /** Never print the password (Lombok's {@code @Data} would). */
    @Override
    public String toString() {
        return "TenantCreateRequest[code=" + code + ", adminUsername=" + adminUsername + "]";
    }
}
