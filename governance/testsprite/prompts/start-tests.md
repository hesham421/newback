# Ready Prompt — Start a Fresh TestSprite Run (Backend)

Use this to run TestSprite from scratch: it (re)generates the PRD, the test
plan, and every test file, then executes them. This **rewrites** whatever
was in `testsprite_tests/` before — read
`governance/testsprite/TESTSPRITE-GOVERNANCE.md` first if you haven't.

Preconditions:
- The Spring Boot app is running locally on `http://localhost:7272`
  (`mvn spring-boot:run`, see `backend/CLAUDE.md`'s "Running Locally").
- `TESTSPRITE_API_KEY` is exported in the shell that launched this session.

---

## Prompt

```
Read backend/governance/testsprite/TESTSPRITE-GOVERNANCE.md in full before doing anything else.

Step 0 — housekeeping: check backend/testsprite_tests/ for any .py files,
standard_prd.json, testsprite_backend_test_plan.json, or report files left
over from a previous run that were never archived (i.e. anything other than
the tmp/ folder). If any exist, archive them first exactly per §4 of
TESTSPRITE-GOVERNANCE.md (classify each .py by §3's endpoint table, git mv it
into governance/modules/<MOD>/testsprite/tests/, and move the
PRD/plan/report trio into a new governance/testsprite/runs/<today>-backend/
folder) before continuing. Do not skip this even if the folder looks mostly
empty — check.

Step 1 — confirm the backend is reachable at http://localhost:7272/actuator/health.
If it isn't, stop and tell me instead of proceeding.

Step 2 — run the full TestSprite pipeline against this backend using the
TestSprite MCP tools, in order:
  1. testsprite_bootstrap_tests — type=backend, scope=codebase,
     localPort=7272, projectPath=<this repo's absolute path>.
  2. testsprite_generate_code_summary
  3. testsprite_generate_standardized_prd
  4. testsprite_generate_backend_test_plan
  5. testsprite_generate_code_and_execute

Step 3 — once the run finishes (code generated, executed, report written),
close it out per §4 "After a run finishes" in TESTSPRITE-GOVERNANCE.md:
classify every TCnnn in the new testsprite_backend_test_plan.json by §3,
move each .py into its module's governance/modules/<MOD>/testsprite/tests/,
and move standard_prd.json + testsprite_backend_test_plan.json +
both report files into governance/testsprite/runs/<YYYY-MM-DD>-backend/
using today's actual date. Confirm testsprite_tests/ ends with nothing but
tmp/ in it.

Step 4 — give me a short summary: how many test cases ran, pass/fail count,
and which modules they landed in.
```
