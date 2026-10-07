<!-- source: content outside every PHASE block (leading / between / trailing sections) -->
# BACKEND EXECUTION PLAN — الأمان / Security (SEC)
══════════════════════════════════════════════════════════════════
Module : SEC   Version : v1   Profile : erp   Dialect : postgresql16
Framework : spring-boot-java (profile.stack.backend.framework)
Inputs : srs (v1, PRD-approved), db-script (v1), registry-srs (v1), registry-db (v1)
Open ADRs : 1 — erp/decisions/SEC/ (ADR-SEC-001, carried from P2; no new ADR this stage)
══════════════════════════════════════════════════════════════════

## PRE-GENERATION EXTRACTION — SEC v1 (working set; not part of the plan proper)

```
── FROM srs ──────────────────────────────────────────────────────────────
ENTITIES      13 — ENT-SEC-001..013, all kind=security (SRS A3)
REQUIREMENTS  35 — REQ-SEC-001..035 (SRS A4), each with ≥1 AC-SEC-* (034/035 added by the 2026-09-11 amendment)
RULES         7 — RULE-SEC-001..007 (SRS A5), full text reused verbatim below
SCREENS       10 — SCR-REQ-SEC-001..010 (SRS Part B), each composite per profile.conventions.composite_screen
PERMISSIONS   SEC_PAGES page codes + PERM_<PAGE_CODE>_<ACTION>, gateway VIEW (SRS §7.1 / Access summary)
LOOKUPS       USER_STATUS, SIGNUP_STATUS, AUDIT_EVENT_TYPE (SRS A6) — CHECK-constrained in v1 per ADR-SEC-001
BUSINESS CODE none — no SEC entity has a platform-numbered business code (SRS §3.3 NUMBERING test: all "No")
── FROM db-script ────────────────────────────────────────────────────────
TABLES        13 tables, exact names SEC_USER … SEC_SIGNUP_REQUEST (db-script-sec.md §3 BLOCK 2/3)
PK GENERATION every table: `GENERATED ALWAYS AS IDENTITY` (postgresql16 identity clause, db-script §3 header)
COLUMNS       104 DBF-SEC-001..104, exact names/types/null/default per db-script §1 and §6
CONSTRAINTS   PK_*, UQ_*, CHK_*, FK_* per db-script §3 BLOCK 5; INDEXES IDX_* per BLOCK 7
XM            none CONSUMED — SEC is ROOT (db-script §2 XM REGISTER: empty in that direction); 1 EXPOSED crossmodule surface, 0 XM rows (db-script §2 amendment 2026-09-11)
── FROM registries ───────────────────────────────────────────────────────
SHARED ENTITIES CONSUMED   none (SEC is ROOT)
EXISTING LOOKUP KEYS        USER_STATUS, SIGNUP_STATUS, AUDIT_EVENT_TYPE (registry-db-sec.md — reused, not recreated)
ID RANGES already used      API: none yet (this is the first use) · QR: none yet (this is the first use)
──────────────────────────────────────────────────────────────────────────
No row required §2A.3 extraction-failure handling — every field bound cleanly.
```

## EXECUTION PLAN INDEX — SEC v1 — backend-execution-plan-sec.md
Profile: erp · dialect: postgresql16 · framework: spring-boot-java
Open ADRs: 1 — decisions/SEC/ (ADR-SEC-001, non-breaking, carried from P2)

**ENTITY REGISTRY**
| ENT | Name | Table | Business code | Operations |
|---|---|---|---|---|
| ENT-SEC-001 | User | SEC_USER | none | create, read, search, update, activate, deactivate |
| ENT-SEC-002 | Role | SEC_ROLE | none | create, read, search, update, deactivate |
| ENT-SEC-003 | UserRoleAssignment | SEC_USER_ROLE | none | create, read (list), delete |
| ENT-SEC-004 | ModuleRegistry | SEC_MODULE_REG | none | create, read, search, deactivate |
| ENT-SEC-005 | ScreenRegistry | SEC_SCREEN_REG | none | create, read, search, deactivate |
| ENT-SEC-006 | ActionRegistry | SEC_ACTION_REG | none | create, read, search, deactivate |
| ENT-SEC-007 | RoleModuleGrant | SEC_ROLE_MODULE_GRANT | none | create, read (list), delete |
| ENT-SEC-008 | RoleScreenGrant | SEC_ROLE_SCREEN_GRANT | none | create, read (list), delete |
| ENT-SEC-009 | RoleActionGrant | SEC_ROLE_ACTION_GRANT | none | create, read (list), delete |
| ENT-SEC-010 | ActiveSession | SEC_ACTIVE_SESSION | none | create (system), read (list), terminate |
| ENT-SEC-011 | AuditLogEntry | SEC_AUDIT_LOG | none | create (system, append-only), read, search |
| ENT-SEC-012 | PasswordResetToken | SEC_PWD_RESET_TOKEN | none | create (system), read (validate), invalidate |
| ENT-SEC-013 | SignupRequest | SEC_SIGNUP_REQUEST | none | create, read, search, approve, reject |

