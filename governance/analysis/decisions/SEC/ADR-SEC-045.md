# ADR-SEC-045 — No SEC code for the rate limit; throttling stays at ingress
Status      : ACCEPTED (non-breaking) — supersedes ADR-SEC-043 §2
Stage       : P3.1      Module: SEC        Version: v2
Lane        : merge-review-notes (pass-1 revise), reconciling ADR-SEC-044
Decided     : 2026-09-23
traces      : API-SEC-031, API-SEC-001, ADR-SEC-043, ADR-SEC-044, ADR-SEC-037

## Decision
SEC mints no `SEC-429-RATE-LIMITED` code. The pre-authentication throttle on API-SEC-031 and
API-SEC-001 sits at the platform ingress, ahead of the application, exactly as ADR-SEC-044 states.
This supersedes the part of ADR-SEC-043 that named a module code for it; ADR-SEC-043's constant-work
decision (ten fixed hash verifications, no short-circuit) stands unchanged.

ADR-SEC-037 §4 is narrowed at the same time: its one-dummy-verification rule now governs API-SEC-001's
login path only, not the client-credentials exchange, which ADR-SEC-043 replaced with the fixed ten.

## Why
`stack.backend.api.http_statuses` declares no 429 (200, 201, 400, 401, 403, 404, 409, 413, 415, 422,
500), so C7.11 refuses any row carrying it — the platform cannot emit the status, so no code path could
ever raise the code. The profile is platform-owned and not this module's to change; the gap is recorded
as an OPEN CAT-10 platform finding against it.

## Consequence
- The throttle is invisible to SEC's contract and untestable from this plan. That is the cost of the
  platform gap, stated rather than hidden.
- If the profile later declares 429, the limit can move into the application and this ADR is the place
  that says why it was not there to begin with.

Source : ADR-SEC-044; C7.11 refusals on four lines; the CAT-10 platform finding
