# DATABASE — الأمان / Security (SEC) — DELTA v2
══════════════════════════════════════════════════════════════════
Module : SEC   Version : v2 (delta on v1 — change set CS-SEC-001, ADDITIVE)   Dialect : postgresql16   Schema prefix : none
Inputs : srs (current state, v1 + v2), registry-srs, db-script v1 (baseline), registry-db v1 (baseline), change-manifest
Identifier transformation : SRS logical field name (camelCase) → physical column
  name (snake_case) — e.g. `principalTypeCode` → `principal_type_code`,
  `serviceAccountCredentialPk` → `service_account_credential_pk`. Applied to every
  identifier this delta adds; no other spelling of any field exists.
PK generation : profile.stack.db.pk_generation = `sequence` — one `SEQ_{TABLE}` per table,
  PK columns plain `BIGINT NOT NULL`, populated by the application from the named sequence.
  v1 built its PKs as identity columns; this migration moves all 13 v1 tables onto named
  sequences so the database carries one strategy only (see the PK generation decision in §4).
Date : 2026-09-23
Counts : 14 tables (+1) · 116 DBF (+12 ADDED, 18 MODIFIED) · 0 XM (SEC is ROOT) · 14 sequences (+14)
Status : COMPLETE — no BLOCKED decision
══════════════════════════════════════════════════════════════════

Delta reading note: the script in §3 is a **migration** on a schema holding the gated SEC v1
script. It contains ALTER / CREATE / DROP statements for the changed objects only. §6 re-issues
only the ADDED and MODIFIED DBF records; every other v1 DBF record is unchanged, and `gov.py state`
carries it over. §1 (matrix), §2 (XM register) and §4 (decisions) have no id to fold by, so they
are restated in full as the current state. Column `v2` marks what changed in them.

## 1. DB FIELD TRACEABILITY MATRIX — SEC v2

Type note: v1's matrix listed intra-module FK columns as `NUMERIC`. v1's own script declares them
`BIGINT`, and the script is what was deployed. The matrix below states `BIGINT`. That corrects the
documentation only; the schema does not change and those DBF records are not modified.

### Table SEC_USER (ENT-SEC-001 — MODIFIED)
| DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default | v2 |
|---|---|---|---|---|---|---|---|
| DBF-SEC-001 | user_pk | BIGINT | ENT-SEC-001 (PK, profile.stack.db.naming.pk_pattern) | REQ-SEC-009 | NOT NULL | — (application, from SEQ_SEC_USER) | MODIFIED (identity → sequence) |
| DBF-SEC-002 | username | VARCHAR(100) | ENT-SEC-001.username | REQ-SEC-001, REQ-SEC-009, REQ-SEC-036, REQ-SEC-046, REQ-SEC-047, REQ-SEC-063 | NOT NULL | — | MODIFIED (also the machine client identifier, DEFAULT D2) |
| DBF-SEC-105 | principal_type_code | VARCHAR(20) | ENT-SEC-001.principalTypeCode (A6 lookup PRINCIPAL_TYPE) | REQ-SEC-036, REQ-SEC-038, REQ-SEC-039, REQ-SEC-040, REQ-SEC-041, REQ-SEC-042 | NOT NULL | 'HUMAN' | ADDED |
| DBF-SEC-003 | email | VARCHAR(255) | ENT-SEC-001.email | REQ-SEC-006, REQ-SEC-009, REQ-SEC-034, REQ-SEC-036, REQ-SEC-064 | NOT NULL | — | MODIFIED (service account: contact email, ADR-SEC-032) |
| DBF-SEC-004 | password_hash | TEXT | ENT-SEC-001.passwordHash | REQ-SEC-001, REQ-SEC-007, REQ-SEC-036, REQ-SEC-063 | NOT NULL | — | MODIFIED (service account: unusable random value, ADR-SEC-032) |
| DBF-SEC-005 | full_name_ar | VARCHAR(200) | ENT-SEC-001.fullNameAr | REQ-SEC-009, REQ-SEC-034 | NOT NULL | — | — |
| DBF-SEC-006 | full_name_en | VARCHAR(200) | ENT-SEC-001.fullNameEn | REQ-SEC-009, REQ-SEC-034 | NOT NULL | — | — |
| DBF-SEC-007 | status_code | VARCHAR(20) | ENT-SEC-001.statusCode (A6 lookup USER_STATUS) | REQ-SEC-004, REQ-SEC-009, REQ-SEC-011, REQ-SEC-031, REQ-SEC-034, REQ-SEC-036, REQ-SEC-052, REQ-SEC-060, REQ-SEC-062 | NOT NULL | 'ACTIVE' | MODIFIED (service account: ACTIVE / DISABLED only) |
| DBF-SEC-008 | last_login_at | TIMESTAMPTZ | ENT-SEC-001.lastLoginAt | REQ-SEC-001 | NULL | — | — |
| DBF-SEC-009 | is_active_fl | BOOLEAN | ENT-SEC-001.isActiveFl | REQ-SEC-011, REQ-SEC-031 | NOT NULL | TRUE | — |
| DBF-SEC-010 | created_by | VARCHAR(100) | profile: entity_defaults.security (audit) + ENT-SEC-001 | REQ-SEC-009 | NOT NULL | — | — |
| DBF-SEC-011 | created_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) + ENT-SEC-001 | REQ-SEC-009 | NOT NULL | now() | — |
| DBF-SEC-012 | updated_by | VARCHAR(100) | profile: entity_defaults.security (audit) + ENT-SEC-001 | REQ-SEC-009 | NULL | — | — |
| DBF-SEC-013 | updated_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) + ENT-SEC-001 | REQ-SEC-009 | NULL | — | — |

`principal_type_code` is placed in SRS order above. Physically, `ALTER TABLE … ADD COLUMN` appends it as the last column.
Its DBF id continues the module sequence (105).

### Table SEC_ROLE (ENT-SEC-002)
| DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default | v2 |
|---|---|---|---|---|---|---|---|
| DBF-SEC-014 | role_pk | BIGINT | ENT-SEC-002 (PK) | REQ-SEC-012 | NOT NULL | — (application, from SEQ_SEC_ROLE) | MODIFIED (identity → sequence) |
| DBF-SEC-015 | code | VARCHAR(50) | ENT-SEC-002.code | REQ-SEC-012 | NOT NULL | — | — |
| DBF-SEC-016 | name_ar | VARCHAR(150) | ENT-SEC-002.nameAr | REQ-SEC-012 | NOT NULL | — | — |
| DBF-SEC-017 | name_en | VARCHAR(150) | ENT-SEC-002.nameEn | REQ-SEC-012 | NOT NULL | — | — |
| DBF-SEC-018 | description_ar | VARCHAR(500) | ENT-SEC-002.descriptionAr | REQ-SEC-012 | NULL | — | — |
| DBF-SEC-019 | description_en | VARCHAR(500) | ENT-SEC-002.descriptionEn | REQ-SEC-012 | NULL | — | — |
| DBF-SEC-020 | is_active_fl | BOOLEAN | ENT-SEC-002.isActiveFl | REQ-SEC-012 | NOT NULL | TRUE | — |
| DBF-SEC-021 | created_by | VARCHAR(100) | profile: entity_defaults.security (audit) | REQ-SEC-012 | NOT NULL | — | — |
| DBF-SEC-022 | created_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) | REQ-SEC-012 | NOT NULL | now() | — |
| DBF-SEC-023 | updated_by | VARCHAR(100) | profile: entity_defaults.security (audit) | REQ-SEC-012 | NULL | — | — |
| DBF-SEC-024 | updated_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) | REQ-SEC-012 | NULL | — | — |

