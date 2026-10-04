# erp-core Flyway chain — `classpath:db/migration/core`

This folder ships inside the erp-core jar. `com.erp.autoconfigure.ErpCoreFlywayAutoConfiguration`
prepends `classpath:db/migration/core` to whatever `spring.flyway.locations` the consuming
application configures, so Spring Boot's Flyway run applies the core chain first and the
application's own scripts after it.

## Version ranges

| Range | Owner | Where |
|---|---|---|
| `V1` .. `V999` | **erp-core** | this folder |
| `V1000` and up | **applications** | the application's own location (default `classpath:db/migration`) |

An application never adds a script below `V1000`, and erp-core never ships one at `V1000` or above.

## Naming

`V<n>__<module>_<slug>.sql`, where `<module>` is one of
`core, cu, mdl, sec, file, notif, tenant, audit, events, sequence, report` and `<slug>` is
lower-case snake case. `MigrationNamingTest` (erp-core tests) fails the build on a version ≥ 1000,
a name outside this pattern, or a duplicate version. This README is the only other file allowed here.

## Additive only

The chain was squashed once, in erp-core step 04, into one schema script and one seed script per
module. **From then on core scripts are additive only**:

- a new table;
- a new column that is nullable or has a default;
- a new index or constraint that existing data already satisfies;
- new seed rows.

No core script renames or drops a table or column, changes a column type, or edits or renumbers an
existing script (Flyway checksums applied scripts). A mistake is fixed forward by a new script.
Each plan step reserves its version numbers (e.g. step 05 = `V10`, step 06 = `V11`, ...).

The only exceptions ever made are the two the step-05 plan itself sanctions inside
`V10__tenant_schema.sql`: dropping the temporary `DEFAULT 1` of every `TENANT_ID` after the backfill,
and replacing each unique constraint of a tenant-scoped table by its composite `(TENANT_ID, ...)` form
(same constraint name).

## Tenant columns (since V10)

Every core table is tenant-scoped except `CORE_TENANT` and the permission catalog
(`SEC_MODULE_REG`, `SEC_SCREEN_REG`, `SEC_ACTION_REG`). A new tenant-scoped table must have:

- `TENANT_ID BIGINT NOT NULL` (no default) with `FK_<TABLE>_TENANT` → `CORE_TENANT (ID)` and
  `IDX_<TABLE>_TENANT`; every unique constraint starts with `TENANT_ID`;
- `VERSION BIGINT NOT NULL DEFAULT 0` and the audit columns `CREATED_BY`, `CREATED_AT`, `UPDATED_BY`,
  `UPDATED_AT`;
- an entity extending `com.erp.common.domain.AuditableEntity` (Hibernate `@TenantId` fills `TENANT_ID`).

A global table (only when a plan step names it global) has no `TENANT_ID`, and its entity extends
`GlobalAuditableEntity`. Seed rows for tenant-scoped tables name `TENANT_ID` explicitly (`1` = PLATFORM).

## The chain

