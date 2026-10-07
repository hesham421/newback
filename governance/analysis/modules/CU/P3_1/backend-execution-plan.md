<!-- backend-execution-plan.md — Governed by Execution Plan Governance Engine (Project 3.1 / PASS 1) -->

# BACKEND EXECUTION PLAN — Common Utils (CU)

## SECTION 0 — PLAN HEADER
══════════════════════════════════════════════════════════════════
Plan Name        : New Feature — Platform Configuration Store — Common Utils — BE
Plan ID          : PLAN-CU-001
Task Type        : 🆕 New Feature
Feature Code     : CU-001
Module           : Common Utils (CU) — L1 Cross-Cutting Foundation — ROOT — Backend-only
Platform         : Foundation (Domain: ERP)
Governed by      : Execution Plan Governance Engine (Project 3.1) v2
Truth Layer      : Layer 3.1 — Backend Execution Truth
DB_TARGET        : POSTGRESQL_16 (per master-registry v1.1.0 — Architect decision 2026-09-02)
BACKEND_STACK    : SPRING_BOOT_JAVA
DBS-ID           : DBS-CU-001
Output Mode      : SINGLE-FILE — Agent-Ready Specification
GOVERNANCE STATE : NORMAL (srs.md + db-script.md both PRESENT)
Open Questions   : None — see OQ Log (SECTION 3)
══════════════════════════════════════════════════════════════════

⚠ GOVERNANCE ADVISORY (CORE-10 STEP A-5): GOVERNANCE-CONFIG.md declares
DB_TARGET = ORACLE_19C, while master-registry.md + db-script-CU.md declare
POSTGRESQL_16 (recorded Architect decision, 2026-09-02). POSTGRESQL_16 is
used throughout this plan per the authoritative registry decision and
explicit human confirmation. Recommend reconciling GOVERNANCE-CONFIG.md.

---

## SECTION 1 — PLAN INDEX — CU — PLAN-ID: PLAN-CU-001
══════════════════════════════════════════════════════════════════

ENTITY REGISTRY (this plan)
───────────────────────────────────────────────────────────────
ENTITY-ID        │ Entity Name       │ DB Table              │ Business Code │ Operations
─────────────────┼───────────────────┼───────────────────────┼───────────────┼───────────
ENTITY-CU-001    │ AppConfiguration  │ CU_APP_CONFIGURATION  │ NO (BC-RULE-0)│ C,R,U,Deactivate

FIELD REGISTRY (this plan)
───────────────────────────────────────────────────────────────
FIELD-ID   │ Java Property       │ DBF-ID   │ Type          │ Read-Only
───────────┼─────────────────────┼──────────┼───────────────┼──────────
FIELD-0001 │ appConfigurationPk  │ DBF-0001 │ Long          │ System
FIELD-0002 │ configKey           │ DBF-0002 │ String(150)   │ After-create (RULE-CU-003)
FIELD-0003 │ configValue         │ DBF-0003 │ String(text)  │ No
FIELD-0004 │ notes               │ DBF-0004 │ String(2000)  │ No
FIELD-0005 │ isActiveFl          │ DBF-0005 │ Boolean       │ System
FIELD-0006 │ createdBy           │ —        │ String(255)   │ System (audit)
FIELD-0007 │ createdAt           │ —        │ LocalDateTime │ System (audit)
FIELD-0008 │ updatedBy           │ —        │ String(255)   │ System (audit)
FIELD-0009 │ updatedAt           │ —        │ LocalDateTime │ System (audit)
Note: CU owns no Business Code (BC-RULE-0 — internal config entity, per srs-CU A3).

API REGISTRY (this plan)
───────────────────────────────────────────────────────────────
API-ID       │ Operation           │ HTTP   │ Endpoint
─────────────┼─────────────────────┼────────┼───────────────────────────────────────
API-CU-001   │ Create              │ POST   │ /api/v1/common/configurations
API-CU-002   │ Search / List       │ GET    │ /api/v1/common/configurations
API-CU-003   │ Update by key       │ PUT    │ /api/v1/common/configurations/{key}
API-CU-004   │ Deactivate (soft)   │ DELETE │ /api/v1/common/configurations/{key}
API-CU-005   │ Get by key          │ GET    │ /api/v1/common/configurations/{key}

