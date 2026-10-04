# Step 05 — Multi-tenancy (row-level, shared schema) — report

## Summary

Every core row now belongs to a tenant, and Hibernate filters every query by the current tenant.

- **Discriminator.** `AuditableEntity` (name kept, now tenant-aware) carries
  `@TenantId @Column(name = "TENANT_ID", nullable = false, updatable = false) Long tenantId`.
  Hibernate 7.2.0.Final then restricts every HQL/criteria query, every join and every load by id
  (its `_tenantId` filter is `applyToLoadByKey`), and it sets the column on insert.
- **Optimistic lock.** `@Version VERSION` lives on the new `GlobalAuditableEntity`, which holds the
  audit columns and the version without a tenant. `AuditableEntity` extends it.
- **Global entities.** The three `Sec*Reg` entities and the new `Tenant` extend
  `GlobalAuditableEntity` directly. The 8 SEC entities that had no base class now extend
  `AuditableEntity`.
- **`com.erp.tenant`** (added to `CORE_PACKAGE_LIST`) contains:
  - `TenantContext`: a ThreadLocal with `current/find/require/set/clear/runAs/callAs`;
  - `TenantConstants`;
  - the provisioning SPI (`TenantProvisioning`, `TenantProvisioningContributor`);
  - `TenantIdentifierResolver` (`CurrentTenantIdentifierResolver<Long>`), registered through a
    `HibernatePropertiesCustomizer`;
  - the `Tenant` entity (`CORE_TENANT`), `TenantDomain`, repository, DTOs, mapper, `TenantService`,
    `PlatformTenantController` and `TenantResolutionFilter`.
- **Tenant resolution on a web request.**
  1. `JwtAuthenticationFilter` makes the token's `tid` claim the request tenant before it loads the
     user. Usernames are unique per tenant only.
  2. `TenantResolutionFilter` comes right after it. It falls back to the `X-Tenant-Code` header
     (unknown code → 404, suspended tenant → 403). It also refuses the tokens of a suspended tenant.
  3. A public, non-exempt path without a tenant gets 400 `TENANT_REQUIRED`.
  4. `/api/v1/platform/**` requires an authenticated PLATFORM-tenant caller holding
     `PLATFORM_TENANT_MANAGE`.
- **Provisioning.** `POST /api/v1/platform/tenants` creates the tenant and, in the same
  transaction, lets SEC, MDL and NOTIF contributors copy the PLATFORM reference catalog:
  - roles and grants, except the platform module;
  - lookups;
  - channel configurations and templates.

  The tenant's first administrator is created from the request.
- **`V10__tenant_schema.sql`** does the following:
  - creates `CORE_TENANT` and seeds PLATFORM (ID 1);
  - adds `TENANT_ID` (backfilled, then `DROP DEFAULT`, FK, index) and `VERSION` to all 18
    tenant-scoped tables;
  - adds `VERSION` to the catalog;
  - adds audit columns to the 8 SEC tables that lacked them;
  - makes 13 unique constraints composite;
  - seeds the `PLATFORM` module and the `PLATFORM_TENANTS` screen with `PERM_PLATFORM_TENANTS_VIEW`
    and `PLATFORM_TENANT_MANAGE`, granted to PLATFORM's `SYS_ADMIN`.
- **The bootstrap admin runner** works under `TenantContext.runAs(PLATFORM)`.

`mvn -q verify` is green: erp-core 111 tests (69 pre-existing, unchanged in their assertions, plus
42 new), erp-app-reference 7 tests (5 adapted, plus 2 new).

### Hibernate 7.2 API actually used (checked with `javap` against hibernate-core-7.2.0.Final)

- **`CurrentTenantIdentifierResolver<T>`** has `T resolveCurrentTenantIdentifier()`,
  `boolean validateExistingCurrentSessions()` and `default boolean isRoot(T)`. Used as
  `<Long>`, with `validateExistingCurrentSessions() = false` and `isRoot` left at its default
  `false`, so no tenant sees other tenants' rows, PLATFORM included.
