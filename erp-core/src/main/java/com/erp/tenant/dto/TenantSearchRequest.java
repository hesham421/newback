package com.erp.tenant.dto;

import com.erp.common.dto.BaseSearchContractRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * {@code POST /api/v1/platform/tenants/search} body. Tenants are a flat root entity: filters and
 * sorting flow through the inherited generic contract; the whitelist lives in {@code TenantService}.
 */
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@SuperBuilder
@Schema(description = "Search request for tenants - طلب بحث المستأجرين")
public class TenantSearchRequest extends BaseSearchContractRequest {
}
