# /FIN/execute-backend-test

Execute API verification for FIN — only for what's actually complete.

> **Self-contained.** This command needs `governance/governance-tools/api-doc-generator`,
> the `api-verify` skill (`.claude/skills/api-verify/SKILL.md`), `governance/shared/platform/rules/api-verify-config.md`,
> and this module's own artifacts under `governance/shared/analysis/modules/FIN/`. Every rule it relies
> on is written below or in those two files — it reads no other external mechanism/governance
> doc, never stops waiting on one, and never calls TestSprite (retired as this project's
> backend test mechanism).

## Usage
/FIN/execute-backend-test

---

## STEP 0 — Plan Load, Gate Check, API-Doc Regeneration + Assessment

### 0.1 — Load the delivered test-gen plan (the REQUIRED COVERAGE)
Read every `TC-FIN-<seq>` block out of `governance/shared/analysis/modules/FIN/test_gen/backend-test-plan-fin.md`
(this is the module's flat test-gen plan file; there is no `backend-test/` fallback copy on
disk for FIN — do not assume one). This command does not read `packages/backend-test/` —
that split-folder shape depended on governance-tools splitter tooling this project no
longer relies on; the flat file is the sole source of truth.

The file contains two `<!-- PHASE:*:START -->` blocks:
- `TEST-PLAN-BE` — the base module-scope phase, with two nested `<!-- SUB:*:START -->`
  blocks: `RULE-SCENARIOS` (TC-FIN-002, 006, 009, 011, 012, 013, 017, 018, 019, 020,
  021, 028, 029, 030, 035, 038, 048, 049, 050, 051, 052, 053, 054, 055, 056, 057, 058,
  059, 060, 061, 062, 092, 093, 103 — 34 TCs) and `API-SCENARIOS` (TC-FIN-001, 003, 004, 005,
  007, 008, 010, 014, 015, 016, 022, 023, 024, 025, 026, 027, 031, 032, 033, 034, 036,
  037, 039, 040, 041, 042, 043, 044, 045, 046, 063–090, 094–102, 104–109 — 73 TCs).
