# Step 01 — Decouple and delete `fin`

**Branch:** `step/01-decouple-fin`
**Goal:** the repository contains no `fin` code, data, seeds, permissions, tests or OpenAPI group, and still builds and runs.
**Why:** `fin` is being redeveloped later as business logic; its 20 migrations are interleaved in the single Flyway chain and `sec` statically knows 45 `PERM_FIN_*` constants. The library cannot be published while this coupling exists.

## Preconditions
- Fresh clone of source at commit `6f9f210`, new remote configured, `main` = source state.
- Docker not required for this step (tests are made runnable in step 02); use `mvn -q -DskipTests package` as the build check here.

## Inputs (facts from the audit)
- Code: `src/main/java/com/erp/fin/**` (controller, domain, dto, entity, exception, mapper, numbering, repository, security, service).
- Tests: `src/test/java/com/erp/fin/**` (7 classes + `FinYearEndFixtures`), `src/test/java/com/erp/sec/FinRegistrationInSecIntegrationTest.java`.
- Migrations under `src/main/resources/db/migration/`: `V22`–`V30`, `V33`–`V41` (`fin_*`). Also fin content inside non-fin files: `V26__fin_mdl_lookup_seed.sql`, `V36__fin_v2_mdl_lookup_seed.sql` (lookup seeds), `V24/V25/V27/V30/V33/V37` (sec grants).
- `src/main/java/com/erp/sec/permission/PermissionConstants.java`: 45 `PERM_FIN_*` constants.
- `src/main/java/com/erp/main/config/OpenApiConfig.java`: `GroupedOpenApi` for `fin`.
- Properties: any `fin.*` / `app.fin.*` keys in `application*.properties`; i18n keys `fin.*` in `src/main/resources/i18n/messages*.properties`.
- Lookup types owned by `FIN` (`MDL_LOOKUP_TYPE.OWNER_MODULE_CODE='FIN'`) seeded in V18/V26/V36.
- `scripts/`, `governance/`, `CLAUDE.md`, `.claude/` reference fin modules in registries — leave `governance/` untouched in this step (removed from the library in step 03) but edit `CLAUDE.md` only to delete fin lines.

## Tasks
1. **Delete code and tests**: remove `src/main/java/com/erp/fin`, `src/test/java/com/erp/fin`, and `src/test/java/com/erp/sec/FinRegistrationInSecIntegrationTest.java`.
2. **Permissions**: delete every `PERM_FIN_*` constant from `PermissionConstants.java`. Grep the whole tree for `PERM_FIN` and `"FIN"` string literals in Java; remove usages (expected only in `fin` and the deleted test).
3. **OpenAPI**: remove the `fin` `GroupedOpenApi` bean from `OpenApiConfig.java`.
4. **Migrations — rewrite, do not append**. Because this becomes a new project with no production data, the chain is cleaned in place:
   1. Delete all files named `V*__fin_*.sql`.
   2. Open every remaining migration and delete fin rows: in `V18__mdl_sequences.sql` (if it seeds FIN lookup types), in any `*_security_seed.sql` / `*_role_grants.sql` that inserts `SEC_MODULE_REG`/`SEC_SCREEN_REG`/`SEC_ACTION_REG` rows with module code `FIN`, and in `V13__cu_security_seed.sql`/`V38`/`V39` config seeds with fin keys (`V38`,`V39` are fin-named and already deleted; check `V1`/`V13` for `FIN_*` config keys).
   3. Renumber the remaining migrations to a contiguous sequence `V1..Vn` preserving relative order (a renumbering table goes into `docs/steps/01-report.md`). Flyway checksums are irrelevant because every environment is recreated from scratch.
   4. Update `src/main/resources/db/migration/README.md` to describe the cleaned chain.
5. **Properties & i18n**: remove `fin.*` keys from `application*.properties`; remove `fin.*` message keys from `messages_ar.properties` / `messages_en.properties`.
6. **CLAUDE.md**: delete lines that list `fin`/`FIN` as a module. Do not touch `governance/`.
7. **pom.xml**: remove dependencies used only by fin (check each dependency with `grep -r` across `src/main`; typical candidates: none — keep if unsure).
8. Create `docs/DEVIATIONS.md` (empty header) and `docs/steps/01-report.md`.

## Acceptance
- `grep -ri "fin" src/main/java --include=*.java -l | xargs grep -l -E "com\.erp\.fin|PERM_FIN|\"FIN\""` returns nothing.
- `ls src/main/resources/db/migration | grep -i fin` returns nothing; versions are contiguous `V1..Vn` with no gaps.
- `mvn -q -DskipTests package` succeeds.
- Application starts against an empty PostgreSQL 16 database (`docker run -e POSTGRES_PASSWORD=erp -p 5432:5432 postgres:16`) with the `dev` profile: Flyway applies `V1..Vn` with no errors; `GET /actuator/health` returns `UP`.
- `GET /v3/api-docs` lists no `fin` paths.

## Verification commands
```bash
mvn -q -DskipTests package
grep -rniE "com\.erp\.fin|PERM_FIN" src || echo OK-no-fin-refs
ls src/main/resources/db/migration | sort -V
```

## Commit
`step(01): remove fin module, migrations, seeds, permissions and OpenAPI group; renumber core migration chain`

## Out of scope
Changing any behaviour of `sec`, `notif`, `file`, `mdl`, `cu`, `common`. Tests are made runnable in step 02.
