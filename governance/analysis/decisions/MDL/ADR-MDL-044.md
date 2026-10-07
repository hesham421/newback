# ADR-MDL-044 — no free sort field is modelled; only the direction of the server's one ordering

Module  : MDL     Version : v1     Stage raised : P3.2 (gate pass-2 round 2, G3 — restates ADR-MDL-022)
Status  : ACCEPTED (non-breaking)

## Context
ADR-MDL-022 recorded this decision as a transcript rather than a decision, in the same class as
ADR-MDL-021: a paragraph as its title, status `RESOLVED-IN-DIALOGUE`, and a Decision body reading
"(the dialogue recorded the title alone)". The plans depend on it, so it is restated here. The
generator defect is filed as PF-MDL-008.

The substance: F1 and F2 modelled `sortField` as a free variable and keyed the query cache on it.
`QR-MDL-001` declares exactly one ordering (`ORDER BY key`), and PHASE 1 states the module offers
no free-form sort parameter. Nothing the client could put in `sortField` changes what the server
returns.

This is the same principle the pass applied elsewhere — *keying a variation the server cannot
produce would fragment the cache for nothing* — simply not applied to itself.

## Decision
**`sortField` is dropped** from the query models and from the cache key. `sortDirection` survives,
because the direction of the server's single ordering is a variation the server does produce.

## Consequences
- The cache stops fragmenting across values that all resolve to the same response.
- No column header offers a sort the server cannot honour, so no affordance promises what the
  published surface will not deliver.
- If a later version publishes a sort parameter, `sortField` returns as a modelled variable and
  this ADR is where the reason for its absence is recorded.
- `traces`: QR-MDL-001.
