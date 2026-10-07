# BACKEND EXECUTION PLAN — الأمان / Security (SEC) — DELTA v2
══════════════════════════════════════════════════════════════════
Module : SEC   Version : v2 (delta on v1 — change set CS-SEC-001, ADDITIVE)   Profile : erp   Dialect : postgresql16
Framework : spring-boot-java (profile.stack.backend.framework)
Inputs : srs, db-script, registry-srs, registry-db (all v2 current state, `_state/`); change-manifest CS-SEC-001
Open ADRs : 9 raised by this stage, all ACCEPTED (non-breaking) — decisions/SEC/ADR-SEC-036.md, decisions/SEC/ADR-SEC-040.md,
            decisions/SEC/ADR-SEC-037.md (§4 amended — governs API-SEC-001's one-dummy-verification only, ADR-SEC-045),
            decisions/SEC/ADR-SEC-038.md, decisions/SEC/ADR-SEC-042.md, decisions/SEC/ADR-SEC-043.md (§2 SUPERSEDED
            by ADR-SEC-045 — no 429 code, throttling stays at ingress per ADR-SEC-044), decisions/SEC/ADR-SEC-044.md,
            decisions/SEC/ADR-SEC-045.md (revise pass, G1).
            ADR-SEC-039.md is SUPERSEDED by ADR-SEC-040 (G4).
            Carried: ADR-SEC-001, ADR-SEC-002, ADR-SEC-006. Applied from upstream v2: ADR-SEC-012 … ADR-SEC-030,
            ADR-SEC-032, ADR-SEC-033, ADR-SEC-034 (which supersedes ADR-SEC-031), ADR-SEC-035. Applied from P2:
            ADR-SEC-041. Applied from P1 (revise pass): ADR-SEC-046 (G4), ADR-SEC-047 (G6). BLOCKED: none.
Delta  : ADDED — the service-account credential endpoints (API-SEC-028 list, API-SEC-029 issue, API-SEC-030
         revoke), the client-credentials token endpoint (API-SEC-031), five endpoints the SRS screens demand and
         the v1 plan never declared (API-SEC-032 … API-SEC-036, ADR-SEC-038), queries QR-SEC-040 … QR-SEC-054,
         the entity block of ENT-SEC-014.
         MODIFIED in substance — API-SEC-001 (no interactive login for a service account), API-SEC-003 (no reset
         token for one), API-SEC-005 (principal-type filter and column), API-SEC-006 (service-account creation),
         the update of API-SEC-007 (principal type fixed), API-SEC-008 (audit target), API-SEC-009 and API-SEC-010
         (effect on credentials), API-SEC-011 (HUMAN on approval; locking read), API-SEC-022 (users overview per
         principal type), the CORE bearer check (per-request live state of a service-account token); every PK
         binding (identity → sequence, ADR-SEC-035). Every other v1 block in annotation only (ADR-SEC-036).
         REMOVED — none.
══════════════════════════════════════════════════════════════════

Delta reading note. This file is the complete current plan, not a fragment. `gov.py state` folds a delta plan
by carrying only a phase's DIRECT children, and nothing outside a marker is carried at all (index, manifest,
catalog, self-check, id lists). Every phase is therefore re-emitted in full, v1 content restated and v2 content
marked. ADR-SEC-036 records why the v1 blocks were brought to the current P3.1 template in the same pass: the
contracts that read that template were added after v1 passed its gate, and a v2 gate reads the whole folded plan.

## PRE-GENERATION EXTRACTION — SEC v2 (working set; not part of the plan proper)

```
── FROM srs ──────────────────────────────────────────────────────────────
ENTITIES      14 — ENT-SEC-001..014, all kind=security. v2 ADDED ENT-SEC-014
              ServiceAccountCredential; MODIFIED ENT-SEC-001 (principalTypeCode; the meaning of
              email and passwordHash for a SERVICE principal, ADR-SEC-032)
REQUIREMENTS  79 — REQ-SEC-001..079 (v2 ADDED 036..079; REQ-SEC-079 added by the pass-1 revise
              review, G6), each with ≥1 AC-SEC-* (AC-SEC-001..085)
RULES         13 — RULE-SEC-001..013 (v2 ADDED 008..013); full text restated below where enforced
SCREENS       10 — SCR-REQ-SEC-001..010; v2 MODIFIED 004 (service accounts, credentials tab) and
              007 (users overview per principal type); no screen added
PERMISSIONS   SEC_PAGES page codes + PERM_<PAGE_CODE>_<ACTION>, gateway VIEW. v2 adds no page and
              no action: credentials are listed under SEC_USERS VIEW, issued and revoked under
              SEC_USERS UPDATE (SCR-REQ-SEC-004 B4). The token operation has no screen (SRS A8)
LOOKUPS       USER_STATUS, SIGNUP_STATUS, AUDIT_EVENT_TYPE (v2: 21 codes — SERVICE_ACCOUNT_REACTIVATED
              added by the pass-1 revise review, G6), PRINCIPAL_TYPE (v2 ADDED:
              HUMAN, SERVICE) — CHECK-constrained code columns (ADR-SEC-001)
BUSINESS CODE none — no SEC entity carries a platform-numbered code (SRS §3.3 test: all "No")
── FROM db-script ────────────────────────────────────────────────────────
TABLES        14 — v2 ADDED SEC_SVC_ACCOUNT_CRED; SEC_USER altered (principal_type_code)
PK GENERATION strategy `sequence`: one SEQ_{TABLE} per table, PK a plain BIGINT filled by the
              application. v2 BLOCK 1 creates all 14 (the 13 v1 identity columns are migrated,
              ADR-SEC-035)
COLUMNS       116 DBF (v2 ADDED 105..116; MODIFIED 001, 002, 003, 004, 007, 014, 025, 030, 039,
              049, 060, 065, 070, 075, 083, 084, 091, 097)
CONSTRAINTS   v2: PK_SEC_SVC_ACCOUNT_CRED, FK_SVC_ACCOUNT_CRED_USER, CHK_SEC_USER_PRINCIPAL_TYPE,
              CHK_SEC_USER_PRINCIPAL_STATUS, CHK_SEC_SVC_ACCOUNT_CRED_REVOKED_BY,
              CHK_SEC_AUDIT_LOG_EVENT_TYPE (re-created, 21 codes). Indexes IDX_SEC_USER_PRINCIPAL_TYPE,
              IDX_SEC_SVC_ACCOUNT_CRED_USER. v1 constraint names as bound in v1, unchanged
XM            none — SEC is ROOT (db-script §2); SEC_SVC_ACCOUNT_CRED.user_id is intra-module
── FROM registries ───────────────────────────────────────────────────────
SHARED ENTITIES CONSUMED   none (SEC is ROOT)
EXISTING LOOKUP KEYS       the three v1 keys are reused; PRINCIPAL_TYPE is SEC-owned, no duplicate
ID RANGES already used     API-SEC-001..027 · QR-SEC-001..039 (QR-SEC-039 was minted by the
                           2026-09-11 amendment and never reached the v1 registry) — v2 continues at
                           API-SEC-028 and QR-SEC-040
──────────────────────────────────────────────────────────────────────────
No row required §2A.3 extraction-failure handling.
```

## EXECUTION PLAN INDEX — SEC v2 — backend-execution-plan-sec.md
Profile: erp · dialect: postgresql16 · framework: spring-boot-java
Open ADRs: 9 new (ADR-SEC-036, ADR-SEC-037, ADR-SEC-038, ADR-SEC-040, ADR-SEC-042, ADR-SEC-043 [§2 SUPERSEDED by ADR-SEC-045], ADR-SEC-044, ADR-SEC-045; ADR-SEC-039 SUPERSEDED by ADR-SEC-040), applied from P2: ADR-SEC-041, applied from P1: ADR-SEC-046, ADR-SEC-047, none BLOCKED — decisions/SEC/

**ENTITY REGISTRY**
| ENT | Name | Table | Business code | Operations (security actions — the endpoints are in each entity block) |
|---|---|---|---|---|
| ENT-SEC-001 | User (v2: HUMAN and SERVICE principals) | SEC_USER | none | VIEW · CREATE · UPDATE (update, assign roles, activate, deactivate, approve a sign-up) |
| ENT-SEC-002 | Role | SEC_ROLE | none | VIEW · CREATE · UPDATE |
| ENT-SEC-003 | UserRoleAssignment | SEC_USER_ROLE | none | VIEW · UPDATE (assign with the user) |
| ENT-SEC-004 | ModuleRegistry | SEC_MODULE_REG | none | VIEW · UPDATE (register) |
| ENT-SEC-005 | ScreenRegistry | SEC_SCREEN_REG | none | VIEW · UPDATE (register) |
| ENT-SEC-006 | ActionRegistry | SEC_ACTION_REG | none | VIEW · UPDATE (register) |
| ENT-SEC-007 | RoleModuleGrant | SEC_ROLE_MODULE_GRANT | none | VIEW · UPDATE (grant, revoke) |
| ENT-SEC-008 | RoleScreenGrant | SEC_ROLE_SCREEN_GRANT | none | VIEW · UPDATE (grant) |
| ENT-SEC-009 | RoleActionGrant | SEC_ROLE_ACTION_GRANT | none | VIEW · UPDATE (grant) |
| ENT-SEC-010 | ActiveSession | SEC_ACTIVE_SESSION | none | VIEW · DELETE (terminate) |
| ENT-SEC-011 | AuditLogEntry | SEC_AUDIT_LOG | none | VIEW (search, export) |
| ENT-SEC-012 | PasswordResetToken | SEC_PWD_RESET_TOKEN | none | CREATE · UPDATE (public, pre-authentication) |
| ENT-SEC-013 | SignupRequest | SEC_SIGNUP_REQUEST | none | CREATE (public) · VIEW · UPDATE (approve, reject) |
| ENT-SEC-014 | ServiceAccountCredential (v2) | SEC_SVC_ACCOUNT_CRED | none | VIEW · UPDATE (issue, revoke); read by the token operation |

**FIELD REGISTRY** — the DB Alignment Manifest below is the single binding (one row per DBF: property, language
type, status). Read-only in every request: each PK, each audit column, passwordHash (never serialized), statusCode
and isActiveFl (moved only by the lifecycle endpoints), lastLoginAt, every system timestamp and actor column,
secretHash (write-once, never serialized), lastUsedAt, revokedAt, revokedBy. v2: principalTypeCode is writable on
create and read-only afterwards (RULE-SEC-012).

**API REGISTRY**
| API | Operation | Verb | Path | ENT | Traces (REQ) |
|---|---|---|---|---|---|
| API-SEC-001 | login (v2: no SERVICE principal) | POST | /api/v1/sec/auth/login | ENT-SEC-001, ENT-SEC-010 | REQ-SEC-001, REQ-SEC-002, REQ-SEC-063 |
| API-SEC-002 | submit sign-up | POST | /api/v1/sec/auth/signup | ENT-SEC-013 | REQ-SEC-003 |
| API-SEC-003 | request password reset (v2: no token for a SERVICE principal) | POST | /api/v1/sec/auth/password-reset/request | ENT-SEC-012, ENT-SEC-001 | REQ-SEC-006, REQ-SEC-029, REQ-SEC-064, REQ-SEC-077 |
| API-SEC-004 | complete password reset | POST | /api/v1/sec/auth/password-reset/complete | ENT-SEC-012, ENT-SEC-001 | REQ-SEC-007, REQ-SEC-008 |
| API-SEC-005 | search users (v2: principal type) | POST | /api/v1/sec/users/search | ENT-SEC-001 | REQ-SEC-009, REQ-SEC-040, REQ-SEC-041 |
| API-SEC-006 | create user (v2: or a service account) | POST | /api/v1/sec/users | ENT-SEC-001 | REQ-SEC-009, REQ-SEC-036, REQ-SEC-037, REQ-SEC-070 |
| API-SEC-007 | update user (v2: principal type fixed) | PUT | /api/v1/sec/users/{id} | ENT-SEC-001 | REQ-SEC-009, REQ-SEC-039 |
| API-SEC-008 | assign roles to user | PUT | /api/v1/sec/users/{id}/roles | ENT-SEC-001, ENT-SEC-003, ENT-SEC-002 | REQ-SEC-010, REQ-SEC-043, REQ-SEC-069 |
| API-SEC-009 | deactivate user (v2: every credential and token of a service account rejected) | DELETE | /api/v1/sec/users/{id} | ENT-SEC-001, ENT-SEC-010 | REQ-SEC-011, REQ-SEC-060, REQ-SEC-071 |
| API-SEC-010 | activate (reactivate) user | PATCH | /api/v1/sec/users/{id} | ENT-SEC-001 | REQ-SEC-031, REQ-SEC-062, REQ-SEC-079 |
| API-SEC-011 | approve / reject sign-up (v2: HUMAN) | PATCH | /api/v1/sec/signup-requests/{id} | ENT-SEC-013, ENT-SEC-001 | REQ-SEC-004, REQ-SEC-005, REQ-SEC-038 |
| API-SEC-012 | search roles | POST | /api/v1/sec/roles/search | ENT-SEC-002 | REQ-SEC-012 |
| API-SEC-013 | create role | POST | /api/v1/sec/roles | ENT-SEC-002 | REQ-SEC-012 |
| API-SEC-014 | grant module to role | POST | /api/v1/sec/roles/{id}/modules | ENT-SEC-007, ENT-SEC-002 | REQ-SEC-012 |
| API-SEC-015 | revoke module grant | DELETE | /api/v1/sec/roles/{id}/modules/{moduleId} | ENT-SEC-007, ENT-SEC-008, ENT-SEC-009 | REQ-SEC-015 |
| API-SEC-016 | grant screen to role | POST | /api/v1/sec/roles/{id}/screens | ENT-SEC-008, ENT-SEC-002 | REQ-SEC-013 |
| API-SEC-017 | grant action to role | POST | /api/v1/sec/roles/{id}/actions | ENT-SEC-009, ENT-SEC-002 | REQ-SEC-014, REQ-SEC-020, REQ-SEC-030, REQ-SEC-044 |
| API-SEC-018 | register module | POST | /api/v1/sec/registry/modules | ENT-SEC-004 | REQ-SEC-016 |
| API-SEC-019 | register screen | POST | /api/v1/sec/registry/screens | ENT-SEC-005 | REQ-SEC-017, REQ-SEC-018 |
| API-SEC-020 | register action | POST | /api/v1/sec/registry/actions | ENT-SEC-006 | REQ-SEC-019 |
| API-SEC-021 | search registry | POST | /api/v1/sec/registry/search | ENT-SEC-004, ENT-SEC-005, ENT-SEC-006 | REQ-SEC-016 |
| API-SEC-022 | dashboard summary (v2: users overview per principal type) | GET | /api/v1/sec/dashboard | ENT-SEC-001, ENT-SEC-002, ENT-SEC-010, ENT-SEC-011 | REQ-SEC-022, REQ-SEC-023, REQ-SEC-042 |
| API-SEC-023 | search audit log | POST | /api/v1/sec/audit-log/search | ENT-SEC-011 | REQ-SEC-025 |
| API-SEC-024 | export audit log | GET | /api/v1/sec/audit-log/export | ENT-SEC-011 | REQ-SEC-026 |
| API-SEC-025 | list active sessions | POST | /api/v1/sec/sessions/search | ENT-SEC-010 | REQ-SEC-027 |
| API-SEC-026 | terminate session | DELETE | /api/v1/sec/sessions/{id} | ENT-SEC-010 | REQ-SEC-028 |
| API-SEC-027 | effective menu | GET | /api/v1/sec/menu | ENT-SEC-004, ENT-SEC-005, ENT-SEC-007, ENT-SEC-008 | REQ-SEC-021, REQ-SEC-032, REQ-SEC-033, REQ-SEC-044, REQ-SEC-045 |
| API-SEC-028 | list a service account's credentials (v2) | GET | /api/v1/sec/users/{id}/credentials | ENT-SEC-014, ENT-SEC-001 | REQ-SEC-053, REQ-SEC-056 |
| API-SEC-029 | issue a credential (v2) | POST | /api/v1/sec/users/{id}/credentials | ENT-SEC-014, ENT-SEC-001 | REQ-SEC-051, REQ-SEC-052, REQ-SEC-054, REQ-SEC-067, REQ-SEC-078 |
| API-SEC-030 | revoke a credential (v2) | DELETE | /api/v1/sec/users/{id}/credentials/{credentialId} | ENT-SEC-014 | REQ-SEC-058, REQ-SEC-061, REQ-SEC-068 |
| API-SEC-031 | issue access token — client credentials (v2) | POST | /api/v1/sec/auth/token | ENT-SEC-001, ENT-SEC-014 | REQ-SEC-046 … REQ-SEC-050, REQ-SEC-055, REQ-SEC-057, REQ-SEC-059, REQ-SEC-065, REQ-SEC-066 |
| API-SEC-032 | read user (v2 declared, ADR-SEC-038) | GET | /api/v1/sec/users/{id} | ENT-SEC-001, ENT-SEC-003 | REQ-SEC-072 (G5) |
| API-SEC-033 | read role (v2 declared) | GET | /api/v1/sec/roles/{id} | ENT-SEC-002 | REQ-SEC-073 (G5) |
| API-SEC-034 | update role (v2 declared) | PUT | /api/v1/sec/roles/{id} | ENT-SEC-002 | REQ-SEC-074 (G5) |
| API-SEC-035 | read a role's grant tree (v2 declared) | GET | /api/v1/sec/roles/{id}/grants | ENT-SEC-007, ENT-SEC-008, ENT-SEC-009 | REQ-SEC-075 (G5) |
| API-SEC-036 | search sign-up requests (v2 declared) | POST | /api/v1/sec/signup-requests/search | ENT-SEC-013 | REQ-SEC-076 (G5) |
| API-SEC-037 | read a user's contact details across modules (v2, ADR-SEC-040) | — (no HTTP surface) | in-process `SecUserDirectoryApi.findContact` | ENT-SEC-001 | REQ-SEC-034 |
| API-SEC-038 | read the holders of a permission code across modules (v2, ADR-SEC-040) | — (no HTTP surface) | in-process `SecUserDirectoryApi.findUserIdsHoldingPermission` | ENT-SEC-001, ENT-SEC-003, ENT-SEC-006, ENT-SEC-009 | REQ-SEC-035 |
| API-SEC-039 | is this module code registered? (v2, ADR-SEC-040) | — (no HTTP surface) | in-process `SecModuleRegistryApi` | ENT-SEC-004 | REQ-SEC-016, REQ-SEC-017 |

**RULE REGISTRY** — scope, where each rule is enforced and the catalog row that carries its message; the ar/en text
of every rule is inline in the entity block that owns it and in the endpoint that enforces it.
| RULE | Name | Scope | ar/en message | Enforced by | Catalog code |
|---|---|---|---|---|---|
| RULE-SEC-001 | no screen grant without its module grant | ENT-SEC-008 | ✓ / ✓ | API-SEC-016 | SEC-409-NO-MODULE-GRANT |
| RULE-SEC-002 | no action grant without its screen grant | ENT-SEC-009 | ✓ / ✓ | API-SEC-017 | SEC-409-NO-SCREEN-GRANT |
| RULE-SEC-003 | cascade revoke on module-grant removal | ENT-SEC-007 | ✓ / ✓ (informational) | API-SEC-015 | — (success path; failure is SEC-404-GRANT) |
| RULE-SEC-004 | no screen under an unregistered module | ENT-SEC-005 | ✓ / ✓ | API-SEC-019 | SEC-409-MODULE-NOT-REGISTERED |
| RULE-SEC-005 | no conflicting actions on one user | ENT-SEC-003, ENT-SEC-009 | ✓ / ✓ | API-SEC-008, API-SEC-017 (inert — the conflicting-pair set is empty) | SEC-409-SOD-CONFLICT |
| RULE-SEC-006 | reject an expired or used reset token | ENT-SEC-012 | ✓ / ✓ | API-SEC-004 | SEC-409-RESET-TOKEN-INVALID |
| RULE-SEC-007 | VIEW is the screen-level gateway | ENT-SEC-009 | ✓ / ✓ | API-SEC-017 (grant) and the CORE gate (every request, every principal) | SEC-409-NO-VIEW-GRANT, SEC-403-FORBIDDEN |
| RULE-SEC-008 | credentials only for an active service account (v2) | ENT-SEC-014 | ✓ / ✓ | API-SEC-029 | SEC-409-NOT-ACTIVE-SERVICE-ACCOUNT |
| RULE-SEC-009 | machine authentication only by an active credential of an active service account (v2) | ENT-SEC-014, ENT-SEC-001 | ✓ / ✓ | API-SEC-031 and the CORE bearer check | SEC-401-INVALID-CLIENT |
| RULE-SEC-010 | no interactive use of a service account (v2) | ENT-SEC-001, ENT-SEC-010, ENT-SEC-012 | ✓ / ✓ (login; the reset request answers the generic confirmation) | API-SEC-001, API-SEC-003 | SEC-401-INVALID-CREDENTIALS |
| RULE-SEC-011 | no revoking an already revoked credential (v2) | ENT-SEC-014 | ✓ / ✓ | API-SEC-030 | SEC-409-CREDENTIAL-ALREADY-REVOKED |
| RULE-SEC-012 | principal type fixed after creation (v2) | ENT-SEC-001 | ✓ / ✓ | API-SEC-007 | SEC-409-PRINCIPAL-TYPE-FIXED |
| RULE-SEC-013 | active credential limit | ENT-SEC-014 | ✓ / ✓ | API-SEC-029 | SEC-409-CREDENTIAL-LIMIT |

