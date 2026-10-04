package com.erp.sec.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** erp-core step 06 — a customer account as the customer sees it. Never the password hash. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Customer profile - الملف الشخصي للعميل")
public class CustomerProfileResponse {

    @Schema(description = "Account id - معرف الحساب", example = "42")
    private Long id;

    @Schema(description = "E-mail address (the login) - البريد الإلكتروني", example = "jane@example.com")
    private String email;

    @Schema(description = "Full name (Arabic) - الاسم الكامل بالعربية", example = "جين دو")
    private String fullNameAr;

    @Schema(description = "Full name (English) - الاسم الكامل بالإنجليزية", example = "Jane Doe")
    private String fullNameEn;

    @Schema(description = "PENDING_VERIFICATION, ACTIVE or DISABLED - حالة الحساب", example = "ACTIVE")
    private String statusCode;

    @Schema(description = "Always CUSTOMER - نطاق المصادقة", example = "CUSTOMER")
    private String realm;

    @Schema(description = "Last login - آخر دخول")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant lastLoginAt;

    @Schema(description = "Created timestamp - تاريخ الإنشاء")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant createdAt;

    @Schema(description = "Created by - أنشئ بواسطة", example = "system")
    private String createdBy;

    @Schema(description = "Updated timestamp - تاريخ التحديث")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant updatedAt;

    @Schema(description = "Updated by - حُدّث بواسطة", example = "jane@example.com")
    private String updatedBy;
}
