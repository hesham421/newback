# REGISTRY EXTRACT — registry-test-be-NOTIF
══════════════════════════════════════════════════════════════════
Module          : Notification Service (NOTIF)
Source artifact : backend-test-plan-NOTIF.md (PLAN-NOTIF-001)
Extracted by    : P-REG (mechanical extraction — not a governance artifact)
Status          : SESSION INPUT ONLY — not loaded as Project Instruction,
                  not a Truth Layer artifact, not subject to P4.1/P4.2 audit
══════════════════════════════════════════════════════════════════

## HEADER
Module name : Notification Service
Module Prefix : NOTIF

## TC-BE-ID REGISTER
| TC-BE-NOTIF-ID | Covers | Data-Class |
|---|---|---|
| TC-BE-NOTIF-001 | RULE-NOTIF-001 | VALID |
| TC-BE-NOTIF-002 | RULE-NOTIF-002 | VALID |
| TC-BE-NOTIF-003 | RULE-NOTIF-002 | BOUNDARY |
| TC-BE-NOTIF-004 | RULE-NOTIF-003 | EDGE_CASE |
| TC-BE-NOTIF-005 | RULE-NOTIF-004 | VALID |
| TC-BE-NOTIF-006 | RULE-NOTIF-004 / ERR-0001 | INVALID |
| TC-BE-NOTIF-007 | RULE-NOTIF-005 | ATTACK |
| TC-BE-NOTIF-008 | RULE-NOTIF-006 | VALID |
| TC-BE-NOTIF-009 | RULE-NOTIF-006 / ERR-0002, ERR-0003 | INVALID |
| TC-BE-NOTIF-010 | RULE-NOTIF-007 | VALID |
| TC-BE-NOTIF-011 | API-NOTIF-001 | VALID |
| TC-BE-NOTIF-012 | API-NOTIF-002 | VALID/EDGE_CASE |
| TC-BE-NOTIF-013 | API-NOTIF-003 / ERR-0004 | VALID |
| TC-BE-NOTIF-014 | API-NOTIF-004 | VALID |
| TC-BE-NOTIF-015 | API-NOTIF-005 | VALID |
| TC-BE-NOTIF-016 | API-NOTIF-006 | VALID/ATTACK |

## TC TRACEABILITY INDEX (compact, backend)
| RULE-ID/API-ID/ERR-ID | TC-BE-NOTIF-IDs |
|---|---|
| RULE-NOTIF-001 | TC-BE-NOTIF-001 |
| RULE-NOTIF-002 | TC-BE-NOTIF-002, 003 |
| RULE-NOTIF-003 | TC-BE-NOTIF-004 |
| RULE-NOTIF-004 | TC-BE-NOTIF-005, 006 |
| RULE-NOTIF-005 | TC-BE-NOTIF-007 |
| RULE-NOTIF-006 | TC-BE-NOTIF-008, 009 |
| RULE-NOTIF-007 | TC-BE-NOTIF-010 |
| API-NOTIF-001..006 | TC-BE-NOTIF-011..016 (1:1 sequential) |
| ERR-0001 | TC-BE-NOTIF-006 |
| ERR-0002 | TC-BE-NOTIF-009 |
| ERR-0003 | TC-BE-NOTIF-009 |
| ERR-0004 | TC-BE-NOTIF-013 |

## XM MOCK STRATEGY REGISTER
Note: no XM Mock Strategy Register section found in source — omitted.

## LAST ASSIGNED TC-BE SEQUENCE
TC-BE-NOTIF: last = TC-BE-NOTIF-016

---
*End of registry-test-be-NOTIF.md*
