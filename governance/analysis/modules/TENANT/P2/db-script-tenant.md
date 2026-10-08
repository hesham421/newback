# DATABASE — المستأجرون / Tenant (TENANT)
══════════════════════════════════════════════════════════════════
Module : TENANT   Version : v1 (as-built baseline, erp-core 1.2.0)   Dialect : postgresql16   Schema prefix : none
Identifier transformation : physical names are UPPER_SNAKE_CASE as created by the migrations; the SRS
  logical field name (camelCase) maps 1:1 (`nameAr` → `NAME_AR`). Read from the migrations, not designed here.
Date : 2026-10-07
Counts : 1 table (`CORE_TENANT`) · 1 sequence · 32 DBF (10 `CORE_TENANT` columns + 22 `TENANT_ID`
         discriminator columns on other modules' tables) · 2 XM (exposed surfaces, no physical FK of TENANT's own)
══════════════════════════════════════════════════════════════════

Source of every row: the core Flyway chain `erp-core/src/main/resources/db/migration/core/` at main @
2274f86 — `V10__tenant_schema.sql` (step 05) creates `CORE_TENANT` and the first 18 `TENANT_ID` columns;
`V11__sec_realms.sql`, `V13__notif_async_inbox.sql`, `V14__sequence_and_settings.sql` and
`V15__audit_schema.sql` create the other 4 with their tables, and V14 makes `CU_APP_CONFIGURATION.TENANT_ID`
nullable. `Vnn:line` below = that file and line. Java paths are relative to `erp-core/src/main/java/com/erp/`.
The chain is additive only from V11 on (`MigrationNamingTest`); nothing here may be renamed or retyped.

## 1. DB FIELD TRACEABILITY MATRIX — TENANT v1

### Table CORE_TENANT (ENT-TENANT-001) — global, no `TENANT_ID`
| DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default | Source |
|---|---|---|---|---|---|---|---|
| DBF-TENANT-001 | ID | BIGINT | ENT-TENANT-001.id (PK `PK_CORE_TENANT`, from `SEQ_CORE_TENANT`) | REQ-TENANT-001, -006 | NOT NULL | — (sequence, `allocationSize = 1`) | V10:32, :48; tenant/entity/Tenant.java:42-46 |
| DBF-TENANT-002 | CODE | VARCHAR(32) | ENT-TENANT-001.code (`UQ_CORE_TENANT_CODE`, `CHK_CORE_TENANT_CODE`) | REQ-TENANT-001…003, -013 | NOT NULL | — | V10:33, :49, :51; tenant/entity/Tenant.java:48-51 |
| DBF-TENANT-003 | NAME_AR | VARCHAR(200) | ENT-TENANT-001.nameAr | REQ-TENANT-001 | NOT NULL | — | V10:34; tenant/entity/Tenant.java:53-56 |
| DBF-TENANT-004 | NAME_EN | VARCHAR(200) | ENT-TENANT-001.nameEn | REQ-TENANT-001 | NOT NULL | — | V10:35; tenant/entity/Tenant.java:58-61 |
| DBF-TENANT-005 | STATUS_CODE | VARCHAR(20) | ENT-TENANT-001.statusCode (`CHK_CORE_TENANT_STATUS`) | REQ-TENANT-008…010 | NOT NULL | 'ACTIVE' | V10:36, :50; tenant/entity/Tenant.java:63-66 |
| DBF-TENANT-006 | CREATED_BY | VARCHAR(100) | audit (`GlobalAuditableEntity`) | REQ-TENANT-001 | NOT NULL | — | V10:37; common/domain/GlobalAuditableEntity.java:34-35 |
| DBF-TENANT-007 | CREATED_AT | TIMESTAMPTZ | audit | REQ-TENANT-001 | NOT NULL | now() | V10:38; common/domain/GlobalAuditableEntity.java:37-38 |
| DBF-TENANT-008 | UPDATED_BY | VARCHAR(100) | audit | REQ-TENANT-008 | NULL | — | V10:39; common/domain/GlobalAuditableEntity.java:40-41 |
| DBF-TENANT-009 | UPDATED_AT | TIMESTAMPTZ | audit | REQ-TENANT-008 | NULL | — | V10:40; common/domain/GlobalAuditableEntity.java:43-44 |
| DBF-TENANT-010 | VERSION | BIGINT | optimistic lock (`@Version`) | REQ-TENANT-008 | NOT NULL | 0 | V10:41; common/domain/GlobalAuditableEntity.java:51-53 |

Constraints and sequence of `CORE_TENANT`
| Object | Definition | Source |
|---|---|---|
| `SEQ_CORE_TENANT` | `START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE`; set to 1 after the PLATFORM seed, so the first provisioned tenant gets id 2 | V10:29, :57 |
| `PK_CORE_TENANT` | `PRIMARY KEY (ID)` | V10:48 |
| `UQ_CORE_TENANT_CODE` | `UNIQUE (CODE)` (also `@UniqueConstraint` on the entity) | V10:49; tenant/entity/Tenant.java:34-38 |
| `CHK_CORE_TENANT_STATUS` | `CHECK (STATUS_CODE IN ('ACTIVE','SUSPENDED'))` | V10:50 |
| `CHK_CORE_TENANT_CODE` | `CHECK (CODE ~ '^[A-Z0-9_]{3,32}$')` | V10:51 |
| Comments | table: "Tenant registry (global — the one core table without TENANT_ID). ID 1 = PLATFORM."; `CODE`, `STATUS_CODE` | V10:44-46 |
| Seed | `(1, 'PLATFORM', 'المنصة', 'Platform', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP)` | V10:55-56 |
No FK leaves `CORE_TENANT`; no index besides the PK and the unique constraint.

### TENANT_ID discriminator columns (22) — `TENANT_ID BIGINT`, FK → `CORE_TENANT(ID)`
Rule (db/migration/core/README.md; docs/steps/05-report.md "Notes for later steps"): `TENANT_ID BIGINT NOT
NULL`, no default, `FK_<TABLE>_TENANT`, `IDX_<TABLE>_TENANT`, every unique constraint leads with
`TENANT_ID`. V10 added the column with `DEFAULT 1` (backfill into PLATFORM) and then dropped the default
(plan-sanctioned, V10:21-23, :82-100). The entity side is `AuditableEntity.tenantId` (`@TenantId`,
`updatable = false`, common/domain/AuditableEntity.java:35-37) for all but `CU_APP_CONFIGURATION`.

