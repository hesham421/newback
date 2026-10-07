# ADR-MDL-043 — the uniqueness pre-check requests a LIKE filter and asserts equality on the client

Module  : MDL     Version : v1     Stage raised : P3.2 (gate pass-2 round 2, G3 — restates ADR-MDL-021)
Status  : ACCEPTED (non-breaking)

## Context
ADR-MDL-021 recorded this decision as a transcript rather than a decision: its title is a
paragraph of review prose, its status is `RESOLVED-IN-DIALOGUE` — none of ACCEPTED, BLOCKED or
SUPERSEDED — and its Decision body reads "(the dialogue recorded the title alone)". The plans
depend on the choice, so it is restated here in a form a reader of the stream can use. The
generator defect itself is filed as PF-MDL-008.

The substance: F3's `UNIQUE_CHECK` sent an **EQUALS** filter on `key` and `code`, but
`QR-MDL-001` binds `WHERE [key LIKE :key]` and `QR-MDL-005` binds `[code LIKE :code]`
unconditionally, and PHASE 1 and SRS §B2 agree both are LIKE. No published filter offers
equality. So typing `PAYMENT` returns `PAYMENT_METHOD`, and the blur check falsely reports the
key as already taken — on the one field that cannot be changed after creation.

## Decision
The pre-check **requests the LIKE filter the server actually binds, and asserts equality
client-side** over the rows that come back: a match counts only when the returned `key` (or
`code`, within the parent type) is equal to the typed value, case-sensitively as the unique
index compares it.

The check stays advisory. `MDL-409-TYPE-DUP` and `MDL-409-VALUE-DUP` from the server remain the
authority, because rows can be created between the check and the submit.

## Consequences
- No unpublished filter is invented, so C9.5 holds: the plan cites `API-MDL-001` and
  `API-MDL-005` as published.
- The false positive is gone on the field that matters most — a key is immutable after creation,
  so a wrong "already taken" costs the user a name they could have had.
- A prefix of an existing key now returns rows the client discards. That is a larger response for
  a cheaper correctness guarantee, and the filter is already paginated.
- `traces`: QR-MDL-001, QR-MDL-005.
