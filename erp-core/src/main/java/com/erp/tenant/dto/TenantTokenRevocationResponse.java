package com.erp.tenant.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** tenant-maturity C12 (REQ-TENANT-035) — the result of revoke-tokens: never the cut-off instant itself. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Result of revoking a tenant's tokens - نتيجة إبطال رموز المستأجر")
public class TenantTokenRevocationResponse {

    @Schema(description = "Tenant ID - معرّف المستأجر", example = "2")
    private Long id;

    @Schema(description = "Tenant code - رمز المستأجر", example = "ACME")
    private String code;

    @Schema(description = "Number of the tenant's sessions that were terminated - عدد الجلسات المنتهية", example = "3")
    private int sessionsTerminated;
}
