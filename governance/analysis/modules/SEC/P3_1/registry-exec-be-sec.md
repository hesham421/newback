## REGISTRY — P3.1 — SEC v1
══════════════════════════════════════════════════════════════════

ID RANGES
API-SEC-001 .. API-SEC-027 · QR-SEC-001 .. QR-SEC-038

API ids: API-SEC-001, API-SEC-002, API-SEC-003, API-SEC-004, API-SEC-005, API-SEC-006,
API-SEC-007, API-SEC-008, API-SEC-009, API-SEC-010, API-SEC-011, API-SEC-012, API-SEC-013,
API-SEC-014, API-SEC-015, API-SEC-016, API-SEC-017, API-SEC-018, API-SEC-019, API-SEC-020,
API-SEC-021, API-SEC-022, API-SEC-023, API-SEC-024, API-SEC-025, API-SEC-026, API-SEC-027

QR ids: QR-SEC-001, QR-SEC-002, QR-SEC-003, QR-SEC-004, QR-SEC-005, QR-SEC-006, QR-SEC-007,
QR-SEC-008, QR-SEC-009, QR-SEC-010, QR-SEC-011, QR-SEC-012, QR-SEC-013, QR-SEC-014,
QR-SEC-015, QR-SEC-016, QR-SEC-017, QR-SEC-018, QR-SEC-019, QR-SEC-020, QR-SEC-021,
QR-SEC-022, QR-SEC-023, QR-SEC-024, QR-SEC-025, QR-SEC-026, QR-SEC-027, QR-SEC-028,
QR-SEC-029, QR-SEC-030, QR-SEC-031, QR-SEC-032, QR-SEC-033, QR-SEC-034, QR-SEC-035,
QR-SEC-036, QR-SEC-037, QR-SEC-038

ENTITIES / TABLES bound
All 13 ENT-SEC-001..013 bound to their db-script tables (SEC_USER … SEC_SIGNUP_REQUEST);
lookups reused (none pre-existing to reuse — SEC is first through the pipeline): new keys
USER_STATUS, SIGNUP_STATUS, AUDIT_EVENT_TYPE (carried from P2, ADR-SEC-001).

XM STATUS
open: none · deferred: none — SEC is ROOT, 0 XM rows.

CATALOG
26 code count (excluding the generic INTERNAL_ERROR/SEC-400-INVALID-SORT which apply
platform-wide) + 2 generic = 28 total error-catalog rows. Rules without a message: none — every RULE-SEC-001
through RULE-SEC-007 that produces a user-facing message has a full ar/en pair (SRS A5,
carried verbatim). Rules needing an ADR for their catalog treatment: none (all 7 SEC RULEs
map directly); PLATFORM-STD rows: ADR-SEC-002.

ALIGN
PASSED ✓ · 0 findings (see backend-execution-plan-sec.md → Alignment self-check).

ADRs
erp/decisions/SEC/ADR-SEC-001.md (ACCEPTED, carried from P2) ·
erp/decisions/SEC/ADR-SEC-002.md (ACCEPTED, new this stage)

TRACEABILITY
REQ covered by ≥1 API/DBF: 33/33 (every REQ-SEC-001..033 appears in ≥1 API block's traces=
or ≥1 DBF record's traces in db-script-sec.md — see backend-execution-plan-sec.md → ALIGN
TRACEABILITY line). Orphan REQ: none.

Event
"P3.1 completed: SEC v1 — 27 API, 38 QR, ALIGN PASSED, 2 ADRs"
══════════════════════════════════════════════════════════════════
