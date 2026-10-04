# Step 04 — Clean core migration chain and version ranges

**Branch:** `step/04-flyway-core-chain`
**Goal:** the core ships one clean, readable Flyway chain (`V1..V999` reserved for core), apps use `V1000+`, and from this step on core migrations are **additive only**.
**Why:** the source chain (after step 01) is a historical sequence of schema + seed + repair scripts (e.g. `V14__drop_legacy_security_schema.sql`, `V20__notif_file_lookup_data_migration.sql`). A library must ship a chain that creates a fresh database cleanly and that consumers can read. A fresh project has no production data, so squashing is allowed **once, here, and never again**.

## Preconditions
- Step 03 merged; `erp-core/src/main/resources/db/migration/core/` holds the renumbered chain from step 01.

## Tasks
1. **Squash into per-module scripts** (one schema + one seed per module, in dependency order). Produce exactly these files:
   ```
   V1__core_common.sql              -- shared sequences/functions if any (may be empty → omit if nothing)
   V2__cu_schema.sql                -- CU_APP_CONFIGURATION
   V3__mdl_schema.sql               -- MDL_LOOKUP_TYPE, MDL_LOOKUP_VALUE
   V4__sec_schema.sql               -- SEC_* (final shape after V14/V16 of the old chain)
   V5__file_schema.sql              -- FILE_DOCUMENT, FILE_CATEGORY
   V6__notif_schema.sql             -- NOTIF_TEMPLATE, NOTIF_CHANNEL_CONFIG, NOTIF_LOG
   V7__sec_seed.sql                 -- module/screen/action registry rows for sec, cu, mdl, file, notif; roles; grants; bootstrap admin (old V3,V7,V9,V13,V17,V19,V21,V31)
   V8__mdl_seed.sql                 -- NOTIF_CHANNEL, NOTIF_STATUS, FILE_FILE_STATUS, FILE_FILE_TYPE lookup types/values (old V18, V20 final state)
   V9__notif_seed.sql               -- email channel config + templates incl. password-reset copy (old V11, V12, V32 final state)
   ```
   Method: start a scratch PostgreSQL, run the current chain, dump schema (`pg_dump --schema-only`) and seed tables (`pg_dump --data-only --table=...`) to derive the final state, then hand-write the squashed scripts in the existing style (explicit `CREATE TABLE`, named constraints, `COMMENT ON` where the old scripts had them). Do **not** ship a raw pg_dump.
2. **Bootstrap admin**: keep the seeded admin user but make the password hash come from a placeholder that the app overrides on first start — simplest compliant approach: seed username `admin` with status `MUST_RESET` (or equivalent existing status) and no usable password; add `erp.core.security.bootstrap-admin-password` property consumed by a `ApplicationRunner` in `sec` that sets the password on first run when the property is present. Document in `docs/steps/04-report.md`.
3. **Reserve ranges**: add `erp-core/src/main/resources/db/migration/core/README.md` stating: core = `V1..V999`; apps = `V1000+`; core scripts after this step are additive only (new table / nullable or defaulted column / index / seed). Add a unit test `MigrationNamingTest` that fails if any core script version ≥ 1000 or if a filename is not `V<n>__<module>_<slug>.sql` with `module ∈ {core,cu,mdl,sec,file,notif,tenant,audit,events,sequence,report}`.
4. **Flyway customizer** (from step 03): verify it prepends `classpath:db/migration/core` and that the app's own `V1000__app_smoke.sql` applies after it.
5. **Schema conventions check**: every table has `ID BIGINT` PK (or the existing `*_PK` where already used — do not rename existing columns), `CREATED_BY/AT`, `UPDATED_BY/AT`. Record exceptions in the report (they are fixed in step 05 when `TENANT_ID`/`VERSION` are added).
6. Delete the old scripts and the old `README.md`.

## Acceptance
- Fresh database from the core chain: `V1..V9` apply with no errors; all existing integration tests green.
- Reference app smoke test green (`V1000` applied after core).
- `MigrationNamingTest` green; adding a file `V1000__x.sql` to core makes it fail (verify manually, then remove).
- `pg_dump --schema-only` of the new chain and of the old chain differ only in constraint/sequence names and comments (compare with `diff` after normalizing; record the diff summary in the report).

## Verification commands
```bash
mvn -q verify
ls erp-core/src/main/resources/db/migration/core | sort -V
```

## Commit
`step(04): squash core migrations into per-module schema/seed scripts; reserve V1..V999 for core, V1000+ for apps; additive-only rule`

## Out of scope
Any column additions (step 05+).