- **The session builder resolves the tenant eagerly** when a session opens
  (`AbstractCommonBuilder` → `SessionFactoryImplementor.resolveTenantIdentifier()`), and it throws
  "SessionFactory configured for multi-tenancy, but no tenant identifier specified" on null.
  Spring Data opens EntityManagers during repository bootstrap, so the resolver tolerates a missing
  tenant (sentinel `-1`, matching no row) only until `ContextRefreshedEvent`. After that it is
  `TenantContext.require()`, which throws `TENANT_CONTEXT_MISSING`.
- **`TenantIdBinder`** defines the filter `_tenantId` with `autoEnabled = false` and
  `applyToLoadByKey = true`. `AbstractSharedSessionContract.setUpMultitenancy` enables it per session
  unless `isRoot`.
- **Registration.** Boot 4.0.1's `HibernateJpaConfiguration` does not pick up a resolver bean, so it
  is put into `hibernate.tenant_identifier_resolver`
  (`MultiTenancySettings.MULTI_TENANT_IDENTIFIER_RESOLVER`) by a `HibernatePropertiesCustomizer`.

## Files changed

**Created**
- `erp-core/src/main/resources/db/migration/core/V10__tenant_schema.sql`
- `erp-core/src/main/java/com/erp/common/domain/GlobalAuditableEntity.java`
- `erp-core/src/main/java/com/erp/tenant/`:
  - `TenantContext.java`, `TenantConstants.java`, `TenantProvisioning.java`, `TenantProvisioningContributor.java`
  - `config/TenantIdentifierResolver.java`, `config/TenantHibernateConfiguration.java`
  - `entity/Tenant.java`, `domain/TenantDomain.java`, `repository/TenantRepository.java`
  - `dto/TenantCreateRequest.java`, `dto/TenantResponse.java`, `dto/TenantSearchRequest.java`, `dto/TenantStatusUpdateRequest.java`
  - `mapper/TenantMapper.java`, `exception/TenantErrorCodes.java`, `service/TenantService.java`
  - `controller/PlatformTenantController.java`, `security/TenantResolutionFilter.java`
- The three provisioning contributors:
  - `erp-core/src/main/java/com/erp/sec/tenant/SecTenantProvisioningContributor.java`
  - `erp-core/src/main/java/com/erp/mdl/tenant/MdlTenantProvisioningContributor.java`
  - `erp-core/src/main/java/com/erp/notif/tenant/NotifTenantProvisioningContributor.java`
- Tests:
  - `erp-core/src/test/java/com/erp/tenant/PlatformTenantApiIntegrationTest.java` (8)
  - `TenantIsolationIntegrationTest.java` (6)
  - `TenantContextIntegrationTest.java` (1)
  - `TenantSchemaIntegrationTest.java` (4)
  - `TenantContextTest.java` (4)
  - `domain/TenantDomainTest.java` (16)
  - `TenantHttp.java` (helper)
- Test support and the SEC-side test:
  - `erp-core/src/test/java/com/erp/sec/TenantScopedQueryIntegrationTest.java` (3)
  - `erp-core/src/test/java/com/erp/testsupport/TenantContextTestExecutionListener.java`
- `docs/steps/05-report.md`

**Modified**
- `common/domain/AuditableEntity.java`: now extends `GlobalAuditableEntity` and adds `@TenantId tenantId`.
- `common/audit/AuditEntityListener.java`: typed on `GlobalAuditableEntity`.
- `common/exception/CommonErrorCodes.java` and `common/web/GlobalExceptionHandler.java`: `CONCURRENT_MODIFICATION`, optimistic lock → 409.
- `autoconfigure/ErpCoreAutoConfiguration.java`: `com.erp.tenant` appended to `CORE_PACKAGE_LIST`.
- `autoconfigure/ErpCoreProperties.java`: `erp.core.tenant.exempt-paths`.
- `autoconfigure/ErpCoreSecurityAutoConfiguration.java`:
  - `TenantResolutionFilter` registered after the JWT filter;
  - the `/api/v1/platform/**` rule;
  - `PLATFORM_PATHS` / `PLATFORM_TENANT_MANAGE_AUTHORITY`.