**SCREEN REGISTRY** — one composite screen per SCR-REQ, one SEC_PAGES row each (SEC-BE).
| Screen | Page code | Type | ENT | Permission names |
|---|---|---|---|---|
| SCR-REQ-SEC-001 Login | SEC_LOGIN | public | ENT-SEC-001, ENT-SEC-010 | none — public |
| SCR-REQ-SEC-002 Sign-up | SEC_SIGNUP | public | ENT-SEC-013 | none — public |
| SCR-REQ-SEC-003 Forgot / reset password | SEC_PWD_RESET | public (wizard) | ENT-SEC-012, ENT-SEC-001 | none — public |
| SCR-REQ-SEC-004 Users (v2 MODIFIED) | SEC_USERS | Search + Entry | ENT-SEC-001, ENT-SEC-002, ENT-SEC-003, ENT-SEC-013, ENT-SEC-014 | PERM_SEC_USERS_VIEW, PERM_SEC_USERS_CREATE, PERM_SEC_USERS_UPDATE |
| SCR-REQ-SEC-005 Roles & permissions | SEC_ROLES | Master + Detail | ENT-SEC-002, ENT-SEC-004 … ENT-SEC-009 | PERM_SEC_ROLES_VIEW, PERM_SEC_ROLES_CREATE, PERM_SEC_ROLES_UPDATE, PERM_SEC_ROLES_DELETE |
| SCR-REQ-SEC-006 Module / screen / action registry | SEC_MODULE_REGISTRY | Master + Detail | ENT-SEC-004, ENT-SEC-005, ENT-SEC-006 | PERM_SEC_MODULE_REGISTRY_VIEW, PERM_SEC_MODULE_REGISTRY_UPDATE |
| SCR-REQ-SEC-007 Admin dashboard (v2 MODIFIED) | SEC_DASHBOARD | dashboard | ENT-SEC-001, ENT-SEC-002, ENT-SEC-010, ENT-SEC-011 | PERM_SEC_DASHBOARD_VIEW (+ each widget's source-screen VIEW) |
| SCR-REQ-SEC-008 Audit log | SEC_AUDIT_LOG | search list | ENT-SEC-011 | PERM_SEC_AUDIT_LOG_VIEW |
| SCR-REQ-SEC-009 Active sessions | SEC_SESSIONS | search list | ENT-SEC-010 | PERM_SEC_SESSIONS_VIEW, PERM_SEC_SESSIONS_DELETE |
| SCR-REQ-SEC-010 Dynamic menu | (none) | global component | ENT-SEC-004, ENT-SEC-005, ENT-SEC-007, ENT-SEC-008 | none of its own |

**LOOKUP REGISTRY** — SEC-owned keys stored as CHECK-constrained code columns (ADR-SEC-001); every API returns and
accepts the code as a plain String; labels come from SRS A6 through the frontend's single resolver until the lookup
module serves SEC's keys (ADR-SEC-006).
| Lookup key | Used in field (`DBF-*`) | `ENT-*` | Table owner module |
|---|---|---|---|
| USER_STATUS | DBF-SEC-007 | ENT-SEC-001 | SEC (CHECK constraint, no table) |
| SIGNUP_STATUS | DBF-SEC-102 | ENT-SEC-013 | SEC (CHECK constraint, no table) |
| AUDIT_EVENT_TYPE | DBF-SEC-084 | ENT-SEC-011 | SEC (CHECK constraint, no table) |
| PRINCIPAL_TYPE | DBF-SEC-105 | ENT-SEC-001 | SEC (CHECK constraint, no table) |

**QRC SUMMARY** — QR-SEC-001 … QR-SEC-054; v2 adds QR-SEC-040 … QR-SEC-054 (Query Reference Catalog below).

```
DB ALIGNMENT      the manifest below — aligned; every PK binding now names its sequence (ADR-SEC-035)
XM STATUS         0 deferred — SEC is ROOT and consumes no module's entity
SECURITY          7 secured screens + 3 public screens + 1 machine operation; grants per role
DBF ROWS          116
XM ROWS           0
API ROWS          39
QR ROWS           54
```

## DB Alignment Manifest — SEC v2
Canonical binding of plan properties to db-script fields. Column names, database types and SRS references are read
from the db-script by `DBF-*`, never repeated here. Status: ✓ aligned · ✗ type mismatch · ⏸ deferred XM. No row is
✗ or ⏸ (SEC has no XM). Language types come from CORE's type-mapping table. Every required column is written by an
endpoint's Request or Orchestration line (SVC-API); none needs an exclusion reason.

| DBF | ENT | plan property | plan type | XM | status |
|---|---|---|---|---|---|
| DBF-SEC-001 | ENT-SEC-001 | userPk | Long | — | ✓ from SEQ_SEC_USER |
| DBF-SEC-002 | ENT-SEC-001 | username | String | — | ✓ |
| DBF-SEC-105 | ENT-SEC-001 | principalTypeCode | String | — | ✓ v2 ADDED |
| DBF-SEC-003 | ENT-SEC-001 | email | String | — | ✓ |
| DBF-SEC-004 | ENT-SEC-001 | passwordHash | String | — | ✓ never serialized |
| DBF-SEC-005 | ENT-SEC-001 | fullNameAr | String | — | ✓ |
| DBF-SEC-006 | ENT-SEC-001 | fullNameEn | String | — | ✓ |
| DBF-SEC-007 | ENT-SEC-001 | statusCode | String | — | ✓ |
| DBF-SEC-008 | ENT-SEC-001 | lastLoginAt | Instant | — | ✓ |
| DBF-SEC-009 | ENT-SEC-001 | isActiveFl | Boolean | — | ✓ |
| DBF-SEC-010 | ENT-SEC-001 | createdBy | String | — | ✓ |
| DBF-SEC-011 | ENT-SEC-001 | createdAt | Instant | — | ✓ |
| DBF-SEC-012 | ENT-SEC-001 | updatedBy | String | — | ✓ |
| DBF-SEC-013 | ENT-SEC-001 | updatedAt | Instant | — | ✓ |
| DBF-SEC-014 | ENT-SEC-002 | rolePk | Long | — | ✓ from SEQ_SEC_ROLE |
| DBF-SEC-015 | ENT-SEC-002 | code | String | — | ✓ |
| DBF-SEC-016 | ENT-SEC-002 | nameAr | String | — | ✓ |
| DBF-SEC-017 | ENT-SEC-002 | nameEn | String | — | ✓ |
| DBF-SEC-018 | ENT-SEC-002 | descriptionAr | String | — | ✓ |
| DBF-SEC-019 | ENT-SEC-002 | descriptionEn | String | — | ✓ |
| DBF-SEC-020 | ENT-SEC-002 | isActiveFl | Boolean | — | ✓ |
| DBF-SEC-021 | ENT-SEC-002 | createdBy | String | — | ✓ |
| DBF-SEC-022 | ENT-SEC-002 | createdAt | Instant | — | ✓ |
| DBF-SEC-023 | ENT-SEC-002 | updatedBy | String | — | ✓ |
| DBF-SEC-024 | ENT-SEC-002 | updatedAt | Instant | — | ✓ |
| DBF-SEC-025 | ENT-SEC-003 | userRolePk | Long | — | ✓ from SEQ_SEC_USER_ROLE |
| DBF-SEC-026 | ENT-SEC-003 | userId | Long | — | ✓ |
| DBF-SEC-027 | ENT-SEC-003 | roleId | Long | — | ✓ |
| DBF-SEC-028 | ENT-SEC-003 | assignedBy | String | — | ✓ |
| DBF-SEC-029 | ENT-SEC-003 | assignedAt | Instant | — | ✓ |
| DBF-SEC-030 | ENT-SEC-004 | moduleRegPk | Long | — | ✓ from SEQ_SEC_MODULE_REG |
| DBF-SEC-031 | ENT-SEC-004 | code | String | — | ✓ |
| DBF-SEC-032 | ENT-SEC-004 | nameAr | String | — | ✓ |
| DBF-SEC-033 | ENT-SEC-004 | nameEn | String | — | ✓ |
| DBF-SEC-034 | ENT-SEC-004 | isActiveFl | Boolean | — | ✓ |
| DBF-SEC-035 | ENT-SEC-004 | createdBy | String | — | ✓ |
| DBF-SEC-036 | ENT-SEC-004 | createdAt | Instant | — | ✓ |
| DBF-SEC-037 | ENT-SEC-004 | updatedBy | String | — | ✓ |
| DBF-SEC-038 | ENT-SEC-004 | updatedAt | Instant | — | ✓ |
| DBF-SEC-039 | ENT-SEC-005 | screenRegPk | Long | — | ✓ from SEQ_SEC_SCREEN_REG |
| DBF-SEC-040 | ENT-SEC-005 | pageCode | String | — | ✓ |
| DBF-SEC-041 | ENT-SEC-005 | moduleId | Long | — | ✓ |
| DBF-SEC-042 | ENT-SEC-005 | nameAr | String | — | ✓ |
| DBF-SEC-043 | ENT-SEC-005 | nameEn | String | — | ✓ |
| DBF-SEC-044 | ENT-SEC-005 | isActiveFl | Boolean | — | ✓ |
| DBF-SEC-045 | ENT-SEC-005 | createdBy | String | — | ✓ |
| DBF-SEC-046 | ENT-SEC-005 | createdAt | Instant | — | ✓ |
| DBF-SEC-047 | ENT-SEC-005 | updatedBy | String | — | ✓ |
| DBF-SEC-048 | ENT-SEC-005 | updatedAt | Instant | — | ✓ |
| DBF-SEC-049 | ENT-SEC-006 | actionRegPk | Long | — | ✓ from SEQ_SEC_ACTION_REG |
| DBF-SEC-050 | ENT-SEC-006 | permissionCode | String | — | ✓ server-built from pageCode and actionCode |
| DBF-SEC-051 | ENT-SEC-006 | screenId | Long | — | ✓ |
| DBF-SEC-052 | ENT-SEC-006 | actionCode | String | — | ✓ |
| DBF-SEC-053 | ENT-SEC-006 | nameAr | String | — | ✓ |
| DBF-SEC-054 | ENT-SEC-006 | nameEn | String | — | ✓ |
| DBF-SEC-055 | ENT-SEC-006 | isActiveFl | Boolean | — | ✓ |
| DBF-SEC-056 | ENT-SEC-006 | createdBy | String | — | ✓ |
| DBF-SEC-057 | ENT-SEC-006 | createdAt | Instant | — | ✓ |
| DBF-SEC-058 | ENT-SEC-006 | updatedBy | String | — | ✓ |
| DBF-SEC-059 | ENT-SEC-006 | updatedAt | Instant | — | ✓ |
| DBF-SEC-060 | ENT-SEC-007 | roleModuleGrantPk | Long | — | ✓ from SEQ_SEC_ROLE_MODULE_GRANT |
| DBF-SEC-061 | ENT-SEC-007 | roleId | Long | — | ✓ |
| DBF-SEC-062 | ENT-SEC-007 | moduleId | Long | — | ✓ |
| DBF-SEC-063 | ENT-SEC-007 | grantedBy | String | — | ✓ |
| DBF-SEC-064 | ENT-SEC-007 | grantedAt | Instant | — | ✓ |
| DBF-SEC-065 | ENT-SEC-008 | roleScreenGrantPk | Long | — | ✓ from SEQ_SEC_ROLE_SCREEN_GRANT |
| DBF-SEC-066 | ENT-SEC-008 | roleId | Long | — | ✓ |
| DBF-SEC-067 | ENT-SEC-008 | screenId | Long | — | ✓ |
| DBF-SEC-068 | ENT-SEC-008 | grantedBy | String | — | ✓ |
| DBF-SEC-069 | ENT-SEC-008 | grantedAt | Instant | — | ✓ |
| DBF-SEC-070 | ENT-SEC-009 | roleActionGrantPk | Long | — | ✓ from SEQ_SEC_ROLE_ACTION_GRANT |
| DBF-SEC-071 | ENT-SEC-009 | roleId | Long | — | ✓ |
| DBF-SEC-072 | ENT-SEC-009 | actionId | Long | — | ✓ |
| DBF-SEC-073 | ENT-SEC-009 | grantedBy | String | — | ✓ |
| DBF-SEC-074 | ENT-SEC-009 | grantedAt | Instant | — | ✓ |
| DBF-SEC-075 | ENT-SEC-010 | activeSessionPk | Long | — | ✓ from SEQ_SEC_ACTIVE_SESSION |
| DBF-SEC-076 | ENT-SEC-010 | userId | Long | — | ✓ |
| DBF-SEC-077 | ENT-SEC-010 | tokenRef | String | — | ✓ never serialized |
| DBF-SEC-078 | ENT-SEC-010 | startedAt | Instant | — | ✓ |
| DBF-SEC-079 | ENT-SEC-010 | lastActivityAt | Instant | — | ✓ |
| DBF-SEC-080 | ENT-SEC-010 | ipAddress | String | — | ✓ |
| DBF-SEC-081 | ENT-SEC-010 | terminatedAt | Instant | — | ✓ |
| DBF-SEC-082 | ENT-SEC-010 | terminatedBy | String | — | ✓ |
| DBF-SEC-083 | ENT-SEC-011 | auditLogPk | Long | — | ✓ from SEQ_SEC_AUDIT_LOG |
| DBF-SEC-084 | ENT-SEC-011 | eventTypeCode | String | — | ✓ 21 codes in v2 (revise pass G6: +SERVICE_ACCOUNT_REACTIVATED) |
| DBF-SEC-085 | ENT-SEC-011 | actorUserId | Long | — | ✓ |
| DBF-SEC-086 | ENT-SEC-011 | occurredAt | Instant | — | ✓ |
| DBF-SEC-087 | ENT-SEC-011 | targetRef | String | — | ✓ |
| DBF-SEC-088 | ENT-SEC-011 | detailsAr | String | — | ✓ |
| DBF-SEC-089 | ENT-SEC-011 | detailsEn | String | — | ✓ |
| DBF-SEC-090 | ENT-SEC-011 | ipAddress | String | — | ✓ |
| DBF-SEC-091 | ENT-SEC-012 | pwdResetTokenPk | Long | — | ✓ from SEQ_SEC_PWD_RESET_TOKEN |
| DBF-SEC-092 | ENT-SEC-012 | userId | Long | — | ✓ |
| DBF-SEC-093 | ENT-SEC-012 | tokenHash | String | — | ✓ never serialized |
| DBF-SEC-094 | ENT-SEC-012 | requestedAt | Instant | — | ✓ |
| DBF-SEC-095 | ENT-SEC-012 | expiresAt | Instant | — | ✓ |
| DBF-SEC-096 | ENT-SEC-012 | usedAt | Instant | — | ✓ |
| DBF-SEC-097 | ENT-SEC-013 | signupRequestPk | Long | — | ✓ from SEQ_SEC_SIGNUP_REQUEST |
| DBF-SEC-098 | ENT-SEC-013 | email | String | — | ✓ |
| DBF-SEC-099 | ENT-SEC-013 | fullNameAr | String | — | ✓ |
| DBF-SEC-100 | ENT-SEC-013 | fullNameEn | String | — | ✓ |
| DBF-SEC-101 | ENT-SEC-013 | submittedAt | Instant | — | ✓ |
| DBF-SEC-102 | ENT-SEC-013 | statusCode | String | — | ✓ |
| DBF-SEC-103 | ENT-SEC-013 | reviewedBy | String | — | ✓ |
| DBF-SEC-104 | ENT-SEC-013 | reviewedAt | Instant | — | ✓ |
| DBF-SEC-106 | ENT-SEC-014 | serviceAccountCredentialPk | Long | — | ✓ v2 ADDED, from SEQ_SEC_SVC_ACCOUNT_CRED |
| DBF-SEC-107 | ENT-SEC-014 | secretHash | String | — | ✓ v2 ADDED, write-once, never serialized |
| DBF-SEC-108 | ENT-SEC-014 | description | String | — | ✓ v2 ADDED |
| DBF-SEC-109 | ENT-SEC-014 | lastUsedAt | Instant | — | ✓ v2 ADDED |
| DBF-SEC-110 | ENT-SEC-014 | revokedAt | Instant | — | ✓ v2 ADDED |
| DBF-SEC-111 | ENT-SEC-014 | revokedBy | String | — | ✓ v2 ADDED |
| DBF-SEC-112 | ENT-SEC-014 | userId | Long | — | ✓ v2 ADDED |
| DBF-SEC-113 | ENT-SEC-014 | createdBy | String | — | ✓ v2 ADDED, the issuing administrator |
| DBF-SEC-114 | ENT-SEC-014 | createdAt | Instant | — | ✓ v2 ADDED, the issuance time |
| DBF-SEC-115 | ENT-SEC-014 | updatedBy | String | — | ✓ v2 ADDED |
| DBF-SEC-116 | ENT-SEC-014 | updatedAt | Instant | — | ✓ v2 ADDED |

## Query Reference Catalog (QR-SEC-*)

> Logical specification only, never executable code. The implementer rewrites every entry with the real entity
> classes, mapped property names and the project's query strategy. The v1 entries stand as delivered unless a row
> says otherwise; the v2 entries and the modified ones carry the full §5 form below the summary table. Standard
> operation defaults (engine §5) apply to every entry that does not state its own. No entry joins to resolve a
> lookup label: every lookup-backed column is returned as its code.

| QR | Operation | API | Entity | Intent |
|---|---|---|---|---|
| QR-SEC-001 | FIND_ONE | API-SEC-001, API-SEC-031 | ENT-SEC-001 | the user a login or a client identifier names, by username — MODIFIED v2: also the token operation |
| QR-SEC-002 | SAVE | API-SEC-002 | ENT-SEC-013 | create a pending sign-up |
| QR-SEC-003 | SAVE | API-SEC-003 | ENT-SEC-012 | issue a reset token |
| QR-SEC-004 | UPDATE | API-SEC-004 | ENT-SEC-001, ENT-SEC-012 | set the new password hash and mark the token used — MODIFIED v2: the token is consumed by a conditional update |
| QR-SEC-005 | FIND_BY_CRITERIA | API-SEC-005 | ENT-SEC-001 | search users — MODIFIED v2: principalTypeCode filter and column |
| QR-SEC-006 | SAVE | API-SEC-006, API-SEC-011 | ENT-SEC-001 | create a user — MODIFIED v2: principal_type_code written, PK from SEQ_SEC_USER |
| QR-SEC-007 | UPDATE | API-SEC-007 | ENT-SEC-001 | update the user's profile fields |
| QR-SEC-008 | SAVE | API-SEC-008 | ENT-SEC-003 | replace a user's role assignments |
| QR-SEC-009 | UPDATE | API-SEC-009 | ENT-SEC-001, ENT-SEC-010 | deactivate a user and terminate its sessions |
| QR-SEC-010 | UPDATE | API-SEC-010 | ENT-SEC-001 | activate a disabled user |
| QR-SEC-011 | UPDATE | API-SEC-011 | ENT-SEC-013, ENT-SEC-001 | decide a sign-up — MODIFIED v2: locking read of the request; an approval creates a HUMAN user |
| QR-SEC-012 | FIND_BY_CRITERIA | API-SEC-012 | ENT-SEC-002 | search roles |
| QR-SEC-013 | SAVE | API-SEC-013 | ENT-SEC-002 | create a role |
| QR-SEC-014 | SAVE | API-SEC-014 | ENT-SEC-007 | grant a module |
| QR-SEC-015 | DELETE | API-SEC-015 | ENT-SEC-007 | revoke a module grant |
| QR-SEC-016 | SAVE | API-SEC-016 | ENT-SEC-008 | grant a screen |
| QR-SEC-017 | SAVE | API-SEC-017 | ENT-SEC-009 | grant an action |
| QR-SEC-018 | SAVE | API-SEC-018 | ENT-SEC-004 | register a module |
| QR-SEC-019 | SAVE | API-SEC-019 | ENT-SEC-005 | register a screen |
| QR-SEC-020 | SAVE | API-SEC-020 | ENT-SEC-006 | register an action |
| QR-SEC-021 | FIND_BY_CRITERIA | API-SEC-021 | ENT-SEC-004, ENT-SEC-005, ENT-SEC-006 | browse the registry tree |
| QR-SEC-022 | AGGREGATE | API-SEC-022 | ENT-SEC-001, ENT-SEC-002, ENT-SEC-010, ENT-SEC-011 | dashboard figures — MODIFIED v2: users overview grouped by principal type |
| QR-SEC-023 | FIND_BY_CRITERIA | API-SEC-023 | ENT-SEC-011 | search the audit log |
| QR-SEC-024 | FIND_BY_CRITERIA | API-SEC-024 | ENT-SEC-011 | audit rows for export |
| QR-SEC-025 | FIND_BY_CRITERIA | API-SEC-025 | ENT-SEC-010 | non-terminated sessions |
| QR-SEC-026 | UPDATE | API-SEC-026, API-SEC-009 | ENT-SEC-010 | terminate a session — MODIFIED v2: conditional update |
| QR-SEC-027 | FIND_BY_CRITERIA | API-SEC-027 | ENT-SEC-004, ENT-SEC-005, ENT-SEC-007, ENT-SEC-008 | the caller's effective grant tree (also the CORE gate's read) |
| QR-SEC-028 | EXISTS | API-SEC-016 | ENT-SEC-007 | RULE-SEC-001: the role holds the screen's module grant |
| QR-SEC-029 | EXISTS | API-SEC-017 | ENT-SEC-008 | RULE-SEC-002: the role holds the action's screen grant |
| QR-SEC-030 | EXISTS | API-SEC-017 | ENT-SEC-009 | RULE-SEC-007: the role holds VIEW on the action's screen |
| QR-SEC-031 | EXISTS | API-SEC-008, API-SEC-017 | ENT-SEC-009 | RULE-SEC-005: the user already holds the conflicting action (inert) |
| QR-SEC-032 | FIND_ALL | API-SEC-015 | ENT-SEC-008, ENT-SEC-009 | RULE-SEC-003: the dependent grants to cascade |
| QR-SEC-033 | EXISTS | API-SEC-006, API-SEC-007 | ENT-SEC-001 | uniqueness of username / email |
| QR-SEC-034 | EXISTS | API-SEC-013 | ENT-SEC-002 | uniqueness of the role code |
| QR-SEC-035 | EXISTS | API-SEC-018 | ENT-SEC-004 | uniqueness of the module code (also the exposed module-registry read, INT-R) |
| QR-SEC-036 | EXISTS | API-SEC-019 | ENT-SEC-004, ENT-SEC-005 | RULE-SEC-004: module registered; page code unique |
| QR-SEC-037 | EXISTS | API-SEC-020 | ENT-SEC-005, ENT-SEC-006 | screen registered; permission code unique |
| QR-SEC-038 | EXISTS | API-SEC-004 | ENT-SEC-012 | RULE-SEC-006: the token is unexpired and unused |
| QR-SEC-039 | FIND_ALL | API-SEC-038 (`SecUserDirectoryApi.findUserIdsHoldingPermission`, INT-R; ADR-SEC-040) | ENT-SEC-001, ENT-SEC-003, ENT-SEC-006, ENT-SEC-009 | REQ-SEC-035: user ids holding a permission code through an active role |
| QR-SEC-040 | FIND_ONE | API-SEC-007, API-SEC-008, API-SEC-009, API-SEC-010, API-SEC-028, API-SEC-032; every secured API (CORE bearer check) | ENT-SEC-001 | a user by PK (v2) |
| QR-SEC-041 | FIND_ONE | API-SEC-029 | ENT-SEC-001 | a user by PK under a write lock — RULE-SEC-008 (v2) |
| QR-SEC-042 | FIND_ALL | API-SEC-028 | ENT-SEC-014 | a service account's credentials, never the secret hash (v2) |
| QR-SEC-043 | SAVE | API-SEC-029 | ENT-SEC-014 | store a newly issued credential (v2) |
| QR-SEC-044 | FIND_ONE | API-SEC-030; every secured API (CORE bearer check) | ENT-SEC-014 | a credential by PK (v2) |
| QR-SEC-045 | UPDATE | API-SEC-030 | ENT-SEC-014 | revoke a credential only while it is unrevoked — RULE-SEC-011 (v2) |
| QR-SEC-046 | FIND_ALL | API-SEC-031, API-SEC-029 | ENT-SEC-014 | a service account's unrevoked credentials — RULE-SEC-009 (v2); also API-SEC-029's credential-count read (RULE-SEC-013) |
| QR-SEC-047 | UPDATE | API-SEC-031 | ENT-SEC-014 | record a credential's last use (v2) |
| QR-SEC-048 | SAVE | API-SEC-001, API-SEC-031; every secured API (CORE bearer check) | ENT-SEC-011 | append an audit entry on a rejection path, in its own transaction (v2) |
| QR-SEC-049 | FIND_ONE | API-SEC-003 | ENT-SEC-001 | a user by email (v2 — the reset request's lookup, stated) |
| QR-SEC-050 | FIND_ALL | API-SEC-032, API-SEC-008 | ENT-SEC-003, ENT-SEC-002 | a user's assigned roles (v2) |
| QR-SEC-051 | FIND_ONE | API-SEC-033, API-SEC-034, API-SEC-014, API-SEC-015, API-SEC-016, API-SEC-017 | ENT-SEC-002 | a role by PK; the grant-tree mutations take it under a write lock (v2) |
| QR-SEC-052 | UPDATE | API-SEC-034 | ENT-SEC-002 | update a role's names and descriptions (v2) |
| QR-SEC-053 | FIND_ALL | API-SEC-035 | ENT-SEC-004, ENT-SEC-005, ENT-SEC-006, ENT-SEC-007, ENT-SEC-008, ENT-SEC-009 | a role's grant tree (v2) |
| QR-SEC-054 | FIND_BY_CRITERIA | API-SEC-036 | ENT-SEC-013 | search sign-up requests (v2) |

#### QR-SEC-001 — the user a login or a client identifier names (MODIFIED v2)
Phase        : SVC-API (SVC-API-INT)
API          : API-SEC-001 — login; API-SEC-031 — issue access token
Entity       : ENT-SEC-001
Operation    : FIND_ONE
Intent       : the one user whose username equals the submitted login identity or client identifier (DEFAULT D2)
Logical spec : SELECT … FROM SEC_USER WHERE username = :username
Join         : NONE
Transaction  : READ_ONLY inside the caller's transaction
Locking      : NONE — the read decides nothing it then writes back; a concurrent deactivation is caught by the next request's live check
Pagination   : NO
Filters      : username EXACT (UQ_SEC_USER_USERNAME)
Result shape : full entity (passwordHash read for verification only, never serialized)
Null handling: no row → the caller's generic rejection, never a distinct "unknown user" answer

#### QR-SEC-004 — set the new password hash and consume the reset token (MODIFIED v2)
Phase        : SVC-API (SVC-API-INT)
API          : API-SEC-004 — complete password reset
Entity       : ENT-SEC-001, ENT-SEC-012
Operation    : UPDATE
Intent       : consume the token exactly once, then write the user's new password hash
Logical spec : UPDATE SEC_PWD_RESET_TOKEN SET used_at = now() WHERE pwd_reset_token_pk = :id AND used_at IS NULL AND expires_at > now(); on 1 row: UPDATE SEC_USER SET password_hash = :hash, updated_by, updated_at WHERE user_pk = :userId
Join         : NONE
Transaction  : READ_WRITE
Locking      : the conditional update is the guard — two submissions of one token both pass QR-SEC-038, only one update matches a row; 0 rows → SEC-409-RESET-TOKEN-INVALID
Pagination   : NO
Filters      : —
Result shape : affected-row count
Null handling: —

#### QR-SEC-005 — search users (MODIFIED v2)
Phase        : SVC-API (SVC-API-SEARCH)
API          : API-SEC-005 — search users
Entity       : ENT-SEC-001
Operation    : FIND_BY_CRITERIA
Intent       : the users matching the screen's filters, each with its principal type (REQ-SEC-040, REQ-SEC-041)
Logical spec : SELECT user_pk, username, email, full_name_ar, full_name_en, status_code, principal_type_code, last_login_at FROM SEC_USER WHERE [username LIKE :u OR email LIKE :u] AND [full_name_ar LIKE :n OR full_name_en LIKE :n] AND [status_code = :status] AND [principal_type_code = :principalType] ORDER BY :sort page :page size :size
Join         : NONE
Transaction  : READ_ONLY
Locking      : NONE
Pagination   : YES (Page<T>) — default size 20, max 200
Filters      : username/email LIKE · fullName LIKE · statusCode EXACT · principalTypeCode EXACT (v2, IDX_SEC_USER_PRINCIPAL_TYPE)
Result shape : projection — the columns above; never password_hash
Null handling: every filter optional; an empty page is success

#### QR-SEC-006 — create a user (MODIFIED v2)
Phase        : SVC-API (SVC-API-CRUD)
API          : API-SEC-006 — create user; API-SEC-011 — approve a sign-up
Entity       : ENT-SEC-001
Operation    : SAVE
Intent       : insert one user of the given principal type
Logical spec : INSERT INTO SEC_USER (user_pk ← nextval SEQ_SEC_USER, username, email, password_hash, full_name_ar, full_name_en, status_code, principal_type_code, is_active_fl, created_by, created_at) — flushed at once
Join         : NONE
Transaction  : READ_WRITE
Locking      : UQ_SEC_USER_USERNAME and UQ_SEC_USER_EMAIL decide a concurrent duplicate (API-SEC-006 Concurrency)
Pagination   : NO
Filters      : —
Result shape : full entity
Null handling: last_login_at NULL; principal_type_code always written, never left to the column default

#### QR-SEC-011 — decide a sign-up (MODIFIED v2)
Phase        : SVC-API (SVC-API-CRUD)
API          : API-SEC-011 — approve / reject sign-up
Entity       : ENT-SEC-013, ENT-SEC-001
Operation    : UPDATE
Intent       : move a PENDING request to APPROVED (creating a HUMAN user through QR-SEC-006) or REJECTED
Logical spec : SELECT … FROM SEC_SIGNUP_REQUEST WHERE signup_request_pk = :id FOR UPDATE; then UPDATE SEC_SIGNUP_REQUEST SET status_code = :decision, reviewed_by = :actor, reviewed_at = now() WHERE signup_request_pk = :id
Join         : NONE
Transaction  : READ_WRITE
Locking      : PESSIMISTIC_WRITE on the request row — a second decision waits, then sees a non-PENDING status and answers SEC-409-INVALID-TRANSITION
Pagination   : NO
Filters      : —
Result shape : full entity
Null handling: reviewed_by / reviewed_at set together

#### QR-SEC-022 — dashboard figures (MODIFIED v2)
Phase        : SVC-API (SVC-API-SEARCH)
API          : API-SEC-022 — dashboard summary
Entity       : ENT-SEC-001, ENT-SEC-002, ENT-SEC-010, ENT-SEC-011
Operation    : AGGREGATE
Intent       : six independent live figures; v2 splits the users overview by principal type (REQ-SEC-042)
Logical spec : users overview — SELECT principal_type_code, status_code, COUNT(*) FROM SEC_USER GROUP BY principal_type_code, status_code; the other five sub-queries as v1 (failed logins 24h from SEC_AUDIT_LOG, open sessions from SEC_ACTIVE_SESSION, recent activity, roles summary, onboarding funnel from SEC_SIGNUP_REQUEST)
Join         : NONE — each sub-query is single-table except the v1 roles summary (SEC_ROLE with SEC_USER_ROLE), unchanged
Transaction  : READ_ONLY
Locking      : NONE
Pagination   : NO
Filters      : —
Result shape : counts
Null handling: a principal type with no user is reported as 0, never omitted

#### QR-SEC-026 — terminate a session (MODIFIED v2)
Phase        : SVC-API (SVC-API-CRUD)
API          : API-SEC-026 — terminate session; API-SEC-009 — deactivate user (every open session of the user)
Entity       : ENT-SEC-010
Operation    : UPDATE
Intent       : end a session exactly once
Logical spec : UPDATE SEC_ACTIVE_SESSION SET terminated_at = now(), terminated_by = :actor WHERE active_session_pk = :id AND terminated_at IS NULL (API-SEC-009: WHERE user_id = :userId AND terminated_at IS NULL)
Join         : NONE
Transaction  : READ_WRITE
Locking      : the conditional update is the guard — 0 rows on API-SEC-026 → SEC-409-ALREADY-TERMINATED
Pagination   : NO
Filters      : —
Result shape : affected-row count
Null handling: a service account has no session rows, so API-SEC-009 updates 0 rows for it and that is success

#### QR-SEC-040 — a user by PK
Phase        : SVC-API (SVC-API-CRUD)
API          : API-SEC-007, API-SEC-008, API-SEC-009, API-SEC-010, API-SEC-028, API-SEC-032; every secured API through the CORE bearer check of a SERVICE token
Entity       : ENT-SEC-001
Operation    : FIND_ONE
Intent       : the user an endpoint's path or a service-account token's subject names
Logical spec : SELECT … FROM SEC_USER WHERE user_pk = :id [FOR UPDATE on API-SEC-008]
Join         : NONE
Transaction  : READ_ONLY (inside the caller's transaction when the caller writes); READ_WRITE on API-SEC-008
Locking      : NONE on every endpoint except API-SEC-008 — the endpoints that decide on the row and write it back state their own guard. On API-SEC-008, PESSIMISTIC_WRITE (the same FOR UPDATE form QR-SEC-041 uses) — the user row lock serializes two replacements of one user's role set, so the second replacement's read of the current set (QR-SEC-050) and its ROLE_ASSIGNED/ROLE_REVOKED diff wait for the first replacement to finish, and compute the diff from its result
Pagination   : NO
Filters      : user_pk EXACT
Result shape : full entity; password_hash never serialized
Null handling: no row → SEC-404-USER on an endpoint, SEC-401-INVALID-CLIENT in the bearer check

#### QR-SEC-041 — a user by PK under a write lock (RULE-SEC-008)
Phase        : SVC-API (SVC-API-CRUD)
API          : API-SEC-029 — issue a credential
Entity       : ENT-SEC-001
Operation    : FIND_ONE
Intent       : read the account's principal type and status and hold the row until the credential is stored
Logical spec : SELECT user_pk, principal_type_code, status_code FROM SEC_USER WHERE user_pk = :id FOR UPDATE
Join         : NONE
Transaction  : READ_WRITE (the issuance transaction)
Locking      : PESSIMISTIC_WRITE — a concurrent deactivation (API-SEC-009 updates the same row) waits for the issuance, or the issuance waits and then sees DISABLED
Pagination   : NO
Filters      : user_pk EXACT
Result shape : projection — pk, principal type, status
Null handling: no row → SEC-404-USER

#### QR-SEC-042 — a service account's credentials
Phase        : SVC-API (SVC-API-SEARCH)
API          : API-SEC-028 — list a service account's credentials
Entity       : ENT-SEC-014
Operation    : FIND_ALL
Intent       : every credential of one account, active and revoked, newest first (REQ-SEC-056)
Logical spec : SELECT service_account_credential_pk, description, created_at, created_by, last_used_at, revoked_at, revoked_by FROM SEC_SVC_ACCOUNT_CRED WHERE user_id = :userId ORDER BY created_at DESC
Join         : NONE
Transaction  : READ_ONLY
Locking      : NONE
Pagination   : NO — an account holds a handful of credentials
Filters      : user_id EXACT (IDX_SEC_SVC_ACCOUNT_CRED_USER)
Result shape : projection above — secret_hash is never selected (REQ-SEC-053)
Null handling: last_used_at, revoked_at, revoked_by returned as null when empty

#### QR-SEC-043 — store a newly issued credential
Phase        : SVC-API (SVC-API-CRUD)
API          : API-SEC-029 — issue a credential
Entity       : ENT-SEC-014
Operation    : SAVE
Intent       : insert one active credential holding only the one-way hash of the generated secret
Logical spec : INSERT INTO SEC_SVC_ACCOUNT_CRED (service_account_credential_pk ← nextval SEQ_SEC_SVC_ACCOUNT_CRED, user_id, secret_hash, description, created_by, created_at)
Join         : NONE
Transaction  : READ_WRITE — the transaction that holds QR-SEC-041's lock
Locking      : QR-SEC-041
Pagination   : NO
Filters      : —
Result shape : full entity (the response never carries secret_hash)
Null handling: description NULL when not sent; last_used_at, revoked_at, revoked_by NULL (CHK_SEC_SVC_ACCOUNT_CRED_REVOKED_BY holds)

#### QR-SEC-044 — a credential by PK
Phase        : CORE (bearer check) and SVC-API (SVC-API-CRUD)
API          : API-SEC-030 — revoke a credential; every secured API through the CORE bearer check of a SERVICE token
Entity       : ENT-SEC-014
Operation    : FIND_ONE
Intent       : the credential a path or a token's credential claim names, with its owner and revocation state
Logical spec : SELECT service_account_credential_pk, user_id, revoked_at FROM SEC_SVC_ACCOUNT_CRED WHERE service_account_credential_pk = :id
Join         : NONE
Transaction  : READ_ONLY
Locking      : NONE — the revocation's own guard is QR-SEC-045; the bearer check reads live state on every request and never caches it (ADR-SEC-037)
Pagination   : NO
Filters      : service_account_credential_pk EXACT
Result shape : projection above
Null handling: no row, or a user_id other than the path's or the token's subject → SEC-404-CREDENTIAL on API-SEC-030, SEC-401-INVALID-CLIENT in the bearer check

#### QR-SEC-045 — revoke a credential while it is unrevoked (RULE-SEC-011)
Phase        : SVC-API (SVC-API-CRUD)
API          : API-SEC-030 — revoke a credential
Entity       : ENT-SEC-014
Operation    : UPDATE
Intent       : set the revocation time and actor once; the first revocation stays the record
Logical spec : UPDATE SEC_SVC_ACCOUNT_CRED SET revoked_at = now(), revoked_by = :actor, updated_by = :actor, updated_at = now() WHERE service_account_credential_pk = :id AND user_id = :userId AND revoked_at IS NULL
Join         : NONE
Transaction  : READ_WRITE
Locking      : the conditional update is the guard — two concurrent revocations both read an active row, only one update matches; 0 rows → SEC-409-CREDENTIAL-ALREADY-REVOKED
Pagination   : NO
Filters      : —
Result shape : affected-row count
Null handling: revoked_at and revoked_by are set together (CHK_SEC_SVC_ACCOUNT_CRED_REVOKED_BY)

#### QR-SEC-046 — a service account's unrevoked credentials (RULE-SEC-009)
Phase        : SVC-API (SVC-API-INT)
API          : API-SEC-031 — issue access token; API-SEC-029 — issue a credential (the credential-count read, RULE-SEC-013)
Entity       : ENT-SEC-014
Operation    : FIND_ALL
Intent       : the credentials whose hash the presented secret is verified against (REQ-SEC-047, REQ-SEC-055); on API-SEC-029, the count of the account's unrevoked credentials against RULE-SEC-013's limit (REQ-SEC-078)
Logical spec : SELECT service_account_credential_pk, secret_hash FROM SEC_SVC_ACCOUNT_CRED WHERE user_id = :userId AND revoked_at IS NULL
Join         : NONE
Transaction  : READ_ONLY inside the token transaction; inside API-SEC-029's issuance transaction under QR-SEC-041's lock
Locking      : NONE on API-SEC-031 — a revocation committed after this read cannot be undone by it: the token issued from the credential is rejected on its first use (REQ-SEC-059). On API-SEC-029, the read is taken under QR-SEC-041's PESSIMISTIC_WRITE lock on the account row, so a concurrent issuance cannot both pass the count check and exceed the limit
Pagination   : NO
Filters      : user_id EXACT, revoked_at IS NULL
Result shape : projection above on API-SEC-031 — secret_hash leaves the repository only into the hash verifier; on API-SEC-029, only the row count is used
Null handling: an empty set → SEC-401-INVALID-CLIENT (API-SEC-031); an empty set on API-SEC-029 is simply a count of 0

#### QR-SEC-047 — record a credential's last use
Phase        : SVC-API (SVC-API-INT)
API          : API-SEC-031 — issue access token
Entity       : ENT-SEC-014
Operation    : UPDATE
Intent       : stamp the successful authentication time on the credential that matched (REQ-SEC-057, ADR-SEC-033)
Logical spec : UPDATE SEC_SVC_ACCOUNT_CRED SET last_used_at = now() WHERE service_account_credential_pk = :id
Join         : NONE
Transaction  : READ_WRITE (the token transaction)
Locking      : NONE — two concurrent authentications both write their own time; the later wins, and no rule reads the value (ADR-SEC-033)
Pagination   : NO
Filters      : —
Result shape : affected-row count
Null handling: —

#### QR-SEC-048 — append an audit entry on a rejection path
Phase        : CORE (bearer check) and SVC-API (SVC-API-INT)
API          : API-SEC-031 — issue access token; API-SEC-001 — login (LOGIN_FAILED); every secured API through the CORE bearer check
Entity       : ENT-SEC-011
Operation    : SAVE
Intent       : record a rejected authentication even though the request that caused it fails
Logical spec : INSERT INTO SEC_AUDIT_LOG (audit_log_pk ← nextval SEQ_SEC_AUDIT_LOG, event_type_code, actor_user_id, occurred_at, target_ref, details_ar, details_en, ip_address)
Join         : NONE
Transaction  : REQUIRES_NEW — ADR-SEC-037: the rejection is raised as an exception that rolls the caller's transaction back, and the audit row must survive it
Locking      : NONE — append-only
Pagination   : NO
Filters      : —
Result shape : none
Null handling: actor_user_id NULL when the client identifier names no SERVICE principal; target_ref then holds the submitted identifier (AC-SEC-071). Never any secret in any column

#### QR-SEC-049 — a user by email
Phase        : SVC-API (SVC-API-INT)
API          : API-SEC-003 — request password reset
Entity       : ENT-SEC-001
Operation    : FIND_ONE
Intent       : the user a reset request names, with its principal type (REQ-SEC-064)
Logical spec : SELECT user_pk, principal_type_code, status_code, email FROM SEC_USER WHERE email = :email
Join         : NONE
Transaction  : READ_ONLY inside the request transaction
Locking      : NONE
Pagination   : NO
Filters      : email EXACT (UQ_SEC_USER_EMAIL)
Result shape : projection above
Null handling: no row → the generic confirmation, exactly as for a SERVICE principal

#### QR-SEC-050 — a user's assigned roles
Phase        : SVC-API (SVC-API-SEARCH)
API          : API-SEC-032 — read user; API-SEC-008 — assign roles (its response)
Entity       : ENT-SEC-003, ENT-SEC-002
Operation    : FIND_ALL
Intent       : the roles a user holds, with their display names
Logical spec : SELECT r.role_pk, r.code, r.name_ar, r.name_en FROM SEC_USER_ROLE ur JOIN SEC_ROLE r ON r.role_pk = ur.role_id WHERE ur.user_id = :userId ORDER BY r.code
Join         : required — the role's code and names are parent data the roles tab shows (ADR-SEC-038)
Transaction  : READ_ONLY
Locking      : NONE
Pagination   : NO
Filters      : user_id EXACT
Result shape : projection above
Null handling: a user with no role → an empty list (AC-SEC-037)

#### QR-SEC-051 — a role by PK
Phase        : SVC-API (SVC-API-CRUD)
API          : API-SEC-033, API-SEC-034; API-SEC-014, API-SEC-015, API-SEC-016, API-SEC-017 take it under a write lock
Entity       : ENT-SEC-002
Operation    : FIND_ONE
Intent       : the role an endpoint's path names
Logical spec : SELECT … FROM SEC_ROLE WHERE role_pk = :id [FOR UPDATE on a grant-tree mutation]
Join         : NONE
Transaction  : READ_ONLY on a read; READ_WRITE on a grant-tree mutation
Locking      : PESSIMISTIC_WRITE on every grant-tree mutation of one role — a module revoke (RULE-SEC-003) and a concurrent screen or action grant (RULE-SEC-001, RULE-SEC-002, RULE-SEC-007) are serialized, so no grant survives its parent
Pagination   : NO
Filters      : role_pk EXACT
Result shape : full entity
Null handling: no row → SEC-404-ROLE

#### QR-SEC-052 — update a role
Phase        : SVC-API (SVC-API-CRUD)
API          : API-SEC-034 — update role
Entity       : ENT-SEC-002
Operation    : UPDATE
Intent       : change a role's bilingual names and descriptions; code, active flag and PK never change here
Logical spec : UPDATE SEC_ROLE SET name_ar, name_en, description_ar, description_en, updated_by, updated_at WHERE role_pk = :id
Join         : NONE
Transaction  : READ_WRITE
Locking      : NONE — last writer wins on free-text fields; no rule reads them
Pagination   : NO
Filters      : —
Result shape : full entity
Null handling: description_ar / description_en may be cleared to NULL

#### QR-SEC-053 — a role's grant tree
Phase        : SVC-API (SVC-API-SEARCH)
API          : API-SEC-035 — read a role's grant tree
Entity       : ENT-SEC-004, ENT-SEC-005, ENT-SEC-006, ENT-SEC-007, ENT-SEC-008, ENT-SEC-009
Operation    : FIND_ALL
Intent       : every module, screen and action a role holds, as the tree the grant editor shows
Logical spec : SELECT … FROM SEC_ROLE_MODULE_GRANT g JOIN SEC_MODULE_REG m ON m.module_reg_pk = g.module_id WHERE g.role_id = :id; the same for SEC_ROLE_SCREEN_GRANT with SEC_SCREEN_REG and SEC_ROLE_ACTION_GRANT with SEC_ACTION_REG; assembled module → screen → action
Join         : required — registry names and codes are parent data of each grant (ADR-SEC-038); every join stays inside SEC
Transaction  : READ_ONLY
Locking      : NONE
Pagination   : NO
Filters      : role_id EXACT
Result shape : projection — tree
Null handling: a role with no grant → an empty tree

#### QR-SEC-054 — search sign-up requests
Phase        : SVC-API (SVC-API-SEARCH)
API          : API-SEC-036 — search sign-up requests
Entity       : ENT-SEC-013
Operation    : FIND_BY_CRITERIA
Intent       : the requests the "Pending sign-ups" tab lists
Logical spec : SELECT … FROM SEC_SIGNUP_REQUEST WHERE [status_code = :status] AND [email LIKE :email] AND [full_name_ar LIKE :n OR full_name_en LIKE :n] ORDER BY :sort page :page size :size
Join         : NONE
Transaction  : READ_ONLY
Locking      : NONE
Pagination   : YES (Page<T>)
Filters      : statusCode EXACT · email LIKE · fullName LIKE
Result shape : full entity
Null handling: every filter optional; an empty page is success

---

<!-- PHASE:CORE:START traces=REQ-SEC-033,REQ-SEC-044,REQ-SEC-045,REQ-SEC-050,REQ-SEC-059,REQ-SEC-060 -->
## PHASE 1 — CORE

**Layers** (`profile.stack.backend.layers`): controller → service → mapper → domain → repository.
- **controller**: HTTP binding and request shape (types, required, lengths); never queries a repository; holds no
  RULE check.
- **service**: orchestration — load → hand facts to the domain → integrate → persist → return; the place the
  `PERM_*` requirement is asserted (`@PreAuthorize`) before anything proceeds.
- **mapper**: entity ↔ DTO only.
- **domain**: the decision "is this operation allowed?". Profile placement is `domain_classes`
  (`profile.conventions.domain_behaviour_placement`). The v2 rules live in two new domain classes, each taking the
  facts the service loaded as plain arguments:
  - `ServiceAccountCredentialDomain` — RULE-SEC-008 (`assertIssuable`), RULE-SEC-009 (`assertAuthenticates`,
    `assertBearerLive`), RULE-SEC-011 (`assertRevocable`);
  - `UserDomain` — RULE-SEC-010 (`assertInteractiveAllowed`), RULE-SEC-012 (`assertPrincipalTypeUnchanged`), and
    the v2 creation shape of a SERVICE principal (ACTIVE, unusable password hash, no role).
  The v1 rules (RULE-SEC-001 … RULE-SEC-007) keep their delivered placement; re-homing them is not part of this
  change set and is recorded as a known deviation in ADR-SEC-036.
- **repository**: Spring Data JPA, one per table; every non-trivial query is a named method behind a `QR-SEC-*`.

**Error signalling**: `LocalizedException → {code, messageAr, messageEn}`. Runtime code format, verbatim from
`profile.stack.backend.api.error_code_format`: `{MOD}-{http}[-{SLUG}]`, which for this module reads
`SEC-{http}[-{SLUG}]`. Every module-owned Error Catalog row is an instance of it and carries one of the statuses the
platform emits (200, 201, 400, 401, 403, 404, 409, 413, 415, 422, 500). Each is a constant in `SecErrorCodes` with
both bundle keys. Three rows are the shared `GlobalExceptionHandler`'s and are not module-scoped: `VALIDATION_ERROR`
(400), `INTERNAL_ERROR` (500) and `DATA_INTEGRITY_VIOLATION` (409); SEC neither declares nor throws them.
`DATA_INTEGRITY_VIOLATION` is what a caller actually receives when a uniqueness constraint refuses an insert that
passed its friendly pre-check. It was cited on API-SEC-002 and API-SEC-006's Concurrency lines and catalogued
nowhere, so its ar/en message and status went unstated; it is listed here as the shared row it is. SEC does not
translate it into a module code: that would be a v1 behaviour change this change set does not make. Revise pass G7:
the rule is general, not particular to those two — every endpoint whose Concurrency line names a UQ constraint as
its guarantee lists `DATA_INTEGRITY_VIOLATION` on its own Errors line, and the Error Catalog's API column is derived
from those Errors lines, not typed independently of them.

**Transaction scope**: `READ_ONLY` for every `FIND_*`/`EXISTS`/`AGGREGATE`; `READ_WRITE` for every
`SAVE`/`UPDATE`/`DELETE`. One exception, v2: QR-SEC-048 appends a rejection-path audit entry in `REQUIRES_NEW`,
because the rejection rolls the caller's transaction back and the entry must survive it (ADR-SEC-037).

**Search contract**: `{filters, page, size, sortField, sortDirection}` in the body (`BaseSearchContractRequest`),
`Page<T>` inside `ApiResponse<T>`, an empty result is success. Allowed sort fields are the screen's own filter and
result columns (SRS §B2), never widened; an unknown one → `SEC-400-INVALID-SORT`. Paging: size 20 by default, 200 at
most [KB:erp-domain-standards §6].

**Audit fields**: `createdBy, createdAt, updatedBy, updatedAt` are filled by the platform from the authenticated
principal and the clock, never by a request; the same holds for `assignedBy/assignedAt`, `grantedBy/grantedAt`,
`reviewedBy/reviewedAt`, `terminatedBy/terminatedAt`, and v2's `revokedBy/revokedAt`, set by the endpoint that
performs the act. For a request authenticated by a service-account token the principal string is the account's
username, so a FIN row it creates carries it in FIN's own audit fields (ADR-SEC-017).

**Type mapping** (postgresql16 → Java, from `profile.stack.db.syntax_map`; column types only — PK generation is not
a type):

| postgresql16 | Java | Notes |
|---|---|---|
| BIGINT | Long | every PK — a plain column filled from its table's own `SEQ_{TABLE}` sequence, no identity clause (ADR-SEC-035) — and every FK |
| VARCHAR(n) | String | every lookup-backed column holds the code |
| TEXT | String | password hash, token hash, secret hash, audit details |
| BOOLEAN | Boolean | every `…Fl` flag |
| TIMESTAMPTZ | Instant | audit and system timestamps, stored UTC |

SEC has no NUMERIC or DATE column.

**Lookup values**: USER_STATUS, SIGNUP_STATUS, AUDIT_EVENT_TYPE and (v2) PRINCIPAL_TYPE are returned and accepted as
plain `String` codes, never a Java enum and never a numeric id. They are CHECK-constrained columns (ADR-SEC-001). A
code a request may carry — v2's `principalTypeCode` on create — is validated before any write against the closed set
the CHECK enforces; a value outside it answers the shared `VALIDATION_ERROR` (400), so the CHECK is never the first
line of defence.

**Numbering**: not applicable — no SEC entity carries a platform-numbered code.

**Workflow engine**: forbidden — every lifecycle (User, SignupRequest, v2 ServiceAccountCredential) is a plain
guarded transition behind its own endpoint.

**Languages**: every name field and every catalog message exists in ar and en. v2 adds no stored bilingual pair
beyond the user's two full names, which for a service account hold the integration's name.

**Cross-module contract placement**: SEC consumes no module's entity (no `XM-*`). It injects one NOTIF-published
in-process interface for the optional reset dispatch (INT-C), and publishes its own in-process read surfaces in
`com.erp.sec.crossmodule` (INT-R). One deployable (`profile.conventions.module_interface: in_process`): no HTTP client
between modules, no network error path.

**Cross-cutting authorization — the CORE gate (REQ-SEC-033, REQ-SEC-044, REQ-SEC-045).** Every secured endpoint —
every API below except API-SEC-001 … API-SEC-004 and API-SEC-031, which are pre-authentication — passes this gate
before its method body runs:
1. The platform JWT filter verifies the token's signature and expiry, then reads its principal-type claim. A token
   without a principal-type claim is treated as HUMAN (step 2) — this covers a v1 HUMAN token issued before the v2
   upgrade, which carries no such claim and stays valid for up to 3600 s (G8, pass-1 review).
2. HUMAN token (v1, unchanged): the ActiveSession row behind the token must exist and be unterminated.
3. SERVICE token (v2 — REQ-SEC-050, REQ-SEC-059, REQ-SEC-060; the mechanism ADR-SEC-014 left to this stage, fixed by
   ADR-SEC-037): no ActiveSession row is read or written. The filter loads the credential named by the token's
   credential claim (QR-SEC-044) and the account named by its subject (QR-SEC-040), and hands both to
   `ServiceAccountCredentialDomain.assertBearerLive`: the credential must exist, belong to that account and have an
   empty revocation time, and the account must be ACTIVE and of principal type SERVICE (RULE-SEC-009). The result is
   never cached — a cache would reopen the trust window ADR-SEC-014 closes. A failure answers 401
   `SEC-401-INVALID-CLIENT` and appends one SERVICE_AUTH_FAILED entry whose actor is the token's account (QR-SEC-048,
   own transaction; AC-SEC-070).
4. Either way, the caller's authorities are then built from the grant tables exactly as for any principal — the same
   effective-grant read as API-SEC-027 (QR-SEC-027): a permission counts only if the same screen also carries a
   granted VIEW (RULE-SEC-007) and its module is granted. A service account is therefore gated exactly as a human is
   (REQ-SEC-044, REQ-SEC-045); `@PreAuthorize` on the service method then names the one `PERM_<PAGE_CODE>_<ACTION>`
   the endpoint needs, and a denial answers `SEC-403-FORBIDDEN`.
This mechanism is declared once, here; every API block's Security line refers to it.

**Service-account secrets (v2, ADR-SEC-037; hashing scheme fixed by ADR-SEC-043 §1, revise pass G1)**: a secret is
32 bytes from `SecureRandom`, base64url-encoded without padding (43 characters). Only its one-way keyed digest is
stored (REQ-SEC-054): HMAC-SHA256 of the secret, keyed by a server-side pepper the platform secret store injects at
runtime (never in the database or the repository) — not the platform `PasswordEncoder` used for human passwords,
because the secret is already 256 bits of random entropy and an adaptive password hash buys nothing against it while
costing more per verification. Verifying a presented secret against a stored `secretHash` is one constant-time HMAC
comparison; API-SEC-031 performs exactly ten of them per call regardless of outcome (ADR-SEC-043 §1, RULE-SEC-009).
The secret exists in memory only for the issuance response; the request and response bodies of API-SEC-029 and
API-SEC-031 are excluded from request logging, and both responses carry `Cache-Control: no-store`.
<!-- PHASE:CORE:END -->

<!-- PHASE:DATA-DOM:START traces=REQ-SEC-001,REQ-SEC-003,REQ-SEC-006,REQ-SEC-009,REQ-SEC-010,REQ-SEC-012,REQ-SEC-013,REQ-SEC-014,REQ-SEC-016,REQ-SEC-017,REQ-SEC-019,REQ-SEC-024,REQ-SEC-036,REQ-SEC-039,REQ-SEC-051,REQ-SEC-054,REQ-SEC-058 -->
## PHASE 2 — DATA-DOM

Fourteen entities, grouped by role: `SUB:DATA-DOM-MASTER` (the user, the role and the three registries),
`SUB:DATA-DOM-TRANSACTIONAL` (assignments, grants, sessions, audit rows, reset tokens, sign-ups and, in v2, the
service-account credentials — every row with a lifecycle or an occurrence time) and `SUB:DATA-DOM-LOOKUP` (empty, as
in v1). Every PK is a plain BIGINT filled by the application from its table's own sequence (db-script v2 BLOCK 1,
ADR-SEC-035); v1's blocks named `GENERATED ALWAYS AS IDENTITY`, which the v2 migration drops, so every binding below
names its sequence instead.

