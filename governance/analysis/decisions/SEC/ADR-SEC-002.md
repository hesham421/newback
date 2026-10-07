# ADR-SEC-002 — PLATFORM-STD umbrella for infrastructure error-catalog rows

Module  : SEC     Version : v1     Stage raised : P3.1 (Backend Execution Plan)
Status  : ACCEPTED (non-breaking)

## Context
The engine's Error Catalog rule (§7) requires "every RULE that produces a user-facing
message" to have a catalog row, and separately allows infrastructure errors (not found,
forbidden, server) to carry `RULE = PLATFORM-STD` with an ADR. SEC v1's 27 endpoints
collectively need ~18 such infrastructure rows (uniqueness conflicts, not-found, invalid
lifecycle transition, the authorization-gate denial, invalid sort field, generic server
error) that are implied by the SRS's own conventions — unique constraints (§P2 db-script
UQ_* names), status lifecycles (SRS A7), and the module/screen/action gate
(POL-SEC-001/007, REQ-SEC-033) — rather than by a distinct numbered `RULE-*` record each.

## Decision
These rows are written once as `RULE = PLATFORM-STD` in the Error Catalog
(backend-execution-plan-sec.md → Error Catalog) and referenced from this single ADR,
instead of minting a dedicated SRS-level RULE for each (which would mean re-opening P1
for content P1 never claimed to own — uniqueness/not-found/lifecycle/authorization
behaviour is a platform-wide convention, not a SEC-specific business rule).

## Consequences
- No change to the SRS, the db-script, or any `RULE-*` id — this ADR governs only how the
  Error Catalog cites its non-`RULE-*` rows.
- A future module's own P3.1 may cite the same `PLATFORM-STD` convention for its own
  infrastructure rows without re-deriving this decision.
- If the platform later formalizes a distinct `RULE-*`-owning stage for cross-cutting
  infrastructure behaviour, this ADR is superseded, not the Error Catalog rows themselves
  (the codes and messages stay stable).

## Traces
REQ-SEC-033 (module gate enforced on every request) · API-SEC-001 through API-SEC-027
(every endpoint contributes at least one PLATFORM-STD row) · RULE-SEC-007 (VIEW gateway,
underlies SEC-403-FORBIDDEN together with REQ-SEC-033)
