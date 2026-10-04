package com.erp.sec.service;

import com.erp.sec.domain.RoleActionGrantDomain;
import com.erp.sec.entity.ActionRegistry;
import com.erp.sec.entity.ModuleRegistry;
import com.erp.sec.entity.ScreenRegistry;
import com.erp.sec.permission.PermissionContributor;
import com.erp.sec.permission.PermissionDef;
import com.erp.sec.permission.PermissionModule;
import com.erp.sec.permission.PermissionScreen;
import com.erp.sec.repository.ActionRegistryRepository;
import com.erp.sec.repository.ModuleRegistryRepository;
import com.erp.sec.repository.ScreenRegistryRepository;
import com.erp.tenant.TenantConstants;
import com.erp.tenant.TenantContext;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Writes the code-defined permission catalog into the global registry on every start (erp-core
 * step 06): every module, screen and permission contributed by a {@link PermissionContributor} bean is
 * upserted into {@code SEC_MODULE_REG} / {@code SEC_SCREEN_REG} / {@code SEC_ACTION_REG}.
 * <ul>
 *   <li>missing rows are inserted (a module or screen referenced only by a permission gets its code as
 *       name);</li>
 *   <li>existing rows get the contributed names when they differ (and nothing else);</li>
 *   <li>nothing is ever deleted, and {@code IS_ACTIVE_FL} is never touched (deactivation through the
 *       registry API stays in force);</li>
 *   <li>running it again changes nothing (idempotent).</li>
 * </ul>
 * The core contributors declare exactly the rows {@code V7__sec_seed.sql} / {@code V10__tenant_schema.sql}
 * seeded, so on a core database the first run is a no-op; an application's contributor adds its own.
 *
 * <p>Runs first among the application runners. Grants are evaluated per request
 * ({@code MenuService.effectiveAuthorityCodes}), so newly contributed permissions are visible to super
 * roles from the next request on. Startup infrastructure without a principal (the
 * {@code BootstrapAdminPasswordRunner} precedent): it uses SEC's repositories directly. The catalog is
 * global, but a Hibernate session always needs a tenant, so it runs as PLATFORM, in one transaction.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@Slf4j
public class PermissionCatalogSynchronizer implements ApplicationRunner {

    /** Counts of one synchronization run. */
    public record Result(int modulesInserted, int screensInserted, int permissionsInserted, int rowsRenamed) {

        public boolean changedNothing() {
            return modulesInserted == 0 && screensInserted == 0 && permissionsInserted == 0 && rowsRenamed == 0;
        }
    }

    private final ObjectProvider<PermissionContributor> contributors;
    private final ModuleRegistryRepository moduleRepository;
    private final ScreenRegistryRepository screenRepository;
    private final ActionRegistryRepository actionRepository;
    private final TransactionTemplate transactionTemplate;

    public PermissionCatalogSynchronizer(ObjectProvider<PermissionContributor> contributors,
                                         ModuleRegistryRepository moduleRepository,
                                         ScreenRegistryRepository screenRepository,
                                         ActionRegistryRepository actionRepository,
                                         PlatformTransactionManager transactionManager) {
        this.contributors = contributors;
        this.moduleRepository = moduleRepository;
        this.screenRepository = screenRepository;
        this.actionRepository = actionRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        Result result = synchronize();
        log.info("Permission catalog synchronized: {} modules, {} screens, {} permissions inserted, {} rows renamed",
            result.modulesInserted(), result.screensInserted(), result.permissionsInserted(), result.rowsRenamed());
    }

    /** One upsert pass over every contributor (PLATFORM tenant context, one transaction). */
    public Result synchronize() {
        List<PermissionContributor> all = contributors.orderedStream().toList();
        return TenantContext.callAs(TenantConstants.PLATFORM_TENANT_ID,
            () -> transactionTemplate.execute(status -> upsert(all)));
    }

