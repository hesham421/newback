# Step 04 — Clean core migration chain and version ranges — report

## Summary

The 20-script historical core chain (`V1..V20` after step 01) is replaced by one schema script and
one seed script per module:

```
V2__cu_schema.sql     V3__mdl_schema.sql    V4__sec_schema.sql    V5__file_schema.sql
V6__notif_schema.sql  V7__sec_seed.sql      V8__mdl_seed.sql      V9__notif_seed.sql
```

`V1__core_common.sql` is not shipped: the old chain has no shared sequence, function or extension,
and the step file allows omitting it.

How the scripts were built:
- The old chain was run into a scratch PostgreSQL 16 and dumped (schema and data).
- The schema scripts carry the final DDL unchanged, under new headers. The legacy SEC schema that
  old V2 created and old V12 dropped no longer exists.
- The seed scripts were rewritten by hand, as set-based inserts, to reproduce the final data.

How the result was compared:
- `pg_dump --schema-only`, old chain vs new chain: identical after normalisation (zero lines of
  difference).
- Seed data compared by natural key: identical, except the bootstrap `admin` row, which differs by
  design (task 2).

Task 2, bootstrap admin:
- The `admin` account (role `SYS_ADMIN`) is now seeded with status `PENDING` and a non-BCrypt
  placeholder hash. No password can log in as it.
- A new `com.erp.sec.security.BootstrapAdminPasswordRunner` reads
  `erp.core.security.bootstrap-admin-password`. On first start it sets the password and activates
  the account, once.
- The well-known `admin/admin` credential no longer exists.

Ranges and rules:
- Core owns `V1..V999`; applications own `V1000+`. After this step, core scripts are additive only.
- These rules are documented in `erp-core/src/main/resources/db/migration/core/README.md`.
- They are enforced by the new `MigrationNamingTest`.

Verification:
- `mvn -q verify` is green: erp-core 69 tests, erp-app-reference 5 tests.
- The reference app applies core V2..V9 first, then its own V1000.

### Old → new mapping (which old scripts fed which new file)

Old names use the pre-step-01 numbering, which is how the step file cites them. The post-step-01
name, i.e. the file actually deleted here, is in brackets.

