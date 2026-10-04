-- ============================================================
-- V35 — Finance / General Ledger (FIN) v2 — account-mapping schema delta
-- Source: governance/shared/analysis/modules/FIN/v2/P2/db-script-fin.md §3 (BLOCK 1..7)
-- Target: POSTGRESQL_16 | 2 new tables (FIN_ACCOUNT_MAPPING, FIN_RULE_LINE_DIM), 2 sequences,
--         1 altered table (FIN_RULE_LINE: DBF-FIN-148 added, DBF-FIN-102 made nullable)
-- Forward-only on top of V22 (applied and checksummed — never edited).
-- Schema only — BLOCK 8's MDL lookup seed is V36, mirroring the V22 / V26 split.
-- Flyway wraps this migration in its own transaction (no explicit COMMIT — matches every
-- earlier migration; the db-script's own trailing COMMIT is intentionally omitted).
-- PKs are plain BIGINT NOT NULL fed by the BLOCK 1 sequences (SEQ_<TABLE>, same as V22).
-- Every table / column / constraint / index name is verbatim from the db-script.
-- ============================================================

-- BLOCK 1 — SEQUENCES (one per NEW table; the 14 v1 sequences exist from V22)
CREATE SEQUENCE SEQ_FIN_ACCOUNT_MAPPING START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE SEQ_FIN_RULE_LINE_DIM START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;

-- BLOCK 2 — PARENT TABLES
-- No new parent table. FIN_RULE_LINE (v1), parent of FIN_RULE_LINE_DIM, is altered here:
-- ENT-FIN-010 MODIFIED — the MAPPING business field is added, and the derivation value
-- becomes optional because a MAPPING line carries no account code (RULE-FIN-019, ADR-FIN-023).
ALTER TABLE FIN_RULE_LINE ADD COLUMN account_business_field_code VARCHAR(50);
ALTER TABLE FIN_RULE_LINE ALTER COLUMN account_derivation_value DROP NOT NULL;

-- BLOCK 3 — CHILD TABLES (parents FIN_ACCOUNT, FIN_RULE_LINE, FIN_DIMENSION,
-- FIN_DIMENSION_VALUE already exist from v1)

CREATE TABLE FIN_ACCOUNT_MAPPING (
  account_mapping_pk   BIGINT        NOT NULL,
  event_type_code      VARCHAR(50)   NOT NULL,
  business_field_code  VARCHAR(50)   NOT NULL,
  business_value       VARCHAR(100)  NOT NULL,
  account_id           BIGINT        NOT NULL,
  is_active_fl         BOOLEAN       NOT NULL DEFAULT TRUE,
  created_by           VARCHAR(100)  NOT NULL,
  created_at           TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_by           VARCHAR(100),
  updated_at           TIMESTAMPTZ
);

CREATE TABLE FIN_RULE_LINE_DIM (
  rule_line_dimension_pk  BIGINT        NOT NULL,
  value_source_code       VARCHAR(50)   NOT NULL,
  business_field_code     VARCHAR(50),
  rule_line_id            BIGINT        NOT NULL,
  dimension_id            BIGINT        NOT NULL,
  dimension_value_id      BIGINT,
  created_at              TIMESTAMPTZ   NOT NULL DEFAULT now()
);

-- BLOCK 4 — COMMENTS (every new table and column, plus the re-described v1 columns)

COMMENT ON TABLE FIN_RULE_LINE IS 'ENT-FIN-010 RuleLine — PRIVATE; [DBF-FIN-098..108, DBF-FIN-148]';
COMMENT ON COLUMN FIN_RULE_LINE.account_derivation_type_code IS 'DBF-FIN-101 — lookup ACCOUNT_DERIVATION_TYPE (XM-FIN-001): CONSTANT or MAPPING only; DIRECT retired (RULE-FIN-018, ADR-FIN-010)';
COMMENT ON COLUMN FIN_RULE_LINE.account_derivation_value IS 'DBF-FIN-102 — the constant account code when CONSTANT; NULL when MAPPING (RULE-FIN-019, CHK_FIN_RULE_LINE_DERIVATION_SPEC)';
COMMENT ON COLUMN FIN_RULE_LINE.account_business_field_code IS 'DBF-FIN-148 — lookup FIN_EVENT_BUSINESS_FIELD (XM-FIN-001): the business field whose event value selects the account through FIN_ACCOUNT_MAPPING when MAPPING; NULL when CONSTANT (RULE-FIN-019)';

COMMENT ON COLUMN FIN_JOURNAL_ENTRY.event_reference IS 'DBF-FIN-041 — RULE-FIN-004 idempotency (UQ_FIN_JOURNAL_ENTRY_EVENT_REF); a violation of that constraint is answered as the duplicate-event error, never a generic error (REQ-FIN-072)';

COMMENT ON TABLE FIN_ACCOUNT_MAPPING IS 'ENT-FIN-015 AccountMapping — PRIVATE config, soft-deleted via is_active_fl; one active row per (event_type_code, business_field_code, business_value) — RULE-FIN-022; [DBF-FIN-149..158]';
COMMENT ON COLUMN FIN_ACCOUNT_MAPPING.account_mapping_pk IS 'DBF-FIN-149 — primary key, drawn by the application from SEQ_FIN_ACCOUNT_MAPPING';
COMMENT ON COLUMN FIN_ACCOUNT_MAPPING.event_type_code IS 'DBF-FIN-150 — lookup ACCOUNTING_EVENT_TYPE (XM-FIN-001); fixed after create (ADR-FIN-024)';
COMMENT ON COLUMN FIN_ACCOUNT_MAPPING.business_field_code IS 'DBF-FIN-151 — lookup FIN_EVENT_BUSINESS_FIELD (XM-FIN-001); fixed after create';
COMMENT ON COLUMN FIN_ACCOUNT_MAPPING.business_value IS 'DBF-FIN-152 — business value stored as a code; an active PAYMENT_METHOD code when business_field_code = PAYMENT_METHOD (RULE-FIN-024, XM-FIN-001); fixed after create';
COMMENT ON COLUMN FIN_ACCOUNT_MAPPING.account_id IS 'DBF-FIN-153 — resolved account; active leaf on save (RULE-FIN-023) and again at post (RULE-FIN-007), application layer';
COMMENT ON COLUMN FIN_ACCOUNT_MAPPING.is_active_fl IS 'DBF-FIN-154 — an inactive mapping resolves nothing (RULE-FIN-020)';
COMMENT ON COLUMN FIN_ACCOUNT_MAPPING.created_by IS 'DBF-FIN-155';
COMMENT ON COLUMN FIN_ACCOUNT_MAPPING.created_at IS 'DBF-FIN-156';
COMMENT ON COLUMN FIN_ACCOUNT_MAPPING.updated_by IS 'DBF-FIN-157';
COMMENT ON COLUMN FIN_ACCOUNT_MAPPING.updated_at IS 'DBF-FIN-158';

COMMENT ON TABLE FIN_RULE_LINE_DIM IS 'ENT-FIN-016 RuleLineDimension — PRIVATE config; follows its rule line (ADR-FIN-025); one tag per dimension per line — RULE-FIN-025; [DBF-FIN-159..165]';
COMMENT ON COLUMN FIN_RULE_LINE_DIM.rule_line_dimension_pk IS 'DBF-FIN-159 — primary key, drawn by the application from SEQ_FIN_RULE_LINE_DIM';
COMMENT ON COLUMN FIN_RULE_LINE_DIM.value_source_code IS 'DBF-FIN-160 — lookup FIN_DIMENSION_VALUE_SOURCE (XM-FIN-001): CONSTANT or BUSINESS_FIELD';
COMMENT ON COLUMN FIN_RULE_LINE_DIM.business_field_code IS 'DBF-FIN-161 — lookup FIN_EVENT_BUSINESS_FIELD (XM-FIN-001) when BUSINESS_FIELD; NULL when CONSTANT (RULE-FIN-026); the event value resolves by FIN_DIMENSION_VALUE.code within dimension_id (ADR-FIN-026, RULE-FIN-027)';
COMMENT ON COLUMN FIN_RULE_LINE_DIM.rule_line_id IS 'DBF-FIN-162';
COMMENT ON COLUMN FIN_RULE_LINE_DIM.dimension_id IS 'DBF-FIN-163 — RULE-FIN-025 (UQ_FIN_RULE_LINE_DIM_LINE_DIM)';
COMMENT ON COLUMN FIN_RULE_LINE_DIM.dimension_value_id IS 'DBF-FIN-164 — constant value when CONSTANT, must belong to dimension_id (RULE-FIN-026, application layer); NULL when BUSINESS_FIELD';
COMMENT ON COLUMN FIN_RULE_LINE_DIM.created_at IS 'DBF-FIN-165';

-- BLOCK 5 — CONSTRAINTS

-- 5a PK
ALTER TABLE FIN_ACCOUNT_MAPPING  ADD CONSTRAINT PK_FIN_ACCOUNT_MAPPING  PRIMARY KEY (account_mapping_pk);
ALTER TABLE FIN_RULE_LINE_DIM    ADD CONSTRAINT PK_FIN_RULE_LINE_DIM    PRIMARY KEY (rule_line_dimension_pk);

-- 5b UNIQUE
ALTER TABLE FIN_RULE_LINE_DIM    ADD CONSTRAINT UQ_FIN_RULE_LINE_DIM_LINE_DIM  UNIQUE (rule_line_id, dimension_id);   -- RULE-FIN-025
-- RULE-FIN-022 (one ACTIVE mapping per key) is a partial unique index, BLOCK 7 (ADR-FIN-028).

-- 5c CHECK — exclusive-or, no lookup code named (ADR-FIN-028; v1 DEFAULT: value sets live in MDL)
ALTER TABLE FIN_RULE_LINE        ADD CONSTRAINT CHK_FIN_RULE_LINE_DERIVATION_SPEC
  CHECK ((account_derivation_value IS NULL) <> (account_business_field_code IS NULL));   -- RULE-FIN-019
ALTER TABLE FIN_RULE_LINE_DIM    ADD CONSTRAINT CHK_FIN_RULE_LINE_DIM_VALUE_SPEC
  CHECK ((dimension_value_id IS NULL) <> (business_field_code IS NULL));                  -- RULE-FIN-026

-- 5d intra-module FK (parent PKs exist from v1)
ALTER TABLE FIN_ACCOUNT_MAPPING  ADD CONSTRAINT FK_ACCOUNT_MAPPING_ACCOUNT    FOREIGN KEY (account_id) REFERENCES FIN_ACCOUNT (account_pk);
ALTER TABLE FIN_RULE_LINE_DIM    ADD CONSTRAINT FK_RULE_LINE_DIM_LINE         FOREIGN KEY (rule_line_id) REFERENCES FIN_RULE_LINE (rule_line_pk);
ALTER TABLE FIN_RULE_LINE_DIM    ADD CONSTRAINT FK_RULE_LINE_DIM_DIMENSION    FOREIGN KEY (dimension_id) REFERENCES FIN_DIMENSION (dimension_pk);
ALTER TABLE FIN_RULE_LINE_DIM    ADD CONSTRAINT FK_RULE_LINE_DIM_VALUE        FOREIGN KEY (dimension_value_id) REFERENCES FIN_DIMENSION_VALUE (dimension_value_pk);

-- BLOCK 6 — TRIGGERS
-- none: every v2 RULE is enforced at the application layer or structurally above / in BLOCK 7.

-- BLOCK 7 — INDEXES (non-PK; every new FK column + every SCR-REQ-FIN-013 filter column)
CREATE UNIQUE INDEX UQ_FIN_ACCOUNT_MAPPING_ACTIVE_KEY  ON FIN_ACCOUNT_MAPPING (event_type_code, business_field_code, business_value) WHERE is_active_fl;   -- RULE-FIN-022
CREATE INDEX IDX_FIN_ACCOUNT_MAPPING_EVT_FLD_VAL       ON FIN_ACCOUNT_MAPPING (event_type_code, business_field_code, business_value);   -- search incl. inactive rows (REQ-FIN-055)
CREATE INDEX IDX_FIN_ACCOUNT_MAPPING_FIELD             ON FIN_ACCOUNT_MAPPING (business_field_code);   -- businessFieldCode filter alone
CREATE INDEX IDX_FIN_ACCOUNT_MAPPING_ACCOUNT           ON FIN_ACCOUNT_MAPPING (account_id);            -- FK + accountId filter
CREATE INDEX IDX_FIN_RULE_LINE_DIM_DIMENSION           ON FIN_RULE_LINE_DIM (dimension_id);            -- FK
CREATE INDEX IDX_FIN_RULE_LINE_DIM_VALUE               ON FIN_RULE_LINE_DIM (dimension_value_id);      -- FK
-- FIN_RULE_LINE_DIM.rule_line_id (FK) is the leading column of UQ_FIN_RULE_LINE_DIM_LINE_DIM,
-- whose implicit index serves it — never duplicated.
