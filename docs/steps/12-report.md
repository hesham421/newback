# Step 12 — Architecture rules, CI, publishing and release 1.0.0 — report

## Summary

The library's structural rules are now enforced by the build, CI is defined, and the version is `1.0.0`.

**Architecture rules.**
- New `CoreLibraryRulesArchTest` (`erp-core/src/test/java/com/erp/architecture/`) with 12 checks for
  step rules 1–7:
  - no dependency on `com.erp.app..`, and only the allowed libraries;
  - every `@Entity` extends an auditable base, and only five listed entities are global;
  - no `*Fin*` class or `com.erp.fin` package, in main and test code;
  - no `@Value` of `${app.`/`${file.`/`${erp.core.`;
  - controllers only under `..controller..`, and `@PreAuthorize` references only the module's own
    `*Permissions`;
  - no `@EnableScheduling` and no `@SpringBootApplication`;
  - no native SQL outside `tenant`/`sequence`/`audit`, plus a raw-JDBC allow-list.
- `CrossModuleBoundaryArchTest` is unchanged; its module list already is
  `cu, mdl, sec, file, notif, tenant, audit, events, sequence, report`. `common` and `autoconfigure`
  are unbounded by design.
- `MigrationNamingTest` moved into `com.erp.architecture`. It gained the additive-only guard for core
  scripts after `V9`.
- Every rule passes on the real code. Each rule was shown to fail on a deliberate violation, and the
  violations were then removed.

**Quality gates.**
- The enforcer adds `banDuplicatePomDependencyVersions` and `requireUpperBoundDeps` to the existing
  JDK `[21,)` rule. No conflicts were found, so no pins were needed.
- `jacoco-maven-plugin` 0.8.13 on erp-core fails `verify` below 60 % line coverage. **Measured: 75.21 %**
  (4590 of 6103 lines), so no coverage tests were added.
- erp-core now always forks its surefire JVM, because the JaCoCo agent needs a fork.

**CI (`.github/workflows/ci.yml`).**
- `build-test`: JDK 21, `ERP_TEST_DB=testcontainers`, `mvn -B -q verify`, uploads the surefire and
  JaCoCo reports.
- `docker-image`: builds `erp-app-reference/Dockerfile`.
- `publish`: `v*` tags only; checks that the tag equals the pom version, then deploys the parent pom,
  the erp-core jar and the erp-core test-jar to GitHub Packages with `GITHUB_TOKEN`.
- `consume-published`: sparse checkout of `erp-app-reference/` only, no Maven cache,
  `-Derp.core.version=<tag version>`, runs the app's tests.
- `<distributionManagement>` points to `https://maven.pkg.github.com/hesham421/newback` (id `github`).

**Release.**
- Every pom is at `1.0.0` in the commit `step(12): release 1.0.0 ...`. **The merger tags that commit
  `v1.0.0`**; pushing the tag runs `publish` and `consume-published` in GitHub Actions.
- The next commit, `step(12): bump to 1.1.0-SNAPSHOT`, moves the poms to the next development version.
- New docs: `docs/RELEASE.md` (semver and migration policy), `docs/CONSUMING.md` (dependency, settings,
  properties, SPIs, events, series and settings, the three rules) and `docs/CHANGELOG.md` (one line per
  step 01–12).

**Not run on this machine (named gaps).** There is no Docker, no `GITHUB_TOKEN` and no authenticated
`gh` here. These run only in GitHub Actions and are **untested**:
- `publish`;
- `consume-published`;
- the Testcontainers path in CI;
- the Docker image build.

As the closest local stand-in, publish → consume was simulated against a file repository; see
Verification output.

## Files changed

**Created**
- `erp-core/src/test/java/com/erp/architecture/CoreLibraryRulesArchTest.java`
- `.github/workflows/ci.yml`
- `docs/RELEASE.md`, `docs/CONSUMING.md`, `docs/CHANGELOG.md`, `docs/steps/12-report.md`

**Moved and modified**
- `erp-core/src/test/java/com/erp/autoconfigure/MigrationNamingTest.java` →
  `erp-core/src/test/java/com/erp/architecture/MigrationNamingTest.java`:
  - package line changed;
  - added the additive guard, a self-test, and the comment/literal stripper;
  - the existing assertions are unchanged.

