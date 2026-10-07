# DATABASE — الأمان / Security (SEC)
══════════════════════════════════════════════════════════════════
Module : SEC   Version : v1   Dialect : postgresql16   Schema prefix : none
Identifier transformation : SRS logical field name (camelCase) → physical column
  name (snake_case) — e.g. `userPk` → `user_pk`, `nameAr` → `name_ar`. Applied to
  every identifier in this script; no other spelling of any field exists.
Date : 2026-09-10
Counts : 13 tables · 104 DBF · 0 XM (SEC is ROOT) · 2 shared infrastructure tables (see ADR-SEC-001)
══════════════════════════════════════════════════════════════════

## 1. DB FIELD TRACEABILITY MATRIX — SEC v1

### Table SEC_USER (ENT-SEC-001)
| DBF id | Column | Type (postgresql16) | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-001 | user_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-001 (PK, profile.stack.db.naming.pk_pattern) | REQ-SEC-009 | NOT NULL | identity |
| DBF-SEC-002 | username | VARCHAR(100) | ENT-SEC-001.username | REQ-SEC-001, REQ-SEC-009 | NOT NULL | — |
| DBF-SEC-003 | email | VARCHAR(255) | ENT-SEC-001.email | REQ-SEC-006, REQ-SEC-009 | NOT NULL | — |
| DBF-SEC-004 | password_hash | TEXT | ENT-SEC-001.passwordHash | REQ-SEC-001, REQ-SEC-007 | NOT NULL | — |
| DBF-SEC-005 | full_name_ar | VARCHAR(200) | ENT-SEC-001.fullNameAr | REQ-SEC-009 | NOT NULL | — |
| DBF-SEC-006 | full_name_en | VARCHAR(200) | ENT-SEC-001.fullNameEn | REQ-SEC-009 | NOT NULL | — |
| DBF-SEC-007 | status_code | VARCHAR(20) | ENT-SEC-001.statusCode (A6 lookup USER_STATUS) | REQ-SEC-004, REQ-SEC-009, REQ-SEC-011, REQ-SEC-031 | NOT NULL | 'ACTIVE' |
| DBF-SEC-008 | last_login_at | TIMESTAMPTZ | ENT-SEC-001.lastLoginAt | REQ-SEC-001 | NULL | — |
| DBF-SEC-009 | is_active_fl | BOOLEAN | ENT-SEC-001.isActiveFl | REQ-SEC-011, REQ-SEC-031 | NOT NULL | TRUE |
| DBF-SEC-010 | created_by | VARCHAR(100) | profile: entity_defaults.security (audit) + ENT-SEC-001 | REQ-SEC-009 | NOT NULL | — |
| DBF-SEC-011 | created_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) + ENT-SEC-001 | REQ-SEC-009 | NOT NULL | now() |
| DBF-SEC-012 | updated_by | VARCHAR(100) | profile: entity_defaults.security (audit) + ENT-SEC-001 | REQ-SEC-009 | NULL | — |
| DBF-SEC-013 | updated_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) + ENT-SEC-001 | REQ-SEC-009 | NULL | — |

### Table SEC_ROLE (ENT-SEC-002)
| DBF id | Column | Type | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-014 | role_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-002 (PK) | REQ-SEC-012 | NOT NULL | identity |
| DBF-SEC-015 | code | VARCHAR(50) | ENT-SEC-002.code | REQ-SEC-012 | NOT NULL | — |
| DBF-SEC-016 | name_ar | VARCHAR(150) | ENT-SEC-002.nameAr | REQ-SEC-012 | NOT NULL | — |
| DBF-SEC-017 | name_en | VARCHAR(150) | ENT-SEC-002.nameEn | REQ-SEC-012 | NOT NULL | — |
| DBF-SEC-018 | description_ar | VARCHAR(500) | ENT-SEC-002.descriptionAr | REQ-SEC-012 | NULL | — |
| DBF-SEC-019 | description_en | VARCHAR(500) | ENT-SEC-002.descriptionEn | REQ-SEC-012 | NULL | — |
| DBF-SEC-020 | is_active_fl | BOOLEAN | ENT-SEC-002.isActiveFl | REQ-SEC-012 | NOT NULL | TRUE |
| DBF-SEC-021 | created_by | VARCHAR(100) | profile: entity_defaults.security (audit) | REQ-SEC-012 | NOT NULL | — |
| DBF-SEC-022 | created_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) | REQ-SEC-012 | NOT NULL | now() |
| DBF-SEC-023 | updated_by | VARCHAR(100) | profile: entity_defaults.security (audit) | REQ-SEC-012 | NULL | — |
| DBF-SEC-024 | updated_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) | REQ-SEC-012 | NULL | — |

### Table SEC_USER_ROLE (ENT-SEC-003 UserRoleAssignment)
| DBF id | Column | Type | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-025 | user_role_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-003 (PK) | REQ-SEC-010 | NOT NULL | identity |
| DBF-SEC-026 | user_id | NUMERIC | ENT-SEC-003.userId → FK ENT-SEC-001 | REQ-SEC-010 | NOT NULL | — |
| DBF-SEC-027 | role_id | NUMERIC | ENT-SEC-003.roleId → FK ENT-SEC-002 | REQ-SEC-010 | NOT NULL | — |
| DBF-SEC-028 | assigned_by | VARCHAR(100) | ENT-SEC-003.assignedBy | REQ-SEC-010 | NOT NULL | — |
| DBF-SEC-029 | assigned_at | TIMESTAMPTZ | ENT-SEC-003.assignedAt | REQ-SEC-010 | NOT NULL | now() |

### Table SEC_MODULE_REG (ENT-SEC-004 ModuleRegistry)
| DBF id | Column | Type | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-030 | module_reg_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-004 (PK) | REQ-SEC-016 | NOT NULL | identity |
| DBF-SEC-031 | code | VARCHAR(10) | ENT-SEC-004.code | REQ-SEC-016 | NOT NULL | — |
| DBF-SEC-032 | name_ar | VARCHAR(150) | ENT-SEC-004.nameAr | REQ-SEC-016 | NOT NULL | — |
| DBF-SEC-033 | name_en | VARCHAR(150) | ENT-SEC-004.nameEn | REQ-SEC-016 | NOT NULL | — |
| DBF-SEC-034 | is_active_fl | BOOLEAN | ENT-SEC-004.isActiveFl | REQ-SEC-016 | NOT NULL | TRUE |
| DBF-SEC-035 | created_by | VARCHAR(100) | profile: entity_defaults.security (audit) | REQ-SEC-016 | NOT NULL | — |
| DBF-SEC-036 | created_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) | REQ-SEC-016 | NOT NULL | now() |
| DBF-SEC-037 | updated_by | VARCHAR(100) | profile: entity_defaults.security (audit) | REQ-SEC-016 | NULL | — |
| DBF-SEC-038 | updated_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) | REQ-SEC-016 | NULL | — |

### Table SEC_SCREEN_REG (ENT-SEC-005 ScreenRegistry)
| DBF id | Column | Type | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-039 | screen_reg_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-005 (PK) | REQ-SEC-017 | NOT NULL | identity |
| DBF-SEC-040 | page_code | VARCHAR(50) | ENT-SEC-005.pageCode | REQ-SEC-017 | NOT NULL | — |
| DBF-SEC-041 | module_id | NUMERIC | ENT-SEC-005.moduleId → FK ENT-SEC-004 [RULE-SEC-004] | REQ-SEC-017, REQ-SEC-018 | NOT NULL | — |
| DBF-SEC-042 | name_ar | VARCHAR(150) | ENT-SEC-005.nameAr | REQ-SEC-017 | NOT NULL | — |
| DBF-SEC-043 | name_en | VARCHAR(150) | ENT-SEC-005.nameEn | REQ-SEC-017 | NOT NULL | — |
| DBF-SEC-044 | is_active_fl | BOOLEAN | ENT-SEC-005.isActiveFl | REQ-SEC-017 | NOT NULL | TRUE |
| DBF-SEC-045 | created_by | VARCHAR(100) | profile: entity_defaults.security (audit) | REQ-SEC-017 | NOT NULL | — |
| DBF-SEC-046 | created_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) | REQ-SEC-017 | NOT NULL | now() |
| DBF-SEC-047 | updated_by | VARCHAR(100) | profile: entity_defaults.security (audit) | REQ-SEC-017 | NULL | — |
| DBF-SEC-048 | updated_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) | REQ-SEC-017 | NULL | — |

### Table SEC_ACTION_REG (ENT-SEC-006 ActionRegistry)
| DBF id | Column | Type | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-049 | action_reg_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-006 (PK) | REQ-SEC-019 | NOT NULL | identity |
| DBF-SEC-050 | permission_code | VARCHAR(100) | ENT-SEC-006.permissionCode | REQ-SEC-019 | NOT NULL | — |
| DBF-SEC-051 | screen_id | NUMERIC | ENT-SEC-006.screenId → FK ENT-SEC-005 | REQ-SEC-019 | NOT NULL | — |
| DBF-SEC-052 | action_code | VARCHAR(40) | ENT-SEC-006.actionCode | REQ-SEC-019 | NOT NULL | — |
| DBF-SEC-053 | name_ar | VARCHAR(150) | ENT-SEC-006.nameAr | REQ-SEC-019 | NOT NULL | — |
| DBF-SEC-054 | name_en | VARCHAR(150) | ENT-SEC-006.nameEn | REQ-SEC-019 | NOT NULL | — |
| DBF-SEC-055 | is_active_fl | BOOLEAN | ENT-SEC-006.isActiveFl | REQ-SEC-019 | NOT NULL | TRUE |
| DBF-SEC-056 | created_by | VARCHAR(100) | profile: entity_defaults.security (audit) | REQ-SEC-019 | NOT NULL | — |
| DBF-SEC-057 | created_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) | REQ-SEC-019 | NOT NULL | now() |
| DBF-SEC-058 | updated_by | VARCHAR(100) | profile: entity_defaults.security (audit) | REQ-SEC-019 | NULL | — |
| DBF-SEC-059 | updated_at | TIMESTAMPTZ | profile: entity_defaults.security (audit) | REQ-SEC-019 | NULL | — |

