package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * API-SEC-006 create-user body (ENT-SEC-001). {@code password} is the raw secret, hashed
 * server-side and never stored or returned; {@code statusCode} is absent because a directly
 * created user always starts ACTIVE (DATA-DOM-MASTER.md ENT-SEC-001 DTO MEMBERSHIP).
 * {@code roleIds} is optional — omitting it creates a user with no roles, exactly as before.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Create a user - إنشاء مستخدم")
public class UserCreateRequest {

    @NotBlank(message = "{validation.required}")
    @Size(max = 100, message = "{validation.size}")
    @Schema(description = "Login identity, immutable after create - اسم الدخول، غير قابل للتعديل", example = "u2")
    private String username;

    @NotBlank(message = "{validation.required}")
    @Email(message = "{validation.invalid}")
    @Size(max = 255, message = "{validation.size}")
    @Schema(description = "Email address - البريد الإلكتروني", example = "u2@example.com")
    private String email;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Full name (Arabic) - الاسم الكامل بالعربية", example = "أحمد علي")
    private String fullNameAr;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Full name (English) - الاسم الكامل بالإنجليزية", example = "Ahmed Ali")
    private String fullNameEn;

    @NotBlank(message = "{validation.required}")
    @Size(max = 200, message = "{validation.size}")
    @Schema(description = "Raw password, hashed server-side - كلمة المرور، تُجزَّأ في الخادم", example = "N3wP@ssw0rd!")
    private String password;

    @Schema(description = "Optional role identifiers to assign at creation; omitted or empty creates the user with no roles"
        + " - معرّفات الأدوار المطلوب إسنادها عند الإنشاء، اختيارية", example = "[1, 2]")
    private List<Long> roleIds;
}
