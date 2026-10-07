<!-- backend-execution-plan.md — Governed by Execution Plan Governance Engine (Project 3.1 / PASS 1) -->

# BACKEND EXECUTION PLAN — File Service (FILE)

## SECTION 0 — PLAN HEADER
══════════════════════════════════════════════════════════════════
Plan Name        : New Feature — File Storage Provider — File Service — BE
Plan ID          : PLAN-FILE-001
Task Type        : 🆕 New Feature   |   Feature Code: FILE-001 (srs-FILE.md v1.1)
Module           : File Service (FILE) — L1 Service (Provider) — dep: CU, SEC — Backend + Frontend
Platform         : Foundation (Domain: ERP)   |   Truth Layer: Layer 3.1
DB_TARGET        : POSTGRESQL_16    BACKEND_STACK: SPRING_BOOT_JAVA    DBS-ID: DBS-FILE-001
Output Mode      : SINGLE-FILE — Agent-Ready Specification
GOVERNANCE STATE : NORMAL (srs.md + db-script.md both PRESENT)
Open Questions   : None (file-delete semantics resolved — RULE-FILE-006 soft-delete)
══════════════════════════════════════════════════════════════════

---

## SECTION 1 — PLAN INDEX — FILE — PLAN-ID: PLAN-FILE-001
══════════════════════════════════════════════════════════════════
ENTITY REGISTRY
ENTITY-FILE-001 │ FileDocument │ FILE_DOCUMENT │ NO (secure id/token) │ Upload,Read/Download,List,Archive,Delete(soft) │ Transactional
ENTITY-FILE-002 │ FileCategory │ FILE_CATEGORY │ NO                   │ Create,Read,Update,Deactivate                   │ Reference

FIELD REGISTRY (FIELD-ID ↔ DBF-ID, 1:1)
FIELD-0001..0011 → DBF-0001..0011 : FILE_DOCUMENT (ID, OWNER_ID, OWNER_TYPE, MODULE_CODE, FILE_NAME, CONTENT_TYPE, FILE_SIZE, FILE_CONTENT[BYTEA], FILE_TYPE_ID, FILE_STATUS_ID, FILE_CATEGORY_FK)
FIELD-0012..0018 → DBF-0012..0018 : FILE_CATEGORY (ID, CATEGORY_CODE, NAME_AR, NAME_EN, MAX_SIZE_BYTES, ALLOWED_CONTENT_TYPES, IS_ACTIVE_FL)
Audit fields (createdBy/At, updatedBy/At): no DBF-ID (AuditEntityListener).

API REGISTRY
API-FILE-001 Upload POST /api/v1/files                       | API-FILE-002 Access-token POST /api/v1/files/{id}/access-token
API-FILE-003 Download GET /api/v1/files/download?token=      | API-FILE-004 Metadata GET /api/v1/files/{id}
API-FILE-005 List-by-owner GET /api/v1/files?ownerId=&ownerType=&moduleCode= | API-FILE-006 Archive/Delete DELETE /api/v1/files/{id}
API-FILE-007 Categories CRUD /api/v1/files/categories        | API-FILE-008 Lookups GET /api/v1/files/lookups/{lookupKey}
Provider (in-process): FileService.store/retrieve/issueAccessToken — injected into NOTIF & future modules.

RULE REGISTRY : RULE-FILE-001..007 (RULE-FILE-001/002/003 carry ⚠ Client-Policy defaults).
SCREEN REGISTRY (CORE-9)
SCR-FILE-001 │ File Categories │ COMPOSITE PATTERN-2 SIDE_DRAWER │ ENTITY-FILE-002 │ page_code FILE_CATEGORIES
SCR-FILE-002 │ File Browser    │ COMPOSITE PATTERN-2 SIDE_DRAWER │ ENTITY-FILE-001 │ page_code FILE_BROWSER
LOV REGISTRY (FILE-local runtime codes)
LOV-FILE-001 │ FILE_FILE_TYPE   │ fileTypeId   │ IMAGE, DOCUMENT, SPREADSHEET, ARCHIVE, OTHER
LOV-FILE-002 │ FILE_FILE_STATUS │ fileStatusId │ ACTIVE, ARCHIVED, DELETED
QRC SUMMARY : QR-FILE-0001..0011 (SECTION B). ⚠ AGENT REFERENCE only.
DB ALIGNMENT : SECTION 2 — ALIGNED ✓ (18 FIELD↔DBF)
XM STATUS    : XM-FILE-001 SOFT-READ → SEC_USER_ACCOUNT (Security) — READY ✓ (SEC gated ACTIVE, built earlier)
SECURITY     : 2 admin screens × 4 CORE-9 permissions (FILE_ADMIN). Auth delegated to Security filter (RULE-FILE-004).
══════════════════════════════════════════════════════════════════

