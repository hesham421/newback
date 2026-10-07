<!-- source: content outside every PHASE block (leading / between / trailing sections) -->
# BACKEND TEST PLAN — الأمان / Security (SEC)
══════════════════════════════════════════════════════════════════
Module : SEC   Version : v1   Profile : erp   Scope : project (modules FIN, MDL, SEC)
Sources: srs-sec.md v1 · backend-execution-plan-sec.md v1 · registry-srs-sec.md v1 · registry-db-sec.md v1
Framework: agnostic (profile.stack.testing.backend) — every block below is a
  specification the consumer repo turns into its own test; no framework name,
  annotation or file layout appears anywhere in this document.
REDUCED: no — every input plan for SEC is present.
Open ADRs: 0 new this run (ADR-SEC-001, ADR-SEC-002 unaffected).
TC count: 33 (module scope) · 0 (integration — SEC is ROOT, declares no XM)
══════════════════════════════════════════════════════════════════



No `INT-XM` phase — SEC is ROOT; it declares no `XM-*` (db-script-sec.md §2). Not an
empty block, simply absent, per §2/§4 of the engine.

## TC TRACEABILITY INDEX
| AC | TC | REQ | API | RULE/code | XM |
|---|---|---|---|---|---|
| AC-SEC-001…033 | TC-SEC-001…033 (1:1) | REQ-SEC-001…033 (1:1) | see each TC's Exercises line | see each TC's Rule/code line | none |

## COVERAGE
AC covered 33/33 (0 gaps) · REQ covered 33/33 · API covered 27/27 (every API-SEC-001..027
exercised by ≥1 TC, either directly or as the side-effect target of TC-SEC-024) · every
selected-module XM covered: not applicable (SEC declares none).
══════════════════════════════════════════════════════════════════
