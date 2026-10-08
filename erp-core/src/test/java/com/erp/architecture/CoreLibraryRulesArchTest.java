package com.erp.architecture;

import com.erp.common.domain.AuditableEntity;
import com.erp.common.domain.GlobalAuditableEntity;
import com.tngtech.archunit.core.domain.JavaAnnotation;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaField;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.domain.JavaParameter;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * erp-core step 12, task A — the structural rules of the library, enforced by the build instead of by
 * convention ("the core is never copied or modified by an app"). {@link CrossModuleBoundaryArchTest} keeps
 * the module boundaries; this suite holds the library-wide rules 1–7 of the step file. Each rule was
 * verified once against a deliberate violation (see {@code docs/steps/12-report.md}).
 *
 * <p>Only production classes are analysed ({@link ImportOption.DoNotIncludeTests}): test code may use test
 * libraries and declares the test {@code @SpringBootApplication}s. Rule 3 (no "Fin") also scans test classes.
 *
 * <p>Documented exceptions (each recorded in {@code docs/DEVIATIONS.md} under [12]) are encoded explicitly
 * below, next to the rule they relax — never by weakening the rule for everything.
 */
@AnalyzeClasses(packages = "com.erp", importOptions = ImportOption.DoNotIncludeTests.class)
class CoreLibraryRulesArchTest {

    /** Rule 1: the packages a core class may depend on (step file list). */
    static final List<String> ALLOWED_DEPENDENCY_PACKAGES = List.of(
            "com.erp..", "java..", "jakarta..", "org.springframework..", "org.hibernate..", "com.fasterxml..",
            "io.jsonwebtoken..", "io.github.bucket4j..", "org.slf4j..", "lombok..", "software.amazon.awssdk..",
            "org.springdoc..", "org.flywaydb..",
            // [12] documented additions — all already on the step-03..11 dependency list, no new library:
            // javax.* is the JDK itself (javax.crypto for HMAC tokens, javax.sql.DataSource);
            "javax..",
            // the OpenAPI annotations (@Schema, @Operation, @Tag) and model (OpenAPI, SecurityScheme — used by
            // ErpCoreOpenApiAutoConfiguration) that springdoc ships, in swagger-core's own package;
            "io.swagger.v3.oas..",
            // Jackson 3 (Spring Boot 4's JSON mapper) lives in tools.jackson — the successor of com.fasterxml;
            "tools.jackson..",
            // the AOP Alliance API that Spring AOP's MethodInterceptor is defined on (SecForbiddenAdvisor).
            "org.aopalliance..",
            // the JDK's own XML API (module java.xml, like javax.xml): the hardened DOM parse of the SVG allow-list
            // (SvgAllowList, TM-D review round 1, DEVIATIONS [TM-D]) — no new library.
            "org.w3c.dom..", "org.xml.sax..");

    /**
     * Rule 2: the only entities that may be global (no TENANT_ID), named in the step files. [12] the step file
     * calls the three registries SecModuleReg/SecScreenReg/SecActionReg; the classes are named *Registry.
     * One list since tenant-maturity C3: {@link TenantScopedEntityTest#GLOBAL_ENTITIES}.
     */
    static final Set<String> GLOBAL_ENTITIES = TenantScopedEntityTest.GLOBAL_ENTITIES;

    /** Rule 7: modules whose native SQL is a documented exception (explicit TENANT_ID in every predicate). */
    static final List<String> NATIVE_SQL_PACKAGES = List.of("com.erp.tenant..", "com.erp.sequence..", "com.erp.audit..");

    /**
     * Rule 7 (raw JDBC half): besides the three modules above, where plain JDBC is a documented exception.
     * <ul>
     *   <li>{@code com.erp.<module>.tenant..} — the {@code TenantProvisioningContributor}s (step 05: JDBC, explicit
     *       TENANT_ID, copying from the source tenant inside the provisioning transaction);</li>
     *   <li>{@code com.erp.autoconfigure..} — wiring only (passes a {@code JdbcTemplate} into a bean);</li>
     *   <li>{@code NotificationRequeueJob} — step 08's cross-tenant stale-QUEUED scan, tenant by tenant;</li>
     *   <li>{@code IdempotencyKeyRetentionJob} — tenant-maturity C4's cross-tenant purge of expired idempotency keys,
     *       tenant by tenant (RULE-TENANT-011; DEVIATIONS [TM-C4]).</li>
     * </ul>
     */
    static final List<String> RAW_JDBC_PACKAGES = List.of(
            "com.erp.tenant..", "com.erp.sequence..", "com.erp.audit..", "com.erp.*.tenant..", "com.erp.autoconfigure..");
    static final Set<String> RAW_JDBC_CLASSES = Set.of("com.erp.notif.service.NotificationRequeueJob",
            "com.erp.common.idempotency.IdempotencyKeyRetentionJob");

