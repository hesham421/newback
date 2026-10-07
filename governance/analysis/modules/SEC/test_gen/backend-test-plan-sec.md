# BACKEND TEST PLAN — الأمان / Security (SEC)
══════════════════════════════════════════════════════════════════
Module : SEC   Version : v1   Profile : erp   Scope : project (modules FIN, MDL, SEC)
Sources: srs-sec.md v1 · backend-execution-plan-sec.md v1 · registry-srs-sec.md v1 · registry-db-sec.md v1
Framework: agnostic (profile.stack.testing.backend) — every block below is a
  specification the consumer repo turns into its own test; no framework name,
  annotation or file layout appears anywhere in this document.
REDUCED: no — every input plan for SEC is present.
Open ADRs: 0 new this run (ADR-SEC-001, ADR-SEC-002 unaffected).
TC count: 35 (module scope) · 0 (integration — SEC registers no XM row in either direction;
  TC-SEC-034/035 exercise SEC's own EXPOSED crossmodule interface and are module-scope)
══════════════════════════════════════════════════════════════════

<!-- PHASE:TEST-PLAN-BE:START traces=REQ-SEC-001,REQ-SEC-002,REQ-SEC-003,REQ-SEC-004,REQ-SEC-005,REQ-SEC-006,REQ-SEC-007,REQ-SEC-008,REQ-SEC-009,REQ-SEC-010,REQ-SEC-011,REQ-SEC-012,REQ-SEC-013,REQ-SEC-014,REQ-SEC-015,REQ-SEC-016,REQ-SEC-017,REQ-SEC-018,REQ-SEC-019,REQ-SEC-020,REQ-SEC-021,REQ-SEC-022,REQ-SEC-023,REQ-SEC-024,REQ-SEC-025,REQ-SEC-026,REQ-SEC-027,REQ-SEC-028,REQ-SEC-029,REQ-SEC-030,REQ-SEC-031,REQ-SEC-032,REQ-SEC-033,REQ-SEC-034,REQ-SEC-035 -->

<!-- SUB:RULE-SCENARIOS:START traces=REQ-SEC-008,REQ-SEC-013,REQ-SEC-014,REQ-SEC-015,REQ-SEC-018,REQ-SEC-020,REQ-SEC-030 -->
### SUB — RULE-SCENARIOS (RULE-driven violations / cascades)

<!-- TC:TC-SEC-008:START traces=AC-SEC-008,REQ-SEC-008,API-SEC-004 -->
### TC-SEC-008 — reject an expired or used reset token
Derived from : AC-SEC-008 (REQ-SEC-008)
Exercises    : API-SEC-004 POST /api/v1/sec/auth/password-reset/complete
Rule / code  : RULE-SEC-006 → SEC-409-RESET-TOKEN-INVALID
Scenario     : VIOLATION · data class INVALID · language ALL
Preconditions: a PasswordResetToken with expiresAt in the past, or usedAt already set
Steps        : 1. submit {token, newPassword} to the complete-reset endpoint
Expected     : 409 SEC-409-RESET-TOKEN-INVALID; message ar "رابط إعادة التعيين غير صالح أو منتهي" / en "This reset link is invalid or has expired"; password unchanged
Test data    : an expired token fixture; a used token fixture (two runs of this TC)
<!-- TC:TC-SEC-008:END -->

<!-- TC:TC-SEC-013:START traces=AC-SEC-013,REQ-SEC-013,API-SEC-016 -->
### TC-SEC-013 — reject a screen grant without its module grant
Derived from : AC-SEC-013 (REQ-SEC-013)
Exercises    : API-SEC-016 POST /api/v1/sec/roles/{id}/screens
Rule / code  : RULE-SEC-001 → SEC-409-NO-MODULE-GRANT
Scenario     : VIOLATION · data class INVALID · language ALL
Preconditions: a role with no RoleModuleGrant for module FIN; an active FIN screen registered
Steps        : 1. request a screen grant for that role naming the FIN screen
Expected     : 409 SEC-409-NO-MODULE-GRANT; message ar "لا يمكن منح شاشة دون منح الوحدة أولًا" / en "Cannot grant a screen without first granting its module"; no RoleScreenGrant row created
Test data    : role R1 (no FIN module grant); screen FIN_ACCOUNTS
<!-- TC:TC-SEC-013:END -->

<!-- TC:TC-SEC-014:START traces=AC-SEC-014,REQ-SEC-014,API-SEC-017 -->
### TC-SEC-014 — reject an action grant without its screen grant
Derived from : AC-SEC-014 (REQ-SEC-014)
Exercises    : API-SEC-017 POST /api/v1/sec/roles/{id}/actions
Rule / code  : RULE-SEC-002 → SEC-409-NO-SCREEN-GRANT
Scenario     : VIOLATION · data class INVALID · language ALL
Preconditions: a role with no RoleScreenGrant for a given screen
Steps        : 1. request an action grant on that screen for that role
Expected     : 409 SEC-409-NO-SCREEN-GRANT; message ar "لا يمكن منح إجراء دون منح الشاشة أولًا" / en "Cannot grant an action without first granting its screen"; no RoleActionGrant row created
Test data    : role R1 (no screen grant); action VIEW on FIN_ACCOUNTS
<!-- TC:TC-SEC-014:END -->

<!-- TC:TC-SEC-015:START traces=AC-SEC-015,REQ-SEC-015,API-SEC-015 -->
### TC-SEC-015 — cascade-revoke screen/action grants when a module grant is revoked
Derived from : AC-SEC-015 (REQ-SEC-015)
Exercises    : API-SEC-015 DELETE /api/v1/sec/roles/{id}/modules/{moduleId}
Rule / code  : RULE-SEC-003 → (no error code — success-path cascade)
Scenario     : STATE · data class VALID · language ALL
Preconditions: a role holding a module grant + 2 screen grants + 3 action grants under that module
Steps        : 1. revoke the role's module grant
Expected     : 200; the module grant plus every dependent screen/action grant deleted (0 remain); response reports revokedScreenGrants=2, revokedActionGrants=3
Test data    : role R1, module FIN, 2 screens, 3 actions pre-granted
<!-- TC:TC-SEC-015:END -->

<!-- TC:TC-SEC-018:START traces=AC-SEC-018,REQ-SEC-018,API-SEC-019 -->
### TC-SEC-018 — reject a screen registered under an unregistered module
Derived from : AC-SEC-018 (REQ-SEC-018)
Exercises    : API-SEC-019 POST /api/v1/sec/registry/screens
Rule / code  : RULE-SEC-004 → SEC-409-MODULE-NOT-REGISTERED
Scenario     : VIOLATION · data class INVALID · language ALL
Preconditions: a module code with no ModuleRegistry row
Steps        : 1. register a screen naming that unregistered module code
Expected     : 409 SEC-409-MODULE-NOT-REGISTERED; message ar "الوحدة غير مسجّلة" / en "Module is not registered"; no ScreenRegistry row created
Test data    : moduleCode="ZZZ" (never registered)
<!-- TC:TC-SEC-018:END -->

<!-- TC:TC-SEC-020:START traces=AC-SEC-020,REQ-SEC-020,API-SEC-017,API-SEC-008 -->
### TC-SEC-020 — prevent one user from holding two conflicting actions
Derived from : AC-SEC-020 (REQ-SEC-020)
Exercises    : API-SEC-017 POST /api/v1/sec/roles/{id}/actions (also reachable via API-SEC-008 role assignment)
Rule / code  : RULE-SEC-005 → SEC-409-SOD-CONFLICT
Scenario     : VIOLATION · data class INVALID · language ALL
Preconditions: two actions declared conflicting by their owning module; a user already holds one of them via role R1
Steps        : 1. attempt to grant role R2 (which carries the conflicting action) to the same user
Expected     : 409 SEC-409-SOD-CONFLICT; message ar "هذا المستخدم يملك إجراءً متعارضًا بالفعل" / en "This user already holds a conflicting action"; assignment not created
Test data    : FIN's entry-creator / period-close-approver pair (general-accounting-system-plan-en.md §8.2)
<!-- TC:TC-SEC-020:END -->

<!-- TC:TC-SEC-030:START traces=AC-SEC-030,REQ-SEC-030,API-SEC-017 -->
### TC-SEC-030 — VIEW required before any other action takes effect
Derived from : AC-SEC-030 (REQ-SEC-030)
Exercises    : API-SEC-017 POST /api/v1/sec/roles/{id}/actions (grant time) and the CORE interceptor (runtime)
Rule / code  : RULE-SEC-007 → SEC-409-NO-VIEW-GRANT (grant time) / SEC-403-FORBIDDEN (runtime)
Scenario     : VIOLATION · data class INVALID · language ALL
Preconditions: a role holds CREATE on a screen but not VIEW on that same screen
Steps        : 1. (grant time) attempt to grant CREATE without VIEW already present — 2. (runtime, if somehow granted) that role's user attempts the CREATE action
Expected     : 1. 409 SEC-409-NO-VIEW-GRANT, message ar "يلزم منح إجراء العرض (VIEW) أولًا على هذه الشاشة" / en "The VIEW action must be granted on this screen first" — 2. 403 SEC-403-FORBIDDEN if reached
Test data    : role R1, screen FIN_ACCOUNTS, action CREATE only
<!-- TC:TC-SEC-030:END -->
<!-- SUB:RULE-SCENARIOS:END -->

<!-- SUB:API-SCENARIOS:START traces=REQ-SEC-001,REQ-SEC-002,REQ-SEC-003,REQ-SEC-004,REQ-SEC-005,REQ-SEC-006,REQ-SEC-007,REQ-SEC-009,REQ-SEC-010,REQ-SEC-011,REQ-SEC-012,REQ-SEC-016,REQ-SEC-017,REQ-SEC-019,REQ-SEC-021,REQ-SEC-022,REQ-SEC-023,REQ-SEC-024,REQ-SEC-025,REQ-SEC-026,REQ-SEC-027,REQ-SEC-028,REQ-SEC-029,REQ-SEC-031,REQ-SEC-032,REQ-SEC-033,REQ-SEC-034,REQ-SEC-035 -->
### SUB — API-SCENARIOS (endpoint-driven happy / state / permission paths)

<!-- TC:TC-SEC-001:START traces=AC-SEC-001,REQ-SEC-001,API-SEC-001 -->
### TC-SEC-001 — successful login
Derived from : AC-SEC-001 (REQ-SEC-001)
Exercises    : API-SEC-001 POST /api/v1/sec/auth/login
Rule / code  : — (happy path)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: an active user with a known username/password
Steps        : 1. POST {username, password} with correct credentials
Expected     : 200; response carries accessToken/tokenType/expiresIn; an ActiveSession row created; lastLoginAt updated
Test data    : username "u1", correct password
<!-- TC:TC-SEC-001:END -->

<!-- TC:TC-SEC-002:START traces=AC-SEC-002,REQ-SEC-002,API-SEC-001 -->
### TC-SEC-002 — reject invalid credentials
Derived from : AC-SEC-002 (REQ-SEC-002)
Exercises    : API-SEC-001 POST /api/v1/sec/auth/login
Rule / code  : — (POL-SEC-004) → SEC-401-INVALID-CREDENTIALS
Scenario     : VIOLATION · data class INVALID · language ALL
Preconditions: a wrong password, or an unknown/disabled username
Steps        : 1. POST {username, password} with the wrong password — 2. repeat with an unknown username
Expected     : 401 SEC-401-INVALID-CREDENTIALS; message ar "بيانات الدخول غير صحيحة" / en "Invalid credentials"; no session issued; one LOGIN_FAILED audit entry per attempt
Test data    : username "u1" + wrong password; username "no-such-user"
<!-- TC:TC-SEC-002:END -->

<!-- TC:TC-SEC-003:START traces=AC-SEC-003,REQ-SEC-003,API-SEC-002 -->
### TC-SEC-003 — submit a sign-up request
Derived from : AC-SEC-003 (REQ-SEC-003)
Exercises    : API-SEC-002 POST /api/v1/sec/auth/signup
Rule / code  : — (happy path)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: email not already registered or pending
Steps        : 1. POST {email, fullNameAr, fullNameEn}
Expected     : 201; SignupRequest created with statusCode=PENDING; no User row created
Test data    : email "new@example.com"
<!-- TC:TC-SEC-003:END -->

<!-- TC:TC-SEC-004:START traces=AC-SEC-004,REQ-SEC-004,API-SEC-011 -->
### TC-SEC-004 — approve a sign-up request
Derived from : AC-SEC-004 (REQ-SEC-004)
Exercises    : API-SEC-011 PATCH /api/v1/sec/signup-requests/{id}
Rule / code  : — (happy path)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: a SignupRequest with statusCode=PENDING
Steps        : 1. PATCH {decision: "APPROVE"}
Expected     : 200; a User created with statusCode=ACTIVE; SignupRequest set APPROVED
Test data    : pending signup for "new@example.com"
<!-- TC:TC-SEC-004:END -->

<!-- TC:TC-SEC-005:START traces=AC-SEC-005,REQ-SEC-005,API-SEC-011 -->
### TC-SEC-005 — reject a sign-up request
Derived from : AC-SEC-005 (REQ-SEC-005)
Exercises    : API-SEC-011 PATCH /api/v1/sec/signup-requests/{id}
Rule / code  : — (happy path, negative decision)
Scenario     : STATE · data class VALID · language ALL
Preconditions: a SignupRequest with statusCode=PENDING
Steps        : 1. PATCH {decision: "REJECT"}
Expected     : 200; SignupRequest set REJECTED; no User created
Test data    : pending signup for "reject-me@example.com"
<!-- TC:TC-SEC-005:END -->

<!-- TC:TC-SEC-006:START traces=AC-SEC-006,REQ-SEC-006,API-SEC-003 -->
### TC-SEC-006 — issue a password-reset token
Derived from : AC-SEC-006 (REQ-SEC-006)
Exercises    : API-SEC-003 POST /api/v1/sec/auth/password-reset/request
Rule / code  : — (happy path)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: user identifies by a registered email
Steps        : 1. POST {email}
Expected     : 200 generic confirmation (same shape whether or not the email exists); a PasswordResetToken created with expiresAt = requestedAt + 30 min; no password changed yet
Test data    : email "u1@example.com"
<!-- TC:TC-SEC-006:END -->

<!-- TC:TC-SEC-007:START traces=AC-SEC-007,REQ-SEC-007,API-SEC-004 -->
### TC-SEC-007 — complete a password reset successfully
Derived from : AC-SEC-007 (REQ-SEC-007)
Exercises    : API-SEC-004 POST /api/v1/sec/auth/password-reset/complete
Rule / code  : — (happy path)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: an unexpired, unused PasswordResetToken; a new password meeting the platform's rules
Steps        : 1. POST {token, newPassword}
Expected     : 200; user's password hash updated; token's usedAt set; one PASSWORD_RESET_COMPLETED audit entry
Test data    : valid token; newPassword "N3wP@ssw0rd!"
<!-- TC:TC-SEC-007:END -->

<!-- TC:TC-SEC-009:START traces=AC-SEC-009,REQ-SEC-009,API-SEC-006 -->
### TC-SEC-009 — create a user directly
Derived from : AC-SEC-009 (REQ-SEC-009)
Exercises    : API-SEC-006 POST /api/v1/sec/users
Rule / code  : — (happy path)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: unique username/email; both name labels supplied
Steps        : 1. POST {username, email, fullNameAr, fullNameEn, password}
Expected     : 201; User created with statusCode=ACTIVE (default)
Test data    : username "u2", email "u2@example.com"
<!-- TC:TC-SEC-009:END -->

<!-- TC:TC-SEC-010:START traces=AC-SEC-010,REQ-SEC-010,API-SEC-008 -->
### TC-SEC-010 — assign one or more roles to a user
Derived from : AC-SEC-010 (REQ-SEC-010)
Exercises    : API-SEC-008 PUT /api/v1/sec/users/{id}/roles
Rule / code  : — (happy path)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: one or more active, non-conflicting roles selected
Steps        : 1. PUT {roleIds: [r1, r2]}
Expected     : 200; one UserRoleAssignment row per selected role
Test data    : two non-conflicting active roles
<!-- TC:TC-SEC-010:END -->

<!-- TC:TC-SEC-011:START traces=AC-SEC-011,REQ-SEC-011,API-SEC-009 -->
### TC-SEC-011 — deactivate a user terminates its sessions
Derived from : AC-SEC-011 (REQ-SEC-011)
Exercises    : API-SEC-009 DELETE /api/v1/sec/users/{id}
Rule / code  : — (happy path)
Scenario     : STATE · data class VALID · language ALL
Preconditions: an active user with one active session
Steps        : 1. DELETE the user
Expected     : 200; statusCode=DISABLED; the active session terminated; a subsequent login attempt (TC-SEC-002 style) then rejected
Test data    : user with 1 open ActiveSession
<!-- TC:TC-SEC-011:END -->

<!-- TC:TC-SEC-012:START traces=AC-SEC-012,REQ-SEC-012,API-SEC-014 -->
### TC-SEC-012 — grant a module to a role
Derived from : AC-SEC-012 (REQ-SEC-012)
Exercises    : API-SEC-014 POST /api/v1/sec/roles/{id}/modules
Rule / code  : — (happy path)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: active role and active registered module
Steps        : 1. POST {moduleId}
Expected     : 201; one RoleModuleGrant row created; one MODULE_GRANTED audit entry
Test data    : role R1, module FIN
<!-- TC:TC-SEC-012:END -->

<!-- TC:TC-SEC-016:START traces=AC-SEC-016,REQ-SEC-016,API-SEC-018 -->
### TC-SEC-016 — register a new module
Derived from : AC-SEC-016 (REQ-SEC-016)
Exercises    : API-SEC-018 POST /api/v1/sec/registry/modules
Rule / code  : — (happy path)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: module code not yet registered
Steps        : 1. POST {code, nameAr, nameEn}
Expected     : 201; one active ModuleRegistry row
Test data    : code "TST"
<!-- TC:TC-SEC-016:END -->

<!-- TC:TC-SEC-017:START traces=AC-SEC-017,REQ-SEC-017,API-SEC-019 -->
### TC-SEC-017 — register a screen under a registered module
Derived from : AC-SEC-017 (REQ-SEC-017)
Exercises    : API-SEC-019 POST /api/v1/sec/registry/screens
Rule / code  : — (happy path)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: a registered, active module
Steps        : 1. POST {moduleCode, pageCode, nameAr, nameEn}
Expected     : 201; one active ScreenRegistry row under that module
Test data    : moduleCode "TST", pageCode "TST_SCREEN"
<!-- TC:TC-SEC-017:END -->

<!-- TC:TC-SEC-019:START traces=AC-SEC-019,REQ-SEC-019,API-SEC-020 -->
### TC-SEC-019 — register an action under a registered screen
Derived from : AC-SEC-019 (REQ-SEC-019)
Exercises    : API-SEC-020 POST /api/v1/sec/registry/actions
Rule / code  : — (happy path)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: a registered, active screen
Steps        : 1. POST {pageCode, actionCode, nameAr, nameEn}
Expected     : 201; one active ActionRegistry row with permissionCode derived as PERM_<pageCode>_<actionCode>
Test data    : pageCode "TST_SCREEN", actionCode "VIEW"
<!-- TC:TC-SEC-019:END -->

<!-- TC:TC-SEC-021:START traces=AC-SEC-021,REQ-SEC-021,API-SEC-027 -->
### TC-SEC-021 — menu shows only effective grants
Derived from : AC-SEC-021 (REQ-SEC-021)
Exercises    : API-SEC-027 GET /api/v1/sec/menu
Rule / code  : — (POL-SEC-006)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: a user whose roles' union grants exactly module FIN with 2 screens
Steps        : 1. GET the effective menu
Expected     : 200; response shows only FIN with exactly those 2 screens beneath it
Test data    : role granting FIN + 2 screens only
<!-- TC:TC-SEC-021:END -->

<!-- TC:TC-SEC-022:START traces=AC-SEC-022,REQ-SEC-022,API-SEC-022 -->
### TC-SEC-022 — dashboard figures computed live
Derived from : AC-SEC-022 (REQ-SEC-022)
Exercises    : API-SEC-022 GET /api/v1/sec/dashboard
Rule / code  : — (POL-SEC-010)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: the dashboard is opened by an authorized administrator
Steps        : 1. GET the dashboard twice with a data change (e.g. a new login) in between
Expected     : 200 both times; the second response reflects the change (no cached/stale counter)
Test data    : any authorized admin session
<!-- TC:TC-SEC-022:END -->

<!-- TC:TC-SEC-023:START traces=AC-SEC-023,REQ-SEC-023,API-SEC-022 -->
### TC-SEC-023 — hide an ungranted dashboard widget
Derived from : AC-SEC-023 (REQ-SEC-023)
Exercises    : API-SEC-022 GET /api/v1/sec/dashboard
Rule / code  : — (POL-SEC-011)
Scenario     : PERMISSION · data class VALID · language ALL
Preconditions: an administrator role without the active-sessions widget's permission
Steps        : 1. GET the dashboard as that role's user
Expected     : 200; the active-sessions widget field is absent from the response
Test data    : role without PERM_SEC_SESSIONS_VIEW
<!-- TC:TC-SEC-023:END -->

<!-- TC:TC-SEC-024:START traces=AC-SEC-024,REQ-SEC-024,API-SEC-001 -->
### TC-SEC-024 — every security event appends an immutable audit entry
Derived from : AC-SEC-024 (REQ-SEC-024)
Exercises    : (side-effect of API-SEC-001/006/008/009/010/011/014/015/016/017/018/019/020/026 — not its own endpoint)
Rule / code  : — (POL-SEC-009)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: any of the listed events occurs (using login as the representative case)
Steps        : 1. perform a login → 2. attempt to update or delete the resulting AuditLogEntry (no such endpoint exists)
Expected     : one AuditLogEntry with correct eventTypeCode/actor/timestamp appended; step 2 has no route to succeed (immutability by omission)
Test data    : any login
<!-- TC:TC-SEC-024:END -->

<!-- TC:TC-SEC-025:START traces=AC-SEC-025,REQ-SEC-025,API-SEC-023 -->
### TC-SEC-025 — search and filter the audit log without altering it
Derived from : AC-SEC-025 (REQ-SEC-025)
Exercises    : API-SEC-023 POST /api/v1/sec/audit-log/search
Rule / code  : — (happy path)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: audit entries across several event types and dates
Steps        : 1. POST /search filtered by eventTypeCode and date range
Expected     : 200; exactly the matching entries, byte-identical to their stored values
Test data    : filter eventTypeCode=LOGIN_FAILED
<!-- TC:TC-SEC-025:END -->

<!-- TC:TC-SEC-026:START traces=AC-SEC-026,REQ-SEC-026,API-SEC-024 -->
### TC-SEC-026 — export the audit log
Derived from : AC-SEC-026 (REQ-SEC-026)
Exercises    : API-SEC-024 GET /api/v1/sec/audit-log/export
Rule / code  : — (happy path)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: a filtered audit-log view
Steps        : 1. GET the export with the same filters
Expected     : 200 text/csv; rows exactly match the filtered set's fields
Test data    : same filter as TC-SEC-025
<!-- TC:TC-SEC-026:END -->

<!-- TC:TC-SEC-027:START traces=AC-SEC-027,REQ-SEC-027,API-SEC-025 -->
### TC-SEC-027 — list only non-terminated sessions
Derived from : AC-SEC-027 (REQ-SEC-027)
Exercises    : API-SEC-025 POST /api/v1/sec/sessions/search
Rule / code  : — (happy path)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: several sessions, some terminated and some not
Steps        : 1. POST /search for active sessions
Expected     : 200; only non-terminated sessions listed, each with user + last-activity time
Test data    : 2 active + 1 terminated session
<!-- TC:TC-SEC-027:END -->

<!-- TC:TC-SEC-028:START traces=AC-SEC-028,REQ-SEC-028,API-SEC-026 -->
### TC-SEC-028 — force-terminate a session
Derived from : AC-SEC-028 (REQ-SEC-028)
Exercises    : API-SEC-026 DELETE /api/v1/sec/sessions/{id}
Rule / code  : — (happy path)
Scenario     : STATE · data class VALID · language ALL
Preconditions: an active session
Steps        : 1. DELETE the session
Expected     : 200; terminatedAt/terminatedBy set; the session's token no longer accepted for a subsequent request
Test data    : one active session
<!-- TC:TC-SEC-028:END -->

<!-- TC:TC-SEC-029:START traces=AC-SEC-029,REQ-SEC-029,API-SEC-003 -->
### TC-SEC-029 — optional notification on password reset
Derived from : AC-SEC-029 (REQ-SEC-029)
Exercises    : API-SEC-003 POST /api/v1/sec/auth/password-reset/request
Rule / code  : — (optional integration, §8)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: the Notifications integration is enabled; a reset token is issued
Steps        : 1. request a password reset
Expected     : one notification dispatch with a password-reset templateCode; REQ-SEC-006 still succeeds unchanged if Notifications is disabled/unavailable (verify as a second run with the integration off)
Test data    : email "u1@example.com"; integration flag ON then OFF
<!-- TC:TC-SEC-029:END -->

<!-- TC:TC-SEC-031:START traces=AC-SEC-031,REQ-SEC-031,API-SEC-010 -->
### TC-SEC-031 — reactivate a disabled user
Derived from : AC-SEC-031 (REQ-SEC-031)
Exercises    : API-SEC-010 PATCH /api/v1/sec/users/{id}
Rule / code  : — (happy path)
Scenario     : STATE · data class VALID · language ALL
Preconditions: a user with statusCode=DISABLED
Steps        : 1. PATCH to reactivate
Expected     : 200; statusCode=ACTIVE; a subsequent login with correct credentials succeeds
Test data    : a previously deactivated user
<!-- TC:TC-SEC-031:END -->

<!-- TC:TC-SEC-032:START traces=AC-SEC-032,REQ-SEC-032,API-SEC-027 -->
### TC-SEC-032 — hide an ungranted module from the menu
Derived from : AC-SEC-032 (REQ-SEC-032)
Exercises    : API-SEC-027 GET /api/v1/sec/menu
Rule / code  : — (POL-SEC-007, first half)
Scenario     : PERMISSION · data class VALID · language ALL
Preconditions: a user whose roles hold no grant for module FIN
Steps        : 1. GET the effective menu
Expected     : 200; FIN does not appear anywhere in the response
Test data    : user with zero FIN grants
<!-- TC:TC-SEC-032:END -->

<!-- TC:TC-SEC-033:START traces=AC-SEC-033,REQ-SEC-033,API-SEC-001 -->
### TC-SEC-033 — module gate enforced on direct access, independent of the menu
Derived from : AC-SEC-033 (REQ-SEC-033)
Exercises    : any FIN endpoint directly (e.g. API-FIN-001), gated by the CORE interceptor
Rule / code  : — (POL-SEC-007, second half)
Scenario     : PERMISSION · data class ATTACK · language ALL
Preconditions: a user whose roles hold no grant for module FIN
Steps        : 1. call a FIN endpoint directly by URL, bypassing the menu entirely
Expected     : 403 (denied), regardless of how the request was reached
Test data    : user with zero FIN grants; direct call to GET /api/v1/fin/accounts
<!-- TC:TC-SEC-033:END -->

<!-- TC:TC-SEC-034:START traces=AC-SEC-034,REQ-SEC-034 -->
### TC-SEC-034 — cross-module read of a user's contact details
Derived from : AC-SEC-034 (REQ-SEC-034)
Exercises    : `com.erp.sec.crossmodule.SecUserDirectoryApi.findContact(userPk)` — an injected cross-module Spring interface, NOT an HTTP endpoint, so no API-SEC-* is cited and this TC is driven in-process the way a consuming module's service calls it (`build-create-service`: cross-module reads use "direct Spring interface injection, not loopback HTTP")
Rule / code  : — (no error code; an unknown id is an empty result, never a 404)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: one ACTIVE user and one DISABLED user exist; the caller is authenticated (the producing side is gated `isAuthenticated()`)
Steps        : 1. ask the interface for the ACTIVE user's contact by user id 2. repeat for the DISABLED user 3. repeat for a user id that does not exist
Expected     : 1. a contact carrying exactly userPk, email, fullNameAr, fullNameEn and active=true — and nothing else: no password hash (POL-SEC-004), no reset or session token, no audit row, no role/grant/session data, and neither the User entity nor an internal DTO 2. the same shape with active=false 3. an empty result, not an error
Test data    : an ACTIVE user "u1@example.com" with both display names set; a DISABLED user; an unused id
<!-- TC:TC-SEC-034:END -->

<!-- TC:TC-SEC-035:START traces=AC-SEC-035,REQ-SEC-035 -->
### TC-SEC-035 — cross-module read of the user ids holding a permission code
Derived from : AC-SEC-035 (REQ-SEC-035)
Exercises    : `com.erp.sec.crossmodule.SecUserDirectoryApi.findUserIdsHoldingPermission(permissionCode)` (QR-SEC-039) — the same injected interface, not an HTTP endpoint; driven in-process as a consuming module's separation-of-duties check would
Rule / code  : — (no error code; a code nobody holds is an empty set, never a 404)
Scenario     : PERMISSION · data class VALID · language ALL
Preconditions: two permission codes the consumer treats as conflicting, each registered as an ACTIVE action; user A holds the first through one ACTIVE role, user B holds it through a second ACTIVE role, user C holds it only through an INACTIVE role, and user D holds an action row that is itself INACTIVE
Steps        : 1. ask the interface for the holders of the first permission code 2. ask for the holders of the second 3. ask for a permission code no action row carries
Expected     : 1. exactly the ids of A and B, each once (DISTINCT), with C excluded because its role is inactive and D excluded because its action is inactive 2. the holder set of the second code, computed the same way 3. an empty list; and in every case nothing but user ids is returned — no name, email, role or grant detail
Test data    : users A, B, C, D; roles R_ACTIVE_1, R_ACTIVE_2, R_INACTIVE; an inactive action row; an unheld permission code
<!-- TC:TC-SEC-035:END -->
<!-- SUB:API-SCENARIOS:END -->
<!-- PHASE:TEST-PLAN-BE:END -->

No `INT-XM` phase — SEC is ROOT; it declares no `XM-*` (db-script-sec.md §2). Not an
empty block, simply absent, per §2/§4 of the engine.

## TC TRACEABILITY INDEX
| AC | TC | REQ | API | RULE/code | XM |
|---|---|---|---|---|---|
| AC-SEC-001…033 | TC-SEC-001…033 (1:1) | REQ-SEC-001…033 (1:1) | see each TC's Exercises line | see each TC's Rule/code line | none |
| AC-SEC-034, AC-SEC-035 | TC-SEC-034, TC-SEC-035 (1:1) | REQ-SEC-034, REQ-SEC-035 (1:1) | none — `SecUserDirectoryApi`, a crossmodule Spring interface, not an HTTP endpoint | — (reads only, no error code) | none registered by SEC — ids for the exposed direction belong to the consuming module's P2 |

## COVERAGE
AC covered 35/35 (0 gaps) · REQ covered 35/35 (REQ-SEC-034/035 added by the 2026-09-11
cross-module amendment) · API covered 27/27 (every API-SEC-001..027
exercised by ≥1 TC, either directly or as the side-effect target of TC-SEC-024) · every
selected-module XM covered: not applicable (SEC declares none).
══════════════════════════════════════════════════════════════════