**FIELD REGISTRY** (full binding detail: DB Alignment Manifest §below)
| DBF | Property | Read-only | ENT |
|---|---|---|---|
| DBF-SEC-001 | userPk | Yes | ENT-SEC-001 |
| DBF-SEC-002 | username | No | ENT-SEC-001 |
| DBF-SEC-003 | email | No | ENT-SEC-001 |
| DBF-SEC-004 | passwordHash | Yes | ENT-SEC-001 |
| DBF-SEC-005 | fullNameAr | No | ENT-SEC-001 |
| DBF-SEC-006 | fullNameEn | No | ENT-SEC-001 |
| DBF-SEC-007 | statusCode | Yes | ENT-SEC-001 |
| DBF-SEC-008 | lastLoginAt | Yes | ENT-SEC-001 |
| DBF-SEC-009 | isActiveFl | Yes | ENT-SEC-001 |
| DBF-SEC-010 | createdBy | Yes | ENT-SEC-001 |
| DBF-SEC-011 | createdAt | Yes | ENT-SEC-001 |
| DBF-SEC-012 | updatedBy | Yes | ENT-SEC-001 |
| DBF-SEC-013 | updatedAt | Yes | ENT-SEC-001 |
| DBF-SEC-014 | rolePk | Yes | ENT-SEC-002 |
| DBF-SEC-015 | code | No | ENT-SEC-002 |
| DBF-SEC-016 | nameAr | No | ENT-SEC-002 |
| DBF-SEC-017 | nameEn | No | ENT-SEC-002 |
| DBF-SEC-018 | descriptionAr | No | ENT-SEC-002 |
| DBF-SEC-019 | descriptionEn | No | ENT-SEC-002 |
| DBF-SEC-020 | isActiveFl | Yes | ENT-SEC-002 |
| DBF-SEC-021 | createdBy | Yes | ENT-SEC-002 |
| DBF-SEC-022 | createdAt | Yes | ENT-SEC-002 |
| DBF-SEC-023 | updatedBy | Yes | ENT-SEC-002 |
| DBF-SEC-024 | updatedAt | Yes | ENT-SEC-002 |
| DBF-SEC-025 | userRolePk | Yes | ENT-SEC-003 |
| DBF-SEC-026 | userId | No | ENT-SEC-003 |
| DBF-SEC-027 | roleId | No | ENT-SEC-003 |
| DBF-SEC-028 | assignedBy | Yes | ENT-SEC-003 |
| DBF-SEC-029 | assignedAt | Yes | ENT-SEC-003 |
| DBF-SEC-030 | moduleRegPk | Yes | ENT-SEC-004 |
| DBF-SEC-031 | code | No | ENT-SEC-004 |
| DBF-SEC-032 | nameAr | No | ENT-SEC-004 |
| DBF-SEC-033 | nameEn | No | ENT-SEC-004 |
| DBF-SEC-034 | isActiveFl | Yes | ENT-SEC-004 |
| DBF-SEC-035 | createdBy | Yes | ENT-SEC-004 |
| DBF-SEC-036 | createdAt | Yes | ENT-SEC-004 |
| DBF-SEC-037 | updatedBy | Yes | ENT-SEC-004 |
| DBF-SEC-038 | updatedAt | Yes | ENT-SEC-004 |
| DBF-SEC-039 | screenRegPk | Yes | ENT-SEC-005 |
| DBF-SEC-040 | pageCode | No | ENT-SEC-005 |
| DBF-SEC-041 | moduleId | No | ENT-SEC-005 |
| DBF-SEC-042 | nameAr | No | ENT-SEC-005 |
| DBF-SEC-043 | nameEn | No | ENT-SEC-005 |
| DBF-SEC-044 | isActiveFl | Yes | ENT-SEC-005 |
| DBF-SEC-045 | createdBy | Yes | ENT-SEC-005 |
| DBF-SEC-046 | createdAt | Yes | ENT-SEC-005 |
| DBF-SEC-047 | updatedBy | Yes | ENT-SEC-005 |
| DBF-SEC-048 | updatedAt | Yes | ENT-SEC-005 |
| DBF-SEC-049 | actionRegPk | Yes | ENT-SEC-006 |
| DBF-SEC-050 | permissionCode | Yes | ENT-SEC-006 |
| DBF-SEC-051 | screenId | No | ENT-SEC-006 |
| DBF-SEC-052 | actionCode | No | ENT-SEC-006 |
| DBF-SEC-053 | nameAr | No | ENT-SEC-006 |
| DBF-SEC-054 | nameEn | No | ENT-SEC-006 |
| DBF-SEC-055 | isActiveFl | Yes | ENT-SEC-006 |
| DBF-SEC-056 | createdBy | Yes | ENT-SEC-006 |
| DBF-SEC-057 | createdAt | Yes | ENT-SEC-006 |
| DBF-SEC-058 | updatedBy | Yes | ENT-SEC-006 |
| DBF-SEC-059 | updatedAt | Yes | ENT-SEC-006 |
| DBF-SEC-060 | roleModuleGrantPk | Yes | ENT-SEC-007 |
| DBF-SEC-061 | roleId | No | ENT-SEC-007 |
| DBF-SEC-062 | moduleId | No | ENT-SEC-007 |
| DBF-SEC-063 | grantedBy | Yes | ENT-SEC-007 |
| DBF-SEC-064 | grantedAt | Yes | ENT-SEC-007 |
| DBF-SEC-065 | roleScreenGrantPk | Yes | ENT-SEC-008 |
| DBF-SEC-066 | roleId | No | ENT-SEC-008 |
| DBF-SEC-067 | screenId | No | ENT-SEC-008 |
| DBF-SEC-068 | grantedBy | Yes | ENT-SEC-008 |
| DBF-SEC-069 | grantedAt | Yes | ENT-SEC-008 |
| DBF-SEC-070 | roleActionGrantPk | Yes | ENT-SEC-009 |
| DBF-SEC-071 | roleId | No | ENT-SEC-009 |
| DBF-SEC-072 | actionId | No | ENT-SEC-009 |
| DBF-SEC-073 | grantedBy | Yes | ENT-SEC-009 |
| DBF-SEC-074 | grantedAt | Yes | ENT-SEC-009 |
| DBF-SEC-075 | activeSessionPk | Yes | ENT-SEC-010 |
| DBF-SEC-076 | userId | Yes | ENT-SEC-010 |
| DBF-SEC-077 | tokenRef | Yes | ENT-SEC-010 |
| DBF-SEC-078 | startedAt | Yes | ENT-SEC-010 |
| DBF-SEC-079 | lastActivityAt | Yes | ENT-SEC-010 |
| DBF-SEC-080 | ipAddress | Yes | ENT-SEC-010 |
| DBF-SEC-081 | terminatedAt | Yes | ENT-SEC-010 |
| DBF-SEC-082 | terminatedBy | Yes | ENT-SEC-010 |
| DBF-SEC-083 | auditLogPk | Yes | ENT-SEC-011 |
| DBF-SEC-084 | eventTypeCode | Yes | ENT-SEC-011 |
| DBF-SEC-085 | actorUserId | Yes | ENT-SEC-011 |
| DBF-SEC-086 | occurredAt | Yes | ENT-SEC-011 |
| DBF-SEC-087 | targetRef | Yes | ENT-SEC-011 |
| DBF-SEC-088 | detailsAr | Yes | ENT-SEC-011 |
| DBF-SEC-089 | detailsEn | Yes | ENT-SEC-011 |
| DBF-SEC-090 | ipAddress | Yes | ENT-SEC-011 |
| DBF-SEC-091 | pwdResetTokenPk | Yes | ENT-SEC-012 |
| DBF-SEC-092 | userId | Yes | ENT-SEC-012 |
| DBF-SEC-093 | tokenHash | Yes | ENT-SEC-012 |
| DBF-SEC-094 | requestedAt | Yes | ENT-SEC-012 |
| DBF-SEC-095 | expiresAt | Yes | ENT-SEC-012 |
| DBF-SEC-096 | usedAt | Yes | ENT-SEC-012 |
| DBF-SEC-097 | signupRequestPk | Yes | ENT-SEC-013 |
| DBF-SEC-098 | email | No | ENT-SEC-013 |
| DBF-SEC-099 | fullNameAr | No | ENT-SEC-013 |
| DBF-SEC-100 | fullNameEn | No | ENT-SEC-013 |
| DBF-SEC-101 | submittedAt | Yes | ENT-SEC-013 |
| DBF-SEC-102 | statusCode | Yes | ENT-SEC-013 |
| DBF-SEC-103 | reviewedBy | Yes | ENT-SEC-013 |
| DBF-SEC-104 | reviewedAt | Yes | ENT-SEC-013 |

