# ADR-SEC-004 — the published api-docs carry no `API-*` id; P3.2 binds them by verb + path

Module  : SEC     Version : v1     Stage raised : P3.2 (Frontend — UX Design + Execution Plan)
Status  : ACCEPTED (non-breaking)

## Context
`_inputs/api-docs-sec.md` is emitted by the backend repo's `api-doc-generator` from the
OpenAPI document. That generator knows nothing of the factory's id grammar, so not one of
the 27 `API-SEC-*` ids registered in `registry-exec-be-sec.md` appears in it. The frontend
plan must cite `API-*` ids (engine §3.1: `API-*` is backend-owned and only cited) and those
citations have to resolve against the api-docs, which is the only endpoint source.

## Decision
An **API ID BINDING** annex is written at the head of `_inputs/api-docs-sec.md`, mapping
each `API-SEC-*` to the endpoint the same document publishes, matched on verb + path. The
annex adds ids, never endpoints: every row's verb, path, request and response shape is read
from the generated body below it. The binding is 1:1 in both directions — 27 registered ids,
27 published endpoints, no unbound id and no undocumented endpoint — with five rows carrying
the shape diff of ADR-SEC-003.

## Consequences
- `frontend-execution-plan-sec.md` cites `API-SEC-*` and the citation resolves in the
  api-docs input, as the C9.5 contract requires.
- The annex is regenerated, not merged, whenever the backend republishes: it is a derived
  index over the body, so a republished api-docs is re-annexed by re-running the binding
  against the registry — it is never hand-edited to keep a stale id alive.
- The durable fix belongs in the backend repo's generator (emit the governance id it already
  knows from its own plan). Until then this annex is the seam, and it is explicit rather
  than implicit.
- No id is minted here: P3.2 owns `UXD` and `SCR` only, and the annex defines neither.

## Traces
API-SEC-001 through API-SEC-027 (every row of the binding) · REQ-SEC-033
