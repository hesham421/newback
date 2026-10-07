# erp-core — platform library and reference application

This repository is the ERP platform's backend: **`erp-core`**, an auto-configured Spring Boot 4
library (`com.erp:erp-core`), and **`erp-app-reference`**, the application that consumes it and
proves the published artifact works outside the reactor. Everything the work here needs lives
inside this repository.

> **No governance submodule and no governance factory: everything this repo needs lives inside
> it; `governance-shared` @ `1087165` is a read-only historical reference.** The project's own
> governance (analysis, rules, test suites, tools) is vendored under `governance/` and pruned to
> the documents that describe the current logic — see `governance/README.md` and
> `docs/governance-vendoring-report.md`.

## Versions

| | |
|---|---|
| Reactor | `com.erp:erp-platform` `1.3.0-SNAPSHOT` (`pom.xml`, `erp-core/pom.xml`, `erp-app-reference/pom.xml` always share one version) |
| Released | `v1.2.0` (latest, 2026-10-05) · `v1.1.0` (first published) · `v1.0.0` (tagged, never published) — `docs/CHANGELOG.md`, `docs/RELEASE.md` |
| JDK | **25** — `maven.compiler.release=25`; the enforcer fails any other JDK at `validate`. `.sdkmanrc` = `java=25-tem`, CI = Temurin 25 |
| Stack | Spring Boot 4.0.1, PostgreSQL 16 (the tested version — Testcontainers and the embedded fallback; `erp-app-reference/docker/docker-compose.yml` still pins `postgres:17`, a pre-existing drift: use 16 locally), Flyway, springdoc, JJWT, bucket4j, ArchUnit, Testcontainers, JaCoCo gate ≥ 60 % lines on erp-core |
| Published to | this repository's GitHub Packages registry (`docs/CONSUMING.md`) |

## Build, run, test

Point `JAVA_HOME` at a JDK 25 before any `mvn` command.

```bash
mvn -q verify                                   # build + tests + ArchUnit + enforcer + JaCoCo gate (both modules)
ERP_TEST_DB=testcontainers mvn -q verify        # force Testcontainers (CI does); without Docker the tests
                                                # fall back to the embedded PostgreSQL automatically
mvn -q -pl erp-core verify                      # the library alone

# Local datastores (named volumes — survive `down`; only `down -v` deletes data, never run it casually)
docker compose --env-file .env -f erp-app-reference/docker/docker-compose.yml up -d

# Run the reference app (profile dev, port 7272)
mvn -pl erp-app-reference spring-boot:run -Dspring-boot.run.profiles=dev

# API contract — regenerate from the RUNNING app, then check (see .claude/commands/generate-api-docs.md)
python docs/api-docs/_tools/generate_all.py --function review     # then generate | update | check
python docs/api-docs/_tools/check_completeness.py

# Core API verification suite (black-box HTTP, against one running reference app)
python docs/test-api/core_api_verify.py --help

# api-doc-generator unit tests
python -m unittest discover -s governance/tools/api-doc-generator/tests -t governance/tools/api-doc-generator
```

Copy `.env.example` to `.env` and fill in values (`.env` is gitignored — never commit real
secrets). `.mcp.json` wires one MCP server, `postgres` (read-only inspection of the local dev
database); it expands `DB_*` from the shell, so `set -a && source .env && set +a` before
launching Claude Code, or the defaults (`postgres`/`postgres`/`erp_db` on 5432) apply.

## Repository structure

```
pom.xml                    the reactor (erp-core, erp-app-reference); versions, JDK 25 enforcer, dependency management
erp-core/                  the library: com.erp.{common,autoconfigure,events,tenant,sec,mdl,cu,file,notif,sequence,audit,report}
  src/main/resources/db/migration/core/   core Flyway chain V1..V999 (additive only)
  src/test/java/com/erp/architecture/     ArchUnit rules (library structure, module boundaries, migration naming)
erp-app-reference/         the consuming application (com.erp.app), its Dockerfile, docker/ compose, V1000+ migrations
docs/
  api-docs/<module>/       THE API CONTRACT — generated from the running app (sec, tenant, file, notif, mdl, cu,
                           sequence, audit, report, app); README.md is the module table; _tools/ the drivers
  test-api/                core API verification: plan, script, classification, report, dated results
  steps/NN-report.md       per-step reports of the erp-core plan (history — do not rewrite)
  DEVIATIONS.md            every deviation from the plan, by step (history)
  CHANGELOG.md · RELEASE.md · CONSUMING.md · governance-vendoring-report.md
governance/                the project's own governance, self-contained (governance/README.md):
  rules/                   GOVERNANCE-RULES.md (skill routing, execution order, convention precedence) · api-verify-config.md
  analysis/                platform/ (overview, registry), domain/, decisions/<MOD>/ (ADRs),
                           modules/<MOD>/{P0,P0_5,P1,P2,P2_5} for SEC, MDL, CU, FILE, NOTIF (+ erp-core 1.2.0 addenda)
  backend/modules/<MOD>/test-api/      adapted legacy API suites
  frontend/modules/<MOD>/tests/        the frontend's E2E spec archives
  tools/api-doc-generator/ the generator behind docs/api-docs
erp-core-plan/             the completed erp-core technical plan, steps 01–13 (history — no edits)
.claude/skills/            backend skills: build-* (generate), gov-* (validate), api-verify
.claude/commands/          generate-api-docs.md; erp-core/ (completed-plan history)
.github/workflows/ci.yml   build-test (JDK 25, Testcontainers), docker-image, publish (v* tags), consume-published
INSTALL.md                 how the erp-core plan was installed and run (history)
```

