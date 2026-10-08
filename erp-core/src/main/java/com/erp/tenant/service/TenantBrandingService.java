package com.erp.tenant.service;

import com.erp.common.domain.status.ServiceResult;
import com.erp.common.domain.status.Status;
import com.erp.common.exception.LocalizedException;
import com.erp.tenant.TenantContext;
import com.erp.tenant.domain.TenantDomain;
import com.erp.tenant.dto.TenantBrandingResponse;
import com.erp.tenant.entity.Tenant;
import com.erp.tenant.exception.TenantErrorCodes;
import com.erp.tenant.mapper.TenantMapper;
import com.erp.tenant.repository.TenantRepository;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * tenant-maturity E — the read side of a tenant's branding: {@code GET /api/v1/tenant/me} (the token's tenant, any realm,
 * REQ-TENANT-031) and {@code GET /api/v1/public/tenants/{tenantCode}/branding} (anonymous, the path's tenant,
 * REQ-TENANT-032). Read-only, no permission (ADR-TENANT-005: every user of a tenant reads its branding); the writes are
 * {@link TenantService}'s, platform-only. No caching ({@code CORE_TENANT} is not on the caching approved-register).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantBrandingService {

    private final TenantRepository repository;
    private final TenantMapper mapper;
    private final TenantLogoUrls logoUrls;

    /** REQ-TENANT-031 — the branding of the caller's own tenant (the token's {@code tid}); suspended → 403. */
    @Transactional(readOnly = true)
    @PreAuthorize("isAuthenticated()")
    public ServiceResult<TenantBrandingResponse> getMyTenantBranding() {
        Long tenantId = TenantContext.require();
        log.debug("Fetching the branding of the caller's tenant ID: {}", tenantId);

        Tenant tenant = repository.findById(tenantId)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, TenantErrorCodes.TENANT_NOT_FOUND, tenantId));
        TenantDomain.from(tenant).assertServed();

        return ServiceResult.success(mapper.toBrandingResponse(tenant, logoUrls.of(tenant)));
    }

    /**
     * REQ-TENANT-032 — the branding of the tenant named in the path (trimmed, upper-cased like the tenant filter's
     * lookup); unknown → 404, suspended → 403. The filter has already resolved the same tenant and rate-limited the caller.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("permitAll()")
    public ServiceResult<TenantBrandingResponse> getPublicTenantBranding(String tenantCode) {
        String code = tenantCode == null ? "" : tenantCode.strip().toUpperCase(Locale.ROOT);
        log.debug("Fetching the public branding of tenant code: {}", code);

        Tenant tenant = repository.findByCode(code)
            .orElseThrow(() -> new LocalizedException(Status.NOT_FOUND, TenantErrorCodes.TENANT_NOT_FOUND, code));
        TenantDomain.from(tenant).assertServed();

        return ServiceResult.success(mapper.toBrandingResponse(tenant, logoUrls.of(tenant)));
    }
}
