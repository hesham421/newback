# Ready Prompt — Fix Bugs Found by TestSprite (Backend, Fix-and-Verify Loop)

Use this after a run (`start-tests.md`) or a re-run (`rerun-tests.md`)
reports failing tests. It drives a diagnose → fix → re-run loop until every
failure is genuinely resolved — either the code was actually wrong and got
fixed, or the test's expectation was legitimately outdated and got
updated per `TESTSPRITE-GOVERNANCE.md` §5. It never gets a test to go
green by weakening the code's actual behavior.

Preconditions:
- You have a concrete list of failing tests and their actual error output
  (from `testsprite-mcp-test-report.md` for a fresh run, or from the table
  produced by `rerun-tests.md`). Don't start this without real failure
  output in hand — "some tests are probably failing" is not enough to
  diagnose against.
- The backend is running locally on `http://localhost:7272`.

---

## Prompt

```
I have TestSprite backend test failures to fix. Failing tests and their
error output: [PASTE the failing test names + assertion/error messages
here — from the report or from rerun-tests.md's output table].

For EACH failing test, follow this loop. Do not skip the diagnosis step to
jump straight to a fix.

STEP 1 — Diagnose, don't guess.
  a. Open the test file itself at
     backend/governance/modules/<MOD>/testsprite/tests/TCnnn_<slug>.py and
     read exactly what it sends and exactly what it asserts.
  b. Trace the real request path in the code it's calling — Controller →
     Service → Domain → Repository — and find the exact point where actual
     behavior diverges from the test's expectation.
  c. Classify the failure as exactly one of:
     - REAL BUG — the code does not correctly implement the documented
       business rule (check the module's P0 business-policies doc if
       unsure what the rule actually is). The code is wrong; the test's
       expectation is right.
     - STALE TEST — the code's behavior changed deliberately and
       correctly (an approved, intentional contract change), and the
       test's assumption is simply out of date. The test is wrong; the
       code is right.
     - ENVIRONMENT ISSUE — not a real bug at all: server not running,
       stale token, missing seed data, port mismatch, timing/flake. Note
       it and move on — don't "fix" this by touching code or the test.
  d. If you cannot confidently tell which of the three it is, STOP and ask
     me — do not guess. Misclassifying a real bug as "stale test" hides
     the bug; misclassifying a stale test as "real bug" risks a wrong fix.

STEP 2 — Fix, respecting governance, only for REAL BUG.
  - Read governance/GOVERNANCE-RULES.md's skill-routing table first, then
    the specific skill(s) for whatever layer you're touching
    (gov-enforce-backend-contract always first, then build-create-entity /
    build-create-repository / build-create-dto / build-create-mapper /
    build-create-service / build-create-controller as applicable) — same as
    any other backend change.
  - The fix must make the code correctly implement the actual, documented
    business rule. It must NOT: loosen or remove validation just to turn a
    400 into a 200, return a stubbed/hardcoded value instead of computing
    the real one, bypass a DataScope or permission check, remove exception
    handling to make a status-code assertion pass, or add a special case
    that exists only to satisfy this test's specific fixture data.
  - If making the test pass would require contradicting a module's
    business-policies doc or the domain-layer rules — that's not a code
    bug, it's a requirements conflict. STOP and flag it instead of
    forcing a change.
  - Never change the columns or behavior of a module its own governance
    docs mark as a permanent exception, just to make a test pass — flag
    it for an explicit human decision instead.
  - Keep the fix minimal and targeted at the one root cause — no unrelated
    refactoring while you're in a bug-fix pass.

STEP 2 (alt) — Update the test, only for STALE TEST.
  - Follow governance/testsprite/TESTSPRITE-GOVERNANCE.md §5 exactly:
    patch only the specific payload/assertion that's out of date, don't
    rewrite the scenario, and don't touch it at all if you're not fully
    sure the code's new behavior is the intended one.

STEP 3 — Re-run and verify, every time, no exceptions.
  - Per rerun-tests.md's mechanism (no TestSprite MCP calls — direct
    `python3 <path>`), re-run the ONE test you just touched first. If it
    still fails, go back to STEP 1 — don't try a second blind fix on top
    of the first.
  - Once it passes, re-run every OTHER test in
    governance/modules/<MOD>/testsprite/tests/ for that same module — a
    fix must not break a sibling scenario in the same module.
  - If the fix touched shared code (a crossmodule/ interface, a common
    DTO/util, anything another module's tests might also exercise),
    additionally re-run the archived tests of every module that consumes
    it.
  - Don't attempt more than 3 distinct fixes on the same test without
    stopping to report progress and ask how to proceed — a test still
    failing after 3 genuinely different diagnoses is a sign something
    bigger is wrong, not a reason to keep guessing.

STEP 4 — Report, once every failure in this batch is resolved or triaged.
  For each original failing test, state: REAL BUG (fixed, with a one-line
  summary of the actual root cause and the fix) / STALE TEST (updated,
  with what changed and why) / STILL OPEN (why, and what you need from me
  to proceed) / ENVIRONMENT ISSUE (what was wrong, whether it's resolved
  now). Then confirm the full affected module's archived suite passes
  end-to-end as a final regression check — not just the tests that were
  originally failing.
```