Module boundaries are package-based (`com.erp.<module>`) and enforced by the ArchUnit suite;
cross-module calls go through each module's `crossmodule` package. `com.erp.common.*` is the
foundation every module consumes (`AuditableEntity`, `Status`/`ServiceResult`,
`LocalizedException`, `ApiResponse`/`GlobalExceptionHandler`, the search builders, the boolean
converters, `SecurityContextHelper`, …) — the skills generate against it; never reinvent it.

## Documentation map — which document is the current reference for what

| Question | Current reference |
|---|---|
| The API contract | `docs/api-docs/<module>/` — generated, never hand-edited, the only copy |
| Behaviour and rationale per module | the "Implementation Addendum — erp-core 1.2.0" sections under `governance/analysis/modules/<MOD>/`, the ADRs under `governance/analysis/decisions/<MOD>/`, and `docs/DEVIATIONS.md` |
| The platform as implemented; the live modules, screens and page codes | `governance/analysis/platform/PROJECT-OVERVIEW.md`, `project-registry.md` |
| How to consume and configure erp-core | `docs/CONSUMING.md` |
| Release policy and what changed per version | `docs/RELEASE.md`, `docs/CHANGELOG.md` |
| Implementation history | `docs/steps/NN-report.md`, `erp-core-plan/` |
| Coding standards and skill routing | `governance/rules/GOVERNANCE-RULES.md`, `.claude/skills/` |
| HTTP verification | `governance/rules/api-verify-config.md`, `docs/test-api/`, the suites under `governance/{backend,frontend}/modules/<MOD>/` |

## Governance and skills

Before generating or modifying any code:

1. Read `governance/rules/GOVERNANCE-RULES.md` — the single copy of the skill routing table,
   execution order, convention precedence and governance rules. This file does not restate it.
2. Load the required skill from `.claude/skills/<skill-name>/SKILL.md` per that table. Two lanes
   by prefix: `build-*` generates code, in the strict order
   `gov-enforce-backend-contract` → `build-create-entity` → `build-create-repository` →
   `build-create-dto` → `build-create-mapper` → `build-create-service` →
   `build-create-controller` → `gov-validate-backend-feature`; `gov-*` validates it.
3. `api-verify` exercises a module's real API against `docs/api-docs/<module>/`; its run
   conventions are `governance/rules/api-verify-config.md`, its output goes to `docs/test-api/`.
4. Names are never invented: entity, table, column, sequence and constraint names come from the
   module's `governance/analysis/modules/<MOD>/P2/db-script*.md`, the existing migrations and
   entities. Business rules live on Domain objects (`create()`/`from()`), never inline in a
   service. No banner comments, Javadoc ≤ 5 lines, no JUnit tests unless asked.

If a governance file you need is missing, stop and report it — never invent governance content.
A new report or note that does not drive code goes under `docs/`; keep `governance/` to the
documents that describe the current logic.

The frontend is a separate repository. It reads this repository's `docs/api-docs/<module>/` as
the API contract and `governance/analysis/` as its analysis; nothing here is copied into it. Its
write set here is exactly two things, always through its own worktree branch merged to `main`:
`governance/frontend/modules/<MOD>/tests/` (its E2E archive) and the append-only
`## Implementation Addendum — frontend <version>` sections of
`governance/analysis/modules/<MOD>/P2_5/` (its own analysis-first rule, mirroring the one below).
A frontend change that needs the backend comes to this repository as a prompt and a gap row, and
goes through "Analysis first" below before any endpoint is added.

## Analysis first — every new requirement or feature (NON-NEGOTIABLE)

A new requirement, a new feature or a behaviour change is **documented in the analysis before it
is implemented, and checked against that documentation after**. Code with no analysis entry, or
an entry the code does not match, is not done. The pattern is the existing
"Implementation Addendum — erp-core 1.2.0" sections under `governance/analysis/modules/<MOD>/`.

**Before writing code** — append, never rewrite (the original analysis above an addendum stays
as it was):

