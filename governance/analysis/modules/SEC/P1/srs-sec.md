# SRS — الأمان / Security (SEC)
══════════════════════════════════════════════════════════════════
Module : SEC   Version : v1   Profile : erp
Inputs : prd, domain-profile, project-registry (PRD approved 2026-09-10)
Counts : ENT 13 · REQ 33 · AC 33 · RULE 7 · SCR-REQ 10 · ADR 0
══════════════════════════════════════════════════════════════════

# PART A — MODULE FOUNDATION

## A1 — Document information
| Item | Value |
|---|---|
| Module | SEC — الأمان / Security |
| Feature code | SEC |
| Version | v1 |
| Date | 2026-09-10 |
| Status | DRAFT (P1) |
| Prepared by | governance-factory (analysis lane) |
| Decisions applied count | 0 (no ADR was needed — no ambiguity §9 was reached) |

## A2 — Functional context

**In scope:** المصادقة (تسجيل الدخول، التسجيل الذاتي، إعادة تعيين كلمة المرور)، RBAC هرمي
بثلاث مستويات (وحدة→شاشة→إجراء)، تسجيل الوحدات/الشاشات/الإجراءات كبيانات لأي وحدة مستهلكة،
فصل المهام (SoD) على مستوى المستخدم، القائمة الديناميكية ثنائية المستوى، لوحة تحكم الأمان،
سجل التدقيق غير القابل للتعديل، إدارة الجلسات النشطة، تكامل اختياري مع خدمة الإشعارات.

**Out of scope:** المصادقة متعددة العوامل (MFA)، تسجيل الدخول الموحد (SSO)/موفرو هوية
خارجيون — غير مذكورين في `security-module-plan-en.md` [business-policies-sec.md →
SCOPE EXCEPTIONS]؛ أي منطق عمل خاص بوحدة مستهلكة.

**Module function (one paragraph):** وحدة SEC هي نظام الأمان الوحيد للمنصة بأكملها: تُصدر
الهوية (المصادقة) وتُقرّر الصلاحيات الفعلية (RBAC هرمي)، بحيث لا تملك أي وحدة أخرى مستخدمين
أو أدوارًا أو تسجيل دخول خاصًا بها؛ كل وحدة تستهلك SEC عبر تسجيل نفسها كبيانات ثم فحص
المنح الصادرة عنها.

**Detailed description (workflow narrative, roles):** مستخدم يُسجّل ذاتيًا فيبقى معلّقًا بلا
صلاحيات → يوافق مسؤول أمان عليه فيصبح نشطًا → يُسنَد له دور واحد أو أكثر → عند كل طلب،
يُفحص منح الوحدة أولاً (بوابة)، ثم منح الشاشة، ثم منح الإجراء. مسؤول الأمان يدير الأدوار
والمستخدمين والجلسات النشطة ويراقب سجل التدقيق ولوحة التحكم. أي وحدة مستهلكة (مثل FIN
لاحقًا) تُسجّل نفسها وشاشاتها وإجراءاتها هنا كبيانات فقط، دون أي تعديل على شيفرة SEC.

**Current situation:** لا يوجد نظام أمان سابق ضمن هذه الدفعة — هذه أول وحدة تُبنى (Tier 0)؛
لا "وضع حالي" يُستبدل داخل هذه المنصة الجديدة.

**Current difficulties:** لا ينطبق (وحدة جديدة بالكامل).

**Proposed system and benefits:** نظام أمان مركزي واحد يمنع ازدواج/تضارب الصلاحيات بين
الوحدات، يضمن بوابة وحدة صارمة (لا شاشة يتيمة)، ويوفر أثرًا تدقيقيًا كاملاً غير قابل للتعديل.

**General notes (constraints, deferred items):** محرك سير العمل ممنوع منصّيًا
(`profiles/erp.yaml → conventions.workflow_engine: forbidden`)؛ لا آلية قفل تلقائي بعد محاولات
دخول فاشلة متكررة — لم يذكرها `security-module-plan-en.md`، فلم تُخترع (تُعرض فقط أعداد
الدخول الفاشل في لوحة التحكم، REQ-SEC-002/022).

## A3 — Entities and fields

Standard fields per kind (profile.conventions.entity_defaults): the `security` entity
kind carries no fixed default-field set in the profile (only master/transactional/
lookup/config do); every SEC entity below still carries the platform's audit fields
(`createdBy, createdAt, updatedBy, updatedAt`, `profile.stack.db.naming.audit_fields`)
except pure append-only log/session rows where a "who created it" field is redundant
with the row's own actor field (documented per entity).

### ENT-SEC-001 — المستخدم / User
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | SHARED (owner) — every module's audit fields (createdBy/updatedBy) reference it | No — login identity (email/username) is the natural key, not a generated number [§3.3 NUMBERING test] | create, read, search, update, activate, deactivate | consumed read-only by every future consumer module for its own audit fields | security-module-plan-en.md §3, §4.4 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| userPk | number | yes (system) | — | primary key | معرّف المستخدم | User id |
| username | text | yes | unique | login identity | اسم المستخدم | Username |
| email | text | yes | unique, valid email | used for password-reset delivery | البريد الإلكتروني | Email |
| passwordHash | text | yes (system) | never exposed to any client [POL-SEC-004] | write-only | تجزئة كلمة المرور | Password hash |
| fullNameAr | text | yes | — | — | الاسم الكامل (عربي) | Full name (Arabic) |
| fullNameEn | text | yes | — | — | الاسم الكامل (إنجليزي) | Full name (English) |
| statusCode | lookup | yes | lookup key `USER_STATUS` (A6) | drives A7 lifecycle | الحالة | Status |
| lastLoginAt | date-time | no | — | informational | آخر دخول | Last login |
| failedLoginCount24h | number | no | derived, not stored per ERP-…(see POL-SEC-010) — displayed from AuditLogEntry, not persisted on User | dashboard-only figure; kept here only as a documentation note, not a real column | عدد محاولات الدخول الفاشلة (٢٤س) | Failed logins (24h) |
| isActiveFl | flag | yes | true/false | mirrors statusCode ≠ DISABLED, kept for the platform's standard flag convention | نشط | Active |
| createdBy, createdAt, updatedBy, updatedAt | system | yes | — | standard audit fields | — | — |

### ENT-SEC-002 — الدور / Role
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | PRIVATE | No | create, read, search, update, deactivate | none | security-module-plan-en.md §4.4 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| rolePk | number | yes (system) | — | primary key | معرّف الدور | Role id |
| code | text | yes | unique | stable machine reference | رمز الدور | Role code |
| nameAr, nameEn | text | yes | — | — | اسم الدور | Role name |
| descriptionAr, descriptionEn | text | no | — | — | الوصف | Description |
| isActiveFl | flag | yes | — | — | نشط | Active |
| createdBy, createdAt, updatedBy, updatedAt | system | yes | — | — | — | — |

### ENT-SEC-003 — ربط المستخدم بالدور / UserRoleAssignment
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | PRIVATE | No | create, read (list), delete (revoke) | none | security-module-plan-en.md §4.4 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| userRoleAssignmentPk | number | yes (system) | — | primary key | معرّف الإسناد | Assignment id |
| userId | reference | yes | ENT-SEC-001 | — | المستخدم | User |
| roleId | reference | yes | ENT-SEC-002 | — | الدور | Role |
| assignedBy, assignedAt | system | yes | — | who/when granted | — | — |

### ENT-SEC-004 — سجل الوحدات / ModuleRegistry
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | SHARED (owner) — every consuming module registers one row of itself here | No — the module code is the platform's own `{prefix}` (factory.ids), not a SEC-generated number | create (by a registering module), read, search, deactivate | consumed by every module registering itself (e.g. FIN in a later pass) | security-module-plan-en.md §4.3, §7 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| moduleRegistryPk | number | yes (system) | — | primary key | معرّف الوحدة المسجّلة | Registered module id |
| code | text | yes | unique — the platform module code (e.g. FIN, SEC) | matches `profile.vocabulary.module_prefixes` | رمز الوحدة | Module code |
| nameAr, nameEn | text | yes | — | — | اسم الوحدة | Module name |
| isActiveFl | flag | yes | — | — | نشط | Active |
| createdBy, createdAt, updatedBy, updatedAt | system | yes | — | — | — | — |

### ENT-SEC-005 — سجل الشاشات / ScreenRegistry (SEC_PAGES)
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | SHARED (owner) | No | create (by a registering module), read, search, deactivate | consumed by every module registering its own screens | security-module-plan-en.md §4.3, §4.5; profiles/erp.yaml conventions.security_model.page_registry |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| screenRegistryPk | number | yes (system) | — | primary key | معرّف الشاشة المسجّلة | Registered screen id |
| pageCode | text | yes | unique | `SEC_PAGES` row per §7.1 | رمز الصفحة | Page code |
| moduleId | reference | yes | ENT-SEC-004; must already be registered [RULE-SEC-004] | — | الوحدة | Module |
| nameAr, nameEn | text | yes | — | — | اسم الشاشة | Screen name |
| isActiveFl | flag | yes | — | — | نشط | Active |
| createdBy, createdAt, updatedBy, updatedAt | system | yes | — | — | — | — |

### ENT-SEC-006 — سجل الإجراءات / ActionRegistry
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | SHARED (owner) | No | create (by a registering module), read, search, deactivate | consumed by every module registering its own actions | security-module-plan-en.md §4.1, §4.3; profiles/erp.yaml conventions.security_model.permission_pattern |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| actionRegistryPk | number | yes (system) | — | primary key | معرّف الإجراء المسجّل | Registered action id |
| permissionCode | text | yes | unique, pattern `PERM_<PAGE_CODE>_<ACTION>` (profile.conventions.security_model.permission_pattern) — derived, never entered a second time as seed data | derivation, not duplication | رمز الصلاحية | Permission code |
| screenId | reference | yes | ENT-SEC-005; must already be registered [RULE-SEC-002] | — | الشاشة | Screen |
| actionCode | text | yes | one of the platform-standard set VIEW/CREATE/UPDATE/DELETE (profile.conventions.security_model.actions) or a module-declared custom code (e.g. "REVERSE_ENTRY") | free beyond the standard four, per plan §4.1 "custom actions" | الإجراء | Action |
| nameAr, nameEn | text | yes | — | — | اسم الإجراء | Action name |
| isActiveFl | flag | yes | — | — | نشط | Active |
| createdBy, createdAt, updatedBy, updatedAt | system | yes | — | — | — | — |

### ENT-SEC-007 — منح الوحدة للدور / RoleModuleGrant
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | PRIVATE | No | create (grant), read (list), delete (revoke — cascades per RULE-SEC-003) | none | security-module-plan-en.md §4.1-§4.2 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| roleModuleGrantPk | number | yes (system) | — | primary key | معرّف منح الوحدة | Module grant id |
| roleId | reference | yes | ENT-SEC-002 | — | الدور | Role |
| moduleId | reference | yes | ENT-SEC-004 | — | الوحدة | Module |
| grantedBy, grantedAt | system | yes | — | — | — | — |

### ENT-SEC-008 — منح الشاشة للدور / RoleScreenGrant
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | PRIVATE | No | create (grant, blocked without matching module grant — RULE-SEC-001), read (list), delete (revoke) | none | security-module-plan-en.md §4.1-§4.2 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| roleScreenGrantPk | number | yes (system) | — | primary key | معرّف منح الشاشة | Screen grant id |
| roleId | reference | yes | ENT-SEC-002 | — | الدور | Role |
| screenId | reference | yes | ENT-SEC-005; role must hold its module [RULE-SEC-001] | — | الشاشة | Screen |
| grantedBy, grantedAt | system | yes | — | — | — | — |

### ENT-SEC-009 — منح الإجراء للدور / RoleActionGrant
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | PRIVATE | No | create (grant, blocked without matching screen grant — RULE-SEC-002, and without VIEW on that screen unless the action itself is VIEW — RULE-SEC-007), read (list), delete (revoke) | none | security-module-plan-en.md §4.1-§4.2 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| roleActionGrantPk | number | yes (system) | — | primary key | معرّف منح الإجراء | Action grant id |
| roleId | reference | yes | ENT-SEC-002 | — | الدور | Role |
| actionId | reference | yes | ENT-SEC-006; role must hold the action's screen [RULE-SEC-002] and, unless the action itself is VIEW, must also hold VIEW on that screen [RULE-SEC-007] | — | الإجراء | Action |
| grantedBy, grantedAt | system | yes | — | — | — | — |

### ENT-SEC-010 — الجلسة النشطة / ActiveSession
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | PRIVATE | No | create (system, on login), read (list), terminate | none | security-module-plan-en.md §5.1, §5.3 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| activeSessionPk | number | yes (system) | — | primary key | معرّف الجلسة | Session id |
| userId | reference | yes | ENT-SEC-001 | — | المستخدم | User |
| tokenRef | text | yes | opaque reference — never the raw token/hash | — | مرجع الرمز | Token reference |
| startedAt | date-time | yes (system) | — | — | بدأت في | Started at |
| lastActivityAt | date-time | yes (system) | — | — | آخر نشاط | Last activity |
| ipAddress | text | no | — | — | عنوان IP | IP address |
| terminatedAt, terminatedBy | date-time / reference | no | set on logout or forced termination | null while active | أُنهيت في / بواسطة | Terminated at / by |

### ENT-SEC-011 — سجل التدقيق / AuditLogEntry
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | PRIVATE | No | create (system, append-only), read, search | none | security-module-plan-en.md §5.2-§5.3 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| auditLogEntryPk | number | yes (system) | — | primary key | معرّف قيد التدقيق | Audit entry id |
| eventTypeCode | lookup | yes | lookup key `AUDIT_EVENT_TYPE` (A6) | — | نوع الحدث | Event type |
| actorUserId | reference | no | ENT-SEC-001 — null for an unauthenticated failed-login attempt | — | المستخدم الفاعل | Actor user |
| occurredAt | date-time | yes (system) | — | immutable once written [POL-SEC-009] | وقت الحدث | Occurred at |
| targetRef | text | no | free text (e.g. affected user/role id) | — | الهدف | Target |
| detailsAr, detailsEn | text | no | — | — | التفاصيل | Details |
| ipAddress | text | no | — | — | عنوان IP | IP address |
Note: this entity has no `createdBy`/`updatedBy` — it IS the audit record; `actorUserId` +
`occurredAt` serve that purpose, and it is never updated after insert (immutability, POL-SEC-009).

### ENT-SEC-012 — رمز إعادة تعيين كلمة المرور / PasswordResetToken
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | PRIVATE | No | create (system, on request), read (validate), invalidate (on use or expiry) | none | security-module-plan-en.md §3 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| passwordResetTokenPk | number | yes (system) | — | primary key | معرّف الرمز | Token id |
| userId | reference | yes | ENT-SEC-001 | — | المستخدم | User |
| tokenHash | text | yes (system) | never exposed in full after issuance | write-once | تجزئة الرمز | Token hash |
| requestedAt, expiresAt | date-time | yes (system) | expiresAt = requestedAt + DEFAULT window (see A7 note) | — | — | — |
| usedAt | date-time | no | set once consumed [RULE-SEC-006] | — | استُخدم في | Used at |

### ENT-SEC-013 — طلب تسجيل معلّق / SignupRequest
| Kind | Ownership | Business number | Operations | Cross-module | Source |
|---|---|---|---|---|---|
| security | PRIVATE | No | create (self-service), read, search, approve, reject | none | security-module-plan-en.md §3 |

| Field | Logical type | Required | Values / source | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| signupRequestPk | number | yes (system) | — | primary key | معرّف طلب التسجيل | Signup request id |
| email | text | yes | valid email | becomes the user's login on approval | البريد الإلكتروني | Email |
| fullNameAr, fullNameEn | text | yes | — | — | الاسم الكامل | Full name |
| submittedAt | date-time | yes (system) | — | — | تاريخ التقديم | Submitted at |
| statusCode | lookup | yes | lookup key `SIGNUP_STATUS` (A6) | — | الحالة | Status |
| reviewedBy, reviewedAt | reference / date-time | no | set on approve/reject | — | — | — |

## A4 — Functional requirements (EARS) and acceptance criteria

### REQ-SEC-001 — تسجيل دخول ناجح / Successful login
Pattern    : event
Statement  : When a registered, active user submits valid credentials, the system shall issue a session/access token representing that user's identity.
Traces     : US-SEC-001
Entities   : ENT-SEC-001, ENT-SEC-010
Rationale  : entry point every consuming module trusts
Source     : security-module-plan-en.md §3
Priority   : HIGH
#### AC-SEC-001 — [REQ-SEC-001]
Given an active user with a known username and password
When the user submits those correct credentials to the login screen
Then the system creates an active session for the user and grants access to the platform

### REQ-SEC-002 — رفض بيانات دخول غير صحيحة / Reject invalid credentials
Pattern    : unwanted
Statement  : If submitted credentials do not match an active user, then the system shall reject the login attempt and record a failed-login audit event.
Traces     : US-SEC-001
Entities   : ENT-SEC-001, ENT-SEC-011
Rationale  : POL-SEC-004 discipline; feeds the dashboard's failed-logins widget
Source     : security-module-plan-en.md §3, §5.1
Priority   : HIGH
#### AC-SEC-002 — [REQ-SEC-002]
Given a login attempt with a wrong password or an unknown/disabled username
When the user submits it
Then the system rejects the attempt with message ar: "بيانات الدخول غير صحيحة" · en: "Invalid credentials", issues no session, and appends one `LOGIN_FAILED` audit entry

### REQ-SEC-003 — تقديم طلب تسجيل / Submit sign-up
Pattern    : event
Statement  : When a prospective user submits a sign-up request, the system shall create a pending sign-up request with no permission granted to anyone.
Traces     : US-SEC-002
Entities   : ENT-SEC-013
Rationale  : self-registration without premature access
Source     : security-module-plan-en.md §3
Priority   : MEDIUM
#### AC-SEC-003 — [REQ-SEC-003]
Given a prospective user fills the sign-up form with a valid, not-already-registered email
When they submit it
Then the system creates a `SignupRequest` with status PENDING and creates no user account yet

