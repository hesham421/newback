# Step 02 — Test infrastructure (portable, green baseline)

**Branch:** `step/02-test-infrastructure`
**Goal:** `mvn -q verify` runs the full test suite on any machine with Docker, with no dependency on a localhost PostgreSQL/Redis/SMTP or on the `dev` profile. The remaining `sec` integration tests and the ArchUnit test are green.
**Why:** every later step must be verifiable automatically. Today all `sec` tests are `@SpringBootTest(classes = ErpMainApplication) @ActiveProfiles("dev")` against real local services; H2 is on the classpath but unusable because the migrations use PostgreSQL idioms (`nextval`, `TEXT`, `BOOLEAN`).

## Preconditions
- Step 01 merged. `mvn -q -DskipTests package` green.

## Inputs
- Tests: `src/test/java/com/erp/sec/*IntegrationTest.java` (7 classes after step 01), `src/test/java/com/erp/architecture/CrossModuleBoundaryArchTest.java`.
- `pom.xml`: `h2` test dependency, `spring-boot-starter-test`, `spring-security-test`, `archunit-junit5`, `maven-surefire-plugin`.
- Properties: `application.properties`, `application-dev.properties`, `application-prod.properties`; mandatory beans: Redis (`spring.cache.type=redis`, file download tokens), `JavaMailSender` (constructor dependency of `notif` `DefaultChannelProvider`), `app.jwt.secret`, `file.access-token.secret`, `app.frontend-url`.

## Tasks
1. **Dependencies (pom.xml)**: add `org.springframework.boot:spring-boot-testcontainers` and `org.testcontainers:postgresql` + `org.testcontainers:junit-jupiter` (test scope). Remove `h2`.
2. **Shared test base**: create `src/test/java/com/erp/testsupport/AbstractIntegrationTest.java`:
   - `@SpringBootTest(webEnvironment = RANDOM_PORT) @ActiveProfiles("test") @Testcontainers`.
   - One static `PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")` annotated `@ServiceConnection` (Boot auto-wires datasource).
   - Container is reused across test classes (static field + `withReuse(true)` and `testcontainers.reuse.enable=true` in `~/.testcontainers.properties` is optional; static per-JVM is sufficient).
3. **Test profile**: create `src/test/resources/application-test.properties`:
   - `spring.flyway.enabled=true`, `spring.jpa.hibernate.ddl-auto=none`.
   - `spring.cache.type=simple` (no Redis). `spring.data.redis.*` absent.
   - `spring.mail.host=localhost`, `spring.mail.port=2525` (mail bean exists but no server; tests must not send real mail — see task 5).
   - `app.jwt.secret=<64-char test secret>`, `app.jwt.expiration-ms=3600000`, `file.access-token.secret=<test>`, `app.frontend-url=http://localhost:3000`, `app.password-reset-path=/reset`.
   - `logging.level.root=WARN`.
4. **Migrate existing tests**: every `sec` integration test extends `AbstractIntegrationTest`; delete their own `@SpringBootTest`/`@ActiveProfiles("dev")` annotations. Keep `@Transactional` where present.
5. **Decouple infrastructure for tests (minimal, no redesign — redesign happens in steps 03/07/08)**:
   - Redis: the file-download token store currently reads/writes Redis directly. Introduce `com.erp.file.service.DownloadTokenStore` interface with two implementations: `RedisDownloadTokenStore` (`@ConditionalOnBean(StringRedisTemplate.class)`) and `InMemoryDownloadTokenStore` (`@ConditionalOnMissingBean(DownloadTokenStore.class)`, `ConcurrentHashMap` with expiry). Replace direct Redis usage in `FileAccessTokenDomainService`/file service with the interface.
   - Mail: in `notif`, make `DefaultChannelProvider` depend on `ObjectProvider<JavaMailSender>`; when absent, log and mark the dispatch `FAILED` with reason `NO_MAIL_SENDER` (existing `NOTIF_LOG` semantics). Tests that assert email dispatch assert the log row, not SMTP.
6. **Surefire**: ensure `maven-surefire-plugin` runs JUnit 5 and that `-Dtest=` filtering works; set `<reuseForks>true</reuseForks>` so one container serves all classes.
7. **ArchUnit test**: keep `CrossModuleBoundaryArchTest` as is; confirm it still passes (module list must no longer include `fin`).
8. **Dev profile**: leave `application-dev.properties` for local runs; it must not be referenced by tests.
9. Write `docs/steps/02-report.md`.

## Acceptance
- `mvn -q verify` green with Docker available and no local PostgreSQL/Redis/SMTP running.
- Running a single test works: `mvn -q -Dtest=SecLogoutIntegrationTest test`.
- No test class references `ActiveProfiles("dev")` or `ErpMainApplication` explicitly.
- `grep -rn "StringRedisTemplate\|RedisTemplate" src/main/java` appears only inside `RedisDownloadTokenStore` (and cache config if any).

## Verification commands
```bash
mvn -q verify
grep -rn "ActiveProfiles(\"dev\")" src/test || echo OK
```

## Commit
`step(02): Testcontainers PostgreSQL test base, test profile, Redis/SMTP made optional for tests`

## Out of scope
Changing production wiring of Redis/mail beyond the two interfaces above; adding new tests for modules without tests (done per step later).
