# DATABASE — سلاسل الترقيم / Sequence (SEQUENCE)
══════════════════════════════════════════════════════════════════
Module : SEQUENCE   Version : v1 (as-built baseline, erp-core 1.2.0)   Dialect : postgresql16   Schema prefix : none
Identifier transformation : physical names are UPPER_SNAKE_CASE as created by the migration; the SRS
  logical field name (camelCase) maps 1:1 (`periodKey` → `PERIOD_KEY`). Read from the migration, not designed here.
Date : 2026-10-07
Counts : 1 table (`CORE_NUMBER_SERIES`) · 1 sequence · 14 DBF · 3 XM (1 HARD-FK + SOFT-READ, 1 SPI implemented, 1 exposed surface)
══════════════════════════════════════════════════════════════════

Source of every row: `erp-core/src/main/resources/db/migration/core/V14__sequence_and_settings.sql` §1
(lines 22–61) at main @ 19b19a4 — the only script that touches `CORE_NUMBER_SERIES`. `V14:line` below =
that file and line. V14 §2 (lines 63–69, `CU_APP_CONFIGURATION`) belongs to CU and is recorded in
`../../CU/P2/db-script.md` → "Implementation Addendum — erp-core 1.2.0". Java paths are relative to
`erp-core/src/main/java/com/erp/`. The core chain is additive only (`MigrationNamingTest`); nothing
here may be renamed or retyped.

## 1. DB FIELD TRACEABILITY MATRIX — SEQUENCE v1

### Table CORE_NUMBER_SERIES (ENT-SEQUENCE-001) — tenant-scoped
| DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default | Source |
|---|---|---|---|---|---|---|---|
| DBF-SEQUENCE-001 | ID | BIGINT | ENT-SEQUENCE-001.id (PK `PK_CORE_NUMBER_SERIES`, from `SEQ_CORE_NUMBER_SERIES`; lowest id per code = anchor) | REQ-SEQUENCE-001, -006, -010 | NOT NULL | — (sequence, `allocationSize = 1`) | V14:32, :56; sequence/entity/NumberSeries.java:53-57 |
| DBF-SEQUENCE-002 | TENANT_ID | BIGINT | ENT-SEQUENCE-001.tenantId (`AuditableEntity` `@TenantId`; FK `FK_CORE_NUMBER_SERIES_TENANT` → `CORE_TENANT(ID)`; index `IDX_CORE_NUMBER_SERIES_TENANT`) | REQ-SEQUENCE-016, -017 | NOT NULL | — (no default) | V14:33, :58, :61; common/domain/AuditableEntity.java:35-36 |
| DBF-SEQUENCE-003 | CODE | VARCHAR(50) | ENT-SEQUENCE-001.code (upper case, immutable; part of `UQ_CORE_NUMBER_SERIES_CODE_PERIOD`) | REQ-SEQUENCE-001, -004, -010 | NOT NULL | — | V14:34, :49, :57; sequence/entity/NumberSeries.java:59-62 |
| DBF-SEQUENCE-004 | PREFIX | VARCHAR(20) | ENT-SEQUENCE-001.prefix (value of `{PREFIX}`) | REQ-SEQUENCE-001, -008, -014 | NULL | — | V14:35, :50; sequence/entity/NumberSeries.java:64-66 |
| DBF-SEQUENCE-005 | PATTERN | VARCHAR(100) | ENT-SEQUENCE-001.pattern (RULE-SEQUENCE-001, -002) | REQ-SEQUENCE-001…003, -008, -014 | NOT NULL | `'{PREFIX}-{YYYY}-{SEQ:6}'` | V14:36, :51; sequence/entity/NumberSeries.java:51, :68-72 |
| DBF-SEQUENCE-006 | RESET_POLICY | VARCHAR(10) | ENT-SEQUENCE-001.resetPolicy (A6; `CHK_CORE_NUMBER_SERIES_RESET`; immutable) | REQ-SEQUENCE-001, -003, -011 | NOT NULL | `'YEARLY'` | V14:37, :52, :59; sequence/entity/NumberSeries.java:74-78 |
| DBF-SEQUENCE-007 | PERIOD_KEY | VARCHAR(7) | ENT-SEQUENCE-001.periodKey (`''` / `YYYY` / `YYYY-MM`; immutable; part of the unique) | REQ-SEQUENCE-001, -011 | NOT NULL | `''` | V14:38, :53, :57; sequence/entity/NumberSeries.java:80-83 |
| DBF-SEQUENCE-008 | NEXT_VALUE | BIGINT | ENT-SEQUENCE-001.nextValue (`CHK_CORE_NUMBER_SERIES_NEXT` ≥ 1; moved only by allocation) | REQ-SEQUENCE-010, -011, -013 | NOT NULL | 1 | V14:39, :54, :60; sequence/entity/NumberSeries.java:85-88, :112-116 |
| DBF-SEQUENCE-009 | IS_ACTIVE | BOOLEAN | ENT-SEQUENCE-001.isActive (native boolean, no converter) | REQ-SEQUENCE-009, -012 | NOT NULL | TRUE | V14:40; sequence/entity/NumberSeries.java:90-92 |
| DBF-SEQUENCE-010 | CREATED_BY | VARCHAR(100) | audit (`GlobalAuditableEntity`) | REQ-SEQUENCE-001, -017 | NULL | — | V14:41; common/domain/GlobalAuditableEntity.java:34 |
| DBF-SEQUENCE-011 | CREATED_AT | TIMESTAMPTZ | audit | REQ-SEQUENCE-001 | NOT NULL | now() | V14:42 |
| DBF-SEQUENCE-012 | UPDATED_BY | VARCHAR(100) | audit | REQ-SEQUENCE-008, -009 | NULL | — | V14:43 |
| DBF-SEQUENCE-013 | UPDATED_AT | TIMESTAMPTZ | audit | REQ-SEQUENCE-008, -009 | NULL | — | V14:44; common/domain/GlobalAuditableEntity.java:43 |
| DBF-SEQUENCE-014 | VERSION | BIGINT | optimistic lock (`@Version`) | REQ-SEQUENCE-008, -010 | NOT NULL | 0 | V14:45; common/domain/GlobalAuditableEntity.java:51-52 |

