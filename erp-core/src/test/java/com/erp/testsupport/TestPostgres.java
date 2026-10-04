package com.erp.testsupport;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Locale;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * The one PostgreSQL 16 database the integration tests of this JVM share. Started lazily, exactly
 * once, and never a localhost service:
 *
 * <ul>
 *   <li>{@link Backend#TESTCONTAINERS} (primary) — a {@code postgres:16} container, exposed to
 *       Spring through {@code @ServiceConnection} (see {@link TestcontainersPostgresConfiguration});</li>
 *   <li>{@link Backend#EMBEDDED} (fallback for machines without Docker) — an in-process PostgreSQL 16
 *       (zonky embedded-postgres binaries) on a random free port, exposed through
 *       {@code spring.datasource.*} dynamic properties.</li>
 * </ul>
 *
 * <p>Selection: system property {@value #SELECTOR_PROPERTY} or environment variable
 * {@value #SELECTOR_ENV} = {@code testcontainers} | {@code embedded} | {@code auto} (default).
 * {@code auto} picks Testcontainers when Docker is reachable, otherwise embedded. Forcing
 * {@code testcontainers} without Docker fails fast instead of silently falling back.
 */
public final class TestPostgres {

    public static final String SELECTOR_PROPERTY = "erp.test.db";
    public static final String SELECTOR_ENV = "ERP_TEST_DB";
    public static final String IMAGE = "postgres:16";

    /** Which kind of PostgreSQL 16 this JVM's tests run against. */
    public enum Backend { TESTCONTAINERS, EMBEDDED }

    private static Backend backend;
    private static PostgreSQLContainer container;
    private static EmbeddedPostgres embedded;

    private TestPostgres() {
    }

    /** The backend this JVM uses; resolved once. */
    public static synchronized Backend backend() {
        if (backend == null) {
            backend = resolveBackend();
            // Printed (not logged) so the choice is visible whatever the test profile's log level.
            System.out.println("[TestPostgres] integration-test database backend: " + backend);
        }
        return backend;
    }

    /** The shared, started container. Only valid for {@link Backend#TESTCONTAINERS}. */
    public static synchronized PostgreSQLContainer container() {
        if (backend() != Backend.TESTCONTAINERS) {
            throw new IllegalStateException("Testcontainers backend not selected (backend=" + backend + ")");
        }
        if (container == null) {
            PostgreSQLContainer created = new SharedPostgreSQLContainer(IMAGE);
            created.start();
            container = created;
        }
        return container;
    }

    /** The shared, started embedded server. Only valid for {@link Backend#EMBEDDED}. */
    public static synchronized EmbeddedPostgres embedded() {
        if (backend() != Backend.EMBEDDED) {
            throw new IllegalStateException("Embedded backend not selected (backend=" + backend + ")");
        }
        if (embedded == null) {
            EmbeddedPostgres started;
            try {
                started = EmbeddedPostgres.builder()
                    // Pin server encoding/locale: initdb otherwise inherits the OS locale, which on
                    // non-UTF-8 Windows code pages (e.g. Cp1256) mangles the Arabic seed data.
                    .setLocaleConfig("encoding", "UTF8")
                    .setLocaleConfig("locale", "C")
                    // zonky defaults to max_connections=300; the postgres:16 image CI uses keeps
                    // PostgreSQL's 100. Same limit here, so the cached contexts' pools cannot fit
                    // locally and then exhaust the container in CI.
                    .setServerConfig("max_connections", "100")
                    .start();
            } catch (IOException e) {
                throw new UncheckedIOException("Could not start embedded PostgreSQL", e);
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> closeQuietly(started),
                "embedded-postgres-shutdown"));
            embedded = started;
        }
        return embedded;
    }

    /**
     * Registers {@code spring.datasource.*} for the embedded backend. For Testcontainers nothing is
     * registered here: the {@code @ServiceConnection} bean supplies the connection details.
     */
    public static void registerDataSource(DynamicPropertyRegistry registry) {
        if (backend() != Backend.EMBEDDED) {
            return;
        }
        registry.add("spring.datasource.url", () -> embedded().getJdbcUrl("postgres", "postgres"));
        registry.add("spring.datasource.username", () -> "postgres");
        registry.add("spring.datasource.password", () -> "postgres");
    }

    /** JDBC URL of the shared database, whichever backend is selected (for tests that build their own context). */
    public static String jdbcUrl() {
        return backend() == Backend.TESTCONTAINERS
            ? container().getJdbcUrl() : embedded().getJdbcUrl("postgres", "postgres");
    }

    /** User name of the shared database. */
    public static String username() {
        return backend() == Backend.TESTCONTAINERS ? container().getUsername() : "postgres";
    }

    /** Password of the shared database. */
    public static String password() {
        return backend() == Backend.TESTCONTAINERS ? container().getPassword() : "postgres";
    }

    private static Backend resolveBackend() {
        String selector = System.getProperty(SELECTOR_PROPERTY);
        if (selector == null || selector.isBlank()) {
            selector = System.getenv(SELECTOR_ENV);
        }
        String mode = (selector == null || selector.isBlank()) ? "auto" : selector.trim().toLowerCase(Locale.ROOT);
        return switch (mode) {
            case "embedded" -> Backend.EMBEDDED;
            case "testcontainers" -> {
                if (!dockerAvailable()) {
                    throw new IllegalStateException(SELECTOR_PROPERTY + "=testcontainers but no Docker "
                        + "environment is available; start Docker or use " + SELECTOR_PROPERTY + "=embedded");
                }
                yield Backend.TESTCONTAINERS;
            }
            case "auto" -> dockerAvailable() ? Backend.TESTCONTAINERS : Backend.EMBEDDED;
            default -> throw new IllegalStateException("Unknown " + SELECTOR_PROPERTY + " value '" + selector
                + "' (expected testcontainers, embedded or auto)");
        };
    }

    private static boolean dockerAvailable() {
        try {
            return DockerClientFactory.instance().isDockerAvailable();
        } catch (RuntimeException | LinkageError e) {
            return false;
        }
    }

    private static void closeQuietly(EmbeddedPostgres server) {
        try {
            server.close();
        } catch (IOException e) {
            // The JVM is exiting; nothing useful can be done with a failed shutdown here.
            System.err.println("embedded PostgreSQL did not shut down cleanly: " + e.getMessage());
        }
    }

    /** Spring condition: true when this JVM uses the Testcontainers backend. */
    public static final class TestcontainersSelected implements Condition {
        @Override
        public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
            return backend() == Backend.TESTCONTAINERS;
        }
    }

    /**
     * The per-JVM container outlives every application context. Spring Boot closes a container bean
     * when its context closes (e.g. after {@code @DirtiesContext}), which would pull the database
     * from under the other cached contexts, so {@code stop()} is a no-op and the container is
     * removed by the Testcontainers Ryuk reaper at JVM exit (the documented singleton-container
     * pattern).
     */
    private static final class SharedPostgreSQLContainer extends PostgreSQLContainer {
        SharedPostgreSQLContainer(String image) {
            super(image);
        }

        @Override
        public void stop() {
            // Intentionally empty: see the class javadoc.
        }
    }
}
