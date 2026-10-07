<!-- Source: PHASE:TEST-PLAN-BE / SUB:RULE-SCENARIOS -->

  <!-- TC:TC-BE-FILE-001:START -->
TC-BE-FILE-001 — Upload within size limit (RULE-FILE-001 happy) | API-FILE-001 | VALID
  Given 2MB file, valid ownership → When POST /files → Then 201; fileStatusId=ACTIVE.
  <!-- TC:TC-BE-FILE-001:END -->
  <!-- TC:TC-BE-FILE-002:START -->
TC-BE-FILE-002 — Oversized file rejected (RULE-FILE-001 violation) | API-FILE-001 | ERR-0001 | BOUNDARY
  Given 6MB file (> 5MB) → When POST → Then 413 ERR-0001; messageAr "حجم الملف يتجاوز المسموح." | BOTH
  <!-- TC:TC-BE-FILE-002:END -->
  <!-- TC:TC-BE-FILE-003:START -->
TC-BE-FILE-003 — Allowed content type (RULE-FILE-002 happy) | API-FILE-001 | VALID
  Given PNG upload, category allows images → When POST → Then 201; contentType auto-detected image/png.
  <!-- TC:TC-BE-FILE-003:END -->
  <!-- TC:TC-BE-FILE-004:START -->
TC-BE-FILE-004 — Disallowed type rejected (RULE-FILE-002 violation) | API-FILE-001 | ERR-0002 | INVALID
  Given .exe disguised as image, server MIME detect → When POST → Then 415 ERR-0002; messageAr "نوع الملف غير مسموح." | BOTH
  <!-- TC:TC-BE-FILE-004:END -->
  <!-- TC:TC-BE-FILE-005:START -->
TC-BE-FILE-005 — Download with valid token (RULE-FILE-003 happy) | API-FILE-003 | VALID
  Given fresh access token → When GET /files/download?token= → Then 200 binary stream; token consumed.
  <!-- TC:TC-BE-FILE-005:END -->
  <!-- TC:TC-BE-FILE-006:START -->
TC-BE-FILE-006 — Expired/reused token rejected (RULE-FILE-003 violation) | API-FILE-003 | ERR-0003 | INVALID
  Given used/expired token → When GET download → Then 401 ERR-0003; messageAr "رابط الوصول غير صالح/منتهٍ." | BOTH
  <!-- TC:TC-BE-FILE-006:END -->
  <!-- TC:TC-BE-FILE-007:START -->
TC-BE-FILE-007 — Auth delegated to Security filter (RULE-FILE-004) | API-FILE-004 | ATTACK
  Given no/invalid JWT → When GET /files/{id} → Then 401 from Security filter before FILE service runs; FILE performs no self JWT check.
  <!-- TC:TC-BE-FILE-007:END -->
  <!-- TC:TC-BE-FILE-008:START -->
TC-BE-FILE-008 — Ownership fields present (RULE-FILE-005 happy) | API-FILE-001 | VALID
  Given ownerId+ownerType+moduleCode → When POST → Then 201.
  <!-- TC:TC-BE-FILE-008:END -->
  <!-- TC:TC-BE-FILE-009:START -->
TC-BE-FILE-009 — Missing ownership rejected (RULE-FILE-005 violation) | API-FILE-001 | ERR-0004 | INVALID
  Given no moduleCode → When POST → Then 400 ERR-0004; messageAr "بيانات الملكية إلزامية." | BOTH
  <!-- TC:TC-BE-FILE-009:END -->
  <!-- TC:TC-BE-FILE-010:START -->
TC-BE-FILE-010 — Soft delete retains bytes (RULE-FILE-006) | API-FILE-006 | VALID
  Given ACTIVE file → When DELETE /files/{id} → Then 200; fileStatusId=DELETED; FILE_CONTENT bytes still present in DB.
  <!-- TC:TC-BE-FILE-010:END -->
  <!-- TC:TC-BE-FILE-011:START -->
TC-BE-FILE-011 — Unique category code (RULE-FILE-007 happy) | API-FILE-007 | VALID
  Given new categoryCode → When POST /files/categories → Then 201.
  <!-- TC:TC-BE-FILE-011:END -->
  <!-- TC:TC-BE-FILE-012:START -->
TC-BE-FILE-012 — Duplicate category code (RULE-FILE-007 violation) | API-FILE-007 | ERR-0005 | INVALID
  Given existing categoryCode → When POST → Then 409 ERR-0005; messageAr "رمز الفئة مستخدَم مسبقاً." | BOTH
  <!-- TC:TC-BE-FILE-012:END -->
