# ADR-TENANT-002 — Per-tenant token cut-off (`TOKENS_INVALID_BEFORE`) instead of a `jti` denylist

Module  : TENANT     Version : erp-core 1.3.0 (tenant-maturity plan, package C.2)     Stage raised : P1 (SRS) — before the code
Status  : ACCEPTED (erp-core 1.3.0, package C12; written PROPOSED-before-code in the analysis commit, accepted after the
          code check, commit 6bfe756; amended in review round 1)

## Context
Since erp-core 1.2.0 a suspended tenant's tokens stop working at once, because `TenantResolutionFilter`
re-reads `CORE_TENANT` for every authenticated request and refuses a tenant that is not `ACTIVE`
(`erp-core/src/main/java/com/erp/tenant/security/TenantResolutionFilter.java:89-99`, REQ-TENANT-010).
Two gaps remained (`docs/plans/tenant-maturity-plan.md` §5 C.1, C.2):
- when the tenant is **re-activated**, every token issued before the suspension works again — a
  suspension never ended a session, it only paused it (`PlatformTenantApiIntegrationTest` asserted it);
- the platform had no way to **invalidate a tenant's tokens** without suspending it (a leaked token, a
  rotated administrator), and `SEC_ACTIVE_SESSION` rows stayed open after a suspension.

Access tokens are JWTs carrying `sub`, `jti` (= the session's `tokenRef`), `uid`, `tid`, `realm`, `iat`
and `exp` (`sec/security/JwtTokenIssuer.java:41-49`). `JwtAuthenticationFilter` already refuses a token
whose session row is terminated (REQ-SEC-028), and package C.1 terminates every session of a tenant on
suspension. Package B stores `CORE_TENANT.TOKENS_INVALID_BEFORE` (`V19__tenant_lifecycle.sql`,
DBF-TENANT-042) on every SUSPENDED → ACTIVE transition, not yet enforced. Two designs were available:
- **A denylist keyed by `jti`** (in the database, or Redis when present, as the download-token store
  does): each revoked token is listed until it expires; every request looks the `jti` up. Revoking "every
  token of a tenant" means enumerating its sessions; the list grows with tokens and needs a cleaner.
- **A per-tenant cut-off instant** on `CORE_TENANT`: a token is valid only if its `iat` is not before the
  instant; revoking a tenant's tokens is one `UPDATE` of a row the tenant filter reads anyway.

## Decision
**Per-tenant cut-off.** `TenantResolutionFilter` refuses, with 401 `TENANT_TOKEN_REVOKED`, a token whose
`iat` lies before its tenant's `TOKENS_INVALID_BEFORE` (RULE-TENANT-023). The cut-off is written by two
operations, both `PLATFORM_TENANT_MANAGE`: activation (`PATCH /{id}/status` to `ACTIVE`, RULE-TENANT-016)
and the new `POST /api/v1/platform/tenants/{id}/revoke-tokens` (audit `TOKENS_REVOKED`), which also ends
the tenant's sessions. No `jti` store is introduced; `jti` keeps its present use (the session reference).

How the token's facts reach the tenant filter: `JwtAuthenticationFilter` (SEC) puts a
`com.erp.tenant.TenantTokenFacts(tenantId, issuedAt)` request attribute for **every signature-valid
token**, whether or not it authenticated. A request attribute rather than a field of SEC's `AuthRealm`
details: the tenant module may not depend on `com.erp.sec.security` (ArchUnit module boundary; the type
lives in the tenant module's public root package, which SEC already uses for `TenantContext`), and the
fact is needed for a token that did **not** authenticate: after C.1 a suspended or revoked tenant's
sessions are terminated, so SEC drops such a token before the tenant filter runs. On a non-public path
the filter therefore checks the token's tenant even then — gone or suspended → 403 `TENANT_SUSPENDED`
(unchanged answer for "an issued token of a suspended tenant"), before its cut-off → 401
`TENANT_TOKEN_REVOKED` — and otherwise lets the request go on unauthenticated as before. A public path
(login, sign-up, password reset, the customer public paths) ignores a stale token, so a client that
still sends an old `Authorization` header can sign in again.

**Precision.** `iat` has whole-second precision (JWT NumericDate, `JwtTokenIssuer` writes seconds);
the cut-off is a `TIMESTAMPTZ` of microsecond precision. The comparison is made in whole seconds:
a token is refused when `iat` (seconds) **<** the cut-off truncated to the second. So a token issued in
the cut-off's own second is **accepted** — a fresh login right after an activation must work (the HTTP
suite signs in again within the same second, TC-CORE-TENANT-032 / -043), and no token of a suspended tenant
can exist in that second before the activation. For revoke-tokens the stored cut-off is itself the next whole
second (below), so the revoke's own second is refused. The alternative "`iat` ≤ cut-off second" refuses a fresh login for up
to one second after every activation and was rejected; so was a sub-second claim of our own (`iat` is
the standard claim every JWT library reads).

**Revoke-tokens' cut-off (review round 1).** Activation stores the activation instant (no token can be issued for a
suspended tenant, so a token of that second is a fresh login and is served). Revoke-tokens stores **the start of the
next whole second** after now: every token issued up to and including the revoke's own second is refused by the
cut-off alone — whatever happens to the sessions (a failed session step, a session re-opened, a login whose session
commits after the termination query). A login later in that second is refused too and simply signs in again a moment
later. If the session step fails after the cut-off committed, the failure is caught, `TOKENS_REVOKED` is recorded in
PLATFORM saying the sessions were not terminated, and the call answers 500 `TENANT_REVOKE_SESSIONS_FAILED`, retryable:
a repeated call moves the cut-off forward and ends the sessions.

