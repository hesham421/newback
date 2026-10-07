# ADR-MDL-004 — the owner-module field reads the security module's registry, and mints UXD-MDL-001

Module  : MDL     Version : v1     Stage raised : P3.2 (Frontend — UX Design + Execution Plan)
Status  : ACCEPTED (non-breaking)

## Context
`ENT-MDL-001.ownerModuleCode` is required on create and "must exist in SEC's ModuleRegistry"
(SRS A3). RULE-MDL-001 refuses a type whose owner module has no ModuleRegistry row, validating
through XM-MDL-001 — a SOFT-READ at the application layer, over the platform's in-process
module interface. MDL stores the code and owns no module list of its own: SRS A8 records
`ModuleRegistry` (ENT-SEC-004) as the one entity MDL consumes.

Two screens display it. `SCR-MDL-001` takes it as an input on create and shows it as a column
and a filter; `SCR-MDL-002` groups the whole registry under it. In both cases the
authoritative source of the set of valid module codes is another module's real API — the
security module's registry search, `API-SEC-021`
(`POST /api/v1/sec/registry/search`, `registry-exec-be-sec.md`).

That is exactly §A.5's definition of a cross-module display dependency.

## Decision
`UXD-MDL-001` is minted for the owner-module field, naming the security module as the owner of
the data and API-SEC-021 as the read behind it. One `UXD-*`, not one per screen: the same
dependency and the same shared hook serve both screens.

The owner-module control on `SCR-MDL-001` is a **select over the registered module codes**,
not a free-text field. A free-text input would let a user type a code the server is certain to
refuse, when the valid set is readable.

The foreign endpoint and its `API-SEC-*` id are named in `ui-ux-spec-mdl.md`, where
`UXD-MDL-001` is defined. `frontend-execution-plan-mdl.md` cites the `UXD-*` and never the
foreign path or the foreign API id: C9.5 requires every `API-*` cited in that plan to be
defined in MDL's own api-docs, and another module's id is by construction not.

## Consequences
- One shared hook, long-lived cache, used by SCR-MDL-001's select and filter and by
  SCR-MDL-002's grouping labels.
- `ownerModuleCode` stays a plain string holding the code in every model — no enum, no union
  of literals, and no module list hardcoded anywhere.
- RULE-MDL-001 is still enforced server-side and its message still routed to the field: the
  select narrows the common case, and a module deregistered between load and submit is caught
  by the server, not by the client.
- If the security module's registry read is unreachable, the select renders empty and the
  create affordance says so rather than falling back to free text — the screen never invents a
  module code.