### Table SEC_ROLE_MODULE_GRANT (ENT-SEC-007)
| DBF id | Column | Type | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-060 | role_module_grant_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-007 (PK) | REQ-SEC-012 | NOT NULL | identity |
| DBF-SEC-061 | role_id | NUMERIC | ENT-SEC-007.roleId → FK ENT-SEC-002 | REQ-SEC-012, REQ-SEC-015 | NOT NULL | — |
| DBF-SEC-062 | module_id | NUMERIC | ENT-SEC-007.moduleId → FK ENT-SEC-004 | REQ-SEC-012, REQ-SEC-015 | NOT NULL | — |
| DBF-SEC-063 | granted_by | VARCHAR(100) | ENT-SEC-007.grantedBy | REQ-SEC-012 | NOT NULL | — |
| DBF-SEC-064 | granted_at | TIMESTAMPTZ | ENT-SEC-007.grantedAt | REQ-SEC-012 | NOT NULL | now() |

### Table SEC_ROLE_SCREEN_GRANT (ENT-SEC-008)
| DBF id | Column | Type | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-065 | role_screen_grant_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-008 (PK) | REQ-SEC-013 | NOT NULL | identity |
| DBF-SEC-066 | role_id | NUMERIC | ENT-SEC-008.roleId → FK ENT-SEC-002 [RULE-SEC-001, app-layer] | REQ-SEC-013, REQ-SEC-015 | NOT NULL | — |
| DBF-SEC-067 | screen_id | NUMERIC | ENT-SEC-008.screenId → FK ENT-SEC-005 | REQ-SEC-013 | NOT NULL | — |
| DBF-SEC-068 | granted_by | VARCHAR(100) | ENT-SEC-008.grantedBy | REQ-SEC-013 | NOT NULL | — |
| DBF-SEC-069 | granted_at | TIMESTAMPTZ | ENT-SEC-008.grantedAt | REQ-SEC-013 | NOT NULL | now() |

### Table SEC_ROLE_ACTION_GRANT (ENT-SEC-009)
| DBF id | Column | Type | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-070 | role_action_grant_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-009 (PK) | REQ-SEC-014 | NOT NULL | identity |
| DBF-SEC-071 | role_id | NUMERIC | ENT-SEC-009.roleId → FK ENT-SEC-002 [RULE-SEC-002, RULE-SEC-005, RULE-SEC-007 — app-layer] | REQ-SEC-014, REQ-SEC-020, REQ-SEC-030 | NOT NULL | — |
| DBF-SEC-072 | action_id | NUMERIC | ENT-SEC-009.actionId → FK ENT-SEC-006 | REQ-SEC-014 | NOT NULL | — |
| DBF-SEC-073 | granted_by | VARCHAR(100) | ENT-SEC-009.grantedBy | REQ-SEC-014 | NOT NULL | — |
| DBF-SEC-074 | granted_at | TIMESTAMPTZ | ENT-SEC-009.grantedAt | REQ-SEC-014 | NOT NULL | now() |

### Table SEC_ACTIVE_SESSION (ENT-SEC-010)
| DBF id | Column | Type | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-075 | active_session_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-010 (PK) | REQ-SEC-001 | NOT NULL | identity |
| DBF-SEC-076 | user_id | NUMERIC | ENT-SEC-010.userId → FK ENT-SEC-001 | REQ-SEC-001, REQ-SEC-011, REQ-SEC-027 | NOT NULL | — |
| DBF-SEC-077 | token_ref | VARCHAR(200) | ENT-SEC-010.tokenRef | REQ-SEC-001 | NOT NULL | — |
| DBF-SEC-078 | started_at | TIMESTAMPTZ | ENT-SEC-010.startedAt | REQ-SEC-001 | NOT NULL | now() |
| DBF-SEC-079 | last_activity_at | TIMESTAMPTZ | ENT-SEC-010.lastActivityAt | REQ-SEC-027 | NOT NULL | now() |
| DBF-SEC-080 | ip_address | VARCHAR(64) | ENT-SEC-010.ipAddress | REQ-SEC-027 | NULL | — |
| DBF-SEC-081 | terminated_at | TIMESTAMPTZ | ENT-SEC-010.terminatedAt | REQ-SEC-011, REQ-SEC-028 | NULL | — |
| DBF-SEC-082 | terminated_by | VARCHAR(100) | ENT-SEC-010.terminatedBy | REQ-SEC-011, REQ-SEC-028 | NULL | — |

### Table SEC_AUDIT_LOG (ENT-SEC-011 AuditLogEntry)
| DBF id | Column | Type | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-083 | audit_log_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-011 (PK) | REQ-SEC-024 | NOT NULL | identity |
| DBF-SEC-084 | event_type_code | VARCHAR(40) | ENT-SEC-011.eventTypeCode (A6 lookup AUDIT_EVENT_TYPE) | REQ-SEC-024 | NOT NULL | — |
| DBF-SEC-085 | actor_user_id | NUMERIC | ENT-SEC-011.actorUserId → FK ENT-SEC-001 (nullable) | REQ-SEC-002, REQ-SEC-024 | NULL | — |
| DBF-SEC-086 | occurred_at | TIMESTAMPTZ | ENT-SEC-011.occurredAt | REQ-SEC-024 | NOT NULL | now() |
| DBF-SEC-087 | target_ref | VARCHAR(200) | ENT-SEC-011.targetRef | REQ-SEC-024 | NULL | — |
| DBF-SEC-088 | details_ar | TEXT | ENT-SEC-011.detailsAr | REQ-SEC-024 | NULL | — |
| DBF-SEC-089 | details_en | TEXT | ENT-SEC-011.detailsEn | REQ-SEC-024 | NULL | — |
| DBF-SEC-090 | ip_address | VARCHAR(64) | ENT-SEC-011.ipAddress | REQ-SEC-024 | NULL | — |

### Table SEC_PWD_RESET_TOKEN (ENT-SEC-012 PasswordResetToken)
| DBF id | Column | Type | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-091 | pwd_reset_token_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-012 (PK) | REQ-SEC-006 | NOT NULL | identity |
| DBF-SEC-092 | user_id | NUMERIC | ENT-SEC-012.userId → FK ENT-SEC-001 | REQ-SEC-006 | NOT NULL | — |
| DBF-SEC-093 | token_hash | TEXT | ENT-SEC-012.tokenHash | REQ-SEC-006, REQ-SEC-007 | NOT NULL | — |
| DBF-SEC-094 | requested_at | TIMESTAMPTZ | ENT-SEC-012.requestedAt | REQ-SEC-006 | NOT NULL | now() |
| DBF-SEC-095 | expires_at | TIMESTAMPTZ | ENT-SEC-012.expiresAt (A7 DEFAULT: requestedAt + 30 min) | REQ-SEC-006, REQ-SEC-008 | NOT NULL | — |
| DBF-SEC-096 | used_at | TIMESTAMPTZ | ENT-SEC-012.usedAt [RULE-SEC-006, app-layer] | REQ-SEC-007, REQ-SEC-008 | NULL | — |

### Table SEC_SIGNUP_REQUEST (ENT-SEC-013)
| DBF id | Column | Type | Traces (ENT.field) | Traces (REQ) | Nullable | Default |
|---|---|---|---|---|---|---|
| DBF-SEC-097 | signup_request_pk | GENERATED ALWAYS AS IDENTITY | ENT-SEC-013 (PK) | REQ-SEC-003 | NOT NULL | identity |
| DBF-SEC-098 | email | VARCHAR(255) | ENT-SEC-013.email | REQ-SEC-003 | NOT NULL | — |
| DBF-SEC-099 | full_name_ar | VARCHAR(200) | ENT-SEC-013.fullNameAr | REQ-SEC-003 | NOT NULL | — |
| DBF-SEC-100 | full_name_en | VARCHAR(200) | ENT-SEC-013.fullNameEn | REQ-SEC-003 | NOT NULL | — |
| DBF-SEC-101 | submitted_at | TIMESTAMPTZ | ENT-SEC-013.submittedAt | REQ-SEC-003 | NOT NULL | now() |
| DBF-SEC-102 | status_code | VARCHAR(20) | ENT-SEC-013.statusCode (A6 lookup SIGNUP_STATUS) | REQ-SEC-003, REQ-SEC-004, REQ-SEC-005 | NOT NULL | 'PENDING' |
| DBF-SEC-103 | reviewed_by | VARCHAR(100) | ENT-SEC-013.reviewedBy | REQ-SEC-004, REQ-SEC-005 | NULL | — |
| DBF-SEC-104 | reviewed_at | TIMESTAMPTZ | ENT-SEC-013.reviewedAt | REQ-SEC-004, REQ-SEC-005 | NULL | — |

Total: 104 DBF ids across 13 tables (plus 2 shared-infrastructure tables under ADR-SEC-001, not SEC ENT-traced — see §2 and §6).

