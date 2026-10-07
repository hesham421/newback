## REGISTRY — P2 — SEC v2
══════════════════════════════════════════════════════════════════
Delta : v2 on v1 (CS-SEC-001, ADDITIVE). This file is the complete current registry (v1 + v2),
        so the fold does not need to carry any v1 line.

Tables
| Table | ENT id | Kind | DBF range | PK sequence |
|---|---|---|---|---|
| SEC_USER | ENT-SEC-001 | security | DBF-SEC-001 … DBF-SEC-013, DBF-SEC-105 (v2: principal_type_code) | SEQ_SEC_USER |
| SEC_ROLE | ENT-SEC-002 | security | DBF-SEC-014 … DBF-SEC-024 | SEQ_SEC_ROLE |
| SEC_USER_ROLE | ENT-SEC-003 | security | DBF-SEC-025 … DBF-SEC-029 | SEQ_SEC_USER_ROLE |
| SEC_MODULE_REG | ENT-SEC-004 | security | DBF-SEC-030 … DBF-SEC-038 | SEQ_SEC_MODULE_REG |
| SEC_SCREEN_REG | ENT-SEC-005 | security | DBF-SEC-039 … DBF-SEC-048 | SEQ_SEC_SCREEN_REG |
| SEC_ACTION_REG | ENT-SEC-006 | security | DBF-SEC-049 … DBF-SEC-059 | SEQ_SEC_ACTION_REG |
| SEC_ROLE_MODULE_GRANT | ENT-SEC-007 | security | DBF-SEC-060 … DBF-SEC-064 | SEQ_SEC_ROLE_MODULE_GRANT |
| SEC_ROLE_SCREEN_GRANT | ENT-SEC-008 | security | DBF-SEC-065 … DBF-SEC-069 | SEQ_SEC_ROLE_SCREEN_GRANT |
| SEC_ROLE_ACTION_GRANT | ENT-SEC-009 | security | DBF-SEC-070 … DBF-SEC-074 | SEQ_SEC_ROLE_ACTION_GRANT |
| SEC_ACTIVE_SESSION | ENT-SEC-010 | security | DBF-SEC-075 … DBF-SEC-082 | SEQ_SEC_ACTIVE_SESSION |
| SEC_AUDIT_LOG | ENT-SEC-011 | security | DBF-SEC-083 … DBF-SEC-090 | SEQ_SEC_AUDIT_LOG |
| SEC_PWD_RESET_TOKEN | ENT-SEC-012 | security | DBF-SEC-091 … DBF-SEC-096 | SEQ_SEC_PWD_RESET_TOKEN |
| SEC_SIGNUP_REQUEST | ENT-SEC-013 | security | DBF-SEC-097 … DBF-SEC-104 | SEQ_SEC_SIGNUP_REQUEST |
| SEC_SVC_ACCOUNT_CRED | ENT-SEC-014 | security | DBF-SEC-106 … DBF-SEC-116 (v2 ADDED) | SEQ_SEC_SVC_ACCOUNT_CRED |

PK generation: `sequence` on every table (profile.stack.db.pk_generation). v2 moves the 13 v1
tables from identity columns onto the sequences above (see Decisions). P3.1 carries each sequence
name into the plan.

DBF ids (full detail in db-script-sec.md → §1): DBF-SEC-001, DBF-SEC-002, DBF-SEC-003,
DBF-SEC-004, DBF-SEC-005, DBF-SEC-006, DBF-SEC-007, DBF-SEC-008, DBF-SEC-009, DBF-SEC-010,
DBF-SEC-011, DBF-SEC-012, DBF-SEC-013, DBF-SEC-014, DBF-SEC-015, DBF-SEC-016, DBF-SEC-017,
DBF-SEC-018, DBF-SEC-019, DBF-SEC-020, DBF-SEC-021, DBF-SEC-022, DBF-SEC-023, DBF-SEC-024,
DBF-SEC-025, DBF-SEC-026, DBF-SEC-027, DBF-SEC-028, DBF-SEC-029, DBF-SEC-030, DBF-SEC-031,
DBF-SEC-032, DBF-SEC-033, DBF-SEC-034, DBF-SEC-035, DBF-SEC-036, DBF-SEC-037, DBF-SEC-038,
DBF-SEC-039, DBF-SEC-040, DBF-SEC-041, DBF-SEC-042, DBF-SEC-043, DBF-SEC-044, DBF-SEC-045,
DBF-SEC-046, DBF-SEC-047, DBF-SEC-048, DBF-SEC-049, DBF-SEC-050, DBF-SEC-051, DBF-SEC-052,
DBF-SEC-053, DBF-SEC-054, DBF-SEC-055, DBF-SEC-056, DBF-SEC-057, DBF-SEC-058, DBF-SEC-059,
DBF-SEC-060, DBF-SEC-061, DBF-SEC-062, DBF-SEC-063, DBF-SEC-064, DBF-SEC-065, DBF-SEC-066,
DBF-SEC-067, DBF-SEC-068, DBF-SEC-069, DBF-SEC-070, DBF-SEC-071, DBF-SEC-072, DBF-SEC-073,
DBF-SEC-074, DBF-SEC-075, DBF-SEC-076, DBF-SEC-077, DBF-SEC-078, DBF-SEC-079, DBF-SEC-080,
DBF-SEC-081, DBF-SEC-082, DBF-SEC-083, DBF-SEC-084, DBF-SEC-085, DBF-SEC-086, DBF-SEC-087,
DBF-SEC-088, DBF-SEC-089, DBF-SEC-090, DBF-SEC-091, DBF-SEC-092, DBF-SEC-093, DBF-SEC-094,
DBF-SEC-095, DBF-SEC-096, DBF-SEC-097, DBF-SEC-098, DBF-SEC-099, DBF-SEC-100, DBF-SEC-101,
DBF-SEC-102, DBF-SEC-103, DBF-SEC-104, DBF-SEC-105, DBF-SEC-106, DBF-SEC-107, DBF-SEC-108,
DBF-SEC-109, DBF-SEC-110, DBF-SEC-111, DBF-SEC-112, DBF-SEC-113, DBF-SEC-114, DBF-SEC-115, DBF-SEC-116

