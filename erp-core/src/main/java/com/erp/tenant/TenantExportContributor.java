package com.erp.tenant;

/**
 * tenant-maturity C5 (XM-TENANT-004) — each module writes its own rows of a tenant into a data export, so the tenant
 * module never reads another module's tables (the mirror of {@link TenantProvisioningContributor}). Both methods run
 * inside {@code TenantContext.callAs(tenantId)} in one read-only snapshot transaction and never write; every statement
 * names {@code TENANT_ID} (RULE-TENANT-011), streams ({@link TenantExportJdbc}) and exports no secret (RULE-TENANT-027).
 */
public interface TenantExportContributor {

    /** The archive folder of this module's files: {@code ^[A-Z][A-Z0-9_]{0,31}$}, unique among the contributors. */
    String moduleCode();

    /** How many rows {@link #export} will write for {@code tenantId}; counted before anything is written. */
    long countRows(Long tenantId);

    /** Writes this module's files of {@code export.tenantId()} through {@link TenantExport#csv}, ordered by primary key. */
    void export(TenantExport export);
}
