# ADR-MDL-005 — operations the SRS names for which no endpoint is published are omitted, not faked

Module  : MDL     Version : v1     Stage raised : P3.2 (Frontend — UX Design + Execution Plan)
Status  : ACCEPTED (non-breaking)

## Context
`srs-mdl.md` Part B names operations that `_inputs/api-docs-mdl.md` publishes no endpoint for:

| Operation | Named in | Published |
|---|---|---|
| **activate** a lookup type or a lookup value | §B3 "Buttons: activate/deactivate per row at both levels" | nothing — `DELETE` deactivates, and no counterpart sets the flag back |
| **read** one lookup type by id | §B1 Operations "search, create, read, update, deactivate" | nothing — no `GET /lookup-types/{id}` |
| **read** one lookup value by id | §B1 Operations (value) | nothing — no `GET /lookup-values/{id}` |

The two by-id reads are the same deliberate v1 exclusion the other modules record: the search
response already returns the complete record (`LookupTypeResponse`, `LookupValueResponse`), so
a by-id read would expose no field the list does not already carry.

`activate` is different. It is named as a button in the SRS, and no REQ or AC asks for it:
REQ-MDL-004 and REQ-MDL-009 both state deactivation only, and AC-MDL-004 and AC-MDL-009 assert
`isActiveFl=false` with no re-activation step. So the button has no requirement behind it and
no endpoint under it.

## Decision
No affordance is drawn for an operation with no published endpoint. Both levels of
`SCR-MDL-001` show Deactivate alone — not a toggle whose second half would fail — and the
deactivate confirmation states plainly that the act is not reversible through this screen.

Entry forms hydrate from the row the search query already holds in cache, so the absent by-id
reads cost the screen nothing and no second read is performed.

## Consequences
- The operations coverage table in the plan's ALIGN-FE section carries a `✗` row with this ADR
  for each omitted operation — a `✗` for a missing endpoint, never for a missing decision.
- A type or value deactivated in error must be corrected in the database or by a later
  `activate` endpoint; the screen does not pretend otherwise.
- If an activate endpoint is later published, both levels gain the second half of the toggle
  and this ADR is superseded for those rows; nothing else in the plan changes.
- No `REQ-*` is unmet: none of the three operations is required by one.
