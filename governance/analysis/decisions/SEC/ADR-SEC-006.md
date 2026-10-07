# ADR-SEC-006 — no lookup endpoint is published for the three SEC lookup keys

Module  : SEC     Version : v1     Stage raised : P3.2 (Frontend — UX Design + Execution Plan)
Status  : ACCEPTED (non-breaking)

## Context
`profiles/erp.yaml → conventions.lookups` is absolute: "all LOV values runtime-loaded from
the lookup module; no hardcoded enums in APIs or field specs". SEC owns three lookup keys
(SRS A6) — `USER_STATUS` (3 values), `SIGNUP_STATUS` (3), `AUDIT_EVENT_TYPE` (14) — each
with its `ar`/`en` labels fixed in the SRS. The published api-docs contain **no** lookup
endpoint: every response returns the code as a bare string (`statusCode: "ACTIVE"`,
`eventTypeCode: "LOGIN_FAILED"`) and no operation returns the code→label set. The Master
Data Lookup module (MDL) that would serve them is not delivered in this pass.

## Decision
The RF2 `LOOKUP` blocks for the three keys are declared **`PENDING ADR-SEC-006`**: one
shared, long-lived hook per key, with its endpoint unresolved. Until that endpoint exists:

- models keep the lookup field as a **string holding the code** — no TypeScript enum, no
  union of literals, no value list in a model or a validator;
- the display label is resolved through a single label resolver seeded from the SRS A6
  table, isolated in one module so it has exactly one call site to delete;
- `LOOKUP_VALID` validation is **not** performed client-side for these keys (a static list
  would be exactly the hardcoded enum the convention forbids); the server's
  `SEC-409-INVALID-TRANSITION` / `VALIDATION_ERROR` response is the authority.

## Consequences
- No screen presents a status filter or a status field built from an invented value set; the
  three value sets are cited to SRS A6 in one place and are traceable there.
- When MDL publishes its lookup endpoint, the three hooks resolve to it and the seeded
  resolver is deleted — no field spec, model or validator changes, because none of them
  encodes a value.
- `SCR-SEC-004`'s status filter and `SCR-SEC-008`'s event-type filter are built from the
  shared hook, so they change in one place.
- Non-breaking: no `REQ-*` requires a lookup-serving endpoint of SEC, and every status
  transition REQ (REQ-SEC-004, REQ-SEC-005, REQ-SEC-011, REQ-SEC-031) is driven by a
  dedicated endpoint, not by submitting a status value from a list.

## Traces
REQ-SEC-004, REQ-SEC-005, REQ-SEC-011, REQ-SEC-025, REQ-SEC-031 · AC-SEC-025 ·
SCR-SEC-004, SCR-SEC-008 · API-SEC-005, API-SEC-023
