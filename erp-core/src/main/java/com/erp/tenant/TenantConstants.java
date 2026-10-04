package com.erp.tenant;

/** Fixed tenant values shared by the security chain, the provisioning API and the seed (V10). */
public final class TenantConstants {

    private TenantConstants() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** {@code CORE_TENANT.ID} of the PLATFORM tenant (seeded by V10; owns the bootstrap admin). */
    public static final long PLATFORM_TENANT_ID = 1L;

    /** {@code CORE_TENANT.CODE} of the PLATFORM tenant. */
    public static final String PLATFORM_TENANT_CODE = "PLATFORM";

    /** Request header carrying the tenant code on unauthenticated requests (login, sign-up, reset). */
    public static final String TENANT_CODE_HEADER = "X-Tenant-Code";

    /** JWT claim carrying the tenant id of an access token. */
    public static final String TENANT_ID_CLAIM = "tid";

    /** Tenant status code ({@code CHK_CORE_TENANT_STATUS}): the tenant may log in and call the API. */
    public static final String STATUS_ACTIVE = "ACTIVE";

    /** Tenant status code ({@code CHK_CORE_TENANT_STATUS}): every request of the tenant is refused (403). */
    public static final String STATUS_SUSPENDED = "SUSPENDED";
}
