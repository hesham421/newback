<!-- source: PHASE:SVC-API / SUB:SVC-API-INT -->
<!-- context: SVC-API-HEADER.md — phase-level preamble -->
<!-- traces: DBF-SEC-002, DBF-SEC-004, DBF-SEC-007, DBF-SEC-031, DBF-SEC-032, DBF-SEC-033, DBF-SEC-040, DBF-SEC-041, DBF-SEC-042, DBF-SEC-043, DBF-SEC-050, DBF-SEC-051, DBF-SEC-052, DBF-SEC-053, DBF-SEC-054, DBF-SEC-075, DBF-SEC-076, DBF-SEC-077, DBF-SEC-078, DBF-SEC-084, DBF-SEC-085, DBF-SEC-086, DBF-SEC-087, DBF-SEC-088, DBF-SEC-089, DBF-SEC-092, DBF-SEC-093, DBF-SEC-094, DBF-SEC-095, DBF-SEC-096, DBF-SEC-098, DBF-SEC-099, DBF-SEC-100, DBF-SEC-101, DBF-SEC-102, REQ-SEC-001, REQ-SEC-002, REQ-SEC-003, REQ-SEC-006, REQ-SEC-007, REQ-SEC-008, REQ-SEC-016, REQ-SEC-017, REQ-SEC-018, REQ-SEC-019, REQ-SEC-026, REQ-SEC-029 -->
<!-- SUB:SVC-API-INT:START traces=REQ-SEC-001,REQ-SEC-002,REQ-SEC-003,REQ-SEC-006,REQ-SEC-007,REQ-SEC-008,REQ-SEC-016,REQ-SEC-017,REQ-SEC-018,REQ-SEC-019,REQ-SEC-026,REQ-SEC-029 -->
### SUB — SVC-API-INT (auth flows, onboarding registration, export)

<!-- API:API-SEC-001:START traces=REQ-SEC-001,REQ-SEC-002,DBF-SEC-002,DBF-SEC-004,DBF-SEC-007,DBF-SEC-075,DBF-SEC-076,DBF-SEC-077,DBF-SEC-078 -->
### API-SEC-001 — login
Endpoint     : POST /api/v1/sec/auth/login   (pre-authentication)
Layers       : controller → `AuthController.login` ; service → `AuthService.login`
Request      : body `{username, password}`
Response     : 200 (success) · `{accessToken, tokenType, expiresIn}` · `ApiResponse<LoginResponse>` — 401 on failure, same envelope shape, `success:false`
Validations  : credentials must match an ACTIVE user (QR-SEC-001 + password verify)
Errors       : `SEC-401-INVALID-CREDENTIALS` (401)
Orchestration: load user by username (QR-SEC-001) → verify active + password → on success: create ActiveSession (QR-SEC-006-style SAVE on ENT-SEC-010), update `lastLoginAt`, append AuditLogEntry `LOGIN_SUCCESS`, issue token → on failure: append AuditLogEntry `LOGIN_FAILED` (actorUserId null if username unknown), return 401 (REQ-SEC-002)
Repository   : QR-SEC-001 · join NONE · transaction READ_WRITE (session + audit write even on the read-mostly path)
Security     : screen SEC_LOGIN · public — no permission required
Localization : `SEC-401-INVALID-CREDENTIALS` message ar: "بيانات الدخول غير صحيحة" / en: "Invalid credentials" (POL-SEC-004)
<!-- API:API-SEC-001:END -->

<!-- API:API-SEC-002:START traces=REQ-SEC-003,DBF-SEC-098,DBF-SEC-099,DBF-SEC-100,DBF-SEC-101,DBF-SEC-102 -->
### API-SEC-002 — submit sign-up
Endpoint     : POST /api/v1/sec/auth/signup   (pre-authentication)
Layers       : controller → `AuthController.signup` ; service → `SignupRequestService.submit`
Request      : body `{email, fullNameAr, fullNameEn}`
Response     : 201 · `SignupRequestResponse` (statusCode=PENDING)
Validations  : email not already a User.email and not an already-PENDING SignupRequest.email
Errors       : `SEC-409-SIGNUP-DUP` (409)
Orchestration: validate → persist (QR-SEC-002, statusCode=PENDING) → return
Repository   : QR-SEC-002 · join NONE · transaction READ_WRITE
Security     : screen SEC_SIGNUP · public — no permission required
Localization : fullNameAr/fullNameEn both required
<!-- API:API-SEC-002:END -->

