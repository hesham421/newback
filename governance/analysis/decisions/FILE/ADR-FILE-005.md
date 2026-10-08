# ADR-FILE-005 — FILE lookup values served from MDL (V8)

Module  : FILE     Version : v1 (as built, erp-core 1.2.0)     Stage raised : P0 → P2 — recorded after the fact (step 04)
Status  : ACCEPTED (non-breaking)

## Context
When FILE was analysed, "no MasterData module exists in the Foundation domain", so the module registry
fixed as an AUTO-DECISION "FileType + FileStatus owned as module-local LOVs — IF WRONG: relocate to a
shared lookup if one is introduced later"; SRS A5 said "لا MD_MASTER_LOOKUP مركزي — قوائم محلية"; the
DB script declared "Lookup Tables: None — runtime-loaded codes" and "no lookup table, no CHECK". MDL
was built afterwards as the platform's lookup hub (`MDL_LOOKUP_TYPE` / `MDL_LOOKUP_VALUE`, owner module
per type, `MdlLookupApi` for other modules), and the old NOTIF / FILE lookup data migration was
squashed in step 04 into `V8__mdl_seed.sql`.

## Decision
The two FILE value sets are MDL lookup types: `FILE_FILE_TYPE` (IMAGE, DOCUMENT, SPREADSHEET,
ARCHIVE, OTHER) and `FILE_FILE_STATUS` (ACTIVE, ARCHIVED, DELETED), owner module `FILE`, seeded by V8
with the codes, bilingual labels and sort orders the SRS A5 listed. API-FILE-008
(`GET /api/v1/files/lookups/{lookupKey}`) reads them live through `MdlLookupApi.readActiveValuesByKey`,
fronts only its two keys (any other key, and an MDL not-found, answer FILE's own 404
`FILE_LOOKUP_KEY_UNKNOWN`, never an MDL code), and returns `code`, `labelAr`, `labelEn` (since 1.3.0
through the shared `OwnedLookups` / `LookupOptionResponse`).

The database columns stay as designed: `FILE_TYPE_ID` and `FILE_STATUS_ID` are plain `VARCHAR(50)`
code columns with no FK to MDL and no CHECK; the codes also remain constants in FILE
(`FileLookupService.TYPE_*`, `FileDocumentDomain.STATUS_*`) because the content classifier and the
lifecycle state machine need them without a database read.

## Consequences
- The "IF WRONG" case of the AUTO-DECISION occurred; FILE now depends on MDL (API, no physical FK),
  so the module registry's "File Service calls NO other module" no longer holds (MDL does not call
  FILE — no cycle).
- Adding a display value is MDL data (an `MDL_LOOKUP_VALUE` row); adding a value the classifier or
  the state machine must act on is a FILE code change as well. Deactivating a value in MDL hides it
  from the dropdown but does not affect stored rows.
- `db-script.md`'s header ("Lookup Tables: None"), design note 3 and BLOCK 8 describe the FILE
  schema correctly (no FILE lookup table) but not where the lists live; the 1.2.0 addendum records
  the contradiction.
- Precedent: ADR-SEC-001 chose CHECK-constrained columns because MDL did not exist yet; FILE's
  columns are not CHECK-constrained either, the value set being enforced in code and in MDL.

## Traces
LOV-FILE-001 · LOV-FILE-002 · API-FILE-008 · `FILE_LOOKUP_KEY_UNKNOWN` ·
module-registry-file.md §LOVs OWNED, §AUTO-DECISIONS (module-local LOVs) · srs.md A5 ·
db-script.md header, design note 3, BLOCK 8 ·
erp-core/src/main/resources/db/migration/core/V8__mdl_seed.sql L25-31, L38-62 ·
erp-core/src/main/resources/db/migration/core/V5__file_schema.sql L6, L92-94 ·
erp-core/src/main/java/com/erp/file/service/FileLookupService.java:17-58 ·
erp-core/src/main/java/com/erp/common/lookup/OwnedLookups.java:32-46 ·
docs/steps/04-report.md → "Old → new mapping" · ADR-SEC-001
