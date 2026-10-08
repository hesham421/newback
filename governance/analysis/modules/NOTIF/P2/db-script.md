<!-- DB Script — Governed by Database Governance Engine (Project 2 / MODE 1.5) -->

# DB SCRIPT — Notification Service (NOTIF)

## 1. DB SCRIPT HEADER
```
DBS-ID          : DBS-NOTIF-001
Module          : Notification Service (NOTIF)
SRS Feature Code: NOTIF-001  (srs-NOTIF.md v1.2 — OQ-NOTIF-001 RESOLVED)
Platform        : Foundation (Domain: ERP)
DB_TARGET       : POSTGRESQL_16   (confirmed by Architect 2026-09-02)
Date            : 2026-09-02
Status          : GATE PASSED
Open Questions  : None
Tables          : 3 (NOTIF_LOG, NOTIF_TEMPLATE, NOTIF_CHANNEL_CONFIG)
XM Dependencies : 2 SOFT-READ (XM-NOTIF-001 -> SEC, XM-NOTIF-002 -> FILE)
Lookup Tables   : None — LOV-NOTIF-001/002 are runtime-loaded codes per SRS A5 (no MD_MASTER_LOOKUP)
```

> Governed design notes (SRS-governs-DB, Layer 1 > Layer 2):
> - RECIPIENT_ID is a SOFT-READ to SEC's UserAccount (identity read at app layer, no FK) -> XM-NOTIF-001. Inactive-recipient dispatch is blocked at the app layer (RULE-NOTIF-007); historical logs retained.
> - ATTACHMENT_FILE_ID is a SOFT/service reference to FILE via the File Service API (no FK) -> XM-NOTIF-002.
> - CHANNEL_TYPE_ID (LOV-NOTIF-001) and NOTIFICATION_STATUS_ID (LOV-NOTIF-002) are runtime-loaded code columns — no lookup table, no CHECK.
> - NOTIF_CHANNEL_CONFIG uses IS_ENABLED_FL only (per SRS A3); no IS_ACTIVE_FL is invented.

## 2. DB FIELD TRACEABILITY MATRIX — Notification Service — DBS-ID: DBS-NOTIF-001
```
DBF-ID    | Table Name           | Column Name            | DB Type      | SRS Source
DBF-0001  | NOTIF_LOG            | ID                     | BIGINT       | ENTITY-NOTIF-001.notificationLogPk
DBF-0002  | NOTIF_LOG            | RECIPIENT_ID           | BIGINT       | ENTITY-NOTIF-001.recipientId (SOFT->SEC)
DBF-0003  | NOTIF_LOG            | CHANNEL_TYPE_ID        | VARCHAR(20)  | ENTITY-NOTIF-001.channelTypeId (LOV-NOTIF-001)
DBF-0004  | NOTIF_LOG            | NOTIFICATION_STATUS_ID | VARCHAR(30)  | ENTITY-NOTIF-001.notificationStatusId (LOV-NOTIF-002)
DBF-0005  | NOTIF_LOG            | MODULE_CODE            | VARCHAR(50)  | ENTITY-NOTIF-001.moduleCode
DBF-0006  | NOTIF_LOG            | REFERENCE_ID           | BIGINT       | ENTITY-NOTIF-001.referenceId
DBF-0007  | NOTIF_LOG            | REFERENCE_TYPE         | VARCHAR(100) | ENTITY-NOTIF-001.referenceType
DBF-0008  | NOTIF_LOG            | RETRY_COUNT            | SMALLINT     | ENTITY-NOTIF-001.retryCount
DBF-0009  | NOTIF_LOG            | ERROR_MESSAGE          | TEXT         | ENTITY-NOTIF-001.errorMessage
DBF-0010  | NOTIF_LOG            | SENT_AT                | TIMESTAMP    | ENTITY-NOTIF-001.sentAt
DBF-0011  | NOTIF_LOG            | TEMPLATE_FK            | BIGINT       | ENTITY-NOTIF-001.templateFk -> ENTITY-NOTIF-002
DBF-0012  | NOTIF_TEMPLATE       | ID                     | BIGINT       | ENTITY-NOTIF-002.notificationTemplatePk
DBF-0013  | NOTIF_TEMPLATE       | TEMPLATE_CODE          | VARCHAR(80)  | ENTITY-NOTIF-002.templateCode
DBF-0014  | NOTIF_TEMPLATE       | NAME_AR                | VARCHAR(200) | ENTITY-NOTIF-002.nameAr
DBF-0015  | NOTIF_TEMPLATE       | NAME_EN                | VARCHAR(100) | ENTITY-NOTIF-002.nameEn
DBF-0016  | NOTIF_TEMPLATE       | SUBJECT_AR             | VARCHAR(300) | ENTITY-NOTIF-002.subjectAr
DBF-0017  | NOTIF_TEMPLATE       | SUBJECT_EN             | VARCHAR(300) | ENTITY-NOTIF-002.subjectEn
DBF-0018  | NOTIF_TEMPLATE       | BODY_AR                | TEXT         | ENTITY-NOTIF-002.bodyAr
DBF-0019  | NOTIF_TEMPLATE       | BODY_EN                | TEXT         | ENTITY-NOTIF-002.bodyEn
DBF-0020  | NOTIF_TEMPLATE       | ATTACHMENT_FILE_ID     | BIGINT       | ENTITY-NOTIF-002.attachmentFileId (SOFT->FILE)
DBF-0021  | NOTIF_TEMPLATE       | IS_ACTIVE_FL           | SMALLINT     | ENTITY-NOTIF-002.isActiveFl
DBF-0022  | NOTIF_CHANNEL_CONFIG | ID                     | BIGINT       | ENTITY-NOTIF-003.notificationChannelConfigPk
DBF-0023  | NOTIF_CHANNEL_CONFIG | CHANNEL_TYPE_ID        | VARCHAR(20)  | ENTITY-NOTIF-003.channelTypeId (LOV-NOTIF-001)
DBF-0024  | NOTIF_CHANNEL_CONFIG | IS_ENABLED_FL          | SMALLINT     | ENTITY-NOTIF-003.isEnabledFl
DBF-0025  | NOTIF_CHANNEL_CONFIG | CONFIG_JSON            | TEXT         | ENTITY-NOTIF-003.configJson
Total: 25 DBF-IDs across 3 tables
```