RULE REGISTRY (this plan)
───────────────────────────────────────────────────────────────
RULE-ID      │ Rule Name              │ Scope   │ ENTITY-ID     │ Message-AR defined
─────────────┼────────────────────────┼─────────┼───────────────┼───────────────────
RULE-CU-001  │ Config key uniqueness  │ CREATE  │ ENTITY-CU-001 │ YES
RULE-CU-002  │ Required fields        │ C/U     │ ENTITY-CU-001 │ YES
RULE-CU-003  │ Config key immutable   │ UPDATE  │ ENTITY-CU-001 │ YES

SCREEN REGISTRY (this plan)
───────────────────────────────────────────────────────────────
(none — CU is Backend-only, no SCR-IDs, no SEC_PAGES; CORE-9 does not apply — srs-CU PART B)

LOV REGISTRY (this plan)
───────────────────────────────────────────────────────────────
(none — CU owns zero LOVs — srs-CU A5)

QUERY REFERENCE CATALOG SUMMARY
───────────────────────────────────────────────────────────────
QR-ID        │ Operation           │ Phase     │ Entity
─────────────┼─────────────────────┼───────────┼───────────────
QR-CU-0001   │ FIND_ONE (by key)   │ DATA+DOM  │ ENTITY-CU-001
QR-CU-0002   │ FIND_BY_CRITERIA    │ DATA+DOM  │ ENTITY-CU-001
QR-CU-0003   │ SAVE (create)       │ DATA+DOM  │ ENTITY-CU-001
QR-CU-0004   │ UPDATE (by key)     │ DATA+DOM  │ ENTITY-CU-001
QR-CU-0005   │ DEACTIVATE (soft)   │ DATA+DOM  │ ENTITY-CU-001
QR-CU-0006   │ EXISTS (key unique) │ DATA+DOM  │ ENTITY-CU-001
⚠ ALL QRC entries are AGENT REFERENCE only — agent rewrites every query
  during implementation using actual entity/field names.

DB ALIGNMENT     : see SECTION 2 — ALIGNED ✓
XM STATUS        : None (ROOT module)
CONTRACT GATE    : DOC ✓ | INT-C ✓ (no XM)
SECURITY         : API-level authorization only (Backend-only — no SEC_PAGES)
══════════════════════════════════════════════════════════════════

---

## SECTION 2 — DB ALIGNMENT MANIFEST — CU — PLAN-ID: PLAN-CU-001 / DBS-ID: DBS-CU-001
══════════════════════════════════════════════════════════════════
FIELD-ID  │ DBF-ID   │ Plan Type     │ FK/XM-ID │ Match Status
──────────┼──────────┼───────────────┼──────────┼─────────────
FIELD-0001│ DBF-0001 │ Long          │ —        │ ✓
FIELD-0002│ DBF-0002 │ String(150)   │ —        │ ✓
FIELD-0003│ DBF-0003 │ String(text)  │ —        │ ✓
FIELD-0004│ DBF-0004 │ String(2000)  │ —        │ ✓
FIELD-0005│ DBF-0005 │ Boolean       │ —        │ ✓
══════════════════════════════════════════════════════════════════
Legend: ✓ = aligned | ✗ = type mismatch | ⏸ = XM deferred
Audit FIELD-0006..0009 (createdBy/At, updatedBy/At): no DBF-ID by convention
(AuditableEntity — filled by AuditEntityListener). Derived, not DB-invented.
CONTRACT-1: 5 columns only — no Column Name / DB Type / SRS Source reproduced.

---

## SECTION 3 — OPEN QUESTIONS LOG (continuation)
══════════════════════════════════════════════════════════════════
Open Questions: None — inherited clean from srs-CU.md OQ Log. No new OQ raised in PASS 1.
══════════════════════════════════════════════════════════════════

---

## SECTION 4 — DERIVATION LOG
══════════════════════════════════════════════════════════════════
DRV-ID   │ Element                         │ Criterion │ Source
─────────┼─────────────────────────────────┼───────────┼──────────────────────────────
DRV-001  │ ERR-0004 NOT_FOUND (config)     │ PLATFORM  │ Platform-standard 404 for get/update/deactivate by key
DRV-002  │ QR-CU-0006 EXISTS uniqueness    │ CRIT-2    │ RULE-CU-001 requires a pre-insert existence check on configKey
DRV-003  │ get/update/deactivate by {key}  │ CRIT-1    │ srs-CU MODULE-LEVEL APIs address entity by configKey (business key), not PK
══════════════════════════════════════════════════════════════════

