## REGISTRY — P3.1 — SEC v2
══════════════════════════════════════════════════════════════════
Change set : CS-SEC-001 (ADDITIVE), a delta on v1. This registry is the complete current P3.1 registry: the v1 rows
             are restated and the v2 rows marked, so the fold needs no v1 line.

ID RANGES
API-SEC-001 .. API-SEC-039 · QR-SEC-001 .. QR-SEC-054
v2 ADDED: API-SEC-028, API-SEC-029, API-SEC-030, API-SEC-031 (service-account credentials and the client-credentials
token), API-SEC-032, API-SEC-033, API-SEC-034, API-SEC-035, API-SEC-036 (endpoints the SRS screens demand, declared by
ADR-SEC-038), API-SEC-037, API-SEC-038, API-SEC-039 (the in-process cross-module read surfaces, given ids by
ADR-SEC-040) · QR-SEC-040, QR-SEC-041, QR-SEC-042, QR-SEC-043, QR-SEC-044, QR-SEC-045, QR-SEC-046, QR-SEC-047,
QR-SEC-048, QR-SEC-049, QR-SEC-050, QR-SEC-051, QR-SEC-052, QR-SEC-053, QR-SEC-054
v2 MODIFIED in substance: API-SEC-001, API-SEC-003, API-SEC-005, API-SEC-006, API-SEC-007, API-SEC-008, API-SEC-009,
API-SEC-010, API-SEC-011, API-SEC-022 · QR-SEC-001, QR-SEC-004, QR-SEC-005, QR-SEC-006, QR-SEC-011, QR-SEC-022,
QR-SEC-026; every other v1 block in annotation only (ADR-SEC-036). Nothing renumbered, no sequence restarted.
QR-SEC-039 was minted in v1 by the 2026-09-11 amendment and never reached the v1 registry; it is registered here.

API ids: API-SEC-001, API-SEC-002, API-SEC-003, API-SEC-004, API-SEC-005, API-SEC-006, API-SEC-007, API-SEC-008,
API-SEC-009, API-SEC-010, API-SEC-011, API-SEC-012, API-SEC-013, API-SEC-014, API-SEC-015, API-SEC-016, API-SEC-017,
API-SEC-018, API-SEC-019, API-SEC-020, API-SEC-021, API-SEC-022, API-SEC-023, API-SEC-024, API-SEC-025, API-SEC-026,
API-SEC-027, API-SEC-028, API-SEC-029, API-SEC-030, API-SEC-031, API-SEC-032, API-SEC-033, API-SEC-034, API-SEC-035,
API-SEC-036, API-SEC-037, API-SEC-038 and API-SEC-039

QR ids: QR-SEC-001, QR-SEC-002, QR-SEC-003, QR-SEC-004, QR-SEC-005, QR-SEC-006, QR-SEC-007, QR-SEC-008, QR-SEC-009,
QR-SEC-010, QR-SEC-011, QR-SEC-012, QR-SEC-013, QR-SEC-014, QR-SEC-015, QR-SEC-016, QR-SEC-017, QR-SEC-018,
QR-SEC-019, QR-SEC-020, QR-SEC-021, QR-SEC-022, QR-SEC-023, QR-SEC-024, QR-SEC-025, QR-SEC-026, QR-SEC-027,
QR-SEC-028, QR-SEC-029, QR-SEC-030, QR-SEC-031, QR-SEC-032, QR-SEC-033, QR-SEC-034, QR-SEC-035, QR-SEC-036,
QR-SEC-037, QR-SEC-038, QR-SEC-039, QR-SEC-040, QR-SEC-041, QR-SEC-042, QR-SEC-043, QR-SEC-044, QR-SEC-045,
QR-SEC-046, QR-SEC-047, QR-SEC-048, QR-SEC-049, QR-SEC-050, QR-SEC-051, QR-SEC-052, QR-SEC-053, and QR-SEC-054