| DBF id | Table (owner module, entity) | Nullable | Column added | FK | Index | Tenant-leading uniques | Traces (REQ) |
|---|---|---|---|---|---|---|---|
| DBF-TENANT-011 | `CU_APP_CONFIGURATION` (CU, `AppConfiguration` — global entity, plain column) | **NULL** since V14 (NULL = platform default) | V10:63 (default dropped :83); `DROP NOT NULL` V14:66 | `FK_CU_APP_CONFIGURATION_TENANT` V10:102 | `IDX_CU_APP_CONFIGURATION_TENANT` V10:121 | `UQ_CU_APP_CONFIG_CONFIG_KEY` — constraint `(TENANT_ID, CONFIG_KEY)` V10:175, replaced by the unique index `((COALESCE(TENANT_ID, 0)), CONFIG_KEY)` V14:67-68 | REQ-TENANT-016 |
| DBF-TENANT-012 | `MDL_LOOKUP_TYPE` (MDL, `LookupType`) | NOT NULL | V10:64 (:84) | `FK_MDL_LOOKUP_TYPE_TENANT` V10:103 | `IDX_MDL_LOOKUP_TYPE_TENANT` V10:122 | `UQ_MDL_LOOKUP_TYPE_KEY (TENANT_ID, key)` V10:178 | REQ-TENANT-016, -020 |
| DBF-TENANT-013 | `MDL_LOOKUP_VALUE` (MDL, `LookupValue`) | NOT NULL | V10:65 (:85) | `FK_MDL_LOOKUP_VALUE_TENANT` V10:104 | `IDX_MDL_LOOKUP_VALUE_TENANT` V10:123 | `UQ_MDL_LOOKUP_VALUE_TYPE_CODE (TENANT_ID, lookup_type_id, code)` V10:180 | REQ-TENANT-016, -020 |
| DBF-TENANT-014 | `SEC_USER` (SEC, `User`) | NOT NULL | V10:66 (:86) | `FK_SEC_USER_TENANT` V10:105 | `IDX_SEC_USER_TENANT` V10:124 | `UQ_SEC_USER_USERNAME`, `UQ_SEC_USER_EMAIL` — `(TENANT_ID, …)` V10:183, :185, widened to `(TENANT_ID, REALM, …)` V11:43, :45 | REQ-TENANT-012, -013, -016, -020 |
| DBF-TENANT-015 | `SEC_ROLE` (SEC, `Role`) | NOT NULL | V10:67 (:87) | `FK_SEC_ROLE_TENANT` V10:106 | `IDX_SEC_ROLE_TENANT` V10:125 | `UQ_SEC_ROLE_CODE (TENANT_ID, code)` V10:187 | REQ-TENANT-016, -020 |
| DBF-TENANT-016 | `SEC_USER_ROLE` (SEC, `UserRoleAssignment`) | NOT NULL | V10:68 (:88) | `FK_SEC_USER_ROLE_TENANT` V10:107 | `IDX_SEC_USER_ROLE_TENANT` V10:126 | `UQ_SEC_USER_ROLE_USER_ROLE (TENANT_ID, user_id, role_id)` V10:189 | REQ-TENANT-016, -020 |
| DBF-TENANT-017 | `SEC_ROLE_MODULE_GRANT` (SEC, `RoleModuleGrant`) | NOT NULL | V10:69 (:89) | `FK_SEC_ROLE_MODULE_GRANT_TENANT` V10:108 | `IDX_SEC_ROLE_MODULE_GRANT_TENANT` V10:127 | `UQ_SEC_ROLE_MODULE_GRANT_ROLE_MODULE (TENANT_ID, role_id, module_id)` V10:191 | REQ-TENANT-016, -020 |
| DBF-TENANT-018 | `SEC_ROLE_SCREEN_GRANT` (SEC, `RoleScreenGrant`) | NOT NULL | V10:70 (:90) | `FK_SEC_ROLE_SCREEN_GRANT_TENANT` V10:109 | `IDX_SEC_ROLE_SCREEN_GRANT_TENANT` V10:128 | `UQ_SEC_ROLE_SCREEN_GRANT_ROLE_SCREEN (TENANT_ID, role_id, screen_id)` V10:193 | REQ-TENANT-016, -020 |
| DBF-TENANT-019 | `SEC_ROLE_ACTION_GRANT` (SEC, `RoleActionGrant`) | NOT NULL | V10:71 (:91) | `FK_SEC_ROLE_ACTION_GRANT_TENANT` V10:110 | `IDX_SEC_ROLE_ACTION_GRANT_TENANT` V10:129 | `UQ_SEC_ROLE_ACTION_GRANT_ROLE_ACTION (TENANT_ID, role_id, action_id)` V10:195 | REQ-TENANT-016, -020 |
| DBF-TENANT-020 | `SEC_ACTIVE_SESSION` (SEC, `ActiveSession`) | NOT NULL | V10:72 (:92) | `FK_SEC_ACTIVE_SESSION_TENANT` V10:111 | `IDX_SEC_ACTIVE_SESSION_TENANT` V10:130 | — | REQ-TENANT-016 |
| DBF-TENANT-021 | `SEC_AUDIT_LOG` (SEC, `AuditLogEntry`) | NOT NULL | V10:73 (:93) | `FK_SEC_AUDIT_LOG_TENANT` V10:112 | `IDX_SEC_AUDIT_LOG_TENANT` V10:131 | — | REQ-TENANT-016 |
| DBF-TENANT-022 | `SEC_PWD_RESET_TOKEN` (SEC, `PasswordResetToken`) | NOT NULL | V10:74 (:94) | `FK_SEC_PWD_RESET_TOKEN_TENANT` V10:113 | `IDX_SEC_PWD_RESET_TOKEN_TENANT` V10:132 | — | REQ-TENANT-016 |
| DBF-TENANT-023 | `SEC_SIGNUP_REQUEST` (SEC, `SignupRequest`) | NOT NULL | V10:75 (:95) | `FK_SEC_SIGNUP_REQUEST_TENANT` V10:114 | `IDX_SEC_SIGNUP_REQUEST_TENANT` V10:133 | — | REQ-TENANT-016 |
| DBF-TENANT-024 | `FILE_CATEGORY` (FILE, `FileCategory`) | NOT NULL | V10:76 (:96) | `FK_FILE_CATEGORY_TENANT` V10:115 | `IDX_FILE_CATEGORY_TENANT` V10:134 | `UQ_FILE_CATEGORY_CATEGORY_CODE (TENANT_ID, CATEGORY_CODE)` V10:198 | REQ-TENANT-016 |
| DBF-TENANT-025 | `FILE_DOCUMENT` (FILE, `FileDocument`) | NOT NULL | V10:77 (:97) | `FK_FILE_DOCUMENT_TENANT` V10:116 | `IDX_FILE_DOCUMENT_TENANT` V10:135 | unique index `UQ_FILE_DOCUMENT_PUBLIC_SLUG (TENANT_ID, PUBLIC_SLUG) WHERE PUBLIC_SLUG IS NOT NULL` V12:43-44 | REQ-TENANT-011, -016 |
| DBF-TENANT-026 | `NOTIF_TEMPLATE` (NOTIF, `NotificationTemplate`) | NOT NULL | V10:78 (:98) | `FK_NOTIF_TEMPLATE_TENANT` V10:117 | `IDX_NOTIF_TEMPLATE_TENANT` V10:136 | `UQ_NOTIF_TEMPLATE_CODE (TENANT_ID, TEMPLATE_CODE)` V10:201 | REQ-TENANT-016, -020 |
| DBF-TENANT-027 | `NOTIF_CHANNEL_CONFIG` (NOTIF, `NotificationChannelConfig`) | NOT NULL | V10:79 (:99) | `FK_NOTIF_CHANNEL_CONFIG_TENANT` V10:118 | `IDX_NOTIF_CHANNEL_CONFIG_TENANT` V10:137 | `UQ_NOTIF_CHANNEL_CONFIG_TYPE (TENANT_ID, CHANNEL_TYPE_ID)` V10:203 | REQ-TENANT-016, -020 |
| DBF-TENANT-028 | `NOTIF_LOG` (NOTIF, `NotificationLog`) | NOT NULL | V10:80 (:100) | `FK_NOTIF_LOG_TENANT` V10:119 | `IDX_NOTIF_LOG_TENANT` V10:138 | — | REQ-TENANT-016 |
| DBF-TENANT-029 | `SEC_CUSTOMER_VERIFY_TOKEN` (SEC, `CustomerVerifyToken`) | NOT NULL | created with the table V11:68 | `FK_SEC_CUSTOMER_VERIFY_TOKEN_TENANT` V11:84 | `IDX_SEC_CUSTOMER_VERIFY_TOKEN_TENANT` V11:86 | `UQ_SEC_CUSTOMER_VERIFY_TOKEN_HASH (TENANT_ID, TOKEN_HASH)` V11:83 | REQ-TENANT-016 |
| DBF-TENANT-030 | `NOTIF_INBOX` (NOTIF, `NotificationInboxItem`) | NOT NULL | created with the table V13:40 | `FK_NOTIF_INBOX_TENANT` V13:61 | `IDX_NOTIF_INBOX_TENANT` V13:62 (+ `IDX_NOTIF_INBOX_RECIPIENT (TENANT_ID, RECIPIENT_USER_ID, READ_AT)` V13:63) | — | REQ-TENANT-016 |
| DBF-TENANT-031 | `CORE_NUMBER_SERIES` (sequence, `NumberSeries`) | NOT NULL | created with the table V14:33 | `FK_CORE_NUMBER_SERIES_TENANT` V14:58 | `IDX_CORE_NUMBER_SERIES_TENANT` V14:61 | `UQ_CORE_NUMBER_SERIES_CODE_PERIOD (TENANT_ID, CODE, PERIOD_KEY)` V14:57 | REQ-TENANT-016, -020 |
| DBF-TENANT-032 | `CORE_AUDIT_EVENT` (audit, `AuditEvent`) | NOT NULL | created with the table V15:18 | `FK_CORE_AUDIT_EVENT_TENANT` V15:40 | `IDX_CORE_AUDIT_EVENT_TENANT` V15:44 (+ `IDX_CORE_AUDIT_EVENT_ENTITY` :45, `IDX_CORE_AUDIT_EVENT_OCCURRED` :46, both leading with `TENANT_ID`) | — | REQ-TENANT-016, -022 |

