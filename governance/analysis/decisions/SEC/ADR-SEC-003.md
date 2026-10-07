# ADR-SEC-003 — search endpoints are POST `…/search` with a filter envelope, not GET with query params

Module  : SEC     Version : v1     Stage raised : P3.2 (Frontend — UX Design + Execution Plan)
Status  : ACCEPTED (non-breaking)

## Context
The SRS Part B `B5 — API expectations` tables and `backend-execution-plan-sec.md` both
planned the five read-collection operations as `GET` with query parameters:

| API id | Planned | Published (`_inputs/api-docs-sec.md`) |
|---|---|---|
| API-SEC-005 | GET `/api/v1/sec/users` | POST `/api/v1/sec/users/search` |
| API-SEC-012 | GET `/api/v1/sec/roles` | POST `/api/v1/sec/roles/search` |
| API-SEC-021 | GET `/api/v1/sec/registry` | POST `/api/v1/sec/registry/search` |
| API-SEC-023 | GET `/api/v1/sec/audit-log` | POST `/api/v1/sec/audit-log/search` |
| API-SEC-025 | GET `/api/v1/sec/sessions` | POST `/api/v1/sec/sessions/search` |

The implemented shape takes a JSON body (`filters[] {field, operator, value}` with the
operator set `EQUALS, NOT_EQUALS, LIKE, GREATER_THAN, GREATER_THAN_OR_EQUAL, LESS_THAN,
LESS_THAN_OR_EQUAL, IN`, plus `sortField`, `sortDirection`, `page`, `size`) and returns
`Page<T>`. The engine's §3.0 rule: the api-docs are the only endpoint source; a naming or
shape difference continues under an ADR, only a missing core operation is breaking.

## Decision
The frontend binds to the **published** shape. Every RF2 read query in
`frontend-execution-plan-sec.md` posts the search envelope; no `GET` collection call is
planned, and no query-parameter variant is offered as a fallback.

## Consequences
- The operation is a `POST` that mutates nothing; it is still modelled as a read query
  (`tanstack-query` query, not mutation), keyed by `[resource, filters]` with `page`/`size`
  inside the filter object — the cache key rule is unaffected by the verb.
- Filter values leave the URL, so a filtered list is not linkable by URL alone; where a
  screen needs a shareable state (audit-log investigation) the filter is additionally
  mirrored into the route's search params by the facade, and the body is built from it.
- `API-SEC-024` (export audit log) remains a real `GET` with query parameters
  (`eventTypeCode, actorUserId, occurredFrom, occurredTo`) — the export and the search of
  the same screen therefore build their parameters from one shared filter object in two
  shapes. Declared once in the RF2 block of `SCR-SEC-008`.
- No SRS `REQ-*` changes: every operation the REQ set needs still exists.
- The "Planned" column above is history, not the current plan. `backend-execution-plan-sec.md`
  carried `GET /users` beside a `UserSearchRequest` body in two of its tables — a request type
  on a verb that sends none — and both tables have since been corrected to the published
  surface. Contract clause **C8.4 (`endpoint-agrees`)** now holds them there: every
  `(verb, path)` a plan states for an `API-*` must be one the api-docs really publish, so this
  divergence cannot reappear unnoticed in this module or any other.

## Traces
REQ-SEC-009, REQ-SEC-012, REQ-SEC-016, REQ-SEC-025, REQ-SEC-026, REQ-SEC-027 ·
API-SEC-005, API-SEC-012, API-SEC-021, API-SEC-023, API-SEC-024, API-SEC-025 ·
SCR-SEC-004, SCR-SEC-005, SCR-SEC-006, SCR-SEC-008, SCR-SEC-009
