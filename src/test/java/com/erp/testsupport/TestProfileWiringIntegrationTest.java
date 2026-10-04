package com.erp.testsupport;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;

/**
 * Pins the test profile's infrastructure: a PostgreSQL 16 database that is not the localhost
 * default, no Redis template (so FILE falls back to the in-memory token store), and the Flyway
 * chain applied.
 */
class TestProfileWiringIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ApplicationContext context;
    @Autowired
    private DataSource dataSource;

    @Test
    void databaseIsTheSharedTestPostgres16_withTheMigrationChainApplied() throws Exception {
        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.getMetaData().getDatabaseProductName()).isEqualTo("PostgreSQL");
            assertThat(connection.getMetaData().getDatabaseMajorVersion()).isEqualTo(16);
            assertThat(connection.getMetaData().getURL()).doesNotContain("localhost:5432");
            try (var rs = connection.createStatement().executeQuery(
                    "select count(*) from flyway_schema_history where success = false")) {
                rs.next();
                assertThat(rs.getInt(1)).isZero();
            }
        }
    }

    @Test
    void noRedisTemplate_andTheInMemoryDownloadTokenStoreIsUsed() {
        assertThat(context.getBeanNamesForType(StringRedisTemplate.class)).isEmpty();
        // By bean name (imported classes are named by FQCN): a type reference into com.erp.file
        // from here would cross the module boundary CrossModuleBoundaryArchTest enforces.
        assertThat(context.containsBean("com.erp.file.service.InMemoryDownloadTokenStore")).isTrue();
        assertThat(context.containsBean("com.erp.file.service.RedisDownloadTokenStore")).isFalse();
    }
}