### Table SEC_USER_ROLE (ENT-SEC-003 UserRoleAssignment)
| DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default | v2 |
|---|---|---|---|---|---|---|---|
| DBF-SEC-025 | user_role_pk | BIGINT | ENT-SEC-003 (PK) | REQ-SEC-010 | NOT NULL | — (application, from SEQ_SEC_USER_ROLE) | MODIFIED (identity → sequence) |
| DBF-SEC-026 | user_id | BIGINT | ENT-SEC-003.userId → FK ENT-SEC-001 | REQ-SEC-010, REQ-SEC-035 | NOT NULL | — | — |
| DBF-SEC-027 | role_id | BIGINT | ENT-SEC-003.roleId → FK ENT-SEC-002 | REQ-SEC-010, REQ-SEC-035 | NOT NULL | — | — |
| DBF-SEC-028 | assigned_by | VARCHAR(100) | ENT-SEC-003.assignedBy | REQ-SEC-010 | NOT NULL | — | — |
| DBF-SEC-029 | assigned_at | TIMESTAMPTZ | ENT-SEC-003.assignedAt | REQ-SEC-010 | NOT NULL | now() | — |

### Table SEC_MODULE_REG (ENT-SEC-004 ModuleRegistry)
| DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default | v2 |
|---|---|---|---|---|---|---|---|
| DBF-SEC-030 | module_reg_pk | BIGINT | ENT-SEC-004 (PK) | REQ-SEC-016 | NOT NULL | — (application, from SEQ_SEC_MODULE_REG) | MODIFIED (identity → sequence) |
| DBF-SEC-031 | code | VARCHAR(10) | ENT-SEC-004.code | REQ-SEC-016 | NOT NULL | — | — |
| DBF-SEC-032 | name_ar | VARCHAR(150) | ENT-SEC-004.nameAr | REQ-SEC-016 | NOT NULL | — | — |
| DBF-SEC-033 | name_en | VARCHAR(150) | ENT-SEC-004.nameEn | REQ-SEC-016 | NOT NULL | — | — |
| DBF-SEC-034 | is_active_fl | BOOLEAN | ENT-SEC-004.isActiveFl | REQ-SEC-016 | NOT NULL | TRUE | — |
| DBF-SEC-035 | created_by | VARCHAR(100) | profile: entity_defaults.security (audit) | REQ-SEC-016 | NOT NULL | — | — |
| DBF-SEC-036 | created_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) | REQ-SEC-016 | NOT NULL | now() | — |
| DBF-SEC-037 | updated_by | VARCHAR(100) | profile: entity_defaults.security (audit) | REQ-SEC-016 | NULL | — | — |
| DBF-SEC-038 | updated_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) | REQ-SEC-016 | NULL | — | — |

### Table SEC_SCREEN_REG (ENT-SEC-005 ScreenRegistry)
| DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default | v2 |
|---|---|---|---|---|---|---|---|
| DBF-SEC-039 | screen_reg_pk | BIGINT | ENT-SEC-005 (PK) | REQ-SEC-017 | NOT NULL | — (application, from SEQ_SEC_SCREEN_REG) | MODIFIED (identity → sequence) |
| DBF-SEC-040 | page_code | VARCHAR(50) | ENT-SEC-005.pageCode | REQ-SEC-017 | NOT NULL | — | — |
| DBF-SEC-041 | module_id | BIGINT | ENT-SEC-005.moduleId → FK ENT-SEC-004 [RULE-SEC-004] | REQ-SEC-017, REQ-SEC-018 | NOT NULL | — | — |
| DBF-SEC-042 | name_ar | VARCHAR(150) | ENT-SEC-005.nameAr | REQ-SEC-017 | NOT NULL | — | — |
| DBF-SEC-043 | name_en | VARCHAR(150) | ENT-SEC-005.nameEn | REQ-SEC-017 | NOT NULL | — | — |
| DBF-SEC-044 | is_active_fl | BOOLEAN | ENT-SEC-005.isActiveFl | REQ-SEC-017 | NOT NULL | TRUE | — |
| DBF-SEC-045 | created_by | VARCHAR(100) | profile: entity_defaults.security (audit) | REQ-SEC-017 | NOT NULL | — | — |
| DBF-SEC-046 | created_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) | REQ-SEC-017 | NOT NULL | now() | — |
| DBF-SEC-047 | updated_by | VARCHAR(100) | profile: entity_defaults.security (audit) | REQ-SEC-017 | NULL | — | — |
| DBF-SEC-048 | updated_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) | REQ-SEC-017 | NULL | — | — |

### Table SEC_ACTION_REG (ENT-SEC-006 ActionRegistry)
| DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default | v2 |
|---|---|---|---|---|---|---|---|
| DBF-SEC-049 | action_reg_pk | BIGINT | ENT-SEC-006 (PK) | REQ-SEC-019 | NOT NULL | — (application, from SEQ_SEC_ACTION_REG) | MODIFIED (identity → sequence) |
| DBF-SEC-050 | permission_code | VARCHAR(100) | ENT-SEC-006.permissionCode | REQ-SEC-019 | NOT NULL | — | — |
| DBF-SEC-051 | screen_id | BIGINT | ENT-SEC-006.screenId → FK ENT-SEC-005 | REQ-SEC-019 | NOT NULL | — | — |
| DBF-SEC-052 | action_code | VARCHAR(40) | ENT-SEC-006.actionCode | REQ-SEC-019 | NOT NULL | — | — |
| DBF-SEC-053 | name_ar | VARCHAR(150) | ENT-SEC-006.nameAr | REQ-SEC-019 | NOT NULL | — | — |
| DBF-SEC-054 | name_en | VARCHAR(150) | ENT-SEC-006.nameEn | REQ-SEC-019 | NOT NULL | — | — |
| DBF-SEC-055 | is_active_fl | BOOLEAN | ENT-SEC-006.isActiveFl | REQ-SEC-019 | NOT NULL | TRUE | — |
| DBF-SEC-056 | created_by | VARCHAR(100) | profile: entity_defaults.security (audit) | REQ-SEC-019 | NOT NULL | — | — |
| DBF-SEC-057 | created_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) | REQ-SEC-019 | NOT NULL | now() | — |
| DBF-SEC-058 | updated_by | VARCHAR(100) | profile: entity_defaults.security (audit) | REQ-SEC-019 | NULL | — | — |
| DBF-SEC-059 | updated_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) | REQ-SEC-019 | NULL | — | — |

### Table SEC_ROLE_MODULE_GRANT (ENT-SEC-007)
| DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default | v2 |
|---|---|---|---|---|---|---|---|
| DBF-SEC-060 | role_module_grant_pk | BIGINT | ENT-SEC-007 (PK) | REQ-SEC-012 | NOT NULL | — (application, from SEQ_SEC_ROLE_MODULE_GRANT) | MODIFIED (identity → sequence) |
| DBF-SEC-061 | role_id | BIGINT | ENT-SEC-007.roleId → FK ENT-SEC-002 | REQ-SEC-012, REQ-SEC-015 | NOT NULL | — | — |
| DBF-SEC-062 | module_id | BIGINT | ENT-SEC-007.moduleId → FK ENT-SEC-004 | REQ-SEC-012, REQ-SEC-015 | NOT NULL | — | — |
| DBF-SEC-063 | granted_by | VARCHAR(100) | ENT-SEC-007.grantedBy | REQ-SEC-012 | NOT NULL | — | — |
| DBF-SEC-064 | granted_at | TIMESTAMPTZ | ENT-SEC-007.grantedAt | REQ-SEC-012 | NOT NULL | now() | — |

