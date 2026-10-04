# Step 03 — Split into `erp-core` library + reference app, auto-configuration, Java 21 — report

## Summary

The single `com.erp:erp-system` application is now a two-module Maven build:

- **`erp-platform`** (root `pom.xml`, `packaging=pom`) aggregates the two modules.
- **`erp-core`** (library jar) holds `com.erp.{common,cu,mdl,sec,file,notif}` plus the new
  `com.erp.autoconfigure`. It has no main class, no `application.properties` and no
  `spring-boot-maven-plugin`. It is wired only by the classes listed in
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
- **`erp-app-reference`** (runnable jar) depends on `erp-core` like any consumer would, and adds
  its own `V1000__app_smoke.sql`.

`com.erp.main` (`ErpMainApplication`, the hand-built `JpaConfig` EMF, `SecurityConfig`,
`OpenApiConfig`) was deleted and replaced as follows:

- `ErpCoreAutoConfiguration`: scans exactly the core packages and relies on Boot's
  `HibernateJpaAutoConfiguration` (dialect auto-detected, no `default_schema`). It also contributes
  the i18n `messageSource`. Its repository registration backs off per repository and collapses
  overlapping packages (review round 1).
- `ErpCoreSecurityAutoConfiguration`: the `erpCoreSecurityFilterChain` bean, `@Order(100)`, which
  backs off on a same-named bean. Public paths come from `erp.core.security.public-paths`.
  `PasswordEncoder` is `@ConditionalOnMissingBean`.
- `ErpCoreFlywayAutoConfiguration`: prepends `classpath:db/migration/core`.
- `ErpCoreOpenApiAutoConfiguration`: `@ConditionalOnClass(GroupedOpenApi)`, one group per module.

Step 02's `DownloadTokenStoreAutoConfiguration` was folded into `com.erp.autoconfigure`, keeping
its Redis-store-first order. Every `app.*`/`file.*` key became `erp.core.*`, bound by the
validated `ErpCoreProperties` (see the migration table below).

Optional dependencies: Redis, mail, springdoc and the PostgreSQL driver are optional in
`erp-core`.

Build and runtime evidence:
- The build runs on **JDK 21** (`maven.compiler.release=21`, enforcer `[21,)`). Spring Boot stays
  at 4.0.1 and no JDK-25-only API was found.
- `mvn -q verify` is green: erp-core has 60 tests (the 51 existing ones, all passing, plus 9 new:
  4 from the first round, 5 from review round 1); erp-app-reference has 4.
- The reference jar, which contains no Redis or mail jars, started against a fresh PostgreSQL 16
  with only the five documented environment variables:
  - Flyway applied V1..V20 + V1000.
  - Health was `UP`.
  - The bootstrap admin login returned a JWT.

### Property migration table

| Old key (`application.properties`) | New key (`ErpCoreProperties`) | Default |
|---|---|---|
| `app.jwt.secret` | `erp.core.security.jwt.secret` | none: required, fails fast |
| `app.jwt.expiration-ms` | `erp.core.security.jwt.expiration-ms` | `3600000` |
| `file.access-token.secret` | `erp.core.files.access-token-secret` | none: required, fails fast |
| `app.frontend-url` | `erp.core.frontend.base-url` | none (reference app: `${FRONTEND_URL:http://localhost:4200}`) |
| `app.password-reset-path` | `erp.core.frontend.password-reset-path` | `/reset` (reference app: `${PASSWORD_RESET_PATH:/password-reset/complete}`) |
| (new) | `erp.core.security.public-paths` | the 9 paths in `ErpCoreProperties.Security.DEFAULT_PUBLIC_PATHS` |
| (constant `DEFAULT_MAX_CONTENT_BYTES`) | `erp.core.files.max-content-bytes` | `5242880` |
| (constant `MAX_REQUEST_BYTES`) | `erp.core.files.max-request-bytes` | `10485760` |

