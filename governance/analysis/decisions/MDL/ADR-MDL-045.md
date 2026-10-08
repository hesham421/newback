# ADR-MDL-045 — Index strategy as built (V3): active-flag and FK indexes, no name indexes

Module  : MDL     Version : v1     Stage raised : erp-core 1.2.0 analysis revision (2026-10-07)
Status  : ACCEPTED (non-breaking) — replaces the dropped ADR-MDL-009 and ADR-MDL-010

## Context
`db-script-mdl.md` §3 BLOCK 7 planned four non-PK indexes for the two MDL tables —
`IDX_MDL_LOOKUP_TYPE_OWNER (owner_module_code)`, `IDX_MDL_LOOKUP_TYPE_NAME_AR (name_ar)`,
`IDX_MDL_LOOKUP_TYPE_NAME_EN (name_en)` and `IDX_MDL_LOOKUP_VALUE_TYPE_SORT (lookup_type_id,
sort_order)` — and stated that neither `is_active_fl` column is indexed, citing ADR-MDL-009
("the filters get an index, the flags do not"). ADR-MDL-010 fixed the text widths at
`key VARCHAR(50)`, `name_ar` / `name_en VARCHAR(200)`, `sort_order INTEGER`.

The shipped schema, `erp-core/src/main/resources/db/migration/core/V3__mdl_schema.sql` (DDL
verbatim from the old `V18__mdl_sequences`, squashed in step 04), built something else:

| Object | Planned | Built (V3) |
|---|---|---|
| `IDX_MDL_LOOKUP_TYPE_OWNER (owner_module_code)` | yes | yes — V3:112 |
| `IDX_MDL_LOOKUP_TYPE_NAME_AR`, `IDX_MDL_LOOKUP_TYPE_NAME_EN` | yes | **never created** |
| `IDX_MDL_LOOKUP_TYPE_ACTIVE (is_active_fl)` | no (ADR-MDL-009) | **yes** — V3:113 |
| `IDX_MDL_LOOKUP_VALUE_TYPE (lookup_type_id)` | no ("no third index over the leading column") | **yes** — V3:114 |
| `(lookup_type_id, sort_order)` | `IDX_MDL_LOOKUP_VALUE_TYPE_SORT` | `IDX_MDL_LOOKUP_VALUE_SORT` — V3:115 |
| `key` | VARCHAR(50) | VARCHAR(80) — V3:26 |
| `name_ar` / `name_en` (both tables) | VARCHAR(200) | VARCHAR(150) — V3:28-29,44-45 |
| `sort_order` | INTEGER | NUMERIC (entity maps `Integer`) — V3:46 |

The JPA entities declare the same four indexes by name (`LookupType.java:39-42`,
`LookupValue.java:43-46`), and `V10__tenant_schema.sql` added `IDX_MDL_LOOKUP_TYPE_TENANT` /
`IDX_MDL_LOOKUP_VALUE_TENANT (TENANT_ID)` on top. Both planning ADRs were dropped from
`decisions/MDL/` during the vendoring review because the migration contradicts them
(`docs/governance-vendoring-report.md`), leaving `db-script-mdl.md` and `registry-db-mdl.md` citing
files that no longer exist. Core migrations are additive only (`MigrationNamingTest`), so neither the
planned name indexes nor the planned widths can be restored by editing `V3`, and a corrective script
would change nothing a shipped consumer relies on.

## Decision
The index strategy of MDL is the one `V3` built, and it is recorded as the current truth:

- `IDX_MDL_LOOKUP_TYPE_OWNER (owner_module_code)` — the EXACT owner filter of both screens.
- `IDX_MDL_LOOKUP_TYPE_ACTIVE (is_active_fl)` — the unconditional `isActiveFl = true` predicate of
  `POST /lookup-types/by-owner/search` and the `isActiveFl` filter of `POST /lookup-types/search`.
- `IDX_MDL_LOOKUP_VALUE_TYPE (lookup_type_id)` — the FK column, so every FK check and the parent-scoped
  value search are covered independently of the sort order.
- `IDX_MDL_LOOKUP_VALUE_SORT (lookup_type_id, sort_order)` — the ordered consumer read
  (`findActiveValuesOfActiveType`) and the default `sortOrder` sort of the value search.
- No index on `name_ar` / `name_en`: the name filters are `LIKE` filters on a table whose size is
  bounded by the number of coded lists per tenant; an index would not be used for a leading-wildcard
  match and is not worth its write cost.
- The type key stays served by the implicit index of `UQ_MDL_LOOKUP_TYPE_KEY (TENANT_ID, key)`; the
  value code by `UQ_MDL_LOOKUP_VALUE_TYPE_CODE (TENANT_ID, lookup_type_id, code)` — never duplicated.
- The text widths and `sort_order NUMERIC` of `V3` stand; the entity maps `NUMERIC` to `Integer`
  and the request DTOs validate `@Size(max = 80 / 10 / 150)`.

No migration is written for this decision. The DBF tables and BLOCK 7 of `db-script-mdl.md` keep the
planned text as history; the "Implementation Addendum — erp-core 1.2.0" of that file and of
`registry-db-mdl.md` carry the as-built rows and cite this ADR.

## Consequences
- `db-script-mdl.md` BLOCK 7 / §4 and `registry-db-mdl.md` "Decisions" cite ADR-MDL-009 / 010 as
  ACCEPTED in their bodies; those citations are history. Their 1.2.0 addenda mark both REMOVED and
  point here.
- Any future index change on `MDL_LOOKUP_TYPE` / `MDL_LOOKUP_VALUE` is a new additive core
  migration (`V<next>__…`) recorded first in the `P2/db-script-mdl.md` addendum, never an edit of `V3`.
- A later decision to widen or narrow a text column is likewise a new ADR and a new migration;
  narrowing is not additive and is therefore not available to the core chain.

## Traces
ENT-MDL-001, ENT-MDL-002 · DBF-MDL-002, DBF-MDL-004, DBF-MDL-005, DBF-MDL-006, DBF-MDL-012,
DBF-MDL-014, DBF-MDL-015, DBF-MDL-016, DBF-MDL-017 · REQ-MDL-005, REQ-MDL-011, REQ-MDL-013 ·
`V3__mdl_schema.sql:24-52,112-115` · `docs/governance-vendoring-report.md`
