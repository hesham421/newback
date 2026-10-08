# DATABASE — أحداث المجال / Events (EVENTS)
══════════════════════════════════════════════════════════════════
Module : EVENTS   Version : v1 (as-built baseline, erp-core 1.2.0)   Dialect : postgresql16   Schema prefix : none
Identifier transformation : not applicable — the module creates and reads no physical object.
Date : 2026-10-07
Counts : 0 tables · 0 sequences · 0 DBF · 0 XM (physical) · 1 value set (String constants)
══════════════════════════════════════════════════════════════════

**No table.** The event bus owns no migration, no table, no sequence, no entity, no repository and no
JDBC: events are in-process Spring application events, delivered after the commit of the publisher's
transaction and never persisted (`erp-core/src/main/java/com/erp/events/support/SpringDomainEventPublisher.java:24-29`;
`docs/steps/08-report.md` Summary; ADR-EVENTS-001). The core Flyway chain `V2..V15` contains no
`EVENTS` object; step 08's only migration, `V13__notif_async_inbox.sql`, belongs to NOTIF
(`NOTIF_LOG` columns, `NOTIF_INBOX`) and is registered in `../../NOTIF/P2/`. Java paths below are
relative to `erp-core/src/main/java/com/erp/`, `file:line` at main @ 19b19a4.

## 1. DB FIELD TRACEABILITY MATRIX — EVENTS v1

No DBF id: no column is owned by the module. ENT-EVENTS-001 (`DomainEvent`) is an immutable value
object (`../P1/srs-events.md` A3) that exists only between a publish and the end of its last listener.

### 1a. Value set (String constants, no CHECK constraint, no lookup row)
| Value set | Values | Where it appears | Source |
|---|---|---|---|
| envelope `realm` | `STAFF`, `CUSTOMER`, `SYSTEM` | `DomainEvent.getRealm()`; mirrored by AUDIT's `CORE_AUDIT_EVENT.ACTOR_REALM` (`CHK_CORE_AUDIT_EVENT_REALM`, owned by AUDIT, `V15__audit_schema.sql`) and SEC's `SEC_USER.REALM` (`STAFF`, `CUSTOMER` only, `V11__sec_realms.sql`) — both aliased from the same `SecurityContextHelper.REALM_*` constants | events/DomainEvent.java:36-43; common/util/SecurityContextHelper.java:11-18; audit/crossmodule/AuditApi.java:28-30 |

### 1b. Persisted side effects of the bus (owned by other modules, listed for traceability)
| Event | What a listener persists | Table (owner) | Source |
|---|---|---|---|
| `NotificationRequestedEvent` | the delivery attempt: `NOTIF_LOG.ATTEMPTS`, `NEXT_ATTEMPT_AT`, `LAST_ERROR`, `NOTIFICATION_STATUS_ID`, `VARIABLES_JSON` cleared; `NOTIF_INBOX` rows for the `IN_APP` channel | `NOTIF_LOG`, `NOTIF_INBOX` (NOTIF, `V13__notif_async_inbox.sql`) | notif/service/NotificationDeliveryListener.java:46-71; notif/service/NotificationDeliveryProcessor.java |
| the other 9 events | nothing in core (no core listener) | — | `../P0/module-registry-events.md` EVENT CATALOGUE OWNED |

## 2. XM REGISTER — EVENTS v1

The module holds no FK and no SOFT-READ of any table. Its cross-module facts are Java surfaces only and
are indexed in `../P1/registry-srs-events.md` (XM-EVENTS-001 the public root package, XM-EVENTS-002 the
consumed `TenantContext`). Nothing to register physically.

| XM-ID | Type | Surface | Target / implementers | Physical object | Status |
|---|---|---|---|---|---|
| (none physical) | — | — | — | — | — |

## 3. FULL_DATABASE_SCRIPT (as built)

```sql
-- ============================================================
-- EVENTS — no DDL. The event bus owns no table, sequence, constraint,
-- index, trigger, view, function or seed; no outbox. Events live in
-- memory from publish to the end of their last listener (ADR-EVENTS-001).
-- ============================================================
```

## 4. Deviations from this analysis
None — written from the code.
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
