# /orchestrate-module (BACKEND)

Master orchestration protocol for executing a BACKEND module's governance
execution pipeline in THIS backend repo. It lives at
`backend/.claude/commands/orchestrate-module.md` and runs from the backend repo
root, drives the module's own `execute-backend.md` per-phase command, and adds a strict session/safety discipline on top of it. This command
knows ONLY the backend — there is no frontend concept, no `--track`, no
cross-repo reach into `frontend/`. (The frontend repo has its own separate
orchestrator; this one never touches it.) Every path below is relative to the
backend repo root unless said otherwise.

## Step 0 — resolve where governance lives (never type it)

```bash
SUMMARY=governance/shared/platform/profile-summary.json
test -f "$SUMMARY" || { echo "MISSING — git submodule update --init governance/shared"; exit 1; }
MODULES=governance/shared/$(jq -r .paths.modules "$SUMMARY")            # every module's analysis — read-only here
GOVROOT=governance/shared/$(jq -r .paths.platform "$SUMMARY")           # project-registry.md and the platform artifacts
PART=governance/shared/$(jq -r .tracks.backend.partition "$SUMMARY")    # this track's own partition ({MOD} unexpanded): execution-state.json, api-docs/, test-api/
PKGS=governance/shared/$(jq -r .tracks.backend.delivery "$SUMMARY")     # the delivered packages ({MOD} unexpanded) — written by the factory, read here
```

**Every governance path lives inside the `governance/shared/` submodule.** The
analysis this command reads (PRD, SRS, db-script, the execution plan), the
delivered packages, and this repo's own partition are all in there; the backend
repo's own tree holds only source, tools, skills and commands. So: bare
`packages/…` paths below resolve under `$PKGS` with `{MOD}` expanded;
`execution-state.json` and `api-docs/` under `$PART` with `{MOD}` expanded; and
bare stage folders — `P0`…`P3_2`, `P1` (SRS/PRD), `P2` (db-script), `test_gen/`,
`manifest.json` — under `$MODULES/{MODULE}/`. A version-suffixed base (`vN/`)
applies to each of the three the same way. Nothing is read from a
`governance/modules/…` path (no such tree), from the backend repo root, or from
`frontend/`. If a path you are about to read does not begin `governance/shared/`
and is not source/skill/command, you have the wrong path — resolve it again.

`$MODULES` and `$GOVROOT` below are those values. The profile folder is the
factory's to name; spelling it here makes a second profile an edit to this file.

## Usage

```
/orchestrate-module [MODULE] [PHASE?] [--auto]
```