---

## SECTION 2 — DB ALIGNMENT MANIFEST — FILE — DBS-ID: DBS-FILE-001
══════════════════════════════════════════════════════════════════
FIELD-ID       │ DBF-ID        │ Plan Type   │ FK/XM-ID                   │ Match
───────────────┼───────────────┼─────────────┼────────────────────────────┼──────
FIELD-0002/3/4 │ DBF-0002/3/4  │ (polymorphic)│ owner ref (NO FK — app-level)│ ✓
FIELD-0008     │ DBF-0008      │ byte[]      │ BYTEA                      │ ✓
FIELD-0011     │ DBF-0011      │ Long        │ FK FILE_CATEGORY_FK→FILE_CATEGORY │ ✓
(created_by identity read of SEC_USER_ACCOUNT is XM-FILE-001 SOFT-READ — application layer, no column FK)
All other FIELD-IDs align 1:1 to their DBF-ID. No type mismatch.
══════════════════════════════════════════════════════════════════
Legend: ✓ aligned | ✗ mismatch | ⏸ XM deferred. CONTRACT-1: FIELD-ID/DBF-ID/Type/FK/Status only. Audit cols: no DBF-ID.

---

## SECTION 3 — OPEN QUESTIONS LOG (continuation)
══════════════════════════════════════════════════════════════════
Open Questions: None — file-delete semantics resolved in srs-FILE (RULE-FILE-006 soft-delete). No new OQ in PASS 1.
══════════════════════════════════════════════════════════════════

---

## SECTION 4 — DERIVATION LOG
══════════════════════════════════════════════════════════════════
DRV-001 │ ERR-0006 NOT_FOUND (file/category)     │ PLATFORM │ Standard 404 for get/metadata/archive by id
DRV-002 │ QR-FILE-0011 EXISTS categoryCode        │ CRIT-2   │ RULE-FILE-007 pre-insert uniqueness check
DRV-003 │ BYTEA streamed with @Lob                │ CRIT-1   │ FILE_CONTENT BYTEA (db-script) — large binary, streamed not eager-loaded on list
DRV-004 │ Effective size/type limit resolution    │ CRIT-2   │ RULE-FILE-001/002 defaults overridden per FileCategory (maxSizeBytes/allowedContentTypes)
══════════════════════════════════════════════════════════════════

---

<!-- PHASE:CORE:START -->
## PHASE CORE — Architectural Policies
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓
CANONICAL ARCHITECTURE (backend): controller/ service/ mapper/ domain/ repository/ entity/ dto/ exception/ config/
Domain behavior placement: separate classes in domain/ (access-token issuing AES/GCM, MIME auto-detect, size/type policy resolution are non-trivial — e.g. FileAccessTokenDomainService, FileValidationDomainService).
PROJECT-STANDARD CONSTRAINTS:
  Entity base      : AuditableEntity on both tables (audit via AuditEntityListener). ✗ orgUnitId never in any DTO.
  Error signaling  : LocalizedException — NotFoundException BANNED.
  Error catalog    : every ERR-ID registered 4× (ErrorCodes + messages.properties + i18n JSON + ErpErrorMapperService).
  Search contract  : SearchRequest extends BaseSearchContractRequest; ALLOWED_SORT_FIELDS per search.
  Deactivation     : FileCategory isActiveFl=false; FileDocument soft-delete via fileStatusId=DELETED (RULE-FILE-006) — bytes retained.
  Auth             : delegate to Security filter — FILE never self-verifies JWT (RULE-FILE-004). Owner-based visibility (RULE-FILE-005).
  Access tokens    : AES/GCM, ~100m TTL, single-use — separate from JWT (RULE-FILE-003).
