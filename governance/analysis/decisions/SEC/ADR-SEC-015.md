# ADR-SEC-015 — No forced credential expiry; rotation is administrator-driven via a second live credential

Module  : SEC     Version : v2     Stage raised : P0 (Platform Inception) — dialogue rounds 1–2
Status  : RESOLVED-IN-DIALOGUE

## Context
CS-SEC-001 requires the service account to "survive unattended for as long as the service
runs, without a person re-authenticating." Options considered: (A) no forced expiry, with an
administrator rotating by issuing a second credential, switching the daemon over, then
revoking the old one; (B) mandatory expiry (e.g. 90 days); (C) a single credential where
rotation means revoke-then-reissue, with downtime in between.

## Decision
**Option A.** Forced expiry (B) is the standard way an unattended integration goes dark when
nobody rotates in time — directly contradicting "survives … as long as the service runs."
Option C has a downtime window the change set does not ask for. Google Cloud service-account
keys can be created without an expiry (an org policy may add one on top); Microsoft Entra
client secrets do expire, which is the documented counterpoint — recorded here as a deferred
item with its own trigger rather than silently foreclosed.

## Consequences
- POL-SEC-021 requires the system to accept more than one active credential per service
  account simultaneously, so rotation has no downtime window.
- "Forced service-credential expiry" is listed under business-policies-sec.md → SCOPE
  EXCEPTIONS, activated only by an explicit future security-policy request.
- Revocation of an individual credential remains immediate per ADR-SEC-014 — having no forced
  expiry does not weaken revocability.

## Traces
POL-SEC-021
