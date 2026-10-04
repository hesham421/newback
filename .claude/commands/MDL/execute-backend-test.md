# /MDL/execute-backend-test

Execute API verification for MDL — only for what's actually complete.

> **Self-contained.** This command needs `governance/governance-tools/api-doc-generator`,
> the `api-verify` skill (`.claude/skills/api-verify/SKILL.md`), `governance/shared/platform/rules/api-verify-config.md`,
> and this module's own artifacts under `governance/shared/analysis/modules/MDL/`. Every rule it relies
> on is written below or in those two files — it reads no other external mechanism/governance
> doc, never stops waiting on one, and never calls TestSprite (retired as this project's
> backend test mechanism).

## Usage
/MDL/execute-backend-test

---

## STEP 0 — Plan Load, Gate Check, API-Doc Regeneration + Assessment

### 0.1 — Load the delivered test-gen plan (the REQUIRED COVERAGE)
Read every `TC-MDL-<seq>` block out of `governance/shared/analysis/modules/MDL/test_gen/backend-test-plan-mdl.md`
(this is the module's flat test-gen plan file; there is no `backend-test/` fallback copy on
disk for MDL — do not assume one). This command does not read `packages/backend-test/` —
that split-folder shape depended on governance-tools splitter tooling this project no
longer relies on; the flat file is the sole source of truth.

The file contains two `<!-- PHASE:*:START -->` blocks:
- `TEST-PLAN-BE` — the base module-scope phase, with two nested `<!-- SUB:*:START -->`
  blocks: `RULE-SCENARIOS` (TC-MDL-002, 003, 004, 007, 011, 012) and `API-SCENARIOS`
  (TC-MDL-001, 005, 006, 008, 009, 010, 013).
- `INT-XM` — the cross-module integration phase (MDL declares XM-MDL-001 → SEC), holding
  one TC directly in the phase with no nested SUB (TC-MDL-014, an INTEGRATION/EDGE
  scenario: graceful degradation when SEC is unreachable during owner-module validation).

Per TC extract: its `TC-MDL-<seq>` id, the `AC-*`/`REQ-*`/`XM-*` it traces (from its
`traces=` marker attribute / `Derived from` line), and its one-line scenario. This list —
13 module-scope TCs (TC-MDL-001..013) + 1 integration TC (TC-MDL-014) — is the
**REQUIRED COVERAGE** for this run.

### 0.2 — Gate Check (MANDATORY)
Read `governance/shared/backend/modules/MDL/execution-state.json` → for each entry in `test_phases[]`
(`TEST-PLAN-BE` and `INT-XM`), confirm every phase listed in its `gated_by_phases[]`
(`CORE, DATA-DOM, SVC-API, DOC, INT-C, INT-R, SEC-BE, ALIGN-BE` — all 8, for both entries)
has `status == COMPLETE`.

If not all complete:
```
══════════════════════════════════════════════════════
⛔ TEST GATE FAILED — MDL
══════════════════════════════════════════════════════
Waiting on : [PHASE: status], ...
══════════════════════════════════════════════════════
```
STOP. Do not regenerate api-docs and do not invoke `api-verify`.