- `sec/security/JwtTokenIssuer.java`: `tid` claim.
- `sec/security/JwtAuthenticationFilter.java`: sets the tenant from `tid` before the user lookup, and restores the previous tenant in a finally.
- `sec/security/BootstrapAdminPasswordRunner.java`: `runAs(PLATFORM)`, no longer `@Transactional`.
- `sec/permission/PermissionConstants.java`: `PLATFORM_TENANT_MANAGE`.
- `sec/entity`:
  - `ActiveSession`, `AuditLogEntry`, `PasswordResetToken`, `RoleActionGrant`, `RoleModuleGrant`, `RoleScreenGrant`, `SignupRequest`, `UserRoleAssignment` → extend `AuditableEntity`;
  - `ModuleRegistry`, `ScreenRegistry`, `ActionRegistry` → extend `GlobalAuditableEntity`;
  - `User`, `Role` → composite `@UniqueConstraint`.
- Composite `@UniqueConstraint` (`TENANT_ID` first, names unchanged): `cu/entity/AppConfiguration`, `file/entity/FileCategory`, `mdl/entity/LookupType`, `mdl/entity/LookupValue`, `notif/entity/NotificationChannelConfig`, `notif/entity/NotificationTemplate`.
- `resources/i18n/messages.properties` and `messages_ar.properties`: 7 `TENANT_*` codes plus `CONCURRENT_MODIFICATION`.
- `resources/db/migration/core/README.md`: V10 row, the sanctioned exceptions, the tenant-column rules for new tables.
- `test/.../testsupport/AbstractIntegrationTest.java`: registers `TenantContextTestExecutionListener`.
- `test/.../architecture/CrossModuleBoundaryArchTest.java`: tenant module bounded, with its root package public.
- `test/.../autoconfigure/ErpCoreAutoConfigurationTest.java`: `CORE_PACKAGES` contains `com.erp.tenant`.
- `erp-app-reference/src/test/java/com/erp/app/ReferenceApplicationSmokeTest.java`:
  - `X-Tenant-Code: PLATFORM` on login;
  - Flyway `2..10, 1000`;
  - 2 new tests.
- `docs/DEVIATIONS.md`: 15 `[05]` entries.

**Deleted**: none.

**Untouched**: `erp-core-plan/execution-state.json`, `erp-app-reference/governance/`,
`erp-app-reference/src/main/resources/application.yml` (task 11: "adds nothing").

### Adaptations of existing tests, per class (no assertion weakened)
- **All `com.erp.sec.*IntegrationTest`, `BootstrapAdminPasswordIntegrationTest` and `TestProfileWiringIntegrationTest`:** no edit. Through the new `TenantContextTestExecutionListener` (in `AbstractIntegrationTest`), each test method runs as the PLATFORM tenant, which owns every seeded row. The tenant is set before the test transaction opens its session.
- **`SecLogoutIntegrationTest`:** no edit. It runs `JwtAuthenticationFilter` on the test thread, which already carries a tenant. So the filter restores the previous tenant instead of clearing it, and it no longer skips when a tenant is present.
- **`BootstrapAdminPasswordIntegrationTest`:** no edit. The runner's constructor is unchanged, and `runAs(PLATFORM)` nests inside the test's PLATFORM context and transaction.
- **`ErpCoreAutoConfigurationTest`:** `corePackages_areTheSixCoreModules` → `corePackages_areTheSevenCoreModules`, which expects `com.erp.tenant` too.
- **`CrossModuleBoundaryArchTest`:** the tenant module was added to `MODULES`. All existing rules are unchanged.
- **`ReferenceApplicationSmokeTest`:**
  - the login helper sends `X-Tenant-Code: PLATFORM`;
  - the Flyway assertion is now `2..10, 1000`;
  - `protectedEndpointsNeedAToken` (401) and `admin/admin` → 401 are unchanged.

## Decisions & deviations

These mirror the 15 `[05]` entries in `docs/DEVIATIONS.md`:

1. **Resolver strictness.** `require()` applies after context refresh. During bootstrap the sentinel tenant `-1` is used, because Spring Data opens sessions while building repository queries. The resolver API is listed above.
2. **TENANT_REQUIRED** (400) is enforced on public, non-exempt paths. A protected path without a token still answers 401.
3. **`TenantResolutionFilter` details.**
   - It is a plain filter in the core chain after the JWT filter, not a bean.
   - The header is trimmed and upper-cased; an unknown code → 404.
   - The tokens of a suspended tenant → 403.
   - `CORE_TENANT` lookups run as PLATFORM.
   - New property `erp.core.tenant.exempt-paths`.