## 2. XM REGISTER — SEC v1

None **in the CONSUME direction** — SEC is ROOT; SRS A8's first table lists no consumed entity
owned by another module (the Notifications integration is an external service, not a registered
platform module, and carries no FK — see SRS A8 second table).

AMENDMENT 2026-09-11 — the EXPOSED direction is not covered by that statement and does not change
it. SEC exposes one read-only inbound surface, `com.erp.sec.crossmodule` (REQ-SEC-034,
REQ-SEC-035; SRS A8's third table). It registers no entity, table or column and carries no FK, so
it produces no XM row here; SEC assigns no XM id of its own. Formal `XM-*` ids for that direction
are assigned by the *consuming* module's own P2, not by SEC — the rule already stated for
`XM-INBOUND-STUB-1` in P3_1/backend-execution-plan-sec.md PHASE 6 (INT-R).

## 3. FULL_DATABASE_SCRIPT

```sql
-- ════════════════════════════════════════════════════════════════
-- SEC v1 — Security module — PostgreSQL 16
-- Identifier transformation: camelCase (SRS) -> snake_case (DB)
-- ════════════════════════════════════════════════════════════════

-- BLOCK 1 — SEQUENCES
-- none: every PK uses GENERATED ALWAYS AS IDENTITY (postgresql16 syntax_map)

-- BLOCK 2 — PARENT TABLES (no FK dependencies)

-- Shared platform lookup infrastructure (see ADR-SEC-001): NOT created here in v1.
-- SEC's own lookup-backed columns (status_code, event_type_code) are CHECK-constrained
-- in this version; centralization into a shared MDL_LOOKUP_TYPE/MDL_LOOKUP_VALUE pair
-- is deferred to when the MDL module's own P2 stage runs (ADR-SEC-001, non-breaking).

CREATE TABLE SEC_USER (
  user_pk        BIGINT GENERATED ALWAYS AS IDENTITY,
  username       VARCHAR(100)  NOT NULL,
  email          VARCHAR(255)  NOT NULL,
  password_hash  TEXT          NOT NULL,
  full_name_ar   VARCHAR(200)  NOT NULL,
  full_name_en   VARCHAR(200)  NOT NULL,
  status_code    VARCHAR(20)   NOT NULL DEFAULT 'ACTIVE',
  last_login_at  TIMESTAMPTZ,
  is_active_fl   BOOLEAN       NOT NULL DEFAULT TRUE,
  created_by     VARCHAR(100)  NOT NULL,
  created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_by     VARCHAR(100),
  updated_at     TIMESTAMPTZ
);

CREATE TABLE SEC_ROLE (
  role_pk          BIGINT GENERATED ALWAYS AS IDENTITY,
  code             VARCHAR(50)   NOT NULL,
  name_ar          VARCHAR(150)  NOT NULL,
  name_en          VARCHAR(150)  NOT NULL,
  description_ar   VARCHAR(500),
  description_en   VARCHAR(500),
  is_active_fl     BOOLEAN       NOT NULL DEFAULT TRUE,
  created_by       VARCHAR(100)  NOT NULL,
  created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_by       VARCHAR(100),
  updated_at       TIMESTAMPTZ
);

CREATE TABLE SEC_MODULE_REG (
  module_reg_pk  BIGINT GENERATED ALWAYS AS IDENTITY,
  code           VARCHAR(10)   NOT NULL,
  name_ar        VARCHAR(150)  NOT NULL,
  name_en        VARCHAR(150)  NOT NULL,
  is_active_fl   BOOLEAN       NOT NULL DEFAULT TRUE,
  created_by     VARCHAR(100)  NOT NULL,
  created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_by     VARCHAR(100),
  updated_at     TIMESTAMPTZ
);

CREATE TABLE SEC_SIGNUP_REQUEST (
  signup_request_pk  BIGINT GENERATED ALWAYS AS IDENTITY,
  email              VARCHAR(255)  NOT NULL,
  full_name_ar       VARCHAR(200)  NOT NULL,
  full_name_en       VARCHAR(200)  NOT NULL,
  submitted_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  status_code        VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
  reviewed_by        VARCHAR(100),
  reviewed_at        TIMESTAMPTZ
);

-- BLOCK 3 — CHILD TABLES (parents already created above; chain respected)

CREATE TABLE SEC_USER_ROLE (
  user_role_pk  BIGINT GENERATED ALWAYS AS IDENTITY,
  user_id       BIGINT        NOT NULL,
  role_id       BIGINT        NOT NULL,
  assigned_by   VARCHAR(100)  NOT NULL,
  assigned_at   TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE SEC_SCREEN_REG (
  screen_reg_pk  BIGINT GENERATED ALWAYS AS IDENTITY,
  page_code      VARCHAR(50)   NOT NULL,
  module_id      BIGINT        NOT NULL,
  name_ar        VARCHAR(150)  NOT NULL,
  name_en        VARCHAR(150)  NOT NULL,
  is_active_fl   BOOLEAN       NOT NULL DEFAULT TRUE,
  created_by     VARCHAR(100)  NOT NULL,
  created_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_by     VARCHAR(100),
  updated_at     TIMESTAMPTZ
);

CREATE TABLE SEC_ACTION_REG (
  action_reg_pk    BIGINT GENERATED ALWAYS AS IDENTITY,
  permission_code  VARCHAR(100)  NOT NULL,
  screen_id        BIGINT        NOT NULL,
  action_code      VARCHAR(40)   NOT NULL,
  name_ar          VARCHAR(150)  NOT NULL,
  name_en          VARCHAR(150)  NOT NULL,
  is_active_fl     BOOLEAN       NOT NULL DEFAULT TRUE,
  created_by       VARCHAR(100)  NOT NULL,
  created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  updated_by       VARCHAR(100),
  updated_at       TIMESTAMPTZ
);

CREATE TABLE SEC_ROLE_MODULE_GRANT (
  role_module_grant_pk  BIGINT GENERATED ALWAYS AS IDENTITY,
  role_id               BIGINT        NOT NULL,
  module_id             BIGINT        NOT NULL,
  granted_by            VARCHAR(100)  NOT NULL,
  granted_at            TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE SEC_ROLE_SCREEN_GRANT (
  role_screen_grant_pk  BIGINT GENERATED ALWAYS AS IDENTITY,
  role_id               BIGINT        NOT NULL,
  screen_id             BIGINT        NOT NULL,
  granted_by            VARCHAR(100)  NOT NULL,
  granted_at            TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE SEC_ROLE_ACTION_GRANT (
  role_action_grant_pk  BIGINT GENERATED ALWAYS AS IDENTITY,
  role_id               BIGINT        NOT NULL,
  action_id             BIGINT        NOT NULL,
  granted_by            VARCHAR(100)  NOT NULL,
  granted_at            TIMESTAMPTZ   NOT NULL DEFAULT now()
);

CREATE TABLE SEC_ACTIVE_SESSION (
  active_session_pk  BIGINT GENERATED ALWAYS AS IDENTITY,
  user_id            BIGINT        NOT NULL,
  token_ref          VARCHAR(200)  NOT NULL,
  started_at         TIMESTAMPTZ   NOT NULL DEFAULT now(),
  last_activity_at   TIMESTAMPTZ   NOT NULL DEFAULT now(),
  ip_address         VARCHAR(64),
  terminated_at      TIMESTAMPTZ,
  terminated_by      VARCHAR(100)
);

CREATE TABLE SEC_AUDIT_LOG (
  audit_log_pk      BIGINT GENERATED ALWAYS AS IDENTITY,
  event_type_code   VARCHAR(40)   NOT NULL,
  actor_user_id     BIGINT,
  occurred_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
  target_ref        VARCHAR(200),
  details_ar        TEXT,
  details_en        TEXT,
  ip_address        VARCHAR(64)
);

CREATE TABLE SEC_PWD_RESET_TOKEN (
  pwd_reset_token_pk  BIGINT GENERATED ALWAYS AS IDENTITY,
  user_id             BIGINT       NOT NULL,
  token_hash          TEXT         NOT NULL,
  requested_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
  expires_at          TIMESTAMPTZ  NOT NULL,
  used_at             TIMESTAMPTZ
);

-- BLOCK 4 — COMMENTS (table + every column; each column comment cites its DBF id)

COMMENT ON TABLE SEC_USER IS 'ENT-SEC-001 User — SHARED(owner); [DBF-SEC-001..013]';
COMMENT ON COLUMN SEC_USER.user_pk IS 'DBF-SEC-001';
COMMENT ON COLUMN SEC_USER.username IS 'DBF-SEC-002';
COMMENT ON COLUMN SEC_USER.email IS 'DBF-SEC-003';
COMMENT ON COLUMN SEC_USER.password_hash IS 'DBF-SEC-004 — never returned to any client (POL-SEC-004)';
COMMENT ON COLUMN SEC_USER.full_name_ar IS 'DBF-SEC-005';
COMMENT ON COLUMN SEC_USER.full_name_en IS 'DBF-SEC-006';
COMMENT ON COLUMN SEC_USER.status_code IS 'DBF-SEC-007 — lookup USER_STATUS (ADR-SEC-001)';
COMMENT ON COLUMN SEC_USER.last_login_at IS 'DBF-SEC-008';
COMMENT ON COLUMN SEC_USER.is_active_fl IS 'DBF-SEC-009';
COMMENT ON COLUMN SEC_USER.created_by IS 'DBF-SEC-010';
COMMENT ON COLUMN SEC_USER.created_at IS 'DBF-SEC-011';
COMMENT ON COLUMN SEC_USER.updated_by IS 'DBF-SEC-012';
COMMENT ON COLUMN SEC_USER.updated_at IS 'DBF-SEC-013';

COMMENT ON TABLE SEC_ROLE IS 'ENT-SEC-002 Role — PRIVATE; [DBF-SEC-014..024]';
COMMENT ON COLUMN SEC_ROLE.role_pk IS 'DBF-SEC-014';
COMMENT ON COLUMN SEC_ROLE.code IS 'DBF-SEC-015';
COMMENT ON COLUMN SEC_ROLE.name_ar IS 'DBF-SEC-016';
COMMENT ON COLUMN SEC_ROLE.name_en IS 'DBF-SEC-017';
COMMENT ON COLUMN SEC_ROLE.description_ar IS 'DBF-SEC-018';
COMMENT ON COLUMN SEC_ROLE.description_en IS 'DBF-SEC-019';
COMMENT ON COLUMN SEC_ROLE.is_active_fl IS 'DBF-SEC-020';
COMMENT ON COLUMN SEC_ROLE.created_by IS 'DBF-SEC-021';
COMMENT ON COLUMN SEC_ROLE.created_at IS 'DBF-SEC-022';
COMMENT ON COLUMN SEC_ROLE.updated_by IS 'DBF-SEC-023';
COMMENT ON COLUMN SEC_ROLE.updated_at IS 'DBF-SEC-024';

COMMENT ON TABLE SEC_USER_ROLE IS 'ENT-SEC-003 UserRoleAssignment — PRIVATE; [DBF-SEC-025..029]';
COMMENT ON COLUMN SEC_USER_ROLE.user_role_pk IS 'DBF-SEC-025';
COMMENT ON COLUMN SEC_USER_ROLE.user_id IS 'DBF-SEC-026';
COMMENT ON COLUMN SEC_USER_ROLE.role_id IS 'DBF-SEC-027';
COMMENT ON COLUMN SEC_USER_ROLE.assigned_by IS 'DBF-SEC-028';
COMMENT ON COLUMN SEC_USER_ROLE.assigned_at IS 'DBF-SEC-029';

COMMENT ON TABLE SEC_MODULE_REG IS 'ENT-SEC-004 ModuleRegistry — SHARED(owner); [DBF-SEC-030..038]';
COMMENT ON COLUMN SEC_MODULE_REG.module_reg_pk IS 'DBF-SEC-030';
COMMENT ON COLUMN SEC_MODULE_REG.code IS 'DBF-SEC-031';
COMMENT ON COLUMN SEC_MODULE_REG.name_ar IS 'DBF-SEC-032';
COMMENT ON COLUMN SEC_MODULE_REG.name_en IS 'DBF-SEC-033';
COMMENT ON COLUMN SEC_MODULE_REG.is_active_fl IS 'DBF-SEC-034';
COMMENT ON COLUMN SEC_MODULE_REG.created_by IS 'DBF-SEC-035';
COMMENT ON COLUMN SEC_MODULE_REG.created_at IS 'DBF-SEC-036';
COMMENT ON COLUMN SEC_MODULE_REG.updated_by IS 'DBF-SEC-037';
COMMENT ON COLUMN SEC_MODULE_REG.updated_at IS 'DBF-SEC-038';

COMMENT ON TABLE SEC_SCREEN_REG IS 'ENT-SEC-005 ScreenRegistry (SEC_PAGES) — SHARED(owner); [DBF-SEC-039..048]';
COMMENT ON COLUMN SEC_SCREEN_REG.screen_reg_pk IS 'DBF-SEC-039';
COMMENT ON COLUMN SEC_SCREEN_REG.page_code IS 'DBF-SEC-040';
COMMENT ON COLUMN SEC_SCREEN_REG.module_id IS 'DBF-SEC-041 — RULE-SEC-004 enforced by FK_SCREEN_REG_MODULE';
COMMENT ON COLUMN SEC_SCREEN_REG.name_ar IS 'DBF-SEC-042';
COMMENT ON COLUMN SEC_SCREEN_REG.name_en IS 'DBF-SEC-043';
COMMENT ON COLUMN SEC_SCREEN_REG.is_active_fl IS 'DBF-SEC-044';
COMMENT ON COLUMN SEC_SCREEN_REG.created_by IS 'DBF-SEC-045';
COMMENT ON COLUMN SEC_SCREEN_REG.created_at IS 'DBF-SEC-046';
COMMENT ON COLUMN SEC_SCREEN_REG.updated_by IS 'DBF-SEC-047';
COMMENT ON COLUMN SEC_SCREEN_REG.updated_at IS 'DBF-SEC-048';

COMMENT ON TABLE SEC_ACTION_REG IS 'ENT-SEC-006 ActionRegistry — SHARED(owner); [DBF-SEC-049..059]';
COMMENT ON COLUMN SEC_ACTION_REG.action_reg_pk IS 'DBF-SEC-049';
COMMENT ON COLUMN SEC_ACTION_REG.permission_code IS 'DBF-SEC-050';
COMMENT ON COLUMN SEC_ACTION_REG.screen_id IS 'DBF-SEC-051';
COMMENT ON COLUMN SEC_ACTION_REG.action_code IS 'DBF-SEC-052';
COMMENT ON COLUMN SEC_ACTION_REG.name_ar IS 'DBF-SEC-053';
COMMENT ON COLUMN SEC_ACTION_REG.name_en IS 'DBF-SEC-054';
COMMENT ON COLUMN SEC_ACTION_REG.is_active_fl IS 'DBF-SEC-055';
COMMENT ON COLUMN SEC_ACTION_REG.created_by IS 'DBF-SEC-056';
COMMENT ON COLUMN SEC_ACTION_REG.created_at IS 'DBF-SEC-057';
COMMENT ON COLUMN SEC_ACTION_REG.updated_by IS 'DBF-SEC-058';
COMMENT ON COLUMN SEC_ACTION_REG.updated_at IS 'DBF-SEC-059';

COMMENT ON TABLE SEC_ROLE_MODULE_GRANT IS 'ENT-SEC-007 RoleModuleGrant — PRIVATE; [DBF-SEC-060..064]';
COMMENT ON COLUMN SEC_ROLE_MODULE_GRANT.role_module_grant_pk IS 'DBF-SEC-060';
COMMENT ON COLUMN SEC_ROLE_MODULE_GRANT.role_id IS 'DBF-SEC-061';
COMMENT ON COLUMN SEC_ROLE_MODULE_GRANT.module_id IS 'DBF-SEC-062';
COMMENT ON COLUMN SEC_ROLE_MODULE_GRANT.granted_by IS 'DBF-SEC-063';
COMMENT ON COLUMN SEC_ROLE_MODULE_GRANT.granted_at IS 'DBF-SEC-064';

COMMENT ON TABLE SEC_ROLE_SCREEN_GRANT IS 'ENT-SEC-008 RoleScreenGrant — PRIVATE; RULE-SEC-001 enforced at application layer (P3.1); [DBF-SEC-065..069]';
COMMENT ON COLUMN SEC_ROLE_SCREEN_GRANT.role_screen_grant_pk IS 'DBF-SEC-065';
COMMENT ON COLUMN SEC_ROLE_SCREEN_GRANT.role_id IS 'DBF-SEC-066';
COMMENT ON COLUMN SEC_ROLE_SCREEN_GRANT.screen_id IS 'DBF-SEC-067';
COMMENT ON COLUMN SEC_ROLE_SCREEN_GRANT.granted_by IS 'DBF-SEC-068';
COMMENT ON COLUMN SEC_ROLE_SCREEN_GRANT.granted_at IS 'DBF-SEC-069';

COMMENT ON TABLE SEC_ROLE_ACTION_GRANT IS 'ENT-SEC-009 RoleActionGrant — PRIVATE; RULE-SEC-002/005/007 enforced at application layer (P3.1); [DBF-SEC-070..074]';
COMMENT ON COLUMN SEC_ROLE_ACTION_GRANT.role_action_grant_pk IS 'DBF-SEC-070';
COMMENT ON COLUMN SEC_ROLE_ACTION_GRANT.role_id IS 'DBF-SEC-071';
COMMENT ON COLUMN SEC_ROLE_ACTION_GRANT.action_id IS 'DBF-SEC-072';
COMMENT ON COLUMN SEC_ROLE_ACTION_GRANT.granted_by IS 'DBF-SEC-073';
COMMENT ON COLUMN SEC_ROLE_ACTION_GRANT.granted_at IS 'DBF-SEC-074';

COMMENT ON TABLE SEC_ACTIVE_SESSION IS 'ENT-SEC-010 ActiveSession — PRIVATE; [DBF-SEC-075..082]';
COMMENT ON COLUMN SEC_ACTIVE_SESSION.active_session_pk IS 'DBF-SEC-075';
COMMENT ON COLUMN SEC_ACTIVE_SESSION.user_id IS 'DBF-SEC-076';
COMMENT ON COLUMN SEC_ACTIVE_SESSION.token_ref IS 'DBF-SEC-077';
COMMENT ON COLUMN SEC_ACTIVE_SESSION.started_at IS 'DBF-SEC-078';
COMMENT ON COLUMN SEC_ACTIVE_SESSION.last_activity_at IS 'DBF-SEC-079';
COMMENT ON COLUMN SEC_ACTIVE_SESSION.ip_address IS 'DBF-SEC-080';
COMMENT ON COLUMN SEC_ACTIVE_SESSION.terminated_at IS 'DBF-SEC-081';
COMMENT ON COLUMN SEC_ACTIVE_SESSION.terminated_by IS 'DBF-SEC-082';

COMMENT ON TABLE SEC_AUDIT_LOG IS 'ENT-SEC-011 AuditLogEntry — PRIVATE, append-only, immutable (POL-SEC-009); [DBF-SEC-083..090]';
COMMENT ON COLUMN SEC_AUDIT_LOG.audit_log_pk IS 'DBF-SEC-083';
COMMENT ON COLUMN SEC_AUDIT_LOG.event_type_code IS 'DBF-SEC-084 — lookup AUDIT_EVENT_TYPE (ADR-SEC-001)';
COMMENT ON COLUMN SEC_AUDIT_LOG.actor_user_id IS 'DBF-SEC-085';
COMMENT ON COLUMN SEC_AUDIT_LOG.occurred_at IS 'DBF-SEC-086';
COMMENT ON COLUMN SEC_AUDIT_LOG.target_ref IS 'DBF-SEC-087';
COMMENT ON COLUMN SEC_AUDIT_LOG.details_ar IS 'DBF-SEC-088';
COMMENT ON COLUMN SEC_AUDIT_LOG.details_en IS 'DBF-SEC-089';
COMMENT ON COLUMN SEC_AUDIT_LOG.ip_address IS 'DBF-SEC-090';

COMMENT ON TABLE SEC_PWD_RESET_TOKEN IS 'ENT-SEC-012 PasswordResetToken — PRIVATE; [DBF-SEC-091..096]';
COMMENT ON COLUMN SEC_PWD_RESET_TOKEN.pwd_reset_token_pk IS 'DBF-SEC-091';
COMMENT ON COLUMN SEC_PWD_RESET_TOKEN.user_id IS 'DBF-SEC-092';
COMMENT ON COLUMN SEC_PWD_RESET_TOKEN.token_hash IS 'DBF-SEC-093';
COMMENT ON COLUMN SEC_PWD_RESET_TOKEN.requested_at IS 'DBF-SEC-094';
COMMENT ON COLUMN SEC_PWD_RESET_TOKEN.expires_at IS 'DBF-SEC-095';
COMMENT ON COLUMN SEC_PWD_RESET_TOKEN.used_at IS 'DBF-SEC-096 — RULE-SEC-006 enforced at application layer (P3.1)';

COMMENT ON TABLE SEC_SIGNUP_REQUEST IS 'ENT-SEC-013 SignupRequest — PRIVATE; [DBF-SEC-097..104]';
COMMENT ON COLUMN SEC_SIGNUP_REQUEST.signup_request_pk IS 'DBF-SEC-097';
COMMENT ON COLUMN SEC_SIGNUP_REQUEST.email IS 'DBF-SEC-098';
COMMENT ON COLUMN SEC_SIGNUP_REQUEST.full_name_ar IS 'DBF-SEC-099';
COMMENT ON COLUMN SEC_SIGNUP_REQUEST.full_name_en IS 'DBF-SEC-100';
COMMENT ON COLUMN SEC_SIGNUP_REQUEST.submitted_at IS 'DBF-SEC-101';
COMMENT ON COLUMN SEC_SIGNUP_REQUEST.status_code IS 'DBF-SEC-102 — lookup SIGNUP_STATUS (ADR-SEC-001)';
COMMENT ON COLUMN SEC_SIGNUP_REQUEST.reviewed_by IS 'DBF-SEC-103';
COMMENT ON COLUMN SEC_SIGNUP_REQUEST.reviewed_at IS 'DBF-SEC-104';

-- BLOCK 5 — CONSTRAINTS

-- 5a PK
ALTER TABLE SEC_USER               ADD CONSTRAINT PK_SEC_USER               PRIMARY KEY (user_pk);
ALTER TABLE SEC_ROLE               ADD CONSTRAINT PK_SEC_ROLE               PRIMARY KEY (role_pk);
ALTER TABLE SEC_USER_ROLE          ADD CONSTRAINT PK_SEC_USER_ROLE          PRIMARY KEY (user_role_pk);
ALTER TABLE SEC_MODULE_REG         ADD CONSTRAINT PK_SEC_MODULE_REG         PRIMARY KEY (module_reg_pk);
ALTER TABLE SEC_SCREEN_REG         ADD CONSTRAINT PK_SEC_SCREEN_REG         PRIMARY KEY (screen_reg_pk);
ALTER TABLE SEC_ACTION_REG         ADD CONSTRAINT PK_SEC_ACTION_REG         PRIMARY KEY (action_reg_pk);
ALTER TABLE SEC_ROLE_MODULE_GRANT  ADD CONSTRAINT PK_SEC_ROLE_MODULE_GRANT  PRIMARY KEY (role_module_grant_pk);
ALTER TABLE SEC_ROLE_SCREEN_GRANT  ADD CONSTRAINT PK_SEC_ROLE_SCREEN_GRANT  PRIMARY KEY (role_screen_grant_pk);
ALTER TABLE SEC_ROLE_ACTION_GRANT  ADD CONSTRAINT PK_SEC_ROLE_ACTION_GRANT  PRIMARY KEY (role_action_grant_pk);
ALTER TABLE SEC_ACTIVE_SESSION     ADD CONSTRAINT PK_SEC_ACTIVE_SESSION     PRIMARY KEY (active_session_pk);
ALTER TABLE SEC_AUDIT_LOG          ADD CONSTRAINT PK_SEC_AUDIT_LOG          PRIMARY KEY (audit_log_pk);
ALTER TABLE SEC_PWD_RESET_TOKEN    ADD CONSTRAINT PK_SEC_PWD_RESET_TOKEN    PRIMARY KEY (pwd_reset_token_pk);
ALTER TABLE SEC_SIGNUP_REQUEST     ADD CONSTRAINT PK_SEC_SIGNUP_REQUEST     PRIMARY KEY (signup_request_pk);

-- 5b UNIQUE
ALTER TABLE SEC_USER               ADD CONSTRAINT UQ_SEC_USER_USERNAME     UNIQUE (username);
ALTER TABLE SEC_USER               ADD CONSTRAINT UQ_SEC_USER_EMAIL        UNIQUE (email);
ALTER TABLE SEC_ROLE               ADD CONSTRAINT UQ_SEC_ROLE_CODE         UNIQUE (code);
ALTER TABLE SEC_USER_ROLE          ADD CONSTRAINT UQ_SEC_USER_ROLE_USER_ROLE UNIQUE (user_id, role_id);
ALTER TABLE SEC_MODULE_REG         ADD CONSTRAINT UQ_SEC_MODULE_REG_CODE   UNIQUE (code);
ALTER TABLE SEC_SCREEN_REG         ADD CONSTRAINT UQ_SEC_SCREEN_REG_PAGE   UNIQUE (page_code);
ALTER TABLE SEC_ACTION_REG         ADD CONSTRAINT UQ_SEC_ACTION_REG_PERM   UNIQUE (permission_code);
ALTER TABLE SEC_ROLE_MODULE_GRANT  ADD CONSTRAINT UQ_SEC_ROLE_MODULE_GRANT_ROLE_MODULE UNIQUE (role_id, module_id);
ALTER TABLE SEC_ROLE_SCREEN_GRANT  ADD CONSTRAINT UQ_SEC_ROLE_SCREEN_GRANT_ROLE_SCREEN UNIQUE (role_id, screen_id);
ALTER TABLE SEC_ROLE_ACTION_GRANT  ADD CONSTRAINT UQ_SEC_ROLE_ACTION_GRANT_ROLE_ACTION UNIQUE (role_id, action_id);

-- 5c CHECK (RULE-SEC-004 handled as an FK below; these two are the closed A6 lookup
-- value sets per ADR-SEC-001 — CHECK today, migrated to an FK on the shared lookup
-- infrastructure once MDL's own P2 stage creates it)
ALTER TABLE SEC_USER            ADD CONSTRAINT CHK_SEC_USER_STATUS          CHECK (status_code IN ('PENDING','ACTIVE','DISABLED'));
ALTER TABLE SEC_SIGNUP_REQUEST  ADD CONSTRAINT CHK_SEC_SIGNUP_REQUEST_STATUS CHECK (status_code IN ('PENDING','APPROVED','REJECTED'));
ALTER TABLE SEC_AUDIT_LOG       ADD CONSTRAINT CHK_SEC_AUDIT_LOG_EVENT_TYPE CHECK (event_type_code IN (
  'LOGIN_SUCCESS','LOGIN_FAILED','LOGOUT','PASSWORD_RESET_REQUESTED','PASSWORD_RESET_COMPLETED',
  'ROLE_ASSIGNED','ROLE_REVOKED','MODULE_GRANTED','MODULE_REVOKED','SCREEN_GRANTED','SCREEN_REVOKED',
  'ACTION_GRANTED','ACTION_REVOKED','SESSION_TERMINATED'));

-- 5d intra-module FK (parent PK first)
ALTER TABLE SEC_USER_ROLE         ADD CONSTRAINT FK_USER_ROLE_USER    FOREIGN KEY (user_id)   REFERENCES SEC_USER (user_pk);
ALTER TABLE SEC_USER_ROLE         ADD CONSTRAINT FK_USER_ROLE_ROLE    FOREIGN KEY (role_id)   REFERENCES SEC_ROLE (role_pk);
ALTER TABLE SEC_SCREEN_REG        ADD CONSTRAINT FK_SCREEN_REG_MODULE FOREIGN KEY (module_id) REFERENCES SEC_MODULE_REG (module_reg_pk);   -- RULE-SEC-004
ALTER TABLE SEC_ACTION_REG        ADD CONSTRAINT FK_ACTION_REG_SCREEN FOREIGN KEY (screen_id) REFERENCES SEC_SCREEN_REG (screen_reg_pk);
ALTER TABLE SEC_ROLE_MODULE_GRANT ADD CONSTRAINT FK_ROLE_MODULE_GRANT_ROLE   FOREIGN KEY (role_id)   REFERENCES SEC_ROLE (role_pk);
ALTER TABLE SEC_ROLE_MODULE_GRANT ADD CONSTRAINT FK_ROLE_MODULE_GRANT_MODULE FOREIGN KEY (module_id) REFERENCES SEC_MODULE_REG (module_reg_pk);
ALTER TABLE SEC_ROLE_SCREEN_GRANT ADD CONSTRAINT FK_ROLE_SCREEN_GRANT_ROLE   FOREIGN KEY (role_id)   REFERENCES SEC_ROLE (role_pk);
ALTER TABLE SEC_ROLE_SCREEN_GRANT ADD CONSTRAINT FK_ROLE_SCREEN_GRANT_SCREEN FOREIGN KEY (screen_id) REFERENCES SEC_SCREEN_REG (screen_reg_pk);
ALTER TABLE SEC_ROLE_ACTION_GRANT ADD CONSTRAINT FK_ROLE_ACTION_GRANT_ROLE   FOREIGN KEY (role_id)   REFERENCES SEC_ROLE (role_pk);
ALTER TABLE SEC_ROLE_ACTION_GRANT ADD CONSTRAINT FK_ROLE_ACTION_GRANT_ACTION FOREIGN KEY (action_id) REFERENCES SEC_ACTION_REG (action_reg_pk);
ALTER TABLE SEC_ACTIVE_SESSION    ADD CONSTRAINT FK_ACTIVE_SESSION_USER      FOREIGN KEY (user_id)   REFERENCES SEC_USER (user_pk);
ALTER TABLE SEC_AUDIT_LOG         ADD CONSTRAINT FK_AUDIT_LOG_USER           FOREIGN KEY (actor_user_id) REFERENCES SEC_USER (user_pk);
ALTER TABLE SEC_PWD_RESET_TOKEN   ADD CONSTRAINT FK_PWD_RESET_TOKEN_USER     FOREIGN KEY (user_id)   REFERENCES SEC_USER (user_pk);

-- BLOCK 6 — TRIGGERS
-- none: no SRS RULE requires a DB-level trigger. RULE-SEC-001/002/003/005/006/007 are
-- multi-row / time-based business rules enforced at the application layer (P3.1) per
-- this stage's boundary (P2 owns structure, not business logic); RULE-SEC-004 is
-- already enforced structurally by FK_SCREEN_REG_MODULE above — no trigger needed.

-- BLOCK 7 — INDEXES (non-PK; every FK column + every SRS search/list filter column)
CREATE INDEX IDX_SEC_USER_STATUS               ON SEC_USER (status_code);
CREATE INDEX IDX_SEC_USER_ROLE_USER             ON SEC_USER_ROLE (user_id);
CREATE INDEX IDX_SEC_USER_ROLE_ROLE             ON SEC_USER_ROLE (role_id);
CREATE INDEX IDX_SEC_SCREEN_REG_MODULE          ON SEC_SCREEN_REG (module_id);
CREATE INDEX IDX_SEC_ACTION_REG_SCREEN          ON SEC_ACTION_REG (screen_id);
CREATE INDEX IDX_SEC_ROLE_MODULE_GRANT_ROLE     ON SEC_ROLE_MODULE_GRANT (role_id);
CREATE INDEX IDX_SEC_ROLE_MODULE_GRANT_MODULE   ON SEC_ROLE_MODULE_GRANT (module_id);
CREATE INDEX IDX_SEC_ROLE_SCREEN_GRANT_ROLE     ON SEC_ROLE_SCREEN_GRANT (role_id);
CREATE INDEX IDX_SEC_ROLE_SCREEN_GRANT_SCREEN   ON SEC_ROLE_SCREEN_GRANT (screen_id);
CREATE INDEX IDX_SEC_ROLE_ACTION_GRANT_ROLE     ON SEC_ROLE_ACTION_GRANT (role_id);
CREATE INDEX IDX_SEC_ROLE_ACTION_GRANT_ACTION   ON SEC_ROLE_ACTION_GRANT (action_id);
CREATE INDEX IDX_SEC_ACTIVE_SESSION_USER        ON SEC_ACTIVE_SESSION (user_id);
CREATE INDEX IDX_SEC_ACTIVE_SESSION_TERMINATED  ON SEC_ACTIVE_SESSION (terminated_at);
CREATE INDEX IDX_SEC_AUDIT_LOG_EVENT_TYPE       ON SEC_AUDIT_LOG (event_type_code);
CREATE INDEX IDX_SEC_AUDIT_LOG_ACTOR            ON SEC_AUDIT_LOG (actor_user_id);
CREATE INDEX IDX_SEC_AUDIT_LOG_OCCURRED_AT      ON SEC_AUDIT_LOG (occurred_at);
CREATE INDEX IDX_SEC_PWD_RESET_TOKEN_USER       ON SEC_PWD_RESET_TOKEN (user_id);
CREATE INDEX IDX_SEC_SIGNUP_REQUEST_STATUS      ON SEC_SIGNUP_REQUEST (status_code);
CREATE INDEX IDX_SEC_SIGNUP_REQUEST_EMAIL       ON SEC_SIGNUP_REQUEST (email);

-- BLOCK 8 — LOOKUP SEED DATA
-- No shared lookup table exists yet in this version (ADR-SEC-001); the A6 value sets
-- (USER_STATUS, SIGNUP_STATUS, AUDIT_EVENT_TYPE) live only as the CHECK constraints
-- above in v1 — there is no separate seed-data table to populate.
COMMIT;

-- BLOCK 9 — VIEWS
-- none required by this SRS.

-- BLOCK 10 — FUNCTIONS / PROCEDURES
-- none required by this SRS.

-- BLOCK 11 — DEFERRED FK PATCH BLOCKS
-- none: SEC is ROOT, no XM row exists this version (see §2 XM REGISTER).
```

## 4. DECISIONS APPLIED

| DEFAULT / ADR | What | Source | Override / status |
|---|---|---|---|
| ADR-SEC-001 | SEC's three owned lookup types (USER_STATUS, SIGNUP_STATUS, AUDIT_EVENT_TYPE) are CHECK-constrained VARCHAR columns in v1, not rows in a shared MDL_LOOKUP_TYPE/MDL_LOOKUP_VALUE pair, because MDL has not yet been analyzed (SEC precedes it in this batch's mandated order — GENERATION-INSTRUCTIONS.md §3) | erp/decisions/SEC/ADR-SEC-001.md | ACCEPTED (non-breaking) — superseded by a migration once MDL's own P2 creates the shared tables |
| DEFAULT | password_hash / token_hash use TEXT (algorithm-agnostic width); no column length assumes a specific hash algorithm | domain best practice; no SRS-stated algorithm | non-breaking |
| DEFAULT | RULE-SEC-001/002/003/005/006/007 enforced at the application layer (P3.1), not as DB triggers/CHECKs, per this stage's boundary (structure, not business logic) | this stage §11 boundaries | non-breaking |

