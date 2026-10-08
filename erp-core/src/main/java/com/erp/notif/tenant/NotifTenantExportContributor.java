package com.erp.notif.tenant;

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
 * NOTIF's part of a tenant data export (tenant-maturity C5, NOTIF srs.md 1.3.0 §6, srs-tenant.md X7): templates, channel
 * settings, the delivery log and the in-app inbox — never {@code CONFIG_JSON} (provider credentials) or
 * {@code VARIABLES_JSON} (links carrying raw tokens) (RULE-TENANT-027). Every statement names {@code TENANT_ID}.
 */
@Component
public class NotifTenantExportContributor implements TenantExportContributor {

    static final List<String> TEMPLATE_COLUMNS = List.of("ID", "TEMPLATE_CODE", "NAME_AR", "NAME_EN", "SUBJECT_AR",
        "SUBJECT_EN", "BODY_AR", "BODY_EN", "ATTACHMENT_FILE_ID", "IS_ACTIVE_FL", "CREATED_BY", "CREATED_AT",
        "UPDATED_BY", "UPDATED_AT");
    static final List<String> CHANNEL_COLUMNS = List.of("ID", "CHANNEL_TYPE_ID", "IS_ENABLED_FL", "CREATED_BY",
        "CREATED_AT", "UPDATED_BY", "UPDATED_AT");
    static final List<String> LOG_COLUMNS = List.of("ID", "RECIPIENT_ID", "CHANNEL_TYPE_ID", "NOTIFICATION_STATUS_ID",
        "MODULE_CODE", "REFERENCE_TYPE", "REFERENCE_ID", "TEMPLATE_FK", "RETRY_COUNT", "ATTEMPTS", "NEXT_ATTEMPT_AT",
        "SENT_AT", "ERROR_MESSAGE", "LAST_ERROR", "CREATED_BY", "CREATED_AT", "UPDATED_BY", "UPDATED_AT");
    static final List<String> INBOX_COLUMNS = List.of("ID", "RECIPIENT_USER_ID", "TITLE_AR", "TITLE_EN", "BODY_AR",
        "BODY_EN", "READ_AT", "REFERENCE_TYPE", "REFERENCE_ID", "CREATED_BY", "CREATED_AT", "UPDATED_BY", "UPDATED_AT");

    private final JdbcTemplate jdbc;

    public NotifTenantExportContributor(DataSource dataSource) {
        this.jdbc = TenantExportJdbc.streaming(dataSource);
    }

    @Override
    public String moduleCode() {
        return "NOTIF";
    }

    @Override
    public long countRows(Long tenantId) {
        return countOfTenant(jdbc, tenantId, "NOTIF_TEMPLATE", "NOTIF_CHANNEL_CONFIG", "NOTIF_LOG", "NOTIF_INBOX");
    }

    @Override
    public void export(TenantExport export) {
        table(export, "NOTIF_TEMPLATE", TEMPLATE_COLUMNS);
        table(export, "NOTIF_CHANNEL_CONFIG", CHANNEL_COLUMNS);
        table(export, "NOTIF_LOG", LOG_COLUMNS);
        table(export, "NOTIF_INBOX", INBOX_COLUMNS);
    }

    private void table(TenantExport export, String table, List<String> columns) {
        export.csv(table, columns, rows -> jdbc.query(selectOfTenant(columns, table, "ID"), rows::addRow,
            export.tenantId()));
    }
}
