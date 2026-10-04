-- ============================================================
-- V6 — Notification Service (NOTIF) — full module schema
-- Source: governance/modules/NOTIF/P2/db-script.md SECTION 4 (FULL_DATABASE_SCRIPT), DBS-NOTIF-001
-- Target: POSTGRESQL_16 | 3 tables, 3 sequences | 25 DBF-IDs, 2 SOFT-READ XM (SEC, FILE)
-- Schema only — NO seed data (LOV-NOTIF-001/002 are runtime-loaded codes; no MD_MASTER_LOOKUP).
-- Flyway wraps this migration in its own transaction (no explicit COMMIT — matches every earlier migration).
-- ============================================================

-- ============================================================
-- BLOCK 1: SEQUENCES
-- ============================================================
CREATE SEQUENCE SEQ_NOTIF_LOG            START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE SEQ_NOTIF_TEMPLATE       START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE SEQ_NOTIF_CHANNEL_CONFIG START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;

-- ============================================================
-- BLOCK 2: PARENT TABLES (no intra-module FK dependencies)
-- ============================================================
CREATE TABLE NOTIF_TEMPLATE (
  ID                 BIGINT        NOT NULL,
  TEMPLATE_CODE      VARCHAR(80)   NOT NULL,
  NAME_AR            VARCHAR(200)  NOT NULL,
  NAME_EN            VARCHAR(100)  NOT NULL,
  SUBJECT_AR         VARCHAR(300),
  SUBJECT_EN         VARCHAR(300),
  BODY_AR            TEXT          NOT NULL,
  BODY_EN            TEXT          NOT NULL,
  ATTACHMENT_FILE_ID BIGINT,
  IS_ACTIVE_FL       SMALLINT      DEFAULT 1 NOT NULL,
  CREATED_BY         VARCHAR(255),
  CREATED_AT         TIMESTAMP,
  UPDATED_BY         VARCHAR(255),
  UPDATED_AT         TIMESTAMP
);

CREATE TABLE NOTIF_CHANNEL_CONFIG (
  ID              BIGINT       NOT NULL,
  CHANNEL_TYPE_ID VARCHAR(20)  NOT NULL,
  IS_ENABLED_FL   SMALLINT     DEFAULT 1 NOT NULL,
  CONFIG_JSON     TEXT,
  CREATED_BY      VARCHAR(255),
  CREATED_AT      TIMESTAMP,
  UPDATED_BY      VARCHAR(255),
  UPDATED_AT      TIMESTAMP
);

-- ============================================================
-- BLOCK 3: CHILD TABLES (intra-module FK dependencies)
-- ============================================================
CREATE TABLE NOTIF_LOG (
  ID                     BIGINT        NOT NULL,
  RECIPIENT_ID           BIGINT        NOT NULL,
  CHANNEL_TYPE_ID        VARCHAR(20)   NOT NULL,
  NOTIFICATION_STATUS_ID VARCHAR(30)   NOT NULL,
  MODULE_CODE            VARCHAR(50)   NOT NULL,
  REFERENCE_ID           BIGINT,
  REFERENCE_TYPE         VARCHAR(100),
  RETRY_COUNT            SMALLINT      DEFAULT 0 NOT NULL,
  ERROR_MESSAGE          TEXT,
  SENT_AT                TIMESTAMP,
  TEMPLATE_FK            BIGINT        NOT NULL,
  CREATED_BY             VARCHAR(255),
  CREATED_AT             TIMESTAMP,
  UPDATED_BY             VARCHAR(255),
  UPDATED_AT             TIMESTAMP
);

