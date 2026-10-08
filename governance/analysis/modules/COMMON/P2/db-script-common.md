# DATABASE — الأساس المشترك / Common foundation (COMMON)
══════════════════════════════════════════════════════════════════
Module : COMMON   Version : v1 (as-built baseline, erp-core 1.2.0)   Dialect : postgresql16   Schema prefix : none
Identifier transformation : physical names are UPPER_SNAKE_CASE as created by the migrations; the mapped
  superclass fields map 1:1 (`createdBy` → `CREATED_BY`, `tenantId` → `TENANT_ID`, `version` → `VERSION`).
Date : 2026-10-07
Counts : 0 tables of its own · 0 sequences · 0 DBF · 0 XM · 3 column conventions every table follows (audit, version, tenant)
══════════════════════════════════════════════════════════════════

**No table of its own.** `com.erp.common` owns no migration, table, sequence or JPA entity. What it
owns physically is the shape of six columns that every core table carries through the two mapped
superclasses (`GlobalAuditableEntity`, `AuditableEntity`): the four audit columns, `VERSION` and
`TENANT_ID`. Their rows are registered by the owning module of each table (SEC, MDL, CU, FILE, NOTIF
P2 addenda; TENANT's `DBF-TENANT-011…032` for the 22 `TENANT_ID` columns); this file records the
convention, as read from the migrations and the entity code. Java paths are relative to
`erp-core/src/main/java/com/erp/`, migrations to `erp-core/src/main/resources/db/migration/core/`,
`file:line` at main @ 19b19a4.

## 1. DB FIELD TRACEABILITY MATRIX — COMMON v1

No DBF id of its own. The columns below are owned by the table's module; the JPA mapping is COMMON's.

### 1a. Audit columns (ENT-COMMON-001 `GlobalAuditableEntity`, every table)
| Column | JPA mapping (common) | Physical type by table family | Nullable | Default | Source |
|---|---|---|---|---|---|
| `CREATED_BY` | `@Column(name = "CREATED_BY", updatable = false, length = 100)` — filled by `AuditEntityListener.prePersist` with the current username (`system` without caller) | `VARCHAR(100)` on SEC, MDL and `CORE_*` tables (`CORE_TENANT`, `CORE_NUMBER_SERIES`, `CORE_AUDIT_EVENT`); `VARCHAR(255)` on CU, FILE, NOTIF tables (6 tables, intentionally wider). The JPA length 100 is the narrowest live width and the true upper bound (the value is the SEC username, capped at 100 by `SEC_USER`); do not widen it | NOT NULL on the original SEC/MDL tables and `CORE_TENANT`; NULL on CU/FILE/NOTIF, the 8 SEC tables that received audit columns in V10, `CORE_NUMBER_SERIES`, `CORE_AUDIT_EVENT`, `NOTIF_INBOX` | — | common/domain/GlobalAuditableEntity.java:30-35; common/audit/AuditEntityListener.java:12-20; V2__cu_schema.sql:26; V3__mdl_schema.sql:31; V4__sec_schema.sql:49, :75; V5__file_schema.sql:26, :47; V6__notif_schema.sql:31, :42, :63; V10__tenant_schema.sql:37, :153-160; V13__notif_async_inbox.sql:49; V14__sequence_and_settings.sql:41; V15__audit_schema.sql:32 |
| `CREATED_AT` | `@Column(name = "CREATED_AT", updatable = false)` `Instant`, set on persist | `TIMESTAMPTZ` on SEC, MDL, `CORE_TENANT`, `CORE_NUMBER_SERIES` (+ the V10 SEC additions); `TIMESTAMP` (UTC wall-clock, no zone) on CU, FILE, NOTIF, `NOTIF_INBOX`, `CORE_AUDIT_EVENT` | NOT NULL `DEFAULT now()` on SEC/MDL/`CORE_TENANT`/`CORE_NUMBER_SERIES`; NULL elsewhere | `now()` where NOT NULL | common/domain/GlobalAuditableEntity.java:37-38; V2:27; V3:32; V4:50; V5:27; V6:32; V10:38, :153-160; V13:50; V14:42; V15:33 |
| `UPDATED_BY` | `@Column(name = "UPDATED_BY", length = 100)`, set on persist and on every update | as `CREATED_BY` (100 / 255) | NULL everywhere | — | common/domain/GlobalAuditableEntity.java:40-41; common/audit/AuditEntityListener.java:22-26; V2:28; V3:33; V4:51; V10:39 |
| `UPDATED_AT` | `@Column(name = "UPDATED_AT")` `Instant`, set on persist and on every update | as `CREATED_AT` (TIMESTAMPTZ / TIMESTAMP) | NULL everywhere | — | common/domain/GlobalAuditableEntity.java:43-44; V2:29; V3:34; V4:52; V10:40 |
The two width / type families are a pre-erp-core difference kept as recorded (renames and type changes
are not additive — docs/DEVIATIONS.md [05] "Kept as recorded: … the CU/FILE/NOTIF vs SEC/MDL audit-column
type differences"); new `CORE_*` tables follow SEC/MDL for width (100) and, for `CORE_AUDIT_EVENT`,
`TIMESTAMP` (docs/DEVIATIONS.md [10] "timestamps are bound as UTC wall-clock like Hibernate's `Instant` binding").

### 1b. Optimistic lock (ENT-COMMON-001, every table)
| Column | JPA mapping (common) | Physical type | Nullable | Default | Source |
|---|---|---|---|---|---|
| `VERSION` | `@Version @Column(name = "VERSION", nullable = false)` `Long`; 0 on insert, +1 per update by Hibernate; never set by hand; a stale update → 409 `CONCURRENT_MODIFICATION` | `BIGINT` | NOT NULL | 0 | common/domain/GlobalAuditableEntity.java:46-53; V10__tenant_schema.sql:8, :63-80 (18 tables), :145-147 (the three registries), :41 (`CORE_TENANT`); V11 (`SEC_CUSTOMER_VERIFY_TOKEN`), V13:53, V14:45, V15:36 |
Every core table carries it: the 22 tenant-scoped tables, `CORE_TENANT` and the three global registries
(`erp-core/src/main/resources/db/migration/core/README.md` "Tenant columns (since V10)").

### 1c. Tenant discriminator (ENT-COMMON-002 `AuditableEntity`, every tenant-scoped table)
| Column | JPA mapping (common) | Physical type | Nullable | Default | Constraints (owned by the table's module) | Source |
|---|---|---|---|---|---|---|
| `TENANT_ID` | `@TenantId @Column(name = "TENANT_ID", nullable = false, updatable = false)` `Long` — filled by Hibernate from the session tenant on insert, added to every query / join / load by id; never set by application code | `BIGINT` | NOT NULL (V10 added it `DEFAULT 1`, backfilled, then dropped the default) | none | `FK_<TABLE>_TENANT` → `CORE_TENANT (ID)`; `IDX_<TABLE>_TENANT`; every unique constraint of a tenant-scoped table leads with `TENANT_ID` | common/domain/AuditableEntity.java:34-37; V10__tenant_schema.sql:7-8, :63-80, :82-100, :102-119, :121-138; README.md "Tenant columns (since V10)" |
The 22 columns are registered one by one as TENANT's DBF-TENANT-011…032 (`../../TENANT/P2/db-script-tenant.md`).
The one table whose `TENANT_ID` is NULLABLE and whose entity is global (`CU_APP_CONFIGURATION`, V14:66)
does not use ENT-COMMON-002 — it is the named exception of the global allow-list (ADR-COMMON-003).

### 1d. Boolean columns (converters of `com.erp.common.converter`)
| Converter | Physical type | Values | Used by | Source |
|---|---|---|---|---|
| `BooleanNumberConverter` | `SMALLINT DEFAULT 1 NOT NULL` with `CHK_<TABLE>_ACTIVE_FL CHECK (IS_ACTIVE_FL IN (0,1))` on the pre-V10 CU / FILE / NOTIF tables (`CU_APP_CONFIGURATION.IS_ACTIVE_FL`, `FILE_CATEGORY.IS_ACTIVE_FL`, `NOTIF_TEMPLATE.IS_ACTIVE_FL`, `NOTIF_CHANNEL_CONFIG.IS_ENABLED_FL`) | 1 / 0 (null stays null) | CU `AppConfiguration`, FILE `FileCategory`, NOTIF `NotificationChannelConfig`, `NotificationTemplate` | common/converter/BooleanNumberConverter.java:7-24; V2__cu_schema.sql:25, :50; V5__file_schema.sql:25, :76; V6__notif_schema.sql:30, :40; cu/entity/AppConfiguration.java:68; file/entity/FileCategory.java:71; notif/entity/NotificationChannelConfig.java:53; notif/entity/NotificationTemplate.java:84 |
| `BooleanCharYNConverter` | (a `CHAR` / `VARCHAR` Y/N column — none exists in the core chain) | `Y` / `N` (null stays null; case-insensitive read) | no `@Convert` site at main @ 19b19a4 (only a comment in `sec/entity/User.java:112`) | common/converter/BooleanCharYNConverter.java:7-24 |
SEC's `is_active_fl` columns (`SEC_USER`, `SEC_ROLE`, the three registries) are `BOOLEAN NOT NULL DEFAULT TRUE`
and map to `Boolean` without a converter (V4__sec_schema.sql:48, :62, :74, :109, :123; sec/entity/User.java:115-117);
every table created since V10 uses `BOOLEAN` directly (`../../SEC/P0/platform-summary.md` addendum "Naming").

## 2. XM REGISTER — COMMON v1

COMMON holds no FK and no SOFT-READ of any table; it is the owner of no physical object. The
`TENANT_ID` FK of every tenant-scoped table is the owning module's XM to TENANT (registered in each
module's P2 addendum and in TENANT's DBF register), not COMMON's.

| XM-ID | Type | Surface | Target | Physical object | Status |
|---|---|---|---|---|---|
| (none) | — | the Java surface `com.erp.common.*` is XM-COMMON-001 in `../P1/registry-srs-common.md` | — | — | — |

## 3. FULL_DATABASE_SCRIPT (as built — the conventions, verbatim from the migrations)

```sql
-- ============================================================
-- COMMON — no DDL of its own. The conventions every core table follows:
-- ============================================================

-- 3a. Audit columns, family SEC / MDL / CORE_* (V4__sec_schema.sql:49-52; V10__tenant_schema.sql:37-40)
  created_by     VARCHAR(100)  NOT NULL,
  created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_by     VARCHAR(100),
  updated_at     TIMESTAMPTZ

-- 3a'. Audit columns, family CU / FILE / NOTIF (V2__cu_schema.sql:26-29; V6__notif_schema.sql:31-34)
  CREATED_BY    VARCHAR(255),
  CREATED_AT    TIMESTAMP,
  UPDATED_BY    VARCHAR(255),
  UPDATED_AT    TIMESTAMP

-- 3b. Optimistic lock (V10__tenant_schema.sql:63-80, :145-147; V14:45; V15:36)
  VERSION       BIGINT        NOT NULL DEFAULT 0

-- 3c. Tenant discriminator, one statement of each kind per tenant-scoped table (V10 §2, SEC_USER shown)
ALTER TABLE SEC_USER ADD COLUMN TENANT_ID BIGINT NOT NULL DEFAULT 1, ADD COLUMN VERSION BIGINT NOT NULL DEFAULT 0;
ALTER TABLE SEC_USER ALTER COLUMN TENANT_ID DROP DEFAULT;
ALTER TABLE SEC_USER ADD CONSTRAINT FK_SEC_USER_TENANT FOREIGN KEY (TENANT_ID) REFERENCES CORE_TENANT (ID);
CREATE INDEX IDX_SEC_USER_TENANT ON SEC_USER (TENANT_ID);
-- a table created after V10 declares the three together (V14__sequence_and_settings.sql:33, :41-45, :58, :61):
  TENANT_ID     BIGINT        NOT NULL,
  CREATED_BY    VARCHAR(100),
  CREATED_AT    TIMESTAMPTZ   NOT NULL DEFAULT now(),
  UPDATED_BY    VARCHAR(100),
  UPDATED_AT    TIMESTAMPTZ,
  VERSION       BIGINT        NOT NULL DEFAULT 0
```

## 4. Deviations from this analysis
None — written from the code. The two audit-column families (100 / 255, TIMESTAMPTZ / TIMESTAMP) are
recorded as they are; the JPA mapping is one (length 100, `Instant`) for both.
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 01, 05, 15 (conventions: V10)
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : none recorded here yet — the idempotency mechanism of the tenant-maturity plan (C.4) is documented by the implementing run when it lands on main
Statement      : This addendum records only what is already on main for 1.3.0; the plan packages' rows are written by the implementing run.

Plan packages B, C, D, E, G: documented by each package as it lands on main (analysis-first, written by the implementing run); the verified reference rows are in docs/plans/tenant-maturity-analysis-reference.md.
