# test-execution-manifest.md — Notification Service (NOTIF) — PLAN-ID: PLAN-NOTIF-001
══════════════════════════════════════════════════════════════════
Derived view (CONTRACT-13) — no new RULE/ERR/TC. Consumed by Project 5.
Source: backend-execution-plan-NOTIF.md + backend-test-plan-NOTIF.md (ALIGN-BE ✓) | DB_TARGET: POSTGRESQL_16
══════════════════════════════════════════════════════════════════

SECTION: DEPENDENCY ORDER (topological — intra-module FK)
  1. NOTIF_TEMPLATE (parent; independent)
  2. NOTIF_CHANNEL_CONFIG (parent; independent)
  3. NOTIF_LOG (FK TEMPLATE_FK → NOTIF_TEMPLATE)
  Cross-module (must be available first — order CU→SEC→FILE→NOTIF):
    XM-NOTIF-001 SOFT-READ → SEC_USER_ACCOUNT (recipient identity/active check)
    XM-NOTIF-002 SOFT-READ → FILE_DOCUMENT via FileService (optional attachment)

SECTION: RULE→ERR→TC TRIPLES
  RULE-ID        │ ERR-ID   │ TC-BE-ID          │ HTTP
  ───────────────┼──────────┼───────────────────┼──────
  RULE-NOTIF-004 │ ERR-0001 │ TC-BE-NOTIF-006   │ 400
  RULE-NOTIF-006 │ ERR-0002 │ TC-BE-NOTIF-009   │ 409 (template)
  RULE-NOTIF-006 │ ERR-0003 │ TC-BE-NOTIF-009   │ 409 (channel)
  (PLATFORM)     │ ERR-0004 │ TC-BE-NOTIF-013   │ 404
  Behavioral rules (no ERR — assert log status): RULE-NOTIF-001 (fan-out) TC-001 · RULE-NOTIF-002 (retry/FAILED) TC-002/003 ·
    RULE-NOTIF-003 (CHANNEL_DISABLED) TC-004 · RULE-NOTIF-005 (auth delegation) TC-007 · RULE-NOTIF-007 (skip inactive) TC-010.

SECTION: ENTITY CRUD CHECKLIST
  Entity                     │ Create │ Search │ Update │ Enable/Disable │ Deactivate │ GetById │ Delete
  ───────────────────────────┼────────┼────────┼────────┼────────────────┼────────────┼─────────┼───────
  NotificationTemplate       │   ✓    │   ✓    │   ✓    │      —         │   (flag)   │   ✓     │   —
  NotificationChannelConfig  │   ✓    │   ✓    │   ✓    │      ✓         │     —      │   ✓     │   —
  NotificationLog            │ fan-out│   ✓    │ status │      —         │     —      │   ✓     │   — (read-only screen)
══════════════════════════════════════════════════════════════════
Regeneration rule: regenerate this manifest if either source plan is amended, before handing to Project 5.
══════════════════════════════════════════════════════════════════

*End of test-execution-manifest.md — NOTIF — PLAN-NOTIF-001*