## 3. CROSS-MODULE DEPENDENCY REGISTER (XM REGISTER) — Notification Service — DBS-ID: DBS-NOTIF-001
```
XM-ID         | Type      | This Table     | FK/Ref Column      | Target Table     | Target Module | Status
XM-NOTIF-001  | SOFT-READ | NOTIF_LOG      | RECIPIENT_ID       | SEC_USER_ACCOUNT | Security      | ACTIVE
XM-NOTIF-002  | SOFT-READ | NOTIF_TEMPLATE | ATTACHMENT_FILE_ID | FILE_DOCUMENT    | File Service  | ACTIVE
```
> XM-NOTIF-001 — recipient identity read from SEC at the app layer (no FK); dispatch to inactive recipients blocked by RULE-NOTIF-007. Target SEC_USER_ACCOUNT gated under DBS-SEC-001 -> ACTIVE.
> XM-NOTIF-002 — optional attachment resolved through the FILE FileService API (no FK). Target FILE_DOCUMENT gated under DBS-FILE-001 -> ACTIVE.

## 4. FULL_DATABASE_SCRIPT
```sql
-- ============================================================
-- FULL DATABASE SCRIPT — Notification Service (NOTIF) — DBS-NOTIF-001
-- Target: POSTGRESQL_16   |   Execute in psql / pgAdmin
-- ============================================================

-- ============================================================
-- BLOCK 1: SEQUENCES
-- ============================================================
CREATE SEQUENCE SEQ_NOTIF_LOG            START WITH 1 INCREMENT BY 1 NO CACHE NO CYCLE;
CREATE SEQUENCE SEQ_NOTIF_TEMPLATE       START WITH 1 INCREMENT BY 1 NO CACHE NO CYCLE;
CREATE SEQUENCE SEQ_NOTIF_CHANNEL_CONFIG START WITH 1 INCREMENT BY 1 NO CACHE NO CYCLE;

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
-- ============================================================
-- (none — LOV-NOTIF-001/002 are runtime-loaded codes; no MD_MASTER_LOOKUP per srs-NOTIF.md A5)

-- BLOCK 9: VIEWS            -- (none)
-- BLOCK 10: FUNCTIONS/PROCS -- (none)
-- BLOCK 11: DEFERRED FK BLOCKS
-- ============================================================
-- XM-NOTIF-001 (->SEC_USER_ACCOUNT) and XM-NOTIF-002 (->FILE_DOCUMENT) are SOFT-READ.
-- No physical FKs are created by design. No deferred patch required.
```