Constraints, indexes and sequence of `CORE_NUMBER_SERIES`
| Object | Definition | Source |
|---|---|---|
| `SEQ_CORE_NUMBER_SERIES` | `START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE` (entity generator `core_number_series_seq`, `allocationSize = 1`) | V14:25-29; sequence/entity/NumberSeries.java:54-55 |
| `PK_CORE_NUMBER_SERIES` | `PRIMARY KEY (ID)` | V14:56 |
| `UQ_CORE_NUMBER_SERIES_CODE_PERIOD` | `UNIQUE (TENANT_ID, CODE, PERIOD_KEY)` (also `@UniqueConstraint` on the entity) | V14:57; sequence/entity/NumberSeries.java:43-45 |
| `FK_CORE_NUMBER_SERIES_TENANT` | `FOREIGN KEY (TENANT_ID) REFERENCES CORE_TENANT (ID)` | V14:58 |
| `CHK_CORE_NUMBER_SERIES_RESET` | `CHECK (RESET_POLICY IN ('NEVER','YEARLY','MONTHLY'))` | V14:59 |
| `CHK_CORE_NUMBER_SERIES_NEXT` | `CHECK (NEXT_VALUE >= 1)` | V14:60 |
| `IDX_CORE_NUMBER_SERIES_TENANT` | `(TENANT_ID)` | V14:61 |
| Comments | table; `CODE`, `PREFIX`, `PATTERN`, `RESET_POLICY`, `PERIOD_KEY`, `NEXT_VALUE` | V14:48-54 |
| Seed | none in core — applications seed series for `TENANT_ID = 1` in their own `V1000+` scripts (docs/CONSUMING.md §3); provisioning copies them into new tenants | sequence/tenant/SequenceTenantProvisioningContributor.java:34-42 |
No FK leaves `CORE_NUMBER_SERIES` besides the tenant FK; no table references it. Timestamps are
`TIMESTAMPTZ`, the convention of the core chain (contrast `CORE_AUDIT_EVENT`, `../../AUDIT/P2/db-script-audit.md`).

