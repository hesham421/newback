# Generate Backend Module Setup

```
Lives at   : backend/.claude/commands/generate-module-setup.md, so it
             auto-loads as a Claude Code slash command
```

## Precondition — the shared submodule is mounted (mechanical, not a judgement)

This runs BEFORE Step 0, so it can name no derived variable — it tests the one
path that is fixed by the `.gitmodules` entry, not by the profile:

```bash
test -f governance/shared/platform/profile-summary.json || echo "MISSING"
```

If MISSING: `git submodule update --init governance/shared`, then start over.
api-docs live only in that submodule now. A checkout cloned without
`--recursive` has none, every path here still reads plausibly, and the failure
surfaces phases later as a contract that cannot be resolved. Stop here instead.

## Step 0 — resolve the factory's facts (mechanical, never typed)

Every factory fact this command needs — the active profile, where governance
sits, and this track's ordered phase list — is published by the factory to one
file. Read it; do not restate anything it carries. A phase list typed here goes
stale the moment the profile gains a phase, and the symptom is a test phase
that runs before the phase it was supposed to wait for.

```bash
SUMMARY=governance/shared/platform/profile-summary.json
test -f "$SUMMARY" || { echo "MISSING — run 'gov.py publish profile-summary' in the factory"; exit 1; }

PROFILE=$(jq -r .profile            "$SUMMARY")
MODULES=governance/shared/$(jq -r .paths.modules "$SUMMARY")
PART=governance/shared/$(jq -r '.tracks.backend.partition' "$SUMMARY")   # {MOD} still unexpanded — this track's own partition
DELIVERY=governance/shared/$(jq -r '.tracks.backend.delivery' "$SUMMARY") # {MOD} still unexpanded — the delivered packages
EXEC_PHASES=$(jq -r '.tracks.backend.plans.exec.phases[].key' "$SUMMARY")
```

Then, for the module being set up:

```bash
MBASE=$MODULES/$MODULE                          # the module's analysis (stages, _state, manifest) — read-only
MINE=$(echo "$PART" | sed "s/{MOD}/$MODULE/")       # this repo's own partition: execution-state.json, api-docs/, test-api/
PKGS=$(echo "$DELIVERY" | sed "s/{MOD}/$MODULE/")   # the factory's delivered packages for this module — read-only
```

`$MBASE` and `$PKGS` are **read-only** to this repo; it writes only under `$MINE`
(`$MINE/api-docs/` included — never `$MINE/packages/`, that is the factory's). A write
anywhere else under `governance/shared/` is out of this track's partition.
What actually STOPS it is `.governance-scope`: `./scripts/governance scope`
sparse-checks the submodule down to the partitions this track may read, so the
other track's tree is not on disk to be written. (`CODEOWNERS` in the shared
repo does NOT stop it — it names one owner for every path, draws no
backend/frontend line, can only request a review, and is bypassed entirely by
the direct push `./scripts/governance push` makes. Do not rely on it.)

**Path resolution — every governance path lives inside `governance/shared/`.**
The analysis, the plans and this repo's partition are all in that submodule;
this repo's own tree holds only tools, skills and commands. So in this file and
in everything it generates, a bare `packages/…` resolves under `$PKGS`,
a bare `execution-state.json` / `api-docs/` / `test-api/` under `$MINE`, and a
bare stage folder (`P0`…`P3_2`, `test_gen/`, `manifest.json`) under `$MBASE` —
each with `{MOD}` expanded. Nothing here ever reads a module artifact from the
backend repo root, from a `governance/modules/…` path (that tree does not
exist), or from `frontend/`.

**`$EXEC_PHASES` is the authority for `gated_by_phases`.** Intersect it with the
phases actually found on disk — never type the list, and never let a phase the
profile declares go missing from the gate.

## Your Task

Scan this repo for the specified module and generate three files:
1. `.claude/commands/[MODULE]/execute-backend.md` — implementation phase execution
2. `.claude/commands/[MODULE]/execute-backend-test.md` — test phase execution,
   gated on execute-backend.md's phases
3. `execution-state.json` — state tracker for both

Each module gets its own `.claude/commands/[MODULE]/` folder — never write
back to the flat `.claude/commands/execute-backend.md` (no module name,
collides with every other module's setup, and silently overwrites whatever
module was generated last).

**Write every governance path into the generated commands FULLY EXPANDED** —
`governance/shared/backend/modules/<MODULE>/packages/backend-execution/…`, not
`packages/backend-execution/…` and not `$PKGS/…`. A generated command is run
standalone by a session that never read this file, so it carries no variable
bindings and no bare-path convention; a bare path there is a path that session
has to guess. The templates below use `$MBASE` / `$MINE` / `$PKGS` **as
placeholders to substitute**, never as text to copy through.

