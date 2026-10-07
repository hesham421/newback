# Governance vendoring report — the cleanup delta

| | |
|---|---|
| Date | 2026-10-07 |
| Decision | owner: a CLEAN, self-contained repository — only documents that describe the current logic (erp-core 1.2.0 as implemented), no governance submodule, no governance factory |
| Source | `governance-shared` at `main` @ `1087165c607ee8bac8c1c9be2598dd9415a2244d` (clean, read-only; not modified, not pushed) |
| Raw import | already on `main`: `b8ce7fa` "merge: chore(governance) project governance inside this repo" (`173cef0`): 879 files copied byte-identical from `1087165` into `governance/` with FIN/NOTE removed, the submodule, `.gitmodules`, `.governance-scope`, `scripts/governance` and the `governance-shared.yml` workflow removed, the FIN commands removed — **but** with every factory artifact still present (`packages/`, `_state/`, `_inputs/`, `test_gen/`, `api_verify/`, `P3_*`, `manifest.json`, `modules-registry.json`, `WORKSPACE.md`, `AMEND-P3-O.md`, `backend/modules/*/api-docs`, `testsprite/`), `erp-app-reference/governance/` (project-artifacts, governance-tools, mcp-servers, testsprite) still in place, the old factory `erp-app-reference/CLAUDE.md`, no root `CLAUDE.md`, the per-module factory commands and `orchestrate-module`/`generate-module-setup` still present, the skills pointing at `governance/backend/modules/<MOD>/api-docs`, `.mcp.json` unchanged. `docs/steps/v4-01-report.md` is that import's own report. |
| This branch | `chore/vendor-governance`, the cleanup delta on top of `b8ce7fa`: one history, no second vendoring |

Result: `governance/` goes from 879 files (16 MB) to **264 files (3.4 MB)** after the review fixes (§7); 762 files were removed
relative to `b8ce7fa` (appendix A lists every one), 63 moved, 18 edited, 2 added at the root
(`CLAUDE.md`, this report), plus `governance/analysis/modules/SEC/implementation-notes.md` re-vendored.

## 1. What `governance/` now holds (target layout)