<!-- API:API-SEC-003:START traces=REQ-SEC-006,REQ-SEC-029,DBF-SEC-092,DBF-SEC-093,DBF-SEC-094,DBF-SEC-095 -->
### API-SEC-003 — request password reset
Endpoint     : POST /api/v1/sec/auth/password-reset/request   (pre-authentication)
Layers       : controller → `AuthController.requestReset` ; service → `PasswordResetService.request`
Request      : body `{email}`
Response     : 200 · generic confirmation `{message}` — always the same shape whether or not the email exists (never reveals which)
Validations  : none exposed to the caller (existence check is internal only, never surfaced)
Errors       : none beyond platform-standard
Orchestration: look up user by email (internal) → if found: create PasswordResetToken (QR-SEC-003, expiresAt=now+30min) → append AuditLogEntry `PASSWORD_RESET_REQUESTED` → **Where** the Notifications integration is enabled (REQ-SEC-029, optional pattern): dispatch per `new project/integration-notifications-fileservice.md` §1.2 through NOTIF's `NotificationDispatchApi.dispatchIndependently()` crossmodule entry point — not `dispatch()`: `dispatchIndependently` runs in an independent transaction (`REQUIRES_NEW`), so a dispatch failure commits or rolls back on its own and can never mark SEC's transaction rollback-only, leaving the token and audit rows intact — with `templateCode` identifying the reset message, `recipientId=userPk`, `moduleCode="SEC"`, and the recipient's `email` carried among the dispatch `variables` (alongside `token` / `expiresAt`) because NOTIF resolves the destination address from `variables.get("email")`, having no crossmodule contact-lookup for a bare recipientId → if not found: do nothing further (still returns the same generic 200) → return
Repository   : QR-SEC-003 · join NONE · transaction READ_WRITE
Security     : screen SEC_PWD_RESET · public — no permission required
Localization : generic confirmation message in ar + en
<!-- API:API-SEC-003:END -->

<!-- API:API-SEC-004:START traces=REQ-SEC-007,REQ-SEC-008,DBF-SEC-093,DBF-SEC-095,DBF-SEC-096,DBF-SEC-004 -->
### API-SEC-004 — complete password reset
Endpoint     : POST /api/v1/sec/auth/password-reset/complete   (pre-authentication)
Layers       : controller → `AuthController.completeReset` ; service → `PasswordResetService.complete`
Request      : body `{token, newPassword}`
Response     : 200 · confirmation `{message}`
Validations  : RULE-SEC-006 (full text: DATA-DOM §ENT-SEC-012) — token must be unexpired and unused (QR-SEC-038)
Errors       : `SEC-409-RESET-TOKEN-INVALID` (409)
Orchestration: validate token (QR-SEC-038) → hash newPassword → update user's passwordHash + mark token usedAt (QR-SEC-004) → append AuditLogEntry `PASSWORD_RESET_COMPLETED` → return
Repository   : QR-SEC-004, QR-SEC-038 · join NONE · transaction READ_WRITE
Security     : screen SEC_PWD_RESET · public — no permission required
Localization : `SEC-409-RESET-TOKEN-INVALID` message ar: "رابط إعادة التعيين غير صالح أو منتهي" / en: "This reset link is invalid or has expired"
<!-- API:API-SEC-004:END -->

<!-- API:API-SEC-018:START traces=REQ-SEC-016,DBF-SEC-031,DBF-SEC-032,DBF-SEC-033 -->
### API-SEC-018 — register module
Endpoint     : POST /api/v1/sec/registry/modules
Layers       : controller → `RegistryController.registerModule` ; service → `RegistryService.registerModule`
Request      : body `{code, nameAr, nameEn}`
Response     : 201 · `ModuleRegistryResponse`
Validations  : uniqueness of code (QR-SEC-035)
Errors       : `SEC-409-MODULE-DUP` (409)
Orchestration: validate → persist (QR-SEC-018) → return
Repository   : QR-SEC-018, QR-SEC-035 · join NONE · transaction READ_WRITE
Security     : screen SEC_MODULE_REGISTRY · permission `PERM_SEC_MODULE_REGISTRY_UPDATE` — called by a registering module's own onboarding process, itself acting through an administrator-held credential; SEC mints no special "system" principal
Localization : nameAr/nameEn both required
<!-- API:API-SEC-018:END -->

