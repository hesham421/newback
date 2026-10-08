<!-- DB Script — Governed by Database Governance Engine (Project 2 / MODE 1.5) -->

# DB SCRIPT — Common Utils (CU)

## 1. DB SCRIPT HEADER
```
DBS-ID          : DBS-CU-001
Module          : Common Utils (CU)
SRS Feature Code: CU-001  (srs-CU.md v1.0)
Platform        : Foundation (Domain: ERP)
DB_TARGET       : POSTGRESQL_16   (confirmed by Architect 2026-09-02; aligns to master-registry v1.0.0)
Date            : 2026-09-02
Status          : GATE PASSED
Open Questions  : None
Tables          : 1 (CU_APP_CONFIGURATION)
XM Dependencies : None (ROOT module — no cross-module dependencies)
Lookup Tables   : None — LOVs are runtime-loaded codes per SRS A5 (no MD_MASTER_LOOKUP in Foundation scope)
```

> Governed design note (SRS-governs-DB, Layer 1 > Layer 2): srs-CU.md A5 declares CU owns zero LOVs and explicitly excludes any central MD_MASTER_LOOKUP in Foundation scope. The Project-2 default shared-lookup pattern is therefore overridden by the authoritative SRS — no lookup tables and no seed data are generated.
> Audit columns (CREATED_BY, CREATED_AT, UPDATED_BY, UPDATED_AT) are mandatory on every table and populated by AuditEntityListener. They are created in DDL but, per convention, are not assigned DBF-IDs.

## 2. DB FIELD TRACEABILITY MATRIX — Common Utils — DBS-ID: DBS-CU-001
```
DBF-ID    | Table Name           | Column Name    | DB Type        | SRS Source
DBF-0001  | CU_APP_CONFIGURATION | ID             | BIGINT         | ENTITY-CU-001.appConfigurationPk
DBF-0002  | CU_APP_CONFIGURATION | CONFIG_KEY     | VARCHAR(150)   | ENTITY-CU-001.configKey
DBF-0003  | CU_APP_CONFIGURATION | CONFIG_VALUE   | TEXT           | ENTITY-CU-001.configValue
DBF-0004  | CU_APP_CONFIGURATION | NOTES          | VARCHAR(2000)  | ENTITY-CU-001.notes
DBF-0005  | CU_APP_CONFIGURATION | IS_ACTIVE_FL   | SMALLINT       | ENTITY-CU-001.isActiveFl
Total: 5 DBF-IDs across 1 table
```

## 3. CROSS-MODULE DEPENDENCY REGISTER (XM REGISTER) — Common Utils — DBS-ID: DBS-CU-001
```
(none) — CU is the ROOT cross-cutting library; it has no outbound cross-module dependencies.
```
> Note: SEC/FILE/NOTIF consume CU as a library (code injection), which is not an XM dependency (master-registry §8).

## 4. FULL_DATABASE_SCRIPT
```sql
-- ============================================================
-- FULL DATABASE SCRIPT — Common Utils (CU) — DBS-CU-001
-- Target: POSTGRESQL_16   |   Execute in psql / pgAdmin
-- ============================================================

-- ============================================================
-- BLOCK 1: SEQUENCES
-- ============================================================
CREATE SEQUENCE SEQ_CU_APP_CONFIGURATION
  START WITH 1
  INCREMENT BY 1
  NO CACHE
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
-- BLOCK 3: CHILD TABLES (intra-module FK dependencies)
-- ============================================================
-- (none)

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
-- 5d. INTRA-MODULE FK
-- (none)

-- ============================================================
-- BLOCK 6: TRIGGERS
-- ============================================================
-- (none — PK population handled by application framework; no audit triggers governed by SRS)

-- ============================================================
-- BLOCK 7: INDEXES
-- ============================================================
-- (CONFIG_KEY already indexed via UQ constraint; no additional indexes required)

-- ============================================================
-- BLOCK 8: LOOKUP SEED DATA
-- ============================================================
-- (none — CU owns zero LOVs; no MD_MASTER_LOOKUP in Foundation scope per srs-CU.md A5)

-- ============================================================
-- BLOCK 9: VIEWS
-- ============================================================
-- (none)

-- ============================================================
-- BLOCK 10: FUNCTIONS AND PROCEDURES
-- ============================================================
-- (none)

-- ============================================================
-- BLOCK 11: DEFERRED FK PATCH BLOCKS
-- ============================================================
-- (none — no cross-module HARD-FKs)
```

