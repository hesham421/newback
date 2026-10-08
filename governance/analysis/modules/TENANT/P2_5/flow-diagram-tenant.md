# FLOW DIAGRAM — المستأجرون / Tenant (TENANT)
══════════════════════════════════════════════════════════════════
Module : TENANT   Version : v1   Profile : erp   Stage : P2.5 (UX design — frontend)
Sources: srs-tenant.md (REQ / AC / SCR-REQ-TENANT-001; 1.3.0 addenda B, E, C12, C4, C5) · prd-tenant.md (US-TENANT-*) · newfront `mxdashboard` 1.2.0 as built
Screens: SCR-TENANT-001 (`PLATFORM_TENANTS`) · read by the shell SCR-SEC-010 and the sign-in pages SCR-SEC-001 … 003 (branding), SCR-SEC-012 (forced change), SCR-FILE-002 (archive re-download)
══════════════════════════════════════════════════════════════════

Created by the frontend step TM-FA in the MDL files' pattern (TENANT had no `P2_5` folder). The baseline records the
1.2.0 navigation; the addendum adds the 1.3.0 flows of `ui-ux-spec-tenant.md` (rows `TEN-Unn`).

## The screens these flows move between (baseline)

| SCR | الاسم / Name | Route | Page code | SRS screen requirement |
|---|---|---|---|---|
| SCR-TENANT-001 | المستأجرون / Tenants | `/platform/tenants` (`?action=create`, `?tenantId=`) | PLATFORM_TENANTS | SCR-REQ-TENANT-001 |

## FLOW — تجهيز مستأجر / Provision a tenant          traces=US-TENANT-001,REQ-TENANT-001,REQ-TENANT-002,REQ-TENANT-003,REQ-TENANT-004,SCR-TENANT-001
```
Screens   : SCR-TENANT-001
Sequence  : menu PLATFORM → Tenants → New tenant (?action=create) → tenant + first administrator
            → POST /api/v1/platform/tenants 201 → toast → list
Trigger   : a new organisation joins the platform
Priority  : HIGH (US-TENANT-001)
```

## FLOW — تعليق مستأجر وإعادة تفعيله / Suspend and activate a tenant (1.2.0)          traces=US-TENANT-003,REQ-TENANT-008,REQ-TENANT-009,SCR-TENANT-001
```
Screens   : SCR-TENANT-001
Sequence  : list → row → ?tenantId= → Suspend → ConfirmDialog → PATCH …/status { SUSPENDED } ; Activate (no dialog)
Trigger   : unpaid invoice, end of contract, abuse
Priority  : HIGH (US-TENANT-003)
```
The PLATFORM row offers no Suspend (RULE-TENANT-005).

---

## Implementation Addendum — frontend 1.3.0
Source version : mxdashboard 1.3.0 (unreleased) against erp-core 1.3.0 (newback `f48b9ab`)
Change         : tenant-maturity plan §8 F2 and F3 — the flows of `ui-ux-spec-tenant.md` "Implementation Addendum — frontend 1.3.0"
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

## FLOW — تحميل علامة الواجهة / Shell branding load          traces=US-TENANT-013,REQ-TENANT-031,REQ-TENANT-030,SCR-SEC-010
```
Screens   : SCR-SEC-010 (the shell)
Sequence  : sign-in succeeds → shell mounts → GET /api/v1/tenant/me once (['tenant','branding', code])
            → logoUrl present: sidebar [platform mark] · [tenant logo]; collapsed rail: tenant logo
            → brandColor present: --color-accent-tenant on :root → sidebar active entry tinted
            → no logo / image onError / call failed: mark only (today's markup)
Branch    : 401 TENANT_TOKEN_REVOKED → session cleared → /login with "your organisation's sessions were ended"
Branch    : 403 TENANT_SUSPENDED → session cleared (1.2.0 endRevokedSession)
Branch    : sign-out / another tenant → branding cache dropped, accent removed
Trigger   : every signed-in session (staff; the call also works for customer tokens and during a forced change)
Priority  : MEDIUM (US-TENANT-013)
```

## FLOW — علامة صفحة الدخول / Sign-in page branding          traces=US-TENANT-014,REQ-TENANT-032,SCR-SEC-001,SCR-SEC-002,SCR-SEC-003
```
Screens   : SCR-SEC-001 (/login), SCR-SEC-002 (/sign-up), SCR-SEC-003 (/password-reset*)
Sequence  : the Tenant code settles (400 ms after the last keystroke, valid pattern; or host-named / pre-filled at once)
            → GET /api/v1/public/tenants/{tenantCode}/branding (no token, no tenant header)
            → 200 with logoUrl: tenant logo above the form; the aside keeps the platform mark
Branch    : 404 TENANT_NOT_FOUND / 403 TENANT_SUSPENDED / network → mark only, no toast (the submit reports the tenant error)
Branch    : 429 TENANT_BRANDING_RATE_LIMITED → mark only, no toast, no request before Retry-After seconds
Branch    : the code changes → that code's own cached answer or a new request — never the previous code's logo
Trigger   : a user opens a sign-in page of their organisation
Priority  : MEDIUM (US-TENANT-014)
```

