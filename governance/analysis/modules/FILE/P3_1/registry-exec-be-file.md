# REGISTRY EXTRACT — registry-exec-be-FILE
══════════════════════════════════════════════════════════════════
Module          : File Service (FILE)
Source artifact : backend-execution-plan-FILE.md (PLAN-FILE-001)
Extracted by    : P-REG (mechanical extraction — not a governance artifact)
Status          : SESSION INPUT ONLY — not loaded as Project Instruction,
                  not a Truth Layer artifact, not subject to P4.1/P4.2 audit
══════════════════════════════════════════════════════════════════

## HEADER
Module name : File Service
Module Prefix : FILE
(ALIGN-BE status and readiness/usability are not extracted here — they remain with
backend-execution-plan-FILE.md and P4.1.)

## FIELD-ID REGISTER (DB Alignment Manifest — compact)
| FIELD-ID | DBF-ID | Plan Type | FK/XM-ID | Match Status |
|---|---|---|---|---|
| FIELD-0002/0003/0004 | DBF-0002/0003/0004 | (polymorphic) | owner ref (no FK, app-level) | ✓ |
| FIELD-0008 | DBF-0008 | byte[] | BYTEA | ✓ |
| FIELD-0011 | DBF-0011 | Long | FK FILE_CATEGORY_FK → FILE_CATEGORY | ✓ |
Note: created_by identity read of SEC_USER_ACCOUNT is XM-FILE-001 SOFT-READ — application layer,
no column FK. All other FIELD-IDs align 1:1 to their DBF-ID. 18/18 aligned.

## ERROR CATALOG (codes only)
| ERR-ID | Source RULE-ID | HTTP Status |
|---|---|---|
| ERR-0001 | RULE-FILE-001 | 413 |
| ERR-0002 | RULE-FILE-002 | 415 |
| ERR-0003 | RULE-FILE-003 | 401 |
| ERR-0004 | RULE-FILE-005 | 400 |
| ERR-0005 | RULE-FILE-007 | 409 |
| ERR-0006 | PLATFORM-STD | 404 |

## INT SUMMARY (XM execution status — Backend only)
| XM-ID | Execution Status | Blocks (API-IDs) | RXE-ID |
|---|---|---|---|
| XM-FILE-001 | READY | — | — |

## TC COVERAGE SUMMARY — BACKEND (from SECTION D, summary rows only)
| RULE-ID | Happy TC-BE-ID | Violation TC-BE-ID | Status |
|---|---|---|---|
| RULE-FILE-001 | TC-BE-FILE-001 | TC-BE-FILE-002 | COVERED |
| RULE-FILE-002 | TC-BE-FILE-003 | TC-BE-FILE-004 | COVERED |
| RULE-FILE-003 | TC-BE-FILE-005 | TC-BE-FILE-006 | COVERED |
| RULE-FILE-004 | TC-BE-FILE-007 | — | COVERED |
| RULE-FILE-005 | TC-BE-FILE-008 | TC-BE-FILE-009 | COVERED |
| RULE-FILE-006 | TC-BE-FILE-010 | — | COVERED |
| RULE-FILE-007 | TC-BE-FILE-011 | TC-BE-FILE-012 | COVERED |

## MODULE GOVERNANCE INDEX (state snapshot)
Note: MODULE GOVERNANCE INDEX section not found in source — omitted.

## FIELD-ID / API-ID / PLAN-ID NAMESPACE
FIELD-FILE : last = FIELD-0018
API-FILE   : last = API-FILE-008
RULE-FILE  : last = RULE-FILE-007
ERR-FILE   : last = ERR-0006
QR-FILE    : last = QR-FILE-0011
PLAN-FILE  : PLAN-FILE-001

---
*End of registry-exec-be-FILE.md*
