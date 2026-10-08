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
Steps          : 04, 05, 06, 07 (migrations V5, V7, V8, V10, V12)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)

Paths cited below are relative to `erp-core/src/main/resources/db/migration/core/` unless stated otherwise
(`com/erp/…` = `erp-core/src/main/java/com/erp/…`). No DBF ids are minted here.

### Migration chain — سلسلة الترحيل
| Kind | Migration | What it carries for FILE | Source |
|---|---|---|---|
| CHANGED | `V5__file_schema.sql` | `FILE_CATEGORY`, `FILE_DOCUMENT`, the two sequences, PK / UQ / CHK / FK and the three indexes — DDL verbatim from SECTION 4 (squashed in step 04 from the old `V8__file_schema`). Sequences are `CACHE 1` (the script said `NO CACHE`; equivalent). | V5 L12-13, L18-89; docs/steps/04-report.md → "Old → new mapping" |
| NEW | `V7__sec_seed.sql` | FILE's SEC registry rows: module `FILE`, screens `FILE_CATEGORIES` / `FILE_BROWSER`, the 8 `PERM_FILE_*` actions, role `FILE_ADMIN`, module / screen / action grants to `SYS_ADMIN` and `FILE_ADMIN`. | V7 L39, L70-71, L129-136, L153, L166-199 |
| NEW | `V8__mdl_seed.sql` | The `FILE_FILE_STATUS` / `FILE_FILE_TYPE` lookup types (owner FILE) and their 3 + 5 values in `MDL_LOOKUP_TYPE` / `MDL_LOOKUP_VALUE`. | V8 L25-31, L38-62 |
| NEW | `V10__tenant_schema.sql` | `TENANT_ID`, `VERSION`, tenant FK / index, per-tenant unique code (step 05). | V10 |
| NEW | `V12__file_storage.sql` | Storage SPI and public-file columns, CHECKs, slug index, `ALLOW_PUBLIC`, `FILE:DOCUMENT:PUBLISH` seed (step 07). Its one relaxation, `FILE_CONTENT DROP NOT NULL`, is prescribed by the step file. | V12 |

### Per-table deltas — التغييرات لكل جدول
| Kind | Table | Delta | Migration |
|---|---|---|---|
| NEW | FILE_CATEGORY / FILE_DOCUMENT | + `TENANT_ID BIGINT NOT NULL` (backfilled 1 = PLATFORM, default dropped, `FK_<TABLE>_TENANT` → `CORE_TENANT(ID)`, `IDX_<TABLE>_TENANT`); + `VERSION BIGINT NOT NULL DEFAULT 0` | V10 L76-77, L96-97, L115-116, L134-135 |
| CHANGED | FILE_CATEGORY | `UQ_FILE_CATEGORY_CATEGORY_CODE` → `(TENANT_ID, CATEGORY_CODE)` | V10 L197-198 |
| NEW | FILE_CATEGORY | + `ALLOW_PUBLIC BOOLEAN NOT NULL DEFAULT FALSE` — a native `BOOLEAN`, not the `SMALLINT 0/1 + CHK` pattern of `IS_ACTIVE_FL`; the entity maps it without a converter | V12 L54; com/erp/file/entity/FileCategory.java:74-77 |
| CHANGED | FILE_CATEGORY.CATEGORY_CODE | Stored trimmed and upper-cased: the entity normalises it on persist and update and the service probes the normalised form; no DB CHECK enforces the case | com/erp/file/entity/FileCategory.java:79-97; com/erp/file/service/FileCategoryService.java:65, 157-159 |
| NEW | FILE_DOCUMENT | + `STORAGE_PROVIDER VARCHAR(8) NOT NULL DEFAULT 'DB'` with `CHK_FILE_DOCUMENT_STORAGE_PROVIDER (DB, LOCAL, S3)` — the CHECK closes the SPI's key set; + `STORAGE_REF VARCHAR(512)` (DB = row id, LOCAL = relative path, S3 = object key); + `VISIBILITY VARCHAR(8) NOT NULL DEFAULT 'PRIVATE'` (`CHK_FILE_DOCUMENT_VISIBILITY`); + `PUBLIC_SLUG VARCHAR(64)` (`CHK_FILE_DOCUMENT_PUBLIC_SLUG`: PUBLIC if and only if slug present); + `CONTENT_HASH VARCHAR(64)` (SHA-256 hex, public ETag) | V12 L16-20, L30-40 |
| NEW | FILE_DOCUMENT | backfill of existing rows: `DB` / `ID::text` / `PRIVATE` / `encode(sha256(FILE_CONTENT),'hex')` | V12 L23-28 |
| CHANGED | FILE_DOCUMENT.FILE_CONTENT | becomes nullable (only the DB provider fills it) | V12 L32 |
| NEW | FILE_DOCUMENT | partial unique index `UQ_FILE_DOCUMENT_PUBLIC_SLUG (TENANT_ID, PUBLIC_SLUG) WHERE PUBLIC_SLUG IS NOT NULL` | V12 L43-44; DEVIATIONS [07] |
| NEW | SEC_ACTION_REG / SEC_ROLE_ACTION_GRANT (seed) | `FILE:DOCUMENT:PUBLISH` on screen `FILE_BROWSER`, granted to PLATFORM's `SYS_ADMIN` and `FILE_ADMIN` (copied to new tenants by provisioning) | V12 L60-69 |
| NEW | SEC_* registry (seed) | module `FILE`, screens, 8 `PERM_FILE_*` actions, role `FILE_ADMIN`, grants (SRS B4 said "no seed for PERM_*") | V7 |
| NEW | MDL_LOOKUP_TYPE / MDL_LOOKUP_VALUE (seed) | `FILE_FILE_STATUS` (ACTIVE, ARCHIVED, DELETED), `FILE_FILE_TYPE` (IMAGE, DOCUMENT, SPREADSHEET, ARCHIVE, OTHER), owner module FILE, bilingual labels, sort orders | V8 L30-31, L53-62 |

