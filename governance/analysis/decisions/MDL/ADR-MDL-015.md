# ADR-MDL-015 — when the owner-module registry read is refused, the select falls back to the codes the browse already returned, never to free text

Module  : MDL     Version : v1     Stage raised : P3.2 (Frontend — UX Design + Execution Plan)
Status  : ACCEPTED (non-breaking)

## Context
ADR-MDL-013 recorded that `UXD-MDL-001` — the owner-module control — reads a surface with a
permission of its own (`PERM_SEC_MODULE_REGISTRY_VIEW`, on SEC's registry search), and chose the
degraded behaviour for `SCR-MDL-001`: on a refused or failed read the select is **empty and
disabled** and the create action is disabled behind it, because a code typed blind is a code
RULE-MDL-001 guarantees the server will refuse.

`SCR-MDL-002` uses the same hook for a different purpose, and the same answer is wrong there.
On the registry browse, `UXD-MDL-001` is not a field of a form that writes: it is the grouping
itself, the label on each group heading, and an optional `ownerModuleCode` filter over a screen
that submits nothing at all. Its one affordance is a link into `SCR-MDL-001`.

An empty, disabled filter on a screen that writes nothing would block browsing to protect a save
that does not exist. Worse, it would do so while the data needed to populate the control is
already in hand: `API-MDL-010` groups its results by owner and returns
`OwnerGroupResponse.ownerModuleCode` on every group, so the response itself carries the distinct
set of codes that currently own types.

## Decision
On `SCR-MDL-002`, when the `UXD-MDL-001` read is refused or fails, the `ownerModuleCode` filter
select **falls back to the distinct `ownerModuleCode` values present in the current
`API-MDL-010` response** — never to free text, and never to a code the registry does not
currently hold.

Group headings fall back to the bare code the browse already returns, unchanged. **Browsing is
never blocked by the degraded source.**

`SCR-MDL-001` is unaffected: ADR-MDL-013's empty-and-disabled behaviour stands there, because
that screen writes and this one does not.

## Consequences
- The two screens degrade differently on the same failed read, and each states why. The
  difference is the presence of a save, not an inconsistency.
- The fallback set is always a subset of what the user can already see. A code that is no longer
  registered but still owns types appears either way, because the grouping is the server's, not
  the frontend's.
- No free-text entry is introduced anywhere. Both this ADR and ADR-MDL-013 refuse it, for the
  same reason: a code the registry does not hold is a code the server will refuse.
- No unpublished surface is called. The fallback reads a field of a response the screen has
  already fetched, so C9.5 holds and the plan cites no foreign `API-*` id.
- The behaviour is checkable without SEC: refusing the registry read must still leave the browse
  navigable, its groups labelled by code, and its filter populated from the response.