4. **Extra error codes:** `TENANT_CODE_DUPLICATE` (409), `TENANT_PLATFORM_PROTECTED` (422), and `CONCURRENT_MODIFICATION` (409 optimistic-lock handler).
5. **Permission seed.** Module `PLATFORM`, screen `PLATFORM_TENANTS`, and a gateway `PERM_PLATFORM_TENANTS_VIEW` next to `PLATFORM_TENANT_MANAGE`. RULE-SEC-007 needs the screen's VIEW, otherwise the authority never reaches the token. The platform path also requires request tenant = PLATFORM.
6. **First administrator of a new tenant:**
   - the fields come from the create request;
   - the SPI `TenantProvisioningContributor` is implemented by SEC, MDL and NOTIF;
   - it uses JDBC with explicit `TENANT_ID`, in the creating transaction, so it is atomic;
   - it copies PLATFORM's catalog roles and grants without the `PLATFORM` module, the lookups, the channels (without `CONFIG_JSON`) and the templates (without attachment).
7. **No login rate limiter exists**, so there is nothing to key by `tenantCode:username`, and none was added.
8. **System jobs.** The only one is `BootstrapAdminPasswordRunner`, which uses `runAs(PLATFORM)` and is no longer `@Transactional`.
9. **Step 04's convention exceptions.**
   - Resolved: the audit columns on the 8 SEC tables.
   - Kept, since changing them is not additive: the `*_pk` names and the audit-column type differences.
10. **The two plan-sanctioned non-additive changes in V10:** `DROP DEFAULT`, and composite uniques under the same names. The composite form also applies to the 4 link tables.
11. **`GlobalAuditableEntity` is the superclass of `AuditableEntity`**, and the listener is typed on it.
12. **Controller shape.**
    - Endpoints: `POST`, `GET` (list), `GET /{id}`, `POST /search`, `PATCH /{id}/status`.
    - There is no PUT, DELETE or usage endpoint, and no Update or Usage DTO.
    - `activate()`/`suspend()` replace `activate()`/`deactivate()`.
    - Codes are validated as given.
13. **The isolation test uses `POST /api/v1/sec/users/search`**, because no GET list exists. The SpecBuilder regression test lives in `com.erp.sec`, because of the ArchUnit test boundary.
14. **ArchUnit.** The tenant module's public surface is its root package plus `crossmodule`.
15. **Existing test adaptations**, as listed above.

## Acceptance checklist

4/4 ✅

- ✅ **All tests green including the isolation tests.**
  - `mvn -q verify` EXIT=0: erp-core tests=111, failures=0, errors=0. erp-app-reference tests=7, failures=0, errors=0.
  - `TenantIsolationIntegrationTest` 6/6. As tenant A, `POST /api/v1/sec/users/search` returns exactly A's `admin` and `alice`, and none of B's ids, even with a header `X-Tenant-Code` of B.
  - As A, `GET /api/v1/sec/users/{idOfB}` → 404, while B gets 200.
  - Login without `X-Tenant-Code` → 400 `TENANT_REQUIRED`.
  - Suspended tenant login → 403 `TENANT_SUSPENDED` (`PlatformTenantApiIntegrationTest`).
  - `runAs` outside a request: `TenantContextIntegrationTest` (plain thread; without `runAs` → `TENANT_CONTEXT_MISSING`).
  - SpecBuilder regression: `TenantScopedQueryIntegrationTest`.
- ✅ **`grep -rn "extends AuditableEntity" erp-core/src/main` covers every `@Entity` except the three `Sec*Reg` and `Tenant`.**
  - 18 entities extend `AuditableEntity`. `ActionRegistry`, `ModuleRegistry`, `ScreenRegistry` and `Tenant` extend `GlobalAuditableEntity`. That is 22 `@Entity` classes in all; see the listing below.
  - The same is asserted at runtime over the JPA metamodel (`TenantSchemaIntegrationTest.everyEntity_extendsAuditableEntity_exceptTheFourGlobalOnes`).
