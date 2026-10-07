<!-- source: PHASE:INT-R -->
<!-- traces: REQ-MDL-002 -->
<!-- PHASE:INT-R:START traces=REQ-MDL-002 -->
## PHASE 6 — INT-R (cross-module resolve)

| XM | Status | Workaround (if not READY/ACTIVE) |
|---|---|---|
| XM-MDL-001 | ACTIVE | not applicable — target already gated |

No DEFERRED row exists this module; no mock/simulated strategy is needed. Inbound
dependency stub: any future consumer of MDL itself (every module from PRC onward, and FIN
next) reaches `ENT-MDL-001`/`ENT-MDL-002` through API-MDL-011 exactly as SEC's own consumers
reach SEC — `XM-INBOUND-STUB-2` (first expected consumer: FIN, per GENERATION-
INSTRUCTIONS.md §3), formal id assigned by FIN's own P2.
<!-- PHASE:INT-R:END -->
