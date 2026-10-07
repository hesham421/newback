package com.erp.tenant.crossmodule;

import java.util.Optional;

/**
 * The tenant module's read-only cross-module surface (erp-core step 07): lets another module turn a
 * tenant id (e.g. {@code TenantContext.require()}) into the tenant's code, for example to build a
 * shareable URL that carries the tenant in its path. Returns plain values only, never the entity.
 */
public interface TenantLookupApi {

    /** The code of the tenant with this id, or empty when no such tenant exists. */
    Optional<String> codeOf(Long tenantId);

    /**
     * tenant-maturity D — the code and both names of the tenant with this id (e.g. SEC's staff
     * {@code /me}), or empty when no such tenant exists.
     */
    Optional<TenantSummary> summaryOf(Long tenantId);
}