---

<!-- PHASE:CORE:START -->
## PHASE CORE — Architectural Policies
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓

CANONICAL ARCHITECTURE — NON-NEGOTIABLE (backend layers):
  controller/ → REST endpoints only (delegate to service; no business logic)
  service/    → application orchestration + transaction boundaries
  mapper/     → Entity ↔ DTO transformation only
  domain/     → business rules owner
  repository/ → data access only
  entity/     → JPA entity + domain behavior
  dto/ · exception/ · config/

Domain behavior placement: embedded in Entity methods (single simple entity —
medium-complexity mandate POLICY-CLI-01; no separate domain/ classes needed).

PROJECT-STANDARD CONSTRAINTS:
  Entity base      : AuditableEntity (createdBy/createdAt/updatedBy/updatedAt via AuditEntityListener)
                     ✗ audit fields never appear in CreateRequest/UpdateRequest
                     ✗ orgUnitId never appears in any DTO
  Error signaling  : service layer signals LocalizedException — NotFoundException BANNED
  Error catalog    : every ERR-ID registered in 4 places — ErrorCodes constant +
                     messages.properties + i18n JSON + ErpErrorMapperService
  Search contract  : SearchRequest extends BaseSearchContractRequest;
                     ALLOWED_SORT_FIELDS declared per search operation;
                     PageableBuilder.from(...) + SpecBuilder.build(...)
  Deactivation     : isActiveFl = false (record preserved — never hard-deleted)
  i18n / Bundle    : AR + EN resolved via CU's own resource bundles (messages_ar/_en)

TYPE MAPPING STANDARDS (POSTGRESQL_16 — project-standard, no DRV-ID):
  BIGINT        → Java Long
  VARCHAR(N)    → Java String
  TEXT          → Java String (+ @Lob if streamed)
  SMALLINT (_FL)→ Java Boolean
  TIMESTAMP     → Java LocalDateTime

MODULE-SPECIFIC NOTES:
  - CU is the ROOT cross-cutting library; it provides infrastructure
    (Specification/Filtering, Global Exceptions, Bundle, Configuration, Events)
    consumed by SEC/FILE/NOTIF via code injection — NOT an XM dependency.
  - AppConfiguration is the ONLY persisted entity. It is key/value (configKey is
    identity) — no nameAr/nameEn (LOC rule waived for this entity; DRV traced in
    srs-CU A2 general note — pure key/value store).
  - No Workflow Engine (RULE-13 = OFF). No LOVs. No screens (Backend-only).
─────────────────────────────────────────────────────────────────
<!-- PHASE:CORE:END -->

<!-- PHASE:DATA-DOM:START -->
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
<!-- PHASE:DATA-DOM:END -->

<!-- PHASE:SVC-API:START -->
## PHASE SVC+API — Service & API Contract Specifications
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓
(5 APIs < 8 threshold → no SUB split; atomic API markers applied)

<!-- API:API-CU-001:START -->
### API-CU-001 — Create Configuration
Endpoint    : POST /api/v1/common/configurations
Controller  : ConfigurationController → create
Service     : ConfigurationService → create
REQUEST  : ConfigurationCreateRequest { configKey: String REQUIRED, configValue: String REQUIRED, notes: String OPTIONAL }
           Excluded: appConfigurationPk, isActiveFl, audit fields
RESPONSE : 201 → ConfigurationResponse { all fields incl. isActiveFl, audit }
VALIDATIONS:
  1. RULE-CU-002 — Required fields: configKey & configValue required. Message-AR: مفتاح الإعداد وقيمته إلزاميان.
  2. RULE-CU-001 — Uniqueness: configKey must not already exist. Message-AR: مفتاح الإعداد موجود مسبقاً — اختر مفتاحاً فريداً.
ERRORS:
  ERR-0002 → RULE-CU-002 → HTTP 400 (missing required field)
  ERR-0001 → RULE-CU-001 → HTTP 409 (duplicate key)
