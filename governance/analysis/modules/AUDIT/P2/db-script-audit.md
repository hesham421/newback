# DATABASE — سجل التدقيق العام / Audit (AUDIT)
══════════════════════════════════════════════════════════════════
Module : AUDIT   Version : v1 (as-built baseline, erp-core 1.2.0)   Dialect : postgresql16   Schema prefix : none
Identifier transformation : physical names are UPPER_SNAKE_CASE as created by the migration; the SRS
  logical field name (camelCase) maps 1:1 (`occurredAt` → `OCCURRED_AT`, `actorRealm` → `ACTOR_REALM`).
  Read from the migration, not designed here.
Date : 2026-10-07
Counts : 1 table (`CORE_AUDIT_EVENT`) · 1 sequence · 20 DBF · 4 XM (1 HARD-FK, 2 exposed surfaces, 1 SPI implemented)
══════════════════════════════════════════════════════════════════

Source of every row: `erp-core/src/main/resources/db/migration/core/V15__audit_schema.sql` at main @
19b19a4 — the only script that touches `CORE_AUDIT_EVENT`. `V15:line` below = that file and line. Java
paths are relative to `erp-core/src/main/java/com/erp/`. The core chain is additive only
(`MigrationNamingTest`); nothing here may be renamed or retyped. The table is written by JDBC only
(`audit/service/AuditEventStore.java:43-46`) and read by JPA (`audit/entity/AuditEvent.java`, `@Immutable`).

## 1. DB FIELD TRACEABILITY MATRIX — AUDIT v1

