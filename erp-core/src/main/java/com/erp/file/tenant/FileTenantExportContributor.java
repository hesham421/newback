package com.erp.file.tenant;

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
 * FILE's part of a tenant data export (tenant-maturity C5, FILE srs.md 1.3.0 §9.3, srs-tenant.md X7): the tenant's
 * categories and document <b>metadata</b> — never {@code FILE_CONTENT}, {@code STORAGE_REF} or {@code PUBLIC_SLUG}
 * (RULE-TENANT-027). Every statement names {@code TENANT_ID} (RULE-TENANT-011).
 */
@Component
public class FileTenantExportContributor implements TenantExportContributor {

    static final List<String> CATEGORY_COLUMNS = List.of("ID", "CATEGORY_CODE", "NAME_AR", "NAME_EN", "MAX_SIZE_BYTES",
        "ALLOWED_CONTENT_TYPES", "ALLOW_PUBLIC", "IS_ACTIVE_FL", "CREATED_BY", "CREATED_AT", "UPDATED_BY", "UPDATED_AT");
    static final List<String> DOCUMENT_COLUMNS = List.of("ID", "OWNER_TYPE", "OWNER_ID", "MODULE_CODE", "FILE_NAME",
        "CONTENT_TYPE", "FILE_SIZE", "FILE_TYPE_ID", "FILE_STATUS_ID", "FILE_CATEGORY_FK", "VISIBILITY", "STORAGE_PROVIDER",
        "CONTENT_HASH", "CREATED_BY", "CREATED_AT", "UPDATED_BY", "UPDATED_AT");

    private final JdbcTemplate jdbc;

    public FileTenantExportContributor(DataSource dataSource) {
        this.jdbc = TenantExportJdbc.streaming(dataSource);
    }

    @Override
    public String moduleCode() {
        return "FILE";
    }

    @Override
    public long countRows(Long tenantId) {
        return countOfTenant(jdbc, tenantId, "FILE_CATEGORY", "FILE_DOCUMENT");
    }

    @Override
    public void export(TenantExport export) {
        Long tenantId = export.tenantId();
        export.csv("FILE_CATEGORY", CATEGORY_COLUMNS, rows -> jdbc.query(
            selectOfTenant(CATEGORY_COLUMNS, "FILE_CATEGORY", "ID"), rows::addRow, tenantId));
        export.csv("FILE_DOCUMENT", DOCUMENT_COLUMNS, rows -> jdbc.query(
            selectOfTenant(DOCUMENT_COLUMNS, "FILE_DOCUMENT", "ID"), rows::addRow, tenantId));
    }
}