- `INT-XM` — the cross-module integration phase. FIN declares exactly ONE XM today:
  XM-FIN-001 → MDL. The phase holds TWO TC ids directly, with no nested SUB: TC-FIN-047
  (INTEGRATION/EDGE — MDL's lookup-type failure translated into FIN-400-INVALID-LOOKUP),
  and TC-FIN-091, which is **RETIRED as of 2026-09-12** and asserts nothing — XM-FIN-002
  → SEC no longer exists, so the phase's live coverage is TC-FIN-047 alone. The id is kept
  rather than deleted so nothing renumbers.

Do not trust these id lists over the file: they are a convenience, and the plan has grown
more than once. Enumerate the `TC:TC-FIN-*:START` markers actually present and use that.

Per TC extract: its `TC-FIN-<seq>` id, the `AC-*`/`REQ-*`/`XM-*` it traces (from its
`traces=` marker attribute / `Derived from` line), and its one-line scenario. This list —
107 module-scope TCs + 2 integration TC ids (TC-FIN-047 and TC-FIN-091), 109 in total,
TC-FIN-001..109 with no gaps — is the **REQUIRED COVERAGE** for this run, MINUS any TC
whose body is marked RETIRED. As of 2026-09-12 exactly one is: TC-FIN-091. That leaves
**108 TCs in force**. Detect retirement from the TC body itself (a `Status : RETIRED`
line / a `RETIRED` title), never from this list.

### 0.2 — Gate Check (MANDATORY)
Read `governance/shared/backend/modules/FIN/execution-state.json` → for each entry in `test_phases[]`
(`TEST-PLAN-BE` and `INT-XM`), confirm every phase listed in its `gated_by_phases[]`
(`CORE, DATA-DOM, SVC-API, DOC, INT-C, INT-R, SEC-BE, ALIGN-BE` — all 8, for both entries)
has `status == COMPLETE`.

If not all complete:
```
══════════════════════════════════════════════════════
⛔ TEST GATE FAILED — FIN
══════════════════════════════════════════════════════
Waiting on : [PHASE: status], ...
══════════════════════════════════════════════════════
```
STOP. Do not regenerate api-docs and do not invoke `api-verify`.

### 0.3 — Regenerate api-docs (MANDATORY, every run, BEFORE api-verify)
`api-verify` treats stale api-docs as a hard blocker — it must never read a
possibly-outdated copy. Regenerate FIN's api-docs from the real, current
implementation first:
```bash
cd governance/governance-tools/api-doc-generator
python3 generate.py --module FIN --function generate
```
(consult that tool's own `README.md` for `--function generate` vs `update` vs
`review` semantics before assuming — use whichever actually (re)writes
the `api_docs_path` this module's `execution-state.json` declares in full for this run). Confirm
`governance/shared/backend/modules/FIN/api-docs/index.md` was written/updated before
proceeding to STEP 0.4 — do not invoke `api-verify` against missing or
unrefreshed api-docs.

### 0.4 — Confirm the app is reachable
`http://localhost:7272/actuator/health` (start it with `mvn spring-boot:run`
if it isn't running). Unreachable →
classify `ENVIRONMENT_FAILURE`, stop, report — do not proceed. (If FIN's
api-doc-generator run in 0.3 itself needs the live app — e.g. to read
a running OpenAPI endpoint rather than a static build artifact — confirm
reachability before 0.3 instead; check the tool's own discovery method rather
than assuming.)

### 0.5 — Same assessment/confirmation pattern as execute-backend.md

---

## STEP 1 — Execution (after confirmation)

Invoke the `api-verify` skill (`.claude/skills/api-verify/SKILL.md`) for
`<MOD>` = `FIN`. Per the skill's own procedure it reads:
- the `api_docs_path` this module's `execution-state.json` declares — regenerated in STEP 0.3, mandatory;
- `governance/shared/analysis/modules/FIN/test_gen/test-execution-manifest-fin.md` when present
  (Full tier: happy-path CRUD + negative RULE checks, dependency order read
  verbatim from the manifest — note this module's dependency chain: lookup/config
  entities (Dimension, EventTypeRule, RuleLine, RecurringTemplate, AllocationRule)
  and master entities (Account, FiscalYear, FiscalPeriod) before any JournalEntry
  posting scenario, and every lookup-backed field validated against MDL per
  XM-FIN-001 first) — otherwise Minimal tier (happy-path CRUD only, FK order
  inferred, negatives stated as skipped and why);
- `governance/shared/platform/rules/api-verify-config.md` for every stack convention (base path
  `/api/v1/fin`, envelope shapes, error-code format `FIN-{http}[-{SLUG}]`,
  permission pattern) — never re-derived here.

It produces, under `governance/shared/backend/modules/FIN/test-api/`:
- `test_fin_apis.py` — one runnable script, one `test_<entity>()` per entity
  in dependency order, each create/update/negative call tagged with a
  traceability comment (`Covers: API-… ; Negative: RULE-… / <code> / TC-…`),
  self-tearing-down;
- `fin_problems_report.md` — failures bucketed likely-real-bug /
  test-assumption-mismatch / infrastructure.

Run the generated script (`python3 governance/shared/backend/modules/FIN/test-api/test_fin_apis.py`)
against the app confirmed reachable in STEP 0.4, and record its pass/fail per
`test_<entity>()` suite. This command never hand-writes verification code
itself and never calls a TestSprite tool.

---

## STEP 1.9 — Coverage cross-check (governed plan ↔ api-verify) — MANDATORY

Map every REQUIRED-COVERAGE `TC-FIN-<seq>` from STEP 0.1 to the `test_<entity>()`
function(s) in `test_fin_apis.py` whose traceability comment names it, and to
that function's actual pass/fail result from STEP 1. A happy-path `TC-*` with
no corresponding `Covers:` entry, or a negative `TC-*` with no corresponding
`Negative:` entry, is a gap — the same is true when the tier is Minimal and
the manifest that would have produced a negative test simply doesn't exist
yet (state that explicitly, don't silently treat it as covered).

TC-FIN-047 (the `INT-XM` phase's cross-module degradation scenario, against MDL)
is checked here exactly like any other — a cross-module dependency with no
exercising test is a gap, same as an uncovered `AC-*`. TC-FIN-091 is RETIRED and is
NOT a gap: the XM it exercised (XM-FIN-002 → SEC) no longer exists, so there is
nothing to call. Record it as `RETIRED`, not `GAP`, and say why.

Produce this table for the report:

```
GOVERNED PLAN ↔ API-VERIFY COVERAGE — FIN  (tier: Full | Minimal)
TC-FIN-<seq>  │ traces (AC/REQ/XM)  │ scenario              │ test_<entity>() ref     │ result
──────────────┼──────────────────────┼───────────────────────┼─────────────────────────┼────────
TC-FIN-001    │ AC-FIN-001           │ create account        │ test_account (Covers)   │ PASS
TC-FIN-047    │ XM-FIN-001           │ MDL unreachable → …   │ ✗ none                  │ GAP
```

- A delivered `TC-*` with no matching `test_<entity>()` reference is a
  **coverage gap** — list it prominently; it is never dropped silently.
- Record the coverage ratio: `<covered>/108` TCs IN FORCE (109 ids, less the one
  RETIRED id TC-FIN-091). Report retired ids separately from the ratio; they are
  neither covered nor a gap. If the enumeration in STEP 0.1 finds a different total
  or a different set of retired ids, those totals are the denominator — the plan is
  the source of truth, not this line.

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

Write `reports/TEST-REPORT-FIN-backend-[YYYY-MM-DD].md` — a module-scoped
digest, distinct from `api-verify`'s own raw output (`fin_problems_report.md`,
left under `governance/shared/backend/modules/FIN/test-api/`, untouched). It MUST include
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
  graded independently — `INT-XM` covers only TC-FIN-047 and TC-FIN-091.
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