### Table SEC_ROLE_SCREEN_GRANT (ENT-SEC-008)
| DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default | v2 |
|---|---|---|---|---|---|---|---|
| DBF-SEC-065 | role_screen_grant_pk | BIGINT | ENT-SEC-008 (PK) | REQ-SEC-013 | NOT NULL | — (application, from SEQ_SEC_ROLE_SCREEN_GRANT) | MODIFIED (identity → sequence) |
| DBF-SEC-066 | role_id | BIGINT | ENT-SEC-008.roleId → FK ENT-SEC-002 [RULE-SEC-001, app-layer] | REQ-SEC-013, REQ-SEC-015 | NOT NULL | — | — |
| DBF-SEC-067 | screen_id | BIGINT | ENT-SEC-008.screenId → FK ENT-SEC-005 | REQ-SEC-013 | NOT NULL | — | — |
| DBF-SEC-068 | granted_by | VARCHAR(100) | ENT-SEC-008.grantedBy | REQ-SEC-013 | NOT NULL | — | — |
| DBF-SEC-069 | granted_at | TIMESTAMPTZ | ENT-SEC-008.grantedAt | REQ-SEC-013 | NOT NULL | now() | — |

### Table SEC_ROLE_ACTION_GRANT (ENT-SEC-009)
| DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default | v2 |
|---|---|---|---|---|---|---|---|
| DBF-SEC-070 | role_action_grant_pk | BIGINT | ENT-SEC-009 (PK) | REQ-SEC-014 | NOT NULL | — (application, from SEQ_SEC_ROLE_ACTION_GRANT) | MODIFIED (identity → sequence) |
| DBF-SEC-071 | role_id | BIGINT | ENT-SEC-009.roleId → FK ENT-SEC-002 [RULE-SEC-002, RULE-SEC-005, RULE-SEC-007 — app-layer] | REQ-SEC-014, REQ-SEC-020, REQ-SEC-030 | NOT NULL | — | — |
| DBF-SEC-072 | action_id | BIGINT | ENT-SEC-009.actionId → FK ENT-SEC-006 | REQ-SEC-014, REQ-SEC-035 | NOT NULL | — | — |
| DBF-SEC-073 | granted_by | VARCHAR(100) | ENT-SEC-009.grantedBy | REQ-SEC-014 | NOT NULL | — | — |
| DBF-SEC-074 | granted_at | TIMESTAMPTZ | ENT-SEC-009.grantedAt | REQ-SEC-014 | NOT NULL | now() | — |

### Table SEC_ACTIVE_SESSION (ENT-SEC-010)
| DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default | v2 |
|---|---|---|---|---|---|---|---|
| DBF-SEC-075 | active_session_pk | BIGINT | ENT-SEC-010 (PK) | REQ-SEC-001 | NOT NULL | — (application, from SEQ_SEC_ACTIVE_SESSION) | MODIFIED (identity → sequence) |
| DBF-SEC-076 | user_id | BIGINT | ENT-SEC-010.userId → FK ENT-SEC-001 | REQ-SEC-001, REQ-SEC-011, REQ-SEC-027 | NOT NULL | — | — |
| DBF-SEC-077 | token_ref | VARCHAR(200) | ENT-SEC-010.tokenRef | REQ-SEC-001 | NOT NULL | — | — |
| DBF-SEC-078 | started_at | TIMESTAMPTZ | ENT-SEC-010.startedAt | REQ-SEC-001 | NOT NULL | now() | — |
| DBF-SEC-079 | last_activity_at | TIMESTAMPTZ | ENT-SEC-010.lastActivityAt | REQ-SEC-027 | NOT NULL | now() | — |
| DBF-SEC-080 | ip_address | VARCHAR(64) | ENT-SEC-010.ipAddress | REQ-SEC-027 | NULL | — | — |
| DBF-SEC-081 | terminated_at | TIMESTAMPTZ | ENT-SEC-010.terminatedAt | REQ-SEC-011, REQ-SEC-028 | NULL | — | — |
| DBF-SEC-082 | terminated_by | VARCHAR(100) | ENT-SEC-010.terminatedBy | REQ-SEC-011, REQ-SEC-028 | NULL | — | — |

### Table SEC_AUDIT_LOG (ENT-SEC-011 AuditLogEntry)
| DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default | v2 |
|---|---|---|---|---|---|---|---|
| DBF-SEC-083 | audit_log_pk | BIGINT | ENT-SEC-011 (PK) | REQ-SEC-024 | NOT NULL | — (application, from SEQ_SEC_AUDIT_LOG) | MODIFIED (identity → sequence) |
| DBF-SEC-084 | event_type_code | VARCHAR(40) | ENT-SEC-011.eventTypeCode (A6 lookup AUDIT_EVENT_TYPE) | REQ-SEC-024, REQ-SEC-065, REQ-SEC-066, REQ-SEC-067, REQ-SEC-068, REQ-SEC-069, REQ-SEC-070, REQ-SEC-071, REQ-SEC-079 | NOT NULL | — | MODIFIED (value set 14 → 21: seven SERVICE_* codes, revise pass G6 adds SERVICE_ACCOUNT_REACTIVATED) |
| DBF-SEC-085 | actor_user_id | BIGINT | ENT-SEC-011.actorUserId → FK ENT-SEC-001 (nullable) | REQ-SEC-002, REQ-SEC-024 | NULL | — | — |
| DBF-SEC-086 | occurred_at | TIMESTAMPTZ | ENT-SEC-011.occurredAt | REQ-SEC-024 | NOT NULL | now() | — |
| DBF-SEC-087 | target_ref | VARCHAR(200) | ENT-SEC-011.targetRef | REQ-SEC-024 | NULL | — | — |
| DBF-SEC-088 | details_ar | TEXT | ENT-SEC-011.detailsAr | REQ-SEC-024 | NULL | — | — |
| DBF-SEC-089 | details_en | TEXT | ENT-SEC-011.detailsEn | REQ-SEC-024 | NULL | — | — |
| DBF-SEC-090 | ip_address | VARCHAR(64) | ENT-SEC-011.ipAddress | REQ-SEC-024 | NULL | — | — |

### Table SEC_PWD_RESET_TOKEN (ENT-SEC-012 PasswordResetToken)
| DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default | v2 |
|---|---|---|---|---|---|---|---|
| DBF-SEC-091 | pwd_reset_token_pk | BIGINT | ENT-SEC-012 (PK) | REQ-SEC-006 | NOT NULL | — (application, from SEQ_SEC_PWD_RESET_TOKEN) | MODIFIED (identity → sequence) |
| DBF-SEC-092 | user_id | BIGINT | ENT-SEC-012.userId → FK ENT-SEC-001 | REQ-SEC-006 | NOT NULL | — | — |
| DBF-SEC-093 | token_hash | TEXT | ENT-SEC-012.tokenHash | REQ-SEC-006, REQ-SEC-007 | NOT NULL | — | — |
| DBF-SEC-094 | requested_at | TIMESTAMPTZ | ENT-SEC-012.requestedAt | REQ-SEC-006 | NOT NULL | now() | — |
| DBF-SEC-095 | expires_at | TIMESTAMPTZ | ENT-SEC-012.expiresAt (A7 DEFAULT: requestedAt + 30 min) | REQ-SEC-006, REQ-SEC-008 | NOT NULL | — | — |
| DBF-SEC-096 | used_at | TIMESTAMPTZ | ENT-SEC-012.usedAt [RULE-SEC-006, app-layer] | REQ-SEC-007, REQ-SEC-008 | NULL | — | — |

