## REGISTRY — P2 — REPORT v1 (as-built baseline, erp-core 1.2.0)
══════════════════════════════════════════════════════════════════

Tables
| Table | ENT id | Kind | DBF range |
|---|---|---|---|
| (none) | ENT-REPORT-001, ENT-REPORT-002 are value objects of the SPI | in-memory registry | — |

DBF ids: none assigned (no column owned by the module).

XM index
| XM-ID | Type | Surface / target | Physical object | Status |
|---|---|---|---|---|
| XM-REPORT-001 | SPI (exposed) | `com.erp.report.ReportProvider` → SEC, NOTIF, AUDIT, reference app | none | ACTIVE |
| XM-REPORT-002 | registry-row write (SEC `PermissionContributor`) | `<MODULE>_REPORTS`, `PERM_<MODULE>_REPORTS_VIEW`, `<MODULE>:REPORT:<CODE>` → `SEC_SCREEN_REG` (`PAGE_CODE VARCHAR(50)`, `NAME_AR/EN VARCHAR(150)`), `SEC_ACTION_REG` (`ACTION_CODE VARCHAR(40)`, `PERMISSION_CODE VARCHAR(100)`, `NAME_AR/EN VARCHAR(150)`), `SEC_MODULE_REG.CODE VARCHAR(10)` | SEC's global catalog tables | ACTIVE |
(XM-REPORT-003, the `MdlLookupApi.readActiveValuesByKey` read, is a crossmodule interface call with no
physical object: `../P1/registry-srs-report.md`.)

Lookups
| Key | Seeded values count | Owner |
|---|---|---|
| `ParamType` | 7 (Java enum, no rows) | REPORT |
| `ColumnType` | 6 (Java enum, no rows) | REPORT |
| `ReportRunDomain.ExportFormat` | 2 (Java enum, no rows) | REPORT |

Sequences
none. Last DBF: none · Last XM: XM-REPORT-002 (physical), XM-REPORT-003 (interface)

Decisions
ADR-REPORT-001, ADR-REPORT-002, ADR-REPORT-003 (ACCEPTED, as built) — see `governance/analysis/decisions/REPORT/`

Event
"P2 completed: REPORT v1 (as built) — 0 tables, 0 DBF, 2 XM (1 SPI, 1 registry-row write), 3 value sets"

Cascade
No registry XM row of another module targets REPORT with status DEFERRED — nothing to resolve. SEC's
analysis records the three registries (ENT-SEC-004…006); the rows REPORT contributes to them are
registered here (XM-REPORT-002) and in SEC's 1.2.0 addenda ("`SEC_REPORTS` … synchronized for the users
report", `../../SEC/P1/registry-srs-sec.md`).
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 11
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : none — the TENANT data export of `docs/plans/tenant-maturity-plan.md` C.5 does NOT use the report engine.
Statement      : This addendum records the deltas being implemented for 1.3.0; every row is verified against the code before the tag.

| # | Kind | Registry delta | Source |
|---|---|---|---|
| — | — | none | docs/plans/tenant-maturity-plan.md C.5 |