**API REGISTRY** (full blocks: §Phase 3 SVC-API below)
| API | Operation | Verb | Path | Traces (REQ, DBF) |
|---|---|---|---|---|
| API-SEC-001 | login | POST | /api/v1/sec/auth/login | REQ-SEC-001,REQ-SEC-002 · DBF-SEC-002,DBF-SEC-004,DBF-SEC-007,DBF-SEC-075..078 |
| API-SEC-002 | submit sign-up | POST | /api/v1/sec/auth/signup | REQ-SEC-003 · DBF-SEC-098,DBF-SEC-099,DBF-SEC-100,DBF-SEC-101,DBF-SEC-102 |
| API-SEC-003 | request password reset | POST | /api/v1/sec/auth/password-reset/request | REQ-SEC-006,REQ-SEC-029 · DBF-SEC-092,DBF-SEC-093,DBF-SEC-094,DBF-SEC-095 |
| API-SEC-004 | complete password reset | POST | /api/v1/sec/auth/password-reset/complete | REQ-SEC-007,REQ-SEC-008 · DBF-SEC-093,DBF-SEC-095,DBF-SEC-096,DBF-SEC-004 |
| API-SEC-005 | search users | POST | /api/v1/sec/users/search | REQ-SEC-009 · DBF-SEC-002,DBF-SEC-003,DBF-SEC-005,DBF-SEC-006,DBF-SEC-007 |
| API-SEC-006 | create user | POST | /api/v1/sec/users | REQ-SEC-009 · DBF-SEC-002,DBF-SEC-003,DBF-SEC-005,DBF-SEC-006,DBF-SEC-007 |
| API-SEC-007 | update user | PUT | /api/v1/sec/users/{id} | REQ-SEC-009 · DBF-SEC-003,DBF-SEC-005,DBF-SEC-006 |
| API-SEC-008 | assign roles to user | PUT | /api/v1/sec/users/{id}/roles | REQ-SEC-010 · DBF-SEC-026,DBF-SEC-027,DBF-SEC-028,DBF-SEC-029 |
| API-SEC-009 | deactivate user | DELETE | /api/v1/sec/users/{id} | REQ-SEC-011 · DBF-SEC-007,DBF-SEC-009,DBF-SEC-081,DBF-SEC-082 |
| API-SEC-010 | reactivate user | PATCH | /api/v1/sec/users/{id} | REQ-SEC-031 · DBF-SEC-007,DBF-SEC-009 |
| API-SEC-011 | approve/reject signup | PATCH | /api/v1/sec/signup-requests/{id} | REQ-SEC-004,REQ-SEC-005 · DBF-SEC-102,DBF-SEC-103,DBF-SEC-104,DBF-SEC-001 |
| API-SEC-012 | search roles | POST | /api/v1/sec/roles/search | REQ-SEC-012 · DBF-SEC-015,DBF-SEC-016,DBF-SEC-017,DBF-SEC-020 |
| API-SEC-013 | create role | POST | /api/v1/sec/roles | REQ-SEC-012 · DBF-SEC-015,DBF-SEC-016,DBF-SEC-017,DBF-SEC-018,DBF-SEC-019 |
| API-SEC-014 | grant module to role | POST | /api/v1/sec/roles/{id}/modules | REQ-SEC-012 · DBF-SEC-061,DBF-SEC-062,DBF-SEC-063,DBF-SEC-064 |
| API-SEC-015 | revoke module grant | DELETE | /api/v1/sec/roles/{id}/modules/{moduleId} | REQ-SEC-015 · DBF-SEC-061,DBF-SEC-062,DBF-SEC-066,DBF-SEC-071 |
| API-SEC-016 | grant screen to role | POST | /api/v1/sec/roles/{id}/screens | REQ-SEC-013 · DBF-SEC-066,DBF-SEC-067,DBF-SEC-068,DBF-SEC-069 |
| API-SEC-017 | grant action to role | POST | /api/v1/sec/roles/{id}/actions | REQ-SEC-014,REQ-SEC-020,REQ-SEC-030 · DBF-SEC-071,DBF-SEC-072,DBF-SEC-073,DBF-SEC-074 |
| API-SEC-018 | register module | POST | /api/v1/sec/registry/modules | REQ-SEC-016 · DBF-SEC-031,DBF-SEC-032,DBF-SEC-033 |
| API-SEC-019 | register screen | POST | /api/v1/sec/registry/screens | REQ-SEC-017,REQ-SEC-018 · DBF-SEC-040,DBF-SEC-041,DBF-SEC-042,DBF-SEC-043 |
| API-SEC-020 | register action | POST | /api/v1/sec/registry/actions | REQ-SEC-019 · DBF-SEC-050,DBF-SEC-051,DBF-SEC-052,DBF-SEC-053,DBF-SEC-054 |
| API-SEC-021 | search registry | POST | /api/v1/sec/registry/search | REQ-SEC-016 · DBF-SEC-031,DBF-SEC-040,DBF-SEC-050 |
| API-SEC-022 | dashboard summary | GET | /api/v1/sec/dashboard | REQ-SEC-022,REQ-SEC-023 · DBF-SEC-007,DBF-SEC-076,DBF-SEC-081,DBF-SEC-084,DBF-SEC-086 |
| API-SEC-023 | search audit log | POST | /api/v1/sec/audit-log/search | REQ-SEC-025 · DBF-SEC-084,DBF-SEC-085,DBF-SEC-086 |
| API-SEC-024 | export audit log | GET | /api/v1/sec/audit-log/export | REQ-SEC-026 · DBF-SEC-084,DBF-SEC-085,DBF-SEC-086,DBF-SEC-087,DBF-SEC-088,DBF-SEC-089 |
| API-SEC-025 | list active sessions | POST | /api/v1/sec/sessions/search | REQ-SEC-027 · DBF-SEC-076,DBF-SEC-079,DBF-SEC-081 |
| API-SEC-026 | terminate session | DELETE | /api/v1/sec/sessions/{id} | REQ-SEC-028 · DBF-SEC-081,DBF-SEC-082 |
| API-SEC-027 | effective menu | GET | /api/v1/sec/menu | REQ-SEC-021,REQ-SEC-032,REQ-SEC-033 · DBF-SEC-031,DBF-SEC-040,DBF-SEC-061,DBF-SEC-066 |

**RULE REGISTRY**
| RULE | Name | Scope (ENT) | Message ar/en ✓ |
|---|---|---|---|
| RULE-SEC-001 | No screen grant without its module grant | ENT-SEC-008 | ✓ |
| RULE-SEC-002 | No action grant without its screen grant | ENT-SEC-009 | ✓ |
| RULE-SEC-003 | Cascade revoke on module-grant removal | ENT-SEC-007 | ✓ |
| RULE-SEC-004 | No screen under an unregistered module | ENT-SEC-005 | ✓ |
| RULE-SEC-005 | Prevent conflicting actions on one user | ENT-SEC-003, ENT-SEC-009 | ✓ |
| RULE-SEC-006 | Reject expired or used reset token | ENT-SEC-012 | ✓ |
| RULE-SEC-007 | VIEW as the screen-level gateway action | ENT-SEC-009 | ✓ |

**SCREEN REGISTRY**
| Screen | Type | ENT | Permission names |
|---|---|---|---|
| SEC_LOGIN | public | ENT-SEC-001 | (none — public) |
| SEC_SIGNUP | public | ENT-SEC-013 | (none — public) |
| SEC_PWD_RESET | public | ENT-SEC-012 | (none — public) |
| SEC_USERS | secured | ENT-SEC-001 | PERM_SEC_USERS_VIEW, PERM_SEC_USERS_CREATE, PERM_SEC_USERS_UPDATE |
| SEC_ROLES | secured | ENT-SEC-002 | PERM_SEC_ROLES_VIEW, PERM_SEC_ROLES_CREATE, PERM_SEC_ROLES_UPDATE, PERM_SEC_ROLES_DELETE |
| SEC_MODULE_REGISTRY | secured | ENT-SEC-004 | PERM_SEC_MODULE_REGISTRY_VIEW, PERM_SEC_MODULE_REGISTRY_UPDATE |
| SEC_DASHBOARD | secured | (aggregate) | PERM_SEC_DASHBOARD_VIEW (+ per-widget source-screen VIEW) |
| SEC_AUDIT_LOG | secured | ENT-SEC-011 | PERM_SEC_AUDIT_LOG_VIEW |
| SEC_SESSIONS | secured | ENT-SEC-010 | PERM_SEC_SESSIONS_VIEW, PERM_SEC_SESSIONS_DELETE |
| (menu) | derived | — | none of its own — filtered per target page's own permissions |

**LOOKUP REGISTRY**
| Key | Used in field | ENT |
|---|---|---|
| USER_STATUS | statusCode | ENT-SEC-001 |
| SIGNUP_STATUS | statusCode | ENT-SEC-013 |
| AUDIT_EVENT_TYPE | eventTypeCode | ENT-SEC-011 |

**QRC SUMMARY** (agent reference only — full catalog below)
39 QR ids, QR-SEC-001..039 — see §Query Reference Catalog (QR-SEC-039 added 2026-09-11 with REQ-SEC-035).

**DB ALIGNMENT** — see manifest below — ALIGNED ✓ / issues: 0
**XM STATUS** — 0 CONSUMED (SEC is ROOT) · 1 EXPOSED crossmodule surface
(`com.erp.sec.crossmodule.SecUserDirectoryApi`, REQ-SEC-034/035) carrying 0 XM rows — it
registers no entity, table or column, and ids for that direction are the consuming module's
own P2 to assign, not SEC's
**SECURITY** — 7 secured screens × role-driven grants (no fixed role count — data-driven per RBAC)

## DB Alignment Manifest — SEC v1
DBF-* │ ENT-* │ plan property │ plan type │ XM-* │ status — sourced by lookup from db-script-sec.md §1; not reproduced here beyond the four bound columns.

All 104 rows: **status ✓ (aligned), XM — (none)** for every row; property/type below (Java types, `profile.stack.backend.framework` = spring-boot-java; TIMESTAMPTZ→Instant, VARCHAR/TEXT→String, BOOLEAN→Boolean, BIGINT identity→Long).

