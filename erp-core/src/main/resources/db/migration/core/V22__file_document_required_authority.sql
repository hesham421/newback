-- =====================================================================================================
-- V22 — FILE: restricted documents (tenant-maturity C5, review round 1)
-- FILE RULE-FILE-012; governance/analysis/modules/FILE/P2/db-script.md 1.3.0 package C5.
-- A document whose REQUIRED_AUTHORITY is set is served only to callers holding that authority (the private
-- store's tenant export archives: PLATFORM_TENANT_MANAGE); NULL = an ordinary document.
-- Additive only: one nullable column, no default, no backfill (every existing document stays unrestricted).
-- =====================================================================================================

ALTER TABLE FILE_DOCUMENT ADD COLUMN REQUIRED_AUTHORITY VARCHAR(100);

COMMENT ON COLUMN FILE_DOCUMENT.REQUIRED_AUTHORITY IS 'RULE-FILE-012 — authority a caller must hold to see or act on the document (besides the FILE permission); NULL = unrestricted. Set by the private store only; immutable.';
