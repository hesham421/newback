<!-- Source: PHASE:SVC-API / SUB:SVC-API-LOOKUP -->
<!-- Context: see SVC-API-HEADER.md for phase-level strategy, registry table, and intro -->

<!-- API:API-FILE-008:START -->
### API-FILE-008 — Lookups
GET /api/v1/files/lookups/{lookupKey} | FileLookupController.get → FileLookupService.get
REQUEST path lookupKey ∈ {FILE_FILE_TYPE, FILE_FILE_STATUS} | RESPONSE 200 [{code,labelAr,labelEn}]
VALIDATIONS: none | ERRORS: ERR-0006 → unknown lookupKey → 404
BINDING: LOV-FILE-001 FILE_FILE_TYPE (IMAGE,DOCUMENT,SPREADSHEET,ARCHIVE,OTHER); LOV-FILE-002 FILE_FILE_STATUS (ACTIVE,ARCHIVED,DELETED).
REPO: (runtime code resolution — no lookup table) — READ_ONLY | SECURITY: Security filter.
<!-- API:API-FILE-008:END -->
