# ADR-MDL-011 — the re-fetched api-docs publishes a Contract ID per endpoint, so the plan cites API ids again

Module  : MDL     Version : v1     Stage raised : P3.2 (Frontend — UX Design + Execution Plan)
Status  : ACCEPTED (non-breaking) — supersedes ADR-MDL-008, which supersedes ADR-MDL-001

## Context
ADR-MDL-008 recorded a property of MDL's api-doc generator: the published document carried no
governance id, so citing `API-MDL-001` in the frontend plan would have been this factory
certifying its own identifier for an endpoint no published document named. Its last consequence
named the durable fix and put it in the backend repo: *"MDL's api-doc generator should emit the
`Contract ID` line it already emits for SEC and FIN; one re-fetch after that restores every
governance id to the published surface."*

That fix has landed. The api-docs input this run reads (`_inputs/api-docs-mdl.md`, source
`backend/modules/MDL/api-docs`, four files, digest `bac95437…`) carries:

| Where | What it now publishes |
|---|---|
| index — API Catalog | an `API` column: `API-MDL-001` … `API-MDL-011`, one per row |
| index — Contract Traceability | "Contract ids joined from `backend-execution-plan-mdl.md` (API REGISTRY): **11 of 11** served endpoints carry one" |
| each endpoint page | a `Contract ID: API-MDL-0nn` line beside the operation id |

Eleven published endpoints, eleven ids, and the index states plainly that a planning document
states the path *proposed* while the catalog states the path *served*.

## Decision
`frontend-execution-plan-mdl.md` cites each endpoint by its **published** `API-MDL-*` id, and
keeps the verb and path beside it as the api-docs themselves index them. The plan-local labels
ADR-MDL-008 introduced (`TYPE-SEARCH`, `VALUE-REORDER`, …) are dropped: they existed only
because no published identifier was available, and a second naming system for the same eleven
endpoints is now a liability rather than a reading aid.

C9.5 (*every `API-*` the plan cites is defined in the fetched api-docs*) therefore examines
eleven bindings and resolves them all in the api-docs, which is what the clause was written to
do. The `ALIGN` block's API row is a ✓ backed by that count, not `— examined nothing`.

No foreign module's `API-*` id is cited in the plan: another module's id is by construction not
defined in MDL's api-docs, so ADR-MDL-004's rule stands unchanged — a cross-module read is
cited as its `UXD-*`, and the foreign endpoint is named in `ui-ux-spec-mdl.md` alone.

## Consequences
- **Nothing about the endpoints changes.** No verb, path, DTO, permission or error code is
  altered, added or dropped — this is about how an endpoint is named in one artifact.
- ADR-MDL-008's binding table is now redundant with the published catalog; the api-docs are the
  authority and the table is not restated anywhere in this run's artifacts.
- C8.2 / C8.3 (`registry-exec-be` ↔ api-docs, both directions) can close at the same eleven ids
  from the other side; this stage does not write either artifact, and does not claim it.
- If a later re-fetch ever drops the `Contract ID` lines again, this ADR is superseded back to
  ADR-MDL-008's answer — the plan's citations, not its design, are what would change.
