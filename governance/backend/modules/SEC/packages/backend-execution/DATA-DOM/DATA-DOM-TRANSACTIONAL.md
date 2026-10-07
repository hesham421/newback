<!-- source: PHASE:DATA-DOM / SUB:DATA-DOM-TRANSACTIONAL -->
<!-- context: DATA-DOM-HEADER.md — phase-level preamble -->
<!-- traces: REQ-SEC-001, REQ-SEC-010, REQ-SEC-012, REQ-SEC-013, REQ-SEC-014, REQ-SEC-015, REQ-SEC-024 -->
<!-- SUB:DATA-DOM-TRANSACTIONAL:START traces=REQ-SEC-001,REQ-SEC-010,REQ-SEC-012,REQ-SEC-013,REQ-SEC-014,REQ-SEC-015,REQ-SEC-024 -->
### SUB — DATA-DOM-TRANSACTIONAL

#### ENT-SEC-003 — UserRoleAssignment      kind: security
BINDINGS: table `SEC_USER_ROLE` · PK `userRolePk` (DBF-SEC-025) · PK generation `GENERATED ALWAYS AS IDENTITY`
FIELDS:
| DBF | property | column | type | null | read-only | constraint |
|---|---|---|---|---|---|---|
| DBF-SEC-025 | userRolePk | user_role_pk | Long | NOT NULL | Yes | PK_SEC_USER_ROLE |
| DBF-SEC-026 | userId | user_id | Long | NOT NULL | No | FK_USER_ROLE_USER, UQ_SEC_USER_ROLE_USER_ROLE |
| DBF-SEC-027 | roleId | role_id | Long | NOT NULL | No | FK_USER_ROLE_ROLE, UQ_SEC_USER_ROLE_USER_ROLE |
| DBF-SEC-028 | assignedBy | assigned_by | String | NOT NULL | Yes | — |
| DBF-SEC-029 | assignedAt | assigned_at | Instant | NOT NULL | Yes | — |
DTO MEMBERSHIP: request = `{roleIds: [Long]}` (batch); response = list of `{roleId, code, nameAr, nameEn}`.
DOMAIN RULES: **RULE-SEC-005** applies here too (assigning a role that carries an action already held via another role, for a conflicting pair, is rejected — same QR-SEC-031 check as API-SEC-017, cited fully under ENT-SEC-009 below to avoid duplication).
CROSS-MODULE: none.
REPOSITORY OPS → QR-SEC-008 (SAVE, batch), QR-SEC-031 (EXISTS, conflict check, shared with API-SEC-017).

#### ENT-SEC-007 — RoleModuleGrant      kind: security
BINDINGS: table `SEC_ROLE_MODULE_GRANT` · PK `roleModuleGrantPk` (DBF-SEC-060) · PK generation `GENERATED ALWAYS AS IDENTITY`
FIELDS:
| DBF | property | column | type | null | read-only | constraint |
|---|---|---|---|---|---|---|
| DBF-SEC-060 | roleModuleGrantPk | role_module_grant_pk | Long | NOT NULL | Yes | PK_SEC_ROLE_MODULE_GRANT |
| DBF-SEC-061 | roleId | role_id | Long | NOT NULL | No | FK_ROLE_MODULE_GRANT_ROLE, UQ_SEC_ROLE_MODULE_GRANT_ROLE_MODULE |
| DBF-SEC-062 | moduleId | module_id | Long | NOT NULL | No | FK_ROLE_MODULE_GRANT_MODULE, UQ_SEC_ROLE_MODULE_GRANT_ROLE_MODULE |
| DBF-SEC-063 | grantedBy | granted_by | String | NOT NULL | Yes | — |
| DBF-SEC-064 | grantedAt | granted_at | Instant | NOT NULL | Yes | — |
DOMAIN RULES: **RULE-SEC-003** — Scope ENT-SEC-007 · Trigger: on delete (module grant) · Statement: "The system shall delete every screen grant and action grant that module covered for that role when its module grant is revoked." · Message ar: "سيتم سحب كل منح الشاشات والإجراءات ضمن هذه الوحدة لهذا الدور" / en: "Every screen and action grant under this module for this role will be revoked" · DB enforcement: application layer (service, transactional) · owner layer: service.
CROSS-MODULE: none.
REPOSITORY OPS → QR-SEC-014 (SAVE), QR-SEC-015 (DELETE), QR-SEC-032 (FIND_ALL, cascade targets).

