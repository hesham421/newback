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

    private static final PermissionScreen CONFIGURATIONS =
        new PermissionScreen(MODULE, "CU_CONFIGURATIONS", "إدارة إعدادات المنصة", "Platform Configuration");

    @Override
    public List<PermissionModule> modules() {
        return List.of(new PermissionModule(MODULE, "الأدوات المشتركة", "Common Utilities"));
    }

    @Override
    public List<PermissionScreen> screens() {
        return List.of(CONFIGURATIONS);
    }

    @Override
    public List<PermissionDef> permissions() {
        return List.of(
            PermissionDef.of(CONFIGURATIONS, "VIEW", "عرض", CONFIG_VIEW),
            PermissionDef.of(CONFIGURATIONS, "CREATE", "إنشاء", CONFIG_CREATE),
            PermissionDef.of(CONFIGURATIONS, "UPDATE", "تعديل", CONFIG_UPDATE),
            PermissionDef.of(CONFIGURATIONS, "DEACTIVATE", "إلغاء تفعيل", CONFIG_DEACTIVATE));
    }
}
