package com.erp.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * {@code PATCH /api/v1/platform/tenants/{id}/status} body: activate or suspend a tenant. Since tenant-maturity B
 * a suspension carries a {@code reason} of 3..500 characters (decided by {@code TenantDomain}, RULE-TENANT-016);
 * an activation ignores it.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Change a tenant's status - تغيير حالة المستأجر")
public class TenantStatusUpdateRequest {

    @NotBlank(message = "{validation.required}")
    @Pattern(regexp = "ACTIVE|SUSPENDED", message = "{validation.pattern}")
    @Schema(description = "Target status: ACTIVE or SUSPENDED - الحالة المطلوبة", example = "SUSPENDED")
    private String statusCode;

    @Schema(description = "Reason of a suspension, 3 to 500 characters: required for SUSPENDED, ignored for ACTIVE"
        + " - سبب التعليق، مطلوب عند التعليق", example = "Unpaid invoice")
    private String reason;
}
