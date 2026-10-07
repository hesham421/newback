# ADR-MDL-024 — TC-MDL-014 keeps its id and is retargeted to RULE-MDL-001's own Test-Hint

Module  : MDL     Version : v1     Stage raised : test-gen (Test Generation, standalone)
Status  : ACCEPTED (non-breaking)

## Context
The previous revision of `backend-test-plan-mdl.md` was produced at `scope: project` (modules
MDL, SEC) and carried a `PHASE:INT-XM` block holding one test:

> TC-MDL-014 — graceful degradation when SEC is unreachable during owner-module validation
> Preconditions: SEC's registry-search endpoint (API-SEC-021) is made unreachable/times out

Two things are wrong with it, and neither is a matter of taste.

1. **The failure mode cannot be constructed.** `profile.conventions.module_interface` is
   `in_process`: XM-MDL-001 is an injected call inside one deployable. backend-execution-plan
   PHASE 6 (INT-R) says it outright — "there is no HTTP-level way to simulate 'SEC unreachable',
   and no test should try to" — and PHASE 8 struck the 503 catalog row for the same reason.
2. **It names the wrong surface.** `API-SEC-021` is the published HTTP read UXD-MDL-001 uses
   from the *frontend*, not the in-process contract XM-MDL-001 consumes, and this module's test
   plan cites no foreign module's endpoint id at all.

The frontend track filed both as **PF-MDL-005** (frontend-execution-plan-mdl.md, API SURFACE),
owner "MDL backend track", with the required change written out: retarget the test to the
SOFT-READ's real untested edge — a module deregistered from SEC after its types were accepted
must not invalidate them — and drop the foreign-endpoint citation. It also noted the old test
duplicated TC-MDL-002's coverage.

This run is `--module MDL`, so the `INT-XM` phase is skipped in any case (§2 rule 3: a module
never gets an integration TC for a module outside the selection, and SEC is not selected). That
disposes of the phase but not of the id: `TC-MDL-014` is already minted, and the engine forbids
renumbering and forbids restarting a sequence.

## Decision
`TC-MDL-014` **keeps its id** and is rewritten, inside `SUB:RULE-SCENARIOS` of the module-scope
phase, as the state test RULE-MDL-001's own **Test-Hint** states in the SRS:

> الفحص عند الإنشاء وحده؛ إلغاء تسجيل وحدة في الأمان لاحقًا لا يُبطل أنواعها القائمة

It derives from **AC-MDL-002 (REQ-MDL-002)** through that hint, exercises this module's own
`API-MDL-001`, `API-MDL-003` and `API-MDL-011`, cites no foreign endpoint, and asserts that a
type accepted for `FIN` stays searchable, renameable and readable after `FIN`'s registry row is
gone. It is tagged `STATE · EDGE`, not `INTEGRATION`: nothing in it crosses into SEC's own
surface.

The `PHASE:INT-XM` block is not emitted at all — that half is mechanical, from the scope rule,
and is recorded here only so the two changes are not confused for one.

## Consequences
- PF-MDL-005 is answered in the artifact that owns `TC-*`, which is this plan and not the
  backend execution plan; the finding can be closed by its filer against this revision.
- The module now has 14 TCs for 13 ACs. That is 1.08× and well under the ~2× over-engineering
  guard, and the extra TC is derived from text the SRS already carries — it invents no scenario.
- No id is reused for a different subject in the sense the engine forbids: TC-MDL-014 still
  tests the boundary between this module and SEC's registry, and still traces to REQ-MDL-002.
  What changed is that it now tests a thing that can happen.
- If a later run is invoked at `scope: modules` or `project` with SEC selected, the `INT-XM`
  phase returns with its own TCs, continuing the sequence from TC-MDL-025 — the last id this
  module version has minted, in `frontend-test-plan-mdl.md`.
