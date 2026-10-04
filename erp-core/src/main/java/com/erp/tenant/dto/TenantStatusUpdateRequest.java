package com.erp.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** {@code PATCH /api/v1/platform/tenants/{id}/status} body: activate or suspend a tenant. */
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
}
