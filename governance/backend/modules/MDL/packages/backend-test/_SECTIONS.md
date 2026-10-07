<!-- source: content outside every PHASE block (leading / between / trailing sections) -->
# BACKEND TEST PLAN — البيانات المرجعية / Master Data Lookup (MDL)
══════════════════════════════════════════════════════════════════
Module : MDL   Version : v1   Profile : erp   Scope : project (modules MDL, SEC)
Sources: srs-mdl.md v1 · backend-execution-plan-mdl.md v1 · registry-srs-mdl.md v1 · registry-db-mdl.md v1
Framework: agnostic. REDUCED: no. Open ADRs: 0 new.
TC count: 13 (module scope) · 1 (integration — XM-MDL-001, MDL declares → SEC)
══════════════════════════════════════════════════════════════════





## TC TRACEABILITY INDEX
| AC | TC | REQ | API | RULE/code | XM |
|---|---|---|---|---|---|
| AC-MDL-001…013 | TC-MDL-001…013 (1:1) | REQ-MDL-001…013 (1:1) | see each TC's Exercises line | see each TC's Rule/code line | — |
| — | TC-MDL-014 | REQ-MDL-002 | API-MDL-002 | — | XM-MDL-001 |

## COVERAGE
AC covered 13/13 (0 gaps) · REQ covered 13/13 · API covered 11/11 · every selected-module
XM covered 1/1 (XM-MDL-001 → TC-MDL-014, no gap).
══════════════════════════════════════════════════════════════════
