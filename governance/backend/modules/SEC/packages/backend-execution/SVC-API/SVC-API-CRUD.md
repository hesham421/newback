<!-- source: PHASE:SVC-API / SUB:SVC-API-CRUD -->
<!-- context: SVC-API-HEADER.md — phase-level preamble -->
<!-- traces: DBF-SEC-001, DBF-SEC-002, DBF-SEC-003, DBF-SEC-005, DBF-SEC-006, DBF-SEC-007, DBF-SEC-009, DBF-SEC-015, DBF-SEC-016, DBF-SEC-017, DBF-SEC-018, DBF-SEC-019, DBF-SEC-026, DBF-SEC-027, DBF-SEC-028, DBF-SEC-029, DBF-SEC-061, DBF-SEC-062, DBF-SEC-063, DBF-SEC-064, DBF-SEC-066, DBF-SEC-067, DBF-SEC-068, DBF-SEC-069, DBF-SEC-071, DBF-SEC-072, DBF-SEC-073, DBF-SEC-074, DBF-SEC-081, DBF-SEC-082, DBF-SEC-102, DBF-SEC-103, DBF-SEC-104, REQ-SEC-004, REQ-SEC-005, REQ-SEC-009, REQ-SEC-010, REQ-SEC-011, REQ-SEC-012, REQ-SEC-013, REQ-SEC-014, REQ-SEC-015, REQ-SEC-020, REQ-SEC-028, REQ-SEC-030, REQ-SEC-031 -->
<!-- SUB:SVC-API-CRUD:START traces=REQ-SEC-009,REQ-SEC-010,REQ-SEC-011,REQ-SEC-012,REQ-SEC-013,REQ-SEC-014,REQ-SEC-015,REQ-SEC-020,REQ-SEC-028,REQ-SEC-030,REQ-SEC-031 -->
### SUB — SVC-API-CRUD (single-entity mutations)

<!-- API:API-SEC-006:START traces=REQ-SEC-009,DBF-SEC-002,DBF-SEC-003,DBF-SEC-005,DBF-SEC-006,DBF-SEC-007 -->
### API-SEC-006 — create user
Endpoint     : POST /api/v1/sec/users
Layers       : controller → `UserController.create` ; service → `UserService.create`
Request      : body `{username, email, fullNameAr, fullNameEn, password, statusCode?}` — statusCode optional, defaults ACTIVE; excludes {userPk, passwordHash, lastLoginAt, isActiveFl, audit}
Response     : 201 · `UserResponse` (no passwordHash)
Validations  : uniqueness of username and email (QR-SEC-033) — violation → catalog `SEC-409-USER-DUP`
Errors       : `SEC-409-USER-DUP` (409)
Orchestration: validate uniqueness (QR-SEC-033) → hash password → persist (QR-SEC-006) → append AuditLogEntry `MODULE_GRANTED`? no — no audit event type is defined for plain user creation in SRS A6, so none is appended here (only the 14 named AUDIT_EVENT_TYPE codes apply; user-creation itself is not one of them, per SRS A6 — no invention) → return
Repository   : QR-SEC-006, QR-SEC-033 · join NONE · transaction READ_WRITE
Security     : screen SEC_USERS · permission `PERM_SEC_USERS_CREATE`
Localization : fullNameAr and fullNameEn both required
<!-- API:API-SEC-006:END -->

<!-- API:API-SEC-007:START traces=REQ-SEC-009,DBF-SEC-003,DBF-SEC-005,DBF-SEC-006 -->
### API-SEC-007 — update user
Endpoint     : PUT /api/v1/sec/users/{id}
Layers       : controller → `UserController.update` ; service → `UserService.update`
Request      : body `{email, fullNameAr, fullNameEn}` — excludes {userPk, username, passwordHash, statusCode, isActiveFl, audit}
Response     : 200 · `UserResponse`
Validations  : uniqueness of email excluding the current PK (QR-SEC-033, EXISTS default "excludes the current PK on update")
Errors       : `SEC-409-USER-DUP` (409), `SEC-404-USER` (404, id not found)
Orchestration: load (QR-SEC-001-style FIND_ONE by PK) → validate → update (QR-SEC-007) → return
Repository   : QR-SEC-007 · join NONE · transaction READ_WRITE
Security     : screen SEC_USERS · permission `PERM_SEC_USERS_UPDATE`
Localization : both name fields updatable independently
<!-- API:API-SEC-007:END -->