- ✅ **Every scoped table has `TENANT_ID NOT NULL` without default (`information_schema.columns` test).**
  - `TenantSchemaIntegrationTest.everyScopedTable_hasTenantIdNotNull_withoutDefault`: 18 `tenant_id` columns, all `is_nullable = NO`, `column_default IS NULL`, `bigint`.
  - The base tables without `tenant_id` are exactly `core_tenant`, `sec_module_reg`, `sec_screen_reg`, `sec_action_reg` and `flyway_schema_history`.
  - Also asserted: an FK to `core_tenant` and a `(tenant_id)` index on each, and all 13 unique constraints contain `tenant_id`.
- ✅ **No raw-SQL query path lacking `tenant_id`.**
  - `nativeQuery = true`: none.
  - `JdbcTemplate`: only the three provisioning contributors (`com.erp.{sec,mdl,notif}.tenant`), which are tenant/platform code reached only from the `PLATFORM_TENANT_MANAGE`-gated `TenantService.create`.
  - Every statement there names `TENANT_ID`: the new tenant on each insert, and `WHERE ... TENANT_ID = ?` (source tenant) on each read.
  - No `createNativeQuery` or `@Formula` exists.

## Verification output

```
$ java -version → openjdk version "21.0.7" 2025-04-15
$ rm -rf target erp-core/target erp-app-reference/target; mvn -q verify
EXIT=0 secs=138   (final run; an identical earlier run took 141 s)
[TestPostgres] integration-test database backend: EMBEDDED      (once per module JVM)

surefire totals (target/surefire-reports/TEST-*.xml)            tests fail err skip
== erp-core
com.erp.architecture.CrossModuleBoundaryArchTest                 2 0 0 0
com.erp.autoconfigure.DownloadTokenStoreAutoConfigurationTest    2 0 0 0
com.erp.autoconfigure.ErpCoreAutoConfigurationTest               9 0 0 0
com.erp.autoconfigure.ErpCoreFlywayAutoConfigurationTest         3 0 0 0
com.erp.autoconfigure.MigrationNamingTest                        2 0 0 0
com.erp.file.service.InMemoryDownloadTokenStoreTest              3 0 0 0
com.erp.notif.service.DefaultChannelProviderTest                 2 0 0 0
com.erp.sec.BootstrapAdminPasswordIntegrationTest                4 0 0 0
com.erp.sec.MenuServiceGatewayIntegrationTest                    2 0 0 0
com.erp.sec.SecCoverageIntegrationTest                           6 0 0 0
com.erp.sec.SecFrontendGapIntegrationTest                        5 0 0 0
com.erp.sec.SecLogoutIntegrationTest                             3 0 0 0
com.erp.sec.SecReadOneIntegrationTest                            6 0 0 0
com.erp.sec.SecSearchFilterIntegrationTest                      10 0 0 0
com.erp.sec.TenantScopedQueryIntegrationTest                     3 0 0 0   (new)
com.erp.sec.UserRolesInResponseIntegrationTest                   8 0 0 0
com.erp.tenant.PlatformTenantApiIntegrationTest                  8 0 0 0   (new)
com.erp.tenant.TenantContextIntegrationTest                      1 0 0 0   (new)
com.erp.tenant.TenantContextTest                                 4 0 0 0   (new)
com.erp.tenant.TenantIsolationIntegrationTest                    6 0 0 0   (new)
com.erp.tenant.TenantSchemaIntegrationTest                       4 0 0 0   (new)
com.erp.tenant.domain.TenantDomainTest                          16 0 0 0   (new)
com.erp.testsupport.TestProfileWiringIntegrationTest             2 0 0 0
erp-core tests=111 failures=0 errors=0 skipped=0
== erp-app-reference
com.erp.app.ReferenceApplicationSmokeTest                        7 0 0 0   (+2)
erp-app-reference tests=7 failures=0 errors=0 skipped=0

$ grep -rn "nativeQuery = true\|JdbcTemplate" erp-core/src/main || echo OK-no-raw-sql
erp-core/src/main/java/com/erp/mdl/tenant/MdlTenantProvisioningContributor.java:7:import org.springframework.jdbc.core.JdbcTemplate;
erp-core/src/main/java/com/erp/mdl/tenant/MdlTenantProvisioningContributor.java:24:    private final JdbcTemplate jdbcTemplate;
erp-core/src/main/java/com/erp/notif/tenant/NotifTenantProvisioningContributor.java:7:import org.springframework.jdbc.core.JdbcTemplate;
erp-core/src/main/java/com/erp/notif/tenant/NotifTenantProvisioningContributor.java:25:    private final JdbcTemplate jdbcTemplate;
erp-core/src/main/java/com/erp/sec/tenant/SecTenantProvisioningContributor.java:14:import org.springframework.jdbc.core.JdbcTemplate;
erp-core/src/main/java/com/erp/sec/tenant/SecTenantProvisioningContributor.java:50:    private final JdbcTemplate jdbcTemplate;
  → justification: tenant provisioning (platform code); every statement names TENANT_ID (insert value = new
    tenant, WHERE ... TENANT_ID = source tenant). No nativeQuery anywhere.
$ grep -rn "createNativeQuery\|@Formula\|NamedParameterJdbc\|getConnection\|DataSourceUtils" erp-core/src/main/java → none

$ grep -rn "extends AuditableEntity" erp-core/src/main      (18 entities; + one SQL comment line in V10)
cu/AppConfiguration  file/FileCategory  file/FileDocument  mdl/LookupType  mdl/LookupValue
notif/NotificationChannelConfig  notif/NotificationLog  notif/NotificationTemplate
sec/ActiveSession  sec/AuditLogEntry  sec/PasswordResetToken  sec/Role  sec/RoleActionGrant
sec/RoleModuleGrant  sec/RoleScreenGrant  sec/SignupRequest  sec/User  sec/UserRoleAssignment
$ every @Entity and its base class:
ActionRegistry→GlobalAuditableEntity  ModuleRegistry→GlobalAuditableEntity  ScreenRegistry→GlobalAuditableEntity
Tenant→GlobalAuditableEntity  (all 18 others above → AuditableEntity)

Scratch check of V2..V10 (native PostgreSQL 16.15, psql -1 -v ON_ERROR_STOP=1 per script, DB erp_step05_scratch):
all scripts applied; 18 tenant_id columns, all NOT NULL, no default; 60 SEC_ROLE_ACTION_GRANT rows in tenant 1
(59 seeded + PLATFORM_TENANT_MANAGE; before PERM_PLATFORM_TENANTS_VIEW was added).
$ dropdb erp_step05_scratch; select count(*) from pg_database where datname like 'erp_step05%' → 0

Failed attempts on the way (fixed):
- first erp-core start: QueryCreationException ... TENANT_CONTEXT_MISSING (Spring Data bootstrap sessions)
  → bootstrap leniency (deviation 1);
- SecLogoutIntegrationTest control failed (filter skipped on a thread that already had a tenant)
  → the filter restores the previous tenant;
- PlatformTenantApiIntegrationTest 7/8 failed with 403 SEC-403-FORBIDDEN (RULE-SEC-007 gateway)
  → PERM_PLATFORM_TENANTS_VIEW seeded (deviation 5).
```

