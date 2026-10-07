# ADR-MDL-042 — no value is created under an inactive type; updating and reordering the ones already there stay legal

Module  : MDL     Version : v1     Stage raised : P3.2 (Frontend — UX Design + Execution Plan, gate pass-2 round 2 G1)
Status  : ACCEPTED (non-breaking)

## Context
Nothing in this version states what happens to a **write** beneath a deactivated lookup type.
The gap is symmetrical across both sides and neither side closes it:

- `API-MDL-006`'s orchestration confirms only that the parent type **exists**; `QR-MDL-006`
  carries no state predicate. `API-MDL-007` and `API-MDL-009` likewise write rows whose parent
  may be inactive.
- Only the consumer read consults the parent's flag (`QR-MDL-015`, RULE-MDL-004).
- On the client, the master list shows inactive types **by requirement** (`AC-MDL-005`), the
  values pane opens over one, and no affordance was conditioned on the parent's state.

The consequence is not untidiness, it is permanent. v1 publishes no activate endpoint at either
level (ADR-MDL-005), and RULE-MDL-004 excludes an inactive type's values from every consumer
read. So a value created under a deactivated type **can never be reached by anything**, and —
because `UQ_MDL_LOOKUP_VALUE_TYPE_CODE` is unique over every row, active or not (db-script
BLOCK 5b) — it **reserves its code under that type forever**. There is no surface in this version
that can undo either half.

`AC-MDL-004` does not cover this. It speaks only of the values that already existed at the moment
the type was deactivated, and guarantees they survive.

## Decision
**CREATE is forbidden under an inactive parent. UPDATE and REORDER remain legal.**

The asymmetry is the whole point: a create adds a row nothing can ever reach, while update and
reorder only maintain rows `AC-MDL-004` already guarantees survive deactivation. Forbidding all
three would contradict that guarantee.

In this stage's writable set:

- `frontend-execution-plan-mdl.md` F2-QUERY VALUE CREATE — the call is not issued when the
  selected parent is inactive. `isActiveFl` is on the row the pane already holds, so the state is
  known without an extra read.
- `frontend-execution-plan-mdl.md` F4 SCR-MDL-001 — the add-value affordance is disabled, and
  `/reference-data/lookups/:typeId/values/new` redirects to the type's value pane rather than
  opening an editor that cannot save. Both state the reason in RULE-MDL-004's own wording —
  ar: «هذا النوع معطّل حاليًا» · en: "This lookup type is currently inactive" — mirroring the
  reorder-handle treatment.
- `ui-ux-spec-mdl.md` under SCR-MDL-001, beside the reserved-code statement the same
  irreversibility produced.

## Consequences
- **The client guard is half the fix and is recorded as such.** A direct call still bypasses it.
  The server half is filed as **PF-MDL-007**, owned by the MDL backend track (P3.1):
  `API-MDL-006`'s orchestration must confirm the parent is active before `QR-MDL-014` runs, and
  must refuse with a catalog row of its own. Broadening `MDL-404-TYPE` is wrong — the type exists.
- **The SRS owes RULE-MDL-004 its write-side half.** As written the rule scopes only to reads.
  That is the same P1 gap the standing `C5.16` minor names against `US-MDL-005`.
- No new error code is minted here. The screen refuses before calling, so no runtime row is
  needed on the client side; the server's row is PF-MDL-007's to name.
- The behaviour is checkable without the backend: select an inactive type, confirm the add-value
  affordance is disabled and carries the message, and confirm editing and reordering its existing
  values still work.
