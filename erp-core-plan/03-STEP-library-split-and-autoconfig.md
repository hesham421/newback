# Step 03 — Split into `erp-core` library + reference app, auto-configuration, Java 21

**Branch:** `step/03-library-split`
**Goal:** `erp-core` is a Spring Boot auto-configured library jar with no `main` class, no hard-coded scan packages, no mandatory Redis/SMTP, configurable security; `erp-app-reference` is a runnable app that depends on it and proves consumption.
**Why:** today `com.erp.main` hand-builds the EntityManagerFactory (`JpaConfig`, `setPackagesToScan("com.erp")`, excludes `HibernateJpaAutoConfiguration`), hard-codes the security whitelist (`SecurityConfig`) and OpenAPI groups, enforces JDK 25 (`maven-enforcer-plugin [25,26)`), and requires Redis + SMTP at startup.

## Preconditions
- Step 02 merged; `mvn -q verify` green.

## Target layout
```
pom.xml                       com.erp:erp-platform:1.0.0-SNAPSHOT  packaging=pom  modules: erp-core, erp-app-reference
erp-core/pom.xml              com.erp:erp-core  packaging=jar (library; NO spring-boot-maven-plugin repackage)
erp-core/src/main/java/com/erp/{common,cu,mdl,sec,file,notif}        (moved as-is)
erp-core/src/main/java/com/erp/autoconfigure/                        (new)
erp-core/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
erp-core/src/main/resources/db/migration/core/                       (migrations moved; renumbered in step 04)
erp-core/src/main/resources/i18n/                                    (moved)
erp-core/src/test/...                                                (all current tests moved; they boot a test-only @SpringBootApplication)
erp-app-reference/pom.xml     com.erp:erp-app-reference  packaging=jar  (spring-boot-maven-plugin)
erp-app-reference/src/main/java/com/erp/app/ReferenceApplication.java
erp-app-reference/src/main/resources/application.yml                 (dev-style config, Redis+SMTP optional)
erp-app-reference/src/main/resources/db/migration/V1000__app_smoke.sql
docs/, .github/ at root. governance/, scripts/, Dockerfile, docker/, depoymentssh, CLAUDE.md move under erp-app-reference/ (untouched content).
```

## Tasks

### A. Maven
1. Create the root `pom.xml` (parent `spring-boot-starter-parent` same version as today; `<modules>`; shared `<properties>`; `<dependencyManagement>` for jjwt, bucket4j, springdoc, archunit, testcontainers).
2. `erp-core/pom.xml`: dependencies = web, data-jpa, validation, security, cache, mail (**optional**), data-redis (**optional**), flyway-core + flyway-database-postgresql, postgresql (runtime, **optional** — consumer supplies the driver), jjwt, bucket4j, lombok (provided), springdoc (**optional**), `spring-boot-autoconfigure`, `spring-boot-configuration-processor` (optional). Test deps as in step 02. **No** `spring-boot-maven-plugin`.
3. `erp-app-reference/pom.xml`: depends on `erp-core`, adds `spring-boot-starter-actuator`, postgresql driver, `spring-boot-maven-plugin` (repackage).
4. **Java 21**: `maven.compiler.release=21`; enforcer rule `[21,)`. Fix any JDK-25-only API usage found by the compiler.

### B. Move code
5. `git mv src/main/java/com/erp/{common,cu,mdl,sec,file,notif} erp-core/src/main/java/com/erp/`.
6. Delete `src/main/java/com/erp/main/**` (ErpMainApplication, JpaConfig, SecurityConfig, OpenApiConfig) — replaced by auto-configuration below.
7. Move resources: `db/migration/*` → `erp-core/src/main/resources/db/migration/core/`; `i18n/*` → `erp-core/src/main/resources/i18n/`.
8. Delete `application*.properties` from core. Core ships **no** `application.properties`; defaults live in `@ConfigurationProperties` classes.

### C. Auto-configuration (package `com.erp.autoconfigure`)
9. `ErpCoreProperties` (`@ConfigurationProperties("erp.core")`) with nested groups and defaults:
   - `security.jwt.secret` (required, no default), `security.jwt.expirationMs=3600000`, `security.publicPaths` (List<String>, default = current whitelist: `/api/v1/sec/auth/login`, `/api/v1/sec/auth/signup`, `/api/v1/sec/auth/password-reset/**`, `/actuator/health/**`, `/actuator/info`, `/v3/api-docs/**`, `/swagger-ui/**`, `/swagger-ui.html`, `/error`).
   - `files.accessTokenSecret` (required), `files.maxContentBytes=5242880`, `files.maxRequestBytes=10485760`.
   - `frontend.baseUrl`, `frontend.passwordResetPath=/reset`.
   - Property migration table (old → new): `app.jwt.secret→erp.core.security.jwt.secret`, `app.jwt.expiration-ms→erp.core.security.jwt.expiration-ms`, `file.access-token.secret→erp.core.files.access-token-secret`, `app.frontend-url→erp.core.frontend.base-url`, `app.password-reset-path→erp.core.frontend.password-reset-path`. Replace every `@Value` usage accordingly.
