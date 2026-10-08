# FLOW DIAGRAM — الأمان / Security (SEC)
══════════════════════════════════════════════════════════════════
Module : SEC   Version : v1   Profile : erp   Stage : P2.5 (UX design — frontend)
Sources: srs-sec.md (REQ / AC / SCR-REQ, 1.3.0 addendum §1–§9) · prd-sec.md (US-SEC-*) · newfront `mxdashboard` 1.2.0 as built
Screens: SCR-SEC-001, SCR-SEC-004, SCR-SEC-005, SCR-SEC-010 (baseline, the screens plan §8 touches) · SCR-SEC-011, SCR-SEC-012 (frontend 1.3.0 addendum)
══════════════════════════════════════════════════════════════════

Created by the frontend step TM-FA in the MDL files' pattern (SEC had no `P2_5` folder). The baseline records the
navigation of the touched screens as built for erp-core 1.2.0; the addendum adds the 1.3.0 flows.

## The screens these flows move between (baseline)

| SCR | الاسم / Name | Route | Page code | SRS screen requirement |
|---|---|---|---|---|
| SCR-SEC-001 | تسجيل الدخول / Login (+ SCR-SEC-002 Sign-up, SCR-SEC-003 Reset) | `/login` (`/sign-up`, `/password-reset*`) | SEC_LOGIN (public) | SCR-REQ-SEC-001 (002, 003) |
| SCR-SEC-004 | المستخدمون / Users | `/security/users` (+ `/new`, `/:id`, `/:id/edit`, `/pending`) | SEC_USERS | SCR-REQ-SEC-004 |
| SCR-SEC-005 | الأدوار والصلاحيات / Roles & permissions | `/security/roles?editId=` | SEC_ROLES | SCR-REQ-SEC-005 |
| SCR-SEC-010 | القائمة الديناميكية / Dynamic menu (the shell) | every authenticated route | — | SCR-REQ-SEC-010 |

## FLOW — تسجيل الدخول / Sign in          traces=US-SEC-001,REQ-SEC-001,REQ-SEC-002,SCR-SEC-001,SCR-SEC-010
```
Screens   : SCR-SEC-001, SCR-SEC-010
Sequence  : /login → tenant + username + password → POST /api/v1/sec/auth/login (X-Tenant-Code)
            → session established → landing route /dashboard inside the shell (menu from GET /api/v1/sec/menu)
Trigger   : a staff user starts work
Priority  : HIGH (US-SEC-001)
```
A refusal stays on `/login` (inline on the Tenant field or the credentials alert); it is not a navigation step.

## FLOW — إدارة المستخدمين / Manage users          traces=US-SEC-004,REQ-SEC-009,REQ-SEC-010,REQ-SEC-011,SCR-SEC-004
```
Screens   : SCR-SEC-004
Sequence  : menu → /security/users → row → /security/users/:id (view) → Edit → /:id/edit → save
            → (roles changed) PUT /users/{id}/roles → back to the list with the toast
Trigger   : an administrator creates, edits, (de)activates a user or assigns roles
Priority  : HIGH (US-SEC-004)
```

## FLOW — منح الأدوار / Grant a role's modules, screens and actions          traces=US-SEC-005,REQ-SEC-012,REQ-SEC-013,REQ-SEC-014,REQ-SEC-015,SCR-SEC-005
```
Screens   : SCR-SEC-005
Sequence  : menu → /security/roles → select a role (?editId=) → stage modules / screens / actions in the tree
            → Save (ordered module → screen → action calls) ; or Revoke module → ConfirmDialog → DELETE /modules/{moduleId}
Trigger   : an administrator shapes a role
Priority  : HIGH (US-SEC-005)
```
In 1.2.0 a saved screen or action could not be revoked on its own (the 1.3.0 addendum closes this).

---

## Implementation Addendum — frontend 1.3.0
Source version : mxdashboard 1.3.0 (unreleased) against erp-core 1.3.0 (newback `f48b9ab`)
Change         : tenant-maturity plan §8 F1 and F4 — the flows of `ui-ux-spec-sec.md` "Implementation Addendum — frontend 1.3.0" (rows `SEC-Unn`)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

