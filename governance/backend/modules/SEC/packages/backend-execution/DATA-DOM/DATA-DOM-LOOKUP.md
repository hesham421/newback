<!-- source: PHASE:DATA-DOM / SUB:DATA-DOM-LOOKUP -->
<!-- context: DATA-DOM-HEADER.md — phase-level preamble -->
<!-- traces: REQ-SEC-004 -->
<!-- SUB:DATA-DOM-LOOKUP:START traces=REQ-SEC-004 -->
### SUB — DATA-DOM-LOOKUP
Not applicable — SEC owns no `kind: lookup` `ENT` of its own this version; `USER_STATUS`,
`SIGNUP_STATUS`, `AUDIT_EVENT_TYPE` are CHECK-constrained value sets on other entities'
`statusCode`/`eventTypeCode` columns (ADR-SEC-001), not separate lookup tables/entities. This
SUB exists to satisfy the engine's grouping convention and is intentionally empty.
<!-- SUB:DATA-DOM-LOOKUP:END -->
