-- ============================================================
-- V18 — Tenant profile — tenant-maturity plan package B (erp-core 1.3.0)
-- Target: POSTGRESQL_16 | after V2..V17 | additive only (README: no rename, drop or type change)
--
-- Written from governance/analysis/modules/TENANT/P2/db-script-tenant.md → "Implementation Addendum —
-- erp-core 1.3.0" (DBF-TENANT-033..038, CHK_CORE_TENANT_LOCALE). The plan expected V16; the number follows
-- the execution order (docs/DEVIATIONS.md [TM-B]).
--
-- CORE_TENANT gains the profile the platform operator edits through PUT /api/v1/platform/tenants/{id}:
--   CONTACT_EMAIL, CONTACT_PHONE, COUNTRY_CODE (ISO 3166-1 alpha-2), DEFAULT_LOCALE ('ar' | 'en'),
--   TIMEZONE (IANA zone id), NOTES — all nullable, no default; every existing row gets NULL.
-- ============================================================

ALTER TABLE CORE_TENANT ADD COLUMN CONTACT_EMAIL  VARCHAR(255);
ALTER TABLE CORE_TENANT ADD COLUMN CONTACT_PHONE  VARCHAR(30);
ALTER TABLE CORE_TENANT ADD COLUMN COUNTRY_CODE   VARCHAR(2);
ALTER TABLE CORE_TENANT ADD COLUMN DEFAULT_LOCALE VARCHAR(5);
ALTER TABLE CORE_TENANT ADD COLUMN TIMEZONE       VARCHAR(64);
ALTER TABLE CORE_TENANT ADD COLUMN NOTES          VARCHAR(1000);

ALTER TABLE CORE_TENANT ADD CONSTRAINT CHK_CORE_TENANT_LOCALE
    CHECK (DEFAULT_LOCALE IS NULL OR DEFAULT_LOCALE IN ('ar', 'en'));

COMMENT ON COLUMN CORE_TENANT.CONTACT_EMAIL IS 'Contact e-mail of the organisation; DBF-TENANT-033.';
COMMENT ON COLUMN CORE_TENANT.CONTACT_PHONE IS 'Contact phone, E.164-ish (optional +, digits, spaces, hyphens); DBF-TENANT-034.';
COMMENT ON COLUMN CORE_TENANT.COUNTRY_CODE IS 'ISO 3166-1 alpha-2 country code, upper case; DBF-TENANT-035.';
COMMENT ON COLUMN CORE_TENANT.DEFAULT_LOCALE IS 'Default UI language of the tenant, ar or en (CHK_CORE_TENANT_LOCALE); DBF-TENANT-036.';
COMMENT ON COLUMN CORE_TENANT.TIMEZONE IS 'IANA time-zone id, e.g. Asia/Riyadh; DBF-TENANT-037.';
COMMENT ON COLUMN CORE_TENANT.NOTES IS 'Free-text notes of the platform operator; DBF-TENANT-038.';