### Table SEC_SIGNUP_REQUEST (ENT-SEC-013)
| DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default | v2 |
|---|---|---|---|---|---|---|---|
| DBF-SEC-097 | signup_request_pk | BIGINT | ENT-SEC-013 (PK) | REQ-SEC-003 | NOT NULL | — (application, from SEQ_SEC_SIGNUP_REQUEST) | MODIFIED (identity → sequence) |
| DBF-SEC-098 | email | VARCHAR(255) | ENT-SEC-013.email | REQ-SEC-003 | NOT NULL | — | — |
| DBF-SEC-099 | full_name_ar | VARCHAR(200) | ENT-SEC-013.fullNameAr | REQ-SEC-003 | NOT NULL | — | — |
| DBF-SEC-100 | full_name_en | VARCHAR(200) | ENT-SEC-013.fullNameEn | REQ-SEC-003 | NOT NULL | — | — |
| DBF-SEC-101 | submitted_at | TIMESTAMPTZ | ENT-SEC-013.submittedAt | REQ-SEC-003 | NOT NULL | now() | — |
| DBF-SEC-102 | status_code | VARCHAR(20) | ENT-SEC-013.statusCode (A6 lookup SIGNUP_STATUS) | REQ-SEC-003, REQ-SEC-004, REQ-SEC-005 | NOT NULL | 'PENDING' | — |
| DBF-SEC-103 | reviewed_by | VARCHAR(100) | ENT-SEC-013.reviewedBy | REQ-SEC-004, REQ-SEC-005 | NULL | — | — |
| DBF-SEC-104 | reviewed_at | TIMESTAMPTZ | ENT-SEC-013.reviewedAt | REQ-SEC-004, REQ-SEC-005 | NULL | — | — |

### Table SEC_SVC_ACCOUNT_CRED (ENT-SEC-014 ServiceAccountCredential — ADDED)
| DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default | v2 |
|---|---|---|---|---|---|---|---|
| DBF-SEC-106 | service_account_credential_pk | BIGINT | ENT-SEC-014.serviceAccountCredentialPk (PK, profile.stack.db.naming.pk_pattern) | REQ-SEC-051 | NOT NULL | — (application, from SEQ_SEC_SVC_ACCOUNT_CRED) | ADDED |
| DBF-SEC-107 | secret_hash | TEXT | ENT-SEC-014.secretHash | REQ-SEC-046, REQ-SEC-047, REQ-SEC-053, REQ-SEC-054 | NOT NULL | — | ADDED |
| DBF-SEC-108 | description | VARCHAR(500) | ENT-SEC-014.description | REQ-SEC-051, REQ-SEC-056 | NULL | — | ADDED |
| DBF-SEC-109 | last_used_at | TIMESTAMPTZ | ENT-SEC-014.lastUsedAt | REQ-SEC-056, REQ-SEC-057 | NULL | — | ADDED |
| DBF-SEC-110 | revoked_at | TIMESTAMPTZ | ENT-SEC-014.revokedAt [RULE-SEC-009, RULE-SEC-011, app-layer] | REQ-SEC-055, REQ-SEC-056, REQ-SEC-058, REQ-SEC-059, REQ-SEC-061 | NULL | — | ADDED |
| DBF-SEC-111 | revoked_by | VARCHAR(100) | ENT-SEC-014.revokedBy (principal string, as v1 `terminated_by` / `reviewed_by`) | REQ-SEC-058, REQ-SEC-061 | NULL | — | ADDED |
| DBF-SEC-112 | user_id | BIGINT | ENT-SEC-014.userId → FK ENT-SEC-001 [RULE-SEC-008, RULE-SEC-009, app-layer] | REQ-SEC-046, REQ-SEC-051, REQ-SEC-052, REQ-SEC-056, REQ-SEC-060 | NOT NULL | — | ADDED |
| DBF-SEC-113 | created_by | VARCHAR(100) | profile: entity_defaults.security (audit) + ENT-SEC-014 (the issuing administrator) | REQ-SEC-051 | NOT NULL | — | ADDED |
| DBF-SEC-114 | created_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) + ENT-SEC-014 (the issuance time) | REQ-SEC-051, REQ-SEC-056 | NOT NULL | now() | ADDED |
| DBF-SEC-115 | updated_by | VARCHAR(100) | profile: entity_defaults.security (audit) + ENT-SEC-014 | REQ-SEC-057, REQ-SEC-058 | NULL | — | ADDED |
| DBF-SEC-116 | updated_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) + ENT-SEC-014 | REQ-SEC-057, REQ-SEC-058 | NULL | — | ADDED |

Total: 116 DBF ids across 14 tables (v2: +12 ADDED, 18 MODIFIED, 0 REMOVED).

## 2. XM REGISTER — SEC v2

None. SEC is ROOT. SRS A8's consumed-entity table is empty in v2 as it was in v1. Every
reference this delta adds stays inside the module: `SEC_SVC_ACCOUNT_CRED.user_id` → `SEC_USER` is an
intra-module FK (§6 FK class INTRA-MODULE), so it gets no XM row. The external Oracle event consumer
is a *caller* that authenticates as a SEC service account. It is not a platform module and has no
table, so it gets no XM (SRS A8, v2 note). The EXPOSED direction (`com.erp.sec.crossmodule`,
REQ-SEC-034 / REQ-SEC-035) is unchanged. Its XM ids belong to the consuming module's own P2, as
in v1. Last XM: none assigned.

## 3. FULL_DATABASE_SCRIPT

