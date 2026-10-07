## REGISTRY — P3.1 — MDL v1
══════════════════════════════════════════════════════════════════
Module : MDL (البيانات المرجعية / Master Data Lookup)   Version : v1   Profile : erp
Source : analysis/modules/MDL/P3_1/backend-execution-plan-mdl.md
══════════════════════════════════════════════════════════════════

ID RANGES
API-MDL-001 .. API-MDL-011 · QR-MDL-001 .. QR-MDL-017
Neither sequence restarts and nothing is renumbered: the API range and QR-MDL-001..015 continue
the assignment this module version already carries, and every downstream artifact that cites one
of them still resolves to the same operation. QR-MDL-016 and QR-MDL-017 are the only ids minted
at this run — the two sequence allocations the `sequence` PK strategy makes explicit.

API ids: API-MDL-001, API-MDL-002, API-MDL-003, API-MDL-004, API-MDL-005, API-MDL-006,
API-MDL-007, API-MDL-008, API-MDL-009, API-MDL-010, API-MDL-011

QR ids: QR-MDL-001, QR-MDL-002, QR-MDL-003, QR-MDL-004, QR-MDL-005, QR-MDL-006, QR-MDL-007,
QR-MDL-008, QR-MDL-009, QR-MDL-010, QR-MDL-011, QR-MDL-012, QR-MDL-013, QR-MDL-014,
QR-MDL-015, QR-MDL-016, QR-MDL-017

ENTITIES / TABLES bound
| ENT | Table | PK generation | Business code |
|---|---|---|---|
| ENT-MDL-001 | MDL_LOOKUP_TYPE | sequence SEQ_MDL_LOOKUP_TYPE | none — `key` is the registrar's business key |
| ENT-MDL-002 | MDL_LOOKUP_VALUE | sequence SEQ_MDL_LOOKUP_VALUE | none — `code`, unique within its type |

All 21 DBF ids (DBF-MDL-001 … DBF-MDL-021) are bound in the DB Alignment Manifest, status ✓ on
every row; no field is PENDING and no entity is PENDING DB. The PK generation recorded here is
the profile's `sequence`, which corrects the `IDENTITY` both entity blocks carried before this
run (db-script §4.2).

Lookups reused / new: none — MDL owns no lookup key and consumes none (SRS A6). It is the store
every other module's keys live in; each of those is registered and seeded by its owning module
through API-MDL-002 and API-MDL-006, never by this module and never by another module's script.

XM STATUS
open: none · deferred: none · active: 1 — XM-MDL-001 (SOFT-READ, MDL → SEC, ENT-SEC-004 /
SEC_MODULE_REG), placed in INT-C and resolved READY in INT-R. The db-script register declares
exactly this row and this stage minted none, so nothing is owed back to the register. The access
is an injected in-process interface (`profile.conventions.module_interface: in_process`), never
an HTTP call. One item is owed outside this module and is recorded, not asserted: SEC's own P3.1
artifacts register only `SecUserDirectoryApi` as an exposed cross-module surface, so the
module-registry read this row consumes is not yet written down on SEC's side — SEC's artifact to
correct on its own re-run.
Inbound: XM-FIN-001 (FIN → MDL, SOFT-READ, ACTIVE) consumes API-MDL-011; the id belongs to FIN.

CATALOG
10 error-catalog rows — 7 module codes, every one an instance of `{MOD}-{http}[-{SLUG}]` on a
status `profile.stack.backend.api.http_statuses` declares, and 3 platform rows carrying the
shared handler's own code strings (ACCESS_DENIED, VALIDATION_ERROR, INTERNAL_ERROR), which this
module does not mint and must not. Rule-backed: RULE-MDL-001 → MDL-409-MODULE-NOT-REGISTERED,
RULE-MDL-002 → MDL-409-VALUE-DUP, RULE-MDL-004 → MDL-404-TYPE-KEY. PLATFORM-STD under the
ADR-SEC-002 umbrella: MDL-409-TYPE-DUP, MDL-404-TYPE, MDL-404-VALUE, MDL-400-REORDER-MISMATCH
and the three platform rows.
Rules without a message: none — every RULE this plan enforces carries its ar and en text
character-perfect from the SRS. RULE-MDL-003 carries no row by construction: the key is absent
from the update request, so no code path can raise one.
Struck this run: the 503 row the previous revision carried for a network failure that cannot
occur in a single deployable, on a status the platform does not declare.

SECURITY
2 composite screens × 3 roles · 5 permissions:
PERM_MDL_LOOKUPS_VIEW (gateway), PERM_MDL_LOOKUPS_CREATE, PERM_MDL_LOOKUPS_UPDATE,
PERM_MDL_LOOKUPS_DELETE on page MDL_LOOKUPS; PERM_MDL_TYPE_REGISTRY_VIEW on page
MDL_TYPE_REGISTRY — each with a BOOTSTRAP DATA row naming its grant target, because registering
a page and minting a name is not a grant.

ALIGN
Verdict as stamped in backend-execution-plan-mdl.md → ALIGN → the verdict line (written by the
orchestrator from the analyze report). Findings fixed during the run: four statements of the
previous revision that described a system nobody built — the IDENTITY PK generation, the three
`GET` read verbs, the REST-call cross-module mechanism and the unraisable 503 catalog row. None
outstanding.

ADRs
No ADR raised at this stage. Applied, not re-assigned: ADR-MDL-002 and ADR-MDL-007 (P3.2),
ADR-MDL-009 and ADR-MDL-010 (P2), and the two platform precedents ADR-SEC-002 and ADR-FIN-001.
Every ADR-MDL-* this plan cites exists in analysis/decisions/MDL/. No BLOCKED ADR — the pass was
not stopped. No question was raised.

TRACEABILITY
REQ covered by ≥1 API/DBF: 13/13 — REQ-MDL-001 … REQ-MDL-013, each named by at least one API
block's traces and by the DBF records of db-script-mdl.md. Orphan REQ: none.
QR reached by ≥1 API: 17/17. Entity operations resolved to an API: 8/8. Screen operations the
SRS names, resolved to an API: 8/8 (search, create, read, update, deactivate, reorder on
SCR-REQ-MDL-001; search, browse on SCR-REQ-MDL-002) — none excluded and none deferred.

Event
"P3.1 completed: MDL v1 — 11 API, 17 QR, 1 XM, 10 catalog rows, 0 new ADRs"
══════════════════════════════════════════════════════════════════
