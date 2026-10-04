package com.erp.mdl.permission;

import com.erp.sec.permission.PermissionContributor;
import com.erp.sec.permission.PermissionDef;
import com.erp.sec.permission.PermissionModule;
import com.erp.sec.permission.PermissionScreen;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * MDL's permission catalog (erp-core step 06) — the rows {@code V7__sec_seed.sql} seeded for MDL, with
 * the same names. The constants are the authorities MDL's own {@code @PreAuthorize} expressions use.
 */
@Component
public class MdlPermissions implements PermissionContributor {

    public static final String MODULE = "MDL";

    /** API-MDL-001, 005, 011 — screen MDL_LOOKUPS. */
    public static final String PERM_MDL_LOOKUPS_VIEW = "PERM_MDL_LOOKUPS_VIEW";
    /** API-MDL-006 — screen MDL_LOOKUPS (also gates API-MDL-002, create lookup type). */
    public static final String PERM_MDL_LOOKUPS_CREATE = "PERM_MDL_LOOKUPS_CREATE";
    /**
     * API-MDL-003, 004, 007, 008, 009 — screen MDL_LOOKUPS. Deactivate is modelled as UPDATE — MDL_LOOKUPS
     * has no DELETE permission.
     */
    public static final String PERM_MDL_LOOKUPS_UPDATE = "PERM_MDL_LOOKUPS_UPDATE";
    /** API-MDL-010 — screen MDL_TYPE_REGISTRY, a separate screen-gate from MDL_LOOKUPS. */
    public static final String PERM_MDL_TYPE_REGISTRY_VIEW = "PERM_MDL_TYPE_REGISTRY_VIEW";

    private static final PermissionScreen LOOKUPS =
        new PermissionScreen(MODULE, "MDL_LOOKUPS", "قوائم البيانات المرجعية", "Master data lookups");
    private static final PermissionScreen TYPE_REGISTRY =
        new PermissionScreen(MODULE, "MDL_TYPE_REGISTRY", "سجل أنواع البيانات المرجعية", "Master data type registry");

    @Override
    public List<PermissionModule> modules() {
        return List.of(new PermissionModule(MODULE, "البيانات المرجعية", "Master Data Lookup"));
    }

    @Override
    public List<PermissionScreen> screens() {
        return List.of(LOOKUPS, TYPE_REGISTRY);
    }

    @Override
    public List<PermissionDef> permissions() {
        return List.of(
            PermissionDef.of(LOOKUPS, "VIEW", "عرض"),
            PermissionDef.of(LOOKUPS, "CREATE", "إنشاء"),
            PermissionDef.of(LOOKUPS, "UPDATE", "تعديل"),
            PermissionDef.of(TYPE_REGISTRY, "VIEW", "عرض"));
    }
}
