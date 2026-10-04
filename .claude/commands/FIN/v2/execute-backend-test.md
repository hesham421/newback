# /FIN/v2/execute-backend-test

Execute API verification for **FIN v2** — only for what's actually complete.

> **Self-contained.** This command needs `governance/governance-tools/api-doc-generator`,
> the `api-verify` skill (`.claude/skills/api-verify/SKILL.md`),
> `governance/shared/platform/rules/api-verify-config.md`, and FIN's own v2 artifacts under
> `governance/shared/analysis/modules/FIN/v2/` and
> `governance/shared/backend/modules/FIN/v2/`. Every rule it relies on is written
> below or in those two files — it reads no other external mechanism/governance doc,
> never stops waiting on one, and never calls TestSprite (retired as this project's
> backend test mechanism).

## Usage
/FIN/v2/execute-backend-test

---

## PRECONDITION — the v2 test plan IS delivered (Shape B)

Re-scanned 2026-09-23, after `./scripts/governance pull` brought the factory's
test-gen publish. The live delivery shape for FIN v2 is **Shape B — one flat file**:

- Shape B — `governance/shared/analysis/modules/FIN/v2/test_gen/backend-test-plan-fin.md`
  → **present** (141KB). One `<!-- PHASE:TEST-PLAN-BE -->` block, two nested subs:
  `RULE-SCENARIOS` (45 TCs) and `API-SCENARIOS` (45 TCs) = **90 backend TCs**.
- Shape A — `governance/shared/backend/modules/FIN/packages/v2/backend-test/`
  → still only `.gitkeep`; the v2 manifest still reports
  `status.split["backend/test"] = false`. **Do not read it.**
- No `INT-XM` phase: the plan states outright that scope = module, so none is
  emitted and none is owed. Its absence is correct, not a coverage gap.
- Also delivered, and used by `api-verify` for the **Full** tier:
  `governance/shared/analysis/modules/FIN/v2/test_gen/test-execution-manifest-fin.md`.

`test_phases[]` in `governance/shared/backend/modules/FIN/v2/execution-state.json`
carries that one phase with its two subs, gated on all 8 execution phases — all
`COMPLETE` today, so the STEP 0.2 gate passes.

Still never fall back to v1's plan under
`governance/shared/backend/modules/FIN/packages/backend-test/` — that plan is v1's
REQUIRED COVERAGE and was already verified COMPLETE (40/40); running it as if it
were v2's would report a green pass over the wrong plan.

---

## STEP 0 — Plan Load, Gate Check, API-Doc Regeneration + Assessment

### 0.1 — Load the delivered v2 test plan (the REQUIRED COVERAGE)
Read every `TC-FIN-<seq>` block out of the Shape B file named in the precondition
above — both `<!-- SUB:RULE-SCENARIOS -->` and `<!-- SUB:API-SCENARIOS -->`, 90 TCs
in total. (If it has gone missing, re-check Shape A before concluding anything — the
delivery shape can change between factory publishes.) Extract per TC: its `TC-FIN-<seq>` id,
the `AC-*` / `XM-*` / `UXD-*` it traces (from its `traces=` marker attribute /
`Derived from` line), and its one-line scenario. This list is the **REQUIRED
COVERAGE** for the run — what the system's own analysis says must be tested,
independent of whatever `api-verify` later discovers from the api-docs.

If both shapes are genuinely empty, STOP and say which two paths you checked.

### 0.2 — Gate Check (MANDATORY)
Read `governance/shared/backend/modules/FIN/v2/execution-state.json` → for each
entry in `test_phases[]`, its `gated_by_phases[]`. Confirm every listed backend
execution phase has `status == COMPLETE`. An empty `gated_by_phases[]` passes
automatically. An empty `test_phases[]` would mean there is nothing to gate — no
longer the case for v2; if you find it empty, the state file has been reset, so
re-run `/generate-module-setup FIN` before going further.

The v2 execution phases, in profile order, are:
`CORE · DATA-DOM · SVC-API · DOC · INT-C · INT-R · SEC-BE · ALIGN-BE`.
All eight exist on disk for v2, so a regenerated `gated_by_phases[]` will name all eight.

