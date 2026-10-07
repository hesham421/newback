# test-execution-manifest.md — File Service (FILE) — PLAN-ID: PLAN-FILE-001
══════════════════════════════════════════════════════════════════
Derived view (CONTRACT-13) — no new RULE/ERR/TC. Consumed by Project 5.
Source: backend-execution-plan-FILE.md + backend-test-plan-FILE.md (ALIGN-BE ✓) | DB_TARGET: POSTGRESQL_16
══════════════════════════════════════════════════════════════════

SECTION: DEPENDENCY ORDER (topological — intra-module FK)
  1. FILE_CATEGORY (parent; independent)
  2. FILE_DOCUMENT (FK FILE_CATEGORY_FK → FILE_CATEGORY; optional)
  Cross-module: XM-FILE-001 SOFT-READ → SEC_USER_ACCOUNT (SEC must be built/available first — dependency order CU→SEC→FILE).

SECTION: RULE→ERR→TC TRIPLES
  RULE-ID       │ ERR-ID   │ TC-BE-ID        │ HTTP
  ──────────────┼──────────┼─────────────────┼──────
  RULE-FILE-001 │ ERR-0001 │ TC-BE-FILE-002  │ 413
  RULE-FILE-002 │ ERR-0002 │ TC-BE-FILE-004  │ 415
  RULE-FILE-003 │ ERR-0003 │ TC-BE-FILE-006  │ 401
  RULE-FILE-005 │ ERR-0004 │ TC-BE-FILE-009  │ 400
  RULE-FILE-007 │ ERR-0005 │ TC-BE-FILE-012  │ 409
  (PLATFORM)    │ ERR-0006 │ TC-BE-FILE-016  │ 404
  (RULE-FILE-004 auth-delegation, RULE-FILE-006 soft-delete: no ERR — TC-BE-FILE-007/010 assert behavior)

SECTION: ENTITY CRUD CHECKLIST
  Entity        │ Create │ Search │ Update │ Activate │ Deactivate │ GetById │ Delete
  ──────────────┼────────┼────────┼────────┼──────────┼────────────┼─────────┼───────
  FileCategory  │   ✓    │   ✓    │   ✓    │  (flag)  │   (flag)   │   ✓     │   —
  FileDocument  │ upload │   ✓    │ archive│    —     │ soft-del   │  ✓/meta │  soft (status=DELETED)
══════════════════════════════════════════════════════════════════
Regeneration rule: regenerate this manifest if either source plan is amended, before handing to Project 5.
══════════════════════════════════════════════════════════════════

*End of test-execution-manifest.md — FILE — PLAN-FILE-001*
