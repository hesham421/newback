# Handover to frontend — self-service logout endpoint

**Date:** 2026-09-18 · **Module:** SEC · **Repo:** `backend` · **Branch:** `main` (merged from `newback2`)
**Contracts touched:** None existing. One endpoint is **new and carries no API-SEC id yet** — see
[§4 About the missing id](#4-about-the-missing-api-sec-id).

---

## TL;DR

```
POST /api/v1/sec/auth/logout
Requires: a valid bearer token (isAuthenticated() — no permission gate)
Body: none
```

Terminates **the caller's own** session server-side and appends one `LOGOUT` audit row. If your
logout flow today only discards the token locally, that leaves the `ActiveSession` row (and the
token itself, until it expires) live on the server — call this endpoint first, then discard the
token client-side as you already do.

Everything is **additive**. No existing endpoint, request shape or response field changed.

---

## 1. What to call

```
POST /api/v1/sec/auth/logout
Authorization: Bearer <the caller's own access token>
```

No request body — the session is resolved from the caller's own token, not from an id you supply.

### Response — `SessionTerminationResponse` (the same shape `API-SEC-026` already returns)

```jsonc
{
  "success": true,
  "data": {
    "activeSessionPk": 42,
    "terminatedAt": "2026-09-18T18:04:11.203Z"
  },
  "timestamp": "…"
}
```

### Idempotent — safe to call more than once

A second call with the same (now-invalid) token is **not** an error. It returns the same
`activeSessionPk` and the **original** `terminatedAt` unchanged — no `409`, ever, on this endpoint.
(`SEC-409-ALREADY-TERMINATED` stays reserved for `API-SEC-026`, where an *administrator* is told
someone else's session was already closed — a caller closing their own has no second party to
report a conflict to.)

### After logout, the token is dead immediately

The same access token is rejected by every other endpoint right after this call returns — it does
not wait for its natural expiry. If your app currently relies on a token surviving until expiry
after "logout" (e.g. a stray in-flight request), that assumption no longer holds.

### It only ends the caller's own session

If a user is logged in on two devices/tabs, calling this from one does **not** end the other. That
is `API-SEC-009`'s bulk-termination behaviour (admin-side), not this endpoint's.

### Errors

None specific to this endpoint. An expired/invalid/missing token gets the usual `401` from the
authentication filter before this code ever runs — there is nothing further to handle in the
`catch` path of your logout call.

---

## 2. What to change on your side

- **Call this before discarding the token**, not instead of discarding it. The client still owns
  clearing its own stored token/state — this endpoint only closes the server-side record.
- **Don't block the UI on the response.** The logout flow is: fire this request, then clear local
  state and redirect, regardless of network hiccups on this call — a failed/timed-out call here
  should never trap a user on a logged-in screen. Worst case, the session self-expires later
  exactly as it does today.
- **No new field to render.** This is a fire-and-forget call from the UI's perspective; nothing in
  the response needs to reach a screen.

---

## 3. What did NOT change

- `API-SEC-026` (admin terminate-session-by-id) — untouched, same shape, same gate
  (`PERM_SEC_SESSIONS_TERMINATE`).
- `API-SEC-009` (bulk termination) — untouched.
- Login, sign-up, password reset — untouched.
- `SecurityConfig`'s `permitAll` list — logout is deliberately **not** on it. It falls through to
  `anyRequest().authenticated()`, same as every other protected endpoint.

---

## 4. About the missing API-SEC id

This endpoint carries **no `API-SEC-0xx` id**, because the SEC execution plan never declared a
self-service logout — same situation as the three endpoints in
`HANDOVER-FE-sec-frontend-gaps-2026-09-18.md`. The implementation was ported from a sibling branch
that had annotated its own code with forward-looking ids (`API-SEC-028`, `REQ-SEC-036`,
`AC-SEC-036`, `DBF-SEC-081/082`) — **none of those exist in this module's governance plan.** They
are code comments carried over from that branch, not assigned contract ids; treat them as
provisional, not authoritative, until the factory assigns a real one.

Recorded as an `ABSENT` entry with `resolution: RESOLVED` in

```
governance/shared/erp/modules/SEC/backend/execution-state.json → api_doc_gaps[]
```

The factory reads this with `gov.py feedback`; the next version's gate will not open while it's
unanswered.

---

## 5. Verified, not assumed

Full backend suite: **36 passed, 0 failed**, including a new `SecLogoutIntegrationTest` (3 tests).
Additionally exercised against a live instance.

| Check | Result |
|---|---|
| logout terminates the caller's own session, appends one LOGOUT audit row | `terminatedAt` stamped, `terminatedBy` = the caller's own username, exactly one audit row |
| a second, unrelated session of the same user | untouched — logout ends one session, not all |
| token rejected by the real authentication filter after logout | control passes before logout, fails after |
| repeat logout | same `activeSessionPk`, same original `terminatedAt`, no second audit row, no exception |

---

## 6. Where the docs are

Regenerated from the running app — **auto-generated, do not hand-edit**:

```
governance/shared/erp/modules/SEC/api-docs/endpoints/authentication.md   ← §1
governance/shared/erp/modules/SEC/api-docs/index.md                      ← catalog + traceability
```

This is the **shared submodule** — the frontend repo mounts the same bytes once published. Pull
the submodule; do not copy anything across.

> **Not published yet.** Nothing is committed anywhere until `./scripts/governance push` runs from
> the backend repo. If a submodule pull does not show this endpoint, that push has not happened.

Backend port is still **7272**, unchanged.