## 5. DB REGISTRY UPDATE — MODE 1.5
```
REGISTRY UPDATE — 2026-09-02
Source Mode : MODE 1.5 | Feature Code: NOTIF-001 | DBS-ID: DBS-NOTIF-001
New Tables  : NOTIF_LOG, NOTIF_TEMPLATE, NOTIF_CHANNEL_CONFIG
New Lookups : None
XM-IDs Open : XM-NOTIF-001 (SOFT-READ -> SEC_USER_ACCOUNT) ACTIVE;
              XM-NOTIF-002 (SOFT-READ -> FILE_DOCUMENT) ACTIVE
OQ-IDs Open : None
Gate Status : PASSED
Next Action : Trigger Project 3.1 — Execution Plan Governance Engine (Backend pass)
Table Registry rows to add (master-registry §7):
  DBS-NOTIF-001 | NOTIF_LOG | NOTIF
  DBS-NOTIF-001 | NOTIF_TEMPLATE | NOTIF
  DBS-NOTIF-001 | NOTIF_CHANNEL_CONFIG | NOTIF
Global XM Index rows to add (master-registry §8):
  XM-NOTIF-001 | NOTIF | SEC  | SOFT-READ | ACTIVE
  XM-NOTIF-002 | NOTIF | FILE | SOFT-READ | ACTIVE
Pipeline Status Grid: NOTIF · P2 = done
```

---
*End of db-script-NOTIF.md | DBS-NOTIF-001 | POSTGRESQL_16 | 3 tables, 25 DBF-IDs, 2 SOFT-READ XM | Next: Project 3.1*

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 01, 04, 05, 06, 08 (migrations V6, V9, V10, V11, V13); 15 reuses columns, no migration
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Paths cited below are relative to `erp-core/src/main/resources/db/migration/core/` at that tag unless
stated otherwise. No DBF ids are minted here.

### Migration chain
- Squashed in step 04: `NOTIF_TEMPLATE`, `NOTIF_CHANNEL_CONFIG`, `NOTIF_LOG` are created by
  `V6__notif_schema.sql` (DDL verbatim from the old `V6__notif_schema`); the EMAIL channel config and the
  `PASSWORD_RESET` / `ACCOUNT_ACTIVATION` templates are seeded by `V9__notif_seed.sql` (from the old
  `V11`, `V12`, `V32`). The `NOTIF_CHANNEL` / `NOTIF_STATUS` lookup rows live in MDL (`V8__mdl_seed.sql`).
  Full old → new mapping: `docs/steps/04-report.md` → "Old → new mapping".
- Later NOTIF changes are additive: `V10__tenant_schema.sql` (step 05), `V11__sec_realms.sql` (step 06,
  template seed), `V13__notif_async_inbox.sql` (step 08). Step 15 (1.2.0) reuses `NEXT_ATTEMPT_AT` as the
  delivery lease — no migration.