## 5. DB REGISTRY UPDATE — MODE 1.5
```
REGISTRY UPDATE — 2026-09-02
Source Mode    : MODE 1.5
Feature Code   : CU-001
DBS-ID         : DBS-CU-001
New Tables     : CU_APP_CONFIGURATION
New Lookups    : None
XM-IDs Open    : None
OQ-IDs Open    : None
Gate Status    : PASSED
Next Action    : Trigger Project 3.1 — Execution Plan Governance Engine (Backend pass)
Table Registry rows to add (master-registry §7):
  DBS-CU-001 | CU_APP_CONFIGURATION | CU | key/value config store
Global XM Index rows to add (master-registry §8):
  (none)
Pipeline Status Grid: CU · P2 = done
```

---
*End of db-script-CU.md | DBS-CU-001 | POSTGRESQL_16 | 1 table, 5 DBF-IDs, 0 XM | Next: Project 3.1*

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 04, 05, 09 (migrations V2, V7, V10, V14; shipped in 1.2.0)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.
Revised        : 2026-10-07 — rows corrected and completed against the code (docs/plans/analysis-coverage-review.md)

Paths cited below are relative to `erp-core/src/main/resources/db/migration/core/` at that tag unless stated
otherwise. No DBF ids are minted here.

### Migration chain
| Kind | Migration | What it does for CU | Source |
|---|---|---|---|
| CHANGED | `V2__cu_schema.sql` (step 04) | creates `SEQ_CU_APP_CONFIGURATION` and `CU_APP_CONFIGURATION` — squashed from the old `V1__cu_app_configuration_schema`; the table DDL, the comments, `PK_CU_APP_CONFIGURATION`, `UQ_CU_APP_CONFIG_CONFIG_KEY (CONFIG_KEY)` and `CHK_CU_APP_CONFIG_ACTIVE_FL` are as in §4 above; the sequence differs (`CACHE 1`, see below); no seed rows | V2:11-50; docs/steps/04-report.md → "Old → new mapping" |
| NEW | `V7__sec_seed.sql` (step 04) | CU's registry rows in SEC's tables: module `CU` (`الأدوات المشتركة` / `Common Utilities`), screen `CU_CONFIGURATIONS` (`إدارة إعدادات المنصة` / `Platform Configuration`), actions `VIEW` / `CREATE` / `UPDATE` / `DEACTIVATE` with the literal codes `CONFIG_VIEW` / `CONFIG_CREATE` / `CONFIG_UPDATE` / `CONFIG_DEACTIVATE`; role `CU_ADMIN` (`مدير الإعدادات` / `Configuration Administrator`) with the `CU` module grant and the derived screen and action grants; `SYS_ADMIN` receives the `CU` module grant too | V7:40,66,114-117,151,172,175,187-200 |
| NEW | `V10__tenant_schema.sql` (step 05) | `TENANT_ID` and `VERSION` columns, `FK_CU_APP_CONFIGURATION_TENANT`, `IDX_CU_APP_CONFIGURATION_TENANT`, unique constraint re-created on `(TENANT_ID, CONFIG_KEY)`; registry module `PLATFORM` (`إدارة المنصة` / `Platform Administration`) under which `PLATFORM_SETTINGS` is later inserted by the startup catalog synchroniser (no migration seeds that screen) | V10:63,83,102,121,174-175,214 |
| NEW | `V14__sequence_and_settings.sql` (step 09) | `TENANT_ID` nullable, the unique constraint replaced by an expression unique index, column comment — the two non-additive edits are the exception the step-09 plan prescribes | V14:66-69 |