TYPE MAPPING (POSTGRESQL_16): BIGINT→Long · VARCHAR(N)→String · TEXT→String(+@Lob) · BYTEA→byte[](@Lob) · SMALLINT(_FL)→Boolean · TIMESTAMP→LocalDateTime.
MODULE-SPECIFIC NOTES:
  - Provider pattern: FileService is a @Service injected into consumers (NOTIF uses it for template attachments). No HTTP call needed in-process.
  - Ownership is polymorphic (ownerId/ownerType/moduleCode) — application reference, NOT a governed FK.
  - LOV values runtime codes (no ENUM, no lookup table). contentType is server-detected, never trusted from client (RULE-FILE-002).
  - No Workflow Engine (RULE-13 = OFF).
─────────────────────────────────────────────────────────────────
<!-- PHASE:CORE:END -->

<!-- PHASE:DATA-DOM:START -->
## PHASE DATA+DOM — Entity & Domain Specifications
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓
(2 entities < 5 → no SUB split)

### ENTITY-FILE-001 — FileDocument
  DB Table FILE_DOCUMENT | PK ID | Sequence SEQ_FILE_DOCUMENT | DBS-FILE-001. BUSINESS CODE: NONE.
  FIELDS (0001..0011 → DBF-0001..0011):
    FIELD-0001 fileDocumentPk  ID              DBF-0001 BIGINT       PK SEQ_FILE_DOCUMENT     | المعرف / ID
    FIELD-0002 ownerId         OWNER_ID        DBF-0002 BIGINT       polymorphic (no FK)      | معرّف المالك / Owner ID
    FIELD-0003 ownerType       OWNER_TYPE      DBF-0003 VARCHAR(100) polymorphic              | نوع المالك / Owner Type
    FIELD-0004 moduleCode      MODULE_CODE     DBF-0004 VARCHAR(50)  owning module            | رمز الموديول / Module Code
    FIELD-0005 fileName        FILE_NAME       DBF-0005 VARCHAR(255) NOT NULL                 | اسم الملف / File Name
    FIELD-0006 contentType     CONTENT_TYPE    DBF-0006 VARCHAR(150) System — auto-detected (RULE-FILE-002) | نوع المحتوى / Content Type
    FIELD-0007 fileSize        FILE_SIZE       DBF-0007 BIGINT       System (bytes)           | الحجم / Size
    FIELD-0008 fileContent     FILE_CONTENT    DBF-0008 BYTEA        NOT NULL (@Lob, streamed)| المحتوى / Content
    FIELD-0009 fileTypeId      FILE_TYPE_ID    DBF-0009 VARCHAR(50)  LOV-FILE-001 code        | نوع الملف / File Type
    FIELD-0010 fileStatusId    FILE_STATUS_ID  DBF-0010 VARCHAR(50)  LOV-FILE-002 code (A6)   | الحالة / Status
    FIELD-0011 fileCategoryFk  FILE_CATEGORY_FK DBF-0011 BIGINT      FK→FILE_CATEGORY (nullable)| الفئة / Category
  DTO: UploadRequest{multipart file, ownerId, ownerType, moduleCode, fileCategoryFk?} (contentType/fileSize/fileTypeId system-derived);
       ResponseDTO: metadata only — fileContent NEVER in a JSON DTO (downloaded via secure token).
  STATE MACHINE (fileStatusId — LOV-FILE-002): ACTIVE → ARCHIVED → DELETED (and ACTIVE → DELETED soft). Initial: ACTIVE.
  DOMAIN RULES: RULE-FILE-001 (size ≤5MB content/≤10MB request, category override), RULE-FILE-002 (MIME auto-detect + allowed types),
       RULE-FILE-003 (AES/GCM single-use ~100m token), RULE-FILE-004 (delegate auth to Security), RULE-FILE-005 (ownership required),
       RULE-FILE-006 (soft delete). Full text in Error Catalog + SVC+API.
  CROSS-MODULE: XM-FILE-001 SOFT-READ → SEC_USER_ACCOUNT (created_by identity + auth filter) — no FK. QR: QR-FILE-0001..0006.

