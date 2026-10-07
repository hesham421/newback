# New MAJOR G2. Reactivating and deactivating an account have no concurrency guard, and the revise pass introduced the gap

Lane   : review-pass · round 2 · claude:opus
Stage  : P3.1        Module: SEC        Version: v2
Written: 2026-09-23T17:41:06+00:00

API-SEC-010's Concurrency line says "two simultaneous activations both reach ACTIVE — no invariant at risk". That stopped being true when revise G6 (ADR-SEC-047) added the SERVICE_ACCOUNT_REACTIVATED append. Both requests read DISABLED (QR-SEC-040, no lock). Both write ACTIVE with QR-SEC-010, a plain UPDATE. Both then append a reactivation entry, so one transition gets two audit records. API-SEC-009 has the same shape: its only check is "the user exists", so deactivating twice (or deactivating an already-DISABLED account) appends a second SERVICE_ACCOUNT_DEACTIVATED. The brief puts this kind of check-then-write on the reviewer. It is the same pattern the pass already fixed with a conditional update in QR-SEC-026, QR-SEC-045 and QR-SEC-004.
