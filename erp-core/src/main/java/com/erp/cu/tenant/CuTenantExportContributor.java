package com.erp.cu.tenant;

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
 * CU's part of a tenant data export (tenant-maturity C5, srs-cu.md 1.3.0, srs-tenant.md X7): the tenant's own
 * {@code CU_APP_CONFIGURATION} overrides; the platform defaults (NULL tenant) are not the tenant's data. The entity is
 * global, so {@code TENANT_ID = ?} is the only scope (RULE-TENANT-011).
 */
@Component
public class CuTenantExportContributor implements TenantExportContributor {

    static final List<String> CONFIGURATION_COLUMNS = List.of("ID", "CONFIG_KEY", "CONFIG_VALUE", "NOTES",
        "IS_ACTIVE_FL", "CREATED_BY", "CREATED_AT", "UPDATED_BY", "UPDATED_AT");

    private final JdbcTemplate jdbc;

    public CuTenantExportContributor(DataSource dataSource) {
        this.jdbc = TenantExportJdbc.streaming(dataSource);
    }

    @Override
    public String moduleCode() {
        return "CU";
    }

    @Override
    public long countRows(Long tenantId) {
        return countOfTenant(jdbc, tenantId, "CU_APP_CONFIGURATION");
    }

    @Override
    public void export(TenantExport export) {
        export.csv("CU_APP_CONFIGURATION", CONFIGURATION_COLUMNS, rows -> jdbc.query(
            selectOfTenant(CONFIGURATION_COLUMNS, "CU_APP_CONFIGURATION", "ID"), rows::addRow, export.tenantId()));
    }
}