No BLOCKED ADR — the pass was not stopped.

## 5. REGISTRY CONTENT
See `registry-db-sec.md`.

## 6. DBF id definitions (cross-reference index — full detail in §1; `[traces]` = ENT + REQ)
**DBF-SEC-001** — SEC_USER.user_pk [ENT-SEC-001, REQ-SEC-009]
**DBF-SEC-002** — SEC_USER.username [ENT-SEC-001, REQ-SEC-001, REQ-SEC-009]
**DBF-SEC-003** — SEC_USER.email [ENT-SEC-001, REQ-SEC-006, REQ-SEC-009]
**DBF-SEC-004** — SEC_USER.password_hash [ENT-SEC-001, REQ-SEC-001, REQ-SEC-007]
**DBF-SEC-005** — SEC_USER.full_name_ar [ENT-SEC-001, REQ-SEC-009]
**DBF-SEC-006** — SEC_USER.full_name_en [ENT-SEC-001, REQ-SEC-009]
**DBF-SEC-007** — SEC_USER.status_code [ENT-SEC-001, REQ-SEC-004, REQ-SEC-009, REQ-SEC-011, REQ-SEC-031]
**DBF-SEC-008** — SEC_USER.last_login_at [ENT-SEC-001, REQ-SEC-001]
**DBF-SEC-009** — SEC_USER.is_active_fl [ENT-SEC-001, REQ-SEC-011, REQ-SEC-031]
**DBF-SEC-010** — SEC_USER.created_by [ENT-SEC-001, REQ-SEC-009]
**DBF-SEC-011** — SEC_USER.created_at [ENT-SEC-001, REQ-SEC-009]
**DBF-SEC-012** — SEC_USER.updated_by [ENT-SEC-001, REQ-SEC-009]
**DBF-SEC-013** — SEC_USER.updated_at [ENT-SEC-001, REQ-SEC-009]
**DBF-SEC-014** — SEC_ROLE.role_pk [ENT-SEC-002, REQ-SEC-012]
**DBF-SEC-015** — SEC_ROLE.code [ENT-SEC-002, REQ-SEC-012]
**DBF-SEC-016** — SEC_ROLE.name_ar [ENT-SEC-002, REQ-SEC-012]
**DBF-SEC-017** — SEC_ROLE.name_en [ENT-SEC-002, REQ-SEC-012]
**DBF-SEC-018** — SEC_ROLE.description_ar [ENT-SEC-002, REQ-SEC-012]
**DBF-SEC-019** — SEC_ROLE.description_en [ENT-SEC-002, REQ-SEC-012]
**DBF-SEC-020** — SEC_ROLE.is_active_fl [ENT-SEC-002, REQ-SEC-012]
**DBF-SEC-021** — SEC_ROLE.created_by [ENT-SEC-002, REQ-SEC-012]
**DBF-SEC-022** — SEC_ROLE.created_at [ENT-SEC-002, REQ-SEC-012]
**DBF-SEC-023** — SEC_ROLE.updated_by [ENT-SEC-002, REQ-SEC-012]
**DBF-SEC-024** — SEC_ROLE.updated_at [ENT-SEC-002, REQ-SEC-012]
**DBF-SEC-025** — SEC_USER_ROLE.user_role_pk [ENT-SEC-003, REQ-SEC-010]
**DBF-SEC-026** — SEC_USER_ROLE.user_id [ENT-SEC-003, ENT-SEC-001, REQ-SEC-010]
**DBF-SEC-027** — SEC_USER_ROLE.role_id [ENT-SEC-003, ENT-SEC-002, REQ-SEC-010]
**DBF-SEC-028** — SEC_USER_ROLE.assigned_by [ENT-SEC-003, REQ-SEC-010]
**DBF-SEC-029** — SEC_USER_ROLE.assigned_at [ENT-SEC-003, REQ-SEC-010]
**DBF-SEC-030** — SEC_MODULE_REG.module_reg_pk [ENT-SEC-004, REQ-SEC-016]
**DBF-SEC-031** — SEC_MODULE_REG.code [ENT-SEC-004, REQ-SEC-016]
**DBF-SEC-032** — SEC_MODULE_REG.name_ar [ENT-SEC-004, REQ-SEC-016]
**DBF-SEC-033** — SEC_MODULE_REG.name_en [ENT-SEC-004, REQ-SEC-016]
**DBF-SEC-034** — SEC_MODULE_REG.is_active_fl [ENT-SEC-004, REQ-SEC-016]
**DBF-SEC-035** — SEC_MODULE_REG.created_by [ENT-SEC-004, REQ-SEC-016]
**DBF-SEC-036** — SEC_MODULE_REG.created_at [ENT-SEC-004, REQ-SEC-016]
**DBF-SEC-037** — SEC_MODULE_REG.updated_by [ENT-SEC-004, REQ-SEC-016]
**DBF-SEC-038** — SEC_MODULE_REG.updated_at [ENT-SEC-004, REQ-SEC-016]
**DBF-SEC-039** — SEC_SCREEN_REG.screen_reg_pk [ENT-SEC-005, REQ-SEC-017]
**DBF-SEC-040** — SEC_SCREEN_REG.page_code [ENT-SEC-005, REQ-SEC-017]
**DBF-SEC-041** — SEC_SCREEN_REG.module_id [ENT-SEC-005, ENT-SEC-004, REQ-SEC-017, REQ-SEC-018]
**DBF-SEC-042** — SEC_SCREEN_REG.name_ar [ENT-SEC-005, REQ-SEC-017]
**DBF-SEC-043** — SEC_SCREEN_REG.name_en [ENT-SEC-005, REQ-SEC-017]
**DBF-SEC-044** — SEC_SCREEN_REG.is_active_fl [ENT-SEC-005, REQ-SEC-017]
**DBF-SEC-045** — SEC_SCREEN_REG.created_by [ENT-SEC-005, REQ-SEC-017]
**DBF-SEC-046** — SEC_SCREEN_REG.created_at [ENT-SEC-005, REQ-SEC-017]
**DBF-SEC-047** — SEC_SCREEN_REG.updated_by [ENT-SEC-005, REQ-SEC-017]
**DBF-SEC-048** — SEC_SCREEN_REG.updated_at [ENT-SEC-005, REQ-SEC-017]
**DBF-SEC-049** — SEC_ACTION_REG.action_reg_pk [ENT-SEC-006, REQ-SEC-019]
**DBF-SEC-050** — SEC_ACTION_REG.permission_code [ENT-SEC-006, REQ-SEC-019]
**DBF-SEC-051** — SEC_ACTION_REG.screen_id [ENT-SEC-006, ENT-SEC-005, REQ-SEC-019]
**DBF-SEC-052** — SEC_ACTION_REG.action_code [ENT-SEC-006, REQ-SEC-019]
**DBF-SEC-053** — SEC_ACTION_REG.name_ar [ENT-SEC-006, REQ-SEC-019]
**DBF-SEC-054** — SEC_ACTION_REG.name_en [ENT-SEC-006, REQ-SEC-019]
**DBF-SEC-055** — SEC_ACTION_REG.is_active_fl [ENT-SEC-006, REQ-SEC-019]
**DBF-SEC-056** — SEC_ACTION_REG.created_by [ENT-SEC-006, REQ-SEC-019]
**DBF-SEC-057** — SEC_ACTION_REG.created_at [ENT-SEC-006, REQ-SEC-019]
**DBF-SEC-058** — SEC_ACTION_REG.updated_by [ENT-SEC-006, REQ-SEC-019]
**DBF-SEC-059** — SEC_ACTION_REG.updated_at [ENT-SEC-006, REQ-SEC-019]
**DBF-SEC-060** — SEC_ROLE_MODULE_GRANT.role_module_grant_pk [ENT-SEC-007, REQ-SEC-012]
**DBF-SEC-061** — SEC_ROLE_MODULE_GRANT.role_id [ENT-SEC-007, ENT-SEC-002, REQ-SEC-012, REQ-SEC-015]
**DBF-SEC-062** — SEC_ROLE_MODULE_GRANT.module_id [ENT-SEC-007, ENT-SEC-004, REQ-SEC-012, REQ-SEC-015]
**DBF-SEC-063** — SEC_ROLE_MODULE_GRANT.granted_by [ENT-SEC-007, REQ-SEC-012]
**DBF-SEC-064** — SEC_ROLE_MODULE_GRANT.granted_at [ENT-SEC-007, REQ-SEC-012]
**DBF-SEC-065** — SEC_ROLE_SCREEN_GRANT.role_screen_grant_pk [ENT-SEC-008, REQ-SEC-013]
**DBF-SEC-066** — SEC_ROLE_SCREEN_GRANT.role_id [ENT-SEC-008, ENT-SEC-002, REQ-SEC-013, REQ-SEC-015]
**DBF-SEC-067** — SEC_ROLE_SCREEN_GRANT.screen_id [ENT-SEC-008, ENT-SEC-005, REQ-SEC-013]
**DBF-SEC-068** — SEC_ROLE_SCREEN_GRANT.granted_by [ENT-SEC-008, REQ-SEC-013]
**DBF-SEC-069** — SEC_ROLE_SCREEN_GRANT.granted_at [ENT-SEC-008, REQ-SEC-013]
**DBF-SEC-070** — SEC_ROLE_ACTION_GRANT.role_action_grant_pk [ENT-SEC-009, REQ-SEC-014]
**DBF-SEC-071** — SEC_ROLE_ACTION_GRANT.role_id [ENT-SEC-009, ENT-SEC-002, REQ-SEC-014, REQ-SEC-020, REQ-SEC-030]
**DBF-SEC-072** — SEC_ROLE_ACTION_GRANT.action_id [ENT-SEC-009, ENT-SEC-006, REQ-SEC-014]
**DBF-SEC-073** — SEC_ROLE_ACTION_GRANT.granted_by [ENT-SEC-009, REQ-SEC-014]
**DBF-SEC-074** — SEC_ROLE_ACTION_GRANT.granted_at [ENT-SEC-009, REQ-SEC-014]
**DBF-SEC-075** — SEC_ACTIVE_SESSION.active_session_pk [ENT-SEC-010, REQ-SEC-001]
**DBF-SEC-076** — SEC_ACTIVE_SESSION.user_id [ENT-SEC-010, ENT-SEC-001, REQ-SEC-001, REQ-SEC-011, REQ-SEC-027]
**DBF-SEC-077** — SEC_ACTIVE_SESSION.token_ref [ENT-SEC-010, REQ-SEC-001]
**DBF-SEC-078** — SEC_ACTIVE_SESSION.started_at [ENT-SEC-010, REQ-SEC-001]
**DBF-SEC-079** — SEC_ACTIVE_SESSION.last_activity_at [ENT-SEC-010, REQ-SEC-027]
**DBF-SEC-080** — SEC_ACTIVE_SESSION.ip_address [ENT-SEC-010, REQ-SEC-027]
**DBF-SEC-081** — SEC_ACTIVE_SESSION.terminated_at [ENT-SEC-010, REQ-SEC-011, REQ-SEC-028]
**DBF-SEC-082** — SEC_ACTIVE_SESSION.terminated_by [ENT-SEC-010, REQ-SEC-011, REQ-SEC-028]
**DBF-SEC-083** — SEC_AUDIT_LOG.audit_log_pk [ENT-SEC-011, REQ-SEC-024]
**DBF-SEC-084** — SEC_AUDIT_LOG.event_type_code [ENT-SEC-011, REQ-SEC-024]
**DBF-SEC-085** — SEC_AUDIT_LOG.actor_user_id [ENT-SEC-011, ENT-SEC-001, REQ-SEC-002, REQ-SEC-024]
**DBF-SEC-086** — SEC_AUDIT_LOG.occurred_at [ENT-SEC-011, REQ-SEC-024]
**DBF-SEC-087** — SEC_AUDIT_LOG.target_ref [ENT-SEC-011, REQ-SEC-024]
**DBF-SEC-088** — SEC_AUDIT_LOG.details_ar [ENT-SEC-011, REQ-SEC-024]
**DBF-SEC-089** — SEC_AUDIT_LOG.details_en [ENT-SEC-011, REQ-SEC-024]
**DBF-SEC-090** — SEC_AUDIT_LOG.ip_address [ENT-SEC-011, REQ-SEC-024]
**DBF-SEC-091** — SEC_PWD_RESET_TOKEN.pwd_reset_token_pk [ENT-SEC-012, REQ-SEC-006]
**DBF-SEC-092** — SEC_PWD_RESET_TOKEN.user_id [ENT-SEC-012, ENT-SEC-001, REQ-SEC-006]
**DBF-SEC-093** — SEC_PWD_RESET_TOKEN.token_hash [ENT-SEC-012, REQ-SEC-006, REQ-SEC-007]
**DBF-SEC-094** — SEC_PWD_RESET_TOKEN.requested_at [ENT-SEC-012, REQ-SEC-006]
**DBF-SEC-095** — SEC_PWD_RESET_TOKEN.expires_at [ENT-SEC-012, REQ-SEC-006, REQ-SEC-008]
**DBF-SEC-096** — SEC_PWD_RESET_TOKEN.used_at [ENT-SEC-012, REQ-SEC-007, REQ-SEC-008]
**DBF-SEC-097** — SEC_SIGNUP_REQUEST.signup_request_pk [ENT-SEC-013, REQ-SEC-003]
**DBF-SEC-098** — SEC_SIGNUP_REQUEST.email [ENT-SEC-013, REQ-SEC-003]
**DBF-SEC-099** — SEC_SIGNUP_REQUEST.full_name_ar [ENT-SEC-013, REQ-SEC-003]
**DBF-SEC-100** — SEC_SIGNUP_REQUEST.full_name_en [ENT-SEC-013, REQ-SEC-003]
**DBF-SEC-101** — SEC_SIGNUP_REQUEST.submitted_at [ENT-SEC-013, REQ-SEC-003]
**DBF-SEC-102** — SEC_SIGNUP_REQUEST.status_code [ENT-SEC-013, REQ-SEC-003, REQ-SEC-004, REQ-SEC-005]
**DBF-SEC-103** — SEC_SIGNUP_REQUEST.reviewed_by [ENT-SEC-013, REQ-SEC-004, REQ-SEC-005]
**DBF-SEC-104** — SEC_SIGNUP_REQUEST.reviewed_at [ENT-SEC-013, REQ-SEC-004, REQ-SEC-005]
══════════════════════════════════════════════════════════════════