| SCR | Kind | الاسم / Name | Route | Page code |
|---|---|---|---|---|
| SCR-SEC-011 | NEW | ملفي الشخصي / My profile | `/account/profile` (`?editId=me`, `?photo=1`, `?changePassword=1`) | none — authentication only (SEC-U70) |
| SCR-SEC-012 | NEW | تغيير كلمة المرور الإلزامي / Forced password change | `/account/change-password` | none — authentication only (`<AuthenticatedRoute>`, no menu read), outside the shell (SEC-U71, SEC-U15) |

## FLOW — تغيير كلمة المرور الإلزامي عند الدخول / Forced password change at sign-in          traces=US-SEC-001,REQ-SEC-084,REQ-SEC-085,REQ-SEC-088,SCR-SEC-001,SCR-SEC-012,SCR-SEC-010
```
Screens   : SCR-SEC-001 → SCR-SEC-012 → SCR-SEC-010
Sequence  : /login → POST /api/v1/sec/auth/login answers passwordChangeRequired = true
            → session established, flag set (SEC-U20) → /account/change-password (SEC-U04)
            → GET /api/v1/tenant/me (logo) + GET /api/v1/sec/me (flag) → current + new + confirm
            → PUT /api/v1/sec/me/password 200 → flag cleared in the store and in ['me'], the menu query reset
            → toast account.toastPasswordChanged {sessionsTerminated} → /dashboard with the SAME token (menu refetched, 200)
Branch    : any shell route opened meanwhile → redirected to /account/change-password (SEC-U03)
Branch    : a staff call answering 403 SEC-403-PASSWORD-CHANGE-REQUIRED (a reload, another tab) → flag set,
            redirect, no toast (SEC-U22)
Branch    : wrong current password → 403 SEC-403-PASSWORD-CURRENT-INVALID inline, stay; policy refusal → 400 inline
Branch    : Sign out → POST /api/v1/sec/auth/logout → /login
Trigger   : an administrator created the user, set their password, or the platform operator reset a tenant
            administrator (TENANT P2_5 admin-reset flow)
Priority  : HIGH (REQ-SEC-084)
```

## FLOW — المسؤول يعيّن كلمة مرور / An administrator sets a user's password          traces=US-SEC-004,REQ-SEC-083,REQ-SEC-082,SCR-SEC-004
```
Screens   : SCR-SEC-004
Sequence  : /security/users → row → /security/users/:id → Set password
            → /security/users/:id?passwordFor=:id (second-level drawer) → new + confirm + "require change" (on)
            → Submit → ConfirmDialog (the user's sessions all end) → Confirm
            → PUT /api/v1/sec/users/{id}/password 200 → toast users.toastPasswordSet {sessionsTerminated}
            → second level closes, the view drawer re-reads the user (passwordChangeRequired = true)
Branch    : the signed-in user's own row → no Set password action; the hint links to /account/profile?changePassword=1
Branch    : 400 SEC-400-PASSWORD-POLICY → inline on newPassword (server message); 404 SEC-404-USER → both drawers close + itemUnavailable toast
Trigger   : a user forgot the password, or a new hand-over is needed
Priority  : HIGH (REQ-SEC-083)
```

## FLOW — المستخدم يغيّر كلمة مروره / Change my own password          traces=US-SEC-001,REQ-SEC-085,REQ-SEC-082,SCR-SEC-010,SCR-SEC-011
```
Screens   : SCR-SEC-010 → SCR-SEC-011
Sequence  : topbar account menu → Change password → /account/profile?changePassword=1
            → current + new + confirm → PUT /api/v1/sec/me/password 200
            → toast account.toastPasswordChanged {sessionsTerminated} → drawer closes; this session stays signed in
Branch    : 403 SEC-403-PASSWORD-CURRENT-INVALID → inline on currentPassword (not a session end)
Trigger   : a staff user rotates the password
Priority  : HIGH (REQ-SEC-085)
```