<!-- API:API-SEC-008:START traces=REQ-SEC-010,DBF-SEC-026,DBF-SEC-027,DBF-SEC-028,DBF-SEC-029 -->
### API-SEC-008 — assign roles to user
Endpoint     : PUT /api/v1/sec/users/{id}/roles
Layers       : controller → `UserController.assignRoles` ; service → `UserRoleService.assign`
Request      : body `{roleIds: [Long]}`
Response     : 200 · `UserResponse` with nested `roles: [{roleId, code, nameAr, nameEn}]`
Validations  : RULE-SEC-005 (full text: DATA-DOM §ENT-SEC-009) — the union of the user's existing + newly assigned roles' actions must contain no conflicting pair (QR-SEC-031) → violation `SEC-409-SOD-CONFLICT`
Errors       : `SEC-409-SOD-CONFLICT` (409), `SEC-404-USER`/`SEC-404-ROLE` (404)
Orchestration: load user + roles → check RULE-SEC-005 (QR-SEC-031) → replace the assignment set (QR-SEC-008) → append AuditLogEntry `ROLE_ASSIGNED` per added role, `ROLE_REVOKED` per removed role → return
Repository   : QR-SEC-008, QR-SEC-031 · join NONE · transaction READ_WRITE
Security     : screen SEC_USERS · permission `PERM_SEC_USERS_UPDATE`
Localization : role name fields returned in both languages
<!-- API:API-SEC-008:END -->

<!-- API:API-SEC-009:START traces=REQ-SEC-011,DBF-SEC-007,DBF-SEC-009,DBF-SEC-081,DBF-SEC-082 -->
### API-SEC-009 — deactivate user
Endpoint     : DELETE /api/v1/sec/users/{id}
Layers       : controller → `UserController.deactivate` ; service → `UserService.deactivate`
Request      : path `id`
Response     : 200 · confirmation `{userPk, statusCode: "DISABLED"}`
Validations  : none beyond existence
Errors       : `SEC-404-USER` (404)
Orchestration: load user → `User.deactivate()` (domain method: statusCode→DISABLED, isActiveFl→false) → persist (QR-SEC-009) → terminate every active session of that user (QR-SEC-026, bulk) → append AuditLogEntry `SESSION_TERMINATED` per session — this is the "immediately end that user's active sessions" half of REQ-SEC-011
Repository   : QR-SEC-009, QR-SEC-026 (bulk) · join NONE · transaction READ_WRITE
Security     : screen SEC_USERS · permission `PERM_SEC_USERS_UPDATE`
Localization : n/a (no message text beyond the confirmation)
<!-- API:API-SEC-009:END -->

<!-- API:API-SEC-010:START traces=REQ-SEC-031,DBF-SEC-007,DBF-SEC-009 -->
### API-SEC-010 — reactivate user
Endpoint     : PATCH /api/v1/sec/users/{id}
Layers       : controller → `UserController.reactivate` ; service → `UserService.reactivate`
Request      : path `id`
Response     : 200 · confirmation `{userPk, statusCode: "ACTIVE"}`
Validations  : the user must currently be DISABLED (A7 lifecycle — an ACTIVE or PENDING user cannot be "reactivated")
Errors       : `SEC-404-USER` (404), `SEC-409-INVALID-TRANSITION` (409)
Orchestration: load user → verify statusCode=DISABLED → `User.reactivate()` → persist (QR-SEC-010) → return
Repository   : QR-SEC-010 · join NONE · transaction READ_WRITE
Security     : screen SEC_USERS · permission `PERM_SEC_USERS_UPDATE`
Localization : n/a
<!-- API:API-SEC-010:END -->