SERVICE ORCHESTRATION:
  1. validate required fields (RULE-CU-002)
  2. check existence of configKey (QR-CU-0006) → if exists, ERR-0001 (RULE-CU-001)
  3. persist via SEQ_CU_APP_CONFIGURATION; audit via AuditEntityListener
REPOSITORY OPERATION: QR-CU-0003 SAVE — Transaction: READ_WRITE — Sequence: SEQ_CU_APP_CONFIGURATION
SECURITY: API-level authorization (no SCR-ID — Backend-only). Restricted to config-admin authority.
<!-- API:API-CU-001:END -->

<!-- API:API-CU-002:START -->
### API-CU-002 — Search / List Configurations
Endpoint    : GET /api/v1/common/configurations
Controller  : ConfigurationController → search
Service     : ConfigurationService → search
REQUEST  : Query params — configKey? (LIKE), isActiveFl? (EXACT), page, size, sortBy, sortDir
           SearchRequest extends BaseSearchContractRequest; ALLOWED_SORT_FIELDS = { configKey, createdAt, updatedAt }
RESPONSE : 200 → Page<ConfigurationResponse> (empty content [] when no match — NEVER 404)
VALIDATIONS: none (read)
ERRORS: none (empty result → HTTP 200)
REPOSITORY OPERATION: QR-CU-0002 FIND_BY_CRITERIA — Transaction: READ_ONLY — Join: NONE
SECURITY: API-level authorization.
<!-- API:API-CU-002:END -->

<!-- API:API-CU-003:START -->
### API-CU-003 — Update Configuration (by key)
Endpoint    : PUT /api/v1/common/configurations/{key}
Controller  : ConfigurationController → update
Service     : ConfigurationService → update
REQUEST  : Path {key}: String; Body ConfigurationUpdateRequest { configValue: String REQUIRED, notes?: String, isActiveFl?: Boolean }
           Excluded: configKey (immutable — RULE-CU-003), appConfigurationPk, audit fields
RESPONSE : 200 → ConfigurationResponse
VALIDATIONS:
  1. RULE-CU-002 — configValue required. Message-AR: مفتاح الإعداد وقيمته إلزاميان.
  2. RULE-CU-003 — configKey not modifiable. Message-AR: لا يمكن تعديل مفتاح الإعداد بعد إنشائه.
ERRORS:
  ERR-0004 → PLATFORM-STD → HTTP 404 (configKey not found)
  ERR-0002 → RULE-CU-002 → HTTP 400 (missing configValue)
  ERR-0003 → RULE-CU-003 → HTTP 422 (attempt to change configKey)
SERVICE ORCHESTRATION:
  1. load by configKey (QR-CU-0001) → if absent, ERR-0004
  2. reject any configKey change (RULE-CU-003) → ERR-0003
  3. validate configValue present (RULE-CU-002)
  4. persist update; audit via AuditEntityListener
REPOSITORY OPERATION: QR-CU-0004 UPDATE — Transaction: READ_WRITE
SECURITY: API-level authorization.
<!-- API:API-CU-003:END -->

<!-- API:API-CU-004:START -->
### API-CU-004 — Deactivate Configuration (soft, by key)
Endpoint    : DELETE /api/v1/common/configurations/{key}
Controller  : ConfigurationController → deactivate
Service     : ConfigurationService → deactivate
REQUEST  : Path {key}: String
RESPONSE : 200 → confirmation (or 204)
VALIDATIONS: none beyond existence
ERRORS:
  ERR-0004 → PLATFORM-STD → HTTP 404 (configKey not found)
SERVICE ORCHESTRATION:
  1. load by configKey (QR-CU-0001) → if absent, ERR-0004
  2. set isActiveFl = false (QR-CU-0005) — record preserved, NOT hard-deleted
REPOSITORY OPERATION: QR-CU-0005 DEACTIVATE — Transaction: READ_WRITE
SECURITY: API-level authorization.
<!-- API:API-CU-004:END -->

<!-- API:API-CU-005:START -->
### API-CU-005 — Get Configuration by key
Endpoint    : GET /api/v1/common/configurations/{key}
Controller  : ConfigurationController → getByKey
Service     : ConfigurationService → getByKey
REQUEST  : Path {key}: String
RESPONSE : 200 → ConfigurationResponse
ERRORS:
  ERR-0004 → PLATFORM-STD → HTTP 404 (configKey not found)
