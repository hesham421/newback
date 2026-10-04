package com.erp.sequence.permission;

import com.erp.sec.permission.PermissionContributor;
import com.erp.sec.permission.PermissionDef;
import com.erp.sec.permission.PermissionModule;
import com.erp.sec.permission.PermissionScreen;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * The sequence module's permission catalog (erp-core step 09). The step file's
 * {@code SEQUENCE:SERIES:MANAGE} is module {@code SEQUENCE}, screen {@code SEQUENCE_SERIES}, action
 * {@code MANAGE}, in the registry's authority format ({@code PERM_<SCREEN>_<ACTION>}, kept since step 06),
 * plus the screen's {@code VIEW} gateway that RULE-SEC-007 requires. Every tenant's super role
 * ({@code SYS_ADMIN}) holds both through the catalog; no grant migration is needed.
 */
@Component
public class SequencePermissions implements PermissionContributor {

    public static final String MODULE = "SEQUENCE";

    /** Read the series (GET by id, search) — the screen's gateway action. */
    public static final String PERM_SEQUENCE_SERIES_VIEW = "PERM_SEQUENCE_SERIES_VIEW";

    /** Create, update, activate and deactivate series ({@code SEQUENCE:SERIES:MANAGE}). */
    public static final String PERM_SEQUENCE_SERIES_MANAGE = "PERM_SEQUENCE_SERIES_MANAGE";

    private static final PermissionScreen SERIES =
        new PermissionScreen(MODULE, "SEQUENCE_SERIES", "سلاسل الترقيم", "Number Series");

    @Override
    public List<PermissionModule> modules() {
        return List.of(new PermissionModule(MODULE, "الترقيم التسلسلي", "Sequences"));
    }

    @Override
    public List<PermissionScreen> screens() {
        return List.of(SERIES);
    }

    @Override
    public List<PermissionDef> permissions() {
        return List.of(
            PermissionDef.of(SERIES, "VIEW", "عرض"),
            PermissionDef.of(SERIES, "MANAGE", "إدارة"));
    }
}
