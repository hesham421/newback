# Tenant-maturity plan — handover to the frontend (package F)

| | |
|---|---|
| From | backend `newback`, closure step TM-Z (branch `tm/z-closure`, base `main` e643d91) |
| To | frontend `newfront` (`mxdashboard/`), plan `docs/plans/tenant-maturity-plan.md` §8 F1–F4 |
| Contract | `docs/api-docs/<module>/` of this repository — regenerated from the merged backend; 125 operations; `check_completeness.py` PASS (no drift at TM-Z) |
| Analysis | the `## Implementation Addendum — erp-core 1.3.0` sections under `governance/analysis/modules/{SEC,TENANT,FILE}/` — each package block ends with a "Frontend impact" table (cited per item below) |
| Status | every backend package F depends on is merged: G (F4), D (F1), E (F2, F3), B + C12 + C4 + C5 (F3). Nothing is BLOCKED on the backend. |

This is a list of facts. It does not design the screens. The plan's §8 tables still describe what each screen
shows; where the backend built something different from the plan, the difference is written here and wins.

## 0. Write these first (newfront `CLAUDE.md` §11c, analysis first)

Before any F code, through a newback worktree branch, append-only:

| Module | Files | Note |
|---|---|---|
| SEC | `governance/analysis/modules/SEC/P2_5/ui-ux-spec-sec.md`, `flow-diagram-sec.md` | **SEC has no `P2_5` folder yet** — create both in the MDL files' pattern (§11c item 3), the frontend addendum as their first implemented section. F1 (users drawer, set-password drawer, photo drawer, my-profile route, forced-change route) and F4 (grant-tree uncheck) |
| TENANT | `governance/analysis/modules/TENANT/P2_5/ui-ux-spec-tenant.md`, `flow-diagram-tenant.md` | **TENANT has no `P2_5` folder yet** — create both (plan §8 says so). F2 (shell branding; the shell has no module of its own, so its rows live with the branding they read) and F3 (`PLATFORM_TENANTS` upgrades) |
| (test plan) | newfront `docs/test-e2e/front-test-plan.md` | `TC-FE-SEC-…`, `TC-FE-XCUT-…`, `TC-FE-PLATFORM-…` cases per plan §8 |

Decision rows the plan already names (write them as "Decision" rows): authentication-only routes for `/account/*`
(no page code: the profile and the forced change are for every signed-in staff user); tenant logo **beside** the
platform mark; logo upload in the tenant detail drawer (second-level drawer).

No new page code, permission, menu entry or screen exists for any of F1–F4: the backend added none (D5 for branding;
`PLATFORM_TENANTS` and `PLATFORM_TENANT_MANAGE` cover every tenant action; `SEC_USERS` / `SEC_ROLES` cover F1 / F4).

## F1 — Users and my profile (module `security`; backend package D)

Contract: `docs/api-docs/sec/endpoints/users.md`, `my-profile.md`, `authentication.md`. Analysis: SEC `P1/srs-sec.md`
1.3.0 §9 (endpoints §9.5, notes §9.9, frontend impact §9.10); ADR-SEC-063, ADR-SEC-064.

