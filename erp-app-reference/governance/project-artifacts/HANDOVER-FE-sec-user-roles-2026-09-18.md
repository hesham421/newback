# Handover to frontend — user roles are now returned on every user read (SEC)

**Date:** 2026-09-18 · **Module:** SEC · **Repo:** `backend` · **Branch:** `newback2`
**Contracts touched:** `API-SEC-005`, `API-SEC-006`, `API-SEC-007` (`API-SEC-008` unchanged)

---

## TL;DR

`roles` is now populated on **every** user-shaped response, not just on the response to
`PUT /users/{id}/roles`. A user holding no roles returns `"roles": []` — never a missing key,
never `null`. Separately, `POST /users` accepts an optional `roleIds`, so creating a user and
giving them roles is now one call instead of two.

The temporary client-side workaround can be deleted. See [What to delete](#what-to-delete).

No new endpoints were added, and no existing request shape became stricter. **Nothing you have
today breaks.**

---

## 1. What changed on the wire

### Before

| Contract | Endpoint | `roles` in the response? |
|---|---|---|
| `API-SEC-008` | `PUT /api/v1/sec/users/{id}/roles` | yes |
| `API-SEC-005` | `POST /api/v1/sec/users/search` | **no — key absent** |
| `API-SEC-006` | `POST /api/v1/sec/users` | **no — key absent** |
| `API-SEC-007` | `PUT /api/v1/sec/users/{id}` | **no — key absent** |

The response to a *write* was the only place in the platform where a user's roles were visible.
There is no `GET /users/{id}` and no `GET /users/{id}/roles` — that has not changed, and none was
added. The four contracts above were always documented as carrying `roles`; the backend simply
never filled it.

### Now

All four return it, in the shape `API-SEC-008` already used:

```jsonc
{
  "userPk": 15,
  "username": "u2_198589",
  "email": "u2upd_198589@example.com",
  "fullNameAr": "أحمد علي",
  "fullNameEn": "Ahmed Ali",
  "statusCode": "DISABLED",
  "lastLoginAt": "2026-09-12T07:36:31.609Z",
  "isActiveFl": false,
  "roles": [
    { "roleId": 223, "code": "FILE_ADMIN", "nameAr": "مدير الملفات", "nameEn": "File Administrator" }
  ],
  "createdAt": "…", "createdBy": "admin", "updatedAt": "…", "updatedBy": "admin"
}
```

A user with no roles:

```jsonc
{ "userPk": 539, "username": "norole_1234", "roles": [], "…": "…" }
```

**`[]` vs missing is now meaningful.** Previously "holds no roles" and "nobody asked about roles"
were indistinguishable — both were an absent key. Now the key is always present, so an empty array
means *this user holds no roles*, full stop. If you were treating `roles == null` as "unknown,
don't render", that branch is dead: render the empty state directly.

The search page is loaded with one batched query for the whole page, so turning on `roles` in the
list does not cost you a query per row. Page size is not a performance concern here.

---

## 2. `POST /api/v1/sec/users` — optional `roleIds`

```jsonc
{
  "username": "roletest_1",
  "email": "roletest_1@example.com",
  "fullNameAr": "اختبار",
  "fullNameEn": "Role Test",
  "password": "N3wP@ssw0rd!",
  "roleIds": [1, 2]          // ← new, OPTIONAL
}
```

- **Optional.** Omitting it, or sending `[]`, creates the user with no roles — exactly today's
  behaviour. Your current create form needs no change to keep working.
- The response carries the roles that were actually assigned, so you do not need a follow-up read.
- It is **atomic**: if any role id is rejected, the user is not created either. There is no
  half-configured account to clean up. Verified — see §5.
- Assignment at creation writes the same `ROLE_ASSIGNED` audit rows a normal assignment does.

### Permissions — read this before adding role pickers to the create form

`POST /users` requires `PERM_SEC_USERS_CREATE`. When `roleIds` is **non-empty**, it *additionally*
requires `PERM_SEC_USERS_UPDATE` — the same permission `PUT /users/{id}/roles` demands. Otherwise
"create" would be a side door around the assignment gate.

Practical consequence for the UI:

- A user holding `CREATE` but not `UPDATE` can still create accounts. Show the create form.
- For that user, **hide or disable the role picker** — sending a non-empty `roleIds` gets
  `403 SEC-403-FORBIDDEN` and creates nothing.
- Empty `[]` never triggers the extra requirement, so a disabled/untouched picker is safe.

The check runs *before* the insert, so a denial writes nothing at all.

---

## 3. What to delete

`API-SEC-005` now returns `roles`, so the session-memory workaround that cached the
`API-SEC-008` response is obsolete:

- `mxdashboard/src/modules/security/features/users/hooks/useUserRoleAssignmentStore.ts` — delete
- `withConfirmedAssignments` in `.../hooks/useUsersFacade.ts` — delete

The bug they existed for (SCR-SEC-004 showing "لا يوجد دور معين" for a user whose roles had just
been saved, making a successful write look failed) is fixed at the source: the list row now carries
the real roles, so the drawer can be built from it directly.

---

## 4. Error codes to handle

| Code | HTTP | When |
|---|---|---|
| `SEC-404-ROLE` | 404 | a `roleId` in `roleIds` does not exist (create or assign). Nothing is written. |
| `SEC-403-FORBIDDEN` | 403 | create-with-roles without `PERM_SEC_USERS_UPDATE` (see §2). Nothing is written. |
| `SEC-409-SOD-CONFLICT` | 409 | RULE-SEC-005 separation-of-duties violation. Reachable from create-with-roles now, not only from assign. |

All three arrive in the standard envelope:

```jsonc
{ "success": false, "error": { "code": "SEC-404-ROLE", "message": "Role not found", "fieldErrors": null }, "timestamp": "…" }
```

`SEC-409-SOD-CONFLICT` resolves to nothing today (the platform declares no conflicting action
pairs yet, and the only real pair is FIN-owned and FIN-enforced), but the guard is live on both
paths, so handle it rather than assume it cannot fire.

---

## 5. Verified, not assumed

Run live against the app plus 8 new integration tests (full backend suite: 28 passed, 0 failed).

| Check | Result |
|---|---|
| search returns roles | `u2_198589` → `[{ roleId: 223, code: "FILE_ADMIN", … }]` |
| no roles ⇒ `[]` | 8-row page, 0 rows missing the key |
| no query-per-row | one batched `IN (?,?,?,?,?,?,?,?)` for the 8-row page |
| create with `roleIds` | roles returned; `ROLE_ASSIGNED` audit row confirmed in the DB |
| create without `roleIds` | succeeds, `roles: []` — backward compatible |
| unknown `roleId` | `SEC-404-ROLE`, and the user does not exist afterwards |
| `PUT /users/{id}` | returns the user's existing roles |
| `PUT /users/{id}/roles` | still **replaces** the set — assigning `[]` clears it |

---

## 6. Ports and where the docs are

| | Port | Notes |
|---|---|---|
| Backend | **7272** | `server.port` in `application.properties`. Unchanged. |
| Frontend dev server | **4200** | The backend's `app.frontend-url` defaults to `http://localhost:4200` (override with `FRONTEND_URL`). Confirmed free on this machine as of 2026-09-18. |

There is no CORS allow-list configured in the backend, so nothing there needs a port change.

**Regenerated API docs** (auto-generated from the running app, do not hand-edit):

```
governance/shared/erp/modules/SEC/api-docs/endpoints/users.md
```

The `UserResponse.roles` description and the new `UserCreateRequest.roleIds` row are both in
there. This is the shared submodule — the frontend repo mounts the same bytes, so pull the
submodule rather than copying anything across.

---

## 7. Not in scope

Deliberately **not** done, so nobody goes looking for them:

- No `GET /users/{id}` and no `GET /users/{id}/roles` were added. The ask was to fill contracts
  that already documented `roles`, not to widen the API surface.
- `PUT /users/{id}/roles` semantics are unchanged: `roleIds` **replaces** the set, it does not
  append.
- `roleIds` on create was not made mandatory and will not be.
