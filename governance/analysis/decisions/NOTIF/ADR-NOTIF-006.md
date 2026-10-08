# ADR-NOTIF-006 — Lookup values served from MDL

Module  : NOTIF    Version : erp-core 1.2.0    Stage raised : P3 (implementation — step 04; MDL seed V8)
Status  : ACCEPTED (non-breaking)

## Context
SRS A5 and the P0 module registry declare the two NOTIF value lists (`NOTIF_CHANNEL`, `NOTIF_STATUS`) as
"local" runtime-loaded code lists — "no MD_MASTER_LOOKUP central, no MasterData module in this domain" —
and P2 BLOCK 8 seeds nothing. The platform then built MDL as the governed lookup hub (the same
architecture rule that ADR-SEC-001 deferred for SEC because MDL did not exist yet). By the time the NOTIF
migrations were squashed (step 04) MDL existed, and keeping two sources for the same codes — a NOTIF list
and an MDL type — would have violated the single-hub rule.

## Decision
`NOTIF_CHANNEL` and `NOTIF_STATUS` are MDL lookup types with owner module `NOTIF`, seeded by MDL's
`V8__mdl_seed.sql` (the analysed five channels and four statuses) and extended by NOTIF's V13 (`IN_APP`,
`QUEUED`, `SKIPPED_NO_PROVIDER`), per tenant. API-NOTIF-006 (`GET /api/v1/notifications/lookups/{lookupKey}`)
stays a NOTIF endpoint: `NotificationLookupService` accepts only its two keys, reads the active values live
through `MdlLookupApi.readActiveValuesByKey`, and translates any other key — or MDL's own not-found for a
tenant where the type is missing — into NOTIF's `NOTIF_LOOKUP_KEY_UNKNOWN` (404), so MDL's error codes never
leak through NOTIF's API. The stored columns remain plain codes with no CHECK and no FK to MDL (`NOTIF_LOG`,
`NOTIF_CHANNEL_CONFIG`).

## Consequences
- Adding a channel or status label is tenant data in MDL, not a NOTIF code change; but the codes NOTIF's
  code relies on (`NotifChannels`, `NotificationLogDomain.STATUS_*`) must match the seeds, and a channel code
  typed into `NOTIF_CHANNEL_CONFIG` is not validated against the list (RULE-NOTIF-013 only normalises it).
- `PENDING` stays in the MDL list although it is never stored since step 08 (a filter on it returns
  nothing); removing it is an MDL data change, left as is.
- New tenants get the values through MDL's provisioning contributor; NOTIF's contributor copies only
  configurations and templates.
- The report `NOTIF_LOG_SUMMARY` declares `channel` and `status` as LOOKUP params on the same MDL types.
- 1.3.0 moves the response type to the shared `com.erp.common.lookup.LookupOptionResponse` /
  `OwnedLookups` (same JSON); FILE uses the same mechanism.

## Traces
LOV-NOTIF-001, LOV-NOTIF-002 · API-NOTIF-006 · DBF-0003, DBF-0004, DBF-0023 · srs.md addendum §1, §6 ·
db-script.md addendum "Migration chain", "Seeds" · code:
`erp-core/src/main/java/com/erp/notif/service/NotificationLookupService.java:31-57`,
`controller/NotificationLookupController.java`, `report/NotifLogSummaryReport.java:74-75`,
`db/migration/core/V8__mdl_seed.sql:28-29, 42-52`, `V13__notif_async_inbox.sql:68-80` · ADR-SEC-001 (the
contrary, earlier choice for SEC) · docs/steps/08-report.md · DEVIATIONS [08], [14], [15]