<!-- SUB:DATA-DOM-MASTER:START traces=REQ-SEC-009,REQ-SEC-012,REQ-SEC-016,REQ-SEC-017,REQ-SEC-019,REQ-SEC-036,REQ-SEC-039 -->
### SUB — DATA-DOM-MASTER

#### ENT-SEC-001 — User      kind: security   (MODIFIED v2)
BINDINGS       table `SEC_USER` · PK `userPk` (DBF-SEC-001) · PK generation `sequence` → `SEQ_SEC_USER` · db-script v2
BUSINESS CODE  none (§3.3 test: no) — the username is the natural key; for a SERVICE principal it is also the client identifier (DEFAULT D2)
DEFAULT FIELDS the `security` kind has no profile default set; every field is explicit below
FIELDS
| DBF | property | column | type | null | read-only | constraint | label-ar / label-en |
|---|---|---|---|---|---|---|---|
| DBF-SEC-001 | userPk | user_pk | Long | NOT NULL | Yes | PK_SEC_USER | معرّف المستخدم / User id |
| DBF-SEC-002 | username | username | String | NOT NULL | Yes after create | UQ_SEC_USER_USERNAME | اسم المستخدم / Username |
| DBF-SEC-105 | principalTypeCode | principal_type_code | String | NOT NULL | Yes after create (RULE-SEC-012) | CHK_SEC_USER_PRINCIPAL_TYPE | نوع الأساس الأمني / Principal type |
| DBF-SEC-003 | email | email | String | NOT NULL | No | UQ_SEC_USER_EMAIL | البريد الإلكتروني (للخدمة: بريد جهة الاتصال) / Email (service: contact email) |
| DBF-SEC-004 | passwordHash | password_hash | String | NOT NULL | Yes, never serialized | — | تجزئة كلمة المرور / Password hash |
| DBF-SEC-005 | fullNameAr | full_name_ar | String | NOT NULL | No | — | الاسم الكامل (عربي) / Full name (Arabic) |
| DBF-SEC-006 | fullNameEn | full_name_en | String | NOT NULL | No | — | الاسم الكامل (إنجليزي) / Full name (English) |
| DBF-SEC-007 | statusCode | status_code | String | NOT NULL | Yes (lifecycle endpoints) | CHK_SEC_USER_PRINCIPAL_STATUS | الحالة / Status |
| DBF-SEC-008 | lastLoginAt | last_login_at | Instant | NULL | Yes | — | آخر دخول / Last login |
| DBF-SEC-009 | isActiveFl | is_active_fl | Boolean | NOT NULL | Yes (lifecycle endpoints) | — | نشط / Active |
| DBF-SEC-010 | createdBy | created_by | String | NOT NULL | Yes | — | أُنشئ بواسطة / Created by |
| DBF-SEC-011 | createdAt | created_at | Instant | NOT NULL | Yes | — | تاريخ الإنشاء / Created at |
| DBF-SEC-012 | updatedBy | updated_by | String | NULL | Yes | — | حُدّث بواسطة / Updated by |
| DBF-SEC-013 | updatedAt | updated_at | Instant | NULL | Yes | — | تاريخ التحديث / Updated at |
DTO MEMBERSHIP create-request `{username, email, fullNameAr, fullNameEn, principalTypeCode?, password?, statusCode?}` — principalTypeCode defaults to HUMAN; for HUMAN the password is required and hashed server-side and statusCode is optional (default ACTIVE); for SERVICE neither password nor statusCode is read: the status is ACTIVE and the password hash an unusable random value (ADR-SEC-032). Excluded: userPk, passwordHash, lastLoginAt, isActiveFl, audit. Update-request `{email, fullNameAr, fullNameEn, principalTypeCode?}` — principalTypeCode may be echoed but never changed (RULE-SEC-012); excluded: userPk, username, passwordHash, statusCode, isActiveFl, audit. Response: every field except passwordHash, plus (read endpoint) the assigned roles.
LOOKUP FIELDS  statusCode (DBF-SEC-007) → key `USER_STATUS`; principalTypeCode (DBF-SEC-105) → key `PRINCIPAL_TYPE` (v2) — codes, CHECK-constrained (ADR-SEC-001), never a numeric FK
DOMAIN RULES
- RULE-SEC-010 (v2) — trigger: on login; on password-reset request. Statement: "The system shall reject an interactive login, and shall issue no password-reset token, when the named user is of principal type SERVICE." Message: ar "بيانات الدخول غير صحيحة" · en "Invalid credentials" (login — the v1 message, so the answer does not disclose the account type); the reset request answers with the generic confirmation REQ-SEC-006 gives for any email. Scope: ALL · DB enforcement: application layer · owner: `UserDomain.assertInteractiveAllowed` · Data source: ENT-SEC-001.principalTypeCode, ENT-SEC-001.username, ENT-SEC-001.email
- RULE-SEC-012 (v2) — trigger: on update (user). Statement: "The system shall prevent any update that changes an existing user's principal type." Message: ar "لا يمكن تغيير نوع الأساس الأمني بعد إنشائه" · en "The principal type cannot be changed after creation". Scope: UPDATE · DB enforcement: application layer (db-script BLOCK 6: no trigger) · owner: `UserDomain.assertPrincipalTypeUnchanged` · Data source: ENT-SEC-001.principalTypeCode
- v2 creation shape of a SERVICE principal (REQ-SEC-036, REQ-SEC-037; not a RULE): status ACTIVE, passwordHash an unusable random value that is never logged, returned or transmitted, zero role assignments — owned by `UserDomain.newServiceAccount`
STATE MACHINE  statusCode (DBF-SEC-007, USER_STATUS) — PENDING → ACTIVE (API-SEC-011, administrator), ACTIVE → DISABLED (API-SEC-009, administrator), DISABLED → ACTIVE (API-SEC-010, administrator); initial ACTIVE on direct create (API-SEC-006). v2: a SERVICE principal starts ACTIVE and never enters PENDING (CHK_SEC_USER_PRINCIPAL_STATUS); its ACTIVE → DISABLED transition makes the CORE bearer check and API-SEC-031 reject every credential and token of the account (REQ-SEC-060), and DISABLED → ACTIVE lets its never-revoked credentials authenticate again (REQ-SEC-062). No terminal state; the transitions are exactly the endpoints that exist.
CROSS-MODULE   none consumed. Exposed read-only through `com.erp.sec.crossmodule.SecUserDirectoryApi` (REQ-SEC-034, REQ-SEC-035 — INT-R); v2: both reads cover service accounts unchanged
OPERATIONS     VIEW · CREATE · UPDATE
REPOSITORY OPS → QR-SEC-001, QR-SEC-005, QR-SEC-006, QR-SEC-007, QR-SEC-009, QR-SEC-010, QR-SEC-033; v2 QR-SEC-040, QR-SEC-041, QR-SEC-049

#### ENT-SEC-002 — Role      kind: security
BINDINGS       table `SEC_ROLE` · PK `rolePk` (DBF-SEC-014) · PK generation `sequence` → `SEQ_SEC_ROLE` · db-script v2
BUSINESS CODE  none — `code` is administrator-chosen and unique (UQ_SEC_ROLE_CODE)
FIELDS
| DBF | property | column | type | null | read-only | constraint | label-ar / label-en |
|---|---|---|---|---|---|---|---|
| DBF-SEC-014 | rolePk | role_pk | Long | NOT NULL | Yes | PK_SEC_ROLE | معرّف الدور / Role id |
| DBF-SEC-015 | code | code | String | NOT NULL | Yes after create | UQ_SEC_ROLE_CODE | رمز الدور / Role code |
| DBF-SEC-016 | nameAr | name_ar | String | NOT NULL | No | — | اسم الدور (عربي) / Role name (Arabic) |
| DBF-SEC-017 | nameEn | name_en | String | NOT NULL | No | — | اسم الدور (إنجليزي) / Role name (English) |
| DBF-SEC-018 | descriptionAr | description_ar | String | NULL | No | — | الوصف (عربي) / Description (Arabic) |
| DBF-SEC-019 | descriptionEn | description_en | String | NULL | No | — | الوصف (إنجليزي) / Description (English) |
| DBF-SEC-020 | isActiveFl | is_active_fl | Boolean | NOT NULL | Yes | — | نشط / Active |
| DBF-SEC-021..024 | createdBy/createdAt/updatedBy/updatedAt | created_by/created_at/updated_by/updated_at | String/Instant | see db-script | Yes | — | audit / audit |
DTO MEMBERSHIP create excludes {rolePk, isActiveFl, audit}; update (API-SEC-034) carries only {nameAr, nameEn, descriptionAr, descriptionEn}; the response includes every field
LOOKUP FIELDS  none
DOMAIN RULES   none scoped to Role alone; the grant rules scope ENT-SEC-007 … ENT-SEC-009
STATE MACHINE  isActiveFl binary — not applicable. Deactivating a role (SRS SCR-REQ-SEC-005 "deactivate") has no endpoint in this version: DEFERRED (ADR-SEC-038)
CROSS-MODULE   none
OPERATIONS     VIEW · CREATE · UPDATE
REPOSITORY OPS → QR-SEC-012, QR-SEC-013, QR-SEC-034; v2 QR-SEC-051, QR-SEC-052

#### ENT-SEC-004 — ModuleRegistry      kind: security
BINDINGS       table `SEC_MODULE_REG` · PK `moduleRegPk` (DBF-SEC-030) · PK generation `sequence` → `SEQ_SEC_MODULE_REG` · db-script v2
BUSINESS CODE  none — `code` is the platform's own module prefix, supplied by the registering module
FIELDS
| DBF | property | column | type | null | read-only | constraint | label-ar / label-en |
|---|---|---|---|---|---|---|---|
| DBF-SEC-030 | moduleRegPk | module_reg_pk | Long | NOT NULL | Yes | PK_SEC_MODULE_REG | معرّف الوحدة المسجّلة / Registered module id |
| DBF-SEC-031 | code | code | String | NOT NULL | No | UQ_SEC_MODULE_REG_CODE | رمز الوحدة / Module code |
| DBF-SEC-032 | nameAr | name_ar | String | NOT NULL | No | — | اسم الوحدة (عربي) / Module name (Arabic) |
| DBF-SEC-033 | nameEn | name_en | String | NOT NULL | No | — | اسم الوحدة (إنجليزي) / Module name (English) |
| DBF-SEC-034..038 | isActiveFl/audit | is_active_fl/… | Boolean/String/Instant | see db-script | Yes | — | — |
DTO MEMBERSHIP create excludes {moduleRegPk, isActiveFl, audit}; the response includes every field
LOOKUP FIELDS  none
DOMAIN RULES   none scoped alone (RULE-SEC-004 scopes ENT-SEC-005)
STATE MACHINE  binary active flag — not applicable. Deactivating a registry row (SRS SCR-REQ-SEC-006 "deactivate") has no endpoint in this version: DEFERRED (ADR-SEC-038)
CROSS-MODULE   none consumed; every module registers into it through API-SEC-018, and MDL reads it in process (INT-R)
OPERATIONS     VIEW · UPDATE
REPOSITORY OPS → QR-SEC-018, QR-SEC-021, QR-SEC-035

