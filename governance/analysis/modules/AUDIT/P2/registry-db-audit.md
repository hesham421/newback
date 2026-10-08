## REGISTRY — P2 — AUDIT v1 (as-built baseline, erp-core 1.2.0)
══════════════════════════════════════════════════════════════════

Read from the core migration at main @ 19b19a4 (`erp-core/src/main/resources/db/migration/core/`;
`V15:line` = `V15__audit_schema.sql` and line). Full detail: `db-script-audit.md`.

Tables
| Table | ENT id | Kind | DBF range | Created by |
|---|---|---|---|---|
| CORE_AUDIT_EVENT | ENT-AUDIT-001 | log (tenant-scoped, append-only; JDBC writes, JPA reads) | DBF-AUDIT-001 … DBF-AUDIT-020 | V15:16-37 |

DBF ids → code location
| DBF id | Column | Type | Null | Constraint / FK | Index | Code location |
|---|---|---|---|---|---|---|
| DBF-AUDIT-001 | CORE_AUDIT_EVENT.ID | BIGINT | NOT NULL | `PK_CORE_AUDIT_EVENT`; `SEQ_CORE_AUDIT_EVENT` | (PK) | V15:17, :39, :14 |
| DBF-AUDIT-002 | CORE_AUDIT_EVENT.TENANT_ID | BIGINT | NOT NULL | `FK_CORE_AUDIT_EVENT_TENANT` | `IDX_CORE_AUDIT_EVENT_TENANT`; leads `IDX_CORE_AUDIT_EVENT_ENTITY`, `IDX_CORE_AUDIT_EVENT_OCCURRED` | V15:18, :40, :44-46 |
| DBF-AUDIT-003 | CORE_AUDIT_EVENT.OCCURRED_AT | TIMESTAMP (without time zone) | NOT NULL | — | `IDX_CORE_AUDIT_EVENT_OCCURRED` (2nd column) | V15:19, :46 |
| DBF-AUDIT-004 | CORE_AUDIT_EVENT.ACTOR | VARCHAR(100) | NOT NULL | — | — | V15:20 |
| DBF-AUDIT-005 | CORE_AUDIT_EVENT.ACTOR_REALM | VARCHAR(16) | NOT NULL | `CHK_CORE_AUDIT_EVENT_REALM` | — | V15:21, :42 |
| DBF-AUDIT-006 | CORE_AUDIT_EVENT.ACTOR_USER_ID | BIGINT | NULL | — (no FK) | — | V15:22 |
| DBF-AUDIT-007 | CORE_AUDIT_EVENT.ACTION | VARCHAR(64) | NOT NULL | `CHK_CORE_AUDIT_EVENT_ACTION` | — | V15:23, :41 |
| DBF-AUDIT-008 | CORE_AUDIT_EVENT.ENTITY_TYPE | VARCHAR(128) | NULL | — | `IDX_CORE_AUDIT_EVENT_ENTITY` (2nd column) | V15:24, :45 |
| DBF-AUDIT-009 | CORE_AUDIT_EVENT.ENTITY_ID | VARCHAR(64) | NULL | — | `IDX_CORE_AUDIT_EVENT_ENTITY` (3rd column) | V15:25, :45 |
| DBF-AUDIT-010 | CORE_AUDIT_EVENT.SUMMARY_AR | VARCHAR(1000) | NULL | — | — | V15:26 |
| DBF-AUDIT-011 | CORE_AUDIT_EVENT.SUMMARY_EN | VARCHAR(1000) | NULL | — | — | V15:27 |
| DBF-AUDIT-012 | CORE_AUDIT_EVENT.CHANGES | JSONB | NULL | — | — | V15:28 |
| DBF-AUDIT-013 | CORE_AUDIT_EVENT.IP | VARCHAR(64) | NULL | — | — | V15:29 |
| DBF-AUDIT-014 | CORE_AUDIT_EVENT.USER_AGENT | VARCHAR(256) | NULL | — | — | V15:30 |
| DBF-AUDIT-015 | CORE_AUDIT_EVENT.REFERENCE | VARCHAR(100) | NULL | — | — | V15:31 |
| DBF-AUDIT-016 | CORE_AUDIT_EVENT.CREATED_BY | VARCHAR(100) | NULL | — | — | V15:32 |
| DBF-AUDIT-017 | CORE_AUDIT_EVENT.CREATED_AT | TIMESTAMP (without time zone) | NULL | — | — | V15:33 |
| DBF-AUDIT-018 | CORE_AUDIT_EVENT.UPDATED_BY | VARCHAR(100) | NULL | — | — | V15:34 |
| DBF-AUDIT-019 | CORE_AUDIT_EVENT.UPDATED_AT | TIMESTAMP (without time zone) | NULL | — | — | V15:35 |
| DBF-AUDIT-020 | CORE_AUDIT_EVENT.VERSION | BIGINT DEFAULT 0 | NOT NULL | — | — | V15:36 |

