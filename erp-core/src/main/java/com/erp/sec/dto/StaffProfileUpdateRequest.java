package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * tenant-maturity D (REQ-SEC-086) — {@code PATCH /api/v1/sec/me}. A null field is left unchanged; an empty
 * string clears {@code phone}, the job titles and {@code preferredLocale}; the names cannot be blanked.
 * E-mail and username are administrator-only ({@code PUT /api/v1/sec/users/{id}}).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Update my staff profile - تحديث ملفي الشخصي")
public class StaffProfileUpdateRequest {

    @Size(max = 200, message = "{validation.size}")
    @Pattern(regexp = StaffProfileConstraints.NOT_BLANK_PATTERN, message = "{validation.required}")
    @Schema(description = "Full name (Arabic) - الاسم الكامل بالعربية", example = "أحمد علي")
    private String fullNameAr;

    @Size(max = 200, message = "{validation.size}")
    @Pattern(regexp = StaffProfileConstraints.NOT_BLANK_PATTERN, message = "{validation.required}")
    @Schema(description = "Full name (English) - الاسم الكامل بالإنجليزية", example = "Ahmed Ali")
    private String fullNameEn;

    @Size(max = 30, message = "{validation.size}")
    @Pattern(regexp = StaffProfileConstraints.PHONE_PATTERN, message = "{validation.invalid}")
    @Schema(description = "Phone, E.164-ish; empty clears it - الهاتف", example = "+966 50 123 4567")
    private String phone;

    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Job title (Arabic); empty clears it - المسمى الوظيفي بالعربية", example = "محاسب")
    private String jobTitleAr;

    @Size(max = 150, message = "{validation.size}")
    @Schema(description = "Job title (English); empty clears it - المسمى الوظيفي بالإنجليزية", example = "Accountant")
    private String jobTitleEn;

    @Pattern(regexp = StaffProfileConstraints.LOCALE_PATTERN, message = "{validation.invalid}")
    @Schema(description = "Preferred language: ar or en; empty clears it - اللغة المفضلة", example = "ar")
    private String preferredLocale;
}