#### ENT-SEC-008 — RoleScreenGrant      kind: security
BINDINGS: table `SEC_ROLE_SCREEN_GRANT` · PK `roleScreenGrantPk` (DBF-SEC-065) · PK generation `GENERATED ALWAYS AS IDENTITY`
FIELDS:
| DBF | property | column | type | null | read-only | constraint |
|---|---|---|---|---|---|---|
| DBF-SEC-065 | roleScreenGrantPk | role_screen_grant_pk | Long | NOT NULL | Yes | PK_SEC_ROLE_SCREEN_GRANT |
| DBF-SEC-066 | roleId | role_id | Long | NOT NULL | No | FK_ROLE_SCREEN_GRANT_ROLE, UQ_SEC_ROLE_SCREEN_GRANT_ROLE_SCREEN |
| DBF-SEC-067 | screenId | screen_id | Long | NOT NULL | No | FK_ROLE_SCREEN_GRANT_SCREEN, UQ_SEC_ROLE_SCREEN_GRANT_ROLE_SCREEN |
| DBF-SEC-068 | grantedBy | granted_by | String | NOT NULL | Yes | — |
| DBF-SEC-069 | grantedAt | granted_at | Instant | NOT NULL | Yes | — |
DOMAIN RULES: **RULE-SEC-001** — Scope ENT-SEC-008 · Trigger: on create (screen grant) · Statement: "The system shall prevent a screen grant for a role that does not hold the screen's module grant." · Message ar: "لا يمكن منح شاشة دون منح الوحدة أولًا" / en: "Cannot grant a screen without first granting its module" · DB enforcement: application layer (service, via QR-SEC-028 pre-check) · owner layer: service.
CROSS-MODULE: none.
REPOSITORY OPS → QR-SEC-016 (SAVE), QR-SEC-028 (EXISTS, module-grant pre-check).