    // ── Rule 1 ────────────────────────────────────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule rule1_core_never_depends_on_an_application =
            noClasses().that().resideInAPackage("com.erp..")
                    .should().dependOnClassesThat().resideInAPackage("com.erp.app..")
                    .as("rule 1: erp-core never depends on com.erp.app.. (an application)");

    @ArchTest
    static final ArchRule rule1_core_depends_only_on_the_allowed_libraries =
            noClasses().that().resideInAPackage("com.erp..")
                    .should().dependOnClassesThat().resideOutsideOfPackages(
                            ALLOWED_DEPENDENCY_PACKAGES.toArray(String[]::new))
                    .as("rule 1: erp-core depends only on " + ALLOWED_DEPENDENCY_PACKAGES);

    // ── Rule 2 ────────────────────────────────────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule rule2_every_entity_extends_an_auditable_base =
            classes().that().areAnnotatedWith(Entity.class)
                    .should().beAssignableTo(GlobalAuditableEntity.class)
                    .as("rule 2: every @Entity extends AuditableEntity (tenant-scoped) or GlobalAuditableEntity");

    @ArchTest
    static void rule2_only_the_listed_entities_are_global(JavaClasses classes) {
        List<String> violations = new ArrayList<>();
        for (JavaClass clazz : classes) {
            if (!clazz.isAnnotatedWith(Entity.class)) {
                continue;
            }
            boolean tenantScoped = clazz.isAssignableTo(AuditableEntity.class);
            boolean global = !tenantScoped && clazz.isAssignableTo(GlobalAuditableEntity.class);
            if (global && !GLOBAL_ENTITIES.contains(clazz.getName())) {
                violations.add(clazz.getName() + " extends GlobalAuditableEntity but is not one of " + GLOBAL_ENTITIES
                        + " — a tenant-scoped entity extends AuditableEntity");
            }
            if (GLOBAL_ENTITIES.contains(clazz.getName()) && tenantScoped) {
                violations.add(clazz.getName() + " is listed as global but extends AuditableEntity");
            }
        }
        assertNoViolations("rule 2: only " + GLOBAL_ENTITIES + " may extend GlobalAuditableEntity directly", violations);
    }

    // ── Rule 3 ────────────────────────────────────────────────────────────────────────────────────────────

    @ArchTest
    static void rule3_no_fin_class_and_no_fin_package_also_in_tests(JavaClasses ignored) {
        JavaClasses all = new ClassFileImporter().importPackages("com.erp");
        List<String> violations = new ArrayList<>();
        for (JavaClass clazz : all) {
            if (clazz.getSimpleName().matches(".*Fin.*")) {
                violations.add(clazz.getName() + ": class name matches .*Fin.*");
            }
            if (clazz.getPackageName().equals("com.erp.fin") || clazz.getPackageName().startsWith("com.erp.fin.")) {
                violations.add(clazz.getName() + ": package com.erp.fin");
            }
        }
        assertNoViolations("rule 3: the fin module never comes back (no class named .*Fin.*, no package com.erp.fin)",
                violations);
    }

    // ── Rule 4 ────────────────────────────────────────────────────────────────────────────────────────────

    /** {@code app.} / {@code file.} are the pre-step-03 keys; {@code erp.core.} must be bound, not injected. */
    private static final Pattern FORBIDDEN_VALUE = Pattern.compile("\\$\\{\\s*(app\\.|file\\.|erp\\.core\\.)");

