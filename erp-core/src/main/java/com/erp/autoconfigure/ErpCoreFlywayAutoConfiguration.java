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
 * so an application keeps that property for its own scripts. Version ranges (core {@code V1..V999},
 * applications {@code V1000+}) are enforced in step 04.
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
