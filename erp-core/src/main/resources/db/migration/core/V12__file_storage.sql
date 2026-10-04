-- ============================================================
-- V12 — File storage SPI and public files (erp-core step 07)
--   FILE_DOCUMENT: STORAGE_PROVIDER / STORAGE_REF (where the content lives: DB | LOCAL | S3),
--                  VISIBILITY (PRIVATE | PUBLIC), PUBLIC_SLUG (random, unique per tenant),
--                  CONTENT_HASH (SHA-256 hex, the public ETag); FILE_CONTENT becomes nullable
--                  (only the DB provider fills it).
--   FILE_CATEGORY: ALLOW_PUBLIC — only such categories may hold PUBLIC files.
--   Permission FILE:DOCUMENT:PUBLISH (PATCH /api/v1/files/{id}/visibility) on screen FILE_BROWSER,
--   granted to the PLATFORM tenant's SYS_ADMIN and FILE_ADMIN (tenant provisioning copies the
--   grants of the catalog roles to every new tenant).
-- Additive only (plan rule 10): new columns with defaults; the one relaxation is FILE_CONTENT
-- DROP NOT NULL, which the step file prescribes.
-- ============================================================

-- 1. FILE_DOCUMENT — storage location, visibility, public slug, content hash
ALTER TABLE FILE_DOCUMENT ADD COLUMN STORAGE_PROVIDER VARCHAR(8);
ALTER TABLE FILE_DOCUMENT ADD COLUMN STORAGE_REF      VARCHAR(512);
ALTER TABLE FILE_DOCUMENT ADD COLUMN VISIBILITY       VARCHAR(8)  DEFAULT 'PRIVATE' NOT NULL;
ALTER TABLE FILE_DOCUMENT ADD COLUMN PUBLIC_SLUG      VARCHAR(64);
ALTER TABLE FILE_DOCUMENT ADD COLUMN CONTENT_HASH     VARCHAR(64);

-- Backfill: every existing document keeps its bytes in the row (DB provider, ref = its own id).
UPDATE FILE_DOCUMENT
   SET STORAGE_PROVIDER = 'DB',
       STORAGE_REF      = ID::text,
       VISIBILITY       = 'PRIVATE',
       CONTENT_HASH     = encode(sha256(FILE_CONTENT), 'hex')
 WHERE STORAGE_PROVIDER IS NULL;

ALTER TABLE FILE_DOCUMENT ALTER COLUMN STORAGE_PROVIDER SET DEFAULT 'DB';
ALTER TABLE FILE_DOCUMENT ALTER COLUMN STORAGE_PROVIDER SET NOT NULL;
ALTER TABLE FILE_DOCUMENT ALTER COLUMN FILE_CONTENT DROP NOT NULL;

ALTER TABLE FILE_DOCUMENT ADD CONSTRAINT CHK_FILE_DOCUMENT_STORAGE_PROVIDER
    CHECK (STORAGE_PROVIDER IN ('DB', 'LOCAL', 'S3'));
ALTER TABLE FILE_DOCUMENT ADD CONSTRAINT CHK_FILE_DOCUMENT_VISIBILITY
    CHECK (VISIBILITY IN ('PRIVATE', 'PUBLIC'));
-- A PUBLIC document always carries a slug; a PRIVATE one never does.
ALTER TABLE FILE_DOCUMENT ADD CONSTRAINT CHK_FILE_DOCUMENT_PUBLIC_SLUG
    CHECK ((VISIBILITY = 'PUBLIC') = (PUBLIC_SLUG IS NOT NULL));

-- Unique per tenant where set (partial unique index: PRIVATE rows have no slug).
CREATE UNIQUE INDEX UQ_FILE_DOCUMENT_PUBLIC_SLUG ON FILE_DOCUMENT (TENANT_ID, PUBLIC_SLUG)
    WHERE PUBLIC_SLUG IS NOT NULL;

COMMENT ON COLUMN FILE_DOCUMENT.STORAGE_PROVIDER IS 'StorageProvider key holding the content: DB | LOCAL | S3 (erp-core step 07).';
COMMENT ON COLUMN FILE_DOCUMENT.STORAGE_REF IS 'Provider-specific reference: DB = row id, LOCAL = path relative to the root, S3 = object key.';
COMMENT ON COLUMN FILE_DOCUMENT.VISIBILITY IS 'PRIVATE (token download) or PUBLIC (served at /api/v1/public/files/{tenantCode}/{publicSlug}).';
COMMENT ON COLUMN FILE_DOCUMENT.PUBLIC_SLUG IS 'Random, non-guessable slug of a PUBLIC document; unique per tenant; NULL when PRIVATE.';
COMMENT ON COLUMN FILE_DOCUMENT.CONTENT_HASH IS 'SHA-256 (hex) of the content; the public ETag.';
COMMENT ON COLUMN FILE_DOCUMENT.FILE_CONTENT IS 'File bytes (BYTEA) — DB storage provider only; NULL for LOCAL / S3.';

-- 2. FILE_CATEGORY — public policy
ALTER TABLE FILE_CATEGORY ADD COLUMN ALLOW_PUBLIC BOOLEAN DEFAULT FALSE NOT NULL;
COMMENT ON COLUMN FILE_CATEGORY.ALLOW_PUBLIC IS 'Only categories allowing public may hold PUBLIC documents (e.g. PRODUCT_IMAGE).';

-- 3. FILE:DOCUMENT:PUBLISH — registry row (global) + grants in the PLATFORM tenant.
--    Screen FILE_BROWSER already carries its VIEW gateway (RULE-SEC-007), so the action is
--    effective for every role holding PERM_FILE_BROWSER_VIEW.
INSERT INTO SEC_ACTION_REG (ACTION_REG_PK, PERMISSION_CODE, SCREEN_ID, ACTION_CODE, NAME_AR, NAME_EN, IS_ACTIVE_FL, CREATED_BY, CREATED_AT)
SELECT nextval('SEQ_SEC_ACTION_REG'), 'FILE:DOCUMENT:PUBLISH', s.SCREEN_REG_PK, 'PUBLISH',
       s.NAME_AR || ' - نشر', s.NAME_EN || ' - PUBLISH', TRUE, 'SYSTEM', CURRENT_TIMESTAMP
FROM SEC_SCREEN_REG s WHERE s.PAGE_CODE = 'FILE_BROWSER';

INSERT INTO SEC_ROLE_ACTION_GRANT (ROLE_ACTION_GRANT_PK, TENANT_ID, ROLE_ID, ACTION_ID, GRANTED_BY, GRANTED_AT, CREATED_BY, CREATED_AT)
SELECT nextval('SEQ_SEC_ROLE_ACTION_GRANT'), 1, r.ROLE_PK, a.ACTION_REG_PK, 'SYSTEM', CURRENT_TIMESTAMP, 'SYSTEM', CURRENT_TIMESTAMP
FROM SEC_ROLE r JOIN SEC_ACTION_REG a ON a.PERMISSION_CODE = 'FILE:DOCUMENT:PUBLISH'
WHERE r.TENANT_ID = 1 AND r.CODE IN ('SYS_ADMIN', 'FILE_ADMIN')
ORDER BY r.ROLE_PK;
