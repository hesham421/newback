package com.erp.tenant.crossmodule;

import com.erp.tenant.TenantConstants;
import com.erp.tenant.TenantContext;
import com.erp.tenant.domain.TenantDomain;
import com.erp.tenant.entity.Tenant;
import com.erp.tenant.repository.TenantRepository;
import java.util.Optional;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link TenantLookupApi} over the tenant repository. No {@code @PreAuthorize}: a tenant code is not
 * secret (it travels in the {@code X-Tenant-Code} header and in public URLs), and the consuming
 * service carries its own gate. {@code CORE_TENANT} is global, so the read is not tenant-filtered.
 */
@Component
@RequiredArgsConstructor
public class TenantLookupApiImpl implements TenantLookupApi {

    private final TenantRepository repository;

    @Override
    @Transactional(readOnly = true)
    public Optional<String> codeOf(Long tenantId) {
        return tenantId == null ? Optional.empty() : repository.findById(tenantId).map(Tenant::getCode);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TenantSummary> summaryOf(Long tenantId) {
        return tenantId == null ? Optional.empty() : repository.findById(tenantId)
            .map(tenant -> new TenantSummary(tenant.getId(), tenant.getCode(), tenant.getNameAr(), tenant.getNameEn()));
    }

    /**
     * Not {@code @Transactional}: without a current tenant (a job) the read runs as PLATFORM, like
     * {@code TenantResolutionFilter}'s lookups; inside a tenant it joins the caller's session ({@code CORE_TENANT} is global).
     */
    @Override
    public boolean isActive(Long tenantId) {
        if (tenantId == null) {
            return false;
        }
        Supplier<Optional<Tenant>> lookup = () -> repository.findById(tenantId);
        Optional<Tenant> tenant = TenantContext.current() != null
            ? lookup.get() : TenantContext.callAs(TenantConstants.PLATFORM_TENANT_ID, lookup);
        return tenant.map(found -> TenantDomain.from(found).isActive()).orElse(false);
    }
}
