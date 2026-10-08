# ADR-SEC-066 — Logout endpoint exists; a session is keyed by the token's `jti` and terminated on logout

Module  : SEC     Version : erp-core 1.2.0 (as-built)     Stage raised : implementation record (analysis-coverage review, 2026-10-08)
Status  : ACCEPTED (non-breaking) — supersedes ADR-SEC-008 in part

## Context
ADR-SEC-008 (frontend stage, v1) recorded that no logout endpoint was published and that a session
ends only by expiry (`expiresIn`) or by an administrator's termination (REQ-SEC-028); the SRS A4 has
no logout requirement, while A6 lists the `AUDIT_EVENT_TYPE` value `LOGOUT` and ENT-SEC-010 gives a
session a `tokenRef`. The backend implemented the endpoint anyway — `POST /api/v1/sec/auth/logout`
has shipped with every published erp-core version (1.1.0, the first published, and 1.2.0;
`docs/api-docs/sec/endpoints/authentication.md`), its service Javadoc cites REQ-SEC-036 and the
integration test cites AC-SEC-036, ids the vendored analysis never defined. Three designs were
possible for ending one's own session:
- No endpoint (ADR-SEC-008): the client discards the token; the server-side session row stays open
  until expiry, and `LOGOUT` is never written.
- A logout that takes a session id: the caller names the session, as the administrator does in
  REQ-SEC-028, and may name someone else's.
- A logout that resolves the caller's own session from the bearer token and ends exactly that one.

## Decision
The third. `AuthService.logout` (`erp-core/src/main/java/com/erp/sec/service/AuthService.java:129-187`,
`AuthController.java:61-66`) is gated by `isAuthenticated()` with no permission, reads the bearer's
`jti` claim, and resolves the session through `ActiveSessionRepository.findByTokenRef` — the access
token's `jti` IS `SEC_ACTIVE_SESSION.TOKEN_REF` (DBF-SEC-077), a random UUID minted per login
(`AuthService.java:95-103`, `JwtTokenIssuer.java:43-54`), and the same key `JwtAuthenticationFilter`
uses to admit the token (`JwtAuthenticationFilter.java:144-148`). An active session is terminated
(`terminatedAt`, `terminatedBy` = the caller), one `LOGOUT` row goes to `SEC_AUDIT_LOG` and one to
`CORE_AUDIT_EVENT` (`AuditApi.ACTION_LOGOUT`, step 10, DEVIATIONS [10]), and the response is the
`SessionTerminationResponse` the administrator's termination also returns. The call is idempotent: a
session already terminated is returned as it stands and `SEC-409-ALREADY-TERMINATED` is not raised —
that code exists for REQ-SEC-028, where an administrator has a second party to report the conflict
to. The terminated session makes the token unusable from the next request, because the filter
authenticates against the session row, not the password hash. There is no customer logout endpoint
(`docs/steps/06-report.md`).

## Consequences
- REQ-SEC-036 / AC-SEC-036 (logout) are recorded as as-built rows in `P1/srs-sec.md` →
  "Implementation Addendum — erp-core 1.2.0" §2, the one id this revision mints, because the code
  already cites it; ADR-SEC-008's "no logout affordance" bullet is superseded (note appended there).
- A caller can only ever end the session behind the token they present: no session id travels, so
  the endpoint cannot be turned against another user's session.
- The `jti` ↔ `TOKEN_REF` equality is load-bearing for logout, for the per-request session check and
  for every termination path (administrator, deactivation, password reset); a token issued without a
  session row is never accepted.
- `LOGOUT` now has a writer, so all 14 `AUDIT_EVENT_TYPE` values of A6 are written by the code.
- Non-breaking: an additive endpoint; no table, column, permission or error code was added for it.

## Traces
REQ-SEC-036, AC-SEC-036 (as-built) · REQ-SEC-024, REQ-SEC-028, REQ-SEC-033 · ENT-SEC-010, ENT-SEC-011 ·
DBF-SEC-077, DBF-SEC-081, DBF-SEC-082 · ADR-SEC-008 (superseded in part) · docs/steps/06-report.md ·
docs/DEVIATIONS.md [10] · `SecLogoutIntegrationTest`
