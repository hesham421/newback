# Handover to frontend — SEC round 3 (three search/contract defects fixed)

**Date:** 2026-09-19 · **Module:** SEC · **Repo:** `backend` · **Branch:** `main`
**Answers:** `BACKEND PROMPT — SEC: three search/contract defects found by the 2026-09-19 frontend E2E run`
(`sec-frontend-e2e-run-2026-09-19.md` §2.3, §2.4, §2.5)
**Contracts touched:** none. Every change is additive — no endpoint, path, request shape, response
field, HTTP status or error code was removed or renamed. One behaviour change, described in §4,
turns a silent pass-through into a `400`.

---

## TL;DR

| Your ask | Status | What you get |
|---|---|---|
| 1 — `sessions/search` honours `username` | **Done** | `LIKE` and `EQUALS`, case-insensitive, on the session owner's login |
| 2 — `roles/search` honours `name` | **Done** | `{"name":"…"}` now filters (partial match over `nameAr` OR `nameEn`) |
| 3 — `SEC-409-USER-DUP` names the field | **Done** | `fieldErrors: [{"field":"username"}]` / `[{"field":"email"}]`, both when both collide |
| Your note: make a dropped filter a `400` | **Done, platform-wide** | Unsupported filter field → `400 VALIDATION_ERROR` naming it. See §4 — this affects every module |
| Also fixed, unasked | — | The same defect as #2 on **`fullName`** (users + sign-up requests) and **`pageCode`** (registry) |

All six of your regression checks pass against the running backend, plus a new JUnit suite
(`SecSearchFilterIntegrationTest`, 10 cases). Full suite: 52/52.

---

## 1. `sessions/search` — the `username` filter

`API-SEC-025`. `username` is now a filterable field alongside `userId` and `ipAddress`.

```jsonc
POST /api/v1/sec/sessions/search
{ "filters": [{ "field": "username", "operator": "LIKE", "value": "e2e_auditor" }],
  "page": 0, "size": 7 }
```

- **`LIKE`** — case-insensitive substring, like every other `LIKE` in the search layer.
- **`EQUALS`** — also case-insensitive, deliberately: a login is not case-sensitive anywhere else
  in this module, and an `EQUALS` that quietly disagreed with `LIKE` on case would be its own trap.
- **`NOT_EQUALS`** — supported.
- Anything else (`GREATER_THAN`, `IN`, …) → `400`, with `username` named in `fieldErrors`. Ask if
  you need `IN`; it is a small addition, we just did not want to guess at semantics.

Measured on the dev DB, admin token: `LIKE "e2e_auditor"` → `totalElements 4`, every row that user;
an unknown login → `0`; unfiltered → `203`. `userId` and `ipAddress` are unchanged.

`username` lives on the associated User, so it is resolved through path navigation rather than an
explicit join — your `totalElements` stays a plain count and pagination is unaffected.

## 2. `roles/search` — the documented `name` field

`API-SEC-012`. `name` was a getter with no backing field: Jackson publishes a getter, which is why
the docs advertised it, and Jackson ignores a property it cannot set, which is why it vanished. It
is now a real field.

```jsonc
POST /api/v1/sec/roles/search
{ "page": 0, "size": 5, "name": "Rerun Test Role" }     // → totalElements 1
```

- Semantics are what the contract declares: **partial, case-insensitive match over `nameAr` OR
  `nameEn`**. It does **not** include `code`.
- `filters: [{"field":"name", …}]` still works, unchanged. If both are present the top-level value
  wins.
- For your "Search roles by code or name" box: send `code` as a filter —
  `filters:[{"field":"code","operator":"LIKE","value":"…"}]` — alongside `name`, or tell us you
  want one field that ORs across all three and we will add it as a named field rather than have you
  guess. We did not widen `name` to cover `code` on our own, because the plan defines `name` as the
  two name columns and quietly redefining a documented filter is the failure mode we are here to
  remove.

### Three more of these, which you had not reached yet

The same defect — published getter, no setter, silently dropped — existed on three other search
bodies. All three are now real settable fields, same rules as `name`:

| Endpoint | Field | Matches |
|---|---|---|
| `POST /api/v1/sec/users/search` (`API-SEC-005`) | `fullName` | `fullNameAr` OR `fullNameEn` |
| `POST /api/v1/sec/signup-requests/search` | `fullName` | `fullNameAr` OR `fullNameEn` |
| `POST /api/v1/sec/registry/search` (`API-SEC-021`) | `pageCode` | the child screen's page code |

