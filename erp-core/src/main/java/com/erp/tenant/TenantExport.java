package com.erp.tenant;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

/**
 * The tenant data export a {@link TenantExportContributor} writes into (tenant-maturity C5, XM-TENANT-004): one CSV
 * file per table, {@code {moduleCode}/{fileName}.csv}, UTF-8 with a byte-order mark, RFC 4180 (srs-tenant.md X6).
 */
public interface TenantExport {

    /** {@code CORE_TENANT.ID} of the tenant being exported. */
    Long tenantId();

    /** Its code. */
    String tenantCode();

    /**
     * Writes one file: {@code fileName} ({@code ^[A-Z][A-Z0-9_]{0,63}$}, unique within the module), a header record of
     * {@code columns}, then every record {@code rows} adds. A file without rows still has its header.
     */
    void csv(String fileName, List<String> columns, Consumer<Rows> rows);

    /** The record sink of one file. */
    interface Rows {

        /**
         * Writes the current row of {@code resultSet}: every selected column in select-list order, which must match the
         * file's columns one to one. Usable as a {@code RowCallbackHandler}: {@code jdbc.query(sql, rows::addRow, id)}.
         */
        void addRow(ResultSet resultSet) throws SQLException;

        /** Writes one record from {@code values}, in column order. */
        void add(Object... values);
    }
}
