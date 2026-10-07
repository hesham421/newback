# REGISTRY EXTRACT — registry-exec-be-NOTIF
══════════════════════════════════════════════════════════════════
Module          : Notification Service (NOTIF)
Source artifact : backend-execution-plan-NOTIF.md (PLAN-NOTIF-001)
Extracted by    : P-REG (mechanical extraction — not a governance artifact)
Status          : SESSION INPUT ONLY — not loaded as Project Instruction,
                  not a Truth Layer artifact, not subject to P4.1/P4.2 audit
══════════════════════════════════════════════════════════════════

## HEADER
Module name : Notification Service
Module Prefix : NOTIF
(ALIGN-BE status and readiness/usability are not extracted here — they remain with
backend-execution-plan-NOTIF.md and P4.1.)

## FIELD-ID REGISTER (DB Alignment Manifest — compact)
| FIELD-ID | DBF-ID | Plan Type | FK/XM-ID | Match Status |
|---|---|---|---|---|
| FIELD-0002 | DBF-0002 | Long | XM-NOTIF-001 SOFT-READ → SEC_USER_ACCOUNT | ✓ (no FK) |
| FIELD-0011 | DBF-0011 | Long | FK TEMPLATE_FK → NOTIF_TEMPLATE | ✓ |
| FIELD-0020 | DBF-0020 | Long | XM-NOTIF-002 SOFT-READ → FILE_DOCUMENT | ✓ (no FK) |
All other FIELD-IDs align 1:1 to their DBF-ID. 25/25 aligned.

## ERROR CATALOG (codes only)
| ERR-ID | Source RULE-ID | HTTP Status |
|---|---|---|
| ERR-0001 | RULE-NOTIF-004 | 400 |
| ERR-0002 | RULE-NOTIF-006 | 409 |
| ERR-0003 | RULE-NOTIF-006 | 409 |
| ERR-0004 | PLATFORM-STD | 404 |

## INT SUMMARY (XM execution status — Backend only)
| XM-ID | Execution Status | Blocks (API-IDs) | RXE-ID |
|---|---|---|---|
| XM-NOTIF-001 | READY | — | — |
| XM-NOTIF-002 | READY | — | — |

## TC COVERAGE SUMMARY — BACKEND (from SECTION D, summary rows only)
| RULE-ID | Happy TC-BE-ID | Violation TC-BE-ID | Status |
|---|---|---|---|
| RULE-NOTIF-001 | TC-BE-NOTIF-001 | — | COVERED |
| RULE-NOTIF-002 | TC-BE-NOTIF-002 | TC-BE-NOTIF-003 | COVERED |
| RULE-NOTIF-003 | TC-BE-NOTIF-004 | — | COVERED |
| RULE-NOTIF-004 | TC-BE-NOTIF-005 | TC-BE-NOTIF-006 | COVERED |
| RULE-NOTIF-005 | TC-BE-NOTIF-007 | — | COVERED |
| RULE-NOTIF-006 | TC-BE-NOTIF-008 | TC-BE-NOTIF-009 | COVERED |
| RULE-NOTIF-007 | TC-BE-NOTIF-010 | — | COVERED |

## MODULE GOVERNANCE INDEX (state snapshot)
Note: MODULE GOVERNANCE INDEX section not found in source — omitted.

## FIELD-ID / API-ID / PLAN-ID NAMESPACE
FIELD-NOTIF : last = FIELD-0025
API-NOTIF   : last = API-NOTIF-006
RULE-NOTIF  : last = RULE-NOTIF-007
ERR-NOTIF   : last = ERR-0004
QR-NOTIF    : last = QR-NOTIF-0013
PLAN-NOTIF  : PLAN-NOTIF-001

---
*End of registry-exec-be-NOTIF.md*