#### ENT-SEC-005 — ScreenRegistry      kind: security
BINDINGS       table `SEC_SCREEN_REG` · PK `screenRegPk` (DBF-SEC-039) · PK generation `sequence` → `SEQ_SEC_SCREEN_REG` · db-script v2
BUSINESS CODE  none — `pageCode` is caller-supplied per the SEC_PAGES convention
FIELDS
| DBF | property | column | type | null | read-only | constraint | label-ar / label-en |
|---|---|---|---|---|---|---|---|
| DBF-SEC-039 | screenRegPk | screen_reg_pk | Long | NOT NULL | Yes | PK_SEC_SCREEN_REG | معرّف الشاشة المسجّلة / Registered screen id |
| DBF-SEC-040 | pageCode | page_code | String | NOT NULL | No | UQ_SEC_SCREEN_REG_PAGE | رمز الصفحة / Page code |
| DBF-SEC-041 | moduleId | module_id | Long | NOT NULL | No | FK_SCREEN_REG_MODULE | الوحدة / Module |
| DBF-SEC-042 | nameAr | name_ar | String | NOT NULL | No | — | اسم الشاشة (عربي) / Screen name (Arabic) |
| DBF-SEC-043 | nameEn | name_en | String | NOT NULL | No | — | اسم الشاشة (إنجليزي) / Screen name (English) |
| DBF-SEC-044..048 | isActiveFl/audit | … | — | see db-script | Yes | — | — |
DTO MEMBERSHIP create `{moduleCode, pageCode, nameAr, nameEn}` — moduleCode is resolved to moduleId; excludes {screenRegPk, isActiveFl, audit}
LOOKUP FIELDS  none
DOMAIN RULES
- RULE-SEC-004 — trigger: on create (screen registration). Statement: "The system shall reject a screen registration whose module code has no ModuleRegistry row." Message: ar "الوحدة غير مسجّلة" · en "Module is not registered". DB enforcement: FK_SCREEN_REG_MODULE (the hard guarantee) with a service pre-check (QR-SEC-036) for the catalog answer · owner: service + database, as delivered
STATE MACHINE  binary active flag — not applicable
CROSS-MODULE   none
OPERATIONS     VIEW · UPDATE
REPOSITORY OPS → QR-SEC-019, QR-SEC-036

#### ENT-SEC-006 — ActionRegistry      kind: security
BINDINGS       table `SEC_ACTION_REG` · PK `actionRegPk` (DBF-SEC-049) · PK generation `sequence` → `SEQ_SEC_ACTION_REG` · db-script v2
BUSINESS CODE  none — `permissionCode` is built by the server as `PERM_<PAGE_CODE>_<ACTION>` and never entered
FIELDS
| DBF | property | column | type | null | read-only | constraint | label-ar / label-en |
|---|---|---|---|---|---|---|---|
| DBF-SEC-049 | actionRegPk | action_reg_pk | Long | NOT NULL | Yes | PK_SEC_ACTION_REG | معرّف الإجراء المسجّل / Registered action id |
| DBF-SEC-050 | permissionCode | permission_code | String | NOT NULL | Yes (server-built) | UQ_SEC_ACTION_REG_PERM | رمز الصلاحية / Permission code |
| DBF-SEC-051 | screenId | screen_id | Long | NOT NULL | No | FK_ACTION_REG_SCREEN | الشاشة / Screen |
| DBF-SEC-052 | actionCode | action_code | String | NOT NULL | No | — | الإجراء / Action |
| DBF-SEC-053 | nameAr | name_ar | String | NOT NULL | No | — | اسم الإجراء (عربي) / Action name (Arabic) |
| DBF-SEC-054 | nameEn | name_en | String | NOT NULL | No | — | اسم الإجراء (إنجليزي) / Action name (English) |
| DBF-SEC-055..059 | isActiveFl/audit | … | — | see db-script | Yes | — | — |
DTO MEMBERSHIP create `{pageCode, actionCode, nameAr, nameEn}` — excludes {actionRegPk, permissionCode, isActiveFl, audit}; the response carries the built permissionCode
LOOKUP FIELDS  none — actionCode is an open set (the four standard actions plus module-declared custom codes)
DOMAIN RULES   none scoped alone
STATE MACHINE  binary active flag — not applicable
CROSS-MODULE   none
OPERATIONS     VIEW · UPDATE
REPOSITORY OPS → QR-SEC-020, QR-SEC-037
<!-- SUB:DATA-DOM-MASTER:END -->

<!-- SUB:DATA-DOM-TRANSACTIONAL:START traces=REQ-SEC-001,REQ-SEC-010,REQ-SEC-012,REQ-SEC-013,REQ-SEC-014,REQ-SEC-015,REQ-SEC-024,REQ-SEC-051,REQ-SEC-054,REQ-SEC-058 -->
### SUB — DATA-DOM-TRANSACTIONAL

#### ENT-SEC-003 — UserRoleAssignment      kind: security
BINDINGS       table `SEC_USER_ROLE` · PK `userRolePk` (DBF-SEC-025) · PK generation `sequence` → `SEQ_SEC_USER_ROLE` · db-script v2
FIELDS
| DBF | property | column | type | null | read-only | constraint |
|---|---|---|---|---|---|---|
| DBF-SEC-025 | userRolePk | user_role_pk | Long | NOT NULL | Yes | PK_SEC_USER_ROLE |
| DBF-SEC-026 | userId | user_id | Long | NOT NULL | Yes (from the path) | FK_USER_ROLE_USER, UQ_SEC_USER_ROLE_USER_ROLE |
| DBF-SEC-027 | roleId | role_id | Long | NOT NULL | No | FK_USER_ROLE_ROLE, UQ_SEC_USER_ROLE_USER_ROLE |
| DBF-SEC-028 | assignedBy | assigned_by | String | NOT NULL | Yes | — |
| DBF-SEC-029 | assignedAt | assigned_at | Instant | NOT NULL | Yes | — |
DTO MEMBERSHIP request `{roleIds: [Long]}` (the whole set); response `[{roleId, code, nameAr, nameEn}]`
DOMAIN RULES   RULE-SEC-005 applies here as well (full text under ENT-SEC-009). v2: a service account's assignments are recorded through this same entity, exactly as a human's (REQ-SEC-043)
CROSS-MODULE   none
OPERATIONS     VIEW · UPDATE
REPOSITORY OPS → QR-SEC-008, QR-SEC-031; v2 QR-SEC-050

#### ENT-SEC-007 — RoleModuleGrant      kind: security
BINDINGS       table `SEC_ROLE_MODULE_GRANT` · PK `roleModuleGrantPk` (DBF-SEC-060) · PK generation `sequence` → `SEQ_SEC_ROLE_MODULE_GRANT` · db-script v2
FIELDS
| DBF | property | column | type | null | read-only | constraint |
|---|---|---|---|---|---|---|
| DBF-SEC-060 | roleModuleGrantPk | role_module_grant_pk | Long | NOT NULL | Yes | PK_SEC_ROLE_MODULE_GRANT |
| DBF-SEC-061 | roleId | role_id | Long | NOT NULL | Yes (from the path) | FK_ROLE_MODULE_GRANT_ROLE, UQ_SEC_ROLE_MODULE_GRANT_ROLE_MODULE |
| DBF-SEC-062 | moduleId | module_id | Long | NOT NULL | No | FK_ROLE_MODULE_GRANT_MODULE, UQ_SEC_ROLE_MODULE_GRANT_ROLE_MODULE |
| DBF-SEC-063 | grantedBy | granted_by | String | NOT NULL | Yes | — |
| DBF-SEC-064 | grantedAt | granted_at | Instant | NOT NULL | Yes | — |
DOMAIN RULES
- RULE-SEC-003 — trigger: on delete (module grant). Statement: "The system shall delete every screen grant and action grant that module covered for that role when its module grant is revoked." Message: ar "سيتم سحب كل منح الشاشات والإجراءات ضمن هذه الوحدة لهذا الدور" · en "Every screen and action grant under this module for this role will be revoked". DB enforcement: application layer, one transaction · owner: service, as delivered
CROSS-MODULE   none
OPERATIONS     VIEW · UPDATE
REPOSITORY OPS → QR-SEC-014, QR-SEC-015, QR-SEC-028, QR-SEC-032; v2 QR-SEC-053

#### ENT-SEC-008 — RoleScreenGrant      kind: security
BINDINGS       table `SEC_ROLE_SCREEN_GRANT` · PK `roleScreenGrantPk` (DBF-SEC-065) · PK generation `sequence` → `SEQ_SEC_ROLE_SCREEN_GRANT` · db-script v2
FIELDS
| DBF | property | column | type | null | read-only | constraint |
|---|---|---|---|---|---|---|
| DBF-SEC-065 | roleScreenGrantPk | role_screen_grant_pk | Long | NOT NULL | Yes | PK_SEC_ROLE_SCREEN_GRANT |
| DBF-SEC-066 | roleId | role_id | Long | NOT NULL | Yes (from the path) | FK_ROLE_SCREEN_GRANT_ROLE, UQ_SEC_ROLE_SCREEN_GRANT_ROLE_SCREEN |
| DBF-SEC-067 | screenId | screen_id | Long | NOT NULL | No | FK_ROLE_SCREEN_GRANT_SCREEN, UQ_SEC_ROLE_SCREEN_GRANT_ROLE_SCREEN |
| DBF-SEC-068 | grantedBy | granted_by | String | NOT NULL | Yes | — |
| DBF-SEC-069 | grantedAt | granted_at | Instant | NOT NULL | Yes | — |
DOMAIN RULES
- RULE-SEC-001 — trigger: on create (screen grant). Statement: "The system shall prevent a screen grant for a role that does not hold the screen's module grant." Message: ar "لا يمكن منح شاشة دون منح الوحدة أولًا" · en "Cannot grant a screen without first granting its module". DB enforcement: application layer (QR-SEC-028) · owner: service, as delivered
CROSS-MODULE   none
OPERATIONS     VIEW · UPDATE
REPOSITORY OPS → QR-SEC-016, QR-SEC-028, QR-SEC-032; v2 QR-SEC-053

#### ENT-SEC-009 — RoleActionGrant      kind: security
BINDINGS       table `SEC_ROLE_ACTION_GRANT` · PK `roleActionGrantPk` (DBF-SEC-070) · PK generation `sequence` → `SEQ_SEC_ROLE_ACTION_GRANT` · db-script v2
FIELDS
| DBF | property | column | type | null | read-only | constraint |
|---|---|---|---|---|---|---|
| DBF-SEC-070 | roleActionGrantPk | role_action_grant_pk | Long | NOT NULL | Yes | PK_SEC_ROLE_ACTION_GRANT |
| DBF-SEC-071 | roleId | role_id | Long | NOT NULL | Yes (from the path) | FK_ROLE_ACTION_GRANT_ROLE, UQ_SEC_ROLE_ACTION_GRANT_ROLE_ACTION |
| DBF-SEC-072 | actionId | action_id | Long | NOT NULL | No | FK_ROLE_ACTION_GRANT_ACTION, UQ_SEC_ROLE_ACTION_GRANT_ROLE_ACTION |
| DBF-SEC-073 | grantedBy | granted_by | String | NOT NULL | Yes | — |
| DBF-SEC-074 | grantedAt | granted_at | Instant | NOT NULL | Yes | — |
DOMAIN RULES
- RULE-SEC-002 — trigger: on create (action grant). Statement: "The system shall prevent an action grant for a role that does not hold the action's screen grant." Message: ar "لا يمكن منح إجراء دون منح الشاشة أولًا" · en "Cannot grant an action without first granting its screen". DB enforcement: application layer (QR-SEC-029) · owner: service, as delivered
- RULE-SEC-005 — trigger: on create (role assignment or action grant). Statement: "The system shall prevent assigning a user, by any combination of roles, both actions of a module-declared conflicting pair." Message: ar "هذا المستخدم يملك إجراءً متعارضًا بالفعل" · en "This user already holds a conflicting action". DB enforcement: application layer (QR-SEC-031) · owner: service. Inert, as in v1: nothing in SEC declares a conflicting pair, so the counterpart set is empty and the guard never fires (REQ-SEC-020 Note, unchanged)
- RULE-SEC-007 — trigger: on evaluate (any action check) and on create (action grant, informational). Statement: "The system shall require a role to hold the VIEW action grant on a screen before any other action grant on that screen takes effect for it." Message: ar "يلزم منح إجراء العرض (VIEW) أولًا على هذه الشاشة" · en "The VIEW action must be granted on this screen first". DB enforcement: application layer (QR-SEC-030 at grant time; the CORE gate on every request) · owner: service. v2: binds every principal; the Oracle event consumer's role therefore holds VIEW on the journal-entry screen as well as its create action (ADR-SEC-034, AC-SEC-077)
CROSS-MODULE   none
OPERATIONS     VIEW · UPDATE
REPOSITORY OPS → QR-SEC-017, QR-SEC-029, QR-SEC-030, QR-SEC-031; v2 QR-SEC-053

#### ENT-SEC-010 — ActiveSession      kind: security
BINDINGS       table `SEC_ACTIVE_SESSION` · PK `activeSessionPk` (DBF-SEC-075) · PK generation `sequence` → `SEQ_SEC_ACTIVE_SESSION` · db-script v2
FIELDS         DBF-SEC-075..082 — see the DB Alignment Manifest; every field is system-set
DTO MEMBERSHIP no create or update request (created by API-SEC-001); the response carries every field except tokenRef
DOMAIN RULES   none scoped alone. v2: a service account never has a row here — neither API-SEC-031 nor the CORE bearer check reads or writes this table for a SERVICE token (REQ-SEC-046, REQ-SEC-048)
STATE MACHINE  terminatedAt empty = open, set = terminated — binary, not applicable
CROSS-MODULE   none
OPERATIONS     VIEW · DELETE
REPOSITORY OPS → QR-SEC-025, QR-SEC-026 (written also by API-SEC-001, a plain SAVE)

#### ENT-SEC-011 — AuditLogEntry      kind: security
BINDINGS       table `SEC_AUDIT_LOG` · PK `auditLogPk` (DBF-SEC-083) · PK generation `sequence` → `SEQ_SEC_AUDIT_LOG` · db-script v2
FIELDS         DBF-SEC-083..090 — see the DB Alignment Manifest; append-only
DTO MEMBERSHIP no client-facing create or update; rows are appended by the orchestration of other endpoints; search and export return every field
LOOKUP FIELDS  eventTypeCode (DBF-SEC-084) → key `AUDIT_EVENT_TYPE` — v2: 21 codes, the seven SERVICE_* codes added (CHK_SEC_AUDIT_LOG_EVENT_TYPE; revise pass G6: +SERVICE_ACCOUNT_REACTIVATED)
DOMAIN RULES   none — immutability is by omission: no UPDATE or DELETE mapping exists on this repository. v2 event sources: SERVICE_ACCOUNT_CREATED (API-SEC-006), SERVICE_ACCOUNT_DEACTIVATED (API-SEC-009), SERVICE_ACCOUNT_REACTIVATED (API-SEC-010, revise pass G6), SERVICE_CREDENTIAL_ISSUED (API-SEC-029), SERVICE_CREDENTIAL_REVOKED (API-SEC-030), SERVICE_AUTH_SUCCESS and SERVICE_AUTH_FAILED (API-SEC-031, the CORE bearer check); ROLE_ASSIGNED / ROLE_REVOKED for a service account as for a human (API-SEC-008). No entry ever holds a secret
CROSS-MODULE   none
OPERATIONS     VIEW
REPOSITORY OPS → QR-SEC-023, QR-SEC-024; v2 QR-SEC-048 (the rejection-path append). Other appends are inline SAVEs of the shape {eventTypeCode, actorUserId, occurredAt=now(), targetRef, detailsAr, detailsEn, ipAddress} in the writing endpoint's own transaction

#### ENT-SEC-012 — PasswordResetToken      kind: security
BINDINGS       table `SEC_PWD_RESET_TOKEN` · PK `pwdResetTokenPk` (DBF-SEC-091) · PK generation `sequence` → `SEQ_SEC_PWD_RESET_TOKEN` · db-script v2
FIELDS         DBF-SEC-091..096 — see the DB Alignment Manifest; system-managed lifecycle
DTO MEMBERSHIP none exposed — internal to API-SEC-003 / API-SEC-004; the client only ever sees the opaque token delivered out of band
DOMAIN RULES
- RULE-SEC-006 — trigger: on submit (password reset completion). Statement: "The system shall reject a password-reset submission whose token is expired or already used." Message: ar "رابط إعادة التعيين غير صالح أو منتهي" · en "This reset link is invalid or has expired". DB enforcement: application layer (QR-SEC-038, and QR-SEC-004's conditional update) · owner: service, as delivered. expiresAt = requestedAt + 30 minutes (SRS A7 DEFAULT)
- v2: no token is ever created for a SERVICE principal (RULE-SEC-010, under ENT-SEC-001)
CROSS-MODULE   none
OPERATIONS     CREATE · UPDATE
REPOSITORY OPS → QR-SEC-003, QR-SEC-004, QR-SEC-038

#### ENT-SEC-013 — SignupRequest      kind: security
BINDINGS       table `SEC_SIGNUP_REQUEST` · PK `signupRequestPk` (DBF-SEC-097) · PK generation `sequence` → `SEQ_SEC_SIGNUP_REQUEST` · db-script v2
FIELDS         DBF-SEC-097..104 — see the DB Alignment Manifest
DTO MEMBERSHIP create `{email, fullNameAr, fullNameEn}`; no update endpoint (decisions go through API-SEC-011); the response includes every field
LOOKUP FIELDS  statusCode (DBF-SEC-102) → key `SIGNUP_STATUS` (ADR-SEC-001)
DOMAIN RULES   none scoped alone. v2: an approval always creates a user of principal type HUMAN (REQ-SEC-038) — sign-up is never a path to a machine principal
STATE MACHINE  statusCode (SIGNUP_STATUS) — PENDING → APPROVED (API-SEC-011, administrator), PENDING → REJECTED (API-SEC-011, administrator); both terminal
CROSS-MODULE   none
OPERATIONS     CREATE · VIEW · UPDATE
REPOSITORY OPS → QR-SEC-002, QR-SEC-011; v2 QR-SEC-054

#### ENT-SEC-014 — ServiceAccountCredential      kind: security   (ADDED v2)
BINDINGS       table `SEC_SVC_ACCOUNT_CRED` · PK `serviceAccountCredentialPk` (DBF-SEC-106) · PK generation `sequence` → `SEQ_SEC_SVC_ACCOUNT_CRED` · db-script v2
BUSINESS CODE  none (§3.3 test: no) — identified inside SEC by its PK and description; the machine caller authenticates by client identifier and secret
FIELDS
| DBF | property | column | type | null | read-only | constraint | label-ar / label-en |
|---|---|---|---|---|---|---|---|
| DBF-SEC-106 | serviceAccountCredentialPk | service_account_credential_pk | Long | NOT NULL | Yes | PK_SEC_SVC_ACCOUNT_CRED | معرّف بيانات الاعتماد / Credential id |
| DBF-SEC-107 | secretHash | secret_hash | String | NOT NULL | Yes — write-once, never serialized | — | تجزئة السرّ / Secret hash |
| DBF-SEC-108 | description | description | String | NULL | No on issue, then Yes | — | الوصف / Description |
| DBF-SEC-109 | lastUsedAt | last_used_at | Instant | NULL | Yes | — | آخر استخدام / Last used |
| DBF-SEC-110 | revokedAt | revoked_at | Instant | NULL | Yes | CHK_SEC_SVC_ACCOUNT_CRED_REVOKED_BY | أُلغيت في / Revoked at |
| DBF-SEC-111 | revokedBy | revoked_by | String | NULL | Yes | CHK_SEC_SVC_ACCOUNT_CRED_REVOKED_BY | أُلغيت بواسطة / Revoked by |
| DBF-SEC-112 | userId | user_id | Long | NOT NULL | Yes (from the path) | FK_SVC_ACCOUNT_CRED_USER | حساب الخدمة / Service account |
| DBF-SEC-113 | createdBy | created_by | String | NOT NULL | Yes | — | أصدرها / Issued by |
| DBF-SEC-114 | createdAt | created_at | Instant | NOT NULL | Yes | — | أُصدرت في / Issued at |
| DBF-SEC-115 | updatedBy | updated_by | String | NULL | Yes | — | حُدّث بواسطة / Updated by |
| DBF-SEC-116 | updatedAt | updated_at | Instant | NULL | Yes | — | تاريخ التحديث / Updated at |
DTO MEMBERSHIP issue-request `{description?}` — excluded: every other field; issue-response `{serviceAccountCredentialPk, description, createdAt, secret}` — the secret appears in this response only and never again (REQ-SEC-051, REQ-SEC-053); list-response `[{serviceAccountCredentialPk, description, createdAt, createdBy, lastUsedAt, revokedAt, revokedBy}]` — never secretHash, never the secret; no update request (revocation is its own endpoint)
LOOKUP FIELDS  none
DOMAIN RULES
- RULE-SEC-008 — trigger: on create (credential issuance). Statement: "The system shall prevent issuing a credential when the target user is not of principal type SERVICE or is not ACTIVE." Message: ar "لا تُصدَر بيانات الاعتماد إلا لحساب خدمة نشط" · en "Credentials can be issued only for an active service account". Scope: CREATE · DB enforcement: application layer (db-script BLOCK 6) · owner: `ServiceAccountCredentialDomain.assertIssuable` · Data source: ENT-SEC-001.principalTypeCode, ENT-SEC-001.statusCode (QR-SEC-041)
- RULE-SEC-009 — trigger: on machine authentication request, and on every request bearing a service-account access token. Statement: "The system shall reject a machine authentication request or a service-account access token when the named user is not an ACTIVE user of principal type SERVICE, when the presented secret matches none of that user's unrevoked credentials, or when the credential that issued the token is revoked." Message: ar "بيانات اعتماد العميل غير صالحة" · en "Invalid client credentials" — one message on every failure path. Scope: ALL · DB enforcement: application layer · owner: `ServiceAccountCredentialDomain.assertAuthenticates` (API-SEC-031) and `assertBearerLive` (CORE) · Data source: ENT-SEC-001.username, ENT-SEC-001.principalTypeCode, ENT-SEC-001.statusCode, ENT-SEC-014.userId, ENT-SEC-014.secretHash, ENT-SEC-014.revokedAt (QR-SEC-001, QR-SEC-040, QR-SEC-044, QR-SEC-046)
- RULE-SEC-011 — trigger: on update (credential revocation). Statement: "The system shall prevent revoking a credential whose revocation time is already set." Message: ar "بيانات الاعتماد هذه ملغاة بالفعل" · en "This credential is already revoked". Scope: UPDATE · DB enforcement: application layer; QR-SEC-045's conditional update makes it race-free · owner: `ServiceAccountCredentialDomain.assertRevocable` · Data source: ENT-SEC-014.revokedAt
STATE MACHINE  two states, active (revokedAt empty) and revoked (API-SEC-030); revoked is terminal (RULE-SEC-011). Whether an active credential authenticates also depends on its account's status (REQ-SEC-060, REQ-SEC-062) — read live, never copied onto the credential
CROSS-MODULE   none (PRIVATE)
OPERATIONS     VIEW · UPDATE
REPOSITORY OPS → v2 QR-SEC-042, QR-SEC-043, QR-SEC-044, QR-SEC-045, QR-SEC-046, QR-SEC-047
<!-- SUB:DATA-DOM-TRANSACTIONAL:END -->

<!-- SUB:DATA-DOM-LOOKUP:START traces=REQ-SEC-004,REQ-SEC-040 -->
### SUB — DATA-DOM-LOOKUP
Not applicable — SEC owns no `kind: lookup` entity. USER_STATUS, SIGNUP_STATUS, AUDIT_EVENT_TYPE and (v2)
PRINCIPAL_TYPE are CHECK-constrained value sets on other entities' code columns (ADR-SEC-001), not tables. This SUB
exists for the profile's grouping and is intentionally empty.
<!-- SUB:DATA-DOM-LOOKUP:END -->
<!-- PHASE:DATA-DOM:END -->

<!-- PHASE:SVC-API:START traces=REQ-SEC-001,REQ-SEC-009,REQ-SEC-012,REQ-SEC-016,REQ-SEC-022,REQ-SEC-025,REQ-SEC-036,REQ-SEC-046,REQ-SEC-051,REQ-SEC-056,REQ-SEC-058 -->
## PHASE 3 — SVC-API

API count = 39 ≥ 8 → split by threshold, grouped CRUD / SEARCH / INT. Every block names its entity and the
operation it answers, every DBF it writes on its Request or Orchestration line, and its concurrency guard. Every
secured block's Security line refers to the CORE gate; for a SERVICE token that gate first runs the live bearer check
(QR-SEC-040, QR-SEC-044; a rejection is audited through QR-SEC-048).

<!-- SUB:SVC-API-SEARCH:START traces=REQ-SEC-009,REQ-SEC-012,REQ-SEC-013,REQ-SEC-014,REQ-SEC-016,REQ-SEC-021,REQ-SEC-022,REQ-SEC-025,REQ-SEC-027,REQ-SEC-040,REQ-SEC-041,REQ-SEC-042,REQ-SEC-053,REQ-SEC-056 -->
### SUB — SVC-API-SEARCH (read-only)

<!-- API:API-SEC-005:START traces=REQ-SEC-009,REQ-SEC-040,REQ-SEC-041,DBF-SEC-002,DBF-SEC-003,DBF-SEC-005,DBF-SEC-006,DBF-SEC-007,DBF-SEC-105 -->
### API-SEC-005 — search users (MODIFIED v2 — principal type)
Entity       : ENT-SEC-001 · operation VIEW (search)
Endpoint     : POST /api/v1/sec/users/search   verb: POST
Layers       : `UserController.search` → `UserService.search`
Request      : body `UserSearchRequest` (BaseSearchContractRequest) — filters over username/email (LIKE; DBF-SEC-002, DBF-SEC-003), fullName (LIKE over DBF-SEC-005 or DBF-SEC-006), statusCode (EXACT; DBF-SEC-007) and, v2, principalTypeCode (EXACT; DBF-SEC-105); page, size, sortField, sortDirection
Response     : 200 · `ApiResponse<Page<UserResponse>>` — userPk, username, email, fullNameAr, fullNameEn, statusCode, principalTypeCode (v2), lastLoginAt; never passwordHash
Validations  : none (read-only); an unknown sortField → SEC-400-INVALID-SORT
Errors       : SEC-400-INVALID-SORT, SEC-403-FORBIDDEN
Orchestration: load (QR-SEC-005) → map → return; the principal-type label is resolved by the frontend from the code (REQ-SEC-040)
Repository   : QR-SEC-005 · join NONE · READ_ONLY
Concurrency  : NONE — read-only
Security     : screen SEC_USERS · `PERM_SEC_USERS_VIEW`, checked before processing by the CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : fullNameAr and fullNameEn both returned
<!-- API:API-SEC-005:END -->

<!-- API:API-SEC-032:START traces=REQ-SEC-072,DBF-SEC-001,DBF-SEC-002,DBF-SEC-003,DBF-SEC-005,DBF-SEC-006,DBF-SEC-007,DBF-SEC-009,DBF-SEC-105,DBF-SEC-026,DBF-SEC-027 -->
### API-SEC-032 — read user (ADDED v2 — declared, ADR-SEC-038)
Entity       : ENT-SEC-001, ENT-SEC-003 · operation VIEW (read — the user detail and its roles tab)
Endpoint     : GET /api/v1/sec/users/{id}   verb: GET
Layers       : `UserController.getById` → `UserService.getById`
Request      : path `id` (DBF-SEC-001)
Response     : 200 · `ApiResponse<UserResponse>` — userPk, username, email, fullNameAr, fullNameEn, statusCode, principalTypeCode, lastLoginAt, isActiveFl, audit fields, roles[{roleId, code, nameAr, nameEn}]; never passwordHash; never a credential (API-SEC-028 lists those)
Validations  : the user exists
Errors       : SEC-404-USER, SEC-403-FORBIDDEN
Orchestration: load (QR-SEC-040) → load its roles (QR-SEC-050) → map → return
Repository   : QR-SEC-040, QR-SEC-050 · join required on QR-SEC-050 (ADR-SEC-038) · READ_ONLY
Concurrency  : NONE — read-only
Security     : screen SEC_USERS · `PERM_SEC_USERS_VIEW`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : both full names and both role names
<!-- API:API-SEC-032:END -->

<!-- API:API-SEC-028:START traces=REQ-SEC-056,REQ-SEC-053,DBF-SEC-106,DBF-SEC-108,DBF-SEC-109,DBF-SEC-110,DBF-SEC-111,DBF-SEC-112,DBF-SEC-113,DBF-SEC-114 -->
### API-SEC-028 — list a service account's credentials (ADDED v2)
Entity       : ENT-SEC-014, ENT-SEC-001 · operation VIEW (list — the credentials tab)
Endpoint     : GET /api/v1/sec/users/{id}/credentials   verb: GET
Layers       : `ServiceAccountCredentialController.list` → `ServiceAccountCredentialService.list`
Request      : path `id` (the account, DBF-SEC-112)
Response     : 200 · `ApiResponse<List<ServiceAccountCredentialResponse>>` — per credential: serviceAccountCredentialPk, description, createdAt (issued at), createdBy, lastUsedAt, revokedAt, revokedBy; newest first. No response field holds the secret or secretHash (REQ-SEC-053, AC-SEC-055). A HUMAN user holds no credential, so its list is empty
Validations  : the user exists
Errors       : SEC-404-USER, SEC-403-FORBIDDEN
Orchestration: load the user (QR-SEC-040) → load its credentials (QR-SEC-042) → map → return (AC-SEC-059)
Repository   : QR-SEC-040, QR-SEC-042 · join NONE · READ_ONLY
Concurrency  : NONE — read-only
Security     : screen SEC_USERS · `PERM_SEC_USERS_VIEW` (SCR-REQ-SEC-004 B4: the credentials list sits under VIEW), CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : no stored text beyond the free-text description
<!-- API:API-SEC-028:END -->

<!-- API:API-SEC-036:START traces=REQ-SEC-076,DBF-SEC-098,DBF-SEC-099,DBF-SEC-100,DBF-SEC-101,DBF-SEC-102 -->
### API-SEC-036 — search sign-up requests (ADDED v2 — declared, ADR-SEC-038)
Entity       : ENT-SEC-013 · operation VIEW (search — the "Pending sign-ups" tab)
Endpoint     : POST /api/v1/sec/signup-requests/search   verb: POST
Layers       : `SignupRequestController.search` → `SignupRequestService.search`
Request      : body `SignupRequestSearchRequest` (BaseSearchContractRequest) — filters over statusCode (EXACT; DBF-SEC-102), email (LIKE; DBF-SEC-098), fullName (LIKE over DBF-SEC-099 or DBF-SEC-100); page, size, sortField, sortDirection
Response     : 200 · `ApiResponse<Page<SignupRequestResponse>>` — every field
Validations  : an unknown sortField → SEC-400-INVALID-SORT
Errors       : SEC-400-INVALID-SORT, SEC-403-FORBIDDEN
Orchestration: load (QR-SEC-054) → map → return
Repository   : QR-SEC-054 · join NONE · READ_ONLY
Concurrency  : NONE — read-only
Security     : screen SEC_USERS · `PERM_SEC_USERS_VIEW`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : both full names returned
<!-- API:API-SEC-036:END -->

<!-- API:API-SEC-037:START traces=REQ-SEC-034,DBF-SEC-003,DBF-SEC-005,DBF-SEC-006,DBF-SEC-007 -->
### API-SEC-037 — read a user's contact details across modules (ADDED v2 — ADR-SEC-040)
Entity       : ENT-SEC-001 · operation VIEW (cross-module read)
Endpoint     : in-process — `com.erp.sec.crossmodule.SecUserDirectoryApi.findContact(Long)`   verb: — (no HTTP surface)
Layers       : `SecUserDirectoryApi` → `UserService.findContact`
Request      : the user's id
Response     : `UserContact` — email (DBF-SEC-003), both display names (DBF-SEC-005, DBF-SEC-006), active state (DBF-SEC-007), and nothing else
Validations  : an unknown id yields an empty result; the caller decides what that means
Errors       : none — an in-process call raises no HTTP error and has no network failure path
Orchestration: load (QR-SEC-040) → map to the read-model → return
Repository   : QR-SEC-040 · join NONE · READ_ONLY
Concurrency  : NONE — read-only
Security     : no CORE gate — the caller is a module in the same deployable, already authorized for its own request; this surface exposes no more than REQ-SEC-034 names
Localization : both display names returned
Consumers    : NOTIF (recipient contact read). A service account's contact email is returned like any user's (ADR-SEC-032)
<!-- API:API-SEC-037:END -->

<!-- API:API-SEC-038:START traces=REQ-SEC-035,DBF-SEC-026,DBF-SEC-027,DBF-SEC-072 -->
### API-SEC-038 — read the holders of a permission code across modules (ADDED v2 — ADR-SEC-040)
Entity       : ENT-SEC-001, ENT-SEC-003, ENT-SEC-006, ENT-SEC-009 · operation VIEW (cross-module read)
Endpoint     : in-process — `com.erp.sec.crossmodule.SecUserDirectoryApi.findUserIdsHoldingPermission(String)`   verb: — (no HTTP surface)
Layers       : `SecUserDirectoryApi` → `RoleActionGrantRepository.findUserIdsHoldingPermission`
Request      : a permission code
Response     : the set of user ids currently holding that code through an active role
Validations  : an unregistered code yields an empty set, never an error
Errors       : none — an in-process call raises no HTTP error and has no network failure path
Orchestration: resolve the code, walk user → role → action grant (QR-SEC-039) → return the ids
Repository   : QR-SEC-039 · join user → user_role → role_action_grant → action · READ_ONLY
Concurrency  : NONE — read-only
Security     : no CORE gate, as for API-SEC-037
Localization : none — ids only
Consumers    : none since FIN retired its separation-of-duties reader on 2026-09-12. The surface stays because REQ-SEC-035 states it; a service account holding the code through its roles is included
<!-- API:API-SEC-038:END -->

<!-- API:API-SEC-039:START traces=REQ-SEC-016,REQ-SEC-017,DBF-SEC-030,DBF-SEC-031 -->
### API-SEC-039 — is this module code registered? (ADDED v2 — ADR-SEC-040, written down for the first time)
Entity       : ENT-SEC-004 · operation VIEW (cross-module read)
Traces       : REQ-SEC-016, REQ-SEC-017 — the registry those requirements write is what this reads back; DBF-SEC-030 (pk), DBF-SEC-031 (module code)
Endpoint     : in-process — `com.erp.sec.crossmodule.SecModuleRegistryApi`   verb: — (no HTTP surface)
Layers       : `SecModuleRegistryApi` → `ModuleRegistryRepository`
Request      : a module code
Response     : whether that code is registered
Validations  : none
Errors       : none — an in-process call raises no HTTP error and has no network failure path
Orchestration: existence read (QR-SEC-035) → return
Repository   : QR-SEC-035 · join NONE · READ_ONLY
Concurrency  : NONE — read-only
Security     : no CORE gate, as for API-SEC-037
Localization : none
Consumers    : MDL — XM-MDL-001. MDL's plan noted that SEC's artifacts never wrote this surface down; this block is that correction
<!-- API:API-SEC-039:END -->

<!-- API:API-SEC-012:START traces=REQ-SEC-012,DBF-SEC-015,DBF-SEC-016,DBF-SEC-017,DBF-SEC-020 -->
### API-SEC-012 — search roles
Entity       : ENT-SEC-002 · operation VIEW (search)
Endpoint     : POST /api/v1/sec/roles/search   verb: POST
Layers       : `RoleController.search` → `RoleService.search`
Request      : body `RoleSearchRequest` (BaseSearchContractRequest) — filters over code (DBF-SEC-015), isActiveFl (DBF-SEC-020), name (LIKE over DBF-SEC-016 or DBF-SEC-017); page, size, sortField, sortDirection
Response     : 200 · `ApiResponse<Page<RoleResponse>>` — rolePk, code, nameAr, nameEn, descriptionAr, descriptionEn, isActiveFl
Validations  : an unknown sortField → SEC-400-INVALID-SORT
Errors       : SEC-400-INVALID-SORT, SEC-403-FORBIDDEN
Orchestration: load (QR-SEC-012) → map → return
Repository   : QR-SEC-012 · join NONE · READ_ONLY
Concurrency  : NONE — read-only
Security     : screen SEC_ROLES · `PERM_SEC_ROLES_VIEW`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : both names and both descriptions returned
<!-- API:API-SEC-012:END -->

<!-- API:API-SEC-033:START traces=REQ-SEC-073,DBF-SEC-014,DBF-SEC-015,DBF-SEC-016,DBF-SEC-017,DBF-SEC-018,DBF-SEC-019,DBF-SEC-020 -->
### API-SEC-033 — read role (ADDED v2 — declared, ADR-SEC-038)
Entity       : ENT-SEC-002 · operation VIEW (read — the role master record)
Endpoint     : GET /api/v1/sec/roles/{id}   verb: GET
Layers       : `RoleController.getById` → `RoleService.getById`
Request      : path `id` (DBF-SEC-014)
Response     : 200 · `ApiResponse<RoleResponse>` — every field
Validations  : the role exists
Errors       : SEC-404-ROLE, SEC-403-FORBIDDEN
Orchestration: load (QR-SEC-051) → map → return
Repository   : QR-SEC-051 · join NONE · READ_ONLY
Concurrency  : NONE — read-only
Security     : screen SEC_ROLES · `PERM_SEC_ROLES_VIEW`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : both names and both descriptions
<!-- API:API-SEC-033:END -->

<!-- API:API-SEC-035:START traces=REQ-SEC-075,DBF-SEC-061,DBF-SEC-062,DBF-SEC-066,DBF-SEC-067,DBF-SEC-071,DBF-SEC-072 -->
### API-SEC-035 — read a role's grant tree (ADDED v2 — declared, ADR-SEC-038)
Entity       : ENT-SEC-007, ENT-SEC-008, ENT-SEC-009 · operation VIEW (read — the 3-level grant editor)
Endpoint     : GET /api/v1/sec/roles/{id}/grants   verb: GET
Layers       : `RoleGrantController.grants` → `RoleGrantService.grants`
Request      : path `id` (the role; DBF-SEC-061, DBF-SEC-066, DBF-SEC-071 are filtered by it)
Response     : 200 · `ApiResponse<RoleGrantTreeResponse>` — the role (rolePk, code, nameAr, nameEn) and modules[] → screens[] → actions[], each node with its registry code, both names and its grant time; actions carry actionCode and permissionCode
Validations  : the role exists
Errors       : SEC-404-ROLE, SEC-403-FORBIDDEN
Orchestration: load the role (QR-SEC-051) → load its grants with their registry rows (QR-SEC-053) → assemble module → screen → action → return
Repository   : QR-SEC-051, QR-SEC-053 · join required on QR-SEC-053, inside SEC (ADR-SEC-038) · READ_ONLY
Concurrency  : NONE — read-only
Security     : screen SEC_ROLES · `PERM_SEC_ROLES_VIEW`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : nameAr and nameEn at every level
<!-- API:API-SEC-035:END -->

<!-- API:API-SEC-021:START traces=REQ-SEC-016,DBF-SEC-031,DBF-SEC-040,DBF-SEC-050 -->
### API-SEC-021 — search registry
Entity       : ENT-SEC-004, ENT-SEC-005, ENT-SEC-006 · operation VIEW (search, read — the registry tree)
Endpoint     : POST /api/v1/sec/registry/search   verb: POST
Layers       : `RegistryController.search` → `RegistryService.search`
Request      : body `RegistrySearchRequest` (BaseSearchContractRequest) — filters over the module code (DBF-SEC-031) and pageCode (LIKE, on the child screen; DBF-SEC-040); page, size, sortField, sortDirection
Response     : 200 · `ApiResponse<Page<RegistryRowResponse>>` — a module row with its active screens and, per screen, its active actions and their permission codes (DBF-SEC-050)
Validations  : an unknown sortField → SEC-400-INVALID-SORT
Errors       : SEC-400-INVALID-SORT, SEC-403-FORBIDDEN
Orchestration: load (QR-SEC-021 — module → screen → action, all inside SEC) → assemble the tree → return
Repository   : QR-SEC-021 · join inside SEC (module/screen/action), as delivered · READ_ONLY
Concurrency  : NONE — read-only
Security     : screen SEC_MODULE_REGISTRY · `PERM_SEC_MODULE_REGISTRY_VIEW`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : nameAr and nameEn at every level
<!-- API:API-SEC-021:END -->

<!-- API:API-SEC-022:START traces=REQ-SEC-022,REQ-SEC-023,REQ-SEC-042,DBF-SEC-007,DBF-SEC-105,DBF-SEC-076,DBF-SEC-081,DBF-SEC-084,DBF-SEC-086 -->
### API-SEC-022 — dashboard summary (MODIFIED v2 — users overview per principal type)
Entity       : ENT-SEC-001, ENT-SEC-002, ENT-SEC-010, ENT-SEC-011 · operation VIEW (read — aggregate figures)
Endpoint     : GET /api/v1/sec/dashboard   verb: GET
Layers       : `DashboardController.summary` → `DashboardService.summary`
Request      : none
Response     : 200 · `ApiResponse<DashboardResponse>` — six widgets, each present only when the caller holds that widget's source-screen VIEW (an omitted field, never a zeroed one): `usersOverview` — v2: `byPrincipalType[{principalTypeCode, total, active, disabled}]` (HUMAN and SERVICE both always present, AC-SEC-042) plus `pendingSignups`; `failedLogins24h{count}`; `activeSessions{count}`; `recentActivity{last N AuditLogEntry}`; `rolesPermissionsSummary{roleCount, privilegedRoleCount, usersPerRole}`; `onboardingFunnel{pendingSignups, stalledCount}`
Validations  : none — the endpoint filters its own output by REQ-SEC-023 rather than rejecting the call
Errors       : SEC-403-FORBIDDEN
Orchestration: resolve the caller's effective permissions (the CORE gate's read) → for each widget whose source-screen VIEW the caller holds (usersOverview needs PERM_SEC_USERS_VIEW, activeSessions PERM_SEC_SESSIONS_VIEW, recentActivity and failedLogins24h PERM_SEC_AUDIT_LOG_VIEW, rolesPermissionsSummary PERM_SEC_ROLES_VIEW) compute it live (QR-SEC-022) → assemble → return. The v1 operative definitions stand unchanged and still await a human's confirmation (REQ-SEC-022 Note): N = 10 most recent entries; stalled = PENDING for more than 7 days; privileged role = holds at least one non-VIEW action grant
Repository   : QR-SEC-022 · join NONE except the v1 roles summary · READ_ONLY
Concurrency  : NONE — read-only; every figure is computed at request time
Security     : screen SEC_DASHBOARD · `PERM_SEC_DASHBOARD_VIEW` (gateway) plus each widget's source-screen VIEW, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : recentActivity entries carry detailsAr and detailsEn; principal-type labels come from the code
<!-- API:API-SEC-022:END -->

