# ADR-SEC-016 — No interactive use for a service account; its credential secret is shown once and stored irreversibly

Module  : SEC     Version : v2     Stage raised : P0 (Platform Inception) — dialogue rounds 1–2
Status  : RESOLVED-IN-DIALOGUE

## Context
A service account is a machine-only caller (ADR-SEC-013). Two options: (A) a service account
cannot log in interactively, self-register or reset a password, and its secret is shown once
at issuance and stored in a form that cannot be reversed; (B) no restriction.

## Decision
**Option A.** Stripe and GitHub both show a generated key once and never again; the OWASP
Secrets Management Cheat Sheet recommends the same. This extends the v1 invariant that a
password hash never reaches the client (POL-SEC-004) to service secrets — an inherited
constant, not a reopened one. Interactive paths (login, sign-up, password reset) have no
legitimate use for a caller that never has a human at the keyboard, and leaving them open
would be unnecessary attack surface.

## Consequences
- POL-SEC-016 rejects any interactive login/sign-up/reset attempt by a service account.
- POL-SEC-020 requires the secret to be revealed only once, at issuance.
- If a secret is lost, the only recovery path is issuing a new credential (ADR-SEC-015 already
  supports holding more than one active credential at a time for exactly this reason).

## Traces
POL-SEC-016, POL-SEC-020
