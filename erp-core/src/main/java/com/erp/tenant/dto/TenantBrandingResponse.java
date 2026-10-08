package com.erp.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A tenant's branding (tenant-maturity E, REQ-TENANT-031/032): what the shell ({@code GET /api/v1/tenant/me}) and the
 * login page ({@code GET /api/v1/public/tenants/{tenantCode}/branding}) show. Read-only and deliberately narrow: no id,
 * status, contact, profile or audit field.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Tenant branding - العلامة التجارية للمستأجر")
public class TenantBrandingResponse {

    @Schema(description = "Tenant code - رمز المستأجر", example = "ACME")
    private String code;

    @Schema(description = "Tenant name (Arabic) - اسم المستأجر بالعربية", example = "شركة أكمي")
    private String nameAr;

    @Schema(description = "Tenant name (English) - اسم المستأجر بالإنجليزية", example = "Acme Ltd")
    private String nameEn;

    @Schema(description = "Public URL of the logo; null when the tenant has none (show the platform mark alone) - رابط الشعار",
        example = "/api/v1/public/files/ACME/3f9c2a7d0b4e4c1a9d8e7f6a5b4c3d2e", nullable = true)
    private String logoUrl;

    @Schema(description = "Accent colour #RRGGBB, or null - لون العلامة", example = "#1A2B3C", nullable = true)
    private String brandColor;

    @Schema(description = "Default UI language: ar or en, or null - اللغة الافتراضية", example = "ar", nullable = true)
    private String defaultLocale;
}