```sql
-- ════════════════════════════════════════════════════════════════
-- SEC v2 — Security module — PostgreSQL 16 — MIGRATION on SEC v1 (CS-SEC-001)
-- Precondition : the schema holds the gated SEC v1 script, unchanged
-- Identifier transformation: camelCase (SRS) -> snake_case (DB)
-- PK generation : sequence (profile.stack.db.pk_generation) — one SEQ_{TABLE} per table
-- Runs as one transaction: BEGIN below, COMMIT after BLOCK 10.
-- ════════════════════════════════════════════════════════════════

BEGIN;

-- BLOCK 1 — SEQUENCES (one per table: 14 tables, 14 sequences)

-- v2 ADDED table
CREATE SEQUENCE SEQ_SEC_SVC_ACCOUNT_CRED START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;

-- v1 tables: PK generation moves from identity to a named sequence (PK generation decision, §4)
CREATE SEQUENCE SEQ_SEC_USER START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE SEQ_SEC_ROLE START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE SEQ_SEC_USER_ROLE START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE SEQ_SEC_MODULE_REG START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE SEQ_SEC_SCREEN_REG START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE SEQ_SEC_ACTION_REG START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE SEQ_SEC_ROLE_MODULE_GRANT START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE SEQ_SEC_ROLE_SCREEN_GRANT START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE SEQ_SEC_ROLE_ACTION_GRANT START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE SEQ_SEC_ACTIVE_SESSION START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE SEQ_SEC_AUDIT_LOG START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE SEQ_SEC_PWD_RESET_TOKEN START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;
CREATE SEQUENCE SEQ_SEC_SIGNUP_REQUEST START WITH 1 INCREMENT BY 1 CACHE 1 NO CYCLE;

-- Lock every v1 table before reading MAX(pk): held until COMMIT (ACCESS EXCLUSIVE),
-- so no v1 backend can insert a row in the gap between this read and DROP IDENTITY
-- below (G3 fix — the v1 backend must also be stopped before this migration runs,
-- ADR-SEC-035 Consequences).
LOCK TABLE SEC_USER, SEC_ROLE, SEC_USER_ROLE, SEC_MODULE_REG, SEC_SCREEN_REG, SEC_ACTION_REG,
  SEC_ROLE_MODULE_GRANT, SEC_ROLE_SCREEN_GRANT, SEC_ROLE_ACTION_GRANT, SEC_ACTIVE_SESSION,
  SEC_AUDIT_LOG, SEC_PWD_RESET_TOKEN, SEC_SIGNUP_REQUEST
  IN ACCESS EXCLUSIVE MODE;

-- Realign each v1 sequence past the keys the identity columns already issued:
-- the next value handed out is MAX(pk) + 1, or 1 for an empty table. Safe now:
-- the lock above prevents any insert between this read and DROP IDENTITY.
SELECT setval('SEQ_SEC_USER',              COALESCE((SELECT MAX(user_pk)              FROM SEC_USER), 0) + 1, false);
SELECT setval('SEQ_SEC_ROLE',              COALESCE((SELECT MAX(role_pk)              FROM SEC_ROLE), 0) + 1, false);
SELECT setval('SEQ_SEC_USER_ROLE',         COALESCE((SELECT MAX(user_role_pk)         FROM SEC_USER_ROLE), 0) + 1, false);
SELECT setval('SEQ_SEC_MODULE_REG',        COALESCE((SELECT MAX(module_reg_pk)        FROM SEC_MODULE_REG), 0) + 1, false);
SELECT setval('SEQ_SEC_SCREEN_REG',        COALESCE((SELECT MAX(screen_reg_pk)        FROM SEC_SCREEN_REG), 0) + 1, false);
SELECT setval('SEQ_SEC_ACTION_REG',        COALESCE((SELECT MAX(action_reg_pk)        FROM SEC_ACTION_REG), 0) + 1, false);
SELECT setval('SEQ_SEC_ROLE_MODULE_GRANT', COALESCE((SELECT MAX(role_module_grant_pk) FROM SEC_ROLE_MODULE_GRANT), 0) + 1, false);
SELECT setval('SEQ_SEC_ROLE_SCREEN_GRANT', COALESCE((SELECT MAX(role_screen_grant_pk) FROM SEC_ROLE_SCREEN_GRANT), 0) + 1, false);
SELECT setval('SEQ_SEC_ROLE_ACTION_GRANT', COALESCE((SELECT MAX(role_action_grant_pk) FROM SEC_ROLE_ACTION_GRANT), 0) + 1, false);
SELECT setval('SEQ_SEC_ACTIVE_SESSION',    COALESCE((SELECT MAX(active_session_pk)    FROM SEC_ACTIVE_SESSION), 0) + 1, false);
SELECT setval('SEQ_SEC_AUDIT_LOG',         COALESCE((SELECT MAX(audit_log_pk)         FROM SEC_AUDIT_LOG), 0) + 1, false);
SELECT setval('SEQ_SEC_PWD_RESET_TOKEN',   COALESCE((SELECT MAX(pwd_reset_token_pk)   FROM SEC_PWD_RESET_TOKEN), 0) + 1, false);
SELECT setval('SEQ_SEC_SIGNUP_REQUEST',    COALESCE((SELECT MAX(signup_request_pk)    FROM SEC_SIGNUP_REQUEST), 0) + 1, false);

-- BLOCK 2 — PARENT TABLES (no FK dependencies) — v1 tables altered; no new parent table

-- SEC_USER (ENT-SEC-001, MODIFIED): identity dropped (the column stays BIGINT NOT NULL);
-- principal type added. Every v1 row is a human user, so the default backfills HUMAN.
ALTER TABLE SEC_USER            ALTER COLUMN user_pk DROP IDENTITY;
ALTER TABLE SEC_USER            ADD COLUMN principal_type_code VARCHAR(20) NOT NULL DEFAULT 'HUMAN';
ALTER TABLE SEC_ROLE            ALTER COLUMN role_pk DROP IDENTITY;
ALTER TABLE SEC_MODULE_REG      ALTER COLUMN module_reg_pk DROP IDENTITY;
ALTER TABLE SEC_SIGNUP_REQUEST  ALTER COLUMN signup_request_pk DROP IDENTITY;

-- BLOCK 3 — CHILD TABLES (parents already exist; chain respected)

ALTER TABLE SEC_USER_ROLE          ALTER COLUMN user_role_pk DROP IDENTITY;
ALTER TABLE SEC_SCREEN_REG         ALTER COLUMN screen_reg_pk DROP IDENTITY;
ALTER TABLE SEC_ACTION_REG         ALTER COLUMN action_reg_pk DROP IDENTITY;
ALTER TABLE SEC_ROLE_MODULE_GRANT  ALTER COLUMN role_module_grant_pk DROP IDENTITY;
ALTER TABLE SEC_ROLE_SCREEN_GRANT  ALTER COLUMN role_screen_grant_pk DROP IDENTITY;
ALTER TABLE SEC_ROLE_ACTION_GRANT  ALTER COLUMN role_action_grant_pk DROP IDENTITY;
ALTER TABLE SEC_ACTIVE_SESSION     ALTER COLUMN active_session_pk DROP IDENTITY;
ALTER TABLE SEC_AUDIT_LOG          ALTER COLUMN audit_log_pk DROP IDENTITY;
ALTER TABLE SEC_PWD_RESET_TOKEN    ALTER COLUMN pwd_reset_token_pk DROP IDENTITY;

-- v2 ADDED: ENT-SEC-014 ServiceAccountCredential (child of SEC_USER)
CREATE TABLE SEC_SVC_ACCOUNT_CRED (
  service_account_credential_pk  BIGINT        NOT NULL,
  secret_hash                    TEXT          NOT NULL,
  description                    VARCHAR(500),
  last_used_at                   TIMESTAMPTZ,
  revoked_at                     TIMESTAMPTZ,
  revoked_by                     VARCHAR(100),
  user_id                        BIGINT        NOT NULL,
  created_by                     VARCHAR(100)  NOT NULL,
  created_at                     TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_by                     VARCHAR(100),
  updated_at                     TIMESTAMPTZ
);

-- BLOCK 4 — COMMENTS (the new table and all its columns; every v1 column whose meaning or generation changed)

COMMENT ON TABLE SEC_USER IS 'ENT-SEC-001 User (HUMAN and SERVICE principals) — SHARED(owner); [DBF-SEC-001..013, DBF-SEC-105]';
COMMENT ON COLUMN SEC_USER.user_pk IS 'DBF-SEC-001 — populated by the application from SEQ_SEC_USER';
COMMENT ON COLUMN SEC_USER.username IS 'DBF-SEC-002 — login identity; for a service account also the client identifier of machine authentication';
COMMENT ON COLUMN SEC_USER.principal_type_code IS 'DBF-SEC-105 — lookup PRINCIPAL_TYPE (ADR-SEC-001); set at creation, read-only afterwards (RULE-SEC-012, application layer)';
COMMENT ON COLUMN SEC_USER.email IS 'DBF-SEC-003 — service account: the responsible contact address, never a password-reset target (ADR-SEC-032)';
COMMENT ON COLUMN SEC_USER.password_hash IS 'DBF-SEC-004 — never returned to any client (POL-SEC-004); service account: an unusable random value (ADR-SEC-032)';
COMMENT ON COLUMN SEC_USER.status_code IS 'DBF-SEC-007 — lookup USER_STATUS (ADR-SEC-001); a service account is ACTIVE or DISABLED, never PENDING';

COMMENT ON COLUMN SEC_ROLE.role_pk IS 'DBF-SEC-014 — populated by the application from SEQ_SEC_ROLE';
COMMENT ON COLUMN SEC_USER_ROLE.user_role_pk IS 'DBF-SEC-025 — populated by the application from SEQ_SEC_USER_ROLE';
COMMENT ON COLUMN SEC_MODULE_REG.module_reg_pk IS 'DBF-SEC-030 — populated by the application from SEQ_SEC_MODULE_REG';
COMMENT ON COLUMN SEC_SCREEN_REG.screen_reg_pk IS 'DBF-SEC-039 — populated by the application from SEQ_SEC_SCREEN_REG';
COMMENT ON COLUMN SEC_ACTION_REG.action_reg_pk IS 'DBF-SEC-049 — populated by the application from SEQ_SEC_ACTION_REG';
COMMENT ON COLUMN SEC_ROLE_MODULE_GRANT.role_module_grant_pk IS 'DBF-SEC-060 — populated by the application from SEQ_SEC_ROLE_MODULE_GRANT';
COMMENT ON COLUMN SEC_ROLE_SCREEN_GRANT.role_screen_grant_pk IS 'DBF-SEC-065 — populated by the application from SEQ_SEC_ROLE_SCREEN_GRANT';
COMMENT ON COLUMN SEC_ROLE_ACTION_GRANT.role_action_grant_pk IS 'DBF-SEC-070 — populated by the application from SEQ_SEC_ROLE_ACTION_GRANT';
COMMENT ON COLUMN SEC_ACTIVE_SESSION.active_session_pk IS 'DBF-SEC-075 — populated by the application from SEQ_SEC_ACTIVE_SESSION';
COMMENT ON COLUMN SEC_AUDIT_LOG.audit_log_pk IS 'DBF-SEC-083 — populated by the application from SEQ_SEC_AUDIT_LOG';
COMMENT ON COLUMN SEC_AUDIT_LOG.event_type_code IS 'DBF-SEC-084 — lookup AUDIT_EVENT_TYPE (ADR-SEC-001); 21 codes incl. the seven SERVICE_* codes';
COMMENT ON COLUMN SEC_PWD_RESET_TOKEN.pwd_reset_token_pk IS 'DBF-SEC-091 — populated by the application from SEQ_SEC_PWD_RESET_TOKEN';
COMMENT ON COLUMN SEC_SIGNUP_REQUEST.signup_request_pk IS 'DBF-SEC-097 — populated by the application from SEQ_SEC_SIGNUP_REQUEST';

COMMENT ON TABLE SEC_SVC_ACCOUNT_CRED IS 'ENT-SEC-014 ServiceAccountCredential — PRIVATE; RULE-SEC-008/009/011 enforced at application layer (P3.1); [DBF-SEC-106..116]';
COMMENT ON COLUMN SEC_SVC_ACCOUNT_CRED.service_account_credential_pk IS 'DBF-SEC-106 — populated by the application from SEQ_SEC_SVC_ACCOUNT_CRED';
COMMENT ON COLUMN SEC_SVC_ACCOUNT_CRED.secret_hash IS 'DBF-SEC-107 — one-way hash of the secret, write-once; never returned by any response (REQ-SEC-053, REQ-SEC-054)';
COMMENT ON COLUMN SEC_SVC_ACCOUNT_CRED.description IS 'DBF-SEC-108 — where the secret is deployed, entered at issuance';
COMMENT ON COLUMN SEC_SVC_ACCOUNT_CRED.last_used_at IS 'DBF-SEC-109 — last successful machine authentication with this credential (ADR-SEC-033); informational';
COMMENT ON COLUMN SEC_SVC_ACCOUNT_CRED.revoked_at IS 'DBF-SEC-110 — NULL = active; set once on revocation, terminal (RULE-SEC-011)';
COMMENT ON COLUMN SEC_SVC_ACCOUNT_CRED.revoked_by IS 'DBF-SEC-111 — principal of the revoking administrator; set with revoked_at';
COMMENT ON COLUMN SEC_SVC_ACCOUNT_CRED.user_id IS 'DBF-SEC-112 — the service account (SEC_USER, principal type SERVICE, ACTIVE at issuance — RULE-SEC-008)';
COMMENT ON COLUMN SEC_SVC_ACCOUNT_CRED.created_by IS 'DBF-SEC-113 — the issuing administrator';
COMMENT ON COLUMN SEC_SVC_ACCOUNT_CRED.created_at IS 'DBF-SEC-114 — the issuance time';
COMMENT ON COLUMN SEC_SVC_ACCOUNT_CRED.updated_by IS 'DBF-SEC-115';
COMMENT ON COLUMN SEC_SVC_ACCOUNT_CRED.updated_at IS 'DBF-SEC-116';

-- BLOCK 5 — CONSTRAINTS

-- 5a PK
ALTER TABLE SEC_SVC_ACCOUNT_CRED  ADD CONSTRAINT PK_SEC_SVC_ACCOUNT_CRED  PRIMARY KEY (service_account_credential_pk);

-- 5b UNIQUE
-- The client identifier is SEC_USER.username, already unique (UQ_SEC_USER_USERNAME, v1).
-- G1 fix: a guarded partial unique index closes the sign-up race API-SEC-002 flagged
-- (registry-db-sec.md previously claimed this index existed under an ADR that was never
-- written; that claim is now made real, under ADR-SEC-041). The guard aborts rather than
-- half-failing the migration if duplicate PENDING rows already exist.
DO $$
DECLARE dup_count INT;
BEGIN
  SELECT COUNT(*) INTO dup_count FROM (
    SELECT email FROM SEC_SIGNUP_REQUEST WHERE status_code = 'PENDING'
    GROUP BY email HAVING COUNT(*) > 1
  ) d;
  IF dup_count > 0 THEN
    RAISE EXCEPTION 'CS-SEC-001 migration aborted: % email(s) have more than one PENDING SEC_SIGNUP_REQUEST row; resolve duplicates before creating UQ_SEC_SIGNUP_REQUEST_PENDING_EMAIL', dup_count;
  END IF;
END $$;

CREATE UNIQUE INDEX UQ_SEC_SIGNUP_REQUEST_PENDING_EMAIL ON SEC_SIGNUP_REQUEST (email) WHERE status_code = 'PENDING';

-- 5c CHECK (closed A6 / A7 value sets per ADR-SEC-001, as in v1)
ALTER TABLE SEC_USER       ADD CONSTRAINT CHK_SEC_USER_PRINCIPAL_TYPE    CHECK (principal_type_code IN ('HUMAN','SERVICE'));
ALTER TABLE SEC_USER       ADD CONSTRAINT CHK_SEC_USER_PRINCIPAL_STATUS  CHECK (principal_type_code <> 'SERVICE' OR status_code IN ('ACTIVE','DISABLED'));
ALTER TABLE SEC_AUDIT_LOG  DROP CONSTRAINT CHK_SEC_AUDIT_LOG_EVENT_TYPE;
ALTER TABLE SEC_AUDIT_LOG  ADD CONSTRAINT CHK_SEC_AUDIT_LOG_EVENT_TYPE CHECK (event_type_code IN (
  'LOGIN_SUCCESS','LOGIN_FAILED','LOGOUT','PASSWORD_RESET_REQUESTED','PASSWORD_RESET_COMPLETED',
  'ROLE_ASSIGNED','ROLE_REVOKED','MODULE_GRANTED','MODULE_REVOKED','SCREEN_GRANTED','SCREEN_REVOKED',
  'ACTION_GRANTED','ACTION_REVOKED','SESSION_TERMINATED',
  'SERVICE_ACCOUNT_CREATED','SERVICE_ACCOUNT_DEACTIVATED','SERVICE_ACCOUNT_REACTIVATED','SERVICE_CREDENTIAL_ISSUED',
  'SERVICE_CREDENTIAL_REVOKED','SERVICE_AUTH_SUCCESS','SERVICE_AUTH_FAILED'));
  -- revise pass G6 (ADR-SEC-047): +SERVICE_ACCOUNT_REACTIVATED, 20 → 21 codes
ALTER TABLE SEC_SVC_ACCOUNT_CRED ADD CONSTRAINT CHK_SEC_SVC_ACCOUNT_CRED_REVOKED_BY CHECK ((revoked_at IS NULL) = (revoked_by IS NULL));

-- 5d intra-module FK (parent PK first)
ALTER TABLE SEC_SVC_ACCOUNT_CRED  ADD CONSTRAINT FK_SVC_ACCOUNT_CRED_USER FOREIGN KEY (user_id) REFERENCES SEC_USER (user_pk);

-- BLOCK 6 — TRIGGERS
-- none: RULE-SEC-008 (issue only for an ACTIVE SERVICE user), RULE-SEC-009 (live-state
-- authentication), RULE-SEC-010 (no interactive use), RULE-SEC-011 (no second revocation)
-- and RULE-SEC-012 (principal type fixed) read other rows or compare old and new values.
-- As in v1, the application layer (P3.1) enforces them, not a trigger. No PK trigger exists.

-- BLOCK 7 — INDEXES (non-PK; every new FK column + every new SRS search/list filter column)
CREATE INDEX IDX_SEC_USER_PRINCIPAL_TYPE      ON SEC_USER (principal_type_code);
CREATE INDEX IDX_SEC_SVC_ACCOUNT_CRED_USER    ON SEC_SVC_ACCOUNT_CRED (user_id);
-- UQ_SEC_SIGNUP_REQUEST_PENDING_EMAIL is created in BLOCK 5 (5b UNIQUE), ahead of its
-- guard's precondition check, not repeated here.

-- BLOCK 8 — LOOKUP SEED DATA
-- none: ADR-SEC-001 still holds (SRS A6: PRINCIPAL_TYPE storage "not reopened here").
-- PRINCIPAL_TYPE (HUMAN, SERVICE) and the six new AUDIT_EVENT_TYPE codes live in the
-- CHECK constraints of 5c. There is no lookup-value table to seed, and this migration has no DML.

-- BLOCK 9 — VIEWS
-- none required by this SRS delta.

-- BLOCK 10 — FUNCTIONS / PROCEDURES
-- none required by this SRS delta.

COMMIT;

-- BLOCK 11 — DEFERRED FK PATCH BLOCKS
-- none: SEC is ROOT, no XM row exists in this version (see §2 XM REGISTER).
```