### ENTITY-FILE-002 — FileCategory
  DB Table FILE_CATEGORY | PK ID | Sequence SEQ_FILE_CATEGORY. FIELDS 0012..0018 (categoryCode UNIQUE, nameAr, nameEn, maxSizeBytes, allowedContentTypes[TEXT], isActiveFl).
  DOMAIN RULES: RULE-FILE-007 (unique categoryCode — Message-AR: رمز الفئة مستخدَم مسبقاً.). Feeds RULE-FILE-001/002 per-category limits.
  QR: QR-FILE-0007..0011.

DATA+DOM Governance: BIND-RULE-1/2/3/4 — exact column/sequence/LOOKUP_CODE/RULE text from srs-FILE/db-script-FILE.
─────────────────────────────────────────────────────────────────
<!-- PHASE:DATA-DOM:END -->

<!-- PHASE:SVC-API:START -->
## PHASE SVC+API — Service & API Contract Specifications
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓
(8 APIs ≥ 8 → SUB split: FILES / CATEGORIES / LOOKUP; atomic API markers applied)

  <!-- SUB:SVC-API-FILES:START -->
<!-- API:API-FILE-001:START -->
### API-FILE-001 — Upload file
POST /api/v1/files (multipart) | FileController.upload → FileService.store
REQUEST UploadRequest{file(multipart), ownerId, ownerType, moduleCode, fileCategoryFk?} | RESPONSE 201 FileMetadataResponse (no bytes)
VALIDATIONS: RULE-FILE-005 (ownerId, ownerType, moduleCode required — Message-AR: بيانات الملكية إلزامية.);
             RULE-FILE-001 (size ≤ limit; category override — Message-AR: حجم الملف يتجاوز المسموح.);
             RULE-FILE-002 (auto-detect MIME; restrict to allowed types — Message-AR: نوع الملف غير مسموح.)
ERRORS: ERR-0004 → RULE-FILE-005 → 400; ERR-0001 → RULE-FILE-001 size → 413; ERR-0002 → RULE-FILE-002 type → 415
ORCHESTRATION: validate ownership (RULE-FILE-005) → resolve category limits (DRV-004) → auto-detect MIME & enforce type (RULE-FILE-002) → enforce size (RULE-FILE-001) → store bytes (QR-FILE-0001) with fileStatusId=ACTIVE.
REPO: QR-FILE-0001 SAVE — READ_WRITE — Sequence SEQ_FILE_DOCUMENT | SECURITY: Security filter (RULE-FILE-004); upload contextual to owner module.
<!-- API:API-FILE-001:END -->
<!-- API:API-FILE-002:START -->
### API-FILE-002 — Issue access token
POST /api/v1/files/{id}/access-token | FileController.issueToken → FileAccessTokenDomainService.issue
REQUEST path {id} | RESPONSE 200 {accessToken, expiresAt}
VALIDATIONS: RULE-FILE-003 (fresh AES/GCM single-use token, ~100m TTL); RULE-FILE-004 (owner visibility)
ERRORS: ERR-0006 → NOT_FOUND → 404
REPO: QR-FILE-0002 FIND_ONE — READ_ONLY | SECURITY: Security filter + ownership (RULE-FILE-005).
<!-- API:API-FILE-002:END -->
<!-- API:API-FILE-003:START -->
### API-FILE-003 — Download file
GET /api/v1/files/download?token= | FileController.download → FileService.retrieve
REQUEST query token | RESPONSE 200 binary stream (Content-Type from stored contentType)
VALIDATIONS: RULE-FILE-003 (token valid, unexpired, single-use); RULE-FILE-004 (delegate auth)
ERRORS: ERR-0003 → RULE-FILE-003 invalid/expired token → 401
ORCHESTRATION: validate & consume token (RULE-FILE-003) → stream FILE_CONTENT (QR-FILE-0003, @Lob stream).
REPO: QR-FILE-0003 FIND_ONE (bytes) — READ_ONLY | SECURITY: token-gated (separate from JWT).
<!-- API:API-FILE-003:END -->
<!-- API:API-FILE-004:START -->
### API-FILE-004 — File metadata
GET /api/v1/files/{id} | FileController.metadata → FileService.getMetadata
RESPONSE 200 FileMetadataResponse (no bytes)
VALIDATIONS: RULE-FILE-004 (delegate auth); ownership (RULE-FILE-005)
ERRORS: ERR-0006 → NOT_FOUND → 404
REPO: QR-FILE-0004 FIND_ONE (metadata projection, no bytes) — READ_ONLY | SECURITY: Security filter.
<!-- API:API-FILE-004:END -->
<!-- API:API-FILE-005:START -->
### API-FILE-005 — List files by owner
GET /api/v1/files?ownerId=&ownerType=&moduleCode= | FileController.listByOwner → FileService.listByOwner
REQUEST params ownerId,ownerType,moduleCode (+ fileTypeId?, fileStatusId?, page,size); ALLOWED_SORT_FIELDS={fileName,createdAt,fileSize}
RESPONSE 200 Page<FileMetadataResponse> (empty → 200 [], never 404)
VALIDATIONS: RULE-FILE-004 (delegate auth) | ERRORS: none (empty → 200)
REPO: QR-FILE-0005 FIND_BY_CRITERIA (metadata only, bytes excluded — DRV-003) — READ_ONLY — Join NONE | SECURITY: Security filter.
<!-- API:API-FILE-005:END -->
<!-- API:API-FILE-006:START -->
### API-FILE-006 — Archive / soft-delete file
DELETE /api/v1/files/{id} | FileController.archiveOrDelete → FileService.softDelete
REQUEST path {id} (+ action=ARCHIVE|DELETE) | RESPONSE 200/204
VALIDATIONS: RULE-FILE-006 (soft delete → fileStatusId=DELETED; ARCHIVE → ARCHIVED; bytes retained — Message-AR: حذف منطقي دون إزالة فيزيائية.)
ERRORS: ERR-0006 → NOT_FOUND → 404
ORCHESTRATION: load (QR-FILE-0004) → set fileStatusId (QR-FILE-0006). No physical byte removal.
REPO: QR-FILE-0006 UPDATE — READ_WRITE | SECURITY: SCR-FILE-002 UPDATE (archive) / DELETE.
<!-- API:API-FILE-006:END -->
  <!-- SUB:SVC-API-FILES:END -->

  <!-- SUB:SVC-API-CATEGORIES:START -->
