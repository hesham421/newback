# ADR-SEC-064 — The staff `/me` payload carries no roles and no permissions
Status      : ACCEPTED (non-breaking)
Stage       : implementation (erp-core 1.3.0)      Module: SEC        Version: erp-core 1.3.0
Lane        : tenant-maturity plan package D
traces      : REQ-SEC-086, AC-SEC-092, ADR-SEC-005, REQ-SEC-021, REQ-SEC-030

Numbering: the tenant-maturity plan (`docs/plans/tenant-maturity-plan.md` §9) names this decision
ADR-SEC-040; it takes the next free number after ADR-SEC-063 (`docs/DEVIATIONS.md` `[TM-D]`).

## Context
erp-core 1.3.0 adds `GET /api/v1/sec/me`, the staff user's own profile, which the frontend loads after
login (avatar menu, profile page, preferred language). A profile endpoint is the obvious place to also
return the caller's roles and effective permission codes, and a client would then gate buttons on them.
ADR-SEC-005 already decided that no endpoint publishes the caller's action-level permissions: the UI
gates navigation on the effective menu (`GET /api/v1/sec/menu`, the module → screen tree) and trusts the
server's 403 for everything else.

## Decision
`StaffProfileResponse` carries the account and profile fields, `photoUrl`, `passwordChangeRequired`,
`lastLoginAt` and the tenant's code and names — **no `roles`, no `permissions`, no authority codes**.
The menu stays the only client-side authority.

Reasons:
- ADR-SEC-005 stands: a second source of authority on the client would drift from the server's real
  decision (super roles hold every authority regardless of grants — srs-sec.md 1.3.0 §6 — so a role
  list would not even describe what the user may do).
- Roles are an administrator concern; a user who needs them sees them on the Users screen when their
  role allows it.
- The payload stays cacheable per user without invalidation on every grant change (grants are re-read
  per request, srs-sec.md 1.3.0 §6).

## Consequences
- The frontend shows the profile from `/me` and builds navigation from `/menu`, as today.
- A test asserts the absence of `roles` and `permissions` keys (AC-SEC-092, `TC-CORE-SEC-047`).
- Non-breaking: a new endpoint; nothing existing changes.
