# ADR-MDL-009 — index strategy: the filters get an index, the flags do not, and no UNIQUE index is duplicated

Module  : MDL     Version : v1     Stage raised : P2 (Database)
Status  : ACCEPTED (non-breaking)

## Context
The engine's §4.3 makes three index classes mandatory — every FK column, every column used in
an SRS PART B search/list filter, every UNIQUE business key — and forbids duplicating an index
the database already creates implicitly. Applied literally to this module those three classes
overlap, and one of them names a column no sane plan would index:

| Column | Why §4.3 names it | Already indexed by |
|---|---|---|
| MDL_LOOKUP_TYPE.key | B2 filter (LIKE) + the consumer read by key (REQ-MDL-011, REQ-MDL-012) | UQ_MDL_LOOKUP_TYPE_KEY |
| MDL_LOOKUP_TYPE.owner_module_code | B2 filter (EXACT) + the grouping of REQ-MDL-013 | — |
| MDL_LOOKUP_TYPE.name_ar / name_en | B2 filter (LIKE, both languages together) | — |
| MDL_LOOKUP_TYPE.is_active_fl | B2 filter (EXACT) | — |
| MDL_LOOKUP_VALUE.lookup_type_id | FK column + the detail read of REQ-MDL-005 | UQ_MDL_LOOKUP_VALUE_TYPE_CODE (leading column) |
| MDL_LOOKUP_VALUE.code | B2 detail filter, always inside a selected type | UQ_MDL_LOOKUP_VALUE_TYPE_CODE |

The SRS leaves the strategy open — it states filters and an ordering, not an access plan — so
§9 of the engine puts the choice here rather than in a question.

## Decision
Four indexes are created, and three candidate indexes are deliberately not:

- `IDX_MDL_LOOKUP_TYPE_OWNER (owner_module_code)` — serves both the B2 exact filter and the
  by-owner grouping that is the whole of SCR-REQ-MDL-002.
- `IDX_MDL_LOOKUP_TYPE_NAME_AR (name_ar)` and `IDX_MDL_LOOKUP_TYPE_NAME_EN (name_en)` — one per
  language, because the SRS filters both together and a single index cannot serve two columns
  with an OR between them.
- `IDX_MDL_LOOKUP_VALUE_TYPE_SORT (lookup_type_id, sort_order)` — the module's hot path: the
  detail list of a selected type (REQ-MDL-005) and the consumer read (REQ-MDL-011) both read by
  type and return ordered by sort order. Leading with `lookup_type_id`, it is also the
  mandatory index of the FK column.
- NOT created: a second index on `lookup_type_id`, and one on `code` — both are led by
  `lookup_type_id` in the UNIQUE constraint above, and `key` is served by its own UNIQUE index.
  §4.3 forbids duplicating an implicit index; these three would be exactly that.
- NOT created: an index on either `is_active_fl`. A two-value BOOLEAN over a table of this size
  has no selectivity to offer, and the platform's other modules index no active flag either
  (`db-script-fin.md` BLOCK 7 indexes status and FK columns, never a flag).

## Consequences
- The infix half of the name filter (`LIKE '%…%'`) is not served by these btree indexes; only a
  prefix or exact match is. No REQ states a performance target for name search, the type
  registry is bounded by the number of modules on the platform, and a trigram index would be a
  new dependency (`pg_trgm`) this version has no requirement for. If a later version states one,
  it is an index added by a migration, not a change to any DBF, column or constraint.
- A query filtering only on an active flag scans; every screen that filters by flag also filters
  by key, owner or type, so it enters through one of the four indexes above.
- The choice is reversible in isolation: indexes are the only objects in this script that carry
  no data and no contract — adding or dropping one changes no DBF id and no API.

Traces : REQ-MDL-005, REQ-MDL-011, REQ-MDL-012, REQ-MDL-013 · ENT-MDL-001, ENT-MDL-002 ·
DBF-MDL-002, DBF-MDL-003, DBF-MDL-004, DBF-MDL-005, DBF-MDL-006, DBF-MDL-012, DBF-MDL-013,
DBF-MDL-016, DBF-MDL-017