### REQ-SEC-004 — الموافقة على طلب التسجيل / Approve a sign-up request
Pattern    : event
Statement  : When an administrator approves a pending sign-up request, the system shall create an active user account from it.
Traces     : US-SEC-002
Entities   : ENT-SEC-013, ENT-SEC-001
Rationale  : conversion from pending to real, permission-bearing identity
Source     : security-module-plan-en.md §3
Priority   : MEDIUM
Note       : the credential of the created account is unspecified here and in AC-SEC-004, yet DBF-SEC-004 `password_hash` is NOT NULL. Operative behaviour: the account is created with an unusable random secret (never logged, returned or transmitted), so it is ACTIVE but cannot be authenticated against; the owner's route to a real credential is the existing REQ-SEC-006 → REQ-SEC-007 password-reset pair (RULE-SEC-006). OPEN for a human (not decided here): no SEC artifact says the approved owner is notified or that a reset token is issued at approval — REQ-SEC-029 triggers on token issuance, not approval, and is itself `optional` — and the state is undiscoverable from outside, since login answers the deliberately uniform "Invalid credentials" error (POL-SEC-004) and the reset request answers the same generic 200 whether or not the email exists, making "approved", "still pending" and "rejected" indistinguishable. The two candidate answers are (a) SEC issues a reset token at approval time — which introduces a NOTIF dependency into a module declared ROOT — or (b) the spec states plainly that the owner is informed out of band.
#### AC-SEC-004 — [REQ-SEC-004]
Given a SignupRequest with status PENDING
When an administrator approves it
Then the system creates a User with status ACTIVE from its email/name, and marks the SignupRequest APPROVED

### REQ-SEC-005 — رفض طلب التسجيل / Reject a sign-up request
Pattern    : unwanted
Statement  : If an administrator rejects a pending sign-up request, then the system shall mark the request rejected and create no user account.
Traces     : US-SEC-002
Entities   : ENT-SEC-013
Rationale  : symmetric negative path to REQ-SEC-004
Source     : security-module-plan-en.md §3
Priority   : LOW
#### AC-SEC-005 — [REQ-SEC-005]
Given a SignupRequest with status PENDING
When an administrator rejects it
Then the system marks it REJECTED and creates no User

### REQ-SEC-006 — إصدار رمز إعادة تعيين / Issue a password-reset token
Pattern    : event
Statement  : When a user requests a password reset, the system shall issue a single-use, time-limited reset token for that user.
Traces     : US-SEC-003
Entities   : ENT-SEC-012
Rationale  : secure self-service reset
Source     : security-module-plan-en.md §3
Priority   : MEDIUM
#### AC-SEC-006 — [REQ-SEC-006]
Given a user identifies themselves by a registered email
When they request a password reset
Then the system creates one PasswordResetToken with an expiry, and no password is changed yet

### REQ-SEC-007 — إتمام إعادة التعيين بنجاح / Complete a password reset
Pattern    : event
Statement  : When a user submits a valid, unexpired reset token with a new password, the system shall update that user's password and invalidate the token.
Traces     : US-SEC-003
Entities   : ENT-SEC-012, ENT-SEC-001
Rationale  : one-time use enforced
Source     : security-module-plan-en.md §3
Priority   : MEDIUM
#### AC-SEC-007 — [REQ-SEC-007]
Given an unexpired, unused PasswordResetToken and a new password meeting the platform's password rules
When the user submits them
Then the system updates the user's password hash, sets the token's usedAt, and appends a `PASSWORD_RESET_COMPLETED` audit entry

### REQ-SEC-008 — رفض رمز منتهٍ أو مُستخدَم / Reject an expired or used reset token
Pattern    : unwanted
Statement  : If a submitted reset token is expired or already used, then the system shall reject the password reset.
Traces     : US-SEC-003
Entities   : ENT-SEC-012
Rationale  : RULE-SEC-006
Source     : security-module-plan-en.md §3
Priority   : MEDIUM
#### AC-SEC-008 — [REQ-SEC-008]
Given a PasswordResetToken that is expired or already has a usedAt value
When it is submitted with a new password
Then the system rejects the request with message ar: "رابط إعادة التعيين غير صالح أو منتهي" · en: "This reset link is invalid or has expired" and changes nothing

### REQ-SEC-009 — إنشاء مستخدم / Create a user
Pattern    : event
Statement  : When an administrator creates a user, the system shall record that user's bilingual name, login identity and status.
Traces     : US-SEC-004
Entities   : ENT-SEC-001
Rationale  : direct administrative provisioning (distinct from self sign-up)
Source     : security-module-plan-en.md §4.4
Priority   : HIGH
#### AC-SEC-009 — [REQ-SEC-009]
Given an administrator fills the user form with a unique username/email and both name labels
When they save it
Then the system creates the User with status ACTIVE (or as chosen)

### REQ-SEC-010 — إسناد أدوار متعددة / Assign one or more roles to a user
Pattern    : event
Statement  : When an administrator assigns one or more roles to a user, the system shall record each assignment individually.
Traces     : US-SEC-004
Entities   : ENT-SEC-001, ENT-SEC-002, ENT-SEC-003
Rationale  : a user may hold several roles; effective permission is their union [POL-SEC-008]
Source     : security-module-plan-en.md §4.4
Priority   : HIGH
#### AC-SEC-010 — [REQ-SEC-010]
Given an administrator selects one or more active roles for a user
When they save the assignment
Then the system creates one UserRoleAssignment row per selected role

### REQ-SEC-011 — تعطيل مستخدم / Deactivate a user
Pattern    : event
Statement  : When an administrator deactivates a user, the system shall immediately end that user's active sessions and prevent new logins for that user.
Traces     : US-SEC-004
Entities   : ENT-SEC-001, ENT-SEC-010
Rationale  : revoking access must be immediate, not just cosmetic
Source     : security-module-plan-en.md §4.4
Priority   : HIGH
#### AC-SEC-011 — [REQ-SEC-011]
Given an active user with one active session
When an administrator deactivates the user
Then the system sets status DISABLED, terminates every active session of that user, and a subsequent login attempt is rejected per REQ-SEC-002

### REQ-SEC-031 — إعادة تفعيل مستخدم / Reactivate a disabled user
Pattern    : event
Statement  : When an administrator reactivates a disabled user, the system shall restore that user's ability to sign in.
Traces     : US-SEC-004
Entities   : ENT-SEC-001
Rationale  : symmetric to REQ-SEC-011; "activate" listed alongside deactivate in the story
Source     : security-module-plan-en.md §4.4
Priority   : MEDIUM
#### AC-SEC-031 — [REQ-SEC-031]
Given a user with status DISABLED
When an administrator reactivates them
Then the system sets status ACTIVE and a subsequent login with correct credentials succeeds

### REQ-SEC-012 — منح وحدة لدور / Grant a module to a role
Pattern    : event
Statement  : When an administrator grants a module to a role, the system shall record that grant as the role's module-level access.
Traces     : US-SEC-005
Entities   : ENT-SEC-002, ENT-SEC-004, ENT-SEC-007
Rationale  : the module gate, evaluated first [POL-SEC-001]
Source     : security-module-plan-en.md §4.1-§4.2
Priority   : HIGH
#### AC-SEC-012 — [REQ-SEC-012]
Given an active role and an active registered module
When an administrator grants that module to that role
Then the system creates one RoleModuleGrant row

### REQ-SEC-013 — رفض منح شاشة دون منح وحدة / Reject a screen grant without its module grant
Pattern    : unwanted
Statement  : If an administrator attempts to grant a screen of a module the target role does not hold, then the system shall reject the grant.
Traces     : US-SEC-005
Entities   : ENT-SEC-002, ENT-SEC-005, ENT-SEC-007, ENT-SEC-008
Rationale  : RULE-SEC-001; structural integrity, no orphaned grant [POL-SEC-002]
Source     : security-module-plan-en.md §4.2
Priority   : HIGH
#### AC-SEC-013 — [REQ-SEC-013]
Given a role with no RoleModuleGrant for module FIN
When an administrator attempts to grant that role a FIN screen
Then the system rejects the grant with message ar: "لا يمكن منح شاشة دون منح الوحدة أولًا" · en: "Cannot grant a screen without first granting its module" and creates no RoleScreenGrant

### REQ-SEC-014 — رفض منح إجراء دون منح شاشة / Reject an action grant without its screen grant
Pattern    : unwanted
Statement  : If an administrator attempts to grant an action of a screen the target role does not hold, then the system shall reject the grant.
Traces     : US-SEC-005
Entities   : ENT-SEC-002, ENT-SEC-006, ENT-SEC-008, ENT-SEC-009
Rationale  : RULE-SEC-002; same structural-integrity principle one level down
Source     : security-module-plan-en.md §4.2
Priority   : HIGH
#### AC-SEC-014 — [REQ-SEC-014]
Given a role with no RoleScreenGrant for a given screen
When an administrator attempts to grant that role an action on that screen
Then the system rejects the grant with message ar: "لا يمكن منح إجراء دون منح الشاشة أولًا" · en: "Cannot grant an action without first granting its screen" and creates no RoleActionGrant

### REQ-SEC-015 — إلغاء المنح المتسلسل عند سحب الوحدة / Cascade-revoke on module-grant removal
Pattern    : event
Statement  : When an administrator revokes a role's module grant, the system shall also remove every screen and action grant that module covered for that role.
Traces     : US-SEC-005
Entities   : ENT-SEC-002, ENT-SEC-007, ENT-SEC-008, ENT-SEC-009
Rationale  : RULE-SEC-003; prevents an orphaned screen/action grant from surviving its module grant
Source     : security-module-plan-en.md §4.2
Priority   : HIGH
#### AC-SEC-015 — [REQ-SEC-015]
Given a role holding a module grant plus two screen grants and three action grants under that module
When an administrator revokes the module grant
Then the system deletes the module grant and every screen/action grant it covered, leaving none behind

### REQ-SEC-016 — تسجيل وحدة جديدة / Register a new module
Pattern    : event
Statement  : When a consuming module registers itself, the system shall record its code and bilingual name in the module registry.
Traces     : US-SEC-006
Entities   : ENT-SEC-004
Rationale  : "no change to security code" onboarding
Source     : security-module-plan-en.md §4.3, §7
Priority   : HIGH
#### AC-SEC-016 — [REQ-SEC-016]
Given a module code not yet registered
When it is registered with its bilingual name
Then the system creates one active ModuleRegistry row

### REQ-SEC-017 — تسجيل شاشة لوحدة مسجّلة / Register a screen under a registered module
Pattern    : event
Statement  : When a consuming module registers a screen, the system shall record it under that module's already-registered code.
Traces     : US-SEC-006
Entities   : ENT-SEC-004, ENT-SEC-005
Rationale  : SEC_PAGES per §7.1
Source     : security-module-plan-en.md §4.3, §4.5
Priority   : HIGH
#### AC-SEC-017 — [REQ-SEC-017]
Given a registered, active module
When it registers a screen with a unique page code and bilingual name
Then the system creates one active ScreenRegistry row under that module

### REQ-SEC-018 — رفض تسجيل شاشة لوحدة غير مسجّلة / Reject a screen registered under an unregistered module
Pattern    : unwanted
Statement  : If a screen registration names a module that is not registered, then the system shall reject the screen registration.
Traces     : US-SEC-006
Entities   : ENT-SEC-004, ENT-SEC-005
Rationale  : RULE-SEC-004
Source     : security-module-plan-en.md §4.3
Priority   : MEDIUM
#### AC-SEC-018 — [REQ-SEC-018]
Given a module code with no ModuleRegistry row
When a screen registration names that code
Then the system rejects it with message ar: "الوحدة غير مسجّلة" · en: "Module is not registered" and creates no ScreenRegistry row

### REQ-SEC-019 — تسجيل إجراء لشاشة مسجّلة / Register an action under a registered screen
Pattern    : event
Statement  : When a consuming module registers an action on one of its screens, the system shall record it under that screen.
Traces     : US-SEC-006
Entities   : ENT-SEC-005, ENT-SEC-006
Rationale  : permission catalog per §4.1
Source     : security-module-plan-en.md §4.1, §4.3
Priority   : HIGH
#### AC-SEC-019 — [REQ-SEC-019]
Given a registered, active screen
When it registers an action code with bilingual name
Then the system creates one active ActionRegistry row with permission code `PERM_<pageCode>_<actionCode>`

### REQ-SEC-020 — منع تضارب الإجراءات لدى مستخدم واحد / Prevent one user from holding two conflicting actions
Pattern    : optional
Statement  : Where a consumer module declares two of its actions as conflicting, the system shall prevent a single user from holding both action grants at the same time, whether obtained through one role or several.
Traces     : US-SEC-007
Entities   : ENT-SEC-001, ENT-SEC-003, ENT-SEC-006, ENT-SEC-009
Rationale  : RULE-SEC-005; SoD moved to the shared RBAC layer per general-accounting-system-plan-en.md §8.2/§10.3
Source     : security-module-plan-en.md §4.4
Priority   : MEDIUM
Note       : DEFERRED in SEC v1 — nothing in SEC v1 declares a conflicting pair, so this `optional` pattern's precondition is never established: there is no ENT, DBF, table, API field or screen element for such a declaration. The guards (RULE-SEC-005, QR-SEC-031) are implemented and inert — the conflicting-counterpart set is empty in v1. The platform's only real conflicting pair is FIN-owned and FIN-enforced: governance/modules/FIN/P3_1/backend-execution-plan-fin.md:1061-1066 (RULE-FIN-015, error `FIN-403-SOD-VIOLATION`). Whether SEC v1 should own a conflicting-pair register at all is an open P1/P2 decision, not an execution one.
#### AC-SEC-020 — [REQ-SEC-020]
Given two actions declared conflicting by their owning module, and a user who already holds one of them (via any role)
When an administrator attempts to assign a role that would give that same user the other conflicting action
Then the system rejects the assignment with message ar: "هذا المستخدم يملك إجراءً متعارضًا بالفعل" · en: "This user already holds a conflicting action"

### REQ-SEC-021 — القائمة تعرض الممنوح فقط / Menu shows only effective grants
Pattern    : event
Statement  : When a user's menu is rendered, the system shall include only the modules that user's effective grants hold, each showing only that user's effective granted screens beneath it.
Traces     : US-SEC-008
Entities   : ENT-SEC-004, ENT-SEC-005, ENT-SEC-007, ENT-SEC-008
Rationale  : POL-SEC-006
Source     : security-module-plan-en.md §6
Priority   : HIGH
#### AC-SEC-021 — [REQ-SEC-021]
Given a user whose roles' union grants exactly module FIN with screens "Journal Entries" and "Trial Balance"
When their menu renders
Then the system shows only FIN as a top-level entry with exactly those two screens beneath it

### REQ-SEC-032 — إخفاء الوحدة غير الممنوحة من القائمة / Hide an ungranted module from the menu
Pattern    : unwanted
Statement  : If a user's effective grants do not include a module, then the system shall omit that module entirely from that user's rendered menu.
Traces     : US-SEC-008
Entities   : ENT-SEC-004, ENT-SEC-007
Rationale  : POL-SEC-007 (first half)
Source     : security-module-plan-en.md §4.2, §6
Priority   : HIGH
#### AC-SEC-032 — [REQ-SEC-032]
Given a user whose roles hold no grant for module FIN
When their menu renders
Then FIN does not appear anywhere in the menu

### REQ-SEC-033 — بوابة الوحدة تُفحص على كل طلب / Module gate enforced on every request
Pattern    : ubiquitous
Statement  : The system shall verify a user's effective module grant before allowing any request to a screen or action of that module, independent of menu visibility.
Traces     : US-SEC-008
Entities   : ENT-SEC-004, ENT-SEC-007
Rationale  : POL-SEC-007 (second half) — "not merely hidden... blocked up front, not merely hidden"
Source     : security-module-plan-en.md §4.2
Priority   : HIGH
#### AC-SEC-033 — [REQ-SEC-033]
Given a user whose roles hold no grant for module FIN
When that user directly calls a FIN endpoint or navigates to a FIN screen URL
Then the system denies the request with an authorization error, regardless of how the request was reached

