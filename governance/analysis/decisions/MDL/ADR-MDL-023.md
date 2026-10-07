# ADR-MDL-023 — the frontend test plan states its AC coverage against the screen-bearing AC set, not all thirteen

Module  : MDL     Version : v1     Stage raised : test-gen (Test Generation, standalone)
Status  : ACCEPTED (non-breaking)

## Context
The engine derives one `TC-*` per `AC-*` and asks each test plan to close with
`AC covered <n>/<total>`, where a gap is ✗ and blocks the run. It does not say what `<total>` is
on a track that cannot reach every AC.

MDL has thirteen ACs, and eleven of them describe something a user does on a screen. The other
two do not:

- **AC-MDL-011** — a consuming module requests a type's values by key and receives the active
  ones, ordered.
- **AC-MDL-012** — the same read with a key no type carries, answered as a not-found.

Both belong to `API-MDL-011`, which the frontend execution plan **binds and no screen calls**:
its F2 block records `Caller : a consuming module's backend`, `Cache key : n/a`, and ADR-MDL-007
settled that it is drawn on no screen and given no route. registry-exec-fe-mdl.md reaches the
same place from the other side — "REQ traced by an `SCR-*` or `UXD-*` record: **11/13** … they
are traced by no screen, because no screen implements them", and "inflating it would mean
asserting a screen that does not exist".

So the frontend track has two ACs it can neither exercise nor honestly claim. Writing `11/13`
with two ✗ would report a missing test for a screen the platform deliberately does not have, and
a ✗ blocks the run; writing `13/13` would claim coverage that does not exist.

## Decision
`frontend-test-plan-mdl.md` states **`AC covered 11/11` against the screen-bearing AC set**, and
lists AC-MDL-011 and AC-MDL-012 by id as out of track — not as gaps — naming the backend TCs that
do cover them (TC-MDL-011, TC-MDL-012) and the decision that put them out of reach (ADR-MDL-007).
The same block states the module-level figure, `AC (module) 13/13`, so no reader has to compose
it.

A ✗ in this plan keeps its full meaning: an AC a screen implements and no TC exercises. Neither
of these two is that.

## Consequences
- The engine's self-check "every AC-* in the SRS has ≥1 TC-*" is satisfied at the module level,
  which is the level it is written at — TC-MDL-011 and TC-MDL-012 exist, on the backend track.
- The two numbers in the frontend COVERAGE block (11/11 and 13/13) cannot be read as a
  contradiction, because each names its own denominator.
- If a later version gives the consumer read a screen, the two ACs enter the screen-bearing set
  and the denominator becomes 13 with two new frontend TCs — no id is renumbered by that, since
  the sequence simply continues.
- Nothing about the backend plan changes: it covers all thirteen and says so.