## Skills checked

- `.claude/skills/build-create-entity/SKILL.md`: `Tenant` is compliant except two named deviations.
  - Compliant: `@SuperBuilder`, `SEQUENCE` with `SEQ_CORE_TENANT` / `allocationSize = 1`, `UQ_CORE_TENANT_CODE` declared in `@Table`, normalization in `@PrePersist`/`@PreUpdate` only.
  - Deviation: the PK column is `ID` (README §4 overrides the skill for new tables).
  - Deviation: it extends `GlobalAuditableEntity` because the step names it global, a declared exemption.
  - Its transitions are `activate()`/`suspend()` (deviation A.1.18).
  - `TenantDomain` is a plain class built with `create`/`from` that throws `LocalizedException`.
  - The skill line "NEVER add a tenant column unless the project is actually multi-tenant" now applies: the project is multi-tenant.
- `build-create-repository`: compliant.
  - `TenantRepository` extends `JpaRepository` + `JpaSpecificationExecutor` and has `@Repository`.
  - `existsByCode` exists, with no `AndIdNot` because the code is immutable.
  - `findByCode` is used by the module's own filter: pre-authentication infrastructure, the same precedent as `JwtAuthenticationFilter`.
- `build-create-dto`: compliant for Create, Response, Search and StatusUpdate.
  - They carry `@Schema` with bilingual text and i18n validation keys, and `@JsonFormat` UTC on the audit timestamps.
  - Named deviation: no Update or Usage DTO, since the code is immutable and tenants are never deleted.
  - `TenantCreateRequest.toString` hides the password.
