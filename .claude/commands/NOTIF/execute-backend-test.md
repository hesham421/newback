# /NOTIF/execute-backend-test

Execute test scenarios for NOTIF — only for what's actually complete.

> Read `TEST-EXECUTION-AGENT.md` first.

## Usage
/NOTIF/execute-backend-test

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
⛔ TEST GATE FAILED — NOTIF
══════════════════════════════════════════════════════
Waiting on : [PHASE: status], ...
══════════════════════════════════════════════════════
```
STOP. Do not generate or run any test.

### 0.2–0.4 — Same assessment/confirmation pattern as execute-backend.md

---

## STEP 1 — Execution (after confirmation)

### 1.0 — Read `header_file` once (if present)
NOTIF's `backend-test` phase has no `TEST-PLAN-BE-HEADER.md` (below the
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

Write to `reports/TEST-REPORT-NOTIF-backend-[YYYY-MM-DD].md`. Any
`FAIL` → hand off to `AUTONOMOUS-FULLSTACK-FIXING-AGENT.md` — never fix here.

---

## Test Sub Map — NOTIF

| Sub | Scenarios | Notes |
|---|---|---|
| RULE-SCENARIOS | TC-BE-NOTIF-001..010 | Rule-focused scenarios covering RULE-NOTIF-001..007, 7/7 rule coverage, 0 gaps: fan-out one log per channel (001), retry-then-success and exhausted-retries-then-FAILED (002/003), disabled channel → CHANNEL_DISABLED no retry (004), bilingual template accepted/rejected (005/006, ERR-0001), auth delegated to Security filter (007, ATTACK 401), unique templateCode/channelTypeId accepted vs. duplicate rejected (008/009, ERR-0002/ERR-0003), skip inactive recipient with history retained (010). |
| API-SCENARIOS | TC-BE-NOTIF-011..016 | API-level happy-path scenarios, one per API-ID (6/6 covered): dispatch → 202 {logIds[]} (011); query logs happy + empty-filter → 200 [] MANDATORY-J-7 (012); log-by-id happy + unknown-id → 404 ERR-0004 (013); templates CRUD happy + no-VIEW-permission → 403 MANDATORY-J-5 (014); channels CRUD/enable happy + isEnabledFl toggle affects dispatch (015); lookups happy + SQLi-injection-safe MANDATORY-J-8 (016). |

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
