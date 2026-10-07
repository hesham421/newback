<!-- backend-test-plan.md — Governed by Execution Plan Governance Engine (Project 3.1) | JUnit -->

# backend-test-plan.md — File Service (FILE) — PLAN-ID: PLAN-FILE-001
══════════════════════════════════════════════════════════════════
Source: backend-execution-plan-FILE.md (ALIGN-BE ✓) · srs-FILE.md (v1.1) · db-script-FILE.md (DBS-FILE-001)
DB_TARGET: POSTGRESQL_16 | Open Questions: None
TARGET TC COUNT: 7 rules (12 TC) + 8 API happy (8 TC) = 20 (within 15–25)
══════════════════════════════════════════════════════════════════

<!-- PHASE:TEST-PLAN-BE:START -->

  <!-- SUB:RULE-SCENARIOS:START -->
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
  <!-- SUB:RULE-SCENARIOS:END -->

  <!-- SUB:API-SCENARIOS:START -->
  <!-- TC:TC-BE-FILE-013:START -->
TC-BE-FILE-013 — API-FILE-001 Upload happy → 201 metadata (no bytes in response).
  <!-- TC:TC-BE-FILE-013:END -->
  <!-- TC:TC-BE-FILE-014:START -->
TC-BE-FILE-014 — API-FILE-002 Access-token happy → 200 {accessToken,expiresAt}.
  <!-- TC:TC-BE-FILE-014:END -->
  <!-- TC:TC-BE-FILE-015:START -->
TC-BE-FILE-015 — API-FILE-003 Download happy → 200 stream; single-use token consumed.
  <!-- TC:TC-BE-FILE-015:END -->
  <!-- TC:TC-BE-FILE-016:START -->
TC-BE-FILE-016 — API-FILE-004 Metadata happy → 200; fileContent absent from DTO.
  <!-- TC:TC-BE-FILE-016:END -->
  <!-- TC:TC-BE-FILE-017:START -->
TC-BE-FILE-017 — API-FILE-005 List-by-owner happy + empty→200 (MANDATORY-J-7) → 200 Page; no bytes in rows.
  <!-- TC:TC-BE-FILE-017:END -->
  <!-- TC:TC-BE-FILE-018:START -->
TC-BE-FILE-018 — API-FILE-006 Archive happy → 200; fileStatusId=ARCHIVED.
  <!-- TC:TC-BE-FILE-018:END -->
  <!-- TC:TC-BE-FILE-019:START -->
TC-BE-FILE-019 — API-FILE-007 Categories CRUD happy + permission (MANDATORY-J-5) → 2xx; no VIEW → 403.
  <!-- TC:TC-BE-FILE-019:END -->
  <!-- TC:TC-BE-FILE-020:START -->
TC-BE-FILE-020 — API-FILE-008 Lookups happy + SQLi (MANDATORY-J-8) → 200 codes; fileName="x' OR '1'='1" stored literal, DB intact.
  <!-- TC:TC-BE-FILE-020:END -->
  <!-- SUB:API-SCENARIOS:END -->

<!-- PHASE:TEST-PLAN-BE:END -->

## TC TRACEABILITY INDEX (BACKEND) — FILE
══════════════════════════════════════════════════════════════════
RULE→TC: FILE-001→001/002 · FILE-002→003/004 · FILE-003→005/006 · FILE-004→007 · FILE-005→008/009 · FILE-006→010 · FILE-007→011/012
API→TC: 001→013 · 002→014 · 003→015 · 004→016 · 005→017 · 006→018 · 007→019 · 008→020
ERR→TC: ERR-0001→002 · 0002→004 · 0003→006 · 0004→009 · 0005→012 · 0006→(404 paths across 016/018/019/020)
══════════════════════════════════════════════════════════════════
Coverage: RULE 7/7 · API 8/8 · Total 20 TCs (< 40 guard)
══════════════════════════════════════════════════════════════════

*End of backend-test-plan.md — FILE — PLAN-FILE-001*