    private Result upsert(List<PermissionContributor> all) {
        Map<String, PermissionModule> modules = new LinkedHashMap<>();
        Map<String, PermissionScreen> screens = new LinkedHashMap<>();
        Map<String, PermissionDef> permissions = new LinkedHashMap<>();
        for (PermissionContributor contributor : all) {
            String source = contributor.getClass().getName();
            contributor.modules().stream().filter(m -> m.code() != null)
                .forEach(m -> modules.putIfAbsent(m.code(), m));
            contributor.screens().stream().filter(s -> s.moduleCode() != null && s.screenCode() != null)
                .forEach(s -> screens.putIfAbsent(s.screenCode(), s));
            for (PermissionDef def : contributor.permissions()) {
                if (!def.isComplete()) {
                    log.error("Permission contributor {} declared an incomplete permission {}; skipped", source, def);
                    continue;
                }
                PermissionDef previous = permissions.putIfAbsent(def.authority(), def);
                if (previous != null && !previous.equals(def)) {
                    log.error("Permission {} is contributed twice with different definitions ({} wins); {} skipped",
                        def.authority(), previous, def);
                }
            }
        }
        // modules and screens referenced only by a permission
        permissions.values().forEach(def -> {
            modules.putIfAbsent(def.moduleCode(), new PermissionModule(def.moduleCode(), def.moduleCode(), def.moduleCode()));
            screens.putIfAbsent(def.screenCode(),
                new PermissionScreen(def.moduleCode(), def.screenCode(), def.screenCode(), def.screenCode()));
        });
        screens.values().forEach(screen -> modules.putIfAbsent(screen.moduleCode(),
            new PermissionModule(screen.moduleCode(), screen.moduleCode(), screen.moduleCode())));

        int[] counts = new int[4];
        Map<String, ModuleRegistry> moduleRows = new LinkedHashMap<>();
        for (PermissionModule module : modules.values()) {
            moduleRows.put(module.code(), upsertModule(module, counts));
        }
        Map<String, ScreenRegistry> screenRows = new LinkedHashMap<>();
        for (PermissionScreen screen : screens.values()) {
            screenRows.put(screen.screenCode(), upsertScreen(screen, moduleRows.get(screen.moduleCode()), counts));
        }
        for (PermissionDef def : permissions.values()) {
            upsertAction(def, screenRows.get(def.screenCode()), counts);
        }
        warnOnMissingGateways(permissions.values());
        return new Result(counts[0], counts[1], counts[2], counts[3]);
    }

    private ModuleRegistry upsertModule(PermissionModule module, int[] counts) {
        ModuleRegistry row = moduleRepository.findByCode(module.code()).orElse(null);
        if (row == null) {
            counts[0]++;
            return moduleRepository.save(ModuleRegistry.builder()
                .code(module.code()).nameAr(nameOr(module.nameAr(), module.code()))
                .nameEn(nameOr(module.nameEn(), module.code())).build());
        }
        if (rename(row.getNameAr(), row.getNameEn(), module.nameAr(), module.nameEn())) {
            row.setNameAr(module.nameAr());
            row.setNameEn(module.nameEn());
            counts[3]++;
            return moduleRepository.save(row);
        }
        return row;
    }

    private ScreenRegistry upsertScreen(PermissionScreen screen, ModuleRegistry module, int[] counts) {
        ScreenRegistry row = screenRepository.findByPageCode(screen.screenCode()).orElse(null);
        if (row == null) {
            counts[1]++;
            return screenRepository.save(ScreenRegistry.builder()
                .pageCode(screen.screenCode()).module(module)
                .nameAr(nameOr(screen.nameAr(), screen.screenCode()))
                .nameEn(nameOr(screen.nameEn(), screen.screenCode())).build());
        }
        if (!Objects.equals(row.getModule().getCode(), screen.moduleCode())) {
            log.warn("Screen {} is registered under module {}, contributed under {}; module left unchanged",
                screen.screenCode(), row.getModule().getCode(), screen.moduleCode());
        }
        if (rename(row.getNameAr(), row.getNameEn(), screen.nameAr(), screen.nameEn())) {
            row.setNameAr(screen.nameAr());
            row.setNameEn(screen.nameEn());
            counts[3]++;
            return screenRepository.save(row);
        }
        return row;
    }

    private void upsertAction(PermissionDef def, ScreenRegistry screen, int[] counts) {
        ActionRegistry row = actionRepository.findByPermissionCode(def.authority()).orElse(null);
        if (row == null) {
            counts[2]++;
            actionRepository.save(ActionRegistry.builder()
                .permissionCode(def.authority()).screen(screen).actionCode(def.actionCode())
                .nameAr(def.nameAr()).nameEn(def.nameEn()).build());
            return;
        }
        if (rename(row.getNameAr(), row.getNameEn(), def.nameAr(), def.nameEn())) {
            row.setNameAr(def.nameAr());
            row.setNameEn(def.nameEn());
            counts[3]++;
            actionRepository.save(row);
        }
    }

    /** Only a declared, different name renames a row (a code-as-name fallback never overwrites). */
    private static boolean rename(String currentAr, String currentEn, String nameAr, String nameEn) {
        return nameAr != null && !nameAr.isBlank() && nameEn != null && !nameEn.isBlank()
            && (!nameAr.equals(currentAr) || !nameEn.equals(currentEn));
    }

    private static String nameOr(String name, String fallback) {
        return name == null || name.isBlank() ? fallback : name;
    }

    /** RULE-SEC-007: a screen's non-VIEW grants only count together with its VIEW. */
    private static void warnOnMissingGateways(java.util.Collection<PermissionDef> permissions) {
        Set<String> screensWithView = permissions.stream()
            .filter(def -> RoleActionGrantDomain.isGatewayAction(def.actionCode()))
            .map(PermissionDef::screenCode).collect(Collectors.toSet());
        permissions.stream().map(PermissionDef::screenCode).distinct()
            .filter(screen -> !screensWithView.contains(screen))
            .forEach(screen -> log.warn("Screen {} contributes permissions but no VIEW action: role grants of its "
                + "other actions never become effective (RULE-SEC-007)", screen));
    }
}
