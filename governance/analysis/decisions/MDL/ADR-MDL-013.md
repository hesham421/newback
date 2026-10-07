# ADR-MDL-013 — the owner-module select reads a surface with its own permission, so the grant travels with the screen

Module  : MDL     Version : v1     Stage raised : P3.2 (Frontend — UX Design + Execution Plan)
Status  : ACCEPTED (non-breaking)

## Context
ADR-MDL-004 minted `UXD-MDL-001` and made the owner-module control a **select over the
registered module codes** rather than a free-text field, read from the security module's
registry search (`API-SEC-021`). What that decision did not state, because the foreign api-docs
had not been read for it, is that the foreign read carries a permission of its own:

> **Required permission(s)**: PERM_SEC_MODULE_REGISTRY_VIEW (found on service:RegistryService)

MDL's own screen is granted through `PERM_MDL_LOOKUPS_*` on page `MDL_LOOKUPS`. Nothing about
holding those makes a caller able to read SEC's registry. So a lookup manager granted CREATE on
`MDL_LOOKUPS` and nothing in SEC reaches the create form and finds the one required field it
cannot fill — while RULE-MDL-001 guarantees the server will refuse whatever they type.

This is a dependency between two **grants**, not between two designs, and it is invisible to
each module's own analysis: SEC's registry endpoint is correct, MDL's screen is correct, and
the pair is unusable.

## Decision
The dependency is recorded as a property of `UXD-MDL-001`, in `ui-ux-spec-mdl.md` where the
`UXD-*` is defined and in the registry's UXD INDEX: **every role granted `PERM_MDL_LOOKUPS_CREATE`
must also hold `PERM_SEC_MODULE_REGISTRY_VIEW`.** The grant is SEC's to make — this stage names
it and mints nothing.

The screen's behaviour when the grant is missing is the degraded behaviour ADR-MDL-004 already
chose, stated for this specific cause: a refused or failed registry read leaves the select
**empty and disabled**, with a message naming the missing read, and the create action disabled
behind it. The screen does not fall back to free text, because a code typed blind is a code the
server is certain to refuse (RULE-MDL-001) — a message about a missing grant is a better answer
than a rejected save.

`SCR-MDL-002` is unaffected in kind but not in degree: it uses the same hook for the owner
column's display labels, and a refused read leaves the group headings showing the bare code the
registry browse already returns (`OwnerGroupResponse.ownerModuleCode`). Browsing is never
blocked by it — only the labels are poorer.

## Consequences
- No MDL artifact grants anything. The requirement is stated where a reader of the delivered
  package meets it, and SEC's BOOTSTRAP DATA is where the grant itself is made.
- The frontend cites no `API-SEC-*` id and no SEC path: C9.5 requires every `API-*` in the plan
  to be defined in MDL's api-docs, so the plan cites `UXD-MDL-001` and the foreign endpoint is
  named only in `ui-ux-spec-mdl.md` (ADR-MDL-004, ADR-MDL-011).
- One shared, long-lived hook serves both screens; a single refusal is therefore visible in both
  places at once and is reported once, not per control.
- The pair of grants is a fact the api-verify stage can confirm and a gate reviewer can check:
  it names two permissions that both exist in their owners' registries.
