## REGISTRY — P2 — EVENTS v1 (as-built baseline, erp-core 1.2.0)
══════════════════════════════════════════════════════════════════

Tables
| Table | ENT id | Kind | DBF range |
|---|---|---|---|
| (none) | ENT-EVENTS-001 is an in-memory value object | event bus, no persistence | — |

DBF ids: none assigned.

XM index
| XM-ID | Type | Surface / target | Physical object | Status |
|---|---|---|---|---|
| (none physical) | — | XM-EVENTS-001 (public root package) and XM-EVENTS-002 (consumed `TenantContext`) are Java surfaces, indexed in `../P1/registry-srs-events.md` | none | — |

Lookups
| Key | Seeded values count | Owner |
|---|---|---|
| envelope `realm` | 3 (String constants `STAFF`, `CUSTOMER`, `SYSTEM`; no rows, no CHECK of its own) | common (`SecurityContextHelper`), aliased by EVENTS |

Sequences
none. Last DBF: none · Last XM: none (physical)

Decisions
ADR-EVENTS-001, ADR-EVENTS-002, ADR-EVENTS-003 (ACCEPTED, as built) — see `governance/analysis/decisions/EVENTS/`

Event
"P2 completed: EVENTS v1 (as built) — 0 tables, 0 DBF, 0 XM, no outbox"

Cascade
No registry XM row of another module targets EVENTS with status DEFERRED — nothing to resolve. The
persisted side effects of the one core listener (`NOTIF_LOG`, `NOTIF_INBOX`) are NOTIF's tables,
registered in `../../NOTIF/P2/` (V13).
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 08
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0-SNAPSHOT (main, in progress)
Change         : none recorded here yet — the three events of the tenant-maturity plan (C.1, D.3) are documented by the implementing run as each package lands on main
Statement      : This addendum records only what is already on main for 1.3.0; the plan packages' rows are written by the implementing run.

Plan packages B, C, D, E, G: documented by each package as it lands on main (analysis-first, written by the implementing run); the verified reference rows are in docs/plans/tenant-maturity-analysis-reference.md.