    @ArchTest
    static void rule4_no_value_injection_of_core_configuration(JavaClasses classes) {
        List<String> violations = new ArrayList<>();
        for (JavaClass clazz : classes) {
            for (JavaField field : clazz.getFields()) {
                checkValue(field.tryGetAnnotationOfType(Value.class.getName()), field.getFullName(), violations);
            }
            for (JavaCodeUnit unit : clazz.getCodeUnits()) {
                checkValue(unit.tryGetAnnotationOfType(Value.class.getName()), unit.getFullName(), violations);
                for (JavaParameter parameter : unit.getParameters()) {
                    checkValue(parameter.tryGetAnnotationOfType(Value.class.getName()),
                            unit.getFullName() + " parameter " + parameter.getIndex(), violations);
                }
            }
        }
        assertNoViolations("rule 4: no @Value(\"${app.…}\") / @Value(\"${file.…}\") (nor ${erp.core.…}) — "
                + "all core configuration is bound through ErpCoreProperties", violations);
    }

    private static void checkValue(Optional<? extends JavaAnnotation<?>> value, String where, List<String> violations) {
        value.flatMap(annotation -> annotation.get("value")).map(Object::toString).ifPresent(expression -> {
            if (FORBIDDEN_VALUE.matcher(expression).find()) {
                violations.add(where + " injects @Value(\"" + expression + "\")");
            }
        });
    }

    // ── Rule 5 ────────────────────────────────────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule rule5_controllers_live_in_controller_packages =
            classes().that().areMetaAnnotatedWith(Controller.class)
                    .should().resideInAPackage("..controller..")
                    .as("rule 5: @Controller/@RestController classes live only under ..controller..");

    private static final Pattern TYPE_REFERENCE = Pattern.compile("T\\(\\s*([a-zA-Z0-9_.$]+)\\s*\\)");
    private static final Pattern LITERAL_AUTHORITY =
            Pattern.compile("has(Any)?(Authority|Role)\\(\\s*['\"]");

    /**
     * Every {@code T(...)} a {@code @PreAuthorize} references is a {@code *Permissions} class of the annotated
     * class's own module, and no authority is spelled as a string literal. Expressions that check no permission
     * ({@code isAuthenticated()}, {@code permitAll()}) are allowed. Documented exceptions ([12]): a
     * {@code com.erp.report.ReportProvider} of the same module (its {@code AUTHORITY} constant is the report's
     * permission, step 11) and an enum of the same module used as a value ({@code SettingScope}, step 09).
     */
    @ArchTest
    static void rule5_preauthorize_references_its_own_modules_permissions_class(JavaClasses classes) {
        List<String> violations = new ArrayList<>();
        for (JavaClass clazz : classes) {
            checkPreAuthorize(clazz, clazz.tryGetAnnotationOfType(PreAuthorize.class.getName()), clazz.getName(),
                    classes, violations);
            for (JavaMethod method : clazz.getMethods()) {
                checkPreAuthorize(clazz, method.tryGetAnnotationOfType(PreAuthorize.class.getName()),
                        method.getFullName(), classes, violations);
            }
        }
        assertNoViolations("rule 5: @PreAuthorize references a *Permissions class of its own module", violations);
    }

    private static void checkPreAuthorize(JavaClass owner, Optional<? extends JavaAnnotation<?>> annotation, String where,
                                          JavaClasses classes, List<String> violations) {
        if (annotation.isEmpty()) {
            return;
        }
        String expression = annotation.get().get("value").map(Object::toString).orElse("");
        if (LITERAL_AUTHORITY.matcher(expression).find()) {
            violations.add(where + ": authority spelled as a string literal in \"" + expression
                    + "\" — reference a constant of the module's *Permissions class");
        }
        Matcher matcher = TYPE_REFERENCE.matcher(expression);
        String ownModule = moduleOf(owner.getPackageName());
        while (matcher.find()) {
            String type = matcher.group(1);
            if (!classes.contain(type)) {
                violations.add(where + ": T(" + type + ") is not a core class");
                continue;
            }
            JavaClass target = classes.get(type);
            if (!moduleOf(target.getPackageName()).equals(ownModule)) {
                violations.add(where + ": T(" + type + ") belongs to another module than " + ownModule);
                continue;
            }
            boolean permissionsClass = target.getSimpleName().endsWith("Permissions");
            boolean reportProvider = target.isAssignableTo("com.erp.report.ReportProvider");
            boolean valueEnum = target.isEnum();
            if (!permissionsClass && !reportProvider && !valueEnum) {
                violations.add(where + ": T(" + type + ") is not a *Permissions class (nor a documented exception: "
                        + "a ReportProvider's AUTHORITY or a value enum)");
            }
        }
    }

