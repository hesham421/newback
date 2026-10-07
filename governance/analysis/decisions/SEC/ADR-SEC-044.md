# ADR-SEC-044 — The pre-authentication endpoints are throttled and bound their inputs
Status      : ACCEPTED (non-breaking)
Stage       : P3.1      Module: SEC        Version: v2
Lane        : operator (pass-1 gate findings, applied after a revise truncated the plan)
Decided     : 2026-09-23
traces      : API-SEC-031, API-SEC-001, QR-SEC-048, REQ-SEC-066, AC-SEC-071, DBF-SEC-087, ADR-SEC-043, RULE-SEC-013

## Decision
Two changes to the endpoints a caller can reach without authenticating:

1. **Throttle.** API-SEC-031 and API-SEC-001 are rate limited per source and per submitted identifier,
   **before any hash verification runs** — by the platform's ingress, ahead of the application, so the
   request never reaches the endpoint and SEC emits no code for it.
2. **Bound the inputs.** `clientId` and `username` are capped at 100 characters, `clientSecret` and
   `password` at 200, checked before any lookup.

## Why

**The throttle exists because of ADR-SEC-043.** Making every authentication path cost exactly ten
adaptive-hash verifications closed a timing channel and, on an endpoint anyone can call unauthenticated,
turned that fixed cost into an amplifier: a stream of unknown client ids would buy ten slow hashes
each. The constant-work guarantee is right and stays; it needs a limit in front of it. The order
matters — the limit is applied before the verifications, or it buys nothing.

**The bound exists because an unbounded identifier could erase its own audit trail.** For an unknown
identifier, QR-SEC-048 writes the submitted `clientId` into `SEC_AUDIT_LOG.target_ref`, which is
VARCHAR(200) (AC-SEC-071). An identifier longer than that makes the REQUIRES_NEW insert fail, so the
caller receives INTERNAL_ERROR 500 instead of SEC-401-INVALID-CLIENT **and the SERVICE_AUTH_FAILED
entry REQ-SEC-066 requires is never written**. A caller could suppress the record of their own failed
authentication attempts by padding the identifier. The same shape applies to every public endpoint.

## Consequence
- **No new SEC code.** `stack.backend.api.http_statuses` declares no 429, so a module cannot answer a
  throttle with a code the platform can emit — C7.11 refused `SEC-429-RATE-LIMITED` on four lines. The
  limit therefore sits ahead of the application. That is a worse place for it: invisible to SEC's
  contract and untestable from this plan. It is recorded as an OPEN platform finding against the
  profile (CAT-10), which is where it belongs; this module records such a finding and never fixes one.
- The limit's numbers are deployment configuration, not a plan constant; what the plan fixes is that a
  limit exists and runs before the verifications.
- A failed authentication is now always recorded, whatever the caller submits.

Source : pass-1 gate review — the amplification introduced by ADR-SEC-043, and the unbounded pre-auth inputs