| Path | Content | Files | Provenance |
|---|---|---|---|
| `governance/README.md` | what the folder is, the layout, the documentation map, what was left out | 1 | rewritten |
| `governance/rules/GOVERNANCE-RULES.md`, `api-verify-config.md`, `README.md` | skill routing, execution order, convention precedence, governance rules; the api-verify conventions — **rewritten to current conventions only**: no submodule, profile, registry, track, partition or factory text; paths point to `governance/…`, `docs/api-docs/<module>/`, `docs/test-api/` | 3 | rewritten |
| `governance/analysis/platform/PROJECT-OVERVIEW.md` | the platform as implemented: library + reference app, the eleven core packages, tenancy and realms, versions 1.0.0 → 1.2.0 → 1.3.0-SNAPSHOT, where contract and tests live. Sources: `docs/CHANGELOG.md`, `docs/CONSUMING.md`, `docs/api-docs/README.md`, the addenda | 1 | **new** (replaces the generator rendering) |
| `governance/analysis/platform/project-registry.md` | the live modules with package, analysis folder, permission module → page codes (from `*Permissions.java`), api-docs folder and operation count, test suites; analysis status per module; cross-module reads | 1 | **new** (replaces the generator registry) |
| `governance/analysis/domain/domain-profile.md` | the original domain analysis | 1 | verbatim |
| `governance/analysis/decisions/{SEC,MDL,CU,FILE,NOTIF}/` | the ADRs that still describe the current code: SEC 001–011, 035, 038 (13); MDL 002–007, 012–016, 043 (12); CU/FILE/NOTIF only `.gitkeep` | 30 | verbatim, pruned (§3) |
| `governance/analysis/modules/{SEC,MDL,CU,FILE,NOTIF}/{P0,P0_5,P1,P2}` | policies, module registry, platform summary, PRD, SRS + registry, DB script + registry — each with its "Implementation Addendum — erp-core 1.2.0" section tying the analysis to the implemented logic (SEC's P1 addendum now also lists the five as-built ADR-SEC-038 endpoints, §7) | 60 | verbatim (+5 addendum rows) |
| `governance/analysis/modules/SEC/implementation-notes.md` | the SEC implementation-notes record cited by eight erp-core classes: verbatim from `b8ce7fa`'s `erp-app-reference/governance/project-artifacts/sec-implementation-notes.md` with a three-line header (historical paths; decisions current as of 1.2.0) | 1 | re-vendored (§7) |
| `governance/analysis/modules/{FILE,NOTIF,MDL}/P2_5` | UI/UX spec + flow diagram (MDL's moved from `P3_2/`, where the generator had put it) | 6 (+2 `.gitkeep`) | verbatim |
| `governance/backend/modules/{SEC,MDL,CU,FILE,NOTIF}/test-api/` | the adapted legacy API suites (`test_<mod>_apis.py` + `<mod>_problems_report.md`) | 11 | verbatim |
| `governance/frontend/modules/{AUDIT,CU,FILE,MDL,NOTIF,PLATFORM,REPORT,SEC,SEQUENCE}/tests/` | the frontend's E2E spec archives (specs, page objects, setup, run reports) | 83 | verbatim |
| `governance/tools/api-doc-generator/` | the generator behind `docs/api-docs` — moved from `erp-app-reference/governance/governance-tools/`; `discovery.py` repointed (output `docs/api-docs/<module>/`, analysis root `governance/analysis/modules/`, no governance mirror), `generate.py` help, `README.md` paths + "In this repository" section | 61 | moved + edited |

### api-docs: verified identical, removed from `governance/`

`diff -r governance/backend/modules/<MOD>/api-docs docs/api-docs/<mod>` on the raw import for the
nine modules it held — AUDIT, CU, FILE, MDL, NOTIF, REPORT, SEC, SEQUENCE, TENANT — reported **no
difference** for any of them (the same result as against `governance-shared` @ `1087165`
directly). `docs/api-docs/` (which also holds `app/`) is the single generated source;
`docs/api-docs/_tools/generate_all.py` no longer mirrors into `governance/` (the raw import had
added a `publish_to_governance` step and a `--no-governance` flag; both are gone).

## 2. Removed relative to `b8ce7fa` (summary; every path in appendix A)

| What | Why | Files |
|---|---|---|
| `governance/analysis/modules/*/{P3_1,P3_2,P3_5_BE,_inputs,_state,api_verify,test_gen,frontend-test,manifest.json}` | plan-generator execution artifacts (backend/frontend execution plans, stage state, gate records, generated test plans, fetched inputs) | 163 |
| `governance/analysis/modules/SEC/v2/` | the service-account change set (CS-SEC-001, analysed as SEC v2) was never implemented; its five G5 endpoint declarations (`GET /sec/users/{id}`, `GET /sec/roles/{id}`, `PUT /sec/roles/{id}`, `GET /sec/roles/{id}/grants`, `POST /sec/signup-requests/search`) are the as-built endpoints recorded in ADR-SEC-038 and `docs/api-docs/sec/` and are listed in the SEC P1 addendum; the rest has no addendum — v1 is the current analysis | 96 |
| `governance/analysis/platform/system-test-{index,full-flow}-erp.md` | old monolith flows (SEC → MDL → FIN) | 2 |
| `governance/analysis/platform/{PROJECT-OVERVIEW,project-registry}.md` (generator versions) | replaced by current versions written from the code and the repo docs | (rewritten) |
| `governance/analysis/decisions/SEC/ADR-SEC-{012…034,036,037,039,040,041,042,043,044,045,046,047}.md` | §3 | 34 |
| `governance/analysis/decisions/MDL/ADR-MDL-{001,008,009,011,017…042,044}.md` | §3 | 31 |
| `governance/backend/modules/*/api-docs/` | identical to `docs/api-docs/<mod>/` (above) | 39 |
| `governance/backend/modules/*/{packages,testsprite,execution-state.json}` | factory-delivered packages, TestSprite archive, per-track state | 215 |
| `governance/frontend/modules/*/{packages,frontend-test,execution-state.json}` | factory-delivered packages and state | 87 |
| `governance/modules-registry.json`, `governance/.gitignore`, `governance/rules/{AMEND-P3-O.md,WORKSPACE.md,.gitkeep}` | the generator's registry and `_state` cache ignores; generator-era records | 5 |
| `erp-app-reference/governance/project-artifacts/` (46 files) | the whole folder: September handovers (`HANDOVER-FE-*`), FIN items (`FIN-RETAINED-EARNINGS-SEED`, `TEST-REPORT-FIN-*`, `fin-chart-of-accounts-review`, `backend/seed-scripts/fin-seed-*`), Oracle items (`oracle-aq-fin-integration-plan`, `seed-and-cutover-findings`, `backend/oracle-cutover/`), old test reports (`TEST-REPORT-PLATFORM-backend-2026-09-12`), generator fix prompts and reports (`API-DOCS-GENERATOR-*`, `PROMPT-api-docs-generator-fixes`, `generator-defect-report-and-fix-prompt`), integration notes and audits written against the monolith (`PLATFORM-MODULES-INTEGRATION-INDEX`, `platform-integration-{common,sec,file,notif}`, `integration-notifications-fileservice`, `platform-audit-widths-and-error-localization`, `INTERFACE-VS-REST-AND-POM-STRUCTURE-RECOMMENDATION` — the single-POM layout erp-core no longer has), SEC notes written against the monolith paths (`sec-alignment-report`, `sec-test-phase-consolidation-2026-09-11`; `sec-implementation-notes.md` was re-vendored under `governance/analysis/modules/SEC/` in §7 because eight erp-core classes cite it), `local-dev-commands.md` (machine-specific paths, `ErpMainApplication` and a root `src/` that no longer exist; the root `CLAUDE.md` carries the current commands), `README.md`. Nothing qualified as still accurate for the current code, so the folder is dropped | 46 |
| `erp-app-reference/governance/{README.md,.gitignore,mcp-servers/,testsprite/}` | the former mount's README, the bespoke Postgres and Oracle MCP servers, TestSprite governance/prompts/runs | 24 |
| `erp-app-reference/CLAUDE.md` | the factory-era instructions (partitions, structural law, TestSprite); replaced by the root `CLAUDE.md` | 1 |
| `erp-app-reference/scripts/{README.md,rename-project.sh}` | pre-erp-core: the script renames a root `src/main/java/com/erp` and `ErpMainApplication`, neither of which exists in the reactor | 2 |
| `.claude/commands/{CU,FILE,MDL,NOTIF,SEC}/`, `orchestrate-module.md`, `generate-module-setup.md` | factory-driven phase execution | 12 |

Kept as history, by instruction: `docs/steps/` (incl. `v4-01-report.md`), `docs/DEVIATIONS.md`,
`docs/api-docs/README.md` (two path facts updated: the generator row and the removed mirroring
step), `docs/test-api/`, `erp-core-plan/`, `INSTALL.md`, `.claude/commands/erp-core/*` (a
one-line "plan complete, governance vendored" note added at the top of each, nothing else).
`.sdkmanrc` (`java=25-tem`) is in sync with `pom.xml` (`maven.compiler.release=25`) and stays.

## 3. ADR pruning rule and result

Rule: keep an ADR only when its decision still describes the current code; drop the ones about
FIN, about the unimplemented SEC v2 change set, about the plan generator's own mechanics, or
contradicted by the code; keep when uncertain.

| Dropped | Reason |
|---|---|
| ADR-SEC-012 … 034, 037, 042, 043, 047 | the SEC v2 service-account change set — no service account, client-credentials grant, credential rotation or constant-work HMAC authentication exists in erp-core (`grep` of `erp-core/src/main/java` finds none) |
| ADR-SEC-036, 046 | the v2 backend plan restatement and the generator's AC-block checker |
| ADR-SEC-039, 040 | the generator's atom kinds / API ids for in-process read surfaces |
| ADR-SEC-041 | a guarded partial unique index on PENDING sign-ups — not in the core migration chain (`V4__sec_schema.sql` has no partial unique index) |
| ADR-SEC-044, 045 | the throttle claims — 045 ("no SEC code for the rate limit; throttling stays at ingress") is contradicted by `com.erp.sec.security.LoginRateLimiter`, and 044 ("the pre-authentication endpoints are throttled") overstates it: only customer login is throttled (`CUSTOMER_LOGIN_RATE_LIMITED` 429); staff login is not |
| ADR-MDL-001, 008, 011 | the generator's api-docs id binding (superseded chain; the current docs carry no contract ids) |
| ADR-MDL-017 … 022, 025 … 041 | review-round dialogue records (findings G1…G8, scores, verdict REVISE) of the generator's gate |
| ADR-MDL-023, 024 | test-plan coverage mechanics of the dropped `test_gen` stage |
| ADR-MDL-009 | index strategy ("the filters get an index, the flags do not") — contradicted by `V3__mdl_schema.sql` (creates `IDX_MDL_LOOKUP_TYPE_ACTIVE` and `IDX_MDL_LOOKUP_VALUE_TYPE`, no name indexes) |
| ADR-MDL-042 | "no value is created under an inactive type" — no such guard exists in `LookupValueService`/`LookupValueDomain` (recorded as a known gap, §6) |
| ADR-MDL-044 | "no free sort field is modelled" — `LookupTypeService` accepts `sortField` (`docs/api-docs/mdl/endpoints/lookup-type-management.md`) |
| ADR-MDL-010 | string precisions (`key` 50, names 200, `sort_order INTEGER`) — contradicted by `V3__mdl_schema.sql:26-29,43-46` (`key` 80, names 150, `sort_order NUMERIC`); drift recorded in the MDL P2 db-script addendum |

| Kept | Why it still holds |
|---|---|
| ADR-SEC-001 … 011 | v1 decisions: CHECK-constrained SEC lookups, the PLATFORM-STD error umbrella, `POST …/search`, api-docs without `API-*` ids, no caller-permission endpoint, no SEC lookup endpoint, screen/form/menu decisions the frontend archive still follows |
| ADR-SEC-035 | every SEC primary key from a named sequence — matches the core migrations and the convention precedence |
| ADR-SEC-038 | the by-id read endpoints the SRS screens demand — served (`GET /api/v1/sec/{users,roles}/{id}`, `SecReadOneIntegrationTest`) |
| ADR-MDL-002 … 007, 012 … 016, 043 | `POST …/search` reads, no by-id read (true of `docs/api-docs/mdl/`), the owner-module select and uniqueness pre-check behaviour the frontend archive follows |

## 4. Dependency removals and rewrites outside `governance/`

| Item | State on `b8ce7fa` | Now |
|---|---|---|
| `.mcp.json` | bespoke Node `postgres` server under `erp-app-reference/governance/mcp-servers/`, `oracle`, `TestSprite` | **`postgres` only**: `npx -y @modelcontextprotocol/server-postgres <url>`, URL from `${DB_USERNAME:-postgres}`, `${DB_PASSWORD:-postgres}`, `${DB_HOST:-localhost}`, `${DB_PORT:-5432}`, `${DB_NAME:-erp_db}`. Chosen because `uvx`/`uv`/`pipx` are not installed on the development machine while `npx` (Node 20) is, and a stdio `initialize` smoke test answered; read-only by design (READ ONLY transactions). The npm package is marked deprecated; the comment names the `uvx postgres-mcp --access-mode=restricted` swap once `uv` exists |
| `.claude/commands/generate-api-docs.md` | profile/partition workflow over `generate.py --module`, writing under `governance/backend/modules/<MOD>/api-docs/` | rewritten: `docs/api-docs/_tools/generate_all.py` over `governance/tools/api-doc-generator/`, output `docs/api-docs/<module>/`, module list = the live modules of the running app, review → write → check (+ `check_completeness.py`) → commit |
| `.claude/skills/api-verify/SKILL.md` | `governance/backend/modules/<MOD>/api-docs`, "partition", `erp-app-reference/governance/governance-tools` | `governance/rules/…`, `docs/api-docs/<module>/`, `docs/test-api/`. `README.md` and `build-create-entity` were already repointed to `governance/rules/` by `b8ce7fa` and are unchanged; coding standards unchanged |
| `.claude/commands/erp-core/*` | — | one note line each; no other edit |
| `.env.example`, `.gitignore` | TestSprite key and paths | removed |
| `erp-app-reference/docker/docker-compose.yml` | comment citing `governance/modules-registry.json` and Oracle | reworded |
| `INSTALL.md` | step-13 sentence naming the former external repos | reworded to the vendored governance |
| `docs/api-docs/README.md`, `docs/api-docs/_tools/generate_all.py` | generator path under `erp-app-reference/governance/`, mirroring into `governance/backend/modules/` | path updated; mirroring removed |
| root `CLAUDE.md` | absent | written (170 lines): what the repo is, versions, JDK 25, build/run/test, structure, documentation map, skills routing, migrations, release, housekeeping, the explicit "no governance submodule and no governance factory" statement |

Not touched: Java sources and migrations, `pom.xml`, `ci.yml`, `erp-core-plan/`, `docs/steps/`,
`docs/DEVIATIONS.md`, `docs/test-api/`.

## 5. Verification

| Check | Result |
|---|---|
| `mvn -q verify` on JDK 25 (`target/` deleted first; no Docker → embedded PostgreSQL) | exit 0 — `erp-core` 390 tests, 0 failures, 0 errors, 0 skipped · `erp-app-reference` 10 tests, 0 failures, 0 errors, 0 skipped |
| `.github/workflows/ci.yml` parses (PyYAML) | OK: `build-test`, `docker-image`, `publish`, `consume-published` |
| api-doc-generator unit tests from `governance/tools/api-doc-generator/` | 59 OK with `PYTHONUTF8=1`; without it one pre-existing test (`CommentsAreNotCode.test_comment_with_public_between_mapping_and_method`) writes its fixture in the Windows locale encoding and cannot read it back as UTF-8 — present before the move |
| `docs/api-docs/_tools/generate_all.py` imports the moved generator; `discovery.default_output_dir('SEC')` → `docs/api-docs/sec` | OK |
| `git status --short` after the commits | empty |

### Grep gates

Terms: `governance/shared`, `profile-summary`, `modules-registry`, `$GOV`, `GOV_`, `factory`,
`testsprite`, `FIN_` / `com.erp.fin`, `oracle` (case-insensitive), repository-wide excluding `.git/`
and `target/`, with the allowed historical set (`docs/steps/`, `docs/DEVIATIONS.md`,
`erp-core-plan/`, `governance/README.md`, this report) left out.

**Zero hits** in every file that drives anything: `.mcp.json`, `.github/`, `.claude/commands/`,
`.claude/skills/README.md` and `api-verify`, `governance/rules/`, `governance/analysis/platform/`,
`governance/tools/api-doc-generator/{discovery,generate}.py`, `docs/api-docs/_tools/`,
`docs/api-docs/README.md`, `.env.example`, `.gitignore`, the poms, the Dockerfile and compose
file, `INSTALL.md`.

Residual matches, all classified — none is a dependency:

| Where | Matches | Classification |
|---|---|---|
| root `CLAUDE.md` | 1 "factory" | the owner-mandated sentence "No governance submodule and no governance factory" |
| `.claude/skills/build-create-entity/SKILL.md` | "Static factory `create(...)`/`from(...)`" | the coding standard's own wording (unchanged by instruction) |
| `erp-core/src/**`, `erp-app-reference/src/**`, `erp-app-reference/pom.xml` | 179 `…Factory`/"factory method", 2 "no token oracle", `FIN_PERIODS` Javadoc example, 2 "TestSprite" in `SecCoverageIntegrationTest`'s Javadoc, `com.erp.fin`/`fin_` in the ArchUnit and naming rules that **forbid** FIN | Java identifiers, a cryptography term, a Javadoc example, a historical Javadoc, and the rules enforcing FIN's absence. Java sources were deliberately not edited |
| `docs/test-api/core-test-plan.md`, `core-verify-report.md` | `com.erp.fin`, `fin_` | the rows citing the rule that forbids FIN code |
| `governance/tools/api-doc-generator/**` | `default_factory`, "`ServiceResult` factory calls", `FIN_409_X` in a docstring example | Python/Java terms and a docstring example |
| `governance/analysis/**` (9 files) | 10 "factory" (the analysis lane that wrote them), 6 "Oracle" (the legacy system named in the domain profile and FILE's module registry), 4 `FIN_` (permission codes cited by SEC's P0/P1) | verbatim analysis of record |
| `governance/backend/**` (6 files) | `default_factory`, one `FIN_PAYMENT_METHOD` note, two "TestSprite" lines in the SEC suite header | verbatim legacy suites |
| `governance/frontend/**` (28 files) | 82 `FIN_`/`PERM_FIN_*` (the run reports record FIN's removal), 27 "`governance/shared` cut from …" report header lines, one `FIN_GL_JOURNAL` unknown-screen fixture | verbatim E2E archives |

## 6. Follow-ups (not done here)

- **Frontend repository.** It still mounts `governance-shared`. Its `erp-front` plan step 02 should
  read **this repository's `docs/api-docs/<module>/`** as the API contract (identical bytes,
  verified), take `governance/frontend/modules/<MOD>/tests/` here as the archive of its own E2E
  specs and `governance/rules/GOVERNANCE-RULES.md` as the shared rules, and drop its own submodule,
  CODEOWNERS-era wording and factory commands — the frontend repository's own change.
- **Known gap (from the dropped ADR-MDL-042).** The analysis decided that no lookup value may be
  created under an inactive lookup type; erp-core 1.2.0 has no such guard in `LookupValueService`
  / `LookupValueDomain`, so a value can be created under an inactive type. Recorded here as a
  backend gap; not fixed in this change.
- **Generator test on Windows.** Pin `encoding="utf-8"` in the fixture `write_text` of
  `tests/test_security_extractor.py` so the suite passes without `PYTHONUTF8=1`.

## 7. Review fixes (same branch, one commit)

| Finding | Fix |
|---|---|
| ADR-MDL-009 contradicted by `V3__mdl_schema.sql:112-115` | dropped |
| ADR-MDL-042 has no guard in the code | dropped; known gap recorded in §6 |
| ADR-SEC-044 makes the same throttle claim as the dropped 045 (staff login has no limiter) | dropped; `project-registry.md` now says customer login is throttled (`CUSTOMER_LOGIN_RATE_LIMITED` 429), staff login is not |
| ADR-MDL-044 contradicted by `sortField` on `LookupTypeService` | dropped |
| `sec-implementation-notes.md` cited by eight live classes | re-vendored verbatim from `b8ce7fa` as `governance/analysis/modules/SEC/implementation-notes.md` (three-line header); the eight Javadoc citations repointed (`LookupTypeController:42`, `LookupTypeService:142`, `RoleModuleGrantDomain:13`, `RoleModuleGrant:32`, `SecErrorCodes:7`, `AuditLogEntryRepository:17`, `PasswordResetTokenRepository:14`, `JwtAuthenticationFilter:33`); the two FIN-plan citations (`RoleGrantService:317`, `UserRoleService:194`) now point to ADR-SEC-038 and the SEC P1 addendum; `SecCoverageIntegrationTest:62-64` now cites `docs/test-api/core-test-plan.md` and `governance/backend/modules/SEC/test-api/test_sec_apis.py`. Comment-only edits; `mvn -q verify` re-run (§5) |
| SEC v2 wording | `governance/README.md`, `project-registry.md` and §2 above now state that the service-account change set (CS-SEC-001) was never implemented and that its five G5 endpoint declarations are the as-built endpoints of ADR-SEC-038 / `docs/api-docs/sec/`; the five rows were appended inside the SEC P1 addendum's Endpoints table (`governance/analysis/modules/SEC/P1/srs-sec.md`) |
| `api-verify-config.md` realm path | `/api/v1/auth/**` → `/api/v1/sec/auth/**` |
| PostgreSQL version drift | `CLAUDE.md` and `docs/CONSUMING.md` note that 16 is the tested version while `docker-compose.yml` pins `postgres:17` (pre-existing) — use 16 |
| §4 overclaim | only `api-verify/SKILL.md` changed among the skills on this branch |
| (round 2) ADR-MDL-010 contradicted by `V3__mdl_schema.sql:26-29,43-46` (`key` 80 not 50, names 150 not 200, `sort_order NUMERIC` not INTEGER) | dropped; the drift is recorded under "Deviations from this analysis" in `governance/analysis/modules/MDL/P2/db-script-mdl.md` so the DBF widths there are not read as current |
| (round 2) "ten" classes cite the SEC notes | eight (`grep -rl implementation-notes.md erp-core/src`); wording fixed in this report and in the notes header |

Final ADR counts: SEC 13 (001–011, 035, 038), MDL 12 (002–007, 012–016, 043).

## Appendix A — every file removed relative to `b8ce7fa` (762)

```
.claude/commands/CU/execute-backend-test.md
.claude/commands/CU/execute-backend.md
.claude/commands/FILE/execute-backend-test.md
.claude/commands/FILE/execute-backend.md
.claude/commands/MDL/execute-backend-test.md
.claude/commands/MDL/execute-backend.md
.claude/commands/NOTIF/execute-backend-test.md
.claude/commands/NOTIF/execute-backend.md
.claude/commands/SEC/execute-backend-test.md
.claude/commands/SEC/execute-backend.md
.claude/commands/generate-module-setup.md
.claude/commands/orchestrate-module.md
erp-app-reference/CLAUDE.md
erp-app-reference/governance/.gitignore
erp-app-reference/governance/README.md
erp-app-reference/governance/mcp-servers/oracle/index.js
erp-app-reference/governance/mcp-servers/oracle/package-lock.json
erp-app-reference/governance/mcp-servers/oracle/package.json
erp-app-reference/governance/mcp-servers/postgres/index.js
erp-app-reference/governance/mcp-servers/postgres/package-lock.json
erp-app-reference/governance/mcp-servers/postgres/package.json
erp-app-reference/governance/project-artifacts/API-DOCS-GENERATOR-FIXES-REPORT-2026-09-23.md
erp-app-reference/governance/project-artifacts/API-DOCS-GENERATOR-IMPROVEMENTS-2026-09-23.md
erp-app-reference/governance/project-artifacts/FIN-RETAINED-EARNINGS-SEED-2026-09-23.md
erp-app-reference/governance/project-artifacts/HANDOVER-FE-fin-blockers-2026-09-19.md
erp-app-reference/governance/project-artifacts/HANDOVER-FE-fin-plan-edits-2026-09-19.md
erp-app-reference/governance/project-artifacts/HANDOVER-FE-fin-round2-2026-09-19.md
erp-app-reference/governance/project-artifacts/HANDOVER-FE-sec-frontend-gaps-2026-09-18.md
erp-app-reference/governance/project-artifacts/HANDOVER-FE-sec-logout-2026-09-18.md
erp-app-reference/governance/project-artifacts/HANDOVER-FE-sec-round2-2026-09-18.md
erp-app-reference/governance/project-artifacts/HANDOVER-FE-sec-round3-2026-09-19.md
erp-app-reference/governance/project-artifacts/HANDOVER-FE-sec-user-roles-2026-09-18.md
erp-app-reference/governance/project-artifacts/INTERFACE-VS-REST-AND-POM-STRUCTURE-RECOMMENDATION.md
erp-app-reference/governance/project-artifacts/PLATFORM-MODULES-INTEGRATION-INDEX.md
erp-app-reference/governance/project-artifacts/PROMPT-api-docs-generator-fixes.md
erp-app-reference/governance/project-artifacts/README.md
erp-app-reference/governance/project-artifacts/TEST-REPORT-FIN-backend-2026-09-12.md
erp-app-reference/governance/project-artifacts/TEST-REPORT-FIN-v2-backend-2026-09-23.md
erp-app-reference/governance/project-artifacts/TEST-REPORT-FIN-v2-backend-2026-09-24.md
erp-app-reference/governance/project-artifacts/TEST-REPORT-PLATFORM-backend-2026-09-12.md
erp-app-reference/governance/project-artifacts/backend/oracle-cutover/19c/00-as-sys-grant-19c.sql
erp-app-reference/governance/project-artifacts/backend/oracle-cutover/19c/01-aq-setup-19c.sql
erp-app-reference/governance/project-artifacts/backend/oracle-cutover/19c/02-package-19c.sql
erp-app-reference/governance/project-artifacts/backend/oracle-cutover/19c/03-triggers-19c.sql
erp-app-reference/governance/project-artifacts/backend/oracle-cutover/FLOW-REFERENCE.md
erp-app-reference/governance/project-artifacts/backend/oracle-cutover/HANDOVER.md
erp-app-reference/governance/project-artifacts/backend/oracle-cutover/legacy-accounting-stop.sql
erp-app-reference/governance/project-artifacts/backend/oracle-cutover/oracle-19c-readiness-check.sql
erp-app-reference/governance/project-artifacts/backend/oracle-cutover/oracle-aq-setup.sql
erp-app-reference/governance/project-artifacts/backend/oracle-cutover/oracle-event-emit-package.sql
erp-app-reference/governance/project-artifacts/backend/oracle-cutover/oracle-event-emit-triggers.sql
erp-app-reference/governance/project-artifacts/backend/seed-scripts/fin-seed-production.sql
erp-app-reference/governance/project-artifacts/backend/seed-scripts/fin-seed-v2-delta.sql
erp-app-reference/governance/project-artifacts/fin-chart-of-accounts-review.md
erp-app-reference/governance/project-artifacts/generator-defect-report-and-fix-prompt.md
erp-app-reference/governance/project-artifacts/integration-notifications-fileservice.md
erp-app-reference/governance/project-artifacts/local-dev-commands.md
erp-app-reference/governance/project-artifacts/oracle-aq-fin-integration-plan.md
erp-app-reference/governance/project-artifacts/platform-audit-widths-and-error-localization.md
erp-app-reference/governance/project-artifacts/platform-integration-common.md
erp-app-reference/governance/project-artifacts/platform-integration-file.md
erp-app-reference/governance/project-artifacts/platform-integration-notif.md
erp-app-reference/governance/project-artifacts/platform-integration-sec.md
erp-app-reference/governance/project-artifacts/sec-alignment-report.md
erp-app-reference/governance/project-artifacts/sec-implementation-notes.md
erp-app-reference/governance/project-artifacts/sec-test-phase-consolidation-2026-09-11.md
erp-app-reference/governance/project-artifacts/seed-and-cutover-findings.md
erp-app-reference/governance/testsprite/TESTSPRITE-GOVERNANCE.md
erp-app-reference/governance/testsprite/prompts/fix-bugs.md
erp-app-reference/governance/testsprite/prompts/rerun-tests.md
erp-app-reference/governance/testsprite/prompts/start-tests.md
erp-app-reference/governance/testsprite/runs/2026-09-11-backend-2/code_summary.yaml
erp-app-reference/governance/testsprite/runs/2026-09-11-backend-2/raw_report.md
erp-app-reference/governance/testsprite/runs/2026-09-11-backend-2/standard_prd.json
erp-app-reference/governance/testsprite/runs/2026-09-11-backend-2/test_results.json
erp-app-reference/governance/testsprite/runs/2026-09-11-backend-2/testsprite-mcp-test-report.html
erp-app-reference/governance/testsprite/runs/2026-09-11-backend-2/testsprite-mcp-test-report.md
erp-app-reference/governance/testsprite/runs/2026-09-11-backend-2/testsprite_backend_test_plan.json
erp-app-reference/governance/testsprite/runs/2026-09-11-backend/code_summary.yaml
erp-app-reference/governance/testsprite/runs/2026-09-11-backend/raw_report.md
erp-app-reference/governance/testsprite/runs/2026-09-11-backend/standard_prd.json
erp-app-reference/governance/testsprite/runs/2026-09-11-backend/test_results.json
erp-app-reference/governance/testsprite/runs/2026-09-11-backend/testsprite_backend_test_plan.json
erp-app-reference/scripts/README.md
erp-app-reference/scripts/rename-project.sh
governance/.gitignore
governance/analysis/decisions/MDL/ADR-MDL-001.md
governance/analysis/decisions/MDL/ADR-MDL-008.md
governance/analysis/decisions/MDL/ADR-MDL-009.md
governance/analysis/decisions/MDL/ADR-MDL-010.md
governance/analysis/decisions/MDL/ADR-MDL-011.md
governance/analysis/decisions/MDL/ADR-MDL-017.md
governance/analysis/decisions/MDL/ADR-MDL-018.md
governance/analysis/decisions/MDL/ADR-MDL-019.md
governance/analysis/decisions/MDL/ADR-MDL-020.md
governance/analysis/decisions/MDL/ADR-MDL-021.md
governance/analysis/decisions/MDL/ADR-MDL-022.md
governance/analysis/decisions/MDL/ADR-MDL-023.md
governance/analysis/decisions/MDL/ADR-MDL-024.md
governance/analysis/decisions/MDL/ADR-MDL-025.md
governance/analysis/decisions/MDL/ADR-MDL-026.md
governance/analysis/decisions/MDL/ADR-MDL-027.md
governance/analysis/decisions/MDL/ADR-MDL-028.md
governance/analysis/decisions/MDL/ADR-MDL-029.md
governance/analysis/decisions/MDL/ADR-MDL-030.md
governance/analysis/decisions/MDL/ADR-MDL-031.md
governance/analysis/decisions/MDL/ADR-MDL-032.md
governance/analysis/decisions/MDL/ADR-MDL-033.md
governance/analysis/decisions/MDL/ADR-MDL-034.md
governance/analysis/decisions/MDL/ADR-MDL-035.md
governance/analysis/decisions/MDL/ADR-MDL-036.md
governance/analysis/decisions/MDL/ADR-MDL-037.md
governance/analysis/decisions/MDL/ADR-MDL-038.md
governance/analysis/decisions/MDL/ADR-MDL-039.md
governance/analysis/decisions/MDL/ADR-MDL-040.md
governance/analysis/decisions/MDL/ADR-MDL-041.md
governance/analysis/decisions/MDL/ADR-MDL-042.md
governance/analysis/decisions/MDL/ADR-MDL-044.md
governance/analysis/decisions/SEC/ADR-SEC-012.md
governance/analysis/decisions/SEC/ADR-SEC-013.md
governance/analysis/decisions/SEC/ADR-SEC-014.md
governance/analysis/decisions/SEC/ADR-SEC-015.md
governance/analysis/decisions/SEC/ADR-SEC-016.md
governance/analysis/decisions/SEC/ADR-SEC-017.md
governance/analysis/decisions/SEC/ADR-SEC-018.md
governance/analysis/decisions/SEC/ADR-SEC-019.md
governance/analysis/decisions/SEC/ADR-SEC-020.md
governance/analysis/decisions/SEC/ADR-SEC-021.md
governance/analysis/decisions/SEC/ADR-SEC-022.md
governance/analysis/decisions/SEC/ADR-SEC-023.md
governance/analysis/decisions/SEC/ADR-SEC-024.md
governance/analysis/decisions/SEC/ADR-SEC-025.md
governance/analysis/decisions/SEC/ADR-SEC-026.md
governance/analysis/decisions/SEC/ADR-SEC-027.md
governance/analysis/decisions/SEC/ADR-SEC-028.md
governance/analysis/decisions/SEC/ADR-SEC-029.md
governance/analysis/decisions/SEC/ADR-SEC-030.md
governance/analysis/decisions/SEC/ADR-SEC-031.md
governance/analysis/decisions/SEC/ADR-SEC-032.md
governance/analysis/decisions/SEC/ADR-SEC-033.md
governance/analysis/decisions/SEC/ADR-SEC-034.md
governance/analysis/decisions/SEC/ADR-SEC-036.md
governance/analysis/decisions/SEC/ADR-SEC-037.md
governance/analysis/decisions/SEC/ADR-SEC-039.md
governance/analysis/decisions/SEC/ADR-SEC-040.md
governance/analysis/decisions/SEC/ADR-SEC-041.md
governance/analysis/decisions/SEC/ADR-SEC-042.md
governance/analysis/decisions/SEC/ADR-SEC-043.md
governance/analysis/decisions/SEC/ADR-SEC-044.md
governance/analysis/decisions/SEC/ADR-SEC-045.md
governance/analysis/decisions/SEC/ADR-SEC-046.md
governance/analysis/decisions/SEC/ADR-SEC-047.md
governance/analysis/modules/CU/P3_1/.gitkeep
governance/analysis/modules/CU/P3_1/backend-execution-plan.md
governance/analysis/modules/CU/P3_1/registry-exec-be-cu.md
governance/analysis/modules/CU/P3_2/.gitkeep
governance/analysis/modules/CU/P3_5_BE/.gitkeep
governance/analysis/modules/CU/P3_5_BE/backend-test-plan.md
governance/analysis/modules/CU/P3_5_BE/registry-test-be-cu.md
governance/analysis/modules/CU/P3_5_BE/test-execution-manifest.md
governance/analysis/modules/CU/_inputs/.gitkeep
governance/analysis/modules/CU/_state/.gitkeep
governance/analysis/modules/CU/api_verify/.gitkeep
governance/analysis/modules/CU/manifest.json
governance/analysis/modules/CU/test_gen/.gitkeep
governance/analysis/modules/FILE/P3_1/.gitkeep
governance/analysis/modules/FILE/P3_1/backend-execution-plan.md
governance/analysis/modules/FILE/P3_1/registry-exec-be-file.md
governance/analysis/modules/FILE/P3_2/.gitkeep
governance/analysis/modules/FILE/P3_5_BE/.gitkeep
governance/analysis/modules/FILE/P3_5_BE/backend-test-plan.md
governance/analysis/modules/FILE/P3_5_BE/registry-test-be-file.md
governance/analysis/modules/FILE/P3_5_BE/test-execution-manifest.md
governance/analysis/modules/FILE/_inputs/.gitkeep
governance/analysis/modules/FILE/_state/.gitkeep
governance/analysis/modules/FILE/api_verify/.gitkeep
governance/analysis/modules/FILE/manifest.json
governance/analysis/modules/FILE/test_gen/.gitkeep
governance/analysis/modules/MDL/P3_1/.gitkeep
governance/analysis/modules/MDL/P3_1/backend-execution-plan-mdl.md
governance/analysis/modules/MDL/P3_1/registry-exec-be-mdl.md
governance/analysis/modules/MDL/P3_2/.gitkeep
governance/analysis/modules/MDL/P3_2/frontend-execution-plan-mdl.md
governance/analysis/modules/MDL/P3_2/registry-exec-fe-mdl.md
governance/analysis/modules/MDL/_inputs/.gitkeep
governance/analysis/modules/MDL/_inputs/api-docs-mdl.md
governance/analysis/modules/MDL/_inputs/api-docs-mdl.md.meta.json
governance/analysis/modules/MDL/_state/.gitkeep
governance/analysis/modules/MDL/_state/analyze-all.json
governance/analysis/modules/MDL/_state/analyze-all.md
governance/analysis/modules/MDL/_state/analyze-gate-pass-1.json
governance/analysis/modules/MDL/_state/analyze-gate-pass-1.md
governance/analysis/modules/MDL/_state/analyze-gate-pass-2.json
governance/analysis/modules/MDL/_state/analyze-gate-pass-2.md
governance/analysis/modules/MDL/_state/analyze-pass-1.json
governance/analysis/modules/MDL/_state/analyze-pass-1.md
governance/analysis/modules/MDL/_state/analyze-pass-2.json
governance/analysis/modules/MDL/_state/analyze-pass-2.md
governance/analysis/modules/MDL/_state/analyze-stage-P0.5.json
governance/analysis/modules/MDL/_state/analyze-stage-P0.5.md
governance/analysis/modules/MDL/_state/analyze-stage-P0.json
governance/analysis/modules/MDL/_state/analyze-stage-P0.md
governance/analysis/modules/MDL/_state/analyze-stage-P1.json
governance/analysis/modules/MDL/_state/analyze-stage-P1.md
governance/analysis/modules/MDL/_state/analyze-stage-P2.json
governance/analysis/modules/MDL/_state/analyze-stage-P2.md
governance/analysis/modules/MDL/_state/analyze-stage-P3.1.json
governance/analysis/modules/MDL/_state/analyze-stage-P3.1.md
governance/analysis/modules/MDL/_state/analyze-stage-P3.2.json
governance/analysis/modules/MDL/_state/analyze-stage-P3.2.md
governance/analysis/modules/MDL/_state/analyze-stage-test-gen.json
governance/analysis/modules/MDL/_state/analyze-stage-test-gen.md
governance/analysis/modules/MDL/_state/approvals/prd-approval.json
governance/analysis/modules/MDL/_state/briefs/P0.5.md
governance/analysis/modules/MDL/_state/briefs/P0.md
governance/analysis/modules/MDL/_state/briefs/P1.md
governance/analysis/modules/MDL/_state/briefs/P1.response1.md
governance/analysis/modules/MDL/_state/briefs/P2.md
governance/analysis/modules/MDL/_state/briefs/P2.response1.md
governance/analysis/modules/MDL/_state/briefs/P3.1.md
governance/analysis/modules/MDL/_state/briefs/P3.1.response1.md
governance/analysis/modules/MDL/_state/briefs/P3.2.md
governance/analysis/modules/MDL/_state/briefs/P3.2.response1.md
governance/analysis/modules/MDL/_state/briefs/gate-pass-1.md
governance/analysis/modules/MDL/_state/briefs/gate-pass-2-round2.md
governance/analysis/modules/MDL/_state/briefs/gate-pass-2-round2.response2.md
governance/analysis/modules/MDL/_state/briefs/gate-pass-2.md
governance/analysis/modules/MDL/_state/briefs/gate-pass-2.response1.md
governance/analysis/modules/MDL/_state/briefs/gate-pass-2.result.json
governance/analysis/modules/MDL/_state/briefs/pass-1.md
governance/analysis/modules/MDL/_state/briefs/pass-2.md
governance/analysis/modules/MDL/_state/briefs/revise-pass-2.md
governance/analysis/modules/MDL/_state/briefs/revise-pass-2.response1.md
governance/analysis/modules/MDL/_state/briefs/test-gen.md
governance/analysis/modules/MDL/_state/briefs/test-gen.response1.md
governance/analysis/modules/MDL/_state/gate-pass-1.json
governance/analysis/modules/MDL/_state/gate-pass-1.md
governance/analysis/modules/MDL/_state/gate-pass-2.json
governance/analysis/modules/MDL/_state/gate-pass-2.md
governance/analysis/modules/MDL/api_verify/.gitkeep
governance/analysis/modules/MDL/manifest.json
governance/analysis/modules/MDL/test_gen/.gitkeep
governance/analysis/modules/MDL/test_gen/backend-test-plan-mdl.md
governance/analysis/modules/MDL/test_gen/frontend-test-plan-mdl.md
governance/analysis/modules/MDL/test_gen/test-execution-manifest-mdl.md
governance/analysis/modules/NOTIF/P3_1/.gitkeep
governance/analysis/modules/NOTIF/P3_1/backend-execution-plan.md
governance/analysis/modules/NOTIF/P3_1/registry-exec-be-notif.md
governance/analysis/modules/NOTIF/P3_2/.gitkeep
governance/analysis/modules/NOTIF/P3_5_BE/.gitkeep
governance/analysis/modules/NOTIF/P3_5_BE/backend-test-plan.md
governance/analysis/modules/NOTIF/P3_5_BE/registry-test-be-notif.md
governance/analysis/modules/NOTIF/P3_5_BE/test-execution-manifest.md
governance/analysis/modules/NOTIF/_inputs/.gitkeep
governance/analysis/modules/NOTIF/_state/.gitkeep
governance/analysis/modules/NOTIF/api_verify/.gitkeep
governance/analysis/modules/NOTIF/manifest.json
governance/analysis/modules/NOTIF/test_gen/.gitkeep
governance/analysis/modules/SEC/P3_1/.gitkeep
governance/analysis/modules/SEC/P3_1/backend-execution-plan-sec.md
governance/analysis/modules/SEC/P3_1/registry-exec-be-sec.md
governance/analysis/modules/SEC/P3_2/.gitkeep
governance/analysis/modules/SEC/_inputs/.gitkeep
governance/analysis/modules/SEC/_inputs/api-docs-binding-sec.md
governance/analysis/modules/SEC/_inputs/api-docs-sec.md
governance/analysis/modules/SEC/_inputs/api-docs-sec.md.meta.json
governance/analysis/modules/SEC/_state/.gitkeep
governance/analysis/modules/SEC/_state/analyze-all.json
governance/analysis/modules/SEC/_state/analyze-all.md
governance/analysis/modules/SEC/_state/analyze-gate-pass-1.json
governance/analysis/modules/SEC/_state/analyze-gate-pass-1.md
governance/analysis/modules/SEC/_state/analyze-gate-pass-2.json
governance/analysis/modules/SEC/_state/analyze-gate-pass-2.md
governance/analysis/modules/SEC/_state/analyze-stage-P-1.json
governance/analysis/modules/SEC/_state/analyze-stage-P-1.md
governance/analysis/modules/SEC/_state/analyze-stage-P0.5.json
governance/analysis/modules/SEC/_state/analyze-stage-P0.5.md
governance/analysis/modules/SEC/_state/analyze-stage-P0.json
governance/analysis/modules/SEC/_state/analyze-stage-P0.md
governance/analysis/modules/SEC/_state/analyze-stage-P1.json
governance/analysis/modules/SEC/_state/analyze-stage-P1.md
governance/analysis/modules/SEC/_state/analyze-stage-P2.json
governance/analysis/modules/SEC/_state/analyze-stage-P2.md
governance/analysis/modules/SEC/_state/analyze-stage-P3.1.json
governance/analysis/modules/SEC/_state/analyze-stage-P3.1.md
governance/analysis/modules/SEC/_state/analyze-stage-P3.2.json
governance/analysis/modules/SEC/_state/analyze-stage-P3.2.md
governance/analysis/modules/SEC/_state/analyze-stage-domain-profile.json
governance/analysis/modules/SEC/_state/analyze-stage-domain-profile.md
governance/analysis/modules/SEC/_state/analyze-stage-test-gen.json
governance/analysis/modules/SEC/_state/analyze-stage-test-gen.md
governance/analysis/modules/SEC/_state/approvals/prd-approval.json
governance/analysis/modules/SEC/_state/briefs/P-1.md
governance/analysis/modules/SEC/_state/briefs/P0.5.md
governance/analysis/modules/SEC/_state/briefs/P0.md
governance/analysis/modules/SEC/_state/briefs/P1.md
governance/analysis/modules/SEC/_state/briefs/P2.md
governance/analysis/modules/SEC/_state/briefs/P3.1.md
governance/analysis/modules/SEC/_state/briefs/P3.2.md
governance/analysis/modules/SEC/_state/briefs/domain-profile.md
governance/analysis/modules/SEC/_state/briefs/gate-pass-1.md
governance/analysis/modules/SEC/_state/briefs/gate-pass-2.md
governance/analysis/modules/SEC/_state/briefs/pass-1.md
governance/analysis/modules/SEC/_state/briefs/pass-2.md
governance/analysis/modules/SEC/_state/briefs/test-gen.md
governance/analysis/modules/SEC/_state/gate-pass-1.json
governance/analysis/modules/SEC/_state/gate-pass-1.md
governance/analysis/modules/SEC/api_verify/.gitkeep
governance/analysis/modules/SEC/frontend-test/.gitkeep
governance/analysis/modules/SEC/frontend-test/TEST-PLAN-FE.md
governance/analysis/modules/SEC/frontend-test/_SECTIONS.md
governance/analysis/modules/SEC/frontend-test/index.md
governance/analysis/modules/SEC/frontend-test/state.json
governance/analysis/modules/SEC/frontend-test/verification.json
governance/analysis/modules/SEC/manifest.json
governance/analysis/modules/SEC/test_gen/.gitkeep
governance/analysis/modules/SEC/test_gen/backend-test-plan-sec.md
governance/analysis/modules/SEC/test_gen/frontend-test-plan-sec.md
governance/analysis/modules/SEC/test_gen/test-execution-manifest-sec.md
governance/analysis/modules/SEC/v2/.gitkeep
governance/analysis/modules/SEC/v2/P0/.gitkeep
governance/analysis/modules/SEC/v2/P0/business-policies-sec.md
governance/analysis/modules/SEC/v2/P0/module-registry-sec.md
governance/analysis/modules/SEC/v2/P0/platform-summary.md
governance/analysis/modules/SEC/v2/P0_5/.gitkeep
governance/analysis/modules/SEC/v2/P0_5/prd-sec.md
governance/analysis/modules/SEC/v2/P1/.gitkeep
governance/analysis/modules/SEC/v2/P1/registry-srs-sec.md
governance/analysis/modules/SEC/v2/P1/srs-sec.md
governance/analysis/modules/SEC/v2/P2/.gitkeep
governance/analysis/modules/SEC/v2/P2/db-script-sec.md
governance/analysis/modules/SEC/v2/P2/registry-db-sec.md
governance/analysis/modules/SEC/v2/P3_1/.gitkeep
governance/analysis/modules/SEC/v2/P3_1/backend-execution-plan-sec.md
governance/analysis/modules/SEC/v2/P3_1/registry-exec-be-sec.md
governance/analysis/modules/SEC/v2/P3_2/.gitkeep
governance/analysis/modules/SEC/v2/_inputs/.gitkeep
governance/analysis/modules/SEC/v2/_state/.gitkeep
governance/analysis/modules/SEC/v2/_state/analyze-all.json
governance/analysis/modules/SEC/v2/_state/analyze-all.md
governance/analysis/modules/SEC/v2/_state/analyze-gate-pass-1.json
governance/analysis/modules/SEC/v2/_state/analyze-gate-pass-1.md
governance/analysis/modules/SEC/v2/_state/analyze-pass-1.json
governance/analysis/modules/SEC/v2/_state/analyze-pass-1.md
governance/analysis/modules/SEC/v2/_state/analyze-stage-P0.5.json
governance/analysis/modules/SEC/v2/_state/analyze-stage-P0.5.md
governance/analysis/modules/SEC/v2/_state/analyze-stage-P0.json
governance/analysis/modules/SEC/v2/_state/analyze-stage-P0.md
governance/analysis/modules/SEC/v2/_state/analyze-stage-P1.json
governance/analysis/modules/SEC/v2/_state/analyze-stage-P1.md
governance/analysis/modules/SEC/v2/_state/analyze-stage-P2.json
governance/analysis/modules/SEC/v2/_state/analyze-stage-P2.md
governance/analysis/modules/SEC/v2/_state/analyze-stage-P3.1.json
governance/analysis/modules/SEC/v2/_state/analyze-stage-P3.1.md
governance/analysis/modules/SEC/v2/_state/approvals/feedback-pass-1.json
governance/analysis/modules/SEC/v2/_state/approvals/prd-approval.json
governance/analysis/modules/SEC/v2/_state/briefs/P0-round2.md
governance/analysis/modules/SEC/v2/_state/briefs/P0-round2.response2.md
governance/analysis/modules/SEC/v2/_state/briefs/P0-round3.md
governance/analysis/modules/SEC/v2/_state/briefs/P0-round3.response3.md
governance/analysis/modules/SEC/v2/_state/briefs/P0.5-round2.md
governance/analysis/modules/SEC/v2/_state/briefs/P0.5-round2.response2.md
governance/analysis/modules/SEC/v2/_state/briefs/P0.5-round3.md
governance/analysis/modules/SEC/v2/_state/briefs/P0.5-round3.response3.md
governance/analysis/modules/SEC/v2/_state/briefs/P0.5.md
governance/analysis/modules/SEC/v2/_state/briefs/P0.5.response1.md
governance/analysis/modules/SEC/v2/_state/briefs/P0.md
governance/analysis/modules/SEC/v2/_state/briefs/P0.response1.md
governance/analysis/modules/SEC/v2/_state/briefs/P1.md
governance/analysis/modules/SEC/v2/_state/briefs/P1.response1.md
governance/analysis/modules/SEC/v2/_state/briefs/P2.md
governance/analysis/modules/SEC/v2/_state/briefs/P2.response1.md
governance/analysis/modules/SEC/v2/_state/briefs/P3.1.md
governance/analysis/modules/SEC/v2/_state/briefs/P3.1.response1.md
governance/analysis/modules/SEC/v2/_state/briefs/gate-pass-1-round2.md
governance/analysis/modules/SEC/v2/_state/briefs/gate-pass-1-round2.response2.md
governance/analysis/modules/SEC/v2/_state/briefs/gate-pass-1.md
governance/analysis/modules/SEC/v2/_state/briefs/gate-pass-1.response1.md
governance/analysis/modules/SEC/v2/_state/briefs/gate-pass-1.result.json
governance/analysis/modules/SEC/v2/_state/briefs/revise-pass-1.md
governance/analysis/modules/SEC/v2/_state/briefs/revise-pass-1.response1.md
governance/analysis/modules/SEC/v2/_state/gate-pass-1.json
governance/analysis/modules/SEC/v2/_state/gate-pass-1.md
governance/analysis/modules/SEC/v2/_state/review-notes/ADR-SEC-041-review-note.md
governance/analysis/modules/SEC/v2/_state/review-notes/ADR-SEC-042-review-note.md
governance/analysis/modules/SEC/v2/_state/review-notes/ADR-SEC-043-review-note.md
governance/analysis/modules/SEC/v2/_state/review-notes/ADR-SEC-044-review-note.md
governance/analysis/modules/SEC/v2/_state/review-notes/ADR-SEC-045-review-note.md
governance/analysis/modules/SEC/v2/_state/review-notes/ADR-SEC-046-review-note.md
governance/analysis/modules/SEC/v2/_state/review-notes/ADR-SEC-047-review-note.md
governance/analysis/modules/SEC/v2/_state/review-notes/ADR-SEC-048-review-note.md
governance/analysis/modules/SEC/v2/_state/review-notes/ADR-SEC-049-review-note.md
governance/analysis/modules/SEC/v2/_state/review-notes/ADR-SEC-050-review-note.md
governance/analysis/modules/SEC/v2/_state/review-notes/ADR-SEC-051-review-note.md
governance/analysis/modules/SEC/v2/_state/review-notes/ADR-SEC-052-review-note.md
governance/analysis/modules/SEC/v2/_state/review-notes/ADR-SEC-053-review-note.md
governance/analysis/modules/SEC/v2/_state/review-notes/ADR-SEC-054-review-note.md
governance/analysis/modules/SEC/v2/_state/review-notes/ADR-SEC-055-review-note.md
governance/analysis/modules/SEC/v2/_state/review-notes/ADR-SEC-056-review-note.md
governance/analysis/modules/SEC/v2/_state/review-notes/ADR-SEC-057-review-note.md
governance/analysis/modules/SEC/v2/_state/review-notes/ADR-SEC-058-review-note.md
governance/analysis/modules/SEC/v2/_state/review-notes/ADR-SEC-059-review-note.md
governance/analysis/modules/SEC/v2/_state/review-notes/ADR-SEC-060-review-note.md
governance/analysis/modules/SEC/v2/_state/review-notes/ADR-SEC-061-review-note.md
governance/analysis/modules/SEC/v2/_state/review-notes/review-pass-round2-01.md
governance/analysis/modules/SEC/v2/_state/review-notes/review-pass-round2-02.md
governance/analysis/modules/SEC/v2/_state/review-notes/review-pass-round2-03.md
governance/analysis/modules/SEC/v2/_state/review-notes/review-pass-round2-04.md
governance/analysis/modules/SEC/v2/_state/review-notes/review-pass-round2-05.md
governance/analysis/modules/SEC/v2/_state/review-notes/review-pass-round2-06.md
governance/analysis/modules/SEC/v2/_state/review-notes/review-pass-round2-07.md
governance/analysis/modules/SEC/v2/api_verify/.gitkeep
governance/analysis/modules/SEC/v2/change-manifest.md
governance/analysis/modules/SEC/v2/manifest.json
governance/analysis/modules/SEC/v2/test_gen/.gitkeep
governance/analysis/platform/system-test-full-flow-erp.md
governance/analysis/platform/system-test-index-erp.md
governance/backend/modules/AUDIT/api-docs/endpoints/audit-log.md
governance/backend/modules/AUDIT/api-docs/index.md
governance/backend/modules/CU/api-docs/endpoints/configuration-management.md
governance/backend/modules/CU/api-docs/index.md
governance/backend/modules/CU/execution-state.json
governance/backend/modules/CU/packages/_agent3-state.json
governance/backend/modules/CU/packages/backend-execution/.gitkeep
governance/backend/modules/CU/packages/backend-execution/ALIGN-BE/.gitkeep
governance/backend/modules/CU/packages/backend-execution/ALIGN-BE/ALIGN-BE.md
governance/backend/modules/CU/packages/backend-execution/ALIGN-BE/index.md
governance/backend/modules/CU/packages/backend-execution/CORE/.gitkeep
governance/backend/modules/CU/packages/backend-execution/CORE/CORE.md
governance/backend/modules/CU/packages/backend-execution/CORE/index.md
governance/backend/modules/CU/packages/backend-execution/DATA-DOM/.gitkeep
governance/backend/modules/CU/packages/backend-execution/DATA-DOM/DATA-DOM.md
governance/backend/modules/CU/packages/backend-execution/DATA-DOM/index.md
governance/backend/modules/CU/packages/backend-execution/DOC/.gitkeep
governance/backend/modules/CU/packages/backend-execution/DOC/DOC.md
governance/backend/modules/CU/packages/backend-execution/DOC/index.md
governance/backend/modules/CU/packages/backend-execution/INT-C/.gitkeep
governance/backend/modules/CU/packages/backend-execution/INT-C/INT-C.md
governance/backend/modules/CU/packages/backend-execution/INT-C/index.md
governance/backend/modules/CU/packages/backend-execution/INT-R/.gitkeep
governance/backend/modules/CU/packages/backend-execution/INT-R/INT-R.md
governance/backend/modules/CU/packages/backend-execution/INT-R/index.md
governance/backend/modules/CU/packages/backend-execution/SEC-BE/.gitkeep
governance/backend/modules/CU/packages/backend-execution/SEC-BE/SEC-BE.md
governance/backend/modules/CU/packages/backend-execution/SEC-BE/index.md
governance/backend/modules/CU/packages/backend-execution/SVC-API/.gitkeep
governance/backend/modules/CU/packages/backend-execution/SVC-API/SVC-API.md
governance/backend/modules/CU/packages/backend-execution/SVC-API/index.md
governance/backend/modules/CU/packages/backend-execution/_SECTIONS.md
governance/backend/modules/CU/packages/backend-execution/index.md
governance/backend/modules/CU/packages/backend-test/.gitkeep
governance/backend/modules/CU/packages/backend-test/API-SCENARIOS.md
governance/backend/modules/CU/packages/backend-test/RULE-SCENARIOS.md
governance/backend/modules/CU/packages/backend-test/index.md
governance/backend/modules/CU/testsprite/tests/TC001_create_new_configuration_entry.py
governance/backend/modules/CU/testsprite/tests/TC002_search_configuration_entries_with_pagination.py
governance/backend/modules/CU/testsprite/tests/TC003_update_existing_configuration_entry.py
governance/backend/modules/CU/testsprite/tests/TC004_get_configuration_entry_by_key.py
governance/backend/modules/CU/testsprite/tests/TC005_deactivate_configuration_entry_by_key.py
governance/backend/modules/CU/testsprite/tests/TC009_post_api_v1_common_configurations_with_valid_data.py
governance/backend/modules/FILE/api-docs/endpoints/file-categories.md
governance/backend/modules/FILE/api-docs/endpoints/file-documents.md
governance/backend/modules/FILE/api-docs/endpoints/file-lookups.md
governance/backend/modules/FILE/api-docs/endpoints/public-files.md
governance/backend/modules/FILE/api-docs/index.md
governance/backend/modules/FILE/execution-state.json
governance/backend/modules/FILE/packages/_agent3-state.json
governance/backend/modules/FILE/packages/backend-execution/.gitkeep
governance/backend/modules/FILE/packages/backend-execution/ALIGN-BE/.gitkeep
governance/backend/modules/FILE/packages/backend-execution/ALIGN-BE/ALIGN-BE.md
governance/backend/modules/FILE/packages/backend-execution/ALIGN-BE/index.md
governance/backend/modules/FILE/packages/backend-execution/CORE/.gitkeep
governance/backend/modules/FILE/packages/backend-execution/CORE/CORE.md
governance/backend/modules/FILE/packages/backend-execution/CORE/index.md
governance/backend/modules/FILE/packages/backend-execution/DATA-DOM/.gitkeep
governance/backend/modules/FILE/packages/backend-execution/DATA-DOM/DATA-DOM.md
governance/backend/modules/FILE/packages/backend-execution/DATA-DOM/index.md
governance/backend/modules/FILE/packages/backend-execution/DOC/.gitkeep
governance/backend/modules/FILE/packages/backend-execution/DOC/DOC.md
governance/backend/modules/FILE/packages/backend-execution/DOC/index.md
governance/backend/modules/FILE/packages/backend-execution/INT-C/.gitkeep
governance/backend/modules/FILE/packages/backend-execution/INT-C/INT-C.md
governance/backend/modules/FILE/packages/backend-execution/INT-C/index.md
governance/backend/modules/FILE/packages/backend-execution/INT-R/.gitkeep
governance/backend/modules/FILE/packages/backend-execution/INT-R/INT-R.md
governance/backend/modules/FILE/packages/backend-execution/INT-R/index.md
governance/backend/modules/FILE/packages/backend-execution/SEC-BE/.gitkeep
governance/backend/modules/FILE/packages/backend-execution/SEC-BE/SEC-BE.md
governance/backend/modules/FILE/packages/backend-execution/SEC-BE/index.md
governance/backend/modules/FILE/packages/backend-execution/SVC-API/.gitkeep
governance/backend/modules/FILE/packages/backend-execution/SVC-API/SVC-API-CATEGORIES.md
governance/backend/modules/FILE/packages/backend-execution/SVC-API/SVC-API-FILES.md
governance/backend/modules/FILE/packages/backend-execution/SVC-API/SVC-API-HEADER.md
governance/backend/modules/FILE/packages/backend-execution/SVC-API/SVC-API-LOOKUP.md
governance/backend/modules/FILE/packages/backend-execution/SVC-API/index.md
governance/backend/modules/FILE/packages/backend-execution/_SECTIONS.md
governance/backend/modules/FILE/packages/backend-execution/index.md
governance/backend/modules/FILE/packages/backend-test/.gitkeep
governance/backend/modules/FILE/packages/backend-test/API-SCENARIOS.md
governance/backend/modules/FILE/packages/backend-test/RULE-SCENARIOS.md
governance/backend/modules/FILE/packages/backend-test/index.md
governance/backend/modules/FILE/testsprite/tests/TC006_create_new_file_category.py
governance/backend/modules/FILE/testsprite/tests/TC007_search_file_categories_with_pagination.py
governance/backend/modules/FILE/testsprite/tests/TC008_update_file_category_by_id.py
governance/backend/modules/FILE/testsprite/tests/TC009_upload_file_with_metadata.py
governance/backend/modules/FILE/testsprite/tests/TC010_issue_access_token_for_file_download.py
governance/backend/modules/FILE/testsprite/tests/TC010_post_api_v1_files_with_valid_multipart_data.py
governance/backend/modules/MDL/api-docs/endpoints/lookup-consumer-api.md
governance/backend/modules/MDL/api-docs/endpoints/lookup-type-management.md
governance/backend/modules/MDL/api-docs/endpoints/lookup-value-management.md
governance/backend/modules/MDL/api-docs/index.md
governance/backend/modules/MDL/execution-state.json
governance/backend/modules/MDL/packages/backend-execution/.gitkeep
governance/backend/modules/MDL/packages/backend-execution/ALIGN-BE/.gitkeep
governance/backend/modules/MDL/packages/backend-execution/ALIGN-BE/ALIGN-BE.md
governance/backend/modules/MDL/packages/backend-execution/ALIGN-BE/index.md
governance/backend/modules/MDL/packages/backend-execution/CORE/.gitkeep
governance/backend/modules/MDL/packages/backend-execution/CORE/CORE.md
governance/backend/modules/MDL/packages/backend-execution/CORE/index.md
governance/backend/modules/MDL/packages/backend-execution/DATA-DOM/.gitkeep
governance/backend/modules/MDL/packages/backend-execution/DATA-DOM/DATA-DOM.md
governance/backend/modules/MDL/packages/backend-execution/DATA-DOM/index.md
governance/backend/modules/MDL/packages/backend-execution/DOC/.gitkeep
governance/backend/modules/MDL/packages/backend-execution/DOC/DOC.md
governance/backend/modules/MDL/packages/backend-execution/DOC/index.md
governance/backend/modules/MDL/packages/backend-execution/INT-C/.gitkeep
governance/backend/modules/MDL/packages/backend-execution/INT-C/INT-C.md
governance/backend/modules/MDL/packages/backend-execution/INT-C/index.md
governance/backend/modules/MDL/packages/backend-execution/INT-R/.gitkeep
governance/backend/modules/MDL/packages/backend-execution/INT-R/INT-R.md
governance/backend/modules/MDL/packages/backend-execution/INT-R/index.md
governance/backend/modules/MDL/packages/backend-execution/SEC-BE/.gitkeep
governance/backend/modules/MDL/packages/backend-execution/SEC-BE/SEC-BE.md
governance/backend/modules/MDL/packages/backend-execution/SEC-BE/index.md
governance/backend/modules/MDL/packages/backend-execution/SVC-API/.gitkeep
governance/backend/modules/MDL/packages/backend-execution/SVC-API/SVC-API-CRUD.md
governance/backend/modules/MDL/packages/backend-execution/SVC-API/SVC-API-HEADER.md
governance/backend/modules/MDL/packages/backend-execution/SVC-API/SVC-API-SEARCH.md
governance/backend/modules/MDL/packages/backend-execution/SVC-API/index.md
governance/backend/modules/MDL/packages/backend-execution/_SECTIONS.md
governance/backend/modules/MDL/packages/backend-execution/index.md
governance/backend/modules/MDL/packages/backend-execution/state.json
governance/backend/modules/MDL/packages/backend-execution/verification.json
governance/backend/modules/MDL/packages/backend-test/.gitkeep
governance/backend/modules/MDL/packages/backend-test/API-SCENARIOS.md
governance/backend/modules/MDL/packages/backend-test/INT-XM.md
governance/backend/modules/MDL/packages/backend-test/RULE-SCENARIOS.md
governance/backend/modules/MDL/packages/backend-test/_SECTIONS.md
governance/backend/modules/MDL/packages/backend-test/index.md
governance/backend/modules/MDL/packages/backend-test/state.json
governance/backend/modules/MDL/packages/backend-test/verification.json
governance/backend/modules/NOTIF/api-docs/endpoints/notification-channels.md
governance/backend/modules/NOTIF/api-docs/endpoints/notification-dispatch.md
governance/backend/modules/NOTIF/api-docs/endpoints/notification-inbox.md
governance/backend/modules/NOTIF/api-docs/endpoints/notification-logs.md
governance/backend/modules/NOTIF/api-docs/endpoints/notification-lookups.md
governance/backend/modules/NOTIF/api-docs/endpoints/notification-templates.md
governance/backend/modules/NOTIF/api-docs/index.md
governance/backend/modules/NOTIF/execution-state.json
governance/backend/modules/NOTIF/packages/_agent3-state.json
governance/backend/modules/NOTIF/packages/backend-execution/.gitkeep
governance/backend/modules/NOTIF/packages/backend-execution/ALIGN-BE/.gitkeep
governance/backend/modules/NOTIF/packages/backend-execution/ALIGN-BE/ALIGN-BE.md
governance/backend/modules/NOTIF/packages/backend-execution/ALIGN-BE/index.md
governance/backend/modules/NOTIF/packages/backend-execution/CORE/.gitkeep
governance/backend/modules/NOTIF/packages/backend-execution/CORE/CORE.md
governance/backend/modules/NOTIF/packages/backend-execution/CORE/index.md
governance/backend/modules/NOTIF/packages/backend-execution/DATA-DOM/.gitkeep
governance/backend/modules/NOTIF/packages/backend-execution/DATA-DOM/DATA-DOM.md
governance/backend/modules/NOTIF/packages/backend-execution/DATA-DOM/index.md
governance/backend/modules/NOTIF/packages/backend-execution/DOC/.gitkeep
governance/backend/modules/NOTIF/packages/backend-execution/DOC/DOC.md
governance/backend/modules/NOTIF/packages/backend-execution/DOC/index.md
governance/backend/modules/NOTIF/packages/backend-execution/INT-C/.gitkeep
governance/backend/modules/NOTIF/packages/backend-execution/INT-C/INT-C.md
governance/backend/modules/NOTIF/packages/backend-execution/INT-C/index.md
governance/backend/modules/NOTIF/packages/backend-execution/INT-R/.gitkeep
governance/backend/modules/NOTIF/packages/backend-execution/INT-R/INT-R.md
governance/backend/modules/NOTIF/packages/backend-execution/INT-R/index.md
governance/backend/modules/NOTIF/packages/backend-execution/SEC-BE/.gitkeep
governance/backend/modules/NOTIF/packages/backend-execution/SEC-BE/SEC-BE.md
governance/backend/modules/NOTIF/packages/backend-execution/SEC-BE/index.md
governance/backend/modules/NOTIF/packages/backend-execution/SVC-API/.gitkeep
governance/backend/modules/NOTIF/packages/backend-execution/SVC-API/SVC-API.md
governance/backend/modules/NOTIF/packages/backend-execution/SVC-API/index.md
governance/backend/modules/NOTIF/packages/backend-execution/_SECTIONS.md
governance/backend/modules/NOTIF/packages/backend-execution/index.md
governance/backend/modules/NOTIF/packages/backend-test/.gitkeep
governance/backend/modules/NOTIF/packages/backend-test/API-SCENARIOS.md
governance/backend/modules/NOTIF/packages/backend-test/RULE-SCENARIOS.md
governance/backend/modules/NOTIF/packages/backend-test/index.md
governance/backend/modules/NOTIF/testsprite/tests/TC010_post_api_v1_notifications_dispatch_with_valid_and_invalid_inputs.py
governance/backend/modules/REPORT/api-docs/endpoints/reports.md
governance/backend/modules/REPORT/api-docs/index.md
governance/backend/modules/SEC/api-docs/endpoints/active-sessions.md
governance/backend/modules/SEC/api-docs/endpoints/audit-log.md
governance/backend/modules/SEC/api-docs/endpoints/authentication.md
governance/backend/modules/SEC/api-docs/endpoints/customer-accounts-public.md
governance/backend/modules/SEC/api-docs/endpoints/customer-accounts-self.md
governance/backend/modules/SEC/api-docs/endpoints/menu.md
governance/backend/modules/SEC/api-docs/endpoints/module-registry.md
governance/backend/modules/SEC/api-docs/endpoints/role-grants.md
governance/backend/modules/SEC/api-docs/endpoints/roles.md
governance/backend/modules/SEC/api-docs/endpoints/security-dashboard.md
governance/backend/modules/SEC/api-docs/endpoints/sign-up-requests.md
governance/backend/modules/SEC/api-docs/endpoints/users.md
governance/backend/modules/SEC/api-docs/index.md
governance/backend/modules/SEC/execution-state.json
governance/backend/modules/SEC/packages/backend-execution/.gitkeep
governance/backend/modules/SEC/packages/backend-execution/ALIGN-BE/.gitkeep
governance/backend/modules/SEC/packages/backend-execution/ALIGN-BE/ALIGN-BE.md
governance/backend/modules/SEC/packages/backend-execution/ALIGN-BE/index.md
governance/backend/modules/SEC/packages/backend-execution/CORE/.gitkeep
governance/backend/modules/SEC/packages/backend-execution/CORE/CORE.md
governance/backend/modules/SEC/packages/backend-execution/CORE/index.md
governance/backend/modules/SEC/packages/backend-execution/DATA-DOM/.gitkeep
governance/backend/modules/SEC/packages/backend-execution/DATA-DOM/DATA-DOM-HEADER.md
governance/backend/modules/SEC/packages/backend-execution/DATA-DOM/DATA-DOM-LOOKUP.md
governance/backend/modules/SEC/packages/backend-execution/DATA-DOM/DATA-DOM-MASTER.md
governance/backend/modules/SEC/packages/backend-execution/DATA-DOM/DATA-DOM-TRANSACTIONAL.md
governance/backend/modules/SEC/packages/backend-execution/DATA-DOM/index.md
governance/backend/modules/SEC/packages/backend-execution/DOC/.gitkeep
governance/backend/modules/SEC/packages/backend-execution/DOC/DOC.md
governance/backend/modules/SEC/packages/backend-execution/DOC/index.md
governance/backend/modules/SEC/packages/backend-execution/INT-C/.gitkeep
governance/backend/modules/SEC/packages/backend-execution/INT-C/INT-C.md
governance/backend/modules/SEC/packages/backend-execution/INT-C/index.md
governance/backend/modules/SEC/packages/backend-execution/INT-R/.gitkeep
governance/backend/modules/SEC/packages/backend-execution/INT-R/INT-R.md
governance/backend/modules/SEC/packages/backend-execution/INT-R/index.md
governance/backend/modules/SEC/packages/backend-execution/SEC-BE/.gitkeep
governance/backend/modules/SEC/packages/backend-execution/SEC-BE/SEC-BE.md
governance/backend/modules/SEC/packages/backend-execution/SEC-BE/index.md
governance/backend/modules/SEC/packages/backend-execution/SVC-API/.gitkeep
governance/backend/modules/SEC/packages/backend-execution/SVC-API/SVC-API-CRUD.md
governance/backend/modules/SEC/packages/backend-execution/SVC-API/SVC-API-HEADER.md
governance/backend/modules/SEC/packages/backend-execution/SVC-API/SVC-API-INT.md
governance/backend/modules/SEC/packages/backend-execution/SVC-API/SVC-API-SEARCH.md
governance/backend/modules/SEC/packages/backend-execution/SVC-API/index.md
governance/backend/modules/SEC/packages/backend-execution/_SECTIONS.md
governance/backend/modules/SEC/packages/backend-execution/index.md
governance/backend/modules/SEC/packages/backend-execution/state.json
governance/backend/modules/SEC/packages/backend-execution/verification.json
governance/backend/modules/SEC/packages/backend-test/.gitkeep
governance/backend/modules/SEC/packages/backend-test/API-SCENARIOS.md
governance/backend/modules/SEC/packages/backend-test/RULE-SCENARIOS.md
governance/backend/modules/SEC/packages/backend-test/_SECTIONS.md
governance/backend/modules/SEC/packages/backend-test/index.md
governance/backend/modules/SEC/packages/backend-test/state.json
governance/backend/modules/SEC/packages/backend-test/verification.json
governance/backend/modules/SEC/packages/v2/backend-execution/.gitkeep
governance/backend/modules/SEC/packages/v2/backend-execution/ALIGN-BE/.gitkeep
governance/backend/modules/SEC/packages/v2/backend-execution/CORE/.gitkeep
governance/backend/modules/SEC/packages/v2/backend-execution/DATA-DOM/.gitkeep
governance/backend/modules/SEC/packages/v2/backend-execution/DOC/.gitkeep
governance/backend/modules/SEC/packages/v2/backend-execution/INT-C/.gitkeep
governance/backend/modules/SEC/packages/v2/backend-execution/INT-R/.gitkeep
governance/backend/modules/SEC/packages/v2/backend-execution/SEC-BE/.gitkeep
governance/backend/modules/SEC/packages/v2/backend-execution/SVC-API/.gitkeep
governance/backend/modules/SEC/packages/v2/backend-test/.gitkeep
governance/backend/modules/SEC/testsprite/tests/TC001_post_api_v1_sec_auth_login_with_valid_credentials.py
governance/backend/modules/SEC/testsprite/tests/TC002_post_api_v1_sec_auth_login_with_invalid_credentials.py
governance/backend/modules/SEC/testsprite/tests/TC003_post_api_v1_sec_auth_signup_with_new_email.py
governance/backend/modules/SEC/testsprite/tests/TC004_post_api_v1_sec_auth_signup_with_duplicate_email.py
governance/backend/modules/SEC/testsprite/tests/TC005_post_api_v1_sec_auth_password_reset_request.py
governance/backend/modules/SEC/testsprite/tests/TC006_post_api_v1_sec_auth_password_reset_complete_with_valid_token.py
governance/backend/modules/SEC/testsprite/tests/TC007_post_api_v1_sec_auth_password_reset_complete_with_invalid_token.py
governance/backend/modules/SEQUENCE/api-docs/endpoints/number-series-management.md
governance/backend/modules/SEQUENCE/api-docs/index.md
governance/backend/modules/TENANT/api-docs/endpoints/platform-tenants.md
governance/backend/modules/TENANT/api-docs/index.md
governance/frontend/modules/CU/packages/frontend-execution/.gitkeep
governance/frontend/modules/CU/packages/frontend-execution/ALIGN-FE/.gitkeep
governance/frontend/modules/CU/packages/frontend-execution/F1/.gitkeep
governance/frontend/modules/CU/packages/frontend-execution/F2/.gitkeep
governance/frontend/modules/CU/packages/frontend-execution/F3/.gitkeep
governance/frontend/modules/CU/packages/frontend-execution/F4/.gitkeep
governance/frontend/modules/CU/packages/frontend-execution/SEC-FE/.gitkeep
governance/frontend/modules/CU/packages/frontend-test/.gitkeep
governance/frontend/modules/FILE/packages/frontend-execution/.gitkeep
governance/frontend/modules/FILE/packages/frontend-execution/ALIGN-FE/.gitkeep
governance/frontend/modules/FILE/packages/frontend-execution/F1/.gitkeep
governance/frontend/modules/FILE/packages/frontend-execution/F2/.gitkeep
governance/frontend/modules/FILE/packages/frontend-execution/F3/.gitkeep
governance/frontend/modules/FILE/packages/frontend-execution/F4/.gitkeep
governance/frontend/modules/FILE/packages/frontend-execution/SEC-FE/.gitkeep
governance/frontend/modules/FILE/packages/frontend-test/.gitkeep
governance/frontend/modules/MDL/execution-state.json
governance/frontend/modules/MDL/packages/frontend-execution/.gitkeep
governance/frontend/modules/MDL/packages/frontend-execution/ALIGN-FE/.gitkeep
governance/frontend/modules/MDL/packages/frontend-execution/ALIGN-FE/ALIGN-FE.md
governance/frontend/modules/MDL/packages/frontend-execution/ALIGN-FE/index.md
governance/frontend/modules/MDL/packages/frontend-execution/F1/.gitkeep
governance/frontend/modules/MDL/packages/frontend-execution/F1/F1-HEADER.md
governance/frontend/modules/MDL/packages/frontend-execution/F1/F1-SCR-MDL-001.md
governance/frontend/modules/MDL/packages/frontend-execution/F1/F1-SCR-MDL-002.md
governance/frontend/modules/MDL/packages/frontend-execution/F1/index.md
governance/frontend/modules/MDL/packages/frontend-execution/F2/.gitkeep
governance/frontend/modules/MDL/packages/frontend-execution/F2/F2-HEADER.md
governance/frontend/modules/MDL/packages/frontend-execution/F2/F2-SCR-MDL-001.md
governance/frontend/modules/MDL/packages/frontend-execution/F2/F2-SCR-MDL-002.md
governance/frontend/modules/MDL/packages/frontend-execution/F2/index.md
governance/frontend/modules/MDL/packages/frontend-execution/F3/.gitkeep
governance/frontend/modules/MDL/packages/frontend-execution/F3/F3-HEADER.md
governance/frontend/modules/MDL/packages/frontend-execution/F3/F3-SCR-MDL-001.md
governance/frontend/modules/MDL/packages/frontend-execution/F3/F3-SCR-MDL-002.md
governance/frontend/modules/MDL/packages/frontend-execution/F3/index.md
governance/frontend/modules/MDL/packages/frontend-execution/F4/.gitkeep
governance/frontend/modules/MDL/packages/frontend-execution/F4/F4-HEADER.md
governance/frontend/modules/MDL/packages/frontend-execution/F4/F4-SCR-MDL-001.md
governance/frontend/modules/MDL/packages/frontend-execution/F4/F4-SCR-MDL-002.md
governance/frontend/modules/MDL/packages/frontend-execution/F4/index.md
governance/frontend/modules/MDL/packages/frontend-execution/SEC-FE/.gitkeep
governance/frontend/modules/MDL/packages/frontend-execution/SEC-FE/SEC-FE.md
governance/frontend/modules/MDL/packages/frontend-execution/SEC-FE/index.md
governance/frontend/modules/MDL/packages/frontend-execution/_SECTIONS.md
governance/frontend/modules/MDL/packages/frontend-execution/index.md
governance/frontend/modules/MDL/packages/frontend-execution/state.json
governance/frontend/modules/MDL/packages/frontend-execution/verification.json
governance/frontend/modules/MDL/packages/frontend-test/.gitkeep
governance/frontend/modules/MDL/packages/frontend-test/INT-FLOW.md
governance/frontend/modules/MDL/packages/frontend-test/TEST-PLAN-FE-HEADER.md
governance/frontend/modules/MDL/packages/frontend-test/UI-FLOWS.md
governance/frontend/modules/MDL/packages/frontend-test/_SECTIONS.md
governance/frontend/modules/MDL/packages/frontend-test/index.md
governance/frontend/modules/MDL/packages/frontend-test/state.json
governance/frontend/modules/MDL/packages/frontend-test/verification.json
governance/frontend/modules/NOTIF/packages/frontend-execution/.gitkeep
governance/frontend/modules/NOTIF/packages/frontend-execution/ALIGN-FE/.gitkeep
governance/frontend/modules/NOTIF/packages/frontend-execution/F1/.gitkeep
governance/frontend/modules/NOTIF/packages/frontend-execution/F2/.gitkeep
governance/frontend/modules/NOTIF/packages/frontend-execution/F3/.gitkeep
governance/frontend/modules/NOTIF/packages/frontend-execution/F4/.gitkeep
governance/frontend/modules/NOTIF/packages/frontend-execution/SEC-FE/.gitkeep
governance/frontend/modules/NOTIF/packages/frontend-test/.gitkeep
governance/frontend/modules/SEC/execution-state.json
governance/frontend/modules/SEC/frontend-test/.gitkeep
governance/frontend/modules/SEC/frontend-test/TEST-PLAN-FE.md
governance/frontend/modules/SEC/frontend-test/_SECTIONS.md
governance/frontend/modules/SEC/frontend-test/index.md
governance/frontend/modules/SEC/frontend-test/state.json
governance/frontend/modules/SEC/frontend-test/verification.json
governance/frontend/modules/SEC/packages/frontend-execution/.gitkeep
governance/frontend/modules/SEC/packages/frontend-execution/ALIGN-FE/.gitkeep
governance/frontend/modules/SEC/packages/frontend-execution/F1/.gitkeep
governance/frontend/modules/SEC/packages/frontend-execution/F2/.gitkeep
governance/frontend/modules/SEC/packages/frontend-execution/F3/.gitkeep
governance/frontend/modules/SEC/packages/frontend-execution/F4/.gitkeep
governance/frontend/modules/SEC/packages/frontend-execution/SEC-FE/.gitkeep
governance/frontend/modules/SEC/packages/frontend-test/.gitkeep
governance/frontend/modules/SEC/packages/v2/frontend-execution/.gitkeep
governance/frontend/modules/SEC/packages/v2/frontend-execution/ALIGN-FE/.gitkeep
governance/frontend/modules/SEC/packages/v2/frontend-execution/F1/.gitkeep
governance/frontend/modules/SEC/packages/v2/frontend-execution/F2/.gitkeep
governance/frontend/modules/SEC/packages/v2/frontend-execution/F3/.gitkeep
governance/frontend/modules/SEC/packages/v2/frontend-execution/F4/.gitkeep
governance/frontend/modules/SEC/packages/v2/frontend-execution/SEC-FE/.gitkeep
governance/frontend/modules/SEC/packages/v2/frontend-test/.gitkeep
governance/modules-registry.json
governance/rules/.gitkeep
governance/rules/AMEND-P3-O.md
governance/rules/WORKSPACE.md
```

