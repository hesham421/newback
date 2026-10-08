package com.erp.sec.tenant;

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
 * SEC's part of a tenant data export (tenant-maturity C5, srs-sec.md 1.3.0 §13, srs-tenant.md X7): nine files of the
 * exported tenant. Never {@code PASSWORD_HASH}, the session's {@code TOKEN_REF} or the two token tables
 * (RULE-TENANT-027); the grant files name the global registry entries by code. Every statement names {@code TENANT_ID}.
 */
@Component
public class SecTenantExportContributor implements TenantExportContributor {

    static final List<String> USER_COLUMNS = List.of("USER_PK", "USERNAME", "EMAIL", "REALM", "FULL_NAME_AR",
        "FULL_NAME_EN", "STATUS_CODE", "IS_ACTIVE_FL", "LAST_LOGIN_AT", "PHONE", "JOB_TITLE_AR", "JOB_TITLE_EN",
        "PREFERRED_LOCALE", "PHOTO_FILE_ID", "PASSWORD_CHANGE_REQUIRED_FL", "PASSWORD_CHANGED_AT", "CREATED_BY",
        "CREATED_AT", "UPDATED_BY", "UPDATED_AT");
    static final List<String> ROLE_COLUMNS = List.of("ROLE_PK", "CODE", "NAME_AR", "NAME_EN", "DESCRIPTION_AR",
        "DESCRIPTION_EN", "IS_SUPER", "IS_ACTIVE_FL", "CREATED_BY", "CREATED_AT", "UPDATED_BY", "UPDATED_AT");
    static final List<String> USER_ROLE_COLUMNS = List.of("USER_ROLE_PK", "USER_ID", "ROLE_ID", "ASSIGNED_BY",
        "ASSIGNED_AT", "CREATED_BY", "CREATED_AT", "UPDATED_BY", "UPDATED_AT");
    static final List<String> MODULE_GRANT_COLUMNS = List.of("ROLE_MODULE_GRANT_PK", "ROLE_ID", "MODULE_ID",
        "MODULE_CODE", "GRANTED_BY", "GRANTED_AT", "CREATED_BY", "CREATED_AT", "UPDATED_BY", "UPDATED_AT");
    static final List<String> SCREEN_GRANT_COLUMNS = List.of("ROLE_SCREEN_GRANT_PK", "ROLE_ID", "SCREEN_ID",
        "PAGE_CODE", "GRANTED_BY", "GRANTED_AT", "CREATED_BY", "CREATED_AT", "UPDATED_BY", "UPDATED_AT");
    static final List<String> ACTION_GRANT_COLUMNS = List.of("ROLE_ACTION_GRANT_PK", "ROLE_ID", "ACTION_ID",
        "PERMISSION_CODE", "GRANTED_BY", "GRANTED_AT", "CREATED_BY", "CREATED_AT", "UPDATED_BY", "UPDATED_AT");
    static final List<String> SESSION_COLUMNS = List.of("ACTIVE_SESSION_PK", "USER_ID", "STARTED_AT",
        "LAST_ACTIVITY_AT", "IP_ADDRESS", "TERMINATED_AT", "TERMINATED_BY", "CREATED_BY", "CREATED_AT", "UPDATED_BY",
        "UPDATED_AT");
    static final List<String> AUDIT_LOG_COLUMNS = List.of("AUDIT_LOG_PK", "EVENT_TYPE_CODE", "ACTOR_USER_ID",
        "OCCURRED_AT", "TARGET_REF", "DETAILS_AR", "DETAILS_EN", "IP_ADDRESS", "CREATED_BY", "CREATED_AT", "UPDATED_BY",
        "UPDATED_AT");
    static final List<String> SIGNUP_COLUMNS = List.of("SIGNUP_REQUEST_PK", "EMAIL", "FULL_NAME_AR", "FULL_NAME_EN",
        "SUBMITTED_AT", "STATUS_CODE", "REVIEWED_BY", "REVIEWED_AT", "CREATED_BY", "CREATED_AT", "UPDATED_BY",
        "UPDATED_AT");

