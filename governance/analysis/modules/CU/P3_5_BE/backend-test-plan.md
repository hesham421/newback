<!-- backend-test-plan.md — Governed by Execution Plan Governance Engine (Project 3.1) | JUnit -->

# backend-test-plan.md — Common Utils (CU) — PLAN-ID: PLAN-CU-001
══════════════════════════════════════════════════════════════════
Source artifacts:
  backend-execution-plan.md : PLAN-CU-001 — Gate ALIGN-BE ✓ confirmed
  srs.md                    : srs-CU.md (CU-001)
  db-script.md              : db-script-CU.md (DBS-CU-001)
DB_TARGET: POSTGRESQL_16
Open Questions: None
TARGET TC COUNT: (RULE 3 × 2) + (API 5 × 1) + mandatory = ~13 (within 15–25 ceiling)
══════════════════════════════════════════════════════════════════

<!-- PHASE:TEST-PLAN-BE:START -->

  <!-- SUB:RULE-SCENARIOS:START -->

  <!-- TC:TC-BE-CU-001:START -->
TC-BE-CU-001 — Create configuration (happy path)
  API-ID: API-CU-001 | RULE-ID: RULE-CU-001/002 | ERR-ID: — | Data class: VALID
  Given : no configuration with key "mail.smtp.host" exists; valid payload
  When  : POST /api/v1/common/configurations { configKey:"mail.smtp.host", configValue:"smtp.local" }
  Then  : HTTP 201; response has appConfigurationPk, isActiveFl=true; configKey NOT echoed from client PK
  <!-- TC:TC-BE-CU-001:END -->

  <!-- TC:TC-BE-CU-002:START -->
TC-BE-CU-002 — Duplicate configKey rejected (RULE-CU-001 violation)
  API-ID: API-CU-001 | RULE-ID: RULE-CU-001 | ERR-ID: ERR-0001 | Data class: INVALID
  Given : configuration "mail.smtp.host" already exists
  When  : POST with configKey "mail.smtp.host"
  Then  : HTTP 409; ERR-0001; messageAr "مفتاح الإعداد موجود مسبقاً — اختر مفتاحاً فريداً."; messageEn present
  Language: BOTH
  <!-- TC:TC-BE-CU-002:END -->

  <!-- TC:TC-BE-CU-003:START -->
TC-BE-CU-003 — Required fields satisfied (RULE-CU-002 happy)
  API-ID: API-CU-001 | RULE-ID: RULE-CU-002 | Data class: VALID
  Given : payload with both configKey and configValue
  When  : POST
  Then  : HTTP 201
  <!-- TC:TC-BE-CU-003:END -->

  <!-- TC:TC-BE-CU-004:START -->
TC-BE-CU-004 — Missing configValue rejected (RULE-CU-002 violation)
  API-ID: API-CU-001 | RULE-ID: RULE-CU-002 | ERR-ID: ERR-0002 | Data class: INVALID
  Given : payload with configKey but no configValue
  When  : POST
  Then  : HTTP 400; ERR-0002; messageAr "مفتاح الإعداد وقيمته إلزاميان."
  Language: BOTH
  <!-- TC:TC-BE-CU-004:END -->

  <!-- TC:TC-BE-CU-005:START -->
TC-BE-CU-005 — Update value keeps key (RULE-CU-003 happy)
  API-ID: API-CU-003 | RULE-ID: RULE-CU-003 | Data class: VALID
  Given : existing configuration "mail.smtp.host"
  When  : PUT /{key} with new configValue only
  Then  : HTTP 200; configValue updated; configKey unchanged
  <!-- TC:TC-BE-CU-005:END -->

  <!-- TC:TC-BE-CU-006:START -->
TC-BE-CU-006 — configKey change attempt rejected (RULE-CU-003 violation)
  API-ID: API-CU-003 | RULE-ID: RULE-CU-003 | ERR-ID: ERR-0003 | Data class: INVALID
  Given : existing configuration "mail.smtp.host"
  When  : PUT /{key} attempting to change configKey to "mail.smtp.host2"
  Then  : HTTP 422; ERR-0003; messageAr "لا يمكن تعديل مفتاح الإعداد بعد إنشائه."
  Language: BOTH
  <!-- TC:TC-BE-CU-006:END -->

  <!-- SUB:RULE-SCENARIOS:END -->

  <!-- SUB:API-SCENARIOS:START -->

  <!-- TC:TC-BE-CU-007:START -->
