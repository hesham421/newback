# ADR-MDL-016 — SCR-MDL-001's master-list owner-module filter degrades like a filter, not like the create-form select
Status: ACCEPTED (non-breaking)
Module: MDL   Version: v1   Raised at: P3.2, gate `pass-2` revision (round 3, G1)

## Context
`UXD-MDL-001` (the owner-module data read from SEC's `ModuleRegistry`) backs two controls on
SCR-MDL-001: the create-form's owner-module select, and the master list's read-only
`ownerModuleCode` search filter. The frontend-execution-plan and ui-ux-spec previously stated one
degraded behaviour for both — "a refused read leaves the select empty and disabled, and the
create action disabled behind it" (ADR-MDL-013) — leaving the filter's own behaviour on a refused
read unspecified, with the nearest stated text implying the same empty-and-disabled treatment.

`PERM_SEC_MODULE_REGISTRY_VIEW`, the grant behind a refused read, is tied by ADR-MDL-013 and
SEC-FE to `PERM_MDL_LOOKUPS_CREATE` alone. A caller who holds `PERM_MDL_LOOKUPS_VIEW` without
`PERM_MDL_LOOKUPS_CREATE` — SRS §B4's consuming-module service account, or a reviewer holding the
screen without CREATE — is expected not to hold the SEC grant either: the degraded path is that
caller's *normal* path, not an edge case. As written, it cost that caller a filter their own
permissions never gated in the first place.

The sibling screen, SCR-MDL-002, already answers the identical failure differently: ADR-MDL-015
falls back its owner-module filter to the distinct `ownerModuleCode` values present in the
current `API-MDL-010` response, specifically so that "browsing is never blocked by the degraded
source." `API-MDL-001` (the master-list search) returns `ownerModuleCode` on every row already
(F1-MODEL, `LookupTypeResponse`), so the same fallback data SCR-MDL-002 uses is already in hand
for SCR-MDL-001's own filter. ADR-MDL-013's own reasoning — "a code typed blind is a code the
server is certain to refuse" (RULE-MDL-001) — is an argument about the *write* path (the create
form must not offer a value the server cannot accept) and does not reach a *read* filter over
rows the server has already returned.

## Decision
`UXD-MDL-001` now has two degraded behaviours on SCR-MDL-001, one per control, not one shared
between them:
- **create-form select** — unchanged (ADR-MDL-013): a refused or failed read leaves the select
  empty and disabled, and the create action disabled behind it. This is the only place the field
  is ever a value the server could refuse.
- **master-list search filter** — a refused or failed read leaves the filter falling back to the
  distinct `ownerModuleCode` values already present in the current `API-MDL-001` response,
  exactly as ADR-MDL-015 already does for SCR-MDL-002's filter. Never free text, never disabled.
  Browsing the type list is never blocked by the degraded source.

## Rationale
The failure mode a caller meets on the filter is not the failure mode ADR-MDL-013 was written
for: nothing is being submitted, nothing can be refused, and the data the fallback needs is
already sitting in the very rows the filter is scoped over. Disabling a read-only filter because
a foreign write-time safeguard fired denies a capability to exactly the callers — VIEW-only,
no CREATE — who were never going to hit that safeguard. Matching SCR-MDL-002's already-accepted
pattern is the non-arbitrary choice: the same data source, the same shape of control (a filter
over rows already on screen), the same failure, the same fallback.

## Consequences
- `frontend-execution-plan-mdl.md` F2-SCREEN-INIT (SCR-MDL-001) and F4-SCREEN Cold-load section
  are unaffected by this ADR directly, but the "Foreign data" line is split into the two
  controls' separate behaviours.
- `ui-ux-spec-mdl.md` UXD-MDL-001's Degraded (SCR-MDL-001) line is split the same way.
- No backend change, no new endpoint, no new permission: the fallback uses data `API-MDL-001`
  already returns on every row.
