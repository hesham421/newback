# PRD — الأمان / Security (SEC)
══════════════════════════════════════════════════════════════════
Module          : SEC     Version : v1
Source artifacts: platform-summary, module-registry, business-policies
Stories         : 12   Policies covered : 11/11   Deferred : 0
Status          : DRAFT — awaiting prd-approval
══════════════════════════════════════════════════════════════════

## USER STORIES

US-SEC-001
  Title          : تسجيل الدخول / Login
  Story          : As a registered platform user, I need to sign in securely, so that I receive an identity/session every consuming module trusts.
  Priority       : HIGH — implied ("secure sign-in issuing a session/token consuming modules trust", the entry point of the whole module)
  Success metric : —
  Traces         : POL-SEC-004
  Source         : security-module-plan-en.md §3
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-002
  Title          : التسجيل الذاتي / Sign-up
  Story          : As a prospective user, I need to self-register, so that an administrator can later grant me access without creating my account manually.
  Priority       : MEDIUM
  Success metric : —
  Traces         : POL-SEC-003
  Source         : security-module-plan-en.md §3
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-003
  Title          : نسيت / إعادة تعيين كلمة المرور / Forgot / reset password
  Story          : As a user who lost access to my password, I need a secure self-service reset, so that I can regain access without exposing my credentials.
  Priority       : MEDIUM
  Success metric : —
  Traces         : POL-SEC-004
  Source         : security-module-plan-en.md §3
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-004
  Title          : إدارة المستخدمين / Manage users
  Story          : As a security administrator, I need to add, edit, activate, deactivate a user and assign one or more roles, so that access reflects who currently should have it.
  Priority       : HIGH
  Success metric : —
  Traces         : POL-SEC-008
  Source         : security-module-plan-en.md §4.4
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-005
  Title          : إدارة الأدوار والمنح الثلاثية / Manage roles and the three-level grant editor
  Story          : As a security administrator, I need to create/edit/deactivate roles and grant them modules, then screens within a granted module, then actions on a granted screen, so that access always follows the module-first hierarchy.
  Priority       : HIGH — plan names this "the core improvement" (§4.1)
  Success metric : —
  Traces         : POL-SEC-001, POL-SEC-002
  Source         : security-module-plan-en.md §4.1, §4.2, §4.4, §4.5
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-006
  Title          : سجل الوحدة/الشاشة/الإجراء / Module / screen / action registry
  Story          : As a consuming module's integrator, I need to register my module, its screens and its actions as data, so that my module can be granted to roles without any change to security code.
  Priority       : HIGH — foundational to every other module's onboarding
  Success metric : —
  Traces         : — (scope only)
  Source         : security-module-plan-en.md §4.3, §4.5, §7; module-registry-sec.md → ENTITIES OWNED (ModuleRegistry, ScreenRegistry, ActionRegistry)
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-007
  Title          : فصل المهام للمستهلكين / Segregation of duties for consumers
  Story          : As a consuming module (e.g. Accounting), I need to require two conflicting actions be held by distinct roles/users, so that no single user can both perform and approve a sensitive transition.
  Priority       : MEDIUM
  Success metric : —
  Traces         : POL-SEC-005
  Source         : security-module-plan-en.md §4.4
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-008
  Title          : القائمة الديناميكية ثنائية المستوى / Dynamic two-tier menu
  Story          : As a signed-in user, I need my menu to show only the modules and screens my roles actually grant, so that I never see or reach something I am not authorized for.
  Priority       : HIGH
  Success metric : —
  Traces         : POL-SEC-006, POL-SEC-007
  Source         : security-module-plan-en.md §6
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-009
  Title          : لوحة تحكم الأمان / Admin dashboard
  Story          : As a security administrator, I need a landing dashboard (users overview, failed logins, active sessions, recent activity, roles/permissions summary, onboarding funnel), so that I can spot risk and over-privileged accounts at a glance.
  Priority       : MEDIUM — plan names this a "recommended baseline" (§5)
  Success metric : —
  Traces         : POL-SEC-010, POL-SEC-011
  Source         : security-module-plan-en.md §5.1, §5.2
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-010
  Title          : سجل التدقيق / Audit log
  Story          : As a security administrator, I need a searchable, filterable, exportable (CSV) audit log of logins, failed logins, resets and role/permission changes, so that I have a complete and trustworthy record of every security-relevant event.
  Priority       : MEDIUM
  Success metric : —
  Traces         : POL-SEC-009
  Source         : security-module-plan-en.md §5.3
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-011
  Title          : إدارة الجلسات النشطة / Active sessions management
  Story          : As a security administrator, I need to see currently signed-in users and force-terminate a session when authorized, so that I can respond to a compromised or abandoned session.
  Priority       : MEDIUM
  Success metric : —
  Traces         : — (scope only)
  Source         : security-module-plan-en.md §5.1, §5.3; module-registry-sec.md → ENTITIES OWNED (ActiveSession)
  Status         : DRAFT → APPROVED (by the PRD approval gate)

