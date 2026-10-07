# ADR-MDL-014 — no by-id read is published, so an edit route hydrates from the search cache and redirects on cold load

Module  : MDL     Version : v1     Stage raised : P3.2 (Frontend — UX Design + Execution Plan)
Status  : ACCEPTED (non-breaking)

## Context
ADR-MDL-005 made the two edit routes and the two entry sub-views hydrate their form from the row
their own search query already holds. That decision is sound while the row is in cache, and it
says nothing about the case where it is not.

The published surface offers no way to recover it. Neither entity has a by-id read: of the eleven
endpoints `api-docs-mdl.md` publishes, none reads one type or one value by its id, and the search
filter sets that could stand in for one do not carry the record's own id either —
`API-MDL-001` filters on `key`, `ownerModuleCode`, `name` and `isActiveFl`, and `API-MDL-005` on
`lookupTypeId` and `code`. This gap is filed against the platform as **PF-MDL-003**, because it is
the search-filter contract for every module that omits a by-id read, not an MDL defect.

So a cold load — a link opened directly rather than navigated to, after a refresh, from a
bookmark or from another person's address bar — reaches an edit route with nothing to hydrate
from and no published call that could fetch it. Rendering the form anyway produces an empty
editor over a record that exists, which is the worst of the three available answers: it invites a
save that would either fail or overwrite the record with blanks.

Inventing a filter is not available to this stage. C9.5 requires every `API-*` the plan cites to
be defined in the fetched api-docs, and a filter operator that no published document declares is a
claim about a surface nobody serves.

## Decision
On a cold load, the route **redirects rather than renders**:

- `/reference-data/lookups/:typeId/edit`, `/reference-data/lookups/:typeId/values/new` and
  `/values/:valueId/edit` redirect to `/reference-data/lookups/:typeId`
- if the type itself is absent from cache as well, to `/reference-data/lookups`

The destination shows the localized message — ar: «افتح السجل من القائمة» ·
en: "Open the record from the list" — instead of a blank editor.

The in-session case the routes were written for is unchanged: navigated to from the list, the row
is in cache and the form hydrates from it exactly as ADR-MDL-005 states.

## Consequences
- Every route stays addressable. A deep link is never rejected; it resolves one level up, where
  the record can be opened for real.
- No unpublished endpoint is called and no filter is invented. The plan cites only ids that
  `api-docs-mdl.md` defines, so C9.5 holds.
- The ALIGN-FE table carries the two operations the SRS names and nothing publishes —
  *read one type by id* and *read one value by id* — as ✗ rows citing this ADR and ADR-MDL-005,
  rather than as empty routes. No row is a ✗ for want of a decision.
- The gap remains open as PF-MDL-003. If a by-id read or an id filter is later published, the
  redirect becomes a fallback rather than the only path, and this ADR is the place that records
  why the redirect was there.
- The behaviour is checkable: a reviewer can confirm each of the three routes has a redirect
  target and that the target is reachable without the missing read.
