# Step 01 — Decouple and delete `fin` — report

## Summary

The `fin` module is gone: its 142 main classes and 8 test classes, the SEC-side
registration test, its 18 Flyway migrations, its 30 `PERM_FIN_*` constants,
its OpenAPI group and its i18n block. The remaining 20 core migrations were
renumbered in place to a contiguous `V1..V20` (relative order preserved). The
build passes (`mvn -q -DskipTests package`, which also compiles the tests),
and the app started on a fresh, empty PostgreSQL 16 database with the `dev`
profile. Flyway applied V1..V20, health was `UP` (once the SMTP health check was
turned off), and no OpenAPI group lists a fin path. No behaviour of `sec`,
`notif`, `file`, `mdl`, `cu` or `common` was changed. The only edits outside the
deleted code were to `@Schema` examples and comments.

## Files changed

**Deleted**
- `src/main/java/com/erp/fin/**` (142 files: controller, domain, dto, entity, exception, mapper, numbering, repository, security, service, crossmodule consumers)
- `src/test/java/com/erp/fin/**` (8 files: `FinAllocationRoundingIntegrationTest`, `FinEventEntryV2IntegrationTest`, `FinReportFilterIntegrationTest`, `FinSoDCoverageIntegrationTest`, `FinYearEndContinuityIntegrationTest`, `FinYearEndCoverageIntegrationTest`, `FinYearEndFixtures`, `domain/EventTypeRuleDomainFieldAmountTest`)
- `src/test/java/com/erp/sec/FinRegistrationInSecIntegrationTest.java`
- `src/main/resources/db/migration/V22..V30__fin_*.sql`, `V33..V41__fin_*.sql` (18 files)

**Renamed (renumbered, content unchanged unless also listed as modified)**: see the renumbering table below (17 files renamed; V1–V3 keep their names).

**Modified**
- `src/main/java/com/erp/sec/permission/PermissionConstants.java`: removed the 30 `PERM_FIN_*` constants and their section comments; file-name references updated (`V11__cu_security_seed.sql`, `V19__cu_notif_file_security_seed.sql`).
- `src/main/java/com/erp/main/config/OpenApiConfig.java`: removed the `finApi()` `GroupedOpenApi` bean.
- `src/main/java/com/erp/mdl/dto/{LookupTypeCreateRequest,LookupTypeResponse,OwnerGroupResponse}.java`: `@Schema` example `"FIN"` → `"NOTIF"`.
- `src/main/java/com/erp/sec/dto/{ModuleMenuResponse,RegistryRowResponse}.java`: `@Schema` examples `"FIN"`/`"المالية"`/`"Finance"` → `"NOTIF"`/`"خدمة الإشعارات"`/`"Notification Service"`.
- `src/main/java/com/erp/sec/dto/ScreenMenuResponse.java`: `@Schema` examples `FIN_JOURNAL_ENTRIES`/`قيود اليومية`/`Journal entries` → `NOTIF_TEMPLATES`/`قوالب الإشعارات`/`Notification templates`.
- `src/main/java/com/erp/sec/service/MenuService.java`: javadoc example `PERM_FIN_PERIODS_*` → neutral `PERM_X_PERIODS_*`.
- `src/main/java/com/erp/sec/service/PasswordResetService.java`: comment file-name reference `V11__notif_email_channel_seed.sql` → `V9__notif_email_channel_seed.sql`.
- `src/test/java/com/erp/architecture/CrossModuleBoundaryArchTest.java`: removed the `com.erp.fin` module entry and its javadoc paragraph.
- `src/test/java/com/erp/sec/MenuServiceGatewayIntegrationTest.java`, `SecCoverageIntegrationTest.java`: javadoc no longer cites `PERM_FIN_*`/`com.erp.fin`/the fin seed; file-name reference `V17__sec_security_seed.sql` → `V14__sec_security_seed.sql`.
- `src/main/resources/db/migration/V14__sec_security_seed.sql`, `V16__mdl_security_seed.sql`, `V19__cu_notif_file_security_seed.sql`: header comments' file-name references updated to the new names; V19's four FIN mentions in comments neutralised. No SQL statements changed.
- `src/main/resources/db/migration/README.md`: rewritten to describe the cleaned chain plus the old→new mapping.
- `src/main/resources/i18n/messages.properties`, `messages_ar.properties`: removed the `# --- Finance / General Ledger (FIN)` block (header + 53 `FIN-*` keys each).