**PLATFORM.** `revoke-tokens` refuses the PLATFORM tenant (422 `TENANT_REVOKE_TOKENS_PLATFORM`,
RULE-TENANT-024): it would sign every platform operator out, the caller included, and PLATFORM can never
be suspended either (RULE-TENANT-005). A platform operator's single session is ended through SEC's
session API; a compromised operator account is deactivated there.

Reasons:
1. **No per-request store.** The check rides on the `CORE_TENANT` row the filter already reads for every
   authenticated request (REQ-TENANT-010 / -012: the filter re-reads the row and caches nothing, so a cut-off is
   effective on the next request, like a suspension).
2. **Tenant-wide by construction.** The platform's cases are tenant-wide (re-activation, "sign everyone
   out"); single-token revocation is already SEC's session termination.
3. **Nothing to expire or clean.** One nullable instant per tenant, no retention job, no Redis
   dependency, no growth with the number of tokens.
4. **Additive.** No migration (V19 has the column); one filter branch; a tenant whose cut-off is NULL
   behaves as in 1.2.0.
5. **Defence in depth with C.1.** Session termination and the cut-off cover each other: a session row
   that survives (a failed after-commit listener) is still cut off on re-activation. For revoke-tokens the
   cut-off is complete on its own (next whole second, review round 1); ending the sessions is a clean-up
   whose failure is reported and retryable, never a gap.

## Consequences
- **Behaviour change:** a token issued before a tenant's re-activation is refused (401
  `TENANT_TOKEN_REVOKED`) instead of working again; clients sign in again (CHANGELOG `[TM-C12]`).
- Revocation granularity is the tenant, never one user: a per-user cut-off (`SEC_USER`) would be a later,
  SEC-owned extension reusing the same mechanism.
- Clock skew between nodes shifts the cut-off by that skew; both instants are server-side.
- The cut-off is never exposed: not in `TenantResponse`, not in the revoke-tokens response, not in an
  audit row (the entity audit drops it by the denylist word `token`; `TOKENS_REVOKED` summaries carry the
  tenant code and the session count only).
- `/api/v1/tenant/me` is served to both realms on the core chain and goes through the same filter
  branch, so a customer token is cut off like a staff token.
- The filter answers like `TENANT_SUSPENDED`: it clears the security context and writes the envelope
  itself (`FilterErrorResponseWriter`); the generated api-docs therefore do not list the code per
  endpoint — the TENANT addendum does.

## Traces
Accepted after the code check (commit 6bfe756); review round 1 amended Decision and Reason 5.
ENT-TENANT-001 · REQ-TENANT-010, REQ-TENANT-012, REQ-TENANT-026, REQ-TENANT-034, REQ-TENANT-035 · RULE-TENANT-006, RULE-TENANT-016,
RULE-TENANT-023, RULE-TENANT-024 · POL-TENANT-015 · DBF-TENANT-042 · SEC REQ-SEC-028, REQ-SEC-092,
REQ-SEC-093 · plan §5 C.1, C.2, §9