| Method | Path | Permission | Request → response | Codes |
|---|---|---|---|---|
| PUT | `/api/v1/sec/users/{id}/password` | `PERM_SEC_USERS_UPDATE` | `{ newPassword, requireChangeAtNextLogin? }` (null = **true**) → `PasswordChangeResponse { userPk, passwordChangeRequired, passwordChangedAt, sessionsTerminated }` (not `UserStatusResponse` as the plan said) | 404 `SEC-404-USER` (unknown or a customer), 422 `SEC-422-PASSWORD-SELF` (own account: use `/me/password`), 400 `SEC-400-PASSWORD-POLICY` |
| GET | `/api/v1/sec/me` | signed-in STAFF (customer token → 403 `REALM_MISMATCH`) | → `StaffProfileResponse { userPk, username, email, fullNameAr, fullNameEn, phone, jobTitleAr, jobTitleEn, preferredLocale, photoUrl, passwordChangeRequired, lastLoginAt, tenant { code, nameAr, nameEn } }` — **no roles, no permissions** (the menu stays the only authority) | 401 |
| PATCH | `/api/v1/sec/me` | same | `{ fullNameAr, fullNameEn, phone, jobTitleAr, jobTitleEn, preferredLocale }` — null = unchanged, `""` clears phone / job titles / locale; names cannot be blank; e-mail and username are admin-only | 400 `VALIDATION_ERROR` |
| PUT | `/api/v1/sec/me/password` | same | `{ currentPassword, newPassword }` → `PasswordChangeResponse` | 403 `SEC-403-PASSWORD-CURRENT-INVALID`, 400 `SEC-400-PASSWORD-POLICY` |
| PUT / DELETE | `/api/v1/sec/me/photo` | same | multipart part `file` → `{ photoUrl }`; DELETE → 204 (also with no photo) | 400 `SEC-400-PHOTO-INVALID`, 400 `VALIDATION_ERROR` (missing part / not multipart / over the multipart ceiling) |
| PUT / DELETE | `/api/v1/sec/users/{id}/photo` | `PERM_SEC_USERS_UPDATE` | same as `/me/photo` for another staff user | + 404 `SEC-404-USER` |
| CHANGED | `POST /api/v1/sec/users`, `PUT /api/v1/sec/users/{id}` | as before | + `phone`, `jobTitleAr`, `jobTitleEn`, `preferredLocale` (`ar` / `en`); create + `requireChangeAtNextLogin` (null = **true**); PUT: absent = unchanged, `""` = cleared | + 400 `SEC-400-PASSWORD-POLICY` on create |
| CHANGED | every `UserResponse` | — | + `phone`, `jobTitleAr`, `jobTitleEn`, `preferredLocale`, `photoUrl` (nullable), `passwordChangeRequired`, `passwordChangedAt` | — |
| CHANGED | `POST /api/v1/sec/auth/login` | public | `LoginResponse` + `passwordChangeRequired` (always `false` for customers) | — |

