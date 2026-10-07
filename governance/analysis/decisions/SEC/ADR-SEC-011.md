# ADR-SEC-011 — SRS Part B's Authorization / Monitoring grouping is not renderable; the two-tier menu wins

Module  : SEC     Version : v1     Stage raised : P3.2 (Frontend — UX Design + Execution Plan)
Status  : ACCEPTED (non-breaking)

## Context
SRS Part B gives six screens a three-segment navigation path:

| Screen | SRS B1 Navigation |
|---|---|
| SCR-SEC-004 Users | SEC → **Authorization** → Users |
| SCR-SEC-005 Roles & permissions | SEC → **Authorization** → Roles & permissions |
| SCR-SEC-006 Module/screen/action registry | SEC → **Authorization** → Module/screen/action registry |
| SCR-SEC-007 Admin dashboard | SEC → **Monitoring** → Dashboard |
| SCR-SEC-008 Audit log | SEC → **Monitoring** → Audit log |
| SCR-SEC-009 Active sessions | SEC → **Monitoring** → Active sessions |

Two authorities contradict that middle segment:

- **REQ-SEC-021 / AC-SEC-021** define the menu as exactly two tiers — "only the modules that
  user's effective grants hold, each showing only that user's effective granted screens
  beneath it". The screen requirement itself is titled "Dynamic **two-tier** menu"
  (SCR-REQ-SEC-010).
- **API-SEC-027** returns `ModuleMenuResponse[]` — `{ moduleRegPk, code, nameAr, nameEn,
  screens[] }` — with no group level, and nothing in the registry entities
  (ENT-SEC-004/005/006) records one: a screen belongs to a module, not to a group.

So the grouping is prose in Part B that neither the requirement nor the published surface can
express. It is also not a securable thing: `SEC_PAGES` has a row per screen, and `Authorization`
and `Monitoring` have no page code, no permission and no registry row.

## Decision
**REQ-SEC-021 wins.** The menu renders two tiers, module → screen, exactly as `API-SEC-027`
returns them, and the `Authorization` / `Monitoring` grouping is dropped in v1. Routes are
flat under the module segment (`/security/users`, `/security/dashboard`, …) rather than
carrying a segment the menu cannot produce, so a route path and a menu entry always agree.

## Consequences
- Six screens sit directly under SEC in the menu instead of in two groups of three. On a
  ten-screen module that is a short list; the grouping would earn its place only once a module
  has enough screens to need it, and SEC does not.
- No client-side grouping table is written. Composing the groups locally is the obvious
  shortcut and it is refused here for the same reason the whole menu is server-derived: a
  local table encodes membership the server never sent, and it outlives any change to it.
- Restoring the grouping is a **menu-contract change, not a frontend change** — a group tier in
  `ModuleMenuResponse` and a registry row to hang it on. Until then no amount of frontend work
  can render it correctly for a caller whose grants the client cannot see.
- Non-breaking: no `REQ-*` loses its screen, no screen loses its route, and the Part B
  Navigation lines remain accurate about *where a screen belongs*, only not about how many
  segments the menu draws.

## Traces
REQ-SEC-021, REQ-SEC-032, REQ-SEC-033 · AC-SEC-021 · SCR-SEC-004, SCR-SEC-005, SCR-SEC-006,
SCR-SEC-007, SCR-SEC-008, SCR-SEC-009, SCR-SEC-010 · API-SEC-027 · ENT-SEC-004, ENT-SEC-005
