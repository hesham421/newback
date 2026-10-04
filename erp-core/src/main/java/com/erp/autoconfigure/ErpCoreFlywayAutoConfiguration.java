package com.erp.autoconfigure;

import java.util.ArrayList;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.flyway.autoconfigure.FlywayAutoConfiguration;
import org.springframework.boot.flyway.autoconfigure.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;

/**
 * Puts the core migration chain ({@value #CORE_LOCATION}) in front of whatever Flyway locations the
 * application configured ({@code spring.flyway.locations}, default {@code classpath:db/migration}),
 * so an application keeps that property for its own scripts. Flyway sorts the locations and drops
 * one nested in another (it scans recursively), so with the default {@code classpath:db/migration}
 * the core folder is found through its parent. Scripts are applied in version order whatever their
 * location: core owns {@code V1..V999} and applications {@code V1000+} (see the folder's
 * {@code README.md}; the core side is enforced by {@code MigrationNamingTest}).
 */
@AutoConfiguration(before = FlywayAutoConfiguration.class)
@ConditionalOnClass(Flyway.class)
public class ErpCoreFlywayAutoConfiguration {

    public static final String CORE_LOCATION = "classpath:db/migration/core";

    @Bean
    public FlywayConfigurationCustomizer erpCoreFlywayLocationsCustomizer() {
        return configuration -> {
            List<String> locations = new ArrayList<>();
            locations.add(CORE_LOCATION);
            for (Location location : configuration.getLocations()) {
                String descriptor = location.getDescriptor();
                if (!descriptor.equals(CORE_LOCATION)) {
                    locations.add(descriptor);
                }
            }
            configuration.locations(locations.toArray(String[]::new));
        };
    }
}