<!-- API:API-FILE-007:START -->
### API-FILE-007 — Categories CRUD
POST/GET/PUT/DELETE /api/v1/files/categories(/{id}) | FileCategoryController → FileCategoryService
REQUEST CategoryCreate/UpdateRequest{categoryCode(create-only), nameAr, nameEn, maxSizeBytes?, allowedContentTypes?, isActiveFl}
RESPONSE 201/200 CategoryResponse; search → Page<CategoryResponse>
VALIDATIONS: RULE-FILE-007 (unique categoryCode — Message-AR: رمز الفئة مستخدَم مسبقاً.); LOC (nameAr,nameEn required)
ERRORS: ERR-0005 → RULE-FILE-007 dup code → 409; ERR-0006 → NOT_FOUND → 404
REPO: QR-FILE-0007..0011 (find_one/search/save/update/EXISTS) — mixed — Sequence SEQ_FILE_CATEGORY | ALLOWED_SORT_FIELDS={categoryCode,nameAr,createdAt}
SECURITY: SCR-FILE-001 (VIEW/CREATE/UPDATE/DELETE — FILE_ADMIN).
<!-- API:API-FILE-007:END -->
  <!-- SUB:SVC-API-CATEGORIES:END -->

  <!-- SUB:SVC-API-LOOKUP:START -->
<!-- API:API-FILE-008:START -->
### API-FILE-008 — Lookups
GET /api/v1/files/lookups/{lookupKey} | FileLookupController.get → FileLookupService.get
REQUEST path lookupKey ∈ {FILE_FILE_TYPE, FILE_FILE_STATUS} | RESPONSE 200 [{code,labelAr,labelEn}]
VALIDATIONS: none | ERRORS: ERR-0006 → unknown lookupKey → 404
BINDING: LOV-FILE-001 FILE_FILE_TYPE (IMAGE,DOCUMENT,SPREADSHEET,ARCHIVE,OTHER); LOV-FILE-002 FILE_FILE_STATUS (ACTIVE,ARCHIVED,DELETED).
REPO: (runtime code resolution — no lookup table) — READ_ONLY | SECURITY: Security filter.
<!-- API:API-FILE-008:END -->
  <!-- SUB:SVC-API-LOOKUP:END -->

