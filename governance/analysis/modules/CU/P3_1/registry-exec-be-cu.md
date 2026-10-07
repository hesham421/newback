# REGISTRY EXTRACT — registry-exec-be-CU
══════════════════════════════════════════════════════════════════
Module          : Common Utils (CU)
Source artifact : backend-execution-plan-CU.md (PLAN-CU-001)
Extracted by    : P-REG (mechanical extraction — not a governance artifact)
Status          : SESSION INPUT ONLY — not loaded as Project Instruction,
                  not a Truth Layer artifact, not subject to P4.1/P4.2 audit
══════════════════════════════════════════════════════════════════

## HEADER
Module name : Common Utils
Module Prefix : CU
(ALIGN-BE status and readiness/usability are not extracted here — they remain with
backend-execution-plan-CU.md and P4.1.)

## FIELD-ID REGISTER (DB Alignment Manifest — compact)
| FIELD-ID | DBF-ID | Plan Type | FK/XM-ID | Match Status |
|---|---|---|---|---|
| FIELD-0001 | DBF-0001 | Long | — | ✓ |
| FIELD-0002 | DBF-0002 | String(150) | — | ✓ |
| FIELD-0003 | DBF-0003 | String(text) | — | ✓ |
| FIELD-0004 | DBF-0004 | String(2000) | — | ✓ |
| FIELD-0005 | DBF-0005 | Boolean | — | ✓ |
Audit FIELD-0006..0009 (createdBy/At, updatedBy/At): no DBF-ID by convention.

## ERROR CATALOG (codes only)
| ERR-ID | Source RULE-ID | HTTP Status |
|---|---|---|
| ERR-0001 | RULE-CU-001 | 409 |
| ERR-0002 | RULE-CU-002 | 400 |
| ERR-0003 | RULE-CU-003 | 422 |
| ERR-0004 | PLATFORM-STD | 404 |

## INT SUMMARY (XM execution status — Backend only)
| XM-ID | Execution Status | Blocks (API-IDs) | RXE-ID |
|---|---|---|---|
| (none) | — | — | — |
CU is a ROOT module — no cross-module runtime dependencies.

## TC COVERAGE SUMMARY — BACKEND (from SECTION D, summary rows only)
| RULE-ID | Happy TC-BE-ID | Violation TC-BE-ID | Status |
|---|---|---|---|
| RULE-CU-001 | TC-BE-CU-001 | TC-BE-CU-002 | COVERED |
| RULE-CU-002 | TC-BE-CU-003 | TC-BE-CU-004 | COVERED |
| RULE-CU-003 | TC-BE-CU-005 | TC-BE-CU-006 | COVERED |

## MODULE GOVERNANCE INDEX (state snapshot)
Note: MODULE GOVERNANCE INDEX section not found in source — omitted.

## FIELD-ID / API-ID / PLAN-ID NAMESPACE
FIELD-CU : last = FIELD-0009 (FIELD-0006..0009 are audit fields, no DBF-ID)
API-CU   : last = API-CU-005
RULE-CU  : last = RULE-CU-003
ERR-CU   : last = ERR-0004
QR-CU    : last = QR-CU-0006
PLAN-CU  : PLAN-CU-001

---
*End of registry-exec-be-CU.md*
