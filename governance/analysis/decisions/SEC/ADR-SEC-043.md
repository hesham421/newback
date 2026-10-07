# ADR-SEC-043 — Constant-work service-account authentication via HMAC-SHA256, and rate limiting on public authentication endpoints

Status: ACCEPTED (non-breaking) — amends ADR-SEC-037 §4
Module: SEC · Raised: pass-1 review (finding G1) · Owner: operator

## Context
ADR-SEC-037 §4 fixed API-SEC-031's timing side-channel by making every call — success or
rejection — perform exactly ten verifications against the platform `PasswordEncoder` (the same
slow adaptive hash used for human passwords). REQ-SEC-054 requires only that the secret be
stored one-way; nothing requires the slow-hash construction the encoder provides, which exists
to resist offline guessing of a low-entropy, human-memorized password (NIST SP 800-63B §5.1.1.2).
A service-account secret is a random 256-bit value (CORE "Service-account secrets"), which needs
no such protection — GitHub and Stripe hash their own high-entropy tokens with a fast keyed hash,
not a slow KDF. Ten adaptive-hash verifications per call, from an unauthenticated caller, on an
endpoint neither AC-SEC-001 through AC-SEC-084 nor any RULE throttles, lets an unauthenticated
caller drive costly CPU work at will and amplify it tenfold — a denial-of-service vector the
constant-work fix (correctly closing a timing leak) made worse by fixing the cost at the
expensive end rather than the cheap one. API-SEC-001 has the same unthrottled shape, at one hash
per call.

## Decision
1. A service-account secret's stored form (`ENT-SEC-014.secretHash`, DBF-SEC-107) is computed as
   HMAC-SHA256 keyed with a server-side pepper held outside the database (a platform secret,
   never derived from the row itself), not the `PasswordEncoder`. Verification is a constant-time
   comparison of two HMAC outputs. ADR-SEC-043 amends ADR-SEC-037 §4: API-SEC-031 still performs
   exactly ten comparisons on every path (RULE-SEC-009's constant-work guarantee is unchanged),
   but each comparison is now a microsecond-scale HMAC compare, not an adaptive-hash verification.
   REQ-SEC-054 ("one-way hash") is satisfied unchanged: HMAC-SHA256 is one-way.
2. API-SEC-031 (issue access token) and API-SEC-001 (login) each carry a rate limit, evaluated
   before any hash or HMAC work: a per-source-IP limit and, for API-SEC-031, an additional
   per-client-identifier limit. A request over either limit is answered with the shared
   PLATFORM-STD a 429 code code before any credential lookup, so the limiter itself
   discloses nothing about which identifier exists.

## Consequences
- QR-SEC-043 (store a newly issued credential) and QR-SEC-046 (read unrevoked credentials for
  comparison) are unchanged in shape; only the hash construction changes.
- The pepper is a new platform secret this stage introduces; its storage and rotation follow the
  platform's existing secret-management convention (outside the database, injected at runtime),
  the same way the `PasswordEncoder`'s parameters already are.
- Rate-limit state is held by the platform's shared rate-limiting mechanism (per profile), not by
  a new SEC entity or table.

## Alternatives considered
- Keep `PasswordEncoder` and add only rate limiting: rejected — leaves ten adaptive hashes per
  allowed call, which is still orders of magnitude more CPU than necessary for a high-entropy
  secret, and still gives an attacker inside the rate limit a costly lever.
- HMAC without a server-side pepper (a per-credential stored salt only): rejected — a full
  database leak would let an offline attacker verify guesses against `secretHash` alone; a
  pepper held outside the database is the added protection a fast keyed hash needs to match the
  leak-resistance the slow encoder gave the password case.
