-- ============================================================
-- V14 — Number series (sequences) and tenant-aware settings — erp-core step 09
-- Target: POSTGRESQL_16 | after V2..V13
--
-- What this script does:
--   1. CORE_NUMBER_SERIES (tenant-scoped): one row per (tenant, series code, period). NEXT_VALUE is
--      the next number to hand out; NumberSeries allocation locks the row (SELECT ... FOR UPDATE).
--      PERIOD_KEY is '' (RESET_POLICY NEVER), 'YYYY' (YEARLY) or 'YYYY-MM' (MONTHLY); a new period
--      gets its own row, starting at 1.
--   2. CU_APP_CONFIGURATION.TENANT_ID becomes NULLABLE: NULL = platform default, non-null = tenant
--      override. Uniqueness moves from the constraint (TENANT_ID, CONFIG_KEY) to a unique index on
--      (COALESCE(TENANT_ID, 0), CONFIG_KEY), under the same name, so that two platform defaults with
--      the same key are impossible too (NULLs never collide in a plain unique constraint).
--      Existing rows keep their tenant (they stay overrides of the tenant that wrote them).
--
-- Additive-only rule (README.md): CORE_NUMBER_SERIES is a new table. The two CU changes (DROP NOT NULL
-- on TENANT_ID, the unique constraint replaced by an expression unique index) are the exception the
-- step-09 plan itself prescribes ("the one deliberate exception to every scoped table has NOT NULL
-- tenant"); existing data satisfies the new index. Recorded in docs/DEVIATIONS.md ([09]).
-- ============================================================

-- ============================================================
-- 1. CORE_NUMBER_SERIES
-- ============================================================
CREATE SEQUENCE SEQ_CORE_NUMBER_SERIES
  START WITH 1
  INCREMENT BY 1
  CACHE 1
  NO CYCLE;

CREATE TABLE CORE_NUMBER_SERIES (
  ID            BIGINT        NOT NULL,
  TENANT_ID     BIGINT        NOT NULL,
  CODE          VARCHAR(50)   NOT NULL,
  PREFIX        VARCHAR(20),
  PATTERN       VARCHAR(100)  NOT NULL DEFAULT '{PREFIX}-{YYYY}-{SEQ:6}',
  RESET_POLICY  VARCHAR(10)   NOT NULL DEFAULT 'YEARLY',
  PERIOD_KEY    VARCHAR(7)    NOT NULL DEFAULT '',
  NEXT_VALUE    BIGINT        NOT NULL DEFAULT 1,
  IS_ACTIVE     BOOLEAN       NOT NULL DEFAULT TRUE,
  CREATED_BY    VARCHAR(100),
  CREATED_AT    TIMESTAMPTZ   NOT NULL DEFAULT now(),
  UPDATED_BY    VARCHAR(100),
  UPDATED_AT    TIMESTAMPTZ,
  VERSION       BIGINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE CORE_NUMBER_SERIES IS 'Tenant-scoped document number series (erp-core step 09): one row per series code and period.';
COMMENT ON COLUMN CORE_NUMBER_SERIES.CODE IS 'Series code, e.g. SALES_INVOICE (upper case, immutable). Apps compose per-branch codes themselves.';
COMMENT ON COLUMN CORE_NUMBER_SERIES.PREFIX IS 'Value of the {PREFIX} token (optional).';
COMMENT ON COLUMN CORE_NUMBER_SERIES.PATTERN IS 'Tokens: {PREFIX} {YYYY} {YY} {MM} {SEQ:n} {TENANT}; exactly one {SEQ:n}.';
COMMENT ON COLUMN CORE_NUMBER_SERIES.RESET_POLICY IS 'NEVER | YEARLY | MONTHLY (immutable).';
COMMENT ON COLUMN CORE_NUMBER_SERIES.PERIOD_KEY IS 'Period of this row: '''' (NEVER), YYYY (YEARLY) or YYYY-MM (MONTHLY).';
COMMENT ON COLUMN CORE_NUMBER_SERIES.NEXT_VALUE IS 'Next sequence value handed out in this period (starts at 1).';

ALTER TABLE CORE_NUMBER_SERIES ADD CONSTRAINT PK_CORE_NUMBER_SERIES PRIMARY KEY (ID);
ALTER TABLE CORE_NUMBER_SERIES ADD CONSTRAINT UQ_CORE_NUMBER_SERIES_CODE_PERIOD UNIQUE (TENANT_ID, CODE, PERIOD_KEY);
ALTER TABLE CORE_NUMBER_SERIES ADD CONSTRAINT FK_CORE_NUMBER_SERIES_TENANT FOREIGN KEY (TENANT_ID) REFERENCES CORE_TENANT (ID);
ALTER TABLE CORE_NUMBER_SERIES ADD CONSTRAINT CHK_CORE_NUMBER_SERIES_RESET CHECK (RESET_POLICY IN ('NEVER','YEARLY','MONTHLY'));
ALTER TABLE CORE_NUMBER_SERIES ADD CONSTRAINT CHK_CORE_NUMBER_SERIES_NEXT CHECK (NEXT_VALUE >= 1);
CREATE INDEX IDX_CORE_NUMBER_SERIES_TENANT ON CORE_NUMBER_SERIES (TENANT_ID);

-- ============================================================
-- 2. CU_APP_CONFIGURATION: platform defaults (TENANT_ID NULL) + tenant overrides
-- ============================================================
ALTER TABLE CU_APP_CONFIGURATION ALTER COLUMN TENANT_ID DROP NOT NULL;
ALTER TABLE CU_APP_CONFIGURATION DROP CONSTRAINT UQ_CU_APP_CONFIG_CONFIG_KEY;
CREATE UNIQUE INDEX UQ_CU_APP_CONFIG_CONFIG_KEY ON CU_APP_CONFIGURATION ((COALESCE(TENANT_ID, 0)), CONFIG_KEY);
COMMENT ON COLUMN CU_APP_CONFIGURATION.TENANT_ID IS 'NULL = platform default; non-null = override of that tenant (erp-core step 09).';
