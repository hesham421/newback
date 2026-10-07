<!-- DB Script — Governed by Database Governance Engine (Project 2 / MODE 1.5) -->

# DB SCRIPT — File Service (FILE)

## 1. DB SCRIPT HEADER
```
DBS-ID          : DBS-FILE-001
Module          : File Service (FILE)
SRS Feature Code: FILE-001  (srs-FILE.md v1.1)
Platform        : Foundation (Domain: ERP)
DB_TARGET       : POSTGRESQL_16   (confirmed by Architect 2026-09-02)
Date            : 2026-09-02
Status          : GATE PASSED
Open Questions  : None
Tables          : 2 (FILE_DOCUMENT, FILE_CATEGORY)
XM Dependencies : 1 SOFT-READ (XM-FILE-001 -> SEC)
Lookup Tables   : None — LOV-FILE-001/002 are runtime-loaded codes per SRS A5 (no MD_MASTER_LOOKUP)
```

> Governed design notes (SRS-governs-DB, Layer 1 > Layer 2):
> - OWNER_ID / OWNER_TYPE / MODULE_CODE form a polymorphic application reference — explicitly no governed FK (srs-FILE.md A3/A7).
> - The FILE->SEC link (created_by identity + auth filter) is SOFT-READ, not a physical FK -> registered as XM-FILE-001.
> - FILE_TYPE_ID (LOV-FILE-001) and FILE_STATUS_ID (LOV-FILE-002) are runtime-loaded code columns — no lookup table, no CHECK.
> - FILE_CONTENT uses BYTEA (bytes stored in-database per SRS A2).

## 2. DB FIELD TRACEABILITY MATRIX — File Service — DBS-ID: DBS-FILE-001
```
DBF-ID    | Table Name    | Column Name           | DB Type       | SRS Source
DBF-0001  | FILE_DOCUMENT | ID                    | BIGINT        | ENTITY-FILE-001.fileDocumentPk
DBF-0002  | FILE_DOCUMENT | OWNER_ID              | BIGINT        | ENTITY-FILE-001.ownerId (polymorphic)
DBF-0003  | FILE_DOCUMENT | OWNER_TYPE            | VARCHAR(100)  | ENTITY-FILE-001.ownerType
DBF-0004  | FILE_DOCUMENT | MODULE_CODE           | VARCHAR(50)   | ENTITY-FILE-001.moduleCode
DBF-0005  | FILE_DOCUMENT | FILE_NAME             | VARCHAR(255)  | ENTITY-FILE-001.fileName
DBF-0006  | FILE_DOCUMENT | CONTENT_TYPE          | VARCHAR(150)  | ENTITY-FILE-001.contentType
DBF-0007  | FILE_DOCUMENT | FILE_SIZE             | BIGINT        | ENTITY-FILE-001.fileSize
DBF-0008  | FILE_DOCUMENT | FILE_CONTENT          | BYTEA         | ENTITY-FILE-001.fileContent
DBF-0009  | FILE_DOCUMENT | FILE_TYPE_ID          | VARCHAR(50)   | ENTITY-FILE-001.fileTypeId (LOV-FILE-001)
DBF-0010  | FILE_DOCUMENT | FILE_STATUS_ID        | VARCHAR(50)   | ENTITY-FILE-001.fileStatusId (LOV-FILE-002)
DBF-0011  | FILE_DOCUMENT | FILE_CATEGORY_FK      | BIGINT        | ENTITY-FILE-001.fileCategoryFk -> ENTITY-FILE-002
DBF-0012  | FILE_CATEGORY | ID                    | BIGINT        | ENTITY-FILE-002.fileCategoryPk
DBF-0013  | FILE_CATEGORY | CATEGORY_CODE         | VARCHAR(50)   | ENTITY-FILE-002.categoryCode
DBF-0014  | FILE_CATEGORY | NAME_AR               | VARCHAR(200)  | ENTITY-FILE-002.nameAr
DBF-0015  | FILE_CATEGORY | NAME_EN               | VARCHAR(100)  | ENTITY-FILE-002.nameEn
DBF-0016  | FILE_CATEGORY | MAX_SIZE_BYTES        | BIGINT        | ENTITY-FILE-002.maxSizeBytes
DBF-0017  | FILE_CATEGORY | ALLOWED_CONTENT_TYPES | TEXT          | ENTITY-FILE-002.allowedContentTypes
DBF-0018  | FILE_CATEGORY | IS_ACTIVE_FL          | SMALLINT      | ENTITY-FILE-002.isActiveFl
Total: 18 DBF-IDs across 2 tables
```

