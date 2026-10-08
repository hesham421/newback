# UI/UX SPEC — الأمان / Security (SEC)
══════════════════════════════════════════════════════════════════
Module : SEC   Version : v1   Profile : erp   Stage : P2.5 (UX design — frontend)
Sources: srs-sec.md (Part B SCR-REQ-SEC-001…010, the 1.2.0 addendum, the 1.3.0 addendum §1–§9) · prd-sec.md ·
         the newfront `mxdashboard` 1.2.0 code as built (`src/core/auth/**`, `src/app/layout/**`,
         `src/modules/security/features/{users,roles}/**`) · ADR-SEC-007 (entry patterns), ADR-SEC-011 (menu groups)
Mints  : none in the baseline (SCR-SEC-001 … SCR-SEC-010 were minted by the SEC frontend analysis — ADR-SEC-007,
         ADR-SEC-011 — and are reused here, never renumbered) · SCR-SEC-011, SCR-SEC-012 in the frontend 1.3.0 addendum
Scope  : the SEC screens the tenant-maturity plan §8 F1 / F4 touches — SCR-SEC-001…005 and SCR-SEC-010 — as built
         in mxdashboard 1.2.0; SCR-SEC-006 … SCR-SEC-009 are untouched and not described here
══════════════════════════════════════════════════════════════════

This file was created by the frontend step TM-FA (newfront `CLAUDE.md` §11c item 3: SEC had no `P2_5` folder). The
baseline below describes the screens as the frontend built them for erp-core 1.2.0; it adds no field, rule or
permission the SRS does not have. Everything new is in the addendum at the end, which is the first implemented
section of this file.

## SCR-SEC-001 — تسجيل الدخول / Login (with SCR-SEC-002 Sign-up and SCR-SEC-003 Forgot / reset password)
Traces            : REQ-SEC-001, REQ-SEC-002, REQ-SEC-003, REQ-SEC-006, REQ-SEC-007, REQ-SEC-008, US-SEC-001, US-SEC-002, US-SEC-003
Screen requirement: SCR-REQ-SEC-001 (SEC_LOGIN) · SCR-REQ-SEC-002 (SEC_SIGNUP) · SCR-REQ-SEC-003 (SEC_PWD_RESET) — public
Routes            : `/login` · `/sign-up` · `/password-reset`, `/password-reset/complete`, `/reset` — outside the authenticated layout, no `<ProtectedRoute>`
UI pattern        : `AuthScreenShell` — split layout: a brand aside (the static platform mark `/brand/avelynq-mark-dark.png`, never a tenant logo) and the form column; a language toggle
Fields shown      : Tenant code (`TenantCodeInput`, upper-cased, pattern `^[A-Z0-9_]{3,32}$`; read-only with the `auth.tenantFromHost` hint when `VITE_TENANT_HOST_SUFFIX` names the tenant) · username · password (login) · e-mail, names (sign-up) · e-mail / token, new password, confirm (reset)
Behaviour         : the login request carries `X-Tenant-Code` and never `Authorization`; tenant refusals (`TENANT_NOT_FOUND`, `TENANT_SUSPENDED`, `TENANT_REQUIRED`) are inline on the Tenant field, 401 is the "Invalid credentials" alert, 409 `CONCURRENT_MODIFICATION` an inline alert; a STAFF token with a numeric `tid` establishes the session and the tenant code is persisted (`avelynq_tenant_code`); then the landing route (`/dashboard`)
States            : submitting (button busy) · refusal inline / alert · no toast on any refusal
data-testid       : `login-form`, `login-tenant`, `login-username`, `login-password`, `login-submit`, `signup-tenant`, `reset-request-tenant`, `reset-complete-tenant`

## SCR-SEC-004 — المستخدمون / Users
Traces            : REQ-SEC-009, REQ-SEC-010, REQ-SEC-011, REQ-SEC-031, REQ-SEC-004, REQ-SEC-005, US-SEC-004
Screen requirement: SCR-REQ-SEC-004 · page code `SEC_USERS`
Routes            : list `/security/users` · pending sign-ups `/security/users/pending` · entry drawer as ROUTE SEGMENTS (owner decision recorded in `usersRoutePaths.ts`): create `/security/users/new`, view `/security/users/:id`, edit `/security/users/:id/edit` · second level on the drawer: role picker `?picker=roles`
UI pattern        : search list + side drawer (ADR-SEC-007 rule 3, `SIDE_DRAWER`); the list stays mounted behind the drawer
Fields shown      : list — user (name in the active language), username, e-mail, roles, realm badge (`user-realm-badge`), status, last login, row actions (view, edit, activate / deactivate); server sort on the whitelist, rows-per-page, `TableExportMenu` · drawer — CREATE: username, e-mail, full name (ar / en), password, roles summary (`assigned-roles-summary` → picker); EDIT: username read-only, e-mail, names, account status read-only, roles; VIEW: read-only rows incl. last login
Permissions       : SRS §B4 — `SEC_USERS`: VIEW (gateway), CREATE, UPDATE (incl. activate / deactivate / approve / reject)
Actions           : create, save, assign roles (`PUT /users/{id}/roles` after the save), deactivate (confirm drawer) / reactivate; success toasts `users.toastCreateSuccess` / `users.toastUpdateSuccess`
States            : empty filtered / unfiltered (two messages) · loading · 404 `SEC-404-USER` on a deep link → drawer closes, one `errors.itemUnavailable` toast

## SCR-SEC-005 — الأدوار والصلاحيات / Roles & permissions
Traces            : REQ-SEC-012, REQ-SEC-013, REQ-SEC-014, REQ-SEC-015, REQ-SEC-020, REQ-SEC-030, US-SEC-005
Screen requirement: SCR-REQ-SEC-005 · page code `SEC_ROLES`
Routes            : `/security/roles` · the selected role `?editId=<rolePk>` · create `?action=create` · role identity drawer `?identity=`
UI pattern        : tree master-detail (ADR-SEC-007 rule 1): role rail (`RoleMasterRail`, "Super role" badge for `isSuper = true`) + the 3-level grant tree (`RoleGrantPanel` → `RoleGrantTree` → `RoleGrantScreenNode`)
Grant tree (1.2.0): grants are STAGED in a draft (`useRoleGrantDraftStore`) and saved in order by one "Save" (module → screen → action, RULE-SEC-001/002/007); a saved screen / action is shown checked and locked with the hint `roles.individualRevokeUnavailable` — the only revoke is the module's (`ModuleRevokeConfirm`, a ConfirmDialog whose target lives in Zustand `useModuleRevokeConfirmStore`, answer `revokedScreenGrants` / `revokedActionGrants`)
Permissions       : SRS §B4 — `SEC_ROLES`: VIEW, CREATE, UPDATE (grant-tree edits), DELETE (deactivate)
States            : tree loading / unreadable / registry drift notice · save progress "step n of N" · stopped-at notice