### Per-table deltas — CU_APP_CONFIGURATION
| Kind | Item | Delta | Migration |
|---|---|---|---|
| CHANGED | `SEQ_CU_APP_CONFIGURATION` | created with **`CACHE 1`** (§4 Block 1 says `NO CACHE`); same `START WITH 1 INCREMENT BY 1 NO CYCLE`; the entity's generator uses `allocationSize = 1` | V2:11-15; erp-core/src/main/java/com/erp/cu/entity/AppConfiguration.java:44-45 |
| NEW | `TENANT_ID BIGINT` | added `NOT NULL DEFAULT 1` (existing rows backfilled to 1 = PLATFORM), default dropped; FK **`FK_CU_APP_CONFIGURATION_TENANT`** → `CORE_TENANT (ID)`; index **`IDX_CU_APP_CONFIGURATION_TENANT (TENANT_ID)`** | V10:63,83,102,121 |
| NEW | `VERSION BIGINT NOT NULL DEFAULT 0` | optimistic lock (server-side only) | V10:63 |
| CHANGED | `UQ_CU_APP_CONFIG_CONFIG_KEY` | `UNIQUE (CONFIG_KEY)` → constraint `UNIQUE (TENANT_ID, CONFIG_KEY)` | V10:174-175 |
| CHANGED | `TENANT_ID` | `DROP NOT NULL` — NULL = platform default, non-null = override of that tenant (the one nullable `TENANT_ID` in core) | V14:66 |
| CHANGED | `UQ_CU_APP_CONFIG_CONFIG_KEY` | constraint dropped; unique **index** of the same name on `((COALESCE(TENANT_ID, 0)), CONFIG_KEY)`, so two platform defaults with one key are impossible too; the JPA `@UniqueConstraint` is removed from the entity because an expression index cannot be declared there | V14:67-68; AppConfiguration.java:33-35 |
| NEW | `COMMENT ON COLUMN CU_APP_CONFIGURATION.TENANT_ID` | `'NULL = platform default; non-null = override of that tenant (erp-core step 09).'` | V14:69 |
| CHANGED | `CONFIG_KEY VARCHAR(150) NOT NULL` | DDL unchanged; values are stored upper-case by the entity (RULE-CU-009), and "unique" in the V2 column comment now means unique per owner (comment text unchanged) | V2:22,37 |
| CHANGED | audit columns | `CREATED_BY` / `UPDATED_BY VARCHAR(255)`, `CREATED_AT` / `UPDATED_AT TIMESTAMP` as in §4; the entity (`GlobalAuditableEntity`) maps `length = 100` and `java.time.Instant`; no migration changes the column types — the two declarations disagree | V2:26-29; erp-core/src/main/java/com/erp/common/domain/GlobalAuditableEntity.java:34-44 |

Unchanged: `PK_CU_APP_CONFIGURATION (ID)`, `CHK_CU_APP_CONFIG_ACTIVE_FL (IS_ACTIVE_FL IN (0,1))`, `ID`, `CONFIG_VALUE TEXT`,
`NOTES VARCHAR(2000)`, `IS_ACTIVE_FL SMALLINT DEFAULT 1`.

### Deviations from this analysis
- `CONFIG_KEY UNIQUE` platform-wide → unique per owner (platform default or one tenant), step 09.
- `SEQ_CU_APP_CONFIGURATION NO CACHE` → `CACHE 1` (V2).
- `CU_APP_CONFIGURATION` is the one core table whose `TENANT_ID` is nullable (named exception in the step-09
  plan; docs/DEVIATIONS.md [09], [12] rule 2).
- §4 Block 8 "no seed data": still no row in a CU table; CU's registry rows and `CU_ADMIN` are seeded by V7 in
  SEC's tables, and the `PLATFORM_SETTINGS` screen by the startup synchroniser.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : shared helpers moved to com.erp.common (commit 6b01816; CHANGELOG [Unreleased]); no CU behaviour change
Statement      : The body and the 1.2.0 addendum above are unchanged; this addendum records the deltas being implemented for 1.3.0. Every row is verified against the code before the 1.3.0 tag.

| Kind | Item | Delta | Source |
|---|---|---|---|
| CHANGED | `AppConfigurationDomain` | uses `com.erp.common.domain.DomainRules` for RULE-CU-001 / RULE-CU-002; no schema effect | erp-core/src/main/java/com/erp/cu/domain/AppConfigurationDomain.java:39-40,51 |
| CHANGED | `ConfigurationService.owner` | uses `TenantContext.isPlatform()`; no schema effect | erp-core/src/main/java/com/erp/cu/service/ConfigurationService.java:214 |

No CU migration after V14; the core chain's last script is `V15__audit_schema.sql` (audit module).