- `MODULE` (required): a live module code. **The live set is read, never
  typed** — `jq -r '.modules[].code' governance/shared/platform/modules-registry.json`
  (that file also carries each module's `current_version` — the IFA check below).
  A module list written into a command is wrong the day a module is added,
  split or retired, and the symptom is a run that refuses a real module or
  accepts a dead one. If the argument is not in that file, STOP and say so;
  do not guess a near-match. Three things must exist, each in its OWN
  tree — they are not siblings:
  - `$MODULES/{MODULE}/` — the analysis (P0…P3_2, `test_gen/`, `manifest.json`)
  - `$PART/execution-state.json` and `$PKGS/backend-execution/` ({MOD} expanded)
  - `.claude/commands/{MODULE}/execute-backend.md` — the per-module command

  If any of this is missing or shaped differently than expected, STOP and ask —
  never guess a module's structure.

  **VERSION (IFA-aware) — resolve the base BEFORE anything else.** A module
  that received an incremental feature (IFA) has a current version ≥ 2 and all
  of the above live under a version-suffixed base. Resolve it directly from
  the filesystem:

  ```bash
  # find, not a glob: under zsh an unmatched `v*/` aborts the command with
  # "no matches found", and 2>/dev/null does not suppress it.
  find $MODULES/{MODULE} -mindepth 1 -maxdepth 1 -type d -name 'v[0-9]*' \
    | sort -t v -k2 -n | tail -1
  ```

  - No `vN` folder found → base `$MODULES/{MODULE}/`      (no suffix, v1)
  - Highest `vN` folder found → base `$MODULES/{MODULE}/v{N}/`

  Call it `{MBASE}`. Every `$MODULES/{MODULE}/…` and bare
  `packages/…` / `execution-state.json` path below resolves under `{MBASE}` — `packages/…` under `$PKGS` and `execution-state.json` under `$PART` (each with `{MOD}` expanded, the same `vN/` suffix applied),
  and the per-module command for a vN module is
  `.claude/commands/{MODULE}/v{N}/execute-backend.md`. By default orchestrate
  the CURRENT version; to drive an older frozen version, ask — never assume.
  Never orchestrate the un-suffixed v1 tree for a module whose current_version
  is ≥ 2.
- `PHASE` (optional): if omitted, resume from `execution-state.json`'s
  `current_phase`/`current_sub` — this command ALWAYS resumes from the last
  completed point, it never restarts a module from scratch.
- `--auto` (optional): AUTONOMOUS mode. Run every remaining phase to completion
  back-to-back WITHOUT pausing for the per-phase confirmation gate. Autonomy
  never weakens safety: subs still run strictly one at a time, the per-sub
  skill-compliance read + STEP 1.3 verification still happen, and the run STILL
  HALTS on any of — a sub error, a validation failure, a skill-compliance report
  that's silent or non-compliant, or a spec gap that STEP 2 cannot resolve from
  the module's own governance artifacts. Every one of those stops is now reached
  through STEP 1.4 first: on an impasse the orchestrator runs the second-agent
  debate, and halts only after that debate fails to resolve it — the debate is an
  added attempt BEFORE the stop, never a replacement for it. `--auto` removes the
  human pause
  between clean phases; it removes no check and no stop. Without `--auto`, the
  per-phase gate applies (recommended for a module's first run or after any spec
  or skill change).

Backend execution phases and their order are **read, never typed** — from
`.tracks.backend.plans.exec.phases[].key` in `profile-summary.json` (Step 0),
intersected with what `$PKGS/backend-execution/` actually holds:

```bash
EXEC_PHASES=$(jq -r '.tracks.backend.plans.exec.phases[].key' "$SUMMARY")
```

(As published today that yields `CORE → DATA-DOM → SVC-API → DOC → INT-C →
INT-R → SEC-BE → ALIGN-BE` — shown to orient you, not to be relied on. A phase
list typed into a command goes stale the moment the profile gains one.) There
is no P4/audit phase. Wherever this file says `ALIGN-BE` as the last execution
phase, read "the LAST key in `$EXEC_PHASES`".

## Portability — never hardcode an absolute path

Keep this file working unchanged on any machine, for any user, with the repo
checked out anywhere. **Never write a specific machine's absolute path into this
file, into a dispatched agent's prompt, or into any config this command
touches.** At the start of every run derive the backend repo root at runtime —
`git rev-parse --show-toplevel`, or by walking up from this file's location —
never a remembered path from an earlier session or machine. Every path a
dispatched agent needs is resolved fresh, this run, on this machine.

---

## Role of the orchestrating session (this session, running this command)

**The orchestrating session never writes code, never edits governance specs,
and never touches the module's source files directly.** Its only jobs are:

1. Read state and spec files to brief each dispatch (read-only).
2. Dispatch exactly ONE Claude Code agent session (via the `Agent` tool) per
   **sub** — one addressable unit of work (e.g. one entity's slice, one API
   group). Never per individual task line, never a whole phase in one shot,
   regardless of what `execute-backend.md`'s weight table says. That file's
   weight-based chunking ("all LIGHT/MEDIUM → whole phase in one pass") is
   **overridden** by this standing rule: **one dispatched session per sub,
   always** — a deliberate context-safety and auditability choice.
3. Run execution **strictly sequentially**: dispatch sub N, wait for its agent
   to fully finish and report, do a lightweight verification pass, THEN dispatch
   sub N+1. Never dispatch two subs in parallel, even when they look
   independent — a later sub in a phase routinely depends on an earlier sub's
   output (a shared repository method, a common DTO/mapper, a cross-entity
   lookup).
4. Gate every **phase** transition on the user's explicit go-ahead. Before
   dispatching the first sub of a phase, print a phase assessment and wait:
   ```
   ══════════════════════════════════════════════════════
   PHASE ASSESSMENT — {MODULE} / {PHASE}
   ══════════════════════════════════════════════════════
   Subs pending : [list, one line each]
   Plan         : N separate Claude Code sessions (one per sub), sequential
   ══════════════════════════════════════════════════════
   Proceed?
   ```
   Never advance to the next phase without the user's explicit confirmation in
   this conversation, even if every sub completed cleanly. **Exception —
   `--auto`:** still PRINT each phase assessment (so the run stays auditable in
   the transcript), but do not wait — proceed automatically, and when the phase
   closes cleanly, on to the next. `--auto` only removes the pause between CLEAN
   phases; it never removes a stop on a sub error, a failed/omitted validation,
   a non-compliant skill report, or an unresolved spec gap.
5. Resolve any spec gap a dispatched agent reports (STEP 2) **before** letting
   the phase be considered done. A phase is not sound — and this command does
   not move on — until every gap opened during it is resolved or explicitly
   escalated to the user, not just recorded.
6. Communicate with dispatched agents in **English** (full technical detail).
   Communicate with the user in **Arabic**, concisely, to help them decide —
   not a narration of tool calls.

---

## STEP 0 — Locate module & resume point

1. Read `$PART/execution-state.json` ({MOD} expanded — this repo's own
   writable partition, e.g. `governance/shared/backend/modules/<MODULE>/`). It is
   NOT under `$MODULES/` and there is no `backend/` segment inside `$PART`.
   Note `current_phase`, `current_sub`, and every phase's/sub's `status`.
2. If a `PHASE` argument was given, use it (but still resume from whatever subs
   in it are not yet `COMPLETE` — never re-run a `COMPLETE` sub). Otherwise use
   `current_phase`.
3. Read this module's own `.claude/commands/{MODULE}/execute-backend.md` for its
   phase list, weight map, and module-specific constraints (those substantive
   rules still apply — this command overrides only the *session-granularity and
   phase-gating* behavior above, not which files never get touched or how errors
   route).
4. **Skill orientation — mandatory, once per run, before the first phase
   assessment.** The backend skills at `.claude/skills/<skill>/SKILL.md` are the
   single binding source of rules — split into `build-*` (generate code) and
   `gov-*` (validate it). Read the ones whose declared scope covers this module's
   work, in full. Do not assume "matching real code precedent" covers this —
   precedent tells you what the codebase currently does, not whether that pattern
   is what the skills prescribe (precedent can itself be non-compliant; only
   reading the skill catches it). The architecture rules live INSIDE the skills:
   Domain-Layer ownership in `build-create-entity` + `gov-enforce-backend-contract`;
   the API contract / response envelope / `Status`→HTTP mapping / error-code
   format in `gov-enforce-backend-contract` + `gov-enforce-error-handling`. A
   skill outranks codebase precedent, never the reverse.
   This is a one-time read for the run — STEP 1.1 still requires identifying,
   per sub, exactly which skill files that sub's work triggers.
5. Print the phase assessment (format above) and wait for confirmation before
   dispatching anything. (In `--auto`, print it but do not wait — see the
   phase-gate rule.)

---

## STEP 1 — Per-sub dispatch protocol

For each `PENDING` sub in the confirmed phase, in the order listed in
`execution-state.json`:

### 1.1 — Orchestrator prep (read-only, in this session)

Before writing the dispatch prompt, read:
- **This command file itself, in full, again — before every single sub**, not
  just once at the start. Long multi-sub runs are exactly where instruction
  drift happens: a rule easy to follow at sub 1 silently stops applying by sub
  20 as your recall of "what this command says" degrades. This file on disk is
  authoritative over your memory of it; re-read it fresh every time.
- The phase's `[PHASE]-HEADER.md` (phase-level contracts/tables) and this sub's
  spec file, both under `packages/backend-execution/{PHASE}/`; and
  `packages/backend-execution/_SECTIONS.md` if present (plan-level content
  outside every phase — Plan Index, DB Alignment Manifest, Error Catalog, Agent
  Handoff Summary).
- **`db-script-<mod-lowercase>.md`** for this module (under `$MBASE/P2/`, e.g.
  `P2/db-script-fin.md` — the file is module-suffixed; `ls $MBASE/P2/` rather
  than assuming a bare `db-script.md`) — the authoritative source for table and
  column names, PK/FK/flag suffix conventions, and types. A spec block naming a
  field is a plan; the db-script is schema ground truth. Never invent a column
  name. Referred to below as **the db-script**.
- **The SRS** (`$MBASE/P1/srs-<mod-lowercase>.md`) for the business rules (RULE-IDs) this sub must
  implement — authoritative for behavior.
- **Skill compliance (mandatory — not satisfied by precedent-matching alone).**
  1. Cross-reference this sub's work type against the skills index from STEP 0.4
     and list, explicitly, every skill file this sub triggers before writing the
     dispatch — err toward more, not fewer. Backend triggers: ALWAYS
     `gov-enforce-backend-contract` first; a new/modified entity →
     `build-create-entity` (plus its Domain companion object for any business
     rule); repository → `build-create-repository`; DTOs → `build-create-dto`;
     mapper → `build-create-mapper`; service (incl. cross-module calls / domain
     events) → `build-create-service`; controller → `build-create-controller`;
     caching → `gov-enforce-caching-rules`; error handling →
     `gov-enforce-error-handling`; whole-feature validation →
     `gov-validate-backend-feature`.
  2. Read every listed skill file **in full** — not skimmed, not assumed from
     its name.
  3. Precedent-matching answers "what does the existing code look like"; this
     step answers "is that code — and the precedent you're about to copy —
     actually compliant." Both must be answered; neither substitutes for the
     other.
  4. If a skill's prescribed pattern conflicts with real precedent, do NOT
     silently pick a side — surface it to the user before/with the phase
     assessment. A consistent codebase-wide divergence from a skill is often the
     right call, but it's the user's to bless, not this orchestrator's to
     assume.
- **The real code precedent to mirror.** Before inventing any structure, find an
  already-`COMPLETE` analog and copy its exact shape: an earlier COMPLETE sub in
  this module (closest by entity shape — flat vs. self-referencing), or failing
  that an already-built sibling module's equivalent
  entity/repository/service/controller/mapper/DTO set. Real, consistent
  precedent in the checked-out code wins over an aspirational structure diagram
  when they disagree.
- Whether this sub needs something an **earlier sub in this phase** already built
  (a shared repository method, mapper, lookup, or cross-entity FK helper). If
  so, name the exact file/symbol to reuse and instruct the agent to import it,
  not duplicate it — and, if it doesn't exist yet, to ADD it there (additive
  only, one new method/export, nothing else in that file touched).