<!-- API:API-SEC-011:START traces=REQ-SEC-004,REQ-SEC-005,DBF-SEC-102,DBF-SEC-103,DBF-SEC-104,DBF-SEC-001 -->
### API-SEC-011 — approve / reject signup
Endpoint     : PATCH /api/v1/sec/signup-requests/{id}
Layers       : controller → `SignupRequestController.decide` ; service → `SignupRequestService.decide`
Request      : body `{decision: "APPROVE"|"REJECT"}`
Response     : 200 · on APPROVE: the created `UserResponse`; on REJECT: the updated `SignupRequestResponse` (statusCode=REJECTED)
Validations  : the request must currently be PENDING (A7 lifecycle)
Errors       : `SEC-404-SIGNUP` (404), `SEC-409-INVALID-TRANSITION` (409)
Orchestration: load SignupRequest → verify PENDING → on APPROVE: create User (QR-SEC-006-style, statusCode=ACTIVE) + update SignupRequest (statusCode=APPROVED, reviewedBy/At) (QR-SEC-011); on REJECT: update SignupRequest (statusCode=REJECTED, reviewedBy/At) only, no User row (REQ-SEC-005) → return
Repository   : QR-SEC-011 · join NONE · transaction READ_WRITE
Security     : screen SEC_USERS (the "Pending sign-ups" tab, SRS B3) · permission `PERM_SEC_USERS_UPDATE`
Localization : n/a
Notes        : credential — no upstream artifact names one for the approved account, yet DBF-SEC-004 `password_hash` is NOT NULL. Operative behaviour: the account is created with an unusable random secret (never logged, returned or transmitted), so it is ACTIVE but cannot be authenticated against; the owner's route to a real credential is the existing API-SEC-003 → API-SEC-004 reset pair (RULE-SEC-006). OPEN for a human (not decided here): no SEC artifact says the approved owner is notified or that a reset token is issued at approval — REQ-SEC-029 triggers on token issuance, not approval, and is itself `optional` — and the state is undiscoverable from outside, since login answers the deliberately uniform `SEC-401-INVALID-CREDENTIALS` (POL-SEC-004) and API-SEC-003 answers the same generic 200 whether or not the email exists, making "approved", "still pending" and "rejected" indistinguishable. The two candidate answers are (a) SEC issues a reset token at approval time — which introduces a NOTIF dependency into a module declared ROOT — or (b) the spec states plainly that the owner is informed out of band.
<!-- API:API-SEC-011:END -->

<!-- API:API-SEC-013:START traces=REQ-SEC-012,DBF-SEC-015,DBF-SEC-016,DBF-SEC-017,DBF-SEC-018,DBF-SEC-019 -->
### API-SEC-013 — create role
Endpoint     : POST /api/v1/sec/roles
Layers       : controller → `RoleController.create` ; service → `RoleService.create`
Request      : body `{code, nameAr, nameEn, descriptionAr?, descriptionEn?}` — excludes {rolePk, isActiveFl, audit}
Response     : 201 · `RoleResponse`
Validations  : uniqueness of code (QR-SEC-034)
Errors       : `SEC-409-ROLE-DUP` (409)
Orchestration: validate uniqueness → persist (QR-SEC-013) → return
Repository   : QR-SEC-013, QR-SEC-034 · join NONE · transaction READ_WRITE
Security     : screen SEC_ROLES · permission `PERM_SEC_ROLES_CREATE`
Localization : nameAr/nameEn required; descriptionAr/En optional but both-or-neither
<!-- API:API-SEC-013:END -->

<!-- API:API-SEC-014:START traces=REQ-SEC-012,DBF-SEC-061,DBF-SEC-062,DBF-SEC-063,DBF-SEC-064 -->
### API-SEC-014 — grant module to role
Endpoint     : POST /api/v1/sec/roles/{id}/modules
Layers       : controller → `RoleGrantController.grantModule` ; service → `RoleGrantService.grantModule`
Request      : body `{moduleId}`
Response     : 201 · `RoleModuleGrantResponse`
Validations  : role and module must be active; grant must not already exist (UQ_SEC_ROLE_MODULE_GRANT_ROLE_MODULE)
Errors       : `SEC-409-GRANT-DUP` (409), `SEC-404-ROLE`/`SEC-404-MODULE` (404)
Orchestration: validate → persist (QR-SEC-014) → append AuditLogEntry `MODULE_GRANTED` → return
Repository   : QR-SEC-014 · join NONE · transaction READ_WRITE
Security     : screen SEC_ROLES · permission `PERM_SEC_ROLES_UPDATE`
Localization : n/a
<!-- API:API-SEC-014:END -->

<!-- API:API-SEC-015:START traces=REQ-SEC-015,DBF-SEC-061,DBF-SEC-062,DBF-SEC-066,DBF-SEC-071 -->
### API-SEC-015 — revoke module grant
Endpoint     : DELETE /api/v1/sec/roles/{id}/modules/{moduleId}
Layers       : controller → `RoleGrantController.revokeModule` ; service → `RoleGrantService.revokeModule`
Request      : path `id` (role), `moduleId`
Response     : 200 · confirmation `{revokedScreenGrants: n, revokedActionGrants: m}`
Validations  : RULE-SEC-003 (full text: DATA-DOM §ENT-SEC-007) — cascade is mandatory, not optional
Errors       : `SEC-404-GRANT` (404, grant does not exist)
Orchestration: find dependent screen/action grants for this role under this module (QR-SEC-032) → delete them, then the module grant itself (QR-SEC-015) → append AuditLogEntry `MODULE_REVOKED` (+ `SCREEN_REVOKED`/`ACTION_REVOKED` per cascaded row) → return
Repository   : QR-SEC-015, QR-SEC-032 · join NONE (sequential deletes, same transaction) · transaction READ_WRITE
Security     : screen SEC_ROLES · permission `PERM_SEC_ROLES_UPDATE`
Localization : n/a
<!-- API:API-SEC-015:END -->