Behaviour notes:
- **Forced password change.** Users created by an administrator, users whose password an administrator set, and tenant
  administrators reset by the platform operator (F3) have `passwordChangeRequired = true`. While it is set, every STAFF
  call answers 403 `SEC-403-PASSWORD-CHANGE-REQUIRED` **except** `GET /api/v1/sec/me`, `PUT /api/v1/sec/me/password`,
  `POST /api/v1/sec/auth/logout` and `GET /api/v1/tenant/me` (so the shell can still load the branding and the user's
  name). Route to `/account/change-password` on the login flag, and also on any 403 with that code. After a successful
  `PUT /me/password` the **same token keeps working** (the user's other sessions end); clear the flag in the `['me']`
  cache.
- **Password policy** (default; the server is the authority, always show `SEC-400-PASSWORD-POLICY`'s message): at least 8
  characters, a letter and a digit, at most **72 UTF-8 bytes** (an Arabic letter takes 2 — 36 Arabic letters max).
  A client-side hint is fine; do not hard-code a 200-character limit (the DTOs accept 200 only so that the policy code
  answers).
- **Set password**: the admin cannot target themselves (422) — hide the action on the signed-in user's own row and send
  them to the change-password page. Show `sessionsTerminated` in the success toast.
- **Photos**: PNG, JPEG or WebP, at most 1 MB, type detected from the bytes (no SVG for photos). `photoUrl` is a public
  URL (no token) — render with `<img>`. A new upload gets a new URL; an old URL may still be served from a browser / CDN
  cache for up to 24 h after replace / remove, so always take `photoUrl` from the latest `me` / user response.
- `preferredLocale` is `ar` / `en` or null; apply it after login (plan F1).
- Not built (later versions): password history / reuse rule; throttling of the current-password check.

## F2 — Dynamic shell: tenant logo beside the platform mark (backend package E)

Contract: `docs/api-docs/tenant/endpoints/tenant-branding.md`. Analysis: TENANT `P1/srs-tenant.md` 1.3.0 package-E
block (E7 SVG serving, E11 frontend impact); ADR-TENANT-005; `docs/CONSUMING.md` "Tenant branding".

| Method | Path | Access | Response | Codes |
|---|---|---|---|---|
| GET | `/api/v1/tenant/me` | any signed-in user, **staff or customer**, GET only (realm-neutral) | `TenantBrandingResponse { code, nameAr, nameEn, logoUrl, brandColor, defaultLocale }` — the token's tenant; read-only, no contact or profile fields | 401; 401 `TENANT_TOKEN_REVOKED`; 403 `TENANT_SUSPENDED` |
| GET | `/api/v1/public/tenants/{tenantCode}/branding` | anonymous (no token, no header); tenant from the path, trimmed and upper-cased | same `TenantBrandingResponse` | 404 `TENANT_NOT_FOUND`, 403 `TENANT_SUSPENDED`, **429 `TENANT_BRANDING_RATE_LIMITED` with `Retry-After` (seconds)** |

Behaviour notes:
- Call `/tenant/me` once at shell mount (it works during a pending forced password change). Key the cache by tenant
  code; clear it on sign-out and on tenant-code change.
- Login / sign-up / reset pages call the public branding after the tenant code settles. **404 and 429 → show the
  platform mark only, no error toast**; on 429 do not ask again before `Retry-After` (the budget is 60 requests per
  minute per client address by default; unknown codes count too — debounce the tenant-code field).
- **Logos through `<img>` only.** An SVG logo is served as an attachment with `X-Content-Type-Options: nosniff` and a
  sandbox CSP: it renders inside `<img>`, but opening the URL directly downloads it. Never inline it, never fetch and
  inject it.
- **Caching**: public files carry `Cache-Control: max-age=86400, public`. Every upload gets a new random URL, so a new
  logo is never hidden behind a cached one, but a removed or replaced logo's **old** URL may stay servable from a cache
  for up to 24 h — always use the `logoUrl` of the latest branding answer, never a remembered URL. `onError` → hide the
  logo, fall back to the mark.
- `brandColor` is `#RRGGBB` (upper case) or null; use it only for the accent the plan names.
- While a tenant is suspended its public branding and its logo URL answer 403 (show the mark only).

## F3 — Platform tenants screen (module `platform`, screen `PLATFORM_TENANTS`; packages B, E, C12, C4, C5)

Contract: `docs/api-docs/tenant/endpoints/platform-tenants.md`; the export download is in
`docs/api-docs/file/endpoints/file-documents.md`. Every endpoint below needs `PLATFORM_TENANT_MANAGE` and a PLATFORM
caller (a tenant administrator gets 403). Analysis: TENANT `P1/srs-tenant.md` 1.3.0 blocks B (B11), E (E11),
C12 (C11), C4 (I11), C5 (X12, X14); ADR-TENANT-002, -003, -005, -006.

| Method | Path | Request → response | Codes |
|---|---|---|---|
| CHANGED | `GET /{id}`, `GET /`, `POST /search` | `TenantResponse` + profile (`contactEmail`, `contactPhone`, `countryCode`, `defaultLocale`, `timezone`, `notes`), suspension facts (`suspendedAt`, `suspendedBy`, `suspensionReason`), `logoUrl`, `brandColor`; search / sort also accept `contactEmail`, `countryCode`, `suspendedAt` | — |
| PUT | `/{id}` | `{ nameAr, nameEn, contactEmail, contactPhone, countryCode, defaultLocale, timezone, notes }` — **no `code`, no `statusCode`** (code read-only); empty → null; country upper-cased | 404 `TENANT_NOT_FOUND`, 400 `VALIDATION_ERROR`, 409 `CONCURRENT_MODIFICATION` |
| CHANGED | `PATCH /{id}/status` | `{ statusCode, reason }` — **`reason` required for `SUSPENDED` (3..500 characters)**, ignored for `ACTIVE` | 400 `TENANT_SUSPENSION_REASON_REQUIRED`, 422 `TENANT_PLATFORM_PROTECTED` (PLATFORM first) |
| POST | `/{id}/admin-reset` | `{ username, newPassword, requireChangeAtNextLogin? }` (null = **true**) → `{ username, sessionsTerminated }` | 404 `TENANT_ADMIN_NOT_FOUND`, 422 `TENANT_ADMIN_NOT_SUPER`, **422 `TENANT_ADMIN_RESET_PLATFORM` (never offer it on the PLATFORM row)**, 400 `SEC-400-PASSWORD-POLICY` |
| GET | `/{id}/usage` | → `{ id, staffUsers, customerUsers, activeSessions, fileDocuments, fileBytes, notificationsLast30Days, collectedAt }` | 404 |
| PUT | `/{id}/logo` | multipart part `file` (PNG / JPEG / WebP / plain SVG, ≤ 1 MB) → `TenantResponse` | 400 `TENANT_LOGO_INVALID` (field `file`), 400 `VALIDATION_ERROR` (part missing), 404 |
| DELETE | `/{id}/logo` | → 204 (idempotent) | 404 |
| PATCH | `/{id}/branding` | `{ brandColor }` (`#RRGGBB`; null or blank clears) → `TenantResponse` | 400 `TENANT_BRAND_COLOR_INVALID` (field `brandColor`) |
| POST | `/{id}/revoke-tokens` | no body → `{ id, code, sessionsTerminated }` | **422 `TENANT_REVOKE_TOKENS_PLATFORM` (not on PLATFORM)**, 500 `TENANT_REVOKE_SESSIONS_FAILED` (tokens are already refused; show the message and offer to repeat) |
| CHANGED | `POST /` (create) | optional header **`Idempotency-Key`** (`^[A-Za-z0-9._:-]{1,64}$`) — generate a UUID when the form opens, reuse it for every retry of that submission; a replay answers the first 201 with response header `Idempotent-Replayed: true` | 400 `IDEMPOTENCY_KEY_INVALID`, 409 `IDEMPOTENCY_KEY_CONFLICT` (same key, other body or other user: a changed form needs a new key); the first administrator's `adminPassword` must meet the password policy (400 `SEC-400-PASSWORD-POLICY`) |
| POST | `/{id}/export` | no body → `{ tenantId, tenantCode, fileId, fileName, sizeBytes, rowCount, downloadToken, downloadTokenExpiresAt }` | 404, 409 `TENANT_EXPORT_IN_PROGRESS`, 422 `TENANT_EXPORT_TOO_LARGE`, 429 `TENANT_EXPORT_BUSY` (no `Retry-After`: say "try again shortly") |
| — | `GET /api/v1/files/download?token={downloadToken}` | the archive (ZIP) | single use, 10 minutes, **bound to the operator who exported**; another user → 401 `FILE_ACCESS_TOKEN_INVALID` |

Behaviour notes:
- **Suspend needs a reason**: the suspend confirm dialog gets a mandatory text field (3..500). The detail drawer shows
  `suspendedAt` / `suspendedBy` / `suspensionReason` (null while ACTIVE). Suspending ends every session of the tenant
  at once; re-activating makes **every earlier token of that tenant answer 401 `TENANT_TOKEN_REVOKED`** — its users sign
  in again.
- **Old tokens after re-activation / revoke**: any request with a revoked token answers 401 `TENANT_TOKEN_REVOKED` (both
  realms). The shell treats it like any 401 (clear the session, go to login), optionally saying that the organisation's
  sessions were ended. A login request sent with the stale token in `Authorization` still works.
- **Admin-reset** forces a change at the target's next sign-in by default (F1's flow); confirm with `<ConfirmDialog>`
  before submit; show `sessionsTerminated`.
- **Usage** is counted live inside the tenant on each call (load it when the section opens).
- **Logo**: the drawer's preview uses `logoUrl` from the returned `TenantResponse`; SVG must be plain / optimised
  (SVGO, Inkscape "Plain/Optimized SVG" without metadata, Figma, Illustrator export): files with `<metadata>`,
  `inkscape:` / `sodipodi:` attributes, a DOCTYPE, scripts, external references or duplicate `id`s are refused with
  `TENANT_LOGO_INVALID`, whose message says so in both languages. PNG / WebP are the safer recommendation. Removal is a
  sensitive action (`<ConfirmDialog>`). The PLATFORM row may carry a logo too; the platform **mark** is a static asset,
  never a tenant logo.
- **Export**: synchronous — a large tenant keeps the request open for tens of seconds; show progress and do not retry
  automatically. Download at once with the returned token. Later re-download: the operator finds the archive in FILE
  (`GET /api/v1/files`, owner `CORE_TENANT` / tenant id, module `TENANT`), gets a new token with
  `POST /api/v1/files/{id}/access-token` and downloads it; **the archive is restricted**: a user without
  `PLATFORM_TENANT_MANAGE` never sees it (404 `FILE_DOCUMENT_NOT_FOUND`), whatever FILE permission they hold. Deleting
  it (`DELETE /api/v1/files/{id}`) removes its bytes and leaves a `DELETED` tombstone. The ZIP holds UTF-8 CSV files with
  a byte-order mark (Excel shows Arabic correctly) and `manifest.json`. Archives are kept until deleted (no retention
  job in 1.3.0).
- **Idempotency-Key is optional**; without it create behaves as in 1.2.0. Core configures no CORS: if the frontend is
  served from another origin, the application must allow the request header `Idempotency-Key` and expose
  `Idempotent-Replayed`.

## F4 — Grant tree: uncheck a screen or an action (module `security`, screen `SEC_ROLES`; backend package G)

Contract: `docs/api-docs/sec/endpoints/role-grants.md`. Analysis: SEC `P1/srs-sec.md` 1.3.0 §1–§8 (frontend impact §8);
ADR-SEC-062.

| Method | Path | Permission | Response | Codes |
|---|---|---|---|---|
| DELETE | `/api/v1/sec/roles/{id}/screens/{screenId}` | `PERM_SEC_ROLES_UPDATE` | **200** `ScreenGrantRevokeResponse { revokedActionGrants }` (not 204: the count confirms the cascade) | 404 `SEC-404-ROLE` (checked first), 404 `SEC-404-GRANT` |
| DELETE | `/api/v1/sec/roles/{id}/actions/{actionId}` | `PERM_SEC_ROLES_UPDATE` | **200** `ActionGrantRevokeResponse { revokedActionGrants }` — the count **includes** the requested grant | same |

Behaviour notes:
- Revoking a screen removes the role's action grants on that screen (name the cascade in the `<ConfirmDialog>` from the
  tree, `GET /roles/{id}/grants`).
