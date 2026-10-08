package com.erp.audit.tenant;

import static com.erp.tenant.TenantExportJdbc.countOfTenant;

import com.erp.tenant.TenantExport;
import com.erp.tenant.TenantExportContributor;
import com.erp.tenant.TenantExportJdbc;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * The audit log's part of a tenant data export (tenant-maturity C5, srs-tenant.md X7, X14): the tenant's
 * {@code CORE_AUDIT_EVENT} rows; {@code CHANGES} never holds a sensitive field (the audit denylist), and a
 * {@code FILE_DOCUMENT} row's {@code storageRef} / {@code publicSlug} changes (audited before 1.3.0's ignore list) are removed.
 */
@Component
public class AuditTenantExportContributor implements TenantExportContributor {

    static final List<String> EVENT_COLUMNS = List.of("ID", "OCCURRED_AT", "ACTOR", "ACTOR_REALM", "ACTOR_USER_ID",
        "ACTION", "ENTITY_TYPE", "ENTITY_ID", "SUMMARY_AR", "SUMMARY_EN", "CHANGES", "IP", "USER_AGENT", "REFERENCE",
        "CREATED_BY", "CREATED_AT", "UPDATED_BY", "UPDATED_AT");

    /** X14: {@code CHANGES} without the FILE_DOCUMENT fields that are never exported; order kept, NULL when none remains. */
    private static final String SCRUBBED_CHANGES = "CASE WHEN ENTITY_TYPE = 'FILE_DOCUMENT' AND jsonb_typeof(CHANGES) = 'array'"
        + " THEN (SELECT jsonb_agg(x.e ORDER BY x.o) FROM jsonb_array_elements(CHANGES) WITH ORDINALITY AS x(e, o)"
        + " WHERE x.e->>'field' IS NULL OR x.e->>'field' NOT IN ('storageRef', 'publicSlug'))"
        + " ELSE CHANGES END AS CHANGES";

    private final JdbcTemplate jdbc;

    public AuditTenantExportContributor(DataSource dataSource) {
        this.jdbc = TenantExportJdbc.streaming(dataSource);
    }

    @Override
    public String moduleCode() {
        return "AUDIT";
    }

    @Override
    public long countRows(Long tenantId) {
        return countOfTenant(jdbc, tenantId, "CORE_AUDIT_EVENT");
    }

    @Override
    public void export(TenantExport export) {
        String columns = String.join(", ", EVENT_COLUMNS).replace("CHANGES", SCRUBBED_CHANGES);
        export.csv("CORE_AUDIT_EVENT", EVENT_COLUMNS, rows -> jdbc.query("SELECT " + columns
            + " FROM CORE_AUDIT_EVENT WHERE TENANT_ID = ? ORDER BY ID", rows::addRow, export.tenantId()));
    }
}
