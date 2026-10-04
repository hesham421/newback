package com.erp.testsupport;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Exposes the shared per-JVM {@code postgres:16} container to Spring Boot via
 * {@code @ServiceConnection}: Boot derives the datasource (and therefore Flyway) connection details
 * from it, overriding the {@code localhost} defaults in {@code application.properties}.
 *
 * <p>Active only when {@link TestPostgres} selected the Testcontainers backend. The annotation sits
 * on a conditional bean rather than on a static field of {@link AbstractIntegrationTest} because
 * Boot resolves field-level {@code @ServiceConnection} unconditionally (it requires a non-null
 * container and asks Docker for its image name), which would make Docker mandatory.
 */
@TestConfiguration(proxyBeanMethods = false)
@Conditional(TestPostgres.TestcontainersSelected.class)
class TestcontainersPostgresConfiguration {

    @Bean
    @ServiceConnection
    PostgreSQLContainer postgresContainer() {
        return TestPostgres.container();
    }
}