### Deviations from this analysis — الانحرافات
| Kind | Analysis said | Implemented | Source |
|---|---|---|---|
| CHANGED | `FILE_CONTENT BYTEA NOT NULL`, bytes always in-database (header, design note 4, DBF-0008) | nullable; LOCAL / S3 providers keep content outside the row; the provider key is recorded per row and closed by CHECK | V12 L32-35; step 07; DEVIATIONS [07]; ADR-FILE-001 |
| CHANGED | `UNIQUE (CATEGORY_CODE)` | `(TENANT_ID, CATEGORY_CODE)` | V10 L197-198; step 05 |
| CHANGED | "Lookup Tables: None — LOV-FILE-001/002 are runtime-loaded codes (no MD_MASTER_LOOKUP)"; design note 3 "no lookup table, no CHECK"; BLOCK 8 "none" | The two LOVs are rows of MDL's `MDL_LOOKUP_TYPE` / `MDL_LOOKUP_VALUE` seeded by V8 (owner FILE). `FILE_TYPE_ID` / `FILE_STATUS_ID` stay plain code columns with no FK and no CHECK, as designed; the value lists live in MDL and are served through `MdlLookupApi`. | V8 L25-62; com/erp/file/service/FileLookupService.java:48-57; ADR-FILE-005 |
| NEW | (no column) | `ALLOW_PUBLIC` as native `BOOLEAN` — departs from the module's `IS_*_FL SMALLINT` flag convention; no converter on the entity | V12 L54; FileCategory.java:74-77 |
| NEW | (no rule) | `CATEGORY_CODE` normalised to upper-case at the application layer (persist / update / pre-check), not by the database | FileCategory.java:79-97 |
| NEW | (beyond the step file) | `CONTENT_HASH` (ETag without hashing per request) and the three CHECKs; `CHK_FILE_DOCUMENT_STORAGE_PROVIDER` means an application-contributed provider can only replace DB / LOCAL / S3, never add a key | DEVIATIONS [07] "Design `FILE_DOCUMENT` columns"; com/erp/autoconfigure/FileStorageAutoConfiguration.java:43-45 |
| CHANGED | `NO CACHE` on both sequences | `CACHE 1` (equivalent) | V5 L12-13 |
| NEW | XM REGISTER: one SOFT-READ (SEC) | + HARD-FK `CORE_TENANT` on both tables; + application-layer read of MDL lookup values (no physical FK; no XM id minted here) | V10 L115-116; V8 |

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

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package C5, review round 1 — `FILE_DOCUMENT.REQUIRED_AUTHORITY` (restricted documents, RULE-FILE-012)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Migration (written from this entry): `erp-core/src/main/resources/db/migration/core/V22__file_document_required_authority.sql`
(the number reserved for this fix). Additive only: one nullable column, no default, no backfill (every existing
document stays unrestricted). No DBF id (the 1.2.0 addendum's precedent for columns added after the original script).

| Table | Column | Type | Null | Default | Entity field | Meaning |
|---|---|---|---|---|---|---|
| FILE_DOCUMENT | REQUIRED_AUTHORITY | VARCHAR(100) | NULL | — | `FileDocument.requiredAuthority` (`updatable = false`) | the authority a caller must hold, besides the FILE permission, to see or act on the document (RULE-FILE-012); NULL = an ordinary document. Width 100 = `SEC_ACTION_REG.PERMISSION_CODE`. Set only by the private store (TENANT export archives: `PLATFORM_TENANT_MANAGE`). |

No index (the column is only read together with an owner or id lookup), no constraint.