## 3. CROSS-MODULE DEPENDENCY REGISTER (XM REGISTER) — File Service — DBS-ID: DBS-FILE-001
```
XM-ID        | Type      | This Table    | FK/Ref Column     | Target Table     | Target Module | Status
XM-FILE-001  | SOFT-READ | (application) | created_by / auth | SEC_USER_ACCOUNT | Security      | ACTIVE
```
> XM-FILE-001 — FILE reads SEC's UserAccount identity at the application layer (auth filter + created_by), with no physical FK (RULE-FILE-004). Target SEC_USER_ACCOUNT is gated under DBS-SEC-001 -> Status ACTIVE. Owner-based access (OWNER_ID/OWNER_TYPE/MODULE_CODE) is a polymorphic app reference, not an XM dependency.

## 4. FULL_DATABASE_SCRIPT
```sql
-- ============================================================
-- FULL DATABASE SCRIPT — File Service (FILE) — DBS-FILE-001
-- Target: POSTGRESQL_16   |   Execute in psql / pgAdmin
-- ============================================================

-- ============================================================
-- BLOCK 1: SEQUENCES
-- ============================================================
CREATE SEQUENCE SEQ_FILE_DOCUMENT START WITH 1 INCREMENT BY 1 NO CACHE NO CYCLE;
CREATE SEQUENCE SEQ_FILE_CATEGORY START WITH 1 INCREMENT BY 1 NO CACHE NO CYCLE;

-- ============================================================
-- BLOCK 2: PARENT TABLES (no intra-module FK dependencies)
-- ============================================================
CREATE TABLE FILE_CATEGORY (
  ID                    BIGINT        NOT NULL,
  CATEGORY_CODE         VARCHAR(50)   NOT NULL,
  NAME_AR               VARCHAR(200)  NOT NULL,
  NAME_EN               VARCHAR(100)  NOT NULL,
  MAX_SIZE_BYTES        BIGINT,
  ALLOWED_CONTENT_TYPES TEXT,
  IS_ACTIVE_FL          SMALLINT      DEFAULT 1 NOT NULL,
  CREATED_BY            VARCHAR(255),
  CREATED_AT            TIMESTAMP,
  UPDATED_BY            VARCHAR(255),
  UPDATED_AT            TIMESTAMP
);

-- ============================================================
-- BLOCK 3: CHILD TABLES (intra-module FK dependencies)
-- ============================================================
CREATE TABLE FILE_DOCUMENT (
  ID               BIGINT        NOT NULL,
  OWNER_ID         BIGINT        NOT NULL,
  OWNER_TYPE       VARCHAR(100)  NOT NULL,
  MODULE_CODE      VARCHAR(50)   NOT NULL,
  FILE_NAME        VARCHAR(255)  NOT NULL,
  CONTENT_TYPE     VARCHAR(150)  NOT NULL,
  FILE_SIZE        BIGINT,
  FILE_CONTENT     BYTEA         NOT NULL,
  FILE_TYPE_ID     VARCHAR(50)   NOT NULL,
  FILE_STATUS_ID   VARCHAR(50)   NOT NULL,
  FILE_CATEGORY_FK BIGINT,
  CREATED_BY       VARCHAR(255),
  CREATED_AT       TIMESTAMP,
  UPDATED_BY       VARCHAR(255),
  UPDATED_AT       TIMESTAMP
);

-- ============================================================
-- BLOCK 4: COMMENTS
-- ============================================================
COMMENT ON TABLE FILE_DOCUMENT IS 'Stored file bytes + metadata (ENTITY-FILE-001). Ownership is polymorphic (OWNER_ID/OWNER_TYPE/MODULE_CODE) — no governed FK.';
COMMENT ON COLUMN FILE_DOCUMENT.OWNER_ID IS 'Polymorphic application owner id (no FK).';
COMMENT ON COLUMN FILE_DOCUMENT.OWNER_TYPE IS 'Polymorphic owner entity type.';
COMMENT ON COLUMN FILE_DOCUMENT.CONTENT_TYPE IS 'Server-side auto-detected MIME (RULE-FILE-002).';
COMMENT ON COLUMN FILE_DOCUMENT.FILE_CONTENT IS 'File bytes (BYTEA).';
COMMENT ON COLUMN FILE_DOCUMENT.FILE_TYPE_ID IS 'File type code (LOV-FILE-001); runtime-loaded.';
COMMENT ON COLUMN FILE_DOCUMENT.FILE_STATUS_ID IS 'Lifecycle code (LOV-FILE-002): ACTIVE/ARCHIVED/DELETED; soft-delete (RULE-FILE-006).';
COMMENT ON COLUMN FILE_DOCUMENT.FILE_CATEGORY_FK IS 'FK to FILE_CATEGORY (optional).';
COMMENT ON TABLE FILE_CATEGORY IS 'Reference: file categories with per-category size/type limits (ENTITY-FILE-002).';
COMMENT ON COLUMN FILE_CATEGORY.IS_ACTIVE_FL IS 'Active flag: 1=active, 0=inactive.';

-- ============================================================
-- BLOCK 5: CONSTRAINTS
-- ============================================================
-- 5a. PRIMARY KEYS
ALTER TABLE FILE_CATEGORY ADD CONSTRAINT PK_FILE_CATEGORY PRIMARY KEY (ID);
ALTER TABLE FILE_DOCUMENT ADD CONSTRAINT PK_FILE_DOCUMENT PRIMARY KEY (ID);
-- 5b. UNIQUE  (RULE-FILE-007)
ALTER TABLE FILE_CATEGORY ADD CONSTRAINT UQ_FILE_CATEGORY_CATEGORY_CODE UNIQUE (CATEGORY_CODE);
-- 5c. CHECK
ALTER TABLE FILE_CATEGORY ADD CONSTRAINT CHK_FILE_CATEGORY_ACTIVE_FL CHECK (IS_ACTIVE_FL IN (0,1));
-- 5d. INTRA-MODULE FK
ALTER TABLE FILE_DOCUMENT ADD CONSTRAINT FK_FILE_DOCUMENT_CATEGORY FOREIGN KEY (FILE_CATEGORY_FK) REFERENCES FILE_CATEGORY (ID);

-- ============================================================
-- BLOCK 6: TRIGGERS       -- (none)
-- ============================================================

-- ============================================================
-- BLOCK 7: INDEXES
-- ============================================================
CREATE INDEX IDX_FILE_DOCUMENT_OWNER       ON FILE_DOCUMENT (OWNER_ID, OWNER_TYPE, MODULE_CODE);
CREATE INDEX IDX_FILE_DOCUMENT_STATUS      ON FILE_DOCUMENT (FILE_STATUS_ID);
CREATE INDEX IDX_FILE_DOCUMENT_CATEGORY_FK ON FILE_DOCUMENT (FILE_CATEGORY_FK);

-- ============================================================
-- BLOCK 8: LOOKUP SEED DATA
-- ============================================================
-- (none — LOV-FILE-001/002 are runtime-loaded codes; no MD_MASTER_LOOKUP per srs-FILE.md A5)

-- BLOCK 9: VIEWS            -- (none)
-- BLOCK 10: FUNCTIONS/PROCS -- (none)
-- BLOCK 11: DEFERRED FK BLOCKS
-- ============================================================
-- XM-FILE-001 is SOFT-READ (application-layer identity read from SEC_USER_ACCOUNT).
-- No physical FK is created by design (RULE-FILE-004). No deferred patch required.
```

