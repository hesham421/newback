<!-- source: PHASE:TEST-PLAN-BE / SUB:RULE-SCENARIOS -->
<!-- traces: AC-SEC-008, AC-SEC-013, AC-SEC-014, AC-SEC-015, AC-SEC-018, AC-SEC-020, AC-SEC-030, API-SEC-004, API-SEC-008, API-SEC-015, API-SEC-016, API-SEC-017, API-SEC-019, REQ-SEC-008, REQ-SEC-013, REQ-SEC-014, REQ-SEC-015, REQ-SEC-018, REQ-SEC-020, REQ-SEC-030 -->
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
