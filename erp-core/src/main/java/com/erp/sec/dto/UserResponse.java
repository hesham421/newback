package com.erp.sec.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * ENT-SEC-001 response — every field except {@code passwordHash}, which is never serialized, and
 * {@code photoFileId}, which is exposed as {@code photoUrl} (tenant-maturity D)
 * (POL-SEC-004). {@code roles} is populated on every user-shaped response; a user holding no
 * roles carries an empty array, never a missing key.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User - مستخدم")
public class UserResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "1")
    private Long userPk;

    @Schema(description = "Login identity - اسم الدخول", example = "u2")
    private String username;

    @Schema(description = "Email address - البريد الإلكتروني", example = "u2@example.com")
    private String email;

    @Schema(description = "Full name (Arabic) - الاسم الكامل بالعربية", example = "أحمد علي")
    private String fullNameAr;

    @Schema(description = "Full name (English) - الاسم الكامل بالإنجليزية", example = "Ahmed Ali")
    private String fullNameEn;

    @Schema(description = "USER_STATUS code - رمز الحالة", example = "ACTIVE")
    private String statusCode;

    @Schema(description = "Auth realm: STAFF or CUSTOMER - نطاق المصادقة", example = "STAFF")
    private String realm;

    @Schema(description = "Last login timestamp - تاريخ آخر دخول")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant lastLoginAt;

    @Schema(description = "Active status - حالة التفعيل", example = "true")
    private Boolean isActiveFl;

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

    @Schema(description = "Whether the user must change the password at the next sign-in - هل يلزم المستخدم تغيير كلمة المرور", example = "false")
    private Boolean passwordChangeRequired;

    @Schema(description = "When a person last set the password - وقت آخر تعيين لكلمة المرور")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant passwordChangedAt;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    @Schema(description = "Assigned roles; empty when the user holds none - الأدوار المُسندة، ومصفوفة فارغة إن لم يحمل المستخدم أي دور")
    private List<RoleSummaryResponse> roles;

    @Schema(description = "Created timestamp - تاريخ الإنشاء")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    @Schema(description = "Created by - أنشئ بواسطة", example = "admin")
    private String createdBy;

    @Schema(description = "Updated timestamp - تاريخ التحديث")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;

    @Schema(description = "Updated by - حُدّث بواسطة", example = "admin")
    private String updatedBy;
}
