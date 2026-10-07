# ADR-SEC-008 — screen operations the SRS names for which no endpoint is published

Module  : SEC     Version : v1     Stage raised : P3.2 (Frontend — UX Design + Execution Plan)
Status  : ACCEPTED (non-breaking)

## Context
SRS Part B lists, under each screen's `B1 — Operations`, work that the published api-docs
expose no endpoint for. Under the §3.0 binding rule the frontend may not invent one, and
under §7 a missing **core** operation would be breaking — so each gap is tested against the
`REQ-*` set rather than against the Part B prose:

| Gap | Screen | Named in | Required by a REQ? |
|---|---|---|---|
| update a role's own fields | SCR-SEC-005 | B1 "update (role)" | no — no `REQ-*` states it |
| deactivate a role | SCR-SEC-005 | B1 "deactivate (role)" | no |
| revoke a **screen** grant individually | SCR-SEC-005 | B3 grant tree un-check | no — REQ-SEC-015 requires the module-level cascade only, and `API-SEC-015` performs it |
| revoke an **action** grant individually | SCR-SEC-005 | B3 grant tree un-check | no — same |
| deactivate a stale registry row | SCR-SEC-006 | B1 / B3 "deactivating a stale row" | no |
| read one user by id | SCR-SEC-004 | implied by the entry form | no |
| logout | — | `AUDIT_EVENT_TYPE.LOGOUT` | no — no `REQ-*` states it |

No `REQ-SEC-001..033` loses its endpoint: every requirement's core operation is published
(see the API ID BINDING annex — 27 of 27 bound). The gaps are Part B operations and one
audit event type, not requirements.

## Decision
Each gap is **omitted from the frontend**, never faked and never routed to a substitute:

- the Roles screen offers create, search and grant editing; no role edit form and no role
  deactivate affordance are drawn;
- the grant tree's only revoke is at module level (`API-SEC-015`), and the tree states this
  where a reader would otherwise expect a per-node un-check — un-checking a screen or action
  node is not offered;
- the Registry screen is read-only (search + the three register calls a consuming module
  makes for itself); no deactivate affordance is drawn;
- the Users entry form hydrates from the row already held by the `API-SEC-005` search
  query's cache instead of a by-id call, and the cache key is the one the list uses, so an
  edit invalidates and re-reads through the same key;
- no logout affordance is drawn; the session ends by expiry (`expiresIn` from `API-SEC-001`)
  or by an administrator's `API-SEC-026` termination.

## Consequences
- The delivered UI is smaller than SRS Part B's prose and exactly as large as the published
  API. A reviewer comparing the two finds the difference here rather than discovering an
  affordance that 404s.
- Each row above is a one-line frontend addition once its endpoint is published; none needs
  a route, a screen or an `SCR-*` that does not already exist.
- The ALIGN-FE operations-coverage table carries these as explicit ✗ rows citing this ADR,
  not as omissions.

## Traces
REQ-SEC-009, REQ-SEC-012, REQ-SEC-013, REQ-SEC-014, REQ-SEC-015, REQ-SEC-016, REQ-SEC-017,
REQ-SEC-019, REQ-SEC-024 · API-SEC-005, API-SEC-013, API-SEC-015, API-SEC-021 ·
SCR-SEC-004, SCR-SEC-005, SCR-SEC-006
