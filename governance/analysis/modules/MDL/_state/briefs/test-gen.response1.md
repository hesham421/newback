Done. All four deliverables written, plus two ADRs the lane rules required.

**Files written**

| file | content |
|---|---|
| `governance-shared/analysis/modules/MDL/test_gen/backend-test-plan-mdl.md` | `PHASE:TEST-PLAN-BE` split into `SUB:RULE-SCENARIOS` / `SUB:API-SCENARIOS` (14 TCs > 12 threshold) — TC-MDL-001…014 |
| `…/test_gen/frontend-test-plan-mdl.md` | `PHASE:TEST-PLAN-FE` split into `SUB:UI-FLOWS` / `SUB:INT-FLOW` (11 TCs > 8) — TC-MDL-015…025 |
| `…/test_gen/test-execution-manifest-mdl.md` | dependency order · RULE→CODE→TC · entity CRUD checklist (`profile.stack.testing.manifest` is ON) |
| `analysis/decisions/MDL/ADR-MDL-023.md`, `ADR-MDL-024.md` | the two non-mechanical derivation choices |

`system-test-index-erp.md` was **not** written and the existing one not touched — that is `scope: project` only.

**Three things worth your attention**

1. **A prior revision existed** at `scope: project` (MDL, SEC) with a `PHASE:INT-XM` block. This run is `--module MDL`, so the integration phase is absent by §2 rule 3. I preserved its id assignment rather than renumbering: TC-MDL-001…013 still map 1:1 onto AC-MDL-001…013, so ids are AC-ordered and therefore not monotonic inside a SUB — the traceability index is the address, and the header says so.
2. **PF-MDL-005 is applied** (ADR-MDL-024). Old TC-MDL-014 tested "SEC unreachable", a failure mode `in_process` cannot produce — INT-R says no test should try — and cited a foreign endpoint id. It keeps its id and now tests RULE-MDL-001's own Test-Hint: a module deregistered from SEC after its types were accepted does not invalidate them. I did **not** act on PF-MDL-004 or PF-MDL-006: both are owned by other tracks, and no TC can assert a rejection the service does not perform.
3. **The frontend AC denominator is 11, not 13** (ADR-MDL-023). AC-MDL-011/012 are the consumer read, which ADR-MDL-007 gives no screen; marking them ✗ would block the run over a screen that deliberately does not exist. Both are covered on the backend track, and the plan states module-level 13/13 beside the track figure.

I also rebuilt the test data from the ACs' own values (`PAYMENT_METHOD`, `XYZ`, `CASH`, `NO_SUCH_KEY`…) — the previous revision invented `ZZZ`, `TEST_TYPE`, `USER_STATUS` and others — and every asserted message is now quoted character-perfect in ar and en.

**Verified mechanically, not by eye:** all 31 marker pairs closed, every TC's `traces=` carries an AC, 25 unique contiguous TC ids across the two files with no duplicate, AC 13/13 and REQ 13/13 and API 11/11 reached on the backend track. I did not run `gov.py analyze` — it writes `_state/analyze-stage-test-gen.md`, which is the runner's to produce, so C10 is still unconfirmed until the orchestrator runs it.