<!-- API:API-SEC-023:START traces=REQ-SEC-025,DBF-SEC-084,DBF-SEC-085,DBF-SEC-086 -->
### API-SEC-023 — search audit log
Entity       : ENT-SEC-011 · operation VIEW (search)
Endpoint     : POST /api/v1/sec/audit-log/search   verb: POST
Layers       : `AuditLogController.search` → `AuditLogService.search`
Request      : body `AuditLogEntrySearchRequest` (BaseSearchContractRequest) — filters over eventTypeCode (DBF-SEC-084), occurredAt range (DBF-SEC-086), actorUserId (EXACT; DBF-SEC-085); page, size, sortField, sortDirection. v2: the six SERVICE_* event codes filter like any other
Response     : 200 · `ApiResponse<Page<AuditLogEntryResponse>>` — every field, unmodified
Validations  : an unknown sortField → SEC-400-INVALID-SORT
Errors       : SEC-400-INVALID-SORT, SEC-403-FORBIDDEN
Orchestration: load (QR-SEC-023) → return unmodified (REQ-SEC-025)
Repository   : QR-SEC-023 · join NONE · READ_ONLY
Concurrency  : NONE — read-only
Security     : screen SEC_AUDIT_LOG · `PERM_SEC_AUDIT_LOG_VIEW`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : detailsAr and detailsEn returned
<!-- API:API-SEC-023:END -->

<!-- API:API-SEC-025:START traces=REQ-SEC-027,DBF-SEC-076,DBF-SEC-079,DBF-SEC-081 -->
### API-SEC-025 — list active sessions
Entity       : ENT-SEC-010 · operation VIEW (search)
Endpoint     : POST /api/v1/sec/sessions/search   verb: POST
Layers       : `SessionController.search` → `SessionService.search`
Request      : body `ActiveSessionSearchRequest` (BaseSearchContractRequest) — filters over ipAddress and userId (EXACT; DBF-SEC-076); page, size, sortField, sortDirection
Response     : 200 · `ApiResponse<Page<ActiveSessionResponse>>` — activeSessionPk, userId, username, startedAt, lastActivityAt, ipAddress; never tokenRef. v2: a service account never appears — it has no session
Validations  : terminatedAt IS NULL is always applied server-side (DBF-SEC-081), never a client filter
Errors       : SEC-400-INVALID-SORT, SEC-403-FORBIDDEN
Orchestration: load (QR-SEC-025) → map → return
Repository   : QR-SEC-025 · join NONE · READ_ONLY
Concurrency  : NONE — read-only
Security     : screen SEC_SESSIONS · `PERM_SEC_SESSIONS_VIEW`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : the username is shown as stored
<!-- API:API-SEC-025:END -->

<!-- API:API-SEC-027:START traces=REQ-SEC-021,REQ-SEC-032,REQ-SEC-033,REQ-SEC-044,REQ-SEC-045,DBF-SEC-031,DBF-SEC-040,DBF-SEC-061,DBF-SEC-066 -->
### API-SEC-027 — effective menu
Entity       : ENT-SEC-004, ENT-SEC-005, ENT-SEC-007, ENT-SEC-008 · operation VIEW (read — the navigation tree)
Endpoint     : GET /api/v1/sec/menu   verb: GET
Layers       : `MenuController.effective` → `MenuService.effective`
Request      : none — the caller is the authenticated principal
Response     : 200 · `ApiResponse<List<ModuleMenuResponse>>` — only the modules the caller's effective grants hold, each with only its effective granted screens (REQ-SEC-021, REQ-SEC-032)
Validations  : none — the endpoint's behaviour is the filter
Errors       : none beyond the CORE gate
Orchestration: resolve the caller's roles and union their grants (QR-SEC-027; module grants DBF-SEC-061 → modules DBF-SEC-031; screen grants DBF-SEC-066 → screens DBF-SEC-040) → assemble modules → screens → return. The same read builds every caller's authorities in the CORE gate, which is why this block carries REQ-SEC-033, and in v2 REQ-SEC-044 and REQ-SEC-045: a service account's effective permissions are that same union of its roles' grants, and a request outside it is denied exactly as a human's is. A service account has no navigation UI, so it has no reason to call this endpoint; if it does, it receives its own tree like any principal
Repository   : QR-SEC-027 · join inside SEC (grant tables to registry tables), as delivered · READ_ONLY
Concurrency  : NONE — read-only
Security     : no page code of its own (SRS SCR-REQ-SEC-010 B4) — every authenticated caller; its content is the boundary. CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : nameAr and nameEn at module and screen level
<!-- API:API-SEC-027:END -->
<!-- SUB:SVC-API-SEARCH:END -->

<!-- SUB:SVC-API-CRUD:START traces=REQ-SEC-004,REQ-SEC-005,REQ-SEC-009,REQ-SEC-010,REQ-SEC-011,REQ-SEC-012,REQ-SEC-013,REQ-SEC-014,REQ-SEC-015,REQ-SEC-020,REQ-SEC-028,REQ-SEC-030,REQ-SEC-031,REQ-SEC-036,REQ-SEC-037,REQ-SEC-038,REQ-SEC-039,REQ-SEC-043,REQ-SEC-044,REQ-SEC-051,REQ-SEC-052,REQ-SEC-058,REQ-SEC-060,REQ-SEC-061,REQ-SEC-062 -->
### SUB — SVC-API-CRUD (mutations)

<!-- API:API-SEC-006:START traces=REQ-SEC-009,REQ-SEC-036,REQ-SEC-037,REQ-SEC-070,DBF-SEC-002,DBF-SEC-003,DBF-SEC-004,DBF-SEC-005,DBF-SEC-006,DBF-SEC-007,DBF-SEC-009,DBF-SEC-105 -->
### API-SEC-006 — create user (MODIFIED v2 — or a service account)
Entity       : ENT-SEC-001 · operation CREATE (a human user, or v2 a service account)
Endpoint     : POST /api/v1/sec/users   verb: POST
Layers       : `UserController.create` → `UserService.create`; the SERVICE shape in `UserDomain.newServiceAccount`
Request      : body `UserCreateRequest` — username DBF-SEC-002 (String, required, ≤ 100; for SERVICE also the client identifier) · email DBF-SEC-003 (String, required, ≤ 255, valid email; for SERVICE the responsible team's contact address) · fullNameAr DBF-SEC-005 and fullNameEn DBF-SEC-006 (String, required, ≤ 200; for SERVICE the integration's name) · v2 principalTypeCode DBF-SEC-105 (String, optional, HUMAN or SERVICE, default HUMAN) · password → hashed into DBF-SEC-004 (required for HUMAN; not read for SERVICE) · statusCode DBF-SEC-007 (optional for HUMAN, default ACTIVE; not read for SERVICE); excluded: userPk (from SEQ_SEC_USER), lastLoginAt, isActiveFl, audit
Response     : 201 · `ApiResponse<UserResponse>` — never passwordHash
Validations  : principalTypeCode within PRINCIPAL_TYPE → VALIDATION_ERROR (CORE "Lookup values"); username and email unique (QR-SEC-033) → SEC-409-USER-DUP
Errors       : VALIDATION_ERROR, SEC-409-USER-DUP, SEC-403-FORBIDDEN
Orchestration: (writes DBF-SEC-004 passwordHash — HUMAN: the hash of the submitted password; SERVICE: the hash of an unusable random value that is never logged, returned or transmitted, ADR-SEC-032 — and DBF-SEC-007 statusCode, DBF-SEC-009 isActiveFl=TRUE, DBF-SEC-105 principalTypeCode) validate → check uniqueness (QR-SEC-033) → persist, flushed (QR-SEC-006) → for SERVICE: no role assignment and no credential are created (REQ-SEC-037, AC-SEC-036) and one SERVICE_ACCOUNT_CREATED entry is appended with the administrator as actor and the new account as target (DBF-SEC-084, DBF-SEC-086, DBF-SEC-087; REQ-SEC-070) → return. A HUMAN creation appends no audit entry, as in v1 (SRS A6 names no such event)
Repository   : QR-SEC-033, QR-SEC-006 · join NONE · READ_WRITE
Concurrency  : two simultaneous creates of one username or email both pass QR-SEC-033; UQ_SEC_USER_USERNAME and UQ_SEC_USER_EMAIL refuse the second insert at flush. The pre-check is the friendly path, the constraints are the guarantee; the refused request answers the shared DATA_INTEGRITY_VIOLATION (409) rather than SEC-409-USER-DUP
Security     : screen SEC_USERS · `PERM_SEC_USERS_CREATE` (SCR-REQ-SEC-004 B4: creating a service account is CREATE), CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : both full names required; the SERVICE-only wording of the email label is the frontend's (ADR-SEC-032)
<!-- API:API-SEC-006:END -->

<!-- API:API-SEC-007:START traces=REQ-SEC-009,REQ-SEC-039,DBF-SEC-003,DBF-SEC-005,DBF-SEC-006,DBF-SEC-105 -->
### API-SEC-007 — update user (MODIFIED v2 — principal type fixed)
Entity       : ENT-SEC-001 · operation UPDATE
Endpoint     : PUT /api/v1/sec/users/{id}   verb: PUT
Layers       : `UserController.update` → `UserService.update`; the decision in `UserDomain.assertPrincipalTypeUnchanged`
Request      : path `id`; body `UserUpdateRequest` — email DBF-SEC-003 · fullNameAr DBF-SEC-005 · fullNameEn DBF-SEC-006 · v2 principalTypeCode DBF-SEC-105 (optional; compared, never written); excluded: userPk, username, passwordHash, statusCode, isActiveFl, audit
Response     : 200 · `ApiResponse<UserResponse>`
Validations  : RULE-SEC-012 — trigger: on update (user). Statement: "The system shall prevent any update that changes an existing user's principal type." Message: ar "لا يمكن تغيير نوع الأساس الأمني بعد إنشائه" · en "The principal type cannot be changed after creation" → SEC-409-PRINCIPAL-TYPE-FIXED (AC-SEC-039; the user is left unchanged). Email unique excluding this user (QR-SEC-033) → SEC-409-USER-DUP
Errors       : SEC-404-USER, SEC-409-PRINCIPAL-TYPE-FIXED, SEC-409-USER-DUP, DATA_INTEGRITY_VIOLATION, SEC-403-FORBIDDEN
Orchestration: load (QR-SEC-040) → when principalTypeCode is sent, hand the stored and submitted values to `assertPrincipalTypeUnchanged` → check email uniqueness (QR-SEC-033) → update the three fields and updatedBy/updatedAt (QR-SEC-007) → return
Repository   : QR-SEC-040, QR-SEC-033, QR-SEC-007 · join NONE · READ_WRITE
Concurrency  : UQ_SEC_USER_EMAIL is the guarantee for a concurrent email change (as API-SEC-006); the principal type is never written, so no race can change it
Security     : screen SEC_USERS · `PERM_SEC_USERS_UPDATE`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : both full names updatable; RULE-SEC-012's message in ar and en
<!-- API:API-SEC-007:END -->

<!-- API:API-SEC-008:START traces=REQ-SEC-010,REQ-SEC-043,REQ-SEC-069,DBF-SEC-026,DBF-SEC-027,DBF-SEC-028,DBF-SEC-029 -->
### API-SEC-008 — assign roles to user (MODIFIED v2 — audit target of a service account)
Entity       : ENT-SEC-001, ENT-SEC-003, ENT-SEC-002 · operation UPDATE (assign roles)
Endpoint     : PUT /api/v1/sec/users/{id}/roles   verb: PUT
Layers       : `UserController.assignRoles` → `UserRoleService.assign`
Request      : path `id` (→ userId DBF-SEC-026); body `{roleIds: [Long]}` (→ roleId DBF-SEC-027, each an active role)
Response     : 200 · `ApiResponse<UserResponse>` with `roles: [{roleId, code, nameAr, nameEn}]` (QR-SEC-050)
Validations  : RULE-SEC-005 (full text under ENT-SEC-009; inert) → SEC-409-SOD-CONFLICT
Errors       : SEC-404-USER, SEC-404-ROLE, SEC-409-SOD-CONFLICT, SEC-403-FORBIDDEN
Orchestration: (sets DBF-SEC-026 userId, DBF-SEC-028 assignedBy and DBF-SEC-029 assignedAt on every new row) load the user under a write lock (QR-SEC-040, taken PESSIMISTIC_WRITE — the same FOR UPDATE form QR-SEC-041 uses) and the roles → read the user's current role set (QR-SEC-050) and compute the ROLE_ASSIGNED/ROLE_REVOKED difference against the submitted set → check RULE-SEC-005 (QR-SEC-031) → replace the assignment set (QR-SEC-008) → append ROLE_ASSIGNED per added role and ROLE_REVOKED per removed role, each with the account as target (DBF-SEC-084, DBF-SEC-086, DBF-SEC-087 — REQ-SEC-069; for a service account exactly as for a human, REQ-SEC-043) → return with the roles (QR-SEC-050)
Repository   : QR-SEC-040, QR-SEC-031, QR-SEC-008, QR-SEC-050 · join NONE except QR-SEC-050 · READ_WRITE
Concurrency  : the user row lock (QR-SEC-040, PESSIMISTIC_WRITE) serializes two replacements of one user's role set: the second replacement waits, then reads the first one's result (QR-SEC-050) and computes its ROLE_ASSIGNED/ROLE_REVOKED difference from it, so neither replace-and-diff step reads a role set another concurrent call is about to overwrite. UQ_SEC_USER_ROLE_USER_ROLE additionally refuses a duplicate row; RULE-SEC-005 is inert, so no check-then-write decision beyond the role set is at risk
Security     : screen SEC_USERS · `PERM_SEC_USERS_UPDATE`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : both role names returned
<!-- API:API-SEC-008:END -->

<!-- API:API-SEC-009:START traces=REQ-SEC-011,REQ-SEC-060,REQ-SEC-071,DBF-SEC-007,DBF-SEC-009,DBF-SEC-081,DBF-SEC-082 -->
### API-SEC-009 — deactivate user (MODIFIED v2 — a service account's credentials and tokens)
Entity       : ENT-SEC-001, ENT-SEC-010 · operation UPDATE (deactivate)
Endpoint     : DELETE /api/v1/sec/users/{id}   verb: DELETE (soft — profile delete_semantics)
Layers       : `UserController.deactivate` → `UserService.deactivate`
Request      : path `id`
Response     : 200 · `ApiResponse<UserStatusResponse>` `{userPk, statusCode: "DISABLED"}`
Validations  : the user exists
Errors       : SEC-404-USER, SEC-403-FORBIDDEN
Orchestration: (writes DBF-SEC-007 statusCode=DISABLED, DBF-SEC-009 isActiveFl=FALSE, and on each open session DBF-SEC-081 terminatedAt and DBF-SEC-082 terminatedBy) load (QR-SEC-040) → set DISABLED (QR-SEC-009) → terminate every open session of the user (QR-SEC-026, bulk) and append SESSION_TERMINATED per session (v1) → v2, for a SERVICE principal: it has no session; nothing is written to its credentials — from the next request on, the CORE bearer check and API-SEC-031 reject every credential and token of the account because its status is read live (REQ-SEC-060, AC-SEC-063) — and one SERVICE_ACCOUNT_DEACTIVATED entry is appended with the administrator as actor and the account as target (DBF-SEC-084, DBF-SEC-086, DBF-SEC-087; REQ-SEC-071) → return
Repository   : QR-SEC-040, QR-SEC-009, QR-SEC-026 · join NONE · READ_WRITE
Concurrency  : repeating the deactivation is harmless (the same end state). A credential issuance running at the same moment is serialized with it by API-SEC-029's locking read of the same row (QR-SEC-041)
Security     : screen SEC_USERS · `PERM_SEC_USERS_UPDATE` (deactivate is UPDATE, SCR-REQ-SEC-004 B4), CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : no message beyond the confirmation
<!-- API:API-SEC-009:END -->

