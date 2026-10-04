# Step 02 — Test infrastructure (portable, green baseline) — report

## Summary

The test suite no longer needs a localhost PostgreSQL, Redis, SMTP server or the `dev` profile.
Every Spring integration test now extends `com.erp.testsupport.AbstractIntegrationTest`, which
starts the full application context (`RANDOM_PORT`, profile `test`) against one PostgreSQL 16
database per JVM:

- **Primary backend: Testcontainers** (`postgres:16`). It is wired through `@ServiceConnection`
  and selected automatically when Docker is reachable.
- **Fallback backend: in-process embedded PostgreSQL 16** (zonky). It is selected when Docker is
  absent.

`-Derp.test.db=testcontainers|embedded|auto` (or env `ERP_TEST_DB`) forces a backend.

Docker is not available on this machine, so every run below used the **embedded** backend. The
Testcontainers path compiles and is wired, and forcing it here fails fast with a clear message, but
it was not executed.

Redis and SMTP are now optional for tests:
- **Redis.** FILE's single-use download tokens go through the new `DownloadTokenStore`.
  `RedisDownloadTokenStore` is used when a `StringRedisTemplate` bean exists, and
  `InMemoryDownloadTokenStore` otherwise.
- **SMTP.** NOTIF's `DefaultChannelProvider` resolves `JavaMailSender` through an `ObjectProvider`.
  With no mail sender, an EMAIL dispatch is recorded as `FAILED` with reason `NO_MAIL_SENDER`.

`H2` was removed. `mvn -q verify` is green: 51 tests, 0 failures, 0 errors. All 7 `sec`
integration classes and `CrossModuleBoundaryArchTest` pass unchanged in their assertions.

## Files changed