- Revoking an action that is the screen's `VIEW` also removes the role's other actions on that screen — warn before.
  **The screen grant stays, so the screen stays in the role's menu** while its endpoints answer 403: to remove the menu
  entry, revoke the screen itself.
- A role with `isSuper = true` keeps every authority regardless of grants; grants only shape its menu — show the hint.
- Sessions are not ended: the next request of an affected user already sees the change. The module revoke is unchanged.

## Codes the frontend must translate (both languages already exist in the backend bundles)

SEC: `SEC-400-PASSWORD-POLICY`, `SEC-422-PASSWORD-SELF`, `SEC-403-PASSWORD-CURRENT-INVALID`,
`SEC-403-PASSWORD-CHANGE-REQUIRED`, `SEC-400-PHOTO-INVALID`. TENANT: `TENANT_SUSPENSION_REASON_REQUIRED`,
`TENANT_ADMIN_NOT_FOUND`, `TENANT_ADMIN_NOT_SUPER`, `TENANT_ADMIN_RESET_PLATFORM`, `TENANT_LOGO_INVALID`,
`TENANT_BRAND_COLOR_INVALID`, `TENANT_BRANDING_RATE_LIMITED`, `TENANT_TOKEN_REVOKED`, `TENANT_REVOKE_TOKENS_PLATFORM`,
`TENANT_REVOKE_SESSIONS_FAILED`, `TENANT_EXPORT_TOO_LARGE`, `TENANT_EXPORT_IN_PROGRESS`, `TENANT_EXPORT_BUSY`.
Common: `IDEMPOTENCY_KEY_INVALID`, `IDEMPOTENCY_KEY_CONFLICT`. FILE (now also for restricted archives):
`FILE_DOCUMENT_NOT_FOUND`. The server's message is localised by `Accept-Language`; the error envelope is unchanged.