-- ============================================================
-- BLOCK 4: COMMENTS
-- ============================================================
COMMENT ON TABLE NOTIF_LOG IS 'Per-channel notification log row (ENTITY-NOTIF-001); fan-out one row per requested channel (RULE-NOTIF-001).';
COMMENT ON COLUMN NOTIF_LOG.RECIPIENT_ID IS 'Recipient UserAccount id — SOFT-READ to SEC (no FK); XM-NOTIF-001.';
COMMENT ON COLUMN NOTIF_LOG.CHANNEL_TYPE_ID IS 'Channel code (LOV-NOTIF-001); runtime-loaded.';
COMMENT ON COLUMN NOTIF_LOG.NOTIFICATION_STATUS_ID IS 'Lifecycle code (LOV-NOTIF-002): PENDING/SENT/FAILED/CHANNEL_DISABLED.';
COMMENT ON COLUMN NOTIF_LOG.RETRY_COUNT IS 'Retry counter, <=5 then FAILED (RULE-NOTIF-002).';
COMMENT ON COLUMN NOTIF_LOG.TEMPLATE_FK IS 'FK to NOTIF_TEMPLATE.';
COMMENT ON TABLE NOTIF_TEMPLATE IS 'Bilingual notification template (ENTITY-NOTIF-002).';
COMMENT ON COLUMN NOTIF_TEMPLATE.ATTACHMENT_FILE_ID IS 'Optional attachment file id — SOFT/service to FILE via FileService (no FK); XM-NOTIF-002.';
COMMENT ON COLUMN NOTIF_TEMPLATE.IS_ACTIVE_FL IS 'Active flag: 1=active, 0=inactive.';
COMMENT ON TABLE NOTIF_CHANNEL_CONFIG IS 'Per-channel enable flag + provider config JSON (ENTITY-NOTIF-003).';
COMMENT ON COLUMN NOTIF_CHANNEL_CONFIG.CHANNEL_TYPE_ID IS 'Unique channel code (LOV-NOTIF-001).';
COMMENT ON COLUMN NOTIF_CHANNEL_CONFIG.IS_ENABLED_FL IS 'Runtime enable flag: 1=enabled, 0=disabled (RULE-NOTIF-003).';
COMMENT ON COLUMN NOTIF_CHANNEL_CONFIG.CONFIG_JSON IS 'Provider config (JSON as text); actual provider is a P3 decision.';

-- ============================================================
-- BLOCK 5: CONSTRAINTS
-- ============================================================
-- 5a. PRIMARY KEYS
ALTER TABLE NOTIF_TEMPLATE       ADD CONSTRAINT PK_NOTIF_TEMPLATE       PRIMARY KEY (ID);
ALTER TABLE NOTIF_CHANNEL_CONFIG ADD CONSTRAINT PK_NOTIF_CHANNEL_CONFIG PRIMARY KEY (ID);
ALTER TABLE NOTIF_LOG            ADD CONSTRAINT PK_NOTIF_LOG            PRIMARY KEY (ID);
-- 5b. UNIQUE  (RULE-NOTIF-006)
ALTER TABLE NOTIF_TEMPLATE       ADD CONSTRAINT UQ_NOTIF_TEMPLATE_CODE       UNIQUE (TEMPLATE_CODE);
ALTER TABLE NOTIF_CHANNEL_CONFIG ADD CONSTRAINT UQ_NOTIF_CHANNEL_CONFIG_TYPE UNIQUE (CHANNEL_TYPE_ID);
-- 5c. CHECK
ALTER TABLE NOTIF_TEMPLATE       ADD CONSTRAINT CHK_NOTIF_TEMPLATE_ACTIVE_FL CHECK (IS_ACTIVE_FL IN (0,1));
ALTER TABLE NOTIF_CHANNEL_CONFIG ADD CONSTRAINT CHK_NOTIF_CHANNEL_ENABLED_FL CHECK (IS_ENABLED_FL IN (0,1));
-- 5d. INTRA-MODULE FK
ALTER TABLE NOTIF_LOG            ADD CONSTRAINT FK_NOTIF_LOG_TEMPLATE FOREIGN KEY (TEMPLATE_FK) REFERENCES NOTIF_TEMPLATE (ID);

-- ============================================================
-- BLOCK 6: TRIGGERS       -- (none)
-- ============================================================

-- ============================================================
-- BLOCK 7: INDEXES
-- ============================================================
CREATE INDEX IDX_NOTIF_LOG_TEMPLATE_FK  ON NOTIF_LOG (TEMPLATE_FK);
CREATE INDEX IDX_NOTIF_LOG_RECIPIENT_ID ON NOTIF_LOG (RECIPIENT_ID);
CREATE INDEX IDX_NOTIF_LOG_STATUS       ON NOTIF_LOG (NOTIFICATION_STATUS_ID);
CREATE INDEX IDX_NOTIF_LOG_MODULE_CODE  ON NOTIF_LOG (MODULE_CODE);

-- ============================================================
-- BLOCK 8: LOOKUP SEED DATA
-- (none — LOV-NOTIF-001/002 are runtime-loaded codes; no MD_MASTER_LOOKUP per srs-NOTIF.md A5)
-- ============================================================
-- XM-NOTIF-001 (->SEC_USER_ACCOUNT) and XM-NOTIF-002 (->FILE_DOCUMENT) are SOFT-READ; no physical FK by design.