#### ENT-SEC-009 — RoleActionGrant      kind: security
BINDINGS: table `SEC_ROLE_ACTION_GRANT` · PK `roleActionGrantPk` (DBF-SEC-070) · PK generation `GENERATED ALWAYS AS IDENTITY`
FIELDS:
| DBF | property | column | type | null | read-only | constraint |
|---|---|---|---|---|---|---|
| DBF-SEC-070 | roleActionGrantPk | role_action_grant_pk | Long | NOT NULL | Yes | PK_SEC_ROLE_ACTION_GRANT |
| DBF-SEC-071 | roleId | role_id | Long | NOT NULL | No | FK_ROLE_ACTION_GRANT_ROLE, UQ_SEC_ROLE_ACTION_GRANT_ROLE_ACTION |
| DBF-SEC-072 | actionId | action_id | Long | NOT NULL | No | FK_ROLE_ACTION_GRANT_ACTION, UQ_SEC_ROLE_ACTION_GRANT_ROLE_ACTION |
| DBF-SEC-073 | grantedBy | granted_by | String | NOT NULL | Yes | — |
| DBF-SEC-074 | grantedAt | granted_at | Instant | NOT NULL | Yes | — |
DOMAIN RULES:
**RULE-SEC-002** — Scope ENT-SEC-009 · Trigger: on create (action grant) · Statement: "The system shall prevent an action grant for a role that does not hold the action's screen grant." · Message ar: "لا يمكن منح إجراء دون منح الشاشة أولًا" / en: "Cannot grant an action without first granting its screen" · DB enforcement: application layer (service, via QR-SEC-029) · owner layer: service.
**RULE-SEC-005** — Scope ENT-SEC-003, ENT-SEC-009 · Trigger: on create (role assignment or action grant) · Statement: "The system shall prevent assigning a user, by any combination of roles, both actions of a module-declared conflicting pair." · Message ar: "هذا المستخدم يملك إجراءً متعارضًا بالفعل" / en: "This user already holds a conflicting action" · DB enforcement: application layer (service, via QR-SEC-031, checked across all of a user's roles) · owner layer: service. · **DEFERRED (SEC v1)**: nothing in SEC v1 declares a conflicting pair — there is no ENT, DBF, table, API field or screen element for such a declaration — so REQ-SEC-020's `optional` precondition is never established and this guard, though implemented, is inert (the conflicting-counterpart set is empty in v1). The platform's only real conflicting pair is FIN-owned and FIN-enforced: governance/modules/FIN/P3_1/backend-execution-plan-fin.md:1061-1066 (RULE-FIN-015, error `FIN-403-SOD-VIOLATION`). Whether SEC v1 should own a conflicting-pair register at all is an open P1/P2 decision, not an execution one.
**RULE-SEC-007** — Scope ENT-SEC-009 · Trigger: on evaluate (any action check) and on create (action grant, informational) · Statement: "The system shall require a role to hold the VIEW action grant on a screen before any other action grant on that screen takes effect for it." · Message ar: "يلزم منح إجراء العرض (VIEW) أولًا على هذه الشاشة" / en: "The VIEW action must be granted on this screen first" · DB enforcement: application layer (service, via QR-SEC-030 at grant time + the CORE interceptor at request time) · owner layer: service.
CROSS-MODULE: none.
REPOSITORY OPS → QR-SEC-017 (SAVE), QR-SEC-029 (EXISTS), QR-SEC-030 (EXISTS), QR-SEC-031 (EXISTS).

#### ENT-SEC-010 — ActiveSession      kind: security
BINDINGS: table `SEC_ACTIVE_SESSION` · PK `activeSessionPk` (DBF-SEC-075) · PK generation `GENERATED ALWAYS AS IDENTITY`
FIELDS: DBF-SEC-075..082 — see DB Alignment Manifest; all `read-only: Yes` (system lifecycle, no client-editable field).
DTO MEMBERSHIP: no create/update request (system-created at login, API-SEC-001); response includes all except `tokenRef` (never serialized back).
DOMAIN RULES: none scoped alone.
STATE MACHINE: `terminatedAt IS NULL` = active, `terminatedAt IS NOT NULL` = terminated — binary, not applicable for a diagram.
CROSS-MODULE: none.
REPOSITORY OPS → QR-SEC-025 (FIND_BY_CRITERIA, non-terminated), QR-SEC-026 (UPDATE, terminate) — also written to by API-SEC-001 (create) and API-SEC-009 (bulk-terminate on deactivate), both via the same repository, no separate QR (plain SAVE / batch UPDATE, standard defaults apply).

#### ENT-SEC-011 — AuditLogEntry      kind: security
BINDINGS: table `SEC_AUDIT_LOG` · PK `auditLogPk` (DBF-SEC-083) · PK generation `GENERATED ALWAYS AS IDENTITY`
FIELDS: DBF-SEC-083..090 — see DB Alignment Manifest; all `read-only: Yes` (append-only, no update endpoint exists at all — immutability, POL-SEC-009).
DTO MEMBERSHIP: no create/update request exposed to any client — rows are written internally by every other API's orchestration step, never by a dedicated "create audit entry" endpoint; response (search/export) includes all fields.
DOMAIN RULES: none (the immutability itself is enforced by omission — no UPDATE/DELETE mapping exists on this repository at all, not by a DB trigger).
CROSS-MODULE: none.
REPOSITORY OPS → QR-SEC-023 (FIND_BY_CRITERIA, search), QR-SEC-024 (FIND_BY_CRITERIA, export) — writes happen inline from every other service method (SAVE, standard defaults, no dedicated QR per writer to avoid 20+ near-identical entries; the write shape is always `{eventTypeCode, actorUserId, occurredAt=now(), targetRef, detailsAr, detailsEn, ipAddress}`).

#### ENT-SEC-012 — PasswordResetToken      kind: security
BINDINGS: table `SEC_PWD_RESET_TOKEN` · PK `pwdResetTokenPk` (DBF-SEC-091) · PK generation `GENERATED ALWAYS AS IDENTITY`
FIELDS: DBF-SEC-091..096 — see DB Alignment Manifest; all `read-only: Yes` (system-managed lifecycle).
DTO MEMBERSHIP: no direct create/read/update DTO — entirely internal to API-SEC-003/004's orchestration; the client only ever sees the opaque token string delivered out-of-band (§API-SEC-003 Response).
DOMAIN RULES: **RULE-SEC-006** — Scope ENT-SEC-012 · Trigger: on submit (password reset completion) · Statement: "The system shall reject a password-reset submission whose token is expired or already used." · Message ar: "رابط إعادة التعيين غير صالح أو منتهي" / en: "This reset link is invalid or has expired" · DB enforcement: application layer (service, via QR-SEC-038) · owner layer: service. `expiresAt` DEFAULT = `requestedAt + 30 minutes` (SRS A7 DEFAULT, non-breaking).
CROSS-MODULE: none.
REPOSITORY OPS → QR-SEC-003 (SAVE), QR-SEC-004 (UPDATE, on completion), QR-SEC-038 (EXISTS, validity check).

#### ENT-SEC-013 — SignupRequest      kind: security
BINDINGS: table `SEC_SIGNUP_REQUEST` · PK `signupRequestPk` (DBF-SEC-097) · PK generation `GENERATED ALWAYS AS IDENTITY`
FIELDS: DBF-SEC-097..104 — see DB Alignment Manifest.
DTO MEMBERSHIP: create-request = `{email, fullNameAr, fullNameEn}`; no update endpoint (only approve/reject via API-SEC-011); response includes all.
LOOKUP FIELDS: `statusCode` → key `SIGNUP_STATUS` (ADR-SEC-001, same treatment as USER_STATUS).
DOMAIN RULES: none scoped alone (RULE-SEC-004/007 do not apply here).
STATE MACHINE: `statusCode` (SIGNUP_STATUS) — values PENDING/APPROVED/REJECTED; initial PENDING; transitions PENDING→APPROVED (API-SEC-011 approve, actor: administrator), PENDING→REJECTED (API-SEC-011 reject, actor: administrator); both terminal; no invalid-transition RULE beyond "only PENDING may transition."
CROSS-MODULE: none.
REPOSITORY OPS → QR-SEC-002 (SAVE), QR-SEC-011 (UPDATE, decision).
<!-- SUB:DATA-DOM-TRANSACTIONAL:END -->