## Contract caveats (generator limitations, not gaps)

- **Ignore the error code `of`.** Eleven Business Responses rows show code and constant `of` (e.g. `users.md` under
  `PasswordPolicy.assertAcceptable`, `platform-tenants.md` under `TenantDomain.assertBrandColorValid` /
  `assertLogoAccepted`, `my-profile.md`, `authentication.md`, `customer-accounts-public.md`, `report/endpoints/reports.md`):
  the generator misread `FieldError.of(...)` as a code. The real code is the one in the endpoint's description or the
  other row of the same method (`SEC-400-PASSWORD-POLICY`, `SEC-400-PHOTO-INVALID`, `TENANT_BRAND_COLOR_INVALID`,
  `TENANT_LOGO_INVALID`, `VALIDATION_ERROR`), with the field in `fieldErrors` (`docs/api-docs/README.md` known limitations).

- Response headers are not rendered by the api-doc generator: `Idempotent-Replayed` (tenant create) and `Retry-After`
  (public branding 429) appear only in the endpoints' descriptions and here (`docs/api-docs/README.md` known limitations).
- Filter-raised codes (`SEC-403-PASSWORD-CHANGE-REQUIRED`, `TENANT_TOKEN_REVOKED`, `TENANT_BRANDING_RATE_LIMITED`) are
  listed in the module's error catalogue (`docs/api-docs/sec/index.md`, `docs/api-docs/tenant/index.md`) rather than in
  every endpoint's table; they can answer any endpoint the filter covers, as described above.
- `SEC-400-PASSWORD-POLICY` on tenant admin-reset and on tenant create comes through a cross-module call the generator
  does not walk; it is named in those endpoints' descriptions.
