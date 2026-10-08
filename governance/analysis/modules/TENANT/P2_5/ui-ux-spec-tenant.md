# UI/UX SPEC — المستأجرون / Tenant (TENANT)
══════════════════════════════════════════════════════════════════
Module : TENANT   Version : v1   Profile : erp   Stage : P2.5 (UX design — frontend)
Sources: srs-tenant.md (Part B SCR-REQ-TENANT-001; 1.3.0 addenda B, E, C12, C4, C5 and their "Frontend impact"
         tables B11, E11, C11, I11, X12) · prd-tenant.md (US-TENANT-*) · ADR-TENANT-002, -003, -005, -006 ·
         the newfront `mxdashboard` 1.2.0 code as built (`src/modules/platform/features/tenants/**`, `src/app/layout/**`,
         `src/core/auth/**`, `src/core/tenant/**`)
Mints  : SCR-TENANT-001 (the platform tenants screen as built for 1.2.0 — the TENANT analysis numbers its screen
         requirement SCR-REQ-TENANT-001 and minted no SCR id, newback `docs/DEVIATIONS.md` [TM-A])
Scope  : SCR-TENANT-001 (`PLATFORM_TENANTS`) and the tenant branding the shell and the sign-in pages read; the shell
         itself is SEC's SCR-SEC-010 and the sign-in pages SCR-SEC-001 … 003 (`../../SEC/P2_5/ui-ux-spec-sec.md`) —
         their branding rows live here, with the branding they read (handover §0)
══════════════════════════════════════════════════════════════════

This file was created by the frontend step TM-FA (TENANT had no `P2_5` folder; plan §8 says so). The baseline
describes the screen as the frontend built it for erp-core 1.2.0; it adds no field, rule or permission the SRS does
not have. The addendum at the end is the first implemented section of this file.

## SCR-TENANT-001 — المستأجرون / Tenants
Traces            : REQ-TENANT-001, REQ-TENANT-002, REQ-TENANT-003, REQ-TENANT-004, REQ-TENANT-005, REQ-TENANT-006, REQ-TENANT-007, REQ-TENANT-008, REQ-TENANT-009, REQ-TENANT-015, US-TENANT-001, US-TENANT-002, US-TENANT-003
Screen requirement: SCR-REQ-TENANT-001 · page code `PLATFORM_TENANTS` (menu module PLATFORM)
Route             : `/platform/tenants` · create drawer `?action=create` · detail drawer `?tenantId=<id>` (`useDrawerUrlState('tenantId')`); query params, never segments
UI pattern        : search list + side drawers (create; detail with actions)
Fields shown      : list — code, Arabic name, English name, status badge, created; filters (filter drawer): code contains, name contains (active language), status · create — Disclosure "Tenant" (code upper-cased, `^[A-Z0-9_]{3,32}$`, names ar / en) and Disclosure "First administrator" (username, e-mail, password + confirm, full names ar / en) · detail — code, names, status, `AuditTrail` (on demand), the PLATFORM protected note and the link to platform defaults
Permissions       : SRS §B4 — `PLATFORM_TENANTS` VIEW (gateway / menu entry); every call needs `PLATFORM_TENANT_MANAGE` and a PLATFORM caller (REQ-TENANT-015)
Actions           : create (201 → toast `tenants.toastCreateSuccess`), suspend (ConfirmDialog `TenantSuspendConfirm` on the shared `DeactivateConfirm`; target in Zustand `useTenantStatusConfirmStore`), activate (no dialog) → toast `tenants.toastStatusChanged`; no edit, no delete (code immutable, names not editable in 1.2.0)
States            : list failed (not an empty list) · empty filtered / unfiltered · record unreadable · PLATFORM: no Suspend, protected note (RULE-TENANT-005)
Errors            : `TENANT_CODE_DUPLICATE` / `TENANT_CODE_INVALID` inline on code; `TENANT_PLATFORM_PROTECTED` banner; `VALIDATION_ERROR` inline on the named field

## Tenant in the shell and on the sign-in pages (1.2.0)
The shell (SCR-SEC-010) shows the session's tenant CODE as a badge (`topbar-tenant`) and the static platform mark;
the sign-in pages (SCR-SEC-001 … 003) carry the Tenant code field and the static mark. 1.2.0 had no tenant branding.

---

## Implementation Addendum — frontend 1.3.0
Source version : mxdashboard 1.3.0 (unreleased) against erp-core 1.3.0 (newback `f48b9ab`, `docs/api-docs/tenant/`, `docs/api-docs/file/`)
Change         : tenant-maturity plan §8 F2 (dynamic shell: tenant logo beside the platform mark, brand accent, sign-in page branding) and F3 (`PLATFORM_TENANTS`: edit, logo and brand colour, suspend with a reason, suspension facts, usage, admin-reset, revoke tokens, export, idempotent create); backend packages B, E, C12, C4, C5 (`../P1/srs-tenant.md` 1.3.0; `docs/steps/tm-frontend-handover.md` F2, F3)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Rows are numbered `TEN-Unn` for the step reports and `docs/test-e2e/front-test-plan.md` (newfront). No new page code,
permission, menu entry or screen exists for F2 / F3 (handover §0; D5): `PLATFORM_TENANTS` + `PLATFORM_TENANT_MANAGE`
cover every tenant action, and the branding reads need only a signed-in user (or none, on the sign-in pages).

