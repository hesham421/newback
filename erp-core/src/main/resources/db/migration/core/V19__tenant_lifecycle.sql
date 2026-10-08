-- ============================================================
-- V19 — Tenant lifecycle facts — tenant-maturity plan package B (erp-core 1.3.0)
-- Target: POSTGRESQL_16 | after V2..V18 | additive only (README: no rename, drop or type change)
--
-- Written from governance/analysis/modules/TENANT/P2/db-script-tenant.md → "Implementation Addendum —
-- erp-core 1.3.0" (DBF-TENANT-039..042). The plan expected V17; the number follows the execution order
-- (docs/DEVIATIONS.md [TM-B]).
--
-- CORE_TENANT gains:
--   SUSPENDED_AT, SUSPENDED_BY, SUSPENSION_REASON — set on suspension, cleared on activation (RULE-TENANT-016)
--   TOKENS_INVALID_BEFORE                         — the token cut-off set on activation; enforced by package C.2
-- All nullable, no default; every existing row gets NULL.
-- ============================================================

ALTER TABLE CORE_TENANT ADD COLUMN SUSPENDED_AT          TIMESTAMPTZ;
ALTER TABLE CORE_TENANT ADD COLUMN SUSPENDED_BY          VARCHAR(100);
ALTER TABLE CORE_TENANT ADD COLUMN SUSPENSION_REASON     VARCHAR(500);
ALTER TABLE CORE_TENANT ADD COLUMN TOKENS_INVALID_BEFORE TIMESTAMPTZ;

COMMENT ON COLUMN CORE_TENANT.SUSPENDED_AT IS 'When the tenant was suspended; NULL while ACTIVE (RULE-TENANT-016); DBF-TENANT-039.';
COMMENT ON COLUMN CORE_TENANT.SUSPENDED_BY IS 'Username of the platform operator who suspended the tenant; DBF-TENANT-040.';
COMMENT ON COLUMN CORE_TENANT.SUSPENSION_REASON IS 'Reason given for the suspension, 3..500 characters; DBF-TENANT-041.';
COMMENT ON COLUMN CORE_TENANT.TOKENS_INVALID_BEFORE IS 'Tokens issued before this instant are refused (set on activation; enforced by package C.2); DBF-TENANT-042.';
