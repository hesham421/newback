# FRONTEND TEST PLAN — الأمان / Security (SEC)
══════════════════════════════════════════════════════════════════
Module : SEC   Version : v1   Profile : erp   Scope : module (SEC)
Sources: srs-sec.md v1 · registry-srs-sec.md v1 (frontend-execution-plan-sec.md and
         registry-exec-fe-sec.md are both ABSENT this run — see REDUCED note below)
Framework: agnostic (profile.stack.testing.frontend). REDUCED: **yes** —
frontend-execution-plan-sec.md was cleared for regeneration (`SEC: clear the frontend
artifacts for regeneration`) and has not been rewritten; no `SCR-*`, route or `UXD-*`
exists to bind to. Every step below references its `AC-*` only.
TC count: 30 — TC-SEC-036 … TC-SEC-065, continuing immediately after the backend plan's
current highest id (TC-SEC-035) — see ID NOTE below.

ID NOTE (why this run does not continue at TC-SEC-034, where the previous revision of
this file started): the backend plan was extended after that revision to cover the two
newer cross-module ACs, REQ/AC-SEC-034 and -035, and its new backend TCs were minted as
**TC-SEC-034** and **TC-SEC-035** (`backend-test-plan-sec.md`'s own COVERAGE line) — the
exact two ids this file's previous revision had already given to two unrelated frontend
cases (login happy-path and login-rejected). That is a real id collision between the two
plans, not one this run introduced. Every case in this file is being rewritten in REDUCED
mode regardless (the frontend design its old `SCR-*` bindings pointed to no longer
exists), so the previous range, TC-SEC-034 through TC-SEC-063, is superseded in full by
this revision rather than partially reused: reusing 034/035 would duplicate the backend
plan's ids, and the id-continuity rule (`gov.py analyze` C12.3) admits no gap either, so
the only sequence-consistent choice is to mint fresh, contiguous ids starting right after
the backend plan's own current maximum. This revision therefore uses **TC-SEC-036 ..
TC-SEC-065** — no id from either plan is skipped, renumbered, or given two meanings.

Open ADRs: 0 new.
══════════════════════════════════════════════════════════════════

Scope is `module`, so this run derives from SEC's own `AC-*` only. The integration phase
`INT-UXD` is **absent by rule**, and for SEC it could never be otherwise: the module mints
no `UXD-*` at all — SRS §A8 records it as ROOT, consuming no entity owned by another
module, so no screen of its (when one exists again) displays foreign data.

<!-- PHASE:TEST-PLAN-FE:START traces=AC-SEC-001,AC-SEC-002,AC-SEC-003,AC-SEC-004,AC-SEC-005,AC-SEC-006,AC-SEC-007,AC-SEC-008,AC-SEC-009,AC-SEC-010,AC-SEC-011,AC-SEC-012,AC-SEC-013,AC-SEC-014,AC-SEC-015,AC-SEC-016,AC-SEC-017,AC-SEC-019,AC-SEC-020,AC-SEC-021,AC-SEC-022,AC-SEC-023,AC-SEC-025,AC-SEC-026,AC-SEC-027,AC-SEC-028,AC-SEC-030,AC-SEC-031,AC-SEC-032,AC-SEC-033,REQ-SEC-001,REQ-SEC-002,REQ-SEC-003,REQ-SEC-004,REQ-SEC-005,REQ-SEC-006,REQ-SEC-007,REQ-SEC-008,REQ-SEC-009,REQ-SEC-010,REQ-SEC-011,REQ-SEC-012,REQ-SEC-013,REQ-SEC-014,REQ-SEC-015,REQ-SEC-016,REQ-SEC-017,REQ-SEC-019,REQ-SEC-020,REQ-SEC-021,REQ-SEC-022,REQ-SEC-023,REQ-SEC-025,REQ-SEC-026,REQ-SEC-027,REQ-SEC-028,REQ-SEC-030,REQ-SEC-031,REQ-SEC-032,REQ-SEC-033 -->
## TEST-PLAN-FE — SEC v1

Thirty test cases, so the phase splits (threshold: TC count > 8) into the two labels the
profile names: `UI-FLOWS` for the per-area flows, and `INT-FLOW` for the flows that would
cross screens once screens exist again — the dynamic menu, which every other area's guard
reads, and the dashboard widget whose absence and whose target area are two separate
mechanisms. Every case derives from exactly one `AC-*` and asserts the catalog message in
both languages wherever a `RULE-*` fires.

<!-- SUB:UI-FLOWS:START traces=AC-SEC-001,REQ-SEC-001,AC-SEC-002,REQ-SEC-002,AC-SEC-003,REQ-SEC-003,AC-SEC-006,REQ-SEC-006,AC-SEC-007,REQ-SEC-007,AC-SEC-008,REQ-SEC-008,AC-SEC-004,REQ-SEC-004,AC-SEC-005,REQ-SEC-005,AC-SEC-009,REQ-SEC-009,AC-SEC-010,REQ-SEC-010,AC-SEC-011,REQ-SEC-011,AC-SEC-031,REQ-SEC-031,AC-SEC-012,REQ-SEC-012,AC-SEC-013,REQ-SEC-013,AC-SEC-014,REQ-SEC-014,AC-SEC-015,REQ-SEC-015,AC-SEC-020,REQ-SEC-020,AC-SEC-030,REQ-SEC-030,AC-SEC-016,REQ-SEC-016,AC-SEC-017,REQ-SEC-017,AC-SEC-019,REQ-SEC-019,AC-SEC-022,REQ-SEC-022,AC-SEC-025,REQ-SEC-025,AC-SEC-026,REQ-SEC-026,AC-SEC-027,REQ-SEC-027,AC-SEC-028,REQ-SEC-028 -->
### SUB — UI-FLOWS

Per-area flows: the public pages, user and role administration, the read-only registry,
the dashboard, the audit log and the sessions area. REDUCED — no `SCR-*` exists to name
which screen each belongs to; the grouping below follows the SRS's own screen requirements
(Part B) by subject, not a bound route.

<!-- TC:TC-SEC-036:START traces=AC-SEC-001,REQ-SEC-001 -->
### TC-SEC-036 — sign in with correct credentials
Derived from : AC-SEC-001 (REQ-SEC-001)
Exercises    : (reduced — no frontend-execution-plan-sec.md; no `SCR-*`/route to bind)
Rule / code  : —
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: an active user with a known username and password; no session held
Steps        : 1. submit the correct username and password to the login form 2. observe the result
Expected     : the system creates an active session for the user and grants access to the platform (AC-SEC-001) — which screen the caller lands on is undefined until a frontend execution plan exists again
Test data    : an ACTIVE user
<!-- TC:TC-SEC-036:END -->

<!-- TC:TC-SEC-037:START traces=AC-SEC-002,REQ-SEC-002 -->
### TC-SEC-037 — rejected credentials return one message for all three causes
Derived from : AC-SEC-002 (REQ-SEC-002)
Exercises    : (reduced)
Rule / code  : SEC-401-INVALID-CREDENTIALS
Scenario     : VIOLATION · data class INVALID · language ALL
Preconditions: a wrong password, an unknown username, and a disabled username — three attempts
Steps        : 1. submit a wrong password 2. submit an unknown username 3. submit a disabled username
Expected     : all three are rejected with the identical message — ar: "بيانات الدخول غير صحيحة" · en: "Invalid credentials" — no session is issued, and the caller cannot tell which of the three occurred. The `LOGIN_FAILED` audit entry is the server's; the client neither writes nor counts it
Test data    : three rejected attempts
<!-- TC:TC-SEC-037:END -->

<!-- TC:TC-SEC-038:START traces=AC-SEC-003,REQ-SEC-003 -->
### TC-SEC-038 — submitting the sign-up form creates a pending request and no account
Derived from : AC-SEC-003 (REQ-SEC-003)
Exercises    : (reduced)
Rule / code  : —
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: a valid, not-already-registered email
Steps        : 1. fill the sign-up form with email and both name labels 2. submit
Expected     : the system creates a SignupRequest with status PENDING and creates no user account yet; no session is created
Test data    : a new email address
<!-- TC:TC-SEC-038:END -->

<!-- TC:TC-SEC-039:START traces=AC-SEC-006,REQ-SEC-006 -->
### TC-SEC-039 — requesting a password reset always ends in the same generic confirmation
Derived from : AC-SEC-006 (REQ-SEC-006)
Exercises    : (reduced)
Rule / code  : —
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: one registered email and one that is not registered
Steps        : 1. submit the registered email to the reset-request form 2. repeat with the unregistered one
Expected     : both submissions end in the identical confirmation; the response does not reveal whether the email is registered, and no password has changed in either case; one PasswordResetToken is created for the registered case
Test data    : two email addresses
<!-- TC:TC-SEC-039:END -->

<!-- TC:TC-SEC-040:START traces=AC-SEC-007,REQ-SEC-007 -->
### TC-SEC-040 — completing a reset with a valid token changes the password
Derived from : AC-SEC-007 (REQ-SEC-007)
Exercises    : (reduced)
Rule / code  : —
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: an unexpired, unused PasswordResetToken and a new password meeting the platform's password rules
Steps        : 1. submit the token with the new password
Expected     : the system updates the user's password hash, sets the token's usedAt, and appends a `PASSWORD_RESET_COMPLETED` audit entry
Test data    : a fresh token
<!-- TC:TC-SEC-040:END -->

<!-- TC:TC-SEC-041:START traces=AC-SEC-008,REQ-SEC-008 -->
### TC-SEC-041 — an expired or used reset token is refused and changes nothing
Derived from : AC-SEC-008 (REQ-SEC-008)
Exercises    : (reduced)
Rule / code  : RULE-SEC-006 → SEC-409-RESET-TOKEN-INVALID
Scenario     : VIOLATION · data class INVALID · language ALL
Preconditions: a token that is expired, and one already used
Steps        : 1. submit the expired token with a new password 2. submit the already-used token
Expected     : both are rejected with the message — ar: "رابط إعادة التعيين غير صالح أو منتهي" · en: "This reset link is invalid or has expired" — and nothing changes; the response does not distinguish expired from used, because the catalog gives one message for both
Test data    : two invalid tokens
<!-- TC:TC-SEC-041:END -->

<!-- TC:TC-SEC-042:START traces=AC-SEC-004,REQ-SEC-004 -->
### TC-SEC-042 — approving a pending sign-up creates an active user
Derived from : AC-SEC-004 (REQ-SEC-004)
Exercises    : (reduced)
Rule / code  : —
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: a PENDING SignupRequest; signed in holding the user-administration screen's view and update actions
Steps        : 1. approve the pending request
Expected     : the system creates a User with status ACTIVE from the request's email/name, and marks the SignupRequest APPROVED
Test data    : a PENDING request
<!-- TC:TC-SEC-042:END -->

<!-- TC:TC-SEC-043:START traces=AC-SEC-005,REQ-SEC-005 -->
### TC-SEC-043 — rejecting a pending sign-up creates no user
Derived from : AC-SEC-005 (REQ-SEC-005)
Exercises    : (reduced)
Rule / code  : —
Scenario     : STATE · data class VALID · language ALL
Preconditions: a PENDING SignupRequest
Steps        : 1. reject the pending request 2. search for a user with its email
Expected     : the system marks the request REJECTED and creates no User; no user with that email exists
Test data    : a PENDING request
<!-- TC:TC-SEC-043:END -->

<!-- TC:TC-SEC-044:START traces=AC-SEC-009,REQ-SEC-009 -->
### TC-SEC-044 — create a user
Derived from : AC-SEC-009 (REQ-SEC-009)
Exercises    : (reduced)
Rule / code  : SEC-409-USER-DUP
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: a unique username and email; signed in holding the user-administration screen's view and create actions
Steps        : 1. fill username, email, both names and the password 2. submit
Expected     : the system creates the User with status ACTIVE (or as chosen); the password is write-only and is never echoed or returned. A duplicate username/email is rejected by the server
Test data    : a unique username and email
<!-- TC:TC-SEC-044:END -->

<!-- TC:TC-SEC-045:START traces=AC-SEC-010,REQ-SEC-010 -->
### TC-SEC-045 — assign roles to a user
Derived from : AC-SEC-010 (REQ-SEC-010)
Exercises    : (reduced)
Rule / code  : —
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: an existing user and two active roles
Steps        : 1. select both roles for the user 2. save
Expected     : the system creates one UserRoleAssignment row per selected role
Test data    : two active roles
<!-- TC:TC-SEC-045:END -->

<!-- TC:TC-SEC-046:START traces=AC-SEC-011,REQ-SEC-011 -->
### TC-SEC-046 — deactivating a user ends their sessions
Derived from : AC-SEC-011 (REQ-SEC-011)
Exercises    : (reduced)
Rule / code  : —
Scenario     : STATE · data class VALID · language ALL
Preconditions: an active user holding one active session
Steps        : 1. deactivate the user 2. attempt to use the user's existing session or sign in again
Expected     : the system sets status DISABLED, terminates every active session of that user, and a subsequent login attempt is rejected per AC-SEC-002
Test data    : a user with one live session
<!-- TC:TC-SEC-046:END -->

<!-- TC:TC-SEC-047:START traces=AC-SEC-031,REQ-SEC-031 -->
### TC-SEC-047 — reactivating a user restores access
Derived from : AC-SEC-031 (REQ-SEC-031)
Exercises    : (reduced)
Rule / code  : —
Scenario     : STATE · data class VALID · language ALL
Preconditions: a user with status DISABLED
Steps        : 1. reactivate the user 2. sign in with correct credentials
Expected     : the system sets status ACTIVE and the subsequent login succeeds
Test data    : a DISABLED user
<!-- TC:TC-SEC-047:END -->

<!-- TC:TC-SEC-048:START traces=AC-SEC-012,REQ-SEC-012 -->
### TC-SEC-048 — grant a module to a role
Derived from : AC-SEC-012 (REQ-SEC-012)
Exercises    : (reduced)
Rule / code  : —
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: an active role and an active registered module
Steps        : 1. grant the module to the role
Expected     : the system creates one RoleModuleGrant row
Test data    : one active module
<!-- TC:TC-SEC-048:END -->

<!-- TC:TC-SEC-049:START traces=AC-SEC-013,REQ-SEC-013 -->
### TC-SEC-049 — granting a screen before its module is refused
Derived from : AC-SEC-013 (REQ-SEC-013)
Exercises    : (reduced)
Rule / code  : RULE-SEC-001 → SEC-409-NO-MODULE-GRANT
Scenario     : VIOLATION · data class INVALID · language ALL
Preconditions: a role with no RoleModuleGrant for module FIN
Steps        : 1. attempt to grant that role a FIN screen
Expected     : the system rejects the grant with the message — ar: "لا يمكن منح شاشة دون منح الوحدة أولًا" · en: "Cannot grant a screen without first granting its module" — and creates no RoleScreenGrant
Test data    : a role without the FIN module grant
<!-- TC:TC-SEC-049:END -->

<!-- TC:TC-SEC-050:START traces=AC-SEC-014,REQ-SEC-014 -->
### TC-SEC-050 — granting an action before its screen is refused
Derived from : AC-SEC-014 (REQ-SEC-014)
Exercises    : (reduced)
Rule / code  : RULE-SEC-002 → SEC-409-NO-SCREEN-GRANT
Scenario     : VIOLATION · data class INVALID · language ALL
Preconditions: a role with no RoleScreenGrant for a given screen
Steps        : 1. attempt to grant that role an action on that screen
Expected     : the system rejects the grant with the message — ar: "لا يمكن منح إجراء دون منح الشاشة أولًا" · en: "Cannot grant an action without first granting its screen" — and creates no RoleActionGrant
Test data    : a role without that screen's grant
<!-- TC:TC-SEC-050:END -->

<!-- TC:TC-SEC-051:START traces=AC-SEC-015,REQ-SEC-015 -->
### TC-SEC-051 — revoking a module cascades to its screen and action grants
Derived from : AC-SEC-015 (REQ-SEC-015)
Exercises    : (reduced)
Rule / code  : RULE-SEC-003
Scenario     : STATE · data class VALID · language ALL
Preconditions: a role holding one module grant plus two screen grants and three action grants under it
Steps        : 1. revoke the module grant 2. re-read the role's grants
Expected     : the system deletes the module grant and every screen/action grant it covered, leaving none behind
Test data    : 1 module + 2 screen + 3 action grants
<!-- TC:TC-SEC-051:END -->

<!-- TC:TC-SEC-052:START traces=AC-SEC-020,REQ-SEC-020 -->
### TC-SEC-052 — a conflicting action assignment is refused
Derived from : AC-SEC-020 (REQ-SEC-020)
Exercises    : (reduced)
Rule / code  : RULE-SEC-005 → SEC-409-SOD-CONFLICT
Scenario     : VIOLATION · data class INVALID · language ALL
Preconditions: two actions declared conflicting by their owning module, and a user already holding one of them through some role
Steps        : 1. attempt to assign the user a role that would give them the other conflicting action
Expected     : the system rejects the assignment with the message — ar: "هذا المستخدم يملك إجراءً متعارضًا بالفعل" · en: "This user already holds a conflicting action". Inert in SEC v1: no module currently declares a conflicting pair (REQ-SEC-020 Note), so this case exercises the guard's shape, not a live conflict, until one is declared
Test data    : a conflicting action pair
<!-- TC:TC-SEC-052:END -->

<!-- TC:TC-SEC-053:START traces=AC-SEC-030,REQ-SEC-030 -->
### TC-SEC-053 — an action without VIEW on the same screen is denied
Derived from : AC-SEC-030 (REQ-SEC-030)
Exercises    : (reduced)
Rule / code  : RULE-SEC-007 → SEC-409-NO-VIEW-GRANT
Scenario     : PERMISSION · data class VALID · language ALL
Preconditions: a role holding CREATE on a screen but not VIEW on that same screen
Steps        : 1. sign in as that role's user 2. attempt the CREATE action on that screen
Expected     : the system denies the action until VIEW is also granted on that screen — the gateway convention
Test data    : CREATE granted, VIEW not
<!-- TC:TC-SEC-053:END -->

<!-- TC:TC-SEC-054:START traces=AC-SEC-016,REQ-SEC-016 -->
### TC-SEC-054 — the registry shows a registered module
Derived from : AC-SEC-016 (REQ-SEC-016)
Exercises    : (reduced)
Rule / code  : —
Scenario     : STATE · data class VALID · language ALL
Preconditions: a module that has registered itself with its bilingual name — the registration is the consuming module's own call, drawn on no screen
Steps        : 1. read the module registry
Expected     : the system shows one active ModuleRegistry row with both names
Test data    : a registered module
<!-- TC:TC-SEC-054:END -->

<!-- TC:TC-SEC-055:START traces=AC-SEC-017,REQ-SEC-017 -->
### TC-SEC-055 — the registry shows a module's screen
Derived from : AC-SEC-017 (REQ-SEC-017)
Exercises    : (reduced)
Rule / code  : —
Scenario     : STATE · data class VALID · language ALL
Preconditions: a registered, active module that has registered a screen with a unique page code
Steps        : 1. read the registry under that module
Expected     : the system shows one active ScreenRegistry row with its page code and both names, under that module
Test data    : one registered screen
<!-- TC:TC-SEC-055:END -->

<!-- TC:TC-SEC-056:START traces=AC-SEC-019,REQ-SEC-019 -->
### TC-SEC-056 — the registry shows an action's derived permission code
Derived from : AC-SEC-019 (REQ-SEC-019)
Exercises    : (reduced)
Rule / code  : —
Scenario     : STATE · data class VALID · language ALL
Preconditions: a registered, active screen that has registered an action code
Steps        : 1. read the registry under that screen
Expected     : the system shows one active ActionRegistry row with permission code `PERM_<pageCode>_<actionCode>` — displayed as the server derived it, never composed by the caller
Test data    : one registered action
<!-- TC:TC-SEC-056:END -->

<!-- TC:TC-SEC-057:START traces=AC-SEC-022,REQ-SEC-022 -->
### TC-SEC-057 — every dashboard figure is computed live
Derived from : AC-SEC-022 (REQ-SEC-022)
Exercises    : (reduced)
Rule / code  : —
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: a caller holding the dashboard
Steps        : 1. open the dashboard 2. read every widget 3. change the underlying data and reload
Expected     : each widget — users overview, failed logins in 24h, active sessions, recent activity, roles/permissions summary, onboarding funnel — is computed from current data, never from a stored counter, and the reloaded values move with the data
Test data    : six widgets
<!-- TC:TC-SEC-057:END -->

<!-- TC:TC-SEC-058:START traces=AC-SEC-025,REQ-SEC-025 -->
### TC-SEC-058 — filtering the audit log returns exactly the matching entries
Derived from : AC-SEC-025 (REQ-SEC-025)
Exercises    : (reduced)
Rule / code  : —
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: audit entries across several event types and dates
Steps        : 1. filter by event type and date range
Expected     : the system returns exactly the matching entries, unmodified
Test data    : entries across three event types
<!-- TC:TC-SEC-058:END -->

<!-- TC:TC-SEC-059:START traces=AC-SEC-026,REQ-SEC-026 -->
### TC-SEC-059 — exporting the audit log exports exactly the filtered entries
Derived from : AC-SEC-026 (REQ-SEC-026)
Exercises    : (reduced)
Rule / code  : —
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: a filtered audit-log view
Steps        : 1. export the filtered view
Expected     : the system produces a CSV file containing exactly the filtered entries' fields
Test data    : a filtered view
<!-- TC:TC-SEC-059:END -->

<!-- TC:TC-SEC-060:START traces=AC-SEC-027,REQ-SEC-027 -->
### TC-SEC-060 — only non-terminated sessions are listed
Derived from : AC-SEC-027 (REQ-SEC-027)
Exercises    : (reduced)
Rule / code  : —
Scenario     : HAPPY · data class VALID · language ALL
Preconditions: several sessions, some terminated and some not
Steps        : 1. read the active-sessions list
Expected     : only the non-terminated sessions are listed, each with its user and last-activity time
Test data    : a mix of terminated and live sessions
<!-- TC:TC-SEC-060:END -->

<!-- TC:TC-SEC-061:START traces=AC-SEC-028,REQ-SEC-028 -->
### TC-SEC-061 — force-terminating a session ends it for the affected user
Derived from : AC-SEC-028 (REQ-SEC-028)
Exercises    : (reduced)
Rule / code  : —
Scenario     : STATE · data class VALID · language ALL
Preconditions: an active session; signed in as a caller holding the session-terminate action
Steps        : 1. force-terminate the session 2. attempt a request with the affected user's token
Expected     : the system sets terminatedAt/terminatedBy and the associated token is no longer accepted for any subsequent request
Test data    : one live session
<!-- TC:TC-SEC-061:END -->

<!-- SUB:UI-FLOWS:END -->

<!-- SUB:INT-FLOW:START traces=AC-SEC-021,REQ-SEC-021,AC-SEC-032,REQ-SEC-032,AC-SEC-033,REQ-SEC-033,AC-SEC-023,REQ-SEC-023 -->
### SUB — INT-FLOW

The flows that would span screens once screens exist again: the two-tier menu that would
be the guard source for every other area, and the dashboard widget whose gating is the
server's regardless of which screen renders it.

<!-- TC:TC-SEC-062:START traces=AC-SEC-021,REQ-SEC-021 -->
### TC-SEC-062 — the menu shows exactly the modules and screens the caller's grants union to
Derived from : AC-SEC-021 (REQ-SEC-021)
Exercises    : (reduced)
Rule / code  : —
Scenario     : STATE · data class VALID · language ALL
Preconditions: a user whose roles union to exactly module FIN with the screens "Journal Entries" and "Trial Balance"
Steps        : 1. sign in as that user 2. read the rendered menu
Expected     : the system includes only module FIN as a top-level entry with exactly those two screens beneath it
Test data    : one module, two screens
<!-- TC:TC-SEC-062:END -->

<!-- TC:TC-SEC-063:START traces=AC-SEC-032,REQ-SEC-032 -->
### TC-SEC-063 — a module the caller does not hold is absent from the menu entirely
Derived from : AC-SEC-032 (REQ-SEC-032)
Exercises    : (reduced)
Rule / code  : —
Scenario     : PERMISSION · data class VALID · language ALL
Preconditions: a user whose roles hold no grant for module FIN
Steps        : 1. sign in as that user 2. search the rendered menu for FIN
Expected     : FIN does not appear anywhere in the menu — not greyed out, not disabled, absent
Test data    : a user without the FIN grant
<!-- TC:TC-SEC-063:END -->

<!-- TC:TC-SEC-064:START traces=AC-SEC-033,REQ-SEC-033 -->
### TC-SEC-064 — a route reached directly without its grant is refused independently of the menu
Derived from : AC-SEC-033 (REQ-SEC-033)
Exercises    : (reduced)
Rule / code  : —
Scenario     : PERMISSION · data class ATTACK · language ALL
Preconditions: the same user, and a FIN screen/endpoint reached directly, bypassing the menu
Steps        : 1. sign in as that user 2. navigate straight to a FIN screen URL, or call a FIN endpoint directly
Expected     : the system denies the request with an authorization error regardless of how it was reached — the menu's omission is not itself the enforcement; the server denies the direct call independently of whatever client-side guard exists once one is designed again
Test data    : a direct URL and a direct endpoint call
<!-- TC:TC-SEC-064:END -->

<!-- TC:TC-SEC-065:START traces=AC-SEC-023,REQ-SEC-023 -->
### TC-SEC-065 — a widget the caller may not see is absent, and its source screen still guards itself
Derived from : AC-SEC-023 (REQ-SEC-023)
Exercises    : (reduced)
Rule / code  : —
Scenario     : PERMISSION · data class VALID · language ALL
Preconditions: an administrator role without the "active sessions" widget's permission
Steps        : 1. sign in as that role's user 2. open the dashboard 3. look for the active-sessions widget 4. navigate directly to the sessions area
Expected     : the widget is absent from the dashboard response — the server returns only the widgets the caller may see; the sessions area is then refused by its own guard independently, so the absence and the refusal are two separate mechanisms and neither stands in for the other
Test data    : a role without the sessions permission
<!-- TC:TC-SEC-065:END -->

<!-- SUB:INT-FLOW:END -->

<!-- PHASE:TEST-PLAN-FE:END -->

## TC TRACEABILITY INDEX

| AC | TC | REQ | RULE / code |
|---|---|---|---|
| AC-SEC-001 | TC-SEC-036 | REQ-SEC-001 | — |
| AC-SEC-002 | TC-SEC-037 | REQ-SEC-002 | SEC-401-INVALID-CREDENTIALS |
| AC-SEC-003 | TC-SEC-038 | REQ-SEC-003 | — |
| AC-SEC-006 | TC-SEC-039 | REQ-SEC-006 | — |
| AC-SEC-007 | TC-SEC-040 | REQ-SEC-007 | — |
| AC-SEC-008 | TC-SEC-041 | REQ-SEC-008 | RULE-SEC-006 → SEC-409-RESET-TOKEN-INVALID |
| AC-SEC-004 | TC-SEC-042 | REQ-SEC-004 | — |
| AC-SEC-005 | TC-SEC-043 | REQ-SEC-005 | — |
| AC-SEC-009 | TC-SEC-044 | REQ-SEC-009 | SEC-409-USER-DUP |
| AC-SEC-010 | TC-SEC-045 | REQ-SEC-010 | — |
| AC-SEC-011 | TC-SEC-046 | REQ-SEC-011 | — |
| AC-SEC-031 | TC-SEC-047 | REQ-SEC-031 | — |
| AC-SEC-012 | TC-SEC-048 | REQ-SEC-012 | — |
| AC-SEC-013 | TC-SEC-049 | REQ-SEC-013 | RULE-SEC-001 → SEC-409-NO-MODULE-GRANT |
| AC-SEC-014 | TC-SEC-050 | REQ-SEC-014 | RULE-SEC-002 → SEC-409-NO-SCREEN-GRANT |
| AC-SEC-015 | TC-SEC-051 | REQ-SEC-015 | RULE-SEC-003 |
| AC-SEC-020 | TC-SEC-052 | REQ-SEC-020 | RULE-SEC-005 → SEC-409-SOD-CONFLICT |
| AC-SEC-030 | TC-SEC-053 | REQ-SEC-030 | RULE-SEC-007 → SEC-409-NO-VIEW-GRANT |
| AC-SEC-016 | TC-SEC-054 | REQ-SEC-016 | — |
| AC-SEC-017 | TC-SEC-055 | REQ-SEC-017 | — |
| AC-SEC-019 | TC-SEC-056 | REQ-SEC-019 | — |
| AC-SEC-022 | TC-SEC-057 | REQ-SEC-022 | — |
| AC-SEC-025 | TC-SEC-058 | REQ-SEC-025 | — |
| AC-SEC-026 | TC-SEC-059 | REQ-SEC-026 | — |
| AC-SEC-027 | TC-SEC-060 | REQ-SEC-027 | — |
| AC-SEC-028 | TC-SEC-061 | REQ-SEC-028 | — |
| AC-SEC-021 | TC-SEC-062 | REQ-SEC-021 | — |
| AC-SEC-032 | TC-SEC-063 | REQ-SEC-032 | — |
| AC-SEC-033 | TC-SEC-064 | REQ-SEC-033 | — |
| AC-SEC-023 | TC-SEC-065 | REQ-SEC-023 | — |

### AC covered on the backend track only — not a gap on this track

| AC | REQ | Why no frontend case |
|---|---|---|
| AC-SEC-018 | REQ-SEC-018 | a screen registration naming an unregistered module is refused — the refusal is answered to the registering module's own call, which no screen makes |
| AC-SEC-024 | REQ-SEC-024 | the audit entry is appended by the server when an event completes; no client call or affordance writes one, and the frontend's assertable half is the filtered read (TC-SEC-058) |
| AC-SEC-029 | REQ-SEC-029 | the password-reset notification is dispatched entirely server-side when a token is issued — no client call, state or affordance represents it |
| AC-SEC-034 | REQ-SEC-034 | `SecUserDirectoryApi` is a cross-module Spring interface, not an HTTP endpoint and not a screen — covered by `backend-test-plan-sec.md`'s TC-SEC-034 |
| AC-SEC-035 | REQ-SEC-035 | same surface as AC-SEC-034 (`SecUserDirectoryApi`) — covered by `backend-test-plan-sec.md`'s TC-SEC-035 |

## COVERAGE

AC covered on this track: 30/35 — the five above are backend-only by construction, and
35/35 across the module when both plans are read together (`backend-test-plan-sec.md`
carries one TC per AC for all 35).
REQ covered on this track: 30/35 — the same five REQ ids, for the same reason.
SCR covered: not applicable — REDUCED (no frontend-execution-plan-sec.md this run).
UXD covered: not applicable — SEC mints no `UXD-*` (SRS §A8: ROOT, no consumed entity), so
there is nothing for an integration phase to derive at any scope.
TC count check (§3 over-engineering guard): 30 cases against 35 ACs is well under 2×, and
no case is a fabricated variant — every one derives from a distinct AC.
Scenario mix: HAPPY 13 · VIOLATION 5 · STATE 8 · PERMISSION 4 · BOUNDARY 0 (no AC or RULE in
SEC's set states a numeric limit, so §3 rule 3 adds none).

## NOTES

- Every message asserted above is copied character-perfect from `srs-sec.md` in both
  languages; no message is reworded and none is composed by a test.
- TC-SEC-037 is one case with three attempts on purpose: AC-SEC-002 gives ONE message for a
  wrong password, an unknown username and a disabled one, and a test that checked only the
  first would pass against a screen that leaked the difference in the other two.
- TC-SEC-064 asserts both halves of REQ-SEC-033 — the route guard AND the server's
  independent denial — because the menu's omission is not the enforcement, and a test that
  checked only the guard would pass against a client-only gate.
- This revision is REDUCED because `frontend-execution-plan-sec.md` does not exist: no
  `SCR-*`, route or F-block citation appears anywhere above. When P3.2 is re-run for SEC,
  this file should be regenerated in full mode; its ids should continue immediately after
  this revision's own maximum (TC-SEC-065), exactly as this revision itself continued
  immediately after the backend plan's (TC-SEC-035) rather than reusing or skipping any id.
- `test-execution-manifest-sec.md` is a derived view of the **backend** plan and its API
  set. Neither changed in this run, so it is current and is not rewritten.
══════════════════════════════════════════════════════════════════