ENTITIES / TABLES bound
All 14 entities, ENT-SEC-001 … ENT-SEC-014, are bound to their db-script tables (SEC_USER … SEC_SVC_ACCOUNT_CRED), and
all 116 DBF ids, DBF-SEC-001 … DBF-SEC-116, carry a DB Alignment Manifest row. Every PK binding names its table's
sequence (SEQ_SEC_USER … SEQ_SEC_SVC_ACCOUNT_CRED, ADR-SEC-035) instead of v1's identity clause.
Lookups: four SEC-owned keys, CHECK-constrained (ADR-SEC-001). Reused: USER_STATUS, SIGNUP_STATUS, AUDIT_EVENT_TYPE
(v2: 21 codes — +SERVICE_ACCOUNT_REACTIVATED, revise pass G6). New in v2: PRINCIPAL_TYPE. Labels through the
frontend's single resolver (ADR-SEC-006).

XM STATUS
open: none · deferred: none — SEC is ROOT, 0 XM rows. Exposed in-process surfaces (no XM of SEC's): SecUserDirectoryApi
(REQ-SEC-034, REQ-SEC-035) and SecModuleRegistryApi (read by MDL's XM-MDL-001) — backend-execution-plan-sec.md, INT-R.

CATALOG
37 rows: 28 carried from v1 (27 module codes and INTERNAL_ERROR), the shared VALIDATION_ERROR row the v1 catalog
omitted, and 8 added in v2 — 35 codes of the `SEC-{http}[-{SLUG}]` format and 2 shared. Of the v2 rows, five (four
plus one) carry a RULE message: SEC-401-INVALID-CLIENT (RULE-SEC-009), SEC-409-NOT-ACTIVE-SERVICE-ACCOUNT
(RULE-SEC-008), SEC-409-CREDENTIAL-ALREADY-REVOKED (RULE-SEC-011), SEC-409-PRINCIPAL-TYPE-FIXED (RULE-SEC-012) and
SEC-409-CREDENTIAL-LIMIT (RULE-SEC-013, ADR-SEC-042 — relabeled from PLATFORM-STD). Two are PLATFORM-STD:
SEC-400-UNSUPPORTED-GRANT-TYPE and SEC-404-CREDENTIAL (both ADR-SEC-037). RULE-SEC-010 reuses
SEC-401-INVALID-CREDENTIALS on purpose, so a login does not disclose the account type. Rules without a message:
none — every rule that produces a user-facing message has a full ar/en pair. PLATFORM-STD rows: ADR-SEC-002.

ALIGN
The orchestrator stamps the verdict from the analyze report into the ALIGN block of backend-execution-plan-sec.md
(PHASE 8, ALIGN-BE). This registry carries no hand-counted verdict. The three findings ADR-SEC-039 recorded (G4 fix:
corrected from "open by design" — they are CLOSED) are closed by ADR-SEC-040: REQ-SEC-034 is served by API-SEC-037,
REQ-SEC-035 by API-SEC-038 (both ADR-SEC-040), and QR-SEC-039 is reached by API-SEC-038. Fixed in v2: the v1 identity
bindings; the v1 code-format declaration; v1 blocks with no entity line or with DBF ids missing from their writing
lines; the missing BOOTSTRAP DATA section; v1's ADR paths, which pointed at `erp/decisions/SEC/`.

ADRs
decisions/SEC/ADR-SEC-036.md — v2 plan restated in full, v1 blocks brought to the current template (ACCEPTED, non-breaking)
decisions/SEC/ADR-SEC-037.md — service-account token mechanism, secret handling, rejection-path audit, two PLATFORM-STD codes (ACCEPTED, non-breaking; §4 amended by ADR-SEC-045, revise pass G1)
decisions/SEC/ADR-SEC-038.md — five SRS-demanded endpoints declared; role and registry-row deactivation DEFERRED (ACCEPTED, non-breaking)
decisions/SEC/ADR-SEC-039.md — exposed in-process surfaces are not endpoints; the three findings it records — **SUPERSEDED by ADR-SEC-040 (G4 fix)** ·
decisions/SEC/ADR-SEC-040.md — those surfaces get API ids (API-SEC-037, API-SEC-038, API-SEC-039), closing all three (ACCEPTED, non-breaking)
decisions/SEC/ADR-SEC-042.md — RULE-SEC-013, the ten-credential active limit on API-SEC-029 (ACCEPTED, non-breaking, G3)
decisions/SEC/ADR-SEC-043.md — HMAC-SHA256 + server-side pepper for secretHash (§1, ACCEPTED); module code a 429 code (§2, **SUPERSEDED by ADR-SEC-045**) — reconciled into the plan by revise pass G1/G2
decisions/SEC/ADR-SEC-044.md — CAT-10, ingress throttling ahead of the application for API-SEC-001 and API-SEC-031, no module code for it (ACCEPTED, non-breaking) — reconciled into the plan by revise pass G2/G3
decisions/SEC/ADR-SEC-045.md — supersedes ADR-SEC-043 §2: no 429 code, throttling stays at ingress per ADR-SEC-044; notes ADR-SEC-037 §4 amended to scope it to API-SEC-001 alone (ACCEPTED, non-breaking, revise pass G1)
Applied from P2: decisions/SEC/ADR-SEC-041.md — UQ_SEC_SIGNUP_REQUEST_PENDING_EMAIL guarded partial unique index (ACCEPTED, non-breaking, G1)
Applied from P1 (revise pass): decisions/SEC/ADR-SEC-046.md — C5.14's AC parser accepts both the labelled and the
v1 unlabelled Given/When/Then forms, rather than rewriting v1 AC text (ACCEPTED, non-breaking, G4)
decisions/SEC/ADR-SEC-047.md — REQ-SEC-079 adds SERVICE_ACCOUNT_REACTIVATED as its own audit code, symmetric to
SERVICE_ACCOUNT_DEACTIVATED, rather than overloading an existing code (ACCEPTED, non-breaking, G6)
Carried: decisions/SEC/ADR-SEC-001.md (ACCEPTED, P2), decisions/SEC/ADR-SEC-002.md (ACCEPTED, P3.1 v1),
decisions/SEC/ADR-SEC-006.md (ACCEPTED, P3.2 v1). Applied from upstream v2: ADR-SEC-012 … ADR-SEC-035 (ADR-SEC-031
SUPERSEDED by ADR-SEC-034). BLOCKED: none.

TRACEABILITY
REQ covered by ≥1 API/DBF: 79/79. Every REQ-SEC-001 … REQ-SEC-079 is in the traces of at least one API block or one
DBF record of db-script-sec.md, including REQ-SEC-034 (API-SEC-037), REQ-SEC-035 (API-SEC-038, ADR-SEC-040),
REQ-SEC-072…077 (G5/G6, pass-1 review — API-SEC-032…036 and API-SEC-003), REQ-SEC-078 (G3, pass-1 review —
API-SEC-029, RULE-SEC-013), and REQ-SEC-079 (revise pass G6 — API-SEC-010, ADR-SEC-047).
Orphan REQ: none. 0 findings open.
Rules RULE-SEC-001 … RULE-SEC-007 as in v1; RULE-SEC-008 … RULE-SEC-013 are each enforced by one endpoint or the CORE
gate, and each rule that rejects has one catalog row.

Event
"P3.1 completed: SEC v2 — 79/79 REQ rows resolved, 39 API (+12), 54 QR (+15), 116 DBF bound, 37 catalog rows
(+7: +6 v2, +1 shared row v1 omitted, plus SEC-409-CREDENTIAL-LIMIT relabeled RULE-SEC-013 — G3), 0 XM, 9 ADRs
(ADR-SEC-036 … ADR-SEC-038, ADR-SEC-040, ADR-SEC-042 … ADR-SEC-045; ADR-SEC-039 SUPERSEDED, ADR-SEC-043 §2
SUPERSEDED), applied from P2: ADR-SEC-041, applied from P1: ADR-SEC-046, ADR-SEC-047; service-account credentials
and client-credentials token added; revise pass (G1-G7) applied — HMAC hashing, ingress throttling on API-SEC-001,
DATA_INTEGRITY_VIOLATION/404 rows reconciled across the grant/registry endpoints, service-credential pepper
bootstrap row, SERVICE_ACCOUNT_REACTIVATED audit; 0 findings open"
══════════════════════════════════════════════════════════════════