| DBF | ENT | property | type |
|---|---|---|---|
| DBF-SEC-001 | ENT-SEC-001 | userPk | Long |
| DBF-SEC-002 | ENT-SEC-001 | username | String |
| DBF-SEC-003 | ENT-SEC-001 | email | String |
| DBF-SEC-004 | ENT-SEC-001 | passwordHash | String |
| DBF-SEC-005 | ENT-SEC-001 | fullNameAr | String |
| DBF-SEC-006 | ENT-SEC-001 | fullNameEn | String |
| DBF-SEC-007 | ENT-SEC-001 | statusCode | String |
| DBF-SEC-008 | ENT-SEC-001 | lastLoginAt | Instant |
| DBF-SEC-009 | ENT-SEC-001 | isActiveFl | Boolean |
| DBF-SEC-010 | ENT-SEC-001 | createdBy | String |
| DBF-SEC-011 | ENT-SEC-001 | createdAt | Instant |
| DBF-SEC-012 | ENT-SEC-001 | updatedBy | String |
| DBF-SEC-013 | ENT-SEC-001 | updatedAt | Instant |
| DBF-SEC-014 | ENT-SEC-002 | rolePk | Long |
| DBF-SEC-015 | ENT-SEC-002 | code | String |
| DBF-SEC-016 | ENT-SEC-002 | nameAr | String |
| DBF-SEC-017 | ENT-SEC-002 | nameEn | String |
| DBF-SEC-018 | ENT-SEC-002 | descriptionAr | String |
| DBF-SEC-019 | ENT-SEC-002 | descriptionEn | String |
| DBF-SEC-020 | ENT-SEC-002 | isActiveFl | Boolean |
| DBF-SEC-021 | ENT-SEC-002 | createdBy | String |
| DBF-SEC-022 | ENT-SEC-002 | createdAt | Instant |
| DBF-SEC-023 | ENT-SEC-002 | updatedBy | String |
| DBF-SEC-024 | ENT-SEC-002 | updatedAt | Instant |
| DBF-SEC-025 | ENT-SEC-003 | userRolePk | Long |
| DBF-SEC-026 | ENT-SEC-003 | userId | Long |
| DBF-SEC-027 | ENT-SEC-003 | roleId | Long |
| DBF-SEC-028 | ENT-SEC-003 | assignedBy | String |
| DBF-SEC-029 | ENT-SEC-003 | assignedAt | Instant |
| DBF-SEC-030 | ENT-SEC-004 | moduleRegPk | Long |
| DBF-SEC-031 | ENT-SEC-004 | code | String |
| DBF-SEC-032 | ENT-SEC-004 | nameAr | String |
| DBF-SEC-033 | ENT-SEC-004 | nameEn | String |
| DBF-SEC-034 | ENT-SEC-004 | isActiveFl | Boolean |
| DBF-SEC-035 | ENT-SEC-004 | createdBy | String |
| DBF-SEC-036 | ENT-SEC-004 | createdAt | Instant |
| DBF-SEC-037 | ENT-SEC-004 | updatedBy | String |
| DBF-SEC-038 | ENT-SEC-004 | updatedAt | Instant |
| DBF-SEC-039 | ENT-SEC-005 | screenRegPk | Long |
| DBF-SEC-040 | ENT-SEC-005 | pageCode | String |
| DBF-SEC-041 | ENT-SEC-005 | moduleId | Long |
| DBF-SEC-042 | ENT-SEC-005 | nameAr | String |
| DBF-SEC-043 | ENT-SEC-005 | nameEn | String |
| DBF-SEC-044 | ENT-SEC-005 | isActiveFl | Boolean |
| DBF-SEC-045 | ENT-SEC-005 | createdBy | String |
| DBF-SEC-046 | ENT-SEC-005 | createdAt | Instant |
| DBF-SEC-047 | ENT-SEC-005 | updatedBy | String |
| DBF-SEC-048 | ENT-SEC-005 | updatedAt | Instant |
| DBF-SEC-049 | ENT-SEC-006 | actionRegPk | Long |
| DBF-SEC-050 | ENT-SEC-006 | permissionCode | String |
| DBF-SEC-051 | ENT-SEC-006 | screenId | Long |
| DBF-SEC-052 | ENT-SEC-006 | actionCode | String |
| DBF-SEC-053 | ENT-SEC-006 | nameAr | String |
| DBF-SEC-054 | ENT-SEC-006 | nameEn | String |
| DBF-SEC-055 | ENT-SEC-006 | isActiveFl | Boolean |
| DBF-SEC-056 | ENT-SEC-006 | createdBy | String |
| DBF-SEC-057 | ENT-SEC-006 | createdAt | Instant |
| DBF-SEC-058 | ENT-SEC-006 | updatedBy | String |
| DBF-SEC-059 | ENT-SEC-006 | updatedAt | Instant |
| DBF-SEC-060 | ENT-SEC-007 | roleModuleGrantPk | Long |
| DBF-SEC-061 | ENT-SEC-007 | roleId | Long |
| DBF-SEC-062 | ENT-SEC-007 | moduleId | Long |
| DBF-SEC-063 | ENT-SEC-007 | grantedBy | String |
| DBF-SEC-064 | ENT-SEC-007 | grantedAt | Instant |
| DBF-SEC-065 | ENT-SEC-008 | roleScreenGrantPk | Long |
| DBF-SEC-066 | ENT-SEC-008 | roleId | Long |
| DBF-SEC-067 | ENT-SEC-008 | screenId | Long |
| DBF-SEC-068 | ENT-SEC-008 | grantedBy | String |
| DBF-SEC-069 | ENT-SEC-008 | grantedAt | Instant |
| DBF-SEC-070 | ENT-SEC-009 | roleActionGrantPk | Long |
| DBF-SEC-071 | ENT-SEC-009 | roleId | Long |
| DBF-SEC-072 | ENT-SEC-009 | actionId | Long |
| DBF-SEC-073 | ENT-SEC-009 | grantedBy | String |
| DBF-SEC-074 | ENT-SEC-009 | grantedAt | Instant |
| DBF-SEC-075 | ENT-SEC-010 | activeSessionPk | Long |
| DBF-SEC-076 | ENT-SEC-010 | userId | Long |
| DBF-SEC-077 | ENT-SEC-010 | tokenRef | String |
| DBF-SEC-078 | ENT-SEC-010 | startedAt | Instant |
| DBF-SEC-079 | ENT-SEC-010 | lastActivityAt | Instant |
| DBF-SEC-080 | ENT-SEC-010 | ipAddress | String |
| DBF-SEC-081 | ENT-SEC-010 | terminatedAt | Instant |
| DBF-SEC-082 | ENT-SEC-010 | terminatedBy | String |
| DBF-SEC-083 | ENT-SEC-011 | auditLogPk | Long |
| DBF-SEC-084 | ENT-SEC-011 | eventTypeCode | String |
| DBF-SEC-085 | ENT-SEC-011 | actorUserId | Long |
| DBF-SEC-086 | ENT-SEC-011 | occurredAt | Instant |
| DBF-SEC-087 | ENT-SEC-011 | targetRef | String |
| DBF-SEC-088 | ENT-SEC-011 | detailsAr | String |
| DBF-SEC-089 | ENT-SEC-011 | detailsEn | String |
| DBF-SEC-090 | ENT-SEC-011 | ipAddress | String |
| DBF-SEC-091 | ENT-SEC-012 | pwdResetTokenPk | Long |
| DBF-SEC-092 | ENT-SEC-012 | userId | Long |
| DBF-SEC-093 | ENT-SEC-012 | tokenHash | String |
| DBF-SEC-094 | ENT-SEC-012 | requestedAt | Instant |
| DBF-SEC-095 | ENT-SEC-012 | expiresAt | Instant |
| DBF-SEC-096 | ENT-SEC-012 | usedAt | Instant |
| DBF-SEC-097 | ENT-SEC-013 | signupRequestPk | Long |
| DBF-SEC-098 | ENT-SEC-013 | email | String |
| DBF-SEC-099 | ENT-SEC-013 | fullNameAr | String |
| DBF-SEC-100 | ENT-SEC-013 | fullNameEn | String |
| DBF-SEC-101 | ENT-SEC-013 | submittedAt | Instant |
| DBF-SEC-102 | ENT-SEC-013 | statusCode | String |
| DBF-SEC-103 | ENT-SEC-013 | reviewedBy | String |
| DBF-SEC-104 | ENT-SEC-013 | reviewedAt | Instant |