**Created**
- `docs/DEVIATIONS.md`
- `docs/steps/01-report.md` (this file)

**Unchanged on purpose**: `pom.xml` (no fin-only dependency), `CLAUDE.md` (no fin module lines), `governance/`, `.claude/`, `scripts/`, `erp-core-plan/execution-state.json`.

### Migration renumbering table

| Old file | New file |
|---|---|
| `V1__cu_app_configuration_schema.sql` | `V1__cu_app_configuration_schema.sql` |
| `V2__sec_security_schema.sql` | `V2__sec_security_schema.sql` |
| `V3__sec_security_seed.sql` | `V3__sec_security_seed.sql` |
| `V6__notif_schema.sql` | `V4__notif_schema.sql` |
| `V7__notif_security_seed.sql` | `V5__notif_security_seed.sql` |
| `V8__file_schema.sql` | `V6__file_schema.sql` |
| `V9__file_security_seed.sql` | `V7__file_security_seed.sql` |
| `V10__sec_bootstrap_admin_user.sql` | `V8__sec_bootstrap_admin_user.sql` |
| `V11__notif_email_channel_seed.sql` | `V9__notif_email_channel_seed.sql` |
| `V12__notif_email_templates_action_link.sql` | `V10__notif_email_templates_action_link.sql` |
| `V13__cu_security_seed.sql` | `V11__cu_security_seed.sql` |
| `V14__drop_legacy_security_schema.sql` | `V12__drop_legacy_security_schema.sql` |
| `V16__sec_schema.sql` | `V13__sec_schema.sql` |
| `V17__sec_security_seed.sql` | `V14__sec_security_seed.sql` |
| `V18__mdl_sequences.sql` | `V15__mdl_sequences.sql` |
| `V19__mdl_security_seed.sql` | `V16__mdl_security_seed.sql` |
| `V20__notif_file_lookup_data_migration.sql` | `V17__notif_file_lookup_data_migration.sql` |
| `V21__mdl_role_grants.sql` | `V18__mdl_role_grants.sql` |
| `V22`–`V30__fin_*.sql` (9 files) | deleted |
| `V31__cu_notif_file_security_seed.sql` | `V19__cu_notif_file_security_seed.sql` |
| `V32__notif_password_reset_copy.sql` | `V20__notif_password_reset_copy.sql` |
| `V33`–`V41__fin_*.sql` (9 files) | deleted |

(V4, V5 and V15 did not exist in the source chain.)

Fin content inside non-fin migrations: none found. `V18__mdl_sequences.sql` (now V15) seeds no FIN lookup types, no remaining `*_security_seed.sql`/`*_role_grants.sql` inserts a `FIN` module/screen/action row, and `V1`/`V13` (now V1/V11) contain no `FIN_*` config keys. FIN lookup types and grants lived only in the deleted `V26`/`V36` and `V24`/`V25`/`V27`/`V30`/`V33`/`V37`. The live DB check below confirms this.

## Decisions & deviations

Entries added to `docs/DEVIATIONS.md` in this step:

