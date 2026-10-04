# Handover to frontend — SEC round 2 (read-one endpoints, Dev reset token, SoD answer)

**Date:** 2026-09-18 · **Module:** SEC · **Repo:** `backend` · **Branch:** `main`
**Answers:** `BACKEND ASKS — SEC, ROUND 2` (from `sec-frontend-e2e-run-2026-09-18.md`)
**Contracts touched:** none existing. Two new endpoints, both **without an API-SEC id** — the
factory assigns those, as with logout last round. Everything below is additive.

---

## TL;DR

| Your ask | Status | What you get |
|---|---|---|
| 1 — `GET /roles/{id}`, `GET /users/{id}` | **Done** | 200 with the same `RoleResponse` / `UserResponse` the search returns; 404 `SEC-404-ROLE` / `SEC-404-USER` |
| 2 — a way to read a reset token in Dev | **Done** | `POST /api/v1/sec/dev/password-reset-token` — Dev profile only |
| 3 — is `SEC-409-SOD-CONFLICT` dormant? | **Answer: (a)** | Intentionally dormant. Record TC-SEC-050 as unreachable-by-design and stop reporting it. |

---

## 1. Read-one endpoints

```
GET /api/v1/sec/roles/{id}    → 200 RoleResponse · 404 SEC-404-ROLE
GET /api/v1/sec/users/{id}    → 200 UserResponse · 404 SEC-404-USER
```

Permissions are the same as the corresponding search — `PERM_SEC_ROLES_VIEW` /
`PERM_SEC_USERS_VIEW` — because neither exposes a field the search did not already return.

The payload is **byte-for-byte the shape you already parse from the search page**, so your existing
row type needs no change. For users that includes the `roles` array added last round:

```jsonc
// GET /api/v1/sec/users/636
{
  "success": true,
  "data": {
    "userPk": 636,
    "username": "e2e_auditor",
    "email": "e2e_auditor@example.com",
    "fullNameAr": "مدقق اختبار",
    "fullNameEn": "E2E Auditor",
    "statusCode": "ACTIVE",
    "lastLoginAt": "2026-09-18T18:10:28.548Z",
    "isActiveFl": true,
    "roles": [
      { "roleId": 343, "code": "E2E_AUDIT_ONLY", "nameAr": "مدقق السجل فقط", "nameEn": "Audit log reader only" }
    ],
    "createdAt": "…", "createdBy": "admin", "updatedAt": "…", "updatedBy": "anonymousUser"
  },
  "timestamp": "…"
}
```

```jsonc
// GET /api/v1/sec/roles/999999
{
  "success": false,
  "error": { "code": "SEC-404-ROLE", "message": "Role not found", "fieldErrors": null },
  "timestamp": "…"
}
```

Your acceptance criterion — opening `/security/roles?editId=<id>` in a fresh tab — is now
satisfiable: fetch the row by id when it is not in the loaded page instead of redirecting to the
list. ADR-SEC-008 can be revisited on the same basis.

**`GET /api/v1/sec/signup-requests/{id}` was deliberately not added**, exactly as you asked. It
still answers 405. Say the word if that changes.

---

## 2. Obtaining a valid password-reset token in Dev

We took your option 2, gated on the Dev profile.

```
POST /api/v1/sec/dev/password-reset-token
Authorization: Bearer <any authenticated caller>
Content-Type: application/json

{ "email": "someone@example.com" }
```

```jsonc
{
  "success": true,
  "data": {
    "token": "c9eb8c17-3ac5-4a99-aa4e-209a5635215f",
    "expiresAt": "2026-09-18T19:08:59.158Z"
  },
  "timestamp": "…"
}
```

Spend `token` at the endpoint you already call:

```
POST /api/v1/sec/auth/password-reset/complete
{ "token": "<token>", "newPassword": "…" }      → 200
```

### Three things worth knowing

- **It mints a fresh token; it does not read back the one `API-SEC-003` mailed.** That token is
  unrecoverable by design — only its SHA-256 hash is stored, and the raw value leaves the process
  in the mail and nowhere else. The minted token is the same entity, the same hashing and the same
  30-minute window, so `API-SEC-004` cannot tell the difference. It writes no audit row and sends
  no mail, because it is a test fixture and not a second implementation of `API-SEC-003`.
- **It requires an authenticated caller.** Only the four pre-authentication endpoints are on the
  permitAll list, and we did not add a fifth. Your harness will need a token (admin is fine) for
  the token-fetch step; the reset flow itself stays anonymous as before.
- **It does not exist outside Dev.** `@Profile("dev")` — verified on a prod-profile instance,
  where the path has no handler while `GET /roles/{id}` still answers 200. An unknown path in this
  app currently surfaces as a 500 rather than a 404 (pre-existing global behaviour, unrelated to
  this endpoint) — so do not treat a 500 there as the endpoint being broken.

Unknown email → `404 SEC-404-USER`. This does reveal whether an address is registered, which is
precisely what `API-SEC-003` refuses to do; that is safe only because this endpoint is Dev-only
and authenticated, and it is why it must never be promoted out of the Dev profile.

**Flagged for the factory, not settled:** this is the module's first non-contract endpoint. It is
recorded as `HUMAN` in `execution-state.json` so the factory can decide whether a Dev-only fixture
belongs in delivered source at all, or whether a MailHog-style catcher declared as *environment*
is the governed answer. It is cheap to remove if they say so.

---

## 3. SoD — answer (a), intentionally dormant

`RULE-SEC-005` is fully wired and deliberately inert.

- Both backend guards are live: `UserRoleService.holdsConflictingAction` on assignment, and
  `RoleGrantService.anyUserOfRoleHoldsConflictingAction` on grant. Both route through their Domain
  objects to `SEC-409-SOD-CONFLICT`.
- Both resolve an **empty** counterpart set, because SEC v1 declares no conflicting-pair source
  anywhere. The platform's only real pair is FIN-owned and FIN-enforced
  (`modules/FIN/P3_1/backend-execution-plan-fin.md:1061-1066`).

We are **not** declaring a Dev pair. A fabricated pair would exercise the plumbing against a fact
the platform does not assert, and would then have to be remembered and removed. With your guard
live on both paths and ours live on both paths, the rule is complete end to end; there is simply
nothing to trigger it.

**So: record TC-SEC-050 (and our TC-SEC-020) as unreachable-by-design and stop reporting them as
gaps.** The real decision — declare where a conflicting pair comes from, or state in the plan that
`RULE-SEC-005` stays dormant in v1 — is the factory's, and is filed for them as `HUMAN`.

---

## 4. Also relevant to you

- **api-docs are regenerated** from the running app: `$GOV/modules/SEC/api-docs/`. Both read-one
  endpoints are in `endpoints/roles.md` / `endpoints/users.md`; the Dev fixture is documented in a
  new `endpoints/dev-support.md`. All three appear as **undeclared** in the Contract Traceability
  table — that is the missing API-SEC id, not a defect.
- **`e2e_auditor`'s password is now `N3wP@ssw0rd!`** — we spent a real reset on it while verifying
  the flow end to end against the dev DB. It is on your own fixture-cleanup list, but if your run
  hardcodes its old password, that is why it broke.
- Nothing else changed. No existing endpoint, request shape, response field, status code or error
  code was touched.