| New file | Fed by (old final state) |
|---|---|
| `V2__cu_schema.sql` | V1__cu_app_configuration_schema [V1] — DDL verbatim |
| `V3__mdl_schema.sql` | V18__mdl_sequences [V15] — DDL verbatim (it seeded nothing) |
| `V4__sec_schema.sql` | V16__sec_schema [V13] — DDL verbatim. Never carried over: V2__sec_security_schema [V2], whose legacy tables V14__drop_legacy_security_schema [V12] dropped |
| `V5__file_schema.sql` | V8__file_schema [V6] — DDL verbatim |
| `V6__notif_schema.sql` | V6__notif_schema [V4] — DDL verbatim |
| `V7__sec_seed.sql` | V17__sec_security_seed [V14] (SEC registry, SYS_ADMIN, its grants, admin user) · V19__mdl_security_seed [V16] (MDL registry) · V20__notif_file_lookup_data_migration [V17] (NOTIF/FILE `SEC_MODULE_REG` rows) · V21__mdl_role_grants [V18] (SYS_ADMIN→MDL grants) · V31__cu_notif_file_security_seed [V19] (CU module; CU/NOTIF/FILE screens and actions; CU_ADMIN/NOTIF_ADMIN/FILE_ADMIN; grants) · V10__sec_bootstrap_admin_user [V8] (admin, superseded by V17's copy) |
| `V8__mdl_seed.sql` | V20__notif_file_lookup_data_migration [V17] (the 4 lookup types and 17 values) |
| `V9__notif_seed.sql` | V11__notif_email_channel_seed [V9] · V12__notif_email_templates_action_link [V10] · V32__notif_password_reset_copy [V20] (final subjects and bodies) |
| (no rows survive) | V3__sec_security_seed [V3], V7__notif_security_seed [V5], V9__file_security_seed [V7], V13__cu_security_seed [V11]: legacy `SEC_MODULE/SEC_PAGE/SEC_PERMISSION...` rows dropped by V14 [V12]. Only the bilingual role and screen names V31 had already re-used survive. |

## Files changed

**Created**
- `erp-core/src/main/resources/db/migration/core/V2__cu_schema.sql`, `V3__mdl_schema.sql`, `V4__sec_schema.sql`, `V5__file_schema.sql`, `V6__notif_schema.sql`, `V7__sec_seed.sql`, `V8__mdl_seed.sql`, `V9__notif_seed.sql`
- `erp-core/src/main/resources/db/migration/core/README.md` (new content: version ranges, naming, the additive-only rule, the chain, the bootstrap admin)
- `erp-core/src/main/java/com/erp/sec/security/BootstrapAdminPasswordRunner.java`
- `erp-core/src/test/java/com/erp/autoconfigure/MigrationNamingTest.java`
- `erp-core/src/test/java/com/erp/autoconfigure/ErpCoreFlywayAutoConfigurationTest.java`
- `erp-core/src/test/java/com/erp/sec/BootstrapAdminPasswordIntegrationTest.java`
- `docs/steps/04-report.md`

**Modified**
- `erp-core/src/main/java/com/erp/autoconfigure/ErpCoreProperties.java`: adds `erp.core.security.bootstrap-admin-password`, optional, no default.
- `erp-core/src/main/java/com/erp/sec/domain/UserDomain.java`: adds `BOOTSTRAP_PASSWORD_PLACEHOLDER` and `awaitsBootstrapPassword(User)`.
- `erp-core/src/main/java/com/erp/autoconfigure/ErpCoreFlywayAutoConfiguration.java`: javadoc only (location normalisation, version ranges).
- Comment-only edits that re-point references to deleted migration files:
  - `sec/permission/PermissionConstants.java`
  - `sec/service/PasswordResetService.java`
  - `file/service/FileLookupService.java`
  - `notif/service/NotificationLookupService.java`
  - `mdl/crossmodule/MdlLookupApi.java`
  - `erp-core/src/test/java/com/erp/sec/SecCoverageIntegrationTest.java`
- `erp-app-reference/src/main/resources/application.yml`:
  - adds `erp.core.security.bootstrap-admin-password: ${ERP_BOOTSTRAP_ADMIN_PASSWORD:}`;
  - the header documents the optional variable.
- `erp-app-reference/src/test/java/com/erp/app/ReferenceApplicationSmokeTest.java`:
  - sets `ERP_BOOTSTRAP_ADMIN_PASSWORD` and logs in with it;
  - the Flyway assertion is now exactly `2..9, 1000`;
  - new test: `admin/admin` → 401.
- `docs/DEVIATIONS.md`: 10 `[04]` entries.

**Deleted**
- `erp-core/src/main/resources/db/migration/core/V1__cu_app_configuration_schema.sql` … `V20__notif_password_reset_copy.sql` (all 20 old scripts)
- the old `erp-core/src/main/resources/db/migration/core/README.md` (replaced by the new one above)

**Untouched**
- `erp-core-plan/execution-state.json`
- `erp-app-reference/governance/`
- `erp-core/src/test/resources/application-test.properties` (no core test logs in as the seeded admin)

## Decisions & deviations

These mirror the 10 `[04]` entries in `docs/DEVIATIONS.md`:

1. **Old numbering.** The step file's "old V…" numbers use the pre-step-01 numbering. V21/V31/V32 exist only there, and V18/V20/V11/V12/V32 match the named content.
2. **`V1__core_common.sql` is omitted.** There is nothing shared to put in it. The chain is `V2..V9`. `V1` can never be added later, because Flyway rejects a version below the applied maximum.
3. **Scratch-database method.** The old and new chains were applied with `psql -1 -v ON_ERROR_STOP=1`, one transaction per script (as Flyway does), into scratch DBs on the native PostgreSQL 16.15, dumped, then dropped. `flyway_schema_history` is excluded from the comparison.
4. **Seeds are set-based** (`INSERT … SELECT … FROM (VALUES …) ORDER BY`), with natural-key foreign keys.
   - The data matches the old final state by natural key.
   - The three grant tables may assign surrogate ids in a different order.
   - Every sequence ends at the same value.
5. **Admin status and hash.** `MUST_RESET` does not exist in `CHK_SEC_USER_STATUS`, so the admin is seeded `PENDING`. Its hash is the placeholder `BOOTSTRAP-PASSWORD-NOT-SET` (the column is NOT NULL). The runner acts only on `PENDING` + placeholder, and calls `User.activate()`.
6. **The runner is startup infrastructure.** It is a `@Component ApplicationRunner` in `com.erp.sec.security` that uses the repository directly, with no `@PreAuthorize`, because there is no principal at startup. This is a named deviation from build-create-service A.5.2.
7. **Tests and the reference app.** `application-test.properties` is unchanged. The reference app maps the property to the optional `ERP_BOOTSTRAP_ADMIN_PASSWORD`, and its smoke test sets it.
8. **Task 4.** Flyway sorts the configured locations and drops nested ones. The effective guarantee is unit-tested, and the applied order (by version) is asserted in the smoke test.
9. **`MigrationNamingTest`** lives in `com.erp.autoconfigure`. It scans the classpath, allows `README.md`, and also rejects duplicate versions.
10. **Stale migration file names in comments** were re-pointed to the new scripts.

### Bootstrap admin: how it works and how to set it

- **Seed** (`V7__sec_seed.sql`): `admin` / `admin@erp.local`, role `SYS_ADMIN`, `STATUS_CODE='PENDING'`, `PASSWORD_HASH='BOOTSTRAP-PASSWORD-NOT-SET'`. Login always answers 401: it requires `ACTIVE`, and the placeholder matches no password.
- **Property:** `erp.core.security.bootstrap-admin-password` (`ErpCoreProperties.Security.bootstrapAdminPassword`). It is optional, and blank counts as absent.
- **Runner:** `com.erp.sec.security.BootstrapAdminPasswordRunner` (`ApplicationRunner`, transactional). When the property is set and `admin` still awaits its bootstrap password (`UserDomain.awaitsBootstrapPassword`), it sets the BCrypt hash and activates the account (`ACTIVE`, active flag true).
  - Otherwise it logs and does nothing.
  - It never overwrites a password once the account is initialised, so later restarts with the property still set are harmless.
  - The password is never logged.
- **Reference app:** `ERP_BOOTSTRAP_ADMIN_PASSWORD=<password>` on the first start, e.g.
  `ERP_BOOTSTRAP_ADMIN_PASSWORD='S3cret!' java -jar erp-app-reference/target/erp-app-reference-1.0.0-SNAPSHOT.jar`.
- **Any other consumer app:** set `erp.core.security.bootstrap-admin-password` by any Spring property source.
- **Tests:**
  - erp-core leaves the property unset, and the seeded admin stays `PENDING`.
  - `BootstrapAdminPasswordIntegrationTest` drives its own runner instance in a rolled-back transaction.
  - The reference smoke test sets `ERP_BOOTSTRAP_ADMIN_PASSWORD=Smoke-Admin-Passw0rd!`.

### Schema conventions check (task 5)

Query over the new chain's `information_schema`/`pg_index` (21 tables). Exceptions, to be fixed in step 05 or recorded there:

| Table | PK column | Missing `CREATED_BY/AT, UPDATED_BY/AT` |
|---|---|---|
| `SEC_USER`, `SEC_ROLE`, `SEC_MODULE_REG`, `SEC_SCREEN_REG`, `SEC_ACTION_REG` | `*_pk` (existing, kept) | none |
| `MDL_LOOKUP_TYPE`, `MDL_LOOKUP_VALUE` | `*_pk` (existing, kept) | none |
| `SEC_USER_ROLE` | `user_role_pk` | all four (has `assigned_by/assigned_at`) |
| `SEC_ROLE_MODULE_GRANT`, `SEC_ROLE_SCREEN_GRANT`, `SEC_ROLE_ACTION_GRANT` | `*_pk` | all four (have `granted_by/granted_at`) |
| `SEC_ACTIVE_SESSION` | `active_session_pk` | all four (has `started_at/last_activity_at/terminated_*`) |
| `SEC_AUDIT_LOG` | `audit_log_pk` | all four (append-only, has `occurred_at`) |
| `SEC_PWD_RESET_TOKEN` | `pwd_reset_token_pk` | all four (has `requested_at/used_at`) |
| `SEC_SIGNUP_REQUEST` | `signup_request_pk` | all four (has `submitted_at/reviewed_by/reviewed_at`) |

`CU_APP_CONFIGURATION`, `FILE_CATEGORY`, `FILE_DOCUMENT`, `NOTIF_TEMPLATE`, `NOTIF_CHANNEL_CONFIG` and `NOTIF_LOG` are compliant: `ID BIGINT` plus all four audit columns.

Further inconsistencies, recorded and not fixed (out of scope here):
- **CU/FILE/NOTIF:** audit columns are `VARCHAR(255)`/`TIMESTAMP` and nullable; flags are `SMALLINT`.
- **SEC/MDL:** `VARCHAR(100)`/`TIMESTAMPTZ`, `created_by`/`created_at` NOT NULL; native `BOOLEAN`.

### pg_dump diff summary (acceptance 4)

- **Schema.** Raw `pg_dump --schema-only --no-owner --no-privileges` of the old chain (`erp_step04_old`) and the new chain (`erp_step04_new`): 2471 lines each.
  - `diff` shows 4 differing lines. These are the two `\restrict`/`\unrestrict` lines, whose random per-dump tokens are a pg_dump 16.10+ security feature.
  - After normalisation (drop `--` comment lines, drop `\restrict`/`\unrestrict`, replace timestamp literals): 1579 vs 1579 lines, `diff` = **0 lines**.
  - Constraint names, sequence names and comments are therefore identical as well, because the DDL was carried over verbatim.
  - Objects: 21 tables, 21 sequences, 30 indexes, 53 constraints.
- **Data, beyond the acceptance.** Compared by natural key (`canon.sql`): 177 vs 177 rows, and one differing row, the bootstrap admin:
  `ACTIVE|$2y$10$.G36…` → `PENDING|BOOTSTRAP-PASSWORD-NOT-SET`.
  - Every sequence ends at the same `last_value`: `seq_sec_action_reg`=38, `seq_sec_role_action_grant`=59, `seq_sec_role_screen_grant`=20, `seq_sec_role_module_grant`=8, `seq_sec_screen_reg`=17, `seq_sec_module_reg`=5, `seq_sec_role`=4, `seq_mdl_lookup_value`=17, …
  - Raw `--data-only` dumps differ only in timestamps and in the surrogate ids of a few screen and grant rows, which were inserted in a different order.

## Acceptance checklist

4/4 ✅

- ✅ **A fresh database from the core chain: `V1..V9` apply with no errors; all existing integration tests green.**
  - The chain is `V2..V9` (`V1` omitted, see deviation 2).
  - Flyway applied it in every erp-core integration test (embedded PostgreSQL 16) and in the reference smoke test, with `flyway_schema_history` = `2,3,4,5,6,7,8,9,1000` and 0 failed rows.
  - `mvn -q verify`: erp-core tests=69, failures=0, errors=0 (all 60 pre-existing tests pass unchanged, plus 9 new).
- ✅ **Reference app smoke test green (`V1000` applied after core).**
  - `ReferenceApplicationSmokeTest`: 5/5.
  - `containsExactly("2","3","4","5","6","7","8","9","1000")` in `installed_rank` order.
  - The admin logs in with `ERP_BOOTSTRAP_ADMIN_PASSWORD`, and `admin/admin` → 401.
- ✅ **`MigrationNamingTest` green; adding `V1000__x.sql` to core makes it fail (verified manually, then removed).**
  - Green in the full verify (2/2).
  - With `V1000__x.sql` added: `Tests run: 2, Failures: 1`, message `"V1000__x.sql: not V<n>__<module>_<slug>.sql with module in {core|cu|mdl|sec|file|notif|tenant|audit|events|sequence|report}"`.
  - With a well-named `V1000__core_x.sql`: `"V1000__core_x.sql: version 1000 is in the application range (V1000+); core owns V1..V999"`.
  - Both files were removed afterwards, from `src` and from `target/classes`.
- ✅ **`pg_dump --schema-only` of new vs old chain differ only in constraint/sequence names and comments.**
  - After normalisation they do not differ at all (0 lines); see the diff summary above.

## Verification output

```
$ java -version  → openjdk version "21.0.7" 2025-04-15

$ rm -rf target erp-core/target erp-app-reference/target; mvn -q verify
EXIT=0 secs=130
[TestPostgres] integration-test database backend: EMBEDDED      (once per module JVM)

surefire totals (target/surefire-reports/TEST-*.xml)       tests fail err skip
== erp-core
com.erp.architecture.CrossModuleBoundaryArchTest                 2 0 0 0
com.erp.autoconfigure.DownloadTokenStoreAutoConfigurationTest    2 0 0 0
com.erp.autoconfigure.ErpCoreAutoConfigurationTest               9 0 0 0
com.erp.autoconfigure.ErpCoreFlywayAutoConfigurationTest         3 0 0 0   (new)
com.erp.autoconfigure.MigrationNamingTest                        2 0 0 0   (new)
com.erp.file.service.InMemoryDownloadTokenStoreTest              3 0 0 0
com.erp.notif.service.DefaultChannelProviderTest                 2 0 0 0
com.erp.sec.BootstrapAdminPasswordIntegrationTest                4 0 0 0   (new)
com.erp.sec.MenuServiceGatewayIntegrationTest                    2 0 0 0
com.erp.sec.SecCoverageIntegrationTest                           6 0 0 0
com.erp.sec.SecFrontendGapIntegrationTest                        5 0 0 0
com.erp.sec.SecLogoutIntegrationTest                             3 0 0 0
com.erp.sec.SecReadOneIntegrationTest                            6 0 0 0
com.erp.sec.SecSearchFilterIntegrationTest                      10 0 0 0
com.erp.sec.UserRolesInResponseIntegrationTest                   8 0 0 0
com.erp.testsupport.TestProfileWiringIntegrationTest             2 0 0 0
erp-core tests=69 failures=0 errors=0 skipped=0
== erp-app-reference
com.erp.app.ReferenceApplicationSmokeTest                        5 0 0 0   (+1)
erp-app-reference tests=5 failures=0 errors=0 skipped=0

$ ls erp-core/src/main/resources/db/migration/core | sort -V
README.md
V2__cu_schema.sql
V3__mdl_schema.sql
V4__sec_schema.sql
V5__file_schema.sql
V6__notif_schema.sql
V7__sec_seed.sql
V8__mdl_seed.sql
V9__notif_seed.sql

(first verify attempt: ErpCoreFlywayAutoConfigurationTest 3 failures. Its first version asserted the
 raw prepend order, which Flyway normalises (sorts, drops nested locations); the test was rewritten
 to the effective guarantee, see deviation 8.)

Scratch comparison (native PostgreSQL 16.15, scratch DBs dropped afterwards):
$ createdb -E UTF8 --locale=C -T template0 erp_step04_old   # + each old V1..V20 via psql -1 -v ON_ERROR_STOP=1
$ createdb -E UTF8 --locale=C -T template0 erp_step04_new   # + each new V2..V9 the same way
$ diff old-schema.sql new-schema.sql | grep -c '^[<>]'   → 4   (\restrict / \unrestrict tokens only)
$ diff old-schema.norm new-schema.norm                   → (empty), rc=0; 1579 vs 1579 lines
$ diff canon-old.txt canon-new.txt                       → 1 row (admin: ACTIVE|$2y$10$… → PENDING|BOOTSTRAP-PASSWORD-NOT-SET)
$ dropdb erp_step04_old; dropdb erp_step04_new; select count(*) from pg_database where datname like 'erp_step04%' → 0

Manual naming check:
$ echo "SELECT 1;" > erp-core/src/main/resources/db/migration/core/V1000__x.sql
$ mvn -q -pl erp-core test -Dtest=MigrationNamingTest   → EXIT=1
[ERROR] Tests run: 2, Failures: 1 ... MigrationNamingTest.everyCoreScriptIsNamedVnModuleSlug_withAVersionBelow1000_andUnique:64
Expecting empty but was: ["V1000__x.sql: not V<n>__<module>_<slug>.sql with module in {core|cu|mdl|sec|file|notif|tenant|audit|events|sequence|report}"]
(same with V1000__core_x.sql → "version 1000 is in the application range (V1000+); core owns V1..V999")
$ rm V1000__x.sql V1000__core_x.sql (src and target/classes)
```

## Skills checked

- `.claude/skills/build-create-entity/SKILL.md`: compliant. No entity was added or changed.
  - The DB naming in the new scripts is the old DDL verbatim (UPPER_SNAKE module-prefixed tables, `PK_/UQ_/FK_/CHK_/IDX_` names, `SEQ_<TABLE>` sequences, no identity columns), so the entities still map 1:1.
  - The audit-column gaps are recorded above (task 5), not fixed (out of scope).
  - `UserDomain` gains one pure predicate and a constant: no Spring/JPA annotation and no repository (A.0.2/A.0.3). The existing `User.activate()` performs the mutation (decide in Domain, execute on entity).
- `.claude/skills/build-create-service/SKILL.md`: no service added.
  - `BootstrapAdminPasswordRunner` is startup infrastructure. It has no principal, so it carries no `@PreAuthorize` and calls `UserRepository` directly (named deviation from A.5.2/A.5.8, the same pattern as `JwtAuthenticationFilter`).
  - It is `@Transactional`, logs writes with `log.info`, never logs the password, makes no cross-module call and throws nothing.
  - The decision ("does this account await its bootstrap password?") is delegated to `UserDomain.awaitsBootstrapPassword` (A.5.18).
  - ArchUnit `CrossModuleBoundaryArchTest` is green.
- `.claude/skills/gov-enforce-error-handling/SKILL.md`: compliant.
  - No exception is thrown by the new code, and no error code or i18n entry was added.
  - The login failure path for the PENDING admin is the existing `LocalizedException(Status.UNAUTHORIZED, SEC_401_INVALID_CREDENTIALS)`, asserted in `BootstrapAdminPasswordIntegrationTest`.

## Notes for later steps

- **Migration numbering.**
  - Core scripts live in `erp-core/src/main/resources/db/migration/core/`, named `V<n>__<module>_<slug>.sql` with module ∈ `core,cu,mdl,sec,file,notif,tenant,audit,events,sequence,report`.
  - `MigrationNamingTest` rejects anything else, any version ≥ 1000 and duplicates. A new module (e.g. `platform`) must first be added to its `MODULES` list and to the README.
  - The current maximum is `V9`. The plan's reservations follow: 05=`V10__tenant_schema.sql`, 06=`V11__sec_realms.sql`, 07=`V12__file_storage.sql`, 08=`V13__notif_async_inbox.sql`, 09=`V14__sequence_and_settings.sql`, 10=`V15__audit_schema.sql`. All of these match the naming rule.
  - `V1` is unusable (below the applied maximum).
- **Additive only from now on:** new table, nullable or defaulted column, index, or seed rows. Never rename, drop, change a type, or edit or renumber a shipped script.
  - Step 05's "drop the `DEFAULT 1` after backfill" and "replace unique constraints with composite ones" are the plan's own sanctioned exceptions inside its V10. Record them as such there.
- **Applications use `V1000+`** in their own location (default `classpath:db/migration`). Flyway finds the core folder through that parent location.
- **Admin password.**
  - Seeded `admin` is `PENDING` with placeholder `UserDomain.BOOTSTRAP_PASSWORD_PLACEHOLDER` (`BOOTSTRAP-PASSWORD-NOT-SET`).
  - It is set once from `erp.core.security.bootstrap-admin-password`. The reference app reads it from env `ERP_BOOTSTRAP_ADMIN_PASSWORD`, which is optional.
  - Any test or API verification that logs in as `admin` must set the property first. Example: the reference smoke test uses `Smoke-Admin-Passw0rd!`. The old `admin/admin` → 401.
  - In erp-core tests the seeded admin stays `PENDING`. Tests that need an admin principal keep using `setAuthenticatedPrincipal(...)` or persist their own user.
- **Step 05 (tenant):** when `TENANT_ID` is added to `SEC_USER`, keep the seed-admin lookup in `BootstrapAdminPasswordRunner` (`findByUsername("admin")`) working, e.g. run it in the PLATFORM tenant context.
- **Step 06** may move screen/action registry rows out of `V7__sec_seed.sql` only by a new additive script or a synchronizer, never by editing V7.
- **Schema-convention exceptions** (8 SEC tables without the four audit columns; `*_pk` PKs on SEC/MDL; type differences between the CU/FILE/NOTIF and SEC/MDL styles) are listed above for step 05.