**Created**
- `src/test/java/com/erp/testsupport/AbstractIntegrationTest.java`: the shared base (`@SpringBootTest(classes = ErpTestApplication, RANDOM_PORT)`, `@ActiveProfiles("test")`, `@Testcontainers`, `@Import(TestcontainersPostgresConfiguration)`, `@DynamicPropertySource`).
- `src/test/java/com/erp/testsupport/TestPostgres.java`: the per-JVM database holder and backend selection (Testcontainers/embedded).
- `src/test/java/com/erp/testsupport/TestcontainersPostgresConfiguration.java`: a conditional `@Bean @ServiceConnection PostgreSQLContainer`.
- `src/test/java/com/erp/testsupport/TestProfileWiringIntegrationTest.java`: pins PG 16, not `localhost:5432`, no Redis template, the in-memory token store, and a clean Flyway history.
- `src/test/java/com/erp/ErpTestApplication.java`: the test-side `@SpringBootConfiguration` (so no test names `ErpMainApplication`).
- `src/test/resources/application-test.properties`
- `src/main/java/com/erp/file/service/DownloadTokenStore.java`, `RedisDownloadTokenStore.java`, `InMemoryDownloadTokenStore.java`
- `src/main/java/com/erp/file/config/DownloadTokenStoreAutoConfiguration.java`
- `src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- `src/test/java/com/erp/file/service/InMemoryDownloadTokenStoreTest.java`, `src/test/java/com/erp/file/config/DownloadTokenStoreAutoConfigurationTest.java`, `src/test/java/com/erp/notif/service/DefaultChannelProviderTest.java`
- `docs/steps/02-report.md` (this file)

**Modified**
- `pom.xml`:
  - removed `h2`;
  - added `spring-boot-testcontainers`, `testcontainers-postgresql`, `testcontainers-junit-jupiter` and `io.zonky.test:embedded-postgres` 2.1.0 (all test scope);
  - imported `embedded-postgres-binaries-bom` 16.4.0;
  - set surefire `<reuseForks>true</reuseForks>`.
- `src/main/java/com/erp/file/service/FileService.java`: `StringRedisTemplate` was replaced by `DownloadTokenStore` (put/get/consume). The behaviour is identical: user-bound tokens, verify before consuming, single use.
- `src/main/java/com/erp/notif/service/DefaultChannelProvider.java`: `ObjectProvider<JavaMailSender>`, plus the `NO_MAIL_SENDER` failure reason.
- `src/test/java/com/erp/sec/{MenuServiceGateway,SecCoverage,SecFrontendGap,SecLogout,SecReadOne,SecSearchFilter,UserRolesInResponse}IntegrationTest.java`:
  - each now `extends AbstractIntegrationTest`;
  - `@SpringBootTest(classes = ErpMainApplication)` and `@ActiveProfiles("dev")` were removed, and `@Transactional` kept;
  - javadoc no longer says "real dev Postgres/Redis";
  - `UserRolesInResponse`: its `properties` moved to `@TestPropertySource`;
  - `SecReadOne`: imports a nested `@TestConfiguration` providing the `@Profile("dev")` `DevPasswordResetSupportService` as a test-only bean.
- `docs/DEVIATIONS.md`: 13 `[02]` entries.

**Deleted**: none (the `h2` dependency only).

**Unchanged on purpose**:
- `application.properties`, `application-dev.properties` and `application-prod.properties` (no test references `dev`).
- `CrossModuleBoundaryArchTest` (already free of `fin`).
- `FileAccessTokenDomainService`.
- `erp-core-plan/execution-state.json`.

## Decisions & deviations

Entries added to `docs/DEVIATIONS.md` (abridged; the full text is there):

- [02] **No Docker on this machine.** The Testcontainers path is primary and auto-selected when Docker exists. An embedded PG 16 fallback (zonky 2.1.0 + binaries 16.4.0) is added, selectable via `erp.test.db`/`ERP_TEST_DB`. `mvn -q verify` was verified on embedded only.
- [02] **Testcontainers 2.0.3 naming** (managed by Boot 4.0.1): `testcontainers-postgresql`, `testcontainers-junit-jupiter`, and the non-generic `org.testcontainers.postgresql.PostgreSQLContainer`.
- [02] **`@ServiceConnection` sits on a conditional `@Bean`**, not on a static field of the base class. Field-level resolution is unconditional and needs Docker. The container is a static per-JVM singleton whose `stop()` is a no-op (Ryuk reaps it).
- [02] **`ErpTestApplication` added in `com.erp`.** The base names it explicitly, because the upward search cannot reach `com.erp.main`, and a recursive search would find two configurations.
- [02] **Token-store registration.** The `DownloadTokenStore` implementations are registered by `DownloadTokenStoreAutoConfiguration` (via the AutoConfiguration.imports file, ordered after Redis), not by component scan. Otherwise `@ConditionalOnBean` would be evaluated too early.
- [02] **Only `FileService` used Redis.** `FileAccessTokenDomainService` was not touched.
- [02] **Test profile exclusions.** The test profile excludes the 3 `DataRedis*` auto-configurations, because Boot creates a `StringRedisTemplate` regardless of properties. Its mail settings fail fast (no auth/STARTTLS, 1 s timeouts, mail health off).
- [02] **`NO_MAIL_SENDER` is a stored failure reason**, not a thrown error code, so it has no i18n entry. With the test profile's `spring.mail.host` the sender exists, so this path is unit-tested.
- [02] **`SecReadOneIntegrationTest`** gets the `@Profile("dev")` service as a test-only bean. Production gating and the assertions are unchanged.
- [02] **`UserRolesInResponseIntegrationTest`** moved its `properties` to `@TestPropertySource`.
- [02] **`reuseForks=true` is set**, but on Windows the existing no-fork profile runs the tests in-process, so it has no effect there.
- [02] **Added tests for the step's new code only**: 3 unit test classes and 1 wiring integration test.
- [02] **Embedded initdb** is pinned to `encoding=UTF8` and `locale=C` (the host code page is Cp1256).

## Acceptance checklist

- ✅ `mvn -q verify` green with no local PostgreSQL/Redis/SMTP in use. Evidence: `EXIT=0`, 51 tests, 0 failures, 0 errors, 0 skipped. Redis (6379) and SMTP (25/465/587/2525) do not listen on this machine. The native PostgreSQL on 5432 is running but unused: `TestProfileWiringIntegrationTest` asserts the JDBC URL does not contain `localhost:5432`, and the backend line prints `EMBEDDED`. **Caveat:** this ran on the embedded fallback, not with Docker (Docker is unavailable here; see deviations).
- ✅ Running a single test works: `mvn -q -Dtest=SecLogoutIntegrationTest test` gives `EXIT=0`, `tests="3" errors="0" skipped="0" failures="0"`.
- ✅ No test class references `ActiveProfiles("dev")` or `ErpMainApplication`. Evidence: both greps over `src/test` return nothing (`OK`, `OK-no-ErpMainApplication`).
- ✅ `grep -rn "StringRedisTemplate\|RedisTemplate" src/main/java` matches only inside `RedisDownloadTokenStore.java` (4 lines). No cache config exists.

4/4 ✅ (the first with the Docker caveat above)

## Verification output

```
$ export JAVA_HOME=".../openjdk-25+36_windows-x64_bin/jdk-25"   (pom still enforces 25 at this step)