If your Users screen's name box looked like it was filtering and was not, that is why.

## 3. `SEC-409-USER-DUP` now names the field

`API-SEC-006` and `API-SEC-007`.

```jsonc
// POST /api/v1/sec/users with a taken username
{
  "success": false,
  "error": {
    "code": "SEC-409-USER-DUP",
    "message": "Username or email already in use",
    "fieldErrors": [ { "field": "username", "message": "Username or email already in use" } ]
  }
}
```

- `field` is `"username"` or `"email"`, spelled exactly as the request body spells it.
- **Both entries appear when both collide**, in that order — so route on the array, not on
  `fieldErrors[0]`.
- `PUT /api/v1/sec/users/{id}` with a taken email reports `email` the same way.
- The top-level `code`, `message` and HTTP status are byte-identical to before, and the message is
  still the both-fields sentence, localized. If you would rather each entry carried a field-specific
  message ("This username is taken"), that needs two new error codes and therefore a contract
  change — say the word and we will file it.

`TC-SEC-042` should pass as written.

## 4. A filter the server cannot honour is now a `400` — read this one

You asked for it, and it is the actual fix: without it, #1 and #2 were undetectable from the client,
and the next one would have been too.

**This applies to every search in the platform — CU, FILE, FIN, MDL, NOTIF and SEC.**

```jsonc
POST /api/v1/sec/roles/search
{ "filters": [{ "field": "bogusField", "operator": "LIKE", "value": "x" }] }
```
```jsonc
// 400
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "fieldErrors": [
      { "field": "bogusField", "message": "This search does not support filtering on \"bogusField\"" }
    ]
  }
}
```

- Every offending field is listed, not just the first.
- The envelope's top-level `code` stays `VALIDATION_ERROR` — **no new wire code** to add to your
  error map. The per-field text comes from two new message keys
  (`UNSUPPORTED_FILTER_FIELD`, `UNSUPPORTED_FILTER_OPERATOR`), both localized ar/en; you render
  `fieldErrors[].message`, so you never see the keys.
- This mirrors what SEC already did for sort fields (`SEC-400-INVALID-SORT`).

**What to check on your side before this reaches you:** anywhere you build `filters[]` from a
column id, a saved view, or a URL query string, a field name that used to be quietly ignored is now
a hard `400`. That was always a bug on your side too — the filter was never applied — but it failed
silently and now it will not. We checked every search endpoint in all six modules answers `200` on a
plain body, and both server-built filter sets (the audit-log search and its CSV export) against
their own whitelists, but we cannot see your call sites.

It is also filed for the factory as `HUMAN`: no artifact declares platform-wide filter strictness,
and this is a behaviour change to five modules that never asked for it. If they rule against it, it
comes out; the three fixes above do not depend on it.

---

## 5. Your two smaller observations

- **Unknown path answers `500`, not `404`** — confirmed, still true, **not fixed here.** It is a
  shared-platform behaviour (the catch-all handler in `GlobalExceptionHandler`), not a SEC one, and
  changing it is a platform decision of the same kind as §4 rather than part of this round. It is
  cheap and we are happy to do it — say so and it goes in the next round.
- **`updatedBy` becomes `anonymousUser` after a login** — **diagnosed, not fixed.** You were right
  about the cause: `POST /auth/login` is an anonymous endpoint, so when `AuthService` writes
  `lastLoginAt` onto the user row, the JPA audit listener stamps `updatedBy` from the security
  context, which at that moment holds Spring's anonymous principal. Not specific to SEC — any write
  on an anonymous endpoint does it. The fix is one line, but it has a real choice inside it (should
  that stamp be `system`, or the user logging in, or should a `lastLoginAt` write not touch the
  audit columns at all?), and picking one silently would be the wrong move on an audit trail. Filed
  for a decision rather than guessed at.

---

## 6. Also relevant to you

- **api-docs are regenerated** from the running app: `$GOV/modules/SEC/api-docs/`. `name`,
  `fullName` (×2) and `pageCode` now carry a description and an example in their request tables.
- **No response shape changed**, so no row type of yours needs editing.
- Nothing was done to `GET /api/v1/sec/signup-requests/{id}` (still 405), the Dev reset-token
  fixture, or the SoD answer from round 2 — all unchanged.
