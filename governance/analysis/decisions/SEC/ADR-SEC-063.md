# ADR-SEC-063 — An administrator-chosen password forces a change at next login, enforced by a server-side gate
Status      : ACCEPTED (non-breaking)
Stage       : implementation (erp-core 1.3.0)      Module: SEC        Version: erp-core 1.3.0
Lane        : tenant-maturity plan package D
traces      : REQ-SEC-083, REQ-SEC-084, REQ-SEC-085, REQ-SEC-088, RULE-SEC-058, RULE-SEC-059, ENT-SEC-001, DBF-SEC-122

Numbering: the tenant-maturity plan (`docs/plans/tenant-maturity-plan.md` §9) names this decision
ADR-SEC-039. ADR-SEC numbers up to 061 were issued historically (`docs/governance-vendoring-report.md`
Appendix A) and 062 went to package G, so it takes the next free number, 063 (`docs/DEVIATIONS.md` `[TM-D]`).

## Context
erp-core 1.3.0 lets an administrator set a staff user's password (`PUT /api/v1/sec/users/{id}/password`)
and keeps creating accounts with an administrator-chosen password (`POST /api/v1/sec/users`). Either way
the administrator knows the password. Three questions follow:
1. Must the user replace it, and by default?
2. Who enforces that — the client, or the server?
3. If the server, where does the per-request check read the "change required" fact from?

For 3 there are three options:
- **A token claim** (`pcr: true`) set at login and checked by a filter.
- **A database read in the new filter** (`SELECT PASSWORD_CHANGE_REQUIRED_FL ...` per request).
- **The user row `JwtAuthenticationFilter` already loads**: it reads `SEC_USER` by `(username, realm)`
  on every authenticated request to check status and activity; the flag travels from there on the
  authentication's details to the gate.

## Decision
1. **Yes, by default.** `requireChangeAtNextLogin` defaults to TRUE on admin-set and on user create
   (RULE-SEC-058); the administrator can opt out per request. Accounts whose password the owner
   chose (self-change, reset completion) or that no person chose here (tenant first administrator,
   sign-up approval with a random hash, bootstrap admin) are not flagged.
2. **The server.** `PasswordChangeRequiredFilter` in the STAFF chain, after authentication, the tenant
   filter and `RealmEnforcementFilter`, answers every request of a flagged caller with 403
   `SEC-403-PASSWORD-CHANGE-REQUIRED` (envelope written by `FilterErrorResponseWriter`), except
   `GET /api/v1/sec/me`, `PUT /api/v1/sec/me/password`, `POST /api/v1/sec/auth/logout` and the
   public paths (RULE-SEC-059). The client only routes to the change page on
   `passwordChangeRequired = true`.
3. **The row already loaded.** `JwtAuthenticationFilter` attaches the flag to `AuthRealm`
   (`AuthRealm.passwordChangeRequired`); the gate reads the details.

Reasons:
- A client-side redirect is a convenience, not a control: any API client could ignore it and use the
  administrator's password indefinitely.
- No extra query: the row is read anyway (REQ-SEC-028 session / status check), so the gate costs a
  field read.
- Immediate effect, no stale state: the self-change clears the column and the very next request of the
  same token passes; with a claim the token would stay "flagged" until it expired or the user logged in
  again, and an admin-set on a user with live sessions would need those tokens revoked anyway (they are:
  admin-set terminates every session of the user).

## Consequences
- A flagged user can read `/me`, change the password and sign out — nothing else, menu included. The
  frontend (plan §8 F1) routes every page to the change-password page until then.
- Admin-set terminates all the user's sessions; the self-change terminates the user's other sessions and
  keeps the caller's, so the same token continues after the change.
- Every existing client that created users and logged in as them must now change the password first, or
  send `requireChangeAtNextLogin: false` (the HTTP suite and the integration tests were adapted — test
  data only, no assertion dropped; `docs/steps/tm-d-report.md`).
- `AuthRealm` gains a second component; the one-argument constructor stays (flag FALSE), so existing
  callers are unchanged.
- Non-breaking at the schema level: `PASSWORD_CHANGE_REQUIRED_FL` defaults to FALSE, so no account is
  forced into a change by the upgrade.
