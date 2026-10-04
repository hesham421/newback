# /erp-core/orchestrate-plan

Master orchestrator for the **erp-core technical plan** (`erp-core-plan/` at
the repo root, steps 01–13). It is the plan-level counterpart of
`orchestrate-module.md`, adapted to this task: there is no governance
submodule, no module registry and no business spec — the step files **are**
the spec. It drives `/erp-core/execute-step` per step through dispatched
agents, adds an independent **reviewer agent** per step, runs independent
steps **in parallel in git worktrees**, merges in numeric order, and supports
full autonomous execution (`--auto`).

```
Lives at : .claude/commands/erp-core/orchestrate-plan.md
Reads    : erp-core-plan/execution-state.json (+ the step files, README, reports)
Writes   : erp-core-plan/execution-state.json (sole writer, on main), merges to main
```

## Usage
```
/erp-core/orchestrate-plan [--auto] [--from NN] [--only NN] [--parallel N] [--no-parallel]
```
- `--auto`: autonomous. No human gate between steps; still **halts** on an
  unresolved failure after the second-agent debate (STEP 4), and on a step that
  genuinely needs human input (external repos/credentials — step 13).
- `--from NN`: start at step NN (earlier steps must already be `COMPLETE`).
- `--only NN`: run one step, then stop (still reviewed and merged).
- `--parallel N`: max concurrent worktrees for the parallel group (default:
  `.max_parallel` in the state file, 3). `--no-parallel`: strictly sequential.
- Without `--auto`: print each step/batch assessment and wait for "proceed".

## Step 0 — Resolve, never type
```bash
ROOT=$(git rev-parse --show-toplevel)
STATE=$ROOT/erp-core-plan/execution-state.json
test -f "$STATE" || { echo "run /erp-core/generate-plan-setup first"; exit 1; }
MAIN=$(jq -r .main_branch "$STATE")
```
Preconditions checked once per run: clean working tree on `$MAIN`
(`git status --porcelain` empty), Docker reachable (`docker info`), JDK 21+
(`java -version`), Maven present. Any failure → STOP with the exact missing
item (this is environment, not a plan impasse — no debate).

**Portability:** derive every path at runtime; never write a machine-specific
absolute path into this file, the state file, or a dispatched prompt. Worktrees
live under `$ROOT/.worktrees/step-NN` (add `.worktrees/` to `.gitignore` in the
first run if missing).

---

## Role of the orchestrating session

**It never writes code, never edits a step file, never resolves a conflict by
hand.** It only: reads state and reports; builds dispatch prompts; dispatches
agents (`Agent` tool); verifies reports against the working tree; relays between
implementer and reviewer; merges via a dispatched merge agent; is the **only
writer** of `erp-core-plan/execution-state.json` (always on `$MAIN`, each
update committed as `plan(NN): <event>`); and talks to the user
**in Arabic, concisely** (agents are briefed in **English, in full detail**).

Three agent roles, every step:
| Role | Writes code? | Runs where | Job |
|---|---|---|---|
| **Implementer** | yes | the step's worktree/branch | follows `/erp-core/execute-step NN` |
| **Reviewer** | no (read + run commands) | the same worktree, read-only | independently re-runs verification, checks every acceptance bullet, diff-reviews against the step file and README rules, returns PASS/FAIL with evidence |
| **Merger** | git only | `$ROOT` on `$MAIN` | rebases the step branch on `$MAIN`, runs the full verify, merges `--no-ff`, records `merged_commit` |

---

## STEP 1 — Schedule

1. Read the state file. Build the ready set: steps `PENDING` whose `depends_on`
   are all `COMPLETE`. Honour `--from/--only`.
2. **Batching rule.** Steps with `parallel_group == null` run **one at a time**
   (01→05, 12, 13). Steps with `parallel_group == "A"` (06–11) run as a
   **batch**: up to `--parallel` worktrees concurrently, each on its own branch
   from the current `$MAIN`, but **merged strictly in numeric order** (06 first)
   — a later step is rebased onto the merged earlier ones before its own merge,
   and its reviewer re-runs verification after the rebase.
3. Print the assessment and (without `--auto`) wait:
   ```
   ══════════════════════════════════════════════════════
   PLAN ASSESSMENT — erp-core / batch <ids>
   ══════════════════════════════════════════════════════
   Ready steps   : [id slug — depends_on ✓]
   Mode          : sequential | parallel (N worktrees), merge order <ids>
   Per step      : implementer → reviewer (≤3 fix rounds) → merger → verify on main
   Halt on       : reviewer FAIL after rounds + debate, env failure, human-only input
   ══════════════════════════════════════════════════════
   Proceed?
   ```

## STEP 2 — Per-step pipeline

### 2.1 Prepare (orchestrator, read-only + git)
- Re-read **this file in full** before every step (instruction drift is real on
  13-step runs); re-read the README and the step file.