US-SEC-012
  Title          : إشعار اختياري عبر خدمة الإشعارات / Optional notification on password reset
  Story          : As a user requesting a password reset, I need to optionally receive that reset through the platform's ready Notifications service, so that I am not blocked if this integration is skipped.
  Priority       : LOW — explicitly "only on real need", never a hard dependency
  Success metric : —
  Traces         : — (scope only)
  Source         : security-module-plan-en.md §8; new project/integration-notifications-fileservice.md §1
  Status         : DRAFT → APPROVED (by the PRD approval gate)

## TRACEABILITY — story → policy
| US | Traces (POL) | Source |
|---|---|---|
| US-SEC-001 | POL-SEC-004 | security-module-plan-en.md §3 |
| US-SEC-002 | POL-SEC-003 | security-module-plan-en.md §3 |
| US-SEC-003 | POL-SEC-004 | security-module-plan-en.md §3 |
| US-SEC-004 | POL-SEC-008 | security-module-plan-en.md §4.4 |
| US-SEC-005 | POL-SEC-001, POL-SEC-002 | security-module-plan-en.md §4.1-§4.2 |
| US-SEC-006 | — (scope only) | security-module-plan-en.md §4.3, §7 |
| US-SEC-007 | POL-SEC-005 | security-module-plan-en.md §4.4 |
| US-SEC-008 | POL-SEC-006, POL-SEC-007 | security-module-plan-en.md §6 |
| US-SEC-009 | POL-SEC-010, POL-SEC-011 | security-module-plan-en.md §5.1-§5.2 |
| US-SEC-010 | POL-SEC-009 | security-module-plan-en.md §5.3 |
| US-SEC-011 | — (scope only) | security-module-plan-en.md §5.1, §5.3 |
| US-SEC-012 | — (scope only) | security-module-plan-en.md §8 |
Every policy POL-SEC-001 … POL-SEC-011 appears in at least one row above (001,002 →
US-005; 003 → US-002; 004 → US-001/US-003; 005 → US-007; 006,007 → US-008;
008 → US-004; 009 → US-010; 010,011 → US-009).

## RESOLVED DECISIONS (dialogue)
| # | Question | Recommended | Confirmed by user | Sources |
|---|---|---|---|---|
None — security-module-plan-en.md and business-policies-sec.md left no story's scope,
priority or role genuinely ambiguous; no dialogue question was required.

## DEFERRED
| US | Reason | Activation trigger |
None — every capability named in security-module-plan-en.md is represented by a story
in this v1 PRD; nothing was pushed out.

## APPROVAL
Approved by : PENDING   Date : PENDING
Once approved, no stage may raise a question; P1 onward self-resolve
per the ambiguity rule (shared/GOVERNANCE-CORE.md).
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 04, 05, 06, 08, 10, 11, 14 (shipped in 1.1.0)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Paths cited below are relative to the erp-core repository at that tag. No US ids are minted here; the
factory assigns them if these capabilities are adopted as stories.

