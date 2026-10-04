package com.erp.tenant.crossmodule;

import com.erp.tenant.entity.Tenant;
import com.erp.tenant.repository.TenantRepository;
import java.util.Optional;
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
}
