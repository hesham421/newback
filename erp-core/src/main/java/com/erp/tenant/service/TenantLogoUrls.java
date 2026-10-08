package com.erp.tenant.service;

import com.erp.file.crossmodule.FileDocumentLookupApi;
import com.erp.tenant.TenantContext;
import com.erp.tenant.entity.Tenant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * tenant-maturity E (XM-TENANT-003) — turns {@code CORE_TENANT.LOGO_FILE_ID} into the public {@code logoUrl} through
 * FILE's {@link FileDocumentLookupApi}. The document lives in the tenant's own rows and FILE's lookup reads the current
 * tenant only, so another tenant's logo is resolved inside {@code TenantContext.callAs(id)} in a new read-only
 * transaction (a caller's open transaction is bound to its own tenant). A logo no longer servable yields null.
 */
@Component
@RequiredArgsConstructor
public class TenantLogoUrls {

    private final FileDocumentLookupApi fileDocumentLookupApi;
    private final PlatformTransactionManager transactionManager;

    /** The logo URL of {@code tenant}, or {@code null}. */
    public String of(Tenant tenant) {
        if (tenant == null || tenant.getLogoFileId() == null) {
            return null;
        }
        Long logoId = tenant.getLogoFileId();
        if (tenant.getId().equals(TenantContext.current())) {
            return fileDocumentLookupApi.publicUrl(logoId).orElse(null);
        }
        // read-only, REQUIRES_NEW: a fresh Hibernate session bound to the logo's tenant, never the caller's
        TransactionTemplate readInTenant = new TransactionTemplate(transactionManager);
        readInTenant.setReadOnly(true);
        readInTenant.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return TenantContext.callAs(tenant.getId(),
            () -> readInTenant.execute(status -> fileDocumentLookupApi.publicUrl(logoId).orElse(null)));
    }

    /** Logo URLs keyed by tenant id; tenants without a servable logo are absent (one lookup per tenant with a logo). */
    public Map<Long, String> of(Collection<Tenant> tenants) {
        Map<Long, String> urls = new HashMap<>();
        for (Tenant tenant : tenants) {
            String url = of(tenant);
            if (url != null) {
                urls.put(tenant.getId(), url);
            }
        }
        return urls;
    }
}
