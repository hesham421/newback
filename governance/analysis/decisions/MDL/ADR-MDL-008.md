# ADR-MDL-008 — the published api-docs carries no governance id, so the frontend plan binds by verb and path

Module  : MDL     Version : v1     Stage raised : P3.2 (Frontend — UX Design + Execution Plan)
Status  : ACCEPTED (non-breaking) — supersedes ADR-MDL-001

## Context
`_inputs/api-docs-mdl.md` was re-fetched from the backend repo (`gov.py fetch-inputs`, source
`backend/modules/MDL/api-docs`, four files folded). The fetched document publishes eleven
endpoints for this module — verb, path, operation id, request and response DTOs, the paging
envelope and the error catalog — and **not one governance `API-*` id**.

That is a property of this module's generator, not of the factory:

| Module | Published api-docs | Governance id in it |
|---|---|---|
| SEC | `api-docs-sec.md` | yes — a `Contract ID: API-SEC-0nn` line per endpoint, and an API column in the catalog |
| FIN | `api-docs-fin.md` | yes — the same |
| MDL | `api-docs-mdl.md` | **no** — no `Contract ID` line anywhere in the four folded files |

`registry-exec-be-mdl.md` registers `API-MDL-001 .. API-MDL-011`, and matched on verb and path
those eleven ids and these eleven endpoints are 1:1 — no unbound id, no undocumented endpoint
(the table is in `registry-exec-fe-mdl.md` → API COVERAGE). So the ids are not wrong; they are
simply not published anywhere this stage is allowed to read them from.

Contract C9.5 [CRITICAL] states it exactly: *every `API-*` the frontend execution plan cites
must be defined in the fetched api-docs — never in the backend plan's contract draft*. Citing
`API-MDL-001` in the plan would therefore be a CRITICAL finding, eleven times over, and the
finding would be right: the plan would be asserting an identifier for an endpoint that no
published document gives that identifier to.

ADR-MDL-001 answered this by writing an **API ID BINDING** annex into the api-docs file itself.
That answer no longer stands, twice over: `fetch-inputs` regenerates that file and erased the
annex, and `factory.yaml → inputs.api-docs.merge.keep_alongside` now provides a companion file
(`api-docs-binding-{mod}.md`) for exactly such hand-written material — which `analyze` does not
read, because it resolves the `api-docs` input to the merged file alone
(`analyze.Ctx.input_text`). Neither the old place nor the new one can make a governance id
resolve in the published document.

## Decision
`frontend-execution-plan-mdl.md` cites **no `API-*` id of any module**, and binds every F-block
to its endpoint by the identifiers the published document itself carries: the **verb and the
path**, with the generator's own `operationId` beside them. A short plan-local label
(`TYPE-SEARCH`, `VALUE-REORDER`, …) heads each block as a reading aid; the BINDING table at the
head of the plan maps label → REQ → verb · path → operationId, and the labels are never used
as identifiers of anything outside this plan.

The id ↔ endpoint binding is kept where nothing reads it as a claim about the published
surface: `registry-exec-fe-mdl.md` → API COVERAGE, and the table below.

The `ALIGN` block's API row is written `— examined nothing` rather than ✓, and the COVERAGE row
names C9.5, because that is what the analyze report says about it. A ✓ there would assert that
a check verified eleven bindings when it verified none.

| Backend id | Published verb · path | operationId |
|---|---|---|
| API-MDL-001 | POST `/api/v1/mdl/lookup-types/search` | `search` |
| API-MDL-002 | POST `/api/v1/mdl/lookup-types` | `create` |
| API-MDL-003 | PUT `/api/v1/mdl/lookup-types/{id}` | `update_1` |
| API-MDL-004 | DELETE `/api/v1/mdl/lookup-types/{id}` | `deactivate_1` |
| API-MDL-005 | POST `/api/v1/mdl/lookup-types/values/search` | `search_1` |
| API-MDL-006 | POST `/api/v1/mdl/lookup-types/{id}/values` | `create_1` |
| API-MDL-007 | PUT `/api/v1/mdl/lookup-values/{id}` | `update` |
| API-MDL-008 | DELETE `/api/v1/mdl/lookup-values/{id}` | `deactivate` |
| API-MDL-009 | PATCH `/api/v1/mdl/lookup-types/{id}/values/reorder` | `reorder` |
| API-MDL-010 | POST `/api/v1/mdl/lookup-types/by-owner/search` | `browseByOwner` |
| API-MDL-011 | GET `/api/v1/mdl/lookups` | `readByKey` |

## Consequences
- **Nothing about the endpoints changes.** No verb, path, DTO, permission or error code is
  altered, added or dropped by this decision — it is about how an endpoint is named in one
  artifact. Non-breaking: no `REQ-*` and no locked decision is contradicted, so the run
  continues per the ambiguity rule.
- **C9.5 examines zero subjects** and is listed among the report's vacuous clauses. The plan
  states that in its ALIGN block rather than leaving a reader to reconstruct it.
- **C8.2 / C8.3 stay open against P3.1**, where they belong: the eleven ids `registry-exec-be`
  registers are absent from the api-docs, which is the same gap seen from the other side. This
  stage cannot close it — `registry-exec-be` is P3.1's artifact and the api-docs are the
  backend repo's.
- **The durable fix is in the backend repo, not in an analysis artifact.** MDL's api-doc
  generator should emit the `Contract ID` line it already emits for SEC and FIN; one re-fetch
  after that restores every governance id to the published surface, and C8.2, C8.3 and C9.5 all
  resolve with no change to any plan. Until then, an annex written by this factory into either
  the input file or its companion would be the factory certifying its own ids — which is what
  C9.5 exists to prevent.
- **The implementer loses nothing.** Verb + path is what the api-docs are indexed by (the
  endpoint headings, the catalog table and the anchors are all verb + path), so an F2 block is
  matched to its published shape by a string search that cannot resolve to the wrong endpoint.