API Governance: RULE-ERR-CARRY ✓; RULE-PLATFORM-ERR ✓ (ERR-0006 = PLATFORM-STD, DRV-001); LOC ✓ (AR+EN). RULE-FILE-004 internal, RULE-FILE-006 permissive (carries its own message).
─────────────────────────────────────────────────────────────────
<!-- PHASE:SVC-API:END -->

<!-- PHASE:DOC:START -->
## PHASE DOC — Contract Stabilization (INTERNAL-ONLY, v2.0)
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓
DOC-1: API-FILE-001..008 all STABLE. DOC-2: fileContent never in JSON DTO; LOV fields String code; no Business Code.
DOC-3: JPA Page<T>; empty → 200; filters ownerId/type/moduleCode EXACT, fileName LIKE, status/type EXACT.
DOC GATE: PASSED ✓  ⚠ INTERNAL-ONLY — PASS 2 gates on real API Docs (CONTRACT-12).
─────────────────────────────────────────────────────────────────
<!-- PHASE:DOC:END -->

<!-- PHASE:INT-C:START -->
## PHASE INT-C — Integration Contract Specifications
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓
## INT-C SUMMARY — FILE — PLAN-ID: PLAN-FILE-001
XM-ID       │ Classification │ Target Module │ Interface     │ Contract Status
────────────┼────────────────┼───────────────┼───────────────┼────────────────
XM-FILE-001 │ SOFT-READ      │ Security (SEC)│ App-layer read│ CONTRACTED ✓

<!-- XM:XM-FILE-001:START -->
### XM-FILE-001 — SOFT-READ of SEC UserAccount identity
Target Module    : Security (SEC) | Target Entity: UserAccount (ENTITY-SEC-001) → SEC_USER_ACCOUNT
Classification   : SOFT-READ (application layer — NO physical FK, RULE-FILE-004)
Interface        : Security auth filter provides the authenticated principal; created_by populated from it (AuditEntityListener).
Data required    : authenticated user identity (for created_by + owner-visibility checks).
Fallback if absent: request rejected by Security filter before reaching FILE (401) — FILE does not self-verify JWT.
Blocks           : none (SEC gated ACTIVE, built before FILE) | Unblock condition: n/a (READY)
Idempotency      : read-only | Retry/Timeout: n/a (in-process filter)
<!-- XM:XM-FILE-001:END -->

INT-C GATE CHECK: [✓] all XM from DB Register accounted (XM-FILE-001) [✓] classification declared [✓] no new XM invented [✓] Open RXEs none
INT-C Gate: PASSED ✓
─────────────────────────────────────────────────────────────────
<!-- PHASE:INT-C:END -->

<!-- PHASE:INT-R:START -->
## PHASE INT-R — Runtime Activation Status
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓
## INT-R STATUS — FILE — PLAN-ID: PLAN-FILE-001
<!-- XM:XM-FILE-001-RT:START -->
XM-FILE-001 │ READY ✓ │ — (SEC_USER_ACCOUNT gated ACTIVE under DBS-SEC-001; SEC built before FILE per dependency order; identity supplied by Security filter at runtime)
<!-- XM:XM-FILE-001-RT:END -->
─────────────────────────────────────────────────────────────────
<!-- PHASE:INT-R:END -->

<!-- PHASE:SEC-BE:START -->
## PHASE SEC-BE — Backend Security Specifications
─────────────────────────────────────────────────────────────────
Gate Status: PASSED ✓
CORE-9 — one SCR-ID = one SEC_PAGES row = 4 permissions (generated by Security, not seeded by name).

### SEC-BE — SCR-FILE-001 — File Categories (page_code FILE_CATEGORIES)
  API enforcement: API-FILE-007 CRUD → VIEW/CREATE/UPDATE/DELETE. Roles: FILE_ADMIN (all).
### SEC-BE — SCR-FILE-002 — File Browser (page_code FILE_BROWSER)
  API enforcement: API-FILE-004/005 VIEW, API-FILE-006 UPDATE(archive)/DELETE(soft). CREATE (upload) is contextual in owner module.
  Roles: FILE_ADMIN.
