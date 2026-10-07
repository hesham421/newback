<!-- source: PHASE:INT-C -->
<!-- traces: REQ-MDL-002 -->
<!-- PHASE:INT-C:START traces=REQ-MDL-002 -->
## PHASE 5 — INT-C (cross-module consume)

One `XM-*` row, below the split threshold (1 < 5) — no SUB opened.

<!-- XM:XM-MDL-001:START traces=REQ-MDL-002 -->
### XM-MDL-001 — validate owner module against SEC
Target        : SEC · ENT-SEC-004 (ModuleRegistry) · classification SOFT-READ
Interface     : REST call — `GET /api/v1/sec/registry?moduleCode={code}` (an instance of `/api/v1/{module}/{resource}` on SEC, per API-SEC-021's search-registry contract)
Contract      : data required = the module code exists and `isActiveFl=true`; fallback if absent = reject with `MDL-409-MODULE-NOT-REGISTERED` (RULE-MDL-001); retry = none (synchronous, user-facing call — a transient SEC outage surfaces as `MDL-503` per the platform-standard infrastructure row, not a silent pass); idempotency = the call is read-only, naturally idempotent
Blocks        : none DEFERRED — SEC v1 is already gated (pass-1 APPROVE); this XM is ACTIVE from the moment MDL v1 is created, never DEFERRED
<!-- XM:XM-MDL-001:END -->
<!-- PHASE:INT-C:END -->
