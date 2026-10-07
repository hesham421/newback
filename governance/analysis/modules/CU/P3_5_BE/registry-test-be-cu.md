# REGISTRY EXTRACT — registry-test-be-CU
══════════════════════════════════════════════════════════════════
Module          : Common Utils (CU)
Source artifact : backend-test-plan-CU.md (PLAN-CU-001)
Extracted by    : P-REG (mechanical extraction — not a governance artifact)
Status          : SESSION INPUT ONLY — not loaded as Project Instruction,
                  not a Truth Layer artifact, not subject to P4.1/P4.2 audit
══════════════════════════════════════════════════════════════════

## HEADER
Module name : Common Utils
Module Prefix : CU

## TC-BE-ID REGISTER
| TC-BE-CU-ID | Covers | Data-Class |
|---|---|---|
| TC-BE-CU-001 | API-CU-001 / RULE-CU-001,002 | VALID |
| TC-BE-CU-002 | RULE-CU-001 / ERR-0001 | INVALID |
| TC-BE-CU-003 | RULE-CU-002 | VALID |
| TC-BE-CU-004 | RULE-CU-002 / ERR-0002 | INVALID |
| TC-BE-CU-005 | RULE-CU-003 | VALID |
| TC-BE-CU-006 | RULE-CU-003 / ERR-0003 | INVALID |
| TC-BE-CU-007 | API-CU-002 | VALID |
| TC-BE-CU-008 | API-CU-003 | VALID |
| TC-BE-CU-009 | API-CU-004 | VALID |
| TC-BE-CU-010 | API-CU-005 | VALID |
| TC-BE-CU-011 | API-CU-005 / ERR-0004 | INVALID |
| TC-BE-CU-012 | API-CU-002 | EDGE_CASE |
| TC-BE-CU-013 | API-CU-001 | ATTACK |

## TC TRACEABILITY INDEX (compact, backend)
| RULE-ID/API-ID/ERR-ID | TC-BE-CU-IDs |
|---|---|
| RULE-CU-001 | TC-BE-CU-001, 002 |
| RULE-CU-002 | TC-BE-CU-003, 004 |
| RULE-CU-003 | TC-BE-CU-005, 006 |
| API-CU-001 | TC-BE-CU-001 |
| API-CU-002 | TC-BE-CU-007, 012 |
| API-CU-003 | TC-BE-CU-008 |
| API-CU-004 | TC-BE-CU-009 |
| API-CU-005 | TC-BE-CU-010, 011 |
| ERR-0001 | TC-BE-CU-002 |
| ERR-0002 | TC-BE-CU-004 |
| ERR-0003 | TC-BE-CU-006 |
| ERR-0004 | TC-BE-CU-011 |

## XM MOCK STRATEGY REGISTER
None — CU has no XM dependencies.

## LAST ASSIGNED TC-BE SEQUENCE
TC-BE-CU: last = TC-BE-CU-013

---
*End of registry-test-be-CU.md*
