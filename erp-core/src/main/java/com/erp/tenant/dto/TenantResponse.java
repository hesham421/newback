package com.erp.tenant.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A tenant, as every platform-tenant endpoint returns it (business fields + audit). The profile and the
 * suspension facts since tenant-maturity B, the logo URL and brand colour since E; the token cut-off is not exposed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tenant - مستأجر")
public class TenantResponse {

    @Schema(description = "Unique identifier - المعرف الفريد", example = "2")
    private Long id;

    @Schema(description = "Tenant code (X-Tenant-Code) - رمز المستأجر", example = "ACME")
    private String code;

    @Schema(description = "Tenant name (Arabic) - اسم المستأجر بالعربية", example = "شركة أكمي")
    private String nameAr;

    @Schema(description = "Tenant name (English) - اسم المستأجر بالإنجليزية", example = "Acme Ltd")
    private String nameEn;

    @Schema(description = "Status: ACTIVE or SUSPENDED - الحالة", example = "ACTIVE")
    private String statusCode;

    @Schema(description = "Contact e-mail - بريد التواصل", example = "contact@acme.example")
    private String contactEmail;

    @Schema(description = "Contact phone - هاتف التواصل", example = "+966 11 555 0100")
    private String contactPhone;

    @Schema(description = "ISO 3166-1 alpha-2 country code - رمز الدولة", example = "SA")
    private String countryCode;

    @Schema(description = "Default UI language: ar or en - اللغة الافتراضية", example = "ar")
    private String defaultLocale;

    @Schema(description = "IANA time-zone id - المنطقة الزمنية", example = "Asia/Riyadh")
    private String timezone;

    @Schema(description = "Operator's notes - ملاحظات", example = "Pilot customer, invoiced yearly")
    private String notes;

    @Schema(description = "When the tenant was suspended; null while ACTIVE - تاريخ التعليق")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant suspendedAt;

    @Schema(description = "Platform operator who suspended the tenant - من علّق المستأجر", example = "admin")
    private String suspendedBy;

    @Schema(description = "Reason of the suspension - سبب التعليق", example = "Unpaid invoice")
    private String suspensionReason;

    @Schema(description = "Public URL of the tenant's logo; null when none (tenant-maturity E) - رابط شعار المستأجر",
        example = "/api/v1/public/files/ACME/3f9c2a7d0b4e4c1a9d8e7f6a5b4c3d2e", nullable = true)
    private String logoUrl;

    @Schema(description = "Brand accent colour #RRGGBB, or null (tenant-maturity E) - لون العلامة", example = "#1A2B3C",
        nullable = true)
    private String brandColor;

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