    private static final String GRANT_AUDIT = ", g.GRANTED_BY, g.GRANTED_AT, g.CREATED_BY, g.CREATED_AT, g.UPDATED_BY,"
        + " g.UPDATED_AT";

    private final JdbcTemplate jdbc;

    public SecTenantExportContributor(DataSource dataSource) {
        this.jdbc = TenantExportJdbc.streaming(dataSource);
    }

    @Override
    public String moduleCode() {
        return "SEC";
    }

    @Override
    public long countRows(Long tenantId) {
        return countOfTenant(jdbc, tenantId, "SEC_USER", "SEC_ROLE", "SEC_USER_ROLE", "SEC_ROLE_MODULE_GRANT",
            "SEC_ROLE_SCREEN_GRANT", "SEC_ROLE_ACTION_GRANT", "SEC_ACTIVE_SESSION", "SEC_AUDIT_LOG", "SEC_SIGNUP_REQUEST");
    }

    @Override
    public void export(TenantExport export) {
        table(export, "SEC_USER", USER_COLUMNS, selectOfTenant(USER_COLUMNS, "SEC_USER", "USER_PK"));
        table(export, "SEC_ROLE", ROLE_COLUMNS, selectOfTenant(ROLE_COLUMNS, "SEC_ROLE", "ROLE_PK"));
        table(export, "SEC_USER_ROLE", USER_ROLE_COLUMNS,
            selectOfTenant(USER_ROLE_COLUMNS, "SEC_USER_ROLE", "USER_ROLE_PK"));
        table(export, "SEC_ROLE_MODULE_GRANT", MODULE_GRANT_COLUMNS,
            "SELECT g.ROLE_MODULE_GRANT_PK, g.ROLE_ID, g.MODULE_ID, m.CODE AS MODULE_CODE" + GRANT_AUDIT
                + " FROM SEC_ROLE_MODULE_GRANT g LEFT JOIN SEC_MODULE_REG m ON m.MODULE_REG_PK = g.MODULE_ID"
                + " WHERE g.TENANT_ID = ? ORDER BY g.ROLE_MODULE_GRANT_PK");
        table(export, "SEC_ROLE_SCREEN_GRANT", SCREEN_GRANT_COLUMNS,
            "SELECT g.ROLE_SCREEN_GRANT_PK, g.ROLE_ID, g.SCREEN_ID, s.PAGE_CODE" + GRANT_AUDIT
                + " FROM SEC_ROLE_SCREEN_GRANT g LEFT JOIN SEC_SCREEN_REG s ON s.SCREEN_REG_PK = g.SCREEN_ID"
                + " WHERE g.TENANT_ID = ? ORDER BY g.ROLE_SCREEN_GRANT_PK");
        table(export, "SEC_ROLE_ACTION_GRANT", ACTION_GRANT_COLUMNS,
            "SELECT g.ROLE_ACTION_GRANT_PK, g.ROLE_ID, g.ACTION_ID, a.PERMISSION_CODE" + GRANT_AUDIT
                + " FROM SEC_ROLE_ACTION_GRANT g LEFT JOIN SEC_ACTION_REG a ON a.ACTION_REG_PK = g.ACTION_ID"
                + " WHERE g.TENANT_ID = ? ORDER BY g.ROLE_ACTION_GRANT_PK");
        table(export, "SEC_ACTIVE_SESSION", SESSION_COLUMNS,
            selectOfTenant(SESSION_COLUMNS, "SEC_ACTIVE_SESSION", "ACTIVE_SESSION_PK"));
        table(export, "SEC_AUDIT_LOG", AUDIT_LOG_COLUMNS,
            selectOfTenant(AUDIT_LOG_COLUMNS, "SEC_AUDIT_LOG", "AUDIT_LOG_PK"));
        table(export, "SEC_SIGNUP_REQUEST", SIGNUP_COLUMNS,
            selectOfTenant(SIGNUP_COLUMNS, "SEC_SIGNUP_REQUEST", "SIGNUP_REQUEST_PK"));
    }

    private void table(TenantExport export, String fileName, List<String> columns, String sql) {
        export.csv(fileName, columns, rows -> jdbc.query(sql, rows::addRow, export.tenantId()));
    }
}
