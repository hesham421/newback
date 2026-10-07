package com.erp.sec.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** tenant-maturity D — the caller's tenant inside {@link StaffProfileResponse}. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "The user's tenant - مستأجر المستخدم")
public class StaffProfileTenantResponse {

    @Schema(description = "Tenant code - رمز المستأجر", example = "ACME")
    private String code;

    @Schema(description = "Tenant name (Arabic) - اسم المستأجر بالعربية", example = "أكمي")
    private String nameAr;

    @Schema(description = "Tenant name (English) - اسم المستأجر بالإنجليزية", example = "Acme")
    private String nameEn;
}
