package com.erp.tenant;

/**
 * SPI through which each core module sets up its own data for a newly created tenant, so that the
 * tenant module never reaches into another module's tables or internals. Implementations are Spring
 * beans in their own module: SEC copies the role catalog and creates the first administrator, MDL
 * copies the lookup catalog, NOTIF copies the channel configuration and the templates.
 *
 * <p>Contract:
 * <ul>
 *   <li>{@code TenantService} calls every contributor, ordered by {@link #order()}, inside the same
 *       database transaction that inserted the {@code CORE_TENANT} row, so provisioning is atomic:
 *       any failure rolls the tenant back too.</li>
 *   <li>The Hibernate session of that transaction belongs to the calling (PLATFORM) tenant, so a
 *       contributor must not write the new tenant's rows through tenant-aware JPA entities. It writes
 *       them with explicit SQL that names {@code TENANT_ID} in every statement (the new tenant on
 *       inserts, {@link TenantProvisioning#sourceTenantId()} on reads).</li>
 *   <li>A contributor is platform code reached only from the {@code PLATFORM_TENANT_MANAGE}-gated
 *       provisioning API; it is not itself an HTTP-reachable service.</li>
 * </ul>
 */
public interface TenantProvisioningContributor {

    /** Lower runs first. */
    default int order() {
        return 0;
    }

    /** Sets up this module's data for {@code provisioning.tenantId()}. */
    void provision(TenantProvisioning provisioning);
}
