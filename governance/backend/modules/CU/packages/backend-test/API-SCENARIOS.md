<!-- Source: PHASE:TEST-PLAN-BE / SUB:API-SCENARIOS -->


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

