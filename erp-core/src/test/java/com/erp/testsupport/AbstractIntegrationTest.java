package com.erp.testsupport;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Base class for every Spring integration test: the full application context on a random port
 * ({@link CoreTestApplication}, i.e. erp-core wired by its auto-configuration only),
 * profile {@code test} ({@code src/test/resources/application-test.properties}), and a real
 * PostgreSQL 16 shared by all test classes of the JVM. It is never a localhost service, and no
 * Redis or SMTP server is needed. Flyway applies the migration chain once, on the first context.
 *
 * <p>Database backend ({@link TestPostgres}): a {@code postgres:16} Testcontainers container wired by
 * {@code @ServiceConnection} when Docker is available, otherwise an in-process embedded PostgreSQL 16.
 * Force one with {@code -Derp.test.db=testcontainers|embedded} (or env {@code ERP_TEST_DB}).
 *
 * <p>Subclasses add only what they need ({@code @Transactional}, {@code @TestPropertySource}, ...)
 * and must not redeclare {@code @SpringBootTest} or {@code @ActiveProfiles}.
 */
@SpringBootTest(classes = CoreTestApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
@Import(TestcontainersPostgresConfiguration.class)
public abstract class AbstractIntegrationTest {

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        TestPostgres.registerDataSource(registry);
    }
}
