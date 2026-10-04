# /FILE/execute-backend-test

Execute test scenarios for FILE — only for what's actually complete.

> Read `TEST-EXECUTION-AGENT.md` first.

## Usage
/FILE/execute-backend-test

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
⛔ TEST GATE FAILED — FILE
══════════════════════════════════════════════════════
Waiting on : [PHASE: status], ...
══════════════════════════════════════════════════════
```
STOP. Do not generate or run any test.

### 0.2–0.4 — Same assessment/confirmation pattern as execute-backend.md

---

## STEP 1 — Execution (after confirmation)

### 1.0 — Read `header_file` once (if present)
FILE's `backend-test` phase has no `TEST-PLAN-BE-HEADER.md` (not present
under `packages/backend-test/` — only `RULE-SCENARIOS.md`, `API-SCENARIOS.md`,
and `index.md` exist there; below the phase-preamble threshold) — skip.

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

Write to `reports/TEST-REPORT-FILE-backend-[YYYY-MM-DD].md`. Any
`FAIL` → hand off to `AUTONOMOUS-FULLSTACK-FIXING-AGENT.md` — never fix here.

---

## Test Sub Map — FILE

| Sub | Scenarios | Notes |
|---|---|---|
| RULE-SCENARIOS | TC-BE-FILE-001..012 | Rule-focused scenarios (happy + violation pairs) covering RULE-FILE-001..007 — size limit (API-FILE-001, ERR-0001), MIME/content-type restriction (API-FILE-001, ERR-0002), access-token validity/single-use (API-FILE-003, ERR-0003), Security-filter auth delegation (API-FILE-004, no self-JWT-check), ownership-fields-required (API-FILE-001, ERR-0004), soft-delete bytes-retained (API-FILE-006), and category-code uniqueness (API-FILE-007, ERR-0005). 7/7 rules covered, 0 gaps per SECTION D of `_SECTIONS.md`. |
| API-SCENARIOS | TC-BE-FILE-013..020 | API-level happy-path scenarios, one per API-ID (API-FILE-001..008 — 8/8 covered), including MANDATORY-J platform scenarios embedded inline: empty-list→200 (MANDATORY-J-7, API-FILE-005), permission/403 (MANDATORY-J-5, API-FILE-007), and SQLi-safe literal storage (MANDATORY-J-8, API-FILE-008). |

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
