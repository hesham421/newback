# ADR-SEC-005 — no endpoint publishes the caller's action-level permissions; the UI gates on VIEW and trusts the server for the rest

Module  : SEC     Version : v1     Stage raised : P3.2 (Frontend — UX Design + Execution Plan)
Status  : ACCEPTED (non-breaking)

## Context
RF5 requires, per screen, a navigation guard on `VIEW` and per-action affordance behaviour
for `CREATE`, `UPDATE`, `DELETE` — all keyed by the `PERM_<PAGE_CODE>_<ACTION>` names the
backend declares. The published api-docs expose exactly two sources of the caller's own
access:

- `API-SEC-027` GET `/api/v1/sec/menu` → the effective **module → screen** tree
  (`code`, `pageCode`, names). Screen level, `isAuthenticated()`.
- every guarded endpoint's own `403 ACCESS_DENIED` response.

There is no "my permissions" / "my effective actions" endpoint, so the client cannot read
whether it holds `PERM_SEC_USERS_CREATE` before attempting the call. A value not in the
api-docs is never invented (engine §3.0).

## Decision
1. **Screen-level gating is real and client-side.** A route's guard admits the caller only
   when the screen's `pageCode` appears in the `API-SEC-027` menu response; otherwise the
   unauthorized redirect. This is the `VIEW` gate, and it is exact — the menu is built from
   the same effective grants the server enforces (REQ-SEC-021, REQ-SEC-032).
2. **Action-level affordances are optimistic.** `CREATE` / `UPDATE` / `DELETE` affordances
   render for a caller who holds the screen, and the authority is the server's `403
   ACCESS_DENIED`, surfaced as the localized forbidden message rather than a silent no-op.
   The affordance is never hidden on a guess.
3. Every RF5 block states which of the two mechanisms gates it, per action. No permission
   name is redeclared by the frontend; the names used are the ones the api-docs attribute to
   each endpoint (`PERM_SEC_USERS_VIEW/CREATE/UPDATE`, `PERM_SEC_ROLES_VIEW/CREATE/UPDATE`,
   `PERM_SEC_MODULE_REGISTRY_VIEW/UPDATE`, `PERM_SEC_DASHBOARD_VIEW`,
   `PERM_SEC_AUDIT_LOG_VIEW`, `PERM_SEC_SESSIONS_VIEW/DELETE`).

## Consequences
- REQ-SEC-033 is unharmed: the module gate is enforced server-side on every request, and the
  SRS itself says access is "blocked up front, not merely hidden". The client gate is a
  usability layer over it, never the enforcement.
- REQ-SEC-023 (hide an ungranted dashboard widget) is satisfied by the server, not the
  client: `API-SEC-022` returns only the widgets the caller may see, so a widget is rendered
  when and only when its field is present in `DashboardResponse`. The client renders what it
  is given and never asks for a permission it cannot read.
  **This rests on an assumption, and it is the weakest point of this decision.** The api-docs
  annotate per-widget permission for exactly one of the six widgets — `recentActivity`
  ("requires SEC_AUDIT_LOG VIEW"). The other five carry no annotation; they are optional, and
  every property of that response is optional, so optionality is not evidence of gating. If
  the server gates only `recentActivity`, five widgets leak to a caller who should not see
  them and REQ-SEC-023 fails silently — there is no client-side check left to catch it,
  because clause 2 above establishes that none is readable. The F2 block for API-SEC-022
  carries this as PENDING. The fix belongs to the backend: annotate all six, or state that
  the omission is uniform. Adding a client-side test is not the fix and is not available.
- A caller lacking, say, `PERM_SEC_USERS_CREATE` sees a "New user" button that fails with a
  localized forbidden message. This is the honest cost of the gap and is visible to the user
  rather than hidden from the reviewer.
- Superseded the moment the backend publishes an effective-permissions endpoint: the RF5
  blocks then read it, and clauses 2 and 3 of this decision fall away without touching a
  route, a facade or a permission name.

## Traces
REQ-SEC-021, REQ-SEC-023, REQ-SEC-030, REQ-SEC-032, REQ-SEC-033 · AC-SEC-023, AC-SEC-030,
AC-SEC-032, AC-SEC-033 · RULE-SEC-007 · API-SEC-022, API-SEC-027 ·
SCR-SEC-004, SCR-SEC-005, SCR-SEC-006, SCR-SEC-007, SCR-SEC-008, SCR-SEC-009, SCR-SEC-010