### 0.3 — Regenerate api-docs (MANDATORY, every run, BEFORE api-verify)
`api-verify` treats stale api-docs as a hard blocker — it must never read a
possibly-outdated copy. Regenerate MDL's api-docs from the real, current
implementation first:
```bash
cd governance/governance-tools/api-doc-generator
python3 generate.py --module MDL --function generate
```
(consult that tool's own `README.md` for `--function generate` vs `update` vs
`review` semantics before assuming — use whichever actually (re)writes
the `api_docs_path` this module's `execution-state.json` declares in full for this run). Confirm
`governance/shared/backend/modules/MDL/api-docs/index.md` was written/updated before
proceeding to STEP 0.4 — do not invoke `api-verify` against missing or
unrefreshed api-docs.

### 0.4 — Confirm the app is reachable
`http://localhost:7272/actuator/health` (start it with `mvn spring-boot:run`
if it isn't running). Unreachable →
classify `ENVIRONMENT_FAILURE`, stop, report — do not proceed. (If MDL's
api-doc-generator run in 0.3 itself needs the live app — e.g. to read
a running OpenAPI endpoint rather than a static build artifact — confirm
reachability before 0.3 instead; check the tool's own discovery method rather
than assuming.)

### 0.5 — Same assessment/confirmation pattern as execute-backend.md

---

## STEP 1 — Execution (after confirmation)

Invoke the `api-verify` skill (`.claude/skills/api-verify/SKILL.md`) for
`<MOD>` = `MDL`. Per the skill's own procedure it reads:
- the `api_docs_path` this module's `execution-state.json` declares — regenerated in STEP 0.3, mandatory;
- `governance/shared/analysis/modules/MDL/test_gen/test-execution-manifest-mdl.md` when present
  (Full tier: happy-path CRUD + negative RULE checks, dependency order read
  verbatim from the manifest — note the module's two-entity FK order,
  LookupType before LookupValue) — otherwise Minimal tier (happy-path CRUD
  only, FK order inferred, negatives stated as skipped and why);
- `governance/shared/platform/rules/api-verify-config.md` for every stack convention (base path
  `/api/v1/mdl`, envelope shapes, error-code format `MDL-{http}[-{SLUG}]`,
  permission pattern) — never re-derived here.

It produces, under `governance/shared/backend/modules/MDL/test-api/`:
- `test_mdl_apis.py` — one runnable script, one `test_<entity>()` per entity
  in dependency order (LookupType, then LookupValue), each create/update/negative
  call tagged with a traceability comment (`Covers: API-… ; Negative: RULE-… /
  <code> / TC-…`), self-tearing-down;
- `mdl_problems_report.md` — failures bucketed likely-real-bug /
  test-assumption-mismatch / infrastructure.

Run the generated script (`python3 governance/shared/backend/modules/MDL/test-api/test_mdl_apis.py`)
against the app confirmed reachable in STEP 0.4, and record its pass/fail per
`test_<entity>()` suite. This command never hand-writes verification code
itself and never calls a TestSprite tool.

---

## STEP 1.9 — Coverage cross-check (governed plan ↔ api-verify) — MANDATORY

Map every REQUIRED-COVERAGE `TC-MDL-<seq>` from STEP 0.1 to the `test_<entity>()`
function(s) in `test_mdl_apis.py` whose traceability comment names it, and to
that function's actual pass/fail result from STEP 1. A happy-path `TC-*` with
no corresponding `Covers:` entry, or a negative `TC-*` with no corresponding
`Negative:` entry, is a gap — the same is true when the tier is Minimal and
the manifest that would have produced a negative test simply doesn't exist
yet (state that explicitly, don't silently treat it as covered).

TC-MDL-014 (the `INT-XM` phase's SEC-unreachable degradation scenario) is
checked here exactly like any other — a cross-module dependency with no
exercising test is a gap, same as an uncovered `AC-*`.

Produce this table for the report:

```
GOVERNED PLAN ↔ API-VERIFY COVERAGE — MDL  (tier: Full | Minimal)
TC-MDL-<seq>  │ traces (AC/REQ/XM)  │ scenario              │ test_<entity>() ref     │ result
──────────────┼──────────────────────┼───────────────────────┼─────────────────────────┼────────
TC-MDL-001    │ AC-MDL-001           │ search lookup types   │ test_lookuptype (Covers)│ PASS
TC-MDL-014    │ XM-MDL-001           │ SEC unreachable → …   │ ✗ none                  │ GAP
```

- A delivered `TC-*` with no matching `test_<entity>()` reference is a
  **coverage gap** — list it prominently; it is never dropped silently.
- Record the coverage ratio: `<covered>/14` REQUIRED-COVERAGE TCs (13 module-
  scope + 1 integration).

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

Write `reports/TEST-REPORT-MDL-backend-[YYYY-MM-DD].md` — a module-scoped
digest, distinct from `api-verify`'s own raw output (`mdl_problems_report.md`,
left under `governance/shared/backend/modules/MDL/test-api/`, untouched). It MUST include
the STEP 1.9 coverage table (governed plan ↔ api-verify) and the coverage
ratio, ABOVE the failure taxonomy — a green taxonomy over an incomplete plan
is not a pass. This report is complete once the test/coverage section above
is written.

Any `FAIL` or coverage GAP → report it here with its taxonomy code and STOP;
this command never fixes source itself. Fixing is a separate, deliberate step
the user runs afterward — do not auto-invoke any fixing agent from here.

### 2.1 — Update `execution-state.json` `test_phases[]` (MANDATORY)
For each entry in `test_phases[]` (`TEST-PLAN-BE`, `INT-XM`), set its status
from the STEP 1.9 result:
- `COMPLETE` only when EVERY `TC-*` under that phase (STEP 0.1) has a passing
  `api-verify` counterpart (STEP 1.9). Note `TEST-PLAN-BE` and `INT-XM` are
  graded independently — `INT-XM` covers only TC-MDL-014.
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
- ALWAYS update `execution-state.json` `test_phases[]` status per STEP 2.1
