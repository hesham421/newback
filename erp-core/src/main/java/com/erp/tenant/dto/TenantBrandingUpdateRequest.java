package com.erp.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * {@code PATCH /api/v1/platform/tenants/{id}/branding} body (tenant-maturity E, REQ-TENANT-030): the brand colour.
 * Its format is decided by {@code TenantDomain} (RULE-TENANT-021, 400 {@code TENANT_BRAND_COLOR_INVALID}), not by
 * bean validation; null, absent or blank clears the colour.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Set or clear a tenant's brand colour - ضبط لون علامة المستأجر أو مسحه")
public class TenantBrandingUpdateRequest {

    @Schema(description = "Accent colour #RRGGBB (stored upper case); null or blank clears it - لون العلامة",
        example = "#1A2B3C", nullable = true)
    private String brandColor;
}