<!-- API:API-SEC-016:START traces=REQ-SEC-013,DBF-SEC-066,DBF-SEC-067,DBF-SEC-068,DBF-SEC-069 -->
### API-SEC-016 — grant screen to role
Endpoint     : POST /api/v1/sec/roles/{id}/screens
Layers       : controller → `RoleGrantController.grantScreen` ; service → `RoleGrantService.grantScreen`
Request      : body `{screenId}`
Response     : 201 · `RoleScreenGrantResponse`
Validations  : RULE-SEC-001 (full text: DATA-DOM §ENT-SEC-008) — role must already hold the screen's module grant (QR-SEC-028) → violation `SEC-409-NO-MODULE-GRANT`
Errors       : `SEC-409-NO-MODULE-GRANT` (409), `SEC-409-GRANT-DUP` (409), `SEC-404-ROLE`/`SEC-404-SCREEN` (404)
Orchestration: resolve screen's module → check RULE-SEC-001 (QR-SEC-028) → persist (QR-SEC-016) → append AuditLogEntry `SCREEN_GRANTED` → return
Repository   : QR-SEC-016, QR-SEC-028 · join NONE · transaction READ_WRITE
Security     : screen SEC_ROLES · permission `PERM_SEC_ROLES_UPDATE`
Localization : n/a
<!-- API:API-SEC-016:END -->

<!-- API:API-SEC-017:START traces=REQ-SEC-014,REQ-SEC-020,REQ-SEC-030,DBF-SEC-071,DBF-SEC-072,DBF-SEC-073,DBF-SEC-074 -->
### API-SEC-017 — grant action to role
Endpoint     : POST /api/v1/sec/roles/{id}/actions
Layers       : controller → `RoleGrantController.grantAction` ; service → `RoleGrantService.grantAction`
Request      : body `{actionId}`
Response     : 201 · `RoleActionGrantResponse`
Validations  : RULE-SEC-002 (full text: DATA-DOM §ENT-SEC-009) — role must hold the action's screen grant (QR-SEC-029); RULE-SEC-007 (full text: DATA-DOM §ENT-SEC-009) — unless the action itself is VIEW, role must also hold VIEW on that screen (QR-SEC-030); RULE-SEC-005 (full text: DATA-DOM §ENT-SEC-009) — the grant must not give the underlying user a conflicting-pair action (QR-SEC-031)
Errors       : `SEC-409-NO-SCREEN-GRANT` (409), `SEC-409-NO-VIEW-GRANT` (409), `SEC-409-SOD-CONFLICT` (409), `SEC-409-GRANT-DUP` (409), `SEC-404-ROLE`/`SEC-404-ACTION` (404)
Orchestration: resolve action's screen → check RULE-SEC-002 (QR-SEC-029) → check RULE-SEC-007 unless actionCode=VIEW (QR-SEC-030) → check RULE-SEC-005 across every user holding this role (QR-SEC-031) → persist (QR-SEC-017) → append AuditLogEntry `ACTION_GRANTED` → return
Repository   : QR-SEC-017, QR-SEC-029, QR-SEC-030, QR-SEC-031 · join NONE · transaction READ_WRITE
Security     : screen SEC_ROLES · permission `PERM_SEC_ROLES_UPDATE`
Localization : n/a
<!-- API:API-SEC-017:END -->

<!-- API:API-SEC-026:START traces=REQ-SEC-028,DBF-SEC-081,DBF-SEC-082 -->
### API-SEC-026 — terminate session
Endpoint     : DELETE /api/v1/sec/sessions/{id}
Layers       : controller → `SessionController.terminate` ; service → `SessionService.terminate`
Request      : path `id`
Response     : 200 · confirmation `{activeSessionPk, terminatedAt}`
Validations  : session must currently be active (`terminatedAt IS NULL`)
Errors       : `SEC-404-SESSION` (404), `SEC-409-ALREADY-TERMINATED` (409)
Orchestration: load session → verify active → set terminatedAt/terminatedBy (QR-SEC-026) → append AuditLogEntry `SESSION_TERMINATED` → the associated tokenRef is invalidated at the auth-filter cache layer (REQ-SEC-028: "no longer accepted for any subsequent request") → return
Repository   : QR-SEC-026 · join NONE · transaction READ_WRITE
Security     : screen SEC_SESSIONS · permission `PERM_SEC_SESSIONS_DELETE`
Localization : n/a
<!-- API:API-SEC-026:END -->
<!-- SUB:SVC-API-CRUD:END -->
