package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * erp-core step 06 — customer self-registration body ({@code POST /api/v1/public/customers/register},
 * tenant from {@code X-Tenant-Code}). The e-mail is also the customer's login; {@code fullName} fills
 * both name columns (a storefront form asks for one name; {@code PATCH /api/v1/customers/me} edits each).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer self-registration - تسجيل عميل جديد")
public class CustomerRegisterRequest {

    @NotBlank(message = "{validation.required}")
    @Email(message = "{validation.invalid}")
    @Size(max = 100, message = "{validation.size}")
    @Schema(description = "E-mail address, also the login - البريد الإلكتروني (اسم الدخول)", example = "jane@example.com")
    private String email;

    @NotBlank(message = "{validation.required}")
    @Size(min = 8, max = 200, message = "{validation.size}")
    @Schema(description = "Raw password, hashed server-side - كلمة المرور", example = "N3wP@ssw0rd!")
    private String password;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Full name - الاسم الكامل", example = "Jane Doe")
    private String fullName;

    /** Never log the password. */
    @Override
    public String toString() {
        return "CustomerRegisterRequest(email=" + email + ", fullName=" + fullName + ")";
    }
}