REPOSITORY OPERATION: QR-CU-0001 FIND_ONE (by configKey) — Transaction: READ_ONLY
SECURITY: API-level authorization.
Note (internal): ConfigurationService.getValue(configKey) is the in-process read
used by other modules (part of CU library) — not an HTTP endpoint.
<!-- API:API-CU-005:END -->

API Governance Rules applied: RULE-ERR-CARRY ✓ (every Validation RULE-ID has a matching ERR-ID);
RULE-PLATFORM-ERR ✓ (ERR-0004 = PLATFORM-STD + DRV-001); LOC-B2 ✓ (all errors carry AR+EN).
─────────────────────────────────────────────────────────────────
<!-- PHASE:SVC-API:END -->

<!-- PHASE:DOC:START -->
## PHASE DOC — Contract Stabilization (INTERNAL-ONLY, v2.0)
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓

### DOC-1: API Contract Summary (internal self-check — NOT the frontend source, CONTRACT-12)
API-ID     │ Endpoint                                  │ Method │ Request DTO                 │ Response DTO           │ Stability
───────────┼───────────────────────────────────────────┼────────┼─────────────────────────────┼────────────────────────┼──────────
API-CU-001 │ /api/v1/common/configurations             │ POST   │ ConfigurationCreateRequest  │ ConfigurationResponse  │ STABLE
API-CU-002 │ /api/v1/common/configurations             │ GET    │ SearchRequest (params)      │ Page<ConfigurationResp>│ STABLE
API-CU-003 │ /api/v1/common/configurations/{key}       │ PUT    │ ConfigurationUpdateRequest  │ ConfigurationResponse  │ STABLE
API-CU-004 │ /api/v1/common/configurations/{key}       │ DELETE │ —                           │ confirmation           │ STABLE
API-CU-005 │ /api/v1/common/configurations/{key}       │ GET    │ —                           │ ConfigurationResponse  │ STABLE

### DOC-2: DTO Typing Rules
  Business Code: N/A (CU owns none). No LOV/ENUM fields.

### DOC-3: Pagination & Filter Standards
  JPA Page<T> used directly; SearchRequest extends BaseSearchContractRequest;
  empty result → HTTP 200 (never 404); filters: configKey LIKE, isActiveFl EXACT.

DOC GATE CHECK:
  [✓] All API-IDs appear in summary  [✓] Error Catalog complete AR+EN
  [✓] All APIs STABLE                [✓] Pagination standard declared
DOC Gate: PASSED ✓
⚠ v2.0: DOC-1 is INTERNAL-ONLY. PASS 2 gates on real API Docs, not this table (CONTRACT-12).
─────────────────────────────────────────────────────────────────
<!-- PHASE:DOC:END -->

<!-- PHASE:INT-C:START -->
## PHASE INT-C — Integration Contract Specifications
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓

## INT-C SUMMARY — CU — PLAN-ID: PLAN-CU-001
XM-ID │ Classification │ Target │ Interface │ Contract Status
──────┼────────────────┼────────┼───────────┼────────────────
(none — CU is the ROOT cross-cutting library; no outbound XM dependencies — db-script-CU §3)