<!-- API:API-SEC-010:START traces=REQ-SEC-031,REQ-SEC-062,REQ-SEC-079,DBF-SEC-007,DBF-SEC-009,DBF-SEC-084,DBF-SEC-086,DBF-SEC-087 -->
### API-SEC-010 — activate a disabled user (MODIFIED v2 — a service account's credentials; revise pass G6 — reactivation audit)
Entity       : ENT-SEC-001, ENT-SEC-011 · operation UPDATE (activate)
Endpoint     : PATCH /api/v1/sec/users/{id}   verb: PATCH
Layers       : `UserController.reactivate` → `UserService.reactivate`
Request      : path `id`
Response     : 200 · `ApiResponse<UserStatusResponse>` `{userPk, statusCode: "ACTIVE"}`
Validations  : the user is DISABLED (A7) → otherwise SEC-409-INVALID-TRANSITION
Errors       : SEC-404-USER, SEC-409-INVALID-TRANSITION, SEC-403-FORBIDDEN
Orchestration: (writes DBF-SEC-007 statusCode=ACTIVE, DBF-SEC-009 isActiveFl=TRUE) load (QR-SEC-040) → verify DISABLED → activate (QR-SEC-010) → return. v2, for a SERVICE principal: its never-revoked credentials authenticate again from the next request, and a revoked one stays rejected (REQ-SEC-062, AC-SEC-065) — nothing is written to them. Revise pass (G6, ADR-SEC-047): when the reactivated principal is SERVICE, append one SERVICE_ACCOUNT_REACTIVATED audit entry with the administrator as actor and the service account as target (DBF-SEC-084, DBF-SEC-086, DBF-SEC-087; REQ-SEC-079, AC-SEC-085) — the act that restores machine access is now audited, closing the one unaudited step of the account lifecycle (POL-SEC-019, POL-SEC-009); a HUMAN reactivation appends nothing, as in v1
Repository   : QR-SEC-040, QR-SEC-010 · join NONE · READ_WRITE
Concurrency  : two simultaneous activations both reach ACTIVE — the same end state, no invariant at risk
Security     : screen SEC_USERS · `PERM_SEC_USERS_UPDATE`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : SEC-409-INVALID-TRANSITION in ar and en (Error Catalog)
<!-- API:API-SEC-010:END -->

<!-- API:API-SEC-011:START traces=REQ-SEC-004,REQ-SEC-005,REQ-SEC-038,DBF-SEC-102,DBF-SEC-103,DBF-SEC-104,DBF-SEC-001,DBF-SEC-002,DBF-SEC-003,DBF-SEC-004,DBF-SEC-105 -->
### API-SEC-011 — approve / reject sign-up (MODIFIED v2 — HUMAN principal; locking read)
Entity       : ENT-SEC-013, ENT-SEC-001 · operation UPDATE (approve, reject)
Endpoint     : PATCH /api/v1/sec/signup-requests/{id}   verb: PATCH
Layers       : `SignupRequestController.decide` → `SignupRequestService.decide`
Request      : path `id`; body `{decision: "APPROVE" | "REJECT"}`
Response     : 200 · APPROVE: `ApiResponse<UserResponse>` (the created user); REJECT: `ApiResponse<SignupRequestResponse>` (statusCode REJECTED)
Validations  : the request is PENDING (A7) → otherwise SEC-409-INVALID-TRANSITION
Errors       : SEC-404-SIGNUP, SEC-409-INVALID-TRANSITION, SEC-409-USER-DUP, DATA_INTEGRITY_VIOLATION, SEC-403-FORBIDDEN
Orchestration: (writes DBF-SEC-102 statusCode, DBF-SEC-103 reviewedBy, DBF-SEC-104 reviewedAt; on APPROVE also the new user's DBF-SEC-002 username = the request's email, DBF-SEC-003 email, DBF-SEC-005 fullNameAr, DBF-SEC-006 fullNameEn, DBF-SEC-004 passwordHash = an unusable random value, DBF-SEC-007 statusCode = ACTIVE, DBF-SEC-009 isActiveFl = TRUE, DBF-SEC-105 principalTypeCode = HUMAN) load the request under a write lock (QR-SEC-011) → verify PENDING → APPROVE: create the user (QR-SEC-006) with principal type HUMAN (REQ-SEC-038, AC-SEC-038), then mark the request APPROVED; REJECT: mark it REJECTED and create no user (REQ-SEC-005) → return. v1's open point on the approved account's first credential stands unchanged (REQ-SEC-004 Note)
Repository   : QR-SEC-011, QR-SEC-006 · join NONE · READ_WRITE
Concurrency  : two simultaneous decisions on one request — QR-SEC-011 loads the row PESSIMISTIC_WRITE, so the second waits, then reads a non-PENDING status and answers SEC-409-INVALID-TRANSITION; UQ_SEC_USER_USERNAME and UQ_SEC_USER_EMAIL remain the guarantee against a second user from one email
Security     : screen SEC_USERS (the "Pending sign-ups" tab) · `PERM_SEC_USERS_UPDATE`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : both full names carried from the request
<!-- API:API-SEC-011:END -->

<!-- API:API-SEC-029:START traces=REQ-SEC-051,REQ-SEC-052,REQ-SEC-054,REQ-SEC-067,DBF-SEC-105,DBF-SEC-007,DBF-SEC-106,DBF-SEC-107,DBF-SEC-108,DBF-SEC-112,DBF-SEC-113,DBF-SEC-114,REQ-SEC-078 -->
### API-SEC-029 — issue a credential (ADDED v2 — G7 maximum edge)
Entity       : ENT-SEC-014, ENT-SEC-001 · operation UPDATE (issue a credential — SCR-REQ-SEC-004 B4 places it under UPDATE)
Endpoint     : POST /api/v1/sec/users/{id}/credentials   verb: POST
Layers       : `ServiceAccountCredentialController.issue` → `ServiceAccountCredentialService.issue`; the decision in `ServiceAccountCredentialDomain.assertIssuable`
Request      : path `id` (the service account → DBF-SEC-112 userId); body `CredentialIssueRequest` — description DBF-SEC-108 (String, optional, ≤ 500 — where the secret is deployed); excluded: the PK (from SEQ_SEC_SVC_ACCOUNT_CRED), secretHash, lastUsedAt, revokedAt, revokedBy, audit
Response     : 201 · `ApiResponse<CredentialIssueResponse>` — serviceAccountCredentialPk, description, createdAt, and `secret` (this response only, REQ-SEC-051), with the message ar "انسخ السرّ الآن؛ لن يُعرض مرة أخرى" · en "Copy the secret now; it will not be shown again" (AC-SEC-052). Header `Cache-Control: no-store`
Validations  : RULE-SEC-008 — trigger: on create (credential issuance). Statement: "The system shall prevent issuing a credential when the target user is not of principal type SERVICE or is not ACTIVE." Message: ar "لا تُصدَر بيانات الاعتماد إلا لحساب خدمة نشط" · en "Credentials can be issued only for an active service account" → SEC-409-NOT-ACTIVE-SERVICE-ACCOUNT, and no credential is created (AC-SEC-053, AC-SEC-054). RULE-SEC-013 (REQ-SEC-078, AC-SEC-084, ADR-SEC-042): the system shall prevent issuing a credential when the target account already holds 10 unrevoked credentials. Message: ar "بلغ عدد بيانات الاعتماد النشطة الحد الأقصى (10)" · en "Active credential limit (10) reached" → SEC-409-CREDENTIAL-LIMIT. Rotation needs only 2 at a time, well under the cap
Errors       : SEC-404-USER, SEC-409-NOT-ACTIVE-SERVICE-ACCOUNT, SEC-409-CREDENTIAL-LIMIT, SEC-403-FORBIDDEN
Orchestration: (writes DBF-SEC-112 userId, DBF-SEC-107 secretHash, DBF-SEC-113 createdBy and DBF-SEC-114 createdAt; the PK DBF-SEC-106 from SEQ_SEC_SVC_ACCOUNT_CRED) load the account under a write lock (QR-SEC-041) and hand its principal type (DBF-SEC-105) and status (DBF-SEC-007) to `assertIssuable` → count the account's unrevoked credentials (QR-SEC-046, under the same QR-SEC-041 row lock, so a concurrent issuance cannot both pass the count check and exceed 10) → ≥10 → SEC-409-CREDENTIAL-LIMIT, no credential created → generate the secret (CORE "Service-account secrets") → compute its HMAC-SHA256 keyed by the platform's service-credential pepper (REQ-SEC-054, ADR-SEC-043 §1) → persist (QR-SEC-043) → append SERVICE_CREDENTIAL_ISSUED with the administrator as actor and the account as target and no secret in any field (DBF-SEC-084, DBF-SEC-086, DBF-SEC-087; REQ-SEC-067, AC-SEC-072) → return the secret once; it is not kept anywhere after the response is written
Repository   : QR-SEC-041, QR-SEC-046, QR-SEC-043 · join NONE · READ_WRITE
Concurrency  : issuance decides on the account's status and credential count and then writes a credential for it. A simultaneous deactivation (API-SEC-009) updates the same SEC_USER row, and QR-SEC-041 reads it PESSIMISTIC_WRITE, so the two are serialized: either the credential is stored before the account turns DISABLED (and is then rejected live, REQ-SEC-060), or the issuance sees DISABLED and answers SEC-409-NOT-ACTIVE-SERVICE-ACCOUNT. Two simultaneous issuances under the same row lock are serialized too, so the count check is never stale — the tenth and eleventh cannot both succeed
Security     : screen SEC_USERS · `PERM_SEC_USERS_UPDATE`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : the copy-now message, RULE-SEC-008's message and the credential-limit message in ar and en
<!-- API:API-SEC-029:END -->

<!-- API:API-SEC-030:START traces=REQ-SEC-058,REQ-SEC-061,REQ-SEC-068,DBF-SEC-110,DBF-SEC-111,DBF-SEC-112,DBF-SEC-115,DBF-SEC-116 -->
### API-SEC-030 — revoke a credential (ADDED v2)
Entity       : ENT-SEC-014 · operation UPDATE (revoke — SCR-REQ-SEC-004 B4 places it under UPDATE)
Endpoint     : DELETE /api/v1/sec/users/{id}/credentials/{credentialId}   verb: DELETE (soft — the row stays, revoked)
Layers       : `ServiceAccountCredentialController.revoke` → `ServiceAccountCredentialService.revoke`; the decision in `ServiceAccountCredentialDomain.assertRevocable`
Request      : path `id` (the account, matched against DBF-SEC-112) and `credentialId`
Response     : 200 · `ApiResponse<CredentialRevokeResponse>` `{serviceAccountCredentialPk, revokedAt, revokedBy}`
Validations  : the credential exists and belongs to the account in the path → otherwise SEC-404-CREDENTIAL. RULE-SEC-011 — trigger: on update (credential revocation). Statement: "The system shall prevent revoking a credential whose revocation time is already set." Message: ar "بيانات الاعتماد هذه ملغاة بالفعل" · en "This credential is already revoked" → SEC-409-CREDENTIAL-ALREADY-REVOKED; revokedAt and revokedBy stay unchanged (AC-SEC-064)
Errors       : SEC-404-CREDENTIAL, SEC-409-CREDENTIAL-ALREADY-REVOKED, SEC-403-FORBIDDEN
Orchestration: (writes DBF-SEC-110 revokedAt, DBF-SEC-111 revokedBy, DBF-SEC-115 updatedBy, DBF-SEC-116 updatedAt) load the credential (QR-SEC-044) → hand its owner and revocation time to `assertRevocable` → revoke conditionally (QR-SEC-045); 0 rows → SEC-409-CREDENTIAL-ALREADY-REVOKED → append SERVICE_CREDENTIAL_REVOKED with the administrator as actor and the account as target (DBF-SEC-084, DBF-SEC-086, DBF-SEC-087; REQ-SEC-068, AC-SEC-073) → return. From the next request on, the secret and every token issued from it are rejected (REQ-SEC-058, REQ-SEC-059 — the CORE bearer check and API-SEC-031 read revokedAt live)
Repository   : QR-SEC-044, QR-SEC-045 · join NONE · READ_WRITE
Concurrency  : two simultaneous revocations both read an active credential; QR-SEC-045 updates only WHERE revoked_at IS NULL, so exactly one succeeds and the other answers SEC-409-CREDENTIAL-ALREADY-REVOKED — the first revocation's time and actor stay the record (RULE-SEC-011)
Security     : screen SEC_USERS · `PERM_SEC_USERS_UPDATE`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : RULE-SEC-011's message in ar and en
<!-- API:API-SEC-030:END -->

<!-- API:API-SEC-013:START traces=REQ-SEC-012,DBF-SEC-015,DBF-SEC-016,DBF-SEC-017,DBF-SEC-018,DBF-SEC-019,DBF-SEC-020 -->
### API-SEC-013 — create role
Entity       : ENT-SEC-002 · operation CREATE
Endpoint     : POST /api/v1/sec/roles   verb: POST
Layers       : `RoleController.create` → `RoleService.create`
Request      : body `RoleCreateRequest` — code DBF-SEC-015 (String, required, ≤ 50) · nameAr DBF-SEC-016 · nameEn DBF-SEC-017 (required, ≤ 150) · descriptionAr DBF-SEC-018 · descriptionEn DBF-SEC-019 (optional, ≤ 500); excluded: rolePk (from SEQ_SEC_ROLE), isActiveFl, audit
Response     : 201 · `ApiResponse<RoleResponse>`
Validations  : code unique (QR-SEC-034) → SEC-409-ROLE-DUP
Errors       : SEC-409-ROLE-DUP, DATA_INTEGRITY_VIOLATION, SEC-403-FORBIDDEN
Orchestration: (sets DBF-SEC-020 isActiveFl=TRUE) check uniqueness (QR-SEC-034) → persist (QR-SEC-013) → return
Repository   : QR-SEC-034, QR-SEC-013 · join NONE · READ_WRITE
Concurrency  : UQ_SEC_ROLE_CODE refuses the second of two simultaneous creates of one code (the pre-check is the friendly path)
Security     : screen SEC_ROLES · `PERM_SEC_ROLES_CREATE`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : both names required; descriptions both-or-neither
<!-- API:API-SEC-013:END -->

