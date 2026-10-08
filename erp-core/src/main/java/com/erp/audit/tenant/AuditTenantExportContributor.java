package com.erp.audit.tenant;

import static com.erp.tenant.TenantExportJdbc.countOfTenant;
import static com.erp.tenant.TenantExportJdbc.selectOfTenant;

import com.erp.tenant.TenantExport;
import com.erp.tenant.TenantExportContributor;
import com.erp.tenant.TenantExportJdbc;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * The audit log's part of a tenant data export (tenant-maturity C5, srs-tenant.md X7): the tenant's
 * {@code CORE_AUDIT_EVENT} rows; {@code CHANGES} never holds a sensitive field (the audit denylist).
 */
@Component
public class AuditTenantExportContributor implements TenantExportContributor {

    static final List<String> EVENT_COLUMNS = List.of("ID", "OCCURRED_AT", "ACTOR", "ACTOR_REALM", "ACTOR_USER_ID",
        "ACTION", "ENTITY_TYPE", "ENTITY_ID", "SUMMARY_AR", "SUMMARY_EN", "CHANGES", "IP", "USER_AGENT", "REFERENCE",
        "CREATED_BY", "CREATED_AT", "UPDATED_BY", "UPDATED_AT");

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
        export.csv("CORE_AUDIT_EVENT", EVENT_COLUMNS, rows -> jdbc.query(
            selectOfTenant(EVENT_COLUMNS, "CORE_AUDIT_EVENT", "ID"), rows::addRow, export.tenantId()));
    }
}