$ mvn -q verify
EXIT=0
[only Maven/JDK warnings, Spring Boot banners and WARN-level app logs; tail:]
... WARN ... DevPasswordResetSupportService : DEV-ONLY: issued a password-reset token for User ID 19 through the test-fixture endpoint
 :: Spring Boot ::                (v4.0.1)
... WARN ... HHH90000025: PostgreSQLDialect does not need to be specified explicitly ...
Using generated security password: 9312febd-...      (pre-existing UserDetailsServiceAutoConfiguration warning)
[backend line:] [TestPostgres] integration-test database backend: EMBEDDED
[expected in auto mode without Docker, once:] ERROR o.t.d.DockerClientProviderStrategy : Could not find a valid Docker environment.

surefire totals (target/surefire-reports/TEST-*.xml):
com.erp.architecture.CrossModuleBoundaryArchTest          2 0 0 0
com.erp.file.config.DownloadTokenStoreAutoConfigurationTest 2 0 0 0
com.erp.file.service.InMemoryDownloadTokenStoreTest       3 0 0 0
com.erp.notif.service.DefaultChannelProviderTest          2 0 0 0
com.erp.sec.MenuServiceGatewayIntegrationTest             2 0 0 0
com.erp.sec.SecCoverageIntegrationTest                    6 0 0 0
com.erp.sec.SecFrontendGapIntegrationTest                 5 0 0 0
com.erp.sec.SecLogoutIntegrationTest                      3 0 0 0
com.erp.sec.SecReadOneIntegrationTest                     6 0 0 0
com.erp.sec.SecSearchFilterIntegrationTest               10 0 0 0
com.erp.sec.UserRolesInResponseIntegrationTest            8 0 0 0
com.erp.testsupport.TestProfileWiringIntegrationTest      2 0 0 0
TOTAL tests=51 failures=0 errors=0 skipped=0

$ mvn -q -Dtest=SecLogoutIntegrationTest test
EXIT=0
[TestPostgres] integration-test database backend: EMBEDDED
TEST-com.erp.sec.SecLogoutIntegrationTest.xml: tests="3" errors="0" skipped="0" failures="0"

$ grep -rn "ActiveProfiles(\"dev\")" src/test || echo OK
OK

$ grep -rn "ErpMainApplication" src/test || echo OK-no-ErpMainApplication
OK-no-ErpMainApplication

$ grep -rn "StringRedisTemplate\|RedisTemplate" src/main/java
src/main/java/com/erp/file/service/RedisDownloadTokenStore.java:6:import org.springframework.data.redis.core.StringRedisTemplate;
src/main/java/com/erp/file/service/RedisDownloadTokenStore.java:15: * {@code @ConditionalOnBean} below sees the {@code StringRedisTemplate} bean when one exists.
src/main/java/com/erp/file/service/RedisDownloadTokenStore.java:18:@ConditionalOnBean(StringRedisTemplate.class)
src/main/java/com/erp/file/service/RedisDownloadTokenStore.java:22:    private final StringRedisTemplate redisTemplate;

$ netstat -ano | grep LISTENING | grep -E ":(6379|25|587|2525|465|5432) "
  TCP    0.0.0.0:5432 ... LISTENING       (native PG — not used by tests)
  TCP    [::]:5432    ... LISTENING