## SCR-SEC-010 — القائمة الديناميكية / Dynamic two-tier menu (the authenticated shell)
Traces            : REQ-SEC-021, REQ-SEC-032, REQ-SEC-033
Screen requirement: SCR-REQ-SEC-010 · no page code (not a destination)
Shell             : `AppShell` = `Sidebar` (brand header: the static platform mark 28 px + the "AVELYNQ" word-mark, hidden in the collapsed rail; collapse toggle; `AppShellNav` from `GET /api/v1/sec/menu`, the active entry marked by `NavLink`, accent `var(--teal-400)`) + `Topbar` (title / breadcrumb, search box, tenant badge `topbar-tenant` = the session tenant code, language toggle `topbar-language`, inbox bell, sign-out `topbar-sign-out`; no identity block — 1.2.0 had no endpoint for the caller's own name)
Guard             : one `<ProtectedRoute>` without a page code wraps the shell (authentication only); every screen inside carries its own `requiredPageCode`
Session end       : 401 on an authenticated call → session cleared, `/login`; 403 `TENANT_SUSPENDED` / `REALM_MISMATCH` → session cleared + one danger toast (`endRevokedSession`)

---

## Implementation Addendum — frontend 1.3.0
Source version : mxdashboard 1.3.0 (unreleased) against erp-core 1.3.0 (newback `f48b9ab`, `docs/api-docs/sec/`)
Change         : tenant-maturity plan §8 F1 (users: profile fields, photo, set password; forced password change; my profile; topbar account menu) and F4 (grant tree: uncheck a screen or an action); backend packages D and G (`../P1/srs-sec.md` 1.3.0 §1–§9; `docs/steps/tm-frontend-handover.md` F1, F4)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Rows are numbered `SEC-Unn` so the step reports and `docs/test-e2e/front-test-plan.md` (newfront) can cite them.
No new page code, permission, menu entry or error code exists for F1 / F4 (handover §0): `SEC_USERS` and `SEC_ROLES`
cover every new action; `/account/*` is authentication-only (SEC-U02, Decision SEC-U70).

### A. Screens and routes
| # | Kind | Screen | Route | Page code / guard | Detail |
|---|---|---|---|---|---|
| SEC-U01 | NEW | **SCR-SEC-011 — ملفي الشخصي / My profile** | `/account/profile` | none — authentication only: a child of the shell's `<ProtectedRoute>` (no `requiredPageCode`) | read view of the caller's own `StaffProfileResponse` + three drawers (SEC-U11 … U13); reached from the topbar account menu (SEC-U05), never from the menu; shell title `account.profileTitle` |
| SEC-U02 | NEW | **SCR-SEC-012 — تغيير كلمة المرور الإلزامي / Forced password change** | `/account/change-password` | none — authentication only, registered OUTSIDE `AppShell` under the NEW `<AuthenticatedRoute>` (SEC-U15), which never reads the menu | `AuthScreenShell` layout (`GET /sec/menu` answers 403 while the change is pending); tenant logo from `GET /api/v1/tenant/me` (TENANT P2_5 TEN-U04, rendered by `<TenantLogo>` TEN-U01); sign-out link; on success the toast `account.toastPasswordChanged` (SEC-U23); when no change is pending the route redirects to `/account/profile?changePassword=1` |
| SEC-U03 | CHANGED | SCR-SEC-010 shell — forced-change gate | every route under `AppShell` | — | `ForcedPasswordChangeGate` wraps the shell's layout guard OUTSIDE `<ProtectedRoute>` and reads only the auth store, never the menu: while `passwordChangeRequired` is true (SEC-U20) every shell route renders `<Navigate to="/account/change-password" replace>`; public routes (`/login`, `/sign-up`, `/password-reset*`) are not gated |
| SEC-U04 | CHANGED | SCR-SEC-001 Login | `/login` | public | after a successful login with `passwordChangeRequired = true` the destination is `/account/change-password`, not the landing route (REQ-SEC-084, AC-SEC-090) |
| SEC-U05 | NEW | SCR-SEC-010 shell — topbar account menu | Topbar, before the sign-out button | — | button `topbar-account`: `Avatar` with `photoUrl` (`<img>`) or the initials of the caller's name in the active language, from the `['me']` query (SEC-U21); menu (`role="menu"`, keyboard as `useMenuKeyboard`): header = full name + username, items "My profile" (`topbar-account-profile` → `/account/profile`), "Change password" (`topbar-account-password` → `/account/profile?changePassword=1`). The existing `topbar-sign-out` button stays where it is (Decision SEC-U73). While `['me']` loads or fails: initials placeholder glyph, menu still offers both items |
| SEC-U06 | CHANGED | SCR-SEC-004 Users — list | `/security/users` | `SEC_USERS` VIEW | the user cell starts with a 28 px `Avatar` (`photoUrl` through `<img>`, else initials; `user-row-avatar`); two optional columns **hidden by default**: phone, job title (active language) — shown through a "Columns" menu (`users-columns-menu`, checkboxes `users-column-phone`, `users-column-jobTitle`); the choice lives in Zustand (`useUserSearchStore.visibleOptionalColumns`, session only, not in the URL); the export menu exports the visible columns |
| SEC-U07 | CHANGED | SCR-SEC-004 Users — entry drawer | `/security/users/new`, `/:id`, `/:id/edit` | CREATE / UPDATE | fields of SEC-U30 … U36; VIEW gains the read-only rows of SEC-U37 and the photo summary row (SEC-U08) and the "Set password" action (SEC-U09) |
| SEC-U08 | NEW | SCR-SEC-004 — photo, second level | `/security/users/:id?photoFor=<id>` | UPDATE | opened from the drawer's photo summary row (`user-photo-row` → `user-photo-change`); a sibling drawer above the user drawer (one drawer, one job): current photo or initials, file input, preview, constraints text, "Save photo", "Remove photo" (ConfirmDialog); closing it removes `photoFor` only. Offered on the VIEW route only: a `photoFor` that differs from the route's `:id`, or on the create (`/new`) or edit (`/:id/edit`) route, is dropped from the URL. A saving level-2 drawer — Decision SEC-U78 |
| SEC-U09 | NEW | SCR-SEC-004 — set password, second level | `/security/users/:id?passwordFor=<id>` | UPDATE | opened from the VIEW drawer's action `user-set-password`; a Drawer (it is a form); its Submit opens a final ConfirmDialog `user-set-password-confirm` (`users.confirmSetPasswordTitle`, naming the user and the end of every session — `erp-action-confirmation` §2 item 4: an administrator-initiated credential reset), and only the confirm sends the call; **hidden on the signed-in user's own row** (`['me'].userPk = :id`), where the drawer shows instead the hint `users.setPasswordOwnHint` with the link `users.setPasswordOwnLink` → `/account/profile?changePassword=1` (handover F1 "Set password", RULE-SEC-057); the same drop rule as `photoFor` (VIEW route only, matching id); a saving level-2 drawer — Decision SEC-U78 |
| SEC-U10 | CHANGED | SCR-SEC-005 Roles — grant tree | `/security/roles?editId=<rolePk>` | `SEC_ROLES` UPDATE | a SAVED screen checkbox and a SAVED action chip become un-checkable (SEC-U50 … U53); a STAGED item is still un-staged locally as before; the super-role hint (SEC-U54) |

### B. Drawers and URL state (SCR-SEC-011)
| # | Kind | Drawer | URL | Detail |
|---|---|---|---|---|
| SEC-U11 | NEW | Edit my profile | `/account/profile?editId=me` (`useDrawerUrlState()` with its default `editId` param: `openEdit('me')`; the only accepted value is `me`, any other is dropped) | fields SEC-U38; one submit `PATCH /api/v1/sec/me` |
| SEC-U12 | NEW | My photo | `/account/profile?photo=1` (a plain search param, like the users drawer's `?picker=roles`) | same body as SEC-U08 against `/api/v1/sec/me/photo` |
| SEC-U13 | NEW | Change my password | `/account/profile?changePassword=1` (a plain search param) | current password, new password, confirm; `PUT /api/v1/sec/me/password` |
| SEC-U14 | NEW | rule | all three | one drawer open at a time (opening one removes the other two params); Back closes the drawer; reload reopens it (URL is the whole state); the profile page itself has no in-memory open flag |
| SEC-U15 | NEW | `<AuthenticatedRoute>` (`src/core/rbac/AuthenticatedRoute.tsx`) | `/account/change-password` | the authentication-only guard: `isAuthenticated` false → `<Navigate to="/login">`, else the children; it NEVER calls `useMenuFacade` / `GET /sec/menu` (the existing `<ProtectedRoute>` reads the menu even without a page code, which would fire and cache a 403 `SEC-403-PASSWORD-CHANGE-REQUIRED` during the change). The shell keeps its `<ProtectedRoute>`, wrapped by the forced-change gate (SEC-U03), so once the flag is known the shell and its menu read are not rendered at all |

### C. Session state, cache and redirects
| # | Kind | Item | Detail |
|---|---|---|---|
| SEC-U20 | NEW | `passwordChangeRequired` in the auth store | `useAuthStore.passwordChangeRequired: boolean` — set from `LoginResponse.passwordChangeRequired`, from every `['me']` answer, and by the transport on 403 `SEC-403-PASSWORD-CHANGE-REQUIRED` (SEC-U22); cleared by a successful `PUT /me/password` and by sign-out; never persisted (a reload re-learns it from `['me']` or from the 403) |
| SEC-U21 | NEW | query `['me']` | `GET /api/v1/sec/me`, fetched once at shell mount and by SCR-SEC-011 / SCR-SEC-012; `queryClient.clear()` on sign-out drops it; after `PATCH /me`, a photo change of the caller, or `PUT /me/password` the cache is updated from the response (the flag cleared in `['me']`, handover F1) |
| SEC-U22 | NEW | transport: 403 `SEC-403-PASSWORD-CHANGE-REQUIRED` | `httpClient` response interceptor, staff calls only (not `skipAuth`, not `authRealm: 'CUSTOMER'`): set the flag and navigate to `/account/change-password` through a registered handler (`setPasswordChangeRequiredHandler`, like `setSessionRevokedHandler`); **no toast**, the session is kept (the token stays valid, REQ-SEC-084). Live trigger: a reload or a second tab of a session with a pending change (the flag is not persisted, so the shell's first `GET /sec/menu` answers this 403) |
| SEC-U23 | NEW | same token after the change | after `PUT /me/password` answers 200 the same token keeps working (AC-SEC-090): no re-login; the flag is cleared (store + `['me']`); the menu query is RESET (`queryClient.resetQueries({ queryKey: SEC_QUERY_ROOTS.menu })`) so a 403 cached before the change never surfaces as `MenuUnavailableView` and the shell refetches it; the success toast `account.toastPasswordChanged` {sessionsTerminated} is shown (as on the profile path); then the landing route; the user's OTHER sessions end server-side |
| SEC-U24 | NEW | `preferredLocale` after login | when `['me']` first resolves in a new session and `preferredLocale` is `ar` / `en` and differs from the current language → `useAuthStore.setLanguage(preferredLocale)` (dir + lang switch); a later manual toggle is NOT written back to the server (Decision SEC-U74) |
| SEC-U25 | NEW | `preferredLocale` saved | a successful `PATCH /me` whose `preferredLocale` is `ar` / `en` switches the UI language at once; `""` (not set) leaves the current language |
| SEC-U26 | NEW | photo URLs | `photoUrl` is public (no token), rendered only through `<img>`; always the value of the latest `['me']` / user response (a replaced photo has a new URL; the old one may stay cached for 24 h); `onError` → initials |

### D. Fields
| # | Kind | Screen / drawer | Field | Control and rule | Read-only |
|---|---|---|---|---|---|
| SEC-U30 | NEW | SCR-SEC-004 create + edit | `phone` | `Input`, optional; client pattern = the DTO's `^$|^\+?[0-9][0-9 -]{5,28}[0-9]$`, max 30; edit: unchanged value is sent as is, emptied → `""` (clears) | no |
| SEC-U31 | NEW | SCR-SEC-004 create + edit | `jobTitleAr`, `jobTitleEn` | `Input`, optional, max 150 each, Arabic field `dir="rtl"` | no |
| SEC-U32 | NEW | SCR-SEC-004 create + edit | `preferredLocale` | `Select`: not set (`""`) / العربية (`ar`) / English (`en`) | no |
| SEC-U33 | NEW | SCR-SEC-004 create | `requireChangeAtNextLogin` | `Checkbox`, **default on** (`user-require-change`), with hint; sent as `true` / `false` | no |
| SEC-U34 | CHANGED | SCR-SEC-004 create | `password` | + policy hint `account.passwordPolicyHint` and a live byte counter (UTF-8 bytes / 72); no client `maxLength` of 200 (handover F1); the server's `SEC-400-PASSWORD-POLICY` message is shown inline on the field | no |
| SEC-U35 | NEW | SCR-SEC-004 PUT body | (wire rule) | edit sends `phone`, `jobTitleAr`, `jobTitleEn`, `preferredLocale` always (absent would mean unchanged; the form always knows the value) | — |
| SEC-U36 | NEW | SCR-SEC-004 set-password drawer | `newPassword`, confirm, `requireChangeAtNextLogin` | `Input type=password` × 2 (confirm must match, `errors.passwordMismatch`), `Checkbox` default on; policy hint + byte counter | no |
| SEC-U37 | NEW | SCR-SEC-004 VIEW | phone, job title (ar / en), preferred language, photo (summary row), password change pending (`passwordChangeRequired`, yes / no), password changed at (`passwordChangedAt`, `formatDateTime`) | `ReadOnlyRow`s | yes |
| SEC-U38 | NEW | SCR-SEC-011 | username, e-mail (read-only, hint `account.adminOnlyHint`), tenant (`tenant.code` · name in the active language), last login (read-only); full name ar / en (required, not blank), phone, job title ar / en, preferred language (editable in SEC-U11) | edit sends only the fields that changed (null / absent = unchanged; `""` clears phone, job titles, locale) | mixed |
| SEC-U39 | NEW | SCR-SEC-011 / 012 change password | `currentPassword`, `newPassword`, confirm | `Input type=password` × 3; policy hint + byte counter | no |
| SEC-U40 | NEW | photo drawers (SEC-U08, SEC-U12) | `file` | `<input type=file accept="image/png,image/jpeg,image/webp">`; client pre-check (type in the accept list, size ≤ 1 048 576 bytes) as a hint only — the server decides from the bytes; preview of the chosen file through an object URL in `<img>` (revoked on close) | no |

### E. Actions and permissions
| # | Kind | Action | Where | Permission | Call | Confirm |
|---|---|---|---|---|---|---|
| SEC-U41 | NEW | Set password | SCR-SEC-004 `?passwordFor=` | `PERM_SEC_USERS_UPDATE` | `PUT /api/v1/sec/users/{id}/password` | ConfirmDialog `user-set-password-confirm` opened by the drawer's Submit (`erp-action-confirmation` §2 item 4), like the tenant admin-reset (TENANT P2_5 TEN-U53) |
| SEC-U42 | NEW | Save / remove another user's photo | SCR-SEC-004 `?photoFor=` | `PERM_SEC_USERS_UPDATE` | `PUT` / `DELETE /api/v1/sec/users/{id}/photo` | remove: ConfirmDialog `users.confirmPhotoRemoveTitle` |
| SEC-U43 | NEW | Edit my profile | SCR-SEC-011 | signed-in STAFF | `PATCH /api/v1/sec/me` | — |
| SEC-U44 | NEW | Save / remove my photo | SCR-SEC-011 `?photo=1` | signed-in STAFF | `PUT` / `DELETE /api/v1/sec/me/photo` | remove: ConfirmDialog |
| SEC-U45 | NEW | Change my password | SCR-SEC-011 `?changePassword=1`, SCR-SEC-012 | signed-in STAFF | `PUT /api/v1/sec/me/password` | — |
| SEC-U46 | CHANGED | Create / update user | SCR-SEC-004 | as before | `POST /api/v1/sec/users`, `PUT /api/v1/sec/users/{id}` (+ the new fields) | — |
| SEC-U47 | NEW | Sign out from the forced-change page | SCR-SEC-012 | — | `POST /api/v1/sec/auth/logout` (exempt from the gate) | — |
| SEC-U50 | NEW | Revoke a saved screen grant | SCR-SEC-005 tree: uncheck a SAVED screen | `PERM_SEC_ROLES_UPDATE` (the tree's UPDATE lock, `isLocked`) | `DELETE /api/v1/sec/roles/{id}/screens/{screenId}` (`screenRegPk`) | ConfirmDialog `role-revoke-screen-confirm`: names the role, the screen and **the number of the role's saved action grants on that screen, counted from the tree** (`GET /api/v1/sec/roles/{id}/grants`), RULE-SEC-054 |
| SEC-U51 | NEW | Revoke a saved non-VIEW action grant | SCR-SEC-005 tree: uncheck a SAVED action chip | same | `DELETE /api/v1/sec/roles/{id}/actions/{actionId}` (`actionRegPk`) | ConfirmDialog `role-revoke-action-confirm` (Decision SEC-U72) |
| SEC-U52 | NEW | Revoke the saved VIEW action grant | same, the screen's gateway chip | same | same | the same ConfirmDialog with the warning `roles.revokeViewWarning` (RULE-SEC-055): the other N saved actions of the screen go too, and the screen STAYS in the role's menu (its pages answer 403) until the screen itself is unchecked |
| SEC-U53 | NEW | after a revoke | SCR-SEC-005 | — | the response count `revokedActionGrants` is shown in the dialog's outcome and in the toast; the grants query `roleQueryKeys.grants(roleId)` (`['role-grants', roleId]`) is invalidated; sessions are not ended (the next request of an affected user already sees it — no message about sessions) | — |
| SEC-U54 | NEW | super-role hint | SCR-SEC-005 grant panel, role with `isSuper = true` | VIEW | — | `Alert tone="info"` `role-super-hint`: `roles.superRoleHint` above the tree |
| SEC-U55 | REMOVED | "Individual revoke unavailable" | SCR-SEC-005 — its four uses: `RoleGrantControls.tsx` (two saved-node titles), `RoleGrantPanel.tsx` (panel note), `ModuleRevokeConfirm.tsx` (module confirm) | — | — | the text `roles.individualRevokeUnavailable` is removed with its key from `en.ts`, `ar.ts` and the key type `core/i18n/i18n.types.ts`: it is false since 1.3.0 |
| SEC-U56 | unchanged | Revoke a module | SCR-SEC-005 | UPDATE | `DELETE /api/v1/sec/roles/{id}/modules/{moduleId}` | `ModuleRevokeConfirm` as before (RULE-SEC-003) |

### F. Empty / error states and messages
| # | Kind | Condition | Where shown |
|---|---|---|---|
| SEC-U60 | NEW | 400 `SEC-400-PASSWORD-POLICY` (create, set password, own change) | inline on the password field named by `fieldErrors[0].field` (`password` / `newPassword`), text = the server's localized message (it carries the configured min / max); fallback key `account.errPasswordPolicy` when the body has no message. The generated `of` rows of the api-docs are ignored (handover "Contract caveats") |
| SEC-U61 | NEW | 422 `SEC-422-PASSWORD-SELF` | banner in the set-password drawer `account.errPasswordSelf` (defensive: the action is hidden on the own row) |
| SEC-U62 | NEW | 403 `SEC-403-PASSWORD-CURRENT-INVALID` | inline on `currentPassword`: `account.errCurrentPasswordInvalid` — NOT a session end (the transport's 403 handling ignores this code) |
| SEC-U63 | NEW | 403 `SEC-403-PASSWORD-CHANGE-REQUIRED` | no message: redirect (SEC-U22); on SCR-SEC-012 the subtitle `account.forcedSubtitle` explains it |
| SEC-U64 | NEW | 400 `SEC-400-PHOTO-INVALID`; 400 `VALIDATION_ERROR` (missing part / too large for the multipart ceiling) | inline on the file input: the server message for `SEC-400-PHOTO-INVALID` (fallback `account.errPhotoInvalid`), `errors.generic` for the other; the previous photo stays |
| SEC-U65 | CHANGED | 404 `SEC-404-USER` on set password / photo | the second-level drawer closes, the user drawer closes, one `errors.itemUnavailable` toast (the 1.2.0 deep-link policy) |
| SEC-U66 | NEW | 400 `VALIDATION_ERROR` on `PATCH /me` / user create-update (`phone`, `preferredLocale`, blank name) | inline on the named field (server message) |
| SEC-U67 | NEW | 404 `SEC-404-ROLE` / `SEC-404-GRANT` on a revoke | the dialog shows `errors.itemUnavailable`, the grants query is re-read (the tree was stale) |
| SEC-U68 | NEW | `['me']` failed | topbar initials placeholder; SCR-SEC-011 shows `errors.loadFailed` with Retry |

### G. Toasts (success, both languages)
| # | Kind | Key | en | ar |
|---|---|---|---|---|
| SEC-U80 | NEW | `users.toastPasswordSet` | Password set. {count} open session(s) ended. | تم تعيين كلمة المرور. أُنهيت {count} من الجلسات المفتوحة. |
| SEC-U81 | NEW | `users.toastPhotoSaved` | The photo has been saved. | تم حفظ الصورة. |
| SEC-U82 | NEW | `users.toastPhotoRemoved` | The photo has been removed. | تمت إزالة الصورة. |
| SEC-U83 | NEW | `account.toastProfileSaved` | Your profile has been saved. | تم حفظ ملفك الشخصي. |
| SEC-U84 | NEW | `account.toastPasswordChanged` | Your password has been changed. {count} other session(s) ended. | تم تغيير كلمة مرورك. أُنهيت {count} من جلساتك الأخرى. |
| SEC-U85 | NEW | `roles.toastScreenRevoked` | Screen grant revoked; {count} action grant(s) revoked with it. | تم سحب منح الشاشة، وسُحبت معه {count} من منح الإجراءات. |
| SEC-U86 | NEW | `roles.toastActionRevoked` | {count} action grant(s) revoked. | تم سحب {count} من منح الإجراءات. |
`{count}` is `sessionsTerminated` (SEC-U80, U84) or `revokedActionGrants` (SEC-U85, U86). The photo toasts are reused by
SCR-SEC-011 (own photo); `account.toastPasswordChanged` is shown by both password-change paths — the profile drawer
(SEC-U13) and the forced change (SCR-SEC-012, SEC-U23).

### H. i18n keys (both dictionaries, `src/core/i18n/translations/{en,ar}.ts`)
| # | Kind | Key | en | ar |
|---|---|---|---|---|
| SEC-U90 | NEW | `users.colPhone` | Phone | الهاتف |
| | NEW | `users.colJobTitle` | Job title | المسمى الوظيفي |
| | NEW | `users.columnsAction` | Columns | الأعمدة |
| | NEW | `users.columnsMenuLabel` | Show or hide columns | إظهار الأعمدة أو إخفاؤها |
| | NEW | `users.photoAlt` | Photo of {name} | صورة {name} |
| | unchanged (reused) | `users.fieldPhone` | Phone Number | رقم الهاتف |
| | NEW | `users.fieldPhoneHint` | Optional. Digits, spaces and hyphens, with an optional leading +. | اختياري. أرقام ومسافات وشرطات، مع علامة + اختيارية في البداية. |
| | NEW | `users.fieldJobTitleAr` | Job title (Arabic) | المسمى الوظيفي (عربي) |
| | NEW | `users.fieldJobTitleEn` | Job title (English) | المسمى الوظيفي (إنجليزي) |
| | unchanged (reused) | `users.fieldPreferredLang` | Preferred Language | اللغة المفضلة |
| | NEW | `users.localeNone` | Not set | غير محددة |
| | NEW | `users.localeAr` | العربية | العربية |
| | NEW | `users.localeEn` | English | English |
| | NEW | `users.fieldRequireChange` | Require a password change at the next sign-in | إلزام المستخدم بتغيير كلمة المرور عند الدخول التالي |
| | NEW | `users.fieldRequireChangeHint` | Recommended: you know this password; the user replaces it with their own. | يُنصح به: كلمة المرور هذه معروفة لك، فيستبدلها المستخدم بكلمة مرور خاصة به. |
| | NEW | `users.fieldPasswordChangeRequired` | Password change pending | تغيير كلمة المرور مطلوب |
| | NEW | `users.fieldPasswordChangedAt` | Password changed at | تاريخ تغيير كلمة المرور |
| | NEW | `users.fieldPhoto` | Photo | الصورة |
| | NEW | `users.photoNone` | No photo — initials are shown | لا توجد صورة — تُعرض الأحرف الأولى |
| | NEW | `users.photoChangeAction` | Change photo | تغيير الصورة |
| | NEW | `users.photoDrawerTitle` | Photo of {username} | صورة {username} |
| | NEW | `users.photoDrawerSubtitle` | Upload, replace or remove the photo | رفع الصورة أو استبدالها أو إزالتها |
| | NEW | `users.photoFieldFile` | Image file | ملف الصورة |
| | NEW | `users.photoConstraints` | PNG, JPEG or WebP, at most 1 MB. | PNG أو JPEG أو WebP، بحجم لا يتجاوز 1 ميغابايت. |
| | NEW | `users.photoPreview` | Preview | معاينة |
| | NEW | `users.photoUploadAction` | Save photo | حفظ الصورة |
| | NEW | `users.photoRemoveAction` | Remove photo | إزالة الصورة |
| | NEW | `users.confirmPhotoRemoveTitle` | Remove this photo? | إزالة هذه الصورة؟ |
| | NEW | `users.confirmPhotoRemove` | The photo of {username} will be removed; initials are shown instead. | ستُزال صورة {username} وتُعرض الأحرف الأولى بدلًا منها. |
| | NEW | `users.setPasswordAction` | Set password | تعيين كلمة المرور |
| | NEW | `users.passwordDrawerTitle` | Set the password of {username} | تعيين كلمة مرور {username} |
| | NEW | `users.passwordDrawerSubtitle` | Every open session of this user ends. | تنتهي كل الجلسات المفتوحة لهذا المستخدم. |
| | NEW | `users.fieldNewPassword` | New password | كلمة المرور الجديدة |
| | NEW | `users.fieldConfirmPassword` | Confirm the new password | تأكيد كلمة المرور الجديدة |
| | NEW | `users.setPasswordOwnHint` | Your own password is changed from My profile. | تُغيَّر كلمة مرورك من ملفي الشخصي. |
| | NEW | `users.setPasswordOwnLink` | Change my password | تغيير كلمة مروري |
| | NEW | `users.confirmSetPasswordTitle` | Set this password? | تعيين كلمة المرور هذه؟ |
| | NEW | `users.confirmSetPassword` | The password of {username} is replaced and every open session of that user ends. | ستُستبدل كلمة مرور {username} وتنتهي كل الجلسات المفتوحة لذلك المستخدم. |
| | NEW | `account.profileTitle` | My profile | ملفي الشخصي |
| | NEW | `account.profileSubtitle` | Your account, contact details and language | حسابك وبيانات التواصل واللغة |
| | NEW | `account.sectionAccount` | Account | الحساب |
| | NEW | `account.sectionProfile` | Profile | الملف الشخصي |
| | NEW | `account.adminOnlyHint` | Only an administrator can change the username and the e-mail. | لا يغيّر اسم الدخول والبريد الإلكتروني إلا المسؤول. |
| | NEW | `account.fieldTenant` | Organisation | المنشأة |
| | NEW | `account.editAction` | Edit profile | تعديل الملف الشخصي |
| | NEW | `account.editDrawerTitle` | Edit my profile | تعديل ملفي الشخصي |
| | NEW | `account.photoDrawerTitle` | My photo | صورتي |
| | NEW | `account.changePasswordAction` | Change password | تغيير كلمة المرور |
| | NEW | `account.changePasswordDrawerTitle` | Change my password | تغيير كلمة مروري |
| | NEW | `account.changePasswordDrawerSubtitle` | Your other sessions end; this one stays signed in. | تنتهي جلساتك الأخرى، وتبقى هذه الجلسة مفتوحة. |
| | NEW | `account.fieldCurrentPassword` | Current password | كلمة المرور الحالية |
| | NEW | `account.passwordPolicyHint` | At least 8 characters with a letter and a digit, at most 72 bytes (an Arabic letter counts as 2). | 8 أحرف على الأقل تتضمن حرفًا ورقمًا، وبحد أقصى 72 بايت (الحرف العربي يُحسب بايتين). |
| | NEW | `account.passwordBytes` | {used} / 72 bytes | {used} / 72 بايت |
| | NEW | `account.forcedTitle` | Choose a new password | اختر كلمة مرور جديدة |
| | NEW | `account.forcedSubtitle` | An administrator set your password. Replace it with your own before you continue. | عيّن المسؤول كلمة مرورك. استبدلها بكلمة مرور خاصة بك قبل المتابعة. |
| | NEW | `account.forcedSubmit` | Change password and continue | تغيير كلمة المرور والمتابعة |
| | NEW | `account.errPasswordPolicy` | The password does not meet the password policy. | كلمة المرور لا تستوفي سياسة كلمات المرور. |
| | NEW | `account.errPasswordSelf` | You cannot set your own password here; use your own password change. | لا يمكنك تعيين كلمة مرورك من هنا؛ استخدم تغيير كلمة المرور الخاصة بك. |
| | NEW | `account.errCurrentPasswordInvalid` | The current password is incorrect. | كلمة المرور الحالية غير صحيحة. |
| | NEW | `account.errPasswordChangeRequired` | You must change your password before you continue. | يجب تغيير كلمة المرور قبل المتابعة. |
| | NEW | `account.errPhotoInvalid` | The photo must be a PNG, JPEG or WebP image of at most 1 MB. | يجب أن تكون الصورة بصيغة PNG أو JPEG أو WebP وبحجم لا يتجاوز 1 ميغابايت. |
| | NEW | `topbar.accountMenu` | Account menu | قائمة الحساب |
| | NEW | `topbar.myProfile` | My profile | ملفي الشخصي |
| | NEW | `topbar.changePassword` | Change password | تغيير كلمة المرور |
| | NEW | `roles.revokeScreenTitle` | Revoke this screen grant? | سحب منح هذه الشاشة؟ |
| | NEW | `roles.confirmRevokeScreen` | The role {role} loses the screen {screen}. | سيفقد الدور {role} الشاشة {screen}. |
| | NEW | `roles.revokeScreenCascade` | {count} action grant(s) of this role on the screen are revoked with it. | تُسحب معه {count} من منح الإجراءات لهذا الدور على الشاشة. |
| | NEW | `roles.revokeActionTitle` | Revoke this action grant? | سحب منح هذا الإجراء؟ |
| | NEW | `roles.confirmRevokeAction` | The role {role} loses {action} on {screen}. | سيفقد الدور {role} الإجراء {action} على {screen}. |
| | NEW | `roles.revokeViewWarning` | Revoking VIEW also revokes this role's other actions on this screen ({count}). The screen stays in the role's menu, but its pages answer 403 — revoke the screen itself to remove the menu entry. | سحب إجراء العرض (VIEW) يسحب بقية إجراءات هذه الشاشة لهذا الدور ({count}). تبقى الشاشة في قائمة الدور لكن صفحاتها ترفض الوصول (403) — اسحب الشاشة نفسها لإزالتها من القائمة. |
| | NEW | `roles.revokeAction` | Revoke | سحب |
| | NEW | `roles.revokeSavedHint` | Saved — uncheck to revoke | محفوظ — أزل التحديد للسحب |
| | NEW | `roles.superRoleHint` | This role holds every authority regardless of grants; grants only shape its menu. | يملك هذا الدور كل الصلاحيات بغض النظر عن المنح؛ المنح تحدد قائمته فقط. |
| | REMOVED | `roles.individualRevokeUnavailable` | — | — |
Every NEW key is also declared in `core/i18n/i18n.types.ts` (the dictionaries are typed against it). Plus the seven toast keys of §G. Labels reused unchanged: `users.fieldPhone` and `users.fieldPreferredLang` (in both dictionaries since 1.0, unused until now), `users.fieldUsername`, `users.fieldEmail`,
`users.fieldFullNameAr`, `users.fieldFullNameEn`, `users.colLastLogin`, `errors.passwordMismatch`, `topbar.signOut`,
`common.*`.

### I. Endpoints (method + path exactly as `docs/api-docs/sec/endpoints/`)
| # | Kind | Method | Path | File | Used by |
|---|---|---|---|---|---|
| SEC-U100 | NEW | GET | `/api/v1/sec/me` | `my-profile.md` | `['me']` (SEC-U21), SCR-SEC-011, SCR-SEC-012, topbar |
| SEC-U101 | NEW | PATCH | `/api/v1/sec/me` | `my-profile.md` | SEC-U11 |
| SEC-U102 | NEW | PUT | `/api/v1/sec/me/password` | `my-profile.md` | SEC-U13, SCR-SEC-012 |
| SEC-U103 | NEW | PUT | `/api/v1/sec/me/photo` | `my-profile.md` | SEC-U12 (multipart part `file`) |
| SEC-U104 | NEW | DELETE | `/api/v1/sec/me/photo` | `my-profile.md` | SEC-U12 (204, also without a photo) |
| SEC-U105 | NEW | PUT | `/api/v1/sec/users/{id}/password` | `users.md` | SEC-U09 → `PasswordChangeResponse` |
| SEC-U106 | NEW | PUT | `/api/v1/sec/users/{id}/photo` | `users.md` | SEC-U08 |
| SEC-U107 | NEW | DELETE | `/api/v1/sec/users/{id}/photo` | `users.md` | SEC-U08 |
| SEC-U108 | CHANGED | POST | `/api/v1/sec/users` | `users.md` | + `phone`, `jobTitleAr`, `jobTitleEn`, `preferredLocale`, `requireChangeAtNextLogin` |
| SEC-U109 | CHANGED | PUT | `/api/v1/sec/users/{id}` | `users.md` | + the four profile fields |
| SEC-U110 | CHANGED | POST | `/api/v1/sec/users/search` | `users.md` | rows carry `photoUrl`, `phone`, `jobTitleAr/En` (SEC-U06) |
| SEC-U111 | CHANGED | GET | `/api/v1/sec/users/{id}` | `users.md` | VIEW rows SEC-U37 |
| SEC-U112 | CHANGED | POST | `/api/v1/sec/auth/login` | `authentication.md` | `passwordChangeRequired` (SEC-U04, SEC-U20) |
| SEC-U113 | unchanged | POST | `/api/v1/sec/auth/logout` | `authentication.md` | SEC-U47 (exempt from the gate) |
| SEC-U114 | NEW | DELETE | `/api/v1/sec/roles/{id}/screens/{screenId}` | `role-grants.md` | SEC-U50 → 200 `{ revokedActionGrants }` |
| SEC-U115 | NEW | DELETE | `/api/v1/sec/roles/{id}/actions/{actionId}` | `role-grants.md` | SEC-U51, U52 → 200 `{ revokedActionGrants }` (the requested grant included) |
| SEC-U116 | unchanged | GET | `/api/v1/sec/roles/{id}/grants` | `role-grants.md` | the cascade count of SEC-U50 / U52 before the call |
| SEC-U117 | unchanged | DELETE | `/api/v1/sec/roles/{id}/modules/{moduleId}` | `role-grants.md` | SEC-U56 |
| SEC-U118 | unchanged | GET | `/api/v1/sec/menu` | `menu.md` | answers 403 `SEC-403-PASSWORD-CHANGE-REQUIRED` while the change is pending (SEC-U22) |
| SEC-U119 | NEW (consumed) | GET | `/api/v1/tenant/me` | `../tenant/endpoints/tenant-branding.md` | SCR-SEC-012 logo (exempt from the gate); described in TENANT P2_5 |

### J. Decisions
| # | Kind | Decision | Alternatives | Choice and why |
|---|---|---|---|---|
| SEC-U70 | NEW (Decision — plan §8 F1, handover §0) | authentication-only routes for `/account/*` | (a) a new page code `SEC_PROFILE` in the menu; (b) authentication-only routes | **(b)**: `ProtectedRoute` is page-code based, and the profile and the forced change are for every signed-in STAFF user; the backend added no page code (`/me` is `isAuthenticated()`), so a page code would be a frontend invention the menu cannot deliver. `/account/profile` is a child of the shell's authentication-only layout route; `/account/change-password` is outside the shell (SEC-U02) |
| SEC-U71 | NEW (Decision) | where the forced-change page lives | (a) inside `AppShell`; (b) a standalone auth-only page | **(b)**: while the change is pending `GET /sec/menu` answers 403, so the shell cannot render its navigation; `AuthScreenShell` (the login layout) is the honest frame, with the tenant logo from `/tenant/me` (allowed during the change). The page is guarded by `<AuthenticatedRoute>` (SEC-U15), not `<ProtectedRoute>`, so it makes no menu request, and the menu query is reset after the change (SEC-U23) |
| SEC-U72 | NEW (Decision) | confirming a non-VIEW action revoke | (a) revoke at once on uncheck; (b) ConfirmDialog for every revoke | **(b)**: every revoke narrows a role's access immediately for all its users (no session end, no undo); the plan names a confirm for the screen and a warning for VIEW — one dialog component serves all three with the cascade text only where it applies |
| SEC-U73 | NEW (Decision) | sign-out placement once the account menu exists | (a) move sign-out into the account menu; (b) keep the separate `topbar-sign-out` button | **(b)**: one-click sign-out stays, the existing locator and TC-FE-AUTH-005 stay valid, and the account menu holds only what is new |
| SEC-U74 | NEW (Decision) | language toggle vs `preferredLocale` | (a) the toggle PATCHes `/me`; (b) the toggle is local, the profile saves the preference | **(b)**: the plan applies `preferredLocale` after login and on save; writing every toggle would turn a quick switch into a profile change |
| SEC-U75 | NEW (Decision) | photo removal | (a) remove at once; (b) ConfirmDialog | **(b)**: the same rule as the tenant logo (TENANT P2_5) — a removal has no undo and the old URL may stay cached |
| SEC-U76 | NEW (Decision) | users drawer URL | plan text `?action=create` / `?editId=` vs the as-built route segments | **as built** (`/security/users/new`, `/:id`, `/:id/edit` — the owner's decision recorded in `usersRoutePaths.ts`); the new second levels are query parameters on those routes (`?photoFor=`, `?passwordFor=`), like the existing `?picker=roles` |
| SEC-U77 | NEW (Decision) | optional list columns | (a) always shown; (b) hidden by default with a column menu | **(b)** as the plan says; the visibility is a per-session view preference in Zustand, not navigation state, so it is not in the URL |
| SEC-U78 | NEW (Decision) | level-2 drawers that SAVE (`?photoFor=`, `?passwordFor=`; TENANT `?logoFor=`, `?adminResetFor=`) | (a) the `CLAUDE.md` §3 / `erp-ui-and-state-mandates` §2.5 rule — a level-2 drawer returns a value to its parent and never saves; (b) a level-2 drawer that saves its own record | **(b), a deliberate exception** dictated by plan §8 F1 / F3 and the handover: the parent is a READ-ONLY detail drawer (users VIEW route, tenant `?tenantId=`) with no form to return a value to, and each action is its own endpoint (photo multipart, password set, logo, admin-reset) — one drawer, one job, one submit; offered on read-only parents only (SEC-U08, SEC-U09: never on `/new` or `/:id/edit`) |

### K. data-testid (new; used by the `TC-FE-*` cases)
`topbar-account`, `topbar-account-menu`, `topbar-account-profile`, `topbar-account-password`, `user-row-avatar`,
`users-columns-menu`, `users-column-phone`, `users-column-jobTitle`, `user-phone`, `user-jobTitleAr`, `user-jobTitleEn`,
`user-preferredLocale`, `user-require-change`, `user-photo-row`, `user-photo-change`, `user-set-password`,
`user-password-drawer`, `user-password-new`, `user-password-confirm`, `user-password-require-change`,
`user-password-submit`, `user-set-password-own-hint`, `photo-drawer`, `photo-file`, `photo-preview`, `photo-save`,
`photo-remove`, `account-profile`, `account-edit`, `account-photo-change`, `account-change-password`,
`account-password-current`, `account-password-new`, `account-password-confirm`, `account-password-submit`,
`account-forced-change`, `account-forced-sign-out`, `role-revoke-screen-confirm`, `role-revoke-action-confirm`,
`role-revoke-cascade-count`, `role-revoke-view-warning`, `role-super-hint`, `user-set-password-confirm`.

### L. Traceability
| Rows | Serves | Test cases (newfront `docs/test-e2e/front-test-plan.md`) |
|---|---|---|
| SEC-U06 | SCR-REQ-SEC-004 CHANGED (1.3.0 §9.4), REQ-SEC-088 | TC-FE-SEC-013 |
| SEC-U07, U30 … U35, U46 | REQ-SEC-088, RULE-SEC-058, RULE-SEC-062 | TC-FE-SEC-014 |
| SEC-U09, U36, U41, U61, U78, U80 | REQ-SEC-083, RULE-SEC-057, RULE-SEC-058 | TC-FE-SEC-015, TC-FE-SEC-016 |
| SEC-U34, U60 | REQ-SEC-082, RULE-SEC-056 | TC-FE-SEC-017 |
| SEC-U02 … U04, U15, U20, U22, U23, U47, U63, U71, U84 | REQ-SEC-084, REQ-SEC-085, RULE-SEC-059 | TC-FE-SEC-018, TC-FE-SEC-019 |
| SEC-U01, U11, U38, U43, U66, U83 | REQ-SEC-086 | TC-FE-SEC-020 |
| SEC-U24, U25 | REQ-SEC-086, REQ-SEC-088, RULE-SEC-062 | TC-FE-SEC-021 |
| SEC-U12, U26, U40, U44, U64, U75, U81, U82 | REQ-SEC-087, RULE-SEC-061 | TC-FE-SEC-022 |
| SEC-U08, U42, U65 | REQ-SEC-087 | TC-FE-SEC-023 |
| SEC-U13, U39, U45, U62, U84 | REQ-SEC-085, RULE-SEC-060 | TC-FE-SEC-024 |
| SEC-U05, U21, U68, U73 | REQ-SEC-086 (ADR-SEC-064: no roles in `/me`) | TC-FE-SEC-025 |
| SEC-U10, U50, U53, U67, U85 | REQ-SEC-080, RULE-SEC-054 | TC-FE-SEC-026 |
| SEC-U51, U72, U86 | REQ-SEC-081 | TC-FE-SEC-027 |
| SEC-U52 | REQ-SEC-081, RULE-SEC-055, ADR-SEC-062 | TC-FE-SEC-028 |
| SEC-U54 | 1.3.0 §6 (super role), §8 | TC-FE-SEC-029 |
| SEC-U55, U56 | REQ-SEC-015, RULE-SEC-003 | TC-FE-SEC-030 |
══════════════════════════════════════════════════════════════════