- `build-create-mapper`: compliant. The mapper is manual and null-safe, does no normalization, and maps the audit fields. There is no update mapping, since status changes go through entity helpers.
- `build-create-service`: compliant.
  - `@Service @RequiredArgsConstructor @Slf4j`.
  - Every public method has `@PreAuthorize("hasAuthority(T(...PermissionConstants).PLATFORM_TENANT_MANAGE)")`.
  - `@Transactional` / `readOnly` as appropriate.
  - Every method returns `ServiceResult`.
  - Rules are delegated to `TenantDomain`.
  - Search uses `SpecBuilder` / `PageableBuilder` with `ALLOWED_SORT_FIELDS`.
  - No cross-module injection: contributors arrive through the tenant SPI.
  - Named infrastructure exceptions with no `@PreAuthorize`: `TenantResolutionFilter`, the provisioning contributors (reached only from `create`), and `BootstrapAdminPasswordRunner`.
- `build-create-controller`: compliant.
  - Thin, `OperationCode.craftResponse`, `@Valid @RequestBody`, `@Operation` on each endpoint, bilingual `@Tag`, `POST /search`.
  - Named deviations A.6.5/A.6.7/A.6.8: `PATCH /status` per the step file; no delete or usage endpoint.
- `gov-enforce-error-handling`: compliant, 23/23 for the new code.
  - Every throw is a `LocalizedException` with a code registered in `TenantErrorCodes` (private constructor that throws) or `CommonErrorCodes`.
  - Every code has an EN and an AR message, with `''{0}''` arguments where they are passed.
  - Not-found → `NOT_FOUND`, duplicate → `ALREADY_EXISTS`, rule violation → `BUSINESS_RULE_VIOLATION`.
  - The `DataIntegrityViolationException` around `saveAndFlush` is caught only to re-raise the duplicate code, the same TOCTOU pattern as `FileCategoryService`.
- `gov-enforce-caching-rules`: compliant.
  - `grep @Cacheable|@CacheEvict|CacheManager` over `erp-core/src/main` finds javadoc mentions only. No cache exists and none was added, so no cross-tenant cache leakage is possible.
  - Note for later steps: any cache over tenant-scoped data must include `TenantContext.require()` in its key.
- `gov-enforce-backend-contract`: tenant feature 80/85.
  - Fails, all named deviations: A.1.18, A.3.12 (no Usage DTO), A.6.5, A.6.7, A.6.8.
  - The 7 CU checks pass.
  - The existing modules' entities changed only in their base class and `@UniqueConstraint` columns.
- `gov-validate-backend-feature` (tenant feature):
  - Stage 0: 8/9 (one authority instead of VIEW/CREATE/UPDATE/DELETE, per the step).
  - Stage 1: 13/15 (no Update/Usage DTO).
  - Stage 2: 80/85.
  - Stage 3: 36/37 (the four-permission check).
  - Stage 4: 2/2.
  - Score **139/148 (93.9%)**, CONDITIONAL by the raw threshold. Every lost point is a shape the step file prescribes (single platform authority, `PATCH status`, tenants never deleted or renamed), recorded as named deviations. No automatic-rejection trigger is hit.

## Notes for later steps

- **Writing a new tenant-scoped entity and table** (rules also in `db/migration/core/README.md`):
  - The entity extends `com.erp.common.domain.AuditableEntity`. Never set `tenantId` or `version`; Hibernate does.
  - The migration adds `TENANT_ID BIGINT NOT NULL` with no default, `FK_<TABLE>_TENANT` → `CORE_TENANT(ID)`, `IDX_<TABLE>_TENANT`, `VERSION BIGINT NOT NULL DEFAULT 0` and the four audit columns.
  - Every unique constraint is `(TENANT_ID, ...)`, also in the entity's `@UniqueConstraint`.
  - Seed rows name `TENANT_ID` explicitly (1 = PLATFORM).
  - A global table needs a step file that says so; its entity extends `GlobalAuditableEntity`.
  - `TenantSchemaIntegrationTest` will fail when a new table or entity breaks this rule, since it counts 18 tables, 18 entities and 13 unique constraints. Update its counts and global sets deliberately.