TC-BE-CU-007 — Search configurations (happy path)
  API-ID: API-CU-002 | Data class: VALID
  Given : several configurations exist
  When  : GET /api/v1/common/configurations?configKey=mail&page=0&size=10
  Then  : HTTP 200; Page with matching rows; sort within ALLOWED_SORT_FIELDS
  <!-- TC:TC-BE-CU-007:END -->

  <!-- TC:TC-BE-CU-008:START -->
TC-BE-CU-008 — Update configuration (happy path)
  API-ID: API-CU-003 | Data class: VALID
  Given : existing configuration
  When  : PUT /{key} with valid configValue
  Then  : HTTP 200; updatedAt refreshed by AuditEntityListener
  <!-- TC:TC-BE-CU-008:END -->

  <!-- TC:TC-BE-CU-009:START -->
TC-BE-CU-009 — Deactivate configuration (soft, happy path)
  API-ID: API-CU-004 | Data class: VALID
  Given : active configuration
  When  : DELETE /{key}
  Then  : HTTP 200/204; isActiveFl=0 in DB; row still present (not hard-deleted)
  <!-- TC:TC-BE-CU-009:END -->

  <!-- TC:TC-BE-CU-010:START -->
TC-BE-CU-010 — Get configuration by key (happy path)
  API-ID: API-CU-005 | Data class: VALID
  Given : existing configuration "mail.smtp.host"
  When  : GET /{key}
  Then  : HTTP 200; full ConfigurationResponse
  <!-- TC:TC-BE-CU-010:END -->

  <!-- TC:TC-BE-CU-011:START -->
TC-BE-CU-011 — Get by unknown key returns 404 (MANDATORY-J platform)
  API-ID: API-CU-005 | ERR-ID: ERR-0004 | Data class: INVALID
  Given : no configuration with key "nope"
  When  : GET /nope
  Then  : HTTP 404; ERR-0004; messageAr "الإعداد غير موجود."
  <!-- TC:TC-BE-CU-011:END -->

  <!-- TC:TC-BE-CU-012:START -->
TC-BE-CU-012 — Empty search returns 200 not 404 (MANDATORY-J-7)
  API-ID: API-CU-002 | Data class: EDGE_CASE
  Given : filter matches no rows
  When  : GET /api/v1/common/configurations?configKey=zzz
  Then  : HTTP 200 with empty content [] — NEVER 404
  <!-- TC:TC-BE-CU-012:END -->

  <!-- TC:TC-BE-CU-013:START -->
TC-BE-CU-013 — SQL injection resistance (MANDATORY-J-8)
  API-ID: API-CU-001 | Data class: ATTACK
  Given : POST accepting string input
  When  : configValue = "x'; DROP TABLE CU_APP_CONFIGURATION; --"
  Then  : value stored as literal string; DB intact; no data leaked
  <!-- TC:TC-BE-CU-013:END -->

  <!-- SUB:API-SCENARIOS:END -->

<!-- PHASE:TEST-PLAN-BE:END -->

## TC TRACEABILITY INDEX (BACKEND) — CU
══════════════════════════════════════════════════════════════════
RULE-ID → TC-IDs:
  RULE-CU-001 → TC-BE-CU-001 (happy) | TC-BE-CU-002 (violation)
  RULE-CU-002 → TC-BE-CU-003 (happy) | TC-BE-CU-004 (violation)
  RULE-CU-003 → TC-BE-CU-005 (happy) | TC-BE-CU-006 (violation)
API-ID → TC-IDs:
  API-CU-001 → TC-BE-CU-001 | API-CU-002 → TC-BE-CU-007/012
  API-CU-003 → TC-BE-CU-008 | API-CU-004 → TC-BE-CU-009 | API-CU-005 → TC-BE-CU-010/011
ERR-ID → TC-IDs:
  ERR-0001 → TC-BE-CU-002 | ERR-0002 → TC-BE-CU-004 | ERR-0003 → TC-BE-CU-006 | ERR-0004 → TC-BE-CU-011
══════════════════════════════════════════════════════════════════
Coverage: RULE 3/3 | API 5/5 | Total backend TCs: 13 (target 15–25 ceiling — under 40 guard)
══════════════════════════════════════════════════════════════════

*End of backend-test-plan.md — CU — PLAN-CU-001*