INBOUND XM STUB NOTATION:
  CU is consumed by SEC/FILE/NOTIF as a code library (dependency injection), NOT
  via an XM data dependency. No INBOUND-STUB required (master-registry §8: "CU is a
  library used by all — not an XM dependency").

INT-C GATE CHECK:
  [✓] All XM-IDs from DB Script XM Register accounted for (zero)
  [✓] No new XM-IDs invented   [✓] Open RXEs acknowledged (none)
INT-C Gate: PASSED ✓
─────────────────────────────────────────────────────────────────
<!-- PHASE:INT-C:END -->

<!-- PHASE:INT-R:START -->
## PHASE INT-R — Runtime Activation Status
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓

## INT-R STATUS — CU — PLAN-ID: PLAN-CU-001
XM-ID │ Status │ Workaround / Mock Strategy
──────┼────────┼───────────────────────────
(none — no cross-module runtime dependencies)
─────────────────────────────────────────────────────────────────
<!-- PHASE:INT-R:END -->

<!-- PHASE:SEC-BE:START -->
## PHASE SEC-BE — Backend Security Specifications
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓

SCOPE: CU is Backend-only with NO screens → no SCR-IDs, no SEC_PAGES rows,
CORE-9 Composite Screen governance does NOT apply (srs-CU PART B).

API-LEVEL ENFORCEMENT:
  Configuration management endpoints are administrative. Every API-CU-00x method
  requires authorization enforced at the API level before request processing
  (config-admin authority). HTTP 403 on failure → mapped via LocalizedException.
  The concrete authority/permission source is the Security (SEC) module; CU
  declares the enforcement requirement, SEC provides the mechanism.

SECURITY SEED DATA REQUIREMENTS: none (no SEC_PAGES/PERMISSIONS rows — no screens).

SEC-BE Governance Rules:
  SEC-IMPL-RULE-1 — every configuration endpoint enforces authorization at API level
  SEC-IMPL-RULE-3 — HTTP 403 mapped via LocalizedException carrying correct ERR-ID
─────────────────────────────────────────────────────────────────
<!-- PHASE:SEC-BE:END -->

<!-- PHASE:ALIGN-BE:START -->
## PHASE ALIGN-BE — Backend Internal Self-Consistency Gate (auto-run)
─────────────────────────────────────────────────────────────────

## ALIGN-BE GATE — CU — PLAN-ID: PLAN-CU-001
TRACEABILITY CHECKS                                          │ Status
─────────────────────────────────────────────────────────────┼────────
All FIELD-IDs used in phases appear in Plan Index            │ ✓
All API-IDs used in phases appear in Plan Index              │ ✓
All RULE-IDs used in phases appear in Plan Index             │ ✓
All ERR-IDs used in Error Catalog appear correctly           │ ✓
All QR-IDs in QRC appear in Plan Index QRC Summary           │ ✓
Derivation Log complete — no undocumented inferences         │ ✓
DB Structural Alignment confirms field coverage              │ ✓
BUSINESS CODE CHECKS                                          │ Status
─────────────────────────────────────────────────────────────┼────────
Business Code excluded from POST/PUT bodies                  │ ✓ (N/A — no BC)
Business Code always present in GET/response DTOs            │ ✓ (N/A — no BC)
LOCALIZATION CHECKS                                          │ Status
─────────────────────────────────────────────────────────────┼────────
All RULE-IDs have Message-AR defined                         │ ✓
All API error responses: messageAr + messageEn               │ ✓
SECURITY CHECKS                                              │ Status
─────────────────────────────────────────────────────────────┼────────
Every API-ID has authorization declared                      │ ✓
SCR-ID / SEC-BE coverage                                     │ ✓ (N/A — no screens)
QUERY REFERENCE CATALOG CHECKS                               │ Status
─────────────────────────────────────────────────────────────┼────────
Every API-ID with DB op has QR-ID in QRC                     │ ✓
Every QR-ID has agent-reference warning label                 │ ✓
No QR entry references ENUM for LOV fields                    │ ✓ (no LOV)
No QR entry joins to lookups table                            │ ✓
Every QR-ID states exact sequence name (SAVE)                │ ✓ SEQ_CU_APP_CONFIGURATION
TEST-BE COVERAGE CHECKS                                       │ Status
─────────────────────────────────────────────────────────────┼────────
TC Coverage Matrix Summary present in SECTION D              │ ✓
No GAP ✗ without DEFERRED                                     │ ✓
ARTIFACT BINDING CHECKS (Section 2A compliance)              │ Status
─────────────────────────────────────────────────────────────┼────────
No placeholder [TABLE_NAME]/[LOOKUP_CODE]/[SEQ_NAME]         │ ✓
No RULE block shows "see SRS" — all text inline              │ ✓
Every column name traces to a DBF-ID                          │ ✓
Every Message-AR is exact text                               │ ✓
DB Alignment Manifest: 5 columns only (CONTRACT-1)           │ ✓
PLAN COMPLETENESS CHECKS                                      │ Status
─────────────────────────────────────────────────────────────┼────────
Canonical architecture declared in PHASE CORE                │ ✓
Domain behavior placement declared                           │ ✓
No orgUnitId in any DTO                                       │ ✓
No audit fields in CreateRequest/UpdateRequest               │ ✓
Error signaling strategy declared (LocalizedException)       │ ✓
All ERR-IDs have 4-registration points declared              │ ✓
All search operations declare ALLOWED_SORT_FIELDS            │ ✓
Empty search → HTTP 200 declared                             │ ✓
Pre-deactivation existence check declared                    │ ✓
═══════════════════════════════════════════════════════════════════
ALIGN-BE GATE RESULT: PASSED ✓
Auto-correction applied: None
═══════════════════════════════════════════════════════════════════

Table 3 — XM Dependency Gate:
XM-ID │ Type │ Status │ Blocks │ Workaround
(none)
─────────────────────────────────────────────────────────────────
<!-- PHASE:ALIGN-BE:END -->

---

## SECTION A — ERROR CATALOG (canonical)
══════════════════════════════════════════════════════════════════════════════════
ERR-ID   │ RULE-ID      │ API-ID              │ HTTP │ Trigger                  │ Message-AR                                   │ Message-EN
─────────┼──────────────┼─────────────────────┼──────┼──────────────────────────┼──────────────────────────────────────────────┼────────────────────────────────
ERR-0001 │ RULE-CU-001  │ API-CU-001          │ 409  │ Duplicate configKey       │ مفتاح الإعداد موجود مسبقاً — اختر مفتاحاً فريداً. │ Configuration key already exists — choose a unique key.
ERR-0002 │ RULE-CU-002  │ API-CU-001/003      │ 400  │ Required field missing    │ مفتاح الإعداد وقيمته إلزاميان.                  │ Config key and value are required.
ERR-0003 │ RULE-CU-003  │ API-CU-003          │ 422  │ configKey change attempt  │ لا يمكن تعديل مفتاح الإعداد بعد إنشائه.          │ Config key cannot be changed after creation.
ERR-0004 │ PLATFORM-STD │ API-CU-003/004/005  │ 404  │ configKey not found       │ الإعداد غير موجود.                              │ Configuration not found.
══════════════════════════════════════════════════════════════════════════════════
Total Errors: 4 (ERR-0004 = PLATFORM-STD, documented DRV-001).
Every ERR-ID registered in 4 places (ErrorCodes + messages.properties + i18n JSON + ErpErrorMapperService).

---

## SECTION B — QUERY REFERENCE CATALOG (agent reference)
══════════════════════════════════════════════════════════════════
⚠ AGENT REFERENCE ONLY — rewrite every query using actual JPA entity/field names.

QR-CU-0001 — Find configuration by key
  Phase: DATA+DOM | API-ID: API-CU-005/003/004 | Entity: ENTITY-CU-001 | Operation: FIND_ONE
  Intent: fetch one AppConfiguration by its business key (configKey).
  Spec: SELECT * FROM CU_APP_CONFIGURATION WHERE CONFIG_KEY = :key
  Transaction: READ_ONLY | Result: entity or throw LocalizedException(ERR-0004) | Join: NONE

QR-CU-0002 — Search configurations
  Phase: DATA+DOM | API-ID: API-CU-002 | Entity: ENTITY-CU-001 | Operation: FIND_BY_CRITERIA
  Intent: paged search with optional configKey (LIKE) and isActiveFl (EXACT).
  Spec: SELECT * FROM CU_APP_CONFIGURATION WHERE (:key IS NULL OR CONFIG_KEY LIKE %:key%)
        AND (:active IS NULL OR IS_ACTIVE_FL = :active) ORDER BY :sort
  Transaction: READ_ONLY | Pagination: YES | Filters: configKey LIKE, isActiveFl EXACT
  Empty → HTTP 200 content [] | Join: NONE

QR-CU-0003 — Create configuration
  Phase: DATA+DOM | API-ID: API-CU-001 | Entity: ENTITY-CU-001 | Operation: SAVE
  Intent: insert a new configuration; PK from SEQ_CU_APP_CONFIGURATION; audit via listener.
  Spec: INSERT INTO CU_APP_CONFIGURATION (ID, CONFIG_KEY, CONFIG_VALUE, NOTES, IS_ACTIVE_FL, ...audit)
        VALUES (SEQ_CU_APP_CONFIGURATION.next, :key, :value, :notes, 1, ...)
  Transaction: READ_WRITE

QR-CU-0004 — Update configuration
  Phase: DATA+DOM | API-ID: API-CU-003 | Entity: ENTITY-CU-001 | Operation: UPDATE
  Intent: update configValue/notes/isActiveFl by key; CONFIG_KEY excluded (immutable).
  Spec: UPDATE CU_APP_CONFIGURATION SET CONFIG_VALUE=:value, NOTES=:notes, IS_ACTIVE_FL=:active
        WHERE CONFIG_KEY = :key
  Transaction: READ_WRITE

QR-CU-0005 — Deactivate configuration (soft)
  Phase: DATA+DOM | API-ID: API-CU-004 | Entity: ENTITY-CU-001 | Operation: UPDATE (soft delete)
  Intent: set IS_ACTIVE_FL = 0; record preserved.
  Spec: UPDATE CU_APP_CONFIGURATION SET IS_ACTIVE_FL = 0 WHERE CONFIG_KEY = :key
  Transaction: READ_WRITE

QR-CU-0006 — Config key existence check
  Phase: DATA+DOM | API-ID: API-CU-001 | Entity: ENTITY-CU-001 | Operation: EXISTS
  Intent: pre-insert uniqueness check for RULE-CU-001.
  Spec: SELECT COUNT(*) > 0 FROM CU_APP_CONFIGURATION WHERE CONFIG_KEY = :key
  Transaction: READ_ONLY
══════════════════════════════════════════════════════════════════

---

## SECTION C — REGISTRY UPDATE BLOCK
══════════════════════════════════════════════════════════════════
## REGISTRY UPDATE — 2026-09-02
Source          : Project 3.1 — PASS 1 (Backend)
Feature Code    : CU-001
DBS-ID          : DBS-CU-001
Plan ID         : PLAN-CU-001
New Entities    : (none new — ENTITY-CU-001 from P1)
New APIs        : API-CU-001..005
QR-IDs Created  : QR-CU-0001..0006 (6)
XM-IDs Open     : None
OQ-IDs Open     : None
Gate Status     : ALIGN-BE PASSED ✓
Next Action     : Project 4.1 — Backend Audit Gate → Pipeline Status Grid: CU · P3.1 = done
══════════════════════════════════════════════════════════════════

---

## SECTION D — TC COVERAGE MATRIX SUMMARY (backend)
══════════════════════════════════════════════════════════════════
NOTE: TC-IDs are placeholders — full blocks in backend-test-plan-CU.md.

RULE-ID COVERAGE:
RULE-ID      │ Happy path TC   │ Violation TC    │ Status
─────────────┼─────────────────┼─────────────────┼────────
RULE-CU-001  │ TC-BE-CU-001    │ TC-BE-CU-002    │ COVERED ✓
RULE-CU-002  │ TC-BE-CU-003    │ TC-BE-CU-004    │ COVERED ✓
RULE-CU-003  │ TC-BE-CU-005    │ TC-BE-CU-006    │ COVERED ✓
Rule coverage: 3/3 covered — 0 deferred — 0 gaps

API-ID COVERAGE:
API-ID       │ Success TC      │ Status
─────────────┼─────────────────┼────────
API-CU-001   │ TC-BE-CU-001    │ COVERED ✓
API-CU-002   │ TC-BE-CU-007    │ COVERED ✓
API-CU-003   │ TC-BE-CU-008    │ COVERED ✓
API-CU-004   │ TC-BE-CU-009    │ COVERED ✓
API-CU-005   │ TC-BE-CU-010    │ COVERED ✓
API coverage: 5/5 covered — 0 deferred

DEFERRED TC REGISTRY: (none)
══════════════════════════════════════════════════════════════════
Gate SECTION D: PASSED ✓ (no GAP ✗ without DEFERRED)

---

## AGENT HANDOFF SUMMARY (BACKEND) — not a phase
This plan is agent-ready. Agent MUST: read full plan first; rewrite all QRC
entries from scratch using actual entity/field names; follow CORE architecture;
apply RULE-CU-001..003 in the domain layer; use ERR-0001..0004 in error handling;
enforce API-level authorization; write tests per backend-test-plan-CU.md; after
implementation run api-doc-generator before PASS 2.

*End of backend-execution-plan.md — CU — PLAN-CU-001 — ALIGN-BE ✓*