## 2. XM REGISTER — SEQUENCE v1

| XM-ID | Type | Surface / column | Target / implementers | Physical object | Status |
|---|---|---|---|---|---|
| XM-SEQUENCE-001 | HARD-FK + SOFT-READ (consumed) | `CORE_NUMBER_SERIES.TENANT_ID` (DBF-SEQUENCE-002) → `CORE_TENANT.ID`; `TenantLookupApi.codeOf(Long)` → `CORE_TENANT.CODE` for the `{TENANT}` token | TENANT (ENT-TENANT-001; tenant side XM-TENANT-001) | `FK_CORE_NUMBER_SERIES_TENANT` (V14:58); read through `TenantRepository` (no SQL of SEQUENCE's own) | ACTIVE |
| XM-SEQUENCE-002 | SPI implemented (consumed) | `TenantProvisioningContributor.provision(TenantProvisioning)` — `INSERT INTO CORE_NUMBER_SERIES … SELECT … FROM CORE_NUMBER_SERIES s WHERE s.TENANT_ID = ? AND s.ID = (SELECT MIN(x.ID) … )`, `NEXT_VALUE = 1`, order 40 | TENANT (tenant side XM-TENANT-002) | writes `CORE_NUMBER_SERIES` with explicit `TENANT_ID` (sequence/tenant/SequenceTenantProvisioningContributor.java:34-42) | ACTIVE |
| XM-SEQUENCE-003 | crossmodule call (exposed) | `NumberSeriesApi.next(String)` / `preview(String)` → `CORE_NUMBER_SERIES` of the current tenant (`SELECT … FOR UPDATE` by Hibernate, DBF-SEQUENCE-008 moved) | applications; no core consumer | none (Java interface; JPQL through `NumberSeriesRepository`, tenant predicate added by Hibernate) | ACTIVE |

## 3. FULL_DATABASE_SCRIPT (as built — verbatim extract of `V14__sequence_and_settings.sql` §1)

```sql
-- V14 §1 (lines 22-61): CORE_NUMBER_SERIES
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
```
The allocation's lock statement Hibernate emits against this table (docs/steps/09-report.md
"Verification output"): `select … from core_number_series ns1_0 where ns1_0.tenant_id = ? and
ns1_0.code = ? order by ns1_0.id fetch first ? rows only for no key update of ns1_0`, followed by
`update core_number_series set …, next_value = ?, … where id = ? and version = ?`.

## 4. DECISIONS APPLIED

| DEFAULT / ADR | What | Source | Status |
|---|---|---|---|
| ADR-SEQUENCE-001 | the counter is serialised by a pessimistic lock on the code's anchor row in a `REQUIRES_NEW` transaction (no database sequence per series; gaps accepted) | governance/analysis/decisions/SEQUENCE/ADR-SEQUENCE-001.md | ACCEPTED (as built) |
| ADR-SEQUENCE-002 | one row per (code, period): `PERIOD_KEY` in the unique key, `RESET_POLICY` CHECK-constrained and immutable | governance/analysis/decisions/SEQUENCE/ADR-SEQUENCE-002.md | ACCEPTED (as built) |
| ADR-SEQUENCE-003 | no delete; `NEXT_VALUE` never edited through the API; provisioning copies anchor rows with `NEXT_VALUE = 1` | governance/analysis/decisions/SEQUENCE/ADR-SEQUENCE-003.md | ACCEPTED (as built) |
| DEFAULT | PK column `ID` (new-table convention), `SEQ_CORE_NUMBER_SERIES` with `CACHE 1` | docs/steps/09-report.md "Skills checked" (build-create-entity); db/migration/core/README.md §4 | as built |
| DEFAULT | `IS_ACTIVE BOOLEAN` (native boolean for new tables), not `IS_ACTIVE_FL SMALLINT` | docs/steps/09-report.md "Skills checked"; SEC platform-summary "Naming" convention | as built |
| DEFAULT | `RESET_POLICY` is a CHECK + Java enum, not an MDL lookup | V14:59; pattern of ADR-SEC-001 | as built |