## FLOW — رفع الصورة أو إزالتها / Upload or remove a photo          traces=US-SEC-004,REQ-SEC-087,SCR-SEC-004,SCR-SEC-011,SCR-SEC-010
```
Screens   : SCR-SEC-011 (own) · SCR-SEC-004 (another staff user)
Sequence  : own — /account/profile → Change photo → ?photo=1 → choose PNG / JPEG / WebP ≤ 1 MB → preview
                  → PUT /api/v1/sec/me/photo 200 { photoUrl } → ['me'] updated → topbar avatar shows the new URL
            other — /security/users/:id → photo summary row → ?photoFor=:id → same → PUT /api/v1/sec/users/{id}/photo
                  → list row avatar and drawer show the new photoUrl
            remove — Remove photo → ConfirmDialog → DELETE …/photo 204 → initials everywhere
Branch    : 400 SEC-400-PHOTO-INVALID (SVG, executable, > 1 MB) → inline on the file input, previous photo kept
Trigger   : a user or an administrator puts a face on an account
Priority  : MEDIUM (REQ-SEC-087)
```

## FLOW — تعديل ملفي واللغة المفضلة / Edit my profile and preferred language          traces=US-SEC-001,REQ-SEC-086,SCR-SEC-011,SCR-SEC-010
```
Screens   : SCR-SEC-010 → SCR-SEC-011
Sequence  : topbar account menu → My profile → /account/profile → Edit → ?editId=me
            → PATCH /api/v1/sec/me (changed fields only) 200 → toast → preferredLocale ar/en applied at once
Branch    : next sign-in → GET /api/v1/sec/me → preferredLocale applied after login (SEC-U24)
Trigger   : a staff user keeps their own details current
Priority  : MEDIUM (REQ-SEC-086)
```

## FLOW — سحب منح شاشة أو إجراء / Revoke a screen or an action grant          traces=US-SEC-005,REQ-SEC-080,REQ-SEC-081,SCR-SEC-005
```
Screens   : SCR-SEC-005
Sequence  : /security/roles?editId=:role → expand a module → uncheck a SAVED screen
            → ConfirmDialog naming the role, the screen and N = the role's saved action grants on it (from GET /roles/{id}/grants)
            → DELETE /api/v1/sec/roles/{id}/screens/{screenId} 200 { revokedActionGrants = N }
            → toast roles.toastScreenRevoked {N} → grants re-read, the screen unchecked
Branch    : uncheck a SAVED non-VIEW action → ConfirmDialog → DELETE /api/v1/sec/roles/{id}/actions/{actionId} → { revokedActionGrants = 1 }
Branch    : uncheck the SAVED VIEW action → ConfirmDialog with the cascade + "screen stays in the menu" warning
            → { revokedActionGrants = 1 + N } → every action of the screen unchecked, the screen stays checked
Branch    : a STAGED (unsaved) item → un-staged locally, no call (unchanged)
Branch    : role with isSuper = true → the hint "grants only shape its menu" stays visible throughout
Trigger   : an administrator narrows a role without revoking the whole module
Priority  : HIGH (REQ-SEC-080, REQ-SEC-081)
```
Sessions are not ended: the affected users' next requests already see the change (1.3.0 §6), so the flow has no
"users signed out" step.

TM-F1 as built (`ui-ux-spec-sec.md` SEC-U16, SEC-U27): in the forced-change flow the `GET /api/v1/tenant/me` logo
step arrives with F2 (TENANT P2_5 TEN-U01, TEN-U04); until then SCR-SEC-012 shows the platform mark. The flag set by
the login answer or by a 403 is cleared only by `PUT /me/password` 200 (or a sign-out), never by a later `['me']` read.

## Story coverage (addendum)

| US / REQ | Flow | SCR |
|---|---|---|
| US-SEC-001 · REQ-SEC-084, -085 | forced change at sign-in; change my own password | SCR-SEC-001 → SCR-SEC-012; SCR-SEC-011 |
| US-SEC-001 · REQ-SEC-086 | edit my profile and preferred language | SCR-SEC-011 |
| US-SEC-004 · REQ-SEC-083 | an administrator sets a user's password | SCR-SEC-004 |
| US-SEC-004 · REQ-SEC-087 | upload or remove a photo | SCR-SEC-004, SCR-SEC-011 |
| US-SEC-005 · REQ-SEC-080, -081 | revoke a screen or an action grant | SCR-SEC-005 |
| REQ-SEC-082 (policy), REQ-SEC-088 (fields), REQ-SEC-089 (e-mail) | inside the flows above; REQ-SEC-089 has no screen (an e-mail sent by NOTIF) | — |
══════════════════════════════════════════════════════════════════