## 5. DB REGISTRY UPDATE — MODE 1.5
```
REGISTRY UPDATE — 2026-09-02
Source Mode : MODE 1.5 | Feature Code: FILE-001 | DBS-ID: DBS-FILE-001
New Tables  : FILE_DOCUMENT, FILE_CATEGORY
New Lookups : None
XM-IDs Open : XM-FILE-001 (SOFT-READ -> SEC_USER_ACCOUNT) — Status ACTIVE
OQ-IDs Open : None
Gate Status : PASSED
Next Action : Trigger Project 3.1 — Execution Plan Governance Engine (Backend pass)
Table Registry rows to add (master-registry §7):
  DBS-FILE-001 | FILE_DOCUMENT | FILE
  DBS-FILE-001 | FILE_CATEGORY | FILE
Global XM Index rows to add (master-registry §8):
  XM-FILE-001 | FILE | SEC | SOFT-READ | ACTIVE
Pipeline Status Grid: FILE · P2 = done
```

---
*End of db-script-FILE.md | DBS-FILE-001 | POSTGRESQL_16 | 2 tables, 18 DBF-IDs, 1 SOFT-READ XM | Next: Project 3.1*

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 04, 05, 07 (migrations V5, V8, V10, V12)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Paths cited below are relative to `erp-core/src/main/resources/db/migration/core/` at that tag unless
stated otherwise. No DBF ids are minted here.

### Migration chain
- Squashed in step 04: `FILE_CATEGORY` and `FILE_DOCUMENT` are created by `V5__file_schema.sql` (DDL
  verbatim from the old `V8__file_schema`); the `FILE_FILE_STATUS` / `FILE_FILE_TYPE` lookup rows live in
  MDL (`V8__mdl_seed.sql`); the FILE registry rows and `FILE_ADMIN` role are in `V7__sec_seed.sql`. Full
  old → new mapping: `docs/steps/04-report.md` → "Old → new mapping".