## Implementation Addendum — erp-core 1.2.0
Source version : erp-core 1.2.0 (tag v1.2.0, https://github.com/hesham421/newback)
Steps          : 01, 04, 05, 06, 07, 10 (migrations V4, V7, V10, V11, V12)
Statement      : Original analysis above is unchanged; this addendum records the implemented deltas.

Paths cited below are relative to `erp-core/src/main/resources/db/migration/core/` at that tag unless
stated otherwise. No DBF ids are minted here.

### Migration chain
- The historical scripts were squashed in step 04: the 13 `SEC_*` tables are created by
  `V4__sec_schema.sql` (DDL verbatim from the old `V16__sec_schema`); the registry, roles, grants and the
  bootstrap admin are seeded by `V7__sec_seed.sql` (from the old `V17`, `V19`, `V20`, `V21`, `V31`, `V10`
  seeds). Full old → new mapping: `docs/steps/04-report.md` → "Old → new mapping".
- The 18 `fin` migrations were deleted in step 01 (renumbering table in `docs/steps/01-report.md`).
- Later SEC changes are additive scripts: `V10__tenant_schema.sql` (step 05), `V11__sec_realms.sql`
  (step 06), `V12__file_storage.sql` (step 07, one SEC_ACTION_REG row). The two plan-sanctioned
  non-additive edits are in V10 / V11: `DROP DEFAULT` after backfill and DROP + re-ADD of unique
  constraints under the same name in composite form.

### Per-table deltas
| Table | Delta | Migration |
|---|---|---|
| SEC_USER | + `TENANT_ID BIGINT NOT NULL` (backfilled 1 = PLATFORM, default dropped, `FK_SEC_USER_TENANT` → `CORE_TENANT(ID)`, `IDX_SEC_USER_TENANT`); + `VERSION BIGINT NOT NULL DEFAULT 0` | V10 |
| SEC_USER | + `REALM VARCHAR(16) NOT NULL` (backfilled `STAFF`, default dropped), `CHK_SEC_USER_REALM` (STAFF, CUSTOMER) | V11 |
| SEC_USER | `UQ_SEC_USER_USERNAME` → `(TENANT_ID, REALM, USERNAME)`; `UQ_SEC_USER_EMAIL` → `(TENANT_ID, REALM, EMAIL)` (V10 made them `(TENANT_ID, …)`, V11 added `REALM`) | V10, V11 |
| SEC_USER | `CHK_SEC_USER_STATUS` widened: PENDING, ACTIVE, DISABLED, + `PENDING_VERIFICATION` | V11 |
| SEC_USER (seed) | bootstrap `admin` / `admin@erp.local`, `STATUS_CODE = 'PENDING'`, `PASSWORD_HASH = 'BOOTSTRAP-PASSWORD-NOT-SET'` (no usable password; no more `admin/admin`) | V7 |
| SEC_ROLE | + `TENANT_ID`, + `VERSION`; `UQ_SEC_ROLE_CODE` → `(TENANT_ID, CODE)` | V10 |
| SEC_ROLE | + `IS_SUPER BOOLEAN NOT NULL DEFAULT FALSE`; every tenant's `SYS_ADMIN` set TRUE | V11 |
| SEC_USER_ROLE | + `TENANT_ID`, + `VERSION`; + nullable `CREATED_BY/AT`, `UPDATED_BY/AT` (backfilled from `ASSIGNED_BY/AT`); `UQ_SEC_USER_ROLE_USER_ROLE` → `(TENANT_ID, USER_ID, ROLE_ID)` | V10 |
| SEC_ROLE_MODULE_GRANT / SEC_ROLE_SCREEN_GRANT / SEC_ROLE_ACTION_GRANT | + `TENANT_ID`, + `VERSION`; + nullable audit columns (backfilled from `GRANTED_BY/AT`); uniques → `(TENANT_ID, ROLE_ID, MODULE_ID / SCREEN_ID / ACTION_ID)` | V10 |
| SEC_ACTIVE_SESSION / SEC_AUDIT_LOG / SEC_PWD_RESET_TOKEN / SEC_SIGNUP_REQUEST | + `TENANT_ID`, + `VERSION`; + nullable audit columns (backfilled `'SYSTEM'` + `STARTED_AT` / `OCCURRED_AT` / `REQUESTED_AT` / `SUBMITTED_AT`) | V10 |
| SEC_MODULE_REG / SEC_SCREEN_REG / SEC_ACTION_REG | + `VERSION` only — global, no `TENANT_ID` (one catalog for all tenants) | V10 |
| SEC_MODULE_REG / SCREEN_REG / ACTION_REG (seed) | + module `PLATFORM`, screen `PLATFORM_TENANTS`, actions `PERM_PLATFORM_TENANTS_VIEW`, `PLATFORM_TENANT_MANAGE` (granted to PLATFORM's `SYS_ADMIN`) | V10 §6 |
| SEC_ACTION_REG (seed) | + `FILE:DOCUMENT:PUBLISH` on screen `FILE_BROWSER` (granted to PLATFORM's `SYS_ADMIN`, `FILE_ADMIN`) | V12 §3 |
| SEC_*_REG (runtime) | since step 06 the catalog is upserted from code at startup by `PermissionCatalogSynchronizer`; later permissions (sequence, settings, audit, reports) have no migration seed. V7 / V10 rows are reproduced identically. | V11 header; DEVIATIONS [06] |
| SEC_CUSTOMER_VERIFY_TOKEN | NEW: `ID BIGINT` (`SEQ_SEC_CUSTOMER_VERIFY_TOKEN`), `TENANT_ID BIGINT NOT NULL`, `USER_ID BIGINT NOT NULL` (FK `SEC_USER(USER_PK)`), `TOKEN_HASH TEXT NOT NULL`, `EXPIRES_AT TIMESTAMPTZ NOT NULL`, `USED_AT TIMESTAMPTZ`, audit columns, `VERSION`; `UQ_SEC_CUSTOMER_VERIFY_TOKEN_HASH (TENANT_ID, TOKEN_HASH)`; FK/IDX to `CORE_TENANT`; `IDX_SEC_CUSTOMER_VERIFY_TOKEN_USER` | V11 §5 |

### Deviations from this analysis
- Analysis: append-only log/session rows carry no "created by" audit fields. Implemented: the 8 tables
  above have nullable audit columns, so every tenant-scoped entity extends `AuditableEntity` (step 04
  "Schema conventions check"; DEVIATIONS [05]).
- Analysis: `USERNAME` / `EMAIL` unique. Implemented: unique per `(TENANT_ID, REALM, …)` (steps 05, 06).
- Kept as recorded (renames are not additive): `*_PK` primary-key names on SEC tables and the SEC vs
  CU/FILE/NOTIF audit-column type differences (DEVIATIONS [05]).
