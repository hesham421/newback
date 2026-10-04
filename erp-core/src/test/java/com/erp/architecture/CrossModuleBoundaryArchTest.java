package com.erp.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * Enforces "module A may only reach module B through B's own {@code crossmodule} package".
 * Pom consolidation (single {@code com.erp:erp-system} artifact, no {@code <modules>}) removed
 * the per-module Maven dependency graph entirely, so this suite is the only thing left
 * enforcing that boundary — there is no compile-time check to fall back on.
 *
 * <p>Recreated {@code 2026-09-11}: the previous version of this suite (module prefixes
 * {@code com.erp.security}/{@code notification}/{@code org}/{@code masterdata}/{@code file})
 * was deleted in commit {@code 1361316} ("cc", 2026-09-03) as apparent collateral of a bulk
 * governance-file cleanup, and was never restored. Module packages have since been renamed
 * ({@code security}→{@code sec}, {@code notification}→{@code notif}, {@code org}→{@code cu});
 * {@code masterdata} had no source under {@code com.erp} at that time. This version targets the
 * current package layout — see {@code src/main/java/com/erp/*}. {@code com.erp.mdl} (Master Data
 * Lookup, the {@code masterdata} module's actual package prefix) was added to {@link #MODULES}
 * once its first source classes landed, so it is no longer misclassified as {@code "shared"}.
 */
@AnalyzeClasses(packages = "com.erp")
public class CrossModuleBoundaryArchTest {

    /**
     * One entry per business module: its own package prefix, and its designated public
     * {@code crossmodule} sub-package. Add an entry the moment a new business module gets its
     * first class. {@code common} (shared foundation) and {@code autoconfigure} (composition root) are
     * deliberately not module-bounded and are not listed here.
     */
    private static final List<Module> MODULES = List.of(
            // erp-core step 06: com.erp.sec.permission (exact) is SEC's permission-catalog SPI
            // (PermissionContributor, PermissionDef, ...) that every module's XxxPermissions implements.
            new Module("com.erp.sec", "com.erp.sec.crossmodule", "com.erp.sec.permission"),
            // erp-core step 08: com.erp.notif.channel (the ChannelProvider SPI) is public too
            new Module("com.erp.notif", "com.erp.notif.crossmodule", "com.erp.notif.channel"),
            new Module("com.erp.file", "com.erp.file.crossmodule"),
            new Module("com.erp.cu", "com.erp.cu.crossmodule"),
            new Module("com.erp.mdl", "com.erp.mdl.crossmodule"),
            // erp-core step 05: the tenant module's public surface is its ROOT package only —
            // TenantContext, TenantConstants and the provisioning SPI (TenantProvisioning,
            // TenantProvisioningContributor), which every module may use. Its entity, repository,
            // service, controller and filter stay internal (com.erp.autoconfigure wires the filter).
            new Module("com.erp.tenant", "com.erp.tenant.crossmodule", "com.erp.tenant"),
            // erp-core step 08: the event bus's public surface is its ROOT package — DomainEvent,
            // DomainEventPublisher, ErpCoreEvents and the core event classes, which every module may
            // publish or listen to. com.erp.events.support (publisher impl, task decorator) is internal.
            new Module("com.erp.events", "com.erp.events.crossmodule", "com.erp.events"),
            // erp-core step 09: number series; public surface = com.erp.sequence.crossmodule (NumberSeriesApi)
            new Module("com.erp.sequence", "com.erp.sequence.crossmodule"),
            // erp-core step 10: the audit log's public surface is its crossmodule package (AuditApi,
            // AuditEntry, AuditChange, the @Audited annotation every audited entity carries)
            new Module("com.erp.audit", "com.erp.audit.crossmodule")
    );

    /**
     * @param exactPublicPackages packages (exact match, sub-packages NOT included) that are public in
     *                            addition to {@code crossModulePackage}
     */
    private record Module(String packagePrefix, String crossModulePackage, String... exactPublicPackages) {
    }

    /**
     * The structural half of the boundary: no class outside module X's own package may depend
     * on a class inside module X's package unless that class is in module X's
     * {@code crossmodule} sub-package. A module with no {@code crossmodule} package yet (e.g.
     * {@code file}, {@code cu} today) is still covered: the predicate simply never matches, so
     * ANY external dependency on that module's internals fails the rule until a
     * {@code crossmodule} package is deliberately introduced.
     *
     * <p>{@code com.erp.autoconfigure} is exempt: it is the library's composition root (step 03
     * replaced the application's {@code com.erp.main} with it), and wiring a concrete class
     * (e.g. {@code ErpCoreSecurityAutoConfiguration} building the {@code SecurityFilterChain} from
     * {@code sec.security.JwtAuthenticationFilter}, or {@code DownloadTokenStoreAutoConfiguration}
     * importing the FILE token stores) is bootstrap wiring, not a business module reaching into
     * another module's internals. A business module doing the same thing would still be caught.
     */
    @ArchTest
    static void modules_only_expose_their_crossmodule_package_to_outsiders(JavaClasses classes) {
        for (Module module : MODULES) {
            DescribedPredicate<JavaClass> publicSurface =
                    JavaClass.Predicates.resideInAPackage(module.crossModulePackage() + "..");
            for (String exact : module.exactPublicPackages()) {
                publicSurface = publicSurface.or(JavaClass.Predicates.resideInAPackage(exact));
            }
            ArchRule rule = noClasses().that().resideOutsideOfPackage(module.packagePrefix() + "..")
                    .and().resideOutsideOfPackage("com.erp.autoconfigure..")
                    .should().dependOnClassesThat(
                            JavaClass.Predicates.resideInAPackage(module.packagePrefix() + "..")
                                    .and(DescribedPredicate.not(publicSurface))
                    )
                    .as("classes outside " + module.packagePrefix()
                            + " must only depend on its " + module.crossModulePackage() + " surface");
            rule.check(classes);
        }
    }

    /**
     * The half normal ArchUnit dependency rules structurally cannot see: a
     * {@code @PreAuthorize} SpEL string's {@code T(...)} type reference is a plain String
     * constant in bytecode, not a real class dependency. This walks every
     * {@code @PreAuthorize}-annotated method's expression looking for a
     * {@code T(fully.qualified.Type)} reference that crosses a module boundary. Any such
     * reference found is a bypass of the structural rule above and must fail the build.
     *
     * <p>erp-core step 06: there is no exception any more. The former cross-module constants class in
     * SEC is gone; every module references the permission constants class of its own module
     * (e.g. {@code com.erp.file.permission.FilePermissions}), which this rule enforces for every
     * {@code @PreAuthorize}.
     */
    @ArchTest
    static void spel_type_references_do_not_bypass_the_module_boundary(JavaClasses classes) {
        Pattern typeReference = Pattern.compile("T\\(([a-zA-Z0-9_.]+)\\)");
        for (JavaClass clazz : classes) {
            for (JavaMethod method : clazz.getMethods()) {
                if (!method.isAnnotatedWith(PreAuthorize.class)) {
                    continue;
                }
                String expression = method.getAnnotationOfType(PreAuthorize.class).value();
                Matcher matcher = typeReference.matcher(expression);
                while (matcher.find()) {
                    String referencedType = matcher.group(1);
                    String callerModule = topLevelModuleOf(clazz.getPackageName());
                    String referencedModule = topLevelModuleOf(packageOf(referencedType));
                    if (!callerModule.equals(referencedModule)) {
                        throw new AssertionError(
                                "New cross-module @PreAuthorize SpEL type reference found: "
                                        + clazz.getFullName() + "#" + method.getName()
                                        + " references " + referencedType
                                        + " — this bypasses the structural ArchUnit rule above and was not "
                                        + "reviewed. Route through the referenced module's crossmodule "
                                        + "package instead, or if this is a deliberate, accepted exception, "
                                        + "add explicit handling for it in this test — do not let it pass "
                                        + "silently.");
                    }
                }
            }
        }
    }

    /**
     * erp-core step 06, task 8 — SEC (identity and permissions) must not depend on FILE, NOTIF, MDL or
     * CU except through their {@code crossmodule} packages. The old central permission-constants class
     * knew every module; the per-module permission contributors replaced it, and this rule keeps SEC
     * from growing such knowledge again.
     */
    @ArchTest
    static void sec_depends_on_other_core_modules_only_through_their_crossmodule_packages(JavaClasses classes) {
        for (String other : List.of("com.erp.file", "com.erp.notif", "com.erp.mdl", "com.erp.cu")) {
            noClasses().that().resideInAPackage("com.erp.sec..")
                    .should().dependOnClassesThat(JavaClass.Predicates.resideInAPackage(other + "..")
                            .and(DescribedPredicate.not(JavaClass.Predicates.resideInAPackage(other + ".crossmodule.."))))
                    .as("com.erp.sec must reach " + other + " only through " + other + ".crossmodule")
                    .check(classes);
        }
    }

    /**
     * erp-core step 06 — every {@code T(...)} type a {@code @PreAuthorize} references lives in the
     * annotated class's own module (the rule above), and the permission constants class
     * that used to be shared by all modules (in com.erp.sec.permission) no longer exists.
     */
    @ArchTest
    static void the_shared_permission_constants_class_is_gone(JavaClasses classes) {
        boolean present = classes.stream().anyMatch(clazz -> clazz.getSimpleName().equals("Permission" + "Constants"));
        if (present) {
            throw new AssertionError("A shared permission constants class exists again: each module declares its own "
                    + "permissions through a com.erp.sec.permission.PermissionContributor");
        }
    }

    /**
     * erp-core step 09 acceptance — {@code NumberSeriesApi} and {@code SettingsApi} are interfaces in their
     * modules' {@code crossmodule} packages, i.e. on the public surface the boundary rule above allows every
     * other module to depend on (the sequence module itself consumes {@code tenant.crossmodule} the same way).
     */
    @ArchTest
    static void step09_apis_are_crossmodule_interfaces_allowed_by_the_boundary_rule(JavaClasses classes) {
        for (String api : List.of("com.erp.sequence.crossmodule.NumberSeriesApi", "com.erp.cu.crossmodule.SettingsApi")) {
            JavaClass clazz = classes.get(api);
            if (!clazz.isInterface()) {
                throw new AssertionError(api + " must be an interface");
            }
            Module module = MODULES.stream()
                    .filter(m -> clazz.getPackageName().startsWith(m.packagePrefix() + "."))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(api + " is not inside a bounded module"));
            if (!JavaClass.Predicates.resideInAPackage(module.crossModulePackage() + "..").test(clazz)) {
                throw new AssertionError(api + " is not in " + module.crossModulePackage());
            }
        }
    }

    private static String topLevelModuleOf(String packageName) {
        for (Module module : MODULES) {
            if (packageName.equals(module.packagePrefix()) || packageName.startsWith(module.packagePrefix() + ".")) {
                return module.packagePrefix();
            }
        }
        return "shared";
    }

    private static String packageOf(String fullyQualifiedClassName) {
        int lastDot = fullyQualifiedClassName.lastIndexOf('.');
        return lastDot < 0 ? "" : fullyQualifiedClassName.substring(0, lastDot);
    }
}