### REQ-SEC-022 — حساب أرقام لوحة التحكم حيًا / Compute dashboard figures live
Pattern    : event
Statement  : When an authorized administrator opens the admin dashboard, the system shall compute every widget figure from live data at that moment.
Traces     : US-SEC-009
Entities   : ENT-SEC-001, ENT-SEC-002, ENT-SEC-010, ENT-SEC-011
Rationale  : POL-SEC-010
Source     : security-module-plan-en.md §5.1-§5.2
Priority   : MEDIUM
Note       : three of the widget figures API-SEC-022 returns are named but never defined here or in AC-SEC-022 — `recentActivity`'s N, the age at which a PENDING sign-up counts as stalled, and what makes a role `privileged` (SCR-REQ-SEC-007 B1-B5, prd-sec.md and all 104 DBF are silent too). Operative definitions, chosen at implementation to fill that silence and subject to a human's confirmation: N = 10 most recent AuditLogEntry rows; stalled = a SignupRequest still PENDING more than 7 days; privileged role = a role holding at least one action grant whose action code is not the VIEW gateway (RULE-SEC-007 / REQ-SEC-030's own VIEW-vs-other distinction). Whether SEC v1 should state these three, or drop the figures from the contract, is an open P1/P2 decision.
#### AC-SEC-022 — [REQ-SEC-022]
Given the dashboard is opened
When it renders
Then every widget (users overview, failed logins 24h, active sessions, recent activity, roles/permissions summary, onboarding funnel) is computed from current data, never from a stored counter

### REQ-SEC-023 — إخفاء عنصر لوحة التحكم غير الممنوح / Hide an ungranted dashboard widget
Pattern    : unwanted
Statement  : If a user's role does not grant a dashboard widget's underlying permission, then the system shall omit that widget for that user.
Traces     : US-SEC-009
Entities   : ENT-SEC-001, ENT-SEC-002, ENT-SEC-007, ENT-SEC-008, ENT-SEC-009
Rationale  : POL-SEC-011
Source     : security-module-plan-en.md §5.1
Priority   : MEDIUM
#### AC-SEC-023 — [REQ-SEC-023]
Given an administrator role without the "active sessions" widget's permission
When that role's user opens the dashboard
Then the active-sessions widget does not appear for that user

### REQ-SEC-024 — تسجيل حدث تدقيقي / Append an audit-log entry
Pattern    : event
Statement  : When any security-relevant event occurs (login, failed login, password reset, role or permission change, session termination), the system shall append one immutable audit-log entry recording it.
Traces     : US-SEC-010
Entities   : ENT-SEC-011
Rationale  : POL-SEC-009
Source     : security-module-plan-en.md §5.2-§5.3
Priority   : MEDIUM
#### AC-SEC-024 — [REQ-SEC-024]
Given any of the listed events occurs
When it completes
Then the system appends one AuditLogEntry with the correct eventTypeCode, actor and timestamp, and no existing entry is altered

### REQ-SEC-025 — بحث/تصفية سجل التدقيق / Search and filter the audit log
Pattern    : event
Statement  : When an administrator searches or filters the audit log, the system shall return matching entries without altering any of them.
Traces     : US-SEC-010
Entities   : ENT-SEC-011
Rationale  : usable audit trail
Source     : security-module-plan-en.md §5.3
Priority   : MEDIUM
#### AC-SEC-025 — [REQ-SEC-025]
Given audit entries exist across several event types and dates
When an administrator filters by event type and date range
Then the system returns exactly the matching entries, unmodified

### REQ-SEC-026 — تصدير سجل التدقيق / Export the audit log
Pattern    : event
Statement  : When an administrator exports the audit log, the system shall produce a CSV file of the currently filtered entries.
Traces     : US-SEC-010
Entities   : ENT-SEC-011
Rationale  : plan §5.3 deliverable "CSV export"
Source     : security-module-plan-en.md §5.3
Priority   : LOW
#### AC-SEC-026 — [REQ-SEC-026]
Given a filtered audit-log view
When the administrator exports it
Then the system produces a CSV file containing exactly the filtered entries' fields

### REQ-SEC-027 — عرض الجلسات النشطة / List active sessions
Pattern    : event
Statement  : When an administrator views the active-sessions screen, the system shall list every session that has not been terminated, with its user and last-activity time.
Traces     : US-SEC-011
Entities   : ENT-SEC-010
Rationale  : plan §5.1, §5.3
Source     : security-module-plan-en.md §5.1, §5.3
Priority   : MEDIUM
#### AC-SEC-027 — [REQ-SEC-027]
Given several sessions exist, some terminated and some not
When an administrator opens the active-sessions screen
Then only the non-terminated sessions are listed, each with its user and last-activity time

### REQ-SEC-028 — إنهاء جلسة قسريًا / Force-terminate a session
Pattern    : event
Statement  : When an authorized administrator force-terminates a session, the system shall immediately end that session and require the affected user to sign in again.
Traces     : US-SEC-011
Entities   : ENT-SEC-010
Rationale  : plan §5.1 "force-terminate a session"
Source     : security-module-plan-en.md §5.1
Priority   : MEDIUM
#### AC-SEC-028 — [REQ-SEC-028]
Given an active session
When an authorized administrator force-terminates it
Then the system sets terminatedAt/terminatedBy and the associated token is no longer accepted for any subsequent request

### REQ-SEC-029 — إشعار اختياري عند إعادة التعيين / Optional notification on password reset
Pattern    : optional
Statement  : Where the Notifications integration is enabled, the system shall dispatch a password-reset message through it when a reset token is issued.
Traces     : US-SEC-012
Entities   : ENT-SEC-012
Rationale  : "only on real need", never a hard dependency
Source     : security-module-plan-en.md §8; new project/integration-notifications-fileservice.md §1
Priority   : LOW
#### AC-SEC-029 — [REQ-SEC-029]
Given the Notifications integration is enabled and a reset token is issued
When the token is created
Then the system dispatches one notification with templateCode identifying the password-reset message, and REQ-SEC-006 succeeds unchanged if the integration is disabled or unavailable

### REQ-SEC-030 — اشتراط VIEW لبقية الإجراءات / VIEW required for any other action on a screen
Pattern    : unwanted
Statement  : If a role does not hold the VIEW action grant for a screen, then the system shall deny every other action on that screen for that role.
Traces     : US-SEC-005
Entities   : ENT-SEC-006, ENT-SEC-009
Rationale  : RULE-SEC-007; profile.conventions.security_model.gateway_action = VIEW
Source     : profiles/erp.yaml conventions.security_model
Priority   : HIGH
#### AC-SEC-030 — [REQ-SEC-030]
Given a role holds CREATE on a screen but not VIEW on that same screen
When that role's user attempts the CREATE action
Then the system denies it until VIEW is also granted on that screen

### REQ-SEC-034 — قراءة بيانات الاتصال بالمستخدم عبر الوحدات / Cross-module read of a user's contact details
Pattern    : optional
Statement  : Where another module must reach a user out of band, the system shall expose that user's email address, bilingual display names and active state — and nothing else — through a read-only cross-module interface.
Traces     : US-SEC-012
Entities   : ENT-SEC-001
Rationale  : REQ-SEC-029's delivery half needs an address for a bare user id; NOTIF's XM-NOTIF-001 recipient-active check needs the same row's state
Source     : AMENDMENT 2026-09-11 — build-create-service "Exposing this module to others"; execution-state.json api_doc_gaps #9
Priority   : LOW
Note       : The surface is `com.erp.sec.crossmodule.SecUserDirectoryApi`, an injected Spring interface — not an HTTP endpoint: `build-create-service` requires cross-module reads to go through "direct Spring interface injection, not loopback HTTP". See §A8's third table.
#### AC-SEC-034 — [REQ-SEC-034]
Given a consumer module that holds only a user id
When it asks SEC for that user's contact details
Then the system returns the email address, both display names and the active flag, and never the password hash (POL-SEC-004) or any role, grant or session data

### REQ-SEC-035 — قراءة حاملي صلاحية معيّنة عبر الوحدات / Cross-module read of the holders of a permission code
Pattern    : optional
Statement  : Where another module enforces a separation-of-duties rule over its own permission codes, the system shall expose to that module the set of user ids currently holding a given permission code through their roles.
Traces     : US-SEC-007
Entities   : ENT-SEC-001, ENT-SEC-003, ENT-SEC-006, ENT-SEC-009
Rationale  : RULE-SEC-005 is inert in SEC v1 (REQ-SEC-020 Note); the platform's only real conflicting pair is FIN-owned and FIN-enforced (RULE-FIN-015), and FIN needs the holder set to enforce it
Source     : AMENDMENT 2026-09-11 — build-create-service "Exposing this module to others"; execution-state.json api_doc_gaps #8
Priority   : MEDIUM
Note       : Same surface as REQ-SEC-034 (`SecUserDirectoryApi`, QR-SEC-039), not an endpoint. It answers with user ids only — the consumer decides; SEC neither learns nor evaluates the consumer's conflicting pair, so REQ-SEC-020's Note stands unchanged.
#### AC-SEC-035 — [REQ-SEC-035]
Given two permission codes that a consumer module declares as conflicting
When that module asks SEC which users hold each of them
Then the system returns, for each code, the set of user ids computed across every active role assignment, and returns nothing else about those users

## A5 — Business rules

### RULE-SEC-001 — منع منح شاشة دون منح الوحدة / No screen grant without its module grant
Scope      : ENT-SEC-008
Trigger    : on create (screen grant)
Statement  : The system shall prevent a screen grant for a role that does not hold the screen's module grant.
Data source: ENT-SEC-007 (the role's module grants) · ENT-SEC-005 (the screen's owning module)
Message    : ar: "لا يمكن منح شاشة دون منح الوحدة أولًا" · en: "Cannot grant a screen without first granting its module"
Traces     : REQ-SEC-013
Source     : security-module-plan-en.md §4.2

### RULE-SEC-002 — منع منح إجراء دون منح الشاشة / No action grant without its screen grant
Scope      : ENT-SEC-009
Trigger    : on create (action grant)
Statement  : The system shall prevent an action grant for a role that does not hold the action's screen grant.
Data source: ENT-SEC-008 (the role's screen grants) · ENT-SEC-006 (the action's owning screen)
Message    : ar: "لا يمكن منح إجراء دون منح الشاشة أولًا" · en: "Cannot grant an action without first granting its screen"
Traces     : REQ-SEC-014
Source     : security-module-plan-en.md §4.2

### RULE-SEC-003 — الإلغاء المتسلسل عند سحب منح الوحدة / Cascade revoke on module-grant removal
Scope      : ENT-SEC-007
Trigger    : on delete (module grant)
Statement  : The system shall delete every screen grant and action grant that module covered for that role when its module grant is revoked.
Data source: ENT-SEC-008 and ENT-SEC-009 (the grants to remove) · ENT-SEC-005 and ENT-SEC-006 (which screens and actions that module covers)
Message    : ar: "سيتم سحب كل منح الشاشات والإجراءات ضمن هذه الوحدة لهذا الدور" · en: "Every screen and action grant under this module for this role will be revoked"
Traces     : REQ-SEC-015
Source     : security-module-plan-en.md §4.2

### RULE-SEC-004 — رفض تسجيل شاشة لوحدة غير مسجّلة / No screen under an unregistered module
Scope      : ENT-SEC-005
Trigger    : on create (screen registration)
Statement  : The system shall reject a screen registration whose module code has no ModuleRegistry row.
Data source: ENT-SEC-004 (the ModuleRegistry row the submitted module code must match)
Message    : ar: "الوحدة غير مسجّلة" · en: "Module is not registered"
Traces     : REQ-SEC-018
Source     : security-module-plan-en.md §4.3

### RULE-SEC-005 — منع تضارب الإجراءات لمستخدم واحد / Prevent conflicting actions on one user
Scope      : ENT-SEC-003, ENT-SEC-009
Trigger    : on create (role assignment or action grant)
Statement  : The system shall prevent assigning a user, by any combination of roles, both actions of a module-declared conflicting pair.
Data source: ENT-SEC-003 (every role the user holds) · ENT-SEC-009 (the action grants those roles carry) · ENT-SEC-006 (the owning module's action declarations, which carry the conflicting pair)
Message    : ar: "هذا المستخدم يملك إجراءً متعارضًا بالفعل" · en: "This user already holds a conflicting action"
Traces     : REQ-SEC-020
Source     : security-module-plan-en.md §4.4; general-accounting-system-plan-en.md §8.2
Note       : DEFERRED in SEC v1 — nothing in SEC v1 declares a conflicting pair, so this `optional` pattern's precondition is never established: there is no ENT, DBF, table, API field or screen element for such a declaration. The guards (RULE-SEC-005, QR-SEC-031) are implemented and inert — the conflicting-counterpart set is empty in v1. The platform's only real conflicting pair is FIN-owned and FIN-enforced: governance/modules/FIN/P3_1/backend-execution-plan-fin.md:1061-1066 (RULE-FIN-015, error `FIN-403-SOD-VIOLATION`). Whether SEC v1 should own a conflicting-pair register at all is an open P1/P2 decision, not an execution one.

### RULE-SEC-006 — رفض رمز إعادة تعيين منتهٍ أو مُستخدَم / Reject expired or used reset token
Scope      : ENT-SEC-012
Trigger    : on submit (password reset completion)
Statement  : The system shall reject a password-reset submission whose token is expired or already used.
Data source: ENT-SEC-012 (the token's expiry and used state)
Message    : ar: "رابط إعادة التعيين غير صالح أو منتهي" · en: "This reset link is invalid or has expired"
Traces     : REQ-SEC-008
Source     : security-module-plan-en.md §3

### RULE-SEC-007 — اشتراط VIEW كبوابة على مستوى الشاشة / VIEW as the screen-level gateway action
Scope      : ENT-SEC-009
Trigger    : on evaluate (any action check) and on create (action grant, informational)
Statement  : The system shall require a role to hold the VIEW action grant on a screen before any other action grant on that screen takes effect for it.
Data source: ENT-SEC-009 (the role's action grants on that screen) · ENT-SEC-006 (which registered action is VIEW, and the screen it belongs to)
Message    : ar: "يلزم منح إجراء العرض (VIEW) أولًا على هذه الشاشة" · en: "The VIEW action must be granted on this screen first"
Traces     : REQ-SEC-030
Source     : profiles/erp.yaml conventions.security_model.gateway_action

## A6 — Lookups

**USER_STATUS** — owned by SEC — used by ENT-SEC-001.statusCode — control type: lookup
| Code | Label (ar) | Label (en) |
|---|---|---|
| PENDING | معلّق | Pending |
| ACTIVE | نشط | Active |
| DISABLED | معطّل | Disabled |
Source: AUTO (module-registry-sec.md → AUTO-DECISIONS), values fixed by the A7 lifecycle below.

**SIGNUP_STATUS** — owned by SEC — used by ENT-SEC-013.statusCode — control type: lookup
| Code | Label (ar) | Label (en) |
|---|---|---|
| PENDING | معلّق | Pending |
| APPROVED | مقبول | Approved |
| REJECTED | مرفوض | Rejected |
DEFAULT: a distinct lookup from USER_STATUS because a rejected sign-up never becomes a
User row (no DISABLED-equivalent needed) — Source: this stage, applying profile.conventions.lookups
("no hardcoded enums") to the SignupRequest lifecycle (REQ-SEC-003/004/005) — Override: merge
into USER_STATUS if the client prefers one shared status set.

**AUDIT_EVENT_TYPE** — owned by SEC — used by ENT-SEC-011.eventTypeCode — control type: lookup
| Code | Label (ar) | Label (en) |
|---|---|---|
| LOGIN_SUCCESS | دخول ناجح | Login success |
| LOGIN_FAILED | دخول فاشل | Login failed |
| LOGOUT | خروج | Logout |
| PASSWORD_RESET_REQUESTED | طلب إعادة تعيين | Password reset requested |
| PASSWORD_RESET_COMPLETED | إتمام إعادة التعيين | Password reset completed |
| ROLE_ASSIGNED | إسناد دور | Role assigned |
| ROLE_REVOKED | سحب دور | Role revoked |
| MODULE_GRANTED | منح وحدة | Module granted |
| MODULE_REVOKED | سحب منح وحدة | Module revoked |
| SCREEN_GRANTED | منح شاشة | Screen granted |
| SCREEN_REVOKED | سحب منح شاشة | Screen revoked |
| ACTION_GRANTED | منح إجراء | Action granted |
| ACTION_REVOKED | سحب منح إجراء | Action revoked |
| SESSION_TERMINATED | إنهاء جلسة | Session terminated |
Source: module-registry-sec.md → AUTO-DECISIONS; profiles/erp.yaml conventions.lookups.

Consumed lookups: none — SEC is a Tier-0 foundation module.

## A7 — Status lifecycle

**ENT-SEC-001 User.statusCode (USER_STATUS, 3 states, >2 transitions — diagram required)**
```
PENDING --(REQ-SEC-004, admin approves sign-up)--> ACTIVE
ACTIVE  --(REQ-SEC-011, admin deactivates)--------> DISABLED
DISABLED--(REQ-SEC-031, admin reactivates)--------> ACTIVE
```
(A User created directly by an administrator — REQ-SEC-009 — starts at ACTIVE, bypassing PENDING.)

**ENT-SEC-013 SignupRequest.statusCode (SIGNUP_STATUS, 3 states)**
```
PENDING --(REQ-SEC-004, approve)--> APPROVED
PENDING --(REQ-SEC-005, reject)---> REJECTED
```
APPROVED and REJECTED are terminal — no further transition.

All other statuses in this module (grant rows, sessions) are a binary
active/terminated flag, not a multi-state lifecycle — not applicable for a diagram.

**DEFAULT — reset-token expiry window**: not stated by the plan; this stage applies a
DEFAULT of 30 minutes from `requestedAt` to `expiresAt` (industry-standard short-lived
reset window) — Source: domain best practice (no conflicting statement in
security-module-plan-en.md or the knowledge file) — Override: configurable value if the
client states a different window; non-breaking, no ADR required (a pure numeric default
with no story/policy it could contradict).

## A8 — Module dependencies

| Consumed entity | Owner ENT id | Owner module | HARD-FK / SOFT-READ | XM candidate (assigned by P2) |
|---|---|---|---|---|
None — SEC is ROOT; it consumes no entity owned by another in-scope module.

| External service | Purpose | Integration kind |
|---|---|---|
| Notifications (ready, external) | password-reset message (REQ-SEC-029) | SOFT / optional, per new project/integration-notifications-fileservice.md §1 |

**AMENDMENT 2026-09-11 — the EXPOSED direction.** The two tables above describe only what SEC
*consumes*; both remain true. SEC additionally exposes one read-only inbound surface. It registers
no entity, table or column, so it carries no XM row here — formal `XM-*` ids for this direction are
assigned by the *consuming* module's own P2, not by SEC.

| Exposed entity | Owner ENT id | Consumer | Read-model | Surface |
|---|---|---|---|---|
| User (contact details only: email + both display names + active) | ENT-SEC-001 | NOTIF — XM-NOTIF-001, and REQ-SEC-029's delivery half | `UserContact` | `com.erp.sec.crossmodule.SecUserDirectoryApi` (REQ-SEC-034) |
| Holders of a permission code (spans User → UserRoleAssignment → RoleActionGrant → ActionRegistry) | ENT-SEC-001, ENT-SEC-003, ENT-SEC-006, ENT-SEC-009 | FIN — RULE-FIN-015 separation of duties | `List<Long>` (user ids only) | `com.erp.sec.crossmodule.SecUserDirectoryApi` (REQ-SEC-035, QR-SEC-039) |

# PART B — SCREEN REQUIREMENTS

## SCR-REQ-SEC-001 — تسجيل الدخول / Login
### B1 — Definition
Purpose      : السماح لمستخدم مسجَّل بالدخول الآمن إلى المنصة.
Entities     : ENT-SEC-001, ENT-SEC-010
Operations   : create (session, via REQ-SEC-001/002)
Users        : أي مستخدم مسجَّل (غير مصادَق بعد)
Navigation   : (public) → Login; to: post-login landing / dashboard per the user's own menu
Content shape: flat record (username/password form)
Traces       : REQ-SEC-001, REQ-SEC-002
Composite    : single screen (no search/detail split)
### B2 — Search / list
Not applicable — no search on this screen.
### B3 — Input
Fields: username (ENT-SEC-001.username), password (write-only, not persisted as entered).
Buttons: "Sign in" → REQ-SEC-001/REQ-SEC-002; "Forgot password?" → navigates to SCR-REQ-SEC-003; "Sign up" → navigates to SCR-REQ-SEC-002.
### B4 — Access
Page code: SEC_LOGIN. Public (pre-authentication) — no role/permission gate applies to reaching this screen itself.
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| login | POST | /api/v1/sec/auth/login | username, password | session/access token | — | REQ-SEC-001, REQ-SEC-002 |

## SCR-REQ-SEC-002 — التسجيل الذاتي / Sign-up
### B1 — Definition
Purpose      : السماح لمستخدم محتمل بتقديم طلب تسجيل ذاتي.
Entities     : ENT-SEC-013
Operations   : create
Users        : زائر غير مصادَق
Navigation   : (public) → Sign-up; from: SCR-REQ-SEC-001
Content shape: flat record
Traces       : REQ-SEC-003
Composite    : single screen
### B2 — Search / list
Not applicable.
### B3 — Input
Fields: email, fullNameAr, fullNameEn (ENT-SEC-013). Button "Submit" → REQ-SEC-003.
### B4 — Access
Page code: SEC_SIGNUP. Public (pre-authentication).
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| submit sign-up | POST | /api/v1/sec/auth/signup | email, fullNameAr, fullNameEn | signup request confirmation | — | REQ-SEC-003 |

## SCR-REQ-SEC-003 — نسيت / إعادة تعيين كلمة المرور / Forgot / reset password
### B1 — Definition
Purpose      : السماح بإعادة تعيين ذاتية آمنة لكلمة المرور.
Entities     : ENT-SEC-012, ENT-SEC-001
Operations   : create (token), update (password)
Users        : أي مستخدم يعرف بريده الإلكتروني المسجَّل
Navigation   : (public) → Forgot password; from: SCR-REQ-SEC-001
Content shape: flat record (two steps: request → token+new password)
Traces       : REQ-SEC-006, REQ-SEC-007, REQ-SEC-008, REQ-SEC-029
Composite    : Wizard (request step + reset step) = ONE screen requirement
### B2 — Search / list
Not applicable.
### B3 — Input
Step 1: email. Step 2: token (from the reset link), new password, confirm password.
### B4 — Access
Page code: SEC_PWD_RESET. Public (pre-authentication).
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| request reset | POST | /api/v1/sec/auth/password-reset/request | email | confirmation (always generic, never reveals whether the email exists) | — | REQ-SEC-006, REQ-SEC-029 |
| complete reset | POST | /api/v1/sec/auth/password-reset/complete | token, newPassword | confirmation | RULE-SEC-006 | REQ-SEC-007, REQ-SEC-008 |

## SCR-REQ-SEC-004 — المستخدمون / Users
### B1 — Definition
Purpose      : إدارة المستخدمين وإسناد الأدوار لهم.
Entities     : ENT-SEC-001, ENT-SEC-002, ENT-SEC-003, ENT-SEC-013
Operations   : search, create, read, update, activate, deactivate; approve/reject a SignupRequest
Users        : مسؤول الأمان
Navigation   : SEC → Authorization → Users; to: user detail (roles tab)
Content shape: flat record (search list + entry form; roles shown as a repeating sub-list on the detail)
Traces       : REQ-SEC-009, REQ-SEC-010, REQ-SEC-011, REQ-SEC-031, REQ-SEC-004, REQ-SEC-005
Composite    : Search + Entry = ONE screen requirement
### B2 — Search / list
Filters: username/email, fullName, statusCode — each corresponds to a result column.
### B3 — Input
Fields: username, email, fullNameAr, fullNameEn, statusCode (ENT-SEC-001); roles multi-select (ENT-SEC-003). Buttons: Activate/Deactivate → REQ-SEC-031/REQ-SEC-011; a separate "Pending sign-ups" tab lists SignupRequest rows with Approve/Reject → REQ-SEC-004/REQ-SEC-005.
### B4 — Access
Page code: SEC_USERS. Actions: VIEW (list/search), CREATE, UPDATE (incl. activate/deactivate/approve/reject), per §7.1 (RULE-SEC-007 gateway).
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| search users | POST | /api/v1/sec/users/search | filters, paging | Page\<User\> | — | REQ-SEC-009 |
| create user | POST | /api/v1/sec/users | user fields | User | — | REQ-SEC-009 |
| update user | PUT | /api/v1/sec/users/{id} | user fields | User | — | REQ-SEC-009 |
| assign roles | PUT | /api/v1/sec/users/{id}/roles | role ids | User with roles | — | REQ-SEC-010 |
| deactivate user | DELETE | /api/v1/sec/users/{id} | id | confirmation | RULE (session termination, REQ-SEC-011) | REQ-SEC-011 |
| reactivate user | PATCH | /api/v1/sec/users/{id} | status=ACTIVE | User | — | REQ-SEC-031 |
| approve/reject signup | PATCH | /api/v1/sec/signup-requests/{id} | decision | User (on approve) / SignupRequest (on reject) | — | REQ-SEC-004, REQ-SEC-005 |

## SCR-REQ-SEC-005 — الأدوار والصلاحيات (محرر المنح الثلاثي) / Roles & permissions (3-level grant editor)
### B1 — Definition
Purpose      : إدارة الأدوار ومنحها الوحدات ثم الشاشات ثم الإجراءات، بالترتيب الهرمي.
Entities     : ENT-SEC-002, ENT-SEC-004, ENT-SEC-005, ENT-SEC-006, ENT-SEC-007, ENT-SEC-008, ENT-SEC-009
Operations   : search, create, read, update, deactivate (role); create/delete (module/screen/action grants)
Users        : مسؤول الأمان
Navigation   : SEC → Authorization → Roles & permissions; from: SEC_USERS (role reference)
Content shape: true hierarchy (parent/child) — module → screen → action tree per role
Traces       : REQ-SEC-012, REQ-SEC-013, REQ-SEC-014, REQ-SEC-015, REQ-SEC-020, REQ-SEC-030
Composite    : Master (role list) + Detail (3-level grant tree) = ONE screen requirement
### B2 — Search / list
Filters: role code/name, active flag — correspond to result columns.
### B3 — Input
Role fields: code, nameAr, nameEn, description. Grant tree: check a module (→ REQ-SEC-012), then screens within it (→ REQ-SEC-013/RULE-SEC-001 blocks illegal ones), then actions within a checked screen (→ REQ-SEC-014/RULE-SEC-002, RULE-SEC-007 VIEW gateway); unchecking a module cascades (REQ-SEC-015/RULE-SEC-003). A conflicting-action pair (RULE-SEC-005) blocks the second grant with its message.
### B4 — Access
Page code: SEC_ROLES. Actions: VIEW, CREATE, UPDATE, DELETE (deactivate role), plus grant-tree edits under UPDATE.
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| search roles | POST | /api/v1/sec/roles/search | filters, paging | Page\<Role\> | — | REQ-SEC-012 |
| create role | POST | /api/v1/sec/roles | role fields | Role | — | REQ-SEC-012 |
| grant module | POST | /api/v1/sec/roles/{id}/modules | moduleId | RoleModuleGrant | — | REQ-SEC-012 |
| revoke module | DELETE | /api/v1/sec/roles/{id}/modules/{moduleId} | — | confirmation | RULE-SEC-003 | REQ-SEC-015 |
| grant screen | POST | /api/v1/sec/roles/{id}/screens | screenId | RoleScreenGrant | RULE-SEC-001 | REQ-SEC-013 |
| grant action | POST | /api/v1/sec/roles/{id}/actions | actionId | RoleActionGrant | RULE-SEC-002, RULE-SEC-005, RULE-SEC-007 | REQ-SEC-014, REQ-SEC-020, REQ-SEC-030 |

## SCR-REQ-SEC-006 — سجل الوحدة/الشاشة/الإجراء / Module / screen / action registry
### B1 — Definition
Purpose      : عرض وإدارة ما سجّلته كل وحدة مستهلكة من وحدات/شاشات/إجراءات كبيانات.
Entities     : ENT-SEC-004, ENT-SEC-005, ENT-SEC-006
Operations   : search, read, deactivate (modules/screens/actions arrive via each module's own registration call, not typed here by hand)
Users        : مسؤول الأمان / مطوّر الوحدة المستهلكة
Navigation   : SEC → Authorization → Module/screen/action registry
Content shape: true hierarchy (module → screen → action)
Traces       : REQ-SEC-016, REQ-SEC-017, REQ-SEC-018, REQ-SEC-019
Composite    : Master-detail tree = ONE screen requirement
### B2 — Search / list
Filters: module code, screen page code — correspond to result columns.
### B3 — Input
Read-mostly: registration itself happens via the registering module's own onboarding call (REQ-SEC-016/017/019); this screen's own edit surface is limited to deactivating a stale row.
### B4 — Access
Page code: SEC_MODULE_REGISTRY. Actions: VIEW, UPDATE (deactivate only).
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| register module | POST | /api/v1/sec/registry/modules | code, nameAr, nameEn | ModuleRegistry | — | REQ-SEC-016 |
| register screen | POST | /api/v1/sec/registry/screens | moduleCode, pageCode, nameAr, nameEn | ScreenRegistry | RULE-SEC-004 | REQ-SEC-017, REQ-SEC-018 |
| register action | POST | /api/v1/sec/registry/actions | pageCode, actionCode, nameAr, nameEn | ActionRegistry | — | REQ-SEC-019 |
| search registry | POST | /api/v1/sec/registry/search | filters, paging | Page\<registry rows\> | — | REQ-SEC-016 |

## SCR-REQ-SEC-007 — لوحة تحكم الأمان / Admin dashboard
### B1 — Definition
Purpose      : عرض حالة الأمان العامة للمنصة بشكل حي.
Entities     : ENT-SEC-001, ENT-SEC-002, ENT-SEC-010, ENT-SEC-011
Operations   : read (aggregate figures)
Users        : مسؤول الأمان (لكل عنصر لوحة تحكم بمقتضى صلاحيته الخاصة)
Navigation   : SEC → Monitoring → Dashboard; to: SCR-REQ-SEC-008 (audit), SCR-REQ-SEC-009 (sessions)
Content shape: other (dashboard — a grid of independent widgets, not a record/list)
Traces       : REQ-SEC-022, REQ-SEC-023
Composite    : single screen (widgets are not separate screen requirements)
### B2 — Search / list
Not applicable — aggregate widgets, not a browsable list.
### B3 — Input
Read-only; no data entry.
### B4 — Access
Page code: SEC_DASHBOARD. Action: VIEW; each widget additionally requires the VIEW permission of the screen it summarizes (REQ-SEC-023) — e.g. the active-sessions widget requires SEC_SESSIONS VIEW.
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| dashboard summary | GET | /api/v1/sec/dashboard | — | live aggregate figures per widget the caller may see | — | REQ-SEC-022, REQ-SEC-023 |

## SCR-REQ-SEC-008 — سجل التدقيق / Audit log
### B1 — Definition
Purpose      : تصفح وتصدير سجل الأحداث الأمنية غير القابل للتعديل.
Entities     : ENT-SEC-011
Operations   : search, export
Users        : مسؤول الأمان
Navigation   : SEC → Monitoring → Audit log; from: SCR-REQ-SEC-007
Content shape: flat record (append-only list)
Traces       : REQ-SEC-024, REQ-SEC-025, REQ-SEC-026
Composite    : single screen (search list; no entry — audit rows are never hand-created)
### B2 — Search / list
Filters: eventTypeCode, actorUserId, date range — each corresponds to a result column.
### B3 — Input
Not applicable — no create/update; rows are system-appended only (REQ-SEC-024).
### B4 — Access
Page code: SEC_AUDIT_LOG. Action: VIEW (search + export share the same permission — export is not a separate mutation).
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| search audit log | POST | /api/v1/sec/audit-log/search | filters, paging | Page\<AuditLogEntry\> | — | REQ-SEC-025 |
| export audit log | GET | /api/v1/sec/audit-log/export | filters | CSV file | — | REQ-SEC-026 |

## SCR-REQ-SEC-009 — إدارة الجلسات النشطة / Active sessions management
### B1 — Definition
Purpose      : عرض الجلسات الحية وإنهاء أي منها قسريًا عند الحاجة.
Entities     : ENT-SEC-010
Operations   : search, terminate
Users        : مسؤول الأمان
Navigation   : SEC → Monitoring → Active sessions; from: SCR-REQ-SEC-007
Content shape: flat record (live list)
Traces       : REQ-SEC-027, REQ-SEC-028
Composite    : single screen (search list + a terminate action; no separate entry form)
### B2 — Search / list
Filters: user, IP address — correspond to result columns.
### B3 — Input
Not applicable for create/update; the only mutation is "Terminate" per row → REQ-SEC-028.
### B4 — Access
Page code: SEC_SESSIONS. Actions: VIEW, DELETE (terminate, RULE-SEC-007 gateway applies).
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| list active sessions | POST | /api/v1/sec/sessions/search | filters, paging | Page\<ActiveSession\> | — | REQ-SEC-027 |
| terminate session | DELETE | /api/v1/sec/sessions/{id} | id | confirmation | — | REQ-SEC-028 |

## SCR-REQ-SEC-010 — القائمة الديناميكية ثنائية المستوى / Dynamic two-tier menu
### B1 — Definition
Purpose      : عرض قائمة تنقّل لكل مستخدم مبنية من منحه الفعلية فقط.
Entities     : ENT-SEC-004, ENT-SEC-005, ENT-SEC-007, ENT-SEC-008
Operations   : read (rendered on every authenticated page load)
Users        : أي مستخدم مصادَق
Navigation   : rendered globally; not itself a destination
Content shape: other (navigation component, not a record/list)
Traces       : REQ-SEC-021, REQ-SEC-032, REQ-SEC-033
Composite    : single global component = ONE screen requirement (not per-module)
### B2 — Search / list
Not applicable.
### B3 — Input
Not applicable — read-only, derived.
### B4 — Access
No page code of its own (it is not a securable destination); its content is filtered
per-user by the same module/screen grants each target page already enforces (REQ-SEC-033).
### B5 — API expectations
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| effective menu | GET | /api/v1/sec/menu | — | modules → screens tree, effective grants only | — | REQ-SEC-021, REQ-SEC-032 |

# STANDALONE

## Traceability matrix
| P0.5 | REQ | AC | RULE | ENT | SCR-REQ |
|---|---|---|---|---|---|
| US-SEC-001 | REQ-SEC-001, REQ-SEC-002 | AC-SEC-001, AC-SEC-002 | — | ENT-SEC-001, ENT-SEC-010, ENT-SEC-011 | SCR-REQ-SEC-001 |
| US-SEC-002 | REQ-SEC-003, REQ-SEC-004, REQ-SEC-005 | AC-SEC-003…005 | — | ENT-SEC-013, ENT-SEC-001 | SCR-REQ-SEC-002, SCR-REQ-SEC-004 |
| US-SEC-003 | REQ-SEC-006, REQ-SEC-007, REQ-SEC-008 | AC-SEC-006…008 | RULE-SEC-006 | ENT-SEC-012, ENT-SEC-001 | SCR-REQ-SEC-003 |
| US-SEC-004 | REQ-SEC-009, REQ-SEC-010, REQ-SEC-011, REQ-SEC-031 | AC-SEC-009…011, AC-SEC-031 | — | ENT-SEC-001, ENT-SEC-002, ENT-SEC-003, ENT-SEC-010 | SCR-REQ-SEC-004 |
| US-SEC-005 | REQ-SEC-012, REQ-SEC-013, REQ-SEC-014, REQ-SEC-015, REQ-SEC-030 | AC-SEC-012…015, AC-SEC-030 | RULE-SEC-001, RULE-SEC-002, RULE-SEC-003, RULE-SEC-007 | ENT-SEC-002, ENT-SEC-004…009 | SCR-REQ-SEC-005 |
| US-SEC-006 | REQ-SEC-016, REQ-SEC-017, REQ-SEC-018, REQ-SEC-019 | AC-SEC-016…019 | RULE-SEC-004 | ENT-SEC-004, ENT-SEC-005, ENT-SEC-006 | SCR-REQ-SEC-006 |
| US-SEC-007 | REQ-SEC-020, REQ-SEC-035 | AC-SEC-020, AC-SEC-035 | RULE-SEC-005 | ENT-SEC-001, ENT-SEC-003, ENT-SEC-006, ENT-SEC-009 | SCR-REQ-SEC-005 |
| US-SEC-008 | REQ-SEC-021, REQ-SEC-032, REQ-SEC-033 | AC-SEC-021, AC-SEC-032, AC-SEC-033 | — | ENT-SEC-004, ENT-SEC-005, ENT-SEC-007, ENT-SEC-008 | SCR-REQ-SEC-010 |
| US-SEC-009 | REQ-SEC-022, REQ-SEC-023 | AC-SEC-022, AC-SEC-023 | — | ENT-SEC-001, ENT-SEC-002, ENT-SEC-010, ENT-SEC-011 | SCR-REQ-SEC-007 |
| US-SEC-010 | REQ-SEC-024, REQ-SEC-025, REQ-SEC-026 | AC-SEC-024…026 | — | ENT-SEC-011 | SCR-REQ-SEC-008 |
| US-SEC-011 | REQ-SEC-027, REQ-SEC-028 | AC-SEC-027, AC-SEC-028 | — | ENT-SEC-010 | SCR-REQ-SEC-009 |
| US-SEC-012 | REQ-SEC-029, REQ-SEC-034 | AC-SEC-029, AC-SEC-034 | — | ENT-SEC-012, ENT-SEC-001 | SCR-REQ-SEC-003 |

Every story traces to ≥1 REQ; every REQ traces to ≥1 AC; every RULE traces to a REQ;
every SCR-REQ traces to ≥1 REQ (rows above). No orphan, no dangling id.

## Decisions applied
| DEFAULT / ADR | What | Source | Override / status |
|---|---|---|---|
| DEFAULT | Password-reset token expiry = 30 minutes | domain best practice (A7 note) | configurable; non-breaking |
| DEFAULT | USER_STATUS/AUDIT_EVENT_TYPE/SIGNUP_STATUS initial lookup values | profiles/erp.yaml conventions.lookups + lookup-module-plan-en.md §3 | extend the value set as new events/states are needed; non-breaking |
No ADR was raised — no ambiguity reached the breaking/non-breaking fork of §9; every
point was settled by business policies, PRD stories, the registries or the knowledge
source, or documented above as a plain DEFAULT.

## Access summary
| Page code | Screen | VIEW | CREATE | UPDATE | DELETE | Custom |
|---|---|---|---|---|---|---|
| SEC_LOGIN | Login | public | — | — | — | — |
| SEC_SIGNUP | Sign-up | public | — | — | — | — |
| SEC_PWD_RESET | Forgot/reset password | public | — | — | — | — |
| SEC_USERS | Users | role-granted | role-granted | role-granted (incl. activate/deactivate, approve/reject signup) | — | — |
| SEC_ROLES | Roles & permissions | role-granted | role-granted | role-granted (grant tree edits) | role-granted (deactivate role) | — |
| SEC_MODULE_REGISTRY | Module/screen/action registry | role-granted | (via registering module's own call) | role-granted (deactivate row) | — | — |
| SEC_DASHBOARD | Admin dashboard | role-granted (+ per-widget VIEW of its source screen) | — | — | — | — |
| SEC_AUDIT_LOG | Audit log | role-granted | — | — | — | export (shares VIEW) |
| SEC_SESSIONS | Active sessions | role-granted | — | — | role-granted (terminate) | — |
| (menu) | Dynamic menu | derived from the above — no page code of its own | — | — | — | — |
Every action beyond VIEW additionally requires VIEW on the same screen (RULE-SEC-007).
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 01, 04, 05, 06, 08, 10, 11, 14 (shipped in 1.1.0), 15 (shipped in 1.2.0)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Paths cited below are relative to the erp-core repository at that tag. Endpoint paths and methods are
taken from `docs/api-docs/sec/`. No REQ / AC / RULE / ENT ids are minted here; items are labelled
NEW / CHANGED / REMOVED for the factory to absorb.

### 1. Endpoints
| Kind | Method | Path | Access | Notes | Source |
|---|---|---|---|---|---|
| NEW | POST | `/api/v1/public/customers/register` | public (tenant from `X-Tenant-Code`) | body `email`, `password` (min 8), `fullName` (fills `fullNameAr` and `fullNameEn`); creates a `PENDING_VERIFICATION` CUSTOMER account and dispatches `CUSTOMER_VERIFY_EMAIL` | docs/api-docs/sec/endpoints/customer-accounts-public.md; DEVIATIONS [06] |
| NEW | POST | `/api/v1/public/customers/verify` | public | consumes the verification token (24 h, single use, stored hashed) → ACTIVE | same |
| NEW | POST | `/api/v1/public/customers/login` | public | body `email`, `password`; rate limited; a successful login is written as `LOGIN` to `CORE_AUDIT_EVENT` | same; DEVIATIONS [10] |
| NEW | POST | `/api/v1/public/customers/password-reset/request` | public | dispatches `CUSTOMER_PASSWORD_RESET` (reuses `SEC_PWD_RESET_TOKEN`) | same |
| NEW | POST | `/api/v1/public/customers/password-reset/complete` | public | also verifies a `PENDING_VERIFICATION` account | same |
| NEW | GET | `/api/v1/customers/me` | `ROLE_CUSTOMER` | own profile | docs/api-docs/sec/endpoints/customer-accounts-self.md |
| NEW | PATCH | `/api/v1/customers/me` | `ROLE_CUSTOMER` | edits `fullNameAr`, `fullNameEn` only | same |
| CHANGED | POST | `/api/v1/sec/auth/login` | public | needs the tenant (`X-Tenant-Code`); token carries `realm`, `tid`, `jti`; success recorded as `LOGIN` in `CORE_AUDIT_EVENT` | docs/steps/05-report.md, 06-report.md; DEVIATIONS [10] |
| CHANGED | POST | `/api/v1/sec/auth/logout`, `/api/v1/sec/auth/password-reset/complete` | as before | also recorded as `LOGOUT` / `PASSWORD_RESET` in `CORE_AUDIT_EVENT` | DEVIATIONS [10] |
| CHANGED | GET / PUT / PATCH / DELETE | `/api/v1/sec/users/{id}`, PUT `/api/v1/sec/users/{id}/roles` | as before | STAFF accounts only: a CUSTOMER id answers 404 `SEC-404-USER` (as an unknown id) and changes nothing | DEVIATIONS [14] |
| CHANGED | POST | `/api/v1/sec/users/search` | as before | always restricted to `realm = STAFF`; `UserResponse` carries `realm` | DEVIATIONS [06], [14] |
| CHANGED | POST / DELETE | `/api/v1/sec/sessions/search`, `/api/v1/sec/sessions/{id}` | as before | STAFF sessions only; a customer session id → 404 `SEC-404-SESSION` | DEVIATIONS [14] |
| CHANGED | GET | `/api/v1/sec/dashboard` | as before | user and session counts cover STAFF accounts only | DEVIATIONS [14] |
| CHANGED | POST | `/api/v1/sec/auth/signup`, PATCH `/api/v1/sec/signup-requests/{id}` | as before | an approved sign-up creates a STAFF account | docs/steps/06-report.md |
| NEW (report) | POST | `/api/v1/report/SEC_USER_LIST/run`, `/api/v1/report/SEC_USER_LIST/export` | `SEC:REPORT:SEC_USER_LIST` | params `realm`, `status`, `activeOnly`, `createdFrom`, `createdTo`; never returns the password hash | DEVIATIONS [11] |
| AS-BUILT (v2 G5) | GET | `/api/v1/sec/users/{id}` | `PERM_SEC_USERS_VIEW` | one of the five endpoints ADR-SEC-038 declared for the SRS screens (the SEC v2 service-account change set itself was never implemented); STAFF accounts only | docs/api-docs/sec/endpoints/users.md; ADR-SEC-038 |
| AS-BUILT (v2 G5) | GET | `/api/v1/sec/roles/{id}` | `PERM_SEC_ROLES_VIEW` | same; role detail | docs/api-docs/sec/endpoints/roles.md; ADR-SEC-038 |
| AS-BUILT (v2 G5) | PUT | `/api/v1/sec/roles/{id}` | `PERM_SEC_ROLES_UPDATE` | same; role update | docs/api-docs/sec/endpoints/roles.md; ADR-SEC-038 |
| AS-BUILT (v2 G5) | GET | `/api/v1/sec/roles/{id}/grants` | `PERM_SEC_ROLES_VIEW` | same; a role's module/screen/action grants | docs/api-docs/sec/endpoints/role-grants.md; ADR-SEC-038 |
| AS-BUILT (v2 G5) | POST | `/api/v1/sec/signup-requests/search` | `PERM_SEC_USERS_VIEW` | same; the list behind the "Pending sign-ups" tab | docs/api-docs/sec/endpoints/sign-up-requests.md; ADR-SEC-038 |
All other SEC endpoints keep their analysed behaviour, now confined to the caller's tenant.

### 2. Business rules
| Kind | Rule | Source |
|---|---|---|
| NEW | Every SEC read and write is confined to the request tenant (Hibernate `@TenantId`); an id of another tenant answers as not found. | docs/steps/05-report.md |
| NEW | `SEC_USER.REALM` ∈ {STAFF, CUSTOMER}, immutable. Username and e-mail are unique per (tenant, realm). Staff lookups (login, reset, sign-up) are STAFF-realm queries; customer flows use realm-aware lookups. | V11; DEVIATIONS [06] |
| NEW | A token of the other realm on a non-public path of a chain → 403 `REALM_MISMATCH` (checked from the token's `realm` claim). A token without a known `realm` no longer authenticates. | DEVIATIONS [06] |
| NEW | A CUSTOMER principal holds only `ROLE_CUSTOMER`; no grant query is run for it. Customers hold no roles. | docs/steps/06-report.md |
| NEW | Customer login before verification → 403 `CUSTOMER_NOT_VERIFIED`. Verification token: 24 h, single use, SHA-256 hash stored. | DEVIATIONS [06] |
| NEW | Customer login rate limit: in-memory buckets keyed `tenantId:realm:username` (lower-cased), default 10 per minute (`erp.core.security.customer-login-rate-limit.*`) → 429 `CUSTOMER_LOGIN_RATE_LIMITED`. Staff login has no limiter. | DEVIATIONS [06] |
| NEW | Super role: a role with `IS_SUPER = TRUE` (every tenant's `SYS_ADMIN`) holds every active catalog authority; `PLATFORM`-module authorities only inside the PLATFORM tenant. The navigation menu is still built from grants. | DEVIATIONS [06] (super role entry) |
| NEW | Bootstrap admin: seeded `PENDING` with a non-BCrypt placeholder; `erp.core.security.bootstrap-admin-password` is applied once on the first start (hash + activate), never again. | docs/steps/04-report.md |
| CHANGED | RULE-NOTIF-007 interplay: a user "may receive notifications" when ACTIVE or `PENDING_VERIFICATION` (`UserContact.active`). | DEVIATIONS [06] |
| CHANGED | RULE-SEC-006 (reset token): a token is usable only for its owner's realm; a token of the other realm answers `SEC-409-RESET-TOKEN-INVALID`. Window stays 30 min. | DEVIATIONS [06]; `PasswordResetToken.DEFAULT_VALIDITY_WINDOW` |
| CHANGED | REQ-SEC-024 (audit append): `SEC_AUDIT_LOG` unchanged; LOGIN / LOGOUT / PASSWORD_RESET are additionally written to `CORE_AUDIT_EVENT`, and User / Role changes are recorded field by field via `@Audited` (`passwordHash`, `lastLoginAt` ignored). Customer register / login / reset are not written to `SEC_AUDIT_LOG`. | DEVIATIONS [10], [06] |
| CHANGED | Optimistic locking on every SEC row (`VERSION`); a concurrent update → 409 `CONCURRENT_MODIFICATION`. | DEVIATIONS [05] |
| NEW | SEC publishes `UserCreatedEvent` (create, sign-up approval), `UserStatusChangedEvent` (deactivate / reactivate), `PasswordResetRequestedEvent` (token row id + expiry, never the raw token), `CustomerRegisteredEvent`, `CustomerVerifiedEvent`. | DEVIATIONS [08] |

### 3. Error codes
| Kind | Code | HTTP | Raised when | Source |
|---|---|---|---|---|
| NEW | `REALM_MISMATCH` | 403 | token of the other realm | docs/api-docs/sec/index.md |
| NEW | `CUSTOMER_EMAIL_TAKEN` | 409 | register with an e-mail already holding a customer account in the tenant | same |
| NEW | `CUSTOMER_NOT_VERIFIED` | 403 | customer login before verification | same |
| NEW | `VERIFY_TOKEN_INVALID` | 409 | unknown, expired or used verification token | same |
| NEW | `CUSTOMER_LOGIN_RATE_LIMITED` | 429 | customer login over the limit (new `Status.TOO_MANY_REQUESTS`) | same; DEVIATIONS [06] |
| NEW (tenant module, surfaced on SEC paths) | `TENANT_REQUIRED` / `TENANT_NOT_FOUND` / `TENANT_SUSPENDED` | 400 / 404 / 403 | missing / unknown / suspended tenant | docs/api-docs/tenant/index.md |
| NEW (common) | `CONCURRENT_MODIFICATION` | 409 | optimistic-lock conflict | DEVIATIONS [05] |
| NEW (common, 1.2.0) | `NOT_FOUND` | 404 | unknown path (was 500) | CHANGELOG [1.2.0] |
All analysed `SEC-*` codes are unchanged.

### 4. Permissions (exact authority strings)
| Kind | Authority | Screen / meaning | Source |
|---|---|---|---|
| unchanged (now code-defined in `SecPermissions`) | `PERM_SEC_USERS_VIEW`, `PERM_SEC_USERS_CREATE`, `PERM_SEC_USERS_UPDATE`, `PERM_SEC_ROLES_VIEW`, `PERM_SEC_ROLES_CREATE`, `PERM_SEC_ROLES_UPDATE`, `PERM_SEC_ROLES_DELETE`, `PERM_SEC_MODULE_REGISTRY_VIEW`, `PERM_SEC_MODULE_REGISTRY_UPDATE`, `PERM_SEC_DASHBOARD_VIEW`, `PERM_SEC_AUDIT_LOG_VIEW`, `PERM_SEC_SESSIONS_VIEW`, `PERM_SEC_SESSIONS_DELETE` | SEC screens of the access summary above | erp-core/src/main/java/com/erp/sec/permission/SecPermissions.java |
| NEW | `ROLE_CUSTOMER` | the only authority of a CUSTOMER principal (not a catalog row) | same |
| NEW | `SEC:REPORT:SEC_USER_LIST` + gateway `PERM_SEC_REPORTS_VIEW` (screen `SEC_REPORTS`) | users report | DEVIATIONS [11] |
| REMOVED | 30 `PERM_FIN_*` constants (`PermissionConstants` deleted in step 06) | `fin` module deleted | docs/steps/01-report.md |
| CHANGED | authority format stays `PERM_<SCREEN>_<ACTION>` (the step file's `module:screen:action` was not adopted, per its "keep the existing format" rule) | — | DEVIATIONS [06] |

### 5. Entities, fields, lifecycle
| Kind | Item | Delta | Source |
|---|---|---|---|
| CHANGED | ENT-SEC-001 User | + `realm` (STAFF / CUSTOMER), + `tenantId`, + `version`; `username` / `email` unique per (tenant, realm). Analysis said "unique" (global); implemented per (tenant, realm) because steps 05 and 06 scope users by tenant and realm. | V10, V11 |
| CHANGED | ENT-SEC-002 Role | + `isSuper`, + `tenantId`, + `version`; `code` unique per tenant | V10, V11 |
| CHANGED | ENT-SEC-003, 007..013 | + `tenantId`, + `version`; the 8 rows the analysis kept without audit fields now carry nullable `createdBy/At`, `updatedBy/At` (backfilled from their own lifecycle columns) | V10 §4; DEVIATIONS [05] |
| CHANGED | ENT-SEC-004..006 (registries) | global (no tenant), + `version`; rows upserted from code at startup | V10 §3; docs/steps/06-report.md |
| NEW | CustomerVerifyToken | `userId`, `tokenHash`, `expiresAt`, `usedAt` (tenant-scoped) | V11 §5 |
| CHANGED | USER_STATUS | + `PENDING_VERIFICATION` | V11 §3 |
| CHANGED | A7 User lifecycle | added: (customer register) → `PENDING_VERIFICATION` → (verify, or customer reset completion) → `ACTIVE`; bootstrap `admin` `PENDING` → `ACTIVE` by the bootstrap runner. A staff administrator can no longer move a customer through DISABLED → ACTIVE (that bypassed verification). | DEVIATIONS [06], [04], [14] |

### 6. Dependencies (A8) — deltas
| Direction | Item | Source |
|---|---|---|
| consumed | tenant: `CORE_TENANT` (FK of every `TENANT_ID`), `TenantContext`, provisioning SPI | docs/steps/05-report.md |
| consumed | NOTIF `NotificationDispatchApi` (in-core, best effort, `dispatchIndependently`) | DEVIATIONS [06] |
| consumed | audit `AuditApi`, `@Audited`; report `ReportProvider` | DEVIATIONS [10], [11] |
| exposed | `SecUserDirectoryApi.findCurrentUserId()` NEW (NOTIF inbox); `findUserIdsHoldingPermission` (REQ-SEC-035) has no consumer since FIN's removal; `findContact` (REQ-SEC-034) is realm-neutral (customers receive notifications) | DEVIATIONS [08], [14] |
| exposed | permission SPI `com.erp.sec.permission.*` for every module | docs/steps/06-report.md |

## Implementation Addendum — erp-core 1.3.0
Source version : erp-core 1.3.0 (unreleased, main)
Change         : tenant-maturity plan package G — revoke a single screen or action grant (closes D7 of `docs/plans/tenant-maturity-plan.md`)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Gap closed. ENT-SEC-008 and ENT-SEC-009 (§A3) list "delete (revoke)" among their operations, and the
AUDIT_EVENT_TYPE lookup (§A6) carries `SCREEN_REVOKED` and `ACTION_REVOKED`, but SCR-REQ-SEC-005 B5
listed only the module revoke, so no endpoint ever deleted one screen or one action grant: taking a
single screen away meant revoking the whole module and granting the rest again. The ids below
continue the module's sequence from the highest number ever issued, not the highest in this file: the
pre-vendoring SEC analysis (`governance-shared`, v1 and v2) issued up to REQ-SEC-079, AC-SEC-085 and RULE-SEC-053,
and some of those ids are still cited in code (e.g. REQ-SEC-036 / AC-SEC-036 for logout in `AuthService` and
`SecLogoutIntegrationTest`), so this addendum starts at REQ-SEC-080, AC-SEC-086, RULE-SEC-054. No ENT, DBF or
lookup value is added, and there is **no schema change and no migration**: both grant tables, their
`SEQ_*`/`UQ_*`/`FK_*` objects and the `CHK_SEC_AUDIT_LOG_EVENT_TYPE` value set (V4__sec_schema.sql)
already hold everything this addendum needs.

### 1. Requirements (§A4) — NEW

### REQ-SEC-080 — سحب منح شاشة من دور / Revoke a screen grant from a role
Pattern    : event
Statement  : When an administrator revokes a role's screen grant, the system shall delete that screen grant and every action grant that role holds on that screen, in one transaction.
Traces     : US-SEC-005
Entities   : ENT-SEC-002, ENT-SEC-008, ENT-SEC-009
Rationale  : RULE-SEC-054; the screen-level counterpart of REQ-SEC-015 — an action grant never outlives its screen grant (RULE-SEC-002 read in reverse, POL-SEC-002)
Source     : docs/plans/tenant-maturity-plan.md §0 D7, §8b
Priority   : HIGH
#### AC-SEC-086 — [REQ-SEC-080]
Given a role holding a screen grant and three action grants on that screen (VIEW, CREATE, UPDATE)
When an administrator revokes that screen grant
Then the system deletes the screen grant and the three action grants, answers `revokedActionGrants = 3`, appends one `SCREEN_REVOKED` and three `ACTION_REVOKED` SEC audit-log entries (N + 1 = 4), and leaves the role's module grant and every other screen's grants untouched

### REQ-SEC-081 — سحب منح إجراء من دور / Revoke an action grant from a role
Pattern    : event
Statement  : When an administrator revokes a role's action grant, the system shall delete that action grant; where the revoked action is the screen's VIEW gateway action, the system shall also delete every other action grant that role holds on the same screen.
Traces     : US-SEC-005
Entities   : ENT-SEC-002, ENT-SEC-006, ENT-SEC-009
Rationale  : RULE-SEC-055, the inverse of RULE-SEC-007: without VIEW the screen's other action grants have no effect (REQ-SEC-030), so leaving them would keep dormant grants that silently come back the day VIEW is granted again — ADR-SEC-062
Source     : docs/plans/tenant-maturity-plan.md §0 D7, §8b, §9 (plan name ADR-SEC-041)
Priority   : HIGH
#### AC-SEC-087 — [REQ-SEC-081]
Given a role holding VIEW, CREATE and UPDATE on one screen
When an administrator revokes the CREATE action grant
Then the system deletes exactly that grant, answers `revokedActionGrants = 1` and appends one `ACTION_REVOKED` entry;
and when the administrator then revokes the VIEW action grant
Then the system deletes VIEW and UPDATE, answers `revokedActionGrants = 2`, appends two `ACTION_REVOKED` entries, and keeps the role's screen grant (only the action level is revoked)

### 2. Business rules (§A5) — NEW

### RULE-SEC-054 — الإلغاء المتسلسل عند سحب منح الشاشة / Cascade revoke on screen-grant removal
Scope      : ENT-SEC-008
Trigger    : on delete (screen grant)
Statement  : The system shall delete every action grant of that screen for that role when its screen grant is revoked.
Data source: ENT-SEC-009 (the role's action grants) · ENT-SEC-006 (the screen each action belongs to)
Message    : ar: "سيتم سحب كل منح الإجراءات ضمن هذه الشاشة لهذا الدور" · en: "Every action grant under this screen for this role will be revoked"
Traces     : REQ-SEC-080
Source     : docs/plans/tenant-maturity-plan.md §8b
Decided by : `RoleScreenGrantDomain` (the cascade set), not the service

### RULE-SEC-055 — سحب العرض (VIEW) يسحب بقية إجراءات الشاشة / Revoking VIEW cascades the screen's other action grants
Scope      : ENT-SEC-009
Trigger    : on delete (action grant whose action code is the gateway `VIEW`)
Statement  : The system shall delete every other action grant that role holds on the same screen when the role's VIEW action grant on that screen is revoked; revoking any other action deletes that action grant only.
Data source: ENT-SEC-009 (the role's action grants on that screen) · ENT-SEC-006 (which registered action is VIEW, and the screen it belongs to)
Message    : ar: "سحب إجراء العرض (VIEW) يسحب بقية إجراءات هذه الشاشة لهذا الدور" · en: "Revoking VIEW also revokes this role's other actions on this screen"
Traces     : REQ-SEC-081
Source     : docs/plans/tenant-maturity-plan.md §8b; ADR-SEC-062
Decided by : `RoleActionGrantDomain` (the gateway test `isGatewayAction` and the cascade set), not the service

### 3. SCR-REQ-SEC-005 — CHANGED
| Kind | Item | Delta |
|---|---|---|
| CHANGED | B1 Traces | + REQ-SEC-080, REQ-SEC-081 |
| CHANGED | B3 Input | Unchecking a **screen** in the grant tree revokes that screen grant (REQ-SEC-080 / RULE-SEC-054: its action grants cascade). Unchecking an **action** revokes that action grant (REQ-SEC-081); unchecking the screen's **VIEW** action cascades the screen's other action grants (RULE-SEC-055). The response count lets the client name what went. |
| unchanged | B4 Access | both revokes are grant-tree edits under UPDATE (`PERM_SEC_ROLES_UPDATE`) |

B5 — API expectations, two rows added (the six existing rows are unchanged):
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| revoke screen | DELETE | /api/v1/sec/roles/{id}/screens/{screenId} | — | confirmation (cascade count) | RULE-SEC-054 | REQ-SEC-080 |
| revoke action | DELETE | /api/v1/sec/roles/{id}/actions/{actionId} | — | confirmation (cascade count) | RULE-SEC-055 | REQ-SEC-081 |

### 4. Endpoints
| Kind | Method | Path | Permission | Response (200, `ApiResponse<T>`) | Errors (HTTP · code) | Notes |
|---|---|---|---|---|---|---|
| NEW | DELETE | `/api/v1/sec/roles/{id}/screens/{screenId}` | `PERM_SEC_ROLES_UPDATE` | `ScreenGrantRevokeResponse { int revokedActionGrants }` — the action grants RULE-SEC-054 removed with the screen grant | 404 · `SEC-404-ROLE` (no role `{id}` in the caller's tenant — another tenant's role answers the same); 404 · `SEC-404-GRANT` (the role holds no grant for `{screenId}`, an unknown screen id included); 403 · `SEC-403-FORBIDDEN` (caller lacks the permission); 401 · `SEC-401-INVALID-CREDENTIALS` (no or invalid token) | `{screenId}` is the registry id `screenRegPk`, as in `POST /screens`. One transaction: the screen grant and its action grants. 200 with a body, not 204 — same reason as the module revoke (the cascade count). |
| NEW | DELETE | `/api/v1/sec/roles/{id}/actions/{actionId}` | `PERM_SEC_ROLES_UPDATE` | `ActionGrantRevokeResponse { int revokedActionGrants }` — **every** action grant this call removed, the requested one included: `1` for a non-VIEW action, `1 + N` when the action is VIEW and the role held N other actions on that screen | 404 · `SEC-404-ROLE`; 404 · `SEC-404-GRANT` (the role holds no grant for `{actionId}`, an unknown action id included); 403 · `SEC-403-FORBIDDEN`; 401 · `SEC-401-INVALID-CREDENTIALS` | `{actionId}` is the registry id `actionRegPk`, as in `POST /actions`. One transaction. The role's screen grant is kept even when VIEW goes. |
| unchanged | DELETE | `/api/v1/sec/roles/{id}/modules/{moduleId}` | `PERM_SEC_ROLES_UPDATE` | `ModuleGrantRevokeResponse` | 404 · `SEC-404-GRANT` | RULE-SEC-003 cascade exactly as before (no role pre-check added to it) |

Order of checks on both new endpoints: role (`SEC-404-ROLE`) → grant (`SEC-404-GRANT`) → cascade
decided by the Domain object → delete → audit. No new error code: `SEC-404-ROLE` and `SEC-404-GRANT`
already exist in `SecErrorCodes` and in both message bundles (`messages.properties`,
`messages_ar.properties`). Revoking from an inactive role is allowed (revoking only narrows access).

### 5. Audit
| Endpoint | SEC_AUDIT_LOG rows (`eventTypeCode`, `targetRef`) |
|---|---|
| revoke screen | one `ACTION_REVOKED` (`<roleId>/<actionRegPk>`) per cascaded action grant, then one `SCREEN_REVOKED` (`<roleId>/<screenRegPk>`) — N + 1 rows, the `revokeModule` pattern |
| revoke action | one `ACTION_REVOKED` (`<roleId>/<actionRegPk>`) for the requested grant and one per action grant RULE-SEC-055 cascaded — `revokedActionGrants` rows |

Both codes are already in the catalogue: §A6 AUDIT_EVENT_TYPE (rows `SCREEN_REVOKED`, `ACTION_REVOKED`),
`module-registry-sec.md` AUTO-DECISIONS, and the `CHK_SEC_AUDIT_LOG_EVENT_TYPE` constraint of
`V4__sec_schema.sql` (§5c); `RoleGrantService` already declares both constants for the module cascade.
Actor = the calling user (`SEC_AUDIT_LOG.ACTOR_ID`), details bilingual. Grants are not `@Audited`
entities, so nothing is written to `CORE_AUDIT_EVENT` (unchanged from 1.2.0).

### 6. Behaviour notes (verified in code)
| Kind | Note | Source |
|---|---|---|
| NEW (note) | **Sessions are not terminated.** A revoke takes effect on the role's users' next request: `JwtAuthenticationFilter.authenticate` re-reads the caller's authorities on every authenticated request through `MenuService.effectiveAuthorityCodes()` (QR-SEC-027, `RoleActionGrantRepository.findEffectiveGrantsForUser`), and the menu is read live by `MenuService` on each `GET /api/v1/sec/menu`. Nothing is cached in the token. | erp-core/src/main/java/com/erp/sec/security/JwtAuthenticationFilter.java (`authenticate`); erp-core/src/main/java/com/erp/sec/service/MenuService.java |
| NEW (note) | **A super role keeps every authority.** For a role with `IS_SUPER = TRUE` (every tenant's `SYS_ADMIN`), `MenuService.withSuperRole` adds every active catalog authority on top of the grants (`RoleRepository.holdsActiveSuperRole`), so revoking its screen or action grants removes no authority; only its navigation menu changes, because the menu is built from grants (`ScreenRegistryRepository.findEffectiveScreensForUser`). Administrators find this surprising; the frontend shows it as a hint (F4). | MenuService.java (`withSuperRole`, `effectiveAuthorityCodes`); this file's 1.2.0 addendum §2 "Super role"; docs/steps/06-report.md "Super role" |
| NEW (note) | The cascade decisions live on the Domain objects: `RoleScreenGrantDomain.cascadeOnRevoke(...)` (RULE-SEC-054) and `RoleActionGrantDomain.cascadeOnRevoke(...)` (RULE-SEC-055, gateway test `isGatewayAction`). The service loads the facts, asks the Domain object, deletes and audits. | build-create-entity "Domain Companion Object" |
| NEW (note) | **After a VIEW revoke the screen stays in the menu.** RULE-SEC-055 keeps the screen grant, and the menu is built from module and screen grants alone (`ScreenRegistryRepository.findEffectiveScreensForUser`, QR-SEC-027's menu shape), so `GET /api/v1/sec/menu` still lists the screen while every endpoint behind it answers 403 (no VIEW, RULE-SEC-007). To remove the menu entry, revoke the screen (`DELETE /roles/{id}/screens/{screenId}`), not only its VIEW. | erp-core/src/main/java/com/erp/sec/repository/ScreenRegistryRepository.java (`findEffectiveScreensForUser`); MenuService.java (`menu`) |
| unchanged | RULE-SEC-003 (module revoke cascade), RULE-SEC-001/002/007 on the grant side | — |

### 7. Decisions
| Kind | ADR | Decision |
|---|---|---|
| NEW | ADR-SEC-062 (plan name ADR-SEC-041; numbers up to 061 were issued historically, `docs/governance-vendoring-report.md` Appendix A) | Revoking VIEW **cascades** the screen's other action grants with a counted response, rather than refusing while other action grants exist — `governance/analysis/decisions/SEC/ADR-SEC-062.md` |

### 8. Frontend impact (read by the frontend repository — plan §8 F4)
| Kind | Item |
|---|---|
| NEW | Two endpoints above, for the grant tree's uncheck of a screen or an action. The screen revoke's confirm dialog can name the cascade from the grant tree (`GET /roles/{id}/grants`) before the call; the response count confirms it after. |
| NEW | Unchecking VIEW warns that the screen's other actions cascade (RULE-SEC-055), and that the screen stays in the role's menu (its endpoints answer 403) until the screen itself is unchecked (§6). To remove the menu entry, revoke the screen. |
| NEW | For a role with `isSuper = true`, the tree shows that grants only shape its menu (§6). |
| unchanged | No new screen, page code, permission or error code. |

### 9. Package D — passwords, profile fields, photo, staff `/me`
Change         : tenant-maturity plan package D — admin-set password, forced change, own password, profile fields, photo, staff `/me` (plan §6 D.1–D.3; D.4 is FILE's)
Statement      : Sections 1–8 above (package G) are unchanged; §9–§17 record package D's implemented deltas.

Ids continue from the highest number ever issued (after package G: REQ-SEC-081, AC-SEC-087, RULE-SEC-055,
DBF-SEC-116, XM-SEC-005, ADR-SEC-062). Package D adds REQ-SEC-082..089, AC-SEC-088..095,
RULE-SEC-056..062, DBF-SEC-117..123 (`P2/db-script-sec.md` 1.3.0 addendum), XM-SEC-006, ADR-SEC-063 and
ADR-SEC-064. No ENT is added: the new fields belong to ENT-SEC-001. Migration `V16__sec_user_profile.sql`
(the plan expected `V19`; the number is re-derived at creation time, plan §1.3 / §11).

#### 9.1 Requirements (§A4) — NEW

### REQ-SEC-082 — سياسة كلمة المرور / Password policy
Pattern    : ubiquitous
Statement  : The system shall accept a new STAFF password — on user create, password-reset completion, admin-set, self-change and the first administrator of a new tenant — only when it is `min-length`..`max-length` characters long (defaults 8..72) and at most 72 UTF-8 bytes (BCrypt's limit; review round 1) and, with the default policy, contains at least one letter and one digit; otherwise it shall refuse it with 400 `SEC-400-PASSWORD-POLICY`, naming the offending field.
Traces     : US-SEC-001, US-SEC-002
Entities   : ENT-SEC-001
Rationale  : RULE-SEC-056; one policy in one place (`PasswordPolicy`, `com.erp.sec.domain`), configured by `erp.core.security.password-policy.*`
Source     : docs/plans/tenant-maturity-plan.md §6 D.1
Priority   : HIGH
#### AC-SEC-088 — [REQ-SEC-082]
Given the default policy
When an administrator creates a user with password `short1` (6 characters), `abcdefgh` (no digit) or `12345678` (no letter)
Then the system answers 400 `SEC-400-PASSWORD-POLICY` with `fieldErrors[0].field = password` and creates nothing;
and with `Passw0rd!Tc1` the user is created;
and a password of 73 ASCII bytes or of 62 Arabic letters (122 bytes) is refused the same way on every path (create, reset completion, admin-set, own change, tenant first administrator, customer register and reset), while exactly 72 bytes is accepted

### REQ-SEC-083 — تعيين كلمة مرور مستخدم من المسؤول / Administrator sets a staff user's password
Pattern    : event
Statement  : When an administrator holding `PERM_SEC_USERS_UPDATE` sets the password of another STAFF user of the tenant, the system shall store the new password's hash, set `passwordChangedAt`, set `passwordChangeRequired` to the request's `requireChangeAtNextLogin` (default TRUE), terminate every open session of that user, record `PASSWORD_SET_BY_ADMIN` in the generic audit log and publish `UserPasswordChangedEvent(userId, byAdmin = true)`.
Traces     : US-SEC-002
Entities   : ENT-SEC-001, ENT-SEC-010, ENT-SEC-011
Rationale  : plan §0 D3; RULE-SEC-057, RULE-SEC-058; ADR-SEC-063
Source     : docs/plans/tenant-maturity-plan.md §6 D.2
Priority   : HIGH
#### AC-SEC-089 — [REQ-SEC-083]
Given a STAFF user U with one open session, and an administrator A of the same tenant
When A calls `PUT /api/v1/sec/users/{U}/password` with `{ newPassword }` only
Then the system answers 200 `{ userPk, passwordChangeRequired: true, passwordChangedAt, sessionsTerminated: 1 }`, U's old token answers 401, U can log in with the new password and the login answers `passwordChangeRequired = true`;
and when A targets their own user id the system answers 422 `SEC-422-PASSWORD-SELF`; a CUSTOMER or unknown id answers 404 `SEC-404-USER`

### REQ-SEC-084 — إلزام تغيير كلمة المرور / Forced password change
Pattern    : state-driven
Statement  : While a STAFF user's `passwordChangeRequired` is TRUE, the system shall answer every request of that user's token with 403 `SEC-403-PASSWORD-CHANGE-REQUIRED`, except `GET /api/v1/sec/me`, `PUT /api/v1/sec/me/password` and `POST /api/v1/sec/auth/logout` (and the public paths).
Traces     : US-SEC-002
Entities   : ENT-SEC-001
Rationale  : RULE-SEC-059; enforced server-side, the client only routes (ADR-SEC-063)
Source     : docs/plans/tenant-maturity-plan.md §6 D.2 "Forced change enforcement"
Priority   : HIGH
#### AC-SEC-090 — [REQ-SEC-084]
Given a user whose password was set by an administrator with the default `requireChangeAtNextLogin`
When that user logs in and calls `GET /api/v1/sec/menu`, `GET /api/v1/sec/me` and then `PUT /api/v1/sec/me/password`
Then the menu call answers 403 `SEC-403-PASSWORD-CHANGE-REQUIRED`, `/me` answers 200 with `passwordChangeRequired = true`, the change answers 200, and the same token then reaches the menu (200)

### REQ-SEC-085 — تغيير المستخدم كلمة مروره / A staff user changes their own password
Pattern    : event
Statement  : When an authenticated STAFF user submits their current password and a new one, the system shall verify the current password, store the new hash, set `passwordChangedAt`, clear `passwordChangeRequired`, terminate the user's other open sessions (the calling session stays valid), record `PASSWORD_CHANGED` and publish `UserPasswordChangedEvent(userId, byAdmin = false)`.
Traces     : US-SEC-001
Entities   : ENT-SEC-001, ENT-SEC-010
Rationale  : RULE-SEC-060
Source     : docs/plans/tenant-maturity-plan.md §6 D.2
Priority   : HIGH
#### AC-SEC-091 — [REQ-SEC-085]
Given a STAFF user with two open sessions S1 and S2
When the user calls `PUT /api/v1/sec/me/password` from S1 with a wrong `currentPassword`
Then the system answers 403 `SEC-403-PASSWORD-CURRENT-INVALID` and changes nothing;
and when the user repeats the call with the right current password
Then the system answers 200 `{ sessionsTerminated: 1, passwordChangeRequired: false }`, S1 keeps working and S2 answers 401

### REQ-SEC-086 — الملف الشخصي للموظف / Staff profile (`/me`)
Pattern    : event
Statement  : When an authenticated STAFF user requests their profile, the system shall return their own account fields, profile fields, photo URL, `passwordChangeRequired`, `lastLoginAt` and their tenant's code and names — and never their roles or permissions; when the user patches their profile, the system shall update only the supplied fields among `fullNameAr`, `fullNameEn`, `phone`, `jobTitleAr`, `jobTitleEn`, `preferredLocale`.
Traces     : US-SEC-001
Entities   : ENT-SEC-001
Rationale  : ADR-SEC-064 (ADR-SEC-005: the effective menu stays the only client authority); e-mail and username stay administrator-only (`PUT /users/{id}`)
Source     : docs/plans/tenant-maturity-plan.md §6 D.2
Priority   : MEDIUM
#### AC-SEC-092 — [REQ-SEC-086]
Given a STAFF user
When the user calls `GET /api/v1/sec/me`
Then the payload carries `userPk`, `username`, `email`, `fullNameAr/En`, `phone`, `jobTitleAr/En`, `preferredLocale`, `photoUrl`, `passwordChangeRequired`, `lastLoginAt`, `tenant { code, nameAr, nameEn }` and no `roles` / `permissions` key;
and `PATCH /api/v1/sec/me` with `{ "preferredLocale": "fr" }` answers 400 `VALIDATION_ERROR` (`fieldErrors[0].field = preferredLocale`), with `{ "phone": "+966 50 123 4567" }` answers 200 and changes only the phone

### REQ-SEC-087 — صورة المستخدم / Profile photo
Pattern    : event
Statement  : When a STAFF user uploads their own photo, or an administrator holding `PERM_SEC_USERS_UPDATE` uploads the photo of another STAFF user, the system shall store it through FILE's image store as a PUBLIC document with a random slug (owner `SEC_USER` / user id, module `SEC`), point `photoFileId` at it, discard the previous photo document and record `PROFILE_PHOTO_CHANGED`; removing the photo shall discard the document and clear `photoFileId`.
Traces     : US-SEC-001, US-SEC-002
Entities   : ENT-SEC-001
Rationale  : RULE-SEC-061; XM-SEC-006; ADR-FILE-008 (public, non-guessable URL)
Source     : docs/plans/tenant-maturity-plan.md §6 D.2, D.4
Priority   : MEDIUM
#### AC-SEC-093 — [REQ-SEC-087]
Given a STAFF user
When the user uploads a PNG of 2 KB to `PUT /api/v1/sec/me/photo`
Then the system answers 200 `{ photoUrl }` and `GET {photoUrl}` (no token) serves the PNG inline;
and an executable, an SVG or a PNG larger than 1 MB answers 400 `SEC-400-PHOTO-INVALID` and leaves the previous photo in place;
and a second upload answers a different `photoUrl` while the previous URL answers 404; `DELETE /api/v1/sec/me/photo` answers 204 and `/me.photoUrl` becomes null

### REQ-SEC-088 — حقول الملف الشخصي في إدارة المستخدمين / Profile fields in user management and login
Pattern    : event
Statement  : The system shall accept `phone`, `jobTitleAr`, `jobTitleEn`, `preferredLocale` on user create and update (absent = unchanged, empty string = cleared), accept `requireChangeAtNextLogin` on create (default TRUE), return those fields plus `photoUrl`, `passwordChangeRequired` and `passwordChangedAt` on every user-shaped response, and return `passwordChangeRequired` on the staff login response.
Traces     : US-SEC-002
Entities   : ENT-SEC-001
Rationale  : plan §0 D4; RULE-SEC-058, RULE-SEC-062
Source     : docs/plans/tenant-maturity-plan.md §6 D.2 (CHANGED rows)
Priority   : MEDIUM
#### AC-SEC-094 — [REQ-SEC-088]
Given an administrator
When they create a user without `requireChangeAtNextLogin` and with `preferredLocale = "ar"`, `phone = "+966501234567"`
Then the response carries those values, `passwordChangeRequired = true` and a `passwordChangedAt`; the new user's login answers `passwordChangeRequired = true`;
and `PUT /api/v1/sec/users/{id}` with `preferredLocale = "fr"` answers 400 `VALIDATION_ERROR`

### REQ-SEC-089 — إشعار تغيير كلمة المرور / Password-change event
Pattern    : event
Statement  : When a STAFF password is set by an administrator or changed by its owner, the system shall publish `UserPasswordChangedEvent(userId, byAdmin)` on the core event bus after commit; NOTIF reacts by e-mailing the user the `STAFF_PASSWORD_CHANGED` template (NOTIF 1.3.0 addendum, RULE-NOTIF-023).
Traces     : US-SEC-001
Entities   : ENT-SEC-001
Rationale  : plan §6 D.3; the event carries ids only, never a password
Source     : docs/plans/tenant-maturity-plan.md §6 D.3
Priority   : LOW
#### AC-SEC-095 — [REQ-SEC-089]
Given an administrator sets a user's password
When the transaction commits
Then exactly one `UserPasswordChangedEvent` with that user's id and `byAdmin = true` is delivered, and a NOTIF log row with template `STAFF_PASSWORD_CHANGED`, channel `EMAIL` and that user as recipient exists; a rolled-back change publishes nothing

#### 9.2 Business rules (§A5) — NEW

### RULE-SEC-056 — سياسة كلمة المرور / Password policy
Scope      : ENT-SEC-001
Trigger    : on create / on password change (create, reset completion, admin-set, self-change, tenant first administrator)
Statement  : A new password shall be `min-length`..`max-length` characters (8..72), at most 72 UTF-8 bytes whatever the characters (BCrypt hashes no more; an Arabic letter takes 2 bytes), and contain at least one letter (`require-letter`) and one digit (`require-digit`); properties `erp.core.security.password-policy.min-length|max-length|require-letter|require-digit`. A configured `max-length` above 72 fails startup (`@Max(72)`, review round 1).
Data source: the request only
Message    : ar: "كلمة المرور لا تستوفي سياسة كلمات المرور: من {0} إلى {1} حرفًا وبحد أقصى 72 بايت (الحرف العربي يشغل بايتين)، وتتضمن حرفًا ورقمًا على الأقل لحسابات الموظفين" · en: "The password does not meet the password policy: {0} to {1} characters and at most 72 bytes (a non-Latin letter takes 2 or 3), including at least one letter and one digit for staff accounts"
Traces     : REQ-SEC-082
Source     : docs/plans/tenant-maturity-plan.md §6 D.1
Decided by : `PasswordPolicy` (`com.erp.sec.domain`, built from the properties by the services), error `SEC-400-PASSWORD-POLICY` (400). Not applied to the bootstrap admin password (`erp.core.security.bootstrap-admin-password`, operator configuration) The CUSTOMER realm gets only the byte limit (`PasswordPolicy.CUSTOMER`: 8..72 characters, ≤ 72 bytes, no composition rule; register and reset completion), because a longer password cannot be hashed (review round 1). The request DTOs keep `@Size(max = 200)` as a transport bound, so an over-long password answers this rule's code, not `VALIDATION_ERROR`.

### RULE-SEC-057 — لا يعيّن المسؤول كلمة مروره بنفسه / No admin-set on oneself
Scope      : ENT-SEC-001
Trigger    : on admin-set password
Statement  : The administrator-set endpoint shall refuse the caller's own account; one's own password changes through `PUT /api/v1/sec/me/password`, which asks for the current one.
Data source: the caller's `SEC_USER` id (principal, STAFF realm) vs the path id
Message    : ar: "لا يمكنك تعيين كلمة مرورك من هنا؛ استخدم تغيير كلمة المرور الخاصة بك" · en: "You cannot set your own password here; use your own password change"
Traces     : REQ-SEC-083
Source     : docs/plans/tenant-maturity-plan.md §6 D.2
Decided by : `UserDomain.assertNotSelfForAdminPasswordSet(...)`, error `SEC-422-PASSWORD-SELF` (422, BUSINESS_RULE_VIOLATION)

### RULE-SEC-058 — كلمة مرور يحددها المسؤول تتطلب التغيير / An administrator-chosen password must be changed
Scope      : ENT-SEC-001
Trigger    : on create (POST /users) and on admin-set
Statement  : A password chosen by an administrator marks the account `passwordChangeRequired = TRUE` unless the request says `requireChangeAtNextLogin = false`; only the owner's self-change or a completed password reset clears the flag. Accounts created by tenant provisioning, sign-up approval and the bootstrap runner are not flagged.
Data source: request `requireChangeAtNextLogin` (null = TRUE)
Message    : — (state, no message)
Traces     : REQ-SEC-083, REQ-SEC-088
Source     : docs/plans/tenant-maturity-plan.md §6 D.2 ("decision row"), §9 (plan ADR-SEC-039)
Decided by : `UserDomain.passwordChangeRequiredFor(Boolean requested)`; ADR-SEC-063

### RULE-SEC-059 — بوابة تغيير كلمة المرور / Forced-change gate
Scope      : every STAFF endpoint
Trigger    : on request (after authentication)
Statement  : While the caller's `PASSWORD_CHANGE_REQUIRED_FL` is TRUE, only `GET /api/v1/sec/me`, `PUT /api/v1/sec/me/password`, `POST /api/v1/sec/auth/logout` and the public paths are served; every other request answers 403 `SEC-403-PASSWORD-CHANGE-REQUIRED` in the standard error envelope.
Data source: the `SEC_USER` row `JwtAuthenticationFilter` already loads for the token (no extra query, no token claim)
Message    : ar: "يجب تغيير كلمة المرور قبل المتابعة" · en: "You must change your password before you continue"
Traces     : REQ-SEC-084
Source     : docs/plans/tenant-maturity-plan.md §6 D.2
Decided by : `PasswordChangeRequiredFilter` (staff chain, after `RealmEnforcementFilter`), flag carried on the authentication details (`AuthRealm.passwordChangeRequired`); ADR-SEC-063

### RULE-SEC-060 — تغيير كلمة المرور يتطلب الحالية / Self-change needs the current password
Scope      : ENT-SEC-001, ENT-SEC-010
Trigger    : on self-change
Statement  : The current password must match the stored hash; then the new password replaces it, the flag clears, and every other open session of the user is terminated (the calling session stays).
Data source: ENT-SEC-001.passwordHash; ENT-SEC-010 (open sessions of the user, minus the caller's `tokenRef` = token `jti`)
Message    : ar: "كلمة المرور الحالية غير صحيحة" · en: "The current password is incorrect"
Traces     : REQ-SEC-085
Source     : docs/plans/tenant-maturity-plan.md §6 D.2
Decided by : `UserDomain.assertCurrentPasswordMatches(...)` on the service's credential check (`PasswordEncoder.matches`, as at login — PLATFORM-STD ADR-SEC-002), error `SEC-403-PASSWORD-CURRENT-INVALID` (403)

### RULE-SEC-061 — صورة المستخدم / Profile photo
Scope      : ENT-SEC-001
Trigger    : on photo upload / removal
Statement  : A profile photo is a PNG, JPEG or WebP image (detected from its bytes, SVG refused) of 1 byte to 1 MB (1 048 576 bytes); a user has at most one photo; replacing or removing it discards the previous document (its public URL answers 404 at once).
Data source: FILE's image-store validation result (RULE-FILE-008)
Message    : ar: "يجب أن تكون الصورة بصيغة PNG أو JPEG أو WebP وبحجم لا يتجاوز 1 ميغابايت" · en: "The photo must be a PNG, JPEG or WebP image of at most 1 MB"
Traces     : REQ-SEC-087
Source     : docs/plans/tenant-maturity-plan.md §6 D.4
Decided by : `UserDomain` — `PHOTO_TYPES`, `PHOTO_MAX_BYTES` (handed to FILE's image store) and `assertPhotoAccepted(...)` (a rejection → `SEC-400-PHOTO-INVALID` 400, `fieldErrors[0].field = file`); one Domain object per entity (A.0.7)

### RULE-SEC-062 — اللغة المفضلة / Preferred locale
Scope      : ENT-SEC-001
Trigger    : on create / update / patch
Statement  : `preferredLocale` is `ar`, `en` or empty (none); anything else is refused by validation (400 `VALIDATION_ERROR`) and by `CHK_SEC_USER_LOCALE`.
Data source: request
Message    : the shared `{validation.invalid}` message
Traces     : REQ-SEC-086, REQ-SEC-088
Source     : docs/plans/tenant-maturity-plan.md §6 D.1
Decided by : DTO `@Pattern` + database CHECK

#### 9.3 ENT-SEC-001 User — CHANGED (fields)
| Kind | Field | Logical type | Required | Notes | Label-ar | Label-en |
|---|---|---|---|---|---|---|
| NEW | phone | text (≤ 30) | no | E.164-ish: optional `+`, digits, spaces, hyphens, 7..30 characters (`^\+?[0-9][0-9 -]{5,28}[0-9]$`) | الهاتف | Phone |
| NEW | jobTitleAr / jobTitleEn | text (≤ 150) | no | bilingual like every label | المسمى الوظيفي (عربي/إنجليزي) | Job title (Arabic/English) |
| NEW | preferredLocale | text (≤ 5) | no | `ar` / `en` (RULE-SEC-062); the frontend applies it at login | اللغة المفضلة | Preferred language |
| NEW | photoFileId | number | no | soft reference to `FILE_DOCUMENT.ID` (XM-SEC-006), never exposed; clients get `photoUrl` | صورة المستخدم | Photo |
| NEW | passwordChangeRequired | flag | yes (default FALSE) | RULE-SEC-058/059 | يلزم تغيير كلمة المرور | Password change required |
| NEW | passwordChangedAt | date-time | no | set whenever a person sets a usable password (create, reset completion, admin-set, self-change, tenant first administrator, bootstrap admin) | تاريخ تغيير كلمة المرور | Password changed at |

#### 9.4 SCR-REQ-SEC-004 Users — CHANGED
| Kind | Item | Delta |
|---|---|---|
| CHANGED | B1 Traces | + REQ-SEC-083, REQ-SEC-087, REQ-SEC-088 |
| CHANGED | B3 Input | + `phone`, `jobTitleAr`, `jobTitleEn`, `preferredLocale` on the entry form; create also `requireChangeAtNextLogin` (default on); detail: "Set password" (second-level form) and "Photo" (upload / remove) |
| unchanged | B4 Access | page code `SEC_USERS`; set password and photo are UPDATE (`PERM_SEC_USERS_UPDATE`) |

B5 — API expectations, rows added:
| Operation | Verb | Path | Inputs | Outputs | RULEs | Traces (REQ) |
|---|---|---|---|---|---|---|
| set password | PUT | /api/v1/sec/users/{id}/password | newPassword, requireChangeAtNextLogin | confirmation (flag, sessions ended) | RULE-SEC-056/057/058 | REQ-SEC-083 |
| set photo | PUT | /api/v1/sec/users/{id}/photo | multipart `file` | photo URL | RULE-SEC-061 | REQ-SEC-087 |
| remove photo | DELETE | /api/v1/sec/users/{id}/photo | — | 204 | RULE-SEC-061 | REQ-SEC-087 |

No new SEC screen, page code or permission: the staff "my profile" and "change password" pages are
authentication-only routes of the frontend (plan §8 F1 decision row, P2_5 addendum of the frontend).

#### 9.5 Endpoints
| Kind | Method | Path | Permission | Request | Response (`ApiResponse<T>`) | Errors (HTTP · code) |
|---|---|---|---|---|---|---|
| NEW | PUT | `/api/v1/sec/users/{id}/password` | `PERM_SEC_USERS_UPDATE` | `AdminPasswordSetRequest { newPassword (required, ≤ 200), requireChangeAtNextLogin (Boolean, null = true) }` | 200 `PasswordChangeResponse { userPk, passwordChangeRequired, passwordChangedAt, sessionsTerminated }` | 404 · `SEC-404-USER` (unknown or CUSTOMER id); 422 · `SEC-422-PASSWORD-SELF`; 400 · `SEC-400-PASSWORD-POLICY`; 400 · `VALIDATION_ERROR`; 403 · `SEC-403-FORBIDDEN`; 401 · `SEC-401-INVALID-CREDENTIALS` |
| NEW | GET | `/api/v1/sec/me` | `isAuthenticated()` — STAFF chain (a customer token: 403 `REALM_MISMATCH`) | — | 200 `StaffProfileResponse { userPk, username, email, fullNameAr, fullNameEn, phone, jobTitleAr, jobTitleEn, preferredLocale, photoUrl, passwordChangeRequired, lastLoginAt, tenant { code, nameAr, nameEn } }` — no roles, no permissions | 401 |
| NEW | PATCH | `/api/v1/sec/me` | same | `StaffProfileUpdateRequest { fullNameAr, fullNameEn, phone, jobTitleAr, jobTitleEn, preferredLocale }` — null = unchanged; empty string clears `phone`, `jobTitleAr/En`, `preferredLocale`; the names cannot be blank | 200 `StaffProfileResponse` | 400 · `VALIDATION_ERROR`; 401 |
| NEW | PUT | `/api/v1/sec/me/password` | same | `PasswordChangeRequest { currentPassword, newPassword }` (both required, ≤ 200) | 200 `PasswordChangeResponse` | 403 · `SEC-403-PASSWORD-CURRENT-INVALID`; 400 · `SEC-400-PASSWORD-POLICY`; 400 · `VALIDATION_ERROR`; 401 |
| NEW | PUT | `/api/v1/sec/me/photo` | same | multipart part `file` | 200 `ProfilePhotoResponse { photoUrl }` | 400 · `SEC-400-PHOTO-INVALID`; 401 |
| NEW | DELETE | `/api/v1/sec/me/photo` | same | — | 204 (also when no photo is set) | 401 |
| NEW | PUT | `/api/v1/sec/users/{id}/photo` | `PERM_SEC_USERS_UPDATE` | multipart part `file` | 200 `ProfilePhotoResponse { photoUrl }` | 404 · `SEC-404-USER`; 400 · `SEC-400-PHOTO-INVALID`; 403 · `SEC-403-FORBIDDEN`; 401 |
| NEW | DELETE | `/api/v1/sec/users/{id}/photo` | `PERM_SEC_USERS_UPDATE` | — | 204 | 404 · `SEC-404-USER`; 403; 401 |
| CHANGED | POST | `/api/v1/sec/users` | as before | `UserCreateRequest` + `phone`, `jobTitleAr`, `jobTitleEn`, `preferredLocale` (optional), `requireChangeAtNextLogin` (Boolean, null = true) | `UserResponse` (below) | + 400 · `SEC-400-PASSWORD-POLICY` |
| CHANGED | PUT | `/api/v1/sec/users/{id}` | as before | `UserUpdateRequest` + `phone`, `jobTitleAr`, `jobTitleEn`, `preferredLocale` (null = unchanged, empty string = cleared) | `UserResponse` | as before |
| CHANGED | GET / POST search / PUT / POST | every `UserResponse` | as before | — | + `phone`, `jobTitleAr`, `jobTitleEn`, `preferredLocale`, `photoUrl` (nullable), `passwordChangeRequired`, `passwordChangedAt` | — |
| CHANGED | POST | `/api/v1/sec/auth/login` | public | — | `LoginResponse` + `passwordChangeRequired` (the customer login answers `false`, customers are never flagged) | — |
| CHANGED | POST | `/api/v1/sec/auth/password-reset/complete` | public | — | unchanged | + 400 · `SEC-400-PASSWORD-POLICY`; completing a reset clears `passwordChangeRequired` and sets `passwordChangedAt` |
| CHANGED (filter) | any | every STAFF-chain path except the three of RULE-SEC-059 and the public paths | — | — | — | + 403 · `SEC-403-PASSWORD-CHANGE-REQUIRED` while the caller's flag is set |

New error codes (`SecErrorCodes`, both bundles): `SEC-400-PASSWORD-POLICY` (400, args min, max),
`SEC-422-PASSWORD-SELF` (422), `SEC-403-PASSWORD-CURRENT-INVALID` (403),
`SEC-403-PASSWORD-CHANGE-REQUIRED` (403), `SEC-400-PHOTO-INVALID` (400). Policy and photo errors also
carry `fieldErrors[0]` naming the field (`password`, `newPassword`, `adminPassword`, `file`).

Order of checks — admin-set: user (`SEC-404-USER`) → self (`SEC-422-PASSWORD-SELF`) → policy → write.
Self-change: current password (`SEC-403-PASSWORD-CURRENT-INVALID`) → policy → write. Photo: user (404)
→ image validation (`SEC-400-PHOTO-INVALID`) → store new → discard previous → write.

#### 9.6 Sessions, audit, events
| Operation | Sessions | `SEC_AUDIT_LOG` | `CORE_AUDIT_EVENT` (AuditApi action) | Event |
|---|---|---|---|---|
| admin-set password | every open session of the target terminated | one `SESSION_TERMINATED` per session (existing event type) | `PASSWORD_SET_BY_ADMIN` (actor = administrator, entity `SEC_USER` / target id) | `UserPasswordChangedEvent(userId, byAdmin = true)` |
| self-change | the user's other open sessions terminated (caller's own kept) | one `SESSION_TERMINATED` per session | `PASSWORD_CHANGED` (actor = the user) | `UserPasswordChangedEvent(userId, byAdmin = false)` |
| photo set / removed (own or another's) | — | — | `PROFILE_PHOTO_CHANGED` (actor = caller, entity `SEC_USER` / target id, summary "set" / "removed") | — |
| PATCH `/me`, PUT `/users/{id}` | — | — | `UPDATE` rows of the `@Audited` User entity (unchanged mechanism) | — |
No secret is audited: the summaries name no password, and the global denylist drops every field whose
name contains `password` (`passwordChangeRequired`, `passwordChangedAt` included) from `CHANGES`.
`UserPasswordChangedEvent` (`com.erp.events`, 11th core event) carries `userId` and `byAdmin` only.

#### 9.7 Cross-module (XM)
| Kind | Id | From → to | What | Kind of link |
|---|---|---|---|---|
| NEW | XM-SEC-006 | SEC → FILE | `SEC_USER.PHOTO_FILE_ID` → `FILE_DOCUMENT.ID`, written through `FileImageStoreApi` (XM-FILE-002: `storePublicImage`, `discard`), URLs read through `FileDocumentLookupApi.publicUrl` / `publicUrls` (XM-FILE-001, `publicUrls` NEW) | SOFT reference, no FK (the `NOTIF_TEMPLATE.ATTACHMENT_FILE_ID` convention, XM-NOTIF-002) |
| CHANGED (consumed) | — | SEC → TENANT | `TenantLookupApi.summaryOf(tenantId)` (XM-TENANT-001, CHANGED in the TENANT 1.3.0 addendum) for `/me.tenant` | in-core API |
| NEW (consumed by) | — | NOTIF ← events | `UserPasswordChangedEvent` → `STAFF_PASSWORD_CHANGED` e-mail (NOTIF RULE-NOTIF-023, XM-NOTIF-003) | event bus |
| CHANGED (consumed by) | — | TENANT → SEC | tenant provisioning's first administrator password now passes RULE-SEC-056 inside `SecTenantProvisioningContributor` (400 `SEC-400-PASSWORD-POLICY`, field `adminPassword`) | provisioning SPI |

#### 9.8 Decisions
| Kind | ADR | Decision |
|---|---|---|
| NEW | ADR-SEC-063 (plan name ADR-SEC-039) | An administrator-chosen password forces a change at next login, default TRUE, enforced server-side by a filter that reads the flag from the user row already loaded per request — `governance/analysis/decisions/SEC/ADR-SEC-063.md` |
| NEW | ADR-SEC-064 (plan name ADR-SEC-040) | The staff `/me` payload carries no roles or permissions — `governance/analysis/decisions/SEC/ADR-SEC-064.md` |

#### 9.9 Behaviour notes and deliberate differences from the plan
| Kind | Note |
|---|---|
| CHANGED (plan) | Plan: admin-set answers `UserStatusResponse`. Implemented: `PasswordChangeResponse { userPk, passwordChangeRequired, passwordChangedAt, sessionsTerminated }`, shared with the self-change (whose result the plan left open): a password change does not change `statusCode`, and the session count is what the administrator needs to know (the same fact B's tenant admin-reset returns). |
| NEW (decision) | On `PUT /users/{id}` the four new optional fields follow "absent = unchanged, empty string = cleared", like the PATCH: a client built before 1.3.0 that does not send them never wipes values the user set on `/me`. |
| NEW (scope) | The policy also guards the tenant's first administrator (`POST /api/v1/platform/tenants`, field `adminPassword`), which is a STAFF user create; the CUSTOMER realm gets only the 72-byte limit (review round 1), not the composition rule (out of the plan's list). |
| NEW (scope) | The bootstrap admin password (`erp.core.security.bootstrap-admin-password`) is not policy-checked and not flagged (operator configuration at startup); `Test1234` meets the default policy anyway. |
| NEW (open) | The policy has no history / reuse rule: a forced change may set the same password again. Recorded for a later version. |
| NEW (open, review round 1) | The current-password check of `PUT /me/password` is not throttled (a stolen token could guess the current password), and admin-set has no super-role guard (an administrator holding `PERM_SEC_USERS_UPDATE` may set a super-role user's password). Recorded for a later version; not implemented. |
| NEW (open, review round 1) | A bootstrap admin password above 72 bytes fails startup inside BCrypt (operator configuration; not policy-checked). |
| NEW (note) | The session-termination loop of admin-set mirrors `UserService.deactivate` and `PasswordResetService.complete` (one `SESSION_TERMINATED` row per session). |

#### 9.10 Frontend impact (read by the frontend repository — plan §8 F1)
| Kind | Item |
|---|---|
| NEW | `passwordChangeRequired` on the login response and on `/me`: route to the change-password page; every other STAFF call answers 403 `SEC-403-PASSWORD-CHANGE-REQUIRED` until `PUT /me/password` succeeds (the same token keeps working afterwards). |
| NEW | Endpoints of §9.5 for the users screen (set password, photo) and the authentication-only "my profile" route; the profile carries no roles (ADR-SEC-064) — the menu stays the authority. |
| NEW | Error codes `SEC-400-PASSWORD-POLICY`, `SEC-422-PASSWORD-SELF`, `SEC-403-PASSWORD-CURRENT-INVALID`, `SEC-403-PASSWORD-CHANGE-REQUIRED`, `SEC-400-PHOTO-INVALID` (messages in both languages). |
| NEW | `photoUrl` is a public URL (no token); a replaced photo gets a new URL, so it can be cached freely. |
| unchanged | No new page code, permission or menu entry. |

### 10. Package B — what the tenant level 1 needs from SEC (usage counts, platform recovery of a tenant administrator)
Change         : tenant-maturity plan package B — `SecUserDirectoryApi` counts and the NEW `SecAdminRecoveryApi` (plan §4 B.4), consumed by TENANT's `GET /api/v1/platform/tenants/{id}/usage` and `POST /api/v1/platform/tenants/{id}/admin-reset`
Statement      : Sections 1–9 above (packages G and D) are unchanged; §10 records package B's implemented deltas.

Ids continue from the highest number ever issued (after package D: REQ-SEC-089, AC-SEC-095, RULE-SEC-062,
XM-SEC-006, ADR-SEC-064; 065 held, 066 … 068 the analysis-coverage work's). Package B adds REQ-SEC-090/091
and AC-SEC-096/097. No endpoint, entity field, table, permission, error code or migration of SEC changes; no
XM-SEC id (in SEC's analysis an XM id is minted by the consuming module; TENANT records the consumption in
its own 1.3.0 addendum, B7). The rule that decides the recovery target (RULE-TENANT-017) is TENANT's; SEC
computes the facts.

#### 10.1 Requirements (§A4) — NEW

### REQ-SEC-090 — أعداد دليل المستخدمين للمستأجر الحالي / Directory counts of the current tenant
Pattern    : ubiquitous
Statement  : The system shall expose, through `com.erp.sec.crossmodule.SecUserDirectoryApi`, the current tenant's number of STAFF users (`int countStaff()`), CUSTOMER users (`int countCustomers()`) — both in any account status — and open sessions (`int countActiveSessions()`: `SEC_ACTIVE_SESSION` rows with `TERMINATED_AT` NULL, either realm); counts only, never a row.
Traces     : US-SEC-002 (supporting TENANT US-TENANT-011)
Entities   : ENT-SEC-001, ENT-SEC-010
Rationale  : plan §4 B.4; the consumer counts another module's data without reading its tables (build-create-service "Cross-Module Calls")
Source     : docs/plans/tenant-maturity-plan.md §4 B.4
Priority   : MEDIUM
Note       : read-only, `isAuthenticated()` like the sibling directory reads (REQ-SEC-034/035); the consuming service carries its own gate (TENANT: `PLATFORM_TENANT_MANAGE`). The tenant is the current one (`TenantContext`): TENANT calls it inside `callAs(id)`.
#### AC-SEC-096 — [REQ-SEC-090]
Given tenant D with its first administrator and nothing else
When the counts are read inside D
Then `countStaff() = 1`, `countCustomers() = 0`, `countActiveSessions() = 0`; after the administrator signs in and creates a user, `countStaff() = 2` and `countActiveSessions() = 1`; no row of another tenant is ever counted (`TenantUsageIntegrationTest`)

### REQ-SEC-091 — استعادة كلمة مرور مستخدم فائق من المنصة / Platform recovery of a super user's password
Pattern    : event
Statement  : The system shall expose `com.erp.sec.crossmodule.SecAdminRecoveryApi` for the platform's recovery of a tenant administrator, running in the current tenant and gated by the authority `PLATFORM_TENANT_MANAGE`: `findRecoveryTarget(username)` answers, for a STAFF user of that name, `RecoveryTarget(userId, username, superRole)` where `superRole` = the user holds an ACTIVE role with `IS_SUPER = TRUE` (empty for an unknown name or a CUSTOMER account); `resetSuperUserPassword(username, rawPassword, requireChangeAtNextLogin)` applies the STAFF password policy (RULE-SEC-056, field `newPassword`), stores the new hash and `passwordChangedAt`, sets `passwordChangeRequired` per RULE-SEC-058 (`requireChangeAtNextLogin` null = TRUE), terminates every open session of the user, records `ADMIN_PASSWORD_RESET` in the generic audit log and publishes `UserPasswordChangedEvent(userId, byAdmin = true)`, and answers the number of terminated sessions.
Traces     : US-SEC-002 (supporting TENANT US-TENANT-010)
Entities   : ENT-SEC-001, ENT-SEC-002, ENT-SEC-003, ENT-SEC-010, ENT-SEC-011
Rationale  : plan §4 B.4 ("built from D's pieces": `PasswordPolicyProvider`, `User.changePassword`, `UserSessionTerminator`); ADR-SEC-063 (an administrator-chosen password forces a change by default — the caller passes the request's flag, SEC applies the default)
Source     : docs/plans/tenant-maturity-plan.md §4 B.2, B.4
Priority   : HIGH
Note       : the operator is not a user of the target tenant: the audit row has `actorUserId` null (actor = the operator's username), and the `SESSION_TERMINATED` rows of `SEC_AUDIT_LOG` carry no actor user. A name that is not a STAFF user holding an active super role answers 404 `SEC-404-USER` from `resetSuperUserPassword` (unreachable through TENANT, which refuses first with its own codes in the same transaction).
#### AC-SEC-097 — [REQ-SEC-091]
Given tenant D whose administrator `td-admin` holds `SYS_ADMIN` (`IS_SUPER`) and has one open session, and a STAFF user without a role
When `findRecoveryTarget` is asked inside D for `td-admin`, for the role-less user and for an unknown name
Then it answers `superRole = true`, `superRole = false` and empty;
when `resetSuperUserPassword("td-admin", "abcdefgh", null)` is called, it refuses with 400 `SEC-400-PASSWORD-POLICY` and changes nothing; with a valid password it answers 1, the old session is terminated, the new password signs in with `passwordChangeRequired = true`, one `ADMIN_PASSWORD_RESET` row (actor = the operator, entity `SEC_USER` / the user's id, no secret) and one `STAFF_PASSWORD_CHANGED` e-mail are recorded in D (`TenantAdminResetIntegrationTest`)

#### 10.2 Cross-module surface (exposed) — NEW / CHANGED
| Kind | Interface (`com.erp.sec.crossmodule`) | Method | Gate | Implemented by |
|---|---|---|---|---|
| CHANGED | `SecUserDirectoryApi` | + `int countStaff()`, `int countCustomers()`, `int countActiveSessions()` | `isAuthenticated()` (`UserService`) | `SecUserDirectoryApiImpl` → `UserService` |
| NEW | `SecAdminRecoveryApi` | `Optional<RecoveryTarget> findRecoveryTarget(String username)` (read-only) | authority `PLATFORM_TENANT_MANAGE` through `SecPermissions.PLATFORM_TENANT_MANAGE` (a non-catalog constant like `ROLE_CUSTOMER`, mirrored because SEC does not depend on the tenant module's `TenantPermissions`; ArchUnit rule 5 forbids a literal) | `SecAdminRecoveryApiImpl` → `UserPasswordService` |
| NEW | `SecAdminRecoveryApi` | `int resetSuperUserPassword(String username, String rawPassword, Boolean requireChangeAtNextLogin)` (null = TRUE, RULE-SEC-058) (write, joins the caller's transaction) | same | same |
| NEW | `RecoveryTarget` (record) | `Long userId`, `String username`, `boolean superRole` — a read-model, never the entity | — | — |
Consumer: TENANT (`TenantService`), always inside `TenantContext.callAs(tenantId)`; the two recovery calls
run in one transaction opened there, so the check and the write are atomic.

#### 10.3 Sessions, audit, events (admin-reset)
| Operation | Sessions | `SEC_AUDIT_LOG` | `CORE_AUDIT_EVENT` (AuditApi action) | Event |
|---|---|---|---|---|
| `resetSuperUserPassword` | every open session of the user terminated (`UserSessionTerminator`, keep none) | one `SESSION_TERMINATED` per session, actor user none | `ADMIN_PASSWORD_RESET` (actor = the platform operator's username, `actorUserId` null, entity `SEC_USER` / user id, tenant = the current = the target tenant) | `UserPasswordChangedEvent(userId, byAdmin = true)` → NOTIF RULE-NOTIF-023 |
No secret is logged, returned, audited or carried by the event (POL-SEC-004).

#### 10.4 Behaviour notes
| Kind | Note |
|---|---|
| CHANGED (code) | `UserSessionTerminator.terminateOpenSessions` gains an overload taking the acting `User` explicitly (null = none): inside another tenant the operator's username could match a different user of that tenant, so the recovery passes none; the existing method keeps resolving the actor by username. |
| NEW (scope) | `ADMIN_PASSWORD_RESET` is a new generic-audit action code (`^[A-Z_]{3,64}$`); the audit module's code and analysis do not change. |