Totals (checked against the code): 22 `TENANT_ID` columns (18 from V10 — V10:60 says "18 tables" — + 4
later), 22 FKs to `CORE_TENANT`, 22 `IDX_<TABLE>_TENANT` indexes; 21 NOT NULL without default + 1 nullable
(`CU_APP_CONFIGURATION`); 14 unique constraints containing `TENANT_ID` (V10's 13 − `UQ_CU_APP_CONFIG_CONFIG_KEY`
+ `UQ_SEC_CUSTOMER_VERIFY_TOKEN_HASH` + `UQ_CORE_NUMBER_SERIES_CODE_PERIOD`) plus 2 tenant-leading unique
indexes (`UQ_CU_APP_CONFIG_CONFIG_KEY`, `UQ_FILE_DOCUMENT_PUBLIC_SLUG`). The same numbers are asserted by
`erp-core/src/test/java/com/erp/tenant/TenantSchemaIntegrationTest.java:51` (22 columns), `:98` (14 unique
constraints) and `:113` (21 tenant-aware entities). Base tables without `TENANT_ID`: `CORE_TENANT`,
`SEC_MODULE_REG`, `SEC_SCREEN_REG`, `SEC_ACTION_REG` (and `flyway_schema_history`) — same test, `:28-29`.

Other tenant rows written by V10 (not DBF): the registry module `PLATFORM`, screen `PLATFORM_TENANTS`, the
actions `PERM_PLATFORM_TENANTS_VIEW` and `PLATFORM_TENANT_MANAGE` (V10:213-228), and three grant statements
producing four grant rows for the PLATFORM tenant's `SYS_ADMIN` — one module grant, one screen grant, two
action grants (V10:230-244) — SEC's global catalog tables and tenant-1 grant rows; since step 06
`TenantPermissions` re-declares the same catalog rows in code (tenant/permission/TenantPermissions.java:28-45).

## 2. XM REGISTER — TENANT v1

TENANT holds no FK to another module and no SOFT-READ of another module's table: nothing in the CONSUME
direction. The inbound HARD FKs from the 22 tables above are the DBF rows DBF-TENANT-011…032. The two
exposed surfaces carry XM ids at the plan's request (see `../P1/registry-srs-tenant.md` note):

| XM-ID | Type | Surface | Target / implementers | Physical object | Status |
|---|---|---|---|---|---|
| XM-TENANT-001 | crossmodule read (exposed) | `TenantLookupApi.codeOf(Long)` → `CORE_TENANT.CODE` (DBF-TENANT-002) by `ID` (DBF-TENANT-001) | consumers FILE, SEQUENCE | none (Java interface; read through `TenantRepository.findById`) | ACTIVE |
| XM-TENANT-002 | SPI (exposed) | `TenantProvisioningContributor.provision(TenantProvisioning)` | implementers SEC, MDL, NOTIF, SEQUENCE — write their own tables with explicit `TENANT_ID` (RULE-TENANT-008) | none | ACTIVE |

## 3. FULL_DATABASE_SCRIPT (as built — verbatim extract of `V10__tenant_schema.sql`)

