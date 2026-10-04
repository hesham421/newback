package com.erp.tenant.permission;

import com.erp.sec.permission.PermissionContributor;
import com.erp.sec.permission.PermissionDef;
import com.erp.sec.permission.PermissionModule;
import com.erp.sec.permission.PermissionScreen;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The tenant module's permission catalog (erp-core step 06) — the {@code PLATFORM} module rows
 * {@code V10__tenant_schema.sql} seeded, with the same names. {@code PLATFORM_TENANT_MANAGE} is the
 * step-05 literal (not {@code PERM_<PAGE>_<ACTION>}); {@code PERM_PLATFORM_TENANTS_VIEW} is the screen's
 * gateway action RULE-SEC-007 needs. These permissions are effective only inside the PLATFORM tenant
 * (tenant provisioning never copies their grants; super roles of other tenants do not receive them).
 */
@Component
public class TenantPermissions implements PermissionContributor {

    public static final String MODULE = "PLATFORM";

    /** Gateway (VIEW) action of screen PLATFORM_TENANTS. */
    public static final String PERM_PLATFORM_TENANTS_VIEW = "PERM_PLATFORM_TENANTS_VIEW";

    /** Every {@code /api/v1/platform/tenants} operation (TenantService). */
    public static final String PLATFORM_TENANT_MANAGE = "PLATFORM_TENANT_MANAGE";

    private static final PermissionScreen TENANTS = new PermissionScreen(MODULE, "PLATFORM_TENANTS", "المستأجرون", "Tenants");

    @Override
    public List<PermissionModule> modules() {
        return List.of(new PermissionModule(MODULE, "إدارة المنصة", "Platform Administration"));
    }

    @Override
    public List<PermissionScreen> screens() {
        return List.of(TENANTS);
    }

    @Override
    public List<PermissionDef> permissions() {
        return List.of(
            PermissionDef.of(TENANTS, "VIEW", "عرض", PERM_PLATFORM_TENANTS_VIEW),
            PermissionDef.of(TENANTS, "MANAGE", "إدارة", PLATFORM_TENANT_MANAGE));
    }
}
