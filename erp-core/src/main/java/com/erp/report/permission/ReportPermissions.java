package com.erp.report.permission;

import com.erp.report.ReportAuthorities;
import com.erp.report.ReportProvider;
import com.erp.report.registry.ReportRegistry;
import com.erp.sec.permission.PermissionContributor;
import com.erp.sec.permission.PermissionDef;
import com.erp.sec.permission.PermissionScreen;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Bridges the report registry into the RBAC catalog (erp-core step 11): every registered report
 * appears in the catalog automatically, so super roles hold it at once and administrators can grant it.
 * <ul>
 *   <li>one screen per owning module, {@code <MODULE>_REPORTS} ({@code SEC_SCREEN_REG.PAGE_CODE} is
 *       globally unique, so the step file's screen {@code REPORT} cannot be shared by several modules);</li>
 *   <li>its {@code VIEW} gateway action ({@code PERM_<MODULE>_REPORTS_VIEW}, RULE-SEC-007: a role's other
 *       grants on the screen count only together with it);</li>
 *   <li>one action per report: action code = report code, permission code
 *       {@code <MODULE>:REPORT:<CODE>} ({@link ReportAuthorities}), names = the report titles.</li>
 * </ul>
 * Module rows are not declared here (that would rename the owning module's row); a module known only
 * from a report is created by the synchronizer with its code as name.
 */
@Component
@RequiredArgsConstructor
public class ReportPermissions implements PermissionContributor {

    /** Suffix of the per-module report screen code. */
    public static final String SCREEN_SUFFIX = "_REPORTS";

    private final ReportRegistry registry;

    /** {@code <MODULE>_REPORTS}. */
    public static String screenCodeOf(String moduleCode) {
        return moduleCode + SCREEN_SUFFIX;
    }

    @Override
    public List<PermissionScreen> screens() {
        return List.copyOf(screensByModule().values());
    }

    @Override
    public List<PermissionDef> permissions() {
        List<PermissionDef> permissions = new ArrayList<>();
        for (PermissionScreen screen : screensByModule().values()) {
            permissions.add(PermissionDef.of(screen, "VIEW", "عرض"));
        }
        for (ReportProvider provider : registry.all()) {
            permissions.add(new PermissionDef(provider.moduleCode(), screenCodeOf(provider.moduleCode()),
                provider.code(), provider.titleAr(), provider.titleEn(), ReportAuthorities.of(provider)));
        }
        return List.copyOf(permissions);
    }

    private Map<String, PermissionScreen> screensByModule() {
        Map<String, PermissionScreen> screens = new LinkedHashMap<>();
        for (ReportProvider provider : registry.all()) {
            String module = provider.moduleCode();
            screens.computeIfAbsent(module, code -> new PermissionScreen(code, screenCodeOf(code),
                "تقارير " + code, code + " Reports"));
        }
        return screens;
    }
}