## Query Reference Catalog (QR-SEC-*)

> Logical specification only — never executable code; the implementer rewrites every
> entry with real entity classes and the project's query strategy (§5 warning, engine).

| QR | Operation | Phase | API | Entity | Kind | Intent |
|---|---|---|---|---|---|---|
| QR-SEC-001 | FIND_ONE | SVC-API | API-SEC-001 | ENT-SEC-001 | FIND_ONE by username | resolve login identity |
| QR-SEC-002 | SAVE | SVC-API | API-SEC-002 | ENT-SEC-013 | SAVE | create pending sign-up |
| QR-SEC-003 | SAVE | SVC-API | API-SEC-003 | ENT-SEC-012 | SAVE | issue reset token |
| QR-SEC-004 | UPDATE | SVC-API | API-SEC-004 | ENT-SEC-001, ENT-SEC-012 | UPDATE | set new password hash + mark token used |
| QR-SEC-005 | FIND_BY_CRITERIA | SVC-API | API-SEC-005 | ENT-SEC-001 | FIND_BY_CRITERIA | search users |
| QR-SEC-006 | SAVE | SVC-API | API-SEC-006 | ENT-SEC-001 | SAVE | create user |
| QR-SEC-007 | UPDATE | SVC-API | API-SEC-007 | ENT-SEC-001 | UPDATE | update user profile fields |
| QR-SEC-008 | SAVE | SVC-API | API-SEC-008 | ENT-SEC-003 | SAVE (batch) | assign roles to user |
| QR-SEC-009 | UPDATE | SVC-API | API-SEC-009 | ENT-SEC-001, ENT-SEC-010 | UPDATE | deactivate user + terminate its sessions |
| QR-SEC-010 | UPDATE | SVC-API | API-SEC-010 | ENT-SEC-001 | UPDATE | reactivate user |
| QR-SEC-011 | UPDATE | SVC-API | API-SEC-011 | ENT-SEC-013, ENT-SEC-001 | UPDATE / SAVE | approve (creates user) / reject sign-up |
| QR-SEC-012 | FIND_BY_CRITERIA | SVC-API | API-SEC-012 | ENT-SEC-002 | FIND_BY_CRITERIA | search roles |
| QR-SEC-013 | SAVE | SVC-API | API-SEC-013 | ENT-SEC-002 | SAVE | create role |
| QR-SEC-014 | SAVE | SVC-API | API-SEC-014 | ENT-SEC-007 | SAVE | grant module |
| QR-SEC-015 | DELETE | SVC-API | API-SEC-015 | ENT-SEC-007 | DELETE | revoke module grant |
| QR-SEC-016 | SAVE | SVC-API | API-SEC-016 | ENT-SEC-008 | SAVE | grant screen |
| QR-SEC-017 | SAVE | SVC-API | API-SEC-017 | ENT-SEC-009 | SAVE | grant action |
| QR-SEC-018 | SAVE | SVC-API | API-SEC-018 | ENT-SEC-004 | SAVE | register module |
| QR-SEC-019 | SAVE | SVC-API | API-SEC-019 | ENT-SEC-005 | SAVE | register screen |
| QR-SEC-020 | SAVE | SVC-API | API-SEC-020 | ENT-SEC-006 | SAVE | register action |
| QR-SEC-021 | FIND_BY_CRITERIA | SVC-API | API-SEC-021 | ENT-SEC-004, ENT-SEC-005, ENT-SEC-006 | FIND_BY_CRITERIA | browse registry tree |
| QR-SEC-022 | AGGREGATE | SVC-API | API-SEC-022 | ENT-SEC-001, ENT-SEC-010, ENT-SEC-011, ENT-SEC-002 | AGGREGATE | dashboard figures (6 widget sub-counts) |
| QR-SEC-023 | FIND_BY_CRITERIA | SVC-API | API-SEC-023 | ENT-SEC-011 | FIND_BY_CRITERIA | search audit log |
| QR-SEC-024 | FIND_BY_CRITERIA | SVC-API | API-SEC-024 | ENT-SEC-011 | FIND_BY_CRITERIA | audit log rows for export |
| QR-SEC-025 | FIND_BY_CRITERIA | SVC-API | API-SEC-025 | ENT-SEC-010 | FIND_BY_CRITERIA | non-terminated sessions |
| QR-SEC-026 | UPDATE | SVC-API | API-SEC-026 | ENT-SEC-010 | UPDATE | terminate session |
| QR-SEC-027 | FIND_BY_CRITERIA | SVC-API | API-SEC-027 | ENT-SEC-004, ENT-SEC-005, ENT-SEC-007, ENT-SEC-008 | FIND_BY_CRITERIA | effective menu tree for caller |
| QR-SEC-028 | EXISTS | SVC-API | API-SEC-016 | ENT-SEC-007 | EXISTS | RULE-SEC-001: role holds the screen's module grant? |
| QR-SEC-029 | EXISTS | SVC-API | API-SEC-017 | ENT-SEC-008 | EXISTS | RULE-SEC-002: role holds the action's screen grant? |
| QR-SEC-030 | EXISTS | SVC-API | API-SEC-017 | ENT-SEC-009 | EXISTS | RULE-SEC-007: role holds VIEW on the action's screen? |
| QR-SEC-031 | EXISTS | SVC-API | API-SEC-008, API-SEC-017 | ENT-SEC-009 | EXISTS | RULE-SEC-005: user already holds the conflicting action? |
| QR-SEC-032 | FIND_ALL | SVC-API | API-SEC-015 | ENT-SEC-008, ENT-SEC-009 | FIND_ALL | RULE-SEC-003: dependent screen/action grants for cascade |
| QR-SEC-033 | EXISTS | SVC-API | API-SEC-006 | ENT-SEC-001 | EXISTS | uniqueness: username / email |
| QR-SEC-034 | EXISTS | SVC-API | API-SEC-013 | ENT-SEC-002 | EXISTS | uniqueness: role code |
| QR-SEC-035 | EXISTS | SVC-API | API-SEC-018 | ENT-SEC-004 | EXISTS | uniqueness: module code |
| QR-SEC-036 | EXISTS | SVC-API | API-SEC-019 | ENT-SEC-004, ENT-SEC-005 | EXISTS | RULE-SEC-004: module registered? + uniqueness: page code |
| QR-SEC-037 | EXISTS | SVC-API | API-SEC-020 | ENT-SEC-005, ENT-SEC-006 | EXISTS | screen exists? + uniqueness: permission code |
| QR-SEC-038 | EXISTS | SVC-API | API-SEC-004 | ENT-SEC-012 | EXISTS | RULE-SEC-006: token unexpired and unused? |
| QR-SEC-039 | FIND_ALL | INT-R | — (no API — `SecUserDirectoryApi`) | ENT-SEC-001, ENT-SEC-003, ENT-SEC-006, ENT-SEC-009 | FIND_ALL | REQ-SEC-035: user ids holding a permission code through an active role |

Standard operation defaults (engine §5) apply to every QR above unless noted; no QR overrides
paging/filter/transaction defaults except where its row states otherwise. Join governance:
every QR above is single-table or a documented FK join within SEC only (no cross-module join
— SEC has zero XM); no QR joins to resolve a lookup label (`statusCode`/`eventTypeCode` are
returned as codes; the frontend resolves the display label).

---

