10. `ErpCoreAutoConfiguration`: `@AutoConfiguration @EnableConfigurationProperties(ErpCoreProperties.class) @ComponentScan(basePackages = CORE_PACKAGES) @EntityScan(basePackages = CORE_PACKAGES) @EnableJpaRepositories(basePackages = CORE_PACKAGES)` where `CORE_PACKAGES = {"com.erp.common","com.erp.cu","com.erp.mdl","com.erp.sec","com.erp.file","com.erp.notif"}` — a single `public static final String[]` constant that later steps extend (`tenant` in 05, `events` in 08, `sequence` in 09, `audit` in 10, `report` in 11). Rely on Boot's standard `HibernateJpaAutoConfiguration` (delete the hand-built EMF). Hibernate dialect is auto-detected; `default_schema` is not set by core.
11. `ErpCoreSecurityAutoConfiguration`: exposes `JwtAuthenticationFilter` as a bean and a `SecurityFilterChain` bean named `erpCoreSecurityFilterChain` annotated `@ConditionalOnMissingBean(name = "erpCoreSecurityFilterChain")` and `@Order(100)`, built from `security.publicPaths`. Also exposes `PasswordEncoder` `@ConditionalOnMissingBean`. A consumer app may define its own chain with a lower order for its own paths.
12. `ErpCoreFlywayAutoConfiguration`: contributes `classpath:db/migration/core` to Flyway locations via a `FlywayConfigurationCustomizer` that **prepends** the core location to whatever the app configured (so apps keep `spring.flyway.locations` for their own scripts). Version ranges are enforced in step 04.
13. `ErpCoreOpenApiAutoConfiguration` (`@ConditionalOnClass(GroupedOpenApi.class)`): one `GroupedOpenApi` per core module (`sec, notif, file, mdl, cu`), each `@ConditionalOnMissingBean(name=...)`.
14. Redis & mail **optional**: `CacheConfig` uses `spring.cache.type` as set by the app (core sets nothing); `RedisDownloadTokenStore` `@ConditionalOnClass(StringRedisTemplate.class) @ConditionalOnBean(StringRedisTemplate.class)`; `DefaultChannelProvider` keeps `ObjectProvider<JavaMailSender>` (step 02). Startup must succeed with neither present.
15. Register in `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (all four classes).
16. Remove the `@Profile("dev")` `DevPasswordResetController` from the library (move to `erp-app-reference` under `com.erp.app.dev` with `@Profile("dev")`).

### D. Reference app
17. `ReferenceApplication` (`@SpringBootApplication` in `com.erp.app`). `application.yml` with datasource env vars (`DB_URL`, `DB_USER`, `DB_PASSWORD`), `erp.core.security.jwt.secret: ${JWT_SECRET}`, `erp.core.files.access-token-secret: ${FILE_TOKEN_SECRET}`, `spring.cache.type: simple`, `spring.flyway.locations: classpath:db/migration` and `V1000__app_smoke.sql` creating table `APP_SMOKE(ID BIGINT PRIMARY KEY)`.
18. Move `Dockerfile`, `docker/`, `depoymentssh`, `scripts/`, `governance/`, `CLAUDE.md`, `.claude/` into `erp-app-reference/` unchanged (they describe an app, not a library). Update `Dockerfile` paths for the module layout.

### E. Tests
19. Move all tests to `erp-core/src/test`. Add `erp-core/src/test/java/com/erp/testsupport/CoreTestApplication.java` (`@SpringBootApplication(scanBasePackages="com.erp.testsupport")`) so the auto-configuration is exercised the same way a consumer would. `AbstractIntegrationTest` points at it.
20. Add `erp-core/src/test/java/com/erp/autoconfigure/ErpCoreAutoConfigurationTest.java` using `ApplicationContextRunner`: (a) context loads with required properties, no Redis, no mail; (b) `erpCoreSecurityFilterChain` is backed off when the user defines a bean with that name; (c) missing `erp.core.security.jwt.secret` fails fast with a clear message.
21. Add `erp-app-reference/src/test/java/com/erp/app/ReferenceApplicationSmokeTest.java` (Testcontainers): context loads, Flyway applied core chain + `V1000`, `GET /actuator/health` = UP, `POST /api/v1/sec/auth/login` with the bootstrap admin (seeded by the core chain) returns a JWT.

## Acceptance
- `mvn -q verify` green at root (both modules).
- `erp-core/target/erp-core-1.0.0-SNAPSHOT.jar` contains no `com/erp/main`, no `application.properties`, has `META-INF/spring/...AutoConfiguration.imports`.
- Reference app starts with only `DB_*`, `JWT_SECRET`, `FILE_TOKEN_SECRET` env vars (no Redis, no SMTP).
- `grep -rn "@Value(\"\${app\.\|@Value(\"\${file\." erp-core/src/main` returns nothing.
- JDK 21 build.

## Verification commands
```bash
mvn -q verify
unzip -l erp-core/target/erp-core-*.jar | grep -E "AutoConfiguration.imports|com/erp/main|application.properties"
```

## Commit
`step(03): split into erp-core library with auto-configuration and erp-app-reference; Java 21; Redis/SMTP optional`

## Out of scope
Migration renumbering (step 04), tenant (05), any new feature.
