package com.erp.tenant.export;

import com.erp.tenant.TenantExport;
import com.erp.tenant.TenantExportContributor;
import com.erp.tenant.TenantExportJdbc;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * The tenant module's part of a tenant data export (tenant-maturity C5, srs-tenant.md X7): the exported tenant's own
 * {@code CORE_TENANT} row — names, profile, suspension facts, branding — never its token cut-off (RULE-TENANT-027).
 * {@code CORE_TENANT} is global, so the predicate is its {@code ID}.
 */
@Component
public class TenantTenantExportContributor implements TenantExportContributor {

    static final String MODULE_CODE = "TENANT";
    static final List<String> CORE_TENANT_COLUMNS = List.of("ID", "CODE", "NAME_AR", "NAME_EN", "STATUS_CODE",
        "CONTACT_EMAIL", "CONTACT_PHONE", "COUNTRY_CODE", "DEFAULT_LOCALE", "TIMEZONE", "NOTES", "SUSPENDED_AT",
        "SUSPENDED_BY", "SUSPENSION_REASON", "LOGO_FILE_ID", "BRAND_COLOR", "CREATED_BY", "CREATED_AT", "UPDATED_BY",
        "UPDATED_AT");

    private final JdbcTemplate jdbc;

    public TenantTenantExportContributor(DataSource dataSource) {
        this.jdbc = TenantExportJdbc.streaming(dataSource);
    }

    @Override
    public String moduleCode() {
        return MODULE_CODE;
    }

    @Override
    public long countRows(Long tenantId) {
        Long rows = jdbc.queryForObject("SELECT COUNT(*) FROM CORE_TENANT WHERE ID = ?", Long.class, tenantId);
        return rows == null ? 0 : rows;
    }

    @Override
    public void export(TenantExport export) {
        export.csv("CORE_TENANT", CORE_TENANT_COLUMNS, rows -> jdbc.query("SELECT " + String.join(", ", CORE_TENANT_COLUMNS)
            + " FROM CORE_TENANT WHERE ID = ? ORDER BY ID", rows::addRow, export.tenantId()));
    }
}