## 5. REGISTRY CONTENT
See `registry-db-sequence.md`.

## 6. DBF id definitions (cross-reference index — full detail in §1; `[traces]` = ENT + REQ)
**DBF-SEQUENCE-001** — CORE_NUMBER_SERIES.ID [ENT-SEQUENCE-001, REQ-SEQUENCE-001, REQ-SEQUENCE-006, REQ-SEQUENCE-010]
**DBF-SEQUENCE-002** — CORE_NUMBER_SERIES.TENANT_ID [ENT-SEQUENCE-001, REQ-SEQUENCE-016, REQ-SEQUENCE-017]
**DBF-SEQUENCE-003** — CORE_NUMBER_SERIES.CODE [ENT-SEQUENCE-001, REQ-SEQUENCE-001, REQ-SEQUENCE-004, REQ-SEQUENCE-010]
**DBF-SEQUENCE-004** — CORE_NUMBER_SERIES.PREFIX [ENT-SEQUENCE-001, REQ-SEQUENCE-001, REQ-SEQUENCE-008, REQ-SEQUENCE-014]
**DBF-SEQUENCE-005** — CORE_NUMBER_SERIES.PATTERN [ENT-SEQUENCE-001, REQ-SEQUENCE-001, REQ-SEQUENCE-002, REQ-SEQUENCE-003, REQ-SEQUENCE-008, REQ-SEQUENCE-014]
**DBF-SEQUENCE-006** — CORE_NUMBER_SERIES.RESET_POLICY [ENT-SEQUENCE-001, REQ-SEQUENCE-001, REQ-SEQUENCE-003, REQ-SEQUENCE-011]
**DBF-SEQUENCE-007** — CORE_NUMBER_SERIES.PERIOD_KEY [ENT-SEQUENCE-001, REQ-SEQUENCE-001, REQ-SEQUENCE-011]
**DBF-SEQUENCE-008** — CORE_NUMBER_SERIES.NEXT_VALUE [ENT-SEQUENCE-001, REQ-SEQUENCE-010, REQ-SEQUENCE-011, REQ-SEQUENCE-013]
**DBF-SEQUENCE-009** — CORE_NUMBER_SERIES.IS_ACTIVE [ENT-SEQUENCE-001, REQ-SEQUENCE-009, REQ-SEQUENCE-012]
**DBF-SEQUENCE-010** — CORE_NUMBER_SERIES.CREATED_BY [ENT-SEQUENCE-001, REQ-SEQUENCE-001, REQ-SEQUENCE-017]
**DBF-SEQUENCE-011** — CORE_NUMBER_SERIES.CREATED_AT [ENT-SEQUENCE-001, REQ-SEQUENCE-001]
**DBF-SEQUENCE-012** — CORE_NUMBER_SERIES.UPDATED_BY [ENT-SEQUENCE-001, REQ-SEQUENCE-008, REQ-SEQUENCE-009]
**DBF-SEQUENCE-013** — CORE_NUMBER_SERIES.UPDATED_AT [ENT-SEQUENCE-001, REQ-SEQUENCE-008, REQ-SEQUENCE-009]
**DBF-SEQUENCE-014** — CORE_NUMBER_SERIES.VERSION [ENT-SEQUENCE-001, REQ-SEQUENCE-008, REQ-SEQUENCE-010]
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 09
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.