1. Behaviour / requirement → the module's `P0`/`P1` document gets (or extends) an addendum headed
   `## Implementation Addendum — erp-core <target version>` (the version `main` is moving to,
   e.g. `1.3.0`), with the same header lines (`Source version`, `Steps`/`Change`, `Statement`) and
   rows labelled **NEW / CHANGED / REMOVED**. New ids continue the module's sequence
   (`REQ-<MOD>-NNN`, `AC-`, `RULE-`, `ENT-`, `DBF-`, `XM-`) from the last used number; nothing is
   renumbered.
2. Schema → `P2/db-script-<mod>.md` addendum: table, columns, types **and widths**, constraints,
   sequences, with the exact physical names the migration will use and the migration file name
   (`V<N>__…`). The migration is written from this entry, not the other way round.
3. A choice between alternatives → `governance/analysis/decisions/<MOD>/ADR-<MOD>-NNN.md`, next
   free number, same template as the existing ADRs.
4. Endpoints → listed in the addendum (method, path, required permission, error codes) so they
   can be checked against the generated `docs/api-docs/<module>/` afterwards.
5. A new module → the full `P0`, `P0_5`, `P1`, `P2` set under `governance/analysis/modules/<MOD>/`
   plus its rows in `governance/analysis/platform/project-registry.md`; a platform-wide change →
   `PROJECT-OVERVIEW.md` and `project-registry.md` as well.
6. Anything the frontend must react to (new screen, page code, permission, contract change) is
   marked in the addendum — the frontend repository reads these files.

**After the code is written** — before calling the work done:

1. Compare the code with the entry item by item: entity/table/column/sequence/constraint names
   and widths, endpoint paths, methods and permissions, error codes, status flows, migration
   number. A difference is fixed in the code; if the deviation was deliberate, the addendum is
   updated and the deviation recorded in `docs/DEVIATIONS.md`. The two are never left disagreeing
   silently — that is exactly the drift the vendoring review had to clean up (ADR-MDL-009/010/044).
2. Regenerate `docs/api-docs/<module>/` from the running app and run
   `docs/api-docs/_tools/check_completeness.py`; every endpoint in the addendum must be there.
3. Extend `docs/test-api/core-test-plan.md` (`TC-CORE-*`) and the api-verify cases for the new
   behaviour; run `gov-validate-backend-feature`.
4. Add the change to `docs/CHANGELOG.md` under the unreleased version.

The step is complete only when the addendum, the code, `docs/api-docs/`, the test plan and the
changelog all say the same thing.

## Database migrations (Flyway)

- **Core** owns `V1..V999` in `erp-core/src/main/resources/db/migration/core/` and is
  **additive only**: a new table, a nullable or defaulted column, an index or constraint existing
  data already satisfies, or seed rows. No core script renames or drops a table or column,
  changes a column type, or edits or renumbers a shipped script — a mistake is fixed forward with
  a new script. `MigrationNamingTest` enforces the naming, the range and the additive rule.
- **Applications** own `V1000+` in their own location (`erp-app-reference/src/main/resources/db/migration/`).
  Neither ever writes into the other's range.
- Naming `V<N>__<snake_case_description>.sql`, strictly sequential, one logical change per
  file. Derive the next number from the directory listing at creation time; never carry one from
  memory. Table/column naming follows `build-create-entity` (UPPER_SNAKE_CASE, module prefix); a
  migration and its JPA entity must agree on the physical name.
- Applied automatically at startup by Spring Boot's Flyway auto-configuration.

## Releasing

Semantic Versioning: MINOR for additive changes (new modules, tables, nullable columns,
endpoints, SPI methods with defaults, property keys with defaults), PATCH for contract-free
fixes, MAJOR expected never. One commit sets every pom to `X.Y.Z` and updates
`docs/CHANGELOG.md`; tag `vX.Y.Z`; CI publishes and then builds `erp-app-reference` against the
published artifact; a second commit moves `main` to `X.(Y+1).0-SNAPSHOT`. Full policy:
`docs/RELEASE.md`.

## Housekeeping

| Pattern | Action |
|---|---|
| `**/target/` | Maven output, gitignored; delete freely (`mvn -q verify` recreates it) |
| `__pycache__/`, `*.pyc` | Python bytecode from `governance/tools`, `docs/api-docs/_tools`, `docs/test-api`; gitignored, delete on sight |
| `.DS_Store`, `Thumbs.db`, IDE files | never tracked; delete on sight |
| `logs/*.log` | delete once the debugging session that produced them is over |

Keep `.gitkeep` files under `governance/analysis/**` (they hold otherwise-empty folders the
analysis structure expects) and the empty `__init__.py` files under
`governance/tools/api-doc-generator/` (Python package markers).

Historical documents — `docs/steps/`, `docs/DEVIATIONS.md`, `docs/api-docs/README.md`'s history
rows, `erp-core-plan/`, the vendored analysis — are records, not instructions: do not rewrite
them to match the present, and do not resurrect what they describe as removed (FIN, the
governance submodule, the former API test-generation service).
