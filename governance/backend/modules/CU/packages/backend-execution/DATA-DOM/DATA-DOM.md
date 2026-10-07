<!-- Source: PHASE:DATA-DOM -->

## PHASE DATA+DOM — Entity & Domain Specifications
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓
(1 entity < 5 threshold → no SUB split)

### ENTITY-CU-001 — AppConfiguration
────────────────────────────────────────────────────────────────────────
SOURCE BINDINGS (from artifact extraction):
  DB Table       : CU_APP_CONFIGURATION          ← db-script-CU.md §4
  PK Column      : ID                             ← DBF-0001
  PK Sequence    : SEQ_CU_APP_CONFIGURATION       ← db-script-CU.md BLOCK 1 (exact)
  PK Trigger     : (none — PK populated by application framework via sequence)
  DBS-ID ref     : DBS-CU-001

BUSINESS CODE: NONE (BC-RULE-0 — internal configuration entity; no business code column exists in db-script-CU.md)

SOFT DEACTIVATION:
  Governed by CORE Deactivation Policy. DB binding: isActiveFl → IS_ACTIVE_FL — DBF-0005.
  CHECK constraint CHK_CU_APP_CONFIG_ACTIVE_FL (IS_ACTIVE_FL IN (0,1)) — from db-script.

AUDIT COLUMNS (from db-script — AuditEntityListener fills automatically):
  CREATED_BY  String  | CREATED_AT TIMESTAMP | UPDATED_BY String | UPDATED_AT TIMESTAMP
  ⚠ MUST NOT appear in CreateRequest/UpdateRequest; never set by mapper/service.
  ⚠ Java camelCase: createdBy, createdAt, updatedBy, updatedAt.

────────────────────────────────────────────────────────────────────────
FIELDS (DB Field Traceability binding):
────────────────────────────────────────────────────────────────────────
FIELD-ID  │ Java Property      │ DB Column       │ DBF-ID   │ DB Type       │ Null │ Read-Only │ Constraint                       │ Label-AR       │ Label-EN
──────────┼────────────────────┼─────────────────┼──────────┼───────────────┼──────┼───────────┼──────────────────────────────────┼────────────────┼──────────
FIELD-0001│ appConfigurationPk │ ID              │ DBF-0001 │ BIGINT        │ No   │ System    │ PK — SEQ_CU_APP_CONFIGURATION    │ المعرف         │ ID
FIELD-0002│ configKey          │ CONFIG_KEY      │ DBF-0002 │ VARCHAR(150)  │ No   │ After-crt │ UNIQUE — UQ_CU_APP_CONFIG_CONFIG_KEY │ مفتاح الإعداد  │ Config Key
FIELD-0003│ configValue        │ CONFIG_VALUE    │ DBF-0003 │ TEXT          │ No   │ No        │ NOT NULL                         │ قيمة الإعداد   │ Config Value
FIELD-0004│ notes              │ NOTES           │ DBF-0004 │ VARCHAR(2000) │ Yes  │ No        │ —                                │ ملاحظات        │ Notes
FIELD-0005│ isActiveFl         │ IS_ACTIVE_FL    │ DBF-0005 │ SMALLINT      │ No   │ System    │ DEFAULT 1 · CHK IN(0,1)          │ نشط            │ Active
────────────────────────────────────────────────────────────────────────
⚠ Every column name above sourced from db-script-CU.md DBF-ID lookup (NO-COLUMN-INVENTION).

DTO MEMBERSHIP RULES:
  CreateRequest : configKey, configValue, notes  (excludes PK, isActiveFl, audit)
  UpdateRequest : configValue, notes, isActiveFl  (excludes PK, configKey [immutable RULE-CU-003], audit)
  ResponseDTO   : all fields incl. isActiveFl + audit

LOV FIELDS: (none)

────────────────────────────────────────────────────────────────────────
DOMAIN RULES (full text — extracted from srs-CU.md A4):
────────────────────────────────────────────────────────────────────────
RULE-CU-001 — Config key uniqueness:
  Trigger    : On create
  Statement  : The system MUST prevent creating an AppConfiguration when configKey already exists.
  Message-AR : مفتاح الإعداد موجود مسبقاً — اختر مفتاحاً فريداً.
  Message-EN : Configuration key already exists — choose a unique key.
  Scope      : CREATE
  DB Enforce : UNIQUE constraint UQ_CU_APP_CONFIG_CONFIG_KEY (+ app-level pre-check QR-CU-0006)
  ERR-ID     : ERR-0001
  Owned by   : domain layer (Entity method)

RULE-CU-002 — Required fields:
  Trigger    : On save / update
  Statement  : The system MUST require configKey and configValue before saving.
  Message-AR : مفتاح الإعداد وقيمته إلزاميان.
  Message-EN : Config key and value are required.
  Scope      : CREATE, UPDATE
  DB Enforce : NOT NULL on CONFIG_KEY, CONFIG_VALUE + app-level validation
  ERR-ID     : ERR-0002
  Owned by   : domain layer

RULE-CU-003 — Config key immutable:
  Trigger    : On update
  Statement  : The system MUST prevent modifying configKey after creation.
  Message-AR : لا يمكن تعديل مفتاح الإعداد بعد إنشائه.
  Message-EN : Config key cannot be changed after creation.
  Scope      : UPDATE
  DB Enforce : app-level (configKey excluded from UpdateRequest)
  ERR-ID     : ERR-0003
  Owned by   : domain layer

STATE MACHINE: (not applicable — isActiveFl only, no status workflow — srs-CU A6)

CROSS-MODULE DEPENDENCIES: None (ROOT module — srs-CU A7)

REPOSITORY OPERATIONS REQUIRED:
  → QR-CU-0001 : FIND_ONE by configKey
  → QR-CU-0002 : FIND_BY_CRITERIA (search)
  → QR-CU-0003 : SAVE (create)
  → QR-CU-0004 : UPDATE (by key)
  → QR-CU-0005 : DEACTIVATE (soft)
  → QR-CU-0006 : EXISTS (configKey uniqueness)
  → QRC entries in SECTION B

DATA+DOM Governance Rules applied:
  LOC-DOM: nameAr/nameEn waived — key/value entity (documented, srs-CU A2)
  SEC-DOM: soft deactivation per CORE Deactivation Policy
  BIND-RULE-1/2: every column + sequence uses exact db-script names
─────────────────────────────────────────────────────────────────
