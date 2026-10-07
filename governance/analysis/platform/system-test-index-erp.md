SYSTEM TEST INDEX — ERP Platform — generated {scope: project}
══════════════════════════════════════════════════════════════════
Profile : erp   Modules rolled up : FIN, MDL, SEC (every module with a committed version)
Sources : backend-test-plan-{fin,mdl,sec}.md v1 · db-script-{fin,mdl,sec}.md v1
══════════════════════════════════════════════════════════════════

## AC → TC
| Module | AC covered | Uncovered AC |
|---|---|---|
| SEC | 33/33 | none (✗ list empty) |
| MDL | 13/13 | none (✗ list empty) |
| FIN | 46/46 | none (✗ list empty) |

## XM → TC
Every `XM-*` between two modules that both have a committed version (SEC, MDL, FIN all
qualify):
| XM | From → To | Covered | TC |
|---|---|---|---|
| XM-MDL-001 | MDL → SEC | ✓ | TC-MDL-014 |
| XM-FIN-001 | FIN → MDL | ✓ | TC-FIN-047 |
No XM targets FIN from any module in scope, and SEC declares no outbound XM (ROOT) — both
correctly absent, not gaps.

## UXD → TC
None — no `UXD-*` exists anywhere in the platform (no module has run P3.2 / frontend
execution planning yet). Not applicable, not a gap.

## COVERAGE %
| Module | AC % | Integration % (declares and/or is targeted by XM) |
|---|---|---|
| SEC | 100% (33/33) | targeted by 1 XM (XM-MDL-001) — 100% covered (1/1) |
| MDL | 100% (13/13) | declares 1 XM (XM-MDL-001) and is targeted by 1 XM (XM-FIN-001) — 100% covered (2/2) |
| FIN | 100% (46/46) | declares 1 XM (XM-FIN-001) — 100% covered (1/1) |

## CROSS-MODULE
| From module | To module | Linking atom | TC id(s) | Gap? |
|---|---|---|---|---|
| MDL | SEC | XM-MDL-001 (SOFT-READ) | TC-MDL-014 | no |
| FIN | MDL | XM-FIN-001 (SOFT-READ) | TC-FIN-047 | no |

No other module pair has a linking atom yet — ORG, PRC, HR, INV, SLS, CTR have no
committed version, so they are out of this rollup's scope (not a gap, per engine §9: "A
module the platform has never produced a version for is out of scope").
══════════════════════════════════════════════════════════════════
