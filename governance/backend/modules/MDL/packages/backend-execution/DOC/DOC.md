<!-- source: PHASE:DOC -->
<!-- traces: REQ-MDL-011 -->
<!-- PHASE:DOC:START traces=REQ-MDL-011 -->
## PHASE 4 — DOC

**API contract summary** (backend self-check only; the frontend stage binds to the real
`api-docs-mdl.md` published after implementation):

| API | Path | Verb | Request DTO | Response DTO | Stability |
|---|---|---|---|---|---|
| API-MDL-001 | /lookup-types/search | POST | LookupTypeSearchRequest (proposed) | Page\<LookupTypeResponse\> (proposed) | v1 |
| API-MDL-002 | /lookup-types | POST | LookupTypeCreateRequest (proposed) | LookupTypeResponse (proposed) | v1 |
| API-MDL-003 | /lookup-types/{id} | PUT | LookupTypeUpdateRequest (proposed) | LookupTypeResponse (proposed) | v1 |
| API-MDL-004 | /lookup-types/{id} | DELETE | — | DeactivateConfirmation (proposed) | v1 |
| API-MDL-005 | /lookup-types/values/search | POST | LookupValueSearchRequest (proposed) | Page\<LookupValueResponse\> (proposed) | v1 |
| API-MDL-006 | /lookup-types/{id}/values | POST | LookupValueCreateRequest (proposed) | LookupValueResponse (proposed) | v1 |
| API-MDL-007 | /lookup-values/{id} | PUT | LookupValueUpdateRequest (proposed) | LookupValueResponse (proposed) | v1 |
| API-MDL-008 | /lookup-values/{id} | DELETE | — | DeactivateConfirmation (proposed) | v1 |
| API-MDL-009 | /lookup-types/{id}/values/reorder | PATCH | ReorderRequest (proposed) | List\<LookupValueResponse\> (proposed) | v1 |
| API-MDL-010 | /lookup-types/by-owner/search | POST | LookupTypeByOwnerSearchRequest (proposed) | List\<OwnerGroupResponse\> (proposed) | v1 |
| API-MDL-011 | /lookups | GET | — | List\<LookupValueResponse\> (proposed) | v1 |
(paths relative to `/api/v1/mdl`. A `—` request means the endpoint takes no body: API-MDL-011's
GET row reads its `type` filter from a query parameter; the three `POST .../search` rows carry
their filter criteria in the request body instead, the platform's generic dynamic
JPA-Specification search contract — updated 2026-09-12 when the module's earlier GET
deviation was reversed, mirroring SEC's own GET-to-POST reversal. Every type name above is
marked `(proposed)` — this stage runs before any implementation exists, so the names are
derived, not decided; the real ones arrive with `api-docs-mdl.md` and the stage that can
resolve them fills them in.)

**DTO typing constraints**: `ownerModuleCode` is `String` (the platform module code, not
an enum); no business code field exists.

**Pagination + filter standard**: same as every module (Phase 1 CORE).
<!-- PHASE:DOC:END -->
