<!-- Source: PHASE:TEST-PLAN-BE / SUB:API-SCENARIOS -->

  <!-- TC:TC-BE-NOTIF-011:START -->
TC-BE-NOTIF-011 — API-NOTIF-001 Dispatch happy → 202 {logIds[]}.
  <!-- TC:TC-BE-NOTIF-011:END -->
  <!-- TC:TC-BE-NOTIF-012:START -->
TC-BE-NOTIF-012 — API-NOTIF-002 Query logs happy + empty→200 (MANDATORY-J-7) → 200 Page; empty filter → 200 [].
  <!-- TC:TC-BE-NOTIF-012:END -->
  <!-- TC:TC-BE-NOTIF-013:START -->
TC-BE-NOTIF-013 — API-NOTIF-003 Log by id happy → 200 (incl. errorMessage, retryCount); unknown id → 404 ERR-0004.
  <!-- TC:TC-BE-NOTIF-013:END -->
  <!-- TC:TC-BE-NOTIF-014:START -->
TC-BE-NOTIF-014 — API-NOTIF-004 Templates CRUD happy + permission (MANDATORY-J-5) → 2xx; no VIEW on SCR-NOTIF-001 → 403.
  <!-- TC:TC-BE-NOTIF-014:END -->
  <!-- TC:TC-BE-NOTIF-015:START -->
TC-BE-NOTIF-015 — API-NOTIF-005 Channels CRUD/enable happy → 2xx; toggle isEnabledFl affects dispatch.
  <!-- TC:TC-BE-NOTIF-015:END -->
  <!-- TC:TC-BE-NOTIF-016:START -->
TC-BE-NOTIF-016 — API-NOTIF-006 Lookups happy + SQLi (MANDATORY-J-8) → 200 codes; moduleCode="x' OR '1'='1" stored literal, DB intact.
  <!-- TC:TC-BE-NOTIF-016:END -->