## FLOW — ضبط شعار المستأجر ولونه وإزالته / Set or remove a tenant's logo and brand colour          traces=US-TENANT-012,REQ-TENANT-029,REQ-TENANT-030,SCR-TENANT-001,SCR-SEC-010
```
Screens   : SCR-TENANT-001 → (the tenant's users) SCR-SEC-010
Sequence  : /platform/tenants → row → ?tenantId=:id → branding summary row → Change branding
            → ?tenantId=:id&logoFor=:id → choose PNG / JPEG / WebP / plain SVG ≤ 1 MB → preview (<img>) → optional #RRGGBB
            → Save: PUT /api/v1/platform/tenants/{id}/logo (200 TenantResponse) → then PATCH …/{id}/branding when the colour changed
            → preview from the returned logoUrl, toasts → the tenant's users see it at their next sign-in (shell flow)
Branch    : Remove logo → ConfirmDialog → DELETE …/{id}/logo 204 → "Platform mark only"
Branch    : 400 TENANT_LOGO_INVALID (unsafe SVG, too large, other type) → inline, current logo kept, colour not sent
Branch    : 400 TENANT_BRAND_COLOR_INVALID → inline on the colour
Branch    : the edited tenant is the operator's own session tenant (PLATFORM) → shell branding re-read at once
Trigger   : an organisation sends its logo to the platform operator (no tenant self-service in 1.3.0, ADR-TENANT-005)
Priority  : MEDIUM (US-TENANT-012)
```

## FLOW — تعديل ملف المستأجر / Edit a tenant's names and profile          traces=US-TENANT-009,REQ-TENANT-025,SCR-TENANT-001
```
Screens   : SCR-TENANT-001
Sequence  : ?tenantId=:id → Edit → ?editId=:id (code locked) → PUT /api/v1/platform/tenants/{id} (full replacement)
            → toast → ?tenantId=:id with the new values
Trigger   : a tenant renames itself or changes its contact details
Priority  : MEDIUM (US-TENANT-009)
```

## FLOW — التعليق بسبب وإعادة التفعيل / Suspend with a reason; reactivate          traces=US-TENANT-003,REQ-TENANT-026,REQ-TENANT-033,REQ-TENANT-034,SCR-TENANT-001,SCR-SEC-001,SCR-SEC-010
```
Screens   : SCR-TENANT-001 · (the tenant's users) SCR-SEC-010 → SCR-SEC-001
Sequence  : ?tenantId=:id → Suspend → ConfirmDialog with the mandatory reason (3..500) → PATCH …/status { SUSPENDED, reason }
            → toast, badge SUSPENDED, "Suspension" facts (at, by, reason) in the detail
            → every session of the tenant ends at once (its users' next request: 403 TENANT_SUSPENDED → /login)
            → later: Activate → PATCH …/status { ACTIVE } → facts cleared
            → every EARLIER token of the tenant answers 401 TENANT_TOKEN_REVOKED → its users land on /login and sign in again
Branch    : reason shorter than 3 / longer than 500 → Confirm disabled; a server 400 TENANT_SUSPENSION_REASON_REQUIRED inline
Trigger   : unpaid invoice, end of contract, abuse
Priority  : HIGH (US-TENANT-003)
```

## FLOW — استخدام المستأجر / Read a tenant's usage          traces=US-TENANT-011,REQ-TENANT-028,SCR-TENANT-001
```
Screens   : SCR-TENANT-001
Sequence  : ?tenantId=:id → open the "Usage" section → GET /api/v1/platform/tenants/{id}/usage (live count) → figures + "Counted at"
            → Refresh → counted again
Trigger   : capacity or billing question about one tenant
Priority  : MEDIUM (US-TENANT-011)
```

## FLOW — إعادة تعيين كلمة مرور مدير المستأجر / Reset a tenant administrator's password          traces=US-TENANT-010,REQ-TENANT-027,SCR-TENANT-001,SCR-SEC-012
```
Screens   : SCR-TENANT-001 · (the administrator) SCR-SEC-001 → SCR-SEC-012
Sequence  : ?tenantId=:id (not PLATFORM) → Reset administrator password → ?tenantId=:id&adminResetFor=:id
            → username + new password + confirm + "require change" (on) → Submit → ConfirmDialog
            → POST /api/v1/platform/tenants/{id}/admin-reset 200 { username, sessionsTerminated } → toast
            → the administrator signs in with the new password → forced password change (SEC P2_5 flow)
Branch    : 404 TENANT_ADMIN_NOT_FOUND / 422 TENANT_ADMIN_NOT_SUPER → inline on username; 400 SEC-400-PASSWORD-POLICY → inline on the password
Branch    : the PLATFORM row → no such action (422 TENANT_ADMIN_RESET_PLATFORM is never provoked)
Trigger   : a tenant lost its only administrator's password
Priority  : HIGH (US-TENANT-010)
```