If not all complete:
```
══════════════════════════════════════════════════════
⛔ TEST GATE FAILED — FIN v2
══════════════════════════════════════════════════════
Waiting on : [PHASE: status], ...
══════════════════════════════════════════════════════
```
STOP. Do not regenerate api-docs and do not invoke `api-verify`.

### 0.3 — Regenerate api-docs (MANDATORY, every run, BEFORE api-verify)
`api-verify` treats stale api-docs as a hard blocker. Regenerate FIN's api-docs from
the real, current implementation first:
```bash
cd governance/governance-tools/api-doc-generator
python3 generate.py --module FIN --function generate
```
(consult that tool's own `README.md` for `--function generate` vs `update` vs
`review` semantics before assuming — use whichever actually (re)writes the api-docs
in full for this run). Confirm
`governance/shared/backend/modules/FIN/api-docs/index.md` was written/updated
before proceeding to STEP 0.4.

**api-docs are NOT version-suffixed.** They are derived from the running
application, so there is one current set per module, not one per plan version —
`governance/shared/backend/modules/FIN/api-docs/`, never `…/FIN/v2/api-docs/`.
Once v2 endpoints are implemented, this one set describes them.

**Then publish them, or they reach nobody.** That directory is inside the
`governance/shared` submodule — a separate repository:

```bash
cd governance/shared
git fetch origin main
git merge-base --is-ancestor HEAD origin/main \
  && git checkout main \
  || echo "HEAD is NOT on origin/main — do not checkout; commit here and push HEAD:main"
git add -A && git commit -m "api-docs(FIN): regenerated" && git push
cd ../.. && git add governance/shared && git commit -m "bump shared" && git push
```

A submodule is checked out on a *commit*, not a branch, so a plain `git push` has no
branch to push to and the commit never leaves this machine. `git checkout main` is
only harmless while the checked-out commit is an ancestor of `origin/main` — hence
the guard. Full sequence and both failure modes: `/generate-api-docs`.