`execute-backend-test.md` is this module's test-verification command — it
regenerates this module's api-docs via `governance/governance-tools/api-doc-generator`
(so verification always runs against the real, current implementation, never a
stale snapshot) and then drives the `api-verify` skill
(`.claude/skills/api-verify/SKILL.md`, this repo's sole adopted backend API
verification mechanism) to turn those api-docs — plus the test-execution-manifest
when present — into one runnable script, a problems report, and one coverage
table, all scoped to this module alone.
The generated command is **fully self-contained**: it depends only on
`governance/governance-tools/api-doc-generator`, the `api-verify` skill,
`governance/shared/platform/rules/api-verify-config.md`, and this module's own artifacts under
`$MBASE/` (analysis) and `$MINE/` (this repo's partition) — never on an external governance/mechanism
doc, and never on TestSprite (retired as this project's backend test
mechanism — do not reintroduce a `TestSprite` MCP dependency here). Every rule
it needs (api-doc regeneration, module scoping, failure taxonomy) is written
into the generated command itself, below.

---

## Input

```
$ARGUMENTS = MODULE
```

If missing, ask for it — do not guess.

**Module validation:** confirm a `$MBASE/` folder
(or its version-suffixed variant, resolved in Step 0.5) exists on disk.
If it doesn't, stop with a plain "unknown module" message — do not guess
or fabricate a structure. This is the only validation this command
performs — there is no other precondition, because backend work has no
upstream gate to wait on.

---

## Step 0.5 — Resolve the module VERSION base (IFA-aware) — MANDATORY

A module that received an incremental feature via IFA has a current version
≥ 2, and ALL its artifacts (packages, execution-state, api-docs, generated
commands) live under a version-suffixed base — never over v1. Resolve the base
BEFORE scanning, directly from the filesystem:

```bash
# find, not a glob: under zsh an unmatched `$MBASE/v*/` aborts the command
# with "no matches found" and 2>/dev/null does not suppress it.
find $MBASE -mindepth 1 -maxdepth 1 -type d -name 'v[0-9]*' | sort -t v -k2 -n | tail -1
```

Rule:
- No `vN` folder found → v1; leave `$MBASE`, `$MINE` and `$PKGS` exactly as
  Step 0 built them.
- Highest `vN` folder found → append `/v$N` to **each of the three** before
  anything below reads or writes through them:
  `MBASE=$MBASE/v$N`, `MINE=$MINE/v$N`, `PKGS=$MINE/v$N/packages`
  (the delivery path is the partition's own `packages/` subtree, so it takes
  the same suffix — re-derive it, never leave it pointing at v1).

The three keep their Step 0 meanings, and nothing below may swap them:

| Variable | Points at | This repo may |
|---|---|---|
| `$MBASE` | `analysis/modules/<MOD>` — P0…P3_2, `test_gen/`, `manifest.json` | **read only** |
| `$PKGS`  | `<partition>/packages` — `backend-execution/`, `backend-test/` | **read only** |
| `$MINE`  | `backend/modules/<MOD>` — `execution-state.json`, `api-docs/`, `test-api/` | **read and write** |

In particular, for a vN module:
- scan `$PKGS/backend-execution` and `$PKGS/backend-test`
  (with `$MBASE/test_gen` as the flat-file fallback — see Step 1's
  Test phase(s) section)
- write `execution-state.json` to `$MINE/execution-state.json`
- `api_docs_path` = `$MINE/api-docs/`
  — NOT `$MBASE/api-docs/`. api-docs are the ONE artifact this repo does
  not keep: they live in the shared repo, which is their single copy, and
  the frontend reads that same copy. They are also NOT version-suffixed —
  they are derived from the running application, so there is one current
  set per module, not one per plan version. STEP 0.3 of the test phase and
  the `Writes to` line of `/generate-api-docs` already name this exact path;
  this line is what must agree with them.
- write the generated commands to `.claude/commands/[MODULE]/v$N/` (so the v1
  commands, still valid history, are never overwritten). For v1 keep the flat
  `.claude/commands/[MODULE]/`.

NEVER hardcode the un-suffixed `modules/$MODULE/` for a module whose
current_version is ≥ 2 — that would scan the frozen v1 packages and generate a
v1 command for a v2 delta.

---

## Step 1 — Scan the repo structure

```bash
find $PKGS/backend-execution -type f -name "*.md" | sort
find $PKGS/backend-test      -type f -name "*.md" | sort
find $MBASE/test_gen -type f -name 'backend-test-plan-*.md' 2>/dev/null   # Shape B; normally empty
```

From the scan results:
- Identify all PHASES (top-level folders under `packages/backend-execution/`)
- For each PHASE, identify all SUBs = the `.md` files inside that phase folder,
  EXCLUDING `index.md`, `.gitkeep`, and any `[PHASE]-HEADER.md` (the HEADER is
  phase-level shared context — read once in execution STEP 1.0, never a sub)
- Ignore `packages/backend-execution/_SECTIONS.md` for phase/sub detection — it
  is a top-level FILE (plan content outside every phase), not a phase folder
- Preserve the exact filesystem sort order **for SUBs within a phase**. Phase
  ORDER is `$EXEC_PHASES`, never the filesystem's — a plain `ls` sorts
  `ALIGN-BE` first, which is the last phase to run.
- For each SUB file, read the first 40 lines and count the tasks

Expected phases, in strict order: **`$EXEC_PHASES` from Step 0**, which is the
profile's own ordered list. Include only the ones actually present on disk, and
keep that order. Do not type the list here — it went stale twice before, and a
phase missing from the gate is invisible until the test phase runs without it.

### Test phase(s) — two delivered shapes, both real; check the filesystem, never assume

The test plan reaches this repo in one of two shapes, and **which one is a
per-module fact on disk, not a project-wide rule**. Do not carry a list of
which module is in which shape: such a list is wrong for the next module
generated and for every module whose plan is re-split. Check both, in this
order, and use whichever actually holds files:

**Shape A — split folder (`$PKGS/backend-test/`, check FIRST).** One `.md` per
unit, e.g. `API-SCENARIOS.md`, `RULE-SCENARIOS.md`, `INT-XM.md`, alongside
`index.md` / `_SECTIONS.md` / `state.json` / `verification.json`.

The folder is flat, so **it cannot tell you by itself which `.md` is a test
PHASE and which is a SUB of one** — and the answer differs per module. Do not
guess it from the file list, and do not carry a table of which module does
what. The profile already declares it, exactly as it declares
`$EXEC_PHASES`:

```bash
TEST_PHASES=$(jq -r '.tracks.backend.plans.test.phases[].key' "$SUMMARY")
# per phase: .sub_labels[] are its subs; .integration marks the cross-module one
jq -r '.tracks.backend.plans.test.phases[]
       | "\(.key)\tintegration=\(.integration)\tsubs=\((.sub_labels // [])|join(","))"' "$SUMMARY"
```

- A declared phase is **present** for this module when its evidence is on disk:
  a phase with `sub_labels` → at least one `<label>.md` exists in the folder
  (each existing label becomes a SUB, in the profile's order); a phase without
  them (the `integration: true` one) → its own `<key>.md` exists, and it is
  recorded as a phase entry with `"subs": []`.
- Declared but absent on disk → omit it. Do NOT fabricate a placeholder. A
  ROOT module has no cross-module integration unit, so no `INT-XM.md` and no
  `INT-XM` entry — that is correct, not a gap.
- On disk but not declared → do not silently fold it in; report it as an
  unrecognised unit and ask, the same as for an unrecognised exec phase.
- **Corroborate when you can.** When `$PKGS/backend-test/state.json` exists it
  carries `units[]` — e.g. `["SUB:RULE-SCENARIOS","SUB:API-SCENARIOS","PHASE:INT-XM"]`
  — the splitter's own record of the SUB/PHASE split. Read it and confirm it
  agrees with the profile-derived answer; if the two disagree, STOP and report
  the disagreement rather than picking one. It is absent for some modules,
  which is why it corroborates rather than decides.
- `header_file` is that folder's `*-HEADER.md` if one exists, else its
  `index.md`, else `null`.

**Shape B — one flat file (`$MBASE/test_gen/backend-test-plan-<mod-lowercase>.md`).**
Used only when Shape A yields nothing. There is no per-phase subfolder and no
per-sub file — detect phases and subs from markers INSIDE that one file:

- PHASES = every `<!-- PHASE:<id>:START -->` … `<!-- PHASE:<id>:END -->`
  block found in the file (in practice, one: `TEST-PLAN-BE`) — its id is the
  `<id>` in the marker, never invented or renamed.
- For each PHASE block, SUBs = every `<!-- SUB:<id>:START -->` …
  `<!-- SUB:<id>:END -->` block nested inside it (e.g. `RULE-SCENARIOS`,
  `API-SCENARIOS`) — its id is the `<id>` in the marker.
- A separate cross-module/integration phase (historically named `INT-XM`) is
  detected the same way — as its own `<!-- PHASE:*:START -->` block — if the
  file contains one. If the file instead states outright that no such phase
  applies (a ROOT module has no cross-module unit), there is none to add —
  do not fabricate an empty placeholder entry for it.
- Preserve marker order as found in the file for both phases and subs.
- `header_file` is the path to this one flat file itself — there is no
  separate `*-HEADER.md` for a flat-file-sourced test phase.

**Both shapes:**
- Each TC is identified by its `TC-[MODULE]-<seq>` id and belongs to whichever
  sub (Shape A: file; Shape B: `<!-- SUB:*:START -->` block) contains it; this
  is also how STEP 0.1 of the generated `execute-backend-test.md` will later
  load the REQUIRED COVERAGE list.
- Each test phase is gated by every backend EXECUTION phase that exists for
  this module — `$EXEC_PHASES` from Step 0, intersected with what is on disk —
  unless the plan's own header narrows it.
- **Record which shape was found**, and write the resolved path into
  `header_file` — the generated `execute-backend-test.md` must name the shape
  that actually exists for THIS module, not both.
- If neither shape yields anything, `test_phases` is an empty array — there is
  nothing to record yet, and that is the correct, honest result (not a bug
  to work around). Say which two paths you checked.

### Weight classification

| Weight | Criteria |
|--------|----------|
| LIGHT  | < 5 tasks, single layer |
| MEDIUM | 5–10 tasks, 1–2 layers |
| HEAVY  | > 10 tasks, multi-layer (Entity+Repo+Service+Controller) |
| XL     | Full feature in one sub |

Record weight and task count for every sub found.

---

## Step 2 — Generate `execution-state.json`

Location: `$MINE/execution-state.json`  (resolved in Step 0.5 — v1 = no suffix, vN = /vN).
**Not** `$MBASE/…`: that is the factory's analysis tree, read-only here. The
factory overwrites it on the next publish, so a write there is lost, not
merged — and nothing in the shared repo refuses it for you.

```json
{
  "module": "[MODULE]",
  "generated_at": "[today's date]",
  "current_phase": "[FIRST_PHASE]",
  "current_sub": "[FIRST_SUB or null]",
  "api_docs_path": "$MINE/api-docs/",
  "phases": [
    {
      "id": "[PHASE_NAME]",
      "status": "PENDING",
      "subs": [
        { "id": "[SUB_NAME]", "status": "PENDING" }
      ]
    }
  ],
  "test_phases": [
    {
      "id": "[TEST_PHASE_NAME]",
      "status": "PENDING",
      "gated_by_phases": [ …$EXEC_PHASES, in its own order, intersected with the phases found on disk… ],
      "header_file": "the path resolved in Step 1 for the shape actually found — Shape A: $PKGS/backend-test/<*-HEADER.md | index.md>; Shape B: $MBASE/test_gen/backend-test-plan-<mod-lowercase>.md (the flat file itself); null if neither exists",
      "subs": [
        { "id": "[SUB_NAME]", "status": "PENDING" }
      ]
    }
  ],
  "blocked": [],
  "deferred_xm": [],
  "api_doc_gaps": []
}
```

Every `$VAR` above is written into the file **expanded** — a real repo-relative
path such as `governance/shared/backend/modules/<MODULE>/api-docs/`, never the
literal `$MINE`. (Every state file in this repo carried a
`governance/shared/erp/modules/<MODULE>/api-docs/` for a while — a path that
exists nowhere; that is what an unexpanded-then-guessed value looks like
later.)

Rules:
- `test_phases` is an ARRAY — one object per real test phase found in
  Step 1's scan (mirrors the main `phases[]` array's shape). A module
  with only a base test phase gets a one-element array; a module whose plan
  spans cross-module dependencies gets the base phase plus its integration
  phase(s) (e.g. `INT-XM`) as additional array elements.
- List only phases/subs actually found in Step 1 — never a fixed name.
- Each element's `gated_by_phases` is `$EXEC_PHASES` (Step 0) filtered to the
  phases that exist for this module, in the profile's order — derived, never
  typed. If a phase the profile declares is missing from disk, say so rather
  than dropping it silently: a gate that waits for nothing passes for the wrong
  reason.
- `header_file` is the path resolved for that phase in Step 1 (Shape A:
  `*-HEADER.md`, else `index.md`; Shape B: the flat plan file), else `null`.
- `blocked`, `deferred_xm`, `api_doc_gaps` start empty.
- The shape above is the MINIMUM, not the whole file. Execution and
  verification add keys of their own to a `test_phases[]` entry
  (`api_verify_run`, `coverage`, `failed_tcs`, `gap_tcs`, `mechanisms`,
  `note`) — those are the record of what actually happened. **Regenerating
  setup for a module that has already run must preserve them**; never
  overwrite a populated state file back down to this skeleton.

### `api_doc_gaps[]` entry format (populated during execution)
```json
{
  "type": "MISSING_IN_DOCS",
  "phase": "[PHASE]",
  "sub": "[SUB]",
  "endpoint": "[METHOD] [path]",
  "detail": "[what was missing]",
  "resolution": "resolved via backend source: <path>",
  "recorded_at": "[timestamp]"
}
```

---

## Step 3 — Generate `.claude/commands/[MODULE]/execute-backend.md`

```markdown
# /[MODULE]/execute-backend

Execute the current phase for [MODULE] — with context safety check.

## Usage
/[MODULE]/execute-backend [PHASE]

---

## STEP 0 — Context Safety Assessment (MANDATORY)

### 0.1 — Read state, identify PENDING subs in the requested phase
### 0.2 — Look up each sub's weight from the Weight Map below
### 0.3 — Classify and decide chunking

| Total weight in phase | Action |
|---|---|
| All LIGHT/MEDIUM | Execute the whole phase in one pass |
| Any HEAVY present | Chunk — one sub (or a few LIGHT subs) per pass |
| Any XL present | That sub alone is one full pass |

### 0.4 — Print assessment, wait for confirmation
```
══════════════════════════════════════════════════════
PHASE ASSESSMENT — [MODULE] / [PHASE]
══════════════════════════════════════════════════════
Subs pending : [list, weight + task count each]
Plan         : [one pass / chunked — list chunks]
══════════════════════════════════════════════════════
Proceed? [waits for confirmation]
```

---

## STEP 1 — Execution (after confirmation)

### 1.0 — Read shared context once (before the per-sub loop)
- The phase's `[PHASE]-HEADER.md` under `$PKGS/backend-execution/[PHASE]/`
  if present — phase-level strategy, tables, and intro that the SUB files
  reference but don't repeat.
- `$PKGS/backend-execution/_SECTIONS.md` if present — plan-level content
  that lives OUTSIDE every phase (Plan Index, DB Alignment Manifest, Error
  Catalog, Agent Handoff Summary). Read once for orientation; it is context,
  not a sub.

### Per sub:
1. Read `$PKGS/backend-execution/[PHASE]/[SUB].md` completely
   (the SUB file is named by its phase-qualified label, e.g. `SVC-API-CRUD.md`)
2. Identify all tasks
3. Match each task to the applicable skill(s) in `.claude/skills/`
   (`build-*` to generate, `gov-*` to validate — skills self-declare what
   they apply to; consult the ones whose scope matches the task)
4. Read those skills from `.claude/skills/<skill>/SKILL.md` before writing
5. Execute all tasks in order
6. Run the phase's validation skill after the last task
7. Mark sub COMPLETE in `$MINE/execution-state.json`

### Blocked items — OQ
OQ-blocked task → skip, add to `blocked[]`, mark in code:
`// TODO: OQ-[ID] — pending resolution`. Continue remaining tasks.

---

## STEP 2 — Session Report

Print phase/sub completed, tasks executed, blocked items, any
api_doc_gaps entries added.

---

## Weight Map — [MODULE]
[Insert actual weight map from Step 1]

## Phase Map — [MODULE]
[Insert actual phase → subs map from Step 1]

---

## Constraints (NON-NEGOTIABLE)

- NEVER skip STEP 0
- NEVER execute without confirmation after assessment
- NEVER invent field/column/route names — always look up
  `$MBASE/P2/db-script-<mod-lowercase>.md` (the file is module-suffixed)
- NEVER implement a blocked OQ item — mark and skip only
- NEVER advance phase without explicit instruction
- ALWAYS update `$MINE/execution-state.json` after every sub
```

---

## Step 3B — Generate `.claude/commands/[MODULE]/execute-backend-test.md`

`api-verify` is module-agnostic and reads only this module's own api-docs (+
test-execution-manifest when present) — there is no whole-backend bootstrap/PRD/plan
step to share across modules, unlike the retired TestSprite mechanism. The
generated command below always regenerates this module's api-docs first (so the
verification script is never built against a stale contract), then runs
`api-verify` scoped to this module only; it never touches another module's
api-docs or output.

```markdown
# /[MODULE]/execute-backend-test

Execute API verification for [MODULE] — only for what's actually complete.

> **Self-contained.** This command needs `governance/governance-tools/api-doc-generator`,
> the `api-verify` skill (`.claude/skills/api-verify/SKILL.md`), `governance/shared/platform/rules/api-verify-config.md`,
> and this module's own artifacts under `$MBASE/` and `$MINE/`. Every rule it relies
> on is written below or in those two files — it reads no other external mechanism/governance
> doc, never stops waiting on one, and never calls TestSprite (retired as this project's
> backend test mechanism).

## Usage
/[MODULE]/execute-backend-test

---

## STEP 0 — Plan Load, Gate Check, API-Doc Regeneration + Assessment

### 0.1 — Load the delivered test plan (the REQUIRED COVERAGE)
Read every `TC-[MODULE]-<seq>` block out of this module's delivered test plan.
**Write the ONE shape Step 1 actually found on disk for this module into the
generated command** — not both, and not a shape you remember:
- **Shape A (split folder):** every `.md` under
  `$PKGS/backend-test/` except `index.md`, `_SECTIONS.md` and `*-HEADER.md`
  — each file is one sub (`API-SCENARIOS`, `RULE-SCENARIOS`, `INT-XM`, …).
- **Shape B (flat file):** `$MBASE/test_gen/backend-test-plan-<mod-lowercase>.md`,
  reading across every `<!-- PHASE:*:START -->` block it contains (the base
  test phase and any integration phase such as `INT-XM`, each nested
  `<!-- SUB:*:START -->` block).

Extract per TC: its `TC-[MODULE]-<seq>` id, the `AC-*` / `XM-*` /
`UXD-*` it traces (from its `traces=` marker attribute / `Derived from` line),
and its one-line scenario. This list is the **REQUIRED COVERAGE** for this
run — it is what the system's own analysis says must be tested, independent
of whatever `api-verify` later discovers from the api-docs. If the recorded
path no longer holds a plan, or it holds no `TC-*` block, re-check the OTHER
shape before concluding anything — the delivery shape can change between
factory publishes. Only if both are genuinely empty, STOP and report it:
there is nothing governed to verify.

### 0.2 — Gate Check (MANDATORY)
Read `$MINE/execution-state.json` → for each entry in `test_phases[]`, its
`gated_by_phases[]` (derived from `$EXEC_PHASES`). Confirm every listed backend execution phase has
`status == COMPLETE`. Empty list → that phase's gate passes automatically.

If not all complete:
```
══════════════════════════════════════════════════════
⛔ TEST GATE FAILED — [MODULE]
══════════════════════════════════════════════════════
Waiting on : [PHASE: status], ...
══════════════════════════════════════════════════════
```
STOP. Do not regenerate api-docs and do not invoke `api-verify`.

### 0.3 — Regenerate api-docs (MANDATORY, every run, BEFORE api-verify)
`api-verify` treats stale api-docs as a hard blocker — it must never read a
possibly-outdated copy. Regenerate this module's api-docs from the real,
current implementation first:
```bash
cd governance/governance-tools/api-doc-generator
python3 generate.py --module [MODULE] --function generate
```
(consult that tool's own `README.md` for `--function generate` vs `update` vs
`review` semantics before assuming — use whichever actually (re)writes
`$MINE/api-docs/` in full for this run). Confirm
`$MINE/api-docs/index.md` was written/updated before
proceeding to STEP 0.4 — do not invoke `api-verify` against missing or
unrefreshed api-docs.

**Then publish them, or they reach nobody.** That directory is inside the
`governance/shared` submodule — a separate repository. Files written there are
untracked in *that* repo, so `api-verify` here reads them while the factory and
the frontend still read the previously pushed commit. Regenerating and stopping
is indistinguishable from success until something downstream contradicts it:

```bash
cd governance/shared
git fetch origin main
git merge-base --is-ancestor HEAD origin/main \
  && git checkout main \
  || echo "HEAD is NOT on origin/main — do not checkout; commit here and push HEAD:main"
git add -A && git commit -m "api-docs([MODULE]): regenerated" && git push
cd ../.. && git add governance/shared && git commit -m "bump shared" && git push
```

Getting onto a branch is not optional housekeeping: a submodule is checked out
on a *commit*, not a branch, so a plain `git push` has no branch to push to and
the commit never leaves this machine. But `git checkout main` is only harmless
**while the checked-out commit is an ancestor of `origin/main`** — hence the
guard above. If it is not (this checkout has been pinned off-branch before),
checking out `main` silently moves you off the tree you just generated against;
commit where you are and `git push HEAD:main` instead. Full sequence and both
failure modes: `/generate-api-docs`.

### 0.4 — Confirm the app is reachable
`http://localhost:7272/actuator/health` (start it with `mvn spring-boot:run`
if it isn't running). Unreachable →
classify `ENVIRONMENT_FAILURE`, stop, report — do not proceed. (If this
module's api-doc-generator run in 0.3 itself needs the live app — e.g. to read
a running OpenAPI endpoint rather than a static build artifact — confirm
reachability before 0.3 instead; check the tool's own discovery method rather
than assuming.)

### 0.5 — Same assessment/confirmation pattern as execute-backend.md

---

## STEP 1 — Execution (after confirmation)

Invoke the `api-verify` skill (`.claude/skills/api-verify/SKILL.md`) for
`<MOD>` = `[MODULE]`. Per the skill's own procedure it reads:
- `$MINE/api-docs/` — regenerated in STEP 0.3, mandatory;
- `$MBASE/test_gen/test-execution-manifest-<mod-lowercase>.md`
  when present (Full tier: happy-path CRUD + negative RULE checks, dependency
  order read verbatim from the manifest) — otherwise Minimal tier (happy-path
  CRUD only, FK order inferred, negatives stated as skipped and why);
- `governance/shared/platform/rules/api-verify-config.md` for every stack convention (base path,
  envelope shapes, error-code format, permission pattern) — never re-derived
  here.

It produces, under `$MINE/test-api/`:
- `test_[mod-lowercase]_apis.py` — one runnable script, one `test_<entity>()`
  per entity in dependency order, each create/update/negative call tagged with
  a traceability comment (`Covers: API-… ; Negative: RULE-… / <code> / TC-…`),
  self-tearing-down;
- `[mod-lowercase]_problems_report.md` — failures bucketed likely-real-bug /
  test-assumption-mismatch / infrastructure.

Run the generated script (`python3 $MINE/test-api/test_[mod-lowercase]_apis.py`)
against the app confirmed reachable in STEP 0.4, and record its pass/fail per
`test_<entity>()` suite. This command never hand-writes verification code
itself and never calls a TestSprite tool.

---

## STEP 1.9 — Coverage cross-check (governed plan ↔ api-verify) — MANDATORY

This is the connective tissue between the delivered test-gen plan (STEP 0.1)
and `api-verify`'s own output. `api-verify`'s Full-tier negatives are already
tagged with the SAME `TC-[MODULE]-<seq>` id space the test-gen plan uses (no
separate numbering scheme to bridge, unlike the retired TestSprite `TCnnn`
ids) — map every REQUIRED-COVERAGE `TC-[MODULE]-<seq>` from STEP 0.1 to the
`test_<entity>()` function(s) in `test_[mod-lowercase]_apis.py` whose
traceability comment names it, and to that function's actual pass/fail result
from STEP 1. A happy-path `TC-*` with no corresponding `Covers:` entry, or a
negative `TC-*` with no corresponding `Negative:` entry, is a gap — the same is
true when a tier is Minimal and the manifest that would have produced a
negative test simply doesn't exist yet (state that explicitly, don't silently
treat it as covered). Produce this table for the report:

```
GOVERNED PLAN ↔ API-VERIFY COVERAGE — [MODULE]  (tier: Full | Minimal)
TC-[MODULE]-<seq>  │ traces (AC/XM/UXD) │ scenario        │ test_<entity>() ref     │ result
───────────────────┼────────────────────┼─────────────────┼─────────────────────────┼────────
TC-[MODULE]-001    │ AC-…               │ …               │ test_widget (Covers)   │ PASS
TC-[MODULE]-0NN    │ XM-… / UXD-…       │ …               │ ✗ none                 │ GAP
```

- A delivered `TC-*` with no matching `test_<entity>()` reference is a
  **coverage gap** — list it prominently; it is never dropped silently.
- Integration `TC-*` — those tracing `XM-*` or `UXD-*`, which come from a phase
  the profile marks `integration` (Step 0's summary carries the flag; the name
  is the profile's to choose) — are checked here exactly like any other: a cross-module
  dependency with no exercising test is a gap, same as an uncovered `AC-*`.
- Record the coverage ratio: `<covered>/<total>` REQUIRED-COVERAGE TCs.

---

## STEP 2 — Classify and report

Classify every failure/skip using this taxonomy — tool-agnostic, describes
outcomes rather than any specific test framework:

| Code | Meaning |
|---|---|
| `TEST_STRUCTURE_FAILURE` | Broken test script itself — not an app bug |
| `DB_PRECONDITION` | Required seed/lookup/master data missing |
| `ENVIRONMENT_FAILURE` | MCP, server, or config unreachable/broken |
| `DEPENDENCY_FAILURE` | Skipped/failed because an upstream TC failed |
| `MISSING_IMPLEMENTATION` | Endpoint or feature not built yet |
| `AUTH_FAILURE` | Login / session / token issue |
| `VALIDATION_FAILURE` | Backend rejected input that should have been valid |
| `SERVER_ERROR` | 5xx from backend |
| `CONTRACT_BREAK` | Response shape no longer matches the documented contract |
| `API_REGRESSION` | API behavior changed vs. expected |
| `DATA_INTEGRITY_ISSUE` | API step reported success but DB state is wrong |
| `BUSINESS_LOGIC_ISSUE` | A functional/business rule behaves incorrectly |

Every failed/skipped test gets exactly one code. Never invent a new one —
if nothing fits, use `ENVIRONMENT_FAILURE` and explain why in the detail.

Write `reports/TEST-REPORT-[MODULE]-backend-[YYYY-MM-DD].md` — a
module-scoped digest, distinct from `api-verify`'s own raw output
(`[mod-lowercase]_problems_report.md`, left under
`$MINE/test-api/`, untouched). It MUST include the
STEP 1.9 coverage table (governed plan ↔ api-verify) and the coverage ratio,
ABOVE the failure taxonomy — a green taxonomy over an incomplete plan is not
a pass. This report is complete once the test/coverage section above is
written.

Any `FAIL` or coverage GAP → report it here with its taxonomy code and STOP;
this command never fixes source itself. Fixing is a separate, deliberate step
the user runs afterward — do not auto-invoke any fixing agent from here.

### 2.1 — Update `$MINE/execution-state.json` `test_phases[]` (MANDATORY)
For each entry in `test_phases[]`, set its status from the STEP 1.9 result:
- `COMPLETE` only when EVERY `TC-*` under that phase (STEP 0.1) has a passing
  `api-verify` counterpart (STEP 1.9).
- `PARTIAL` when some pass but at least one `TC-*` is a gap or a fail — attach
  the gap/fail `TC-*` list to the entry.
- `PENDING` if the phase never ran.
Scope the edit to `test_phases[]` (and, if a real doc gap surfaced, one
`api_doc_gaps[]` append in the canonical shape) — touch nothing else. The
`api-verify` run MUST leave `test_phases[]` reflecting exactly what it verified.

---

## Constraints (NON-NEGOTIABLE)

- NEVER run before the gate check (0.2) passes
- NEVER invoke `api-verify` (STEP 1) before this run's own api-doc-generator
  regeneration (STEP 0.3) has completed and been confirmed written — a stale
  api-docs copy produces a script that tests the wrong contract
- NEVER call a TestSprite tool of any kind — TestSprite is retired as this
  project's backend test mechanism; `api-verify` is the sole adopted one
- NEVER modify application source code — report, don't fix
- NEVER hand-edit a generated `test-api` script — rerun STEP 0.3 → STEP 1 to
  regenerate it instead
- ALWAYS classify every failure/skip
- ALWAYS load the governed `TC-*` plan (STEP 0.1) and emit the STEP 1.9
  coverage table before considering any test phase complete
- ALWAYS update `$MINE/execution-state.json` `test_phases[]` status per STEP 2.1
```

---

## Step 4 — Verify and report

```
══════════════════════════════════════════════════════
BACKEND MODULE SETUP COMPLETE: [MODULE]
══════════════════════════════════════════════════════
execution-state.json      ✓  $MINE/  (v1 = <partition>/[MODULE]/, vN = <partition>/[MODULE]/vN/)
execute-backend.md        ✓  .claude/commands/[MODULE]/
execute-backend-test.md   ✓  .claude/commands/[MODULE]/

Phases detected       : [count]
Total subs detected   : [count]
Test phases detected  : [list of test-phase folders found, e.g. base + INT-XM / "none"]
  gated by : [phases found]

Weight map:
  [PHASE] / [SUB]  → [WEIGHT]  ([N] tasks)

Heavy phases (require chunking): [list or "none"]

To start execution:
  /[MODULE]/execute-backend [FIRST_PHASE]

To verify (test) once implementation is COMPLETE:
  /[MODULE]/execute-backend-test
══════════════════════════════════════════════════════
```

---

## Constraints (this command itself — NON-NEGOTIABLE)

- NEVER run without MODULE specified
- NEVER invent a phase, sub, or file path not found in Step 1's scan
- NEVER reach into `frontend/governance/` for anything — this command
  and the tools it calls have no concept of a frontend track at all
- NEVER write a specific machine's absolute path into this file, into a
  generated command, or into `execution-state.json`. Derive the backend
  repo root at runtime (`git rev-parse --show-toplevel`, or by walking up
  from this file's location) — never a remembered path from an earlier
  session or machine.
