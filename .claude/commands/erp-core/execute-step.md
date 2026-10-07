# /erp-core/execute-step

> Note (2026-10-07): the erp-core plan is complete (steps 01–13, `erp-core-plan/execution-state.json`) and the governance is now vendored inside this repository under `governance/`; this command is kept as completed-plan history.

Executes **one step** of the erp-core technical plan in the current checkout
(a worktree when the orchestrator dispatched it, the main checkout when run by
hand). This is the per-step counterpart of `[MODULE]/execute-backend.md`; it
is what a dispatched implementer agent follows, and it can also be run
interactively.

## Usage
```
/erp-core/execute-step NN [--dispatched] [--no-commit]
```
- `NN` (required): a step id present in `erp-core-plan/execution-state.json`.
- `--dispatched`: set by the orchestrator. In this mode the step **never edits
  `execution-state.json`** (the orchestrator owns it on `main`; editing it on a
  branch would create merge conflicts) — status changes are conveyed through
  the report instead. Without the flag (interactive use) this command updates
  the state file itself as described below.
- `--no-commit`: leave changes in the working tree (interactive use only; the
  orchestrator never passes it).

## Step vocabulary
`PENDING → IN_PROGRESS → REVIEW → (FIXING → REVIEW)* → MERGING → COMPLETE`, or
`BLOCKED`. `SKIPPED` only for steps whose file was removed.

## STEP 0 — Context and safety (MANDATORY)
```bash
ROOT=$(git rev-parse --show-toplevel)
STATE=$ROOT/erp-core-plan/execution-state.json
STEP=$(jq -r --arg id "$NN" '.steps[] | select(.id==$id)' "$STATE")
```
1. The step must exist and be `PENDING` or `FIXING`. Any other status → STOP and
   say so (never re-run a `COMPLETE` step; never start a `BLOCKED` one).
2. Every id in `depends_on` must be `COMPLETE` **on `main`** (check
   `merged_commit` is set, and `git merge-base --is-ancestor <merged_commit> HEAD`
   for the current branch). Otherwise STOP: the orchestrator sequences steps;
   this command never works around a missing dependency.
3. You must be on the step's branch (`git branch --show-current` equals
   `.branch`). If the branch does not exist, create it from `main`:
   `git checkout -b <branch> main`. If it exists but you are not on it, STOP.
4. Read, **in full and in this order**:
   - `erp-core-plan/00-README-EXECUTION-PLAN.md` (rules, defaults, DoD);
   - this step's file (`.file`);
   - `docs/DEVIATIONS.md` if it exists (earlier deviations bind later steps);
   - `docs/steps/<prev>-report.md` for every id in `depends_on` (what the
     previous steps actually did, including deviations and renamed things);
   - the skills under `.claude/skills/` whose scope the step's tasks trigger
     (`build-*` for code generated, `gov-*` for validation) — read each in
     full; the README's conventions and a skill both bind; the step file wins
     only where it explicitly overrides (e.g. new table naming).
5. Print the assessment (no wait when dispatched by the orchestrator; wait when
   interactive):
   ```
   ══════════════════════════════════════════════════════
   STEP ASSESSMENT — erp-core / NN <slug>
   ══════════════════════════════════════════════════════
   Branch       : <branch>   (worktree: <path or main checkout>)
   Depends on   : [ids] — all COMPLETE on main
   Tasks        : <count> (from the step file's ## Tasks)
   Migrations   : <reserved versions or none>
   Verification : <commands>
   ══════════════════════════════════════════════════════
   ```
6. Interactive only: set the step `status` to `IN_PROGRESS` (only this field +
   a `log[]` entry). With `--dispatched`: do nothing to the state file.

## STEP 1 — Execution
Work through the step file's `## Tasks` **in order**. For each task:
1. Before creating anything, check whether it already exists (class, table,
   endpoint, property). If it exists: modify/integrate, never create a
   competing implementation.
2. Every value — table, column, property key, endpoint path, permission code,
   error code, event name — is taken from the step file or the README defaults.
   If the step file is silent, apply the README `§4 Defaults`. If still
   undecidable, choose the option that preserves the step's `Goal`, implement
   it, and record it in `docs/DEVIATIONS.md` as
   `- [NN] <what was unspecified> → <what was chosen> (<why>)`. **Never stop
   to ask.**
3. Keep changes inside the step's scope (`## Out of scope` is binding). A
   change the step needs in a file owned by an earlier step is allowed only
   when additive and named in your report.
4. Tests listed under the step's `## Tasks` are part of the step — write them.

After the last task:
5. Run **every** line of the step's `## Verification commands` and every check
   implied by `## Acceptance`, and record real output. Never claim a pass you
   did not run. A failing check → fix and re-run (bounded: 5 attempts per
   check); still failing → go to STEP 3 (blocked).
6. Write `docs/steps/NN-report.md` with exactly these sections: *Summary*,
   *Files changed* (paths, grouped created/modified/deleted), *Decisions &
   deviations* (mirrors `docs/DEVIATIONS.md` entries added), *Acceptance
   checklist* (every bullet of the step's `## Acceptance` with ✅/❌ and the
   evidence line), *Verification output* (trimmed), *Skills checked* (path →
   compliant | named deviation + reason), *Notes for later steps*.
7. Commit on the step branch (unless `--no-commit`): message
   `step(NN): <summary>` + body + the attribution lines the session requires.
   One or more commits are fine; the last one must contain the report.
8. Interactive only: update `execution-state.json` — this step only —
   `status: "REVIEW"`, `report: "docs/steps/NN-report.md"`, `attempts += 1`,
   `deviations` (list of the entries you added), and one `log[]` entry; commit
   it on the same branch. With `--dispatched`: **do not touch the state file**;
   put the same facts in your report (the orchestrator records them).

## STEP 2 — Report back (to the orchestrator or the user)
Scannable, capped: step id; completed y/n; branch + last commit sha; files
created/changed/deleted (paths); acceptance checklist result (n/N ✅, each ❌
with the reason); verification results (each command → pass/fail); deviations
added; skills checked and compliance; the exact scope of the state-file edit;
anything a later step must know.

## STEP 3 — Blocked
If a verification/acceptance item cannot be made to pass within the bounds
above, or a task is impossible as written (e.g. an API removed in the Spring
Boot version in use):
1. Do **not** mark the step COMPLETE or REVIEW. Interactive: set
   `status: "BLOCKED"` with a `halts[]` entry: `reason` (precise, with the
   failing command/output), `needs_human: false` (the orchestrator's debate
   decides whether a human is needed — never set `true` yourself). Dispatched:
   report `BLOCKED: <reason>` as the first line instead.
2. Commit what is green so far on the step branch with
   `step(NN): WIP — blocked: <reason>`.
3. Report per STEP 2 with the block clearly first.

## Constraints (NON-NEGOTIABLE)
- NEVER edit `main` directly; never merge — merging is the orchestrator's.
- NEVER run a step whose dependencies are not COMPLETE on main.
- NEVER touch another step's reserved migration version; never renumber an
  existing core migration after step 04.
- NEVER add a column/table without `TENANT_ID` after step 05 unless the step
  file names it global (`GlobalAuditableEntity`).
- NEVER ask the user a question; decide per the README defaults and record a
  deviation.
- NEVER claim a verification you did not run; paste the real output.
- ALWAYS commit the report before reporting back; interactive runs update the
  state file scoped to this step only, dispatched runs never touch it.
