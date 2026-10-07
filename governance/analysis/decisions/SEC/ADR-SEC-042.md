# ADR-SEC-042 — A service account holds at most ten unrevoked credentials
Status      : ACCEPTED (non-breaking, ADDITIVE)
Stage       : P3.1      Module: SEC        Version: v2
Lane        : merge-review-notes (pass-1 revise, findings G7 then G3)
Decided     : 2026-09-23
traces      : REQ-SEC-078, AC-SEC-084, RULE-SEC-013, API-SEC-029, API-SEC-031, ENT-SEC-014, US-SEC-016

## Decision
An account may hold at most **ten** unrevoked credentials. API-SEC-029 counts them under the same row
lock it already takes on the account (QR-SEC-041, so two concurrent issuances cannot both pass the
check), and answers `SEC-409-CREDENTIAL-LIMIT` without creating a credential when the count is already
ten. REQ-SEC-078 states the requirement, AC-SEC-084 tests it, RULE-SEC-013 carries it as a business rule.

## Why
REQ-SEC-051 says issuance "shall generate" a credential, with no cap, and POL-SEC-021 accepts more than
one active credential so that rotation needs no downtime. Nothing bounded the set.

API-SEC-031 verifies a presented secret against **each** unrevoked credential's hash with the platform
password encoder. Unbounded, the cost of one authentication grows without bound with the number of
credentials an account has accumulated — and a compromised account becomes proportionally harder to
clean up, since every credential must be found and revoked. Rotation, the reason more than one is
allowed at all, needs exactly two at a time.

The first revise added the refusal to the plan and the error catalog alone. The gate's next round (G3)
was right that this was a downstream stage silently narrowing an upstream requirement: a rejection path
with no REQ, no AC to test it and no rule behind it. The cap is the decision; this ADR and the three
upstream ids are where it is now stated.

## Consequence
- One new error code, `SEC-409-CREDENTIAL-LIMIT` (409), with its own AC.
- No DB constraint: the count is under the account's row lock, and a partial index cannot express
  "at most N rows per parent". The lock is the guarantee, and it is the same one issuance already takes.
- Ten is a policy number, not a physical one. It follows Google Cloud's per-account service-account-key
  limit and leaves eight spare over what rotation needs.

Source : pass-1 gate review, finding G7 (unbounded set) and finding G3 (narrowed upstream requirement)