Auth model: all FILE endpoints sit behind the Security filter (RULE-FILE-004); FILE never self-verifies JWT.
Owner visibility (RULE-FILE-005) enforced in service using the authenticated principal (XM-FILE-001).
SECURITY SEED DATA REQUIREMENTS:
  SEC_PAGE rows (owned by SEC, registered for FILE): FILE_CATEGORIES, FILE_BROWSER (parent: File Service).
  Permissions auto-generated 4-per-page by Security (RULE-SEC-011): PERM_FILE_CATEGORIES_{...}, PERM_FILE_BROWSER_{...} → granted to FILE_ADMIN.
SEC-BE Rules: SEC-IMPL-RULE-1/3/4 applied.
─────────────────────────────────────────────────────────────────
<!-- PHASE:SEC-BE:END -->

<!-- PHASE:ALIGN-BE:START -->
## PHASE ALIGN-BE — Backend Internal Self-Consistency Gate (auto-run)
─────────────────────────────────────────────────────────────────
## ALIGN-BE GATE — FILE — PLAN-ID: PLAN-FILE-001
Traceability: all FIELD/API/RULE/ERR/QR/XM-IDs appear in Plan Index ✓ | Derivation Log complete ✓ | DB field coverage ✓
Business Code: N/A ✓ | Localization: all RULE Message-AR ✓ | error responses AR+EN ✓
Security: every screen-serving API-ID has permission declared ✓ | SCR-FILE-001/002 have SEC-BE blocks ✓ | CORE-9 ✓
QRC: every DB-op API has QR-ID ✓ | agent-reference labels ✓ | no ENUM for LOV ✓ | no join to lookups ✓ | exact sequence names on SAVE ✓
TEST-BE: SECTION D present ✓ | no GAP without DEFERRED ✓
Artifact binding: no placeholders ✓ | RULE text inline ✓ | every column→DBF-ID ✓ | Message-AR exact ✓ | Manifest CONTRACT-1 ✓
Plan completeness: CORE arch ✓ | domain placement ✓ | no orgUnitId in DTO ✓ | no audit in Create/Update ✓ | LocalizedException ✓ | ERR 4-registration ✓ | ALLOWED_SORT_FIELDS ✓ | empty search→200 ✓ | soft-delete (RULE-FILE-006) ✓ | fileContent never in JSON DTO ✓
CROSS-MODULE: XM-FILE-001 SOFT-READ declared + READY, workaround n/a ✓ | INBOUND stubs n/a
═══════════════════════════════════════════════════════════════════
ALIGN-BE GATE RESULT: PASSED ✓  |  Auto-correction: None
═══════════════════════════════════════════════════════════════════

Table 3 — XM Dependency Gate:
XM-FILE-001 │ SOFT-READ │ READY ✓ │ — │ —
─────────────────────────────────────────────────────────────────
<!-- PHASE:ALIGN-BE:END -->

---

## SECTION A — ERROR CATALOG (canonical)
══════════════════════════════════════════════════════════════════════════════════
ERR-ID   │ RULE-ID       │ API-ID              │ HTTP │ Trigger                 │ Message-AR                      │ Message-EN
─────────┼───────────────┼─────────────────────┼──────┼─────────────────────────┼──────────────────────────────────┼──────────────────────────
ERR-0001 │ RULE-FILE-001 │ API-FILE-001        │ 413  │ File exceeds size limit  │ حجم الملف يتجاوز المسموح.        │ File exceeds allowed size.
ERR-0002 │ RULE-FILE-002 │ API-FILE-001        │ 415  │ Disallowed content type  │ نوع الملف غير مسموح.             │ File type not allowed.
ERR-0003 │ RULE-FILE-003 │ API-FILE-003        │ 401  │ Invalid/expired token    │ رابط الوصول غير صالح/منتهٍ.       │ Access link invalid or expired.
ERR-0004 │ RULE-FILE-005 │ API-FILE-001        │ 400  │ Missing ownership fields │ بيانات الملكية إلزامية.          │ Ownership fields required.
ERR-0005 │ RULE-FILE-007 │ API-FILE-007        │ 409  │ Duplicate category code  │ رمز الفئة مستخدَم مسبقاً.         │ Category code already exists.
ERR-0006 │ PLATFORM-STD  │ API-FILE-002/004/006/007/008 │ 404 │ Resource not found │ العنصر غير موجود.               │ Resource not found.
══════════════════════════════════════════════════════════════════════════════════
Total Errors: 6 (ERR-0006 = PLATFORM-STD, DRV-001). RULE-FILE-004 internal; RULE-FILE-006 permissive (own message, no error).
Every ERR-ID registered in 4 places.

