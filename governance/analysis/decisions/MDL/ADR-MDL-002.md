# ADR-MDL-002 — three reads are POST `…/search`, and the backend plan's contract table is what lags

Module  : MDL     Version : v1     Stage raised : P3.2 (Frontend — UX Design + Execution Plan)
Status  : ACCEPTED (non-breaking)

## Context
Three read operations differ between `backend-execution-plan-mdl.md`'s API table and the
published surface:

| API id | Backend plan's table | Published (`_inputs/api-docs-mdl.md`) | SRS `B5` |
|---|---|---|---|
| API-MDL-001 | GET `/api/v1/mdl/lookup-types` | POST `/api/v1/mdl/lookup-types/search` | POST `…/search` |
| API-MDL-005 | GET `/api/v1/mdl/lookup-types/{id}/values` | POST `/api/v1/mdl/lookup-types/values/search` | POST `…/search` |
| API-MDL-010 | GET `/api/v1/mdl/lookup-types/by-owner` | POST `/api/v1/mdl/lookup-types/by-owner/search` | POST `…/search` |

The point worth recording is which two of the three agree: **the api-docs and the SRS Part B
`B5` tables already state the same POST shape.** Only the backend execution plan's contract
summary still carries the GET form it predicted before implementation. So this is not a
divergence between the implementation and the requirement — it is one stale table.

`gov.py analyze` reports the same three as C8.4 `endpoint-agrees` findings against
`backend-execution-plan`, which is the artifact that actually disagrees.

## Decision
The frontend binds to the **published** shape, which is also the SRS's: each of the three is a
`POST` taking a `filters[] {field, operator, value}` envelope (operators EQUALS, NOT_EQUALS,
LIKE, GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN, LESS_THAN_OR_EQUAL, IN). No `GET`
collection call is planned and no query-parameter variant is offered as a fallback.

§3.0 of the P3.2 engine already forbids reading the backend plan's contract summary as an API
source, so no exception is being made here — this ADR records why the three rows look like a
diff and where the fix belongs.

## Consequences
- API-MDL-001 and API-MDL-005 are `POST` requests that mutate nothing; both are still modelled
  as read queries keyed by `[resource, filters]` with `page`/`size` inside the filter object —
  the verb does not change the cache-key rule.
- API-MDL-010 takes `filters` **without** paging and returns a bare array of owner groups, so
  its cache key carries the filters alone and no page or size.
- Filter values leave the URL, so a filtered list is not linkable by URL alone; where a screen
  needs a shareable state the facade mirrors the filter into the route's search params and
  builds the body from it.
- The fix for the three C8.4 findings is three rows in `backend-execution-plan-mdl.md`, a P3.1
  artifact that §8 puts outside this stage's boundary. Recorded, not silently corrected.
- No SRS `REQ-*` changes: every operation the REQ set needs exists.
