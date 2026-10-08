package com.erp.sec.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tenant-maturity D (REQ-SEC-086) — {@code GET/PATCH /api/v1/sec/me}: the STAFF caller's own account and
 * profile. Deliberately no roles and no permissions (ADR-SEC-064; the menu is the client's authority).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "My staff profile - ملفي الشخصي")
public class StaffProfileResponse {

    @Schema(description = "User identifier - معرّف المستخدم", example = "12")
    private Long userPk;

    @Schema(description = "Login identity - اسم الدخول", example = "u2")
    private String username;

    @Schema(description = "Email address - البريد الإلكتروني", example = "u2@example.com")
    private String email;

    @Schema(description = "Full name (Arabic) - الاسم الكامل بالعربية", example = "أحمد علي")
    private String fullNameAr;

    @Schema(description = "Full name (English) - الاسم الكامل بالإنجليزية", example = "Ahmed Ali")
    private String fullNameEn;

    @Schema(description = "Phone - الهاتف", example = "+966 50 123 4567")
    private String phone;

    @Schema(description = "Job title (Arabic) - المسمى الوظيفي بالعربية", example = "محاسب")
    private String jobTitleAr;

    @Schema(description = "Job title (English) - المسمى الوظيفي بالإنجليزية", example = "Accountant")
    private String jobTitleEn;

    @Schema(description = "Preferred language: ar or en - اللغة المفضلة", example = "ar")
    private String preferredLocale;

    @Schema(description = "Public URL of the photo, null without one - الرابط العام للصورة",
        example = "/api/v1/public/files/ACME/3q2-7wEjK9mZ0aBcDeFgHiJkLmNoPqRs")
    private String photoUrl;

    @Schema(description = "Whether the password must be changed before anything else - هل يلزم تغيير كلمة المرور", example = "false")
    private Boolean passwordChangeRequired;

    @Schema(description = "Last login timestamp - تاريخ آخر دخول")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant lastLoginAt;

    @Schema(description = "The user's tenant - مستأجر المستخدم")
    private StaffProfileTenantResponse tenant;
}
