# ADR-NOTIF-005 — Dispatch is gated by authentication only, with caller-supplied variables.email — accepted risk, to be revisited

Module  : NOTIF    Version : erp-core 1.2.0    Stage raised : P3 (implementation — steps 02, 14)
Status  : ACCEPTED (non-breaking; to be revisited)

## Context
RULE-NOTIF-005 delegates authorisation to the Security filter, and API-NOTIF-001 (`POST
/api/v1/notifications/dispatch`) is tied to no screen, so no `PERM_*` row can be seeded for it
(`SEC_PERMISSION.PAGE_FK` is NOT NULL). The step-02 implementation therefore gated `DispatchService.dispatch`
and `dispatchSystem` with `@PreAuthorize("isAuthenticated()")`; SEC's in-process callers satisfy it through
`InternalCallerContext`. Step 14 added the `variables.email` override so that a password-reset mail can be
addressed even when the `REQUIRES_NEW` dispatch cannot see the account the caller's transaction created, and
dispatch fills the recipient's account e-mail in when the request supplies none.

Taken together, any authenticated staff principal of a tenant can call `POST /dispatch` with any active
template of the tenant (`PASSWORD_RESET` included), any eligible recipient, any `variables` — including
`email` (an arbitrary external address) and `actionLink` (an arbitrary URL that the EMAIL provider renders as
the call-to-action button) — and the platform sends it from `spring.mail.username`. There is no rate limit
and no audit entry for a dispatch. This is an open-relay / phishing surface limited only by who holds a
staff account.

## Decision
Accepted as is for 1.2.0: the HTTP endpoint stays `isAuthenticated()`, the `variables.email` override stays
available to HTTP callers and in-process callers alike, and the risk is recorded here rather than in a
silent "unchanged" row. The decision is explicitly marked to be revisited; the candidates, none of which is
chosen yet, are:
1. a dispatch permission — a `NOTIF:DISPATCH`-style authority declared like the report authorities (which
   need no screen row), or a screen row for a dispatch console;
2. a recipient-only mode for HTTP — the endpoint ignores `variables.email` and addresses the recipient's
   account e-mail, keeping the override for in-process callers (`NotificationDispatchApi`) only;
3. an allow-list of templates that may be dispatched over HTTP and/or whose `email` override is permitted
   (`PASSWORD_RESET` and the customer templates would be in-process only);
4. a dispatch rate limit per principal and an audit event per HTTP dispatch.

## Consequences
- The frontend and operators must treat every staff account as able to send platform e-mail; the inbox and
  lookup endpoints carry the same gate but expose only the caller's own data.
- Any mitigation above is additive (a new authority, a property, a seed row) and fits a MINOR release;
  option 2 changes the HTTP contract of `variables.email` and must be announced in `docs/api-docs/notif/`.
- Until revisited, the test plan (`docs/test-api/core-test-plan.md`) treats a 200 from `POST /dispatch`
  by a permission-less staff user as expected behaviour.

## Traces
RULE-NOTIF-005, RULE-NOTIF-011, RULE-NOTIF-019, RULE-NOTIF-022 · API-NOTIF-001 · srs.md addendum §2
(decision row), §5 · business-policies-notif.md addendum row 14 · code:
`erp-core/src/main/java/com/erp/notif/service/DispatchService.java:69-73, 98-100, 171-180`,
`dto/DispatchRequest.java:54-60`, `channel/EmailChannelProvider.java:49-64, 91-110`,
`com/erp/sec/security/InternalCallerContext.java` · docs/steps/08-report.md · DEVIATIONS [08], [14], [15]
