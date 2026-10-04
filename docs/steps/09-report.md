# Step 09 — Number series (sequences) and typed, tenant-aware settings API — report

## Summary

**Number series (`com.erp.sequence`, new core package).**
- `CORE_NUMBER_SERIES` (V14) is tenant-scoped, with one row per (code, period). `NEXT_VALUE` is the next number. `PERIOD_KEY` is `''` for `NEVER`, `YYYY` for `YEARLY` and `YYYY-MM` for `MONTHLY`. The unique key is `(TENANT_ID, CODE, PERIOD_KEY)`.
- `NumberSeriesApi` (`com.erp.sequence.crossmodule`) offers `next(code)` and `preview(code)`.
  - `next` runs in a `REQUIRES_NEW` transaction. It locks the series' anchor row (the lowest id of the code). Hibernate generates `... where tenant_id = ? and code = ? order by id fetch first ? rows only for no key update`.
  - It then locks the row of the current period, or creates it at 1 on a YEARLY/MONTHLY reset. It takes the counter and commits.
  - Numbers are atomic and consecutive within an allocation. A rolled-back caller leaves a gap, which the plan accepts.
  - An unknown or inactive code raises `SEQUENCE_NOT_CONFIGURED`. Nothing is created implicitly.
- `NumberPattern` handles the tokens `{PREFIX} {YYYY} {YY} {MM} {SEQ:n} {TENANT}`. It is validated at save time and raises `SEQUENCE_PATTERN_INVALID` for:
  - an unknown token or malformed braces;
  - anything other than exactly one `{SEQ:n}` with 1 ≤ n ≤ 18;
  - a pattern that would repeat numbers under its reset policy.
