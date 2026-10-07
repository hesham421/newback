# ADR-MDL-007 — API-MDL-011 (read values by key) is bound but called by no screen

Module  : MDL     Version : v1     Stage raised : P3.2 (Frontend — UX Design + Execution Plan)
Status  : ACCEPTED (non-breaking)

## Context
`GET /api/v1/mdl/lookups?type=<key>` (API-MDL-011) returns a lookup type's active values,
ordered by sort order. REQ-MDL-011 is written from the caller's side — "When a **consuming
module** requests the active values of a lookup type by key" — and REQ-MDL-012 is the
not-found answer that same caller receives for an unknown key. The backend plan's own block
says it plainly: "called by other modules' backends, not an end-user screen", gated by
`PERM_MDL_LOOKUPS_VIEW` granted to the calling module's service principal.

`srs-mdl.md` SCR-REQ-MDL-002 §B5 lists it because that is where the SRS records the consumer
contract, not because the registry screen calls it.

MDL's own screens do not need it: SCR-MDL-001 manages values through API-MDL-005 (the full
value rows, active and inactive alike, which a manager must see), and SCR-MDL-002 renders the
groups API-MDL-010 returns.

## Decision
API-MDL-011 is **bound** — it has an F2 block on SCR-MDL-002 stating its verb, path, request,
response and error routing, so the published surface is completely accounted for — and is
**called by no screen**. No "preview this type's values" affordance is invented for it: the
SRS asks for none, and the values a manager needs are already on SCR-MDL-001 through a read
that does not hide the inactive ones.

## Consequences
- The operations coverage table carries API-MDL-011 with a `✗` route and this ADR.
- REQ-MDL-011 and REQ-MDL-012 stay covered in the only way a UI can cover them: the values and
  the active flag that decide what the consumer read returns are managed on SCR-MDL-001, and
  the type whose deactivation hides them (RULE-MDL-004) is deactivated there too.
- The frontend of every **consuming** module reaches this endpoint through its own
  cross-module dependency, not through MDL's screens — as FIN's `UXD-FIN-001..012` do.