### Table CORE_AUDIT_EVENT (ENT-AUDIT-001) — tenant-scoped, append-only
| DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default | Source |
|---|---|---|---|---|---|---|---|
| DBF-AUDIT-001 | ID | BIGINT | ENT-AUDIT-001.id (PK `PK_CORE_AUDIT_EVENT`; `nextval('SEQ_CORE_AUDIT_EVENT')` in the JDBC insert) | REQ-AUDIT-001, -012 | NOT NULL | — (sequence) | V15:17, :39; audit/entity/AuditEvent.java:40-44; audit/service/AuditEventStore.java:46 |
| DBF-AUDIT-002 | TENANT_ID | BIGINT | ENT-AUDIT-001.tenantId (`AuditableEntity` `@TenantId` for reads; bound explicitly on insert; FK `FK_CORE_AUDIT_EVENT_TENANT`; index `IDX_CORE_AUDIT_EVENT_TENANT`) | REQ-AUDIT-003, -010, -012 | NOT NULL | — (no default) | V15:18, :40, :44; audit/service/AuditEventStore.java:66; common/domain/AuditableEntity.java:35-36 |
| DBF-AUDIT-003 | OCCURRED_AT | TIMESTAMP (without time zone; UTC wall-clock) | ENT-AUDIT-001.occurredAt (leads `IDX_CORE_AUDIT_EVENT_OCCURRED` after `TENANT_ID`) | REQ-AUDIT-001, -009, -012, -013, -018 | NOT NULL | — | V15:19, :46; audit/entity/AuditEvent.java:46-47; audit/service/AuditEventStore.java:67, :125-127; ADR-AUDIT-003 |
| DBF-AUDIT-004 | ACTOR | VARCHAR(100) | ENT-AUDIT-001.actor (username or `system`; cut at 100) | REQ-AUDIT-009, -011, -012 | NOT NULL | — | V15:20; audit/entity/AuditEvent.java:49-50; audit/service/AuditRecordingService.java:38, :67-68 |
| DBF-AUDIT-005 | ACTOR_REALM | VARCHAR(16) | ENT-AUDIT-001.actorRealm (A6; `CHK_CORE_AUDIT_EVENT_REALM`) | REQ-AUDIT-009 | NOT NULL | — | V15:21, :42; audit/entity/AuditEvent.java:52-53 |
| DBF-AUDIT-006 | ACTOR_USER_ID | BIGINT | ENT-AUDIT-001.actorUserId (no FK, no default; set by the caller) | REQ-AUDIT-009, -016 | NULL | — | V15:22; audit/entity/AuditEvent.java:55-56; audit/service/AuditEventStore.java:70-74 |
| DBF-AUDIT-007 | ACTION | VARCHAR(64) | ENT-AUDIT-001.action (`CHK_CORE_AUDIT_EVENT_ACTION` `^[A-Z_]{3,64}$`) | REQ-AUDIT-002, -005…007, -012, -016 | NOT NULL | — | V15:23, :41, :49; audit/entity/AuditEvent.java:58-59 |
| DBF-AUDIT-008 | ENTITY_TYPE | VARCHAR(128) | ENT-AUDIT-001.entityType (`@Audited.entityType`, by convention the table name; cut at 128; in `IDX_CORE_AUDIT_EVENT_ENTITY`) | REQ-AUDIT-005…007, -011, -012 | NULL | — | V15:24, :45; audit/entity/AuditEvent.java:61-62; audit/service/AuditRecordingService.java:39, :70 |
| DBF-AUDIT-009 | ENTITY_ID | VARCHAR(64) | ENT-AUDIT-001.entityId (the entity's id as text; cut at 64; in `IDX_CORE_AUDIT_EVENT_ENTITY`) | REQ-AUDIT-005…007, -011, -012 | NULL | — | V15:25, :45; audit/entity/AuditEvent.java:64-65; audit/listener/AuditedEntityListener.java:127 |
| DBF-AUDIT-010 | SUMMARY_AR | VARCHAR(1000) | ENT-AUDIT-001.summaryAr (cut at 1000) | REQ-AUDIT-001, -005…007, -011 | NULL | — | V15:26; audit/entity/AuditEvent.java:67-68; audit/service/AuditRecordingService.java:41, :72 |
| DBF-AUDIT-011 | SUMMARY_EN | VARCHAR(1000) | ENT-AUDIT-001.summaryEn (cut at 1000) | REQ-AUDIT-001, -005…007, -011 | NULL | — | V15:27; audit/entity/AuditEvent.java:70-71; audit/service/AuditRecordingService.java:73 |
| DBF-AUDIT-012 | CHANGES | JSONB | ENT-AUDIT-001.changes — JSON array of `{field, old, new}`; `CAST(? AS JSONB)` on insert; NULL when there is no change; read as JSON text (`@JsonRawValue` in the response) | REQ-AUDIT-005…008, -011 | NULL | — | V15:28, :50; audit/entity/AuditEvent.java:73-75; audit/service/AuditEventStore.java:46, :80, :111-123; audit/dto/AuditEventResponse.java:48-51 |
| DBF-AUDIT-013 | IP | VARCHAR(64) | ENT-AUDIT-001.ip (`getRemoteAddr()`, cut at 64) | REQ-AUDIT-009 | NULL | — | V15:29; audit/entity/AuditEvent.java:77-78; audit/web/RequestInfoHolder.java:32, :50 |
| DBF-AUDIT-014 | USER_AGENT | VARCHAR(256) | ENT-AUDIT-001.userAgent (`User-Agent` header, cut at 256) | REQ-AUDIT-009 | NULL | — | V15:30; audit/entity/AuditEvent.java:80-81; audit/web/RequestInfoHolder.java:33, :51 |
| DBF-AUDIT-015 | REFERENCE | VARCHAR(100) | ENT-AUDIT-001.reference (caller's correlation id; cut at 100) | REQ-AUDIT-001, -011 | NULL | — | V15:31, :51; audit/entity/AuditEvent.java:83-84; audit/service/AuditRecordingService.java:42, :79 |
| DBF-AUDIT-016 | CREATED_BY | VARCHAR(100) | audit column (`GlobalAuditableEntity`); bound to the actor on insert | REQ-AUDIT-001 | NULL | — | V15:32; audit/service/AuditEventStore.java:84; common/domain/GlobalAuditableEntity.java:34 |
| DBF-AUDIT-017 | CREATED_AT | TIMESTAMP (without time zone) | audit column; bound to now (UTC) on insert | REQ-AUDIT-001 | NULL | — | V15:33; audit/service/AuditEventStore.java:64, :85 |
| DBF-AUDIT-018 | UPDATED_BY | VARCHAR(100) | audit column; bound to the actor on insert (never updated afterwards) | REQ-AUDIT-015 | NULL | — | V15:34; audit/service/AuditEventStore.java:86 |
| DBF-AUDIT-019 | UPDATED_AT | TIMESTAMP (without time zone) | audit column; bound to now on insert (never updated afterwards) | REQ-AUDIT-015 | NULL | — | V15:35; audit/service/AuditEventStore.java:87 |
| DBF-AUDIT-020 | VERSION | BIGINT | optimistic lock column of the base class; always 0 (rows are never updated) | REQ-AUDIT-015 | NOT NULL | 0 | V15:36; audit/service/AuditEventStore.java:46 (`0`); common/domain/GlobalAuditableEntity.java:51-52 |

Constraints, indexes and sequence of `CORE_AUDIT_EVENT`
| Object | Definition | Source |
|---|---|---|
| `SEQ_CORE_AUDIT_EVENT` | `START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE` (entity generator `core_audit_event_seq`, `allocationSize = 1`, used for reads only; the insert calls `nextval` directly) | V15:14; audit/entity/AuditEvent.java:41-42; audit/service/AuditEventStore.java:46 |
| `PK_CORE_AUDIT_EVENT` | `PRIMARY KEY (ID)` | V15:39 |
| `FK_CORE_AUDIT_EVENT_TENANT` | `FOREIGN KEY (TENANT_ID) REFERENCES CORE_TENANT (ID)` | V15:40 |
| `CHK_CORE_AUDIT_EVENT_ACTION` | `CHECK (ACTION ~ '^[A-Z_]{3,64}$')` (also enforced in `AuditEventDomain` → `AUDIT_ACTION_INVALID`) | V15:41; audit/domain/AuditEventDomain.java:22 |
| `CHK_CORE_AUDIT_EVENT_REALM` | `CHECK (ACTOR_REALM IN ('STAFF', 'CUSTOMER', 'SYSTEM'))` | V15:42 |
| `IDX_CORE_AUDIT_EVENT_TENANT` | `(TENANT_ID)` — the step-05 tenant rule | V15:44 |
| `IDX_CORE_AUDIT_EVENT_ENTITY` | `(TENANT_ID, ENTITY_TYPE, ENTITY_ID)` — the entity-history query (also `@Index` on the entity) | V15:45; audit/entity/AuditEvent.java:33 |
| `IDX_CORE_AUDIT_EVENT_OCCURRED` | `(TENANT_ID, OCCURRED_AT)` — the newest-first listing and the retention delete (also `@Index` on the entity) | V15:46; audit/entity/AuditEvent.java:34 |
| Comments | table; `ACTION`, `CHANGES`, `REFERENCE` | V15:48-51 |
| Seed | none (`AUDIT:EVENT:READ` is deliberately NOT seeded: `AuditPermissions` contributes it) | V15:10-11 |
No unique constraint (every row is a distinct event). No FK leaves the table besides the tenant FK; no
table references it.

**Timestamp type.** `OCCURRED_AT`, `CREATED_AT` and `UPDATED_AT` are `TIMESTAMP` *without* time zone,
whereas every other core table of the chain uses `TIMESTAMPTZ` (e.g. `CORE_TENANT`
`V10__tenant_schema.sql:38`, `CORE_NUMBER_SERIES` `V14__sequence_and_settings.sql:42`). The JDBC writer
binds UTC wall-clock `LocalDateTime`s (`audit/service/AuditEventStore.java:125-127`) — the same value
Hibernate writes for an `Instant` into a `TIMESTAMP` column — so the JPA read model
(`AuditEvent.occurredAt` is an `Instant`) reads them back unchanged, and the API renders them as UTC
ISO-8601 (`audit/dto/AuditEventResponse.java:21`). The retention cut-off is bound the same way
(`AuditEventStore.java:97`). Recorded in ADR-AUDIT-003; changing the type is forbidden by the
additive-only rule.

## 2. XM REGISTER — AUDIT v1

| XM-ID | Type | Surface / column | Target / implementers | Physical object | Status |
|---|---|---|---|---|---|
| XM-AUDIT-001 | HARD-FK (consumed) | `CORE_AUDIT_EVENT.TENANT_ID` (DBF-AUDIT-002) → `CORE_TENANT.ID`; `TenantContext.require()` fills it for explicit entries and global entities | TENANT (ENT-TENANT-001; tenant side DBF-TENANT-032) | `FK_CORE_AUDIT_EVENT_TENANT` (V15:40) | ACTIVE |
| XM-AUDIT-002 | crossmodule call (exposed) | `AuditApi.record(AuditEntry)` → one `INSERT INTO CORE_AUDIT_EVENT` on the Spring-transaction-bound connection | SEC (`SecAuditEntries` — LOGIN, LOGOUT, PASSWORD_RESET); applications | `AuditEventStore.INSERT` (audit/service/AuditEventStore.java:43-46, :54-59) | ACTIVE |
| XM-AUDIT-003 | annotation + Hibernate listener (exposed) | `@Audited(entityType, ignore)` → `POST_INSERT` / `POST_UPDATE` / `POST_DELETE` → the same `INSERT` on the flushing session's connection (`Session.doWork`) | SEC `SEC_USER`, `SEC_ROLE`; TENANT `CORE_TENANT`; FILE `FILE_DOCUMENT`, `FILE_CATEGORY`; NOTIF `NOTIF_TEMPLATE`; MDL `MDL_LOOKUP_TYPE`, `MDL_LOOKUP_VALUE`; CU `CU_APP_CONFIGURATION`; SEQUENCE `CORE_NUMBER_SERIES` | `AuditEventStore.INSERT` (audit/service/AuditEventStore.java:62-90); audit/listener/AuditedEntityListener.java:138 | ACTIVE |
| XM-AUDIT-004 | SPI implemented (consumed) | `ReportProvider` — `AuditEventListReport` reads `CORE_AUDIT_EVENT` through `AuditEventRepository` + `SpecBuilder` (tenant-filtered by Hibernate, no native SQL) | report (`ReportRegistry`, `/api/v1/report/AUDIT_EVENT_LIST/{run,export}`) | none (JPQL) | ACTIVE |
Raw SQL of the module (all name `TENANT_ID`, step-05 rule; ArchUnit allow-list `com.erp.audit..`,
`erp-core/src/test/java/com/erp/architecture/CoreLibraryRulesArchTest.java:77`): the insert
(`AuditEventStore.java:43-46`), `SELECT DISTINCT TENANT_ID FROM CORE_AUDIT_EVENT WHERE OCCURRED_AT < ?`
and `DELETE FROM CORE_AUDIT_EVENT WHERE TENANT_ID = ? AND OCCURRED_AT < ?` (`AuditEventStore.java:98-103`).

## 3. FULL_DATABASE_SCRIPT (as built — verbatim extract of `V15__audit_schema.sql`)

```sql
-- V15 (lines 14-51): CORE_AUDIT_EVENT
CREATE SEQUENCE SEQ_CORE_AUDIT_EVENT START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;

CREATE TABLE CORE_AUDIT_EVENT (
  ID             BIGINT         NOT NULL,
  TENANT_ID      BIGINT         NOT NULL,
  OCCURRED_AT    TIMESTAMP      NOT NULL,
  ACTOR          VARCHAR(100)   NOT NULL,
  ACTOR_REALM    VARCHAR(16)    NOT NULL,
  ACTOR_USER_ID  BIGINT,
  ACTION         VARCHAR(64)    NOT NULL,
  ENTITY_TYPE    VARCHAR(128),
  ENTITY_ID      VARCHAR(64),
  SUMMARY_AR     VARCHAR(1000),
  SUMMARY_EN     VARCHAR(1000),
  CHANGES        JSONB,
  IP             VARCHAR(64),
  USER_AGENT     VARCHAR(256),
  REFERENCE      VARCHAR(100),
  CREATED_BY     VARCHAR(100),
  CREATED_AT     TIMESTAMP,
  UPDATED_BY     VARCHAR(100),
  UPDATED_AT     TIMESTAMP,
  VERSION        BIGINT         DEFAULT 0 NOT NULL
);

ALTER TABLE CORE_AUDIT_EVENT ADD CONSTRAINT PK_CORE_AUDIT_EVENT PRIMARY KEY (ID);
ALTER TABLE CORE_AUDIT_EVENT ADD CONSTRAINT FK_CORE_AUDIT_EVENT_TENANT FOREIGN KEY (TENANT_ID) REFERENCES CORE_TENANT (ID);
ALTER TABLE CORE_AUDIT_EVENT ADD CONSTRAINT CHK_CORE_AUDIT_EVENT_ACTION CHECK (ACTION ~ '^[A-Z_]{3,64}$');
ALTER TABLE CORE_AUDIT_EVENT ADD CONSTRAINT CHK_CORE_AUDIT_EVENT_REALM CHECK (ACTOR_REALM IN ('STAFF', 'CUSTOMER', 'SYSTEM'));

CREATE INDEX IDX_CORE_AUDIT_EVENT_TENANT   ON CORE_AUDIT_EVENT (TENANT_ID);
CREATE INDEX IDX_CORE_AUDIT_EVENT_ENTITY   ON CORE_AUDIT_EVENT (TENANT_ID, ENTITY_TYPE, ENTITY_ID);
CREATE INDEX IDX_CORE_AUDIT_EVENT_OCCURRED ON CORE_AUDIT_EVENT (TENANT_ID, OCCURRED_AT);

COMMENT ON TABLE CORE_AUDIT_EVENT IS 'Generic tenant-scoped audit trail (erp-core step 10): explicit AuditApi.record calls and @Audited entity changes.';
COMMENT ON COLUMN CORE_AUDIT_EVENT.ACTION IS 'CREATE, UPDATE, DELETE, STATUS_CHANGE, LOGIN, LOGOUT, PASSWORD_RESET, ... (free, validated ^[A-Z_]{3,64}$).';
COMMENT ON COLUMN CORE_AUDIT_EVENT.CHANGES IS 'JSON array of {field, old, new}; sensitive fields (password, secret, token, hash, ...) are never written.';
COMMENT ON COLUMN CORE_AUDIT_EVENT.REFERENCE IS 'Optional correlation id chosen by the caller.';
```
The write statement (`audit/service/AuditEventStore.java:43-46`):
`INSERT INTO CORE_AUDIT_EVENT (ID, TENANT_ID, OCCURRED_AT, ACTOR, ACTOR_REALM, ACTOR_USER_ID, ACTION,
ENTITY_TYPE, ENTITY_ID, SUMMARY_AR, SUMMARY_EN, CHANGES, IP, USER_AGENT, REFERENCE, CREATED_BY, CREATED_AT,
UPDATED_BY, UPDATED_AT, VERSION) VALUES (nextval('SEQ_CORE_AUDIT_EVENT'), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,
CAST(? AS JSONB), ?, ?, ?, ?, ?, ?, ?, 0)`.

## 4. DECISIONS APPLIED

| DEFAULT / ADR | What | Source | Status |
|---|---|---|---|
| ADR-AUDIT-001 | rows are inserted by JDBC on the writer's own connection (Spring-bound or the flushing Hibernate session's), never through the JPA entity; `AuditEvent` is an `@Immutable` read model | governance/analysis/decisions/AUDIT/ADR-AUDIT-001.md | ACCEPTED (as built) |
| ADR-AUDIT-002 | `CHANGES` never carries a denylisted, technical or `@Audited(ignore)` field; collections and binary values are skipped | governance/analysis/decisions/AUDIT/ADR-AUDIT-002.md | ACCEPTED (as built) |
| ADR-AUDIT-003 | `SEC_AUDIT_LOG` kept beside this table; retention by the application; `OCCURRED_AT` (and the audit columns) `TIMESTAMP` without zone bound as UTC wall-clock | governance/analysis/decisions/AUDIT/ADR-AUDIT-003.md | ACCEPTED (as built) |
| DEFAULT | widths and nullability the step left open: `ACTOR VARCHAR(100)`, `ACTOR_REALM VARCHAR(16)` + CHECK, `SUMMARY_* VARCHAR(1000)`, `REFERENCE VARCHAR(100)`, `ENTITY_TYPE` / `ENTITY_ID` nullable, `ACTION` CHECK, audit columns `VARCHAR(100)`, extra `(TENANT_ID)` index | docs/DEVIATIONS.md [10] (column details entry) | as built |
| DEFAULT | PK column `ID` (new-table convention); `SEQ_CORE_AUDIT_EVENT` `CACHE 1` | docs/steps/10-report.md "Skills checked" (build-create-entity); db/migration/core/README.md §4 | as built |
| DEFAULT | no permission seed in V15 (`AuditPermissions` contributes `AUDIT:EVENT:READ`) | V15:10-11; docs/DEVIATIONS.md [10] (permission entry) | as built |

## 5. REGISTRY CONTENT
See `registry-db-audit.md`.

## 6. DBF id definitions (cross-reference index — full detail in §1; `[traces]` = ENT + REQ)
**DBF-AUDIT-001** — CORE_AUDIT_EVENT.ID [ENT-AUDIT-001, REQ-AUDIT-001, REQ-AUDIT-012]
**DBF-AUDIT-002** — CORE_AUDIT_EVENT.TENANT_ID [ENT-AUDIT-001, REQ-AUDIT-003, REQ-AUDIT-010, REQ-AUDIT-012]
**DBF-AUDIT-003** — CORE_AUDIT_EVENT.OCCURRED_AT [ENT-AUDIT-001, REQ-AUDIT-001, REQ-AUDIT-009, REQ-AUDIT-012, REQ-AUDIT-013, REQ-AUDIT-018]
**DBF-AUDIT-004** — CORE_AUDIT_EVENT.ACTOR [ENT-AUDIT-001, REQ-AUDIT-009, REQ-AUDIT-011, REQ-AUDIT-012]
**DBF-AUDIT-005** — CORE_AUDIT_EVENT.ACTOR_REALM [ENT-AUDIT-001, REQ-AUDIT-009]
**DBF-AUDIT-006** — CORE_AUDIT_EVENT.ACTOR_USER_ID [ENT-AUDIT-001, REQ-AUDIT-009, REQ-AUDIT-016]
**DBF-AUDIT-007** — CORE_AUDIT_EVENT.ACTION [ENT-AUDIT-001, REQ-AUDIT-002, REQ-AUDIT-005, REQ-AUDIT-006, REQ-AUDIT-007, REQ-AUDIT-012, REQ-AUDIT-016]
**DBF-AUDIT-008** — CORE_AUDIT_EVENT.ENTITY_TYPE [ENT-AUDIT-001, REQ-AUDIT-005, REQ-AUDIT-006, REQ-AUDIT-007, REQ-AUDIT-011, REQ-AUDIT-012]
**DBF-AUDIT-009** — CORE_AUDIT_EVENT.ENTITY_ID [ENT-AUDIT-001, REQ-AUDIT-005, REQ-AUDIT-006, REQ-AUDIT-007, REQ-AUDIT-011, REQ-AUDIT-012]
**DBF-AUDIT-010** — CORE_AUDIT_EVENT.SUMMARY_AR [ENT-AUDIT-001, REQ-AUDIT-001, REQ-AUDIT-005, REQ-AUDIT-006, REQ-AUDIT-007, REQ-AUDIT-011]
**DBF-AUDIT-011** — CORE_AUDIT_EVENT.SUMMARY_EN [ENT-AUDIT-001, REQ-AUDIT-001, REQ-AUDIT-005, REQ-AUDIT-006, REQ-AUDIT-007, REQ-AUDIT-011]
**DBF-AUDIT-012** — CORE_AUDIT_EVENT.CHANGES [ENT-AUDIT-001, REQ-AUDIT-005, REQ-AUDIT-006, REQ-AUDIT-007, REQ-AUDIT-008, REQ-AUDIT-011]
**DBF-AUDIT-013** — CORE_AUDIT_EVENT.IP [ENT-AUDIT-001, REQ-AUDIT-009]
**DBF-AUDIT-014** — CORE_AUDIT_EVENT.USER_AGENT [ENT-AUDIT-001, REQ-AUDIT-009]
**DBF-AUDIT-015** — CORE_AUDIT_EVENT.REFERENCE [ENT-AUDIT-001, REQ-AUDIT-001, REQ-AUDIT-011]
**DBF-AUDIT-016** — CORE_AUDIT_EVENT.CREATED_BY [ENT-AUDIT-001, REQ-AUDIT-001]
**DBF-AUDIT-017** — CORE_AUDIT_EVENT.CREATED_AT [ENT-AUDIT-001, REQ-AUDIT-001]
**DBF-AUDIT-018** — CORE_AUDIT_EVENT.UPDATED_BY [ENT-AUDIT-001, REQ-AUDIT-015]
**DBF-AUDIT-019** — CORE_AUDIT_EVENT.UPDATED_AT [ENT-AUDIT-001, REQ-AUDIT-015]
**DBF-AUDIT-020** — CORE_AUDIT_EVENT.VERSION [ENT-AUDIT-001, REQ-AUDIT-015]
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 10
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.