| Version | Script | Content |
|---|---|---|
| — | `V1__core_common.sql` | not shipped: there are no shared sequences or functions (`V1` stays free) |
| V2 | `V2__cu_schema.sql` | `CU_APP_CONFIGURATION` |
| V3 | `V3__mdl_schema.sql` | `MDL_LOOKUP_TYPE`, `MDL_LOOKUP_VALUE` |
| V4 | `V4__sec_schema.sql` | the 13 `SEC_*` tables |
| V5 | `V5__file_schema.sql` | `FILE_CATEGORY`, `FILE_DOCUMENT` |
| V6 | `V6__notif_schema.sql` | `NOTIF_TEMPLATE`, `NOTIF_CHANNEL_CONFIG`, `NOTIF_LOG` |
| V7 | `V7__sec_seed.sql` | module/screen/action registry of SEC, MDL, NOTIF, FILE, CU; roles; grants; bootstrap `admin` (no usable password) |
| V8 | `V8__mdl_seed.sql` | lookup types/values `NOTIF_CHANNEL`, `NOTIF_STATUS`, `FILE_FILE_STATUS`, `FILE_FILE_TYPE` |
| V9 | `V9__notif_seed.sql` | `EMAIL` channel config; `PASSWORD_RESET` / `ACCOUNT_ACTIVATION` templates |
| V10 | `V10__tenant_schema.sql` | `CORE_TENANT` + PLATFORM tenant (ID 1); `TENANT_ID`/`VERSION` on every tenant-scoped table (backfilled to PLATFORM), `VERSION` on the catalog, audit columns on 8 SEC tables, composite uniques; `PLATFORM` module/screen, `PERM_PLATFORM_TENANTS_VIEW` + `PLATFORM_TENANT_MANAGE` granted to PLATFORM's `SYS_ADMIN` |
| V11 | `V11__sec_realms.sql` | `SEC_USER.REALM` (STAFF/CUSTOMER) + per-realm user uniques, status `PENDING_VERIFICATION`, `SEC_ROLE.IS_SUPER` (every `SYS_ADMIN` super), `SEC_CUSTOMER_VERIFY_TOKEN`, templates `CUSTOMER_VERIFY_EMAIL` / `CUSTOMER_PASSWORD_RESET` for every tenant. The permission catalog is upserted from code at startup by `PermissionCatalogSynchronizer` (V7/V10 catalog rows stay, reproduced identically) |
| V12 | `V12__file_storage.sql` | erp-core step 07: `FILE_DOCUMENT.STORAGE_PROVIDER/STORAGE_REF/VISIBILITY/PUBLIC_SLUG/CONTENT_HASH` (backfilled to DB / id / PRIVATE), `FILE_CONTENT` nullable, partial unique `UQ_FILE_DOCUMENT_PUBLIC_SLUG (TENANT_ID, PUBLIC_SLUG)`, `FILE_CATEGORY.ALLOW_PUBLIC`; permission `FILE:DOCUMENT:PUBLISH` (screen `FILE_BROWSER`) granted to PLATFORM's `SYS_ADMIN` and `FILE_ADMIN` (V11 is reserved by step 06) |
| V13 | `V13__notif_async_inbox.sql` | step 08: `NOTIF_LOG` `ATTEMPTS`/`NEXT_ATTEMPT_AT`/`LAST_ERROR`/`VARIABLES_JSON`; `NOTIF_INBOX` (IN_APP channel); `NOTIF_STATUS` values `QUEUED`/`SKIPPED_NO_PROVIDER`, `NOTIF_CHANNEL` value `IN_APP` and an enabled `IN_APP` channel config for every existing tenant |
| V14 | `V14__sequence_and_settings.sql` | `CORE_NUMBER_SERIES` (tenant-scoped number series, one row per code and period; erp-core step 09). `CU_APP_CONFIGURATION.TENANT_ID` becomes nullable (NULL = platform default, the step-09 plan's one deliberate exception to NOT NULL tenants; the entity extends `GlobalAuditableEntity` and is filtered explicitly) and its uniqueness becomes the unique index `UQ_CU_APP_CONFIG_CONFIG_KEY (COALESCE(TENANT_ID, 0), CONFIG_KEY)`. |

## Bootstrap admin

`V7__sec_seed.sql` creates the account `admin` (role `SYS_ADMIN`) with status `PENDING` and a
placeholder instead of a password hash, so it cannot log in. Set
`erp.core.security.bootstrap-admin-password` (the reference app maps it to the optional
environment variable `ERP_BOOTSTRAP_ADMIN_PASSWORD`) and, on the first start,
`com.erp.sec.security.BootstrapAdminPasswordRunner` hashes it into the account and activates it.
Once the account is initialised the property is ignored. The account belongs to the PLATFORM tenant:
log in with `X-Tenant-Code: PLATFORM`.
