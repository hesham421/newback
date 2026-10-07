<!-- Source: PHASE:TEST-PLAN-BE / SUB:RULE-SCENARIOS -->


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