## Error Catalog — SEC v1

Envelope: `LocalizedException → {code, messageAr, messageEn}`. Runtime code format: `SEC-<HTTP-status>-<SCENARIO>` (Phase 1 CORE).

| code | RULE / PLATFORM-STD | API | HTTP | trigger | message-AR | message-EN |
|---|---|---|---|---|---|---|
| SEC-401-INVALID-CREDENTIALS | PLATFORM-STD (ADR-SEC-002, new — see below) | API-SEC-001 | 401 | wrong/unknown credentials | بيانات الدخول غير صحيحة | Invalid credentials |
| SEC-409-USER-DUP | PLATFORM-STD (uniqueness) | API-SEC-006, 007 | 409 | duplicate username/email | اسم المستخدم أو البريد الإلكتروني مستخدم بالفعل | Username or email already in use |
| SEC-404-USER | PLATFORM-STD (not found) | API-SEC-007, 008, 009, 010 | 404 | unknown user id | المستخدم غير موجود | User not found |
| SEC-409-SOD-CONFLICT | RULE-SEC-005 | API-SEC-008, 017 | 409 | conflicting action pair on one user | هذا المستخدم يملك إجراءً متعارضًا بالفعل | This user already holds a conflicting action |
| SEC-404-ROLE | PLATFORM-STD (not found) | API-SEC-008, 014, 016, 017 | 404 | unknown role id | الدور غير موجود | Role not found |
| SEC-409-INVALID-TRANSITION | PLATFORM-STD (lifecycle) | API-SEC-010, 011 | 409 | transition not allowed from current status | لا يمكن تنفيذ هذا الانتقال من الحالة الحالية | This transition is not allowed from the current status |
| SEC-404-SIGNUP | PLATFORM-STD (not found) | API-SEC-011 | 404 | unknown signup request id | طلب التسجيل غير موجود | Signup request not found |
| SEC-409-ROLE-DUP | PLATFORM-STD (uniqueness) | API-SEC-013 | 409 | duplicate role code | رمز الدور مستخدم بالفعل | Role code already in use |
| SEC-409-GRANT-DUP | PLATFORM-STD (uniqueness) | API-SEC-014, 016, 017 | 409 | grant already exists | هذا المنح موجود بالفعل | This grant already exists |
| SEC-404-MODULE | PLATFORM-STD (not found) | API-SEC-014, 015 | 404 | unknown module id | الوحدة غير موجودة | Module not found |
| SEC-404-GRANT | PLATFORM-STD (not found) | API-SEC-015 | 404 | grant does not exist | المنح غير موجود | Grant not found |
| SEC-409-NO-MODULE-GRANT | RULE-SEC-001 | API-SEC-016 | 409 | screen grant attempted without module grant | لا يمكن منح شاشة دون منح الوحدة أولًا | Cannot grant a screen without first granting its module |
| SEC-404-SCREEN | PLATFORM-STD (not found) | API-SEC-016, 017, 019 | 404 | unknown screen id / page code | الشاشة غير موجودة | Screen not found |
| SEC-409-NO-SCREEN-GRANT | RULE-SEC-002 | API-SEC-017 | 409 | action grant attempted without screen grant | لا يمكن منح إجراء دون منح الشاشة أولًا | Cannot grant an action without first granting its screen |
| SEC-409-NO-VIEW-GRANT | RULE-SEC-007 | API-SEC-017 | 409 | non-VIEW action grant attempted without VIEW | يلزم منح إجراء العرض (VIEW) أولًا على هذه الشاشة | The VIEW action must be granted on this screen first |
| SEC-404-ACTION | PLATFORM-STD (not found) | API-SEC-017 | 404 | unknown action id | الإجراء غير موجود | Action not found |
| SEC-409-MODULE-DUP | PLATFORM-STD (uniqueness) | API-SEC-018 | 409 | duplicate module code | رمز الوحدة مستخدم بالفعل | Module code already in use |
| SEC-409-MODULE-NOT-REGISTERED | RULE-SEC-004 | API-SEC-019 | 409 | screen registered under an unregistered module | الوحدة غير مسجّلة | Module is not registered |
| SEC-409-SCREEN-DUP | PLATFORM-STD (uniqueness) | API-SEC-019 | 409 | duplicate page code | رمز الصفحة مستخدم بالفعل | Page code already in use |
| SEC-409-SCREEN-NOT-REGISTERED | PLATFORM-STD (referential) | API-SEC-020 | 409 | action registered under an unregistered screen | الشاشة غير مسجّلة | Screen is not registered |
| SEC-409-ACTION-DUP | PLATFORM-STD (uniqueness) | API-SEC-020 | 409 | duplicate permission code | رمز الصلاحية مستخدم بالفعل | Permission code already in use |
| SEC-409-RESET-TOKEN-INVALID | RULE-SEC-006 | API-SEC-004 | 409 | expired or used reset token | رابط إعادة التعيين غير صالح أو منتهي | This reset link is invalid or has expired |
| SEC-409-SIGNUP-DUP | PLATFORM-STD (uniqueness) | API-SEC-002 | 409 | duplicate pending/registered email | البريد الإلكتروني مستخدم بالفعل | This email is already in use |
| SEC-404-SESSION | PLATFORM-STD (not found) | API-SEC-026 | 404 | unknown session id | الجلسة غير موجودة | Session not found |
| SEC-409-ALREADY-TERMINATED | PLATFORM-STD (lifecycle) | API-SEC-026 | 409 | session already terminated | هذه الجلسة منتهية بالفعل | This session is already terminated |
| SEC-403-FORBIDDEN | PLATFORM-STD (RULE-SEC-007 + REQ-SEC-033, CORE interceptor) | every secured API | 403 | missing module/screen/action grant | غير مصرح بهذا الإجراء | You are not authorized to perform this action |
| SEC-400-INVALID-SORT | PLATFORM-STD (search contract) | every search API | 400 | unrecognized `sort` field | حقل الترتيب غير معروف | Unrecognized sort field |
| INTERNAL_ERROR | PLATFORM-STD (infrastructure, shared GlobalExceptionHandler — not module-scoped) | any | 500 | unhandled server error | حدث خطأ غير متوقع. يرجى المحاولة لاحقاً. | An unexpected error occurred. Please try again later. |

`SEC-401-INVALID-CREDENTIALS` and every other PLATFORM-STD row is a standard infrastructure
error (not-found / conflict / server / forbidden), not sourced from a specific SRS RULE — per
engine §7 these carry `RULE = PLATFORM-STD` and are covered by a single umbrella note rather
than 20 near-identical ADRs: **ADR-SEC-002** (below) records this once for the whole catalog.

## ADR-SEC-002 (new this stage)
See `erp/decisions/SEC/ADR-SEC-002.md` — PLATFORM-STD error rows (not-found, duplicate,
invalid-transition, forbidden, server, invalid-sort) are standard REST/CRUD infrastructure
errors implied by the SRS's own conventions (unique constraints, status lifecycles, the
module/screen/action gate) rather than restated as individual RULE-* ids; non-breaking.

## Alignment self-check (ALIGN) — SEC v1