- Create the worktree via a tiny dispatched git action or directly (git
  plumbing is allowed for the orchestrator; code is not):
  `git worktree add -b <branch> .worktrees/step-NN $MAIN` (sequential steps may
  use the main checkout instead; parallel steps **must** use worktrees).
- State: `status: IN_PROGRESS`, `worktree`, `current_step`, `log`.

### 2.2 Dispatch the implementer (Agent tool, background allowed for the parallel batch)
Self-contained English prompt containing:
- the absolute worktree path (derived this run) and the branch; "you are in a
  git worktree of repo X; never touch `$MAIN` or other worktrees";
- the exact files to read first, as fully expanded paths:
  `<worktree>/erp-core-plan/00-README-EXECUTION-PLAN.md`,
  `<worktree>/erp-core-plan/NN-STEP-….md`, `<worktree>/docs/DEVIATIONS.md`,
  the `docs/steps/<dep>-report.md` of every dependency, and the command file
  `<worktree>/.claude/commands/erp-core/execute-step.md` — "follow it exactly as
  `/erp-core/execute-step NN --dispatched`" (dispatched mode: the agent never
  edits `execution-state.json`; the orchestrator owns that file on `$MAIN`);
- the skills to read (`<worktree>/.claude/skills/...`) that the step's tasks
  trigger, listed by path;
- "no questions: decide by README defaults and record deviations";
- "run every verification command and paste real output; commit on the branch;
  update only this step in the state file; report in the STEP 2 format".
- Record `implementer_agent` (agentId) in the state.

### 2.3 Orchestrator verification of the implementer's report
- `git -C <worktree> status --short` must be empty (everything committed) and
  `git log $MAIN..<branch> --oneline` must show `step(NN)` commits.
- `docs/steps/NN-report.md` exists in the last commit; the branch did **not**
  touch `erp-core-plan/execution-state.json`
  (`git diff --quiet $MAIN..<branch> -- erp-core-plan/execution-state.json`).
  The orchestrator then records on `$MAIN`: `status: REVIEW`, `report`,
  `attempts += 1`, `deviations` (from the report), `log`.
- A report silent on *Skills checked* or *Acceptance checklist* is incomplete →
  `SendMessage` to the implementer to complete it; do not proceed to review.