**Modified**
- `pom.xml`:
  - version `1.0.0`, then `1.1.0-SNAPSHOT`;
  - `jacoco.version` and an empty `argLine` default;
  - surefire `@{argLine}`;
  - jacoco in `pluginManagement`;
  - the enforcer hygiene execution;
  - `<distributionManagement>`.
- `erp-core/pom.xml`:
  - parent version;
  - `surefire.forkCount=1`;
  - the jacoco `prepare-agent`, `report` and `check` (LINE ≥ 0.60) executions.
- `erp-app-reference/pom.xml`:
  - parent version;
  - property `erp.core.version` (default `${project.version}`), used on the erp-core jar and test-jar.
- `erp-core/src/main/resources/db/migration/core/README.md`: one paragraph saying the additive rule is
  now a test.
- `docs/DEVIATIONS.md`: the `[12]` entries.

**Deleted:** none. No production Java code changed, and there are no migrations.

## Decisions & deviations

These mirror the `[12]` entries in `docs/DEVIATIONS.md`.

- **Local only.** There is no Docker, token or `gh` here.
  - `publish`, `consume-published`, the Testcontainers path in CI and the Docker image build are untested.
  - The merger creates the tag `v1.0.0` on the release commit.
- **Release sequencing.** Two commits:
  - `release 1.0.0` holds all step-12 code and docs and is the tag target;
  - `bump to 1.1.0-SNAPSHOT` follows it.

  "After publish" therefore became "after the release commit".