## FLOW — إبطال رموز المستأجر / Revoke a tenant's tokens          traces=US-TENANT-015,REQ-TENANT-035,REQ-TENANT-034,SCR-TENANT-001,SCR-SEC-001
```
Screens   : SCR-TENANT-001 · (the tenant's users) SCR-SEC-001
Sequence  : ?tenantId=:id (not PLATFORM) → Sign every user out → ConfirmDialog
            → POST /api/v1/platform/tenants/{id}/revoke-tokens 200 { id, code, sessionsTerminated } → toast
            → the tenant's users: next request 401 TENANT_TOKEN_REVOKED → /login with the message → sign in again
Branch    : 500 TENANT_REVOKE_SESSIONS_FAILED → the dialog stays with the message → Try again → the same call
Trigger   : a leaked credential or a security incident in one organisation
Priority  : HIGH (US-TENANT-015)
```

## FLOW — تصدير بيانات المستأجر وتنزيلها / Export a tenant's data and download it          traces=US-TENANT-016,REQ-TENANT-037,SCR-TENANT-001,SCR-FILE-002
```
Screens   : SCR-TENANT-001 · later SCR-FILE-002
Sequence  : ?tenantId=:id → Export data → ConfirmDialog → Export
            → POST /api/v1/platform/tenants/{id}/export (synchronous: progress + elapsed seconds, no automatic retry)
            → 200 { fileName, rowCount, sizeBytes, downloadToken } → GET /api/v1/files/download?token= at once → ZIP saved
            → the dialog shows the file name, rows and size and the link "Open in the file browser" → toast
Branch    : 409 TENANT_EXPORT_IN_PROGRESS / 422 TENANT_EXPORT_TOO_LARGE / 429 TENANT_EXPORT_BUSY ("try again shortly") → message in the dialog
Branch    : the download fails (token used / expired) → message: get it from the file browser
Branch    : re-download later → ?tenantId=:id → "Exported archives" (or the result's link)
            → deep link /files/browser?moduleCode=TENANT&ownerType=CORE_TENANT&ownerId=:id[&fileId=]
            → the archive → Download (POST /api/v1/files/{id}/access-token → new token → download);
            the scope cannot be picked by hand (no TENANT row in the module registry) — the link is the way in;
            a user without PLATFORM_TENANT_MANAGE never sees the archive (404 FILE_DOCUMENT_NOT_FOUND)
Trigger   : an organisation asks for its data (portability, exit)
Priority  : MEDIUM (US-TENANT-016)
```

## FLOW — إنشاء مستأجر بإعادة آمنة / Create a tenant with a safe retry          traces=US-TENANT-001,REQ-TENANT-036,SCR-TENANT-001
```
Screens   : SCR-TENANT-001
Sequence  : New tenant (?action=create) → a UUID Idempotency-Key is minted for this form → submit → POST with the key
            → (timeout / network loss) → submit again with the SAME key and body → 201 (Idempotent-Replayed: true) → same toast as a first 201
Branch    : a field changed after a failed attempt → a new key before the next submit
Branch    : 409 IDEMPOTENCY_KEY_CONFLICT → banner, a new key minted; 400 IDEMPOTENCY_KEY_INVALID → banner
Branch    : 400 SEC-400-PASSWORD-POLICY on adminPassword → inline, nothing created
Trigger   : a slow network while provisioning
Priority  : MEDIUM (REQ-TENANT-036)
```

## Story coverage (addendum)

| US | Flow | SCR |
|---|---|---|
| US-TENANT-001 | create a tenant with a safe retry | SCR-TENANT-001 |
| US-TENANT-003 | suspend with a reason; reactivate | SCR-TENANT-001 → SCR-SEC-001 |
| US-TENANT-009 | edit a tenant's names and profile | SCR-TENANT-001 |
| US-TENANT-010 | reset a tenant administrator's password | SCR-TENANT-001 → SCR-SEC-012 |
| US-TENANT-011 | read a tenant's usage | SCR-TENANT-001 |
| US-TENANT-012 | set or remove a tenant's logo and brand colour | SCR-TENANT-001 → SCR-SEC-010 |
| US-TENANT-013 | shell branding load | SCR-SEC-010 |
| US-TENANT-014 | sign-in page branding | SCR-SEC-001, SCR-SEC-002, SCR-SEC-003 |
| US-TENANT-015 | revoke a tenant's tokens | SCR-TENANT-001 → SCR-SEC-001 |
| US-TENANT-016 | export a tenant's data and download it | SCR-TENANT-001, SCR-FILE-002 |
══════════════════════════════════════════════════════════════════