- The admin CRUD is `/api/v1/sequence/series`: create, get, search, update, activate, deactivate. It is STAFF-only, under `PERM_SEQUENCE_SERIES_MANAGE` (the step file's `SEQUENCE:SERIES:MANAGE`) with the gateway `PERM_SEQUENCE_SERIES_VIEW`. `SequencePermissions` contributes both.
- `{TENANT}` is resolved through step 07's `com.erp.tenant.crossmodule.TenantLookupApi`.
- `SequenceTenantProvisioningContributor` copies PLATFORM's series definitions into new tenants, with the counter set back to 1.

**Settings (`com.erp.cu`).**
- `CU_APP_CONFIGURATION.TENANT_ID` is now nullable: `NULL` is a platform default, and a tenant id is that tenant's override. This is the plan's one exception. The entity now extends `GlobalAuditableEntity` and is filtered explicitly.
- Uniqueness is the unique index `UQ_CU_APP_CONFIG_CONFIG_KEY (COALESCE(TENANT_ID,0), CONFIG_KEY)`.
- `SettingsApi` (`com.erp.cu.crossmodule`) offers `get`, `get(key, Class)`, `find`, `find(key, Class)`, `getOrDefault` and `getOrDefault(key, Class, default)`.
  - Supported types: `String`, `Integer`, `Long`, `Boolean`, `BigDecimal` and `Duration`.
  - Resolution order: the active tenant override, then the active platform default, then `NoSuchSettingException` (`SETTING_NOT_FOUND`).
  - A value that cannot be read as the requested type raises `SETTING_TYPE_MISMATCH`.
- The cache is the Spring cache `erpCoreSettings`. `ConfigurationService.resolve` is `@Cacheable` with key `<tenantId>:<KEY>`.
- Every CRUD write evicts the cache with `@CacheEvict(allEntries = true)`.
- The CRUD endpoints take `?scope=TENANT|PLATFORM`, default `TENANT`. `PLATFORM` requires `PLATFORM_SETTINGS_MANAGE`, which `CuPermissions` declares under the PLATFORM registry module, and the PLATFORM tenant.
- The new `ErpCoreCacheAutoConfiguration` provides `@EnableCaching` plus a `CacheManagerCustomizer` that registers `erpCoreSettings` for the `simple` manager. The cached value is a plain `String`, so the Redis cache type works too.

`mvn -q verify` is green:
- erp-core: 215 tests (138 before, plus 77 new);
- erp-app-reference: 7 tests.

The concurrency test was green on 3 consecutive separate runs.

### Rebase onto 07/08

The branch was rebased onto `main` after steps 07 (merge `6c0eae1`) and 08 (merge `de18fe9`). Conflicts and how they were resolved:

- `CORE_PACKAGE_LIST`: `... ,com.erp.tenant` + `,com.erp.events` (08) + `,com.erp.sequence` (09). `ErpCoreAutoConfigurationTest` expects that order.
- `AutoConfiguration.imports`: 07's `FileStorageAutoConfiguration`, then 08's `ErpCoreEventsAutoConfiguration` and `ErpCoreNotifAutoConfiguration`, then `ErpCoreCacheAutoConfiguration`.
- i18n bundles, `docs/DEVIATIONS.md`, `db/migration/core/README.md`: the 06, 07 and 08 blocks, then 09. The V14 row and the V14 header no longer call V12/V13 "reserved"; V14 now follows V2..V13. V14 has not been applied anywhere outside this branch, so its checksum is not a concern.
- `CrossModuleBoundaryArchTest.MODULES`: 08's `com.erp.events` entry, then `com.erp.sequence`. 08's rules are kept.
- `TenantSchemaIntegrationTest`, reconciled against the run:
  - `tenant_id` columns: 21 (18 + SEC_CUSTOMER_VERIFY_TOKEN + NOTIF_INBOX + CORE_NUMBER_SERIES), with `cu_app_configuration` the one nullable exception.
  - Unique constraints: 14.
  - Tenant-aware entities: 20 (+NotificationInboxItem, +NumberSeries, −AppConfiguration). `AppConfiguration` stays in the global set.
- `PermissionCatalogIntegrationTest` merged without conflict: 07's 41 seeded actions, plus 09's skip of its 4 synchronizer-only permissions and 2 screens.
- `ReferenceApplicationSmokeTest`: Flyway `2..14, 1000`.

Further changes:

- **Duplicate API dropped.** Step 07 had added `com.erp.tenant.crossmodule.TenantLookupApi.codeOf(tenantId)`, which does the same as this step's `TenantDirectoryApi`. `TenantDirectoryApi`/`Impl` were deleted, and `NumberAllocationService` uses `TenantLookupApi` for `{TENANT}`. The deviation entry was updated. The swap is part of the rebased step commit; this report and its notes are the follow-up commit.
- **Interplay with 08, no conflict.**
  - 08's `ErpCoreEventsAutoConfiguration` adds `@EnableAsync` at the default order, plus its own executor. `ErpCoreCacheAutoConfiguration`'s `@EnableCaching(order = LOWEST_PRECEDENCE - 1)` is a separate advisor; no method is both `@Async` and cached.
  - 08 publishes no event for configuration or settings changes, so none was added here. Cache eviction stays on the CRUD writes.
  - Tenant provisioning is still the `TenantProvisioningContributor` SPI, so `SequenceTenantProvisioningContributor` is unchanged.

After the rebase:

- `rm -rf target */target; mvn -q -o verify` → EXIT=0 in 198 s.
  - erp-core: 283 tests, 0 failures, 0 errors, 0 skipped.
  - erp-app-reference: 9 tests, 0 failures, 0 errors, 0 skipped.
- `NumberSeriesConcurrencyIntegrationTest` re-run: 1/1 passed (32.2 s including context start), BUILD SUCCESS.

## Files changed

**Created**
- Migration: `erp-core/src/main/resources/db/migration/core/V14__sequence_and_settings.sql`
- `erp-core/src/main/java/com/erp/sequence/`:
  - `entity/NumberSeries`
  - `domain/NumberSeriesDomain`, `domain/NumberPattern`, `domain/ResetPolicy`
  - `repository/NumberSeriesRepository`
  - `dto/NumberSeries{Create,Update}Request`, `dto/NumberSeriesResponse`, `dto/NumberSeriesSearchRequest`
  - `mapper/NumberSeriesMapper`
  - `service/NumberSeriesService` (admin), `service/NumberAllocationService` (`REQUIRES_NEW`)
  - `controller/NumberSeriesController`
  - `permission/SequencePermissions`
  - `exception/SequenceErrorCodes`
  - `crossmodule/NumberSeriesApi`, `crossmodule/NumberSeriesApiImpl`
  - `tenant/SequenceTenantProvisioningContributor`
- `erp-core/src/main/java/com/erp/cu/`:
  - `crossmodule/SettingsApi`, `crossmodule/SettingsApiImpl`, `crossmodule/NoSuchSettingException`
  - `domain/SettingScope`, `domain/SettingValueConverter`
- `erp-core/src/main/java/com/erp/autoconfigure/ErpCoreCacheAutoConfiguration`
- Tests:
  - `sequence/domain/NumberSeriesDomainTest` (25)
  - `sequence/NumberSeriesConcurrencyIntegrationTest` (1)
  - `sequence/NumberSeriesIntegrationTest` (6)
  - `sequence/NumberSeriesApiIntegrationTest` (7)
  - `cu/domain/SettingValueConverterTest` (19)
  - `cu/domain/AppConfigurationDomainScopeTest` (2)
  - `cu/SettingsCacheTest` (4)
  - `cu/SettingsIntegrationTest` (6)
  - `cu/ConfigurationScopeApiIntegrationTest` (3)
  - `autoconfigure/ErpCoreCacheAutoConfigurationTest` (3)
  - `testsupport/StaffApiClient` (helper)
- `docs/steps/09-report.md`

**Modified**
- `cu/`:
  - `entity/AppConfiguration`: global entity with a nullable `tenantId`; the `@UniqueConstraint` is removed because the index is an expression.
  - `repository/AppConfigurationRepository`: owner-explicit finders, `findOverrideAndDefault` and `ownedBy`.
  - `service/ConfigurationService`: scope on every operation, `resolve` with the cache, eviction, and the unused `getValue` removed.
  - `controller/ConfigurationController`: the `scope` parameter.
  - `mapper/ConfigurationMapper`: the owner on `toEntity`, and `scope` in the response.
  - `dto/ConfigurationResponse`: `scope`.
  - `domain/AppConfigurationDomain`: the scope rule and resolution.
  - `exception/CuErrorCodes`: 3 codes.
  - `permission/CuPermissions`: `PLATFORM_SETTINGS` screen, `PERM_PLATFORM_SETTINGS_VIEW` and `PLATFORM_SETTINGS_MANAGE`.
- `autoconfigure/ErpCoreAutoConfiguration`: `com.erp.sequence` appended to `CORE_PACKAGE_LIST`.
- `META-INF/spring/...AutoConfiguration.imports`: `ErpCoreCacheAutoConfiguration` appended.
- `i18n/messages.properties` and `messages_ar.properties`: the step-09 block (7 codes).
- `db/migration/core/README.md`: V14 row.
- Tests:
  - `architecture/CrossModuleBoundaryArchTest`: the sequence module and the acceptance rule.
  - `autoconfigure/ErpCoreAutoConfigurationTest`: package list.
  - `sec/PermissionCatalogIntegrationTest`: excludes non-seeded step-09 rows from the seed check.
  - `tenant/TenantSchemaIntegrationTest`: counts and the nullable exception.
  - `erp-app-reference/.../ReferenceApplicationSmokeTest`: Flyway `2..11, 14, 1000`.
- `docs/DEVIATIONS.md`: 14 `[09]` entries.

**Deleted**: none (one unused method, `ConfigurationService.getValue`, was removed).

**Untouched**: `erp-core-plan/execution-state.json`, `erp-app-reference/governance/`, and the V12/V13 migration versions.

## Decisions & deviations

These mirror the 14 `[09]` entries in `docs/DEVIATIONS.md`:

1. **Permission format.** `PERM_SEQUENCE_SERIES_MANAGE`, plus the `PERM_SEQUENCE_SERIES_VIEW` gateway, which gates reads.
2. **`PLATFORM_SETTINGS_MANAGE`.**
   - It sits under the PLATFORM module, so it is effective only in the PLATFORM tenant.
   - It gates all `scope=PLATFORM` operations, reads included.
   - The extra 403 code `SETTING_PLATFORM_SCOPE_FORBIDDEN` is the tenant check.
3. **Series row model.**
   - An anchor lock comes first, and the period row is locked or created next.
   - Code, policy and counter are immutable through the API.
   - Update and (de)activation apply to every row of a code.
   - There is no delete or usage endpoint.
   - The default policy is YEARLY, and `PREFIX` is nullable.
4. **Extra pattern validation.** Exactly one `{SEQ:n}`, 1 ≤ n ≤ 18, balanced braces, and the period must be present in the pattern under YEARLY/MONTHLY.
5. **Inactive series.** An inactive series is reported as not configured, with 422. There are 2 extra admin codes.
6. **Date source.** The `Clock` bean when one exists, otherwise the JVM zone.
7. **Tenant code.** Step 07's `TenantLookupApi` in `tenant.crossmodule` provides it for `{TENANT}`.
8. **Provisioning.** The sequence provisioning contributor copies PLATFORM's anchor rows, with the counter set to 1.
9. **No `@PreAuthorize` on `NumberSeriesApi` / `resolve`.** Both are in-process library calls. `resolve` returns a raw `String`.
10. **Existing CU rows.** They keep their tenant. Inactive rows count as absent. The default scope `TENANT` keeps the old behaviour. `getValue` was removed.
11. **CU search.** It uses `SpecBuilder` combined with a hand-written `ownedBy` Specification.
12. **Caching.**
    - `@EnableCaching(order = LOWEST_PRECEDENCE - 1)`, so the cache is evicted after commit.
    - The customizer keeps an on-demand manager dynamic.
    - Eviction is `allEntries`.
    - A missing value is cached as `null`.
13. **Typed reads.** The strict rules for Boolean and Duration. A default never hides a mismatch. Settings stay in `com.erp.cu`.
14. **Test adaptations.** The adaptations listed above, plus `StaffApiClient`.

## Acceptance checklist

2/2 ✅

- ✅ **Tests green; concurrency test stable across 3 consecutive runs.**
  - `mvn -q verify` EXIT=0: erp-core 215/0/0/0, erp-app-reference 7/0/0/0.
  - `NumberSeriesConcurrencyIntegrationTest`: 50 threads released together by a latch call `next("T")`. The result is exactly `T-0001..T-0050`, `NEXT_VALUE` = 51, and there is one row. It passed in 3 separate Maven runs (output below).
  - Every task-6 test is present:
    - YEARLY reset creates a new period row: `yearlyReset_createsANewPeriodRow_...`.
    - Pattern table test: `NumberSeriesDomainTest.render_table` (8 rows) plus 12 invalid patterns.
    - Tenants A and B each start at 1: `tenantsAandB_eachStartAtOne_forTheSameCode`.
    - Resolution order, override beats default: `SettingsIntegrationTest.tenantOverride_beatsThePlatformDefault_...`.
    - Cache hit, repository mock invoked once: `SettingsCacheTest.aRepeatedRead_hitsTheRepositoryOnce_...`.
    - Eviction on update: `SettingsCacheTest.create_update_andDeactivate_evictTheCache`, and `SettingsIntegrationTest.aCachedValue_survivesAChangeBehindTheApi_untilACrudWriteEvictsIt` (through HTTP).
    - Type conversion errors: `SettingValueConverterTest` and `SettingsIntegrationTest.typedReads_andTypeMismatch`.
- ✅ **`NumberSeriesApi` and `SettingsApi` are in `crossmodule` packages and allowed by the ArchUnit rules.**
  - `com.erp.sequence.crossmodule.NumberSeriesApi` and `com.erp.cu.crossmodule.SettingsApi` exist.
  - `CrossModuleBoundaryArchTest` (5/5) now also bounds `com.erp.sequence`, whose public surface is `com.erp.sequence.crossmodule`.
  - The new rule `step09_apis_are_crossmodule_interfaces_allowed_by_the_boundary_rule` asserts that both are interfaces on their module's public surface.
  - The sequence module's own cross-module use (`tenant.crossmodule.TenantLookupApi`) passes the boundary rule.

The step 05 rule against raw SQL without a tenant still holds:
- `nativeQuery`, `createNativeQuery`: none.
- `JdbcTemplate` appears only in the four provisioning contributors. In the new one, `TENANT_ID` is in the insert and in both predicates.
- The allocation SQL carries `tenant_id = ?`, as shown in the log below.

## Verification output

```
$ java -version → openjdk 21.0.7
$ rm -rf target erp-core/target erp-app-reference/target; mvn -q -o verify
EXIT=0 secs=176        [TestPostgres] backend: EMBEDDED
(first attempt EXIT=1: BootstrapAdminPasswordIntegrationTest counted two 'admin' users across tenants —
 the new tests had provisioned tenants whose first administrator was named 'admin'; StaffApiClient now
 uses 'tenant-admin'. No assertion changed.)

surefire totals                                                   tests fail err skip
== erp-core
com.erp.architecture.CrossModuleBoundaryArchTest                 5  0  0  0   (+1)
com.erp.autoconfigure.DownloadTokenStoreAutoConfigurationTest    2  0  0  0
com.erp.autoconfigure.ErpCoreAutoConfigurationTest              10  0  0  0
com.erp.autoconfigure.ErpCoreCacheAutoConfigurationTest          3  0  0  0   (new)
com.erp.autoconfigure.ErpCoreFlywayAutoConfigurationTest         3  0  0  0
com.erp.autoconfigure.MigrationNamingTest                        2  0  0  0
com.erp.cu.ConfigurationScopeApiIntegrationTest                  3  0  0  0   (new)
com.erp.cu.SettingsCacheTest                                     4  0  0  0   (new)
com.erp.cu.SettingsIntegrationTest                               6  0  0  0   (new)
com.erp.cu.domain.AppConfigurationDomainScopeTest                2  0  0  0   (new)
com.erp.cu.domain.SettingValueConverterTest                     19  0  0  0   (new)
com.erp.file.service.InMemoryDownloadTokenStoreTest              3  0  0  0
com.erp.notif.service.DefaultChannelProviderTest                 2  0  0  0
com.erp.sec.BootstrapAdminPasswordIntegrationTest                4  0  0  0
com.erp.sec.CustomerRealmIntegrationTest                         9  0  0  0
com.erp.sec.MenuServiceGatewayIntegrationTest                    2  0  0  0
com.erp.sec.PermissionCatalogIntegrationTest                     4  0  0  0
com.erp.sec.SecCoverageIntegrationTest                           6  0  0  0
com.erp.sec.SecFrontendGapIntegrationTest                        5  0  0  0
com.erp.sec.SecLogoutIntegrationTest                             3  0  0  0
com.erp.sec.SecReadOneIntegrationTest                            6  0  0  0
com.erp.sec.SecSearchFilterIntegrationTest                      10  0  0  0
com.erp.sec.TenantScopedQueryIntegrationTest                     3  0  0  0
com.erp.sec.UserRolesInResponseIntegrationTest                   8  0  0  0
com.erp.sec.domain.CustomerDomainRulesTest                       4  0  0  0
com.erp.sec.permission.PermissionDefTest                         4  0  0  0
com.erp.sec.security.LoginRateLimiterTest                        3  0  0  0
com.erp.sequence.NumberSeriesApiIntegrationTest                  7  0  0  0   (new)
com.erp.sequence.NumberSeriesConcurrencyIntegrationTest          1  0  0  0   (new)
com.erp.sequence.NumberSeriesIntegrationTest                     6  0  0  0   (new)
com.erp.sequence.domain.NumberSeriesDomainTest                  25  0  0  0   (new)
com.erp.tenant.PlatformTenantApiIntegrationTest                  8  0  0  0
com.erp.tenant.TenantContextIntegrationTest                      1  0  0  0
com.erp.tenant.TenantContextTest                                 4  0  0  0
com.erp.tenant.TenantIsolationIntegrationTest                    6  0  0  0
com.erp.tenant.TenantSchemaIntegrationTest                       4  0  0  0
com.erp.tenant.domain.TenantDomainTest                          16  0  0  0
com.erp.testsupport.TestProfileWiringIntegrationTest             2  0  0  0
erp-core tests=215 failures=0 errors=0 skipped=0
== erp-app-reference
com.erp.app.ReferenceApplicationSmokeTest                        7  0  0  0   (Flyway 2..11, 14, 1000)
erp-app-reference tests=7 failures=0 errors=0 skipped=0

$ for i in 1 2 3; do mvn -o -pl erp-core test -Dtest=NumberSeriesConcurrencyIntegrationTest; done
run 1: Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 28.53 s -- in com.erp.sequence.NumberSeriesConcurrencyIntegrationTest  BUILD SUCCESS
run 2: Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 30.65 s -- in com.erp.sequence.NumberSeriesConcurrencyIntegrationTest  BUILD SUCCESS
run 3: Tests run: 1, Failures: 0, Errors: 0, Skipped: 0, Time elapsed: 30.53 s -- in com.erp.sequence.NumberSeriesConcurrencyIntegrationTest  BUILD SUCCESS
(elapsed includes the Spring context start)

$ (concurrency test with -Dlogging.level.org.hibernate.SQL=DEBUG) SQL on core_number_series, counted:
  50 select ... from core_number_series ns1_0 where ns1_0.tenant_id = ? and ns1_0.code=? order by ns1_0.id
     fetch first ? rows only for no key update of ns1_0
  50 update core_number_series set ...,next_value=?,... where id=? and version=?
   1 insert into core_number_series (...) ; 1 select nextval('seq_core_number_series')

$ grep -rn "nativeQuery = true\|JdbcTemplate\|createNativeQuery" erp-core/src/main/java  (non-import lines)
mdl/tenant/MdlTenantProvisioningContributor.java:24, notif/tenant/NotifTenantProvisioningContributor.java:25,
sec/tenant/SecTenantProvisioningContributor.java:57, sequence/tenant/SequenceTenantProvisioningContributor.java:25
  → provisioning contributors only; the new one names TENANT_ID in its insert and both predicates.
```

## Skills checked

- **`.claude/skills/build-create-entity`**
  - `NumberSeries` complies:
    - `AuditableEntity`, `@SuperBuilder`, `SEQ_CORE_NUMBER_SERIES` with `allocationSize = 1`;
    - `UQ_CORE_NUMBER_SERIES_CODE_PERIOD` in `@Table`;
    - native `BOOLEAN` without a converter;
    - normalization only in `@PrePersist`/`@PreUpdate`;
    - `activate()`/`deactivate()`.
  - Named deviation: the PK is `ID` (README §4). `takeNextValue()` is a pure field mutation.
  - `NumberSeriesDomain` is a plain class built with `create`/`from` that throws `LocalizedException`.
  - `AppConfiguration` extends `GlobalAuditableEntity`, as the step file names it. Its `@UniqueConstraint` was dropped because the index is an expression; this is a named deviation from A.1.12.
- **`build-create-repository`**: compliant.
  - `JpaRepository` + `JpaSpecificationExecutor` and `@Repository`.
  - `existsByCode` / `existsByTenantId...ConfigKey`.
  - There is no `...AndIdNot`, because the keys are immutable.
  - Every method has a service caller.
  - Named deviation: the `ownedBy` Specification (SH.1).
- **`build-create-dto`**: compliant (bilingual `@Schema`, i18n keys, UTC `@JsonFormat`, Update without immutable fields). Named deviation: no Usage DTO, since nothing references a series.
- **`build-create-mapper`**: compliant. Mapping is manual and null-safe, `update` is a void in-place update, and there is no normalization. The child-style `toNewPeriod(anchor, period)` takes its source entity as a parameter.
- **`build-create-service`**: `NumberSeriesService` and `ConfigurationService` comply:
  - `@PreAuthorize` with the module's own constants;
  - `@Transactional` / `readOnly`;
  - `ServiceResult`;
  - `SpecBuilder` / `PageableBuilder` with whitelists;
  - rules delegated to the Domain objects;
  - `info` logging for writes and `debug` for reads.
  - Named deviations: `NumberAllocationService` and `resolve` have no `@PreAuthorize` and return raw values (internal library calls).
  - Cross-module access goes only through `tenant.crossmodule`.
  - `REQUIRES_NEW` is stated and justified at the allocation.
- **`build-create-controller`**: compliant: thin controllers, `OperationCode`, `@Valid`, `@Operation`, `POST /search`, separate activate/deactivate. Named deviations: no delete and no usage endpoint for series. The CU controller only gained the `scope` parameter.
- **`gov-enforce-error-handling`**: compliant.
  - The 7 new codes are registered in `SequenceErrorCodes` / `CuErrorCodes` (both classes have a private constructor that throws).
  - Each code has an EN and an AR message, with `''{0}''` arguments.
  - Statuses: not-found → `NOT_FOUND`; duplicate → `ALREADY_EXISTS`; invalid pattern → `VALIDATION_ERROR`; not configured and type mismatch → `BUSINESS_RULE_VIOLATION`; platform scope → `FORBIDDEN`.
  - No raw runtime exception is thrown, and no `DataIntegrityViolationException` is caught.
- **`gov-enforce-caching-rules`**: compliant.
  - Register: `AppConfiguration` → `erpCoreSettings`, by the step-09 plan's decision.
  - `@Cacheable` sits only on the service read `resolve`, which is not a search, in the order `@Cacheable` → `@Transactional(readOnly)`.
  - `@CacheEvict(allEntries = true)` comes first on `create`, `update` and `deactivate`, the entity's only writes (D.3.7, D.4.1–4).
  - The same cache name is used everywhere through a constant.
  - There are no direct cache-client calls in services.
  - The key includes the tenant.
  - `NumberSeries` is transactional and is not cached.
- **`gov-enforce-backend-contract`** and **`gov-validate-backend-feature`** (the number-series feature and the settings feature): no automatic-rejection trigger. Points are lost only on the named deviations above:
  - PK `ID`;
  - no delete, usage or Usage DTO;
  - internal ungated library methods;
  - the `ownedBy` Specification;
  - one VIEW/MANAGE authority pair instead of VIEW/CREATE/UPDATE/DELETE, per the step file.

## Notes for later steps

- **Migrations.** V2..V14 are taken; the chain is contiguous after the rebase. `TenantSchemaIntegrationTest` now expects 21 tenant columns (`cu_app_configuration` nullable), 14 uniques and 20 tenant entities, with `AppConfiguration` global. Steps 10/11 add theirs on top.
- **Numbers.** Inject `com.erp.sequence.crossmodule.NumberSeriesApi`.
  - It numbers the current tenant.
  - Seed series in the app's `V1000+` migration for PLATFORM, and new tenants get copies.
  - Inside a caller transaction, `next()` takes a second connection because of `REQUIRES_NEW`. Size connection pools accordingly.
  - Per-branch series are composed codes (`SALES_INVOICE_BR01`).
- **Settings.** Inject `com.erp.cu.crossmodule.SettingsApi`.
  - It needs a tenant context (`TenantContext.runAs` in jobs).
  - Write through the CU CRUD (or `ConfigurationService`), never with SQL, or the cache goes stale until the next CRUD write.
  - Platform defaults: `POST /api/v1/common/configurations?scope=PLATFORM` from the PLATFORM tenant.
- **`CU_APP_CONFIGURATION` is not tenant-filtered by Hibernate.** Any new query on it must name the owner (`tenantId` or `tenantId IS NULL`).
- **Caching.** Core now has `@EnableCaching` (`order = LOWEST_PRECEDENCE - 1`). A later cache must go on the register (step-file decision) and include the tenant in its key. On an on-demand simple manager the core never fixes the cache names.
- **Tests.** `com.erp.testsupport.StaffApiClient` gives any module's tests a PLATFORM super-admin token, tenant provisioning (admin `tenant-admin`) and HTTP helpers. Never name a provisioned administrator `admin`: `BootstrapAdminPasswordIntegrationTest` looks `admin` up across tenants.
- **Shared files touched:**
  - `ErpCoreAutoConfiguration.CORE_PACKAGE_LIST`
  - `AutoConfiguration.imports`
  - the i18n bundles (own block at the end)
  - `db/migration/core/README.md` (V14 row)
  - `docs/DEVIATIONS.md`
  - `CrossModuleBoundaryArchTest` (MODULES entry and one new rule)
  - `TenantSchemaIntegrationTest`, `PermissionCatalogIntegrationTest`, `ErpCoreAutoConfigurationTest`, `ReferenceApplicationSmokeTest`
  - `CuPermissions` (PLATFORM screen)
