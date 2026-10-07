# ADR-SEC-012 — Machine callers authenticate via OAuth 2.0 client-credentials grant, no refresh token, no human session

Module  : SEC     Version : v2     Stage raised : P0 (Platform Inception) — dialogue rounds 1–2
Status  : RESOLVED-IN-DIALOGUE

## Context
Change set CS-SEC-001 adds a daemon that consumes business events from the legacy Oracle
system and calls FIN continuously, unattended. SEC v1 has no concept of a non-human caller:
its access token expires after an hour and every request is validated against a live session
row, so the daemon would otherwise have to run on a human user's credentials.

Options considered: (A) a dedicated service account using the OAuth 2.0 client-credentials
grant (RFC 6749 §4.4) — the daemon exchanges a secret for a short-lived access token and
re-acquires one on its own when it expires, no refresh token, no human session behind it;
(B) a long-lived static API key on every request; (C) a documented refresh path running on a
human user's login — excluded by the change manifest itself; (D) mTLS client certificates or
workload identity federation.

## Decision
**Option A.** RFC 6749 §4.4.3 records that a refresh token SHOULD NOT be issued for this
grant, since the client can simply request again — matching "survives unattended" without a
second long-lived secret in play. Google Cloud IAM service accounts and Microsoft Entra
service principals use the same shape for unattended callers; NIST SP 800-53 Rev.5 AC-2 and
IA-5 cover service accounts as a managed account/credential type. B leaves one non-expiring
secret permanently on the wire; D is more infrastructure than one internal daemon needs.

## Consequences
- P1/P3.1 design the token-issuance endpoint and access-token format under this grant; no API
  shape is fixed here.
- The access token itself must still be checked against live account/credential state on every
  request, not merely trusted for its signature and expiry — see ADR-SEC-014.
- mTLS / workload identity federation stays a deferred item, reopened only if an external or
  cloud-hosted caller needs it.

## Traces
POL-SEC-012, POL-SEC-014, POL-SEC-015, POL-SEC-017