- [01] Acceptance runs the app against `docker run ... postgres:16` → used a natively installed PostgreSQL 16.15 (localhost:5432) with a fresh, empty scratch database `erp_step01`, dropped afterwards (Docker is not available on the execution machine; same major version, same empty-database condition).
- [01] Acceptance expects `GET /actuator/health` = `UP` with the `dev` profile → started with `--management.health.mail.enabled=false` (and `--spring.cache.type=simple`); without overrides the app starts and Flyway applies V1..V20, but overall health is `DOWN` solely because the `mail` indicator cannot authenticate to the default `smtp.gmail.com` with no credentials. Redis is not needed at startup. Making SMTP/Redis optional is step 02/03's job.
- [01] Step file says 45 `PERM_FIN_*` constants → the source held 30; all deleted.
- [01] Step file names `messages_en.properties` / `fin.*` keys → English bundle is `messages.properties`, keys are `FIN-*`; the FIN block was removed from both bundles. No fin keys in `application*.properties`.
- [01] Non-fin code mentioning FIN → `@Schema` examples switched to NOTIF; `PERM_FIN`/`com.erp.fin` comments rewritten; purely historical provenance comments (e.g. "REQ-FIN-015") left untouched.
- [01] Stale version numbers in comments after renumbering → full file-name references updated; bare `V<n>` mentions left; mapping recorded in the migration README and here.
- [01] Task 6 → `CLAUDE.md` has no fin module lines; no edit.
- [01] Task 7 → no fin-only dependency; `pom.xml` unchanged (`bucket4j-core` is unused by all code, not fin-only, kept).
- [01] `CrossModuleBoundaryArchTest` listed `com.erp.fin` → entry removed.

## Acceptance checklist