```sql
-- V10 §1 (lines 29-57): CORE_TENANT
CREATE SEQUENCE SEQ_CORE_TENANT START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;

CREATE TABLE CORE_TENANT (
  ID           BIGINT        NOT NULL,
  CODE         VARCHAR(32)   NOT NULL,
  NAME_AR      VARCHAR(200)  NOT NULL,
  NAME_EN      VARCHAR(200)  NOT NULL,
  STATUS_CODE  VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
  CREATED_BY   VARCHAR(100)  NOT NULL,
  CREATED_AT   TIMESTAMPTZ   NOT NULL DEFAULT now(),
  UPDATED_BY   VARCHAR(100),
  UPDATED_AT   TIMESTAMPTZ,
  VERSION      BIGINT        NOT NULL DEFAULT 0
);

COMMENT ON TABLE CORE_TENANT IS 'Tenant registry (global — the one core table without TENANT_ID). ID 1 = PLATFORM.';
COMMENT ON COLUMN CORE_TENANT.CODE IS 'Tenant code, ^[A-Z0-9_]{3,32}$, unique; sent by clients as X-Tenant-Code.';
COMMENT ON COLUMN CORE_TENANT.STATUS_CODE IS 'ACTIVE | SUSPENDED — a suspended tenant cannot log in or call any endpoint.';

ALTER TABLE CORE_TENANT ADD CONSTRAINT PK_CORE_TENANT PRIMARY KEY (ID);
ALTER TABLE CORE_TENANT ADD CONSTRAINT UQ_CORE_TENANT_CODE UNIQUE (CODE);
ALTER TABLE CORE_TENANT ADD CONSTRAINT CHK_CORE_TENANT_STATUS CHECK (STATUS_CODE IN ('ACTIVE','SUSPENDED'));
ALTER TABLE CORE_TENANT ADD CONSTRAINT CHK_CORE_TENANT_CODE CHECK (CODE ~ '^[A-Z0-9_]{3,32}$');

INSERT INTO CORE_TENANT (ID, CODE, NAME_AR, NAME_EN, STATUS_CODE, CREATED_BY, CREATED_AT)
VALUES (1, 'PLATFORM', 'المنصة', 'Platform', 'ACTIVE', 'SYSTEM', CURRENT_TIMESTAMP);
SELECT setval('SEQ_CORE_TENANT', 1, true);

-- V10 §2 (lines 63-138): the discriminator, one statement of each kind per table (SEC_USER shown;
-- the other 17 V10 tables are identical apart from the names in §1 above)
ALTER TABLE SEC_USER              ADD COLUMN TENANT_ID BIGINT NOT NULL DEFAULT 1, ADD COLUMN VERSION BIGINT NOT NULL DEFAULT 0;
ALTER TABLE SEC_USER              ALTER COLUMN TENANT_ID DROP DEFAULT;
ALTER TABLE SEC_USER              ADD CONSTRAINT FK_SEC_USER_TENANT              FOREIGN KEY (TENANT_ID) REFERENCES CORE_TENANT (ID);
CREATE INDEX IDX_SEC_USER_TENANT              ON SEC_USER (TENANT_ID);
```
A table created after V10 declares `TENANT_ID BIGINT NOT NULL` in its `CREATE TABLE` and adds the FK and
index in the same script (V11:68/:84/:86, V13:40/:61/:62, V14:33/:58/:61, V15:18/:40/:44).

## 4. DECISIONS APPLIED

| DEFAULT / ADR | What | Source | Status |
|---|---|---|---|
| ADR-TENANT-001 | one shared schema with a `TENANT_ID` discriminator column, not a schema or database per tenant | governance/analysis/decisions/TENANT/ADR-TENANT-001.md | ACCEPTED (as built) |
| plan-sanctioned (step 05) | V10's two non-additive changes: `DROP DEFAULT` after the backfill; DROP + re-ADD of each unique constraint as `(TENANT_ID, …)` under the same name | V10:19-23; docs/DEVIATIONS.md [05] | as built |
| plan-sanctioned (step 09) | V14 makes `CU_APP_CONFIGURATION.TENANT_ID` nullable and replaces its unique constraint by an expression index | V14__sequence_and_settings.sql:10-17, :66-68; docs/DEVIATIONS.md [09] | as built |
| DEFAULT | PK column is `ID` (new-table convention), not `TENANT_PK` | docs/steps/05-report.md "Skills checked" (build-create-entity deviation) | as built |

## 5. REGISTRY CONTENT
See `registry-db-tenant.md`.

## 6. DBF id definitions (cross-reference index — full detail in §1; `[traces]` = ENT + REQ)
**DBF-TENANT-001** — CORE_TENANT.ID [ENT-TENANT-001, REQ-TENANT-001, REQ-TENANT-006]
**DBF-TENANT-002** — CORE_TENANT.CODE [ENT-TENANT-001, REQ-TENANT-001, REQ-TENANT-002, REQ-TENANT-003, REQ-TENANT-013]
**DBF-TENANT-003** — CORE_TENANT.NAME_AR [ENT-TENANT-001, REQ-TENANT-001]
**DBF-TENANT-004** — CORE_TENANT.NAME_EN [ENT-TENANT-001, REQ-TENANT-001]
**DBF-TENANT-005** — CORE_TENANT.STATUS_CODE [ENT-TENANT-001, REQ-TENANT-008, REQ-TENANT-009, REQ-TENANT-010]
**DBF-TENANT-006** — CORE_TENANT.CREATED_BY [ENT-TENANT-001, REQ-TENANT-001]
**DBF-TENANT-007** — CORE_TENANT.CREATED_AT [ENT-TENANT-001, REQ-TENANT-001]
**DBF-TENANT-008** — CORE_TENANT.UPDATED_BY [ENT-TENANT-001, REQ-TENANT-008]
**DBF-TENANT-009** — CORE_TENANT.UPDATED_AT [ENT-TENANT-001, REQ-TENANT-008]
**DBF-TENANT-010** — CORE_TENANT.VERSION [ENT-TENANT-001, REQ-TENANT-008]
**DBF-TENANT-011** — CU_APP_CONFIGURATION.TENANT_ID (nullable) [ENT-TENANT-001, REQ-TENANT-016]
**DBF-TENANT-012** — MDL_LOOKUP_TYPE.TENANT_ID [ENT-TENANT-001, REQ-TENANT-016, REQ-TENANT-020]
**DBF-TENANT-013** — MDL_LOOKUP_VALUE.TENANT_ID [ENT-TENANT-001, REQ-TENANT-016, REQ-TENANT-020]
**DBF-TENANT-014** — SEC_USER.TENANT_ID [ENT-TENANT-001, REQ-TENANT-012, REQ-TENANT-013, REQ-TENANT-016, REQ-TENANT-020]
**DBF-TENANT-015** — SEC_ROLE.TENANT_ID [ENT-TENANT-001, REQ-TENANT-016, REQ-TENANT-020]
**DBF-TENANT-016** — SEC_USER_ROLE.TENANT_ID [ENT-TENANT-001, REQ-TENANT-016, REQ-TENANT-020]
**DBF-TENANT-017** — SEC_ROLE_MODULE_GRANT.TENANT_ID [ENT-TENANT-001, REQ-TENANT-016, REQ-TENANT-020]
**DBF-TENANT-018** — SEC_ROLE_SCREEN_GRANT.TENANT_ID [ENT-TENANT-001, REQ-TENANT-016, REQ-TENANT-020]
**DBF-TENANT-019** — SEC_ROLE_ACTION_GRANT.TENANT_ID [ENT-TENANT-001, REQ-TENANT-016, REQ-TENANT-020]
**DBF-TENANT-020** — SEC_ACTIVE_SESSION.TENANT_ID [ENT-TENANT-001, REQ-TENANT-016]
**DBF-TENANT-021** — SEC_AUDIT_LOG.TENANT_ID [ENT-TENANT-001, REQ-TENANT-016]
**DBF-TENANT-022** — SEC_PWD_RESET_TOKEN.TENANT_ID [ENT-TENANT-001, REQ-TENANT-016]
**DBF-TENANT-023** — SEC_SIGNUP_REQUEST.TENANT_ID [ENT-TENANT-001, REQ-TENANT-016]
**DBF-TENANT-024** — FILE_CATEGORY.TENANT_ID [ENT-TENANT-001, REQ-TENANT-016]
**DBF-TENANT-025** — FILE_DOCUMENT.TENANT_ID [ENT-TENANT-001, REQ-TENANT-011, REQ-TENANT-016]
**DBF-TENANT-026** — NOTIF_TEMPLATE.TENANT_ID [ENT-TENANT-001, REQ-TENANT-016, REQ-TENANT-020]
**DBF-TENANT-027** — NOTIF_CHANNEL_CONFIG.TENANT_ID [ENT-TENANT-001, REQ-TENANT-016, REQ-TENANT-020]
**DBF-TENANT-028** — NOTIF_LOG.TENANT_ID [ENT-TENANT-001, REQ-TENANT-016]
**DBF-TENANT-029** — SEC_CUSTOMER_VERIFY_TOKEN.TENANT_ID [ENT-TENANT-001, REQ-TENANT-016]
**DBF-TENANT-030** — NOTIF_INBOX.TENANT_ID [ENT-TENANT-001, REQ-TENANT-016]
**DBF-TENANT-031** — CORE_NUMBER_SERIES.TENANT_ID [ENT-TENANT-001, REQ-TENANT-016, REQ-TENANT-020]
**DBF-TENANT-032** — CORE_AUDIT_EVENT.TENANT_ID [ENT-TENANT-001, REQ-TENANT-016, REQ-TENANT-022]
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package B — tenant profile and lifecycle facts on `CORE_TENANT` (plan §4 B.1)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Migrations (written from this entry): `erp-core/src/main/resources/db/migration/core/V18__tenant_profile.sql`
and `V19__tenant_lifecycle.sql`. The plan expected `V16__tenant_profile.sql` / `V17__tenant_lifecycle.sql`;
package D, executed first, took V16 / V17, so the numbers follow the execution order (plan §1.3 / §11,
`docs/DEVIATIONS.md` `[TM-B]`). Additive only (`MigrationNamingTest`): ten nullable columns without a default
and one CHECK every existing row satisfies (NULL); `CODE`, `STATUS_CODE` and the 1.2.0 constraints are
untouched. DBF ids continue from DBF-TENANT-032.

