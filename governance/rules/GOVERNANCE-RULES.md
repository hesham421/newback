# ERP Governance Rules

Shared governance content for every AI runtime operating on this platform
(currently Claude Code via `CLAUDE.md`; any future runtime plugs in the same way —
the repo-root `.github/` directory that once carried Copilot instructions has been
removed). This is the single copy of the skill routing table,
execution order, context references, and governance rules — runtime files must
reference this document, not restate it.

---

## Governance Content Map

| Artifact | Path | Present |
|----------|------|---------|
| Backend skills | repo-root `.claude/skills/` (not under `governance/` — moved 2026-08-31 so they auto-load via the Skill tool every session; this table still governs which skill to use and when) | ✅ |
| Modules registry | `governance/modules-registry.json` | ✅ — CU, NOTIF, FILE, **MDL** registered (MDL added 2026-09-12: backend complete through ALIGN-BE, and it is the platform's adopted lookup mechanism — every module registers its lookup types via API-MDL-002 and reads them via API-MDL-011). SEC still has no registry entry despite being fully built and live. FIN has a module folder and planning artifacts but no backend code yet — correctly unregistered. Since 2026-09-12 this file is WRITTEN BY THE GOVERNANCE FACTORY (`gov.py publish`), not by hand — the factory derives the module set and versions from its own filesystem (the version authority) and writes an identical copy into every consumer repo at `governance/modules-registry.json`. The project-root `shared/` copy is gone; each repo reads only its own. A hand-edit here is overwritten by the next publish; a description belongs in the module's factory manifest (`created_at`/`description` are preserved, never regenerated). SEC is now registered (it was built and live but missing); FIN now shows v1 — its ANALYSIS version, since the factory's authority is the analysis filesystem, not backend source. |
| AI commands | repo-root `.claude/commands/` (not under `governance/` — moved 2026-09-05 so they auto-load as Claude Code slash commands, same reason skills moved) | ✅ — `bootstrap-legacy-frontend`, `governance-tools-launcher` and `process-project-files` restored 2026-09-11 from `85d2210^` (same silent-deletion failure as this file; the `governance-tools/*.py` scripts they drive are all still present) |
| Governance automation tools | `governance/governance-tools/` | ✅ |
| Backend TestSprite governance (mechanism, prompts, module archive) | `governance/testsprite/` | ✅ rules restored 2026-09-11 (`TESTSPRITE-GOVERNANCE.md` + `prompts/`, recovered from `b9bb8a2^` after the same silent-deletion failure as this file). Dated `runs/` bundles were NOT restored — they are ephemeral output, not rules. |
| Module execution plans | `governance/modules/` | ✅ — CU, FILE, FIN, MDL, NOTIF, SEC present |
| `api-verify` run requirements (stack conventions, input/output paths — module-agnostic) | `governance/api-verify-config.md` | ✅ |
| Backend architecture context | `governance/.github/context/backend.md` | ❌ not present |
| Frontend skills | `frontend/governance/.github/skills/frontend/` — **the frontend repo, not this one** | n/a here |
| DevOps / deploy skill | the `deploy` repo | n/a here |
| Frontend architecture context | `frontend/governance/.github/context/frontend.md` — the frontend repo | n/a here |
| Frontend TestSprite governance | `frontend/governance/testsprite/` — the frontend repo | n/a here |

> ❌ rows are content this model expects but that does not exist in the repository right now. Do
> not silently substitute something else for a missing artifact, and do not invent its content —
> stop and report the gap, per `CLAUDE.md`.

---

## Required-present governance files

These artifacts are load-bearing. Work must not proceed without them:

- `governance/GOVERNANCE-RULES.md` — this file (routing table, execution order, governance rules)
- `.claude/skills/` — the ten `build-*` / `gov-*` skill files
- `governance/modules/<MOD>/execution-state.json` — for the module being executed
- `governance/modules/<MOD>/packages/` — that module's execution plan content

An agent that finds any of these missing STOPS and reports the missing artifact. It does not
proceed silently, substitute a different file, or reconstruct the content from memory. (This
file was itself deleted as collateral in commit `85d2210` and the loss went unnoticed through
an entire module implementation — that is the failure this list exists to prevent.)

---

## Task → Skill Routing

Read the matching skill BEFORE generating or modifying any code.
Backend skill files are at `backend/.claude/skills/<skill-name>/SKILL.md`, split into two lanes
by prefix: `build-*` generates code, `gov-*` validates it. Frontend and DevOps skills live in
their own repos at `.github/skills/<category>/<skill-name>/SKILL.md`.

### Backend (code lives in `backend` repo)

| Task | Skill |
|------|-------|
| **Always first — contract validation** | `gov-enforce-backend-contract` |
| Create / modify Entity | `build-create-entity` |
| Create / modify Repository | `build-create-repository` |
| Create / modify DTOs | `build-create-dto` |
| Create / modify Mapper | `build-create-mapper` |
| Create / modify Service | `build-create-service` |
| Create / modify Controller | `build-create-controller` |
| Review / validate backend code | `gov-enforce-backend-contract` |
| Add / review caching | `gov-enforce-caching-rules` |
| Add / review error handling | `gov-enforce-error-handling` |
| Validate a complete feature | `gov-validate-backend-feature` |
| Verify a module's live API against its api-docs (post-implementation, on demand, never a gate) | `api-verify` — run requirements (stack conventions, input/output paths) live in `governance/api-verify-config.md`, not in the skill itself |

### Frontend (code lives in `frontend` repo)

> Precedence when general React guidance (react.dev, community convention,
> an external library's docs) appears to conflict with a project rule: see
> `erp-priority-override`.

| Task | Skill |
|------|-------|
| Create / modify TS DTO types, Zod form schema, FormMapper | `create-models` |
| Create / modify the HTTP client / a feature's typed API module | `create-api-client` |
| Create / modify TanStack Query hooks (reads + mutations) | `create-queries` |
| Create / modify entry forms (React Hook Form + Zod) | `create-forms` |
| Create / modify feature UI (columns, list page, entry page) | `create-components` |
| Create / modify routing (route tree, guards, navigation model) | `create-routing` |
| Create / modify cross-cutting client state (Language/Auth Context) | `create-app-state` |
| Create / modify the auth/session layer (tokens, refresh, login/logout) | `create-auth-session` |
| Create / modify confirm handlers for destructive/state-changing actions | `create-confirm-actions` |
| Create / modify the error architecture (taxonomy, boundaries, mapping) | `create-error-handling` |
| Create / modify the test suite (Vitest, MSW, RTL, Playwright) | `create-tests` |
| Review frontend architecture | `enforce-frontend-architecture` |
| Review UI/UX, design tokens, i18n & accessibility | `enforce-ui-ux` |
| Review code reusability | `enforce-reusability` |
| Review permissions | `enforce-permissions` |
| Review state management | `enforce-state-management` |
| Review frontend security (tokens, XSS, CSRF, uploads, secrets) | `enforce-security` |
| Resolve conflicts with external React guidance | `erp-priority-override` |
| Validate a complete feature | `validate-frontend-feature` |

### DevOps (infrastructure lives in `deploy` repo)

| Task | Skill |
|------|-------|
| Dockerfiles / docker-compose / nginx / deployment | `deploy` |

---

## Execution Order

**Backend (strict):**
`gov-enforce-backend-contract` → `build-create-entity` → `build-create-repository` → `build-create-dto` → `build-create-mapper` → `build-create-service` → `build-create-controller` → `gov-validate-backend-feature`

> `build-create-entity` emits two artifacts in this one step when applicable: the JPA entity, and
> its Domain companion object (business rules) — see that skill's "Domain Companion Object"
> section. This does not add a step to the sequence above.

**Frontend — foundation (build once, before any feature):**
`create-auth-session` → `create-error-handling` → `create-app-state`

**Frontend — per feature (strict):**
`create-models` → `create-api-client` → `create-queries` → `create-forms` → `create-components` → `create-routing` → `create-confirm-actions` → `create-tests` → `validate-frontend-feature`

> This order is read directly from each skill's own stated step number in
> its `SKILL.md` front matter (e.g. `create-models` = "Step 2.1",
> `create-queries` = "Step 2.3", `create-confirm-actions` = "Step 2.8") —
> not invented for this document. `create-routing` carries no step number
> of its own; it is placed after `create-components` because a route's
> lazy import needs the page component to already exist. The six
> `enforce-*` skills and `erp-priority-override` are on-demand review /
> precedence skills, not fixed points in the generation sequence.

---

## Governance Rules

- NEVER generate backend code without first reading the corresponding skill
- NEVER generate frontend code without first reading the corresponding skill
- NEVER duplicate governance content in backend, frontend, or deploy repositories
- When a task spans multiple layers, read ALL relevant skills
- After completing a feature, run the validation skill to verify compliance
- Reference existing implementations in the codebase as canonical examples
- Business-rule conditions (anything answering "is this operation allowed?") must be
  implemented on a dedicated Domain object created via `create()`/`from()` factory methods —
  never inlined in Service, Repository, Controller, Mapper, or the Entity. The rule and its
  checks live in `build-create-entity`'s "Domain Companion Object" section and
  `gov-enforce-backend-contract`'s LAYER 0. This is a Governance requirement, not a prescription
  of which Backend Skill produces the Domain object — that remains an implementation detail of
  the Backend Skills.
- NEVER write banner/section-divider comments (`// ==== Section ====`, `// ─────...─────`,
  or any repeated-character line used to slice one file into visual sections). If a class has
  grown enough sections to need dividers, that is a signal to split the class, not to add ASCII
  art.
- NEVER write a Javadoc block longer than ~5 lines. No `@author` tags, no embedded usage
  examples, no restated "Architecture Rules" prose — that content belongs in a
  `governance/project-artifacts/` doc, not repeated inside every class that touches the
  concept. A Javadoc comment states the one non-obvious thing a reader couldn't get from the
  method/class signature and name; if there isn't one, omit the Javadoc entirely.
- NEVER create or modify a JUnit test file (anything under `src/test/java/` or
  `packages/backend-test/`) unless the user explicitly asked for tests, or the dedicated
  `execute-backend-test` phase is what's currently running. Implementing or fixing a feature
  is not, on its own, a request for tests — `packages/backend-test/` is a separate, gated
  phase for a reason (see `generate-module-setup.md`'s Step 1).

---

## Convention Precedence — db-script vs skills

The generated `P2/db-script-*.md` files and the binding skills in `.claude/skills/` disagree on
three points. These are SETTLED. An implementing agent must NOT re-derive, re-argue or
re-decide them per module — apply the decision and move on.

**1. Primary-key strategy — the skill wins (rules A.1.3 / A.1.4).**
Conflict: SEC, FIN and MDL db-scripts declare `GENERATED ALWAYS AS IDENTITY` (and a
"BLOCK 1 — SEQUENCES / none" block); CU, NOTIF and FILE db-scripts declare `CREATE SEQUENCE`.
Decision: always `GenerationType.SEQUENCE` with
`@SequenceGenerator(..., sequenceName = "SEQ_<TABLE>", allocationSize = 1)`. The module's own
Flyway migration creates the `SEQ_<TABLE>` sequences instead of identity columns.
Reason: one database already holds four modules built on `SEQ_<TABLE>` (V1–V15 applied).
Adopting IDENTITY for newer modules would put two PK strategies in one database permanently and
would require rewriting already-applied migrations, which Flyway forbids.

**2. Boolean mapping — read the column's real type (rule A.1.6).**
Conflict: the skill reads as if every boolean needs a converter, so a native `BOOLEAN` column
looks like a violation. It is not.
Decision: pick the mapping from the physical column type — native `BOOLEAN` → plain `Boolean`
field with NO `@Convert`; numeric (`SMALLINT` / `NUMBER`) → `BooleanNumberConverter`;
`CHAR(1)` → `BooleanCharYNConverter`.
Reason: this is a skill wording defect, not a convention reversal. The skill never stated the
native-BOOLEAN case; converting an already-boolean column is wrong.

**3. Unique-constraint naming — the db-scripts win (rule A.1.13).**
Conflict: the skill said `UK_<TABLE>_<DESC>`; all six db-scripts and all existing built code use
`UQ_`. No db-script anywhere in this repo uses `UK_`.
Decision: `UQ_<TABLE>_<DESC>`. The skill was simply wrong and has been corrected.
Reason: constraint and index names are physical database objects — they must match the
migration verbatim, so they are taken from the module's db-script, not invented by the skill.

> The plan generator (`gov.py`, `factory.yaml`, the profile) lives outside this repository, so
> the db-script side of conflicts 1 and 2 cannot be fixed at source here. This section is the
> override of record until it is.

---

## Context Reference (read on demand)

The `governance/.github/context/` guideline documents this section used to point at
(`backend.md`, `domain-layer.md`, `api-contract.md`, `frontend.md`) are **not present in this
repository**. Their essential content now lives inside the skills themselves:

| Topic | Where it lives now |
|-------|--------------------|
| Domain Layer — Business Rule ownership, the Decision Test | `build-create-entity` → "Domain Companion Object"; `gov-enforce-backend-contract` → LAYER 0 |
| API contract — response envelope, `Status` → HTTP mapping, error-code format | `gov-enforce-backend-contract` → "Status → HTTP Mapping"; `gov-enforce-error-handling` |
| Cross-module boundaries and eventing | `build-create-service` → "Cross-Module Calls" / "Publishing Domain Events" |
| Layer-by-layer detailed rules | the ten skills in `.claude/skills/` |

If a genuine architecture-overview document is needed again, create it under
`governance/project-artifacts/` and link it here — do not reintroduce
`governance/.github/context/` without an explicit decision, since the skills are now the single
place these rules are stated.
