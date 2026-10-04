package com.erp.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.jupiter.api.Test;

/**
 * The core Flyway customizer hands Flyway {@value ErpCoreFlywayAutoConfiguration#CORE_LOCATION}
 * first, followed by the application's own locations. Flyway then normalises the list: it sorts
 * the locations and drops one that lies inside another (it scans recursively), so what is
 * asserted here is the effective guarantee — the core chain is always on Flyway's path, and every
 * application location is kept, once. The order scripts are APPLIED in is decided by version, not
 * by location, which is why core owns {@code V1..V999} and applications {@code V1000+}; the
 * reference app's smoke test proves the applied order (core V2..V9, then V1000).
 */
class ErpCoreFlywayAutoConfigurationTest {

    private final ErpCoreFlywayAutoConfiguration configuration = new ErpCoreFlywayAutoConfiguration();

    @Test
    void addsTheCoreLocationNextToSeparateApplicationLocations() {
        FluentConfiguration flyway = Flyway.configure().locations("classpath:db/app", "classpath:db/app-extra");

        configuration.erpCoreFlywayLocationsCustomizer().customize(flyway);

        assertThat(descriptors(flyway)).containsExactlyInAnyOrder(
            ErpCoreFlywayAutoConfiguration.CORE_LOCATION, "classpath:db/app", "classpath:db/app-extra");
    }

    @Test
    void withFlywaysDefaultLocation_theCoreChainIsCoveredByTheRecursiveDbMigrationScan() {
        FluentConfiguration flyway = Flyway.configure();

        configuration.erpCoreFlywayLocationsCustomizer().customize(flyway);

        // classpath:db/migration/core is a sub-location of classpath:db/migration: Flyway keeps
        // the parent only (one WARN line at startup) and still finds every core script through it.
        assertThat(descriptors(flyway)).containsExactly("classpath:db/migration");
        assertThat(coversCoreLocation(descriptors(flyway))).isTrue();
    }

    @Test
    void neverDuplicatesTheCoreLocationWhenTheApplicationAlreadyListsIt() {
        FluentConfiguration flyway = Flyway.configure()
            .locations("classpath:db/app", ErpCoreFlywayAutoConfiguration.CORE_LOCATION);

        configuration.erpCoreFlywayLocationsCustomizer().customize(flyway);

        assertThat(descriptors(flyway)).containsExactlyInAnyOrder(
            ErpCoreFlywayAutoConfiguration.CORE_LOCATION, "classpath:db/app");
    }

    private static boolean coversCoreLocation(List<String> descriptors) {
        String core = ErpCoreFlywayAutoConfiguration.CORE_LOCATION;
        return descriptors.stream().anyMatch(d -> core.equals(d) || core.startsWith(d + "/"));
    }

    private static List<String> descriptors(FluentConfiguration flyway) {
        return Arrays.stream(flyway.getLocations()).map(Location::getDescriptor).toList();
    }
}
