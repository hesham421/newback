<!-- source: PHASE:INT-XM -->
<!-- traces: API-MDL-002, REQ-MDL-002, XM-MDL-001 -->
<!-- PHASE:INT-XM:START traces=REQ-MDL-002,XM-MDL-001 -->
MDL declares one XM (XM-MDL-001, SOFT-READ → SEC's ModuleRegistry); SEC is in the current
selection, so this is a real linking atom.

<!-- TC:TC-MDL-014:START traces=XM-MDL-001,REQ-MDL-002,API-MDL-002 -->
### TC-MDL-014 — graceful degradation when SEC is unreachable during owner-module validation
Derived from : XM-MDL-001 (REQ-MDL-002)
Exercises    : API-MDL-002 POST /api/v1/mdl/lookup-types
Rule / code  : XM-MDL-001 (SOFT-READ) → (a defined error, never a 500/unhandled state)
Scenario     : INTEGRATION · data class EDGE · language ALL
Preconditions: SEC's registry-search endpoint (API-SEC-021) is made unreachable/times out
Steps        : 1. POST a new lookup-type registration while SEC is unreachable
Expected     : the request fails with a defined, documented error (not a raw 500/timeout
  leak) — MDL's own flow returns a controlled response rather than crashing
Test data    : any lookup-type payload; SEC endpoint simulated as down
<!-- TC:TC-MDL-014:END -->
<!-- PHASE:INT-XM:END -->