- **Migrations:** V10 is taken. Additive-only applies again from V11 on.
- **Tenant context:**
  - Read it with `com.erp.tenant.TenantContext.current()/require()`.
  - System code outside a request (step 08 async notifications, schedulers, listeners) must wrap its work in `TenantContext.runAs(tenantId, ...)` / `callAs`.
  - Hibernate binds the tenant when a session opens, so call `runAs` around the `@Transactional` call, or open a `REQUIRES_NEW` inside it, never inside an open transaction.
  - An async event must carry its tenant id.
- **Tests:**
  - Each test method extending `AbstractIntegrationTest` runs as PLATFORM (`TenantContextTestExecutionListener`), which owns all seed data.
  - For another tenant, either:
    - use `runAs` around repository calls in a non-transactional test (see `TenantScopedQueryIntegrationTest`), or
    - go through HTTP with `com.erp.tenant.TenantHttp`: `platformOperator(...)` creates a PLATFORM SYS_ADMIN account once per JVM, and `provisionTenant(...)` / `token(code, "admin")` / `createUser(...)` do the rest.
  - Login over HTTP needs `X-Tenant-Code`. The bootstrap admin is `PLATFORM`/`admin`.
  - Tests in `com.erp.tenant` must not import other modules' internals (ArchUnit covers test classes).
- **JWT:**
  - Tokens carry `tid`. A token without `tid` no longer authenticates, so tokens issued before this step are invalid.
  - Step 06 (realms) must keep `tid` in every realm's token and keep "set tenant before the user lookup".
- **Exempt and public paths:**
  - `erp.core.tenant.exempt-paths` defaults to `/actuator/**`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/error`, `/api/v1/platform/**`.
  - Every public (unauthenticated) path that is not exempt requires `X-Tenant-Code` (400 `TENANT_REQUIRED` otherwise). That includes login, signup and password reset. The emailed reset link carries no tenant code, so the frontend must send it.
  - When step 06/07 adds `/api/v1/public/**` to `erp.core.security.public-paths`, those endpoints automatically require `X-Tenant-Code`. That is right for tenant storefront and public file URLs. A public URL that must work without a header (e.g. a public file link) must either be added to `erp.core.tenant.exempt-paths` and carry and resolve its tenant itself (e.g. inside a signed token), or embed the tenant code in the path.
- **Platform API and provisioning:**
  - `/api/v1/platform/**` needs a PLATFORM-tenant token with `PLATFORM_TENANT_MANAGE`.
  - A module that seeds reference data needed by every tenant must also implement `com.erp.tenant.TenantProvisioningContributor`: JDBC, explicit `TENANT_ID`, same transaction, copy from `sourceTenantId`. Otherwise new tenants will not have that data. Step 09 (settings/sequences) and step 08 (channels) will need this.
- **Security chain:** an app that replaces `erpCoreSecurityFilterChain`, or adds its own chain for its own paths, does not get `TenantResolutionFilter`. Requests on such a chain run without a tenant and fail on the first JPA access with `TENANT_CONTEXT_MISSING`. Making the filter reusable by apps is left to a later step (it is tenant-internal today; `com.erp.autoconfigure` builds it).
- **Caching:** none exists. Any future `@Cacheable` over tenant-scoped data must include the tenant in its key.
- **Rate limiting:** none exists. A future login limiter must key by `tenantCode + ':' + username`.
- **Optimistic locking:** a stale update → 409 `CONCURRENT_MODIFICATION`. Update DTOs do not carry `version` yet, so clients cannot detect lost updates across requests; a later step may expose it.
- **Cross-tenant FKs** (e.g. a user role pointing to another tenant's role) are prevented by the application, since entity loads are tenant-filtered, not by composite FKs (out of scope: no RLS).