```
TRACEABILITY      ✓ every API-*/QR-*/RULE-*/DBF-* used in a phase appears in the Plan Index; every PHASE/SUB/atom carries traces=; every traces target exists upstream (REQ-SEC-001..035, DBF-SEC-001..104 all defined in srs/db-script)
BINDING (§2A)     ⚠ no placeholder table/column/key object; every column cites a DBF (Field Registry + per-entity FIELDS tables); every RULE message present in ar+en; business code: none applicable (stated, not silently skipped). Two things this row did not catch: a real column-name mismatch (ENT-SEC-007's `grant_at` vs db-script's `granted_at`, DBF-SEC-064 — api_doc_gaps #2, since corrected), and the generation object it certifies is NOT what is built — the module ships 13 `SEQ_SEC_*` sequences + `GenerationType.SEQUENCE` per GOVERNANCE-RULES.md §Convention Precedence 1, while 13 BINDINGS lines and the Phase 1 type table still name `GENERATED ALWAYS AS IDENTITY`
MANIFEST (§4)     ✓ only the 4 mandated columns beyond DBF/ENT (property, type — status/XM added per engine format); all 104 DBF of every bound table listed; 0 ⏸ rows (0 XM)
QRC (§5)          ⚠ every API with a DB operation has ≥1 QR — re-verified, all 27 API atoms cite one; every QR entry carries the "logical spec, not code" framing (catalog header); no join for a lookup label. But the exact generation object named (`GENERATED ALWAYS AS IDENTITY`, Phase 1 CORE type table) is superseded as above. Separately, QR-SEC-025's declared query was dead code: `ActiveSessionRepository.findNonTerminated(Pageable)` had no caller after API-SEC-025 moved to a SpecBuilder-driven POST /search (A.2.9). CLOSED 2026-09-11: the plan declares `join NONE` for QR-SEC-025 (plan:792) and this catalog already registers it as FIND_BY_CRITERIA (plan:376), so the plan never required the JOIN FETCH — the orphan method was deleted and A.2.9 now holds across all 37 methods the module's 13 repositories declare
API (R3)          ⚠ every RULE in a Validations line has a catalog row (28 catalog rows = 27 module-owned rows, one per SecErrorCodes constant, all present in messages.properties AND messages_ar.properties, plus the 1 platform row INTERNAL_ERROR the shared GlobalExceptionHandler owns and SEC does not declare); platform errors carry RULE=PLATFORM-STD + ADR-SEC-002; create/update requests exclude PK/audit/system fields (DTO MEMBERSHIP, Phase 2; Request lines, Phase 3) — verified in code. But the check only tests field EXCLUSION, never whether a field an API NAMES is DEFINED, which is why it passed API-SEC-011 with the approved account's credential unspecified (api_doc_gaps #4) and API-SEC-022 with three Response figures undefined (api_doc_gaps #6). Separately `SEC-500`, named by 8 API blocks (not 9) as their only error, was never emitted — the shared GlobalExceptionHandler answers `INTERNAL_ERROR` and SecErrorCodes.SEC_500 had zero references. CLOSED 2026-09-11: those 8 Errors lines and the catalog row now name `INTERNAL_ERROR`, and `SecErrorCodes.SEC_500` plus both `SEC-500=` bundle keys were removed
CROSS-MODULE      ⚠ 0 XM rows from db-script, 0 placed, 0 mismatched — still exact, but only for the CONSUME direction and only at the SCHEMA level (db-script §2 XM REGISTER, as amended 2026-09-11: "None in the CONSUME direction"; no consumed entity, table or FK); inbound stub uses XM-INBOUND-STUB-1 notation, not TODO. At the API level SEC is not isolated in EITHER direction. CONSUMES: PasswordResetService injects `com.erp.notif.crossmodule.NotificationDispatchApi` (INT-C — srs-sec.md §A8's one declared SOFT integration, the module's only non-sec/non-common import). EXPOSES, new 2026-09-11: `com.erp.sec.crossmodule.SecUserDirectoryApi` — `findContact` (REQ-SEC-034) and `findUserIdsHoldingPermission` (REQ-SEC-035, QR-SEC-039) — implemented by `SecUserDirectoryApiImpl` delegating to `UserService`, injecting no repository, returning only the `UserContact` read-model and a list of ids; it registers no entity, table or column, so the XM row count legitimately stays 0 and the CONSUMING module's own P2 assigns any XM id for that direction. Both inbound gaps INT-R recorded are therefore CLOSED (this row previously read "two inbound contracts SEC does not satisfy"): XM-INBOUND-GAP-1 by `findUserIdsHoldingPermission`, XM-INBOUND-GAP-2 on both halves (`email` among the dispatch variables, plus `findContact`). MODULE-LEVEL CYCLE — stated here rather than discovered later: SEC already consumes NOTIF's `NotificationDispatchApi`, so the moment NOTIF adopts `SecUserDirectoryApi` the two modules point at each other at the MODULE level. That is NOT the circular dependency build-create-service forbids — neither crossmodule interface calls the other, and the two paths are independent (SEC→NOTIF at reset-token issuance; NOTIF→SEC at delivery, to resolve a bare recipientId) — but it is real, and must be weighed before any further surface is added in either direction
SECURITY (R7)     ⚠ every secured screen has a Phase 7 seed row — now checkable and true: all 9 page codes and all 13 permission codes the Phase 7 matrix names are seeded by V17__sec_security_seed.sql. Every secured API declares its PERM_* with two stated exceptions: API-SEC-027 has no page code of its own (SRS B4) and is gated `isAuthenticated()`, and API-SEC-001..004 are public by contract — 22 of 27 Security lines name a PERM_*, matching exactly 22 PERM_-based @PreAuthorize in com.erp.sec.service. profile.review.extra_checks ERP-4 ("every mutation endpoint declares its PERM_*") is FALSE as written: API-SEC-002, API-SEC-003 and API-SEC-004 are POST mutations writing SEC_SIGNUP_REQUEST / SEC_PWD_RESET_TOKEN / SEC_USER rows and state "public — no permission required". One seeded code, PERM_SEC_ROLES_DELETE, has no PermissionConstants constant (Phase 7 marks it reserved)
CORE (R1)         ✓ layers declared, domain placement declared (entity methods for single-entity, service for multi-row), error signalling declared (code format `SEC-<HTTP-status>-<SCENARIO>`, verified against all 27 SecErrorCodes values), type mapping declared (postgresql16 → Java table) — holds only after api_doc_gaps #1: as generated this row certified a `SEC-<3-digit-sequence>` format matching no catalog row and no emitted code
DECISIONS         ✗ ADR-SEC-001 (lookup centralization deferral) and ADR-SEC-002 (PLATFORM-STD catalog umbrella) are cited as ACCEPTED at `erp/decisions/SEC/ADR-SEC-00N.md` throughout P2/P3.1 and in modules/project-registry.md — NEITHER FILE EXISTS anywhere in this repository and there is no decisions/ directory (`find . -iname 'ADR-SEC-*'` returns nothing). Both decisions are stated inline in the artifacts citing them and nowhere else, so ACCEPTED is unevidenced. Correct on its own terms: no BLOCKED ADR
RESULT            FAILED ✗ — 11 findings, not 0, when this block was re-run at ALIGN-BE. Current state of governance/modules/SEC/execution-state.json, re-derived 2026-09-11 by reading all 13 `api_doc_gaps[]` resolution fields: 13 entries, re-derived again 2026-09-11 after the api-docs closure — 9 CLOSED (#1 and #2 by realignment; #5 both halves — the internal-caller authorization half, and the transaction-propagation half via NOTIF's `NotificationDispatchApi.dispatchIndependently` REQUIRES_NEW crossmodule entry point; #10 the dead QR-SEC-025 query, #11 the unreachable SEC-500 code, #12 the five stale GET rows in _SECTIONS.md's EXECUTION PLAN INDEX API table; plus, on the human-authorized amendment, #8 XM-INBOUND-GAP-1 — closed by `SecUserDirectoryApi.findUserIdsHoldingPermission` (REQ-SEC-035, QR-SEC-039), a crossmodule Spring interface and deliberately NOT a REST endpoint, since build-create-service requires "direct Spring interface injection, not loopback HTTP" — and #9 XM-INBOUND-GAP-2 on both halves, the delivery half by carrying `email` among the dispatch variables and the durable half by `SecUserDirectoryApi.findContact` (REQ-SEC-034); and #7 the missing producer for the published api-docs, closed by the per-module `GroupedOpenApi` beans in `src/main/java/com/erp/main/config/OpenApiConfig.java` plus an actual generator run, which wrote `index.md` and ten `endpoints/<group-slug>.md` files under `governance/modules/SEC/api-docs/`, and by striking `api-docs-sec.md` from the DOC-phase paragraph in favour of that layout) · 3 answered by an implementation choice with a human-only P1/P2 question left over (#3 RULE-SEC-005's absent conflicting-pair source, #4 the approved sign-up's credential, #6 the three undefined dashboard figures) · 0 partially closed · 1 open and needing a decision outside SEC (#13 the reset mail's unspecified `lang` and `actionLink`). Owed elsewhere, deliberately not written from this SEC-driven pass: NOTIF's own governance artifacts still need a matching cross-module contract entry for `dispatchIndependently`, and its `DefaultRecipientStatusReader` stub is NOTIF's to replace over `SecUserDirectoryApi` (governance/modules/NOTIF/** and src/main/java/com/erp/notif/** both left untouched); FIN's plan (`governance/modules/FIN/P3_1/backend-execution-plan-fin.md:383-386`) still describes the SoD read as going "through SEC's role/grant read APIs" — superseded wording that FIN's own pass must correct (governance/modules/FIN/** left untouched). Four of the ten rows above are only partly true and one is false. Master validation: the last full pass against the implemented module scored 122/133 applicable checks (91.7%), verdict CONDITIONAL, with 9 labelled deviations — recorded 2026-09-11 BEFORE the A.2.9 fix, and deliberately NOT re-derived at this closure. A.2.9 now passes where it then failed and its automatic-rejection trigger no longer fires; against that, API-SEC-025's page no longer uses a fetch join, so A.2.6 would have to be re-judged. No new score is asserted here — deriving one honestly means re-running the full 148-check pass. Per-check evidence, the N/A reasons and every deviation's justification: governance/project-artifacts/sec-alignment-report.md
```