NEW product capabilities
| Capability | Actor | Implemented behaviour | Source |
|---|---|---|---|
| Customer self-registration | prospective customer | Registers with e-mail, password (min 8) and one full name in a tenant (`X-Tenant-Code`); the username is the e-mail. The account is `PENDING_VERIFICATION` and a verification mail (`CUSTOMER_VERIFY_EMAIL`) is sent. An e-mail that already has a customer account in that tenant is refused. | docs/steps/06-report.md; DEVIATIONS [06] |
| Customer e-mail verification | customer | Uses the single-use link (valid 24 h); the account becomes ACTIVE. | DEVIATIONS [06] |
| Customer sign-in | customer | Signs in with e-mail and password; refused until verified; rate limited (default 10 attempts per minute); receives a CUSTOMER-realm token usable only on customer endpoints. | DEVIATIONS [06] |
| Customer password reset | customer | Requests a reset link (`CUSTOMER_PASSWORD_RESET` mail) and completes it; completing it also verifies a still-unverified account. | DEVIATIONS [06] |
| Customer profile | customer | Views own profile; edits only the two display names. | DEVIATIONS [06] |
| Customer in-app inbox | customer (and staff) | Lists own in-app notifications and marks them read (owned by NOTIF; see analysis/modules/NOTIF). | DEVIATIONS [08] |
| Tenant-scoped sign-in | every user | Every account belongs to one tenant; a sign-in names the tenant (`X-Tenant-Code`); a suspended tenant cannot sign in. | docs/steps/05-report.md |
| First-start administrator password | platform operator | No default `admin/admin`; the operator sets the bootstrap admin password once by configuration. | docs/steps/04-report.md |
| Users report | staff with `SEC:REPORT:SEC_USER_LIST` | Runs / exports (CSV, JSON) a list of user accounts of both realms, filterable by realm, status, active flag and creation date. | DEVIATIONS [11] |

CHANGED behaviour of existing stories
| Story | Delta | Source |
|---|---|---|
| US-SEC-001 Login | Staff login now needs the tenant (`X-Tenant-Code`); the token carries `realm` and `tid`. A successful login is also recorded in the platform audit log. | docs/steps/05-report.md, 06-report.md; DEVIATIONS [10] |
| US-SEC-002 Sign-up | Unchanged flow (admin approval), now explicitly STAFF realm. | docs/steps/06-report.md |
| US-SEC-003 Forgot / reset password | Staff flow unchanged; completion is also recorded in the platform audit log. A reset token of the other realm is refused like an unknown token. | DEVIATIONS [06], [10] |
| US-SEC-004 Manage users | Administrators manage STAFF accounts only: a customer account is invisible to user search and answers "user not found" on every by-id operation. | DEVIATIONS [14]; CHANGELOG [1.1.0] |
| US-SEC-005 Manage roles and grants | `SYS_ADMIN` of every tenant is a super role (all catalog permissions without grants); its grant tree no longer limits it. | DEVIATIONS [06] |
| US-SEC-006 Module / screen / action registry | The catalog is now declared in code by each module and synchronized at startup (still visible through the registry search). | docs/steps/06-report.md |
| US-SEC-007 Segregation of duties | No consumer declares a conflicting pair since `fin` was removed; unchanged and inert. | docs/steps/01-report.md |
| US-SEC-009 Admin dashboard | User and session counts cover STAFF accounts only. | CHANGELOG [1.1.0] Security |
| US-SEC-011 Active sessions | Lists and terminates STAFF sessions only. | DEVIATIONS [14] |
| US-SEC-012 Optional notification on password reset | Delivery is asynchronous with retries (NOTIF); the customer realm has its own reset mail. | DEVIATIONS [06], [08] |

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package D — passwords, profile, photo, staff `/me` (package G's grant revoke is described in `P1/srs-sec.md` 1.3.0 §1–§8)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

| Kind | Capability | Who | What it does | Source |
|---|---|---|---|---|
| NEW | Set a user's password | security administrator | Sets another staff user's password from the Users screen; by default the user must change it at the next sign-in, and every session of the user ends. | srs-sec.md 1.3.0 §9 (REQ-SEC-083) |
| NEW | Forced password change | staff user | After an administrator chose the password, only the profile, the password change and sign-out work until the user picks their own. | REQ-SEC-084 |
| NEW | Change my password | staff user | Changes the own password, giving the current one; the user's other sessions end. | REQ-SEC-085 |
| NEW | My profile | staff user | Reads and edits names, phone, job title and preferred language; uploads or removes a photo. | REQ-SEC-086, REQ-SEC-087 |
| NEW | Password policy | anyone choosing a staff password | 8..200 characters with a letter and a digit (configurable). | REQ-SEC-082 |
| NEW | Password-change e-mail | staff user | An e-mail tells the user that their password was changed and by whom. | REQ-SEC-089; NOTIF RULE-NOTIF-009 |
