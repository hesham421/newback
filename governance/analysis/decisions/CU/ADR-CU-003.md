# ADR-CU-003 — Configuration keys are case-insensitive and stored upper-case

Module  : CU     Version : v1 (erp-core 1.2.0, as-built)     Stage raised : P3 (erp-core step 09)
Status  : ACCEPTED (as-built, non-breaking)

## Context
`configKey` is the business key of `AppConfiguration` (every non-search endpoint addresses the row by
`/{key}`; `SettingsApi` reads by key), and RULE-CU-001 makes it unique per owner. The SRS (A3) declares
the column `VARCHAR(150) UNIQUE` without saying whether `mail.host` and `MAIL.HOST` are one key or two.
Keys are typed by administrators and spelled in application code; a case-sensitive key would let two
rows that look identical coexist and would make `SettingsApi.get("mail.host")` miss a row written as
`MAIL.HOST`. The database unique index is case-sensitive (`CONFIG_KEY`, no `lower()` expression).

## Decision
- The canonical form of a key is upper-case. The entity upper-cases `configKey` in `@PrePersist` and
  `@PreUpdate` (`erp-core/src/main/java/com/erp/cu/entity/AppConfiguration.java:71-86`), so the stored
  value, the unique index and every response carry the upper-case form (a create with `mail.host`
  answers `MAIL.HOST`).
- Every lookup normalises the caller's key the same way before touching the database —
  `ConfigurationService.normalize(key) = key.trim().toUpperCase()` — in the uniqueness pre-check, in
  `findInScope` (GET / PUT / DELETE `/{key}`) and in `SettingsApiImpl.get` / `find`
  (`ConfigurationService.java:83,219,233-235`; `SettingsApiImpl.java:28,39`). The key in
  `NoSuchSettingException.getKey()` is the normalised one.
- Uniqueness therefore holds on the canonical form: `MAIL.HOST` and `mail.host` are one key per owner
  (RULE-CU-001 in `../../modules/CU/P1/srs-cu.md` addendum §2).

## Consequences
- Clients and modules may spell a key in any case; the contract value is the upper-case spelling.
- Search filters on `configKey` compare against the stored upper-case value: a `LIKE` filter must use
  upper-case text (the filter value is not normalised).
- Known defect to fix: the persist hooks upper-case but do **not** trim, while the lookups trim. A
  create whose `configKey` carries a leading or trailing blank (`" key"`, which passes `@NotBlank`)
  stores `" KEY"`; the uniqueness pre-check looked for `"KEY"` and found nothing, and no later lookup
  (`GET /{key}`, `SettingsApi`) can reach the row. The fix belongs in `AppConfiguration.onCreate` /
  `onUpdate` (trim before upper-casing) — an additive code change with no migration; existing rows with
  surrounding blanks, if any, need a one-off data correction. Recorded as RULE-CU-009's defect note.
- The V2 column comment "Unique configuration key; read-only after creation" (`V2__cu_schema.sql:37`)
  does not mention the canonical form; it is informational and left as shipped (core scripts are never
  edited).

## Traces
ENTITY-CU-001 · RULE-CU-001, RULE-CU-003, RULE-CU-009 · API-CU-001, API-CU-003, API-CU-004, API-CU-005 ·
DBF-0002 (`CONFIG_KEY`) · `ConfigurationScopeApiIntegrationTest`, `SettingsCacheTest`
