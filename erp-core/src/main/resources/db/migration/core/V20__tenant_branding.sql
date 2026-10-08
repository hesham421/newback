-- ============================================================
-- V20 — Tenant branding — tenant-maturity plan package E (erp-core 1.3.0)
-- Target: POSTGRESQL_16 | after V2..V19 | additive only (README: no rename, drop or type change)
--
-- Written from governance/analysis/modules/TENANT/P2/db-script-tenant.md → "Implementation Addendum —
-- erp-core 1.3.0" (package E: DBF-TENANT-043..044, CHK_CORE_TENANT_BRAND_COLOR, XM-TENANT-003). The plan
-- expected V18; the number follows the execution order (docs/DEVIATIONS.md [TM-E]).
--
-- CORE_TENANT gains the branding the platform operator sets on PLATFORM_TENANTS (decision D5, ADR-TENANT-005):
--   LOGO_FILE_ID — soft reference to FILE_DOCUMENT.ID, no FK (the logo document lives in the tenant's own rows);
--   BRAND_COLOR  — optional accent colour #RRGGBB.
-- Both nullable, no default; every existing row gets NULL. No registry rows (no module, screen, action or grant).
-- ============================================================

ALTER TABLE CORE_TENANT ADD COLUMN LOGO_FILE_ID BIGINT;
ALTER TABLE CORE_TENANT ADD COLUMN BRAND_COLOR  VARCHAR(7);

ALTER TABLE CORE_TENANT ADD CONSTRAINT CHK_CORE_TENANT_BRAND_COLOR
    CHECK (BRAND_COLOR ~ '^#[0-9A-Fa-f]{6}$');

COMMENT ON COLUMN CORE_TENANT.LOGO_FILE_ID IS 'Logo of the tenant: soft reference (no FK) to the PUBLIC FILE_DOCUMENT stored in this tenant''s rows; XM-TENANT-003, DBF-TENANT-043.';
COMMENT ON COLUMN CORE_TENANT.BRAND_COLOR IS 'Optional brand accent colour #RRGGBB, upper case (CHK_CORE_TENANT_BRAND_COLOR); DBF-TENANT-044.';
