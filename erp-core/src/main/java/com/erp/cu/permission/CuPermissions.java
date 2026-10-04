package com.erp.cu.permission;

import com.erp.sec.permission.PermissionContributor;
import com.erp.sec.permission.PermissionDef;
import com.erp.sec.permission.PermissionModule;
import com.erp.sec.permission.PermissionScreen;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * CU's permission catalog (erp-core step 06) — the rows {@code V7__sec_seed.sql} seeded for CU, with
 * the same names. CU is backend-only: its four codes deliberately deviate from
 * {@code PERM_<PAGE_CODE>_<ACTION>} (a recorded decision, "Do NOT rename them") and are anchored to the
 * backend-only holder screen {@code CU_CONFIGURATIONS}; they are passed as explicit authorities.
 */
@Component
public class CuPermissions implements PermissionContributor {

    public static final String MODULE = "CU";

    /** API-CU-002 (search configurations), API-CU-003 (read by key). */
    public static final String CONFIG_VIEW = "CONFIG_VIEW";
    /** API-CU-001 (create configuration). */
    public static final String CONFIG_CREATE = "CONFIG_CREATE";
    /** API-CU-004 (update configuration value). */
    public static final String CONFIG_UPDATE = "CONFIG_UPDATE";
    /** API-CU-005 (deactivate configuration) — DELETE-class; the action code stays DEACTIVATE. */
    public static final String CONFIG_DEACTIVATE = "CONFIG_DEACTIVATE";

    // --- erp-core step 09: platform defaults of the settings (scope=PLATFORM) ---
    // Declared under the PLATFORM registry module, so — like PLATFORM_TENANT_MANAGE — they are effective
    // only inside the PLATFORM tenant: tenant provisioning never copies PLATFORM-module grants, and super
    // roles of other tenants never receive PLATFORM-module authorities (MenuService).

    /** Registry module of the platform administration screens (declared, with its names, by the tenant module). */
    public static final String PLATFORM_MODULE = "PLATFORM";

    /** Gateway (VIEW) action of screen PLATFORM_SETTINGS (RULE-SEC-007). */
    public static final String PERM_PLATFORM_SETTINGS_VIEW = "PERM_PLATFORM_SETTINGS_VIEW";

    /** Read and write the platform defaults ({@code scope=PLATFORM} on every configuration endpoint). */
    public static final String PLATFORM_SETTINGS_MANAGE = "PLATFORM_SETTINGS_MANAGE";

    private static final PermissionScreen PLATFORM_SETTINGS =
        new PermissionScreen(PLATFORM_MODULE, "PLATFORM_SETTINGS", "إعدادات المنصة الافتراضية", "Platform Default Settings");

    private static final PermissionScreen CONFIGURATIONS =
        new PermissionScreen(MODULE, "CU_CONFIGURATIONS", "إدارة إعدادات المنصة", "Platform Configuration");

    @Override
    public List<PermissionModule> modules() {
        return List.of(new PermissionModule(MODULE, "الأدوات المشتركة", "Common Utilities"));
    }

    @Override
    public List<PermissionScreen> screens() {
        return List.of(CONFIGURATIONS, PLATFORM_SETTINGS);
    }

    @Override
    public List<PermissionDef> permissions() {
        return List.of(
            PermissionDef.of(CONFIGURATIONS, "VIEW", "عرض", CONFIG_VIEW),
            PermissionDef.of(CONFIGURATIONS, "CREATE", "إنشاء", CONFIG_CREATE),
            PermissionDef.of(CONFIGURATIONS, "UPDATE", "تعديل", CONFIG_UPDATE),
            PermissionDef.of(CONFIGURATIONS, "DEACTIVATE", "إلغاء تفعيل", CONFIG_DEACTIVATE),
            PermissionDef.of(PLATFORM_SETTINGS, "VIEW", "عرض", PERM_PLATFORM_SETTINGS_VIEW),
            PermissionDef.of(PLATFORM_SETTINGS, "MANAGE", "إدارة", PLATFORM_SETTINGS_MANAGE));
    }
}
