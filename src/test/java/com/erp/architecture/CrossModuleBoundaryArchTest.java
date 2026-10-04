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
     * first class. {@code common} (shared foundation) and {@code main} (composition root) are
     * deliberately not module-bounded and are not listed here.
     */
    private static final List<Module> MODULES = List.of(
            new Module("com.erp.sec", "com.erp.sec.crossmodule"),
            new Module("com.erp.notif", "com.erp.notif.crossmodule"),
            new Module("com.erp.file", "com.erp.file.crossmodule"),
            new Module("com.erp.cu", "com.erp.cu.crossmodule"),
            new Module("com.erp.mdl", "com.erp.mdl.crossmodule")
    );

    private record Module(String packagePrefix, String crossModulePackage) {
    }

    /**
     * The structural half of the boundary: no class outside module X's own package may depend
     * on a class inside module X's package unless that class is in module X's
     * {@code crossmodule} sub-package. A module with no {@code crossmodule} package yet (e.g.
     * {@code file}, {@code cu} today) is still covered: the predicate simply never matches, so
     * ANY external dependency on that module's internals fails the rule until a
     * {@code crossmodule} package is deliberately introduced.
     *
     * <p>{@code com.erp.main} is exempt: it is the Spring composition root, and wiring a
     * concrete security filter class (e.g. {@code SecurityConfig} constructing the
     * {@code SecurityFilterChain} bean from {@code sec.security.JwtAuthenticationFilter}) is
     * bootstrap wiring, not a business module reaching into another module's internals. A
     * business module doing the same thing would still be caught.
     */
    @ArchTest
    static void modules_only_expose_their_crossmodule_package_to_outsiders(JavaClasses classes) {
        for (Module module : MODULES) {
            ArchRule rule = noClasses().that().resideOutsideOfPackage(module.packagePrefix() + "..")
                    .and().resideOutsideOfPackage("com.erp.main..")
                    .should().dependOnClassesThat(
                            JavaClass.Predicates.resideInAPackage(module.packagePrefix() + "..")
                                    .and(DescribedPredicate.not(
                                            JavaClass.Predicates.resideInAPackage(module.crossModulePackage() + "..")))
                    )
                    .as("classes outside " + module.packagePrefix()
                            + " must only depend on its " + module.crossModulePackage() + " surface");
            rule.check(classes);
        }
    }

    /** Fully-qualified name of the one class exempted by {@link #spel_type_references_do_not_bypass_the_module_boundary}. */
    private static final String PERMISSION_CONSTANTS_CLASS = "com.erp.sec.permission.PermissionConstants";

    /**
     * The half normal ArchUnit dependency rules structurally cannot see: a
     * {@code @PreAuthorize} SpEL string's {@code T(...)} type reference is a plain String
     * constant in bytecode, not a real class dependency. This walks every
     * {@code @PreAuthorize}-annotated method's expression looking for a
     * {@code T(fully.qualified.Type)} reference that crosses a module boundary. Any such
     * reference found is a new, unreviewed bypass of the structural rule above and must fail
     * the build.
     *
     * <p><b>Deliberate, accepted exception:</b> a reference to exactly
     * {@code com.erp.sec.permission.PermissionConstants} is allowed from any caller module.
     * That class is a pure, stateless string-constants holder with no logic — the project's
     * shared permission-naming registry, not a protected SEC-internal — and every module's
     * {@code build-create-service} skill-generated {@code @PreAuthorize} checks are
     * <em>mandated</em> to reference it by fully-qualified name (see
     * {@code build-create-service/SKILL.md}'s {@code <PERMISSIONS_CLASS>} variable: "Fully-
     * qualified name of the project's permission constants class"). This exception is an exact
     * class-name match only — no other class under {@code com.erp.sec} is exempt, so a real
     * future bypass (some other SEC-internal class referenced via SpEL {@code T(...)}) still
     * fails.
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
                    if (referencedType.equals(PERMISSION_CONSTANTS_CLASS)) {
                        continue;
                    }
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
