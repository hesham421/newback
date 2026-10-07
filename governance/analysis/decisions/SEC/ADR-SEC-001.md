# ADR-SEC-001 — SEC-owned lookups stay CHECK-constrained in v1, not in a shared lookup table

Module  : SEC     Version : v1     Stage raised : P2 (Database)
Status  : ACCEPTED (non-breaking)

## Context
The platform's governing architecture (`new project/lookup-module-plan-en.md`; domain-profile
§5 G4/G5) mandates one central, governed hub for all coded value lists (a generic
lookup-type/lookup-value master-detail pair), owned by the MDL module. `P2`'s own engine
(§4.2) further instructs that this shared physical infrastructure, if the platform uses
it, is "created once by the first module that needs them." SEC is the first module built
in this batch (GENERATION-INSTRUCTIONS.md §3: SEC → MDL → FIN) and is the first to own
lookup-backed fields (`USER_STATUS`, `SIGNUP_STATUS`, `AUDIT_EVENT_TYPE` — SRS A6), but
MDL itself has not yet been analyzed, so no shared `MDL_LOOKUP_TYPE`/`MDL_LOOKUP_VALUE`
table pair exists yet to seed into — and P2's own boundary forbids this stage from
inventing entities/tables that trace to no SEC `ENT` (NO-COLUMN-INVENTION, P2 §3).

## Decision
SEC's v1 database script stores its three owned lookup types as plain, CHECK-constrained
`VARCHAR` columns (`SEC_USER.status_code`, `SEC_SIGNUP_REQUEST.status_code`,
`SEC_AUDIT_LOG.event_type_code`) rather than as rows in a shared lookup table. The stored
value remains the lookup CODE in every case (never a numeric surrogate), preserving the
platform rule "no hardcoded enums in APIs or field specs" at the application/API surface
even though, physically, v1 enforces the closed value set with a `CHECK` instead of an FK.

## Consequences
- No SEC requirement, entity, or API shape changes as a result of this decision — the
  column type and the value domain are identical to what an FK-backed design would expose.
- When the MDL module's own `P2` stage runs and creates the shared lookup infrastructure,
  a follow-up migration (a new SEC version per `shared/VERSIONING.md`, or MDL's own
  onboarding note) registers these three types under MDL's lookup-type registry and
  replaces the three `CHECK` constraints with FKs to the shared lookup-value table.
- Until that migration, adding a new value to `USER_STATUS`, `SIGNUP_STATUS` or
  `AUDIT_EVENT_TYPE` requires an SEC schema change (`ALTER ... CHECK`), not a data-only
  change — a known, accepted, temporary cost of sequencing SEC before MDL.

## Traces
ENT-SEC-001, ENT-SEC-011, ENT-SEC-013 · REQ-SEC-001, REQ-SEC-002, REQ-SEC-003,
REQ-SEC-004, REQ-SEC-005, REQ-SEC-011, REQ-SEC-024, REQ-SEC-031 · DBF-SEC-007,
DBF-SEC-084, DBF-SEC-102
