## REGISTRY — P2 — SEQUENCE v1 (as-built baseline, erp-core 1.2.0)
══════════════════════════════════════════════════════════════════

Read from the core migration at main @ 19b19a4 (`erp-core/src/main/resources/db/migration/core/`;
`V14:line` = `V14__sequence_and_settings.sql` and line). Full detail: `db-script-sequence.md`.

Tables
| Table | ENT id | Kind | DBF range | Created by |
|---|---|---|---|---|
| CORE_NUMBER_SERIES | ENT-SEQUENCE-001 | config (tenant-scoped, one row per code and period) | DBF-SEQUENCE-001 … DBF-SEQUENCE-014 | V14:31-46 |

DBF ids → code location
| DBF id | Column | Type | Null | Constraint / FK | Index | Code location |
|---|---|---|---|---|---|---|
| DBF-SEQUENCE-001 | CORE_NUMBER_SERIES.ID | BIGINT | NOT NULL | `PK_CORE_NUMBER_SERIES`; `SEQ_CORE_NUMBER_SERIES` | (PK) | V14:32, :56, :25-29 |
| DBF-SEQUENCE-002 | CORE_NUMBER_SERIES.TENANT_ID | BIGINT | NOT NULL | `FK_CORE_NUMBER_SERIES_TENANT`; leads `UQ_CORE_NUMBER_SERIES_CODE_PERIOD` | `IDX_CORE_NUMBER_SERIES_TENANT` | V14:33, :57, :58, :61 |
| DBF-SEQUENCE-003 | CORE_NUMBER_SERIES.CODE | VARCHAR(50) | NOT NULL | in `UQ_CORE_NUMBER_SERIES_CODE_PERIOD` | (unique) | V14:34, :57 |
| DBF-SEQUENCE-004 | CORE_NUMBER_SERIES.PREFIX | VARCHAR(20) | NULL | — | — | V14:35 |
| DBF-SEQUENCE-005 | CORE_NUMBER_SERIES.PATTERN | VARCHAR(100) DEFAULT '{PREFIX}-{YYYY}-{SEQ:6}' | NOT NULL | — | — | V14:36 |
| DBF-SEQUENCE-006 | CORE_NUMBER_SERIES.RESET_POLICY | VARCHAR(10) DEFAULT 'YEARLY' | NOT NULL | `CHK_CORE_NUMBER_SERIES_RESET` | — | V14:37, :59 |
| DBF-SEQUENCE-007 | CORE_NUMBER_SERIES.PERIOD_KEY | VARCHAR(7) DEFAULT '' | NOT NULL | in `UQ_CORE_NUMBER_SERIES_CODE_PERIOD` | (unique) | V14:38, :57 |
| DBF-SEQUENCE-008 | CORE_NUMBER_SERIES.NEXT_VALUE | BIGINT DEFAULT 1 | NOT NULL | `CHK_CORE_NUMBER_SERIES_NEXT` (≥ 1) | — | V14:39, :60 |
| DBF-SEQUENCE-009 | CORE_NUMBER_SERIES.IS_ACTIVE | BOOLEAN DEFAULT TRUE | NOT NULL | — | — | V14:40 |
| DBF-SEQUENCE-010 | CORE_NUMBER_SERIES.CREATED_BY | VARCHAR(100) | NULL | — | — | V14:41 |
| DBF-SEQUENCE-011 | CORE_NUMBER_SERIES.CREATED_AT | TIMESTAMPTZ DEFAULT now() | NOT NULL | — | — | V14:42 |
| DBF-SEQUENCE-012 | CORE_NUMBER_SERIES.UPDATED_BY | VARCHAR(100) | NULL | — | — | V14:43 |
| DBF-SEQUENCE-013 | CORE_NUMBER_SERIES.UPDATED_AT | TIMESTAMPTZ | NULL | — | — | V14:44 |
| DBF-SEQUENCE-014 | CORE_NUMBER_SERIES.VERSION | BIGINT DEFAULT 0 | NOT NULL | — | — | V14:45 |

Counts verified: 14 columns (V14:32-45); 1 PK, 1 unique, 1 FK, 2 CHECK, 1 index (V14:56-61). The tenant
column is one of the 22 counted by `erp-core/src/test/java/com/erp/tenant/TenantSchemaIntegrationTest.java:51`
(tenant-side register entry DBF-TENANT-031 in `../../TENANT/P2/db-script-tenant.md`).

XM index
| XM-ID | Type | Surface | Target / implementers | Status |
|---|---|---|---|---|
| XM-SEQUENCE-001 | HARD-FK + SOFT-READ (consumed) | `TENANT_ID` → `CORE_TENANT(ID)`; `TenantLookupApi.codeOf(Long)` | TENANT | ACTIVE |
| XM-SEQUENCE-002 | SPI implemented (consumed) | `TenantProvisioningContributor` (order 40, JDBC with explicit `TENANT_ID`) | TENANT | ACTIVE |
| XM-SEQUENCE-003 | crossmodule call (exposed) | `NumberSeriesApi.next` / `preview` | applications (no core consumer) | ACTIVE |

Lookups
| Key | Seeded values count | Owner |
|---|---|---|
| `CORE_NUMBER_SERIES.RESET_POLICY` | 3 (CHECK-constrained `CHK_CORE_NUMBER_SERIES_RESET` + enum `ResetPolicy`, not seeded rows) | SEQUENCE |

Sequences
`SEQ_CORE_NUMBER_SERIES` (V14:25-29; `CACHE 1`, entity `allocationSize = 1`).
Last DBF: DBF-SEQUENCE-014 · Last XM: XM-SEQUENCE-003

Decisions
ADR-SEQUENCE-001, ADR-SEQUENCE-002, ADR-SEQUENCE-003 (ACCEPTED, as built) — see `governance/analysis/decisions/SEQUENCE/`

Event
"P2 completed: SEQUENCE v1 (as built) — 1 table, 1 sequence, 14 DBF, 3 XM"

Cascade
No XM row of another module targets SEQUENCE: nothing references `CORE_NUMBER_SERIES`, and `NumberSeriesApi`
has no core consumer. The tenant-side register (`../../TENANT/P2/registry-db-tenant.md`, DBF-TENANT-031)
records the same `TENANT_ID` column from the tenant's point of view; this file is the owning register.
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 09
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.
