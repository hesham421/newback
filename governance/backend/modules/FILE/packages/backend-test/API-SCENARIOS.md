<!-- Source: PHASE:TEST-PLAN-BE / SUB:API-SCENARIOS -->

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
