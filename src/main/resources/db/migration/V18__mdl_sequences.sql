-- ============================================================
-- V18 — Master Data Lookup (MDL) — full module schema (first MDL migration)
-- Source: governance/modules/MDL/P2/db-script-mdl.md §3 FULL_DATABASE_SCRIPT (BLOCK 2/3/5/7)
-- Target: POSTGRESQL_16 | 2 tables, 2 sequences | 21 DBF-IDs | 1 XM (SOFT-READ -> SEC, no FK)
--
-- Named "*_sequences" per this repo's naming convention for the module's first migration, but
-- also creates the two tables themselves: MDL_LOOKUP_TYPE/MDL_LOOKUP_VALUE do not exist in any
-- prior migration (grepped — both tables are new to this migration; MDL is a never-before-migrated
-- module).
--
-- DEVIATION from db-script §3 BLOCK 1/2/3 (same deviation V1/V2/V6/V8/V16 all apply):
-- the db-script declares every PK as `GENERATED ALWAYS AS IDENTITY` with "BLOCK 1 — none".
-- This repo's entity contract mandates GenerationType.SEQUENCE + @SequenceGenerator
-- (build-create-entity A.1.3/A.1.4; GenerationType.IDENTITY is an automatic rejection
-- trigger), so PK columns are plain BIGINT NOT NULL, fed by the sequences created in BLOCK 1
-- below (no DB-level DEFAULT nextval() on the column — Hibernate calls nextval() itself via
-- the sequence generator, matching V1/V2/V6/V8/V16's idiom). Every table/column/constraint/
-- index name is otherwise verbatim from the db-script.
-- ============================================================

-- ============================================================
-- BLOCK 1: SEQUENCES
-- ============================================================
CREATE SEQUENCE SEQ_MDL_LOOKUP_TYPE  START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE SEQ_MDL_LOOKUP_VALUE START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;

-- ============================================================
-- BLOCK 2: PARENT TABLES (no FK dependencies)
-- ============================================================
CREATE TABLE MDL_LOOKUP_TYPE (
  lookup_type_pk      BIGINT        NOT NULL,
  key                  VARCHAR(80)   NOT NULL,
  owner_module_code    VARCHAR(10)   NOT NULL,
  name_ar              VARCHAR(150)  NOT NULL,
  name_en              VARCHAR(150)  NOT NULL,
  is_active_fl         BOOLEAN       NOT NULL DEFAULT TRUE,
  created_by           VARCHAR(100)  NOT NULL,
  created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_by           VARCHAR(100),
  updated_at           TIMESTAMPTZ
);

-- ============================================================
-- BLOCK 3: CHILD TABLES (parent already created above)
-- ============================================================
CREATE TABLE MDL_LOOKUP_VALUE (
  lookup_value_pk  BIGINT        NOT NULL,
  lookup_type_id   BIGINT        NOT NULL,
  code             VARCHAR(50)   NOT NULL,
  name_ar          VARCHAR(150)  NOT NULL,
  name_en          VARCHAR(150)  NOT NULL,
  sort_order       NUMERIC       NOT NULL DEFAULT 0,
  is_active_fl     BOOLEAN       NOT NULL DEFAULT TRUE,
  created_by       VARCHAR(100)  NOT NULL,
  created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_by       VARCHAR(100),
  updated_at       TIMESTAMPTZ
);

-- ============================================================
-- BLOCK 4: COMMENTS
-- ============================================================
COMMENT ON TABLE MDL_LOOKUP_TYPE IS 'ENT-MDL-001 LookupType — SHARED(owner); [DBF-MDL-001..010]';
COMMENT ON COLUMN MDL_LOOKUP_TYPE.lookup_type_pk IS 'DBF-MDL-001';
COMMENT ON COLUMN MDL_LOOKUP_TYPE.key IS 'DBF-MDL-002 — immutable after create, RULE-MDL-003 (application layer)';
COMMENT ON COLUMN MDL_LOOKUP_TYPE.owner_module_code IS 'DBF-MDL-003 — XM-MDL-001 SOFT-READ validates against SEC_MODULE_REG.code, RULE-MDL-001 (application layer)';
COMMENT ON COLUMN MDL_LOOKUP_TYPE.name_ar IS 'DBF-MDL-004';
COMMENT ON COLUMN MDL_LOOKUP_TYPE.name_en IS 'DBF-MDL-005';
COMMENT ON COLUMN MDL_LOOKUP_TYPE.is_active_fl IS 'DBF-MDL-006';
COMMENT ON COLUMN MDL_LOOKUP_TYPE.created_by IS 'DBF-MDL-007';
COMMENT ON COLUMN MDL_LOOKUP_TYPE.created_at IS 'DBF-MDL-008';
COMMENT ON COLUMN MDL_LOOKUP_TYPE.updated_by IS 'DBF-MDL-009';
COMMENT ON COLUMN MDL_LOOKUP_TYPE.updated_at IS 'DBF-MDL-010';

COMMENT ON TABLE MDL_LOOKUP_VALUE IS 'ENT-MDL-002 LookupValue — SHARED(owner); [DBF-MDL-011..021]';
COMMENT ON COLUMN MDL_LOOKUP_VALUE.lookup_value_pk IS 'DBF-MDL-011';
COMMENT ON COLUMN MDL_LOOKUP_VALUE.lookup_type_id IS 'DBF-MDL-012';
COMMENT ON COLUMN MDL_LOOKUP_VALUE.code IS 'DBF-MDL-013 — unique within lookup_type_id, UQ_MDL_LOOKUP_VALUE_TYPE_CODE, RULE-MDL-002';
COMMENT ON COLUMN MDL_LOOKUP_VALUE.name_ar IS 'DBF-MDL-014';
COMMENT ON COLUMN MDL_LOOKUP_VALUE.name_en IS 'DBF-MDL-015';
COMMENT ON COLUMN MDL_LOOKUP_VALUE.sort_order IS 'DBF-MDL-016';
COMMENT ON COLUMN MDL_LOOKUP_VALUE.is_active_fl IS 'DBF-MDL-017';
COMMENT ON COLUMN MDL_LOOKUP_VALUE.created_by IS 'DBF-MDL-018';
COMMENT ON COLUMN MDL_LOOKUP_VALUE.created_at IS 'DBF-MDL-019';
COMMENT ON COLUMN MDL_LOOKUP_VALUE.updated_by IS 'DBF-MDL-020';
COMMENT ON COLUMN MDL_LOOKUP_VALUE.updated_at IS 'DBF-MDL-021';

-- ============================================================
-- BLOCK 5: CONSTRAINTS
-- ============================================================
-- 5a. PRIMARY KEYS
ALTER TABLE MDL_LOOKUP_TYPE  ADD CONSTRAINT PK_MDL_LOOKUP_TYPE  PRIMARY KEY (lookup_type_pk);
ALTER TABLE MDL_LOOKUP_VALUE ADD CONSTRAINT PK_MDL_LOOKUP_VALUE PRIMARY KEY (lookup_value_pk);

-- 5b. UNIQUE
ALTER TABLE MDL_LOOKUP_TYPE  ADD CONSTRAINT UQ_MDL_LOOKUP_TYPE_KEY        UNIQUE (key);
ALTER TABLE MDL_LOOKUP_VALUE ADD CONSTRAINT UQ_MDL_LOOKUP_VALUE_TYPE_CODE UNIQUE (lookup_type_id, code);   -- RULE-MDL-002, enforced structurally

-- 5c. CHECK
-- none — no closed value set on any MDL column itself (MDL stores others' coded values, not its own).

-- 5d. INTRA-MODULE FK (parent PK first)
ALTER TABLE MDL_LOOKUP_VALUE ADD CONSTRAINT FK_LOOKUP_VALUE_TYPE FOREIGN KEY (lookup_type_id) REFERENCES MDL_LOOKUP_TYPE (lookup_type_pk);

-- No FK for owner_module_code -> SEC_MODULE_REG.code: XM-MDL-001 is SOFT-READ by design
-- (db-script §6 FK classification — SOFT-READ never gets a live constraint), not a HARD-FK.

-- ============================================================
-- BLOCK 6: TRIGGERS
-- none: RULE-MDL-001/003/004 are application-layer (cross-module read, field-omission, and
-- join-time filter respectively); RULE-MDL-002 is already enforced structurally by
-- UQ_MDL_LOOKUP_VALUE_TYPE_CODE above — no trigger needed.
-- ============================================================

-- ============================================================
-- BLOCK 7: INDEXES (non-PK; every FK column + every SRS search/list filter column)
-- ============================================================
CREATE INDEX IDX_MDL_LOOKUP_TYPE_OWNER   ON MDL_LOOKUP_TYPE (owner_module_code);
CREATE INDEX IDX_MDL_LOOKUP_TYPE_ACTIVE  ON MDL_LOOKUP_TYPE (is_active_fl);
CREATE INDEX IDX_MDL_LOOKUP_VALUE_TYPE   ON MDL_LOOKUP_VALUE (lookup_type_id);
CREATE INDEX IDX_MDL_LOOKUP_VALUE_SORT   ON MDL_LOOKUP_VALUE (lookup_type_id, sort_order);

-- BLOCK 8 LOOKUP SEED DATA / BLOCK 9 VIEWS / BLOCK 10 FUNCTIONS / BLOCK 11 DEFERRED FK PATCHES
-- — none (db-script §8, §9, §10, §11).