### 0.4 — Confirm the app is reachable
`http://localhost:7272/actuator/health` (start it with `mvn spring-boot:run` if it
isn't running; `source .env` first, and `JAVA_HOME` must point at a JDK 25). Unreachable →
classify `ENVIRONMENT_FAILURE`, stop, report — do not proceed. (If the
api-doc-generator run in 0.3 itself needs the live app, confirm reachability before
0.3 instead; check the tool's own discovery method rather than assuming.)

### 0.5 — Print the same assessment and wait for confirmation as `/FIN/v2/execute-backend`

---

## STEP 1 — Execution (after confirmation)

Invoke the `api-verify` skill (`.claude/skills/api-verify/SKILL.md`) for
`<MOD>` = `FIN`. Per the skill's own procedure it reads:
- `governance/shared/backend/modules/FIN/api-docs/` — regenerated in STEP 0.3, mandatory;
- `governance/shared/analysis/modules/FIN/v2/test_gen/test-execution-manifest-fin.md`
  when present (Full tier: happy-path CRUD + negative RULE checks, dependency order
  read verbatim from the manifest) — otherwise Minimal tier (happy-path CRUD only, FK
  order inferred, negatives stated as skipped and why). **As of setup this manifest
  does not exist for v2**, so a run today would be Minimal tier; say so explicitly
  rather than letting a Minimal run read as full coverage;
- `governance/shared/platform/rules/api-verify-config.md` for every stack convention
  (base path, envelope shapes, error-code format, permission pattern) — never
  re-derived here.

It produces, under `governance/shared/backend/modules/FIN/v2/test-api/`:
- `test_fin_apis.py` — one runnable script, one `test_<entity>()` per entity in
  dependency order, each create/update/negative call tagged with a traceability
  comment (`Covers: API-… ; Negative: RULE-… / <code> / TC-…`), self-tearing-down;
- `fin_problems_report.md` — failures bucketed likely-real-bug /
  test-assumption-mismatch / infrastructure.

Run the generated script
(`python3 governance/shared/backend/modules/FIN/v2/test-api/test_fin_apis.py`)
against the app confirmed reachable in STEP 0.4, and record its pass/fail per
`test_<entity>()` suite. This command never hand-writes verification code itself and
never calls a TestSprite tool.

---

## STEP 1.9 — Coverage cross-check (governed plan ↔ api-verify) — MANDATORY

`api-verify`'s Full-tier negatives are already tagged with the SAME `TC-FIN-<seq>` id
space the test-gen plan uses. Map every REQUIRED-COVERAGE `TC-FIN-<seq>` from
STEP 0.1 to the `test_<entity>()` function(s) in `test_fin_apis.py` whose
traceability comment names it, and to that function's actual pass/fail result from
STEP 1. A happy-path `TC-*` with no corresponding `Covers:` entry, or a negative
`TC-*` with no corresponding `Negative:` entry, is a gap — the same is true when the
tier is Minimal and the manifest that would have produced a negative test simply
doesn't exist yet. State that explicitly; don't silently treat it as covered.

```
GOVERNED PLAN ↔ API-VERIFY COVERAGE — FIN v2  (tier: Full | Minimal)
TC-FIN-<seq>  │ traces (AC/XM/UXD) │ scenario        │ test_<entity>() ref     │ result
──────────────┼────────────────────┼─────────────────┼─────────────────────────┼────────
TC-FIN-001    │ AC-…               │ …               │ test_account (Covers)   │ PASS
TC-FIN-0NN    │ XM-… / UXD-…       │ …               │ ✗ none                  │ GAP
```

- A delivered `TC-*` with no matching `test_<entity>()` reference is a **coverage
  gap** — list it prominently; it is never dropped silently.
- Integration `TC-*` — those tracing `XM-*` or `UXD-*`, from the profile's
  `integration`-flagged test phase (`INT-XM`) — are checked exactly like any other.
- Record the coverage ratio: `<covered>/<total>` REQUIRED-COVERAGE TCs.

---

## STEP 2 — Classify and report

Classify every failure/skip using this taxonomy:

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

Every failed/skipped test gets exactly one code. Never invent a new one — if nothing
fits, use `ENVIRONMENT_FAILURE` and explain why in the detail.

Write `reports/TEST-REPORT-FIN-v2-backend-[YYYY-MM-DD].md` — a module-scoped digest,
distinct from `api-verify`'s own raw output (`fin_problems_report.md`, left under
`governance/shared/backend/modules/FIN/v2/test-api/`, untouched). It MUST include the
STEP 1.9 coverage table and the coverage ratio, ABOVE the failure taxonomy — a green
taxonomy over an incomplete plan is not a pass.

Any `FAIL` or coverage GAP → report it here with its taxonomy code and STOP; this
command never fixes source itself. Fixing is a separate, deliberate step the user
runs afterward — do not auto-invoke any fixing agent from here.

### 2.1 — Update `execution-state.json` `test_phases[]` (MANDATORY)
In `governance/shared/backend/modules/FIN/v2/execution-state.json`, for each entry in
`test_phases[]`, set its status from the STEP 1.9 result:
- `COMPLETE` only when EVERY `TC-*` under that phase (STEP 0.1) has a passing
  `api-verify` counterpart (STEP 1.9).
- `PARTIAL` when some pass but at least one `TC-*` is a gap or a fail — attach the
  gap/fail `TC-*` list to the entry.
- `PENDING` if the phase never ran.

Scope the edit to `test_phases[]` (and, if a real doc gap surfaced, one
`api_doc_gaps[]` append in the canonical shape) — touch nothing else. Do not
overwrite the execution `phases[]` array or any key execution added.

---

## Constraints (NON-NEGOTIABLE)

- NEVER fall back to v1's test plan or v1's `execution-state.json` — v2 has its own
- NEVER run before the gate check (0.2) passes
- NEVER invoke `api-verify` (STEP 1) before this run's own api-doc-generator
  regeneration (STEP 0.3) has completed and been confirmed written
- NEVER call a TestSprite tool of any kind — `api-verify` is the sole adopted mechanism
- NEVER modify application source code — report, don't fix
- NEVER hand-edit a generated `test-api` script — rerun STEP 0.3 → STEP 1 instead
- ALWAYS classify every failure/skip
- ALWAYS load the governed `TC-*` plan (STEP 0.1) and emit the STEP 1.9 coverage
  table before considering any test phase complete
- ALWAYS update `test_phases[]` status per STEP 2.1