### Table CORE_TENANT (ENT-TENANT-001) — NEW columns
| DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default | Constraint | Migration |
|---|---|---|---|---|---|---|---|---|
| DBF-TENANT-033 | CONTACT_EMAIL | VARCHAR(255) | ENT-TENANT-001.contactEmail | REQ-TENANT-025 | NULL | — | — | V18 |
| DBF-TENANT-034 | CONTACT_PHONE | VARCHAR(30) | ENT-TENANT-001.contactPhone | REQ-TENANT-025 | NULL | — | — | V18 |
| DBF-TENANT-035 | COUNTRY_CODE | VARCHAR(2) | ENT-TENANT-001.countryCode | REQ-TENANT-025 | NULL | — | — | V18 |
| DBF-TENANT-036 | DEFAULT_LOCALE | VARCHAR(5) | ENT-TENANT-001.defaultLocale | REQ-TENANT-025 | NULL | — | `CHK_CORE_TENANT_LOCALE` | V18 |
| DBF-TENANT-037 | TIMEZONE | VARCHAR(64) | ENT-TENANT-001.timezone | REQ-TENANT-025 | NULL | — | — | V18 |
| DBF-TENANT-038 | NOTES | VARCHAR(1000) | ENT-TENANT-001.notes | REQ-TENANT-025 | NULL | — | — | V18 |
| DBF-TENANT-039 | SUSPENDED_AT | TIMESTAMPTZ | ENT-TENANT-001.suspendedAt | REQ-TENANT-026 | NULL | — | — | V19 |
| DBF-TENANT-040 | SUSPENDED_BY | VARCHAR(100) | ENT-TENANT-001.suspendedBy | REQ-TENANT-026 | NULL | — | — | V19 |
| DBF-TENANT-041 | SUSPENSION_REASON | VARCHAR(500) | ENT-TENANT-001.suspensionReason | REQ-TENANT-026 | NULL | — | — | V19 |
| DBF-TENANT-042 | TOKENS_INVALID_BEFORE | TIMESTAMPTZ | ENT-TENANT-001.tokensInvalidBefore | REQ-TENANT-026 (written on activation; enforced by package C.2) | NULL | — | — | V19 |

### Constraints
| Name | Definition | Note |
|---|---|---|
| `CHK_CORE_TENANT_LOCALE` | `CHECK (DEFAULT_LOCALE IS NULL OR DEFAULT_LOCALE IN ('ar', 'en'))` | the `SEC_USER.PREFERRED_LOCALE` pattern (`CHK_SEC_USER_LOCALE`, V16); every existing row has NULL |
No index (the new search fields are filters over a table of a few hundred rows at most; `CORE_TENANT` has
none besides the PK and `UQ_CORE_TENANT_CODE`), no sequence, no FK.

### Script (`V18__tenant_profile.sql`)
```sql
ALTER TABLE CORE_TENANT ADD COLUMN CONTACT_EMAIL  VARCHAR(255);
ALTER TABLE CORE_TENANT ADD COLUMN CONTACT_PHONE  VARCHAR(30);
ALTER TABLE CORE_TENANT ADD COLUMN COUNTRY_CODE   VARCHAR(2);
ALTER TABLE CORE_TENANT ADD COLUMN DEFAULT_LOCALE VARCHAR(5);
ALTER TABLE CORE_TENANT ADD COLUMN TIMEZONE       VARCHAR(64);
ALTER TABLE CORE_TENANT ADD COLUMN NOTES          VARCHAR(1000);

ALTER TABLE CORE_TENANT ADD CONSTRAINT CHK_CORE_TENANT_LOCALE
    CHECK (DEFAULT_LOCALE IS NULL OR DEFAULT_LOCALE IN ('ar', 'en'));
```

