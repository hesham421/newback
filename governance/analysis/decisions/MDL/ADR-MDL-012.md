# ADR-MDL-012 — no published surface tells a screen which ACTIONS its caller holds, so no affordance is hidden on a guess

Module  : MDL     Version : v1     Stage raised : P3.2 (Frontend — UX Design + Execution Plan)
Status  : ACCEPTED (non-breaking)

## Context
The security half of a frontend plan states, per screen, the navigation guard and the UI
behaviour per action: without VIEW the affordance is hidden or read-only, and the same for
CREATE, UPDATE and DELETE. That phrasing presumes the client can read the caller's grants.

What the platform actually publishes, read off the two api-docs this stage is allowed to read:

| Surface | What it returns | Granularity |
|---|---|---|
| the security module's effective menu | the modules and the **page codes** granted to the caller | the screen — `MDL_LOOKUPS`, `MDL_TYPE_REGISTRY` |
| the security module's registry search | the actions **defined** on a screen and their derived permission codes | the registry, not the caller |
| every MDL endpoint | its own result, or `ACCESS_DENIED` (403) | per call, after the fact |

The gateway action is the only one a client can evaluate before rendering: `VIEW` is "this
screen's page code is in my effective menu". `CREATE`, `UPDATE` and `DELETE` are enforced per
endpoint (`PERM_MDL_LOOKUPS_CREATE`, `_UPDATE`, `_DELETE` — the api-docs name each endpoint's
requirement) and are readable by no caller-scoped surface. The registry search answers which
actions *exist*, never which ones *this* caller was granted.

A screen that hid its Save button on an action permission it cannot read would be hiding it on
a guess — and the guess fails in the dangerous direction as readily as the safe one: a manager
who holds CREATE would be shown a screen with no way to create, with nothing saying why.

## Decision
Per `SCR-*`, the frontend states exactly what it can evaluate:

- **VIEW** — the gateway, and a real gate: a caller whose effective menu lacks the screen's page
  code never reaches the route. The guard redirects to the platform's unauthorized destination;
  it is not a hidden button.
- **CREATE / UPDATE / DELETE** — not readable. The affordance renders for a caller who holds the
  screen, the call is made, and a `403 ACCESS_DENIED` is shown as its localized catalog message
  on the surface that attempted it. No affordance is hidden and no field is pre-emptively
  disabled on an unreadable permission.

Read-only-ness that comes from the **data** rather than from a grant is unaffected and stays
declared where it belongs: `key`, `ownerModuleCode` and `code` are read-only on edit because no
published update request carries them (ADR-MDL-006), not because of a permission.

## Consequences
- The SEC-FE phase names all five permissions of the two screens — they are the backend
  registry's names, cited, never redeclared — and states for each action which of the two
  mechanisms answers it: the menu gate, or the server's 403.
- A caller who holds `MDL_LOOKUPS` VIEW alone sees the management screen and is refused on
  submit. That is a worse first experience than a hidden button and a better one than a hidden
  button that was hidden wrongly; it is also the only behaviour the published surface supports.
- If the security module later publishes the caller's effective **permission codes** (it
  publishes the effective menu today), this ADR is superseded and the per-action affordances
  bind to it. Nothing else in the plan changes: the guard, the routes and the facades are
  already per action.
- Access narrows, never widens: a failure to load the effective menu renders no MDL entry and
  grants no MDL route.
