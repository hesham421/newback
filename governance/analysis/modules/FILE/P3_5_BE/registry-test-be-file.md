# REGISTRY EXTRACT — registry-test-be-FILE
══════════════════════════════════════════════════════════════════
Module          : File Service (FILE)
Source artifact : backend-test-plan-FILE.md (PLAN-FILE-001)
Extracted by    : P-REG (mechanical extraction — not a governance artifact)
Status          : SESSION INPUT ONLY — not loaded as Project Instruction,
                  not a Truth Layer artifact, not subject to P4.1/P4.2 audit
══════════════════════════════════════════════════════════════════

## HEADER
Module name : File Service
Module Prefix : FILE

## TC-BE-ID REGISTER
| TC-BE-FILE-ID | Covers | Data-Class |
|---|---|---|
| TC-BE-FILE-001 | RULE-FILE-001 | VALID |
| TC-BE-FILE-002 | RULE-FILE-001 / ERR-0001 | BOUNDARY |
| TC-BE-FILE-003 | RULE-FILE-002 | VALID |
| TC-BE-FILE-004 | RULE-FILE-002 / ERR-0002 | INVALID |
| TC-BE-FILE-005 | RULE-FILE-003 | VALID |
| TC-BE-FILE-006 | RULE-FILE-003 / ERR-0003 | INVALID |
| TC-BE-FILE-007 | RULE-FILE-004 | ATTACK |
| TC-BE-FILE-008 | RULE-FILE-005 | VALID |
| TC-BE-FILE-009 | RULE-FILE-005 / ERR-0004 | INVALID |
| TC-BE-FILE-010 | RULE-FILE-006 | VALID |
| TC-BE-FILE-011 | RULE-FILE-007 | VALID |
| TC-BE-FILE-012 | RULE-FILE-007 / ERR-0005 | INVALID |
| TC-BE-FILE-013 | API-FILE-001 | VALID |
| TC-BE-FILE-014 | API-FILE-002 | VALID |
| TC-BE-FILE-015 | API-FILE-003 | VALID |
| TC-BE-FILE-016 | API-FILE-004 | VALID |
| TC-BE-FILE-017 | API-FILE-005 | VALID/EDGE_CASE |
| TC-BE-FILE-018 | API-FILE-006 | VALID |
| TC-BE-FILE-019 | API-FILE-007 | VALID |
| TC-BE-FILE-020 | API-FILE-008 | VALID/ATTACK |

## TC TRACEABILITY INDEX (compact, backend)
| RULE-ID/API-ID/ERR-ID | TC-BE-FILE-IDs |
|---|---|
| RULE-FILE-001 | TC-BE-FILE-001, 002 |
| RULE-FILE-002 | TC-BE-FILE-003, 004 |
| RULE-FILE-003 | TC-BE-FILE-005, 006 |
| RULE-FILE-004 | TC-BE-FILE-007 |
| RULE-FILE-005 | TC-BE-FILE-008, 009 |
| RULE-FILE-006 | TC-BE-FILE-010 |
| RULE-FILE-007 | TC-BE-FILE-011, 012 |
| API-FILE-001..008 | TC-BE-FILE-013..020 (1:1 sequential) |
| ERR-0001 | TC-BE-FILE-002 |
| ERR-0002 | TC-BE-FILE-004 |
| ERR-0003 | TC-BE-FILE-006 |
| ERR-0004 | TC-BE-FILE-009 |
| ERR-0005 | TC-BE-FILE-012 |
| ERR-0006 | 404 paths across TC-BE-FILE-016/018/019/020 |

## XM MOCK STRATEGY REGISTER
Note: no XM Mock Strategy Register section found in source — omitted.

## LAST ASSIGNED TC-BE SEQUENCE
TC-BE-FILE: last = TC-BE-FILE-020

---
*End of registry-test-be-FILE.md*