<!-- API:API-SEC-034:START traces=REQ-SEC-074,DBF-SEC-016,DBF-SEC-017,DBF-SEC-018,DBF-SEC-019 -->
### API-SEC-034 — update role (ADDED v2 — declared, ADR-SEC-038)
Entity       : ENT-SEC-002 · operation UPDATE (the role's names and descriptions)
Endpoint     : PUT /api/v1/sec/roles/{id}   verb: PUT
Layers       : `RoleController.update` → `RoleService.update`
Request      : path `id`; body `RoleUpdateRequest` — nameAr DBF-SEC-016 · nameEn DBF-SEC-017 (required, ≤ 150) · descriptionAr DBF-SEC-018 · descriptionEn DBF-SEC-019 (optional, ≤ 500); excluded: rolePk, code (immutable), isActiveFl, audit
Response     : 200 · `ApiResponse<RoleResponse>`
Validations  : the role exists
Errors       : SEC-404-ROLE, SEC-403-FORBIDDEN
Orchestration: load (QR-SEC-051) → update the four fields and updatedBy/updatedAt (QR-SEC-052) → return
Repository   : QR-SEC-051, QR-SEC-052 · join NONE · READ_WRITE
Concurrency  : NONE — free-text fields, last writer wins, no rule reads them
Security     : screen SEC_ROLES · `PERM_SEC_ROLES_UPDATE`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : both names required
<!-- API:API-SEC-034:END -->

<!-- API:API-SEC-014:START traces=REQ-SEC-012,DBF-SEC-061,DBF-SEC-062,DBF-SEC-063,DBF-SEC-064 -->
### API-SEC-014 — grant module to role
Entity       : ENT-SEC-007, ENT-SEC-002 · operation UPDATE (grant-tree edit)
Endpoint     : POST /api/v1/sec/roles/{id}/modules   verb: POST
Layers       : `RoleGrantController.grantModule` → `RoleGrantService.grantModule`
Request      : path `id` (→ roleId DBF-SEC-061); body `{moduleId}` (→ DBF-SEC-062)
Response     : 201 · `ApiResponse<RoleModuleGrantResponse>`
Validations  : role and module active; the grant does not exist yet
Errors       : SEC-404-ROLE, SEC-404-MODULE, SEC-409-GRANT-DUP, DATA_INTEGRITY_VIOLATION, SEC-403-FORBIDDEN
Orchestration: (sets DBF-SEC-061 roleId, DBF-SEC-063 grantedBy, DBF-SEC-064 grantedAt) load the role under a write lock (QR-SEC-051) → validate → persist (QR-SEC-014) → append MODULE_GRANTED → return
Repository   : QR-SEC-051, QR-SEC-014 · join NONE · READ_WRITE
Concurrency  : UQ_SEC_ROLE_MODULE_GRANT_ROLE_MODULE refuses a duplicate; the role row lock (QR-SEC-051) serializes this with any other grant-tree edit of the role
Security     : screen SEC_ROLES · `PERM_SEC_ROLES_UPDATE`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : n/a
<!-- API:API-SEC-014:END -->

<!-- API:API-SEC-015:START traces=REQ-SEC-015,DBF-SEC-061,DBF-SEC-062,DBF-SEC-066,DBF-SEC-071 -->
### API-SEC-015 — revoke module grant
Entity       : ENT-SEC-007, ENT-SEC-008, ENT-SEC-009 · operation UPDATE (grant-tree edit — revoke, verb DELETE)
Endpoint     : DELETE /api/v1/sec/roles/{id}/modules/{moduleId}   verb: DELETE
Layers       : `RoleGrantController.revokeModule` → `RoleGrantService.revokeModule`
Request      : path `id` (role, DBF-SEC-061), `moduleId` (DBF-SEC-062)
Response     : 200 · `ApiResponse<ModuleGrantRevokeResponse>` `{revokedScreenGrants, revokedActionGrants}`
Validations  : RULE-SEC-003 (full text under ENT-SEC-007) — the cascade is mandatory
Errors       : SEC-404-GRANT, SEC-404-MODULE, SEC-403-FORBIDDEN
Orchestration: load the role under a write lock (QR-SEC-051) → find the dependent screen and action grants of this role under this module (QR-SEC-032; DBF-SEC-066, DBF-SEC-071) → delete them, then the module grant (QR-SEC-015) → append MODULE_REVOKED, plus SCREEN_REVOKED / ACTION_REVOKED per cascaded row → return
Repository   : QR-SEC-051, QR-SEC-032, QR-SEC-015 · join NONE · READ_WRITE
Concurrency  : a revoke decides on the dependent set and then deletes it; a simultaneous screen or action grant on the same role would add a row the revoke never saw. Both take the role row PESSIMISTIC_WRITE (QR-SEC-051), so they are serialized and no orphan grant survives
Security     : screen SEC_ROLES · `PERM_SEC_ROLES_UPDATE`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : RULE-SEC-003's informational message in ar and en (under ENT-SEC-007)
<!-- API:API-SEC-015:END -->

<!-- API:API-SEC-016:START traces=REQ-SEC-013,DBF-SEC-066,DBF-SEC-067,DBF-SEC-068,DBF-SEC-069 -->
### API-SEC-016 — grant screen to role
Entity       : ENT-SEC-008, ENT-SEC-002 · operation UPDATE (grant-tree edit)
Endpoint     : POST /api/v1/sec/roles/{id}/screens   verb: POST
Layers       : `RoleGrantController.grantScreen` → `RoleGrantService.grantScreen`
Request      : path `id` (→ roleId DBF-SEC-066); body `{screenId}` (→ DBF-SEC-067)
Response     : 201 · `ApiResponse<RoleScreenGrantResponse>`
Validations  : RULE-SEC-001 — trigger: on create (screen grant). Statement: "The system shall prevent a screen grant for a role that does not hold the screen's module grant." Message: ar "لا يمكن منح شاشة دون منح الوحدة أولًا" · en "Cannot grant a screen without first granting its module" → SEC-409-NO-MODULE-GRANT (QR-SEC-028)
Errors       : SEC-409-NO-MODULE-GRANT, SEC-409-GRANT-DUP, DATA_INTEGRITY_VIOLATION, SEC-404-ROLE, SEC-404-SCREEN, SEC-403-FORBIDDEN
Orchestration: (sets DBF-SEC-066 roleId, DBF-SEC-068 grantedBy, DBF-SEC-069 grantedAt) load the role under a write lock (QR-SEC-051) → resolve the screen's module → check RULE-SEC-001 (QR-SEC-028) → persist (QR-SEC-016) → append SCREEN_GRANTED → return
Repository   : QR-SEC-051, QR-SEC-028, QR-SEC-016 · join NONE · READ_WRITE
Concurrency  : the role row lock (QR-SEC-051) serializes the RULE-SEC-001 read with a concurrent module revoke (API-SEC-015); UQ_SEC_ROLE_SCREEN_GRANT_ROLE_SCREEN refuses a duplicate
Security     : screen SEC_ROLES · `PERM_SEC_ROLES_UPDATE`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : RULE-SEC-001's message in ar and en
<!-- API:API-SEC-016:END -->

<!-- API:API-SEC-017:START traces=REQ-SEC-014,REQ-SEC-020,REQ-SEC-030,REQ-SEC-044,DBF-SEC-071,DBF-SEC-072,DBF-SEC-073,DBF-SEC-074 -->
### API-SEC-017 — grant action to role
Entity       : ENT-SEC-009, ENT-SEC-002 · operation UPDATE (grant-tree edit)
Endpoint     : POST /api/v1/sec/roles/{id}/actions   verb: POST
Layers       : `RoleGrantController.grantAction` → `RoleGrantService.grantAction`
Request      : path `id` (→ roleId DBF-SEC-071); body `{actionId}` (→ DBF-SEC-072)
Response     : 201 · `ApiResponse<RoleActionGrantResponse>`
Validations  : RULE-SEC-002 — trigger: on create (action grant). Statement: "The system shall prevent an action grant for a role that does not hold the action's screen grant." Message: ar "لا يمكن منح إجراء دون منح الشاشة أولًا" · en "Cannot grant an action without first granting its screen" → SEC-409-NO-SCREEN-GRANT (QR-SEC-029). RULE-SEC-007 — trigger: on create (action grant). Statement: "The system shall require a role to hold the VIEW action grant on a screen before any other action grant on that screen takes effect for it." Message: ar "يلزم منح إجراء العرض (VIEW) أولًا على هذه الشاشة" · en "The VIEW action must be granted on this screen first" → SEC-409-NO-VIEW-GRANT unless the action itself is VIEW (QR-SEC-030). RULE-SEC-005 (full text under ENT-SEC-009; inert) → SEC-409-SOD-CONFLICT (QR-SEC-031)
Errors       : SEC-409-NO-SCREEN-GRANT, SEC-409-NO-VIEW-GRANT, SEC-409-SOD-CONFLICT, SEC-409-GRANT-DUP, DATA_INTEGRITY_VIOLATION, SEC-404-ROLE, SEC-404-ACTION, SEC-403-FORBIDDEN
Orchestration: (sets DBF-SEC-071 roleId, DBF-SEC-073 grantedBy, DBF-SEC-074 grantedAt) load the role under a write lock (QR-SEC-051) → resolve the action's screen → RULE-SEC-002 (QR-SEC-029) → RULE-SEC-007 unless the action is VIEW (QR-SEC-030) → RULE-SEC-005 across every holder of the role (QR-SEC-031) → persist (QR-SEC-017) → append ACTION_GRANTED → return. v2: a role meant for a service account is built with the same four calls — the Oracle event consumer's role is FIN module (API-SEC-014) + journal-entry screen (API-SEC-016) + VIEW then create on that screen (this endpoint, twice), and the second call is accepted without SEC-409-NO-VIEW-GRANT because VIEW is already held (ADR-SEC-034, AC-SEC-077)
Repository   : QR-SEC-051, QR-SEC-029, QR-SEC-030, QR-SEC-031, QR-SEC-017 · join NONE · READ_WRITE
Concurrency  : the role row lock (QR-SEC-051) serializes the RULE-SEC-002 / RULE-SEC-007 reads with a concurrent module revoke; UQ_SEC_ROLE_ACTION_GRANT_ROLE_ACTION refuses a duplicate
Security     : screen SEC_ROLES · `PERM_SEC_ROLES_UPDATE`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : every rule message in ar and en
<!-- API:API-SEC-017:END -->

<!-- API:API-SEC-026:START traces=REQ-SEC-028,DBF-SEC-081,DBF-SEC-082 -->
### API-SEC-026 — terminate session
Entity       : ENT-SEC-010 · operation DELETE (terminate)
Endpoint     : DELETE /api/v1/sec/sessions/{id}   verb: DELETE
Layers       : `SessionController.terminate` → `SessionService.terminate`
Request      : path `id`
Response     : 200 · `ApiResponse<SessionTerminationResponse>` `{activeSessionPk, terminatedAt}`
Validations  : the session is open (terminatedAt IS NULL)
Errors       : SEC-404-SESSION, SEC-409-ALREADY-TERMINATED, SEC-403-FORBIDDEN
Orchestration: (writes DBF-SEC-081 terminatedAt, DBF-SEC-082 terminatedBy) terminate conditionally (QR-SEC-026); 0 rows on an existing session → SEC-409-ALREADY-TERMINATED → append SESSION_TERMINATED → the token behind it is refused from the next request (REQ-SEC-028) → return
Repository   : QR-SEC-026 · join NONE · READ_WRITE
Concurrency  : two simultaneous terminations — QR-SEC-026 updates only WHERE terminated_at IS NULL, so one succeeds and the other answers SEC-409-ALREADY-TERMINATED
Security     : screen SEC_SESSIONS · `PERM_SEC_SESSIONS_DELETE`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : n/a
<!-- API:API-SEC-026:END -->
<!-- SUB:SVC-API-CRUD:END -->

<!-- SUB:SVC-API-INT:START traces=REQ-SEC-001,REQ-SEC-002,REQ-SEC-003,REQ-SEC-006,REQ-SEC-007,REQ-SEC-008,REQ-SEC-016,REQ-SEC-017,REQ-SEC-018,REQ-SEC-019,REQ-SEC-026,REQ-SEC-029,REQ-SEC-046,REQ-SEC-047,REQ-SEC-048,REQ-SEC-049,REQ-SEC-050,REQ-SEC-055,REQ-SEC-057,REQ-SEC-059,REQ-SEC-063,REQ-SEC-064,REQ-SEC-065,REQ-SEC-066 -->
### SUB — SVC-API-INT (authentication, onboarding registration, export)

<!-- API:API-SEC-031:START traces=REQ-SEC-046,REQ-SEC-047,REQ-SEC-048,REQ-SEC-049,REQ-SEC-050,REQ-SEC-055,REQ-SEC-057,REQ-SEC-059,REQ-SEC-065,REQ-SEC-066,DBF-SEC-002,DBF-SEC-105,DBF-SEC-007,DBF-SEC-107,DBF-SEC-109,DBF-SEC-110,DBF-SEC-112 -->
### API-SEC-031 — issue access token, client credentials (ADDED v2 — MODIFIED, non-disclosure timing)
Entity       : ENT-SEC-001, ENT-SEC-014 · operation machine authentication (no screen, no page code — SRS A8, DEFAULT D3)
Endpoint     : POST /api/v1/sec/auth/token   verb: POST   (pre-authentication; the client authenticates by its credential)
Layers       : `AuthController.token` → `ServiceTokenService.issue`; the decision in `ServiceAccountCredentialDomain.assertAuthenticates`
Request      : body `ClientCredentialsTokenRequest` — grantType (String, required, must be `client_credentials`) · clientId (String, required, **max 100** — the service account's username, DBF-SEC-002, DEFAULT D2) · clientSecret (String, required, **max 200** — verified against DBF-SEC-107, never stored or logged). Both bounds are checked before any lookup: an over-long clientId would otherwise be written to SEC_AUDIT_LOG.target_ref (VARCHAR(200)) by QR-SEC-048 and fail that REQUIRES_NEW insert, answering INTERNAL_ERROR instead of SEC-401-INVALID-CLIENT and losing the SERVICE_AUTH_FAILED entry REQ-SEC-066 requires (ADR-SEC-044). The parameter meanings are RFC 6749 §4.4.2's, carried in the platform's JSON envelope like API-SEC-001 (ADR-SEC-037)
Response     : 200 · `ApiResponse<TokenResponse>` `{accessToken, tokenType: "Bearer", expiresIn: 3600}` — no refresh token (REQ-SEC-049, AC-SEC-050); header `Cache-Control: no-store`. The access token is a signed JWT whose subject is the account's userPk, with the principal-type claim SERVICE and the credential claim naming the matched credential's PK — what the CORE bearer check reads on every later request (REQ-SEC-050)
Validations  : grantType other than `client_credentials` → SEC-400-UNSUPPORTED-GRANT-TYPE (ADR-SEC-037). RULE-SEC-009 — trigger: on machine authentication request, and on every request bearing a service-account access token. Statement: "The system shall reject a machine authentication request or a service-account access token when the named user is not an ACTIVE user of principal type SERVICE, when the presented secret matches none of that user's unrevoked credentials, or when the credential that issued the token is revoked." Message: ar "بيانات اعتماد العميل غير صالحة" · en "Invalid client credentials" → SEC-401-INVALID-CLIENT, the same answer on every failure path so it never tells which condition failed (AC-SEC-047, AC-SEC-048, AC-SEC-061)
Errors       : SEC-400-UNSUPPORTED-GRANT-TYPE, SEC-401-INVALID-CLIENT
Orchestration: (writes DBF-SEC-109 lastUsedAt on the matched credential) load the user by clientId (QR-SEC-001) → hand its principal type (DBF-SEC-105) and status (DBF-SEC-007) to `assertAuthenticates`. Every path — success or any rejection — performs **exactly ten** constant-time HMAC comparisons before it answers, never fewer and never more, and never short-circuits on a match (ADR-SEC-043 §1, unchanged by ADR-SEC-045). The account's unrevoked credentials (QR-SEC-046; DBF-SEC-112, DBF-SEC-110) supply as many as it holds, at most ten by RULE-SEC-013; a fixed dummy secretHash supplies the rest. An unknown client identifier, a non-SERVICE principal and a non-ACTIVE account take ten dummy comparisons. The work is therefore independent of whether the identifier names an account, of that account's type and status, and of how many credentials it holds, so response timing discloses none of them (REQ-SEC-063, RULE-SEC-010). Every active credential is accepted until it is individually revoked (REQ-SEC-055, AC-SEC-057, AC-SEC-058) → on a match: record the time on that credential only (QR-SEC-047; REQ-SEC-057, AC-SEC-060), append SERVICE_AUTH_SUCCESS with the account as actor (REQ-SEC-065, AC-SEC-068), sign and return the token with an expiry of 3600 seconds (DEFAULT D1). No ActiveSession row is created or read and lastLoginAt is not touched (REQ-SEC-046, REQ-SEC-048 — the call succeeds whatever happened to any login session, AC-SEC-049) → on any rejection: append SERVICE_AUTH_FAILED in its own transaction (QR-SEC-048) — actor the named account when it is a SERVICE principal, otherwise empty with targetRef the submitted identifier (REQ-SEC-066, AC-SEC-069, AC-SEC-071) — and answer the error. REQ-SEC-059 is met by the token's credential claim: after a revocation the CORE bearer check refuses the token on its next use
Repository   : QR-SEC-001, QR-SEC-046, QR-SEC-047, QR-SEC-048 · join NONE · READ_WRITE (QR-SEC-048 REQUIRES_NEW)
Concurrency  : lastUsedAt is written by every successful authentication; two at once each write their own time and the later wins — no rule reads it (ADR-SEC-033). A revocation or deactivation committed between the credential read and the response cannot be undone by this call: the token it returns is refused on first use by the CORE bearer check, which reads live state. Nothing unique is allocated
Security     : public at the gate (no bearer yet) — the client authenticates by its credential; no page code, no PERM (SRS A8). **Throttled** per source and per clientId ahead of the application, by the platform's ingress, so the request never reaches this endpoint and SEC emits no code for it (ADR-SEC-044). ADR-SEC-043's ten fixed HMAC comparisons make every call cost the same; without a limit ahead of them an unauthenticated caller could spend that cost at will. ADR-SEC-043 §2's original a 429 code module code is SUPERSEDED by ADR-SEC-045 — SEC emits no rate-limit code of its own; ingress throttling is the whole answer (ADR-SEC-044). SEC cannot answer the throttle itself: `stack.backend.api.http_statuses` declares no 429, which is recorded as a platform finding, not settled here
Localization : RULE-SEC-009's message in ar and en; the unsupported-grant message in ar and en
<!-- API:API-SEC-031:END -->

<!-- API:API-SEC-001:START traces=REQ-SEC-001,REQ-SEC-002,REQ-SEC-063,DBF-SEC-002,DBF-SEC-004,DBF-SEC-007,DBF-SEC-105,DBF-SEC-008,DBF-SEC-076,DBF-SEC-077,DBF-SEC-078,DBF-SEC-079 -->
### API-SEC-001 — login (MODIFIED v2 — no interactive use of a service account; non-disclosure timing)
Entity       : ENT-SEC-001, ENT-SEC-010 · operation create (a session, via REQ-SEC-001 / REQ-SEC-002)
Endpoint     : POST /api/v1/sec/auth/login   verb: POST   (pre-authentication)
Layers       : `AuthController.login` → `AuthService.login`; v2 decision in `UserDomain.assertInteractiveAllowed`
Request      : body `LoginRequest` — username (DBF-SEC-002, **max 100**) · password (verified against DBF-SEC-004, never stored as entered, **max 200**). Both bounds are checked before any lookup, for the reason ADR-SEC-044 gives for API-SEC-031
Response     : 200 · `ApiResponse<LoginResponse>` `{accessToken, tokenType, expiresIn}`; 401 on failure, same envelope
Validations  : the credentials match an ACTIVE user (QR-SEC-001 + password verification). v2 RULE-SEC-010 — trigger: on login. Statement: "The system shall reject an interactive login, and shall issue no password-reset token, when the named user is of principal type SERVICE." Message: ar "بيانات الدخول غير صحيحة" · en "Invalid credentials" — the v1 message, so the answer does not disclose the account type
Errors       : SEC-401-INVALID-CREDENTIALS
Orchestration: (on success writes the session's DBF-SEC-076 userId, DBF-SEC-077 tokenRef, DBF-SEC-078 startedAt, DBF-SEC-079 lastActivityAt and the user's DBF-SEC-008 lastLoginAt) load the user by username (QR-SEC-001) → verify the submitted password against the stored hash first, for every principal type (a SERVICE principal's hash is the unusable value of ADR-SEC-032, so this verification always fails for it) → only then apply `assertInteractiveAllowed` (RULE-SEC-010: reject a SERVICE principal or an inactive account) → G2 fix: every failed login now costs exactly one hash verification regardless of cause — unknown username, wrong password, non-ACTIVE account, or a SERVICE principal — so response timing does not disclose which one occurred or that the account is a service account (ADR-SEC-037 §4, amended by ADR-SEC-045 to state plainly this governs only API-SEC-001's single `PasswordEncoder` verification — a separate concern from API-SEC-031's ten HMAC comparisons under ADR-SEC-043 §1) → success: create the ActiveSession (plain SAVE on ENT-SEC-010), update lastLoginAt, append LOGIN_SUCCESS, issue the token with the principal-type claim HUMAN → failure: append one LOGIN_FAILED in its own transaction (QR-SEC-048; actor empty when the username is unknown) and answer 401 (REQ-SEC-002, REQ-SEC-063)
Repository   : QR-SEC-001, QR-SEC-048 · join NONE · READ_WRITE
Concurrency  : NONE — every login creates its own session row; nothing unique is allocated
Security     : screen SEC_LOGIN · public. Throttled per source and per submitted username by the platform ingress, ahead of the application, before any hash verification; SEC emits no code for it (ADR-SEC-044; platform finding CAT-10) — revise pass G3, the same ingress guarantee API-SEC-031's Request bounds already relied on
Localization : SEC-401-INVALID-CREDENTIALS in ar and en
<!-- API:API-SEC-001:END -->

<!-- API:API-SEC-002:START traces=REQ-SEC-003,DBF-SEC-098,DBF-SEC-099,DBF-SEC-100,DBF-SEC-101,DBF-SEC-102 -->
### API-SEC-002 — submit sign-up (MODIFIED v2 — concurrency guard)
Entity       : ENT-SEC-013 · operation create
Endpoint     : POST /api/v1/sec/auth/signup   verb: POST   (pre-authentication)
Layers       : `AuthController.signup` → `SignupRequestService.submit`
Request      : body `SignupSubmitRequest` — email DBF-SEC-098 (required, ≤ 255) · fullNameAr DBF-SEC-099 · fullNameEn DBF-SEC-100 (required, ≤ 200)
Response     : 200 · `ApiResponse<SignupRequestResponse>` (statusCode PENDING)
Validations  : the email is neither a user's email nor a PENDING request's → SEC-409-SIGNUP-DUP
Errors       : SEC-409-SIGNUP-DUP
Orchestration: (sets DBF-SEC-101 submittedAt and DBF-SEC-102 statusCode=PENDING) validate → create the request (QR-SEC-002) → return; no user is created (REQ-SEC-003). v2: self sign-up never produces a SERVICE principal (REQ-SEC-038, enforced at approval)
Repository   : QR-SEC-002 · join NONE · READ_WRITE
Concurrency  : `UQ_SEC_SIGNUP_REQUEST_PENDING_EMAIL` (v2, ADR-SEC-041) is the guarantee — a partial unique index on `SEC_SIGNUP_REQUEST(email) WHERE status_code='PENDING'`. Two simultaneous submissions of one email both pass the friendly pre-check; the second insert is refused by the index, so only one PENDING row for that email ever exists, and the approval-time failure on UQ_SEC_USER_EMAIL that a second PENDING row used to cause cannot occur. The refused insert answers the shared `DATA_INTEGRITY_VIOLATION` (409), the same as API-SEC-006's Concurrency line
Security     : screen SEC_SIGNUP · public
Localization : both full names required
<!-- API:API-SEC-002:END -->

<!-- API:API-SEC-003:START traces=REQ-SEC-006,REQ-SEC-029,REQ-SEC-064,REQ-SEC-077,DBF-SEC-092,DBF-SEC-093,DBF-SEC-094,DBF-SEC-095 -->
### API-SEC-003 — request password reset (MODIFIED v2 — no token for a service account; G6 unreachable-integration record)
Entity       : ENT-SEC-012, ENT-SEC-001 · operation create (a reset token)
Endpoint     : POST /api/v1/sec/auth/password-reset/request   verb: POST   (pre-authentication)
Layers       : `AuthController.requestReset` → `PasswordResetService.request`; v2 decision in `UserDomain.assertInteractiveAllowed`
Request      : body `{email}`
Response     : 200 · the generic confirmation `{message}`, identical whether or not the email exists and, v2, whether it belongs to a service account (AC-SEC-067)
Validations  : none exposed to the caller. v2 RULE-SEC-010 (full text under ENT-SEC-001) — no reset token for a SERVICE principal; the reset request answers with the generic confirmation REQ-SEC-006 gives for any email
Errors       : none beyond the platform's own
Orchestration: (writes DBF-SEC-092 userId, DBF-SEC-093 tokenHash, DBF-SEC-094 requestedAt, DBF-SEC-095 expiresAt) look up the user by email (QR-SEC-049) → none, or v2 a SERVICE principal: do nothing further — no token, no dispatch, no audit entry, exactly as for an unregistered email (REQ-SEC-064) → a HUMAN user: create the token (QR-SEC-003, expiresAt = now + 30 minutes) → append PASSWORD_RESET_REQUESTED → where the Notifications integration is enabled (REQ-SEC-029, optional) but unreachable when dispatch is attempted, complete the request unchanged and append one WARN application-log entry naming the user id, never the email (REQ-SEC-077, G6; POL-SEC-024) — no AUDIT_EVENT_TYPE code, no CHECK re-creation — where it is enabled and reachable, dispatch through NOTIF's `NotificationDispatchApi.dispatchIndependently()` (INT-C) with the email among the variables → return the generic 200
Repository   : QR-SEC-049, QR-SEC-003 · join NONE · READ_WRITE
Concurrency  : NONE — each request creates its own token; nothing unique is allocated
Security     : screen SEC_PWD_RESET · public
Localization : the generic confirmation in ar and en
<!-- API:API-SEC-003:END -->

<!-- API:API-SEC-004:START traces=REQ-SEC-007,REQ-SEC-008,DBF-SEC-093,DBF-SEC-095,DBF-SEC-096,DBF-SEC-004 -->
### API-SEC-004 — complete password reset
Entity       : ENT-SEC-012, ENT-SEC-001 · operation update (the password)
Endpoint     : POST /api/v1/sec/auth/password-reset/complete   verb: POST   (pre-authentication)
Layers       : `AuthController.completeReset` → `PasswordResetService.complete`
Request      : body `{token, newPassword}` — the token is matched against DBF-SEC-093; newPassword is hashed into DBF-SEC-004
Response     : 200 · confirmation `{message}`
Validations  : RULE-SEC-006 — trigger: on submit (password reset completion). Statement: "The system shall reject a password-reset submission whose token is expired or already used." Message: ar "رابط إعادة التعيين غير صالح أو منتهي" · en "This reset link is invalid or has expired" → SEC-409-RESET-TOKEN-INVALID (QR-SEC-038)
Errors       : SEC-409-RESET-TOKEN-INVALID
Orchestration: (writes DBF-SEC-096 usedAt and DBF-SEC-004 passwordHash) validate the token (QR-SEC-038; DBF-SEC-095 expiresAt) → hash the new password → consume the token and update the hash (QR-SEC-004) → append PASSWORD_RESET_COMPLETED → return
Repository   : QR-SEC-038, QR-SEC-004 · join NONE · READ_WRITE
Concurrency  : two submissions of one token both pass QR-SEC-038; QR-SEC-004 consumes it only WHERE used_at IS NULL, so the second changes nothing and answers SEC-409-RESET-TOKEN-INVALID
Security     : screen SEC_PWD_RESET · public
Localization : RULE-SEC-006's message in ar and en
<!-- API:API-SEC-004:END -->

<!-- API:API-SEC-018:START traces=REQ-SEC-016,DBF-SEC-031,DBF-SEC-032,DBF-SEC-033,DBF-SEC-034 -->
### API-SEC-018 — register module
Entity       : ENT-SEC-004 · operation UPDATE (register)
Endpoint     : POST /api/v1/sec/registry/modules   verb: POST
Layers       : `RegistryController.registerModule` → `RegistryService.registerModule`
Request      : body `ModuleRegistryCreateRequest` — code DBF-SEC-031 (required, ≤ 10) · nameAr DBF-SEC-032 · nameEn DBF-SEC-033 (required, ≤ 150)
Response     : 201 · `ApiResponse<ModuleRegistryResponse>`
Validations  : code unique (QR-SEC-035) → SEC-409-MODULE-DUP
Errors       : SEC-409-MODULE-DUP, DATA_INTEGRITY_VIOLATION, SEC-403-FORBIDDEN
Orchestration: (sets DBF-SEC-034 isActiveFl=TRUE) check uniqueness (QR-SEC-035) → persist (QR-SEC-018) → return
Repository   : QR-SEC-035, QR-SEC-018 · join NONE · READ_WRITE
Concurrency  : UQ_SEC_MODULE_REG_CODE refuses the second of two simultaneous registrations of one code
Security     : screen SEC_MODULE_REGISTRY · `PERM_SEC_MODULE_REGISTRY_UPDATE`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token) — called by a registering module's onboarding through an administrator-held credential
Localization : both names required
<!-- API:API-SEC-018:END -->

<!-- API:API-SEC-019:START traces=REQ-SEC-017,REQ-SEC-018,DBF-SEC-040,DBF-SEC-041,DBF-SEC-042,DBF-SEC-043,DBF-SEC-044 -->
### API-SEC-019 — register screen
Entity       : ENT-SEC-005 · operation UPDATE (register)
Endpoint     : POST /api/v1/sec/registry/screens   verb: POST
Layers       : `RegistryController.registerScreen` → `RegistryService.registerScreen`
Request      : body `ScreenRegistryCreateRequest` — moduleCode (resolved to DBF-SEC-041 moduleId) · pageCode DBF-SEC-040 (required, ≤ 50) · nameAr DBF-SEC-042 · nameEn DBF-SEC-043 (required, ≤ 150)
Response     : 201 · `ApiResponse<ScreenRegistryResponse>`
Validations  : RULE-SEC-004 — trigger: on create (screen registration). Statement: "The system shall reject a screen registration whose module code has no ModuleRegistry row." Message: ar "الوحدة غير مسجّلة" · en "Module is not registered" → SEC-409-MODULE-NOT-REGISTERED (QR-SEC-036); pageCode unique (QR-SEC-036) → SEC-409-SCREEN-DUP
Errors       : SEC-409-MODULE-NOT-REGISTERED, SEC-409-SCREEN-DUP, DATA_INTEGRITY_VIOLATION, SEC-404-SCREEN, SEC-403-FORBIDDEN
Orchestration: (sets DBF-SEC-041 moduleId and DBF-SEC-044 isActiveFl=TRUE) check RULE-SEC-004 and uniqueness (QR-SEC-036) → persist (QR-SEC-019) → return
Repository   : QR-SEC-036, QR-SEC-019 · join NONE · READ_WRITE
Concurrency  : UQ_SEC_SCREEN_REG_PAGE refuses a duplicate page code; FK_SCREEN_REG_MODULE is the guarantee behind RULE-SEC-004
Security     : screen SEC_MODULE_REGISTRY · `PERM_SEC_MODULE_REGISTRY_UPDATE`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : both names required; RULE-SEC-004's message in ar and en
<!-- API:API-SEC-019:END -->

<!-- API:API-SEC-020:START traces=REQ-SEC-019,DBF-SEC-050,DBF-SEC-051,DBF-SEC-052,DBF-SEC-053,DBF-SEC-054,DBF-SEC-055 -->
### API-SEC-020 — register action
Entity       : ENT-SEC-006 · operation UPDATE (register)
Endpoint     : POST /api/v1/sec/registry/actions   verb: POST
Layers       : `RegistryController.registerAction` → `RegistryService.registerAction`
Request      : body `ActionRegistryCreateRequest` — pageCode (resolved to DBF-SEC-051 screenId) · actionCode DBF-SEC-052 (required, ≤ 40) · nameAr DBF-SEC-053 · nameEn DBF-SEC-054 (required, ≤ 150)
Response     : 201 · `ApiResponse<ActionRegistryResponse>` (with the built permissionCode)
Validations  : the screen is registered → SEC-409-SCREEN-NOT-REGISTERED; the built permission code is unique (QR-SEC-037) → SEC-409-ACTION-DUP
Errors       : SEC-409-SCREEN-NOT-REGISTERED, SEC-409-ACTION-DUP, DATA_INTEGRITY_VIOLATION, SEC-403-FORBIDDEN
Orchestration: (sets DBF-SEC-051 screenId, DBF-SEC-050 permissionCode = `PERM_` + pageCode + `_` + actionCode, DBF-SEC-055 isActiveFl=TRUE) resolve the screen by pageCode → build the permission code → check uniqueness (QR-SEC-037) → persist (QR-SEC-020) → return
Repository   : QR-SEC-037, QR-SEC-020 · join NONE · READ_WRITE
Concurrency  : UQ_SEC_ACTION_REG_PERM refuses a duplicate permission code
Security     : screen SEC_MODULE_REGISTRY · `PERM_SEC_MODULE_REGISTRY_UPDATE`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : both names required
<!-- API:API-SEC-020:END -->

<!-- API:API-SEC-024:START traces=REQ-SEC-026,DBF-SEC-084,DBF-SEC-085,DBF-SEC-086,DBF-SEC-087,DBF-SEC-088,DBF-SEC-089 -->
### API-SEC-024 — export audit log
Entity       : ENT-SEC-011 · operation VIEW (export — shares the VIEW permission)
Endpoint     : GET /api/v1/sec/audit-log/export   verb: GET
Layers       : `AuditLogController.export` → `AuditLogService.export`
Request      : query params — the filter set of API-SEC-023 (eventTypeCode DBF-SEC-084, actorUserId DBF-SEC-085, occurredFrom / occurredTo over DBF-SEC-086), unpaged
Response     : 200 · `Content-Type: text/csv`, one row per matching entry, every field (detailsAr and detailsEn as separate columns)
Validations  : none
Errors       : SEC-403-FORBIDDEN
Orchestration: load the full filtered set (QR-SEC-024) → serialize to CSV → return (REQ-SEC-026)
Repository   : QR-SEC-024 · join NONE · READ_ONLY
Concurrency  : NONE — read-only
Security     : screen SEC_AUDIT_LOG · `PERM_SEC_AUDIT_LOG_VIEW`, CORE gate (QR-SEC-040, QR-SEC-044 for a SERVICE token)
Localization : detailsAr and detailsEn as two columns
<!-- API:API-SEC-024:END -->
<!-- SUB:SVC-API-INT:END -->
<!-- PHASE:SVC-API:END -->

<!-- PHASE:DOC:START traces=REQ-SEC-016,REQ-SEC-046,REQ-SEC-051 -->
## PHASE 4 — DOC

**API contract summary** — backend self-check only. The frontend binds to the api-docs the implementation
publishes under `backend/modules/SEC/api-docs/` (`index.md` plus `endpoints/<group-slug>.md`), never to this table.
Paths are relative to `/api/v1/sec`.

| API | Path | Verb | Request DTO | Response DTO | Stability |
|---|---|---|---|---|---|
| API-SEC-001 | /auth/login | POST | LoginRequest | LoginResponse | v1 (v2 behaviour) |
| API-SEC-002 | /auth/signup | POST | SignupSubmitRequest | SignupRequestResponse | v1 |
| API-SEC-003 | /auth/password-reset/request | POST | PasswordResetRequest | ConfirmationResponse | v1 (v2 behaviour) |
| API-SEC-004 | /auth/password-reset/complete | POST | PasswordResetCompleteRequest | ConfirmationResponse | v1 |
| API-SEC-005 | /users/search | POST | UserSearchRequest | Page\<UserResponse\> | v1 (v2 field) |
| API-SEC-006 | /users | POST | UserCreateRequest | UserResponse | v1 (v2 field) |
| API-SEC-007 | /users/{id} | PUT | UserUpdateRequest | UserResponse | v1 (v2 field) |
| API-SEC-008 | /users/{id}/roles | PUT | UserRoleAssignmentRequest | UserResponse | v1 |
| API-SEC-009 | /users/{id} | DELETE | — | UserStatusResponse | v1 |
| API-SEC-010 | /users/{id} | PATCH | — | UserStatusResponse | v1 |
| API-SEC-011 | /signup-requests/{id} | PATCH | SignupDecisionRequest | UserResponse \| SignupRequestResponse | v1 |
| API-SEC-012 | /roles/search | POST | RoleSearchRequest | Page\<RoleResponse\> | v1 |
| API-SEC-013 | /roles | POST | RoleCreateRequest | RoleResponse | v1 |
| API-SEC-014 | /roles/{id}/modules | POST | RoleModuleGrantRequest | RoleModuleGrantResponse | v1 |
| API-SEC-015 | /roles/{id}/modules/{moduleId} | DELETE | — | ModuleGrantRevokeResponse | v1 |
| API-SEC-016 | /roles/{id}/screens | POST | RoleScreenGrantRequest | RoleScreenGrantResponse | v1 |
| API-SEC-017 | /roles/{id}/actions | POST | RoleActionGrantRequest | RoleActionGrantResponse | v1 |
| API-SEC-018 | /registry/modules | POST | ModuleRegistryCreateRequest | ModuleRegistryResponse | v1 |
| API-SEC-019 | /registry/screens | POST | ScreenRegistryCreateRequest | ScreenRegistryResponse | v1 |
| API-SEC-020 | /registry/actions | POST | ActionRegistryCreateRequest | ActionRegistryResponse | v1 |
| API-SEC-021 | /registry/search | POST | RegistrySearchRequest | Page\<RegistryRowResponse\> | v1 |
| API-SEC-022 | /dashboard | GET | — | DashboardResponse | v1 (v2 field) |
| API-SEC-023 | /audit-log/search | POST | AuditLogEntrySearchRequest | Page\<AuditLogEntryResponse\> | v1 |
| API-SEC-024 | /audit-log/export | GET | (query params) | text/csv | v1 |
| API-SEC-025 | /sessions/search | POST | ActiveSessionSearchRequest | Page\<ActiveSessionResponse\> | v1 |
| API-SEC-026 | /sessions/{id} | DELETE | — | SessionTerminationResponse | v1 |
| API-SEC-027 | /menu | GET | — | List\<ModuleMenuResponse\> | v1 |
| API-SEC-028 | /users/{id}/credentials | GET | — | List\<ServiceAccountCredentialResponse\> | v2 |
| API-SEC-029 | /users/{id}/credentials | POST | CredentialIssueRequest | CredentialIssueResponse | v2 |
| API-SEC-030 | /users/{id}/credentials/{credentialId} | DELETE | — | CredentialRevokeResponse | v2 |
| API-SEC-031 | /auth/token | POST | ClientCredentialsTokenRequest | TokenResponse | v2 |
| API-SEC-032 | /users/{id} | GET | — | UserResponse | v2 (declared) |
| API-SEC-033 | /roles/{id} | GET | — | RoleResponse | v2 (declared) |
| API-SEC-034 | /roles/{id} | PUT | RoleUpdateRequest | RoleResponse | v2 (declared) |
| API-SEC-035 | /roles/{id}/grants | GET | — | RoleGrantTreeResponse | v2 (declared) |
| API-SEC-036 | /signup-requests/search | POST | SignupRequestSearchRequest | Page\<SignupRequestResponse\> | v2 (declared) |
| API-SEC-037 | in-process `SecUserDirectoryApi.findContact` | — | userId | UserContact | v2 (ADR-SEC-040) |
| API-SEC-038 | in-process `SecUserDirectoryApi.findUserIdsHoldingPermission` | — | permission code | user ids | v2 (ADR-SEC-040) |
| API-SEC-039 | in-process `SecModuleRegistryApi` | — | module code | boolean | v2 (ADR-SEC-040) |

**DTO typing constraints**: statusCode, principalTypeCode, eventTypeCode and actionCode are `String` codes, never a
Java enum. No PK appears in a create body. No response DTO has a field for passwordHash, tokenHash or secretHash;
`secret` exists on `CredentialIssueResponse` only.

**Pagination and filters**: `{page, size, sortField, sortDirection, filters}` — the shared
`BaseSearchContractRequest`; an unknown sortField → `SEC-400-INVALID-SORT`; an empty result is `200` with empty
content, never `404`.

**For the machine caller** (published with the api-docs of API-SEC-031): request a token with `grantType`
`client_credentials`, the account's username and the secret; send it as `Authorization: Bearer`; request a new one
when `expiresIn` has elapsed or on any 401 — there is no refresh token. Every 401 `SEC-401-INVALID-CLIENT` on the
token call means the credential or the account is not usable and needs an administrator; retrying with the same
secret will not succeed.
<!-- PHASE:DOC:END -->

<!-- PHASE:INT-C:START traces=REQ-SEC-029 -->
## PHASE 5 — INT-C (cross-module consume)

No `XM-*` row exists for SEC (db-script §2: "None — SEC is ROOT"). SEC consumes no other module's entity, table or
column, and v2 adds none: `SEC_SVC_ACCOUNT_CRED.user_id` is an intra-module key. No SUB is opened (XM count 0 < 5).

One in-process call crosses the boundary, unchanged from v1 and sanctioned by SRS §A8 (`SOFT / optional`):
`PasswordResetService.dispatchResetNotification` (API-SEC-003) injects NOTIF's
`com.erp.notif.crossmodule.NotificationDispatchApi` and calls `dispatchIndependently()` — its own transaction
(`REQUIRES_NEW` on NOTIF's side), so a dispatch failure never rolls back SEC's token and audit rows, and the generic
200 answers whether or not Notifications is available (REQ-SEC-006, REQ-SEC-029). The recipient's email travels among
the dispatch variables. Access mechanism: the injected in-process interface
(`profile.conventions.module_interface: in_process`) — no HTTP client, no base path, no network error path, so no
Error Catalog row describes one. It registers no entity, table or column, so it is not an `XM-*` row. v2: a service
account never reaches this call (REQ-SEC-064).

The external Oracle event consumer is a *caller* of SEC, not a dependency: it authenticates through API-SEC-031 and
then calls FIN's event-entry endpoint with the token. It is not a platform module and owns no table, so it has no XM
row (SRS §A8, db-script §2).
<!-- PHASE:INT-C:END -->

<!-- PHASE:INT-R:START traces=REQ-SEC-016,REQ-SEC-034,REQ-SEC-035 -->
## PHASE 6 — INT-R (cross-module resolve)

No `XM-*` row to resolve — same basis as INT-C; no SUB (0 < 5). This phase carries SEC's EXPOSED in-process surfaces
in `com.erp.sec.crossmodule`. They register no entity, table or column, so they add no row to db-script §2, and the
`XM-*` id of each dependency belongs to the consuming module's own P2.

| Surface | Operation | Reads | Query | Consumer | v2 |
|---|---|---|---|---|---|
| `SecUserDirectoryApi` | `findContact(Long)` → `UserContact` (email, both display names, active) — REQ-SEC-034 | ENT-SEC-001 | a user by PK (QR-SEC-040) | NOTIF (its recipient contact read) | a service account's contact email is returned like any user's (ADR-SEC-032) |
| `SecUserDirectoryApi` | `findUserIdsHoldingPermission(String)` → user ids — REQ-SEC-035 | ENT-SEC-001, ENT-SEC-003, ENT-SEC-006, ENT-SEC-009 | QR-SEC-039 | none since FIN retired its separation-of-duties reader on 2026-09-12 | a service account holding the code through its roles is included |
| `SecModuleRegistryApi` | is this module code registered? | ENT-SEC-004 | the module-code existence read (QR-SEC-035) | MDL — XM-MDL-001 | unchanged; written down here for the first time (ADR-SEC-040) |

Each surface returns read-models only, injects no repository into its caller, and has no network failure path.
G4 fix: REQ-SEC-034 and REQ-SEC-035 are served by API-SEC-037/038 (ADR-SEC-040), not by "no endpoint" — the plan's
own EXECUTION PLAN INDEX (P4 pattern above) lists them as API-SEC-037 … 039. ADR-SEC-039's three findings (REQ-SEC-034,
REQ-SEC-035, QR-SEC-039 uncovered) are SUPERSEDED and closed by ADR-SEC-040, which minted these three in-process
surfaces their own API ids rather than leaving them uncited.

Inbound dependency stub, not `TODO`: `XM-INBOUND-STUB-1` — any future module registers itself through API-SEC-018 …
API-SEC-020 and consumes identity and authorization through API-SEC-001, API-SEC-027 and the CORE gate; v2 adds
API-SEC-031 for a machine caller that module deploys. Formal `XM-*` ids for that direction are the consumer's.

Status per surface: READY — same deployable, nothing DEFERRED, MOCKED, SIMULATED, BLOCKED or in EXTERNAL_WAIT.
<!-- PHASE:INT-R:END -->

<!-- PHASE:SEC-BE:START traces=REQ-SEC-012,REQ-SEC-013,REQ-SEC-014,REQ-SEC-023,REQ-SEC-030,REQ-SEC-033,REQ-SEC-044,REQ-SEC-045,REQ-SEC-050 -->
## PHASE 7 — SEC-BE (security, backend half)

Permission matrix — one row per composite screen; a `✓` cell names the endpoints it serves and the permission for
that action. No new page and no new action in v2: the credential endpoints sit under SEC_USERS (SCR-REQ-SEC-004 B4).

| Screen (page code) | ENT | API serving it | VIEW | CREATE | UPDATE | DELETE |
|---|---|---|---|---|---|---|
| SEC_LOGIN | ENT-SEC-001, ENT-SEC-010 | API-SEC-001 | public | — | — | — |
| SEC_SIGNUP | ENT-SEC-013 | API-SEC-002 | public | — | — | — |
| SEC_PWD_RESET | ENT-SEC-012 | API-SEC-003, API-SEC-004 | public | — | — | — |
| SEC_USERS | ENT-SEC-001, ENT-SEC-003, ENT-SEC-013, ENT-SEC-014 | API-SEC-005 … API-SEC-011, API-SEC-028 … API-SEC-030, API-SEC-032, API-SEC-036 | ✓ `PERM_SEC_USERS_VIEW` (API-SEC-005, API-SEC-032, API-SEC-036; v2 API-SEC-028) | ✓ `PERM_SEC_USERS_CREATE` (API-SEC-006, incl. v2 service accounts) | ✓ `PERM_SEC_USERS_UPDATE` (API-SEC-007 … API-SEC-011; v2 API-SEC-029 issue, API-SEC-030 revoke) | — |
| SEC_ROLES | ENT-SEC-002, ENT-SEC-007, ENT-SEC-008, ENT-SEC-009 | API-SEC-012 … API-SEC-017, API-SEC-033 … API-SEC-035 | ✓ `PERM_SEC_ROLES_VIEW` (API-SEC-012, API-SEC-033, API-SEC-035) | ✓ `PERM_SEC_ROLES_CREATE` (API-SEC-013) | ✓ `PERM_SEC_ROLES_UPDATE` (API-SEC-034; grant-tree edits API-SEC-014 … API-SEC-017) | registered, no endpoint — deactivating a role is DEFERRED (ADR-SEC-038) |
| SEC_MODULE_REGISTRY | ENT-SEC-004, ENT-SEC-005, ENT-SEC-006 | API-SEC-018 … API-SEC-021 | ✓ `PERM_SEC_MODULE_REGISTRY_VIEW` (API-SEC-021) | — | ✓ `PERM_SEC_MODULE_REGISTRY_UPDATE` (API-SEC-018, API-SEC-019, API-SEC-020 register) | — |
| SEC_DASHBOARD | ENT-SEC-001, ENT-SEC-002, ENT-SEC-010, ENT-SEC-011 | API-SEC-022 | ✓ `PERM_SEC_DASHBOARD_VIEW` (API-SEC-022; each widget also its source-screen VIEW) | — | — | — |
| SEC_AUDIT_LOG | ENT-SEC-011 | API-SEC-023, API-SEC-024 | ✓ `PERM_SEC_AUDIT_LOG_VIEW` (API-SEC-023, API-SEC-024 export) | — | — | — |
| SEC_SESSIONS | ENT-SEC-010 | API-SEC-025, API-SEC-026 | ✓ `PERM_SEC_SESSIONS_VIEW` (API-SEC-025) | — | — | ✓ `PERM_SEC_SESSIONS_DELETE` (API-SEC-026 terminate) |
| (menu) | ENT-SEC-004, ENT-SEC-005, ENT-SEC-007, ENT-SEC-008 | API-SEC-027 | authenticated, no page code | — | — | — |
| (machine token, v2) | ENT-SEC-001, ENT-SEC-014 | API-SEC-031 | client-authenticated (RULE-SEC-009), no page code | — | — | — |

**Every API verifies its permission before processing.** Each secured service method carries `@PreAuthorize` naming
its cell's permission from `PermissionConstants`; no service holds a hard-coded role check. The DELETE verb of
API-SEC-009 and API-SEC-030 is a soft deactivation or revocation under the screen's UPDATE permission, exactly as
SCR-REQ-SEC-004 B4 states; SEC_USERS has no DELETE action. The SEC_MODULE_REGISTRY "deactivate a row" operation has no
endpoint in this version either — DEFERRED with deactivating a role (ADR-SEC-038).

**v2 — a service account's authorization.** A service account is gated by the same CORE gate as any user
(REQ-SEC-044, REQ-SEC-045): module grant → screen grant → VIEW gateway → action grant. It is created with no role
(REQ-SEC-037) and receives permissions only through a role an administrator assigns (API-SEC-008). The Oracle event
consumer's role holds FIN's module grant, the journal-entry screen grant, that screen's VIEW and its create action,
and nothing else (ADR-SEC-034, AC-SEC-077): it can create and also read FIN journal entries, and reaches no other FIN
screen and no other module. A request outside its grants answers `SEC-403-FORBIDDEN` (or the owning module's own
forbidden code) with the message a human receives. Its token is re-validated against live state on every request
(CORE, RULE-SEC-009).

**Gateway**: every non-VIEW permission requires VIEW on the same screen (RULE-SEC-007) — enforced by the CORE gate on
every request and at grant time by API-SEC-017 (QR-SEC-030).

**Forbidden responses**: a denial by the CORE gate maps to `LocalizedException{code: "SEC-403-FORBIDDEN", messageAr:
"غير مصرح بهذا الإجراء", messageEn: "You are not authorized to perform this action"}`; a rejected service-account
token maps to `SEC-401-INVALID-CLIENT` (Error Catalog).

### BOOTSTRAP DATA — SEC v2
Every row a fresh deployment needs before a SEC endpoint can succeed, each with who produces it. SEC's lookup keys
have no table: their value sets are CHECK constraints the db-script creates (ADR-SEC-001), so no row is seeded, and
their labels come from SRS A6 through the frontend's single resolver (ADR-SEC-006). Registering a permission is not a
grant: every permission names the role it is granted to (`profile.conventions.security_model.grant_target` = role).
v2 adds no page and no permission, so no new security seed row.

```
LOOKUP KEY  USER_STATUS        │ seed source: none as rows — CHECK constraint of SEC_USER.status_code, created by the db-script (ADR-SEC-001); labels from SRS A6 (ADR-SEC-006)
LOOKUP KEY  SIGNUP_STATUS      │ seed source: none as rows — CHECK constraint of SEC_SIGNUP_REQUEST.status_code (ADR-SEC-001); labels from SRS A6
LOOKUP KEY  AUDIT_EVENT_TYPE   │ seed source: none as rows — CHK_SEC_AUDIT_LOG_EVENT_TYPE, re-created with 21 codes by the v2 db-script (revise pass G6: +SERVICE_ACCOUNT_REACTIVATED); labels from SRS A6
PLATFORM SECRET  SEC service-credential pepper │ seed source: platform secret store, injected at runtime, never in the database or repository (ADR-SEC-043 §1, revise pass G5)
LOOKUP KEY  PRINCIPAL_TYPE     │ seed source: none as rows — CHK_SEC_USER_PRINCIPAL_TYPE, added by the v2 db-script; the column default backfills HUMAN on every v1 row; labels from SRS A6
PERMISSION  PERM_SEC_USERS_VIEW              │ grant target: role SYS_ADMIN — page and action registered with the SEC security seed (V17__sec_security_seed.sql, as built v1)
PERMISSION  PERM_SEC_USERS_CREATE            │ grant target: role SYS_ADMIN — SEC security seed (V17, as built)
PERMISSION  PERM_SEC_USERS_UPDATE            │ grant target: role SYS_ADMIN — SEC security seed (V17, as built); v2's credential issue and revoke need nothing more
PERMISSION  PERM_SEC_ROLES_VIEW              │ grant target: role SYS_ADMIN — SEC security seed (V17, as built)
PERMISSION  PERM_SEC_ROLES_CREATE            │ grant target: role SYS_ADMIN — SEC security seed (V17, as built)
PERMISSION  PERM_SEC_ROLES_UPDATE            │ grant target: role SYS_ADMIN — SEC security seed (V17, as built)
PERMISSION  PERM_SEC_ROLES_DELETE            │ grant target: role SYS_ADMIN — SEC security seed (V17, as built); no endpoint uses it yet (ADR-SEC-038)
PERMISSION  PERM_SEC_MODULE_REGISTRY_VIEW    │ grant target: role SYS_ADMIN — SEC security seed (V17, as built)
PERMISSION  PERM_SEC_MODULE_REGISTRY_UPDATE  │ grant target: role SYS_ADMIN — SEC security seed (V17, as built)
PERMISSION  PERM_SEC_DASHBOARD_VIEW          │ grant target: role SYS_ADMIN — SEC security seed (V17, as built)
PERMISSION  PERM_SEC_AUDIT_LOG_VIEW          │ grant target: role SYS_ADMIN — SEC security seed (V17, as built)
PERMISSION  PERM_SEC_SESSIONS_VIEW           │ grant target: role SYS_ADMIN — SEC security seed (V17, as built)
PERMISSION  PERM_SEC_SESSIONS_DELETE         │ grant target: role SYS_ADMIN — SEC security seed (V17, as built)
PERMISSION  PERM_FIN_JOURNAL_ENTRIES_VIEW    │ grant target: role — the Oracle event consumer's role, created by an administrator (below); registered by FIN, not by SEC
PERMISSION  PERM_FIN_JOURNAL_ENTRIES_CREATE  │ grant target: role — the Oracle event consumer's role, created by an administrator (below); registered by FIN, not by SEC
SERVICE ACCOUNT  the Oracle event consumer   │ seed source: an administrator at deployment — API-SEC-006 with principalTypeCode SERVICE, then API-SEC-029 for its credential, whose secret is handed to the daemon's configuration. Never seeded by a migration: a seeded secret would live in the repository
ROLE             the consumer's role          │ seed source: an administrator — API-SEC-013 (the role), API-SEC-014 (FIN module), API-SEC-016 (journal-entry screen), API-SEC-017 (VIEW, then create), API-SEC-008 (assigned to the service account); AC-SEC-077. Until it exists every FIN call of the consumer is forbidden
```
<!-- PHASE:SEC-BE:END -->

<!-- PHASE:ALIGN-BE:START traces=REQ-SEC-033,REQ-SEC-050 -->
## PHASE 8 — ALIGN-BE

The self-check of this plan. Each row names the `gov.py analyze` check that backs it, and its mark is that check's
result in the stage report (`_state/analyze-stage-P3.1.md`), copied rather than judged. A clause that examined nothing
is written as such, never ✓. The RESULT line is written by the orchestrator.

ADR-SEC-039 recorded three findings here — REQ-SEC-034, REQ-SEC-035 and QR-SEC-039 uncovered, because SEC's
in-process `SecUserDirectoryApi` was not an `API-*`. ADR-SEC-040 closed them at the human stop: an `API` is the
module's published contract surface, not an HTTP route, so the in-process surfaces are API-SEC-037, API-SEC-038 and
API-SEC-039. No row below is ✗.

```
ALIGN — SEC v2
row               backing check        assertion                                                                  mark
TRACEABILITY      traces               every PHASE/SUB/atom block carries traces=, and every API traces to its     ✓ (C7.2: 53 blocks · C7.3: 39 API)
                                       REQ and its DBF
COVERED           orphans              every REQ is covered by ≥1 API or DBF                                      ✓ (C7.6: 79 — REQ-SEC-034 by API-SEC-037, REQ-SEC-035 by API-SEC-038, REQ-SEC-078 by API-SEC-029, REQ-SEC-079 by API-SEC-010 [revise pass G6])
BINDING (§2A)     value-agreement      every DBF names the same physical column here as the db-script declares   ✓ (C7.10)
MANIFEST (§4)     count-agrees         every total this plan states equals the rows it heads                      ✓ (C7.18: 4 totals)
WRITERS           required-writer      every required column is written by an endpoint, or the row states why    ✓ (C7.19: 60 columns)
QRC (§5)          orphans              every catalogued query is reached by ≥1 API                                ✓ (C7.22: 54 — QR-SEC-039 by API-SEC-038)
API (R3)          code-format          every catalog code is an instance of the declared format and carries a    ✓ (C7.11)
                                       status the platform can emit
RULE INPUTS       data-source          every RULE enforced at runtime names where the data it READS comes from,  ✓ (C6.9, stage P2 report)
                                       or is deferred
CROSS-MODULE      registry-agree       every registered XM is placed here, and every XM minted here is           — examined nothing (C7.5, C7.5b: SEC is ROOT, 0 XM)
                                       back-registered
FOREIGN IDS       xref-resolve         every id of another module cited here is defined in that module's own     ✓ (C7.12)
                                       registry
BOOTSTRAP DATA    bootstrap-complete   every lookup key and every permission has a row naming who produces it    ✓ (C7.21: 20 names — +1 PLATFORM SECRET row, revise pass G5)
SECURITY (R7)     operation-resolves   every declared entity operation resolves to an API, and every marked      ✓ (C7.20: 42)
                                       matrix cell names its API and its permission
DEMAND (SRS)      operation-resolves   every operation an SRS screen names is built by an API, or the plan       ✓ (C7.23: 28)
                                       states why it is not
DECISIONS         refs-exist           every ADR this plan cites exists on disk in analysis/decisions/SEC/        ✓ (C7.13)
PATHS             paths-resolve        every path the generated manifest and execution state emit resolves to    ✓ (C7.14)
                                       something that exists
COVERAGE          (the report)         clauses the report lists as having examined nothing: C7.5, C7.5b (SEC is ROOT — 0 XM by nature)
RESULT            PASSED ✓ — 0 findings
```

**Coverage — ENT/DBF → phases → QR → XM.** Every ENT-SEC-001 … ENT-SEC-014 has exactly one DATA-DOM block and at least
one SVC-API endpoint; all 116 DBF ids have one manifest row; SEC has no XM.
| ENT | DBF | Endpoints | Queries | XM |
|---|---|---|---|---|
| ENT-SEC-001 | DBF-SEC-001..013, DBF-SEC-105 | API-SEC-001, 003, 005 … 011, 031, 032 | QR-SEC-001, 005, 006, 007, 009, 010, 033, 040, 041, 049 | — |
| ENT-SEC-002 | DBF-SEC-014..024 | API-SEC-012, 013, 033, 034 | QR-SEC-012, 013, 034, 051, 052 | — |
| ENT-SEC-003 | DBF-SEC-025..029 | API-SEC-008, 032 | QR-SEC-008, 031, 050 | — |
| ENT-SEC-004 | DBF-SEC-030..038 | API-SEC-018, 021, 027 | QR-SEC-018, 021, 035 | — |
| ENT-SEC-005 | DBF-SEC-039..048 | API-SEC-019, 021, 027 | QR-SEC-019, 036 | — |
| ENT-SEC-006 | DBF-SEC-049..059 | API-SEC-020, 021 | QR-SEC-020, 037 | — |
| ENT-SEC-007 | DBF-SEC-060..064 | API-SEC-014, 015, 035 | QR-SEC-014, 015, 028, 032, 053 | — |
| ENT-SEC-008 | DBF-SEC-065..069 | API-SEC-016, 015, 035 | QR-SEC-016, 028, 032, 053 | — |
| ENT-SEC-009 | DBF-SEC-070..074 | API-SEC-017, 015, 035 | QR-SEC-017, 029, 030, 031, 053 | — |
| ENT-SEC-010 | DBF-SEC-075..082 | API-SEC-001, 009, 025, 026 | QR-SEC-025, 026 | — |
| ENT-SEC-011 | DBF-SEC-083..090 | API-SEC-022, 023, 024 (appended by most mutations) | QR-SEC-023, 024, 048 | — |
| ENT-SEC-012 | DBF-SEC-091..096 | API-SEC-003, 004 | QR-SEC-003, 004, 038 | — |
| ENT-SEC-013 | DBF-SEC-097..104 | API-SEC-002, 011, 036 | QR-SEC-002, 011, 054 | — |
| ENT-SEC-014 | DBF-SEC-106..116 | API-SEC-028, 029, 030, 031 | QR-SEC-042 … 047 | — |

**Coverage — RULE → API → catalog code**: the Rule Registry of the Plan Index carries it, one row per RULE-SEC-001 …
RULE-SEC-013; every rule that raises a user-facing rejection has exactly one Error Catalog row per code.

**Coverage — XM → status → blocks → workaround**: not applicable (0 XM).
<!-- PHASE:ALIGN-BE:END -->

## Error Catalog — SEC v2

Envelope: `LocalizedException → {code, messageAr, messageEn}`. Runtime code format: `SEC-{http}[-{SLUG}]` (CORE). The
one location of every code; downstream stages cite the code and never restate the text. v2 adds eight rows at the
end (six from the v2 delta plus DATA_INTEGRITY_VIOLATION, restated as a shared row, and SEC-409-CREDENTIAL-LIMIT,
RULE-SEC-013), adds RULE-SEC-010 to the login row, and adds the shared `VALIDATION_ERROR` row the v1 catalog omitted.

| code | RULE / PLATFORM-STD | API | HTTP | trigger | message-AR | message-EN |
|---|---|---|---|---|---|---|
| SEC-401-INVALID-CREDENTIALS | PLATFORM-STD (ADR-SEC-002); v2 also RULE-SEC-010 | API-SEC-001 | 401 | wrong or unknown credentials; v2 also any interactive login naming a service account | بيانات الدخول غير صحيحة | Invalid credentials |
| SEC-409-USER-DUP | PLATFORM-STD (uniqueness) | API-SEC-006, 007, 011 (revise pass G7) | 409 | duplicate username or email | اسم المستخدم أو البريد الإلكتروني مستخدم بالفعل | Username or email already in use |
| DATA_INTEGRITY_VIOLATION | PLATFORM-STD (shared handler) | API-SEC-002, API-SEC-006, API-SEC-007, API-SEC-011, API-SEC-013, API-SEC-014, API-SEC-016, API-SEC-017, API-SEC-018, API-SEC-019, API-SEC-020 (revise pass G7 — every endpoint whose Concurrency line names a UQ constraint as its guarantee) | 409 | a uniqueness constraint refused an insert that passed its friendly pre-check; the shared `GlobalExceptionHandler` answers, SEC neither declares nor throws it | نصّ المعالِج المشترك | the shared handler's own text |
| SEC-404-USER | PLATFORM-STD (not found) | API-SEC-007, 008, 009, 010, 028, 029, 032 | 404 | unknown user id | المستخدم غير موجود | User not found |
| SEC-409-SOD-CONFLICT | RULE-SEC-005 | API-SEC-008, 017 | 409 | conflicting action pair on one user (inert) | هذا المستخدم يملك إجراءً متعارضًا بالفعل | This user already holds a conflicting action |
| SEC-404-ROLE | PLATFORM-STD (not found) | API-SEC-008, 014, 016, 017, 033, 034, 035 | 404 | unknown role id | الدور غير موجود | Role not found |
| SEC-409-INVALID-TRANSITION | PLATFORM-STD (lifecycle) | API-SEC-010, 011 | 409 | transition not allowed from the current status | لا يمكن تنفيذ هذا الانتقال من الحالة الحالية | This transition is not allowed from the current status |
| SEC-404-SIGNUP | PLATFORM-STD (not found) | API-SEC-011 | 404 | unknown sign-up request id | طلب التسجيل غير موجود | Signup request not found |
| SEC-409-ROLE-DUP | PLATFORM-STD (uniqueness) | API-SEC-013 | 409 | duplicate role code | رمز الدور مستخدم بالفعل | Role code already in use |
| SEC-409-GRANT-DUP | PLATFORM-STD (uniqueness) | API-SEC-014, 016, 017 | 409 | grant already exists | هذا المنح موجود بالفعل | This grant already exists |
| SEC-404-MODULE | PLATFORM-STD (not found) | API-SEC-014, 015 | 404 | unknown module id | الوحدة غير موجودة | Module not found |
| SEC-404-GRANT | PLATFORM-STD (not found) | API-SEC-015 | 404 | grant does not exist | المنح غير موجود | Grant not found |
| SEC-409-NO-MODULE-GRANT | RULE-SEC-001 | API-SEC-016 | 409 | screen grant without its module grant | لا يمكن منح شاشة دون منح الوحدة أولًا | Cannot grant a screen without first granting its module |
| SEC-404-SCREEN | PLATFORM-STD (not found) | API-SEC-016, 017, 019 | 404 | unknown screen id or page code | الشاشة غير موجودة | Screen not found |
| SEC-409-NO-SCREEN-GRANT | RULE-SEC-002 | API-SEC-017 | 409 | action grant without its screen grant | لا يمكن منح إجراء دون منح الشاشة أولًا | Cannot grant an action without first granting its screen |
| SEC-409-NO-VIEW-GRANT | RULE-SEC-007 | API-SEC-017 | 409 | non-VIEW action grant without VIEW | يلزم منح إجراء العرض (VIEW) أولًا على هذه الشاشة | The VIEW action must be granted on this screen first |
| SEC-404-ACTION | PLATFORM-STD (not found) | API-SEC-017 | 404 | unknown action id | الإجراء غير موجود | Action not found |
| SEC-409-MODULE-DUP | PLATFORM-STD (uniqueness) | API-SEC-018 | 409 | duplicate module code | رمز الوحدة مستخدم بالفعل | Module code already in use |
| SEC-409-MODULE-NOT-REGISTERED | RULE-SEC-004 | API-SEC-019 | 409 | screen under an unregistered module | الوحدة غير مسجّلة | Module is not registered |
| SEC-409-SCREEN-DUP | PLATFORM-STD (uniqueness) | API-SEC-019 | 409 | duplicate page code | رمز الصفحة مستخدم بالفعل | Page code already in use |
| SEC-409-SCREEN-NOT-REGISTERED | PLATFORM-STD (referential) | API-SEC-020 | 409 | action under an unregistered screen | الشاشة غير مسجّلة | Screen is not registered |
| SEC-409-ACTION-DUP | PLATFORM-STD (uniqueness) | API-SEC-020 | 409 | duplicate permission code | رمز الصلاحية مستخدم بالفعل | Permission code already in use |
| SEC-409-RESET-TOKEN-INVALID | RULE-SEC-006 | API-SEC-004 | 409 | expired or used reset token | رابط إعادة التعيين غير صالح أو منتهي | This reset link is invalid or has expired |
| SEC-409-SIGNUP-DUP | PLATFORM-STD (uniqueness) | API-SEC-002 | 409 | duplicate pending or registered email | البريد الإلكتروني مستخدم بالفعل | This email is already in use |
| SEC-404-SESSION | PLATFORM-STD (not found) | API-SEC-026 | 404 | unknown session id | الجلسة غير موجودة | Session not found |
| SEC-409-ALREADY-TERMINATED | PLATFORM-STD (lifecycle) | API-SEC-026 | 409 | session already terminated | هذه الجلسة منتهية بالفعل | This session is already terminated |
| SEC-403-FORBIDDEN | PLATFORM-STD (RULE-SEC-007 + REQ-SEC-033, the CORE gate) | every secured API | 403 | missing module, screen or action grant — for every principal, v2 a service account included (REQ-SEC-045) | غير مصرح بهذا الإجراء | You are not authorized to perform this action |
| SEC-400-INVALID-SORT | PLATFORM-STD (search contract) | every search API | 400 | unrecognized sort field | حقل الترتيب غير معروف | Unrecognized sort field |
| VALIDATION_ERROR | PLATFORM-STD (shared GlobalExceptionHandler — not module-scoped) | every API with a body | 400 | malformed or invalid body; v2 also a principalTypeCode outside PRINCIPAL_TYPE | the shared handler's own text — SEC declares none | the shared handler's own text — SEC declares none |
| INTERNAL_ERROR | PLATFORM-STD (shared GlobalExceptionHandler — not module-scoped) | any | 500 | unhandled server error | حدث خطأ غير متوقع. يرجى المحاولة لاحقاً. | An unexpected error occurred. Please try again later. |
| SEC-401-INVALID-CLIENT | RULE-SEC-009 | API-SEC-031; every secured API (CORE bearer check of a SERVICE token) | 401 | ADDED v2 — unknown client identifier, not an ACTIVE SERVICE principal, no unrevoked credential matching the secret, or a token whose credential is revoked or whose account is disabled | بيانات اعتماد العميل غير صالحة | Invalid client credentials |
| SEC-400-UNSUPPORTED-GRANT-TYPE | PLATFORM-STD (ADR-SEC-037) | API-SEC-031 | 400 | ADDED v2 — grantType other than client_credentials | نوع المنحة غير مدعوم | Unsupported grant type |
| SEC-409-NOT-ACTIVE-SERVICE-ACCOUNT | RULE-SEC-008 | API-SEC-029 | 409 | ADDED v2 — issuance for a HUMAN user or a DISABLED service account | لا تُصدَر بيانات الاعتماد إلا لحساب خدمة نشط | Credentials can be issued only for an active service account |
| SEC-404-CREDENTIAL | PLATFORM-STD (not found; ADR-SEC-037) | API-SEC-030 | 404 | ADDED v2 — unknown credential id, or one that belongs to another account | بيانات الاعتماد غير موجودة | Credential not found |
| SEC-409-CREDENTIAL-ALREADY-REVOKED | RULE-SEC-011 | API-SEC-030 | 409 | ADDED v2 — the credential's revocation time is already set | بيانات الاعتماد هذه ملغاة بالفعل | This credential is already revoked |
| SEC-409-PRINCIPAL-TYPE-FIXED | RULE-SEC-012 | API-SEC-007 | 409 | ADDED v2 — an update changing the principal type | لا يمكن تغيير نوع الأساس الأمني بعد إنشائه | The principal type cannot be changed after creation |
| SEC-409-CREDENTIAL-LIMIT | RULE-SEC-013 | API-SEC-029 | 409 | ADDED v2 — issuance for an account already holding 10 unrevoked credentials | بلغ عدد بيانات الاعتماد النشطة الحد الأقصى (10) | Active credential limit (10) reached |

Every PLATFORM-STD row follows the umbrella convention ADR-SEC-002 records; the two v2 PLATFORM-STD rows are also
recorded in ADR-SEC-037. `SEC-409-USER-DUP` on a concurrent duplicate is answered by the store as the shared
`DATA_INTEGRITY_VIOLATION` (409) — see API-SEC-006's Concurrency line.

## QR id definitions (cross-reference index — full detail in the Query Reference Catalog above)
**QR-SEC-001** — FIND_ONE user by username [ENT-SEC-001, API-SEC-001, API-SEC-031]
**QR-SEC-002** — SAVE signup request [ENT-SEC-013, API-SEC-002]
**QR-SEC-003** — SAVE password reset token [ENT-SEC-012, API-SEC-003]
**QR-SEC-004** — UPDATE user password + consume token conditionally [ENT-SEC-001, ENT-SEC-012, API-SEC-004]
**QR-SEC-005** — FIND_BY_CRITERIA search users, with principal type [ENT-SEC-001, API-SEC-005]
**QR-SEC-006** — SAVE create user of a principal type [ENT-SEC-001, API-SEC-006, API-SEC-011]
**QR-SEC-007** — UPDATE user profile fields [ENT-SEC-001, API-SEC-007]
**QR-SEC-008** — SAVE assign roles to user [ENT-SEC-003, API-SEC-008]
**QR-SEC-009** — UPDATE deactivate user + terminate sessions [ENT-SEC-001, ENT-SEC-010, API-SEC-009]
**QR-SEC-010** — UPDATE reactivate user [ENT-SEC-001, API-SEC-010]
**QR-SEC-011** — UPDATE approve/reject signup under a write lock [ENT-SEC-013, ENT-SEC-001, API-SEC-011]
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
**QR-SEC-022** — AGGREGATE dashboard figures, users overview per principal type [ENT-SEC-001, ENT-SEC-010, ENT-SEC-011, ENT-SEC-002, API-SEC-022]
**QR-SEC-023** — FIND_BY_CRITERIA search audit log [ENT-SEC-011, API-SEC-023]
**QR-SEC-024** — FIND_BY_CRITERIA audit log export [ENT-SEC-011, API-SEC-024]
**QR-SEC-025** — FIND_BY_CRITERIA non-terminated sessions [ENT-SEC-010, API-SEC-025]
**QR-SEC-026** — UPDATE terminate session conditionally [ENT-SEC-010, API-SEC-026, API-SEC-009]
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
**QR-SEC-040** — FIND_ONE user by PK [ENT-SEC-001, API-SEC-007, API-SEC-008, API-SEC-009, API-SEC-010, API-SEC-028, API-SEC-032]
**QR-SEC-041** — FIND_ONE user by PK under a write lock (RULE-SEC-008) [ENT-SEC-001, API-SEC-029]
**QR-SEC-042** — FIND_ALL a service account's credentials, no secret hash [ENT-SEC-014, API-SEC-028]
**QR-SEC-043** — SAVE a newly issued credential [ENT-SEC-014, API-SEC-029]
**QR-SEC-044** — FIND_ONE credential by PK [ENT-SEC-014, API-SEC-030]
**QR-SEC-045** — UPDATE revoke a credential while unrevoked (RULE-SEC-011) [ENT-SEC-014, API-SEC-030]
**QR-SEC-046** — FIND_ALL a service account's unrevoked credentials (RULE-SEC-009) [ENT-SEC-014, API-SEC-031, API-SEC-029]
**QR-SEC-047** — UPDATE a credential's last use [ENT-SEC-014, API-SEC-031]
**QR-SEC-048** — SAVE audit entry on a rejection path, REQUIRES_NEW [ENT-SEC-011, API-SEC-001, API-SEC-031]
**QR-SEC-049** — FIND_ONE user by email [ENT-SEC-001, API-SEC-003]
**QR-SEC-050** — FIND_ALL a user's assigned roles [ENT-SEC-003, ENT-SEC-002, API-SEC-032, API-SEC-008]
**QR-SEC-051** — FIND_ONE role by PK, write lock on grant-tree edits [ENT-SEC-002, API-SEC-033, API-SEC-034, API-SEC-014, API-SEC-015, API-SEC-016, API-SEC-017]
**QR-SEC-052** — UPDATE role names and descriptions [ENT-SEC-002, API-SEC-034]
**QR-SEC-053** — FIND_ALL a role's grant tree [ENT-SEC-007, ENT-SEC-008, ENT-SEC-009, API-SEC-035]
**QR-SEC-054** — FIND_BY_CRITERIA search sign-up requests [ENT-SEC-013, API-SEC-036]

## Registry content
See `registry-exec-be-sec.md`.
══════════════════════════════════════════════════════════════════
