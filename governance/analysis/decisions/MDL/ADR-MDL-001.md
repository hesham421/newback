# ADR-MDL-001 — the api-docs input carries an API ID BINDING annex

Module  : MDL     Version : v1     Stage raised : P3.2 (Frontend — UX Design + Execution Plan)
Status  : SUPERSEDED by ADR-MDL-008 (was ACCEPTED, non-breaking)

> The annex this decision wrote lived INSIDE `_inputs/api-docs-mdl.md`. That file is generated
> by `gov.py fetch-inputs`, which re-fetched it on 2026-09-19 and regenerated it from the
> backend's published folder — the annex is gone, and `factory.yaml → inputs.api-docs.merge`
> now says plainly where a hand-written companion belongs (`api-docs-binding-{mod}.md`,
> `keep_alongside`), which is beside the file and not inside it. A companion beside it is not
> read by `analyze`, which resolves the `api-docs` input to the merged file alone, so it
> cannot make C9.5 resolve either. ADR-MDL-008 records what is done instead.

## Context
`_inputs/api-docs-mdl.md` is generated from the implemented backend's OpenAPI document and
emits no governance `API-*` id — it knows verbs, paths and DTOs, nothing about the factory's
id grammar. Two contracts read that file expecting ids in it: C8.2/C8.3 (the api-docs and
`registry-exec-be` must name the same API ids) and C9.5 (every `API-*` the frontend plan cites
must be defined in the api-docs).

The published surface is exactly eleven endpoints and `registry-exec-be-mdl.md` registers
exactly `API-MDL-001 .. API-MDL-011`. Matched on verb + path the mapping is 1:1 with no
unbound id and no undocumented endpoint. Three rows differ in shape from what
`backend-execution-plan-mdl.md`'s contract table predicted — ADR-MDL-002.

## Decision
An **API ID BINDING** annex is added to `_inputs/api-docs-mdl.md`, following the precedent of
`_inputs/api-docs-sec.md` (ADR-SEC-004) and `_inputs/api-docs-fin.md` (ADR-FIN-002): one row
per `API-MDL-*`, binding it to the endpoint this document publishes, with the three shape
diffs marked ⚠. No endpoint is invented, no id is re-numbered, and the generated body below
the annex is untouched.

## Consequences
- C9.5 resolves: every `API-MDL-*` the frontend plan cites is defined in the api-docs.
- C8.2 and C8.3 resolve in both directions: the eleven registered ids and the eleven annexed
  ids are the same set.
- No `REQ-*` changes and no operation is added or removed by this decision.