v2 delta — ADDED: DBF-SEC-105 … DBF-SEC-116 (12) · MODIFIED: DBF-SEC-001, DBF-SEC-002,
DBF-SEC-003, DBF-SEC-004, DBF-SEC-007, DBF-SEC-014, DBF-SEC-025, DBF-SEC-030, DBF-SEC-039,
DBF-SEC-049, DBF-SEC-060, DBF-SEC-065, DBF-SEC-070, DBF-SEC-075, DBF-SEC-083, DBF-SEC-084,
DBF-SEC-091, DBF-SEC-097 (18) · REMOVED: none

XM index
none — SEC is ROOT (→ dependency index: no row this module). v2: unchanged. The Oracle event
consumer is an external caller that authenticates as a SEC service account, not a platform module.

Lookups
| Key | Seeded values count | Owner |
|---|---|---|
| USER_STATUS | 3 (CHECK-constrained, not seeded rows — ADR-SEC-001) | SEC |
| SIGNUP_STATUS | 3 (CHECK-constrained, not seeded rows — ADR-SEC-001) | SEC |
| AUDIT_EVENT_TYPE | 21 (v2: +7 SERVICE_* codes — SERVICE_ACCOUNT_REACTIVATED added by the pass-1 REVISE review, RG6/ADR-SEC-047; CHECK-constrained, not seeded rows — ADR-SEC-001) | SEC |
| PRINCIPAL_TYPE | 2 (v2 ADDED: HUMAN, SERVICE; CHECK-constrained, not seeded rows — ADR-SEC-001) | SEC |

Indexes (non-PK, non-FK-support)
| Index | Table | Kind | Guards |
|---|---|---|---|
| IDX_SEC_USER_PRINCIPAL_TYPE | SEC_USER | plain | search/filter by principal type (v2) |
| IDX_SEC_SVC_ACCOUNT_CRED_USER | SEC_SVC_ACCOUNT_CRED | plain | FK support (v2) |
| UQ_SEC_SIGNUP_REQUEST_PENDING_EMAIL | SEC_SIGNUP_REQUEST | partial unique (`WHERE status_code='PENDING'`) | RULE guard against two concurrent PENDING sign-ups for one email (v2, ADR-SEC-041) |

Sequences
Last DBF: DBF-SEC-116 · Last XM: none assigned (0 rows)

Decisions
ADR-SEC-001 (ACCEPTED, non-breaking; continued in v2 for PRINCIPAL_TYPE and the extended AUDIT_EVENT_TYPE) ·
ADR-SEC-035 (ACCEPTED, non-breaking; v2 — PK generation moved to named sequences on all 14 tables; Consequences
amended by the pass-1 review to lock all 13 v1 tables ACCESS EXCLUSIVE before setval/DROP IDENTITY, G3) ·
ADR-SEC-041 (ACCEPTED, non-breaking, ADDITIVE; v2 — UQ_SEC_SIGNUP_REQUEST_PENDING_EMAIL, a guarded partial unique
index, closes the concurrent-PENDING-signup race API-SEC-002 flagged; G1) ·
Applied from P1 (pass-1 REVISE review): ADR-SEC-047 (ACCEPTED, non-breaking; v2 — CHK_SEC_AUDIT_LOG_EVENT_TYPE
carries SERVICE_ACCOUNT_REACTIVATED, closing the unaudited-reactivation gap G6 found; REQ-SEC-079) ·
— see analysis/decisions/SEC/.
This registry previously withdrew a claimed UQ_SEC_SIGNUP_REQUEST_PENDING_EMAIL index, citing an ADR id that was
never written and stating the race was left as a v1 defect. The pass-1 review (G1) built the index for real, under
ADR-SEC-041: db-script §3 BLOCK 5 now creates it, guarded by a pre-check that aborts the migration on existing
duplicate PENDING rows rather than half-failing partway through. API-SEC-002's Concurrency line now names this
index as the guarantee, matching API-SEC-006's pattern (the refused insert answers the shared DATA_INTEGRITY_VIOLATION).
No BLOCKED decision.

Event
"P2 completed: SEC v2 — 14 tables, 116 DBF, 0 XM, 1 new index (UQ_SEC_SIGNUP_REQUEST_PENDING_EMAIL, G1); revise pass
applied ADR-SEC-047 — CHK_SEC_AUDIT_LOG_EVENT_TYPE now 21 codes (+SERVICE_ACCOUNT_REACTIVATED, RG6)"

Cascade
No registry XM row anywhere in the platform targets SEC with status DEFERRED: SEC is ROOT, and
consumers reach it through the platform interceptor and the `com.erp.sec.crossmodule` surface, not
through a deferred FK. The new table SEC_SVC_ACCOUNT_CRED is PRIVATE, so no other module can hold
an FK to it. Nothing to resolve.
══════════════════════════════════════════════════════════════════
