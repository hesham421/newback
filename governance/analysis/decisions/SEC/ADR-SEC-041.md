# ADR-SEC-041 — A guarded partial unique index closes the concurrent PENDING sign-up race
Status      : ACCEPTED (non-breaking, ADDITIVE)
Stage       : P2        Module: SEC        Version: v2
Lane        : merge-review-notes (pass-1 revise, finding G1)
Decided     : 2026-09-23
traces      : API-SEC-002, ENT-SEC-013, DBF-SEC-098, DBF-SEC-102, REQ-SEC-003

## Decision
`db-script` §3 BLOCK 5 creates `UQ_SEC_SIGNUP_REQUEST_PENDING_EMAIL`, a partial unique index on
`SEC_SIGNUP_REQUEST(email) WHERE status_code = 'PENDING'`. API-SEC-002's Concurrency line names it as
the guarantee behind its friendly pre-check, matching the pattern API-SEC-006 already follows: the
refused insert answers the shared `DATA_INTEGRITY_VIOLATION` (409).

The migration is **guarded**. A `DO` block counts existing duplicate PENDING rows first and aborts the
migration if it finds any, rather than letting `CREATE UNIQUE INDEX` fail partway through a deploy.

## Why
API-SEC-002 checked that an email was unused and then wrote, with no named guard, and recorded the race
instead of stopping it: two simultaneous submissions both left a PENDING request, and the second
approval later failed on `UQ_SEC_USER_EMAIL` — at approval time, to an administrator, for a defect
created at submission time.

This index was claimed once before. `registry-db-sec.md` listed it against an ADR id that was never
written, so the registry asserted a database guarantee the SQL did not create; the claim was withdrawn
at the pass-1 escalation and the race left recorded as v1 debt. The pass-1 review raised it again (G1)
and the answer taken here is the other one: build it, so the registry's claim and the executable script
agree by construction rather than by editing the claim down to match.

Taking it inside this change set is a widening, and a small and bounded one: one index, one guarded
migration block, no endpoint change, no new code path. The alternative left a known way to create
duplicate pending sign-ups in a module this change set is already migrating.

## Consequence
- One new index; the change set stays ADDITIVE.
- A deploy aborts, loudly and before any change, if duplicate PENDING rows already exist — they must be
  resolved first. That is deliberate: a half-applied migration is worse than a refused one.
- API-SEC-002 no longer records an unguarded race.

Source : pass-1 gate review finding G1; the withdrawn claim in registry-db-sec.md
