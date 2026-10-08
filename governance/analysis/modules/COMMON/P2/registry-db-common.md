## REGISTRY — P2 — COMMON v1 (as-built baseline, erp-core 1.2.0)
══════════════════════════════════════════════════════════════════

Tables
| Table | ENT id | Kind | DBF range |
|---|---|---|---|
| (none of its own) | ENT-COMMON-001, ENT-COMMON-002 are mapped superclasses | column conventions on every core table | — (the columns are registered by each table's module; `TENANT_ID` ×22 by TENANT: DBF-TENANT-011…032) |

DBF ids: none assigned.

Column conventions (full detail in `db-script-common.md` §1)
| Convention | Columns | Physical shape | Tables |
|---|---|---|---|
| audit (ENT-COMMON-001) | `CREATED_BY`, `CREATED_AT`, `UPDATED_BY`, `UPDATED_AT` | `VARCHAR(100)` + `TIMESTAMPTZ` (SEC, MDL, `CORE_TENANT`, `CORE_NUMBER_SERIES`); `VARCHAR(255)` + `TIMESTAMP` (CU, FILE, NOTIF, `NOTIF_INBOX`); `VARCHAR(100)` + `TIMESTAMP` (`CORE_AUDIT_EVENT`); JPA length 100, `Instant` | every core table |
| optimistic lock (ENT-COMMON-001) | `VERSION` | `BIGINT NOT NULL DEFAULT 0` | every core table (22 tenant-scoped + `CORE_TENANT` + 3 registries) |
| tenant (ENT-COMMON-002) | `TENANT_ID` | `BIGINT NOT NULL`, `FK_<TABLE>_TENANT`, `IDX_<TABLE>_TENANT`, uniques lead with it | 22 tenant-scoped tables (`CU_APP_CONFIGURATION` nullable, global entity — exception) |
| booleans | `IS_*_FL` | `SMALLINT` 1/0 + `CHK_*_ACTIVE_FL` (`BooleanNumberConverter`) on the pre-V10 CU / FILE / NOTIF tables; `BOOLEAN` on the SEC tables and on every table since V10 (no converter); `BooleanCharYNConverter` has no `@Convert` site | CU, FILE, NOTIF (converter); SEC and later tables (`BOOLEAN`) |

XM index
| XM-ID | Type | Surface / target | Physical object | Status |
|---|---|---|---|---|
| (none) | — | XM-COMMON-001 (the Java API) is in `../P1/registry-srs-common.md`; the `TENANT_ID` FKs are each table's module's XM to TENANT | — | — |

Lookups
| Key | Seeded values count | Owner |
|---|---|---|
| `Status` | 14 (Java enum, no rows) | COMMON |
| `SearchOperator` | 8 (Java enum, no rows) | COMMON |
| realm | 3 (String constants, no rows) | COMMON (`SecurityContextHelper`) |

Sequences
none. Last DBF: none · Last XM: none

Decisions
ADR-COMMON-001, ADR-COMMON-002, ADR-COMMON-003 (ACCEPTED, as built) — see `governance/analysis/decisions/COMMON/`

Event
"P2 completed: COMMON v1 (as built) — 0 tables of its own, 0 DBF, 0 XM, 3 column conventions"

Cascade
No registry XM row of another module targets COMMON with status DEFERRED — nothing to resolve. Every
module's 1.2.0 P2 addendum already records its own `TENANT_ID` and `VERSION` columns (SEC, MDL, CU,
FILE, NOTIF) and TENANT's register holds the 22 discriminator columns; this file adds the JPA side of
the same columns and does not replace those entries.
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 01, 05, 15
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : none recorded here yet — the idempotency mechanism of the tenant-maturity plan (C.4) is documented by the implementing run when it lands on main
Statement      : This addendum records only what is already on main for 1.3.0; the plan packages' rows are written by the implementing run.

Plan packages B, C, D, E, G: documented by each package as it lands on main (analysis-first, written by the implementing run); the verified reference rows are in docs/plans/tenant-maturity-analysis-reference.md.
