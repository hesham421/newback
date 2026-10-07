# ADR-SEC-007 — §A.4's container patterns apply to entry screens; screens with no entry sub-view take FULL_PAGE

Module  : SEC     Version : v1     Stage raised : P3.2 (Frontend — UX Design + Execution Plan)
Status  : ACCEPTED (non-breaking)

## Context
The engine's §A.4 decision order — hierarchical → `TREE_MASTER_DETAIL`; header + repeating
line items with a computed total → `FULL_PAGE`; otherwise → `SIDE_DRAWER` — is stated for
**entry screens**, and its fallback assumes a Search + Entry composite where the drawer opens
over a host list. Six of SEC's ten screens have no entry sub-view at all:

- `SCR-SEC-001` Login, `SCR-SEC-002` Sign-up, `SCR-SEC-003` Forgot/reset password —
  public, pre-authentication pages with no host list to open a drawer over;
- `SCR-SEC-007` Admin dashboard — a read-only widget grid (SRS content shape "other");
- `SCR-SEC-008` Audit log — append-only, never hand-created (SRS B3: not applicable);
- `SCR-SEC-009` Active sessions — a live list whose only mutation is a per-row terminate.

Taking rule 3 literally would put a login form in a side drawer with nothing behind it. The
engine's own guidance for a screen fitting no pattern is to re-read the SRS field list, not
to invent a fourth pattern — and the SRS is unambiguous that these screens have no entry form.

## Decision
`Container pattern` is recorded for the screens that actually have an entry sub-view, by the
unmodified §A.4 order:

| SCR | Entry sub-view | Container pattern | §A.4 rule |
|---|---|---|---|
| SCR-SEC-004 Users | yes (Search + Entry) | `SIDE_DRAWER` | rule 3 |
| SCR-SEC-005 Roles & permissions | yes (Master + grant tree) | `TREE_MASTER_DETAIL` | rule 1 |
| SCR-SEC-006 Module/screen/action registry | yes (master-detail tree) | `TREE_MASTER_DETAIL` | rule 1 |

The six screens above take `FULL_PAGE` as their **route shape**, recorded as
`FULL_PAGE (no entry sub-view — ADR-SEC-007)`, and `SCR-SEC-010` (the dynamic menu) records
none: it is a global shell component with no route of its own.

## Consequences
- No fourth pattern is introduced, and no screen that does have an entry form escapes the
  §A.4 order.
- `SCR-SEC-003` remains ONE screen with a two-step wizard inside it (composite-screen rule),
  rendered full page; the step is a route param, not local-only state.
- The RF4 component naming rule is applied per recorded pattern: `SIDE_DRAWER` →
  SearchPage + FormDrawer; `TREE_MASTER_DETAIL` → TreePage; `FULL_PAGE` with no entry → a
  single Page component and no Entry route.

## Traces
REQ-SEC-001, REQ-SEC-003, REQ-SEC-006, REQ-SEC-009, REQ-SEC-012, REQ-SEC-016, REQ-SEC-022,
REQ-SEC-025, REQ-SEC-027 · SCR-SEC-001 through SCR-SEC-010
