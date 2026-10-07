<!-- Source: PHASE:INT-C -->

## PHASE INT-C — Integration Contract Specifications
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓
## INT-C SUMMARY — FILE — PLAN-ID: PLAN-FILE-001
XM-ID       │ Classification │ Target Module │ Interface     │ Contract Status
────────────┼────────────────┼───────────────┼───────────────┼────────────────
XM-FILE-001 │ SOFT-READ      │ Security (SEC)│ App-layer read│ CONTRACTED ✓

<!-- XM:XM-FILE-001:START -->
### XM-FILE-001 — SOFT-READ of SEC UserAccount identity
Target Module    : Security (SEC) | Target Entity: UserAccount (ENTITY-SEC-001) → SEC_USER_ACCOUNT
Classification   : SOFT-READ (application layer — NO physical FK, RULE-FILE-004)
Interface        : Security auth filter provides the authenticated principal; created_by populated from it (AuditEntityListener).
Data required    : authenticated user identity (for created_by + owner-visibility checks).
Fallback if absent: request rejected by Security filter before reaching FILE (401) — FILE does not self-verify JWT.
Blocks           : none (SEC gated ACTIVE, built before FILE) | Unblock condition: n/a (READY)
Idempotency      : read-only | Retry/Timeout: n/a (in-process filter)
<!-- XM:XM-FILE-001:END -->

INT-C GATE CHECK: [✓] all XM from DB Register accounted (XM-FILE-001) [✓] classification declared [✓] no new XM invented [✓] Open RXEs none
INT-C Gate: PASSED ✓
─────────────────────────────────────────────────────────────────
