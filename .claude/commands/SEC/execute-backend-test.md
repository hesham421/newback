# /SEC/execute-backend-test

Execute TestSprite-based test scenarios for SEC — only for what's
actually complete.

> **Self-contained.** This command needs only the `TestSprite` MCP server
> (wired in `.mcp.json`) and this module's own artifacts under
> `governance/shared/analysis/modules/SEC/`. Every rule it relies on is written below —
> it reads no external mechanism/governance doc, and never stops waiting on one.

## Usage
/SEC/execute-backend-test

---

## STEP 0 — Plan Load, Gate Check + Assessment

### 0.1 — Load the delivered test-gen plan (the REQUIRED COVERAGE)
Before any TestSprite call, read every `TC-SEC-<seq>` block out of this
module's flat test-gen plan file:
`governance/shared/analysis/modules/SEC/test_gen/backend-test-plan-sec.md` (current
location; fall back to `governance/shared/analysis/modules/SEC/test_gen/backend-test-plan-sec.md`
if that path doesn't exist). This command does not read `packages/backend-test/`
— that split-folder shape depended on governance-tools splitter tooling this
project no longer relies on; the flat file is the sole source of truth.

The file wraps everything in one `<!-- PHASE:TEST-PLAN-BE:START -->` block,
containing two nested `<!-- SUB:*:START -->` blocks:
- `RULE-SCENARIOS` — 7 TCs (TC-SEC-008, 013, 014, 015, 018, 020, 030):
  RULE-driven violations/cascades.
- `API-SCENARIOS` — 26 TCs (TC-SEC-001..007, 009..012, 016, 017, 019,
  021..029, 031..033): endpoint-driven happy/state/permission paths.

Total REQUIRED COVERAGE = 33 TCs (matches the file's own header count). The
file states explicitly there is no `INT-XM` phase — SEC is ROOT, declares no
`XM-*` — so no integration phase applies here.

For each TC, extract: its `TC-SEC-<seq>` id, the `AC-*`/`REQ-*`/`API-*` it
traces (from its `traces=` marker attribute / `Derived from` line), and its
one-line scenario. This list is the **REQUIRED COVERAGE** for this run — it
is what the system's own analysis says must be tested, independent of
whatever TestSprite later discovers from the code surface. If neither
location yields a `backend-test-plan-*.md` file, or it holds no `TC-*` block,
STOP and report it — there is nothing governed to verify.

### 0.2 — Gate Check (MANDATORY)
Read `execution-state.json` → for each entry in `test_phases[]`, its
`gated_by_phases[]`. Confirm every listed backend execution phase has
`status == COMPLETE`. Empty list → that phase's gate passes automatically.

`execution-state.json`'s `test_phases[]` has one entry, `TEST-PLAN-BE`, gated
by all 8 backend execution phases (CORE, DATA-DOM, SVC-API, DOC, INT-C,
INT-R, SEC-BE, ALIGN-BE). None of them is COMPLETE yet — this run will fail
the gate until implementation (`/SEC/execute-backend`) finishes all of them.

If not all complete:
```
══════════════════════════════════════════════════════
⛔ TEST GATE FAILED — SEC
══════════════════════════════════════════════════════
Waiting on : [PHASE: status], ...
══════════════════════════════════════════════════════
```
STOP. Do not call any TestSprite tool.

### 0.3 — Confirm the app is reachable
`http://localhost:7272/actuator/health` (start it with `mvn spring-boot:run`
if it isn't running). Unreachable →
classify `ENVIRONMENT_FAILURE`, stop, report — do not proceed.

### 0.4 — Same assessment/confirmation pattern as execute-backend.md

---

## STEP 1 — Execution (after confirmation)

Pick the branch by whether this module already has archived tests:

### Branch A — RERUN
This module already has `.py` files under
`governance/shared/backend/modules/SEC/testsprite/tests/` and the API surface hasn't
changed since. No TestSprite MCP tool call at all: run each archived file
directly (`python3 <path>`, never pytest — each file already calls its own
`test_*()` at the bottom) and record pass/fail per file.

### Branch B — NEW
No archived tests exist yet for this module, or the API surface changed
since the last archive. Run the TestSprite pipeline via the wired `TestSprite`
MCP server, calling its tools as the live server actually exposes them (verify
current tool names/params against the connected server before calling — do not
assume the names below never drift across a TestSprite MCP version bump):

1. **Housekeeping** — if any leftover, unarchived run is sitting in the repo-root
   `testsprite_tests/` working directory, archive it (Branch-B close-out below)
   before starting a new one; never let two runs' output mix.
2. `testsprite_bootstrap` — ONLY if `testsprite_tests/tmp/config.json`
   does not already exist (`type: backend`, `testScope: codebase`,
   `localPort: 7272`, `projectPath: <repo root>`).
3. `testsprite_generate_code_summary`
4. `testsprite_generate_standardized_prd`
5. `testsprite_generate_backend_test_plan` — (re)writes
   `testsprite_tests/testsprite_backend_test_plan.json`, spanning the
   WHOLE backend, not just this module.
6. **Module scoping (self-contained).** From that plan, select only the `TCnnn`
   entries whose endpoint path matches THIS module's own API path prefix:
   `/api/v1/sec` (per `packages/backend-execution/DOC/DOC.md`'s API contract
   summary table — "paths relative to /api/v1/sec" — and confirmed against
   `packages/backend-execution/SVC-API/*.md`; re-derive from those files
   rather than trusting this note if the API surface has since changed).
   Collect the matching ids; this is the module scoping step.
7. `testsprite_generate_code_and_execute` with `testIds` = exactly that
   filtered id list (never the full-plan default, which would drag every
   other module's scenarios into this module's run) — `projectName` /
   `projectPath` as usual, `serverMode` matching how the app was actually
   started (`production` only if it was built+started that way).
8. **Close out (self-contained archive).** `git mv` this module's `TCnnn_*.py`
   files into `governance/shared/backend/modules/SEC/testsprite/tests/`, and the run's
   PRD/plan/report trio into `governance/testsprite/runs/<YYYY-MM-DD>-backend/`
   (create the folders if absent — everything for a module lives under its own
   `governance/shared/backend/modules/SEC/testsprite/`). Leave the repo-root
   `testsprite_tests/` working directory clean afterward.

---

## STEP 1.9 — Coverage cross-check (governed plan ↔ TestSprite) — MANDATORY

This is the connective tissue between the delivered test-gen plan (STEP 0.1)
and TestSprite's own output. Without it the two id spaces (`TC-SEC-<seq>`
vs TestSprite's `TCnnn`) stay permanently disconnected and TestSprite's
code-surface discovery silently becomes the only coverage that counts.

Map every REQUIRED-COVERAGE `TC-SEC-<seq>` from STEP 0.1 to the TestSprite
`TCnnn` file(s) that actually exercise it — matched by endpoint + scenario, not
by number (the two numbering schemes are unrelated). Produce this table for the
report:

```
GOVERNED PLAN ↔ TESTSPRITE COVERAGE — SEC
TC-SEC-<seq>       │ traces (AC/XM/UXD) │ scenario        │ TestSprite TCnnn │ result
───────────────────┼────────────────────┼─────────────────┼──────────────────┼────────
TC-SEC-001         │ AC-…               │ …               │ TC003            │ PASS
TC-SEC-0NN         │ XM-… / UXD-…       │ …               │ ✗ none           │ GAP
```

- A delivered `TC-*` with NO matching TestSprite test is a **coverage gap** —
  list it prominently; it is never dropped silently.
- Integration `TC-*` (those tracing `XM-*` or `UXD-*`) are checked here
  exactly like any other — SEC has no outbound `XM-*` rows of its own
  (it is ROOT), so any integration coverage here would trace the inbound-
  consumer stub noted in `packages/backend-execution/INT-R/INT-R.md`.
- Record the coverage ratio: `<covered>/<total>` REQUIRED-COVERAGE TCs.

Build this table from the two subs loaded in STEP 0.1 — `RULE-SCENARIOS`
(TC-SEC-008, 013, 014, 015, 018, 020, 030) and `API-SCENARIOS` (TC-SEC-001..007,
009..012, 016, 017, 019, 021..029, 031..033) — 33 rows total, none of which
has a TestSprite counterpart yet (no run has happened for SEC).

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

Write `reports/TEST-REPORT-SEC-backend-[YYYY-MM-DD].md` — a
module-scoped digest, distinct from TestSprite's own raw report (which is
archived under `governance/testsprite/runs/<YYYY-MM-DD>-backend/`,
untouched). It MUST include the STEP 1.9 coverage table (governed plan ↔
TestSprite) and the coverage ratio, ABOVE the failure taxonomy — a green
taxonomy over an incomplete plan is not a pass. This report is complete once
the test/coverage section above is written.

Any `FAIL` or coverage GAP → report it here with its taxonomy code and STOP;
this command never fixes source itself. Fixing is a separate, deliberate step
the user runs afterward — do not auto-invoke any fixing agent from here.

### 2.1 — Update `execution-state.json` `test_phases[]` (MANDATORY)
For each entry in `test_phases[]`, set its status from the STEP 1.9 result:
- `COMPLETE` only when EVERY `TC-*` under that phase (STEP 0.1) has a passing
  TestSprite counterpart (STEP 1.9).
- `PARTIAL` when some pass but at least one `TC-*` is a gap or a fail — attach
  the gap/fail `TC-*` list to the entry.
- `PENDING` if the phase never ran.
Scope the edit to `test_phases[]` (and, if a real doc gap surfaced, one
`api_doc_gaps[]` append in the canonical shape) — touch nothing else. The
TestSprite run MUST leave `test_phases[]` reflecting exactly what it verified.

`test_phases[]` has one entry, `TEST-PLAN-BE` — update its `status` and its
two subs' (`RULE-SCENARIOS`, `API-SCENARIOS`) statuses per the rule above once
this command actually runs.

---

## Constraints (NON-NEGOTIABLE)

- NEVER run before the gate check passes
- NEVER call a TestSprite MCP tool in Branch A (RERUN) — direct `python3`
  execution of the already-archived files only
- NEVER call the bootstrap tool when `testsprite_tests/tmp/config.json`
  already exists
- NEVER skip STEP 1's housekeeping/archiving steps
- NEVER modify application source code — report, don't fix
- NEVER hand-edit an archived `.py` test file, EXCEPT the one sanctioned case:
  when backend code an archived test already covers changed (endpoint path,
  request/response fields, status/error codes, auth), update that test's
  payload/assertions to match rather than leave it silently broken
- ALWAYS classify every failure/skip
- ALWAYS load the governed `TC-*` plan (STEP 0.1) and emit the STEP 1.9
  coverage table before considering any test phase complete
- ALWAYS update `execution-state.json` `test_phases[]` status per STEP 2.1
