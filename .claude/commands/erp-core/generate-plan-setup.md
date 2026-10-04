# /erp-core/generate-plan-setup

Generates (or refreshes) the execution state for the **erp-core technical plan**
from the step files in `erp-core-plan/` at the repo root, and (re)generates the
per-step executor command `.claude/commands/erp-core/execute-step.md` from the
template in Step 3. Mirrors `generate-module-setup.md`, but this plan is
**self-contained in the repo**: there is no `governance/shared/` submodule, no
`profile-summary.json`, no module registry. Everything it needs is under
`erp-core-plan/`.

```
Lives at : .claude/commands/erp-core/generate-plan-setup.md
Reads    : erp-core-plan/00-README-EXECUTION-PLAN.md, erp-core-plan/NN-STEP-*.md
Writes   : erp-core-plan/execution-state.json, .claude/commands/erp-core/execute-step.md
```

## Usage
```
/erp-core/generate-plan-setup [--force]
```
Without `--force`, an existing `execution-state.json` is **merged** (statuses,
branches, reports and deviations already recorded are preserved; new steps are
added; removed steps are flagged, never deleted). With `--force` the file is
rebuilt from scratch — only allowed when no step is `IN_PROGRESS` or `COMPLETE`.

## Step 0 — Resolve the repo root and the plan folder (never type a path)
```bash
ROOT=$(git rev-parse --show-toplevel)
PLAN=$ROOT/erp-core-plan
test -f "$PLAN/00-README-EXECUTION-PLAN.md" || { echo "MISSING $PLAN/00-README-EXECUTION-PLAN.md"; exit 1; }
ls "$PLAN"/[0-9][0-9]-STEP-*.md
```
If the README or any step file is missing: STOP and say which.

## Step 1 — Scan the plan (mechanical)
For each `NN-STEP-<slug>.md` (numeric order), extract:
- `id` = `NN`; `slug`; `file` = repo-relative path.
- `branch` from the line starting `**Branch:**` (e.g. `step/05-multi-tenancy`).
- `depends_on` from the README's order table column *Depends on* (`—` → `[]`, `06–11` → the expanded list). The README is authoritative over anything inferred.
- `migrations` reserved by the step: every `V<n>__…sql` named under its *Tasks* that is **created** by the step (not the old-chain references in steps 01/04).
- `verification` = the fenced block under `## Verification commands` (verbatim lines); if absent, `["mvn -q verify"]`.
- `acceptance` = the bullet lines under `## Acceptance` (verbatim).
- `touches_external_repos` = `true` only for a step whose *Repos:* line names repositories other than this one (step 13).
- `parallel_group` = `"A"` for steps the README marks as independent after 05 (06–11); otherwise `null`.

`99-LATER-*.md` is **not** a step: record it under `later[]` only.

## Step 2 — Generate `erp-core-plan/execution-state.json`
```json
{
  "plan": "erp-core-technical",
  "plan_readme": "erp-core-plan/00-README-EXECUTION-PLAN.md",
  "generated_at": "<ISO timestamp>",
  "repo_root_note": "paths are repo-relative; the orchestrator resolves the root at runtime",
  "main_branch": "main",
  "current_step": "01",
  "max_parallel": 3,
  "steps": [
    {
      "id": "01", "slug": "decouple-fin", "file": "erp-core-plan/01-STEP-decouple-fin.md",
      "branch": "step/01-decouple-fin", "depends_on": [], "parallel_group": null,
      "migrations": [], "touches_external_repos": false,
      "status": "PENDING",
      "worktree": null, "attempts": 0,
      "implementer_agent": null, "reviewer_agent": null,
      "review": null,
      "merged_commit": null,
      "report": null,
      "deviations": [],
      "verification": ["mvn -q -DskipTests package", "…"],
      "acceptance": ["…"]
    }
  ],
  "later": [ { "id": "99", "file": "erp-core-plan/99-LATER-per-module-docs.md", "status": "NOT_SCHEDULED" } ],
  "halts": [],
  "log": []
}
```
Status vocabulary (the only values allowed): `PENDING`, `IN_PROGRESS`, `REVIEW`,
`FIXING`, `MERGING`, `COMPLETE`, `BLOCKED`, `SKIPPED`.
`review` shape when present: `{"verdict":"PASS|FAIL","round":n,"evidence":"…","checked_at":"…"}`.
`halts[]` entry: `{"step":"NN","reason":"…","needs_human":true|false,"recorded_at":"…"}`.
`log[]` entry: `{"at":"…","step":"NN","event":"dispatched|reported|review|fix|merged|halt","note":"…"}`.

Merge rule (no `--force`): for every step already in the file keep `status,
worktree, attempts, *_agent, review, merged_commit, report, deviations` and
refresh only `file, branch, depends_on, parallel_group, migrations,
verification, acceptance`. Steps present on disk but absent in the file are
appended as `PENDING`; steps in the file but gone from disk get
`"status":"SKIPPED","note":"step file removed"` — never deleted.

## Step 3 — Generate `.claude/commands/erp-core/execute-step.md`
Write the file **exactly** as the template in that file's current committed
version, substituting nothing — it is parameterised by the state file at run
time, not by this generator. If the committed `execute-step.md` is missing,
STOP: it is part of the delivered plan package, not something to invent.
(The generator's only job regarding that file is to confirm it exists and that
its `## Step vocabulary` section matches the status vocabulary above; if not,
update that section only.)

## Step 4 — Verify and report
- `jq . erp-core-plan/execution-state.json` parses; step ids are `01..13`
  contiguous; every `depends_on` id exists; no two steps reserve the same
  migration version; every `parallel_group:"A"` step depends on `05`.
- Print, in Arabic, a one-screen summary: number of steps, the dependency
  chain, the parallel group, any steps already COMPLETE (when merging), and
  the command to start: `/erp-core/orchestrate-plan --auto`.

## Constraints
- NEVER write anything outside `erp-core-plan/execution-state.json` and
  `.claude/commands/erp-core/execute-step.md`.
- NEVER type a step list, a branch name or a migration number by hand — every
  value is read from the step files and the README.
- NEVER downgrade a populated state file to the skeleton without `--force`.
