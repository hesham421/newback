# Ready Prompt — Re-run Existing Tests (Backend, No Regeneration)

Use this when you want to know if the **already-generated** tests still
pass against the current code — after a bug fix, a migration, or just to
re-check — without asking TestSprite to regenerate anything (no new PRD, no
new test plan, no renumbering, no new files). This runs the archived `.py`
files directly with plain Python, the same way TestSprite itself executes
them (each file is self-contained and ends with a bare call to its own
`test_*()` function — do not wrap them in pytest, that would call the
function twice).

Preconditions:
- The Spring Boot app is running locally on `http://localhost:7272`.
- You are not trying to test new/changed endpoints that don't have a
  generated test yet — this only re-executes what already exists. If the
  API surface changed, use `start-tests.md` instead.

---

## Prompt

```
Do not call any TestSprite MCP tool for this — no bootstrap, no
generate_code_summary, no generate_standardized_prd, no
generate_backend_test_plan, no generate_code_and_execute. This is a plain
re-execution of tests that already exist under
backend/governance/modules/*/testsprite/tests/ — nothing gets rewritten.

Step 1 — confirm the backend is reachable at http://localhost:7272/actuator/health.
If it isn't, stop and tell me instead of proceeding.

Step 2 — [choose one]
  (a) Re-run every archived backend TestSprite test: for each .py file under
      backend/governance/modules/*/testsprite/tests/, run it with
      `python3 <path>` (not pytest — each file already calls its own test
      function at the bottom) and record pass ("no assertion error, exit 0")
      or fail (the assertion message / traceback) per file.
  (b) Re-run only module [NAME]: same as (a), but scoped to
      backend/governance/modules/[NAME]/testsprite/tests/*.py.

Step 3 — report a simple table: file name, module, pass/fail, and for any
failure the assertion message. Do not modify any .py file to make it pass —
if a test fails because the API genuinely changed, tell me that's a
regression or an intentional contract change to review, and point me at
start-tests.md if regenerating is actually what's needed.
```
