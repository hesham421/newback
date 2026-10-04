# /CU/execute-backend-test

Execute test scenarios for CU — only for what's actually complete.

> Read `TEST-EXECUTION-AGENT.md` first.

## Usage
/CU/execute-backend-test

---

## STEP 0 — Gate Check + Assessment

### 0.1 — Gate Check (MANDATORY)
Read `execution-state.json` → `test_phase.gated_by_phases[]`. Confirm
every listed phase has `status == COMPLETE`. Empty list → gate passes
automatically.

Gated by: CORE, DATA-DOM, SVC-API, DOC, INT-C, INT-R, SEC-BE, ALIGN-BE.

If not all complete:
```
══════════════════════════════════════════════════════
⛔ TEST GATE FAILED — CU
══════════════════════════════════════════════════════
Waiting on : [PHASE: status], ...
══════════════════════════════════════════════════════
```
STOP. Do not generate or run any test.

### 0.2–0.4 — Same assessment/confirmation pattern as execute-backend.md

---

## STEP 1 — Execution (after confirmation)

### 1.0 — Read `header_file` once (if present)
CU's `backend-test` phase has no `TEST-PLAN-BE-HEADER.md` (below the
phase-preamble threshold) — skip.

### Per sub:
1. Read `packages/backend-test/[SUB].md` completely
2. Identify all scenarios
3. Generate: Spring Boot test class (`@SpringBootTest`/`@WebMvcTest` +
   `MockMvc`), file `src/test/java/.../[Scenario]Test.java`
4. Run: `mvn test -Dtest=[Class]` via bash. The `postgres` MCP server wired
   in `.mcp.json` (`postgres-mcp`, `--access-mode=restricted`, read-only) for
   any DB assertion.
5. Classify every failure/skip using the shared taxonomy
6. Update `execution-state.json`

---

## STEP 2 — Session Report

Write to `reports/TEST-REPORT-CU-backend-[YYYY-MM-DD].md`. Any
`FAIL` → hand off to `AUTONOMOUS-FULLSTACK-FIXING-AGENT.md` — never fix here.

---

## Test Sub Map — CU

| Sub | Scenarios | Notes |
|---|---|---|
| RULE-SCENARIOS | TC-BE-CU-001..006+ | Rule-focused scenarios (uniqueness, required fields, update semantics) for API-CU-001/003. |
| API-SCENARIOS | TC-BE-CU-007..011+ | API-level happy-path + MANDATORY-J platform scenarios (search, update, deactivate, get, get-unknown-404) across API-CU-002/003/004/005. |

---

## Constraints (NON-NEGOTIABLE)

- NEVER run before the gate check passes
- NEVER treat `*-HEADER.md` as a sub
- NEVER skip mandatory scenarios (the Mandatory-J TC blocks embedded inside
  the RULE-SCENARIOS / API-SCENARIOS sub files)
- NEVER modify application source code — report, don't fix
- NEVER run mutating SQL via the PostgreSQL MCP (read-only only)
- ALWAYS classify every failure/skip
- ALWAYS update execution-state.json after every sub