- Later FILE changes are additive: `V10__tenant_schema.sql` (step 05) and `V12__file_storage.sql`
  (step 07). V12's one relaxation, `FILE_CONTENT DROP NOT NULL`, is prescribed by the step file.

### Per-table deltas
| Table | Delta | Migration |
|---|---|---|
| FILE_CATEGORY / FILE_DOCUMENT | + `TENANT_ID BIGINT NOT NULL` (backfilled 1 = PLATFORM, default dropped, `FK_<TABLE>_TENANT` → `CORE_TENANT(ID)`, `IDX_<TABLE>_TENANT`); + `VERSION BIGINT NOT NULL DEFAULT 0` | V10 |
| FILE_CATEGORY | `UQ_FILE_CATEGORY_CATEGORY_CODE` → `(TENANT_ID, CATEGORY_CODE)` | V10 |
| FILE_CATEGORY | + `ALLOW_PUBLIC BOOLEAN NOT NULL DEFAULT FALSE` | V12 §2 |
| FILE_DOCUMENT | + `STORAGE_PROVIDER VARCHAR(8) NOT NULL DEFAULT 'DB'` (`CHK_FILE_DOCUMENT_STORAGE_PROVIDER`: DB, LOCAL, S3); + `STORAGE_REF VARCHAR(512)` (DB = row id, LOCAL = relative path, S3 = object key); + `VISIBILITY VARCHAR(8) NOT NULL DEFAULT 'PRIVATE'` (`CHK_FILE_DOCUMENT_VISIBILITY`); + `PUBLIC_SLUG VARCHAR(64)` (`CHK_FILE_DOCUMENT_PUBLIC_SLUG`: PUBLIC if and only if slug present); + `CONTENT_HASH VARCHAR(64)` (SHA-256 hex, public ETag) | V12 §1 |
| FILE_DOCUMENT | backfill of existing rows: `DB` / `ID::text` / `PRIVATE` / `encode(sha256(FILE_CONTENT),'hex')` | V12 §1 |
| FILE_DOCUMENT | `FILE_CONTENT` becomes nullable (only the DB provider fills it) | V12 §1 |
| FILE_DOCUMENT | partial unique index `UQ_FILE_DOCUMENT_PUBLIC_SLUG (TENANT_ID, PUBLIC_SLUG) WHERE PUBLIC_SLUG IS NOT NULL` | V12 §1; DEVIATIONS [07] |
| SEC_ACTION_REG / SEC_ROLE_ACTION_GRANT (seed) | `FILE:DOCUMENT:PUBLISH` on screen `FILE_BROWSER`, granted to PLATFORM's `SYS_ADMIN` and `FILE_ADMIN` (copied to new tenants by provisioning) | V12 §3 |

### Deviations from this analysis
- Analysis: `FILE_CONTENT BYTEA NOT NULL`, bytes always in-database. Implemented: nullable; LOCAL / S3
  providers keep content outside the row (step 07; DEVIATIONS [07]).
- Analysis: `UNIQUE (CATEGORY_CODE)`. Implemented: `(TENANT_ID, CATEGORY_CODE)` (step 05).
- Added beyond the step file: `CONTENT_HASH` (ETag without hashing per request) and the three CHECKs
  (DEVIATIONS [07] "Design `FILE_DOCUMENT` columns").

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package D.4 — shared image store
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

**No schema change and no migration.** Image-store documents use the existing `FILE_DOCUMENT` columns:
`FILE_CATEGORY_FK` NULL, `VISIBILITY = 'PUBLIC'` with a `PUBLIC_SLUG` (so `CHK_FILE_DOCUMENT_PUBLIC_SLUG` and
`UQ_FILE_DOCUMENT_PUBLIC_SLUG` hold), `FILE_STATUS_ID = 'ACTIVE'`, `FILE_TYPE_ID = 'IMAGE'`, `OWNER_TYPE` /
`OWNER_ID` / `MODULE_CODE` of the consumer (`SEC_USER` / user id / `SEC`). Discard sets `FILE_STATUS_ID =
'DELETED'`, `VISIBILITY = 'PRIVATE'`, `PUBLIC_SLUG = NULL`.

Query change (no DDL): the public lookup (`FileDocumentRepository.findPublicMetadataTupleBySlug`) accepts
`FILE_CATEGORY_FK IS NULL` besides a category with `ALLOW_PUBLIC = TRUE` (RULE-FILE-010).