## 4. DECISIONS APPLIED

| DEFAULT / ADR | What | Source | Override / status |
|---|---|---|---|
| ADR-SEC-035 | PK generation for all 14 tables is a named `SEQ_{TABLE}` sequence. v1's 13 identity columns are migrated with `DROP IDENTITY` and a sequence realigned to `MAX(pk) + 1`. The new table is created on its sequence. | profile.stack.db.pk_generation = `sequence` ("a second strategy cannot be introduced beside them"); engine §4 / §12 | ACCEPTED (non-breaking). Override: if the target database already carries these `SEQ_SEC_*` objects, skip the 13 v1 `CREATE SEQUENCE` lines. |
| ADR-SEC-001 | SEC-owned lookups stay CHECK-constrained VARCHAR code columns. PRINCIPAL_TYPE gets its own CHECK. AUDIT_EVENT_TYPE's CHECK is dropped and re-created with 21 codes (revise pass G6: +SERVICE_ACCOUNT_REACTIVATED). No seed rows. | SRS A6 (PRINCIPAL_TYPE storage "not reopened here", project-registry DECISION INDEX #7) | ACCEPTED (non-breaking, continued from v1) |
| ADR-SEC-047 (applied from P1) | `CHK_SEC_AUDIT_LOG_EVENT_TYPE` carries `SERVICE_ACCOUNT_REACTIVATED` alongside the six v2 SERVICE_* codes, closing the reactivation-audit gap G6 found in the plan | REQ-SEC-079; ADR-SEC-047 | ACCEPTED (non-breaking) |
| DEFAULT | `principal_type_code` defaults to `'HUMAN'`. The default backfills every v1 row, all of them human users. It also keeps any insert that omits the column on the non-privileged principal type: a SERVICE principal is only ever created explicitly (REQ-SEC-036, REQ-SEC-038). | this stage; SRS A3 ENT-SEC-001 | non-breaking; drop the default after backfill if the client wants the column always stated |
| DEFAULT | `CHK_SEC_USER_PRINCIPAL_STATUS`: a SERVICE principal is never PENDING | SRS A7 ("a service account starts at ACTIVE … and never enters PENDING") | non-breaking |
| DEFAULT | `CHK_SEC_SVC_ACCOUNT_CRED_REVOKED_BY`: `revoked_at` and `revoked_by` are both set or both empty | SRS A3 ENT-SEC-014 (revokedBy "set with revokedAt") | non-breaking |
| ADR-SEC-041 | `UQ_SEC_SIGNUP_REQUEST_PENDING_EMAIL`, a partial unique index on `SEC_SIGNUP_REQUEST(email) WHERE status_code='PENDING'`, guarded by a pre-check that aborts the migration on existing duplicates. API-SEC-002's Concurrency line now names this index as the guarantee; the refused insert answers the shared `DATA_INTEGRITY_VIOLATION` (409), as API-SEC-006 already does. | G1 (pass-1 review) | ACCEPTED (non-breaking, ADDITIVE) |
| ADR-SEC-035 (Consequences, amended) | `LOCK TABLE ... IN ACCESS EXCLUSIVE MODE` on all 13 v1 tables runs before every `setval`/`DROP IDENTITY`, so the sequence realignment reads a state no v1 backend can still write past. The v1 backend must be stopped before this migration runs. | G3 (pass-1 review) | ACCEPTED (non-breaking) |
| DEFAULT | `revoked_by` is a principal string `VARCHAR(100)`, not a numeric FK, although SRS A3 types it as a reference to ENT-SEC-001 | v1 precedent (`terminated_by`, `reviewed_by`, `assigned_by`, `granted_by`); engine §3 "user columns hold a principal string" | non-breaking |
| DEFAULT | `description VARCHAR(500)`; `secret_hash TEXT` | SRS gives no length. 500 follows v1's description columns. TEXT follows v1's algorithm-agnostic hash DEFAULT. | non-breaking |
| DEFAULT | RULE-SEC-008 … RULE-SEC-012 are enforced at the application layer (P3.1), not by triggers | v1 DEFAULT (structure, not business logic); engine §7.1 BLOCK 6 | non-breaking |
| DEFAULT | Matrix FK types restated as `BIGINT` (v1 matrix said `NUMERIC`; v1 SQL declares `BIGINT`) | v1 script | documentation correction only, no schema change |
| DEFAULT | The migration runs in one transaction (`BEGIN` … `COMMIT`) | PostgreSQL transactional DDL | non-breaking |

