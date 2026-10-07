# ADR-SEC-014 — Per-request validation checks live credential/account state, including requests bearing a previously issued access token

Module  : SEC     Version : v2     Stage raised : P0 (Platform Inception) — dialogue rounds 1–2
Status  : RESOLVED-IN-DIALOGUE

## Context
SEC v1 validates every human request against a live session row, which is what makes
revocation immediate. Round 1 proposed three options for a machine caller with no session
row: (A) check each request against the live status of the credential and the account; (B)
give machine callers session rows exempt from cleanup; (C) stateless tokens checked only for
signature and expiry.

Round 1 recommended A and rejected B (leaves the integration dependent on whatever the
cleanup routine does — the exact failure the change set describes) and C (a revoked token
would keep working until it expires). Round 2 found that A, as worded ("reject every
subsequent request made with that credential"), only clearly covers a request presenting the
raw secret. Under the client-credentials flow (ADR-SEC-012), ordinary calls present a
short-lived *access token* derived from the secret, not the secret itself. If the live check
only runs at secret-exchange time, a token issued moments before revocation keeps working
until its own expiry — the same silent-continuation failure the change set exists to close,
just bounded by the token TTL instead of session cleanup.

## Decision
**Option A, amended:** the live check against credential/account status applies to every
request, whether it presents the credential's secret directly (token issuance) or an access
token previously issued from that credential (ordinary calls). No session row is used for
either. This mirrors v1's own behaviour of checking live state per request rather than trusting
a token's self-contained claims.

## Consequences
- POL-SEC-017 is worded to cover "that credential or any access token issued from it."
- P1/P3.1 choose the mechanism (introspection call, a deliberately short access-token TTL with
  frequent re-validation, etc.); this ADR fixes the requirement, not the implementation.
- ActiveSession stays human-login only; nothing here reopens that v1 boundary.

## Traces
POL-SEC-015, POL-SEC-017