### A. Shell branding — SCR-SEC-010 (plan §8 F2)
| # | Kind | Item | Detail |
|---|---|---|---|
| TEN-U01 | NEW | `<TenantLogo>` (`src/app/layout/TenantLogo.tsx`) | `<img src={logoUrl} alt={t('branding.tenantLogoAlt', {name})} data-testid="tenant-logo">`, height 28 px (shell) / 40 px (sign-in pages), `object-fit: contain`, `max-inline-size` 120 px; `onError` → the component renders nothing and reports it, so the caller shows the mark alone. The logo is ONLY ever an `<img>` — never `<object>`, `<embed>`, `<iframe>`, inline SVG, or fetched-and-injected markup (E7: an SVG logo is served as an attachment with a sandbox CSP) |
| TEN-U02 | CHANGED | Sidebar brand header, expanded | with a `logoUrl`: `[platform mark] · [tenant logo]` — the mark stays, a thin separator (`sidebar-brand-separator`), then `<TenantLogo>` in place of the "AVELYNQ" word-mark; without a `logoUrl` (null, query pending or failed, `onError`): **today's markup unchanged** (mark + word-mark) |
| TEN-U03 | CHANGED | Sidebar brand header, collapsed rail | the tenant logo alone when present (28 px, centred), else the mark (as today) |
| TEN-U04 | NEW | query `['tenant','branding', sessionTenantCode]` | `GET /api/v1/tenant/me` (`TenantBrandingResponse { code, nameAr, nameEn, logoUrl, brandColor, defaultLocale }`) **once at shell mount**: `staleTime: Infinity`, no refetch on focus, `retry: false`; also read by SCR-SEC-012 (it is exempt from the forced-change gate). The `logoUrl` used is always the one of the latest answer, never a remembered URL (a replaced or removed logo's old URL may be served from a cache for 24 h) |
| TEN-U05 | NEW | brand colour accent | when `brandColor` (`#RRGGBB`) is present: `document.documentElement.style.setProperty('--color-accent-tenant', brandColor)`; the sidebar ACTIVE entry (`ScreenMenuItem` indicator and active background tint) uses `var(--color-accent-tenant, var(--teal-400, #2dd4bf))`; nothing else is repainted in v1 (no buttons, links, topbar). Absent `brandColor` → the property is removed |
| TEN-U06 | NEW | cache lifetime | branding is tenant-scoped: the key carries the tenant code; sign-out (`queryClient.clear()`) drops it and removes `--color-accent-tenant`; a session of another tenant code reads its own entry (and `removeQueries(['tenant','branding'])` drops the others); when the PLATFORM operator saves or removes the logo / colour of the tenant the session belongs to (TEN-U51, TEN-U52 on the PLATFORM row), that entry is invalidated so the shell shows the change at once |
| TEN-U07 | NEW | errors of `/tenant/me` | 401 → the 1.2.0 rule (session cleared, `/login`); 401 `TENANT_TOKEN_REVOKED` → the same, plus one info toast `auth.errTenantTokenRevoked` (TEN-U08); 403 `TENANT_SUSPENDED` → the 1.2.0 `endRevokedSession` (session cleared, one toast); anything else (404, 5xx, network) → mark only, no toast |
| TEN-U08 | NEW | `TENANT_TOKEN_REVOKED` on ANY request (filter code, both realms) | the transport treats it as a 401 (session cleared → `/login`) and, once per burst, shows `auth.errTenantTokenRevoked` ("your organisation's sessions were ended"); a sign-in sent afterwards works (the login call never carries the stale token — 1.2.0 rule) |
| TEN-U09 | unchanged | topbar tenant badge | still the session tenant code (`topbar-tenant`); names come from branding only where shown |

### B. Sign-in page branding — SCR-SEC-001 / SCR-SEC-002 / SCR-SEC-003 (plan §8 F2)
| # | Kind | Item | Detail |
|---|---|---|---|
| TEN-U10 | NEW | when the public branding is asked | `usePublicTenantBranding(code)` (`src/core/tenant/`): the Tenant code field's value, trimmed + upper-cased, **400 ms after the last change** (debounce), and only when it matches `^[A-Z0-9_]{3,32}$`; a host-named tenant (`VITE_TENANT_HOST_SUFFIX`) or a pre-filled code is asked at once; query key `['tenant','publicBranding', code]`, `retry: false`, `staleTime` 5 minutes (a code typed again is not asked again) |
| TEN-U11 | NEW | what is shown | `<TenantLogo>` 40 px **above the form heading** in the form column (`auth-tenant-logo`); the brand aside keeps the static platform mark; no logo → nothing above the heading (the page as in 1.2.0) |
| TEN-U12 | NEW | 404 `TENANT_NOT_FOUND`, 403 `TENANT_SUSPENDED`, network / 5xx | **platform mark only, no error toast, no inline message** — the login submit produces the real tenant error on the Tenant field (unchanged 1.2.0 behaviour) |
| TEN-U13 | NEW | 429 `TENANT_BRANDING_RATE_LIMITED` | platform mark only, no toast; the `Retry-After` header (seconds) is honoured: `usePublicBrandingThrottleStore.blockedUntil = now + Retry-After` (60 s when the header is absent or unreadable); until then no public branding request is sent for any code; the next settled code after that is asked |
| TEN-U14 | NEW | transport of the public call | `/api/v1/public/tenants/` is a public prefix in `httpClient` like `/api/v1/public/files/`: no `Authorization`, no `X-Tenant-Code` (the tenant is the path), and its refusals never reach the session handlers (a 403 `TENANT_SUSPENDED` here is about the typed code, not about a held session) |
| TEN-U15 | NEW | cache on tenant change | each code has its own entry; changing the code shows the new code's logo or nothing — never the previous code's logo |

### C. SCR-TENANT-001 — screens, routes, drawers and URL state (plan §8 F3)
| # | Kind | Item | URL / state | Detail |
|---|---|---|---|---|
| TEN-U20 | CHANGED | detail drawer | `?tenantId=<id>` | sections (top → bottom): identity (code, names, status) · profile (TEN-U41) · suspension facts (TEN-U42, only when `suspendedAt` is set) · branding summary row (TEN-U43) · usage (TEN-U44, Disclosure) · the link "Exported archives" (`tenant-archives-link`, TEN-U59) · `AuditTrail` (unchanged) · actions (TEN-U50 …) |
| TEN-U21 | NEW | edit drawer | `?editId=<id>` (`useDrawerUrlState('editId')`) | opened by "Edit" in the detail drawer: the URL becomes `?editId=<id>` (the detail param is replaced — one first-level drawer at a time); Cancel / save return to `?tenantId=<id>`; fields TEN-U40; code read-only with `FieldLock` (locked skin) and `tenants.codeReadOnlyHint` |
| TEN-U22 | NEW | logo / branding drawer, second level | `?tenantId=<id>&logoFor=<id>` | opened from the branding summary row (`tenant-branding-change`); a sibling drawer above the detail drawer: current logo (`<img>` of `logoUrl`) or "Platform mark only", constraints text, file input + preview, brand colour, "Save", "Remove logo"; closing removes `logoFor` only; a `logoFor` without the matching `tenantId` is dropped; a saving level-2 drawer — Decision SEC P2_5 SEC-U78 (citation added by TM-F3, TEN-U131) |
| TEN-U23 | NEW | admin-reset drawer, second level | `?tenantId=<id>&adminResetFor=<id>` | opened by "Reset administrator password" in the detail drawer; **never offered on the PLATFORM row** (the action is hidden; a deep link with the PLATFORM id is dropped from the URL); fields TEN-U45; Submit opens a final ConfirmDialog (TEN-U53); a saving level-2 drawer — Decision SEC P2_5 SEC-U78 (citation added by TM-F3, TEN-U131) |
| TEN-U24 | CHANGED | suspend confirmation | Zustand `useTenantStatusConfirmStore` (no route, as in 1.2.0) | the ConfirmDialog gains a mandatory reason (TEN-U46) — a confirmation with a text field is still a sensitive-action confirmation |
| TEN-U25 | NEW | revoke-tokens and export confirmations | Zustand `useTenantActionConfirmStore` `{ kind: 'revokeTokens' | 'export', tenant }` (no route, like the suspend and module-revoke confirmations) | ConfirmDialogs TEN-U54, TEN-U55 |
| TEN-U26 | NEW | create drawer — idempotency | `?action=create` (unchanged URL) | an `Idempotency-Key` (`crypto.randomUUID()`) is minted when the create drawer opens and kept in the drawer's Zustand store; every retry of the SAME body sends the same key; when any field changes after a failed attempt a new key is minted before the next submit (the same key with another body answers 409); a 201 with `Idempotent-Replayed: true` is a success like any 201 (same toast, same close) |
| TEN-U27 | CHANGED | filters | filter drawer | + "Contact e-mail contains" (`contactEmail`, text filter) and "Country code" (`countryCode`, `EQUALS`, upper-cased); the sort whitelist gains `contactEmail`, `countryCode`, `suspendedAt` (`modules/platform/shared/api/searchWhitelist.ts`) to mirror the contract — they are SORT-ONLY fields with no list column and no sort control in v1 (the list keeps its 1.2.0 columns; Decision TEN-U123); `suspendedAt` is shown in the detail, not filtered (Decision TEN-U79) |

### D. SCR-TENANT-001 — fields
| # | Kind | Where | Field | Control and rule | Read-only |
|---|---|---|---|---|---|
| TEN-U40 | NEW | edit drawer | `code` (locked) · `nameAr`*, `nameEn`* (max 200) · `contactEmail` (e-mail, max 255) · `contactPhone` (pattern `^$|^\+?[0-9][0-9 -]{5,28}[0-9]$`, max 30) · `countryCode` (2 letters, upper-cased as typed) · `defaultLocale` (Select: not set / العربية / English) · `timezone` (IANA id, max 64, hint) · `notes` (Textarea, max 1000) | `PUT /{id}` is a FULL replacement: every editable field is sent, an emptied optional field as `""` (stored as null); no `code`, no `statusCode` in the body | code only |
| TEN-U41 | NEW | detail drawer | contact e-mail, contact phone, country code, default language, time zone, notes | `Field` rows; absent → `—` | yes |
| TEN-U42 | NEW | detail drawer | `suspendedAt` (`formatDateTime`), `suspendedBy`, `suspensionReason` | section "Suspension" (`tenant-suspension-facts`), shown only while the facts are set (they are null while ACTIVE; an activation clears them) | yes |
| TEN-U43 | NEW | detail drawer | branding summary | logo thumbnail (`<img>` 32 px of `logoUrl`, `tenant-branding-logo`) or the text "Platform mark only"; colour swatch + `brandColor` hex or "No brand colour"; button `tenant-branding-change`. The PLATFORM row may carry a logo too; the platform MARK is a static asset, never this logo | yes |
| TEN-U44 | NEW | detail drawer | usage (`Disclosure` `tenant-usage`, **closed by default**) | opening it calls `GET /{id}/usage` (counted live on each call — no cache across openings, `staleTime: 0`); figures: staff users, customer accounts, open sessions, documents, storage used (`formatBytes`), notifications in the last 30 days, "Counted at" (`collectedAt`, `formatDateTime`); Refresh button; failure → `tenants.usageFailed` inside the section only | yes |
| TEN-U45 | NEW | admin-reset drawer | `username`* (max 100), `newPassword`* + confirm, `requireChangeAtNextLogin` (Checkbox, **default on**) | policy hint `account.passwordPolicyHint` + byte counter (SEC P2_5 SEC-U34); no client 200 cap | no |
| TEN-U46 | NEW | suspend ConfirmDialog | `reason`* | `Textarea` (`tenant-suspend-reason`), 3 … 500 characters after trimming, live counter; the Confirm button stays disabled until valid; hint `tenants.suspendReasonHint` | no |
| TEN-U47 | NEW | logo drawer | `file` · `brandColor` | file input `accept="image/png,image/jpeg,image/webp,image/svg+xml"` (client pre-check ≤ 1 048 576 bytes as a hint only), preview of the chosen file through an object URL **in `<img>`**; brand colour: text `#RRGGBB` (`tenant-brand-color`) beside a native colour picker, blank = clear | no |
| TEN-U48 | CHANGED | create drawer | `adminPassword` | hint becomes the password policy (`tenants.fieldAdminPasswordHint` CHANGED) + byte counter; the server's `SEC-400-PASSWORD-POLICY` (field `adminPassword`) is shown inline | no |

### E. SCR-TENANT-001 — actions (all `PLATFORM_TENANT_MANAGE`, PLATFORM caller; gated like the 1.2.0 create / suspend actions)
| # | Kind | Action (`data-testid`) | Call | Confirm | Result |
|---|---|---|---|---|---|
| TEN-U50 | NEW | Edit (`tenant-action-edit`) | `PUT /api/v1/platform/tenants/{id}` | — | toast `tenants.toastUpdateSuccess`; back to `?tenantId=`; list + detail re-read |
| TEN-U51 | NEW | Save branding (`tenant-branding-save`) | when a file is chosen `PUT /api/v1/platform/tenants/{id}/logo` (multipart `file`), then — when the colour changed — `PATCH /api/v1/platform/tenants/{id}/branding` `{ brandColor }` (an ordered pair: the colour is not sent when the logo was refused) | — | toast `tenants.toastLogoSaved` and / or `tenants.toastBrandColorSaved`; the preview switches to the `logoUrl` of the RETURNED `TenantResponse` |
| TEN-U52 | NEW | Remove logo (`tenant-logo-remove`, only when `logoUrl` is set) | `DELETE /api/v1/platform/tenants/{id}/logo` (204, idempotent) | ConfirmDialog `tenants.confirmRemoveLogoTitle` (sensitive) | toast `tenants.toastLogoRemoved`; "Platform mark only" |
| TEN-U53 | NEW | Reset administrator password (`tenant-action-admin-reset`; hidden on PLATFORM) | `POST /api/v1/platform/tenants/{id}/admin-reset` | the drawer's Submit opens ConfirmDialog `tenants.confirmAdminResetTitle` naming the username and the tenant | toast `tenants.toastAdminReset` {username, sessionsTerminated}; the target must change the password at the next sign-in by default (SEC P2_5 forced-change flow) |
| TEN-U54 | NEW | Sign every user out (`tenant-action-revoke-tokens`; hidden on PLATFORM) | `POST /api/v1/platform/tenants/{id}/revoke-tokens` (no body) | ConfirmDialog `tenants.confirmRevokeTokensTitle` | toast `tenants.toastTokensRevoked` {code, sessionsTerminated}; on 500 `TENANT_REVOKE_SESSIONS_FAILED` the dialog stays open with the message and a "Try again" button (`tenant-revoke-retry`) that repeats the call (the tokens are already refused) |
| TEN-U55 | NEW | Export data (`tenant-action-export`; every row, PLATFORM and suspended tenants included) | `POST /api/v1/platform/tenants/{id}/export` (no body; request timeout raised above the 30 s default — 10 minutes — because it is synchronous) then at once `GET /api/v1/files/download?token={downloadToken}` through the FILE module's `fetchFileDownload` (not `httpClient`, so its 401 `FILE_ACCESS_TOKEN_INVALID` never ends the session), saved as `fileName` | ConfirmDialog `tenants.confirmExportTitle`; while running it shows an indeterminate progress bar and the elapsed seconds, its Cancel is disabled, and nothing is retried automatically | the dialog shows `fileName`, `rowCount`, `sizeBytes` (`formatBytes`), the re-download hint and the link `tenant-export-open-files` → `/files/browser?moduleCode=TENANT&ownerType=CORE_TENANT&ownerId=<tenantId>&fileId=<fileId>` (TEN-U59); toast `tenants.toastExportReady`; results shown inside the ConfirmDialog — Decision TEN-U124 |
| TEN-U56 | CHANGED | Suspend (`tenant-action-suspend`) | `PATCH /api/v1/platform/tenants/{id}/status` `{ statusCode: 'SUSPENDED', reason }` | the ConfirmDialog with the reason (TEN-U46); effect text CHANGED (`tenants.confirmSuspendEffect`) | toast `tenants.toastStatusChanged`; the detail shows the suspension facts |
| TEN-U57 | CHANGED | Activate (`tenant-action-activate`) | `PATCH …/status` `{ statusCode: 'ACTIVE' }` (no reason) | none (as before) | toast as before; facts cleared; every earlier token of the tenant now answers 401 `TENANT_TOKEN_REVOKED` — its users sign in again (TEN-U08) |
| TEN-U58 | CHANGED | Create (`POST /api/v1/platform/tenants`) | + header `Idempotency-Key` (TEN-U26) | — | as before; a replay is a success |
| TEN-U59 | NEW | Re-download an archive later (`tenant-archives-link` in the detail drawer; `tenant-export-open-files` in the export result) | a DEEP LINK into the FILE browser SCR-FILE-002: `/files/browser?moduleCode=TENANT&ownerType=CORE_TENANT&ownerId=<tenantId>` (the result link adds `&fileId=<fileId>`, which opens the archive's metadata drawer) → Download (`POST /api/v1/files/{id}/access-token` → download). The FILE browser's scope Select lists only SEC module-registry rows, which have no `TENANT` row, so picking the scope by hand cannot reach the archive; `useFileBrowserUrlState` already reads any `moduleCode` / `ownerType` / `ownerId` / `fileId` from the URL (checked in mxdashboard 1.2.0), so the link works as is. FILE needs one small change, recorded in `../../FILE/P2_5/ui-ux-spec.md` addendum FILE-U01: the scope drawer keeps a deep-linked `moduleCode` that is not a registry row as an extra option, so the scope stays editable | — | the archive is RESTRICTED: a user without `PLATFORM_TENANT_MANAGE` never sees it (the owner list leaves it out; `GET /files/{id}` answers 404 `FILE_DOCUMENT_NOT_FOUND`, shown by the FILE browser's existing not-found handling); deleting it (`DELETE /api/v1/files/{id}`) removes its bytes and keeps a `DELETED` tombstone |

### F. Errors and messages (SCR-TENANT-001; the server message is localized by `Accept-Language`)
| # | Kind | Code | Where | Text |
|---|---|---|---|---|
| TEN-U60 | NEW | 400 `TENANT_SUSPENSION_REASON_REQUIRED` | inline on the reason | `tenants.errSuspensionReasonRequired` (the client already blocks < 3 / > 500) |
| TEN-U61 | NEW | 404 `TENANT_ADMIN_NOT_FOUND`, 422 `TENANT_ADMIN_NOT_SUPER` | inline on `username` | `tenants.errAdminNotFound`, `tenants.errAdminNotSuper` |
| TEN-U62 | NEW | 422 `TENANT_ADMIN_RESET_PLATFORM` | banner (defensive: the action is hidden on PLATFORM) | `tenants.errAdminResetPlatform` |
| TEN-U63 | NEW | 400 `SEC-400-PASSWORD-POLICY` (admin-reset `newPassword`, create `adminPassword`) | inline on the named field | the server message; fallback `account.errPasswordPolicy` |
| TEN-U64 | NEW | 400 `TENANT_LOGO_INVALID` (field `file`); 400 `VALIDATION_ERROR` (no part) | inline on the file input | the server message (it names the plain-SVG rule in both languages); fallback `tenants.errLogoInvalid`; the current logo stays |
| TEN-U65 | NEW | 400 `TENANT_BRAND_COLOR_INVALID` (field `brandColor`) | inline on the colour | `tenants.errBrandColorInvalid` |
| TEN-U66 | NEW | 422 `TENANT_REVOKE_TOKENS_PLATFORM` | dialog message (defensive) | `tenants.errRevokeTokensPlatform` |
| TEN-U67 | NEW | 500 `TENANT_REVOKE_SESSIONS_FAILED` | dialog message + Try again | `tenants.errRevokeSessionsFailed` |
| TEN-U68 | NEW | 409 `TENANT_EXPORT_IN_PROGRESS`, 422 `TENANT_EXPORT_TOO_LARGE`, 429 `TENANT_EXPORT_BUSY` (no `Retry-After`) | export dialog message | `tenants.errExportInProgress`, the server message for TOO_LARGE (it carries the row count and the limit; fallback `tenants.errExportTooLarge`), `tenants.errExportBusy` ("try again shortly") |
| TEN-U69 | NEW | download failure after a successful export (e.g. 401 `FILE_ACCESS_TOKEN_INVALID`) | export dialog message | `tenants.errExportDownload` — the archive exists; get it from the file browser (TEN-U59); the session is untouched |
| TEN-U70 | NEW | 400 `IDEMPOTENCY_KEY_INVALID`, 409 `IDEMPOTENCY_KEY_CONFLICT` | create drawer banner; on CONFLICT a new key is minted | `tenants.errIdempotencyInvalid`, `tenants.errIdempotencyConflict` |
| TEN-U71 | CHANGED | 404 `TENANT_NOT_FOUND`, 409 `CONCURRENT_MODIFICATION`, 400 `VALIDATION_ERROR` on the new calls | the 1.2.0 rules: record unreadable / reload toast / inline on the named field | existing keys |
| TEN-U72 | NEW | `TENANT_TOKEN_REVOKED` (401, any request) | session end + toast | `auth.errTenantTokenRevoked` (TEN-U08) |
| TEN-U73 | NEW | `TENANT_BRANDING_RATE_LIMITED` (429, public branding) | nothing visible by design (TEN-U13) | — |

### G. Toasts (success, both languages)
| # | Kind | Key | en | ar |
|---|---|---|---|---|
| TEN-U80 | NEW | `tenants.toastUpdateSuccess` | Tenant {code} has been updated. | تم تحديث المستأجر {code}. |
| TEN-U81 | NEW | `tenants.toastLogoSaved` | The logo has been saved. | تم حفظ الشعار. |
| TEN-U82 | NEW | `tenants.toastLogoRemoved` | The logo has been removed. | تمت إزالة الشعار. |
| TEN-U83 | NEW | `tenants.toastBrandColorSaved` | The brand colour has been saved. | تم حفظ لون العلامة. |
| TEN-U84 | NEW | `tenants.toastAdminReset` | The password of {username} has been reset. {count} session(s) ended. | أُعيد تعيين كلمة مرور {username}. أُنهيت {count} من الجلسات. |
| TEN-U85 | NEW | `tenants.toastTokensRevoked` | Every user of {code} has been signed out. {count} session(s) ended. | تم إخراج جميع مستخدمي {code}. أُنهيت {count} من الجلسات. |
| TEN-U86 | NEW | `tenants.toastExportReady` | The export of {code} has been downloaded. | تم تنزيل تصدير {code}. |
| TEN-U87 | unchanged | `tenants.toastCreateSuccess`, `tenants.toastStatusChanged` | — | — |

### H. i18n keys (both dictionaries)
| # | Kind | Key | en | ar |
|---|---|---|---|---|
| TEN-U90 | NEW | `branding.tenantLogoAlt` | {name} logo | شعار {name} |
| | NEW | `auth.errTenantTokenRevoked` | Your organisation's sessions were ended. Please sign in again. | أُنهيت جلسات منشأتك. يرجى تسجيل الدخول مجددًا. |
| | NEW | `tenants.actionEdit` | Edit | تعديل |
| | NEW | `tenants.drawerTitleEdit` | Edit tenant {code} | تعديل المستأجر {code} |
| | NEW | `tenants.codeReadOnlyHint` | The tenant code cannot be changed. | لا يمكن تغيير رمز المستأجر. |
| | NEW | `tenants.sectionProfile` | Profile | الملف |
| | NEW | `tenants.fieldContactEmail` | Contact e-mail | بريد التواصل |
| | NEW | `tenants.fieldContactPhone` | Contact phone | هاتف التواصل |
| | NEW | `tenants.fieldCountryCode` | Country code | رمز الدولة |
| | NEW | `tenants.fieldCountryCodeHint` | Two letters, ISO 3166-1 (e.g. SA). | حرفان وفق ISO 3166-1 (مثل SA). |
| | NEW | `tenants.fieldDefaultLocale` | Default language | اللغة الافتراضية |
| | NEW | `tenants.fieldTimezone` | Time zone | المنطقة الزمنية |
| | NEW | `tenants.fieldTimezoneHint` | IANA id, e.g. Asia/Riyadh. | معرّف IANA، مثل Asia/Riyadh. |
| | NEW | `tenants.fieldNotes` | Notes | ملاحظات |
| | NEW | `tenants.filterContactEmail` | Contact e-mail contains | بريد التواصل يحتوي |
| | NEW | `tenants.filterCountryCode` | Country code | رمز الدولة |
| | NEW | `tenants.sectionSuspension` | Suspension | التعليق |
| | NEW | `tenants.fieldSuspendedAt` | Suspended at | تاريخ التعليق |
| | NEW | `tenants.fieldSuspendedBy` | Suspended by | علّقه |
| | NEW | `tenants.fieldSuspensionReason` | Reason | السبب |
| | NEW | `tenants.suspendReasonLabel` | Reason for the suspension | سبب التعليق |
| | NEW | `tenants.suspendReasonHint` | Required, 3 to 500 characters. Recorded with the suspension. | مطلوب، من 3 إلى 500 حرف. يُسجَّل مع التعليق. |
| | NEW | `tenants.errSuspensionReasonRequired` | A suspension needs a reason of 3 to 500 characters. | يتطلب تعليق المستأجر سببًا من 3 إلى 500 حرف. |
| | CHANGED | `tenants.confirmSuspendEffect` | All of its users are signed out at once and cannot sign in until it is activated again; after the activation they sign in again. | يُخرَج جميع مستخدميه فورًا ولا يمكنهم الدخول حتى يُعاد تفعيله، وبعد التفعيل يسجّلون الدخول من جديد. |
| | CHANGED | `tenants.fieldAdminPasswordHint` | At least 8 characters with a letter and a digit, at most 72 bytes. Hand it to the administrator securely. | 8 أحرف على الأقل تتضمن حرفًا ورقمًا، وبحد أقصى 72 بايت. سلّمها للمدير بطريقة آمنة. |
| | NEW | `tenants.sectionBranding` | Branding | العلامة التجارية |
| | NEW | `tenants.brandingMarkOnly` | Platform mark only | علامة المنصة فقط |
| | NEW | `tenants.brandColorNone` | No brand colour | بلا لون علامة |
| | NEW | `tenants.actionChangeBranding` | Change branding | تغيير العلامة |
| | NEW | `tenants.logoDrawerTitle` | Branding of {code} | علامة {code} |
| | NEW | `tenants.logoDrawerSubtitle` | The logo shown beside the platform mark, and an optional accent colour | الشعار المعروض بجانب علامة المنصة، ولون تمييز اختياري |
| | NEW | `tenants.logoCurrent` | Current logo | الشعار الحالي |
| | NEW | `tenants.logoPreview` | Preview | معاينة |
| | NEW | `tenants.fieldLogoFile` | Logo file | ملف الشعار |
| | NEW | `tenants.logoConstraints` | PNG, JPEG, WebP or plain SVG, at most 1 MB. PNG or WebP recommended; an SVG must be plain or optimised (no scripts, external references or editor metadata). | PNG أو JPEG أو WebP أو SVG بسيطة، بحجم لا يتجاوز 1 ميغابايت. يُنصح بـ PNG أو WebP؛ ويجب أن تكون SVG بسيطة أو محسّنة (دون نصوص برمجية أو مراجع خارجية أو بيانات محرّر). |
| | NEW | `tenants.fieldBrandColor` | Brand colour | لون العلامة |
| | NEW | `tenants.fieldBrandColorHint` | #RRGGBB, used for the active menu entry. Leave empty for none. | بالصيغة #RRGGBB، ويُستخدم لعنصر القائمة النشط. اتركه فارغًا لعدم استخدامه. |
| | NEW | `tenants.actionRemoveLogo` | Remove logo | إزالة الشعار |
| | NEW | `tenants.confirmRemoveLogoTitle` | Remove the logo? | إزالة الشعار؟ |
| | NEW | `tenants.confirmRemoveLogo` | The logo of {code} will be removed; its users see the platform mark only. | سيُزال شعار {code}، ولن يرى مستخدموه إلا علامة المنصة. |
| | NEW | `tenants.errLogoInvalid` | The logo must be a PNG, JPEG, WebP or plain SVG image of at most 1 MB. | يجب أن يكون الشعار صورة PNG أو JPEG أو WebP أو SVG بسيطة بحجم لا يتجاوز 1 ميغابايت. |
| | NEW | `tenants.errBrandColorInvalid` | Use #RRGGBB (six hexadecimal digits). | استخدم الصيغة #RRGGBB (ستة أرقام ست عشرية). |
| | NEW | `tenants.sectionUsage` | Usage | الاستخدام |
| | NEW | `tenants.usageStaffUsers` | Staff users | مستخدمو الموظفين |
| | NEW | `tenants.usageCustomerUsers` | Customer accounts | حسابات العملاء |
| | NEW | `tenants.usageActiveSessions` | Open sessions | الجلسات المفتوحة |
| | NEW | `tenants.usageFileDocuments` | Documents | المستندات |
| | NEW | `tenants.usageFileBytes` | Storage used | المساحة المستخدمة |
| | NEW | `tenants.usageNotifications` | Notifications, last 30 days | الإشعارات خلال آخر 30 يومًا |
| | NEW | `tenants.usageCollectedAt` | Counted at {time} | حُسبت في {time} |
| | NEW | `tenants.usageFailed` | The usage figures could not be read. | تعذّرت قراءة أرقام الاستخدام. |
| | NEW | `tenants.actionAdminReset` | Reset administrator password | إعادة تعيين كلمة مرور المدير |
| | NEW | `tenants.adminResetDrawerTitle` | Reset an administrator's password — {code} | إعادة تعيين كلمة مرور مدير — {code} |
| | NEW | `tenants.adminResetDrawerSubtitle` | For a user of this tenant holding a super role. Every session of that user ends. | لمستخدم في هذا المستأجر يملك دورًا فائقًا. تنتهي كل جلسات ذلك المستخدم. |
| | NEW | `tenants.confirmAdminResetTitle` | Reset this password? | إعادة تعيين كلمة المرور هذه؟ |
| | NEW | `tenants.confirmAdminReset` | The password of {username} in {code} is replaced and every session of that user ends. | ستُستبدل كلمة مرور {username} في {code} وتنتهي كل جلسات ذلك المستخدم. |
| | NEW | `tenants.errAdminNotFound` | No staff user of this tenant has this username. | لا يوجد مستخدم موظف بهذا الاسم في هذا المستأجر. |
| | NEW | `tenants.errAdminNotSuper` | This user holds no super role; only a tenant administrator can be reset here. | لا يملك هذا المستخدم دورًا فائقًا؛ لا يُعاد هنا إلا تعيين كلمة مرور مدير المستأجر. |
| | NEW | `tenants.errAdminResetPlatform` | The platform tenant's administrators change passwords in Users. | يغيّر مديرو مستأجر المنصة كلمات المرور من شاشة المستخدمين. |
| | NEW | `tenants.actionRevokeTokens` | Sign every user out | إخراج جميع المستخدمين |
| | NEW | `tenants.confirmRevokeTokensTitle` | Sign every user of this tenant out? | إخراج جميع مستخدمي هذا المستأجر؟ |
| | NEW | `tenants.confirmRevokeTokens` | Every sign-in of {code} ends at once, staff and customers; they sign in again. | تنتهي فورًا كل جلسات دخول {code}، للموظفين والعملاء، ويسجّلون الدخول من جديد. |
| | NEW | `tenants.errRevokeTokensPlatform` | The platform tenant's tokens cannot be revoked. | لا يمكن إبطال رموز مستأجر المنصة. |
| | NEW | `tenants.errRevokeSessionsFailed` | The old sign-ins are already refused, but ending the open sessions failed. Try again. | رُفضت جلسات الدخول القديمة، لكن تعذّر إنهاء الجلسات المفتوحة. أعد المحاولة. |
| | NEW | `tenants.actionExport` | Export data | تصدير البيانات |
| | NEW | `tenants.confirmExportTitle` | Export the data of {code}? | تصدير بيانات {code}؟ |
| | NEW | `tenants.confirmExport` | One ZIP archive of every module's data (CSV), without passwords, tokens or file contents. A large tenant takes tens of seconds; keep this window open. | أرشيف ZIP واحد لبيانات كل الوحدات (CSV)، دون كلمات مرور أو رموز أو محتوى ملفات. قد يستغرق المستأجر الكبير عشرات الثواني؛ أبقِ هذه النافذة مفتوحة. |
| | NEW | `tenants.exportRunning` | Exporting… {seconds} s | جارٍ التصدير… {seconds} ث |
| | NEW | `tenants.exportDone` | {fileName} — {rows} rows, {size} | {fileName} — {rows} سجل، {size} |
| | NEW | `tenants.exportRedownloadHint` | The archive stays in the file browser; open it from here to download it again. | يبقى الأرشيف في مستعرض الملفات؛ افتحه من هنا لتنزيله مجددًا. |
| | NEW | `tenants.actionOpenArchives` | Exported archives | الأرشيفات المصدَّرة |
| | NEW | `tenants.actionOpenArchive` | Open in the file browser | فتح في مستعرض الملفات |
| | NEW | `tenants.errExportTooLarge` | This tenant is too large to export in one archive. | هذا المستأجر أكبر من أن يُصدَّر في أرشيف واحد. |
| | NEW | `tenants.errExportInProgress` | An export of this tenant is already running. | يجري الآن تصدير لهذا المستأجر. |
| | NEW | `tenants.errExportBusy` | Too many exports are running. Try again shortly. | تجري الآن عمليات تصدير كثيرة. أعد المحاولة بعد قليل. |
| | NEW | `tenants.errExportDownload` | The archive was created but the download failed; get it from the file browser. | أُنشئ الأرشيف لكن تعذّر تنزيله؛ نزّله من مستعرض الملفات. |
| | NEW | `tenants.errIdempotencyInvalid` | The request could not be identified. Reopen the form and try again. | تعذّر تعريف الطلب. أعد فتح النموذج وحاول مجددًا. |
| | NEW | `tenants.errIdempotencyConflict` | This submission was already used with other values. Submit again to send the new values. | استُخدم هذا الإرسال من قبل بقيم أخرى. أرسل مجددًا لإرسال القيم الجديدة. |
Every NEW key is also declared in `core/i18n/i18n.types.ts`. Plus the seven toast keys of §G. Reused: `users.fieldNewPassword`, `users.fieldConfirmPassword`,
`users.fieldRequireChange`, `users.localeNone` / `users.localeAr` / `users.localeEn`, `account.passwordPolicyHint`,
`account.passwordBytes`, `account.errPasswordPolicy` (SEC P2_5), `tenants.fieldAdminUsername`, `common.*`.

### I. Endpoints (method + path exactly as `docs/api-docs/tenant/endpoints/` and `docs/api-docs/file/endpoints/`)
| # | Kind | Method | Path | File | Used by |
|---|---|---|---|---|---|
| TEN-U100 | NEW | GET | `/api/v1/tenant/me` | `tenant-branding.md` | TEN-U04 (shell, SCR-SEC-012) |
| TEN-U101 | NEW | GET | `/api/v1/public/tenants/{tenantCode}/branding` | `tenant-branding.md` | TEN-U10 (sign-in pages) |
| TEN-U102 | NEW | PUT | `/api/v1/platform/tenants/{id}` | `platform-tenants.md` | TEN-U50 |
| TEN-U103 | NEW | PUT | `/api/v1/platform/tenants/{id}/logo` | `platform-tenants.md` | TEN-U51 |
| TEN-U104 | NEW | DELETE | `/api/v1/platform/tenants/{id}/logo` | `platform-tenants.md` | TEN-U52 |
| TEN-U105 | NEW | PATCH | `/api/v1/platform/tenants/{id}/branding` | `platform-tenants.md` | TEN-U51 |
| TEN-U106 | NEW | GET | `/api/v1/platform/tenants/{id}/usage` | `platform-tenants.md` | TEN-U44 |
| TEN-U107 | NEW | POST | `/api/v1/platform/tenants/{id}/admin-reset` | `platform-tenants.md` | TEN-U53 |
| TEN-U108 | NEW | POST | `/api/v1/platform/tenants/{id}/revoke-tokens` | `platform-tenants.md` | TEN-U54 |
| TEN-U109 | NEW | POST | `/api/v1/platform/tenants/{id}/export` | `platform-tenants.md` | TEN-U55 |
| TEN-U110 | CHANGED | PATCH | `/api/v1/platform/tenants/{id}/status` | `platform-tenants.md` | TEN-U56 (+ `reason`), TEN-U57 |
| TEN-U111 | CHANGED | POST | `/api/v1/platform/tenants` | `platform-tenants.md` | TEN-U58 (+ `Idempotency-Key`; response header `Idempotent-Replayed`) |
| TEN-U112 | CHANGED | GET | `/api/v1/platform/tenants/{id}` | `platform-tenants.md` | TEN-U20 (profile, suspension facts, `logoUrl`, `brandColor`) |
| TEN-U113 | CHANGED | POST | `/api/v1/platform/tenants/search` | `platform-tenants.md` | TEN-U27 (+ filters / sorts) |
| TEN-U114 | unchanged | GET | `/api/v1/platform/tenants` | `platform-tenants.md` | — (the screen uses the search) |
| TEN-U115 | CONSUMED | GET | `/api/v1/files/download` (`?token=`) | `../../file/endpoints/file-documents.md` | TEN-U55 |
| TEN-U116 | CONSUMED | POST | `/api/v1/files/{id}/access-token` | `../../file/endpoints/file-documents.md` | TEN-U59 (FILE browser, through the deep link) |
| TEN-U117 | CONSUMED | GET | `/api/v1/files` | `../../file/endpoints/file-documents.md` | TEN-U59 (FILE browser owner list, through the deep link) |
`logoUrl` (`/api/v1/public/files/{tenantCode}/{publicSlug}`, `public-files.md`) is only ever the `src` of an `<img>`; the
frontend never requests it itself.

### J. Decisions
| # | Kind | Decision | Alternatives | Choice and why |
|---|---|---|---|---|
| TEN-U74 | NEW (Decision — plan §0 / §8 F2, handover §0) | tenant logo **beside** the platform mark | (a) the tenant logo replaces the mark; (b) beside it | **(b)** `[platform mark] · [tenant logo]`: the platform stays recognisable and a tenant without a logo always has a fallback (RULE-TENANT-019); the word-mark gives way to the logo for width; in the collapsed rail only one image fits, so the logo wins there when present |
| TEN-U75 | NEW (Decision — plan §8 F3, handover §0) | logo upload in the tenant detail drawer as a **second-level drawer** | (a) inside the edit drawer; (b) a branding summary row in the detail drawer opening a second-level drawer `?logoFor=` | **(b)**: one drawer, one job — the edit drawer is a `PUT` of names and profile, the logo is a multipart upload with its own preview and its own removal; ADR-TENANT-005 keeps branding with the platform operator, so it lives on `PLATFORM_TENANTS` |
| TEN-U76 | NEW (Decision — handover §0, SEC P2_5 SEC-U70) | authentication-only routes for `/account/*` | — | recorded in SEC P2_5 SEC-U70; cited here because the forced-change page (SCR-SEC-012) shows the tenant branding |
| TEN-U77 | NEW (Decision) | the edit drawer's URL | (a) a second level over the detail `?tenantId=&editId=`; (b) a first-level drawer `?editId=` replacing the detail | **(b)** as the plan names it: edit and detail never show together; Cancel returns to the detail |
| TEN-U78 | NEW (Decision) | logo and colour in one drawer | (a) two forms with two buttons; (b) one Save running the changed parts in order | **(b)**: one submit per open form; the logo goes first (a refused image stops the colour, so nothing half-applies silently) |
| TEN-U79 | NEW (Decision) | `suspendedAt` as a filter | (a) a date-range filter; (b) shown in the detail and sortable only | **(b)** for v1: the plan's F3 rows name no such filter; the status filter already isolates suspended tenants |
| TEN-U120 | NEW (Decision) | the export dialog while running | (a) closable, the request continues; (b) Cancel disabled until the answer | **(b)**: the request is synchronous and must not be retried automatically; closing would lose the single-use token of the immediate download |
| TEN-U121 | NEW (Decision) | `TENANT_BRANDING_RATE_LIMITED` text | (a) an inline note; (b) nothing visible | **(b)** as the handover says (mark only, no toast); the code has no UI key |
| TEN-U122 | NEW (note) | CORS for `Idempotency-Key` / `Idempotent-Replayed` | — | none needed: the frontend is served same-origin through the proxy (1.2.0 deployment rule, core configures no CORS); a cross-origin deployment must allow the request header and expose the response header (handover F3) |
| TEN-U123 | NEW (note) | sort-only fields | (a) add list columns for `contactEmail`, `countryCode`, `suspendedAt`; (b) whitelist them for sort only | **(b)** in v1: the plan's F3 rows add no list column; the whitelist mirrors the contract (TEN-U27) so a later column needs no mapper change; no sort control offers them now |
| TEN-U124 | NEW (Decision) | where the results of revoke-tokens and export are shown | (a) a new result drawer; (b) inside the ConfirmDialog that started the action (outcome state), plus the toast | **(b)**, the `ModuleRevokeConfirm` precedent (SEC-005 shows `revokedScreenGrants` / `revokedActionGrants` in its dialog): the result is the outcome of the confirmed action — read-only facts (file name, rows, size, session count, retry) and the archive link — not a form, so it does not break the "no modal for forms" mandate; the dialog closes with Close |

### K. data-testid (new; used by the `TC-FE-*` cases)
`tenant-logo`, `sidebar-brand-separator`, `sidebar-brand-mark`, `auth-tenant-logo`, `tenant-action-edit`,
`tenant-edit-drawer`, `tenant-edit-code`, `tenant-edit-contactEmail`, `tenant-edit-contactPhone`,
`tenant-edit-countryCode`, `tenant-edit-defaultLocale`, `tenant-edit-timezone`, `tenant-edit-notes`, `tenant-edit-submit`,
`tenant-suspension-facts`, `tenant-branding-row`, `tenant-branding-logo`, `tenant-branding-change`,
`tenant-branding-drawer`, `tenant-logo-file`, `tenant-logo-preview`, `tenant-brand-color`, `tenant-branding-save`,
`tenant-logo-remove`, `tenant-usage`, `tenant-usage-refresh`, `tenant-action-admin-reset`, `tenant-admin-reset-drawer`,
`tenant-admin-reset-username`, `tenant-admin-reset-password`, `tenant-admin-reset-confirm-password`,
`tenant-admin-reset-require-change`, `tenant-admin-reset-submit`, `tenant-suspend-reason`,
`tenant-action-revoke-tokens`, `tenant-revoke-retry`, `tenant-action-export`, `tenant-export-progress`,
`tenant-export-result`, `tenant-export-open-files`, `tenant-archives-link`, `tenants-filter-contactEmail`, `tenants-filter-countryCode`.

### L. Traceability
| Rows | Serves | Test cases (newfront `docs/test-e2e/front-test-plan.md`) |
|---|---|---|
| TEN-U01, U02, U04, U74 | REQ-TENANT-031, RULE-TENANT-019 | TC-FE-XCUT-015 |
| TEN-U01, U02 (fallback) | REQ-TENANT-031 | TC-FE-XCUT-016 |
| TEN-U03 | REQ-TENANT-031 | TC-FE-XCUT-017 |
| TEN-U05 | REQ-TENANT-030, REQ-TENANT-031 | TC-FE-XCUT-018 |
| TEN-U10, U11, U14, U15 | REQ-TENANT-032 | TC-FE-XCUT-019 |
| TEN-U12 | REQ-TENANT-032, RULE-TENANT-006 | TC-FE-XCUT-020 |
| TEN-U13, U73, U121 | RULE-TENANT-022 | TC-FE-XCUT-021 |
| TEN-U06 | REQ-TENANT-031 | TC-FE-XCUT-022 |
| TEN-U04 (forced change exemption) | REQ-TENANT-031; SEC RULE-SEC-059 | TC-FE-XCUT-023 |
| TEN-U07, U08, U72 | REQ-TENANT-034, RULE-TENANT-023 | TC-FE-XCUT-024 |
| TEN-U21, U40, U50, U77, U80 | REQ-TENANT-025, RULE-TENANT-003 | TC-FE-PLATFORM-007 |
| TEN-U22, U43, U47, U51, U75, U78, U81, U83 | REQ-TENANT-029, REQ-TENANT-030 | TC-FE-PLATFORM-008, TC-FE-PLATFORM-009 |
| TEN-U64, U65 | RULE-TENANT-018, RULE-TENANT-021 | TC-FE-PLATFORM-010 |
| TEN-U52, U82 | REQ-TENANT-029 | TC-FE-PLATFORM-011 |
| TEN-U04, U02 (tenant session) | REQ-TENANT-029, REQ-TENANT-031 | TC-FE-PLATFORM-012 |
| TEN-U24, U42, U46, U56, U60 | REQ-TENANT-026, RULE-TENANT-016 | TC-FE-PLATFORM-013 |
| TEN-U57, U08 | REQ-TENANT-033, REQ-TENANT-034 | TC-FE-PLATFORM-014 |
| TEN-U44 | REQ-TENANT-028 | TC-FE-PLATFORM-015 |
| TEN-U23, U45, U53, U61, U63, U84 | REQ-TENANT-027, RULE-TENANT-017 | TC-FE-PLATFORM-016 |
| TEN-U23, U54 (PLATFORM row), U62, U66 | RULE-TENANT-017, RULE-TENANT-024 | TC-FE-PLATFORM-017 |
| TEN-U25, U54, U85, U124 | REQ-TENANT-035 | TC-FE-PLATFORM-018 |
| TEN-U67 | REQ-TENANT-035 | TC-FE-PLATFORM-019 |
| TEN-U55, U120, U86 | REQ-TENANT-037, RULE-TENANT-027 | TC-FE-PLATFORM-020 |
| TEN-U68, U69 | RULE-TENANT-027, RULE-TENANT-028 | TC-FE-PLATFORM-021 |
| TEN-U59, U124 | REQ-TENANT-037 (restricted archive); FILE P2_5 FILE-U01 | TC-FE-PLATFORM-020, TC-FE-PLATFORM-022 |
| TEN-U26, U58, U70, U122 | REQ-TENANT-036, RULE-TENANT-025, RULE-TENANT-026 | TC-FE-PLATFORM-023 |
| TEN-U48, U63, U70 | REQ-TENANT-001 (1.3.0 policy), REQ-TENANT-036 | TC-FE-PLATFORM-024 |
| TEN-U27, U41, U123 | REQ-TENANT-007, REQ-TENANT-025 | TC-FE-PLATFORM-025 |

### M. Implementation notes and corrections — step TM-F3 (appended; rows above unchanged except the TEN-U22 / TEN-U23 citation)
| # | Kind | Item | Detail |
|---|---|---|---|
| TEN-U125 | CHANGED (F3) | i18n keys of the admin-reset drawer and the edit drawer's language Select (TEN-U40, TEN-U45, §H "Reused") | the F1 keys named there (`users.fieldNewPassword`, `users.fieldConfirmPassword`, `users.fieldRequireChange`, `users.localeNone` / `localeAr` / `localeEn`, `account.passwordPolicyHint`, `account.passwordBytes`, `account.errPasswordPolicy`) are written by the concurrent step F1 and do not exist on F3's base; F3 uses tenants-scoped keys instead: `tenants.fieldNewPassword` (New password / كلمة المرور الجديدة), `tenants.fieldRequireChange` (Require a password change at the next sign-in / إلزام بتغيير كلمة المرور عند الدخول التالي), `tenants.passwordBytes` ({bytes} of 72 bytes / {bytes} من 72 بايت), `tenants.errPasswordPolicy` (fallback of `SEC-400-PASSWORD-POLICY`), `tenants.localeNone` / `localeAr` / `localeEn` (Not set / العربية / English); the confirm field reuses `tenants.fieldAdminPasswordConfirm` and the policy hint reuses `tenants.fieldAdminPasswordHint` (CHANGED, §H). The closure step may fold them into the F1 keys |
| TEN-U126 | NEW (F3) | further keys the rows imply | `tenants.suspendReasonCount` ({count} / 500), `tenants.errCountryCodeInvalid`, `tenants.errPhoneInvalid`, `tenants.errTimezoneInvalid` (client checks of TEN-U40 mirroring the contract patterns; the server's `VALIDATION_ERROR` `fieldErrors` text still wins), `tenants.actionSaveBranding` (Save branding / حفظ العلامة), both languages |
| TEN-U127 | NEW (F3) | `ConfirmDialog` primitive (`src/components/overlays/ConfirmDialog.tsx`) | gains three optional props, default off: `confirmDisabled` (the suspend Confirm stays disabled until the reason is valid, TEN-U46), `confirmTestId` (the revoke dialog's Confirm becomes `tenant-revoke-retry` "Retry" after a 500, TEN-U54), `hideConfirm` (the outcome state of the export dialog shows Close only, TEN-U124) — the global primitive is extended, never copied (`CLAUDE.md` §3.8) |
| TEN-U128 | NEW (F3) | where the detail drawer's actions sit (TEN-U20) | footer: Close · Edit · Suspend / Activate (as 1.2.0 plus Edit); the body's last row after `AuditTrail` (`tenant-detail-actions`): Reset administrator password (not on PLATFORM) · Sign every user out (not on PLATFORM) · Export data — the footer keeps at most three buttons at drawer width |
| TEN-U129 | NEW (F3, FA review note) | the FILE deep links of TEN-U59 | `tenant-archives-link` (detail) and `tenant-export-open-files` (export outcome) are rendered only when the effective menu holds `FILE_BROWSER` (`useMenuFacade().holdsScreen`); an operator without the FILE screen sees neither link but keeps the re-download hint text, so no link lands on the 403 view |
| TEN-U130 | NEW (F3) | create drawer key lifecycle (TEN-U26) | the key is minted when the drawer opens (`useTenantCreateIdempotencyStore.open`), re-minted after any field change that follows a failed attempt (the store remembers the body of the last attempt and compares), re-minted after 409 `IDEMPOTENCY_KEY_CONFLICT`, and dropped when the drawer closes |
| TEN-U131 | NEW (F3, correction) | TEN-U22, TEN-U23 | append-only correction of FA's rows (TM-FA review round 2, LOW 2): the two saving level-2 drawers now cite Decision SEC P2_5 SEC-U78, like SEC-U08 / SEC-U09 |
| TEN-U132 | NEW (F3) | logo size pre-check (TEN-U47) | a chosen file over 1 048 576 bytes is refused on the client with `tenants.errLogoInvalid` and not sent; the type, the content and the plain-SVG rule stay the server's (`TENANT_LOGO_INVALID` with its own message) |
| TEN-U133 | NEW (F3) | how the URL moves (TEN-U21 … U23) | `?action=create` opens through `useDrawerUrlState('tenantId').openCreate` as in 1.2.0; detail ⇄ edit and the two second levels go through `useTenantUrlState` (one search-param update each, so `editId` replaces `tenantId` in a single history entry, and closing a second level removes only its own param); a `logoFor` / `adminResetFor` that is not numeric, not the open `tenantId`, or (admin reset) the PLATFORM row is removed with `replace` |
| TEN-U134 | NEW (F3) | export dialog states (TEN-U55, TEN-U120, TEN-U124) | confirm → running (indeterminate `<progress>`, elapsed seconds, Cancel / Escape / scrim disabled) → outcome (Close only) or refusal (the message, the Export button stays for a MANUAL repeat — nothing is retried automatically); a download failure after a successful export shows the outcome plus `tenants.errExportDownload` |

### N. Implementation notes — step TM-F2 (appended; rows above unchanged)
| # | Kind | Item | Detail |
|---|---|---|---|
| TEN-U135 | CHANGED (F2) | i18n key of TEN-U08 / TEN-U72 / §H | `auth.errTenantTokenRevoked` ships as **`branding.errTenantTokenRevoked`** (same en / ar texts), beside `branding.tenantLogoAlt` in ONE new `branding` section per dictionary and in `core/i18n/i18n.types.ts` (one localized tm-F2 block per shared file while F3 was merged in parallel); newfront `docs/DEVIATIONS.md` `[TM-F2]` |
| TEN-U136 | NEW (F2) | what reaches `<img src>` and the accent (TEN-U01, TEN-U05) | `safeLogoUrl`: only a same-origin path (`/…`, not `//…`) or an `http(s)` URL is rendered, anything else is treated as "no logo" (mark alone); `safeBrandColor`: only `#RRGGBB` (upper-cased) is written to `--color-accent-tenant`, anything else leaves the property absent (`src/core/tenant/tenantBrandingDisplay.ts`) |
| TEN-U137 | NEW (F2) | how the sign-in pages feed the typed code (TEN-U10, TEN-U11) | the page keeps the Tenant field's value in local state (initialised from the device default) through `TenantCodeInput.onValueChange` → each form's `onTenantCodeChange`; `AuthScreenShell` gains an optional `tenantLogo` slot rendered above the `<h1>`; one wrapper `AuthBrandingLogo` (40 px, `auth-tenant-logo`) serves SCR-SEC-001 … 003 (public branding) and SCR-SEC-012 (`/tenant/me`, SEC P2_5 SEC-U121) |
| TEN-U138 | NEW (F2) | when the 429 back-off is checked (TEN-U13) | the gate is TanStack `enabled` as a callback reading `usePublicBrandingThrottleStore.blockedUntil`, evaluated when the settled code changes — never on a timer: when `Retry-After` has passed, the code already in the field is not re-asked; the next settled code is. Codes answered before the 429 stay served from their cache entries (no request) |
| TEN-U139 | NEW (F2) | active entry tint (TEN-U05) | indicator `var(--color-accent-tenant, var(--teal-400, #2dd4bf))`; active background `color-mix(in srgb, var(--color-accent-tenant, #ffffff) 12%, transparent)` — identical to the 1.2.0 `rgba(255,255,255,0.12)` when no accent is set |
| TEN-U140 | NEW (F2) | transport (TEN-U08, TEN-U14) | `/api/v1/public/tenants/` joins `/api/v1/public/files/` as an anonymous public read in `httpClient` (request: no bearer, no tenant header; response: never reaches the 401, session-ended or forced-change handlers, whatever its code); `setTenantTokenRevokedHandler` sits beside `setPasswordChangeRequiredHandler`: a 401 `TENANT_TOKEN_REVOKED` calls it INSTEAD of the plain 401 handler → `endRevokedSession('TENANT_TOKEN_REVOKED')` (cache cleared, session cleared, one `info` toast while still signed in, the route guard goes to `/login`); 403 `TENANT_SUSPENDED` keeps the 1.2.0 handler (`danger` toast) |
| TEN-U141 | NEW (F2) | logo `alt` (TEN-U90) | `branding.tenantLogoAlt` with the tenant name in the active language (`nameAr` under Arabic, `nameEn` under English, then the other name, then the code) |
| TEN-U142 | NEW (F2) | test ids (§K) | + `sidebar-brand-wordmark` (the AVELYNQ word-mark, asserted by TC-FE-XCUT-015 … 017, 022) |
| TEN-U143 | NEW (F2) | test plan as built | TC-FE-XCUT-021: the pre-filled `PLATFORM` asked at mount is the first (429) request, then `E2E_T2` and `E2E_T9` send nothing, after 31 s `E2E_T2` is asked once; TC-FE-XCUT-023 also checks no horizontal scroll at 360 / 390 / 412 px in en and ar (SEC-U121); TC-FE-XCUT-024 waits for the shell's first reads before the revoke and triggers the refusal by a menu navigation |


══════════════════════════════════════════════════════════════════