**Coverage — ENT/DBF → phases → QR → XM**: every ENT-SEC-001..013 appears in exactly one
DATA-DOM entity block (SUB-MASTER or SUB-TRANSACTIONAL) with ≥1 QR cited under REPOSITORY
OPS; every DBF-SEC-001..104 appears in the DB Alignment Manifest and its owning entity's
FIELDS table; 0 XM (n/a).

**Coverage — RULE → API → catalog code**: RULE-SEC-001→API-SEC-016→SEC-409-NO-MODULE-GRANT ·
RULE-SEC-002→API-SEC-017→SEC-409-NO-SCREEN-GRANT · RULE-SEC-003→API-SEC-015→(no dedicated
code — success path with a count in the response; failure paths are SEC-404-GRANT) ·
RULE-SEC-004→API-SEC-019→SEC-409-MODULE-NOT-REGISTERED ·
RULE-SEC-005→API-SEC-008,017→SEC-409-SOD-CONFLICT · RULE-SEC-006→API-SEC-004→
SEC-409-RESET-TOKEN-INVALID · RULE-SEC-007→API-SEC-017→SEC-409-NO-VIEW-GRANT (+ the
CORE interceptor at runtime → SEC-403-FORBIDDEN).

**Coverage — XM → status → blocks → workaround**: not applicable (0 XM).

## QR id definitions (cross-reference index — full detail in Query Reference Catalog above)
**QR-SEC-001** — FIND_ONE user by username [ENT-SEC-001, API-SEC-001]
**QR-SEC-002** — SAVE signup request [ENT-SEC-013, API-SEC-002]
**QR-SEC-003** — SAVE password reset token [ENT-SEC-012, API-SEC-003]
**QR-SEC-004** — UPDATE user password + mark token used [ENT-SEC-001, ENT-SEC-012, API-SEC-004]
**QR-SEC-005** — FIND_BY_CRITERIA search users [ENT-SEC-001, API-SEC-005]
**QR-SEC-006** — SAVE create user [ENT-SEC-001, API-SEC-006]
**QR-SEC-007** — UPDATE user profile fields [ENT-SEC-001, API-SEC-007]
**QR-SEC-008** — SAVE assign roles to user [ENT-SEC-003, API-SEC-008]
**QR-SEC-009** — UPDATE deactivate user + terminate sessions [ENT-SEC-001, ENT-SEC-010, API-SEC-009]
**QR-SEC-010** — UPDATE reactivate user [ENT-SEC-001, API-SEC-010]
**QR-SEC-011** — UPDATE approve/reject signup [ENT-SEC-013, ENT-SEC-001, API-SEC-011]
**QR-SEC-012** — FIND_BY_CRITERIA search roles [ENT-SEC-002, API-SEC-012]
**QR-SEC-013** — SAVE create role [ENT-SEC-002, API-SEC-013]
**QR-SEC-014** — SAVE grant module [ENT-SEC-007, API-SEC-014]
**QR-SEC-015** — DELETE revoke module grant [ENT-SEC-007, API-SEC-015]
**QR-SEC-016** — SAVE grant screen [ENT-SEC-008, API-SEC-016]
**QR-SEC-017** — SAVE grant action [ENT-SEC-009, API-SEC-017]
**QR-SEC-018** — SAVE register module [ENT-SEC-004, API-SEC-018]
**QR-SEC-019** — SAVE register screen [ENT-SEC-005, API-SEC-019]
**QR-SEC-020** — SAVE register action [ENT-SEC-006, API-SEC-020]
**QR-SEC-021** — FIND_BY_CRITERIA browse registry tree [ENT-SEC-004, ENT-SEC-005, ENT-SEC-006, API-SEC-021]
**QR-SEC-022** — AGGREGATE dashboard figures [ENT-SEC-001, ENT-SEC-010, ENT-SEC-011, ENT-SEC-002, API-SEC-022]
**QR-SEC-023** — FIND_BY_CRITERIA search audit log [ENT-SEC-011, API-SEC-023]
**QR-SEC-024** — FIND_BY_CRITERIA audit log export [ENT-SEC-011, API-SEC-024]
**QR-SEC-025** — FIND_BY_CRITERIA non-terminated sessions [ENT-SEC-010, API-SEC-025]
**QR-SEC-026** — UPDATE terminate session [ENT-SEC-010, API-SEC-026]
**QR-SEC-027** — FIND_BY_CRITERIA effective menu tree [ENT-SEC-004, ENT-SEC-005, ENT-SEC-007, ENT-SEC-008, API-SEC-027]
**QR-SEC-028** — EXISTS role holds module grant (RULE-SEC-001) [ENT-SEC-007, API-SEC-016]
**QR-SEC-029** — EXISTS role holds screen grant (RULE-SEC-002) [ENT-SEC-008, API-SEC-017]
**QR-SEC-030** — EXISTS role holds VIEW on screen (RULE-SEC-007) [ENT-SEC-009, API-SEC-017]
**QR-SEC-031** — EXISTS conflicting action already held (RULE-SEC-005) [ENT-SEC-009, API-SEC-008, API-SEC-017]
**QR-SEC-032** — FIND_ALL dependent screen/action grants for cascade (RULE-SEC-003) [ENT-SEC-008, ENT-SEC-009, API-SEC-015]
**QR-SEC-033** — EXISTS uniqueness username/email [ENT-SEC-001, API-SEC-006]
**QR-SEC-034** — EXISTS uniqueness role code [ENT-SEC-002, API-SEC-013]
**QR-SEC-035** — EXISTS uniqueness module code [ENT-SEC-004, API-SEC-018]
**QR-SEC-036** — EXISTS module registered + uniqueness page code (RULE-SEC-004) [ENT-SEC-004, ENT-SEC-005, API-SEC-019]
**QR-SEC-037** — EXISTS screen exists + uniqueness permission code [ENT-SEC-005, ENT-SEC-006, API-SEC-020]
**QR-SEC-038** — EXISTS reset token unexpired and unused (RULE-SEC-006) [ENT-SEC-012, API-SEC-004]
**QR-SEC-039** — FIND_ALL DISTINCT user ids holding a permission code through an active role (REQ-SEC-035) [ENT-SEC-001, ENT-SEC-003, ENT-SEC-006, ENT-SEC-009, no API — `SecUserDirectoryApi`]

## Registry content
See `registry-exec-be-sec.md`.
══════════════════════════════════════════════════════════════════
