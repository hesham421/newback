-- ============================================================
-- V1 — Common Utils (CU) — CU_APP_CONFIGURATION schema
-- Source: governance/modules/CU/P2/db-script.md SECTION 4 (FULL_DATABASE_SCRIPT)
-- DBS-ID: DBS-CU-001 | Target: POSTGRESQL_16
-- ============================================================

-- ============================================================
-- BLOCK 1: SEQUENCES
-- ============================================================
CREATE SEQUENCE SEQ_CU_APP_CONFIGURATION
  START WITH 1
  INCREMENT BY 1
  CACHE 1
  NO CYCLE;

-- ============================================================
-- BLOCK 2: PARENT TABLES (no FK dependencies)
-- ============================================================
CREATE TABLE CU_APP_CONFIGURATION (
  ID            BIGINT           NOT NULL,
  CONFIG_KEY    VARCHAR(150)     NOT NULL,
  CONFIG_VALUE  TEXT             NOT NULL,
  NOTES         VARCHAR(2000),
  IS_ACTIVE_FL  SMALLINT         DEFAULT 1 NOT NULL,
  CREATED_BY    VARCHAR(255),
  CREATED_AT    TIMESTAMP,
  UPDATED_BY    VARCHAR(255),
  UPDATED_AT    TIMESTAMP
);

-- ============================================================
-- BLOCK 4: COMMENTS
-- ============================================================
COMMENT ON TABLE CU_APP_CONFIGURATION IS 'Platform runtime key/value configuration store (ENTITY-CU-001 / CU-001).';
COMMENT ON COLUMN CU_APP_CONFIGURATION.ID IS 'PK — populated by framework via SEQ_CU_APP_CONFIGURATION.';
COMMENT ON COLUMN CU_APP_CONFIGURATION.CONFIG_KEY IS 'Unique configuration key; read-only after creation (RULE-CU-003).';
COMMENT ON COLUMN CU_APP_CONFIGURATION.CONFIG_VALUE IS 'Configuration value (text).';
COMMENT ON COLUMN CU_APP_CONFIGURATION.NOTES IS 'Optional description.';
COMMENT ON COLUMN CU_APP_CONFIGURATION.IS_ACTIVE_FL IS 'Active flag: 1=active, 0=inactive (soft deactivate).';

-- ============================================================
-- BLOCK 5: CONSTRAINTS
-- ============================================================
-- 5a. PRIMARY KEY
ALTER TABLE CU_APP_CONFIGURATION ADD CONSTRAINT PK_CU_APP_CONFIGURATION PRIMARY KEY (ID);
-- 5b. UNIQUE  (RULE-CU-001)
ALTER TABLE CU_APP_CONFIGURATION ADD CONSTRAINT UQ_CU_APP_CONFIG_CONFIG_KEY UNIQUE (CONFIG_KEY);
-- 5c. CHECK
ALTER TABLE CU_APP_CONFIGURATION ADD CONSTRAINT CHK_CU_APP_CONFIG_ACTIVE_FL CHECK (IS_ACTIVE_FL IN (0,1));
