# test-execution-manifest.md — Common Utils (CU) — PLAN-ID: PLAN-CU-001
══════════════════════════════════════════════════════════════════
Derived view (CONTRACT-13) — introduces no new RULE/ERR/TC. Consumed by Project 5.
Source: backend-execution-plan-CU.md + backend-test-plan-CU.md (ALIGN-BE ✓)
DB_TARGET: POSTGRESQL_16
══════════════════════════════════════════════════════════════════

SECTION: DEPENDENCY ORDER
  1. AppConfiguration (ENTITY-CU-001) — root entity, no parent, no XM dependency.
  (CU is the ROOT cross-cutting module; single entity, no build ordering needed.)

SECTION: RULE→ERR→TC TRIPLES
  RULE-ID      │ ERR-ID   │ TC-BE-ID       │ HTTP
  ─────────────┼──────────┼────────────────┼──────
  RULE-CU-001  │ ERR-0001 │ TC-BE-CU-002   │ 409
  RULE-CU-002  │ ERR-0002 │ TC-BE-CU-004   │ 400
  RULE-CU-003  │ ERR-0003 │ TC-BE-CU-006   │ 422
  (PLATFORM)   │ ERR-0004 │ TC-BE-CU-011   │ 404

SECTION: ENTITY CRUD CHECKLIST
  Entity            │ Create │ Search │ Update │ Activate │ Deactivate │ GetById │ Delete
  ──────────────────┼────────┼────────┼────────┼──────────┼────────────┼─────────┼───────
  AppConfiguration  │   ✓    │   ✓    │   ✓    │ (update) │     ✓      │  ✓(key) │   —
══════════════════════════════════════════════════════════════════
Regeneration rule: if either source plan is amended, regenerate this manifest
in the same session before handing to Project 5.
══════════════════════════════════════════════════════════════════

*End of test-execution-manifest.md — CU — PLAN-CU-001*
