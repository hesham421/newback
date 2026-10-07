package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * API-SEC-006 create-user body (ENT-SEC-001). {@code password} is the raw secret, hashed
 * server-side and never stored or returned; {@code statusCode} is absent because a directly
 * created user always starts ACTIVE (DATA-DOM-MASTER.md ENT-SEC-001 DTO MEMBERSHIP).
 * {@code roleIds} is optional — omitting it creates a user with no roles, exactly as before.
 * tenant-maturity D: optional profile fields, and the password must meet RULE-SEC-056.
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
    @ToString.Exclude
    private String password;

    @Schema(description = "Optional role identifiers to assign at creation; omitted or empty creates the user with no roles"
        + " - معرّفات الأدوار المطلوب إسنادها عند الإنشاء، اختيارية", example = "[1, 2]")
    private List<Long> roleIds;

    @Size(max = 30, message = "{validation.size}")
    @Pattern(regexp = StaffProfileConstraints.PHONE_PATTERN, message = "{validation.invalid}")
    @Schema(description = "Phone, E.164-ish (tenant-maturity D) - الهاتف", example = "+966 50 123 4567")
    private String phone;

    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Job title (Arabic) - المسمى الوظيفي بالعربية", example = "محاسب")
    private String jobTitleAr;

    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Job title (English) - المسمى الوظيفي بالإنجليزية", example = "Accountant")
    private String jobTitleEn;

    @Pattern(regexp = StaffProfileConstraints.LOCALE_PATTERN, message = "{validation.invalid}")
    @Schema(description = "Preferred language: ar or en - اللغة المفضلة", example = "ar")
    private String preferredLocale;

    @Schema(description = "Whether the new user must change the password at the first sign-in; null means true"
        + " (RULE-SEC-058) - إلزام المستخدم بتغيير كلمة المرور عند أول دخول", example = "true")
    private Boolean requireChangeAtNextLogin;
}
