<!-- source: PHASE:TEST-PLAN-BE / SUB:API-SCENARIOS -->
<!-- traces: AC-SEC-001, AC-SEC-002, AC-SEC-003, AC-SEC-004, AC-SEC-005, AC-SEC-006, AC-SEC-007, AC-SEC-009, AC-SEC-010, AC-SEC-011, AC-SEC-012, AC-SEC-016, AC-SEC-017, AC-SEC-019, AC-SEC-021, AC-SEC-022, AC-SEC-023, AC-SEC-024, AC-SEC-025, AC-SEC-026, AC-SEC-027, AC-SEC-028, AC-SEC-029, AC-SEC-031, AC-SEC-032, AC-SEC-033, API-SEC-001, API-SEC-002, API-SEC-003, API-SEC-004, API-SEC-006, API-SEC-008, API-SEC-009, API-SEC-010, API-SEC-011, API-SEC-014, API-SEC-018, API-SEC-019, API-SEC-020, API-SEC-022, API-SEC-023, API-SEC-024, API-SEC-025, API-SEC-026, API-SEC-027, REQ-SEC-001, REQ-SEC-002, REQ-SEC-003, REQ-SEC-004, REQ-SEC-005, REQ-SEC-006, REQ-SEC-007, REQ-SEC-009, REQ-SEC-010, REQ-SEC-011, REQ-SEC-012, REQ-SEC-016, REQ-SEC-017, REQ-SEC-019, REQ-SEC-021, REQ-SEC-022, REQ-SEC-023, REQ-SEC-024, REQ-SEC-025, REQ-SEC-026, REQ-SEC-027, REQ-SEC-028, REQ-SEC-029, REQ-SEC-031, REQ-SEC-032, REQ-SEC-033 -->
<!-- SUB:API-SCENARIOS:START traces=REQ-SEC-001,REQ-SEC-002,REQ-SEC-003,REQ-SEC-004,REQ-SEC-005,REQ-SEC-006,REQ-SEC-007,REQ-SEC-009,REQ-SEC-010,REQ-SEC-011,REQ-SEC-012,REQ-SEC-016,REQ-SEC-017,REQ-SEC-019,REQ-SEC-021,REQ-SEC-022,REQ-SEC-023,REQ-SEC-024,REQ-SEC-025,REQ-SEC-026,REQ-SEC-027,REQ-SEC-028,REQ-SEC-029,REQ-SEC-031,REQ-SEC-032,REQ-SEC-033 -->
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
Exercises    : API-SEC-023 GET /api/v1/sec/audit-log
Rule / code  : — (happy path)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: audit entries across several event types and dates
Steps        : 1. GET filtered by eventTypeCode and date range
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
Exercises    : API-SEC-025 GET /api/v1/sec/sessions
Rule / code  : — (happy path)
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: several sessions, some terminated and some not
Steps        : 1. GET active sessions
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
<!-- SUB:API-SCENARIOS:END -->
