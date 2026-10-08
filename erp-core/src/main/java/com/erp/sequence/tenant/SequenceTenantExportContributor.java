package com.erp.sequence.tenant;

import static com.erp.tenant.TenantExportJdbc.countOfTenant;
import static com.erp.tenant.TenantExportJdbc.selectOfTenant;

import com.erp.tenant.TenantExport;
import com.erp.tenant.TenantExportContributor;
import com.erp.tenant.TenantExportJdbc;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** The sequence module's part of a tenant data export (tenant-maturity C5, srs-tenant.md X7): the tenant's number series. */
@Component
public class SequenceTenantExportContributor implements TenantExportContributor {

    static final List<String> SERIES_COLUMNS = List.of("ID", "CODE", "PREFIX", "PATTERN", "RESET_POLICY", "PERIOD_KEY",
        "NEXT_VALUE", "IS_ACTIVE", "CREATED_BY", "CREATED_AT", "UPDATED_BY", "UPDATED_AT");

    private final JdbcTemplate jdbc;

    public SequenceTenantExportContributor(DataSource dataSource) {
        this.jdbc = TenantExportJdbc.streaming(dataSource);
    }

    @Override
    public String moduleCode() {
        return "SEQUENCE";
    }

    @Override
    public long countRows(Long tenantId) {
        return countOfTenant(jdbc, tenantId, "CORE_NUMBER_SERIES");
    }

    @Override
    public void export(TenantExport export) {
        export.csv("CORE_NUMBER_SERIES", SERIES_COLUMNS, rows -> jdbc.query(
            selectOfTenant(SERIES_COLUMNS, "CORE_NUMBER_SERIES", "ID"), rows::addRow, export.tenantId()));
    }
}
