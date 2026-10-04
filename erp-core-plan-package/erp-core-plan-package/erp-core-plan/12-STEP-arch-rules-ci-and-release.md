# Step 12 — Architecture rules, CI, publishing and release 1.0.0

**Branch:** `step/12-release`
**Goal:** the library's structural rules are enforced by the build; CI builds, tests and publishes `erp-core` to GitHub Packages on tags; the reference app is verified against the **published** artifact; version `1.0.0` is released with release notes and an upgrade/compatibility policy.
**Why:** the whole vision rests on "the core is never copied or modified by an app". That must be a build failure, not a convention.

## Preconditions
- Steps 06–11 merged; `mvn -q verify` green.

## Tasks

### A. ArchUnit rules (`erp-core/src/test/java/com/erp/architecture/`)
Keep `CrossModuleBoundaryArchTest` (module list = `common, cu, mdl, sec, file, notif, tenant, audit, events, sequence, report`, `autoconfigure` may depend on all) and add `CoreLibraryRulesArchTest`:
1. No class in `com.erp..` depends on `com.erp.app..` or any package outside `com.erp`, `java`, `jakarta`, `org.springframework`, `org.hibernate`, `com.fasterxml`, `io.jsonwebtoken`, `io.github.bucket4j`, `org.slf4j`, `lombok`, `software.amazon.awssdk`, `org.springdoc`, `org.flywaydb`.
2. Every `@Entity` extends `AuditableEntity` or `GlobalAuditableEntity`; classes extending `GlobalAuditableEntity` are only: `SecModuleReg, SecScreenReg, SecActionReg, Tenant, AppConfiguration`.
3. No class named `.*Fin.*` and no package `com.erp.fin`.
4. No `@Value("${app.` or `@Value("${file.` — all config via `ErpCoreProperties`.
5. Controllers only under `..controller..`; `@PreAuthorize` strings reference a `*Permissions` class in the same module.
6. No `@EnableScheduling`, no `@SpringBootApplication` in `src/main`.
7. No `nativeQuery = true` outside `tenant`, `sequence`, `audit` (documented exceptions).
Add `MigrationNamingTest` (step 04) to the same package if not already there, and extend it: core scripts after `V9` must not contain `DROP TABLE`, `DROP COLUMN`, `RENAME`, `ALTER COLUMN ... TYPE` (additive-only guard, regex on SQL with comments stripped).

### B. Quality gates in Maven
8. `maven-enforcer-plugin`: JDK `[21,)`, `banDuplicatePomDependencyVersions`, `requireUpperBoundDeps`.
9. `jacoco-maven-plugin` with a line-coverage minimum of 60% for `erp-core` (fail build below).
10. Surefire + Failsafe split is **not** required; keep everything under `verify`.

### C. CI (`.github/workflows/ci.yml`)
11. Jobs: `build-test` (JDK 21, Docker services enabled for Testcontainers, `mvn -B -q verify`, upload surefire reports + jacoco); `publish` (on tag `v*`, `mvn -B -q -pl erp-core -am deploy -DskipTests` to GitHub Packages via `GITHUB_TOKEN`); `consume-published` (after publish: checkout only `erp-app-reference`, temporarily replace the reactor dependency with the published version via `-Derp.core.version=${tag}` and run its smoke test — proves the jar works outside the reactor).
12. Root pom `<distributionManagement>` → `https://maven.pkg.github.com/<owner>/<repo>`; document consumer `settings.xml` snippet in `docs/CONSUMING.md`.

### D. Release
13. Version bump: root and modules to `1.0.0`; tag `v1.0.0`; after publish, bump to `1.1.0-SNAPSHOT`.
14. `docs/RELEASE.md`: semantic versioning policy — **MAJOR** only for removing/renaming public API, migration semantics or property keys (expected: never); **MINOR** for new modules/columns/endpoints/events (additive); **PATCH** for fixes. Migration rule restated: core scripts are additive only; apps own `V1000+`.
15. `docs/CONSUMING.md`: how an app depends on `erp-core` (pom snippet), required properties (`erp.core.security.jwt.secret`, `erp.core.files.access-token-secret`, datasource), optional infra (Redis, SMTP, S3), how to add permissions (`PermissionContributor`), events (`@TransactionalEventListener`), reports (`ReportProvider`), channels (`ChannelProvider`), storage (`StorageProvider`), number series seeding, settings keys, and the three rules of the vision (core never copied/modified; app logic lives in the app's `domain/` package; a change needed by two apps goes to core).
16. `docs/steps/12-report.md` + aggregate `docs/CHANGELOG.md` for 1.0.0 (one line per step).

## Acceptance
- CI green on `main`; `v1.0.0` tag published; `consume-published` job green.
- ArchUnit tests fail when intentionally violated (verify each rule once with a throwaway change, then revert; note in the report).
- JaCoCo ≥ 60% on `erp-core`.

## Commit(s)
`step(12): library architecture rules, enforcer/jacoco gates, CI with publish-on-tag and consume-published check, docs, release 1.0.0`

## Out of scope
Maven Central publication, signed artifacts, SBOM.