    // ── Rule 6 ────────────────────────────────────────────────────────────────────────────────────────────

    @ArchTest
    static final ArchRule rule6_no_enable_scheduling =
            noClasses().should().beAnnotatedWith(EnableScheduling.class)
                    .as("rule 6: no @EnableScheduling in erp-core (scheduling is the application's decision)");

    @ArchTest
    static final ArchRule rule6_no_spring_boot_application =
            noClasses().should().beAnnotatedWith(SpringBootApplication.class)
                    .as("rule 6: no @SpringBootApplication in erp-core (it is a library)");

    // ── Rule 7 ────────────────────────────────────────────────────────────────────────────────────────────

    @ArchTest
    static void rule7_native_queries_only_in_tenant_sequence_audit(JavaClasses classes) {
        List<String> violations = new ArrayList<>();
        for (JavaClass clazz : classes) {
            if (resideInAny(clazz, NATIVE_SQL_PACKAGES)) {
                continue;
            }
            for (JavaMethod method : clazz.getMethods()) {
                Optional<? extends JavaAnnotation<?>> query =
                        method.tryGetAnnotationOfType("org.springframework.data.jpa.repository.Query");
                if (query.isPresent() && Boolean.TRUE.equals(query.get().get("nativeQuery").orElse(false))) {
                    violations.add(method.getFullName() + ": @Query(nativeQuery = true)");
                }
                if (method.tryGetAnnotationOfType("org.springframework.data.jpa.repository.NativeQuery").isPresent()) {
                    violations.add(method.getFullName() + ": @NativeQuery");
                }
            }
            for (JavaMethodCall call : clazz.getMethodCallsFromSelf()) {
                if (call.getName().equals("createNativeQuery")) {
                    violations.add(call.getOrigin().getFullName() + " calls " + call.getTarget().getFullName());
                }
            }
        }
        assertNoViolations("rule 7: no native SQL outside " + NATIVE_SQL_PACKAGES
                + " (native SQL bypasses Hibernate's tenant discriminator)", violations);
    }

    @ArchTest
    static void rule7_raw_jdbc_only_in_documented_places(JavaClasses classes) {
        List<String> violations = new ArrayList<>();
        for (JavaClass clazz : classes) {
            if (resideInAny(clazz, RAW_JDBC_PACKAGES) || RAW_JDBC_CLASSES.contains(clazz.getName())
                    || RAW_JDBC_CLASSES.contains(clazz.getName().replaceFirst("\\$.*", ""))) {
                continue;
            }
            clazz.getDirectDependenciesFromSelf().stream()
                    .map(dependency -> dependency.getTargetClass().getName())
                    .filter(target -> target.startsWith("org.springframework.jdbc.core."))
                    .distinct()
                    .forEach(target -> violations.add(clazz.getName() + " uses " + target));
        }
        assertNoViolations("rule 7: raw JDBC only in " + RAW_JDBC_PACKAGES + " and " + RAW_JDBC_CLASSES, violations);
    }

    // ── helpers ───────────────────────────────────────────────────────────────────────────────────────────

    private static boolean resideInAny(JavaClass clazz, List<String> packageIdentifiers) {
        return packageIdentifiers.stream().anyMatch(identifier -> JavaClass.Predicates.resideInAPackage(identifier).test(clazz));
    }

    /** {@code com.erp.<module>} of a core package ({@code com.erp.sec.service} → {@code com.erp.sec}). */
    static String moduleOf(String packageName) {
        String[] parts = packageName.split("\\.");
        return parts.length >= 3 ? parts[0] + "." + parts[1] + "." + parts[2] : packageName;
    }

    private static void assertNoViolations(String rule, List<String> violations) {
        if (!violations.isEmpty()) {
            throw new AssertionError("Architecture violated — " + rule + " (" + violations.size() + " times):\n  "
                    + String.join("\n  ", violations));
        }
    }
}