### Script (`V19__tenant_lifecycle.sql`)
```sql
ALTER TABLE CORE_TENANT ADD COLUMN SUSPENDED_AT          TIMESTAMPTZ;
ALTER TABLE CORE_TENANT ADD COLUMN SUSPENDED_BY          VARCHAR(100);
ALTER TABLE CORE_TENANT ADD COLUMN SUSPENSION_REASON     VARCHAR(500);
ALTER TABLE CORE_TENANT ADD COLUMN TOKENS_INVALID_BEFORE TIMESTAMPTZ;
```
plus one `COMMENT ON COLUMN` per new column in each script. Existing rows (PLATFORM and every provisioned
tenant) get NULL everywhere: a tenant suspended before the upgrade keeps `SUSPENDED` without facts until it is
re-activated or suspended again.

### CHECK-constrained value sets — delta
| Key | Values | Constraint | Owner |
|---|---|---|---|
| `CORE_TENANT.DEFAULT_LOCALE` | `ar`, `en` (NULL allowed) | `CHK_CORE_TENANT_LOCALE` | TENANT |
| `CORE_TENANT.STATUS_CODE` | unchanged: `ACTIVE`, `SUSPENDED` | `CHK_CORE_TENANT_STATUS` | TENANT |

### DBF id definitions — delta
**DBF-TENANT-033** — CORE_TENANT.CONTACT_EMAIL [ENT-TENANT-001, REQ-TENANT-025]
**DBF-TENANT-034** — CORE_TENANT.CONTACT_PHONE [ENT-TENANT-001, REQ-TENANT-025]
**DBF-TENANT-035** — CORE_TENANT.COUNTRY_CODE [ENT-TENANT-001, REQ-TENANT-025]
**DBF-TENANT-036** — CORE_TENANT.DEFAULT_LOCALE [ENT-TENANT-001, REQ-TENANT-025]
**DBF-TENANT-037** — CORE_TENANT.TIMEZONE [ENT-TENANT-001, REQ-TENANT-025]
**DBF-TENANT-038** — CORE_TENANT.NOTES [ENT-TENANT-001, REQ-TENANT-025]
**DBF-TENANT-039** — CORE_TENANT.SUSPENDED_AT [ENT-TENANT-001, REQ-TENANT-026]
**DBF-TENANT-040** — CORE_TENANT.SUSPENDED_BY [ENT-TENANT-001, REQ-TENANT-026]
**DBF-TENANT-041** — CORE_TENANT.SUSPENSION_REASON [ENT-TENANT-001, REQ-TENANT-026]
**DBF-TENANT-042** — CORE_TENANT.TOKENS_INVALID_BEFORE [ENT-TENANT-001, REQ-TENANT-026]

### Deviations
- Plan §4 B.1 / §11 `V16__tenant_profile.sql`, `V17__tenant_lifecycle.sql` → `V18__tenant_profile.sql`,
  `V19__tenant_lifecycle.sql` (execution order D before B).

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package E — tenant branding columns on `CORE_TENANT` (plan §7 E.1)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Migration (written from this entry): `erp-core/src/main/resources/db/migration/core/V20__tenant_branding.sql`.
The plan expected `V18__tenant_branding.sql`; packages D and B, executed first, took V16 … V19, so the number
follows the execution order (plan §1.3 / §11, `docs/DEVIATIONS.md` `[TM-E]`). Additive only
(`MigrationNamingTest`): two nullable columns without a default and one CHECK every existing row satisfies
(NULL). **No registry rows** (plan §0 D5, ADR-TENANT-005): no module, screen, action or grant seed. DBF ids
continue from DBF-TENANT-042.

### Table CORE_TENANT (ENT-TENANT-001) — NEW columns
| DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default | Constraint | Migration |
|---|---|---|---|---|---|---|---|---|
| DBF-TENANT-043 | LOGO_FILE_ID | BIGINT | ENT-TENANT-001.logoFileId | REQ-TENANT-029 | NULL | — | soft reference to `FILE_DOCUMENT.ID`, **no FK** (XM-TENANT-003; the `NOTIF_TEMPLATE.ATTACHMENT_FILE_ID` / `SEC_USER.PHOTO_FILE_ID` convention: the document lives in the tenant's own `FILE_DOCUMENT` rows and is discarded, never deleted) | V20 |
| DBF-TENANT-044 | BRAND_COLOR | VARCHAR(7) | ENT-TENANT-001.brandColor | REQ-TENANT-030 | NULL | — | `CHK_CORE_TENANT_BRAND_COLOR` | V20 |

### Constraints
| Name | Definition | Note |
|---|---|---|
| `CHK_CORE_TENANT_BRAND_COLOR` | `CHECK (BRAND_COLOR ~ '^#[0-9A-Fa-f]{6}$')` | the plan's expression verbatim; a NULL value passes (a CHECK fails only on FALSE); every existing row has NULL |
No index (the columns are read with the row, never searched), no sequence, no FK.

### Script (`V20__tenant_branding.sql`)
```sql
ALTER TABLE CORE_TENANT ADD COLUMN LOGO_FILE_ID BIGINT;
ALTER TABLE CORE_TENANT ADD COLUMN BRAND_COLOR  VARCHAR(7);

ALTER TABLE CORE_TENANT ADD CONSTRAINT CHK_CORE_TENANT_BRAND_COLOR
    CHECK (BRAND_COLOR ~ '^#[0-9A-Fa-f]{6}$');
```
plus one `COMMENT ON COLUMN` per new column. Existing rows (PLATFORM and every provisioned tenant) get NULL: no
logo, no brand colour.

The logo document itself is a `FILE_DOCUMENT` row **in the target tenant's rows** (`TENANT_ID = {id}`,
`OWNER_TYPE = CORE_TENANT`, `OWNER_ID = {id}`, `MODULE_CODE = TENANT`, `FILE_NAME = logo.<png|jpg|webp|svg>`,
`VISIBILITY = PUBLIC`, no category), written inside `TenantContext.callAs(id)` through FILE's image store — no
schema change in FILE (FILE RULE-FILE-010, ADR-FILE-008).

### CHECK-constrained value sets — delta
| Key | Values | Constraint | Owner |
|---|---|---|---|
| `CORE_TENANT.BRAND_COLOR` | `#RRGGBB` (hexadecimal; stored upper-case by the entity), NULL allowed | `CHK_CORE_TENANT_BRAND_COLOR` | TENANT |

### XM register — delta
| Kind | XM id | Kind | Column → target | Owner of the target | Enforcement | Status |
|---|---|---|---|---|---|---|
| NEW | XM-TENANT-003 | SOFT-REF (consumed) | `CORE_TENANT.LOGO_FILE_ID` → `FILE_DOCUMENT.ID` | FILE (`FileImageStoreApi`, `FileDocumentLookupApi.publicUrl`) | column only, no FK; written and read inside `TenantContext.callAs(id)` | IMPLEMENTED (1.3.0) |

### DBF id definitions — delta
**DBF-TENANT-043** — CORE_TENANT.LOGO_FILE_ID [ENT-TENANT-001, REQ-TENANT-029, XM-TENANT-003]
**DBF-TENANT-044** — CORE_TENANT.BRAND_COLOR [ENT-TENANT-001, REQ-TENANT-030]

