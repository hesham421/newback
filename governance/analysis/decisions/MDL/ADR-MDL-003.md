# ADR-MDL-003 — container pattern for the registry browse screen, which has no entry sub-view

Module  : MDL     Version : v1     Stage raised : P3.2 (Frontend — UX Design + Execution Plan)
Status  : ACCEPTED (non-breaking)

## Context
§A.4's container-pattern decision order is written for entry screens: it chooses between
`TREE_MASTER_DETAIL`, `FULL_PAGE` and `SIDE_DRAWER` by the shape of the data a user edits.

`SCR-MDL-002` edits nothing. Its SRS §B3 reads "Read-only browse; no create/update here
(management happens on SCR-REQ-MDL-001)", its §B4 grants VIEW alone, and it is served by one
endpoint, API-MDL-010. Its content shape is nonetheless a hierarchy — owner module → its types
— so rule 1 would name `TREE_MASTER_DETAIL`, a two-pane pattern whose second pane is "a
permanently visible form" this screen does not have.

## Decision
`SCR-MDL-002` carries `FULL_PAGE (no entry sub-view — ADR-MDL-003)`, and the owner → types
hierarchy is rendered as the grouped list the endpoint returns (`OwnerGroupResponse[]`, each
group carrying its own `types[]`). No fourth pattern is invented, and `TREE_MASTER_DETAIL` is
not claimed for a screen with nothing in its detail pane.

`SCR-MDL-001` is unaffected: it has a real entry at both levels and takes
`TREE_MASTER_DETAIL` by rule 1 on its own merits.

## Consequences
- SCR-MDL-002 gets one route and one lazy chunk, with no `new`, `:id` or `:id/edit` route.
- Its filters are mirrored into the route's search params, so a filtered registry view is
  shareable — which is the only state it has.
- The grouping is the server's: the screen renders the groups API-MDL-010 returns and composes
  none of its own.