- **Rule 1.** The step's package list was kept and four packages that the code already uses were added:
  - `javax..` (JDK);
  - `io.swagger.v3.oas..` (springdoc's annotations and model);
  - `tools.jackson..` (Jackson 3 in Boot 4);
  - `org.aopalliance..` (Spring AOP API).

  A separate check forbids depending on `com.erp.app..`. Only production classes are analysed; rule 3
  also scans test classes.
- **Rule 2.** The global allow-list uses the real class names:
  - `ModuleRegistry`/`ScreenRegistry`/`ActionRegistry` (the step calls them `SecModuleReg`/`SecScreenReg`/
    `SecActionReg`);
  - plus `Tenant` and `AppConfiguration`.

  Steps 06–11 added no other global entity. The README's `TenantAwareEntity` is `AuditableEntity`.
- **Rule 4.** Fields, methods and parameters are all checked, and `${erp.core.` is forbidden as well.
  Core has no `@Value` at all, so no exception was needed.
- **Rule 5.** Each `T(...)` must be the same module's `*Permissions` class, and literal authorities are
  forbidden. `isAuthenticated()` and `permitAll()` are allowed. Documented same-module exceptions:
  - a `ReportProvider`'s `AUTHORITY` (step 11);
  - a value enum (`SettingScope`, step 09).
- **Rule 7.** Neither `nativeQuery = true` nor `createNativeQuery` exists anywhere. The raw-JDBC half
  allows `org.springframework.jdbc.core.*` only in:
  - `tenant`, `sequence` and `audit`;
  - the `com.erp.*.tenant..` provisioning contributors;
  - `autoconfigure`;
  - `NotificationRequeueJob`.
- **`MigrationNamingTest`.**
  - It moved packages.
  - The guard strips comments **and blanks string literals**, and it also catches `ALTER <col> TYPE`
    and `SET DATA TYPE`.
  - V10–V15 pass, because `DROP DEFAULT`, `DROP NOT NULL` and `DROP CONSTRAINT` are not on the list.
- **JaCoCo.**
  - erp-core forks its tests (`surefire.forkCount=1`) so the agent attaches. Under the Windows
    no-fork profile the gate would have skipped silently.
  - Root `argLine` defaults to empty.
  - Coverage is 75.2 %, so no tests were added.
- **Enforcer.** No upper-bound conflicts were found, so no pins were needed.
- **CI.**
  - There are four jobs; `docker-image` was added.
  - `publish` needs `build-test` and `docker-image`.
  - Tag/pom version guard.
  - The test-jar is published (`-DskipTests` still compiles the tests).
  - `consume-published` runs on a sparse checkout without a root pom: the parent pom and erp-core come
    from GitHub Packages through a generated `settings.xml`. It uses no cache and checks
    `_remote.repositories`.
  - Actions are pinned to `@v4`.
  - `governance-shared.yml` is untouched.
- **`erp.core.version`** is an erp-app-reference property; the reactor build is unchanged.
- **`<distributionManagement>`** points to `https://maven.pkg.github.com/hesham421/newback`, id
  `github`, for releases and snapshots.

## Acceptance checklist

| # | Acceptance | Result | Evidence |
|---|---|---|---|
| 1 | CI green on `main`; `v1.0.0` tag published; `consume-published` green | ⚠️ untested (named gap) | It cannot run here: no `GITHUB_TOKEN`, no push, no Docker. `ci.yml` parses (python yaml: 4 jobs, needs/if/permissions as designed). The merger tags the release commit, and the jobs run on push. Local stand-in: deploy to a file repository, then build erp-app-reference alone (no root pom) against it with `-Derp.core.version=1.0.0` → 10/10 tests green; `_remote.repositories` shows both `erp-core` and `erp-platform` came from the deployed repository. |
| 2 | ArchUnit tests fail when intentionally violated; noted per rule | ✅ | A throwaway probe, `com.erp.cu.archprobe.ArchProbes` + `com.erp.app.ArchProbeApp` + `V16__core_arch_probe.sql`, made all 12 `CoreLibraryRulesArchTest` checks and `MigrationNamingTest.coreScriptsAfterTheBaseline_areAdditiveOnly` fail, each with exactly one violation (messages below). The probe was deleted afterwards; the same tests are green on the real code. |
| 3 | JaCoCo ≥ 60 % on `erp-core` | ✅ | `jacoco:check` "All coverage checks have been met"; `jacoco.csv`: 4590 covered / 1513 missed = **75.21 %**. |

Plan DoD items this step owns:
- **Core has zero dependencies on `com.erp.app..`:** ✅ (rule 1).
- **No `*Fin*` class:** ✅ (rule 3).
- **Every `@Entity` extends the tenant-aware base or a listed global entity:** ✅ (rule 2).
- **erp-core 1.0.0 published and the reference app built against it in CI:** ⚠️ untested, as in row 1.

## Verification output

Each rule failing on its deliberate violation (`mvn -pl erp-core test -Dtest='CoreLibraryRulesArchTest,MigrationNamingTest'`, probe present, messages trimmed):

```
Tests run: 16, Failures: 13
rule1_core_never_depends_on_an_application: Rule 'rule 1: erp-core never depends on com.erp.app.. (an application)' was violated (1 times):
  Field <com.erp.cu.archprobe.ArchProbes.app> has type <com.erp.app.ArchProbeApp>
rule1_core_depends_only_on_the_allowed_libraries: Rule 'rule 1: erp-core depends only on [com.erp.., java.., ...]' was violated (1 times):
  Field <com.erp.cu.archprobe.ArchProbes.hikari> has type <com.zaxxer.hikari.HikariDataSource>
rule2_every_entity_extends_an_auditable_base: Rule 'rule 2: every @Entity extends AuditableEntity (tenant-scoped) or GlobalAuditableEntity' was violated (1 times):
  Class <com.erp.cu.archprobe.ArchProbes$ArchProbeEntity> is not assignable to com.erp.common.domain.GlobalAuditableEntity
rule2_only_the_listed_entities_are_global: rule 2: only [...ModuleRegistry, ...ActionRegistry, ...Tenant, ...AppConfiguration, ...ScreenRegistry] may extend GlobalAuditableEntity directly (1 times):
  com.erp.cu.archprobe.ArchProbes$ArchProbeGlobal extends GlobalAuditableEntity but is not one of [...] - a tenant-scoped entity extends AuditableEntity
rule3_no_fin_class_and_no_fin_package_also_in_tests: rule 3: the fin module never comes back (no class named .*Fin.*, no package com.erp.fin) (1 times):
  com.erp.cu.archprobe.ArchProbes$ArchProbeFinThing: class name matches .*Fin.*
rule4_no_value_injection_of_core_configuration: rule 4: no @Value("${app.…}") / @Value("${file.…}") (nor ${erp.core.…}) - all core configuration is bound through ErpCoreProperties (1 times):
  com.erp.cu.archprobe.ArchProbes.legacyKey injects @Value("${app.something}")
rule5_controllers_live_in_controller_packages: Rule 'rule 5: @Controller/@RestController classes live only under ..controller..' was violated (1 times):
  Class <com.erp.cu.archprobe.ArchProbes$ArchProbeRest> does not reside in a package '..controller..'
rule5_preauthorize_references_its_own_modules_permissions_class: rule 5: @PreAuthorize references a *Permissions class of its own module (2 times):
  com.erp.cu.archprobe.ArchProbes.guarded(): authority spelled as a string literal in "hasAuthority('PERM_LITERAL') or hasAuthority(T(com.erp.cu.archprobe.ArchProbes).FOO)" - reference a constant of the module's *Permissions class
  com.erp.cu.archprobe.ArchProbes.guarded(): T(com.erp.cu.archprobe.ArchProbes) is not a *Permissions class (nor a documented exception: a ReportProvider's AUTHORITY or a value enum)
rule6_no_enable_scheduling: Rule 'rule 6: no @EnableScheduling in erp-core (scheduling is the application's decision)' was violated (1 times):
  Class <com.erp.cu.archprobe.ArchProbes$ArchProbeScheduling> is annotated with @EnableScheduling
rule6_no_spring_boot_application: Rule 'rule 6: no @SpringBootApplication in erp-core (it is a library)' was violated (1 times):
  Class <com.erp.cu.archprobe.ArchProbes$ArchProbeBoot> is annotated with @SpringBootApplication
rule7_native_queries_only_in_tenant_sequence_audit: rule 7: no native SQL outside [com.erp.tenant.., com.erp.sequence.., com.erp.audit..] (native SQL bypasses Hibernate's tenant discriminator) (1 times):
  com.erp.cu.archprobe.ArchProbes$ArchProbeRepo.one(): @Query(nativeQuery = true)
rule7_raw_jdbc_only_in_documented_places: rule 7: raw JDBC only in [com.erp.tenant.., com.erp.sequence.., com.erp.audit.., com.erp.*.tenant.., com.erp.autoconfigure..] and [com.erp.notif.service.NotificationRequeueJob] (1 times):
  com.erp.cu.archprobe.ArchProbes uses org.springframework.jdbc.core.JdbcTemplate
MigrationNamingTest.coreScriptsAfterTheBaseline_areAdditiveOnly:
  Expecting empty but was: ["V16__core_arch_probe.sql: non-additive DDL 'DROP COLUMN' (core scripts after V9 are additive only: fix forward with a new table/column instead)"]
```

After the probe was removed, on the real code:
```
CoreLibraryRulesArchTest tests="12" errors="0" failures="0"
CrossModuleBoundaryArchTest tests="5" errors="0" failures="0"
MigrationNamingTest tests="4" errors="0" failures="0"
```

Enforcer (`mvn -B validate`):
```
enforce-java-21:              RequireJavaVersion passed
enforce-dependency-hygiene:   BanDuplicatePomDependencyVersions passed, RequireUpperBoundDeps passed   (erp-platform, erp-core, erp-app-reference)
```

Full build at version 1.0.0 (embedded PostgreSQL fallback; no Docker here):
```
$ rm -rf target erp-core/target erp-app-reference/target; mvn -q verify
EXIT=0 secs=234
erp-core          tests=350 failures=0 errors=0 skipped=0   (336 before + 12 CoreLibraryRulesArchTest + 2 MigrationNamingTest)
erp-app-reference tests=10  failures=0 errors=0 skipped=0
erp-core JaCoCo lines covered 4590 missed 1513 ratio 75.21%   (jacoco:check: All coverage checks have been met)
artifacts: erp-core-1.0.0.jar, erp-core-1.0.0-tests.jar, erp-app-reference-1.0.0.jar
```

CI yaml syntax (`python -c "import yaml; yaml.safe_load(...)"`):
```
jobs: ['build-test', 'docker-image', 'publish', 'consume-published']
on: {'push': {'branches': ['main'], 'tags': ['v*']}, 'pull_request': None, 'workflow_dispatch': None}
publish needs= ['build-test', 'docker-image'] if= startsWith(github.ref, 'refs/tags/v') perms= {'contents': 'read', 'packages': 'write'}
consume-published needs= publish perms= {'contents': 'read', 'packages': 'read'}
uses: actions/checkout@v4, actions/setup-java@v4, actions/upload-artifact@v4 only
```

Local publish → consume simulation, with a file repository in place of GitHub Packages:
```
$ mvn -B -q -pl erp-core -am deploy -DskipTests -DaltDeploymentRepository=local::file:///<scratch>/s12-repo
DEPLOY_EXIT=0
s12-repo/com/erp/erp-core/1.0.0/erp-core-1.0.0.jar, erp-core-1.0.0-tests.jar, erp-core-1.0.0.pom
s12-repo/com/erp/erp-platform/1.0.0/erp-platform-1.0.0.pom
$ rm -rf ~/.m2/repository/com/erp/{erp-core,erp-platform}/1.0.0
$ # erp-app-reference/{pom.xml,src} copied alone into <scratch>/s12-consume/x (no ../pom.xml, no erp-core)
$ mvn -B -q -s <settings with the file repo> -f <scratch>/s12-consume/x/pom.xml verify -Derp.core.version=1.0.0
CONSUME_EXIT=0 secs=51
_remote.repositories: erp-core-1.0.0-tests.jar>localpub= / erp-core-1.0.0.jar>localpub= / erp-platform-1.0.0.pom>localpub=
com.erp.app.ReferenceApplicationSmokeTest tests="10" errors="0" skipped="0" failures="0"
```
The local repository was cleaned of the `1.0.0` entries and the scratch repository was deleted afterwards.

## Skills checked

- `.claude/skills/gov-enforce-backend-contract/SKILL.md`: compliant. No feature code was generated. Its
  entity rule A.1.1 ("extends `AuditableEntity`; exception = a declared exemption") is now enforced by
  rule 2. The declared exemptions are the five global entities. The cross-module violations it lists are
  enforced by `CrossModuleBoundaryArchTest` (unchanged) and rule 5.
- `.claude/skills/gov-validate-backend-feature/SKILL.md`: compliant. The master validation's
  "Cross-module calls" and "entity extends `AuditableEntity`" checks map to the ArchUnit suite. No feature
  was built, so its 85-rule score does not apply to this step.
- `build-*` skills: not triggered (no entity, repository, DTO, service or controller was generated).

## Notes for later steps

- **Tagging (merger / human).** Tag the `step(12): release 1.0.0 ...` commit `v1.0.0` after the merge,
  and push `main` and the tag. CI then runs `publish` and `consume-published`.
  - They are untested.
  - If `publish` fails on a token scope, check the repository setting
    *Actions → Workflow permissions* (packages: write must be allowed).
  - GitHub Packages Maven reads need a token even for a public repository.
- **Changing the architecture rules.**
  - A new third-party library used by core must be added to
    `CoreLibraryRulesArchTest.ALLOWED_DEPENDENCY_PACKAGES`, with a reason.
  - A new global entity needs a step file that names it, plus an entry in `GLOBAL_ENTITIES`.
  - New raw JDBC belongs in `tenant`, `sequence` or `audit`, or a module's `tenant` provisioning package.
- **Coverage.** The gate is 60 % and erp-core is at 75.2 %. Tests run forked in erp-core on every OS. A
  `-Dsurefire.forkCount=0` run skips the gate (no execution data).
- **Releases from now on.** Follow `docs/RELEASE.md`: a version commit, a tag, then a bump commit. `main`
  is at `1.1.0-SNAPSHOT`.
- **Step 13 (factory registration).** The released coordinates are `com.erp:erp-core:1.0.0` (plus the
  `tests` classifier), and the parent pom is `com.erp:erp-platform:1.0.0`, from
  `https://maven.pkg.github.com/hesham421/newback`.
- **Observation, out of scope and not changed.** `spring-configuration-metadata.json` lists odd keys
  such as `erp.core.security.d-e-f-a-u-l-t-public-paths`. The configuration processor picks up the
  `DEFAULT_*` list constants of `ErpCoreProperties`. These keys are harmless, but a later MINOR could
  hide them from metadata.
- **Shared files touched.**
  - `pom.xml`, `erp-core/pom.xml`, `erp-app-reference/pom.xml`;
  - `docs/DEVIATIONS.md`;
  - `db/migration/core/README.md`;
  - `MigrationNamingTest`, which moved package.
  - Not touched: `CrossModuleBoundaryArchTest`, `ErpCoreProperties`, `CORE_PACKAGE_LIST`, the i18n
    bundles, `AutoConfiguration.imports`, `TenantSchemaIntegrationTest`.

## Release commits

- **`a02e500bc3c5a737cfe3a90839012969430c6be3`**, `step(12): release 1.0.0 — ...`. Every pom is at
  `1.0.0`, and the commit contains all step-12 code and docs. **Tag this commit `v1.0.0`.**
- The next commit, `step(12): bump to 1.1.0-SNAPSHOT`, sets every pom to `1.1.0-SNAPSHOT` and adds this
  section. Re-verified:
  ```
  $ rm -rf target erp-core/target erp-app-reference/target; mvn -q verify
  EXIT=0 secs=236
  erp-core          tests=350 failures=0 errors=0 skipped=0
  erp-app-reference tests=10  failures=0 errors=0 skipped=0
  artifacts: erp-core-1.1.0-SNAPSHOT.jar, erp-core-1.1.0-SNAPSHOT-tests.jar
  ```

## Post-merge CI fix

**What failed.** After the merge (`eb4dd28`) and the `v1.0.0` tag, GitHub Actions ran `ci.yml`:
`docker-image` passed, `build-test` failed in "mvn verify" (exit 1), so `publish` and
`consume-published` were skipped. Surefire reports were uploaded but no JaCoCo report, so tests
failed. Job logs and artifacts need a token to download (403), and there is none here, so the
failing tests themselves were not visible. The only public data were the check-run annotations,
and they held nothing but "Process completed with exit code 1".

**What changed (branch `step/12-ci-fix`).**
- `.github/workflows/ci.yml` (`build-test` only):
  - the Docker step publishes the server/API version as a notice annotation;
  - `mvn -B -ntp verify` (no `-q`) runs under `set -o pipefail` and is teed to `mvn-verify.log`;
  - a new `if: failure()` step runs `.github/scripts/ci-annotate-failures.py`, which writes the root
    cause into public annotations:
    - a summary: report totals, the Maven "Failed to execute goal" line and every failed test id;
    - one annotation per failed test: file/line, message and 15 stack lines. At most 29, because
      GitHub keeps 10 annotations per level per step;
    - when no test failed (enforcer, compilation, JaCoCo gate, container start-up), the Maven
      `[ERROR]` lines instead;
  - `mvn-verify.log` joins the surefire artifact. The surefire upload already ran on `if: always()`.
- `erp-core/src/test/java/com/erp/sec/BootstrapAdminPasswordIntegrationTest.java` had an
  order-dependent assertion, a genuine Linux-only failure that was reproduced here:
  - Surefire's default `runOrder=filesystem` is alphabetical on NTFS but hash order on Linux, and
    every test class of a JVM shares one database.
  - Run with `-Dsurefire.runOrder=reversealphabetical`,
    `seededAdmin_isPendingWithThePlaceholderHash_holdsSysAdmin_andCannotLogIn` failed: its raw-SQL
    role query returned 28 `SYS_ADMIN` rows, one for the `admin` of each tenant that other classes
    had provisioned.
  - Fix: the query is now scoped to the PLATFORM tenant. The assertion is unchanged.
- Root `pom.xml`: surefire `runOrder` is pinned to `${surefire.runOrder}`, default `alphabetical`.
  CI therefore runs the same order as Windows, so a CI failure can be reproduced locally. Override
  with `-Dsurefire.runOrder=reversealphabetical|random` to probe for order dependence.
- `TestPostgres`: the embedded fallback now uses `max_connections=100`, matching the `postgres:16`
  container (zonky's default is 300).

**Checked, no finding.**
- Testcontainers 2.0.3 already includes the Docker 29 API fix from 2.0.2.
- Resource and class names match case-sensitively.
- No locale, time-zone, line-separator or path-separator assumptions in the tests.

**Verification (local, Windows, embedded PostgreSQL).**
- `mvn -q verify` on clean `target/`s: green, erp-core 350/0/0/0 and app 10/0/0/0.
- `TZ=UTC mvn -B verify -Duser.timezone=UTC -Duser.language=en -Duser.country=US -Dfile.encoding=UTF-8`:
  green, 350 + 10, BUILD SUCCESS (enforcer and JaCoCo gate included).
- `mvn -pl erp-core test` in reverse-alphabetical order and in random orders (seeds 11, 22, 33):
  350/0/0/0 each.
- An earlier probe with ICU `en-US` collation on the embedded server was also green.
- `ci.yml` parses with python yaml.
- The annotation script was exercised on synthetic reports, both with failures and with none.

**Still unknown until CI runs.** Nobody has seen the actual CI failure. The order dependence above
is the best evidence-backed cause, but other order-dependent pairs may exist that the four orders
tried here did not hit. Testcontainers-only differences (glibc `en_US.utf8` collation, database
`test` / user `test`) also cannot be exercised without Docker. If `build-test` is still red, the
new annotations name the failing tests:
`GET https://api.github.com/repos/hesham421/newback/check-runs/<job_id>/annotations`.
