package com.erp.tenant;

/**
 * What a {@link TenantProvisioningContributor} needs to set up a newly created tenant: the new
 * tenant, the tenant whose reference catalog it starts from (always PLATFORM today), the principal
 * performing the provisioning (for the audit columns) and the tenant's first administrator.
 *
 * @param tenantId       {@code CORE_TENANT.ID} of the new tenant
 * @param tenantCode     its code
 * @param sourceTenantId tenant whose reference data (roles, lookups, templates, ...) is copied
 * @param provisionedBy  username of the platform operator (audit {@code CREATED_BY})
 * @param admin          the new tenant's first administrator account
 */
public record TenantProvisioning(Long tenantId, String tenantCode, Long sourceTenantId,
                                 String provisionedBy, Administrator admin) {

    /**
     * The first administrator of the new tenant. {@code rawPassword} is hashed by the contributor
     * that creates the account and is never stored or logged in clear ({@link #toString()} omits it).
     */
    public record Administrator(String username, String email, String rawPassword,
                                String fullNameAr, String fullNameEn) {

        @Override
        public String toString() {
            return "Administrator[username=" + username + ", email=" + email + "]";
        }
    }
}