- ✅ `grep -ri "fin" src/main/java --include=*.java -l | xargs grep -l -E "com\.erp\.fin|PERM_FIN|\"FIN\""` returns nothing. Evidence: empty output, exit 123 (xargs' grep found no match).
- ✅ `ls src/main/resources/db/migration | grep -i fin` returns nothing; versions are contiguous `V1..V20`. Evidence: empty output (exit 1); contiguity check prints `contiguous V1..V20`.
- ✅ `mvn -q -DskipTests package` succeeds. Evidence: `EXIT=0`, `target/erp-system-1.0.0-SNAPSHOT.jar` built (JDK 25, as the current pom enforces).
- ✅ Application starts against an empty PostgreSQL 16 database with the `dev` profile, Flyway applies `V1..V20` with no errors, and `GET /actuator/health` returns `UP`. Evidence: `Successfully applied 20 migrations to schema "public", now at version v20`, all 20 `flyway_schema_history` rows `success=true`, 0 `ERROR` log lines, health `{"status":"UP",...}`. This ran on native PG 16.15 rather than Docker, with `--management.health.mail.enabled=false`; see deviations.
- ✅ `GET /v3/api-docs` lists no `fin` paths. Evidence: HTTP 200, 60 paths, 0 matching `fin`. All five groups (`cu` 3, `file` 8, `mdl` 9, `notif` 10, `sec` 30 paths) have 0 fin paths, and no `fin` group remains in `swagger-config`.

5/5 ✅

## Verification output

```
$ mvn -q -DskipTests package          (JAVA_HOME=openjdk-25+36)
openjdk version "25" 2025-09-16
[only JVM sun.misc.Unsafe warnings from Maven's own guice]
EXIT=0
-rw-r--r-- 1 ... 82562771 Oct  4 17:57 target/erp-system-1.0.0-SNAPSHOT.jar

$ grep -rniE "com\.erp\.fin|PERM_FIN" src || echo OK-no-fin-refs
OK-no-fin-refs

$ ls src/main/resources/db/migration | sort -V
README.md
V1__cu_app_configuration_schema.sql
V2__sec_security_schema.sql
V3__sec_security_seed.sql
V4__notif_schema.sql
V5__notif_security_seed.sql
V6__file_schema.sql
V7__file_security_seed.sql
V8__sec_bootstrap_admin_user.sql
V9__notif_email_channel_seed.sql
V10__notif_email_templates_action_link.sql
V11__cu_security_seed.sql
V12__drop_legacy_security_schema.sql
V13__sec_schema.sql
V14__sec_security_seed.sql
V15__mdl_sequences.sql
V16__mdl_security_seed.sql
V17__notif_file_lookup_data_migration.sql
V18__mdl_role_grants.sql
V19__cu_notif_file_security_seed.sql
V20__notif_password_reset_copy.sql
contiguity: contiguous V1..V20

$ createdb erp_step01   (PostgreSQL 16.15, compiled by Visual C++ build 1944, 64-bit; 0 tables)
$ DB_URL=jdbc:postgresql://localhost:5432/erp_step01 java -jar target/erp-system-1.0.0-SNAPSHOT.jar \
    --spring.profiles.active=dev --spring.cache.type=simple --management.health.mail.enabled=false
The following 1 profile is active: "dev"
Current version of schema "public": << Empty Schema >>
Successfully applied 20 migrations to schema "public", now at version v20 (execution time 00:00.567s)
Started ErpMainApplication in 10.768 seconds
ERROR lines: 0

flyway_schema_history: 1 cu app configuration schema true; 2 sec security schema true; ... ;
  19 cu notif file security seed true; 20 notif password reset copy true
FIN module rows=0 | FIN lookup types=0 | fin tables=0 | PERM_FIN actions=0 | modules=CU,FILE,MDL,NOTIF,SEC

GET /actuator/health → {"status":"UP","components":{"db":"UP","diskSpace":"UP","livenessState":"UP","ping":"UP","readinessState":"UP","ssl":"UP"}}
GET /v3/api-docs → HTTP 200, total paths: 60, fin-paths: 0
swagger-config groups: ["/v3/api-docs/cu","/v3/api-docs/file","/v3/api-docs/mdl","/v3/api-docs/notif","/v3/api-docs/sec"]
/v3/api-docs/cu: paths=3 fin-paths=0
/v3/api-docs/file: paths=8 fin-paths=0
/v3/api-docs/mdl: paths=9 fin-paths=0
/v3/api-docs/notif: paths=10 fin-paths=0
/v3/api-docs/sec: paths=30 fin-paths=0

Control run, dev profile with NO overrides (fresh DB again):
Successfully applied 20 migrations ... now at version v20
Started ErpMainApplication in 10.618 seconds
health → {"status":"DOWN","components":{"db":"UP",...,"mail":"DOWN",...}}   ← only the SMTP indicator

App stopped; scratch database dropped (pg_database count for erp_step01 = 0).
```

## Skills checked

- `.claude/skills/gov-enforce-error-handling/SKILL.md`: not applicable. Error codes were only deleted, together with the code that threw them; no error code was added or changed, so the both-bundles rule still holds (`FIN-*` was removed from both `messages.properties` and `messages_ar.properties`).
- `build-*` skills: not applicable (this step generates no code).

## Notes for later steps

- **Migration chain is now `V1..V20`.** Step 04 squashes this chain. Comments inside the files still cite pre-renumbering bare numbers (e.g. "V16 schema" = `V13__sec_schema.sql`); use the table above or the migration README.
- **dev startup**: needs only PostgreSQL. Redis is connected lazily (`management.health.redis.enabled=false`), but `spring.cache.type=redis` is the default, so cache operations would try Redis at first use. The `mail` health indicator makes `/actuator/health` `DOWN` whenever SMTP credentials are absent. Steps 02/03 should make both optional.
- `GET /v3/api-docs/<unknown-group>` (e.g. `/v3/api-docs/fin`) returns HTTP 500 `INTERNAL_ERROR` through `GlobalExceptionHandler` instead of 404. This is existing behaviour for any unknown group, not caused by this step.
- `PermissionConstants` now holds 37 constants (SEC, MDL, CU, NOTIF, FILE). Step 06 replaces it with a pluggable catalog.
- Fin references remain **outside `src/`**, deliberately untouched here: `.claude/commands/FIN/**` (FIN execute commands), `governance/**` and `scripts/`. `pom.xml` comments mention the old `erp-finance-gl` module (history only). Step 03 removes `governance/` from the library. The `.claude/commands/FIN` commands should go with it or in step 12's "no `*Fin*`" cleanup.
- Historical "FIN" provenance comments remain in `com.erp.common` (`ErrorDetail`, `LocalizedException`, `GlobalExceptionHandler`, `CommonErrorCodes`, `AuditableEntity`, `BaseSearchContractRequest`) and `sec`/`mdl` javadoc. They contain no `com.erp.fin`/`PERM_FIN`/`"FIN"`. Step 12's ArchUnit rule targets class names, which are clean.
- The worktree branch was cut from `56d09eb`; `main` has since gained `fe61168 plan(01): dispatched` (state file only). There is no overlap with this step's files.
- `bucket4j-core` is declared but unused by any code. Step 03 can drop it when it splits the pom.
