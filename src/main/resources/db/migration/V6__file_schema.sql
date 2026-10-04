-- ============================================================
-- V8 — File Service (FILE) — full module schema
-- Source: governance/modules/FILE/P2/db-script.md SECTION 4 (FULL_DATABASE_SCRIPT), DBS-FILE-001
-- Target: POSTGRESQL_16 | 2 tables, 2 sequences | 18 DBF-IDs, 1 SOFT-READ XM (SEC)
-- Schema only — NO seed data (LOV-FILE-001/002 are runtime-loaded codes; no MD_MASTER_LOOKUP).
-- Flyway wraps this migration in its own transaction (no explicit COMMIT — matches V1..V7).
-- ============================================================

-- ============================================================
-- BLOCK 1: SEQUENCES
-- ============================================================
CREATE SEQUENCE SEQ_FILE_DOCUMENT START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE SEQ_FILE_CATEGORY START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;

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
-- (none — LOV-FILE-001/002 are runtime-loaded codes; no MD_MASTER_LOOKUP per srs-FILE.md A5)
-- ============================================================
-- XM-FILE-001 (->SEC_USER_ACCOUNT) is SOFT-READ (application-layer identity); no physical FK by design.
