<!-- source: PHASE:DATA-DOM — preamble before the first SUB -->
<!-- traces: REQ-SEC-001, REQ-SEC-003, REQ-SEC-006, REQ-SEC-009, REQ-SEC-010, REQ-SEC-012, REQ-SEC-013, REQ-SEC-014, REQ-SEC-016, REQ-SEC-017, REQ-SEC-019, REQ-SEC-024 -->
## PHASE 2 — DATA-DOM

Entity count is 13 (≥ the engine's self-check threshold for a split) — grouped below under
`SUB:DATA-DOM-MASTER` (reference/master-like registries), `SUB:DATA-DOM-TRANSACTIONAL` (grant
and session/audit rows with a lifecycle or an occurrence timestamp), and
`SUB:DATA-DOM-LOOKUP` (none — SEC owns no `kind: lookup` entity in its own SRS; its lookup
*values* live in USER_STATUS/SIGNUP_STATUS/AUDIT_EVENT_TYPE, which are CHECK constraints on
other entities, not their own ENT — this SUB is intentionally empty and stated so, not omitted).
