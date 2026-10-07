# ADR-SEC-047 — Reactivating a service account is audited
Status      : ACCEPTED (non-breaking, ADDITIVE)
Stage       : P1        Module: SEC        Version: v2
Lane        : merge-review-notes (pass-1 revise, finding G6)
Decided     : 2026-09-23
traces      : REQ-SEC-079, ENT-SEC-011, POL-SEC-019, AUDIT_EVENT_TYPE

## Decision
`CHK_SEC_AUDIT_LOG_EVENT_TYPE` carries `SERVICE_ACCOUNT_REACTIVATED` alongside the six v2 `SERVICE_*`
codes, taking the lookup from 20 to 21 codes. REQ-SEC-079 states the requirement.

## Why
v2 audits creation, deactivation, credential issue and revoke, and both authentication outcomes. It did
not audit **reactivation** — the one transition that gives a disabled machine principal its access back.
POL-SEC-019 requires every change to a service account's standing to be attributable; an account could
be re-enabled with nothing in the audit log saying who did it or when.

A gap of this shape matters more for a machine principal than a human one: nobody notices a daemon
quietly regaining access the way a person notices their own account changing.

## Consequence
- One new AUDIT_EVENT_TYPE value; the change set stays ADDITIVE.
- The deactivate/reactivate pair is now symmetric in the audit log.

Source : pass-1 gate review finding G6