Counts verified: 20 columns (V15:17-36); 1 PK, 1 FK, 2 CHECK, 3 indexes, 0 unique (V15:39-46). The
tenant column is one of the 22 counted by `erp-core/src/test/java/com/erp/tenant/TenantSchemaIntegrationTest.java:51`
(tenant-side register entry DBF-TENANT-032 in `../../TENANT/P2/db-script-tenant.md`). Timestamp columns
are `TIMESTAMP` without zone (ADR-AUDIT-003), unlike the `TIMESTAMPTZ` of the other core tables.

XM index
| XM-ID | Type | Surface | Target / implementers | Status |
|---|---|---|---|---|
| XM-AUDIT-001 | HARD-FK (consumed) | `TENANT_ID` → `CORE_TENANT(ID)`; `TenantContext.require()` | TENANT | ACTIVE |
| XM-AUDIT-002 | crossmodule call (exposed) | `AuditApi.record(AuditEntry)` | SEC (`SecAuditEntries`); applications | ACTIVE |
| XM-AUDIT-003 | annotation + listener (exposed) | `@Audited(entityType, ignore)` | SEC, TENANT, FILE, NOTIF, MDL, CU, SEQUENCE (10 entities); applications | ACTIVE |
| XM-AUDIT-004 | SPI implemented (consumed) | `ReportProvider` — `AuditEventListReport` (`AUDIT_EVENT_LIST`) | report | ACTIVE |

Lookups
| Key | Seeded values count | Owner |
|---|---|---|
| `CORE_AUDIT_EVENT.ACTOR_REALM` | 3 (CHECK-constrained `CHK_CORE_AUDIT_EVENT_REALM`, not seeded rows) | AUDIT |
| `CORE_AUDIT_EVENT.ACTION` | free code, regex `CHK_CORE_AUDIT_EVENT_ACTION`; 7 standard constants on `AuditApi` | AUDIT |

Sequences
`SEQ_CORE_AUDIT_EVENT` (V15:14; `CACHE 1`; `nextval` called in the JDBC insert, `audit/service/AuditEventStore.java:46`).
Last DBF: DBF-AUDIT-020 · Last XM: XM-AUDIT-004

Decisions
ADR-AUDIT-001, ADR-AUDIT-002, ADR-AUDIT-003 (ACCEPTED, as built) — see `governance/analysis/decisions/AUDIT/`

Event
"P2 completed: AUDIT v1 (as built) — 1 table, 1 sequence, 20 DBF, 4 XM"

Cascade
No XM row of another module targets `CORE_AUDIT_EVENT` with a FK. The seven modules that carry `@Audited`
and SEC's `AuditApi` calls are the consumers of XM-AUDIT-002/003; their own 1.2.0 addenda record the
dependency from their side (SEC, MDL, CU, FILE, NOTIF: "audit — SOFT, `@Audited`"; TENANT and SEQUENCE:
their module registries). The tenant-side register (`../../TENANT/P2/registry-db-tenant.md`,
DBF-TENANT-032) records the same `TENANT_ID` column from the tenant's point of view; this file is the
owning register.
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0)
Steps          : 10
Statement      : This artifact was written from the implemented code on 2026-10-07 (as-built); there is no earlier analysis, so the body above IS the implemented state and this addendum records no delta.