Backend selector check (Docker absent):
$ mvn -Dtest=SecLogoutIntegrationTest -Derp.test.db=testcontainers test
EXIT=1
Caused by: java.lang.IllegalStateException: erp.test.db=testcontainers but no Docker environment is available; start Docker or use erp.test.db=embedded
```

## Skills checked

- `.claude/skills/gov-enforce-error-handling/SKILL.md`: compliant.
  - No new exception or error code was added.
  - `FileService` keeps its existing `LocalizedException(Status.UNAUTHORIZED, FILE_ACCESS_TOKEN_INVALID)` paths.
  - `NO_MAIL_SENDER` is a NOTIF_LOG failure-reason value (`ChannelSendResult.errorMessage`), the same mechanism as the existing `"missing recipient email address"`. It is never thrown, so the "registered in both bundles" rule does not apply (named deviation).
  - No exception is swallowed: the new branch returns a failure result that is persisted as `FAILED`.
- `.claude/skills/gov-enforce-caching-rules/SKILL.md`: compliant.
  - No `@Cacheable`/`@CacheEvict` was added. No `@EnableCaching` exists, so `spring.cache.type=simple` in tests only guarantees that no Redis cache manager is created.
  - D.5.2 ("no direct cache-client calls in service code") is now better met than before: `FileService` no longer calls Redis directly. The token store is not the Spring cache abstraction.
- `.claude/skills/build-create-service/SKILL.md`: compliant for the touched service.
  - `FileService` stays orchestration-only, with unchanged `@Transactional`/`@PreAuthorize` and no cross-module injection.
  - The new `*DownloadTokenStore` classes are infrastructure adapters, not `@Service` CRUD services, so the CRUD/`@PreAuthorize` template does not apply to them. They are not HTTP-reachable and are only injected into `FileService`.
  - `DefaultChannelProvider` is a `@Component` channel adapter; only its constructor dependency changed.
  - ArchUnit boundary test green: the test classes reference `com.erp.file` beans by name, not by type, from `testsupport`.

## Notes for later steps

- **How to write an integration test from now on:** `class XxxIntegrationTest extends AbstractIntegrationTest { ... }`.
  - Add only what you need: `@Transactional` for rollback, `@TestPropertySource` for extra properties, `@Import(SomeTestConfig.class)` for test beans.
  - Never redeclare `@SpringBootTest`/`@ActiveProfiles`, and never reference `ErpMainApplication` or the `dev` profile.
  - Nested `@TestConfiguration` classes are **not** auto-detected, because the base names `classes = ErpTestApplication.class`. `@Import` them explicitly (see `SecReadOneIntegrationTest`).
  - Each distinct `@TestPropertySource`/`@Import` combination creates another cached Spring context (3 today), but all of them share the one database.
- **Backend selection:**
  - `com.erp.testsupport.TestPostgres` reads `-Derp.test.db` (or env `ERP_TEST_DB`) = `auto` (default) | `testcontainers` | `embedded`. `auto` probes Docker via `DockerClientFactory`; without Docker it logs one `DockerClientProviderStrategy` ERROR line and uses embedded.
  - The chosen backend is printed as `[TestPostgres] integration-test database backend: ...`.
  - On Windows the surefire no-fork profile runs tests inside the Maven JVM, so `-D` on the mvn command line reaches `TestPostgres` directly. In forked runs (Linux CI), prefer the env var or pass the property through `argLine`/`systemPropertyVariables`.
  - CI with Docker gets Testcontainers automatically. Consider `ERP_TEST_DB=testcontainers` there so that a missing Docker fails instead of silently falling back.
- **Database state is shared across test classes in a JVM** (no reset between classes). Existing tests keep data clean via `@Transactional` rollback or unique suffixes; later tests should do the same. Flyway migrates once, on the first context.
- **Step 03:**
  - `ErpTestApplication` (test-side composition: scans `com.erp`, excludes `HibernateJpaAutoConfiguration`) is the natural seed for erp-core's test application once `ErpMainApplication` moves to `erp-app-reference`.
  - `DownloadTokenStoreAutoConfiguration` and the new `META-INF/spring/...AutoConfiguration.imports` file are the first core auto-configuration. Fold them into step 03's `com.erp.autoconfigure` and keep the import order (Redis store, then in-memory).
  - When Redis becomes an optional dependency, guard `RedisDownloadTokenStore` with `@ConditionalOnClass` as well.
  - The test profile's `spring.autoconfigure.exclude` of the three `DataRedis*` classes uses Boot 4 names (`org.springframework.boot.data.redis.autoconfigure.*`).
  - Java 21: `TestPostgres` and the tests use only Java 17 syntax.
- **Step 07** replaces the token stores with the storage/token SPI. The behaviour to preserve: put with TTL, get, and atomic single-use consume.
- **Step 08:**
  - `NO_MAIL_SENDER` is the reason recorded when no `JavaMailSender` exists.
  - In the test profile the mail sender **does** exist (localhost:2525, nothing listening), so an EMAIL dispatch in an integration test ends `FAILED` with a connection-refused message after the retry loop (no sleep, about 1 s timeouts). Assert the NOTIF_LOG row, not SMTP.
- `DevPasswordResetSupportService`/`DevPasswordResetController` remain `@Profile("dev")`. The dev endpoint does not exist in tests.
- The Testcontainers path should be exercised once on a machine with Docker (e.g. step 12's CI) to confirm it end to end. Boot's generic `JdbcContainerConnectionDetailsFactory` handles any `JdbcDatabaseContainer`, including the 2.x `PostgreSQLContainer` used here.