### Decisions
| Kind | Decision | Source |
|---|---|---|
| ADR | No registry rows for branding (D5) | ADR-TENANT-005 |
| DEFAULT | `LOGO_FILE_ID` is a soft reference without FK | plan §6 D.1 / §7 E.1 (same convention as `NOTIF_TEMPLATE.ATTACHMENT_FILE_ID`, XM-NOTIF-002, and `SEC_USER.PHOTO_FILE_ID`, XM-SEC-006) |

### Deviations
- Plan §7 E.1 / §11 `V18__tenant_branding.sql` → `V20__tenant_branding.sql` (execution order D, B before E).

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package C12 — the token cut-off `CORE_TENANT.TOKENS_INVALID_BEFORE` enforced; `TenantLookupApi.isActive` (plan §5 C.1, C.2)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

**No migration, no new column, constraint, index or sequence.** The column exists since
`V19__tenant_lifecycle.sql` (package B, DBF-TENANT-042; the plan's §11 row `V17__tenant_lifecycle.sql` "B / C.2" was
written by B). DBF ids are unchanged (last DBF-TENANT-044).

### Columns — CHANGED use
| DBF id | Column | Delta | Writers | Reader |
|---|---|---|---|---|
| DBF-TENANT-042 | CORE_TENANT.TOKENS_INVALID_BEFORE (TIMESTAMPTZ, NULL) | now **enforced**: a token whose `iat` (whole seconds) is less than this instant truncated to the second is refused (RULE-TENANT-023, ADR-TENANT-002); still never exposed | SUSPENDED → ACTIVE (`Tenant.activate`: the application's `Instant.now()`), `POST /{id}/revoke-tokens` (`Tenant.revokeTokens`: the start of the next whole second, `TenantDomain.revocationCutOff`, review round 1) — both in a PLATFORM transaction | `TenantResolutionFilter` (as PLATFORM, every request with a token) |
| DBF-TENANT-005 | CORE_TENANT.STATUS_CODE | + read by `TenantLookupApi.isActive` (XM-TENANT-001, NOTIF) | — | NOTIF claim / requeue |

### XM register — delta
| Kind | XM id | Kind | Column → target | Owner of the target | Enforcement | Status |
|---|---|---|---|---|---|---|
| CHANGED | XM-TENANT-001 | crossmodule read (exposed) | + `TenantLookupApi.isActive(Long)` → `CORE_TENANT.STATUS_CODE` (DBF-TENANT-005) by `ID` | consumer NOTIF (claim, requeue) | none (Java interface; `TenantRepository.findById`, uncached) | IMPLEMENTED (1.3.0) |

### Decisions
| Kind | Decision | Source |
|---|---|---|
| ADR | `TOKENS_INVALID_BEFORE` on `CORE_TENANT` instead of a token denylist table | ADR-TENANT-002 (ACCEPTED) |

Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package C4 — the idempotency table `CORE_IDEMPOTENCY_KEY` (owned by `com.erp.common.idempotency`; first consumer tenant create) (plan §5 C.4)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Migration (written from this entry): `erp-core/src/main/resources/db/migration/core/V21__core_idempotency_key.sql`.
The plan expected `V20__core_idempotency_key.sql`; packages D, B and E, executed first, took V16 … V20, so the
number follows the execution order (plan §1.3 / §11, `docs/DEVIATIONS.md` `[TM-C4]`). Additive only
(`MigrationNamingTest`): one new table, its sequence, constraints and indexes; no seed. The table is common's, not
TENANT's (no ENT-TENANT id); it is registered here because tenant create is its first consumer and this repository
has no COMMON analysis folder. DBF ids continue from DBF-TENANT-044; only its `TENANT_ID` carries one (the
discriminator register).

**AuditableEntity convention (the reference analysis' open point, decided here).** The plan lists nine columns. The
entity is tenant-scoped, so it extends `AuditableEntity` (`TenantScopedEntityTest`, RULE-TENANT-010 — no new global
entity) and the table carries the convention's audit columns (`db/migration/core/README.md` "Tenant columns"):
`CREATED_BY`, `UPDATED_BY`, `UPDATED_AT` are added to the plan's list; `CREATED_AT` and `VERSION` were already in it.
`CREATED_BY` is NOT NULL because it identifies the owner of the key (RULE-TENANT-026); the entity listener always
fills it. **Final column list (12):** `ID`, `TENANT_ID`, `IDEMPOTENCY_KEY`, `ENDPOINT`, `REQUEST_HASH`,
`RESPONSE_STATUS`, `RESPONSE_BODY`, `CREATED_BY`, `CREATED_AT`, `UPDATED_BY`, `UPDATED_AT`, `VERSION`.

### Table CORE_IDEMPOTENCY_KEY — NEW (entity `com.erp.common.idempotency.IdempotencyKey`, extends `AuditableEntity`)
| DBF id | Column | Type (postgresql16) | Entity field | Nullable | Default | Constraint / index | Migration |
|---|---|---|---|---|---|---|---|
| — (common) | ID | BIGINT | id | NOT NULL | `SEQ_CORE_IDEMPOTENCY_KEY` (`@SequenceGenerator`, allocationSize 1) | `PK_CORE_IDEMPOTENCY_KEY` | V21 |
| DBF-TENANT-045 | TENANT_ID | BIGINT | tenantId (`AuditableEntity`, `@TenantId`) | NOT NULL | — (no default) | `FK_CORE_IDEMPOTENCY_KEY_TENANT` → `CORE_TENANT (ID)`; `IDX_CORE_IDEMPOTENCY_KEY_TENANT`; leads `UQ_CORE_IDEMPOTENCY_KEY` | V21 |
| — (common) | IDEMPOTENCY_KEY | VARCHAR(64) | idempotencyKey | NOT NULL | — | part of `UQ_CORE_IDEMPOTENCY_KEY`; value `^[A-Za-z0-9._:-]{1,64}$` (RULE-TENANT-025, checked in code, no CHECK) | V21 |
| — (common) | ENDPOINT | VARCHAR(200) | endpoint | NOT NULL | — | part of `UQ_CORE_IDEMPOTENCY_KEY`; the consumer's constant id, e.g. `POST /api/v1/platform/tenants` | V21 |
| — (common) | REQUEST_HASH | VARCHAR(64) | requestHash | NOT NULL | — | lower-case hex HMAC-SHA256 of the canonical request body (RULE-TENANT-026) | V21 |
| — (common) | RESPONSE_STATUS | INT | responseStatus | NOT NULL | — | the stored HTTP status (2xx once committed; `0` only while the claim's transaction is open, never committed) | V21 |
| — (common) | RESPONSE_BODY | TEXT | responseBody | NULL | — | the JSON envelope as answered | V21 |
| — (common) | CREATED_BY | VARCHAR(100) | createdBy (`GlobalAuditableEntity`) | NOT NULL | — | the key's owner (RULE-TENANT-026) | V21 |
| — (common) | CREATED_AT | TIMESTAMPTZ | createdAt | NOT NULL | `now()` | `IDX_CORE_IDEMPOTENCY_KEY_CREATED_AT` (retention scan) | V21 |
| — (common) | UPDATED_BY | VARCHAR(100) | updatedBy | NULL | — | — | V21 |
| — (common) | UPDATED_AT | TIMESTAMPTZ | updatedAt | NULL | — | — | V21 |
| — (common) | VERSION | BIGINT | version (`@Version`) | NOT NULL | 0 | — | V21 |

### Constraints, indexes, sequence
| Name | Definition | Note |
|---|---|---|
| `PK_CORE_IDEMPOTENCY_KEY` | `PRIMARY KEY (ID)` | |
| `FK_CORE_IDEMPOTENCY_KEY_TENANT` | `FOREIGN KEY (TENANT_ID) REFERENCES CORE_TENANT (ID)` | `FK_<TABLE>_TENANT` convention |
| `UQ_CORE_IDEMPOTENCY_KEY` | `UNIQUE (TENANT_ID, IDEMPOTENCY_KEY, ENDPOINT)` | the plan's name; tenant-leading; serialises same-key requests (RULE-TENANT-026) |
| `IDX_CORE_IDEMPOTENCY_KEY_TENANT` | `(TENANT_ID)` | `IDX_<TABLE>_TENANT` convention |
| `IDX_CORE_IDEMPOTENCY_KEY_CREATED_AT` | `(CREATED_AT)` | the plan's "index on `CREATED_AT`"; `IDX_<TABLE>_<COLUMN>` |
| `SEQ_CORE_IDEMPOTENCY_KEY` | `START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE` | |
No CHECK constraint (the key format and the 2xx status are decided in code; a CHECK on the status would refuse the
claim's in-transaction placeholder). No seed row.

### Script (`V21__core_idempotency_key.sql`)
```sql
CREATE SEQUENCE SEQ_CORE_IDEMPOTENCY_KEY START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;

CREATE TABLE CORE_IDEMPOTENCY_KEY (
  ID               BIGINT         NOT NULL,
  TENANT_ID        BIGINT         NOT NULL,
  IDEMPOTENCY_KEY  VARCHAR(64)    NOT NULL,
  ENDPOINT         VARCHAR(200)   NOT NULL,
  REQUEST_HASH     VARCHAR(64)    NOT NULL,
  RESPONSE_STATUS  INT            NOT NULL,
  RESPONSE_BODY    TEXT,
  CREATED_BY       VARCHAR(100)   NOT NULL,
  CREATED_AT       TIMESTAMPTZ    NOT NULL DEFAULT now(),
  UPDATED_BY       VARCHAR(100),
  UPDATED_AT       TIMESTAMPTZ,
  VERSION          BIGINT         NOT NULL DEFAULT 0
);

ALTER TABLE CORE_IDEMPOTENCY_KEY ADD CONSTRAINT PK_CORE_IDEMPOTENCY_KEY PRIMARY KEY (ID);
ALTER TABLE CORE_IDEMPOTENCY_KEY ADD CONSTRAINT FK_CORE_IDEMPOTENCY_KEY_TENANT FOREIGN KEY (TENANT_ID) REFERENCES CORE_TENANT (ID);
ALTER TABLE CORE_IDEMPOTENCY_KEY ADD CONSTRAINT UQ_CORE_IDEMPOTENCY_KEY UNIQUE (TENANT_ID, IDEMPOTENCY_KEY, ENDPOINT);

CREATE INDEX IDX_CORE_IDEMPOTENCY_KEY_TENANT     ON CORE_IDEMPOTENCY_KEY (TENANT_ID);
CREATE INDEX IDX_CORE_IDEMPOTENCY_KEY_CREATED_AT ON CORE_IDEMPOTENCY_KEY (CREATED_AT);
```
plus `COMMENT ON TABLE` and one `COMMENT ON COLUMN` for `IDEMPOTENCY_KEY`, `ENDPOINT`, `REQUEST_HASH`,
`RESPONSE_STATUS`, `RESPONSE_BODY`.

### TENANT_ID discriminator register — delta
| DBF id | Table (owner module, entity) | Nullable | Column added | FK | Index | Tenant-leading uniques | Traces (REQ) |
|---|---|---|---|---|---|---|---|
| DBF-TENANT-045 | `CORE_IDEMPOTENCY_KEY` (common, `IdempotencyKey`) | NOT NULL | created with the table V21 | `FK_CORE_IDEMPOTENCY_KEY_TENANT` V21 | `IDX_CORE_IDEMPOTENCY_KEY_TENANT` V21 | `UQ_CORE_IDEMPOTENCY_KEY (TENANT_ID, IDEMPOTENCY_KEY, ENDPOINT)` V21 | REQ-TENANT-016, -036 |
Totals after V21: **23** `TENANT_ID` columns / FKs / `IDX_<TABLE>_TENANT` indexes (22 NOT NULL without default + 1
nullable), **15** unique constraints containing `TENANT_ID` (+ `UQ_CORE_IDEMPOTENCY_KEY`) plus the 2 tenant-leading
unique indexes, **22** tenant-aware entities (+ `IdempotencyKey`). `TenantSchemaIntegrationTest` asserts these
numbers (22 → 23, 14 → 15, 21 → 22).

### Retention (no schema object)
Rows older than `erp.core.idempotency.retention` (24 h) are ignored at lookup and deleted by
`IdempotencyKeyRetentionJob.run()`, tenant by tenant (`SELECT DISTINCT TENANT_ID FROM CORE_IDEMPOTENCY_KEY WHERE
CREATED_AT < ?`, then `DELETE FROM CORE_IDEMPOTENCY_KEY WHERE TENANT_ID = ? AND CREATED_AT < ?` — RULE-TENANT-011);
its trigger `erp.core.idempotency.retention-cron` (default `-`) fires only when the application enables scheduling.

### DBF id definitions — delta
**DBF-TENANT-045** — CORE_IDEMPOTENCY_KEY.TENANT_ID [ENT-TENANT-001 (FK target), REQ-TENANT-016, REQ-TENANT-036]

### Decisions
| Kind | Decision | Source |
|---|---|---|
| ADR | Idempotency keys in a core table behind the common mechanism, 24 h retention, first consumer tenant create | ADR-TENANT-003 |
| DEFAULT | Audit columns per the tenant-scoped convention (entity extends `AuditableEntity`) | `db/migration/core/README.md`; RULE-TENANT-010 |

### Deviations
- Plan §5 C.4 / §11 `V20__core_idempotency_key.sql` → `V21__core_idempotency_key.sql` (execution order D, B, E before C4).
- Plan §5 C.4's nine columns → twelve (+ `CREATED_BY` NOT NULL, `UPDATED_BY`, `UPDATED_AT`; `AuditableEntity`).
