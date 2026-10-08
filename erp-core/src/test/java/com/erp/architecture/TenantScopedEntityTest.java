package com.erp.architecture;

import com.erp.common.domain.AuditableEntity;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import jakarta.persistence.Entity;
import org.hibernate.annotations.TenantId;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Tenant-maturity C3 (TENANT RULE-TENANT-010, REQ-TENANT-024): every {@code @Entity} of the library is
 * tenant-scoped — it extends {@link AuditableEntity}, whose {@code TENANT_ID} is Hibernate's {@link TenantId}
 * discriminator — except the documented global set. A new global entity is never silent: it is added to
 * {@link #GLOBAL_ENTITIES} explicitly, with an analysis entry that says why.
 */
@AnalyzeClasses(packages = "com.erp", importOptions = ImportOption.DoNotIncludeTests.class)
class TenantScopedEntityTest {

    /** RULE-TENANT-010: the only entities without a tenant discriminator (the same rows for every tenant). */
    static final Set<String> GLOBAL_ENTITIES = Set.of(
            "com.erp.tenant.entity.Tenant",
            "com.erp.sec.entity.ModuleRegistry",
            "com.erp.sec.entity.ScreenRegistry",
            "com.erp.sec.entity.ActionRegistry",
            // step 09: nullable TENANT_ID (NULL = platform default); ConfigurationService names the owner in every query
            "com.erp.cu.entity.AppConfiguration");

    @ArchTest
    static void every_entity_is_tenant_scoped_unless_listed_as_global(JavaClasses classes) {
        assertNoViolations("every @Entity extends AuditableEntity (@TenantId on TENANT_ID), except " + GLOBAL_ENTITIES,
                entitiesWithoutTenantId(classes));
    }

    @ArchTest
    static void every_listed_global_entity_is_an_entity_without_a_tenant_id(JavaClasses classes) {
        assertNoViolations("GLOBAL_ENTITIES lists only existing entities without @TenantId",
                staleGlobalEntries(classes));
    }

    static List<String> entitiesWithoutTenantId(JavaClasses classes) {
        List<String> violations = new ArrayList<>();
        for (JavaClass clazz : classes) {
            if (!clazz.isAnnotatedWith(Entity.class) || GLOBAL_ENTITIES.contains(clazz.getName())) {
                continue;
            }
            if (!clazz.isAssignableTo(AuditableEntity.class) || !carriesTenantId(clazz)) {
                violations.add(clazz.getName() + " is not tenant-scoped: extend AuditableEntity (TENANT_ID as @TenantId)"
                        + " — or, only if it is truly global, add it explicitly to TenantScopedEntityTest.GLOBAL_ENTITIES"
                        + " with an analysis entry (TENANT RULE-TENANT-010) that says why");
            }
        }
        return violations;
    }

    static List<String> staleGlobalEntries(JavaClasses classes) {
        List<String> violations = new ArrayList<>();
        for (String name : GLOBAL_ENTITIES) {
            if (!classes.contain(name) || !classes.get(name).isAnnotatedWith(Entity.class)) {
                violations.add(name + " is listed as a global entity but is no @Entity — remove it from GLOBAL_ENTITIES");
            } else if (carriesTenantId(classes.get(name))) {
                violations.add(name + " is listed as a global entity but carries @TenantId — remove it from GLOBAL_ENTITIES");
            }
        }
        return violations;
    }

    private static boolean carriesTenantId(JavaClass clazz) {
        return Stream.concat(Stream.of(clazz), clazz.getAllRawSuperclasses().stream())
                .flatMap(type -> type.getFields().stream())
                .anyMatch(field -> field.isAnnotatedWith(TenantId.class));
    }

    private static void assertNoViolations(String rule, List<String> violations) {
        if (!violations.isEmpty()) {
            throw new AssertionError("Architecture violated — " + rule + " (" + violations.size() + " times):\n  "
                    + String.join("\n  ", violations));
        }
    }
}
