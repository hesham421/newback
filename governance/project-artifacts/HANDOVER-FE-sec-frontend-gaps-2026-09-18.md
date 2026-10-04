# Handover to frontend — the four SEC gaps from your 2026-09-18 E2E run are closed

**Date:** 2026-09-18 · **Module:** SEC · **Repo:** `backend` · **Branch:** `newback2`
**Answers:** `sec-frontend-e2e-run-2026-09-18.md` / `BACKEND FIX HANDOFF — SEC`
**Contracts touched:** `API-SEC-024` (charset only). Three endpoints are **new and carry no
API-SEC id yet** — see [§6 Contract ids](#6-about-the-missing-api-sec-ids).

---

## TL;DR

All four items are implemented, verified against the running app, and covered by tests.

| # | What you asked for | Now | Severity you gave it |
|---|---|---|---|
| 1 | Sign-up request list | `POST /api/v1/sec/signup-requests/search` | HIGH |
| 2 | A role's currently-held grants | `GET /api/v1/sec/roles/{id}/grants` | HIGH |
| 3 | Role update | `PUT /api/v1/sec/roles/{id}` | MEDIUM |
| 4 | CSV export charset | `text/csv;charset=UTF-8` + UTF-8 BOM | MEDIUM |

Everything is **additive**. No existing request shape got stricter, no response field was removed
or renamed. **Nothing you have today breaks** — with one caveat if you generate a typed client
from the OpenAPI document, see [§7](#7-one-thing-to-watch-if-you-generate-a-client).

Both "no published endpoint" disclosures can come out of the UI. See
[What to delete](#5-what-to-delete).

---

## 1. Sign-up request list — unblocks TC-SEC-040, TC-SEC-041

```
POST /api/v1/sec/signup-requests/search
Requires: PERM_SEC_USERS_VIEW
```

Built to the **same search contract as `API-SEC-012`** (roles search), so if you already have a
paged-search client you can point it here unchanged.

### Request

```jsonc
{
  "filters": [
    { "field": "statusCode", "operator": "EQUALS", "value": "PENDING" }
  ],
  "sortField": "submittedAt",
  "sortDirection": "ASC",
  "page": 0,
  "size": 20
}
```

**Filterable / sortable fields:** `email`, `fullNameAr`, `fullNameEn`, `statusCode`,
`submittedAt`.

Plus one lifted convenience filter, exactly like `fullName` on the users search and `name` on the
roles search:

- **`fullName`** — matches `fullNameAr` **OR** `fullNameEn`, case-insensitive substring. It is a
  filter entry like any other (`{ "field": "fullName", "operator": "LIKE", "value": "ali" }`); it
  is lifted out of the generic set because the shared search layer has no OR operator. It is
  **not** a sortable field.

`statusCode` values are the `SIGNUP_STATUS` closed set: `PENDING`, `APPROVED`, `REJECTED`.
Sending no `statusCode` filter returns all three — the pending screen must filter explicitly.

### Response — `Page<SignupRequestResponse>`

```jsonc
{
  "success": true,
  "data": {
    "content": [
      {
        "signupRequestPk": 1,
        "email": "testuser_54e1fb18@example.com",
        "fullNameAr": "اختبار المستخدم",
        "fullNameEn": "Test User",
        "submittedAt": "2026-09-12T07:34:35.577Z",
        "statusCode": "PENDING",
        "reviewedBy": null,
        "reviewedAt": null
      }
    ],
    "totalElements": 2, "number": 0, "size": 20, "first": true, "last": true, "empty": false
  },
  "timestamp": "…"
}
```

`reviewedBy` / `reviewedAt` stay `null` until a decision is taken. An empty match is
`200` with `content: []` — never a 404.

### The dashboard contradiction is gone

The `PENDING` filter and the dashboard's `usersOverview.pendingSignups` now read the same rows.
Verified live on this machine: dashboard reported `pendingSignups: 2`, the filtered list returned
exactly those 2.

### The decision endpoint is unchanged

`PATCH /api/v1/sec/signup-requests/{id}` (`API-SEC-011`) is untouched — same body, same
split payload (the created `UserResponse` on `APPROVE`, the updated `SignupRequestResponse` on
`REJECT`). Your approve/reject wiring keeps working as-is; only the list in front of it is new.

---

## 2. A role's held grants — unblocks TC-SEC-049, uncripples TC-SEC-047/048/051

```
GET /api/v1/sec/roles/{id}/grants
Requires: PERM_SEC_ROLES_VIEW
```

One nested **module → screen → action** tree, deliberately shaped like `API-SEC-027`'s menu so you
can diff it against `API-SEC-021`'s registry tree node-for-node.

### Response — `RoleGrantTreeResponse`

```jsonc
{
  "success": true,
  "data": {
    "rolePk": 1,
    "code": "SYS_ADMIN",
    "nameAr": "مدير النظام",
    "nameEn": "System Administrator",
    "modules": [
      {
        "moduleRegPk": 1,
        "code": "SEC",
        "nameAr": "الأمان",
        "nameEn": "Security",
        "granted": true,
        "grantedAt": "2026-09-11T21:33:36.182Z",
        "screens": [
          {
            "screenRegPk": 4,
            "pageCode": "SEC_USERS",
            "nameAr": "المستخدمون",
            "nameEn": "Users",
            "granted": true,
            "grantedAt": "2026-09-11T21:33:36.182Z",
            "actions": [
              {
                "actionRegPk": 1,
                "actionCode": "VIEW",
                "permissionCode": "PERM_SEC_USERS_VIEW",
                "nameAr": "المستخدمون - عرض",
                "nameEn": "Users - VIEW",
                "grantedAt": "2026-09-11T21:33:36.182Z"
              }
            ]
          }
        ]
      }
    ]
  }
}
```

### How to read it — three rules that matter for the checkbox tree

1. **Only held grants appear.** A registry node absent from this tree is a grant the role does
   **not** hold. That is the whole point: an unchecked box now means *not granted*, and the
   warning text can go.
2. **Actions have no `granted` flag** — presence *is* the grant. Modules and screens do have one,
   because of rule 3.
3. **`granted: false` is possible on a module or screen node, and it is not noise.** The tree is
   built from the *union* of what all three grant levels reach, not from module grants alone. So a
   role holding an action whose screen grant is missing still shows that screen — flagged
   `granted: false`, with `grantedAt: null`, carrying the action beneath it. `RULE-SEC-001/002`
   forbid creating that state through the API, so it can only arrive as data drift — but an audit
   read that silently dropped it would hide exactly the thing an audit is for. **Render it as a
   distinct state** (indeterminate / warning), not as checked and not as absent.

Ordering is stable: modules by `code`, screens by `pageCode`, actions by `actionCode`.

### The revoke confirmation (TC-SEC-049)

`DELETE /api/v1/sec/roles/{id}/modules/{moduleId}` cascades away every screen and action grant
under that module (`RULE-SEC-003`). **The subtree under a `granted: true` module node is exactly
what that cascade will remove** — enumerate it to name the consequences in the confirmation
dialog before the call.

After the call, the existing `ModuleGrantRevokeResponse` (`{ revokedScreenGrants,
revokedActionGrants }`) still reports what actually happened, so you can reconcile your
pre-computed list against the real counts.

### No active-flag filtering — read this before you diff against the registry

This endpoint applies **no `isActiveFl` predicate at any level**. It is an audit of what the role
holds, not an effective-access resolution, so a grant pointing at a deactivated registry row is
still returned. `API-SEC-021`'s registry tree, by contrast, returns only **active** screens and
actions.

Practical consequence: a node can exist here and be missing from the registry tree. Do not treat
that as an error — it is a grant on something since deactivated, and it is worth surfacing.
`API-SEC-027` (the menu) is the one that answers "what can this user actually reach"; neither this
endpoint nor the registry tree is.

### Cost

Three flat queries, one per grant level, grouped in memory. Not one query per row — role size is
not a performance concern.

### Errors

`404 SEC-404-ROLE` for an unknown role id. A role holding nothing returns `200` with
`"modules": []`.

---

## 3. Role update

```
PUT /api/v1/sec/roles/{id}
Requires: PERM_SEC_ROLES_UPDATE
```

```jsonc
{
  "nameAr": "مدير الأمان",          // required, max 150
  "nameEn": "Security administrator", // required, max 150
  "descriptionAr": "إدارة المستخدمين", // optional, max 500
  "descriptionEn": "Manages users"     // optional, max 500
}
```

Returns the updated `RoleResponse` — the same shape `POST /roles` returns.

### What is deliberately **not** in the body

- **`code`** — the immutable natural key. It is not editable and will not become editable. Keep it
  read-only in the edit form; there is no rename path, by design.
- **`isActiveFl`** — SEC declares no role activate/deactivate API. Unchanged by this work.

### The description pair is both-or-neither

Same rule the create form already enforces: send both `descriptionAr` and `descriptionEn`, or
neither. Sending one alone is `400 VALIDATION_ERROR` with
`fieldErrors: [{ "field": "descriptionBilingual", "message": "This field is required" }]`.

Note the field name in `fieldErrors` is **`descriptionBilingual`**, not `descriptionAr` /
`descriptionEn` — it is a cross-field check, so map it to the pair rather than to one input.

### Errors

| Code | HTTP | When |
|---|---|---|
| `SEC-404-ROLE` | 404 | unknown role id |
| `VALIDATION_ERROR` | 400 | blank `nameAr`/`nameEn`, over-length, or a half-filled description pair |

---

## 4. Audit-log CSV export — charset and BOM

```
GET /api/v1/sec/audit-log/export
```

No request change. Two things changed on the response:

- `Content-Type` is now **`text/csv;charset=UTF-8`** (was `text/csv`, no charset)
- the body is now prefixed with the **UTF-8 BOM, `EF BB BF`**

The bytes were always valid UTF-8 — the problem was that nothing *declared* it, so Excel opened
the file as ANSI and mojibaked the whole Arabic `detailsAr` column. Both the header and the BOM
are needed: Excel reads the BOM, not the HTTP header.

**If you do anything to the body client-side, preserve the BOM.** Handing the blob straight to a
download is correct. If you parse it for an in-browser preview, strip a leading `﻿` before
splitting on the header row, or your first column name will carry an invisible character.

What already worked and still does: the export covers the **whole active filter**, not the current
page (28 rows for a filter whose page showed 7).

---

## 5. What to delete

Both of these were honest disclosures of a real backend gap. The gap is closed, so they are now
wrong:

- The **"no published endpoint"** notice rendered in place of the pending sign-ups list
  (`/security/users/pending`) — replace with the real list from §1.
- The **"an empty box does not mean not granted"** warning on the Roles grant tree — replace with
  real pre-checked state from §2. Per rule 1 in §2, an unchecked box now genuinely means not
  granted.

---

## 6. About the missing API-SEC ids

The three new endpoints carry **no `API-SEC-0xx` id**, because the SEC execution plan never
declared them — that is precisely why they were missing. The backend cannot assign an id: the plan
is the governance factory's to write, and `CODEOWNERS` refuses a change to it from this repo.

What was done instead, through the sanctioned channel: all four items are recorded as `ABSENT`
entries with `resolution: RESOLVED` in

```
governance/shared/erp/modules/SEC/backend/execution-state.json → api_doc_gaps[]
```

The factory reads these with `gov.py feedback`, and the next version's gate will not open while
one is unanswered. So the ids are coming; **do not wait for them to integrate.** The generated API
docs show these three endpoints with a blank Contract ID and the module summary reads
`27/30 … 3 DRIFT` — that is the expected, tracked state, not a defect.

The same file records that `API-SEC-024`'s Response line should state the charset explicitly.

---

## 7. One thing to watch if you generate a client

springdoc auto-numbers duplicate operation ids. Adding a fourth `search` method shifted the
existing ones:

| Endpoint | Was | Now |
|---|---|---|
| `POST /sessions/search` (`API-SEC-025`) | `search_1` | `search_2` |
| `POST /registry/search` (`API-SEC-021`) | `search_3` | `search_4` |
| `POST /audit-log/search` (`API-SEC-023`) | `search_4` | `search_5` |
| `POST /roles/search` (`API-SEC-012`) | `search_2` | `search_3` |
| `POST /signup-requests/search` (new) | — | `search_1` |

**No path, verb, request or response changed** — only the operation id. If you hand-write your
client this is irrelevant. If you **generate** one from `/v3/api-docs/sec`, your generated method
names will shuffle, and `search_4` now means a different endpoint than it did yesterday. Regenerate
rather than reconcile by hand, and treat operation ids as unstable identifiers generally — they
will shift again the next time a search endpoint is added anywhere in SEC.

---

## 8. Verified, not assumed

Full backend suite: **33 passed, 0 failed**, including a new `SecFrontendGapIntegrationTest`
(5 tests, one per gap). All four were additionally exercised against a live instance.

| Check | Result |
|---|---|
| pending list vs dashboard count | dashboard `pendingSignups: 2`; filtered list returned exactly those 2 rows |
| `statusCode` filter discriminates | no non-`PENDING` row leaked through |
| grant tree, role holding grants | full nested tree, `granted: true` + `grantedAt` at every level |
| grant tree, role holding nothing | `"modules": []` |
| grant tree, unknown role | `404 SEC-404-ROLE` |
| role rename | names + descriptions updated, `code` unmoved, `updatedBy: "admin"` |
| role update, unknown id | `404 SEC-404-ROLE` |
| role update, half-filled description | `400 VALIDATION_ERROR`, field `descriptionBilingual` |
| CSV header | `Content-Type: text/csv;charset=UTF-8` |
| CSV first bytes | `ef bb bf 61 75 64 69 74 …` |
| CSV Arabic round-trip | `محاولة دخول فاشلة` legible |

The hand-written API script's TC-SEC-026 case was strengthened while we were in there: it had been
*reporting* the content-type without asserting it, so a status-only check would have passed on a
file no admin could read. It now asserts both charset and BOM.

---

## 9. Where the docs are

Regenerated from the running app — **auto-generated, do not hand-edit**:

```
governance/shared/erp/modules/SEC/api-docs/endpoints/sign-up-requests.md   ← §1
governance/shared/erp/modules/SEC/api-docs/endpoints/role-grants.md        ← §2
governance/shared/erp/modules/SEC/api-docs/endpoints/roles.md              ← §3
governance/shared/erp/modules/SEC/api-docs/endpoints/audit-log.md          ← §4
governance/shared/erp/modules/SEC/api-docs/index.md                        ← catalog + traceability
```

This is the **shared submodule** — the frontend repo mounts the same bytes. Pull the submodule;
do not copy anything across.

> **These are not published yet.** They are committed nowhere until someone runs
> `./scripts/governance push` from the backend repo. If a submodule pull does not show the three
> new endpoints, that push has not happened — ask before assuming the docs are stale for some
> other reason.

Backend port is still **7272** (`server.port` in `application.properties`, unchanged). A second
instance was run on 7273 purely to verify without disturbing a concurrently running one — nothing
is deployed there, and no config references it.

---

## 10. Still yours, not ours

From your own "Explicitly NOT backend items" list — restated so nothing falls between the two
repos. None of these were touched:

- **Users-screen drawer URL encoding** (`/security/users/new` vs `?action=create`) — frontend,
  MANDATE F4-RULE-7. The roles screen already complies; the users screen does not.
- **Screen caption codes** (`SCR-SEC-001` on the users screen) — frontend or governance numbering.
- **TC-SEC-040's "tab" wording** — a defect in the test plan, not in the implementation. Tabs are
  prohibited and the sub-route implementation is correct. Worth raising with whoever owns the test
  plan rather than leaving it to fail on wording.
- **CLAUDE.md §3.6 `requiredModule`/`requiredPermission`** — stale documentation; the code's
  `pageCode` form is correct per `ADR-SEC-005`.
- **Dev-DB fixture pollution** (`TST*` modules, duplicate `SEC_ADMIN_*` roles) — environment
  hygiene. Note this is visible in the grant tree from §2, since it reflects real rows; a
  `TST223693` module appearing there is fixture data, not a bug in the endpoint.

---

## 11. Not in scope

Deliberately **not** done, so nobody goes looking:

- **No `GET /signup-requests/{id}`** — the ask was a list; the decision endpoint already takes the
  id directly.
- **No grant-tree write.** Grants are still written one at a time through the existing
  `API-SEC-014/016/017` and revoked through `API-SEC-015`. There is no "save the whole tree" bulk
  endpoint, and adding one would need a decision about cascade semantics that nobody has taken.
- **No role delete, no role activate/deactivate.** SEC declares neither.
- **No change to `API-SEC-027`** (the effective menu) or `API-SEC-021` (the registry tree). §2 is a
  third, separate read — it does not replace either.
- **No frontend changes of any kind**, as your handoff asked.
