package com.erp.tenant;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The JDBC helpers of the {@link TenantExportContributor}s (tenant-maturity C5): result sets streamed in pages, and the
 * plain one-table statements, which always name {@code TENANT_ID} (RULE-TENANT-011).
 */
public final class TenantExportJdbc {

    /** Rows fetched per round trip; PostgreSQL streams a result set only with a fetch size, inside a transaction. */
    public static final int FETCH_SIZE = 1_000;

    private TenantExportJdbc() {
        throw new UnsupportedOperationException("Utility class — cannot be instantiated");
    }

    /** A {@link JdbcTemplate} on {@code dataSource} (it joins the export's transaction) with {@link #FETCH_SIZE}. */
    public static JdbcTemplate streaming(DataSource dataSource) {
        JdbcTemplate template = new JdbcTemplate(dataSource);
        template.setFetchSize(FETCH_SIZE);
        return template;
    }

    /** {@code SELECT <columns> FROM <table> WHERE TENANT_ID = ? ORDER BY <primaryKey>} — the select list is the header. */
    public static String selectOfTenant(List<String> columns, String table, String primaryKey) {
        return "SELECT " + String.join(", ", columns) + " FROM " + table + " WHERE TENANT_ID = ? ORDER BY " + primaryKey;
    }

    /** The tenant's rows in {@code tables}, one {@code COUNT(*)} per table, each naming {@code TENANT_ID}. */
    public static long countOfTenant(JdbcTemplate jdbc, Long tenantId, String... tables) {
        String sql = Arrays.stream(tables)
            .map(table -> "(SELECT COUNT(*) FROM " + table + " WHERE TENANT_ID = ?)")
            .collect(Collectors.joining(" + ", "SELECT ", ""));
        Long rows = jdbc.queryForObject(sql, Long.class, Collections.nCopies(tables.length, tenantId).toArray());
        return rows == null ? 0 : rows;
    }
}
