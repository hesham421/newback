<!-- Source: PHASE:TEST-PLAN-BE / SUB:RULE-SCENARIOS -->

  <!-- TC:TC-BE-NOTIF-001:START -->
TC-BE-NOTIF-001 — Fan-out one log per channel (RULE-NOTIF-001) | API-NOTIF-001 | VALID
  Given channelHint=[EMAIL,SMS], active recipient, valid template → When POST /dispatch → Then 202; exactly 2 NOTIF_LOG rows (one per channel).
  <!-- TC:TC-BE-NOTIF-001:END -->
  <!-- TC:TC-BE-NOTIF-002:START -->
TC-BE-NOTIF-002 — Retry then success (RULE-NOTIF-002 happy) | API-NOTIF-001 | VALID
  Given provider fails twice then succeeds → When dispatch → Then status=SENT; retryCount≤5.
  <!-- TC:TC-BE-NOTIF-002:END -->
  <!-- TC:TC-BE-NOTIF-003:START -->
TC-BE-NOTIF-003 — Exhausted retries → FAILED (RULE-NOTIF-002 violation) | API-NOTIF-001 | BOUNDARY
  Given provider fails 6 times → When dispatch → Then status=FAILED after 5 retries; errorMessage recorded.
  <!-- TC:TC-BE-NOTIF-003:END -->
  <!-- TC:TC-BE-NOTIF-004:START -->
TC-BE-NOTIF-004 — Disabled channel → CHANNEL_DISABLED (RULE-NOTIF-003) | API-NOTIF-001 | EDGE_CASE
  Given SMS channel isEnabledFl=0 → When dispatch with SMS → Then NOTIF_LOG status=CHANNEL_DISABLED; no retry, no provider call.
  <!-- TC:TC-BE-NOTIF-004:END -->
  <!-- TC:TC-BE-NOTIF-005:START -->
TC-BE-NOTIF-005 — Bilingual template accepted (RULE-NOTIF-004 happy) | API-NOTIF-004 | VALID
  Given bodyAr+bodyEn present → When POST /templates → Then 201.
  <!-- TC:TC-BE-NOTIF-005:END -->
  <!-- TC:TC-BE-NOTIF-006:START -->
TC-BE-NOTIF-006 — Missing bilingual body rejected (RULE-NOTIF-004 violation) | API-NOTIF-004 | ERR-0001 | INVALID
  Given bodyEn missing → When POST /templates → Then 400 ERR-0001; messageAr "قوالب ثنائية اللغة والمرفق عبر الملفات." | BOTH
  <!-- TC:TC-BE-NOTIF-006:END -->
  <!-- TC:TC-BE-NOTIF-007:START -->
TC-BE-NOTIF-007 — Auth delegated to Security filter (RULE-NOTIF-005) | API-NOTIF-002 | ATTACK
  Given no/invalid JWT → When GET /logs → Then 401 from Security filter; NOTIF performs no self JWT check.
  <!-- TC:TC-BE-NOTIF-007:END -->
  <!-- TC:TC-BE-NOTIF-008:START -->
TC-BE-NOTIF-008 — Unique codes accepted (RULE-NOTIF-006 happy) | API-NOTIF-004/005 | VALID
  Given new templateCode / new channelTypeId → When POST → Then 201.
  <!-- TC:TC-BE-NOTIF-008:END -->
  <!-- TC:TC-BE-NOTIF-009:START -->
TC-BE-NOTIF-009 — Duplicate code rejected (RULE-NOTIF-006 violation) | API-NOTIF-004/005 | ERR-0002/0003 | INVALID
  Given existing templateCode → When POST /templates → Then 409 ERR-0002; existing channelTypeId → 409 ERR-0003; messageAr "رمز القالب والقناة فريدان." | BOTH
  <!-- TC:TC-BE-NOTIF-009:END -->
  <!-- TC:TC-BE-NOTIF-010:START -->
TC-BE-NOTIF-010 — Skip inactive recipient (RULE-NOTIF-007) | API-NOTIF-001 | VALID
  Given recipient UserAccount inactive (SEC, XM-NOTIF-001) → When dispatch → Then no send; log reflects skip; prior historical logs for that recipient retained.
  <!-- TC:TC-BE-NOTIF-010:END -->
