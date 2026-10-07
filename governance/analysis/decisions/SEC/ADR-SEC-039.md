# ADR-SEC-039 — SEC's in-process read surfaces are not endpoints; the three findings that follow are recorded, not hidden
Status      : ACCEPTED (non-breaking)
Stage       : P3.1      Module: SEC        Version: v2
Lane        : analysis · round 1 · claude:opus
traces      : REQ-SEC-034, REQ-SEC-035, ENT-SEC-001, ENT-SEC-004

## Context
SEC publishes two in-process read surfaces in `com.erp.sec.crossmodule`:
- **`SecUserDirectoryApi`**, added by the 2026-09-11 amendment. It has two operations: `findContact` (REQ-SEC-034) and
  `findUserIdsHoldingPermission` (REQ-SEC-035), whose query is QR-SEC-039.
- **`SecModuleRegistryApi`**, which MDL's XM-MDL-001 injects to ask "is this module code registered?". MDL's plan
  notes that SEC's artifacts never wrote it down, and that doing so is SEC's to correct on its own re-run.

The SRS states, for both REQs, that the surface is "an injected Spring interface — not an HTTP endpoint". The
factory's contracts have no atom kind for an operation a module exposes in process:
- `orphans` (C7.6) wants every REQ covered by an `API-*` or a `DBF-*`;
- `orphans` (C7.22) wants every `QR-*` cited by an `API-*`.

So REQ-SEC-034, REQ-SEC-035 and QR-SEC-039 are findings. SEC v1's own report already lists all three.

Three ways out were weighed:
- **Mint `API-*` ids for the interface operations.** An `API-*` is an endpoint on the base path. It feeds the api-docs,
  the frontend binding and api-verify, and none of them can reach an in-process method. It would also contradict the
  SRS sentence above.
- **Cite the REQs or the query from an unrelated endpoint block.** That satisfies the check and states nothing true.
  It is the manufactured confidence the self-check rules forbid.
- **Record the gap.**

## Decision
Record it. The plan documents both surfaces in INT-R: operation, entities read, query, consumer and v2 effect.
- `SecModuleRegistryApi` is written down for the first time. It reuses the module-code existence read (QR-SEC-035),
  which API-SEC-018 also cites, so it adds no finding.
- The three findings stay open and are named in the plan's ALIGN block and in the registry.

The fix belongs to the factory: a contract clause, or an atom kind, that accepts an exposed in-process operation as
coverage.

## Consequences
- The P3.1 stage report for SEC v2 carries three MAJOR findings that no plan edit can honestly close. The pass-1 gate
  needs a human decision: a waiver with this ADR as the reason, or a factory change first.
- REQ-SEC-035's surface has had no consumer since FIN retired its separation-of-duties reader on 2026-09-12. Whether
  SEC keeps it is a separate P1 question. This plan does not remove an implemented, SRS-required operation.
- Non-breaking.