No BLOCKED ADR — the pass was not stopped.

## 5. REGISTRY CONTENT
See `registry-db-sec.md`.

## 6. DBF id definitions — v2 ADDED and MODIFIED only (full detail in §1; `[traces]` = ENT + REQ; every other v1 DBF is carried unchanged)
**DBF-SEC-001** — SEC_USER.user_pk — v2 MODIFIED: BIGINT NOT NULL populated from SEQ_SEC_USER (identity dropped) [ENT-SEC-001, REQ-SEC-009]
**DBF-SEC-002** — SEC_USER.username — v2 MODIFIED: also the service account's machine client identifier [ENT-SEC-001, REQ-SEC-001, REQ-SEC-009, REQ-SEC-036, REQ-SEC-046, REQ-SEC-047, REQ-SEC-063]
**DBF-SEC-003** — SEC_USER.email — v2 MODIFIED: a service account's contact address, never a reset target; exposed by the cross-module contact read [ENT-SEC-001, REQ-SEC-006, REQ-SEC-009, REQ-SEC-034, REQ-SEC-036, REQ-SEC-064]
**DBF-SEC-004** — SEC_USER.password_hash — v2 MODIFIED: a service account's unusable random value [ENT-SEC-001, REQ-SEC-001, REQ-SEC-007, REQ-SEC-036, REQ-SEC-063]
**DBF-SEC-007** — SEC_USER.status_code — v2 MODIFIED: a service account is ACTIVE or DISABLED only (CHK_SEC_USER_PRINCIPAL_STATUS); the active state the cross-module contact read returns [ENT-SEC-001, REQ-SEC-004, REQ-SEC-009, REQ-SEC-011, REQ-SEC-031, REQ-SEC-034, REQ-SEC-036, REQ-SEC-052, REQ-SEC-060, REQ-SEC-062]
**DBF-SEC-014** — SEC_ROLE.role_pk — v2 MODIFIED: BIGINT NOT NULL populated from SEQ_SEC_ROLE (identity dropped) [ENT-SEC-002, REQ-SEC-012]
**DBF-SEC-025** — SEC_USER_ROLE.user_role_pk — v2 MODIFIED: BIGINT NOT NULL populated from SEQ_SEC_USER_ROLE (identity dropped) [ENT-SEC-003, REQ-SEC-010]
**DBF-SEC-030** — SEC_MODULE_REG.module_reg_pk — v2 MODIFIED: BIGINT NOT NULL populated from SEQ_SEC_MODULE_REG (identity dropped) [ENT-SEC-004, REQ-SEC-016]
**DBF-SEC-039** — SEC_SCREEN_REG.screen_reg_pk — v2 MODIFIED: BIGINT NOT NULL populated from SEQ_SEC_SCREEN_REG (identity dropped) [ENT-SEC-005, REQ-SEC-017]
**DBF-SEC-049** — SEC_ACTION_REG.action_reg_pk — v2 MODIFIED: BIGINT NOT NULL populated from SEQ_SEC_ACTION_REG (identity dropped) [ENT-SEC-006, REQ-SEC-019]
**DBF-SEC-060** — SEC_ROLE_MODULE_GRANT.role_module_grant_pk — v2 MODIFIED: BIGINT NOT NULL populated from SEQ_SEC_ROLE_MODULE_GRANT (identity dropped) [ENT-SEC-007, REQ-SEC-012]
**DBF-SEC-065** — SEC_ROLE_SCREEN_GRANT.role_screen_grant_pk — v2 MODIFIED: BIGINT NOT NULL populated from SEQ_SEC_ROLE_SCREEN_GRANT (identity dropped) [ENT-SEC-008, REQ-SEC-013]
**DBF-SEC-026** — SEC_USER_ROLE.user_id — v2 MODIFIED: trace only, no change — the user end of the path the cross-module permission-holder read walks [ENT-SEC-003, REQ-SEC-010, REQ-SEC-035]
**DBF-SEC-027** — SEC_USER_ROLE.role_id — v2 MODIFIED: trace only, no change — the role end of that same path [ENT-SEC-003, REQ-SEC-010, REQ-SEC-035]
**DBF-SEC-072** — SEC_ROLE_ACTION_GRANT.action_id — v2 MODIFIED: trace only, no change — the action the permission code resolves to [ENT-SEC-009, REQ-SEC-014, REQ-SEC-035]
**DBF-SEC-070** — SEC_ROLE_ACTION_GRANT.role_action_grant_pk — v2 MODIFIED: BIGINT NOT NULL populated from SEQ_SEC_ROLE_ACTION_GRANT (identity dropped) [ENT-SEC-009, REQ-SEC-014]
**DBF-SEC-075** — SEC_ACTIVE_SESSION.active_session_pk — v2 MODIFIED: BIGINT NOT NULL populated from SEQ_SEC_ACTIVE_SESSION (identity dropped) [ENT-SEC-010, REQ-SEC-001]
**DBF-SEC-083** — SEC_AUDIT_LOG.audit_log_pk — v2 MODIFIED: BIGINT NOT NULL populated from SEQ_SEC_AUDIT_LOG (identity dropped) [ENT-SEC-011, REQ-SEC-024]
**DBF-SEC-084** — SEC_AUDIT_LOG.event_type_code — v2 MODIFIED: value set 14 → 21, the seven SERVICE_* codes added (revise pass G6: +SERVICE_ACCOUNT_REACTIVATED) [ENT-SEC-011, REQ-SEC-024, REQ-SEC-065, REQ-SEC-066, REQ-SEC-067, REQ-SEC-068, REQ-SEC-069, REQ-SEC-070, REQ-SEC-071, REQ-SEC-079]
**DBF-SEC-091** — SEC_PWD_RESET_TOKEN.pwd_reset_token_pk — v2 MODIFIED: BIGINT NOT NULL populated from SEQ_SEC_PWD_RESET_TOKEN (identity dropped) [ENT-SEC-012, REQ-SEC-006]
**DBF-SEC-097** — SEC_SIGNUP_REQUEST.signup_request_pk — v2 MODIFIED: BIGINT NOT NULL populated from SEQ_SEC_SIGNUP_REQUEST (identity dropped) [ENT-SEC-013, REQ-SEC-003]
**DBF-SEC-105** — SEC_USER.principal_type_code — v2 ADDED [ENT-SEC-001, REQ-SEC-036, REQ-SEC-038, REQ-SEC-039, REQ-SEC-040, REQ-SEC-041, REQ-SEC-042]
**DBF-SEC-106** — SEC_SVC_ACCOUNT_CRED.service_account_credential_pk — v2 ADDED [ENT-SEC-014, REQ-SEC-051]
**DBF-SEC-107** — SEC_SVC_ACCOUNT_CRED.secret_hash — v2 ADDED [ENT-SEC-014, REQ-SEC-046, REQ-SEC-047, REQ-SEC-053, REQ-SEC-054]
**DBF-SEC-108** — SEC_SVC_ACCOUNT_CRED.description — v2 ADDED [ENT-SEC-014, REQ-SEC-051, REQ-SEC-056]
**DBF-SEC-109** — SEC_SVC_ACCOUNT_CRED.last_used_at — v2 ADDED [ENT-SEC-014, REQ-SEC-056, REQ-SEC-057]
**DBF-SEC-110** — SEC_SVC_ACCOUNT_CRED.revoked_at — v2 ADDED [ENT-SEC-014, REQ-SEC-055, REQ-SEC-056, REQ-SEC-058, REQ-SEC-059, REQ-SEC-061]
**DBF-SEC-111** — SEC_SVC_ACCOUNT_CRED.revoked_by — v2 ADDED [ENT-SEC-014, REQ-SEC-058, REQ-SEC-061]
**DBF-SEC-112** — SEC_SVC_ACCOUNT_CRED.user_id — v2 ADDED [ENT-SEC-014, ENT-SEC-001, REQ-SEC-046, REQ-SEC-051, REQ-SEC-052, REQ-SEC-056, REQ-SEC-060]
**DBF-SEC-113** — SEC_SVC_ACCOUNT_CRED.created_by — v2 ADDED [ENT-SEC-014, REQ-SEC-051]
**DBF-SEC-114** — SEC_SVC_ACCOUNT_CRED.created_at — v2 ADDED [ENT-SEC-014, REQ-SEC-051, REQ-SEC-056]
**DBF-SEC-115** — SEC_SVC_ACCOUNT_CRED.updated_by — v2 ADDED [ENT-SEC-014, REQ-SEC-057, REQ-SEC-058]
**DBF-SEC-116** — SEC_SVC_ACCOUNT_CRED.updated_at — v2 ADDED [ENT-SEC-014, REQ-SEC-057, REQ-SEC-058]
══════════════════════════════════════════════════════════════════
