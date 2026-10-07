<!-- Source: PHASE:SVC-API -->

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