### Per-table deltas
| Table | Delta | Migration |
|---|---|---|
| NOTIF_TEMPLATE / NOTIF_CHANNEL_CONFIG / NOTIF_LOG | + `TENANT_ID BIGINT NOT NULL` (backfilled 1 = PLATFORM, default dropped, `FK_<TABLE>_TENANT` → `CORE_TENANT(ID)`, `IDX_<TABLE>_TENANT`); + `VERSION BIGINT NOT NULL DEFAULT 0` | V10 |
| NOTIF_TEMPLATE | `UQ_NOTIF_TEMPLATE_CODE` → `(TENANT_ID, TEMPLATE_CODE)` | V10 |
| NOTIF_CHANNEL_CONFIG | `UQ_NOTIF_CHANNEL_CONFIG_TYPE` → `(TENANT_ID, CHANNEL_TYPE_ID)` | V10 |
| NOTIF_TEMPLATE (seed) | + `CUSTOMER_VERIFY_EMAIL`, `CUSTOMER_PASSWORD_RESET` (AR/EN, `{actionLink}`, `{expiresAt}`) for every tenant existing at that time; later tenants copy them from PLATFORM | V11 §6 |
| NOTIF_LOG | + `ATTEMPTS INT NOT NULL DEFAULT 0`, + `NEXT_ATTEMPT_AT TIMESTAMP` (next retry while QUEUED; since 1.2.0 also the claim lease), + `LAST_ERROR TEXT`, + `VARIABLES_JSON TEXT` (cleared at final status); + index `IDX_NOTIF_LOG_STATUS_NEXT_ATTEMPT (NOTIFICATION_STATUS_ID, NEXT_ATTEMPT_AT)`. `RETRY_COUNT` and `ERROR_MESSAGE` kept and maintained. | V13 §1; DEVIATIONS [08], [15] |
| NOTIF_LOG.NOTIFICATION_STATUS_ID | new values `QUEUED`, `SKIPPED_NO_PROVIDER` (no CHECK on the column) | V13 header |
| NOTIF_INBOX | NEW: `ID BIGINT` (`SEQ_NOTIF_INBOX`), `TENANT_ID BIGINT NOT NULL` (FK / IDX to `CORE_TENANT`), `RECIPIENT_USER_ID BIGINT NOT NULL` (SEC_USER id, either realm — SOFT-READ, no FK), `TITLE_AR/EN VARCHAR(300) NOT NULL`, `BODY_AR/EN TEXT NOT NULL`, `READ_AT TIMESTAMP`, `REFERENCE_TYPE VARCHAR(100)`, `REFERENCE_ID BIGINT`, audit columns, `VERSION`; index `IDX_NOTIF_INBOX_RECIPIENT (TENANT_ID, RECIPIENT_USER_ID, READ_AT)` | V13 §2 |
| NOTIF_CHANNEL_CONFIG (seed) | + enabled `IN_APP` row (`CONFIG_JSON` NULL) for every existing tenant | V13 §3b |
| MDL_LOOKUP_VALUE (seed, NOTIF-owned types) | + `NOTIF_STATUS` `QUEUED`, `SKIPPED_NO_PROVIDER`; + `NOTIF_CHANNEL` `IN_APP` for every existing tenant | V13 §3a |

### Deviations from this analysis
- Analysis: `UNIQUE (TEMPLATE_CODE)` and unique channel type platform-wide. Implemented: unique per tenant
  (step 05).
- Analysis: `NOTIF_LOG` status lifecycle PENDING → SENT / FAILED / CHANNEL_DISABLED. Implemented: QUEUED
  is the persisted pending state and SKIPPED_NO_PROVIDER is a new final state (step 08; DEVIATIONS [08]).

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package D.3 — password-change e-mail (`STAFF_PASSWORD_CHANGED`)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Migration: `erp-core/src/main/resources/db/migration/core/V17__notif_seed_password_changed.sql` (seed only;
the plan expected `V21__notif_seed_password_changed.sql` "if it lands separately" — it does, one logical
change per file; numbers re-derived at creation time). No DDL.

### Seed
| Table | Rows | Values |
|---|---|---|
| NOTIF_TEMPLATE | one per `CORE_TENANT` row that has no `STAFF_PASSWORD_CHANGED` template yet | `TEMPLATE_CODE = 'STAFF_PASSWORD_CHANGED'`, `NAME_AR = 'إشعار تغيير كلمة مرور الموظف'`, `NAME_EN = 'Staff password changed'`, `SUBJECT_AR = 'تم تغيير كلمة المرور'`, `SUBJECT_EN = 'Your password was changed'`, `BODY_AR` / `BODY_EN` with `{changedAt}` and `{changedBy}`, `ATTACHMENT_FILE_ID` NULL, `IS_ACTIVE_FL = 1`, `CREATED_BY = 'SYSTEM'`, `CREATED_AT = CURRENT_TIMESTAMP`, `ID = nextval('SEQ_NOTIF_TEMPLATE')` |

Pattern: `INSERT … SELECT … FROM CORE_TENANT t CROSS JOIN (VALUES …) WHERE NOT EXISTS (…)` like V11 §6, plus the
`NOT EXISTS` guard against `UQ_NOTIF_TEMPLATE_CODE (TENANT_ID, TEMPLATE_CODE)` (an application may already
have created a template of that code). Name widths: `NAME_EN` ≤ 100, `SUBJECT_*` ≤ 300.

Package C12 (tenant-maturity plan §5 C.1): **no schema change, no migration.** `NOTIF_LOG` rows of a suspended tenant keep
`NOTIFICATION_STATUS_ID = 'QUEUED'` with `ATTEMPTS` and `NEXT_ATTEMPT_AT` untouched; the delivery claim and the requeue job
skip them by the tenant's status (`TenantLookupApi.isActive`, RULE-NOTIF-024 in `../P1/srs.md` 1.3.0 §5). The status
set (`CHK` / LOV-NOTIF-002) is unchanged.