<!-- API:API-SEC-019:START traces=REQ-SEC-017,REQ-SEC-018,DBF-SEC-040,DBF-SEC-041,DBF-SEC-042,DBF-SEC-043 -->
### API-SEC-019 — register screen
Endpoint     : POST /api/v1/sec/registry/screens
Layers       : controller → `RegistryController.registerScreen` ; service → `RegistryService.registerScreen`
Request      : body `{moduleCode, pageCode, nameAr, nameEn}`
Response     : 201 · `ScreenRegistryResponse`
Validations  : RULE-SEC-004 (full text: DATA-DOM §ENT-SEC-005) — module must already be registered (QR-SEC-036); uniqueness of pageCode (QR-SEC-036)
Errors       : `SEC-409-MODULE-NOT-REGISTERED` (409), `SEC-409-SCREEN-DUP` (409)
Orchestration: check RULE-SEC-004 + uniqueness (QR-SEC-036) → persist (QR-SEC-019) → return
Repository   : QR-SEC-019, QR-SEC-036 · join NONE · transaction READ_WRITE
Security     : screen SEC_MODULE_REGISTRY · permission `PERM_SEC_MODULE_REGISTRY_UPDATE`
Localization : nameAr/nameEn both required
<!-- API:API-SEC-019:END -->

<!-- API:API-SEC-020:START traces=REQ-SEC-019,DBF-SEC-050,DBF-SEC-051,DBF-SEC-052,DBF-SEC-053,DBF-SEC-054 -->
### API-SEC-020 — register action
Endpoint     : POST /api/v1/sec/registry/actions
Layers       : controller → `RegistryController.registerAction` ; service → `RegistryService.registerAction`
Request      : body `{pageCode, actionCode, nameAr, nameEn}`
Response     : 201 · `ActionRegistryResponse` (includes server-derived `permissionCode`)
Validations  : screen must already be registered; uniqueness of the derived permissionCode (QR-SEC-037)
Errors       : `SEC-409-SCREEN-NOT-REGISTERED` (409), `SEC-409-ACTION-DUP` (409)
Orchestration: resolve screen by pageCode → derive `permissionCode = PERM_<pageCode>_<actionCode>` → validate uniqueness (QR-SEC-037) → persist (QR-SEC-020) → return
Repository   : QR-SEC-020, QR-SEC-037 · join NONE · transaction READ_WRITE
Security     : screen SEC_MODULE_REGISTRY · permission `PERM_SEC_MODULE_REGISTRY_UPDATE`
Localization : nameAr/nameEn both required
<!-- API:API-SEC-020:END -->

<!-- API:API-SEC-024:START traces=REQ-SEC-026,DBF-SEC-084,DBF-SEC-085,DBF-SEC-086,DBF-SEC-087,DBF-SEC-088,DBF-SEC-089 -->
### API-SEC-024 — export audit log
Endpoint     : GET /api/v1/sec/audit-log/export
Layers       : controller → `AuditLogController.export` ; service → `AuditLogService.export`
Request      : query params — same filter set as API-SEC-023 (eventTypeCode, actorUserId, occurredFrom/occurredTo), no paging (exports the full filtered set)
Response     : 200 · `Content-Type: text/csv` body, one row per matching AuditLogEntry, all fields
Validations  : none
Errors       : INTERNAL_ERROR only (platform-standard, shared handler)
Orchestration: load the full filtered set (QR-SEC-024, same filters as QR-SEC-023, unpaged) → serialize to CSV → return (REQ-SEC-026: "exactly the filtered entries' fields")
Repository   : QR-SEC-024 · join NONE · transaction READ_ONLY
Security     : screen SEC_AUDIT_LOG · permission `PERM_SEC_AUDIT_LOG_VIEW` (shares VIEW — export is not a separate mutation, SRS Access summary)
Localization : detailsAr/detailsEn both included as separate CSV columns
<!-- API:API-SEC-024:END -->
<!-- SUB:SVC-API-INT:END -->