---

## SECTION B — QUERY REFERENCE CATALOG (agent reference)
══════════════════════════════════════════════════════════════════
⚠ AGENT REFERENCE ONLY — rewrite every query using actual JPA entity/field names.
QR-FILE-0001 SAVE file document + bytes (SEQ_FILE_DOCUMENT) — READ_WRITE
QR-FILE-0002 FIND_ONE document by ID (for token issue) — READ_ONLY
QR-FILE-0003 FIND_ONE document bytes by ID (download, @Lob stream) — READ_ONLY
QR-FILE-0004 FIND_ONE document metadata by ID (bytes excluded) — READ_ONLY
QR-FILE-0005 FIND_BY_CRITERIA documents by owner (metadata only; ownerId/type/moduleCode EXACT, fileName LIKE, status/type EXACT) — READ_ONLY, paged
QR-FILE-0006 UPDATE fileStatusId (archive/soft-delete) — READ_WRITE
QR-FILE-0007 FIND_ONE category by ID — READ_ONLY
QR-FILE-0008 FIND_BY_CRITERIA categories — READ_ONLY, paged
QR-FILE-0009 SAVE category (SEQ_FILE_CATEGORY) — READ_WRITE
QR-FILE-0010 UPDATE category — READ_WRITE
QR-FILE-0011 EXISTS categoryCode (RULE-FILE-007) — READ_ONLY
Join governance: NEVER join to a lookups table (none). List queries exclude BYTEA content (DRV-003).
══════════════════════════════════════════════════════════════════

---

## SECTION C — REGISTRY UPDATE BLOCK
══════════════════════════════════════════════════════════════════
## REGISTRY UPDATE — 2026-09-02
Source: Project 3.1 PASS 1 | Feature Code FILE-001 | DBS-FILE-001 | Plan PLAN-FILE-001
New APIs: API-FILE-001..008 | QR-IDs: QR-FILE-0001..0011 (11)
XM-IDs: XM-FILE-001 SOFT-READ → SEC (READY ✓, ACTIVE)
OQ-IDs Open: None | Gate Status: ALIGN-BE PASSED ✓ | Next: Project 4.1 → Pipeline Grid FILE · P3.1 = done
══════════════════════════════════════════════════════════════════

---

## SECTION D — TC COVERAGE MATRIX SUMMARY (backend)
══════════════════════════════════════════════════════════════════
RULE-ID COVERAGE:
RULE-FILE-001 │ TC-BE-FILE-001 │ TC-BE-FILE-002 │ COVERED ✓
RULE-FILE-002 │ TC-BE-FILE-003 │ TC-BE-FILE-004 │ COVERED ✓
RULE-FILE-003 │ TC-BE-FILE-005 │ TC-BE-FILE-006 │ COVERED ✓
RULE-FILE-004 │ TC-BE-FILE-007 │ —             │ COVERED ✓ (auth-delegation assertion)
RULE-FILE-005 │ TC-BE-FILE-008 │ TC-BE-FILE-009 │ COVERED ✓
RULE-FILE-006 │ TC-BE-FILE-010 │ —             │ COVERED ✓ (soft-delete, bytes retained)
RULE-FILE-007 │ TC-BE-FILE-011 │ TC-BE-FILE-012 │ COVERED ✓
Rule coverage: 7/7 — 0 gaps.
API-ID COVERAGE: API-FILE-001..008 each ≥1 happy-path TC (TC-BE-FILE-013..020) — 8/8 covered.
DEFERRED TC REGISTRY: (none)
══════════════════════════════════════════════════════════════════
Gate SECTION D: PASSED ✓

---

## AGENT HANDOFF SUMMARY (BACKEND) — not a phase
Agent-ready. Rewrite QRC from scratch; store bytes in BYTEA @Lob (stream, never eager-load on list); auto-detect MIME server-side;
issue AES/GCM single-use access tokens (~100m) separate from JWT; delegate auth to Security filter (never self-verify JWT);
soft-delete = fileStatusId=DELETED (retain bytes); enforce per-category size/type limits. Run api-doc-generator before PASS 2.

*End of backend-execution-plan.md — FILE — PLAN-FILE-001 — ALIGN-BE ✓*
