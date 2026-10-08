package com.erp.tenant.controller;

import com.erp.common.web.ApiResponse;
import com.erp.common.web.OperationCode;
import com.erp.tenant.dto.TenantBrandingResponse;
import com.erp.tenant.service.TenantBrandingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * tenant-maturity E — a tenant's branding for the frontend: the shell's {@code GET /api/v1/tenant/me} (authenticated,
 * either realm; realm-neutral on the core chain) and the login page's anonymous {@code GET
 * /api/v1/public/tenants/{tenantCode}/branding} (customer chain, tenant from the path, rate-limited per client address).
 * Read-only; the writes are on {@link PlatformTenantController}. Pure delegation — zero business logic.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Tag(name = "Tenant Branding", description = "A tenant's logo, brand colour and names for the UI - العلامة التجارية للمستأجر")
public class TenantBrandingController {

    private final TenantBrandingService service;
    private final OperationCode operationCode;

    @GetMapping("/tenant/me")
    @Operation(summary = "Get the branding of my tenant",
        description = "The tenant of the caller's token, staff or customer: code, names, logo URL, brand colour and default"
            + " language - علامة مستأجري")
    public ResponseEntity<ApiResponse<TenantBrandingResponse>> getMyTenantBranding() {
        return operationCode.craftResponse(service.getMyTenantBranding());
    }

    @GetMapping("/public/tenants/{tenantCode}/branding")
    @Operation(summary = "Get a tenant's public branding by its code",
        description = "No token, no header: the tenant comes from the path (unknown 404 TENANT_NOT_FOUND, suspended 403"
            + " TENANT_SUSPENDED); at most erp.core.tenant.public-branding-rate-limit.capacity calls per period and client"
            + " address (IPv6 by /64), else 429 TENANT_BRANDING_RATE_LIMITED with Retry-After - العلامة العامة للمستأجر برمزه")
    public ResponseEntity<ApiResponse<TenantBrandingResponse>> getPublicTenantBranding(@PathVariable String tenantCode) {
        return operationCode.craftResponse(service.getPublicTenantBranding(tenantCode));
    }
}