### 2.4 Dispatch the reviewer (Agent tool, separate agent, same worktree, read-only)
Prompt: the same file list as 2.2 plus the implementer's full report and the
diff range `$MAIN..<branch>`. Its job, in order: (1) re-run **every**
verification command itself and record output; (2) walk the step's
`## Acceptance` bullets one by one with evidence (command output, file path +
line, test name); (3) diff review against the step's `## Tasks`, `## Out of
scope`, the README rules (§1, §4) and the additive-only migration rule;
(4) check that deviations are recorded, not silent; (5) return
`VERDICT: PASS` or `VERDICT: FAIL` followed by a numbered list of concrete,
actionable findings (file, line, what, why, which rule). It may not edit files.
Record `reviewer_agent` and `review{verdict, round, evidence}`.

### 2.5 Fix loop (bounded)
On `FAIL`: state `status: FIXING`; `SendMessage` the reviewer's findings to the
**same implementer** (by agentId) with "fix only these, re-run verification,
amend the report, commit, set state back to REVIEW"; then re-run 2.3 and 2.4
with `round += 1`. **Maximum 3 rounds.** Still FAIL after round 3 → STEP 4
(impasse debate). A reviewer finding that is itself wrong (the implementer
shows artifact-grounded evidence) is settled by the orchestrator relaying both
positions once; unresolved disagreement also goes to STEP 4.

### 2.6 Merge (dispatched merger agent)
On `PASS`: state `status: MERGING`. Dispatch a merger with: `$ROOT`, `$MAIN`,
the branch, and instructions: `git checkout $MAIN && git pull --ff-only` (if a
remote exists) → `git rebase $MAIN` on the branch (in the worktree) → if
conflicts: resolve **only** in `docs/DEVIATIONS.md` and `docs/CHANGELOG.md`
by keeping both sides in order; any conflict in source, migrations or tests is
**not** resolved by the merger — it reports back and the orchestrator sends
the conflict to the implementer (counts as a fix round) → `mvn -q verify` on
the rebased branch → `git merge --no-ff <branch> -m "merge: step(NN) <slug>"`
on `$MAIN` → `mvn -q verify` on `$MAIN` → report the merge sha. Record
`merged_commit`, `status: COMPLETE`, remove the worktree
(`git worktree remove .worktrees/step-NN`), keep the branch.
A post-merge verify failure on `$MAIN` is a halt condition handled by STEP 4
(the merge is reverted first: `git revert -m 1 <merge-sha>`).

### 2.7 Parallel batch specifics (06–11)
- Dispatch up to N implementers concurrently (`run_in_background: true`), each
  in its own worktree. Reviews run as reports arrive.
- Merge strictly in numeric order: step 07 may be `PASS` before 06 — it waits.
  After 06 merges, 07's merger rebases 07 onto the new `$MAIN`; if the rebase
  changed anything in 07's tree (not just fast-forward), the **reviewer re-runs
  verification** on the rebased branch before the merge proceeds.
- Migration numbers are reserved per step (state `migrations`), so rebases do
  not collide on Flyway versions; a collision found anyway is a FAIL for the
  later step.

## STEP 3 — Step closure and hand-back
After each merge: `git status --short` clean on `$MAIN`; state consistent
(`COMPLETE`, `merged_commit`, worktree removed); report to the user in Arabic in
≤ 8 lines: what the step delivered, deviations recorded, review rounds, merge
sha, anything a later step must watch. Then the next assessment (wait unless
`--auto`).

## STEP 4 — Impasse: second-agent debate before any halt
Trigger: implementer BLOCKED; 3 failed review rounds; merge/verify failure on
`$MAIN`; implementer–reviewer disagreement.
1. Dispatch a **third agent** (analyst, read-only) briefed with: README, the
   step file, the dependency reports, `DEVIATIONS.md`, the implementer's and
   reviewer's full accounts, the failing output. Its job: one concrete,
   grounded resolution traceable to a plan line / README default / real build
   output — or the finding that the plan itself is wrong at a named line.
2. Relay between analyst and implementer (bounded: stop when an exchange adds
   no new evidence). An agreed resolution is applied **only** by the
   implementer, then re-reviewed (2.4) — the debate removes no check.
3. If the agreed resolution requires changing a **plan file** (a step file or
   README): the orchestrator does not edit it. In `--auto`, record the proposed
   edit in `halts[]` with `needs_human: true` and halt — plan edits are the
   owner's. Without `--auto`, present the proposed edit to the user and wait.
4. Halt (`BLOCKED` + `halts[]`) only if no grounded resolution exists or the
   missing piece is human-only. In `--auto` the run stops here; everything
   merged so far stays merged.

## STEP 5 — Step 13 (external repos) and step 12 (publishing)
- Step 12 needs a Git remote and `GITHUB_TOKEN` for the publish/consume jobs:
  if absent, the implementer completes everything local (ArchUnit, jacoco, CI
  yaml, docs, version bump, tag) and records the publish jobs as *untested*
  in the report; the reviewer treats that as PASS with a named gap; the
  orchestrator records a `halts[]` entry `needs_human: true, reason: "push +
  tag to run publish/consume jobs"` **without** stopping the run.
- Step 13 touches the factory and `governance-shared` repos. The orchestrator
  requires their local paths from env `ERP_FACTORY_DIR` and `ERP_GOV_SHARED_DIR`
  (checked at STEP 1 when 13 becomes ready). Missing → in `--auto`: 13 is set
  `BLOCKED, needs_human: true` and the run finishes cleanly after 12; without
  `--auto`: ask the user for the two paths once.

## STEP 6 — Plan closure
When 01–12 are `COMPLETE` (13 `COMPLETE` or `BLOCKED` for human reasons only):
```
══════════════════════════════════════════════════════
PLAN COMPLETE — erp-core technical plan
══════════════════════════════════════════════════════
Steps          : 01–12 COMPLETE on main (merge shas listed)   13: <status>
Verify on main : mvn -q verify — PASS
Open for human : <halts with needs_human:true, one line each>
Later phase    : /erp-core per-module docs (erp-core-plan/99-LATER-per-module-docs.md) — NOT run from here
══════════════════════════════════════════════════════
```

## Constraints (NON-NEGOTIABLE)
- NEVER let the orchestrating session write source, migrations, tests or plan
  files; every change is attributable to a dispatched agent and a branch.
- NEVER merge a step without a reviewer `PASS` from a **different** agent than
  the implementer, and never without a green `mvn -q verify` on the rebased
  branch **and** on `$MAIN` after the merge.
- NEVER merge the parallel group out of numeric order; never skip the
  post-rebase re-verification when a rebase was not a fast-forward.
- NEVER run two sequential-group steps concurrently; never exceed `--parallel`.
- NEVER escalate to the user before the STEP 4 debate; never let the debate
  loop without new evidence.
- NEVER edit a step file or the README from this run — a plan defect is a
  human halt, not an in-flight patch.
- ALWAYS re-read this file before each step; always keep the state file the
  single source of progress, **written only by this orchestrator on `$MAIN`**
  (dispatched agents never touch it — this is what keeps parallel branches
  conflict-free). Commit each state update on `$MAIN` as
  `plan(NN): <event>`.
