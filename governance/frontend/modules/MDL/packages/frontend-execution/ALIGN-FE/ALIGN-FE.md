<!-- source: PHASE:ALIGN-FE -->
<!-- traces: AC-MDL-001, AC-MDL-002, AC-MDL-003, AC-MDL-004, AC-MDL-005, AC-MDL-006, AC-MDL-007, AC-MDL-008, AC-MDL-009, AC-MDL-010, AC-MDL-011, AC-MDL-012, AC-MDL-013, API-MDL-001, API-MDL-002, API-MDL-003, API-MDL-004, API-MDL-005, API-MDL-006, API-MDL-007, API-MDL-008, API-MDL-009, API-MDL-010, API-MDL-011, REQ-MDL-001, REQ-MDL-002, REQ-MDL-003, REQ-MDL-004, REQ-MDL-005, REQ-MDL-006, REQ-MDL-007, REQ-MDL-008, REQ-MDL-009, REQ-MDL-010, REQ-MDL-011, REQ-MDL-012, REQ-MDL-013, SCR-MDL-001, SCR-MDL-002, UXD-MDL-001 -->
<!-- PHASE:ALIGN-FE:START traces=SCR-MDL-001,SCR-MDL-002,UXD-MDL-001,REQ-MDL-001,REQ-MDL-002,REQ-MDL-003,REQ-MDL-004,REQ-MDL-005,REQ-MDL-006,REQ-MDL-007,REQ-MDL-008,REQ-MDL-009,REQ-MDL-010,REQ-MDL-011,REQ-MDL-012,REQ-MDL-013,AC-MDL-001,AC-MDL-002,AC-MDL-003,AC-MDL-004,AC-MDL-005,AC-MDL-006,AC-MDL-007,AC-MDL-008,AC-MDL-009,AC-MDL-010,AC-MDL-011,AC-MDL-012,AC-MDL-013,API-MDL-001,API-MDL-002,API-MDL-003,API-MDL-004,API-MDL-005,API-MDL-006,API-MDL-007,API-MDL-008,API-MDL-009,API-MDL-010,API-MDL-011 -->
## PHASE 6 — ALIGN-FE

The alignment self-check is this phase's content. Every row names the check that backs it, and
there are no other rows: a row nothing can falsify manufactures confidence, so a dimension with
no check behind it is not asserted here at all. The `RESULT` row is written by the orchestrator
from the analyze report. Never split — level-1 only.

```
ALIGN-FE — MDL v1
row           backing check   assertion
SCREENS       orphans         every SCR is referenced by a plan block — SCR-MDL-001 and
                              SCR-MDL-002 each carry a SUB in F1, F2, F3 and F4 and a block in
                              SEC-FE
COMPOSITION   screen-composition  every SCR names where its secondary detail sits and that it
                              saves once — SCR-MDL-001: none inline, a summary row per value and
                              its editor as a second level, one submit per open surface;
                              SCR-MDL-002: none, and it submits nothing
UXD           orphans         every UXD is cited by a plan block — UXD-MDL-001 is cited by both
                              F2 SCREEN-INIT blocks, by both facades, by the F3 validator for
                              RULE-MDL-001, by both F4 Cross-module lines and by SEC-FE
TRACES        traces          every PHASE and SUB carries traces=; UXD-MDL-001 traces to its REQ
                              and its AC; every SCR traces to its REQ and its UXD
API           traces          every API this plan cites is defined in the fetched api-docs —
                              API-MDL-001..011, each carrying its own Contract ID line there
                              (ADR-MDL-011) — and never in the backend plan's contract summary.
                              No foreign module's API id is cited here at all
FOREIGN       xref-surface    every reference to another module's surface resolves in that
                              module's own artifacts. This plan writes no foreign path and no
                              foreign id: the one cross-module read is cited as UXD-MDL-001 and
                              named in ui-ux-spec-mdl.md, so this clause has no subject here
REGISTRY      registry-agree  the UXD and both SCR defined here are in registry-exec-fe-mdl.md,
                              and nothing else is
LANGUAGES     languages       labels and messages in ar + en
MARKERS       markers         the parser reports no structural or semantic error for this track
                              and plan
DECISIONS     refs-exist      every ADR this plan cites exists on disk in
                              analysis/decisions/MDL/ — ADR-MDL-002, ADR-MDL-003, ADR-MDL-004,
                              ADR-MDL-005, ADR-MDL-006, ADR-MDL-007, ADR-MDL-011, ADR-MDL-012,
                              ADR-MDL-013, ADR-MDL-014, ADR-MDL-015, ADR-MDL-016, and the two
                              superseded ones they cite
COVERAGE      (the report)    none — the analyze report lists no clause as having examined
                              nothing. Two facts sit behind that word: every clause that counts
                              its subjects counted at least one here, and `xref-surface` reports
                              no count at all, so it appears in neither the coverage map nor that
                              list. It has no subject in this plan, by the decision the API row
                              above records.
RESULT        PASSED ✓ — 0 findings
```

### Operations coverage

| Operation | API | SCR action | Route | Status |
|---|---|---|---|---|
| search lookup types | API-MDL-001 | SCR-MDL-001 · type search | `/reference-data/lookups` | ✓ |
| create lookup type | API-MDL-002 | SCR-MDL-001 · type entry, create | `/reference-data/lookups/new` | ✓ |
| update lookup type | API-MDL-003 | SCR-MDL-001 · type entry, edit | `/reference-data/lookups/:typeId/edit` | ✓ |
| deactivate lookup type | API-MDL-004 | SCR-MDL-001 · deactivate a type | `/reference-data/lookups/:typeId` | ✓ |
| search a type's values | API-MDL-005 | SCR-MDL-001 · the values pane | `/reference-data/lookups/:typeId` | ✓ |
| create lookup value | API-MDL-006 | SCR-MDL-001 · value entry, create | `/reference-data/lookups/:typeId/values/new` | ✓ |
| update lookup value | API-MDL-007 | SCR-MDL-001 · value entry, edit | `/reference-data/lookups/:typeId/values/:valueId/edit` | ✓ |
| deactivate lookup value | API-MDL-008 | SCR-MDL-001 · deactivate a value | `/reference-data/lookups/:typeId` | ✓ |
| reorder a type's values | API-MDL-009 | SCR-MDL-001 · drag to reorder (disabled while filtered, G3) | `/reference-data/lookups/:typeId` | ✓ |
| browse the registry by owner | API-MDL-010 | SCR-MDL-002 · the browse itself | `/reference-data/type-registry` | ✓ |
| read active values by key | API-MDL-011 | — a consuming module's backend call | — (ADR-MDL-007) | ✗ |
| read one type by id | — none published | hydrated from the search cache, redirected on cold load | — (ADR-MDL-005, ADR-MDL-014) | ✗ |
| read one value by id | — none published | hydrated from the search cache, redirected on cold load | — (ADR-MDL-005, ADR-MDL-014) | ✗ |

Ten of the eleven published endpoints carry a route and a ✓. Three rows carry a ✗ with the ADR
that explains it: one endpoint published for a caller that is not this frontend, and two
operations the SRS names for which nothing is published. No row is a ✗ for want of a decision,
and no row carries an empty route without one.
<!-- PHASE:ALIGN-FE:END -->
