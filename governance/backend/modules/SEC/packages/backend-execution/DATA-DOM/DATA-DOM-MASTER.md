<!-- source: PHASE:DATA-DOM / SUB:DATA-DOM-MASTER -->
<!-- context: DATA-DOM-HEADER.md — phase-level preamble -->
<!-- traces: REQ-SEC-009, REQ-SEC-012, REQ-SEC-016, REQ-SEC-017, REQ-SEC-019 -->
<!-- SUB:DATA-DOM-MASTER:START traces=REQ-SEC-009,REQ-SEC-012,REQ-SEC-016,REQ-SEC-017,REQ-SEC-019 -->
### SUB — DATA-DOM-MASTER

#### ENT-SEC-001 — User      kind: security
BINDINGS: table `SEC_USER` · PK `userPk` (DBF-SEC-001) · PK generation `GENERATED ALWAYS AS IDENTITY` · db-script v1
BUSINESS CODE: none (§3.3 test: no)
DEFAULT FIELDS: `security` kind carries no profile-fixed default set (SRS A3 note); every field below is explicit
FIELDS:
| DBF | property | column | type | null | read-only | constraint | label-ar / label-en |
|---|---|---|---|---|---|---|---|
| DBF-SEC-001 | userPk | user_pk | Long | NOT NULL | Yes | PK_SEC_USER | معرّف المستخدم / User id |
| DBF-SEC-002 | username | username | String | NOT NULL | No | UQ_SEC_USER_USERNAME | اسم المستخدم / Username |
| DBF-SEC-003 | email | email | String | NOT NULL | No | UQ_SEC_USER_EMAIL | البريد الإلكتروني / Email |
| DBF-SEC-004 | passwordHash | password_hash | String | NOT NULL | Yes | — | تجزئة كلمة المرور / Password hash |
| DBF-SEC-005 | fullNameAr | full_name_ar | String | NOT NULL | No | — | الاسم الكامل (عربي) / Full name (Arabic) |
| DBF-SEC-006 | fullNameEn | full_name_en | String | NOT NULL | No | — | الاسم الكامل (إنجليزي) / Full name (English) |
| DBF-SEC-007 | statusCode | status_code | String | NOT NULL | Yes | CHK_SEC_USER_STATUS | الحالة / Status |
| DBF-SEC-008 | lastLoginAt | last_login_at | Instant | NULL | Yes | — | آخر دخول / Last login |
| DBF-SEC-009 | isActiveFl | is_active_fl | Boolean | NOT NULL | Yes | — | نشط / Active |
| DBF-SEC-010 | createdBy | created_by | String | NOT NULL | Yes | — | أُنشئ بواسطة / Created by |
| DBF-SEC-011 | createdAt | created_at | Instant | NOT NULL | Yes | — | تاريخ الإنشاء / Created at |
| DBF-SEC-012 | updatedBy | updated_by | String | NULL | Yes | — | حُدّث بواسطة / Updated by |
| DBF-SEC-013 | updatedAt | updated_at | Instant | NULL | Yes | — | تاريخ التحديث / Updated at |
DTO MEMBERSHIP: create-request excludes {userPk, passwordHash(raw password field instead, hashed server-side), statusCode, lastLoginAt, isActiveFl, audit}; update-request excludes {userPk, username, passwordHash, statusCode, isActiveFl, audit} (username immutable after create; password changes only via API-SEC-004); response includes all except passwordHash (never serialized).
LOOKUP FIELDS: `statusCode` → key `USER_STATUS` → GET /api/v1/mdl/lookups?type=USER_STATUS (API-MDL-011 — MDL's consumer lookup API; v1 validates server-side against the closed set per ADR-SEC-001) — stores the code, never a numeric FK.
DOMAIN RULES: none scoped to User alone (RULE-SEC-005 scopes ENT-SEC-003/009, cited there).
STATE MACHINE: `statusCode` (USER_STATUS) — values PENDING/ACTIVE/DISABLED; initial ACTIVE (direct create, API-SEC-006) or PENDING (via sign-up approval, API-SEC-011); transitions PENDING→ACTIVE (API-SEC-011, actor: administrator), ACTIVE→DISABLED (API-SEC-009, actor: administrator), DISABLED→ACTIVE (API-SEC-010, actor: administrator); no terminal state; no invalid-transition RULE beyond "the four listed transitions are the only ones exposed" (enforced by which endpoint exists, not a DB CHECK on the transition itself).
CROSS-MODULE: none CONSUMED (SEC is ROOT). EXPOSED since 2026-09-11: the contact projection
(email + both display names + active, DBF-SEC-003/005/006/007) and this entity's pk as the
permission-holder result, both read-only via `com.erp.sec.crossmodule.SecUserDirectoryApi`
(REQ-SEC-034, REQ-SEC-035) — no table, column or FK registered, so still no XM row (§PHASE 6).
REPOSITORY OPS → QR-SEC-001 (FIND_ONE by username), QR-SEC-005 (FIND_BY_CRITERIA), QR-SEC-006 (SAVE), QR-SEC-007 (UPDATE), QR-SEC-009 (UPDATE, deactivate), QR-SEC-010 (UPDATE, reactivate), QR-SEC-033 (EXISTS, uniqueness).

#### ENT-SEC-002 — Role      kind: security
BINDINGS: table `SEC_ROLE` · PK `rolePk` (DBF-SEC-014) · PK generation `GENERATED ALWAYS AS IDENTITY`
BUSINESS CODE: none
FIELDS:
| DBF | property | column | type | null | read-only | constraint | label-ar / label-en |
|---|---|---|---|---|---|---|---|
| DBF-SEC-014 | rolePk | role_pk | Long | NOT NULL | Yes | PK_SEC_ROLE | معرّف الدور / Role id |
| DBF-SEC-015 | code | code | String | NOT NULL | No | UQ_SEC_ROLE_CODE | رمز الدور / Role code |
| DBF-SEC-016 | nameAr | name_ar | String | NOT NULL | No | — | اسم الدور (عربي) / Role name (Arabic) |
| DBF-SEC-017 | nameEn | name_en | String | NOT NULL | No | — | اسم الدور (إنجليزي) / Role name (English) |
| DBF-SEC-018 | descriptionAr | description_ar | String | NULL | No | — | الوصف (عربي) / Description (Arabic) |
| DBF-SEC-019 | descriptionEn | description_en | String | NULL | No | — | الوصف (إنجليزي) / Description (English) |
| DBF-SEC-020 | isActiveFl | is_active_fl | Boolean | NOT NULL | Yes | — | نشط / Active |
| DBF-SEC-021..024 | createdBy/createdAt/updatedBy/updatedAt | created_by/created_at/updated_by/updated_at | String/Instant | see db-script | Yes | — | audit / audit |
DTO MEMBERSHIP: create-request excludes {rolePk, isActiveFl, audit}; update-request excludes {rolePk, code, isActiveFl, audit} (code immutable); response includes all.
LOOKUP FIELDS: none.
DOMAIN RULES: none scoped to Role alone.
STATE MACHINE: `isActiveFl` binary only — not applicable for a diagram (SRS A7).
CROSS-MODULE: none.
REPOSITORY OPS → QR-SEC-012 (FIND_BY_CRITERIA), QR-SEC-013 (SAVE), QR-SEC-034 (EXISTS, uniqueness).

#### ENT-SEC-004 — ModuleRegistry      kind: security
BINDINGS: table `SEC_MODULE_REG` · PK `moduleRegPk` (DBF-SEC-030) · PK generation `GENERATED ALWAYS AS IDENTITY`
BUSINESS CODE: none — `code` is the platform's own module prefix (factory.ids), supplied by the registering module, not system-generated.
FIELDS:
| DBF | property | column | type | null | read-only | constraint | label-ar / label-en |
|---|---|---|---|---|---|---|---|
| DBF-SEC-030 | moduleRegPk | module_reg_pk | Long | NOT NULL | Yes | PK_SEC_MODULE_REG | معرّف الوحدة المسجّلة / Registered module id |
| DBF-SEC-031 | code | code | String | NOT NULL | No | UQ_SEC_MODULE_REG_CODE | رمز الوحدة / Module code |
| DBF-SEC-032 | nameAr | name_ar | String | NOT NULL | No | — | اسم الوحدة (عربي) / Module name (Arabic) |
| DBF-SEC-033 | nameEn | name_en | String | NOT NULL | No | — | اسم الوحدة (إنجليزي) / Module name (English) |
| DBF-SEC-034..038 | isActiveFl/audit | is_active_fl/… | Boolean/String/Instant | see db-script | Yes | — | — |
DTO MEMBERSHIP: create-request excludes {moduleRegPk, isActiveFl, audit}; update: deactivate only, no field-level update endpoint; response includes all.
LOOKUP FIELDS: none.
DOMAIN RULES: none scoped alone (RULE-SEC-004 scopes ENT-SEC-005, cited there).
STATE MACHINE: binary active flag only — not applicable.
CROSS-MODULE: none — this table IS the shared registry every future module writes into; no XM row exists because a registration is a plain REST call to SEC (API-SEC-018), not a physical FK from another module's schema.
REPOSITORY OPS → QR-SEC-018 (SAVE), QR-SEC-021 (FIND_BY_CRITERIA), QR-SEC-035 (EXISTS, uniqueness).

#### ENT-SEC-005 — ScreenRegistry      kind: security
BINDINGS: table `SEC_SCREEN_REG` · PK `screenRegPk` (DBF-SEC-039) · PK generation `GENERATED ALWAYS AS IDENTITY`
BUSINESS CODE: none — `pageCode` is caller-supplied per SEC_PAGES convention, not system-generated.
FIELDS:
| DBF | property | column | type | null | read-only | constraint | label-ar / label-en |
|---|---|---|---|---|---|---|---|
| DBF-SEC-039 | screenRegPk | screen_reg_pk | Long | NOT NULL | Yes | PK_SEC_SCREEN_REG | معرّف الشاشة المسجّلة / Registered screen id |
| DBF-SEC-040 | pageCode | page_code | String | NOT NULL | No | UQ_SEC_SCREEN_REG_PAGE | رمز الصفحة / Page code |
| DBF-SEC-041 | moduleId | module_id | Long | NOT NULL | No | FK_SCREEN_REG_MODULE | الوحدة / Module |
| DBF-SEC-042 | nameAr | name_ar | String | NOT NULL | No | — | اسم الشاشة (عربي) / Screen name (Arabic) |
| DBF-SEC-043 | nameEn | name_en | String | NOT NULL | No | — | اسم الشاشة (إنجليزي) / Screen name (English) |
| DBF-SEC-044..048 | isActiveFl/audit | … | — | see db-script | Yes | — | — |
DTO MEMBERSHIP: create-request excludes {screenRegPk, isActiveFl, audit}; response includes all.
LOOKUP FIELDS: none.
DOMAIN RULES: **RULE-SEC-004** — Scope ENT-SEC-005 · Trigger: on create (screen registration) · Statement: "The system shall reject a screen registration whose module code has no ModuleRegistry row." · Message ar: "الوحدة غير مسجّلة" / en: "Module is not registered" · DB enforcement: `FK_SCREEN_REG_MODULE` (structural — the FK constraint itself makes an unregistered module impossible; the service layer pre-checks with QR-SEC-036 to raise the friendly catalog error before the DB would reject it) · owner layer: service (pre-check) + database (hard guarantee).
STATE MACHINE: binary active flag only — not applicable.
CROSS-MODULE: none.
REPOSITORY OPS → QR-SEC-019 (SAVE), QR-SEC-036 (EXISTS: module registered + page code unique).

#### ENT-SEC-006 — ActionRegistry      kind: security
BINDINGS: table `SEC_ACTION_REG` · PK `actionRegPk` (DBF-SEC-049) · PK generation `GENERATED ALWAYS AS IDENTITY`
BUSINESS CODE: none — `permissionCode` is derived (`PERM_<pageCode>_<actionCode>`), never entered a second time as seed data (single source of truth).
FIELDS:
| DBF | property | column | type | null | read-only | constraint | label-ar / label-en |
|---|---|---|---|---|---|---|---|
| DBF-SEC-049 | actionRegPk | action_reg_pk | Long | NOT NULL | Yes | PK_SEC_ACTION_REG | معرّف الإجراء المسجّل / Registered action id |
| DBF-SEC-050 | permissionCode | permission_code | String | NOT NULL | Yes (derived) | UQ_SEC_ACTION_REG_PERM | رمز الصلاحية / Permission code |
| DBF-SEC-051 | screenId | screen_id | Long | NOT NULL | No | FK_ACTION_REG_SCREEN | الشاشة / Screen |
| DBF-SEC-052 | actionCode | action_code | String | NOT NULL | No | — | الإجراء / Action |
| DBF-SEC-053 | nameAr | name_ar | String | NOT NULL | No | — | اسم الإجراء (عربي) / Action name (Arabic) |
| DBF-SEC-054 | nameEn | name_en | String | NOT NULL | No | — | اسم الإجراء (إنجليزي) / Action name (English) |
| DBF-SEC-055..059 | isActiveFl/audit | … | — | see db-script | Yes | — | — |
DTO MEMBERSHIP: create-request excludes {actionRegPk, permissionCode (server-derived), isActiveFl, audit}; response includes all, including the derived `permissionCode`.
LOOKUP FIELDS: none — `actionCode` is not lookup-backed (open set: VIEW/CREATE/UPDATE/DELETE + module-declared custom codes, SRS A3 note).
DOMAIN RULES: none scoped alone.
STATE MACHINE: binary active flag only — not applicable.
CROSS-MODULE: none.
REPOSITORY OPS → QR-SEC-020 (SAVE), QR-SEC-037 (EXISTS: screen exists + permission code unique).
<!-- SUB:DATA-DOM-MASTER:END -->
