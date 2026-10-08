package com.erp.mdl.tenant;

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
 * MDL's part of a tenant data export (tenant-maturity C5, srs-mdl.md 1.3.0, srs-tenant.md X7): the tenant's lookup types
 * and values; the value file adds its type's {@code KEY}, joined within the same tenant (both tables name
 * {@code TENANT_ID}, RULE-TENANT-011).
 */
@Component
public class MdlTenantExportContributor implements TenantExportContributor {

    static final List<String> TYPE_COLUMNS = List.of("LOOKUP_TYPE_PK", "KEY", "OWNER_MODULE_CODE", "NAME_AR",
        "NAME_EN", "IS_ACTIVE_FL", "CREATED_BY", "CREATED_AT", "UPDATED_BY", "UPDATED_AT");
    static final List<String> VALUE_COLUMNS = List.of("LOOKUP_VALUE_PK", "LOOKUP_TYPE_ID", "LOOKUP_TYPE_KEY", "CODE",
        "NAME_AR", "NAME_EN", "SORT_ORDER", "IS_ACTIVE_FL", "CREATED_BY", "CREATED_AT", "UPDATED_BY", "UPDATED_AT");

    private static final String VALUE_SQL = "SELECT v.LOOKUP_VALUE_PK, v.LOOKUP_TYPE_ID, t.KEY AS LOOKUP_TYPE_KEY, v.CODE,"
        + " v.NAME_AR, v.NAME_EN, v.SORT_ORDER, v.IS_ACTIVE_FL, v.CREATED_BY, v.CREATED_AT, v.UPDATED_BY, v.UPDATED_AT"
        + " FROM MDL_LOOKUP_VALUE v LEFT JOIN MDL_LOOKUP_TYPE t ON t.LOOKUP_TYPE_PK = v.LOOKUP_TYPE_ID AND t.TENANT_ID = ?"
        + " WHERE v.TENANT_ID = ? ORDER BY v.LOOKUP_VALUE_PK";

    private final JdbcTemplate jdbc;

    public MdlTenantExportContributor(DataSource dataSource) {
        this.jdbc = TenantExportJdbc.streaming(dataSource);
    }

    @Override
    public String moduleCode() {
        return "MDL";
    }

    @Override
    public long countRows(Long tenantId) {
        return countOfTenant(jdbc, tenantId, "MDL_LOOKUP_TYPE", "MDL_LOOKUP_VALUE");
    }

    @Override
    public void export(TenantExport export) {
        Long tenantId = export.tenantId();
        export.csv("MDL_LOOKUP_TYPE", TYPE_COLUMNS, rows -> jdbc.query(
            selectOfTenant(TYPE_COLUMNS, "MDL_LOOKUP_TYPE", "LOOKUP_TYPE_PK"), rows::addRow, tenantId));
        export.csv("MDL_LOOKUP_VALUE", VALUE_COLUMNS, rows -> jdbc.query(VALUE_SQL, rows::addRow, tenantId, tenantId));
    }
}