Reference-app environment variables: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`,
`FILE_TOKEN_SECRET`. The old app used `DB_USERNAME` and `FILE_ACCESS_TOKEN_SECRET`.

## Files changed

**Moved (`git mv`, commit `211764e`, content unchanged)**
- `src/main/java/com/erp/{common,cu,mdl,sec,file,notif}/**` → `erp-core/src/main/java/com/erp/…`
- `src/main/resources/db/migration/*` → `erp-core/src/main/resources/db/migration/core/`
- `src/main/resources/{i18n,META-INF}` → `erp-core/src/main/resources/`
- `src/test/{java,resources}` → `erp-core/src/test/`
- `Dockerfile`, `docker/`, `scripts/`, `governance/` (with the `governance/shared` gitlink), `CLAUDE.md` → `erp-app-reference/`
- `.gitmodules`: submodule `path` → `erp-app-reference/governance/shared` (its only change)

**Moved in the second commit, with edits**
- `erp-core/.../file/config/DownloadTokenStoreAutoConfiguration.java` → `erp-core/.../autoconfigure/` (package line only)
- `erp-core/src/test/.../file/config/DownloadTokenStoreAutoConfigurationTest.java` → `erp-core/src/test/.../autoconfigure/` (package line only)
- `erp-core/.../sec/controller/DevPasswordResetController.java` → `erp-app-reference/src/main/java/com/erp/app/dev/` (package and javadoc; still `@Profile("dev")`)

**Created**
- `erp-core/pom.xml`, `erp-app-reference/pom.xml`
- `erp-core/src/main/java/com/erp/autoconfigure/{ErpCoreProperties,ErpCoreAutoConfiguration,ErpCoreSecurityAutoConfiguration,ErpCoreFlywayAutoConfiguration,ErpCoreOpenApiAutoConfiguration}.java`
- `erp-core/src/test/java/com/erp/autoconfigure/ErpCoreAutoConfigurationTest.java`
- `erp-core/src/test/java/com/erp/testsupport/CoreTestApplication.java`
- `erp-app-reference/src/main/java/com/erp/app/ReferenceApplication.java`
- `erp-app-reference/src/main/resources/application.yml`
- `erp-app-reference/src/main/resources/db/migration/V1000__app_smoke.sql`
- `erp-app-reference/src/test/java/com/erp/app/ReferenceApplicationSmokeTest.java`
- `docs/steps/03-report.md`

**Modified**
- `pom.xml`: now the `erp-platform` aggregator:
  - `<modules>`;
  - shared properties;
  - `dependencyManagement` for jjwt, bucket4j, springdoc, lombok, archunit, testcontainers, zonky and erp-core (including its test-jar);
  - compiler/surefire `pluginManagement` (Lombok + configuration-processor annotation paths);
  - enforcer `[21,)`;
  - the Windows no-fork profile.
- `erp-app-reference/Dockerfile`: builds from the repo root (`-f erp-app-reference/Dockerfile .`) on temurin 21 and copies `erp-app-reference/target/erp-app-reference-*.jar`.
- `erp-core/.../META-INF/spring/...AutoConfiguration.imports`: the four core auto-configurations plus `DownloadTokenStoreAutoConfiguration`.
- Classes now reading `ErpCoreProperties`:
  - `sec/security/JwtTokenIssuer.java`, `JwtTokenValidator.java`;
  - `sec/service/PasswordResetService.java`;
  - `file/config/FileTokenConfig.java`;
  - `file/service/FileService.java` (size limits).
- `file/domain/FileValidationDomainService.java`: overloads that take the configured limits (the old methods and constants are kept).
- `sec/security/JwtAuthenticationFilter.java`: `@Component` removed (the bean now comes from the security auto-configuration).
- `file/service/RedisDownloadTokenStore.java`: `@ConditionalOnClass(StringRedisTemplate.class)` added.
- Comment/javadoc and message text only:
  - `file/service/{DownloadTokenStore,InMemoryDownloadTokenStore}.java` (link target);
  - `file/domain/FileAccessTokenDomainService.java` (property name in javadoc and message);
  - `sec/controller/AuthController.java` (comments that named `SecurityConfig`);
  - `db/migration/core/README.md` (location header).
- Tests:
  - `testsupport/AbstractIntegrationTest.java` → `CoreTestApplication`;
  - `testsupport/TestPostgres.java`: `registerDataSource`/condition made public, plus `jdbcUrl()/username()/password()`;
  - `testsupport/TestcontainersPostgresConfiguration.java` made public;
  - `architecture/CrossModuleBoundaryArchTest.java`: the `com.erp.main` exemption is now `com.erp.autoconfigure`;
  - `src/test/resources/application-test.properties` → `erp.core.*` keys, plus `open-in-view=false` and Jackson `fail-on-unknown-properties=false`.
- `.sdkmanrc`: `java=21.0.7-tem`.
- `docs/DEVIATIONS.md`: 18 `[03]` entries.

**Deleted**
- `src/main/java/com/erp/main/**` (`ErpMainApplication`, `config/JpaConfig`, `config/SecurityConfig`, `config/OpenApiConfig`)
- `src/main/resources/application.properties`, `application-dev.properties`, `application-prod.properties`
- `erp-core/src/test/java/com/erp/ErpTestApplication.java` (replaced by `CoreTestApplication`)

**Untouched on purpose**
- `.claude/`: stays at the root (orchestrator decision).
- `erp-core-plan/execution-state.json`.
- Every migration file.

## Decisions & deviations

These mirror the 18 `[03]` entries in `docs/DEVIATIONS.md` (the full text is there):

1. **`.claude/` stays at the repository root** (orchestrator decision). `depoymentssh` does not exist. The other root dotfiles stay, and `.sdkmanrc` moves to JDK 21.
2. **`CORE_PACKAGES`.** An annotation cannot take a `String[]` constant. `CORE_PACKAGE_LIST` (a comma-separated `String` that `@ComponentScan` tokenizes) is the single place to edit, and `CORE_PACKAGES` is derived from it. Entity scan and JPA repositories are registered programmatically from `CORE_PACKAGES`.
3. **Application packages are scanned too, with back-off.** The application's `AutoConfigurationPackages` are added to entity scanning, and to repository scanning only when the app registers no repositories itself. Overlapping packages are collapsed, and an already-registered repository is never registered again (review round 1).
4. **Core contributes the `messageSource`.** Application bundles come first, then `i18n/messages`; UTF-8; no system-locale fallback.
5. **`JwtAuthenticationFilter`** is a `@Bean` of the security auto-configuration, not a `@Component`. That auto-configuration is ordered before Boot's web-security auto-configurations.
6. **Public paths are method-agnostic.** The old config allowed POST only on the four auth endpoints.
7. **Required secrets fail fast.** `@Validated`/`@NotBlank` messages name the property. Modules inject `ErpCoreProperties`. The file size limits are wired. The reset-path default is `/reset`.
8. **No `CacheConfig` or `@EnableCaching` exists**, so there was nothing to change.
9. **Mail and Redis are optional, and this was verified by starting the reference jar without either.** `RedisDownloadTokenStore` also has `@ConditionalOnClass`.
10. **`DownloadTokenStoreAutoConfiguration` moved to `com.erp.autoconfigure`** and is the fifth `.imports` entry.
11. **Flyway discards the prepended core location as a sub-location of `classpath:db/migration`.** Same result, plus one WARN line.
12. **Extra reference-app dependencies:** springdoc and Lombok. The `flyway-maven-plugin` was not carried over.
13. **`application.yml` carries the app-level settings of the old properties.** The plan's env var names are used. The Dockerfile builds from the root and drops `go-offline`; it was not built here (no Docker).
14. **The smoke test reuses erp-core's test-jar** (`TestPostgres` + `TestcontainersPostgresConfiguration` only).
15. **`erp-app-reference` always forks surefire.** Under the Windows no-fork profile, a second Tomcat in the Maven JVM fails with "factory already defined".
16. **The ArchUnit exemption moved from `com.erp.main` to `com.erp.autoconfigure`.**
17. **The reference start check ran against the native PostgreSQL 16** (scratch DB, dropped), with `env -i` plus the five variables.

### Review round 1 fixes

The reviewer found one blocking issue. The core JPA repository registrar never backed off and never
collapsed overlapping packages. As a result, startup crashed with `BeanDefinitionOverrideException`
in two cases:

- **Case A:** an app in `com.acme` with its own `@EnableJpaRepositories("com.acme")`.
- **Case B:** an app whose configuration lives in `com.erp`.

Fixed in `erp-core/src/main/java/com/erp/autoconfigure/ErpCoreAutoConfiguration.java`:

- **Package collapse.** `collapsePackages(...)` drops exact duplicates and every package covered by
  another entry (prefix + `.`). Both the entity-scan registrar and the repository registrar use it.
- **Repository registrar back-off.** `CoreJpaRepositoriesRegistrar` now builds its own
  `AnnotationRepositoryConfigurationSource` and `RepositoryConfigurationDelegate` instead of
  extending Boot's `AbstractRepositoryConfigurationSourceSupport`. It works in three steps:
  - It reads the repository interfaces already defined in the registry (application configuration
    is processed before auto-configurations).
  - It adds the application's auto-configuration packages only when there are none. This mirrors
    Boot's `DataJpaRepositoriesAutoConfiguration` back-off.
  - It filters out every candidate whose interface is already registered.
- **Result:** each core repository is registered exactly once. This holds when the app's
  `@EnableJpaRepositories` covers only its own package, and also when it covers `com.erp`.

Tests added to `ErpCoreAutoConfigurationTest`. The fixtures are test-only classes `com.acme.widget.{Widget,WidgetRepository}`,
`com.acme.{AcmeAppWithOwnRepositories,AcmeAppCoveringCorePackages,AcmeAppWithoutRepositoryConfig}`
and `com.erp.ErpRootConsumerApplication`.

- `appWithItsOwnEnableJpaRepositories_coreRepositoriesStillRegisteredOnce_appRepositoryOnce` (case A)
- `appWhoseEnableJpaRepositoriesCoversCorePackages_coreDoesNotRegisterThemAgain`
- `appWithoutRepositoryConfig_getsItsOwnRepositoriesAndEntitiesFromItsPackage` (the entity scan includes `com.acme` + core)
- `appConfigurationInAParentOfTheCorePackages_collapsesOverlappingPackages` (case B; the entity scan is exactly `com.erp`)
- `collapsePackages_dropsPackagesCoveredByAnotherEntry`

Each context test asserts:
- the context starts;
- `hasSingleBean` holds for the app and core repositories;
- every repository interface is backed by exactly one Spring Data factory bean.

The `[03]` deviation entry in `docs/DEVIATIONS.md` was updated to match. The reviewer's
non-blocking note (Boot's "Using generated security password" warning) was left as is: backing off
`UserDetailsServiceAutoConfiguration` would require core to define an authentication bean.

## Acceptance checklist

5/5 ✅. Each item with its evidence:

- ✅ **`mvn -q verify` green at root (both modules).**
  - `EXIT=0` under JDK 21.0.7, Maven 3.9.10, after deleting all `target/` directories.
  - erp-core: tests=60, failures=0, errors=0, skipped=0. erp-app-reference: tests=4, failures=0, errors=0, skipped=0 (re-run after review round 1).
  - Both modules used the embedded backend (no Docker).
- ✅ **`erp-core/target/erp-core-1.0.0-SNAPSHOT.jar` contains no `com/erp/main`, no `application.properties`, and has `META-INF/spring/...AutoConfiguration.imports`.**
  - `unzip -l` lists only `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
  - `jar tf` counts: `com/erp/main` 0, `application.properties` 0.
- ✅ **The reference app starts with only `DB_*`, `JWT_SECRET`, `FILE_TOKEN_SECRET` (no Redis, no SMTP).**
  - The jar's `BOOT-INF/lib` has no mail, Redis or Lettuce jar.
  - With `env -i` plus the 5 variables, the log shows `Successfully applied 21 migrations ... now at version v1000` and `Started ReferenceApplication`.
  - Probes: health `{"status":"UP"}`; login HTTP 200; `flyway_schema_history` = `1..20,1000`, all successful.
- ✅ **`grep -rn "@Value(\"\${app\.\|@Value(\"\${file\." erp-core/src/main` returns nothing** (grep exit 1).
- ✅ **JDK 21 build.**
  - `java -version` → `openjdk version "21.0.7"`.
  - The enforcer accepts `[21,)`, and `release=21`.

## Verification output

```
$ export JAVA_HOME=".../openlogic-openjdk-21.0.7+6-windows-x64"; java -version
openjdk version "21.0.7" 2025-04-15
$ mvn -v
Apache Maven 3.9.10 (5f519b97e944483d878815739f519b2eade0a91d)

$ rm -rf target erp-core/target erp-app-reference/target; mvn -q verify
EXIT=0 secs=131      (re-run after review round 1, all target/ deleted first)
[TestPostgres] integration-test database backend: EMBEDDED      (printed twice: once per module JVM)

surefire totals (target/surefire-reports/TEST-*.xml):
== erp-core
com.erp.architecture.CrossModuleBoundaryArchTest              2 0 0 0
com.erp.autoconfigure.DownloadTokenStoreAutoConfigurationTest 2 0 0 0
com.erp.autoconfigure.ErpCoreAutoConfigurationTest            9 0 0 0   (new: 4 + 5 from review round 1)
com.erp.file.service.InMemoryDownloadTokenStoreTest           3 0 0 0
com.erp.notif.service.DefaultChannelProviderTest              2 0 0 0
com.erp.sec.MenuServiceGatewayIntegrationTest                 2 0 0 0
com.erp.sec.SecCoverageIntegrationTest                        6 0 0 0
com.erp.sec.SecFrontendGapIntegrationTest                     5 0 0 0
com.erp.sec.SecLogoutIntegrationTest                          3 0 0 0
com.erp.sec.SecReadOneIntegrationTest                         6 0 0 0
com.erp.sec.SecSearchFilterIntegrationTest                   10 0 0 0
com.erp.sec.UserRolesInResponseIntegrationTest                8 0 0 0
com.erp.testsupport.TestProfileWiringIntegrationTest          2 0 0 0
erp-core tests=60 failures=0 errors=0 skipped=0
== erp-app-reference
com.erp.app.ReferenceApplicationSmokeTest                     4 0 0 0   (new)
erp-app-reference tests=4 failures=0 errors=0 skipped=0

$ unzip -l erp-core/target/erp-core-1.0.0-SNAPSHOT.jar | grep -E "AutoConfiguration.imports|com/erp/main|application.properties"
      267  2026-10-04 18:54   META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
  (note: the plan's literal `erp-core-*.jar` glob now also matches erp-core-1.0.0-SNAPSHOT-tests.jar,
   which unzip then treats as a member filter; the exact file name was used)
$ jar tf erp-core/target/erp-core-1.0.0-SNAPSHOT.jar | grep -c com/erp/main          → 0
$ jar tf erp-core/target/erp-core-1.0.0-SNAPSHOT.jar | grep -c application.properties → 0
$ jar tf erp-core/target/erp-core-1.0.0-SNAPSHOT-tests.jar | grep class
com/erp/testsupport/TestcontainersPostgresConfiguration.class
com/erp/testsupport/TestPostgres$Backend.class
com/erp/testsupport/TestPostgres$SharedPostgreSQLContainer.class
com/erp/testsupport/TestPostgres$TestcontainersSelected.class
com/erp/testsupport/TestPostgres.class

$ grep -rn "@Value(\"\${app\.\|@Value(\"\${file\." erp-core/src/main ; echo "grep exit=$?"
grep exit=1

Reference-app start check (native PostgreSQL 16, scratch DB erp_step03):
$ psql -c "CREATE DATABASE erp_step03"
$ env -i SystemRoot=… TEMP=… TMP=… DB_URL=jdbc:postgresql://localhost:5432/erp_step03 DB_USER=postgres \
    DB_PASSWORD=postgres JWT_SECRET=<56 chars> FILE_TOKEN_SECRET=<57 chars> \
    java -jar erp-app-reference/target/erp-app-reference-1.0.0-SNAPSHOT.jar
WARN  o.flywaydb.core.internal.util.Locations : Discarding location 'classpath:db/migration/core' as it is a sub-location of 'classpath:db/migration'
INFO  o.f.core.internal.command.DbMigrate     : Successfully applied 21 migrations to schema "public", now at version v1000 (execution time 00:00.630s)
INFO  com.erp.app.ReferenceApplication        : Started ReferenceApplication in 18.811 seconds (process running for 20.225)
health: {"groups":["liveness","readiness"],"status":"UP"}
login: 200            (POST /api/v1/sec/auth/login admin/admin)
flyway_schema_history: 1,2,3,…,20,1000 | all success = t
(earlier run of the same jar: GET /api/v1/sec/users → 401; GET /v3/api-docs/sec → 200)
$ taskkill //PID <pid> //F ; psql -c "DROP DATABASE erp_step03"   → DROP DATABASE
```

## Skills checked

- `.claude/skills/build-create-service/SKILL.md`: compliant. No service was generated.
  - The touched services (`PasswordResetService`, `FileService`) keep their `@Transactional`/`@PreAuthorize`/`ServiceResult` shape. They gain only a constructor-injected `ErpCoreProperties` (configuration, not another module's internals).
  - Size decisions are still delegated to `FileValidationDomainService` (A.5.18), now with the configured limits passed in as plain arguments.
  - No cross-module injection; ArchUnit is green.
- `.claude/skills/build-create-controller/SKILL.md`: the moved `DevPasswordResetController` is unchanged in shape:
  - `@RestController`/`@RequestMapping`/`@RequiredArgsConstructor`, bilingual `@Tag`, `@Operation`;
  - `@Valid @RequestBody`;
  - service + `OperationCode` only, no logic.
  
  Named deviation (pre-existing, unchanged): it is a single-action dev fixture, so the CRUD endpoint set does not apply.
- `.claude/skills/gov-enforce-caching-rules/SKILL.md`: compliant (0 caching annotations, 30/30 not applicable).
  - No `@Cacheable`/`@CacheEvict` was added, and core sets no cache type.
  - The register stays empty; `CacheConfig` does not exist.
  - D.5.2 holds: Redis is touched only by `RedisDownloadTokenStore`, an infrastructure adapter.
- `.claude/skills/gov-enforce-error-handling/SKILL.md`: compliant.
  - No new runtime error code or `LocalizedException`.
  - The fail-fast messages are startup configuration-binding failures (Bean Validation on `@ConfigurationProperties`), not request errors, so no `<Module>ErrorCodes`/i18n entry applies (named deviation, same reasoning as step 02's `NO_MAIL_SENDER`).
  - The existing `IllegalStateException` in `FileAccessTokenDomainService` only had its property name in the message updated.
  - The i18n bundles are unchanged and still resolve: asserted in `ErpCoreAutoConfigurationTest`, and the error-message assertions of the 7 sec integration classes are green.
- `.claude/skills/gov-enforce-backend-contract/SKILL.md`: sanity check of the moved layers.
  - Entities, repositories, DTOs, mappers, services and controllers were moved byte-for-byte; only the files listed above changed.
  - CU.1–CU.8 unchanged.
  - The one structural change is that `JwtAuthenticationFilter` is no longer component-scanned (a security filter, not a contract layer).
  - No violations introduced.

## Notes for later steps

- **Where `CORE_PACKAGES` lives:** `erp-core/src/main/java/com/erp/autoconfigure/ErpCoreAutoConfiguration.java`.
  - To add a core package (tenant in 05, events in 08, sequence in 09, audit in 10, report in 11), **append it to `CORE_PACKAGE_LIST`** (a comma-separated `String`).
  - `CORE_PACKAGES` (`String[]`) is derived from it and drives component scan, entity scan and JPA repositories alike.
  - Update `ErpCoreAutoConfigurationTest.corePackages_areTheSixCoreModules` with it.
  - Add the module to `CrossModuleBoundaryArchTest.MODULES` if it is bounded.
- **How to add an auto-configuration:**
  - Put an `@AutoConfiguration` class in `com.erp.autoconfigure`. That package is deliberately not in `CORE_PACKAGES`, so it is never component-scanned.
  - List it in `erp-core/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`.
  - Use `@ConditionalOnMissingBean` for anything an app may replace, and `@ConditionalOnClass` for optional dependencies (Redis, mail, springdoc).
- **New settings go into `ErpCoreProperties` (`erp.core.*`).** Modules inject `ErpCoreProperties`; never `@Value`, never an `application.properties` in core.
- **Security:**
  - The core chain `erpCoreSecurityFilterChain` matches all requests, `@Order(100)`.
  - An app adds its own chain with `@Order(<100)` **and** a `securityMatcher(...)`, or replaces the core chain by defining a bean with that name.
  - Public endpoints are `erp.core.security.public-paths`. Setting it replaces the list, so include the defaults: `ErpCoreProperties.Security.DEFAULT_PUBLIC_PATHS`.
  - `/api/v1/public/**` (README §4) is **not** in the default list yet. The step that adds the first public endpoint must add it.
- **Flyway:** core scripts go in `erp-core/src/main/resources/db/migration/core/`; app scripts go in the app's `classpath:db/migration` (V1000+). Step 04 renumbers and enforces the ranges.
- **i18n:** core keys stay in `erp-core/src/main/resources/i18n/messages{,_ar}.properties`. An app's own bundles (`spring.messages.basename`) are searched first.
- **Tests: how each module gets its database.**
  - *erp-core*: extend `com.erp.testsupport.AbstractIntegrationTest`. It boots `CoreTestApplication`, which scans only `com.erp.testsupport`, so the core is wired by its auto-configuration exactly as for a consumer. The rules from step 02 still apply: no `@SpringBootTest` redeclaration, and nested `@TestConfiguration`s are `@Import`ed.
  - *Context-runner tests* (`ApplicationContextRunner`): pass `spring.datasource.*` from `TestPostgres.jdbcUrl()/username()/password()`.
  - *erp-app-reference* (and any future app module): depend on `com.erp:erp-core:test-jar` (test scope; managed in the root pom) plus testcontainers-postgresql, testcontainers-junit-jupiter, spring-boot-testcontainers and zonky embedded-postgres. `@Import(TestcontainersPostgresConfiguration.class)` and feed `TestPostgres` into the datasource (see `ReferenceApplicationSmokeTest`).
  - App modules always fork surefire (`surefire.forkCount=1` in the module pom). This is required on Windows, where the root profile runs erp-core in-process.
  - Backend selection is unchanged: `-Derp.test.db` / `ERP_TEST_DB` = `auto|testcontainers|embedded`. In forked JVMs (erp-app-reference, and every module on Linux CI), prefer the env var.
- **JDK:** use JDK 21 (`/c/Program Files/Java/openlogic-openjdk-21.0.7+6-windows-x64` on this machine); any JDK ≥ 21 passes the enforcer. Boot is 4.0.1.
- **Deleting `target/`:** delete all `target/` directories before a full verify (stale classes can duplicate migrations).
- **Unchecked paths:**
  - The Testcontainers path and the Docker image build were not executed here (no Docker). Step 12's CI should run both.
  - Release builds must publish the erp-core test-jar too, or exclude the reference module's tests.
