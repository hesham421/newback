# ADR-SEC-067 — Registry registration endpoints are gated by `PERM_SEC_MODULE_REGISTRY_UPDATE`

Module  : SEC     Version : erp-core 1.2.0 (as-built)     Stage raised : implementation record (analysis-coverage review, 2026-10-08)
Status  : ACCEPTED (non-breaking)

## Context
SCR-REQ-SEC-006 describes the registry screen as read-mostly: "registration itself happens via the
registering module's own onboarding call (REQ-SEC-016/017/019)", and B4 gives the screen the actions
VIEW and "UPDATE (deactivate only)". B5 nevertheless lists the three registration operations as
`POST /api/v1/sec/registry/modules`, `/screens` and `/actions` with no permission named. The code
had to choose who may call them:
- Unauthenticated or `isAuthenticated()` only, so that "a module's own call" works without a grant.
  Any signed-in user could then add catalog rows that other administrators grant.
- A dedicated CREATE permission on `SEC_MODULE_REGISTRY`. V7's matrix deliberately registers no
  CREATE action for that screen (`V7__sec_seed.sql:77-83`).
- The screen's existing UPDATE permission, reading "UPDATE" as "writes to the registry".

## Decision
The third. All three registration methods of `RegistryService` carry
`@PreAuthorize(hasAuthority(PERM_SEC_MODULE_REGISTRY_UPDATE))`
(`erp-core/src/main/java/com/erp/sec/service/RegistryService.java:73, 88, 110`), and
`SecPermissions.PERM_SEC_MODULE_REGISTRY_UPDATE` documents itself as the gate of API-SEC-018/019/020
(`SecPermissions.java:35-36`); V7's seed comment states the same
(`V7__sec_seed.sql:83`). Registration normalises codes (trim + upper-case), refuses a screen under an
unregistered module (RULE-SEC-004, `SEC-409-MODULE-NOT-REGISTERED`) and an action under an
unregistered screen (`SEC-409-SCREEN-NOT-REGISTERED`), and writes no audit row. The deactivation the
analysis reserved UPDATE for was never built: no endpoint toggles `IS_ACTIVE_FL` on the three
registries, and the startup `PermissionCatalogSynchronizer` never does either (ADR-SEC-038 records
registry-row deactivate as DEFERRED). Since step 06 the normal way a module enters the catalog is its
`PermissionContributor`, upserted at startup; the endpoints remain the administrative path for a
module that is not part of the running application.

## Consequences
- SCR-REQ-SEC-006 B4 reads, as built: VIEW (search) and UPDATE (register module / screen / action);
  there is no deactivate affordance and the frontend must not draw one as working (ADR-SEC-038).
- The analysis's "the module's own onboarding call" is served by the contributor SPI, not by an
  unauthenticated endpoint; a consuming application registers its catalog in code.
- `PERM_SEC_ROLES_DELETE` and the deactivate half of `PERM_SEC_MODULE_REGISTRY_UPDATE` stay registered
  and granted to `SYS_ADMIN` with no gate consuming them; adding a deactivate endpoint later needs no
  new permission.
- Non-breaking: no table, column or error code changes; the permission existed in V7.

## Traces
REQ-SEC-016, REQ-SEC-017, REQ-SEC-018, REQ-SEC-019 · RULE-SEC-004 · ENT-SEC-004, ENT-SEC-005,
ENT-SEC-006 · SCR-REQ-SEC-006 · ADR-SEC-038 · docs/steps/06-report.md (PermissionCatalogSynchronizer) ·
docs/DEVIATIONS.md [06]