### 1.2 — Dispatch (Agent tool, one sub, `run_in_background: false`)

Write a fully self-contained English prompt — the dispatched agent has no memory
of this conversation. It MUST include:

- **Repo root**: the backend repo's absolute path (derived this run). It's a git
  repo, no worktree needed, work on the current checkout. The agent NEVER
  consults or touches the frontend repo — it does not exist for this work.
- **What NOT to touch**: every other module/phase/sub's output files; any file
  from an earlier sub in this phase (except the one specific additive change
  named in 1.1, if any — name that file and say "the ONLY change allowed here is
  X"); any controller/route/config not this phase's spec explicitly requires
  wiring into; any legacy module's files.
- **The exact files to read first, in full** (the ones identified in 1.1:
  HEADER, sub spec, the db-script, SRS slice, the named skills, the precedent)
  — **each as a fully expanded path**, e.g.
  `governance/shared/backend/modules/<MODULE>/packages/backend-execution/SVC-API/SVC-API-CRUD.md`
  and `governance/shared/analysis/modules/<MODULE>/P2/db-script-<module>.md`. The
  dispatched agent has none of this session's variable bindings and no
  bare-path convention, so `$PKGS/…` or a bare `packages/…` in its prompt is a
  path it has to guess at — and the tree it would guess (the repo root) has no
  governance in it at all.
- **The "don't build a competing implementation" check**: before writing code,
  confirm whether the thing this sub needs already exists (an entity/repository/
  service/controller for this resource). If it exists: integrate/modify it,
  never create a competing new one. If genuinely absent, flag it in the report
  and implement it as a minimal explicit addition.
- **Contract ground-truth rule (backend)**: the authoritative sources are, in
  order, the db-script (schema), the SRS (rules), and the sub's own spec block.
  The backend PRODUCES its API contract (api-docs are generated FROM the code
  after implementation) — the agent NEVER invents a contract detail the
  SRS/spec/db-script doesn't give, and NEVER consults the frontend. If a needed
  detail is confirmed absent or contradictory across those sources, it records
  ONE `api_doc_gaps[]` entry (shape below) and continues with everything else;
  it does NOT guess. Only the orchestrator (STEP 2) runs a gap to ground.
- **OQ-blocked items**: skip, note in the report, and write
  `// TODO: OQ-[ID] — pending resolution` only if the spec explicitly names that
  OQ ID for that exact field/behavior — never invent one.
- **XM-ID prohibition**: an XM-ID is a governance marker, not code — never write
  an XM-ID reference anywhere in the code. INT-C/INT-R cross-module dependencies
  are implemented via the established service/interface layer, not by emitting
  an XM-ID; if one seems needed in code, stop and flag it.
- **No parallel/competing mechanism for an owned responsibility** — use the
  established repository/service/mapper layering; no bypassing it with ad-hoc
  queries, no second HTTP/data path.
- **Skill compliance instruction**: name the exact skill file(s) from 1.1 by
  path, instruct the agent to read each in full before writing code, and to
  check its work against that skill's own "Verify before finishing" / violations
  list. A dispatch that never names a skill file means that step was skipped.
- **Validation step**: compile/build the module (`mvn -q -DskipTests compile` at
  minimum; the phase's own `gov-validate-*` skill where it applies) — run it and
  report the real result, never claim success without running it.
- **execution-state.json update, precisely scoped**: set only this sub's
  `status` to `"COMPLETE"`; advance `current_sub` only if it still equals this
  sub's id; if this is the LAST sub in the phase, also set the phase's `status`
  to `"COMPLETE"` and advance `current_phase`/`current_sub` to the next phase's
  first sub. If (and only if) a new gap was found, append ONE entry to the
  top-level `api_doc_gaps[]` in this exact canonical shape (identical to
  `generate-module-setup.md`'s, so generator and orchestrator never diverge):
  ```json
  {"type": "MISSING_IN_DOCS | NAMING_MISMATCH | MAPPING_GAP | ABSENT", "phase": "...", "sub": "...", "endpoint": "[METHOD] [path] or the field/contract in question", "detail": "what was missing/wrong", "resolution": "blocked pending spec clarification", "recorded_at": "[timestamp]"}
  ```
  Nothing else in that file may be touched. Append, never overwrite.
- **No `git commit`/`git push`** — leave changes in the working tree.
- **Required report-back format** (cap word count, keep it scannable): sub
  completed y/n; files created/changed (exact paths, plus an explicit "did NOT
  touch X" where ambiguity was possible); contract discrepancies vs. the spec
  and how resolved; any `api_doc_gaps` added; any OQ-blocked items; validation
  result; **skill compliance** (which skill file(s) checked, and for each:
  compliant or a named, justified deviation — not silence); and the exact scope
  of the `execution-state.json` edit.

### 1.3 — Orchestrator verification (after the agent reports, before the next sub)

Do this yourself, read-only, in this session:
- `git status --short` in the repo — confirm the changed-files list matches
  exactly what the agent claimed, nothing more.
- Re-run the validation command yourself if cheap (`mvn -q -DskipTests compile`),
  or at least read the agent's own run output critically.
- Re-read the relevant slice of `execution-state.json` to confirm the status
  update is scoped exactly as instructed (no other phase/sub/gap disturbed).
- Confirm the agent's report actually names the skill file(s) it checked and
  states compliant-or-deviation-with-reason for each — a report silent on this
  means the skill step was skipped, not that no skill applied; do not accept the
  sub as done on such a report. Send it back instead.
- If anything is off, send a follow-up to the same agent (by `agentId`, via
  `SendMessage`) to fix it in place — keep every code change attributable to a
  dispatched session, never patch it yourself.

### 1.4 — Impasse: second-agent debate before any escalation

**Trigger** — any one of:
- a dispatched agent reports it is blocked, or cannot complete its sub cleanly;
- STEP 1.3 verification reveals a failure the same-agent follow-up could not fix;
- STEP 2's mechanical gap resolution reaches step 5 (ABSENT — unresolved from
  the db-script, the SRS, cross-module artifacts, or existing source).

**An impasse is never escalated to the user on one agent's word.** Before any
escalation, put a SECOND agent on it and let the two converge. This is the same
philosophy as STEP 4's coverage debate and STEP 5's fixing agent: a second,
independently-reasoning agent grounded in the analysis files, not a human
interrupt, is the first response to a stall.

1. **Dispatch a SECOND agent** (`Agent` tool, read-heavy — it analyses, it does
   not write code) briefed with ALL of this module's analysis files:
   - the PRD (`$MBASE/P0_5/prd-<mod-lowercase>.md`) and the SRS
     (`$MBASE/P1/srs-<mod-lowercase>.md` — `RULE-ID`s / `AC-*`). The PRD is
     stage P0_5, NOT P1; `P0/` holds business-policies and the module registry;
   - the db-script under `$MBASE/P2/`;
   - the execution plan, this sub's own spec file, the phase's
     `[PHASE]-HEADER.md`, and `packages/backend-execution/_SECTIONS.md`;
   - the exact skill files this sub triggers, from `.claude/skills/` (the list
     built in 1.1);
   - PLUS the first agent's FULL account of what it attempted, what it observed,
     and precisely where it stalled.

   Its job: independently reason the impasse against those artifacts and propose
   one concrete, grounded resolution — traceable to a real artifact line, not an
   opinion. It reads only `.claude/skills/` and this module's own artifacts under
   `{MBASE}`; it needs nothing else.

2. **Let the two converge.** The orchestrator relays between them — a follow-up
   to the first agent by `agentId` via `SendMessage` carrying the reviewer's
   proposal, or a dispatch of the reviewer with the first agent's report as its
   input — until they agree on ONE concrete solution traceable to a real
   artifact. **Bounded, never open-ended**: if an exchange produces no new
   artifact-grounded evidence, the debate has not converged — stop it and move
   to step 5 rather than looping.

3. **Same invention ban as everywhere else.** The debate's job is to FIND the
   grounded answer, never to fabricate one. No agreed "solution" may invent a
   contract, column, route, endpoint, error code, or business rule. If the
   artifacts genuinely do not contain it, that is an ABSENT case — step 5 — not
   something the two agents may settle between themselves.

4. **Apply the agreed solution ONLY via a dispatched agent session** — the
   orchestrator still never edits code itself — then verify it per STEP 1.3
   before continuing to the next sub.

5. **Escalate to the USER only if** the debate could not converge AND the missing
   piece is genuinely human-only: a business decision stated in no artifact, an
   external credential/value, a real-world fact absent from every source. State
   exactly what is needed and why it is unreachable from the artifacts, in Arabic
   per the communication rule, and wait. In `--auto`, THIS is the HALT point —
   the debate runs first; the halt applies only when the debate also cannot
   ground the answer.

---

## STEP 2 — Spec-gap resolution (same repo only — never reaches into frontend)

Trigger: a dispatched agent's report includes a new `api_doc_gaps` entry, or you
notice one still open from a prior sub. A gap must be run to ground before the
current phase is considered safe to build on — do not carry an unresolved gap
into the next phase.

Resolution stays entirely inside THIS backend repo — the backend is the source
of truth for its own contracts; there is nothing to ask the frontend. Work the
gap in this order:

1. **The db-script** (`$MBASE/P2/db-script-<mod-lowercase>.md`) — is the field/column actually
   defined, under a different real name or suffix? If so it's a NAMING_MISMATCH:
   correct this side's understanding (spec/comment wording) and proceed.
2. **The SRS** (`$MBASE/P1/srs-<mod-lowercase>.md`) — does a RULE-ID specify the behavior the spec left
   implicit? If so it's MISSING_IN_DOCS resolved from the SRS; document it.
3. **A cross-module (INT-C/INT-R) dependency** — if the gap is a contract owned
   by ANOTHER backend module, read that module's own governance artifacts
   (the db-script, SRS, generated `api-docs/`) in this same repo to confirm the
   real contract. This is a same-repo cross-module read, never a cross-repo one.
4. **Existing backend source** — an already-built sibling entity's
   entity/mapper/service may already implement the analogous field correctly;
   mirror it (record it as resolved via source).
5. **Genuinely absent / contradictory** — if none of the above resolves it, it's
   ABSENT: do NOT guess and do NOT invent a contract. **First run the STEP 1.4
   second-agent debate on it** — a second agent, briefed with ALL of this
   module's analysis files plus what steps 1–4 already checked, reasons the gap
   independently; if the debate grounds an answer in a real artifact, apply it
   via a dispatched agent session and record that as the resolution. Only if the
   debate ALSO fails to ground it, escalate to the USER with the exact gap and
   what you checked, and leave the `api_doc_gaps` entry open with `resolution`
   describing the state. In `--auto`, that post-debate escalation is a HALT.

Whatever the outcome, update the gap's `resolution` in `execution-state.json`
from the placeholder to a factual note of what was found — keep the entry as a
historical record, never delete it — and sweep for any stale code comment that
still describes the field as an open gap. Only once every gap opened in the
current phase reads resolved (or is explicitly escalated) may the phase be
presented as done.

---

## STEP 3 — Phase closure and hand-back to the user

When the last sub in a phase completes and every gap is resolved:
1. Own final sweep: `git status --short`, a full compile/validation run,
   `execution-state.json` phase/sub statuses, and the `api_doc_gaps`
   resolutions.
2. Report to the user in Arabic, concisely: what got built, any corrections
   applied, any gaps found-and-fixed (with the real root cause, not just
   "resolved"), any non-blocking issues worth flagging for later — help them
   decide, don't just narrate.
3. Print the next phase's assessment and wait for explicit confirmation before
   dispatching anything in it. (In `--auto`, print it and proceed automatically
   — unless this phase closed on a halt condition, in which case wait regardless
   of `--auto`.)

---

## STEP 4 — Test phase (a real gated phase, entered in the same run)

Trigger: STEP 3 just closed the LAST key in `$EXEC_PHASES` (`ALIGN-BE` as published today) — and
every gap opened during it is resolved (never escalated-and-still-open).

> **The test phase is a gated phase.** It is entered behind the explicit human
> "Proceed?" gate, then runs test → coverage debate → fix as one continued
> flow (steps 3–5), halting only on failure or on human-only input it cannot
> reach. Code review (`debate-review`) and anything after it stay SEPARATE,
> manually-run steps — named at the end, never dispatched from here.

Treat the test phase exactly like `CORE … ALIGN-BE`:

1. **Read the test phase(s).** From `execution-state.json` read `test_phases[]`
   — one entry per real test phase, with its `header_file` naming where that
   phase's plan actually lives. The delivery comes in two shapes and which one
   applies is a per-module fact: the split folder `$PKGS/backend-test/` (today
   the common one) or a single flat `$MBASE/test_gen/backend-test-plan-<mod>.md`.
   Follow `header_file`; read whatever the array holds; assume no fixed shape.

2. **Print the SAME phase-assessment block** used for every execution phase, and
   wait for explicit confirmation (respect `--auto` identically: print but do
   not wait; still halt on any failure):
   ```
   ══════════════════════════════════════════════════════
   PHASE ASSESSMENT — [MODULE] / TEST
   ══════════════════════════════════════════════════════
   Test phases  : [list each test phase + its pending subs]
   Gated by     : all backend execution phases (CORE … ALIGN-BE) — COMPLETE
   Plan         : run /[MODULE]/execute-backend-test → debate coverage (2nd agent)
                  → fix agreed fail/blocked → close
   ══════════════════════════════════════════════════════
   Proceed?
   ```

3. **On confirmation, dispatch `/[MODULE]/execute-backend-test`** as this run's
   next phase, using the SAME one-dispatch / wait-for-report / verify discipline
   as a sub (STEP 1.2 dispatch, STEP 1.3 verification). That command reads the
   delivered `TC-[MODULE]-<seq>` plan from whichever location its own STEP 0.1
   records for this module, then —
   BEFORE any verification runs — regenerates this module's api-docs via
   `governance/governance-tools/api-doc-generator` (never verify against a
   possibly-stale copy), then invokes the `api-verify` skill
   (`.claude/skills/api-verify/SKILL.md`, this repo's sole adopted backend API
   verification mechanism — TestSprite is retired, never dispatch it) scoped to
   this module, and emits the governed-plan ↔ api-verify coverage table
   (`TC-[MODULE]-<seq>` → the matching `test_<entity>()` traceability comment,
   or "✗ no matching test"). Read that report and that table.

4. **Coverage decision — adopt a second agent to debate it.** Do not accept the
   run's verdict as final on its own. Dispatch a SECOND agent (read-only,
   separate from the one that produced the report) whose only job is to review
   this module's ANALYSIS files against the coverage table and challenge the
   verdict:
   - It reads the analysis sources — the PRD (`$MBASE/P0_5/`), the SRS
     (`$MBASE/P1/`, `AC-*`), the execution
     plan, and the delivered test plan at the location `header_file` names
     (including any `INT-XM` phase and the `XM-*`/`UXD-*` those integration
     `TC-*` trace).
   - For every claimed GAP it asks: is this genuinely uncovered, or already
     exercised by another `test_<entity>()` traceability comment under a
     different `TC-*`? For every claimed PASS it asks: does the
     `AC-*`/`XM-*`/`UXD-*` behind that `TC-*` actually get verified, or only
     touched superficially?
   - The two agents exchange until they converge on ONE agreed set of real
     failures / real coverage gaps / blocked items. Only that agreed set is
     written to the report as the verdict — a disputed "gap" that the debate
     resolves as already-covered is struck; a "pass" the debate finds hollow
     becomes a gap. This is the correct-decision step, not a rubber stamp.

5. **After tests: continue with a fixing agent on the agreed fail/blocked set.**
   If the agreed verdict (step 4) is clean, skip to step 6. Otherwise do NOT
   just halt — hand the report's agreed FAIL + BLOCKED + GAP items to a fixing
   agent that continues the work:
   - It fixes application code for real failures and fills real coverage gaps,
     following the binding skills in `.claude/skills/` (a skill outranks
     codebase precedent), then re-runs the affected tests via
     `/[MODULE]/execute-backend-test` and re-checks coverage until the agreed
     set is closed.
   - It derives everything it can from what it can reach — the analysis files,
     the code, the skills, `execution-state.json`. It works autonomously on all
     of that without pausing.
   - **It stops and asks the human ONLY for information it genuinely cannot
     reach** — a business decision not stated in any analysis file, an external
     credential/value, a real-world fact absent from every artifact. It never
     stops to ask for anything derivable from the sources above. When it does
     stop, it reports in Arabic per the communication rule, states exactly what
     human input it needs and why it is unreachable, and waits.
   - Any item that cannot be closed without that human input is left BLOCKED
     with the reason recorded; the run halts there rather than presenting the
     module as done.

6. **Only when the agreed set is fully closed** (every `TC-*` covered and
   passing, `test_phases[]` set to COMPLETE), print the closing banner naming
   the remaining SEPARATE, manually-run steps — do NOT dispatch them:
   ```
   ══════════════════════════════════════════════════════
   MODULE COMPLETE — [MODULE]
   ══════════════════════════════════════════════════════
   Implementation : all phases COMPLETE (CORE → ALIGN-BE)
   Tests          : all governed TC-* covered & passing (coverage table in report)

   Next steps (run separately, when ready — NOT auto-run from here):
     Code review : debate-review --local
   ══════════════════════════════════════════════════════
   ```

---

## Constraints (non-negotiable, every sub, every phase)

- NEVER treat this as anything but a BACKEND command — no frontend concept, no
  cross-repo reach into `frontend/`, ever. The one sanctioned cross-repo
  artifact in the ecosystem (the published module registry) is the tooling's
  concern, not this orchestrator's.
- NEVER skip the phase-assessment gate, and never advance a phase without the
  user's explicit instruction — the ONLY exception is `--auto`, which
  auto-advances between CLEAN phases (still printing each assessment) and still
  HALTS on any sub error, validation failure, silent/non-compliant skill report,
  or unresolved gap — in each case after the STEP 1.4 second-agent debate fails
  to resolve it, never before running that debate — and never skips the per-sub
  skill read or STEP 1.3 verification.
- NEVER dispatch more than one sub's agent at a time, even for LIGHT subs, even
  when they look independent.
- NEVER escalate an execution-time impasse to the user before running the STEP
  1.4 second-agent debate on it; and never let that debate run unbounded — it
  converges on an artifact-grounded solution, or it ends and the impasse
  escalates. The debate weakens no stop condition: it is an extra attempt before
  the halt, and the halt still stands when it fails.
- NEVER invent a route path, entity/field/column name, endpoint, error code, or
  permission code — trace every value to a real spec block, the db-script, or
  SRS entry; raise a gap or an OQ instead of guessing.
- NEVER redesign an entity/repository/service/controller that already exists.
- NEVER write an XM-ID reference in code.
- NEVER dispatch a sub without first identifying (against the skills index from
  STEP 0.4) and reading, in full, every skill file that sub's work triggers.
  Matching real code precedent is necessary but not sufficient — precedent can
  itself be non-compliant, and only an actual skill read catches that. This
  discipline was learned the hard way on a frontend-side run of this same
  ecosystem (2026-08-29: 30 subs dispatched with zero skill files read,
  discovered only when the user asked — `create-forms`' RHF requirement and
  `enforce-permissions`' Layer-3 `can()` check were both silently violated
  module-wide). The incident was frontend, the discipline is identical here —
  don't wait for a backend repeat to take it seriously.
- ALWAYS update `execution-state.json` after every sub, scoped exactly as
  described — never let two subs' updates land in one dispatch.
- ALWAYS keep every code change attributable to a dispatched Claude Code
  session; this orchestrating session reads, briefs, verifies, and reports — it
  does not edit source itself.
- ALWAYS re-read this command file in full immediately before every sub's STEP
  1.1 prep — never rely on memory of it from earlier in the same conversation,
  no matter how many subs deep the run is.
